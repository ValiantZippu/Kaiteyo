package ua.syt0r.kanji.desktopApp

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
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
import ua.syt0r.kanji.di.appModules
import ua.syt0r.kanji.presentation.KaiteyoApp
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.screen.main.screen.credits.GetCreditLibrariesUseCase

val desktopAppModule = module {
    factory<GetCreditLibrariesUseCase> { JvmGetCreditLibrariesUseCase }

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
    // and never reads/writes the user's saved bounds.
    val savedBounds = if (captureState != null) {
        SavedWindowBounds(width = 1200, height = 800)
    } else {
        WindowStateStore.load()
    }
    val windowState = rememberWindowState(
        size = DpSize(
            savedBounds.width.takeIf { it > 0 }?.dp ?: 1200.dp,
            savedBounds.height.takeIf { it > 0 }?.dp ?: 800.dp
        ),
        position = if (captureState == null && savedBounds.x != null && savedBounds.y != null) {
            WindowPosition(savedBounds.x.dp, savedBounds.y.dp)
        } else {
            WindowPosition.PlatformDefault
        }
    )

    Window(
        onCloseRequest = { exitApplication() },
        state = windowState,
        title = resolveString { appName },
        icon = painterResource(Res.drawable.windowIcon),
        undecorated = true
    ) {
        // KJD language database: quietly download + apply data patches to the
        // bundled database at startup (never blocks the UI; failures surface
        // via the updater's state, and the applied-state file makes re-runs
        // skip work). The desktop suite additionally mirrors the result into
        // Settings via its own hook.
        LaunchedEffect(Unit) {
            org.koin.core.context.GlobalContext.get()
                .get<KjdDatabaseUpdater>()
                .checkOnStartup("stable")
        }

        KaiteyoWindow(
            windowState = windowState,
            onClose = { exitApplication() },
            rememberWindowBounds = captureState == null,
            captureState = captureState,
            content = {
                KaiteyoApp(
                    windowSizeClass = calculateWindowSizeClass()
                )
            }
        )
    }

    // Capture mode: exit on our own once the script has had time to shoot.
    if (captureDwellMs > 0) {
        LaunchedEffect(Unit) {
            delay(captureDwellMs)
            exitApplication()
        }
    }
}