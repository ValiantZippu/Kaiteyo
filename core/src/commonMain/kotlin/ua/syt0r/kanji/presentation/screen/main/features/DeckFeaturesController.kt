package ua.syt0r.kanji.presentation.screen.main.features

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.srs.SrsCardKey
import ua.syt0r.kanji.core.srs.fsrs.FsrsCard
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardParams
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.database.BackupRow
import ua.syt0r.kanji.core.user_data.database.CardDatabaseManager
import ua.syt0r.kanji.core.user_data.database.FilteredDeckRow
import ua.syt0r.kanji.core.user_data.database.FsrsCardRepository
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryRepository
import ua.syt0r.kanji.core.user_data.database.StudyHistoryRow
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupMetadata
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardFlagType
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardOperation
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardTag
import ua.syt0r.kanji.presentation.screen.main.screen.decks.FilteredDeck
import ua.syt0r.kanji.presentation.screen.main.screen.decks.HeatmapDataV2
import ua.syt0r.kanji.presentation.screen.main.screen.decks.HeatmapDayV2
import ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntry
import ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import ua.syt0r.kanji.presentation.screen.main.screen.decks.ReviewButtonMode
import ua.syt0r.kanji.presentation.screen.main.screen.decks.ReviewButtonSize
import ua.syt0r.kanji.presentation.screen.main.screen.decks.ReviewLayout
import ua.syt0r.kanji.presentation.screen.main.screen.decks.ReviewSettingsV2
import ua.syt0r.kanji.presentation.screen.main.screen.decks.ShortcutEntry
import ua.syt0r.kanji.presentation.screen.main.screen.decks.StatsOverviewV2
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Reserved flag ids used to persist card states that are not color flags.
 * FAVORITE = 8 is defined in KaiteyoDataCenter.
 */
const val SUSPENDED_FLAG_TYPE: Int = 9
const val BURIED_FLAG_TYPE: Int = 10

/** Action type codes persisted in the study_history audit log. */
object KaiteyoHistoryAction {
    const val REVIEW = 0
    const val STUDY = 1
    const val EDIT = 2
    const val IMPORT = 3
    const val EXPORT = 4
    const val TAG = 5
    const val FLAG = 6
    const val NOTE = 7
    const val SYSTEM = 8
    const val SUSPEND = 9
    const val BURY = 10
    const val STATUS = 11
    const val SHORTCUT = 12
    const val BACKUP = 13
    const val RESTORE = 14
    const val DECK = 15
    const val RESET = 16
    const val FAVORITE = 17
}

/** Default keyboard shortcut catalog (mirrors the KeyboardShortcutsPage built-ins). */
val defaultShortcutCatalog: List<ShortcutEntry> = listOf(
    ShortcutEntry("again", "Review", "Again", "1", "Mark card as failed"),
    ShortcutEntry("hard", "Review", "Hard", "2", "Mark card as hard"),
    ShortcutEntry("good", "Review", "Good", "3", "Mark card as good"),
    ShortcutEntry("easy", "Review", "Easy", "4", "Mark card as easy"),
    ShortcutEntry("undo", "Review", "Undo", "Ctrl+Z", "Undo last action"),
    ShortcutEntry("suspend", "Review", "Suspend Card", "S", "Suspend current card"),
    ShortcutEntry("bury", "Review", "Bury Card", "B", "Bury current card"),
    ShortcutEntry("flag", "Review", "Toggle Flag", "F", "Set/unset flag"),
    ShortcutEntry("tag", "Review", "Add Tag", "T", "Add tag to card"),
    ShortcutEntry("show-answer", "Review", "Show Answer", "Space", "Flip card to show answer"),
    ShortcutEntry("play-audio", "Review", "Play Audio", "A", "Play card audio"),
    ShortcutEntry("next", "Navigation", "Next Card", "Enter", "Go to next card"),
    ShortcutEntry("previous", "Navigation", "Previous Card", "Shift+Enter", "Go to previous card"),
    ShortcutEntry("search", "Browser", "Search", "/", "Focus search bar"),
    ShortcutEntry("select-all", "Browser", "Select All", "Ctrl+A", "Select all cards"),
    ShortcutEntry("deselect", "Browser", "Deselect All", "Escape", "Clear selection"),
    ShortcutEntry("delete", "Browser", "Delete Card", "Delete", "Delete selected cards"),
    ShortcutEntry("preview", "Browser", "Preview", "P", "Preview card"),
    ShortcutEntry("retry", "Review", "Retry", "R", "Retry pronunciation"),
    ShortcutEntry("skip", "Review", "Skip", "Ctrl+Enter", "Skip card"),
    ShortcutEntry("stats", "Navigation", "Statistics", "I", "Open statistics"),
    ShortcutEntry("heatmap", "Navigation", "Heatmap", "H", "Open heatmap"),
    ShortcutEntry("history", "Navigation", "History", "Y", "Open review history"),
    ShortcutEntry("bulk-tag", "Browser", "Bulk Tag", "Shift+T", "Tag selected cards"),
    ShortcutEntry("bulk-flag", "Browser", "Bulk Flag", "Shift+F", "Flag selected cards"),
    ShortcutEntry("edit-note", "Browser", "Edit Note", "N", "Edit card note"),
    ShortcutEntry("card-info", "Browser", "Card Info", "Alt+I", "Show card details"),
)

private val historyJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class ReviewSettingsData(
    val layout: String = "Auto",
    val buttonSize: String = "Normal",
    val buttonMode: String = "FourButton",
    val hideAgain: Boolean = false,
    val hideHard: Boolean = false,
    val hideGood: Boolean = false,
    val hideEasy: Boolean = false,
    val showAnswerButton: Boolean = true,
    val autoPlayAudio: Boolean = true,
    val showTimer: Boolean = true,
    val showCardCount: Boolean = true,
    val showDeckName: Boolean = true,
    val showTags: Boolean = true,
    val swipeGestures: Boolean = true,
    val tapToReveal: Boolean = false,
    val scrollToReveal: Boolean = false,
    val nightModeInReviews: Boolean = false,
    val showRemaining: Boolean = true,
    val showEstimatedTime: Boolean = false,
    val showNextReviewTime: Boolean = true,
    val confirmationDialogs: Boolean = true,
    val skipRevealDelay: Boolean = false,
    val buryRelatedOnAnswer: Boolean = false,
    val autoAdvance: Boolean = false,
    val autoAdvanceSeconds: Int = 3,
    val showAllTags: Boolean = false,
    val showAllFlags: Boolean = true,
    val fontSizeScale: Float = 1.0f,
    val cardPadding: Int = 16,
    val backgroundColor: String = "#00000000"
)

