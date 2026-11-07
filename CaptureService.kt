package com.baccarat.ai

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: android.hardware.display.VirtualDisplay? = null
    private var thread: HandlerThread? = null
    private var worker: Handler? = null
    private val busy = AtomicBoolean(false)
    private var lastFrameAt = 0L
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("Baccarat AI • Screen reader")
            .setContentText("Screen analysis is active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true).build()
        startForeground(
            NOTIFICATION_ID, notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA)
        }
        if (code != -1 || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(code, data)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopSelf() }
            }, Handler(mainLooper))

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            thread = HandlerThread("BaccaratOCR").also { it.start() }
            worker = Handler(thread!!.looper)
            reader = ImageReader.newInstance(
                metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2
            )
            reader?.setOnImageAvailableListener({ source ->
                val now = System.currentTimeMillis()
                if (now - lastFrameAt < 1200 || !busy.compareAndSet(false, true)) {
                    source.acquireLatestImage()?.close()
                    return@setOnImageAvailableListener
                }
                val frame = source.acquireLatestImage()
                if (frame == null) {
                    busy.set(false)
                    return@setOnImageAvailableListener
                }
                lastFrameAt = now
                analyze(frame)
            }, worker)

            display = projection?.createVirtualDisplay(
                "BaccaratSpeedCapture", metrics.widthPixels, metrics.heightPixels,
                metrics.densityDpi,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader?.surface, null, worker
            )
            startService(Intent(this, FloatingPanelService::class.java))
        } catch (error: Exception) {
            Log.e("BaccaratAI", "Capture could not start", error)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun analyze(frame: Image) {
        var bitmap: Bitmap? = null
        try {
            bitmap = frameToBitmap(frame)
            frame.close()
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { AnalyzerState.update(it.text) }
                .addOnFailureListener { Log.w("BaccaratAI", "OCR failed", it) }
                .addOnCompleteListener {
                    bitmap?.recycle()
                    busy.set(false)
                }
        } catch (error: Exception) {
            frame.close()
            bitmap?.recycle()
            busy.set(false)
            Log.w("BaccaratAI", "Frame processing failed", error)
        }
    }

    private fun frameToBitmap(frame: Image): Bitmap {
        val plane = frame.planes[0]
        val pixelStride = plane.pixelStride
        val rowPadding = plane.rowStride - pixelStride * frame.width
        val paddedWidth = frame.width + rowPadding / pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, frame.height, Bitmap.Config.ARGB_8888)
        val buffer: ByteBuffer = plane.buffer
        padded.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, frame.width, frame.height)
        padded.recycle()
        return cropped
    }

    override fun onDestroy() {
        display?.release()
        reader?.close()
        projection?.stop()
        thread?.quitSafely()
        try { recognizer.close() } catch (_: Exception) {}
        stopService(Intent(this, FloatingPanelService::class.java))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Screen reading", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_CODE = "projection_code"
        const val EXTRA_DATA = "projection_data"
        private const val CHANNEL = "baccarat_screen_reader"
        private const val NOTIFICATION_ID = 4160
    }
}
