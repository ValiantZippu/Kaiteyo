package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspacePreview
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.model.SrsStatus
import kotlinx.coroutines.delay
import java.io.File

/**
 * Centralized preview bridge: observes AppState and pushes real data into
 * [AppState.workspacePreviews] so the Launchpad always reflects actual
 * application state. Single source of truth — no per-screen duplication.
 *
 * Drop this composable inside [KaiteyoWorkspace] and it runs invisibly.
 */
@Composable
fun WorkspacePreviewBridge(state: AppState) {
    // ── Dashboard preview ──
    LaunchedEffect(state.cards.size, state.reviewLog.size) {
        val totalCards = state.cards.size
        val dueToday = state.cards.count {
            it.status == SrsStatus.New || it.status == SrsStatus.Learning
        }
        val reviews = state.reviewLog.size
        state.updatePreview(WorkspaceView.Dashboard) {
            it.copy(
                title = "Dashboard",
                subtitle = "$totalCards cards loaded",
                detail = if (dueToday > 0) "$dueToday due today" else "All caught up",
                accentEmoji = "📊",
                progress = if (totalCards > 0) (reviews.toFloat() / totalCards).coerceIn(0f, 1f) else -1f
            )
        }
    }

    // ── Library preview ──
    LaunchedEffect(state.cards.size, state.library.decks.size) {
        val deckCount = state.library.decks.size
        val cardCount = state.cards.size
        state.updatePreview(WorkspaceView.Library) {
            it.copy(
                title = "Library",
                subtitle = "$deckCount decks · $cardCount cards",
                detail = "",
                accentEmoji = "📚"
            )
        }
    }

    // ── Dictionary preview ──
    LaunchedEffect(state.dictionary.installed.size) {
        val dicts = state.dictionary.installed.size
        val entries = state.dictionary.installed.sumOf { it.entryCount }
        state.updatePreview(WorkspaceView.Dictionary) {
            it.copy(
                title = "Dictionary",
                subtitle = "$dicts dictionaries installed",
                detail = if (entries > 0) "$entries entries" else "No dictionaries",
                accentEmoji = "📖"
            )
        }
    }

    // ── Statistics preview ──
    LaunchedEffect(state.reviewLog.size, state.summaries.size) {
        val totalReviews = state.reviewLog.size
        val streakDays = state.summaries.size
        state.updatePreview(WorkspaceView.Statistics) {
            it.copy(
                title = "Statistics",
                subtitle = "$totalReviews total reviews",
                detail = if (streakDays > 0) "$streakDays study days" else "No study data yet",
                accentEmoji = "📈",
                progress = if (streakDays > 0) (streakDays.toFloat() / 30f).coerceIn(0f, 1f) else -1f
            )
        }
    }

    // ── Media preview ──
    LaunchedEffect(state.media.isPlaying) {
        val isPlaying = state.media.isPlaying
        val item = state.media.currentItem
        val path = item?.path
        val mediaName = if (path != null) {
            try { File(path).nameWithoutExtension } catch (_: Exception) { "Media" }
        } else null
        state.updatePreview(WorkspaceView.Media) {
            it.copy(
                title = "Media Center",
                subtitle = if (mediaName != null) mediaName else "No media loaded",
                detail = when {
                    mediaName != null && isPlaying -> "▶ Playing"
                    mediaName != null -> "⏸ Paused"
                    else -> "Open media to begin"
                },
                accentEmoji = if (isPlaying) "🎬" else "🎥",
                progress = -1f
            )
        }
    }

    // ── Mining preview ──
    LaunchedEffect(state.miningStatistics.totalMined) {
        val mined = state.miningStatistics.totalMined
        state.updatePreview(WorkspaceView.Mining) {
            it.copy(
                title = "Mining",
                subtitle = "$mined total mined",
                detail = if (mined > 0) "Mining active" else "Ready to mine",
                accentEmoji = "⛏️"
            )
        }
    }

    // ── Settings preview (static) ──
    LaunchedEffect(Unit) {
        state.updatePreview(WorkspaceView.Settings) {
            it.copy(
                title = "Settings",
                subtitle = "Configure Kaiteyo",
                detail = "",
                accentEmoji = "⚙️"
            )
        }
    }

    // Keep time-sensitive previews fresh every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            // Re-trigger the media preview for playback position
            val item = state.media.currentItem
            if (item != null) {
                state.updatePreview(WorkspaceView.Media) {
                    it.copy(progress = -1f) // refresh trigger
                }
            }
        }
    }
}
