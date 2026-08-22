@file:OptIn(ExperimentalMaterial3Api::class)

package ua.syt0r.kanji.presentation.screen.main.screen.decks

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
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog

// ════════════════════════════════════════════
// NOTES SYSTEM — Full note editor
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorFullScreen(
    cards: List<KaiteyoCard> = emptyList(),
    onSaveNote: (String, String) -> Unit = { _, _ -> },
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    var editContent by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }
    var showFormattingHelp by remember { mutableStateOf(false) }

    val cardsWithNotes = cards.filter { it.notes.isNotBlank() }
    val cardsWithoutNotes = cards.filter { it.notes.isBlank() }

    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) cards
        else cards.filter {
            it.character.contains(searchQuery) || it.meaning.contains(searchQuery) ||
            it.notes.contains(searchQuery) || it.deck.contains(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    if (selectedCardId != null) {
                        IconButton(onClick = { showPreview = !showPreview }) {
                            Icon(if (showPreview) Icons.Default.Edit else Icons.Default.Visibility, "Toggle Preview")
                        }
                        IconButton(onClick = { showFormattingHelp = true }) {
                            Icon(Icons.Default.Help, "Formatting Help")
                        }
                        TextButton(onClick = {
                            selectedCardId?.let { onSaveNote(it, editContent) }
                            selectedCardId = null
                            editContent = ""
                        }) { Text("Save") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColors.surface,
                    titleContentColor = surfaceColors.textPrimary
                )
            )
        }
    ) { padding ->
        if (selectedCardId == null) {
            // Card list with notes overview
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Stats
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${cardsWithNotes.size} with notes", fontSize = 12.sp, color = surfaceColors.textMuted)
                    Text("${cardsWithoutNotes.size} without", fontSize = 12.sp, color = surfaceColors.textMuted)
                    Text("${cards.size} total", fontSize = 12.sp, color = surfaceColors.textMuted)
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search cards...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCards, key = { it.id }) { card ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedCardId = card.id
                                editContent = card.notes
                            },
                            colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(card.character, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(card.meaning, fontSize = 13.sp, color = surfaceColors.textPrimary, maxLines = 1)
                                    Text(card.deck, fontSize = 11.sp, color = surfaceColors.textMuted)
                                }
                                if (card.notes.isNotBlank()) {
                                    Icon(Icons.Default.Description, null, Modifier.size(16.dp), tint = accent.primary)
                                } else {
                                    Icon(Icons.Default.Description, null, Modifier.size(16.dp), tint = surfaceColors.textMuted.copy(alpha = 0.4f))
                                }
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = surfaceColors.textMuted)
                            }
                        }
                    }
                }
            }
        } else {
            // Note editor
            val selectedCard = cards.find { it.id == selectedCardId }
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Card info header
                if (selectedCard != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(surfaceColors.surfaceElevated)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedCard.character, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(selectedCard.meaning, fontSize = 13.sp, color = surfaceColors.textPrimary)
                            Text(selectedCard.deck, fontSize = 11.sp, color = surfaceColors.textMuted)
                        }
                        Spacer(Modifier.weight(1f))
                        // Flag indicator
                        if (selectedCard.flag != CardFlagType.None) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(selectedCard.flag.colorFromHex()))
                        }
                    }
                }

                // Formatting toolbar
                NoteFormattingToolbar(
                    onInsertBold = { editContent += "**bold**" },
                    onInsertItalic = { editContent += "*italic*" },
                    onInsertUnderline = { editContent += "<u>underline</u>" },
                    onInsertStrikethrough = { editContent += "~~strikethrough~~" },
                    onInsertHeader = { editContent += "\n## Header\n" },
                    onInsertLink = { editContent += "[text](url)" },
                    onInsertImage = { editContent += "![alt](image.png)" },
                    onInsertTable = { editContent += "\n| Col1 | Col2 |\n|------|------|\n| Cell | Cell |\n" },
                    onInsertCode = { editContent += "\n```\ncode\n```\n" },
                    onInsertChecklist = { editContent += "\n- [ ] Task\n" },
                    onInsertList = { editContent += "\n- Item\n" },
                    onInsertNumberedList = { editContent += "\n1. Item\n" },
                    onInsertQuote = { editContent += "\n> Quote\n" },
                    onInsertDivider = { editContent += "\n---\n" },
                    surfaceColors = surfaceColors,
                    accent = accent
                )

                if (showPreview) {
                    // Markdown preview
                    NotePreview(
                        content = editContent,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        surfaceColors = surfaceColors
                    )
                } else {
                    // Editor
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                        placeholder = { Text("Write your notes here...\n\nMarkdown supported:\n- **Bold**\n- *Italic*\n- [Links](url)\n- ![Images](file.png)\n- Tables, code blocks, checklists, etc.") },
                        textStyle = TextStyle(fontSize = 14.sp, color = surfaceColors.textPrimary, lineHeight = 20.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = surfaceColors.border.copy(alpha = 0.3f),
                            cursorColor = accent.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }

    if (showFormattingHelp) {
        NoteFormattingHelpDialog(onDismiss = { showFormattingHelp = false })
    }
}

// ── Note Formatting Toolbar ──

