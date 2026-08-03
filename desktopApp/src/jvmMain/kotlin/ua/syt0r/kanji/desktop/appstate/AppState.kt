package ua.syt0r.kanji.desktop.appstate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ua.syt0r.kanji.desktop.data.buildDemoCards
import ua.syt0r.kanji.desktop.data.buildStressDataset
import ua.syt0r.kanji.desktop.designsystem.DsToastHost
import ua.syt0r.kanji.desktop.engine.collections.CollectionStore
import ua.syt0r.kanji.desktop.engine.history.ActivityLog
import ua.syt0r.kanji.desktop.engine.plugin.PluginRegistry
import ua.syt0r.kanji.desktop.engine.search.SavedFilterStore
import ua.syt0r.kanji.desktop.engine.search.SearchEngine
import ua.syt0r.kanji.desktop.engine.settings.SettingsEngine
import ua.syt0r.kanji.desktop.engine.shortcuts.ShortcutDispatcher
import ua.syt0r.kanji.desktop.engine.shortcuts.ShortcutRegistry
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.sync.SyncEngine
import ua.syt0r.kanji.desktop.engine.theming.ThemePresets
import ua.syt0r.kanji.desktop.engine.theming.ThemeSerializer
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryService
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.mining.MiningEngine
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.browser.BrowserEngine
import ua.syt0r.kanji.desktop.engine.ocr.OcrEngine
import ua.syt0r.kanji.desktop.engine.api.LocalApiServer
import ua.syt0r.kanji.desktop.model.CollectionDef
import ua.syt0r.kanji.desktop.model.CollectionKind
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewLogEntry
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.StudyDaySummary
import ua.syt0r.kanji.desktop.model.ToastKind
import ua.syt0r.kanji.desktop.engine.review.ReviewSettings
import ua.syt0r.kanji.desktop.engine.review.ReviewSession
import ua.syt0r.kanji.desktop.engine.review.ReviewSessionStats
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** A single view of the workspace. */
enum class WorkspaceView(val label: String, val icon: String) {
    Dashboard("Dashboard", "I"),
    Browser("Browser", "B"),
    Review("Review", "R"),
    Collections("Collections", "C"),
    Tags("Tags & Flags", "T"),
    Statistics("Statistics", "S"),
    History("Activity Log", "H"),
    Transfer("Import / Export", "E"),
    Sync("Sync", "Y"),
    Shortcuts("Shortcuts", "K"),
    Plugins("Plugins", "P"),
    ThemeStudio("Theme Studio", "M"),
    Settings("Settings", "G"),
    Contributions("Contributions", "A"),
    Dictionary("Dictionary", "D"),
    Mining("Mining", "M"),
    Media("Media", "V"),
    LearningBrowser("Learning Browser", "W"),
    Ocr("OCR", "O"),
    Integrations("Integrations", "A")
}

/** Type of browser display. */
enum class BrowserViewMode { Grid, List, Details }

/** Edge of the window the navigation dock attaches to. */
enum class NavPosition(val label: String) {
    Left("Left"),
    Right("Right"),
    Top("Top"),
    Bottom("Bottom")
}

/** How navigation is surfaced: persistent dock, floating launcher, or both. */
enum class NavMode(val label: String) {
    Traditional("Sidebar dock"),
    Floating("Floating launcher"),
    Both("Both")
}

/**
 * Central in-memory facade for the whole desktop suite.
 * Holds live state (cards, review log, navigation, selection)
 * and owns the engine singletons. Created once per window.
 */
