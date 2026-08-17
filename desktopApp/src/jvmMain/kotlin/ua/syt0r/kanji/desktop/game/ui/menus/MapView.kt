package ua.syt0r.kanji.desktop.game.ui.menus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.engine.input.InputAction
import ua.syt0r.kanji.desktop.game.world.Region
import kotlin.math.PI
import kotlin.math.sin

/**
 * The map (spec §69-70): a stylized, zoomable surface — world → region —
 * that doubles as the player's discovery record. Undiscovered areas show
 * stylized geography, never fake detail or "???" pins (spec §118). Two levels:
 *
 * - **World**: every region as an island on the sea, travel lines between
 *   stations, progressive reveal (regions you've set foot in are named; the
 *   rest stay dim silhouettes).
 * - **Region**: the real tile geography of the region scaled to fit, with
 *   discovered locations, the station network, and "you are here".
 *
 * Zoom is a single animated step between levels (world → region), with pan +
 * pinch/wheel zoom inside a region. Keyboard/controller reachable: arrows move
 * the world cursor, Interact zooms, Back returns (or closes).
 */
@Composable
fun MapView(session: GameSession, onClose: () -> Unit = { session.state.closeAllPanels() }) {
    val world = session.world
    val regions = world.regions
    val discovered = session.state.worldState.discoveredLocations.toSet()
    val playerRegionId = session.player.state.regionId

    // Zoom state: null = world level; a region id = zoomed into that region.
    var zoomRegionId by remember { mutableStateOf<String?>(null) }
    var cursor by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .onSizeChanged { cursor = cursor.coerceAtMost((regions.size - 1).coerceAtLeast(0)) }
    ) {
        Column(Modifier.fillMaxSize().padding(DsSpacing.Lg)) {
            // Header: current level + living world (time/weather, spec §40-41).
            MapHeader(session, zoomRegionId)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(DsRadius.Lg))
                    .background(Color(0xFF10151C).copy(alpha = 0.9f))
            ) {
                val regionId = zoomRegionId
                if (regionId == null) {
                    WorldMapSurface(
                        session = session,
                        regions = regions,
                        discovered = discovered,
                        playerRegionId = playerRegionId,
                        cursor = cursor,
                        onZoomIn = { zoomRegionId = it }
                    )
                } else {
                    val region = world.region(regionId)
                    if (region != null) {
                        RegionMapSurface(
                            session = session,
                            region = region,
                            discovered = discovered,
                            onZoomOut = { zoomRegionId = null }
                        )
                    }
                }
            }
            MapFooter(session, zoomRegionId, onZoomOut = { zoomRegionId = null }, onClose = onClose)
        }

        // Keyboard/controller navigation for the world cursor.
        LaunchedEffect(session, zoomRegionId) {
            while (true) {
                val input = session.input.state
                if (zoomRegionId == null) {
                    if (input.wasPressedThisFrame(InputAction.MoveLeft) || input.wasPressedThisFrame(InputAction.MoveUp)) {
                        cursor = (cursor - 1 + regions.size) % regions.size
                    }
                    if (input.wasPressedThisFrame(InputAction.MoveRight) || input.wasPressedThisFrame(InputAction.MoveDown)) {
                        cursor = (cursor + 1) % regions.size
                    }
                    if (input.wasPressedThisFrame(InputAction.Interact)) {
                        regions.getOrNull(cursor)?.let { zoomRegionId = it.id }
                    }
                } else {
                    if (input.wasPressedThisFrame(InputAction.Back)) zoomRegionId = null
                }
                delay(40)
            }
        }
    }
}

// ------------------------------------------------------------
// Header & footer
// ------------------------------------------------------------

