package ua.syt0r.kanji.desktop.engine.media

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import ua.syt0r.kanji.desktop.engine.dictionary.SegmentToken
import ua.syt0r.kanji.desktop.ui.media.expandRange
import ua.syt0r.kanji.desktop.ui.media.tokenIndexAt
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure-logic tests for the multi-word subtitle selection helpers: the token
 * hit-test (click/shift-click/drag resolve to token indices), range expansion
 * (contiguous selection between anchor and pointer) and phrase reconstruction
 * (joined token surfaces reproduce the exact original text).
 */
class MediaSelectionTest {

    private fun token(surface: String, offset: Int) =
        SegmentToken(surface = surface, offset = offset, isJapanese = true, isKanji = false)

    @Test
    fun `joinTokenSurfaces reproduces the original text in offset order`() {
        val tokens = listOf(
            token("学校", 2), token("に", 4), token("行き", 6), token("ました", 8)
        ).shuffled()
        assertEquals("学校に行きました", joinTokenSurfaces(tokens))
    }

    @Test
    fun `joinTokenSurfaces handles single and empty selections`() {
        assertEquals("", joinTokenSurfaces(emptyList()))
        assertEquals("猫", joinTokenSurfaces(listOf(token("猫", 0))))
    }

    @Test
    fun `tokenIndexAt picks the smallest token containing the point`() {
        val bounds = mapOf(
            0 to Rect(0f, 0f, 40f, 20f),   // wide token
            1 to Rect(30f, 0f, 60f, 20f),  // narrow token overlapping at x=35
            2 to Rect(100f, 0f, 140f, 20f)
        )
        assertEquals(0, tokenIndexAt(Offset(10f, 10f), bounds))
        assertEquals(1, tokenIndexAt(Offset(35f, 10f), bounds)) // narrow wins at overlap
        assertEquals(2, tokenIndexAt(Offset(120f, 10f), bounds))
        assertEquals(-1, tokenIndexAt(Offset(200f, 10f), bounds))
        assertEquals(-1, tokenIndexAt(Offset(10f, 50f), bounds))
        assertEquals(-1, tokenIndexAt(Offset.Zero, emptyMap()))
    }

    @Test
    fun `expandRange orders the anchor and pointer`() {
        assertEquals(2..5, expandRange(2, 5))
        assertEquals(2..5, expandRange(5, 2))
        assertEquals(3..3, expandRange(3, 3))
        assertEquals(-1..3, expandRange(-1, 3))
    }
}
