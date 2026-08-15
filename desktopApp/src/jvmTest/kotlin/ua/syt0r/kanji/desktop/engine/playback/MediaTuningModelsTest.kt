package ua.syt0r.kanji.desktop.engine.playback

import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pure-logic tests for the video/audio tuning models added with the media panel. */
class MediaTuningModelsTest {

    // ------------------------------------------------------------
    // Equalizer presets
    // ------------------------------------------------------------

    @Test
    fun everyPresetHasTenBandsWithinRange() {
        EqualizerPreset.entries.forEach { preset ->
            assertEquals(10, preset.bandsDb.size, "${preset.label} must define 10 bands")
            preset.bandsDb.forEach { band ->
                assertTrue(band in -20f..20f, "${preset.label} band out of range: $band")
            }
            assertTrue(preset.preampDb in -20f..20f, "${preset.label} preamp out of range")
        }
    }

    @Test
    fun flatPresetIsAllZeroAndInactive() {
        val flat = EqualizerPreset.Flat
        assertTrue(flat.bandsDb.all { it == 0f })
        assertEquals(0f, flat.preampDb)
        assertFalse(EqualizerSettings(preset = flat).active)
    }

    @Test
    fun nonFlatPresetIsActive() {
        assertTrue(EqualizerSettings(preset = EqualizerPreset.Rock).active)
        assertTrue(EqualizerSettings(preset = EqualizerPreset.Techno).active)
    }

    @Test
    fun presetLookupByLabel() {
        assertEquals(EqualizerPreset.Rock, EqualizerPreset.fromLabel("Rock"))
        assertEquals(EqualizerPreset.FullBass, EqualizerPreset.fromLabel("Full Bass"))
        assertEquals(EqualizerPreset.Flat, EqualizerPreset.fromLabel("Does not exist"))
    }

    @Test
    fun customBandEditsDoNotMutateSharedPreset() {
        val base = EqualizerSettings(preset = EqualizerPreset.Club)
        val edited = base.withBand(2, 12f)
        assertEquals(12f, edited.bandsDb[2])
        // The preset itself is untouched — repeated applies are stable.
        assertEquals(base.bandsDb, EqualizerSettings(preset = EqualizerPreset.Club).bandsDb)
        assertEquals(10, edited.normalizedBands().size)
    }

    @Test
    fun bandEditClampsToRange() {
        val settings = EqualizerSettings(preset = EqualizerPreset.Flat).withBand(0, 99f)
        assertEquals(20f, settings.bandsDb[0])
        val low = settings.withBand(0, -99f)
        assertEquals(-20f, low.bandsDb[0])
    }

    // ------------------------------------------------------------
    // Video adjustments
    // ------------------------------------------------------------

    @Test
    fun neutralDefaultsAndReset() {
        val neutral = VideoAdjustments()
        assertTrue(neutral.neutral())
        assertTrue(VideoAdjustments().withBrightness(150f).withBrightness(100f).neutral())
    }

    @Test
    fun adjustmentSettersClampRanges() {
        val a = VideoAdjustments()
        assertEquals(200f, a.withBrightness(500f).brightness)
        assertEquals(0f, a.withContrast(-40f).contrast)
        assertEquals(180f, a.withHue(999f).hue)
        assertEquals(-180f, a.withHue(-999f).hue)
        assertTrue(a.withDeinterlace(true).deinterlace)
        assertFalse(a.withDeinterlace(true).withDeinterlace(false).deinterlace)
    }

    @Test
    fun anyNonNeutralFieldBreaksNeutral() {
        val a = VideoAdjustments()
        assertFalse(a.withSaturation(130f).neutral())
        assertFalse(a.withGamma(70f).neutral())
        assertFalse(a.withHue(15f).neutral())
        assertFalse(a.withDeinterlace(true).neutral())
    }

    // ------------------------------------------------------------
    // Display modes / aspect ratios
    // ------------------------------------------------------------

    @Test
    fun displayModesAreComplete() {
        // The cycle hotkey advances through every mode — make sure none is
        // lost and the labels match the settings options.
        val labels = VideoDisplayMode.entries.map { it.label }
        assertEquals(listOf("Fit", "Fill", "Crop", "Original", "Stretch"), labels)
        assertEquals(VideoDisplayMode.Fit, VideoDisplayMode.entries.first { it.name == "Fit" })
    }

    @Test
    fun aspectRatioLookupByLabel() {
        assertEquals(AspectRatioPreset.R16x9, AspectRatioPreset.fromLabel("16:9"))
        assertEquals(AspectRatioPreset.Auto, AspectRatioPreset.fromLabel("unknown"))
    }

    // ------------------------------------------------------------
    // Screenshot naming
    // ------------------------------------------------------------

    @Test
    fun screenshotNameUsesMediaTimeAndExt() {
        val name = MediaEngine.screenshotFileName("Kaiteyo EP07 [1080p].mkv", 18 * 60_000 + 42 * 1000, "png")
        assertEquals("Kaiteyo_Kaiteyo EP07 1080p_18-42-00.png", name)
    }

    @Test
    fun screenshotJpegNormalizesExtension() {
        assertEquals("Kaiteyo_media_00-01-02.jpg", MediaEngine.screenshotFileName("", 62_000, "jpeg"))
        assertEquals("Kaiteyo_media_00-01-02.jpg", MediaEngine.screenshotFileName("???", 62_000, "jpg"))
    }

    @Test
    fun screenshotInvalidFormatFallsBackToPng() {
        assertEquals("Kaiteyo_media_00-00-00.png", MediaEngine.screenshotFileName("", 0, "bmp"))
    }
}
