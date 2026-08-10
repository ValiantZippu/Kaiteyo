package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule as OutlinedSchedule
import androidx.compose.material.icons.outlined.School as OutlinedSchool
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableColumn
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.launchOnInvoke
import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.srs.VocabPracticeType
import ua.syt0r.kanji.dialog_apply
import ua.syt0r.kanji.dialog_cancel
import ua.syt0r.kanji.general_dashboard_action_browse_cards
import ua.syt0r.kanji.general_dashboard_action_import_export
import ua.syt0r.kanji.general_dashboard_action_search
import ua.syt0r.kanji.general_dashboard_action_statistics
import ua.syt0r.kanji.general_dashboard_collections
import ua.syt0r.kanji.general_dashboard_continue_studying
import ua.syt0r.kanji.general_dashboard_continue_studying_subtitle
import ua.syt0r.kanji.general_dashboard_downloads
import ua.syt0r.kanji.general_dashboard_header_reviews
import ua.syt0r.kanji.general_dashboard_header_streak_current
import ua.syt0r.kanji.general_dashboard_header_streak_longest
import ua.syt0r.kanji.general_dashboard_header_total_reviews
import ua.syt0r.kanji.general_dashboard_no_recent_activity
import ua.syt0r.kanji.general_dashboard_no_recent_decks
import ua.syt0r.kanji.general_dashboard_quick_actions
import ua.syt0r.kanji.general_dashboard_recent_activity
import ua.syt0r.kanji.general_dashboard_recent_decks
import ua.syt0r.kanji.general_dashboard_see_all
import ua.syt0r.kanji.general_dashboard_social
import ua.syt0r.kanji.general_dashboard_study_now
import ua.syt0r.kanji.general_dashboard_study_target_daily_limit
import ua.syt0r.kanji.general_dashboard_study_target_edit
import ua.syt0r.kanji.general_dashboard_study_target_empty
import ua.syt0r.kanji.general_dashboard_study_target_no_decks
import ua.syt0r.kanji.general_dashboard_study_target_nothing_left
import ua.syt0r.kanji.general_dashboard_study_target_title
import ua.syt0r.kanji.general_dashboard_text_analysis
import ua.syt0r.kanji.general_dashboard_today_progress
import ua.syt0r.kanji.general_dashboard_tutorial
import ua.syt0r.kanji.general_dashboard_weekly_summary
import ua.syt0r.kanji.presentation.common.AppDropdownMenu
import ua.syt0r.kanji.presentation.common.AppDropdownMenuItem
import ua.syt0r.kanji.presentation.common.AppListItem
import ua.syt0r.kanji.presentation.common.AppListItemDefaults
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.common.ScreenLetterPracticeType
import ua.syt0r.kanji.presentation.common.ScreenVocabPracticeType
import ua.syt0r.kanji.presentation.common.copyCentered
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.snapSizeTransform
import ua.syt0r.kanji.presentation.common.ui.FancyLoading
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoActivity
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoActivityType
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.GeneralDashboardScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.ui.TutorialDialog
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeConfigurationCard
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeScreenConfiguration
import ua.syt0r.kanji.srs_status_due
import ua.syt0r.kanji.srs_status_new
import kotlin.math.roundToInt
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun GeneralDashboardScreenUI(
    state: State<ScreenState>,
    navigateToDailyLimitConfiguration: () -> Unit,
    navigateToCreateLetterDeck: () -> Unit,
    navigateToCreateVocabDeck: () -> Unit,
    navigateToLetterPractice: (MainDestination.LetterPractice) -> Unit,
    navigateToVocabPractice: (MainDestination.VocabPractice) -> Unit,
    navigateToDeckDetails: (DashboardDeckSummary) -> Unit,
    navigateToSearch: () -> Unit,
    navigateToCardBrowser: () -> Unit,
    navigateToStatistics: () -> Unit,
    navigateToImportExport: () -> Unit,
    downloadsClick: () -> Unit,
    socialClick: (SocialButton) -> Unit,
    textAnalysisClick: () -> Unit
) {

    var showTutorialDialog by remember { mutableStateOf(false) }
    if (showTutorialDialog) {
        TutorialDialog { showTutorialDialog = false }
    }

    ScreenLayout(state) { screenState, snackbarHostState ->

        val coroutineScope = rememberCoroutineScope()
        var showStudyTargetsEditDialog by rememberSaveable { mutableStateOf(false) }
        if (showStudyTargetsEditDialog) {
            StudyTargetsEditDialog(
                onDismissRequest = { showStudyTargetsEditDialog = false },
                state = screenState
            )
        }

        DashboardHeader(screenState)

        ScreenDivider()

        ContinueStudyingCard(
            screenState = screenState,
            navigateToLetterPractice = navigateToLetterPractice,
            navigateToVocabPractice = navigateToVocabPractice,
            notifyNothingLeftToStudy = coroutineScope.launchOnInvoke {
                val message = getString(Res.string.general_dashboard_study_target_nothing_left)
                snackbarHostState.showSnackbar(message, withDismissAction = true)
            }
        )

        ScreenDivider()

        WeeklySummaryCard(screenState.stats)

        ScreenDivider()

        StudyTargets(
            state = screenState,
            showEditDialog = { showStudyTargetsEditDialog = true },
            navigateToDailyLimitConfiguration = navigateToDailyLimitConfiguration,
            navigateToCreateLetterDeck = navigateToCreateLetterDeck,
            navigateToCreateVocabDeck = navigateToCreateVocabDeck,
            navigateToLetterPractice = navigateToLetterPractice,
            navigateToVocabPractice = navigateToVocabPractice,
            notifyNothingLeftToStudy = coroutineScope.launchOnInvoke {
                val message = getString(Res.string.general_dashboard_study_target_nothing_left)
                snackbarHostState.showSnackbar(message, withDismissAction = true)
            }
        )

        ScreenDivider()

        RecentDecksSection(
            decks = screenState.recentDecks,
            navigateToDeckDetails = navigateToDeckDetails,
            createLetterDeck = navigateToCreateLetterDeck,
            createVocabDeck = navigateToCreateVocabDeck
        )

        ScreenDivider()

        RecentActivitySection(screenState.recentActivity)

        ScreenDivider()

        QuickActionsSection(
            navigateToSearch = navigateToSearch,
            navigateToCardBrowser = navigateToCardBrowser,
            navigateToStatistics = navigateToStatistics,
            navigateToImportExport = navigateToImportExport,
            textAnalysisClick = textAnalysisClick
        )

        ScreenDivider()

        CollectionsSection(screenState.collections)

        ScreenDivider()

        AppListItem(
            onClick = textAnalysisClick,
            headlineContent = { Text(stringResource(Res.string.general_dashboard_text_analysis)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
        )

        SocialButton(
            selected = socialClick
        )

        AppListItem(
            onClick = downloadsClick,
            headlineContent = { Text(stringResource(Res.string.general_dashboard_downloads)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
        )

        AppListItem(
            onClick = { showTutorialDialog = true },
            headlineContent = { Text(stringResource(Res.string.general_dashboard_tutorial)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
        )

    }

}

@Composable
private fun ScreenDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
}

@Composable
private fun StudyTargetsEditDialog(
    onDismissRequest: () -> Unit,
    state: ScreenState.Loaded
) {

    var states by remember {
        mutableStateOf(state.studyTargets.value)
    }

    val toggleEnabledAtIndex = { index: Int ->
        states = states.toMutableList().apply {
            val itemState = get(index).run { copy(enabled = !enabled) }
            set(index, itemState)
        }
    }

    MultiplatformDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.general_dashboard_study_target_title)) },
        paddedContent = false,
        content = {
            ReorderableColumn(
                list = states.toList(),
                onSettle = { fromIndex, toIndex ->
                    states = states.toList()
                        .toMutableList()
                        .apply { add(toIndex, removeAt(fromIndex)) }
                }
            ) { index, item, _ ->
                val studyTarget = item.studyTarget
                key(studyTarget) {
                    AppListItem(
                        onClick = { toggleEnabledAtIndex(index) },
                        leadingContent = {
                            Icon(Icons.Outlined.DragIndicator, null, Modifier.draggableHandle())
                        },
                        overlineContent = { Text(stringResource(studyTarget.categoryTitle)) },
                        headlineContent = { Text(stringResource(studyTarget.typeTitleRes)) },
                        trailingContent = {
                            Switch(
                                checked = item.enabled,
                                onCheckedChange = { toggleEnabledAtIndex(index) }
                            )
                        }
                    )
                }
            }
        },
        buttons = {
            TextButton(onDismissRequest) {
                Text(stringResource(Res.string.dialog_cancel))
            }
            TextButton(
                onClick = {
                    state.studyTargets.value = states
                    onDismissRequest()
                }
            ) {
                Text(stringResource(Res.string.dialog_apply))
            }
        }
    )
}

@Composable
private fun SocialButton(selected: (SocialButton) -> Unit) {
    var showDropdown by rememberSaveable { mutableStateOf(false) }

    AppListItem(
        onClick = { showDropdown = true },
        headlineContent = { Text(stringResource(Res.string.general_dashboard_social)) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            AppDropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false }
            ) {
                SocialButton.entries.forEach {
                    AppDropdownMenuItem(
                        onClick = { selected(it) },
                        content = {
                            Icon(painterResource(it.icon), null)
                            Text(stringResource(it.title))
                        }
                    )
                }
            }
        }
    )
}

