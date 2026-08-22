package ua.syt0r.kanji.presentation.screen.main.screen.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.statistics.ExamQuestionRecord
import ua.syt0r.kanji.core.statistics.GradedExam
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog
import ua.syt0r.kanji.presentation.screen.main.features.StatisticsController
import kotlin.math.roundToInt

// ============================================================
// EXAM RUNNER
// Runs an in-progress exam question by question:
//   - multiple-choice questions show selectable options,
//   - production questions collect free text,
//   - answers are persisted immediately after confirmation,
//   - a timed exam auto-submits when the clock runs out,
//   - quitting asks for confirmation and abandons the exam,
//   - after the last question the exam is graded and the full
//     review (every question + user answer + result) is shown.
// ============================================================

@Composable
fun ExamRunnerScreen(
    controller: StatisticsController,
    onExit: () -> Unit,
    onDismissResults: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val scope = rememberCoroutineScope()

    // A freshly graded exam takes over the whole runner with its review.
    controller.lastGradedExam?.let { graded ->
        ExamResultsScreen(
            graded = graded,
            onDone = onDismissResults
        )
        return
    }

    val inProgress = controller.activeExam
    if (inProgress == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No exam in progress.", color = surfaceColors.textMuted)
        }
        return
    }

    val questions = inProgress.questions
    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This exam has no questions.", color = surfaceColors.textMuted)
        }
        return
    }

    var currentIndex by remember(inProgress.exam.id) { mutableStateOf(0) }
    var showQuitConfirm by remember { mutableStateOf(false) }

    val question = questions.getOrNull(currentIndex)
    if (question == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing to show.", color = surfaceColors.textMuted)
        }
        return
    }

    val options = decodeOptions(question)
    // Per-question answer state, re-initialized from persisted answers on
    // resume so an interrupted exam keeps its progress.
    var selectedOption by remember(question.id) {
        mutableStateOf(
            when {
                options == null || question.userAnswer == null -> null
                else -> question.userAnswer.takeIf { it in options }
            }
        )
    }
    var textAnswer by remember(question.id) {
        mutableStateOf(if (options == null) question.userAnswer.orEmpty() else "")
    }

    // Timed exams: count down and auto-submit when time runs out.
    var elapsedSeconds by remember(inProgress.exam.id) { mutableStateOf(0L) }
    LaunchedEffect(inProgress.exam.id) {
        while (isActive) {
            delay(1_000)
            elapsedSeconds += 1
            val limitMs = inProgress.exam.timeLimitMs
            if (limitMs != null && elapsedSeconds * 1000 >= limitMs) {
                scope.launch { controller.finishExam() }
                break
            }
        }
    }
    val remainingMs = inProgress.exam.timeLimitMs
        ?.let { limit -> (limit - elapsedSeconds * 1000).coerceAtLeast(0L) }

    fun confirmAnswer() {
        val answer = if (options != null) selectedOption.orEmpty() else textAnswer
        scope.launch {
            controller.answerActiveExam(currentIndex, answer)
            if (currentIndex >= questions.lastIndex) {
                controller.finishExam()
            } else {
                currentIndex += 1
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Header: progress + timer + quit
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Question ${currentIndex + 1} of ${questions.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = surfaceColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            remainingMs?.let { remaining ->
                Text(
                    "⏱ ${formatClock(remaining)}",
                    fontSize = 12.sp,
                    color = if (remaining < 30_000) LocalKaiteyoSemanticColors.current.error else surfaceColors.textSecondary
                )
                Spacer(Modifier.width(10.dp))
            }
            IconButton(onClick = { showQuitConfirm = true }) {
                Icon(Icons.Default.Close, "Quit exam", tint = surfaceColors.textMuted)
            }
        }
        LinearProgressIndicator(
            progress = { ((currentIndex + 1).toFloat() / questions.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = LocalKaiteyoAccent.current.primary,
            trackColor = surfaceColors.surfaceInteractive
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    question.prompt,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = surfaceColors.textPrimary
                )
                if (question.skill.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "skill: ${question.skill} · ${question.questionType.replace("_", " ")}",
                        fontSize = 11.sp,
                        color = surfaceColors.textMuted
                    )
                }
            }

            if (options != null) {
                items(options, key = { it }) { option ->
                    val selected = selectedOption == option
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) LocalKaiteyoAccent.current.primary.copy(alpha = 0.14f)
                                else surfaceColors.surface
                            )
                            .clickable { selectedOption = option }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (selected) "●" else "○",
                                fontSize = 14.sp,
                                color = if (selected) LocalKaiteyoAccent.current.primary else surfaceColors.textMuted
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(option, fontSize = 15.sp, color = surfaceColors.textPrimary)
                        }
                    }
                }
            } else {
                item {
                    OutlinedTextField(
                        value = textAnswer,
                        onValueChange = { textAnswer = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type your answer (kana or kanji)...", fontSize = 13.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 17.sp, color = surfaceColors.textPrimary)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Free-text answers are normalized (kana folding, spacing) so correct variants are accepted.",
                        fontSize = 11.sp,
                        color = surfaceColors.textMuted
                    )
                }
            }
        }

        // Footer: skip / confirm
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        if (currentIndex >= questions.lastIndex) controller.finishExam()
                        else currentIndex += 1
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (currentIndex >= questions.lastIndex) "Finish without answering" else "Skip",
                    fontSize = 13.sp
                )
            }
            val canConfirm = if (options != null) selectedOption != null else textAnswer.isNotBlank()
            Button(
                onClick = { confirmAnswer() },
                enabled = canConfirm,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (currentIndex >= questions.lastIndex) "Finish" else "Confirm", fontSize = 13.sp)
            }
        }
    }

    if (showQuitConfirm) {
        KaiteyoAlertDialog(
            onDismissRequest = { showQuitConfirm = false },
            containerColor = surfaceColors.surface,
            title = { Text("Quit exam?", color = surfaceColors.textPrimary) },
            text = {
                Text(
                    "Your answers so far are saved, but the exam will be marked as abandoned.",
                    fontSize = 13.sp,
                    color = surfaceColors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showQuitConfirm = false
                    scope.launch { controller.abandonExam() }
                    onExit()
                }) { Text("Quit", color = LocalKaiteyoSemanticColors.current.error) }
            },
            dismissButton = { TextButton(onClick = { showQuitConfirm = false }) { Text("Keep going") } }
        )
    }
}

