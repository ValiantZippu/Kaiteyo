package ua.syt0r.kanji.core.knowledge

import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus
import ua.syt0r.kanji.core.srs.SrsCard
import kotlin.time.Duration

// ============================================================
// STUDY STATE
// ------------------------------------------------------------
// One explicit study-state machine shared by every surface
// (dictionary pages, card browser, library, graph). No scattered
// isLearned / isStudied / isKnown booleans anywhere.
//
// The states are a projection of the real FSRS card state:
//   - NEW         never studied
//   - LEARNING    in the learning steps
//   - KNOWN       review card, not yet due
//   - DUE         review card whose interval has elapsed
//   - MASTERED    review card with a long stable interval
//   - RELEARNING  FSRS relearning state (lapsed)
//   - SUSPENDED   user-suspended (persisted user intent, not FSRS)
//
// The projection is lossy on purpose: it answers "what should the
// UI say", the FSRS card remains the source of truth for
// scheduling. [project] derives the state from a real card; the
// [transition] table models user-intent events on top.
// ============================================================

enum class StudyState(val label: String, val jpLabel: String) {
    New("New", "新規"),
    Learning("Learning", "学習中"),
    Known("Known", "既知"),
    Due("Due", "復習"),
    Mastered("Mastered", "定着"),
    Relearning("Relearning", "再学習"),
    Suspended("Suspended", "一時停止")
}

/** User-intent events that move study state. */
enum class StudyEvent {
    /** A card enters a study session. */
    Study,
    /** Rated Again in review. */
    RateAgain,
    /** Rated Hard or Good. */
    RateHardGood,
    /** Rated Easy. */
    RateEasy,
    Suspend,
    Resume,
    /** Resets a card back to New (user forget). */
    Forget
}

object StudyStateMachine {

    /**
     * Projects a real FSRS card onto the UI study state.
     *
     * [now] is the reference instant used for the due check. A card whose
     * interval has elapsed is Due; a stable review card (21+ day interval)
     * is Mastered. Suspended is never produced here — it is user intent.
     */
    fun project(card: SrsCard?, now: Instant): StudyState {
        if (card == null) return StudyState.New
        val fsrs = card.fsrsCard
        return when (fsrs.status) {
            FsrsCardStatus.New -> StudyState.New
            FsrsCardStatus.Learning -> StudyState.Learning
            FsrsCardStatus.Relearning -> StudyState.Relearning
            FsrsCardStatus.Review -> {
                val due = card.expectedReview != null && now >= card.expectedReview
                if (due) StudyState.Due
                else if (card.interval >= MASTERED_INTERVAL) StudyState.Mastered
                else StudyState.Known
            }
        }
    }

    /**
     * Pure state machine for user-intent transitions. Used for optimistic UI
     * updates and for surfaces that do not hold a live FSRS card. The actual
     * scheduling always flows through the SRS scheduler; this table only
     * decides the *displayed* state after an event.
     */
    fun transition(current: StudyState, event: StudyEvent): StudyState = when (event) {
        StudyEvent.Study -> when (current) {
            StudyState.New -> StudyState.Learning
            StudyState.Suspended -> current
            else -> current
        }

        StudyEvent.RateAgain -> when (current) {
            StudyState.Learning -> StudyState.Learning
            StudyState.Known, StudyState.Due, StudyState.Mastered -> StudyState.Relearning
            else -> current
        }

        StudyEvent.RateHardGood -> when (current) {
            StudyState.Learning -> StudyState.Known
            StudyState.Relearning -> StudyState.Known
            StudyState.Known, StudyState.Due, StudyState.Mastered -> current
            else -> current
        }

        StudyEvent.RateEasy -> when (current) {
            StudyState.Learning -> StudyState.Mastered
            StudyState.Relearning -> StudyState.Known
            StudyState.Known, StudyState.Due -> StudyState.Mastered
            else -> current
        }

        StudyEvent.Suspend -> if (current == StudyState.Suspended) current else StudyState.Suspended
        StudyEvent.Resume -> if (current == StudyState.Suspended) StudyState.New else current
        StudyEvent.Forget -> StudyState.New
    }

    /** Longest interval (in days) that still counts as Known rather than Mastered. */
    const val MASTERED_INTERVAL_DAYS: Long = 21
    private val MASTERED_INTERVAL: Duration = Duration.parse("${MASTERED_INTERVAL_DAYS}d")
}
