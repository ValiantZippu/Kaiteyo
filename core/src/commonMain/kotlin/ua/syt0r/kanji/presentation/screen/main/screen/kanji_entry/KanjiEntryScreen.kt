@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ua.syt0r.kanji.presentation.screen.main.screen.kanji_entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.knowledge.ComponentKnowledge
import ua.syt0r.kanji.core.knowledge.FrequencyBand
import ua.syt0r.kanji.core.knowledge.KanjiKnowledge
import ua.syt0r.kanji.core.knowledge.KanjiSetKind
import ua.syt0r.kanji.core.knowledge.setKind
import ua.syt0r.kanji.core.knowledge.KnowledgeGraph
import ua.syt0r.kanji.core.knowledge.KnowledgeGraphRepository
import ua.syt0r.kanji.core.knowledge.StudyEntry
import ua.syt0r.kanji.core.knowledge.StudyState
import ua.syt0r.kanji.core.knowledge.StudyStatusProvider
import ua.syt0r.kanji.core.knowledge.cards.KanjiCardLayout
import ua.syt0r.kanji.core.knowledge.cards.KanjiCardPresets
import ua.syt0r.kanji.core.knowledge.media.MediaReference
import ua.syt0r.kanji.core.knowledge.media.MediaReferenceStore
import ua.syt0r.kanji.core.knowledge.cards.KanjiCardType
import ua.syt0r.kanji.core.japanese.isKana
import ua.syt0r.kanji.core.japanese.kanaToRomaji
import ua.syt0r.kanji.core.knowledge.frequencyRankLabel
import ua.syt0r.kanji.core.knowledge.level.DisplayOverridesStore
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.KaiteyoPill
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionCard
import ua.syt0r.kanji.presentation.common.ui.KaiteyoTag
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.common.ui.knowledge.KnowledgeGraphCanvas
import ua.syt0r.kanji.presentation.common.ui.knowledge.studyStateColor
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.kanji_entry.KanjiEntryContract.ScreenState

// ============================================================
// KANJI ENTRY — SCREEN
// ------------------------------------------------------------
// The modular kanji page. Cards come from the persisted
// KanjiCardLayout; "Customize" switches to edit mode where each
// card can be shown/hidden, reordered, and presets applied.
// Every card renders real knowledge-core data.
// ============================================================

@Composable
fun KanjiEntryScreen(
    character: String,
    onClose: () -> Unit,
    onOpenWord: (Long) -> Unit,
    onOpenGraph: (() -> Unit)? = null,
    onOpenKanji: (String) -> Unit = {},
    onOpenSentence: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val viewModel = getMultiplatformViewModel<KanjiEntryContract.ViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(character) { viewModel.load(character) }

    ProvidePageIdentity(
        PageIdentity(id = "kanji_entry", name = "Kanji entry", route = "/kanji/$character", panel = null)
    ) {
        Column(modifier.fillMaxSize()) {
            EntryHeader(character = character, onClose = onClose)
            when (val current = state) {
                is ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ScreenState.Error -> KaiteyoEmptyState(
                    icon = "⚠️",
                    title = "Kanji unavailable",
                    message = current.message,
                    actionLabel = "Retry",
                    onAction = viewModel::retry
                )
                is ScreenState.Loaded -> EntryContent(
                    state = current,
                    onOpenWord = onOpenWord,
                    onOpenKanji = onOpenKanji,
                    onOpenGraph = onOpenGraph,
                    onOpenSentence = onOpenSentence,
                    onToggleCard = viewModel::toggleCard,
                    onMoveUp = viewModel::moveCardUp,
                    onMoveDown = viewModel::moveCardDown,
                    onSetCardLimit = viewModel::setCardLimit,
                    onApplyPreset = viewModel::applyPreset,
                    onToggleEdit = viewModel::setEditMode
                )
            }
        }
    }
}

@Composable
private fun EntryHeader(character: String, onClose: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = surfaceColors.textPrimary)
        }
        Text(character, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        Text("Kanji entry", style = MaterialTheme.typography.labelMedium, color = surfaceColors.textMuted)
    }
}

