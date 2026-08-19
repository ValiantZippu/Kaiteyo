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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import kotlin.math.roundToInt

// ============================================================
// STUDY HEATMAP — canonical shared component
//
// Used by:
//   - Home (compact mode)
//   - Stats (expanded mode)
//
// Receives:
//   - activity data (date → count)
//   - display mode (compact/expanded)
//   - interaction callbacks
//
// One implementation. Two presentations.
// ============================================================

@Immutable
data class HeatmapDayData(
    val date: LocalDate,
    val count: Int,
    val studyMinutes: Int = 0,
    val reviewsCount: Int = 0
)

@Immutable
enum class HeatmapDisplayMode {
    Compact,    // Home: last 3 months
    Expanded    // Stats: full year
}

// ── Canonical heatmap composable ─────────────────────────────

@Composable
fun StudyHeatmap(
    activityData: List<HeatmapDayData>,
    displayMode: HeatmapDisplayMode = HeatmapDisplayMode.Compact,
    onDayClick: (LocalDate) -> Unit = {},
    onDayHover: (LocalDate?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val countByDate = remember(activityData) {
        activityData.associate { it.date to it.count }
    }
    val maxCount = countByDate.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    // Determine date range based on display mode
    val (yearStart, totalWeeks) = remember(today, displayMode) {
        when (displayMode) {
            HeatmapDisplayMode.Compact -> {
                // Last 3 months
                val threeMonthsAgo = today.minus(3, DateTimeUnit.MONTH)
                val start = LocalDate(threeMonthsAgo.year, threeMonthsAgo.month, 1)
                val weeks = start.daysUntil(today) / 7 + 2
                start to weeks.coerceAtMost(14)
            }
            HeatmapDisplayMode.Expanded -> {
                // Full year
                val start = LocalDate(today.year, 1, 1)
                val weeks = 53
                start to weeks
            }
        }
    }

    val yearEnd = today
    val startOffset = yearStart.dayOfWeek.isoDayNumber - 1
    val totalDays = yearStart.daysUntil(yearEnd) + 1
    val totalCells = startOffset + totalDays
    val effectiveWeeks = (totalCells + 6) / 7

    val cellSize = when (displayMode) {
        HeatmapDisplayMode.Compact -> 10.dp
        HeatmapDisplayMode.Expanded -> 12.dp
    }
    val cellGap = 2.dp
    val shape = RoundedCornerShape(2.dp)

    var hoveredDay by remember { mutableStateOf<LocalDate?>(null) }
    var tooltipPosition by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .border(1.dp, surfaceColors.surfaceInteractive.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Header with legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (displayMode == HeatmapDisplayMode.Compact) "Activity" else "Study Activity",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textPrimary
                )
                if (displayMode == HeatmapDisplayMode.Expanded) {
                    Text(
                        text = "Review consistency over time",
                        fontSize = 9.sp,
                        color = surfaceColors.textMuted
                    )
                }
            }
            // Legend
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Less", fontSize = 8.sp, color = surfaceColors.textMuted)
                Spacer(Modifier.width(3.dp))
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(accent.primary.copy(alpha = 0.08f))
                )
                HeatmapLevelAlphas.forEach { alpha ->
                    Box(
                        Modifier
                            .padding(horizontal = 1.dp)
                            .size(7.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(accent.primary.copy(alpha = alpha))
                    )
                }
                Spacer(Modifier.width(3.dp))
                Text("More", fontSize = 8.sp, color = surfaceColors.textMuted)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (countByDate.isEmpty()) {
            Text(
                text = "No review activity yet — start studying to see your progress.",
                fontSize = 11.sp,
                color = surfaceColors.textMuted
            )
            return@Column
        }

        // Weekday labels + grid
        Row(verticalAlignment = Alignment.Top) {
            // Day-of-week gutter
            Column(
                Modifier.width(20.dp),
                verticalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                Spacer(Modifier.height(12.dp)) // month label spacer
                listOf("", "M", "", "W", "", "F", "").forEach { label ->
                    Text(
                        label,
                        fontSize = 7.sp,
                        color = surfaceColors.textMuted,
                        modifier = Modifier
                            .width(20.dp)
                            .height(cellSize),
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(3.dp))

            Column {
                // Month labels row
                if (displayMode == HeatmapDisplayMode.Expanded) {
                    Row {
                        var prevMonth: Int? = null
                        for (week in 0 until effectiveWeeks) {
                            val weekStartDate = yearStart.minus(startOffset, DateTimeUnit.DAY)
                                .plus(week * 7L, DateTimeUnit.DAY)
                            val labelMonth = (0..6).mapNotNull { dow ->
                                val date = weekStartDate.plus(dow.toLong(), DateTimeUnit.DAY)
                                if (date >= yearStart && date <= yearEnd && date.monthNumber != prevMonth) {
                                    date.monthNumber
                                } else null
                            }.firstOrNull()

                            val label = if (labelMonth != null && labelMonth != prevMonth) {
                                kotlinx.datetime.Month.entries[labelMonth - 1].name.take(1)
                            } else ""

                            if (labelMonth != null) prevMonth = labelMonth

                            Text(
                                label,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Medium,
                                color = surfaceColors.textMuted,
                                modifier = Modifier
                                    .width(cellSize + cellGap)
                                    .height(12.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }

                // Day grid
                for (dow in 0 until 7) {
                    Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                        for (week in 0 until effectiveWeeks) {
                            val cellIndex = week * 7 + dow - startOffset
                            val dayNum = cellIndex - startOffset + 1
                            val date = if (dayNum in 1..totalDays) {
                                yearStart.plus((dayNum - 1).toLong(), DateTimeUnit.DAY)
                            } else null

                            val isValidDate = date != null && date >= yearStart && date <= yearEnd
                            val isToday = date == today

                            if (isValidDate && date != null) {
                                val count = countByDate[date] ?: 0
                                val alpha = if (count <= 0) 0.06f
                                else heatmapLevelAlpha(count, maxCount)

                                val interactionSource = remember { MutableInteractionSource() }
                                val isHovered by interactionSource.collectIsHoveredAsState()

                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clip(shape)
                                        .background(
                                            when {
                                                isToday -> accent.primary
                                                else -> accent.primary.copy(alpha = alpha)
                                            }
                                        )
                                        .then(
                                            if (isToday) Modifier.border(1.dp, accent.primary, shape)
                                            else Modifier
                                        )
                                        .hoverable(interactionSource)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            enabled = count > 0,
                                            onClick = { onDayClick(date) }
                                        )
                                        .graphicsLayer {
                                            if (isHovered) {
                                                scaleX = 1.3f
                                                scaleY = 1.3f
                                                translationX = -cellSize.toPx() * 0.15f
                                                translationY = -cellSize.toPx() * 0.15f
                                            }
                                        }
                                )
                            } else {
                                Box(Modifier.size(cellSize))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Summary
        val yearTotal = countByDate.filterKeys { it.year == today.year }.values.sum()
        val activeDays = countByDate.filter { it.key.year == today.year && it.value > 0 }.size
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$yearTotal reviews across $activeDays days in ${today.year}",
                fontSize = 9.sp,
                color = surfaceColors.textMuted
            )
            Spacer(Modifier.weight(1f))
            if (displayMode == HeatmapDisplayMode.Compact) {
                Text(
                    text = "View full stats →",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = accent.primary
                )
            }
        }
    }
}

// ── Tooltip for heatmap day ──────────────────────────────────

@Composable
fun HeatmapDayTooltip(
    data: HeatmapDayData,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceColors.surfaceElevated)
            .border(1.dp, surfaceColors.surfaceInteractive, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${data.date.dayOfWeek.name.take(3)} ${data.date.month.name.take(3)} ${data.date.dayOfMonth}",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = surfaceColors.textPrimary
        )
        if (data.studyMinutes > 0) {
            Text(
                text = "${data.studyMinutes} min studied",
                fontSize = 9.sp,
                color = surfaceColors.textSecondary
            )
        }
        if (data.reviewsCount > 0) {
            Text(
                text = "${data.reviewsCount} reviews",
                fontSize = 9.sp,
                color = surfaceColors.textSecondary
            )
        }
        if (data.count > 0) {
            Text(
                text = "${data.count} items",
                fontSize = 9.sp,
                color = accent.primary
            )
        }
    }
}

// ── Color ramp ───────────────────────────────────────────────

private val HeatmapLevelAlphas = floatArrayOf(0.12f, 0.30f, 0.55f, 1f)

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
