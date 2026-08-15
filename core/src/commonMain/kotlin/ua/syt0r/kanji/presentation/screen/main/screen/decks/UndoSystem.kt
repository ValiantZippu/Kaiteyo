package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// ============================================
// UNDO SYSTEM
// Tracks all user actions and supports undo/redo
// ============================================

/** Represents a single action that can be undone */
sealed class UndoableAction {
    abstract val description: String
    abstract val timestamp: Instant

    data class ReviewCard(
        val cardKey: String,
        val practiceType: Long,
        val previousStatus: CardStatus,
        val previousInterval: Int,
        val previousEase: Float,
        override val timestamp: Instant = Clock.System.now()
    ) : UndoableAction() {
        override val description: String get() = "Review card"
    }

    data class ChangeCardStatus(
        val cardKey: String,
        val practiceType: Long,
        val previousStatus: CardStatus,
        val newStatus: CardStatus,
        override val timestamp: Instant = Clock.System.now()
    ) : UndoableAction() {
        override val description: String get() = "Change status to ${newStatus.displayName}"
    }

    data class SetFlag(
        val cardKey: String,
        val practiceType: Long,
        val previousFlag: CardFlagType,
        val newFlag: CardFlagType,
        override val timestamp: Instant = Clock.System.now()
    ) : UndoableAction() {
        override val description: String get() = "Set flag to ${newFlag.displayName}"
    }

    data class AddTag(
        val cardKey: String,
        val practiceType: Long,
        val tagId: Long,
        val tagName: String,
        override val timestamp: Instant = Clock.System.now()
    ) : UndoableAction() {
        override val description: String get() = "Add tag $tagName"
    }

    data class RemoveTag(
        val cardKey: String,
        val practiceType: Long,
        val tagId: Long,
        val tagName: String,
        override val timestamp: Instant = Clock.System.now()
    ) : UndoableAction() {
        override val description: String get() = "Remove tag $tagName"
    }

    data class EditNote(
        val cardKey: String,
        val practiceType: Long,
        val previousContent: String,
        val newContent: String,
        override val timestamp: Instant = Clock.System.now()
    ) : UndoableAction() {
        override val description: String get() = "Edit note"
    }

    data class SuspendCard(
        val cardKey: String,
        val practiceType: Long,
        override val timestamp: Instant = Clock.System.now()
    ) : UndoableAction() {
        override val description: String get() = "Suspend card"
    }

    data class BuryCard(
        val cardKey: String,
        val practiceType: Long,
        override val timestamp: Instant = Clock.System.now()
    ) : UndoableAction() {
        override val description: String get() = "Bury card"
    }

    data class DeleteCard(
        val cardKey: String,
        val practiceType: Long,
        override val timestamp: Instant = Clock.System.now()
    ) : UndoableAction() {
        override val description: String get() = "Delete card"
    }

    data class BulkOperation(
        override val description: String,
        val affectedCards: List<Pair<String, Long>>,
        val undoAction: () -> Unit,
        override val timestamp: Instant = Clock.System.now()
    ) : UndoableAction()

    data class Custom(
        override val description: String,
        val undoAction: () -> Unit,
        val redoAction: (() -> Unit)? = null,
        override val timestamp: Instant = Clock.System.now()
    ) : UndoableAction()
}

/** Undo stack manager — maintains undo/redo history */
class UndoManager(
    private val maxHistorySize: Int = 100
) {
    private val undoStack = mutableListOf<UndoableAction>()
    private val redoStack = mutableListOf<UndoableAction>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size
    val currentHistory: List<UndoableAction> get() = undoStack.toList()

    fun push(action: UndoableAction) {
        undoStack.add(action)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo(): UndoableAction? {
        if (undoStack.isEmpty()) return null
        val action = undoStack.removeLast()
        redoStack.add(action)
        return action
    }

    fun redo(): UndoableAction? {
        if (redoStack.isEmpty()) return null
        val action = redoStack.removeLast()
        undoStack.add(action)
        return action
    }

    fun peekUndo(): UndoableAction? = undoStack.lastOrNull()
    fun peekRedo(): UndoableAction? = redoStack.lastOrNull()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun getUndoDescription(): String = peekUndo()?.description ?: "Nothing to undo"
    fun getRedoDescription(): String = peekRedo()?.description ?: "Nothing to redo"
}

// ============================================
// UNDO UI COMPONENT
// ============================================

@Composable
fun UndoSnackbar(
    undoManager: UndoManager = remember { UndoManager() },
    onUndo: ((UndoableAction) -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var showHistory by remember { mutableStateOf(false) }

    // Floating undo button
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surfaceElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Undo, null, Modifier.size(18.dp), tint = accent.primary)
            Text(undoManager.getUndoDescription(),
                color = surfaceColors.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))

            if (undoManager.canUndo) {
                TextButton(
                    onClick = {
                        val action = undoManager.undo()
                        if (action != null) onUndo?.invoke(action)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) { Text("Undo", fontWeight = FontWeight.SemiBold) }
            }

            if (undoManager.canRedo) {
                IconButton(onClick = { undoManager.redo() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Redo, "Redo", Modifier.size(16.dp))
                }
            }

            IconButton(onClick = { showHistory = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.History, "History", Modifier.size(16.dp))
            }
        }
    }

    if (showHistory) {
        UndoHistoryDialog(
            history = undoManager.currentHistory.reversed(),
            onDismiss = { showHistory = false },
            onUndoFromHistory = { action ->
                // Would need index-based undo
                showHistory = false
            }
        )
    }
}

@Composable
private fun UndoHistoryDialog(
    history: List<UndoableAction>,
    onDismiss: () -> Unit,
    onUndoFromHistory: (UndoableAction) -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Action History") },
        text = {
            if (history.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No recent actions", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.height(400.dp)) {
                    items(history.reversed()) { action ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (action) {
                                    is UndoableAction.ReviewCard -> Icons.Default.CheckCircle
                                    is UndoableAction.ChangeCardStatus -> Icons.Default.Circle
                                    is UndoableAction.SetFlag -> Icons.Default.Flag
                                    is UndoableAction.AddTag -> Icons.Default.Label
                                    is UndoableAction.RemoveTag -> Icons.Default.Label
                                    is UndoableAction.EditNote -> Icons.Default.Edit
                                    is UndoableAction.SuspendCard -> Icons.Default.Block
                                    is UndoableAction.BuryCard -> Icons.Default.VisibilityOff
                                    is UndoableAction.DeleteCard -> Icons.Default.Delete
                                    is UndoableAction.BulkOperation -> Icons.Default.Build
                                    is UndoableAction.Custom -> Icons.Default.Info
                                    else -> Icons.Default.Info
                                },
                                null, Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(action.description, style = MaterialTheme.typography.bodySmall)
                                Text(action.timestamp.toString().take(19).replace("T", " "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
