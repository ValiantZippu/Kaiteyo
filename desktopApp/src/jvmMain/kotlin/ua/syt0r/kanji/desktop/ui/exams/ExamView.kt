package ua.syt0r.kanji.desktop.ui.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsStatTile
import ua.syt0r.kanji.desktop.designsystem.DsTabRow
import ua.syt0r.kanji.desktop.designsystem.DsTextButton
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.errorColor
import ua.syt0r.kanji.desktop.designsystem.infoColor
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.desktop.engine.learning.ExamAnswer
import ua.syt0r.kanji.desktop.engine.learning.ExamDraft
import ua.syt0r.kanji.desktop.engine.learning.ExamQuestion
import ua.syt0r.kanji.desktop.engine.learning.ExamQuestionType
import ua.syt0r.kanji.desktop.engine.learning.ExamResult
import ua.syt0r.kanji.desktop.engine.learning.ExamType
import ua.syt0r.kanji.desktop.engine.learning.StudyVsExamGap
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString

// ============================================
// EXAM VIEW
// A real exam system: choose the exam type,
// answer generated questions, get scored, and
// inspect exam analytics — all backed by the
// unified LearningStore (no fake questions).
//
//   * Skill exams — kanji / vocab / radical /
//                   grammar, recognition +
//                   production directions
//   * JLPT simulation — timed, sectioned exam
//                   (Vocabulary · Grammar ·
//                   Reading) mirroring the real
//                   JLPT structure and pacing
//   * Weekly + mistakes — from real study
//   * Smart recommendations — driven by your
//                   weakest measured areas
// ============================================

private enum class ExamTab(val labelKey: () -> String) {
    Take({ resolveSuiteString { tabTakeExam } }),
    History({ resolveSuiteString { tabResults } }),
    Analytics({ resolveSuiteString { tabAnalytics } });

    val label: String get() = labelKey()
}

@Composable
fun ExamView(state: AppState) {
    val learning = state.learning
    var tab by remember { mutableStateOf(ExamTab.Take) }
    var activeDraft by remember { mutableStateOf<ExamDraft?>(null) }
    var lastResult by remember { mutableStateOf<ExamResult?>(null) }

    // The command palette may stage a generated draft for immediate start
    // ("Start weekly assessment", "Start mistakes review") — consume it here
    // so the quick-start lands in the Take tab exactly like a manual start.
    LaunchedEffect(state.pendingExamDraft) {
        state.pendingExamDraft?.let { draft ->
            activeDraft = draft
            lastResult = null
            tab = ExamTab.Take
            state.pendingExamDraft = null
        }
    }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsTabRow(
            tabs = ExamTab.entries.map { it.label },
            selectedIndex = ExamTab.entries.indexOf(tab),
            onSelect = { tab = ExamTab.entries[it] }
        )

        // Leaving the Take tab dismisses any in-flight exam state cleanly.
        LaunchedEffect(tab) {
            if (tab != ExamTab.Take) {
                lastResult = null
                activeDraft = null
            }
        }

        val draft = activeDraft
        when {
            lastResult != null -> {
                val result = lastResult!!
                ExamResultScreen(
                    state = state,
                    result = result,
                    draft = draft,
                    onDone = { lastResult = null; activeDraft = null },
                    onRetake = {
                        val regenerated = learning.exams.generate(
                            type = draft?.type ?: ExamType.MixedJlpt,
                            questionCount = draft?.questions?.size ?: 20,
                            deckId = draft?.deckId.orEmpty(),
                            jlpt = draft?.jlpt,
                            weekly = draft?.weekly ?: false
                        )
                        if (regenerated != null) {
                            lastResult = null
                            activeDraft = regenerated
                        } else {
                            lastResult = null
                            activeDraft = null
                            state.toastHost.show(
                                resolveSuiteString { noMatchesForExam },
                                kind = ua.syt0r.kanji.desktop.model.ToastKind.Warning
                            )
                        }
                    }
                )
            }
            draft != null -> ExamTakingScreen(
                state = state,
                draft = draft,
                onFinish = { result -> lastResult = result },
                onCancel = { activeDraft = null }
            )
            tab == ExamTab.Take -> ExamConfigScreen(state, onStart = { activeDraft = it })
            tab == ExamTab.History -> ExamHistoryScreen(state)
            else -> ExamAnalyticsScreen(state)
        }
    }
}

// ============================================
// EXAM CONFIGURATION
// ============================================

