package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

// ============================================
// KAITEYO v1.2 — LEARNING POWER DATA MODELS
// All data classes, enums, and type definitions
// for Tags, Flags, Notes, Card Status, Review
// Settings, Shortcuts, History, Backup, Plugins
// ============================================

// ── Card Difficulty / Ease ──

enum class CardDifficultyV2(val displayName: String, val multiplier: Float) {
    Again("Again", 0.0f),
    Hard("Hard", 0.8f),
    Good("Good", 1.0f),
    Easy("Easy", 1.3f)
}

enum class StudyActionV2(val displayName: String) {
    ShowAnswer("Show Answer"),
    Again("Again"),
    Hard("Hard"),
    Good("Good"),
    Easy("Easy"),
    Suspend("Suspend"),
    Bury("Bury"),
    Skip("Skip"),
    Preview("Preview"),
    Undo("Undo"),
    Retry("Retry"),
    Flag("Flag"),
    Note("Note"),
    Tag("Tag"),
    Delete("Delete"),
    Edit("Edit"),
    More("More Options"),
    PlayAudio("Play Audio"),
    ShowHint("Show Hint"),
    MarkAsKnown("Mark as Known")
}

// ── Review Settings ──

data class ReviewSettingsV2(
    val layout: ReviewLayout = ReviewLayout.Auto,
    val buttonSize: ReviewButtonSize = ReviewButtonSize.Normal,
    val buttonMode: ReviewButtonMode = ReviewButtonMode.FourButton,
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
    val backgroundColor: String = "#00000000",
    // Smart study-time: don't count idle gaps inside a review as study time.
    // When enabled, any single review longer than inactivityThresholdMinutes
    // is capped at that threshold before it reaches the statistics.
    val smartActivityDetection: Boolean = true,
    val inactivityThresholdMinutes: Int = 10
)

enum class ReviewLayout(val displayName: String) {
    Auto("Auto"),
    Compact("Compact"),
    Vertical("Vertical"),
    Horizontal("Horizontal"),
    Wide("Wide"),
    FullScreen("Full Screen")
}

enum class ReviewButtonSize(val displayName: String) {
    Small("Small"),
    Normal("Normal"),
    Large("Large"),
    ExtraLarge("Extra Large")
}

enum class ReviewButtonMode(val displayName: String) {
    FourButton("4-Button"),
    ThreeButton("3-Button (No Easy)"),
    TwoButton("2-Button (Hard/Good)"),
    OneButton("1-Button (Good only)"),
    Classic("Classic Anki"),
    Minimalist("Minimalist"),
    LearningOnly("Learning Phase"),
    ReviewOnly("Review Phase")
}

// ── History ──

data class HistoryEntry(
    val id: Long = 0,
    val type: HistoryEntryType,
    val timestamp: Instant = Clock.System.now(),
    val description: String = "",
    val cardIds: List<String> = emptyList(),
    val deckId: String? = null,
    val undoable: Boolean = true,
    val undoData: String? = null
)

enum class HistoryEntryType(val displayName: String) {
    Review("Review"),
    Import("Import"),
    Export("Export"),
    Edit("Edit"),
    Delete("Delete"),
    Restore("Restore"),
    BulkOperation("Bulk Operation"),
    TagChange("Tag Change"),
    FlagChange("Flag Change"),
    DeckChange("Deck Change"),
    NoteChange("Note Change"),
    StatusChange("Status Change"),
    ScheduleChange("Schedule Change"),
    BackupCreated("Backup Created"),
    BackupRestored("Backup Restored"),
    PluginAction("Plugin Action")
}

// ── Undo System ──

data class UndoState(
    val history: List<HistoryEntry> = emptyList(),
    val undoStack: List<HistoryEntry> = emptyList(),
    val redoStack: List<HistoryEntry> = emptyList()
)

// ── Backup ──

data class BackupConfigV2(
    val autoBackupEnabled: Boolean = true,
    val autoBackupIntervalHours: Int = 24,
    val maxAutoBackups: Int = 30,
    val compressionEnabled: Boolean = true,
    val compressionLevel: Int = 6,
    val verifyAfterBackup: Boolean = true,
    val includeMedia: Boolean = true,
    val includeSettings: Boolean = true,
    val includePlugins: Boolean = true,
    val backupPath: String = "",
    val lastBackupTime: Instant? = null
)

data class BackupPointV2(
    val id: String = "",
    val name: String = "",
    val timestamp: Instant = Clock.System.now(),
    val sizeBytes: Long = 0,
    val isAutoBackup: Boolean = false,
    val isVerified: Boolean = false,
    val compressionRatio: Float = 0f,
    val includesMedia: Boolean = true,
    val includesSettings: Boolean = true,
    val includesPlugins: Boolean = true,
    val checksum: String = ""
)

// ── Keyboard Shortcuts ──

data class ShortcutEntryV2(
    val id: String = "",
    val actionName: String = "",
    val category: ShortcutCategory = ShortcutCategory.General,
    val primaryKey: KeyCombination? = null,
    val secondaryKey: KeyCombination? = null,
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = true,
    val description: String = "",
    val icon: @Composable () -> Unit = {}
)

