package com.example.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.PeerVideoFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

class VideoEngine(private val context: Context) {

    companion object {
        private const val TAG = "VideoEngine"
        private const val HEADER_PEER_ID_SIZE = 32
        private const val HEADER_PEER_NAME_SIZE = 32
        private const val HEADER_META_SIZE = 32
        const val HEADER_TOTAL = HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE + HEADER_META_SIZE // 96 bytes

        // Wire framing after the 96-byte identity header:
        // [flags 1][frameSeq 2 BE][chunkIndex 2 BE][totalChunks 2 BE][payload <= MAX_CHUNK_PAYLOAD]
        // frameSeq identifies the logical frame; stale chunks arriving out-of-order from a
        // previous frame are discarded instead of corrupting the current reassembly buffer.
        const val FRAMING_SIZE = 7
        const val FLAG_ENCRYPTED: Byte = 0x01

        // Packets stay far below the 1472-byte UDP/MTU ceiling: 96 + 7 + 1200 = 1303.
        // This eliminates router/AP silent drops for oversized datagrams.
        private const val MAX_CHUNK_PAYLOAD = 1200
        private const val MAX_WIRE_PACKET = HEADER_TOTAL + FRAMING_SIZE + MAX_CHUNK_PAYLOAD
        private const val REASSEMBLY_TIMEOUT_MS = 2000L

        const val FRAME_TYPE_CAMERA: Byte = 0
        const val FRAME_TYPE_CAMERA_OFF: Byte = 1
        const val FRAME_TYPE_SCREEN_SHARE: Byte = 2
    }

    /** In-flight chunked frame reassembly state for one (peerId, frameType) stream. */
    private class FrameReassembly(val totalChunks: Int, val frameSeq: Int) {
        val chunks = arrayOfNulls<ByteArray>(totalChunks)
        var received = 0
        var timestamp = System.currentTimeMillis()
    }

    private var frameSeqCounter = 0

    private val scope = CoroutineScope(Dispatchers.IO)
    private var videoReceiveJob: Job? = null
    private var screenShareJob: Job? = null
    private var cameraOffKeepAliveJob: Job? = null

    private var sendSocket: DatagramSocket? = null
    private var receiveSocket: DatagramSocket? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentLifecycleOwner: WeakReference<LifecycleOwner>? = null
    private var currentSurfaceProvider: Preview.SurfaceProvider? = null

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera = _isFrontCamera.asStateFlow()

    private val _isVideoStreaming = MutableStateFlow(false)
    val isVideoStreaming = _isVideoStreaming.asStateFlow()
    val isStreaming = _isVideoStreaming.asStateFlow()

    private val _isCameraOff = MutableStateFlow(false)
    val isCameraOff = _isCameraOff.asStateFlow()

    private val _isScreenSharing = MutableStateFlow(false)
    val isScreenSharing = _isScreenSharing.asStateFlow()

    private val _sharedAppName = MutableStateFlow<String?>(null)
    val sharedAppName = _sharedAppName.asStateFlow()

    private val _localScreenShareBitmap = MutableStateFlow<Bitmap?>(null)
    val localScreenShareBitmap = _localScreenShareBitmap.asStateFlow()

    private val _remoteVideoFrames = MutableStateFlow<Map<String, PeerVideoFrame>>(emptyMap())
    val remoteVideoFrames = _remoteVideoFrames.asStateFlow()

    private val activeTargetAddresses = ConcurrentHashMap<String, Pair<InetAddress, Int>>()
    private val reassemblyMap = ConcurrentHashMap<String, FrameReassembly>()

    private var myPeerId: String = ""
    private var myPeerName: String = ""

    init {
        try {
            sendSocket = DatagramSocket()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create video send socket", e)
        }
    }

    fun setMyIdentity(id: String, name: String) {
        myPeerId = id
        myPeerName = name
    }

    fun updateTarget(id: String, address: InetAddress, port: Int = NetworkUtils.VIDEO_PORT) {
        activeTargetAddresses[id] = Pair(address, port)
    }

