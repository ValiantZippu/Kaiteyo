package ua.syt0r.kanji.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.theming.KaiteyoTheme
import ua.syt0r.kanji.desktop.engine.theming.ThemeMapper
import ua.syt0r.kanji.desktop.engine.theming.ThemePresets
import ua.syt0r.kanji.desktop.ui.workspace.KaiteyoWorkspace
import ua.syt0r.kanji.presentation.common.theme.AllAccentSchemes
import ua.syt0r.kanji.presentation.common.theme.AppTheme
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme

// ============================================
// KAITEYO DESKTOP SUITE — root coordinator
// Owns the single AppState instance, seeds the
// demo deck on first launch, derives the live
// theme from the active theme studio preset and
// mounts the workspace shell + global overlays.
// ============================================

@Composable
fun KaiteyoDesktopSuite() {
    val state = remember { AppState() }

    LaunchedEffect(Unit) {
        state.seedDemoData()
    }

    val theme = remember(state.activeThemeId) {
        ThemePresets.all.firstOrNull { it.id == state.activeThemeId } ?: ThemePresets.default
    }

    AppTheme(
        baseMode = ThemeMapper.baseMode(theme),
        accentScheme = accentForTheme(theme.id)
    ) {
        KaiteyoWorkspace(state = state)
    }
}

/** Maps a theme-studio preset id to the closest built-in accent scheme. */
private fun accentForTheme(themeId: String): KaiteyoAccentScheme = when (themeId) {
    "cotton" -> byName("Cotton Candy")
    "ocean" -> byName("Ocean")
    "forest" -> byName("Forest")
    "material" -> byName("Monochrome")
    else -> byName("Signature Pineapple")
}

private fun byName(name: String): KaiteyoAccentScheme =
    AllAccentSchemes.firstOrNull { it.name == name } ?: AllAccentSchemes.first()
