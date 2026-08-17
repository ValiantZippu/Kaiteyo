package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure geometry tests for the navigation layout math — the single model that
 * derives the docked bar size, the adaptive sidebar width and the content
 * reservation. These are the guarantees behind "the sidebar can never swallow
 * the window" and "the transition can never produce negative padding".
 */
class NavGeometryTest {

    private fun settings(widthIndex: Int = 1) =
        NavigationSettings(sidebar = SidebarSettings(expandedWidthIndex = widthIndex))

    // ---------------------------------------------------------------
    // adaptiveSidebarWidth
    // ---------------------------------------------------------------

    @Test
    fun smallWindowClampsToMinimumSidebarWidth() {
        // 16% of 1200dp = 192dp → clamps to the 208dp minimum.
        val width = NavGeometry.adaptiveSidebarWidth(1200.dp, settings(widthIndex = 0))
        assertEquals(NavGeometry.MinSidebarWidth, width)
    }

    @Test
    fun hugeWindowClampsToMaximumSidebarWidth() {
        // 22% of 3000dp = 660dp → clamps to the 384dp maximum.
        val width = NavGeometry.adaptiveSidebarWidth(3000.dp, settings(widthIndex = 3))
        assertEquals(NavGeometry.MaxSidebarWidth, width)
    }

    @Test
    fun widthIndexPicksRatioBetweenBounds() {
        // 2000dp * 0.18 (index 1) = 360dp — inside the [208, 384] band.
        val width = NavGeometry.adaptiveSidebarWidth(2000.dp, settings(widthIndex = 1))
        assertEquals(360.dp, width)
        // 2000dp * 0.22 (index 3) = 440dp → clamped.
        val clamped = NavGeometry.adaptiveSidebarWidth(2000.dp, settings(widthIndex = 3))
        assertEquals(NavGeometry.MaxSidebarWidth, clamped)
    }

    @Test
    fun invalidWidthIndexFallsBackToDefaultRatio() {
        // Out-of-range persisted index → 0.20 ratio (1500 * 0.20 = 300dp).
        val width = NavGeometry.adaptiveSidebarWidth(1500.dp, settings(widthIndex = 99))
        assertEquals(300.dp, width)
    }

    @Test
    fun sidebarWidthNeverGrowsWithWindowPastMaximum() {
        // Even a 10 000dp-wide window keeps the sidebar at the maximum.
        val width = NavGeometry.adaptiveSidebarWidth(10_000.dp, settings(widthIndex = 1))
        assertEquals(NavGeometry.MaxSidebarWidth, width)
    }

    // ---------------------------------------------------------------
    // dockedBarSize
    // ---------------------------------------------------------------

    @Test
    fun expandedVerticalSidebarUsesAdaptiveWidth() {
        val size = NavGeometry.dockedBarSize(
            settings = settings(widthIndex = 1),
            formFactor = FormFactor.Desktop,
            expanded = true,
            vertical = true,
            containerWidthDp = 2000.dp
        )
        assertEquals(360.dp, size)
    }

    @Test
    fun compactVerticalSidebarIsFixedRailWidth() {
        val size = NavGeometry.dockedBarSize(
            settings = settings(),
            formFactor = FormFactor.Desktop,
            expanded = false,
            vertical = true,
            containerWidthDp = 2000.dp
        )
        assertEquals(NavTokens.CompactRailWidth, size)
    }

    @Test
    fun phoneBarsUseFixedTouchHeight() {
        val expanded = NavGeometry.dockedBarSize(
            settings = settings(),
            formFactor = FormFactor.Phone,
            expanded = true,
            vertical = false,
            containerWidthDp = 400.dp
        )
        val compact = NavGeometry.dockedBarSize(
            settings = settings(),
            formFactor = FormFactor.Phone,
            expanded = false,
            vertical = false,
            containerWidthDp = 400.dp
        )
        assertEquals(NavTokens.PhoneBarHeight, expanded)
        assertEquals(NavTokens.PhoneBarHeight, compact)
    }

