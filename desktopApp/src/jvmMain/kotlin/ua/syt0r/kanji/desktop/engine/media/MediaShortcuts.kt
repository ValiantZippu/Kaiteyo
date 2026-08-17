package ua.syt0r.kanji.desktop.engine.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.engine.shortcuts.KeyChord
import java.io.File

// ============================================
// KAITEYO MEDIA SHORTCUTS
// The single source of truth for the media
// workspace hotkeys. Every action has a default
// chord and can be rebound from Media → Settings
// → Keyboard shortcuts; bindings persist in
// ~/.kaiteyo/media-hotkeys.json. MediaEngine
// dispatches pressed keys through this catalog,
// so there is exactly one shortcut system for
// the immersion workspace (the global Shortcut
// Registry keeps app / review / browser keys).
// ============================================

/** A rebindable media workspace action. */
data class MediaAction(
    val id: String,
    val label: String,
    val defaultChord: KeyChord,
    val description: String = ""
)

/** Every media workspace action, with the built-in defaults. */
object MediaActions {

    val all: List<MediaAction> = listOf(
        MediaAction("play-pause", "Play / Pause", KeyChord(" "), "Toggle playback"),
        MediaAction("seek-back", "Seek back", KeyChord("ArrowLeft"), "Seek back by the configured amount"),
        MediaAction("seek-forward", "Seek forward", KeyChord("ArrowRight"), "Seek forward by the configured amount"),
        MediaAction("seek-back-30s", "Seek back 30 s", KeyChord("ArrowLeft", ctrl = true)),
        MediaAction("seek-forward-30s", "Seek forward 30 s", KeyChord("ArrowRight", ctrl = true)),
        MediaAction("cycle-word-back", "Previous word", KeyChord("ArrowLeft", alt = true), "Cycle the selected word backwards"),
        MediaAction("cycle-word-forward", "Next word", KeyChord("ArrowRight", alt = true), "Cycle the selected word forwards"),
        MediaAction("volume-up", "Volume up", KeyChord("ArrowUp")),
        MediaAction("volume-down", "Volume down", KeyChord("ArrowDown")),
        MediaAction("mine", "Mine sentence", KeyChord("a"), "Open the mining dialog for the current subtitle"),
        MediaAction("dictionary", "Toggle dictionary", KeyChord("d"), "Show or hide the dictionary panel"),
        MediaAction("transcript", "Toggle transcript", KeyChord("f"), "Show or hide the subtitle transcript"),
        MediaAction("subtitles", "Toggle subtitles", KeyChord("t"), "Show or hide the subtitle overlay"),
        MediaAction("library", "Toggle library panel", KeyChord("m"), "Show or hide the media library"),
        MediaAction("replay", "Replay subtitle", KeyChord("r"), "Replay the current subtitle"),
        MediaAction("loop", "Loop subtitle", KeyChord("l"), "Loop the current subtitle"),
        MediaAction("set-loop-a", "Set A–B loop point A", KeyChord("u"), "Mark the start of an A–B repeat range"),
        MediaAction("set-loop-b", "Set A–B loop point B", KeyChord("p"), "Mark the end of an A–B repeat range"),
        MediaAction("screenshot", "Capture screenshot", KeyChord("s"), "Capture the current video frame"),
        MediaAction("condensed", "Toggle condensed playback", KeyChord("e"), "Skip unsubtitled sections"),
        MediaAction("capture-audio", "Capture audio clip", KeyChord("c"), "Extract the current subtitle's audio"),
        MediaAction("bookmark", "Add bookmark", KeyChord("b"), "Bookmark the current moment"),
        MediaAction("study-mode", "Toggle study mode", KeyChord("g"), "Count watch time as study time"),
        MediaAction("next", "Play next", KeyChord("n"), "Next item in the queue or series"),
        MediaAction("previous", "Play previous", KeyChord("v"), "Previous item in the queue"),
        MediaAction("next-cue", "Next subtitle", KeyChord("ArrowRight", shift = true), "Jump to the next subtitle cue"),
        MediaAction("prev-cue", "Previous subtitle", KeyChord("ArrowLeft", shift = true), "Jump to the previous subtitle cue"),
        // ---- Rendering / audio transport ---------------------------
        MediaAction("mute", "Mute / unmute", KeyChord("q"), "Toggle audio mute"),
        MediaAction("fullscreen", "Toggle fullscreen", KeyChord("F11"), "Enter or leave fullscreen"),
        MediaAction("subtitle-delay-back", "Subtitle delay −0.5 s", KeyChord("j"), "Shift subtitles earlier"),
        MediaAction("subtitle-delay-reset", "Subtitle delay reset", KeyChord("k"), "Clear the subtitle offset"),
        MediaAction("subtitle-delay-forward", "Subtitle delay +0.5 s", KeyChord("l", shift = true), "Shift subtitles later"),
        MediaAction("speed-down", "Speed down", KeyChord("openbracket"), "Reduce playback speed"),
        MediaAction("speed-up", "Speed up", KeyChord("closebracket"), "Increase playback speed"),
        MediaAction("frame-step-back", "Previous frame", KeyChord("comma"), "Step one frame back where supported"),
        MediaAction("frame-step-forward", "Next frame", KeyChord("period"), "Step one frame forward where supported"),
        MediaAction("chapter-previous", "Previous chapter", KeyChord("pageup"), "Jump to the previous chapter"),
        MediaAction("chapter-next", "Next chapter", KeyChord("pagedown"), "Jump to the next chapter"),
        MediaAction("cycle-display", "Cycle display mode", KeyChord("i"), "Fit / Fill / Crop / Original / Stretch"),
        MediaAction("cycle-aspect", "Cycle aspect ratio", KeyChord("o"), "Cycle the forced aspect ratio preset")
    )

