package ua.syt0r.kanji.desktop.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.history.ActivityEntry
import ua.syt0r.kanji.presentation.common.theme.favoriteYellow
import ua.syt0r.kanji.presentation.common.theme.semanticError
import ua.syt0r.kanji.presentation.common.theme.semanticInfo
import ua.syt0r.kanji.presentation.common.theme.semanticNew
import ua.syt0r.kanji.presentation.common.theme.semanticSuccess
import ua.syt0r.kanji.presentation.common.theme.semanticWarning
import ua.syt0r.kanji.desktop.engine.history.ActivityFormatters
import ua.syt0r.kanji.desktop.engine.media.MediaDayStat
import ua.syt0r.kanji.desktop.engine.stats.BreakdownRow
import ua.syt0r.kanji.desktop.engine.stats.DailyRatingSeries
import ua.syt0r.kanji.desktop.engine.stats.ForecastDay
import ua.syt0r.kanji.desktop.engine.stats.LearningCurvePoint
import ua.syt0r.kanji.desktop.engine.stats.Milestone
import ua.syt0r.kanji.desktop.engine.stats.RatingBreakdown
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewRating

// ============================================
// STATS CHARTS & PANELS
// Reusable visualizations for the analytics
// dashboard. Every panel is driven by pure engine
// output so it stays honest and testable.
// ============================================

// Rating/status colors read the shared Kaiteyo semantic palette so charts
// always match the app's success / warning / info / danger language.
private val AgainColor = semanticError
private val HardColor = semanticWarning
private val GoodColor = semanticInfo
private val EasyColor = semanticSuccess

@Composable
fun ratingColor(rating: ReviewRating): Color = when (rating) {
    ReviewRating.Again -> AgainColor
    ReviewRating.Hard -> HardColor
    ReviewRating.Good -> GoodColor
    ReviewRating.Easy -> EasyColor
}

// ============================================
// DAILY ACTIVITY (stacked rating bars)
// ============================================

@Composable
fun DailyActivityChart(
    series: List<DailyRatingSeries>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val sc = surfaceColors()
    val maxTotal = series.maxOfOrNull { it.breakdown.total }?.takeIf { it > 0 } ?: 1
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        series.forEach { point ->
            val total = point.breakdown.total
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((height * (total.toFloat() / maxTotal)).coerceAtLeast(if (total > 0) 3.dp else 2.dp))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (total == 0) sc.surfaceInteractive else Color.Transparent)
                ) {
                    if (total > 0) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            StackedSegment(point.breakdown.easy.toFloat() / total, EasyColor)
                            StackedSegment(point.breakdown.good.toFloat() / total, GoodColor)
                            StackedSegment(point.breakdown.hard.toFloat() / total, HardColor)
                            StackedSegment(point.breakdown.again.toFloat() / total, AgainColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.StackedSegment(fraction: Float, color: Color) {
    if (fraction <= 0f) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(fraction)
            .background(color)
    )
}

// ============================================
// MEDIA ACTIVITY (immersion watch vs study time)
// ============================================

/**
 * Daily immersion bars for the global Statistics view. Each column shows
 * total watch minutes, stacked so the study-mode share (solid accent) sits
 * above the leisure share (dimmed accent). Fed by [MediaDayStat] — real
 * recorded media activity, never synthetic.
 */
@Composable
fun MediaActivityChart(
    days: List<Pair<LocalDate, MediaDayStat>>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val sc = surfaceColors()
    val ac = accent()
    val maxMs = maxOf(1L, days.maxOfOrNull { it.second.watchMs } ?: 0L)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        days.forEach { (date, stat) ->
            val watch = stat.watchMs
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(((height - 20.dp) * (watch.toFloat() / maxMs)).coerceAtLeast(if (watch > 0) 3.dp else 2.dp))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (watch == 0L) sc.surfaceInteractive else Color.Transparent)
                ) {
                    if (watch > 0L) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            StackedSegment(stat.studyMs.toFloat() / watch, ac.primary)
                            StackedSegment((watch - stat.studyMs).toFloat() / watch, ac.primary.copy(alpha = 0.35f))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = date.dayOfMonth.toString(),
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
    }
}