// ============================================================
// EXAM RESULTS / REVIEW
// ============================================================

@Composable
private fun ExamResultsScreen(
    graded: GradedExam,
    onDone: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val total = graded.questions.size
    val correct = graded.score
    val wrong = total - correct
    val scorePercent = if (total == 0) 0f else graded.accuracy

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            "Exam complete",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(graded.exam.title, fontSize = 12.sp, color = surfaceColors.textMuted)

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                title = "Score",
                value = "$correct / $total",
                subtitle = "${(scorePercent * 100).roundToInt()}%",
                color = accent.primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Correct",
                value = "$correct",
                subtitle = "questions",
                color = LocalKaiteyoSemanticColors.current.success,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Incorrect",
                value = "$wrong",
                subtitle = "incl. skipped",
                color = LocalKaiteyoSemanticColors.current.error,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))
        Text("Review", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
        Spacer(Modifier.height(6.dp))

        graded.questions.forEachIndexed { index, q ->
            val isCorrect = q.isCorrect == true
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isCorrect) "✓" else "✗",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) LocalKaiteyoSemanticColors.current.success else LocalKaiteyoSemanticColors.current.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${index + 1}. ${q.prompt}",
                        fontSize = 13.sp,
                        color = surfaceColors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Column(Modifier.padding(start = 22.dp)) {
                    Text(
                        "Your answer: ${q.userAnswer?.ifBlank { "—" } ?: "—"}",
                        fontSize = 12.sp,
                        color = if (isCorrect) surfaceColors.textSecondary else LocalKaiteyoSemanticColors.current.error
                    )
                    if (!isCorrect) {
                        Spacer(Modifier.height(2.dp))
                        Text("Correct answer: ${q.answer}", fontSize = 12.sp, color = LocalKaiteyoSemanticColors.current.success)
                        if (!q.mistakeCategory.isNullOrBlank() && q.mistakeCategory != "none") {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Mistake: ${q.mistakeCategory.replace("_", " ")}",
                                fontSize = 11.sp,
                                color = surfaceColors.textMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Incorrect answers feed your weakness analytics and future exams.",
            fontSize = 11.sp,
            color = surfaceColors.textMuted
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Back to statistics")
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ============================================================
// Helpers
// ============================================================

private fun decodeOptions(question: ExamQuestionRecord): List<String>? {
    val json = question.optionsJson ?: return null
    return runCatching { Json.decodeFromString<List<String>>(json) }.getOrNull()
}

private fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