class AppState(
    val settings: SettingsEngine = SettingsEngine(),
    val shortcutRegistry: ShortcutRegistry = ShortcutRegistry(),
    val shortcutDispatcher: ShortcutDispatcher = ShortcutDispatcher(shortcutRegistry),
    val filterStore: SavedFilterStore = SavedFilterStore(),
    val collections: CollectionStore = CollectionStore(),
    val activityLog: ActivityLog = ActivityLog(),
    val pluginRegistry: PluginRegistry = PluginRegistry(),
    val syncEngine: SyncEngine = SyncEngine(),
    val toastHost: DsToastHost = DsToastHost()
) {

init {
        loadWorkspacePanels()
        pluginRegistry.restoreSnapshot(settings.getString("plugins.installed"))
        settings.observe { key, _, newValue ->
            when (key) {
                "navigation.position" -> navPosition = NavPosition.entries.firstOrNull { it.name.lowercase() == newValue } ?: NavPosition.Left
                "navigation.mode" -> navMode = NavMode.entries.firstOrNull { it.name.lowercase() == newValue } ?: NavMode.Traditional
                "navigation.collapsed" -> navCollapsed = newValue.toBooleanStrictOrNull() ?: false
            }
        }
    }

    // ---------------------------------------------------------------
    // Learning workspace engines (dictionary, mining, media, browser,
    // OCR and the local integration API).
    // ---------------------------------------------------------------
    val dictionary = DictionaryService(DictionaryRepository(dictionaryDir()))
    val mining = MiningEngine(this)
    val media = MediaEngine()
    val browserEngine = BrowserEngine()
    val ocr = OcrEngine()
    val localApi = LocalApiServer(mining)

    private fun dictionaryDir(): java.io.File =
        java.io.File(System.getProperty("user.home"), ".kaiteyo/dictionary")

    // ---------------------------------------------------------------
    // Data
    // ---------------------------------------------------------------
    val cards = mutableStateListOf<DesktopCard>()
    val reviewLog = mutableStateListOf<ReviewLogEntry>()
    val summaries = mutableStateListOf<StudyDaySummary>()

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------
    var currentView by mutableStateOf(WorkspaceView.Dashboard)
    val openPanels = mutableStateListOf<OpenPanel>()
    var navPosition by mutableStateOf(
        NavPosition.entries.firstOrNull { it.name.lowercase() == settings.getString("navigation.position", "left") }
            ?: NavPosition.Left
    )
    var navCollapsed by mutableStateOf(settings.getBool("navigation.collapsed"))
    var navMode by mutableStateOf(
        NavMode.entries.firstOrNull { it.name.lowercase() == settings.getString("navigation.mode", "traditional") }
            ?: NavMode.Traditional
    )

    fun updateNavPosition(position: NavPosition) {
        navPosition = position
        settings.set("navigation.position", position.name.lowercase())
        activityLog.record(ActivityCategory.System, "Navigation moved to ${position.label}")
    }

    fun updateNavMode(mode: NavMode) {
        navMode = mode
        settings.set("navigation.mode", mode.name.lowercase())
        activityLog.record(ActivityCategory.System, "Navigation mode: ${mode.label}")
    }

    fun updateNavCollapsed(collapsed: Boolean) {
        navCollapsed = collapsed
        settings.set("navigation.collapsed", collapsed)
    }

    // ---------------------------------------------------------------
    // Browser state
    // ---------------------------------------------------------------
    var browserQuery by mutableStateOf("")
    var browserViewMode by mutableStateOf(BrowserViewMode.Grid)
    var browserShowPreview by mutableStateOf(true)
    var selectedCard by mutableStateOf<DesktopCard?>(null)
    val selectedCardIds = mutableStateListOf<String>()

    // ---------------------------------------------------------------
    // Review session state
    // ---------------------------------------------------------------
    var reviewSession by mutableStateOf<ReviewSession?>(null)
    var reviewSettings by mutableStateOf(ReviewSettings())
    val sessionResults = mutableStateListOf<ReviewResult>()
    var sessionStartedAt by mutableStateOf(Clock.System.now())
    var answerRevealed by mutableStateOf(false)
    var lastSessionStats by mutableStateOf<ReviewSessionStats?>(null)

    // ---------------------------------------------------------------
    // Theme studio state
    // ---------------------------------------------------------------
    var activeThemeId by mutableStateOf(ThemePresets.default.id)
    var themeStudioDirty by mutableStateOf(false)

    // ---------------------------------------------------------------
    // Sync state
    // ---------------------------------------------------------------
    var syncBusy by mutableStateOf(false)
    var lastSyncAt by mutableStateOf<Instant?>(null)
    var lastSyncMessage by mutableStateOf("Never synced")

    // ---------------------------------------------------------------
    // Search helpers
    // ---------------------------------------------------------------
    fun searchCards(query: String): List<DesktopCard> =
        if (query.isBlank()) cards.toList()
        else cards.filter { SearchEngine.matches(it, query) }

    fun filterByCollection(def: CollectionDef): List<DesktopCard> {
        val resolved = collections.collections.firstOrNull { it.id == def.id } ?: def
        return collections.resolveCards(resolved, cards.toList())
    }

    // ---------------------------------------------------------------
    // Review lifecycle
    // ---------------------------------------------------------------
    fun startReview(
        query: String? = null,
        collection: CollectionDef? = null,
        settings: ReviewSettings = reviewSettings
    ) {
        val now = Clock.System.now()
        val pool = when {
            collection != null -> collections.resolveCards(collection, cards.toList())
            !query.isNullOrBlank() -> cards.filter { SearchEngine.matches(it, query) }
            else -> cards.toList()
        }
        val newCards = if (settings.includeNew) pool.filter { it.status == SrsStatus.New }.take(settings.newLimit) else emptyList()
        val due = pool.filter { it.status != SrsStatus.New && it.dueAt != null && it.dueAt <= now }
            .take(settings.reviewLimit)
        var queue = (newCards + due).distinctBy { it.id }
        if (settings.shuffle) queue = queue.shuffled(Random(7))

        if (queue.isEmpty()) {
            toastHost.show("No cards match the current review queue", kind = ToastKind.Info)
            return
        }

        reviewSettings = settings
        val session = ReviewSession(name = "Review", createdAt = now)
        session.enqueue(queue, shuffle = false)
        reviewSession = session
        sessionResults.clear()
        sessionStartedAt = now
        answerRevealed = false
        currentView = WorkspaceView.Review
    }

    fun rateCurrent(rating: ReviewRating) {
        val session = reviewSession ?: return
        if (session.isFinished) return
        val entry = session.current() ?: return
        val card = entry.card
        val beforeStatus = card.status
        val beforeInterval = card.intervalDays

        val updated = session.answer(rating)
        sessionResults.add(ReviewResult(card.id, rating, updated.status, updated.intervalDays))

        val idx = cards.indexOfFirst { it.id == card.id }
        if (idx >= 0) cards[idx] = updated

        reviewLog.add(
            ReviewLogEntry(
                cardId = card.id,
                reviewedAt = updated.lastReviewedAt ?: Clock.System.now(),
                rating = rating,
                intervalBefore = beforeInterval,
                intervalAfter = updated.intervalDays,
                wasNew = beforeStatus == SrsStatus.New
            )
        )
        activityLog.record(ActivityCategory.Review, "Reviewed ${card.character} — ${rating.displayName}")

        answerRevealed = false
        if (session.isFinished) endReview()
    }

    fun buryCurrent() {
        val session = reviewSession ?: return
        session.bury()
        activityLog.record(ActivityCategory.Review, "Buried a card")
        if (session.isFinished) endReview()
    }

    fun suspendCurrent() {
        val session = reviewSession ?: return
        val card = session.current()?.card
        val updated = session.suspend()
        if (card != null) {
            val idx = cards.indexOfFirst { it.id == card.id }
            if (idx >= 0) cards[idx] = updated
            activityLog.record(ActivityCategory.Review, "Suspended ${card.character}")
        }
        if (session.isFinished) endReview()
    }

    fun skipCurrent() {
        val session = reviewSession ?: return
        val id = session.current()?.card?.id
        session.skip()
        if (id != null && session.current()?.card?.id == id) {
            session.removeCard(id)
        }
        if (session.isFinished) endReview()
    }

    fun undoLast() {
        val session = reviewSession ?: return
        if (sessionResults.isNotEmpty()) sessionResults.removeAt(sessionResults.lastIndex)
        session.undo()
        activityLog.record(ActivityCategory.Undo, "Undid last review action")
        answerRevealed = false
    }

    fun retryCurrent() {
        val session = reviewSession ?: return
        val id = session.current()?.card?.id
        session.retry()
        if (id != null && session.current()?.card?.id == id) {
            session.removeCard(id)
        }
        if (session.isFinished) endReview()
    }

    fun forgetCurrent() {
        val session = reviewSession ?: return
        val entry = session.current()
        val updated = session.forget()
        if (entry != null) {
            val idx = cards.indexOfFirst { it.id == entry.card.id }
            if (idx >= 0) cards[idx] = updated
            session.removeCard(entry.card.id)
            activityLog.record(ActivityCategory.Review, "Forgot ${entry.card.character}")
        }
        if (session.isFinished) endReview()
    }

    fun rescheduleCurrent(days: Int) {
        val session = reviewSession ?: return
        val entry = session.current()
        session.setCustomInterval(days.toDouble())
        if (entry != null) {
            val idx = cards.indexOfFirst { it.id == entry.card.id }
            if (idx >= 0) {
                cards[idx] = cards[idx].copy(
                    dueAt = Clock.System.now().minus(-days.toLong(), DateTimeUnit.DAY, TimeZone.currentSystemDefault()),
                    intervalDays = days.toDouble()
                )
            }
        }
        answerRevealed = false
        if (session.isFinished) endReview()
    }

    fun endReview() {
        val elapsed = Clock.System.now() - sessionStartedAt
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val correct = sessionResults.count { it.rating != ReviewRating.Again }
        val wrong = sessionResults.count { it.rating == ReviewRating.Again }
        val newCount = sessionResults.count { it.newStatus == SrsStatus.Learning }
        val reviewCount = sessionResults.count { it.newStatus == SrsStatus.Review }

        val daySummary = StudyDaySummary(
            day = today,
            newCount = newCount,
            reviewCount = reviewCount,
            correctCount = correct,
            wrongCount = wrong,
            timeSpent = elapsed
        )
        val existingIdx = summaries.indexOfFirst { it.day == today }
        if (existingIdx >= 0) {
            val s = summaries[existingIdx]
            summaries[existingIdx] = s.copy(
                newCount = s.newCount + daySummary.newCount,
                reviewCount = s.reviewCount + daySummary.reviewCount,
                correctCount = s.correctCount + daySummary.correctCount,
                wrongCount = s.wrongCount + daySummary.wrongCount,
                timeSpent = s.timeSpent + daySummary.timeSpent
            )
        } else {
            summaries.add(daySummary)
        }

        val rated = sessionResults.size
        lastSessionStats = ReviewSessionStats(
            total = rated,
            again = sessionResults.count { it.rating == ReviewRating.Again },
            hard = sessionResults.count { it.rating == ReviewRating.Hard },
            good = sessionResults.count { it.rating == ReviewRating.Good },
            easy = sessionResults.count { it.rating == ReviewRating.Easy },
            accuracy = if (rated == 0) 1f else sessionResults.count { it.rating != ReviewRating.Again }.toFloat() / rated
        )
        reviewSession = null
        answerRevealed = false
        toastHost.show("Session complete — $rated cards rated", kind = ToastKind.Success)
        currentView = WorkspaceView.Dashboard
    }

    // ---------------------------------------------------------------
    // Counts (dashboard / badge helpers)
    // ---------------------------------------------------------------
    fun countByStatus(status: SrsStatus): Int = cards.count { it.status == status }

    fun dueCount(now: Instant = Clock.System.now()): Int =
        cards.count { (it.status == SrsStatus.Review || it.status == SrsStatus.Learning) && it.dueAt != null && it.dueAt <= now }

    fun newCount(): Int = cards.count { it.status == SrsStatus.New }
    fun suspendedCount(): Int = cards.count { it.status == SrsStatus.Suspended }
    fun masteredCount(): Int = cards.count { it.status == SrsStatus.Review && it.intervalDays >= 21 }

    fun totalStudyTime(): Duration =
        summaries.fold(Duration.ZERO) { acc, summary -> acc + summary.timeSpent }

    // ---------------------------------------------------------------
    // Card creation / mutation (used by mining, dictionary, browser,
    // media, OCR and the local API).
    // ---------------------------------------------------------------
    fun addCard(card: DesktopCard): DesktopCard {
        val existingIdx = cards.indexOfFirst { it.id == card.id }
        if (existingIdx >= 0) {
            cards[existingIdx] = card
        } else {
            cards.add(0, card)
        }
        activityLog.record(ActivityCategory.Study, "Added card \"${card.character}\"")
        return card
    }

    fun deleteCard(id: String) {
        val card = cards.firstOrNull { it.id == id }
        cards.removeAll { it.id == id }
        if (selectedCard?.id == id) selectedCard = null
        selectedCardIds.remove(id)
        activityLog.record(ActivityCategory.Study, "Deleted card \"${card?.character ?: id}\"")
    }

    fun updateCard(card: DesktopCard) {
        val idx = cards.indexOfFirst { it.id == card.id }
        if (idx >= 0) cards[idx] = card
    }

    // ---------------------------------------------------------------
    // Theme setup
    // ---------------------------------------------------------------
    fun applyTheme(themeId: String) {
        activeThemeId = themeId
        themeStudioDirty = true
    }

    fun exportThemeJson(): String {
        val theme = ThemePresets.all.firstOrNull { it.id == activeThemeId } ?: ThemePresets.default
        return ThemeSerializer.export(theme)
    }

    fun importThemeJson(json: String): Boolean = try {
        val theme = ThemeSerializer.validate(json).getOrThrow()
        activeThemeId = theme.id
        themeStudioDirty = true
        toastHost.show("Theme '${theme.name}' imported", kind = ToastKind.Success)
        true
    } catch (e: Exception) {
        toastHost.show("Invalid theme JSON: ${e.message}", kind = ToastKind.Error)
        false
    }

    // ---------------------------------------------------------------
    // Stress dataset (perf demo)
    // ---------------------------------------------------------------
    fun loadStressDataset(count: Int) {
        val before = cards.size
        val stress = buildStressDataset(count)
        cards.addAll(stress)
        toastHost.show("Added $count synthetic cards ($before → ${cards.size})", kind = ToastKind.Success)
        activityLog.record(ActivityCategory.Study, "Loaded stress dataset ($count cards)")
    }

    // ---------------------------------------------------------------
    // Demo seeding
    // ---------------------------------------------------------------
    fun seedDemoData() {
        if (cards.isNotEmpty()) return
        val demo = buildDemoCards()
        cards.addAll(demo)
        seedSummaries()
        seedCollections()
        seedActivity()
        seedDictionary()
    }

    /** Install the bundled kanji dictionary on first run. */
    private fun seedDictionary() {
        if (dictionary.isInstalled(DictionaryService.SEED_DICTIONARY_ID)) return
        dictionary.install(
            DictionaryService.seedMeta(),
            DictionaryService.seedEntries(),
            state = this
        )
    }

    private fun seedSummaries() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val now = Clock.System.now()
        for (offset in 180 downTo 0) {
            val date = today.minus(offset, DateTimeUnit.DAY)
            val dayOfWeek = date.dayOfWeek.ordinal
            val newCount = if (dayOfWeek == 6) 0 else (3 + (offset * 7) % 14)
            val reviewCount = 10 + (offset * 13) % 40
            val wrong = reviewCount / 7
            val correct = reviewCount - wrong
            val timeMs = (newCount * 45L + reviewCount * 30L) * 1000L
            summaries.add(
                StudyDaySummary(
                    day = date.toString(),
                    newCount = newCount,
                    reviewCount = reviewCount,
                    correctCount = correct,
                    wrongCount = wrong,
                    timeSpent = timeMs.milliseconds
                )
            )
        }
        demoKanji().forEachIndexed { index, seed ->
            val day = (index * 5) % 5
            val ts = now.minus(day.toLong(), DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                .minus(index * 23L, DateTimeUnit.MINUTE)
            val rated = index % 9
            val status = if (rated < 3) SrsStatus.Learning else SrsStatus.Review
            reviewLog.add(
                ReviewLogEntry(
                    cardId = seed.cardId,
                    reviewedAt = ts,
                    rating = ReviewRating.entries[rated % 4],
                    intervalBefore = 0.0,
                    intervalAfter = (index % 20).toDouble(),
                    wasNew = true,
                    source = "seed"
                )
            )
            if (index % 3 == 0) {
                activityLog.record(ActivityCategory.Review, "Reviewed ${seed.character} as ${ReviewRating.entries[rated % 4].displayName}")
            }
        }
    }

    private fun seedCollections() {
        val demoIds = cards.take(14).map { it.id }
        val first = collections.create("First Week", "The very first kanji you learned", CollectionKind.Manual)
        collections.update(first.copy(cardIds = demoIds))
        val favoriteIds = cards.filter { it.favorite }.map { it.id }
        val fav = collections.create("Favorites", "Kanji you starred", CollectionKind.Manual)
        collections.update(fav.copy(cardIds = favoriteIds))
        collections.togglePinned(collections.collections.first { it.id == "smart-due" }.id)
    }

    private fun seedActivity() {
        activityLog.record(ActivityCategory.System, "Kaiteyo Desktop started")
        activityLog.record(ActivityCategory.Study, "Demo dataset loaded (${cards.size} cards)")
        activityLog.record(ActivityCategory.Theme, "Theme set to ${ThemePresets.default.name}")
        activityLog.record(ActivityCategory.Import, "Imported demo saved filters")
    }
}

/** Minimal demo-seed mirror so [seedSummaries] can tag its log entries. */
private data class DemoSeedRef(val character: String, val cardId: String)

private fun demoKanji(): List<DemoSeedRef> =
    ua.syt0r.kanji.desktop.data.demoKanji.map { DemoSeedRef(it.character, "kanji-${it.character.hashCode().toString(16)}") }

/** Result of a single rating inside a live review session. */
data class ReviewResult(
    val cardId: String,
    val rating: ReviewRating,
    val newStatus: SrsStatus,
    val newInterval: Double
)
