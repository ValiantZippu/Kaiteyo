package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

/** Aggregated day bucket returned by the SQL review aggregation. */
data class DayAggregate(
    val date: String,
    val reviews: Long,
    val correct: Long,
    val incorrect: Long,
    val lapses: Long,
    val studyTimeMs: Long,
    val cardsStudied: Long
)

/** Aggregated day bucket filtered to one practice type group. */
data class DayAggregateForType(
    val date: String,
    val reviews: Long,
    val correct: Long,
    val studyTimeMs: Long,
    val cardsStudied: Long
)

/** Per-item review totals used for accuracy computations. */
data class ItemReviewTotals(
    val key: String,
    val practiceType: Long,
    val reviews: Long,
    val correct: Long
)

/** Row for a persisted writing attempt. */
data class WritingAttemptRecord(
    val id: Long = 0,
    val character: String,
    val practiceType: Long,
    val timestamp: Instant,
    val deckId: Long = 0,
    val sessionId: Long? = null,
    val strokeCount: Int = 0,
    val mistakes: Int = 0,
    val wrongOrder: Int = 0,
    val almost: Int = 0,
    val accuracy: Float? = null
)

/** Row for a persisted exam question. */
data class ExamQuestionRecord(
    val id: Long = 0,
    val examId: Long,
    val questionIndex: Int,
    val questionType: String,
    val prompt: String,
    val answer: String,
    val optionsJson: String?,
    val userAnswer: String?,
    val isCorrect: Boolean?,
    val timeMs: Long,
    val entityKey: String,
    val skill: String,
    val jlptLevel: Int?,
    val mistakeCategory: String?
)

/** In-progress exam holder used while the user answers questions. */
data class InProgressExam(
    val exam: ExamRecord,
    val questions: List<ExamQuestionRecord>
) {
    val answeredCount: Int get() = questions.count { it.userAnswer != null }
    val correctCount: Int get() = questions.count { it.isCorrect == true }
}

/**
 * Data access layer for the statistics & examination system.
 * All reads go through real database queries; nothing is mocked.
 */
interface StatisticsRepository {

    // ---- Review history (raw access for the calculator) ----

    suspend fun getReviews(start: Instant, end: Instant): List<ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem>
    suspend fun getTotalReviewsCount(): Long
    suspend fun getStreaks(timeOffset: LocalTime): List<ua.syt0r.kanji.core.user_data.database.StreakData>
    suspend fun getTotalPracticeTime(singleReviewDurationLimit: Long): Duration

    // ---- Aggregated review queries ----

    suspend fun getReviewDayAggregation(start: Instant, offsetMillis: Long): List<DayAggregate>
    suspend fun getReviewDayAggregationForType(start: Instant, offsetMillis: Long, practiceTypes: List<Long>): List<DayAggregateForType>
    suspend fun getGradeBreakdown(start: Instant, end: Instant?): GradeDistribution
    suspend fun getIntervalBreakdown(start: Instant, practiceTypes: List<Long>): List<IntervalBucket>
    suspend fun getFirstReviewTime(): Instant?
    suspend fun getLapseStats(start: Instant): Pair<Long, Long>
    suspend fun getReviewPerItem(start: Instant): List<ItemReviewTotals>

    // ---- Study sessions ----

    suspend fun insertStudySession(session: StudySessionRecord): Long
    suspend fun completeStudySession(
        id: Long,
        endTime: Instant,
        duration: Duration,
        itemsStudied: Int,
        newItems: Int,
        reviewItems: Int,
        correct: Int,
        incorrect: Int
    )
    suspend fun updateStudySessionCounters(
        sessionId: Long,
        deltaItems: Int,
        deltaNew: Int,
        deltaReview: Int,
        deltaCorrect: Int,
        deltaIncorrect: Int,
        durationMs: Long
    )
    suspend fun getStudySessions(start: Instant, end: Instant): List<StudySessionRecord>
    suspend fun getAllStudySessions(limit: Int): List<StudySessionRecord>

    // ---- Writing attempts ----

    suspend fun insertWritingAttempt(attempt: WritingAttemptRecord)
    suspend fun getWritingAttempts(start: Instant, end: Instant): List<WritingAttemptRecord>
    suspend fun getWritingAttemptsForCharacter(character: String, limit: Int): List<WritingAttemptRecord>
    suspend fun getWritingSummaryForCharacter(character: String): WritingCharacterStats
    suspend fun getWritingVerifiedCharacters(): Set<String>

    // ---- Exams ----

    suspend fun insertExam(exam: ExamRecord): Long
    suspend fun insertExamQuestion(question: ExamQuestionRecord)
    suspend fun completeExam(id: Long, score: Int, accuracy: Float, totalTimeMs: Long)
    suspend fun abandonExam(id: Long)
    suspend fun getExams(limit: Int): List<ExamRecord>
    suspend fun getExam(id: Long): ExamRecord?
    suspend fun getCompletedExams(limit: Int): List<ExamRecord>
    suspend fun getExamQuestions(examId: Long): List<ExamQuestionRecord>
    suspend fun updateExamQuestionAnswer(questionId: Long, userAnswer: String, isCorrect: Boolean, timeMs: Long, mistakeCategory: String)
    suspend fun deleteExam(id: Long)

    // ---- Learning mistakes ----

    suspend fun insertLearningMistake(mistake: LearningMistake)
    suspend fun getLearningMistakes(start: Instant, end: Instant, limit: Int): List<LearningMistake>
    suspend fun getMistakeCount(): Long
    suspend fun getWeakEntities(start: Instant, limit: Int): List<Pair<String, Pair<String, Long>>>
    suspend fun getMistakeCategories(): List<Pair<String, Long>>

    // ---- Precomputed daily rollups ----

    suspend fun incrementDailyReview(
        date: LocalDate,
        reviews: Int,
        newCards: Int,
        reviewCards: Int,
        correct: Int,
        incorrect: Int,
        lapses: Int,
        studyTimeMs: Long
    )
    suspend fun incrementDailyWriting(date: LocalDate, attempts: Int, correct: Int, studyTimeMs: Long)
    suspend fun incrementDailyExam(date: LocalDate, score: Int)
    suspend fun incrementDailySession(date: LocalDate)
    suspend fun getDailyStats(start: LocalDate, end: LocalDate): List<DailyActivity>
    suspend fun getDailyStatsAll(): List<DailyActivity>
}
