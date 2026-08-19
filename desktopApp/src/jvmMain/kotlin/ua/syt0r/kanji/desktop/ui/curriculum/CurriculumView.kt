package ua.syt0r.kanji.desktop.ui.curriculum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsToolbar
import ua.syt0r.kanji.desktop.designsystem.DsToolbarDivider
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.curriculum.CurriculumCourse
import ua.syt0r.kanji.desktop.engine.curriculum.CurriculumLessonStatus
import ua.syt0r.kanji.desktop.engine.curriculum.CurriculumObjectiveStatus
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString

/**
 * The Curriculum workspace: structured courses (kana foundation, JLPT paths)
 * whose objectives are measured against real study data. Shows the active
 * lesson, every objective's live progress, and the single next objective to
 * focus on.
 */
@Composable
fun CurriculumView(state: AppState, modifier: Modifier = Modifier) {
    val engine = state.curriculum
    val sc = surfaceColors()

    // Re-measure objectives whenever study data changes (reviews, mining,
    // imports) and auto-advance when a lesson completes.
    LaunchedEffect(state.reviewLog.size, state.cards.size) {
        engine.refresh()
        engine.advanceIfComplete()
    }

    Column(modifier.fillMaxSize()) {
        DsToolbar(
            title = resolveSuiteString { curriculumTitle },
            subtitle = resolveSuiteString { curriculumSubtitle }
        )
        DsToolbarDivider()

        val active = engine.activeCourse()
        if (active == null) {
            CoursePicker(engine, modifier = Modifier.weight(1f))
        } else {
            ActiveCourseContent(engine, active, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CoursePicker(engine: ua.syt0r.kanji.desktop.engine.curriculum.CurriculumEngine, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = DsSpacing.Lg,
            vertical = DsSpacing.Lg
        ),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        item {
            Text(
                text = resolveSuiteString { noActiveCourse },
                color = sc.textSecondary,
                fontSize = DsType.BodyLarge
            )
            Spacer(Modifier.height(DsSpacing.Md))
        }
        items(engine.courses, key = { it.id }) { course ->
            CourseCard(
                course = course,
                completion = engine.courseCompletion(course),
                onClick = { engine.startCourse(course.id) }
            )
        }
    }
}

@Composable
private fun CourseCard(
    course: CurriculumCourse,
    completion: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()

    DsCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DsSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = course.title,
                    color = sc.textPrimary,
                    fontSize = DsType.BodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = course.description,
                    color = sc.textMuted,
                    fontSize = DsType.Body,
                    lineHeight = 18.sp
                )
                Text(
                    text = "${course.lessons.size} lessons · ${(completion * 100).toInt()}% complete",
                    color = sc.textSecondary,
                    fontSize = DsType.Caption
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ProgressPill(fraction = completion)
                DsButton(
                    text = resolveSuiteString { startCourse },
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
private fun ActiveCourseContent(
    engine: ua.syt0r.kanji.desktop.engine.curriculum.CurriculumEngine,
    course: CurriculumCourse,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val activeLesson = engine.activeLesson()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = DsSpacing.Lg,
            vertical = DsSpacing.Lg
        ),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        item {
            // Course header + switch action.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = course.title,
                        color = sc.textPrimary,
                        fontSize = DsType.Title,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${resolveSuiteString { courseCompletion }}: ${(engine.courseCompletion(course) * 100).toInt()}%",
                        color = sc.textSecondary,
                        fontSize = DsType.Body
                    )
                }
                DsButton(
                    text = resolveSuiteString { switchCourse },
                    kind = DsButtonKind.Ghost,
                    compact = true,
                    onClick = { engine.exitCourse() }
                )
            }
        }

        // ---- Next objective card ---------------------------------
        val next = engine.nextObjective()
        if (next != null) {
            item {
                NextObjectiveCard(next)
            }
        }

        // ---- Lesson list ------------------------------------------
        item {
            Spacer(Modifier.height(DsSpacing.Sm))
            Text(
                text = resolveSuiteString { objectivesLabel },
                color = sc.textPrimary,
                fontSize = DsType.BodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(DsSpacing.Sm))
        }
        items(course.lessons, key = { it.id }) { lesson ->
            val status = engine.lessonStatus(lesson)
            LessonCard(
                status = status,
                isActive = lesson.id == activeLesson?.id,
                onClick = { engine.startLesson(lesson.id) }
            )
        }
    }
}

@Composable
private fun NextObjectiveCard(status: CurriculumObjectiveStatus) {
    val sc = surfaceColors()
    val ac = accent()

    DsCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Column(Modifier.padding(DsSpacing.Md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = resolveSuiteString { nextObjective }.uppercase(),
                color = ac.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = status.objective.label,
                color = sc.textPrimary,
                fontSize = DsType.BodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = progressLine(status),
                color = sc.textSecondary,
                fontSize = DsType.Body
            )
            ProgressBar(
                fraction = status.fraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DsSpacing.Xs)
            )
        }
    }
}

@Composable
private fun LessonCard(
    status: CurriculumLessonStatus,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()

    DsCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DsSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = status.lesson.title,
                    color = if (isActive) ac.primary else sc.textPrimary,
                    fontSize = DsType.BodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (status.isComplete) {
                    Text(
                        text = "✓ ${resolveSuiteString { lessonComplete }}",
                        color = ac.primary,
                        fontSize = DsType.Caption,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (status.lesson.description.isNotBlank()) {
                Text(
                    text = status.lesson.description,
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }
            ProgressBar(
                fraction = status.fraction,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${status.completedObjectives}/${status.totalObjectives} ${resolveSuiteString { objectivesLabel }}",
                color = sc.textMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val ac = accent()
    Box(
        modifier = modifier
            .height(6.dp)
            .fillMaxWidth()
    ) {
        // A tiny deterministic bar: filled portion in accent, rest in surface.
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val fillWidth = size.width * fraction.coerceIn(0f, 1f)
            drawRoundRect(color = sc.surfaceInteractive)
            drawRoundRect(
                color = ac.primary,
                size = androidx.compose.ui.geometry.Size(fillWidth, size.height)
            )
        }
    }
}

@Composable
private fun ProgressPill(fraction: Float) {
    val ac = accent()
    Text(
        text = "${(fraction * 100).toInt()}%",
        color = ac.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

private fun progressLine(status: CurriculumObjectiveStatus): String {
    val objective = status.objective
    val progressText = when (objective.kind) {
        ua.syt0r.kanji.desktop.engine.curriculum.CurriculumObjectiveKind.NewCardCount -> "cards in deck"
        ua.syt0r.kanji.desktop.engine.curriculum.CurriculumObjectiveKind.ReviewCount -> "cards reviewed"
        ua.syt0r.kanji.desktop.engine.curriculum.CurriculumObjectiveKind.TotalReviewCount -> "total reviews"
    }
    return if (!status.available) {
        "${objective.label} — ${resolveSuiteString { notAvailable }}"
    } else {
        "${status.progress} / ${objective.target} $progressText"
    }
}
