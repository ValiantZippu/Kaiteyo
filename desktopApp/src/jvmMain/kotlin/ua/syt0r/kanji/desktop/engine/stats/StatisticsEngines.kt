package ua.syt0r.kanji.desktop.engine.stats

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.desktop.engine.collections.CollectionStore
import ua.syt0r.kanji.desktop.engine.history.ActivityEntry
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewLogEntry
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.StudyDaySummary
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// ============================================
// STATISTICS ENGINES
// Heatmaps (GitHub-style), learning curves,
// retention, goals, and weak-spot analysis.
// All pure functions over study summaries & cards.
// ============================================

/** A single day cell on the heatmap. */
data class HeatmapCell(
    val date: LocalDate,
    val count: Int,
    val level: Int // 0..4
)

data class HeatmapMonth(
    val label: String,
    val weeks: List<List<HeatmapCell?>>
)

object HeatmapEngine {

    /**
     * Build a 52-week GitHub-style heatmap ending at [today].
     * Days with zero activity appear as empty cells.
     */
    fun build(
        summaries: List<StudyDaySummary>,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        weeks: Int = 52
    ): List<HeatmapMonth> {
        val byDay = summaries.associateBy { it.day }
        val counts = summaries.associate { it.day to (it.newCount + it.reviewCount) }
        val maxCount = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)

        val start = today.minus((weeks * 7 - 1), kotlinx.datetime.DateTimeUnit.DAY)
        val cells = mutableListOf<HeatmapCell?>()
        var cursor = start
        repeat(weeks * 7) {
            val key = cursor.toString()
            val count = counts[key] ?: 0
            cells.add(
                if (count == 0 && key !in byDay) null
                else HeatmapCell(cursor, count, levelFor(count, maxCount))
            )
            cursor = cursor.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
        }

        // Group into weeks (columns), then months (rows of weeks).
        val weekColumns = cells.chunked(7)
        return groupByMonth(weekColumns)
    }

    fun levelFor(count: Int, maxCount: Int): Int = when {
        count <= 0 -> 0
        maxCount == 0 -> 0
        else -> when {
            count > maxCount * 0.75 -> 4
            count > maxCount * 0.5 -> 3
            count > maxCount * 0.25 -> 2
            else -> 1
        }
    }

    private fun groupByMonth(weekColumns: List<List<HeatmapCell?>>): List<HeatmapMonth> {
        val months = LinkedHashMap<String, MutableList<List<HeatmapCell?>>>()
        weekColumns.forEach { week ->
            val firstCell = week.firstOrNull { it != null }
            val label = firstCell?.date?.monthNumber?.toString()?.padStart(2, '0') ?: "??"
            months.getOrPut(label) { mutableListOf() }.add(week)
        }
        return months.map { (label, weeks) -> HeatmapMonth(label, weeks) }
    }

    // ---------------------------------------------------------------
    // Weekday-aligned builder for the interactive heatmap panel.
    // Columns are weeks (Mon..Sun), rows are weekdays.
    // ---------------------------------------------------------------

    data class AlignedDay(val date: LocalDate, val count: Int, val level: Int)
    data class AlignedWeek(val days: List<AlignedDay?>, val monthLabel: String)
    data class AlignedGrid(val weeks: List<AlignedWeek>)

    /**
     * Build a weekday-aligned grid ending at [today]. Every column is a
     * week with exactly seven slots aligned to Monday. The first column
     * is left-padded so the calendar lines up with weekday labels.
     */
    fun buildAligned(
        summaries: List<StudyDaySummary>,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        weeks: Int = 52
    ): AlignedGrid {
        val counts = summaries.associate { it.day to (it.newCount + it.reviewCount) }
        val maxCount = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)
        val start = today.minus((weeks * 7 - 1).toLong(), DateTimeUnit.DAY)
        return buildAlignedFrom(counts, maxCount, start, weeks * 7)
    }

    /**
     * Build a full calendar-year grid ([year]-01-01 … [year]-12-31, or the
     * current day when [year] is this year). Used by the heatmap's year
     * view so switching years animates as a push/slide between two real
     * calendars rather than one grid magically changing in place.
     */
    fun buildAlignedYear(
        summaries: List<StudyDaySummary>,
        year: Int,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): AlignedGrid {
        val counts = summaries.associate { it.day to (it.newCount + it.reviewCount) }
        val maxCount = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)
        val start = LocalDate(year, kotlinx.datetime.Month.JANUARY, 1)
        val end = if (today.year == year) today else LocalDate(year, kotlinx.datetime.Month.DECEMBER, 31)
        val totalDays = start.daysUntil(end) + 1
        return buildAlignedFrom(counts, maxCount, start, totalDays)
    }

    private fun buildAlignedFrom(
        counts: Map<String, Int>,
        maxCount: Int,
        start: LocalDate,
        totalDays: Int
    ): AlignedGrid {
        val days = mutableListOf<AlignedDay?>()
        var cursor = start
        repeat(totalDays) {
            val key = cursor.toString()
            val count = counts[key] ?: 0
            days.add(AlignedDay(cursor, count, levelFor(count, maxCount)))
            cursor = cursor.plus(1, DateTimeUnit.DAY)
        }

        val leadPad = start.dayOfWeek.ordinal
        val padded = List(leadPad) { null as AlignedDay? } + days
        val weekColumns = padded.chunked(7).mapNotNull { week ->
            if (week.isEmpty()) null else {
                val firstDay = week.firstNotNullOfOrNull { it }
                AlignedWeek(
                    days = week,
                    monthLabel = firstDay?.date?.month?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""
                )
            }
        }
        return AlignedGrid(weekColumns)
    }

    /** Total streak (consecutive days with ≥1 activity). */
    fun currentStreak(summaries: List<StudyDaySummary>, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Int {
        val active = summaries.map { it.day }.toSet()
        var streak = 0
        var cursor = today
        while (active.contains(cursor.toString())) {
            streak++
            cursor = cursor.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        }
        return streak
    }
}

