package ua.syt0r.kanji.desktop.ui.media

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.theme.tweenDuration
import java.io.File
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsChip
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsPromptDialog
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTextButton
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.errorColor
import ua.syt0r.kanji.desktop.designsystem.favoriteColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.media.MediaItem
import ua.syt0r.kanji.desktop.engine.media.MediaKind
import ua.syt0r.kanji.desktop.engine.media.MediaLibrary
import ua.syt0r.kanji.desktop.engine.media.MediaMiningEvent
import ua.syt0r.kanji.desktop.engine.media.MediaPlaylist
import ua.syt0r.kanji.desktop.engine.media.PlaylistFolder
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// MEDIA CENTRE HOME
// The immersion hub's front door. A real home:
// Continue Watching, Recently Added, Pinned,
// Watch Later, Collections, Playlists (with
// folders), Recently Mined and watched folders —
// laid out in an adaptive grid so desktop width
// is actually used. A Browse mode adds search,
// sorting, filters and folder navigation.
// ============================================

@Composable
fun MediaHomePanel(state: AppState, onOpenDetail: (String) -> Unit) {
    val media = state.media
    var browse by remember { mutableStateOf(false) }
    var subtitleSearch by remember { mutableStateOf(false) }
    var collectionFilter by remember { mutableStateOf<String?>(null) }
    var folderPath by remember { mutableStateOf<String?>(null) }

    // Typing in the toolbar search box opens Browse automatically.
    LaunchedEffect(media.librarySearchQuery) {
        if (media.librarySearchQuery.isNotBlank()) {
            browse = true
            subtitleSearch = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        HomeToolbar(
            state = state,
            browse = browse,
            subtitleSearch = subtitleSearch,
            onToggleBrowse = { browse = it },
            onToggleSubtitleSearch = { subtitleSearch = it }
        )
        if (subtitleSearch) {
            SubtitleSearchPanel(state = state)
        } else if (browse) {
            MediaBrowsePanel(
                state = state,
                onOpenDetail = onOpenDetail,
                collectionFilter = collectionFilter,
                onCollectionChange = { collectionFilter = it },
                folderPath = folderPath,
                onFolderPathChange = { folderPath = it }
            )
        } else {
            MediaHomeGrid(
                state = state,
                onOpenDetail = onOpenDetail,
                onBrowseCollection = {
                    collectionFilter = it
                    folderPath = null
                    browse = true
                    subtitleSearch = false
                },
                onBrowseFolder = {
                    folderPath = it
                    collectionFilter = null
                    browse = true
                    subtitleSearch = false
                }
            )
        }
    }
}

@Composable
private fun HomeToolbar(
    state: AppState,
    browse: Boolean,
    subtitleSearch: Boolean,
    onToggleBrowse: (Boolean) -> Unit,
    onToggleSubtitleSearch: (Boolean) -> Unit
) {
    val media = state.media
    val sc = surfaceColors()
    var urlPromptOpen by remember { mutableStateOf(false) }
    var createPlaylistOpen by remember { mutableStateOf(false) }

    val summary = media.statistics.summary()

    Column(
        Modifier.fillMaxWidth().padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        DsSectionHeader(
            title = "Media Centre",
            subtitle = "${media.library.items.size} items · ${media.formatDuration(media.library.totalWatchTimeMs())} watched · ${summary.totalMined} mined · ${summary.totalLookups} lookups",
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(text = "Add file", icon = Icons.Default.Add, compact = true, onClick = { chooseMediaFile(state) })
                    DsButton(text = "Add folder", icon = Icons.Default.FolderOpen, kind = DsButtonKind.Secondary, compact = true, onClick = { chooseMediaFolder(state) })
                    DsButton(text = "Open URL", icon = Icons.Default.Language, kind = DsButtonKind.Secondary, compact = true, onClick = { urlPromptOpen = true })
                    DsButton(text = "New playlist", icon = Icons.Default.PlayArrow, kind = DsButtonKind.Secondary, compact = true, onClick = { createPlaylistOpen = true })
                }
            }
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            DsChip(text = "Home", selected = !browse && !subtitleSearch, onClick = {
                onToggleBrowse(false)
                onToggleSubtitleSearch(false)
            })
            DsChip(text = "Browse", selected = browse, onClick = {
                onToggleBrowse(true)
                onToggleSubtitleSearch(false)
            })
            DsChip(text = "Subtitle search", selected = subtitleSearch, onClick = {
                onToggleSubtitleSearch(!subtitleSearch)
                if (subtitleSearch) onToggleBrowse(false)
            })
            Spacer(Modifier.width(DsSpacing.Sm))
            Text("Drop files or folders anywhere in Media to add them", color = sc.textMuted, fontSize = DsType.Caption)
        }
    }

    if (urlPromptOpen) {
        DsPromptDialog(
            title = "Open network media",
            placeholder = "https://… (video or audio URL)",
            onConfirm = { raw ->
                urlPromptOpen = false
                media.openUrl(raw)
            },
            onDismiss = { urlPromptOpen = false }
        )
    }
    if (createPlaylistOpen) {
        DsPromptDialog(
            title = "Create playlist",
            placeholder = "Playlist name",
            onConfirm = { name ->
                val id = media.library.createPlaylist(name)
                if (id.isNotBlank()) state.toastHost.show("Playlist \"${name.trim()}\" created", kind = ToastKind.Success)
                createPlaylistOpen = false
            },
            onDismiss = { createPlaylistOpen = false }
        )
    }
}

// ============================================
// HOME GRID — adaptive sections
// ============================================

