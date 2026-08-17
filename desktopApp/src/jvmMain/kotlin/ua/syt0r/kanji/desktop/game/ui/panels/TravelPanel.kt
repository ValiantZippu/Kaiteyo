package ua.syt0r.kanji.desktop.game.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.world.Station

/**
 * Travel (spec §47-48): stations connect locations. The slice has one line —
 * the architecture supports schedules, platforms, announcements and route
 * maps (TODO). Destinations unlock through quest rewards.
 */
@Composable
fun TravelPanel(session: GameSession) {
    if (!session.state.travelOpen) return

    Box(
        modifier = Modifier.fillMaxWidth().padding(DsSpacing.Xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(DsRadius.Lg))
                .background(Color(0xFF1E272E).copy(alpha = 0.94f))
                .padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Text(
                text = "Travel — 駅",
                color = Color(0xFFFFD54F),
                fontSize = DsType.Heading,
                fontWeight = FontWeight.Bold
            )
            session.world.travel.stations.forEach { station ->
                StationRow(session, station)
            }
            DsButton(
                text = "Back",
                kind = DsButtonKind.Ghost,
                onClick = { session.state.travelOpen = false }
            )
        }
    }
}

@Composable
private fun StationRow(session: GameSession, station: Station) {
    val unlocked = session.isTravelUnlocked(station.id)
    val isHere = session.player.state.cellId == station.cellId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "${station.name}  ${station.nameJp}",
                color = Color.White,
                fontSize = DsType.Body,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (unlocked) "Available" else "Locked — keep exploring",
                color = if (unlocked) Color(0xFFA5D6A7) else Color.White.copy(alpha = 0.5f),
                fontSize = DsType.Caption
            )
        }
        if (isHere) {
            Text("You are here", color = Color(0xFFFFD54F), fontSize = DsType.Caption)
        } else {
            DsButton(
                text = "Travel",
                kind = if (unlocked) DsButtonKind.Primary else DsButtonKind.Ghost,
                enabled = unlocked,
                onClick = { session.travelTo(station.id) }
            )
        }
    }
}
