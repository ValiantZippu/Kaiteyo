package ua.syt0r.kanji.presentation.screen.main.screen.practice_common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import ua.syt0r.kanji.core.stroke_evaluator.StrokeClassification
import ua.syt0r.kanji.core.stroke_evaluator.StrokeEvaluation
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceEvaluation
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceIssueType
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.extraColorScheme
import ua.syt0r.kanji.presentation.common.ui.kanji.AnimatedStroke
import ua.syt0r.kanji.presentation.common.ui.kanji.Kanji
import ua.syt0r.kanji.presentation.common.ui.kanji.Stroke
import ua.syt0r.kanji.presentation.common.ui.kanji.StrokeInput
import ua.syt0r.kanji.presentation.common.ui.kanji.StrokeWidth
import ua.syt0r.kanji.presentation.common.ui.kanji.defaultStrokeColor
import ua.syt0r.kanji.presentation.common.ui.kanji.drawKanjiStroke
import ua.syt0r.kanji.presentation.common.ui.kanji.rememberStrokeInputState
import kotlin.math.max

@Composable
fun CharacterWriter(
    state: CharacterWriterState,
    modifier: Modifier = Modifier,
    brushSettings: BrushSettings = BrushSettings.default
) {

    Box(modifier) {
        when (val writerContent = state.content.value) {

            is CharacterWriterContent.SingleStrokeInput -> {

                SingleStrokeInputContent(
                    strokes = state.strokes,
                    inputState = writerContent,
                    onStrokeDrawn = { state.submit(it) },
                    brushSettings = brushSettings
                )

            }

            is CharacterWriterContent.MultipleStrokeInput -> {
                MultipleStrokeInputContent(
                    state = rememberUpdatedState(writerContent),
                    brushSettings = brushSettings
                )
            }

            is CharacterWriterContent.Animation -> {
                AnimatedCharacter(state.strokes) { state.toggleAnimationState() }
            }

        }
    }

}