@Composable
private fun MediaHomeGrid(
    state: AppState,
    onOpenDetail: (String) -> Unit,
    onBrowseCollection: (String) -> Unit,
    onBrowseFolder: (String) -> Unit
) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()

    if (library.items.isEmpty()) {
        // A useful empty state, not a blank page — the whole space invites the
        // first file instead of shrinking to a tiny centered card.
        Column(
            Modifier.fillMaxSize().padding(DsSpacing.Xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Movie,
                contentDescription = null,
                tint = accent().primary.copy(alpha = 0.45f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(DsSpacing.Md))
            Text("Your media library is empty", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(DsSpacing.Xs))
            Text(
                "Add a file, scan a folder or drop media anywhere in this workspace. Kaiteyo remembers position, subtitles, playlists and history for every item.",
                color = sc.textMuted,
                fontSize = DsType.Body,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.55f)
            )
            Spacer(Modifier.height(DsSpacing.Lg))
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(text = "Add file", icon = Icons.Default.Add, onClick = { chooseMediaFile(state) })
                DsButton(text = "Add folder", icon = Icons.Default.FolderOpen, kind = DsButtonKind.Secondary, onClick = { chooseMediaFolder(state) })
            }
        }
        return
    }

    val continueWatching = library.continueWatching(12)
    // Sections must be disjoint — the grid keys items by id and duplicate
    // keys crash LazyVerticalGrid. Show each item once, in its most useful
    // section (progress > pinned > watch later > newest).
    val favorites = library.favorites().filter { f -> continueWatching.none { it.id == f.id } }
    val watchLater = library.watchLater().filter { w ->
        continueWatching.none { it.id == w.id } && favorites.none { it.id == w.id }
    }
    // Japanese listening practice — audio items that aren't already surfaced in
    // a more specific section (progress > pinned > watch later).
    val listening = library.items.filter { it.kind == MediaKind.Audio }.filter { audio ->
        continueWatching.none { it.id == audio.id } &&
            favorites.none { it.id == audio.id } &&
            watchLater.none { it.id == audio.id }
    }
    val recent = library.recentlyAdded(12).filter { item ->
        continueWatching.none { it.id == item.id } &&
            favorites.none { it.id == item.id } &&
            watchLater.none { it.id == item.id } &&
            listening.none { it.id == item.id }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(176.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(DsSpacing.Lg),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        if (continueWatching.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Continue watching", "${continueWatching.size} in progress — tap a card to resume")
            }
            gridItems(continueWatching, key = { it.id }) { item ->
                MediaItemCard(state, item, showProgress = true, onOpenDetail = onOpenDetail)
            }
        }

        if (favorites.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Pinned media", "Your favorites, always one click away")
            }
            gridItems(favorites, key = { it.id }) { item ->
                MediaItemCard(state, item, showProgress = true, onOpenDetail = onOpenDetail)
            }
        }

        if (watchLater.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Watch later", "Tagged from the detail view or any card")
            }
            gridItems(watchLater, key = { it.id }) { item ->
                MediaItemCard(state, item, showProgress = false, onOpenDetail = onOpenDetail)
            }
        }

        val collections = library.allCollections.filter { c -> library.byCollection(c).isNotEmpty() }
        if (collections.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Collections", "Anime · Movies · TV · Music — browse one category")
            }
            gridItems(collections, key = { it }) { collection ->
                CollectionTile(collection, library.byCollection(collection).size, onClick = { onBrowseCollection(collection) })
            }
        }

        if (listening.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Japanese listening", "Audiobooks, podcasts and music — train your ear")
            }
            gridItems(listening, key = { it.id }) { item ->
                MediaItemCard(state, item, showProgress = false, onOpenDetail = onOpenDetail)
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle("Recently added", "The newest arrivals in your library")
        }
        gridItems(recent, key = { it.id }) { item ->
            MediaItemCard(state, item, showProgress = false, onOpenDetail = onOpenDetail)
        }

        if (library.playlists.isNotEmpty() || library.playlistFolders.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Playlists", "Persistent, folder-organized queues")
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                PlaylistsLibraryCard(state)
            }
        }

        val recentMined = media.miningEvents.take(8)
        if (recentMined.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Recently mined", "Cards pulled from your media")
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                RecentlyMinedCard(state, recentMined)
            }
        }

        val watchedFolders = library.existingWatchedFolders()
        if (watchedFolders.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Watched folders", "Auto-scanned media roots")
            }
            gridItems(watchedFolders, key = { it }) { folder ->
                FolderTile(folder, onClick = { onBrowseFolder(folder) })
            }
        }
    }
}

@Composable
internal fun SectionTitle(title: String, subtitle: String) {
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
        if (subtitle.isNotBlank()) {
            Text(subtitle, color = sc.textMuted, fontSize = DsType.Caption)
        }
    }
}

/**
 * Grid tile: the media art is the hero. 16:9 poster frame with progress
 * overlaid on the art, a play overlay that fades in on hover, and a quiet
 * favorite toggle. Metadata stays secondary — no boxes inside boxes. Hover
 * reveals a compact More menu (playlists, watch later, reveal, remove).
 */
