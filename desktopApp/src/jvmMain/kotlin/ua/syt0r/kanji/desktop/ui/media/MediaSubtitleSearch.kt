package ua.syt0r.kanji.desktop.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.media.SubtitleSearchHit
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// SUBTITLE SEARCH PANEL
// Library-wide subtitle search: type a Japanese
// word (or any text) and get every cue that
// contains it across ALL media — not just the
// loaded episode. Clicking a hit opens the media,
// seeks to the timestamp, loads the track and
// leaves the transcript open on the line.
//
// Indexing is incremental and background; the
// status line shows what is currently searchable.
// ============================================

@Composable
fun SubtitleSearchPanel(state: AppState) {
    val media = state.media
    val index = media.subtitleSearchIndex
    val sc = surfaceColors()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<SubtitleSearchHit>()) }
    var searchFocused by remember { mutableStateOf(false) }

    // Keep the engine's text-input flag in sync so immersion hotkeys stand down.
    LaunchedEffect(searchFocused) { media.textInputFocused = searchFocused }

    // Index on first show (incremental — only new/changed files are parsed).
    LaunchedEffect(Unit) {
        index.refreshAsync()
    }

    // Debounced search over the (in-memory) index.
    LaunchedEffect(query) {
        delay(250)
        if (query.isBlank()) {
            results = emptyList()
        } else {
            results = media.subtitleSearch(query)
        }
    }
    // Re-run the last search when indexing finishes so new tracks appear.
    LaunchedEffect(index.indexing, index.indexedFiles) {
        if (!index.indexing && query.isNotBlank()) {
            results = media.subtitleSearch(query)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search every subtitle in your library… e.g. 食べる",
                modifier = Modifier.weight(1f).onFocusChanged { searchFocused = it.isFocused }
            )
            DsButton(
                text = "Re-index",
                icon = Icons.Default.Refresh,
                kind = DsButtonKind.Ghost,
                compact = true,
                onClick = {
                    index.refreshAsync()
                    state.toastHost.show("Subtitle index refresh started", kind = ToastKind.Info)
                }
            )
        }

        // Index status — honest about what is searchable.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text(
                when {
                    index.indexing -> "Indexing subtitles…"
                    index.indexedFiles == 0 -> index.lastMessage.ifBlank { "No subtitle tracks indexed yet" }
                    else -> index.lastMessage
                },
                color = sc.textMuted,
                fontSize = DsType.Caption,
                modifier = Modifier.weight(1f)
            )
            if (index.indexing) {
                Text("…", color = accent().primary, fontSize = DsType.Body)
            } else {
                Text(
                    "kana-insensitive: たべる finds タベル",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }

        when {
            query.isBlank() -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                        Text("Search subtitles across your whole library", color = sc.textPrimary, fontSize = DsType.Body)
                        Text(
                            "Find every episode a word appears in, then jump straight to the line — with dictionary and mining ready.",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                }
            }
            index.indexedFiles == 0 && !index.indexing -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "No subtitles indexed — add media with .srt/.ass/.ssa/.vtt companions or drop a subtitle file onto media.",
                        color = sc.textMuted,
                        fontSize = DsType.Body,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                }
            }
            results.isEmpty() -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No subtitle line contains \"${query.trim()}\"", color = sc.textMuted, fontSize = DsType.Body)
                }
            }
            else -> {
                // Group hits by media — each group header + its hits, virtualized.
                val grouped = remember(results) {
                    buildList<Any> {
                        var lastMedia = ""
                        results.forEach { hit ->
                            if (hit.mediaName != lastMedia) {
                                add(hit.mediaName)
                                lastMedia = hit.mediaName
                            }
                            add(hit)
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(vertical = DsSpacing.Xs),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(grouped, key = { _, row ->
                        when (row) {
                            is String -> "media-$row"
                            is SubtitleSearchHit -> "hit-${row.mediaId}-${row.trackPath}-${row.cueIndex}"
                            else -> row.hashCode().toString()
                        }
                    }) { _, row ->
                        when (row) {
                            is String -> SubtitleSearchMediaHeader(row)
                            is SubtitleSearchHit -> SubtitleSearchResultRow(state, row, query)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleSearchMediaHeader(mediaName: String) {
    val sc = surfaceColors()
    val ac = accent()
    Text(
        mediaName,
        color = ac.primary,
        fontSize = DsType.Label,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = DsSpacing.Xs, top = DsSpacing.Sm)
    )
}

@Composable
private fun SubtitleSearchResultRow(state: AppState, hit: SubtitleSearchHit, query: String) {
    val media = state.media
    val sc = surfaceColors()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(sc.surfaceElevated.copy(alpha = 0.5f))
            .clickable { media.openSubtitleHit(hit) }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text(
                    MediaEngine.formatTime(hit.startMs),
                    color = accent().primary,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.SemiBold
                )
                if (hit.trackName != hit.mediaName && hit.trackName.isNotBlank()) {
                    Text(hit.trackName, color = sc.textMuted, fontSize = DsType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                Text("${hit.cueIndex + 1}", color = sc.textMuted, fontSize = DsType.Caption)
            }
            Text(
                highlightCue(hit.cueText, query, accent().primary),
                color = sc.textPrimary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Bold the first case-insensitive occurrence of [query] in [text] (plain when absent). */
private fun highlightCue(text: String, query: String, highlightColor: Color): AnnotatedString {
    val q = query.trim()
    if (q.isBlank()) return AnnotatedString(text)
    val idx = text.lowercase().indexOf(q.lowercase())
    if (idx < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, idx))
        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor))
        append(text.substring(idx, idx + q.length))
        pop()
        append(text.substring(idx + q.length))
    }
}
