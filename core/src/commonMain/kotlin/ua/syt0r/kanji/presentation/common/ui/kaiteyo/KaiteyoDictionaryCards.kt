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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.animation.core.Animatable
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
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.ClickableFuriganaText
import ua.syt0r.kanji.presentation.common.ui.FuriganaText
import ua.syt0r.kanji.presentation.common.ui.kanji.Kanji
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiBackground
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiRadicalDetails
import ua.syt0r.kanji.presentation.screen.main.screen.info.LetterInfoData
import ua.syt0r.kanji.presentation.screen.main.screen.info.VocabInfoData

// ============================================================
// KAITEYO DICTIONARY CARDS — the presentation kit
//
// Every word / kanji / sentence screen is assembled from these
// cards so the whole dictionary feels like one living reference:
//   · rounded section cards with gradient backgrounds + glow
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
// Kaiteyo identity: gradient surface background, subtle glow border,
// inner highlight, hover accent bloom. NOT generic Material.

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

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val baseModifier = modifier
        .clip(KaiteyoCardShape)
        .drawBehind {
            // 1) Gradient surface background — subtle accent tint, not flat
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accent.primary.copy(alpha = 0.04f),
                        accent.secondary.copy(alpha = 0.02f),
                        surfaceColors.surface
                    ),
                    startY = 0f,
                    endY = size.height
                ),
                cornerRadius = CornerRadius(16.dp.toPx()),
                size = size
            )

            // 2) Gradient border — accent sweep at low alpha
            val borderAlpha = if (hovered) 0.35f else 0.15f
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.primary.copy(alpha = borderAlpha),
                        accent.secondary.copy(alpha = borderAlpha * 0.6f),
                        accent.primary.copy(alpha = borderAlpha * 0.4f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3) Inner top-edge highlight — premium depth
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (hovered) 0.06f else 0.03f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.3f
                ),
                cornerRadius = CornerRadius(16.dp.toPx()),
                size = size
            )

            // 4) Hover glow bloom — accent radiates from center
            if (hovered) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.primary.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.6f
                    ),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    size = size
                )
            }
        }

    val clickModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else baseModifier

    Column(modifier = clickModifier) {
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

@Composable
fun KaiteyoKanjiPill(
    character: String,
    meaning: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .clip(KaiteyoPillShape)
            .background(
                if (hovered) accent.primary.copy(alpha = 0.20f)
                else accent.primary.copy(alpha = 0.10f)
            )
            .border(1.dp, accent.primary.copy(alpha = 0.25f), KaiteyoPillShape)
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

// ── Reading rows ────────────────────────────────────────────

@Composable
fun KaiteyoReadingsCard(
    on: List<String>,
    kun: List<String>,
    vocab: List<JapaneseWord> = emptyList(),
    onPlayReading: ((String) -> Unit)? = null,
    isPlayingReading: String? = null,
    onWordClick: ((JapaneseWord) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var onExpanded by remember { mutableStateOf(false) }
    var kunExpanded by remember { mutableStateOf(false) }

    KaiteyoCard(
        modifier = modifier,
        header = "Readings",
        subtitle = "On'yomi and Kun'yomi — tap heading to expand vocab"
    ) {
        if (on.isEmpty() && kun.isEmpty()) {
            Text("No readings available.", fontSize = 12.sp, color = surfaceColors.textMuted)
            return@KaiteyoCard
        }

        // ── On'yomi ──────────────────────────────────────
        if (on.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpanded = !onExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "On",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent.secondary,
                    modifier = Modifier.width(28.dp)
                )
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    on.forEach { reading ->
                        ReadingPill(
                            text = reading,
                            isPlaying = isPlayingReading == reading,
                            onClick = { onPlayReading?.invoke(reading) }
                        )
                    }
                }
                Text(
                    text = if (onExpanded) "▾" else "▸",
                    fontSize = 12.sp,
                    color = surfaceColors.textMuted
                )
            }
            // Expandable on'yomi vocab
            if (onExpanded && vocab.isNotEmpty()) {
                val onVocab = vocab.filter { word ->
                    val reading = word.reading.kanjiReading ?: word.reading.kanaReading
                    on.any { r ->
                        // Match reading without okurigana markers
                        val cleanReading = r.replace(".*".toRegex(), "")
                        reading.contains(cleanReading)
                    }
                }
                if (onVocab.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 34.dp, top = 4.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        onVocab.take(10).forEach { word ->
                            KaiteyoVocabRow(
                                word = word,
                                onClick = { onWordClick?.invoke(word) },
                                onBookmarkClick = { }
                            )
                        }
                        if (onVocab.size > 10) {
                            Text(
                                text = "+${onVocab.size - 10} more…",
                                fontSize = 10.sp,
                                color = surfaceColors.textMuted
                            )
                        }
                    }
                }
            }
        }

        // ── Kun'yomi ─────────────────────────────────────
        if (kun.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { kunExpanded = !kunExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Kun",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent.primary,
                    modifier = Modifier.width(28.dp)
                )
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    kun.forEach { reading ->
                        ReadingPill(
                            text = reading,
                            isPlaying = isPlayingReading == reading,
                            onClick = { onPlayReading?.invoke(reading) }
                        )
                    }
                }
                Text(
                    text = if (kunExpanded) "▾" else "▸",
                    fontSize = 12.sp,
                    color = surfaceColors.textMuted
                )
            }
            // Expandable kun'yomi vocab
            if (kunExpanded && vocab.isNotEmpty()) {
                val kunVocab = vocab.filter { word ->
                    val reading = word.reading.kanjiReading ?: word.reading.kanaReading
                    kun.any { r ->
                        val cleanReading = r.replace(".*".toRegex(), "")
                        reading.contains(cleanReading)
                    }
                }
                if (kunVocab.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 34.dp, top = 4.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        kunVocab.take(10).forEach { word ->
                            KaiteyoVocabRow(
                                word = word,
                                onClick = { onWordClick?.invoke(word) },
                                onBookmarkClick = { }
                            )
                        }
                        if (kunVocab.size > 10) {
                            Text(
                                text = "+${kunVocab.size - 10} more…",
                                fontSize = 10.sp,
                                color = surfaceColors.textMuted
                            )
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
        if (isPlaying) {
            Icon(
                imageVector = Icons.Default.MoreVert, // TODO: use proper play icon
                contentDescription = null,
                tint = accent.primary,
                modifier = Modifier.size(12.dp)
            )
        }
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

    KaiteyoCard(
        modifier = modifier,
        header = "Meanings",
        subtitle = data.meanings.joinToString(", ")
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            data.meanings.forEach { meaning ->
                KaiteyoBadge(
                    text = meaning,
                    containerColor = surfaceColors.surfaceInteractive.copy(alpha = 0.45f),
                    contentColor = surfaceColors.textPrimary
                )
            }
            data.jlptLevel?.let { jlpt ->
                KaiteyoBadge(
                    text = "JLPT N$jlpt",
                    containerColor = LocalKaiteyoAccent.current.primary.copy(alpha = 0.14f),
                    contentColor = LocalKaiteyoAccent.current.primary
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
    modifier: Modifier = Modifier
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    KaiteyoCard(
        modifier = modifier,
        header = "Decomposition",
        subtitle = "Components that compose this kanji"
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            radicals.forEach { radical ->
                KaiteyoKanjiPill(
                    character = radical.value,
                    meaning = radical.meanings.firstOrNull(),
                    onClick = { onRadicalClick(radical.value) }
                )
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
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val reading = word.reading
    val displayText = reading.kanjiReading ?: reading.kanaReading

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (hovered) accent.primary.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = displayText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textPrimary
        )
        Text(
            text = word.glossary.joinToString(", ").take(40),
            fontSize = 12.sp,
            color = surfaceColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
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

// ── Kanji hero — the big character display card ─────────────

@Composable
fun KaiteyoKanjiHero(
    data: LetterInfoData.Kanji,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    KaiteyoCard(modifier = modifier, contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 20.dp)) {
        // Type badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KaiteyoBadge(
                text = "漢字",
                containerColor = accent.primary.copy(alpha = 0.14f),
                contentColor = accent.primary
            )
            data.jlptLevel?.let { jlpt ->
                KaiteyoBadge(
                    text = "JLPT N${jlpt}",
                    containerColor = accent.secondary.copy(alpha = 0.14f),
                    contentColor = accent.secondary
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Stroke order — static display with all strokes visible
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            KanjiBackground(Modifier.fillMaxSize())
            Kanji(
                strokes = data.strokes,
                modifier = Modifier.size(100.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "${data.strokes.size} strokes",
            fontSize = 11.sp,
            color = surfaceColors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Meanings
        Text(
            text = data.meanings.joinToString(", "),
            fontSize = 14.sp,
            color = surfaceColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Readings preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (data.on.isNotEmpty()) {
                Text(
                    text = data.on.joinToString(" "),
                    fontSize = 13.sp,
                    color = accent.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
            if (data.on.isNotEmpty() && data.kun.isNotEmpty()) {
                Text(
                    text = "·",
                    fontSize = 13.sp,
                    color = surfaceColors.textMuted
                )
            }
            if (data.kun.isNotEmpty()) {
                Text(
                    text = data.kun.joinToString(" "),
                    fontSize = 13.sp,
                    color = accent.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KaiteyoActionButton(
                label = "📋 Copy",
                onClick = onCopy,
                container = accent.primary.copy(alpha = 0.14f),
                content = accent.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
