package ua.syt0r.kanji.desktop.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.media.MediaEngine

// ============================================
// PERSISTENT MINI PLAYER
// A compact floating player that keeps media
// playing while the user browses other parts of
// Kaiteyo. Playback is owned by the engine, not
// the view, so leaving Media never stops audio;
// with the mpv backend the video window also
// stays alive. One click expands back to the
// full Media workspace.
// ============================================

@Composable
fun MediaMiniPlayer(state: AppState) {
    val media = state.media
    val sc = surfaceColors()
    val ac = accent()
    val item = media.currentItem

    Box(
        Modifier
            .fillMaxSize()
            .padding(DsSpacing.Lg),
        contentAlignment = Alignment.BottomEnd
    ) {
        Row(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(DsRadius.Xl))
                .background(sc.surfaceElevated)
                .border(androidx.compose.ui.unit.Dp.Hairline, sc.border, RoundedCornerShape(DsRadius.Xl))
                .clickable { state.currentView = WorkspaceView.Media }
                .padding(DsSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ac.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item?.kind == ua.syt0r.kanji.desktop.engine.media.MediaKind.Video) Icons.Default.Movie
                    else Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = ac.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    item?.name ?: "Media",
                    color = sc.textPrimary,
                    fontSize = DsType.Body,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${MediaEngine.formatTime(media.positionMs)} / ${MediaEngine.formatTime(media.durationMs)} · ${media.speed}x",
                        color = sc.textMuted,
                        fontSize = DsType.Caption,
                        modifier = Modifier.weight(1f)
                    )
                    if (media.condensedPlayback) {
                        Text("· condensed", color = sc.textMuted, fontSize = DsType.Caption)
                    }
                }
                Slider(
                    value = media.positionMs.toFloat().coerceIn(0f, media.durationMs.coerceAtLeast(1).toFloat()),
                    onValueChange = { media.seekTo(it.toLong()) },
                    valueRange = 0f..media.durationMs.coerceAtLeast(1).toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            DsIconButton(
                icon = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                onClick = { media.togglePlay() },
                contentDescription = "Play / Pause",
                tint = ac.primary,
                size = 36.dp
            )
            Spacer(Modifier.width(DsSpacing.Xs))
            DsIconButton(
                icon = Icons.Default.OpenInFull,
                onClick = { state.currentView = WorkspaceView.Media },
                contentDescription = "Open in Media workspace",
                size = 30.dp
            )
            DsIconButton(
                icon = Icons.Default.Close,
                onClick = { media.closeMiniPlayer() },
                contentDescription = "Close mini player (keeps playing)",
                size = 30.dp
            )
        }
    }
}
