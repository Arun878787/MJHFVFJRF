package com.baccarat.ai

object AnalyzerState {
    @Volatile var ocrText: String = "Waiting for screen…"
    fun update(text: String) {
        ocrText = text.replace(Regex("\\s+"), " ").take(300)
    }
}
