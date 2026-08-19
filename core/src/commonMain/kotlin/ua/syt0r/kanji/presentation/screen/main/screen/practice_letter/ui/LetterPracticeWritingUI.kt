package ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.core.app_data.data.formattedVocabDefinition
import ua.syt0r.kanji.core.app_data.data.withEncodedText
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.presentation.common.AppListItemDefaults
import ua.syt0r.kanji.presentation.common.FuriganaWordHeadline
import ua.syt0r.kanji.presentation.common.MultiplatformBackHandler
import ua.syt0r.kanji.presentation.common.clickable
import ua.syt0r.kanji.presentation.common.copyCentered
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.trackItemPosition
import ua.syt0r.kanji.presentation.common.ui.FuriganaText
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Material3BottomSheetScaffold
import ua.syt0r.kanji.presentation.common.ui.Orientation
import ua.syt0r.kanji.presentation.common.ui.rememberAdaptiveContentMaxWidth
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.BrushSelector
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.BrushSettings
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWritingProgress
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswerButtonsContainer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswerButtonsRow
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswers
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.WritingAttemptStats
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeExampleWord
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeLayoutConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeReviewState

@Composable
fun LetterPracticeWritingUI(
    layoutConfiguration: LetterPracticeLayoutConfiguration.WritingLayoutConfiguration,
    reviewState: LetterPracticeReviewState.Writing,
    onNextClick: (PracticeAnswer) -> Unit,
    speakKana: (KanaReading) -> Unit,
    onWordClick: (JapaneseWord) -> Unit
) {

    val reviewState = rememberUpdatedState(reviewState)

    var brushSettings by remember { mutableStateOf(BrushSettings.default) }

    val infoSectionState = reviewState.asInfoSectionState(layoutConfiguration)
    val wordsBottomSheetState = reviewState.asWordsBottomSheetState()

    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    if (scaffoldState.bottomSheetState.isExpanded) {
        MultiplatformBackHandler {
            coroutineScope.launch { scaffoldState.bottomSheetState.collapse() }
        }
    }

    val expressionSectionCoordinates = remember { MutableSharedFlow<LayoutCoordinates?>(1) }
    val bottomSheetScaffoldCoordinates = remember { MutableSharedFlow<LayoutCoordinates?>(1) }

    val onExpressionSectionCoordinatesUpdate: (LayoutCoordinates?) -> Unit = {
        coroutineScope.launch { expressionSectionCoordinates.emit(it) }
    }
    val onBottomSheetScaffoldCoordinatesUpdate: (LayoutCoordinates) -> Unit = {
        coroutineScope.launch { bottomSheetScaffoldCoordinates.emit(it) }
    }

    val bottomSheetHeight = letterPracticeWritingWordsBottomSheetHeight(
        scaffoldCoordinates = bottomSheetScaffoldCoordinates,
        expressionSectionCoordinates = expressionSectionCoordinates
    )

    val openBottomSheet: () -> Unit = {
        coroutineScope.launch { scaffoldState.bottomSheetState.expand() }
    }

    val hideBottomSheet: () -> Unit = {
        coroutineScope.launch { scaffoldState.bottomSheetState.collapse() }
    }

    val answersSection: @Composable BoxScope.() -> Unit = {
        AnswerButtons(
            letterWritingButtonsState = reviewState.toAnswerButtonsState(),
            studyCompleted = { reviewState.value.isStudyMode.value = false },
            answerSelected = onNextClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (LocalOrientation.current == Orientation.Portrait) {

        Material3BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
                LetterPracticeWritingWordsBottomSheet(
                    state = wordsBottomSheetState,
                    sheetContentHeight = bottomSheetHeight,
                    hideSheet = hideBottomSheet,
                    onWordClick = onWordClick
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned(onBottomSheetScaffoldCoordinatesUpdate)
        ) {

            val infoSectionBottomPadding = remember { mutableStateOf(0.dp) }

            LetterPracticeWritingInfoSection(
                state = infoSectionState,
                onExpressionsClick = openBottomSheet,
                onExpressionSectionCoordinatesUpdate = onExpressionSectionCoordinatesUpdate,
                speakKana = speakKana,
                extraBottomPaddingState = infoSectionBottomPadding,
                modifier = Modifier.fillMaxSize(),
            )

            // Brush selector toolbar — floating above drawing input
            BrushSelector(
                brushSettings = brushSettings,
                onBrushSettingsChange = { brushSettings = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .trackItemPosition { infoSectionBottomPadding.value = it.heightFromScreenBottom }
                    .sizeIn(maxWidth = rememberAdaptiveContentMaxWidth(
                        phoneMax = 400.dp,
                        mediumMax = 480.dp,
                        wideMax = 560.dp
                    ))
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 4.dp)
            )

            LetterPracticeWritingInputSection(
                state = reviewState,
                brushSettings = brushSettings,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .trackItemPosition {
                        infoSectionBottomPadding.value = it.heightFromScreenBottom
                    }
                    .sizeIn(maxWidth = rememberAdaptiveContentMaxWidth(
                        phoneMax = 400.dp,
                        mediumMax = 480.dp,
                        wideMax = 560.dp
                    ))
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 64.dp)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
            )

            answersSection()

        }

    } else {

        val accent = LocalKaiteyoAccent.current

        val infoSection: @Composable RowScope.() -> Unit = {
            Material3BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetContent = {
                    LetterPracticeWritingWordsBottomSheet(
                        state = wordsBottomSheetState,
                        sheetContentHeight = bottomSheetHeight,
                        hideSheet = hideBottomSheet,
                        onWordClick = onWordClick
                    )
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .onGloballyPositioned(onBottomSheetScaffoldCoordinatesUpdate)
            ) {
                LetterPracticeWritingInfoSection(
                    state = infoSectionState,
                    onExpressionsClick = openBottomSheet,
                    onExpressionSectionCoordinatesUpdate = onExpressionSectionCoordinatesUpdate,
                    speakKana = speakKana,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        val inputSection: @Composable RowScope.() -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .wrapContentSize()
            ) {
                BrushSelector(
                    brushSettings = brushSettings,
                    onBrushSettingsChange = { brushSettings = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                LetterPracticeWritingInputSection(
                    state = reviewState,
                    brushSettings = brushSettings,
                    modifier = Modifier
                        .fillMaxSize()
                        .sizeIn(maxWidth = rememberAdaptiveContentMaxWidth(
                            phoneMax = 400.dp,
                            mediumMax = 480.dp,
                            wideMax = 560.dp
                        ))
                        .aspectRatio(1f)
                        .padding(16.dp)
                )
            }
        }

        val (firstSection, secondSection) = when (layoutConfiguration.leftHandedMode) {
            true -> inputSection to infoSection
            false -> infoSection to inputSection
        }

        Box {

            Row(
                modifier = Modifier.fillMaxSize()
            ) {

                firstSection()

                // Accent gradient divider between panels
                Spacer(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 1.dp, max = 1.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accent.primary.copy(alpha = 0.0f),
                                    accent.primary.copy(alpha = 0.15f),
                                    accent.secondary.copy(alpha = 0.15f),
                                    accent.primary.copy(alpha = 0.0f)
                                )
                            )
                        )
                )

                secondSection()

            }

            answersSection()

        }

    }

}


private sealed interface LetterWritingButtonsState {
    object Hidden : LetterWritingButtonsState
    object StudyButtons : LetterWritingButtonsState
    data class DefaultButtons(
        val answers: PracticeAnswers,
        val mistakes: Int,
        val writingStats: WritingAttemptStats? = null
    ) : LetterWritingButtonsState
}

@Composable
private fun State<LetterPracticeReviewState.Writing>.toAnswerButtonsState(): State<LetterWritingButtonsState> =
    remember {
        derivedStateOf {
            val currentState = value
            val writerState = currentState.writerState.value
            val progress = writerState.progress.value

            when {
                progress is CharacterWritingProgress.Completed.Idle -> {
                    if (currentState.isStudyMode.value) LetterWritingButtonsState.StudyButtons
                    else LetterWritingButtonsState.DefaultButtons(
                        answers = currentState.answers,
                        mistakes = progress.mistakes,
                        writingStats = currentState.writerState.value.attemptStats.value
                    )
                }

                else -> LetterWritingButtonsState.Hidden
            }
        }
    }


@Composable
private fun AnswerButtons(
    letterWritingButtonsState: State<LetterWritingButtonsState>,
    studyCompleted: () -> Unit,
    answerSelected: (PracticeAnswer) -> Unit,
    modifier: Modifier
) {

    val buttonsTransition = updateTransition(letterWritingButtonsState.value)
    buttonsTransition.AnimatedContent(
        transitionSpec = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn() togetherWith
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut()
        },
        contentKey = { it !is LetterWritingButtonsState.Hidden },
        modifier = modifier
    ) { writingButtonsState ->

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {

            when (writingButtonsState) {
                LetterWritingButtonsState.Hidden -> {

                }

                LetterWritingButtonsState.StudyButtons -> {
                    PracticeAnswerButtonsContainer {
                        Row(
                            modifier = Modifier
                                .padding(AppListItemDefaults.ListItemDefaultPaddings)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(studyCompleted)
                                .widthIn(max = rememberAdaptiveContentMaxWidth(
                                    phoneMax = 400.dp,
                                    mediumMax = 480.dp,
                                    wideMax = 560.dp
                                ))
                                .padding(Dimens.SpacingBig),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMid)
                        ) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = resolveString { letterPractice.studyFinishedButton },
                                modifier = Modifier,
                                style = MaterialTheme.typography.bodyLarge.copyCentered(),
                                textAlign = TextAlign.Center
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.NavigateNext,
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(24.dp, 24.dp)
                                    .wrapContentSize(Alignment.CenterStart)
                            )
                        }
                    }
                }

                is LetterWritingButtonsState.DefaultButtons -> {
                    PracticeAnswerButtonsRow(
                        answers = writingButtonsState.answers,
                        onClick = {
                            val updatedAnswer = it.copy(
                                mistakes = writingButtonsState.mistakes,
                                writingStats = writingButtonsState.writingStats
                            )
                            answerSelected(updatedAnswer)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WritingPracticeVocabHeadline(
    word: LetterPracticeExampleWord,
    reveal: Boolean,
    letter: String
) {
    when {
        word.romaji != null -> {
            Text(formattedVocabDefinition(word.romaji, word.word.combinedGlossary()))
        }

        reveal -> {
            FuriganaWordHeadline(
                reading = word.word.reading,
                glossary = word.word.combinedGlossary()
            )
        }

        else -> {
            val text = formattedVocabDefinition(word.word.reading, word.word.combinedGlossary())
                .withEncodedText(letter)
            FuriganaText(text)
        }
    }
}
