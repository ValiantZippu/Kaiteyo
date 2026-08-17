package ua.syt0r.kanji.desktop.game.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.collection.CollectibleKind

/**
 * Collection (spec §46): stamps, postcards, phrases, locations, words,
 * souvenirs — grouped by kind with honest owned/total counts.
 */
@Composable
fun CollectionView(session: GameSession) {
    val manager = session.collections
    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        CollectibleKind.entries.forEach { kind ->
            val (owned, total) = manager.countByKind(kind)
            Text(
                text = "${kind.label}  —  $owned / $total",
                color = Color(0xFFFFD54F),
                fontSize = DsType.Label,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = DsSpacing.Sm)
            )
            val items = manager.all.filter { it.kind == kind }
            items.forEach { item ->
                val unlocked = manager.isUnlocked(item.id)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(Color.White.copy(alpha = if (unlocked) 0.07f else 0.02f))
                        .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
                ) {
                    Text(
                        text = if (unlocked) "${item.title}${if (item.titleJp.isNotBlank()) "  ${item.titleJp}" else ""}" else "???",
                        color = if (unlocked) Color.White else Color.White.copy(alpha = 0.3f),
                        fontSize = DsType.Body,
                        fontWeight = if (unlocked) FontWeight.Medium else FontWeight.Normal
                    )
                    if (unlocked && item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = DsType.Caption
                        )
                    }
                }
            }
        }
    }
}
