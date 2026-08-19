package ua.syt0r.kanji.core.knowledge

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.srs.SrsCard
import ua.syt0r.kanji.core.srs.SrsCardKey

// ============================================================
// STUDY OVERLAY — real SRS state projected onto search
// ------------------------------------------------------------
// The bridge that lets search sort by "recently studied" and
// filter by study state (spec §15, §18–§19, todo #108–#110):
// one snapshot of the user's real FSRS cards, keyed by kanji,
// built once per search from SrsCardRepository.getAll().
//
// Honest semantics (mirrors StudyGate / StudyStatusProvider):
//   - a kanji with NO card at all is New (never fabricated as
//     studied);
//   - a kanji studied in several practices reports the state of
//     its most-advanced practice (Mastered > Known > … > New);
//   - timestamps come from the real card fields (lastReview,
//     createdAt when present) — absent timestamps are null, and
//     null never pretends to be a date.
// ============================================================

/** One kanji's projected study snapshot. */
data class KanjiStudyInfo(
    val state: StudyState,
    /** Real last-review instant (null = never reviewed). */
    val lastReview: Instant?,
    /** Real first-seen instant when the card records it (null = unknown). */
    val added: Instant?
)

/**
 * Immutable snapshot of every kanji's study state, built once and
 * reused across a search pass. [info] absent = never studied (New).
 */
data class StudyOverlay(
    private val kanji: Map<String, KanjiStudyInfo> = emptyMap()
) {
    fun info(character: String): KanjiStudyInfo? = kanji[character]

    /** A kanji's effective state — null card projects to New. */
    fun state(character: String): StudyState = kanji[character]?.state ?: StudyState.New

    val studiedCount: Int get() = kanji.size

    val knownCount: Int get() = kanji.values.count { it.state == StudyState.Known || it.state == StudyState.Mastered }

    val dueCount: Int get() = kanji.values.count { it.state == StudyState.Due || it.state == StudyState.Relearning }

    val learningCount: Int get() = kanji.values.count { it.state == StudyState.Learning }
}

/**
 * Builds a [StudyOverlay] from the real SRS card map (pure —
 * inject a card map in tests, `SrsCardRepository.getAll()` in
 * production). [now] anchors the due projection.
 */
object StudyOverlayBuilder {

    fun build(
        cards: Map<SrsCardKey, SrsCard>,
        now: Instant = Clock.System.now()
    ): StudyOverlay {
        // Group cards by their item key (the kanji character for letter
        // practices). Vocabulary cards are excluded — this overlay drives the
        // kanji search surface.
        val byKanji = mutableMapOf<String, MutableList<SrsCard>>()
        cards.forEach { (key, card) ->
            if (key.practiceType == ua.syt0r.kanji.core.srs.SrsPracticeType.LetterWriting.value ||
                key.practiceType == ua.syt0r.kanji.core.srs.SrsPracticeType.LetterReading.value
            ) {
                byKanji.getOrPut(key.itemKey) { mutableListOf() }.add(card)
            }
        }

        val infos = byKanji.mapValues { (_, practiceCards) ->
            // Most-advanced state wins; last review is the newest across
            // practices. "Added" honestly means the EARLIEST real review
            // (the FsrsCard model records no separate creation date, so we
            // never invent one) — a kanji that was never reviewed reports
            // null, and a New card's null lastReview stays null.
            val state = practiceCards
                .map { StudyStateMachine.project(it, now) }
                .maxByOrNull { STATE_PRIORITY[it] ?: 0 } ?: StudyState.New
            val lastReview = practiceCards.mapNotNull { it.lastReview }.maxOrNull()
            val added = practiceCards.mapNotNull { it.lastReview }.minOrNull()
            KanjiStudyInfo(
                state = state,
                lastReview = lastReview,
                added = added
            )
        }

        return StudyOverlay(infos)
    }

    /** Higher = more advanced (used to pick the dominant practice). */
    private val STATE_PRIORITY: Map<StudyState, Int> = mapOf(
        StudyState.Suspended to 0,
        StudyState.New to 1,
        StudyState.Learning to 2,
        StudyState.Relearning to 3,
        StudyState.Due to 4,
        StudyState.Known to 5,
        StudyState.Mastered to 6
    )
}
