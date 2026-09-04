package com.example.network

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer

/**
 * Real screen broadcasting foreground service (Android 10 -> 15 compliant).
 *
 * Flow: MediaProjection token -> VirtualDisplay (720p-scaled) -> ImageReader
 * (RGBA_8888) -> dedicated handler thread converts each frame to a Bitmap which is
 * pushed into [onFrameCaptured] so the P2P VideoEngine compresses/encrypts/streams it.
 */
class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val CHANNEL_ID = "localconnect_screen_share_channel"
        private const val NOTIFICATION_ID = 4711
        private const val MAX_LONG_EDGE = 1280 // 720p-class capture scale
        private const val TARGET_FPS_MS = 66L // ~15 fps steady broadcast
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_PROJECTION_DATA = "extra_projection_data"
        const val EXTRA_APP_NAME = "extra_app_name"

        /** Frame sink wired by LocalP2PEngine before the service starts. */
        @Volatile
        var onFrameCaptured: ((Bitmap) -> Unit)? = null

        /** Invoked when sharing stops (user, system, or projection revocation). */
        @Volatile
        var onShareStopped: (() -> Unit)? = null

        @Volatile
        private var activeService: ScreenCaptureService? = null

        fun recycleBitmap(bitmap: Bitmap) {
            activeService?.recycleBitmapInternal(bitmap)
        }

        fun start(context: Context, resultCode: Int, projectionData: Intent, appName: String) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_PROJECTION_DATA, projectionData)
                putExtra(EXTRA_APP_NAME, appName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, ScreenCaptureService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop screen capture service", e)
            }
        }
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var lastFrameSentAt = 0L
    private var isCapturing = false

    // Reusable per-capture buffers (allocated once per capture resolution) so the
    // converter produces ZERO per-frame allocations — previously each row allocated
    // an IntArray, generating millions of objects/minute and freezing the GC.
    private var pixelBuffer: IntArray? = null
    private var rowBuffer: ByteArray? = null

    // Small rotating Bitmap pool: the newest frame is handed to the UI/encoder while
    // the converter fills the next one; slots are recycled only after a full cycle.
    private val bitmapPool = ArrayDeque<Bitmap>()

    override fun onCreate() {
        super.onCreate()
        activeService = this
    }

    fun recycleBitmapInternal(bitmap: Bitmap) {
        if (bitmap.isMutable && !bitmap.isRecycled) {
            synchronized(bitmapPool) {
                if (bitmapPool.size < 4) {
                    bitmapPool.addLast(bitmap)
                } else {
                    try { bitmap.recycle() } catch (_: Exception) {}
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val projectionData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_PROJECTION_DATA)
        }
        val appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: "شاشة النظام"

        startInForeground(appName)

        if (projectionData != null && !isCapturing) {
            try {
                startCapture(resultCode, projectionData)
            } catch (e: Exception) {
                Log.e(TAG, "MediaProjection setup failed — stopping service", e)
                notifyShareStopped()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startInForeground(appName: String) {
        if (Build.VERSION.SDK_INT >= 34) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION") != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "FOREGROUND_SERVICE_MEDIA_PROJECTION not granted")
                stopSelf()
                return
            }
        }
        createNotificationChannel()
        val notification = buildNotification(appName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "مشاركة الشاشة",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(appName: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("مشاركة الشاشة قيد التشغيل...")
            .setContentText("يتم بث \"$appName\" مباشرة عبر الشبكة المحلية")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startCapture(resultCode: Int, projectionData: Intent) {
        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                ?: throw IllegalStateException("MediaProjectionManager unavailable")
        val projection = projectionManager.getMediaProjection(resultCode, projectionData)
            ?: throw IllegalStateException("MediaProjection token rejected")
        mediaProjection = projection
        isCapturing = true

        captureThread = HandlerThread("LocalConnectScreenCapture").also { it.start() }
        captureHandler = Handler(captureThread!!.looper)

        // Register revocation callback (user stops casting from system UI)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    notifyShareStopped()
                    stopSelf()
                }
            }, captureHandler)
        } else {
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    notifyShareStopped()
                    stopSelf()
                }
            }, captureHandler)
        }

        val metrics = resources.displayMetrics
        val longEdge = maxOf(metrics.widthPixels, metrics.heightPixels)
        val scale = if (longEdge > MAX_LONG_EDGE) MAX_LONG_EDGE.toFloat() / longEdge else 1f
        val captureWidth = (metrics.widthPixels * scale).toInt().coerceAtLeast(320)
        val captureHeight = (metrics.heightPixels * scale).toInt().coerceAtLeast(480)
        val dpi = metrics.densityDpi

        imageReader = ImageReader.newInstance(
            captureWidth,
            captureHeight,
            PixelFormat.RGBA_8888,
            4
        ).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val now = System.currentTimeMillis()
                    if (now - lastFrameSentAt >= TARGET_FPS_MS) {
                        lastFrameSentAt = now
                        val bitmap = imageToBitmap(image)
                        if (bitmap != null) {
                            onFrameCaptured?.invoke(bitmap)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Screen frame conversion failed", e)
                } finally {
                    try {
                        image.close()
                    } catch (_: Exception) {
                    }
                }
            }, captureHandler)
        }

        virtualDisplay = projection.createVirtualDisplay(
            "LocalConnectScreenBroadcast",
            captureWidth,
            captureHeight,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            captureHandler
        )

        Log.i(TAG, "Screen capture started: ${captureWidth}x${captureHeight} @$dpi dpi")
    }

    /** Converts an RGBA_8888 Image into a pooled Bitmap (row-stride safe, zero-alloc steady state). */
    private fun imageToBitmap(image: Image): Bitmap? {
        if (image.format != PixelFormat.RGBA_8888) return null
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        if (pixelStride != 4) return null

        val width = image.width
        val height = image.height

        if (pixelBuffer == null || pixelBuffer!!.size != width * height) {
            pixelBuffer = IntArray(width * height)
            rowBuffer = ByteArray(width * 4)
            bitmapPool.clear()
            repeat(3) { bitmapPool.addLast(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)) }
        }
        val pixels = pixelBuffer!!
        val rowBytes = rowBuffer!!

        val bitmap = obtainBitmap(width, height)

        if (rowStride == width * 4) {
            // Fast path: packed rows — raw copy straight into the bitmap
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)
        } else {
            // Padded row strides: reuse one row buffer and one full-size pixel buffer.
            // Bitmap.setPixels expects ARGB int packing: (A shl 24) or (R shl 16) or (G shl 8) or B,
            // while the Image buffer holds raw RGBA byte order — remap strictly.
            for (row in 0 until height) {
                buffer.position(row * rowStride)
                buffer.get(rowBytes)
                var base = row * width
                for (x in 0 until width) {
                    val o = x * 4
                    val r = rowBytes[o].toInt() and 0xFF
                    val g = rowBytes[o + 1].toInt() and 0xFF
                    val b = rowBytes[o + 2].toInt() and 0xFF
                    val a = rowBytes[o + 3].toInt() and 0xFF
                    pixels[base + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        }
        return bitmap
    }

    private fun obtainBitmap(width: Int, height: Int): Bitmap {
        val pooled = synchronized(bitmapPool) { bitmapPool.removeFirstOrNull() }
        if (pooled != null && pooled.width == width && pooled.height == height && pooled.isMutable && !pooled.isRecycled) {
            return pooled
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    private fun notifyShareStopped() {
        try {
            onShareStopped?.invoke()
        } catch (e: Exception) {
            Log.w(TAG, "Screen share stop callback failed", e)
        }
    }

    override fun onDestroy() {
        activeService = null
        onFrameCaptured = null
        isCapturing = false
        synchronized(bitmapPool) {
            bitmapPool.forEach { try { it.recycle() } catch (_: Exception) {} }
            bitmapPool.clear()
        }
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        imageReader = null
        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        mediaProjection = null
        try {
            captureThread?.quitSafely()
        } catch (_: Exception) {
        }
        captureThread = null
        captureHandler = null
        super.onDestroy()
    }
}
