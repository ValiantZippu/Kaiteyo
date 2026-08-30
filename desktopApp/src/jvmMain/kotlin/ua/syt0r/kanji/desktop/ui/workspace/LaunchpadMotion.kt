package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Centralized motion tokens for the Launchpad / workspace task-view system.
 * Every animation in the Launchpad reads from here instead of scattering
 * magic constants across composables.
 */
object LaunchpadMotion {

    // ── Launchpad entrance / exit ──
    val enterScale = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 340f
    )
    val exitScale = tween<Float>(180)
    val enterAlpha = tween<Float>(200)
    val exitAlpha = tween<Float>(150)

    // ── Card interactions ──
    val cardHoverScale = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 400f
    )
    val cardPressScale = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 500f
    )

    // ── Bubble physics ──
    val bubbleSnap = spring<androidx.compose.ui.geometry.Offset>(
        dampingRatio = 0.48f,
        stiffness = 280f
    )
    val bubbleGrabScale = spring<Float>(
        dampingRatio = 0.55f,
        stiffness = 380f
    )
    val bubbleHoverScale = spring<Float>(
        dampingRatio = 0.5f,
        stiffness = 420f
    )
    val bubblePressScale = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 600f
    )

    // ── Mode panel ──
    val modePanelScale = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 420f
    )
    val modePanelAlpha = tween<Float>(150)

    // ── Card active indicator pulse ──
    val activeIndicatorPulse = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // ── Workspace preview content fade ──
    val previewContentFade = tween<Float>(120)

    // ── Duration constants (ms) ──
    const val LAUNCHPAD_EXIT_MS = 180
    const val BUBBLE_LONG_PRESS_MS = 480L
    const val CARD_HOVER_DELAY_MS = 80L
    const val PREVIEW_UPDATE_INTERVAL_MS = 500L
}
