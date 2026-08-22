package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors

// ============================================================
// TIME PROGRESS SYSTEM
//
// Multi-scale temporal visualization:
//   YEAR → MONTH → WEEK → DAY → HOUR
//
// Each scale renders as a gradient progress bar with:
//   - elapsed (filled) / remaining (empty) ratio
//   - current-position marker
//   - label + remaining time text
//   - subtle glow at the current point
//
// The domain model is pure Kotlin (no Compose dependencies)
// so it can be tested and reused in Stats.
// ============================================================

// ── Domain model ─────────────────────────────────────────────

@Immutable
data class TimeProgressState(
    val year: ScaleProgress,
    val month: ScaleProgress,
    val week: ScaleProgress,
    val day: ScaleProgress,
    val hour: ScaleProgress,
    val isDaytime: Boolean,
    val currentTime: String,
    val currentDate: String,
    val dayOfWeek: String
)

@Immutable
data class ScaleProgress(
    val label: String,
    val elapsed: Int,
    val total: Int,
    val remaining: Int,
    val remainingLabel: String,
    val fraction: Float // 0..1
) {
    companion object {
        fun of(elapsed: Int, total: Int, label: String, remainingLabel: String): ScaleProgress {
            val remaining = (total - elapsed).coerceAtLeast(0)
            val fraction = if (total > 0) elapsed.toFloat() / total else 0f
            return ScaleProgress(
                label = label,
                elapsed = elapsed,
                total = total,
                remaining = remaining,
                remainingLabel = remainingLabel,
                fraction = fraction.coerceIn(0f, 1f)
            )
        }
    }
}

/**
 * Calculate the current time progress state from the system clock.
 * Safe to call from any coroutine; pure calculations.
 */
fun calculateTimeProgress(): TimeProgressState {
    val now = Clock.System.now()
    val tz = TimeZone.currentSystemDefault()
    val localDateTime = now.toLocalDateTime(tz)
    val date = localDateTime.date
    val time = localDateTime.time

    // ── YEAR ──────────────────────────────────────
    val yearStart = LocalDate(date.year, 1, 1)
    val yearEnd = LocalDate(date.year, 12, 31)
    val yearTotal = yearStart.daysUntil(yearEnd) + 1
    val yearElapsed = yearStart.daysUntil(date) + 1
    val yearRemaining = yearTotal - yearElapsed

    // ── MONTH ─────────────────────────────────────
    val monthStart = LocalDate(date.year, date.month, 1)
    val nextMonth = monthStart.plus(1, DateTimeUnit.MONTH)
    val monthTotal = monthStart.daysUntil(nextMonth)
    val monthElapsed = date.dayOfMonth
    val monthRemaining = monthTotal - monthElapsed

    // ── WEEK (Monday-start) ───────────────────────
    val dayOfWeekNum = date.dayOfWeek.isoDayNumber // 1=Mon, 7=Sun
    val weekElapsed = dayOfWeekNum
    val weekTotal = 7
    val weekRemaining = weekTotal - weekElapsed

    // ── DAY ───────────────────────────────────────
    val dayElapsed = time.hour
    val dayTotal = 24
    val dayRemaining = dayTotal - dayElapsed

    // ── HOUR ──────────────────────────────────────
    val hourElapsed = time.minute
    val hourTotal = 60
    val hourRemaining = hourTotal - hourElapsed

    // ── DAY/NIGHT ─────────────────────────────────
    val isDaytime = time.hour in 6..18

    // ── FORMATTING ────────────────────────────────
    val currentTimeStr = "%02d:%02d".format(time.hour, time.minute)
    val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val monthName = kotlinx.datetime.Month.entries[date.monthNumber - 1].name
        .lowercase().replaceFirstChar { it.uppercase() }
    val currentDateStr = "$dayName, ${date.dayOfMonth} $monthName"

    return TimeProgressState(
        year = ScaleProgress.of(
            elapsed = yearElapsed,
            total = yearTotal,
            label = "YEAR ${date.year}",
            remainingLabel = "$yearRemaining days left"
        ),
        month = ScaleProgress.of(
            elapsed = monthElapsed,
            total = monthTotal,
            label = "MONTH",
            remainingLabel = "$monthRemaining days left"
        ),
        week = ScaleProgress.of(
            elapsed = weekElapsed,
            total = weekTotal,
            label = "WEEK",
            remainingLabel = "$weekRemaining days left"
        ),
        day = ScaleProgress.of(
            elapsed = dayElapsed,
            total = dayTotal,
            label = "DAY",
            remainingLabel = "$dayRemaining hours left"
        ),
        hour = ScaleProgress.of(
            elapsed = hourElapsed,
            total = hourTotal,
            label = "HOUR",
            remainingLabel = "$hourRemaining min left"
        ),
        isDaytime = isDaytime,
        currentTime = currentTimeStr,
        currentDate = currentDateStr,
        dayOfWeek = dayName
    )
}

// ── Composable: TimeProgressGroup ────────────────────────────

/**
 * The full time-progress visualization: 5 gradient bars stacked
 * with a clock display at the bottom. Each bar shows elapsed/remaining
 * with a current-position marker and glow.
 */