@Composable
private fun EntryContent(
    state: ScreenState.Loaded,
    onOpenWord: (Long) -> Unit,
    onOpenKanji: (String) -> Unit,
    onOpenGraph: (() -> Unit)?,
    onOpenSentence: ((String, String) -> Unit)?,
    onToggleCard: (KanjiCardType) -> Unit,
    onMoveUp: (KanjiCardType) -> Unit,
    onMoveDown: (KanjiCardType) -> Unit,
    onSetCardLimit: (KanjiCardType, Int) -> Unit,
    onApplyPreset: (String) -> Unit,
    onToggleEdit: (Boolean) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    // Per-card settings apply here: each content card receives only as many
    // items as the user configured (spec §21), so the stepper in edit mode
    // is a real control, not decoration.
    val context = KanjiCardContext(
        kanji = state.kanji,
        radicals = state.radicals,
        words = state.words.take(state.layout.exampleLimit(KanjiCardType.Vocabulary, 24)),
        related = state.related.take(state.layout.exampleLimit(KanjiCardType.Related, 18)),
        sentences = state.sentences.take(state.layout.exampleLimit(KanjiCardType.Sentence, 6)),
        grammar = state.grammar,
        onOpenWord = onOpenWord,
        onOpenKanji = onOpenKanji,
        onOpenSentence = onOpenSentence
    )

    Column(Modifier.fillMaxSize()) {
        // Edit-mode toolbar.
        if (state.editMode) {
            Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.Space3)) {
                Text(
                    text = "Customize cards — toggle visibility, drag to reorder (or use the arrows), or apply a preset.",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space2),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    KaiteyoPill(text = "Done", selected = false, onClick = { onToggleEdit(false) })
                    KanjiCardPresets.all.forEach { preset ->
                        KaiteyoPill(
                            text = preset.name,
                            selected = state.layout.hidden == preset.layout.hidden,
                            onClick = { onApplyPreset(preset.id) }
                        )
                    }
                }
            }
        }

        LazyColumn(
            // Responsive card layout (KT-CARD-004, spec §22): on wide windows
            // the card column is capped to a readable measure and centered —
            // cards never stretch edge-to-edge at 2000px, and narrow windows
            // still get the full width (no clipping, no negative padding).
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1080.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
        ) {
            state.layout.visibleCards().forEach { type ->
                item(key = type.id) {
                    CardSlot(
                        type = type,
                        editMode = state.editMode,
                        layout = state.layout,
                        onToggle = { onToggleCard(type) },
                        onMoveUp = { onMoveUp(type) },
                        onMoveDown = { onMoveDown(type) },
                        onSetCardLimit = { limit -> onSetCardLimit(type, limit) }
                    ) {
                        when (type) {
                            KanjiCardType.Hero -> HeroCard(context)
                            KanjiCardType.Meaning -> MeaningCard(context)
                            KanjiCardType.Readings -> ReadingsCard(context)
                            KanjiCardType.Frequency -> FrequencyCard(context)
                            KanjiCardType.Classification -> ClassificationCard(context)
                            KanjiCardType.Radical -> RadicalCard(context)
                            KanjiCardType.Component -> ComponentCard(context)
                            KanjiCardType.Stroke -> StrokeCard(context)
                            KanjiCardType.Vocabulary -> VocabularyCard(context)
                            KanjiCardType.Related -> RelatedCard(context)
                            KanjiCardType.Variant -> VariantCard(context)
                            KanjiCardType.Sentence -> SentenceCard(context)
                            KanjiCardType.Grammar -> GrammarCard(context)
                            KanjiCardType.Graph -> GraphCard(
                                character = state.character,
                                onOpenFullGraph = onOpenGraph
                            )
                            KanjiCardType.Media -> MediaCard(context)
                            KanjiCardType.Study -> StudyCard(character = state.character)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// CARD SLOT + CONTEXT
// ============================================================

private data class KanjiCardContext(
    val kanji: KanjiKnowledge,
    val radicals: List<ComponentKnowledge>,
    val words: List<ua.syt0r.kanji.core.knowledge.WordKnowledge>,
    val related: List<KanjiKnowledge>,
    val sentences: List<ua.syt0r.kanji.core.knowledge.SentenceKnowledge>,
    val grammar: List<ua.syt0r.kanji.core.knowledge.GrammarMatch>,
    val onOpenWord: (Long) -> Unit,
    val onOpenKanji: (String) -> Unit,
    val onOpenSentence: ((String, String) -> Unit)? = null
)

@Composable
private fun CardSlot(
    type: KanjiCardType,
    editMode: Boolean,
    layout: KanjiCardLayout,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSetCardLimit: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!editMode) {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
        return
    }

    // Per-card settings (spec §21): content cards expose an item-limit
    // stepper. The limit is persisted in the layout and applied by the card
    // itself (see VocabularyCard / RelatedCard / SentenceCard).
    val defaultLimit = defaultLimitFor(type)
    val limit = layout.exampleLimit(type, defaultLimit)

    // Drag-to-reorder: long-press the card header, then drag vertically. When
    // the accumulated offset crosses the midpoint of the next slot, the card
    // moves one position (persisted) and the offset is corrected by the slot
    // stride, so the dragged card follows the pointer without teleporting.
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val density = LocalDensity.current
    var dragOffsetY by remember { mutableStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var slotHeight by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (dragging) 1f else 0f)
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .onSizeChanged { slotHeight = it.height }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusSm))
                .background(if (dragging) accent.primary.copy(alpha = 0.10f) else Color.Transparent)
                .pointerInput(type.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            dragging = false
                            dragOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (slotHeight <= 0) return@detectDragGesturesAfterLongPress
                            val stride = (slotHeight + with(density) { Dimens.Space3.toPx() }).coerceAtLeast(1f)
                            val threshold = stride / 2f
                            dragOffsetY += dragAmount.y
                            while (dragOffsetY >= threshold) {
                                onMoveDown()
                                dragOffsetY -= stride
                            }
                            while (dragOffsetY <= -threshold) {
                                onMoveUp()
                                dragOffsetY += stride
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Drag to reorder",
                tint = if (dragging) accent.primary else surfaceColors.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(LocalKaiteyoAccent.current.primary)
            )
            Text(
                text = type.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = surfaceColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onMoveUp, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up", tint = surfaceColors.textMuted, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down", tint = surfaceColors.textMuted, modifier = Modifier.size(16.dp))
            }
            if (defaultLimit > 0) {
                // Compact item-limit stepper for content cards.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.RadiusSm))
                        .background(surfaceColors.surfaceInteractive)
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = surfaceColors.textSecondary,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    IconButton(onClick = { onSetCardLimit((limit - 1).coerceAtLeast(1)) }, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "Fewer items", tint = surfaceColors.textMuted, modifier = Modifier.size(12.dp))
                    }
                    IconButton(onClick = { onSetCardLimit((limit + 1).coerceAtMost(50)) }, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "More items", tint = surfaceColors.textMuted, modifier = Modifier.size(12.dp))
                    }
                }
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Hide card",
                    tint = surfaceColors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        content()
    }
}

