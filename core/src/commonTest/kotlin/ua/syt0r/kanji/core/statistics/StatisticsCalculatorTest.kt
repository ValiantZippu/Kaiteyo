package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import ua.syt0r.kanji.core.srs.fsrs.FsrsCard
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardParams
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/**
 * Unit tests for the pure statistics calculations. Every metric definition
 * documented in StatisticsCalculator (studied / learned / mature / mastered /
 * weak / retention) is exercised with exact expected values.
 */
class StatisticsCalculatorTest {

    private val today = LocalDate(2026, 8, 11)
    private val epoch: Instant get() = Instant.fromEpochMilliseconds(0)

    private fun card(
        status: FsrsCardStatus,
        intervalDays: Long,
        lapses: Int = 0,
        repeats: Int = 1,
        reviewTime: Instant = epoch
    ): FsrsCard = FsrsCard(
        status = status,
        params = FsrsCardParams.Existing(
            difficulty = 5.0,
            stability = 1.0,
            reviewTime = reviewTime
        ),
        interval = intervalDays.days,
        lapses = lapses,
        repeats = repeats
    )

    private fun day(date: String, reviews: Long, correct: Long, incorrect: Long, lapses: Long = 0, studyTimeMs: Long = 1000L, cardsStudied: Long = reviews) =
        DayAggregate(date, reviews, correct, incorrect, lapses, studyTimeMs, cardsStudied)

    // ============================================================
    // Knowledge states
    // ============================================================

    @Test
    fun knowledgeStatesMapToDocumentedDefinitions() {
        assertEquals(KnowledgeState.New, StatisticsCalculator.knowledgeState(null))
        assertEquals(KnowledgeState.New, StatisticsCalculator.knowledgeState(card(FsrsCardStatus.New, 0, repeats = 0)))
        assertEquals(KnowledgeState.Learning, StatisticsCalculator.knowledgeState(card(FsrsCardStatus.New, 0, repeats = 1)))
        assertEquals(KnowledgeState.Learning, StatisticsCalculator.knowledgeState(card(FsrsCardStatus.Learning, 0)))
        assertEquals(KnowledgeState.Learned, StatisticsCalculator.knowledgeState(card(FsrsCardStatus.Review, 1)))
        assertEquals(KnowledgeState.Mature, StatisticsCalculator.knowledgeState(card(FsrsCardStatus.Review, 21)))
        assertEquals(KnowledgeState.Mastered, StatisticsCalculator.knowledgeState(card(FsrsCardStatus.Review, 180)))
        assertEquals(KnowledgeState.Relearning, StatisticsCalculator.knowledgeState(card(FsrsCardStatus.Relearning, 0)))
    }

    @Test
    fun weakRequiresLapsesAndNotNew() {
        assertFalse(StatisticsCalculator.isWeak(null))
        assertFalse(StatisticsCalculator.isWeak(card(FsrsCardStatus.New, 0, lapses = 5)))
        assertTrue(StatisticsCalculator.isWeak(card(FsrsCardStatus.Review, 30, lapses = 3)))
        assertTrue(StatisticsCalculator.isWeak(card(FsrsCardStatus.Relearning, 0, lapses = 4)))
        assertFalse(StatisticsCalculator.isWeak(card(FsrsCardStatus.Review, 30, lapses = 2)))
    }

    // ============================================================
    // Streaks
    // ============================================================

    @Test
    fun emptyHistoryHasNoStreaks() {
        assertEquals(0 to 0, StatisticsCalculator.computeStreaks(emptySet(), today))
    }

    @Test
    fun singleDayCountsAsOne() {
        assertEquals(1 to 1, StatisticsCalculator.computeStreaks(setOf(today), today))
    }

    @Test
    fun currentStreakWalksBackFromToday() {
        val active = setOf(today.minus(2, kotlinx.datetime.DateTimeUnit.DAY), today.minus(1, kotlinx.datetime.DateTimeUnit.DAY), today)
        assertEquals(3 to 3, StatisticsCalculator.computeStreaks(active, today))
    }

    @Test
    fun yesterdayCountsTowardCurrentStreak() {
        // Today inactive, yesterday active and yesterday-1 active → current 2.
        val active = setOf(
            today.minus(1, kotlinx.datetime.DateTimeUnit.DAY),
            today.minus(2, kotlinx.datetime.DateTimeUnit.DAY)
        )
        assertEquals(2 to 2, StatisticsCalculator.computeStreaks(active, today))
    }

    @Test
    fun longestStreakSurvivesGaps() {
        val active = (1..5).map { today.minus(it.toLong(), kotlinx.datetime.DateTimeUnit.DAY) }.toSet() +
            setOf(today.minus(1, kotlinx.datetime.DateTimeUnit.DAY), today)
        val (current, longest) = StatisticsCalculator.computeStreaks(active, today)
        assertEquals(2, current)
        assertEquals(5, longest)
    }

