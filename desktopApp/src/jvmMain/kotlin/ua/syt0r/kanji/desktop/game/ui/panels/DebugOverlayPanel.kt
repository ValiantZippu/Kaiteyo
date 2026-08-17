package ua.syt0r.kanji.desktop.game.ui.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.time.Season
import ua.syt0r.kanji.desktop.game.time.WeatherKind

/**
 * Optional dev overlay (spec §122) — never shown to normal players. F3 in
 * the game toggles it; debug tools live behind it.
 */
@Composable
fun DebugOverlayPanel(session: GameSession) {
    val debug = session.debug
    if (!debug.enabled) return

    Column(
        modifier = Modifier
            .padding(DsSpacing.Lg)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(DsSpacing.Md)
    ) {
        val lines = listOf(
            "FPS ${debug.fps.toInt()} · avg ${debug.avgFrameTimeMs.toInt()}ms · p95 ${debug.p95FrameTimeMs.toInt()}ms · max ${debug.maxFrameTimeMs.toInt()}ms",
            "cells loaded: ${debug.loadedCells} · entities: ${debug.entityCount}",
            "cell: ${debug.currentCellId} · region: ${debug.currentRegionId}",
            "active quests: ${debug.activeQuests}",
            "content errors: ${session.validator.errors.size} · warnings: ${session.validator.warnings.size}",
            "F3 hide · [debug: node ids ${debug.showNodeIds}, bounds ${debug.showInteractionBounds}, collision ${debug.showCollision}]"
        )
        lines.forEach { line ->
            Text(
                text = line,
                color = Color(0xFF9BE7FF),
                fontSize = DsType.Caption,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
        // Frame-time sparkline — a 2 s rolling window so frame pacing is
        // visible at a glance (spec §94: measure, then optimize).
        FrameTimeSparkline(debug.frameTimes)
        // Season / weather / time forcing (spec §121-122) — the fastest way
        // to reach seasonal content while testing.
        Text(
            text = "Season: ${session.seasons.current.label}",
            color = Color(0xFFFFD54F),
            fontSize = DsType.Caption,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = DsSpacing.Sm)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            Season.entries.forEach { season ->
                DebugChip(season.label) { session.debugForceSeason(season) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            WeatherKind.entries.forEach { kind ->
                DebugChip(kind.label) { session.debugForceWeather(kind) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            DebugChip("Morning") { session.debugSetTime(8 * 60) }
            DebugChip("Day") { session.debugSetTime(12 * 60) }
            DebugChip("Evening") { session.debugSetTime(18 * 60) }
            DebugChip("Night") { session.debugSetTime(22 * 60) }
        }
        // Teleport (spec §121): jump to any discovered location instantly.
        val discovered = session.state.worldState.discoveredLocations
        if (discovered.isNotEmpty()) {
            Text(
                text = "Teleport (discovered locations)",
                color = Color(0xFFFFD54F),
                fontSize = DsType.Caption,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = DsSpacing.Sm)
            )
            val chips = session.world.allLocations().filter { it.id in discovered }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                chips.take(6).forEach { location ->
                    DebugChip(location.name) { session.debugTeleportToLocation(location.id) }
                }
            }
        }
    }
}

/**
 * A tiny bar chart of the last ~120 frame times (ms). The 16.7 ms target
 * (60 Hz) is drawn as a reference line so sustained spikes are obvious.
 */
@Composable
private fun FrameTimeSparkline(times: List<Float>) {
    if (times.isEmpty()) return
    val capMs = 50f // clamp so one bad frame doesn't squash the rest
    Canvas(modifier = Modifier.fillMaxWidth().height(22.dp)) {
        val max = times.maxOrNull()?.coerceAtMost(capMs)?.coerceAtLeast(1f) ?: 1f
        val step = size.width / times.size
        times.forEachIndexed { index, ms ->
            val t = ms.coerceAtMost(capMs)
            val h = (t / max) * size.height
            drawRect(
                color = if (ms > 33f) Color(0xFFFF8A80) else Color(0xFF69F0AE),
                topLeft = Offset(index * step, size.height - h),
                size = androidx.compose.ui.geometry.Size((step * 0.8f).coerceAtLeast(1f), h)
            )
        }
        // 60 Hz reference line.
        val y = size.height - (16.7f / max) * size.height
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }
}

@Composable
private fun DebugChip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = DsType.Caption,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = DsSpacing.Sm, vertical = 3.dp)
    )
}
