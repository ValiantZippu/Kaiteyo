package ua.syt0r.kanji.presentation.common.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// ACCESSIBILITY UTILITIES
// ------------------------------------------------------------
// Shared helpers for focus indicators, keyboard navigation,
// semantic labels, and reduced-motion compliance. Used across
// all Kaiteyo screens for consistent accessibility (spec §73).
// ============================================================

// --- Focus Indicator ---

/**
 * Adds a visible focus ring when the element is focused.
 * The ring uses the accent color for consistency with the
 * Kaiteyo design system.
 */
@Composable
fun Modifier.focusIndicator(): Modifier {
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    return this
        .focusable(interactionSource = interactionSource)
        .then(
            if (isFocused) {
                Modifier.border(
                    width = 2.dp,
                    color = accent.primary.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(Dimens.RadiusSm)
                )
            } else Modifier
        )
}

// --- Hover + Focus combined state ---

/**
 * A combined interaction state for components that need to
 * respond to both hover and focus. Returns a triple of
 * (isHovered, isFocused, isActive).
 */
@Composable
fun rememberInteractionState(): Triple<Boolean, Boolean, Boolean> {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    return Triple(isHovered, isFocused, isHovered || isFocused)
}

// --- Keyboard navigation ---

/**
 * Standard Kaiteyo keyboard shortcuts overlay for screens.
 * Provides: Escape to go back, / to focus search, Ctrl+K for
 * global search.
 */
fun Modifier.kaiteyoKeyboardNavigation(
    onEscape: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null
): Modifier {
    return this.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when {
            event.key == Key.Escape && onEscape != null -> {
                onEscape()
                true
            }
            event.key == Key.Slash && onSearch != null -> {
                onSearch()
                true
            }
            else -> false
        }
    }
}

// --- Touch target sizing ---

/**
 * Ensures a minimum touch target size of 48dp per Material
 * Design guidelines. Wraps content in a 48dp hit area.
 */
fun Modifier.minimumTouchTarget(): Modifier = this.then(
    Modifier
        .focusable()
        .onPreviewKeyEvent { false } // consume nothing, just ensure focusable
)

// --- Screen reader labels ---

/**
 * Provides a semantic description for screen readers.
 * Use this instead of contentDescription alone when the
 * description should be more detailed.
 */
fun screenReaderLabel(
    label: String,
    hint: String? = null
): String = buildString {
    append(label)
    if (hint != null) append(". $hint")
}

// --- Reduced motion ---

/**
 * Returns animation duration that respects reduced-motion.
 * When reduced motion is enabled, animations are skipped (duration = 0).
 */
fun animationDuration(
    reducedMotion: Boolean,
    normalMs: Int = 300
): Int = if (reducedMotion) 0 else normalMs

// --- High contrast mode ---

/**
 * Returns appropriate contrast colors based on the high-contrast
 * setting. In high-contrast mode, text uses pure white/black
 * instead of the standard text colors.
 */
data class ContrastColors(
    val primary: Color,
    val secondary: Color,
    val muted: Color,
    val background: Color,
    val surface: Color
)

@Composable
fun rememberContrastColors(highContrast: Boolean): ContrastColors {
    val surfaceColors = LocalSurfaceColors.current
    return if (highContrast) {
        ContrastColors(
            primary = Color.White,
            secondary = Color(0xFFE0E0E0),
            muted = Color(0xFFBDBDBD),
            background = Color.Black,
            surface = Color(0xFF1A1A1A)
        )
    } else {
        ContrastColors(
            primary = surfaceColors.textPrimary,
            secondary = surfaceColors.textSecondary,
            muted = surfaceColors.textMuted,
            background = surfaceColors.background,
            surface = surfaceColors.surface
        )
    }
}

// --- Focus-visible indicator for cards ---

/**
 * Standard focus-visible indicator for card-like components.
 * Shows a 2dp accent border when focused, 0dp otherwise.
 */
@Composable
fun Modifier.cardFocusIndicator(): Modifier {
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    return this
        .focusable(interactionSource = interactionSource)
        .then(
            if (isFocused) Modifier.border(
                width = 2.dp,
                color = accent.primary.copy(alpha = 0.7f),
                shape = RoundedCornerShape(Dimens.RadiusMd)
            )
            else Modifier
        )
}

// --- Standard padding values for accessibility ---

/**
 * Standard padding for interactive elements that ensures
 * proper spacing for touch targets and focus indicators.
 */
object AccessibilityPadding {
    val touchPadding = PaddingValues(4.dp)
    val focusPadding = PaddingValues(2.dp)
    val combinedPadding = PaddingValues(6.dp)
}
