package ua.syt0r.kanji.presentation.common.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

// ============================================================
// ANIMATION TOKENS — one motion language (spec §23, §68–§69)
// ------------------------------------------------------------
// Durations and specs come from here, never per-screen literals.
// Motion categories:
//   - Feedback  (hover/press states)      — fast, subtle
//   - Content   (visibility/position)     — medium, standard
//   - Emphasis  (hero / screen transitions) — slow, deliberate
// Springs are the default for position/scale (no-bouncy); tweens
// are used for alpha/visibility. The reduced-motion gate lives
// with the accessibility settings — surfaces pass their own flag
// into duration() so animations can collapse to 0 ms.
// ============================================================

object AnimationTokens {

    // Durations (ms)
    const val DurationFeedbackFast = 100
    const val DurationContent = 220
    const val DurationEmphasis = 360

    // Spring stiffness for content vs. emphasis motion.
    const val StiffnessContent = Spring.StiffnessMediumLow
    const val StiffnessEmphasis = Spring.StiffnessLow

    /** Standard content spring (position/scale, no bounce). */
    fun contentSpring() = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = StiffnessContent
    )

    /** Emphasis spring (hero elements, deliberate settle). */
    fun emphasisSpring() = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = StiffnessEmphasis
    )

    /** Content tween for alpha/visibility. */
    fun contentTween() = tween<Float>(DurationContent)

    /** Feedback tween for quick fades (tooltips, chip states). */
    fun feedbackTween() = tween<Float>(DurationFeedbackFast)

    /** Collapses a duration to 0 ms under reduced motion. */
    fun duration(reduceMotion: Boolean, normal: Int): Int = if (reduceMotion) 0 else normal
}
