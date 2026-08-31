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
import java.util.concurrent.Executors

class VideoEngine(private val context: Context) {

    companion object {
        private const val TAG = "VideoEngine"
        private const val MAX_PACKET_SIZE = 65000
        private const val HEADER_PEER_ID_SIZE = 32
        private const val HEADER_PEER_NAME_SIZE = 32
        private const val HEADER_META_SIZE = 32
        const val HEADER_TOTAL = HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE + HEADER_META_SIZE // 96 bytes

        const val FRAME_TYPE_CAMERA: Byte = 0
        const val FRAME_TYPE_CAMERA_OFF: Byte = 1
        const val FRAME_TYPE_SCREEN_SHARE: Byte = 2
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var videoReceiveJob: Job? = null
    private var screenShareJob: Job? = null
    private var cameraOffKeepAliveJob: Job? = null

    private var sendSocket: DatagramSocket? = null
    private var receiveSocket: DatagramSocket? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentLifecycleOwner: LifecycleOwner? = null
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
        lifecycleOwner: LifecycleOwner? = currentLifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider? = currentSurfaceProvider
    ) {
        _isFrontCamera.value = !_isFrontCamera.value
        val owner = lifecycleOwner ?: currentLifecycleOwner
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
            startCameraOffKeepAlive()
        } else {
            cameraOffKeepAliveJob?.cancel()
            cameraOffKeepAliveJob = null
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

    fun sendControlFrame(frameType: Byte, metaText: String = "") {
        if (activeTargetAddresses.isEmpty()) return
        scope.launch {
            try {
                val sendBuffer = ByteArray(HEADER_TOTAL)
                val idBytes = myPeerId.toByteArray(Charsets.UTF_8).copyOf(HEADER_PEER_ID_SIZE)
                val nameBytes = myPeerName.toByteArray(Charsets.UTF_8).copyOf(HEADER_PEER_NAME_SIZE)
                val metaBytes = metaText.toByteArray(Charsets.UTF_8).copyOf(HEADER_META_SIZE - 1)

                System.arraycopy(idBytes, 0, sendBuffer, 0, HEADER_PEER_ID_SIZE)
                System.arraycopy(nameBytes, 0, sendBuffer, HEADER_PEER_ID_SIZE, HEADER_PEER_NAME_SIZE)
                sendBuffer[HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE] = frameType
                System.arraycopy(metaBytes, 0, sendBuffer, HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE + 1, metaBytes.size)

                for ((_, target) in activeTargetAddresses) {
                    try {
                        val packet = DatagramPacket(
                            sendBuffer,
                            HEADER_TOTAL,
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
        _localScreenShareBitmap.value = bitmap
        if (activeTargetAddresses.isEmpty()) return
        scope.launch {
            sendBitmapFrame(bitmap, FRAME_TYPE_SCREEN_SHARE, appName)
        }
    }

    fun stopScreenShare() {
        _isScreenSharing.value = false
        _sharedAppName.value = null
        _localScreenShareBitmap.value = null
        screenShareJob?.cancel()
        screenShareJob = null
    }

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
            val jpegBytes = stream.toByteArray()

            if (jpegBytes.size < (MAX_PACKET_SIZE - HEADER_TOTAL)) {
                val totalLength = HEADER_TOTAL + jpegBytes.size
                val sendBuffer = ByteArray(totalLength)

                val idBytes = myPeerId.toByteArray(Charsets.UTF_8).copyOf(HEADER_PEER_ID_SIZE)
                val nameBytes = myPeerName.toByteArray(Charsets.UTF_8).copyOf(HEADER_PEER_NAME_SIZE)
                val metaBytes = metaText.toByteArray(Charsets.UTF_8).copyOf(HEADER_META_SIZE - 1)

                System.arraycopy(idBytes, 0, sendBuffer, 0, HEADER_PEER_ID_SIZE)
                System.arraycopy(nameBytes, 0, sendBuffer, HEADER_PEER_ID_SIZE, HEADER_PEER_NAME_SIZE)
                sendBuffer[HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE] = frameType
                System.arraycopy(metaBytes, 0, sendBuffer, HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE + 1, metaBytes.size)
                System.arraycopy(jpegBytes, 0, sendBuffer, HEADER_TOTAL, jpegBytes.size)

                for ((_, target) in activeTargetAddresses) {
                    try {
                        val packet = DatagramPacket(
                            sendBuffer,
                            totalLength,
                            target.first,
                            target.second
                        )
                        sendSocket?.send(packet)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendBitmapFrame error", e)
        }
    }

    /**
     * Starts listening for incoming UDP video frames from other peers.
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

                val buffer = ByteArray(MAX_PACKET_SIZE)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    try {
                        receiveSocket?.receive(packet)
                        val length = packet.length
                        if (length >= HEADER_TOTAL) {
                            val peerIdBytes = ByteArray(HEADER_PEER_ID_SIZE)
                            val peerNameBytes = ByteArray(HEADER_PEER_NAME_SIZE)
                            System.arraycopy(buffer, 0, peerIdBytes, 0, HEADER_PEER_ID_SIZE)
                            System.arraycopy(buffer, HEADER_PEER_ID_SIZE, peerNameBytes, 0, HEADER_PEER_NAME_SIZE)

                            val frameType = buffer[HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE]
                            val metaBytes = ByteArray(HEADER_META_SIZE - 1)
                            System.arraycopy(buffer, HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE + 1, metaBytes, 0, metaBytes.size)

                            val peerId = String(peerIdBytes, Charsets.UTF_8).trimEnd { it == '\u0000' }
                            val peerName = String(peerNameBytes, Charsets.UTF_8).trimEnd { it == '\u0000' }
                            val metaText = String(metaBytes, Charsets.UTF_8).trimEnd { it == '\u0000' }

                            if (peerId.isNotEmpty()) {
                                val current = _remoteVideoFrames.value.toMutableMap()
                                when (frameType) {
                                    FRAME_TYPE_CAMERA_OFF -> {
                                        current[peerId] = PeerVideoFrame(
                                            peerId = peerId,
                                            peerName = peerName,
                                            bitmap = null,
                                            timestamp = System.currentTimeMillis(),
                                            isCameraOff = true,
                                            isScreenShare = false
                                        )
                                        _remoteVideoFrames.value = current
                                    }
                                    FRAME_TYPE_SCREEN_SHARE -> {
                                        val jpegOffset = HEADER_TOTAL
                                        val jpegLength = length - HEADER_TOTAL
                                        if (jpegLength > 0) {
                                            val bitmap = BitmapFactory.decodeByteArray(buffer, jpegOffset, jpegLength)
                                            if (bitmap != null) {
                                                current[peerId] = PeerVideoFrame(
                                                    peerId = peerId,
                                                    peerName = peerName,
                                                    bitmap = bitmap,
                                                    timestamp = System.currentTimeMillis(),
                                                    isCameraOff = false,
                                                    isScreenShare = true,
                                                    appTitle = metaText.ifBlank { "شاشة مشتركة" }
                                                )
                                                _remoteVideoFrames.value = current
                                            }
                                        }
                                    }
                                    else -> { // FRAME_TYPE_CAMERA
                                        val jpegOffset = HEADER_TOTAL
                                        val jpegLength = length - HEADER_TOTAL
                                        if (jpegLength > 0) {
                                            val bitmap = BitmapFactory.decodeByteArray(buffer, jpegOffset, jpegLength)
                                            if (bitmap != null) {
                                                current[peerId] = PeerVideoFrame(
                                                    peerId = peerId,
                                                    peerName = peerName,
                                                    bitmap = bitmap,
                                                    timestamp = System.currentTimeMillis(),
                                                    isCameraOff = false,
                                                    isScreenShare = false
                                                )
                                                _remoteVideoFrames.value = current
                                            }
                                        }
                                    }
                                }
                            }
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

    fun stopVideoReceiver() {
        videoReceiveJob?.cancel()
        videoReceiveJob = null
        try {
            receiveSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        receiveSocket = null
        _remoteVideoFrames.value = emptyMap()
    }

    /**
     * Binds CameraX to lifecycleOwner and starts streaming frames.
     */
    fun startCameraStream(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider? = null
    ) {
        currentLifecycleOwner = lifecycleOwner
        if (surfaceProvider != null) {
            currentSurfaceProvider = surfaceProvider
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
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
                    if (frameCounter % 2 == 0 && activeTargetAddresses.isNotEmpty() && !_isCameraOff.value && !_isScreenSharing.value) {
                        processAndSendFrame(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                _isVideoStreaming.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera lifecycle", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processAndSendFrame(imageProxy: ImageProxy) {
        try {
            val jpegBytes = imageToJpegByteArray(imageProxy)
            if (jpegBytes != null && jpegBytes.size < (MAX_PACKET_SIZE - HEADER_TOTAL)) {
                val totalLength = HEADER_TOTAL + jpegBytes.size
                val sendBuffer = ByteArray(totalLength)

                val idBytes = myPeerId.toByteArray(Charsets.UTF_8).copyOf(HEADER_PEER_ID_SIZE)
                val nameBytes = myPeerName.toByteArray(Charsets.UTF_8).copyOf(HEADER_PEER_NAME_SIZE)

                System.arraycopy(idBytes, 0, sendBuffer, 0, HEADER_PEER_ID_SIZE)
                System.arraycopy(nameBytes, 0, sendBuffer, HEADER_PEER_ID_SIZE, HEADER_PEER_NAME_SIZE)
                sendBuffer[HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE] = FRAME_TYPE_CAMERA

                System.arraycopy(jpegBytes, 0, sendBuffer, HEADER_TOTAL, jpegBytes.size)

                for ((_, target) in activeTargetAddresses) {
                    try {
                        val packet = DatagramPacket(
                            sendBuffer,
                            totalLength,
                            target.first,
                            target.second
                        )
                        sendSocket?.send(packet)
                    } catch (e: Exception) {
                        // Ignore individual dropped frames
                    }
                }
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
        val resultBytes = finalOut.toByteArray()
        return if (resultBytes.size < (MAX_PACKET_SIZE - HEADER_TOTAL)) {
            resultBytes
        } else {
            val fallbackOut = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 45, fallbackOut)
            fallbackOut.toByteArray()
        }
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
