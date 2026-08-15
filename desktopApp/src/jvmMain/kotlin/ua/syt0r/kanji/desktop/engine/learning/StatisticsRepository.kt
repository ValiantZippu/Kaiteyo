package ua.syt0r.kanji.desktop.engine.learning

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import ua.syt0r.kanji.desktop.model.ReviewRating
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// ============================================
// STATISTICS REPOSITORY
// The single statistics pipeline:
//
//   ReviewEvent / WritingAttempt / ExamResult
//                ↓
//        aggregations (pure functions)
//                ↓
//              UI
//
// Nothing here reads "current card state" for a
// statistic that should reflect history. Totals
// come from events, and every number can be traced
// back to individual events (drill-down).
// ============================================

data class PeriodStats(
    val reviews: Int = 0,
    val newCards: Int = 0,
    val correct: Int = 0,
    val again: Int = 0,
    val studyTimeMs: Long = 0,
    val activeDays: Int = 0,
    val writingAttempts: Int = 0,
    val writingCorrect: Int = 0,
    val exams: Int = 0,
    val examQuestions: Int = 0,
    val examCorrect: Int = 0
) {
    val answered: Int get() = correct + again
    val accuracy: Float get() = if (answered == 0) 0f else correct.toFloat() / answered
    val writingAccuracy: Float get() = if (writingAttempts == 0) 0f else writingCorrect.toFloat() / writingAttempts
    val examAccuracy: Float get() = if (examQuestions == 0) 0f else examCorrect.toFloat() / examQuestions
}

data class StreakInfo(
    val current: Int = 0,
    val longest: Int = 0
)

data class JlptCoverage(
    val level: Int,
    val total: Int,
    val known: Int,
    val learning: Int,
    val unseen: Int,
    val due: Int
) {
    val introducedFraction: Float get() = if (total == 0) 0f else (known + learning).toFloat() / total
    val establishedFraction: Float get() = if (total == 0) 0f else known.toFloat() / total
}

data class CharacterProgress(
    val uniqueKanji: Int = 0,
    val uniqueKanjiStudied: Int = 0,
    val uniqueKanjiEstablished: Int = 0,
    val uniqueVocabulary: Int = 0,
    val uniqueVocabularyEstablished: Int = 0,
    val totalWritingAttempts: Int = 0,
    val successfulWritingAttempts: Int = 0
)

data class WritingStatRow(
    val expression: String,
    val attempts: Int,
    val correct: Int,
    val accuracy: Float,
    val kind: LearningItemKind
)

data class CardHistoryEntry(
    val event: LearningReviewEvent,
    val noteExpression: String,
    val cardTypeLabel: String
)

data class ExamStatRow(
    val result: ExamResult
)

data class StudyVsExamGap(
    val studyAccuracy: Float,
    val examRecognitionAccuracy: Float,
    val examProductionAccuracy: Float,
    val examWritingAccuracy: Float
)

data class ForecastPoint(
    val date: LocalDate,
    val due: Int
)

object StatisticsRepository {

    private fun tz(): TimeZone = TimeZone.currentSystemDefault()

    private fun LocalDate.key(): String = toString()

    // ------------------------------------------------------------
    // Period helpers
    // ------------------------------------------------------------
    private fun dayKey(instant: Instant): String = instant.toLocalDateTime(tz()).date.toString()

    private fun periodRange(period: StatsPeriod, today: LocalDate): Pair<LocalDate, LocalDate>? = when (period) {
        StatsPeriod.Today -> today to today
        StatsPeriod.Week -> {
            val monday = today.minus(today.dayOfWeek.ordinal.toLong(), DateTimeUnit.DAY)
            monday to monday.plus(6, DateTimeUnit.DAY)
        }
        StatsPeriod.Month -> {
            val first = LocalDate(today.year, today.month, 1)
            first to LocalDate(today.year, today.month, today.month.length(java.time.Year.isLeap(today.year.toLong())))
        }
        StatsPeriod.Year -> LocalDate(today.year, 1, 1) to LocalDate(today.year, 12, 31)
        StatsPeriod.All -> null
    }

    private fun inRange(instant: Instant, range: Pair<LocalDate, LocalDate>?): Boolean {
        if (range == null) return true
        val d = instant.toLocalDateTime(tz()).date
        return d >= range.first && d <= range.second
    }

