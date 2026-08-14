package ua.syt0r.kanji.core.statistics

// ============================================================
// DECK ANALYTICS
// Per-deck aggregates derived from recorded study sessions.
// Each deck gets the same treatment — built-in decks and custom
// decks are not special-cased.
// ============================================================

/** Aggregated retention/activity for a single deck, from real sessions. */
data class DeckRetention(
    val deckId: Long,
    val deckName: String,
    val sessions: Int = 0,
    val itemsStudied: Int = 0,
    val correct: Int = 0,
    val studyTimeMs: Long = 0L
) {
    val accuracy: Float get() = if (itemsStudied == 0) 0f else correct.toFloat() / itemsStudied
    val hasData: Boolean get() = itemsStudied > 0
}

object DeckRetentionCalculator {

    /**
     * Groups recorded study sessions by deck and aggregates accuracy and
     * volume. Sessions without a deck (deckId == 0, e.g. legacy records)
     * are grouped under "General".
     */
    fun fromSessions(sessions: List<StudySessionRecord>): List<DeckRetention> =
        sessions
            .groupBy { it.deckId }
            .map { (deckId, list) ->
                val name = list.firstNotNullOfOrNull { it.deckName.takeIf { n -> n.isNotBlank() } }
                    ?: if (deckId == 0L) "General" else "Deck $deckId"
                DeckRetention(
                    deckId = deckId,
                    deckName = name,
                    sessions = list.size,
                    itemsStudied = list.sumOf { it.itemsStudied },
                    correct = list.sumOf { it.correct },
                    studyTimeMs = list.sumOf { it.duration.inWholeMilliseconds }
                )
            }
            .sortedWith(compareByDescending<DeckRetention> { it.itemsStudied }.thenBy { it.deckName.lowercase() })

    /** Decks with the weakest observed accuracy (items > 0), best for the "weak deck" list. */
    fun weakest(sessions: List<StudySessionRecord>, limit: Int = 5): List<DeckRetention> =
        fromSessions(sessions)
            .filter { it.hasData }
            .sortedBy { it.accuracy }
            .take(limit)
}
