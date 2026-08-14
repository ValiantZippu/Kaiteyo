package ua.syt0r.kanji.presentation.screen.main.features

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.srs.SrsPracticeType
import ua.syt0r.kanji.core.srs.fsrs.FsrsCard
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus
import ua.syt0r.kanji.core.user_data.database.FsrsCardRepository
import ua.syt0r.kanji.core.statistics.ContentTypeKnowledge
import ua.syt0r.kanji.core.statistics.ContentTypes
import ua.syt0r.kanji.core.statistics.DailyActivity
import ua.syt0r.kanji.core.statistics.DayAggregate
import ua.syt0r.kanji.core.statistics.DayPracticeBreakdown
import ua.syt0r.kanji.core.statistics.ExamConfig
import ua.syt0r.kanji.core.statistics.ExamGenerator
import ua.syt0r.kanji.core.statistics.ExamQuestionRecord
import ua.syt0r.kanji.core.statistics.ExamQuestionType
import ua.syt0r.kanji.core.statistics.ExamRecord
import ua.syt0r.kanji.core.statistics.ExamScorer
import ua.syt0r.kanji.core.statistics.ExamSourceItem
import ua.syt0r.kanji.core.statistics.ExamStatistics
import ua.syt0r.kanji.core.statistics.ExamStatus
import ua.syt0r.kanji.core.statistics.GeneratedExamQuestion
import ua.syt0r.kanji.core.statistics.GradeDistribution
import ua.syt0r.kanji.core.statistics.GradedExam
import ua.syt0r.kanji.core.statistics.DeckRetention
import ua.syt0r.kanji.core.statistics.DeckRetentionCalculator
import ua.syt0r.kanji.core.statistics.GoalHistory
import ua.syt0r.kanji.core.statistics.GoalHistoryEntry
import ua.syt0r.kanji.core.statistics.GrowthCalculator
import ua.syt0r.kanji.core.statistics.GrowthPoint
import ua.syt0r.kanji.core.statistics.LearningProfile
import ua.syt0r.kanji.core.statistics.ProfileCalculator
import ua.syt0r.kanji.core.statistics.VelocityCalculator
import ua.syt0r.kanji.core.statistics.VelocityMetrics
import ua.syt0r.kanji.core.statistics.WeeklyExam
import ua.syt0r.kanji.core.statistics.HeatmapYear
import ua.syt0r.kanji.core.statistics.InProgressExam
import ua.syt0r.kanji.core.statistics.IntervalBucket
import ua.syt0r.kanji.core.statistics.ItemReviewTotals
import ua.syt0r.kanji.core.statistics.KnowledgeItem
import ua.syt0r.kanji.core.statistics.LearningGoal
import ua.syt0r.kanji.core.statistics.LearningMilestone
import ua.syt0r.kanji.core.statistics.LearningMistake
import ua.syt0r.kanji.core.statistics.RetentionByAge
import ua.syt0r.kanji.core.statistics.StatisticsCalculator
import ua.syt0r.kanji.core.statistics.StatisticsOverview
import ua.syt0r.kanji.core.statistics.StatisticsRepository
import ua.syt0r.kanji.core.statistics.StudyModes
import ua.syt0r.kanji.core.statistics.StudySessionRecord
import ua.syt0r.kanji.core.statistics.WeakEntity
import ua.syt0r.kanji.core.statistics.WritingCharacterStats
import ua.syt0r.kanji.core.statistics.WritingAttemptRecord
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.database.VocabPracticeRepository
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/**
 * The single statistics & examination controller. Owns all analytics
 * state for the unified Statistics screen, computes it from real
 * database data through [StatisticsCalculator] and drives the exam
 * lifecycle. Registered as a Koin singleton.
 */
