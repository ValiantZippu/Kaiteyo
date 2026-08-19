package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog
import ua.syt0r.kanji.presentation.screen.main.screen.info.use_case.DeckMembership
import ua.syt0r.kanji.presentation.screen.main.screen.info.use_case.ItemLearningState
import ua.syt0r.kanji.presentation.screen.main.screen.info.use_case.WritingLearningStats
import kotlin.math.roundToInt

// ============================================
// LEARNING STATUS SECTION — kanji / vocab detail
// Shows real SRS state, review history, writing
// accuracy, deck membership, tags and note, with
// working study actions (practice, add to deck,
// suspend, note). All actions mutate persisted
// state through the callbacks provided by the
// screen.
// ============================================

data class LearningAction(
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun LearningStatusSection(
    state: ItemLearningState,
    actions: List<LearningAction>
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "STUDY STATUS",
            style = MaterialTheme.typography.labelMedium,
            color = colors.primary
        )

        if (!state.hasProgress) {
            Text(
                text = "Not studied yet — practice this item to start tracking it.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        } else {
            state.practiceTypes.forEach { practice ->
                val due = practice.nextDue
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(
                        text = practice.status,
                        color = when (practice.status) {
                            "New" -> colors.tertiary
                            "Review" -> colors.error
                            else -> colors.primary
                        }
                    )
                    Text(
                        text = practice.practiceTypeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = buildString {
                            append("${practice.reviews} reviews")
                            if (practice.intervalDays > 0) append(" · ${practice.intervalDays}d")
                            if (due != null) append(" · next ${formatDue(due)}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            state.writing?.let { writing ->
                WritingAccuracyRow(writing)
            }
        }

        if (state.decks.any { it.contains }) {
            LabeledChips(
                label = "Decks",
                values = state.decks.filter { it.contains }.map { it.title },
                colors = colors
            )
        }

        if (state.tags.isNotEmpty()) {
            LabeledChips(
                label = "Tags",
                values = state.tags,
                colors = colors
            )
        }

        if (state.note.isNotBlank()) {
            Text(
                text = "Note: ${state.note}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        if (actions.isNotEmpty()) {
            // Kaiteyo-style action buttons: elevated surface + accent text
            // instead of full neon-green Material buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                actions.take(3).forEach { action ->
                    StudyActionButton(
                        label = action.label,
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (actions.size > 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    actions.drop(3).forEach { action ->
                        StudyActionButton(
                            label = action.label,
                            onClick = action.onClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (hovered) accent.primary.copy(alpha = 0.14f)
                else surfaceColors.surfaceInteractive.copy(alpha = 0.5f)
            )
            .border(
                1.dp,
                if (hovered) accent.primary.copy(alpha = 0.4f)
                else surfaceColors.surfaceInteractive.copy(alpha = 0.7f),
                RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .hoverable(interactionSource)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (hovered) accent.primary else surfaceColors.textSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun WritingAccuracyRow(writing: WritingLearningStats) {
    val colors = MaterialTheme.colorScheme
    val percent = (writing.accuracy * 100).roundToInt()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Writing",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$percent% accuracy · ${writing.attempts} attempts",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun LabeledChips(
    label: String,
    values: List<String>,
    colors: androidx.compose.material3.ColorScheme
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.width(56.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            values.forEach { value ->
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun formatDue(instant: Instant): String {
    val now = Clock.System.now()
    val days = (instant - now).inWholeDays
    return when {
        days <= 0 -> "today"
        days == 1L -> "tomorrow"
        days < 30 -> "in ${days}d"
        else -> instant.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }
}

// ============================================
// DIALOGS
// ============================================

@Composable
fun DeckMembershipDialog(
    decks: List<DeckMembership>,
    onToggle: (DeckMembership, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to deck") },
        text = {
            if (decks.isEmpty()) {
                Text(
                    "No decks yet. Create a kanji or vocabulary deck from the Library to add items to it."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(decks) { deck ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val hovered by interactionSource.collectIsHoveredAsState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (hovered) MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.surface
                                )
                                .hoverable(interactionSource)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) { onToggle(deck, !deck.contains) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = deck.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (deck.contains) "Remove" else "Add",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (deck.contains) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun NoteDialog(
    initialNote: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf(initialNote) }
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Notes are stored per card and persist.") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(note)
                    onDismiss()
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
