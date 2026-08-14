package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.core.srs.fsrs.FsrsCard
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/**
 * Pure statistics calculations. No database access — every function
 * derives its results from the data handed to it, which keeps the
 * definitions consistent, reviewable and unit-testable.
 *
 * Metric definitions (shared app-wide):
 * - Studied:   has at least one review recorded.
 * - Learned:   FSRS card exists with `repeats >= 1` (left the new state).
 * - Mature:    FSRS status == Review and interval >= 21 days.
 * - Mastered:  FSRS status == Review and interval >= 180 days.
 * - Relearning: FSRS status == Relearning.
 * - Weak:      `lapses >= 3` on the FSRS card.
 * - Retention/accuracy: correct answers (grade > 1) / total reviews.
 */
object StatisticsCalculator {

    private const val MATURE_INTERVAL_DAYS = 21L
    private const val MASTERED_INTERVAL_DAYS = 180L
    private const val WEAK_LAPSE_THRESHOLD = 3

    // ============================================================
    // Knowledge states
    // ============================================================

    fun knowledgeState(card: FsrsCard?): KnowledgeState {
        if (card == null) return KnowledgeState.New
        return when (card.status) {
            FsrsCardStatus.New -> if (card.repeats > 0) KnowledgeState.Learning else KnowledgeState.New
            FsrsCardStatus.Learning -> KnowledgeState.Learning
            FsrsCardStatus.Review -> when {
                card.interval >= MASTERED_INTERVAL_DAYS.days -> KnowledgeState.Mastered
                card.interval >= MATURE_INTERVAL_DAYS.days -> KnowledgeState.Mature
                else -> KnowledgeState.Learned
            }
            FsrsCardStatus.Relearning -> KnowledgeState.Relearning
        }
    }

    fun isWeak(card: FsrsCard?): Boolean =
        card != null && card.lapses >= WEAK_LAPSE_THRESHOLD && card.status != FsrsCardStatus.New

    fun isWritingVerified(writingAccuracy: Float?): Boolean =
        writingAccuracy != null && writingAccuracy >= 0.7f

    // ============================================================
    // Streaks
    // ============================================================

    /** Computes current + longest streak from a set of active (local) dates. */
    fun computeStreaks(activeDates: Set<LocalDate>, today: LocalDate): Pair<Int, Int> {
        if (activeDates.isEmpty()) return 0 to 0
        val dates = activeDates.sorted()

        // Longest run
        var longest = 0
        var run = 1
        for (i in 1 until dates.size) {
            run = if (dates[i].toEpochDays() == dates[i - 1].toEpochDays() + 1) run + 1 else 1
            if (run > longest) longest = run
        }
        longest = maxOf(longest, run)

        // Current streak: walk backwards from today (today counts if active,
        // otherwise yesterday may still extend a current streak).
        var cursor = today
        if (cursor !in activeDates) cursor = cursor.minus(1, DateTimeUnit.DAY)
        var current = 0
        while (cursor in activeDates) {
            current += 1
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }
        return current to longest
    }

    // ============================================================
    // Heatmap
    // ============================================================

    /**
     * Builds the yearly heatmap. [reviewDays] is the timezone-aware SQL
     * day aggregation of real review history; [dailyStats] contributes
     * writing/exam/session activity recorded since the feature shipped.
     */
    fun buildHeatmapYear(
        year: Int,
        reviewDays: List<DayAggregate>,
        dailyStats: List<DailyActivity>,
        today: LocalDate
    ): HeatmapYear {
        val zone = TimeZone.currentSystemDefault()
        val cells = mutableMapOf<LocalDate, DailyActivity>()

        reviewDays.forEach { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@forEach
            if (date.year == year) {
                cells[date] = DailyActivity(
                    date = date,
                    reviews = day.reviews.toInt(),
                    newCards = 0,
                    reviewCards = day.reviews.toInt(),
                    correct = day.correct.toInt(),
                    incorrect = day.incorrect.toInt(),
                    lapses = day.lapses.toInt(),
                    studyTime = day.studyTimeMs.toDurationSafe(),
                    cardsStudied = day.cardsStudied.toInt()
                )
            }
        }

        // Merge the incremental rollups (writing/exams/sessions) into the day.
        dailyStats.forEach { stat ->
            val date = stat.date ?: return@forEach
            if (date.year != year) return@forEach
            val existing = cells[date] ?: DailyActivity(date = date)
            cells[date] = existing.copy(
                writingAttempts = stat.writingAttempts,
                writingCorrect = stat.writingCorrect,
                examsTaken = stat.examsTaken,
                examScoreSum = stat.examScoreSum,
                examScoreCount = stat.examScoreCount,
                sessions = stat.sessions
            )
        }

        // Kanji/vocab split for heatmap cells comes from per-type aggregation.
        val activeDates = cells.keys
        val (current, longest) = computeStreaks(activeDates, today)

        return HeatmapYear(
            year = year,
            cells = cells,
            totalReviews = cells.values.sumOf { it.reviews.toLong() },
            totalStudyTime = cells.values.sumOf { it.studyTime.inWholeMilliseconds }.toDurationSafe(),
            activeDays = activeDates.size,
            currentStreak = current,
            longestStreak = longest
        )
    }