/**
 * The default item limit for a content card (0 = not limitable). The layout
 * resolves missing per-card settings against this, so the persisted default
 * and the rendered default can never drift apart.
 */
private fun defaultLimitFor(type: KanjiCardType): Int = when (type) {
    KanjiCardType.Vocabulary -> 24
    KanjiCardType.Related -> 18
    KanjiCardType.Sentence -> 6
    else -> 0
}

// ============================================================
// CARDS
// ============================================================

@Composable
private fun HeroCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val kanji = context.kanji
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusXl))
            .background(surfaceColors.surface)
            .padding(Dimens.Space6),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(kanji.character, fontSize = 84.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        kanji.keyword?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, color = surfaceColors.textSecondary)
        }
        Spacer(Modifier.height(Dimens.Space3))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
            kanji.jlpt?.let { KaiteyoTag(text = it.label, tint = accent.primary) }
            kanji.classifications.filterIsInstance<ua.syt0r.kanji.core.knowledge.KanjiTag.Grade>()
                .firstOrNull()?.let { KaiteyoTag(text = it.label, tint = accent.secondary) }
            kanji.strokeCount?.let { KaiteyoTag(text = "$it strokes") }
        }
    }
}

@Composable
private fun MeaningCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    KaiteyoSectionCard(title = "Meanings") {
        context.kanji.meanings.forEach { meaning ->
            Text(
                text = "·  $meaning",
                style = MaterialTheme.typography.bodyMedium,
                color = surfaceColors.textPrimary,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ReadingsCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    // User-level romaji override (spec §24): shared with word pages — when on,
    // a romanized line appears under the on/kun readings (kana-only readings
    // only; kanji in kun readings are left as-is, never fabricated).
    val overrides = koinInject<DisplayOverridesStore>()
    var showRomaji by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { showRomaji = overrides.load().override }

    KaiteyoSectionCard(title = "Readings") {
        ReadingLine("ON", context.kanji.onReadings)
        ReadingLine("KUN", context.kanji.kunReadings)
        if (showRomaji == true) {
            val onRomaji = context.kanji.onReadings.mapNotNull { romajiFor(it) }
            val kunRomaji = context.kanji.kunReadings.mapNotNull { romajiFor(it) }
            if (onRomaji.isNotEmpty() || kunRomaji.isNotEmpty()) {
                Spacer(Modifier.height(Dimens.Space2))
                Text(
                    text = "ROMANIZATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = surfaceColors.textMuted
                )
                if (onRomaji.isNotEmpty()) {
                    Text(onRomaji.joinToString("・"), style = MaterialTheme.typography.bodySmall, color = surfaceColors.textSecondary)
                }
                if (kunRomaji.isNotEmpty()) {
                    Text(kunRomaji.joinToString("・"), style = MaterialTheme.typography.bodySmall, color = surfaceColors.textSecondary)
                }
            }
        }
    }
}

/** Romaji for a reading, only when it is purely kana (kanji are left out). */
private fun romajiFor(reading: String): String? =
    if (reading.isNotEmpty() && reading.all { it.isKana() }) reading.kanaToRomaji() else null

@Composable
private fun ReadingLine(type: String, readings: List<String>) {
    if (readings.isEmpty()) return
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        Text(type, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = surfaceColors.textMuted)
        Text(readings.joinToString("・"), style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary)
    }
}

@Composable
private fun FrequencyCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    val rank = context.kanji.frequencyRank
    val band = FrequencyBand.forRank(rank)
    KaiteyoSectionCard(title = "Frequency", subtitle = "Source: KANJIDIC") {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space3), verticalAlignment = Alignment.CenterVertically) {
            if (band != null) KaiteyoTag(text = band.label, tint = LocalKaiteyoAccent.current.primary)
            Text(frequencyRankLabel(rank), style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary)
            if (band != null) {
                Text(
                    text = band.jpLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted
                )
            }
        }
    }
}

