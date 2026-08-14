package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryRepository
import ua.syt0r.kanji.core.user_data.database.StreakData
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract
import ua.syt0r.kanji.core.userdata.db.Exam
import ua.syt0r.kanji.core.userdata.db.Exam_question
import ua.syt0r.kanji.core.userdata.db.Learning_mistake
import ua.syt0r.kanji.core.userdata.db.Study_session
import ua.syt0r.kanji.core.userdata.db.UserData_statisticsQueries
import ua.syt0r.kanji.core.userdata.db.Writing_attempt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * SQLDelight-backed [StatisticsRepository].
 *
 * Review-history queries (already exposed by [ReviewHistoryRepository])
 * are delegated; everything else is served by the statistics tables
 * declared in `UserData_statistics.sq`.
 */
class SqlDelightStatisticsRepository(
    private val userDataDatabaseManager: UserDataDatabaseContract.Manager,
    private val reviewHistoryRepository: ReviewHistoryRepository
) : StatisticsRepository {

    // Queries declared in UserData_statistics.sq live on UserData_statisticsQueries,
    // so transactions here run against the whole database rather than just UserDataQueries.
    private suspend fun <T> readStats(block: UserData_statisticsQueries.() -> T): T =
        userDataDatabaseManager.readDatabaseTransaction { userData_statisticsQueries.block() }

    private suspend fun <T> writeStats(block: UserData_statisticsQueries.() -> T): T =
        userDataDatabaseManager.writeDatabaseTransaction { userData_statisticsQueries.block() }

    // ============ Review history ============

    override suspend fun getReviews(start: Instant, end: Instant): List<ReviewHistoryItem> =
        reviewHistoryRepository.getReviews(start, end)

    override suspend fun getTotalReviewsCount(): Long = reviewHistoryRepository.getTotalReviewsCount()

    override suspend fun getStreaks(timeOffset: LocalTime): List<StreakData> =
        reviewHistoryRepository.getStreaks(timeOffset)

    override suspend fun getTotalPracticeTime(singleReviewDurationLimit: Long): Duration =
        reviewHistoryRepository.getTotalPracticeTime(singleReviewDurationLimit)

    // ============ Aggregated review queries ============

    override suspend fun getReviewDayAggregation(start: Instant, offsetMillis: Long): List<DayAggregate> =
        readStats {
            reviewDayAggregation(start.toEpochMilliseconds(), offsetMillis).executeAsList().map {
                DayAggregate(
                    date = it.day ?: "",
                    reviews = it.reviews,
                    correct = it.correct ?: 0L,
                    incorrect = it.incorrect ?: 0L,
                    lapses = it.lapses ?: 0L,
                    studyTimeMs = it.study_time_ms ?: 0L,
                    cardsStudied = it.cards_studied ?: 0L
                )
            }
        }

    override suspend fun getReviewDayAggregationForType(
        start: Instant,
        offsetMillis: Long,
        practiceTypes: List<Long>
    ): List<DayAggregateForType> = readStats {
        reviewDayAggregationForType(start.toEpochMilliseconds(), offsetMillis, practiceTypes).executeAsList().map {
            DayAggregateForType(
                date = it.day ?: "",
                reviews = it.reviews,
                correct = it.correct ?: 0L,
                studyTimeMs = it.study_time_ms ?: 0L,
                cardsStudied = it.cards_studied ?: 0L
            )
        }
    }

    override suspend fun getGradeBreakdown(start: Instant, end: Instant?): GradeDistribution {
        val rows: List<Pair<Long, Long>> = readStats {
            if (end == null) {
                reviewGradeBreakdown(start.toEpochMilliseconds()).executeAsList().map { it.grade to it.count }
            } else {
                reviewGradeBreakdownForRange(
                    start.toEpochMilliseconds(),
                    end.toEpochMilliseconds()
                ).executeAsList().map { it.grade to it.count }
            }
        }
        var again = 0L
        var hard = 0L
        var good = 0L
        var easy = 0L
        rows.forEach { (grade, count) ->
            when (grade.toInt()) {
                0 -> again = count
                1 -> hard = count
                2 -> good = count
                3 -> easy = count
            }
        }
        return GradeDistribution(again, hard, good, easy)
    }

    override suspend fun getIntervalBreakdown(
        start: Instant,
        practiceTypes: List<Long>
    ): List<IntervalBucket> = readStats {
        reviewIntervalBreakdown(start.toEpochMilliseconds(), practiceTypes).executeAsList().map {
            IntervalBucket(
                label = intervalLabel(it.bucket.toInt()),
                minDays = it.bucket.toInt(),
                count = it.count
            )
        }
    }

    override suspend fun getFirstReviewTime(): Instant? = readStats {
        reviewFirstSeen().executeAsOneOrNull()?.first_seen?.let { Instant.fromEpochMilliseconds(it) }
    }

    override suspend fun getLapseStats(start: Instant): Pair<Long, Long> {
        val row = readStats {
            reviewLapseStats(start.toEpochMilliseconds()).executeAsOne()
        }
        return (row.lapsed_items ?: 0L) to (row.lapsed_reviews ?: 0L)
    }

    override suspend fun getReviewPerItem(start: Instant): List<ItemReviewTotals> =
        readStats {
            reviewPerItem(start.toEpochMilliseconds()).executeAsList().map {
                ItemReviewTotals(
                    key = it.key,
                    practiceType = it.practice_type,
                    reviews = it.reviews,
                    correct = it.correct ?: 0L
                )
            }
        }

    // ============ Study sessions ============

    override suspend fun insertStudySession(session: StudySessionRecord): Long =
        writeStats {
            insertStudySession(
                start_time = session.startTime.toEpochMilliseconds(),
                end_time = session.endTime?.toEpochMilliseconds(),
                duration_ms = session.duration.inWholeMilliseconds,
                practice_type = session.practiceType,
                mode = session.mode,
                deck_id = session.deckId,
                deck_name = session.deckName,
                items_studied = session.itemsStudied.toLong(),
                new_items = session.newItems.toLong(),
                review_items = session.reviewItems.toLong(),
                correct = session.correct.toLong(),
                incorrect = session.incorrect.toLong(),
                is_complete = if (session.isComplete) 1L else 0L
            )
            getLastInsertRowId().executeAsOne()
        }

    override suspend fun completeStudySession(
        id: Long,
        endTime: Instant,
        duration: Duration,
        itemsStudied: Int,
        newItems: Int,
        reviewItems: Int,
        correct: Int,
        incorrect: Int
    ) = writeStats {
        completeStudySession(
            end_time = endTime.toEpochMilliseconds(),
            duration_ms = duration.inWholeMilliseconds,
            items_studied = itemsStudied.toLong(),
            new_items = newItems.toLong(),
            review_items = reviewItems.toLong(),
            correct = correct.toLong(),
            incorrect = incorrect.toLong(),
            id = id
        )
    }

    override suspend fun updateStudySessionCounters(
        sessionId: Long,
        deltaItems: Int,
        deltaNew: Int,
        deltaReview: Int,
        deltaCorrect: Int,
        deltaIncorrect: Int,
        durationMs: Long
    ) = writeStats {
        updateStudySessionCounters(
            delta_items = deltaItems.toLong(),
            delta_new = deltaNew.toLong(),
            delta_review = deltaReview.toLong(),
            delta_correct = deltaCorrect.toLong(),
            delta_incorrect = deltaIncorrect.toLong(),
            duration_ms = durationMs,
            session_id = sessionId
        )
    }

    override suspend fun getStudySessions(start: Instant, end: Instant): List<StudySessionRecord> =
        readStats {
            getStudySessions(start.toEpochMilliseconds(), end.toEpochMilliseconds()).executeAsList().map { it.toRecord() }
        }

    override suspend fun getAllStudySessions(limit: Int): List<StudySessionRecord> =
        readStats {
            getAllStudySessions(limit.toLong()).executeAsList().map { it.toRecord() }
        }

    // ============ Writing attempts ============

    override suspend fun insertWritingAttempt(attempt: WritingAttemptRecord) =
        writeStats {
            insertWritingAttempt(
                character = attempt.character,
                practice_type = attempt.practiceType,
                timestamp = attempt.timestamp.toEpochMilliseconds(),
                deck_id = attempt.deckId,
                session_id = attempt.sessionId,
                stroke_count = attempt.strokeCount.toLong(),
                mistakes = attempt.mistakes.toLong(),
                wrong_order = attempt.wrongOrder.toLong(),
                almost = attempt.almost.toLong(),
                accuracy = attempt.accuracy?.toDouble()
            )
        }

    override suspend fun getWritingAttempts(start: Instant, end: Instant): List<WritingAttemptRecord> =
        readStats {
            getWritingAttempts(start.toEpochMilliseconds(), end.toEpochMilliseconds()).executeAsList().map { it.toRecord() }
        }

    override suspend fun getWritingAttemptsForCharacter(character: String, limit: Int): List<WritingAttemptRecord> =
        readStats {
            getWritingAttemptsForCharacter(character, limit.toLong()).executeAsList().map { it.toRecord() }
        }

    override suspend fun getWritingSummaryForCharacter(character: String): WritingCharacterStats =
        readStats {
            val row = getWritingSummaryForCharacter(character).executeAsOne()
            WritingCharacterStats(
                character = character,
                attempts = (row.attempts ?: 0L).toInt(),
                correct = (row.correct ?: 0L).toInt(),
                mistakes = (row.total_mistakes ?: 0L).toInt(),
                wrongOrder = (row.wrong_order ?: 0L).toInt(),
                almost = (row.almost ?: 0L).toInt()
            )
        }

    override suspend fun getWritingVerifiedCharacters(): Set<String> =
        readStats {
            getWritingVerifiedCharacters().executeAsList().toSet()
        }

    // ============ Exams ============

    override suspend fun insertExam(exam: ExamRecord): Long = writeStats {
        insertExam(
            title = exam.title,
            exam_type = exam.examType,
            scope_json = exam.scopeJson,
            question_count = exam.questionCount.toLong(),
            time_limit_ms = exam.timeLimitMs,
            seed = exam.seed,
            started_at = exam.startedAt.toEpochMilliseconds(),
            finished_at = exam.finishedAt?.toEpochMilliseconds(),
            status = exam.status.dbValue.toLong(),
            score = exam.score.toLong(),
            accuracy = exam.accuracy.toDouble(),
            total_time_ms = exam.totalTimeMs
        )
        getLastInsertRowId().executeAsOne()
    }

    override suspend fun insertExamQuestion(question: ExamQuestionRecord) =
        writeStats {
            insertExamQuestion(
                exam_id = question.examId,
                question_index = question.questionIndex.toLong(),
                question_type = question.questionType,
                prompt = question.prompt,
                answer = question.answer,
                options_json = question.optionsJson,
                user_answer = question.userAnswer,
                is_correct = question.isCorrect?.let { if (it) 1L else 0L },
                time_ms = question.timeMs,
                entity_key = question.entityKey,
                skill = question.skill,
                jlpt_level = question.jlptLevel?.toLong(),
                mistake_category = question.mistakeCategory
            )
        }

    override suspend fun completeExam(id: Long, score: Int, accuracy: Float, totalTimeMs: Long) =
        writeStats {
            completeExam(
                finished_at = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                score = score.toLong(),
                accuracy = accuracy.toDouble(),
                total_time_ms = totalTimeMs,
                id = id
            )
        }

    override suspend fun abandonExam(id: Long) = writeStats {
        abandonExam(
            finished_at = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            id = id
        )
    }

    override suspend fun getExams(limit: Int): List<ExamRecord> = readStats {
        getExams(limit.toLong()).executeAsList().map { it.toRecord() }
    }

    override suspend fun getExam(id: Long): ExamRecord? = readStats {
        getExam(id).executeAsOneOrNull()?.toRecord()
    }

    override suspend fun getCompletedExams(limit: Int): List<ExamRecord> =
        readStats {
            getCompletedExams(limit.toLong()).executeAsList().map { it.toRecord() }
        }

    override suspend fun getExamQuestions(examId: Long): List<ExamQuestionRecord> =
        readStats {
            getExamQuestions(examId).executeAsList().map { it.toRecord() }
        }

    override suspend fun updateExamQuestionAnswer(
        questionId: Long,
        userAnswer: String,
        isCorrect: Boolean,
        timeMs: Long,
        mistakeCategory: String
    ) = writeStats {
        updateExamQuestionAnswer(
            user_answer = userAnswer,
            is_correct = if (isCorrect) 1L else 0L,
            time_ms = timeMs,
            mistake_category = mistakeCategory,
            id = questionId
        )
    }

    override suspend fun deleteExam(id: Long) = writeStats {
        deleteExam(id)
    }

    // ============ Learning mistakes ============

    override suspend fun insertLearningMistake(mistake: LearningMistake) =
        writeStats {
            insertLearningMistake(
                timestamp = mistake.timestamp.toEpochMilliseconds(),
                entity_key = mistake.entityKey,
                content_type = mistake.contentType,
                mode = mistake.mode,
                question_type = mistake.questionType,
                expected = mistake.expected,
                actual_ = mistake.actual,
                category = mistake.category,
                severity = mistake.severity.toLong(),
                session_id = mistake.sessionId,
                exam_id = mistake.examId,
                deck_id = mistake.deckId
            )
        }

    override suspend fun getLearningMistakes(start: Instant, end: Instant, limit: Int): List<LearningMistake> =
        readStats {
            getLearningMistakes(start.toEpochMilliseconds(), end.toEpochMilliseconds(), limit.toLong())
                .executeAsList().map { it.toRecord() }
        }

    override suspend fun getMistakeCount(): Long = readStats {
        getMistakeCount().executeAsOne()
    }

    override suspend fun getWeakEntities(start: Instant, limit: Int): List<Pair<String, Pair<String, Long>>> =
        readStats {
            getWeakEntities(start.toEpochMilliseconds(), limit.toLong()).executeAsList().map {
                it.entity_key to (it.content_type to it.mistake_count)
            }
        }

    override suspend fun getMistakeCategories(): List<Pair<String, Long>> =
        readStats {
            getMistakeCategoryBreakdown().executeAsList().map { it.category to it.count }
        }

    // ============ Daily rollups ============

    override suspend fun incrementDailyReview(
        date: LocalDate,
        reviews: Int,
        newCards: Int,
        reviewCards: Int,
        correct: Int,
        incorrect: Int,
        lapses: Int,
        studyTimeMs: Long
    ) = writeStats {
        ensureDailyStatsRow(date.toString())
        incrementDailyReview(
            date = date.toString(),
            reviews = reviews.toLong(),
            new_cards = newCards.toLong(),
            review_cards = reviewCards.toLong(),
            correct = correct.toLong(),
            incorrect = incorrect.toLong(),
            lapses = lapses.toLong(),
            study_time_ms = studyTimeMs
        )
    }

    override suspend fun incrementDailyWriting(date: LocalDate, attempts: Int, correct: Int, studyTimeMs: Long) =
        writeStats {
            ensureDailyStatsRow(date.toString())
            incrementDailyWriting(
                date = date.toString(),
                attempts = attempts.toLong(),
                correct = correct.toLong(),
                study_time_ms = studyTimeMs
            )
        }

    override suspend fun incrementDailyExam(date: LocalDate, score: Int) =
        writeStats {
            ensureDailyStatsRow(date.toString())
            incrementDailyExam(date = date.toString(), score = score.toLong())
        }

    override suspend fun incrementDailySession(date: LocalDate) =
        writeStats {
            ensureDailyStatsRow(date.toString())
            incrementDailySession(date.toString())
        }

    override suspend fun getDailyStats(start: LocalDate, end: LocalDate): List<DailyActivity> =
        readStats {
            getDailyStats(start.toString(), end.toString()).executeAsList().map { it.toActivity() }
        }

    override suspend fun getDailyStatsAll(): List<DailyActivity> = readStats {
        getDailyStatsAll().executeAsList().map { it.toActivity() }
    }

    // ============ Mappers ============

    private fun Study_session.toRecord(): StudySessionRecord = StudySessionRecord(
        id = id,
        startTime = Instant.fromEpochMilliseconds(start_time),
        endTime = end_time?.let { Instant.fromEpochMilliseconds(it) },
        duration = duration_ms.milliseconds,
        practiceType = practice_type,
        mode = mode,
        deckId = deck_id,
        deckName = deck_name,
        itemsStudied = items_studied.toInt(),
        newItems = new_items.toInt(),
        reviewItems = review_items.toInt(),
        correct = correct.toInt(),
        incorrect = incorrect.toInt(),
        isComplete = is_complete != 0L
    )

    private fun Writing_attempt.toRecord(): WritingAttemptRecord = WritingAttemptRecord(
        id = id,
        character = character,
        practiceType = practice_type,
        timestamp = Instant.fromEpochMilliseconds(timestamp),
        deckId = deck_id,
        sessionId = session_id,
        strokeCount = stroke_count.toInt(),
        mistakes = mistakes.toInt(),
        wrongOrder = wrong_order.toInt(),
        almost = almost.toInt(),
        accuracy = accuracy?.toFloat()
    )

    private fun Exam.toRecord(): ExamRecord = ExamRecord(
        id = id,
        title = title,
        examType = exam_type,
        scopeJson = scope_json,
        questionCount = question_count.toInt(),
        timeLimitMs = time_limit_ms,
        seed = seed,
        startedAt = Instant.fromEpochMilliseconds(started_at),
        finishedAt = finished_at?.let { Instant.fromEpochMilliseconds(it) },
        status = ExamStatus.from(status.toInt()),
        score = score.toInt(),
        accuracy = accuracy.toFloat(),
        totalTimeMs = total_time_ms
    )

    private fun Exam_question.toRecord(): ExamQuestionRecord = ExamQuestionRecord(
        id = id,
        examId = exam_id,
        questionIndex = question_index.toInt(),
        questionType = question_type,
        prompt = prompt,
        answer = answer,
        optionsJson = options_json,
        userAnswer = user_answer,
        isCorrect = is_correct?.let { it != 0L },
        timeMs = time_ms,
        entityKey = entity_key,
        skill = skill,
        jlptLevel = jlpt_level?.toInt(),
        mistakeCategory = mistake_category
    )

    private fun Learning_mistake.toRecord(): LearningMistake = LearningMistake(
        id = id,
        timestamp = Instant.fromEpochMilliseconds(timestamp),
        entityKey = entity_key,
        contentType = content_type,
        mode = mode,
        questionType = question_type,
        expected = expected,
        actual = actual_,
        category = category,
        severity = severity.toInt(),
        sessionId = session_id,
        examId = exam_id,
        deckId = deck_id
    )

    private fun ua.syt0r.kanji.core.userdata.db.Daily_stats.toActivity(): DailyActivity {
        val date = runCatching { LocalDate.parse(date) }.getOrNull()
        return DailyActivity(
            date = date,
            reviews = reviews.toInt(),
            newCards = new_cards.toInt(),
            reviewCards = review_cards.toInt(),
            correct = correct.toInt(),
            incorrect = incorrect.toInt(),
            lapses = lapses.toInt(),
            studyTime = study_time_ms.milliseconds,
            writingAttempts = writing_attempts.toInt(),
            writingCorrect = writing_correct.toInt(),
            examsTaken = exams_taken.toInt(),
            examScoreSum = exam_score_sum.toInt(),
            examScoreCount = exam_score_count.toInt(),
            sessions = sessions.toInt()
        )
    }

    private fun intervalLabel(bucket: Int): String = when (bucket) {
        0 -> "<1d"
        1 -> "1-3d"
        7 -> "4-7d"
        14 -> "8-14d"
        30 -> "15-30d"
        90 -> "31-90d"
        365 -> "91-365d"
        else -> "1y+"
    }
}