    // ============================================================
    // Heatmap
    // ============================================================

    @Test
    fun heatmapYearKeepsOnlyThatYear() {
        val year = 2026
        val reviewDays = listOf(
            day("2026-08-10", 10, 8, 2, lapses = 1, studyTimeMs = 5000),
            day("2026-08-11", 5, 4, 1, studyTimeMs = 3000),
            day("2025-12-31", 99, 90, 9) // wrong year — excluded
        )
        val dailyStats = listOf(
            DailyActivity(
                date = LocalDate(2026, 8, 10),
                writingAttempts = 3,
                writingCorrect = 2,
                examsTaken = 1,
                examScoreSum = 80,
                examScoreCount = 1,
                sessions = 2
            )
        )
        val heatmap = StatisticsCalculator.buildHeatmapYear(year, reviewDays, dailyStats, today)

        assertEquals(2, heatmap.cells.size)
        assertFalse(heatmap.cells.containsKey(LocalDate(2025, 12, 31)))
        val aug10 = heatmap.cells[LocalDate(2026, 8, 10)]
        assertTrue(aug10 != null)
        assertEquals(10, aug10.reviews)
        assertEquals(3, aug10.writingAttempts)
        assertEquals(1, aug10.examsTaken)
        assertEquals(15L, heatmap.totalReviews)
        assertEquals(2, heatmap.activeDays)
        assertEquals(2, heatmap.currentStreak)
        assertEquals(2, heatmap.longestStreak)
    }

    @Test
    fun mergeTypeCountsAttachesKanjiVocabSplit() {
        val base = StatisticsCalculator.buildHeatmapYear(
            2026,
            listOf(day("2026-08-11", 10, 8, 2)),
            emptyList(),
            today
        )
        val merged = StatisticsCalculator.mergeTypeCounts(
            base,
            kanjiDays = mapOf("2026-08-11" to 7L),
            vocabDays = mapOf("2026-08-11" to 3L)
        )
        val cell = merged.cells[LocalDate(2026, 8, 11)]!!
        assertEquals(7, cell.kanjiReviews)
        assertEquals(3, cell.vocabReviews)
    }

    // ============================================================
    // Overview
    // ============================================================

    @Test
    fun overviewAggregatesWindows() {
        val week = listOf(day("2026-08-05", 10, 8, 2), day("2026-08-06", 20, 15, 5))
        val month = listOf(day("2026-07-20", 30, 20, 10), day("2026-07-21", 10, 5, 5))
        val year = listOf(day("2026-01-10", 100, 75, 25), day("2026-08-01", 50, 40, 10))
        val retention = year

        val overview = StatisticsCalculator.buildOverview(
            todayActivity = DailyActivity(date = today, reviews = 5, correct = 4, incorrect = 1),
            weekReviews = week,
            monthReviews = month,
            yearReviews = year,
            totalReviews = 1000,
            totalStudyTime = 3_600_000L.millisecondsSafe,
            streaks = 2 to 7,
            statusCounts = CardStatusCounts(total = 30),
            forecastNextDays = listOf(3, 1, 0),
            firstStudyDate = LocalDate(2025, 1, 1),
            retentionWindow = retention
        )

        assertEquals(30, overview.weekReviews)
        assertEquals(40, overview.monthReviews)
        assertEquals(23f / 30f, overview.weekAccuracy, 0.001f)
        assertEquals(25f / 40f, overview.monthAccuracy, 0.001f)
        assertEquals(1000L, overview.totalReviews)
        assertEquals(2, overview.currentStreak)
        assertEquals(7, overview.longestStreak)
        assertEquals(2, overview.activeDays)
        assertEquals(115L, overview.retention.correct)
    }

    // ============================================================
    // Knowledge aggregation
    // ============================================================