    // ------------------------------------------------------------
    // Period snapshot (from events, not summaries)
    // ------------------------------------------------------------
    fun periodStats(
        store: LearningStore,
        period: StatsPeriod,
        today: LocalDate = Clock.System.todayIn(tz())
    ): PeriodStats {
        val range = periodRange(period, today)
        val reviews = store.reviewEvents.filter { inRange(it.reviewedAt, range) }
        val writing = store.writingAttempts.filter { inRange(it.attemptedAt, range) }
        val exams = store.examResults.filter { inRange(it.finishedAt, range) }

        val newCards = reviews.count { it.wasNew }
        val correct = reviews.count { it.correct }
        val again = reviews.count { it.rating == ReviewRating.Again }
        val studyTime = reviews.sumOf { it.responseTimeMs } + writing.sumOf { it.durationMs }
        val activeDays = (reviews.map { dayKey(it.reviewedAt) } + writing.map { dayKey(it.attemptedAt) } + exams.map { dayKey(it.finishedAt) }).distinct().size

        return PeriodStats(
            reviews = reviews.size,
            newCards = newCards,
            correct = correct,
            again = again,
            studyTimeMs = studyTime,
            activeDays = activeDays,
            writingAttempts = writing.size,
            writingCorrect = writing.count { it.correct },
            exams = exams.size,
            examQuestions = exams.sumOf { it.questionCount },
            examCorrect = exams.sumOf { it.correctCount }
        )
    }

    /** Full-history totals. */
    fun lifetime(store: LearningStore): PeriodStats = periodStats(store, StatsPeriod.All)

    // ------------------------------------------------------------
    // Streaks — based on actual learning activity days
    // ------------------------------------------------------------
    fun streaks(store: LearningStore, today: LocalDate = Clock.System.todayIn(tz())): StreakInfo {
        val activeDays = (
            store.reviewEvents.map { dayKey(it.reviewedAt) } +
                store.writingAttempts.map { dayKey(it.attemptedAt) } +
                store.examResults.map { dayKey(it.finishedAt) }
            ).toSet()
        var current = 0
        var cursor = today
        while (activeDays.contains(cursor.key())) {
            current++
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }
        val sorted = activeDays.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
        var longest = 0
        var run = 0
        var prev: LocalDate? = null
        sorted.forEach { d ->
            run = if (prev == null || d.minus(1, DateTimeUnit.DAY) == prev) run + 1 else 1
            if (run > longest) longest = run
            prev = d
        }
        return StreakInfo(current, longest)
    }

    // ------------------------------------------------------------
    // JLPT coverage — from real notes + card stages
    // ------------------------------------------------------------
    fun jlptCoverage(store: LearningStore): List<JlptCoverage> {
        val byLevel = store.notes.filter { it.jlpt != null && it.jlpt in 1..5 }.groupBy { it.jlpt!! }
        return (1..5).map { level ->
            val notes = byLevel[level].orEmpty()
            val noteIds = notes.map { it.id }.toSet()
            val cardsForLevel = store.cards.filter { it.noteId in noteIds }
            JlptCoverage(
                level = level,
                total = notes.size,
                known = cardsForLevel.count { it.stage == LearningStage.Established || it.stage == LearningStage.Mature },
                learning = cardsForLevel.count { it.stage == LearningStage.Learning },
                unseen = cardsForLevel.count { it.stage == LearningStage.Introduced },
                due = cardsForLevel.count { !it.isSuspended && !it.buried && !it.isNew && it.isDue }
            )
        }.sortedByDescending { it.level }
    }

    // ------------------------------------------------------------
    // Character progress
    // ------------------------------------------------------------
    fun characterProgress(store: LearningStore): CharacterProgress {
        val kanjiNotes = store.notes.filter { it.kind == LearningItemKind.Kanji }
        val vocabNotes = store.notes.filter { it.kind == LearningItemKind.Vocabulary }
        val studiedKanjiIds = store.reviewEvents.filter { it.activityType != StudyActivityType.Exam }
            .map { it.noteId }.toSet()
        val kanjiCards = store.cards.filter { it.noteId in kanjiNotes.map { n -> n.id } }
        val vocabCards = store.cards.filter { it.noteId in vocabNotes.map { n -> n.id } }
        return CharacterProgress(
            uniqueKanji = kanjiNotes.size,
            uniqueKanjiStudied = kanjiCards.count { it.reps > 0 },
            uniqueKanjiEstablished = kanjiCards.count { it.stage == LearningStage.Established || it.stage == LearningStage.Mature },
            uniqueVocabulary = vocabNotes.size,
            uniqueVocabularyEstablished = vocabCards.count { it.stage == LearningStage.Established || it.stage == LearningStage.Mature },
            totalWritingAttempts = store.writingAttempts.size,
            successfulWritingAttempts = store.writingAttempts.count { it.correct }
        )
    }

