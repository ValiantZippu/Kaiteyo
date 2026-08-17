package ua.syt0r.kanji.desktopApp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.game.ui.GameView
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.game.GameCentreContent

// ============================================
// KAITEYO WORLD — DESKTOP HOST
// The shipped desktop app (Main.kt) registers
// this as the real GameCentreContent: it mounts
// the exploration game with its own AppState
// (world, player, quests, learning, photography,
// save — everything lives in the GameSession the
// view owns). The suite's design system reads the
// same theme CompositionLocals as the core app,
// so the game inherits the active Kaiteyo theme.
//
// "Exit to Kaiteyo" inside the game routes back
// through the app's navigation (onClose pops the
// Game destination); leaving the destination by
// any other path saves the journey via the view's
// onDispose.
// ============================================

object DesktopGameCentreContent : GameCentreContent {

    @Composable
    override fun Content(navigationState: MainNavigationState, onClose: () -> Unit) {
        val state = remember { AppState() }
        GameView(state = state, onExitToKaiteyoOverride = onClose)
    }
}