    fun removeTarget(id: String) {
        activeTargetAddresses.remove(id)
    }

    fun clearTargets() {
        activeTargetAddresses.clear()
    }

    fun toggleCamera() {
        switchCamera()
    }

    fun switchCamera(
        lifecycleOwner: LifecycleOwner? = currentLifecycleOwner?.get(),
        surfaceProvider: Preview.SurfaceProvider? = currentSurfaceProvider
    ) {
        _isFrontCamera.value = !_isFrontCamera.value
        val owner = lifecycleOwner ?: currentLifecycleOwner?.get()
        val surface = surfaceProvider ?: currentSurfaceProvider
        if (owner != null) {
            startCameraStream(owner, surface)
        }
    }

    fun toggleCameraVideo() {
        val newState = !_isCameraOff.value
        setCameraOff(newState)
    }

    fun setCameraOff(off: Boolean) {
        _isCameraOff.value = off
        if (off) {
            // Unbind the camera entirely: the sensor keeps draining battery and
            // heating the device if left bound while frames are dropped. Callers
            // render a "camera off" placeholder, so the preview is not needed.
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {
            }
            _isVideoStreaming.value = false
            startCameraOffKeepAlive()
        } else {
            cameraOffKeepAliveJob?.cancel()
            cameraOffKeepAliveJob = null
            // Rebind immediately so the preview resumes without waiting for the UI
            currentLifecycleOwner?.get()?.let { owner ->
                startCameraStream(owner)
            }
        }
    }

    private fun startCameraOffKeepAlive() {
        cameraOffKeepAliveJob?.cancel()
        cameraOffKeepAliveJob = scope.launch {
            while (isActive && _isCameraOff.value) {
                sendControlFrame(FRAME_TYPE_CAMERA_OFF, "")
                delay(1200L)
            }
        }
    }