    // ------------------------------------------------------------
    // Writing statistics — weakest characters from real attempts
    // ------------------------------------------------------------
    fun writingStats(store: LearningStore, limit: Int = 10): List<WritingStatRow> {
        val byExpression = store.writingAttempts.groupBy { it.expected }
        return byExpression.map { (expression, attempts) ->
            val correct = attempts.count { it.correct }
            WritingStatRow(
                expression = expression,
                attempts = attempts.size,
                correct = correct,
                accuracy = correct.toFloat() / attempts.size,
                kind = attempts.first().let { store.note(it.noteId)?.kind ?: LearningItemKind.Custom }
            )
        }.filter { it.attempts >= 2 }.sortedBy { it.accuracy }.take(limit)
    }

    /** Writing accuracy per kanji (for the weakest-kanji dashboard). */
    fun weakestKanji(store: LearningStore, limit: Int = 8): List<WritingStatRow> =
        writingStats(store, limit).filter { it.kind == LearningItemKind.Kanji }

    // ------------------------------------------------------------
    // Card history (drill-down)
    // ------------------------------------------------------------
    fun cardHistory(store: LearningStore, cardId: String, limit: Int = 100): List<CardHistoryEntry> {
        val note = store.cards.firstOrNull { it.id == cardId }?.noteId?.let { store.note(it) }
        return store.reviewEvents.filter { it.cardId == cardId }
            .sortedByDescending { it.reviewedAt }
            .take(limit)
            .map { e ->
                CardHistoryEntry(
                    event = e,
                    noteExpression = note?.expression ?: "",
                    cardTypeLabel = e.cardType.label
                )
            }
    }

    // ------------------------------------------------------------
    // Exam analytics
    // ------------------------------------------------------------
    fun examHistory(store: LearningStore, limit: Int = 30): List<ExamResult> =
        store.examResults.sortedByDescending { it.finishedAt }.take(limit)

    fun examAggregates(store: LearningStore): ExamAggregates {
        val results = store.examResults
        if (results.isEmpty()) return ExamAggregates()
        val scores = results.map { it.percentage }
        return ExamAggregates(
            count = results.size,
            averageScore = scores.average().roundToInt(),
            bestScore = scores.maxOrNull() ?: 0,
            worstScore = scores.minOrNull() ?: 0,
            totalQuestions = results.sumOf { it.questionCount },
            averageAccuracy = results.map { it.score }.average().toFloat(),
            totalTimeMs = results.sumOf { it.durationMs }
        )
    }

    /** Accuracy by question type (recognition vs production). */
    fun accuracyByType(store: LearningStore): Map<String, Float> {
        val questions = store.examResults.flatMap { it.questions }
        return questions.groupBy { it.questionType }.mapValues { (_, qs) ->
            qs.count { it.correct }.toFloat() / qs.size
        }
    }

    /** Accuracy by JLPT band across exam questions. */
    fun accuracyByJlpt(store: LearningStore): Map<Int, Float> {
        val questions = store.examResults.flatMap { it.questions }.filter { it.jlpt != null }
        return questions.groupBy { it.jlpt!! }.mapValues { (_, qs) ->
            qs.count { it.correct }.toFloat() / qs.size
        }
    }

    /** Accuracy by exam section (Vocabulary / Grammar / Reading). */
    fun accuracyBySection(store: LearningStore): Map<String, Float> {
        val questions = store.examResults.flatMap { it.questions }.filter { it.section.isNotBlank() }
        return questions.groupBy { it.section }.mapValues { (_, qs) ->
            qs.count { it.correct }.toFloat() / qs.size
        }
    }

