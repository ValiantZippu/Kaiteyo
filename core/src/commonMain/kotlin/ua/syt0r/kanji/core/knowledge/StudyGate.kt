package ua.syt0r.kanji.core.knowledge

import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.srs.SrsCard
import ua.syt0r.kanji.core.srs.SrsItemStatus

// ============================================================
// STUDY GATE — study state on knowledge entities (spec §15)
// ------------------------------------------------------------
// A thin projection layer that answers \"what is my study state
// for this kanji / word?\" uniformly. The source of truth is
// always the real SRS card (FSRS scheduling); the gate only
// maps card state onto the shared StudyState vocabulary and
// bridges the coarse SrsItemStatus (New/Done/Review) used by
// list screens.
//
// Lookups are intentionally async and go through a callback so
// dictionary screens can show study badges without the UI ever
// knowing about SRS internals.
// ============================================================

object StudyGate {

    /** Projects a real FSRS card onto the shared study state. */
    fun stateFor(card: SrsCard?, now: Instant): StudyState =
        StudyStateMachine.project(card, now)

    /** Bridges the coarse list-screen status used by browsers. */
    fun fromSrsItemStatus(status: SrsItemStatus): StudyState = when (status) {
        SrsItemStatus.New -> StudyState.New
        SrsItemStatus.Done -> StudyState.Known
        SrsItemStatus.Review -> StudyState.Due
    }

    /**
     * The SRS card keys a kanji participates in: the writing and reading
     * letter practices. Kept here so the UI never builds SRS keys by hand.
     */
    fun cardKeysForKanji(character: String): List<ua.syt0r.kanji.core.srs.SrsCardKey> =
        listOf(
            ua.syt0r.kanji.core.srs.LetterPracticeType.Writing.toSrsKey(character),
            ua.syt0r.kanji.core.srs.LetterPracticeType.Reading.toSrsKey(character)
        )

    /** The SRS card key a word participates in (flashcard practice). */
    fun cardKeyForWord(wordId: Long): ua.syt0r.kanji.core.srs.SrsCardKey =
        ua.syt0r.kanji.core.srs.VocabPracticeType.Flashcard.toSrsKey(wordId)

    /**
     * True when a card is considered \"learned\" enough to count toward a
     * learner's known set (used by sentence difficulty overlays).
     */
    fun isKnown(state: StudyState): Boolean = when (state) {
        StudyState.Known, StudyState.Mastered -> true
        StudyState.New, StudyState.Learning, StudyState.Due,
        StudyState.Relearning, StudyState.Suspended -> false
    }
}
