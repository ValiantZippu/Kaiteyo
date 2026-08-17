package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ua.syt0r.kanji.desktop.game.bridge.BridgeLookup
import ua.syt0r.kanji.desktop.game.bridge.BridgeMinePayload
import ua.syt0r.kanji.desktop.game.bridge.BridgeToastKind
import ua.syt0r.kanji.desktop.game.bridge.GameActivityKind
import ua.syt0r.kanji.desktop.game.bridge.GameBridge
import ua.syt0r.kanji.desktop.game.dialogue.DialogueLine
import ua.syt0r.kanji.desktop.game.tts.DialogueTts
import ua.syt0r.kanji.desktop.game.tts.dialogueSpeechText

/**
 * Spoken dialogue (spec §61-62): kana extraction for the clip voice, and the
 * bridge speech flow — a spoken line records a real listening signal.
 */
class DialogueTtsTest {

    private class RecordingBridge : GameBridge {
        var spoken: String? = null
        val activities = mutableListOf<GameActivityKind>()

        override fun lookup(headword: String): BridgeLookup? = null
        override fun hasStudyMaterialFor(headword: String): Boolean = false
        override fun mine(payload: BridgeMinePayload): Boolean = false
        override fun speakJp(kanaText: String): Boolean {
            spoken = kanaText
            return true
        }

        override fun recordActivity(kind: GameActivityKind, detail: String) {
            activities.add(kind)
        }

        override fun toast(message: String, kind: BridgeToastKind) {}
        override fun getSetting(key: String, default: String): String = default
        override fun setSetting(key: String, value: String) {}
    }

    // ------------------------------------------------------------
    // Kana extraction (pure)
    // ------------------------------------------------------------

    @Test
    fun `reading is spoken as-is when present`() {
        val line = DialogueLine(id = "l", jp = "駅はどこですか", reading = "えきはどこですか")
        assertEquals("えきはどこですか", dialogueSpeechText(line))
    }

    @Test
    fun `blank reading falls back to kana inside the japanese text`() {
        // Kanji (駅) and punctuation (？) fall away; kana stays.
        val line = DialogueLine(id = "l", jp = "駅はどこですか？")
        assertEquals("はどこですか", dialogueSpeechText(line))
    }

    @Test
    fun `katakana is preserved for the clip voice`() {
        val line = DialogueLine(id = "l", jp = "ジュース", reading = "ジュース")
        assertEquals("ジュース", dialogueSpeechText(line))
    }

    @Test
    fun `empty reading yields empty speech text`() {
        val line = DialogueLine(id = "l", jp = "abc", reading = "")
        assertEquals("", dialogueSpeechText(line))
    }

    // ------------------------------------------------------------
    // Speech flow through the bridge
    // ------------------------------------------------------------

    @Test
    fun `speak plays the line and records listening activity`() = runBlocking {
        val bridge = RecordingBridge()
        val tts = DialogueTts(bridge)
        val line = DialogueLine(id = "l1", jp = "こんにちは", reading = "こんにちは")

        tts.speak(line)
        // The job runs on its own scope — wait until the bridge was actually
        // asked to speak (the only writer of [bridge.spoken]).
        withTimeout(2_000) {
            while (bridge.spoken == null) delay(10)
        }

        assertEquals("こんにちは", bridge.spoken)
        assertTrue(bridge.activities.contains(GameActivityKind.DialogueListened))
        assertTrue(tts.voiceAvailable)
        assertFalse(tts.speaking)
        tts.shutdown()
    }

    @Test
    fun `silent voice does not fabricate listening time`() = runBlocking {
        val bridge = object : RecordingBridge() {
            override fun speakJp(kanaText: String): Boolean = false
        }
        val tts = DialogueTts(bridge)
        tts.speak(DialogueLine(id = "l", jp = "こんにちは", reading = "こんにちは"))
        // The job flips voiceAvailable to false only when it has finished a
        // silent line — a reliable completion marker.
        withTimeout(2_000) {
            while (tts.voiceAvailable) delay(10)
        }

        assertFalse(bridge.activities.contains(GameActivityKind.DialogueListened))
        assertFalse(tts.voiceAvailable)
        tts.shutdown()
    }
}
