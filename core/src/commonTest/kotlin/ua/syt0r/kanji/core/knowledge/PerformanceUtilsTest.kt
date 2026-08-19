package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals

class PerformanceUtilsTest {

    // ---------------------------------------------------------------
    // frequencyNormalized
    // ---------------------------------------------------------------

    @Test
    fun frequencyNormalizedRank1() {
        assertEquals(1.0f, frequencyNormalized(1, 5000), 0.001f)
    }

    @Test
    fun frequencyNormalizedRankMid() {
        assertEquals(0.5f, frequencyNormalized(2500, 5000), 0.001f)
    }

    @Test
    fun frequencyNormalizedNull() {
        assertEquals(0f, frequencyNormalized(null))
    }

    @Test
    fun frequencyNormalizedZero() {
        assertEquals(0f, frequencyNormalized(0))
    }

    @Test
    fun frequencyNormalizedNegative() {
        assertEquals(0f, frequencyNormalized(-1))
    }

    @Test
    fun frequencyNormalizedClamped() {
        assertEquals(0f, frequencyNormalized(5000, 5000), 0.001f)
        assertEquals(1.0f, frequencyNormalized(1, 1), 0.001f)
    }
}
