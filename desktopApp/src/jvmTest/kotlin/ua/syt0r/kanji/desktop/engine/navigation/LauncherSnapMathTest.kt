package ua.syt0r.kanji.desktop.engine.navigation

import androidx.compose.ui.geometry.Offset
import ua.syt0r.kanji.desktop.appstate.LauncherSnapPoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the floating launcher's drag → snap → persist and restart-restore
 * flows end-to-end at the geometry level (the same code path the running app
 * executes — DsFloatingLauncher delegates to LauncherSnapMath).
 */
class LauncherSnapMathTest {

    private val layout = LauncherSnapMath.SnapLayout(
        windowWidth = 1280f,
        windowHeight = 800f,
        bubbleSize = 52f,
        edgeInset = 0f
    )

    private fun near(a: Float, b: Float, eps: Float = 0.001f): Boolean = abs(a - b) <= eps

    private fun assertInWindow(pos: Offset, layout: LauncherSnapMath.SnapLayout) {
        assertTrue(pos.x >= 0f, "x=$pos must be >= 0")
        assertTrue(pos.y >= 0f, "y=$pos must be >= 0")
        assertTrue(
            pos.x + layout.bubbleSize <= layout.windowWidth + 0.001f,
            "bubble right edge ${pos.x + layout.bubbleSize} exceeds window width ${layout.windowWidth}"
        )
        assertTrue(
            pos.y + layout.bubbleSize <= layout.windowHeight + 0.001f,
            "bubble bottom edge ${pos.y + layout.bubbleSize} exceeds window height ${layout.windowHeight}"
        )
    }

    // ---------------------------------------------------------------
    // Snap grid geometry
    // ---------------------------------------------------------------

    @Test
    fun allTwelveAnchorsKeepBubbleInsideWindow() {
        LauncherSnapPoint.entries.forEach { snap ->
            val pos = LauncherSnapMath.anchorPosition(snap, layout)
            assertInWindow(pos, layout)
        }
    }

    @Test
    fun edgeAnchorsRespectEdgeInsetForTopAndBottom() {
        val insetLayout = layout.copy(edgeInset = 72f)
        assertEquals(72f, LauncherSnapMath.anchorPosition(LauncherSnapPoint.TopLeft, insetLayout).y)
        assertEquals(72f, LauncherSnapMath.anchorPosition(LauncherSnapPoint.TopCenter, insetLayout).y)
        assertEquals(
            insetLayout.windowHeight - 72f - insetLayout.bubbleSize,
            LauncherSnapMath.anchorPosition(LauncherSnapPoint.BottomCenter, insetLayout).y
        )
        // Left/right anchors are vertically centered, unaffected by the inset.
        assertEquals(
            (layout.windowHeight - layout.bubbleSize) / 2f,
            LauncherSnapMath.anchorPosition(LauncherSnapPoint.LeftCenter, layout).y
        )
    }

    @Test
    fun safeMarginOffsetsAnchorsFromWindowEdges() {
        val marginLayout = layout.copy(safeMargin = 16f)
        // Left anchors are offset by the margin from x=0.
        assertEquals(16f, LauncherSnapMath.anchorPosition(LauncherSnapPoint.LeftCenter, marginLayout).x)
        assertEquals(16f, LauncherSnapMath.anchorPosition(LauncherSnapPoint.TopLeft, marginLayout).x)
        // Right anchors are inset from the right edge by the margin.
        assertEquals(
            marginLayout.windowWidth - marginLayout.bubbleSize - 16f,
            LauncherSnapMath.anchorPosition(LauncherSnapPoint.RightCenter, marginLayout).x
        )
        assertEquals(
            marginLayout.windowWidth - marginLayout.bubbleSize - 16f,
            LauncherSnapMath.anchorPosition(LauncherSnapPoint.TopRight, marginLayout).x
        )
    }

    @Test
    fun safeMarginOverridesEdgeInsetWhenLarger() {
        val mixedLayout = layout.copy(edgeInset = 4f, safeMargin = 20f)
        // TopY should be max(edgeInset, safeMargin) = 20.
        assertEquals(20f, LauncherSnapMath.anchorPosition(LauncherSnapPoint.TopCenter, mixedLayout).y)
    }

    @Test
    fun cornerAnchorsCollapseToIdenticalPositions() {
        assertEquals(
            LauncherSnapMath.anchorPosition(LauncherSnapPoint.TopLeft, layout),
            LauncherSnapMath.anchorPosition(LauncherSnapPoint.LeftTop, layout)
        )
        assertEquals(
            LauncherSnapMath.anchorPosition(LauncherSnapPoint.BottomRight, layout),
            LauncherSnapMath.anchorPosition(LauncherSnapPoint.RightBottom, layout)
        )
    }

    @Test
    fun nearestSnapSelectsClosestAnchor() {
        // A drag dropped exactly on an anchor must settle on that anchor.
        LauncherSnapPoint.entries.forEach { snap ->
            val anchor = LauncherSnapMath.anchorPosition(snap, layout)
            assertEquals(snap, LauncherSnapMath.nearestSnap(anchor, layout), "drop at $snap")
        }
        // Slight offsets near an anchor still resolve to it (magnetic behavior).
        val rightCenter = LauncherSnapMath.anchorPosition(LauncherSnapPoint.RightCenter, layout)
        assertEquals(
            LauncherSnapPoint.RightCenter,
            LauncherSnapMath.nearestSnap(Offset(rightCenter.x + 14f, rightCenter.y - 9f), layout)
        )
    }