    @Test
    fun desktopHorizontalBarsUseFixedHeights() {
        val expanded = NavGeometry.dockedBarSize(
            settings = settings(),
            formFactor = FormFactor.Desktop,
            expanded = true,
            vertical = false,
            containerWidthDp = 2000.dp
        )
        val compact = NavGeometry.dockedBarSize(
            settings = settings(),
            formFactor = FormFactor.Desktop,
            expanded = false,
            vertical = false,
            containerWidthDp = 2000.dp
        )
        assertEquals(NavTokens.HorizontalBarHeight, expanded)
        assertEquals(NavTokens.HorizontalBarCompactHeight, compact)
    }

    @Test
    fun dockedSizeNeverExceedsHalfTheWindow() {
        // Degenerate inputs can never let the sidebar swallow the screen: the
        // docked region is hard-capped at 50% (vertical) / 40% (horizontal) of
        // the window regardless of settings or persisted state.
        for (container in listOf(300.dp, 800.dp, 2000.dp)) {
            for (expanded in listOf(true, false)) {
                for (vertical in listOf(true, false)) {
                    val size = NavGeometry.dockedBarSize(
                        settings = settings(widthIndex = 3),
                        formFactor = FormFactor.Desktop,
                        expanded = expanded,
                        vertical = vertical,
                        containerWidthDp = container
                    )
                    val limit = if (vertical) container * 0.5f else container * 0.4f
                    assertTrue(size <= limit, "$container $expanded $vertical -> $size (limit $limit)")
                }
            }
        }
    }

    @Test
    fun dockedSizeWithDegenerateWindowWidthsStaysValid() {
        // A window that reports zero or near-zero width during startup/resize
        // must never produce a negative or exploding docked size — both would
        // flow into the content padding and crash layout.
        for (container in listOf(0.dp, 1.dp, 100.dp)) {
            for (vertical in listOf(true, false)) {
                val size = NavGeometry.dockedBarSize(
                    settings = settings(widthIndex = 3),
                    formFactor = FormFactor.Desktop,
                    expanded = true,
                    vertical = vertical,
                    containerWidthDp = container
                )
                assertTrue(size >= 0.dp, "$container $vertical -> $size")
                val limit = if (vertical) container * 0.5f else container * 0.4f
                assertTrue(size <= limit, "$container $vertical -> $size (limit $limit)")
            }
        }
    }

    // ---------------------------------------------------------------
    // contentReserve — never negative, mode-aware
    // ---------------------------------------------------------------

    @Test
    fun floatingModeReservesNothing() {
        assertEquals(0.dp, NavGeometry.contentReserve(NavigationMode.Floating, 360.dp, 0.dp))
    }

    @Test
    fun sidebarModeReservesDockedSizePlusInset() {
        assertEquals(360.dp, NavGeometry.contentReserve(NavigationMode.Sidebar, 360.dp, 0.dp))
        assertEquals(384.dp, NavGeometry.contentReserve(NavigationMode.Sidebar, 360.dp, 24.dp))
    }

    @Test
    fun reserveIsAlwaysNonNegative() {
        // Any combination of valid inputs must stay >= 0 so the animated
        // content padding can never produce a negative value mid-transition.
        for (mode in NavigationMode.entries) {
            for (docked in listOf(0.dp, 64.dp, 208.dp, 384.dp)) {
                for (inset in listOf(0.dp, 24.dp)) {
                    val reserve = NavGeometry.contentReserve(mode, docked, inset)
                    kotlin.test.assertTrue(reserve >= 0.dp, "$mode $docked $inset -> $reserve")
                }
            }
        }
    }

    @Test
    fun sidebarPairFitsTheWindow() {
        // The sidebar surface plus the content it reserves are the two halves
        // of the same docked size — together they must always fit the window
        // so "sidebar + content" can never overflow or collapse into each other.
        for (container in listOf(320.dp, 640.dp, 1280.dp, 2560.dp)) {
            for (vertical in listOf(true, false)) {
                val docked = NavGeometry.dockedBarSize(
                    settings = settings(widthIndex = 3),
                    formFactor = FormFactor.Desktop,
                    expanded = true,
                    vertical = vertical,
                    containerWidthDp = container
                )
                val reserve = NavGeometry.contentReserve(NavigationMode.Sidebar, docked, 0.dp)
                assertTrue(docked <= container, "$container $vertical -> docked $docked")
                assertTrue(reserve in 0.dp..container, "$container $vertical -> reserve $reserve")
                assertTrue(docked + reserve <= container + 0.001.dp)
            }
        }
    }
}
