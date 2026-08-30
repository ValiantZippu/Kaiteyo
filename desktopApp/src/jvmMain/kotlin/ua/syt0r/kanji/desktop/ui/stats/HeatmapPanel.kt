package ua.syt0r.kanji.desktop.ui.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.errorColor
import ua.syt0r.kanji.desktop.designsystem.infoColor
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.desktop.engine.history.ActivityFormatters
import ua.syt0r.kanji.desktop.engine.stats.DayActivity
import ua.syt0r.kanji.desktop.engine.stats.DayActivityEngine
import ua.syt0r.kanji.desktop.engine.stats.HeatmapEngine
import ua.syt0r.kanji.desktop.engine.stats.RatingBreakdown
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.StudyDaySummary
import kotlin.time.Duration

// ============================================
// INTERACTIVE ACTIVITY HEATMAP
// GitHub-style intensity grid. Hovering a day
// shows a clean summary tooltip; clicking opens
// the full day-detail history dialog.
// ============================================

private val WeekdayLabels = listOf("Mon", "", "Wed", "", "Fri", "", "")

private val cellSize = 14.dp
private val cellGap = 3.dp

private data class HeatmapViewKey(val scope: String, val year: Int)

@Composable
fun HeatmapPanel(state: AppState, summaries: List<StudyDaySummary>) {
    val sc = surfaceColors()
    val ac = accent()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    // View scope: null = rolling 52 weeks; otherwise a full calendar year.
    var yearScope by remember { mutableStateOf<Int?>(null) }
    // Years offered: the current year plus the three previous ones (any of
    // them may be empty — blank cells are honest).
    val availableYears = remember(today.year) { (today.year downTo today.year - 3).toList() }
    val key = HeatmapViewKey(if (yearScope == null) "weeks" else "year", yearScope ?: today.year)

    var hoveredDate by remember { mutableStateOf<LocalDate?>(null) }
    val cellPositions = remember { mutableStateMapOf<String, IntOffset>() }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val reducedMotion = state.navReducedMotion || state.settings.getBool("appearance.reduced-motion")
    val transitionMs = if (reducedMotion) 0 else 300
    val slideMotion = tween<IntOffset>(transitionMs)
    val fadeMotion = tween<Float>(transitionMs)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            // Scope chips: rolling 52 weeks ↔ calendar years. Switching years
            // animates as a push/slide between two real calendars.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeatmapScopeChip(
                    label = "52 weeks",
                    selected = yearScope == null,
                    onClick = { yearScope = null }
                )
                availableYears.forEach { year ->
                    HeatmapScopeChip(
                        label = year.toString(),
                        selected = yearScope == year,
                        onClick = { yearScope = year }
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${HeatmapEngine.currentStreak(summaries)}-day streak · ${summaries.size} active days",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Column(verticalArrangement = Arrangement.spacedBy(cellGap), modifier = Modifier.padding(top = DsSpacing.Sm)) {
                    WeekdayLabels.forEachIndexed { index, label ->
                        if (label.isEmpty()) Spacer(Modifier.height(cellSize))
                        else Text(
                            text = label,
                            color = sc.textMuted,
                            fontSize = DsType.Caption,
                            modifier = Modifier
                                .height(cellSize)
                                .width(cellSize + 8.dp)
                                .padding(top = 2.dp)
                        )
                    }
                }
                AnimatedContent(
                    targetState = key,
                    transitionSpec = {
                        val forward = targetState.year > initialState.year ||
                            (initialState.scope == "weeks" && targetState.scope == "year")
                        if (forward) {
                            (slideInHorizontally(slideMotion) { it } + fadeIn(fadeMotion)) togetherWith
                                (slideOutHorizontally(slideMotion) { -it / 3 } + fadeOut(fadeMotion))
                        } else {
                            (slideInHorizontally(slideMotion) { -it } + fadeIn(fadeMotion)) togetherWith
                                (slideOutHorizontally(slideMotion) { it / 3 } + fadeOut(fadeMotion))
                        }
                    },
                    label = "heatmapYear"
                ) { targetKey ->
                    val targetGrid = remember(targetKey) {
                        if (targetKey.scope == "weeks") HeatmapEngine.buildAligned(summaries, today)
                        else HeatmapEngine.buildAlignedYear(summaries, targetKey.year, today)
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.Top
                    ) {
                        targetGrid.weeks.forEachIndexed { index, week ->
                            val label = if (index == 0 || week.monthLabel != targetGrid.weeks[index - 1].monthLabel) week.monthLabel else ""
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption,
                                    modifier = Modifier.height(DsSpacing.Lg)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                                    week.days.forEach { day ->
                                        HeatCell(
                                            day = day,
                                            size = cellSize,
                                            onHover = { hoveredDate = it },
                                            onHoverExit = {
                                                if (hoveredDate == day?.date) hoveredDate = null
                                            },
                                            onClick = { if (day != null) selectedDate = day.date },
                                            onPosition = { date, pos -> cellPositions[date] = pos }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                Text("Less", color = sc.textMuted, fontSize = DsType.Caption)
                repeat(5) { level ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(heatColor(level, sc, ac))
                    )
                }
                Text("More", color = sc.textMuted, fontSize = DsType.Caption)
            }
        }

        hoveredDate?.let { date ->
            val pos = cellPositions[date.toString()]
            val activity = remember(date) {
                DayActivityEngine.forDay(date, summaries, state.reviewLog.toList(), state.cards.toList(), state.activityLog)
            }
            if (pos != null) {
                Popup(
                    offset = pos + IntOffset(0, 28),
                    properties = PopupProperties(focusable = false),
                    onDismissRequest = {}
                ) {
                    HeatTooltip(activity)
                }
            }
        }
    }

    selectedDate?.let { date ->
        DayDetailDialog(
            activity = remember(date) {
                DayActivityEngine.forDay(date, summaries, state.reviewLog.toList(), state.cards.toList(), state.activityLog)
            },
            collections = state.collections,
            onDismiss = { selectedDate = null }
        )
    }
}

@Composable
private fun HeatCell(
    day: HeatmapEngine.AlignedDay?,
    size: Dp,
    onHover: (LocalDate) -> Unit,
    onHoverExit: () -> Unit,
    onClick: () -> Unit,
    onPosition: (String, IntOffset) -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val color = if (day == null) sc.surfaceInteractive.copy(alpha = 0.35f) else heatColor(day.level, sc, ac)

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(if (hovered && day != null) Color.White.copy(alpha = 0.85f) else color)
            .hoverable(interaction)
            .onGloballyPositioned { coords ->
                if (day != null) onPosition(day.date.toString(), coords.positionInWindow().round())
            }
            .clickable { onClick() }
    )

    LaunchedEffect(hovered, day) {
        if (hovered && day != null) onHover(day.date)
        else if (!hovered) onHoverExit()
    }
}

