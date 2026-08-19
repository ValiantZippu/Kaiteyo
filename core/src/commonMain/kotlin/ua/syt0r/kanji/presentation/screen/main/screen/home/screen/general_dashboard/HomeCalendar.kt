package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// FULL-YEAR HEATMAP + MONTHLY CALENDAR
//
// Top: 52-week GitHub contribution grid showing the entire year
//      of review activity (Mon–Sun rows, week columns)
// Bottom: Monthly mini-calendar with date numbers for navigation
// ============================================================

// ──────────────────────────────────────────────────────────────
// FULL-YEAR HEATMAP — GitHub-style contribution grid
// Shows all 365 days in a compact 7-row × 52-column grid
// ──────────────────────────────────────────────────────────────

@Composable
fun FullYearHeatmap(
    stats: GeneralDashboardStats,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val countByDate = remember(stats) {
        stats.heatmapSummary.associate { it.date to it.count }
    }
    val maxCount = countByDate.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    // Build the full year: start from Jan 1 of current year (or earliest data)
    val yearStart = remember(today) { LocalDate(today.year, 1, 1) }
    val yearEnd = remember(today) { LocalDate(today.year, 12, 31) }

    // Pad to start on Monday
    val startOffset = yearStart.dayOfWeek.isoDayNumber - 1
    val totalDays = yearStart.daysUntil(yearEnd) + 1
    val totalCells = startOffset + totalDays
    val totalWeeks = (totalCells + 6) / 7

    val cellSize = 11.dp
    val cellGap = 2.dp
    val monthRowHeight = 14.dp
    val dayLabelWidth = 22.dp
    val shape = RoundedCornerShape(2.5.dp)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = surfaceColors.textPrimary
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Less",
                fontSize = 9.sp,
                color = surfaceColors.textMuted
            )
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .padding(horizontal = 1.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.primary.copy(alpha = 0.10f))
            )
            HeatmapLevelAlphas.forEach { alpha ->
                Box(
                    Modifier
                        .padding(horizontal = 1.dp)
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent.primary.copy(alpha = alpha))
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "More",
                fontSize = 9.sp,
                color = surfaceColors.textMuted
            )
        }

        Spacer(Modifier.height(8.dp))

        if (countByDate.isEmpty()) {
            Text(
                text = "No review activity yet — start studying to see your progress here.",
                style = MaterialTheme.typography.bodySmall,
                color = surfaceColors.textMuted
            )
            return@Column
        }

        // Weekday labels (Mon/Wed/Fri on left)
        Row(verticalAlignment = Alignment.Top) {
            // Day-of-week gutter
            Column(
                Modifier.width(dayLabelWidth),
                verticalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                // Month label spacer
                Spacer(Modifier.height(monthRowHeight + 2.dp))
                listOf("", "Mon", "", "Wed", "", "Fri", "").forEach { label ->
                    Text(
                        label,
                        fontSize = 8.sp,
                        color = surfaceColors.textMuted,
                        modifier = Modifier
                            .width(dayLabelWidth)
                            .height(cellSize),
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            Column {
                // Month labels row
                Row {
                    var prevMonth: Int? = null
                    for (week in 0 until totalWeeks) {
                        // Find the date that starts this week
                        val weekStartDate = yearStart.minus(startOffset, DateTimeUnit.DAY)
                            .plus(week * 7L, DateTimeUnit.DAY)
                        val labelMonth = (0..6).mapNotNull { dow ->
                            val date = weekStartDate.plus(dow.toLong(), DateTimeUnit.DAY)
                            if (date >= yearStart && date <= yearEnd && date.monthNumber != prevMonth) {
                                date.monthNumber
                            } else null
                        }.firstOrNull()

                        val label = if (labelMonth != null && labelMonth != prevMonth) {
                            Month.entries[labelMonth - 1].name.take(1)
                        } else ""

                        if (labelMonth != null) prevMonth = labelMonth

                        Text(
                            label,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = surfaceColors.textMuted,
                            modifier = Modifier
                                .width(cellSize + cellGap)
                                .height(monthRowHeight),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Day grid: 7 rows × totalWeeks columns
                for (dow in 0 until 7) {
                    Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                        for (week in 0 until totalWeeks) {
                            val cellIndex = week * 7 + dow - startOffset
                            val dayNum = cellIndex - startOffset + 1
                            val date = if (dayNum in 1..totalDays) {
                                LocalDate(today.year, 1, 1).plus((dayNum - 1).toLong(), DateTimeUnit.DAY)
                            } else null

                            val isValidDate = date != null && date >= yearStart && date <= yearEnd
                            val isToday = date == today

                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(shape)
                                    .background(
                                        when {
                                            !isValidDate -> Color.Transparent
                                            isToday -> accent.primary
                                            else -> {
                                                val count = countByDate[date] ?: 0
                                                if (count <= 0) accent.primary.copy(alpha = 0.06f)
                                                else accent.primary.copy(alpha = heatmapLevelAlpha(count, maxCount))
                                            }
                                        }
                                    )
                                    .then(
                                        if (isToday) Modifier.border(1.dp, accent.primary, shape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Summary row
        val yearTotal = countByDate.filterKeys { it.year == today.year }.values.sum()
        val activeDays = countByDate.filter { it.key.year == today.year && it.value > 0 }.size
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$yearTotal reviews across $activeDays days in ${today.year}",
                fontSize = 10.sp,
                color = surfaceColors.textMuted
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "this year",
                fontSize = 9.sp,
                color = surfaceColors.textMuted
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// MONTHLY CALENDAR — navigable month grid with review counts
// ──────────────────────────────────────────────────────────────

@Composable
fun HomeCalendarCard(
    stats: GeneralDashboardStats,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val countByDate = remember(stats) {
        stats.heatmapSummary.associate { it.date to it.count }
    }
    val maxCount = countByDate.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    val todayMonth = LocalDate(today.year, today.month, 1)
    val dataEarliest = countByDate.keys.minOrNull()
    val minMonth = dataEarliest?.let { LocalDate(it.year, it.month, 1) } ?: todayMonth

    var shownMonth by remember { mutableStateOf(todayMonth) }

    val firstDay = shownMonth
    val nextMonth = shownMonth.plus(1, DateTimeUnit.MONTH)
    val daysInMonth = firstDay.daysUntil(nextMonth)
    val leadingBlanks = firstDay.dayOfWeek.isoDayNumber - 1
    val totalCells = leadingBlanks + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .border(1.dp, surfaceColors.surfaceInteractive.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header with month navigation
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = "Calendar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textPrimary
                )
                Text(
                    text = "review activity per day",
                    fontSize = 9.sp,
                    color = surfaceColors.textMuted
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { shownMonth = shownMonth.minus(1, DateTimeUnit.MONTH) },
                enabled = shownMonth > minMonth
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = surfaceColors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "${Month.entries[shownMonth.monthNumber - 1].name.take(3).uppercase()} ${shownMonth.year}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = surfaceColors.textPrimary,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { shownMonth = shownMonth.plus(1, DateTimeUnit.MONTH) },
                enabled = shownMonth < todayMonth
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = surfaceColors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Weekday header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Day grid
        (0 until rows).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                (0 until 7).forEach { col ->
                    val dayNumber = row * 7 + col - leadingBlanks + 1
                    val date = if (dayNumber in 1..daysInMonth) {
                        LocalDate(shownMonth.year, shownMonth.month, dayNumber)
                    } else null

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            CalendarDayCell(
                                date = date,
                                count = countByDate[date] ?: 0,
                                maxCount = maxCount,
                                isToday = date == today,
                                isFuture = date > today,
                                onOpenDay = onOpenDay
                            )
                        } else {
                            Spacer(Modifier.size(28.dp))
                        }
                    }
                }
            }
        }

        // Legend
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Less", fontSize = 9.sp, color = surfaceColors.textMuted)
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .padding(horizontal = 1.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.primary.copy(alpha = 0.10f))
            )
            CalendarLevelAlphas.forEach { alpha ->
                Box(
                    Modifier
                        .padding(horizontal = 1.dp)
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent.primary.copy(alpha = alpha))
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("More", fontSize = 9.sp, color = surfaceColors.textMuted)
            Spacer(Modifier.weight(1f))
            Text(
                text = "today",
                fontSize = 9.sp,
                color = accent.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Day cell composable (shared by calendar)
// ──────────────────────────────────────────────────────────────

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    count: Int,
    maxCount: Int,
    isToday: Boolean,
    isFuture: Boolean,
    onOpenDay: (LocalDate) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val background = when {
        isToday -> accent.primary
        isFuture -> surfaceColors.surfaceInteractive.copy(alpha = 0.20f)
        count <= 0 -> surfaceColors.surfaceInteractive.copy(alpha = 0.30f)
        else -> accent.primary.copy(alpha = calendarLevelAlpha(count, maxCount))
    }
    val textColor = when {
        isToday -> accent.onPrimary
        hovered -> accent.primary
        else -> surfaceColors.textSecondary
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .then(
                if (isToday) Modifier.border(1.5.dp, accent.primary, RoundedCornerShape(8.dp))
                else Modifier
            )
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isFuture && count > 0,
                onClick = { onOpenDay(date) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 10.sp,
            fontWeight = if (isToday || count > 0) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Color ramps
// ──────────────────────────────────────────────────────────────

private fun heatmapLevelAlpha(count: Int, maxCount: Int): Float {
    if (maxCount <= 0) return HeatmapLevelAlphas[0]
    val ratio = count.toFloat() / maxCount
    return when {
        ratio <= 0.25f -> HeatmapLevelAlphas[0]
        ratio <= 0.5f -> HeatmapLevelAlphas[1]
        ratio <= 0.75f -> HeatmapLevelAlphas[2]
        else -> HeatmapLevelAlphas[3]
    }
}

private fun calendarLevelAlpha(count: Int, maxCount: Int): Float {
    if (maxCount <= 0) return CalendarLevelAlphas[0]
    val ratio = count.toFloat() / maxCount
    return when {
        ratio <= 0.25f -> CalendarLevelAlphas[0]
        ratio <= 0.5f -> CalendarLevelAlphas[1]
        ratio <= 0.75f -> CalendarLevelAlphas[2]
        else -> CalendarLevelAlphas[3]
    }
}

private val HeatmapLevelAlphas = floatArrayOf(0.15f, 0.35f, 0.60f, 1f)
private val CalendarLevelAlphas = floatArrayOf(0.28f, 0.48f, 0.72f, 1f)