    /** Accuracy per exam type — drives the "weakest skill" recommendation. */
    fun accuracyByExamType(store: LearningStore): Map<String, Float> {
        val results = store.examResults
        return results.groupBy { it.examType }.mapValues { (_, rs) ->
            val total = rs.sumOf { it.questionCount }.coerceAtLeast(1)
            rs.sumOf { it.correctCount }.toFloat() / total
        }
    }

    /** Score trend for the history chart: (date label, percentage), oldest first. */
    fun examTrend(store: LearningStore, limit: Int = 30): List<Pair<String, Int>> {
        val tz = TimeZone.currentSystemDefault()
        return store.examResults
            .sortedByDescending { it.finishedAt }
            .take(limit)
            .reversed()
            .map { r -> r.finishedAt.toLocalDateTime(tz).date.toString() to r.percentage }
    }

    // ------------------------------------------------------------
    // Study vs exam gap
    // ------------------------------------------------------------
    fun studyVsExamGap(store: LearningStore): StudyVsExamGap {
        val study = store.reviewEvents.filter { it.activityType == StudyActivityType.Review }
        val studyAccuracy = if (study.isEmpty()) 0f else study.count { it.correct }.toFloat() / study.size
        var recTotal = 0
        var recCorrect = 0
        var prodTotal = 0
        var prodCorrect = 0
        var writeTotal = 0
        var writeCorrect = 0
        store.examResults.forEach { exam ->
            recTotal += exam.recognitionQuestions.size
            recCorrect += exam.recognitionQuestions.count { it.correct }
            prodTotal += exam.productionQuestions.size
            prodCorrect += exam.productionQuestions.count { it.correct }
            val writingQ = exam.questions.filter { it.questionType.contains("writing", ignoreCase = true) }
            writeTotal += writingQ.size
            writeCorrect += writingQ.count { it.correct }
        }
        return StudyVsExamGap(
            studyAccuracy = studyAccuracy,
            examRecognitionAccuracy = if (recTotal == 0) 0f else recCorrect.toFloat() / recTotal,
            examProductionAccuracy = if (prodTotal == 0) 0f else prodCorrect.toFloat() / prodTotal,
            examWritingAccuracy = if (writeTotal == 0) 0f else writeCorrect.toFloat() / writeTotal
        )
    }

    // ------------------------------------------------------------
    // Forecast — expected due workload from actual due dates
    // ------------------------------------------------------------
    fun forecast(store: LearningStore, days: Int = 30, today: LocalDate = Clock.System.todayIn(tz())): List<ForecastPoint> {
        val buckets = IntArray(days)
        store.cards.filter { !it.isSuspended && !it.buried && it.status != ua.syt0r.kanji.desktop.model.SrsStatus.New && it.dueAt != null }
            .forEach { card ->
                val d = card.dueAt!!.toLocalDateTime(tz()).date
                if (d < today) {
                    buckets[0]++
                } else {
                    val idx = (0 until days).firstOrNull { today.plus(it.toLong(), DateTimeUnit.DAY) == d }
                    idx?.let { buckets[it]++ }
                }
            }
        return (0 until days).map { offset ->
            ForecastPoint(today.plus(offset.toLong(), DateTimeUnit.DAY), buckets[offset])
        }
    }

    fun dueToday(store: LearningStore, now: Instant = Clock.System.now()): Int =
        store.cards.count { !it.isSuspended && !it.buried && it.status != ua.syt0r.kanji.desktop.model.SrsStatus.New && it.dueAt != null && it.dueAt <= now }

    // ------------------------------------------------------------
    // Daily series for charts (from events)
    // ------------------------------------------------------------
    fun dailySeries(store: LearningStore, days: Int = 30, today: LocalDate = Clock.System.todayIn(tz())): List<DailyActivityPoint> {
        val byDay = store.reviewEvents.groupBy { dayKey(it.reviewedAt) }
        return (0 until days).map { offset ->
            val d = today.minus((days - 1 - offset).toLong(), DateTimeUnit.DAY)
            val events = byDay[d.key()].orEmpty()
            DailyActivityPoint(
                date = d,
                reviews = events.size,
                correct = events.count { it.correct },
                again = events.count { it.rating == ReviewRating.Again },
                newCards = events.count { it.wasNew },
                studyTimeMs = events.sumOf { it.responseTimeMs }
            )
        }
    }

