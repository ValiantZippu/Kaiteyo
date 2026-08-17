package ua.syt0r.kanji.desktop.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsChip
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTextButton
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.dictionary.JapaneseSegmenter
import ua.syt0r.kanji.desktop.engine.dictionary.WordStatus
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.media.TranscriptFilter
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// TRANSCRIPT PANEL
// Chronological subtitle list with live search,
// word-status filters and click-to-seek. The
// active line follows playback and stays visible.
// ============================================

@Composable
fun TranscriptPanel(state: AppState, modifier: Modifier = Modifier) {
    val media = state.media
    val sc = surfaceColors()
    val cues = media.subtitles.sortedCues()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(TranscriptFilter.All) }
    var focused by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    val selection = remember { mutableStateListOf<String>() }
    var dictForm by remember { mutableStateOf(false) }

    // Follow the active cue, but never fight the user: only scroll when the
    // current line is out of view. If it is already visible (or the user
    // scrolled somewhere else with the line still on screen), playback keeps
    // going and the header offers a subtle "Jump to current" instead of
    // yanking the list around.
    val activeIndex = media.activeCueIndex
    LaunchedEffect(activeIndex, cues.size) {
        if (activeIndex >= 0) {
            val idx = activeIndex.coerceAtMost(cues.lastIndex.coerceAtLeast(0))
            val info = listState.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull()?.index ?: -1
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            if (idx < first || idx > last) {
                listState.animateScrollToItem(idx)
            }
        }
    }

    // Keep the engine's text-input flag in sync so hotkeys stand down.
    LaunchedEffect(focused) { media.textInputFocused = focused }

    Column(modifier.fillMaxSize().background(sc.surface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transcript", color = sc.textPrimary, fontSize = DsType.Label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${cues.size} lines", color = sc.textMuted, fontSize = DsType.Caption)
            if (activeIndex >= 0) {
                DsTextButton(
                    text = "Jump to current",
                    onClick = {
                        val idx = activeIndex.coerceAtMost(cues.lastIndex.coerceAtLeast(0))
                        scope.launch { listState.animateScrollToItem(idx) }
                    }
                )
            }
            Spacer(Modifier.width(DsSpacing.Sm))
            if (selecting) {
                DsButton(
                    text = "Mine ${selection.size}",
                    compact = true,
                    enabled = selection.isNotEmpty(),
                    onClick = {
                        val selectedCues = cues.filter { it.id in selection }
                        media.bulkMine(selectedCues)
                        selection.clear()
                        selecting = false
                    }
                )
                DsTextButton(
                    text = "Cancel",
                    onClick = {
                        selection.clear()
                        selecting = false
                    }
                )
            } else {
                DsTextButton(
                    text = "Export",
                    onClick = { exportTranscript(state, query) }
                )
                DsTextButton(
                    text = "Select",
                    onClick = { selecting = true }
                )
            }
        }

        DsSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search subtitles…",
            modifier = Modifier.padding(horizontal = DsSpacing.Md).onFocusChanged { focused = it.isFocused }
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
        ) {
            TranscriptFilter.entries.forEach { f ->
                DsChip(
                    text = f.name,
                    selected = filter == f,
                    onClick = { filter = f }
                )
            }
            DsChip(
                text = "Dict form",
                selected = dictForm,
                onClick = { dictForm = !dictForm }
            )
        }

        val filtered = remember(query, filter, cues, dictForm, state.cards.size) {
            val q = query.trim()
            val list = if (q.isBlank()) cues else cues.filter { cue ->
                if (dictForm) cueMatchesDictForm(cue, media, state, q)
                else media.displayTextFor(cue).contains(q, ignoreCase = true)
            }
            when (filter) {
                TranscriptFilter.All -> list
                TranscriptFilter.Unknown -> list.filter { cue -> cueHasStatus(cue, state, WordStatus.Unknown) }
                TranscriptFilter.Known -> list.filter { cue ->
                    cueHasStatus(cue, state, WordStatus.Known) || cueHasStatus(cue, state, WordStatus.Mature)
                }
                TranscriptFilter.Mined -> list.filter { cue -> cueHasStatus(cue, state, WordStatus.Mined) }
                TranscriptFilter.New -> list.filter { cue -> cueHasStatus(cue, state, WordStatus.New) }
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(DsSpacing.Xl), contentAlignment = Alignment.Center) {
                Text("No matching subtitles", color = sc.textMuted, fontSize = DsType.Body)
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(DsSpacing.Sm)) {
                itemsIndexed(filtered, key = { _, cue -> cue.id }) { index, cue ->
                    val isSelected = cue.id in selection
                    TranscriptRow(
                        state = state,
                        cue = cue,
                        active = cue.id == media.activeCue?.id,
                        selecting = selecting,
                        selected = isSelected,
                        onToggle = {
                            if (isSelected) selection.remove(cue.id) else selection.add(cue.id)
                        },
                        onClick = {
                            if (selecting) {
                                if (isSelected) selection.remove(cue.id) else selection.add(cue.id)
                            } else {
                                media.seekToCue(media.subtitles.sortedCues().indexOfFirst { it.id == cue.id })
                            }
                        },
                        onDoubleClick = {
                            if (selecting) {
                                if (isSelected) selection.remove(cue.id) else selection.add(cue.id)
                            } else {
                                val idx = media.subtitles.sortedCues().indexOfFirst { it.id == cue.id }
                                media.seekToCue(idx)
                                media.play()
                                media.replayCue()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptRow(
    state: AppState,
    cue: ua.syt0r.kanji.desktop.engine.media.SubtitleCue,
    active: Boolean,
    selecting: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val media = state.media
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        selected -> ac.primary.copy(alpha = 0.28f)
                        active -> ac.primary.copy(alpha = 0.16f)
                        else -> sc.surfaceElevated.copy(alpha = 0.6f)
                    }
                )
                .combinedClickable(
                    onClick = onClick,
                    onDoubleClick = onDoubleClick,
                    onLongClick = { if (!selecting) menuOpen = true }
                )
                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (selecting) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(
                        if (selected) "☑" else "☐",
                        color = if (selected) ac.primary else sc.textMuted,
                        fontSize = DsType.BodyLarge,
                        modifier = Modifier.clickable(onClick = onToggle)
                    )
                    Text(
                        "${MediaEngine.formatTime(cue.startMs + media.subtitles.globalOffsetMs)}",
                        color = if (selected) ac.primary else sc.textMuted,
                        fontSize = DsType.Caption,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    "${MediaEngine.formatTime(cue.startMs + media.subtitles.globalOffsetMs)}",
                    color = if (active) ac.primary else sc.textMuted,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                media.displayTextFor(cue),
                color = sc.textPrimary,
                fontSize = DsType.Body,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            fun cueIndex(): Int = media.subtitles.sortedCues().indexOfFirst { it.id == cue.id }
            DropdownMenuItem(
                text = { Text("Look up in dictionary") },
                onClick = {
                    menuOpen = false
                    media.lookupText(media.displayTextFor(cue))
                }
            )
            DropdownMenuItem(
                text = { Text("Mine sentence") },
                onClick = {
                    menuOpen = false
                    media.mineCue(cue)
                }
            )
            DropdownMenuItem(
                text = { Text("Copy subtitle") },
                onClick = {
                    menuOpen = false
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(media.displayTextFor(cue)), null)
                }
            )
            DropdownMenuItem(
                text = { Text("Replay") },
                onClick = {
                    menuOpen = false
                    media.seekToCue(cueIndex())
                    media.play()
                    media.replayCue()
                }
            )
            DropdownMenuItem(
                text = { Text("Loop subtitle") },
                onClick = {
                    menuOpen = false
                    media.seekToCue(cueIndex())
                    media.toggleLoopCue()
                }
            )
            DropdownMenuItem(
                text = { Text("Capture audio clip") },
                onClick = {
                    menuOpen = false
                    media.captureAudioClip(cue)
                }
            )
            DropdownMenuItem(
                text = { Text("Screenshot") },
                onClick = {
                    menuOpen = false
                    media.captureScreenshot()
                }
            )
        }
    }
}

/** Export the (filtered) transcript as a .txt file. */
private fun exportTranscript(state: AppState, query: String) {
    val media = state.media
    val cues = media.subtitles.sortedCues()
    val list = if (query.isBlank()) cues else cues.filter { media.displayTextFor(it).contains(query.trim(), ignoreCase = true) }
    val chooser = javax.swing.JFileChooser().apply {
        dialogTitle = "Export transcript"
        fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
        selectedFile = java.io.File("kaiteyo-transcript-${System.currentTimeMillis()}.txt")
    }
    if (chooser.showSaveDialog(null) != javax.swing.JFileChooser.APPROVE_OPTION) return
    val target = chooser.selectedFile
    val content = buildString {
        append("# Kaiteyo transcript — ").append(media.currentItem?.name ?: "media").append("\n")
        append("# ").append(list.size).append(" of ").append(cues.size).append(" lines\n\n")
        list.forEach { cue ->
            append(MediaEngine.formatTime(cue.startMs)).append("  ").append(media.displayTextFor(cue)).append("\n")
        }
    }
    runCatching { target.writeText(content) }
        .onSuccess { state.toastHost.show("Transcript exported to ${target.name}", kind = ToastKind.Success) }
        .onFailure { state.toastHost.show("Export failed: ${it.message}", kind = ToastKind.Warning) }
}

/**
 * Dictionary-form search: a cue matches when the query appears in the surface
 * text, in a token's reading, or in the dictionary headword the token resolved
 * to (handles inflected forms — searching 行く also finds 行かなかった).
 */
private fun cueMatchesDictForm(
    cue: ua.syt0r.kanji.desktop.engine.media.SubtitleCue,
    media: ua.syt0r.kanji.desktop.engine.media.MediaEngine,
    state: AppState,
    q: String
): Boolean {
    val text = media.displayTextFor(cue)
    if (text.contains(q, ignoreCase = true)) return true
    return JapaneseSegmenter.segment(text, state.dictionary.repository)
        .any { token ->
            token.dictionaryMatch?.entry?.headword?.contains(q, ignoreCase = true) == true ||
                token.reading.contains(q, ignoreCase = true)
        }
}

/** True if any Japanese token in the cue has the given status (segmentation is cached). */
private fun cueHasStatus(
    cue: ua.syt0r.kanji.desktop.engine.media.SubtitleCue,
    state: AppState,
    status: WordStatus
): Boolean {
    return JapaneseSegmenter.segment(state.media.displayTextFor(cue), state.dictionary.repository, state.cards.toList())
        .any { it.isJapanese && it.status == status }
}
