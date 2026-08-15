package ua.syt0r.kanji.presentation.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================
// ADAPTIVE CONTENT WIDTH
// The app was originally built phone-first, so
// many screens cap their content at a 400dp
// column. On desktop those screens render a tiny
// column in a huge window — the classic "desktop
// is a big phone" problem.
//
// This helper exposes the current layout tier so
// screens can decide how much width they use.
// Phone keeps the classic column; wider windows
// expand the content (optionally capped) so the
// screen actually uses the space it is given.
// ============================================

/** Coarse layout tier based on the actual window width. */
enum class ContentLayoutTier {
    /** < 600dp — classic phone column. */
    Phone,
    /** 600..< 900dp — tablets and small windows. */
    Medium,
    /** >= 900dp — desktop. */
    Wide
}

@Composable
fun rememberContentLayoutTier(): ContentLayoutTier {
    val density = LocalDensity.current
    val width = with(density) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    return when {
        width < 600.dp -> ContentLayoutTier.Phone
        width < 900.dp -> ContentLayoutTier.Medium
        else -> ContentLayoutTier.Wide
    }
}

/** True when the window is wide enough for multi-column desktop layouts. */
@Composable
fun isWideContentLayout(): Boolean =
    rememberContentLayoutTier() == ContentLayoutTier.Wide

/**
 * Max content width for the current layout tier. Screens that used to be a
 * hardcoded 400dp column call this instead:
 *
 *     .wrapContentWidth()
 *     .widthIn(max = rememberAdaptiveContentMaxWidth())
 *
 * [wideMax] caps how wide the content grows on desktop. Pass
 * [Dp.Infinity] to let a screen use the full window.
 */
@Composable
fun rememberAdaptiveContentMaxWidth(
    phoneMax: Dp = 400.dp,
    mediumMax: Dp = 600.dp,
    wideMax: Dp = 720.dp
): Dp = when (rememberContentLayoutTier()) {
    ContentLayoutTier.Phone -> phoneMax
    ContentLayoutTier.Medium -> mediumMax
    ContentLayoutTier.Wide -> wideMax
}

/**
 * Adaptive width for dialog surfaces (see MultiplatformDialog and
 * KaiteyoAlertDialog). The app used to pin dialogs at a fixed 360–560dp
 * regardless of the window, leaving them floating in the middle of wide
 * desktop windows. This mirrors the suite's DsDialog behavior: the panel
 * follows the available space ([maxWidth] from the BoxWithConstraints scope
 * inside a Dialog) instead of parking at a fixed size.
 *
 * - Phone: fill the available space (minus the 24dp dialog margin each side).
 * - Medium: 80% of the window, capped at 600dp.
 * - Wide: 60% of the window, clamped to 480–860dp (same as the suite).
 */
@Composable
fun rememberAdaptiveDialogWidth(maxWidth: Dp): Dp = when {
    maxWidth < 600.dp -> (maxWidth - 48.dp).coerceAtLeast(280.dp)
    maxWidth < 900.dp -> (maxWidth * 0.8f).coerceIn(400.dp, 600.dp)
    else -> (maxWidth * 0.6f).coerceIn(480.dp, 860.dp)
}