@Composable
internal fun MediaItemCard(
    state: AppState,
    item: MediaItem,
    showProgress: Boolean,
    onOpenDetail: (String) -> Unit
) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()
    val ac = accent()
    val missing = remember(item.id, item.path) { !media.library.fileExists(item) }
    val thumbPath = media.scanner.thumbnailState[item.id]
    LaunchedEffect(item.id) { media.scanner.requestThumbnail(item) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val duration = tweenDuration(LocalAnimationConfig.current, 180)
    var moreMenuOpen by remember { mutableStateOf(false) }
    var removeConfirm by remember(item.id) { mutableStateOf(false) }
    // Hover reveals a play overlay without resizing anything — the art never
    // jumps, so neighboring cards stay put.
    val playAlpha by animateFloatAsState(if (hovered) 1f else 0f, tween(duration), label = "cardPlayAlpha")
    val playScale by animateFloatAsState(if (hovered) 1f else 0.85f, tween(duration), label = "cardPlayScale")
    val moreAlpha by animateFloatAsState(if (hovered) 1f else 0f, tween(duration), label = "cardMoreAlpha")

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Lg))
            .background(sc.surfaceElevated.copy(alpha = 0.5f))
            .clickable(interactionSource = interaction, indication = null, onClick = { onOpenDetail(item.id) })
            .hoverable(interaction)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(ac.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbPath != null) {
                ThumbnailImage(thumbPath, Modifier.matchParentSize())
            } else {
                Icon(
                    if (item.kind == MediaKind.Video) Icons.Default.Movie else Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = if (missing) errorColor() else ac.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            // Progress sits on the art's bottom edge — obvious but subtle.
            if (showProgress && item.durationMs > 0 && item.progressFraction in 0.01f..0.99f) {
                DsProgressBar(
                    fraction = item.progressFraction,
                    height = 3.dp,
                    color = ac.primary,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                )
            }
            // Hover play — resumes when applicable.
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.55f * playAlpha))
                    .graphicsLayer {
                        alpha = playAlpha
                        scaleX = playScale
                        scaleY = playScale
                    }
                    .clickable { media.openItem(item) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play / Resume", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            // Hover More menu — secondary actions stay out of the way until
            // wanted, then fade in like the play overlay.
            Box(Modifier.align(Alignment.TopStart).padding(DsSpacing.Xs)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More actions",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .graphicsLayer { alpha = moreAlpha }
                        .clickable(enabled = hovered) { moreMenuOpen = true }
                        .padding(3.dp)
                )
                DropdownMenu(expanded = moreMenuOpen, onDismissRequest = { moreMenuOpen = false }) {
                    if (library.playlists.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No playlists — create one on the home screen") },
                            onClick = { moreMenuOpen = false }
                        )
                    } else {
                        library.playlists.forEach { playlist ->
                            val inPlaylist = item.id in playlist.itemIds
                            DropdownMenuItem(
                                text = { Text((if (inPlaylist) "✓ " else "") + playlist.name) },
                                onClick = {
                                    if (inPlaylist) library.removeFromPlaylist(playlist.id, item.id)
                                    else {
                                        library.addToPlaylist(playlist.id, item.id)
                                        state.toastHost.show("Added to \"${playlist.name}\"", kind = ToastKind.Success)
                                    }
                                    moreMenuOpen = false
                                }
                            )
                        }
                    }
                    DropdownMenuItem(
                        text = { Text("Watch later") },
                        onClick = {
                            moreMenuOpen = false
                            media.toggleWatchLater(item.id)
                        }
                    )
                    if (missing) {
                        DropdownMenuItem(
                            text = { Text("Relink…") },
                            onClick = {
                                moreMenuOpen = false
                                relinkItemDialog(state, item)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Reveal in folder") },
                            onClick = {
                                moreMenuOpen = false
                                runCatching {
                                    java.awt.Desktop.getDesktop().open(File(item.path).parentFile ?: File(item.path))
                                }
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Remove from library") },
                        onClick = {
                            moreMenuOpen = false
                            removeConfirm = true
                        }
                    )
                }
            }
            // Favorite toggle — quiet until favorited, never covering the art.
            Icon(
                if (item.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Favorite",
                tint = if (item.favorite) favoriteColor() else Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(DsSpacing.Xs)
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { media.library.toggleFavorite(item.id) }
                    .padding(3.dp)
            )
        }
        Column(
            Modifier.padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                item.name,
                color = sc.textPrimary,
                fontSize = DsType.Body,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.episode.isNotBlank()) {
                    DsBadge(text = item.episode, tint = ac.primary)
                }
                if (missing) {
                    DsBadge(text = "Missing", tint = errorColor())
                }
                if (item.durationMs > 0) {
                    Text(
                        "${MediaEngine.formatTime(item.durationMs)}${if (item.completed) " · done" else ""}",
                        color = sc.textMuted,
                        fontSize = DsType.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }

    if (removeConfirm) {
        DsConfirmDialog(
            title = "Remove from library",
            message = "Remove \"${item.name}\"? Files stay on disk; watch history is kept.",
            confirmText = "Remove",
            danger = true,
            onConfirm = {
                library.removeItem(item.id, forgetHistory = false)
                removeConfirm = false
            },
            onDismiss = { removeConfirm = false }
        )
    }
}

@Composable
private fun CollectionTile(collection: String, count: Int, onClick: () -> Unit) {
    val ac = accent()
    val sc = surfaceColors()
    DsCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(DsSpacing.Lg),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
        ) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(DsRadius.Md)).background(ac.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = ac.primary, modifier = Modifier.size(20.dp))
            }
            Text(collection, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$count item${if (count == 1) "" else "s"}", color = sc.textMuted, fontSize = DsType.Caption)
        }
    }
}

