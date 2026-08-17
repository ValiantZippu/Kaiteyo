package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.audio.Sfx
import ua.syt0r.kanji.desktop.game.audio.Tone
import ua.syt0r.kanji.desktop.game.audio.Wave
import ua.syt0r.kanji.desktop.game.audio.shortToBytes
import ua.syt0r.kanji.desktop.game.audio.synthTone

/**
 * Game audio (spec §91-92): the pure synthesis core — sample length,
 * envelope (no clicks), volume clamping and PCM encoding. The mixer itself
 * needs an audio device, so only the math is unit-tested.
 */
class GameAudioTest {

    @Test
    fun `tone length matches duration times sample rate`() {
        val samples = synthTone(frequency = 440f, seconds = 0.25f, volume = 0.5f, sampleRate = 1000)
        assertEquals(250, samples.size)
    }

    @Test
    fun `tone decays so notes never click`() {
        val samples = synthTone(frequency = 440f, seconds = 0.5f, volume = 1f, sampleRate = 1000)
        val first = kotlin.math.abs(samples.first().toInt())
        val last = kotlin.math.abs(samples.last().toInt())
        assertTrue(first > 0, "first sample should be audible")
        assertTrue(last < first, "last sample should have decayed (got $last vs $first)")
    }

    @Test
    fun `silent volume produces silence`() {
        val samples = synthTone(frequency = 440f, seconds = 0.1f, volume = 0f, sampleRate = 1000)
        assertTrue(samples.all { it.toInt() == 0 })
    }

    @Test
    fun `zero duration yields empty output`() {
        assertEquals(0, synthTone(frequency = 440f, seconds = 0f, volume = 1f).size)
    }

    @Test
    fun `pcm encoding is 16-bit little endian`() {
        val bytes = shortToBytes(shortArrayOf(0x1234.toShort(), (-2).toShort()))
        assertEquals(4, bytes.size)
        assertEquals(0x34, bytes[0].toInt() and 0xFF)
        assertEquals(0x12, bytes[1].toInt() and 0xFF)
        assertEquals(0xFE, bytes[2].toInt() and 0xFF)
        assertEquals(0xFF, bytes[3].toInt() and 0xFF)
    }

    @Test
    fun `effects are non-empty tone sequences`() {
        for (effect in Sfx.entries) {
            assertTrue(effect.tones.isNotEmpty(), "${effect.name} should have tones")
        }
    }

    @Test
    fun `tone data defaults are sane`() {
        val tone = Tone(frequency = 440f, durationSeconds = 0.1f)
        assertEquals(Wave.Sine, tone.wave)
        assertEquals(0.5f, tone.volume)
        assertEquals(0f, tone.gapSeconds)
    }
}
