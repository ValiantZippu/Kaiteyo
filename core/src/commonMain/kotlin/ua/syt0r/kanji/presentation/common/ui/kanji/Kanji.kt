package ua.syt0r.kanji.presentation.common.ui.kanji

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import ua.syt0r.kanji.core.lerpTo
import ua.syt0r.kanji.core.svg.SvgCommandParser
import ua.syt0r.kanji.core.svg.SvgPathCreator
import ua.syt0r.kanji.presentation.common.ExcludeNavigationGesturesModifier
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.BrushSettings
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveAlpha
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeCap
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeJoin
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeWidth

const val KanjiSize = 109
const val StrokeWidth = 3f

@Composable
fun defaultStrokeColor(): Color {
    return MaterialTheme.colorScheme.onSurface
}

@Composable
fun Kanji(
    strokes: List<Path>,
    modifier: Modifier = Modifier,
    strokeColor: Color = defaultStrokeColor(),
    stokeWidth: Float = StrokeWidth,
    brushSettings: BrushSettings = BrushSettings.default
) {
    Canvas(modifier) {
        strokes.forEach { drawKanjiStroke(it, strokeColor, stokeWidth, brushSettings = brushSettings) }
    }
}

@Composable
fun Kanji(
    strokes: State<List<Path>>,
    modifier: Modifier = Modifier,
    strokeColor: Color = defaultStrokeColor(),
    stokeWidth: Float = StrokeWidth,
    brushSettings: BrushSettings = BrushSettings.default
) {
    Canvas(modifier) {
        strokes.value.forEach { drawKanjiStroke(it, strokeColor, stokeWidth, brushSettings = brushSettings) }
    }
}

@Composable
fun Stroke(
    path: Path,
    modifier: Modifier = Modifier,
    color: Color = defaultStrokeColor(),
    stokeWidth: Float = StrokeWidth,
    brushSettings: BrushSettings = BrushSettings.default
) {
    Canvas(modifier) {
        clipRect { drawKanjiStroke(path, color, stokeWidth, brushSettings = brushSettings) }
    }
}

class StrokeInputState(
    val keepLastDrawnStroke: Boolean
) {

    val internalShowStroke = mutableStateOf(keepLastDrawnStroke)
    val internalPath = mutableStateOf(Path(), neverEqualPolicy())
    val internalDrawAreaSize = mutableStateOf(0)

    fun hideStroke() {
        internalShowStroke.value = false
    }

}

@Composable
fun rememberStrokeInputState(
    keepLastDrawnStroke: Boolean = false
): StrokeInputState {
    return remember { StrokeInputState(keepLastDrawnStroke) }
}

@Composable
fun StrokeInput(
    onUserPathDrawn: (Path) -> Unit,
    state: StrokeInputState = rememberStrokeInputState(),
    modifier: Modifier = Modifier,
    color: Color = defaultStrokeColor(),
    stokeWidth: Float = StrokeWidth,
    brushSettings: BrushSettings = BrushSettings.default
) {

    Canvas(
        modifier = modifier
            .then(ExcludeNavigationGesturesModifier)
            .onGloballyPositioned { state.internalDrawAreaSize.value = it.size.height }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        val areaSize = state.internalDrawAreaSize.value
                        state.internalPath.value = Path().apply {
                            moveTo(
                                it.x / areaSize * KanjiSize,
                                it.y / areaSize * KanjiSize
                            )
                        }
                        state.internalShowStroke.value = true
                    },
                    onDrag = { _, dragAmount ->
                        val areaSize = state.internalDrawAreaSize.value
                        state.internalPath.value = state.internalPath.value.apply {
                            relativeLineTo(
                                dragAmount.x / areaSize * KanjiSize,
                                dragAmount.y / areaSize * KanjiSize
                            )
                        }
                    },
                    onDragEnd = {
                        onUserPathDrawn(state.internalPath.value)
                        if (!state.keepLastDrawnStroke) {
                            state.internalShowStroke.value = false
                        }
                    }
                )
            }
    ) {
        if (state.internalShowStroke.value) {
            clipRect {
                drawKanjiStroke(
                    path = state.internalPath.value,
                    color = color,
                    width = stokeWidth,
                    brushSettings = brushSettings
                )
            }
        }
    }

}

@Composable
fun AnimatedStroke(
    fromPath: Path,
    toPath: Path,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    strokeColor: Color = defaultStrokeColor(),
    stokeWidth: Float = StrokeWidth,
    brushSettings: BrushSettings = BrushSettings.default
) {
    Canvas(modifier) {
        val path = fromPath.lerpTo(toPath, progress())
        drawKanjiStroke(path, strokeColor, stokeWidth, brushSettings = brushSettings)
    }
}

expect fun DrawScope.drawKanjiStroke(
    path: Path,
    color: Color,
    width: Float,
    drawProgress: Float? = null,
    brushSettings: BrushSettings = BrushSettings.default
)

fun parseKanjiStrokes(strokes: List<String>): List<Path> {
    return strokes.map { SvgCommandParser.parse(it) }
        .map { SvgPathCreator.convert(it) }
}
