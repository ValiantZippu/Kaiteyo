package ua.syt0r.kanji.presentation.screen.main.screen.statistics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import ua.syt0r.kanji.core.statistics.DailyActivity
import ua.syt0r.kanji.core.statistics.HeatmapYear
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import kotlin.math.roundToInt

// ============================================================
// YEARLY ACTIVITY HEATMAP — the classic contribution grid,
// revamped for Kaiteyo.
//
// • Fixed-size square cells in a Mon→Sun week layout, like the
//   Anki heatmap addon / GitHub contribution graph.
// • A discrete 5-level intensity ramp (empty → full accent)
//   instead of a washed-out continuous gradient.
// • Month labels run across the top where each month starts;
//   Mon / Wed / Fri anchor the weekday gutter.
// • Year navigation with animated counters (active days,
//   reviews, streak), a cursor-following tooltip, and tap for
//   the full day report.
// ============================================================

private val HeatmapCellSize = 16.dp
private val HeatmapCellGap = 3.dp
private val HeatmapWeekGap = 3.dp
private val HeatmapMonthRowHeight = 18.dp
private val HeatmapGutterWidth = 28.dp

/** Discrete level alphas on the accent — level 0 is the neutral empty cell. */
private val HeatmapLevelAlphas = floatArrayOf(0.28f, 0.48f, 0.72f, 1f)

