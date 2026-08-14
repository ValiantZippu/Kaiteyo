package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import ua.syt0r.kanji.core.srs.SrsPracticeType
import ua.syt0r.kanji.core.time.TimeUtils
import kotlin.time.Duration

/**
 * Records study activity as it happens: study sessions, per-review
 * counters, writing attempts, failed-review mistakes and the
 * precomputed daily rollups (so the heatmap and today panel stay
 * cheap). All data ends up in the user database through
 * [StatisticsRepository] — nothing is kept in memory only.
 *
 * There is exactly one active session at a time (the current practice
 * queue); starting a new session finalizes the previous one.
 */
class StatisticsRecorder(
    private val statisticsRepository: StatisticsRepository,
    private val timeUtils: TimeUtils
) {

    private class ActiveSession(
        val id: Long,
        val practiceType: Long,
        val mode: String,
        val deckId: Long,
        val deckName: String,
        val startTime: Instant
    ) {
        var items = 0
        var newItems = 0
        var reviewItems = 0
        var correct = 0
        var incorrect = 0
        var reviewTimeMs = 0L
    }

    private var activeSession: ActiveSession? = null

    private val localDate: kotlinx.datetime.LocalDate
        get() = timeUtils.getCurrentTime().date

    // ---- Session lifecycle ----

    suspend fun startSession(practiceType: Long, deckId: Long, deckName: String = ""): Long {
        finishSession()
        val now = timeUtils.now()
        val mode = modeForPracticeType(practiceType)
        val id = statisticsRepository.insertStudySession(
            StudySessionRecord(
                startTime = now,
                practiceType = practiceType,
                mode = mode,
                deckId = deckId,
                deckName = deckName
            )
        )
        statisticsRepository.incrementDailySession(localDate)
        activeSession = ActiveSession(id, practiceType, mode, deckId, deckName, now)
        return id
    }

    suspend fun finishSession() {
        val session = activeSession ?: return
        activeSession = null
        val now = timeUtils.now()
        val wallDuration = now - session.startTime
        statisticsRepository.completeStudySession(
            id = session.id,
            endTime = now,
            duration = wallDuration,
            itemsStudied = session.items,
            newItems = session.newItems,
            reviewItems = session.reviewItems,
            correct = session.correct,
            incorrect = session.incorrect
        )
    }

    /** Discard the current session counters when the practice flow is abandoned. */
    suspend fun cancelSession() {
        activeSession = null
    }

    // ---- Review recording ----

    /**
     * Called for every answered review. Updates the active session and
     * the daily rollup, and persists a structured mistake record when
     * the answer was a failure (grade Again/Hard-0).
     */
    suspend fun recordReview(
        key: String,
        practiceType: Long,
        isNew: Boolean,
        isCorrect: Boolean,
        mistakes: Int,
        duration: Duration,
        deckId: Long
    ) {
        val now = timeUtils.now()
        val session = activeSession

        session?.let { s ->
            s.items += 1
            if (isNew) s.newItems += 1 else s.reviewItems += 1
            if (isCorrect) s.correct += 1 else s.incorrect += 1
            s.reviewTimeMs += duration.inWholeMilliseconds
            statisticsRepository.updateStudySessionCounters(
                sessionId = s.id,
                deltaItems = 1,
                deltaNew = if (isNew) 1 else 0,
                deltaReview = if (isNew) 0 else 1,
                deltaCorrect = if (isCorrect) 1 else 0,
                deltaIncorrect = if (isCorrect) 0 else 1,
                durationMs = s.reviewTimeMs
            )
        }

        statisticsRepository.incrementDailyReview(
            date = localDate,
            reviews = 1,
            newCards = if (isNew) 1 else 0,
            reviewCards = if (isNew) 0 else 1,
            correct = if (isCorrect) 1 else 0,
            incorrect = if (isCorrect) 0 else 1,
            lapses = if (isCorrect) 0 else 1,
            studyTimeMs = duration.inWholeMilliseconds
        )

        if (!isCorrect) {
            statisticsRepository.insertLearningMistake(
                LearningMistake(
                    timestamp = now,
                    entityKey = key,
                    contentType = contentTypeForPracticeType(practiceType),
                    mode = modeForPracticeType(practiceType),
                    questionType = "srs-review",
                    category = "again",
                    severity = 1,
                    sessionId = session?.id,
                    deckId = deckId
                )
            )
        }
    }

    // ---- Writing attempts ----

    suspend fun recordWritingAttempt(
        character: String,
        practiceType: Long,
        strokeCount: Int,
        mistakes: Int,
        wrongOrder: Int,
        almost: Int,
        accuracy: Float?,
        deckId: Long,
        studyTimeMs: Long
    ) {
        val attempt = WritingAttemptRecord(
            character = character,
            practiceType = practiceType,
            timestamp = timeUtils.now(),
            deckId = deckId,
            sessionId = activeSession?.id,
            strokeCount = strokeCount,
            mistakes = mistakes,
            wrongOrder = wrongOrder,
            almost = almost,
            accuracy = accuracy
        )
        statisticsRepository.insertWritingAttempt(attempt)
        statisticsRepository.incrementDailyWriting(
            date = localDate,
            attempts = 1,
            correct = if ((accuracy ?: 0f) >= 0.7f) 1 else 0,
            studyTimeMs = studyTimeMs
        )
        if ((accuracy ?: 0f) < 0.7f) {
            statisticsRepository.insertLearningMistake(
                LearningMistake(
                    timestamp = attempt.timestamp,
                    entityKey = character,
                    contentType = contentTypeForPracticeType(practiceType),
                    mode = StudyModes.WRITING,
                    questionType = "writing",
                    category = when {
                        wrongOrder > 0 -> "wrong_stroke_order"
                        mistakes > 0 -> "wrong_stroke"
                        else -> "shape"
                    },
                    severity = 1,
                    sessionId = activeSession?.id,
                    deckId = deckId
                )
            )
        }
    }

    // ---- Exams ----

    suspend fun recordExamCompletion(date: kotlinx.datetime.LocalDate, score: Int) {
        statisticsRepository.incrementDailyExam(date, score)
    }

    // ---- Mapping helpers (shared definitions) ----

    fun modeForPracticeType(practiceType: Long): String = when (practiceType) {
        SrsPracticeType.LetterWriting.value -> StudyModes.WRITING
        SrsPracticeType.LetterReading.value -> StudyModes.READING
        SrsPracticeType.VocabFlashcard.value -> StudyModes.FLASHCARD
        SrsPracticeType.VocabReadingPicker.value -> StudyModes.READING
        SrsPracticeType.VocabWriting.value -> StudyModes.WRITING
        else -> StudyModes.FLASHCARD
    }

    fun contentTypeForPracticeType(practiceType: Long): String = when (practiceType) {
        SrsPracticeType.LetterWriting.value,
        SrsPracticeType.LetterReading.value -> ContentTypes.KANJI

        SrsPracticeType.VocabFlashcard.value,
        SrsPracticeType.VocabReadingPicker.value,
        SrsPracticeType.VocabWriting.value -> ContentTypes.VOCAB

        else -> ContentTypes.KANJI
    }
}
