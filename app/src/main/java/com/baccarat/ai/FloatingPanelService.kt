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
    private var wm: WindowManager? = null
    private var panel: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            val estimate = AnalyzerState.estimateNext()
            val resultText = if (estimate.confidence == 0) {
                "ESTIMATE: " + estimate.side + "\\n" + estimate.pattern
            } else {
                "PATTERN ESTIMATE: " + estimate.side + "\\nHistorical frequency: " + estimate.confidence + "%\\nMatched sample: " + estimate.sampleSize + " • " + estimate.pattern
            }
            panel?.text = "BACCARAT AI • LIVE\\n" + resultText + "\\n\\nOCR: " + AnalyzerState.latestOcrText.take(90)
            handler.postDelayed(this, 1200)
        }
    }
    override fun onCreate() {
        super.onCreate()
        if (!android.provider.Settings.canDrawOverlays(this)) { stopSelf(); return }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        panel = TextView(this).apply {
            text = "BACCARAT AI • LIVE\\nPreparing pattern analysis…"
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(0xE8172033.toInt())
            setPadding(18, 14, 18, 14)
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT)
            .apply { gravity = Gravity.TOP or Gravity.END; x = 12; y = 190 }
        wm?.addView(panel, params)
        handler.post(refresh)
    }
    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        panel?.let { wm?.removeView(it) }
        panel = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}