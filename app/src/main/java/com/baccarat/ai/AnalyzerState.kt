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
        val locked = recordedHistory
        if (locked.isNotEmpty() && candidate.size < locked.size) {
            pendingMatches = 0
            roadmapStatus = "Roadmap read incomplete • keeping " + locked.size + " saved beads"
            return
        }
        if (locked.isNotEmpty() && candidate.take(locked.size) != locked) {
            pendingMatches = 0
            roadmapStatus = "Roadmap partly blocked • waiting for a clean read"
            return
        }
        if (candidate == pendingHistory) pendingMatches++ else {
            pendingHistory = candidate
            pendingMatches = 1
        }
        if (pendingMatches >= 3) {
            recordedHistory = candidate
            val p = candidate.count { it == "P" }
            val b = candidate.count { it == "B" }
            val t = candidate.count { it == "T" }
            roadmapStatus = "Big Road locked • ${candidate.size} beads • P:${p} B:${b} T:${t}"
        } else {
            roadmapStatus = "Confirming Roadmap • ${pendingMatches}/3 stable reads"
        }
    }

    /**
     * Reports historical conditional frequencies only. It is not a prediction.
     * No overall-history fallback: if the current sequence has too few comparable
     * examples, show NO CLEAR EDGE rather than manufacture a confident side.
     */
    fun estimateNext(): PatternEstimate {
        val outcomes = recordedHistory.filter { it == "P" || it == "B" }
        if (outcomes.size < 12) {
            return PatternEstimate("WAITING", 0, outcomes.size, "Need at least 12 confirmed Player/Banker results")
        }

        fun following(patternLength: Int): List<String> {
            val suffix = outcomes.takeLast(patternLength)
            val found = mutableListOf<String>()
            for (i in 0 until outcomes.size - patternLength) {
                if (outcomes.subList(i, i + patternLength) == suffix) {
                    found.add(outcomes[i + patternLength])
                }
            }
            return found
        }

        // Prefer the most specific repeatable sequence; use shorter context only
        // when it has a larger, defensible sample.
        val three = following(3)
        val two = following(2)
        val one = following(1)
        val selected = when {
            three.size >= 6 -> three to "After last 3 results"
            two.size >= 8 -> two to "After last 2 results"
            one.size >= 12 -> one to "After last result"
            else -> return PatternEstimate("NO CLEAR EDGE", 0,
                maxOf(three.size, two.size, one.size),
                "Not enough matching historical examples")
        }

        val sample = selected.first
        val p = sample.count { it == "P" }
        val b = sample.count { it == "B" }
        val total = p + b
        if (total < 6) return PatternEstimate("NO CLEAR EDGE", 0, total, "Insufficient comparable results")

        val rate = maxOf(p, b) * 100 / total
        // Require a meaningful gap before displaying a side; 50/50 or near-even
        // samples must not be presented as a Player/Banker call.
        if (kotlin.math.abs(p - b) < 2 || rate < 60) {
            return PatternEstimate("NO CLEAR EDGE", rate, total, selected.second)
        }
        val side = if (p > b) "PLAYER" else "BANKER"
        return PatternEstimate(side, rate, total, selected.second)
    }
}