// ============================================
// TOOLTIP
// ============================================

@Composable
private fun HeatTooltip(activity: DayActivity) {
    val sc = surfaceColors()
    val ac = accent()
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surfaceElevated)
            .border(1.dp, sc.border.copy(alpha = 0.6f), RoundedCornerShape(DsRadius.Md))
            .padding(DsSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        Text(
            text = formatFullDate(activity.date),
            color = sc.textPrimary,
            fontSize = DsType.Label,
            fontWeight = FontWeight.SemiBold
        )
        val total = activity.reviews + activity.newCards
        Text(
            text = when {
                total == 0 -> "No study activity"
                else -> "$total cards · ${activity.reviews} reviews · ${activity.newCards} new"
            },
            color = if (total == 0) sc.textMuted else ac.primary,
            fontSize = DsType.Body
        )
        if (total > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Column {
                    Text("Time", color = sc.textMuted, fontSize = DsType.Caption)
                    Text(formatDuration(activity.timeSpent), color = sc.textPrimary, fontSize = DsType.Body)
                }
                Column {
                    Text("Accuracy", color = sc.textMuted, fontSize = DsType.Caption)
                    Text("${(activity.accuracy * 100).toInt()}%", color = sc.textPrimary, fontSize = DsType.Body)
                }
                Column {
                    Text("Decks", color = sc.textMuted, fontSize = DsType.Caption)
                    Text(activity.decks.size.toString(), color = sc.textPrimary, fontSize = DsType.Body)
                }
            }
            Text("Click to open day details →", color = sc.textMuted, fontSize = DsType.Caption)
        }
    }
}

// ============================================
// DAY DETAIL DIALOG
// A full history browser for a single day.
// ============================================

