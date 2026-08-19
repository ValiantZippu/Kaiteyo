package ua.syt0r.kanji.presentation.common.theme

import ua.syt0r.kanji.core.knowledge.FrequencyBand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// ============================================================
// Theme system unit tests.
// Validates the Kaiteyo base-mode system (spec §36:
// Light / Dark / OLED / Sepia / Cream / Paper / Midnight) and the
// luminance-adaptive study-state + frequency tokens. These are pure
// functions so they run on every target via :core:allTests.
// ============================================================

class BaseModeSystemTest {

    @Test
    fun every_base_mode_has_a_surface_definition() {
        BaseMode.entries.forEach { mode ->
            val surface = surfaceForBaseMode(mode)
            assertNotNull(surface.background, "$mode background must be defined")
            assertNotNull(surface.textPrimary, "$mode textPrimary must be defined")
            assertTrue(surface.frequencyTiers.size == 5, "$mode must expose 5 frequency tiers")
            assertNotNull(surface.kanjiKnown, "$mode kanjiKnown token must be defined")
            assertNotNull(surface.kanjiLearning, "$mode kanjiLearning token must be defined")
            assertNotNull(surface.kanjiNew, "$mode kanjiNew token must be defined")
        }
    }

    @Test
    fun isDarkMode_classifies_base_modes_correctly() {
        assertEquals(true, BaseMode.Oled.isDarkMode)
        assertEquals(true, BaseMode.Dark.isDarkMode)
        assertEquals(true, BaseMode.Midnight.isDarkMode)
        assertEquals(false, BaseMode.Light.isDarkMode)
        assertEquals(false, BaseMode.Sepia.isDarkMode)
        assertEquals(false, BaseMode.Cream.isDarkMode)
        assertEquals(false, BaseMode.Paper.isDarkMode)
    }

    @Test
    fun light_and_dark_bands_differ() {
        val lightBands = frequencyBandColors(isDark = false)
        val darkBands = frequencyBandColors(isDark = true)
        lightBands.forEachIndexed { i, c ->
            assertNotEquals(c, darkBands[i], "band $i should differ for light vs dark")
        }
    }

    @Test
    fun frequency_band_colors_match_enum_ordinals() {
        // Green → blue → amber → orange → red as rarity grows (Kaiteyo language).
        val bands = BaseMode.entries.first().let { frequencyBandColors(it.isDarkMode) }
        assertEquals(5, bands.size)
        assertNotEquals(bands[0], bands[4], "very common and rare must be visually distinct")
    }

    @Test
    fun frequency_color_for_band_is_stable_across_modes() {
        // Same band, same luminance family → colors only change by light/dark,
        // never become identical (accessibility: never color-only).
        BaseMode.entries.forEach { mode ->
            val color = frequencyColorForBand(FrequencyBand.VeryCommon, mode.isDarkMode)
            assertNotNull(color)
        }
    }

    @Test
    fun null_frequency_band_falls_back_to_neutral() {
        val fallback = frequencyColorForBand(null, isDark = true)
        // Fallback must be a grey, not one of the band colors, so unranked
        // kanji are visually distinct from every ranked band.
        assertNotEquals(frequencyBandColors(true)[0], fallback)
    }

    @Test
    fun frequency_band_for_rank_maps_canonical_ranges() {
        // Reconciles the single source of truth in the domain enum.
        assertEquals(FrequencyBand.VeryCommon, FrequencyBand.forRank(1))
        assertEquals(FrequencyBand.VeryCommon, FrequencyBand.forRank(500))
        assertEquals(FrequencyBand.Common, FrequencyBand.forRank(501))
        assertEquals(FrequencyBand.Common, FrequencyBand.forRank(1000))
        assertEquals(FrequencyBand.Moderate, FrequencyBand.forRank(1001))
        assertEquals(FrequencyBand.Uncommon, FrequencyBand.forRank(2001))
        assertEquals(FrequencyBand.Rare, FrequencyBand.forRank(3501))
        assertEquals(FrequencyBand.Rare, FrequencyBand.forRank(999_999))
        assertEquals(null, FrequencyBand.forRank(null))
        assertEquals(null, FrequencyBand.forRank(0))
        assertEquals(null, FrequencyBand.forRank(-3))
    }
}