// ============================================
// LEARNING CURVE / RETENTION
// ============================================

data class LearningCurvePoint(val day: Int, val accuracy: Float, val reviews: Int, val timeSpent: Duration)

object LearningCurveEngine {

    /** Day-by-day accuracy/review curve, oldest first. */
    fun build(summaries: List<StudyDaySummary>): List<LearningCurvePoint> =
        summaries.sortedBy { it.day }.mapIndexed { index, s ->
            LearningCurvePoint(index, s.accuracy, s.reviewCount + s.newCount, s.timeSpent)
        }

    /** Rolling average accuracy over a window of days. */
    fun rollingAccuracy(points: List<LearningCurvePoint>, window: Int = 7): List<Float> {
        if (points.isEmpty()) return emptyList()
        return points.indices.map { i ->
            val lo = (i - window + 1).coerceAtLeast(0)
            val slice = points.subList(lo, i + 1)
            slice.map { it.accuracy }.average().toFloat()
        }
    }

    /** Overall retention: correct / total across the window. */
    fun retention(summaries: List<StudyDaySummary>): Float {
        val total = summaries.sumOf { it.correctCount + it.wrongCount }
        if (total == 0) return 0f
        return summaries.sumOf { it.correctCount }.toFloat() / total
    }

    /** Average time spent per review. */
    fun avgTimePerReview(summaries: List<StudyDaySummary>): Duration {
        val total = summaries.sumOf { it.newCount + it.reviewCount }
        if (total == 0) return Duration.ZERO
        return summaries.fold(Duration.ZERO) { acc, s -> acc + s.timeSpent } / total
    }
}

// ============================================
// GOALS ENGINE
// Daily/weekly/monthly/yearly progress tracking.
// ============================================

enum class GoalPeriod { Daily, Weekly, Monthly, Yearly }

data class GoalDef(
    val id: String,
    val name: String,
    val period: GoalPeriod,
    val target: Int,
    val metric: GoalMetric
)

enum class GoalMetric { Reviews, NewCards, Minutes }

data class GoalProgress(
    val goal: GoalDef,
    val achieved: Int,
    val target: Int,
    val fraction: Float,
    val complete: Boolean
)

object GoalsEngine {

