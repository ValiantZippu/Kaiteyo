package ua.syt0r.kanji.desktop.appstate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
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
import ua.syt0r.kanji.desktop.engine.activity.ActivityTracker
import ua.syt0r.kanji.desktop.engine.activity.SignalContext
import ua.syt0r.kanji.desktop.engine.sync.CloudSyncCoordinator
import ua.syt0r.kanji.desktop.engine.sync.SyncEngine
import ua.syt0r.kanji.desktop.engine.theming.ThemeManager
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryService
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.mining.MiningEngine
import ua.syt0r.kanji.desktop.engine.mining.MiningStatisticsStore
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.browser.BrowserEngine
import ua.syt0r.kanji.desktop.engine.ocr.OcrEngine
import ua.syt0r.kanji.desktop.engine.api.LocalApiServer
import ua.syt0r.kanji.desktop.model.CollectionDef
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewLogEntry
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.StudyDaySummary
import ua.syt0r.kanji.desktop.model.StudyMode
import ua.syt0r.kanji.desktop.model.ToastKind
import ua.syt0r.kanji.desktop.engine.library.LibraryStore
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
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Duration

/** A single view of the workspace. */
@Serializable
enum class WorkspaceView(val label: String, val icon: String) {
    Dashboard("Dashboard", "I"),
    Browser("Browser", "B"),
    Reading("Reading", "R"),
    Curriculum("Curriculum", "C"),
    Graph("Knowledge Graph", "K"),
    Review("Review", "R"),
    Writing("Writing", "W"),
    Grammar("Grammar", "G"),
    Library("Library", "L"),
    Collections("Collections", "C"),
    Tags("Tags & Flags", "T"),
    Statistics("Statistics", "S"),
    Mistakes("Mistakes", "!"),
    History("Activity Log", "H"),
    Transfer("Import / Export", "E"),
    Sync("Sync", "Y"),
    Shortcuts("Shortcuts", "K"),
    Plugins("Plugins", "P"),
    ThemeStudio("Theme Studio", "M"),
    Settings("Settings", "G"),
    Account("Account", "A"),
    Contributions("About", "B"),
    Dictionary("Dictionary", "D"),
    Mining("Mining", "M"),
    Media("Media", "V"),
    Exams("Exams", "E"),
    LearningBrowser("Web Browser", "W"),
    Ocr("OCR", "O"),
    Integrations("Integrations", "A"),
    // The game world — an optional second space inside Kaiteyo.
    Game("Game", "G")
}

/** Type of browser display. */
@Serializable
enum class BrowserViewMode { Grid, List, Details }

/** Edge of the window the navigation dock attaches to. */
enum class NavPosition(val label: String) {
    Left("Left"),
    Right("Right"),
    Top("Top"),
    Bottom("Bottom")
}

/**
 * The two navigation modes — Floating and Sidebar. There is exactly one dock
 * and no free sizing: the sidebar's layout is chosen through [NavExpansion]
 * (Expanded shows icons + labels, Compact is an icon-only rail), and Floating
 * removes the dock entirely in favor of the launcher bubble.
 */
enum class NavLayout(val label: String) {
    Sidebar("Sidebar"),
    Floating("Floating")
}

/** The two predefined sidebar layouts — no free resizing. */
enum class NavExpansion(val label: String) {
    Expanded("Expanded"),
    Compact("Compact icons")
}

/**
 * Snap targets for the floating launcher — 12 points, three per screen edge.
 * Corner points appear twice (once per adjacent edge) at identical positions.
 */
enum class LauncherSnapPoint(val label: String) {
    TopLeft("Top left"),
    TopCenter("Top center"),
    TopRight("Top right"),
    BottomLeft("Bottom left"),
    BottomCenter("Bottom center"),
    BottomRight("Bottom right"),
    LeftTop("Left top"),
    LeftCenter("Left center"),
    LeftBottom("Left bottom"),
    RightTop("Right top"),
    RightCenter("Right center"),
    RightBottom("Right bottom");

    companion object {
        fun fromName(name: String?): LauncherSnapPoint =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: BottomRight
    }
}

enum class LauncherSize(val label: String) {
    Small("Small"),
    Medium("Medium"),
    Large("Large");

    companion object {
        fun fromName(name: String?): LauncherSize =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Medium
    }
}

enum class LauncherIconSize(val label: String) {
    Small("Small"),
    Medium("Medium"),
    Large("Large");

    companion object {
        fun fromName(name: String?): LauncherIconSize =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Medium
    }
}

/** Icon scale for the compact (tab bar) navigation. */
enum class CompactIconSize(val label: String) {
    Small("Small"),
    Medium("Medium"),
    Large("Large");

    companion object {
        fun fromName(name: String?): CompactIconSize =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Medium
    }
}

/** Predefined expanded-dock widths — the dock is never free-resizable. */
enum class SidebarWidth(val label: String, val dp: androidx.compose.ui.unit.Dp) {
    Narrow("Narrow", 200.dp),
    Standard("Standard", 236.dp),
    Wide("Wide", 280.dp);

    companion object {
        fun fromName(name: String?): SidebarWidth =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Standard
    }
}

/** Icon scale for the dock (sidebar / bar / switcher) icons. */
enum class NavIconSize(val label: String) {
    Small("Small"),
    Medium("Medium"),
    Large("Large");

    companion object {
        fun fromName(name: String?): NavIconSize =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Medium
    }
}

/** Label visibility inside the expanded dock. */
enum class NavLabelMode(val label: String) {
    Always("Always"),
    OnHover("On hover"),
    Hidden("Hidden");

    companion object {
        fun fromName(name: String?): NavLabelMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Always
    }
}

/** Vertical rhythm between dock items (compact icon spacing). */
enum class NavSpacing(val label: String) {
    Tight("Tight"),
    Comfortable("Comfortable"),
    Spacious("Spacious");

    companion object {
        fun fromName(name: String?): NavSpacing =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Comfortable
    }
}

/**
 * Central in-memory facade for the whole desktop suite.
 * Holds live state (cards, review log, navigation, selection)
 * and owns the engine singletons. Created once per window.
 */
