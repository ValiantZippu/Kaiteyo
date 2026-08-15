package ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.core.app_data.data.FuriganaString
import ua.syt0r.kanji.presentation.common.AutopaddedScrollableColumn
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.ui.CenteredBoxWithSide
import ua.syt0r.kanji.presentation.common.ui.FuriganaText
import ua.syt0r.kanji.presentation.common.ui.isWideContentLayout
import ua.syt0r.kanji.presentation.common.ui.rememberAdaptiveContentMaxWidth
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.FlashcardPracticeAnswerButtonsRow
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswers
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeQueueProgress
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabReviewState

@Composable
fun VocabPracticeFlashcardUI(
    reviewState: VocabReviewState.Flashcard,
    answers: PracticeAnswers,
    progress: PracticeQueueProgress,
    deckTitle: String?,
    currentStreak: Int,
    onRevealAnswerClick: () -> Unit,
    onNextClick: (PracticeAnswer) -> Unit,
    onInfoClick: () -> Unit
) {

    if (isWideContentLayout()) {
        // Desktop: contextual session panel beside the card — progress, deck
        // and streak — instead of a bare card floating in a huge window.
        Row(modifier = Modifier.fillMaxSize()) {
            FlashcardContextPanel(
                progress = progress,
                deckTitle = deckTitle,
                currentStreak = currentStreak
            )
            VerticalDivider()
            Box(modifier = Modifier.weight(1f)) {
                FlashcardContent(
                    reviewState = reviewState,
                    answers = answers,
                    onRevealAnswerClick = onRevealAnswerClick,
                    onNextClick = onNextClick,
                    onInfoClick = onInfoClick
                )
            }
        }
        return
    }

    FlashcardContent(
        reviewState = reviewState,
        answers = answers,
        onRevealAnswerClick = onRevealAnswerClick,
        onNextClick = onNextClick,
        onInfoClick = onInfoClick
    )
}

/**
 * Desktop-only session context shown beside the card in wide layouts:
 * queue progress, the deck being studied and the current streak. Kept
 * quiet so the card itself stays the focus.
 */
@Composable
private fun FlashcardContextPanel(
    progress: PracticeQueueProgress,
    deckTitle: String?,
    currentStreak: Int
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .widthIn(max = 240.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Session",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val total = progress.pending + progress.completed
        Text(
            text = if (total > 0) "${progress.completed} / $total done" else "Preparing…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (progress.repeats > 0) {
            Text(
                text = "${progress.repeats} repeat(s) in queue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        deckTitle?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Deck",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (currentStreak > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Streak",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$currentStreak day(s)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FlashcardContent(
    reviewState: VocabReviewState.Flashcard,
    answers: PracticeAnswers,
    onRevealAnswerClick: () -> Unit,
    onNextClick: (PracticeAnswer) -> Unit,
    onInfoClick: () -> Unit
) {

    AutopaddedScrollableColumn(
        modifier = Modifier.fillMaxSize(),
        bottomOverlayContent = {
            FlashcardPracticeAnswerButtonsRow(
                answers = answers,
                showAnswer = reviewState.showAnswer,
                onRevealAnswerClick = onRevealAnswerClick,
                onAnswerClick = onNextClick
            )
        }
    ) {

        val meaningUI = @Composable {
            CenteredBoxWithSide(
                modifier = Modifier.widthIn(max = rememberAdaptiveContentMaxWidth(
                    phoneMax = 400.dp,
                    mediumMax = 480.dp,
                    wideMax = 560.dp
                )),
                placeSideContentAtStart = false,
                centerContent = {
                    Text(
                        text = reviewState.meaning,
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                sideContent = {
                    IconButton(
                        enabled = reviewState.showAnswer.value,
                        onClick = onInfoClick
                    ) {
                        Icon(Icons.Default.ArrowOutward, null)
                    }
                }
            )
        }

        val wordUI = @Composable { furigana: FuriganaString ->
            FuriganaText(
                furiganaString = furigana,
                textStyle = MaterialTheme.typography.displayLarge,
                annotationTextStyle = MaterialTheme.typography.bodyLarge
            )
        }

        val sentenceUI = @Composable { showTranslation: Boolean ->
            reviewState.exampleSentence?.let {
                Spacer(Modifier.height(Dimens.SpacingBig))
                SelectionContainer {
                    if (showTranslation) {
                        FuriganaText(
                            it.furigana,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.widthIn(max = rememberAdaptiveContentMaxWidth(
                                phoneMax = 400.dp,
                                mediumMax = 480.dp,
                                wideMax = 560.dp
                            ))
                        )
                    } else {
                        Text(
                            text = it.text,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = rememberAdaptiveContentMaxWidth(
                                phoneMax = 400.dp,
                                mediumMax = 480.dp,
                                wideMax = 560.dp
                            ))
                        )
                    }
                }
                if (showTranslation) {
                    SelectionContainer {
                        Text(
                            text = it.translation,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = rememberAdaptiveContentMaxWidth(
                                phoneMax = 400.dp,
                                mediumMax = 480.dp,
                                wideMax = 560.dp
                            ))
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (reviewState.showMeaningInFront) {

                meaningUI()

                if (reviewState.showAnswer.value) {
                    Spacer(Modifier.height(8.dp))
                    wordUI(reviewState.reading)
                    sentenceUI(true)
                }

            } else {

                val text = reviewState.run { if (showAnswer.value) reading else noFuriganaReading }
                wordUI(text)

                if (reviewState.showAnswer.value) {
                    Spacer(Modifier.height(Dimens.SpacingMid))
                    meaningUI()
                }

                sentenceUI(reviewState.showAnswer.value)

            }

        }

    }

}
