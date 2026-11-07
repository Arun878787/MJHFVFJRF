package com.baccarat.ai

data class PatternEstimate(val side: String, val confidence: Int, val sampleSize: Int, val pattern: String)

object AnalyzerState {
    @Volatile var latestOcrText: String = "Waiting for screen…"
    @Volatile var playerCount: Int? = null
    @Volatile var bankerCount: Int? = null
    @Volatile var tieCount: Int? = null
    @Volatile var roadmapStatus: String = "Waiting for game screen…"
    @Volatile private var recordedHistory: List<String> = emptyList()

    fun update(text: String) { latestOcrText = text.replace(Regex("\\s+"), " ").take(400) }
    fun setHistory(history: List<String>) { recordedHistory = history.toList() }

    fun estimateNext(): PatternEstimate {
        val outcomes = recordedHistory.filter { it == "P" || it == "B" }
        if (outcomes.size < 8) return PatternEstimate("NOT ENOUGH DATA", 0, outcomes.size, "Need 8+ Player/Banker results")
        val lastTwo = outcomes.takeLast(2).joinToString("")
        val lastOne = outcomes.last().toString()
        fun matches(suffix: String): List<String> {
            val found = mutableListOf<String>()
            for (i in 0 until outcomes.size - suffix.length) {
                if (outcomes.subList(i, i + suffix.length).joinToString("") == suffix) found.add(outcomes[i + suffix.length])
            }
            return found
        }
        val twoBack = matches(lastTwo)
        val oneBack = matches(lastOne)
        val sample = when { twoBack.size >= 4 -> twoBack; oneBack.size >= 4 -> oneBack; else -> outcomes }
        val p = sample.count { it == "P" }
        val b = sample.count { it == "B" }
        val total = p + b
        val side = if (p >= b) "PLAYER" else "BANKER"
        val confidence = if (total == 0) 0 else maxOf(p, b) * 100 / total
        val pattern = when { twoBack.size >= 4 -> "After " + lastTwo; oneBack.size >= 4 -> "After " + lastOne; else -> "Overall recorded sequence" }
        return PatternEstimate(side, confidence, total, pattern)
    }
}