@Composable
fun NoteFormattingToolbar(
    onInsertBold: () -> Unit,
    onInsertItalic: () -> Unit,
    onInsertUnderline: () -> Unit,
    onInsertStrikethrough: () -> Unit,
    onInsertHeader: () -> Unit,
    onInsertLink: () -> Unit,
    onInsertImage: () -> Unit,
    onInsertTable: () -> Unit,
    onInsertCode: () -> Unit,
    onInsertChecklist: () -> Unit,
    onInsertList: () -> Unit,
    onInsertNumberedList: () -> Unit,
    onInsertQuote: () -> Unit,
    onInsertDivider: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    var showMore by remember { mutableStateOf(false) }

    Column {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColors.surfaceElevated)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                FormatButton(Icons.Default.FormatBold, "Bold") { onInsertBold() }
                FormatButton(Icons.Default.FormatItalic, "Italic") { onInsertItalic() }
                FormatButton(Icons.Default.FormatUnderlined, "Underline") { onInsertUnderline() }
                FormatButton(Icons.Default.FormatStrikethrough, "Strikethrough") { onInsertStrikethrough() }
            }
            item { Spacer(Modifier.width(4.dp)) }
            item {
                FormatButton(Icons.Default.Title, "Header") { onInsertHeader() }
                FormatButton(Icons.Default.Link, "Link") { onInsertLink() }
                FormatButton(Icons.Default.Image, "Image") { onInsertImage() }
                FormatButton(Icons.Default.TableChart, "Table") { onInsertTable() }
            }
            item { Spacer(Modifier.width(4.dp)) }
            item {
                FormatButton(Icons.Default.Code, "Code") { onInsertCode() }
                FormatButton(Icons.Default.CheckBox, "Checklist") { onInsertChecklist() }
                FormatButton(Icons.Default.FormatListBulleted, "List") { onInsertList() }
                FormatButton(Icons.Default.FormatListNumbered, "Numbered") { onInsertNumberedList() }
            }
            item { Spacer(Modifier.width(4.dp)) }
            item {
                FormatButton(Icons.Default.FormatQuote, "Quote") { onInsertQuote() }
                FormatButton(Icons.Default.HorizontalRule, "Divider") { onInsertDivider() }
            }
        }
    }
}

@Composable
private fun FormatButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(icon, contentDescription, Modifier.size(18.dp), tint = surfaceColors.textPrimary)
    }
}

// ── Note Preview ──

@Composable
private fun NotePreview(
    content: String,
    modifier: Modifier = Modifier,
    surfaceColors: SurfaceColors
) {
    Column(modifier = modifier) {
        val lines = content.split("\n")
        var inCodeBlock = false
        var inTable = false

        lines.forEach { line ->
            when {
                line.startsWith("```") -> {
                    inCodeBlock = !inCodeBlock
                    if (inCodeBlock) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(surfaceColors.surfaceInteractive)
                                .padding(8.dp)
                        ) {
                            Text(line.removePrefix("```"), fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = surfaceColors.textPrimary)
                        }
                    }
                }
                inCodeBlock -> {
                    Text(line, fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = surfaceColors.textPrimary,
                        modifier = Modifier.padding(start = 8.dp))
                }
                line.startsWith("# ") -> Text(line.removePrefix("# "), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                line.startsWith("## ") -> Text(line.removePrefix("## "), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                line.startsWith("### ") -> Text(line.removePrefix("### "), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
                line.startsWith("> ") -> Text(line.removePrefix("> "), fontSize = 13.sp, color = surfaceColors.textMuted,
                    modifier = Modifier.padding(start = 8.dp).then(Modifier.fillMaxWidth().background(surfaceColors.surfaceInteractive.copy(alpha = 0.3f)).padding(8.dp)))
                line.startsWith("- [ ] ") -> Text("☐ ${line.removePrefix("- [ ] ")}", fontSize = 13.sp, color = surfaceColors.textPrimary)
                line.startsWith("- [x] ") -> Text("☑ ${line.removePrefix("- [x] ")}", fontSize = 13.sp, color = surfaceColors.textPrimary)
                line.startsWith("- ") -> Text("• ${line.removePrefix("- ")}", fontSize = 13.sp, color = surfaceColors.textPrimary)
                line.startsWith("---") -> HorizontalDivider(color = surfaceColors.border, modifier = Modifier.padding(vertical = 4.dp))
                line.startsWith("|") -> {
                    if (!inTable) { inTable = true }
                    val cells = line.split("|").filter { it.isNotBlank() }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cells.forEach { cell ->
                            Text(cell.trim(), fontSize = 12.sp, color = surfaceColors.textPrimary,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
                line.startsWith("![") -> {
                    val alt = line.substringAfter("![").substringBefore("]")
                    Text("[Image: $alt]", fontSize = 13.sp, color = surfaceColors.textMuted)
                }
                line.startsWith("[") -> {
                    val text = line.substringAfter("[").substringBefore("]")
                    val url = line.substringAfter("(").substringBefore(")")
                    Text(text, fontSize = 13.sp, color = LocalKaiteyoSemanticColors.current.info,
                        textDecoration = TextDecoration.Underline)
                }
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                else -> Text(line, fontSize = 13.sp, color = surfaceColors.textPrimary)
            }
            if (line.isBlank() && inTable) inTable = false
        }
    }
}

// ── Note Formatting Help Dialog ──

@Composable
private fun NoteFormattingHelpDialog(onDismiss: () -> Unit) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Markdown Formatting") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    Text("**Bold**", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("*Italic*", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("~~Strikethrough~~", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("[Link](url)", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("![Image](file.png)", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("Headers:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("# H1", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("## H2", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("### H3", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("Lists:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("- Unordered", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("1. Ordered", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("- [ ] Checklist", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("- [x] Done", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("Code & Tables:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("`inline code`", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("```\\ncode block\\n```", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("| Col1 | Col2 |", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("|------|------|", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("Other:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("> Quote", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("--- Divider", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}
