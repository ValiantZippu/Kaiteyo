package ua.syt0r.kanji.desktop.ui.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsPromptDialog
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.media.AudioClip
import ua.syt0r.kanji.desktop.engine.media.AudioPlayer
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.media.MediaKind
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

// ============================================
// KAITEYO MEDIA WORKSPACE
// Video / audio / images / PDF / text with
// subtitle sync, bookmarks, audio clips and
// one-click mining into study cards.
// ============================================

@Composable
fun MediaView(state: AppState) {
    var clipLabelOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = "Media Workspace",
            subtitle = "Study Japanese with video, audio, images, PDF and subtitles — mine words straight into your deck.",
            action = {
                DsButton(text = "Open file", icon = Icons.Default.FileOpen, onClick = { chooseAndOpenMedia(state) })
            }
        )

        val doc = state.media.currentDocument
        if (doc == null) {
            DsCard {
                DsEmptyState(
                    title = "No media open",
                    message = "Open a video (mp4/mkv/webm), audio (mp3/wav/ogg), image, PDF or subtitle file to start learning.",
                    icon = Icons.Default.Movie,
                    action = {
                        DsButton(text = "Open file", icon = Icons.Default.FileOpen, onClick = { chooseAndOpenMedia(state) })
                    }
                )
            }
        } else {
            PlayerCard(state, onRequestSaveClip = { clipLabelOpen = true })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                SubtitleCard(state)
                BookmarkCard(state)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                AudioClipsCard(state)
                RecentFilesCard(state)
            }
        }
    }

    if (clipLabelOpen) {
        DsPromptDialog(
            title = "Save audio clip",
            placeholder = "Label (e.g. 行ってきます)",
            onConfirm = { label ->
                state.media.addAudioClip(label)
                state.toastHost.show("Audio clip saved")
                clipLabelOpen = false
            },
            onDismiss = { clipLabelOpen = false }
        )
    }
}

@Composable
private fun PlayerCard(state: AppState, onRequestSaveClip: () -> Unit) {
    val sc = surfaceColors()
    val media = state.media
    val doc = media.currentDocument ?: return
    val player = remember(doc.path) { AudioPlayer() }

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(doc.name, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                DsBadge(text = doc.kind.name, tint = sc.textSecondary)
            }
            Text(
                buildString {
                    append(formatBytes(doc.sizeBytes))
                    if (doc.kind == MediaKind.Video || doc.kind == MediaKind.Audio) {
                        append("  ·  ").append(MediaEngine.formatTime(media.currentDurationMs))
                    }
                },
                color = sc.textMuted,
                fontSize = DsType.Caption
            )

            if (doc.kind == MediaKind.Audio) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsIconButton(
                        icon = if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = {
                            if (!player.isPlaying) {
                                if (player.positionMs == 0L) player.load(File(doc.path))
                                player.play()
                            } else player.pause()
                        },
                        contentDescription = if (player.isPlaying) "Pause" else "Play",
                        size = 44.dp
                    )
                    DsIconButton(icon = Icons.Default.Stop, onClick = { player.stop() }, contentDescription = "Stop", size = 36.dp)
                    Text(MediaEngine.formatTime(player.positionMs), color = sc.textSecondary, fontSize = DsType.Body)
                    Text("/ ${MediaEngine.formatTime(player.lengthMs)}", color = sc.textMuted, fontSize = DsType.Body)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text("Speed", color = sc.textMuted, fontSize = DsType.Caption)
                    listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { rate ->
                        DsButton(
                            text = "${rate}x",
                            kind = if (media.playbackSpeed == rate) DsButtonKind.Primary else DsButtonKind.Secondary,
                            compact = true,
                            onClick = { media.playbackSpeed = rate }
                        )
                    }
                }
            } else {
                Text(
                    "Full playback UI is rendered by the platform renderer. You can still bookmark the current position, add clips and mine words from subtitles.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                DsButton(
                    text = "Bookmark",
                    icon = Icons.Default.Bookmark,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = {
                        media.addBookmark()
                        state.toastHost.show("Bookmarked at ${MediaEngine.formatTime(media.currentPositionMs)}")
                    }
                )
                DsButton(
                    text = "Save clip",
                    icon = Icons.Default.AudioFile,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = onRequestSaveClip
                )
                if (media.subtitleTrack != null) {
                    DsButton(
                        text = if (media.subtitleVisible) "Hide subtitles" else "Show subtitles",
                        icon = Icons.Default.Subtitles,
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = { media.subtitleVisible = !media.subtitleVisible }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitleCard(state: AppState) {
    val sc = surfaceColors()
    val media = state.media
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Subtitles", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        media.subtitleTrack?.let { "${it.name} · ${it.cues.size} cues · ${it.format.name}" } ?: "No subtitle track loaded",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                DsButton(
                    text = "Load subtitles",
                    icon = Icons.Default.Subtitles,
                    kind = DsButtonKind.Ghost,
                    compact = true,
                    onClick = {
                        val file = chooseFile("subtitle")
                        if (file != null) {
                            media.openSubtitle(file)
                            state.toastHost.show("Loaded subtitle track '${file.name}'")
                        }
                    }
                )
            }
            val cue = media.cueAt(media.currentPositionMs)
            val cueText = cue?.text.orEmpty()
            if (cueText.isNotBlank()) {
                Text(cueText, color = sc.textPrimary, fontSize = DsType.BodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm), modifier = Modifier.padding(top = DsSpacing.Sm)) {
                    DsButton(
                        text = "Mine sentence",
                        icon = Icons.Default.PlayArrow,
                        kind = DsButtonKind.Primary,
                        compact = true,
                        onClick = {
                            state.mining.openMining(
                                MiningPayload(
                                    headword = cueText.take(40),
                                    sentence = cueText,
                                    source = "subtitle",
                                    sourceDetail = media.subtitleTrack?.name.orEmpty(),
                                    timestamp = cue.startMs / 1000.0
                                )
                            )
                        }
                    )
                    cue.tokens().take(6).forEach { token ->
                        DsButton(
                            text = token,
                            kind = DsButtonKind.Secondary,
                            compact = true,
                            onClick = { state.dictionary.query = token }
                        )
                    }
                }
            } else {
                Text("No cue at the current position — scrub or open a track.", color = sc.textMuted, fontSize = DsType.Caption)
            }
        }
    }
}