@Composable
private fun MapHeader(session: GameSession, zoomRegionId: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = DsSpacing.Sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (zoomRegionId == null) "Map — Japan  日本地図" else "Map — ${session.world.region(zoomRegionId)?.name ?: ""}",
                color = Color(0xFFFFD54F),
                fontSize = DsType.Heading,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Time · ${session.clock.hourLabel()} ${session.clock.phase.label} · ${session.weather.current.label}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = DsType.Caption
            )
        }
        Text(
            text = "${session.state.worldState.discoveredLocations.size} locations discovered",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = DsType.Label,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MapFooter(session: GameSession, zoomRegionId: String?, onZoomOut: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = DsSpacing.Md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (zoomRegionId == null) "Arrows: choose a region · Enter: zoom in" else "Drag to pan · Wheel/pinch to zoom · Back: return",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = DsType.Caption
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            if (zoomRegionId != null) {
                DsButton(
                    text = "Back to Japan",
                    kind = DsButtonKind.Ghost,
                    onClick = onZoomOut
                )
            }
            DsButton(
                text = "Close",
                kind = DsButtonKind.Ghost,
                onClick = onClose
            )
        }
    }
}

// ------------------------------------------------------------
// World level — stylized archipelago with progressive reveal
// ------------------------------------------------------------

private data class RegionSlot(
    val region: Region,
    val center: Offset,
    val radiusX: Float,
    val radiusY: Float
)

/** Arc layout so the fictional regions read as a little island chain. */
private fun worldLayout(regions: List<Region>, size: IntSize): List<RegionSlot> {
    if (regions.isEmpty()) return emptyList()
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    val n = regions.size
    val spacing = (w - 120f) / n.coerceAtLeast(1)
    val arc = 0.35f
    val rx = 90f.coerceAtMost(spacing * 0.32f)
    val ry = rx * 0.62f
    return regions.mapIndexed { i, region ->
        val t = if (n == 1) 0.5f else i / (n - 1).toFloat()
        val x = 60f + spacing * i + spacing / 2f
        val y = h / 2f + sin(t * PI.toFloat() * arc * 2f) * h * 0.16f
        RegionSlot(region, Offset(x, y), rx, ry)
    }
}

