package com.baccarat.ai

data class PatternEstimate(val side: String, val confidence: Int, val sampleSize: Int, val pattern: String)

object AnalyzerState {
    @Volatile var latestOcrText: String = "Waiting for screen…"
    @Volatile var playerCount: Int? = null
    @Volatile var bankerCount: Int? = null
    @Volatile var tieCount: Int? = null
    @Volatile var roadmapStatus: String = "Waiting for game screen…"
    @Volatile private var recordedHistory: List<String> = emptyList()
    private var pendingHistory: List<String> = emptyList()
    private var pendingMatches = 0

    fun update(text: String) { latestOcrText = text.replace(Regex("\\s+"), " ").take(400) }
    fun setHistory(history: List<String>) { recordedHistory = history.toList() }

    @Synchronized
    fun submitDetectedHistory(history: List<String>) {
        val candidate = history.filter { it == "P" || it == "B" || it == "T" }
        if (candidate.size < 8) {
            pendingMatches = 0
            roadmapStatus = "Reading Roadmap… not enough clear beads"
            return
        }
        if (candidate == pendingHistory) {
            pendingMatches++
        } else {
            pendingHistory = candidate
            pendingMatches = 1
        }
        if (pendingMatches >= 3) {
            recordedHistory = candidate
            val p = candidate.count { it == "P" }
            val b = candidate.count { it == "B" }
            roadmapStatus = "Big Road locked • ${candidate.size} beads • P:$p B:$b"
        } else {
            roadmapStatus = "Confirming Roadmap • ${pendingMatches}/3 stable reads"
        }
    }

    fun estimateNext(): PatternEstimate {
        val outcomes = recordedHistory.filter { it == "P" || it == "B" }
        if (outcomes.size < 8) return PatternEstimate("WAITING", 0, outcomes.size, "Waiting for 3 matching Roadmap reads")
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
        if (total == 0) return PatternEstimate("WAITING", 0, 0, "No usable Player/Banker sample")
        val side = if (p > b) "PLAYER" else if (b > p) "BANKER" else "NO CLEAR EDGE"
        val confidence = maxOf(p, b) * 100 / total
        val pattern = when { twoBack.size >= 4 -> "After " + lastTwo; oneBack.size >= 4 -> "After " + lastOne; else -> "Overall recorded sequence" }
        return PatternEstimate(side, confidence, total, pattern)
    }
}