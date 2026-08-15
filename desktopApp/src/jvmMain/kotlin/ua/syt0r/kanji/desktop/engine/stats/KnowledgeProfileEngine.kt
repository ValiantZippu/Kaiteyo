package ua.syt0r.kanji.desktop.engine.stats

import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.kana.kanaCatalog
import ua.syt0r.kanji.desktop.engine.learning.CardType
import ua.syt0r.kanji.desktop.engine.learning.LearningItemKind
import ua.syt0r.kanji.desktop.engine.learning.LearningStage
import ua.syt0r.kanji.desktop.engine.learning.NoteCard

// ============================================
// KNOWLEDGE PROFILE
// A study-based estimate of what the learner can
// currently handle — never a fake JLPT score.
//
// Every number is derived from real learning data
// (notes, card stages, review events, writing
// attempts). The language is deliberately honest:
// "estimated coverage", "study-based estimate",
// "theoretical JLPT coverage". JLPT band sizes are
// approximate reference totals (the published
// cumulative lists vary by source) and are labelled
// as such.
// ============================================

/** One coverage dimension of the knowledge profile. */
data class KnowledgeDimension(
    val key: String,
    val label: String,
    val known: Int,
    val total: Int,
    /** Measured accuracy (0..1) where real attempts exist. */
    val accuracy: Float? = null
) {
    val fraction: Float get() = if (total == 0) 0f else (known.toFloat() / total).coerceIn(0f, 1f)
    val knownPercent: Int get() = (fraction * 100).toInt()
}

/** Theoretical JLPT coverage at a cumulative level (N5 → N1). */
data class JlptCoverageEstimate(
    val level: Int,
    val kanjiKnown: Int,
    val kanjiTotal: Int,
    val vocabKnown: Int,
    val vocabTotal: Int
)

/** Vocabulary coverage inside a frequency band (e.g. top 1,000 words). */
data class FrequencyCoverageEstimate(
    val band: Int,
    val known: Int,
    val total: Int
)

data class KnowledgeProfile(
    val dimensions: List<KnowledgeDimension>,
    val writingAccuracy: Float?,
    val writingAttempts: Int,
    val jlpt: List<JlptCoverageEstimate>,
    val frequency: List<FrequencyCoverageEstimate>,
    /** Rough confidence in the estimate, driven by how much data exists. */
    val confidence: String
) {
    val overallFraction: Float
        get() = if (dimensions.isEmpty()) 0f
        else dimensions.filter { it.total > 0 }.let { d ->
            if (d.isEmpty()) 0f else d.sumOf { it.fraction.toDouble() }.toFloat() / d.size
        }
}

/** Approximate cumulative JLPT word/character band sizes (theoretical). */
private val JLPT_KANJI_TOTALS = mapOf(5 to 100, 4 to 300, 3 to 650, 2 to 1000, 1 to 2000)
private val JLPT_VOCAB_TOTALS = mapOf(5 to 800, 4 to 1500, 3 to 3000, 2 to 6000, 1 to 10000)

object KnowledgeProfileEngine {

    /** The learner is "known" (usable) once a card is established (21d+). */
    private fun isKnown(card: NoteCard): Boolean =
        card.stage == LearningStage.Established || card.stage == LearningStage.Mature

    fun profile(state: AppState): KnowledgeProfile {
        val learning = state.learning
        val notes = learning.notes
        val cards = learning.cards

        fun cardsFor(kind: LearningItemKind): List<NoteCard> =
            cards.filter { card -> notes.firstOrNull { it.id == card.noteId }?.kind == kind }

        fun establishedCount(kind: LearningItemKind): Int =
            cardsFor(kind).count { isKnown(it) }

        // ---- Dimensions -------------------------------------------------
        val kanjiTotal = notes.count { it.kind == LearningItemKind.Kanji }
        val vocabTotal = notes.count { it.kind == LearningItemKind.Vocabulary }
        val kanaTotal = kanaCatalog.size

        val kanjiKnown = establishedCount(LearningItemKind.Kanji)
        val vocabKnown = establishedCount(LearningItemKind.Vocabulary)
        val kanaKnown = establishedCount(LearningItemKind.Kana)

        // Reading/recognition accuracy from real review events.
        fun accuracyFor(cardType: CardType): Float? {
            val events = learning.reviewEvents.filter { it.cardType == cardType }
            if (events.isEmpty()) return null
            return events.count { it.correct }.toFloat() / events.size
        }

        // Writing accuracy from real stroke evaluations.
        val writingAttempts = learning.writingAttempts
        val writingAccuracy = if (writingAttempts.isEmpty()) null
        else writingAttempts.map { it.accuracy }.average().toFloat().coerceIn(0f, 1f)

        val dimensions = buildList {
            add(KnowledgeDimension("kana", "Kana", kanaKnown, kanaTotal, accuracyFor(CardType.Recognition)))
            add(KnowledgeDimension("kanji", "Kanji", kanjiKnown, kanjiTotal, accuracyFor(CardType.Reading)))
            add(KnowledgeDimension("vocabulary", "Vocabulary", vocabKnown, vocabTotal, accuracyFor(CardType.Reading)))
            if (kanjiTotal > 0) {
                add(KnowledgeDimension("writing", "Writing (kanji + kana)", kanjiKnown + kanaKnown, kanjiTotal + kanaTotal, writingAccuracy))
            }
        }

        // ---- Theoretical JLPT coverage (cumulative, approximate) --------
        val jlpt = (5 downTo 1).map { level ->
            val cumulative = level..5
            val kanjiKnownCum = notes.count {
                it.kind == LearningItemKind.Kanji && it.jlpt != null && it.jlpt in cumulative &&
                    cardsFor(LearningItemKind.Kanji).any { c -> c.noteId == it.id && isKnown(c) }
            }
            val vocabKnownCum = notes.count {
                it.kind == LearningItemKind.Vocabulary && it.jlpt != null && it.jlpt in cumulative &&
                    cardsFor(LearningItemKind.Vocabulary).any { c -> c.noteId == it.id && isKnown(c) }
            }
            JlptCoverageEstimate(
                level = level,
                kanjiKnown = kanjiKnownCum,
                kanjiTotal = JLPT_KANJI_TOTALS[level] ?: 0,
                vocabKnown = vocabKnownCum,
                vocabTotal = JLPT_VOCAB_TOTALS[level] ?: 0
            )
        }

        // ---- Frequency coverage (top 1k/2k/5k/10k vocabulary) -----------
        val frequency = listOf(1000, 2000, 5000, 10000).map { band ->
            val inBand = notes.filter {
                it.kind == LearningItemKind.Vocabulary && it.frequency != null && it.frequency <= band
            }
            FrequencyCoverageEstimate(
                band = band,
                known = inBand.count { note ->
                    cardsFor(LearningItemKind.Vocabulary).any { c -> c.noteId == note.id && isKnown(c) }
                },
                total = inBand.size
            )
        }.filter { it.total > 0 }

        // ---- Confidence: more measured events = higher confidence --------
        val evidence = learning.reviewEvents.size + learning.writingAttempts.size
        val confidence = when {
            evidence >= 500 -> "High"
            evidence >= 100 -> "Medium"
            else -> "Low"
        }

        return KnowledgeProfile(
            dimensions = dimensions,
            writingAccuracy = writingAccuracy,
            writingAttempts = writingAttempts.size,
            jlpt = jlpt,
            frequency = frequency,
            confidence = confidence
        )
    }
}
