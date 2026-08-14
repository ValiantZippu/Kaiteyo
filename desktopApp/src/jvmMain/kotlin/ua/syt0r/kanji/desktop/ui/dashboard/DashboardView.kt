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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsStatTile
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.dueColor
import ua.syt0r.kanji.desktop.designsystem.errorColor
import ua.syt0r.kanji.desktop.designsystem.infoColor
import ua.syt0r.kanji.desktop.designsystem.newColor
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.stats.GoalsEngine
import ua.syt0r.kanji.desktop.engine.stats.HeatmapEngine
import ua.syt0r.kanji.desktop.engine.stats.LearningCurveEngine
import ua.syt0r.kanji.desktop.engine.stats.HeatmapCell
import ua.syt0r.kanji.desktop.engine.stats.WeakSpotEngine
import ua.syt0r.kanji.desktop.model.CollectionDef
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.StudyMode
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

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
        // Hero: continue studying (deck with the most due/new work) or start review
        val continueDeck = remember(state.library.revision, state.cards.size) {
            state.library.allDecks()
                .map { deck -> deck to state.library.deckStats(deck, state.cards.toList()) }
                .filter { it.second.anyDue + it.second.anyNew > 0 }
                .maxByOrNull { it.second.anyDue + it.second.anyNew }
                ?.first
        }
        DsCard(elevated = true) {
            Row(
                modifier = Modifier.padding(DsSpacing.Xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (continueDeck != null) "Continue \"${continueDeck.name}\"" else "Ready to study?",
                        color = sc.textPrimary,
                        fontSize = DsType.Heading,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(DsSpacing.Xs))
                    Text(
                        text = if (continueDeck != null) {
                            val s = state.library.deckStats(continueDeck, state.cards.toList())
                            "${s.anyDue} due · ${s.anyNew} new in this deck · ${state.dueCount()} cards due across your library"
                        } else {
                            "${state.dueCount()} cards due now · ${state.newCount()} new cards available"
                        },
                        color = sc.textMuted,
                        fontSize = DsType.Body
                    )
                }
                if (continueDeck != null) {
                    DsButton(
                        text = "Continue",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            val mode = StudyMode.forKind(continueDeck.kind).firstOrNull()
                            if (mode == StudyMode.Writing) state.startLibraryWriting(continueDeck.id)
                            else if (mode != null) state.startLibraryStudy(continueDeck.id, mode)
                            else state.startReview()
                        }
                    )
                } else {
                    DsButton(
                        text = "Start Review",
                        icon = Icons.Default.PlayArrow,
                        onClick = { state.startReview() }
                    )
                }
            }
        }

        // Quick actions
        DsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick actions",
                    color = surfaceColors().textMuted,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                DsButton(
                    text = "Study",
                    icon = Icons.Default.PlayArrow,
                    compact = true,
                    onClick = { state.startReview() }
                )
                DsButton(
                    text = "Writing",
                    icon = Icons.Default.Create,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { state.startWritingPractice() }
                )
                DsButton(
                    text = "Browse",
                    icon = Icons.Default.GridView,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { state.currentView = WorkspaceView.Browser }
                )
                DsButton(
                    text = "New card",
                    icon = Icons.Default.Add,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { state.newCard() }
                )
                DsButton(
                    text = "Collections",
                    icon = Icons.Default.Folder,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { state.currentView = WorkspaceView.Collections }
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
                delta = "${state.dueCount()} review${if (state.dueCount() == 1) "" else "s"}",
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
                label = "Study time",
                value = state.formatDuration(state.totalStudyTime()),
                modifier = Modifier.weight(1f),
                delta = "${state.totalReviews()} total reviews"
            )
            DsStatTile(
                label = "Total cards",
                value = cards.size.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsStatTile(
                label = "This week",
                value = state.weeklyReviews().toString(),
                modifier = Modifier.weight(1f),
                delta = "${state.studiedDaysInWeek()}/7 days active"
            )
            DsStatTile(
                label = "Streak",
                value = "${HeatmapEngine.currentStreak(state.summaries)}d",
                modifier = Modifier.weight(1f),
                delta = "current streak"
            )
            DsStatTile(
                label = "Suspended",
                value = state.suspendedCount().toString(),
                modifier = Modifier.weight(1f)
            )
            DsStatTile(
                label = "Recalled",
                value = state.collections.collections.count { it.favorite }.toString(),
                modifier = Modifier.weight(1f),
                delta = "favorite collections"
            )
        }

        // Immersion: media activity today
        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                DsSectionHeader(
                    title = "Immersion",
                    subtitle = "Media activity today",
                    action = {
                        DsButton(
                            text = "Open Media",
                            icon = Icons.Default.PlayArrow,
                            kind = DsButtonKind.Secondary,
                            compact = true,
                            onClick = { state.currentView = WorkspaceView.Media }
                        )
                    }
                )
                Spacer(Modifier.height(DsSpacing.Md))
                val mediaStats = state.media.statistics
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val todayStat = mediaStats.day(today)
                val last7 = today.minus(6, DateTimeUnit.DAY)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    ImmersionStat(
                        label = "Watched today",
                        value = MediaEngine.formatTime(todayStat.watchMs),
                        detail = "${mediaStats.watchMsBetween(last7, today) / 3600000}h in the last 7 days",
                        modifier = Modifier.weight(1f)
                    )
                    ImmersionStat(
                        label = "Media study",
                        value = MediaEngine.formatTime(todayStat.studyMs),
                        detail = if (todayStat.studyMs > 0) "counts toward study time" else "enable study mode in the player",
                        modifier = Modifier.weight(1f)
                    )
                    ImmersionStat(
                        label = "Mined today",
                        value = todayStat.mined.toString(),
                        detail = "${mediaStats.minedBetween(last7, today)} in the last 7 days",
                        modifier = Modifier.weight(1f)
                    )
                    ImmersionStat(
                        label = "Lookups today",
                        value = todayStat.lookups.toString(),
                        detail = "dictionary lookups",
                        modifier = Modifier.weight(1f)
                    )
                    ImmersionStat(
                        label = "Mined all (7d)",
                        value = state.miningStatistics.minedBetween(last7, today).toString(),
                        detail = "dictionary · media · OCR · browser",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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
                                    color = if (progress.complete) successColor() else sc.textMuted,
                                    fontSize = DsType.Caption,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            DsProgressBar(
                                fraction = progress.fraction,
                                color = if (progress.complete) successColor() else Color.Unspecified
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

        // Study recommendations
        StudyRecommendationsCard(state)

        // Pinned decks + recent imports
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            PinnedDecksCard(state, Modifier.weight(1f))
            RecentImportsCard(state, Modifier.weight(1f))
        }

        // Recent decks + recently added cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            RecentDecksCard(state, Modifier.weight(1f))
            RecentlyAddedCard(state, Modifier.weight(1f))
        }
    }
}