data class KeyCombination(
    val key: String = "",
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false
) {
    val displayText: String get() = buildString {
        if (ctrl) append("Ctrl+")
        if (alt) append("Alt+")
        if (shift) append("Shift+")
        if (meta) append("Win+")
        append(key)
    }

    val isDefined: Boolean get() = key.isNotBlank()
}

enum class ShortcutCategory(val displayName: String) {
    General("General"),
    Review("Review"),
    Browse("Browse"),
    Deck("Deck Management"),
    Editing("Editing"),
    Tags("Tags"),
    Flags("Flags"),
    Statistics("Statistics"),
    Backup("Backup"),
    Plugins("Plugins")
}

data class ShortcutProfileV2(
    val id: String = "",
    val name: String = "Default",
    val shortcuts: List<ShortcutEntryV2> = emptyList(),
    val isBuiltIn: Boolean = false
)

// ── Plugin System ──

data class PluginDefinitionV2(
    val id: String = "",
    val name: String = "",
    val version: String = "1.0.0",
    val author: String = "",
    val description: String = "",
    val entryPoint: String = "",
    val extensionPoints: List<PluginExtensionPoint> = emptyList(),
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val settings: Map<String, String> = emptyMap(),
    val permissions: List<PluginPermission> = emptyList(),
    val icon: String = "🧩"
)

enum class PluginExtensionPoint(val displayName: String) {
    ImportFormat("Import Format"),
    ExportFormat("Export Format"),
    Dictionary("Dictionary"),
    StatisticsPanel("Statistics Panel"),
    StudyMode("Study Mode"),
    Theme("Theme"),
    DeckSource("Deck Source"),
    MediaProvider("Media Provider"),
    AudioSource("Audio Source"),
    DataSync("Data Sync"),
    Widget("Widget"),
    CardTemplate("Card Template")
}

enum class PluginPermission(val displayName: String) {
    ReadCards("Read Cards"),
    WriteCards("Write Cards"),
    ReadDecks("Read Decks"),
    WriteDecks("Write Decks"),
    ReadTags("Read Tags"),
    WriteTags("Write Tags"),
    ReadSettings("Read Settings"),
    WriteSettings("Write Settings"),
    NetworkAccess("Network Access"),
    FileAccess("File Access"),
    AudioPlayback("Audio Playback"),
    AudioRecording("Audio Recording")
}

// ── Bulk Actions ──

data class BulkActionV2(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: @Composable () -> Unit = {},
    val action: (List<String>) -> Unit = {},
    val requiresConfirmation: Boolean = true,
    val canBeUndone: Boolean = true
)

// ── Search ──

data class SearchQueryV2(
    val text: String = "",
    val fields: Set<SearchField> = setOf(SearchField.All),
    val tagIds: List<Long> = emptyList(),
    val flagTypes: List<CardFlagType> = emptyList(),
    val statuses: List<CardStatus> = emptyList(),
    val deckIds: List<String> = emptyList(),
    val jlptLevel: Int? = null,
    val minStrokeCount: Int? = null,
    val maxStrokeCount: Int? = null,
    val minFrequency: Int? = null,
    val maxFrequency: Int? = null,
    val minInterval: Int? = null,
    val maxInterval: Int? = null,
    val minEase: Float? = null,
    val maxEase: Float? = null,
    val minLapses: Int? = null,
    val maxLapses: Int? = null,
    val minReviews: Int? = null,
    val maxReviews: Int? = null,
    val minAccuracy: Float? = null,
    val maxAccuracy: Float? = null,
    val createdAfter: String? = null,
    val createdBefore: String? = null,
    val modifiedAfter: String? = null,
    val modifiedBefore: String? = null,
    val lastReviewedAfter: String? = null,
    val lastReviewedBefore: String? = null,
    val isRegex: Boolean = false,
    val matchCase: Boolean = false,
    val sortBy: SearchSortField = SearchSortField.Relevance,
    val sortAscending: Boolean = true
)

enum class SearchField(val displayName: String) {
    All("All Fields"),
    Kanji("Kanji"),
    Kana("Kana"),
    Meaning("Meaning"),
    Reading("Reading"),
    Tag("Tag"),
    Flag("Flag"),
    Deck("Deck"),
    Notes("Notes"),
    StrokeCount("Stroke Count"),
    JLPT("JLPT Level"),
    Frequency("Frequency"),
    Status("Card Status"),
    Created("Created Date"),
    Modified("Modified Date"),
    Interval("Interval"),
    Ease("Ease"),
    Lapses("Lapses"),
    Reviews("Review Count"),
    Accuracy("Accuracy"),
    SrsStage("SRS Stage")
}

enum class SearchSortField(val displayName: String) {
    Relevance("Relevance"),
    Kanji("Kanji (A-Z)"),
    Reading("Reading (A-Z)"),
    Meaning("Meaning (A-Z)"),
    Deck("Deck"),
    Created("Created Date"),
    Modified("Modified Date"),
    Frequency("Frequency"),
    StrokeCount("Stroke Count"),
    JLPT("JLPT Level"),
    Interval("Interval"),
    Ease("Ease"),
    Reviews("Review Count"),
    Lapses("Lapses"),
    Accuracy("Accuracy"),
    LastReviewed("Last Reviewed"),
    NextReview("Next Review"),
    Status("Status"),
    Flag("Flag")
}

