package ua.syt0r.kanji.desktopApp

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import ua.syt0r.kanji.desktop.engine.updates.DesktopUpdateInstaller
import ua.syt0r.kanji.desktop.engine.updates.HttpUpdateChecker
import ua.syt0r.kanji.desktop.engine.updates.HttpUpdateDownloader
import ua.syt0r.kanji.desktop.engine.updates.UpdatePolicy
import ua.syt0r.kanji.desktop.engine.updates.UpdateService
import ua.syt0r.kanji.desktop.engine.updates.UPDATE_FEED_BASE_URL
import ua.syt0r.kanji.desktop.engine.updates.currentAppVersion
import ua.syt0r.kanji.desktop.engine.updates.updatesDataDir
import ua.syt0r.kanji.desktop.engine.updates.kjd.HttpKjdPatchChecker
import ua.syt0r.kanji.desktop.engine.updates.kjd.HttpKjdPatchDownloader
import ua.syt0r.kanji.desktop.engine.updates.kjd.KJD_PATCH_FEED_BASE_URL
import ua.syt0r.kanji.desktop.engine.updates.kjd.KjdDatabaseLocator
import ua.syt0r.kanji.desktop.engine.updates.kjd.KjdDatabaseUpdater
import ua.syt0r.kanji.desktop.engine.updates.kjd.kjdUpdatesDataDir
import ua.syt0r.kanji.desktop.engine.monitoring.SentryBridge
import ua.syt0r.kanji.di.appModules
import ua.syt0r.kanji.presentation.KaiteyoApp
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.screen.main.screen.credits.GetCreditLibrariesUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.game.GameCentreContent
import ua.syt0r.kanji.presentation.screen.main.screen.media.MediaCentreContent

val desktopAppModule = module {
    factory<GetCreditLibrariesUseCase> { JvmGetCreditLibrariesUseCase }

    // Universal-search "Scan image" OCR input mode (desktop only). The core
    // overlay checks Koin for an optional SearchOcrProvider — on Android/iOS
    // nothing is registered and the control is simply hidden, never a dead
    // button.
    single<ua.syt0r.kanji.core.knowledge.SearchOcrProvider> {
        ua.syt0r.kanji.desktop.engine.ocr.DesktopSearchOcrProvider()
    }

    // The Media Centre destination's real implementation — the desktop suite's
    // MediaView (player, subtitles, dictionary, mining) mounted with its own
    // AppState. Overrides the core default (honest desktop-only placeholder).
    // Koin 4 overrides duplicate definitions automatically (last module wins),
    // so no `override` flag is needed (it no longer exists in the DSL).
    single<MediaCentreContent> { DesktopMediaCentreContent }

    // The Kaiteyo World destination's real implementation — the desktop suite's
    // exploration game mounted with its own AppState. Overrides the core
    // default (the node-based curriculum game). Same last-module-wins rule.
    single<GameCentreContent> { DesktopGameCentreContent }

    // Background scope for long-running engines (update checks, downloads).
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // Auto-update coordinator — feed URL, HTTPS checker, sha256 downloader and
    // the per-OS installer. Configured with the running version at creation.
    single {
        UpdateService(
            scope = get(),
            checker = HttpUpdateChecker(UPDATE_FEED_BASE_URL),
            downloader = HttpUpdateDownloader(),
            installer = DesktopUpdateInstaller(dataDir = updatesDataDir()),
            policy = UpdatePolicy(),
            dataDir = updatesDataDir()
        ).apply { configure(currentAppVersion()) }
    }

    // KJD language database updater — downloads and applies incremental data
    // patches to the bundled KJD database (non-destructive, fingerprint
    // verified). Mirrors the app UpdateService; see engine/updates/kjd/.
    single {
        KjdDatabaseUpdater(
            scope = get(),
            checker = HttpKjdPatchChecker(KJD_PATCH_FEED_BASE_URL),
            downloader = HttpKjdPatchDownloader(),
            locator = KjdDatabaseLocator(),
            dataDir = kjdUpdatesDataDir()
        )
    }
}

