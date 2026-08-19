package ua.syt0r.kanji.desktop.ui.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsTextButton
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.errorColor
import ua.syt0r.kanji.desktop.designsystem.infoColor
import ua.syt0r.kanji.desktop.designsystem.newColor
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.theme.tweenDuration
import ua.syt0r.kanji.desktop.engine.dictionary.SegmentToken
import ua.syt0r.kanji.desktop.engine.dictionary.WordStatus
import ua.syt0r.kanji.desktop.engine.media.AnnotationMode
import ua.syt0r.kanji.desktop.engine.media.AutoPauseMode
import ua.syt0r.kanji.desktop.engine.media.LoopMode
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.mining.MinedRecord
import ua.syt0r.kanji.desktop.engine.playback.AudioBackend
import ua.syt0r.kanji.desktop.engine.playback.BackendKind
import ua.syt0r.kanji.desktop.engine.playback.MpvBackend
import ua.syt0r.kanji.desktop.engine.playback.PlaybackCapability
import ua.syt0r.kanji.desktop.engine.playback.TrackKind
import ua.syt0r.kanji.desktop.engine.playback.VlcBackend
import ua.syt0r.kanji.desktop.model.ToastKind
import ua.syt0r.kanji.desktop.ui.dictionary.DictionaryPopupContent
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

// ============================================
// KAITEYO MEDIA PLAYER WORKSPACE
// Video surface (embedded VLC / mpv window /
// audio), subtitle overlay with token annotation,
// auto-hiding transport controls and the docked
// dictionary + transcript panels. Every control
// drives the PlaybackBackend — nothing is fake.
// ============================================

/** Preset subtitle looks, selectable in Settings → Media → Subtitles. */
enum class SubtitleTheme(
    val key: String,
    val label: String,
    val backgroundAlpha: Float,
    val textColor: Color,
    val outline: Boolean,
    val weight: FontWeight
) {
    Classic("classic", "Classic", 0.55f, Color.White, true, FontWeight.Bold),
    Minimal("minimal", "Minimal", 0f, Color.White, false, FontWeight.Normal),
    Cinema("cinema", "Cinema", 0.3f, Color.White, true, FontWeight.SemiBold),
    HighContrast("high-contrast", "High contrast", 0.85f, Color(0xFFFFE14D), true, FontWeight.Bold),
    Custom("custom", "Custom", 0.55f, Color.White, true, FontWeight.Bold);

    companion object {
        fun fromKey(key: String): SubtitleTheme = entries.firstOrNull { it.key == key } ?: Classic
    }
}

private fun subtitleWeight(key: String): FontWeight = when (key) {
    "normal" -> FontWeight.Normal
    "medium" -> FontWeight.Medium
    "semibold" -> FontWeight.SemiBold
    else -> FontWeight.Bold
}

@Composable
fun MediaPlayerWorkspace(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    val item = media.currentItem

    if (item == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DsCard(modifier = Modifier.fillMaxWidth(0.8f)) {
                DsEmptyState(
                    title = "Nothing playing",
                    message = "Open a video, anime episode or audio file. Subtitles load automatically when a matching file exists next to the media.",
                    icon = Icons.Default.Movie,
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                            DsButton(text = "Open file", icon = Icons.Default.PlayArrow, onClick = { chooseMediaFile(state) })
                            DsButton(text = "Open folder", icon = Icons.Default.AudioFile, kind = DsButtonKind.Secondary, onClick = { chooseMediaFolder(state) })
                        }
                    }
                )
            }
        }
        return
    }

    // Controls auto-hide while playing; hovering the video brings them back.
    // The inactivity delay is configurable (5/10/15/30 s, 0 = never hide).
    val videoInteraction = remember { MutableInteractionSource() }
    val videoHovered by videoInteraction.collectIsHoveredAsState()
    val hideDelayMs = remember {
        state.settings.getInt("media.controls-hide-ms", 3000).coerceIn(0, 60000)
    }
    LaunchedEffect(media.isPlaying, videoHovered, hideDelayMs) {
        if (media.isPlaying && !videoHovered && hideDelayMs > 0) {
            delay(hideDelayMs.toLong())
            media.controlsVisible = false
        }
    }

    val panelAnimMs = tweenDuration(LocalAnimationConfig.current, 240)

    // Inline mining confirmation — when a mine completes while the player is
    // on screen, a small pill fades in over the video and drifts away on its
    // own. Playback never pauses and nothing navigates away.
    val minedCount = state.mining.minedRecords.size
    val initialMinedCount = remember { minedCount }
    var mineConfirm by remember { mutableStateOf<MinedRecord?>(null) }
    var mineFading by remember { mutableStateOf(false) }
    LaunchedEffect(minedCount) {
        if (minedCount > initialMinedCount) {
            mineConfirm = state.mining.minedRecords.firstOrNull()
            mineFading = false
            delay(2400)
            mineFading = true
            delay(320)
            mineConfirm = null
            mineFading = false
        }
    }
    val minePillAlpha by animateFloatAsState(
        targetValue = if (mineFading) 0f else 1f,
        animationSpec = tween(tweenDuration(LocalAnimationConfig.current, 240)),
        label = "minePillAlpha"
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val windowMaxWidth = maxWidth
        // The study workspace adapts: narrower windows get a slimmer side panel
        // so the video keeps most of the screen. The user can drag its left
        // edge to resize within sane bounds.
        var userPanelWidth by remember { mutableStateOf<Float?>(null) }
        val sidePanelWidth = userPanelWidth ?: if (maxWidth < 1080.dp) 320f else 380f
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                QueueStrip(state)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(sc.background)
                        .hoverable(videoInteraction)
                ) {
                    VideoSurface(state)
                    media.playbackError?.let { err ->
                        ErrorOverlay(err.userMessage, Modifier.align(Alignment.Center))
                    }
                    if (media.buffering) {
                        BufferingBadge(Modifier.align(Alignment.Center))
                    }
                    mineConfirm?.let { record ->
                        MineConfirmPill(
                            record = record,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = DsSpacing.Md)
                                .graphicsLayer { alpha = minePillAlpha }
                        )
                    }
                    if (media.subtitleVisible) {
                        val subtitleAlign = if (state.settings.getString("media.subtitle-position", "bottom") == "top")
                            Alignment.TopCenter else Alignment.BottomCenter
                        SubtitleOverlay(state, Modifier.align(subtitleAlign))
                    }
                    if (media.controlsVisible) {
                        ControlsOverlay(state, Modifier.align(Alignment.BottomCenter))
                    }
                }
                MediaDashboardStrip(state)
                CurrentCueBar(state)
            }
            // Transcript / dictionary slide in from the right instead of
            // popping — context is preserved, nothing jumps.
            AnimatedVisibility(
                visible = media.transcriptOpen || media.dictionaryOpen,
                enter = expandHorizontally(animationSpec = tween(panelAnimMs), expandFrom = Alignment.End) + fadeIn(tween(panelAnimMs)),
                exit = shrinkHorizontally(animationSpec = tween(panelAnimMs), shrinkTowards = Alignment.End) + fadeOut(tween(panelAnimMs))
            ) {
                Box {
                    Column(
                        Modifier
                            .width(sidePanelWidth.dp)
                            .fillMaxHeight()
                            .background(sc.surface)
                            .border(androidx.compose.ui.unit.Dp.Hairline, sc.border)
                    ) {
                        if (media.transcriptOpen) {
                            TranscriptPanel(state, Modifier.weight(1f))
                        }
                        if (media.dictionaryOpen) {
                            DictionaryPanel(state, Modifier.weight(1f))
                        }
                    }
                    // Drag handle on the panel's left edge — resize without
                    // ever leaving the player.
                    val handleInteraction = remember { MutableInteractionSource() }
                    val handleHovered by handleInteraction.collectIsHoveredAsState()
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(8.dp)
                            .fillMaxHeight()
                            .background(if (handleHovered) sc.border.copy(alpha = 0.9f) else sc.border.copy(alpha = 0.25f))
                            .hoverable(handleInteraction)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    userPanelWidth = (sidePanelWidth - dragAmount.x)
                                        .coerceIn(240f, (windowMaxWidth.value * 0.55f).coerceAtLeast(240f))
                                }
                            }
                    )
                }
            }
        }
    }

    if (media.cueEditOpen) {
        CueEditDialog(state)
    }

    if (media.resumePromptPending) {
        ResumePromptDialog(state)
    }

    if (media.endOfEpisodeVisible) {
        EndOfEpisodeDialog(state)
    }
}