class AppState(
    // Persisted to ~/.kaiteyo/settings.json so media keys, integration config
    // and the local API token survive restarts (SettingsEngine persists only
    // when given a file).
    val settings: SettingsEngine = SettingsEngine(
        persistFile = java.io.File(System.getProperty("user.home"), ".kaiteyo/settings.json")
    ),
    val shortcutRegistry: ShortcutRegistry = ShortcutRegistry(),
    val shortcutDispatcher: ShortcutDispatcher = ShortcutDispatcher(shortcutRegistry),
    val filterStore: SavedFilterStore = SavedFilterStore(),
    val collections: CollectionStore = CollectionStore(),
    val library: LibraryStore = LibraryStore(),
    val activityLog: ActivityLog = ActivityLog(),
    val pluginRegistry: PluginRegistry = PluginRegistry(),
    val syncEngine: SyncEngine = SyncEngine(),
    val toastHost: DsToastHost = DsToastHost(),
    /** Account control center: identity, providers, devices, sessions, storage. */
    val account: ua.syt0r.kanji.desktop.engine.account.AccountEngine = ua.syt0r.kanji.desktop.engine.account.AccountEngine(
        dataDir = ua.syt0r.kanji.desktop.engine.account.AccountEngine.accountDataDir(),
        settings = settings,
        activityLog = activityLog
    )
) {

    /**
     * Cloud sync: real GitHub-gist transport bridged to the AccountEngine
     * connection, with auto-sync scheduling. Lazy so it can reference the
     * fully-constructed state (including [account]) on first access.
     */
    val cloudSync: CloudSyncCoordinator by lazy {
        CloudSyncCoordinator(state = this, account = account)
    }

    /**
     * Engagement tracker: study time is measured from real interactions
     * (clicks, keys, grading, writing strokes), never from "app open" time.
     * Also drives AFK detection and the AFK rain overlay.
     */
    val activity = ActivityTracker(settings)

    /**
     * Append-only domain event log (EVENT_CATALOG): the single source of
     * truth for derived metrics. JSON snapshot in the user data dir.
     */
    val eventLog: ua.syt0r.kanji.desktop.engine.events.EventLog by lazy {
        ua.syt0r.kanji.desktop.engine.events.EventLog(
            java.io.File(System.getProperty("user.home"), ".kaiteyo/event_log.json")
        )
    }

    /**
     * Media node family — Series → Episode → Scene → SubtitleLine, rebuilt
     * lazily from real mined-card provenance so the graph surfaces (media
     * exposure, sentence search) reflect actual watch/mine history.
     */
    val mediaNodeGraph: ua.syt0r.kanji.desktop.engine.media.MediaNodeGraph by lazy {
        ua.syt0r.kanji.desktop.engine.media.MediaNodeGraph().apply {
            media.miningEvents.forEach { addMiningEvent(it) }
            media.bookmarks.forEach { addBookmark(it) }
        }
    }


    /** Reduced motion is the OR of the global preference and the nav-specific one. */
    private fun refreshReducedMotion() {
        navReducedMotion = settings.getBool("appearance.reduced-motion") || settings.getBool("navigation.reduced-motion")
    }

    // ---------------------------------------------------------------
    // Learning workspace engines (dictionary, mining, media, browser,
    // OCR and the local integration API).
    // ---------------------------------------------------------------
    val dictionary = DictionaryService(DictionaryRepository(dictionaryDir()))
    /**
     * Writing evaluation facade — routes through the canonical KanjiVG stack
     * when a licensed KanjiVG dataset directory is present, otherwise the
     * built-in common-kanji dataset. Both are real; the source is surfaced
     * in the UI and never faked.
     */
    val writingEvaluator by lazy {
        ua.syt0r.kanji.desktop.engine.stroke_evaluator.WritingEvaluator(
            repository = dictionary.repository
        )
    }
    /** Unified learning ecosystem: notes, cards, review events, exams, mistakes, statistics. */
    val learning = ua.syt0r.kanji.desktop.engine.learning.LearningEngine()
    val miningIntegration = ua.syt0r.kanji.desktop.engine.mining.MiningIntegrationManager(settings)
    val mining = MiningEngine(this)
    /** All-source mining counters (per day + per source) for Statistics/Dashboard. */
    val miningStatistics = MiningStatisticsStore()
    val media = MediaEngine(this)
    val browserEngine = BrowserEngine()
    val ocr = OcrEngine()
    /**
     * Native reading workspace: local documents (TXT/Markdown/HTML) with
     * dictionary-backed word lookup, mining, bookmarks, highlights and
     * reading history. Lazy so it can reference the dictionary repository.
     */
    val reading by lazy {
        ua.syt0r.kanji.desktop.engine.reading.ReadingEngine(
            dictionaryRepository = dictionary.repository
        )
    }
    /** Persists the reading library + history to ~/.kaiteyo/reading/. */
    val readingLibrary by lazy {
        ua.syt0r.kanji.desktop.engine.reading.ReadingLibrary(reading)
    }
    /**
     * Canonical language database (jdata) — the read-only knowledge graph
     * source. Built once from the installed dictionaries; null when no
     * dictionary data is available.
     */
    val languageDatabase by lazy {
        runCatching {
            val data = ua.syt0r.kanji.desktop.engine.jdata.integration.PlatformBuilder.fromRepository(
                dictionary.repository,
                geometry = ua.syt0r.kanji.desktop.engine.jdata.engine.NoStrokeGeometryProvider
            )
            if (data.kanji.isEmpty() && data.vocab.isEmpty()) null
            else ua.syt0r.kanji.desktop.engine.jdata.api.LanguageDatabase.open(data)
        }.getOrNull()
    }
    /** Knowledge graph: nodes over the language database + live card pool. */
    val knowledgeGraph by lazy {
        ua.syt0r.kanji.desktop.engine.graph.KnowledgeGraph(
            database = languageDatabase,
            repository = dictionary.repository,
            cards = cards
        )
    }
    /** Structured curriculum measured against real study data. */
    val curriculum by lazy {
        ua.syt0r.kanji.desktop.engine.curriculum.CurriculumEngine(
            dataSource = ua.syt0r.kanji.desktop.engine.curriculum.AppStateCurriculumDataSource(this)
        )
    }
    val localApi = LocalApiServer(mining, media, settings)
    /** Anki → Kaiteyo import bridge (deck/note/card/tag pull with dedupe). */
    val ankiImporter = ua.syt0r.kanji.desktop.engine.transfer.AnkiImporter(this, miningIntegration.anki)

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
    // ---------------------------------------------------------------
    // Workspace tabs — browser-style multi-instance navigation.
    // Every view lives in a tab with its own per-instance state; the
    // whole set persists across restarts (SettingsEngine workspace.tabs).
    // ---------------------------------------------------------------
    /** Tabs are optional — disable for the classic single-view shell. */
    var tabsEnabled by mutableStateOf(settings.getBool("workspace.tabs-enabled", true))
    /** Open workspace sessions, in display order. */
    val tabs = mutableStateListOf<WorkspaceTab>()
    /** The active session — the tab whose view is on screen. */
    var activeTabId by mutableStateOf<String?>(null)

    /** Resolved active tab, or null when tabs are disabled/empty. */
    val activeTab: WorkspaceTab? get() = tabs.firstOrNull { it.id == activeTabId }

    /**
     * The view shown in the active tab. Every navigation assignment routes
     * through the tab system; with tabs disabled it behaves like the legacy
     * single-view shell.
     */
    var currentView: WorkspaceView
        get() = activeTab?.view ?: _fallbackView
        set(value) {
            if (tabsEnabled && tabs.isNotEmpty()) {
                setActiveTabView(value)
            } else {
                _fallbackView = value
            }
        }
    /** Single-view fallback used while tabs are disabled (or before load). */
    private var _fallbackView by mutableStateOf(WorkspaceView.Dashboard)

    /** Animated-content target: which tab + view is on screen right now. */
    val contentTarget: WorkspaceContentTarget
        get() {
            val tab = activeTab
            return if (tabsEnabled && tab != null) {
                WorkspaceContentTarget(tab.id, tab.view)
            } else {
                WorkspaceContentTarget(null, _fallbackView)
            }
        }

    val openPanels = mutableStateListOf<OpenPanel>()
    var navPosition by mutableStateOf(
        NavPosition.entries.firstOrNull { it.name.lowercase() == settings.getString("navigation.position", "left") }
            ?: NavPosition.Left
    )
    var navLayout by mutableStateOf(loadNavLayout())
    /** Sidebar sub-layout: Expanded (icons + labels) or Compact (icons only). */
    var navExpansion by mutableStateOf(
        NavExpansion.entries.firstOrNull { it.name.equals(settings.getString("navigation.expansion", "expanded"), ignoreCase = true) }
            ?: NavExpansion.Expanded
    )

    /** Compact-window navigation edge — restricted to Top or Bottom by design. */
    var compactNavPosition by mutableStateOf(
        NavPosition.entries
            .firstOrNull { it.name.equals(settings.getString("navigation.compact-position", "bottom"), ignoreCase = true) && it != NavPosition.Left && it != NavPosition.Right }
            ?: NavPosition.Bottom
    )

    // ---------------------------------------------------------------
    // Navigation animation & accessibility settings (live mirrors)
    // ---------------------------------------------------------------
    var navigationAnimations by mutableStateOf(settings.getBool("navigation.animations"))
    var navigationAnimationSpeed by mutableStateOf(settings.getFloat("navigation.animation-speed", 1f))
    var navigationLargerIcons by mutableStateOf(settings.getBool("navigation.larger-icons"))
    var navigationLargerHitbox by mutableStateOf(settings.getBool("navigation.larger-hitbox"))
    var navHighContrast by mutableStateOf(settings.getBool("navigation.high-contrast"))
    var compactIconSize by mutableStateOf(CompactIconSize.fromName(settings.getString("navigation.compact-icon-size", "medium")))
    var navigationTooltipDelayMs by mutableStateOf(settings.getInt("navigation.tooltip-delay", 450).coerceIn(0, 3000))
    var sidebarWidth by mutableStateOf(SidebarWidth.fromName(settings.getString("navigation.sidebar-width", "standard")))
    var navIconSize by mutableStateOf(NavIconSize.fromName(settings.getString("navigation.icon-size", "medium")))
    var navLabelMode by mutableStateOf(NavLabelMode.fromName(settings.getString("navigation.label-mode", "always")))
    var navSpacing by mutableStateOf(NavSpacing.fromName(settings.getString("navigation.compact-spacing", "comfortable")))
    /** Reduced motion is the OR of the global preference and the nav-specific one. */
    var navReducedMotion by mutableStateOf(
        settings.getBool("appearance.reduced-motion") || settings.getBool("navigation.reduced-motion")
    )

    // ---------------------------------------------------------------
    // Floating launcher settings (live mirrors) + remembered position
    // ---------------------------------------------------------------
    /** True only while Floating mode is active — the launcher is the mode, not an overlay. */
    val launcherEnabled: Boolean get() = navLayout == NavLayout.Floating
    var launcherAutoFade by mutableStateOf(settings.getBool("launcher.auto-fade"))
    var launcherFadeDelayMs by mutableStateOf(settings.getInt("launcher.fade-delay", 6) * 1000)
    var launcherFadeOpacity by mutableStateOf(settings.getFloat("launcher.fade-opacity", 0.25f))
    var launcherFadeDurationMs by mutableStateOf(settings.getInt("launcher.fade-duration", 450))
    var launcherSnapEnabled by mutableStateOf(settings.getBool("launcher.snap"))
    var launcherSnapSensitivity by mutableStateOf(settings.getFloat("launcher.snap-sensitivity", 1f))
    var launcherAnimationSpeed by mutableStateOf(settings.getFloat("launcher.animation-speed", 1f))
    var launcherSnapPoint by mutableStateOf(LauncherSnapPoint.fromName(settings.getString("launcher.snap-point", "bottom-right")))
    var launcherSize by mutableStateOf(LauncherSize.fromName(settings.getString("launcher.size", "medium")))
    var launcherIconSize by mutableStateOf(LauncherIconSize.fromName(settings.getString("launcher.icon-size", "medium")))
    /** Remembered launcher position as fractions (0..1) of the window size. */
    var launcherPosX by mutableStateOf(settings.getFloat("launcher.pos-x", 0.88f))
    var launcherPosY by mutableStateOf(settings.getFloat("launcher.pos-y", 0.86f))
    /** Phone keeps its own remembered position so it never fights the tab bar. */
    var launcherPosXPhone by mutableStateOf(settings.getFloat("launcher.pos-x-phone", 0.88f))
    var launcherPosYPhone by mutableStateOf(settings.getFloat("launcher.pos-y-phone", 0.78f))
init {
        // Shut down owned media processes (VLC/mpv children, audio clips)
        // when the application exits — never leave orphan players behind.
        Runtime.getRuntime().addShutdownHook(Thread {
            runCatching { media.shutdown() }
        })
        loadWorkspacePanels()
        loadOnboardingFlag()
        // Reading library + history survive restarts.
        readingLibrary.load()
        // The card pool is the user's study data: restore it first so a
        // relaunch continues exactly where the previous session stopped.
        // On a true first run there is nothing to restore — the suite starts
        // empty; nothing demo is ever seeded into the user library.
        library.loadCards()?.let { cards.addAll(it) }
        // The Kana syllabary is first-class content: the folder, the premade
        // decks and every kana card are seeded idempotently on every launch
        // (new installs and upgrades alike — nothing existing is touched).
        ua.syt0r.kanji.desktop.engine.kana.seedKanaInto(this)
        // Bundled reference dictionary (offline lookup) — installed once, real
        // curated data, never study content.
        seedDictionary()
        loadWorkspaceTabs()
        // Bridge the legacy card pool into the unified learning model so new
        // systems (exams, statistics, mistakes) see the same real data.
        learning.syncFromLegacy(cards.toList())
        // Study statistics (review log + daily summaries) persist to disk so
        // the dashboards survive restarts — see LibraryStore.statistics.
        library.loadStatistics()?.let { snap ->
            reviewLog.addAll(snap.reviewLog)
            summaries.addAll(snap.summaries)
        }
        // Force the cloud sync coordinator to start (auto-sync scheduling,
        // sync-on-start) even when the Sync view is never opened.
        cloudSync
        pluginRegistry.restoreSnapshot(settings.getString("plugins.installed"))
        // Reconcile the persisted floating toggle with the stored mode so both stay in sync.
        if (settings.getBool("launcher.enabled") && navLayout != NavLayout.Floating) {
            navLayout = NavLayout.Floating
        }
        // Migrate a legacy "compact" dock into the sidebar expansion.
        if (navExpansion == NavExpansion.Expanded && "compact".equals(settings.getString("navigation.layout"), ignoreCase = true)) {
            navExpansion = NavExpansion.Compact
        }
        settings.observe { key, _, newValue ->
            when (key) {
                "navigation.position" -> navPosition = NavPosition.entries.firstOrNull { it.name.equals(newValue, ignoreCase = true) } ?: NavPosition.Left
                "navigation.layout" -> navLayout = navLayoutFromStored(newValue) ?: NavLayout.Sidebar
                "navigation.expansion" -> navExpansion = NavExpansion.entries
                    .firstOrNull { it.name.equals(newValue, ignoreCase = true) } ?: NavExpansion.Expanded
                "navigation.compact-position" -> compactNavPosition = NavPosition.entries
                    .firstOrNull { it.name.equals(newValue, ignoreCase = true) && it != NavPosition.Left && it != NavPosition.Right }
                    ?: NavPosition.Bottom
                "navigation.animations" -> navigationAnimations = newValue.toBooleanStrictOrNull() ?: true
                "navigation.animation-speed" -> navigationAnimationSpeed = (newValue.toFloatOrNull() ?: 1f).coerceIn(0.25f, 3f)
                "navigation.reduced-motion" -> refreshReducedMotion()
                "navigation.larger-icons" -> navigationLargerIcons = newValue.toBooleanStrictOrNull() ?: false
                "navigation.larger-hitbox" -> navigationLargerHitbox = newValue.toBooleanStrictOrNull() ?: false
                "navigation.high-contrast" -> navHighContrast = newValue.toBooleanStrictOrNull() ?: false
                "navigation.compact-icon-size" -> compactIconSize = CompactIconSize.fromName(newValue)
                "navigation.tooltip-delay" -> navigationTooltipDelayMs = (newValue.toIntOrNull() ?: 450).coerceIn(0, 3000)
                "navigation.sidebar-width" -> sidebarWidth = SidebarWidth.fromName(newValue)
                "navigation.icon-size" -> navIconSize = NavIconSize.fromName(newValue)
                "navigation.label-mode" -> navLabelMode = NavLabelMode.fromName(newValue)
                "navigation.compact-spacing" -> navSpacing = NavSpacing.fromName(newValue)
                "workspace.tabs-enabled" -> {
                    val on = newValue.toBooleanStrictOrNull() ?: true
                    tabsEnabled = on
                    if (on && tabs.isEmpty()) openTab(currentView, activate = true)
                }
                "launcher.enabled" -> {
                    val on = newValue.toBooleanStrictOrNull() ?: false
                    if (on && navLayout != NavLayout.Floating) updateNavLayout(NavLayout.Floating)
                    else if (!on && navLayout == NavLayout.Floating) updateNavLayout(NavLayout.Sidebar)
                }
                "launcher.auto-fade" -> launcherAutoFade = newValue.toBooleanStrictOrNull() ?: true
                "launcher.fade-delay" -> launcherFadeDelayMs = (newValue.toIntOrNull() ?: 6).coerceIn(1, 120) * 1000
                "launcher.fade-opacity" -> launcherFadeOpacity = (newValue.toFloatOrNull() ?: 0.25f).coerceIn(0f, 1f)
                "launcher.fade-duration" -> launcherFadeDurationMs = (newValue.toIntOrNull() ?: 450).coerceIn(50, 3000)
                "launcher.snap" -> launcherSnapEnabled = newValue.toBooleanStrictOrNull() ?: true
                "launcher.snap-sensitivity" -> launcherSnapSensitivity = (newValue.toFloatOrNull() ?: 1f).coerceIn(0.25f, 2f)
                "launcher.animation-speed" -> launcherAnimationSpeed = (newValue.toFloatOrNull() ?: 1f).coerceIn(0.25f, 3f)
                "launcher.snap-point" -> launcherSnapPoint = LauncherSnapPoint.fromName(newValue)
                "launcher.size" -> launcherSize = LauncherSize.fromName(newValue)
                "launcher.icon-size" -> launcherIconSize = LauncherIconSize.fromName(newValue)
                "appearance.reduced-motion" -> refreshReducedMotion()
            }
        }
    }

    /** Persist a new launcher position (fractions of the window size). */
    fun setLauncherPos(x: Float, y: Float, compact: Boolean = false) {
        if (compact) {
            launcherPosXPhone = x.coerceIn(0f, 1f)
            launcherPosYPhone = y.coerceIn(0f, 1f)
            settings.set("launcher.pos-x-phone", launcherPosXPhone)
            settings.set("launcher.pos-y-phone", launcherPosYPhone)
        } else {
            launcherPosX = x.coerceIn(0f, 1f)
            launcherPosY = y.coerceIn(0f, 1f)
            settings.set("launcher.pos-x", launcherPosX)
            settings.set("launcher.pos-y", launcherPosY)
        }
    }

    /**
     * Expanded dock width for the current window. Predefined sizes only —
     * capped on narrower windows so a tablet never loses the content area.
     */
    fun effectiveExpandedWidth(windowWidth: Float): androidx.compose.ui.unit.Dp {
        val cap = (windowWidth * 0.34f).coerceAtLeast(170f)
        return androidx.compose.ui.unit.Dp(minOf(sidebarWidth.dp.value, cap))
    }

    fun updateNavPosition(position: NavPosition) {
        navPosition = position
        settings.set("navigation.position", position.name.lowercase())
        activityLog.record(ActivityCategory.System, "Navigation moved to ${position.label}")
    }

    fun updateNavLayout(layout: NavLayout) {
        navLayout = layout
        settings.set("navigation.layout", layout.name.lowercase())
        // Keep the settings-page floating toggle in sync with the active mode.
        settings.set("launcher.enabled", layout == NavLayout.Floating)
        activityLog.record(ActivityCategory.System, "Navigation mode: ${layout.label}")
    }

    /** Switch the sidebar between Expanded and Compact (persisted). */
    fun updateNavExpansion(expansion: NavExpansion) {
        navExpansion = expansion
        settings.set("navigation.expansion", expansion.name.lowercase())
    }

    /** Remember the launcher's active snap point (persisted). */
    fun updateLauncherSnapPoint(snap: LauncherSnapPoint) {
        launcherSnapPoint = snap
        settings.set("launcher.snap-point", snap.name.lowercase())
    }

    /** Compact-window edge — coerced to Top or Bottom (desktop edges never leak in). */
    fun updateCompactNavPosition(position: NavPosition) {
        val safe = if (position == NavPosition.Top || position == NavPosition.Bottom) position else NavPosition.Bottom
        compactNavPosition = safe
        settings.set("navigation.compact-position", safe.name.lowercase())
    }

    /** Toggle between Sidebar and Floating (bound to Ctrl+Shift+N). */
    fun cycleNavLayout() {
        val next = if (navLayout == NavLayout.Sidebar) NavLayout.Floating else NavLayout.Sidebar
        updateNavLayout(next)
    }

    /**
     * Reads the persisted mode. Honors "Remember previous mode": when
     * enabled the last used mode wins; otherwise the configured default
     * startup mode is used. Legacy "hidden"/"floating" values migrate
     * to Bubble.
     */
    private fun loadNavLayout(): NavLayout {
        if (settings.getBool("navigation.remember-last")) {
            settings.getString("navigation.layout")?.let { stored ->
                navLayoutFromStored(stored)?.let { return it }
            }
        }
        settings.getString("navigation.default-layout")?.let { stored ->
            navLayoutFromStored(stored)?.let { return it }
        }
        return when (settings.getString("navigation.mode", "traditional")?.lowercase()) {
            "floating", "both", "hidden" -> NavLayout.Floating
            else -> NavLayout.Sidebar
        }
    }

    // ---------------------------------------------------------------
    // Browser state
    // ---------------------------------------------------------------
    var browserQuery by mutableStateOf("")
    var browserViewMode by mutableStateOf(BrowserViewMode.Grid)
    var browserShowPreview by mutableStateOf(true)
    var selectedCard by mutableStateOf<DesktopCard?>(null)
    val selectedCardIds = mutableStateListOf<String>()
    /**
     * Deep link into the Library: when set, the Library opens scoped to this
     * collection on next composition (consumed and cleared by LibraryView).
     * Used by Home's collection cards — the Library is the hub, so opening a
     * collection from anywhere lands inside it.
     */
    var pendingCollectionId by mutableStateOf<String?>(null)

    // ---------------------------------------------------------------
    // Workspace tab lifecycle
    // ---------------------------------------------------------------

    /** Index of a tab in the open list, or -1. */
    fun tabIndexOf(id: String?): Int = tabs.indexOfFirst { it.id == id }

    /** Open a fresh session for [view]; activates it by default. */
    fun openTab(view: WorkspaceView, activate: Boolean = true): String {
        val normalized = if (view == WorkspaceView.Browser) WorkspaceView.Library else view
        val tab = WorkspaceTab(id = newTabId(), view = normalized)
        tabs.add(tab)
        if (activate) activateTab(tab.id) else persistTabs()
        return tab.id
    }

    /** Open a copy of the tab with the given id (tab menu / duplicate). */
    fun duplicateTab(id: String) {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx < 0) return
        val copy = tabs[idx].copy(id = newTabId())
        tabs.add(idx + 1, copy)
        activateTab(copy.id)
    }

    /** Make [id] the active tab, snapshotting the outgoing tab's live state. */
    fun activateTab(id: String) {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx < 0) return
        val leaving = activeTab
        if (leaving != null && leaving.id != id) {
            val li = tabs.indexOfFirst { it.id == leaving.id }
            if (li >= 0) tabs[li] = snapshotTabState(leaving)
        }
        activeTabId = id
        restoreTabState(tabs[idx])
        persistTabs()
    }

    fun closeActiveTab() {
        activeTab?.let { closeTab(it.id) }
    }

    /** Close [id]; the right neighbor (or left) becomes active. */
    fun closeTab(id: String) {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx < 0) return
        val wasActive = activeTabId == id
        if (wasActive) {
            tabs[idx] = snapshotTabState(tabs[idx])
        }
        val removed = tabs.removeAt(idx)
        rememberClosedTab(removed)
        if (tabs.isEmpty()) {
            // The workspace never ends up tab-less — a fresh dashboard tab takes over.
            openTab(WorkspaceView.Dashboard, activate = true)
            return
        }
        if (wasActive) {
            activateTab(tabs[minOf(idx, tabs.lastIndex)].id)
        } else {
            persistTabs()
        }
    }

    fun closeOtherTabs(id: String) {
        val keepIdx = tabs.indexOfFirst { it.id == id }
        if (keepIdx < 0) return
        val keep = tabs[keepIdx]
        val keepSnapshot = if (activeTabId == id) snapshotTabState(keep) else keep
        tabs.filterNot { it.id == id }.forEach { rememberClosedTab(it) }
        tabs.clear()
        tabs.add(keepSnapshot)
        activeTabId = id
        restoreTabState(keepSnapshot)
        persistTabs()
    }

    fun closeTabsAfter(id: String) {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx < 0 || idx >= tabs.lastIndex) return
        val keep = tabs[idx]
        val keepSnapshot = if (activeTabId == id) snapshotTabState(keep) else keep
        val closing = tabs.drop(idx + 1)
        closing.forEach { rememberClosedTab(it) }
        val activeWasClosed = closing.any { it.id == activeTabId }
        tabs.removeAll { closing.any { c -> c.id == it.id } }
        if (activeWasClosed) {
            tabs[idx] = keepSnapshot
            activeTabId = id
            restoreTabState(keepSnapshot)
        }
        persistTabs()
    }

    /** Reorder the open tabs (drag on the strip). */
    fun moveTab(from: Int, to: Int) {
        if (from == to || from !in tabs.indices || to !in tabs.indices) return
        val moved = tabs.removeAt(from)
        tabs.add(to, moved)
        persistTabs()
    }

    fun cycleTab(direction: Int) {
        if (tabs.size < 2) return
        val idx = tabIndexOf(activeTabId)
        if (idx < 0) return
        activateTab(tabs[(idx + direction + tabs.size) % tabs.size].id)
    }

    fun jumpToTab(index: Int) {
        tabs.getOrNull(index - 1)?.let { activateTab(it.id) }
    }

    /** Bounded stack of recently-closed tabs for Ctrl+Shift+T. */
    private val recentlyClosedTabs = ArrayDeque<WorkspaceTab>()

    private fun rememberClosedTab(tab: WorkspaceTab) {
        if (recentlyClosedTabs.size >= 10) recentlyClosedTabs.removeFirst()
        recentlyClosedTabs.addLast(tab)
    }

    fun reopenClosedTab() {
        val tab = recentlyClosedTabs.removeLastOrNull() ?: return
        tabs.add(tab)
        activateTab(tab.id)
    }

    private fun newTabId(): String =
        "tab-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(9999)}"

    // ---------------------------------------------------------------
    // Per-tab state snapshotting (browser / library)
    // ---------------------------------------------------------------

    private fun snapshotTabState(tab: WorkspaceTab): WorkspaceTab =
        tab.copy(
            browserQuery = browserQuery,
            browserViewMode = browserViewMode,
            browserShowPreview = browserShowPreview,
            selectedCardId = selectedCard?.id,
            selectedCardIds = selectedCardIds.toList()
        )

    private fun restoreTabState(tab: WorkspaceTab) {
        browserQuery = tab.browserQuery
        browserViewMode = tab.browserViewMode
        browserShowPreview = tab.browserShowPreview
        selectedCard = tab.selectedCardId?.let { id -> cards.firstOrNull { it.id == id } }
        selectedCardIds.clear()
        selectedCardIds.addAll(tab.selectedCardIds)
    }

    /** Navigate the active tab (the [currentView] setter target). */
    private fun setActiveTabView(view: WorkspaceView) {
        val tab = activeTab ?: return
        val idx = tabs.indexOfFirst { it.id == tab.id }
        if (idx < 0) return
        val normalized = if (view == WorkspaceView.Browser) WorkspaceView.Library else view
        tabs[idx] = snapshotTabState(tab).copy(view = normalized, title = normalized.label)
        persistTabs()
    }

    // ---------------------------------------------------------------
    // Tab persistence (~/.kaiteyo/settings.json → workspace.tabs)
    // ---------------------------------------------------------------

    private fun persistTabs() {
        if (!tabsEnabled) return
        val json = Json { encodeDefaults = true }
        settings.set("workspace.tabs", json.encodeToString(WorkspaceTabsPayload(tabs.toList(), activeTabId)))
    }

    private fun loadWorkspaceTabs() {
        if (!settings.getBool("workspace.tabs-enabled", true)) {
            tabsEnabled = false
            return
        }
        val stored = settings.getString("workspace.tabs")
        val decoded = runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString<WorkspaceTabsPayload>(stored)
        }.getOrNull()
        if (stored.isNotBlank() && decoded != null && decoded.tabs.isNotEmpty()) {
            tabs.addAll(decoded.tabs)
            val active = decoded.activeTabId?.takeIf { id -> decoded.tabs.any { it.id == id } }
                ?: decoded.tabs.first().id
            activeTabId = active
            restoreTabState(tabs.first { it.id == active })
            return
        }
        // First run (or a corrupt payload): start on the configured home view.
        openTab(startupView(), activate = true)
    }

    private fun startupView(): WorkspaceView = when (settings.getString("general.startup-view", "dashboard").lowercase()) {
        "browser" -> WorkspaceView.Library
        "review" -> WorkspaceView.Review
        "collections" -> WorkspaceView.Collections
        else -> WorkspaceView.Dashboard
    }

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
    // Unified review source — when a session was started from the unified
    // learning store (StudyEngine queue), grading flows through the unified
    // StudyEngine (full-fidelity events) instead of the legacy pool, while
    // the existing ReviewSession UI keeps working unchanged.
    // ---------------------------------------------------------------
    var unifiedReviewActive by mutableStateOf(false)
    var unifiedReviewQueue by mutableStateOf<List<ua.syt0r.kanji.desktop.engine.learning.StudyQueueItem>>(emptyList())
    var unifiedReviewIndex by mutableStateOf(0)
    var unifiedReviewDeck by mutableStateOf<String?>(null)
    var unifiedSession by mutableStateOf<ua.syt0r.kanji.desktop.engine.learning.StudySessionRecord?>(null)

    // ---------------------------------------------------------------
    // Library study-mode context — when a session was launched from a
    // deck's study mode, ratings update that mode's independent
    // progress instead of the shared card state.
    // ---------------------------------------------------------------
    var libraryActiveDeck by mutableStateOf<String?>(null)
    var libraryActiveMode by mutableStateOf<StudyMode?>(null)

    // ---------------------------------------------------------------
    // Card editor state (opened from browser, review, dashboard)
    // ---------------------------------------------------------------
    var editingCard by mutableStateOf<DesktopCard?>(null)

    // ---------------------------------------------------------------
    // Staged exam draft (palette / shortcut quick-start)
    // ---------------------------------------------------------------
    // A generated ExamDraft waiting to be picked up by the Exams view. Set by
    // the command palette ("Start weekly assessment", "Start mistakes review")
    // and consumed by ExamView on next composition — the view owns session
    // state, so staging keeps the quick-start honest without duplicating it.
    var pendingExamDraft by mutableStateOf<ua.syt0r.kanji.desktop.engine.learning.ExamDraft?>(null)

    // ---------------------------------------------------------------
    // Staged graph node (deep link from dictionary surfaces)
    // ---------------------------------------------------------------
    // An expression waiting to be opened in the Knowledge Graph. Set by the
    // "Explore in graph" actions on dictionary/lookup surfaces and consumed
    // by GraphExplorerView on next composition (Phase 4: traversal reachable
    // from any kanji/word page in the suite).
    var pendingGraphNode by mutableStateOf<String?>(null)

    // ---------------------------------------------------------------
    // Writing practice session state (kanji handwriting drills)
    // ---------------------------------------------------------------
    var writingSession by mutableStateOf<ReviewSession?>(null)
    var writingStartedAt by mutableStateOf(Clock.System.now())
    val writingResults = mutableStateListOf<ReviewResult>()
    var writingRevealed by mutableStateOf(false)

    // ---------------------------------------------------------------
    // Theme studio state
    // ---------------------------------------------------------------
    /** Owns the theme library, active theme and live edits. */
    val themeManager: ThemeManager = ThemeManager()
    /** Live mirror of the manager's active theme id. */
    val activeThemeId: String get() = themeManager.activeThemeId
    var themeStudioDirty by mutableStateOf(false)

    // ---------------------------------------------------------------
    // Onboarding state
    // ---------------------------------------------------------------
    /** True while the first-run wizard is expected on screen. */
    val onboardingCompleted: Boolean get() = settings.getBool("onboarding.completed")

    /** Set true (e.g. from Settings) to re-open the onboarding wizard immediately. */
    var onboardingRequested by mutableStateOf(false)

    // The SettingsEngine is in-memory only, so the one-shot flag is mirrored
    // to ~/.kaiteyo/onboarding.txt to survive restarts.
    private val onboardingFile: java.io.File =
        java.io.File(System.getProperty("user.home"), ".kaiteyo/onboarding.txt")

    private fun loadOnboardingFlag() {
        val persisted = runCatching { onboardingFile.readText().trim() == "1" }.getOrDefault(false)
        if (persisted) settings.setBool("onboarding.completed", true)
    }

    private fun persistOnboardingFlag() {
        runCatching {
            onboardingFile.parentFile?.mkdirs()
            onboardingFile.writeText(if (settings.getBool("onboarding.completed")) "1" else "0")
        }
    }

    /** Mark the wizard as finished so it never shows again until re-requested. */
    fun completeOnboarding() {
        settings.setBool("onboarding.completed", true)
        onboardingRequested = false
        persistOnboardingFlag()
    }

    /** Re-open onboarding (used by the "Show again" action). */
    fun requestOnboarding() {
        settings.setBool("onboarding.completed", false)
        onboardingRequested = true
        persistOnboardingFlag()
    }

    // ---------------------------------------------------------------
    // Product tutorial state (Settings → General → Product tutorial)
    // ---------------------------------------------------------------
    /** True while the product tutorial overlay should be on screen. */
    var tutorialRequested by mutableStateOf(false)

    /** Open the product tutorial immediately (used from Settings). */
    fun requestTutorial() {
        tutorialRequested = true
    }

    /** Chapters the user has completed, in order — persisted. */
    fun tutorialCompleted(): List<String> =
        settings.getString("tutorial.completed", "")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun tutorialChapterComplete(chapter: String): Boolean =
        chapter in tutorialCompleted()

    /** Mark one chapter complete (persisted; idempotent). */
    fun markTutorialChapterComplete(chapter: String) {
        val done = tutorialCompleted().toMutableSet()
        if (done.add(chapter)) {
            settings.setString("tutorial.completed", done.joinToString(","))
        }
    }

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
        return collections.resolveCards(resolved, cards.toList(), library)
    }

    /**
     * Distinct deck names studied on [date], resolved from real review events
     * (event → card → deck). Cards whose deck has no user-visible name fall
     * back to the raw deck id — same convention as the Library view.
     */
    fun decksStudiedOn(date: LocalDate, tz: TimeZone = TimeZone.currentSystemDefault()): List<String> {
        val ids = buildSet {
            for (entry in reviewLog) {
                if (entry.reviewedAt.toLocalDateTime(tz).date == date) {
                    cards.firstOrNull { it.id == entry.cardId }?.deckId?.let { add(it) }
                }
            }
        }
        return ids.mapNotNull { id -> library.deck(id)?.name ?: id }.sorted()
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
        activity.recordSignal(SignalContext.Study)
        val session = ReviewSession(name = "Review", createdAt = now)
        session.enqueue(queue, shuffle = false)
        reviewSession = session
        sessionResults.clear()
        sessionStartedAt = now
        answerRevealed = false
        currentView = WorkspaceView.Review
    }

    /**
     * Start a study session for a deck in one of its study modes.
     * The queue is projected onto the mode's independent SRS state;
     * ratings are written back to that mode's progress only.
     */
    fun startLibraryStudy(deckId: String, mode: StudyMode) {
        val deck = library.deck(deckId) ?: return
        val now = Clock.System.now()
        val queue = library.modeQueue(deck, mode, cards.toList(), reviewSettings, now)
        if (queue.isEmpty()) {
            toastHost.show("Nothing due in \"${deck.name}\" — ${mode.label}", kind = ToastKind.Info)
            return
        }
        activity.recordSignal(SignalContext.Study)
        val session = ReviewSession(name = "${deck.name} — ${mode.label}", createdAt = now)
        session.enqueue(queue, shuffle = false)
        libraryActiveDeck = deckId
        libraryActiveMode = mode
        reviewSession = session
        sessionResults.clear()
        sessionStartedAt = now
        answerRevealed = false
        activityLog.record(ActivityCategory.Review, "Started ${mode.label} for deck \"${deck.name}\" (${queue.size} cards)")
        currentView = WorkspaceView.Review
    }

    /**
     * Start a review session from the unified learning store's real queue
     * (StudyEngine: due + new, per-deck limits, mode-aware). Grading flows
     * through the unified StudyEngine so every answer writes a full-fidelity
     * review event; the legacy ReviewSession UI drives presentation.
     */
    fun startUnifiedReview(
        deckId: String = "",
        mode: ua.syt0r.kanji.desktop.model.StudyMode = ua.syt0r.kanji.desktop.model.StudyMode.Flashcards,
        limit: Int = 100
    ) {
        val queue = if (deckId.isNotBlank()) {
            learning.study.buildQueue(deckId = deckId, mode = mode, newLimit = 20, reviewLimit = limit)
        } else {
            // All decks: merge per-deck queues (respecting each deck's limits).
            library.allDecks().filter { !it.archived }.flatMap { deck ->
                learning.study.buildQueue(deckId = deck.id, mode = mode, newLimit = 10, reviewLimit = limit)
            }.distinctBy { it.card.id }.take(limit)
        }
        if (queue.isEmpty()) {
            toastHost.show("Nothing due in the unified study queue", kind = ToastKind.Info)
            return
        }
        val legacy = queue.mapNotNull { learning.legacyCardsForDeck(it.card.deckId).firstOrNull { c -> c.id == it.card.id } }
            .ifEmpty { learning.allLegacyCards().filter { legacy -> queue.any { it.card.id == legacy.id } } }
        if (legacy.isEmpty()) {
            // Cards exist only in the unified store — materialize them.
            learning.syncFromLegacy(queue.mapNotNull { item ->
                ua.syt0r.kanji.desktop.model.DesktopCard(
                    id = item.card.id,
                    character = item.note.expression,
                    meaning = item.note.meanings.joinToString("; "),
                    onReadings = item.note.onReadings,
                    kunReadings = item.note.kunReadings,
                    status = item.card.status,
                    intervalDays = item.card.intervalDays,
                    dueAt = item.card.dueAt,
                    lapses = item.card.lapses,
                    reps = item.card.reps,
                    ease = item.card.ease,
                    accuracy = item.card.accuracy,
                    deckId = item.card.deckId,
                    contentKind = when (item.note.kind) {
                        ua.syt0r.kanji.desktop.engine.learning.LearningItemKind.Kanji -> ua.syt0r.kanji.desktop.model.ContentKind.Kanji
                        ua.syt0r.kanji.desktop.engine.learning.LearningItemKind.Vocabulary -> ua.syt0r.kanji.desktop.model.ContentKind.Vocabulary
                        ua.syt0r.kanji.desktop.engine.learning.LearningItemKind.Kana -> ua.syt0r.kanji.desktop.model.ContentKind.Kana
                        ua.syt0r.kanji.desktop.engine.learning.LearningItemKind.Radical -> ua.syt0r.kanji.desktop.model.ContentKind.Radical
                        ua.syt0r.kanji.desktop.engine.learning.LearningItemKind.Grammar -> ua.syt0r.kanji.desktop.model.ContentKind.Grammar
                        else -> ua.syt0r.kanji.desktop.model.ContentKind.Sentence
                    }
                )
            })
        }
        activity.recordSignal(SignalContext.Study)
        val session = ReviewSession(name = if (deckId.isNotBlank()) "Unified deck study" else "Unified review", createdAt = Clock.System.now())
        session.enqueue(legacy, shuffle = false)
        reviewSession = session
        sessionResults.clear()
        sessionStartedAt = Clock.System.now()
        answerRevealed = false
        libraryActiveDeck = null
        libraryActiveMode = null
        unifiedReviewActive = true
        unifiedReviewQueue = queue
        unifiedReviewIndex = 0
        unifiedReviewDeck = deckId
        unifiedSession = learning.study.openSession(deckId.ifBlank { "all" }, mode)
        activityLog.record(ActivityCategory.Review, "Started unified review (${queue.size} cards)")
        currentView = WorkspaceView.Review
    }

    /** Unified review scoped to one deck's study mode. */
    fun startUnifiedDeckReview(deckId: String, mode: ua.syt0r.kanji.desktop.model.StudyMode) {
        startUnifiedReview(deckId = deckId, mode = mode)
    }

    /** End a unified review session and persist its study-session record. */
    private fun finishUnifiedReview() {
        unifiedSession?.let { session ->
            val completed = sessionResults.size
            learning.study.finishSession(
                session.copy(
                    cardsSeen = completed,
                    cardsCompleted = completed,
                    correctCount = sessionResults.count { it.rating != ReviewRating.Again },
                    againCount = sessionResults.count { it.rating == ReviewRating.Again }
                )
            )
        }
        unifiedReviewActive = false
        unifiedReviewQueue = emptyList()
        unifiedReviewIndex = 0
        unifiedReviewDeck = null
        unifiedSession = null
    }

    fun rateCurrent(rating: ReviewRating) {
        val session = reviewSession ?: return
        if (session.isFinished) return
        // Grading is the strongest engagement signal — it keeps the active
        // study interval alive (see ActivityTracker).
        activity.recordSignal(SignalContext.Study)
        val entry = session.current() ?: return
        val card = entry.card
        val beforeStatus = card.status
        val beforeInterval = card.intervalDays

        val updated = session.answer(rating)
        sessionResults.add(ReviewResult(card.id, rating, updated.status, updated.intervalDays))

        // Unified-source sessions grade through the real StudyEngine so the
        // answer writes a full-fidelity event and SRS state into the store.
        if (unifiedReviewActive) {
            val item = unifiedReviewQueue.getOrNull(unifiedReviewIndex)
            unifiedReviewIndex += 1
            if (item != null) {
                learning.study.grade(
                    item = item,
                    rating = rating,
                    responseTimeMs = 0,
                    sessionId = unifiedSession?.id.orEmpty(),
                    now = Clock.System.now()
                )
                // Keep the legacy pool's copy of this card in sync so the rest
                // of the UI (browser, library) sees the new SRS state.
                val idx = cards.indexOfFirst { it.id == card.id }
                if (idx >= 0) cards[idx] = updated
            }
            activityLog.record(ActivityCategory.Review, "Unified review: ${card.character} — ${rating.displayName}")
        } else {
            val mode = libraryActiveMode
            if (mode != null) {
                // Independent per-mode progress.
                library.recordRating(card.id, mode, rating)
                activityLog.record(ActivityCategory.Review, "${mode.label}: ${card.character} — ${rating.displayName}")
            } else {
                val idx = cards.indexOfFirst { it.id == card.id }
                if (idx >= 0) cards[idx] = updated
                activityLog.record(ActivityCategory.Review, "Reviewed ${card.character} — ${rating.displayName}")
            }

            reviewLog.add(
                ReviewLogEntry(
                    cardId = card.id,
                    reviewedAt = updated.lastReviewedAt ?: Clock.System.now(),
                    rating = rating,
                    intervalBefore = beforeInterval,
                    intervalAfter = updated.intervalDays,
                    wasNew = beforeStatus == SrsStatus.New,
                    source = mode?.name?.lowercase() ?: "review"
                )
            )
            // Keep the unified review-event stream in sync — statistics, exams
            // and mistakes all read from it.
            learning.recordLegacyReview(updated, rating)
        }
        persistStatistics()
        if (libraryActiveMode == null) persistCards()

        answerRevealed = false
        if (session.isFinished) {
            if (unifiedReviewActive) finishUnifiedReview()
            endReview()
        }
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
            if (unifiedReviewActive) {
                learning.study.suspend(card.id)
                val idx = cards.indexOfFirst { it.id == card.id }
                if (idx >= 0) cards[idx] = updated
                persistCards()
            } else {
                val mode = libraryActiveMode
                if (mode != null) {
                    library.suspend(card.id, mode)
                    activityLog.record(ActivityCategory.Review, "Suspended ${card.character} (${mode.label})")
                } else {
                    val idx = cards.indexOfFirst { it.id == card.id }
                    if (idx >= 0) {
                        cards[idx] = updated
                        persistCards()
                    }
                    activityLog.record(ActivityCategory.Review, "Suspended ${card.character}")
                }
            }
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
            if (unifiedReviewActive) {
                learning.study.forget(entry.card.id)
                val idx = cards.indexOfFirst { it.id == entry.card.id }
                if (idx >= 0) cards[idx] = updated
                persistCards()
            } else {
                val mode = libraryActiveMode
                if (mode != null) {
                    library.forget(entry.card.id, mode)
                    activityLog.record(ActivityCategory.Review, "Forgot ${entry.card.character} (${mode.label})")
                } else {
                    val idx = cards.indexOfFirst { it.id == entry.card.id }
                    if (idx >= 0) {
                        cards[idx] = updated
                        persistCards()
                    }
                    activityLog.record(ActivityCategory.Review, "Forgot ${entry.card.character}")
                }
            }
            session.removeCard(entry.card.id)
        }
        if (session.isFinished) endReview()
    }

    fun rescheduleCurrent(days: Int) {
        val session = reviewSession ?: return
        val entry = session.current()
        session.setCustomInterval(days.toDouble())
        if (entry != null) {
            val mode = libraryActiveMode
            if (mode != null) {
                library.reschedule(entry.card.id, mode, days)
            } else {
                val idx = cards.indexOfFirst { it.id == entry.card.id }
                if (idx >= 0) {
                    cards[idx] = cards[idx].copy(
                        dueAt = Clock.System.now().minus(-days.toLong(), DateTimeUnit.DAY, TimeZone.currentSystemDefault()),
                        intervalDays = days.toDouble()
                    )
                    persistCards()
                }
            }
        }
        answerRevealed = false
        if (session.isFinished) endReview()
    }

    fun endReview() {
        // Study time is engagement time, never session wall-clock: if the
        // user walked away mid-session the lapsed intervals are excluded.
        // Falls back to wall time when tracking is disabled.
        val elapsed = activity.engagedSince(sessionStartedAt)
        val correct = sessionResults.count { it.rating != ReviewRating.Again }
        val wrong = sessionResults.count { it.rating == ReviewRating.Again }
        val newCount = sessionResults.count { it.newStatus == SrsStatus.Learning }
        val reviewCount = sessionResults.count { it.newStatus == SrsStatus.Review }

        mergeIntoToday(newCount, reviewCount, correct, wrong, elapsed)

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
        libraryActiveDeck = null
        libraryActiveMode = null
        if (unifiedReviewActive) finishUnifiedReview()
        // Reward the finish: a success toast summarizing the session, with a
        // special line for a perfect run.
        toastHost.show(
            completionToastMessage(rated, correct, "cards", "Session complete"),
            kind = ToastKind.Success,
            durationMs = 4200
        )
        currentView = WorkspaceView.Dashboard
    }

    // ---------------------------------------------------------------
    // Card editor
    // ---------------------------------------------------------------
    fun openEditor(card: DesktopCard?) {
        editingCard = card
    }

    /** Create a blank card and open it in the editor (saved on Save). */
    fun newCard() {
        val card = DesktopCard(
            id = "card-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(9999)}",
            character = "",
            meaning = "",
            status = SrsStatus.New,
            createdAt = Clock.System.now()
        )
        editingCard = card
        currentView = WorkspaceView.Browser
    }

    fun saveEditedCard(card: DesktopCard) {
        val idx = cards.indexOfFirst { it.id == card.id }
        if (idx >= 0) {
            cards[idx] = card
        } else {
            cards.add(0, card)
        }
        persistCards()
        activityLog.record(ActivityCategory.Study, "Edited card \"${card.character}\"")
        toastHost.show("Card saved", kind = ToastKind.Success)
        editingCard = null
    }

    fun deleteEditingCard() {
        val card = editingCard ?: return
        deleteCard(card.id)
        toastHost.show("Card deleted", kind = ToastKind.Info)
        editingCard = null
    }

    // ---------------------------------------------------------------
    // Writing practice (kanji handwriting drills)
    // ---------------------------------------------------------------
    fun startWritingPractice(limit: Int = 12, includeNew: Boolean = true) {
        val now = Clock.System.now()
        val pool = cards.filter { card ->
            card.status != SrsStatus.Suspended &&
                card.status != SrsStatus.Buried &&
                isWritableCharacter(card.character)
        }
        val due = pool.filter { it.status != SrsStatus.New && it.dueAt != null && it.dueAt <= now }
        val newCards = if (includeNew) pool.filter { it.status == SrsStatus.New } else emptyList()
        val queue = (newCards.take(limit / 2) + due.take(limit - limit / 2))
            .distinctBy { it.id }
            .shuffled(Random(11))
            .take(limit)

        if (queue.isEmpty()) {
            toastHost.show("No writable cards available for writing practice", kind = ToastKind.Info)
            return
        }

        activity.recordSignal(SignalContext.Writing)
        val session = ReviewSession(name = "Writing practice", createdAt = now)
        session.enqueue(queue, shuffle = false)
        writingSession = session
        writingResults.clear()
        writingStartedAt = now
        writingRevealed = false
        currentView = WorkspaceView.Writing
    }

    fun rateWriting(rating: ReviewRating, canvas: ua.syt0r.kanji.desktop.ui.writing.WritingCanvasState? = null) {
        val session = writingSession ?: return
        if (session.isFinished) return
        activity.recordSignal(SignalContext.Writing)
        val entry = session.current() ?: return
        val card = entry.card
        val beforeStatus = card.status

        val updated = session.answer(rating)
        writingResults.add(ReviewResult(card.id, rating, updated.status, updated.intervalDays))

        val mode = libraryActiveMode
        if (mode != null) {
            library.recordRating(card.id, mode, rating)
        } else {
            val idx = cards.indexOfFirst { it.id == card.id }
            if (idx >= 0) cards[idx] = updated
        }

        reviewLog.add(
            ReviewLogEntry(
                cardId = card.id,
                reviewedAt = updated.lastReviewedAt ?: Clock.System.now(),
                rating = rating,
                intervalBefore = card.intervalDays,
                intervalAfter = updated.intervalDays,
                wasNew = beforeStatus == SrsStatus.New,
                source = mode?.name?.lowercase() ?: "writing"
            )
        )
        // Record the writing attempt into the unified store (feeds writing
        // statistics and the weakest-kanji dashboard). When the canvas has
        // strokes and the evaluator has canonical data for the character,
        // the real per-stroke evaluation is stored; otherwise fall back to
        // the self-grade (Again = miss).
        val drawn = canvas?.normalizedStrokes().orEmpty()
        if (drawn.isNotEmpty()) {
            learning.recordEvaluatedWriting(
                card = updated,
                expected = card.character,
                drawnStrokes = drawn,
                canvasWidth = canvas?.canvasSize?.width ?: 380f,
                canvasHeight = canvas?.canvasSize?.height ?: 380f,
                selfRating = rating,
                evaluator = writingEvaluator
            )
        } else {
            val correctWriting = rating != ReviewRating.Again
            learning.recordWritingAttempt(
                card = updated,
                expected = card.character,
                accuracy = if (correctWriting) 1f else 0.3f,
                mistakeCount = if (correctWriting) 0 else 1,
                completed = true
            )
        }
        activityLog.record(ActivityCategory.Review, "Writing: ${card.character} — ${rating.displayName}")
        if (mode == null) persistCards()

        writingRevealed = false
        if (session.isFinished) endWriting()
    }

    fun skipWriting() {
        val session = writingSession ?: return
        session.skip()
        writingRevealed = false
        if (session.isFinished) endWriting()
    }

    /**
     * Study the real mistake queue (Again reviews, failed writing attempts,
     * wrong exam answers, lapsed cards). Cards come from the unified store;
     * ratings flow through the normal review flow, so SRS and statistics
     * update exactly like regular study.
     */
    fun startMistakesReview(limit: Int = 50) {
        val cards = learning.mistakeCards(limit)
        if (cards.isEmpty()) {
            toastHost.show("No recorded mistakes to study yet — review some cards first", kind = ToastKind.Info)
            return
        }
        val now = Clock.System.now()
        activity.recordSignal(SignalContext.Study)
        val session = ReviewSession(name = "Mistakes review", createdAt = now)
        session.enqueue(cards, shuffle = false)
        libraryActiveDeck = null
        libraryActiveMode = null
        reviewSession = session
        sessionResults.clear()
        sessionStartedAt = now
        answerRevealed = false
        activityLog.record(ActivityCategory.Review, "Started mistakes review (${cards.size} cards)")
        currentView = WorkspaceView.Review
    }

    fun endWriting() {
        // Same engagement accounting as review sessions.
        val elapsed = activity.engagedSince(writingStartedAt)
        val correct = writingResults.count { it.rating != ReviewRating.Again }
        val wrong = writingResults.count { it.rating == ReviewRating.Again }
        val newCount = writingResults.count { it.newStatus == SrsStatus.Learning }
        val reviewCount = writingResults.count { it.newStatus == SrsStatus.Review }

        mergeIntoToday(newCount, reviewCount, correct, wrong, elapsed)

        val rated = writingResults.size
        writingSession = null
        writingRevealed = false
        libraryActiveDeck = null
        libraryActiveMode = null
        // Same accuracy-aware completion toast as review sessions, so both
        // finish flows celebrate identically.
        toastHost.show(
            completionToastMessage(rated, correct, "characters", "Writing practice done"),
            kind = ToastKind.Success,
            durationMs = 4200
        )
        currentView = WorkspaceView.Dashboard
    }

    /** Start writing practice scoped to a deck's Writing study mode. */
    fun startLibraryWriting(deckId: String) {
        val deck = library.deck(deckId) ?: return
        val now = Clock.System.now()
        val projected = library.cardsIn(deck, cards.toList()).map { card ->
            library.modeProgress(card.id, StudyMode.Writing).let { p ->
                ua.syt0r.kanji.desktop.engine.library.LibraryScheduler.project(card, p)
            }
        }
        val pool = projected.filter { card ->
            card.status != SrsStatus.Suspended &&
                card.status != SrsStatus.Buried &&
                isWritableCharacter(card.character)
        }
        val due = pool.filter { it.status != SrsStatus.New && it.dueAt != null && it.dueAt <= now }
        val newCards = pool.filter { it.status == SrsStatus.New }
        val queue = (newCards.take(12) + due.take(24)).distinctBy { it.id }.shuffled(Random(11))
        if (queue.isEmpty()) {
            toastHost.show("No writable characters due in \"${deck.name}\"", kind = ToastKind.Info)
            return
        }
        activity.recordSignal(SignalContext.Writing)
        val session = ReviewSession(name = "${deck.name} — Writing", createdAt = now)
        session.enqueue(queue, shuffle = false)
        libraryActiveDeck = deckId
        libraryActiveMode = StudyMode.Writing
        writingSession = session
        writingResults.clear()
        writingStartedAt = now
        writingRevealed = false
        currentView = WorkspaceView.Writing
    }

    /** Record non-card practice time (e.g. grammar drills) into today's summary. */
    fun recordPracticeTime(elapsed: Duration) {
        mergeIntoToday(0, 0, 0, 0, elapsed)
    }

    /** Merge one graded batch into today's study summary. */
    private fun mergeIntoToday(
        newCount: Int,
        reviewCount: Int,
        correct: Int,
        wrong: Int,
        elapsed: Duration
    ) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val existingIdx = summaries.indexOfFirst { it.day == today }
        if (existingIdx >= 0) {
            val s = summaries[existingIdx]
            summaries[existingIdx] = s.copy(
                newCount = s.newCount + newCount,
                reviewCount = s.reviewCount + reviewCount,
                correctCount = s.correctCount + correct,
                wrongCount = s.wrongCount + wrong,
                timeSpent = s.timeSpent + elapsed
            )
        } else {
            summaries.add(
                StudyDaySummary(
                    day = today,
                    newCount = newCount,
                    reviewCount = reviewCount,
                    correctCount = correct,
                    wrongCount = wrong,
                    timeSpent = elapsed
                )
            )
        }
        persistStatistics()
    }

    /** Push the in-memory review log + daily summaries to disk. */
    private fun persistStatistics() {
        library.saveStatistics(reviewLog.toList(), summaries.toList())
    }

    /** Push the in-memory card pool to disk. */
    private fun persistCards() {
        library.saveCards(cards.toList())
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

    /** Total reviews recorded across all summaries. */
    fun totalReviews(): Int = summaries.sumOf { it.newCount + it.reviewCount }

    /** Reviews recorded in the last 7 days (including today). */
    fun weeklyReviews(): Int {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val weekStart = today.minus(6, DateTimeUnit.DAY)
        return summaries
            .filter { it.day >= weekStart.toString() }
            .sumOf { it.newCount + it.reviewCount }
    }

    /** Days studied within the last 7 days (for the streak/weekly view). */
    fun studiedDaysInWeek(): Int {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val weekStart = today.minus(6, DateTimeUnit.DAY)
        return summaries.count { it.day >= weekStart.toString() && (it.newCount + it.reviewCount) > 0 }
    }

    /** Human-friendly total study time, e.g. "12h 34m". */
    fun formatDuration(duration: Duration): String {
        if (duration < Duration.ZERO) return "0m"
        val totalMinutes = duration.inWholeMinutes.coerceAtLeast(0)
        if (totalMinutes == 0L) return "0m"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

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
        persistCards()
        activityLog.record(ActivityCategory.Study, "Added card \"${card.character}\"")
        return card
    }

    fun deleteCard(id: String) {
        val card = cards.firstOrNull { it.id == id }
        cards.removeAll { it.id == id }
        if (selectedCard?.id == id) selectedCard = null
        selectedCardIds.remove(id)
        persistCards()
        activityLog.record(ActivityCategory.Study, "Deleted card \"${card?.character ?: id}\"")
    }

    fun updateCard(card: DesktopCard) {
        val idx = cards.indexOfFirst { it.id == card.id }
        if (idx >= 0) {
            cards[idx] = card
            persistCards()
        }
    }

    /**
     * Bulk-import cards (used by the AnkiConnect importer). New cards are
     * added at the front; updated cards replace their existing entry. The
     * pool is persisted exactly once so large imports don't rewrite the
     * cards file per card.
     */
    fun importCards(newCards: List<DesktopCard>, updatedCards: List<DesktopCard>) {
        val additions = newCards.filter { new -> cards.none { it.id == new.id } }
        if (additions.isNotEmpty()) {
            cards.addAll(0, additions)
        }
        updatedCards.forEach { card ->
            val idx = cards.indexOfFirst { it.id == card.id }
            if (idx >= 0) cards[idx] = card
        }
        if (additions.isNotEmpty() || updatedCards.isNotEmpty()) {
            persistCards()
        }
    }

    // ---------------------------------------------------------------
    // Theme setup
    // ---------------------------------------------------------------
    fun applyTheme(themeId: String) {
        themeManager.applyTheme(themeId)
        themeStudioDirty = true
    }

    fun exportThemeJson(): String = themeManager.exportJson(themeManager.activeThemeId)

    fun importThemeJson(json: String): Boolean {
        val ok = themeManager.importJson(json)
        if (ok) {
            themeStudioDirty = true
            toastHost.show("Theme imported", kind = ToastKind.Success)
        } else {
            toastHost.show("Invalid theme JSON", kind = ToastKind.Error)
        }
        return ok
    }

    // ---------------------------------------------------------------
    // Stress dataset (perf demo)
    // ---------------------------------------------------------------
    fun loadStressDataset(count: Int) {
        val before = cards.size
        val stress = buildStressDataset(count)
        cards.addAll(stress)
        persistCards()
        toastHost.show("Added $count synthetic cards ($before → ${cards.size})", kind = ToastKind.Success)
        activityLog.record(ActivityCategory.Study, "Loaded stress dataset ($count cards)")
    }

    // ---------------------------------------------------------------
    // Bundled reference content
    // ---------------------------------------------------------------
    /** Install the bundled kanji reference dictionary (offline lookup). */
    private fun seedDictionary() {
        if (dictionary.isInstalled(DictionaryService.SEED_DICTIONARY_ID)) return
        dictionary.install(
            DictionaryService.seedMeta(),
            DictionaryService.seedEntries(),
            state = this
        )
    }
}

