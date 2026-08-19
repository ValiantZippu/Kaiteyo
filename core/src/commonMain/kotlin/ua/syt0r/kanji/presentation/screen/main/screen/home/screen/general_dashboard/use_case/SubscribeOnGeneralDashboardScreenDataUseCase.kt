package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.use_case

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.core.RefreshableData
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.core.logger.runWithTimeLog
import ua.syt0r.kanji.core.mergeSharedFlows
import ua.syt0r.kanji.core.refreshableDataProducerFlow
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.core.knowledge.RecommendationCandidate
import ua.syt0r.kanji.core.knowledge.StudyOverlayBuilder
import ua.syt0r.kanji.core.knowledge.StudyRecommendationEngine
import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.srs.LetterSrsDeck
import ua.syt0r.kanji.core.srs.LetterSrsManager
import ua.syt0r.kanji.core.srs.SrsCardRepository
import ua.syt0r.kanji.core.srs.SrsDecksData
import ua.syt0r.kanji.core.srs.VocabPracticeType
import ua.syt0r.kanji.core.srs.VocabSrsDeck
import ua.syt0r.kanji.core.srs.VocabSrsManager
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryRepository
import ua.syt0r.kanji.core.user_data.database.StreakData
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.LifecycleState
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.DashboardDaySummary
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.DashboardDeckCategory
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.DashboardRecommendation
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.DashboardDeckSummary
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.GeneralDashboardScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.GeneralDashboardStats
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.StudyTarget
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.StudyTargetPracticeOptions
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.StudyTargetProgress
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.StudyTargetState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeScreenConfiguration
import kotlin.time.Duration.Companion.days

interface SubscribeOnGeneralDashboardScreenDataUseCase {

    operator fun invoke(
        coroutineScope: CoroutineScope, lifecycleState: StateFlow<LifecycleState>
    ): Flow<RefreshableData<ScreenState.Loaded>>

}

