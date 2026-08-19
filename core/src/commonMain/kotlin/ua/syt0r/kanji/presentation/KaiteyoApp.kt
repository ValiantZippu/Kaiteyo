package ua.syt0r.kanji.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.theme_manager.ThemeManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesTheme
import ua.syt0r.kanji.presentation.common.theme.AppTheme
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.isDarkMode
import ua.syt0r.kanji.presentation.common.theme.KaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalThemeSettingsState
import ua.syt0r.kanji.presentation.common.theme.ThemeSettings
import ua.syt0r.kanji.presentation.common.theme.ThemeSettingsState
import ua.syt0r.kanji.presentation.common.ui.Orientation
import ua.syt0r.kanji.presentation.screen.main.MainScreen
import ua.syt0r.kanji.presentation.screen.main.features.DeepLinkHandler

/**
 * Root composable for the Kaiteyo application.
 *
 * Sets up the theme system (dark/light/AMOLED, accent scheme, orientation)
 * and renders the [MainScreen] with deep-link handling.
 *
 * @param windowSizeClass The current window size class, used to determine layout orientation.
 * @param deepLinkHandler Handles incoming deep links (e.g. `kaiteyo://` scheme navigation).
 * @param themeManager Provides the current theme preference and persists theme changes.
 * @param shell Optional desktop shell mounted *inside* the theme root, so window
 *   chrome (title bar, controls, window surface) renders with the same theme
 *   tokens as the content — never an untinted default. Defaults to the identity
 *   and is unused on mobile.
 */
@Composable
fun KaiteyoApp(
    windowSizeClass: WindowSizeClass,
    deepLinkHandler: DeepLinkHandler = koinInject(),
    themeManager: ThemeManager = koinInject(),
    shell: @Composable (content: @Composable () -> Unit) -> Unit = { content -> content() }
) {
    KaiteyoThemeRoot(
        windowSizeClass = windowSizeClass,
        themeManager = themeManager
    ) {
        shell {
            Surface {
                Box(
                    modifier = Modifier.safeDrawingPadding()
                ) {
                    MainScreen(deepLinkHandler)
                }
            }
        }
    }
}

/**
 * Single theme root for the Kaiteyo application.
 *
 * Owns the live [KaiteyoThemeState], restores and persists the appearance
 * configuration (accent, radius, density, motion, typography) and provides the
 * theme to everything composed beneath it. The desktop window shell is mounted
 * through [KaiteyoApp.shell], so its chrome participates in the same animated
 * theme — Theme Studio edits reach the window surface live.
 */
@Composable
fun KaiteyoThemeRoot(
    windowSizeClass: WindowSizeClass,
    themeManager: ThemeManager = koinInject(),
    content: @Composable () -> Unit
) {

    val orientation = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> Orientation.Landscape
        else -> Orientation.Portrait
    }

    // themeManager.currentTheme is a compose State<PreferencesTheme>, so 'by' delegate works with import
    val currentPrefTheme: PreferencesTheme by themeManager.currentTheme
    val themeState = remember { KaiteyoThemeState() }

    // Persisted appearance configuration (accent, radius, density, motion,
    // typography). Restored on launch and kept in sync with KaiteyoThemeState
    // so changes from the Settings Center and the Theme Studio both persist.
    val themeSettingsState = koinInject<ThemeSettingsState>()

    // Apply the persisted configuration to the live theme state (and re-apply
    // whenever it changes, e.g. from the Settings Center).
    LaunchedEffect(themeSettingsState.settings) {
        themeSettingsState.applyTo(themeState)
    }

    // Persist any change made to KaiteyoThemeState anywhere in the app (the
    // Theme Studio mutates it directly). The equality guard prevents loops.
    LaunchedEffect(Unit) {
        snapshotFlow { ThemeSettings.from(themeState) }
            .drop(1)
            .distinctUntilChanged()
            .collect { themeSettingsState.accept(it) }
    }

    // Map PreferencesTheme to BaseMode
    val baseMode: BaseMode = when (currentPrefTheme) {
        PreferencesTheme.System -> {
            @Suppress("DEPRECATION")
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            if (isDark) BaseMode.Dark else BaseMode.Light
        }
        PreferencesTheme.Light -> BaseMode.Light
        PreferencesTheme.Dark -> BaseMode.Dark
        PreferencesTheme.Amoled -> BaseMode.Oled
        PreferencesTheme.Sepia -> BaseMode.Sepia
        PreferencesTheme.Cream -> BaseMode.Cream
        PreferencesTheme.Paper -> BaseMode.Paper
        PreferencesTheme.Midnight -> BaseMode.Midnight
    }

    val useDarkTheme = baseMode.isDarkMode

    // Update theme state when preference changes
    LaunchedEffect(baseMode) {
        themeState.baseMode = baseMode
    }

    // Use the accent scheme from themeState (allows Appearance Studio changes to persist)
    val accentScheme: KaiteyoAccentScheme = themeState.accentScheme

    CompositionLocalProvider(
        LocalKaiteyoThemeState provides themeState,
        LocalThemeSettingsState provides themeSettingsState
    ) {
        AppTheme(
            useDarkTheme = useDarkTheme,
            useAmoledTheme = currentPrefTheme == PreferencesTheme.Amoled,
            orientation = orientation,
            baseMode = baseMode,
            accentScheme = accentScheme,
            // Feed the live Theme Studio state through so Motion / Layout /
            // Glow / Radius edits apply app-wide instantly (and persist via
            // the snapshotFlow below). Previously only accent + type scale
            // reached AppTheme, which made those studio tabs cosmetic.
            animationConfig = themeState.animationConfig,
            radiusConfig = themeState.radiusConfig,
            glowConfig = themeState.glowConfig,
            layoutConfig = themeState.layoutConfig,
            typeScale = themeState.typeScale
        ) {
            content()
        }
    }
}
