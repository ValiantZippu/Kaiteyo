package ua.syt0r.kanji.desktop.engine.srs

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.math.pow
import kotlin.math.roundToInt

// ============================================
// SRS SCHEDULER
// A clean, deterministic, testable spaced-repetition
// engine inspired by FSRS + SM-2. Pure Kotlin — no
// platform dependencies, fully unit-testable.
// ============================================

/**
 * Tunable scheduler parameters.
 */
data class SrsParameters(
    val learningStepsMinutes: List<Long> = listOf(1, 10),
    val relearningStepsMinutes: List<Long> = listOf(10),
    val graduatingIntervalDays: Double = 1.0,
    val easyBonus: Double = 1.3,
    val intervalModifier: Double = 1.0,
    val maximumIntervalDays: Double = 3650.0,
    val targetRetention: Double = 0.9,
    val lapseMultiplier: Double = 0.5,
    val newCardInitialEase: Double = 2.5,
    val hardIntervalPenalty: Double = 1.2
) {
    companion object {
        val Default = SrsParameters()
    }
}

/**
 * Immutable scheduling outcome for one answer.
 */
data class SrsScheduleResult(
    val status: SrsSchedulingStatus,
    val intervalDays: Double,
    val ease: Double,
    val dueAt: Instant,
    val stepIndex: Int = -1
)

enum class SrsSchedulingStatus { Learning, Review, Relearning }

object SrsScheduler {

    /**
     * Compute the next state of a card given the answer rating.
     *
     * @param currentStatus   current lifecycle status
     * @param currentInterval interval (in days) before this review
     * @param currentEase     current ease multiplier
     * @param lapses          number of times the card has lapsed
     * @param learningSteps   number of completed learning steps so far
     * @param rating          how the learner answered
     * @param now             clock timestamp used for scheduling
     * @param params          tunable parameters
     */
    fun schedule(
        currentStatus: SrsStatusLike,
        currentInterval: Double,
        currentEase: Double,
        lapses: Int,
        learningSteps: Int,
        rating: ReviewRatingLike,
        now: Instant = Clock.System.now(),
        params: SrsParameters = SrsParameters.Default
    ): SrsScheduleResult {

        when (rating) {
            ReviewRatingLike.Again -> return rescheduleFailed(
                currentStatus, currentInterval, currentEase, lapses, learningSteps, now, params
            )

            ReviewRatingLike.Hard -> {
                if (currentStatus == SrsStatusLike.Learning || currentStatus == SrsStatusLike.Relearning) {
                    val nextStep = learningSteps + 1
                    val stepMinutes = params.learningStepsMinutes.getOrElse(nextStep) {
                        params.learningStepsMinutes.lastOrNull() ?: 10
                    }
                    return SrsScheduleResult(
                        status = if (currentStatus == SrsStatusLike.Relearning) SrsSchedulingStatus.Relearning else SrsSchedulingStatus.Learning,
                        intervalDays = stepMinutes / 1440.0,
                        ease = currentEase,
                        dueAt = now + stepMinutes.minutes,
                        stepIndex = nextStep
                    )
                }
                return SrsScheduleResult(
                    status = SrsSchedulingStatus.Review,
                    intervalDays = (currentInterval / params.hardIntervalPenalty).coerceAtLeast(0.1),
                    ease = currentEase,
                    dueAt = now + intervalToDuration((currentInterval / params.hardIntervalPenalty).coerceAtLeast(0.1))
                )
            }

            ReviewRatingLike.Good, ReviewRatingLike.Easy -> {
                // Still inside learning steps?
                val stepMinutes = params.learningStepsMinutes.getOrNull(learningSteps + 1)
                if (currentStatus == SrsStatusLike.New || currentStatus == SrsStatusLike.Learning) {
                    if (stepMinutes != null) {
                        return SrsScheduleResult(
                            status = SrsSchedulingStatus.Learning,
                            intervalDays = stepMinutes / 1440.0,
                            ease = currentEase,
                            dueAt = now + stepMinutes.minutes,
                            stepIndex = learningSteps + 1
                        )
                    }
                    // Graduate: move from learning to review.
                    val interval = params.graduatingIntervalDays * if (rating == ReviewRatingLike.Easy) params.easyBonus else 1.0
                    val newEase = if (currentEase <= 1.0) params.newCardInitialEase else currentEase
                    return SrsScheduleResult(
                        status = SrsSchedulingStatus.Review,
                        intervalDays = interval,
                        ease = newEase,
                        dueAt = now + intervalToDuration(interval)
                    )
                }

                // Review / relearning success.
                val bonus = if (rating == ReviewRatingLike.Easy) params.easyBonus else 1.0
                val easeDelta = if (rating == ReviewRatingLike.Easy) 0.15 else 0.0
                val newEase = (currentEase + easeDelta).coerceIn(1.3, 5.0)
                var interval = if (currentInterval <= 0.0) {
                    params.graduatingIntervalDays
                } else {
                    currentInterval * newEase * bonus * params.intervalModifier
                }
                interval = interval.coerceIn(1.0, params.maximumIntervalDays)
                return SrsScheduleResult(
                    status = if (currentStatus == SrsStatusLike.Relearning) SrsSchedulingStatus.Review else SrsSchedulingStatus.Review,
                    intervalDays = interval,
                    ease = newEase,
                    dueAt = now + intervalToDuration(interval)
                )
            }
        }
    }