@Composable
fun StatisticsHeatmap(
    heatmap: HeatmapYear,
    availableYears: List<Int>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    onDayClick: (DailyActivity) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val animationConfig = LocalAnimationConfig.current
    val reducedMotion = animationConfig.reducedMotion

    var hoveredDay by remember { mutableStateOf<DailyActivity?>(null) }
    // Plain (non-snapshot) cache: refreshed during layout, read live by the
    // tooltip's offset lambda so it never triggers recomposition storms.
    val coordsByDate = remember { HashMap<LocalDate, LayoutCoordinates>() }
    var boxCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
    val scrollState = rememberScrollState()

    // A new year means new cells — drop the stale hover position.
    LaunchedEffect(heatmap.year) { hoveredDay = null }

    Column(Modifier.fillMaxWidth()) {
        // ── Header: year navigation + animated counters ──
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                enabled = heatmap.year > (availableYears.minOrNull() ?: heatmap.year),
                onClick = { onYearSelected(heatmap.year - 1) }
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous year", tint = surfaceColors.textPrimary) }

            AnimatedContent(
                targetState = heatmap.year,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        val forward = targetState > initialState
                        (fadeIn(tween(160)) +
                            slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) {
                                if (forward) it / 6 else -it / 6
                            }) togetherWith
                            (fadeOut(tween(120)) +
                                slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) {
                                    if (forward) -it / 6 else it / 6
                                })
                    }
                },
                label = "heatmap-year-label"
            ) { year ->
                Text(
                    "$year activity",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = surfaceColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }

            IconButton(
                enabled = heatmap.year < (availableYears.maxOrNull() ?: heatmap.year),
                onClick = { onYearSelected(heatmap.year + 1) }
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next year", tint = surfaceColors.textPrimary) }

            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedCount(heatmap.activeDays, surfaceColors.textPrimary)
                Text(" active days", fontSize = 11.sp, color = surfaceColors.textMuted)
                Text("  ·  ", fontSize = 11.sp, color = surfaceColors.textMuted)
                AnimatedCount(heatmap.totalReviews.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), accent.primary)
                Text(" reviews", fontSize = 11.sp, color = surfaceColors.textMuted)
                Text("  ·  ", fontSize = 11.sp, color = surfaceColors.textMuted)
                AnimatedCount(heatmap.currentStreak, LocalKaiteyoSemanticColors.current.favorite)
                Text("-day streak", fontSize = 11.sp, color = surfaceColors.textMuted)
            }
        }

        // ── Grid + floating tooltip overlay ──
        Box(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { boxCoords = it }
        ) {
            AnimatedContent(
                targetState = heatmap,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        val forward = targetState.year > initialState.year
                        val shift = { full: Int -> if (forward) full / 7 else -full / 7 }
                        (fadeIn(tween(140)) +
                            slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { shift(it) } +
                            scaleIn(tween(240), initialScale = 0.985f)) togetherWith
                            (fadeOut(tween(110)) +
                                slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { -shift(it) } +
                                scaleOut(tween(200), targetScale = 0.985f))
                    }
                },
                label = "heatmap-year"
            ) { target ->
                if (target.cells.isEmpty()) {
                    EmptyState(
                        title = "No study activity in ${target.year}",
                        message = "There is no recorded history for this year yet. Study something and it will appear here."
                    )
                    return@AnimatedContent
                }
                val weeks = remember(target.year) { buildYearWeeks(target.year) }
                val maxTotal = (target.cells.values.maxOfOrNull {
                    it.reviews + it.writingAttempts + it.examsTaken * 20
                } ?: 1).coerceAtLeast(1)

                HeatmapGrid(
                    weeks = weeks,
                    cells = target.cells,
                    accent = accent.primary,
                    surfaceColors = surfaceColors,
                    maxTotal = maxTotal,
                    scrollState = scrollState,
                    onHover = { hoveredDay = it },
                    onClick = { day ->
                        hoveredDay = null
                        onDayClick(day)
                    },
                    onPositioned = { d, coords -> coordsByDate[d] = coords }
                )
            }

            // ── Floating cursor-following tooltip ──
            val hovered = hoveredDay
            if (hovered != null && !hovered.isEmpty) {
                val cellCoords = coordsByDate[hovered.date]
                if (cellCoords != null) {
                    HoveredDayTooltip(
                        hovered = hovered,
                        cellCoords = cellCoords,
                        boxCoords = boxCoords,
                        tooltipSize = tooltipSize,
                        onTooltipSizeChanged = { tooltipSize = it }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        HeatmapLegend(surfaceColors.textMuted, accent.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            "Hover or focus a day for a live summary · tap / Enter for the full day report (every card you practiced).",
            fontSize = 10.sp,
            color = surfaceColors.textMuted
        )
    }
}

// ============================================================
// Grid — month labels · weekday gutter · fixed square cells
// ============================================================

@Composable
private fun HeatmapGrid(
    weeks: List<List<LocalDate?>>,
    cells: Map<LocalDate, DailyActivity>,
    accent: Color,
    surfaceColors: SurfaceColors,
    maxTotal: Int,
    scrollState: androidx.compose.foundation.ScrollState,
    onHover: (DailyActivity?) -> Unit,
    onClick: (DailyActivity) -> Unit,
    onPositioned: (LocalDate, LayoutCoordinates) -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    val emptyColor = surfaceColors.textMuted.copy(alpha = 0.10f)

    Row(verticalAlignment = Alignment.Top) {
        // Weekday gutter — Mon / Wed / Fri, aligned with the cell rows and
        // pushed below the month-label row so the grid lines up.
        Column(
            Modifier
                .width(HeatmapGutterWidth)
                .padding(top = HeatmapMonthRowHeight + 4.dp),
            verticalArrangement = Arrangement.spacedBy(HeatmapCellGap)
        ) {
            (0 until 7).forEach { dow ->
                val label = when (dow) {
                    1 -> "Mon"
                    3 -> "Wed"
                    5 -> "Fri"
                    else -> ""
                }
                Text(
                    label,
                    fontSize = 9.sp,
                    color = surfaceColors.textMuted,
                    modifier = Modifier
                        .width(HeatmapGutterWidth)
                        .height(HeatmapCellSize),
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Row(Modifier.horizontalScroll(scrollState), verticalAlignment = Alignment.Top) {
            Column {
                // Month labels — shown at the week column where each month starts
                // (its day 1), falling back to the week's first date when a week
                // contains no 1st.
                Row {
                    var prevMonth: Int? = null
                    weeks.forEach { week ->
                        val labelMonth = week.firstOrNull { it?.dayOfMonth == 1 }?.monthNumber
                            ?: week.firstOrNull()?.monthNumber
                        val label = if (labelMonth != null && labelMonth != prevMonth) {
                            Month.entries[labelMonth - 1].name.take(3)
                        } else {
                            ""
                        }
                        if (labelMonth != null) prevMonth = labelMonth
                        Text(
                            label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = surfaceColors.textMuted,
                            modifier = Modifier
                                .width(HeatmapCellSize)
                                .height(HeatmapMonthRowHeight),
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(HeatmapWeekGap)) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(HeatmapCellGap)) {
                            (0 until 7).forEach { dow ->
                                val date = week.getOrNull(dow) ?: return@forEach
                                val activity = cells[date]
                                HeatmapCell(
                                    date = date,
                                    activity = activity,
                                    accent = accent,
                                    maxTotal = maxTotal,
                                    emptyColor = emptyColor,
                                    shape = shape,
                                    onHover = onHover,
                                    onClick = onClick,
                                    onPositioned = { coords -> onPositioned(date, coords) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    date: LocalDate,
    activity: DailyActivity?,
    accent: Color,
    maxTotal: Int,
    emptyColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
    onHover: (DailyActivity?) -> Unit,
    onClick: (DailyActivity) -> Unit,
    onPositioned: (LayoutCoordinates) -> Unit
) {
    var isHovered by remember(date) { mutableStateOf(false) }
    var isFocused by remember(date) { mutableStateOf(false) }
    val enabled = activity != null && !activity.isEmpty

    Box(
        modifier = Modifier
            .size(HeatmapCellSize)
            .clip(shape)
            .background(activity?.heatmapLevelColor(accent, maxTotal, emptyColor) ?: Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isHovered || isFocused) accent.copy(alpha = 0.9f) else Color.Transparent,
                shape = shape
            )
            .onGloballyPositioned(onPositioned)
            .pointerInput(date, activity) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> {
                                isHovered = true
                                onHover(activity)
                            }
                            PointerEventType.Exit -> {
                                isHovered = false
                                onHover(null)
                            }
                            else -> {}
                        }
                    }
                }
            }
            // Keyboard access: empty days are skipped entirely (no tab stop),
            // active days can be focused and opened with Enter / Space. A
            // focused day also shows the live tooltip (same path as hover), so
            // keyboard users get the summary too — not just pointer users.
            .focusable(enabled = enabled)
            .onFocusChanged { focused ->
                isFocused = focused.isFocused
                // Report the focused day as the hovered day (and clear on
                // focus loss) so the tooltip follows keyboard navigation.
                if (enabled) onHover(if (focused.isFocused) activity else null)
            }
            .onKeyEvent { event ->
                if (enabled && event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.Spacebar)
                ) {
                    activity?.let(onClick)
                    true
                } else {
                    false
                }
            }
            .semantics {
                if (enabled && activity != null) {
                    role = Role.Button
                    contentDescription =
                        "${activity.date} — ${activity.reviews} reviews, " +
                            "${activity.studyTime.inWholeMinutes} minutes studied, " +
                            "${(activity.accuracy * 100).roundToInt()}% accuracy"
                }
            }
            .clickable(enabled = enabled) {
                activity?.let(onClick)
            }
    )
}

// ============================================================
// Floating tooltip
// ============================================================

@Composable
private fun HoveredDayTooltip(
    hovered: DailyActivity,
    cellCoords: LayoutCoordinates,
    boxCoords: LayoutCoordinates?,
    tooltipSize: IntSize,
    onTooltipSizeChanged: (IntSize) -> Unit
) {
    // Extracted to its own composable so the popup is not nested inside a
    // Column receiver (which would force the ColumnScope.AnimatedVisibility
    // overload and fail to compile inside the Box overlay).
    AnimatedVisibility(
        visible = true,
        modifier = Modifier
            .zIndex(2f)
            .offset {
                val box = boxCoords
                if (box == null) return@offset IntOffset.Zero
                // boundsInRoot() is live: it reflects the current
                // scroll translation on every evaluation.
                val boxRoot = box.boundsInRoot()
                val cellRoot = cellCoords.boundsInRoot()
                val anchorX = cellRoot.left - boxRoot.left + cellRoot.width / 2f
                val anchorY = cellRoot.top - boxRoot.top
                val w = tooltipSize.width.toFloat()
                val h = tooltipSize.height.toFloat()

                val x = if (boxRoot.width <= w + 8f) {
                    4f
                } else {
                    (anchorX - w / 2f).coerceIn(4f, boxRoot.width - w - 4f)
                }
                val above = anchorY - h - 8f
                val y = if (above >= 4f) {
                    above
                } else {
                    (anchorY + cellRoot.height + 8f)
                        .coerceAtMost((boxRoot.height - h - 4f).coerceAtLeast(4f))
                }
                IntOffset(x.roundToInt(), y.roundToInt())
            },
        enter = fadeIn(tween(130)) + scaleIn(tween(150), initialScale = 0.92f),
        exit = fadeOut(tween(90)) + scaleOut(tween(120), targetScale = 0.95f),
        label = "heatmap-tooltip"
    ) {
        FloatingDayTooltip(
            day = hovered,
            modifier = Modifier.onSizeChanged { onTooltipSizeChanged(it) }
        )
    }
}

@Composable
private fun FloatingDayTooltip(day: DailyActivity, modifier: Modifier = Modifier) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val sem = LocalKaiteyoSemanticColors.current

    Column(
        modifier
            .widthIn(min = 150.dp)
            .clip(RoundedCornerShape(12.dp))
            .shadow(10.dp, RoundedCornerShape(12.dp))
            .background(surfaceColors.surfaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                day.date?.toString() ?: "",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = surfaceColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${(day.accuracy * 100).roundToInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (day.accuracy >= 0.7f) sem.success else sem.error
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TooltipChip("${day.reviews}", "reviews", accent.primary)
            TooltipChip(minutesLabel(day.studyTime.inWholeMinutes), "time", sem.info)
            TooltipChip("${day.newCards}", "new", sem.new)
        }
        if (day.kanjiReviews > 0 || day.vocabReviews > 0 || day.writingAttempts > 0 || day.examsTaken > 0) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (day.kanjiReviews > 0) TooltipChip("${day.kanjiReviews}", "kanji", sem.success)
                if (day.vocabReviews > 0) TooltipChip("${day.vocabReviews}", "vocab", sem.favorite)
                if (day.writingAttempts > 0) TooltipChip("${day.writingAttempts}", "writing", sem.warning)
                if (day.examsTaken > 0) TooltipChip("${day.examsTaken}", "exams", sem.error)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Tap for the full day report", fontSize = 9.sp, color = surfaceColors.textMuted)
    }
}

@Composable
private fun TooltipChip(value: String, label: String, color: Color) {
    val surfaceColors = LocalSurfaceColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 9.sp, color = surfaceColors.textMuted)
    }
}

// ============================================================
// Animated counter (counts up to the real value)
// ============================================================

@Composable
private fun AnimatedCount(
    value: Int,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    val animated = remember { Animatable(value.toFloat()) }
    LaunchedEffect(value) {
        animated.animateTo(value.toFloat(), tween(650, easing = FastOutSlowInEasing))
    }
    Text(
        animated.value.roundToInt().toString(),
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color
    )
}

// ============================================================
// Legend & helpers
// ============================================================

@Composable
private fun HeatmapLegend(muted: Color, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Less", fontSize = 9.sp, color = muted)
        Spacer(Modifier.width(4.dp))
        // Level 0 (empty) plus the four intensity levels.
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(muted.copy(alpha = 0.10f))
        )
        Spacer(Modifier.width(2.dp))
        HeatmapLevelAlphas.forEach { alpha ->
            Box(
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.copy(alpha = alpha))
            )
            Spacer(Modifier.width(2.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text("More", fontSize = 9.sp, color = muted)
    }
}

/** Discrete 5-level ramp: empty → four accent intensities by share of the busiest day. */
private fun DailyActivity.heatmapLevelColor(
    accent: Color,
    maxTotal: Int,
    emptyColor: Color
): Color {
    val total = reviews + writingAttempts + examsTaken * 20
    if (total <= 0) return emptyColor
    if (maxTotal <= 0) return accent.copy(alpha = HeatmapLevelAlphas[0])
    val ratio = total.toFloat() / maxTotal
    val index = when {
        ratio <= 0.25f -> 0
        ratio <= 0.5f -> 1
        ratio <= 0.75f -> 2
        else -> 3
    }
    return accent.copy(alpha = HeatmapLevelAlphas[index])
}

private fun minutesLabel(minutes: Long): String = when {
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    minutes > 0 -> "${minutes}m"
    else -> "<1m"
}

/** Builds a GitHub-style week layout (Mon-Sun columns) for the given year. */
private fun buildYearWeeks(year: Int): List<List<LocalDate?>> {
    val jan1 = LocalDate(year, Month.JANUARY, 1)
    val dec31 = LocalDate(year, Month.DECEMBER, 31)
    val firstMondayOffset = ((jan1.dayOfWeek.isoDayNumber - 1) + 6) % 7
    val start = jan1.minus(firstMondayOffset, DateTimeUnit.DAY)

    val weeks = mutableListOf<List<LocalDate?>>()
    var cursor = start
    while (cursor <= dec31) {
        weeks.add(
            (0 until 7).map { offset ->
                val date = cursor.plus(offset, DateTimeUnit.DAY)
                date.takeIf { it.year == year }
            }
        )
        cursor = cursor.plus(7, DateTimeUnit.DAY)
    }
    return weeks
}


