package ua.syt0r.kanji.desktop.game.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession

/**
 * The stall ordering flow (spec §56): a Japanese menu, the player picks an
 * item by its Japanese name (reading + meaning help below), and the order
 * completes. This is the "order food" minigame — real use, not arcade.
 */
@Composable
fun OrderPanel(session: GameSession) {
    val order = session.order
    if (!session.state.orderOpen || !order.isActive) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(DsRadius.Lg))
                .background(Color(0xFF1B2233).copy(alpha = 0.97f))
                .padding(DsSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Text(
                text = "屋台のメニュー  —  Stall menu",
                color = Color(0xFFFFD54F),
                fontSize = DsType.Body,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Choose from the Japanese menu. 何にしますか？",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = DsType.Caption
            )

            val lastOrdered = order.lastOrdered
            if (lastOrdered != null) {
                Text(
                    text = "ありがとう！ ${lastOrdered.nameJp} coming right up.",
                    color = Color(0xFFA5D6A7),
                    fontSize = DsType.Body,
                    fontWeight = FontWeight.Medium
                )
            } else {
                order.items.forEach { item ->
                    OrderRow(item.nameJp, item.reading, item.meaning) {
                        session.chooseOrderItem(item.id)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (lastOrdered != null) "Close" else "Leave",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = DsType.Label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .clickable { session.closeOrder() }
                        .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
                )
            }
        }
    }
}

@Composable
private fun OrderRow(nameJp: String, reading: String, meaning: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(if (hovered) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = nameJp, color = Color.White, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
            Text(text = reading, color = Color(0xFF90CAF9), fontSize = DsType.Caption)
        }
        Text(text = meaning, color = Color.White.copy(alpha = 0.65f), fontSize = DsType.Caption)
    }
}