@Composable
private fun WorldMapSurface(
    session: GameSession,
    regions: List<Region>,
    discovered: Set<String>,
    playerRegionId: String,
    cursor: Int,
    onZoomIn: (String) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val slots = remember(size, regions) { worldLayout(regions, size) }
    val travel = session.world.travel

    fun regionRevealed(region: Region): Boolean =
        region.locations.any { it.id in discovered } || region.id == playerRegionId

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        // Sea.
        drawRect(Color(0xFF0E2A3A))
        // Gentle wave dots.
        var seed = 3
        var y = 20f
        while (y < size.height - 20f) {
            val x = ((seed * 97) % 173) * 1.7f % (size.width - 20f) + 10f
            drawCircle(Color.White.copy(alpha = 0.05f), radius = 2f, center = Offset(x, y))
            seed += 7
            y += 46f
        }

        // Travel lines between regions that actually have stations.
        for (line in travel.lines) {
            val lineColor = Color(line.color)
            for (edge in travel.edges) {
                if (edge.lineId != line.id) continue
                val from = travel.stations.firstOrNull { it.id == edge.fromStationId } ?: continue
                val to = travel.stations.firstOrNull { it.id == edge.toStationId } ?: continue
                val fromRegion = session.world.regionIdForCell(from.cellId) ?: continue
                val toRegion = session.world.regionIdForCell(to.cellId) ?: continue
                if (fromRegion == toRegion) continue
                val a = slots.firstOrNull { it.region.id == fromRegion }?.center ?: continue
                val b = slots.firstOrNull { it.region.id == toRegion }?.center ?: continue
                drawLine(lineColor.copy(alpha = 0.8f), a, b, strokeWidth = 3f)
                // Small train marker mid-way.
                val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                drawCircle(Color.White, radius = 3f, center = mid)
            }
        }

        // Region islands.
        slots.forEachIndexed { index, slot ->
            val region = slot.region
            val revealed = regionRevealed(region)
            val focused = index == cursor
            val islandColor = when {
                !revealed -> Color(0xFF2A3640)
                region.id == playerRegionId -> Color(0xFF3D7A4A)
                else -> Color(0xFF4E6E58)
            }
            drawOval(
                color = islandColor,
                topLeft = Offset(slot.center.x - slot.radiusX, slot.center.y - slot.radiusY),
                size = Size(slot.radiusX * 2f, slot.radiusY * 2f)
            )
            // Focus ring for keyboard/controller.
            if (focused) {
                drawOval(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(slot.center.x - slot.radiusX - 4f, slot.center.y - slot.radiusY - 4f),
                    size = Size(slot.radiusX * 2f + 8f, slot.radiusY * 2f + 8f),
                    style = Stroke(width = 2f)
                )
            }
            // "You are here" beacon.
            if (region.id == playerRegionId) {
                drawCircle(Color(0xFFFFD54F), radius = 5f, center = Offset(slot.center.x, slot.center.y - slot.radiusY - 14f))
            }
            if (revealed) {
                drawMapLabel(
                    textMeasurer = textMeasurer,
                    text = "${region.name}  ${region.nameJp}",
                    at = Offset(slot.center.x, slot.center.y - slot.radiusY * 0.15f),
                    color = Color.White
                )
                val count = region.locations.count { it.id in discovered }
                drawMapLabel(
                    textMeasurer = textMeasurer,
                    text = if (count == 0) "unvisited" else "$count/${region.locations.size} places",
                    at = Offset(slot.center.x, slot.center.y + slot.radiusY * 0.35f),
                    color = Color.White.copy(alpha = 0.6f),
                    bg = Color.Black.copy(alpha = 0.0f)
                )
            } else {
                drawMapLabel(
                    textMeasurer = textMeasurer,
                    text = "· · ·",
                    at = Offset(slot.center.x, slot.center.y),
                    color = Color.White.copy(alpha = 0.35f),
                    bg = Color.Black.copy(alpha = 0.0f)
                )
            }
        }

        if (regions.isEmpty()) {
            drawMapLabel(textMeasurer, "No regions yet", Offset(size.width / 2f, size.height / 2f), Color.White.copy(alpha = 0.5f))
        }
    }
    // Tap a region island to zoom in (mouse + touch).
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(size, slots) {
                detectTapGestures { offset ->
                    slots.firstOrNull { slot ->
                        val dx = offset.x - slot.center.x
                        val dy = offset.y - slot.center.y
                        (dx * dx) / (slot.radiusX * slot.radiusX) + (dy * dy) / (slot.radiusY * slot.radiusY) <= 1f
                    }?.let { onZoomIn(it.region.id) }
                }
            }
    )
}

// ------------------------------------------------------------
// Region level — real tile geography, discovery pins, "you are here"
// ------------------------------------------------------------

