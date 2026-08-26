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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class VideoEngine(private val context: Context) {

    companion object {
        private const val TAG = "VideoEngine"
        private const val MAX_PACKET_SIZE = 65000
        private const val HEADER_PEER_ID_SIZE = 32
        private const val HEADER_PEER_NAME_SIZE = 32
        private const val HEADER_TOTAL = HEADER_PEER_ID_SIZE + HEADER_PEER_NAME_SIZE
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var videoReceiveJob: Job? = null

    private var sendSocket: DatagramSocket? = null
    private var receiveSocket: DatagramSocket? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera = _isFrontCamera.asStateFlow()

    private val _isVideoStreaming = MutableStateFlow(false)
    val isVideoStreaming = _isVideoStreaming.asStateFlow()
    val isStreaming = _isVideoStreaming.asStateFlow()

    private val _remoteVideoFrames = MutableStateFlow<Map<String, PeerVideoFrame>>(emptyMap())
    val remoteVideoFrames = _remoteVideoFrames.asStateFlow()

    // Local latest preview frame if needed
    private val _localPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val localPreviewBitmap = _localPreviewBitmap.asStateFlow()

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
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun switchCamera() {
        toggleCamera()
    }

    /**
     * Starts listening for incoming UDP video frames from other peers.
     */
    fun startVideoReceiver() {
        if (videoReceiveJob?.isActive == true) return

        videoReceiveJob = scope.launch {
            try {
                receiveSocket?.close()
                receiveSocket = DatagramSocket(NetworkUtils.VIDEO_PORT)
                receiveSocket?.reuseAddress = true
                receiveSocket?.receiveBufferSize = 1024 * 1024 // 1MB buffer

                val buffer = ByteArray(MAX_PACKET_SIZE)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    try {
                        receiveSocket?.receive(packet)
                        val length = packet.length
                        if (length > HEADER_TOTAL) {
                            val peerIdBytes = ByteArray(HEADER_PEER_ID_SIZE)
                            val peerNameBytes = ByteArray(HEADER_PEER_NAME_SIZE)
                            System.arraycopy(buffer, 0, peerIdBytes, 0, HEADER_PEER_ID_SIZE)
                            System.arraycopy(buffer, HEADER_PEER_ID_SIZE, peerNameBytes, 0, HEADER_PEER_NAME_SIZE)

                            val peerId = String(peerIdBytes, Charsets.UTF_8).trimEnd { it == '\u0000' }
                            val peerName = String(peerNameBytes, Charsets.UTF_8).trimEnd { it == '\u0000' }

                            val jpegOffset = HEADER_TOTAL
                            val jpegLength = length - HEADER_TOTAL

                            val bitmap = BitmapFactory.decodeByteArray(buffer, jpegOffset, jpegLength)
                            if (bitmap != null && peerId.isNotEmpty()) {
                                val current = _remoteVideoFrames.value.toMutableMap()
                                current[peerId] = PeerVideoFrame(
                                    peerId = peerId,
                                    peerName = peerName,
                                    bitmap = bitmap,
                                    timestamp = System.currentTimeMillis()
                                )
                                _remoteVideoFrames.value = current
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
     * Binds CameraX to lifeCycleOwner and starts streaming frames.
     */
    fun startCameraStream(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val cameraSelector = if (_isFrontCamera.value) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val preview = Preview.Builder()
                    .build()
                if (surfaceProvider != null) {
                    preview.surfaceProvider = surfaceProvider
                }

                // Image analysis for lightweight transmission
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                var frameCounter = 0

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    frameCounter++
                    // Downsample framerate to ~12-15 fps to preserve bandwidth on local Wi-Fi
                    if (frameCounter % 2 == 0 && activeTargetAddresses.isNotEmpty()) {
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

        // Convert YUV_420_888 to NV21
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
        // Compress quality 40% for smooth local streaming
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 40, outStream)
        val rawJpeg = outStream.toByteArray()

        // Downscale and rotate image to portrait
        val originalBitmap = BitmapFactory.decodeByteArray(rawJpeg, 0, rawJpeg.size) ?: return null
        val matrix = Matrix()
        val rotationDegrees = image.imageInfo.rotationDegrees.toFloat()
        matrix.postRotate(rotationDegrees)
        if (_isFrontCamera.value) {
            matrix.postScale(-1f, 1f) // Mirror front camera
        }

        // Scale to 240p width for high speed low bandwidth
        val targetWidth = 240
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
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, finalOut)
        return finalOut.toByteArray()
    }

    fun stopCameraStream() {
        _isVideoStreaming.value = false
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
