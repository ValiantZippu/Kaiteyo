package ua.syt0r.kanji.core.knowledge

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.srs.SrsCard
import ua.syt0r.kanji.core.srs.SrsCardKey
import ua.syt0r.kanji.core.srs.SrsCardRepository
import ua.syt0r.kanji.core.srs.SrsPracticeType

// ============================================================
// STUDY STATUS PROVIDER — real SRS state on knowledge pages
// ------------------------------------------------------------
// The bridge between dictionary pages and the study system
// (spec §15): given a kanji or word, fetch the REAL FSRS cards
// (via SrsCardRepository + StudyGate) and project them onto the
// shared StudyState vocabulary. A null card means "never
// studied" — shown as New, never fabricated.
//
// This is the data source for the Study cards on kanji and word
// entries — the cards previously showed a static legend of all
// possible states, which was decoration; now they show the
// actual per-practice state.
// ============================================================

/** One practice's study state for an entity. */
data class StudyEntry(
    /** Human practice label, e.g. "Writing" / "Reading" / "Flashcard". */
    val practiceLabel: String,
    val state: StudyState,
    /** True when no SRS card exists yet — never studied. */
    val isNew: Boolean
)

class StudyStatusProvider(
    private val cards: SrsCardRepository
) {

    /** Study states for a kanji's writing + reading practices. */
    suspend fun kanjiStates(
        character: String,
        now: Instant = Clock.System.now()
    ): List<StudyEntry> = StudyGate.cardKeysForKanji(character).map { key ->
        val card = cards.get(key)
        StudyEntry(
            practiceLabel = practiceLabel(key),
            state = StudyGate.stateFor(card, now),
            isNew = card == null
        )
    }

    /** Study state for a word's flashcard practice, or null when no key exists. */
    suspend fun wordState(
        wordId: Long,
        now: Instant = Clock.System.now()
    ): StudyEntry? {
        val key = StudyGate.cardKeyForWord(wordId)
        val card = cards.get(key)
        return StudyEntry(
            practiceLabel = practiceLabel(key),
            state = StudyGate.stateFor(card, now),
            isNew = card == null
        )
    }

    private fun practiceLabel(key: SrsCardKey): String = when (key.practiceType) {
        SrsPracticeType.LetterWriting.value -> "Writing"
        SrsPracticeType.LetterReading.value -> "Reading"
        SrsPracticeType.VocabFlashcard.value -> "Flashcard"
        SrsPracticeType.VocabReadingPicker.value -> "Reading picker"
        SrsPracticeType.VocabWriting.value -> "Writing"
        else -> "Practice"
    }
}
