package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import ua.syt0r.kanji.presentation.common.AppListItem
import ua.syt0r.kanji.presentation.common.resources.icon.Copy
import ua.syt0r.kanji.presentation.common.resources.icon.ExtraIcons
import ua.syt0r.kanji.presentation.common.resolveString
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.extraColorScheme
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.AnimatedGlyphGraph
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.CompoundGroup
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.GlyphNode
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.GlyphNodeType
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoCompoundsCard
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoFormulaCard
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoStrokeOrderCard
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoMeaningsTagsCard
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoMnemonicCard
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoReadingsCard
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoCard
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoSentenceRow
import ua.syt0r.kanji.presentation.common.ui.kanji.AnimatedKanji
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiBackground
import ua.syt0r.kanji.presentation.screen.main.screen.info.LetterInfoData

// ============================================================
// LETTER INFO HEADING UI — Two-Column Knowledge Explorer
//
// Responsive Architecture:
//   · Desktop / Landscape: 2-column layout
//       Left: Interactive Glyph Graph + Stroke Order Card + Decomposition + Meanings
//       Right: Study Status + Readings→Vocab explorer + Mnemonic
//   · Portrait: Fluid single-column progressive hierarchy
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterInfoKanaHeading(data: LetterInfoData.Kana) {
    AppListItem(
        headlineContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimatableCharacter(data.strokes)
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val readings = data.reading.let {
                            if (it.alternative != null) listOf(it.nihonShiki) + it.alternative
                            else listOf(it.nihonShiki)
                        }
                        val messages = listOf(
                            data.kanaSystem.resolveString(),
                            resolveString { info.romajiMessage(readings) }
                        )
                        Text(
                            text = messages.joinToString("\n"),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall
                        )
                        CopyButton(data.character)
                    }
                }
            }
        }
    )
}

