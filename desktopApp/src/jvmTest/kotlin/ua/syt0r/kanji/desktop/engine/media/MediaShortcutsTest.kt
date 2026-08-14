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
}
