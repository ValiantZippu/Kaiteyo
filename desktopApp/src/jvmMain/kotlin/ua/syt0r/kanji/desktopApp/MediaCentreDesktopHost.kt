package ua.syt0r.kanji.desktopApp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.japanese.isHiragana
import ua.syt0r.kanji.core.japanese.isKanji
import ua.syt0r.kanji.core.japanese.isKatakana
import ua.syt0r.kanji.core.knowledge.media.MediaReference
import ua.syt0r.kanji.core.knowledge.media.MediaReferenceKind
import ua.syt0r.kanji.core.knowledge.media.MediaReferenceStore
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.ui.media.MediaView
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.media.DefaultMediaCentreContent
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
    override fun Content(navigationState: MainNavigationState?, onClose: () -> Unit) {
        // The workspace AppState is heavy and its construction can fail on
        // unusual machines (a corrupt settings/state file, a locked file, a
        // missing playback backend, …). The Media Centre must NEVER take the
        // whole application down (spec §40, §27, §31): a failure here is
        // surfaced as a controlled, explainable, retryable error state inside
        // this destination — never a crash. AppState() is plain Kotlin
        // construction, so runCatching genuinely captures init failures.
        var attempt by remember { mutableStateOf(0) }
        val stateResult = remember(attempt) { runCatching { AppState() } }
        val state = stateResult.getOrNull()
        val scope = rememberCoroutineScope()
        val mediaReferenceStore = koinInject<MediaReferenceStore>()

        if (state == null) {
            // Fall back to the core multiplatform Media Centre instead of showing
            // a blank error screen — the user always sees a working media experience.
            Box(Modifier.fillMaxSize().background(surfaceColors().background)) {
                DefaultMediaCentreContent.Content(
                    navigationState = navigationState,
                    onClose = onClose
                )
            }
            return
        }

        // Knowledge ⇄ media bridge (spec §28): every bookmark whose label is
        // Japanese (a subtitle cue) is recorded into the core media-reference
        // store, so word/kanji entries show real "Found in your media" rows.
        LaunchedEffect(Unit) {
            state.media.onBookmarkCreated = { bookmark ->
                val text = bookmark.label.trim()
                if (text.any { it.isKanji() || it.isHiragana() || it.isKatakana() }) {
                    scope.launch {
                        mediaReferenceStore.record(
                            MediaReference(
                                kind = MediaReferenceKind.Bookmark,
                                title = bookmark.mediaPath.substringAfterLast('/').substringAfterLast('\\'),
                                text = text,
                                timestampMs = bookmark.timestampMs,
                                recordedAt = Clock.System.now().toEpochMilliseconds()
                            )
                        )
                    }
                }
            }
            // Mined cues carry the REAL desktop card id — record it so the
            // node layer builds a real mined_from edge (ADR-0013, §149).
            state.media.onMined = { event ->
                val text = event.cueText.trim()
                if (text.any { it.isKanji() || it.isHiragana() || it.isKatakana() }) {
                    scope.launch {
                        mediaReferenceStore.record(
                            MediaReference(
                                kind = MediaReferenceKind.Mined,
                                title = event.mediaName,
                                text = text,
                                timestampMs = event.timestampMs,
                                recordedAt = Clock.System.now().toEpochMilliseconds(),
                                cardId = event.cardId
                            )
                        )
                    }
                }
            }
        }

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
    /**
     * Graceful, themed error state for the Media destination. Rendered when
     * the workspace cannot be constructed so the failure is contained to this
     * screen (the app keeps running) and the user gets a real reason plus a
     * Retry instead of a crash (spec §27, §31).
     */
    @Composable
    private fun MediaInitError(reason: String, onRetry: () -> Unit, onClose: () -> Unit) {
        val sc = surfaceColors()
        Box(
            Modifier
                .fillMaxSize()
                .background(sc.background)
                .padding(DsSpacing.Xl),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                Text(
                    text = "Media Centre couldn't start",
                    color = sc.textPrimary,
                    fontSize = DsType.Heading,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = reason,
                    color = sc.textMuted,
                    fontSize = DsType.Body,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    DsButton(text = "Retry", onClick = onRetry)
                    DsButton(text = "Back", kind = DsButtonKind.Ghost, onClick = onClose)
                }
            }
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
