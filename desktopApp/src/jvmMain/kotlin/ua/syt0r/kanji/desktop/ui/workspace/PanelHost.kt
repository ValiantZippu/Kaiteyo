package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.OpenPanel
import ua.syt0r.kanji.desktop.appstate.PanelKind
import ua.syt0r.kanji.desktop.appstate.PanelPlacement
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.appstate.closePanel
import ua.syt0r.kanji.desktop.appstate.movePanel
import ua.syt0r.kanji.desktop.appstate.resizePanel
import ua.syt0r.kanji.desktop.appstate.setPanelPlacement
import ua.syt0r.kanji.desktop.appstate.togglePanel
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsMenuItem
import ua.syt0r.kanji.desktop.designsystem.DsMenuPanel
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsToolbarDivider
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.ui.collections.CollectionsView
import ua.syt0r.kanji.desktop.ui.themes.ThemeStudioView
import kotlin.math.roundToInt

// ============================================
// WORKSPACE PANEL HOST
// Docked (split) and floating (draggable, resizable)
// reference panels. Lives in the content area so
// panels stay visible over any view — including a
// running review session.
// ============================================

fun panelKindIcon(kind: PanelKind): ImageVector = when (kind) {
    PanelKind.Dictionary -> Icons.Default.MenuBook
    PanelKind.DeckBrowser -> Icons.Default.Folder
    PanelKind.ThemeStudio -> Icons.Default.Palette
    PanelKind.Search -> Icons.Default.Search
    PanelKind.Media -> Icons.Default.Movie
    PanelKind.Ocr -> Icons.Default.Camera
}

// ============================================
// DOCKED PANEL COLUMN (right-hand split)
// ============================================

@Composable
fun DsDockColumn(state: AppState) {
    val docked = state.openPanels.filter { it.placement == PanelPlacement.Dock }
    if (docked.isEmpty()) return
    val sc = surfaceColors()

    Column(
        modifier = Modifier
            .width(360.dp)
            .fillMaxHeight()
            .background(sc.background)
    ) {
        docked.forEachIndexed { index, panel ->
            if (index > 0) DsToolbarDivider()
            Column(Modifier.weight(1f).fillMaxWidth()) {
                DsPanelFrame(state, panel) { PanelContent(state, panel.kind) }
            }
        }
    }
}

// ============================================
// FLOATING PANEL LAYER (draggable / resizable)
// ============================================

@Composable
fun DsFloatingPanelLayer(state: AppState) {
    val floating = state.openPanels.filter { it.placement == PanelPlacement.Floating }
    if (floating.isEmpty()) return
    Box(Modifier.fillMaxSize()) {
        floating.forEach { panel ->
            DsFloatingPanelWindow(state, panel)
        }
    }
}

@Composable
private fun DsFloatingPanelWindow(state: AppState, panel: OpenPanel) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Lg)

    Box(
        modifier = Modifier
            .offset { IntOffset(panel.x, panel.y) }
            .size(panel.width.dp, panel.height.dp)
            .shadow(DsRadius.Lg, shape)
            .clip(shape)
            .border(1.dp, sc.border.copy(alpha = 0.5f), shape)
            .background(sc.surfaceElevated)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(panel.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            state.movePanel(panel.id, (panel.x + dragAmount.x).roundToInt(), (panel.y + dragAmount.y).roundToInt())
                        }
                    }
                    .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                    .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = panel.kind.label,
                    color = sc.textPrimary,
                    fontSize = DsType.Label,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                DsIconButton(
                    icon = Icons.Default.ViewSidebar,
                    onClick = { state.setPanelPlacement(panel.id, PanelPlacement.Dock) },
                    contentDescription = "Dock panel",
                    size = 26.dp
                )
                DsIconButton(
                    icon = Icons.Default.Close,
                    onClick = { state.closePanel(panel.id) },
                    contentDescription = "Close panel",
                    size = 26.dp
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                PanelContent(state, panel.kind)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .pointerInput(panel.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                state.resizePanel(panel.id, (panel.width + dragAmount.x).roundToInt(), (panel.height + dragAmount.y).roundToInt())
                            }
                        }
                        .background(sc.surfaceInteractive)
                )
            }
        }
    }
}

// ============================================
// SHARED PANEL FRAME (used by docked panels)
// ============================================

@Composable
private fun DsPanelFrame(
    state: AppState,
    panel: OpenPanel,
    content: @Composable () -> Unit
) {
    val sc = surfaceColors()
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = panel.kind.label,
                color = sc.textPrimary,
                fontSize = DsType.Label,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            DsIconButton(
                icon = Icons.Default.Window,
                onClick = { state.setPanelPlacement(panel.id, PanelPlacement.Floating) },
                contentDescription = "Float panel",
                size = 26.dp
            )
            DsIconButton(
                icon = Icons.Default.Close,
                onClick = { state.closePanel(panel.id) },
                contentDescription = "Close panel",
                size = 26.dp
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
    }
}

// ============================================
// PANEL CONTENT DISPATCH
// ============================================

@Composable
fun PanelContent(state: AppState, kind: PanelKind) {
    when (kind) {
        PanelKind.Dictionary -> DictionaryPanel(state)
        PanelKind.DeckBrowser -> CollectionsView(state)
        PanelKind.ThemeStudio -> ThemeStudioView(state)
        PanelKind.Search -> SearchPanel(state)
        PanelKind.Media -> MediaPanel(state)
        PanelKind.Ocr -> OcrPanel(state)
    }
}

// ============================================
// MEDIA PANEL
// ============================================