// ── Export/Import ──

data class ExportConfigV2(
    val format: ExportFormatV2 = ExportFormatV2.CSV,
    val includeTags: Boolean = true,
    val includeFlags: Boolean = true,
    val includeNotes: Boolean = true,
    val includeStatus: Boolean = true,
    val includeStats: Boolean = true,
    val includeScheduling: Boolean = true,
    val includeMedia: Boolean = false,
    val includeCreatedDates: Boolean = true,
    val includeModifiedDates: Boolean = true,
    val includeDeckStructure: Boolean = true,
    val delimiter: String = ",",
    val encoding: String = "UTF-8",
    val dateFormat: String = "yyyy-MM-dd",
    val includeHeaders: Boolean = true,
    val selectedDecks: List<String> = emptyList(),
    val selectedTags: List<Long> = emptyList(),
    val selectedFlags: List<CardFlagType> = emptyList(),
    val selectedStatuses: List<CardStatus> = emptyList()
)

data class ImportConfigV2(
    val format: ImportFormatV2 = ImportFormatV2.CSV,
    val appendToDeck: Boolean = true,
    val targetDeck: String = "",
    val matchByField: String = "kanji",
    val overwriteExisting: Boolean = false,
    val createMissingDecks: Boolean = true,
    val importTags: Boolean = true,
    val importFlags: Boolean = true,
    val importNotes: Boolean = true,
    val importStatus: Boolean = false,
    val importStats: Boolean = false,
    val importScheduling: Boolean = false,
    val delimiter: String = ",",
    val encoding: String = "UTF-8",
    val dateFormat: String = "yyyy-MM-dd",
    val skipFirstRow: Boolean = true,
    val fieldMapping: Map<String, String> = emptyMap(),
    val validateOnly: Boolean = false,
    val dryRun: Boolean = false
)

enum class ExportFormatV2(val displayName: String, val extension: String) {
    CSV("CSV (Comma Separated)", "csv"),
    JSON("JSON (JavaScript Object Notation)", "json"),
    TXT("Plain Text", "txt"),
    Markdown("Markdown", "md"),
    APKG("Anki Package (APKG)", "apkg"),
    XML("XML", "xml"),
    Excel("Excel (XLSX)", "xlsx")
}

enum class ImportFormatV2(val displayName: String, val extension: String) {
    CSV("CSV (Comma Separated)", "csv"),
    JSON("JSON (JavaScript Object Notation)", "json"),
    TXT("Plain Text", "txt"),
    Markdown("Markdown", "md"),
    APKG("Anki Package (APKG)", "apkg"),
    XML("XML", "xml"),
    Excel("Excel (XLSX)", "xlsx")
}

// ── Statistics ──

data class StatsOverviewV2(
    val todayReviews: Int = 0,
    val todayCardsStudied: Int = 0,
    val todayTimeStudied: Long = 0,
    val todayAccuracy: Float = 0f,
    val todayNewCards: Int = 0,
    val todayLapses: Int = 0,
    val weekReviews: Int = 0,
    val weekTimeStudied: Long = 0,
    val weekAccuracy: Float = 0f,
    val monthReviews: Int = 0,
    val monthTimeStudied: Long = 0,
    val monthAccuracy: Float = 0f,
    val totalReviews: Int = 0,
    val totalCards: Int = 0,
    val totalTimeStudied: Long = 0,
    val overallAccuracy: Float = 0f,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val averageReviewsPerDay: Float = 0f,
    val averageTimePerCard: Long = 0,
    val cardsDue: Int = 0,
    val cardsNew: Int = 0,
    val cardsLearning: Int = 0,
    val cardsYoung: Int = 0,
    val cardsMature: Int = 0,
    val cardsRelearning: Int = 0,
    val cardsSuspended: Int = 0,
    val cardsBuried: Int = 0,
    val cardsArchived: Int = 0,
    val flaggedCards: Int = 0,
    val averageInterval: Int = 0,
    val averageEase: Float = 2.5f,
    val forecastNextDays: List<Int> = emptyList(),
    val retentionRate: Float = 0f,
    val predictedRetention: Float = 0f
)

data class PerDeckStatsV2(
    val deckId: String = "",
    val deckName: String = "",
    val cardCount: Int = 0,
    val dueCount: Int = 0,
    val newCount: Int = 0,
    val learningCount: Int = 0,
    val matureCount: Int = 0,
    val accuracy: Float = 0f,
    val retention: Float = 0f,
    val averageInterval: Int = 0,
    val averageEase: Float = 2.5f,
    val totalReviews: Int = 0,
    val totalLapses: Int = 0,
    val totalTimeStudied: Long = 0,
    val difficultCards: Int = 0,
    val forgottenCards: Int = 0,
    val leastReviewedCards: Int = 0,
    val completionRate: Float = 0f,
    val dailyReviewCounts: Map<String, Int> = emptyMap(),
    val dailyAccuracy: Map<String, Float> = emptyMap()
)

