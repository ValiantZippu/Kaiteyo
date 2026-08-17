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
import ua.syt0r.kanji.desktop.game.learning.AssistanceLevel
import ua.syt0r.kanji.desktop.game.learning.DiscoveryEvent

/**
 * The discovery card (spec §108): 犬 · いぬ · dog, its learning chain, and
 * "Open in Kaiteyo" — the game hands the player back to the dictionary.
 */
@Composable
fun KnowledgeDiscoveryPanel(
    session: GameSession,
    onOpenInKaiteyo: (String) -> Unit
) {
    val event = session.state.discoveryQueue.firstOrNull() ?: return
    val textScale = session.settings.textSizeScale

    Box(
        modifier = Modifier.fillMaxWidth().padding(DsSpacing.Xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(DsRadius.Lg))
                .background(Color(0xFF1E272E).copy(alpha = 0.94f))
                .padding(DsSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            Text(
                text = "New discovery!",
                color = Color(0xFFFFD54F),
                fontSize = DsType.Label * textScale,
                fontWeight = FontWeight.SemiBold
            )
            DiscoveryBody(event, textScale)
            // Enrichment badges (spec §63, §108): what kind of node this is
            // and what Kaiteyo's dictionary says about it.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(event.node.kind.label)
                event.bridgeLookup?.jlpt?.let { Badge("JLPT $it") }
                event.bridgeLookup?.dictionaryName?.takeIf { it.isNotBlank() }?.let { Badge(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = "Open in Kaiteyo",
                    kind = DsButtonKind.Primary,
                    onClick = { onOpenInKaiteyo(event.node.lookupKey()) }
                )
                DsButton(
                    text = "Mine to deck",
                    kind = DsButtonKind.AccentTint,
                    onClick = { session.mineDiscovery(event.node.id) }
                )
                DsButton(
                    text = "Close",
                    kind = DsButtonKind.Ghost,
                    onClick = { session.dismissDiscovery() }
                )
            }
        }
    }
}

@Composable
private fun DiscoveryBody(event: DiscoveryEvent, textScale: Float) {
    val node = event.node
    Text(
        text = node.headword,
        color = Color.White,
        fontSize = DsType.Heading * textScale,
        fontWeight = FontWeight.Bold
    )
    if (node.reading.isNotBlank() && event.supportLevel != AssistanceLevel.Minimal) {
        Text(
            text = node.reading,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = DsType.BodyLarge * textScale
        )
    }
    if (event.supportLevel == AssistanceLevel.Guided || event.supportLevel == AssistanceLevel.Kids) {
        Text(
            text = node.meaning,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = DsType.Body * textScale
        )
    }
    node.sentence?.let { sentence ->
        if (event.supportLevel != AssistanceLevel.Minimal) {
            Text(
                text = sentence.jp,
                color = Color(0xFFFFF3E0),
                fontSize = DsType.Body * textScale
            )
        }
    }
    if (event.chain.size > 1 && event.supportLevel != AssistanceLevel.Minimal) {
        Text(
            text = event.chain.joinToString(" → ") { it.headword },
            color = Color.White.copy(alpha = 0.5f),
            fontSize = DsType.Caption * textScale
        )
    }
    // Real dictionary data through the bridge (spec §17, §63): senses with
    // part of speech, and for kanji nodes the on/kun readings, stroke count
    // and radicals — all from Kaiteyo's dictionary, never fabricated.
    event.bridgeLookup?.let { lookup ->
        if (lookup.senses.isNotEmpty()) {
            lookup.senses.forEach { sense ->
                val pos = sense.partOfSpeech.joinToString("/")
                val glosses = sense.glosses.joinToString("; ")
                if (glosses.isNotBlank()) {
                    Text(
                        text = if (pos.isBlank()) glosses else "$pos — $glosses",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = DsType.Caption * textScale
                    )
                }
            }
        } else if (lookup.meaning.isNotBlank()) {
            Text(
                text = lookup.meaning,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = DsType.Caption * textScale
            )
        }
        lookup.kanji.take(2).forEach { kanji ->
            val on = kanji.onReadings.joinToString("・").ifBlank { "—" }
            val kun = kanji.kunReadings.joinToString("・").ifBlank { "—" }
            val strokes = kanji.strokeCounts.firstOrNull()?.let { "$it strokes" } ?: ""
            val radicals = kanji.radicals.take(3).joinToString("・")
            Text(
                text = listOf(
                    "${kanji.character}  on: $on  kun: $kun",
                    strokes,
                    radicals?.takeIf { it.isNotBlank() }?.let { "radicals: $it" } ?: ""
                ).filter { it.isNotBlank() }.joinToString("  "),
                color = Color(0xFF90CAF9).copy(alpha = 0.7f),
                fontSize = DsType.Caption * textScale
            )
        }
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text = text,
        color = Color(0xFF90CAF9),
        fontSize = DsType.Caption,
        modifier = Modifier
            .padding(end = DsSpacing.Xs, top = DsSpacing.Xs)
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(Color(0xFF90CAF9).copy(alpha = 0.14f))
            .padding(horizontal = DsSpacing.Sm, vertical = 2.dp)
    )
}

/** Human-readable kind label for the discovery badges. */
private val ua.syt0r.kanji.desktop.game.learning.KnowledgeKind.label: String
    get() = when (this) {
        ua.syt0r.kanji.desktop.game.learning.KnowledgeKind.WORD -> "単語 word"
        ua.syt0r.kanji.desktop.game.learning.KnowledgeKind.KANJI -> "漢字 kanji"
        ua.syt0r.kanji.desktop.game.learning.KnowledgeKind.GRAMMAR -> "文法 grammar"
        ua.syt0r.kanji.desktop.game.learning.KnowledgeKind.SENTENCE -> "文 sentence"
    }