    // ---------------------------------------------------------------
    // Drag → snap → persist
    // ---------------------------------------------------------------

    @Test
    fun settleProducesAnchorAndNormalizedFractions() {
        val released = Offset(1100f, 300f)
        val result = LauncherSnapMath.settle(released, layout)

        assertEquals(LauncherSnapPoint.RightCenter, result.snap)
        assertEquals(
            LauncherSnapMath.anchorPosition(LauncherSnapPoint.RightCenter, layout),
            result.anchor
        )
        // Normalized fractions must round-trip back to the anchor in the same window.
        assertTrue(near(result.posX, (layout.windowWidth - layout.bubbleSize) / layout.windowWidth))
        assertTrue(near(result.posY, (layout.windowHeight - layout.bubbleSize) / 2f / layout.windowHeight))
        assertTrue(result.posX in 0f..1f)
        assertTrue(result.posY in 0f..1f)
    }

    // ---------------------------------------------------------------
    // Restart persistence (the critical regression scenario)
    // ---------------------------------------------------------------

    @Test
    fun rightCenterPositionSurvivesRestartInSameWindow() {
        // Simulate: user drags to RIGHT-CENTER, app persists the settled spot.
        val settled = LauncherSnapMath.settle(
            Offset(1100f, 300f), // anywhere near the right edge center
            layout
        )
        // Restart: restore from the persisted fractions in the same window.
        val restored = LauncherSnapMath.restorePosition(settled.posX, settled.posY, layout)

        assertEquals(settled.anchor, restored)
        assertEquals(settled.snap, LauncherSnapMath.nearestSnap(restored, layout))
        assertInWindow(restored, layout)
    }

    @Test
    fun storedPositionRecalculatesWhenWindowShrinks() {
        // Persisted on a 1920×1080 display at the bottom-right anchor.
        val bigLayout = LauncherSnapMath.SnapLayout(1920f, 1080f, layout.bubbleSize, 0f)
        val settled = LauncherSnapMath.settle(Offset(1850f, 1000f), bigLayout)
        // Restart into a much smaller window — must not park the bubble
        // off-screen and must not crash.
        val smallLayout = LauncherSnapMath.SnapLayout(640f, 480f, layout.bubbleSize, 0f)
        val restored = LauncherSnapMath.restorePosition(settled.posX, settled.posY, smallLayout)

        assertInWindow(restored, smallLayout)
        // Because the stored spot no longer fits, it re-snaps to the nearest
        // valid anchor — a concrete snap point, not a drifting offset.
        assertEquals(
            LauncherSnapMath.anchorPosition(LauncherSnapMath.nearestSnap(restored, smallLayout), smallLayout),
            restored
        )
    }

    @Test
    fun corruptStoredValuesNeverCrashAndStayInWindow() {
        // Negative / oversized / NaN fractions from a corrupt settings file.
        val garbage = listOf(
            Offset(-500f, -500f),
            Offset(9999f, 9999f),
            Offset(Float.NaN, Float.NaN),
            Offset(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        )
        garbage.forEach { (x, y) ->
            val restored = LauncherSnapMath.restorePosition(x, y, layout)
            assertInWindow(restored, layout)
            assertEquals(LauncherSnapMath.nearestSnap(restored, layout).let { snap ->
                LauncherSnapMath.anchorPosition(snap, layout)
            }, restored, "corrupt input ($x, $y) must re-snap to a valid anchor")
        }
    }

    @Test
    fun restoreRespectsSafeMargin() {
        val marginLayout = layout.copy(safeMargin = 16f)
        // Stored at (0, 0) — should be clamped to the margin.
        val restored = LauncherSnapMath.restorePosition(0f, 0f, marginLayout)
        assertTrue(restored.x >= 16f, "x=${restored.x} must be >= safeMargin")
        assertTrue(restored.y >= 16f, "y=${restored.y} must be >= safeMargin")
        // Stored way off-screen — must re-snap to a valid anchor (which is within margin).
        val far = LauncherSnapMath.restorePosition(0.99f, 0.99f, marginLayout)
        assertInWindow(far, marginLayout)
        assertEquals(
            LauncherSnapMath.anchorPosition(LauncherSnapMath.nearestSnap(far, marginLayout), marginLayout),
            far
        )
    }

    @Test
    fun invalidStoredSnapPointNameFallsBackToBottomRight() {
        assertEquals(LauncherSnapPoint.BottomRight, LauncherSnapPoint.fromName("not-a-snap"))
        assertEquals(LauncherSnapPoint.BottomRight, LauncherSnapPoint.fromName(null))
        // Case-insensitive round-trip of a legitimately persisted name.
        assertEquals(LauncherSnapPoint.RightCenter, LauncherSnapPoint.fromName("RIGHT-CENTER"))
    }
}