@Composable
private fun RegionMapSurface(
    session: GameSession,
    region: Region,
    discovered: Set<String>,
    onZoomOut: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    val cell = region.districts.firstNotNullOfOrNull { it.cells.firstOrNull() }
    val grid = cell?.let { ua.syt0r.kanji.desktop.game.world.TileGrid(it) }
    val worldWidth = grid?.worldWidth ?: 2208f
    val worldHeight = grid?.worldHeight ?: 1440f
    val station = session.world.travel.stations.firstOrNull { session.world.regionIdForCell(it.cellId) == region.id }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(size, region.id) {
                detectTransformGestures { centroid, panChange, zoomChange, _ ->
                    val newScale = (scale * zoomChange).coerceIn(0.5f, 6f)
                    // Zoom around the gesture centroid.
                    pan = Offset(
                        pan.x - (centroid.x) * (newScale / scale - 1f),
                        pan.y - (centroid.y) * (newScale / scale - 1f)
                    ) + panChange
                    scale = newScale
                }
            }
    ) {
        drawRect(Color(0xFF0E2A3A))

        // Fit the region into the panel at scale 1 (before user zoom/pan).
        val pad = 40f
        val fit = ((size.width - pad * 2) / worldWidth)
            .coerceAtMost((size.height - pad * 2) / worldHeight)
        val baseScale = fit * scale
        val basePan = Offset(
            (size.width - worldWidth * baseScale) / 2f + pan.x,
            (size.height - worldHeight * baseScale) / 2f + pan.y
        )
        fun toScreen(wx: Float, wy: Float): Offset =
            Offset(basePan.x + wx * baseScale, basePan.y + wy * baseScale)

        // Land: draw the real tiles, dimmed toward the sea so pins read.
        if (grid != null) {
            for (ty in 0 until grid.height) {
                for (tx in 0 until grid.width) {
                    val color = grid.tileColor(tx, ty) ?: continue
                    val p = toScreen(tx * grid.tileSize, ty * grid.tileSize)
                    val s = grid.tileSize * baseScale
                    if (p.x + s < -20f || p.x > size.width + 20f || p.y + s < -20f || p.y > size.height + 20f) continue
                    drawRect(Color(color), topLeft = p, size = Size(s, s))
                }
            }
        }

        // Station: the hub of the region's travel network.
        if (station != null) {
            val sp = toScreen(station.position.x, station.position.y)
            drawCircle(Color(0xFFFFD54F), radius = 7f * baseScale.coerceAtLeast(1f), center = sp)
            drawMapLabel(
                textMeasurer,
                "${station.name}  ${station.nameJp}",
                Offset(sp.x, sp.y + 16f * baseScale.coerceAtLeast(1f)),
                Color(0xFFFFD54F)
            )
        }

        // Discovered locations only — no fake pins for the undiscovered.
        for (location in region.locations) {
            if (location.id !in discovered) continue
            val p = toScreen(location.anchor.x, location.anchor.y)
            val color = locationColor(location.kind)
            drawCircle(color, radius = 5f * baseScale.coerceAtLeast(1f), center = p)
            drawMapLabel(
                textMeasurer,
                "${location.name}  ${location.nameJp}",
                Offset(p.x, p.y - 12f * baseScale.coerceAtLeast(1f)),
                Color.White
            )
        }

        // "You are here" — only when the player is in this region.
        if (session.player.state.regionId == region.id) {
            val pp = toScreen(session.player.entity.position.x, session.player.entity.position.y)
            drawCircle(Color.White, radius = 8f * baseScale.coerceAtLeast(1f), center = pp)
            drawCircle(Color(0xFF40C4FF), radius = 5f * baseScale.coerceAtLeast(1f), center = pp)
            drawMapLabel(textMeasurer, "You are here", Offset(pp.x, pp.y - 16f * baseScale.coerceAtLeast(1f)), Color(0xFF40C4FF))
        }
    }
}

/** Pin colour per location kind — matches the world's reading of places. */
private fun locationColor(kind: ua.syt0r.kanji.desktop.game.world.LocationKind): Color = when (kind) {
    ua.syt0r.kanji.desktop.game.world.LocationKind.Station -> Color(0xFFFFD54F)
    ua.syt0r.kanji.desktop.game.world.LocationKind.Beach -> Color(0xFF4FC3F7)
    ua.syt0r.kanji.desktop.game.world.LocationKind.Park -> Color(0xFF81C784)
    ua.syt0r.kanji.desktop.game.world.LocationKind.Temple -> Color(0xFFEF5350)
    ua.syt0r.kanji.desktop.game.world.LocationKind.Shop -> Color(0xFFFFB74D)
    ua.syt0r.kanji.desktop.game.world.LocationKind.Landmark -> Color(0xFFBA68C8)
    else -> Color(0xFF90CAF9)
}

// ------------------------------------------------------------
// Shared label drawing
// ------------------------------------------------------------

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMapLabel(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    at: Offset,
    color: Color,
    bg: Color = Color.Black.copy(alpha = 0.55f)
) {
    if (text.isBlank()) return
    val layout = textMeasurer.measure(
        AnnotatedString(text),
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
    )
    val w = layout.size.width.toFloat()
    val h = layout.size.height.toFloat()
    val topLeft = Offset(at.x - w / 2f - 5f, at.y - h / 2f - 2f)
    if (bg.alpha > 0f) {
        drawRoundRect(
            color = bg,
            topLeft = topLeft,
            size = Size(w + 10f, h + 4f),
            cornerRadius = CornerRadius(4f)
        )
    }
    drawText(layout, topLeft = Offset(topLeft.x + 5f, topLeft.y + 2f))
}
