package ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.app_data.data.CharacterRadical
import ua.syt0r.kanji.core.app_data.data.formattedFurigana
import ua.syt0r.kanji.core.app_data.data.formattedVocabDefinition
import ua.syt0r.kanji.core.app_data.data.withEncodedText
import ua.syt0r.kanji.core.getUnicodeHex
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.FuriganaText
import ua.syt0r.kanji.presentation.common.ui.kanji.Kanji
import ua.syt0r.kanji.presentation.common.ui.kanji.RadicalKanji
import ua.syt0r.kanji.presentation.common.ui.kanji.getColoredKanjiStrokes
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWriterConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWritingProgress
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeExampleWord
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeItemData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeLayoutConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeReviewState
import kotlin.math.min

private const val NoTranslationLayoutPreviewWordsLimit = 5

data class WritingPracticeInfoSectionData(
    val characterData: LetterPracticeItemData.WritingData,
    val isStudyMode: Boolean,
    val revealCharacter: Boolean,
    val layoutConfiguration: LetterPracticeLayoutConfiguration.WritingLayoutConfiguration
)

@Composable
fun State<LetterPracticeReviewState.Writing>.asInfoSectionState(
    layoutConfiguration: LetterPracticeLayoutConfiguration.WritingLayoutConfiguration
): State<WritingPracticeInfoSectionData> {
    return remember {
        derivedStateOf {
            val currentState = value
            val writerState = currentState.writerState.value
            val revealCharacter = writerState.progress.value !is CharacterWritingProgress.Writing

            when (val configuration = writerState.configuration) {
                is CharacterWriterConfiguration.CharacterInput -> {
                    WritingPracticeInfoSectionData(
                        characterData = currentState.itemData,
                        isStudyMode = false,
                        revealCharacter = revealCharacter,
                        layoutConfiguration = layoutConfiguration
                    )
                }

                is CharacterWriterConfiguration.StrokeInput -> {
                    WritingPracticeInfoSectionData(
                        characterData = currentState.itemData,
                        isStudyMode = configuration.isStudyMode,
                        revealCharacter = revealCharacter,
                        layoutConfiguration = layoutConfiguration
                    )
                }
            }
        }
    }
}