@Composable
private fun ClassificationCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    KaiteyoSectionCard(title = "Classification") {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            context.kanji.classifications.forEach { tag ->
                KaiteyoTag(text = tag.label, tint = if (tag is ua.syt0r.kanji.core.knowledge.KanjiTag.Jlpt) accent.primary else accent.secondary)
            }
        }
        Spacer(Modifier.height(Dimens.Space2))
        val grade = context.kanji.classifications.filterIsInstance<ua.syt0r.kanji.core.knowledge.KanjiTag.Grade>().firstOrNull()
        Text(
            text = when (grade?.setKind()) {
                KanjiSetKind.Kyōiku -> "Kyōiku kanji (school grade ${grade.number})"
                KanjiSetKind.Joyo -> "Jōyō kanji (remaining set)"
                KanjiSetKind.Jinmeiyo -> "Jinmeiyō kanji (name use)"
                KanjiSetKind.Supplementary -> "Supplementary / nonstandard"
                KanjiSetKind.Unknown -> "Unclassified in the bundled dataset"
                null -> "Not classified in the bundled dataset"
            },
            style = MaterialTheme.typography.bodySmall,
            color = surfaceColors.textMuted
        )
    }
}

@Composable
private fun RadicalCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    val primary = context.radicals.firstOrNull()
    KaiteyoSectionCard(title = "Radical") {
        if (primary == null) {
            Text("No radical data in the bundled dataset.", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)) {
                Text(primary.component, fontSize = 30.sp, color = surfaceColors.textPrimary, fontWeight = FontWeight.Bold)
                Text(
                    "${primary.strokesCount} strokes",
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textMuted
                )
            }
        }
    }
}