@Composable
private fun BookmarkCard(state: AppState) {
    val sc = surfaceColors()
    val bookmarks = state.media.bookmarks
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text("Bookmarks", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            if (bookmarks.isEmpty()) {
                Text("No bookmarks yet.", color = sc.textMuted, fontSize = DsType.Caption)
            } else {
                bookmarks.take(8).forEach { bm ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(bm.label.ifBlank { "Bookmark" }, color = sc.textPrimary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                        Text(MediaEngine.formatTime(bm.timestampMs), color = sc.textMuted, fontSize = DsType.Caption)
                        DsIconButton(
                            icon = Icons.Default.Delete,
                            onClick = { state.media.removeBookmark(bm.id) },
                            contentDescription = "Delete bookmark",
                            size = 26.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioClipsCard(state: AppState) {
    val sc = surfaceColors()
    val clips = state.media.audioClips
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text("Audio clips", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            if (clips.isEmpty()) {
                Text("No audio clips yet. Save a clip while listening to build a pronunciation library.", color = sc.textMuted, fontSize = DsType.Caption)
            } else {
                clips.take(8).forEach { clip -> AudioClipRow(state, clip) }
            }
        }
    }
}

@Composable
private fun AudioClipRow(state: AppState, clip: AudioClip) {
    val sc = surfaceColors()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(clip.label.ifBlank { "Clip" }, color = sc.textPrimary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
        DsBadge(text = clip.sourcePath.substringAfterLast(File.separatorChar), tint = sc.textMuted)
        DsIconButton(
            icon = Icons.Default.Delete,
            onClick = { state.media.removeClip(clip.id) },
            contentDescription = "Delete clip",
            size = 26.dp
        )
    }
}

@Composable
private fun RecentFilesCard(state: AppState) {
    val sc = surfaceColors()
    val recent = state.media.recentFiles
    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text("Recent files", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            if (recent.isEmpty()) {
                Text("Nothing opened yet.", color = sc.textMuted, fontSize = DsType.Caption)
            } else {
                recent.take(8).forEach { path ->
                    Row(
                        Modifier.fillMaxWidth().clickable { state.media.openFile(File(path)) }.padding(vertical = DsSpacing.Xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(path.substringAfterLast(File.separatorChar), color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun chooseAndOpenMedia(state: AppState) {
    val file = chooseFile("media") ?: return
    val doc = state.media.openFile(file)
    state.toastHost.show("Opened '${doc.name}'")
}

private fun chooseFile(kind: String): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = when (kind) {
            "subtitle" -> "Open subtitle file (SRT / ASS / SSA / VTT)"
            else -> "Open media file"
        }
        fileSelectionMode = JFileChooser.FILES_ONLY
        if (kind == "subtitle") {
            addChoosableFileFilter(FileNameExtensionFilter("Subtitles", "srt", "ass", "ssa", "vtt"))
        } else {
            addChoosableFileFilter(FileNameExtensionFilter("Media", "mp4", "mkv", "webm", "mov", "mp3", "wav", "ogg", "png", "jpg", "pdf", "txt"))
        }
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    return when {
        bytes < 1024 -> "$bytes B"
        kb < 1024 -> String.format("%.1f KB", kb)
        else -> String.format("%.1f MB", kb / 1024)
    }
}