/** Maps a stored navigation-mode string to the two-mode model, migrating legacy values. */
private fun navLayoutFromStored(value: String?): NavLayout? = when (value?.lowercase()) {
    "sidebar" -> NavLayout.Sidebar
    "floating" -> NavLayout.Floating
    // Legacy sidebar docks map onto the sidebar with their own expansion state.
    "expanded", "compact" -> NavLayout.Sidebar
    "bubble", "hidden", "both" -> NavLayout.Floating
    else -> null
}

/**
 * Whether a character can be graded by the writing engine: a single kanji
 * or a single kana unit (the built-in evaluator and KanjiVG both cover the
 * full syllabary). Multi-character clusters like きゃ are studied via the
 * other modes — their per-character strokes are never guessed here.
 */
fun isWritableCharacter(character: String): Boolean {
    if (character.length != 1) return false
    val code = character[0].code
    return code in 0x4E00..0x9FFF || // CJK unified ideographs (kanji)
        code in 0x3400..0x4DBF ||    // CJK extension A
        code in 0x3040..0x309F ||    // hiragana
        code in 0x30A0..0x30FF ||    // katakana
        code in 0x31F0..0x31FF       // katakana phonetic extensions
}

/** Builds the accuracy-aware completion toast text shared by review and writing sessions. */
private fun completionToastMessage(
    rated: Int,
    correct: Int,
    subject: String,
    base: String
): String {
    val accuracyPct = if (rated == 0) 100 else (correct * 100 / rated)
    return when {
        rated == 0 -> base
        accuracyPct == 100 -> "Perfect session — $rated $subject, 100% accuracy"
        else -> "$base — $rated $subject, $accuracyPct% accuracy"
    }
}

/** Result of a single rating inside a live review session. */
data class ReviewResult(
    val cardId: String,
    val rating: ReviewRating,
    val newStatus: SrsStatus,
    val newInterval: Double
)

/** What [AppState.currentView] means right now: an active tab, or the legacy fallback. */
data class WorkspaceContentTarget(
    val tabId: String?,
    val view: WorkspaceView
)
