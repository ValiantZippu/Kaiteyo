package ua.syt0r.kanji.desktop.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsStatTile
import ua.syt0r.kanji.desktop.designsystem.DsTabRow
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.stats.AnalyticsEngine
import ua.syt0r.kanji.desktop.engine.stats.BreakdownEngine
import ua.syt0r.kanji.desktop.engine.stats.CardInsightEngine
import ua.syt0r.kanji.desktop.engine.stats.ForecastEngine
import ua.syt0r.kanji.desktop.engine.stats.GoalsEngine
import ua.syt0r.kanji.desktop.engine.stats.LearningCurveEngine
import ua.syt0r.kanji.desktop.engine.stats.MilestoneEngine
import ua.syt0r.kanji.desktop.engine.stats.ReviewDistributionEngine
import ua.syt0r.kanji.desktop.engine.stats.StatsPeriod
import ua.syt0r.kanji.desktop.engine.stats.StreakEngine
import ua.syt0r.kanji.desktop.engine.stats.WeakSpotEngine
import ua.syt0r.kanji.desktop.model.SrsStatus

// ============================================
// STATISTICS — ANALYTICS DASHBOARD
// Everything meaningful about study activity:
// KPIs per period, interactive heatmap, learning
// curve, rating distribution, forecast, streaks,
// goals, breakdowns, milestones, and history.
// ============================================

@Composable
fun StatsView(state: AppState) {
    val sc = surfaceColors()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var period by remember { mutableStateOf(StatsPeriod.Week) }

    val summaries = state.summaries.toList()
    val cards = state.cards.toList()
    val log = state.reviewLog.toList()

    val current = AnalyticsEngine.snapshot(summaries, period, today)
    val previous = AnalyticsEngine.previousSnapshot(summaries, period, today)
    val retention = AnalyticsEngine.retention(summaries, 7, today)
    val range = AnalyticsEngine.range(period, today)
    val rating = ReviewDistributionEngine.breakdown(log, range?.first, range?.second)
    val forecast = ForecastEngine.upcoming(cards, 30, today)
    val milestones = MilestoneEngine.compute(cards, summaries, log, today)
    val curve = LearningCurveEngine.build(summaries).takeLast(60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        DsTabRow(
            tabs = StatsPeriod.entries.map { it.label },
            selectedIndex = period.ordinal,
            onSelect = { period = StatsPeriod.entries[it] }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsStatTile(
                label = "Reviews",
                value = current.reviews.toString(),
                modifier = Modifier.weight(1f),
                delta = deltaText(current.reviews, previous?.reviews),
                deltaPositive = current.reviews >= (previous?.reviews ?: 0)
            )
            DsStatTile(
                label = "New cards",
                value = current.newCards.toString(),
                modifier = Modifier.weight(1f),
                delta = deltaText(current.newCards, previous?.newCards),
                deltaPositive = current.newCards >= (previous?.newCards ?: 0)
            )
            DsStatTile(
                label = "Forgotten",
                value = current.forgotten.toString(),
                modifier = Modifier.weight(1f),
                delta = deltaText(current.forgotten, previous?.forgotten),
                deltaPositive = current.forgotten <= (previous?.forgotten ?: 0)
            )
            DsStatTile(
                label = "Accuracy",
                value = "${(current.accuracy * 100).toInt()}%",
                modifier = Modifier.weight(1f),
                delta = deltaText(current.accuracy, previous?.accuracy),
                deltaPositive = current.accuracy >= (previous?.accuracy ?: 0f)
            )
            DsStatTile(
                label = "Retention (7d)",
                value = "${(retention * 100).toInt()}%",
                modifier = Modifier.weight(1f)
            )
            DsStatTile(
                label = "Study time",
                value = formatDuration(current.studyTime),
                modifier = Modifier.weight(1f),
                delta = deltaText(current.studyTime.inWholeMinutes.toInt(), previous?.studyTime?.inWholeMinutes?.toInt()),
                deltaPositive = current.studyTime >= (previous?.studyTime ?: kotlin.time.Duration.ZERO)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsStatTile("Study streak", "${StreakEngine.currentStudyStreak(summaries, today)}d", Modifier.weight(1f))
            DsStatTile("Learning streak", "${StreakEngine.currentLearningStreak(summaries, today)}d", Modifier.weight(1f))
            DsStatTile("Review streak", "${StreakEngine.currentReviewStreak(summaries, today)}d", Modifier.weight(1f))
            DsStatTile("Best streak", "${StreakEngine.bestStudyStreak(summaries)}d", Modifier.weight(1f))
            DsStatTile("Avg / review", avgPerReview(current), Modifier.weight(1f))
            DsStatTile("Learning speed", "%.1f".format(current.learningSpeed) + "/day", Modifier.weight(1f))
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(
                    title = "Activity Heatmap",
                    subtitle = "${period.label} activity · hover for a summary · click to explore a day",
                    action = {
                        Text(
                            text = "${summaries.size} active days · ${summaries.sumOf { it.newCount + it.reviewCount }} reviews",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                )
                Spacer(Modifier.height(DsSpacing.Lg))
                HeatmapPanel(state, summaries)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Learning Curve", subtitle = "Reviews per session, colored by accuracy")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    LearningCurveChart(curve)
                }
            }
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Answer Distribution", subtitle = "How you rate cards (${rating.total} answers)")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    ReviewDistributionChart(rating)
                    Spacer(Modifier.height(DsSpacing.Md))
                    ChartLegend(
                        listOf(
                            "Again" to Color(0xFFFF6B6B),
                            "Hard" to Color(0xFFFEAB57),
                            "Good" to Color(0xFF7BC8FF),
                            "Easy" to Color(0xFFC2FC8B)
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Daily Activity", subtitle = "Last 14 days, stacked by rating")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    DailyActivityChart(ReviewDistributionEngine.dailySeries(log, 14, today))
                    Spacer(Modifier.height(DsSpacing.Md))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("14 days ago", color = sc.textMuted, fontSize = DsType.Caption)
                        Text("today", color = sc.textMuted, fontSize = DsType.Caption)
                    }
                }
            }
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Goals", subtitle = "Daily · weekly · monthly · yearly")
                    Spacer(Modifier.height(DsSpacing.Md))
                    GoalsPanel(summaries, today)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(
                        title = "Upcoming Workload",
                        subtitle = "Cards due each day for the next two weeks",
                        action = {
                            Text(
                                text = "${ForecastEngine.dueToday(cards)} due now · ${ForecastEngine.dueThisWeek(cards)} this week",
                                color = sc.textMuted,
                                fontSize = DsType.Caption
                            )
                        }
                    )
                    Spacer(Modifier.height(DsSpacing.Lg))
                    ForecastChart(forecast)
                }
            }
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Milestones", subtitle = "Achievements derived from real study data")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    MilestonesPanel(milestones)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "By Deck", subtitle = "Cards, due and accuracy")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    BreakdownPanel(BreakdownEngine.byDeck(cards))
                }
            }
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "By Collection", subtitle = "Cards in your collections")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    BreakdownPanel(BreakdownEngine.byCollection(state.collections, cards))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "By Tag", subtitle = "Most populated tags")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    BreakdownPanel(BreakdownEngine.byTag(cards))
                }
            }
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "By Flag", subtitle = "Flagged cards by color")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    BreakdownPanel(BreakdownEngine.byFlag(cards))
                }
            }
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Status", subtitle = "Card lifecycle distribution")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    StatusPanel(cards)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Weakest Cards", subtitle = "Lowest accuracy, most lapses")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    CardRankingPanel(WeakSpotEngine.mostDifficult(cards, 6)) { card ->
                        "${(card.accuracy * 100).toInt()}% · ${card.lapses} lapses"
                    }
                }
            }
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Strongest Cards", subtitle = "Highest accuracy at volume")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    CardRankingPanel(CardInsightEngine.strongest(cards, 6)) { card ->
                        "${(card.accuracy * 100).toInt()}% · ${card.reps} reps"
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Recently Improved", subtitle = "Last review raised the interval")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    CardRankingPanel(CardInsightEngine.recentlyImproved(cards, log, 6), emptyMessage = "No improvements yet.") { card ->
                        "${card.intervalDays.toInt()}d"
                    }
                }
            }
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Recently Forgotten", subtitle = "Last review answered 'Again'")
                    Spacer(Modifier.height(DsSpacing.Lg))
                    CardRankingPanel(CardInsightEngine.recentlyForgotten(cards, log, 6), emptyMessage = "Nothing forgotten recently.") { card ->
                        "${card.intervalDays.toInt()}d"
                    }
                }
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(title = "Recent Activity", subtitle = "Everything Kaiteyo has recorded")
                Spacer(Modifier.height(DsSpacing.Lg))
                TimelinePanel(state.activityLog.entries)
            }
        }
    }
}