    /** Attaches per-type (kanji/vocab) review counts to already-built heatmap cells. */
    fun mergeTypeCounts(
        heatmap: HeatmapYear,
        kanjiDays: Map<String, Long>,
        vocabDays: Map<String, Long>
    ): HeatmapYear = heatmap.copy(
        cells = heatmap.cells.mapValues { (date, cell) ->
            cell.copy(
                kanjiReviews = (kanjiDays[date.toString()] ?: 0L).toInt(),
                vocabReviews = (vocabDays[date.toString()] ?: 0L).toInt()
            )
        }
    )

    // ============================================================
    // Day practice (cards practiced on a day)
    // ============================================================

    /**
     * Builds the per-day "cards practiced" breakdown from raw review history.
     * Pure and deterministic: given the same reviews it always produces the
     * same breakdown. [contentResolver] maps a (key, practiceType) pair to
     * display content (content / reading / meaning); the default simply uses
     * the key itself so the function is usable without any data source.
     */
    fun buildDayPracticeBreakdown(
        date: LocalDate,
        reviews: List<ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem>,
        kanjiTypes: Set<Long>,
        writingTypes: Set<Long>,
        contentResolver: (key: String, practiceType: Long) -> Triple<String, String, String> =
            { key, _ -> Triple(key, "", "") }
    ): DayPracticeBreakdown {
        if (reviews.isEmpty()) return DayPracticeBreakdown(date = date, writingTypes = writingTypes)

        val grouped = reviews.groupBy { it.key to it.practiceType }
        val kanji = mutableListOf<DayItemPractice>()
        val vocab = mutableListOf<DayItemPractice>()

        grouped.forEach { (keyAndType, items) ->
            val (key, practiceType) = keyAndType
            val (content, reading, meaning) = contentResolver(key, practiceType)
            val item = DayItemPractice(
                key = key,
                contentType = if (practiceType in kanjiTypes) ContentTypes.KANJI else ContentTypes.VOCAB,
                practiceType = practiceType,
                practiceLabel = practiceTypeLabel(practiceType),
                content = content,
                reading = reading,
                meaning = meaning,
                count = items.size,
                correct = items.count { it.grade > 1 },
                mistakes = items.sumOf { it.mistakes },
                lastGrade = items.maxOfOrNull { it.grade } ?: -1
            )
            if (practiceType in kanjiTypes) kanji.add(item) else vocab.add(item)
        }

        kanji.sortWith(compareByDescending<DayItemPractice> { it.count }.thenBy { it.content })
        vocab.sortWith(compareByDescending<DayItemPractice> { it.count }.thenBy { it.content })
        return DayPracticeBreakdown(date = date, kanji = kanji, vocab = vocab, writingTypes = writingTypes)
    }

    /** Human label for an SRS practice type id (used in day reports). */
    fun practiceTypeLabel(practiceType: Long): String = when (practiceType) {
        ua.syt0r.kanji.core.srs.SrsPracticeType.LetterWriting.value -> "Writing"
        ua.syt0r.kanji.core.srs.SrsPracticeType.LetterReading.value -> "Reading"
        ua.syt0r.kanji.core.srs.SrsPracticeType.VocabFlashcard.value -> "Flashcard"
        ua.syt0r.kanji.core.srs.SrsPracticeType.VocabReadingPicker.value -> "Reading picker"
        ua.syt0r.kanji.core.srs.SrsPracticeType.VocabWriting.value -> "Writing"
        else -> "Practice"
    }

    // ============================================================
    // Overview
    // ============================================================

