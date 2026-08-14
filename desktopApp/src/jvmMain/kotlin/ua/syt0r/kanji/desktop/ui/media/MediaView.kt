package ua.syt0r.kanji.desktop.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import kotlinx.coroutines.delay
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.awt.datatransfer.DataFlavor
import java.io.File
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsPromptDialog
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTabRow
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.media.AnnotationMode
import ua.syt0r.kanji.desktop.engine.media.AutoPauseMode

// ============================================
// KAITEYO MEDIA WORKSPACE
// The immersion environment: a real player
// (VLC/mpv/Java Sound), subtitle engine, Japanese
// dictionary and mining pipeline in one screen.
//
//   MEDIA → SUBTITLES → TEXT → DICTIONARY
//        → UNDERSTANDING → MINING → CARD → SRS
// ============================================

enum class MediaPanel(val label: String) {
    Player("Player"), Library("Library"), Stats("Stats"), Bookmarks("Bookmarks"), Settings("Settings")
}

@Composable
fun MediaView(state: AppState) {
    val media = state.media
    var panel by remember { mutableStateOf(MediaPanel.Player) }
    // The ComposeWindow hosting this workspace (best effort; null in previews).
    // Found via the AWT window list because Compose's LocalWindow is not public
    // API in this version and LocalView no longer exists on desktop.
    val window = remember {
        java.awt.Window.getWindows().firstOrNull { it is ComposeWindow && it.isShowing } as? ComposeWindow
    }

    // Drag & drop: dropping a video/audio file opens it, a subtitle file
    // attaches to the current media, and a folder gets scanned into the
    // library. Active only while the Media workspace is composed.
    DisposableEffect(window) {
        val host = window ?: return@DisposableEffect onDispose {}
        val dropTarget = DropTarget(
            host.contentPane,
            object : DropTargetListener {
                override fun dragEnter(e: DropTargetDragEvent) = e.acceptDrag(DnDConstants.ACTION_COPY)
                override fun dragOver(e: DropTargetDragEvent) = e.acceptDrag(DnDConstants.ACTION_COPY)
                override fun dropActionChanged(e: DropTargetDragEvent) = Unit
                override fun dragExit(e: DropTargetEvent) = Unit
                override fun drop(e: DropTargetDropEvent) {
                    e.acceptDrop(DnDConstants.ACTION_COPY)
                    val files = runCatching {
                        e.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                    }.getOrNull()
                    e.dropComplete(true)
                    if (files != null) handleMediaDrop(state, files.filterIsInstance<File>())
                }
            }
        )
        onDispose { host.contentPane.dropTarget = null }
    }

    // The 10 Hz reconciliation loop: position, active subtitle, condensed
    // playback, auto-pause, looping and periodic watch-progress saves.
    LaunchedEffect(Unit) {
        media.speed = state.settings.getFloat("media.default-speed", 1f).coerceIn(0.25f, 2f)
        media.seekAmountMs = state.settings.getInt("media.seek-amount-ms", 5000).toLong().coerceAtLeast(1000)
        media.autoPauseMode = autoPauseFromSettings(state.settings.getString("media.auto-pause", "off"))
        media.condensedPlayback = state.settings.getBool("media.condensed-playback")
        media.condensedFastForward = state.settings.getBool("media.condensed-fast-forward")
        media.annotationMode = annotationFromSettings(state.settings.getString("media.subtitle-annotation", "status"))
        media.studyMode = state.settings.getBool("media.study-mode-default")
        media.subtitles.showSecondary = state.settings.getBool("media.dual-subtitles")
        media.miniPlayerEnabled = state.settings.getBool("media.mini-player")
        media.resumePromptEnabled = state.settings.getBool("media.resume-prompt")
        while (true) {
            media.tick()
            delay(100)
        }
    }

    // Apply fullscreen intent to the host window (best effort per platform).
    LaunchedEffect(media.fullscreenActive) {
        runCatching {
            window?.placement = if (media.fullscreenActive) WindowPlacement.Fullscreen else WindowPlacement.Floating
        }
    }

    // Cinema mode removes the chrome and pins the player.
    LaunchedEffect(media.cinemaMode) {
        if (media.cinemaMode) panel = MediaPanel.Player
    }

    // Engine flags can request a tab switch (e.g. "Back to library" from the
    // end-of-episode dialog) — honor them here since the panel is local state.
    LaunchedEffect(media.libraryOpen) {
        if (media.libraryOpen) {
            panel = MediaPanel.Library
            media.libraryOpen = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (!media.cinemaMode) {
            MediaToolbar(state, panel, onSelectPanel = { panel = it })
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (panel) {
                MediaPanel.Player -> MediaPlayerWorkspace(state)
                MediaPanel.Library -> MediaLibraryPanel(state)
                MediaPanel.Stats -> MediaStatsPanel(state)
                MediaPanel.Bookmarks -> MediaBookmarksPanel(state)
                MediaPanel.Settings -> MediaSettingsPanel(state)
            }
        }
    }
}

@Composable
private fun MediaToolbar(state: AppState, panel: MediaPanel, onSelectPanel: (MediaPanel) -> Unit) {
    val sc = surfaceColors()
    val media = state.media
    val item = media.currentItem

    var urlPromptOpen by remember { mutableStateOf(false) }
    var urlDraft by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Column(Modifier.weight(1f)) {
                Text("Media", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.SemiBold)
                Text(
                    text = when {
                        media.backendKind.name == "None" && item == null -> "Immersion workspace — open a video, episode or audio file"
                        item != null -> "${item.name} · ${media.backendKind.name} · ${MediaEngineFormat.time(media.positionMs)} / ${MediaEngineFormat.time(media.durationMs)}"
                        else -> "No media loaded"
                    },
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            // Quick actions — always available.
            DsButton(
                text = "Open file",
                icon = Icons.Default.PlayArrow,
                compact = true,
                onClick = { chooseMediaFile(state) }
            )
            DsButton(
                text = "Open folder",
                icon = Icons.Default.FolderOpen,
                kind = DsButtonKind.Secondary,
                compact = true,
                onClick = { chooseMediaFolder(state) }
            )
            DsButton(
                text = "Open URL",
                icon = Icons.Default.Language,
                kind = DsButtonKind.Secondary,
                compact = true,
                onClick = { urlPromptOpen = true }
            )
            // In-player library search — types land straight in the library panel.
            DsSearchField(
                value = media.librarySearchQuery,
                onValueChange = {
                    media.librarySearchQuery = it
                    onSelectPanel(MediaPanel.Library)
                },
                placeholder = "Search library…",
                modifier = Modifier.width(190.dp).onFocusChanged { state.media.textInputFocused = it.isFocused }
            )
            DsButton(
                text = if (media.cinemaMode) "Exit cinema" else "Cinema",
                icon = Icons.Default.Fullscreen,
                kind = if (media.cinemaMode) DsButtonKind.Primary else DsButtonKind.Ghost,
                compact = true,
                onClick = { media.toggleCinemaMode() }
            )
            DsIconButton(
                icon = Icons.Default.PhotoCamera,
                onClick = { media.captureScreenshot() },
                contentDescription = "Screenshot",
                size = 34.dp
            )
            DsIconButton(
                icon = Icons.Default.Subtitles,
                onClick = {
                    val subFile = chooseSubtitleFile()
                    if (subFile != null) media.openSubtitleFile(subFile)
                },
                contentDescription = "Load subtitles",
                size = 34.dp
            )
            DsIconButton(
                icon = Icons.Default.Add,
                onClick = {
                    val subFile = chooseSubtitleFile()
                    if (subFile != null) media.openSecondarySubtitleFile(subFile)
                },
                contentDescription = "Load secondary subtitles (dual-language)",
                size = 34.dp
            )
            DsIconButton(
                icon = Icons.Default.BarChart,
                onClick = { onSelectPanel(MediaPanel.Stats) },
                contentDescription = "Media study stats",
                size = 34.dp
            )
            DsIconButton(
                icon = Icons.Default.Bookmarks,
                onClick = { onSelectPanel(MediaPanel.Bookmarks) },
                contentDescription = "Media bookmarks",
                size = 34.dp
            )
            DsIconButton(
                icon = Icons.Default.Settings,
                onClick = { onSelectPanel(MediaPanel.Settings) },
                contentDescription = "Media settings",
                size = 34.dp
            )
        }
        DsTabRow(
            tabs = MediaPanel.entries.map { it.label },
            selectedIndex = MediaPanel.entries.indexOf(panel),
            onSelect = { onSelectPanel(MediaPanel.entries[it]) },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (urlPromptOpen) {
        DsPromptDialog(
            title = "Open network media",
            placeholder = "https://… (video or audio URL)",
            initialValue = urlDraft,
            onConfirm = { raw ->
                urlDraft = raw
                urlPromptOpen = false
                media.openUrl(raw)
            },
            onDismiss = { urlPromptOpen = false }
        )
    }
}

// ============================================
// Drag & drop handling
// ============================================

/** Route dropped files: video/audio → open, subtitle → attach, folder → scan. */
private fun handleMediaDrop(state: AppState, files: List<File>) {
    val subtitleExtensions = setOf("srt", "ass", "ssa", "vtt")
    var opened = 0
    var subtitles = 0
    var folders = 0
    var unsupported = 0
    files.forEach { file ->
        when {
            file.isDirectory -> {
                state.media.library.addFolder(file.absolutePath)
                state.media.scanner.scan(file, recursive = true)
                folders++
            }
            file.extension.lowercase() in subtitleExtensions -> {
                state.media.openSubtitleFile(file)
                subtitles++
            }
            ua.syt0r.kanji.desktop.engine.media.MediaKind.of(file) != null -> {
                state.media.openFile(file)
                opened++
            }
            else -> unsupported++
        }
    }
    val summary = buildList {
        if (opened > 0) add("$opened media file(s) opened")
        if (subtitles > 0) add("$subtitles subtitle track(s) attached")
        if (folders > 0) add("$folders folder(s) scanned")
        if (unsupported > 0) add("$unsupported file(s) skipped")
    }.joinToString(" · ")
    if (summary.isNotBlank()) {
        state.toastHost.show(summary, kind = ua.syt0r.kanji.desktop.model.ToastKind.Info)
    }
}

// ============================================
// File pickers (desktop)
// ============================================

internal fun chooseMediaFile(state: AppState) {
    val chooser = javax.swing.JFileChooser().apply {
        dialogTitle = "Open media file (video / audio / image)"
        fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = true
        addChoosableFileFilter(
            javax.swing.filechooser.FileNameExtensionFilter(
                "Media",
                "mp4", "mkv", "webm", "mov", "avi", "m4v", "mp3", "wav", "ogg", "flac", "m4a", "aac", "opus"
            )
        )
    }
    if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
        chooser.selectedFiles.forEach { state.media.openFile(it) }
    }
}

internal fun chooseMediaFolder(state: AppState) {
    val chooser = javax.swing.JFileChooser().apply {
        dialogTitle = "Add media folder to library"
        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
    }
    if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
        val folder = chooser.selectedFile
        state.media.scanner.scan(folder, recursive = true) { added ->
            state.toastHost.show("Added $added media files from ${folder.name}")
        }
    }
}

internal fun chooseSubtitleFile(): java.io.File? {
    val chooser = javax.swing.JFileChooser().apply {
        dialogTitle = "Open subtitle file (SRT / ASS / SSA / VTT)"
        fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
        addChoosableFileFilter(
            javax.swing.filechooser.FileNameExtensionFilter("Subtitles", "srt", "ass", "ssa", "vtt")
        )
    }
    return if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

// ============================================
// Settings mapping helpers
// ============================================

internal fun autoPauseFromSettings(value: String): AutoPauseMode = when (value) {
    "at-cue-start" -> AutoPauseMode.AtCueStart
    "at-cue-end" -> AutoPauseMode.AtCueEnd
    "before-cue" -> AutoPauseMode.BeforeCue
    else -> AutoPauseMode.Off
}

internal fun annotationFromSettings(value: String): AnnotationMode = when (value) {
    "reading" -> AnnotationMode.Reading
    "frequency" -> AnnotationMode.Frequency
    "off" -> AnnotationMode.Off
    else -> AnnotationMode.Status
}

/** Small formatter alias so the UI doesn't import the engine companion directly. */
internal object MediaEngineFormat {
    fun time(ms: Long): String = ua.syt0r.kanji.desktop.engine.media.MediaEngine.formatTime(ms)
}
