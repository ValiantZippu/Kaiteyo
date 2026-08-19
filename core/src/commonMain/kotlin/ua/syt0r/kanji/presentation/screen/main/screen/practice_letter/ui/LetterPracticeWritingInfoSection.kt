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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.app_data.data.CharacterRadical
import ua.syt0r.kanji.core.app_data.data.formattedFurigana
import ua.syt0r.kanji.core.app_data.data.withEncodedText
import ua.syt0r.kanji.core.getUnicodeHex
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.FuriganaText
import ua.syt0r.kanji.presentation.common.ui.kanji.Kanji
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiReadingsContainer
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
private val PillShape = RoundedCornerShape(12.dp)

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
        val scrollState = remember(scrollStateResetKey) { ScrollState(0) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

            val examples = currentSectionData.characterData.examples

            if (examples.total != 0) {
                ExpressionsSection(
                    letter = currentSectionData.characterData.character,
                    reveal = state.value.run { revealCharacter || isStudyMode },
                    totalExamplesCount = examples.total,
                    examples = examples.list.value,
                    isNoTranslationLayout = currentSectionData.layoutConfiguration.noTranslationsLayout,
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

            // Kanji + meanings in a gradient card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accent.primary.copy(alpha = 0.06f),
                                accent.secondary.copy(alpha = 0.04f),
                                surfaceColors.surface.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(0.5.dp, accent.primary.copy(alpha = 0.10f), CardShape)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (isStudyMode) {
                    AnimatedKanjiSection(
                        strokes = details.strokes,
                        radicals = details.radicals,
                        shouldHighlightRadicals = shouldHighlightRadicals,
                        toggleRadicalsHighlight = toggleRadicalsHighlight
                    )
                }

                KanjiMeanings(
                    meanings = details.meanings,
                    modifier = Modifier.weight(1f)
                )

            }
        }
    }

    // Readings in a clean card
    KanjiReadingsContainer(
        on = details.on,
        kun = details.kun,
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(surfaceColors.surface.copy(alpha = 0.6f))
            .border(0.5.dp, accent.primary.copy(alpha = 0.08f), CardShape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )

    // Variants + meta info
    if (details.variants != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(accent.primary.copy(alpha = 0.06f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KanjiVariantsRow(details.variants)

            val unicodeHex = details.character.first().getUnicodeHex()
            Text(
                text = resolveString { letterPractice.unicodeTitle(unicodeHex) },
                fontSize = 11.sp,
                color = surfaceColors.textSecondary
            )

            Text(
                text = resolveString { letterPractice.strokeCountTitle(details.strokes.size) },
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
            .size(80.dp)
            .clip(MaterialTheme.shapes.small)
            .border(
                width = 1.dp,
                color = if (shouldHighlightRadicals.value) accent.primary.copy(alpha = 0.3f)
                else accent.primary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
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
private fun KanjiMeanings(
    meanings: List<String>,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current

    if (meanings.isNotEmpty()) {
        Text(
            text = meanings.joinToString(),
            style = MaterialTheme.typography.headlineSmall,
            color = surfaceColors.textPrimary,
            modifier = modifier
        )
    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpressionsSection(
    letter: String,
    reveal: Boolean,
    totalExamplesCount: Int,
    examples: List<LetterPracticeExampleWord>,
    isNoTranslationLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.secondary.copy(alpha = 0.06f),
                        surfaceColors.surface.copy(alpha = 0.7f)
                    )
                )
            )
            .border(0.5.dp, accent.secondary.copy(alpha = 0.10f), CardShape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 14.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Accent dot
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(6.dp)
            ) {
                drawCircle(accent.secondary)
            }

            Text(
                text = resolveString { letterPractice.headerWordsMessage(totalExamplesCount) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = surfaceColors.textPrimary
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {

            FlowRow(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 14.dp, top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                maxLines = 1
            ) {
                if (isNoTranslationLayout) {
                    examples.take(NoTranslationLayoutPreviewWordsLimit).forEach { exampleWord ->
                        when {
                            exampleWord.romaji != null -> Text(
                                text = exampleWord.romaji,
                                color = surfaceColors.textSecondary,
                                fontSize = 13.sp
                            )
                            else -> {
                                val string = exampleWord.word.reading.formattedFurigana()
                                    .let { if (reveal) it else it.withEncodedText(letter) }
                                FuriganaText(string)
                            }
                        }
                    }
                } else {
                    WritingPracticeVocabHeadline(
                        word = examples.first(),
                        reveal = reveal,
                        letter = letter
                    )
                }

            }

            IconButton(
                onClick = onClick,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    null,
                    tint = accent.secondary
                )
            }

        }

    }

}
