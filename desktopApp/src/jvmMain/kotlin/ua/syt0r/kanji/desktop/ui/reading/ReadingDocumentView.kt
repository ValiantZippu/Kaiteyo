package ua.syt0r.kanji.desktop.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryResultGroup
import ua.syt0r.kanji.desktop.engine.dictionary.JapaneseSegmenter
import ua.syt0r.kanji.desktop.engine.dictionary.SegmentToken
import ua.syt0r.kanji.desktop.engine.dictionary.WordStatus
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString
import ua.syt0r.kanji.desktop.engine.reading.minePhraseSentence
import ua.syt0r.kanji.desktop.engine.reading.ReadingBlock
import ua.syt0r.kanji.desktop.engine.reading.ReadingBlockKind
import ua.syt0r.kanji.desktop.engine.reading.ReadingDocument
import ua.syt0r.kanji.desktop.engine.reading.ReadingHighlight
import ua.syt0r.kanji.desktop.engine.reading.ReadingParsers
import ua.syt0r.kanji.desktop.ui.workspace.rememberAppState

/**
 * The reader: renders a document's blocks as tokenized, clickable text.
 * Japanese tokens are dictionary-backed (JapaneseSegmenter) — clicking one
 * opens the read-along glossary, where the word can be mined with the
 * sentence it appeared in. Reading position feeds the engine (progress,
 * bookmarks, history) and is persisted debounced.
 */