@Composable
private fun FolderTile(folderPath: String, onClick: () -> Unit) {
    val ac = accent()
    val sc = surfaceColors()
    DsCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(DsSpacing.Lg),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
        ) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(DsRadius.Md)).background(ac.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = ac.primary, modifier = Modifier.size(20.dp))
            }
            Text(File(folderPath).name.ifBlank { folderPath }, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(folderPath, color = sc.textMuted, fontSize = DsType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Recently mined events: which card came from which media, with a jump-back link. */
@Composable
private fun RecentlyMinedCard(state: AppState, events: List<MediaMiningEvent>) {
    val sc = surfaceColors()
    val ac = accent()
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            events.forEach { event ->
                val card = state.cards.firstOrNull { it.id == event.cardId }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            card?.character?.takeIf { it.isNotBlank() } ?: event.cueText,
                            color = sc.textPrimary,
                            fontSize = DsType.Body,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${event.mediaName} · ${MediaEngine.formatTime(event.timestampMs)}",
                            color = sc.textMuted,
                            fontSize = DsType.Caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DsBadge(text = "mined", tint = ac.primary)
                    DsTextButton(
                        text = "Open in Media",
                        onClick = {
                            if (card != null) state.media.openFromCard(card)
                            else state.media.library.itemByPath(event.mediaPath)?.let { item ->
                                state.media.openItemAt(item, event.timestampMs)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ============================================
// BROWSE — search, sort, filter, folder navigation
// ============================================

@Composable
private fun MediaBrowsePanel(
    state: AppState,
    onOpenDetail: (String) -> Unit,
    collectionFilter: String?,
    onCollectionChange: (String?) -> Unit,
    folderPath: String?,
    onFolderPathChange: (String?) -> Unit
) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()
    var sortMode by remember { mutableStateOf(MediaLibrary.MediaSortMode.Title) }
    var watchFilter by remember { mutableStateOf(MediaLibrary.MediaWatchFilter.Any) }
    var kindFilter by remember { mutableStateOf<MediaKind?>(null) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var listMode by remember { mutableStateOf(false) }
    val query = media.librarySearchQuery

    val baseItems = folderPath?.let { library.itemsDirectlyUnder(it) } ?: library.items.toList()
    val filtered = remember(query, baseItems, sortMode, watchFilter, kindFilter, tagFilter, collectionFilter, library.items.size) {
        var list = baseItems
        if (query.isNotBlank()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) || it.path.contains(query, ignoreCase = true) ||
                    it.collection.contains(query, ignoreCase = true) ||
                    it.tags.any { t -> t.contains(query, ignoreCase = true) }
            }
        }
        if (collectionFilter != null) list = list.filter { it.collection == collectionFilter }
        kindFilter?.let { k -> list = list.filter { it.kind == k } }
        tagFilter?.let { t -> list = list.filter { t in it.tags } }
        list = list.filter { library.matchesWatchFilter(it, watchFilter) }
        library.sortItems(list, sortMode)
    }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        BrowseFolderBar(state, folderPath, onFolderPathChange)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSelect(
                selected = sortMode,
                options = MediaLibrary.MediaSortMode.entries.toList(),
                onSelected = { sortMode = it },
                labelOf = { it.label },
                modifier = Modifier.width(160.dp)
            )
            DsSelect(
                selected = watchFilter,
                options = MediaLibrary.MediaWatchFilter.entries.toList(),
                onSelected = { watchFilter = it },
                labelOf = { it.label },
                modifier = Modifier.width(140.dp)
            )
            MediaKind.entries.forEach { kind ->
                DsChip(text = kind.name, selected = kindFilter == kind, onClick = { kindFilter = if (kindFilter == kind) null else kind })
            }
            if (library.allTags().isNotEmpty()) {
                DsSelect(
                    selected = tagFilter ?: "",
                    options = listOf("") + library.allTags(),
                    onSelected = { tagFilter = it.ifBlank { null } },
                    labelOf = { if (it.isBlank()) "All tags" else it },
                    modifier = Modifier.width(140.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text("${filtered.size} item${if (filtered.size == 1) "" else "s"}", color = sc.textMuted, fontSize = DsType.Caption)
            DsIconButton(icon = Icons.Default.GridView, onClick = { listMode = false }, contentDescription = "Grid view", tint = if (!listMode) accent().primary else null, size = 30.dp)
            DsIconButton(icon = Icons.Default.ViewList, onClick = { listMode = true }, contentDescription = "List view", tint = if (listMode) accent().primary else null, size = 30.dp)
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No media matches — adjust the search or filters", color = sc.textMuted, fontSize = DsType.Body)
            }
        } else if (listMode) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    MediaItemListRow(state, item, onOpenDetail = onOpenDetail)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(176.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                gridItems(filtered, key = { it.id }) { item ->
                    MediaItemCard(state, item, showProgress = true, onOpenDetail = onOpenDetail)
                }
            }
        }
    }
}

/** Folder breadcrumb + subfolder tiles for the current browse level. */
@Composable
private fun BrowseFolderBar(state: AppState, folderPath: String?, onFolderPathChange: (String?) -> Unit) {
    val library = state.media.library
    val sc = surfaceColors()
    val ac = accent()

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DsChip(
                text = "All media",
                selected = folderPath == null,
                onClick = { onFolderPathChange(null) }
            )
            if (folderPath != null) {
                Text("›", color = sc.textMuted, fontSize = DsType.Caption)
                Text(library.folderName(folderPath), color = ac.primary, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                DsTextButton(text = "Up", onClick = {
                    val parent = File(folderPath).parentFile?.absolutePath
                    onFolderPathChange(parent)
                })
            }
        }
        if (folderPath != null) {
            val subfolders = library.subfoldersUnder(folderPath)
            if (subfolders.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                    subfolders.forEach { sub ->
                        DsChip(text = library.folderName(sub), selected = false, onClick = { onFolderPathChange(sub) })
                    }
                }
            }
        } else {
            val roots = library.existingWatchedFolders()
            if (roots.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                    roots.forEach { root ->
                        DsChip(text = library.folderName(root), selected = false, onClick = { onFolderPathChange(root) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaItemListRow(state: AppState, item: MediaItem, onOpenDetail: (String) -> Unit) {
    val media = state.media
    val sc = surfaceColors()
    val missing = remember(item.id, item.path) { !media.library.fileExists(item) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(sc.surfaceElevated.copy(alpha = 0.5f))
            .clickable { onOpenDetail(item.id) }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Icon(
            if (item.kind == MediaKind.Video) Icons.Default.Movie else Icons.Default.AudioFile,
            contentDescription = null,
            tint = if (missing) errorColor() else accent().primary,
            modifier = Modifier.size(18.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(item.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    if (item.durationMs > 0) append(MediaEngine.formatTime(item.durationMs))
                    if (item.collection.isNotBlank()) append(" · ").append(item.collection)
                    if (item.episode.isNotBlank()) append(" · ").append(item.episode)
                    if (item.lastWatchedAt.isNotBlank()) append(" · last watched ${MediaEngine.formatTime(item.lastPositionMs)}")
                },
                color = sc.textMuted,
                fontSize = DsType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.durationMs > 0 && item.progressFraction in 0.01f..0.99f) {
            DsProgressBar(fraction = item.progressFraction, height = 3.dp, modifier = Modifier.width(70.dp))
        }
        DsIconButton(icon = Icons.Default.PlayArrow, onClick = { media.openItem(item) }, contentDescription = "Play", size = 28.dp)
        DsIconButton(icon = Icons.Default.SkipNext, onClick = { media.addToQueue(item) }, contentDescription = "Add to queue", size = 28.dp)
        Icon(
            if (item.favorite) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = "Favorite",
            tint = if (item.favorite) favoriteColor() else sc.textMuted,
            modifier = Modifier.size(18.dp).clickable { media.library.toggleFavorite(item.id) }
        )
    }
}

// ============================================
// PLAYLISTS — folder tree, favorite, duplicate, shuffle
// ============================================

@Composable
private fun PlaylistsLibraryCard(state: AppState) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()
    var createFolderOpen by remember { mutableStateOf(false) }
    var createPlaylistOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<MediaPlaylist?>(null) }
    var moveTarget by remember { mutableStateOf<MediaPlaylist?>(null) }

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Playlists", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Persistent queues in folders — play, shuffle, favorite or duplicate",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                DsButton(text = "New folder", icon = Icons.Default.CreateNewFolder, kind = DsButtonKind.Secondary, compact = true, onClick = { createFolderOpen = true })
                DsButton(text = "New playlist", icon = Icons.Default.Add, kind = DsButtonKind.Secondary, compact = true, onClick = { createPlaylistOpen = true })
            }

            // Root-level playlists first, then folders (folders contain playlists + subfolders).
            library.playlistsInFolder("").forEach { playlist ->
                PlaylistRow(state, playlist, onRename = { renameTarget = it }, onMove = { moveTarget = it })
            }
            library.folderChildren("").forEach { folder ->
                PlaylistFolderNode(state, folder, depth = 0, onRename = { renameTarget = it }, onMove = { moveTarget = it })
            }
            if (library.playlists.isEmpty() && library.playlistFolders.isEmpty()) {
                Text("No playlists yet — create one above or from any media card's menu.", color = sc.textMuted, fontSize = DsType.Caption)
            }
        }
    }

    if (createFolderOpen) {
        DsPromptDialog(
            title = "New playlist folder",
            placeholder = "Folder name",
            onConfirm = { name ->
                if (library.createPlaylistFolder(name).isNotBlank()) {
                    state.toastHost.show("Playlist folder \"${name.trim()}\" created", kind = ToastKind.Success)
                }
                createFolderOpen = false
            },
            onDismiss = { createFolderOpen = false }
        )
    }
    if (createPlaylistOpen) {
        DsPromptDialog(
            title = "Create playlist",
            placeholder = "Playlist name",
            onConfirm = { name ->
                if (library.createPlaylist(name).isNotBlank()) {
                    state.toastHost.show("Playlist \"${name.trim()}\" created", kind = ToastKind.Success)
                }
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
    moveTarget?.let { playlist ->
        MovePlaylistDialog(state, playlist) { moveTarget = null }
    }
}

@Composable
private fun PlaylistRow(
    state: AppState,
    playlist: MediaPlaylist,
    onRename: (MediaPlaylist) -> Unit,
    onMove: (MediaPlaylist) -> Unit
) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()
    val ac = accent()
    val items = library.playlistItems(playlist)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            if (playlist.favorite) {
                Icon(Icons.Default.Star, contentDescription = null, tint = favoriteColor(), modifier = Modifier.size(14.dp))
            }
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ac.primary, modifier = Modifier.size(14.dp))
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
            DsButton(text = "Shuffle", icon = Icons.Default.Shuffle, kind = DsButtonKind.Ghost, compact = true, enabled = items.size > 1, onClick = { media.playShuffled(playlist) })
            DsIconButton(
                icon = if (playlist.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                onClick = { library.togglePlaylistFavorite(playlist.id) },
                contentDescription = "Favorite playlist",
                tint = if (playlist.favorite) favoriteColor() else null,
                size = 26.dp
            )
            PlaylistMenu(state, playlist, onRename = onRename, onMove = onMove)
        }
        if (items.isNotEmpty()) {
            items.forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth().padding(start = DsSpacing.Lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${index + 1}. ${item.name}",
                            color = sc.textSecondary,
                            fontSize = DsType.Body,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DsIconButton(icon = Icons.Default.KeyboardArrowUp, onClick = {
                        library.reorderPlaylist(playlist.id, item.id, index - 1)
                    }, contentDescription = "Move up", size = 20.dp)
                    DsIconButton(icon = Icons.Default.KeyboardArrowDown, onClick = {
                        library.reorderPlaylist(playlist.id, item.id, index + 1)
                    }, contentDescription = "Move down", size = 20.dp)
                    DsIconButton(icon = Icons.Default.SkipNext, onClick = { media.playPlaylist(playlist, item.id) }, contentDescription = "Play from here", size = 20.dp)
                    DsIconButton(icon = Icons.Default.Delete, onClick = { library.removeFromPlaylist(playlist.id, item.id) }, contentDescription = "Remove from playlist", size = 20.dp)
                }
            }
        }
    }
}

@Composable
private fun PlaylistMenu(
    state: AppState,
    playlist: MediaPlaylist,
    onRename: (MediaPlaylist) -> Unit,
    onMove: (MediaPlaylist) -> Unit
) {
    val library = state.media.library
    var open by remember { mutableStateOf(false) }
    Box {
        DsIconButton(icon = Icons.Default.MoreVert, onClick = { open = true }, contentDescription = "Playlist actions", size = 26.dp)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Duplicate playlist") },
                onClick = {
                    open = false
                    library.duplicatePlaylist(playlist.id)?.let {
                        state.toastHost.show("Playlist duplicated", kind = ToastKind.Success)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    open = false
                    onRename(playlist)
                }
            )
            DropdownMenuItem(
                text = { Text("Move to folder…") },
                onClick = {
                    open = false
                    onMove(playlist)
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    open = false
                    library.deletePlaylist(playlist.id)
                    state.toastHost.show("Playlist \"${playlist.name}\" deleted", kind = ToastKind.Info)
                }
            )
        }
    }
}

/** Move a playlist into any folder (or back to the library root). */
@Composable
private fun MovePlaylistDialog(state: AppState, playlist: MediaPlaylist, onDismiss: () -> Unit) {
    val library = state.media.library
    val sc = surfaceColors()
    val ac = accent()
    DsDialog(title = "Move \"${playlist.name}\"", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                    library.movePlaylistToFolder(playlist.id, "")
                    onDismiss()
                }.padding(DsSpacing.Md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (playlist.folderId.isBlank()) "✓ " else "",
                    color = ac.primary,
                    fontSize = DsType.Body
                )
                Text("Library root", color = if (playlist.folderId.isBlank()) ac.primary else sc.textPrimary, fontSize = DsType.Body)
            }
            library.playlistFolders.forEach { folder ->
                val chain = library.folderChain(folder.id)
                val indent = chain.size - 1
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                        library.movePlaylistToFolder(playlist.id, folder.id)
                        onDismiss()
                    }.padding(start = DsSpacing.Md + (indent * 14).dp, end = DsSpacing.Md, top = DsSpacing.Sm, bottom = DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (playlist.folderId == folder.id) "✓ " else "",
                        color = ac.primary,
                        fontSize = DsType.Body
                    )
                    Text(folder.name, color = if (playlist.folderId == folder.id) ac.primary else sc.textPrimary, fontSize = DsType.Body)
                }
            }
        }
    }
}

