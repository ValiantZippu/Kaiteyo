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

/** Quest complete celebration (spec §22, §87) — rewards, then keep exploring. */
@Composable
fun QuestCompletePanel(session: GameSession) {
    val quest = session.state.completedQuest ?: return
    val textScale = session.settings.textSizeScale

    Box(
        modifier = Modifier.fillMaxWidth().padding(DsSpacing.Xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .clip(RoundedCornerShape(DsRadius.Lg))
                .background(Color(0xFF1E272E).copy(alpha = 0.94f))
                .padding(DsSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            Text(
                text = "Quest complete!",
                color = Color(0xFFFFD54F),
                fontSize = DsType.Label * textScale,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = quest.title,
                color = Color.White,
                fontSize = DsType.Heading * textScale,
                fontWeight = FontWeight.Bold
            )
            if (quest.titleJp.isNotBlank()) {
                Text(
                    text = quest.titleJp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = DsType.Body * textScale
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                if (quest.rewards.xp > 0) {
                    RewardChip("+${quest.rewards.xp} XP", textScale)
                }
                if (quest.rewards.unlocks.isNotEmpty()) {
                    RewardChip("+${quest.rewards.unlocks.size} unlock", textScale)
                }
                if (quest.learningTargets.isNotEmpty()) {
                    RewardChip("+${quest.learningTargets.size} words", textScale)
                }
            }
            DsButton(
                text = "Continue",
                kind = DsButtonKind.Primary,
                onClick = { session.state.completedQuest = null }
            )
        }
    }
}

@Composable
private fun RewardChip(text: String, textScale: Float) {
    Text(
        text = text,
        color = Color(0xFFA5D6A7),
        fontSize = DsType.Label * textScale,
        modifier = Modifier
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = DsSpacing.Md, vertical = 4.dp)
    )
}