    fun periodWindow(period: GoalPeriod, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Pair<LocalDate, LocalDate> {
        return when (period) {
            GoalPeriod.Daily -> today to today
            GoalPeriod.Weekly -> {
                val monday = today.minus((today.dayOfWeek.ordinal).toLong(), kotlinx.datetime.DateTimeUnit.DAY)
                monday to monday.plus(6, kotlinx.datetime.DateTimeUnit.DAY)
            }
            GoalPeriod.Monthly -> {
                val first = LocalDate(today.year, today.month, 1)
                first to LocalDate(today.year, today.month, today.month.length(java.time.Year.isLeap(today.year.toLong())))
            }
            GoalPeriod.Yearly -> {
                LocalDate(today.year, 1, 1) to LocalDate(today.year, 12, 31)
            }
        }
    }

    fun progress(goal: GoalDef, summaries: List<StudyDaySummary>, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): GoalProgress {
        val (start, end) = periodWindow(goal.period, today)
        val inWindow = summaries.filter { s ->
            // Safe parse — a corrupt/legacy day string must never crash the
            // dashboard; it is skipped like everywhere else (see AnalyticsEngine.parseDate).
            val date = AnalyticsEngine.parseDate(s.day) ?: return@filter false
            date >= start && date <= end
        }
        val achieved = when (goal.metric) {
            GoalMetric.Reviews -> inWindow.sumOf { it.reviewCount }
            GoalMetric.NewCards -> inWindow.sumOf { it.newCount }
            GoalMetric.Minutes -> (inWindow.sumOf { it.timeSpent.inWholeMinutes }).toInt()
        }
        val fraction = if (goal.target <= 0) 0f else (achieved.toFloat() / goal.target).coerceIn(0f, 1f)
        return GoalProgress(goal, achieved, goal.target, fraction, achieved >= goal.target)
    }

    /**
     * The built-in goal set. The daily review goal is the user's configurable
     * study target (Settings → Statistics → Daily review target) so the
     * Dashboard study target and the Goals card never disagree.
     */
    fun defaultGoals(dailyReviewTarget: Int = 20): List<GoalDef> = listOf(
        GoalDef("goal-daily-reviews", "Daily reviews", GoalPeriod.Daily, dailyReviewTarget, GoalMetric.Reviews),
        GoalDef("goal-daily-new", "Daily new cards", GoalPeriod.Daily, 5, GoalMetric.NewCards),
        GoalDef("goal-daily-minutes", "Daily study time", GoalPeriod.Daily, 15, GoalMetric.Minutes),
        GoalDef("goal-weekly-reviews", "Weekly reviews", GoalPeriod.Weekly, 150, GoalMetric.Reviews),
        GoalDef("goal-monthly-reviews", "Monthly reviews", GoalPeriod.Monthly, 600, GoalMetric.Reviews),
        GoalDef("goal-yearly-reviews", "Yearly reviews", GoalPeriod.Yearly, 8000, GoalMetric.Reviews)
    )
}

// ============================================
// WEAK SPOT ANALYSIS
// Most difficult cards, weakest radicals, weakest
// readings. Drives targeted study recommendations.
// ============================================

data class WeakRadical(val radical: String, val accuracy: Float, val sample: Int)
data class WeakReading(val reading: String, val accuracy: Float, val sample: Int)

object WeakSpotEngine {

    /**
     * Rank the most difficult cards (lowest accuracy, high lapse weight).
     */
    fun mostDifficult(cards: List<ua.syt0r.kanji.desktop.model.DesktopCard>, limit: Int = 10): List<ua.syt0r.kanji.desktop.model.DesktopCard> {
        return cards
            .filter { it.reps >= 3 }
            .sortedWith(compareBy({ it.accuracy }, { -it.lapses }, { it.reps }))
            .take(limit)
    }

    /**
     * Aggregate accuracy by radical. Radicals are matched by substring
     * against each card's radical list.
     */
    fun weakestRadicals(cards: List<ua.syt0r.kanji.desktop.model.DesktopCard>, limit: Int = 8): List<WeakRadical> {
        val acc = mutableMapOf<String, MutableList<Float>>()
        cards.forEach { card ->
            card.radicals.forEach { r ->
                acc.getOrPut(r) { mutableListOf() }.add(card.accuracy)
            }
        }
        return acc.entries
            .map { (radical, accs) -> WeakRadical(radical, accs.average().toFloat(), accs.size) }
            .filter { it.sample >= 2 }
            .sortedBy { it.accuracy }
            .take(limit)
    }

