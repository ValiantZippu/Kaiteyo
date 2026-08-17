package ua.syt0r.kanji.desktop.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.bridge.KaiteyoBridge
import ua.syt0r.kanji.desktop.game.content.WorldContentLoader
import ua.syt0r.kanji.desktop.game.ui.hud.GameHud
import ua.syt0r.kanji.desktop.game.ui.menus.GameMenuHost
import ua.syt0r.kanji.desktop.game.ui.TouchControlsOverlay
import ua.syt0r.kanji.desktop.game.ui.panels.DialoguePanel
import ua.syt0r.kanji.desktop.game.ui.panels.KnowledgeDiscoveryPanel
import ua.syt0r.kanji.desktop.game.ui.panels.PhotoModeOverlay
import ua.syt0r.kanji.desktop.game.ui.panels.QuestCompletePanel
import ua.syt0r.kanji.desktop.game.ui.panels.TravelPanel
import ua.syt0r.kanji.desktop.game.ui.panels.WritingActivityPanel
import ua.syt0r.kanji.desktop.game.ui.panels.OrderPanel
import ua.syt0r.kanji.desktop.game.ui.panels.PhotoDetailPanel
import ua.syt0r.kanji.desktop.game.ui.panels.DebugOverlayPanel

/**
 * The Game workspace view. One [GameSession] per composition — the whole
 * world, the player, quests, learning, photography and save live there; the
 * UI is a thin layer over it (spec §103-105).
 */
@Composable
fun GameView(
    state: AppState,
    // When mounted inside the shipped app (DesktopGameCentreContent) the
    // workspace the suite would navigate to does not exist — the host
    // supplies the real "exit" action (pop the Game destination) instead.
    onExitToKaiteyoOverride: (() -> Unit)? = null
) {
    val session = remember {
        GameSession(
            bridge = KaiteyoBridge(state),
            content = WorldContentLoader.load()
        )
    }

    // Save the journey when leaving the Game workspace.
    DisposableEffect(session) {
        onDispose {
            session.quitToKaiteyo()
        }
    }

    // Deep-link a discovery back into Kaiteyo's dictionary (spec §63, §108).
    val onOpenInKaiteyo: (String) -> Unit = { headword ->
        session.quitToKaiteyo()
        state.dictionary.query = headword
        state.currentView = ua.syt0r.kanji.desktop.appstate.WorkspaceView.Dictionary
    }
    val onExitToKaiteyo: () -> Unit = {
        session.quitToKaiteyo()
        if (onExitToKaiteyoOverride != null) onExitToKaiteyoOverride()
        else state.currentView = ua.syt0r.kanji.desktop.appstate.WorkspaceView.Dashboard
    }

    Box(Modifier.fillMaxSize()) {
        GameCanvas(session, Modifier.fillMaxSize())
        // Touch layer sits directly on the canvas so dialogue/menus above it
        // keep winning their own taps (spec §16).
        TouchControlsOverlay(session)
        GameHud(session)
        PhotoModeOverlay(session)
        DialoguePanel(session)
        KnowledgeDiscoveryPanel(session, onOpenInKaiteyo)
        QuestCompletePanel(session)
        TravelPanel(session)
        WritingActivityPanel(session)
        OrderPanel(session)
        GameMenuHost(session, onOpenInKaiteyo, onExitToKaiteyo)
        PhotoDetailPanel(session)
        DebugOverlayPanel(session)
    }
}
