package ua.syt0r.kanji.desktopApp

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.ui.media.MediaView
import ua.syt0r.kanji.presentation.screen.main.screen.media.MediaCentreContent

// ============================================
// MEDIA CENTRE — DESKTOP HOST
// The shipped desktop app (Main.kt) registers
// this as the real MediaCentreContent: it mounts
// the desktop suite's MediaView with its own
// AppState (media library, player backends,
// subtitles, dictionary and mining all live
// there). The suite's design system reads the
// same theme CompositionLocals as the core app,
// so the Media Centre inherits the active theme.
//
// Immersion hotkeys (space, arrows, F11, …) are
// forwarded to the media engine while this host
// has focus, mirroring what the standalone suite
// does at the workspace level.
// ============================================

object DesktopMediaCentreContent : MediaCentreContent {

    @Composable
    override fun Content(onClose: () -> Unit) {
        val state = remember { AppState() }
        Box(
            Modifier
                .fillMaxSize()
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    state.media.handleKey(
                        key = mediaKeyName(event.key),
                        ctrl = event.isCtrlPressed,
                        shift = event.isShiftPressed,
                        alt = event.isAltPressed,
                        meta = event.isMetaPressed
                    )
                }
        ) {
            MediaView(state = state, onBack = onClose)
        }
    }

    /** Normalize a [Key] to the string format used by the media hotkey catalog. */
    private fun mediaKeyName(key: Key): String = when (key) {
        Key.Zero -> "0"; Key.One -> "1"; Key.Two -> "2"; Key.Three -> "3"
        Key.Four -> "4"; Key.Five -> "5"; Key.Six -> "6"; Key.Seven -> "7"
        Key.Eight -> "8"; Key.Nine -> "9"
        Key.A -> "a"; Key.B -> "b"; Key.C -> "c"; Key.D -> "d"; Key.E -> "e"; Key.F -> "f"
        Key.G -> "g"; Key.H -> "h"; Key.I -> "i"; Key.J -> "j"; Key.K -> "k"; Key.L -> "l"
        Key.M -> "m"; Key.N -> "n"; Key.O -> "o"; Key.P -> "p"; Key.Q -> "q"; Key.R -> "r"
        Key.S -> "s"; Key.T -> "t"; Key.U -> "u"; Key.V -> "v"; Key.W -> "w"; Key.X -> "x"
        Key.Y -> "y"; Key.Z -> "z"
        Key.Spacebar -> " "
        Key.Enter -> "enter"
        Key.Comma -> "comma"
        Key.Slash -> "/"
        Key.Delete -> "delete"
        Key.Backspace -> "backspace"
        Key.Escape -> "escape"
        Key.Tab -> "tab"
        else -> key.toString()
    }
}
