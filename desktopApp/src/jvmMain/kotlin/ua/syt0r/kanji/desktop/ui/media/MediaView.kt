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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    Home("Home"), Player("Player"), Library("Library"), Stats("Stats"), Bookmarks("Bookmarks"), Tuning("Video & Audio"), Settings("Settings")
}

@Composable
fun MediaView(state: AppState, onBack: (() -> Unit)? = null) {
    val media = state.media
    var panel by remember { mutableStateOf(MediaPanel.Home) }
    // Media item currently shown in the detail view (null = normal panels).
    var detailItemId by remember { mutableStateOf<String?>(null) }
    // The ComposeWindow hosting this workspace (best effort; null in previews).
    // Found via the AWT window list because Compose's LocalWindow is not public
    // API in this version and LocalView no longer exists on desktop.
    val window = remember {
        runCatching {
            java.awt.Window.getWindows().firstOrNull { it is ComposeWindow && it.isShowing } as? ComposeWindow
        }.getOrNull()
    }

    // Drag & drop: dropping a video/audio file opens it, a subtitle file
    // attaches to the current media, and a folder gets scanned into the
    // library. Active only while the Media workspace is composed.
    DisposableEffect(window) {
        val host = window ?: return@DisposableEffect onDispose {}
        // Drag & drop is a nicety — if the AWT drop target cannot be created
        // (unusual desktop/headless combinations) the workspace still opens.
        val dropTarget = runCatching {
            DropTarget(
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
        }.getOrNull() ?: return@DisposableEffect onDispose {}
        onDispose { host.contentPane.dropTarget = null }
    }

    // The 10 Hz reconciliation loop: position, active subtitle, condensed
    // playback, auto-pause, looping and periodic watch-progress saves.
    LaunchedEffect(Unit) {
        // Restoring preferences must never take the workspace down — every
        // getter already falls back to a default, and this guard makes a
        // single unexpected read safe too.
        runCatching {
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
            // Rendering + audio preferences restored on launch.
            media.displayMode = ua.syt0r.kanji.desktop.engine.playback.VideoDisplayMode.entries
                .firstOrNull { it.name.equals(state.settings.getString("media.display-mode", "fit"), ignoreCase = true) }
                ?: ua.syt0r.kanji.desktop.engine.playback.VideoDisplayMode.Fit
            media.aspectRatio = ua.syt0r.kanji.desktop.engine.playback.AspectRatioPreset.entries
                .firstOrNull { it.name.equals(state.settings.getString("media.aspect-ratio", "auto"), ignoreCase = true) }
                ?: ua.syt0r.kanji.desktop.engine.playback.AspectRatioPreset.Auto
            media.videoAdjustments = ua.syt0r.kanji.desktop.engine.playback.VideoAdjustments(
                brightness = state.settings.getInt("media.video-brightness", 100).toFloat(),
                contrast = state.settings.getInt("media.video-contrast", 100).toFloat(),
                saturation = state.settings.getInt("media.video-saturation", 100).toFloat(),
                gamma = state.settings.getInt("media.video-gamma", 100).toFloat(),
                hue = state.settings.getInt("media.video-hue", 0).toFloat(),
                deinterlace = state.settings.getBool("media.video-deinterlace")
            )
            media.subtitleDelayMs = state.settings.getInt("media.subtitle-delay-ms", 0).toLong()
            media.audioDelayMs = state.settings.getInt("media.audio-delay-ms", 0).toLong()
            media.audioChannel = ua.syt0r.kanji.desktop.engine.playback.AudioChannelPreset.entries
                .firstOrNull { it.name.equals(state.settings.getString("media.audio-channel", "stereo"), ignoreCase = true) }
                ?: ua.syt0r.kanji.desktop.engine.playback.AudioChannelPreset.Stereo
            media.audioOutputId = state.settings.getString("media.audio-output", "").ifBlank { null }
            val eqPreset = ua.syt0r.kanji.desktop.engine.playback.EqualizerPreset.entries
                .firstOrNull { it.name.equals(state.settings.getString("media.eq-preset", "flat"), ignoreCase = true) }
                ?: ua.syt0r.kanji.desktop.engine.playback.EqualizerPreset.Flat
            val eqBands = state.settings.getString("media.eq-bands-db", "").split(",")
                .mapNotNull { it.trim().toFloatOrNull() }
            media.equalizer = if (eqBands.size == 10) {
                ua.syt0r.kanji.desktop.engine.playback.EqualizerSettings(
                    preset = eqPreset,
                    preampDb = state.settings.getFloat("media.eq-preamp-db", eqPreset.preampDb),
                    bandsDb = eqBands
                )
            } else {
                ua.syt0r.kanji.desktop.engine.playback.EqualizerSettings(preset = eqPreset)
            }
        }
        // The 10 Hz reconciliation loop — media.tick() is engine-hardened and
        // never throws; delay() cancellation is normal teardown.
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
            MediaToolbar(state, panel, onSelectPanel = { panel = it }, onBack = onBack)
        }
        // The detail view overlays the panels while an item is open; opening
        // other media or navigating tabs dismisses it.
        val detailId = detailItemId
        val liveItem = detailId?.let { media.library.item(it) }
        if (liveItem != null) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                MediaDetailPanel(state = state, itemId = liveItem.id, onBack = { detailItemId = null })
            }
        } else {
            if (detailId != null) {
                LaunchedEffect(detailId) { detailItemId = null }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (panel) {
                    MediaPanel.Home -> MediaHomePanel(state, onOpenDetail = { detailItemId = it })
                    MediaPanel.Player -> MediaPlayerWorkspace(state)
                    MediaPanel.Library -> MediaLibraryPanel(state, onOpenDetail = { detailItemId = it })
                    MediaPanel.Stats -> MediaStatsPanel(state)
                    MediaPanel.Bookmarks -> MediaBookmarksPanel(state)
                    MediaPanel.Tuning -> MediaTuningPanel(state)
                    MediaPanel.Settings -> MediaSettingsPanel(state)
                }
            }
        }
    }
}

@Composable
private fun MediaToolbar(
    state: AppState,
    panel: MediaPanel,
    onSelectPanel: (MediaPanel) -> Unit,
    onBack: (() -> Unit)? = null
) {
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
            if (onBack != null) {
                DsIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Back",
                    size = 34.dp
                )
            }
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
            // In-player library search — types land in the Media Centre home
            // (which switches to its Browse view when a query is present).
            DsSearchField(
                value = media.librarySearchQuery,
                onValueChange = {
                    media.librarySearchQuery = it
                    onSelectPanel(MediaPanel.Home)
                },
                placeholder = "Search library…",
                modifier = Modifier.width(190.dp).onFocusChanged { state.media.textInputFocused = it.isFocused }
            )
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
            // Less frequent actions live in an overflow so the toolbar stays
            // calm — the tabs already cover Stats / Bookmarks / Tuning / Settings.
            var moreOpen by remember { mutableStateOf(false) }
            Box {
                DsIconButton(
                    icon = Icons.Default.MoreVert,
                    onClick = { moreOpen = true },
                    contentDescription = "More media actions",
                    size = 34.dp
                )
                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Open URL…") },
                        onClick = {
                            moreOpen = false
                            urlPromptOpen = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Load secondary subtitles (dual-language)…") },
                        onClick = {
                            moreOpen = false
                            val subFile = chooseSubtitleFile()
                            if (subFile != null) media.openSecondarySubtitleFile(subFile)
                        }
                    )
                }
            }
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
