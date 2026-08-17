package ua.syt0r.kanji.desktop.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
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
import ua.syt0r.kanji.desktop.engine.playback.AspectRatioPreset
import ua.syt0r.kanji.desktop.engine.playback.AudioChannelPreset
import ua.syt0r.kanji.desktop.engine.playback.EqualizerPreset
import ua.syt0r.kanji.desktop.engine.playback.PlaybackCapability
import ua.syt0r.kanji.desktop.engine.playback.VideoDisplayMode
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
fun MediaLibraryPanel(state: AppState, onOpenDetail: (String) -> Unit) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()
    // Single source of truth with the player toolbar's search box — typing in
    // either place filters the same list, in both directions.
    val query = media.librarySearchQuery
    var collection by remember { mutableStateOf("") }
    // Playlist creation / rename dialog state.
    var createPlaylistOpen by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ua.syt0r.kanji.desktop.engine.media.MediaPlaylist?>(null) }

    val items = remember(query, collection, library.items.size) {
        var list = library.search(query)
        if (collection.isNotBlank()) list = list.filter { it.collection == collection }
        list
    }
    val continueWatching = remember(library.items.size) { library.continueWatching(8) }
    val searching = query.isNotBlank() || collection.isNotBlank()

    if (library.items.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
            DsSectionHeader(title = "Media library", subtitle = "Add media to get started")
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = accent().primary.copy(alpha = 0.45f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Your library is empty", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Open a file, scan a folder or drop media anywhere in the Media workspace.",
                        color = sc.textMuted,
                        fontSize = DsType.Body
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        DsButton(text = "Open file", icon = Icons.Default.PlayArrow, onClick = { chooseMediaFile(state) })
                        DsButton(text = "Scan folder", icon = Icons.Default.FolderOpen, kind = DsButtonKind.Secondary, onClick = { chooseMediaFolder(state) })
                    }
                }
            }
        }
        return
    }

    // The whole library is one adaptive grid: full-span sections (header,
    // week strip, search, filters, rails) with the media cards as the actual
    // cells — the window width decides the column count, nothing is fixed.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(200.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = DsSpacing.Lg, end = DsSpacing.Lg, top = DsSpacing.Lg, bottom = DsSpacing.Xxl),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
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
        }

        // This week — the mini 7-day immersion strip (watch bars + mined/lookups).
        item(span = { GridItemSpan(maxLineSpan) }) {
            MediaWeekStrip(state)
        }

        // Scanning progress
        if (media.scanner.scanning) {
            item(span = { GridItemSpan(maxLineSpan) }) {
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
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            DsSearchField(
                value = query,
                onValueChange = { media.librarySearchQuery = it },
                placeholder = "Search library…",
                modifier = Modifier.onFocusChanged { state.media.textInputFocused = it.isFocused }
            )
        }

        // Collection filter chips — scrolls horizontally so many collections
        // never crowd the grid.
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                DsChip(text = "All", selected = collection.isEmpty(), onClick = { collection = "" })
                library.allCollections.forEach { c ->
                    DsChip(text = c, selected = collection == c, onClick = { collection = if (collection == c) "" else c })
                }
            }
        }

        // Continue watching — a wide horizontal rail, not a list of rows.
        if (!searching && continueWatching.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Continue watching", "${continueWatching.size} in progress — tap a card to resume")
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().height(196.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                        items(continueWatching, key = { it.id }) { item ->
                            Box(Modifier.width(220.dp)) {
                                MediaItemCard(state, item, showProgress = true, onOpenDetail = onOpenDetail)
                            }
                        }
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle(if (searching) "Search results" else "All media", "${items.size} item${if (items.size == 1) "" else "s"}")
        }
        if (items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(DsSpacing.Xxl), contentAlignment = Alignment.Center) {
                    Text("No media matches — adjust the search or filters", color = sc.textMuted, fontSize = DsType.Body)
                }
            }
        } else {
            gridItems(items, key = { it.id }) { item ->
                MediaItemCard(state, item, showProgress = true, onOpenDetail = onOpenDetail)
            }
        }

        if (media.playQueue.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
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
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            DsCard {
                Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Playlists", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Named, reorderable playlists that persist in the library — create one, add items, then play it as a queue.",
                                color = sc.textMuted,
                                fontSize = DsType.Caption
                            )
                        }
                        if (library.playlists.isNotEmpty()) {
                            DsButton(text = "New", icon = Icons.Default.Add, kind = DsButtonKind.Secondary, compact = true, onClick = { newPlaylistName = "" ; createPlaylistOpen = true })
                        }
                    }
                    if (library.playlists.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                            Text(
                                "No playlists yet.",
                                color = sc.textMuted,
                                fontSize = DsType.Caption,
                                modifier = Modifier.weight(1f)
                            )
                            DsButton(text = "Create playlist", icon = Icons.Default.Add, compact = true, onClick = { newPlaylistName = "" ; createPlaylistOpen = true })
                        }
                    } else {
                        library.playlists.forEach { playlist ->
                            PlaylistCard(state, playlist, onRename = { renameTarget = it })
                        }
                    }
                }
            }
        }

        if (library.folders.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
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

    if (createPlaylistOpen) {
        DsPromptDialog(
            title = "Create playlist",
            placeholder = "Playlist name",
            initialValue = newPlaylistName,
            onConfirm = { name ->
                val id = library.createPlaylist(name)
                if (id.isNotBlank()) state.toastHost.show("Playlist \"${name.trim()}\" created", kind = ToastKind.Success)
                createPlaylistOpen = false
            },
            onDismiss = { createPlaylistOpen = false }
        )
    }
    renameTarget?.let { playlist ->
        DsPromptDialog(
            title = "Rename playlist",
            placeholder = "Playlist name",
            initialValue = playlist.name,
            onConfirm = { name ->
                if (library.renamePlaylist(playlist.id, name)) {
                    state.toastHost.show("Playlist renamed", kind = ToastKind.Success)
                } else {
                    state.toastHost.show("Name invalid or already in use", kind = ToastKind.Warning)
                }
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
}

/** One named playlist: play / queue / rename / delete, with its item list. */
@Composable
private fun PlaylistCard(
    state: AppState,
    playlist: ua.syt0r.kanji.desktop.engine.media.MediaPlaylist,
    onRename: (ua.syt0r.kanji.desktop.engine.media.MediaPlaylist) -> Unit
) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()
    val items = library.playlistItems(playlist)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accent().primary, modifier = Modifier.size(16.dp))
            Text(
                playlist.name,
                color = sc.textPrimary,
                fontSize = DsType.Body,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text("${items.size} item${if (items.size == 1) "" else "s"}", color = sc.textMuted, fontSize = DsType.Caption)
            DsButton(text = "Play", icon = Icons.Default.PlayArrow, compact = true, enabled = items.isNotEmpty(), onClick = { media.playPlaylist(playlist) })
            DsButton(text = "Queue", kind = DsButtonKind.Ghost, compact = true, enabled = items.isNotEmpty(), onClick = { media.queuePlaylist(playlist) })
            DsButton(text = "Rename", kind = DsButtonKind.Ghost, compact = true, onClick = { onRename(playlist) })
            DsButton(
                text = "Delete",
                kind = DsButtonKind.Ghost,
                compact = true,
                onClick = {
                    library.deletePlaylist(playlist.id)
                    state.toastHost.show("Playlist \"${playlist.name}\" deleted", kind = ToastKind.Info)
                }
            )
        }
        if (items.isEmpty()) {
            Text("Empty — add items from the library below", color = sc.textMuted, fontSize = DsType.Caption)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        Text("${index + 1}", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(22.dp))
                        Text(item.name, color = sc.textSecondary, fontSize = DsType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        DsIconButton(
                            icon = Icons.Default.SkipNext,
                            onClick = { media.playPlaylist(playlist, item.id) },
                            contentDescription = "Play from here",
                            size = 20.dp
                        )
                        DsIconButton(
                            icon = Icons.Default.Delete,
                            onClick = { library.removeFromPlaylist(playlist.id, item.id) },
                            contentDescription = "Remove from playlist",
                            size = 20.dp
                        )
                    }
                }
            }
        }
    }
}

