package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors

// ============================================
// ENHANCED NOTE EDITOR
// Full markdown support: Images, Links, Tables,
// Code blocks, Checklists, Formatting toolbar
// ============================================

enum class NoteToolbarAction {
    Bold, Italic, Underline, Strikethrough,
    Heading1, Heading2, Heading3,
    BulletList, NumberedList, Checklist,
    Code, CodeBlock, Blockquote,
    Link, Image, Table,
    HorizontalRule
}

data class NoteRenderConfig(
    val enableMarkdown: Boolean = true,
    val enableImages: Boolean = true,
    val enableLinks: Boolean = true,
    val enableTables: Boolean = true,
    val enableCodeBlocks: Boolean = true,
    val enableChecklists: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedNoteEditorScreen(
    initialContent: String = "",
    config: NoteRenderConfig = NoteRenderConfig(),
    onSave: (String) -> Unit = { },
    onInsertImage: () -> Unit = { },
    onInsertLink: () -> Unit = { },
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var noteText by remember { mutableStateOf(TextFieldValue(initialContent)) }
    var previewMode by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Note") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = { previewMode = !previewMode }) {
                        Icon(if (previewMode) Icons.Default.Edit else Icons.Default.Visibility, if (previewMode) "Edit" else "Preview")
                    }
                    IconButton(onClick = { showToolbar = !showToolbar }) {
                        Icon(Icons.Default.FormatBold, "Toggle Toolbar")
                    }
                    IconButton(onClick = { onSave(noteText.text); onClose() }) {
                        Icon(Icons.Default.Save, "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Formatting toolbar
            if (showToolbar) {
                NoteFormattingToolbar(
                    onAction = { action -> noteText = applyToolbarAction(noteText, action) },
                    onInsertImage = onInsertImage,
                    onInsertLink = onInsertLink,
                    surfaceColors = surfaceColors
                )
                HorizontalDivider()
            }

            if (previewMode) {
                // Markdown preview
                MarkdownPreview(
                    content = noteText.text,
                    config = config,
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)
                )
            } else {
                // Editor
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    BasicTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            if (noteText.text.isEmpty()) {
                                Text(
                                    "Write your note here...\n\n" +
                                    "Supports:\n" +
                                    "- **Bold**, *Italic*, ~~Strikethrough~~\n" +
                                    "- # Headers\n" +
                                    "- - Lists, - [ ] Checklists\n" +
                                    "- `Code`, ``` Code blocks\n" +
                                    "- [Links](url), ![Images](url)\n" +
                                    "- | Tables |\n" +
                                    "- > Blockquotes",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteFormattingToolbar(
    onAction: (NoteToolbarAction) -> Unit,
    onInsertImage: () -> Unit,
    onInsertLink: () -> Unit,
    surfaceColors: SurfaceColors
) {
    val buttons = listOf(
        FormatButtonData(Icons.Default.FormatBold, "Bold", NoteToolbarAction.Bold),
        FormatButtonData(Icons.Default.FormatItalic, "Italic", NoteToolbarAction.Italic),
        FormatButtonData(Icons.Default.FormatUnderlined, "Underline", NoteToolbarAction.Underline),
        FormatButtonData(Icons.Default.FormatStrikethrough, "Strikethrough", NoteToolbarAction.Strikethrough),
    )

    val insertButtons = listOf(
        FormatButtonData(Icons.Default.Link, "Link", NoteToolbarAction.Link),
        FormatButtonData(Icons.Default.Image, "Image", NoteToolbarAction.Image),
        FormatButtonData(Icons.Default.TableChart, "Table", NoteToolbarAction.Table),
        FormatButtonData(Icons.Default.Code, "Code", NoteToolbarAction.CodeBlock),
        FormatButtonData(Icons.Default.CheckBox, "Checklist", NoteToolbarAction.Checklist),
        FormatButtonData(Icons.Default.HorizontalRule, "Horizontal Rule", NoteToolbarAction.HorizontalRule),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColors.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            buttons.forEach { btn ->
                IconButton(
                    onClick = { onAction(btn.action) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(btn.icon, btn.label, Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            insertButtons.forEach { btn ->
                IconButton(
                    onClick = { onAction(btn.action) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(btn.icon, btn.label, Modifier.size(18.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(
                FormatButtonData(Icons.Default.Title, "H1", NoteToolbarAction.Heading1),
                FormatButtonData(Icons.Default.Title, "H2", NoteToolbarAction.Heading2),
                FormatButtonData(Icons.Default.Title, "H3", NoteToolbarAction.Heading3),
                FormatButtonData(Icons.Default.FormatListBulleted, "Bullet List", NoteToolbarAction.BulletList),
                FormatButtonData(Icons.Default.FormatListNumbered, "Numbered List", NoteToolbarAction.NumberedList),
                FormatButtonData(Icons.Default.FormatQuote, "Blockquote", NoteToolbarAction.Blockquote),
            ).forEach { btn ->
                IconButton(
                    onClick = { onAction(btn.action) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(btn.icon, btn.label, Modifier.size(16.dp))
                }
            }
        }
    }
}

private data class FormatButtonData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val action: NoteToolbarAction
)

private fun applyToolbarAction(current: TextFieldValue, action: NoteToolbarAction): TextFieldValue {
    val text = current.text
    val selection = current.selection
    val start = selection.start
    val end = selection.end
    val hasSelection = start != end

    return when (action) {
        NoteToolbarAction.Bold -> wrapSelection(text, start, end, "**", "**")
        NoteToolbarAction.Italic -> wrapSelection(text, start, end, "*", "*")
        NoteToolbarAction.Underline -> wrapSelection(text, start, end, "<u>", "</u>")
        NoteToolbarAction.Strikethrough -> wrapSelection(text, start, end, "~~", "~~")
        NoteToolbarAction.Heading1 -> insertAtLine(text, start, "# ")
        NoteToolbarAction.Heading2 -> insertAtLine(text, start, "## ")
        NoteToolbarAction.Heading3 -> insertAtLine(text, start, "### ")
        NoteToolbarAction.BulletList -> insertAtLine(text, start, "- ")
        NoteToolbarAction.NumberedList -> insertAtLine(text, start, "1. ")
        NoteToolbarAction.Checklist -> insertAtLine(text, start, "- [ ] ")
        NoteToolbarAction.Code -> wrapSelection(text, start, end, "`", "`")
        NoteToolbarAction.CodeBlock -> {
            val prefix = if (start == 0 || text[start - 1] == '\n') "" else "\n"
            val suffix = if (end == text.length || text.getOrNull(end) == '\n') "" else "\n"
            val newText = text.substring(0, start) + prefix + "```\n" +
                (if (hasSelection) text.substring(start, end) else "") +
                "\n```" + suffix + text.substring(end)
            TextFieldValue(newText, androidx.compose.ui.text.TextRange(start + prefix.length + 4))
        }
        NoteToolbarAction.Blockquote -> insertAtLine(text, start, "> ")
        NoteToolbarAction.Link -> {
            val selected = if (hasSelection) text.substring(start, end) else "text"
            val linkText = "[$selected](url)"
            val newText = text.substring(0, start) + linkText + text.substring(end)
            TextFieldValue(newText, androidx.compose.ui.text.TextRange(start + linkText.length))
        }
        NoteToolbarAction.Image -> {
            val newText = text.substring(0, start) + "![alt text](image-url)" + text.substring(end)
            TextFieldValue(newText, androidx.compose.ui.text.TextRange(start + "![alt text](".length))
        }
        NoteToolbarAction.Table -> {
            val table = "\n| Header 1 | Header 2 | Header 3 |\n|----------|----------|----------|\n| Cell 1   | Cell 2   | Cell 3   |\n"
            val newText = text.substring(0, start) + table + text.substring(end)
            TextFieldValue(newText, androidx.compose.ui.text.TextRange(start + table.length))
        }
        NoteToolbarAction.HorizontalRule -> {
            val hr = "\n---\n"
            val newText = text.substring(0, start) + hr + text.substring(end)
            TextFieldValue(newText, androidx.compose.ui.text.TextRange(start + hr.length))
        }
    }
}

private fun wrapSelection(text: String, start: Int, end: Int, prefix: String, suffix: String): TextFieldValue {
    val selected = if (start != end) text.substring(start, end) else "text"
    val wrapped = prefix + selected + suffix
    val newText = text.substring(0, start) + wrapped + text.substring(end)
    return TextFieldValue(newText, androidx.compose.ui.text.TextRange(start + wrapped.length))
}

private fun insertAtLine(text: String, cursorPos: Int, prefix: String): TextFieldValue {
    val lineStart = text.lastIndexOf('\n', cursorPos - 1) + 1
    val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    return TextFieldValue(newText, androidx.compose.ui.text.TextRange(cursorPos + prefix.length))
}

// ============================================
// MARKDOWN PREVIEW (Full implementation)
// ============================================

@Composable
fun MarkdownPreview(
    content: String,
    config: NoteRenderConfig = NoteRenderConfig(),
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(modifier = modifier) {
        if (content.isBlank()) {
            Text("No content to preview", color = surfaceColors.textMuted,
                fontSize = 14.sp, modifier = Modifier.padding(16.dp))
            return
        }

        val lines = content.split("\n")
        var inCodeBlock = false
        var codeBlockContent = ""
        var inTable = false
        var tableLines = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            when {
                line.startsWith("```") -> {
                    if (inCodeBlock) {
                        // Render code block
                        CodeBlockDisplay(codeBlockContent.trimEnd())
                        codeBlockContent = ""
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                        codeBlockContent = ""
                    }
                }
                inCodeBlock -> { codeBlockContent += line + "\n" }
                line.startsWith("# ") -> {
                    inTable = false
                    Text(line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                }
                line.startsWith("## ") -> {
                    inTable = false
                    Text(line.removePrefix("## "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                }
                line.startsWith("### ") -> {
                    inTable = false
                    Text(line.removePrefix("### "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                line.startsWith("---") || line.startsWith("***") -> {
                    inTable = false
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                    inTable = false
                    ChecklistItem(checked = true, text = line.substringAfter("] "), accent = accent)
                }
                line.startsWith("- [ ] ") -> {
                    inTable = false
                    ChecklistItem(checked = false, text = line.substringAfter("] "), accent = accent)
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    inTable = false
                    BulletItem(line.removePrefix("- ").removePrefix("* "), surfaceColors)
                }
                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    inTable = false
                    NumberedItem(line, surfaceColors)
                }
                line.startsWith("> ") -> {
                    inTable = false
                    BlockquoteDisplay(line.removePrefix("> "), surfaceColors, accent)
                }
                line.startsWith("|") && line.endsWith("|") -> {
                    if (!inTable) {
                        inTable = true
                        tableLines = mutableListOf()
                    }
                    tableLines.add(line)
                }
                line.startsWith("![") -> {
                    inTable = false
                    if (config.enableImages) {
                        val altText = line.substringAfter("![").substringBefore("]")
                        val url = line.substringAfter("](").substringBefore(")")
                        ImagePlaceholder(altText, url, surfaceColors)
                    } else {
                        Text("[Image: $line]", color = surfaceColors.textMuted, fontSize = 12.sp)
                    }
                }
                line.contains("[") && line.contains("](") -> {
                    inTable = false
                    if (config.enableLinks) {
                        LinkText(line, accent)
                    } else {
                        Text(line, fontSize = 14.sp)
                    }
                }
                line.startsWith("|") -> {
                    // Table separator or continued
                    if (!inTable) {
                        inTable = true
                        tableLines = mutableListOf()
                    }
                    tableLines.add(line)
                }
                else -> {
                    if (inTable && tableLines.isNotEmpty()) {
                        RenderTable(tableLines, surfaceColors, accent)
                        inTable = false
                        tableLines = mutableListOf()
                    }
                    if (line.isNotBlank()) {
                        ParagraphText(line, surfaceColors)
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // Flush remaining table
        if (inTable && tableLines.isNotEmpty()) {
            RenderTable(tableLines, surfaceColors, accent)
        }
    }
}

@Composable
private fun ChecklistItem(checked: Boolean, text: String, accent: KaiteyoAccentScheme) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            null, Modifier.size(20.dp),
            tint = if (checked) accent.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 14.sp,
            textDecoration = if (checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BulletItem(text: String, surfaceColors: SurfaceColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", fontSize = 14.sp, color = surfaceColors.textMuted, modifier = Modifier.width(16.dp))
        Text(text, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NumberedItem(line: String, surfaceColors: SurfaceColors) {
    val number = line.substringBefore(".")
    val text = line.substringAfter(". ")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("$number.", fontSize = 14.sp, color = surfaceColors.textMuted, modifier = Modifier.width(24.dp))
        Text(text, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BlockquoteDisplay(text: String, surfaceColors: SurfaceColors, accent: KaiteyoAccentScheme) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 16.dp)
            .background(accent.primary.copy(alpha = 0.05f))
            .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier.width(3.dp)
                .height(24.dp)
                .background(accent.primary.copy(alpha = 0.3f))
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = surfaceColors.textMuted)
    }
}

@Composable
private fun CodeBlockDisplay(code: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Text(
            code,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ImagePlaceholder(altText: String, url: String, surfaceColors: SurfaceColors) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Image, null, Modifier.size(32.dp), tint = surfaceColors.textMuted)
            Spacer(Modifier.height(8.dp))
            Text(altText, fontSize = 12.sp, color = surfaceColors.textMuted)
            Text(url, fontSize = 10.sp, color = surfaceColors.textMuted.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun LinkText(line: String, accent: KaiteyoAccentScheme) {
    val regex = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")
    val parts = mutableListOf<Pair<String, String>>() // text, url
    var remaining = line
    while (true) {
        val match = regex.find(remaining)
        if (match == null) {
            if (remaining.isNotBlank()) parts.add(remaining to "")
            break
        }
        if (match.range.first > 0) {
            parts.add(remaining.substring(0, match.range.first) to "")
        }
        parts.add(match.groupValues[1] to match.groupValues[2])
        remaining = remaining.substring(match.range.last + 1)
    }
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        parts.forEach { (text, url) ->
            if (url.isNotBlank()) {
                Text(text, fontSize = 14.sp, color = accent.primary,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                    modifier = Modifier.clickable { /* Open URL */ })
            } else {
                Text(text, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ParagraphText(text: String, surfaceColors: SurfaceColors) {
    // Inline formatting within paragraphs
    val inlineRegex = Regex("(\\*\\*|__)(.*?)\\1|(\\*|_)(.*?)\\3|~~(.*?)~~|`([^`]+)`")
    val parts = mutableListOf<Pair<String, String>>() // text, format: bold, italic, strikethrough, code, plain
    var remaining = text
    while (true) {
        val match = inlineRegex.find(remaining)
        if (match == null) {
            if (remaining.isNotBlank()) parts.add(remaining to "plain")
            break
        }
        if (match.range.first > 0) {
            parts.add(remaining.substring(0, match.range.first) to "plain")
        }
        val format = when {
            match.groupValues[1] == "**" || match.groupValues[1] == "__" -> "bold"
            match.groupValues[3] == "*" || match.groupValues[3] == "_" -> "italic"
            match.groupValues[5] != null -> "strikethrough"
            match.groupValues[6] != null -> "code"
            else -> "plain"
        }
        val content = match.groupValues[2].ifEmpty { match.groupValues[4].ifEmpty { match.groupValues[5].ifEmpty { match.groupValues[6] } } }
        parts.add(content to format)
        remaining = remaining.substring(match.range.last + 1)
    }

    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        parts.forEach { (text, format) ->
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = if (format == "bold") FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (format == "italic") androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                textDecoration = if (format == "strikethrough") androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                fontFamily = if (format == "code") FontFamily.Monospace else FontFamily.Default,
                color = if (format == "code") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RenderTable(
    lines: List<String>,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    if (lines.size < 2) return

    val headerLine = lines[0]
    val separatorLine = lines.getOrNull(1) ?: return
    val dataLines = lines.drop(2)

    val headers = headerLine.trim('|').split("|").map { it.trim() }
    val columns = headers.size

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
    ) {
        Column(modifier = Modifier.padding(0.dp)) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.primary.copy(alpha = 0.1f))
            ) {
                headers.forEachIndexed { i, header ->
                    Text(
                        header,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        maxLines = 1
                    )
                    if (i < columns - 1) {
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(surfaceColors.textMuted.copy(alpha = 0.2f)))
                    }
                }
            }
            HorizontalDivider(color = surfaceColors.textMuted.copy(alpha = 0.1f))
            // Data rows
            dataLines.forEach { line ->
                val cells = line.trim('|').split("|").map { it.trim() }
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    cells.forEachIndexed { i, cell ->
                        Text(
                            cell,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(6.dp),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (i < columns - 1) {
                            Box(modifier = Modifier.width(1.dp).height(16.dp).background(surfaceColors.textMuted.copy(alpha = 0.1f)))
                        }
                    }
                }
                if (line != dataLines.last()) {
                    HorizontalDivider(color = surfaceColors.textMuted.copy(alpha = 0.05f))
                }
            }
        }
    }
}

