package ua.syt0r.kanji.desktop.ui.mistakes

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
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
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsStatTile
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.errorColor
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.desktop.engine.learning.MistakeCategory
import ua.syt0r.kanji.desktop.engine.learning.MistakeItem

// ============================================
// MISTAKES
// A real mistakes queue generated from actual
// recorded failures — Again reviews, failed
// writing attempts, wrong exam answers and
// lapsed cards. Studying mistakes routes into
// the normal review flow, so SRS state and
// statistics update exactly like regular study.
// ============================================

@Composable
fun MistakesView(state: AppState) {
    val sc = surfaceColors()
    val learning = state.learning
    val snapshot = remember(learning.revision) { learning.mistakeSnapshot() }
    val breakdown = remember(learning.revision) { learning.mistakeBreakdown() }
    val queue = remember(learning.revision) { learning.mistakeQueue(200) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            DsStatTile("Mistakes", queue.map { it.cardId }.distinct().size.toString(), Modifier.weight(1f))
            DsStatTile("Again reviews", snapshot.againEvents.toString(), Modifier.weight(1f))
            DsStatTile("Writing misses", snapshot.writingMistakes.toString(), Modifier.weight(1f))
            DsStatTile("Exam misses", snapshot.examMistakes.toString(), Modifier.weight(1f))
            DsStatTile("Lapsed cards", snapshot.lapsedCards.toString(), Modifier.weight(1f))
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                DsSectionHeader(
                    title = "Study your mistakes",
                    subtitle = "Review the exact cards you got wrong — ratings update SRS like normal study"
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    DsButton(
                        text = "Study mistakes (${snapshot.againEvents.coerceAtLeast(1)} cards available)",
                        icon = Icons.Default.PlayArrow,
                        onClick = { state.startMistakesReview() }
                    )
                    DsBadge(text = "real queue", tint = successColor())
                    Text(
                        "Everything here is derived from your review events, writing attempts and exam answers — nothing is fabricated.",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                Text(
                    "Exams can also target your mistakes: open Exams → Quick exams → Mistakes review.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsSectionHeader(title = "By category", subtitle = "Where your mistakes come from")
                Spacer(Modifier.height(DsSpacing.Sm))
                if (breakdown.isEmpty()) {
                    Text("No mistakes recorded yet.", color = sc.textMuted, fontSize = DsType.Body)
                } else {
                    MistakeCategory.entries.forEach { category ->
                        val count = breakdown[category] ?: 0
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = DsSpacing.Xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(mistakeColor(category))
                            )
                            Spacer(Modifier.width(DsSpacing.Sm))
                            Text(category.label, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                            Text(count.toString(), color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsSectionHeader(
                    title = "Mistake queue",
                    subtitle = "Worst first — most frequent failures on top",
                    action = {
                        Text(
                            text = "${queue.size} entries",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                )
                Spacer(Modifier.height(DsSpacing.Sm))
                if (queue.isEmpty()) {
                    DsEmptyState(
                        title = "No mistakes to show",
                        message = "Get some reviews under your belt and the failures worth fixing will land here automatically.",
                        icon = Icons.Default.Warning
                    )
                } else {
                    queue.take(100).forEach { item ->
                        MistakeRow(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun MistakeRow(item: MistakeItem) {
    val sc = surfaceColors()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            item.expression,
            color = sc.textPrimary,
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(140.dp)
        )
        DsBadge(text = item.category.label, tint = mistakeColor(item.category))
        Spacer(Modifier.width(DsSpacing.Sm))
        Text(
            if (item.count > 1) "${item.count}×" else "1×",
            color = if (item.count >= 3) errorColor() else sc.textMuted,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Text(
            item.lastMistakeAt.toString().take(16).replace('T', ' '),
            color = sc.textMuted,
            fontSize = DsType.Caption
        )
    }
}

@Composable
private fun mistakeColor(category: MistakeCategory): Color = when (category) {
    MistakeCategory.Writing -> warningColor()
    MistakeCategory.Reading -> Color(0xFF42A5F5)
    MistakeCategory.Meaning -> Color(0xFFAB47BC)
    MistakeCategory.Recognition -> accent().primary
    MistakeCategory.Exam -> errorColor()
    MistakeCategory.Lapses -> Color(0xFFFF7043)
}
