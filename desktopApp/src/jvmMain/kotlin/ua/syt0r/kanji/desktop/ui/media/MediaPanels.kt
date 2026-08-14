package ua.syt0r.kanji.desktop.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsChip
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.errorColor
import ua.syt0r.kanji.desktop.designsystem.favoriteColor
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.desktop.engine.media.MediaAction
import ua.syt0r.kanji.desktop.engine.media.MediaActions
import ua.syt0r.kanji.desktop.engine.media.MediaBookmark
import ua.syt0r.kanji.desktop.engine.media.MediaCapture
import ua.syt0r.kanji.desktop.engine.media.MediaCoverageStats
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.media.MediaItem
import ua.syt0r.kanji.desktop.engine.mining.MiningMode
import ua.syt0r.kanji.desktop.engine.shortcuts.KeyChord
import ua.syt0r.kanji.desktop.designsystem.DsPromptDialog
import ua.syt0r.kanji.desktop.model.ToastKind
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.ColumnScope
import java.io.File
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

// ============================================
// MEDIA LIBRARY PANEL
// The catalog: continue watching, all media,
// watched folders, favorites and collections.
// Everything persists in ~/.kaiteyo/media.
// ============================================

@Composable
fun MediaLibraryPanel(state: AppState) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()
    // Single source of truth with the player toolbar's search box — typing in
    // either place filters the same list, in both directions.
    val query = media.librarySearchQuery
    var collection by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = "Media library",
            subtitle = "${library.items.size} items · ${library.watchedMediaCount()} watched · ${MediaEngine.formatTime(library.totalWatchTimeMs())} total watch time",
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(text = "Open file", icon = Icons.Default.PlayArrow, compact = true, onClick = { chooseMediaFile(state) })
                    DsButton(text = "Scan folder", icon = Icons.Default.FolderOpen, kind = DsButtonKind.Secondary, compact = true, onClick = { chooseMediaFolder(state) })
                }
            }
        )

        // This week — the mini 7-day immersion strip (watch bars + mined/lookups).
        MediaWeekStrip(state)

        // Scanning progress
        if (media.scanner.scanning) {
            DsCard {
                Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(media.scanner.scanMessage, color = sc.textSecondary, fontSize = DsType.Body)
                    DsProgressBar(fraction = media.scanner.progress)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${media.scanner.scannedFiles} files", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.weight(1f))
                        DsButton(text = "Cancel", kind = DsButtonKind.Ghost, compact = true, onClick = { media.scanner.cancel() })
                    }
                }
            }
        }

        DsSearchField(
            value = query,
            onValueChange = { media.librarySearchQuery = it },
            placeholder = "Search library…",
            modifier = Modifier.onFocusChanged { state.media.textInputFocused = it.isFocused }
        )

        // Collection filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            DsChip(text = "All", selected = collection.isEmpty(), onClick = { collection = "" })
            library.allCollections.take(8).forEach { c ->
                DsChip(text = c, selected = collection == c, onClick = { collection = if (collection == c) "" else c })
            }
        }

        val items = remember(query, collection, library.items.size) {
            var list = library.search(query)
            if (collection.isNotBlank()) list = list.filter { it.collection == collection }
            list
        }

        val continueWatching = remember(library.items.size) { library.continueWatching(8) }
        if (query.isBlank() && collection.isBlank() && continueWatching.isNotEmpty()) {
            DsCard {
                Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text("Continue watching", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    continueWatching.forEach { item -> MediaItemRow(state, item, prominent = true) }
                }
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("All media", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                if (items.isEmpty()) {
                    Text(
                        "Nothing here yet — open a file or scan a folder. Kaiteyo remembers position, subtitles and history for every item.",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                } else {
                    LazyColumn(Modifier.fillMaxWidth().height(420.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(items, key = { it.id }) { item -> MediaItemRow(state, item) }
                    }
                }
            }
        }

        if (media.playQueue.isNotEmpty()) {
            DsCard {
                Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Play queue", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        DsButton(text = "Clear", kind = DsButtonKind.Ghost, compact = true, onClick = { media.clearQueue() })
                    }
                    Text(
                        "When an item ends, Kaiteyo continues with the next queued item — or the next episode of the same series.",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                    media.playQueue.forEachIndexed { i, qItem ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                            Text(
                                if (i == media.queueIndex) "▶" else "${i + 1}",
                                color = if (i == media.queueIndex) accent().primary else sc.textMuted,
                                fontSize = DsType.Caption,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(22.dp)
                            )
                            Text(qItem.name, color = sc.textSecondary, fontSize = DsType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            DsIconButton(
                                icon = Icons.Default.SkipNext,
                                onClick = {
                                    // Position the cursor at the clicked item and
                                    // play exactly that item (playNext would skip it).
                                    media.queueIndex = i
                                    media.openItem(qItem)
                                },
                                contentDescription = "Play from here",
                                size = 22.dp
                            )
                            DsIconButton(icon = Icons.Default.Delete, onClick = { media.removeFromQueue(i) }, contentDescription = "Remove from queue", size = 22.dp)
                        }
                    }
                }
            }
        }

        if (library.folders.isNotEmpty()) {
            DsCard {
                Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text("Watched folders", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    library.folders.forEach { folder ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = sc.textSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(DsSpacing.Sm))
                            Text(folder.path, color = sc.textSecondary, fontSize = DsType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            DsButton(text = "Rescan", kind = DsButtonKind.Ghost, compact = true, onClick = {
                                val dir = File(folder.path)
                                if (dir.exists()) media.scanner.scan(dir, folder.includeSubdirs) else state.toastHost.show("Folder missing", kind = ToastKind.Warning)
                            })
                            DsIconButton(
                                icon = Icons.Default.Delete,
                                onClick = { library.folders.remove(folder) },
                                contentDescription = "Remove folder",
                                size = 26.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    var watchFolders by remember { mutableStateOf(state.settings.getBool("media.watch-folders")) }
                    DsToggle(
                        checked = watchFolders,
                        onCheckedChange = {
                            watchFolders = it
                            state.settings.set("media.watch-folders", it)
                            if (it) media.startFolderWatcher() else media.stopFolderWatcher()
                        },
                        label = "Watch these folders — auto-add new media"
                    )
                    if (media.scanner.watcherActive) {
                        Text(
                            if (media.scanner.lastWatchFound > 0) "Watcher active · ${media.scanner.lastWatchFound} new file(s) auto-added"
                            else "Watcher active · scanning every ~45 s",
                            color = successColor(),
                            fontSize = DsType.Caption
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaItemRow(state: AppState, item: MediaItem, prominent: Boolean = false) {
    val media = state.media
    val sc = surfaceColors()
    val ac = accent()

    // Poster frame (async ffmpeg) once available; in-flight reads null.
    val thumbPath = media.scanner.thumbnailState[item.id]
    LaunchedEffect(item.id) { media.scanner.requestThumbnail(item) }
    // One stat per item per path change — never a per-recomposition probe.
    val missing = remember(item.id, item.path) { !media.library.fileExists(item) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (item == media.currentItem) ac.primary.copy(alpha = 0.1f) else sc.surfaceElevated.copy(alpha = 0.5f))
            .clickable { if (missing) relinkItemDialog(state, item) else media.openItem(item) }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Box(
            modifier = Modifier
                .size(if (prominent) 44.dp else 34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ac.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbPath != null) {
                ThumbnailImage(thumbPath, Modifier.matchParentSize().clip(RoundedCornerShape(8.dp)))
            } else {
                Icon(
                    if (item.kind == ua.syt0r.kanji.desktop.engine.media.MediaKind.Video) Icons.Default.Movie
                    else if (item.kind == ua.syt0r.kanji.desktop.engine.media.MediaKind.Audio) Icons.Default.AudioFile
                    else Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = if (missing) errorColor() else ac.primary,
                    modifier = Modifier.size(if (prominent) 22.dp else 16.dp)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    color = sc.textPrimary,
                    fontSize = if (prominent) DsType.BodyLarge else DsType.Body,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.episode.isNotBlank()) {
                    DsBadge(text = item.episode, tint = accent().primary)
                }
                if (item.collection.isNotBlank()) {
                    DsBadge(text = item.collection, tint = sc.textSecondary)
                }
            }
            if (missing) {
                Text(
                    "File moved or renamed — click to relink",
                    color = errorColor(),
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
            } else if (item.durationMs > 0) {
                Text(
                    "${MediaEngine.formatTime(item.durationMs)} · ${item.watchCount} watch${if (item.watchCount == 1) "" else "es"}${if (item.completed) " · completed" else ""}",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            if (item.lastWatchedAt.isNotBlank() && item.progressFraction > 0f && item.progressFraction < 1f) {
                Spacer(Modifier.height(3.dp))
                DsProgressBar(fraction = item.progressFraction, height = 3.dp, color = if (prominent) ac.primary else sc.textSecondary)
            }
        }
        if (missing) {
            DsButton(text = "Relink", kind = DsButtonKind.Ghost, compact = true, onClick = { relinkItemDialog(state, item) })
        } else {
            DsIconButton(
                icon = Icons.Default.SkipNext,
                onClick = { media.addToQueue(item) },
                contentDescription = "Add to queue",
                size = 26.dp
            )
        }
        Icon(
            if (item.favorite) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = "Favorite",
            tint = if (item.favorite) favoriteColor() else sc.textMuted,
            modifier = Modifier.size(18.dp).clickable { media.library.toggleFavorite(item.id) }
        )
    }
}

/** Load a cached poster frame from disk onto the composition (never blocks). */
@Composable
private fun ThumbnailImage(path: String, modifier: Modifier = Modifier) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { javax.imageio.ImageIO.read(java.io.File(path)).toComposeImageBitmap() }.getOrNull()
        }
    }
    bitmap?.let { Image(bitmap = it, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop) }
}

/** Ask the user where a moved file currently lives, then relink it. */
private fun relinkItemDialog(state: AppState, item: MediaItem) {
    val chooser = javax.swing.JFileChooser().apply {
        dialogTitle = "Relink \"${item.name}\" — choose its current location"
        fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
    }
    if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
        state.media.relinkItem(item.id, chooser.selectedFile)
    }
}

// ============================================
// MEDIA SETTINGS PANEL (in-workspace)
// Backend diagnostics, capture tools and study
// mode. The full settings grid lives in
// Settings → Media.
// ============================================

@Composable
fun MediaSettingsPanel(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    var probes by remember { mutableStateOf(media.probeBackends()) }
    var rebindTarget by remember { mutableStateOf<MediaAction?>(null) }
    var hotkeyVersion by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = "Media settings",
            subtitle = "Backend status, capture and study preferences",
            action = {
                DsButton(text = "Re-probe backends", kind = DsButtonKind.Ghost, compact = true, onClick = {
                    media.backends.refreshVlc()
                    media.backends.refreshMpv()
                    probes = media.probeBackends()
                    state.toastHost.show("Backends re-probed", kind = ToastKind.Info)
                })
            }
        )

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Playback backends", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                probes.forEach { probe ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(probe.kind.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
                            Text(probe.version.ifBlank { probe.message }, color = sc.textMuted, fontSize = DsType.Caption, maxLines = 1)
                        }
                        DsBadge(text = probe.statusLabel, tint = if (probe.available) successColor() else errorColor())
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ffmpeg (audio capture)", color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
                        Text(
                            if (MediaCapture.ffmpegAvailable) "Installed — audio clips can be extracted from any media"
                            else "Not found — WAV/AIFF sources still work",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                    DsBadge(text = if (MediaCapture.ffmpegAvailable) "Installed" else "Not installed", tint = if (MediaCapture.ffmpegAvailable) successColor() else warningColor())
                }
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Study preferences", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                DsToggle(
                    checked = media.studyMode,
                    onCheckedChange = { media.studyMode = it },
                    label = "Study mode — count watch time as study time"
                )
                DsToggle(
                    checked = media.condensedPlayback,
                    onCheckedChange = { media.condensedPlayback = it },
                    label = "Condensed playback — skip unsubtitled sections"
                )
                var fastForward by remember { mutableStateOf(state.settings.getBool("media.condensed-fast-forward")) }
                DsToggle(
                    checked = fastForward,
                    onCheckedChange = {
                        fastForward = it
                        state.settings.set("media.condensed-fast-forward", it)
                        media.condensedFastForward = it
                    },
                    label = "Fast-forward through unsubtitled gaps instead of jumping"
                )
                DsToggle(
                    checked = media.subtitles.showSecondary,
                    onCheckedChange = { media.subtitles.showSecondary = it },
                    label = "Dual subtitles — show the secondary track"
                )
                var mineVideo by remember { mutableStateOf(state.settings.getBool("media.mine-video")) }
                DsToggle(
                    checked = mineVideo,
                    onCheckedChange = {
                        mineVideo = it
                        state.settings.set("media.mine-video", it)
                    },
                    label = "Attach a video clip to mined cards (needs ffmpeg)"
                )
                DsToggle(
                    checked = media.miniPlayerEnabled,
                    onCheckedChange = {
                        media.miniPlayerEnabled = it
                        state.settings.set("media.mini-player", it)
                    },
                    label = "Persistent mini player — keep playing while browsing other workspaces"
                )
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Keyboard shortcuts", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Rebindable media hotkeys — the single source of truth for the player keys. Esc keeps its contextual behaviour.",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                    DsButton(
                        text = "Reset all",
                        kind = DsButtonKind.Ghost,
                        compact = true,
                        onClick = {
                            media.hotkeys.resetAll()
                            hotkeyVersion++
                            state.toastHost.show("Media shortcuts restored to defaults", kind = ToastKind.Info)
                        }
                    )
                }
                val actions = remember(hotkeyVersion) { MediaActions.all }
                actions.forEach { action ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(action.label, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
                            if (action.description.isNotBlank()) {
                                Text(action.description, color = sc.textMuted, fontSize = DsType.Caption)
                            }
                        }
                        Text(
                            media.hotkeys.chordLabel(action.id),
                            color = accent().primary,
                            fontSize = DsType.Label,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accent().primary.copy(alpha = 0.12f))
                                .padding(horizontal = DsSpacing.Sm, vertical = 3.dp)
                        )
                        DsButton(
                            text = "Rebind",
                            kind = DsButtonKind.Ghost,
                            compact = true,
                            onClick = { rebindTarget = action }
                        )
                        DsButton(
                            text = "Reset",
                            kind = DsButtonKind.Ghost,
                            compact = true,
                            onClick = {
                                media.hotkeys.reset(action.id)
                                hotkeyVersion++
                            }
                        )
                    }
                }
                Text(
                    "Keys work while the Media workspace has focus. Rebind via chord syntax, e.g. Ctrl+Shift+K.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("System media keys & notifications", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Dedicated keyboard media buttons (Play/Pause, Next, Previous, Stop) control the player even while Kaiteyo is in the background. Windows captures them through a global keyboard hook; on macOS/Linux the tray menu and in-app hotkeys cover background control.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
                var keysEnabled by remember { mutableStateOf(state.settings.getBool("media.system-media-keys")) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsToggle(
                        checked = keysEnabled,
                        onCheckedChange = {
                            keysEnabled = it
                            state.settings.set("media.system-media-keys", it)
                            if (it) media.startSystemMediaKeys() else media.stopSystemMediaKeys()
                        },
                        label = "Global media keys"
                    )
                    DsBadge(
                        text = when {
                            !media.systemMediaKeysSupported -> "Unsupported on this OS"
                            media.systemMediaKeysActive -> "Listening"
                            keysEnabled -> "Registration failed"
                            else -> "Disabled"
                        },
                        tint = when {
                            !media.systemMediaKeysSupported -> sc.textSecondary
                            media.systemMediaKeysActive -> successColor()
                            else -> warningColor()
                        }
                    )
                }
                if (keysEnabled && !media.systemMediaKeysActive && media.systemMediaKeysSupported) {
                    media.systemMediaKeys.lastError?.let {
                        Text(it, color = errorColor(), fontSize = DsType.Caption)
                    }
                }
                var notificationsEnabled by remember { mutableStateOf(state.settings.getBool("media.notifications")) }
                DsToggle(
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        notificationsEnabled = it
                        state.settings.set("media.notifications", it)
                    },
                    label = "Playback notifications (start, pause, finish)"
                )
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("GameSentenceMiner integration", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                val gsm = state.miningIntegration.gsm
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(gsm.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
                        Text(
                            gsm.lastError ?: if (gsm.connected) "Connected to ${state.settings.getString("media.gsm.host", "127.0.0.1")}:${state.settings.getString("media.gsm.port", "9000")}" else "Not connected",
                            color = sc.textMuted,
                            fontSize = DsType.Caption,
                            maxLines = 1
                        )
                    }
                    DsBadge(text = if (gsm.connected) "Connected" else "Offline", tint = if (gsm.connected) successColor() else warningColor())
                }
                var host by remember { mutableStateOf(state.settings.getString("media.gsm.host", "127.0.0.1")) }
                var port by remember { mutableStateOf(state.settings.getString("media.gsm.port", "9000")) }
                DsTextField(value = host, onValueChange = { host = it }, placeholder = "GSM host (127.0.0.1)", label = "Host")
                DsTextField(value = port, onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) }, placeholder = "GSM port (9000)", label = "Port")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(
                        text = "Save & test",
                        compact = true,
                        onClick = {
                            state.settings.set("media.gsm.host", host)
                            state.settings.set("media.gsm.port", port.toIntOrNull() ?: 9000)
                            gsm.testConnection()
                                .onSuccess { msg -> state.toastHost.show(msg, kind = ToastKind.Success) }
                                .onFailure { e -> state.toastHost.show("GSM unreachable: ${e.message}", kind = ToastKind.Warning) }
                        }
                    )
                    Text("Mining mode", color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                    DsSelect(
                        selected = state.miningIntegration.mode,
                        options = MiningMode.entries,
                        onSelected = { state.settings.set("media.mining-mode", it.name.lowercase()) },
                        labelOf = { it.label },
                        modifier = Modifier.width(220.dp)
                    )
                }
                Text(
                    "Kaiteyo's own card pool always receives mines — GameSentenceMiner is an optional mirror for users who want it.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Text hook & player WebSocket", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Send Japanese text into the dictionary from texthookers or scripts, and stream live player state to external tools. Both are opt-in and local-only.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )

                var hookEnabled by remember { mutableStateOf(state.settings.getBool("media.text-hook.enabled")) }
                var hookPort by remember { mutableStateOf(state.settings.getInt("media.text-hook.port", 8766).toString()) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsToggle(
                        checked = hookEnabled,
                        onCheckedChange = {
                            hookEnabled = it
                            state.settings.set("media.text-hook.enabled", it)
                            if (it) media.startTextHook(hookPort.toIntOrNull() ?: 8766) else media.stopTextHook()
                        },
                        label = "Text hook (TCP)"
                    )
                    DsTextField(
                        value = hookPort,
                        onValueChange = { hookPort = it.filter { c -> c.isDigit() }.take(5) },
                        placeholder = "8766",
                        modifier = Modifier.width(120.dp)
                    )
                    DsBadge(
                        text = if (media.textHookRunning) "Listening · ${media.textHookClients} client(s)" else "Off",
                        tint = if (media.textHookRunning) successColor() else sc.textSecondary
                    )
                }
                Text(
                    "Send a line via echo or netcat to 127.0.0.1 ${hookPort.ifBlank { "8766" }} — send CLEAR to reset the lookup.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )

                var wsEnabled by remember { mutableStateOf(state.settings.getBool("media.ws.enabled")) }
                var wsPort by remember { mutableStateOf(state.settings.getInt("media.ws.port", 8765).toString()) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsToggle(
                        checked = wsEnabled,
                        onCheckedChange = {
                            wsEnabled = it
                            state.settings.set("media.ws.enabled", it)
                            if (it) media.startPlayerSocket(wsPort.toIntOrNull() ?: 8765) else media.stopPlayerSocket()
                        },
                        label = "Player WebSocket"
                    )
                    DsTextField(
                        value = wsPort,
                        onValueChange = { wsPort = it.filter { c -> c.isDigit() }.take(5) },
                        placeholder = "8765",
                        modifier = Modifier.width(120.dp)
                    )
                    DsBadge(
                        text = if (media.wsRunning) "Broadcasting · ${media.wsClients} client(s)" else "Off",
                        tint = if (media.wsRunning) successColor() else sc.textSecondary
                    )
                }
                Text(
                    "ws://127.0.0.1:${wsPort.ifBlank { "8765" }} — live state JSON every 500 ms. Send JSON control frames like {command: play} or {command: seek, positionMs: 9000}.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Debug — live player state", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    DsButton(
                        text = "Copy",
                        kind = DsButtonKind.Ghost,
                        compact = true,
                        onClick = {
                            java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(
                                java.awt.datatransfer.StringSelection(media.debugSnapshot()),
                                null
                            )
                            state.toastHost.show("Debug snapshot copied", kind = ToastKind.Info)
                        }
                    )
                }
                Text(
                    media.debugSnapshot(),
                    color = sc.textSecondary,
                    fontSize = DsType.Caption,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Media cache", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${MediaCapture.cacheFileCount()} screenshots and audio clips · ${(MediaCapture.cacheSizeBytes() / 1048576.0).let { String.format("%.1f MB", it) }}",
                            color = sc.textSecondary,
                            fontSize = DsType.Body
                        )
                        Text("Captured frames and clips live in ~/.kaiteyo/media-cache, outside the study database.", color = sc.textMuted, fontSize = DsType.Caption)
                    }
                    DsButton(text = "Clear cache", kind = DsButtonKind.Ghost, compact = true, onClick = {
                        val removed = MediaCapture.clearCache()
                        state.toastHost.show("Cleared $removed cached files", kind = ToastKind.Info)
                    })
                }
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("This session", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${MediaEngine.formatTime(media.watchTimeMs)} watched · ${media.subtitles.tracks.size} subtitle track(s) · ${media.audioClips.size} audio clip(s) · ${media.bookmarks.size} bookmark(s)",
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
                Text(
                    "Mined items appear in the Library and Review automatically — media is not a separate database.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
    }

    rebindTarget?.let { action ->
        DsPromptDialog(
            title = "Rebind '${action.label}'",
            placeholder = "Chord, e.g. Ctrl+Shift+K",
            initialValue = media.hotkeys.chordLabel(action.id),
            onConfirm = { raw ->
                val chord = KeyChord.fromLabel(raw)
                if (chord == null) {
                    state.toastHost.show("Could not parse '${raw}' as a chord", kind = ToastKind.Error)
                } else {
                    val ok = media.hotkeys.bind(action.id, chord)
                    if (ok) {
                        hotkeyVersion++
                        state.toastHost.show("'${action.label}' → ${chord.label}", kind = ToastKind.Success)
                    } else {
                        state.toastHost.show("Chord ${chord.label} is invalid or already in use", kind = ToastKind.Warning)
                    }
                }
                rebindTarget = null
            },
            onDismiss = { rebindTarget = null }
        )
    }
}

