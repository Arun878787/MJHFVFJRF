package com.baccarat.ai

import java.util.Locale

/** Shared, on-device OCR output. No screen images are uploaded. */
object AnalyzerState {
    @Volatile var ocrText: String = "Waiting for screen capture…"
        private set

    @Volatile var lastUpdated: Long = 0L
        private set

    fun update(text: String) {
        ocrText = text.replace(Regex("\\s+"), " ").trim().take(1200)
        lastUpdated = System.currentTimeMillis()
    }

    /** Reads only an on-screen P/B/T tally when OCR can see one. */
    fun visibleTally(): String? {
        val text = ocrText.uppercase(Locale.US)
        val match = Regex("\\bP\\s*[:]?\\s*(\\d{1,4})\\D{0,18}\\bB\\s*[:]?\\s*(\\d{1,4})\\D{0,18}\\bT\\s*[:]?\\s*(\\d{1,4})\\b")
            .find(text) ?: return null
        val p = match.groupValues[1].toIntOrNull() ?: return null
        val b = match.groupValues[2].toIntOrNull() ?: return null
        val t = match.groupValues[3].toIntOrNull() ?: return null
        val total = p + b + t
        if (total == 0) return null
        return "Visible history: P $p (${p * 100 / total}%)  •  B $b (${b * 100 / total}%)  •  T $t (${t * 100 / total}%)"
    }
}