/** One playlist folder node in the tree: its playlists + child folders, collapsible. */
@Composable
private fun PlaylistFolderNode(
    state: AppState,
    folder: PlaylistFolder,
    depth: Int,
    onRename: (MediaPlaylist) -> Unit,
    onMove: (MediaPlaylist) -> Unit
) {
    val library = state.media.library
    val sc = surfaceColors()
    var expanded by remember(folder.id) { mutableStateOf(true) }
    var folderMenuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var newPlaylistOpen by remember { mutableStateOf(false) }
    var newSubfolderOpen by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(start = (depth * 16).dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { expanded = !expanded }
                .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            Text(if (expanded) "▾" else "▸", color = sc.textMuted, fontSize = DsType.Caption)
            Icon(Icons.Default.Folder, contentDescription = null, tint = accent().primary, modifier = Modifier.size(16.dp))
            Text(folder.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "${library.playlistsInFolder(folder.id).size} playlist${if (library.playlistsInFolder(folder.id).size == 1) "" else "s"}",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
            Box {
                DsIconButton(icon = Icons.Default.MoreVert, onClick = { folderMenuOpen = true }, contentDescription = "Folder actions", size = 22.dp)
                DropdownMenu(expanded = folderMenuOpen, onDismissRequest = { folderMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename folder") },
                        onClick = {
                            folderMenuOpen = false
                            renameOpen = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New playlist here") },
                        onClick = {
                            folderMenuOpen = false
                            newPlaylistOpen = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New subfolder") },
                        onClick = {
                            folderMenuOpen = false
                            newSubfolderOpen = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete folder") },
                        onClick = {
                            folderMenuOpen = false
                            deleteConfirm = true
                        }
                    )
                }
            }
        }
        if (expanded) {
            library.playlistsInFolder(folder.id).forEach { playlist ->
                Row(Modifier.fillMaxWidth().padding(start = DsSpacing.Md)) {
                    PlaylistRow(state, playlist, onRename = onRename, onMove = onMove)
                }
            }
            library.folderChildren(folder.id).forEach { child ->
                PlaylistFolderNode(state, child, depth + 1, onRename = onRename, onMove = onMove)
            }
        }
    }

    if (renameOpen) {
        DsPromptDialog(
            title = "Rename folder",
            placeholder = "Folder name",
            initialValue = folder.name,
            onConfirm = { name ->
                if (library.renamePlaylistFolder(folder.id, name)) {
                    state.toastHost.show("Folder renamed", kind = ToastKind.Success)
                }
                renameOpen = false
            },
            onDismiss = { renameOpen = false }
        )
    }
    if (newPlaylistOpen) {
        DsPromptDialog(
            title = "New playlist in \"${folder.name}\"",
            placeholder = "Playlist name",
            onConfirm = { name ->
                if (library.createPlaylist(name, folder.id).isNotBlank()) {
                    state.toastHost.show("Playlist created in \"${folder.name}\"", kind = ToastKind.Success)
                }
                newPlaylistOpen = false
            },
            onDismiss = { newPlaylistOpen = false }
        )
    }
    if (newSubfolderOpen) {
        DsPromptDialog(
            title = "New subfolder in \"${folder.name}\"",
            placeholder = "Folder name",
            onConfirm = { name ->
                if (library.createPlaylistFolder(name, folder.id).isNotBlank()) {
                    state.toastHost.show("Subfolder created", kind = ToastKind.Success)
                }
                newSubfolderOpen = false
            },
            onDismiss = { newSubfolderOpen = false }
        )
    }
    if (deleteConfirm) {
        DsConfirmDialog(
            title = "Delete folder",
            message = "Delete \"${folder.name}\"? Its playlists move to the parent folder — nothing is lost.",
            confirmText = "Delete",
            danger = true,
            onConfirm = {
                library.deletePlaylistFolder(folder.id)
                state.toastHost.show("Folder deleted", kind = ToastKind.Info)
            },
            onDismiss = { deleteConfirm = false }
        )
    }
}

// ============================================
// MEDIA DETAIL — full item page
// ============================================

@Composable
fun MediaDetailPanel(state: AppState, itemId: String, onBack: () -> Unit) {
    val item = state.media.library.item(itemId) ?: return
    val missing = !state.media.library.fileExists(item)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DsSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        // Header + progress span the full width; the detail cards below split
        // into two balanced columns on wide windows so the space is actually
        // used instead of a single narrow stack.
        DetailHeader(state, item, missing, onBack)
        DetailProgress(state, item, missing)

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val content: @Composable () -> Unit = {
                DetailMetadata(state, item)
                DetailOrganize(state, item)
                SubtitleAssociationCard(state, item)
                DetailPlaylists(state, item)
                DetailWatchHistory(state, item)
                DetailMined(state, item)
                DetailBookmarksAndClips(state, item)
                DetailManage(state, item, missing, onBack)
            }
            if (maxWidth >= 1100.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
                        DetailMetadata(state, item)
                        DetailOrganize(state, item)
                        SubtitleAssociationCard(state, item)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
                        DetailPlaylists(state, item)
                        DetailWatchHistory(state, item)
                        DetailMined(state, item)
                        DetailBookmarksAndClips(state, item)
                        DetailManage(state, item, missing, onBack)
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(state: AppState, item: MediaItem, missing: Boolean, onBack: () -> Unit) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        DsIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack, contentDescription = "Back")
        Column(Modifier.weight(1f)) {
            Text(item.name, color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    append(item.kind.name)
                    if (item.collection.isNotBlank()) append(" · ").append(item.collection)
                    if (item.episode.isNotBlank()) append(" · ").append(item.episode)
                    if (missing) append(" · ").append("file unavailable")
                },
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
        DsIconButton(
            icon = if (item.favorite) Icons.Default.Star else Icons.Default.StarBorder,
            onClick = { library.toggleFavorite(item.id) },
            contentDescription = "Favorite",
            tint = if (item.favorite) favoriteColor() else null
        )
        DsIconButton(
            icon = Icons.Default.WatchLater,
            onClick = { media.toggleWatchLater(item.id) },
            contentDescription = "Watch later",
            tint = if (library.isWatchLater(item.id)) accent().primary else null
        )
        DsButton(text = if (item.lastPositionMs > 5000 && !item.completed) "Resume" else "Play", icon = Icons.Default.PlayArrow, onClick = { media.openItem(item) })
        if (!missing) {
            DsButton(text = "From start", kind = DsButtonKind.Secondary, compact = true, onClick = { media.openItem(item, resume = false) })
        }
    }
}

@Composable
private fun DetailProgress(state: AppState, item: MediaItem, missing: Boolean) {
    val media = state.media
    val sc = surfaceColors()
    if (item.durationMs <= 0) return
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (missing) "File unavailable — relink below to restore this item"
                    else "${(item.progressFraction * 100).toInt()}% watched",
                    color = sc.textPrimary,
                    fontSize = DsType.BodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (!missing && item.progressFraction in 0.01f..0.99f) {
                    DsTextButton(text = "Open at ${MediaEngine.formatTime(item.lastPositionMs)}", onClick = { media.openItem(item) })
                }
            }
            DsProgressBar(fraction = item.progressFraction)
            Text(
                "${MediaEngine.formatTime(item.lastPositionMs)} / ${MediaEngine.formatTime(item.durationMs)} · ${item.watchCount} watch${if (item.watchCount == 1) "" else "es"}${if (item.completed) " · completed" else ""}",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

@Composable
private fun DetailMetadata(state: AppState, item: MediaItem) {
    val sc = surfaceColors()
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            Text("Metadata", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            DetailRow("Format", item.kind.name + (item.name.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""))
            DetailRow("Duration", if (item.durationMs > 0) MediaEngine.formatTime(item.durationMs) else "Unknown")
            DetailRow("Size", if (item.sizeBytes > 0) humanSize(item.sizeBytes) else "—")
            DetailRow("Path", item.path)
            DetailRow("Added", item.addedAt.ifBlank { "—" })
            DetailRow("Last watched", item.lastWatchedAt.ifBlank { "Never" })
        }
    }
}

@Composable
private fun DetailOrganize(state: AppState, item: MediaItem) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()
    var newCollectionOpen by remember { mutableStateOf(false) }
    var tagsDraft by remember(item.id) { mutableStateOf(item.tags.joinToString(", ")) }
    var noteDraft by remember(item.id) { mutableStateOf(item.note) }

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Text("Organize", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Column(Modifier.weight(1f)) {
                    Text("Collection", color = sc.textSecondary, fontSize = DsType.Caption)
                    DsSelect(
                        selected = item.collection,
                        options = (library.allCollections + item.collection).distinct().filter { it.isNotBlank() },
                        onSelected = { library.setCollection(item.id, it) },
                        labelOf = { if (it.isBlank()) "None" else it }
                    )
                }
                DsButton(text = "New", kind = DsButtonKind.Ghost, compact = true, onClick = { newCollectionOpen = true })
            }
            DsTextField(
                value = tagsDraft,
                onValueChange = {
                    tagsDraft = it
                    library.setTags(item.id, it.split(',').map { t -> t.trim() }.filter { t -> t.isNotEmpty() })
                },
                placeholder = "Tags (comma separated) — e.g. N4, slice-of-life, listening",
                label = "Tags"
            )
            DsTextField(
                value = noteDraft,
                onValueChange = {
                    noteDraft = it
                    library.setNote(item.id, it)
                },
                placeholder = "Notes about this media",
                label = "Notes",
                singleLine = false
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Column(Modifier.weight(1f)) {
                    Text("Comprehension", color = sc.textSecondary, fontSize = DsType.Caption)
                    DsSelect(
                        selected = item.comprehension,
                        options = listOf(0, 1, 2, 3, 4, 5),
                        onSelected = { library.setComprehension(item.id, it) },
                        labelOf = { if (it == 0) "Unset" else "$it / 5" }
                    )
                }
                DsTextButton(text = "Watch later", onClick = { media.toggleWatchLater(item.id) })
            }
        }
    }

    if (newCollectionOpen) {
        DsPromptDialog(
            title = "New collection",
            placeholder = "Collection name",
            onConfirm = { name ->
                val clean = name.trim()
                if (clean.isNotBlank()) library.setCollection(item.id, clean)
                newCollectionOpen = false
            },
            onDismiss = { newCollectionOpen = false }
        )
    }
}

