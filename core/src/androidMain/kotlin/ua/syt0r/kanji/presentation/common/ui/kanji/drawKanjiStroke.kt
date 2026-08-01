package ua.syt0r.kanji.presentation.common.ui.kanji

import android.graphics.Matrix
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.BrushSettings
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveAlpha
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeCap
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeJoin
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeWidth

/***
 * Drawing path without scale using android-specific matrix transformations
 * There are issues with hardware acceleration on older Android versions when drawing on scaled
 * canvas using compose API
 * Details: https://github.com/syt0r/Kanji-Dojo/issues/12
 */

actual fun DrawScope.drawKanjiStroke(
    path: Path,
    color: Color,
    width: Float,
    drawProgress: Float?,
    brushSettings: BrushSettings
) {

    val kanjiScale = size.maxDimension / KanjiSize

    val matrix = Matrix()
    matrix.setScale(kanjiScale, kanjiScale)

    val androidScaledPath = android.graphics.Path()
    path.asAndroidPath().apply { transform(matrix, androidScaledPath) }
    val scaledPath = androidScaledPath.asComposePath()

    val pathEffect = drawProgress?.let { progress ->
        val pathLength = PathMeasure().apply { setPath(scaledPath, false) }.length
        PathEffect.dashPathEffect(floatArrayOf(pathLength * progress, Float.MAX_VALUE))
    }

    drawPath(
        path = scaledPath,
        color = color,
        alpha = color.alpha * brushSettings.resolveAlpha(),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = brushSettings.resolveStrokeWidth(width) * kanjiScale,
            cap = brushSettings.resolveStrokeCap(),
            join = brushSettings.resolveStrokeJoin(),
            pathEffect = pathEffect
        )
    )
}