@Composable
private fun ComponentCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    KaiteyoSectionCard(title = "Components", subtitle = "Radical-derived decomposition (source: KANJIDIC)") {
        if (context.radicals.isEmpty()) {
            Text("No component data.", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
            return@KaiteyoSectionCard
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            context.radicals.forEach { component ->
                Column(
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                        .background(surfaceColors.surfaceInteractive)
                        .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(component.component, fontSize = 24.sp, color = surfaceColors.textPrimary)
                    Text("${component.strokesCount} strokes", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun StrokeCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    KaiteyoSectionCard(title = "Strokes") {
        context.kanji.strokeCount?.let {
            Text(
                text = "$it strokes",
                style = MaterialTheme.typography.bodyLarge,
                color = surfaceColors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        } ?: Text("No stroke data.", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
        Text(
            text = "Stroke-order animation and evaluation live in Writing practice.",
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted,
            modifier = Modifier.padding(top = Dimens.Space1)
        )
    }
}

@Composable
private fun VocabularyCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    if (context.words.isEmpty()) return
    KaiteyoSectionCard(title = "Vocabulary", subtitle = "${context.words.size} words using ${context.kanji.character}") {
        context.words.forEach { word ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .clickable { context.onOpenWord(word.id) }
                    .padding(horizontal = Dimens.Space2, vertical = Dimens.Space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
            ) {
                Text(word.displaySpelling, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
                Text(word.kanaReading, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
                Text(
                    text = word.combinedGlossary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textSecondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RelatedCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    if (context.related.isEmpty()) {
        KaiteyoSectionCard(title = "Related kanji", subtitle = "Shared-radical relationships") {
            Text("No shared-radical kanji in the bundled dataset.", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
        }
        return
    }
    KaiteyoSectionCard(
        title = "Related kanji",
        subtitle = "${context.related.size} kanji sharing a radical — tap to open"
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            context.related.forEach { related ->
                Column(
                    modifier = Modifier
                        .widthIn(min = 56.dp)
                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                        .background(surfaceColors.surfaceInteractive)
                        .clickable { context.onOpenKanji(related.character) }
                        .padding(horizontal = Dimens.Space2, vertical = Dimens.Space2),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(related.character, fontSize = 22.sp, color = surfaceColors.textPrimary)
                    related.keyword?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    val family = context.kanji.variantFamily
    // The family string lists the character together with its variants
    // (e.g. 漢㐤) — strip the character itself to show the variants.
    val variants = family?.replace(context.kanji.character, "")?.trim()
    KaiteyoSectionCard(title = "Variant family", subtitle = "Simplified / traditional / variant relationships") {
        if (variants.isNullOrEmpty()) {
            Text(
                text = "No variant relationships in the bundled dataset for this character.",
                style = MaterialTheme.typography.bodySmall,
                color = surfaceColors.textMuted
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)) {
                Text(variants, fontSize = 30.sp, color = surfaceColors.textPrimary, fontWeight = FontWeight.Bold)
                Text(
                    text = "Variant(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted
                )
            }
            Text(
                text = "Related spellings of this character in the bundled dataset.",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted,
                modifier = Modifier.padding(top = Dimens.Space1)
            )
        }
    }
}

@Composable
private fun SentenceCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    if (context.sentences.isEmpty()) return
    KaiteyoSectionCard(title = "Sentences", subtitle = "From the bundled corpus — tap to analyze") {
        context.sentences.forEach { sentence ->
            val onClick = context.onOpenSentence
            val clickableModifier = if (onClick != null) {
                Modifier.clickable { onClick(sentence.text, sentence.translation) }
            } else Modifier
            Column(clickableModifier.fillMaxWidth().padding(vertical = Dimens.Space1)) {
                Text(sentence.text, style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary)
                Text(sentence.translation, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
            }
        }
    }
}

@Composable
private fun GrammarCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    if (context.grammar.isEmpty()) return
    KaiteyoSectionCard(title = "Grammar in examples", subtitle = "Built-in reference catalog") {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            context.grammar.forEach { match ->
                KaiteyoTag(text = match.matchedText, tint = accent.secondary)
            }
        }
    }
}

@Composable
private fun GraphCard(
    character: String,
    onOpenFullGraph: (() -> Unit)?
) {
    val graphRepository = koinInject<KnowledgeGraphRepository>()
    val scope = rememberCoroutineScope()
    var graph by remember { mutableStateOf<KnowledgeGraph?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    fun expand(nodeId: String) {
        val current = graph ?: return
        loading = true
        scope.launch {
            val result = graphRepository.expand(current, nodeId)
            graph = result.graph
            selected = nodeId
            loading = false
        }
    }

    LaunchedEffect(character) {
        loading = true
        graph = graphRepository.initialGraph(character)
        selected = graph?.rootId
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentGraph = graph
    if (currentGraph == null || currentGraph.isEmpty) {
        KaiteyoEmptyState(icon = "🗺️", title = "Graph unavailable", message = "Nothing to show for this kanji.")
        return
    }

    KaiteyoSectionCard(title = "Knowledge graph", subtitle = "Tap a node to focus, tap again to expand") {
        KnowledgeGraphCanvas(
            graph = currentGraph,
            selectedId = selected,
            onSelectNode = { id -> selected = id },
            onExpandNode = { id -> expand(id) },
            height = 380.dp,
            modifier = Modifier.fillMaxWidth()
        )
        if (onOpenFullGraph != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.Space2),
                horizontalArrangement = Arrangement.End
            ) {
                KaiteyoPill(text = "Open full graph", selected = false, onClick = onOpenFullGraph)
            }
        }
    }
}

@Composable
private fun MediaCard(context: KanjiCardContext) {
    val surfaceColors = LocalSurfaceColors.current
    val mediaStore = koinInject<MediaReferenceStore>()
    var references by remember { mutableStateOf<List<MediaReference>?>(null) }

    LaunchedEffect(context.kanji.character) {
        val readings = (context.kanji.onReadings + context.kanji.kunReadings).distinct().take(3)
        references = buildList {
            readings.forEach { reading -> addAll(mediaStore.matching(reading, limit = 3)) }
        }.distinctBy { it.text + it.title + it.timestampMs }.take(6)
    }

    KaiteyoSectionCard(title = "Media", subtitle = "Where this kanji appears in your media library") {
        when (val current = references) {
            null -> Text(
                text = "Loading…",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted
            )
            else -> if (current.isEmpty()) {
                Text(
                    text = "No media references yet — bookmark Japanese subtitle text in the Media Centre and they will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textSecondary
                )
            } else {
                current.forEach { ref ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space1),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = ref.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = surfaceColors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${ref.title} · ${mediaTimestamp(ref.timestampMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = surfaceColors.textMuted,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun mediaTimestamp(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
private fun StudyCard(character: String) {
    val surfaceColors = LocalSurfaceColors.current
    val provider = koinInject<StudyStatusProvider>()
    var entries by remember { mutableStateOf<List<StudyEntry>?>(null) }

    LaunchedEffect(character) {
        entries = provider.kanjiStates(character)
    }

    KaiteyoSectionCard(title = "Study", subtitle = "Real SRS state for this kanji") {
        when (val current = entries) {
            null -> Text(
                text = "Loading…",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted
            )
            else -> {
                current.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space1),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                    ) {
                        Text(
                            text = entry.practiceLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = surfaceColors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (entry.isNew) {
                            Text(
                                text = "not started",
                                style = MaterialTheme.typography.labelSmall,
                                color = surfaceColors.textMuted
                            )
                        } else {
                            KaiteyoTag(text = entry.state.label, tint = studyStateColor(entry.state))
                        }
                    }
                }
                Text(
                    text = "Study actions live on the deck detail page — this card reflects your real cards.",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.padding(top = Dimens.Space2)
                )
            }
        }
    }
}

