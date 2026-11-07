package com.baccarat.ai

object AnalyzerState {
    @Volatile var latestOcrText: String = "Waiting for screen…"
    @Volatile var playerCount: Int? = null
    @Volatile var bankerCount: Int? = null
    @Volatile var tieCount: Int? = null

    fun update(text: String) {
        latestOcrText = text.replace(Regex("\\s+"), " ").take(400)
    }
}