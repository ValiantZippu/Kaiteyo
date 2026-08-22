package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.LinearProgressIndicator as MaterialLinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule as OutlinedSchedule
import androidx.compose.material.icons.outlined.School as OutlinedSchool
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableColumn
import ua.syt0r.kanji.Res
import androidx.compose.material.icons.filled.Search
import ua.syt0r.kanji.core.launchOnInvoke
import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.srs.VocabPracticeType
import ua.syt0r.kanji.dialog_apply
import ua.syt0r.kanji.dialog_cancel
import ua.syt0r.kanji.general_dashboard_activity
import ua.syt0r.kanji.general_dashboard_activity_less
import ua.syt0r.kanji.general_dashboard_activity_more
import kotlinx.coroutines.delay
import ua.syt0r.kanji.general_dashboard_no_recent_activity
import ua.syt0r.kanji.general_dashboard_no_recent_decks
import ua.syt0r.kanji.general_dashboard_recent_activity
import ua.syt0r.kanji.general_dashboard_recent_decks
import ua.syt0r.kanji.general_dashboard_see_all
import ua.syt0r.kanji.general_dashboard_study_target_daily_limit
import ua.syt0r.kanji.general_dashboard_study_target_edit
import ua.syt0r.kanji.general_dashboard_study_target_empty
import ua.syt0r.kanji.general_dashboard_study_target_no_decks
import ua.syt0r.kanji.general_dashboard_study_target_nothing_left
import ua.syt0r.kanji.general_dashboard_study_target_title
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
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.theme.snapSizeTransform
import ua.syt0r.kanji.presentation.common.ui.FancyLoading
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation
import ua.syt0r.kanji.presentation.common.ui.isWideContentLayout
import ua.syt0r.kanji.presentation.common.ui.rememberAdaptiveContentMaxWidth
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.HeatmapDayData
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.HeatmapDisplayMode
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.StudyCalendar
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.StudyCalendarState
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.StudyHeatmap
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.TimeProgressCompact
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.TimeProgressGroup
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.calculateTimeProgress
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoActivity
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoActivityType
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeCommandCenterSection
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.GeneralDashboardScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DashboardErrorState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.ui.TutorialDialog
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeConfigurationCard
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeScreenConfiguration
import ua.syt0r.kanji.srs_status_due
import ua.syt0r.kanji.srs_status_new
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// ============================================================
// HOME DASHBOARD — complete rewrite
//
// Architecture:
//   ┌─────────────────────────────────────────────────┐
//   │ HEADER: greeting + KPIs + search CTA            │
//   ├───────────────────┬─────────────────────────────┤
//   │ TODAY: continue   │ TIME: 5-bar progress        │
//   │ studying hero     │ year/month/week/day/hour     │
//   ├───────────────────┴─────────────────────────────┤
//   │ HEATMAP: shared canonical component              │
//   ├───────────────────┬─────────────────────────────┤
//   │ STUDY TARGETS     │ RECENT DECKS + ACTIVITY      │
//   └───────────────────┴─────────────────────────────┘
//
// Desktop: two-column sections side by side
// Phone: single column stacked
// ============================================================

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
    navigateToCollections: () -> Unit,
    navigateToDictionary: () -> Unit,
    navigateToRadicals: () -> Unit,
    navigateToKanjiBrowser: () -> Unit,
    navigateToSentences: () -> Unit,
    navigateToLearnerProfile: () -> Unit,
    textAnalysisClick: () -> Unit,
    onOpenKanji: (String) -> Unit = {},
    onOpenWord: (Long) -> Unit = {},
    onOpenBrowse: () -> Unit = {},
    onOpenRadicals: () -> Unit = navigateToRadicals,
    onOpenComponents: () -> Unit = {},
    navigateToLibrary: () -> Unit = {},
    onOpenDay: (kotlinx.datetime.LocalDate) -> Unit = {},
    retryLoad: () -> Unit
) {

    var showTutorialDialog by remember { mutableStateOf(false) }
    if (showTutorialDialog) {
        TutorialDialog { showTutorialDialog = false }
    }

    // Live time progress — updates every minute
    var timeProgress by remember { mutableStateOf(calculateTimeProgress()) }
    LaunchedEffect(Unit) {
        while (true) {
            timeProgress = calculateTimeProgress()
            delay(60_000L) // Update every minute
        }
    }

    // Study calendar state
    var calendarState by remember { mutableStateOf(StudyCalendarState.calculate()) }

    ScreenLayout(state, onRetry = retryLoad) { screenState, snackbarHostState, isWide ->

        val coroutineScope = rememberCoroutineScope()
        var showStudyTargetsEditDialog by rememberSaveable { mutableStateOf(false) }
        if (showStudyTargetsEditDialog) {
            StudyTargetsEditDialog(
                onDismissRequest = { showStudyTargetsEditDialog = false },
                state = screenState
            )
        }

        // ── HEADER: greeting + KPIs + search ──────────────────
        DashboardHeader(
            state = screenState,
            onSearchClick = navigateToSearch
        )

        // ── COMMAND CENTER: recent searches + entries + discover ──
        HomeCommandCenterSection(
            onOpenKanji = onOpenKanji,
            onOpenWord = onOpenWord,
            onOpenBrowse = onOpenBrowse,
            onOpenRadicals = onOpenRadicals,
            onOpenComponents = onOpenComponents,
            onOpenCollections = navigateToCollections,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (isWide) {
            // ── DESKTOP: two-column primary area ───────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // LEFT: Continue studying + study targets
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ContinueStudyingCard(
                        screenState = screenState,
                        navigateToLetterPractice = navigateToLetterPractice,
                        navigateToVocabPractice = navigateToVocabPractice,
                        navigateToLibrary = navigateToLibrary
                    )

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
                }

                // RIGHT: Time progress + Calendar + Heatmap
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Time progress visualization
                    TimeProgressGroup(
                        state = timeProgress,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Study calendar
                    StudyCalendar(
                        state = calendarState,
                        onMonthChange = { /* Update month */ },
                        onDayClick = onOpenDay,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Activity heatmap (must have both calendar and heatmap)
                    val heatmapData = remember(screenState) {
                        screenState.stats.heatmapSummary.map {
                            HeatmapDayData(date = it.date, count = it.count)
                        }
                    }
                    StudyHeatmap(
                        activityData = heatmapData,
                        displayMode = HeatmapDisplayMode.Compact,
                        onDayClick = onOpenDay,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // ── PHONE: single column ───────────────────────────
            // Time progress (compact)
            TimeProgressCompact(
                state = timeProgress,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Continue studying
            ContinueStudyingCard(
                screenState = screenState,
                navigateToLetterPractice = navigateToLetterPractice,
                navigateToVocabPractice = navigateToVocabPractice,
                navigateToLibrary = navigateToLibrary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Heatmap + Calendar combined
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val heatmapData = remember(screenState) {
                    screenState.stats.heatmapSummary.map {
                        HeatmapDayData(date = it.date, count = it.count)
                    }
                }
                StudyHeatmap(
                    activityData = heatmapData,
                    displayMode = HeatmapDisplayMode.Compact,
                    onDayClick = onOpenDay,
                    modifier = Modifier.weight(1f)
                )
            }

            // Study calendar (full width)
            StudyCalendar(
                state = calendarState,
                onMonthChange = { /* Update month */ },
                onDayClick = onOpenDay,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Study targets
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
        }

        // ── DISCOVER: quick-access tool tiles ──────────────────
        DiscoverSection(
            navigateToDictionary = navigateToDictionary,
            navigateToRadicals = navigateToRadicals,
            navigateToKanjiBrowser = navigateToKanjiBrowser,
            navigateToSentences = navigateToSentences,
            navigateToLearnerProfile = navigateToLearnerProfile,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        ScreenDivider()

        // ── BOTTOM: recent decks + activity (always full width) ─
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                RecentDecksSection(
                    decks = screenState.recentDecks,
                    navigateToDeckDetails = navigateToDeckDetails,
                    createLetterDeck = navigateToCreateLetterDeck,
                    createVocabDeck = navigateToCreateVocabDeck,
                    seeAll = navigateToCardBrowser,
                    modifier = Modifier.weight(1f)
                )
                RecentActivitySection(
                    activity = screenState.recentActivity,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            RecentDecksSection(
                decks = screenState.recentDecks,
                navigateToDeckDetails = navigateToDeckDetails,
                createLetterDeck = navigateToCreateLetterDeck,
                createVocabDeck = navigateToCreateVocabDeck,
                seeAll = navigateToCardBrowser,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            ScreenDivider()

            RecentActivitySection(
                activity = screenState.recentActivity,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun ScreenDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
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

// ============================================================
// HEADER — compact greeting + KPIs + queue progress
// ============================================================

@Composable
private fun DashboardHeader(
    state: ScreenState.Loaded,
    onSearchClick: () -> Unit
) {

    val surfaceColors = LocalSurfaceColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Greeting hero (compact)
        KaiteyoDashboardHero(
            stats = state.stats,
            onSearchClick = onSearchClick,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Queue progress bar (only show if there are items)
        val queueTotal = state.stats.reviewedToday + state.stats.leftoverToday
        if (queueTotal > 0) {
            val queueFraction = state.stats.reviewedToday.toFloat() / queueTotal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(surfaceColors.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.stats.reviewedToday}/${queueTotal}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textPrimary
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(surfaceColors.surfaceInteractive.copy(alpha = 0.3f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(queueFraction.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(surfaceColors.kanjiKnown)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${(queueFraction * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = surfaceColors.kanjiKnown
                )
            }
        }
    }
}

// ============================================================
// SECTION HEADER
// ============================================================

@Composable
private fun SectionHeader(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
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
    navigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {

    val enabledTargets = screenState.studyTargets.value.filter { it.enabled }
    val totalNew = enabledTargets.sumOf {
        (it.progress as? StudyTargetProgress.WithDecks)?.options?.newCards?.size ?: 0
    }
    val totalDue = enabledTargets.sumOf {
        (it.progress as? StudyTargetProgress.WithDecks)?.options?.dueCards?.size ?: 0
    }

    val bestTarget = enabledTargets
        .mapNotNull { target ->
            val progress = target.progress as? StudyTargetProgress.WithDecks ?: return@mapNotNull null
            target to progress.options.combinedCards.size
        }
        .maxByOrNull { it.second }

    val onContinue: () -> Unit = {
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

    KaiteyoStudyHeroCard(
        stats = screenState.stats,
        newCount = totalNew,
        dueCount = totalDue,
        onContinue = onContinue,
        onNothingLeft = navigateToLibrary,
        modifier = modifier
    )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LocalSurfaceColors.current.surface)
                    .clickable(onClick = showEditDialog)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OutlinedSchool,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Set up study targets",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(Res.string.general_dashboard_study_target_empty),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    val surfaceColors = LocalSurfaceColors.current
    val noDecks = studyProgress is StudyTargetProgress.NoDecks

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .clickable {
                when (studyProgress) {
                    StudyTargetProgress.NoDecks -> createDeck()
                    is StudyTargetProgress.WithDecks -> {
                        startPractice(studyProgress.options.combinedCards)
                    }
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.OutlinedSchool,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(studyTarget.categoryTitle) + "・" + stringResource(studyTarget.typeTitleRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when {
                        noDecks -> stringResource(Res.string.general_dashboard_study_target_no_decks)
                        else -> {
                            val progress = studyProgress as StudyTargetProgress.WithDecks
                            "${progress.options.newCards.size} new · ${progress.options.dueCards.size} due"
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (studyProgress is StudyTargetProgress.WithDecks) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                MaterialLinearProgressIndicator(
                    progress = studyProgress.totalProgress,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = (studyProgress.totalProgress * 100).roundToInt().toString() + "%",
                    style = LocalTextStyle.current.copyCentered()
                )
            }
        }
    }
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
    createVocabDeck: () -> Unit,
    seeAll: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {

        SectionHeader(
            text = stringResource(Res.string.general_dashboard_recent_decks),
            icon = Icons.Default.Description,
            trailing = {
                TextButton(onClick = seeAll) { Text(stringResource(Res.string.general_dashboard_see_all)) }
            }
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
                text = deck.lastReview?.let { formatRelativeTime(it) } ?: "Never studied",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (deck.totalCount > 0) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { deck.progressFraction },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${(deck.progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (deck.newCount > 0 || deck.dueCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val sem = LocalKaiteyoSemanticColors.current
                if (deck.newCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(sem.new.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = deck.newCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = sem.new
                        )
                    }
                }
                if (deck.dueCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(sem.error.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = deck.dueCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = sem.error
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
private fun RecentActivitySection(
    activity: List<KaiteyoActivity>,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {

        SectionHeader(
            text = stringResource(Res.string.general_dashboard_recent_activity),
            icon = Icons.Default.History
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

@Composable
private fun activityTypeColor(type: KaiteyoActivityType): Color {
    val sem = LocalKaiteyoSemanticColors.current
    return when (type) {
        KaiteyoActivityType.Review -> sem.activityReview
        KaiteyoActivityType.ReviewFailed -> sem.activityReviewFailed
        KaiteyoActivityType.Edit -> sem.activityEdit
        KaiteyoActivityType.Import -> sem.activityImport
        KaiteyoActivityType.Export -> sem.activityExport
        KaiteyoActivityType.Tag -> sem.activityTag
        KaiteyoActivityType.Flag -> sem.activityFlag
        KaiteyoActivityType.Note -> sem.activityNote
        KaiteyoActivityType.Study -> sem.activityStudy
        KaiteyoActivityType.System -> sem.activitySystem
    }
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

@Composable
private fun ScreenLayout(
    state: State<ScreenState>,
    onRetry: () -> Unit,
    content: @Composable ColumnScope.(ScreenState.Loaded, SnackbarHostState, Boolean) -> Unit
) {

    val snackbarHostState = remember { SnackbarHostState() }
    val isWide = isWideContentLayout()

    Box {
        AnimatedContent(
            targetState = state.value,
            transitionSpec = { ContentTransform(targetContentEnter = fadeIn(), initialContentExit = fadeOut(), sizeTransform = snapSizeTransform()) }
        ) { screenState ->

            when (screenState) {
                ScreenState.Loading -> {
                    FancyLoading(Modifier.fillMaxSize().wrapContentSize())
                }

                is ScreenState.Error -> {
                    DashboardErrorState(
                        message = screenState.message,
                        onRetry = onRetry
                    )
                }

                is ScreenState.Loaded -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .wrapContentWidth()
                            .widthIn(max = rememberAdaptiveContentMaxWidth(
                                phoneMax = 520.dp,
                                mediumMax = 640.dp,
                                wideMax = 1100.dp
                            ))
                    ) {

                        if (LocalOrientation.current == Orientation.Landscape) {
                            Spacer(Modifier.height(20.dp))
                        }

                        content(screenState, snackbarHostState, isWide)

                        Spacer(Modifier.height(20.dp))

                    }
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

// ============================================================
// DISCOVER — quick-access tiles to exploration tools
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverSection(
    navigateToDictionary: () -> Unit,
    navigateToRadicals: () -> Unit,
    navigateToKanjiBrowser: () -> Unit,
    navigateToSentences: () -> Unit,
    navigateToLearnerProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val sem = LocalKaiteyoSemanticColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Discover",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        // Two-column grid of discover tiles
        val tiles = listOf(
            DiscoverTileData(
                title = "Dictionary",
                description = "Search kanji, words, sentences and grammar",
                icon = Icons.Filled.Search,
                color = accent.primary,
                onClick = navigateToDictionary
            ),
            DiscoverTileData(
                title = "Radicals",
                description = "Browse kanji by radical \u2014 stroke, JLPT and grade filters",
                icon = Icons.Outlined.AutoStories,
                color = sem.success,
                onClick = navigateToRadicals
            ),
            DiscoverTileData(
                title = "Kanji Browser",
                description = "Filter the full kanji set by JLPT, grade and frequency",
                icon = Icons.Filled.School,
                color = sem.info,
                onClick = navigateToKanjiBrowser
            ),
            DiscoverTileData(
                title = "Sentences",
                description = "Read and analyze corpus sentences token by token",
                icon = Icons.Filled.Translate,
                color = sem.favorite,
                onClick = navigateToSentences
            ),
            DiscoverTileData(
                title = "Learner Profile",
                description = "Adapt kanji, word and sentence pages to your level",
                icon = Icons.Outlined.Person,
                color = sem.warning,
                onClick = navigateToLearnerProfile
            )
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (tile in tiles) {
                DiscoverTile(
                    data = tile,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class DiscoverTileData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun DiscoverTile(
    data: DiscoverTileData,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        targetValue = if (isHovered) data.color.copy(alpha = 0.1f) else surfaceColors.surface,
        label = "discoverTileBg"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isHovered) data.color.copy(alpha = 0.3f) else surfaceColors.border,
                shape = RoundedCornerShape(12.dp)
            )
            .hoverable(interactionSource)
            .clickable { data.onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                tint = data.color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = data.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = surfaceColors.textPrimary
            )
        }
        Text(
            text = data.description,
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