@Composable
fun LetterInfoKanjiHeading(
    data: LetterInfoData.Kanji,
    onRadicalClick: (String) -> Unit = {},
    onPlayReading: ((String) -> Unit)? = null,
    isPlayingReading: String? = null,
    userNote: String? = null,
    onSaveUserNote: ((String) -> Unit)? = null,
    onWordClick: ((ua.syt0r.kanji.core.app_data.data.JapaneseWord) -> Unit)? = null,
    statusContent: (@Composable () -> Unit)? = null,
    readingsVocabContent: (@Composable () -> Unit)? = null
) {
    val isLandscape = LocalOrientation.current == Orientation.Landscape
    val surfaceColors = LocalSurfaceColors.current

    // Build connected components for the Glyph Graph
    val glyphComponents = remember(data.radicalsSectionData.radicals, data.phonetics) {
        data.radicalsSectionData.radicals.mapIndexed { index, rad ->
            val isPhonetic = rad.value in data.phonetics && index > 0
            GlyphNode(
                character = rad.value,
                meaning = rad.meanings.firstOrNull() ?: "radical",
                type = when {
                    index == 0 -> GlyphNodeType.Radical
                    isPhonetic -> GlyphNodeType.Phonetic
                    else -> GlyphNodeType.SemanticComponent
                },
                depth = 1,
                strokes = rad.strokeIndicies.count(),
                readingHint = data.on.firstOrNull()
            )
        }
    }

    if (isLandscape) {
        // ── Landscape / Desktop Two-Column Layout ───────────
        // Kanjiverse architecture: Graph → Formula → Identity → Readings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Glyph Graph & Stroke Order (top priority)
            Column(
                modifier = Modifier.weight(1.1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Glyph Graph
                AnimatedGlyphGraph(
                    rootCharacter = data.character,
                    rootMeaning = data.meanings.joinToString(", ").ifEmpty { "Kanji" },
                    components = glyphComponents,
                    onCharacterClick = onRadicalClick,
                    onPlayAudio = onPlayReading,
                    modifier = Modifier.fillMaxWidth().height(290.dp)
                )

                // Stroke Order Card (replaces static kanji hero — shows kanji with animation)
                KaiteyoStrokeOrderCard(
                    strokes = data.strokes,
                    modifier = Modifier.fillMaxWidth()
                )

                // Hierarchical Formula
                KaiteyoFormulaCard(
                    character = data.character,
                    radicals = data.radicalsSectionData.radicals,
                    onRadicalClick = onRadicalClick,
                    glyphNodes = glyphComponents
                )

                // Meanings & Tags
                KaiteyoMeaningsTagsCard(
                    data = data,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Right Column: Identity & Study — status, readings → vocab
//             explorer (inline sentences), mnemonics
            Column(
                modifier = Modifier.weight(0.9f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (statusContent != null) statusContent()

                // Readings → vocabulary explorer with inline sentences
                if (readingsVocabContent != null) {
                    readingsVocabContent()
                } else {
                    KaiteyoReadingsCard(
                        character = data.character,
                        on = data.on,
                        kun = data.kun,
                        vocab = data.vocab.list.value,
                        sentences = data.sentences.list.value,
                        totalVocab = data.vocab.total,
                        onPlayReading = onPlayReading,
                        isPlayingReading = isPlayingReading,
                        onWordClick = onWordClick
                    )
                }

                // Mnemonics
                KaiteyoMnemonicCard(
                    character = data.character,
                    mnemonics = data.mnemonics,
                    userNote = userNote,
                    onSaveUserMnemonic = onSaveUserNote,
                    onComponentClick = onRadicalClick
                )

                // Standalone sentences card — only without the explorer,
                // which already shows example sentences per word
                if (readingsVocabContent == null) {
                    val sentences = data.sentences.list.value
                    if (sentences.isNotEmpty()) {
                        KaiteyoCard(
                            modifier = Modifier.fillMaxWidth(),
                            header = "Sentences",
                            subtitle = "Example sentences"
                        ) {
                            sentences.take(3).forEach { sentence ->
                                KaiteyoSentenceRow(
                                    sentence = sentence,
                                    onFuriganaClick = onRadicalClick
                                )
                            }
                            if (sentences.size > 3) {
                                Text(
                                    text = "+${sentences.size - 3} more...",
                                    fontSize = 11.sp,
                                    color = surfaceColors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // ── Portrait Single-Column Layout ───────────────────
        // Kanjiverse architecture: Graph → Formula → Hero → Readings
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Interactive Glyph Graph (top priority)
            AnimatedGlyphGraph(
                rootCharacter = data.character,
                rootMeaning = data.meanings.joinToString(", ").ifEmpty { "Kanji" },
                components = glyphComponents,
                onCharacterClick = onRadicalClick,
                onPlayAudio = onPlayReading,
                modifier = Modifier.fillMaxWidth().height(250.dp)
            )

            // 2. Stroke Order Card (replaces static kanji hero — shows kanji with animation)
            KaiteyoStrokeOrderCard(
                strokes = data.strokes,
                modifier = Modifier.fillMaxWidth()
            )

            // Study status (real SRS state for this kanji)
            if (statusContent != null) statusContent()

            // 3. Hierarchical Formula
            KaiteyoFormulaCard(
                character = data.character,
                radicals = data.radicalsSectionData.radicals,
                onRadicalClick = onRadicalClick,
                glyphNodes = glyphComponents
            )

            // 4. Meanings & Tags
            KaiteyoMeaningsTagsCard(
                data = data,
                modifier = Modifier.fillMaxWidth()
            )

            // 5. Readings → vocabulary explorer with inline sentences
            if (readingsVocabContent != null) {
                readingsVocabContent()
            } else {
                KaiteyoReadingsCard(
                    character = data.character,
                    on = data.on,
                    kun = data.kun,
                    vocab = data.vocab.list.value,
                    sentences = data.sentences.list.value,
                    totalVocab = data.vocab.total,
                    onPlayReading = onPlayReading,
                    isPlayingReading = isPlayingReading,
                    onWordClick = onWordClick
                )
            }

            // 6. Mnemonics
            KaiteyoMnemonicCard(
                character = data.character,
                mnemonics = data.mnemonics,
                userNote = userNote,
                onSaveUserMnemonic = onSaveUserNote,
                onComponentClick = onRadicalClick
            )

            // 7. Compounds
            val compounds = remember(data) {
                data.vocab.list.value.groupBy { word ->
                    val reading = word.reading.kanjiReading ?: word.reading.kanaReading
                    if (reading.startsWith(data.character)) "Words starting with ${data.character}"
                    else "Words containing ${data.character}"
                }.map { (label, words) -> CompoundGroup(label, words) }
            }
            if (compounds.isNotEmpty()) {
                KaiteyoCompoundsCard(
                    character = data.character,
                    groups = compounds,
                    onWordClick = { word -> onWordClick?.invoke(word) },
                    onBookmarkClick = null
                )
            }

            // Standalone sentences card — only without the explorer,
            // which already shows example sentences per word
            if (readingsVocabContent == null) {
                val sentences = data.sentences.list.value
                if (sentences.isNotEmpty()) {
                    KaiteyoCard(
                        modifier = Modifier.fillMaxWidth(),
                        header = "Sentences",
                        subtitle = "Example sentences using this kanji"
                    ) {
                        sentences.take(5).forEach { sentence ->
                            KaiteyoSentenceRow(
                                sentence = sentence,
                                onFuriganaClick = onRadicalClick
                            )
                        }
                        if (sentences.size > 5) {
                            Text(
                                text = "+${sentences.size - 5} more sentences...",
                                fontSize = 11.sp,
                                color = surfaceColors.textMuted,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatableCharacter(strokes: List<Path>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.size(110.dp),
            elevation = CardDefaults.elevatedCardElevation(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                KanjiBackground(Modifier.fillMaxSize())
                AnimatedKanji(
                    strokes = strokes,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = resolveString { info.strokesMessage(strokes.size) },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private const val CopyAnimationDuration = 800L

@Composable
private fun CopyButton(
    copyData: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { copying }
            .filter { it }
            .collect {
                delay(CopyAnimationDuration)
                copying = false
            }
    }

    IconButton(
        onClick = {
            clipboardManager.setText(AnnotatedString(copyData))
            copying = true
        },
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = copying,
            transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() }
        ) {
            if (it) Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .background(MaterialTheme.extraColorScheme.success, CircleShape)
                    .size(24.dp)
                    .padding(2.dp)
            )
            else Icon(ExtraIcons.Copy, null)
        }
    }
}