// ============================================
// PLAY QUEUE STRIP + END-OF-EPISODE
// ============================================

/** Compact "up next" strip shown whenever the play queue has items. */
@Composable
private fun QueueStrip(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    if (media.playQueue.isEmpty()) return

    Row(
        Modifier.fillMaxWidth().background(sc.surfaceElevated).padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Icon(Icons.Default.SkipNext, contentDescription = null, tint = accent().primary, modifier = Modifier.size(14.dp))
        Text("Queue", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
        media.playQueue.take(3).forEachIndexed { i, qItem ->
            val current = i == media.queueIndex
            Text(
                if (current) "▶ ${qItem.name}" else qItem.name,
                color = if (current) accent().primary else sc.textSecondary,
                fontSize = DsType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp)
            )
        }
        if (media.playQueue.size > 3) {
            Text("+${media.playQueue.size - 3} more", color = sc.textMuted, fontSize = DsType.Caption)
        }
        Spacer(Modifier.weight(1f))
        DsIconButton(icon = Icons.Default.SkipPrevious, onClick = { media.playPrevious() }, contentDescription = "Previous in queue", size = 22.dp)
        DsIconButton(icon = Icons.Default.SkipNext, onClick = { media.playNext() }, contentDescription = "Next in queue", size = 22.dp)
        DsIconButton(icon = Icons.Default.Close, onClick = { media.clearQueue() }, contentDescription = "Clear queue", size = 22.dp)
    }
}

