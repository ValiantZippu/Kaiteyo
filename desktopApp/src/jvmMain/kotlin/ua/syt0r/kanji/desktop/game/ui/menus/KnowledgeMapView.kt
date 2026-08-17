package ua.syt0r.kanji.desktop.game.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.learning.KnowledgeNode

/**
 * The knowledge map (spec §73-74): a visual word graph. Every discovered
 * word opens its neighbourhood — 食 → 食べる → 食べました — and each node
 * resolves back to Kaiteyo's dictionary. Navigate indefinitely.
 */
@Composable
fun KnowledgeMapView(
    session: GameSession,
    onOpenInKaiteyo: (String) -> Unit
) {
    val graph = session.knowledgeGraph
    val discovered = session.learning.discovered
    var selectedId by remember { mutableStateOf<String?>(null) }

    val nodes = graph.nodes.filter { it.id in discovered }.sortedBy { it.headword }

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        if (nodes.isEmpty()) {
            Text(
                text = "No words discovered yet — inspect signs and talk to people.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = DsType.Body
            )
            return
        }
        Text(
            text = "${nodes.size} words discovered",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = DsType.Caption
        )
        nodes.forEach { node ->
            NodeCard(
                node = node,
                selected = node.id == selectedId,
                onClick = { selectedId = if (selectedId == node.id) null else node.id }
            )
            if (node.id == selectedId) {
                Text(
                    text = "Open in Kaiteyo →",
                    color = Color(0xFFFFD54F),
                    fontSize = DsType.Caption,
                    modifier = Modifier
                        .padding(start = DsSpacing.Lg)
                        .clickable { onOpenInKaiteyo(node.lookupKey()) }
                )
                NeighbourList(graph.neighbours(node.id))
            }
        }
    }
}

@Composable
private fun NodeCard(node: KnowledgeNode, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(
                when {
                    selected -> Color(0xFFFFD54F).copy(alpha = 0.15f)
                    hovered -> Color.White.copy(alpha = 0.09f)
                    else -> Color.White.copy(alpha = 0.05f)
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Text(
            text = node.headword,
            color = Color.White,
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.Bold
        )
        Column {
            if (node.reading.isNotBlank()) {
                Text(node.reading, color = Color.White.copy(alpha = 0.7f), fontSize = DsType.Caption)
            }
            if (node.meaning.isNotBlank()) {
                Text(node.meaning, color = Color.White.copy(alpha = 0.55f), fontSize = DsType.Caption)
            }
        }
        Text(
            text = if (selected) "▾" else "▸",
            color = Color(0xFFFFD54F),
            fontSize = DsType.Body,
            modifier = Modifier.padding(start = DsSpacing.Sm)
        )
    }
}

@Composable
private fun NeighbourList(neighbours: List<KnowledgeNode>) {
    if (neighbours.isEmpty()) {
        Text(
            text = "No connected nodes yet.",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = DsType.Caption,
            modifier = Modifier.padding(start = DsSpacing.Lg)
        )
        return
    }
    Column(
        modifier = Modifier.padding(start = DsSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        neighbours.forEach { neighbour ->
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text(
                    text = "↳ ${neighbour.headword}",
                    color = Color(0xFF90CAF9),
                    fontSize = DsType.Body
                )
                Text(
                    text = neighbour.meaning,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = DsType.Caption,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