// ============================================================
// HEADER
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardHeader(state: ScreenState.Loaded) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 4.dp
            )
            .padding(AppListItemDefaults.ExtraPaddings)
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fun Int.numberOrDash(): String = if (this == 0) "-" else toString()

        HeaderStatItem(
            title = stringResource(Res.string.general_dashboard_header_streak_current),
            text = state.stats.currentStreak.numberOrDash(),
            icon = Icons.Default.LocalFireDepartment,
            modifier = Modifier.weight(1f)
        )
        HeaderStatItem(
            title = stringResource(Res.string.general_dashboard_header_streak_longest),
            text = state.stats.longestStreak.numberOrDash(),
            icon = Icons.Default.Timeline,
            modifier = Modifier.weight(1f)
        )
        HeaderStatItem(
            title = stringResource(Res.string.general_dashboard_header_reviews),
            text = state.stats.reviewsToday.numberOrDash(),
            icon = Icons.Default.Schedule,
            modifier = Modifier.weight(1f)
        )
        HeaderStatItem(
            title = stringResource(Res.string.general_dashboard_header_total_reviews),
            text = state.stats.totalReviews.toString(),
            icon = Icons.Default.BarChart,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HeaderStatItem(
    title: String,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .clip(MaterialTheme.shapes.medium)
            .background(surfaceColors.surface)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

// ============================================================
// CONTINUE STUDYING
// ============================================================

@Composable
private fun ContinueStudyingCard(
    screenState: ScreenState.Loaded,
    navigateToLetterPractice: (MainDestination.LetterPractice) -> Unit,
    navigateToVocabPractice: (MainDestination.VocabPractice) -> Unit,
    notifyNothingLeftToStudy: () -> Unit
) {

    val surfaceColors = LocalSurfaceColors.current
    val enabledTargets = screenState.studyTargets.value.filter { it.enabled }
    val totalNew = enabledTargets.sumOf {
        (it.progress as? StudyTargetProgress.WithDecks)?.options?.newCards?.size ?: 0
    }
    val totalDue = enabledTargets.sumOf {
        (it.progress as? StudyTargetProgress.WithDecks)?.options?.dueCards?.size ?: 0
    }
    val totalReady = totalNew + totalDue
    val doneFraction = screenState.stats.todayProgressFraction

    val bestTarget = enabledTargets
        .mapNotNull { target ->
            val progress = target.progress as? StudyTargetProgress.WithDecks ?: return@mapNotNull null
            target to progress.options.combinedCards.size
        }
        .maxByOrNull { it.second }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .clickable {
                if (totalReady == 0) {
                    notifyNothingLeftToStudy()
                } else {
                    bestTarget?.let { (target, _) ->
                        val cards = (target.progress as StudyTargetProgress.WithDecks).options.combinedCards
                        when (val practiceType = target.studyTarget.practiceType) {
                            is LetterPracticeType -> {
                                val configuration = LetterPracticeScreenConfiguration(
                                    cards = cards as List<LetterPracticeScreenConfiguration.Card>,
                                    practiceType = ScreenLetterPracticeType.from(practiceType)
                                )
                                navigateToLetterPractice(MainDestination.LetterPractice(configuration))
                            }

                            is VocabPracticeType -> {
                                val configuration = VocabPracticeScreenConfiguration(
                                    cards = cards as List<VocabPracticeScreenConfiguration.Card>,
                                    practiceType = ScreenVocabPracticeType.from(practiceType)
                                )
                                navigateToVocabPractice(MainDestination.VocabPractice(configuration))
                            }
                        }
                    }
                }
            }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(Modifier.weight(1f)) {

            Text(
                text = stringResource(Res.string.general_dashboard_continue_studying),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (totalReady == 0) {
                    stringResource(Res.string.general_dashboard_study_target_nothing_left)
                } else {
                    stringResource(Res.string.general_dashboard_continue_studying_subtitle, totalReady)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (totalReady > 0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudyCountPill(
                        text = stringResource(Res.string.srs_status_new),
                        count = totalNew,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    StudyCountPill(
                        text = stringResource(Res.string.srs_status_due),
                        count = totalDue,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val progressColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                )
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * doneFraction,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = (doneFraction * 100).roundToInt().toString() + "%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(Res.string.general_dashboard_today_progress),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StudyCountPill(text: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(
            text = "$count $text",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ============================================================
// WEEKLY SUMMARY
// ============================================================

@Composable
private fun WeeklySummaryCard(stats: GeneralDashboardStats) {

    val surfaceColors = LocalSurfaceColors.current
    val maxCount = stats.weeklySummary.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.general_dashboard_weekly_summary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(Res.string.general_dashboard_header_reviews),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stats.weeklySummary.forEach { day ->
                WeeklyBar(day, maxCount, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WeeklyBar(
    day: DashboardDaySummary,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    val fraction = if (maxCount == 0) 0f else day.count.toFloat() / maxCount
    val barHeight by animateDpAsState(
        targetValue = (fraction * 56).dp,
        label = "weeklyBar"
    )
    val isToday = day.date == Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (day.count > 0) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = day.date.dayOfWeek.name.first().toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// STUDY TARGETS
// ============================================================

@Composable
private fun StudyTargets(
    state: ScreenState.Loaded,
    showEditDialog: () -> Unit,
    navigateToDailyLimitConfiguration: () -> Unit,
    navigateToCreateLetterDeck: () -> Unit,
    navigateToCreateVocabDeck: () -> Unit,
    navigateToLetterPractice: (MainDestination.LetterPractice) -> Unit,
    navigateToVocabPractice: (MainDestination.VocabPractice) -> Unit,
    notifyNothingLeftToStudy: () -> Unit
) {

    Column {

        Row(
            modifier = Modifier.padding(start = 24.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = stringResource(Res.string.general_dashboard_study_target_title),
                style = MaterialTheme.typography.titleSmall.copyCentered(),
                fontWeight = FontWeight.SemiBold
            )
            var showPopup by remember { mutableStateOf(false) }

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { showPopup = true }
            ) {

                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null
                )

                AppDropdownMenu(
                    expanded = showPopup,
                    onDismissRequest = { showPopup = false }
                ) {
                    AppDropdownMenuItem(
                        onClick = {
                            showEditDialog()
                            showPopup = false
                        }
                    ) {
                        Icon(Icons.Outlined.Edit, null)
                        Text(stringResource(Res.string.general_dashboard_study_target_edit))
                    }
                    AppDropdownMenuItem(
                        onClick = {
                            navigateToDailyLimitConfiguration()
                            showPopup = false
                        }
                    ) {
                        Icon(Icons.Outlined.Settings, null)
                        Text(stringResource(Res.string.general_dashboard_study_target_daily_limit))
                    }
                }

            }

        }

        val displayList = state.studyTargets.value.filter { it.enabled }

        if (displayList.isEmpty()) {
            AppListItem(
                onClick = showEditDialog,
                headlineContent = {
                    Text(
                        text = stringResource(Res.string.general_dashboard_study_target_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            displayList.forEach {
                StudyTargetItem(
                    studyTargetState = it,
                    createDeck = {
                        when (it.studyTarget.practiceType) {
                            is LetterPracticeType -> navigateToCreateLetterDeck()
                            is VocabPracticeType -> navigateToCreateVocabDeck()
                        }
                    },
                    startPractice = { cards ->
                        if (cards.isEmpty()) {
                            notifyNothingLeftToStudy()
                            return@StudyTargetItem
                        }
                        when (val practiceType = it.studyTarget.practiceType) {
                            is LetterPracticeType -> {
                                val configuration = LetterPracticeScreenConfiguration(
                                    cards = cards as List<LetterPracticeScreenConfiguration.Card>,
                                    practiceType = ScreenLetterPracticeType.from(practiceType)
                                )
                                val destination = MainDestination.LetterPractice(configuration)
                                navigateToLetterPractice(destination)
                            }

                            is VocabPracticeType -> {
                                val configuration = VocabPracticeScreenConfiguration(
                                    cards = cards as List<VocabPracticeScreenConfiguration.Card>,
                                    practiceType = ScreenVocabPracticeType.from(practiceType)
                                )
                                val destination = MainDestination.VocabPractice(configuration)
                                navigateToVocabPractice(destination)
                            }
                        }
                    }
                )
            }
        }

    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyTargetItem(
    studyTargetState: StudyTargetState,
    createDeck: () -> Unit,
    startPractice: (List<PracticeConfigurationCard>) -> Unit
) {

    val studyTarget = studyTargetState.studyTarget
    val studyProgress = studyTargetState.progress

    AppListItem(
        onClick = {
            when (studyProgress) {
                StudyTargetProgress.NoDecks -> createDeck()
                is StudyTargetProgress.WithDecks -> {
                    startPractice(studyProgress.options.combinedCards)
                }
            }
        },
        headlineContent = {
            Text(
                stringResource(studyTarget.categoryTitle) + "・" + stringResource(studyTarget.typeTitleRes)
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = null
            )
        },
        supportingContent = {
            if (studyProgress is StudyTargetProgress.NoDecks) {
                Text(stringResource(Res.string.general_dashboard_study_target_no_decks))
                return@AppListItem
            }

            studyProgress as StudyTargetProgress.WithDecks

            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingTiny)
            ) {

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(-Dimens.SpacingMid)
                ) {

                    ClickableStudyRow(
                        imageVector = Icons.Outlined.OutlinedSchool,
                        title = stringResource(Res.string.srs_status_new),
                        count = studyProgress.options.newCards.size,
                        onClick = { startPractice(studyProgress.options.newCards) }
                    )

                    ClickableStudyRow(
                        imageVector = Icons.Outlined.OutlinedSchedule,
                        title = stringResource(Res.string.srs_status_due),
                        count = studyProgress.options.dueCards.size,
                        onClick = { startPractice(studyProgress.options.dueCards) }
                    )

                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMid)
                ) {

                    LinearProgressIndicator(
                        progress = studyProgress.totalProgress,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Text(
                        text = studyProgress.totalProgress.times(100).roundToInt().toString() + "%",
                        style = LocalTextStyle.current.copyCentered()
                    )

                }
            }
        }
    )
}

@Composable
private fun ClickableStudyRow(
    imageVector: ImageVector,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpacingMid, vertical = Dimens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
    ) {
        val iconSize = 18.dp
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(iconSize)
        )

        val textStyle = LocalTextStyle.current.copyCentered()

        Text(
            text = title,
            style = textStyle
        )

        if (count == 0) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(iconSize)
            )
        } else {
            Text(
                text = count.toString(),
                style = textStyle
            )
        }
    }
}

// ============================================================
// RECENT DECKS
// ============================================================

@Composable
private fun RecentDecksSection(
    decks: List<DashboardDeckSummary>,
    navigateToDeckDetails: (DashboardDeckSummary) -> Unit,
    createLetterDeck: () -> Unit,
    createVocabDeck: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {

        Text(
            text = stringResource(Res.string.general_dashboard_recent_decks),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )

        if (decks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.general_dashboard_no_recent_decks),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = createLetterDeck) {
                        Text(stringResource(Res.string.dialog_apply))
                    }
                    TextButton(onClick = createVocabDeck) {
                        Text(stringResource(Res.string.dialog_apply))
                    }
                }
            }
            return
        }

        decks.forEach { deck ->
            DashboardDeckRow(deck = deck, onClick = { navigateToDeckDetails(deck) })
        }

    }
}

@Composable
private fun DashboardDeckRow(
    deck: DashboardDeckSummary,
    onClick: () -> Unit
) {

    val surfaceColors = LocalSurfaceColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val backgroundColor by animateColorAsState(
        if (isHovered) surfaceColors.surfaceInteractive else surfaceColors.surface
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (deck.category == DashboardDeckCategory.Letters)
                    Icons.Default.School else Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = deck.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = deck.lastReview?.let { formatRelativeTime(it) } ?: "—",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (deck.newCount > 0 || deck.dueCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (deck.newCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = deck.newCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                if (deck.dueCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = deck.dueCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// RECENT ACTIVITY
// ============================================================

@Composable
private fun RecentActivitySection(activity: List<KaiteyoActivity>) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {

        Text(
            text = stringResource(Res.string.general_dashboard_recent_activity),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )

        if (activity.isEmpty()) {
            Text(
                text = stringResource(Res.string.general_dashboard_no_recent_activity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp).padding(vertical = 8.dp)
            )
            return
        }

        activity.take(6).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(activityTypeColor(item.type))
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    if (item.details.isNotBlank() && item.details != item.title) {
                        Text(
                            text = item.details,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    text = formatRelativeTime(item.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun activityTypeColor(type: KaiteyoActivityType): Color = when (type) {
    KaiteyoActivityType.Review -> Color(0xFF4CAF50)
    KaiteyoActivityType.ReviewFailed -> Color(0xFFF44336)
    KaiteyoActivityType.Edit -> Color(0xFF2196F3)
    KaiteyoActivityType.Import -> Color(0xFF9C27B0)
    KaiteyoActivityType.Export -> Color(0xFF009688)
    KaiteyoActivityType.Tag -> Color(0xFFFF9800)
    KaiteyoActivityType.Flag -> Color(0xFFFF5722)
    KaiteyoActivityType.Note -> Color(0xFF3F51B5)
    KaiteyoActivityType.Study -> Color(0xFF00BCD4)
    KaiteyoActivityType.System -> Color(0xFF9E9E9E)
}

private fun formatRelativeTime(instant: Instant): String {
    val now = Clock.System.now()
    val duration = now - instant
    return when {
        duration < 60.seconds -> "just now"
        duration < 60.minutes -> "${duration.inWholeMinutes}m ago"
        duration < 24.hours -> "${duration.inWholeHours}h ago"
        duration < 7.days -> "${duration.inWholeDays}d ago"
        else -> {
            val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${dt.dayOfMonth}/${dt.monthNumber}"
        }
    }
}

// ============================================================
// QUICK ACTIONS
// ============================================================

@Composable
private fun QuickActionsSection(
    navigateToSearch: () -> Unit,
    navigateToCardBrowser: () -> Unit,
    navigateToStatistics: () -> Unit,
    navigateToImportExport: () -> Unit,
    textAnalysisClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {

        Text(
            text = stringResource(Res.string.general_dashboard_quick_actions),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            QuickActionCard(
                icon = Icons.Default.Search,
                label = stringResource(Res.string.general_dashboard_action_search),
                onClick = navigateToSearch,
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                icon = Icons.Default.History,
                label = stringResource(Res.string.general_dashboard_action_browse_cards),
                onClick = navigateToCardBrowser,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            QuickActionCard(
                icon = Icons.Default.BarChart,
                label = stringResource(Res.string.general_dashboard_action_statistics),
                onClick = navigateToStatistics,
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                icon = Icons.Default.ImportExport,
                label = stringResource(Res.string.general_dashboard_action_import_export),
                onClick = navigateToImportExport,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        QuickActionCard(
            icon = Icons.Default.School,
            label = stringResource(Res.string.general_dashboard_text_analysis),
            onClick = textAnalysisClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val surfaceColors = LocalSurfaceColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val backgroundColor by animateColorAsState(
        if (isHovered) surfaceColors.surfaceInteractive else surfaceColors.surface
    )

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

// ============================================================
// COLLECTIONS
// ============================================================

@Composable
private fun CollectionsSection(
    collections: List<ua.syt0r.kanji.presentation.screen.main.features.KaiteyoCollection>
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {

        Text(
            text = stringResource(Res.string.general_dashboard_collections),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        if (collections.isEmpty()) {
            Text(
                text = stringResource(Res.string.general_dashboard_no_recent_decks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
            return
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            collections.take(4).forEach { collection ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = {})
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(collection.icon, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                    Text(
                        text = collection.cardIds.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenLayout(
    state: State<ScreenState>,
    content: @Composable ColumnScope.(ScreenState.Loaded, SnackbarHostState) -> Unit
) {

    val snackbarHostState = remember { SnackbarHostState() }

    Box {
        AnimatedContent(
            targetState = state.value,
            transitionSpec = { ContentTransform(targetContentEnter = fadeIn(), initialContentExit = fadeOut(), sizeTransform = snapSizeTransform()) }
        ) { screenState ->

            when (screenState) {
                ScreenState.Loading -> {
                    FancyLoading(Modifier.fillMaxSize().wrapContentSize())
                }

                is ScreenState.Loaded -> Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .wrapContentWidth()
                        .widthIn(max = 520.dp)
                ) {

                    if (LocalOrientation.current == Orientation.Landscape) {
                        Spacer(Modifier.height(20.dp))
                    }

                    content(screenState, snackbarHostState)

                    Spacer(Modifier.height(20.dp))

                }

            }

        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = {
                Snackbar(
                    snackbarData = it,
                    containerColor = MaterialTheme.colorScheme.surfaceDim,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionContentColor = MaterialTheme.colorScheme.primary,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        )

    }

}
