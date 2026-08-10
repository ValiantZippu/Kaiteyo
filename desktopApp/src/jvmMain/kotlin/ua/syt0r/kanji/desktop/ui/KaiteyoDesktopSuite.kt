package ua.syt0r.kanji.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.theming.ThemeMapper
import ua.syt0r.kanji.desktop.engine.updates.UpdateChannel
import ua.syt0r.kanji.desktop.engine.updates.UpdateService
import ua.syt0r.kanji.desktop.ui.workspace.KaiteyoWorkspace
import ua.syt0r.kanji.desktopApp.OnboardingWizard
import ua.syt0r.kanji.presentation.common.theme.AppTheme
import ua.syt0r.kanji.presentation.common.theme.ThemeTransitionMillis
import ua.syt0r.kanji.presentation.common.theme.tweenDuration

// ============================================
// KAITEYO DESKTOP SUITE — root coordinator
// Owns the single AppState instance, seeds the
// demo deck on first launch, derives the live
// theme from the active theme studio preset and
// mounts the workspace shell + global overlays.
// First launch shows the onboarding wizard;
// afterwards it never appears again unless the
// user explicitly re-requests it from Settings.
// ============================================

@Composable
fun KaiteyoDesktopSuite() {
    val state = remember { AppState() }

    // First-run gating: show onboarding when it has never been completed,
    // or the moment the user re-requests it from Settings. Reading
    // onboardingRequested here subscribes to the AppState value, so a
    // request from Settings shows the wizard immediately.
    var showOnboarding by remember { mutableStateOf(!state.onboardingCompleted) }
    val onboardingRequested = state.onboardingRequested
    val onboardingVisible = showOnboarding || onboardingRequested

    // Quiet update check at startup when the user opted in (Settings → Updates).
    val updateService = koinInject<UpdateService>()

    LaunchedEffect(Unit) {
        state.seedDemoData()
        if (state.settings.getBool("updates.check-on-startup")) {
            updateService.setChannel(
                UpdateChannel.fromName(state.settings.getString("updates.channel", "stable"))
            )
            updateService.check()
        }
    }

    // The live theme flows through every design-system knob: surfaces,
    // accent, typography, display scaling, spacing, corners and animation.
    val theme = remember(state.themeManager.activeThemeId, state.themeManager.revision) {
        state.themeManager.activeTheme
    }
    val animationConfig = ThemeMapper.animationConfig(theme)

    // A subtle settle: right after the color crossfade finishes, the whole
    // window content gives one tiny spring pop so big preset / accent
    // switches feel dimensional. Gated by the same theme-transition toggle
    // and reduced-motion preference — and it never plays on first launch.
    val settleScale = remember { Animatable(1f) }
    val visualKey = buildString {
        append(theme.baseMode)
        append('|').append(theme.colors.primary)
        append('|').append(theme.colors.secondary)
        append('|').append(theme.colors.tertiary)
        append('|').append(theme.colors.background)
        append('|').append(theme.colors.surface)
        append('|').append(theme.colors.border)
        append('|').append(theme.colors.textPrimary)
    }
    val previousVisualKey = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(visualKey) {
        val previous = previousVisualKey.value
        previousVisualKey.value = visualKey
        if (previous == null) {
            // First composition — the app must not animate in.
            settleScale.snapTo(1f)
            return@LaunchedEffect
        }
        val fadeDuration = tweenDuration(animationConfig, ThemeTransitionMillis)
        if (!animationConfig.themeTransitionEnabled || fadeDuration <= 0) {
            settleScale.snapTo(1f)
            return@LaunchedEffect
        }
        // Let the color crossfade play out, then breathe: dip slightly and
        // spring back into place.
        delay(fadeDuration.toLong())
        settleScale.snapTo(0.985f)
        settleScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 340f))
    }

    AppTheme(
        baseMode = ThemeMapper.baseMode(theme),
        accentScheme = ThemeMapper.accentScheme(theme),
        customSurface = ThemeMapper.surfaceColors(theme),
        layoutConfig = ThemeMapper.layoutConfig(theme),
        radiusConfig = ThemeMapper.radiusConfig(theme),
        animationConfig = animationConfig,
        typeScale = ThemeMapper.typeScale(theme),
        typography = ThemeMapper.typography(theme)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = settleScale.value
                    scaleY = settleScale.value
                }
        ) {
            KaiteyoWorkspace(state = state)
            if (onboardingVisible) {
                OnboardingWizard(
                    state = state,
                    onComplete = { showOnboarding = false }
                )
            }
        }
    }
}
