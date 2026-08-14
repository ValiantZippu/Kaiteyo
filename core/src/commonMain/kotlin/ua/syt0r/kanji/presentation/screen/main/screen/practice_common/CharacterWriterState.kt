package ua.syt0r.kanji.presentation.screen.main.screen.practice_common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.core.launchUnit
import ua.syt0r.kanji.core.stroke_evaluator.KanjiStrokeEvaluator
import ua.syt0r.kanji.core.stroke_evaluator.StrokeClassification
import ua.syt0r.kanji.core.stroke_evaluator.StrokeEvaluation
import ua.syt0r.kanji.core.stroke_evaluator.StrokeEvaluationConfig
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceEvaluation
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceEvaluator
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceIssueType
import kotlin.math.max


interface CharacterWriterState {

    val character: String
    val strokes: List<Path>
    val configuration: CharacterWriterConfiguration
    val content: State<CharacterWriterContent>
    val progress: State<CharacterWritingProgress>

    /** Scored statistics of the current (or last completed) writing attempt. */
    val attemptStats: State<WritingAttemptStats>

    fun submit(inputData: CharacterInputData)
    fun toggleAnimationState()

}

sealed interface CharacterWriterConfiguration {

    data class StrokeInput(
        val isStudyMode: Boolean,
        /** Tolerance/strictness model used when scoring each drawn stroke. */
        val evaluationConfig: StrokeEvaluationConfig = StrokeEvaluationConfig.Normal
    ) : CharacterWriterConfiguration

    /** Whole-character input: the character is drawn freely, then evaluated as a sequence. */
    data class CharacterInput(
        val evaluationConfig: StrokeEvaluationConfig = StrokeEvaluationConfig.Normal
    ) : CharacterWriterConfiguration

}

sealed interface CharacterInputData {

    data class MultipleStrokes(
        val characterStrokes: List<Path>,
        val inputStrokes: List<Path>
    ) : CharacterInputData

    data class SingleStroke(
        val userPath: Path,
        val kanjiPath: Path
    ) : CharacterInputData
}

sealed interface StrokeProcessingResult {

    /** Scored evaluation of the attempted stroke, when available. */
    val evaluation: StrokeEvaluation?

    data class Correct(
        val userPath: Path,
        val kanjiPath: Path,
        override val evaluation: StrokeEvaluation
    ) : StrokeProcessingResult

    data class Mistake(
        val hintStroke: Path,
        /** The drawn stroke that was rejected (null for missing/extra in character input). */
        val attemptStroke: Path? = null,
        override val evaluation: StrokeEvaluation? = null
    ) : StrokeProcessingResult

}

sealed interface CharacterWriterContent {

    interface SingleStrokeInput : CharacterWriterContent {

        val isStudyMode: Boolean
        val drawnStrokesCount: State<Int>
        val currentStrokeMistakes: State<Int>
        val totalMistakes: State<Int>
        val hintClicksSharedFlow: SharedFlow<Unit>
        val inputProcessingResults: SharedFlow<StrokeProcessingResult>
        /** Last evaluation per stroke index (null while a stroke has not been scored). */
        val strokeEvaluations: State<List<StrokeEvaluation?>>

        suspend fun notifyHintClick()
        fun skipRemainingStrokes()

    }

    sealed interface MultipleStrokeInput : CharacterWriterContent {

        data class Writing(
            val strokes: MutableState<List<Path>>
        ) : MultipleStrokeInput

        data class Processing(
            val strokes: List<Path>
        ) : MultipleStrokeInput

        data class Processed(
            val strokeProcessingResults: List<StrokeProcessingResult>,
            val mistakes: Int,
            val completedAnimation: MutableState<Boolean>,
            /** Whole-character analysis (wrong order / missing / extra strokes). */
            val sequenceEvaluation: StrokeSequenceEvaluation? = null
        ) : MultipleStrokeInput

    }

    data class Animation(
        val previousState: CharacterWriterContent
    ) : CharacterWriterContent

}

private data class MutableSingleStrokeInputWriterContent(
    override val isStudyMode: Boolean,
    private val totalStrokesCount: Int,
    override val drawnStrokesCount: MutableState<Int>,
    override val currentStrokeMistakes: MutableState<Int>,
    override val totalMistakes: MutableState<Int>,
    override val hintClicksSharedFlow: MutableSharedFlow<Unit>,
    override val inputProcessingResults: MutableSharedFlow<StrokeProcessingResult>,
    override val strokeEvaluations: MutableState<List<StrokeEvaluation?>>
) : CharacterWriterContent.SingleStrokeInput {

    override suspend fun notifyHintClick() {
        currentStrokeMistakes.value += 1
        totalMistakes.value += 1
        hintClicksSharedFlow.emit(Unit)
    }

    override fun skipRemainingStrokes() {
        totalMistakes.value += (totalStrokesCount - drawnStrokesCount.value)
        drawnStrokesCount.value = totalStrokesCount
    }

}