    /** Mistake categories from real events — powers the Mistakes queue. */
    fun mistakeSnapshot(store: LearningStore): MistakeSnapshot {
        val again = store.reviewEvents.filter { it.rating == ReviewRating.Again }
        val writingMistakes = store.writingAttempts.filter { !it.correct }
        val examWrong = store.examResults.flatMap { it.questions }.filter { !it.correct }
        val lapsedCards = store.cards.filter { it.lapses >= 2 }
        return MistakeSnapshot(
            againEvents = again.size,
            writingMistakes = writingMistakes.size,
            examMistakes = examWrong.size,
            lapsedCards = lapsedCards.size,
            againCardIds = again.map { it.cardId }.distinct(),
            writingNoteIds = writingMistakes.map { it.noteId }.distinct(),
            examWrongCardIds = examWrong.mapNotNull { it.cardId }.distinct(),
            lapsedCardIds = lapsedCards.map { it.id }
        )
    }

    /** Study time (real) for goals/dashboard. */
    fun totalStudyTimeMs(store: LearningStore): Long =
        store.reviewEvents.sumOf { it.responseTimeMs } + store.writingAttempts.sumOf { it.durationMs } + store.examResults.sumOf { it.durationMs }
}

data class ExamAggregates(
    val count: Int = 0,
    val averageScore: Int = 0,
    val bestScore: Int = 0,
    val worstScore: Int = 0,
    val totalQuestions: Int = 0,
    val averageAccuracy: Float = 0f,
    val totalTimeMs: Long = 0
)

data class DailyActivityPoint(
    val date: LocalDate,
    val reviews: Int,
    val correct: Int,
    val again: Int,
    val newCards: Int,
    val studyTimeMs: Long
)

data class MistakeSnapshot(
    val againEvents: Int = 0,
    val writingMistakes: Int = 0,
    val examMistakes: Int = 0,
    val lapsedCards: Int = 0,
    val againCardIds: List<String> = emptyList(),
    val writingNoteIds: List<String> = emptyList(),
    val examWrongCardIds: List<String> = emptyList(),
    val lapsedCardIds: List<String> = emptyList()
)

/** Reused by both the repository and the goals engine. */
enum class StatsPeriod(val label: String) {
    Today("Today"),
    Week("Week"),
    Month("Month"),
    Year("Year"),
    All("All time")
}

/** Goal definitions backed by real metrics from the repository. */
data class LearningGoal(
    val id: String,
    val name: String,
    val metric: GoalMetric,
    val target: Int,
    val period: StatsPeriod = StatsPeriod.Today
)

enum class GoalMetric { Reviews, NewCards, Minutes }

data class GoalProgress(
    val goal: LearningGoal,
    val achieved: Int,
    val fraction: Float,
    val complete: Boolean
)

object GoalsRepository {

    fun defaultGoals(): List<LearningGoal> = listOf(
        LearningGoal("goal-reviews-daily", "Daily reviews", GoalMetric.Reviews, 20, StatsPeriod.Today),
        LearningGoal("goal-new-daily", "Daily new cards", GoalMetric.NewCards, 5, StatsPeriod.Today),
        LearningGoal("goal-minutes-daily", "Daily study time (min)", GoalMetric.Minutes, 15, StatsPeriod.Today),
        LearningGoal("goal-reviews-weekly", "Weekly reviews", GoalMetric.Reviews, 150, StatsPeriod.Week),
        LearningGoal("goal-minutes-weekly", "Weekly study time (min)", GoalMetric.Minutes, 90, StatsPeriod.Week)
    )

    fun progress(goal: LearningGoal, stats: PeriodStats): GoalProgress {
        val achieved = when (goal.metric) {
            GoalMetric.Reviews -> stats.reviews
            GoalMetric.NewCards -> stats.newCards
            GoalMetric.Minutes -> (stats.studyTimeMs / 60000).toInt()
        }
        val fraction = if (goal.target <= 0) 0f else (achieved.toFloat() / goal.target).coerceIn(0f, 1f)
        return GoalProgress(goal, achieved, fraction, achieved >= goal.target)
    }

    fun allProgress(store: LearningStore, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): List<GoalProgress> {
        return defaultGoals().map { goal ->
            val stats = StatisticsRepository.periodStats(store, goal.period, today)
            progress(goal, stats)
        }
    }
}
