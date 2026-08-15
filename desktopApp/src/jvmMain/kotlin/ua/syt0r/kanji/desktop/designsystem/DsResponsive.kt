package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — RESPONSIVE HELPERS
// Width tiers shared by dialogs, panels, forms
// and grids so every surface adapts to the
// window it lives in instead of parking at a
// fixed size in the middle of the screen.
//
// All values are breakpoint-derived (see the
// tier docs) — nothing here is a magic number.
// ============================================

/** Meaningful desktop width tiers, matching the workspace shell's breakpoints. */
object DsWidthTiers {

    /** Below this the compact layout is active (see WorkspaceShell.Breakpoints). */
    val Compact = 720.dp

    /** Typical restored window / small laptop. */
    val Standard = 1024.dp

    /** Wide window — grids start spreading into more columns here. */
    val Wide = 1440.dp

    /** Extra-wide window (maximized on large monitors) — content spreads fully. */
    val ExtraWide = 1920.dp
}

/**
 * The available width tier as a comparable ordinal, so callers can pick
 * column counts and sizes with `when`/`if` without repeating breakpoints.
 */
@Composable
fun rememberWidthTier(availableWidth: Dp): Int = when {
    availableWidth < DsWidthTiers.Compact -> 0
    availableWidth < DsWidthTiers.Standard -> 1
    availableWidth < DsWidthTiers.Wide -> 2
    availableWidth < DsWidthTiers.ExtraWide -> 3
    else -> 4
}

/**
 * A width that grows with the window but never becomes absurd: a fraction of
 * the available width (falling back to [minimum] on narrow windows), capped
 * at [maximum]. Defaults suit modal dialogs (readable on small windows,
 * generously sized on wide ones).
 */
fun adaptiveWidth(
    availableWidth: Dp,
    fraction: Float,
    minimum: Dp,
    maximum: Dp
): Dp {
    val target = availableWidth * fraction
    return target.coerceIn(minimum, maximum)
}

/** Dialog width that follows the window: compact dialogs stay readable, wide ones spread. */
@Composable
fun adaptiveDialogWidth(availableWidth: Dp, compact: Boolean = false): Dp {
    if (compact) return adaptiveWidth(availableWidth, 0.5f, 400.dp, 560.dp)
    return adaptiveWidth(availableWidth, 0.6f, 480.dp, 860.dp)
}

/**
 * Column count for adaptive grids: 1 column in compact widths, spreading up
 * to [maxColumns] as the window widens. Each column stays ≥ [minColumnWidth].
 */
fun gridColumnCount(availableWidth: Dp, minColumnWidth: Dp, maxColumns: Int = 6): Int {
    val byWidth = (availableWidth / minColumnWidth).toInt().coerceAtLeast(1)
    return byWidth.coerceAtMost(maxColumns)
}
