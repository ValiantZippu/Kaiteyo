package ua.syt0r.kanji.desktop.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge

import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsStatTile
import ua.syt0r.kanji.desktop.designsystem.DsTabRow
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.dueColor
import ua.syt0r.kanji.desktop.designsystem.errorColor
import ua.syt0r.kanji.desktop.designsystem.infoColor
import ua.syt0r.kanji.desktop.designsystem.newColor
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString
import ua.syt0r.kanji.desktop.engine.stats.AnalyticsEngine
import ua.syt0r.kanji.desktop.engine.stats.BreakdownEngine
import ua.syt0r.kanji.desktop.engine.stats.CardInsightEngine
import ua.syt0r.kanji.desktop.engine.stats.ForecastEngine
import ua.syt0r.kanji.desktop.engine.stats.GoalsEngine
import ua.syt0r.kanji.desktop.engine.stats.KnowledgeProfileEngine
import ua.syt0r.kanji.desktop.engine.stats.LearningCurveEngine
import ua.syt0r.kanji.desktop.engine.stats.MilestoneEngine
import ua.syt0r.kanji.desktop.engine.stats.ReviewDistributionEngine
import ua.syt0r.kanji.desktop.engine.stats.StatsPeriod
import ua.syt0r.kanji.desktop.engine.stats.StreakEngine
import ua.syt0r.kanji.desktop.engine.stats.WeakSpotEngine
import ua.syt0r.kanji.desktop.engine.learning.LearningItemKind
import ua.syt0r.kanji.desktop.engine.learning.LearningStage
import ua.syt0r.kanji.desktop.engine.learning.StatsPeriod as LearningStatsPeriod
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

    // Media immersion stats — same period as everything above, straight from
    // MediaStatisticsStore (real watch time, lookups and mined sentences).
    val mediaStats = state.media.statistics
    val mediaWatchMs = if (range != null) mediaStats.watchMsBetween(range.first, range.second) else mediaStats.totalWatchMs
    val mediaStudyMs = if (range != null) mediaStats.studyMsBetween(range.first, range.second) else mediaStats.totalStudyMs
    val mediaLookups = if (range != null) mediaStats.lookupsBetween(range.first, range.second) else mediaStats.totalLookups
    val mediaSessions = if (range != null) mediaStats.daysBetween(range.first, range.second).sumOf { it.sessions } else mediaStats.totalSessions
    val mediaActiveDays = if (range != null) mediaStats.daysBetween(range.first, range.second).size else mediaStats.days.size
    val prevMediaRange = AnalyticsEngine.previousRange(period, today)
    val prevWatchMs = prevMediaRange?.let { mediaStats.watchMsBetween(it.first, it.second) }
    val prevStudyMs = prevMediaRange?.let { mediaStats.studyMsBetween(it.first, it.second) }
    val prevLookups = prevMediaRange?.let { mediaStats.lookupsBetween(it.first, it.second) }
    // Last 14 local days for the chart (oldest → today). Computed inline (not
    // remembered) so live bumps to today's bucket — e.g. the mini-player
    // playing while this view is open — stay fresh.
    val mediaDaily = (0L until 14L).map { offset ->
        val d = today.minus(offset, DateTimeUnit.DAY)
        d to mediaStats.day(d)
    }.reversed()

    // Mining stats — EVERY source (dictionary, media, OCR, browser, API…), recorded
    // per local day by MiningStatisticsStore (single source of truth for mining volume).
    val miningStats = state.miningStatistics
    val miningPeriod = if (range != null) miningStats.minedBetween(range.first, range.second) else miningStats.totalMined
    val prevMining = prevMediaRange?.let { miningStats.minedBetween(it.first, it.second) }
    val miningDaily = (0L until 7L).map { offset ->
        val d = today.minus(offset, DateTimeUnit.DAY)
        d to miningStats.minedOn(d)
    }.reversed()
    val miningSources = miningStats.minedBySourceTop(6)
    val recentMines = state.mining.minedRecords.take(8)
    // Fixed 7-day window for the rate — independent of the selected period tab.
    val miningWeek = miningStats.minedBetween(today.minus(6, DateTimeUnit.DAY), today)

    // Learning velocity + JLPT coverage — computed from the real card pool.
    val weekStart = today.minus(6, DateTimeUnit.DAY)
    val monthStart = LocalDate(today.year, today.month, 1)
    val newCardsWeek = cards.count { it.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date >= weekStart }
    val newCardsMonth = cards.count { it.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date >= monthStart }
    val reviewsLast7 = summaries.filter { it.day >= weekStart.toString() }.sumOf { it.newCount + it.reviewCount }
    val studyLast7 = summaries.filter { it.day >= weekStart.toString() }
        .fold(kotlin.time.Duration.ZERO) { acc, s -> acc + s.timeSpent }
    val jlptCards = cards.filter { it.jlpt != null }

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
                label = resolveSuiteString { reviewsLabel },
                value = current.reviews.toString(),
                modifier = Modifier.weight(1f),
                delta = deltaText(current.reviews, previous?.reviews),
                deltaPositive = current.reviews >= (previous?.reviews ?: 0)
            )
            DsStatTile(
                label = resolveSuiteString { newCardsLabel },
                value = current.newCards.toString(),
                modifier = Modifier.weight(1f),
                delta = deltaText(current.newCards, previous?.newCards),
                deltaPositive = current.newCards >= (previous?.newCards ?: 0)
            )
            DsStatTile(
                label = resolveSuiteString { forgottenLabel },
                value = current.forgotten.toString(),
                modifier = Modifier.weight(1f),
                delta = deltaText(current.forgotten, previous?.forgotten),
                deltaPositive = current.forgotten <= (previous?.forgotten ?: 0)
            )
            DsStatTile(
                label = resolveSuiteString { accuracyLabel },
                value = "${(current.accuracy * 100).toInt()}%",
                modifier = Modifier.weight(1f),
                delta = deltaText(current.accuracy, previous?.accuracy),
                deltaPositive = current.accuracy >= (previous?.accuracy ?: 0f)
            )
            DsStatTile(
                label = resolveSuiteString { retention7dLabel },
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
            DsStatTile(resolveSuiteString { studyStreakLabel }, "${StreakEngine.currentStudyStreak(summaries, today)}d", Modifier.weight(1f))
            DsStatTile(resolveSuiteString { learningStreakLabel }, "${StreakEngine.currentLearningStreak(summaries, today)}d", Modifier.weight(1f))
            DsStatTile(resolveSuiteString { reviewStreakLabel }, "${StreakEngine.currentReviewStreak(summaries, today)}d", Modifier.weight(1f))
            DsStatTile(resolveSuiteString { bestStreakLabel }, "${StreakEngine.bestStudyStreak(summaries)}d", Modifier.weight(1f))
            DsStatTile("Avg / review", avgPerReview(current), Modifier.weight(1f))
            DsStatTile(resolveSuiteString { learningSpeedLabel }, "%.1f".format(current.learningSpeed) + "/day", Modifier.weight(1f))
        }

        // UNIFIED LEARNING OVERVIEW — every number here is derived from the
        // immutable review-event stream, writing attempts and exam results in
        // the LearningStore, never from current card state alone.
        val learning = state.learning
        val learnToday = learning.periodStats(LearningStatsPeriod.Today)
        val learnAll = learning.periodStats(LearningStatsPeriod.All)
        val streaks = learning.streaks()
        val goals = learning.goalProgress()
        val mistakes = learning.mistakeSnapshot()
        val weakKanji = learning.weakestKanji(5)
        val coverage = learning.jlptCoverage()
        val charProgress = learning.characterProgress()
        val learningForecast = learning.forecast(14)
        // Kana is its own learning domain — counts straight from the store.
        val kanaNotes = learning.notes.filter { it.kind == LearningItemKind.Kana }
        val kanaEstablished = learning.cards.count { card ->
            kanaNotes.any { it.id == card.noteId } &&
                (card.stage == LearningStage.Established || card.stage == LearningStage.Mature)
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(
                    title = resolveSuiteString { learningOverviewTitle },
                    subtitle = resolveSuiteString { learningOverviewSubtitle }
                )
                Spacer(Modifier.height(DsSpacing.Lg))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    DsStatTile(resolveSuiteString { dueNowLabel }, learning.dueToday().toString(), Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { streakLabel }, "${streaks.current}d", Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { longestLabel }, "${streaks.longest}d", Modifier.weight(1f))
                    DsStatTile("Study time (all)", formatDuration(learnAll.studyTimeMs.milliseconds), Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { writingAttemptsLabel }, learnAll.writingAttempts.toString(), Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { writingAccuracyLabel }, "${(learnAll.writingAccuracy * 100).toInt()}%", Modifier.weight(1f))
                }
                Spacer(Modifier.height(DsSpacing.Md))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    DsStatTile(resolveSuiteString { reviewsTodayLabel }, learnToday.reviews.toString(), Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { accuracyTodayLabel }, "${(learnToday.accuracy * 100).toInt()}%", Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { kanjiStudiedLabel }, charProgress.uniqueKanjiStudied.toString(), Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { kanjiEstablishedLabel }, charProgress.uniqueKanjiEstablished.toString(), Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { vocabularyLabel }, charProgress.uniqueVocabulary.toString(), Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { vocabEstablishedLabel }, charProgress.uniqueVocabularyEstablished.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(DsSpacing.Md))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    DsStatTile(resolveSuiteString { kanaStudiedLabel }, kanaNotes.size.toString(), Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { kanaEstablishedLabel }, kanaEstablished.toString(), Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { kanaWritingAccuracyLabel }, kanaWritingAccuracyLabel(learning), Modifier.weight(1f))
                }

                Spacer(Modifier.height(DsSpacing.Lg))
                Text(resolveSuiteString { dailyGoalsLabel }, color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(DsSpacing.Sm))
                goals.take(3).forEach { gp ->
                    Column(Modifier.padding(vertical = DsSpacing.Sm)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(gp.goal.name, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                            Text("${gp.achieved} / ${gp.goal.target}", color = if (gp.complete) successColor() else sc.textMuted, fontSize = DsType.Caption)
                        }
                        Spacer(Modifier.height(4.dp))
                        DsProgressBar(fraction = gp.fraction, color = if (gp.complete) successColor() else Color.Unspecified)
                    }
                }

                Spacer(Modifier.height(DsSpacing.Lg))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xl)) {
                    MediaStatPill(resolveSuiteString { againReviewsLabel }, mistakes.againEvents.toString())
                    MediaStatPill(resolveSuiteString { writingMistakesLabel }, mistakes.writingMistakes.toString())
                    MediaStatPill(resolveSuiteString { examMistakesLabel }, mistakes.examMistakes.toString())
                    MediaStatPill(resolveSuiteString { lapsedCardsLabel }, mistakes.lapsedCards.toString())
                }

                if (weakKanji.isNotEmpty()) {
                    Spacer(Modifier.height(DsSpacing.Lg))
                    Text(resolveSuiteString { weakestWritingLabel }, color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(DsSpacing.Sm))
                    weakKanji.forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                            Text(row.expression, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium, modifier = Modifier.width(60.dp))
                            DsProgressBar(fraction = row.accuracy, modifier = Modifier.weight(1f), color = if (row.accuracy >= 0.7f) successColor() else if (row.accuracy >= 0.5f) warningColor() else errorColor())
                            Spacer(Modifier.width(DsSpacing.Sm))
                            Text("${(row.accuracy * 100).toInt()}%", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(44.dp))
                        }
                    }
                }

                Spacer(Modifier.height(DsSpacing.Lg))
                Text("JLPT coverage (stage-based)", color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(DsSpacing.Sm))
                if (coverage.isEmpty()) {
                    Text("No JLPT-classified notes yet.", color = sc.textMuted, fontSize = DsType.Body)
                } else {
                    coverage.forEach { c ->
                        Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                            Text("N${c.level}", color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
                            DsProgressBar(fraction = c.introducedFraction, modifier = Modifier.weight(1f), color = jlptColor(c.level))
                            Spacer(Modifier.width(DsSpacing.Sm))
                            Text("${(c.introducedFraction * 100).toInt()}% introduced · ${(c.establishedFraction * 100).toInt()}% established", color = sc.textMuted, fontSize = DsType.Caption)
                        }
                    }
                }

                Spacer(Modifier.height(DsSpacing.Lg))
                Text("Due forecast (14 days)", color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(DsSpacing.Sm))
                val maxDue = (learningForecast.maxOfOrNull { it.due } ?: 0).coerceAtLeast(1)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                    learningForecast.forEach { point ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .fillMaxWidth(if (maxDue == 0) 0.04f else point.due.toFloat() / maxDue)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (point.due > 0) accent().primary else sc.surfaceInteractive)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(point.date.dayOfMonth.toString(), color = sc.textMuted, fontSize = DsType.Caption)
                        }
                    }
                }
            }
        }

        // KNOWLEDGE PROFILE — a study-based estimate of what the learner can
        // actually handle: kana / kanji / vocabulary coverage, writing
        // accuracy, theoretical JLPT coverage and frequency coverage. Honest
        // language throughout — this estimates readiness, it never certifies.
        val knowledge = remember(learning.revision) { KnowledgeProfileEngine.profile(state) }
        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(
                    title = resolveSuiteString { knowledgeProfileTitle },
                    subtitle = resolveSuiteString { knowledgeProfileSubtitle },
                    action = {
                        DsBadge(
                            text = "${resolveSuiteString { confidenceLabel }}: ${knowledge.confidence}",
                            tint = when (knowledge.confidence) {
                                "High" -> successColor()
                                "Medium" -> warningColor()
                                else -> infoColor()
                            }
                        )
                    }
                )
                Spacer(Modifier.height(DsSpacing.Lg))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = resolveSuiteString { overallCoverageLabel },
                        color = sc.textSecondary,
                        fontSize = DsType.Body,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(knowledge.overallFraction * 100).toInt()}%",
                        color = accent().primary,
                        fontSize = DsType.Title,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                DsProgressBar(fraction = knowledge.overallFraction, color = accent().primary)
                Spacer(Modifier.height(DsSpacing.Lg))

                if (knowledge.dimensions.isEmpty()) {
                    Text(
                        "No learning data yet — study kana, kanji and vocabulary and your profile fills in here.",
                        color = sc.textMuted,
                        fontSize = DsType.Body
                    )
                } else {
                    knowledge.dimensions.forEach { dim ->
                        Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                dim.label,
                                color = sc.textSecondary,
                                fontSize = DsType.Body,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(180.dp)
                            )
                            DsProgressBar(
                                fraction = dim.fraction,
                                modifier = Modifier.weight(1f),
                                color = when {
                                    dim.fraction >= 0.7f -> successColor()
                                    dim.fraction >= 0.3f -> warningColor()
                                    else -> dueColor()
                                }
                            )
                            Spacer(Modifier.width(DsSpacing.Sm))
                            Text(
                                text = if (dim.accuracy != null) {
                                    "${dim.known}/${dim.total} · ${(dim.accuracy * 100).toInt()}% acc"
                                } else {
                                    "${dim.known}/${dim.total}"
                                },
                                color = sc.textMuted,
                                fontSize = DsType.Caption,
                                modifier = Modifier.width(150.dp)
                            )
                        }
                    }
                }

                if (knowledge.writingAttempts > 0) {
                    Spacer(Modifier.height(DsSpacing.Sm))
                    Text(
                        "Writing: ${(knowledge.writingAccuracy!! * 100).toInt()}% accuracy over ${knowledge.writingAttempts} real stroke evaluations",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }

                Spacer(Modifier.height(DsSpacing.Lg))
                Text(resolveSuiteString { theoreticalJlptLabel }, color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                Text(
                    "Cumulative study-based coverage against approximate reference band sizes — a readiness estimate, not an official score.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
                Spacer(Modifier.height(DsSpacing.Sm))
                knowledge.jlpt.forEach { est ->
                    Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                        Text("N${est.level}", color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
                        DsProgressBar(
                            fraction = est.kanjiKnown.toFloat() / est.kanjiTotal.coerceAtLeast(1),
                            modifier = Modifier.weight(1f),
                            color = jlptColor(est.level)
                        )
                        Spacer(Modifier.width(DsSpacing.Sm))
                        Text(
                            "${est.kanjiKnown}/${est.kanjiTotal} kanji · ${est.vocabKnown}/${est.vocabTotal} vocab",
                            color = sc.textMuted,
                            fontSize = DsType.Caption,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }

                if (knowledge.frequency.isNotEmpty()) {
                    Spacer(Modifier.height(DsSpacing.Lg))
                    Text(resolveSuiteString { vocabFrequencyCoverageLabel }, color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Words you have established within each frequency band (from corpus-linked vocabulary in your pool).",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                    Spacer(Modifier.height(DsSpacing.Sm))
                    knowledge.frequency.forEach { freq ->
                        Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                            Text("Top ${freq.band}", color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(90.dp))
                            DsProgressBar(
                                fraction = freq.known.toFloat() / freq.total.coerceAtLeast(1),
                                modifier = Modifier.weight(1f),
                                color = accent().primary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(DsSpacing.Sm))
                            Text("${freq.known}/${freq.total}", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(72.dp))
                        }
                    }
                }
            }
        }

        // WRITING STATISTICS — per-attempt history with stroke detail and a
        // per-character accuracy trend, all from real WritingAttemptEvents.
        val recentWriting = learning.recentWritingAttempts(14)
        val writingCharacters = recentWriting.map { it.expected }.distinct().take(5)
        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(
                    title = resolveSuiteString { writingHistoryTitle },
                    subtitle = resolveSuiteString { writingHistorySubtitle },
                    action = {
                        Text(
                            text = "${learnAll.writingAttempts} attempts total",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                )
                Spacer(Modifier.height(DsSpacing.Lg))
                if (recentWriting.isEmpty()) {
                    Text(
                        "No writing attempts recorded yet — practice writing a kanji and your stroke evaluations land here.",
                        color = sc.textMuted,
                        fontSize = DsType.Body
                    )
                } else {
                    recentWriting.take(8).forEach { attempt ->
                        val char = attempt.expected
                        Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = char,
                                color = sc.textPrimary,
                                fontSize = DsType.BodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(44.dp)
                            )
                            Text(
                                text = attempt.attemptedAt.toString().substring(0, 16).replace('T', ' '),
                                color = sc.textMuted,
                                fontSize = DsType.Caption,
                                modifier = Modifier.width(130.dp)
                            )
                            if (attempt.strokes.isNotEmpty()) {
                                Text(
                                    text = attempt.strokes.count { it.correct }.let { "$it/${attempt.strokes.size} strokes" },
                                    color = sc.textSecondary,
                                    fontSize = DsType.Caption,
                                    modifier = Modifier.width(96.dp)
                                )
                            } else {
                                Text(
                                    text = if (attempt.completed) "self-graded" else "incomplete",
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption,
                                    modifier = Modifier.width(96.dp)
                                )
                            }
                            DsProgressBar(
                                fraction = attempt.accuracy,
                                modifier = Modifier.weight(1f),
                                color = if (attempt.accuracy >= 0.7f) successColor() else if (attempt.accuracy >= 0.5f) warningColor() else errorColor()
                            )
                            Spacer(Modifier.width(DsSpacing.Sm))
                            Text(
                                "${(attempt.accuracy * 100).toInt()}%",
                                color = sc.textMuted,
                                fontSize = DsType.Caption,
                                modifier = Modifier.width(44.dp)
                            )
                        }
                    }

                    if (writingCharacters.isNotEmpty()) {
                        Spacer(Modifier.height(DsSpacing.Lg))
                        Text(resolveSuiteString { accuracyTrendByCharLabel }, color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(DsSpacing.Sm))
                        writingCharacters.forEach { char ->
                            val trend = learning.writingAccuracyTrend(char, 30)
                            Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                                Text(char, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium, modifier = Modifier.width(44.dp))
                                Text(
                                    text = "${trend.size} attempts",
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption,
                                    modifier = Modifier.width(80.dp)
                                )
                                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                                    trend.takeLast(14).forEach { (_, acc) ->
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .height(16.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    when {
                                                        acc >= 0.7f -> successColor()
                                                        acc >= 0.5f -> warningColor()
                                                        else -> errorColor()
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // EXAM ANALYTICS — real results from the unified store: study-vs-exam
        // gap, accuracy by question type and the exam history.
        val gap = learning.studyVsExamGap()
        val examAggregates = learning.examAggregates()
        val examByType = learning.examAccuracyByExamType()
        val examHistory = learning.examHistory(10)
        val examTrend = learning.examTrend(14)
        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(
                    title = resolveSuiteString { examAnalyticsTitle },
                    subtitle = resolveSuiteString { examAnalyticsSubtitle },
                    action = {
                        DsBadge(text = "${examAggregates.count} exams · ${examAggregates.totalQuestions} questions")
                    }
                )
                Spacer(Modifier.height(DsSpacing.Lg))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    DsStatTile(resolveSuiteString { examsTakenLabel }, examAggregates.count.toString(), Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { averageScoreLabel }, "${examAggregates.averageScore}%", Modifier.weight(1f))
                    DsStatTile("Best / worst", "${examAggregates.bestScore}% / ${examAggregates.worstScore}%", Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { studyAccuracyLabel }, "${(gap.studyAccuracy * 100).toInt()}%", Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { examRecognitionLabel }, "${(gap.examRecognitionAccuracy * 100).toInt()}%", Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { examProductionLabel }, "${(gap.examProductionAccuracy * 100).toInt()}%", Modifier.weight(1f))
                    DsStatTile(resolveSuiteString { examWritingLabel }, "${(gap.examWritingAccuracy * 100).toInt()}%", Modifier.weight(1f))
                }

                if (examByType.isNotEmpty()) {
                    Spacer(Modifier.height(DsSpacing.Lg))
                    Text(resolveSuiteString { accuracyByExamTypeLabel }, color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(DsSpacing.Sm))
                    examByType.toList().sortedBy { it.second }.forEach { (type, acc) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                            Text(type, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(180.dp))
                            DsProgressBar(
                                fraction = acc,
                                modifier = Modifier.weight(1f),
                                color = when {
                                    acc >= 0.8f -> successColor()
                                    acc >= 0.6f -> warningColor()
                                    else -> errorColor()
                                }
                            )
                            Spacer(Modifier.width(DsSpacing.Sm))
                            Text("${(acc * 100).toInt()}%", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(44.dp))
                        }
                    }
                }

                if (examTrend.isNotEmpty()) {
                    Spacer(Modifier.height(DsSpacing.Lg))
                    Text(resolveSuiteString { scoreTrendLabel }, color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(DsSpacing.Sm))
                    val maxTrend = (examTrend.maxOfOrNull { it.second } ?: 100).coerceAtLeast(1)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                        examTrend.forEach { (label, score) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(score.toFloat() / maxTrend.coerceAtLeast(1))
                                        .height(22.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (score >= 60) successColor() else errorColor())
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(label.substring(5), color = sc.textMuted, fontSize = DsType.Caption)
                            }
                        }
                    }
                }

                if (examHistory.isNotEmpty()) {
                    Spacer(Modifier.height(DsSpacing.Lg))
                    Text(resolveSuiteString { recentExamsLabel }, color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(DsSpacing.Sm))
                    examHistory.take(6).forEach { exam ->
                        Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                            Text(exam.title, color = sc.textSecondary, fontSize = DsType.Body, maxLines = 1, modifier = Modifier.weight(1f))
                            Text("${exam.correctCount}/${exam.questionCount}", color = sc.textMuted, fontSize = DsType.Caption)
                            Spacer(Modifier.width(DsSpacing.Md))
                            DsBadge(
                                text = "${exam.percentage}%",
                                tint = if (exam.percentage >= 60) successColor() else errorColor()
                            )
                        }
                    }
                }
            }
        }

        // CARD HISTORY — drill into any card's full review timeline.
        val historyCardIds = cards.take(60).map { it.id }
        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(
                    title = "Card History",
                    subtitle = "Review timeline for recently seen cards — every entry is a real review event"
                )
                Spacer(Modifier.height(DsSpacing.Lg))
                var historyCardId by remember { mutableStateOf<String?>(null) }
                val targetId = historyCardId ?: historyCardIds.firstOrNull()
                val historyRows = targetId?.let { learning.cardHistory(it, 30) }.orEmpty()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                    DsSelect(
                        selected = targetId ?: "",
                        options = historyCardIds,
                        onSelected = { historyCardId = it },
                        labelOf = { id -> cards.firstOrNull { c -> c.id == id }?.character ?: id.take(12) },
                        modifier = Modifier.weight(1f)
                    )
                    if (historyRows.isNotEmpty()) {
                        DsBadge(text = "${historyRows.size} reviews")
                    }
                }
                Spacer(Modifier.height(DsSpacing.Sm))
                if (historyRows.isEmpty()) {
                    Text("No review events recorded for this card yet.", color = sc.textMuted, fontSize = DsType.Body)
                } else {
                    historyRows.take(12).forEach { entry ->
                        Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                entry.event.reviewedAt.toString().substring(0, 16).replace('T', ' '),
                                color = sc.textMuted,
                                fontSize = DsType.Caption,
                                modifier = Modifier.width(130.dp)
                            )
                            Text(entry.event.rating.name, color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.Medium, modifier = Modifier.width(64.dp))
                            Text(entry.cardTypeLabel, color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(110.dp))
                            Text(
                                text = "${entry.event.intervalBefore.toInt()}d → ${entry.event.intervalAfter.toInt()}d",
                                color = sc.textMuted,
                                fontSize = DsType.Caption,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (entry.event.correct) "correct" else "miss",
                                color = if (entry.event.correct) successColor() else errorColor(),
                                fontSize = DsType.Caption,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Media immersion KPIs — period-aware, with deltas vs the previous window.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsStatTile(
                label = "Media watched",
                value = mediaDurationLabel(mediaWatchMs),
                modifier = Modifier.weight(1f),
                delta = deltaText((mediaWatchMs / 60000).toInt(), prevWatchMs?.div(60000)?.toInt()),
                deltaPositive = mediaWatchMs >= (prevWatchMs ?: 0L)
            )
            DsStatTile(
                label = "Immersion study",
                value = mediaDurationLabel(mediaStudyMs),
                modifier = Modifier.weight(1f),
                delta = deltaText((mediaStudyMs / 60000).toInt(), prevStudyMs?.div(60000)?.toInt()),
                deltaPositive = mediaStudyMs >= (prevStudyMs ?: 0L)
            )
            DsStatTile(
                label = "Dictionary lookups",
                value = mediaLookups.toString(),
                modifier = Modifier.weight(1f),
                delta = deltaText(mediaLookups, prevLookups),
                deltaPositive = mediaLookups >= (prevLookups ?: 0)
            )
            DsStatTile(
                label = "Sentences mined",
                value = miningPeriod.toString(),
                modifier = Modifier.weight(1f),
                delta = deltaText(miningPeriod, prevMining),
                deltaPositive = miningPeriod >= (prevMining ?: 0)
            )
            DsStatTile(
                label = "Watch sessions",
                value = mediaSessions.toString(),
                modifier = Modifier.weight(1f)
            )
            DsStatTile(
                label = "Media days",
                value = mediaActiveDays.toString(),
                modifier = Modifier.weight(1f)
            )
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

        // DAILY ACTIVITY — real per-day totals: engaged time (measured from
        // actual interaction, never app-open time), engagement sessions and
        // the decks touched that day (from real review events).
        DailyActivityCard(state, today)

        // Media immersion — the 14-day chart plus honest lifetime totals.
        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(
                    title = "Media Activity",
                    subtitle = "Immersion watch time vs study time, per day",
                    action = {
                        Text(
                            text = "${mediaStats.days.size} media days · ${mediaDurationLabel(mediaStats.totalWatchMs)} total",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                )
                Spacer(Modifier.height(DsSpacing.Lg))
                if (mediaStats.days.isEmpty()) {
                    Text(
                        "No media activity yet — watch something in the Media workspace and it appears here.",
                        color = sc.textMuted,
                        fontSize = DsType.Body
                    )
                } else {
                    MediaActivityChart(mediaDaily)
                    Spacer(Modifier.height(DsSpacing.Md))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                    ) {
                        Text("14 days ago", color = sc.textMuted, fontSize = DsType.Caption)
                        ChartLegend(
                            listOf(
                                "Study (immersion)" to accent().primary,
                                "Watch (leisure)" to accent().primary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text("today", color = sc.textMuted, fontSize = DsType.Caption)
                    }
                }
                Spacer(Modifier.height(DsSpacing.Lg))
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xl)) {
                    MediaStatPill("Total watch", mediaDurationLabel(mediaStats.totalWatchMs))
                    MediaStatPill("Immersion study", mediaDurationLabel(mediaStats.totalStudyMs))
                    MediaStatPill("Lookups", mediaStats.totalLookups.toString())
                    MediaStatPill("Mined from media", mediaStats.totalMined.toString())
                    MediaStatPill("Sessions", mediaStats.totalSessions.toString())
                }
            }
        }

        // Mining activity — every source, real records from MiningStatisticsStore.
        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(
                    title = "Mining Activity",
                    subtitle = "Sentences mined from every source, per day",
                    action = {
                        Text(
                            text = "${miningStats.totalMined} total · ${miningStats.bySource.size} source(s)",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                )
                Spacer(Modifier.height(DsSpacing.Lg))
                if (miningStats.totalMined == 0) {
                    Text(
                        "Nothing mined yet — look up words in the Dictionary or mine subtitles in Media and it appears here.",
                        color = sc.textMuted,
                        fontSize = DsType.Body
                    )
                } else {
                    MiningActivityChart(miningDaily)
                    Spacer(Modifier.height(DsSpacing.Md))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                    ) {
                        Text("7 days ago", color = sc.textMuted, fontSize = DsType.Caption)
                        ChartLegend(listOf("Mined" to accent().primary), modifier = Modifier.weight(1f))
                        Text("today", color = sc.textMuted, fontSize = DsType.Caption)
                    }
                    if (miningSources.isNotEmpty()) {
                        Spacer(Modifier.height(DsSpacing.Lg))
                        Text("By source", color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(DsSpacing.Sm))
                        miningSources.forEach { (source, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(source, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(110.dp))
                                DsProgressBar(
                                    fraction = count.toFloat() / miningStats.totalMined.coerceAtLeast(1),
                                    modifier = Modifier.weight(1f),
                                    color = accent().primary.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.width(DsSpacing.Sm))
                                Text(count.toString(), color = sc.textPrimary, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(DsSpacing.Lg))
                    Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xl)) {
                        MediaStatPill("This period", miningPeriod.toString())
                        MediaStatPill("Mining rate (7d)", "%.1f".format(miningWeek / 7f) + "/day")
                        MediaStatPill("All-time", miningStats.totalMined.toString())
                    }
                    if (recentMines.isNotEmpty()) {
                        Spacer(Modifier.height(DsSpacing.Lg))
                        Text("Recent mines", color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(DsSpacing.Sm))
                        recentMines.forEach { rec ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(accent().primary.copy(alpha = 0.6f))
                                )
                                Spacer(Modifier.width(DsSpacing.Sm))
                                Text(rec.headword, color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Text(rec.source, color = sc.textMuted, fontSize = DsType.Caption)
                                Text(rec.createdAt.take(10), color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.padding(start = DsSpacing.Md))
                            }
                        }
                    }
                }
            }
        }

        // JLPT coverage + learning velocity — both computed from the real card pool.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(
                        title = "JLPT Coverage",
                        subtitle = "Cards tagged with a JLPT level — study coverage, not a certification"
                    )
                    Spacer(Modifier.height(DsSpacing.Lg))
                    if (jlptCards.isEmpty()) {
                        Text(
                            "No JLPT-tagged cards yet — mined cards inherit JLPT from the dictionary automatically.",
                            color = sc.textMuted,
                            fontSize = DsType.Body
                        )
                    } else {
                        val maxLevelCount = jlptCards.groupingBy { (it.jlpt ?: 0).coerceIn(1, 5) }.eachCount().values.maxOrNull() ?: 1
                        (5 downTo 1).forEach { level ->
                            val count = jlptCards.count { it.jlpt == level }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = DsSpacing.Xs)) {
                                Text("N$level", color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
                                DsProgressBar(fraction = count.toFloat() / maxLevelCount, modifier = Modifier.weight(1f), color = jlptColor(level))
                                Spacer(Modifier.width(DsSpacing.Sm))
                                Text(count.toString(), color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(40.dp))
                            }
                        }
                        Spacer(Modifier.height(DsSpacing.Sm))
                        Text(
                            "${jlptCards.size} of ${cards.size} cards carry JLPT data. N5 = easiest band, N1 = hardest — this reflects your pool's tagging, not exam readiness.",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                }
            }
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg)) {
                    DsSectionHeader(title = "Learning Velocity", subtitle = "How fast new material enters your pool")
                    Spacer(Modifier.height(DsSpacing.Md))
                    VelocityStat("New cards (7d)", newCardsWeek.toString())
                    VelocityStat("New cards (30d)", newCardsMonth.toString())
                    VelocityStat("Reviews (7d)", reviewsLast7.toString())
                    VelocityStat("Study time (7d)", formatDuration(studyLast7))
                    Spacer(Modifier.height(DsSpacing.Sm))
                    Text(
                        "Velocity is derived from card creation dates and daily summaries — mined cards count the moment they land.",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
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
                            "Again" to errorColor(),
                            "Hard" to warningColor(),
                            "Good" to infoColor(),
                            "Easy" to successColor()
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
                    color = if (progress.complete) successColor() else sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            Spacer(Modifier.height(4.dp))
            DsProgressBar(fraction = progress.fraction, color = if (progress.complete) successColor() else Color.Unspecified)
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
    SrsStatus.New -> newColor()
    SrsStatus.Learning -> infoColor()
    SrsStatus.Review -> successColor()
    SrsStatus.Relearning -> warningColor()
    SrsStatus.Suspended, SrsStatus.Buried -> surfaceColors().textMuted
}

/** Compact label for a Long millisecond duration (5m / 3h 12m / 2d 4h). */
private fun mediaDurationLabel(ms: Long): String {
    val totalMinutes = (ms / 60000).coerceAtLeast(0)
    return when {
        totalMinutes < 60 -> "${totalMinutes}m"
        totalMinutes < 60 * 24 -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
        else -> "${totalMinutes / 1440}d ${(totalMinutes % 1440) / 60}h"
    }
}

@Composable
private fun MediaStatPill(label: String, value: String) {
    val sc = surfaceColors()
    Column {
        Text(value, color = accent().primary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
    }
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

/** Accent color per JLPT band (N5 easiest → N1 hardest). */
@Composable
private fun jlptColor(level: Int): Color = when (level) {
    5 -> successColor()
    4 -> infoColor()
    3 -> warningColor()
    2 -> errorColor()
    else -> surfaceColors().textMuted
}

/** Kana writing accuracy from real stroke evaluations, or an honest "—". */
private fun kanaWritingAccuracyLabel(learning: ua.syt0r.kanji.desktop.engine.learning.LearningEngine): String {
    val kanaNoteIds = learning.notes.filter { it.kind == LearningItemKind.Kana }.map { it.id }.toSet()
    val attempts = learning.writingAttempts.filter { it.noteId in kanaNoteIds }
    if (attempts.isEmpty()) return "—"
    val accuracy = attempts.map { it.accuracy }.average().toFloat().coerceIn(0f, 1f)
    return "${(accuracy * 100).toInt()}%"
}

/** Label + value row used by the Learning Velocity card. */
@Composable
private fun VelocityStat(label: String, value: String) {
    val sc = surfaceColors()
    Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
        Text(value, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Bold)
    }
}

// ============================================
// DAILY ACTIVITY — engaged time, sessions and
// decks per day, straight from the activity
// tracker (real interaction) and review events.
// ============================================

private data class DailyActivityRow(
    val date: LocalDate,
    val engaged: Duration,
    val sessions: Int,
    val decks: List<String>
)

/**
 * Last-14-days table of real engagement totals. Refreshes every 30 s (one
 * bounded coroutine, cancelled with the composition) so "today" keeps
 * accumulating while this view is open — same freshness the media chart has.
 */
@Composable
private fun DailyActivityCard(state: AppState, today: LocalDate) {
    val sc = surfaceColors()
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }
    val rows = remember(tick) { buildDailyActivityRows(state, today) }
    val active = rows
        .filter { it.engaged > Duration.ZERO || it.sessions > 0 || it.decks.isNotEmpty() }
        .sortedByDescending { it.date }
    val totalEngaged = rows.fold(Duration.ZERO) { acc, r -> acc + r.engaged }

    DsCard {
        Column(Modifier.padding(DsSpacing.Lg)) {
            DsSectionHeader(
                title = "Daily Activity",
                subtitle = "Engaged time comes from real interaction — sessions are continuous stretches of activity, never app-open time",
                action = {
                    Text(
                        text = "${active.size}/14 active days · ${formatDuration(totalEngaged)} engaged",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            )
            Spacer(Modifier.height(DsSpacing.Lg))
            if (active.isEmpty()) {
                Text(
                    "No study activity yet — review cards, practise writing or watch media and your daily totals appear here.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Day", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.weight(1f))
                    Text("Engaged", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(84.dp))
                    Text("Sessions", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(72.dp))
                    Text("Decks", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.weight(2f))
                }
                Spacer(Modifier.height(2.dp))
                active.forEach { row ->
                    val isToday = row.date == today
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isToday) "Today" else shortDayLabel(row.date),
                            color = if (isToday) accent().primary else sc.textSecondary,
                            fontSize = DsType.Body,
                            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatDuration(row.engaged),
                            color = if (row.engaged > Duration.ZERO) sc.textPrimary else sc.textMuted,
                            fontSize = DsType.Body,
                            modifier = Modifier.width(84.dp)
                        )
                        Text(
                            text = row.sessions.toString(),
                            color = if (row.sessions > 0) sc.textPrimary else sc.textMuted,
                            fontSize = DsType.Body,
                            modifier = Modifier.width(72.dp)
                        )
                        Text(
                            text = deckLabel(row.decks),
                            color = sc.textSecondary,
                            fontSize = DsType.Body,
                            maxLines = 1,
                            modifier = Modifier.weight(2f)
                        )
                    }
                }
            }
        }
    }
}

private fun buildDailyActivityRows(state: AppState, today: LocalDate): List<DailyActivityRow> =
    state.activity.dayActivities(today, 14).map { act ->
        DailyActivityRow(
            date = act.date,
            engaged = act.engaged,
            sessions = act.sessions,
            decks = state.decksStudiedOn(act.date)
        )
    }

private fun shortDayLabel(date: LocalDate): String =
    "${date.dayOfWeek.name.take(3)} ${date.dayOfMonth} ${date.month.name.take(3)}"

private fun deckLabel(decks: List<String>): String {
    if (decks.isEmpty()) return "—"
    val shown = decks.take(3).joinToString(" · ")
    return if (decks.size > 3) "$shown +${decks.size - 3}" else shown
}