/** Shown when an episode ends: next up / replay / back to the library. */
@Composable
private fun EndOfEpisodeDialog(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    val next = media.suggestedNextUp

    DsDialog(
        title = "Playback finished",
        onDismiss = { media.dismissEndOfEpisode() }
    ) {
        Text(
            "\"${media.currentItem?.name ?: "This media"}\" has ended.",
            color = sc.textSecondary,
            fontSize = DsType.Body
        )
        if (next != null) {
            Spacer(Modifier.height(DsSpacing.Sm))
            Text(
                "Up next: ${next.name}${if (next.episode.isNotBlank()) " (${next.episode})" else ""}",
                color = accent().primary,
                fontSize = DsType.Body,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(DsSpacing.Xl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
        ) {
            DsTextButton(text = "Back to library", onClick = { media.returnToLibrary() })
            DsTextButton(text = "Replay", onClick = { media.restartEpisode() })
            if (next != null) {
                DsButton(text = "Play next", icon = Icons.Default.SkipNext, onClick = { media.playSuggestedNext() })
            }
        }
    }
}

/** Ask whether to continue from the saved position or start over. */
@Composable
private fun ResumePromptDialog(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    val resumeMs = media.pendingResumeMs

    DsDialog(
        title = "Resume playback",
        onDismiss = { media.answerResumePrompt(false) }
    ) {
        Text(
            "You last watched this media until ${MediaEngine.formatTime(resumeMs)}.",
            color = sc.textSecondary,
            fontSize = DsType.Body
        )
        Spacer(Modifier.height(DsSpacing.Xl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
        ) {
            DsTextButton(text = "Start over", onClick = { media.answerResumePrompt(false) })
            DsButton(text = "Resume at ${MediaEngine.formatTime(resumeMs)}", onClick = { media.answerResumePrompt(true) })
        }
    }
}

// ============================================
// SUBTITLE OFFSET + TEXT CORRECTION
// ============================================

/** Fine subtitle synchronization: shift the whole track by ±0.5s. */
@Composable
private fun SubtitleOffsetAdjust(state: AppState) {
    val media = state.media
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        OffsetChip("-0.5s") { media.adjustSubtitleDelay(-500) }
        OffsetChip("+0.5s") { media.adjustSubtitleDelay(500) }
        // Dual-language track has its own independent timing.
        if (media.subtitles.showSecondary && media.subtitles.secondaryTrack != null) {
            OffsetChip("2nd −0.5s") { media.adjustSecondaryOffset(-500) }
            OffsetChip("2nd +0.5s") { media.adjustSecondaryOffset(500) }
        }
    }
}

@Composable
private fun OffsetChip(label: String, onClick: () -> Unit) {
    val sc = surfaceColors()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(sc.surfaceInteractive)
            .clickable(onClick = onClick)
            .padding(horizontal = DsSpacing.Sm, vertical = 4.dp)
    ) {
        Text(label, color = sc.textSecondary, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
    }
}

/** In-memory subtitle text correction — never writes to the source file. */
@Composable
private fun CueEditDialog(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    val cue = media.activeCue
    if (cue == null) {
        media.cueEditOpen = false
        return
    }
    var text by remember(cue.id) { mutableStateOf(media.displayTextFor(cue)) }

    DsDialog(
        title = "Edit subtitle text",
        onDismiss = { media.closeCueEditor() }
    ) {
        Text(
            "Corrections apply to lookup, mining, the transcript and cards — the original subtitle file is never modified.",
            color = sc.textSecondary,
            fontSize = DsType.Body
        )
        Spacer(Modifier.height(DsSpacing.Lg))
        DsTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = "Corrected subtitle text",
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(DsSpacing.Lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
        ) {
            if (media.hasCueEdits()) {
                DsTextButton(text = "Reset all corrections", onClick = {
                    media.resetCueEdits()
                    text = media.displayTextFor(cue)
                })
            }
            DsTextButton(text = "Cancel", onClick = { media.closeCueEditor() })
            DsButton(
                text = "Save",
                onClick = {
                    media.editCueText(cue.id, text)
                    media.closeCueEditor()
                }
            )
        }
    }
}

// ============================================
// VIDEO SURFACE
// ============================================

@Composable
private fun VideoSurface(state: AppState) {
    val media = state.media
    val backend = media.activeBackend
    when (backend) {
        is VlcBackend -> {
            val component = remember(backend) { backend.ensureComponent() }
            if (component != null) {
                SwingPanel(
                    factory = { component },
                    modifier = Modifier.fillMaxSize().background(Color.Black)
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    DsEmptyState("VLC not available", "Install VLC to watch video inside Kaiteyo.", icon = Icons.Default.Movie)
                }
            }
        }
        is MpvBackend -> {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text("mpv playback window", color = Color.White.copy(alpha = 0.85f), fontSize = DsType.Title, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Playback is rendered by mpv in its own window — every control below still works via the IPC connection.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = DsType.Body,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = DsSpacing.Xl)
                    )
                }
            }
        }
        is AudioBackend -> AudioNowPlaying(media)
        else -> {
            if (media.backendKind == BackendKind.None) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    DsEmptyState(
                        "No playback backend",
                        "Install VLC or mpv to play video. Audio files always work with the built-in engine.",
                        icon = Icons.Default.Movie
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioNowPlaying(media: ua.syt0r.kanji.desktop.engine.media.MediaEngine) {
    val sc = surfaceColors()
    val item = media.currentItem
    Box(Modifier.fillMaxSize().background(sc.surface), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(accent().primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = null, tint = accent().primary, modifier = Modifier.size(44.dp))
            }
            Text(item?.name ?: "Audio", color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${MediaEngine.formatTime(media.positionMs)} / ${MediaEngine.formatTime(media.durationMs)} · ${media.speed}x",
                color = sc.textMuted,
                fontSize = DsType.Body
            )
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, modifier: Modifier = Modifier) {
    Box(modifier.padding(DsSpacing.Xl).clip(RoundedCornerShape(DsRadius.Md)).background(Color(0xCC1A1A1A)).padding(DsSpacing.Lg)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            Text("Playback error", color = errorColor(), fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            Text(message, color = Color.White.copy(alpha = 0.85f), fontSize = DsType.Body, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BufferingBadge(modifier: Modifier = Modifier) {
    DsBadge(text = "Buffering…", modifier = modifier)
}

/** Compact inline confirmation after a successful mine — drifts away on its
 *  own, playback never pauses. */
@Composable
private fun MineConfirmPill(record: MinedRecord, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val pillText = when {
        record.destination == "anki" && record.ankiStatus == "success" -> "→ Anki ✓"
        record.destination == "anki" -> "Anki unavailable — saved to Kaiteyo"
        else -> "→ Kaiteyo deck"
    }
    val pillColor = when {
        record.destination == "anki" && record.ankiStatus == "success" -> successColor()
        record.destination == "anki" -> warningColor()
        else -> successColor()
    }
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = pillColor, modifier = Modifier.size(14.dp))
        Text(
            record.headword.ifBlank { "Mined" },
            color = Color.White,
            fontSize = DsType.Body,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(pillText, color = pillColor, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================
// SUBTITLE OVERLAY
// ============================================

@Composable
fun SubtitleOverlay(state: AppState, modifier: Modifier = Modifier) {
    val media = state.media
    val cue = media.activeCue
    val sc = surfaceColors()
    if (cue == null || cue.text.isBlank()) return

    val tokens = media.tokensFor(cue)
    val baseSize = state.settings.getInt("media.subtitle-font-size", 20)
    val scale = state.settings.getFloat("media.subtitle-scale", 1f).coerceIn(0.5f, 2f)
    val fontSize = (baseSize * scale).sp
    val position = state.settings.getString("media.subtitle-position", "bottom")

    // Theme presets + custom overrides (opacity/weight) are applied here so
    // the overlay reflects Settings → Media → Subtitles immediately.
    val theme = SubtitleTheme.fromKey(state.settings.getString("media.subtitle-theme", "classic"))
    val customOpacity = state.settings.getFloat("media.subtitle-opacity", 0.55f).coerceIn(0f, 1f)
    val bgAlpha = if (theme == SubtitleTheme.Custom) customOpacity else theme.backgroundAlpha
    val textColor = if (theme == SubtitleTheme.Custom) Color.White else theme.textColor
    val weight = if (theme == SubtitleTheme.Custom) subtitleWeight(state.settings.getString("media.subtitle-weight", "bold")) else theme.weight
    val outline = if (theme == SubtitleTheme.Custom) state.settings.getBool("media.subtitle-outline") else theme.outline

    Column(
        modifier
            .padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Md)
            .then(if (position == "top") Modifier.padding(top = 24.dp) else Modifier.padding(bottom = 72.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val secondary = media.secondaryCue
        if (secondary != null && secondary.text.isNotBlank()) {
            Text(
                secondary.text,
                color = textColor.copy(alpha = 0.7f),
                fontSize = (fontSize.value * 0.75f).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = DsSpacing.Xs)
            )
        }
        // Live hit-test bounds (window space) so click / shift-click / drag all
        // resolve to token indices regardless of the annotation layout used.
        val tokenBounds = remember(tokens) { mutableStateMapOf<Int, Rect>() }
        var rowTopLeft by remember { mutableStateOf(Offset.Zero) }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (outline) Color.Black.copy(alpha = bgAlpha) else Color.Transparent)
                .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Sm)
                .onGloballyPositioned { rowTopLeft = it.positionInRoot() }
                .pointerInput(tokens) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val start = tokenIndexAt(down.position + rowTopLeft, tokenBounds)
                        var range: IntRange = IntRange.EMPTY
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) {
                                if (range.isEmpty()) {
                                    // Plain press-release: single token select (shift = extend).
                                    val end = tokenIndexAt(change.position + rowTopLeft, tokenBounds)
                                    val token = tokens.getOrNull(if (end >= 0) end else start)
                                    if (event.keyboardModifiers.isShiftPressed) {
                                        if (end >= 0 && token != null) media.selectToken(token, extend = true)
                                    } else if (token != null) {
                                        media.selectToken(token)
                                    } else {
                                        media.clearSelection()
                                    }
                                }
                                break
                            }
                            if (change.positionChanged()) {
                                val idx = tokenIndexAt(change.position + rowTopLeft, tokenBounds)
                                if (idx >= 0 && start >= 0) {
                                    val r = expandRange(start, idx)
                                    if (r != range) {
                                        range = r
                                        media.selectTokenRange(r.first, r.last)
                                    }
                                }
                            }
                        }
                    }
                },
            horizontalArrangement = Arrangement.Center
        ) {
            tokens.forEachIndexed { index, token ->
                AnnotatedToken(
                    token = token,
                    mode = media.annotationMode,
                    fontSize = fontSize,
                    baseColor = textColor,
                    weight = weight,
                    selected = media.selectedTokens.any { it.surface == token.surface && it.offset == token.offset },
                    modifier = Modifier.onGloballyPositioned { tokenBounds[index] = it.boundsInRoot() }
                )
            }
        }

        // Selection bar: phrase actions for multi-word selections, a subtle
        // affordance otherwise.
        SelectionActions(state = state, media = media, tokens = tokens)
    }
}

@Composable
private fun AnnotatedToken(
    token: SegmentToken,
    mode: AnnotationMode,
    fontSize: androidx.compose.ui.unit.TextUnit,
    baseColor: Color,
    weight: FontWeight,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val ac = accent()
    val color = when {
        selected -> ac.primary
        mode == AnnotationMode.Status && token.isJapanese -> statusColor(token.status)
        else -> baseColor.copy(alpha = if (selected) 1f else 0.95f)
    }

    when (mode) {
        AnnotationMode.Reading -> {
            if (token.isKanji && token.reading.isNotBlank()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(horizontal = 1.dp)) {
                    Text(
                        token.reading,
                        color = ac.primary.copy(alpha = 0.9f),
                        fontSize = (fontSize.value * 0.45f).sp,
                        lineHeight = (fontSize.value * 0.6f).sp
                    )
                    Text(token.surface, color = color, fontSize = fontSize, fontWeight = weight)
                }
            } else {
                TokenText(token.surface, color, fontSize, weight, selected, modifier)
            }
        }
        AnnotationMode.Frequency -> {
            val rank = token.dictionaryMatch?.entry?.frequency?.rank
            Row(verticalAlignment = Alignment.Bottom, modifier = modifier.padding(horizontal = 1.dp)) {
                Text(token.surface, color = color, fontSize = fontSize, fontWeight = weight)
                if (rank != null) {
                    Text(
                        "$rank",
                        color = baseColor.copy(alpha = 0.55f),
                        fontSize = (fontSize.value * 0.45f).sp,
                        modifier = Modifier.padding(start = 1.dp, bottom = 2.dp)
                    )
                }
            }
        }
        else -> TokenText(token.surface, color, fontSize, weight, selected, modifier)
    }
}

@Composable
private fun TokenText(text: String, color: Color, fontSize: androidx.compose.ui.unit.TextUnit, weight: FontWeight, selected: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = weight,
        modifier = modifier
            .padding(horizontal = 1.dp)
            .then(
                if (selected) Modifier.background(accent().primary.copy(alpha = 0.25f)) else Modifier
            )
    )
}