    fun buildOverview(
        todayActivity: DailyActivity,
        weekReviews: List<DayAggregate>,
        monthReviews: List<DayAggregate>,
        yearReviews: List<DayAggregate>,
        totalReviews: Long,
        totalStudyTime: Duration,
        streaks: Pair<Int, Int>,
        statusCounts: CardStatusCounts,
        forecastNextDays: List<Int>,
        firstStudyDate: LocalDate?,
        retentionWindow: List<DayAggregate>
    ): StatisticsOverview {
        fun aggregate(days: List<DayAggregate>): RetentionSummary = RetentionSummary(
            totalReviews = days.sumOf { it.reviews },
            correct = days.sumOf { it.correct }
        )

        val week = aggregate(weekReviews)
        val month = aggregate(monthReviews)
        val year = aggregate(yearReviews)
        val retention = aggregate(retentionWindow)

        val averageReviewsPerDay = if (year.totalReviews > 0 && firstStudyDate != null) {
            val daysActive = (Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays() -
                firstStudyDate.toEpochDays() + 1).coerceAtLeast(1)
            year.totalReviews.toFloat() / daysActive
        } else 0f

        return StatisticsOverview(
            today = todayActivity,
            weekReviews = week.totalReviews.toInt(),
            weekStudyTime = weekReviews.sumOf { it.studyTimeMs }.toDurationSafe(),
            weekAccuracy = week.accuracy,
            monthReviews = month.totalReviews.toInt(),
            monthStudyTime = monthReviews.sumOf { it.studyTimeMs }.toDurationSafe(),
            monthAccuracy = month.accuracy,
            totalReviews = totalReviews,
            totalStudyTime = totalStudyTime,
            overallAccuracy = year.accuracy,
            currentStreak = streaks.first,
            longestStreak = streaks.second,
            averageReviewsPerDay = averageReviewsPerDay,
            cards = statusCounts,
            retention = retention,
            forecastNextDays = forecastNextDays,
            firstStudyDate = firstStudyDate,
            activeDays = yearReviews.size
        )
    }

    // ============================================================
    // Knowledge (kanji / vocab)
    // ============================================================

    /**
     * Aggregates per-item knowledge. [cards] maps item key -> FSRS card,
     * [jlptByItem] maps item key -> JLPT level, [totalsByJlpt] is the
     * catalog size per JLPT level (for coverage ratios),
     * [writingVerifiedKeys] is the set of items with a successful writing
     * record, and [itemTotals] gives per-item review counts for accuracy.
     */
    fun buildContentKnowledge(
        contentType: String,
        cards: Map<String, FsrsCard>,
        jlptByItem: Map<String, Int>,
        totalsByJlpt: Map<Int, Int>,
        frequencyByItem: Map<String, Int> = emptyMap(),
        writingVerifiedKeys: Set<String> = emptySet(),
        itemTotals: Map<String, ItemReviewTotals> = emptyMap(),
        weakLimit: Int = 25
    ): ContentTypeKnowledge {
        var studied = 0
        var learned = 0
        var mature = 0
        var mastered = 0
        var learning = 0
        var relearning = 0
        var weak = 0
        var suspended = 0
        var writingVerified = 0
        var recognitionOnly = 0

        val weakItems = mutableListOf<KnowledgeItem>()
        val studiedKeys = mutableSetOf<String>()
        val jlptSeen = mutableMapOf<Int, MutableList<KnowledgeItem>>()

        cards.forEach { (key, card) ->
            if (card.status == FsrsCardStatus.New && card.repeats == 0) return@forEach
            val state = knowledgeState(card)
            studiedKeys += key
            if (isWeak(card)) weak++
            when (state) {
                KnowledgeState.Learning -> learning++
                KnowledgeState.Learned -> learned++
                KnowledgeState.Mature -> mature++
                KnowledgeState.Mastered -> mastered++
                KnowledgeState.Relearning -> relearning++
                KnowledgeState.Suspended -> suspended++
                else -> {}
            }
            if (key in writingVerifiedKeys) {
                writingVerified++
            }
            val item = KnowledgeItem(
                key = key,
                content = key,
                state = state,
                jlptLevel = jlptByItem[key],
                lapses = card.lapses,
                reviews = card.repeats,
                intervalDays = card.interval.inWholeDays.toInt(),
                accuracy = itemTotals[key]?.let {
                    if (it.reviews == 0L) 0f else it.correct.toFloat() / it.reviews
                } ?: 0f,
                writingAccuracy = null
            )
            if (isWeak(card)) {
                weakItems.add(item)
            }
            item.jlptLevel?.let { level -> jlptSeen.getOrPut(level) { mutableListOf() }.add(item) }
        }

        val jlptCoverage = (5 downTo 1).map { level ->
            val total = totalsByJlpt[level] ?: 0
            val seen = jlptSeen[level].orEmpty()
            JlptCoverage(
                level = level,
                total = total,
                encountered = seen.size,
                studied = seen.count { it.state != KnowledgeState.New },
                learned = seen.count { it.state in setOf(KnowledgeState.Learned, KnowledgeState.Mature, KnowledgeState.Mastered) },
                mastered = seen.count { it.state == KnowledgeState.Mastered }
            )
        }

        val frequencyCoverage = buildFrequencyCoverage(frequencyByItem, studiedKeys)

        return ContentTypeKnowledge(
            contentType = contentType,
            totalCatalog = totalsByJlpt.values.sum(),
            studied = studiedKeys.size,
            learned = learned + mature + mastered,
            mature = mature,
            mastered = mastered,
            learning = learning,
            relearning = relearning,
            weak = weak,
            suspended = suspended,
            writingVerified = writingVerified,
            recognitionOnly = (studiedKeys.size - writingVerified).coerceAtLeast(0),
            jlptCoverage = jlptCoverage,
            frequencyCoverage = frequencyCoverage,
            weakItems = weakItems.sortedBy { it.accuracy }.take(weakLimit)
        )
    }