@Composable
private fun DetailPlaylists(state: AppState, item: MediaItem) {
    val library = state.media.library
    val sc = surfaceColors()
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text("Playlists", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            if (library.playlists.isEmpty()) {
                Text("No playlists yet — create one in the Media Centre home.", color = sc.textMuted, fontSize = DsType.Caption)
            } else {
                library.playlists.forEach { playlist ->
                    val inPlaylist = item.id in playlist.itemIds
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        Icon(
                            if (inPlaylist) Icons.Default.Check else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (inPlaylist) accent().primary else sc.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(playlist.name, color = sc.textPrimary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                        DsTextButton(
                            text = if (inPlaylist) "Remove" else "Add",
                            onClick = {
                                if (inPlaylist) library.removeFromPlaylist(playlist.id, item.id)
                                else {
                                    library.addToPlaylist(playlist.id, item.id)
                                    state.toastHost.show("Added to \"${playlist.name}\"", kind = ToastKind.Success)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailWatchHistory(state: AppState, item: MediaItem) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()
    val history = library.history.filter { it.mediaId == item.id }.take(15)
    if (history.isEmpty()) return
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text("Watch history", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            history.forEach { entry ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(
                        entry.watchedAt.ifBlank { "—" },
                        color = sc.textMuted,
                        fontSize = DsType.Caption,
                        modifier = Modifier.width(120.dp)
                    )
                    Text(
                        "${(entry.percentage * 100).toInt()}% · ${MediaEngine.formatTime(entry.positionMs)}${if (entry.subtitleUsed.isNotBlank()) " · sub: ${entry.subtitleUsed}" else ""}",
                        color = sc.textSecondary,
                        fontSize = DsType.Body,
                        modifier = Modifier.weight(1f)
                    )
                    DsTextButton(text = "Open at", onClick = { media.openItemAt(item, entry.positionMs) })
                }
            }
        }
    }
}

@Composable
private fun DetailMined(state: AppState, item: MediaItem) {
    val media = state.media
    val sc = surfaceColors()
    val minedEvents = media.miningEvents.filter { it.mediaPath == item.path }.take(20)
    if (minedEvents.isEmpty()) return
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text("Mined from this media", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            minedEvents.forEach { event ->
                val card = state.cards.firstOrNull { it.id == event.cardId }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            card?.character?.takeIf { it.isNotBlank() } ?: event.cueText.take(40),
                            color = sc.textPrimary,
                            fontSize = DsType.Body,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${MediaEngine.formatTime(event.timestampMs)}${card?.let { " · ${it.status.name}" } ?: ""}",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                    DsTextButton(
                        text = "Open in Media",
                        onClick = {
                            if (card != null) media.openFromCard(card)
                            else media.openItemAt(item, event.timestampMs)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailBookmarksAndClips(state: AppState, item: MediaItem) {
    val media = state.media
    val sc = surfaceColors()
    val bookmarks = media.bookmarksFor(item)
    val clips = media.clipsFor(item)
    if (bookmarks.isEmpty() && clips.isEmpty()) return
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text("Bookmarks & audio clips", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            bookmarks.forEach { bm ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text("${MediaEngine.formatTime(bm.timestampMs)}", color = accent().primary, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(70.dp))
                    Text(bm.label.ifBlank { "Bookmark" }, color = sc.textSecondary, fontSize = DsType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    DsTextButton(text = "Open", onClick = { media.openItemAt(item, bm.timestampMs) })
                    DsIconButton(icon = Icons.Default.Delete, onClick = { media.removeBookmark(bm.id) }, contentDescription = "Remove bookmark", size = 22.dp)
                }
            }
            clips.forEach { clip ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text("${MediaEngine.formatTime(clip.startMs)}", color = accent().primary, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(70.dp))
                    Text(clip.label.ifBlank { "Audio clip" }, color = sc.textSecondary, fontSize = DsType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    DsIconButton(icon = Icons.Default.Delete, onClick = { media.removeClip(clip.id) }, contentDescription = "Remove clip", size = 22.dp)
                }
            }
        }
    }
}

@Composable
private fun DetailManage(state: AppState, item: MediaItem, missing: Boolean, onBack: () -> Unit) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()
    var removeConfirm by remember { mutableStateOf(false) }

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text("Manage", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                if (!missing) {
                    DsButton(text = "Reveal in folder", icon = Icons.Default.Launch, kind = DsButtonKind.Secondary, compact = true, onClick = {
                        runCatching {
                            java.awt.Desktop.getDesktop().open(File(item.path).parentFile ?: File(item.path))
                        }
                    })
                }
                DsButton(text = "Relink…", kind = DsButtonKind.Secondary, compact = true, onClick = { relinkItemDialog(state, item) })
                DsButton(text = "Remove from library", icon = Icons.Default.Delete, kind = DsButtonKind.Danger, compact = true, onClick = { removeConfirm = true })
            }
        }
    }

    if (removeConfirm) {
        DsConfirmDialog(
            title = "Remove from library",
            message = "Remove \"${item.name}\" from the library? Files stay on disk; watch history is kept.",
            confirmText = "Remove",
            danger = true,
            onConfirm = {
                library.removeItem(item.id, forgetHistory = false)
                onBack()
            },
            onDismiss = { removeConfirm = false }
        )
    }
}

/** Subtitle association: the remembered track + companion files + a file picker. */
@Composable
private fun SubtitleAssociationCard(state: AppState, item: MediaItem) {
    val media = state.media
    val library = media.library
    val sc = surfaceColors()

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text("Subtitles", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            if (!item.isRemote) {
                val companions = remember(item.id) { library.findCompanionSubtitle(File(item.path)) }
                if (companions.isNotEmpty()) {
                    companions.forEach { sub ->
                        val selected = item.subtitlePath == sub.absolutePath
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                            Icon(
                                if (selected) Icons.Default.Check else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (selected) accent().primary else sc.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(sub.name, color = sc.textPrimary, fontSize = DsType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            DsTextButton(
                                text = if (selected) "Loaded" else "Load",
                                onClick = {
                                    media.openSubtitleFile(sub)
                                    state.toastHost.show("Subtitles: ${sub.name}", kind = ToastKind.Success)
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        "No companion subtitle files next to this media. Load one below — the choice is remembered for next time.",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(text = "Add subtitle…", icon = Icons.Default.AudioFile, kind = DsButtonKind.Secondary, compact = true, onClick = {
                    val file = chooseSubtitleFile()
                    if (file != null) media.openSubtitleFile(file)
                })
                if (item.subtitlePath.isNotBlank()) {
                    DsTextButton(text = "Forget track", onClick = { library.setSubtitle(item.id, "") })
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val sc = surfaceColors()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(110.dp))
        Text(value, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
    }
}

/** Human-readable byte size. */
internal fun humanSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes B" else "%.1f %s".format(value, units[unit])
}