private val MaxTransitionSlideDistance = 200.dp
private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun LetterPracticeWritingInfoSection(
    state: State<WritingPracticeInfoSectionData>,
    onExpressionsClick: () -> Unit,
    onExpressionSectionCoordinatesUpdate: (LayoutCoordinates?) -> Unit,
    speakKana: (KanaReading) -> Unit,
    extraBottomPaddingState: State<Dp> = rememberUpdatedState(0.dp),
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(
        targetState = state.value,
        label = "Content Change Transition"
    )

    val density = LocalDensity.current
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    transition.AnimatedContent(
        contentKey = { it.characterData.character to it.isStudyMode },
        modifier = modifier,
        transitionSpec = {
            val enterTransition = slideInHorizontally {
                min(it / 3, with(density) { MaxTransitionSlideDistance.roundToPx() })
            } + fadeIn()
            val exitTransition = slideOutHorizontally {
                -min(it / 3, with(density) { MaxTransitionSlideDistance.roundToPx() })
            } + fadeOut()
            ContentTransform(
                targetContentEnter = enterTransition,
                initialContentExit = exitTransition,
                sizeTransform = SizeTransform(clip = false)
            )
        }
    ) { currentSectionData ->

        val scrollStateResetKey = currentSectionData.run { characterData.character to isStudyMode }
        val scrollState = remember(scrollStateResetKey) { androidx.compose.foundation.ScrollState(0) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            when (currentSectionData.characterData) {
                is LetterPracticeItemData.KanaWritingData -> {
                    val autoPlay = currentSectionData.layoutConfiguration.kanaAutoPlay
                    KanaDetails(
                        details = currentSectionData.characterData,
                        isStudyMode = currentSectionData.isStudyMode,
                        autoPlay = autoPlay,
                        toggleAutoPlay = { autoPlay.value = autoPlay.value.not() },
                        speakKana = speakKana
                    )
                }

                is LetterPracticeItemData.KanjiWritingData -> {
                    val highlightRadicals = currentSectionData.layoutConfiguration.radicalsHighlight
                    KanjiDetails(
                        details = currentSectionData.characterData,
                        isStudyMode = currentSectionData.isStudyMode,
                        noTranslationsLayout = currentSectionData.layoutConfiguration.noTranslationsLayout,
                        shouldHighlightRadicals = highlightRadicals,
                        toggleRadicalsHighlight = {
                            highlightRadicals.value = highlightRadicals.value.not()
                        }
                    )
                }
            }

            // Vocab list
            val examples = currentSectionData.characterData.examples
            if (examples.total != 0) {
                VocabListSection(
                    letter = currentSectionData.characterData.character,
                    reveal = state.value.run { revealCharacter || isStudyMode },
                    examples = examples.list.value,
                    totalCount = examples.total,
                    onClick = onExpressionsClick,
                    modifier = Modifier.onGloballyPositioned {
                        if (state.value == currentSectionData)
                            onExpressionSectionCoordinatesUpdate(it)
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    if (state.value == currentSectionData)
                        onExpressionSectionCoordinatesUpdate(null)
                }
            }

            Spacer(modifier = Modifier.height(extraBottomPaddingState.value))
        }
    }
}

@Composable
private fun ColumnScope.KanaDetails(
    details: LetterPracticeItemData.KanaWritingData,
    isStudyMode: Boolean,
    autoPlay: State<Boolean>,
    toggleAutoPlay: () -> Unit,
    speakKana: (KanaReading) -> Unit
) {
    if (isStudyMode) {
        Kanji(
            strokes = details.strokes,
            modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally)
        )
    }

    LetterPracticeKanaInfo(
        kanaSystem = details.kanaSystem,
        reading = details.reading,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )

    KanaVoiceMenu(
        autoPlayEnabled = autoPlay,
        clickable = true,
        onAutoPlayToggleClick = toggleAutoPlay,
        onSpeakClick = { speakKana(details.reading) }
    )
}