    @Test
    fun contentKnowledgeCountsMatchDefinitions() {
        val cards = mapOf(
            "食" to card(FsrsCardStatus.Review, 200, repeats = 5),   // mastered
            "水" to card(FsrsCardStatus.Review, 30, repeats = 4),    // mature
            "山" to card(FsrsCardStatus.Review, 2, repeats = 2),     // learned
            "火" to card(FsrsCardStatus.Learning, 0, repeats = 1),   // learning
            "金" to card(FsrsCardStatus.Relearning, 0, lapses = 3),  // relearning + weak
            "木" to card(FsrsCardStatus.New, 0, repeats = 0)         // never studied — skipped
        )
        val knowledge = StatisticsCalculator.buildContentKnowledge(
            contentType = ContentTypes.KANJI,
            cards = cards,
            jlptByItem = mapOf("食" to 5, "水" to 5, "山" to 4, "火" to 4, "金" to 3),
            totalsByJlpt = mapOf(5 to 3, 4 to 3, 3 to 2),
            writingVerifiedKeys = setOf("食", "水"),
            itemTotals = mapOf(
                "食" to ItemReviewTotals("食", 1, 5, 4),
                "金" to ItemReviewTotals("金", 1, 2, 0)
            )
        )

        assertEquals(5, knowledge.studied)
        assertEquals(3, knowledge.learned)   // 山 + 水 + 食
        assertEquals(1, knowledge.mature)
        assertEquals(1, knowledge.mastered)
        assertEquals(1, knowledge.learning)
        assertEquals(1, knowledge.relearning)
        assertEquals(1, knowledge.weak)      // 金
        assertEquals(2, knowledge.writingVerified)
        assertEquals(3, knowledge.recognitionOnly)
        assertEquals(1, knowledge.weakItems.size)
        assertEquals("金", knowledge.weakItems.first().key)
        assertEquals(0f, knowledge.weakItems.first().accuracy)

        val n5 = knowledge.jlptCoverage.first { it.level == 5 }
        assertEquals(3, n5.total)
        assertEquals(2, n5.encountered)
        assertEquals(2, n5.studied)
        assertEquals(2, n5.learned)
        assertEquals(1, n5.mastered)
    }

    @Test
    fun frequencyCoverageBands() {
        val knowledge = StatisticsCalculator.buildContentKnowledge(
            contentType = ContentTypes.VOCAB,
            cards = mapOf(
                "食べる" to card(FsrsCardStatus.Review, 30),
                "食堂" to card(FsrsCardStatus.Review, 30)
            ),
            jlptByItem = emptyMap(),
            totalsByJlpt = emptyMap(),
            frequencyByItem = mapOf("食べる" to 500, "食堂" to 1500)
        )
        val top1000 = knowledge.frequencyCoverage.first { it.bandStart == 0 }
        assertEquals(1, top1000.total)
        assertEquals(1, top1000.studied)
        val band1k2k = knowledge.frequencyCoverage.first { it.bandStart == 1001 }
        assertEquals(1, band1k2k.total)
        assertEquals(1, band1k2k.studied)
    }

    @Test
    fun emptyKnowledgeIsZeroed() {
        val knowledge = StatisticsCalculator.buildContentKnowledge(
            contentType = ContentTypes.KANJI,
            cards = emptyMap(),
            jlptByItem = emptyMap(),
            totalsByJlpt = emptyMap()
        )
        assertEquals(0, knowledge.studied)
        assertEquals(0, knowledge.weak)
        assertEquals(5, knowledge.jlptCoverage.size)
        assertEquals(0, knowledge.jlptCoverage.first().total)
    }

    // ============================================================
    // Retention
    // ============================================================

    @Test
    fun retentionByAgeBucketsReviewsByRecency() {
        val reviewDays = listOf(
            day("2026-08-05", 10, 9, 1), // 6 days ago → last 7d
            day("2026-07-15", 20, 10, 10), // 27 days ago → 8–30d
            day("2026-05-01", 40, 30, 10), // > 90 days → 91d+
            day("2026-08-11", 5, 4, 1) // today → last 7d
        )
        val buckets = StatisticsCalculator.retentionByAge(reviewDays, today)

        val last7 = buckets.first { it.label == "last 7d" }
        assertEquals(15L, last7.total)
        assertEquals(13L, last7.correct)
        val month = buckets.first { it.label == "8–30d" }
        assertEquals(20L, month.total)
        val old = buckets.first { it.label == "91d+" }
        assertEquals(40L, old.total)
    }

    // ============================================================
    // Exams
    // ============================================================

    private fun examRecord(id: Long, score: Int, type: String = "kanji", scope: String = """{"jlptLevel":5}""") = ExamRecord(
        id = id,
        title = "Test",
        examType = type,
        scopeJson = scope,
        questionCount = 10,
        seed = id,
        startedAt = Instant.fromEpochMilliseconds(id * 1000),
        finishedAt = Instant.fromEpochMilliseconds(id * 1000 + 60_000),
        status = ExamStatus.Completed,
        score = score,
        accuracy = score / 10f,
        totalTimeMs = 60_000
    )

    @Test
    fun examStatisticsAggregateScores() {
        val stats = StatisticsCalculator.buildExamStatistics(
            listOf(examRecord(1, 8), examRecord(2, 5), examRecord(3, 9, type = "vocab"))
        )
        assertEquals(3, stats.completed)
        assertEquals(22f / 3f, stats.averageScore, 0.001f)
        assertEquals(9, stats.highestScore)
        assertEquals(5, stats.lowestScore)
        assertEquals(3, stats.scoreTrend.size)
        assertEquals(listOf("kanji" to 2, "vocab" to 1), stats.byType)
        assertEquals(listOf(5 to 3), stats.byJlpt)
    }