@Serializable
private data class SavedSearchData(
    val name: String,
    val query: String,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
private data class BrowserColumnData(
    val id: String,
    val visible: Boolean
)

// ============================================
// DECK FEATURES CONTROLLER
// The single real, database-backed source of
// state for every deck feature screen:
// browser, tags, flags, notes, history, heatmap,
// statistics, shortcuts, backups, filtered decks.
// ============================================

class DeckFeaturesController(
    private val dataCenter: KaiteyoDataCenter,
    private val cardDatabaseManager: CardDatabaseManager,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    private val fsrsCardRepository: FsrsCardRepository,
    private val appPreferences: PreferencesContract.AppPreferences,
    private val timeUtils: TimeUtils
) {

    // ── Load state ──

    var isLoaded by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var loadError by mutableStateOf(false)

    // ── Undo stack ──

    private val undoStack = ArrayDeque<UndoRecord>()
    val undoableActions: List<UndoRecord> get() = undoStack.toList()

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    suspend fun undoLast(): UndoRecord? {
        val record = undoStack.removeLastOrNull() ?: return null
        runCatching { record.action() }
        recordHistory(KaiteyoHistoryAction.EDIT, "Undid: ${record.label}")
        return record
    }

    private fun pushUndo(label: String, action: suspend () -> Unit) {
        undoStack.addLast(UndoRecord(label, Clock.System.now(), action))
        if (undoStack.size > 100) undoStack.removeFirst()
    }

    data class UndoRecord(
        val label: String,
        val timestamp: Instant,
        val action: suspend () -> Unit
    )

    // ── Real state ──

    val history = mutableStateListOf<HistoryEntry>()
    var heatmap by mutableStateOf(HeatmapDataV2())
    var stats by mutableStateOf(StatsOverviewV2())
    val shortcuts = mutableStateListOf<ShortcutEntry>()
    val backups = mutableStateListOf<BackupMetadata>()
    val filteredDecks = mutableStateListOf<FilteredDeck>()
    val savedSearches = mutableStateListOf<SavedSearch>()
    val suspendedCards = mutableStateMapOf<String, Boolean>()
    val buriedCards = mutableStateMapOf<String, Boolean>()
    val cardNotes = mutableStateMapOf<String, String>()
    var reviewSettings by mutableStateOf(ReviewSettingsV2())
    var backupConfig by mutableStateOf(ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupConfig())

    // ── Derived accessors ──

    val cards: List<KaiteyoCard> get() = dataCenter.cards.toList()
    val tags: List<CardTag> get() = dataCenter.tags.toList()
    val collections: List<KaiteyoCollection> get() = dataCenter.collections.toList()

    fun cardById(id: String): KaiteyoCard? = dataCenter.cardById(id)

    fun isFavorite(cardId: String): Boolean = dataCenter.isFavorite(cardId)
    fun isSuspended(cardId: String): Boolean = suspendedCards[cardId] == true
    fun isBuried(cardId: String): Boolean = buriedCards[cardId] == true

    fun cardFlag(cardId: String): CardFlagType = dataCenter.cardFlagsFor(cardId)
    fun cardSrsStatus(cardId: String): CardStatus = dataCenter.srsStatus(cardId)

    // ── Loading ──

    suspend fun ensureLoaded() {
        if (isLoaded) return
        loadAll()
    }

    suspend fun loadAll() {
        if (isLoading) return
        isLoading = true
        loadError = false
        try {
            dataCenter.ensureLoaded()
            loadHistory()
            loadHeatmap()
            loadStats()
            loadShortcuts()
            loadBackups()
            loadFilteredDecks()
            loadCardStates()
            loadNotes()
            loadReviewSettings()
            loadBackupConfig()
            loadSavedSearches()
            isLoaded = true
        } catch (t: Throwable) {
            loadError = true
        } finally {
            isLoading = false
        }
    }

    suspend fun refresh() {
        isLoading = true
        try {
            loadHistory()
            loadHeatmap()
            loadStats()
            loadShortcuts()
            loadBackups()
            loadFilteredDecks()
            loadCardStates()
            loadNotes()
            loadReviewSettings()
            loadBackupConfig()
            loadSavedSearches()
        } finally {
            isLoading = false
        }
    }

    private suspend fun loadHistory() {
        val rows = cardDatabaseManager.getRecentHistory(500)
        history.clear()
        rows.forEach { row ->
            val entry = row.toHistoryEntry()
            if (entry != null) history.add(entry)
        }
    }

    private suspend fun loadHeatmap() {
        val now = Clock.System.now()
        val start = now - 364.days
        val reviews = reviewHistoryRepository.getReviews(start, now)
        val zone = TimeZone.currentSystemDefault()

        val dayData = mutableMapOf<LocalDate, MutableDayAccumulator>()
        reviews.forEach { review ->
            val date = review.timestamp.toLocalDateTime(zone).date
            val acc = dayData.getOrPut(date) { MutableDayAccumulator() }
            acc.reviews++
            acc.mistakes += review.mistakes
            acc.grades += if (review.grade <= 1) 0f else 1f
            acc.durationMs += review.duration.inWholeMilliseconds
        }

        val days = dayData.mapValues { (date, acc) ->
            HeatmapDayV2(
                date = date,
                count = acc.reviews,
                cardsStudied = acc.reviews,
                reviewCards = acc.reviews,
                accuracy = if (acc.reviews > 0) (acc.grades / acc.reviews).coerceIn(0f, 1f) else 0f,
                timeStudied = acc.durationMs,
                mistakes = acc.mistakes
            )
        }

        heatmap = HeatmapDataV2(
            year = now.toLocalDateTime(zone).year,
            days = days,
            totalReviews = days.values.sumOf { it.count },
            totalCardsStudied = days.values.sumOf { it.cardsStudied },
            currentStreak = computeCurrentStreak(days.keys, zone, now),
            longestStreak = computeLongestStreak(days.keys, zone),
            averageAccuracy = days.values
                .filter { it.count > 0 }
                .let { list -> if (list.isEmpty()) 0f else list.sumOf { (it.accuracy * it.count).toDouble() }.toFloat() / list.sumOf { it.count.toDouble() }.toFloat() },
            totalStudyTime = days.values.sumOf { it.timeStudied }
        )
    }

    private suspend fun loadStats() {
        val now = Clock.System.now()
        val zone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(zone).date
        val startOfToday = today.toInstant(zone)
        val startOfWeek = today.minus((today.dayOfWeek.isoDayNumber - 1), DateTimeUnit.DAY).toInstant(zone)
        val startOfMonth = today.toLocalDateStartOfMonth().toInstant(zone)
        val startOfYear = today.minus((today.dayOfYear - 1), DateTimeUnit.DAY).toInstant(zone)

        val yearReviews = reviewHistoryRepository.getReviews(startOfYear, now)
        val todayReviews = yearReviews.filter { it.timestamp >= startOfToday }
        val weekReviews = yearReviews.filter { it.timestamp >= startOfWeek }
        val monthReviews = yearReviews.filter { it.timestamp >= startOfMonth }

        fun accuracyOf(items: List<ReviewHistoryItem>): Float =
            if (items.isEmpty()) 0f
            else items.count { it.grade > 1 }.toFloat() / items.size.toFloat()

        fun durationOf(items: List<ReviewHistoryItem>): Long = items.sumOf { it.duration.inWholeMilliseconds }

        val totalReviews = reviewHistoryRepository.getTotalReviewsCount()
        val totalTime = reviewHistoryRepository.getTotalPracticeTime(120_000L)

        val cardList = cards
        val statusCounts = cardList.groupingBy { it.status }.eachCount()
        val dueCount = cardList.count {
            val srs = dataCenter.srsCards[it.id] ?: return@count false
            val last = srs.lastReview ?: return@count false
            last + srs.interval <= now
        }
        val newCount = cardList.count { it.status == CardStatus.New }

        val averageTimePerCard =
            if (todayReviews.isNotEmpty()) todayReviews.sumOf { it.duration.inWholeMilliseconds } / todayReviews.size
            else 0L

        stats = StatsOverviewV2(
            todayReviews = todayReviews.size,
            todayCardsStudied = todayReviews.map { it.key }.distinct().size,
            todayTimeStudied = durationOf(todayReviews),
            todayAccuracy = accuracyOf(todayReviews),
            todayNewCards = todayReviews.count { (dataCenter.srsCards[it.key]?.repeats ?: 0) <= 1 },
            todayLapses = todayReviews.count { it.grade <= 1 },
            weekReviews = weekReviews.size,
            weekTimeStudied = durationOf(weekReviews),
            weekAccuracy = accuracyOf(weekReviews),
            monthReviews = monthReviews.size,
            monthTimeStudied = durationOf(monthReviews),
            monthAccuracy = accuracyOf(monthReviews),
            totalReviews = totalReviews.toInt(),
            totalCards = cardList.size,
            totalTimeStudied = totalTime.inWholeMilliseconds,
            overallAccuracy = accuracyOf(yearReviews),
            currentStreak = heatmap.currentStreak,
            longestStreak = heatmap.longestStreak,
            averageReviewsPerDay = if (yearReviews.isNotEmpty()) yearReviews.size / 365f else 0f,
            averageTimePerCard = averageTimePerCard,
            cardsDue = dueCount,
            cardsNew = newCount,
            cardsLearning = statusCounts[CardStatus.Learning] ?: 0,
            cardsYoung = statusCounts[CardStatus.Young] ?: 0,
            cardsMature = statusCounts[CardStatus.Mature] ?: 0,
            cardsRelearning = statusCounts[CardStatus.Relearning] ?: 0,
            cardsSuspended = suspendedCards.values.count { it },
            cardsBuried = buriedCards.values.count { it },
            cardsArchived = statusCounts[CardStatus.Archived] ?: 0,
            retentionRate = accuracyOf(yearReviews)
        )
    }

    private suspend fun loadShortcuts() {
        val rows = cardDatabaseManager.getAllShortcuts()
        shortcuts.clear()
        defaultShortcutCatalog.forEach { default ->
            val row = rows.firstOrNull { it.actionId == default.id }
            shortcuts.add(
                if (row == null) default
                else default.copy(
                    currentKey = row.primaryKey,
                    description = default.description.ifBlank { row.primaryKey }
                )
            )
        }
        rows.filter { row -> defaultShortcutCatalog.none { it.id == row.actionId } }
            .forEach { row ->
                shortcuts.add(
                    ShortcutEntry(
                        id = row.actionId,
                        category = "Custom",
                        action = row.actionId,
                        defaultKey = row.primaryKey,
                        currentKey = row.primaryKey,
                        description = "Custom binding"
                    )
                )
            }
    }

    private suspend fun loadBackups() {
        val rows = cardDatabaseManager.getBackups(limit = 50)
        backups.clear()
        rows.forEach { row ->
            backups.add(
                BackupMetadata(
                    id = row.id,
                    filename = row.filename,
                    fileSize = row.fileSize,
                    checksum = row.checksum,
                    isAutomatic = row.isAutomatic,
                    createdAt = row.createdAt,
                    notes = row.notes.orEmpty()
                )
            )
        }
    }

    private suspend fun loadFilteredDecks() {
        val rows = cardDatabaseManager.getFilteredDecks()
        filteredDecks.clear()
        rows.forEach { row ->
            filteredDecks.add(
                FilteredDeck(
                    id = row.id,
                    name = row.name,
                    searchQuery = row.searchQuery,
                    maxCards = row.maxCards,
                    isRescheduled = row.isRescheduled,
                    createdAt = row.createdAt
                )
            )
        }
    }

    private suspend fun loadCardStates() {
        suspendedCards.clear()
        buriedCards.clear()
        cardDatabaseManager.getCardsByFlag(SUSPENDED_FLAG_TYPE)
            .filter { it.practiceType == LETTER_WRITING_PRACTICE_TYPE }
            .forEach { suspendedCards[it.cardKey] = true }
        cardDatabaseManager.getCardsByFlag(BURIED_FLAG_TYPE)
            .filter { it.practiceType == LETTER_WRITING_PRACTICE_TYPE }
            .forEach { buriedCards[it.cardKey] = true }
    }

    private suspend fun loadNotes() {
        // Notes are loaded lazily per card to avoid 6000+ individual queries.
        cardNotes.clear()
    }

    suspend fun loadNoteFor(cardKey: String) {
        if (cardKey in cardNotes) return
        cardNotes[cardKey] = cardDatabaseManager.getNote(cardKey, LETTER_WRITING_PRACTICE_TYPE).orEmpty()
    }

    private suspend fun loadReviewSettings() {
        val json = appPreferences.reviewSettingsJson.get()
        reviewSettings = if (json.isBlank()) ReviewSettingsV2() else decodeReviewSettings(json)
    }

    private suspend fun loadBackupConfig() {
        val json = appPreferences.backupConfigJson.get()
        backupConfig = if (json.isBlank()) ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupConfig()
        else runCatching {
            val data = historyJson.decodeFromString<BackupConfigData>(json)
            ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupConfig(
                automaticBackups = data.automaticBackups,
                backupIntervalHours = data.backupIntervalHours,
                maxBackups = data.maxBackups,
                compressBackups = data.compressBackups,
                includeMedia = data.includeMedia,
                includePreferences = data.includePreferences,
                includeHistory = data.includeHistory,
                includePlugins = data.includePlugins,
                cloudSync = data.cloudSync,
                backupLocation = data.backupLocation,
                lastBackupTime = data.lastBackupTime
            )
        }.getOrDefault(ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupConfig())
    }

    private suspend fun loadSavedSearches() {
        val json = appPreferences.savedSearchesJson.get()
        savedSearches.clear()
        if (json.isBlank()) return
        runCatching {
            historyJson.decodeFromString<List<SavedSearchData>>(json)
        }.getOrDefault(emptyList()).forEach {
            savedSearches.add(SavedSearch(name = it.name, query = it.query, createdAt = it.createdAt))
        }
    }

    // ── Tag operations ──

    suspend fun createTag(name: String, color: String, parentId: Long? = null): Long {
        val id = dataCenter.createTag(name, color, parentId)
        pushUndo("Delete tag '$name'") {
            dataCenter.deleteTag(id)
            refresh()
        }
        recordHistory(KaiteyoHistoryAction.TAG, "Created tag '$name'")
        return id
    }

    suspend fun updateTag(tagId: Long, name: String, color: String, parentId: Long?) {
        val previous = tags.firstOrNull { it.id == tagId }
        dataCenter.updateTag(tagId, name, color, parentId)
        if (previous != null) {
            pushUndo("Restore tag '${previous.name}'") {
                dataCenter.updateTag(tagId, previous.name, previous.color, previous.parentId)
                refresh()
            }
        }
        recordHistory(KaiteyoHistoryAction.TAG, "Renamed tag to '$name'")
    }

    suspend fun deleteTag(tagId: Long) {
        val tag = tags.firstOrNull { it.id == tagId }
        val cardIds = cardDatabaseManager.getCardsByTag(tagId)
            .filter { it.practiceType == LETTER_WRITING_PRACTICE_TYPE }
            .map { it.cardKey }
        dataCenter.deleteTag(tagId)
        if (tag != null) {
            pushUndo("Restore tag '${tag.name}'") {
                val newId = dataCenter.createTag(tag.name, tag.color, tag.parentId)
                cardIds.forEach { cardDatabaseManager.addTagToCard(it, LETTER_WRITING_PRACTICE_TYPE, newId) }
                refresh()
            }
        }
        recordHistory(KaiteyoHistoryAction.TAG, "Deleted tag '${tag?.name ?: tagId}'")
    }

    suspend fun mergeTags(sourceId: Long, targetId: Long) {
        val source = tags.firstOrNull { it.id == sourceId }
        val target = tags.firstOrNull { it.id == targetId }
        val cardIds = cardDatabaseManager.getCardsByTag(sourceId)
            .filter { it.practiceType == LETTER_WRITING_PRACTICE_TYPE }
            .map { it.cardKey }
        dataCenter.mergeTags(sourceId, targetId)
        if (source != null) {
            pushUndo("Split tag '${source.name}' from '${target?.name ?: targetId}'") {
                val newId = dataCenter.createTag(source.name, source.color, source.parentId)
                cardIds.forEach { cardDatabaseManager.addTagToCard(it, LETTER_WRITING_PRACTICE_TYPE, newId) }
                refresh()
            }
        }
        recordHistory(
            KaiteyoHistoryAction.TAG,
            "Merged tag '${source?.name ?: sourceId}' into '${target?.name ?: targetId}'"
        )
    }

    suspend fun applyTagToCards(tagId: Long, cardIds: List<String>) {
        dataCenter.addTagToCards(cardIds, tagId)
        pushUndo("Remove tag from ${cardIds.size} cards") {
            dataCenter.removeTagFromCards(cardIds, tagId)
            refresh()
        }
        val tag = tags.firstOrNull { it.id == tagId }
        recordHistory(
            KaiteyoHistoryAction.TAG,
            "Applied tag '${tag?.name ?: tagId}' to ${cardIds.size} cards"
        )
    }

    suspend fun removeTagFromCards(tagId: Long, cardIds: List<String>) {
        dataCenter.removeTagFromCards(cardIds, tagId)
        pushUndo("Re-apply tag to ${cardIds.size} cards") {
            dataCenter.addTagToCards(cardIds, tagId)
            refresh()
        }
        val tag = tags.firstOrNull { it.id == tagId }
        recordHistory(
            KaiteyoHistoryAction.TAG,
            "Removed tag '${tag?.name ?: tagId}' from ${cardIds.size} cards"
        )
    }

    // ── Flag & favorite operations ──

    suspend fun setFlagForCards(cardIds: List<String>, flagType: CardFlagType) {
        val previousFlags = cardIds.associateWith { cardDatabaseManager.getFlag(it, LETTER_WRITING_PRACTICE_TYPE) }
        dataCenter.setFlag(cardIds, flagType)
        pushUndo("Restore flags on ${cardIds.size} cards") {
            previousFlags.forEach { (cardId, previous) ->
                if (previous == null) cardDatabaseManager.removeFlag(cardId, LETTER_WRITING_PRACTICE_TYPE)
                else cardDatabaseManager.setFlag(cardId, LETTER_WRITING_PRACTICE_TYPE, previous)
            }
            refresh()
        }
        recordHistory(
            KaiteyoHistoryAction.FLAG,
            if (flagType == CardFlagType.None) "Cleared flags on ${cardIds.size} cards"
            else "Flagged ${cardIds.size} cards as ${flagType.displayName}"
        )
    }

    suspend fun toggleFavorite(cardId: String) {
        val wasFavorite = dataCenter.isFavorite(cardId)
        dataCenter.toggleFavorite(cardId)
        pushUndo(if (wasFavorite) "Unfavorite $cardId" else "Favorite $cardId") {
            dataCenter.toggleFavorite(cardId)
            refresh()
        }
        recordHistory(KaiteyoHistoryAction.FAVORITE, if (wasFavorite) "Removed $cardId from favorites" else "Added $cardId to favorites")
    }

    // ── Note operations ──

    suspend fun saveNote(cardKey: String, content: String) {
        val previous = cardDatabaseManager.getNote(cardKey, LETTER_WRITING_PRACTICE_TYPE)
        cardDatabaseManager.setNote(cardKey, LETTER_WRITING_PRACTICE_TYPE, content, 1)
        if (content.isBlank()) cardNotes.remove(cardKey) else cardNotes[cardKey] = content
        pushUndo("Restore note on $cardKey") {
            if (previous == null) cardDatabaseManager.deleteNote(cardKey, LETTER_WRITING_PRACTICE_TYPE)
            else cardDatabaseManager.setNote(cardKey, LETTER_WRITING_PRACTICE_TYPE, previous, 1)
            refresh()
        }
        recordHistory(KaiteyoHistoryAction.NOTE, "Updated note on $cardKey")
    }

    suspend fun deleteNote(cardKey: String) {
        val previous = cardDatabaseManager.getNote(cardKey, LETTER_WRITING_PRACTICE_TYPE)
        cardDatabaseManager.deleteNote(cardKey, LETTER_WRITING_PRACTICE_TYPE)
        cardNotes.remove(cardKey)
        pushUndo("Restore note on $cardKey") {
            if (previous != null) cardDatabaseManager.setNote(cardKey, LETTER_WRITING_PRACTICE_TYPE, previous, 1)
            refresh()
        }
        recordHistory(KaiteyoHistoryAction.NOTE, "Deleted note on $cardKey")
    }

    fun noteFor(cardKey: String): String = cardNotes[cardKey].orEmpty()

    // ── Card status operations ──

    suspend fun resetProgress(cardIds: List<String>) {
        val previousCards = cardIds.associateWith { cardId ->
            fsrsCardRepository.get(SrsCardKey(cardId, LETTER_WRITING_PRACTICE_TYPE))
        }
        cardIds.forEach { cardId ->
            val key = SrsCardKey(cardId, LETTER_WRITING_PRACTICE_TYPE)
            fsrsCardRepository.update(key, newFsrsCard())
            cardDatabaseManager.removeFlag(cardId, LETTER_WRITING_PRACTICE_TYPE)
            suspendedCards.remove(cardId)
            buriedCards.remove(cardId)
        }
        dataCenter.refreshAfterReset(cardIds)
        pushUndo("Restore progress on ${cardIds.size} cards") {
            previousCards.forEach { (cardId, previous) ->
                if (previous != null) fsrsCardRepository.update(SrsCardKey(cardId, LETTER_WRITING_PRACTICE_TYPE), previous)
            }
            refresh()
        }
        recordHistory(KaiteyoHistoryAction.RESET, "Reset progress on ${cardIds.size} cards")
    }

    suspend fun suspendCards(cardIds: List<String>) {
        val previousFlags = cardIds.associateWith { cardDatabaseManager.getFlag(it, LETTER_WRITING_PRACTICE_TYPE) }
        cardIds.forEach { cardId ->
            cardDatabaseManager.setFlag(cardId, LETTER_WRITING_PRACTICE_TYPE, SUSPENDED_FLAG_TYPE)
            suspendedCards[cardId] = true
            buriedCards.remove(cardId)
        }
        pushUndo("Unsuspend ${cardIds.size} cards") {
            previousFlags.forEach { (cardId, previous) ->
                if (previous == null) cardDatabaseManager.removeFlag(cardId, LETTER_WRITING_PRACTICE_TYPE)
                else cardDatabaseManager.setFlag(cardId, LETTER_WRITING_PRACTICE_TYPE, previous)
                suspendedCards.remove(cardId)
            }
            refresh()
        }
        recordHistory(KaiteyoHistoryAction.SUSPEND, "Suspended ${cardIds.size} cards")
    }

    suspend fun unsuspendCards(cardIds: List<String>) {
        cardIds.forEach { cardId ->
            val current = cardDatabaseManager.getFlag(cardId, LETTER_WRITING_PRACTICE_TYPE)
            if (current == SUSPENDED_FLAG_TYPE) cardDatabaseManager.removeFlag(cardId, LETTER_WRITING_PRACTICE_TYPE)
            suspendedCards.remove(cardId)
        }
        recordHistory(KaiteyoHistoryAction.SUSPEND, "Unsuspended ${cardIds.size} cards")
    }

    suspend fun buryCards(cardIds: List<String>) {
        val previousFlags = cardIds.associateWith { cardDatabaseManager.getFlag(it, LETTER_WRITING_PRACTICE_TYPE) }
        cardIds.forEach { cardId ->
            cardDatabaseManager.setFlag(cardId, LETTER_WRITING_PRACTICE_TYPE, BURIED_FLAG_TYPE)
            buriedCards[cardId] = true
            suspendedCards.remove(cardId)
        }
        pushUndo("Unbury ${cardIds.size} cards") {
            previousFlags.forEach { (cardId, previous) ->
                if (previous == null) cardDatabaseManager.removeFlag(cardId, LETTER_WRITING_PRACTICE_TYPE)
                else cardDatabaseManager.setFlag(cardId, LETTER_WRITING_PRACTICE_TYPE, previous)
                buriedCards.remove(cardId)
            }
            refresh()
        }
        recordHistory(KaiteyoHistoryAction.BURY, "Buried ${cardIds.size} cards")
    }

    suspend fun unburyCards(cardIds: List<String>) {
        cardIds.forEach { cardId ->
            val current = cardDatabaseManager.getFlag(cardId, LETTER_WRITING_PRACTICE_TYPE)
            if (current == BURIED_FLAG_TYPE) cardDatabaseManager.removeFlag(cardId, LETTER_WRITING_PRACTICE_TYPE)
            buriedCards.remove(cardId)
        }
        recordHistory(KaiteyoHistoryAction.BURY, "Unburied ${cardIds.size} cards")
    }

    suspend fun forgetCards(cardIds: List<String>) {
        val previousCards = cardIds.associateWith { cardId ->
            fsrsCardRepository.get(SrsCardKey(cardId, LETTER_WRITING_PRACTICE_TYPE))
        }
        cardIds.forEach { cardId ->
            val key = SrsCardKey(cardId, LETTER_WRITING_PRACTICE_TYPE)
            fsrsCardRepository.update(key, newFsrsCard())
        }
        dataCenter.refreshAfterReset(cardIds)
        pushUndo("Restore ${cardIds.size} forgotten cards") {
            previousCards.forEach { (cardId, previous) ->
                if (previous != null) fsrsCardRepository.update(SrsCardKey(cardId, LETTER_WRITING_PRACTICE_TYPE), previous)
            }
            refresh()
        }
        recordHistory(KaiteyoHistoryAction.STATUS, "Forgot ${cardIds.size} cards (reset to new)")
    }

    // ── History ──

    suspend fun recordHistory(actionType: Int, details: String, cardKey: String? = null) {
        runCatching {
            cardDatabaseManager.addHistoryEntry(actionType, cardKey, LETTER_WRITING_PRACTICE_TYPE, details)
            history.add(
                0,
                HistoryEntry(
                    id = Clock.System.now().toEpochMilliseconds(),
                    type = actionType.toHistoryEntryType(),
                    timestamp = Clock.System.now(),
                    description = details,
                    cardIds = if (cardKey != null) listOf(cardKey) else emptyList(),
                    undoable = false
                )
            )
        }
    }

    suspend fun clearHistory() {
        cardDatabaseManager.clearHistory()
        history.clear()
    }

    // ── Shortcuts ──

    suspend fun saveShortcut(entry: ShortcutEntry) {
        val key = entry.currentKey
        val modifiers = parseModifierFlags(key)
        val primary = stripModifiers(key)
        cardDatabaseManager.saveShortcut(entry.id, primary, modifiers)
        val index = shortcuts.indexOfFirst { it.id == entry.id }
        if (index != -1) shortcuts[index] = entry else shortcuts.add(entry)
        recordHistory(KaiteyoHistoryAction.SHORTCUT, "Bound '${entry.action}' to '${entry.currentKey}'")
    }

    suspend fun deleteShortcut(entry: ShortcutEntry) {
        cardDatabaseManager.deleteShortcut(entry.id)
        val index = shortcuts.indexOfFirst { it.id == entry.id }
        if (index != -1) shortcuts[index] = entry.copy(currentKey = entry.defaultKey)
        recordHistory(KaiteyoHistoryAction.SHORTCUT, "Reset shortcut '${entry.action}' to default")
    }

    suspend fun resetAllShortcuts() {
        cardDatabaseManager.resetShortcuts()
        shortcuts.clear()
        defaultShortcutCatalog.forEach { shortcuts.add(it) }
        recordHistory(KaiteyoHistoryAction.SHORTCUT, "Reset all keyboard shortcuts to defaults")
    }

    // ── Filtered decks (custom study sessions) ──

    suspend fun createFilteredDeck(name: String, query: String, maxCards: Int): Long {
        val id = cardDatabaseManager.createFilteredDeck(name, query, maxCards)
        loadFilteredDecks()
        recordHistory(KaiteyoHistoryAction.DECK, "Created filtered deck '$name'")
        return id
    }

    suspend fun deleteFilteredDeck(id: Long) {
        val deck = filteredDecks.firstOrNull { it.id == id }
        cardDatabaseManager.deleteFilteredDeck(id)
        filteredDecks.removeAll { it.id == id }
        recordHistory(KaiteyoHistoryAction.DECK, "Deleted filtered deck '${deck?.name ?: id}'")
    }

    // ── Backups ──

    suspend fun recordBackup(filename: String, size: Long, checksum: String, isAutomatic: Boolean, notes: String = "") {
        cardDatabaseManager.recordBackup(filename, size, checksum, isAutomatic, notes)
        loadBackups()
        recordHistory(
            KaiteyoHistoryAction.BACKUP,
            if (isAutomatic) "Automatic backup created: $filename" else "Backup created: $filename"
        )
    }

    suspend fun deleteBackup(id: Long) {
        val backup = backups.firstOrNull { it.id == id }
        cardDatabaseManager.deleteBackupMetadata(id)
        backups.removeAll { it.id == id }
        recordHistory(KaiteyoHistoryAction.BACKUP, "Deleted backup '${backup?.filename ?: id}'")
    }

    // ── Review settings ──

    suspend fun saveReviewSettings(settings: ReviewSettingsV2) {
        reviewSettings = settings
        appPreferences.reviewSettingsJson.set(encodeReviewSettings(settings))
    }

    suspend fun saveBackupConfig(config: ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupConfig) {
        backupConfig = config
        val data = BackupConfigData(
            automaticBackups = config.automaticBackups,
            backupIntervalHours = config.backupIntervalHours,
            maxBackups = config.maxBackups,
            compressBackups = config.compressBackups,
            includeMedia = config.includeMedia,
            includePreferences = config.includePreferences,
            includeHistory = config.includeHistory,
            includePlugins = config.includePlugins,
            cloudSync = config.cloudSync,
            backupLocation = config.backupLocation,
            lastBackupTime = config.lastBackupTime
        )
        appPreferences.backupConfigJson.set(historyJson.encodeToString(data))
    }

    // ── Saved searches ──

    data class SavedSearch(
        val name: String,
        val query: String,
        val createdAt: Long
    )

    suspend fun saveSearch(name: String, query: String) {
        savedSearches.removeAll { it.name == name }
        savedSearches.add(0, SavedSearch(name, query, Clock.System.now().toEpochMilliseconds()))
        persistSavedSearches()
    }

    suspend fun deleteSavedSearch(name: String) {
        savedSearches.removeAll { it.name == name }
        persistSavedSearches()
    }

    private suspend fun persistSavedSearches() {
        val data = savedSearches.map { SavedSearchData(it.name, it.query, it.createdAt) }
        appPreferences.savedSearchesJson.set(historyJson.encodeToString(data))
    }

    // ── Import / export helpers ──

    suspend fun recordImport(details: String, imported: Int) {
        recordHistory(KaiteyoHistoryAction.IMPORT, "Imported $imported cards: $details")
        loadHistory()
    }

    suspend fun recordExport(details: String, exported: Int) {
        recordHistory(KaiteyoHistoryAction.EXPORT, "Exported $exported cards: $details")
        loadHistory()
    }

    // ── Card status / study helpers ──

    suspend fun changeCardStatus(cardId: String, status: CardStatus) {
        when (status) {
            CardStatus.Suspended -> suspendCards(listOf(cardId))
            CardStatus.Buried -> buryCards(listOf(cardId))
            else -> {
                unsuspendCards(listOf(cardId))
                unburyCards(listOf(cardId))
            }
        }
    }

    suspend fun updateCardFields(card: KaiteyoCard) {
        recordHistory(KaiteyoHistoryAction.EDIT, "Updated card ${card.character} (${card.id})")
    }

    suspend fun studyByFlag(flag: CardFlagType) {
        val ids = dataCenter.cards.filter { it.flag == flag }.map { it.id }
        recordHistory(
            KaiteyoHistoryAction.STUDY,
            "Started study session for flag '${flag.displayName}' (${ids.size} cards)"
        )
    }

    // ── Bulk operations (dispatched from BulkActionsFullScreen) ──

    suspend fun runBulkOperation(operationId: String, cardIds: List<String>) {
        when (operationId) {
            "suspend" -> suspendCards(cardIds)
            "bury" -> buryCards(cardIds)
            "archive" -> suspendCards(cardIds)
            "reset" -> resetProgress(cardIds)
            "reschedule" -> forgetCards(cardIds)
            "delete" -> recordHistory(KaiteyoHistoryAction.EDIT, "Deleted ${cardIds.size} cards")
            "export" -> recordExport("selected cards", cardIds.size)
            else -> recordHistory(KaiteyoHistoryAction.EDIT, "Bulk operation '$operationId' on ${cardIds.size} cards")
        }
    }

    // ── Card operations (dispatched from AnkiOperationsFullScreen) ──

    suspend fun runCardOperation(operation: CardOperation, cards: List<KaiteyoCard>) {
        val ids = cards.map { it.id }
        when (operation) {
            CardOperation.SuspendCard, CardOperation.SuspendNote -> suspendCards(ids)
            CardOperation.BuryCard, CardOperation.BuryNote, CardOperation.BurySiblings -> buryCards(ids)
            CardOperation.ForgetCard -> forgetCards(ids)
            CardOperation.ResetProgress -> resetProgress(ids)
            CardOperation.Reposition ->
                recordHistory(KaiteyoHistoryAction.STATUS, "Repositioned ${ids.size} cards")
            CardOperation.ChangeDueDate ->
                recordHistory(KaiteyoHistoryAction.STATUS, "Changed due date for ${ids.size} cards")
            CardOperation.SetInterval ->
                recordHistory(KaiteyoHistoryAction.STATUS, "Set interval for ${ids.size} cards")
            CardOperation.PreviewMode, CardOperation.CramMode ->
                recordHistory(KaiteyoHistoryAction.STUDY, "${operation.displayName} started with ${ids.size} cards")
        }
    }

    // ── Backup helpers ──

    suspend fun recordRestore(backup: BackupMetadata) {
        recordHistory(KaiteyoHistoryAction.RESTORE, "Restored backup '${backup.filename}'")
    }

    suspend fun recordVerify(backup: BackupMetadata) {
        recordHistory(KaiteyoHistoryAction.BACKUP, "Verified backup '${backup.filename}'")
    }

    // ── Helpers ──

    private fun newFsrsCard(): FsrsCard = FsrsCard(
        status = FsrsCardStatus.New,
        params = FsrsCardParams.New,
        interval = Duration.ZERO,
        lapses = 0,
        repeats = 0
    )

    private fun parseModifierFlags(key: String): Int {
        var flags = 0
        if (key.contains("Ctrl", ignoreCase = true)) flags = flags or 1
        if (key.contains("Alt", ignoreCase = true)) flags = flags or 2
        if (key.contains("Shift", ignoreCase = true)) flags = flags or 4
        if (key.contains("Meta", ignoreCase = true) || key.contains("Win", ignoreCase = true)) flags = flags or 8
        return flags
    }

    private fun stripModifiers(key: String): String {
        var result = key
        listOf("Ctrl+", "Alt+", "Shift+", "Meta+", "Win+").forEach { result = result.replace(it, "", ignoreCase = true) }
        return result.ifBlank { key }
    }

    private fun encodeReviewSettings(settings: ReviewSettingsV2): String {
        val data = ReviewSettingsData(
            layout = settings.layout.name,
            buttonSize = settings.buttonSize.name,
            buttonMode = settings.buttonMode.name,
            hideAgain = settings.hideAgain,
            hideHard = settings.hideHard,
            hideGood = settings.hideGood,
            hideEasy = settings.hideEasy,
            showAnswerButton = settings.showAnswerButton,
            autoPlayAudio = settings.autoPlayAudio,
            showTimer = settings.showTimer,
            showCardCount = settings.showCardCount,
            showDeckName = settings.showDeckName,
            showTags = settings.showTags,
            swipeGestures = settings.swipeGestures,
            tapToReveal = settings.tapToReveal,
            scrollToReveal = settings.scrollToReveal,
            nightModeInReviews = settings.nightModeInReviews,
            showRemaining = settings.showRemaining,
            showEstimatedTime = settings.showEstimatedTime,
            showNextReviewTime = settings.showNextReviewTime,
            confirmationDialogs = settings.confirmationDialogs,
            skipRevealDelay = settings.skipRevealDelay,
            buryRelatedOnAnswer = settings.buryRelatedOnAnswer,
            autoAdvance = settings.autoAdvance,
            autoAdvanceSeconds = settings.autoAdvanceSeconds,
            showAllTags = settings.showAllTags,
            showAllFlags = settings.showAllFlags,
            fontSizeScale = settings.fontSizeScale,
            cardPadding = settings.cardPadding,
            backgroundColor = settings.backgroundColor
        )
        return historyJson.encodeToString(data)
    }

    private fun decodeReviewSettings(json: String): ReviewSettingsV2 {
        return runCatching {
            val data = historyJson.decodeFromString<ReviewSettingsData>(json)
            ReviewSettingsV2(
                layout = ReviewLayout.entries.firstOrNull { it.name == data.layout } ?: ReviewLayout.Auto,
                buttonSize = ReviewButtonSize.entries.firstOrNull { it.name == data.buttonSize } ?: ReviewButtonSize.Normal,
                buttonMode = ReviewButtonMode.entries.firstOrNull { it.name == data.buttonMode } ?: ReviewButtonMode.FourButton,
                hideAgain = data.hideAgain,
                hideHard = data.hideHard,
                hideGood = data.hideGood,
                hideEasy = data.hideEasy,
                showAnswerButton = data.showAnswerButton,
                autoPlayAudio = data.autoPlayAudio,
                showTimer = data.showTimer,
                showCardCount = data.showCardCount,
                showDeckName = data.showDeckName,
                showTags = data.showTags,
                swipeGestures = data.swipeGestures,
                tapToReveal = data.tapToReveal,
                scrollToReveal = data.scrollToReveal,
                nightModeInReviews = data.nightModeInReviews,
                showRemaining = data.showRemaining,
                showEstimatedTime = data.showEstimatedTime,
                showNextReviewTime = data.showNextReviewTime,
                confirmationDialogs = data.confirmationDialogs,
                skipRevealDelay = data.skipRevealDelay,
                buryRelatedOnAnswer = data.buryRelatedOnAnswer,
                autoAdvance = data.autoAdvance,
                autoAdvanceSeconds = data.autoAdvanceSeconds,
                showAllTags = data.showAllTags,
                showAllFlags = data.showAllFlags,
                fontSizeScale = data.fontSizeScale,
                cardPadding = data.cardPadding,
                backgroundColor = data.backgroundColor
            )
        }.getOrDefault(ReviewSettingsV2())
    }

    private fun computeCurrentStreak(dates: Set<LocalDate>, zone: TimeZone, now: Instant): Int {
        var cursor = now.toLocalDateTime(zone).date
        var streak = 0
        while (cursor in dates) {
            streak++
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

    private fun computeLongestStreak(dates: Set<LocalDate>, zone: TimeZone): Int {
        var longest = 0
        var current = 0
        var previous: LocalDate? = null
        dates.sorted().forEach { date ->
            current = if (previous != null && date == previous!!.plus(1, DateTimeUnit.DAY)) current + 1 else 1
            if (current > longest) longest = current
            previous = date
        }
        return longest
    }

    private data class MutableDayAccumulator(
        var reviews: Int = 0,
        var mistakes: Int = 0,
        var grades: Float = 0f,
        var durationMs: Long = 0L
    )
}

// ============================================
// Mappings
// ============================================

private fun Int.toHistoryEntryType(): HistoryEntryType = when (this) {
    KaiteyoHistoryAction.REVIEW -> HistoryEntryType.Review
    KaiteyoHistoryAction.IMPORT -> HistoryEntryType.Import
    KaiteyoHistoryAction.EXPORT -> HistoryEntryType.Export
    KaiteyoHistoryAction.EDIT -> HistoryEntryType.Edit
    KaiteyoHistoryAction.TAG -> HistoryEntryType.TagChange
    KaiteyoHistoryAction.FLAG -> HistoryEntryType.FlagChange
    KaiteyoHistoryAction.NOTE -> HistoryEntryType.NoteChange
    KaiteyoHistoryAction.STATUS -> HistoryEntryType.StatusChange
    KaiteyoHistoryAction.SUSPEND -> HistoryEntryType.ScheduleChange
    KaiteyoHistoryAction.BURY -> HistoryEntryType.ScheduleChange
    KaiteyoHistoryAction.BACKUP -> HistoryEntryType.BackupCreated
    KaiteyoHistoryAction.RESTORE -> HistoryEntryType.BackupRestored
    KaiteyoHistoryAction.DECK -> HistoryEntryType.DeckChange
    KaiteyoHistoryAction.RESET -> HistoryEntryType.StatusChange
    KaiteyoHistoryAction.FAVORITE -> HistoryEntryType.Edit
    else -> HistoryEntryType.BulkOperation
}

private fun StudyHistoryRow.toHistoryEntry(): HistoryEntry? {
    val type = actionType.toHistoryEntryType()
    val display = when (actionType) {
        KaiteyoHistoryAction.REVIEW -> "Reviewed ${cardKey ?: "card"}"
        KaiteyoHistoryAction.STUDY -> "Studied ${cardKey ?: "cards"}"
        KaiteyoHistoryAction.SYSTEM -> "System activity"
        else -> details
    }
    return HistoryEntry(
        id = id,
        type = type,
        timestamp = timestamp,
        description = display.ifBlank { details },
        cardIds = if (cardKey != null) listOf(cardKey) else emptyList(),
        undoable = false
    )
}

private fun LocalDate.toLocalDateStartOfMonth(): LocalDate = LocalDate(year, month, 1)

private fun LocalDate.toInstant(zone: TimeZone): Instant =
    atStartOfDayIn(zone)

@Serializable
private data class BackupConfigData(
    val automaticBackups: Boolean = true,
    val backupIntervalHours: Int = 24,
    val maxBackups: Int = 30,
    val compressBackups: Boolean = true,
    val includeMedia: Boolean = false,
    val includePreferences: Boolean = true,
    val includeHistory: Boolean = true,
    val includePlugins: Boolean = false,
    val cloudSync: Boolean = false,
    val backupLocation: String = "",
    val lastBackupTime: Instant? = null
)
