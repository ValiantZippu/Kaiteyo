package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.runtime.IntState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Tracks the left edge of the **content area** (in window pixels) so the
 * custom title bar can exclude the sidebar and floating bubble from its
 * hover-trigger zone.  NavShell writes this; KaiteyoWindow reads it.
 */
val LocalContentAreaLeftPx: MutableIntState = mutableIntStateOf(0)
