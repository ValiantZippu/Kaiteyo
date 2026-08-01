package ua.syt0r.kanji.presentation.common.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

// ============================================
// KAITEYO PAGE TRANSITIONS
// Smooth, purposeful navigation transitions
// Supports: Crossfade, Slide, FadeThrough, Scale
// Configured via AnimationConfig from Appearance Studio
// ============================================

/**
 * Reads the current animation config and returns
 * an enter/exit transition pair based on the selected style.
 */
private fun getTransitionForConfig(
    config: AnimationConfig,
    isForward: Boolean = true
): ContentTransform {
    val direction = if (isForward) 1 else -1
    val duration = if (config.reducedMotion) 0 else config.defaultDuration

    val springFloatSpec: FiniteAnimationSpec<Float> = if (config.reducedMotion) {
        tween(0)
    } else {
        spring(
            dampingRatio = config.springDamping,
            stiffness = config.springStiffness
        )
    }
    val springOffsetSpec: FiniteAnimationSpec<IntOffset> = if (config.reducedMotion) {
        tween(0)
    } else {
        spring(
            dampingRatio = config.springDamping,
            stiffness = config.springStiffness
        )
    }
    val tweenSpec = tween<Float>(duration)

    val enter: EnterTransition
    val exit: ExitTransition

    when (config.pageTransition) {
        PageTransitionType.Crossfade -> {
            enter = fadeIn(tweenSpec)
            exit = fadeOut(tweenSpec)
        }
        PageTransitionType.Slide -> {
            enter = slideInHorizontally(
                animationSpec = springOffsetSpec,
                initialOffsetX = { it * direction }
            ) + fadeIn(tweenSpec)
            exit = slideOutHorizontally(
                animationSpec = springOffsetSpec,
                targetOffsetX = { -it * direction }
            ) + fadeOut(tweenSpec)
        }
        PageTransitionType.FadeThrough -> {
            enter = fadeIn(tweenSpec)
            exit = fadeOut(tween(duration / 2))
        }
        PageTransitionType.Scale -> {
            enter = fadeIn(tweenSpec) + scaleIn(springFloatSpec)
            exit = fadeOut(tween(duration / 2))
        }
    }

    return enter togetherWith exit
}

/**
 * Applies a scale transformation for the Scale transition type
 */
private fun scaleIn(animationSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float>): EnterTransition {
    return androidx.compose.animation.scaleIn(
        animationSpec = animationSpec,
        initialScale = 0.95f
    )
}

/**
 * AnimatedContent wrapper that uses Kaiteyo's animation config.
 * Drop-in replacement for AnimatedContent with theme-aware transitions.
 *
 * Usage:
 * ```
 * KaiteyoAnimatedContent(targetState = currentPage) { page ->
 *     when (page) { ... }
 * }
 * ```
 */
@Composable
fun <S> KaiteyoAnimatedContent(
    targetState: S,
    modifier: Modifier = Modifier,
    label: String = "KaiteyoAnimatedContent",
    content: @Composable AnimatedContentScope.(S) -> Unit
) {
    val config = LocalAnimationConfig.current

    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            getTransitionForConfig(config, isForward = true)
        },
        modifier = modifier,
        label = label,
        content = content
    )
}

/**
 * Returns the enter/exit transition for use outside AnimatedContent.
 * Use with togetherWith() or directly in AnimatedContent's transitionSpec.
 */
fun getPageTransition(
    config: AnimationConfig,
    isForward: Boolean = true
): ContentTransform = getTransitionForConfig(config, isForward)