package ua.syt0r.kanji.presentation.screen.main.screen.practice_common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.debounceFirst
import ua.syt0r.kanji.core.srs.SrsAnswers
import ua.syt0r.kanji.core.srs.SrsCard
import ua.syt0r.kanji.core.srs.SrsCardKey
import ua.syt0r.kanji.core.srs.SrsCardRepository
import ua.syt0r.kanji.core.srs.SrsScheduler
import ua.syt0r.kanji.core.statistics.StatisticsRecorder
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryRepository
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


interface PracticeQueue<State, Descriptor> {

    val state: StateFlow<State>

    suspend fun initialize(items: List<Descriptor>)
    suspend fun submitAnswer(answer: PracticeAnswer)
    fun immediateFinish()

}

interface PracticeQueueItem<T : PracticeQueueItem<T>> {

    val srsCardKey: SrsCardKey
    val srsCard: SrsCard
    val deckId: Long
    val repeats: Int
    val totalMistakes: Int
    val data: Deferred<Any>

    fun copyForRepeat(answer: PracticeAnswer): T

}

data class PracticeQueueProgress(
    val pending: Int,
    val repeats: Int,
    val completed: Int
)

interface PracticeSummaryItem {
    val totalReviews: Deferred<Int>
    val nextInterval: Duration
}

/**
 * The two review-settings fields the queue reads to keep study time honest.
 * Decoded straight from [PreferencesContract.AppPreferences.reviewSettingsJson]
 * with defaults, so old saved JSON (without these keys) behaves as "on".
 */
@Serializable
private data class StudyTimingData(
    val smartActivityDetection: Boolean = true,
    val inactivityThresholdMinutes: Int = 10
)

private val studyTimingJson = Json { ignoreUnknownKeys = true }