@Composable
private fun GoalsPanel(summaries: List<ua.syt0r.kanji.desktop.model.StudyDaySummary>, today: LocalDate) {
    val sc = surfaceColors()
    GoalsEngine.defaultGoals().forEach { goal ->
        val progress = GoalsEngine.progress(goal, summaries, today)
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
                    fontSize = DsType.Caption
                )
            }
            Spacer(Modifier.height(4.dp))
            DsProgressBar(fraction = progress.fraction, color = if (progress.complete) Color(0xFFC2FC8B) else Color.Unspecified)
        }
    }
}

@Composable
private fun StatusPanel(cards: List<ua.syt0r.kanji.desktop.model.DesktopCard>) {
    val sc = surfaceColors()
    if (cards.isEmpty()) {
        Text("No data yet.", color = sc.textMuted, fontSize = DsType.Body)
        return
    }
    SrsStatus.entries.forEach { status ->
        val count = cards.count { it.status == status }
        val fraction = count.toFloat() / cards.size
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(status.name, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(110.dp))
            DsProgressBar(fraction = fraction, modifier = Modifier.weight(1f), color = statusColor(status))
            Spacer(Modifier.width(DsSpacing.Sm))
            Text(count.toString(), color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
private fun statusColor(status: SrsStatus): Color = when (status) {
    SrsStatus.New -> Color(0xFFA78BFA)
    SrsStatus.Learning -> Color(0xFF7BC8FF)
    SrsStatus.Review -> Color(0xFFC2FC8B)
    SrsStatus.Relearning -> Color(0xFFFEAB57)
    SrsStatus.Suspended, SrsStatus.Buried -> Color(0xFF606060)
}

private fun avgPerReview(current: ua.syt0r.kanji.desktop.engine.stats.AnalyticsSnapshot): String {
    val avg = current.avgReviewDuration
    val seconds = avg.inWholeSeconds
    return when {
        seconds < 60 -> "${seconds}s"
        else -> "${seconds / 60}m ${seconds % 60}s"
    }
}

/** "+12%" style delta vs previous period; null when there is no baseline. */
private fun deltaText(current: Int, previous: Int?): String? {
    if (previous == null || previous == 0) return null
    val pct = ((current - previous).toFloat() / previous * 100).toInt()
    return if (pct >= 0) "+$pct%" else "$pct%"
}

private fun deltaText(current: Float, previous: Float?): String? {
    if (previous == null || previous <= 0f) return null
    val pct = ((current - previous) / previous * 100).toInt()
    return if (pct >= 0) "+$pct%" else "$pct%"
}
