package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FrequencySystemTest {

    @Test
    fun ranksMapToBands() {
        assertEquals(FrequencyBand.VeryCommon, FrequencyBand.forRank(1))
        assertEquals(FrequencyBand.VeryCommon, FrequencyBand.forRank(500))
        assertEquals(FrequencyBand.Common, FrequencyBand.forRank(501))
        assertEquals(FrequencyBand.Moderate, FrequencyBand.forRank(1500))
        assertEquals(FrequencyBand.Uncommon, FrequencyBand.forRank(2500))
        assertEquals(FrequencyBand.Rare, FrequencyBand.forRank(4000))
    }

    @Test
    fun unknownAndInvalidRanksAreNull() {
        assertNull(FrequencyBand.forRank(null))
        assertNull(FrequencyBand.forRank(0))
        assertNull(FrequencyBand.forRank(-5))
    }

    @Test
    fun rankLabelFormats() {
        assertEquals("#183", frequencyRankLabel(183))
        assertEquals("Unranked", frequencyRankLabel(null))
        assertEquals("Unranked", frequencyRankLabel(0))
    }

    @Test
    fun everyBandHasBothLanguageLabels() {
        FrequencyBand.entries.forEach { band ->
            assertEquals(true, band.label.isNotBlank())
            assertEquals(true, band.jpLabel.isNotBlank())
        }
    }
}