    private fun rescheduleFailed(
        currentStatus: SrsStatusLike,
        currentInterval: Double,
        currentEase: Double,
        lapses: Int,
        learningSteps: Int,
        now: Instant,
        params: SrsParameters
    ): SrsScheduleResult {
        if (currentStatus == SrsStatusLike.New) {
            val stepMinutes = params.learningStepsMinutes.firstOrNull() ?: 1
            return SrsScheduleResult(
                status = SrsSchedulingStatus.Learning,
                intervalDays = stepMinutes / 1440.0,
                ease = params.newCardInitialEase,
                dueAt = now + stepMinutes.minutes,
                stepIndex = 0
            )
        }

        // A mature card that fails returns to relearning with a shortened interval.
        val relearnMinutes = params.relearningStepsMinutes.firstOrNull() ?: 10
        val newEase = (currentEase - 0.2).coerceAtLeast(1.3)
        val interval = (currentInterval * params.lapseMultiplier).coerceAtLeast(0.1)
        return SrsScheduleResult(
            status = SrsSchedulingStatus.Relearning,
            intervalDays = interval,
            ease = newEase,
            dueAt = now + relearnMinutes.minutes,
            stepIndex = 0
        )
    }

    private fun intervalToDuration(intervalDays: Double): Duration {
        val wholeDays = intervalDays.toInt()
        val fraction = intervalDays - wholeDays
        val minutes = (fraction * 1440.0).roundToInt()
        return wholeDays.days + minutes.minutes
    }

    /**
     * Predicted retention for a card given its interval and ease.
     * Lower ease => faster forgetting; higher retention for shorter intervals.
     */
    fun predictedRetention(intervalDays: Double, ease: Double): Double {
        if (intervalDays <= 0.0) return 0.9
        val decay = 1.0 - (ease - 1.3) * 0.1
        val retention = 0.9 * (intervalDays.pow(-decay))
        return retention.coerceIn(0.0, 1.0)
    }
}

/**
 * Protocol-style status/rating enums so the scheduler stays decoupled
 * from the presentation enums and stays trivially testable.
 */
enum class SrsStatusLike { New, Learning, Review, Relearning }
enum class ReviewRatingLike { Again, Hard, Good, Easy }

/** Convenience bridge from the shared model. */
/** Convert a (possibly fractional) interval in days to a [Duration]. */
fun intervalDaysToDuration(days: Double): Duration {
    val wholeDays = days.toInt()
    val fraction = days - wholeDays
    val minutes = (fraction * 1440.0).roundToInt()
    return wholeDays.days + minutes.minutes
}

fun ua.syt0r.kanji.desktop.model.ReviewRating.toLike(): ReviewRatingLike = when (this) {
    ua.syt0r.kanji.desktop.model.ReviewRating.Again -> ReviewRatingLike.Again
    ua.syt0r.kanji.desktop.model.ReviewRating.Hard -> ReviewRatingLike.Hard
    ua.syt0r.kanji.desktop.model.ReviewRating.Good -> ReviewRatingLike.Good
    ua.syt0r.kanji.desktop.model.ReviewRating.Easy -> ReviewRatingLike.Easy
}

fun ua.syt0r.kanji.desktop.model.SrsStatus.toLike(): SrsStatusLike = when (this) {
    ua.syt0r.kanji.desktop.model.SrsStatus.New -> SrsStatusLike.New
    ua.syt0r.kanji.desktop.model.SrsStatus.Learning -> SrsStatusLike.Learning
    ua.syt0r.kanji.desktop.model.SrsStatus.Review,
    ua.syt0r.kanji.desktop.model.SrsStatus.Buried -> SrsStatusLike.Review
    ua.syt0r.kanji.desktop.model.SrsStatus.Relearning -> SrsStatusLike.Relearning
    ua.syt0r.kanji.desktop.model.SrsStatus.Suspended -> SrsStatusLike.Review
}