/** The dev-only `--capture-state=` values accepted by the launcher. */
private val captureStates = setOf("shell", "menu", "launchpad", "strip")

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun main(args: Array<String>) = application {

    // Initialize Sentry error tracking early so uncaught exceptions are
    // captured from the very first line. No-op if the Sentry SDK isn't on
    // the classpath (graceful degradation — the app works identically).
    runCatching {
        SentryBridge.init(
            dsn = System.getenv("SENTRY_DSN") ?: "",
            environment = if (args.contains("--debug")) "development" else "production"
        )
    }

    val koinModuleList = appModules.plus(desktopAppModule)
    startKoin { loadKoinModules(koinModuleList) }

    // Dev-only capture mode: `--capture-state=<shell|menu|launchpad|strip>`
    // pre-opens a launcher state for scripts/capture-window-shell.sh to
    // screenshot, with `--capture-dwell=` (ms) controlling how long the app
    // stays open before exiting on its own (default 20s). Normal runs have
    // captureState == null and behave exactly as before.
    val captureState = args.firstOrNull { it.startsWith("--capture-state=") }
        ?.substringAfter("=")
        ?.takeIf { it in captureStates }
    val captureDwellMs = args.firstOrNull { it.startsWith("--capture-dwell=") }
        ?.substringAfter("=")?.toLongOrNull()
        ?: if (captureState != null) 20_000L else 0L

    // Capture mode forces a fixed window so every screenshot is identical,
    // and never reads/writes the user's saved bounds. Normal runs restore the
    // saved geometry corrected against the current display work areas, so the
    // window can never reopen under the taskbar or off-screen.
    val startup = if (captureState != null) {
        WindowStartupBounds(
            widthDp = 1200f,
            heightDp = 800f,
            xDp = null,
            yDp = null,
            maximized = false
        )
    } else {
        WindowStateStore.load()
    }
    val windowState = rememberWindowState(
        size = DpSize(startup.widthDp.dp, startup.heightDp.dp),
        position = if (captureState == null && startup.xDp != null && startup.yDp != null) {
            WindowPosition(startup.xDp.dp, startup.yDp.dp)
        } else {
            WindowPosition.PlatformDefault
        }
    )

    // Reopen maximized when the previous session ended maximized. Maximizing
    // is delegated to the OS (MAXIMIZED_BOTH / zoom), which respects the
    // usable work area on every platform.
    LaunchedEffect(Unit) {
        if (startup.maximized) windowState.placement = WindowPlacement.Maximized
    }

    Window(
        onCloseRequest = { exitApplication() },
        state = windowState,
        title = resolveString { appName },
        icon = painterResource(Res.drawable.windowIcon),
        undecorated = true
    ) {
        // KJD language database: quietly download + apply data patches to the
        // bundled database at startup (Sentry breadcrumb for debugging) (never blocks the UI; failures surface
        // via the updater's state, and the applied-state file makes re-runs
        // skip work). The desktop suite additionally mirrors the result into
        // Settings via its own hook.
        LaunchedEffect(Unit) {
            SentryBridge.addBreadcrumb("KJD database update check starting", "startup")
            org.koin.core.context.GlobalContext.get()
                .get<KjdDatabaseUpdater>()
                .checkOnStartup("stable")
        }

        // The KaiteyoApp theme root lives ABOVE the window shell, so the
        // custom title bar and chrome render with the real Kaiteyo theme
        // (never the untinted OLED default) and live Theme Studio edits reach
        // the window surface immediately. `shell` mounts KaiteyoWindow inside
        // that theme, wrapping the app content.
        KaiteyoApp(
            windowSizeClass = calculateWindowSizeClass()
        ) { appContent ->
            KaiteyoWindow(
                windowState = windowState,
                onClose = { exitApplication() },
                rememberWindowBounds = captureState == null,
                captureState = captureState,
                content = { appContent() }
            )
        }
    }

    // Capture mode: exit on our own once the script has had time to shoot.
    if (captureDwellMs > 0) {
        LaunchedEffect(Unit) {
            delay(captureDwellMs)
            exitApplication()
        }
    }
}
