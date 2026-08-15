package ua.syt0r.kanji.desktop.ui.writing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsNumericField
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTagChip
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.errorColor
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus

// ============================================
// WRITING PRACTICE
// First-class kanji handwriting drill. The prompt
// shows the meaning + readings; the learner writes
// the kanji freehand on a stroke grid, reveals the
// answer, then self-grades. Grading feeds the SRS
// scheduler and today's study summary.
// ============================================

@Composable
fun WritingPracticeView(state: AppState) {
    val session = state.writingSession
    if (session == null) {
        WritingLaunchPanel(state)
    } else {
        WritingSessionPanel(state, session)
    }
}

// ============================================
// LAUNCH PANEL
// ============================================

@Composable
private fun WritingLaunchPanel(state: AppState) {
    val sc = surfaceColors()
    var count by remember { mutableStateOf(12) }
    var includeNew by remember { mutableStateOf(true) }

    val available = state.cards.count {
        it.status != SrsStatus.Suspended &&
            it.status != SrsStatus.Buried &&
            it.character.any { ch -> ch.code in 0x4E00..0x9FFF }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        DsCard(elevated = true) {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Writing practice", color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.Bold)
                Text(
                    text = "Handwrite kanji from memory. You will see the meaning and readings, draw the kanji on the grid, then reveal and grade yourself.",
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsBadge(text = "$available kanji available", tint = accent().primary)
                    DsBadge(text = "SRS aware", tint = Color(0xFFC2FC8B))
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    Text("Cards", color = sc.textSecondary, fontSize = DsType.Body)
                    DsNumericField(
                        value = count,
                        onValueChange = { count = it.coerceIn(1, 100) },
                        modifier = Modifier.width(110.dp)
                    )
                    DsToggle(
                        checked = includeNew,
                        onCheckedChange = { includeNew = it },
                        label = "Include new cards"
                    )
                }

                DsButton(
                    text = "Start writing",
                    icon = Icons.Default.PlayArrow,
                    enabled = available > 0,
                    onClick = { state.startWritingPractice(limit = count, includeNew = includeNew) }
                )
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("How it works", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.SemiBold)
                WritingHowRow("1", "Read the meaning and readings shown at the top.")
                WritingHowRow("2", "Write the kanji freehand on the stroke grid with your mouse or stylus.")
                WritingHowRow("3", "Press Reveal to compare your strokes with the correct character.")
                WritingHowRow("4", "Grade yourself — Again, Hard, Good or Easy. The scheduler adjusts the next review.")
            }
        }
    }
}

@Composable
private fun WritingHowRow(number: String, text: String) {
    val sc = surfaceColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(DsRadius.Full))
                .background(accent().primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = accent().primary, fontSize = DsType.Caption, fontWeight = FontWeight.Bold)
        }
        Text(text, color = sc.textSecondary, fontSize = DsType.Body)
    }
}

// ============================================
// SESSION PANEL
// ============================================

