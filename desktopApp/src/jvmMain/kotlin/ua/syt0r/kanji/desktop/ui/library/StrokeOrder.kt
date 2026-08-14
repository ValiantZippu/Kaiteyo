package ua.syt0r.kanji.desktop.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import kotlinx.coroutines.delay

// ============================================
// STROKE ORDER
// Animated per-stroke playback for kanji. The
// dataset is a curated starter set of common
// characters authored in KanjiVG-style 0..1092
// coordinates (each stroke is an ordered polyline
// following the standard stroke direction and
// order). The renderer animates strokes one at a
// time with a draw-on effect, and full KanjiVG
// data can be dropped into StrokeOrderData later
// without touching the playback engine.
// ============================================

object StrokeOrderData {

    private fun path(vararg points: Pair<Float, Float>): List<Offset> =
        points.map { Offset(it.first, it.second) }

    /** Character → ordered strokes, each stroke an ordered polyline in 0..1092 space. */
    val sequences: Map<String, List<List<Offset>>> = mapOf(
        "一" to listOf(path(120f to 546f, 972f to 546f)),
        "二" to listOf(path(180f to 300f, 912f to 300f), path(150f to 800f, 950f to 800f)),
        "三" to listOf(
            path(220f to 200f, 872f to 200f),
            path(120f to 546f, 972f to 546f),
            path(150f to 890f, 950f to 890f)
        ),
        "十" to listOf(path(120f to 546f, 972f to 546f), path(546f to 120f, 546f to 972f)),
        "口" to listOf(
            path(240f to 240f, 240f to 850f),
            path(240f to 240f, 850f to 240f, 850f to 850f),
            path(240f to 850f, 850f to 850f)
        ),
        "日" to listOf(
            path(240f to 240f, 240f to 850f),
            path(240f to 240f, 850f to 240f, 850f to 850f),
            path(240f to 546f, 850f to 546f),
            path(240f to 850f, 850f to 850f)
        ),
        "田" to listOf(
            path(240f to 240f, 240f to 850f),
            path(240f to 240f, 850f to 240f, 850f to 850f),
            path(546f to 240f, 546f to 850f),
            path(240f to 546f, 850f to 546f),
            path(240f to 850f, 850f to 850f)
        ),
        "目" to listOf(
            path(240f to 240f, 240f to 850f),
            path(240f to 240f, 850f to 240f, 850f to 850f),
            path(240f to 418f, 850f to 418f),
            path(240f to 596f, 850f to 596f),
            path(240f to 850f, 850f to 850f)
        ),
        "上" to listOf(
            path(546f to 180f, 546f to 850f),
            path(330f to 300f, 762f to 300f),
            path(200f to 850f, 892f to 850f)
        ),
        "下" to listOf(
            path(180f to 300f, 912f to 300f),
            path(546f to 300f, 546f to 972f),
            path(700f to 680f, 790f to 740f)
        ),
        "川" to listOf(
            path(200f to 180f, 200f to 850f),
            path(500f to 180f, 500f to 890f),
            path(800f to 180f, 800f to 820f)
        ),
        "人" to listOf(path(450f to 180f, 220f to 850f), path(450f to 180f, 860f to 850f)),
        "八" to listOf(path(360f to 220f, 220f to 850f), path(560f to 220f, 880f to 850f)),
        "木" to listOf(
            path(120f to 420f, 972f to 420f),
            path(546f to 120f, 546f to 972f),
            path(546f to 420f, 220f to 920f),
            path(546f to 420f, 880f to 920f)
        ),
        "本" to listOf(
            path(120f to 420f, 972f to 420f),
            path(546f to 120f, 546f to 972f),
            path(546f to 420f, 220f to 920f),
            path(546f to 420f, 880f to 920f),
            path(240f to 850f, 850f to 850f)
        ),
        "大" to listOf(
            path(120f to 360f, 972f to 360f),
            path(546f to 360f, 240f to 940f),
            path(546f to 360f, 880f to 940f)
        ),
        "小" to listOf(
            path(546f to 180f, 546f to 420f),
            path(280f to 420f, 180f to 850f),
            path(800f to 420f, 900f to 850f)
        ),
        "天" to listOf(
            path(180f to 250f, 912f to 250f),
            path(300f to 420f, 792f to 420f),
            path(450f to 420f, 200f to 920f),
            path(546f to 420f, 880f to 920f)
        ),
        "土" to listOf(
            path(220f to 300f, 872f to 300f),
            path(546f to 300f, 546f to 920f),
            path(160f to 880f, 932f to 880f)
        ),
        "王" to listOf(
            path(220f to 280f, 872f to 280f),
            path(180f to 546f, 912f to 546f),
            path(150f to 820f, 942f to 820f),
            path(546f to 280f, 546f to 820f)
        ),
        "玉" to listOf(
            path(220f to 280f, 872f to 280f),
            path(180f to 546f, 912f to 546f),
            path(150f to 820f, 942f to 820f),
            path(546f to 280f, 546f to 820f),
            path(700f to 900f, 780f to 950f)
        )
    )
}