    /** Aggregate accuracy by reading (onyomi/kunyomi). */
    fun weakestReadings(cards: List<ua.syt0r.kanji.desktop.model.DesktopCard>, limit: Int = 8): List<WeakReading> {
        val acc = mutableMapOf<String, MutableList<Float>>()
        cards.forEach { card ->
            card.readings.forEach { r ->
                acc.getOrPut(r) { mutableListOf() }.add(card.accuracy)
            }
        }
        return acc.entries
            .map { (reading, accs) -> WeakReading(reading, accs.average().toFloat(), accs.size) }
            .filter { it.sample >= 2 }
            .sortedBy { it.accuracy }
            .take(limit)
    }

    /** Cards lapsed 3+ times (frequently failed). */
    fun frequentlyFailed(cards: List<ua.syt0r.kanji.desktop.model.DesktopCard>, minLapses: Int = 3): List<ua.syt0r.kanji.desktop.model.DesktopCard> =
        cards.filter { it.lapses >= minLapses }.sortedByDescending { it.lapses }
}

// ============================================
// PERIOD ANALYTICS
// KPI snapshots for today / week / month / year
// and the previous window for delta comparison.
// ============================================

enum class StatsPeriod(val label: String) {
    Today("Today"),
    Week("Week"),
    Month("Month"),
    Year("Year"),
    All("All time")
}

data class AnalyticsSnapshot(
    val reviews: Int = 0,
    val newCards: Int = 0,
    val correct: Int = 0,
    val forgotten: Int = 0,
    val studyTime: Duration = Duration.ZERO,
    val activeDays: Int = 0
) {
    val answered: Int get() = correct + forgotten
    val accuracy: Float get() = if (answered == 0) 0f else correct.toFloat() / answered
    val avgReviewDuration: Duration
        get() = if (reviews + newCards == 0) Duration.ZERO else studyTime / (reviews + newCards)
    val learningSpeed: Float get() = if (activeDays == 0) 0f else newCards.toFloat() / activeDays
}

object AnalyticsEngine {

    fun monthLength(year: Int, month: kotlinx.datetime.Month): Int =
        month.length(java.time.Year.isLeap(year.toLong()))

    fun parseDate(day: String): LocalDate? {
        val parts = day.split('-')
        if (parts.size != 3) return null
        return runCatching { LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt()) }.getOrNull()
    }

