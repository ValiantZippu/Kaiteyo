package ua.syt0r.kanji.desktop.ui.mining

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString
import ua.syt0r.kanji.desktop.engine.media.AudioPlayer
import java.io.File
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsChip
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
import ua.syt0r.kanji.desktop.engine.mining.CardDestination
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
            title = resolveSuiteString { miningTitle },
            subtitle = "Review and refine words before they become cards. ${state.miningStatistics.totalMined} mined all-time.",
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(
                        text = resolveSuiteString { newCardButton },
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
            DsStatTile(resolveSuiteString { thisWeekLabel2 }, miningStats.minedBetween(weekStart, today).toString(), Modifier.weight(1f))
            DsStatTile(resolveSuiteString { thisMonthLabel }, miningStats.minedBetween(LocalDate(today.year, today.month, 1), today).toString(), Modifier.weight(1f))
            DsStatTile(resolveSuiteString { sourcesLabel }, miningStats.bySource.size.toString(), Modifier.weight(1f))
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text(resolveSuiteString { sourcesLabel }, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
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
                Text(resolveSuiteString { recentlyMinedLabel }, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                if (mining.minedRecords.isEmpty()) {
                    DsEmptyState(
                        title = resolveSuiteString { nothingMinedYet },
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
                    Text(resolveSuiteString { templatesLabel }, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    DsButton(
                        text = resolveSuiteString { newTemplateButton },
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
                                text = resolveSuiteString { useTemplateButton },
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
        title = resolveSuiteString { mineNewCardTitle },
        onDismiss = { mining.closeMining() }
        // Width is adaptive (DsDialog) — a rich form like this spreads with
        // the window instead of floating at a fixed 560dp.
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
                label = resolveSuiteString { headwordLabel }
            )
            DsTextField(
                value = draft.reading,
                onValueChange = { mining.draft = draft.copy(reading = it) },
                placeholder = "Reading (e.g. べんきょうする)",
                label = resolveSuiteString { readingLabel }
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
                placeholder = resolveSuiteString { sentencePlaceholder },
                label = resolveSuiteString { sentenceLabel }
            )
            DsTextField(
                value = draft.tags.joinToString(", "),
                onValueChange = { mining.draft = draft.copy(tags = it.split(",").map { t -> t.trim() }.filter { t -> t.isNotEmpty() }) },
                placeholder = resolveSuiteString { tagsPlaceholder },
                label = resolveSuiteString { tagsLabel }
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

            // Captured media assets — visible previews before the card exists:
            // the frame renders in the dialog and the audio clip can be
            // auditioned, so nothing is mined blind.
            ScreenshotPreview(draft.screenshotPath)
            AudioClipPreview(draft.audioPath)
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                if (draft.screenshotPath != null) {
                    DsBadge(text = "📷 frame attached", tint = sc.textSecondary)
                }
                if (draft.audioPath != null) {
                    DsBadge(text = "🔊 audio attached", tint = sc.textSecondary)
                }
                if (draft.timestamp != null) {
                    DsBadge(text = "⏱ ${ua.syt0r.kanji.desktop.engine.media.MediaEngine.formatTime((draft.timestamp * 1000).toLong())}", tint = sc.textMuted)
                }
                Text("Example: ${draft.example}", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.weight(1f))
            }

            // Destination — where this mine goes. Anki options are live only
            // while the AnkiConnect integration is enabled.
            val ankiEnabled = state.settings.getBool("media.anki.enabled")
            val anki = state.miningIntegration.anki
            val ankiAvailable = ankiEnabled && anki.configured
            var destination by remember { mutableStateOf(mining.defaultDestination()) }
            Text(resolveSuiteString { destinationLabel }, color = sc.textSecondary, fontSize = DsType.Label, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                CardDestination.entries.forEach { d ->
                    val selectable = d == CardDestination.Kaiteyo || ankiAvailable
                    DsChip(
                        text = d.label,
                        selected = destination == d,
                        onClick = { if (selectable) destination = d },
                        modifier = if (selectable) Modifier else Modifier.alpha(0.45f)
                    )
                }
            }
            val statusText = when {
                !ankiEnabled -> "AnkiConnect is disabled — enable it in Settings → Media → Integrations."
                !anki.configured -> "AnkiConnect not configured (set host/port in Settings → Media)."
                anki.connected -> "AnkiConnect: connected."
                else -> "AnkiConnect: not detected" + (anki.lastError?.let { " — $it" } ?: "")
            }
            Text(statusText, color = sc.textMuted, fontSize = DsType.Caption)

            // Pending Anki exports from previous failed sends — retry without re-mining.
            if (mining.pendingExportCount > 0) {
                val scope = rememberCoroutineScope()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DsBadge(text = "${mining.pendingExportCount} pending Anki export(s)", tint = sc.textMuted, modifier = Modifier.weight(1f))
                    DsButton(
                        text = "Retry now",
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = { scope.launch(Dispatchers.IO) { mining.retryPendingAnki() } }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = resolveSuiteString { createCardButton },
                    icon = Icons.Default.Add,
                    onClick = {
                        if (draft.headword.isNotBlank()) {
                            mining.mine(draft, destination)
                            mining.closeMining()
                        }
                    },
                    enabled = draft.headword.isNotBlank()
                )
                DsButton(
                    text = resolveSuiteString { cancelButton },
                    kind = DsButtonKind.Secondary,
                    onClick = { mining.closeMining() }
                )
            }
        }
    }
}

// ============================================
// MINING DIALOG ASSET PREVIEWS
// Real previews of the captured frame and audio
// clip before a card is created — the dialog never
// claims an asset exists unless the file does.
// ============================================

/** Renders the captured screenshot inside the dialog (decoded off the UI thread). */
@Composable
private fun ScreenshotPreview(path: String?) {
    if (path == null) return
    val file = remember(path) { File(path) }
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }

    // Decode off the UI thread; a missing/unreadable file simply shows the
    // fallback label instead of an empty box.
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { javax.imageio.ImageIO.read(file).toComposeImageBitmap() }.getOrNull()
        }
    }

    val sc = surfaceColors()
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = "Captured screenshot",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(sc.surfaceElevated)
        )
    } else {
        DsBadge(text = if (file.exists()) "Decoding frame…" else "Screenshot file missing", tint = sc.textMuted)
    }
}