// ============================================
// PINNED DECKS
// ============================================

@Composable
private fun PinnedDecksCard(state: AppState, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val pinned = remember(state.library.revision) { state.library.allDecks().filter { it.pinned } }

    DsCard(modifier = modifier) {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(
                title = "Pinned decks",
                action = {
                    androidx.compose.material3.TextButton(onClick = { state.currentView = WorkspaceView.Library }) {
                        androidx.compose.material3.Text("Library", color = accent().primary)
                    }
                }
            )
            if (pinned.isEmpty()) {
                Text(
                    text = "Pin decks from the Library to keep your favourites one click away.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }
            pinned.forEach { deck ->
                val stats = state.library.deckStats(deck, state.cards.toList())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                        .clickable { state.currentView = WorkspaceView.Library }
                        .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = accent().primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(DsSpacing.Sm))
                    Column(Modifier.weight(1f)) {
                        Text(deck.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${deck.kind.label} · ${stats.total} cards",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                    if (stats.anyDue + stats.anyNew > 0) {
                        DsBadge(text = "${stats.anyDue + stats.anyNew} ready", tint = dueColor())
                    }
                    DsButton(
                        text = "Study",
                        icon = Icons.Default.PlayArrow,
                        compact = true,
                        onClick = {
                            val mode = StudyMode.forKind(deck.kind).firstOrNull()
                            if (mode == StudyMode.Writing) state.startLibraryWriting(deck.id)
                            else if (mode != null) state.startLibraryStudy(deck.id, mode)
                        }
                    )
                }
            }
        }
    }
}

// ============================================
// RECENT IMPORTS
// ============================================

