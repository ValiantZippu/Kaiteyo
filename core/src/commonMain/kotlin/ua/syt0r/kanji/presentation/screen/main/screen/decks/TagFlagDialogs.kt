@file:OptIn(ExperimentalMaterial3Api::class)

package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog

// ════════════════════════════════════════════
// SHARED COMPONENTS
// ════════════════════════════════════════════

@Composable
fun StatCard2(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = color ?: accent.primary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                Text(label, fontSize = 10.sp, color = surfaceColors.textMuted)
            }
        }
    }
}

// ════════════════════════════════════════════
// FLAG SELECTOR DIALOG (reusable)
// ════════════════════════════════════════════

@Composable
fun TagFlagSelectorDialog(
    currentFlag: CardFlagType,
    onSelect: (CardFlagType) -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Select Flag", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CardFlagType.entries.forEach { flag ->
                val color = flag.colorFromHex()
                val isSelected = currentFlag == flag
                IconButton(
                    onClick = { onSelect(flag) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        modifier = Modifier.size(24.dp)
                            .clip(CircleShape)
                            .background(if (flag == CardFlagType.None) Color.Transparent else color)
                            .then(
                                if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .then(
                                if (flag == CardFlagType.None) Modifier.border(1.dp, Color.Gray, CircleShape)
                                else Modifier
                            )
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════
// CARD STATUS SELECTOR DIALOG (reusable)
// ════════════════════════════════════════════

@Composable
fun TagCardStatusSelectorDialog(
    currentStatus: CardStatus,
    onSelect: (CardStatus) -> Unit,
    onDismiss: () -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Card Status") },
        text = {
            Column {
                CardStatus.entries.forEach { status ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onSelect(status) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentStatus == status,
                            onClick = { onSelect(status) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(status.displayName, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ════════════════════════════════════════════
// NOTE EDITOR DIALOG (reusable inline)
// ════════════════════════════════════════════

@Composable
fun TagNoteEditorDialog(
    initialContent: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Note") },
        text = {
            Column {
                NoteFormattingToolbar(
                    onInsertBold = { content += "**bold**" },
                    onInsertItalic = { content += "*italic*" },
                    onInsertUnderline = { content += "<u>underline</u>" },
                    onInsertStrikethrough = { content += "~~strikethrough~~" },
                    onInsertHeader = { content += "\n## Header\n" },
                    onInsertLink = { content += "[text](url)" },
                    onInsertImage = { content += "![alt](image.png)" },
                    onInsertTable = { content += "\n| Col1 | Col2 |\n|------|------|\n| Cell | Cell |\n" },
                    onInsertCode = { content += "\n```\ncode\n```\n" },
                    onInsertChecklist = { content += "\n- [ ] Task\n" },
                    onInsertList = { content += "\n- Item\n" },
                    onInsertNumberedList = { content += "\n1. Item\n" },
                    onInsertQuote = { content += "\n> Quote\n" },
                    onInsertDivider = { content += "\n---\n" },
                    surfaceColors = LocalSurfaceColors.current,
                    accent = LocalKaiteyoAccent.current
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    placeholder = { Text("Write your note...") },
                    textStyle = TextStyle(fontSize = 14.sp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(content) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
