package ua.syt0r.kanji.ios

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.compose.koinInject
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController
import ua.syt0r.kanji.core.theme_manager.ThemeManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesTheme
import ua.syt0r.kanji.presentation.KaiteyoApp

@OptIn(ExperimentalForeignApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
fun MainViewController(): UIViewController = ComposeUIViewController {

    UpdateTheme()

    KaiteyoApp(
        windowSizeClass = calculateWindowSizeClass()
    )

}

@Composable
private fun UpdateTheme() {
    val controller = LocalUIViewController.current
    val themeManager = koinInject<ThemeManager>()
    LaunchedEffect(Unit) {
        snapshotFlow { themeManager.currentTheme.value }.collect {
            val style = when (it) {
                PreferencesTheme.System -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
                PreferencesTheme.Light, PreferencesTheme.Sepia,
                PreferencesTheme.Cream, PreferencesTheme.Paper -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
                PreferencesTheme.Dark, PreferencesTheme.Amoled,
                PreferencesTheme.Midnight -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
            }
            controller.overrideUserInterfaceStyle = style
            controller.setNeedsStatusBarAppearanceUpdate()
        }
    }
}