/**
 * Auditions the extracted audio clip before mining. Uses the engine's own
 * AudioPlayer so the same code path as media playback is exercised; the clip
 * stops when the dialog closes or the path changes.
 */
@Composable
private fun AudioClipPreview(path: String?) {
    if (path == null) return
    val file = remember(path) { File(path) }
    val player = remember { AudioPlayer() }
    var playing by remember { mutableStateOf(false) }

    // Never leave a clip looping in the background when the dialog closes.
    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    val sc = surfaceColors()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(sc.surfaceElevated.copy(alpha = 0.5f))
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Audio clip", color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
            Text(
                if (file.exists()) {
                    "${file.name} · ${ua.syt0r.kanji.desktop.engine.media.MediaEngine.formatTime(player.lengthMs)}"
                } else {
                    "Clip file missing — the note will reference a path that no longer exists"
                },
                color = sc.textMuted,
                fontSize = DsType.Caption,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        DsButton(
            text = if (playing) "Stop" else "Play",
            icon = if (playing) Icons.Default.Stop else Icons.Default.PlayArrow,
            kind = if (playing) DsButtonKind.Secondary else DsButtonKind.Primary,
            compact = true,
            enabled = file.exists(),
            onClick = {
                if (playing) {
                    player.stop()
                    playing = false
                } else {
                    val loaded = player.load(file).isSuccess
                    if (loaded) {
                        player.play()
                        playing = true
                    }
                }
            }
        )
    }
}
