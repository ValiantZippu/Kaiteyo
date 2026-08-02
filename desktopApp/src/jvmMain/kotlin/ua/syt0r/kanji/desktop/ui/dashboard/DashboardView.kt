package ua.syt0r.kanji.desktop.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsStatTile
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.stats.GoalsEngine
import ua.syt0r.kanji.desktop.engine.stats.HeatmapEngine
import ua.syt0r.kanji.desktop.engine.stats.LearningCurveEngine
import ua.syt0r.kanji.desktop.engine.stats.HeatmapCell
import ua.syt0r.kanji.desktop.engine.stats.WeakSpotEngine
import ua.syt0r.kanji.desktop.model.SrsStatus

// ============================================
// DASHBOARD
// At-a-glance overview: counts, heatmap, learning
// curve, goals, weak spots, and one-click review.
// ============================================

@Composable
fun DashboardView(state: AppState) {
    val sc = surfaceColors()
    val cards = state.cards

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(DsSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        // Hero: start review
        DsCard(elevated = true) {
            Row(
                modifier = Modifier.padding(DsSpacing.Xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Ready to study?",
                        color = sc.textPrimary,
                        fontSize = DsType.Heading,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(DsSpacing.Xs))
                    Text(
                        text = "${state.dueCount()} cards due now · ${state.newCount()} new cards available",
                        color = sc.textMuted,
                        fontSize = DsType.Body
                    )
                }
                DsButton(
                    text = "Start Review",
                    icon = Icons.Default.PlayArrow,
                    onClick = { state.startReview() }
                )
            }
        }

        // Stat tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsStatTile(
                label = "Due now",
                value = state.dueCount().toString(),
                modifier = Modifier.weight(1f),
                delta = "+${state.dueCount()} today",
                deltaPositive = state.dueCount() >= 0
            )
            DsStatTile(
                label = "New",
                value = state.newCount().toString(),
                modifier = Modifier.weight(1f)
            )
            DsStatTile(
                label = "Mastered",
                value = state.masteredCount().toString(),
                modifier = Modifier.weight(1f),
                delta = "21d+ intervals"
            )
            DsStatTile(
                label = "Suspended",
                value = state.suspendedCount().toString(),
                modifier = Modifier.weight(1f)
            )
            DsStatTile(
                label = "Total cards",
                value = cards.size.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        // Heatmap + curve row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            DsCard(modifier = Modifier.weight(1.35f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(
                        title = "Activity Heatmap",
                        subtitle = "${HeatmapEngine.currentStreak(state.summaries)} day streak",
                        action = {
                            DsBadge(text = "last 52 weeks")
                        }
                    )
                    Spacer(Modifier.height(DsSpacing.Lg))
                    HeatmapChart(state.summaries)
                }
            }
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(
                        title = "Review Pace",
                        subtitle = "Daily reviews, last 30 days"
                    )
                    Spacer(Modifier.height(DsSpacing.Lg))
                    ReviewPaceChart(state.summaries)
                }
            }
        }

        // Goals + weak spots row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(
                        title = "Goals",
                        subtitle = "Progress toward your targets",
                        action = {
                            androidx.compose.material3.TextButton(onClick = { state.currentView = WorkspaceView.Statistics }) {
                                androidx.compose.material3.Text("All stats", color = accent().primary)
                            }
                        }
                    )
                    Spacer(Modifier.height(DsSpacing.Md))
                    GoalsEngine.defaultGoals().take(4).forEach { goal ->
                        val progress = GoalsEngine.progress(goal, state.summaries)
                        Column(Modifier.padding(vertical = DsSpacing.Sm)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = goal.name,
                                    color = sc.textPrimary,
                                    fontSize = DsType.Body,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${progress.achieved} / ${progress.target}",
                                    color = if (progress.complete) Color(0xFFC2FC8B) else sc.textMuted,
                                    fontSize = DsType.Caption,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            DsProgressBar(
                                fraction = progress.fraction,
                                color = if (progress.complete) Color(0xFFC2FC8B) else Color.Unspecified
                            )
                        }
                    }
                }
            }
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(
                        title = "Weak Spots",
                        subtitle = "Cards that need attention"
                    )
                    Spacer(Modifier.height(DsSpacing.Md))
                    val difficult = WeakSpotEngine.mostDifficult(cards.toList(), limit = 4)
                    difficult.forEach { card ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(DsRadius.Md))
                                .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                                .clickable { state.selectedCard = card; state.currentView = WorkspaceView.Browser }
                                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = card.character,
                                color = sc.textPrimary,
                                fontSize = DsType.Title,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(48.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = card.meaning,
                                    color = sc.textSecondary,
                                    fontSize = DsType.Body,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${card.lapses} lapses · ${(card.accuracy * 100).toInt()}% acc",
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption
                                )
                            }
                            DsBadge(text = card.status.name, tint = accent().primary)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    if (difficult.isEmpty()) {
                        Text(
                            text = "Nothing to fix yet — keep reviewing!",
                            color = sc.textMuted,
                            fontSize = DsType.Body,
                            modifier = Modifier.padding(DsSpacing.Sm)
                        )
                    }
                }
            }
        }

        // Recent activity
        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(
                    title = "Recent Activity",
                    action = {
                        androidx.compose.material3.TextButton(onClick = { state.currentView = WorkspaceView.History }) {
                            androidx.compose.material3.Text("View all", color = accent().primary)
                        }
                    }
                )
                Spacer(Modifier.height(DsSpacing.Sm))
                state.activityLog.entries.take(6).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = DsSpacing.Sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(categoryColor(entry.category))
                        )
                        Spacer(Modifier.width(DsSpacing.Sm))
                        Text(
                            text = entry.summary,
                            color = sc.textSecondary,
                            fontSize = DsType.Body,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            text = ua.syt0r.kanji.desktop.engine.history.ActivityFormatters.relative(entry.timestamp),
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun categoryColor(category: ua.syt0r.kanji.desktop.engine.history.ActivityCategory): Color =
    when (category) {
        ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Review -> Color(0xFFC2FC8B)
        ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Study -> Color(0xFF7BC8FF)
        ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Undo -> Color(0xFFFEAB57)
        ua.syt0r.kanji.desktop.engine.history.ActivityCategory.System -> Color(0xFFA78BFA)
        else -> Color(0xFF606060)
    }

// ============================================
// HEATMAP CHART
// ============================================

@Composable
private fun HeatmapChart(summaries: List<ua.syt0r.kanji.desktop.model.StudyDaySummary>) {
    val sc = surfaceColors()
    val ac = accent()
    val months = HeatmapEngine.build(summaries)

    Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        months.forEach { month ->
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = month.label,
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
                month.weeks.forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        week.forEach { cell ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(heatColor(cell, sc, ac))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun heatColor(cell: HeatmapCell?, sc: ua.syt0r.kanji.presentation.common.theme.SurfaceColors, ac: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme): Color {
    if (cell == null) return sc.surfaceInteractive.copy(alpha = 0.35f)
    return when (cell.level) {
        0 -> sc.surfaceInteractive
        1 -> ac.primary.copy(alpha = 0.25f)
        2 -> ac.primary.copy(alpha = 0.45f)
        3 -> ac.primary.copy(alpha = 0.7f)
        else -> ac.primary
    }
}

// ============================================
// REVIEW PACE CHART (last 30 days bars)
// ============================================

@Composable
private fun ReviewPaceChart(summaries: List<ua.syt0r.kanji.desktop.model.StudyDaySummary>) {
    val sc = surfaceColors()
    val ac = accent()
    val points = LearningCurveEngine.build(summaries).takeLast(30)
    val max = (points.maxOfOrNull { it.reviews } ?: 1).coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (points.isEmpty()) {
            Text(
                text = "No data yet",
                color = sc.textMuted,
                fontSize = DsType.Body
            )
            return
        }
        points.forEach { point ->
            val fraction = point.reviews.toFloat() / max
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((fraction * 100).dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (point.accuracy > 0.7f) ac.primary.copy(alpha = 0.85f) else Color(0xFFFF6B6B).copy(alpha = 0.8f))
                )
            }
        }
    }
    Spacer(Modifier.height(DsSpacing.Sm))
    Text(
        text = "Green = ≥70% accuracy · Red = below",
        color = sc.textMuted,
        fontSize = DsType.Caption
    )
}
