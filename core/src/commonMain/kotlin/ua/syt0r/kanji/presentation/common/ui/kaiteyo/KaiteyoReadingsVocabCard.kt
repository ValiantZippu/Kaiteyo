package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ua.syt0r.kanji.core.app_data.Sentence
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.core.app_data.data.formattedFurigana
import ua.syt0r.kanji.core.japanese.isHiragana
import ua.syt0r.kanji.core.japanese.isKatakana
import ua.syt0r.kanji.core.japanese.katakanaToHiragana
import ua.syt0r.kanji.presentation.common.PaginateableState
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.FuriganaText
import ua.syt0r.kanji.presentation.common.ui.KaiteyoPill

// ============================================================
// READINGS & VOCABULARY EXPLORER
//
// One clean section replacing the separate vocab + sentence
// panels on the kanji details page:
//   · On/Kun reading groups shown as tabs with live counts
//   · Reading pills — tap one to browse only the words that
//     actually use that reading (proper kun okurigana and
//     katakana→hiragana matching)
//   · Tap a word to expand its example sentences inline
// ============================================================

/** How many words are listed per selection before a "+N more" hint appears. */
private const val MaxVisibleWords = 24

data class ReadingVocabGroup(
    val label: String,
    val readings: List<String>,
    /** Fallback group collecting words matched by no other group. */
    val isFallback: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaiteyoReadingsVocabCard(
    groups: List<ReadingVocabGroup>,
    vocab: PaginateableState<JapaneseWord>,
    modifier: Modifier = Modifier,
    onWordClick: ((JapaneseWord) -> Unit)? = null,
    onFuriganaClick: (String) -> Unit = {},
    onPlayReading: ((String) -> Unit)? = null,
    isPlayingReading: String? = null,
    sentenceProvider: suspend (JapaneseWord) -> List<Sentence> = { emptyList() }
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    // Progressively load every page so reading filters see the full set.
    LaunchedEffect(vocab.paginateable) {
        while (vocab.canLoadMore) {
            vocab.loadMore()
            delay(80)
        }
    }

    val allWords = vocab.list

    // Words bucketed per group, computed once per dataset change.
    val wordsByGroup = remember(groups, allWords) {
        groups.map { group ->
            if (group.isFallback) {
                allWords.filter { word ->
                    groups.filterNot { it.isFallback }.none { word.matchesGroup(it) }
                }
            } else {
                allWords.filter { it.matchesGroup(group) }
            }
        }
    }

    // Hide the fallback tab when every word already belongs to a reading.
    val displayGroups = remember(groups, wordsByGroup) {
        groups.filterIndexed { index, group ->
            !group.isFallback || wordsByGroup[index].isNotEmpty()
        }
    }

    var selectedGroupLabel by remember { mutableStateOf<String?>(null) }
    var selectedReading by remember { mutableStateOf<String?>(null) }
    var expandedWordId by remember { mutableStateOf<Long?>(null) }
    var expandedSentences by remember { mutableStateOf<List<Sentence>?>(null) }

    val selectedGroupIndex = displayGroups
        .indexOfFirst { it.label == selectedGroupLabel }
        .let { if (it == -1) 0 else it }
        .coerceIn(0, (displayGroups.size - 1).coerceAtLeast(0))
    val selectedGroup = displayGroups.getOrNull(selectedGroupIndex)

    // Auto-select the first reading whenever the active group changes.
    val activeReading = selectedReading
        ?.takeIf { selectedGroup != null && it in selectedGroup.readings }
        ?: selectedGroup?.readings?.firstOrNull()

    LaunchedEffect(selectedGroup?.label) { expandedWordId = null }

    LaunchedEffect(expandedWordId) {
        expandedSentences = null
        val word = allWords.firstOrNull { it.id == expandedWordId } ?: return@LaunchedEffect
        expandedSentences = runCatching { sentenceProvider(word) }.getOrDefault(emptyList())
    }

    KaiteyoCard(
        modifier = modifier,
        header = "Readings & Vocabulary",
        subtitle = "Tap a reading to browse the words that use it — tap a word for sentences"
    ) {
        if (displayGroups.isEmpty()) return@KaiteyoCard

        // ── Group tabs ────────────────────────────────────
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            displayGroups.forEachIndexed { index, group ->
                val count = wordsByGroup[groups.indexOf(group)].size
                KaiteyoPill(
                    text = "${group.label} · $count",
                    selected = index == selectedGroupIndex,
                    tint = accent.primary,
                    onClick = {
                        selectedGroupLabel = group.label
                        selectedReading = null
                    }
                )
            }
        }

        // ── Reading pills ─────────────────────────────────
        val groupReadings = selectedGroup?.readings.orEmpty()
        if (groupReadings.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                groupReadings.forEach { reading ->
                    ReadingChip(
                        reading = reading,
                        selected = reading == activeReading,
                        isPlaying = isPlayingReading == reading,
                        onSelect = {
                            selectedReading = if (reading == activeReading) null else reading
                            expandedWordId = null
                        },
                        onPlay = { onPlayReading?.invoke(reading) }
                    )
                }
            }
        }

        // ── Vocabulary of the current selection ───────────
        val wordsForSelection = if (selectedGroup == null) emptyList() else {
            val bucket = wordsByGroup.getOrElse(groups.indexOf(selectedGroup)) { emptyList() }
            if (activeReading != null) bucket.filter { it.matchesReading(activeReading) }
            else bucket
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            if (wordsForSelection.isEmpty()) {
                Text(
                    text = if (vocab.canLoadMore) "Loading vocabulary…"
                    else "No vocabulary found for this reading.",
                    fontSize = 12.sp,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else {
                wordsForSelection.take(MaxVisibleWords).forEach { word ->
                    VocabSentenceRow(
                        word = word,
                        expanded = expandedWordId == word.id,
                        sentences = if (expandedWordId == word.id) expandedSentences else null,
                        onToggleExpand = {
                            expandedWordId = if (expandedWordId == word.id) null else word.id
                        },
                        onOpenWord = { onWordClick?.invoke(word) },
                        showOpenAction = onWordClick != null,
                        onFuriganaClick = onFuriganaClick
                    )
                    if (word !== wordsForSelection.last()) Divider()
                }
                if (wordsForSelection.size > MaxVisibleWords) {
                    Text(
                        text = "+${wordsForSelection.size - MaxVisibleWords} more words",
                        fontSize = 11.sp,
                        color = surfaceColors.textMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingChip(
    reading: String,
    selected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val background = when {
        isPlaying -> accent.primary.copy(alpha = 0.28f)
        selected -> accent.primary.copy(alpha = 0.16f)
        hovered -> surfaceColors.surfaceInteractive
        else -> surfaceColors.surfaceInteractive.copy(alpha = 0.45f)
    }

    Row(
        modifier = Modifier
            .clip(KaiteyoPillShape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected || isPlaying) accent.primary.copy(alpha = 0.45f) else Color.Transparent,
                shape = KaiteyoPillShape
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .hoverable(interactionSource)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = reading,
            fontSize = 14.sp,
            fontWeight = if (selected || isPlaying) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected || isPlaying) accent.primary else surfaceColors.textSecondary
        )
        IconButton(onClick = onPlay, modifier = Modifier.size(26.dp)) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play reading",
                tint = if (selected || isPlaying) accent.primary else surfaceColors.textMuted,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun VocabSentenceRow(
    word: JapaneseWord,
    expanded: Boolean,
    sentences: List<Sentence>?,
    onToggleExpand: () -> Unit,
    onOpenWord: () -> Unit,
    showOpenAction: Boolean,
    onFuriganaClick: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (hovered || expanded) accent.primary.copy(alpha = 0.06f) else Color.Transparent)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interactionSource, indication = null, onClick = onToggleExpand)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FuriganaText(
                furiganaString = word.reading.formattedFurigana(),
                color = surfaceColors.textPrimary,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                annotationTextStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 10.sp,
                    color = surfaceColors.textMuted
                ),
                modifier = Modifier.weight(1.1f, fill = false)
            )
            Text(
                text = word.glossary.firstOrNull().orEmpty(),
                fontSize = 12.sp,
                color = surfaceColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.4f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Hide sentences" else "Show sentences",
                tint = if (expanded) accent.primary else surfaceColors.textMuted,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = if (expanded) 180f else 0f }
            )
            if (showOpenAction) {
                IconButton(onClick = onOpenWord, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Open word",
                        tint = surfaceColors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 10.dp, bottom = 8.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                when {
                    sentences == null -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )

                    sentences.isEmpty() -> Text(
                        text = "No example sentences in the corpus.",
                        fontSize = 11.sp,
                        color = surfaceColors.textMuted
                    )

                    else -> sentences.forEach { sentence ->
                        KaiteyoSentenceRow(
                            sentence = sentence,
                            onFuriganaClick = onFuriganaClick,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalSurfaceColors.current.surfaceInteractive.copy(alpha = 0.4f))
    )
}

// ── Reading matching heuristics ─────────────────────────────

private fun JapaneseWord.matchesGroup(group: ReadingVocabGroup): Boolean =
    group.readings.any { matchesReading(it) }

/**
 * Kun'yomi use `kanji.okurigana` notation ("い.う") — the word's kana must
 * contain stem + okurigana together. On'yomi are stored in katakana and are
 * normalized to hiragana before containment matching against the kana reading.
 */
internal fun JapaneseWord.matchesReading(reading: String): Boolean {
    val wordKana = this.reading.kanaReading
    return when {
        reading.contains('.') -> {
            val parts = reading.split('.', limit = 2)
            val candidate = parts[0] + parts.getOrElse(1) { "" }
            wordKana.contains(candidate)
        }

        reading.any { it.isHiragana() } -> wordKana.contains(reading)

        else -> {
            val normalized = reading.map { char ->
                if (char.isKatakana()) katakanaToHiragana(char) else char
            }.joinToString("")
            wordKana.contains(normalized)
        }
    }
}
