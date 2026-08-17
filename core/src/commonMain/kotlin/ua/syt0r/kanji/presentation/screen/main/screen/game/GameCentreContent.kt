package ua.syt0r.kanji.presentation.screen.main.screen.game

import androidx.compose.runtime.Composable
import org.koin.dsl.module
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

// ============================================
// KAITEYO WORLD — CORE DESTINATION CONTRACT
// The Game destination renders platform-specific
// content. The desktop app supplies the real
// implementation through this contract (the
// exploration game mounted with its own AppState,
// mirroring how the Media Centre is folded in);
// every other platform gets the core node-based
// curriculum game. This keeps Game a first-class
// navigation destination without dead links.
// ============================================

/** Renders the platform's Kaiteyo World content. Implemented by the desktop app. */
fun interface GameCentreContent {
    @Composable
    fun Content(navigationState: MainNavigationState, onClose: () -> Unit)
}

/**
 * Default implementation for all platforms: the core node-based game that
 * runs on top of the user's real study state (kanji nodes auto-track against
 * the SRS; kana/vocab nodes are completed explicitly).
 */
object CoreGameContent : GameCentreContent {

    @Composable
    override fun Content(navigationState: MainNavigationState, onClose: () -> Unit) {
        GameScreen(navigationState = navigationState)
    }
}

val gameCentreModule = module {
    single<GameCentreContent> { CoreGameContent }
}