// ============================================
// MINING ACTIVITY (sentences mined per day)
// ============================================

/**
 * Daily mined-sentence bars for the global Statistics view. Fed by the
 * MiningStatisticsStore — every source (dictionary, media, OCR, browser,
 * API…) counts, and zero days render as an empty track rather than a lie.
 */
@Composable
fun MiningActivityChart(
    days: List<Pair<LocalDate, Int>>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val sc = surfaceColors()
    val ac = accent()
    val maxMined = maxOf(1, days.maxOfOrNull { it.second } ?: 0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        days.forEach { (date, mined) ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(((height - 20.dp) * (mined.toFloat() / maxMined)).coerceAtLeast(if (mined > 0) 3.dp else 2.dp))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (mined == 0) sc.surfaceInteractive else ac.primary.copy(alpha = 0.8f))
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = date.dayOfMonth.toString(),
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
    }
}

// ============================================
// REVIEW DISTRIBUTION (horizontal bars)
// ============================================

@Composable
fun ReviewDistributionChart(
    breakdown: RatingBreakdown,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val max = maxOf(1, breakdown.total)
    val ratings = listOf(ReviewRating.Again, ReviewRating.Hard, ReviewRating.Good, ReviewRating.Easy)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        ratings.forEach { rating ->
            val count = breakdown.count(rating)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rating.displayName, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(56.dp))
                DsProgressBar(
                    fraction = count.toFloat() / max,
                    modifier = Modifier.weight(1f),
                    color = ratingColor(rating)
                )
                Spacer(Modifier.width(DsSpacing.Sm))
                Text(count.toString(), color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(32.dp))
            }
        }
    }
}

// ============================================
// LEARNING CURVE
// ============================================

@Composable
fun LearningCurveChart(
    points: List<LearningCurvePoint>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val sc = surfaceColors()
    val ac = accent()
    val maxReviews = (points.maxOfOrNull { it.reviews } ?: 1).coerceAtLeast(1)

    if (points.isEmpty()) {
        Text("No data yet — complete a few sessions.", color = sc.textMuted, fontSize = DsType.Body)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            points.forEach { point ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height((height * (point.reviews.toFloat() / maxReviews)).coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(if (point.accuracy > 0.7f) ac.primary.copy(alpha = 0.8f) else AgainColor.copy(alpha = 0.7f))
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("oldest", color = sc.textMuted, fontSize = DsType.Caption)
            Text("today", color = sc.textMuted, fontSize = DsType.Caption)
        }
    }
}

// ============================================
// FORECAST
// ============================================

@Composable
fun ForecastChart(
    days: List<ForecastDay>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val sc = surfaceColors()
    val ac = accent()
    val max = maxOf(1, days.maxOfOrNull { it.due } ?: 0)
    val visible = days.take(14)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        visible.forEach { day ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (day.due > 0) day.due.toString() else "",
                    color = if (day.due > 0) sc.textSecondary else sc.textMuted,
                    fontSize = DsType.Caption
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(((height - 20.dp) * (day.due.toFloat() / max)).coerceAtLeast(if (day.due > 0) 3.dp else 2.dp))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (day.due > 0) ac.primary.copy(alpha = 0.8f) else sc.surfaceInteractive)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = day.date.dayOfWeek.name.lowercase().take(2),
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
    }
}

// ============================================
// BREAKDOWN PANEL
// ============================================

