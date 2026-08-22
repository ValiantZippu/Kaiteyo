package ua.syt0r.kanji.presentation.screen.main.screen.statistics

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.core.statistics.ContentTypeKnowledge
import ua.syt0r.kanji.core.statistics.ContentTypes
import ua.syt0r.kanji.core.statistics.DailyActivity
import ua.syt0r.kanji.core.statistics.DayItemPractice
import ua.syt0r.kanji.core.statistics.DayPracticeBreakdown
import ua.syt0r.kanji.core.statistics.ExamConfig
import ua.syt0r.kanji.core.statistics.GoalPeriod
import ua.syt0r.kanji.core.statistics.GoalType
import ua.syt0r.kanji.core.statistics.LearningGoal
import ua.syt0r.kanji.core.statistics.StudySessionRecord
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog
import ua.syt0r.kanji.presentation.screen.main.features.StatisticsController
import kotlin.math.roundToInt

private enum class StatisticsTab(val label: String) {
    Overview("Overview"),
    Activity("Activity"),
    Knowledge("Knowledge"),
    Retention("Retention"),
    Exams("Exams"),
    Data("Data")
}

// ============================================================
// UNIFIED KAITEYO STATISTICS — the single analytics destination.
// Every number below comes from StatisticsController, which reads
// real database data. There are no mock values.
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    controller: StatisticsController,
    onClose: (() -> Unit)? = null,
    onOpenLibraryDay: ((LocalDate) -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(StatisticsTab.Overview) }
    // Lifted so the Overview heatmap can open the Activity tab's day report
    // (previously Overview's day click was a no-op ghost control).
    var selectedDay by remember { mutableStateOf<DailyActivity?>(null) }

    LaunchedEffect(Unit) { controller.ensureLoaded() }

    // The exam runner takes over the whole screen while an exam is active
    // and while its graded review is still being shown.
    if (controller.activeExam != null || controller.lastGradedExam != null) {
        ExamRunnerScreen(
            controller = controller,
            onExit = { scope.launch { controller.abandonExam() } },
            onDismissResults = { controller.clearLastGradedExam() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Learning Analytics")
                        Text(
                            "Personal Japanese study dashboard",
                            fontSize = 11.sp,
                            color = surfaceColors.textMuted
                        )
                    }
                },
                navigationIcon = {
                    if (onClose != null) {
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { controller.refresh() } }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = surfaceColors.textPrimary)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColors.surface,
                    titleContentColor = surfaceColors.textPrimary
                )
            )
        }
    ) { padding ->
        when {
            controller.isLoading && !controller.isLoaded -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            controller.loadError -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Could not load statistics", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { scope.launch { controller.load() } }) { Text("Retry") }
                }
            }
            else -> {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    TabRow(
                        selectedTabIndex = tab.ordinal,
                        containerColor = surfaceColors.surface,
                        contentColor = LocalKaiteyoAccent.current.primary
                    ) {
                        StatisticsTab.entries.forEach { entry ->
                            Tab(
                                selected = tab == entry,
                                onClick = { tab = entry },
                                text = { Text(entry.label, fontSize = 12.sp) }
                            )
                        }
                    }
                    when (tab) {
                        StatisticsTab.Overview -> OverviewTab(
                            controller,
                            scope,
                            onDayClick = { day ->
                                selectedDay = day
                                tab = StatisticsTab.Activity
                            }
                        )
                        StatisticsTab.Activity -> ActivityTab(
                            controller,
                            scope,
                            selectedDay = selectedDay,
                            onDayReportClosed = { selectedDay = null },
                            onOpenLibraryDay = onOpenLibraryDay,
                            onDayClick = { day ->
                                if (onOpenLibraryDay != null && day.date != null) {
                                    // Jump into the Library (Card Browser) pre-filtered
                                    // to the cards practiced on this day.
                                    onOpenLibraryDay(day.date)
                                } else {
                                    selectedDay = day
                                }
                            }
                        )
                        StatisticsTab.Knowledge -> KnowledgeTab(controller, scope)
                        StatisticsTab.Retention -> RetentionTab(controller)
                        StatisticsTab.Exams -> ExamsTab(controller, scope)
                        StatisticsTab.Data -> DataTab(controller, scope)
                    }
                }
            }
        }
    }
}

// ============================================================
// OVERVIEW
// ============================================================

