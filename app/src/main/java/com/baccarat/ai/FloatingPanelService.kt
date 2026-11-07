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
            val displayText = if (estimate.confidence == 0) {
                "HISTORICAL PATTERN CHECK\nWAITING FOR RESULTS\n" + estimate.pattern
            } else {
                "HISTORICAL PATTERN CHECK\n" + estimate.side +
                    "\nHistorical frequency: " + estimate.confidence + "%" +
                    "\nMatched hands: " + estimate.sampleSize +
                    "\nPattern: " + estimate.pattern
            }
            panel?.text = "BACCARAT AI  •  ROADMAP READER\n" + AnalyzerState.roadmapStatus + "\n\n" +
                displayText +
                "\n\nPast results do not predict the next hand."
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        panel = TextView(this).apply {
            text = "BACCARAT AI\nPreparing estimate…"
            textSize = 10f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(0xF01A2940.toInt())
            setPadding(10, 8, 10, 8)
            setLineSpacing(1f, 1f)
            maxWidth = (resources.displayMetrics.widthPixels * 0.43f).toInt()
            elevation = 12f
        }
        val type = if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 8
            y = 8
        }
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