@Composable
private fun ColumnScope.KanjiDetails(
    details: LetterPracticeItemData.KanjiWritingData,
    isStudyMode: Boolean,
    noTranslationsLayout: Boolean,
    shouldHighlightRadicals: State<Boolean>,
    toggleRadicalsHighlight: () -> Unit,
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    when {
        noTranslationsLayout -> {
            if (isStudyMode) {
                AnimatedKanjiSection(
                    strokes = details.strokes,
                    radicals = details.radicals,
                    shouldHighlightRadicals = shouldHighlightRadicals,
                    toggleRadicalsHighlight = toggleRadicalsHighlight,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        else -> {
            // Clean kanji header with meanings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(surfaceColors.surface)
                    .border(0.5.dp, accent.primary.copy(alpha = 0.10f), CardShape)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large kanji character
                if (isStudyMode) {
                    AnimatedKanjiSection(
                        strokes = details.strokes,
                        radicals = details.radicals,
                        shouldHighlightRadicals = shouldHighlightRadicals,
                        toggleRadicalsHighlight = toggleRadicalsHighlight,
                        modifier = Modifier.size(100.dp)
                    )
                } else {
                    // Placeholder for kanji stroke preview
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(surfaceColors.surfaceInteractive.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = details.character,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Light,
                            color = accent.primary
                        )
                    }
                }

                // Meanings
                Text(
                    text = details.meanings.joinToString(", "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = surfaceColors.textPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    // Readings in a clean card
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(surfaceColors.surface)
            .border(0.5.dp, accent.primary.copy(alpha = 0.08f), CardShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // On'yomi
        if (details.on.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "On",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent.secondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    details.on.forEach { reading ->
                        ReadingPill(reading = reading, isOnyomi = true)
                    }
                }
            }
        }

        // Kun'yomi
        if (details.kun.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Kun",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    details.kun.forEach { reading ->
                        ReadingPill(reading = reading, isOnyomi = false)
                    }
                }
            }
        }
    }

    // Variants + meta info
    if (details.variants != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(surfaceColors.surface)
                .border(0.5.dp, accent.primary.copy(alpha = 0.08f), CardShape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Variants label
            Text(
                text = "Variants:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = surfaceColors.textSecondary
            )

            // Click to reveal
            Text(
                text = "Click to reveal",
                fontSize = 12.sp,
                color = accent.primary,
                modifier = Modifier.clickable { }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Unicode
            val unicodeHex = details.character.first().getUnicodeHex()
            Text(
                text = "Unicode: U+$unicodeHex",
                fontSize = 11.sp,
                color = surfaceColors.textMuted
            )

            // Stroke count
            Text(
                text = "Stroke count: ${details.strokes.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent.primary
            )
        }
    }
}

@Composable
private fun AnimatedKanjiSection(
    strokes: List<Path>,
    radicals: List<CharacterRadical>,
    shouldHighlightRadicals: State<Boolean>,
    toggleRadicalsHighlight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalKaiteyoAccent.current

    val radicalsTransition = updateTransition(
        targetState = shouldHighlightRadicals.value,
        label = "Radical highlight transition"
    )

    radicalsTransition.AnimatedContent(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (shouldHighlightRadicals.value) accent.primary.copy(alpha = 0.3f)
                else accent.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = toggleRadicalsHighlight),
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { shouldHighlight ->
        when (shouldHighlight) {
            true -> RadicalKanji(
                strokes = getColoredKanjiStrokes(
                    strokes = strokes,
                    radicalToStrokeRangeList = radicals.map {
                        val radicalStrokeRange =
                            it.startPosition until (it.startPosition + it.strokesCount)
                        it.radical to radicalStrokeRange
                    }
                ),
                modifier = Modifier.fillMaxSize()
            )

            false -> Kanji(
                strokes = strokes,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ReadingPill(
    reading: String,
    isOnyomi: Boolean
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    val bgColor = if (isOnyomi) accent.secondary.copy(alpha = 0.12f) else accent.primary.copy(alpha = 0.12f)
    val textColor = if (isOnyomi) accent.secondary else accent.primary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Play icon
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = reading,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun VocabListSection(
    letter: String,
    reveal: Boolean,
    examples: List<LetterPracticeExampleWord>,
    totalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(surfaceColors.surface)
            .border(0.5.dp, accent.primary.copy(alpha = 0.08f), CardShape)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Words ($totalCount)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = surfaceColors.textPrimary
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                null,
                tint = accent.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Vocab list items
        examples.take(10).forEachIndexed { index, exampleWord ->
            VocabListItem(
                index = index + 1,
                word = exampleWord,
                reveal = reveal,
                letter = letter,
                onClick = onClick
            )
        }

        if (totalCount > 10) {
            Text(
                text = "+${totalCount - 10} more words...",
                fontSize = 12.sp,
                color = surfaceColors.textMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun VocabListItem(
    index: Int,
    word: LetterPracticeExampleWord,
    reveal: Boolean,
    letter: String,
    onClick: () -> Unit
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Number
        Text(
            text = "$index",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = surfaceColors.textMuted,
            modifier = Modifier.width(24.dp)
        )

        // Word content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Furigana
            if (word.word.reading.furigana != null) {
                FuriganaText(
                    furiganaString = word.word.reading.furigana,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Kanji + reading
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val kanjiText = word.word.reading.kanjiReading ?: word.word.reading.kanaReading
                Text(
                    text = kanjiText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = surfaceColors.textPrimary
                )
                Text(
                    text = "·",
                    fontSize = 16.sp,
                    color = surfaceColors.textMuted
                )
                Text(
                    text = word.word.combinedGlossary().take(40),
                    fontSize = 13.sp,
                    color = surfaceColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Add button
        IconButton(
            onClick = { },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add to deck",
                tint = accent.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