@Composable
fun ReadingDocumentView(
    document: ReadingDocument,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberAppState()
    val engine = state.reading
    val sc = surfaceColors()
    val ac = accent()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Parse the source once per document.
    val blocks = remember(document.id, document.sourcePath) {
        parseBlocks(document)
    }

    val listState = rememberLazyListState()

    // The reader takes keyboard focus so arrow/Esc keys work immediately.
    LaunchedEffect(document.id) {
        focusRequester.requestFocus()
    }

    // Restore the saved position when the document opens.
    LaunchedEffect(document.id) {
        val target = document.position.blockIndex.coerceIn(0, (blocks.lastIndex).coerceAtLeast(0))
        listState.scrollToItem(target)
    }

    // Feed position → engine, persist debounced.
    LaunchedEffect(document.id) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(1200)
            .collect { index ->
                engine.setPosition(index)
                engine.updateHistoryProgress()
                state.readingLibrary.save()
            }
    }

    // Lookup state (query + results + sentence for mining).
    var lookup by remember { mutableStateOf<ReadingLookup?>(null) }

    // In-document search state.
    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<Int>>(emptyList()) }
    var searchIndex by remember { mutableStateOf(0) }

    val firstVisible by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    val bookmarkedAtCurrent = document.bookmarks.any { it.blockIndex == firstVisible }

    Column(
        modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.Escape -> {
                        if (lookup != null) {
                            lookup = null
                            true
                        } else false
                    }
                    Key.DirectionDown, Key.PageDown -> {
                        scope.launch { listState.animateScrollBy(viewportHeight(listState)) }
                        true
                    }
                    Key.DirectionUp, Key.PageUp -> {
                        scope.launch { listState.animateScrollBy(-viewportHeight(listState)) }
                        true
                    }
                    else -> false
                }
            }
    ) {
        // ---- Reader toolbar -------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DsSpacing.Sm, end = DsSpacing.Md, top = DsSpacing.Sm, bottom = DsSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            DsIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                contentDescription = "Back to reading library"
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    color = sc.textPrimary,
                    fontSize = DsType.BodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${(document.progress * 100).toInt()}% read · ${document.bookmarkCount} bookmarks",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            DsSearchField(
                value = searchQuery,
                onValueChange = { raw ->
                    searchQuery = raw
                    if (raw.isBlank()) {
                        searchMatches = emptyList()
                        searchIndex = 0
                    } else {
                        val needle = raw.lowercase()
                        val all = blocks.mapIndexedNotNull { index, block ->
                            index.takeIf { block.text.lowercase().contains(needle) }
                        }
                        if (all.isEmpty()) {
                            searchMatches = emptyList()
                            searchIndex = 0
                        } else {
                            val start = firstVisible
                            searchMatches = all.filter { it >= start } + all.filter { it < start }
                            searchIndex = 0
                            scope.launch { listState.scrollToItem(searchMatches[0]) }
                        }
                    }
                },
                placeholder = resolveSuiteString { readerSearchPlaceholder },

                modifier = Modifier.width(240.dp)
            )
            if (searchMatches.isNotEmpty()) {
                DsIconButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    onClick = {
                        searchIndex = (searchIndex - 1 + searchMatches.size) % searchMatches.size
                        scope.launch { listState.scrollToItem(searchMatches[searchIndex]) }
                    },
                    contentDescription = "Previous match",
                    size = 30.dp
                )
                DsIconButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    onClick = {
                        searchIndex = (searchIndex + 1) % searchMatches.size
                        scope.launch { listState.scrollToItem(searchMatches[searchIndex]) }
                    },
                    contentDescription = "Next match",
                    size = 30.dp
                )
                Text(
                    text = "${searchIndex + 1}/${searchMatches.size}",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            DsIconButton(
                icon = if (bookmarkedAtCurrent) Icons.Default.Star else Icons.Default.StarBorder,
                onClick = {
                    engine.toggleBookmark(firstVisible, label = "Page ${firstVisible + 1}")
                    state.readingLibrary.save()
                },
                contentDescription = if (bookmarkedAtCurrent) "Remove bookmark" else "Bookmark this position",
                tint = if (bookmarkedAtCurrent) ac.primary else null
            )
        }

        // ---- Document body ---------------------------------------
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                itemsIndexed(
                    items = blocks,
                    key = { _, block -> block.index }
                ) { index, block ->
                    ReadingBlockView(
                        block = block,
                        highlights = document.highlights.filter { it.blockIndex == index },
                        onWordClick = { token ->
                            if (token.isJapanese && token.surface.isNotBlank()) {
                                scope.launch {
                                    val groups = withContext(Dispatchers.IO) {
                                        runCatching { engine.lookupGrouped(token.surface) }
                                            .getOrDefault(emptyList())
                                    }
                                    lookup = ReadingLookup(token.surface, groups, block.text)
                                }
                            }
                        }
                    )
                }
            }

            // Read-along glossary overlay — click-outside and Esc dismiss.
            lookup?.let { result ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { lookup = null }
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(DsSpacing.Md)
                ) {
                    ReadingLookupPopup(
                        state = state,
                        query = result.query,
                        groups = result.groups,
                        sentence = result.sentence,
                        documentTitle = document.title,
                        onMineSentence = {
                            state.mining.minePhraseSentence(
                                phrase = result.query,
                                sentence = result.sentence,
                                documentTitle = document.title
                            )
                            lookup = null
                        },
                        onOpenDictionary = {
                            lookup = null
                            state.currentView = ua.syt0r.kanji.desktop.appstate.WorkspaceView.Dictionary
                        },
                        onClose = { lookup = null }
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Block parsing
// ------------------------------------------------------------

private fun viewportHeight(listState: androidx.compose.foundation.lazy.LazyListState): Float =
    listState.layoutInfo.viewportSize.height.toFloat().coerceAtLeast(240f)

private fun parseBlocks(document: ReadingDocument): List<ReadingBlock> {
    val file = java.io.File(document.sourcePath)
    if (!file.exists()) return emptyList()
    return runCatching {
        ReadingParsers.parseBlocks(file.readText(), document.kind)
    }.getOrDefault(emptyList())
}

// ------------------------------------------------------------
// Lookup + search helpers
// ------------------------------------------------------------

private data class ReadingLookup(
    val query: String,
    val groups: List<DictionaryResultGroup>,
    val sentence: String
)

/** One normalized block, tokenized with dictionary-backed word spans. */
@Composable
private fun ReadingBlockView(
    block: ReadingBlock,
    highlights: List<ReadingHighlight>,
    onWordClick: (SegmentToken) -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val state = rememberAppState()
    val repository = state.dictionary.repository

    // Tokenize once per block (the segmenter caches its work).
    val tokens = remember(block.index, block.text) {
        JapaneseSegmenter.segment(block.text, repository, state.cards)
    }

    when (block.kind) {
        ReadingBlockKind.Heading -> Text(
            text = block.text,
            color = sc.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier.padding(top = DsSpacing.Md)
        )

        ReadingBlockKind.Quote -> Box(
            modifier = modifier
                .fillMaxWidth()
                .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
        ) {
            TokenFlow(
                tokens = tokens,
                highlights = highlights,
                onWordClick = onWordClick
            )
        }

        ReadingBlockKind.Code -> Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(sc.surfaceElevated)
                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
        ) {
            Text(
                text = block.text,
                color = sc.textSecondary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        ReadingBlockKind.Divider -> Text(
            text = "——",
            color = sc.textMuted,
            fontSize = 14.sp,
            modifier = modifier.fillMaxWidth().padding(vertical = DsSpacing.Sm)
        )

        else -> TokenFlow(
            tokens = tokens,
            highlights = highlights,
            onWordClick = onWordClick
        )
    }
}

/** Renders a block's tokens as clickable inline spans with word status colors. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TokenFlow(
    tokens: List<SegmentToken>,
    highlights: List<ReadingHighlight>,
    onWordClick: (SegmentToken) -> Unit,
    modifier: Modifier = Modifier
) {
    val ac = accent()

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        tokens.forEach { token ->
            val highlighted = highlights.any { h ->
                token.offset < h.end && (token.offset + token.surface.length) > h.start
            }
            WordToken(
                token = token,
                highlighted = highlighted,
                onClick = { onWordClick(token) }
            )
        }
    }
}

@Composable
private fun WordToken(
    token: SegmentToken,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val baseColor = when {
        token.status == WordStatus.Suspended -> sc.textMuted
        token.status == WordStatus.Mined -> ac.primary
        else -> sc.textPrimary
    }

    val annotated = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = baseColor,
                fontSize = if (token.isKanji) 17.sp else 15.sp,
                fontWeight = if (token.isKanji) FontWeight.Medium else FontWeight.Normal,
                textDecoration = if (token.status == WordStatus.Suspended) TextDecoration.LineThrough else TextDecoration.None,
                background = when {
                    highlighted -> ac.primary.copy(alpha = 0.22f)
                    hovered && token.isJapanese -> sc.surfaceInteractive
                    else -> Color.Transparent
                }
            )
        ) {
            append(token.surface)
        }
    }

    Text(
        text = annotated,
        modifier = modifier
            .then(
                if (token.isJapanese) {
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                        .hoverable(interaction)
                } else Modifier
            )
            .padding(horizontal = 1.dp, vertical = 1.dp),
        style = TextStyle(lineHeight = 24.sp)
    )
}