@Composable
fun TimeProgressGroup(
    state: TimeProgressState,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surface)
            .border(1.dp, surfaceColors.surfaceInteractive.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Time",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textPrimary
                )
                Text(
                    text = "Where you are in time",
                    fontSize = 9.sp,
                    color = surfaceColors.textMuted
                )
            }
            // Day/Night indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (state.isDaytime) Color(0xFFFFD93D) else Color(0xFF7BC8FF)
                        )
                )
                Text(
                    text = if (state.isDaytime) "Day" else "Night",
                    fontSize = 9.sp,
                    color = surfaceColors.textMuted
                )
            }
        }

        // All bars use consistent height for clean, uniform look
        val barHeight = 10.dp

        // Year bar
        TimeProgressBar(
            progress = state.year,
            accent = accent.primary,
            surfaceColors = surfaceColors,
            barHeight = barHeight,
            showMarker = true
        )

        // Month bar
        TimeProgressBar(
            progress = state.month,
            accent = accent.primary,
            surfaceColors = surfaceColors,
            barHeight = barHeight,
            showMarker = true
        )

        // Week bar
        TimeProgressBar(
            progress = state.week,
            accent = accent.primary,
            surfaceColors = surfaceColors,
            barHeight = barHeight,
            showMarker = true
        )

        // Day bar
        TimeProgressBar(
            progress = state.day,
            accent = accent.primary,
            surfaceColors = surfaceColors,
            barHeight = barHeight,
            showMarker = true
        )

        // Hour bar
        TimeProgressBar(
            progress = state.hour,
            accent = accent.primary,
            surfaceColors = surfaceColors,
            barHeight = barHeight,
            showMarker = true
        )

        // Clock display
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Current time with subtle glow
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.primary.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accent.primary)
                    )
                    Text(
                        text = state.currentTime,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = surfaceColors.textPrimary
                    )
                    Text(
                        text = state.dayOfWeek,
                        fontSize = 11.sp,
                        color = surfaceColors.textMuted
                    )
                }
            }
        }
    }
}

// ── Composable: Single progress bar ──────────────────────────

@Composable
private fun TimeProgressBar(
    progress: ScaleProgress,
    accent: Color,
    surfaceColors: SurfaceColors,
    barHeight: androidx.compose.ui.unit.Dp,
    showMarker: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(800, easing = LinearEasing),
        label = "timeBar_${progress.label}"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Label + remaining text
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = progress.label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = surfaceColors.textSecondary
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = progress.remainingLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = if (hovered) accent else surfaceColors.textMuted
            )
        }

        // Bar canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(barHeight / 2))
        ) {
            drawTimeBar(
                fraction = animatedFraction,
                accent = accent,
                surfaceColors = surfaceColors,
                showMarker = showMarker,
                hovered = hovered
            )
        }
    }
}

// ── Canvas drawing ───────────────────────────────────────────

private fun DrawScope.drawTimeBar(
    fraction: Float,
    accent: Color,
    surfaceColors: SurfaceColors,
    showMarker: Boolean,
    hovered: Boolean
) {
    val barWidth = size.width
    val barHeight = size.height
    val cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)

    // Background track with subtle gradient
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                surfaceColors.surfaceInteractive.copy(alpha = 0.2f),
                surfaceColors.surfaceInteractive.copy(alpha = 0.35f)
            )
        ),
        size = Size(barWidth, barHeight),
        cornerRadius = cornerRadius
    )

    // Elapsed fill with rich gradient
    if (fraction > 0f) {
        val fillWidth = barWidth * fraction
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.5f),
                    accent.copy(alpha = 0.8f),
                    accent.copy(alpha = if (hovered) 1f else 0.9f)
                ),
                startX = 0f,
                endX = fillWidth
            ),
            size = Size(fillWidth, barHeight),
            cornerRadius = cornerRadius
        )

        // Glow at current position
        if (showMarker && fraction > 0.01f) {
            val markerX = fillWidth
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(markerX, barHeight / 2),
                    radius = barHeight * 2f
                ),
                radius = barHeight * 2f,
                center = Offset(markerX, barHeight / 2)
            )

            // Marker line (bright)
            drawLine(
                color = accent,
                start = Offset(markerX, 0f),
                end = Offset(markerX, barHeight),
                strokeWidth = 2.dp.toPx()
            )
        }
    }

    // Remaining: visible dashed pattern
    if (fraction < 1f) {
        val remainingStart = barWidth * fraction
        val dotRadius = 1.2.dp.toPx()
        val dotSpacing = 5.dp.toPx()
        var x = remainingStart + dotSpacing / 2
        while (x < barWidth - dotRadius) {
            drawCircle(
                color = surfaceColors.textMuted.copy(alpha = 0.25f),
                radius = dotRadius,
                center = Offset(x, barHeight / 2)
            )
            x += dotSpacing
        }
    }
}

// ── Compact version for small spaces ─────────────────────────

@Composable
fun TimeProgressCompact(
    state: TimeProgressState,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Clock
        Text(
            text = state.currentTime,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textPrimary
        )

        // Mini bars
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            MiniProgressBar("Y", state.year.fraction, accent.primary, surfaceColors)
            MiniProgressBar("M", state.month.fraction, accent.primary, surfaceColors)
            MiniProgressBar("W", state.week.fraction, accent.primary, surfaceColors)
        }

        // Day/Night + remaining
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (state.isDaytime) "☀ Day" else "🌙 Night",
                fontSize = 9.sp,
                color = surfaceColors.textMuted
            )
            Text(
                text = state.day.remainingLabel,
                fontSize = 9.sp,
                color = surfaceColors.textMuted
            )
        }
    }
}

@Composable
private fun MiniProgressBar(
    label: String,
    fraction: Float,
    accent: Color,
    surfaceColors: SurfaceColors
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 7.sp,
            fontWeight = FontWeight.SemiBold,
            color = surfaceColors.textMuted,
            modifier = Modifier.width(10.dp)
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            // Track
            drawRoundRect(
                color = surfaceColors.surfaceInteractive.copy(alpha = 0.3f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            // Fill
            if (fraction > 0f) {
                drawRoundRect(
                    color = accent.copy(alpha = 0.7f),
                    size = Size(size.width * fraction, size.height),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}
