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
import ua.syt0r.kanji.desktop.engine.updates.kjd.KjdDatabaseUpdater
import ua.syt0r.kanji.desktop.ui.tutorial.TutorialOverlay
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
fun KaiteyoDesktopSuite(
    // Optional window shell mounted *inside* the suite's AppTheme, so the
    // custom title bar and chrome render with the suite's live theme (never
    // the untinted OLED default). Defaults to the identity.
    shell: @Composable (content: @Composable () -> Unit) -> Unit = { content -> content() }
) {
    val state = remember { AppState() }

    // First-run gating: show onboarding when it has never been completed,
    // or the moment the user re-requests it from Settings. Reading
    // onboardingRequested here subscribes to the AppState value, so a
    // request from Settings shows the wizard immediately.
    var showOnboarding by remember { mutableStateOf(!state.onboardingCompleted) }
    val onboardingRequested = state.onboardingRequested
    val onboardingVisible = showOnboarding || onboardingRequested

    // Quiet update checks at startup when the user opted in (Settings → Updates):
    // the app binary feed and the KJD language database patch feed.
    val updateService = koinInject<UpdateService>()
    val kjdUpdater = koinInject<KjdDatabaseUpdater>()

    LaunchedEffect(Unit) {
        // First run starts empty by design — no demo data is ever seeded into
        // the user library; study content is earned through real activity.
        if (state.settings.getBool("updates.check-on-startup")) {
            updateService.setChannel(
                UpdateChannel.fromName(state.settings.getString("updates.channel", "stable"))
            )
            updateService.check()
        }

        // KJD language data: download + apply patches to the bundled database.
        // Mirrors the recorded applied state into Settings so the Updates
        // section can surface it, then runs the quiet check/apply pipeline.
        if (state.settings.getBool("updates.kjd-check-on-startup")) {
            val updater = kjdUpdater
            updater.onChecked = { checkedAt ->
                state.settings.setString("updates.kjd-last-checked", checkedAt)
            }
            updater.onApplied = { applied ->
                state.settings.setString("updates.kjd-applied-version", applied.databaseVersion)
                state.settings.setString("updates.kjd-applied-fingerprint", applied.fingerprint)
                state.settings.setString("updates.kjd-last-checked", applied.appliedAt)
            }
            updater.checkOnStartup(
                state.settings.getString("updates.kjd-channel", "stable")
            )
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
        shell {
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
                // Product tutorial — opened from Settings → General, layered
                // above the workspace exactly like the onboarding wizard.
                if (state.tutorialRequested) {
                    TutorialOverlay(
                        state = state,
                        onClose = { state.tutorialRequested = false }
                    )
                }
            }
        }
    }
}
