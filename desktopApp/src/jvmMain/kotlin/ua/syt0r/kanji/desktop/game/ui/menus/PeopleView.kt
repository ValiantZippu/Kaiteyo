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

/**
 * People (spec §53): who the player has met, talked to and bonded with.
 * Relationship is light — met/talked/helped counts and an affinity score
 * that grows when dialogue touches an NPC's favorite topics. The topics
 * themselves are the Japanese the player is learning.
 */
@Composable
fun PeopleView(session: GameSession) {
    val npcs = session.npcDirector.allNpcs()
        .sortedByDescending { it.relationship.affinity }
        .sortedByDescending { it.relationship.met }
    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text(
            text = "People",
            color = Color(0xFFFFD54F),
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Talk to people about what they love — the words come back as friendship.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = DsType.Caption
        )
        for (npc in npcs) {
            PersonCard(session, npc)
        }
    }
}

@Composable
private fun PersonCard(
    session: GameSession,
    npc: ua.syt0r.kanji.desktop.game.npc.NpcRuntime
) {
    val rel = npc.relationship
    val met = rel.met
    val topics = rel.favoriteTopics
        .mapNotNull { session.knowledgeGraph.node(it) }
        .joinToString("  ") { "${it.headword}（${it.reading}）" }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(if (met) Color(0xFFFFD54F).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${npc.definition.name}  ${npc.definition.nameJp}",
                color = if (met) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.7f),
                fontSize = DsType.Body,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (met) "met · talked ${rel.talkedCount} · ♥ ${rel.affinity}"
                else "not met yet",
                color = if (met) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.35f),
                fontSize = DsType.Caption
            )
        }
        Text(
            text = npc.definition.role,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = DsType.Caption
        )
        if (met && topics.isNotBlank()) {
            Text(
                text = "Loves: $topics",
                color = Color(0xFF90CAF9).copy(alpha = 0.8f),
                fontSize = DsType.Caption
            )
        }
    }
}