sealed interface CharacterWritingProgress {

    data object Writing : CharacterWritingProgress

    sealed interface Completed : CharacterWritingProgress {
        val isCorrect: Boolean
        val mistakes: Int

        data class Idle(
            override val isCorrect: Boolean,
            override val mistakes: Int
        ) : Completed

        data class Animating(
            override val isCorrect: Boolean,
            override val mistakes: Int
        ) : Completed
    }

}

class DefaultCharacterWriterState(
    private val coroutineScope: CoroutineScope,
    private val strokeEvaluator: KanjiStrokeEvaluator,
    override val character: String,
    override val strokes: List<Path>,
    override val configuration: CharacterWriterConfiguration
) : CharacterWriterState {

    private val _content = mutableStateOf(createInputState())
    override val content: State<CharacterWriterContent> = _content

    override val progress: State<CharacterWritingProgress> = derivedStateOf {
        content.value.toWritingProgress()
    }

    override val attemptStats: State<WritingAttemptStats> = derivedStateOf {
        content.value.toAttemptStats(strokes.size)
    }

    override fun submit(inputData: CharacterInputData) = coroutineScope.launchUnit {
        when (inputData) {
            is CharacterInputData.SingleStroke -> {
                handleSingleStrokeInput(inputData)
            }

            is CharacterInputData.MultipleStrokes -> {
                handleMultipleStrokeInput(inputData)
            }
        }
    }

    override fun toggleAnimationState() {
        val currentContent = content.value
        _content.value = when (currentContent) {
            is CharacterWriterContent.Animation -> currentContent.previousState
            else -> CharacterWriterContent.Animation(currentContent)
        }
    }

    private fun strokeEvaluationConfig(): StrokeEvaluationConfig {
        return when (configuration) {
            is CharacterWriterConfiguration.StrokeInput -> configuration.evaluationConfig
            is CharacterWriterConfiguration.CharacterInput -> configuration.evaluationConfig
        }
    }

    private fun createInputState(): CharacterWriterContent {
        return when (configuration) {
            is CharacterWriterConfiguration.StrokeInput -> {
                MutableSingleStrokeInputWriterContent(
                    isStudyMode = configuration.isStudyMode,
                    totalStrokesCount = strokes.size,
                    drawnStrokesCount = mutableStateOf(0),
                    currentStrokeMistakes = mutableStateOf(0),
                    totalMistakes = mutableStateOf(0),
                    hintClicksSharedFlow = MutableSharedFlow(),
                    inputProcessingResults = MutableSharedFlow(),
                    strokeEvaluations = mutableStateOf(List(strokes.size) { null })
                )
            }

            is CharacterWriterConfiguration.CharacterInput -> {
                CharacterWriterContent.MultipleStrokeInput.Writing(
                    strokes = mutableStateOf(emptyList())
                )
            }
        }
    }

    private suspend fun handleSingleStrokeInput(
        inputData: CharacterInputData.SingleStroke,
    ) {
        val mutableState = content.value as MutableSingleStrokeInputWriterContent

        val evaluation = withContext(Dispatchers.IO) {
            strokeEvaluator.evaluate(
                expected = inputData.kanjiPath,
                drawn = inputData.userPath,
                config = strokeEvaluationConfig()
            )
        }
        // Record the outcome for the stroke index being attempted (last attempt wins).
        val strokeIndex = mutableState.drawnStrokesCount.value
        mutableState.strokeEvaluations.value = mutableState.strokeEvaluations.value
            .toMutableList()
            .also { if (strokeIndex < it.size) it[strokeIndex] = evaluation }

        val result = if (evaluation.isAcceptable) {
            mutableState.drawnStrokesCount.value += 1
            StrokeProcessingResult.Correct(
                userPath = inputData.userPath,
                kanjiPath = inputData.kanjiPath,
                evaluation = evaluation
            )
        } else {
            val currentStrokeMistakes = mutableState.run {
                currentStrokeMistakes.value += 1
                totalMistakes.value += 1
                currentStrokeMistakes.value
            }
            val path = when {
                currentStrokeMistakes > 2 -> inputData.kanjiPath
                else -> inputData.userPath
            }
            StrokeProcessingResult.Mistake(
                hintStroke = path,
                attemptStroke = inputData.userPath,
                evaluation = evaluation
            )
        }
        mutableState.inputProcessingResults.emit(result)
    }

    private suspend fun handleMultipleStrokeInput(
        inputData: CharacterInputData.MultipleStrokes
    ) {
        _content.value = CharacterWriterContent.MultipleStrokeInput.Processing(
            strokes = inputData.inputStrokes
        )

        val processedState = withContext(Dispatchers.IO) {
            val evaluationConfig = strokeEvaluationConfig()

            // Whole-character analysis: correct matching of strokes regardless
            // of the order they were drawn, with order/missing/extra detection.
            val sequenceEvaluation = StrokeSequenceEvaluator(strokeEvaluator).evaluate(
                expectedStrokes = inputData.characterStrokes,
                drawnStrokes = inputData.inputStrokes,
                config = evaluationConfig
            )

            val strokesCount = max(inputData.characterStrokes.size, inputData.inputStrokes.size)
            val results = (0 until strokesCount).map { index ->
                val input = inputData.inputStrokes.getOrNull(index)
                val stroke = inputData.characterStrokes.getOrNull(index)

                if (input == null || stroke == null) {
                    StrokeProcessingResult.Mistake(
                        hintStroke = input ?: stroke!!
                    )
                } else {
                    val evaluation = strokeEvaluator.evaluate(stroke, input, evaluationConfig)
                    if (evaluation.isAcceptable) {
                        StrokeProcessingResult.Correct(input, stroke, evaluation)
                    } else {
                        StrokeProcessingResult.Mistake(
                            hintStroke = stroke,
                            attemptStroke = input,
                            evaluation = evaluation
                        )
                    }
                }
            }

            CharacterWriterContent.MultipleStrokeInput.Processed(
                strokeProcessingResults = results.take(inputData.characterStrokes.size),
                mistakes = results.count { it is StrokeProcessingResult.Mistake },
                completedAnimation = mutableStateOf(false),
                sequenceEvaluation = sequenceEvaluation
            )
        }

        _content.value = processedState
    }

    private fun CharacterWriterContent.toWritingProgress(): CharacterWritingProgress {
        return when (this) {
            is CharacterWriterContent.SingleStrokeInput -> {
                when (drawnStrokesCount.value == strokes.size) {
                    false -> CharacterWritingProgress.Writing
                    true -> {
                        val mistakes = totalMistakes.value
                        CharacterWritingProgress.Completed.Idle(
                            isCorrect = isResultCorrect(mistakes),
                            mistakes = mistakes
                        )
                    }
                }
            }

            is CharacterWriterContent.MultipleStrokeInput.Writing,
            is CharacterWriterContent.MultipleStrokeInput.Processing -> {
                CharacterWritingProgress.Writing
            }

            is CharacterWriterContent.MultipleStrokeInput.Processed -> {
                CharacterWritingProgress.Completed.Idle(
                    isCorrect = isResultCorrect(mistakes),
                    mistakes = mistakes
                )
            }

            is CharacterWriterContent.Animation -> {
                val completedProgress = previousState.toWritingProgress()
                completedProgress as CharacterWritingProgress.Completed
                CharacterWritingProgress.Completed.Animating(
                    isCorrect = completedProgress.isCorrect,
                    mistakes = completedProgress.mistakes
                )
            }
        }
    }

    private fun isResultCorrect(characterMistakes: Int): Boolean {
        return when (strokes.size) {
            1 -> characterMistakes == 0
            2, 3 -> characterMistakes < 2
            else -> characterMistakes <= 2
        }
    }

}

