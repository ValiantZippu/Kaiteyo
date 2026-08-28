package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiBackground
import ua.syt0r.kanji.presentation.common.ui.kanji.StrokeWidth

/**
 * Stroke order card with smooth animation, scrubbing, and stroke number popups.
 */
@Composable
fun KaiteyoStrokeOrderCard(
    strokes: List<Path>,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val coroutineScope = rememberCoroutineScope()

    val totalStrokes = strokes.size.coerceAtLeast(1)

    // Core state
    var isPlaying by remember { mutableStateOf(false) }
    var currentStrokeIndex by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    val strokeProgress = remember { Animatable(0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // For computing a smooth playback head (0..1) that maps all strokes
    val progressFraction: Float =
        if (totalStrokes == 0) 0f
        else (currentStrokeIndex + strokeProgress.value) / totalStrokes

    // --- auto-play loop ---
    LaunchedEffect(isPlaying, totalStrokes) {
        if (!isPlaying) return@LaunchedEffect
        for (s in currentStrokeIndex until totalStrokes) {
            if (!isPlaying) return@LaunchedEffect
            currentStrokeIndex = s
            strokeProgress.snapTo(0f)
            val durationMs = (400f / playbackSpeed).toInt().coerceIn(80, 2000)
            strokeProgress.animateTo(
                1f,
                tween(durationMs, easing = LinearEasing)
            )
            delay(80)
        }
        isPlaying = false
        currentStrokeIndex = totalStrokes
        strokeProgress.snapTo(1f)
    }

    // --- reset when strokes change ---
    LaunchedEffect(strokes) {
        isPlaying = false
        currentStrokeIndex = 0
        strokeProgress.snapTo(0f)
    }

    KaiteyoCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        // Header
        Text(
            text = "Stroke Order",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Stroke canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.15f))
                .onGloballyPositioned { canvasSize = it.size },
            contentAlignment = Alignment.Center
        ) {
            KanjiBackground(Modifier.fillMaxSize())

            // Draw completed strokes + current animating stroke
            Canvas(Modifier.fillMaxSize()) {
                val drawStrokeWidth = StrokeWidth * 1.1f
                val style = Stroke(
                    width = drawStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val measure = PathMeasure()

                // Completed strokes (dimmed)
                strokes.take(currentStrokeIndex).forEach { stroke ->
                    drawPath(stroke, accent.primary.copy(alpha = 0.45f), style = style)
                }

                // Currently animating stroke
                if (currentStrokeIndex < totalStrokes) {
                    val stroke = strokes[currentStrokeIndex]
                    val progress = strokeProgress.value.coerceIn(0f, 1f)
                    if (progress > 0f) {
                        measure.setPath(stroke, false)
                        val len = measure.length * progress
                        val partial = Path()
                        measure.getSegment(0f, len, partial, startWithMoveTo = true)
                        drawPath(partial, accent.primary, style = style)
                    }
                }
            }

            // Stroke number popups at stroke endpoints
            if (canvasSize.width > 0 && canvasSize.height > 0 && strokes.isNotEmpty()) {
                // Compute the bounding box of ALL strokes to fit the canvas
                val allBounds = remember(strokes) {
                    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
                    var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
                    strokes.forEach { p ->
                        val b = p.getBounds()
                        minX = minOf(minX, b.left); minY = minOf(minY, b.top)
                        maxX = maxOf(maxX, b.right); maxY = maxOf(maxY, b.bottom)
                    }
                    androidx.compose.ui.geometry.Rect(minX, minY, maxX, maxY)
                }

                val padding = 40f
                val availW = canvasSize.width.toFloat() - padding * 2
                val availH = canvasSize.height.toFloat() - padding * 2
                val contentW = allBounds.right - allBounds.left
                val contentH = allBounds.bottom - allBounds.top
                val scale = minOf(
                    availW / contentW.coerceAtLeast(1f),
                    availH / contentH.coerceAtLeast(1f),
                    1f
                )
                val offsetX = (canvasSize.width - contentW * scale) / 2f - allBounds.left * scale
                val offsetY = (canvasSize.height - contentH * scale) / 2f - allBounds.top * scale

                val pm = PathMeasure()
                val shownCount = currentStrokeIndex.coerceAtMost(totalStrokes)

                for (i in 0 until shownCount) {
                    val stroke = strokes[i]
                    pm.setPath(stroke, false)
                    val endPt = pm.getPosition(pm.length)

                    val sx = endPt.x * scale + offsetX
                    val sy = endPt.y * scale + offsetY

                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(LocalDensity.current) { (sx - 10).toDp() },
                                y = with(LocalDensity.current) { (sy - 10).toDp() }
                            )
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(accent.primary)
                            .padding(0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${i + 1}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Play overlay when idle at start
            if (!isPlaying && currentStrokeIndex == 0 && strokeProgress.value == 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            currentStrokeIndex = 0
                            isPlaying = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow, null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accent.primary.copy(alpha = 0.15f))
                            .padding(10.dp),
                        tint = accent.primary
                    )
                }
            }

            // Replay overlay when finished
            if (!isPlaying && currentStrokeIndex >= totalStrokes && totalStrokes > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            currentStrokeIndex = 0
                            coroutineScope.launch { strokeProgress.snapTo(0f) }
                            isPlaying = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Replay, null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accent.primary.copy(alpha = 0.15f))
                            .padding(10.dp),
                        tint = accent.primary
                    )
                }
            }

            // Stroke count badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${currentStrokeIndex.coerceAtMost(totalStrokes)} / $totalStrokes",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Play/Pause
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.primary.copy(alpha = 0.12f))
                    .clickable {
                        if (isPlaying) {
                            isPlaying = false
                        } else {
                            if (currentStrokeIndex >= totalStrokes) {
                                currentStrokeIndex = 0
                                coroutineScope.launch { strokeProgress.snapTo(0f) }
                            }
                            isPlaying = true
                        }
                    }
                    .padding(5.dp),
                tint = accent.primary
            )

            // Progress slider — smooth 0..1
            Slider(
                value = progressFraction,
                onValueChange = { target ->
                    // Scrub: compute which stroke and progress to jump to
                    val targetStroke = (target * totalStrokes).toInt().coerceIn(0, totalStrokes - 1)
                    val targetProgress = (target * totalStrokes - targetStroke).coerceIn(0f, 1f)
                    isPlaying = false
                    currentStrokeIndex = targetStroke
                    coroutineScope.launch {
                        strokeProgress.snapTo(targetProgress)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = accent.primary,
                    activeTrackColor = accent.primary,
                    inactiveTrackColor = surfaceColors.surfaceInteractive.copy(alpha = 0.3f)
                )
            )

            // Reset
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Reset",
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable {
                        isPlaying = false
                        currentStrokeIndex = 0
                        coroutineScope.launch { strokeProgress.snapTo(0f) }
                    }
                    .padding(2.dp),
                tint = surfaceColors.textMuted
            )
        }

        Spacer(Modifier.height(4.dp))

        // Speed controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${"%.1f".format(playbackSpeed)}×",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accent.secondary,
                modifier = Modifier.width(32.dp)
            )

            Slider(
                value = playbackSpeed,
                onValueChange = { playbackSpeed = it },
                valueRange = 0.5f..3f,
                steps = 0,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = accent.secondary,
                    activeTrackColor = accent.secondary,
                    inactiveTrackColor = surfaceColors.surfaceInteractive.copy(alpha = 0.3f)
                )
            )

            // Reset speed
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Reset speed",
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { playbackSpeed = 1f }
                    .padding(2.dp),
                tint = surfaceColors.textMuted
            )
        }
    }
}
