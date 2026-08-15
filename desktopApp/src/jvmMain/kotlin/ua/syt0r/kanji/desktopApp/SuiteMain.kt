package ua.syt0r.kanji.desktopApp

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import ua.syt0r.kanji.desktop.ui.KaiteyoDesktopSuite
import ua.syt0r.kanji.di.appModules

// ============================================
// KAITEYO DESKTOP SUITE — standalone entry point
// Launches the self-contained desktop suite
// (engines + design system) in the same borderless
// window shell as the main app. Invoke this entry
// point directly (named main is owned by Main.kt).
//
// Koin is started here (same modules as the main
// app) so composables in the suite can use
// koinInject() — e.g. the UpdateService in Settings.
// The suite's own theme root wraps the window shell
// (see the `shell` slot on KaiteyoDesktopSuite), so
// the chrome follows the suite's Theme Studio theme.
// ============================================

fun desktopSuiteMain() = application {
    val koinModuleList = appModules.plus(desktopAppModule)
    startKoin { loadKoinModules(koinModuleList) }

    val windowState = rememberWindowState(
        size = DpSize(1280.dp, 820.dp)
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Kaiteyo Desktop Suite",
        undecorated = true
    ) {
        KaiteyoDesktopSuite { suiteContent ->
            KaiteyoWindow(
                windowState = windowState,
                onClose = ::exitApplication,
                // The dev suite keeps its own fixed size and never touches the
                // saved window bounds of the main application. Its minimum is
                // smaller than the main app's so the compact tab-bar tier
                // (WorkspaceShell, 720dp) is actually reachable while testing.
                rememberWindowBounds = false,
                minSize = DpSize(
                    WindowConstraints.SuiteMinWidth,
                    WindowConstraints.SuiteMinHeight
                ),
                content = { suiteContent() }
            )
        }
    }
}
