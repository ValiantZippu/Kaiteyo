package ua.syt0r.kanji.presentation.screen.main.screen.practice_common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.syt0r.kanji.presentation.common.resources.icon.ExtraIcons
import ua.syt0r.kanji.presentation.common.resources.icon.Help
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.snapToBiggerContainerCrossfadeTransitionSpec
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiBackground

/**
 * Premium writing canvas frame.
 * Accent-gradient outer glow, subtle inner shadow, theme-aware colors.
 */
@Composable
fun CharacterWriterDecorations(
    modifier: Modifier,
    state: State<CharacterWriterState?>,
    content: @Composable BoxScope.() -> Unit
) {

    val inputShape = MaterialTheme.shapes.extraLarge
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(inputShape)
            .background(surfaceColors.surface, inputShape)
            .drawBehind {
                // Outer glow ring — accent gradient at very low opacity
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.primary.copy(alpha = 0.18f),
                            accent.secondary.copy(alpha = 0.12f),
                            accent.primary.copy(alpha = 0.08f)
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    cornerRadius = CornerRadius(32.dp.toPx()),
                    size = size
                )
                // Inner shadow overlay — top/left highlight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.03f)
                        ),
                        startY = 0f,
                        endY = size.height
                    ),
                    cornerRadius = CornerRadius(32.dp.toPx()),
                    size = size
                )
            }
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.primary.copy(alpha = 0.35f),
                        accent.secondary.copy(alpha = 0.20f),
                        accent.primary.copy(alpha = 0.35f)
                    )
                ),
                shape = inputShape
            )
            .padding(2.dp)
    ) {

        KanjiBackground(Modifier.fillMaxSize())
        content()
        SingleStrokeInputButtons(state)
        MultiStrokeInputButtons(state)
        SummaryButtons(state)

    }

}

@Composable
private fun BoxScope.SingleStrokeInputButtons(state: State<CharacterWriterState?>) {
    val transitionState = remember {
        derivedStateOf {
            val writerState = state.value
            val contentState = writerState?.content?.value
                ?.let { it as? CharacterWriterContent.SingleStrokeInput }
            val progress = writerState?.progress?.value

            contentState?.takeIf { progress == CharacterWritingProgress.Writing }

        }
    }

    val coroutineScope = rememberCoroutineScope()
    val accent = LocalKaiteyoAccent.current
    val transition = updateTransition(transitionState.value)

    transition.AnimatedContent(
        transitionSpec = snapToBiggerContainerCrossfadeTransitionSpec(),
        modifier = Modifier.align(Alignment.TopEnd)
    ) {
        if (it == null) return@AnimatedContent

        IconButton(
            onClick = { coroutineScope.launch { it.notifyHintClick() } },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = accent.primary.copy(alpha = 0.10f),
                contentColor = accent.primary
            )
        ) {
            Icon(ExtraIcons.Help, null)
        }
    }

    transition.AnimatedContent(
        transitionSpec = snapToBiggerContainerCrossfadeTransitionSpec(),
        modifier = Modifier.align(Alignment.BottomEnd)
    ) {
        if (it == null) return@AnimatedContent

        IconButton(
            onClick = { it.skipRemainingStrokes() },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = accent.primary.copy(alpha = 0.10f),
                contentColor = accent.primary
            )
        ) {
            Icon(Icons.Default.Check, null)
        }
    }
}


@Composable
private fun BoxScope.MultiStrokeInputButtons(state: State<CharacterWriterState?>) {
    val buttonState = remember {
        derivedStateOf {
            val writerState = state.value
            val contentState = writerState?.content?.value
                ?.let { it as? CharacterWriterContent.MultipleStrokeInput }
            writerState to contentState
        }
    }

    val accent = LocalKaiteyoAccent.current
    val multipleStrokeButtonsTransition = updateTransition(buttonState.value)
    multipleStrokeButtonsTransition.AnimatedContent(
        contentKey = { (_, contentState) -> contentState },
        transitionSpec = snapToBiggerContainerCrossfadeTransitionSpec(),
        modifier = Modifier.align(Alignment.BottomEnd)
    ) { (writerState, contentState) ->
        if (contentState !is CharacterWriterContent.MultipleStrokeInput.Writing)
            return@AnimatedContent

        IconButton(
            onClick = {
                writerState!!.submit(
                    CharacterInputData.MultipleStrokes(
                        characterStrokes = writerState.strokes,
                        inputStrokes = contentState.strokes.value
                    )
                )
            },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = accent.primary.copy(alpha = 0.10f),
                contentColor = accent.primary
            )
        ) {
            Icon(Icons.Default.Check, null)
        }
    }

    multipleStrokeButtonsTransition.AnimatedContent(
        contentKey = { (_, contentState) -> contentState },
        transitionSpec = snapToBiggerContainerCrossfadeTransitionSpec(),
        modifier = Modifier.align(Alignment.BottomStart)
    ) { (_, contentState) ->
        if (contentState !is CharacterWriterContent.MultipleStrokeInput.Writing)
            return@AnimatedContent

        IconButton(
            onClick = {
                contentState.strokes.value = contentState.strokes.value.dropLast(1)
            },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = accent.secondary.copy(alpha = 0.10f),
                contentColor = accent.secondary
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.Undo, null)
        }
    }
}

@Composable
private fun BoxScope.SummaryButtons(state: State<CharacterWriterState?>) {
    val buttonState = remember {
        derivedStateOf {
            val writerState = state.value
            val progress = writerState?.progress?.value
            writerState to progress
        }
    }

    val accent = LocalKaiteyoAccent.current
    val hintButtonTransition = updateTransition(targetState = buttonState.value)
    hintButtonTransition.AnimatedContent(
        transitionSpec = snapToBiggerContainerCrossfadeTransitionSpec(),
        modifier = Modifier.align(Alignment.TopEnd)
    ) { (writerState, progress) ->
        if (writerState == null || progress == null || progress == CharacterWritingProgress.Writing)
            return@AnimatedContent

        IconButton(
            onClick = { writerState.toggleAnimationState() },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = accent.primary.copy(alpha = 0.10f),
                contentColor = accent.primary
            )
        ) {
            val icon = when (progress) {
                is CharacterWritingProgress.Completed.Idle -> Icons.Default.PlayArrow
                is CharacterWritingProgress.Completed.Animating -> Icons.Default.Stop
                CharacterWritingProgress.Writing -> throw IllegalStateException()
            }
            Icon(icon, null)
        }
    }
}