@Composable
private fun RecentImportsCard(state: AppState, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val imports = state.activityLog.entries.filter { it.category == ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Import }.take(5)

    DsCard(modifier = modifier) {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(
                title = "Recent imports",
                action = {
                    androidx.compose.material3.TextButton(onClick = { state.currentView = WorkspaceView.Transfer }) {
                        androidx.compose.material3.Text("Transfer", color = accent().primary)
                    }
                }
            )
            if (imports.isEmpty()) {
                Text(
                    text = "No imports yet — bring content in from the Import / Export view.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }
            imports.forEach { entry ->
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
                            .background(infoColor())
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

// ============================================
// STUDY RECOMMENDATIONS
// ============================================

private data class Recommendation(val title: String, val detail: String, val view: WorkspaceView)

@Composable
private fun StudyRecommendationsCard(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

    val recommendations = remember(state.cards.size, state.summaries.size, state.reviewSession != null) {
        buildList {
            val due = state.dueCount()
            if (due > 0) {
                add(Recommendation("Review $due due cards", "Keep your streak alive and clear today's queue.", WorkspaceView.Review))
            }
            val newCount = state.newCount()
            if (newCount > 0 && size < 3) {
                add(Recommendation("Learn $newCount new cards", "Fresh material is waiting — introduce it gradually.", WorkspaceView.Review))
            }
            val weak = WeakSpotEngine.mostDifficult(state.cards.toList(), limit = 1)
            if (weak.isNotEmpty() && size < 3) {
                add(
                    Recommendation(
                        "Retrain \"${weak.first().character}\"",
                        "Lowest accuracy in your pool — worth a dedicated pass.",
                        WorkspaceView.Browser
                    )
                )
            }
            val studiedToday = state.summaries.any { it.day == today && (it.newCount + it.reviewCount) > 0 }
            if (!studiedToday && size < 3) {
                add(Recommendation("Start today's session", "A few minutes now compounds into a long streak.", WorkspaceView.Dashboard))
            }
            val recent = state.cards.sortedByDescending { it.createdAt }.take(3)
            if (recent.isNotEmpty() && size < 3) {
                add(Recommendation("Review recent additions", "\"${recent.first().character}\" was added recently — reinforce it.", WorkspaceView.Browser))
            }
            if (isEmpty()) {
                add(Recommendation("All caught up", "Nothing due right now — perfect time to explore or mine new cards.", WorkspaceView.Browser))
            }
        }
    }

    DsCard {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(
                title = "Recommended for you",
                subtitle = "Based on your current workload"
            )
            recommendations.forEach { rec ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                        .clickable { state.currentView = rec.view }
                        .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = ac.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(DsSpacing.Sm))
                    Column(Modifier.weight(1f)) {
                        Text(rec.title, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
                        Text(rec.detail, color = sc.textMuted, fontSize = DsType.Caption)
                    }
                }
            }
        }
    }
}

// ============================================
// RECENT DECKS
// ============================================

@Composable
private fun RecentDecksCard(state: AppState, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val recent = state.collections.collections
        .filter { !it.archived }
        .sortedWith(compareByDescending<CollectionDef> { it.pinned }.thenByDescending { it.favorite }.thenByDescending { it.createdAt })
        .take(4)

    DsCard(modifier = modifier) {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(
                title = "Recent decks",
                action = {
                    androidx.compose.material3.TextButton(onClick = { state.currentView = WorkspaceView.Collections }) {
                        androidx.compose.material3.Text("All", color = accent().primary)
                    }
                }
            )
            if (recent.isEmpty()) {
                Text(
                    text = "No collections yet — create one from the Collections view.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }
            recent.forEach { def ->
                val cardCount = state.collections.resolveCards(def, state.cards.toList()).size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                        .clickable {
                            state.selectedCardIds.clear()
                            state.selectedCard = null
                            state.currentView = WorkspaceView.Collections
                        }
                        .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (def.pinned) accent().primary else sc.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(DsSpacing.Sm))
                    Column(Modifier.weight(1f)) {
                        Text(def.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
                        Text("$cardCount cards", color = sc.textMuted, fontSize = DsType.Caption)
                    }
                    DsButton(
                        text = "Study",
                        icon = Icons.Default.PlayArrow,
                        compact = true,
                        onClick = { state.startReview(collection = def) }
                    )
                }
            }
        }
    }
}

// ============================================
// RECENTLY ADDED CARDS
// ============================================

@Composable
private fun RecentlyAddedCard(state: AppState, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val recent = state.cards.sortedByDescending { it.createdAt }.take(6)

    DsCard(modifier = modifier) {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(
                title = "Recently added",
                action = {
                    androidx.compose.material3.TextButton(onClick = { state.currentView = WorkspaceView.Browser }) {
                        androidx.compose.material3.Text("Browse", color = accent().primary)
                    }
                }
            )
            if (recent.isEmpty()) {
                Text(
                    text = "Nothing added yet — mine from the dictionary or use New card.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }
            recent.forEach { card ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                        .clickable {
                            state.selectedCard = card
                            state.currentView = WorkspaceView.Browser
                        }
                        .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.character.ifBlank { "—" },
                        color = sc.textPrimary,
                        fontSize = DsType.Title,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(44.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = card.meaning.ifBlank { "No meaning yet" },
                            color = sc.textSecondary,
                            fontSize = DsType.Body,
                            maxLines = 1
                        )
                        Text(
                            text = "created ${ua.syt0r.kanji.desktop.engine.history.ActivityFormatters.relative(card.createdAt)}",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                    DsBadge(text = card.status.name, tint = if (card.status == SrsStatus.New) infoColor() else accent().primary)
                    DsIconButton(
                        icon = Icons.Default.Edit,
                        onClick = { state.openEditor(card) },
                        contentDescription = "Edit card",
                        size = 28.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun categoryColor(category: ua.syt0r.kanji.desktop.engine.history.ActivityCategory): Color =
    when (category) {
        ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Review -> successColor()
        ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Study -> infoColor()
        ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Undo -> warningColor()
        ua.syt0r.kanji.desktop.engine.history.ActivityCategory.System -> newColor()
        else -> surfaceColors().textMuted
    }

// ============================================
// IMMERSION STAT
// ============================================

@Composable
private fun ImmersionStat(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Lg))
            .background(sc.surfaceInteractive.copy(alpha = 0.5f))
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = sc.textMuted,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = sc.textPrimary,
            fontSize = DsType.Title,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = detail,
            color = sc.textMuted,
            fontSize = DsType.Caption,
            maxLines = 1
        )
    }
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
                        .background(if (point.accuracy > 0.7f) ac.primary.copy(alpha = 0.85f) else errorColor().copy(alpha = 0.8f))
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
