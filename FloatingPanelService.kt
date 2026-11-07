package com.baccarat.ai

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class FloatingPanelService : Service() {
    private var windowManager: WindowManager? = null
    private var panel: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            val tally = AnalyzerState.visibleTally()
            val detail = tally ?: AnalyzerState.ocrText.take(260)
            panel?.text = "BACCARAT AI • LIVE OCR\n$detail\n\nHistorical frequency only — not a next-hand prediction"
            handler.postDelayed(this, 1500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        panel = TextView(this).apply {
            text = "BACCARAT AI • LIVE OCR\nWaiting for readable screen text…"
            textSize = 12f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(0xE8172033.toInt())
            setPadding(18, 14, 18, 14)
        }
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 12
            y = 190
            width = (resources.displayMetrics.widthPixels * 0.78f).toInt()
        }
        windowManager?.addView(panel, params)
        handler.post(refresh)
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        panel?.let { windowManager?.removeView(it) }
        panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
