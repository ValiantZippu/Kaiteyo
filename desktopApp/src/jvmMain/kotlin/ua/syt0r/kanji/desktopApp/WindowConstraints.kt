package ua.syt0r.kanji.desktopApp

import androidx.compose.ui.unit.dp

// ============================================
// WINDOW CONSTRAINTS
// Content-derived geometry bounds. No magic
// numbers: each value traces to a layout
// requirement (see the KDoc on each).
// ============================================

object WindowConstraints {

    /**
     * Minimum window width — the expanded sidebar (up to ~280dp) plus a
     * viable content column (top bar + page chrome). Below this the compact
     * tab-bar layout takes over, so the minimum never needs to be absurdly
     * large to keep the app usable.
     */
    val MinWidth = 860.dp

    /**
     * Minimum window height — custom title bar (44dp) + content column tall
     * enough for a page header, a toolbar and one content row.
     */
    val MinHeight = 600.dp

    /** Default first-run window size, in dp (a constant visual size at any DPI). */
    val DefaultWidth = 1200.dp
    val DefaultHeight = 800.dp

    /**
     * Dev-suite minimum size. Deliberately below the main app's minimum so
     * the suite's compact tab-bar tier (WorkspaceShell Breakpoints,
     * 720dp / 760dp hysteresis exit) is reachable while testing — the main
     * app's 860dp stays content-derived and untouched. 700dp clears the
     * breakpoint with hysteresis margin instead of sitting on it.
     */
    val SuiteMinWidth = 700.dp
    val SuiteMinHeight = 560.dp
}