@Composable
private fun SelectionActions(state: AppState, media: MediaEngine, tokens: List<SegmentToken>) {
    val sc = surfaceColors()
    val phrase = media.selectedPhrase()
    val multi = media.selectedTokens.size > 1
    Row(
        modifier = Modifier.padding(top = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        when {
            multi -> {
                Text(
                    "\u201c$phrase\u201d",
                    color = sc.textPrimary,
                    fontSize = DsType.Body,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                DsTextButton(text = "Lookup", onClick = { media.lookupText(phrase) })
                DsTextButton(text = "Mine", onClick = { media.mineCurrentCue() })
                DsTextButton(text = "Copy", onClick = {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(phrase), null)
                    state.toastHost.show("Phrase copied", kind = ToastKind.Info)
                })
                DsTextButton(text = "Clear", onClick = { media.clearSelection() })
            }
            media.selectedTokens.isEmpty() && tokens.size > 1 -> {
                Text("Click a word · shift-click or drag for a phrase", color = sc.textMuted, fontSize = DsType.Caption)
                DsTextButton(text = "Select all", onClick = { media.selectAllTokens() })
            }
        }
    }
}

/** Index of the token whose window-space [bounds] contain [position]; -1 for none. */
internal fun tokenIndexAt(position: Offset, bounds: Map<Int, Rect>): Int {
    var best = -1
    var bestWidth = Float.MAX_VALUE
    for ((index, rect) in bounds) {
        if (rect.contains(position) && rect.width < bestWidth) {
            best = index
            bestWidth = rect.width
        }
    }
    return best
}

/** Contiguous selection range between an anchor and a dragged/clicked index. */
internal fun expandRange(anchor: Int, current: Int): IntRange =
    minOf(anchor, current)..maxOf(anchor, current)

@Composable
private fun statusColor(status: WordStatus): Color = when (status) {
    WordStatus.Unknown -> Color.White.copy(alpha = 0.95f)
    WordStatus.Known -> successColor()
    WordStatus.Learning -> warningColor()
    WordStatus.Mature -> infoColor()
    WordStatus.New -> newColor()
    WordStatus.Mined -> androidx.compose.ui.graphics.Color(0xFFD2A3FF)
    WordStatus.Suspended -> Color.Gray
}

// ============================================
// CURRENT CUE BAR
// ============================================

@Composable
fun CurrentCueBar(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    val cue = media.activeCue

    Row(
        Modifier.fillMaxWidth().background(sc.surfaceElevated).padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        if (cue != null) {
            Text(
                "${MediaEngine.formatTime(cue.startMs + media.subtitles.globalOffsetMs)} – ${MediaEngine.formatTime(cue.endMs + media.subtitles.globalOffsetMs)}",
                color = accent().primary,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                cue.text,
                color = sc.textPrimary,
                fontSize = DsType.BodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            DsButton(
                text = "Mine sentence",
                icon = Icons.Default.PlayArrow,
                compact = true,
                onClick = { media.mineCurrentCue() }
            )
            DsIconButton(icon = Icons.Default.Replay, onClick = { media.replayCue() }, contentDescription = "Replay subtitle", size = 32.dp)
            DsIconButton(
                icon = Icons.Default.Loop,
                onClick = { media.toggleLoopCue() },
                contentDescription = "Loop subtitle",
                tint = if (media.loopMode == LoopMode.CurrentCue) accent().primary else null,
                size = 32.dp
            )
            DsIconButton(
                icon = Icons.Default.PhotoCamera,
                onClick = { media.captureScreenshot() },
                contentDescription = "Screenshot",
                enabled = media.can(PlaybackCapability.CanScreenshot),
                size = 32.dp
            )
            DsIconButton(
                icon = Icons.Default.AudioFile,
                onClick = { media.captureAudioClip(cue) },
                contentDescription = "Capture audio clip",
                enabled = MediaCaptureRef.ffmpegAvailable(),
                size = 32.dp
            )
            DsIconButton(
                icon = Icons.Default.ContentCopy,
                onClick = {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(media.displayTextFor(cue)), null)
                    state.toastHost.show("Subtitle copied", kind = ToastKind.Info)
                },
                contentDescription = "Copy subtitle",
                size = 32.dp
            )
            SubtitleOffsetAdjust(state)
            DsIconButton(
                icon = Icons.Default.Edit,
                onClick = { media.openCueEditor() },
                contentDescription = "Edit subtitle text (in-memory)",
                size = 32.dp
            )
        } else {
            Text("No subtitle at this position — scrub the timeline or open a track.", color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.weight(1f))
            DsButton(text = "Load subtitles", icon = Icons.Default.AudioFile, kind = DsButtonKind.Ghost, compact = true, onClick = {
                val f = chooseSubtitleFile()
                if (f != null) media.openSubtitleFile(f)
            })
        }
    }
}

/** Alias so the UI doesn't reach into engine internals for the ffmpeg probe. */
internal object MediaCaptureRef {
    fun ffmpegAvailable(): Boolean = ua.syt0r.kanji.desktop.engine.media.MediaCapture.ffmpegAvailable
}

// ============================================
// TRANSPORT CONTROLS (auto-hiding)
// ============================================

/** Compact live dashboard while watching: progress, coverage, mined count. */
@Composable
fun MediaDashboardStrip(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    val ac = accent()
    val item = media.currentItem ?: return

    Row(
        Modifier.fillMaxWidth().background(sc.surface).padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val pct = (item.progressFraction * 100).toInt()
            Text(
                if (item.durationMs > 0) "${pct}% watched · ${item.watchCount} watch${if (item.watchCount == 1) "" else "es"}" else " ",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
        if (media.activeCue != null) {
            val coverage = (media.currentCoverage * 100).toInt()
            DsBadge(text = "Known ${coverage}%", tint = if (coverage >= 70) successColor() else warningColor())
            DsBadge(text = "Unknown ${(100 - coverage).coerceAtLeast(0)}%", tint = sc.textSecondary)
        }
        DsBadge(text = "${media.currentMinedCount} mined", tint = ac.primary)
        DsBadge(text = "${media.backendKind.name}", tint = sc.textSecondary)
    }
}

@Composable
fun ControlsOverlay(state: AppState, modifier: Modifier = Modifier) {
    val media = state.media
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    // Keep controls alive while the pointer is over them, but never re-show
    // them once the auto-hide timer has fired (that caused show/hide flicker).
    LaunchedEffect(hovered) { if (hovered) media.controlsVisible = true }
    val animMs = tweenDuration(LocalAnimationConfig.current, 220)
    var moreOpen by remember { mutableStateOf(false) }

    // The bar fades and slides up rather than popping into existence.
    AnimatedVisibility(
        visible = media.controlsVisible,
        enter = fadeIn(animationSpec = tween(animMs)) +
            slideInVertically(animationSpec = tween(animMs), initialOffsetY = { it / 2 }),
        exit = fadeOut(animationSpec = tween(animMs)) +
            slideOutVertically(animationSpec = tween(animMs), targetOffsetY = { it / 2 }),
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.72f))
                .hoverable(interaction)
                .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Sm)
        ) {
            MediaSeekBar(state)
            Spacer(Modifier.height(DsSpacing.Xs))
            // Primary transport row — only the essentials. Everything else
            // lives in the More menu so the main bar never becomes a wall of
            // icons.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                DsIconButton(
                    icon = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    onClick = { media.togglePlay() },
                    contentDescription = "Play / Pause",
                    tint = Color.White,
                    size = 40.dp
                )
                DsIconButton(icon = Icons.Default.FastRewind, onClick = { media.seekBy(-media.seekAmountMs) }, contentDescription = "Back", tint = Color.White, size = 28.dp)
                DsIconButton(icon = Icons.Default.FastForward, onClick = { media.seekBy(media.seekAmountMs) }, contentDescription = "Forward", tint = Color.White, size = 28.dp)
                DsIconButton(icon = Icons.Default.Stop, onClick = { media.stop() }, contentDescription = "Stop", tint = Color.White, size = 28.dp)
                DsIconButton(icon = Icons.Default.SkipPrevious, onClick = { media.replayPreviousCue() }, contentDescription = "Previous subtitle", tint = Color.White, size = 28.dp)
                DsIconButton(icon = Icons.Default.SkipNext, onClick = { media.replayNextCue() }, contentDescription = "Next subtitle", tint = Color.White, size = 28.dp)
                Spacer(Modifier.width(DsSpacing.Sm))
                Icon(
                    if (media.muted || media.volume == 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp).clickable { media.toggleMute() }
                )
                Slider(
                    value = media.volume.toFloat(),
                    onValueChange = { media.updateVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.width(110.dp)
                )
                Spacer(Modifier.weight(1f))
                // Study workspace — one tap turns the player into the study
                // surface (transcript + dictionary) and back. Same workspace,
                // one more layer — not a navigation jump.
                DsIconButton(
                    icon = Icons.Default.AutoAwesome,
                    onClick = { toggleStudyWorkspace(media) },
                    contentDescription = "Study workspace (transcript + dictionary)",
                    tint = if (media.transcriptOpen || media.dictionaryOpen) ac.primary else Color.White,
                    size = 30.dp
                )
                DsIconButton(
                    icon = Icons.Default.Subtitles,
                    onClick = { media.subtitleVisible = !media.subtitleVisible },
                    contentDescription = "Toggle subtitles",
                    tint = if (media.subtitleVisible) Color.White else Color.White.copy(alpha = 0.45f),
                    size = 30.dp
                )
                Box {
                    DsIconButton(
                        icon = Icons.Default.MoreVert,
                        onClick = { moreOpen = true },
                        contentDescription = "More player options",
                        tint = if (moreOpen) ac.primary else Color.White,
                        size = 30.dp
                    )
                    PlayerMoreMenu(state, expanded = moreOpen, onDismiss = { moreOpen = false })
                }
                DsIconButton(icon = Icons.Default.Fullscreen, onClick = { media.toggleFullscreen() }, contentDescription = "Fullscreen", tint = Color.White, size = 30.dp)
            }
        }
    }
}