data class PerCardStatsV2(
    val cardId: String = "",
    val character: String = "",
    val reading: String = "",
    val meaning: String = "",
    val totalReviews: Int = 0,
    val totalTimeMs: Long = 0,
    val averageTimeMs: Long = 0,
    val accuracy: Float = 0f,
    val retention: Float = 0f,
    val ease: Float = 2.5f,
    val interval: Int = 0,
    val lapses: Int = 0,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val firstReview: String = "",
    val lastReview: String = "",
    val nextReview: String = "",
    val reviewHistory: List<ReviewEvent> = emptyList(),
    val intervalHistory: List<IntervalEvent> = emptyList(),
    val easeHistory: List<EaseEvent> = emptyList(),
    val learningCurve: List<LearningCurvePoint> = emptyList(),
    val accuracyByDay: Map<String, Float> = emptyMap(),
    val reviewsByDay: Map<String, Int> = emptyMap(),
    val averageTimeByDay: Map<String, Long> = emptyMap()
)

data class ReviewEvent(
    val timestamp: String = "",
    val ease: CardDifficulty = CardDifficulty.Good,
    val timeMs: Long = 0,
    val intervalBefore: Int = 0,
    val intervalAfter: Int = 0
)

data class IntervalEvent(
    val date: String = "",
    val interval: Int = 0
)

data class EaseEvent(
    val date: String = "",
    val ease: Float = 2.5f
)

data class LearningCurvePoint(
    val reviewNumber: Int = 0,
    val accuracy: Float = 0f,
    val averageTimeMs: Long = 0
)

// ── Heatmap ──

data class HeatmapDayV2(
    val date: LocalDate,
    val count: Int,
    val cardsStudied: Int = 0,
    val newCards: Int = 0,
    val reviewCards: Int = 0,
    val accuracy: Float = 0f,
    val timeStudied: Long = 0L,
    val mistakes: Int = 0
)

data class HeatmapDataV2(
    val year: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year,
    val days: Map<LocalDate, HeatmapDayV2> = emptyMap(),
    val totalReviews: Int = 0,
    val totalCardsStudied: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val averageAccuracy: Float = 0f,
    val totalStudyTime: Long = 0L
)