@Composable
private fun MediaPanel(state: AppState) {
    val sc = surfaceColors()
    val media = state.media
    Column(Modifier.fillMaxSize().padding(DsSpacing.Md), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text("Media", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
        media.currentDocument?.let { doc ->
            Text(doc.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
            Text(
                media.subtitles.activeTrack?.let { "Subtitles: ${it.name} (${it.track.cues.size} cues)" } ?: "No subtitle track",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
            val cue = media.subtitles.activeCueAt(media.positionMs)
            if (cue != null) {
                Text(cue.text, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.fillMaxWidth().padding(top = DsSpacing.Sm))
                Text(MediaEngine.formatTime(cue.startMs), color = sc.textMuted, fontSize = DsType.Caption)
            }
        } ?: Text("No media open.", color = sc.textMuted, fontSize = DsType.Caption)
        Text("${media.bookmarks.size} bookmarks · ${media.audioClips.size} clips", color = sc.textMuted, fontSize = DsType.Caption)
    }
}

// ============================================
// OCR PANEL
// ============================================

@Composable
private fun OcrPanel(state: AppState) {
    val sc = surfaceColors()
    val ocr = state.ocr
    Column(Modifier.fillMaxSize().padding(DsSpacing.Md), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text("OCR", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
        DsButton(
            text = "Capture clipboard",
            icon = Icons.Default.Camera,
            kind = DsButtonKind.Secondary,
            compact = true,
            onClick = { runCatching { ocr.ocrClipboard() } }
        )
        val result = ocr.lastResult
        if (result != null) {
            Text(
                result.text,
                color = sc.textPrimary,
                fontSize = DsType.Body,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )
        } else {
            Text("No text captured yet.", color = sc.textMuted, fontSize = DsType.Caption)
        }
    }
}

// ============================================
// DICTIONARY PANEL
// ============================================

@Composable
private fun DictionaryPanel(state: AppState) {
    val sc = surfaceColors()
    var query by remember { mutableStateOf("") }

    val results = remember(query) {
        val q = query.trim()
        if (q.isBlank()) emptyList()
        else state.cards.filter { card ->
            card.character == q ||
                card.readings.any { it == q } ||
                card.meaning.contains(q, ignoreCase = true) ||
                card.searchableText.contains(q.lowercase())
        }.take(50)
    }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Md)) {
        DsSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "水, mizu, water…",
            autoFocus = true
        )
        Spacer(Modifier.height(DsSpacing.Sm))
        if (query.isNotBlank() && results.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No dictionary matches", color = sc.textMuted, fontSize = DsType.Body)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(results, key = { _, card -> card.id }) { _, card ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                state.selectedCard = card
                                state.browserQuery = card.character
                                state.currentView = WorkspaceView.Browser
                            }
                            .padding(vertical = DsSpacing.Sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.character,
                            color = sc.textPrimary,
                            fontSize = DsType.Heading,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(48.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = buildString {
                                    card.onReadings.forEach { append(it).append(" ") }
                                    card.kunReadings.take(2).forEach { append(it).append(" ") }
                                }.trim().ifBlank { "—" },
                                color = sc.textSecondary,
                                fontSize = DsType.Body
                            )
                            Text(
                                text = card.meaning,
                                color = sc.textMuted,
                                fontSize = DsType.Caption,
                                maxLines = 1
                            )
                        }
                        DsBadge(text = card.status.name, tint = sc.textMuted)
                    }
                }
            }
        }
    }
}

// ============================================
// SEARCH PANEL
// ============================================

@Composable
private fun SearchPanel(state: AppState) {
    val sc = surfaceColors()
    var query by remember { mutableStateOf(state.browserQuery) }
    val results = state.searchCards(query)

    Column(Modifier.fillMaxSize().padding(DsSpacing.Md)) {
        DsSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search all cards…"
        )
        Spacer(Modifier.height(DsSpacing.Sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${results.size} results", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.weight(1f))
            DsButton(
                text = "Open in browser",
                icon = Icons.Default.Search,
                kind = DsButtonKind.Secondary,
                compact = true,
                onClick = {
                    state.browserQuery = query
                    state.currentView = WorkspaceView.Browser
                }
            )
        }
        Spacer(Modifier.height(DsSpacing.Sm))
        if (results.isEmpty()) {
            DsEmptyState(
                title = "No cards match",
                message = "Try a broader search.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(results, key = { _, card -> card.id }) { _, card ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                state.selectedCard = card
                                state.currentView = WorkspaceView.Browser
                            }
                            .padding(vertical = DsSpacing.Sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.character,
                            color = sc.textPrimary,
                            fontSize = DsType.Heading,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(48.dp)
                        )
                        Text(
                            text = card.meaning,
                            color = sc.textSecondary,
                            fontSize = DsType.Body,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        DsBadge(text = card.status.name, tint = sc.textMuted)
                    }
                }
            }
        }
    }
}

// ============================================
// PANEL MENU BUTTON (top bar entry point)
// ============================================

@Composable
fun DsPanelMenuButton(state: AppState) {
    var open by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = Modifier
            .onGloballyPositioned { if (anchor != it) anchor = it }
            .padding(2.dp)
    ) {
        DsIconButton(
            icon = Icons.Default.Widgets,
            onClick = { open = true },
            contentDescription = "Panels",
            tint = accent().primary
        )
    }

    val coords = anchor
    if (open && coords != null) {
        val pos = coords.positionInWindow()
        Popup(
            onDismissRequest = { open = false },
            offset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt() + coords.size.height),
            properties = PopupProperties(focusable = true)
        ) {
            DsMenuPanel(
                menuItems = PanelKind.entries.map { kind ->
                    DsMenuItem(
                        label = kind.label,
                        icon = panelKindIcon(kind),
                        checked = state.openPanels.any { it.kind == kind },
                        onAction = { state.togglePanel(kind, PanelPlacement.Dock) }
                    )
                },
                onDismiss = { open = false }
            )
        }
    }
}