    /**
     * Sends a control frame (e.g. camera-off keep-alive). totalChunks = 0 marks it
     * as control-only on the wire so receivers never try to reassemble it.
     */
    fun sendControlFrame(frameType: Byte, metaText: String = "") {
        if (activeTargetAddresses.isEmpty()) return
        scope.launch {
            try {
                val sendBuffer = ByteArray(HEADER_TOTAL + FRAMING_SIZE)
                writeIdentityHeader(sendBuffer, frameType, metaText)
                sendBuffer[HEADER_TOTAL] = 0 // no flags, no payload
                sendBuffer[HEADER_TOTAL + 1] = 0
                sendBuffer[HEADER_TOTAL + 2] = 0
                sendBuffer[HEADER_TOTAL + 3] = 0
                sendBuffer[HEADER_TOTAL + 4] = 0

                for ((_, target) in activeTargetAddresses) {
                    try {
                        val packet = DatagramPacket(
                            sendBuffer,
                            HEADER_TOTAL + FRAMING_SIZE,
                            target.first,
                            target.second
                        )
                        sendSocket?.send(packet)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun writeIdentityHeader(buffer: ByteArray, frameType: Byte, metaText: String) {
        val idBytes = myPeerId.toByteArray(Charsets.UTF_8).copyOf(HEADER_PEER_ID_SIZE)
        val nameBytes = myPeerName.toByteArray(Charsets.UTF_8).copyOf(HEADER_PEER_NAME_SIZE)
        val metaBytes = metaText.toByteArray(Charsets.UTF_8).copyOf(HEADER_META_SIZE - 1)
        System.arraycopy(idBytes, 0, buffer, 0, HEADER_PEER_ID_SIZE)
        System.arraycopy(nameBytes, 0, buffer, HEADER_PEER_ID_SIZE, HEADER_PEER_NAME_SIZE)
        buffer[HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE] = frameType
        System.arraycopy(
            metaBytes, 0, buffer,
            HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE + 1, metaBytes.size
        )
    }

    fun startScreenShare(appName: String, frameProvider: () -> Bitmap?) {
        _isScreenSharing.value = true
        _sharedAppName.value = appName

        screenShareJob?.cancel()
        screenShareJob = scope.launch {
            while (isActive && _isScreenSharing.value) {
                try {
                    val bmp = frameProvider()
                    if (bmp != null) {
                        _localScreenShareBitmap.value = bmp
                        if (activeTargetAddresses.isNotEmpty()) {
                            sendBitmapFrame(bmp, FRAME_TYPE_SCREEN_SHARE, appName)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Screen share frame error", e)
                }
                delay(60L) // ~16 fps for fluid live screen broadcast
            }
        }
    }

    fun sendDirectScreenShareBitmap(bitmap: Bitmap, appName: String) {
        // Mark the sharing state so the camera analyzer suppresses itself — otherwise
        // camera frames keep interleaving with screen frames and the remote view
        // flickers between the two streams.
        if (!_isScreenSharing.value) {
            _isScreenSharing.value = true
            _sharedAppName.value = appName
        } else if (_sharedAppName.value != appName) {
            _sharedAppName.value = appName
        }
        val oldBmp = _localScreenShareBitmap.value
        _localScreenShareBitmap.value = bitmap
        if (oldBmp != null && oldBmp !== bitmap && !oldBmp.isRecycled) {
            ScreenCaptureService.recycleBitmap(oldBmp)
        }
        if (activeTargetAddresses.isEmpty()) return
        scope.launch {
            sendBitmapFrame(bitmap, FRAME_TYPE_SCREEN_SHARE, appName)
        }
    }

    fun stopScreenShare() {
        _isScreenSharing.value = false
        _sharedAppName.value = null
        val oldBmp = _localScreenShareBitmap.value
        _localScreenShareBitmap.value = null
        if (oldBmp != null && !oldBmp.isRecycled) {
            ScreenCaptureService.recycleBitmap(oldBmp)
        }
        screenShareJob?.cancel()
        screenShareJob = null
    }

    /**
     * RULE 5: the compressed JPEG frame is encrypted with AES-256-GCM first, then the
     * ciphertext is chunked into MTU-safe packets ([chunkIndex/totalChunks] framing).
     */
    private fun sendBitmapFrame(bitmap: Bitmap, frameType: Byte, metaText: String) {
        try {
            val stream = ByteArrayOutputStream()
            // Scale if oversized to guarantee fast network throughput
            val maxDim = 640
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetW = if (ratio >= 1f) maxDim else (maxDim * ratio).toInt()
                val targetH = if (ratio >= 1f) (maxDim / ratio).toInt() else maxDim
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else {
                bitmap
            }

            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 65, stream)
            transmitFrame(frameType, metaText, stream.toByteArray(), encrypted = true)
        } catch (e: Exception) {
            Log.e(TAG, "sendBitmapFrame error", e)
        }
    }

    /**
     * Encrypts [bodyBytes] (optional) and streams it as MTU-safe chunked packets to
     * all active targets. Empty bodies are sent as control frames (totalChunks = 0).
     * Each logical frame gets a fresh 16-bit sequence stamped into every chunk.
     */
    private fun transmitFrame(frameType: Byte, metaText: String, bodyBytes: ByteArray, encrypted: Boolean) {
        if (activeTargetAddresses.isEmpty()) return

        val payload = if (encrypted && bodyBytes.isNotEmpty()) {
            LocalCryptoEngine.encrypt(bodyBytes)
        } else {
            bodyBytes
        }
        val flags: Byte = if (encrypted && payload.isNotEmpty()) FLAG_ENCRYPTED else 0

        val totalChunks = if (payload.isEmpty()) {
            0
        } else {
            (payload.size + MAX_CHUNK_PAYLOAD - 1) / MAX_CHUNK_PAYLOAD
        }

        val frameSeq = if (totalChunks == 0) {
            0
        } else {
            frameSeqCounter = (frameSeqCounter + 1) and 0xFFFF
            frameSeqCounter
        }

        val sendBuffer = ByteArray(MAX_WIRE_PACKET)
        writeIdentityHeader(sendBuffer, frameType, metaText)
        sendBuffer[HEADER_TOTAL] = flags
        sendBuffer[HEADER_TOTAL + 1] = ((frameSeq shr 8) and 0xFF).toByte()
        sendBuffer[HEADER_TOTAL + 2] = (frameSeq and 0xFF).toByte()

        if (totalChunks == 0) {
            sendBuffer[HEADER_TOTAL + 3] = 0
            sendBuffer[HEADER_TOTAL + 4] = 0
            sendBuffer[HEADER_TOTAL + 5] = 0
            sendBuffer[HEADER_TOTAL + 6] = 0
            val packet = DatagramPacket(sendBuffer, HEADER_TOTAL + FRAMING_SIZE)
            dispatchPacket(packet)
            return
        }

        for (chunkIndex in 0 until totalChunks) {
            val offset = chunkIndex * MAX_CHUNK_PAYLOAD
            val chunkSize = minOf(MAX_CHUNK_PAYLOAD, payload.size - offset)
            sendBuffer[HEADER_TOTAL + 3] = ((chunkIndex shr 8) and 0xFF).toByte()
            sendBuffer[HEADER_TOTAL + 4] = (chunkIndex and 0xFF).toByte()
            sendBuffer[HEADER_TOTAL + 5] = ((totalChunks shr 8) and 0xFF).toByte()
            sendBuffer[HEADER_TOTAL + 6] = (totalChunks and 0xFF).toByte()
            System.arraycopy(payload, offset, sendBuffer, HEADER_TOTAL + FRAMING_SIZE, chunkSize)

            val packet = DatagramPacket(
                sendBuffer,
                HEADER_TOTAL + FRAMING_SIZE + chunkSize
            )
            dispatchPacket(packet)
        }
    }

    private fun dispatchPacket(packet: DatagramPacket) {
        for ((_, target) in activeTargetAddresses) {
            try {
                packet.address = target.first
                packet.port = target.second
                sendSocket?.send(packet)
            } catch (e: Exception) {
                // Ignore individual dropped frames
            }
        }
    }

    /**
     * Starts listening for incoming UDP video frames from other peers.
     * Handles the MTU-safe chunked framing and decrypts reassembled frames.
     */
    fun startVideoReceiver() {
        if (videoReceiveJob?.isActive == true) return

        videoReceiveJob = scope.launch {
            try {
                receiveSocket?.close()
                receiveSocket = DatagramSocket(NetworkUtils.VIDEO_PORT).apply {
                    reuseAddress = true
                    receiveBufferSize = 2 * 1024 * 1024 // 2MB buffer for high stability
                }

                val buffer = ByteArray(65535)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    try {
                        receiveSocket?.receive(packet)
                        val length = packet.length
                        if (length >= HEADER_TOTAL + FRAMING_SIZE) {
                            handleIncomingFramePacket(buffer, length)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Video receive loop error", e)
            } finally {
                try {
                    receiveSocket?.close()
                } catch (e: Exception) {
                    // Ignore
                }
                receiveSocket = null
            }
        }
    }

    private fun handleIncomingFramePacket(buffer: ByteArray, length: Int) {
        val peerIdBytes = ByteArray(HEADER_PEER_ID_SIZE)
        val peerNameBytes = ByteArray(HEADER_PEER_NAME_SIZE)
        System.arraycopy(buffer, 0, peerIdBytes, 0, HEADER_PEER_ID_SIZE)
        System.arraycopy(buffer, HEADER_PEER_ID_SIZE, peerNameBytes, 0, HEADER_PEER_NAME_SIZE)

        val frameType = buffer[HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE]
        val metaBytes = ByteArray(HEADER_META_SIZE - 1)
        System.arraycopy(
            buffer, HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE + 1,
            metaBytes, 0, metaBytes.size
        )

        val peerId = String(peerIdBytes, Charsets.UTF_8).trimEnd { it == '\u0000' }
        val peerName = String(peerNameBytes, Charsets.UTF_8).trimEnd { it == '\u0000' }
        val metaText = String(metaBytes, Charsets.UTF_8).trimEnd { it == '\u0000' }
        if (peerId.isEmpty()) return

        val flags = buffer[HEADER_TOTAL].toInt()
        val encrypted = (flags and FLAG_ENCRYPTED.toInt()) != 0
        val frameSeq =
            ((buffer[HEADER_TOTAL + 1].toInt() and 0xFF) shl 8) or
                    (buffer[HEADER_TOTAL + 2].toInt() and 0xFF)
        val chunkIndex =
            ((buffer[HEADER_TOTAL + 3].toInt() and 0xFF) shl 8) or
                    (buffer[HEADER_TOTAL + 4].toInt() and 0xFF)
        val totalChunks =
            ((buffer[HEADER_TOTAL + 5].toInt() and 0xFF) shl 8) or
                    (buffer[HEADER_TOTAL + 6].toInt() and 0xFF)

        if (totalChunks > 500) {
            Log.w(TAG, "Ignoring frame with excessive totalChunks: $totalChunks")
            return
        }

        val payloadOffset = HEADER_TOTAL + FRAMING_SIZE
        val payloadLength = length - payloadOffset

        // Control frame (camera-off keep-alive) — no payload, no reassembly
        if (totalChunks == 0) {
            if (frameType == FRAME_TYPE_CAMERA_OFF) {
                publishFrame(
                    peerId, peerName, null, metaText,
                    isCameraOff = true, isScreenShare = false
                )
            }
            return
        }

        val key = "${peerId}#${frameType}"
        val reassembly = if (chunkIndex == 0) {
            // New frame: replaces any in-progress (possibly stale) reassembly
            FrameReassembly(totalChunks, frameSeq).also { reassemblyMap[key] = it }
        } else {
            // Continuation chunk: only accept when it belongs to the CURRENT frame;
            // late chunks from a previous (out-of-order) frame are discarded so they
            // can never mix into the new frame's pixel data.
            reassemblyMap[key]?.takeIf { it.frameSeq == frameSeq }
        }

        if (reassembly == null || reassembly.totalChunks != totalChunks || chunkIndex >= totalChunks) {
            // Lost the first chunk or stale state — drop until the next key chunk
            if (chunkIndex == 0) reassemblyMap.remove(key)
            return
        }

        if (reassembly.chunks[chunkIndex] == null) {
            reassembly.chunks[chunkIndex] = buffer.copyOfRange(payloadOffset, length)
            reassembly.received++
        }

        if (reassembly.received >= totalChunks) {
            reassemblyMap.remove(key)
            val cipherStream = ByteArrayOutputStream(payloadLength * totalChunks)
            for (chunk in reassembly.chunks) {
                cipherStream.write(chunk)
            }
            val frameBytes: ByteArray? = if (encrypted) {
                LocalCryptoEngine.decrypt(cipherStream.toByteArray())
            } else {
                cipherStream.toByteArray()
            }

            if (frameBytes != null && frameBytes.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)
                if (bitmap != null) {
                    publishFrame(
                        peerId, peerName, bitmap, metaText,
                        isCameraOff = false,
                        isScreenShare = frameType == FRAME_TYPE_SCREEN_SHARE
                    )
                }
            }
        }

        // Drop incomplete reassemblies that timed out (lost chunks)
        reassemblyMap.entries.removeIf { (_, state) ->
            System.currentTimeMillis() - state.timestamp > REASSEMBLY_TIMEOUT_MS
        }
    }

    private fun publishFrame(
        peerId: String,
        peerName: String,
        bitmap: Bitmap?,
        appTitle: String,
        isCameraOff: Boolean,
        isScreenShare: Boolean
    ) {
        val current = _remoteVideoFrames.value.toMutableMap()
        current[peerId] = PeerVideoFrame(
            peerId = peerId,
            peerName = peerName,
            bitmap = bitmap,
            timestamp = System.currentTimeMillis(),
            isCameraOff = isCameraOff,
            isScreenShare = isScreenShare,
            appTitle = if (isScreenShare) appTitle.ifBlank { "شاشة مشتركة" } else null
        )
        _remoteVideoFrames.value = current
    }

    fun stopVideoReceiver() {
        videoReceiveJob?.cancel()
        videoReceiveJob = null
        try {
            receiveSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        receiveSocket = null
        reassemblyMap.clear()
        _remoteVideoFrames.value = emptyMap()
    }

    /**
     * Binds CameraX to lifecycleOwner and starts streaming frames.
     */
    fun startCameraStream(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider? = null
    ) {
        if (lifecycleOwner.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) {
            return
        }

        currentLifecycleOwner = WeakReference(lifecycleOwner)
        if (surfaceProvider != null) {
            currentSurfaceProvider = surfaceProvider
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                if (lifecycleOwner.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) {
                    provider.unbindAll()
                    return@addListener
                }

                val cameraSelector = if (_isFrontCamera.value) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val preview = Preview.Builder().build()
                val activeSurface = surfaceProvider ?: currentSurfaceProvider
                if (activeSurface != null) {
                    preview.surfaceProvider = activeSurface
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                var frameCounter = 0
                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    frameCounter++
                    // Screen sharing suspends the camera stream entirely: both frame
                    // types map to the same peer slot on the receiver, and sending
                    // both simultaneously would flicker the remote view 15x/second.
                    if (frameCounter % 2 == 0 && activeTargetAddresses.isNotEmpty() && !_isCameraOff.value && !_isScreenSharing.value) {
                        processAndSendFrame(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                _isVideoStreaming.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera lifecycle", e)
                _isVideoStreaming.value = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processAndSendFrame(imageProxy: ImageProxy) {
        try {
            val jpegBytes = imageToJpegByteArray(imageProxy)
            if (jpegBytes != null) {
                transmitFrame(FRAME_TYPE_CAMERA, "", jpegBytes, encrypted = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Frame process error", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun imageToJpegByteArray(image: ImageProxy): ByteArray? {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        val vRowStride = image.planes[2].rowStride
        val uRowStride = image.planes[1].rowStride
        val vPixelStride = image.planes[2].pixelStride
        val uPixelStride = image.planes[1].pixelStride

        val width = image.width
        val height = image.height

        var pos = ySize
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vIndex = row * vRowStride + col * vPixelStride
                val uIndex = row * uRowStride + col * uPixelStride
                if (vIndex < vBuffer.limit() && uIndex < uBuffer.limit()) {
                    nv21[pos++] = vBuffer.get(vIndex)
                    nv21[pos++] = uBuffer.get(uIndex)
                }
            }
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val outStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 65, outStream)
        val rawJpeg = outStream.toByteArray()

        val originalBitmap = BitmapFactory.decodeByteArray(rawJpeg, 0, rawJpeg.size) ?: return null
        val matrix = Matrix()
        val rotationDegrees = image.imageInfo.rotationDegrees.toFloat()
        matrix.postRotate(rotationDegrees)
        if (_isFrontCamera.value) {
            matrix.postScale(-1f, 1f)
        }

        val targetWidth = 400
        val targetHeight = (originalBitmap.height * (targetWidth.toFloat() / originalBitmap.width)).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
        val rotatedBitmap = Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )

        val finalOut = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 65, finalOut)

        if (originalBitmap != scaledBitmap) {
            originalBitmap.recycle()
        }
        if (scaledBitmap != rotatedBitmap) {
            scaledBitmap.recycle()
        }
        rotatedBitmap.recycle()

        return finalOut.toByteArray()
    }

    fun stopCameraStream() {
        _isVideoStreaming.value = false
        _isCameraOff.value = false
        _isScreenSharing.value = false
        cameraOffKeepAliveJob?.cancel()
        cameraOffKeepAliveJob = null
        screenShareJob?.cancel()
        screenShareJob = null
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun release() {
        stopCameraStream()
        stopVideoReceiver()
        cameraExecutor.shutdown()
        sendSocket?.close()
        sendSocket = null
    }
}
