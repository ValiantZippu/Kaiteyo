package ua.syt0r.kanji.desktop.game.ui.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.activity.Stroke
import ua.syt0r.kanji.desktop.game.activity.StrokePoint
import ua.syt0r.kanji.desktop.game.activity.WritingEvaluator

/**
 * The in-world writing desk (spec §57-59): trace the target kana with mouse
 * or touch. Evaluation is lenient (coverage, not penmanship) — a rough trace
 * passes, because the world rewards the attempt, not perfection.
 */
@Composable
fun WritingActivityPanel(session: GameSession) {
    if (!session.state.writingOpen) return
    val node = session.currentWritingTarget() ?: run {
        androidx.compose.runtime.LaunchedEffect(Unit) { session.state.writingOpen = false }
        return
    }

    // Strokes in normalized 0..1 space (canvas size independent).
    var strokes by remember { mutableStateOf<List<Stroke>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<Stroke>(emptyList()) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var passed by remember { mutableStateOf(false) }

    // A new target starts a clean slate (the panel reuses this composition).
    androidx.compose.runtime.LaunchedEffect(node.id) {
        strokes = emptyList()
        currentStroke = emptyList()
        feedback = null
        passed = false
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(DsSpacing.Lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(DsRadius.Lg))
                .background(Color(0xFF171E26).copy(alpha = 0.96f))
                .padding(DsSpacing.Lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            Text(
                text = "書いてみよう — try writing",
                color = Color(0xFFFFD54F),
                fontSize = DsType.Label,
                fontWeight = FontWeight.SemiBold
            )
            // The target glyph (faint) behind the drawing surface.
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(Color.White.copy(alpha = 0.06f))
                    .pointerInput(node.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val size = this.size
                                for (change in event.changes) {
                                    val pos = change.position
                                    val point = StrokePoint(
                                        (pos.x / size.width).coerceIn(0f, 1f),
                                        (pos.y / size.height).coerceIn(0f, 1f)
                                    )
                                    when {
                                        change.pressed && change.changedToDown() -> {
                                            currentStroke = listOf(point)
                                        }
                                        change.pressed && change.positionChanged() -> {
                                            currentStroke = currentStroke + point
                                        }
                                        else -> {
                                            if (currentStroke.isNotEmpty()) {
                                                strokes = strokes + listOf(currentStroke)
                                                currentStroke = emptyList()
                                            }
                                        }
                                    }
                                    change.consume()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = node.headword,
                    color = Color.White.copy(alpha = 0.14f),
                    fontSize = DsType.Display
                )
                // Draw the captured strokes.
                Canvas(Modifier.fillMaxWidth()) {
                    val path = Path()
                    for (stroke in strokes + listOf(currentStroke)) {
                        if (stroke.size < 2) continue
                        path.reset()
                        val first = stroke.first()
                        path.moveTo(first.x * size.width, first.y * size.height)
                        for (point in stroke.drop(1)) {
                            path.lineTo(point.x * size.width, point.y * size.height)
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFFFFD54F),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = StrokeCap.Round)
                        )
                    }
                }
            }
            // Reading + meaning of the target.
            Text(
                text = node.reading.ifBlank { node.headword },
                color = Color.White.copy(alpha = 0.8f),
                fontSize = DsType.Body
            )
            if (node.meaning.isNotBlank() && session.settings.assistanceLevel != ua.syt0r.kanji.desktop.game.learning.AssistanceLevel.Minimal) {
                Text(
                    text = node.meaning,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = DsType.Caption
                )
            }
            // Kanji targets show their real stroke count from Kaiteyo's
            // dictionary (spec §64) — the goal is on the board before you
            // draw. Stroke-order verification waits for real stroke data.
            if (node.kind == ua.syt0r.kanji.desktop.game.learning.KnowledgeKind.KANJI) {
                val strokes = session.bridge.lookup(node.lookupKey())
                    ?.kanji?.firstOrNull()?.strokeCounts?.firstOrNull()
                if (strokes != null) {
                    Text(
                        text = "漢字 · $strokes strokes",
                        color = Color(0xFF90CAF9).copy(alpha = 0.75f),
                        fontSize = DsType.Caption
                    )
                }
            }
            feedback?.let {
                Text(
                    text = it,
                    color = if (passed) Color(0xFFA5D6A7) else Color(0xFFFFAB91),
                    fontSize = DsType.Body,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = "Clear",
                    kind = DsButtonKind.Ghost,
                    onClick = {
                        strokes = emptyList()
                        currentStroke = emptyList()
                        feedback = null
                    }
                )
                DsButton(
                    text = "Check チェック",
                    kind = DsButtonKind.Primary,
                    onClick = {
                        val all = strokes + listOf(currentStroke)
                        val result = WritingEvaluator.evaluate(all)
                        if (result.pass) {
                            passed = true
                            feedback = "Great!  ${node.headword} → ${node.reading.ifBlank { node.headword }}"
                            session.completeWriting(node.id)
                        } else {
                            passed = false
                            feedback = "Almost — try again. もう一度"
                            session.failedWriting()
                        }
                    }
                )
            }
            Text(
                text = "Esc to close",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = DsType.Caption,
                modifier = Modifier
                    .clip(RoundedCornerShape(DsRadius.Sm))
                    .clickable { session.state.writingOpen = false }
                    .padding(DsSpacing.Sm)
            )
        }
    }
}