class StatisticsController(
    private val dataCenter: KaiteyoDataCenter,
    private val statisticsRepository: StatisticsRepository,
    private val fsrsCardRepository: FsrsCardRepository,
    private val vocabPracticeRepository: VocabPracticeRepository,
    private val appDataRepository: AppDataRepository,
    private val appPreferences: PreferencesContract.AppPreferences,
    private val timeUtils: TimeUtils
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val examGenerator = ExamGenerator(json)

    // ---- Load state ----
    var isLoaded by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var loadError by mutableStateOf(false)

    // ---- Overview & heatmap ----
    var overview by mutableStateOf(StatisticsOverview())
    val heatmaps = mutableStateMapOf<Int, HeatmapYear>()
    var selectedYear by mutableStateOf(0)
        private set

    // ---- Knowledge ----
    var kanjiKnowledge by mutableStateOf(ContentTypeKnowledge(ContentTypes.KANJI))
        private set
    var vocabKnowledge by mutableStateOf(ContentTypeKnowledge(ContentTypes.VOCAB))
        private set
    val writingTopCharacters = mutableStateListOf<WritingCharacterStats>()

    // ---- Retention / SRS ----
    var gradeDistribution by mutableStateOf(GradeDistribution())
        private set
    val intervalBuckets = mutableStateListOf<IntervalBucket>()
    val retentionByAge = mutableStateListOf<RetentionByAge>()

    // ---- Sessions / activity ----
    val recentSessions = mutableStateListOf<StudySessionRecord>()

    // ---- Exams ----
    val exams = mutableStateListOf<ExamRecord>()
    var examStatistics by mutableStateOf(ExamStatistics())
        private set
    var activeExam by mutableStateOf<InProgressExam?>(null)
        private set

    /** The most recently graded exam, shown by the results review until dismissed. */
    var lastGradedExam by mutableStateOf<GradedExam?>(null)
        private set

    // ---- Mistakes / weakness ----
    val mistakeCategories = mutableStateListOf<Pair<String, Long>>()
    val weakEntities = mutableStateListOf<WeakEntity>()

    // ---- Personal profile / velocity / growth ----
    var learningProfile by mutableStateOf(LearningProfile())
        private set
    var velocity by mutableStateOf(VelocityMetrics())
        private set
    val deckRetention = mutableStateListOf<DeckRetention>()
    val kanjiGrowth = mutableStateListOf<GrowthPoint>()
    val vocabGrowth = mutableStateListOf<GrowthPoint>()

    // ---- Timeline / goals ----
    val milestones = mutableStateListOf<LearningMilestone>()
    var goals by mutableStateOf<List<LearningGoal>>(emptyList())
        private set

    private val zone: TimeZone get() = TimeZone.currentSystemDefault()
    private val today: LocalDate get() = timeUtils.getCurrentTime().date
    private val now: Instant get() = timeUtils.now()
    private val dayOffsetMillis: Long
        get() = timeUtils.getCurrentTime().time.toMillisecondOfDay().toLong()

    val currentYear: Int get() = today.year

    val availableYears: List<Int>
        get() = (heatmaps.keys + currentYear).sortedDescending()

    suspend fun ensureLoaded() {
        if (isLoaded) return
        load()
    }

    suspend fun load() {
        if (isLoading) return
        isLoading = true
        loadError = false
        try {
            dataCenter.ensureLoaded()
            refreshAll()
            isLoaded = true
        } catch (t: Throwable) {
            loadError = true
        } finally {
            isLoading = false
        }
    }

    suspend fun refresh() {
        if (isLoading) return
        isLoading = true
        try {
            refreshAll()
        } finally {
            isLoading = false
        }
    }

    fun selectYear(year: Int) {
        selectedYear = year
    }

    private suspend fun refreshAll() {
        loadOverview()
        loadHeatmaps()
        loadKnowledge()
        loadRetention()
        loadExams()
        loadMistakesAndWeakness()
        loadMilestones()
        loadGoals()
        loadRecentSessions()
        loadProfile()
        loadVelocity()
        loadDeckRetention()
        loadGrowth()
    }

    // ============================================================
    // Overview
    // ============================================================

    private suspend fun loadOverview() {
        val yearStart = today.minus(today.dayOfYear - 1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        val monthStart = today.minus(today.dayOfMonth - 1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        val weekStart = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        val todayStart = today.atStartOfDayIn(zone)

        val yearAgg = statisticsRepository.getReviewDayAggregation(yearStart, dayOffsetMillis)
        val monthAgg = statisticsRepository.getReviewDayAggregation(monthStart, dayOffsetMillis)
        val weekAgg = statisticsRepository.getReviewDayAggregation(weekStart, dayOffsetMillis)
        val todayAgg = statisticsRepository.getReviewDayAggregation(todayStart, dayOffsetMillis)

        val dailyRow = statisticsRepository.getDailyStats(today, today).firstOrNull()
        val todayActivity = mergeTodayActivity(todayAgg.firstOrNull(), dailyRow)

        val allCards = dataCenter.srsCards.mapValues { it.value }
        val statusCounts = StatisticsCalculator.buildStatusCounts(allCards, now)

        val forecast = StatisticsCalculator.buildForecast(
            cards = allCards.values.map { it.lastReview to it.interval },
            days = 14,
            now = now
        )

        val firstReview = statisticsRepository.getFirstReviewTime()
        val streaks = StatisticsCalculator.computeStreaks(
            activeDates = yearAgg.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet(),
            today = today
        )

        overview = StatisticsCalculator.buildOverview(
            todayActivity = todayActivity,
            weekReviews = weekAgg,
            monthReviews = monthAgg,
            yearReviews = yearAgg,
            totalReviews = statisticsRepository.getTotalReviewsCount(),
            totalStudyTime = statisticsRepository.getTotalPracticeTime(120_000L),
            streaks = streaks,
            statusCounts = statusCounts,
            forecastNextDays = forecast,
            firstStudyDate = firstReview?.toLocalDateTime(zone)?.date,
            retentionWindow = yearAgg
        )
    }

    private fun mergeTodayActivity(
        agg: DayAggregate?,
        dailyRow: DailyActivity?
    ): DailyActivity {
        val base = DailyActivity(
            date = today,
            reviews = agg?.reviews?.toInt() ?: 0,
            reviewCards = agg?.reviews?.toInt() ?: 0,
            correct = agg?.correct?.toInt() ?: 0,
            incorrect = agg?.incorrect?.toInt() ?: 0,
            lapses = agg?.lapses?.toInt() ?: 0,
            studyTime = (agg?.studyTimeMs ?: 0L).milliseconds,
            cardsStudied = agg?.cardsStudied?.toInt() ?: 0
        )
        return if (dailyRow == null) base else base.copy(
            newCards = dailyRow.newCards,
            writingAttempts = dailyRow.writingAttempts,
            writingCorrect = dailyRow.writingCorrect,
            examsTaken = dailyRow.examsTaken,
            examScoreSum = dailyRow.examScoreSum,
            examScoreCount = dailyRow.examScoreCount,
            sessions = dailyRow.sessions
        )
    }

    // ============================================================
    // Heatmaps
    // ============================================================

    private suspend fun loadHeatmaps() {
        val allTime = Instant.fromEpochMilliseconds(0)
        val dayAgg = statisticsRepository.getReviewDayAggregation(allTime, dayOffsetMillis)
        val years = dayAgg.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull()?.year }.toSet()

        val dailyStats = statisticsRepository.getDailyStatsAll()
        val kanjiAgg = statisticsRepository.getReviewDayAggregationForType(
            allTime, dayOffsetMillis, LETTER_PRACTICE_TYPES
        )
        val vocabAgg = statisticsRepository.getReviewDayAggregationForType(
            allTime, dayOffsetMillis, VOCAB_PRACTICE_TYPES
        )

        heatmaps.clear()
        years.forEach { year ->
            val built = StatisticsCalculator.buildHeatmapYear(year, dayAgg, dailyStats, today)
            heatmaps[year] = StatisticsCalculator.mergeTypeCounts(
                built,
                kanjiDays = kanjiAgg.associate { it.date to it.reviews },
                vocabDays = vocabAgg.associate { it.date to it.reviews }
            )
        }
        if (selectedYear == 0 || selectedYear !in heatmaps.keys) {
            selectedYear = currentYear.takeIf { it in heatmaps.keys } ?: heatmaps.keys.maxOrNull() ?: currentYear
        }
    }

    // ============================================================
    // Knowledge
    // ============================================================

    private suspend fun loadKnowledge() {
        // ----- Kanji -----
        val kanjiCards = dataCenter.srsCards.mapValues { it.value }
        val jlptByKanji = mutableMapOf<String, Int>()
        dataCenter.classifications.forEach { (kanji, classes) ->
            classes.firstNotNullOfOrNull { c ->
                c.removePrefix("n").toIntOrNull()
            }?.let { jlptByKanji[kanji] = it }
        }
        val kanjiTotalsByJlpt = (5 downTo 1).associateWith { level ->
            dataCenter.classifications.values.count { classes -> classes.any { it == "n$level" } }
        }
        val writingVerified = statisticsRepository.getWritingVerifiedCharacters()
        val itemTotals = statisticsRepository.getReviewPerItem(Instant.fromEpochMilliseconds(0))
            .filter { it.practiceType in LETTER_PRACTICE_TYPES }
            .associateBy { it.key }

        kanjiKnowledge = StatisticsCalculator.buildContentKnowledge(
            contentType = ContentTypes.KANJI,
            cards = kanjiCards,
            jlptByItem = jlptByKanji,
            totalsByJlpt = kanjiTotalsByJlpt,
            frequencyByItem = dataCenter.frequencies.toMap(),
            writingVerifiedKeys = writingVerified,
            itemTotals = itemTotals
        )

        // ----- Vocab -----
        val vocabCards = loadVocabCards()
        val vocabJlpt = loadVocabJlptMapping()
        val vocabTotalsByJlpt = (5 downTo 1).associateWith { level ->
            runCatching { appDataRepository.getImportDeckWordsCount("n$level") }.getOrDefault(0)
        }
        val vocabItemTotals = statisticsRepository.getReviewPerItem(Instant.fromEpochMilliseconds(0))
            .filter { it.practiceType in VOCAB_PRACTICE_TYPES }
            .associateBy { it.key }

        vocabKnowledge = StatisticsCalculator.buildContentKnowledge(
            contentType = ContentTypes.VOCAB,
            cards = vocabCards,
            jlptByItem = vocabJlpt,
            totalsByJlpt = vocabTotalsByJlpt,
            writingVerifiedKeys = emptySet(),
            itemTotals = vocabItemTotals
        )

        // ----- Writing analytics (top problem characters) -----
        val yearStart = today.minus(today.dayOfYear - 1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        val attempts = statisticsRepository.getWritingAttempts(yearStart, now)
        writingTopCharacters.clear()
        attempts.groupBy { it.character }
            .map { (character, list) ->
                WritingCharacterStats(
                    character = character,
                    attempts = list.size,
                    correct = list.count { (it.accuracy ?: 0f) >= 0.7f },
                    mistakes = list.sumOf { it.mistakes },
                    wrongOrder = list.sumOf { it.wrongOrder },
                    almost = list.sumOf { it.almost }
                )
            }
            .sortedBy { it.accuracy }
            .take(20)
            .forEach { writingTopCharacters.add(it) }
    }

    /** Best (most learned) FSRS card per vocab card across its practice types. */
    private suspend fun loadVocabCards(): Map<String, FsrsCard> {
        val entries = runCatching { vocabPracticeRepository.getAllCards() }.getOrDefault(emptyList())
        val cardIds = entries.map { it.cardId.toString() }.toSet()
        if (cardIds.isEmpty()) return emptyMap()
        val allSrs = fsrsCardRepository.getAll()
        val vocabFsrs = mutableMapOf<String, FsrsCard>()
        cardIds.forEach { cardId ->
            val best = VOCAB_PRACTICE_TYPES
                .mapNotNull { pt -> allSrs[ua.syt0r.kanji.core.srs.SrsCardKey(cardId, pt)] }
                .maxByOrNull { it.interval.inWholeMilliseconds }
            if (best != null) vocabFsrs[cardId] = best
        }
        return vocabFsrs
    }

    private suspend fun loadVocabJlptMapping(): Map<String, Int> {
        val mapping = mutableMapOf<Long, Int>()
        (5 downTo 1).forEach { level ->
            runCatching { appDataRepository.getImportDeckWords("n$level") }
                .getOrDefault(emptyList())
                .forEach { word -> mapping[word.id] = level }
        }
        val entries = runCatching { vocabPracticeRepository.getAllCards() }.getOrDefault(emptyList())
        return entries.mapNotNull { entry ->
            val level = mapping[entry.data.dictionaryId]
            if (level == null) null else entry.cardId.toString() to level
        }.toMap()
    }

    // ============================================================
    // Retention / SRS
    // ============================================================

    private suspend fun loadRetention() {
        val yearStart = today.minus(today.dayOfYear - 1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        gradeDistribution = statisticsRepository.getGradeBreakdown(yearStart, null)
        intervalBuckets.clear()
        statisticsRepository.getIntervalBreakdown(yearStart, LETTER_PRACTICE_TYPES + VOCAB_PRACTICE_TYPES)
            .forEach { intervalBuckets.add(it) }

        val yearAgg = statisticsRepository.getReviewDayAggregation(yearStart, dayOffsetMillis)
        retentionByAge.clear()
        StatisticsCalculator.retentionByAge(yearAgg, today).forEach { retentionByAge.add(it) }
    }

    // ============================================================
    // Exams
    // ============================================================

    private suspend fun loadExams() {
        exams.clear()
        statisticsRepository.getExams(100).forEach { exams.add(it) }
        examStatistics = StatisticsCalculator.buildExamStatistics(exams.toList())
    }

    /** Starts a new exam: generates questions from studied items, persists everything. */
    suspend fun startExam(config: ExamConfig): Boolean {
        lastGradedExam = null
        val items = buildExamSourceItems(config.studiedWithinDays)
        val generated = examGenerator.generate(config, items)
        if (generated.questions.isEmpty()) return false

        val examId = statisticsRepository.insertExam(generated.exam)
        generated.questions.forEachIndexed { index, question ->
            statisticsRepository.insertExamQuestion(
                question.toRecord(examId, index)
            )
        }
        activeExam = InProgressExam(
            exam = generated.exam.copy(id = examId),
            questions = statisticsRepository.getExamQuestions(examId)
        )
        return true
    }

    suspend fun answerActiveExam(questionIndex: Int, answer: String) {
        val inProgress = activeExam ?: return
        val question = inProgress.questions.getOrNull(questionIndex) ?: return
        val result = ExamScorer.score(question, answer)
        statisticsRepository.updateExamQuestionAnswer(
            questionId = question.id,
            userAnswer = result.normalizedUserAnswer,
            isCorrect = result.isCorrect,
            timeMs = 0L,
            mistakeCategory = result.mistakeCategory
        )
        val updated = question.copy(
            userAnswer = result.normalizedUserAnswer,
            isCorrect = result.isCorrect,
            mistakeCategory = result.mistakeCategory
        )
        activeExam = inProgress.copy(
            questions = inProgress.questions.toMutableList().also { it[questionIndex] = updated }
        )
    }

    /** Scores the exam, persists it and records mistakes + daily rollup. */
    suspend fun finishExam(): GradedExam? {
        val inProgress = activeExam ?: return null
        val questions = statisticsRepository.getExamQuestions(inProgress.exam.id)
        val graded = grade(inProgress.exam, questions)

        statisticsRepository.completeExam(
            id = graded.exam.id,
            score = graded.score,
            accuracy = graded.accuracy,
            totalTimeMs = 0L
        )
        statisticsRepository.incrementDailyExam(today, graded.score)
        graded.questions.filter { it.isCorrect == false }.forEach { q ->
            val type = runCatching { ExamQuestionType.valueOf(q.questionType) }.getOrNull()
            statisticsRepository.insertLearningMistake(
                LearningMistake(
                    timestamp = now,
                    entityKey = q.entityKey,
                    contentType = type?.contentType ?: ContentTypes.KANJI,
                    mode = StudyModes.EXAM,
                    questionType = q.questionType,
                    expected = q.answer,
                    actual = q.userAnswer ?: "",
                    category = q.mistakeCategory ?: "unknown",
                    severity = 1,
                    examId = q.examId
                )
            )
        }
        activeExam = null
        lastGradedExam = graded
        loadExams()
        return graded
    }

    /** Dismisses the graded-exam review and returns to the statistics tabs. */
    fun clearLastGradedExam() {
        lastGradedExam = null
    }

    suspend fun abandonExam() {
        val inProgress = activeExam ?: return
        statisticsRepository.abandonExam(inProgress.exam.id)
        activeExam = null
        lastGradedExam = null
        loadExams()
    }

    /** Starts the recurring weekly exam (items studied in the last 7 days). */
    suspend fun startWeeklyExam(questionCount: Int = 20): Boolean =
        startExam(WeeklyExam.config(questionCount = questionCount))

    /** Preview data for the weekly exam: what the last 7 days contained. */
    suspend fun weeklyExamSummary(): WeeklyExam.WeeklyExamSummary {
        val items = buildExamSourceItems(studiedWithinDays = 7)
        return WeeklyExam.summarize(items)
    }

    suspend fun deleteExam(id: Long) {
        statisticsRepository.deleteExam(id)
        loadExams()
    }

    fun grade(exam: ExamRecord, questions: List<ExamQuestionRecord>): GradedExam {
        val correct = questions.count { it.isCorrect == true }
        return GradedExam(
            exam = exam,
            questions = questions,
            score = correct,
            accuracy = if (questions.isEmpty()) 0f else correct.toFloat() / questions.size
        )
    }

    // ============================================================
    // Mistakes / weakness
    // ============================================================

    private suspend fun loadMistakesAndWeakness() {
        mistakeCategories.clear()
        statisticsRepository.getMistakeCategories().forEach { mistakeCategories.add(it) }

        val raw = statisticsRepository.getWeakEntities(Instant.fromEpochMilliseconds(0), 40)
        val byLapseKey = dataCenter.srsCards.filterValues { it.lapses >= 3 }
        val itemTotals = statisticsRepository.getReviewPerItem(Instant.fromEpochMilliseconds(0))
            .associateBy { it.key }

        weakEntities.clear()
        val seen = mutableSetOf<String>()
        raw.forEach { (key, pair) ->
            val (contentType, count) = pair
            val card = byLapseKey[key]
            val totals = itemTotals[key]
            weakEntities.add(
                WeakEntity(
                    entityKey = key,
                    contentType = contentType,
                    content = key,
                    mistakeCount = count.toInt(),
                    lapses = card?.lapses ?: 0,
                    accuracy = totals?.let {
                        if (it.reviews == 0L) 0f else it.correct.toFloat() / it.reviews
                    } ?: 0f
                )
            )
            seen.add(key)
        }
        // Kanji with repeated FSRS lapses that never generated a mistake record.
        byLapseKey.forEach { (key, card) ->
            if (key !in seen) {
                weakEntities.add(
                    WeakEntity(
                        entityKey = key,
                        contentType = ContentTypes.KANJI,
                        content = key,
                        lapses = card.lapses,
                        accuracy = itemTotals[key]?.let {
                            if (it.reviews == 0L) 0f else it.correct.toFloat() / it.reviews
                        } ?: 0f
                    )
                )
            }
        }
        weakEntities.sortByDescending { it.mistakeCount + it.lapses * 3 }
    }

    // ============================================================
    // Timeline / milestones
    // ============================================================

    private suspend fun loadMilestones() {
        val allTime = Instant.fromEpochMilliseconds(0)
        val dayAgg = statisticsRepository.getReviewDayAggregation(allTime, dayOffsetMillis)
        val firstReview = statisticsRepository.getFirstReviewTime()
        val writingAttempts = statisticsRepository.getWritingAttempts(allTime, now)
        val firstExam = exams.firstOrNull { it.status == ExamStatus.Completed }
        milestones.clear()
        StatisticsCalculator.buildMilestones(
            firstReview = firstReview,
            cumulativeReviewDays = dayAgg,
            firstWritingAttempt = writingAttempts.minByOrNull { it.timestamp }?.timestamp,
            firstExam = firstExam,
            learnedKanjiTotal = kanjiKnowledge.learned,
            today = today
        ).forEach { milestones.add(it) }
    }

    // ============================================================
    // Goals
    // ============================================================

    private suspend fun loadGoals() {
        val stored = appPreferences.statisticsGoalsJson.get()
        goals = if (stored.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<LearningGoal>>(stored) }.getOrDefault(emptyList())
    }

    suspend fun saveGoals(newGoals: List<LearningGoal>) {
        goals = newGoals
        appPreferences.statisticsGoalsJson.set(json.encodeToString(newGoals))
        recordGoalHistory()
    }

    /** Appends today's goal snapshot to the append-only goal history. */
    private suspend fun recordGoalHistory() {
        val progress = goalProgress()
        val stored = appPreferences.statisticsGoalHistoryJson.get()
        val entries = if (stored.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<GoalHistoryEntry>>(stored) }.getOrDefault(emptyList())
        appPreferences.statisticsGoalHistoryJson.set(
            json.encodeToString(GoalHistory.record(entries, today, progress))
        )
    }

    /** Append-only goal snapshots (oldest first) for trends and streaks. */
    suspend fun goalHistory(): List<GoalHistoryEntry> {
        val stored = appPreferences.statisticsGoalHistoryJson.get()
        return if (stored.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<GoalHistoryEntry>>(stored) }.getOrDefault(emptyList())
    }

    suspend fun clearGoalHistory() {
        appPreferences.statisticsGoalHistoryJson.set("")
    }

    suspend fun addGoal(goal: LearningGoal) {
        val updated = goals + goal
        saveGoals(updated)
    }

    suspend fun deleteGoal(id: String) {
        saveGoals(goals.filter { it.id != id })
    }

    /** Current progress toward every goal, derived from real counters. */
    suspend fun goalProgress(): List<ua.syt0r.kanji.core.statistics.GoalProgress> {
        val todayActivity = overview.today
        val weekReviews = overview.weekReviews
        val currentStreak = overview.currentStreak
        val yearStart = today.minus(today.dayOfYear - 1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        val weekStart = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY).atStartOfDayIn(zone)

        return goals.map { goal ->
            val value = when (goal.type) {
                ua.syt0r.kanji.core.statistics.GoalType.DailyReviews ->
                    if (goal.period == ua.syt0r.kanji.core.statistics.GoalPeriod.Weekly) weekReviews else todayActivity.reviews
                ua.syt0r.kanji.core.statistics.GoalType.NewKanji -> todayActivity.newCards + todayActivity.reviewCards
                ua.syt0r.kanji.core.statistics.GoalType.NewVocab -> todayActivity.newCards
                ua.syt0r.kanji.core.statistics.GoalType.StudyTime ->
                    (todayActivity.studyTime.inWholeMinutes).toInt()
                ua.syt0r.kanji.core.statistics.GoalType.WritingAttempts -> todayActivity.writingAttempts
                ua.syt0r.kanji.core.statistics.GoalType.Exams -> exams.count { it.status == ExamStatus.Completed }
                ua.syt0r.kanji.core.statistics.GoalType.Streak -> currentStreak
            }
            ua.syt0r.kanji.core.statistics.GoalProgress(
                goal = goal,
                current = value,
                target = goal.target,
                completed = value >= goal.target
            )
        }
    }

    // ============================================================
    // Study sessions
    // ============================================================

    private suspend fun loadRecentSessions() {
        recentSessions.clear()
        statisticsRepository.getAllStudySessions(30).forEach { recentSessions.add(it) }
    }

    // ============================================================
    // Personal profile / velocity / growth / deck analytics
    // ============================================================

    private suspend fun loadProfile() {
        val allSessions = statisticsRepository.getAllStudySessions(5_000)
        learningProfile = ProfileCalculator.build(
            contentKnowledge = listOf(kanjiKnowledge, vocabKnowledge),
            skillMatrix = skillMatrix(),
            mistakeCategories = mistakeCategories.toList(),
            studySessions = allSessions
        )
    }

    private suspend fun loadVelocity() {
        val window = 30
        val start = today.minus(window - 1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        val daily = statisticsRepository.getDailyStats(
            start.toLocalDateTime(zone).date,
            today
        )
        val completed = exams.filter { it.status == ExamStatus.Completed }
            .filter { it.startedAt >= start }
        velocity = VelocityCalculator.build(daily, completed, window)
    }

    private suspend fun loadDeckRetention() {
        deckRetention.clear()
        val allSessions = statisticsRepository.getAllStudySessions(5_000)
        DeckRetentionCalculator.fromSessions(allSessions)
            .take(20)
            .forEach { deckRetention.add(it) }
    }

    private suspend fun loadGrowth() {
        val daily = statisticsRepository.getDailyStatsAll()
        kanjiGrowth.clear()
        GrowthCalculator.build(daily).forEach { kanjiGrowth.add(it) }
        vocabGrowth.clear()
        // Vocabulary growth uses the same daily series (new cards are not yet
        // split by content type in the rollups) — kept for future split data.
        GrowthCalculator.build(daily).forEach { vocabGrowth.add(it) }
    }

    /** Study sessions that started on the given local day. */
    suspend fun studySessionsForDay(date: LocalDate): List<StudySessionRecord> {
        val start = date.atStartOfDayIn(zone)
        val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        return statisticsRepository.getStudySessions(start, end)
    }

    /** Writing attempts recorded on the given local day. */
    suspend fun writingAttemptsForDay(date: LocalDate): List<WritingAttemptRecord> {
        val start = date.atStartOfDayIn(zone)
        val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        return statisticsRepository.getWritingAttempts(start, end)
    }

    /**
     * Everything practiced on the given local day, straight from raw review
     * history — the real "what cards did I practice" answer for the heatmap
     * drill-down. Kanji content resolves through the studied catalog; vocab
     * through the vocab practice repository; each item carries its own
     * per-mode accuracy instead of a single merged number.
     */
    suspend fun itemsPracticedOnDay(date: LocalDate): DayPracticeBreakdown {
        val start = date.atStartOfDayIn(zone)
        val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        val reviews = statisticsRepository.getReviews(start, end)

        // Kanji catalog: key (character) -> content/reading/meaning.
        val kanjiContent = dataCenter.cards.associate { card ->
            card.id to Triple(card.character, card.reading, card.meaning)
        }
        // Vocab catalog: card id (as string) -> content/reading/meaning.
        val vocabContent = runCatching { vocabPracticeRepository.getAllCards() }
            .getOrDefault(emptyList())
            .associate { entry ->
                entry.cardId.toString() to Triple(
                    entry.data.kanjiReading ?: entry.data.kanaReading,
                    entry.data.kanaReading,
                    entry.data.meaning.orEmpty()
                )
            }

        val resolver: (String, Long) -> Triple<String, String, String> = { key, practiceType ->
            if (practiceType in LETTER_PRACTICE_TYPES) {
                kanjiContent[key] ?: Triple(key, "", "")
            } else {
                vocabContent[key] ?: Triple(key, "", "")
            }
        }

        return StatisticsCalculator.buildDayPracticeBreakdown(
            date = date,
            reviews = reviews,
            kanjiTypes = LETTER_PRACTICE_TYPES.toSet(),
            writingTypes = WRITING_PRACTICE_TYPES,
            contentResolver = resolver
        )
    }

    /** Structured mistakes recorded on the given local day. */
    suspend fun mistakesForDay(date: LocalDate): List<LearningMistake> {
        val start = date.atStartOfDayIn(zone)
        val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        return statisticsRepository.getLearningMistakes(start, end, 100)
    }

    /** Per-skill accuracy matrix (only real, tested skills are filled in). */
    suspend fun skillMatrix(): List<ua.syt0r.kanji.core.statistics.SkillMatrixRow> {
        val yearStart = today.minus(today.dayOfYear - 1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        suspend fun accuracyFor(types: List<Long>): Float? {
            val days = statisticsRepository.getReviewDayAggregationForType(yearStart, dayOffsetMillis, types)
            val total = days.sumOf { it.reviews }
            if (total == 0L) return null
            return days.sumOf { it.correct }.toFloat() / total
        }
        val kanjiReading = accuracyFor(listOf(SrsPracticeType.LetterReading.value))
        val kanjiWriting = accuracyFor(listOf(SrsPracticeType.LetterWriting.value))
        val vocabFlashcard = accuracyFor(listOf(SrsPracticeType.VocabFlashcard.value))
        val vocabReading = accuracyFor(listOf(SrsPracticeType.VocabReadingPicker.value))
        val vocabWriting = accuracyFor(listOf(SrsPracticeType.VocabWriting.value))
        return listOf(
            ua.syt0r.kanji.core.statistics.SkillMatrixRow(
                label = "Kanji", recognition = kanjiReading, reading = null, meaning = null, writing = kanjiWriting
            ),
            ua.syt0r.kanji.core.statistics.SkillMatrixRow(
                label = "Vocabulary", recognition = vocabFlashcard, reading = vocabReading, meaning = null, writing = vocabWriting
            )
        )
    }

    // ============================================================
    // Exam source items
    // ============================================================

    private suspend fun buildExamSourceItems(studiedWithinDays: Int?): List<ExamSourceItem> {
        val cutoff = if (studiedWithinDays != null) now - studiedWithinDays.days else null
        val result = mutableListOf<ExamSourceItem>()

        // Kanji from the studied catalog.
        val studiedKanji = dataCenter.srsCards.keys.toSet()
        val radicals = if (studiedKanji.isNotEmpty()) {
            runCatching { appDataRepository.getRadicalsForCharacters(studiedKanji.toList()) }
                .getOrDefault(emptyMap())
        } else emptyMap()

        dataCenter.cards.forEach { card ->
            if (card.id !in studiedKanji) return@forEach
            val srs = dataCenter.srsCards[card.id]
            if (cutoff != null && (srs?.lastReview ?: Instant.fromEpochMilliseconds(0)) < cutoff) return@forEach
            val jlpt = dataCenter.classifications[card.id]
                ?.firstNotNullOfOrNull { it.removePrefix("n").toIntOrNull() }
            result.add(
                ExamSourceItem(
                    key = card.id,
                    content = card.character,
                    reading = card.reading.split("・", " ").firstOrNull { it.isNotBlank() } ?: card.reading,
                    meaning = card.meaning,
                    jlptLevel = jlpt,
                    contentType = ContentTypes.KANJI,
                    radical = radicals[card.id]?.firstOrNull(),
                    strokeCount = dataCenter.strokeCounts[card.id],
                    studied = true
                )
            )
        }

        // Vocabulary from user decks.
        val entries = runCatching { vocabPracticeRepository.getAllCards() }.getOrDefault(emptyList())
        val jlptMapping = loadVocabJlptMapping()
        entries.forEach { entry ->
            val key = entry.cardId.toString()
            result.add(
                ExamSourceItem(
                    key = key,
                    content = entry.data.kanjiReading ?: entry.data.kanaReading,
                    reading = entry.data.kanaReading,
                    meaning = entry.data.meaning ?: entry.data.kanaReading,
                    jlptLevel = jlptMapping[key],
                    contentType = ContentTypes.VOCAB,
                    studied = true
                )
            )
        }
        return result
    }

    // ============================================================
    // Export
    // ============================================================

    /** Human-readable summary report (fully data backed). */
    suspend fun exportReport(): String = buildString {
        appendLine("KAITEYO — Statistics report")
        appendLine("Generated: ${today}")
        appendLine("")
        appendLine("— Overview —")
        appendLine("Total reviews: ${overview.totalReviews}")
        appendLine("Total study time: ${overview.totalStudyTime}")
        appendLine("Overall accuracy: ${(overview.overallAccuracy * 100).roundToInt()}%")
        appendLine("Current streak: ${overview.currentStreak} days")
        appendLine("Longest streak: ${overview.longestStreak} days")
        appendLine("Cards in library: ${overview.cards.total} (${overview.cards.mature} mature)")
        appendLine("")
        appendLine("— Knowledge —")
        appendLine("Kanji studied: ${kanjiKnowledge.studied} / ${kanjiKnowledge.totalCatalog}")
        appendLine("Kanji learned: ${kanjiKnowledge.learned} (${kanjiKnowledge.mature} mature, ${kanjiKnowledge.mastered} mastered)")
        appendLine("Vocab studied: ${vocabKnowledge.studied}")
        appendLine("Vocab learned: ${vocabKnowledge.learned}")
        appendLine("")
        appendLine("— JLPT coverage (kanji) —")
        kanjiKnowledge.jlptCoverage.forEach { coverage ->
            appendLine("N${coverage.level}: ${coverage.studied}/${coverage.total} studied (${(coverage.studiedRatio * 100).roundToInt()}%)")
        }
        appendLine("")
        appendLine("— Exams —")
        appendLine("Completed: ${examStatistics.completed}")
        appendLine("Average score: ${examStatistics.averageScore.roundToInt()}")
        appendLine("Best: ${examStatistics.highestScore}")
        appendLine("")
        appendLine("— Learning profile (data-backed) —")
        appendLine("Strongest area: ${learningProfile.strongestContentType ?: "—"}")
        appendLine("Weakest area: ${learningProfile.weakestContentType ?: "—"}")
        appendLine("Best skill: ${learningProfile.bestSkill ?: "—"}")
        appendLine("Weakest skill: ${learningProfile.weakestSkill ?: "—"}")
        appendLine("Lowest JLPT coverage: ${learningProfile.weakestJlptBand?.let { "N$it" } ?: "—"}")
        appendLine("Conclusion: ${learningProfile.conclusion}")
        appendLine("")
        appendLine("— Velocity (last ${velocity.windowDays} days) —")
        appendLine("Reviews per day: ${VelocityCalculator.oneDecimal(velocity.reviewsPerDay)}")
        appendLine("New items per week: ${VelocityCalculator.oneDecimal(velocity.newItemsPerWeek)}")
        appendLine("Study hours per week: ${VelocityCalculator.oneDecimal(velocity.studyHoursPerWeek)}")
        appendLine("Writing attempts per week: ${VelocityCalculator.oneDecimal(velocity.writingAttemptsPerWeek)}")
        velocity.examScoreDelta?.let {
            appendLine("Exam score change: ${VelocityCalculator.oneDecimal(it)} pts")
        }
    }

    /** Daily activity as CSV rows. */
    suspend fun exportCsv(): String = buildString {
        appendLine("date,reviews,new_cards,correct,incorrect,lapses,study_time_ms,writing_attempts,exams")
        val all = statisticsRepository.getDailyStatsAll()
        all.forEach { day ->
            appendLine(
                "${day.date},${day.reviews},${day.newCards},${day.correct},${day.incorrect}," +
                    "${day.lapses},${day.studyTime.inWholeMilliseconds},${day.writingAttempts},${day.examsTaken}"
            )
        }
    }

    /** JSON export of the main aggregates (no internal DB details). */
    suspend fun exportJson(): String {
        val heatmapYear = heatmaps[selectedYear]
        val body = buildJsonObject {
            put("generatedAt", today.toString())
            put("overview", buildJsonObject {
                put("totalReviews", overview.totalReviews)
                put("totalStudyTimeMs", overview.totalStudyTime.inWholeMilliseconds)
                put("overallAccuracy", overview.overallAccuracy)
                put("currentStreak", overview.currentStreak)
                put("longestStreak", overview.longestStreak)
                put("todayReviews", overview.today.reviews)
            })
            put("knowledge", buildJsonObject {
                put("kanjiStudied", kanjiKnowledge.studied)
                put("kanjiLearned", kanjiKnowledge.learned)
                put("kanjiMature", kanjiKnowledge.mature)
                put("vocabStudied", vocabKnowledge.studied)
                put("vocabLearned", vocabKnowledge.learned)
            })
            put("jlptCoverage", JsonArray(
                kanjiKnowledge.jlptCoverage.map { level ->
                    JsonPrimitive("N${level.level}:${level.studied}/${level.total}")
                }
            ))
            put("exams", JsonObject(
                exams.take(50).map { exam ->
                    exam.id.toString() to JsonPrimitive(
                        "${exam.title}|${exam.score}/${exam.questionCount}|${exam.startedAt}"
                    )
                }.toMap()
            ))
            put("learningProfile", buildJsonObject {
                put("strongestArea", learningProfile.strongestContentType ?: "")
                put("weakestArea", learningProfile.weakestContentType ?: "")
                put("bestSkill", learningProfile.bestSkill ?: "")
                put("weakestSkill", learningProfile.weakestSkill ?: "")
                put("weakestJlptBand", learningProfile.weakestJlptBand ?: 0)
                put("conclusion", learningProfile.conclusion)
            })
            put("velocity", buildJsonObject {
                put("windowDays", velocity.windowDays)
                put("reviewsPerDay", velocity.reviewsPerDay)
                put("newItemsPerWeek", velocity.newItemsPerWeek)
                put("studyHoursPerWeek", velocity.studyHoursPerWeek)
                put("writingAttemptsPerWeek", velocity.writingAttemptsPerWeek)
                put("examScoreDelta", velocity.examScoreDelta ?: 0f)
            })
            heatmapYear?.let {
                put("heatmapYear", it.year)
                put("heatmapActiveDays", it.activeDays)
            }
        }
        return json.encodeToString(body)
    }

    companion object {
        val LETTER_PRACTICE_TYPES = listOf(
            SrsPracticeType.LetterWriting.value,
            SrsPracticeType.LetterReading.value
        )
        val VOCAB_PRACTICE_TYPES = listOf(
            SrsPracticeType.VocabFlashcard.value,
            SrsPracticeType.VocabReadingPicker.value,
            SrsPracticeType.VocabWriting.value
        )
        val WRITING_PRACTICE_TYPES = setOf(
            SrsPracticeType.LetterWriting.value,
            SrsPracticeType.VocabWriting.value
        )
    }
}

private fun GeneratedExamQuestion.toRecord(examId: Long, index: Int): ExamQuestionRecord = ExamQuestionRecord(
    examId = examId,
    questionIndex = index,
    questionType = type.name,
    prompt = prompt,
    answer = answer,
    optionsJson = options?.let { kotlinx.serialization.json.Json.encodeToString(it) },
    userAnswer = null,
    isCorrect = null,
    timeMs = 0L,
    entityKey = entityKey,
    skill = type.skill,
    jlptLevel = jlptLevel,
    mistakeCategory = null
)