@Composable
private fun ExamConfigScreen(state: AppState, onStart: (ExamDraft) -> Unit) {
    val learning = state.learning
    val sc = surfaceColors()
    var type by remember { mutableStateOf(ExamType.MixedJlpt) }
    var questionCount by remember { mutableStateOf(20) }
    var deckId by remember { mutableStateOf("") }
    var jlpt by remember { mutableStateOf<Int?>(null) }
    var timeLimit by remember { mutableStateOf(0) } // minutes, 0 = none

    if (learning.isEmpty) {
        DsCard {
            DsEmptyState(
                title = resolveSuiteString { examNoContentTitle },
                message = resolveSuiteString { examNoContentMessage },
                icon = Icons.Default.School
            )
        }
        return
    }

    val deckOptions = state.library.decks.filter { !it.archived }
    val simulation = type == ExamType.JlptSimulation

    val startExam: (ExamType, Int?, Int) -> Unit = { examType, level, count ->
        val draft = learning.exams.generate(
            type = examType,
            questionCount = count,
            deckId = deckId,
            jlpt = level
        )
        if (draft == null) {
            state.toastHost.show(resolveSuiteString { noMatchesForExam }, kind = ua.syt0r.kanji.desktop.model.ToastKind.Warning)
        } else {
            onStart(draft)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        ExamRecommendationsCard(state, onStart = { t, level -> startExam(t, level, 15) })

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                DsSectionHeader(title = resolveSuiteString { examSetupTitle }, subtitle = "Questions are generated from your real learning state")
                ExamTypeSelect(type) { type = it }

                if (simulation) {
                    // The simulation has a fixed, timed structure — explain it
                    // instead of pretending a flat question count applies.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DsRadius.Md))
                            .background(sc.surfaceInteractive.copy(alpha = 0.5f))
                            .padding(DsSpacing.Md)
                    ) {
                        Text(
                            "Three timed sections mirroring the real JLPT — 文字・語彙 (Vocabulary), 文法 (Grammar), 読解 (Reading). " +
                                "Each section has its own clock; questions come from your ${if (jlpt != null) "N$jlpt" else "all-level"} notes.",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                        Text(resolveSuiteString { questionsLabel }, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(100.dp))
                        Slider(
                            value = questionCount.toFloat(),
                            onValueChange = { questionCount = it.toInt() },
                            valueRange = 5f..50f,
                            steps = 8,
                            modifier = Modifier.weight(1f)
                        )
                        Text("$questionCount", color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                        Text(resolveSuiteString { timeLimitLabel }, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(100.dp))
                        Slider(
                            value = timeLimit.toFloat(),
                            onValueChange = { timeLimit = it.toInt() },
                            valueRange = 0f..60f,
                            steps = 11,
                            modifier = Modifier.weight(1f)
                        )
                        Text(if (timeLimit == 0) resolveSuiteString { noneLabel } else "${timeLimit}m", color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    Text(resolveSuiteString { scopeLabel }, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(100.dp))
                    if (deckOptions.isNotEmpty()) {
                        DsSelect(
                            selected = deckId,
                            options = listOf("") + deckOptions.map { it.id },
                            onSelected = { deckId = it },
                            labelOf = { id -> if (id.isBlank()) resolveSuiteString { allDecks } else (deckOptions.firstOrNull { it.id == id }?.name ?: id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    Text(resolveSuiteString { jlptBandLabel }, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(100.dp))
                    DsSelect(
                        selected = jlpt,
                        options = listOf<Int?>(null, 5, 4, 3, 2, 1),
                        onSelected = { jlpt = it },
                        labelOf = { level -> if (level == null) resolveSuiteString { anyLevel } else "N$level" },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(DsSpacing.Sm))
                DsButton(
                    text = if (simulation) resolveSuiteString { startJlptSimulation } else resolveSuiteString { startExamButton },
                    icon = Icons.Default.PlayArrow,
                    onClick = {
                        val draft = learning.exams.generate(
                            type = type,
                            questionCount = questionCount,
                            deckId = deckId,
                            jlpt = jlpt,
                            timeLimitMs = if (simulation) 0 else timeLimit * 60_000L
                        )
                        if (draft == null) {
                            state.toastHost.show(resolveSuiteString { noMatchesForExam }, kind = ua.syt0r.kanji.desktop.model.ToastKind.Warning)
                        } else {
                            onStart(draft)
                        }
                    }
                )
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsSectionHeader(title = resolveSuiteString { quickExams }, subtitle = "One click, generated from real state")
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(text = resolveSuiteString { weeklyAssessmentLabel }, kind = DsButtonKind.Secondary, onClick = {
                        val draft = learning.exams.generateWeekly()
                        if (draft == null) state.toastHost.show(resolveSuiteString { nothingStudiedThisWeek }, kind = ua.syt0r.kanji.desktop.model.ToastKind.Warning)
                        else onStart(draft)
                    })
                    DsButton(text = resolveSuiteString { mistakesReviewLabel }, kind = DsButtonKind.Secondary, onClick = {
                        val draft = learning.exams.generate(ExamType.Mistakes, questionCount = 15)
                        if (draft == null) state.toastHost.show(resolveSuiteString { noMistakesRecorded }, kind = ua.syt0r.kanji.desktop.model.ToastKind.Warning)
                        else onStart(draft)
                    })
                }
                Text(
                    "The weekly assessment covers what you actually studied this week. Mistakes review tests your real recorded mistakes.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
    }
}

/**
 * Smart recommendations — every one is derived from real exam analytics
 * (weakest question type, weakest JLPT band, recognition-vs-production gap).
 * Nothing here is a guess.
 */
@Composable
private fun ExamRecommendationsCard(state: AppState, onStart: (ExamType, Int?) -> Unit) {
    val learning = state.learning
    val sc = surfaceColors()
    val aggregates = learning.examAggregates()
    if (aggregates.count == 0) return

    val byType = learning.examAccuracyByType()
    val byJlpt = learning.examAccuracyByJlpt()
    val gap = learning.studyVsExamGap()

    data class Rec(val title: String, val detail: String, val icon: ImageVector, val action: () -> Unit)
    val recs = buildList {
        if (byType.isNotEmpty()) {
            val weakest = byType.minByOrNull { it.value }!!
            val examType = examTypeForQuestionType(weakest.key)
            add(
                Rec(
                    title = "Weakest skill",
                    detail = "${questionTypeShortLabel(weakest.key)} · ${(weakest.value * 100).toInt()}% correct",
                    icon = Icons.Default.Lightbulb,
                    action = { onStart(examType, null) }
                )
            )
        }
        if (byJlpt.isNotEmpty()) {
            val weakest = byJlpt.minByOrNull { it.value }!!
            add(
                Rec(
                    title = "Weakest band",
                    detail = "JLPT N${weakest.key} · ${(weakest.value * 100).toInt()}% correct",
                    icon = Icons.Default.School,
                    action = { onStart(ExamType.MixedJlpt, weakest.key) }
                )
            )
        }
        if (gap.studyAccuracy > 0f && gap.examProductionAccuracy > 0f && gap.studyAccuracy - gap.examProductionAccuracy > 0.15f) {
            add(
                Rec(
                    title = "Production gap",
                    detail = "You recognize material ${((gap.studyAccuracy - gap.examProductionAccuracy) * 100).toInt()}pt better than you produce it",
                    icon = Icons.Default.Create,
                    action = { onStart(ExamType.MixedJlpt, null) }
                )
            )
        }
    }
    if (recs.isEmpty()) {
        DsCard {
            Row(Modifier.padding(DsSpacing.Lg), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = successColor(), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(DsSpacing.Sm))
                Text(
                    "No weak areas detected — your measured exam accuracy is solid. Keep it up.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }
        }
        return
    }

    DsCard {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(title = "Recommended for you", subtitle = "From your real exam results")
            recs.forEach { rec ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(sc.surfaceInteractive.copy(alpha = 0.45f))
                        .padding(DsSpacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent().primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(rec.icon, contentDescription = null, tint = accent().primary, modifier = Modifier.size(17.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(rec.title, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                        Text(rec.detail, color = sc.textMuted, fontSize = DsType.Caption)
                    }
                    DsButton(text = "Practice", kind = DsButtonKind.Secondary, compact = true, onClick = rec.action)
                }
            }
        }
    }
}

private fun examTypeForQuestionType(label: String): ExamType = when {
    label.contains("grammar:pattern") -> ExamType.GrammarStructure
    label.contains("cloze") || label.contains("grammar") -> ExamType.GrammarUsage
    label.contains("writing") -> ExamType.VocabProduction
    label.contains("reading") -> ExamType.VocabReading
    label.contains("meaning") -> ExamType.VocabMeaning
    else -> ExamType.MixedJlpt
}

/** Short human label for an exam question-type label (stats keys). */
private fun questionTypeShortLabel(label: String): String = when {
    label.contains("pattern") -> "Pattern"
    label.contains("cloze") -> "Cloze"
    label.contains("writing") -> "Writing"
    label.contains("reading") -> "Reading"
    label.contains("multiple") -> "Multiple-select"
    label.contains("matching") -> "Matching"
    else -> "Meaning"
}

@Composable
private fun ExamTypeSelect(selected: ExamType, onSelected: (ExamType) -> Unit) {
    val sc = surfaceColors()
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
        ExamType.entries.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) accent().primary.copy(alpha = 0.25f) else sc.surfaceInteractive)
                    .clickable { onSelected(option) }
                    .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
            ) {
                Text(
                    option.label,
                    color = if (active) accent().primary else sc.textSecondary,
                    fontSize = DsType.Caption,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ============================================
// EXAM TAKING
// ============================================

/** A question plus the section it belongs to. */
private data class QEntry(val question: ExamQuestion, val section: ua.syt0r.kanji.desktop.engine.learning.ExamSection, val sectionIndex: Int)

@Composable
private fun ExamTakingScreen(state: AppState, draft: ExamDraft, onFinish: (ExamResult) -> Unit, onCancel: () -> Unit) {
    val learning = state.learning
    val sc = surfaceColors()
    val entries = remember(draft) {
        draft.sections.flatMapIndexed { si, s -> s.questions.map { q -> QEntry(q, s, si) } }
    }
    var index by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    var typedAnswer by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var multiSelected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confidence by remember { mutableStateOf(2) }
    var questionStart by remember { mutableStateOf(Clock.System.now()) }
    var sectionElapsedSec by remember { mutableStateOf(0L) }
    val answers = remember { mutableStateMapOf<String, ExamAnswer>() }
    val startedAt = remember { Clock.System.now() }

    val current = entries.getOrNull(index)
    if (current == null || finished) {
        val result = learning.exams.evaluate(draft, answers.toMap(), startedAt)
        onFinish(result)
        return
    }

    val question = current.question
    val section = current.section
    val sectionStart = entries.indexOfFirst { it.sectionIndex == current.sectionIndex }
    val sectionEnd = entries.indexOfLast { it.sectionIndex == current.sectionIndex }
    val sectionProgress = index - sectionStart + 1
    val isLast = index == entries.lastIndex

    val indexState = rememberUpdatedState(index)
    val finishedState = rememberUpdatedState(finished)

    // Section countdown — each timed section auto-advances when its clock hits
    // zero: unanswered questions are marked skipped, then the next section
    // starts (or the exam finishes).
    LaunchedEffect(current.sectionIndex) {
        sectionElapsedSec = 0
        val limitSec = section.timeLimitMs / 1000
        if (limitSec <= 0) return@LaunchedEffect
        var elapsed = 0L
        while (elapsed < limitSec && !finishedState.value) {
            delay(1000)
            elapsed++
            sectionElapsedSec = elapsed
        }
        if (elapsed >= limitSec && !finishedState.value) {
            val idx = indexState.value
            for (i in idx..sectionEnd) {
                val q = draft.questions[i]
                if (answers[q.id] == null) {
                    answers[q.id] = ExamAnswer(q.id, skipped = true)
                }
            }
            val next = sectionEnd + 1
            if (next >= entries.size) {
                finished = true
                onFinish(learning.exams.evaluate(draft, answers.toMap(), startedAt))
            } else {
                index = next
            }
        }
    }

    // Reset per-question input state.
    LaunchedEffect(index) {
        revealed = false
        typedAnswer = ""
        selectedOption = null
        multiSelected = emptySet()
        confidence = 2
        questionStart = Clock.System.now()
    }

    val advance: () -> Unit = {
        val answer = when {
            question.options.isNotEmpty() && question.questionType == ExamQuestionType.MultipleSelect ->
                multiSelected.sorted().joinToString("|")
            question.options.isNotEmpty() -> selectedOption.orEmpty()
            else -> typedAnswer
        }
        val elapsedMs = (Clock.System.now() - questionStart).inWholeMilliseconds
        answers[question.id] = ExamAnswer(
            questionId = question.id,
            answer = answer,
            confidence = confidence,
            skipped = answer.isBlank(),
            responseTimeMs = elapsedMs
        )
        if (isLast) {
            finished = true
            onFinish(learning.exams.evaluate(draft, answers.toMap(), startedAt))
        } else {
            index++
        }
    }

    val skipCurrent: () -> Unit = {
        answers[question.id] = ExamAnswer(question.id, skipped = true)
        if (isLast) {
            finished = true
            onFinish(learning.exams.evaluate(draft, answers.toMap(), startedAt))
        } else {
            index = (index + 1).coerceAtMost(entries.lastIndex)
        }
    }

    val remainingSec = if (section.timeLimitMs > 0) (section.timeLimitMs / 1000 - sectionElapsedSec).toInt().coerceAtLeast(0) else -1

    Column(
        Modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (keyEvent.key) {
                    Key.Enter -> {
                        if (revealed || question.options.isNotEmpty()) advance()
                        true
                    }
                    Key.R -> { revealed = true; true }
                    Key.S -> { skipCurrent(); true }
                    Key.One, Key.Two, Key.Three, Key.Four, Key.Five,
                    Key.Six, Key.Seven, Key.Eight, Key.Nine, Key.Zero -> {
                        val idx = when (keyEvent.key) {
                            Key.One -> 0
                            Key.Two -> 1
                            Key.Three -> 2
                            Key.Four -> 3
                            Key.Five -> 4
                            Key.Six -> 5
                            Key.Seven -> 6
                            Key.Eight -> 7
                            Key.Nine -> 8
                            else -> 9 // Key.Zero
                        }
                        val options = question.options
                        if (options.isNotEmpty() && question.questionType != ExamQuestionType.MultipleSelect && idx < options.size) {
                            selectedOption = options[idx]
                            true
                        } else if (options.isNotEmpty() && idx < options.size) {
                            val opt = options[idx]
                            multiSelected = if (opt in multiSelected) multiSelected - opt else multiSelected + opt
                            true
                        } else false
                    }
                    else -> false
                }
            },
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Text(draft.title, color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            DsBadge(text = "${index + 1} / ${entries.size}")
            if (draft.sections.size > 1) {
                DsBadge(text = "Section ${current.sectionIndex + 1}/${draft.sections.size}", tint = accent().primary)
            }
            if (remainingSec >= 0) {
                DsBadge(
                    text = if (remainingSec > 0) "${remainingSec / 60}m ${(remainingSec % 60).toString().padStart(2, '0')}s" else "Time's up",
                    tint = if (remainingSec < 60) errorColor() else sc.textMuted
                )
            }
        }

        // Section header — label, per-section progress, intro on first question.
        if (draft.sections.size > 1) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text(
                    section.label.uppercase(),
                    color = accent().primary,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                DsProgressBar(
                    fraction = sectionProgress.toFloat() / section.questions.size.coerceAtLeast(1),
                    modifier = Modifier.weight(1f)
                )
                Text("$sectionProgress / ${section.questions.size}", color = sc.textMuted, fontSize = DsType.Caption)
            }
            if (index == sectionStart && section.intro.isNotBlank()) {
                Text(section.intro, color = sc.textMuted, fontSize = DsType.Caption)
            }
        }

        DsProgressBar(fraction = (index + 1).toFloat() / entries.size.coerceAtLeast(1))

        DsCard(modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(question.prompt, color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    DsBadge(text = questionTypeShortLabel(question.questionType.label), tint = infoColor())
                }
                ExamQuestionInput(
                    question = question,
                    typedAnswer = typedAnswer,
                    onTyped = { typedAnswer = it },
                    selectedOption = selectedOption,
                    onSelectOption = { selectedOption = it },
                    multiSelected = multiSelected,
                    onToggleMulti = { selectedOption ->
                        multiSelected = if (selectedOption in multiSelected) multiSelected - selectedOption else multiSelected + selectedOption
                    }
                )
                if (revealed) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(DsRadius.Md)).background(sc.surfaceInteractive.copy(alpha = 0.5f)).padding(DsSpacing.Md)) {
                        Text("Answer: ${question.correctAnswer}", color = successColor(), fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    Text("Confidence", color = sc.textSecondary, fontSize = DsType.Caption)
                    Slider(value = confidence.toFloat(), onValueChange = { confidence = it.toInt() }, valueRange = 1f..3f, steps = 1, modifier = Modifier.width(160.dp))
                    Text(when (confidence) { 1 -> "Unsure"; 2 -> "Fairly sure"; else -> "Very sure" }, color = sc.textPrimary, fontSize = DsType.Caption)
                    Spacer(Modifier.weight(1f))
                    if (question.options.isNotEmpty() && question.questionType != ExamQuestionType.MultipleSelect) {
                        Text("1–9 select", color = sc.textMuted, fontSize = DsType.Caption)
                    }
                    DsTextButton(text = "Reveal (R)", onClick = { revealed = true })
                    DsTextButton(text = "Skip (S)", onClick = skipCurrent)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
            DsTextButton(text = "Cancel exam", onClick = onCancel)
            Spacer(Modifier.weight(1f))
            val answer = when {
                question.options.isNotEmpty() && question.questionType == ExamQuestionType.MultipleSelect ->
                    multiSelected.sorted().joinToString("|")
                question.options.isNotEmpty() -> selectedOption.orEmpty()
                else -> typedAnswer
            }
            DsButton(
                text = if (isLast) "Finish (Enter)" else "Next (Enter)",
                icon = if (isLast) Icons.Default.Check else Icons.Default.PlayArrow,
                enabled = !answer.isBlank() || question.options.isNotEmpty() || revealed,
                onClick = advance
            )
        }
    }
}

@Composable
private fun ExamQuestionInput(
    question: ExamQuestion,
    typedAnswer: String,
    onTyped: (String) -> Unit,
    selectedOption: String?,
    onSelectOption: (String) -> Unit,
    multiSelected: Set<String>,
    onToggleMulti: (String) -> Unit
) {
    val sc = surfaceColors()
    when {
        question.options.isEmpty() -> {
            DsTextField(
                value = typedAnswer,
                onValueChange = onTyped,
                placeholder = "Type your answer (kana/kanji)",
                modifier = Modifier.fillMaxWidth()
            )
        }
        question.questionType == ExamQuestionType.MultipleSelect -> {
            Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                question.options.forEach { option ->
                    val selected = option in multiSelected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DsRadius.Md))
                            .background(if (selected) accent().primary.copy(alpha = 0.18f) else sc.surfaceInteractive.copy(alpha = 0.5f))
                            .clickable { onToggleMulti(option) }
                            .padding(DsSpacing.Md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (selected) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (selected) successColor() else sc.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(DsSpacing.Sm))
                        Text(option, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                question.options.forEachIndexed { optionIndex, option ->
                    val selected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DsRadius.Md))
                            .background(if (selected) accent().primary.copy(alpha = 0.18f) else sc.surfaceInteractive.copy(alpha = 0.5f))
                            .clickable { onSelectOption(option) }
                            .padding(DsSpacing.Md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(22.dp).clip(RoundedCornerShape(11.dp))
                                .background(if (selected) accent().primary else sc.textMuted.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = accent().onPrimary, modifier = Modifier.size(13.dp))
                            } else {
                                Text("${optionIndex + 1}", color = sc.textMuted, fontSize = DsType.Caption)
                            }
                        }
                        Spacer(Modifier.width(DsSpacing.Sm))
                        Text(option, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

// ============================================
// EXAM RESULTS
// ============================================

@Composable
private fun ExamResultScreen(
    state: AppState,
    result: ExamResult,
    draft: ExamDraft?,
    onDone: () -> Unit,
    onRetake: () -> Unit
) {
    val sc = surfaceColors()
    var showReview by remember { mutableStateOf(false) }
    val sections = result.questions.groupBy { it.section }.filterKeys { it.isNotBlank() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsCard {
            Column(Modifier.padding(DsSpacing.Lg).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text(result.title, color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold)
                Text(
                    "${result.percentage}%",
                    color = when {
                        result.percentage >= 80 -> successColor()
                        result.percentage >= 60 -> warningColor()
                        else -> errorColor()
                    },
                    fontSize = DsType.Display,
                    fontWeight = FontWeight.Bold
                )
                Text("${result.correctCount} / ${result.questionCount} correct · ${result.skippedCount} skipped", color = sc.textSecondary, fontSize = DsType.Body)
                Text(
                    "Time: ${result.durationMs / 60000}m ${(result.durationMs % 60000) / 1000}s",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    DsStatTile("Recognition", "${(result.recognitionAccuracy * 100).toInt()}%", Modifier.weight(1f))
                    DsStatTile("Production", "${(result.productionAccuracy * 100).toInt()}%", Modifier.weight(1f))
                    DsStatTile("Writing", "${(result.writingAccuracy * 100).toInt()}%", Modifier.weight(1f))
                }
                Spacer(Modifier.height(DsSpacing.Md))
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(text = resolveSuiteString { takeAgain }, icon = Icons.Default.Refresh, kind = DsButtonKind.Secondary, onClick = onRetake)
                    DsButton(text = resolveSuiteString { doneButton }, onClick = onDone)
                }
            }
        }

        if (sections.isNotEmpty()) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsSectionHeader(title = "By section", subtitle = "How each timed section went")
                    sections.forEach { (label, qs) ->
                        val correct = qs.count { it.correct }
                        val accuracy = correct.toFloat() / qs.size
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(110.dp), fontWeight = FontWeight.Medium)
                            DsProgressBar(
                                fraction = accuracy,
                                modifier = Modifier.weight(1f),
                                color = if (accuracy >= 0.7f) successColor() else if (accuracy >= 0.5f) warningColor() else errorColor()
                            )
                            Spacer(Modifier.width(DsSpacing.Sm))
                            Text("$correct/${qs.size} · ${(accuracy * 100).toInt()}%", color = sc.textMuted, fontSize = DsType.Caption)
                        }
                    }
                }
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DsSectionHeader(title = "Answers", subtitle = "Every question, your answer vs the correct one")
                    Spacer(Modifier.weight(1f))
                    DsTextButton(text = if (showReview) "Hide review" else "Review answers", onClick = { showReview = !showReview })
                }
                if (showReview) {
                    val zipped = draft?.questions?.zip(result.questions) ?: emptyList()
                    if (zipped.isEmpty()) {
                        Text("Question details unavailable for this exam.", color = sc.textMuted, fontSize = DsType.Body)
                    } else {
                        zipped.forEach { (q, r) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(DsRadius.Md))
                                    .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                                    .padding(DsSpacing.Md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (r.correct) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (r.correct) successColor() else errorColor(),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(DsSpacing.Sm))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(q.prompt.replace("\n", " "), color = sc.textPrimary, fontSize = DsType.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                                        Text(
                                            if (r.correct) "Correct: ${r.correctAnswer}" else "Yours: ${r.answer.ifBlank { "—" }} · Correct: ${r.correctAnswer}",
                                            color = if (r.correct) successColor() else errorColor(),
                                            fontSize = DsType.Caption
                                        )
                                        if (r.section.isNotBlank()) {
                                            DsBadge(text = r.section, tint = sc.textMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Reveal every question with your answer and the correct one — a built-in review pass for the mistakes you just made.",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
        }
    }
}

// ============================================
// EXAM HISTORY
// ============================================

@Composable
private fun ExamHistoryScreen(state: AppState) {
    val sc = surfaceColors()
    val learning = state.learning
    val history = learning.examHistory(30)
    if (history.isEmpty()) {
        DsCard {
            DsEmptyState(title = "No exams taken", message = "Take an exam and every result lands here — nothing is fabricated.", icon = Icons.Default.School)
        }
        return
    }
    val trend = learning.examTrend(20)
    val byExamType = learning.examAccuracyByExamType()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(title = "Score trend", subtitle = "Your last ${trend.size} exams — progress over time")
                Spacer(Modifier.height(DsSpacing.Lg))
                ScoreTrendChart(trend)
            }
        }

        if (byExamType.isNotEmpty()) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "By exam type", subtitle = "Where your score is strongest and weakest")
                    Spacer(Modifier.height(DsSpacing.Md))
                    byExamType.entries.sortedByDescending { it.value }.forEach { (typeName, accuracy) ->
                        val label = ExamType.entries.firstOrNull { it.name == typeName }?.label ?: typeName
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = DsSpacing.Xs)) {
                            Text(label, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(170.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            DsProgressBar(
                                fraction = accuracy,
                                modifier = Modifier.weight(1f),
                                color = if (accuracy >= 0.7f) successColor() else if (accuracy >= 0.5f) warningColor() else errorColor()
                            )
                            Spacer(Modifier.width(DsSpacing.Sm))
                            Text("${(accuracy * 100).toInt()}%", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(44.dp))
                        }
                    }
                }
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsSectionHeader(title = "Exam log", subtitle = "Every completed exam")
                history.forEach { result ->
                    Row(Modifier.padding(DsSpacing.Xs).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(result.title, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${result.finishedAt.toString().take(16).replace('T', ' ')} · ${result.correctCount}/${result.questionCount} · ${(result.durationMs / 1000).toInt()}s" +
                                    if (result.questions.any { it.section.isNotBlank() }) " · ${result.questions.map { it.section }.distinct().joinToString(" + ")}" else "",
                                color = sc.textMuted,
                                fontSize = DsType.Caption
                            )
                        }
                        DsBadge(text = "${result.percentage}%", tint = if (result.percentage >= 60) successColor() else errorColor())
                    }
                }
            }
        }
    }
}

/** Simple bar chart of exam scores, colored by band. */
@Composable
private fun ScoreTrendChart(trend: List<Pair<String, Int>>) {
    val sc = surfaceColors()
    if (trend.isEmpty()) {
        Text("No exam history yet.", color = sc.textMuted, fontSize = DsType.Body)
        return
    }
    val maxScore = trend.maxOf { it.second }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        trend.forEach { (date, score) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((score.toFloat() / maxScore * 120).dp.coerceAtLeast(6.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (score >= 80) successColor()
                            else if (score >= 60) warningColor()
                            else errorColor()
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (trend.size <= 12) date.takeLast(5) else "",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
    }
}

// ============================================
// EXAM ANALYTICS
// ============================================

@Composable
private fun ExamAnalyticsScreen(state: AppState) {
    val sc = surfaceColors()
    val learning = state.learning
    val aggregates = learning.examAggregates()
    val gap: StudyVsExamGap = learning.studyVsExamGap()
    val byType = learning.examAccuracyByType()
    val byJlpt = learning.examAccuracyByJlpt()
    val bySection = learning.examAccuracyBySection()

    if (aggregates.count == 0) {
        DsCard {
            DsEmptyState(
                title = "No exam analytics yet",
                message = "Once you take exams, accuracy by question type, JLPT band and the study-vs-exam gap appear here.",
                icon = Icons.Default.School
            )
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            DsStatTile("Exams taken", aggregates.count.toString(), Modifier.weight(1f))
            DsStatTile("Avg score", "${aggregates.averageScore}%", Modifier.weight(1f))
            DsStatTile("Best", "${aggregates.bestScore}%", Modifier.weight(1f))
            DsStatTile("Worst", "${aggregates.worstScore}%", Modifier.weight(1f))
            DsStatTile("Questions", aggregates.totalQuestions.toString(), Modifier.weight(1f))
            DsStatTile("Exam time", "${aggregates.totalTimeMs / 3600000}h ${(aggregates.totalTimeMs % 3600000) / 60000}m", Modifier.weight(1f))
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(title = "Study vs exam gap", subtitle = "Recognition vs production — where you actually are")
                Spacer(Modifier.height(DsSpacing.Lg))
                GapBar("Flashcard study", gap.studyAccuracy)
                GapBar("Exam recognition", gap.examRecognitionAccuracy)
                GapBar("Exam production", gap.examProductionAccuracy)
                GapBar("Exam writing", gap.examWritingAccuracy)
                Spacer(Modifier.height(DsSpacing.Sm))
                val productionGap = gap.studyAccuracy - gap.examProductionAccuracy
                Text(
                    if (productionGap > 0.15f) "You recognize this material well but production is measurably weaker (${(productionGap * 100).toInt()}pt gap)."
                    else "Recognition and production are close — keep it that way.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }

        if (bySection.isNotEmpty()) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Accuracy by section", subtitle = "Vocabulary · Grammar · Reading — from real exam answers")
                    Spacer(Modifier.height(DsSpacing.Md))
                    bySection.entries.sortedByDescending { it.value }.forEach { (section, accuracy) ->
                        GapBar(section, accuracy)
                    }
                }
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(title = "Accuracy by question type", subtitle = "From real exam answers")
                Spacer(Modifier.height(DsSpacing.Md))
                if (byType.isEmpty()) {
                    Text("No question data yet.", color = sc.textMuted, fontSize = DsType.Body)
                } else {
                    byType.entries.sortedByDescending { it.value }.forEach { (type, accuracy) ->
                        GapBar(questionTypeShortLabel(type), accuracy)
                    }
                }
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(title = "Accuracy by JLPT band", subtitle = "From real exam answers")
                Spacer(Modifier.height(DsSpacing.Md))
                if (byJlpt.isEmpty()) {
                    Text("No JLPT-tagged exam questions yet.", color = sc.textMuted, fontSize = DsType.Body)
                } else {
                    (5 downTo 1).forEach { level ->
                        val accuracy = byJlpt[level]
                        if (accuracy != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = DsSpacing.Xs)) {
                                Text("N$level", color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp))
                                DsProgressBar(fraction = accuracy, modifier = Modifier.weight(1f), color = jlptColor(level))
                                Spacer(Modifier.width(DsSpacing.Sm))
                                Text("${(accuracy * 100).toInt()}%", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(44.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GapBar(label: String, accuracy: Float) {
    val sc = surfaceColors()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = DsSpacing.Xs)) {
        Text(label, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(150.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        DsProgressBar(fraction = accuracy, modifier = Modifier.weight(1f), color = if (accuracy >= 0.7f) successColor() else if (accuracy >= 0.5f) warningColor() else errorColor())
        Spacer(Modifier.width(DsSpacing.Sm))
        Text("${(accuracy * 100).toInt()}%", color = sc.textPrimary, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(44.dp))
    }
}

@Composable
private fun jlptColor(level: Int): Color = when (level) {
    5 -> successColor()
    4 -> androidx.compose.ui.graphics.Color(0xFF42A5F5)
    3 -> warningColor()
    2 -> androidx.compose.ui.graphics.Color(0xFFFF7043)
    else -> errorColor()
}
