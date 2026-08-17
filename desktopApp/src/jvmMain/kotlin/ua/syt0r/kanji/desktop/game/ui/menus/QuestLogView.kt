package ua.syt0r.kanji.desktop.game.ui.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.game.GameSession
import ua.syt0r.kanji.desktop.game.quest.Quest
import ua.syt0r.kanji.desktop.game.quest.QuestCategory
import ua.syt0r.kanji.desktop.game.quest.QuestState

private enum class QuestFilter { All, Active, Completed }

/**
 * Quest log (spec §20-22): real-world-like tasks with progress, grouped by
 * state with filter chips and category chips. New quests appear as
 * prerequisites complete — never a linear list.
 */
@Composable
fun QuestLogView(session: GameSession) {
    val quests = session.quests
    val active = quests.activeQuests()
    val available = quests.availableQuests()
    val completed = quests.allQuests.filter { quests.isComplete(it.id) }
    var filter by remember { mutableStateOf(QuestFilter.All) }
    // Category chip filter (spec §21): browse quests by learning type.
    var category by remember { mutableStateOf<QuestCategory?>(null) }

    val byCategory = { list: List<Quest> ->
        if (category == null) list else list.filter { it.category == category }
    }

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            FilterChip("All (${active.size + available.size + completed.size})", filter == QuestFilter.All) { filter = QuestFilter.All }
            FilterChip("Active (${active.size})", filter == QuestFilter.Active) { filter = QuestFilter.Active }
            FilterChip("Completed (${completed.size})", filter == QuestFilter.Completed) { filter = QuestFilter.Completed }
        }
        // Category chips — only categories that have quests in this world.
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            FilterChip("All types", category == null) { category = null }
            QuestCategory.entries
                .filter { cat -> quests.allQuests.any { it.category == cat } }
                .forEach { cat ->
                    val count = quests.allQuests.count { it.category == cat }
                    FilterChip("${cat.label} ($count)", category == cat) { category = cat }
                }
        }

        val shownActive = byCategory(active)
        val shownAvailable = byCategory(available)
        val shownCompleted = byCategory(completed)
        if (filter != QuestFilter.Completed) {
            if (shownActive.isEmpty() && shownAvailable.isEmpty()) {
                Text(
                    text = "No quests right now — talk to people and explore.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = DsType.Body
                )
            }
            shownActive.forEach { quest -> QuestCard(session, quest, QuestState.Active) }
            if (filter == QuestFilter.All && shownAvailable.isNotEmpty()) {
                SectionTitle("Available")
                shownAvailable.forEach { quest -> QuestCard(session, quest, QuestState.Available) }
            }
        }
        if (filter != QuestFilter.Active && shownCompleted.isNotEmpty()) {
            if (filter == QuestFilter.All) SectionTitle("Completed")
            shownCompleted.forEach { quest -> QuestCard(session, quest, QuestState.Complete) }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) Color(0xFF1B2233) else Color.White.copy(alpha = 0.7f),
        fontSize = DsType.Label,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(if (selected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(horizontal = DsSpacing.Sm, vertical = 5.dp)
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFFFFD54F),
        fontSize = DsType.Label,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = DsSpacing.Sm)
    )
}

@Composable
private fun QuestCard(session: GameSession, quest: Quest, state: QuestState) {
    val progress = session.quests.progressFor(quest.id)
    val dimmed = state == QuestState.Complete || state == QuestState.Available
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(Color.White.copy(alpha = if (dimmed) 0.04f else 0.09f))
            .padding(DsSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${quest.title}${if (quest.titleJp.isNotBlank()) "  ${quest.titleJp}" else ""}",
                color = if (dimmed) Color.White.copy(alpha = 0.5f) else Color.White,
                fontSize = DsType.Body,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when (state) {
                    QuestState.Active -> "ACTIVE"
                    QuestState.Available -> "AVAILABLE"
                    QuestState.Complete -> "✓"
                    QuestState.Locked -> "🔒"
                },
                color = when (state) {
                    QuestState.Active -> Color(0xFFFFD54F)
                    QuestState.Complete -> Color(0xFFA5D6A7)
                    QuestState.Available -> Color(0xFF90CAF9)
                    QuestState.Locked -> Color.White.copy(alpha = 0.3f)
                },
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium
            )
        }
        if (state == QuestState.Active && progress != null) {
            val doneCount = progress.objectives.count { it.complete }
            QuestProgressBar(doneCount, quest.objectives.size)
            quest.objectives.forEach { objective ->
                val objProgress = progress.objectives.firstOrNull { it.objectiveId == objective.id }
                val done = objProgress?.complete == true
                Text(
                    text = "${if (done) "☑" else "☐"} ${objective.description}${if (!done && objProgress != null && objProgress.target > 1) "  (${objProgress.current}/${objProgress.target})" else ""}",
                    color = if (done) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.8f),
                    fontSize = DsType.Body
                )
                if (objective.jpHint.isNotBlank() && !done) {
                    Text(
                        text = "     ${objective.jpHint}",
                        color = Color(0xFF90CAF9),
                        fontSize = DsType.Caption
                    )
                }
            }
        } else if (state == QuestState.Available) {
            Text(
                text = quest.description,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = DsType.Caption
            )
        }
    }
}

@Composable
private fun QuestProgressBar(done: Int, total: Int) {
    val fraction = if (total == 0) 0f else done.toFloat() / total.toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(DsRadius.Sm))
                .background(Color(0xFFFFD54F))
        )
    }
}
