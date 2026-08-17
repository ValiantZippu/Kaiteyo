package ua.syt0r.kanji.desktop.game.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.bridge.BridgeLookup
import ua.syt0r.kanji.desktop.game.learning.KnowledgeNode

/**
 * In-game dictionary (spec §63, §17): every knowledge node the world can
 * teach, with the entry's real senses, kanji detail and pitch accents from
 * Kaiteyo's dictionary through the bridge. Search filters the world's own
 * vocabulary; discovered words are starred; anything can be mined straight
 * into the Kaiteyo deck.
 */
@Composable
fun DictionaryView(
    session: GameSession,
    onOpenInKaiteyo: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val nodes = session.knowledgeGraph.nodes
        .sortedBy { node -> !session.learning.isDiscovered(node.id) }
    val filtered = nodes.filter { node ->
        query.isBlank() ||
            node.headword.contains(query) ||
            node.reading.contains(query) ||
            node.meaning.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Text(
            text = "Dictionary",
            color = Color(0xFFFFD54F),
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search 単語 — headword, reading or meaning", color = Color.White.copy(alpha = 0.4f)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        for (node in filtered) {
            EntryCard(session, node, onOpenInKaiteyo)
        }
    }
}

@Composable
private fun EntryCard(
    session: GameSession,
    node: KnowledgeNode,
    onOpenInKaiteyo: (String) -> Unit
) {
    val discovered = session.learning.isDiscovered(node.id)
    val lookup = remember(node.id) { session.bridge.lookup(node.lookupKey()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(if (discovered) Color(0xFFFFD54F).copy(alpha = 0.07f) else Color.White.copy(alpha = 0.03f))
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "${node.headword}  ${node.reading}",
                color = Color.White,
                fontSize = DsType.Body,
                fontWeight = FontWeight.SemiBold
            )
            if (discovered) {
                Text("★ discovered", color = Color(0xFFFFD54F), fontSize = DsType.Caption)
            }
        }
        lookup?.let { entry -> EntryDetail(entry) }
        if (lookup == null) {
            Text(
                text = node.meaning,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = DsType.Caption
            )
        }
        node.sentence?.let { sentence ->
            Text(
                text = "${sentence.jp} — ${sentence.translation}",
                color = Color(0xFFFFF3E0).copy(alpha = 0.7f),
                fontSize = DsType.Caption
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            DsButton(
                text = if (session.learning.mined.contains(node.id)) "Mined ✓" else "Mine to deck",
                kind = if (session.learning.mined.contains(node.id)) DsButtonKind.Ghost else DsButtonKind.AccentTint,
                onClick = { session.learning.mine(node.id) }
            )
            DsButton(
                text = "Open in Kaiteyo",
                kind = DsButtonKind.Ghost,
                onClick = { onOpenInKaiteyo(node.lookupKey()) }
            )
        }
    }
}

@Composable
private fun EntryDetail(entry: BridgeLookup) {
    if (entry.senses.isNotEmpty()) {
        entry.senses.take(3).forEach { sense ->
            val pos = sense.partOfSpeech.joinToString("/")
            val glosses = sense.glosses.joinToString("; ")
            if (glosses.isNotBlank()) {
                Text(
                    text = if (pos.isBlank()) glosses else "$pos — $glosses",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = DsType.Caption
                )
            }
        }
    } else if (entry.meaning.isNotBlank()) {
        Text(
            text = entry.meaning,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = DsType.Caption
        )
    }
    // Real pitch accents (spec §19): position 0 is 平板 (no downstep);
    // otherwise the accent sits on that mora.
    if (entry.pitchAccents.isNotEmpty()) {
        Text(
            text = entry.pitchAccents.joinToString(" · ") { pitch ->
                if (pitch.position == 0) "平板 (heiban)" else "accent ${pitch.position}"
            },
            color = Color(0xFF90CAF9).copy(alpha = 0.8f),
            fontSize = DsType.Caption
        )
    }
    entry.kanji.take(2).forEach { kanji ->
        val on = kanji.onReadings.joinToString("・").ifBlank { "—" }
        val kun = kanji.kunReadings.joinToString("・").ifBlank { "—" }
        val strokes = kanji.strokeCounts.firstOrNull()?.let { "$it strokes" } ?: ""
        Text(
            text = listOf(
                "${kanji.character}  on: $on  kun: $kun",
                strokes,
                kanji.radicals.take(3).joinToString("・").takeIf { it.isNotBlank() }?.let { "radicals: $it" } ?: ""
            ).filter { it.isNotBlank() }.joinToString("  "),
            color = Color(0xFF90CAF9).copy(alpha = 0.6f),
            fontSize = DsType.Caption
        )
    }
}