    @Test
    fun examStatisticsHandleEmptyHistory() {
        val stats = StatisticsCalculator.buildExamStatistics(emptyList())
        assertEquals(0, stats.completed)
        assertEquals(0f, stats.averageScore)
        assertEquals(0, stats.highestScore)
        assertTrue(stats.scoreTrend.isEmpty())
    }

    @Test
    fun examJlptParsingHandlesGeneratorScopeJson() {
        val stats = StatisticsCalculator.buildExamStatistics(
            listOf(
                examRecord(1, 8, scope = """{"jlptLevel":3,"contentType":"kanji","questionTypes":["KanjiToMeaning"]}"""),
                examRecord(2, 6, scope = """{"jlptLevel":4}"""),
                examRecord(3, 7, scope = """{"jlpt":2}"""),
                examRecord(4, 9, scope = """{"contentType":"vocab"}""")
            )
        )
        assertEquals(listOf(3 to 1, 4 to 1, 2 to 1), stats.byJlpt)
    }

    // ============================================================
    // Milestones
    // ============================================================

    @Test
    fun milestonesAreDerivedFromRealHistory() {
        val firstReview = Instant.fromEpochMilliseconds(0)
        val cumulative = listOf(
            day("2026-01-01", 100, 80, 20),
            day("2026-02-01", 500, 400, 100),
            day("2026-03-01", 600, 500, 100)
        )
        val milestones = StatisticsCalculator.buildMilestones(
            firstReview = firstReview,
            cumulativeReviewDays = cumulative,
            firstWritingAttempt = Instant.fromEpochMilliseconds(100),
            firstExam = examRecord(1, 8),
            learnedKanjiTotal = 50,
            today = today
        )
        val titles = milestones.map { it.title }
        assertTrue(titles.contains("First review"))
        assertTrue(titles.contains("100 reviews"))
        assertTrue(titles.contains("500 reviews"))
        assertTrue(titles.contains("1000 reviews"))
        assertTrue(titles.contains("First writing practice"))
        assertTrue(titles.contains("First exam"))
        // Sorted chronologically.
        assertEquals(milestones.sortedBy { it.date }, milestones)
    }

    @Test
    fun emptyHistoryHasNoMilestones() {
        val milestones = StatisticsCalculator.buildMilestones(
            firstReview = null,
            cumulativeReviewDays = emptyList(),
            firstWritingAttempt = null,
            firstExam = null,
            learnedKanjiTotal = 0,
            today = today
        )
        assertTrue(milestones.isEmpty())
    }

    // ============================================================
    // Forecast
    // ============================================================

    @Test
    fun forecastBucketsDueCardsByDay() {
        val now = Instant.fromEpochMilliseconds(1_000_000)
        // Instants cannot subtract date units (DAY is date-based); use Duration
        // arithmetic: lastReview = now - N days, interval = 2 days.
        val cards = listOf(
            (now - 1.days to 2.days), // due +1
            (now - 2.days to 2.days), // due 0
            (null to 1.days), // never reviewed — skipped
            (now - 10.days to 2.days) // due in the past — skipped
        )
        val forecast = StatisticsCalculator.buildForecast(cards, days = 7, now = now)
        assertEquals(listOf(1, 1, 0, 0, 0, 0, 0), forecast)
    }

    @Test
    fun statusCountsAggregateCardState() {
        val now = Instant.fromEpochMilliseconds(1000)
        val cards = mapOf(
            "a" to card(FsrsCardStatus.Review, 30, reviewTime = epoch),   // mature + due
            "b" to card(FsrsCardStatus.Review, 5, reviewTime = epoch),    // young + due
            "c" to card(FsrsCardStatus.Learning, 0, reviewTime = epoch),  // learning + due
            "d" to card(FsrsCardStatus.Relearning, 0, reviewTime = epoch),
            "e" to card(FsrsCardStatus.New, 0, repeats = 0, reviewTime = epoch),
            "f" to card(FsrsCardStatus.Review, 5000, reviewTime = epoch) // not yet due
        )
        val counts = StatisticsCalculator.buildStatusCounts(cards, now)
        assertEquals(6, counts.total)
        assertEquals(5, counts.due)
        assertEquals(1, counts.learning)
        assertEquals(1, counts.young)
        assertEquals(2, counts.mature)
        assertEquals(1, counts.relearning)
        // Intervals: 30, 5, 0, 0, 0 (new card), 5000 → mean 839.
        assertEquals(839, counts.averageIntervalDays)
        assertEquals(5.0f, counts.averageEase)
    }

    private val Long.millisecondsSafe: kotlin.time.Duration
        get() = this.milliseconds
}
