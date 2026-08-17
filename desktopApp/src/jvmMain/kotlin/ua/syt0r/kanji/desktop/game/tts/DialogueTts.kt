package ua.syt0r.kanji.desktop.game.tts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ua.syt0r.kanji.desktop.game.bridge.GameActivityKind
import ua.syt0r.kanji.desktop.game.bridge.GameBridge
import ua.syt0r.kanji.desktop.game.dialogue.DialogueLine

/**
 * Spoken dialogue (spec §61-62, §91-92). Plays an NPC line's kana reading
 * through Kaiteyo's voice engine ([GameBridge.speakJp]) — the game never
 * touches audio hardware; it asks the bridge to speak and tracks state for
 * the UI (auto-play on line change, replay button, "…" while speaking).
 *
 * Listening is a real learning signal: a fully played line records
 * [GameActivityKind.DialogueListened] so stats stay honest (spec §66-67).
 */
class DialogueTts(private val bridge: GameBridge) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var job: Job? = null

    /** True while a line is being spoken. */
    var speaking by mutableStateOf(false)
        private set

    /** The line currently (or last) being spoken. */
    var speakingLineId by mutableStateOf<String?>(null)
        private set

    /** Whether the last speech attempt actually produced sound. */
    var voiceAvailable by mutableStateOf(true)
        private set

    /** Speak [line] from the start; a new call cuts the previous one off. */
    fun speak(line: DialogueLine) {
        stop()
        val text = dialogueSpeechText(line)
        if (text.isBlank()) return
        speakingLineId = line.id
        job = scope.launch {
            speaking = true
            try {
                // A missing voice/clip must never wedge the UI: any failure
                // degrades to silence, not a stuck "…".
                val spoken = runCatching { bridge.speakJp(text) }.getOrDefault(false)
                voiceAvailable = spoken
                if (spoken) {
                    bridge.recordActivity(
                        GameActivityKind.DialogueListened,
                        "Listened to \"${line.jp.take(40)}\""
                    )
                }
            } finally {
                speaking = false
            }
        }
    }

    /** Cut off any line currently being spoken. */
    fun stop() {
        job?.cancel()
        job = null
        speaking = false
    }

    /** Shut the engine down with the session. */
    fun shutdown() {
        stop()
        scope.cancel()
    }
}

/**
 * The kana that should be spoken for a line: the authored reading when
 * present, otherwise the Japanese text filtered down to kana only (kanji and
 * punctuation fall away — the kana-clip voice can't read kanji).
 */
fun dialogueSpeechText(line: DialogueLine): String {
    val reading = line.reading.ifBlank { line.jp }
    // Hiragana + katakana blocks (incl. dakuten, small kana and ー); kanji
    // and punctuation fall away — the kana-clip voice can't read kanji.
    return reading.filter { char ->
        char in '\u3040'..'\u309F' || char in '\u30A0'..'\u30FF'
    }
}