// ============================================
// MEDIA BOOKMARKS PANEL
// Moments saved while watching — click to seek,
// delete to forget. Bookmarks persist in
// ~/.kaiteyo/media-state.json.
// ============================================

@Composable
fun MediaBookmarksPanel(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    val item = media.currentItem
    val bookmarks = remember(item, media.bookmarks.size) { item?.let { media.bookmarksFor(it) } ?: emptyList() }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = "Bookmarks",
            subtitle = if (item != null) "${bookmarks.size} moment${if (bookmarks.size == 1) "" else "s"} saved in ${item.name}" else "Open media to bookmark moments",
            action = {
                DsButton(
                    text = "Bookmark now",
                    icon = Icons.Default.Bookmark,
                    compact = true,
                    enabled = item != null,
                    onClick = { media.addBookmark() }
                )
            }
        )
        if (item == null) {
            DsEmptyState(
                "Nothing bookmarked",
                "Play something and press ${state.media.hotkeys.chordLabel("bookmark")} — bookmarks remember the exact moment so you can jump back later.",
                icon = Icons.Default.Bookmark
            )
            return@Column
        }
        if (bookmarks.isEmpty()) {
            DsCard {
                Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text("No bookmarks yet", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Press ${state.media.hotkeys.chordLabel("bookmark")} or use the bookmark button in the player controls while watching.", color = sc.textMuted, fontSize = DsType.Caption)
                }
            }
        } else {
            DsCard {
                Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Saved moments", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    bookmarks.forEach { bm -> BookmarkRow(state, bm) }
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(state: AppState, bm: MediaBookmark) {
    val media = state.media
    val sc = surfaceColors()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                media.seekTo(bm.timestampMs)
                media.play()
            }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Icon(Icons.Default.Bookmark, contentDescription = null, tint = accent().primary, modifier = Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(bm.label.ifBlank { "Bookmark" }, color = sc.textPrimary, fontSize = DsType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${MediaEngine.formatTime(bm.timestampMs)} · ${bm.note.ifBlank { "Seek & play" }}",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
        DsIconButton(icon = Icons.Default.Delete, onClick = { media.removeBookmark(bm.id) }, contentDescription = "Delete bookmark", size = 26.dp)
    }
}

// ============================================
// MEDIA STUDY STATS PANEL
// Honest per-media numbers: known-word coverage
// (clearly an estimate), kanji inventory, watch
// vs study time, mining totals. No fabricated
// comprehension scores.
// ============================================

@Composable
fun MediaStatsPanel(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    val item = media.currentItem
    val cueCount = media.subtitles.activeTrack?.track?.cues?.size ?: 0
    val stats = remember(item, cueCount) { item?.let { media.mediaStatsFor(it) } ?: MediaCoverageStats(0, 0, 0, 0, 0, 0) }
    val kanji = remember(item, cueCount) {
        media.subtitles.activeTrack?.track?.cues
            ?.flatMap { cue -> media.kanjiIn(media.displayTextFor(cue)) }
            ?.distinct() ?: emptyList()
    }
    val mined = remember(item, media.miningEvents.size) {
        item?.let { media.miningEvents.count { e -> e.mediaPath == item.path } } ?: 0
    }
    val bookmarksForItem = remember(item, media.bookmarks.size) { item?.let { media.bookmarksFor(it) } ?: emptyList() }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = "Media study stats",
            subtitle = if (item != null) "${item.name} · ${item.episode.ifBlank { "no episode tag" }}" else "Open media with subtitles to see coverage"
        )
        if (item == null) {
            DsEmptyState(
                "No media loaded",
                "Open a video or episode with subtitles — coverage and kanji inventories are computed from the real subtitle track.",
                icon = Icons.Default.AudioFile
            )
            return@Column
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Vocabulary coverage", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${stats.totalTokens} unique words in the subtitle track, classified against your card pool.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
                DsProgressBar(fraction = stats.coverage)
                Text(
                    "${(stats.coverage * 100).toInt()}% known or learning — an estimate, not a comprehension score",
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
                    StatPill("Known", stats.known, successColor())
                    StatPill("Learning", stats.learning, warningColor())
                    StatPill("Unknown", stats.unknown, sc.textSecondary)
                    StatPill("Mined", stats.mined, accent().primary)
                    if (stats.suspended > 0) StatPill("Suspended", stats.suspended, errorColor())
                }
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Kanji in this media", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                if (kanji.isEmpty()) {
                    Text("No kanji found in the loaded subtitle track.", color = sc.textMuted, fontSize = DsType.Caption)
                } else {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                        kanji.forEach { ch ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(sc.surfaceInteractive)
                                    .padding(horizontal = DsSpacing.Sm, vertical = 4.dp)
                            ) {
                                Text(ch.toString(), color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Text("${kanji.size} distinct kanji — each one is a candidate for review practice.", color = sc.textMuted, fontSize = DsType.Caption)
                }
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Organize", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                val collections = media.library.allCollections
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text("Collection", color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                    DsSelect(
                        selected = collections.firstOrNull { it == item.collection } ?: "None",
                        options = listOf("None") + collections,
                        onSelected = { media.library.setCollection(item.id, if (it == "None") "" else it) },
                        labelOf = { it },
                        modifier = Modifier.width(220.dp)
                    )
                }
                Text("Collections keep your anime, movies, audiobooks and study series organized without touching the files.", color = sc.textMuted, fontSize = DsType.Caption)
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Comprehension (your estimate)", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Kaiteyo never pretends to measure comprehension from watch time — rate how much you understood yourself.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                    (1..5).forEach { rating ->
                        Icon(
                            if (item.comprehension >= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$rating of 5",
                            tint = if (item.comprehension >= rating) favoriteColor() else sc.textMuted,
                            modifier = Modifier.size(26.dp).clickable {
                                media.library.setComprehension(
                                    item.id,
                                    if (item.comprehension == rating) 0 else rating
                                )
                            }
                        )
                    }
                    if (item.comprehension > 0) {
                        Text("${item.comprehension}/5", color = sc.textSecondary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Notes & tags", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                var note by remember(item.id, item.note) { mutableStateOf(item.note) }
                var tags by remember(item.id, item.tags) { mutableStateOf(item.tags.joinToString(", ")) }
                DsTextField(value = note, onValueChange = { note = it }, placeholder = "Notes about this media…", singleLine = false)
                DsTextField(value = tags, onValueChange = { tags = it }, placeholder = "anime, slice-of-life, jlpt-n3")
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                    DsButton(
                        text = "Save",
                        compact = true,
                        onClick = {
                            media.library.setNote(item.id, note)
                            media.library.setTags(item.id, tags.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                            state.toastHost.show("Notes & tags saved", kind = ToastKind.Info)
                        }
                    )
                    Text("Tags also appear in library search.", color = sc.textMuted, fontSize = DsType.Caption)
                }
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Progress", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${(item.progressFraction * 100).toInt()}% watched · ${item.watchCount} watch${if (item.watchCount == 1) "" else "es"} · $cueCount subtitle lines",
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
                Text(
                    "${MediaEngine.formatTime(media.watchTimeMs)} watch time this session${if (media.studyMode) " — counted as study time" else ""}",
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
                Text(
                    "$mined sentence${if (mined == 1) "" else "s"} mined · ${bookmarksForItem.size} bookmark${if (bookmarksForItem.size == 1) "" else "s"} · ${media.dictionaryLookupCount} dictionary lookups",
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: Int, color: Color) {
    val sc = surfaceColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = color, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
    }
}

// ============================================
// THIS WEEK — MINI 7-DAY MEDIA STATS STRIP
// Compact week-at-a-glance for the library panel:
// watch-time bars (study share on top in solid
// accent, leisure below dimmed), plus mined /
// lookup / session totals for the same window.
// Everything comes from the real statistics
// stores — never synthetic.
// ============================================

@Composable
private fun MediaWeekStrip(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    val ac = accent()
    val stats = media.statistics
    val miningStats = state.miningStatistics
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val weekStart = today.minus(6, DateTimeUnit.DAY)
    // Oldest → today, computed inline so live bumps to today's bucket stay fresh.
    val days = (0L until 7L).map { offset -> today.minus(offset, DateTimeUnit.DAY) }.reversed()
    val maxMs = maxOf(1L, days.maxOf { stats.day(it).watchMs })
    val weekWatch = stats.watchMsBetween(weekStart, today)
    val weekStudy = stats.studyMsBetween(weekStart, today)
    val weekMined = miningStats.minedBetween(weekStart, today)
    val weekLookups = stats.lookupsBetween(weekStart, today)
    val weekSessions = stats.daysBetween(weekStart, today).sumOf { it.sessions }
    val weekdayLabels = listOf("月", "火", "水", "木", "金", "土", "日")

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("This week", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Watch time per day · solid = study mode", color = sc.textMuted, fontSize = DsType.Caption)
                }
                Text(
                    "${MediaEngine.formatTime(weekWatch)} watched · ${MediaEngine.formatTime(weekStudy)} study",
                    color = sc.textSecondary,
                    fontSize = DsType.Caption
                )
            }
            Row(
                Modifier.fillMaxWidth().height(52.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                days.forEach { day ->
                    val stat = stats.day(day)
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((44.dp * (stat.watchMs.toFloat() / maxMs)).coerceAtLeast(if (stat.watchMs > 0) 3.dp else 2.dp))
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(if (stat.watchMs == 0L) sc.surfaceInteractive else Color.Transparent)
                        ) {
                            if (stat.watchMs > 0L) {
                                Column(Modifier.fillMaxWidth()) {
                                    WeekSegment(stat.studyMs.toFloat() / stat.watchMs, ac.primary)
                                    WeekSegment((stat.watchMs - stat.studyMs).toFloat() / stat.watchMs, ac.primary.copy(alpha = 0.35f))
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            weekdayLabels[day.dayOfWeek.ordinal],
                            color = if (day == today) ac.primary else sc.textMuted,
                            fontSize = DsType.Caption,
                            fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xl)) {
                WeekStatPill("Mined", weekMined.toString())
                WeekStatPill("Lookups", weekLookups.toString())
                WeekStatPill("Sessions", weekSessions.toString())
                WeekStatPill("Study", MediaEngine.formatTime(weekStudy))
            }
            if (weekWatch == 0L) {
                Text(
                    "No media activity this week — open a video or episode and watch with subtitles to build your immersion stats.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.WeekSegment(fraction: Float, color: Color) {
    if (fraction <= 0f) return
    Box(Modifier.fillMaxWidth().weight(fraction).background(color))
}

@Composable
private fun WeekStatPill(label: String, value: String) {
    val sc = surfaceColors()
    Column {
        Text(value, color = accent().primary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
    }
}
