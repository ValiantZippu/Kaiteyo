package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import ua.syt0r.kanji.core.app_data.Sentence
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.core.app_data.data.formattedFurigana
import ua.syt0r.kanji.core.japanese.isKatakana
import ua.syt0r.kanji.core.japanese.katakanaToHiragana
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import androidx.compose.material.icons.filled.PlayArrow
import ua.syt0r.kanji.presentation.common.ui.ClickableFuriganaText
import ua.syt0r.kanji.presentation.common.ui.FuriganaText
import ua.syt0r.kanji.presentation.common.ui.kanji.Kanji
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiRadicalDetails
import ua.syt0r.kanji.presentation.screen.main.screen.info.LetterInfoData
import ua.syt0r.kanji.presentation.screen.main.screen.info.VocabInfoData

// ============================================================
// KAITEYO DICTIONARY CARDS — the presentation kit
//
// Every word / kanji / sentence screen is assembled from these
// cards so the whole dictionary feels like one living reference:
//   · rounded section cards
//   · frequency + part-of-speech badges
//   · type-first heroes (furigana above the writing)
//   · JMdict-style numbered senses
//   · kanji pill lists, reading rows
//   · sentence cards with furigana + translation + bookmark
// All colors from active Kaiteyo theme — nothing hardcoded.
// ============================================================

// ── Shared tokens ────────────────────────────────────────────

internal val KaiteyoCardShape = RoundedCornerShape(16.dp)
internal val KaiteyoPillShape = RoundedCornerShape(10.dp)

internal fun SurfaceColors.kaiteyoElevated(): Color =
    surfaceInteractive.copy(alpha = 0.55f)

// ── Section card — the fundamental container ─────────────────
//
// Clean, minimal card: surface background + subtle border.
// No gradients, no glow, no drawBehind animation.

@Composable
fun KaiteyoCard(
    modifier: Modifier = Modifier,
    header: String? = null,
    subtitle: String? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    val cardModifier = modifier
        .clip(KaiteyoCardShape)
        .background(surfaceColors.surface)
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier
        )

    Column(modifier = cardModifier) {
        if (header != null || subtitle != null) {
            KaiteyoCardHeader(
                title = header,
                subtitle = subtitle,
                accent = accent,
                surfaceColors = surfaceColors
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun KaiteyoCardHeader(
    title: String?,
    subtitle: String?,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = accent.primary.copy(alpha = 0.75f)
                )
            }
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = surfaceColors.textMuted
                )
            }
        }
        KaiteyoKebabButton()
    }
}

@Composable
fun KaiteyoKebabButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "More options",
            tint = surfaceColors.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Badges & pills ──────────────────────────────────────────

enum class KaiteyoFrequency(val label: String) {
    Common("Common"),
    Uncommon("Uncommon"),
    Rare("Rare"),
    Obscure("Obscure")
}

@Composable
fun KaiteyoFrequencyBadge(frequency: KaiteyoFrequency?) {
    if (frequency == null) return
    val surfaceColors = LocalSurfaceColors.current
    val (bg, fg) = when (frequency) {
        KaiteyoFrequency.Common -> surfaceColors.kanjiKnown.copy(alpha = 0.14f) to surfaceColors.kanjiKnown
        KaiteyoFrequency.Uncommon -> Color(0xFF3A3157) to LocalKaiteyoAccent.current.secondary
        KaiteyoFrequency.Rare -> Color(0xFF3B3040) to LocalKaiteyoAccent.current.primary.copy(alpha = 0.7f)
        KaiteyoFrequency.Obscure -> surfaceColors.surfaceInteractive to surfaceColors.textMuted
    }
    KaiteyoBadge(text = frequency.label, containerColor = bg, contentColor = fg)
}