    fun range(period: StatsPeriod, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Pair<LocalDate, LocalDate>? =
        when (period) {
            StatsPeriod.Today -> today to today
            StatsPeriod.Week -> {
                val monday = today.minus(today.dayOfWeek.ordinal.toLong(), DateTimeUnit.DAY)
                monday to monday.plus(6, DateTimeUnit.DAY)
            }
            StatsPeriod.Month -> {
                val first = LocalDate(today.year, today.month, 1)
                first to LocalDate(today.year, today.month, monthLength(today.year, today.month))
            }
            StatsPeriod.Year -> LocalDate(today.year, 1, 1) to LocalDate(today.year, 12, 31)
            StatsPeriod.All -> null
        }

    /** The window of equal length immediately before [period]. */
    fun previousRange(period: StatsPeriod, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Pair<LocalDate, LocalDate>? =
        when (period) {
            StatsPeriod.Today -> today.minus(1, DateTimeUnit.DAY) to today.minus(1, DateTimeUnit.DAY)
            StatsPeriod.Week -> {
                val end = today.minus(1, DateTimeUnit.DAY)
                end.minus(6, DateTimeUnit.DAY) to end
            }
            StatsPeriod.Month -> {
                val prevEnd = LocalDate(today.year, today.month, 1).minus(1, DateTimeUnit.DAY)
                LocalDate(prevEnd.year, prevEnd.month, 1) to prevEnd
            }
            StatsPeriod.Year -> LocalDate(today.year - 1, 1, 1) to LocalDate(today.year - 1, 12, 31)
            StatsPeriod.All -> null
        }

    fun snapshotInRange(summaries: List<StudyDaySummary>, start: LocalDate?, end: LocalDate?): AnalyticsSnapshot {
        val inWindow = if (start == null || end == null) summaries else summaries.filter { s ->
            val d = parseDate(s.day) ?: return@filter false
            d >= start && d <= end
        }
        return AnalyticsSnapshot(
            reviews = inWindow.sumOf { it.reviewCount },
            newCards = inWindow.sumOf { it.newCount },
            correct = inWindow.sumOf { it.correctCount },
            forgotten = inWindow.sumOf { it.wrongCount },
            studyTime = inWindow.fold(Duration.ZERO) { acc, s -> acc + s.timeSpent },
            activeDays = inWindow.size
        )
    }

    fun snapshot(
        summaries: List<StudyDaySummary>,
        period: StatsPeriod,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): AnalyticsSnapshot {
        val range = range(period, today)
        return snapshotInRange(summaries, range?.first, range?.second)
    }

    fun previousSnapshot(
        summaries: List<StudyDaySummary>,
        period: StatsPeriod,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): AnalyticsSnapshot? {
        val range = previousRange(period, today) ?: return null
        return snapshotInRange(summaries, range.first, range.second)
    }

    /** Rolling mean of per-day accuracy over a trailing window. */
    fun retention(
        summaries: List<StudyDaySummary>,
        windowDays: Int = 7,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): Float {
        val start = today.minus((windowDays - 1).toLong(), DateTimeUnit.DAY)
        val slice = summaries.filter { s ->
            val d = parseDate(s.day) ?: return@filter false
            d >= start && d <= today
        }
        val accurate = slice.filter { it.accuracy > 0f }
        if (accurate.isEmpty()) return 0f
        return accurate.map { it.accuracy }.average().toFloat()
    }
}

// ============================================
// STREAKS
// Study / learning / review streaks plus the
// best streak ever recorded.
// ============================================

object StreakEngine {

    fun activeSet(summaries: List<StudyDaySummary>): Set<String> = summaries.map { it.day }.toSet()

    fun currentStudyStreak(
        summaries: List<StudyDaySummary>,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): Int {
        val active = activeSet(summaries)
        var streak = 0
        var cursor = today
        while (active.contains(cursor.toString())) {
            streak++
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

    fun bestStudyStreak(summaries: List<StudyDaySummary>): Int {
        val days = summaries.mapNotNull { AnalyticsEngine.parseDate(it.day) }.sorted()
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        days.forEach { d ->
            run = if (prev == null || d.minus(1, DateTimeUnit.DAY) == prev) run + 1 else 1
            if (run > best) best = run
            prev = d
        }
        return best
    }

    private fun countStreak(
        summaries: List<StudyDaySummary>,
        today: LocalDate,
        predicate: (StudyDaySummary) -> Boolean
    ): Int {
        val byDay = summaries.associateBy { it.day }
        var streak = 0
        var cursor = today
        while (true) {
            val s = byDay[cursor.toString()]
            if (s == null || !predicate(s)) break
            streak++
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

    fun currentLearningStreak(
        summaries: List<StudyDaySummary>,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): Int = countStreak(summaries, today) { it.newCount > 0 }

    fun currentReviewStreak(
        summaries: List<StudyDaySummary>,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): Int = countStreak(summaries, today) { it.reviewCount > 0 }
}

// ============================================
// REVIEW DISTRIBUTION
// Again / Hard / Good / Easy counts per period
// and a daily series for charts.
// ============================================

data class RatingBreakdown(
    val again: Int = 0,
    val hard: Int = 0,
    val good: Int = 0,
    val easy: Int = 0
) {
    val total: Int get() = again + hard + good + easy
    val accuracy: Float get() = if (total == 0) 0f else (good + easy).toFloat() / total
    fun count(rating: ReviewRating): Int = when (rating) {
        ReviewRating.Again -> again
        ReviewRating.Hard -> hard
        ReviewRating.Good -> good
        ReviewRating.Easy -> easy
    }
}

data class DailyRatingSeries(val day: String, val label: String, val breakdown: RatingBreakdown)

object ReviewDistributionEngine {

    fun breakdown(
        log: List<ReviewLogEntry>,
        start: LocalDate? = null,
        end: LocalDate? = null,
        tz: TimeZone = TimeZone.currentSystemDefault()
    ): RatingBreakdown {
        var again = 0
        var hard = 0
        var good = 0
        var easy = 0
        log.forEach { e ->
            val d = e.reviewedAt.toLocalDateTime(tz).date
            if (start != null && d < start) return@forEach
            if (end != null && d > end) return@forEach
            when (e.rating) {
                ReviewRating.Again -> again++
                ReviewRating.Hard -> hard++
                ReviewRating.Good -> good++
                ReviewRating.Easy -> easy++
            }
        }
        return RatingBreakdown(again, hard, good, easy)
    }

    fun dailySeries(
        log: List<ReviewLogEntry>,
        days: Int = 14,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): List<DailyRatingSeries> {
        val tz = TimeZone.currentSystemDefault()
        val byDay = log.groupBy { it.reviewedAt.toLocalDateTime(tz).date }
        return (0 until days).map { offset ->
            val d = today.minus((days - 1 - offset).toLong(), DateTimeUnit.DAY)
            val entries = byDay[d] ?: emptyList()
            var again = 0
            var hard = 0
            var good = 0
            var easy = 0
            entries.forEach {
                when (it.rating) {
                    ReviewRating.Again -> again++
                    ReviewRating.Hard -> hard++
                    ReviewRating.Good -> good++
                    ReviewRating.Easy -> easy++
                }
            }
            DailyRatingSeries(d.toString(), d.dayOfWeek.name.lowercase().take(2), RatingBreakdown(again, hard, good, easy))
        }
    }
}

// ============================================
// REVIEW FORECAST
// Upcoming workload from due dates.
// ============================================

data class ForecastDay(val date: LocalDate, val due: Int)

object ForecastEngine {

    fun upcoming(
        cards: List<DesktopCard>,
        days: Int = 30,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): List<ForecastDay> {
        val tz = TimeZone.currentSystemDefault()
        val buckets = IntArray(days)
        cards.forEach { card ->
            val due = card.dueAt ?: return@forEach
            val d = due.toLocalDateTime(tz).date
            val index = if (d < today) 0 else (0 until days).firstOrNull { today.plus(it.toLong(), DateTimeUnit.DAY) == d }
            index?.let { buckets[it]++ }
        }
        return (0 until days).map { offset ->
            ForecastDay(today.plus(offset.toLong(), DateTimeUnit.DAY), buckets[offset])
        }
    }

    fun dueToday(cards: List<DesktopCard>, now: Instant = Clock.System.now()): Int =
        cards.count { (it.status == SrsStatus.Review || it.status == SrsStatus.Learning) && it.dueAt != null && it.dueAt <= now }

    fun dueTomorrow(cards: List<DesktopCard>, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Int =
        upcoming(cards, days = 2, today = today).getOrNull(1)?.due ?: 0

    fun dueThisWeek(cards: List<DesktopCard>, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Int =
        upcoming(cards, days = 7, today = today).sumOf { it.due }
}

// ============================================
// MILESTONES
// Derived achievements with progress towards
// each target. Never decorative — every badge is
// computed from real study activity.
// ============================================

data class Milestone(
    val id: String,
    val title: String,
    val description: String,
    val achieved: Boolean,
    val progress: Float,
    val metric: String
)

object MilestoneEngine {

    fun compute(
        cards: List<DesktopCard>,
        summaries: List<StudyDaySummary>,
        reviewLog: List<ReviewLogEntry>,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): List<Milestone> {
        val totalReviews = reviewLog.size
        val newTotal = summaries.sumOf { it.newCount }
        val studyHours = summaries.fold(Duration.ZERO) { a, s -> a + s.timeSpent }.inWholeHours.toInt()
        val streak = StreakEngine.currentStudyStreak(summaries, today)
        val bestStreak = StreakEngine.bestStudyStreak(summaries)
        val mastered = cards.count { it.status == SrsStatus.Review && it.intervalDays >= 21 }
        val tagged = cards.count { it.tags.isNotEmpty() }
        val favorites = cards.count { it.favorite }

        fun m(id: String, title: String, description: String, current: Int, target: Int): Milestone {
            val progress = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
            return Milestone(id, title, description, current >= target, progress, "$current / $target")
        }

        return listOf(
            m("m-first", "First steps", "Complete your first review", totalReviews, 1),
            m("m-100", "Century club", "Complete 100 reviews", totalReviews, 100),
            m("m-500", "Half thousand", "Complete 500 reviews", totalReviews, 500),
            m("m-1000", "Thousand reviews", "Complete 1,000 reviews", totalReviews, 1000),
            m("m-5000", "Marathoner", "Complete 5,000 reviews", totalReviews, 5000),
            m("m-new-100", "Curious learner", "Learn 100 new cards", newTotal, 100),
            m("m-new-1000", "Explorer", "Learn 1,000 new cards", newTotal, 1000),
            m("m-hours-1", "Warming up", "Study for 1 hour", studyHours, 1),
            m("m-hours-10", "Dedicated", "Study for 10 hours", studyHours, 10),
            m("m-hours-100", "Deep diver", "Study for 100 hours", studyHours, 100),
            m("m-streak-3", "Momentum", "Reach a 3-day streak", streak, 3),
            m("m-streak-7", "Weekly warrior", "Reach a 7-day streak", streak, 7),
            m("m-streak-30", "Iron will", "Reach a 30-day streak", streak, 30),
            m("m-best-30", "Best 30-day streak", "Your best streak reached 30 days", bestStreak, 30),
            m("m-master-50", "First masters", "Master 50 cards (21d+ interval)", mastered, 50),
            m("m-master-200", "Sensei", "Master 200 cards", mastered, 200),
            m("m-tags-10", "Organizer", "Tag 10 cards", tagged, 10),
            m("m-fav-10", "Curator", "Favorite 10 cards", favorites, 10)
        )
    }
}

// ============================================
// BREAKDOWNS
// Status / deck / tag / flag / collection stats.
// ============================================

data class BreakdownRow(
    val key: String,
    val count: Int,
    val due: Int,
    val accuracy: Float,
    val avgInterval: Double
)

object BreakdownEngine {

    private fun rowsFor(groups: Map<String, List<DesktopCard>>): List<BreakdownRow> =
        groups.map { (key, cards) ->
            BreakdownRow(
                key = key,
                count = cards.size,
                due = cards.count { it.status != SrsStatus.New && it.dueAt != null && it.dueAt <= Clock.System.now() },
                accuracy = if (cards.isEmpty()) 0f else cards.map { it.accuracy }.average().toFloat(),
                avgInterval = if (cards.isEmpty()) 0.0 else cards.map { it.intervalDays }.average()
            )
        }.sortedByDescending { it.count }

    fun byStatus(cards: List<DesktopCard>): List<BreakdownRow> = rowsFor(cards.groupBy { it.status.name })

    fun byDeck(cards: List<DesktopCard>): List<BreakdownRow> = rowsFor(cards.groupBy { it.deckId })

    fun byTag(cards: List<DesktopCard>): List<BreakdownRow> =
        cards.flatMap { c -> c.tags.map { it to c } }
            .groupBy { it.first }
            .mapValues { (_, pairs) -> pairs.map { it.second } }
            .let { rowsFor(it) }

    fun byFlag(cards: List<DesktopCard>): List<BreakdownRow> =
        cards.flatMap { c -> c.flags.map { it to c } }
            .groupBy { it.first }
            .mapValues { (_, pairs) -> pairs.map { it.second } }
            .let { rowsFor(it) }

    fun byCollection(collections: CollectionStore, cards: List<DesktopCard>): List<BreakdownRow> =
        collections.collections.map { def ->
            val resolved = collections.resolveCards(def, cards)
            BreakdownRow(
                key = def.name,
                count = resolved.size,
                due = resolved.count { it.status != SrsStatus.New && it.dueAt != null && it.dueAt <= Clock.System.now() },
                accuracy = if (resolved.isEmpty()) 0f else resolved.map { it.accuracy }.average().toFloat(),
                avgInterval = if (resolved.isEmpty()) 0.0 else resolved.map { it.intervalDays }.average()
            )
        }.sortedByDescending { it.count }

    /** Most recently studied tags sorted by last review recency. */
    fun byCollectionTop(collections: CollectionStore, cards: List<DesktopCard>, limit: Int = 6): List<BreakdownRow> =
        byCollection(collections, cards).take(limit)
}

// ============================================
// CARD INSIGHTS
// Strongest / weakest / recently changed cards
// and per-deck difficulty.
// ============================================

object CardInsightEngine {

    fun strongest(cards: List<DesktopCard>, limit: Int = 8): List<DesktopCard> =
        cards.filter { it.reps >= 3 }
            .sortedWith(compareByDescending<DesktopCard> { it.accuracy }.thenByDescending { it.reps }.thenBy { it.lapses })
            .take(limit)

    fun weakest(cards: List<DesktopCard>, limit: Int = 8): List<DesktopCard> =
        cards.filter { it.reps >= 3 }
            .sortedWith(compareBy<DesktopCard> { it.accuracy }.thenByDescending { it.lapses })
            .take(limit)

    private fun latestReview(log: List<ReviewLogEntry>): Map<String, ReviewLogEntry?> =
        log.groupBy { it.cardId }.mapValues { it.value.maxByOrNull { e -> e.reviewedAt } }

    /** Cards whose most recent review was a success that raised the interval. */
    fun recentlyImproved(cards: List<DesktopCard>, reviewLog: List<ReviewLogEntry>, limit: Int = 8): List<DesktopCard> {
        val latest = latestReview(reviewLog)
        return cards.mapNotNull { card ->
            val last = latest[card.id] ?: return@mapNotNull null
            if (last.rating != ReviewRating.Again && last.intervalAfter > last.intervalBefore) card else null
        }.take(limit)
    }

    /** Cards whose most recent review was "Again". */
    fun recentlyForgotten(cards: List<DesktopCard>, reviewLog: List<ReviewLogEntry>, limit: Int = 8): List<DesktopCard> {
        val latest = latestReview(reviewLog)
        return cards.mapNotNull { card ->
            val last = latest[card.id] ?: return@mapNotNull null
            if (last.rating == ReviewRating.Again) card else null
        }.take(limit)
    }

    /** Decks ranked by lowest aggregated accuracy. */
    fun difficultDecks(cards: List<DesktopCard>, limit: Int = 6): List<Pair<String, Float>> =
        cards.filter { it.reps >= 3 }.groupBy { it.deckId }
            .map { (deck, cs) -> deck to cs.map { it.accuracy }.average().toFloat() }
            .sortedBy { it.second }
            .take(limit)

    /** Cards that are due now, sorted by how overdue they are. */
    fun dueSoonest(cards: List<DesktopCard>, now: Instant = Clock.System.now(), limit: Int = 8): List<DesktopCard> =
        cards.filter { it.status != SrsStatus.New && it.dueAt != null }
            .sortedBy { it.dueAt }
            .take(limit)
}

// ============================================
// DAY ACTIVITY
// Everything about one calendar day — used by the
// heatmap tooltip and the day-detail dialog.
// ============================================

data class DayActivity(
    val date: LocalDate,
    val reviews: Int,
    val newCards: Int,
    val correct: Int,
    val forgotten: Int,
    val timeSpent: Duration,
    val rating: RatingBreakdown,
    val cards: List<DesktopCard>,
    val decks: List<String>,
    val activityEntries: List<ActivityEntry>
) {
    val accuracy: Float get() = if (correct + forgotten == 0) 0f else correct.toFloat() / (correct + forgotten)
}

object DayActivityEngine {

    fun forDay(
        date: LocalDate,
        summaries: List<StudyDaySummary>,
        reviewLog: List<ReviewLogEntry>,
        cards: List<DesktopCard>,
        activityLog: ua.syt0r.kanji.desktop.engine.history.ActivityLog
    ): DayActivity {
        val key = date.toString()
        val summary = summaries.firstOrNull { it.day == key }
        val tz = TimeZone.currentSystemDefault()
        val dayLog = reviewLog.filter { it.reviewedAt.toLocalDateTime(tz).date == date }
        val reviewedIds = dayLog.map { it.cardId }.toSet()
        val dayCards = cards.filter { it.id in reviewedIds }
        return DayActivity(
            date = date,
            reviews = summary?.reviewCount ?: dayLog.size,
            newCards = summary?.newCount ?: dayLog.count { it.wasNew },
            correct = summary?.correctCount ?: dayLog.count { it.rating != ReviewRating.Again },
            forgotten = summary?.wrongCount ?: dayLog.count { it.rating == ReviewRating.Again },
            timeSpent = summary?.timeSpent ?: Duration.ZERO,
            rating = ReviewDistributionEngine.breakdown(dayLog),
            cards = dayCards.sortedBy { it.character },
            decks = dayCards.map { it.deckId }.distinct(),
            activityEntries = activityLog.byDate(key)
        )
    }
}
