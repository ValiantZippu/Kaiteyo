package ua.syt0r.kanji.desktop.appstate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.data.buildDemoCards
import ua.syt0r.kanji.desktop.data.buildDemoContentCards
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
import ua.syt0r.kanji.desktop.engine.sync.CloudSyncCoordinator
import ua.syt0r.kanji.desktop.engine.sync.SyncEngine
import ua.syt0r.kanji.desktop.engine.theming.ThemeManager
import ua.syt0r.kanji.desktop.engine.theming.ThemePresets
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryService
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.mining.MiningEngine
import ua.syt0r.kanji.desktop.engine.mining.MiningStatisticsStore
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
import ua.syt0r.kanji.desktop.model.StudyMode
import ua.syt0r.kanji.desktop.model.StudyModeProgress
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
import kotlinx.datetime.todayIn
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** A single view of the workspace. */
enum class WorkspaceView(val label: String, val icon: String) {
    Dashboard("Dashboard", "I"),
    Browser("Browser", "B"),
    Review("Review", "R"),
    Writing("Writing", "W"),
    Grammar("Grammar", "G"),
    Library("Library", "L"),
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
    Account("Account", "A"),
    Contributions("About", "B"),
    Dictionary("Dictionary", "D"),
    Mining("Mining", "M"),
    Media("Media", "V"),
    LearningBrowser("Web Browser", "W"),
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


    /** Reduced motion is the OR of the global preference and the nav-specific one. */
    private fun refreshReducedMotion() {
        navReducedMotion = settings.getBool("appearance.reduced-motion") || settings.getBool("navigation.reduced-motion")
    }

    // ---------------------------------------------------------------
    // Learning workspace engines (dictionary, mining, media, browser,
    // OCR and the local integration API).
    // ---------------------------------------------------------------
    val dictionary = DictionaryService(DictionaryRepository(dictionaryDir()))
    val miningIntegration = ua.syt0r.kanji.desktop.engine.mining.MiningIntegrationManager(settings)
    val mining = MiningEngine(this)
    /** All-source mining counters (per day + per source) for Statistics/Dashboard. */
    val miningStatistics = MiningStatisticsStore()
    val media = MediaEngine(this)
    val browserEngine = BrowserEngine()
    val ocr = OcrEngine()
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
    var currentView by mutableStateOf(WorkspaceView.Dashboard)
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
        // The card pool is the user's study data: restore it first so a
        // relaunch continues exactly where the previous session stopped.
        // On a true first run there is nothing to restore and demo data is
        // seeded once (see seedDemoData) — never on every launch.
        library.loadCards()?.let { cards.addAll(it) }
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

    fun rateCurrent(rating: ReviewRating) {
        val session = reviewSession ?: return
        if (session.isFinished) return
        val entry = session.current() ?: return
        val card = entry.card
        val beforeStatus = card.status
        val beforeInterval = card.intervalDays

        val updated = session.answer(rating)
        sessionResults.add(ReviewResult(card.id, rating, updated.status, updated.intervalDays))

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
        persistStatistics()
        if (mode == null) persistCards()

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
        val elapsed = Clock.System.now() - sessionStartedAt
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
                card.character.any { it.code in 0x4E00..0x9FFF }
        }
        val due = pool.filter { it.status != SrsStatus.New && it.dueAt != null && it.dueAt <= now }
        val newCards = if (includeNew) pool.filter { it.status == SrsStatus.New } else emptyList()
        val queue = (newCards.take(limit / 2) + due.take(limit - limit / 2))
            .distinctBy { it.id }
            .shuffled(Random(11))
            .take(limit)

        if (queue.isEmpty()) {
            toastHost.show("No kanji cards available for writing practice", kind = ToastKind.Info)
            return
        }