    fun defaultChord(actionId: String): KeyChord =
        all.firstOrNull { it.id == actionId }?.defaultChord ?: KeyChord("")
}

@Serializable
private data class MediaHotkeyDto(val chords: Map<String, String> = emptyMap())

/**
 * Persisted media hotkey bindings. Reads are cheap map lookups; the engine
 * consults this on every key event so rebinding takes effect immediately.
 */
class MediaHotkeys(
    private val file: File = File(System.getProperty("user.home"), ".kaiteyo/media-hotkeys.json")
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val overrides = mutableMapOf<String, String>()

    init {
        load()
    }

    /** The current chord label for an action (rebound or default). */
    fun chordLabel(actionId: String): String = chordFor(actionId).label

    fun chordFor(actionId: String): KeyChord {
        val label = overrides[actionId] ?: return MediaActions.defaultChord(actionId)
        return KeyChord.fromLabel(label) ?: MediaActions.defaultChord(actionId)
    }

    /** Rebind an action; false when the chord is invalid or already in use. */
    fun bind(actionId: String, chord: KeyChord): Boolean {
        if (chord.key.isBlank()) return false
        val takenBy = MediaActions.all.firstOrNull { it.id != actionId && chordFor(it.id) == chord }
        if (takenBy != null) return false
        overrides[actionId] = chord.label
        save()
        return true
    }

    fun reset(actionId: String) {
        overrides.remove(actionId)
        save()
    }

    fun resetAll() {
        overrides.clear()
        save()
    }

    /**
     * Resolve a pressed key (with modifiers) to a media action, honoring
     * user rebinds. Compared canonically via [KeyChord.matches], so modifier
     * order never matters and the key comparison is case-insensitive.
     */
    fun actionForPressed(key: String, ctrl: Boolean, shift: Boolean, alt: Boolean, meta: Boolean): MediaAction? =
        MediaActions.all.firstOrNull { chordFor(it.id).matches(key, ctrl, shift, alt, meta) }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            val dto = json.decodeFromString<MediaHotkeyDto>(file.readText())
            overrides.clear()
            dto.chords.forEach { (id, label) ->
                if (KeyChord.fromLabel(label) != null) overrides[id] = label
            }
        }
    }

    private fun save() {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(MediaHotkeyDto(overrides.toMap())))
        }
    }
}
