package ua.syt0r.kanji.core.knowledge

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.srs.FsrsCard
import ua.syt0r.kanji.core.srs.FsrsCardParams
import ua.syt0r.kanji.core.srs.FsrsCardStatus
import ua.syt0r.kanji.core.srs.SrsCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class StudyStateMachineTest {

    private val now: Instant = Clock.System.now()

    private fun card(
        status: FsrsCardStatus,
        intervalDays: Long,
        reviewTime: Instant? = now
    ): SrsCard = SrsCard(
        FsrsCard(
            status = status,
            params = if (reviewTime == null) FsrsCardParams.New
            else FsrsCardParams.Existing(
                difficulty = 5.0,
                stability = 1.0,
                reviewTime = reviewTime
            ),
            interval = intervalDays.days,
            lapses = 0,
            repeats = 0
        )
    )

    @Test
    fun missingCardProjectsToNew() {
        assertEquals(StudyState.New, StudyStateMachine.project(null, now))
    }

    @Test
    fun newCardProjectsToNew() {
        assertEquals(StudyState.New, StudyStateMachine.project(card(FsrsCardStatus.New, 0, null), now))
    }

    @Test
    fun learningCardProjectsToLearning() {
        assertEquals(StudyState.Learning, StudyStateMachine.project(card(FsrsCardStatus.Learning, 1), now))
    }

    @Test
    fun relearningCardProjectsToRelearning() {
        assertEquals(StudyState.Relearning, StudyStateMachine.project(card(FsrsCardStatus.Relearning, 1), now))
    }

    @Test
    fun overdueReviewProjectsToDue() {
        val overdue = card(FsrsCardStatus.Review, intervalDays = 7, reviewTime = now - 8.days)
        assertEquals(StudyState.Due, StudyStateMachine.project(overdue, now))
    }

    @Test
    fun futureReviewProjectsToKnown() {
        val notDue = card(FsrsCardStatus.Review, intervalDays = 7, reviewTime = now)
        assertEquals(StudyState.Known, StudyStateMachine.project(notDue, now))
    }

    @Test
    fun stableReviewProjectsToMastered() {
        val stable = card(FsrsCardStatus.Review, intervalDays = 30, reviewTime = now)
        assertEquals(StudyState.Mastered, StudyStateMachine.project(stable, now))
    }

    // ---------------------------------------------------------------
    // Transitions
    // ---------------------------------------------------------------

    @Test
    fun studyingMovesNewToLearning() {
        assertEquals(StudyState.Learning, StudyStateMachine.transition(StudyState.New, StudyEvent.Study))
    }

    @Test
    fun againMovesKnownToRelearning() {
        assertEquals(StudyState.Relearning, StudyStateMachine.transition(StudyState.Known, StudyEvent.RateAgain))
    }

    @Test
    fun againStaysInLearning() {
        assertEquals(StudyState.Learning, StudyStateMachine.transition(StudyState.Learning, StudyEvent.RateAgain))
    }

    @Test
    fun goodMovesLearningToKnown() {
        assertEquals(StudyState.Known, StudyStateMachine.transition(StudyState.Learning, StudyEvent.RateHardGood))
    }

    @Test
    fun easyMovesLearningToMastered() {
        assertEquals(StudyState.Mastered, StudyStateMachine.transition(StudyState.Learning, StudyEvent.RateEasy))
    }

    @Test
    fun suspendAndResumeRoundTrip() {
        val suspended = StudyStateMachine.transition(StudyState.Known, StudyEvent.Suspend)
        assertEquals(StudyState.Suspended, suspended)
        assertEquals(StudyState.New, StudyStateMachine.transition(suspended, StudyEvent.Resume))
    }

    @Test
    fun forgetAlwaysReturnsNew() {
        assertEquals(StudyState.New, StudyStateMachine.transition(StudyState.Mastered, StudyEvent.Forget))
        assertEquals(StudyState.New, StudyStateMachine.transition(StudyState.Suspended, StudyEvent.Forget))
    }

    @Test
    fun suspendedIgnoresStudyEvent() {
        assertEquals(StudyState.Suspended, StudyStateMachine.transition(StudyState.Suspended, StudyEvent.Study))
    }
}
