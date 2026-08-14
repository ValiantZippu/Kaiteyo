package ua.syt0r.kanji.desktop.engine.jdata.engine

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression + feature tests for [SvgPathConverter], the only place the data
 * platform touches Compose. Covers the full command set KanjiVG emits, the
 * relative-coordinate cursor math, S/T control-point reflection and the
 * multi-subpath `Z` handling that previously looped forever.
 */
class SvgPathConverterTest {

    private fun bounds(pathData: String): Rect {
        val path = assertNotNull(SvgPathConverter.toComposePath(pathData), "should parse: $pathData")
        return path.getBounds()
    }

    @Test
    fun absoluteLines() {
        val b = bounds("M10,10 L100,10")
        assertEquals(10f, b.left, 0.01f)
        assertEquals(100f, b.right, 0.01f)
        assertEquals(10f, b.top, 0.01f)
        assertEquals(10f, b.bottom, 0.01f)
    }

    @Test
    fun implicitLinetoAfterMove() {
        // Extra coordinate pairs after M are implicit lineto (SVG spec).
        val b = bounds("M10,10 100,10")
        assertEquals(10f, b.left, 0.01f)
        assertEquals(100f, b.right, 0.01f)
    }

    @Test
    fun horizontalAndVerticalCommands() {
        val b = bounds("M0,0 H100 V50")
        assertEquals(0f, b.left, 0.01f)
        assertEquals(100f, b.right, 0.01f)
        assertEquals(0f, b.top, 0.01f)
        assertEquals(50f, b.bottom, 0.01f)
    }

    @Test
    fun relativeCommandsAccumulateCursor() {
        // M10,10 then relative m5,5 l5,0 h5 v5 -> ends at (25,20).
        val b = bounds("M10,10 m5,5 l5,0 h5 v5")
        assertEquals(10f, b.left, 0.01f)
        assertEquals(25f, b.right, 0.01f)
        assertEquals(10f, b.top, 0.01f)
        assertEquals(20f, b.bottom, 0.01f)
    }

    @Test
    fun cubicCurvesWithReflection() {
        // S reflects the previous control point: control (20,10) about (30,0)
        // -> (40,-10). The curve dips below the baseline, so bounds y < 0.
        val b = bounds("M0,0 C10,10 20,10 30,0 S50,-10 60,0")
        assertEquals(0f, b.left, 0.01f)
        assertEquals(60f, b.right, 0.01f)
        assertTrue(b.top <= 0f, "curve should dip negative, top=${b.top}")
        assertTrue(b.bottom >= -10.5f, "control points at -10, bottom=${b.bottom}")
    }

    @Test
    fun quadraticCurves() {
        val b = bounds("M0,0 Q50,100 100,0")
        assertEquals(0f, b.left, 0.01f)
        assertEquals(100f, b.right, 0.01f)
    }

    @Test
    fun multiSubpathCloseCompletes() {
        // Regression: this used to re-enter Z forever. A coordinate pair after Z
        // is an implicit moveto (SVG spec), so this yields two subpaths.
        val b = bounds("M0,0 L10,0 Z M20,0 L30,0 Z")
        assertEquals(0f, b.left, 0.01f)
        assertEquals(30f, b.right, 0.01f)
    }

    @Test
    fun arcIsApproximatedAsLine() {
        val b = bounds("M0,0 A10 10 0 0 1 20,0")
        assertEquals(0f, b.left, 0.01f)
        assertEquals(20f, b.right, 0.01f)
    }

    @Test
    fun relativeArc() {
        val b = bounds("M0,0 a10 10 0 0 1 20,0")
        assertEquals(0f, b.left, 0.01f)
        assertEquals(20f, b.right, 0.01f)
    }

    @Test
    fun kanjiVgStyleCoordinatesWithExponents() {
        // Real-world KanjiVG data: decimal + exponent tokens, spaces everywhere.
        val b = bounds("M 28.44 12.5 C 28.44 12.5 24.56 9.5 23.38 8.5 L 8.5 23.38")
        assertEquals(8.5f, b.left, 0.5f)
        assertEquals(28.44f, b.right, 0.5f)
    }

    @Test
    fun unknownCommandWithCoordinatesRejected() {
        assertNull(SvgPathConverter.toComposePath("X 10 10"))
    }

    @Test
    fun truncatedCurveRejected() {
        assertNull(SvgPathConverter.toComposePath("M0,0 C10,10 20"))
    }

    @Test
    fun emptyInputRejected() {
        assertNull(SvgPathConverter.toComposePath(""))
        assertNull(SvgPathConverter.toComposePath("   "))
    }
}
