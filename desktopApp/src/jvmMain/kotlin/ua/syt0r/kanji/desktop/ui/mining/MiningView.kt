package ua.syt0r.kanji.desktop.ui.mining

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsStatTile
import ua.syt0r.kanji.desktop.designsystem.DsTextArea
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload
import ua.syt0r.kanji.desktop.engine.mining.MiningSource
import ua.syt0r.kanji.desktop.engine.mining.MiningTemplate

// ============================================
// KAITEYO MINING WORKSPACE
// The card-creation hub. Every source (dictionary,
// browser, subtitle, OCR, clipboard, media, API)
// lands here for review, editing and one-click
// creation into the study deck. Templates make the
// power-user workflow fast.
// ============================================

@Composable
fun MiningView(state: AppState) {
    val sc = surfaceColors()
    val mining = state.mining

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = "Mining",
            subtitle = "Review and refine words before they become cards. ${state.miningStatistics.totalMined} mined all-time.",
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(
                        text = "New card",
                        icon = Icons.Default.Add,
                        onClick = { mining.openMining(MiningPayload(headword = "")) }
                    )
                }
            }
        )

        // Mining volume at a glance — every source, real records.
        val miningStats = state.miningStatistics
        val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
        val weekStart = today.minus(6, DateTimeUnit.DAY)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsStatTile("Mined all-time", miningStats.totalMined.toString(), Modifier.weight(1f))
            DsStatTile("This week", miningStats.minedBetween(weekStart, today).toString(), Modifier.weight(1f))
            DsStatTile("This month", miningStats.minedBetween(LocalDate(today.year, today.month, 1), today).toString(), Modifier.weight(1f))
            DsStatTile("Sources", miningStats.bySource.size.toString(), Modifier.weight(1f))
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Sources", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                ) {
                    mining.sourceOptions.forEach { source ->
                        DsButton(
                            text = source.label,
                            kind = DsButtonKind.Secondary,
                            compact = true,
                            onClick = {
                                mining.openMining(
                                    MiningPayload(
                                        headword = "",
                                        source = source.name.lowercase(),
                                        sourceDetail = source.label
                                    )
                                )
                            }
                        )
                    }
                }
                Text(
                    "Recent sources: " + mining.recentSources.joinToString(" → "),
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }

        // Recent mines feed
        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Recently mined", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                if (mining.minedRecords.isEmpty()) {
                    DsEmptyState(
                        title = "Nothing mined yet",
                        message = "Look up a word in the Dictionary workspace, select text in the Learning Browser, run OCR or use the local API to create your first mined card."
                    )
                } else {
                    mining.minedRecords.take(20).forEach { rec ->
                        Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                            Text(rec.headword, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            DsBadge(text = rec.source, tint = sc.textSecondary)
                            Text(rec.createdAt.take(10), color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.padding(start = DsSpacing.Md))
                            val event = state.media.miningEvents.firstOrNull { it.cardId == rec.cardId }
                            val sourceCard = state.cards.firstOrNull { it.id == rec.cardId }
                            if (event != null && sourceCard != null) {
                                DsIconButton(
                                    icon = Icons.Default.PlayArrow,
                                    onClick = { state.media.openFromCard(sourceCard) },
                                    contentDescription = "Open in Media at timestamp",
                                    size = 26.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Templates
        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Templates", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    DsButton(
                        text = "New template",
                        icon = Icons.Default.Add,
                        kind = DsButtonKind.Ghost,
                        compact = true,
                        onClick = {
                            mining.templates.add(
                                MiningTemplate(
                                    id = "tpl-${System.currentTimeMillis()}",
                                    name = "Template ${mining.templates.size + 1}",
                                    description = "A reusable mining template.",
                                    tags = listOf("template")
                                )
                            )
                        }
                    )
                }
                if (mining.templates.isEmpty()) {
                    Text("No templates yet. Create one to bundle default tags and decks for recurring mining tasks.", color = sc.textMuted, fontSize = DsType.Caption)
                } else {
                    mining.templates.take(10).forEach { tpl ->
                        Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tpl.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                                if (tpl.description.isNotBlank()) {
                                    Text(tpl.description, color = sc.textMuted, fontSize = DsType.Caption)
                                }
                            }
                            DsButton(
                                text = "Use",
                                kind = DsButtonKind.Secondary,
                                compact = true,
                                onClick = {
                                    mining.openMining(
                                        MiningPayload(
                                            headword = "",
                                            source = tpl.source,
                                            tags = tpl.tags,
                                            deckId = tpl.deckId
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// MINING DIALOG — create/edit a card from a payload
// ============================================

@Composable
fun MiningDialog(state: AppState) {
    val mining = state.mining
    val draft = mining.draft
    val sc = surfaceColors()

    DsDialog(
        title = "Mine a new card",
        onDismiss = { mining.closeMining() },
        modifier = Modifier.width(560.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsBadge(text = draft.source, tint = sc.textSecondary)
                if (draft.sourceDetail.isNotBlank()) {
                    DsBadge(text = draft.sourceDetail, tint = sc.textMuted)
                }
            }
            DsTextField(
                value = draft.headword,
                onValueChange = { mining.draft = draft.copy(headword = it) },
                placeholder = "Word (e.g. 勉強する)",
                label = "Headword"
            )
            DsTextField(
                value = draft.reading,
                onValueChange = { mining.draft = draft.copy(reading = it) },
                placeholder = "Reading (e.g. べんきょうする)",
                label = "Reading"
            )
            DsTextArea(
                value = draft.definition,
                onValueChange = { mining.draft = draft.copy(definition = it) },
                modifier = Modifier.fillMaxWidth(),
                height = 96.dp
            )
            DsTextField(
                value = draft.sentence,
                onValueChange = { mining.draft = draft.copy(sentence = it) },
                placeholder = "Sentence context",
                label = "Sentence"
            )
            DsTextField(
                value = draft.tags.joinToString(", "),
                onValueChange = { mining.draft = draft.copy(tags = it.split(",").map { t -> t.trim() }.filter { t -> t.isNotEmpty() }) },
                placeholder = "tags, comma, separated",
                label = "Tags"
            )

            // Deck selection — mined cards land in a real Kaiteyo deck.
            val decks = state.library.allDecks()
            if (decks.isNotEmpty()) {
                val selectedDeck = remember(draft.deckId, decks) {
                    decks.firstOrNull { it.id == draft.deckId } ?: decks.first()
                }
                DsSelect(
                    selected = selectedDeck,
                    options = decks,
                    onSelected = { mining.draft = draft.copy(deckId = it.id) },
                    labelOf = { "Deck: ${it.name}" },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Captured media assets attach to the card note automatically.
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                if (draft.screenshotPath != null) {
                    DsBadge(text = "📷 screenshot", tint = sc.textSecondary)
                }
                if (draft.audioPath != null) {
                    DsBadge(text = "🔊 audio clip", tint = sc.textSecondary)
                }
                if (draft.timestamp != null) {
                    DsBadge(text = "⏱ ${ua.syt0r.kanji.desktop.engine.media.MediaEngine.formatTime((draft.timestamp * 1000).toLong())}", tint = sc.textMuted)
                }
                Text("Example: ${draft.example}", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = "Create card",
                    icon = Icons.Default.Add,
                    onClick = {
                        if (draft.headword.isNotBlank()) {
                            mining.mine(draft)
                            mining.closeMining()
                        }
                    },
                    enabled = draft.headword.isNotBlank()
                )
                DsButton(
                    text = "Cancel",
                    kind = DsButtonKind.Secondary,
                    onClick = { mining.closeMining() }
                )
            }
        }
    }
}