@Composable
private fun WritingSessionPanel(state: AppState, session: ua.syt0r.kanji.desktop.engine.review.ReviewSession) {
    val sc = surfaceColors()
    val entry = session.current()
    val card = entry?.card
    val progress = if (session.total == 0) 0f else session.currentIndex.toFloat() / session.total

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        // Progress
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Text(
                text = "${(session.currentIndex + 1).coerceAtMost(session.total)} / ${session.total}",
                color = sc.textPrimary,
                fontSize = DsType.Title,
                fontWeight = FontWeight.SemiBold
            )
            DsProgressBar(fraction = progress, modifier = Modifier.weight(1f))
            DsIconButton(icon = Icons.Default.SkipNext, onClick = { state.skipWriting() }, contentDescription = "Skip card")
        }

        if (card == null) {
            DsEmptyState(
                title = "Practice complete",
                message = "All queued kanji handled. Nice work!",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            return
        }

        // Prompt
        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text(
                    text = card.meaning.ifBlank { "No meaning" },
                    color = sc.textPrimary,
                    fontSize = DsType.Heading,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    card.readings.take(3).forEach { reading ->
                        DsTagChip(label = reading, colorHex = "#7BC8FF")
                    }
                    if (card.strokeCount > 0) {
                        DsBadge(text = "${card.strokeCount} strokes", tint = sc.textMuted)
                    }
                }
                Text(
                    text = "Write the kanji for the meaning above.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }
        }

        // Canvas
        val canvasState = remember { WritingCanvasState() }
        WritingCanvas(
            revealed = state.writingRevealed,
            answer = card.character,
            canvasState = canvasState,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        )

        // Stroke evaluation feedback (shown after reveal when the evaluator
        // has canonical data for this character).
        val evaluation = remember(state.writingRevealed, card.character, canvasState.strokes.size) {
            if (!state.writingRevealed || canvasState.strokes.isEmpty()) {
                null
            } else {
                state.writingEvaluator.evaluate(
                    expression = card.character,
                    drawnStrokes = canvasState.normalizedStrokes(),
                    canvasWidth = canvasState.canvasSize?.width?.toDouble() ?: 380.0,
                    canvasHeight = canvasState.canvasSize?.height?.toDouble() ?: 380.0
                )
            }
        }
        if (evaluation != null && evaluation.supported && evaluation.strokes.isNotEmpty()) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Md), verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        Text(
                            text = "Stroke analysis",
                            color = sc.textPrimary,
                            fontSize = DsType.Label,
                            fontWeight = FontWeight.SemiBold
                        )
                        DsBadge(
                            text = "${evaluation.correctStrokes}/${evaluation.strokes.size} strokes · ${(evaluation.accuracy * 100).toInt()}% · ${evaluation.sourceLabel}",
                            tint = if (evaluation.accuracy >= 0.7f) successColor() else warningColor()
                        )
                        if (!evaluation.kanjiVgPresent) {
                            DsBadge(
                                text = "install KanjiVG for full coverage",
                                tint = sc.textMuted
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "Your self-grade still drives the schedule",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                    evaluation.strokes.forEach { ev ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stroke ${ev.strokeIndex + 1}",
                                color = sc.textSecondary,
                                fontSize = DsType.Caption,
                                modifier = Modifier.width(72.dp)
                            )
                            Text(
                                text = when (ev.mistake) {
                                    ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokeMistake.None -> "correct"
                                    ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokeMistake.Shape -> "shape deviation ${ev.deviation.toInt()}"
                                    ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokeMistake.Direction -> "wrong direction ${ev.directionErrorDegrees.toInt()}°"
                                    ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokeMistake.ShapeAndDirection -> "shape + direction"
                                },
                                color = if (ev.correct) successColor() else errorColor(),
                                fontSize = DsType.Caption
                            )
                        }
                    }
                }
            }
        }

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.writingRevealed) {
                DsButton(
                    text = "Again",
                    kind = DsButtonKind.Danger,
                    onClick = { state.rateWriting(ReviewRating.Again, canvasState) },
                    compact = false
                )
                DsButton(
                    text = "Hard",
                    kind = DsButtonKind.Secondary,
                    onClick = { state.rateWriting(ReviewRating.Hard, canvasState) }
                )
                DsButton(
                    text = "Good",
                    onClick = { state.rateWriting(ReviewRating.Good, canvasState) }
                )
                DsButton(
                    text = "Easy",
                    kind = DsButtonKind.Secondary,
                    onClick = { state.rateWriting(ReviewRating.Easy, canvasState) }
                )
            } else {
                DsButton(
                    text = "Reveal",
                    icon = Icons.Default.Visibility,
                    onClick = { state.writingRevealed = true }
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (state.writingRevealed) "Grade your attempt" else "Reveal to check",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

// ============================================
// HANDWRITING CANVAS
// ============================================

class WritingCanvasState {
    val strokes = mutableStateListOf<List<Offset>>()
    var current by mutableStateOf<List<Offset>>(emptyList())
    var canvasSize by mutableStateOf<androidx.compose.ui.geometry.Size?>(null)

    val isEmpty: Boolean get() = strokes.isEmpty() && current.isEmpty()

    fun undo() {
        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
    }

    fun clear() {
        strokes.clear()
        current = emptyList()
    }

    /** Completed strokes as normalized [StrokePoint]s for the evaluator. */
    fun normalizedStrokes(): List<List<ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokePoint>> {
        val size = canvasSize ?: return emptyList()
        if (size.width <= 0f || size.height <= 0f) return emptyList()
        return strokes.map { stroke ->
            stroke.map { p ->
                ua.syt0r.kanji.desktop.engine.stroke_evaluator.StrokePoint(p.x.toDouble(), p.y.toDouble())
            }
        }
    }
}

@Composable
internal fun WritingCanvas(
    revealed: Boolean,
    answer: String,
    canvasState: WritingCanvasState,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    val state = canvasState

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(DsRadius.Lg))
                .background(sc.surfaceElevated)
                .onSizeChanged { state.canvasSize = androidx.compose.ui.geometry.Size(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.first().position
                            when (event.type) {
                                PointerEventType.Press -> {
                                    state.current = listOf(position)
                                }
                                PointerEventType.Move -> {
                                    if (state.current.isNotEmpty()) {
                                        state.current = state.current + position
                                    }
                                }
                                PointerEventType.Release -> {
                                    if (state.current.isNotEmpty()) {
                                        state.strokes.add(state.current)
                                        state.current = emptyList()
                                    }
                                }
                                else -> Unit
                            }
                        }
                    }
                }
        ) {
            val border = sc.border.copy(alpha = 0.6f)
            val grid = sc.border.copy(alpha = 0.25f)

            // Outer frame
            drawRect(
                color = Color.Transparent
            )
            drawLine(
                color = border,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 3f
            )
            drawLine(
                color = border,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 3f
            )
            drawLine(
                color = border,
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 3f
            )
            drawLine(
                color = border,
                start = Offset(size.width, 0f),
                end = Offset(size.width, size.height),
                strokeWidth = 3f
            )

            // Inner grid: midlines + diagonals of the inner square
            val inset = size.minDimension * 0.06f
            val inner = androidx.compose.ui.geometry.Rect(
                left = inset,
                top = inset,
                right = size.width - inset,
                bottom = size.height - inset
            )
            val midX = (inner.left + inner.right) / 2f
            val midY = (inner.top + inner.bottom) / 2f

            drawLine(grid, Offset(inner.left, midY), Offset(inner.right, midY), 2f)
            drawLine(grid, Offset(midX, inner.top), Offset(midX, inner.bottom), 2f)
            drawLine(grid, inner.topLeft, inner.bottomRight, 1.5f)
            drawLine(grid, Offset(inner.right, inner.top), Offset(inner.left, inner.bottom), 1.5f)

            // Strokes
            val strokeWidth = (6 * density).coerceAtLeast(4f)
            (state.strokes + listOf(state.current)).forEach { stroke ->
                if (stroke.size >= 2) {
                    val path = Path()
                    path.moveTo(stroke.first().x, stroke.first().y)
                    stroke.drop(1).forEach { p ->
                        path.lineTo(p.x, p.y)
                    }
                    drawPath(
                        path = path,
                        color = if (revealed) Color(0xFFFF6B6B) else ac.primary,
                        style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                    )
                }
            }

            // Answer overlay after reveal
            if (revealed) {
                drawCircle(
                    color = sc.surfaceInteractive.copy(alpha = 0.5f),
                    radius = size.minDimension * 0.34f,
                    center = center
                )
            }
        }

        // Ghost answer rendered above the canvas
        if (revealed) {
            Text(
                text = answer,
                color = sc.textPrimary.copy(alpha = 0.14f),
                fontSize = (190).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    // Toolbar below the canvas
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DsIconButton(icon = Icons.Default.Undo, onClick = { state.undo() }, contentDescription = "Undo stroke", enabled = state.strokes.isNotEmpty())
        DsIconButton(icon = Icons.Default.Clear, onClick = { state.clear() }, contentDescription = "Clear canvas", enabled = !state.isEmpty)
        Spacer(Modifier.weight(1f))
        Text(
            text = "${state.strokes.size} strokes",
            color = sc.textMuted,
            fontSize = DsType.Caption
        )
    }
}