@Composable
fun DayDetailDialog(
    activity: DayActivity,
    collections: ua.syt0r.kanji.desktop.engine.collections.CollectionStore,
    onDismiss: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val dayCardIds = activity.cards.map { it.id }.toSet()
    val collectionsHit = collections.collections
        .filter { collections.resolveCards(it, activity.cards).isNotEmpty() }
        .map { it.name }
    val tags = activity.cards.flatMap { it.tags }.distinct().sorted()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(680.dp)
                .clip(RoundedCornerShape(DsRadius.Xl))
                .background(sc.surfaceElevated)
                .padding(DsSpacing.Xl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = formatFullDate(activity.date),
                        color = sc.textPrimary,
                        fontSize = DsType.Heading,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (activity.reviews + activity.newCards == 0) "No study activity recorded" else "Study day overview",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Close", color = ac.primary)
                }
            }
            Spacer(Modifier.height(DsSpacing.Lg))

            Column(
                modifier = Modifier
                    .height(520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DayStat("Reviews", activity.reviews.toString(), Modifier.weight(1f))
                    DayStat("New cards", activity.newCards.toString(), Modifier.weight(1f))
                    DayStat("Forgotten", activity.forgotten.toString(), Modifier.weight(1f))
                    DayStat("Study time", formatDuration(activity.timeSpent), Modifier.weight(1f))
                    DayStat("Accuracy", "${(activity.accuracy * 100).toInt()}%", Modifier.weight(1f))
                    DayStat("Decks", activity.decks.size.toString(), Modifier.weight(1f))
                }

                RatingPanel(activity.rating)

                if (tags.isNotEmpty() || collectionsHit.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        Text("Context", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                            if (tags.isEmpty()) {
                                DsBadge(text = "No tags", tint = sc.textMuted)
                            } else {
                                tags.forEach { tag -> DsBadge(text = tag, tint = ac.primary) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                            if (collectionsHit.isEmpty()) {
                                DsBadge(text = "No collections", tint = sc.textMuted)
                            } else {
                                collectionsHit.forEach { name -> DsBadge(text = name, tint = infoColor()) }
                            }
                        }
                    }
                }

                if (activity.cards.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        Text("Cards studied", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                        activity.cards.forEach { card ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(DsRadius.Md))
                                    .background(sc.surface)
                                    .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(card.character, color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(card.meaning, color = sc.textSecondary, fontSize = DsType.Body, maxLines = 1)
                                    Text(card.readings.take(2).joinToString(" · "), color = sc.textMuted, fontSize = DsType.Caption, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                if (activity.activityEntries.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        Text("Timeline", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                        activity.activityEntries.take(30).forEach { entry ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = ActivityFormatters.relative(entry.timestamp),
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption,
                                    modifier = Modifier.width(72.dp)
                                )
                                Text(
                                    text = entry.summary,
                                    color = sc.textSecondary,
                                    fontSize = DsType.Body,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
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
private fun DayStat(label: String, value: String, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surface)
            .padding(DsSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        Text(label.uppercase(), color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.Medium)
        Text(value, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RatingPanel(breakdown: RatingBreakdown) {
    val sc = surfaceColors()
    val ratings = listOf(
        ReviewRating.Again to errorColor(),
        ReviewRating.Hard to warningColor(),
        ReviewRating.Good to infoColor(),
        ReviewRating.Easy to successColor()
    )
    val max = maxOf(1, breakdown.total)
    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text("Answer distribution", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
        ratings.forEach { (rating, color) ->
            val count = breakdown.count(rating)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rating.displayName, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(64.dp))
                DsProgressBar(
                    fraction = count.toFloat() / max,
                    modifier = Modifier.weight(1f),
                    color = color
                )
                Spacer(Modifier.width(DsSpacing.Sm))
                Text(count.toString(), color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(32.dp))
            }
        }
    }
}

// ============================================
// HELPERS
// ============================================

/** Scope chip for the heatmap's 52-week ↔ year switching. */
@Composable
private fun HeatmapScopeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) ac.primary.copy(alpha = 0.16f) else sc.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = DsSpacing.Md, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) ac.primary else sc.textSecondary,
            fontSize = DsType.Caption,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
internal fun heatColor(level: Int, sc: ua.syt0r.kanji.presentation.common.theme.SurfaceColors, ac: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme): Color = when (level) {
    0 -> sc.surfaceInteractive
    1 -> ac.primary.copy(alpha = 0.25f)
    2 -> ac.primary.copy(alpha = 0.45f)
    3 -> ac.primary.copy(alpha = 0.7f)
    else -> ac.primary
}

@Composable
internal fun formatFullDate(date: LocalDate): String {
    val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$dayName, $monthName ${date.dayOfMonth}, ${date.year}"
}

internal fun formatDuration(duration: Duration): String {
    val minutes = duration.inWholeMinutes
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        minutes < 1 -> "${duration.inWholeSeconds}s"
        hours == 0L -> "${mins}m"
        mins == 0L -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}
