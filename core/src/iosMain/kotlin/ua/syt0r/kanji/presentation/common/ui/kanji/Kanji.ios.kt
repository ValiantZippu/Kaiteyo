package ua.syt0r.kanji.presentation.common.ui.kanji

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.BrushSettings
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveAlpha
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeCap
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeJoin
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeWidth

actual fun DrawScope.drawKanjiStroke(
    path: Path,
    color: Color,
    width: Float,
    drawProgress: Float?,
    brushSettings: BrushSettings
) {

    val scale = size.maxDimension / KanjiSize
    scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero) {
        drawPath(
            path = path,
            color = color,
            alpha = color.alpha * brushSettings.resolveAlpha(),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = brushSettings.resolveStrokeWidth(width),
                cap = brushSettings.resolveStrokeCap(),
                join = brushSettings.resolveStrokeJoin(),
                pathEffect = drawProgress?.let {
                    val pathLength = PathMeasure().apply { setPath(path, false) }.length
                    PathEffect.dashPathEffect(floatArrayOf(pathLength * it, Float.MAX_VALUE))
                }
            )
        )
    }

}