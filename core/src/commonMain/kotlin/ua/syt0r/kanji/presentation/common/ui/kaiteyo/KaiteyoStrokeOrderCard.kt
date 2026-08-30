package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.wrapContentSize
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
 * Stroke order card with:
 * - Animated stroke drawing (smooth spring physics)
 * - Playback progress slider with scrubbing
 * - Speed control (0.5x to 3x)
 * - Stroke number popups at each stroke endpoint (dynamically positioned)
 */
@Composable
fun KaiteyoStrokeOrderCard(
    strokes: List<Path>,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Animation state
    var isPlaying by remember { mutableStateOf(false) }
    var completedStrokeCount by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    val strokeAnimProgress = remember { Animatable(0f) }
    val totalStrokes = strokes.size.coerceAtLeast(1)

    // Track which strokes have shown their number popup
    var shownStrokeNumbers by remember { mutableStateOf(setOf<Int>()) }

    // Canvas size for popup positioning
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Scrubbing state — separate from isPlaying to avoid LaunchedEffect restarts
    var isScrubbing by remember { mutableStateOf(false) }

    // Handle scrubbing: when user releases slider, jump to target stroke
    LaunchedEffect(isScrubbing) {
        if (!isScrubbing) return@LaunchedEffect
        val target = (playbackProgress * totalStrokes).toInt().coerceIn(0, totalStrokes)
        completedStrokeCount = target
        shownStrokeNumbers = (0 until target).toSet()
        strokeAnimProgress.snapTo(1f)
        isPlaying = false
    }

    // Auto-play animation — uses coroutine so changing speed doesn't restart
    LaunchedEffect(isPlaying, totalStrokes) {
        if (!isPlaying) return@LaunchedEffect

        val strokeDurationMs = (500 / playbackSpeed).toInt().coerceAtLeast(80)

        for (s in completedStrokeCount until totalStrokes) {
            if (!isPlaying) return@LaunchedEffect
            completedStrokeCount = s
            playbackProgress = s.toFloat() / totalStrokes
            strokeAnimProgress.snapTo(0f)
            strokeAnimProgress.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow * playbackSpeed
                )
            )
            shownStrokeNumbers = shownStrokeNumbers + s
            playbackProgress = (s + 1).toFloat() / totalStrokes
            delay(120) // Brief pause between strokes for clarity
        }
        isPlaying = false
        completedStrokeCount = totalStrokes
        playbackProgress = 1f
    }

    // Reset when strokes change
    LaunchedEffect(strokes) {
        completedStrokeCount = 0
        playbackProgress = 0f
        isPlaying = false
        isScrubbing = false
        shownStrokeNumbers = emptySet()
        strokeAnimProgress.snapTo(0f)
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

            // Draw completed strokes
            strokes.take(completedStrokeCount).forEach { stroke ->
                Canvas(Modifier.fillMaxSize()) {
                    drawPathMeasureStroke(stroke, accent.primary.copy(alpha = 0.6f), StrokeWidth * 1.1f, 1f)
                }
            }

            // Draw currently animating stroke
            if (isPlaying && completedStrokeCount < totalStrokes) {
                Canvas(Modifier.fillMaxSize()) {
                    drawPathMeasureStroke(
                        strokes[completedStrokeCount],
                        accent.primary,
                        StrokeWidth * 1.2f,
                        strokeAnimProgress.value
                    )
                }
            }

            // Stroke number popups — dynamically positioned using actual canvas bounds
            if (canvasSize.width > 0 && canvasSize.height > 0) {
                shownStrokeNumbers.forEach { strokeIdx ->
                    if (strokeIdx < strokes.size) {
                        val stroke = strokes[strokeIdx]
                        val measure = PathMeasure().apply { setPath(stroke, false) }
                        val endPoint = measure.getPosition(measure.length)

                        // Get the bounding box of this individual stroke
                        val bounds = stroke.getBounds()
                        val strokeW = bounds.right - bounds.left
                        val strokeH = bounds.bottom - bounds.top

                        // Scale to fit within canvas, maintaining aspect ratio
                        val padding = 40f
                        val availW = canvasSize.width.toFloat() - padding * 2
                        val availH = canvasSize.height.toFloat() - padding * 2
                        val scaleX = availW / (strokeW.coerceAtLeast(1f))
                        val scaleY = availH / (strokeH.coerceAtLeast(1f))
                        val scale = minOf(scaleX, scaleY, 1f)

                        val offsetX = (canvasSize.width - strokeW * scale) / 2f - bounds.left * scale
                        val offsetY = (canvasSize.height - strokeH * scale) / 2f - bounds.top * scale

                        val screenX = endPoint.x * scale + offsetX
                        val screenY = endPoint.y * scale + offsetY

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = with(density) { (screenX - 10).toDp() },
                                    y = with(density) { (screenY - 10).toDp() }
                                )
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(accent.primary)
                                .wrapContentSize(Alignment.Center)
                        ) {
                            Text(
                                text = "${strokeIdx + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Play overlay when idle
            if (!isPlaying && completedStrokeCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            completedStrokeCount = 0
                            shownStrokeNumbers = emptySet()
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
            if (!isPlaying && completedStrokeCount >= totalStrokes && totalStrokes > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            completedStrokeCount = 0
                            shownStrokeNumbers = emptySet()
                            coroutineScope.launch { strokeAnimProgress.snapTo(0f) }
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
                    text = "${completedStrokeCount.coerceAtMost(totalStrokes)} / $totalStrokes",
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
                            if (completedStrokeCount >= totalStrokes) {
                                completedStrokeCount = 0
                                shownStrokeNumbers = emptySet()
                                coroutineScope.launch { strokeAnimProgress.snapTo(0f) }
                            }
                            isPlaying = true
                        }
                    }
                    .padding(5.dp),
                tint = accent.primary
            )

            // Progress slider
            Slider(
                value = playbackProgress,
                onValueChange = { v ->
                    playbackProgress = v
                    isScrubbing = true
                },
                onValueChangeFinished = { isScrubbing = false },
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
                        completedStrokeCount = 0
                        playbackProgress = 0f
                        shownStrokeNumbers = emptySet()
                        coroutineScope.launch { strokeAnimProgress.snapTo(0f) }
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

/** Draw a kanji stroke using Compose PathMeasure */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPathMeasureStroke(
    path: Path,
    color: Color,
    strokeWidth: Float,
    progress: Float
) {
    if (progress <= 0f) return
    val measure = PathMeasure().apply { setPath(path, false) }
    val totalLen = measure.length
    val drawLen = totalLen * progress.coerceIn(0f, 1f)
    val partial = Path()
    measure.getSegment(0f, drawLen, partial, startWithMoveTo = true)

    drawPath(
        partial,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
