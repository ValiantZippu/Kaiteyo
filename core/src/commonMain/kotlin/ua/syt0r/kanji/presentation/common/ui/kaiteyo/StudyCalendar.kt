package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors

// ============================================================
// STUDY CALENDAR — Windows-style monthly view with heatmap
//
// Features:
//   · Month navigation (prev/next)
//   · Study intensity heatmap (green gradient)
//   · Today highlight
//   · Selected day details
//   · Mini stats (streak, total reviews)
// ============================================================

@Immutable
data class StudyCalendarState(
    val currentMonth: LocalDate, // First day of displayed month
    val today: LocalDate,
    val selectedDay: LocalDate?,
    val studyData: Map<LocalDate, Int>, // date -> review count
    val streak: Int,
    val totalReviewsThisMonth: Int
) {
    companion object {
        fun calculate(): StudyCalendarState {
            val now = Clock.System.now()
            val tz = TimeZone.currentSystemDefault()
            val today = now.toLocalDateTime(tz).date
            val currentMonth = LocalDate(today.year, today.month, 1)

            // Generate mock study data for demo (replace with real data)
            val studyData = mutableMapOf<LocalDate, Int>()
            val daysInMonth = currentMonth.daysUntil(
                currentMonth.plus(1, DateTimeUnit.MONTH)
            )
            for (day in 0 until daysInMonth) {
                val date = currentMonth.plus(day, DateTimeUnit.DAY)
                if (date <= today) {
                    // Simulate study activity (random 0-50 reviews)
                    val hash = (date.dayOfMonth * 7 + date.monthNumber * 13) % 100
                    studyData[date] = when {
                        hash < 20 -> 0 // No study
                        hash < 50 -> (hash % 15) + 5 // Light
                        hash < 80 -> (hash % 30) + 20 // Medium
                        else -> (hash % 50) + 30 // Heavy
                    }
                }
            }

            // Calculate streak
            var streak = 0
            var checkDate = today
            while (checkDate >= currentMonth) {
                val reviews = studyData[checkDate] ?: 0
                if (reviews > 0) {
                    streak++
                    checkDate = checkDate.minus(1, DateTimeUnit.DAY)
                } else {
                    break
                }
            }

            val totalReviews = studyData.values.sum()

            return StudyCalendarState(
                currentMonth = currentMonth,
                today = today,
                selectedDay = null,
                studyData = studyData,
                streak = streak,
                totalReviewsThisMonth = totalReviews
            )
        }
    }
}

@Composable
fun StudyCalendar(
    state: StudyCalendarState,
    onMonthChange: (LocalDate) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var displayMonth by remember { mutableStateOf(state.currentMonth) }
    var selectedDay by remember { mutableStateOf(state.selectedDay) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surface)
            .border(1.dp, surfaceColors.surfaceInteractive.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = displayMonth.month.name.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = surfaceColors.textPrimary
                )
                Text(
                    text = "${displayMonth.year}",
                    fontSize = 12.sp,
                    color = surfaceColors.textMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        displayMonth = displayMonth.minus(1, DateTimeUnit.MONTH)
                        onMonthChange(displayMonth)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = surfaceColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        displayMonth = displayMonth.plus(1, DateTimeUnit.MONTH)
                        onMonthChange(displayMonth)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = surfaceColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Day of week headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calendar grid
        CalendarGrid(
            month = displayMonth,
            today = state.today,
            selectedDay = selectedDay,
            studyData = state.studyData,
            onDayClick = { day ->
                selectedDay = day
                onDayClick(day)
            }
        )

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Streak",
                value = "${state.streak} days",
                accent = accent.primary,
                surfaceColors = surfaceColors
            )
            StatItem(
                label = "This Month",
                value = "${state.totalReviewsThisMonth}",
                accent = accent.secondary,
                surfaceColors = surfaceColors
            )
            StatItem(
                label = "Avg/Day",
                value = if (state.streak > 0) "${state.totalReviewsThisMonth / maxOf(state.streak, 1)}" else "0",
                accent = Color(0xFF4CAF50),
                surfaceColors = surfaceColors
            )
        }

        // Heatmap legend
        HeatmapLegend(
            accent = accent.primary,
            surfaceColors = surfaceColors
        )
    }
}

