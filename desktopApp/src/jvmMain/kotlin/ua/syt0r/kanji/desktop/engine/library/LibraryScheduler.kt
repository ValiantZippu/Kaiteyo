package ua.syt0r.kanji.desktop.engine.library

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.desktop.engine.srs.SrsSchedulingStatus
import ua.syt0r.kanji.desktop.engine.srs.SrsScheduler
import ua.syt0r.kanji.desktop.engine.srs.toLike
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.StudyModeProgress

// ============================================
// LIBRARY SCHEDULER
// Drives the independent per-study-mode SRS state.
// Reuses the shared SrsScheduler for scheduling so
// each mode behaves like its own Anki deck, but
// every mutation is scoped to StudyModeProgress —
// never to the shared card.
// ============================================

object LibraryScheduler {

    /** Grade one mode progress entry. Returns the updated progress. */
    fun schedule(
        progress: StudyModeProgress,
        rating: ReviewRating,
        now: Instant = Clock.System.now()
    ): StudyModeProgress {
        val result = SrsScheduler.schedule(
            currentStatus = progress.status.toLike(),
            currentInterval = progress.intervalDays,
            currentEase = progress.ease,
            lapses = progress.lapses,
            learningSteps = 0,
            rating = rating.toLike(),
            now = now
        )
        val correct = rating != ReviewRating.Again
        val nextStreak = if (correct) progress.streak + 1 else 0
        val nextReps = progress.reps + 1
        val nextTotalReviews = progress.totalReviews + 1
        val nextTotalCorrect = progress.totalCorrect + if (correct) 1 else 0
        val nextAccuracy = ((progress.reps * progress.accuracy) + if (correct) 1.0 else 0.0) / nextReps.toDouble()

        return progress.copy(
            status = when (result.status) {
                SrsSchedulingStatus.Learning -> SrsStatus.Learning
                SrsSchedulingStatus.Review -> SrsStatus.Review
                SrsSchedulingStatus.Relearning -> SrsStatus.Relearning
            },
            intervalDays = result.intervalDays,
            ease = result.ease,
            dueAt = result.dueAt,
            reps = nextReps,
            lapses = progress.lapses + if (rating == ReviewRating.Again) 1 else 0,
            accuracy = nextAccuracy.toFloat().coerceIn(0f, 1f),
            streak = nextStreak,
            bestStreak = maxOf(progress.bestStreak, nextStreak),
            lastReviewedAt = now,
            totalReviews = nextTotalReviews,
            totalCorrect = nextTotalCorrect
        )
    }

    /** Project a shared card onto a mode-specific view (independent SRS fields). */
    fun project(card: ua.syt0r.kanji.desktop.model.DesktopCard, progress: StudyModeProgress): ua.syt0r.kanji.desktop.model.DesktopCard =
        card.copy(
            status = progress.status,
            intervalDays = progress.intervalDays,
            dueAt = progress.dueAt,
            reps = progress.reps,
            lapses = progress.lapses,
            ease = progress.ease,
            accuracy = progress.accuracy,
            lastReviewedAt = progress.lastReviewedAt
        )

    /** Reset a mode's progress back to a fresh card. */
    fun forget(progress: StudyModeProgress): StudyModeProgress =
        progress.copy(
            status = SrsStatus.New,
            intervalDays = 0.0,
            dueAt = null,
            reps = 0,
            lapses = 0,
            ease = 2.5,
            accuracy = 0.5f,
            streak = 0,
            lastReviewedAt = null,
            totalReviews = 0,
            totalCorrect = 0
        )

    /** Reschedule a mode's progress to a fixed interval in days. */
    fun reschedule(progress: StudyModeProgress, days: Int, now: Instant = Clock.System.now()): StudyModeProgress {
        val d = days.toDouble().coerceAtLeast(0.1)
        return progress.copy(
            status = if (d < 1.0) SrsStatus.Learning else SrsStatus.Review,
            intervalDays = d,
            dueAt = now + ua.syt0r.kanji.desktop.engine.srs.intervalDaysToDuration(d)
        )
    }
}