class DefaultSubscribeOnGeneralDashboardScreenDataUseCase(
    private val letterSrsManager: LetterSrsManager,
    private val vocabSrsManager: VocabSrsManager,
    private val preferencesRepository: PreferencesContract.AppPreferences,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    private val timeUtils: TimeUtils,
    private val dataCenter: KaiteyoDataCenter,
    private val srsCardRepository: SrsCardRepository,
    private val knowledgeRepository: KnowledgeRepository
) : SubscribeOnGeneralDashboardScreenDataUseCase {

    override fun invoke(
        coroutineScope: CoroutineScope,
        lifecycleState: StateFlow<LifecycleState>
    ): Flow<RefreshableData<ScreenState.Loaded>> = refreshableDataProducerFlow(
        dataChangeFlow = mergeSharedFlows(
            coroutineScope,
            letterSrsManager.dataChangeFlow,
            vocabSrsManager.dataChangeFlow
        ),
        lifecycleState = lifecycleState,
        producer = { withContext(Dispatchers.IO) { produceState() } }
    )

    private suspend fun ProducerScope<ScreenState.Loaded>.produceState() {
        Logger.d("produceState")

        dataCenter.ensureLoaded()

        val deferredLettersDecks = async {
            runWithTimeLog("letterDecksData") { letterSrsManager.getDecks() }
        }

        val deferredVocabDecks = async {
            runWithTimeLog("vocabDecksData") { vocabSrsManager.getDecks() }
        }

        val deferredStats = async {
            runWithTimeLog("statsData") { getStats() }
        }

        val preferencesStudyTargets = preferencesRepository.generalDashboardStudyTargets.get()
            .mapNotNull { (name, enabled) ->
                val target = StudyTarget.entries.find { it.name == name } ?: return@mapNotNull null
                target to enabled
            }

        val missingStudyTargets = StudyTarget.entries
            .minus(preferencesStudyTargets.map { it.first })
            .map { it to false }

        val studyTargets = mutableStateOf(
            preferencesStudyTargets.plus(missingStudyTargets).map { (studyTarget, enabled) ->
                async {
                    StudyTargetState(
                        studyTarget = studyTarget,
                        enabled = enabled,
                        progress = when (studyTarget.practiceType) {
                            is LetterPracticeType -> getPracticeTypeProgress(
                                decksData = deferredLettersDecks.await(),
                                practiceType = studyTarget.practiceType
                            )

                            is VocabPracticeType -> getPracticeTypeProgress(
                                decksData = deferredVocabDecks.await(),
                                practiceType = studyTarget.practiceType
                            )
                        }
                    )
                }
            }.awaitAll()
        )

        val lettersDecks = deferredLettersDecks.await()
        val vocabDecks = deferredVocabDecks.await()

        // Every deck (premade or custom) so Collections can show them all;
        // Recent decks is the same list re-sorted by recency.
        val allDecks = buildList {
            addAll(lettersDecks.decks.map { it.toDeckSummary(DashboardDeckCategory.Letters) })
            addAll(vocabDecks.decks.map { it.toDeckSummary(DashboardDeckCategory.Vocabulary) })
        }.sortedBy { it.title }

        val recentDecks = allDecks.sortedWith(
            compareByDescending<DashboardDeckSummary> { it.lastReview }
                .thenBy { it.title }
        ).take(RECENT_DECKS_LIMIT)

        // "What should I study next?" — real FSRS state per kanji + real
        // corpus frequency, ranked by the recommendation engine (spec §31).
        // A kanji with no card projects to New; nothing is ever invented.
        val recommendations = getRecommendations(lettersDecks)

        val state = ScreenState.Loaded(
            studyTargets = studyTargets,
            stats = deferredStats.await(),
            recentDecks = recentDecks,
            allDecks = allDecks,
            recentActivity = dataCenter.activity.take(ACTIVITY_LIMIT),
            collections = dataCenter.collections.toList(),
            recommendations = recommendations
        )
        send(state)

        snapshotFlow { studyTargets.value }
            .drop(1)
            .onEach {
                val updatedMap = it.associate { it.studyTarget.name to it.enabled }
                preferencesRepository.generalDashboardStudyTargets.set(updatedMap)
            }
            .launchIn(this)

    }

    private fun LetterSrsDeck.toDeckSummary(category: DashboardDeckCategory): DashboardDeckSummary {
        return DashboardDeckSummary(
            deckId = id,
            title = title,
            category = category,
            lastReview = lastReview,
            newCount = progressMap.values.sumOf { it.dailyNew.size },
            dueCount = progressMap.values.sumOf { it.dailyDue.size },
            totalCount = items.size,
            studiedCount = progressMap.values
                .flatMap { it.done + it.due }
                .distinct()
                .size
        )
    }

    private fun VocabSrsDeck.toDeckSummary(category: DashboardDeckCategory): DashboardDeckSummary {
        return DashboardDeckSummary(
            deckId = id,
            title = title,
            category = category,
            lastReview = lastReview,
            newCount = progressMap.values.sumOf { it.dailyNew.size },
            dueCount = progressMap.values.sumOf { it.dailyDue.size },
            totalCount = items.size,
            studiedCount = progressMap.values
                .flatMap { it.done + it.due }
                .distinct()
                .size
        )
    }

    private fun CoroutineScope.getPracticeTypeProgress(
        decksData: SrsDecksData<LetterSrsDeck, LetterPracticeType>,
        practiceType: LetterPracticeType
    ): StudyTargetProgress {
        if (decksData.decks.isEmpty()) return StudyTargetProgress.NoDecks

        val combinedDailyNew = mutableMapOf<String, Long>()
        val combinedDailyDue = mutableMapOf<String, Long>()
        val combinedNotNew = mutableSetOf<String>()

        decksData.decks
            .map { it.id to it.progressMap.getValue(practiceType) }
            .forEach { (deckId, srsProgress) ->
                srsProgress.dailyNew.associateWith { deckId }
                    .forEach { combinedDailyNew[it.key] = it.value }
                srsProgress.dailyDue.associateWith { deckId }
                    .forEach { combinedDailyDue[it.key] = it.value }

                combinedNotNew.addAll(srsProgress.due)
                combinedNotNew.addAll(srsProgress.done)
            }

        val leftover = decksData.dailyProgress.leftoversByPracticeTypeMap.getValue(practiceType)

        fun Pair<String, Long>.mapToCard() = LetterPracticeScreenConfiguration.Card(first, second)

        val resultNew = combinedDailyNew.toList().take(leftover.new).map { it.mapToCard() }
        val dueCards = combinedDailyDue.toList().take(leftover.due).map { it.mapToCard() }
        val combinedCards = resultNew.plus(dueCards)

        return StudyTargetProgress.WithDecks(
            options = StudyTargetPracticeOptions(
                newCards = resultNew,
                dueCards = dueCards,
                combinedCards = combinedCards
            ),
            totalProgress = getProgress(decksData.uniqueCardsCount, combinedNotNew.size)
        )
    }

    private fun CoroutineScope.getPracticeTypeProgress(
        decksData: SrsDecksData<VocabSrsDeck, VocabPracticeType>,
        practiceType: VocabPracticeType
    ): StudyTargetProgress {
        if (decksData.decks.isEmpty()) return StudyTargetProgress.NoDecks

        val combinedDailyNew = mutableMapOf<Long, Long>()
        val combinedDailyDue = mutableMapOf<Long, Long>()
        val combinedNotNew = mutableSetOf<Long>()

        decksData.decks
            .map { it.id to it.progressMap.getValue(practiceType) }
            .forEach { (deckId, srsProgress) ->
                srsProgress.dailyNew.associateWith { deckId }
                    .forEach { combinedDailyNew[it.key] = it.value }
                srsProgress.dailyDue.associateWith { deckId }
                    .forEach { combinedDailyDue[it.key] = it.value }

                combinedNotNew.addAll(srsProgress.due)
                combinedNotNew.addAll(srsProgress.done)
            }

        val leftover = decksData.dailyProgress.leftoversByPracticeTypeMap.getValue(practiceType)

        fun Pair<Long, Long>.mapToCard() = VocabPracticeScreenConfiguration.Card(first, second)

        val resultNew = combinedDailyNew.toList().take(leftover.new).map { it.mapToCard() }
        val dueCards = combinedDailyDue.toList().take(leftover.due).map { it.mapToCard() }
        val combinedCards = resultNew.plus(dueCards)

        return StudyTargetProgress.WithDecks(
            options = StudyTargetPracticeOptions(
                newCards = resultNew,
                dueCards = dueCards,
                combinedCards = combinedCards
            ),
            totalProgress = getProgress(decksData.uniqueCardsCount, combinedNotNew.size)
        )
    }

    private suspend fun getRecommendations(lettersDecks: SrsDecksData<LetterSrsDeck, LetterPracticeType>): List<DashboardRecommendation> {
        val cards = srsCardRepository.getAll()
        val overlay = StudyOverlayBuilder.build(cards)
        val frequencyRanks = knowledgeRepository.kanjiFrequencyRanks()

        // Candidates: every kanji in the letter decks (the real study pool),
        // with its real projected study state and real frequency rank.
        val candidates = lettersDecks.decks
            .flatMap { it.items }
            .distinct()
            .map { character ->
                RecommendationCandidate(
                    character = character,
                    keyword = null,
                    frequencyRank = frequencyRanks[character],
                    studyState = overlay.state(character),
                    lastReviewMs = overlay.info(character)?.lastReview?.toEpochMilliseconds(),
                    jlpt = null
                )
            }

        val recommended = StudyRecommendationEngine.recommend(
            candidates = candidates,
            limit = RECOMMENDATIONS_LIMIT
        )
        // Keywords for the recommended set only (a handful of cheap lookups).
        return recommended.map { rec ->
            DashboardRecommendation(
                character = rec.character,
                keyword = rec.keyword
                    ?: runCatching { knowledgeRepository.kanji(rec.character)?.meanings?.firstOrNull() }.getOrNull(),
                reason = rec.reason,
                urgency = rec.urgency
            )
        }
    }

    private suspend fun getStats(): GeneralDashboardStats {

        fun StreakData.includesDate(date: LocalDate): Boolean {
            return date in start..end
        }

        val resetTime = preferencesRepository.dailyResetTime.get()
        val streaks = reviewHistoryRepository.getStreaks(resetTime)
        val longestStreak = streaks.maxByOrNull { it.length }

        val currentDate = timeUtils.getCurrentDate()

        val currentStreakSearchDates = setOf(
            currentDate, currentDate.minus(1, DateTimeUnit.DAY)
        )
        val currentStreak = streaks.find { streak ->
            currentStreakSearchDates.any { streak.includesDate(it) }
        }

        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        // One query covering the heatmap range (12 weeks) also feeds the
        // 7-day weekly summary below.
        val heatmapStart = now - HEATMAP_DAYS.days
        val heatmapReviews = reviewHistoryRepository.getReviews(heatmapStart, now)
        val reviewsByDate = heatmapReviews
            .groupBy { it.timestamp.toLocalDateTime(zone).date }
            .mapValues { it.value.size }

        val weeklySummary = (0L..6L).map { offset ->
            val date = currentDate.minus(offset, DateTimeUnit.DAY)
            DashboardDaySummary(
                date = date,
                count = reviewsByDate[date] ?: 0
            )
        }.reversed()

        val heatmapSummary = (HEATMAP_DAYS - 1L downTo 0L).map { offset ->
            val date = currentDate.minus(offset, DateTimeUnit.DAY)
            DashboardDaySummary(
                date = date,
                count = reviewsByDate[date] ?: 0
            )
        }

        val startOfToday = currentDate.atStartOfDayIn(zone)
        val reviewsToday = reviewHistoryRepository.getReviews(
            start = startOfToday,
            end = startOfToday.plus(1, DateTimeUnit.DAY, zone)
        ).size

        val totalReviews = reviewHistoryRepository.getTotalReviewsCount()

        return GeneralDashboardStats(
            currentStreak = currentStreak?.length ?: 0,
            longestStreak = longestStreak?.length ?: 0,
            reviewsToday = reviewsToday,
            totalReviews = totalReviews,
            weeklySummary = weeklySummary,
            heatmapSummary = heatmapSummary,
            newReviewedToday = 0,
            dueReviewedToday = 0,
            newLeftoverToday = 0,
            dueLeftoverToday = 0
        )
    }

    private fun getProgress(uniqueCardsCount: Int, notNewCardsCount: Int): Float = uniqueCardsCount
        .takeIf { it != 0 }
        ?.let { notNewCardsCount.toFloat() / it }
        ?: 0f

    companion object {
        private const val RECENT_DECKS_LIMIT = 6
        private const val ACTIVITY_LIMIT = 8
        private const val HEATMAP_DAYS = 84L
        private const val RECOMMENDATIONS_LIMIT = 6
    }

}