/** Whole-character attempt statistics derived from the current writer content. */
private fun CharacterWriterContent.toAttemptStats(strokesCount: Int): WritingAttemptStats = when (this) {
    is CharacterWriterContent.SingleStrokeInput -> {
        val evaluations = strokeEvaluations.value
        val anyScored = evaluations.any { it != null }
        WritingAttemptStats(
            strokeCount = strokesCount,
            mistakes = totalMistakes.value,
            wrongOrderCount = 0,
            almostCount = evaluations.count {
                it?.classification == StrokeClassification.AlmostCorrect
            },
            // Skipped strokes (never scored, e.g. hint-revealed or skipped) count as 0
            // once any stroke was scored, keeping the accuracy honest.
            strokeAccuracy = if (anyScored) {
                evaluations.sumOf { it?.metrics?.overallScore?.toDouble() ?: 0.0 }.toFloat() /
                    evaluations.size
            } else {
                null
            }
        )
    }

    is CharacterWriterContent.MultipleStrokeInput.Writing,
    is CharacterWriterContent.MultipleStrokeInput.Processing -> {
        WritingAttemptStats(strokeCount = strokesCount)
    }

    is CharacterWriterContent.MultipleStrokeInput.Processed -> {
        WritingAttemptStats(
            strokeCount = strokesCount,
            mistakes = mistakes,
            wrongOrderCount = sequenceEvaluation?.issues
                ?.count { it.type == StrokeSequenceIssueType.WrongOrder }
                ?: 0,
            almostCount = strokeProcessingResults.count { result ->
                (result as? StrokeProcessingResult.Correct)
                    ?.evaluation
                    ?.classification == StrokeClassification.AlmostCorrect
            },
            strokeAccuracy = sequenceEvaluation?.overallAccuracy
        )
    }

    is CharacterWriterContent.Animation -> previousState.toAttemptStats(strokesCount)
}
