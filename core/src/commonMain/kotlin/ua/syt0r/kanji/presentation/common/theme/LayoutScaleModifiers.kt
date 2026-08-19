package ua.syt0r.kanji.presentation.common.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================
// LAYOUT SCALE MODIFIERS
//
// These modifiers read from LocalLayoutConfig to apply the
// user's buttonScale, iconScale, displayScale, transparency,
// blur, and glass settings throughout the app.
//
// Usage:
//   Button(modifier = Modifier.scaledButton()) { ... }
//   Icon(modifier = Modifier.scaledIcon()) { ... }
//   Box(modifier = Modifier.glassSurface()) { ... }
// ============================================================

/**
 * Scale a button by the user's buttonScale setting.
 * Apply to the outermost Modifier chain of any Button composable.
 */
@Composable
fun Modifier.scaledButton(): Modifier {
    val config = LocalLayoutConfig.current
    val scale = config.buttonScale
    return if (scale != 1f) this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    } else this
}

/**
 * Scale an icon by the user's iconScale setting.
 * Apply to any Icon composable's modifier.
 */
@Composable
fun Modifier.scaledIcon(): Modifier {
    val config = LocalLayoutConfig.current
    val scale = config.iconScale
    return if (scale != 1f) this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    } else this
}

/**
 * Scale a surface/container by the user's displayScale setting.
 * This is a per-element display scale — the root-level scale
 * is applied in AppTheme via graphicsLayer on the content wrapper.
 */
@Composable
fun Modifier.scaledDisplay(): Modifier {
    val config = LocalLayoutConfig.current
    val scale = config.displayScale
    return if (scale != 1f) this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    } else this
}

/**
 * Apply the user's bubble scale to the floating launcher.
 */
@Composable
fun Modifier.scaledBubble(): Modifier {
    val config = LocalLayoutConfig.current
    val scale = config.bubbleScale
    return if (scale != 1f) this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    } else this
}

/**
 * Glass/transparency surface effect.
 * When transparency is enabled, applies a semi-transparent background
 * with optional blur (glass morphism). When disabled, uses solid background.
 */
@Composable
fun Modifier.glassSurface(
    baseAlpha: Float = 0.85f
): Modifier {
    val config = LocalLayoutConfig.current
    return if (config.transparencyEnabled) {
        this
            .then(
                if (config.blurEnabled) Modifier.blur(8.dp) else Modifier
            )
            .background(
                LocalSurfaceColors.current.surface.copy(
                    alpha = config.glassOpacity.coerceIn(0.3f, 1f)
                )
            )
    } else {
        this.background(LocalSurfaceColors.current.surface)
    }
}

/**
 * Glass modifier for elevated surfaces (cards, dialogs).
 */
@Composable
fun Modifier.glassElevated(): Modifier {
    val config = LocalLayoutConfig.current
    return if (config.transparencyEnabled) {
        this
            .then(
                if (config.blurEnabled) Modifier.blur(4.dp) else Modifier
            )
            .background(
                LocalSurfaceColors.current.surfaceElevated.copy(
                    alpha = config.glassOpacity.coerceIn(0.3f, 1f)
                )
            )
    } else {
        this.background(LocalSurfaceColors.current.surfaceElevated)
    }
}

/**
 * Scale the toolbar height by toolbarHeightScale.
 */
@Composable
fun Modifier.scaledToolbarHeight(): Modifier {
    val config = LocalLayoutConfig.current
    val scale = config.toolbarHeightScale
    return if (scale != 1f) this.graphicsLayer {
        scaleY = scale
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
    } else this
}

/**
 * Apply window padding scale.
 */
@Composable
fun Modifier.scaledPadding(basePadding: Dp): Modifier {
    val config = LocalLayoutConfig.current
    val scaled = basePadding * config.windowPaddingScale
    return this.padding(scaled)
}

// ============================================================
// COMPOSITION LOCAL for glow config (consumed by accent cards)
// ============================================================

/**
 * Glow intensity modifier for accent-colored elements.
 * Reads from LocalGlowConfig to apply user's glow settings.
 */
@Composable
fun Modifier.accentGlow(
    intensity: Float = 1f
): Modifier {
    val glow = LocalGlowConfig.current
    val accentPrimary = LocalKaiteyoAccent.current.primary
    val effectiveIntensity = intensity * glow.intensity
    if (effectiveIntensity <= 0f) return this
    return this.drawBehind {
        val radius = 40f * glow.radius
        drawCircle(
            color = accentPrimary.copy(
                alpha = 0.15f * glow.opacity * effectiveIntensity
            ),
            radius = radius,
            center = Offset(size.width / 2, size.height / 2)
        )
    }
}

// ============================================================
// HELPERS
// ============================================================

private operator fun Dp.times(factor: Float): Dp = this * factor
