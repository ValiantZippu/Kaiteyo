package ua.syt0r.kanji.desktop.engine.navigation

import androidx.compose.ui.geometry.Offset
import ua.syt0r.kanji.desktop.appstate.LauncherSnapPoint

/**
 * Pure geometry for the floating launcher's 12-point snap grid (three anchors
 * per edge). Kept free of composable/state so the drag → snap → persist and
 * restart-restore flows can be verified by unit tests.
 *
 * All coordinates are in the same unit as [SnapLayout] (window-space pixels).
 */
object LauncherSnapMath {

    /** Bounds the launcher operates in. */
    data class SnapLayout(
        val windowWidth: Float,
        val windowHeight: Float,
        val bubbleSize: Float,
        /** Top/bottom anchors keep this clearance from the window edge (tab bars / gesture zones). */
        val edgeInset: Float,
        /** Minimum clearance from every window edge so the bubble never clips. */
        val safeMargin: Float = 0f
    )

    /** Top-left position of the bubble when parked on [snap]. */
    fun anchorPosition(snap: LauncherSnapPoint, layout: SnapLayout): Offset {
        val w = layout.windowWidth
        val h = layout.windowHeight
        val size = layout.bubbleSize
        val inset = layout.edgeInset
        val margin = layout.safeMargin
        // Vertical clearance: the larger of the phone gesture inset and the
        // safe margin so the bubble never clips the window edge or gesture zone.
        val topY = maxOf(inset, margin)
        val bottomY = maxOf(inset, margin)
        return when (snap) {
            LauncherSnapPoint.TopLeft, LauncherSnapPoint.LeftTop -> Offset(margin, topY)
            LauncherSnapPoint.TopCenter -> Offset((w - size) / 2f, topY)
            LauncherSnapPoint.TopRight, LauncherSnapPoint.RightTop -> Offset(w - size - margin, topY)
            LauncherSnapPoint.BottomLeft, LauncherSnapPoint.LeftBottom -> Offset(margin, h - bottomY - size)
            LauncherSnapPoint.BottomCenter -> Offset((w - size) / 2f, h - bottomY - size)
            LauncherSnapPoint.BottomRight, LauncherSnapPoint.RightBottom -> Offset(w - size - margin, h - bottomY - size)
            LauncherSnapPoint.LeftCenter -> Offset(margin, (h - size) / 2f)
            LauncherSnapPoint.RightCenter -> Offset(w - size - margin, (h - size) / 2f)
        }
    }

    /** The snap point whose anchor is closest to [position] (Euclidean distance). */
    fun nearestSnap(position: Offset, layout: SnapLayout): LauncherSnapPoint =
        LauncherSnapPoint.entries.minByOrNull { snap ->
            val anchor = anchorPosition(snap, layout)
            val dx = position.x - anchor.x
            val dy = position.y - anchor.y
            dx * dx + dy * dy
        } ?: LauncherSnapPoint.BottomRight

    /** Result of releasing a drag: the chosen anchor plus the normalized fractions to persist. */
    data class SettleResult(
        val snap: LauncherSnapPoint,
        val anchor: Offset,
        val posX: Float,
        val posY: Float
    )

    /**
     * The full release path: pick the nearest anchor, then produce the
     * normalized (0..1) window fractions the app persists across restarts.
     */
    fun settle(position: Offset, layout: SnapLayout): SettleResult {
        val snap = nearestSnap(position, layout)
        val anchor = anchorPosition(snap, layout)
        return SettleResult(
            snap = snap,
            anchor = anchor,
            posX = (anchor.x / layout.windowWidth).coerceIn(0f, 1f),
            posY = (anchor.y / layout.windowHeight).coerceIn(0f, 1f)
        )
    }

    /**
     * Restore a persisted position (normalized fractions from a previous
     * session) into the current window. Window sizes change between sessions,
     * and stored values can be stale or corrupt — this never crashes and
     * never parks the bubble off-screen:
     *   · fractions are clamped to 0..1 first,
     *   · the resulting offset is clamped so the whole bubble stays inside,
     *   · if the stored spot is out of bounds it snaps to the nearest valid
     *     anchor instead of drifting out of the window.
     */
    fun restorePosition(storedX: Float, storedY: Float, layout: SnapLayout): Offset {
        val margin = layout.safeMargin
        val maxX = (layout.windowWidth - layout.bubbleSize - margin).coerceAtLeast(0f)
        val maxY = (layout.windowHeight - layout.bubbleSize - margin).coerceAtLeast(0f)
        // NaN would poison every downstream comparison — treat corrupt input
        // as "unset" (falls back to the bottom-right default region).
        fun sane(v: Float): Float = if (v.isFinite()) v.coerceIn(0f, 1f) else 0f
        val raw = Offset(
            sane(storedX) * layout.windowWidth,
            sane(storedY) * layout.windowHeight
        )
        val clamped = Offset(raw.x.coerceIn(margin, maxX), raw.y.coerceIn(margin, maxY))
        // The stored spot only stays as-is when it lands the bubble fully
        // inside the window; otherwise re-snap to the nearest valid anchor.
        return if (clamped == raw) clamped else anchorPosition(nearestSnap(clamped, layout), layout)
    }
}
