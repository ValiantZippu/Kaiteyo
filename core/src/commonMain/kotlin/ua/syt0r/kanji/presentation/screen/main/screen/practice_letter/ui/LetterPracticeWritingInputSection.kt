package ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.BrushSettings
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWriter
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWriterDecorations
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeReviewState

@Composable
fun LetterPracticeWritingInputSection(
    state: State<LetterPracticeReviewState.Writing>,
    modifier: Modifier = Modifier,
    brushSettings: BrushSettings = BrushSettings.default
) {

    val writerState = remember { derivedStateOf { state.value.writerState.value } }

    CharacterWriterDecorations(
        modifier = modifier,
        state = writerState
    ) {

        val transition = updateTransition(
            targetState = writerState.value,
            label = "Different Characters Transition"
        )

        transition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(),
                    initialContentExit = fadeOut()
                )
            }
        ) { currentWriterState ->

            CharacterWriter(
                state = currentWriterState,
                modifier = Modifier.fillMaxSize(),
                brushSettings = brushSettings
            )

        }

    }

}
