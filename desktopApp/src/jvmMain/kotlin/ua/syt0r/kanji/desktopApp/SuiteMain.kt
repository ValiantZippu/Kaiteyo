package ua.syt0r.kanji.desktopApp

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ua.syt0r.kanji.desktop.ui.KaiteyoDesktopSuite

// ============================================
// KAITEYO DESKTOP SUITE — standalone entry point
// Launches the self-contained desktop suite
// (engines + design system) in the same borderless
// window shell as the main app. Invoke this entry
// point directly (named main is owned by Main.kt).
// ============================================

fun desktopSuiteMain() = application {
    val windowState = rememberWindowState(
        size = DpSize(1280.dp, 820.dp)
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Kaiteyo Desktop Suite",
        undecorated = true
    ) {
        KaiteyoWindow(
            windowState = windowState,
            onClose = ::exitApplication,
            content = {
                KaiteyoDesktopSuite()
            }
        )
    }
}