    private fun buildFrequencyCoverage(
        frequencyByItem: Map<String, Int>,
        studiedKeys: Set<String>
    ): List<FrequencyCoverage> {
        if (frequencyByItem.isEmpty()) return emptyList()
        val bands = listOf(
            FrequencyCoverage("Top 1000", 0, 1000),
            FrequencyCoverage("1k–2k", 1001, 2000),
            FrequencyCoverage("2k–5k", 2001, 5000),
            FrequencyCoverage("5k–10k", 5001, 10000),
            FrequencyCoverage("10k+", 10001, null)
        )
        return bands.map { band ->
            val inBand = frequencyByItem.filter { (_, freq) ->
                freq >= band.bandStart && (band.bandEnd == null || freq <= band.bandEnd)
            }
            FrequencyCoverage(
                label = band.label,
                bandStart = band.bandStart,
                bandEnd = band.bandEnd,
                total = inBand.size,
                studied = inBand.keys.count { it in studiedKeys }
            )
        }
    }

    // ============================================================
    // Retention / SRS analytics
    // ============================================================

    /** Accuracy split by review recency (how long ago the reviews happened). */
    fun retentionByAge(reviewDays: List<DayAggregate>, today: LocalDate): List<RetentionByAge> {
        val buckets = listOf(
            "last 7d" to 0..6,
            "8–30d" to 7..30,
            "31–90d" to 31..90,
            "91d+" to 91..Int.MAX_VALUE
        )
        return buckets.map { (label, range) ->
            var total = 0L
            var correct = 0L
            reviewDays.forEach { day ->
                val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@forEach
                val age = today.toEpochDays() - date.toEpochDays()
                if (age in range) {
                    total += day.reviews
                    correct += day.correct
                }
            }
            RetentionByAge(label, total, correct)
        }
    }

    // ============================================================
    // Exams
    // ============================================================

    fun buildExamStatistics(exams: List<ExamRecord>): ExamStatistics {
        val completed = exams.filter { it.status == ExamStatus.Completed }
        if (completed.isEmpty()) return ExamStatistics()

        val scores = completed.map { it.score }
        return ExamStatistics(
            completed = completed.size,
            averageScore = scores.average().toFloat(),
            highestScore = scores.maxOrNull() ?: 0,
            lowestScore = scores.minOrNull() ?: 0,
            averageAccuracy = completed.map { it.accuracy }.average().toFloat(),
            averageTimeMs = completed.map { it.totalTimeMs }.average().toLong(),
            scoreTrend = completed.sortedBy { it.startedAt }.map {
                ExamScorePoint(it.startedAt, it.score, it.accuracy)
            },
            byType = completed.groupingBy { it.examType }.eachCount().toList(),
            byJlpt = completed
                .mapNotNull { parseJlptFromScope(it.scopeJson) }
                .groupingBy { it }
                .eachCount()
                .toList()
        )
    }

