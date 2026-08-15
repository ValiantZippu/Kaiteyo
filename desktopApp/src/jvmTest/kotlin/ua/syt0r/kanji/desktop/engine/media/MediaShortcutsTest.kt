package ua.syt0r.kanji.desktop.engine.media

import ua.syt0r.kanji.desktop.engine.shortcuts.KeyChord
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaShortcutsTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = createTempDir("kaiteyo-hotkeys-test")
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun hotkeys(): MediaHotkeys =
        MediaHotkeys(File(tempDir, "hotkeys.json"))

    @Test
    fun `defaults resolve the built-in chords`() {
        val h = hotkeys()
        assertEquals(" ", h.chordLabel("play-pause"))
        assertEquals("A", h.chordLabel("mine"))
        assertEquals("Ctrl+ArrowLeft", h.chordLabel("seek-back-30s"))
        assertEquals("Shift+ArrowRight", h.chordLabel("next-cue"))
    }

    @Test
    fun `pressed keys resolve to actions with and without modifiers`() {
        val h = hotkeys()
        assertEquals("mine", h.actionForPressed("a", false, false, false, false)?.id)
        assertEquals("seek-back-30s", h.actionForPressed("ArrowLeft", true, false, false, false)?.id)
        assertEquals("cycle-word-forward", h.actionForPressed("ArrowRight", false, false, true, false)?.id)
        assertEquals("next-cue", h.actionForPressed("ArrowRight", false, true, false, false)?.id)
        assertNull(h.actionForPressed("k", true, false, false, false))
        assertNull(h.actionForPressed("a", true, false, false, false)) // Ctrl+A is not bound
    }

    @Test
    fun `rebinding takes effect immediately and persists`() {
        val h = hotkeys()
        assertTrue(h.bind("mine", KeyChord("x")))
        assertEquals("mine", h.actionForPressed("x", false, false, false, false)?.id)
        assertNull(h.actionForPressed("a", false, false, false, false))

        // A second instance (same file) reads the persisted binding.
        val reloaded = MediaHotkeys(File(tempDir, "hotkeys.json"))
        assertEquals("mine", reloaded.actionForPressed("x", false, false, false, false)?.id)
        assertNull(reloaded.actionForPressed("a", false, false, false, false))
    }

    @Test
    fun `conflicting chords are rejected`() {
        val h = hotkeys()
        assertTrue(h.bind("mine", KeyChord("x")))
        assertFalse(h.bind("replay", KeyChord("x")))
        assertTrue(h.bind("replay", KeyChord("y")))
    }

    @Test
    fun `reset restores the default chord`() {
        val h = hotkeys()
        h.bind("mine", KeyChord("x"))
        h.reset("mine")
        assertEquals("mine", h.actionForPressed("a", false, false, false, false)?.id)
    }

    @Test
    fun `every catalog action has a unique default chord`() {
        val chords = MediaActions.all.map { it.defaultChord }
        assertEquals(chords.size, chords.distinct().size, "default chords must be unique")
        chords.forEach { assertFalse(it.key.isBlank(), "no action may bind an empty key") }
        assertNotNull(MediaActions.defaultChord("play-pause"))
    }

    @Test
    fun `rendering and transport extras resolve their defaults`() {
        val h = hotkeys()
        assertEquals("mute", h.actionForPressed("q", false, false, false, false)?.id)
        assertEquals("fullscreen", h.actionForPressed("f11", false, false, false, false)?.id)
        assertEquals("subtitle-delay-back", h.actionForPressed("j", false, false, false, false)?.id)
        assertEquals("subtitle-delay-reset", h.actionForPressed("k", false, false, false, false)?.id)
        assertEquals("subtitle-delay-forward", h.actionForPressed("l", false, true, false, false)?.id)
        assertEquals("speed-down", h.actionForPressed("openbracket", false, false, false, false)?.id)
        assertEquals("speed-up", h.actionForPressed("closebracket", false, false, false, false)?.id)
        assertEquals("frame-step-back", h.actionForPressed("comma", false, false, false, false)?.id)
        assertEquals("frame-step-forward", h.actionForPressed("period", false, false, false, false)?.id)
        assertEquals("chapter-previous", h.actionForPressed("pageup", false, false, false, false)?.id)
        assertEquals("chapter-next", h.actionForPressed("pagedown", false, false, false, false)?.id)
        assertEquals("cycle-display", h.actionForPressed("i", false, false, false, false)?.id)
        assertEquals("cycle-aspect", h.actionForPressed("o", false, false, false, false)?.id)
    }

    @Test
    fun `new tuning chords rebind cleanly`() {
        val h = hotkeys()
        assertTrue(h.bind("mute", KeyChord("x")))
        assertNull(h.actionForPressed("q", false, false, false, false))
        assertEquals("mute", h.actionForPressed("x", false, false, false, false)?.id)
        // Original default is now free for another action.
        assertTrue(h.bind("cycle-display", KeyChord("q")))
    }
}