/** Flip the player between pure viewing and the study workspace. */
private fun toggleStudyWorkspace(media: MediaEngine) {
    val opening = !(media.transcriptOpen || media.dictionaryOpen)
    media.transcriptOpen = opening
    media.dictionaryOpen = opening
}

/** Secondary player controls — everything that doesn't belong on the main
 *  transport bar lives here: speed, loops, chapters, tracks, capture and
 *  lookup. Compact chips keep the menu dense without turning into a form. */
@Composable
private fun PlayerMoreMenu(state: AppState, expanded: Boolean, onDismiss: () -> Unit) {
    val media = state.media
    val sc = surfaceColors()

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(320.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            if (media.can(PlaybackCapability.CanChangeSpeed)) {
                MenuLabel("Playback speed")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { rate ->
                        PlayerMenuChip("${rate}x", media.speed == rate) { media.updateSpeed(rate) }
                    }
                }
            }
            MenuLabel("Playback")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayerMenuChip("Loop cue", media.loopMode == LoopMode.CurrentCue) { media.toggleLoopCue() }
                PlayerMenuChip("Condensed", media.condensedPlayback) { media.toggleCondensed() }
                PlayerMenuChip(if (media.cinemaMode) "Exit cinema" else "Cinema", media.cinemaMode) { media.toggleCinemaMode() }
            }
            // A–B range loop — set both points from the current position.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayerMenuChip(
                    if (media.loopStartMs > 0) "A ${MediaEngine.formatTime(media.loopStartMs)}" else "Set A",
                    media.loopMode == LoopMode.Range && media.loopStartMs > 0
                ) {
                    if (media.loopMode == LoopMode.Range && media.loopStartMs > 0 && media.positionMs > media.loopStartMs) {
                        media.loopEndMs = media.positionMs
                    } else {
                        media.loopStartMs = media.positionMs
                        media.loopMode = LoopMode.Range
                    }
                }
                PlayerMenuChip(
                    if (media.loopEndMs > media.loopStartMs && media.loopMode == LoopMode.Range) "B ${MediaEngine.formatTime(media.loopEndMs)}" else "Set B",
                    media.loopMode == LoopMode.Range && media.loopEndMs > media.loopStartMs
                ) {
                    media.loopEndMs = media.positionMs
                    media.loopMode = LoopMode.Range
                }
                if (media.loopMode == LoopMode.Range) {
                    PlayerMenuChip("Clear A–B", false) {
                        media.loopMode = LoopMode.Off
                        media.loopStartMs = 0
                        media.loopEndMs = 0
                    }
                }
            }
            AutoPauseSelect(state)
            if (media.chapters.isNotEmpty()) {
                MenuLabel("Chapters")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayerMenuChip("◀ Chapter", false, enabled = media.currentChapterIndex > 0) { media.previousChapter() }
                    PlayerMenuChip("Chapter ▶", false, enabled = media.currentChapterIndex < media.chapters.lastIndex) { media.nextChapter() }
                    media.chapters.getOrNull(media.currentChapterIndex)?.let { chapter ->
                        Text(
                            chapter.title.ifBlank { "Ch. ${media.currentChapterIndex + 1}" },
                            color = sc.textMuted,
                            fontSize = DsType.Caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (media.can(PlaybackCapability.CanFrameStep)) {
                MenuLabel("Frame stepping")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PlayerMenuChip("Previous frame", false) { media.frameStepBackward() }
                    PlayerMenuChip("Next frame", false) { media.frameStepForward() }
                }
            }
            if (media.subtitleTracks.isNotEmpty() || media.audioTracks.isNotEmpty() || media.videoTracks.size >= 2) {
                MenuLabel("Tracks")
                SubtitleTrackSelect(state)
                AudioTrackSelect(state)
                VideoTrackSelect(state)
            }
            MenuLabel("Capture & lookup")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayerMenuChip("Screenshot", false, enabled = media.can(PlaybackCapability.CanScreenshot)) { media.captureScreenshot() }
                PlayerMenuChip("OCR frame", false) { media.ocrFrame() }
                PlayerMenuChip("Clipboard", false) { media.lookupClipboard() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayerMenuChip("Bookmark", false) { media.addBookmark() }
                PlayerMenuChip("Secondary subs", media.subtitles.showSecondary) { media.subtitles.showSecondary = !media.subtitles.showSecondary }
            }
        }
    }
}