@Composable
private fun CalendarGrid(
    month: LocalDate,
    today: LocalDate,
    selectedDay: LocalDate?,
    studyData: Map<LocalDate, Int>,
    onDayClick: (LocalDate) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    // Calculate first day of month and days in month
    val firstDayOfMonth = LocalDate(month.year, month.month, 1)
    val daysInMonth = firstDayOfMonth.daysUntil(
        firstDayOfMonth.plus(1, DateTimeUnit.MONTH)
    )
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.ordinal // 0=Monday

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // Weeks (max 6 rows)
        for (week in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (dayOfWeek in 0 until 7) {
                    val dayIndex = week * 7 + dayOfWeek - startDayOfWeek + 1
                    val day = if (dayIndex in 1..daysInMonth) {
                        LocalDate(month.year, month.month, dayIndex)
                    } else null

                    CalendarDayCell(
                        day = day,
                        today = today,
                        selectedDay = selectedDay,
                        studyCount = day?.let { studyData[it] } ?: 0,
                        onClick = { day?.let { onDayClick(it) } },
                        surfaceColors = surfaceColors,
                        accent = accent.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: LocalDate?,
    today: LocalDate,
    selectedDay: LocalDate?,
    studyCount: Int,
    onClick: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    if (day == null) {
        // Empty cell
        Box(modifier = modifier.aspectRatio(1f))
        return
    }

    val isToday = day == today
    val isSelected = day == selectedDay
    val isFuture = day > today

    // Heatmap intensity (0..1)
    val intensity = when {
        isFuture -> 0f
        studyCount == 0 -> 0f
        studyCount < 10 -> 0.2f
        studyCount < 20 -> 0.4f
        studyCount < 30 -> 0.6f
        studyCount < 40 -> 0.8f
        else -> 1f
    }

    val animatedIntensity by animateFloatAsState(
        targetValue = intensity,
        animationSpec = tween(300),
        label = "day_$day"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> accent.copy(alpha = 0.3f)
                    isToday -> accent.copy(alpha = 0.15f)
                    animatedIntensity > 0f -> Color(0xFF4CAF50).copy(alpha = animatedIntensity * 0.5f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (isToday) Modifier.border(1.5.dp, accent, CircleShape)
                else if (isSelected) Modifier.border(1.dp, accent.copy(alpha = 0.5f), CircleShape)
                else Modifier
            )
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${day.dayOfMonth}",
            fontSize = 12.sp,
            fontWeight = when {
                isToday -> FontWeight.Bold
                isSelected -> FontWeight.SemiBold
                else -> FontWeight.Normal
            },
            color = when {
                isToday -> accent
                isSelected -> accent
                isFuture -> surfaceColors.textMuted.copy(alpha = 0.5f)
                else -> surfaceColors.textPrimary
            }
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    accent: Color,
    surfaceColors: SurfaceColors
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = surfaceColors.textMuted
        )
    }
}

@Composable
private fun HeatmapLegend(
    accent: Color,
    surfaceColors: SurfaceColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Less",
            fontSize = 9.sp,
            color = surfaceColors.textMuted
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Legend squares
        listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f).forEach { intensity ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Color(0xFF4CAF50).copy(alpha = if (intensity == 0f) 0.1f else intensity * 0.5f)
                    )
            )
            Spacer(modifier = Modifier.width(2.dp))
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "More",
            fontSize = 9.sp,
            color = surfaceColors.textMuted
        )
    }
}

// ── Compact calendar for small spaces ────────────────────────

@Composable
fun StudyCalendarCompact(
    state: StudyCalendarState,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Month + streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = surfaceColors.textPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent.primary)
                )
                Text(
                    text = "${state.streak} day streak",
                    fontSize = 10.sp,
                    color = accent.primary
                )
            }
        }

        // Mini calendar (just the grid, no navigation)
        CalendarGrid(
            month = state.currentMonth,
            today = state.today,
            selectedDay = null,
            studyData = state.studyData,
            onDayClick = onDayClick
        )
    }
}