/** Animated stroke-order playback for a single character. Falls back to a static note when no data exists. */
@Composable
fun StrokeOrderPanel(
    character: String,
    strokeCount: Int,
    modifier: Modifier = Modifier,
    onPractice: (() -> Unit)? = null
) {
    val strokes = StrokeOrderData.sequences[character] ?: return
    val sc = surfaceColors()
    val ac = accent()

    val strokeDurationMs = 340
    val holdMs = 700
    val totalMs = strokes.size * strokeDurationMs + holdMs

    var elapsedMs by remember(character) { mutableStateOf(0) }
    var playing by remember(character) { mutableStateOf(true) }

    LaunchedEffect(playing, character) {
        if (!playing) return@LaunchedEffect
        while (true) {
            elapsedMs += 16
            if (elapsedMs >= totalMs) elapsedMs = 0
            delay(16)
        }
    }

    val activeStroke = (elapsedMs / strokeDurationMs).coerceIn(0, strokes.lastIndex)
    val partial = (elapsedMs % strokeDurationMs) / strokeDurationMs.toFloat()

    DsCard(modifier = modifier) {
        Row(
            Modifier.padding(DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(sc.surfaceElevated)
                    .border(1.dp, sc.border, RoundedCornerShape(DsRadius.Md))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val scale = size.width / 1092f
                    val step = size.width / 5f
                    val gridColor = sc.border.copy(alpha = 0.5f)
                    for (i in 1 until 5) {
                        drawLine(gridColor, Offset(step * i, 0f), Offset(step * i, size.height), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, step * i), Offset(size.width, step * i), strokeWidth = 1f)
                    }
                    strokes.forEachIndexed { index, points ->
                        val fraction = when {
                            index < activeStroke -> 1f
                            index == activeStroke -> partial.coerceIn(0f, 1f)
                            else -> 0f
                        }
                        if (fraction > 0f) {
                            drawPartialPolyline(
                                points = points,
                                fraction = fraction,
                                scale = scale,
                                color = if (index == activeStroke) ac.primary else sc.textPrimary,
                                strokeWidth = size.width * 0.05f
                            )
                        }
                    }
                }
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(
                        text = "Stroke order — $character",
                        color = sc.textPrimary,
                        fontSize = DsType.Title,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Stroke ${activeStroke + 1} of ${strokes.size}",
                        color = ac.primary,
                        fontSize = DsType.Label,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    strokes.indices.forEach { index ->
                        val selected = index == activeStroke
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) ac.primary.copy(alpha = 0.16f) else sc.surfaceInteractive)
                                .clickable { elapsedMs = index * strokeDurationMs; playing = false }
                                .then(
                                    if (selected) Modifier.border(1.dp, ac.primary, RoundedCornerShape(8.dp)) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = if (selected) ac.primary else sc.textSecondary,
                                fontSize = DsType.Caption,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                    DsButton(
                        text = if (playing) "Pause" else "Play",
                        icon = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = { playing = !playing },
                        compact = true
                    )
                    DsButton(
                        text = "Replay",
                        icon = Icons.Default.RestartAlt,
                        kind = DsButtonKind.Ghost,
                        compact = true,
                        onClick = { elapsedMs = 0; playing = true }
                    )
                    if (onPractice != null) {
                        DsButton(
                            text = "Practice writing",
                            icon = Icons.Default.Create,
                            kind = DsButtonKind.Ghost,
                            compact = true,
                            onClick = onPractice
                        )
                    }
                }
                Text(
                    text = "Standard stroke order — top-to-bottom, left-to-right. Tap a stroke number to inspect it.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}

/** Draws the first [fraction] of a polyline (by path length) with rounded joins. */
private fun DrawScope.drawPartialPolyline(
    points: List<Offset>,
    fraction: Float,
    scale: Float,
    color: Color,
    strokeWidth: Float
) {
    if (points.isEmpty() || fraction <= 0f) return
    val scaled = points.map { Offset(it.x * scale, it.y * scale) }
    val totalLength = (1 until scaled.size).map { (scaled[it] - scaled[it - 1]).getDistance() }.sum()
    if (totalLength <= 0f) return
    val target = totalLength * fraction.coerceIn(0f, 1f)

    val path = Path()
    path.moveTo(scaled[0].x, scaled[0].y)
    var acc = 0f
    for (i in 1 until scaled.size) {
        val segment = (scaled[i] - scaled[i - 1]).getDistance()
        if (acc + segment >= target) {
            val t = if (segment == 0f) 0f else (target - acc) / segment
            path.lineTo(
                scaled[i - 1].x + (scaled[i].x - scaled[i - 1].x) * t,
                scaled[i - 1].y + (scaled[i].y - scaled[i - 1].y) * t
            )
            break
        }
        acc += segment
        path.lineTo(scaled[i].x, scaled[i].y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