        val session = ReviewSession(name = "Writing practice", createdAt = now)
        session.enqueue(queue, shuffle = false)
        writingSession = session
        writingResults.clear()
        writingStartedAt = now
        writingRevealed = false
        currentView = WorkspaceView.Writing
    }

    fun rateWriting(rating: ReviewRating) {
        val session = writingSession ?: return
        if (session.isFinished) return
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

    fun endWriting() {
        val elapsed = Clock.System.now() - writingStartedAt
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
            completionToastMessage(rated, correct, "kanji", "Writing practice done"),
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
                card.character.any { it.code in 0x4E00..0x9FFF }
        }
        val due = pool.filter { it.status != SrsStatus.New && it.dueAt != null && it.dueAt <= now }
        val newCards = pool.filter { it.status == SrsStatus.New }
        val queue = (newCards.take(12) + due.take(24)).distinctBy { it.id }.shuffled(Random(11))
        if (queue.isEmpty()) {
            toastHost.show("No kanji due for writing in \"${deck.name}\"", kind = ToastKind.Info)
            return
        }
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
    // Demo seeding
    // ---------------------------------------------------------------
    fun seedDemoData() {
        // Never reseed over a real pool: demo data is only for a true first
        // run. Imported / edited / reviewed cards must survive every launch.
        if (cards.isNotEmpty() || library.hasPersistedCards()) return
        val demo = buildDemoCards()
        cards.addAll(demo)
        cards.addAll(buildDemoContentCards())
        seedSummaries()
        seedCollections()
        seedActivity()
        seedDictionary()
        seedLibrary()
        // Persist the seeded stats so they survive restarts instead of being
        // regenerated on every launch until the first real review.
        persistStatistics()
        persistCards()
    }

    /** Seed the library: per-mode progress for every card + a couple of recent searches. */
    private fun seedLibrary() {
        val now = Clock.System.now()
        val bulk = HashMap<String, Map<StudyMode, StudyModeProgress>>()
        cards.forEachIndexed { index, card ->
            val modes = LinkedHashMap<StudyMode, StudyModeProgress>()
            StudyMode.entries.forEach { mode ->
                var progress = library.modeProgress(card.id, mode)
                // Give demo cards a plausible spread of mode progress so
                // independent tracks are visible from the first run.
                if (index % 3 == 0 && card.status != SrsStatus.New) {
                    progress = progress.copy(
                        status = if (index % 4 == 0) SrsStatus.Learning else SrsStatus.Review,
                        reps = (index % 9) + 1,
                        intervalDays = ((index % 7) * 1.5).toDouble(),
                        accuracy = (0.6f + (index % 35) / 100f).coerceAtMost(0.98f),
                        streak = index % 6,
                        bestStreak = (index % 11),
                        totalReviews = (index % 9) + 1,
                        totalCorrect = index % 7,
                        dueAt = if (index % 5 == 0) now else null,
                        lastReviewedAt = now.minus((index % 9).toLong(), DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                    )
                }
                modes[mode] = progress
            }
            bulk[card.id] = modes
        }
        // Persist once instead of once per card per mode — keeps first launch instant.
        library.bulkSetProgress(bulk)
        library.recordSearch("kind:vocabulary")
        library.recordSearch("jlpt:5")
        activityLog.record(ActivityCategory.Study, "Library seeded (${library.decks.size} decks, ${cards.size} entries)")
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

/** Maps a stored navigation-mode string to the two-mode model, migrating legacy values. */
private fun navLayoutFromStored(value: String?): NavLayout? = when (value?.lowercase()) {
    "sidebar" -> NavLayout.Sidebar
    "floating" -> NavLayout.Floating
    // Legacy sidebar docks map onto the sidebar with their own expansion state.
    "expanded", "compact" -> NavLayout.Sidebar
    "bubble", "hidden", "both" -> NavLayout.Floating
    else -> null
}

/** Minimal demo-seed mirror so [seedSummaries] can tag its log entries. */
private data class DemoSeedRef(val character: String, val cardId: String)

private fun demoKanji(): List<DemoSeedRef> =
    ua.syt0r.kanji.desktop.data.demoKanji.map { DemoSeedRef(it.character, "kanji-${it.character.hashCode().toString(16)}") }

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