@Composable
fun KaiteyoBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = contentColor,
        modifier = modifier
            .clip(KaiteyoPillShape)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun KaiteyoPosBadge(pos: String) {
    val surfaceColors = LocalSurfaceColors.current
    KaiteyoBadge(
        text = pos,
        containerColor = surfaceColors.surfaceInteractive.copy(alpha = 0.5f),
        contentColor = surfaceColors.textSecondary
    )
}

// ── Hero — the big "type-first" header ──────────────────────

@Composable
fun KanjiHero(
    character: String,
    reading: String,
    meanings: List<String>,
    modifier: Modifier = Modifier,
    onCharacterClick: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    KaiteyoCard(modifier = modifier, contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 20.dp)) {
        Text(
            text = reading,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = surfaceColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = character,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth().then(
                if (onCharacterClick != null) Modifier.clickable(onClick = onCharacterClick)
                else Modifier
            )
        )

        if (meanings.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = meanings.joinToString("\n"),
                fontSize = 13.sp,
                color = surfaceColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Word hero — the big "type-first" header ──────────────────

@Composable
fun KaiteyoVocabHero(
    word: JapaneseWord,
    typeBadge: String? = null,
    frequency: KaiteyoFrequency? = null,
    onAddToDeck: () -> Unit,
    onOpenJisho: () -> Unit,
    modifier: Modifier = Modifier,
    onWordClick: ((JapaneseWord) -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    val reading = word.reading
    val bigText = reading.kanjiReading ?: reading.kanaReading

    KaiteyoCard(modifier = modifier, contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (typeBadge != null || frequency != null) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    typeBadge?.let {
                        KaiteyoBadge(
                            text = it,
                            containerColor = accent.primary.copy(alpha = 0.14f),
                            contentColor = accent.primary
                        )
                    }
                    KaiteyoFrequencyBadge(frequency)
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                KaiteyoKebabButton()
            }
        }

        Spacer(Modifier.height(14.dp))

        FuriganaText(
            furiganaString = reading.formattedFurigana(),
            color = surfaceColors.textSecondary,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            annotationTextStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 11.sp,
                color = surfaceColors.textMuted,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = bigText,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth().then(
                if (onWordClick != null) Modifier.clickable { onWordClick(word) }
                else Modifier
            )
        )

        if (word.glossary.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = word.glossary.take(3).joinToString("\n"),
                fontSize = 13.sp,
                color = surfaceColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KaiteyoActionButton(
                label = "＋ Add to deck",
                onClick = onAddToDeck,
                container = accent.primary.copy(alpha = 0.14f),
                content = accent.primary,
                modifier = Modifier.weight(1f)
            )
            KaiteyoActionButton(
                label = "Jisho ↗",
                onClick = onOpenJisho,
                container = surfaceColors.kaiteyoElevated(),
                content = surfaceColors.textSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun KaiteyoActionButton(
    label: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(KaiteyoPillShape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 1
        )
    }
}

// ── Senses — the JMdict card ────────────────────────────────

@Composable
fun KaiteyoSenseList(
    senses: List<VocabInfoData.Sense>,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    KaiteyoCard(
        modifier = modifier,
        header = "JMdict",
        subtitle = "Dictionary senses for this entry"
    ) {
        if (senses.isEmpty()) {
            Text(
                "No senses recorded for this entry.",
                fontSize = 12.sp,
                color = surfaceColors.textMuted
            )
            return@KaiteyoCard
        }
        senses.forEachIndexed { index, sense ->
            if (index > 0) {
                KaiteyoDivider()
            }
            KaiteyoSenseRow(index = index, sense = sense)
        }
    }
}

@Composable
private fun KaiteyoSenseRow(index: Int, sense: VocabInfoData.Sense) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${index + 1}.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = LocalKaiteyoAccent.current.primary.copy(alpha = 0.85f),
            modifier = Modifier.width(26.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (sense.pos != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sense.pos.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        .forEach { KaiteyoPosBadge(it) }
                }
            }
            Text(
                text = sense.glossary,
                fontSize = 14.sp,
                color = surfaceColors.textPrimary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun KaiteyoDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = LocalSurfaceColors.current.surfaceInteractive.copy(alpha = 0.5f)
    )
}

// ── Kanji list — pill buttons for each kanji ────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaiteyoKanjiPills(
    letters: List<String>,
    onLetterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    KaiteyoCard(
        modifier = modifier,
        header = "Kanji List",
        subtitle = "Tap a character to explore it"
    ) {
        if (letters.isEmpty()) {
            Text(
                "Written in kana only — no kanji in this entry.",
                fontSize = 12.sp,
                color = LocalSurfaceColors.current.textMuted
            )
            return@KaiteyoCard
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            letters.forEach { letter ->
                KaiteyoKanjiPill(
                    character = letter,
                    onClick = { onLetterClick(letter) }
                )
            }
        }
    }
}

enum class PillType { Radical, Phonetic, Component }

@Composable
fun KaiteyoKanjiPill(
    character: String,
    meaning: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pillType: PillType = PillType.Component
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val (bgColor, borderColor, typeLabel) = when (pillType) {
        PillType.Radical -> Triple(
            accent.primary.copy(alpha = if (hovered) 0.22f else 0.12f),
            accent.primary.copy(alpha = 0.40f),
            ""
        )
        PillType.Phonetic -> Triple(
            accent.secondary.copy(alpha = if (hovered) 0.22f else 0.12f),
            accent.secondary.copy(alpha = 0.40f),
            ""
        )
        PillType.Component -> Triple(
            surfaceColors.surfaceInteractive.copy(alpha = if (hovered) 0.7f else 0.45f),
            surfaceColors.surfaceInteractive.copy(alpha = 0.6f),
            ""
        )
    }

    Row(
        modifier = modifier
            .clip(KaiteyoPillShape)
            .background(bgColor)
            .border(1.dp, borderColor, KaiteyoPillShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = character,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textPrimary
        )
        if (meaning != null) {
            Text(
                text = meaning,
                fontSize = 11.sp,
                color = surfaceColors.textMuted,
                maxLines = 1
            )
        }
    }
}

// ── Readings & vocabulary hub ───────────────────────
//
// One clean flow instead of separate vocab/sentence panels:
//   tap a reading → see every word read that way
//   tap a word   → see that word's example sentences inline

private fun JapaneseWord.wordText(): String =
    reading.kanjiReading ?: reading.kanaReading

private fun JapaneseWord.wordKana(): String =
    reading.kanaReading ?: reading.kanjiReading ?: ""

/** Katakana-normalized form of a kana string for reading comparisons. */
private fun String.toHiragana(): String =
    map { char -> if (char.isKatakana()) katakanaToHiragana(char) else char }.joinToString("")

/** All comparable stems of a reading: the full reading plus the kanji-only stem. */
private fun readingStems(reading: String): List<String> {
    val normalized = reading.replace("・", ".").replace("．", ".")
    val stems = mutableListOf(normalized.replace(".", ""))
    if (normalized.contains('.')) stems += normalized.substringBefore('.')
    return stems.filter { it.isNotBlank() }.map { it.toHiragana() }
}

private fun JapaneseWord.matchesReading(reading: String): Boolean {
    val kana = wordKana().toHiragana()
    if (kana.isEmpty()) return false
    return readingStems(reading).any { stem -> kana.contains(stem) }
}

private fun sentencesForWord(word: JapaneseWord, sentences: List<Sentence>): List<Sentence> {
    val kanji = word.reading.kanjiReading
    val kana = word.reading.kanaReading
    return sentences.filter { sentence ->
        (kanji != null && sentence.value.contains(kanji)) ||
            (kana != null && sentence.value.contains(kana))
    }
}

@Composable
fun KaiteyoReadingsCard(
    character: String,
    on: List<String>,
    kun: List<String>,
    vocab: List<JapaneseWord>,
    sentences: List<Sentence>,
    totalVocab: Int,
    modifier: Modifier = Modifier,
    onPlayReading: ((String) -> Unit)? = null,
    isPlayingReading: String? = null,
    onWordClick: ((JapaneseWord) -> Unit)? = null,
    onFuriganaClick: (String) -> Unit = {},
    canLoadMoreVocab: Boolean = false,
    onLoadMoreVocab: () -> Unit = {},
    /** Optional repository-backed sentence lookup; falls back to filtering [sentences]. */
    sentenceProvider: (suspend (JapaneseWord) -> List<Sentence>)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    // One open reading group and one open word at a time keeps the page calm
    var expandedReading by remember { mutableStateOf<String?>(null) }
    var expandedWord by remember { mutableStateOf<JapaneseWord?>(null) }
    var expandedWordSentences by remember { mutableStateOf<List<Sentence>?>(null) }
    var otherExpanded by remember { mutableStateOf(false) }

    // Resolve the open word's example sentences asynchronously
    LaunchedEffect(expandedWord) {
        expandedWordSentences = null
        val word = expandedWord ?: return@LaunchedEffect
        expandedWordSentences = sentenceProvider?.invoke(word)
            ?: sentencesForWord(word, sentences)
    }

    // Group loaded vocab by reading; anything unmatched falls into "other"
    val grouped = remember(vocab, on, kun) {
        val matched = mutableSetOf<JapaneseWord>()
        val onGroups = on.map { reading ->
            reading to vocab.filter { it.matchesReading(reading) }.also { matched.addAll(it) }
        }
        val kunGroups = kun.map { reading ->
            reading to vocab.filter { it.matchesReading(reading) }.also { matched.addAll(it) }
        }
        val other = vocab.filter { it !in matched }
        Triple(onGroups, kunGroups, other)
    }

    KaiteyoCard(
        modifier = modifier,
        header = "Readings & Vocabulary",
        subtitle = "Tap a reading to browse its words — tap a word to see sentences"
    ) {
        if (on.isEmpty() && kun.isEmpty()) {
            // No readings recorded — fall back to a single flat group
            ReadingGroupLabel("Vocabulary", accent.secondary)
            vocab.take(20).forEach { word ->
                KaiteyoVocabWithSentencesRow(
                    word = word,
                    sentences = wordSentences(word, expandedWord, expandedWordSentences, sentences),
                    expanded = expandedWord == word,
                    onToggle = {
                        expandedWord = if (expandedWord == word) null else word
                    },
                    onOpenDetails = { onWordClick?.invoke(word) },
                    onFuriganaClick = onFuriganaClick
                )
            }
            return@KaiteyoCard
        }

        ReadingSection(label = "On'yomi", tint = accent.secondary) {
            grouped.first.forEach { (reading, words) ->
                KaiteyoReadingRow(
                    reading = reading,
                    wordCount = words.size,
                    isPlaying = isPlayingReading == reading,
                    expanded = expandedReading == reading,
                    onToggle = {
                        expandedReading = if (expandedReading == reading) null else reading
                        expandedWord = null
                    },
                    onPlay = { onPlayReading?.invoke(reading) }
                )
                AnimatedVisibility(visible = expandedReading == reading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (words.isEmpty()) {
                            Text(
                                text = "No vocabulary loaded for this reading",
                                fontSize = 11.sp,
                                color = surfaceColors.textMuted,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                            )
                        }
                        words.forEach { word ->
                            KaiteyoVocabWithSentencesRow(
                                word = word,
                                sentences = wordSentences(word, expandedWord, expandedWordSentences, sentences),
                                expanded = expandedWord == word,
                                onToggle = {
                                    expandedWord = if (expandedWord == word) null else word
                                },
                                onOpenDetails = { onWordClick?.invoke(word) },
                                onFuriganaClick = onFuriganaClick
                            )
                        }
                        if (words.isNotEmpty() && canLoadMoreVocab) {
                            KaiteyoLoadMoreChip(onClick = onLoadMoreVocab)
                        }
                    }
                }
            }
        }

        ReadingSection(label = "Kun'yomi", tint = accent.primary) {
            grouped.second.forEach { (reading, words) ->
                KaiteyoReadingRow(
                    reading = reading,
                    wordCount = words.size,
                    isPlaying = isPlayingReading == reading,
                    expanded = expandedReading == reading,
                    onToggle = {
                        expandedReading = if (expandedReading == reading) null else reading
                        expandedWord = null
                    },
                    onPlay = { onPlayReading?.invoke(reading) }
                )
                AnimatedVisibility(visible = expandedReading == reading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (words.isEmpty()) {
                            Text(
                                text = "No vocabulary loaded for this reading",
                                fontSize = 11.sp,
                                color = surfaceColors.textMuted,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                            )
                        }
                        words.forEach { word ->
                            KaiteyoVocabWithSentencesRow(
                                word = word,
                                sentences = wordSentences(word, expandedWord, expandedWordSentences, sentences),
                                expanded = expandedWord == word,
                                onToggle = {
                                    expandedWord = if (expandedWord == word) null else word
                                },
                                onOpenDetails = { onWordClick?.invoke(word) },
                                onFuriganaClick = onFuriganaClick
                            )
                        }
                        if (words.isNotEmpty() && canLoadMoreVocab) {
                            KaiteyoLoadMoreChip(onClick = onLoadMoreVocab)
                        }
                    }
                }
            }
        }

        // Words whose reading isn't in the On/Kun lists (irregular, kana-only, …)
        if (grouped.third.isNotEmpty()) {
            KaiteyoDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KaiteyoPillShape)
                    .clickable { otherExpanded = !otherExpanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Other vocabulary",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                KaiteyoCountBadge(grouped.third.size)
                Text(
                    text = if (otherExpanded) "▾" else "▸",
                    fontSize = 12.sp,
                    color = surfaceColors.textMuted
                )
            }
            AnimatedVisibility(visible = otherExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    grouped.third.forEach { word ->
                        KaiteyoVocabWithSentencesRow(
                            word = word,
                            sentences = wordSentences(word, expandedWord, expandedWordSentences, sentences),
                            expanded = expandedWord == word,
                            onToggle = {
                                expandedWord = if (expandedWord == word) null else word
                            },
                            onOpenDetails = { onWordClick?.invoke(word) },
                            onFuriganaClick = onFuriganaClick
                        )
                    }
                }
            }
        }

        if (totalVocab > vocab.size) {
            Text(
                text = "Showing ${vocab.size} of $totalVocab words",
                fontSize = 10.sp,
                color = surfaceColors.textMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ReadingSection(
    label: String,
    tint: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ReadingGroupLabel(label, tint)
        content()
    }
}

@Composable
private fun ReadingGroupLabel(text: String, tint: Color) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = tint.copy(alpha = 0.85f),
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun KaiteyoReadingRow(
    reading: String,
    wordCount: Int,
    isPlaying: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPlay: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(KaiteyoPillShape)
            .background(
                when {
                    isPlaying -> accent.secondary.copy(alpha = 0.14f)
                    hovered -> surfaceColors.surfaceInteractive.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play reading",
            tint = if (isPlaying) accent.secondary else surfaceColors.textMuted.copy(alpha = 0.5f),
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .clickable(onClick = onPlay)
        )
        Text(
            text = reading,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isPlaying) accent.secondary else surfaceColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (wordCount > 0) KaiteyoCountBadge(wordCount)
        Text(
            text = if (expanded) "▾" else "▸",
            fontSize = 12.sp,
            color = surfaceColors.textMuted
        )
    }
}

@Composable
fun KaiteyoCountBadge(count: Int) {
    val surfaceColors = LocalSurfaceColors.current
    Text(
        text = "$count",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = surfaceColors.textSecondary,
        modifier = Modifier
            .clip(KaiteyoPillShape)
            .background(surfaceColors.surfaceInteractive.copy(alpha = 0.55f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
private fun KaiteyoLoadMoreChip(onClick: () -> Unit) {
    val accent = LocalKaiteyoAccent.current
    Text(
        text = "Load more words…",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = accent.primary,
        modifier = Modifier
            .clip(KaiteyoPillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** Sentences shown for a word row — resolved lazily for the open word. */
private fun wordSentences(
    word: JapaneseWord,
    expandedWord: JapaneseWord?,
    expandedWordSentences: List<Sentence>?,
    sentences: List<Sentence>
): List<Sentence>? =
    if (expandedWord == word) expandedWordSentences
    else sentencesForWord(word, sentences)

/** Vocab row that expands inline into its example sentences. */
@Composable
private fun KaiteyoVocabWithSentencesRow(
    word: JapaneseWord,
    sentences: List<Sentence>?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenDetails: () -> Unit,
    onFuriganaClick: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val reading = word.reading

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    expanded -> accent.primary.copy(alpha = 0.07f)
                    hovered -> surfaceColors.surfaceInteractive.copy(alpha = 0.35f)
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (reading.furigana != null) {
                    FuriganaText(
                        furiganaString = reading.formattedFurigana(),
                        color = surfaceColors.textMuted,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                        annotationTextStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 8.sp,
                            color = surfaceColors.textMuted
                        )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = word.wordText(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (expanded) accent.primary else surfaceColors.textPrimary
                    )
                    Text(
                        text = word.glossary.joinToString(", "),
                        fontSize = 11.sp,
                        color = surfaceColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            val sentenceCount = sentences?.size ?: 0
            if (sentenceCount > 0) {
                Text(
                    text = "$sentenceCount 文",
                    fontSize = 10.sp,
                    color = surfaceColors.textMuted
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Open entry",
                tint = surfaceColors.textMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenDetails)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (sentences == null) {
                    Text(
                        text = "Loading sentences…",
                        fontSize = 11.sp,
                        color = surfaceColors.textMuted
                    )
                } else if (sentences.isEmpty()) {
                    Text(
                        text = "No example sentences for this word",
                        fontSize = 11.sp,
                        color = surfaceColors.textMuted
                    )
                } else {
                    sentences.forEach { sentence ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.3f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                KaiteyoFuriganaClickable(
                                    furigana = sentence.furigana,
                                    onFuriganaClick = onFuriganaClick
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = sentence.translation,
                                    fontSize = 11.sp,
                                    color = surfaceColors.textSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingPill(
    text: String,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    val bgColor = if (isPlaying) accent.primary.copy(alpha = 0.25f)
    else accent.primary.copy(alpha = 0.08f)
    val textColor = if (isPlaying) accent.primary else surfaceColors.textPrimary
    val borderColor = if (isPlaying) accent.primary.copy(alpha = 0.5f)
    else accent.primary.copy(alpha = 0.15f)

    Row(
        modifier = Modifier
            .clip(KaiteyoPillShape)
            .background(bgColor)
            .border(1.dp, borderColor, KaiteyoPillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (isPlaying) accent.primary else textColor.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

// ── Sentence card ───────────────────────────────────────────

@Composable
fun KaiteyoSentenceCard(
    sentence: Sentence,
    characterToHighlight: String?,
    onCharacterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    KaiteyoCard(modifier = modifier, contentPadding = PaddingValues(14.dp, 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KaiteyoBadge(
                text = "SENTENCE",
                containerColor = accent.secondary.copy(alpha = 0.12f),
                contentColor = accent.secondary
            )
        }

        Spacer(Modifier.height(8.dp))

        FuriganaText(
            furiganaString = sentence.furigana,
            color = surfaceColors.textPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = sentence.translation,
            fontSize = 12.sp,
            color = surfaceColors.textSecondary,
            lineHeight = 17.sp
        )
    }
}



// ── Glyph Graph Card wrapper ────────────────────────────────

@Composable
fun KaiteyoGlyphGraphCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    KaiteyoCard(modifier = modifier, header = "Structure", subtitle = "Component relationships") {
        content()
    }
}

// ── Meanings & tags card ────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaiteyoMeaningsTagsCard(
    data: LetterInfoData.Kanji,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val strokeCount = data.strokes.size
    val radicalCount = data.radicalsSectionData.radicals.size
    val hasPhonetics = data.phonetics.isNotEmpty()

    KaiteyoCard(
        modifier = modifier,
        header = "Meanings & Tags",
        subtitle = data.meanings.joinToString(", ")
    ) {
        // Main meaning text
        Text(
            text = data.meanings.joinToString(", "),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = surfaceColors.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Tags flow
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Jōyō badge (if has grade)
            data.grade?.let {
                KaiteyoBadge(
                    text = "Jōyō",
                    containerColor = accent.primary.copy(alpha = 0.14f),
                    contentColor = accent.primary
                )
            }

            // Compound badge (if multiple components)
            if (radicalCount > 1) {
                KaiteyoBadge(
                    text = "Compound",
                    containerColor = accent.secondary.copy(alpha = 0.14f),
                    contentColor = accent.secondary
                )
            }

            // Phono-semantic badge
            if (hasPhonetics) {
                KaiteyoBadge(
                    text = "Phono-semantic",
                    containerColor = Color(0xFF3A3157),
                    contentColor = accent.secondary
                )
            }

            // Radical badge
            data.radicalsSectionData.radicals.firstOrNull()?.let { radical ->
                KaiteyoBadge(
                    text = "Radical: ${radical.value}",
                    containerColor = surfaceColors.surfaceInteractive.copy(alpha = 0.45f),
                    contentColor = surfaceColors.textPrimary
                )
            }

            // Phonetic badge
            data.phonetics.firstOrNull()?.let { phonetic ->
                KaiteyoBadge(
                    text = "Phonetic: $phonetic",
                    containerColor = surfaceColors.surfaceInteractive.copy(alpha = 0.45f),
                    contentColor = surfaceColors.textPrimary
                )
            }

            // Stroke count
            KaiteyoBadge(
                text = "$strokeCount strokes",
                containerColor = surfaceColors.surfaceInteractive.copy(alpha = 0.45f),
                contentColor = surfaceColors.textPrimary
            )

            // Grade
            data.grade?.let { grade ->
                KaiteyoBadge(
                    text = "Grade: $grade",
                    containerColor = surfaceColors.surfaceInteractive.copy(alpha = 0.45f),
                    contentColor = surfaceColors.textPrimary
                )
            }

            // JLPT
            data.jlptLevel?.let { jlpt ->
                KaiteyoBadge(
                    text = "JLPT: N$jlpt",
                    containerColor = accent.primary.copy(alpha = 0.14f),
                    contentColor = accent.primary
                )
            }

            // Frequency (as Kanken proxy)
            data.frequency?.let { freq ->
                KaiteyoBadge(
                    text = "Freq: $freq",
                    containerColor = surfaceColors.surfaceInteractive.copy(alpha = 0.45f),
                    contentColor = surfaceColors.textPrimary
                )
            }
        }
    }
}

// ── Formula card ────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaiteyoFormulaCard(
    character: String,
    radicals: List<ua.syt0r.kanji.presentation.common.ui.kanji.KanjiRadicalDetails>,
    onRadicalClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    glyphNodes: List<GlyphNode> = emptyList()
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    // Build lookup: character -> GlyphNodeType from the graph
    val nodeTypeMap = glyphNodes.associateBy({ it.character }, { it.type })

    KaiteyoCard(
        modifier = modifier,
        header = "Decomposition",
        subtitle = "How this kanji is built"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            radicals.forEachIndexed { index, radical ->
                val nodeType = nodeTypeMap[radical.value]
                val pillType = when (nodeType) {
                    GlyphNodeType.Radical -> PillType.Radical
                    GlyphNodeType.Phonetic -> PillType.Phonetic
                    else -> if (index == 0) PillType.Radical else PillType.Component
                }
                val meaning = radical.meanings.firstOrNull() ?: "component"

                // Each line: [Character] Meaning = [sub-components] or +
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Main character pill
                    KaiteyoKanjiPill(
                        character = radical.value,
                        meaning = meaning,
                        onClick = { onRadicalClick(radical.value) },
                        pillType = pillType
                    )

                    // Equals sign for first component
                    if (index == 0) {
                        Text(
                            text = "=",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = surfaceColors.textMuted
                        )
                    } else {
                        // Plus sign for additional components
                        Text(
                            text = "+",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = surfaceColors.textMuted
                        )
                    }

                    // Sub-components (if any)
                    val strokeCount = radical.strokeIndicies.count()
                    if (strokeCount > 1) {
                        repeat(strokeCount - 1) {
                            Text(
                                text = "+",
                                fontSize = 14.sp,
                                color = surfaceColors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

// KaiteyoMnemonicCard is defined in KaiteyoMnemonicCard.kt

// ── Pitch accent card ───────────────────────────────────────

@Composable
fun KaiteyoPitchAccentCard(
    reading: String,
    pitch: List<Int>?, // Downstep position, -1 = nakadaka
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    KaiteyoCard(
        modifier = modifier,
        header = "Pitch Accent",
        subtitle = "Intonation pattern"
    ) {
        if (pitch == null || pitch.isEmpty()) {
            Text(
                "No pitch accent data available.",
                fontSize = 12.sp,
                color = surfaceColors.textMuted
            )
            return@KaiteyoCard
        }

        // Simple pitch visualization
        Text(
            text = reading,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textPrimary,
            letterSpacing = 2.sp
        )
    }
}

// (KaiteyoKanjiHero defined below after KaiteyoFuriganaClickable)

// ── Vocab row — used in expandable vocab sections ───────────

@Composable
fun KaiteyoVocabRow(
    word: JapaneseWord,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
    onKanjiClick: ((String) -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val reading = word.reading

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (hovered) accent.primary.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Furigana above the kanji
        if (reading.furigana != null) {
            FuriganaText(
                furiganaString = reading.furigana,
                color = surfaceColors.textMuted,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                annotationTextStyle = androidx.compose.ui.text.TextStyle(fontSize = 8.sp, color = surfaceColors.textMuted),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Kanji with individual click support
            if (onKanjiClick != null && reading.furigana != null) {
                ClickableFuriganaText(
                    furiganaString = reading.furigana,
                    onClick = onKanjiClick,
                    modifier = Modifier.weight(0.4f)
                )
            } else {
                val kanjiText = reading.kanjiReading ?: reading.kanaReading
                Text(
                    text = kanjiText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = surfaceColors.textPrimary,
                    modifier = Modifier.weight(0.4f)
                )
            }
            Text(
                text = word.glossary.joinToString(", ").take(50),
                fontSize = 12.sp,
                color = surfaceColors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}

// ── Sentence row — used in expandable sentence sections ─────

@Composable
fun KaiteyoSentenceRow(
    sentence: Sentence,
    onFuriganaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (hovered) accent.secondary.copy(alpha = 0.06f)
                else Color.Transparent
            )
            .clickable(interactionSource = interactionSource, indication = null) { }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            KaiteyoFuriganaClickable(
                furigana = sentence.furigana,
                onFuriganaClick = onFuriganaClick
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = sentence.translation,
                fontSize = 12.sp,
                color = surfaceColors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

// ── Furigana clickable — renders furigana with clickable kanji ──

@Composable
fun KaiteyoFuriganaClickable(
    furigana: ua.syt0r.kanji.core.app_data.data.FuriganaString,
    onFuriganaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ClickableFuriganaText(
        furiganaString = furigana,
        onClick = onFuriganaClick,
        modifier = modifier
    )
}


