package ua.syt0r.kanji.presentation.screen.main.features

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.core.statistics.ContentTypes
import ua.syt0r.kanji.core.statistics.ExamConfig
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog
import ua.syt0r.kanji.presentation.screen.main.screen.statistics.ExamRunnerScreen
import ua.syt0r.kanji.presentation.screen.main.screen.statistics.LineChart
import ua.syt0r.kanji.presentation.screen.main.screen.statistics.SectionCard
import ua.syt0r.kanji.presentation.screen.main.screen.statistics.StatCard
import kotlin.math.roundToInt

// ============================================================
// EXAM WORKSPACE — one exam UI, two homes.
// The full exam system (generate → run → score → history) lives
// here so both Statistics ("Exams" tab) and the Library ("Exams"
// mode) present the exact same workspace. Exam generation reads
// the user's actual studied content via StatisticsController.
// ============================================================

@Composable
fun ExamWorkspace(controller: StatisticsController, scope: CoroutineScope) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var showConfig by remember { mutableStateOf(false) }
    val stats = controller.examStatistics

    // The exam runner takes over the whole workspace while an exam is
    // active (and while its graded review is still on screen), exactly
    // like the Statistics host does — so starting an exam from the
    // Library runs the identical question-by-question experience.
    if (controller.activeExam != null || controller.lastGradedExam != null) {
        ExamRunnerScreen(
            controller = controller,
            onExit = { scope.launch { controller.abandonExam() } },
            onDismissResults = { controller.clearLastGradedExam() }
        )
        return
    }

    // The workspace is self-contained: loading exam history is its own
    // concern, so either host (Stats tab or Library mode) gets a ready view.
    LaunchedEffect(Unit) { controller.ensureLoaded() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(
                title = "Examination system",
                subtitle = "Original questions generated from the language data you have studied"
            ) {
                Row(Modifier.fillMaxWidth()) {
                    StatCard("Completed", stats.completed.toString(), "exams", accent.primary, Modifier.weight(1f))
                    StatCard("Avg score", "${stats.averageScore.roundToInt()}", "of ${controller.exams.firstOrNull()?.questionCount ?: 20}", Color(0xFFC2FC8B), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatCard("Best", stats.highestScore.toString(), "score", Color(0xFF7BC8FF), Modifier.weight(1f))
                    StatCard("Avg accuracy", "${(stats.averageAccuracy * 100).roundToInt()}%", "correct answers", Color(0xFFA78BFA), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { showConfig = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Start new exam")
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { scope.launch { controller.startWeeklyExam() } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start weekly exam (last 7 days)")
                }
                if (controller.exams.any { it.status.ordinal == 0 }) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "An exam is in progress — finish it to see results.",
                        fontSize = 11.sp, color = surfaceColors.textMuted
                    )
                }
            }
        }

        if (stats.scoreTrend.isNotEmpty()) {
            item {
                SectionCard(title = "Score trend", subtitle = "Your exam results over time") {
                    LineChart(
                        points = stats.scoreTrend.map { it.date.toString() to it.score.toFloat() },
                        color = accent.primary
                    )
                }
            }
        }

        if (controller.exams.isNotEmpty()) {
            item {
                SectionCard(title = "Exam history", subtitle = "${controller.exams.size} recorded") {
                    controller.exams.take(20).forEach { exam ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(exam.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary)
                                Text(
                                    exam.startedAt.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString() +
                                        " · ${exam.examType} · ${exam.questionCount} questions",
                                    fontSize = 11.sp, color = surfaceColors.textMuted
                                )
                            }
                            when (exam.status) {
                                ua.syt0r.kanji.core.statistics.ExamStatus.Completed -> {
                                    Text(
                                        "${exam.score}/${exam.questionCount} · ${(exam.accuracy * 100).roundToInt()}%",
                                        fontSize = 12.sp,
                                        color = if (exam.accuracy >= 0.7f) Color(0xFFC2FC8B) else Color(0xFFFF6B6B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                ua.syt0r.kanji.core.statistics.ExamStatus.InProgress -> {
                                    Text("in progress", fontSize = 11.sp, color = Color(0xFFFFD93D))
                                }
                                else -> Text("abandoned", fontSize = 11.sp, color = surfaceColors.textMuted)
                            }
                            IconButton(
                                onClick = { scope.launch { controller.deleteExam(exam.id) } }
                            ) { Icon(Icons.Default.Close, "Delete exam", tint = surfaceColors.textMuted, modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Exams are generated from items you have actually studied (kanji and vocabulary from your decks), with multiple-choice distractors drawn from related content and optional free-text production questions. Results feed your weakness analytics.",
                fontSize = 11.sp, color = surfaceColors.textMuted
            )
        }
    }

    if (showConfig) {
        ExamConfigDialog(
            onDismiss = { showConfig = false },
            onStart = { config ->
                showConfig = false
                scope.launch { controller.startExam(config) }
            }
        )
    }
}

@Composable
private fun ExamConfigDialog(
    onDismiss: () -> Unit,
    onStart: (ExamConfig) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    var questionCount by remember { mutableStateOf(20) }
    var jlpt by remember { mutableStateOf<Int?>(null) }
    var contentType by remember { mutableStateOf<String?>(null) }
    var includeProduction by remember { mutableStateOf(true) }
    var timed by remember { mutableStateOf(false) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColors.surface,
        title = { Text("New exam", color = surfaceColors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Questions: $questionCount", fontSize = 13.sp, color = surfaceColors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 20, 30, 50).forEach { count ->
                        TextButton(
                            onClick = { questionCount = count },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = if (questionCount == count) LocalKaiteyoAccent.current.primary else surfaceColors.textMuted
                            )
                        ) { Text("$count") }
                    }
                }
                Text("Scope", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Mixed" to null, "Kanji" to ContentTypes.KANJI, "Vocab" to ContentTypes.VOCAB).forEach { (label, value) ->
                        TextButton(
                            onClick = { contentType = value },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = if (contentType == value) LocalKaiteyoAccent.current.primary else surfaceColors.textMuted
                            )
                        ) { Text(label) }
                    }
                }
                Text("JLPT level", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All" to null, "N5" to 5, "N4" to 4, "N3" to 3, "N2" to 2, "N1" to 1).forEach { (label, value) ->
                        TextButton(
                            onClick = { jlpt = value },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = if (jlpt == value) LocalKaiteyoAccent.current.primary else surfaceColors.textMuted
                            )
                        ) { Text(label) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Include writing questions", fontSize = 13.sp, color = surfaceColors.textPrimary, modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = includeProduction,
                        onCheckedChange = { includeProduction = it }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Timed (45s per question)", fontSize = 13.sp, color = surfaceColors.textPrimary, modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = timed,
                        onCheckedChange = { timed = it }
                    )
                }
                Text(
                    "Questions sample the items you have studied. Results are stored and feed the analytics below.",
                    fontSize = 11.sp, color = surfaceColors.textMuted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onStart(
                    ExamConfig(
                        title = "Custom exam",
                        questionCount = questionCount,
                        jlptLevel = jlpt,
                        contentType = contentType,
                        includeProduction = includeProduction,
                        timeLimitMs = if (timed) questionCount * 45_000L else null,
                        seed = kotlin.random.Random.nextLong()
                    )
                )
            }) { Text("Start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
