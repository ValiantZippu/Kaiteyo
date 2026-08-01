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
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.theme_manager.ThemeManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesTheme
import ua.syt0r.kanji.presentation.common.theme.AllAccentSchemes
import ua.syt0r.kanji.presentation.common.theme.AppTheme
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.KaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.ui.Orientation
import ua.syt0r.kanji.presentation.screen.main.MainScreen
import ua.syt0r.kanji.presentation.screen.main.features.DeepLinkHandler

@Composable
fun KaiteyoApp(
    windowSizeClass: WindowSizeClass,
    deepLinkHandler: DeepLinkHandler = koinInject(),
    themeManager: ThemeManager = koinInject()
) {

    val orientation = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> Orientation.Landscape
        else -> Orientation.Portrait
    }

    // themeManager.currentTheme is a compose State<PreferencesTheme>, so 'by' delegate works with import
    val currentPrefTheme: PreferencesTheme by themeManager.currentTheme
    val themeState = remember { KaiteyoThemeState() }

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
    }

    val useDarkTheme = baseMode != BaseMode.Light

    // Update theme state when preference changes
    LaunchedEffect(baseMode) {
        themeState.baseMode = baseMode
    }

    // Use the accent scheme from themeState (allows Appearance Studio changes to persist)
    val accentScheme: KaiteyoAccentScheme = themeState.accentScheme

    CompositionLocalProvider(
        LocalKaiteyoThemeState provides themeState
    ) {
        AppTheme(
            useDarkTheme = useDarkTheme,
            useAmoledTheme = currentPrefTheme == ua.syt0r.kanji.core.user_data.preferences.PreferencesTheme.Amoled,
            orientation = orientation,
            baseMode = baseMode,
            accentScheme = accentScheme
        ) {
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