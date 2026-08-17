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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.player.PlayerState

/**
 * Character (spec §11, §68, §88): level, honest progress stats and cosmetics
 * — the level represents "I know more Japanese and explored more of the
 * world", never a grind score.
 */
@Composable
fun CharacterView(session: GameSession) {
    val player = session.player.state
    val state = session.state
    val nextXp = PlayerState.xpForNext(player.level)

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text(
            text = "Level ${player.level}",
            color = Color(0xFFFFD54F),
            fontSize = DsType.Heading,
            fontWeight = FontWeight.Bold
        )
        // XP as a modest progress bar — no giant stat spreadsheet (spec §4).
        Text(
            text = "XP ${player.xp} / $nextXp",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = DsType.Caption
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DsRadius.Sm))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(2.dp)
        ) {
            val fraction = (player.xp.toFloat() / nextXp).coerceIn(0f, 1f)
            Row(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(DsRadius.Sm))
                    .background(Color(0xFFFFD54F))
                    .padding(vertical = 4.dp)
            ) {}
        }

        StatRow("Words discovered", session.learning.wordsLearned.toString())
        StatRow("Kanji discovered", session.learning.kanjiDiscovered.toString())
        StatRow("Quests completed", state.stats.questsCompleted.toString())
        StatRow("Photos taken", state.stats.photosTaken.toString())
        StatRow("Locations discovered", state.stats.locationsDiscovered.toString())
        StatRow("Active play time", "${session.activeSeconds / 60} min")

        if (player.cosmetics.isNotEmpty()) {
            Text(
                text = "Outfits",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = DsType.Label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = DsSpacing.Sm)
            )
            player.cosmetics.forEach { cosmetic ->
                Text(
                    text = "◈ $cosmetic",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = DsType.Body
                )
            }
        }

        if (player.inventory.isNotEmpty()) {
            Text(
                text = "Items",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = DsType.Label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = DsSpacing.Sm)
            )
            player.inventory.forEach { stack ->
                Text(
                    text = "• ${stack.itemId} ×${stack.count}",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = DsType.Body
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = DsType.Body)
        Text(value, color = Color.White, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
    }
}