abstract class BasePracticeQueue<State, Descriptor, QueueItem, SummaryItem>(
    private val practiceScope: CoroutineScope,
    protected val timeUtils: TimeUtils,
    protected val srsScheduler: SrsScheduler,
    protected val srsCardRepository: SrsCardRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    private val statisticsRecorder: StatisticsRecorder,
    analyticsManager: AnalyticsManager,
    private val appPreferences: PreferencesContract.AppPreferences
) : PracticeQueue<State, Descriptor>
        where QueueItem : PracticeQueueItem<QueueItem>,
              SummaryItem : PracticeSummaryItem {

    protected open lateinit var queue: MutableList<QueueItem>
    protected val summaryItems = mutableMapOf<SrsCardKey, SummaryItem>()

    protected lateinit var practiceStartInstant: Instant
    private lateinit var currentReviewStartInstant: Instant

    private val submittedAnswersChannel = Channel<PracticeAnswer>()

    private val _state: MutableStateFlow<State> = MutableStateFlow(value = this.getLoadingState())
    override val state: StateFlow<State> = _state

    private val reviewReporter = PracticeReviewReporter(analyticsManager)

    init {
        submittedAnswersChannel.consumeAsFlow()
            .debounceFirst()
            .onEach { handleAnswer(it) }
            .launchIn(practiceScope)
    }

    protected abstract suspend fun Descriptor.toQueueItem(): QueueItem
    protected abstract fun createSummaryItem(
        queueItem: QueueItem,
        totalReviews: Deferred<Int>
    ): SummaryItem

    protected abstract fun getLoadingState(): State
    protected abstract suspend fun getReviewState(item: QueueItem, answers: PracticeAnswers): State
    protected abstract fun getSummaryState(): State

    override suspend fun initialize(items: List<Descriptor>) {
        practiceStartInstant = timeUtils.now()
        queue = items.map { it.toQueueItem() }.toMutableList()
        // Begin a statistics session for the practice flow (finalized on finish).
        if (items.isNotEmpty()) {
            val firstItem = queue.first()
            statisticsRecorder.startSession(
                practiceType = firstItem.srsCardKey.practiceType,
                deckId = firstItem.deckId
            )
        }
        updateState()
    }

    override suspend fun submitAnswer(answer: PracticeAnswer) {
        submittedAnswersChannel.send(answer)
    }

    override fun immediateFinish() {
        practiceScope.launch { statisticsRecorder.finishSession() }
        val isLoading = summaryItems.any { it.value.totalReviews.isCompleted.not() }
        if (isLoading) {
            _state.value = getLoadingState()
            practiceScope.launch {
                summaryItems.forEach { it.value.totalReviews.await() }
                _state.value = getSummaryState()
            }
        } else _state.value = getSummaryState()
    }

    protected fun getProgress(): PracticeQueueProgress {
        return PracticeQueueProgress(
            pending = queue.count { it.repeats == 0 },
            repeats = queue.count { it.repeats > 0 },
            completed = summaryItems.filter { it.value.nextInterval >= 1.days }.size
        )
    }

    private fun getAnswers(answers: SrsAnswers): PracticeAnswers {
        return PracticeAnswers(
            again = PracticeAnswer(answers.again),
            hard = PracticeAnswer(answers.hard),
            good = PracticeAnswer(answers.good),
            easy = PracticeAnswer(answers.easy)
        )
    }

    /**
     * Cap a review duration at the configured inactivity threshold when smart
     * activity detection is on. A review that took far longer than a normal
     * card (user stepped away mid-card, app left open) is almost all idle
     * time — counting it inflates "time studied" with time nobody studied.
     */
    private suspend fun activeReviewDuration(raw: Duration): Duration {
        val json = appPreferences.reviewSettingsJson.get()
        if (json.isBlank()) return raw
        val timing = runCatching {
            studyTimingJson.decodeFromString<StudyTimingData>(json)
        }.getOrNull() ?: return raw
        if (!timing.smartActivityDetection) return raw
        val threshold = timing.inactivityThresholdMinutes.coerceIn(1, 120).minutes
        return if (raw > threshold) threshold else raw
    }

    private suspend fun handleAnswer(answer: PracticeAnswer) {
        val item = queue.removeFirstOrNull() ?: return
        val updatedItem = item.copyForRepeat(answer)

        saveSummaryData(updatedItem)

        val instant = timeUtils.now()
        val reviewDuration = activeReviewDuration(instant - currentReviewStartInstant)

        if (answer.srsAnswer.card.interval < 1.days) {
            placeItemBackToQueue(updatedItem)
        }

        updateState()

        srsCardRepository.update(item.srsCardKey, answer.srsAnswer.card)
        saveReviewHistory(item, answer, instant, reviewDuration)
        reviewReporter.reportReview(updatedItem, answer, reviewDuration)

        recordStatistics(item, answer, reviewDuration)
    }

    private suspend fun recordStatistics(
        item: QueueItem,
        answer: PracticeAnswer,
        reviewDuration: Duration
    ) {
        val key = item.srsCardKey
        val isNew = item.srsCard.fsrsCard.repeats == 0
        val isCorrect = answer.srsAnswer.grade > 1
        statisticsRecorder.recordReview(
            key = key.itemKey,
            practiceType = key.practiceType,
            isNew = isNew,
            isCorrect = isCorrect,
            mistakes = answer.mistakes,
            duration = reviewDuration,
            deckId = item.deckId
        )
        answer.writingStats?.takeIf { it.strokeCount > 0 }?.let { writing ->
            statisticsRecorder.recordWritingAttempt(
                character = key.itemKey,
                practiceType = key.practiceType,
                strokeCount = writing.strokeCount,
                mistakes = writing.mistakes,
                wrongOrder = writing.wrongOrderCount,
                almost = writing.almostCount,
                accuracy = writing.strokeAccuracy,
                deckId = item.deckId,
                studyTimeMs = reviewDuration.inWholeMilliseconds
            )
        }
    }

    private suspend fun updateState() {
        val item = queue.getOrNull(0)
        if (item == null) {
            immediateFinish()
        } else {
            if (!item.data.isCompleted) {
                _state.value = getLoadingState()
            }
            val time = timeUtils.now()
            val srsAnswers = srsScheduler.answers(item.srsCard, time)

            item.data.await()
            currentReviewStartInstant = timeUtils.now()

            _state.value = getReviewState(item, getAnswers(srsAnswers))

            queue.getOrNull(1)?.apply {
                data.start()
            }
        }
    }

    private fun placeItemBackToQueue(
        updatedQueueItem: QueueItem
    ) {
        val nextReviewTime = getExpectedReviewTime(updatedQueueItem.srsCard)
        val insertPosition = queue.asSequence()
            .map { getExpectedReviewTime(it.srsCard) }
            .indexOfFirst { nextReviewTime < it }
            .takeIf { it != -1 }
            ?.let {
                if (it == 0 && queue.size > 0) min(MIN_QUEUE_POSITION_SHIFT - 1, queue.size)
                else min(it, MAX_QUEUE_POSITION_SHIFT - 1)
            }
            ?: min(queue.size, MAX_QUEUE_POSITION_SHIFT - 1)

        queue.add(insertPosition, updatedQueueItem)
    }

    private fun getExpectedReviewTime(srsItem: SrsCard): Instant {
        return (srsItem.lastReview ?: Instant.DISTANT_PAST) + srsItem.interval
    }

    private fun saveSummaryData(queueItem: QueueItem) {
        val summaryItem = createSummaryItem(
            queueItem = queueItem,
            totalReviews = practiceScope.async {
                queueItem.srsCardKey.run {
                    reviewHistoryRepository.getTotalReviewCount(itemKey, practiceType)
                        .toInt()
                        .plus(1) // To count current review before its saved
                }
            }
        )
        summaryItems[queueItem.srsCardKey] = summaryItem
    }

    private suspend fun saveReviewHistory(
        queueItem: QueueItem,
        answer: PracticeAnswer,
        reviewStart: Instant,
        reviewDuration: Duration
    ) {
        val item = ReviewHistoryItem(
            key = queueItem.srsCardKey.itemKey,
            practiceType = queueItem.srsCardKey.practiceType,
            timestamp = reviewStart,
            duration = reviewDuration,
            grade = answer.srsAnswer.grade,
            mistakes = answer.mistakes,
            interval = answer.srsAnswer.card.interval.inWholeDays,
            deckId = queueItem.deckId
        )
        reviewHistoryRepository.addReview(item)
    }


    companion object {
        private const val MIN_QUEUE_POSITION_SHIFT = 3
        private const val MAX_QUEUE_POSITION_SHIFT = 10
    }

}

private class PracticeReviewReporter<T : PracticeQueueItem<*>>(
    private val analyticsManager: AnalyticsManager
) {

    fun reportReview(
        item: T,
        answer: PracticeAnswer,
        reviewDuration: Duration
    ) {
        analyticsManager.sendEvent("review") {
            put("key", item.srsCardKey.itemKey)
            put("practice_type", item.srsCardKey.practiceType)
            put("duration", reviewDuration.inWholeMilliseconds)
            put("mistakes", answer.mistakes)
            put("repeats", item.srsCard.fsrsCard.repeats)
            put("lapses", item.srsCard.fsrsCard.lapses)
        }
    }

}