    private fun parseJlptFromScope(scopeJson: String): Int? {
        // The generator serializes ExamScope as {"jlptLevel":5,...}; also
        // accept the legacy {"jlpt":5} form and plain "jlpt: 5".
        val regex = Regex("jlpt(?:Level)?[=:]\\s*(\\d)").find(scopeJson) ?: return null
        return regex.groupValues[1].toIntOrNull()
    }

    // ============================================================
    // Study timeline milestones (derived from real history)
    // ============================================================

    fun buildMilestones(
        firstReview: Instant?,
        cumulativeReviewDays: List<DayAggregate>,
        firstWritingAttempt: Instant?,
        firstExam: ExamRecord?,
        learnedKanjiTotal: Int,
        today: LocalDate
    ): List<LearningMilestone> {
        val zone = TimeZone.currentSystemDefault()
        val milestones = mutableListOf<LearningMilestone>()

        firstReview?.let {
            milestones.add(
                LearningMilestone(
                    date = it.toLocalDateTime(zone).date,
                    title = "First review",
                    icon = "🌱"
                )
            )
        }

        // Cumulative review milestones (100 / 500 / 1000 / 5000)
        val sortedDays = cumulativeReviewDays
            .mapNotNull { day ->
                runCatching { LocalDate.parse(day.date) }.getOrNull()?.let { date -> date to day.reviews }
            }
            .sortedBy { it.first }

        var cumulative = 0L
        val targets = mutableListOf(100L, 500L, 1000L, 5000L)
        sortedDays.forEach { (date, count) ->
            cumulative += count
            targets.firstOrNull { cumulative >= it }?.let { target ->
                milestones.add(
                    LearningMilestone(
                        date = date,
                        title = "$target reviews",
                        icon = "🎯",
                        value = "$target"
                    )
                )
            }
            // remove reached targets (each only once)
            while (targets.isNotEmpty() && cumulative >= targets.first()) {
                targets.removeAt(0)
            }
        }

        firstWritingAttempt?.let {
            milestones.add(
                LearningMilestone(
                    date = it.toLocalDateTime(zone).date,
                    title = "First writing practice",
                    icon = "✍️"
                )
            )
        }

        firstExam?.let {
            milestones.add(
                LearningMilestone(
                    date = it.startedAt.toLocalDateTime(zone).date,
                    title = "First exam",
                    icon = "📝"
                )
            )
        }

        return milestones.sortedBy { it.date }.takeLast(12)
    }

    // ============================================================
    // Forecast
    // ============================================================

    /** Number of cards due per day for the next [days] days (FSRS intervals). */
    fun buildForecast(
        cards: List<Pair<Instant?, Duration>>,
        days: Int,
        now: Instant
    ): List<Int> {
        val result = MutableList(days) { 0 }
        cards.forEach { (lastReview, interval) ->
            val due = (lastReview ?: return@forEach).plus(interval)
            val diffDays = ((due - now).inWholeDays).toInt()
            if (diffDays in 0 until days) {
                result[diffDays] += 1
            }
        }
        return result
    }

    /** Counts per FSRS status + derived aggregates from the raw card map. */
    fun buildStatusCounts(cards: Map<String, FsrsCard>, now: Instant): CardStatusCounts {
        val list = cards.values
        var due = 0
        var young = 0
        var mature = 0
        var relearning = 0
        var learning = 0
        list.forEach { card ->
            val last = card.lastReview
            if (last != null && last + card.interval <= now) due++
            when (card.status) {
                FsrsCardStatus.New -> {}
                FsrsCardStatus.Learning -> learning++
                FsrsCardStatus.Review -> {
                    if (card.interval >= MATURE_INTERVAL_DAYS.days) mature++ else young++
                }
                FsrsCardStatus.Relearning -> relearning++
            }
        }
        val intervals = list.mapNotNull { it.interval.inWholeDays }
        return CardStatusCounts(
            total = cards.size,
            due = due,
            learning = learning,
            young = young,
            mature = mature,
            relearning = relearning,
            averageIntervalDays = if (intervals.isNotEmpty()) (intervals.average()).roundToInt() else 0,
            averageEase = if (list.isNotEmpty()) {
                val difficulties = list.mapNotNull { (it.params as? ua.syt0r.kanji.core.srs.fsrs.FsrsCardParams.Existing)?.difficulty }
                if (difficulties.isEmpty()) 2.5f else difficulties.average().toFloat()
            } else 2.5f
        )
    }
}

private fun Long.toDurationSafe(): Duration = this.milliseconds