/** Load a cached poster frame from disk onto the composition (never blocks). */
@Composable
internal fun ThumbnailImage(path: String, modifier: Modifier = Modifier) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { javax.imageio.ImageIO.read(java.io.File(path)).toComposeImageBitmap() }.getOrNull()
        }
    }
    bitmap?.let { Image(bitmap = it, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop) }
}

/** Ask the user where a moved file currently lives, then relink it. */
internal fun relinkItemDialog(state: AppState, item: MediaItem) {
    val chooser = javax.swing.JFileChooser().apply {
        dialogTitle = "Relink \"${item.name}\" — choose its current location"
        fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
    }
    if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
        state.media.relinkItem(item.id, chooser.selectedFile)
    }
}

// ============================================
// MEDIA TUNING PANEL — Video & Audio
// Display mode, aspect ratio, video adjustments,
// equalizer, audio delay / channel / output.
// Every control gates on the backend's real
// capabilities — nothing decorative.
// ============================================

@Composable
fun MediaTuningPanel(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    val caps = media.activeBackend?.capabilities ?: emptySet()

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = "Video & audio",
            subtitle = if (media.currentItem != null) "Tune ${media.currentItem!!.name}" else "Open media to adjust playback — every control is wired to the live backend"
        )

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Display", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    TuningSelect(
                        label = "Display mode",
                        selected = media.displayMode,
                        options = VideoDisplayMode.entries,
                        onSelected = { media.updateDisplayMode(it) },
                        labelOf = { "${it.label} — ${it.description}" },
                        modifier = Modifier.weight(1f)
                    )
                    TuningSelect(
                        label = "Aspect ratio",
                        selected = media.aspectRatio,
                        options = AspectRatioPreset.entries,
                        onSelected = { media.updateAspectRatio(it) },
                        labelOf = { it.label },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (PlaybackCapability.CanVideoAdjustments !in caps) {
                    Text(
                        "This backend (${media.backendKind.name}) does not expose video adjustments — the sliders below are hidden, not faked.",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
        }

        if (PlaybackCapability.CanVideoAdjustments in caps) {
            DsCard {
                Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Video adjustments", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        DsButton(
                            text = "Reset",
                            kind = DsButtonKind.Ghost,
                            compact = true,
                            enabled = !media.videoAdjustments.neutral(),
                            onClick = { media.resetVideoAdjustments() }
                        )
                    }
                    TuningSlider(label = "Brightness", value = media.videoAdjustments.brightness, range = 0f..200f, neutral = 100f, onValue = { media.updateVideoAdjustment { a -> a.withBrightness(it) } })
                    TuningSlider(label = "Contrast", value = media.videoAdjustments.contrast, range = 0f..200f, neutral = 100f, onValue = { media.updateVideoAdjustment { a -> a.withContrast(it) } })
                    TuningSlider(label = "Saturation", value = media.videoAdjustments.saturation, range = 0f..200f, neutral = 100f, onValue = { media.updateVideoAdjustment { a -> a.withSaturation(it) } })
                    TuningSlider(label = "Gamma", value = media.videoAdjustments.gamma, range = 0f..200f, neutral = 100f, onValue = { media.updateVideoAdjustment { a -> a.withGamma(it) } })
                    TuningSlider(label = "Hue", value = media.videoAdjustments.hue, range = -180f..180f, neutral = 0f, onValue = { media.updateVideoAdjustment { a -> a.withHue(it) } })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Deinterlace", color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                        DsToggle(
                            checked = media.videoAdjustments.deinterlace,
                            onCheckedChange = { media.updateVideoAdjustment { it.withDeinterlace(!it.deinterlace) } },
                            label = ""
                        )
                    }
                }
            }
        }

        if (PlaybackCapability.CanChangeSpeed in caps || media.speed != 1f) {
            DsCard {
                Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    Text("Playback", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                        Text("Speed", color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(110.dp))
                        Text(
                            "${media.speed}×",
                            color = accent().primary,
                            fontSize = DsType.Label,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(56.dp)
                        )
                        Slider(
                            value = media.speed,
                            onValueChange = { media.updateSpeed(it) },
                            valueRange = 0.25f..2f,
                            modifier = Modifier.weight(1f)
                        )
                        DsButton(text = "1×", kind = DsButtonKind.Ghost, compact = true, enabled = media.speed != 1f, onClick = { media.updateSpeed(1f) })
                    }
                }
            }
        }

        DsCard {
            Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Audio", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    TuningSelect(
                        label = "Channel preset",
                        selected = media.audioChannel,
                        options = AudioChannelPreset.entries,
                        onSelected = { media.updateAudioChannel(it) },
                        labelOf = { it.label },
                        modifier = Modifier.weight(1f)
                    )
                }
                TuningStepper(
                    label = "Audio delay",
                    value = media.audioDelayMs,
                    format = { "${it} ms" },
                    enabled = PlaybackCapability.CanAudioDelay in caps || media.audioDelayMs != 0L,
                    onDelta = { media.adjustAudioDelay(it) },
                    onReset = { media.updateAudioDelay(0) }
                )
            }
        }

        if (PlaybackCapability.CanEqualizer in caps) {
            DsCard {
                Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Equalizer", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        DsButton(text = "Off", kind = DsButtonKind.Ghost, compact = true, enabled = media.equalizer.active, onClick = { media.disableEqualizer() })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                        Text("Preset", color = sc.textSecondary, fontSize = DsType.Body)
                        DsSelect(
                            selected = media.equalizer.preset,
                            options = EqualizerPreset.entries,
                            onSelected = { media.setEqualizerPreset(it) },
                            labelOf = { it.label },
                            modifier = Modifier.width(220.dp)
                        )
                    }
                    TuningSlider(
                        label = "Preamp",
                        value = media.equalizer.preampDb,
                        range = -20f..20f,
                        neutral = 0f,
                        step = 0.5f,
                        format = { "${it} dB" },
                        onValue = { media.updateEqualizerPreamp(it) }
                    )
                    Text("Bands (${ua.syt0r.kanji.desktop.engine.playback.EQUALIZER_BAND_FREQUENCIES_HZ.first()} Hz – ${ua.syt0r.kanji.desktop.engine.playback.EQUALIZER_BAND_FREQUENCIES_HZ.last() / 1000} kHz)", color = sc.textSecondary, fontSize = DsType.Caption)
                    media.equalizer.normalizedBands().forEachIndexed { index, db ->
                        val hz = ua.syt0r.kanji.desktop.engine.playback.EQUALIZER_BAND_FREQUENCIES_HZ[index]
                        val label = if (hz >= 1000) "${(hz / 1000).toInt()} kHz" else "${hz.toInt()} Hz"
                        TuningSlider(
                            label = label,
                            value = db,
                            range = -20f..20f,
                            neutral = 0f,
                            step = 0.5f,
                            format = { "${it} dB" },
                            onValue = { media.updateEqualizerBand(index, it) }
                        )
                    }
                }
            }
        }
    }
}