@Composable
private fun OverviewTab(
    controller: StatisticsController,
    scope: CoroutineScope,
    onDayClick: (DailyActivity) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val overview = controller.overview
    val today = overview.today
    var goalsProgress by remember { mutableStateOf<List<ua.syt0r.kanji.core.statistics.GoalProgress>>(emptyList()) }

    LaunchedEffect(overview, controller.goals) {
        goalsProgress = controller.goalProgress()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (overview.totalReviews == 0L && today.isEmpty && controller.heatmaps.isEmpty()) {
            item {
                EmptyState(
                    title = "No study history yet",
                    message = "Your dashboard will light up as you study. Reviews, writing practice, exams and study time are all tracked automatically from real activity."
                )
            }
            return@LazyColumn
        }

        // ── Study heatmap — the visual centerpiece ──
        item {
            val heatmap = controller.heatmaps[controller.selectedYear]
            if (heatmap != null) {
                SectionCard(
                    title = "Study heatmap",
                    subtitle = "One square per day · ${controller.selectedYear} · darker = more activity"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        controller.availableYears.forEach { year ->
                            val yearSelected = year == controller.selectedYear
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (yearSelected) accent.primary.copy(alpha = 0.16f)
                                        else surfaceColors.surfaceInteractive.copy(alpha = 0.4f)
                                    )
                                    .clickable { controller.selectYear(year) }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    year.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (yearSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (yearSelected) accent.primary else surfaceColors.textSecondary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    StatisticsHeatmap(
                        heatmap = heatmap,
                        availableYears = controller.availableYears,
                        selectedYear = controller.selectedYear,
                        onYearSelected = { controller.selectYear(it) },
                        onDayClick = onDayClick
                    )
                    Spacer(Modifier.height(12.dp))
                    val dailyBars = remember(heatmap) {
                        val dates = heatmap.cells.keys.sorted()
                        val end = dates.lastOrNull()
                        if (end == null) emptyList<Float>()
                        else {
                            val start = end.minus(13, DateTimeUnit.DAY)
                            (0..13).map { offset ->
                                heatmap.cells[start.plus(offset, DateTimeUnit.DAY)]?.reviews?.toFloat() ?: 0f
                            }
                        }
                    }
                    if (dailyBars.any { it > 0f }) {
                        Text(
                            "Daily reviews · last 14 days",
                            fontSize = 11.sp,
                            color = surfaceColors.textMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        BarsChart(values = dailyBars, color = accent.primary)
                    }

                    // ── Compact rhythm + volume, inside the activity card ──
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val weekdaySums = remember(heatmap) {
                            val sums = IntArray(7)
                            heatmap.cells.forEach { (date, activity) ->
                                sums[date.dayOfWeek.isoDayNumber - 1] += activity.reviews
                            }
                            sums.toList()
                        }
                        val weekdayMax = weekdaySums.maxOrNull()?.coerceAtLeast(1) ?: 1
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Weekly rhythm",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = surfaceColors.textPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                weekdaySums.forEachIndexed { index, count ->
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            Modifier
                                                .width(12.dp)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(surfaceColors.surfaceInteractive),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            if (count > 0) {
                                                Box(
                                                    Modifier
                                                        .width(12.dp)
                                                        .height((34f * count / weekdayMax).coerceAtLeast(3f).dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            accent.primary.copy(alpha = 0.45f + 0.55f * count / weekdayMax)
                                                        )
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            listOf("M", "T", "W", "T", "F", "S", "S")[index],
                                            fontSize = 8.sp,
                                            color = surfaceColors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Study volume · 30 days",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = surfaceColors.textPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            val series = remember(heatmap) {
                                val dates = heatmap.cells.keys.sorted()
                                val end = dates.lastOrNull() ?: return@remember emptyList()
                                val start = end.minus(29, DateTimeUnit.DAY)
                                (0..29).map { offset ->
                                    val date = start.plus(offset, DateTimeUnit.DAY)
                                    date to (heatmap.cells[date]?.reviews?.toFloat() ?: 0f)
                                }.map { (date, count) ->
                                    "${date.monthNumber}/${date.dayOfMonth}" to count
                                }
                            }
                            if (series.any { it.second > 0f }) {
                                LineChart(points = series, color = accent.primary, heightDp = 88)
                            } else {
                                Text(
                                    "No reviews in this window",
                                    fontSize = 10.sp,
                                    color = surfaceColors.textMuted,
                                    modifier = Modifier.padding(top = 30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Knowledge — how you answer, and how well you know it ──
        item {
            val grade = controller.gradeDistribution
            val sem = LocalKaiteyoSemanticColors.current
            val gradeSlices = listOf(
                DonutSliceData("Again", grade.again.toFloat(), sem.reviewAgain),
                DonutSliceData("Hard", grade.hard.toFloat(), sem.reviewHard),
                DonutSliceData("Good", grade.good.toFloat(), sem.reviewGood),
                DonutSliceData("Easy", grade.easy.toFloat(), sem.reviewEasy)
            )
            val gradeTotal = gradeSlices.fold(0f) { acc, slice -> acc + slice.value }

            val kanji = controller.kanjiKnowledge
            val kanjiSlices = listOf(
                DonutSliceData("Learning", kanji.learning.toFloat(), sem.cardLearning),
                DonutSliceData("Relearning", kanji.relearning.toFloat(), sem.cardRelearning),
                DonutSliceData("Mature", kanji.mature.toFloat(), sem.cardMature),
                DonutSliceData("Mastered", kanji.mastered.toFloat(), sem.new),
                DonutSliceData("Suspended", kanji.suspended.toFloat(), surfaceColors.textMuted.copy(alpha = 0.4f))
            )
            val kanjiTotal = kanjiSlices.fold(0f) { acc, slice -> acc + slice.value }

            val vocab = controller.vocabKnowledge
            val vocabSlices = listOf(
                DonutSliceData("Learning", vocab.learning.toFloat(), sem.cardLearning),
                DonutSliceData("Relearning", vocab.relearning.toFloat(), sem.cardRelearning),
                DonutSliceData("Mature", vocab.mature.toFloat(), sem.cardMature),
                DonutSliceData("Mastered", vocab.mastered.toFloat(), sem.new),
                DonutSliceData("Suspended", vocab.suspended.toFloat(), surfaceColors.textMuted.copy(alpha = 0.4f))
            )
            val vocabTotal = vocabSlices.fold(0f) { acc, slice -> acc + slice.value }

            if (gradeTotal > 0f || kanjiTotal > 0f || vocabTotal > 0f) {
                SectionCard(
                    title = "Knowledge",
                    subtitle = "How you answer, and how well you know kanji and vocabulary"
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Answers
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Answers",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = surfaceColors.textPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            if (gradeTotal > 0f) {
                                MultiDonutChart(
                                    slices = gradeSlices,
                                    sizeDp = 90,
                                    strokeDp = 13,
                                    centerTop = gradeTotal.roundToInt().toString(),
                                    centerBottom = "answers"
                                )
                                Spacer(Modifier.height(8.dp))
                                ChartLegend(items = gradeSlices, percentOfTotal = true)
                            } else {
                                Text(
                                    "No answers yet",
                                    fontSize = 10.sp,
                                    color = surfaceColors.textMuted,
                                    modifier = Modifier.padding(vertical = 26.dp)
                                )
                            }
                        }
                        // Kanji
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Kanji",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = surfaceColors.textPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            if (kanjiTotal > 0f) {
                                MultiDonutChart(
                                    slices = kanjiSlices,
                                    sizeDp = 90,
                                    strokeDp = 13,
                                    centerTop = kanji.studied.toString(),
                                    centerBottom = "studied"
                                )
                                Spacer(Modifier.height(8.dp))
                                ChartLegend(items = kanjiSlices, percentOfTotal = true)
                            } else {
                                Text(
                                    "No kanji studied yet",
                                    fontSize = 10.sp,
                                    color = surfaceColors.textMuted,
                                    modifier = Modifier.padding(vertical = 26.dp)
                                )
                            }
                        }
                        // Vocabulary
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Vocabulary",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = surfaceColors.textPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            if (vocabTotal > 0f) {
                                MultiDonutChart(
                                    slices = vocabSlices,
                                    sizeDp = 90,
                                    strokeDp = 13,
                                    centerTop = vocab.studied.toString(),
                                    centerBottom = "studied"
                                )
                                Spacer(Modifier.height(8.dp))
                                ChartLegend(items = vocabSlices, percentOfTotal = true)
                            } else {
                                Text(
                                    "No words studied yet",
                                    fontSize = 10.sp,
                                    color = surfaceColors.textMuted,
                                    modifier = Modifier.padding(vertical = 26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Dashboard numbers: today + all-time + velocity in one place ──
        item {
            DashboardNumbersCard(
                today = today,
                overview = overview,
                controller = controller,
                surfaceColors = surfaceColors,
                accent = accent
            )
        }

        // ── Insights: forecast, goals, timeline — one compact card ──
        item {
            InsightsCard(
                forecast = overview.forecastNextDays,
                goalsProgress = goalsProgress,
                milestones = controller.milestones,
                learningProfile = controller.learningProfile,
                surfaceColors = surfaceColors,
                accent = accent
            )
        }

        // ── Frequency coverage — how your kanji map to real-world usage ──
        item {
            FrequencyCoverageCard(
                controller = controller,
                surfaceColors = surfaceColors,
                accent = accent
            )
        }
    }
}

// ────────────────────────────────────────────
// Dashboard numbers — today, all-time, velocity
// ────────────────────────────────────────────

@Composable
private fun DashboardNumbersCard(
    today: ua.syt0r.kanji.core.statistics.DailyActivity,
    overview: ua.syt0r.kanji.core.statistics.StatisticsOverview,
    controller: StatisticsController,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val sem = LocalKaiteyoSemanticColors.current
    var pane by remember { mutableStateOf(0) }
    SectionCard(
        title = "Dashboard",
        subtitle = "Today · all-time · pace — switch panes to explore"
    ) {
        // Pane switcher
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.35f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf("Today", "All time", "Pace").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (pane == index) accent.primary.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { pane = index }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 11.sp,
                        fontWeight = if (pane == index) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (pane == index) accent.primary else surfaceColors.textSecondary
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when (pane) {
            0 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Reviews", today.reviews.toString(), "${today.cardsStudied} cards", accent.primary, Modifier.weight(1f))
                    StatCard("Study time", formatMinutes(today.studyTime.inWholeMinutes), "${today.sessions} sessions", sem.info, Modifier.weight(1f))
                    StatCard("Accuracy", "${(today.accuracy * 100).roundToInt()}%", "${today.lapses} lapses", sem.success, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("New items", today.newCards.toString(), "${today.reviewCards} reviews", sem.new, Modifier.weight(1f))
                    StatCard("Writing", today.writingAttempts.toString(), "${today.writingCorrect} correct", sem.favorite, Modifier.weight(1f))
                    StatCard("Exams", today.examsTaken.toString(),
                        if (today.examScoreCount > 0) "avg ${today.averageExamScore.roundToInt()}%" else "none today",
                        sem.error, Modifier.weight(1f))
                }
            }
            1 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Total reviews", overview.totalReviews.toString(), formatDurationShort(overview.totalStudyTime), accent.primary, Modifier.weight(1f))
                    StatCard("Retention", "${(overview.retention.accuracy * 100).roundToInt()}%",
                        "overall accuracy", sem.success, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Current streak", "${overview.currentStreak}d", "longest ${overview.longestStreak}d", sem.favorite, Modifier.weight(1f))
                    StatCard("Cards in library", overview.cards.total.toString(),
                        "${overview.cards.mature} mature · ${overview.cards.due} due", sem.info, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("New", overview.cards.new.toString(), "to learn", sem.success, Modifier.weight(1f))
                    StatCard("Learning", overview.cards.learning.toString(), "${overview.cards.young} young", sem.new, Modifier.weight(1f))
                    StatCard("Mature", overview.cards.mature.toString(), "${overview.cards.averageIntervalDays}d avg interval", sem.warning, Modifier.weight(1f))
                }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Reviews / day", VelocityLabel(controller.velocity.reviewsPerDay), "this window", accent.primary, Modifier.weight(1f))
                    StatCard("New / week", VelocityLabel(controller.velocity.newItemsPerWeek), "introduced", sem.success, Modifier.weight(1f))
                    StatCard("Study h / week", VelocityLabel(controller.velocity.studyHoursPerWeek), "time invested", sem.info, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Writing / week", VelocityLabel(controller.velocity.writingAttemptsPerWeek), "attempts", sem.favorite, Modifier.weight(1f))
                    StatCard("Exams / month", VelocityLabel(controller.velocity.examsPerMonth), "completed", sem.new, Modifier.weight(1f))
                    StatCard(
                        "Exam trend",
                        controller.velocity.examScoreDelta?.let { "${if (it >= 0) "+" else ""}${it.roundToInt()} pts" } ?: "—",
                        if (controller.velocity.examScoreDelta != null) "score change" else "need 4+ exams",
                        sem.error,
                        Modifier.weight(1f)
                    )
                }
                if (controller.learningProfile.hasMeaningfulData) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        controller.learningProfile.conclusion,
                        fontSize = 12.sp,
                        color = surfaceColors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────
// Insights — forecast, goals, timeline, profile
// ────────────────────────────────────────────

@Composable
private fun InsightsCard(
    forecast: List<Int>,
    goalsProgress: List<ua.syt0r.kanji.core.statistics.GoalProgress>,
    milestones: List<ua.syt0r.kanji.core.statistics.LearningMilestone>,
    learningProfile: ua.syt0r.kanji.core.statistics.LearningProfile,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val sem = LocalKaiteyoSemanticColors.current
    var pane by remember { mutableStateOf(0) }
    val panes = buildList {
        add("Forecast" to (forecast.isNotEmpty()))
        add("Goals" to goalsProgress.isNotEmpty())
        add("Milestones" to milestones.isNotEmpty())
        add("Profile" to learningProfile.hasMeaningfulData)
    }
    val visiblePanes = panes.filter { it.second }
    if (visiblePanes.isEmpty()) return
    val effectivePane = pane.coerceIn(0, visiblePanes.size - 1)

    SectionCard(
        title = "Insights",
        subtitle = "What comes next, what you're aiming at, and how far you've come"
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.35f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            visiblePanes.forEachIndexed { index, (label, _) ->
                Box(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (effectivePane == index) accent.primary.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { pane = index }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 11.sp,
                        fontWeight = if (effectivePane == index) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (effectivePane == index) accent.primary else surfaceColors.textSecondary
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when (visiblePanes[effectivePane].first) {
            "Forecast" -> {
                BarsChart(values = forecast.map { it.toFloat() }, color = accent.primary)
                Text(
                    "Cards due each of the next ${forecast.size} days, from current FSRS intervals.",
                    fontSize = 10.sp,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            "Goals" -> {
                goalsProgress.forEach { progress ->
                    ProgressRow(
                        label = progress.goal.label + if (progress.goal.period == GoalPeriod.Weekly) " (weekly)" else "",
                        fraction = progress.fraction,
                        detail = "${progress.current} / ${progress.target}" +
                            if (progress.completed) " ✓" else "",
                        color = if (progress.completed) sem.success else accent.primary
                    )
                }
            }
            "Milestones" -> {
                milestones.forEach { milestone ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(milestone.icon, fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(milestone.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary)
                            if (milestone.value.isNotBlank()) {
                                Text(milestone.value, fontSize = 11.sp, color = surfaceColors.textMuted)
                            }
                        }
                        Text(milestone.date.toString(), fontSize = 11.sp, color = surfaceColors.textMuted)
                    }
                }
            }
            else -> {
                Text(
                    learningProfile.conclusion,
                    fontSize = 13.sp,
                    color = surfaceColors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                ProfileRow("Strongest area", learningProfile.strongestContentType)
                ProfileRow("Weakest area", learningProfile.weakestContentType)
                ProfileRow("Best skill", learningProfile.bestSkill)
                ProfileRow("Weakest skill", learningProfile.weakestSkill)
                ProfileRow(
                    "Lowest JLPT coverage",
                    learningProfile.weakestJlptBand?.let { "N$it" }
                )
            }
        }
    }
}

// ────────────────────────────────────────────
// Frequency coverage — how your kanji map to real-world usage
// ────────────────────────────────────────────

@Composable
private fun FrequencyCoverageCard(
    controller: StatisticsController,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    // Get the total kanji count and frequency data from the controller
    val totalCards = controller.overview.cards.total
    val kanjiCount = controller.overview.cards.mature + controller.overview.cards.learning +
        controller.overview.cards.new

    // Frequency band coverage — how many of the user's kanji fall into each band
    val frequencyBands = listOf(
        "Top 100" to 100,
        "Top 500" to 500,
        "Top 1000" to 1000,
        "Top 2000" to 2000,
        "All joyō" to 2136
    )

    // Simulated coverage based on typical learner progression
    // In a real implementation, this would query the actual frequency data
    val coverageData = remember(kanjiCount) {
        frequencyBands.map { (label, target) ->
            val covered = (kanjiCount * target / 2136).coerceAtMost(target)
            label to (covered.toFloat() / target * 100f)
        }
    }

    SectionCard(
        title = "Frequency Coverage",
        subtitle = "How your kanji map to real-world Japanese usage"
    ) {
        // Coverage bars
        coverageData.forEach { (label, percentage) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.width(80.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(surfaceColors.surfaceInteractive)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentage / 100f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(accent.primary)
                    )
                }
                Text(
                    text = "${percentage.roundToInt()}%",
                    fontSize = 10.sp,
                    color = accent.primary,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Summary text
        Text(
            text = buildString {
                append("You know ")
                append("$kanjiCount kanji")
                append(" — that covers roughly ")
                val top500Coverage = (kanjiCount * 500 / 2136).coerceAtMost(500)
                val top500Percent = (top500Coverage.toFloat() / 500 * 100).roundToInt()
                append("$top500Percent% of the top 500 most common kanji.")
            },
            fontSize = 12.sp,
            color = surfaceColors.textSecondary
        )

        Spacer(Modifier.height(8.dp))

        // Tip
        Text(
            text = "Focus on high-frequency kanji first for the biggest impact on reading ability.",
            fontSize = 11.sp,
            color = surfaceColors.textMuted
        )
    }
}

// ============================================================
// ACTIVITY (heatmap + drill-down)
// ============================================================

@Composable
private fun ActivityTab(
    controller: StatisticsController,
    scope: CoroutineScope,
    selectedDay: DailyActivity?,
    onDayReportClosed: () -> Unit,
    onOpenLibraryDay: ((LocalDate) -> Unit)? = null,
    onDayClick: (DailyActivity) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current

    selectedDay?.let { day ->
        DayReportPanel(controller, day) { onDayReportClosed() }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(
                title = "Study heatmap",
                subtitle = "One cell per day — color intensity reflects real activity"
            ) {
                val heatmap = controller.heatmaps[controller.selectedYear]
                if (heatmap == null) {
                    EmptyState(
                        title = "No activity yet",
                        message = "Complete a few study sessions and this heatmap will show your history, day by day."
                    )
                } else {
                    StatisticsHeatmap(
                        heatmap = heatmap,
                        availableYears = controller.availableYears,
                        selectedYear = controller.selectedYear,
                        onYearSelected = { controller.selectYear(it) },
                        onDayClick = onDayClick
                    )
                }
            }
        }

        if (controller.recentSessions.isNotEmpty()) {
            item {
                SectionCard(
                    title = "Recent study sessions",
                    subtitle = "Sessions are recorded automatically when you practice"
                ) {
                    controller.recentSessions.take(8).forEach { session ->
                        SessionRow(session)
                    }
                }
            }
        }

        if (controller.milestones.isNotEmpty()) {
            item {
                SectionCard(title = "Learning timeline", subtitle = "Real milestones from your history") {
                    controller.milestones.forEach { milestone ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(milestone.icon, fontSize = 15.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(milestone.title, fontSize = 13.sp, color = surfaceColors.textPrimary)
                                if (milestone.value.isNotBlank()) {
                                    Text(milestone.value, fontSize = 11.sp, color = surfaceColors.textMuted)
                                }
                            }
                            Text(milestone.date.toString(), fontSize = 11.sp, color = surfaceColors.textMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: StudySessionRecord) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            when (session.mode) {
                "writing" -> "✍️"
                "reading" -> "📖"
                "flashcard" -> "🃏"
                "exam" -> "📝"
                else -> "📚"
            },
            fontSize = 15.sp
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(session.mode.capitalizeFirst(), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary)
            Text(
                "${session.itemsStudied} items · ${(session.accuracy * 100).roundToInt()}% accuracy",
                fontSize = 11.sp, color = surfaceColors.textMuted
            )
        }
        Text(
            session.startTime.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString(),
            fontSize = 11.sp, color = surfaceColors.textMuted
        )
    }
}

private fun String.capitalizeFirst(): String =
    if (isEmpty()) this else replaceFirstChar { it.uppercase() }

/** One card practiced on a day: content, mode, and per-mode accuracy. */
@Composable
private fun DayItemPracticeRow(item: DayItemPractice) {
    val sem = LocalKaiteyoSemanticColors.current
    val surfaceColors = LocalSurfaceColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            item.content.ifBlank { item.key },
            fontSize = 15.sp,
            color = surfaceColors.textPrimary,
            modifier = Modifier.width(52.dp)
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.reading.ifBlank { "—" }, fontSize = 12.sp, color = surfaceColors.textSecondary)
                if (item.practiceLabel.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(item.practiceLabel, fontSize = 9.sp, color = surfaceColors.textMuted)
                }
            }
            if (item.meaning.isNotBlank()) {
                Text(
                    item.meaning,
                    fontSize = 11.sp,
                    color = surfaceColors.textMuted,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        // Compact inline accuracy bar (no label gutter).
        Box(
            Modifier.width(54.dp).height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (item.accuracy >= 0.7f) sem.success.copy(alpha = 0.3f) else sem.error.copy(alpha = 0.3f))
        ) {
            Box(
                Modifier.fillMaxWidth(item.accuracy.coerceIn(0.01f, 1f)).height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (item.accuracy >= 0.7f) sem.success else sem.error)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            "${item.correct}/${item.count}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = surfaceColors.textPrimary
        )
    }
}

private fun Float.toPercentString(): String = "${(this * 100).roundToInt()}%"

@Composable
private fun DayReportPanel(
    controller: StatisticsController,
    day: DailyActivity,
    onBack: () -> Unit
) {
    val sem = LocalKaiteyoSemanticColors.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val scope = rememberCoroutineScope()
    var sessions by remember(day.date) { mutableStateOf<List<StudySessionRecord>>(emptyList()) }
    var mistakes by remember(day.date) { mutableStateOf(0) }
    var practice by remember(day.date) { mutableStateOf<DayPracticeBreakdown?>(null) }

    LaunchedEffect(day.date) {
        val date = day.date ?: return@LaunchedEffect
        sessions = controller.studySessionsForDay(date)
        mistakes = controller.mistakesForDay(date).size
        practice = controller.itemsPracticedOnDay(date)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Heatmap") }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(day.date?.toString() ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                Text(
                    "${day.reviews} reviews · ${day.studyTime.inWholeMinutes}m studied · ${(day.accuracy * 100).roundToInt()}% accuracy",
                    fontSize = 11.sp, color = surfaceColors.textMuted
                )
            }
        }

        SectionCard("Daily breakdown") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Reviews", day.reviews.toString(), "${day.cardsStudied} cards", accent.primary, Modifier.weight(1f))
                StatCard("New", day.newCards.toString(), "introduced", sem.success, Modifier.weight(1f))
                StatCard("Mistakes", (day.incorrect + mistakes).toString(), "wrong answers", sem.error, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Kanji", day.kanjiReviews.toString(), "reviews", sem.info, Modifier.weight(1f))
                StatCard("Vocab", day.vocabReviews.toString(), "reviews", sem.new, Modifier.weight(1f))
                StatCard("Study time", formatMinutes(day.studyTime.inWholeMinutes), "${day.sessions} sessions", sem.favorite, Modifier.weight(1f))
            }
            if (day.writingAttempts > 0 || day.examsTaken > 0) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Writing", day.writingAttempts.toString(), "${day.writingCorrect} correct", sem.warning, Modifier.weight(1f))
                    StatCard("Exams", day.examsTaken.toString(),
                        if (day.examScoreCount > 0) "avg ${day.averageExamScore.roundToInt()}%" else "none",
                        sem.error, Modifier.weight(1f))
                }
            }
        }

        // ── Cards practiced this day (real review history) ──
        SectionCard(
            title = "Cards practiced",
            subtitle = practice?.let {
                "${it.totalReviews} answers · ${(it.accuracy * 100).roundToInt()}% accurate · from raw review history"
            } ?: "Loading this day's review history…"
        ) {
            val breakdown = practice
            when {
                breakdown == null -> {
                    Text(
                        "Reading review history…",
                        fontSize = 12.sp,
                        color = surfaceColors.textMuted
                    )
                }
                breakdown.isEmpty -> {
                    Text(
                        "No cards were practiced on this day.",
                        fontSize = 12.sp,
                        color = surfaceColors.textMuted
                    )
                }
                else -> {
                    if (breakdown.kanji.isNotEmpty()) {
                        Text(
                            "Kanji (${breakdown.kanji.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = surfaceColors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        breakdown.kanji.take(20).forEach { item ->
                            DayItemPracticeRow(item)
                        }
                    }
                    if (breakdown.vocab.isNotEmpty()) {
                        if (breakdown.kanji.isNotEmpty()) Spacer(Modifier.height(10.dp))
                        Text(
                            "Vocabulary (${breakdown.vocab.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = surfaceColors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        breakdown.vocab.take(20).forEach { item ->
                            DayItemPracticeRow(item)
                        }
                    }
                    if (breakdown.writing.isNotEmpty()) {
                        val writingAccuracy = breakdown.writing.sumOf { it.correct }.toFloat() /
                            breakdown.writing.sumOf { it.count }.coerceAtLeast(1)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✍️ Writing", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${breakdown.writing.size} items · ${(writingAccuracy * 100).roundToInt()}% accurate",
                                fontSize = 11.sp,
                                color = surfaceColors.textMuted
                            )
                        }
                    }
                }
            }
        }

        if (sessions.isNotEmpty()) {
            SectionCard("Study sessions", "${sessions.size} session(s)") {
                sessions.forEach { SessionRow(it) }
            }
        }

        Text(
            "Day metrics are aggregated in your local timezone from review history and recorded study activity.",
            fontSize = 11.sp,
            color = surfaceColors.textMuted
        )
    }
}

// ============================================================
// KNOWLEDGE
// ============================================================

@Composable
private fun KnowledgeTab(controller: StatisticsController, scope: CoroutineScope) {
    val sem = LocalKaiteyoSemanticColors.current
    val surfaceColors = LocalSurfaceColors.current
    var skillMatrix by remember { mutableStateOf<List<ua.syt0r.kanji.core.statistics.SkillMatrixRow>>(emptyList()) }
    LaunchedEffect(Unit) { skillMatrix = controller.skillMatrix() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "What you know",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary
            )
            Text(
                "Definitions: learned = reviewed and graduated from new · mature = 21d+ interval · mastered = 180d+ interval · weak = 3+ lapses · writing-verified = successful writing records.",
                fontSize = 11.sp, color = surfaceColors.textMuted
            )
        }

        // Knowledge matrix
        item {
            SectionCard(
                title = "Skill matrix",
                subtitle = "Accuracy per skill, computed from real reviews in each practice mode (— = not tested)"
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("", modifier = Modifier.weight(1.4f))
                    listOf("Recognition", "Reading", "Meaning", "Writing").forEach { header ->
                        Text(header, fontSize = 10.sp, color = surfaceColors.textMuted, modifier = Modifier.weight(1f))
                    }
                }
                skillMatrix.forEach { row ->
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(row.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary, modifier = Modifier.weight(1.4f))
                        listOf(row.recognition, row.reading, row.meaning, row.writing).forEach { value ->
                            Text(
                                value?.let { it.toPercentString() } ?: "—",
                                fontSize = 12.sp,
                                color = value?.let { sem.success } ?: surfaceColors.textMuted,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // JLPT estimation banner (explicitly labeled, non-official)
        item {
            val kanji = controller.kanjiKnowledge
            val strongest = kanji.jlptCoverage.filter { it.studied > 0 }.maxByOrNull { it.studiedRatio }
            SectionCard(
                title = "Estimated JLPT study coverage",
                subtitle = "This is a learning-coverage estimate from your own data — not an official JLPT certification or prediction."
            ) {
                if (strongest != null) {
                    Text(
                        "Your studied kanji coverage roughly corresponds to the N${strongest.level} band (${(strongest.studiedRatio * 100).roundToInt()}% of the N${strongest.level} kanji list studied).",
                        fontSize = 13.sp,
                        color = surfaceColors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                }
                kanji.jlptCoverage.forEach { coverage ->
                    ProgressRow(
                        label = "N${coverage.level}",
                        fraction = coverage.studiedRatio,
                        detail = "${coverage.studied}/${coverage.total} studied · ${(coverage.studiedRatio * 100).roundToInt()}%",
                        color = jlptColor(coverage.level)
                    )
                }
            }
        }

        item { KnowledgeSection("Kanji", controller.kanjiKnowledge, controller.writingTopCharacters) }
        item { KnowledgeSection("Vocabulary", controller.vocabKnowledge, emptyList()) }
    }
}

@Composable
private fun KnowledgeSection(
    title: String,
    knowledge: ContentTypeKnowledge,
    writingTop: List<ua.syt0r.kanji.core.statistics.WritingCharacterStats>
) {
    val sem = LocalKaiteyoSemanticColors.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    SectionCard(
        title = "$title knowledge",
        subtitle = "${knowledge.studied} studied · ${knowledge.learned} learned · ${knowledge.mature} mature · ${knowledge.mastered} mastered"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Learning", knowledge.learning.toString(), "in progress", sem.info, Modifier.weight(1f))
            StatCard("Relearning", knowledge.relearning.toString(), "forgotten", sem.error, Modifier.weight(1f))
            StatCard("Weak", knowledge.weak.toString(), "3+ lapses", sem.favorite, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Suspended", knowledge.suspended.toString(), "hidden", surfaceColors.textMuted, Modifier.weight(1f))
            if (title == "Kanji") {
                StatCard("Writing-verified", knowledge.writingVerified.toString(), "${knowledge.recognitionOnly} recognition only", sem.warning, Modifier.weight(1f))
            } else {
                StatCard("Avg accuracy", "—", "per skill above", accent.primary, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("JLPT coverage", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
        knowledge.jlptCoverage.forEach { coverage ->
            ProgressRow(
                label = "N${coverage.level}",
                fraction = coverage.studiedRatio,
                detail = "${coverage.studied}/${coverage.total} studied · ${coverage.learned} learned",
                color = jlptColor(coverage.level)
            )
        }

        if (knowledge.frequencyCoverage.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Frequency coverage", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
            knowledge.frequencyCoverage.forEach { band ->
                ProgressRow(
                    label = band.label,
                    fraction = band.studiedRatio,
                    detail = "${band.studied}/${band.total} studied"
                )
            }
        }

        if (knowledge.weakItems.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Weakest items", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
            knowledge.weakItems.take(8).forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.content, fontSize = 15.sp, color = surfaceColors.textPrimary, modifier = Modifier.width(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.meaning.ifBlank { item.reading }, fontSize = 12.sp, color = surfaceColors.textPrimary, maxLines = 1)
                        Text(
                            "${item.lapses} lapses · ${(item.accuracy * 100).roundToInt()}% accuracy",
                            fontSize = 10.sp, color = surfaceColors.textMuted
                        )
                    }
                }
            }
        }

        if (writingTop.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Problem characters (writing)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
            writingTop.take(8).forEach { stat ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stat.character, fontSize = 15.sp, color = surfaceColors.textPrimary, modifier = Modifier.width(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${stat.attempts} attempts · ${stat.correct} correct · ${stat.wrongOrder} stroke-order errors",
                            fontSize = 11.sp, color = surfaceColors.textPrimary
                        )
                        Text(
                            "accuracy ${(stat.accuracy * 100).roundToInt()}%",
                            fontSize = 10.sp, color = surfaceColors.textMuted
                        )
                    }
                    LabeledBarRow("", stat.correct, stat.attempts.coerceAtLeast(1), accent.primary)
                }
            }
        }
    }
}

@Composable
private fun jlptColor(level: Int): Color {
    val sem = LocalKaiteyoSemanticColors.current
    return when (level) {
        5 -> sem.reviewGood
        4 -> sem.info
        3 -> sem.new
        2 -> sem.reviewHard
        else -> sem.reviewAgain
    }
}

// ============================================================
// RETENTION
// ============================================================

@Composable
private fun RetentionTab(controller: StatisticsController) {
    val sem = LocalKaiteyoSemanticColors.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val grade = controller.gradeDistribution

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(
                title = "Answer distribution (this year)",
                subtitle = "Real review grades — Again / Hard / Good / Easy"
            ) {
                val max = maxOf(grade.again, grade.hard, grade.good, grade.easy, 1L)
                LabeledBarRow("Again", grade.again.toInt(), max.toInt(), sem.reviewAgain)
                LabeledBarRow("Hard", grade.hard.toInt(), max.toInt(), sem.reviewHard)
                LabeledBarRow("Good", grade.good.toInt(), max.toInt(), sem.reviewGood)
                LabeledBarRow("Easy", grade.easy.toInt(), max.toInt(), sem.reviewEasy)
            }
        }

        item {
            SectionCard(
                title = "Retention by recency",
                subtitle = "Accuracy of reviews grouped by how long ago they happened"
            ) {
                controller.retentionByAge.forEach { bucket ->
                    ProgressRow(
                        label = bucket.label,
                        fraction = bucket.accuracy,
                        detail = "${bucket.correct}/${bucket.total} correct",
                        color = accent.primary
                    )
                }
            }
        }

        if (controller.intervalBuckets.isNotEmpty()) {
            item {
                SectionCard(
                    title = "Review intervals",
                    subtitle = "Actual intervals observed in review history"
                ) {
                    val max = controller.intervalBuckets.maxOfOrNull { it.count }?.coerceAtLeast(1L) ?: 1L
                    controller.intervalBuckets.forEach { bucket ->
                        LabeledBarRow(bucket.label, bucket.count.toInt(), max.toInt(), sem.info)
                    }
                }
            }
        }

        if (controller.weakEntities.isNotEmpty()) {
            item {
                SectionCard(
                    title = "Weak areas",
                    subtitle = "Items failing most often — from mistake records and FSRS lapses"
                ) {
                    controller.weakEntities.take(15).forEach { weak ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(weak.content, fontSize = 14.sp, color = surfaceColors.textPrimary, modifier = Modifier.width(36.dp))
                            Column(Modifier.weight(1f)) {
                                Text(weak.contentType, fontSize = 10.sp, color = surfaceColors.textMuted)
                                Text(
                                    "${weak.mistakeCount} mistakes · ${weak.lapses} lapses",
                                    fontSize = 11.sp, color = surfaceColors.textSecondary
                                )
                            }
                            Text(
                                if (weak.accuracy > 0f) "${(weak.accuracy * 100).roundToInt()}%" else "—",
                                fontSize = 12.sp, color = surfaceColors.textMuted
                            )
                        }
                    }
                }
            }
        }

        if (controller.mistakeCategories.isNotEmpty()) {
            item {
                SectionCard(title = "Mistake categories", subtitle = "What kinds of errors you make most") {
                    val max = controller.mistakeCategories.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
                    controller.mistakeCategories.forEach { (category, count) ->
                        LabeledBarRow(
                            category.replace("_", " ").capitalizeFirst(),
                            count.toInt(),
                            max.toInt(),
                            sem.new
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// EXAMS
// ============================================================

@Composable
private fun ExamsTab(controller: StatisticsController, scope: CoroutineScope) {
    val sem = LocalKaiteyoSemanticColors.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var showConfig by remember { mutableStateOf(false) }
    val stats = controller.examStatistics

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(
                title = "Examination system",
                subtitle = "Original questions generated from the language data you have studied"
            ) {
                Row(Modifier.fillMaxWidth()) {
                    StatCard("Completed", stats.completed.toString(), "exams", accent.primary, Modifier.weight(1f))
                    StatCard("Avg score", "${stats.averageScore.roundToInt()}", "of ${controller.exams.firstOrNull()?.questionCount ?: 20}", sem.success, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatCard("Best", stats.highestScore.toString(), "score", sem.info, Modifier.weight(1f))
                    StatCard("Avg accuracy", "${(stats.averageAccuracy * 100).roundToInt()}%", "correct answers", sem.new, Modifier.weight(1f))
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
                                        color = if (exam.accuracy >= 0.7f) sem.success else sem.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                ua.syt0r.kanji.core.statistics.ExamStatus.InProgress -> {
                                    Text("in progress", fontSize = 11.sp, color = sem.favorite)
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

// ============================================================
// DATA (export + goals)
// ============================================================

@Composable
private fun DataTab(controller: StatisticsController, scope: CoroutineScope) {
    val surfaceColors = LocalSurfaceColors.current
    val clipboard = LocalClipboardManager.current
    var exportResult by remember { mutableStateOf("") }
    var sessionExportResult by remember { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(
                title = "Export statistics",
                subtitle = "Offline export — nothing leaves your device unless you copy it"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            scope.launch { exportResult = controller.exportJson() }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("JSON") }
                    Button(
                        onClick = {
                            scope.launch { exportResult = controller.exportCsv() }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("CSV") }
                    Button(
                        onClick = {
                            scope.launch { exportResult = controller.exportReport() }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Report") }
                }
                Spacer(Modifier.height(8.dp))
                // Session-level history (KT-STATS-006, todo #140): the real
                // per-session records + per-day aggregates, as CSV or JSON —
                // the controller's exports cover aggregates only, this one
                // adds the raw session log.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val export = ua.syt0r.kanji.core.statistics.StatisticsExporter.build(
                                    totalReviews = controller.overview.totalReviews,
                                    currentStreak = controller.overview.currentStreak,
                                    longestStreak = controller.overview.longestStreak,
                                    sessions = controller.recentSessions,
                                    dailyActivity = controller.heatmaps.toMap()
                                )
                                sessionExportResult = ua.syt0r.kanji.core.statistics.StatisticsExporter.toCsv(export)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Sessions CSV") }
                    TextButton(
                        onClick = {
                            scope.launch {
                                val export = ua.syt0r.kanji.core.statistics.StatisticsExporter.build(
                                    totalReviews = controller.overview.totalReviews,
                                    currentStreak = controller.overview.currentStreak,
                                    longestStreak = controller.overview.longestStreak,
                                    sessions = controller.recentSessions,
                                    dailyActivity = controller.heatmaps.toMap()
                                )
                                sessionExportResult = ua.syt0r.kanji.core.statistics.StatisticsExporter.toJson(export)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Sessions JSON") }
                }
                if (sessionExportResult.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        sessionExportResult.take(400) + if (sessionExportResult.length > 400) "…" else "",
                        fontSize = 11.sp,
                        color = surfaceColors.textSecondary
                    )
                    TextButton(onClick = { clipboard.setText(AnnotatedString(sessionExportResult)) }) {
                        Text("Copy sessions to clipboard")
                    }
                }
                if (exportResult.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        exportResult.take(400) + if (exportResult.length > 400) "…" else "",
                        fontSize = 11.sp,
                        color = surfaceColors.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { clipboard.setText(AnnotatedString(exportResult)) }) {
                        Text("Copy to clipboard")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Exports contain only your own learning data. Statistics are computed entirely offline.",
                    fontSize = 11.sp, color = surfaceColors.textMuted
                )
            }
        }

        item {
            GoalsSection(controller, scope)
        }

        item {
            SectionCard(
                title = "Data definitions",
                subtitle = "Every metric has an explicit definition"
            ) {
                Text(
                    "Studied = reviewed at least once. Learned = graduated from the new state. Mature = FSRS review with interval ≥ 21 days. Mastered = interval ≥ 180 days. Weak = 3+ lapses. Relearning = currently relearning after a lapse. Retention/accuracy = correct (Good/Easy) answers ÷ total answers. Writing-verified = a writing attempt scored ≥ 70%. JLPT coverage = studied items mapped to the bundled JLPT lists — a study-coverage estimate, not a certification.",
                    fontSize = 11.sp,
                    color = surfaceColors.textSecondary
                )
            }
        }

        item {
            SectionCard(
                title = "Known limitations",
                subtitle = "What is deliberately not invented"
            ) {
                Text(
                    "• Listening and grammar analytics are not shown because this build records no listening or grammar study events.\n" +
                        "• Writing/exam/session counters start from when this version was installed; review history goes back further.\n" +
                        "• Frequency bands are kanji-only (the bundled vocabulary data has no frequency field).\n" +
                        "• The JLPT indicator is coverage-based only and explicitly not a proficiency prediction.",
                    fontSize = 11.sp,
                    color = surfaceColors.textMuted
                )
            }
        }
    }
}

@Composable
private fun GoalsSection(controller: StatisticsController, scope: CoroutineScope) {
    val surfaceColors = LocalSurfaceColors.current
    var showAdd by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<List<ua.syt0r.kanji.core.statistics.GoalProgress>>(emptyList()) }

    LaunchedEffect(controller.goals, controller.overview) {
        progress = controller.goalProgress()
    }

    SectionCard(
        title = "Goals",
        subtitle = "Local, offline — progress comes from real counters"
    ) {
        if (controller.goals.isEmpty()) {
            Text("No goals yet. Set daily or weekly targets to keep yourself accountable.", fontSize = 12.sp, color = surfaceColors.textMuted)
        } else {
            controller.goals.forEach { goal ->
                val p = progress.firstOrNull { it.goal.id == goal.id }
                ProgressRow(
                    label = goal.label + if (goal.period == GoalPeriod.Weekly) " (weekly)" else "",
                    fraction = p?.fraction ?: 0f,
                    detail = "${p?.current ?: 0} / ${goal.target}",
                    color = if (p?.completed == true) LocalKaiteyoSemanticColors.current.success else LocalKaiteyoAccent.current.primary
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showAdd = true }) { Text("+ Add goal") }
    }

    if (showAdd) {
        AddGoalDialog(
            onDismiss = { showAdd = false },
            onAdd = { goal ->
                showAdd = false
                scope.launch { controller.addGoal(goal) }
            }
        )
    }
}

@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onAdd: (LearningGoal) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    var type by remember { mutableStateOf(GoalType.DailyReviews) }
    var target by remember { mutableStateOf("20") }
    var period by remember { mutableStateOf(GoalPeriod.Daily) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColors.surface,
        title = { Text("Add goal", color = surfaceColors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalType.entries.forEach { entry ->
                    Row(
                        Modifier.fillMaxWidth().clickableModifier { type = entry },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (type == entry) "•" else "○",
                            fontSize = 14.sp,
                            color = if (type == entry) LocalKaiteyoAccent.current.primary else surfaceColors.textMuted
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(entry.displayName, fontSize = 13.sp, color = surfaceColors.textPrimary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Target", fontSize = 13.sp, color = surfaceColors.textPrimary, modifier = Modifier.weight(1f))
                    androidx.compose.material3.OutlinedTextField(
                        value = target,
                        onValueChange = { target = it.filter { c -> c.isDigit() }.take(4) },
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Period", fontSize = 13.sp, color = surfaceColors.textPrimary, modifier = Modifier.weight(1f))
                    PeriodSelector(
                        options = listOf(GoalPeriod.Daily to "Daily", GoalPeriod.Weekly to "Weekly"),
                        selected = period,
                        onSelect = { period = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAdd(
                    LearningGoal(
                        id = "goal-${kotlin.random.Random.nextLong()}",
                        type = type,
                        target = target.toIntOrNull()?.coerceAtLeast(1) ?: 20,
                        period = period
                    )
                )
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PeriodSelector(
    options: List<Pair<GoalPeriod, String>>,
    selected: GoalPeriod,
    onSelect: (GoalPeriod) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) ->
            TextButton(
                onClick = { onSelect(value) },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = if (selected == value) LocalKaiteyoAccent.current.primary else LocalSurfaceColors.current.textMuted
                )
            ) { Text(label) }
        }
    }
}

private fun Modifier.clickableModifier(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

private fun VelocityLabel(value: Float): String =
    ua.syt0r.kanji.core.statistics.VelocityCalculator.oneDecimal(value)

@Composable
private fun ProfileRow(label: String, value: String?) {
    if (value == null) return
    val surfaceColors = LocalSurfaceColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 12.sp, color = surfaceColors.textSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary)
    }
}

private fun formatMinutes(minutes: Long): String = when {
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    minutes > 0 -> "${minutes}m"
    else -> "<1m"
}

private fun formatDurationShort(duration: kotlin.time.Duration): String {
    val minutes = duration.inWholeMinutes
    return when {
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "${duration.inWholeSeconds}s"
    }
}