@Composable
private fun BoxScope.MultipleStrokeInputContent(
    state: State<CharacterWriterContent.MultipleStrokeInput>,
    brushSettings: BrushSettings = BrushSettings.default
) {

    when (val currentState = state.value) {
        is CharacterWriterContent.MultipleStrokeInput.Writing -> {
            var strokes by currentState.strokes

            Kanji(
                strokes = strokes,
                modifier = Modifier.fillMaxSize(),
                brushSettings = brushSettings
            )

            StrokeInput(
                onUserPathDrawn = { path -> strokes = strokes.plus(path) },
                modifier = Modifier.fillMaxSize(),
                brushSettings = brushSettings
            )
        }

        is CharacterWriterContent.MultipleStrokeInput.Processing -> {
            Kanji(
                strokes = currentState.strokes,
                modifier = Modifier.fillMaxSize()
            )
        }

        is CharacterWriterContent.MultipleStrokeInput.Processed -> {

            val lerpProgress = remember {
                Animatable(if (currentState.completedAnimation.value) 1f else 0f)
            }

            LaunchedEffect(Unit) {
                lerpProgress.animateTo(1f)
                currentState.completedAnimation.value = true
            }

            currentState.strokeProcessingResults.forEach { strokeResult ->
                when (strokeResult) {
                    is StrokeProcessingResult.Correct -> {
                        AnimatedStroke(
                            fromPath = strokeResult.userPath,
                            toPath = strokeResult.kanjiPath,
                            progress = { lerpProgress.value },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is StrokeProcessingResult.Mistake -> {
                        Stroke(
                            path = strokeResult.hintStroke,
                            color = strokeResult.evaluation.feedbackStrokeColor(),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            SequenceIssueChips(
                sequenceEvaluation = currentState.sequenceEvaluation,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

    }

}

/** Whole-character issue feedback: wrong order / missing / extra strokes. */
@Composable
private fun SequenceIssueChips(
    sequenceEvaluation: StrokeSequenceEvaluation?,
    modifier: Modifier = Modifier
) {
    val issues = sequenceEvaluation?.issues ?: return
    val uniqueTypes = issues.map { it.type }.distinct()
    if (uniqueTypes.isEmpty()) return

    val accent = LocalKaiteyoAccent.current
    Column(
        modifier = modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        uniqueTypes.forEach { type ->
            val label = when (type) {
                StrokeSequenceIssueType.WrongOrder ->
                    resolveString { commonPractice.sequenceIssueWrongOrder }

                StrokeSequenceIssueType.MissingStroke ->
                    resolveString { commonPractice.sequenceIssueMissingStroke }

                StrokeSequenceIssueType.ExtraStroke ->
                    resolveString { commonPractice.sequenceIssueExtraStroke }
            }
            val count = issues.count { it.type == type }
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(accent.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (count > 1) "$label ×$count" else label,
                    color = accent.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/** Stroke color used for rejected strokes: accent secondary for near-misses, accent primary otherwise. */
@Composable
private fun StrokeEvaluation?.feedbackStrokeColor(): Color {
    val accent = LocalKaiteyoAccent.current
    return when (this?.classification) {
        StrokeClassification.AlmostCorrect -> accent.secondary
        else -> accent.primary
    }
}

@Composable
private fun BoxScope.SingleStrokeInputContent(
    strokes: List<Path>,
    inputState: CharacterWriterContent.SingleStrokeInput,
    onStrokeDrawn: (CharacterInputData.SingleStroke) -> Unit,
    brushSettings: BrushSettings = BrushSettings.default
) {

    val isAnimatingCorrectStroke = remember { mutableStateOf(false) }
    val correctStrokeAnimations = remember { Channel<StrokeProcessingResult.Correct>() }
    val strokeInputState = rememberStrokeInputState(keepLastDrawnStroke = true)

    val mistakeStrokeAnimations = remember { Channel<StrokeProcessingResult.Mistake>() }

    val adjustedDrawnStrokesCount = remember {
        derivedStateOf {
            max(
                a = 0,
                b = inputState.drawnStrokesCount.value - if (isAnimatingCorrectStroke.value) 1 else 0
            )
        }
    }

    Kanji(
        strokes = strokes.take(adjustedDrawnStrokesCount.value),
        modifier = Modifier.fillMaxSize()
    )

    when (inputState.isStudyMode) {
        true -> {
            StudyStroke(
                strokes = strokes,
                drawnStrokesCount = adjustedDrawnStrokesCount,
                hintClicksFlow = inputState.hintClicksSharedFlow
            )
        }

        false -> {
            HintStroke(
                strokes = strokes,
                inputState = inputState,
                hintClicksFlow = inputState.hintClicksSharedFlow
            )
        }
    }

    ErrorFadeOutStroke(
        mistakeFlow = remember { mistakeStrokeAnimations.consumeAsFlow() },
        onAnimationEnd = { }
    )

    CorrectMovingStroke(
        correctFlow = remember { correctStrokeAnimations.consumeAsFlow() },
        onAnimationEnd = { isAnimatingCorrectStroke.value = false }
    )

    StrokeFeedbackPill(
        feedbackFlow = inputState.inputProcessingResults
    )

    val shouldShowStrokeInput by remember {
        derivedStateOf { strokes.size > inputState.drawnStrokesCount.value }
    }

    LaunchedEffect(Unit) {
        inputState.inputProcessingResults.collect {
            strokeInputState.hideStroke()
            when (it) {
                is StrokeProcessingResult.Correct -> {
                    correctStrokeAnimations.trySend(it)
                    isAnimatingCorrectStroke.value = true
                }

                is StrokeProcessingResult.Mistake -> {
                    mistakeStrokeAnimations.trySend(it)
                }
            }
        }
    }

    if (shouldShowStrokeInput) {
        StrokeInput(
            onUserPathDrawn = { drawnPath ->
                onStrokeDrawn(
                    CharacterInputData.SingleStroke(
                        userPath = drawnPath,
                        kanjiPath = strokes[inputState.drawnStrokesCount.value]
                    )
                )

            },
            state = strokeInputState,
            modifier = Modifier.fillMaxSize(),
            brushSettings = brushSettings
        )
    }

}


@Composable
fun HintStroke(
    strokes: List<Path>,
    inputState: CharacterWriterContent.SingleStrokeInput,
    hintClicksFlow: Flow<Unit>
) {

    val currentState by rememberUpdatedState(inputState)

    val stroke = remember { mutableStateOf<Path?>(null, neverEqualPolicy()) }
    val strokeDrawProgress = remember { Animatable(initialValue = 0f) }
    val strokeAlpha = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(Unit) {

        hintClicksFlow.collectLatest {
            stroke.value = currentState.run {
                strokes.getOrNull(drawnStrokesCount.value)
            }

            strokeAlpha.snapTo(1f)
            strokeDrawProgress.snapTo(0f)

            strokeDrawProgress.animateTo(1f, tween(600))
            strokeAlpha.animateTo(0f)

            stroke.value = null
        }

    }

    val hintColor = LocalKaiteyoAccent.current.primary
    stroke.value?.let {
        AnimatedStroke(
            stroke = it,
            modifier = Modifier.fillMaxSize(),
            strokeColor = hintColor,
            drawProgress = { strokeDrawProgress.value },
            strokeAlpha = { strokeAlpha.value }
        )
    }

}

@Composable
fun ErrorFadeOutStroke(
    mistakeFlow: Flow<StrokeProcessingResult.Mistake>,
    onAnimationEnd: () -> Unit
) {

    val lastData = remember { mutableStateOf<StrokeProcessingResult.Mistake?>(null) }
    val strokeAlpha = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(Unit) {
        mistakeFlow.collect {
            lastData.value = it
            strokeAlpha.snapTo(1f)
            strokeAlpha.animateTo(0f, tween(600))
            onAnimationEnd()
        }
    }

    lastData.value?.let {
        AnimatedStroke(
            // Show the user's own rejected stroke when available — more actionable
            // feedback than re-drawing the reference.
            stroke = it.attemptStroke ?: it.hintStroke,
            modifier = Modifier.fillMaxSize(),
            strokeColor = it.evaluation.feedbackStrokeColor(),
            drawProgress = { 1f },
            strokeAlpha = { strokeAlpha.value }
        )
    }

}

/**
 * Real-time stroke feedback: a compact pill that appears after every stroke
 * evaluation with the classification and the scored accuracy (0..100).
 * Color + text + percent keep the feedback usable without relying on color alone.
 */
@Composable
private fun BoxScope.StrokeFeedbackPill(
    feedbackFlow: Flow<StrokeProcessingResult>,
    modifier: Modifier = Modifier
) {
    val feedback = remember { mutableStateOf<StrokeProcessingResult?>(null) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        feedbackFlow.collect {
            feedback.value = it
            alpha.snapTo(1f)
            delay(700)
            alpha.animateTo(0f, tween(300))
        }
    }

    val result = feedback.value ?: return
    val percent = result.evaluation?.metrics?.accuracyPercent()
    val accent = LocalKaiteyoAccent.current
    val (label, color) = when (result) {
        is StrokeProcessingResult.Correct -> {
            resolveString { commonPractice.writingStrokeCorrect } to
                MaterialTheme.extraColorScheme.success
        }

        is StrokeProcessingResult.Mistake -> {
            val almost = result.evaluation?.classification == StrokeClassification.AlmostCorrect
            if (almost) {
                resolveString { commonPractice.writingStrokeAlmost } to
                    accent.secondary
            } else {
                resolveString { commonPractice.writingStrokeIncorrect } to
                    accent.primary
            }
        }
    }

    Box(
        modifier = modifier
            .align(Alignment.TopCenter)
            .padding(top = 10.dp)
            .graphicsLayer { this.alpha = alpha.value }
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(1.5.dp, color, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (percent != null) "$label · $percent%" else label,
            color = color,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun CorrectMovingStroke(
    correctFlow: Flow<StrokeProcessingResult.Correct>,
    onAnimationEnd: () -> Unit
) {

    val lastData = remember { mutableStateOf<StrokeProcessingResult.Correct?>(null) }
    val strokeLength = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(Unit) {
        correctFlow.collect {
            lastData.value = it
            strokeLength.snapTo(0f)
            strokeLength.animateTo(1f)
            lastData.value = null
            onAnimationEnd()
        }
    }

    lastData.value?.let {
        AnimatedStroke(
            fromPath = it.userPath,
            toPath = it.kanjiPath,
            progress = { strokeLength.value },
            modifier = Modifier.fillMaxSize()
        )
    }

}

@Composable
private fun StudyStroke(
    strokes: List<Path>,
    drawnStrokesCount: State<Int>,
    hintClicksFlow: Flow<Unit>
) {

    val stroke = remember { mutableStateOf<Path?>(null) }
    val strokeDrawProgress = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(Unit) {
        val autoStartFlow = merge(flowOf(Unit), hintClicksFlow)
        snapshotFlow { drawnStrokesCount.value }
            .combine(autoStartFlow) { a, b -> a }
            .collectLatest { drawnStrokesCount ->
                // Waits for stroke animation to complete
                stroke.value = strokes.getOrNull(drawnStrokesCount)
                strokeDrawProgress.snapTo(0f)
                if (drawnStrokesCount == 0) delay(300)
                strokeDrawProgress.animateTo(1f, tween(600))
            }

    }

    stroke.value?.let {
        AnimatedStroke(
            stroke = it,
            modifier = Modifier.fillMaxSize(),
            strokeColor = defaultStrokeColor(),
            drawProgress = { strokeDrawProgress.value },
            strokeAlpha = { 0.5f }
        )
    }

}


@Composable
private fun AnimatedCharacter(
    strokes: List<Path>,
    onAnimationCompleted: () -> Unit
) {

    val strokesToDraw = remember { mutableStateOf(strokes) }
    val lastStrokeAnimationProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        for (strokesCount in 1..strokes.size) {
            val paths = strokes.subList(0, strokesCount)
            strokesToDraw.value = paths

            lastStrokeAnimationProgress.snapTo(0f)
            lastStrokeAnimationProgress.animateTo(1f, tween(600))
        }
        onAnimationCompleted()
    }

    val strokeColor = defaultStrokeColor()

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        clipRect {

            val strokesList = strokesToDraw.value

            strokesList.dropLast(1).forEach {
                drawKanjiStroke(
                    path = it,
                    color = strokeColor,
                    width = StrokeWidth,
                    brushSettings = BrushSettings.default
                )
            }

            strokesList.lastOrNull()?.also {
                drawKanjiStroke(
                    path = it,
                    color = strokeColor,
                    width = StrokeWidth,
                    drawProgress = lastStrokeAnimationProgress.value,
                    brushSettings = BrushSettings.default
                )
            }

        }
    }

}