/** A labeled enum selector used by the tuning panel. */
@Composable
private fun <T> TuningSelect(
    label: String,
    selected: T,
    options: List<T>,
    onSelected: (T) -> Unit,
    labelOf: (T) -> String,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = ua.syt0r.kanji.desktop.designsystem.surfaceColors().textSecondary, fontSize = DsType.Caption)
        DsSelect(
            selected = selected,
            options = options,
            onSelected = onSelected,
            labelOf = labelOf
        )
    }
}

/** A labeled slider with a neutral reset indicator. */
@Composable
internal fun TuningSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    neutral: Float,
    step: Float = 0f,
    format: (Float) -> String = { it.toInt().toString() },
    onValue: (Float) -> Unit
) {
    val sc = ua.syt0r.kanji.desktop.designsystem.surfaceColors()
    val ac = ua.syt0r.kanji.desktop.designsystem.accent()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        Text(label, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(110.dp))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValue,
            valueRange = range,
            steps = if (step > 0f) ((range.endInclusive - range.start) / step).toInt() - 1 else 0,
            modifier = Modifier.weight(1f)
        )
        Text(
            format(value),
            color = if (value == neutral) sc.textMuted else ac.primary,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(56.dp)
        )
    }
}

/** A − / + stepper with a reset (used for audio delay). */
@Composable
private fun TuningStepper(
    label: String,
    value: Long,
    format: (Long) -> String,
    enabled: Boolean,
    onDelta: (Long) -> Unit,
    onReset: () -> Unit
) {
    val sc = ua.syt0r.kanji.desktop.designsystem.surfaceColors()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text(label, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
        DsButton(text = "−50", kind = DsButtonKind.Ghost, compact = true, enabled = enabled, onClick = { onDelta(-50) })
        DsButton(text = "−500", kind = DsButtonKind.Ghost, compact = true, enabled = enabled, onClick = { onDelta(-500) })
        Text(format(value), color = ua.syt0r.kanji.desktop.designsystem.accent().primary, fontSize = DsType.Label, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(80.dp))
        DsButton(text = "+500", kind = DsButtonKind.Ghost, compact = true, enabled = enabled, onClick = { onDelta(500) })
        DsButton(text = "+50", kind = DsButtonKind.Ghost, compact = true, enabled = enabled, onClick = { onDelta(50) })
        if (value != 0L) {
            DsButton(text = "Reset", kind = DsButtonKind.Ghost, compact = true, onClick = onReset)
        }
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
                Text("Audio clip capture", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "When mining audio from a subtitle, pad the clip slightly before/after the cue and cap its length so a long line never produces a huge file.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
                var padBefore by remember { mutableStateOf(state.settings.getInt("media.audio-padding-before-ms", 200).toString()) }
                var padAfter by remember { mutableStateOf(state.settings.getInt("media.audio-padding-after-ms", 200).toString()) }
                var maxDuration by remember { mutableStateOf(state.settings.getInt("media.audio-max-duration-ms", 10000).toString()) }
                AudioClipSettingField(
                    label = "Padding before (ms)",
                    value = padBefore,
                    onValueChange = {
                        padBefore = it.filter { c -> c.isDigit() }.take(5)
                        padBefore.toLongOrNull()?.let { v -> state.settings.set("media.audio-padding-before-ms", v.coerceIn(0L, 5000L)) }
                    }
                )
                AudioClipSettingField(
                    label = "Padding after (ms)",
                    value = padAfter,
                    onValueChange = {
                        padAfter = it.filter { c -> c.isDigit() }.take(5)
                        padAfter.toLongOrNull()?.let { v -> state.settings.set("media.audio-padding-after-ms", v.coerceIn(0L, 5000L)) }
                    }
                )
                AudioClipSettingField(
                    label = "Max clip duration (ms, 0 = no cap)",
                    value = maxDuration,
                    onValueChange = {
                        maxDuration = it.filter { c -> c.isDigit() }.take(6)
                        maxDuration.toLongOrNull()?.let { v -> state.settings.set("media.audio-max-duration-ms", v.coerceIn(0L, 120000L)) }
                    }
                )
                Text(
                    "Applied the next time an audio clip is captured (subtitle menu → Capture audio clip, or mining with audio enabled).",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
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

/** Label + numeric field row for the audio-clip capture settings. */
@Composable
private fun AudioClipSettingField(label: String, value: String, onValueChange: (String) -> Unit) {
    val sc = surfaceColors()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text(label, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
        DsTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(120.dp)
        )
    }
}