data class HeatmapData(
    val dailyCounts: Map<String, Int> = emptyMap(),
    val dailyAccuracy: Map<String, Float> = emptyMap(),
    val dailyTime: Map<String, Long> = emptyMap(),
    val dailyNewCards: Map<String, Int> = emptyMap(),
    val dailyReviews: Map<String, Int> = emptyMap(),
    val dailyLapses: Map<String, Int> = emptyMap(),
    val dailyMistakes: Map<String, List<String>> = emptyMap(),
    val dailyDecksStudied: Map<String, List<String>> = emptyMap(),
    val startDate: String = "",
    val endDate: String = "",
    val totalDaysStudied: Int = 0,
    val maxInDay: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

data class DayDetailData(
    val date: String = "",
    val totalReviews: Int = 0,
    val newCards: Int = 0,
    val reviewCards: Int = 0,
    val totalTimeMs: Long = 0,
    val accuracy: Float = 0f,
    val decksStudied: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val timeline: List<TimelineEntry> = emptyList()
)

data class TimelineEntry(
    val time: String = "",
    val cardId: String = "",
    val character: String = "",
    val result: String = "",
    val timeMs: Long = 0
)

// ── Card Browser Columns ──

data class BrowserColumn(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val width: Int = 120,
    val sortable: Boolean = true,
    val alignment: ColumnAlignment = ColumnAlignment.Left
)

enum class ColumnAlignment {
    Left, Center, Right
}

val defaultBrowserColumns = listOf(
    BrowserColumn("select", "", isVisible = true, width = 40, sortable = false, alignment = ColumnAlignment.Center),
    BrowserColumn("kanji", "Card", isVisible = true, width = 90, alignment = ColumnAlignment.Center),
    BrowserColumn("deck", "Deck", isVisible = true, width = 130),
    BrowserColumn("tags", "Tags", isVisible = true, width = 110),
    BrowserColumn("stability", "Stability", isVisible = true, width = 90, alignment = ColumnAlignment.Right),
    BrowserColumn("difficulty", "Difficulty", isVisible = true, width = 90, alignment = ColumnAlignment.Right),
    BrowserColumn("due", "Due", isVisible = true, width = 80, alignment = ColumnAlignment.Center),
    BrowserColumn("ease", "Ease", isVisible = true, width = 70, alignment = ColumnAlignment.Right),
    BrowserColumn("reading", "Reading", isVisible = true, width = 110),
    BrowserColumn("meaning", "Meaning", isVisible = true, width = 160),
    BrowserColumn("flag", "Flag", isVisible = true, width = 60, alignment = ColumnAlignment.Center),
    BrowserColumn("status", "Status", isVisible = true, width = 90),
    BrowserColumn("interval", "Interval", isVisible = true, width = 70, alignment = ColumnAlignment.Right),
    BrowserColumn("reviews", "Reviews", isVisible = true, width = 70, alignment = ColumnAlignment.Right),
    BrowserColumn("lapses", "Lapses", isVisible = true, width = 60, alignment = ColumnAlignment.Right),
    BrowserColumn("created", "Created", isVisible = false, width = 110),
    BrowserColumn("modified", "Modified", isVisible = false, width = 110),
    BrowserColumn("nextReview", "Next Review", isVisible = false, width = 110),
    BrowserColumn("lastReview", "Last Review", isVisible = false, width = 110),
    BrowserColumn("accuracy", "Accuracy", isVisible = false, width = 80, alignment = ColumnAlignment.Right),
    BrowserColumn("timeStudied", "Time Studied", isVisible = false, width = 90, alignment = ColumnAlignment.Right),
    BrowserColumn("jlpt", "JLPT", isVisible = false, width = 50, alignment = ColumnAlignment.Center),
    BrowserColumn("strokeCount", "Strokes", isVisible = false, width = 60, alignment = ColumnAlignment.Right),
    BrowserColumn("frequency", "Freq", isVisible = false, width = 60, alignment = ColumnAlignment.Right),
    BrowserColumn("srsStage", "SRS", isVisible = false, width = 50, alignment = ColumnAlignment.Center),
    BrowserColumn("note", "Note", isVisible = false, width = 150)
)

// ── Card with Full Metadata for Browser ──

data class FullCardDataV2(
    val key: String = "",
    val practiceType: Long = 0,
    val character: String = "",
    val reading: String = "",
    val meaning: String = "",
    val deckName: String = "",
    val deckId: String = "",
    val tags: List<CardTag> = emptyList(),
    val flag: CardFlagType = CardFlagType.None,
    val status: CardStatus = CardStatus.New,
    val note: String = "",
    val interval: Int = 0,
    val ease: Float = 2.5f,
    val reviews: Int = 0,
    val lapses: Int = 0,
    val accuracy: Float = 0f,
    val totalTimeMs: Long = 0,
    val createdDate: String = "",
    val modifiedDate: String = "",
    val nextReviewDate: String = "",
    val lastReviewDate: String = "",
    val jlptLevel: Int? = null,
    val strokeCount: Int? = null,
    val frequencyRank: Int? = null,
    val srsStage: Int = 0,
    val isSuspended: Boolean = false,
    val isBuried: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false
)

// ── Filtered Deck / Custom Study ──

data class FilteredDeckConfigV2(
    val name: String = "",
    val searchQuery: String = "",
    val maxCards: Int = 100,
    val order: FilteredDeckOrderV2 = FilteredDeckOrderV2.DueFirst,
    val reschedule: Boolean = true,
    val previewBeforeFilter: Boolean = false,
    val stepSize: Int = 0,
    val startNow: Boolean = true
)

enum class FilteredDeckOrderV2(val displayName: String) {
    DueFirst("Due First"),
    NewFirst("New First"),
    ReviewFirst("Review First"),
    LowestEase("Lowest Ease First"),
    HighestEase("Highest Ease First"),
    Random("Random"),
    MostLapses("Most Lapses First"),
    ByDeck("Group by Deck"),
    OrderAdded("Order Added"),
    OrderModified("Order Modified"),
    MostRecent("Most Recent Review"),
    LeastRecent("Least Recent Review"),
    MostTime("Most Time Studied"),
    LeastTime("Least Time Studied")
}

// ── Flag Statistics ──

data class FlagStatsV2(
    val flagType: CardFlagType,
    val totalCards: Int = 0,
    val dueCards: Int = 0,
    val newCards: Int = 0,
    val averageEase: Float = 2.5f,
    val averageAccuracy: Float = 0f,
    val totalReviews: Int = 0,
    val totalLapses: Int = 0,
    val retentionRate: Float = 0f
)

// ── SRS Algorithm Settings ──

data class SrsSettings(
    val algorithm: SrsAlgorithm = SrsAlgorithm.SM2,
    val initialInterval: Int = 1,
    val easyInterval: Int = 4,
    val intervalModifier: Float = 1.0f,
    val maximumInterval: Int = 36500,
    val startingEase: Float = 2.5f,
    val easyBonus: Float = 1.3f,
    val hardInterval: Float = 1.2f,
    val newInterval: Float = 0.0f,
    val graduatingInterval: Int = 1,
    val easyGraduatingInterval: Int = 2,
    val learningSteps: List<Int> = listOf(1, 10),
    val relearningSteps: List<Int> = listOf(10),
    val minimumCorrectForGraduation: Int = 1,
    val leechThreshold: Int = 8,
    val leechAction: LeechAction = LeechAction.Suspend,
    val buryRelatedNewCards: Boolean = true,
    val buryRelatedReviews: Boolean = false,
    val newCardsPerDay: Int = 20,
    val reviewsPerDay: Int = 200,
    val maxReviewsPerDay: Int = 9999,
    val newCardsOrder: NewCardsOrder = NewCardsOrder.DeckThenDue
)

enum class SrsAlgorithm(val displayName: String) {
    SM2("SM-2 (Anki Classic)"),
    SM3("SM-3"),
    SM4("SM-4"),
    SM5("SM-5 (Anki 2.1+)"),
    FSRS("FSRS (Free Spaced Repetition Scheduler)"),
    Custom("Custom")
}

enum class LeechAction(val displayName: String) {
    TagOnly("Tag Only"),
    Suspend("Suspend"),
    Both("Tag and Suspend")
}

enum class NewCardsOrder(val displayName: String) {
    DeckThenDue("Deck then Due"),
    DueThenDeck("Due then Deck"),
    Random("Random"),
    AscendingInterval("Ascending Interval"),
    DescendingInterval("Descending Interval"),
    AscendingEase("Ascending Ease"),
    DescendingEase("Descending Ease")
}

// ── Study Session ──

data class StudySession(
    val id: String = "",
    val deckId: String = "",
    val deckName: String = "",
    val startTime: Instant = Clock.System.now(),
    val endTime: Instant? = null,
    val cardsStudied: Int = 0,
    val cardsNew: Int = 0,
    val cardsReview: Int = 0,
    val cardsLearning: Int = 0,
    val cardsRelearning: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val totalTimeMs: Long = 0,
    val accuracy: Float = 0f,
    val isComplete: Boolean = false
)

// ── Cram Mode ──

data class CramConfigV2(
    val name: String = "Cram Session",
    val sourceDeck: String = "",
    val cardLimit: Int = 50,
    val orderType: CramOrder = CramOrder.Random,
    val showBothSides: Boolean = true,
    val autoAdvance: Boolean = true,
    val autoAdvanceDelayMs: Long = 3000,
    val repeatIncorrect: Boolean = true,
    val maxRepetitions: Int = 3,
    val stopAfterCorrect: Boolean = false,
    val timeLimitMinutes: Int = 0,
    val targetCorrectCount: Int = 0,
    val includeSuspended: Boolean = false,
    val includeBuried: Boolean = false,
    val shuffleCards: Boolean = true,
    val startNow: Boolean = false,
    val previewBeforeFilter: Boolean = true
)

enum class CramOrder(val displayName: String) {
    Random("Random"),
    Sequential("Sequential"),
    MostLapses("Most Lapses First"),
    LeastReviewed("Least Reviewed First"),
    LowestEase("Lowest Ease First"),
    Newest("Newest First"),
    Oldest("Oldest First"),
    ByAccuracy("Lowest Accuracy First")
}

// ── Notification / Toast ──

data class ActionToast(
    val id: Long = 0,
    val message: String = "",
    val type: ToastType = ToastType.Info,
    val durationMs: Long = 3000,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

enum class ToastType {
    Success, Error, Warning, Info
}

// ── Keyboard Shortcut Presets ──

val defaultShortcuts = listOf(
    ShortcutEntryV2("showAnswer", "Show Answer", ShortcutCategory.Review,
        KeyCombination("Space")),
    ShortcutEntryV2("again", "Again", ShortcutCategory.Review,
        KeyCombination("1")),
    ShortcutEntryV2("hard", "Hard", ShortcutCategory.Review,
        KeyCombination("2")),
    ShortcutEntryV2("good", "Good", ShortcutCategory.Review,
        KeyCombination("3")),
    ShortcutEntryV2("easy", "Easy", ShortcutCategory.Review,
        KeyCombination("4")),
    ShortcutEntryV2("editCard", "Edit Card", ShortcutCategory.Review,
        KeyCombination("E")),
    ShortcutEntryV2("flagCard", "Flag Card", ShortcutCategory.Review,
        KeyCombination("F")),
    ShortcutEntryV2("suspendCard", "Suspend Card", ShortcutCategory.Review,
        KeyCombination("S")),
    ShortcutEntryV2("buryCard", "Bury Card", ShortcutCategory.Review,
        KeyCombination("B")),
    ShortcutEntryV2("playAudio", "Play Audio", ShortcutCategory.Review,
        KeyCombination("R")),
    ShortcutEntryV2("showHint", "Show Hint", ShortcutCategory.Review,
        KeyCombination("H")),
    ShortcutEntryV2("undo", "Undo", ShortcutCategory.General,
        KeyCombination("Z", ctrl = true)),
    ShortcutEntryV2("search", "Search", ShortcutCategory.Browse,
        KeyCombination("F", ctrl = true)),
    ShortcutEntryV2("browser", "Card Browser", ShortcutCategory.Browse,
        KeyCombination("B", ctrl = true)),
    ShortcutEntryV2("deckBrowser", "Deck Browser", ShortcutCategory.Deck,
        KeyCombination("D", ctrl = true)),
    ShortcutEntryV2("stats", "Statistics", ShortcutCategory.Statistics,
        KeyCombination("T", ctrl = true)),
    ShortcutEntryV2("tagManager", "Tag Manager", ShortcutCategory.Tags,
        KeyCombination("T", ctrl = true, shift = true)),
    ShortcutEntryV2("flagManager", "Flag Manager", ShortcutCategory.Flags,
        KeyCombination("M", ctrl = true, shift = true)),
    ShortcutEntryV2("export", "Export", ShortcutCategory.General,
        KeyCombination("E", ctrl = true, shift = true)),
    ShortcutEntryV2("import", "Import", ShortcutCategory.General,
        KeyCombination("I", ctrl = true)),
    ShortcutEntryV2("backup", "Backup", ShortcutCategory.Backup,
        KeyCombination("B", ctrl = true, alt = true)),
    ShortcutEntryV2("restore", "Restore", ShortcutCategory.Backup,
        KeyCombination("R", ctrl = true, alt = true))
)

// ── Preview Mode State ──

data class PreviewState(
    val isActive: Boolean = false,
    val cards: List<FullCardDataV2> = emptyList(),
    val currentIndex: Int = 0,
    val showAnswer: Boolean = false,
    val isReversed: Boolean = false,
    val autoAdvance: Boolean = false,
    val autoAdvanceDelay: Long = 3000
)

// ── Helper Extension Functions ──

fun CardFlagType.colorFromHex(): Color {
    return try {
        val hex = hexColor.removePrefix("#")
        val a = hex.substring(0..1).toInt(16)
        val r = hex.substring(2..3).toInt(16)
        val g = hex.substring(4..5).toInt(16)
        val b = hex.substring(6..7).toInt(16)
        Color(r, g, b, a)
    } catch (_: Exception) {
        Color.Gray
    }
}

fun CardStatus.nextStatus(): CardStatus = when (this) {
    CardStatus.New -> CardStatus.Learning
    CardStatus.Learning -> CardStatus.Young
    CardStatus.Young -> CardStatus.Mature
    CardStatus.Mature -> CardStatus.Relearning
    CardStatus.Relearning -> CardStatus.Mature
    CardStatus.Suspended -> CardStatus.New
    CardStatus.Buried -> CardStatus.New
    CardStatus.Archived -> CardStatus.New
}

fun CardStatus.isActive(): Boolean = this in listOf(
    CardStatus.New, CardStatus.Learning, CardStatus.Young,
    CardStatus.Mature, CardStatus.Relearning
)

fun CardStatus.isHidden(): Boolean = this in listOf(
    CardStatus.Suspended, CardStatus.Buried, CardStatus.Archived
)

fun formatFloat(value: Float, decimals: Int = 1): String {
    val factor = when (decimals) {
        0 -> 1
        1 -> 10
        2 -> 100
        3 -> 1000
        else -> 100
    }
    val rounded = (value * factor).toInt()
    val intPart = rounded / factor
    val fracPart = kotlin.math.abs(rounded % factor)
    return if (decimals > 0) "$intPart.${
        fracPart.toString().padStart(decimals, '0')
    }" else intPart.toString()
}

fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

fun formatInterval(days: Int): String = when {
    days < 1 -> "<1d"
    days == 1 -> "1d"
    days < 30 -> "${days}d"
    days < 365 -> {
        val months = days / 30
        "${months}mo"
    }
    else -> {
        val years = days / 365
        "${years}y"
    }
}

fun countByStatus(cards: List<KaiteyoCard>, status: CardStatus): Int =
    cards.count { it.status == status }

fun countByFlag(cards: List<KaiteyoCard>, flag: CardFlagType): Int =
    cards.count { it.flag == flag }

fun filterCardsByQuery(cards: List<KaiteyoCard>, query: String): List<KaiteyoCard> {
    if (query.isBlank()) return cards
    val q = query.lowercase()
    return cards.filter { card ->
        card.character.lowercase().contains(q) ||
        card.meaning.lowercase().contains(q) ||
        card.reading.lowercase().contains(q) ||
        card.deck.lowercase().contains(q) ||
        card.tagNames.any { it.lowercase().contains(q) } ||
        card.notes.lowercase().contains(q) ||
        card.flag.displayName.lowercase().contains(q) ||
        card.status.displayName.lowercase().contains(q)
    }
}

fun sortCards(
    cards: List<KaiteyoCard>,
    field: SearchSortField,
    ascending: Boolean
): List<KaiteyoCard> {
    val sorted = when (field) {
        SearchSortField.Kanji -> cards.sortedBy { it.character }
        SearchSortField.Reading -> cards.sortedBy { it.reading }
        SearchSortField.Meaning -> cards.sortedBy { it.meaning }
        SearchSortField.Deck -> cards.sortedBy { it.deck }
        SearchSortField.Created -> cards.sortedBy { it.createdAt }
        SearchSortField.Modified -> cards.sortedBy { it.modifiedAt }
        SearchSortField.Interval -> cards.sortedBy { it.interval }
        SearchSortField.Ease -> cards.sortedBy { it.ease }
        SearchSortField.Reviews -> cards.sortedBy { it.reviewCount }
        SearchSortField.Lapses -> cards.sortedBy { it.lapses }
        SearchSortField.Accuracy -> cards.sortedBy { it.accuracy }
        SearchSortField.LastReviewed -> cards.sortedBy { it.lastReviewed }
        SearchSortField.NextReview -> cards.sortedBy { it.lastReviewed }
        SearchSortField.Status -> cards.sortedBy { it.status.ordinal }
        SearchSortField.Flag -> cards.sortedBy { it.flag.ordinal }
        SearchSortField.Frequency -> cards.sortedBy { it.character.length }
        SearchSortField.StrokeCount -> cards.sortedBy { it.character.length }
        SearchSortField.JLPT -> cards.sortedBy { it.character.length }
        SearchSortField.Relevance -> cards
    }
    return if (ascending) sorted else sorted.reversed()
}

// ── Mock Data Generators ──

fun generateMockCards(count: Int = 20): List<KaiteyoCard> {
    val kanjiList = listOf("水", "火", "木", "金", "土", "日", "月", "星", "空", "海",
        "山", "川", "花", "鳥", "魚", "虫", "犬", "猫", "馬", "牛")
    val meaningList = listOf("Water", "Fire", "Tree", "Gold", "Earth", "Sun", "Moon",
        "Star", "Sky", "Sea", "Mountain", "River", "Flower", "Bird", "Fish",
        "Insect", "Dog", "Cat", "Horse", "Cow")
    val readingList = listOf("みず/スイ", "ひ/カ", "き/モク", "きん/コン", "つち/ド",
        "にち/ジツ", "つき/ゲツ", "ほし/セイ", "そら/クウ", "うみ/カイ",
        "やま/サン", "かわ/セン", "はな/カ", "とり/チョウ", "さかな/ギョ",
        "むし/チュウ", "いぬ/ケン", "ねこ/ビョウ", "うま/バ", "うし/ギュウ")
    val deckList = listOf("N5 Kanji", "N4 Kanji", "N3 Kanji", "Core 2000", "Core 6000")
    val flags = CardFlagType.entries.drop(1)
    val statuses = CardStatus.entries

    return (0 until count).map { i ->
        val idx = i % kanjiList.size
        KaiteyoCard(
            id = "card_${i + 1}".padStart(8, '0'),
            character = kanjiList[idx],
            meaning = meaningList[idx],
            reading = readingList[idx],
            deck = deckList[i % deckList.size],
            deckId = (i % 5).toLong(),
            tagNames = mutableListOf(
                listOf("jlpt-n5", "common", "kanji")[i % 3],
                listOf("water", "fire", "earth")[i % 3]
            ),
            flag = if (i % 4 == 0) flags[i % 7] else CardFlagType.None,
            status = statuses[i % statuses.size],
            interval = (i * 3) % 365,
            ease = 1.5f + (i % 10) * 0.1f,
            reviewCount = i * 7 % 100,
            lapses = i % 5,
            accuracy = 0.5f + (i % 5) * 0.1f,
            createdAt = "2026-01-${(i % 28) + 1}".padStart(10, '0'),
            modifiedAt = "2026-07-${(i % 28) + 1}".padStart(10, '0'),
            lastReviewed = "2026-07-${(i % 28) + 1}".padStart(10, '0')
        )
    }
}

fun generateMockDecks(): List<KaiteyoDeck> = listOf(
    KaiteyoDeck(name = "JLPT", icon = "N", isPinned = true, cardCount = 650,
        children = mutableListOf(
            KaiteyoDeck(name = "N5 Kanji", cardCount = 120, newCount = 23, dueCount = 12),
            KaiteyoDeck(name = "N4 Kanji", cardCount = 180),
            KaiteyoDeck(name = "N3 Kanji", cardCount = 350))),
    KaiteyoDeck(name = "Vocabulary", icon = "語", isFavorite = true, cardCount = 1200,
        children = mutableListOf(
            KaiteyoDeck(name = "Core 2000", cardCount = 500),
            KaiteyoDeck(name = "Core 6000", cardCount = 700))),
    KaiteyoDeck(name = "Smart: Difficult", isSmart = true, isVirtual = true, icon = "⚡", cardCount = 45),
    KaiteyoDeck(name = "Smart: Forgotten", isSmart = true, isVirtual = true, icon = "🔄", cardCount = 23),
    KaiteyoDeck(name = "Archived 2025", isArchived = true, icon = "📦", cardCount = 0)
)

fun generateMockTags(): List<CardTag> = listOf(
    CardTag(id = 1, name = "jlpt-n5", color = "#FFC2FC8B"),
    CardTag(id = 2, name = "jlpt-n4", color = "#FFFEAB57"),
    CardTag(id = 3, name = "jlpt-n3", color = "#FF7BC8FF"),
    CardTag(id = 4, name = "common", color = "#FFA78BFA"),
    CardTag(id = 5, name = "rare", color = "#FFFF6B6B"),
    CardTag(id = 6, name = "animal", color = "#FFFFD93D", parentId = 4),
    CardTag(id = 7, name = "food", color = "#FFFFD93D", parentId = 4),
    CardTag(id = 8, name = "weather", color = "#FF7BC8FF", parentId = 4),
    CardTag(id = 9, name = "water", color = "#FF7BC8FF"),
    CardTag(id = 10, name = "fire", color = "#FFFF6B6B"),
    CardTag(id = 11, name = "earth", color = "#FFC2FC8B"),
    CardTag(id = 12, name = "metal", color = "#FFB0B0B0")
)

fun generateMockHeatmapData(): HeatmapData {
    val counts = mutableMapOf<String, Int>()
    val accuracy = mutableMapOf<String, Float>()
    val time = mutableMapOf<String, Long>()
    for (day in 1..365) {
        val date = "2026-${(day / 30 + 1).toString().padStart(2, '0')}-${(day % 28 + 1).toString().padStart(2, '0')}"
        if (day % 3 != 0) {
            counts[date] = (5..50).random()
            accuracy[date] = 0.7f + (0..30).random() * 0.01f
            time[date] = (300000..3600000).random().toLong()
        }
    }
    return HeatmapData(
        dailyCounts = counts,
        dailyAccuracy = accuracy,
        dailyTime = time,
        startDate = "2026-01-01",
        endDate = "2026-12-31",
        totalDaysStudied = counts.size,
        maxInDay = counts.values.maxOrNull() ?: 50,
        currentStreak = 7,
        longestStreak = 45
    )
}