@Composable
fun BreakdownPanel(
    rows: List<BreakdownRow>,
    modifier: Modifier = Modifier,
    maxRows: Int = 8
) {
    val sc = surfaceColors()
    val ac = accent()
    val visible = rows.take(maxRows)
    val maxCount = maxOf(1, visible.maxOfOrNull { it.count } ?: 0)

    if (visible.isEmpty()) {
        Text("No data yet.", color = sc.textMuted, fontSize = DsType.Body)
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        visible.forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.key, color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    if (row.due > 0) {
                        DsBadge(text = "${row.due} due", tint = AgainColor)
                        Spacer(Modifier.width(DsSpacing.Xs))
                    }
                    Text("${(row.accuracy * 100).toInt()}%", color = sc.textMuted, fontSize = DsType.Caption)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DsProgressBar(
                        fraction = row.count.toFloat() / maxCount,
                        modifier = Modifier.weight(1f),
                        color = ac.primary.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(DsSpacing.Sm))
                    Text(row.count.toString(), color = sc.textPrimary, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

// ============================================
// MILESTONES
// ============================================

@Composable
fun MilestonesPanel(
    milestones: List<Milestone>,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        milestones.forEach { m ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(DsRadius.Full))
                        .background(if (m.achieved) EasyColor else sc.surfaceInteractive)
                )
                Spacer(Modifier.width(DsSpacing.Sm))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            m.title,
                            color = if (m.achieved) sc.textPrimary else sc.textSecondary,
                            fontSize = DsType.Body,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            m.metric,
                            color = if (m.achieved) EasyColor else sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    DsProgressBar(fraction = m.progress, height = 4.dp, color = if (m.achieved) EasyColor else ac.primary.copy(alpha = 0.5f))
                    Text(m.description, color = sc.textMuted, fontSize = DsType.Caption)
                }
            }
        }
    }
}

// ============================================
// ACTIVITY TIMELINE
// ============================================

private fun categoryColor(category: ActivityCategory): Color = when (category) {
    ActivityCategory.Review -> GoodColor
    ActivityCategory.Study -> EasyColor
    ActivityCategory.Import, ActivityCategory.Export -> GoodColor.copy(alpha = 0.8f)
    ActivityCategory.Undo -> HardColor
    ActivityCategory.Tag, ActivityCategory.Flag, ActivityCategory.Plugin -> semanticNew
    ActivityCategory.Favorite -> favoriteYellow
    ActivityCategory.Sync -> GoodColor
    ActivityCategory.Note, ActivityCategory.Deck, ActivityCategory.Settings, ActivityCategory.Theme, ActivityCategory.System -> GoodColor.copy(alpha = 0.6f)
}

@Composable
fun TimelinePanel(
    entries: List<ActivityEntry>,
    modifier: Modifier = Modifier,
    maxEntries: Int = 12
) {
    val sc = surfaceColors()
    if (entries.isEmpty()) {
        Text("No activity yet.", color = sc.textMuted, fontSize = DsType.Body)
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        entries.take(maxEntries).forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ActivityFormatters.relative(entry.timestamp),
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    modifier = Modifier.width(64.dp)
                )
                DsBadge(text = entry.category.name, tint = categoryColor(entry.category))
                Spacer(Modifier.width(DsSpacing.Sm))
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

// ============================================
// CARD RANKING PANEL
// ============================================

@Composable
fun CardRankingPanel(
    cards: List<DesktopCard>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No data yet.",
    metric: (DesktopCard) -> String
) {
    val sc = surfaceColors()
    if (cards.isEmpty()) {
        Text(emptyMessage, color = sc.textMuted, fontSize = DsType.Body)
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
        cards.forEach { card ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(sc.surface)
                    .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(card.character, color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.meaning, color = sc.textSecondary, fontSize = DsType.Body, maxLines = 1)
                    Text(card.readings.take(2).joinToString(" · "), color = sc.textMuted, fontSize = DsType.Caption, maxLines = 1)
                }
                Text(metric(card), color = sc.textMuted, fontSize = DsType.Caption)
            }
        }
    }
}

// ============================================
// LEGEND
// ============================================

@Composable
fun ChartLegend(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        items.forEach { (label, color) ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
                Text(label, color = surfaceColors().textMuted, fontSize = DsType.Caption)
            }
        }
    }
}
