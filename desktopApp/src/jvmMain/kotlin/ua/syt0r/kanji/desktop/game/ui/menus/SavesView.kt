package ua.syt0r.kanji.desktop.game.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * Save slots (spec §97): one journey per slot. Lists every slot with its
 * saved time, lets the player save the current journey into any slot, load a
 * different journey, delete a slot, or start a fresh journey in an empty one.
 */
@Composable
fun SavesView(session: GameSession) {
    val slots = session.saveManager.listSlots()

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text(
            text = "Save slots",
            color = Color(0xFFFFD54F),
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Each slot holds a separate journey. The game autosaves into the active slot.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = DsType.Caption
        )

        // The active slot is always first.
        val ordered = buildList {
            add(session.activeSlot)
            addAll(slots.filter { it != session.activeSlot }.sorted())
        }.distinct()

        ordered.forEach { slot ->
            SlotRow(session, slot)
        }

        // Offer a fresh journey if there is room for one more slot.
        val nextSlot = "slot-${(slots.mapNotNull { it.removePrefix("slot-").toIntOrNull() }.maxOrNull() ?: 0) + 1}"
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsButton(
                text = "New journey ($nextSlot)",
                kind = DsButtonKind.Ghost,
                onClick = { session.newJourneyIn(nextSlot) }
            )
            DsButton(
                text = "Close",
                kind = DsButtonKind.Ghost,
                onClick = { session.state.closeAllPanels() }
            )
        }
    }
}

@Composable
private fun SlotRow(session: GameSession, slot: String) {
    val isActive = slot == session.activeSlot
    val exists = session.slotExists(slot)
    val savedAt = session.slotSavedAt(slot)?.take(19)?.replace('T', ' ')
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(if (isActive) Color(0xFFFFD54F).copy(alpha = 0.10f) else Color.White.copy(alpha = 0.04f))
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            Text(
                text = slot + if (isActive) "  ← active" else "",
                color = if (isActive) Color(0xFFFFD54F) else Color.White,
                fontSize = DsType.Body,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (exists) "Saved: ${savedAt ?: "—"}" else "Empty journey",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = DsType.Caption
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            if (!isActive) {
                DsButton(
                    text = if (exists) "Load" else "Start",
                    kind = DsButtonKind.Ghost,
                    onClick = { session.loadSlot(slot) }
                )
            }
            DsButton(
                text = "Save here",
                kind = if (isActive) DsButtonKind.Primary else DsButtonKind.Ghost,
                onClick = { session.saveToSlot(slot) }
            )
            if (exists && !isActive) {
                DsButton(
                    text = "Delete",
                    kind = DsButtonKind.Ghost,
                    onClick = { session.deleteSlot(slot) }
                )
            }
        }
    }
}