@Composable
private fun MenuLabel(text: String) {
    Text(
        text,
        color = surfaceColors().textMuted,
        fontSize = DsType.Caption,
        fontWeight = FontWeight.SemiBold
    )
}

/** Compact selectable chip used inside the player's More menu. */
@Composable
private fun PlayerMenuChip(text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) ac.primary.copy(alpha = 0.18f) else sc.surfaceInteractive)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = DsSpacing.Sm, vertical = 4.dp)
    ) {
        Text(
            text,
            color = if (selected) ac.primary else sc.textSecondary,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AutoPauseSelect(state: AppState) {
    val media = state.media
    DsSelect(
        selected = media.autoPauseMode,
        options = AutoPauseMode.entries,
        onSelected = { media.autoPauseMode = it },
        labelOf = { it.label },
        modifier = Modifier.width(140.dp)
    )
}

@Composable
private fun SubtitleTrackSelect(state: AppState) {
    val media = state.media
    val tracks = media.subtitleTracks
    if (tracks.isEmpty()) return
    DsSelect(
        selected = tracks.first(),
        options = tracks,
        onSelected = { media.selectSubtitleTrack(it.id) },
        labelOf = { "Sub: ${it.title.ifBlank { it.language.ifBlank { it.id } }}" },
        modifier = Modifier.width(130.dp)
    )
}

@Composable
private fun AudioTrackSelect(state: AppState) {
    val media = state.media
    val tracks = media.audioTracks
    if (tracks.isEmpty()) return
    DsSelect(
        selected = tracks.first(),
        options = tracks,
        onSelected = { media.selectAudioTrack(it.id) },
        labelOf = { "Audio: ${it.title.ifBlank { it.language.ifBlank { it.id }}}" },
        modifier = Modifier.width(130.dp)
    )
}

/**
 * Video track selection — only shown when the loaded media actually exposes
 * multiple video tracks (rare, but real for multi-angle MKVs). Wired to the
 * backend's TrackKind.Video handling, never faked.
 */
@Composable
private fun VideoTrackSelect(state: AppState) {
    val media = state.media
    val tracks = media.videoTracks
    // A single video track is just "the video" — nothing to choose from.
    if (tracks.size < 2) return
    DsSelect(
        selected = tracks.first(),
        options = tracks,
        onSelected = { media.selectVideoTrack(it.id) },
        labelOf = { "Video: ${it.title.ifBlank { it.language.ifBlank { it.id }}}" },
        modifier = Modifier.width(130.dp)
    )
}

// ============================================
// SEEK BAR with subtitle markers + A–B range
// ============================================

@Composable
fun MediaSeekBar(state: AppState, modifier: Modifier = Modifier) {
    val media = state.media
    val sc = surfaceColors()
    val ac = accent()
    val duration = media.durationMs.coerceAtLeast(1)
    val position = media.positionMs.coerceIn(0, duration)
    val buffered = media.bufferedPositionMs.coerceIn(position, duration)
    val markers = remember(media.subtitles.activeTrackId, media.subtitles.tracks.size) { media.subtitles.cueMarkers() }
    val chapters = remember(media.activeBackend) { media.chapters }
    // Hover timestamp: show the time under the cursor while dragging or hovering.
    var hoverMs by remember { mutableStateOf<Long?>(null) }

    fun offsetToMs(x: Float, width: Float): Long = ((x / width.coerceAtLeast(1f)) * duration).toLong().coerceIn(0, duration)

    Box(
        modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset -> media.seekTo(offsetToMs(offset.x, size.width.toFloat())) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        hoverMs = offsetToMs(offset.x, size.width.toFloat())
                        media.seekTo(hoverMs!!)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        hoverMs = offsetToMs(change.position.x, size.width.toFloat())
                        media.seekTo(hoverMs!!)
                    },
                    onDragEnd = { hoverMs = null },
                    onDragCancel = { hoverMs = null }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        val pos = change?.position
                        if (pos == null) continue
                        // Clear the preview once the pointer leaves the bar
                        // (including above it — the bar is only 26 dp tall).
                        if (pos.y !in 0f..size.height.toFloat() || pos.x !in 0f..size.width.toFloat()) {
                            hoverMs = null
                        } else if (change.pressed == false) {
                            hoverMs = offsetToMs(pos.x, size.width.toFloat())
                        }
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val trackY = h / 2
            // track
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset(0f, trackY - 2.dp.toPx() / 2),
                size = androidx.compose.ui.geometry.Size(w, 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
            // played + buffered region
            val playedX = (position.toFloat() / duration) * w
            val bufferedX = (buffered.toFloat() / duration) * w
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(playedX, trackY - 2.dp.toPx() / 2),
                size = androidx.compose.ui.geometry.Size((bufferedX - playedX).coerceAtLeast(0f), 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
            drawRoundRect(
                color = ac.primary,
                topLeft = Offset(0f, trackY - 2.dp.toPx() / 2),
                size = androidx.compose.ui.geometry.Size(playedX, 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
            // A-B range
            if (media.loopMode == LoopMode.Range && media.loopEndMs > media.loopStartMs) {
                val ax = (media.loopStartMs.toFloat() / duration) * w
                val bx = (media.loopEndMs.toFloat() / duration) * w
                drawRect(color = ac.primary.copy(alpha = 0.25f), topLeft = Offset(ax, trackY - 4.dp.toPx()), size = androidx.compose.ui.geometry.Size((bx - ax).coerceAtLeast(0f), 8.dp.toPx()))
            }
            // subtitle markers
            markers.forEach { markerMs ->
                val x = (markerMs.toFloat() / duration) * w
                if (x in 0f..w) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(x, trackY - 4.dp.toPx()),
                        end = Offset(x, trackY + 4.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
            // chapter markers (accent, taller)
            chapters.forEach { chapter ->
                val x = (chapter.startMs.toFloat() / duration) * w
                if (x in 0f..w) {
                    drawLine(
                        color = ac.primary.copy(alpha = 0.85f),
                        start = Offset(x, trackY - 6.dp.toPx()),
                        end = Offset(x, trackY + 6.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            // thumb
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(playedX, trackY))
            drawCircle(color = ac.primary, radius = 3.dp.toPx(), center = Offset(playedX, trackY))
            // hover cursor + time preview
            val hover = hoverMs
            if (hover != null) {
                val hx = (hover.toFloat() / duration) * w
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(hx, trackY - 5.dp.toPx()),
                    end = Offset(hx, trackY + 5.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                MediaEngine.formatTime(hoverMs ?: position),
                color = sc.textPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                "${MediaEngine.formatTime(position)} / ${MediaEngine.formatTime(duration)}",
                color = sc.textMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// ============================================
// DICTIONARY PANEL (docked Yomitan-style)
// ============================================

@Composable
fun DictionaryPanel(state: AppState, modifier: Modifier = Modifier) {
    val media = state.media
    val sc = surfaceColors()
    val query = media.lookupQuery ?: ""

    Column(modifier.fillMaxWidth().fillMaxHeight().background(sc.surface)) {
        Row(
            Modifier.fillMaxWidth().background(sc.surfaceInteractive.copy(alpha = 0.4f)).padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Dictionary", color = sc.textPrimary, fontSize = DsType.Label, fontWeight = FontWeight.SemiBold)
                if (query.isNotBlank()) {
                    Text("Lookup: $query", color = sc.textMuted, fontSize = DsType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            DsIconButton(
                icon = Icons.Default.Close,
                onClick = { media.clearLookup() },
                contentDescription = "Close dictionary",
                size = 24.dp
            )
        }
        // Sentence context: the exact subtitle this lookup came from, with
        // previous/next navigation, replay, and the resolved dictionary form
        // when the selected word is inflected (食べました → 食べる).
        val cue = media.activeCue
        if (cue != null) {
            val token = media.selectedTokens.firstOrNull()
            val headword = token?.dictionaryMatch?.entry?.headword
            Row(
                Modifier.fillMaxWidth().background(sc.surfaceElevated.copy(alpha = 0.5f)).padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
            ) {
                DsIconButton(icon = Icons.Default.ChevronLeft, onClick = { media.replayPreviousCue() }, contentDescription = "Previous sentence", size = 26.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                        Text("Current sentence", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
                        if (token != null && headword != null && headword != token.surface) {
                            DsBadge(text = "辞書形: $headword", tint = accent().primary)
                        }
                    }
                    Text(
                        media.displayTextFor(cue),
                        color = sc.textPrimary,
                        fontSize = DsType.Body,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DsIconButton(icon = Icons.Default.Replay, onClick = { media.replayCue() }, contentDescription = "Replay sentence", size = 26.dp)
                DsIconButton(icon = Icons.Default.ChevronRight, onClick = { media.replayNextCue() }, contentDescription = "Next sentence", size = 26.dp)
            }
            // Kaiteyo vocabulary breakdown of this line: every real token
            // with status, reading and one-click lookup / mine actions.
            KaiteyoSentenceBreakdown(
                media = media,
                cue = cue,
                modifier = Modifier.padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
            )
        }

        val groups = remember(query) {
            if (query.isBlank()) emptyList() else state.dictionary.lookup(query)
        }
        DictionaryPopupContent(
            state = state,
            query = query,
            onMine = { payload -> state.mining.openMining(payload) },
            groups = groups
        )
    }
}

// ============================================
// BUFFERING + EMPTY HELPERS
// ============================================
