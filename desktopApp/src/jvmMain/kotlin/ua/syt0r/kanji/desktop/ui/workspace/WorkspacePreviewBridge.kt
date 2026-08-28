package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspacePreview
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import kotlinx.coroutines.delay

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
        val dueToday = state.cards.count { it.srsLevel < 2 }
        val reviews = state.reviewLog.size
        val summaries = state.summaries.size
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
        val collectionCount = state.collections.items.size
        state.updatePreview(WorkspaceView.Library) {
            it.copy(
                title = "Library",
                subtitle = "$deckCount decks · $cardCount cards",
                detail = if (collectionCount > 0) "$collectionCount collections" else "",
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
                detail = if (entries > 0) "${entries.toLocaleString()} entries" else "No dictionaries",
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
    LaunchedEffect(state.media.isPlaying, state.media.currentMediaItem) {
        val item = state.media.currentMediaItem
        val isPlaying = state.media.isPlaying
        state.updatePreview(WorkspaceView.Media) {
            it.copy(
                title = "Media Center",
                subtitle = if (item != null) {
                    item.title ?: item.file.nameWithoutExtension
                } else "No media loaded",
                detail = when {
                    item != null && isPlaying -> "▶ Playing"
                    item != null -> "⏸ Paused"
                    else -> "Open media to begin"
                },
                accentEmoji = if (isPlaying) "🎬" else "🎥",
                progress = if (item != null && item.durationMs > 0) {
                    (state.media.positionMs.toFloat() / item.durationMs).coerceIn(0f, 1f)
                } else -1f
            )
        }
    }

    // ── Review preview ──
    LaunchedEffect(state.reviewSession, state.unifiedReviewActive) {
        val session = state.reviewSession
        val unified = state.unifiedSession
        val active = session != null || state.unifiedReviewActive
        state.updatePreview(WorkspaceView.Review) {
            it.copy(
                title = "Review",
                subtitle = when {
                    unified != null -> "${unified.completed}/${unified.total} completed"
                    session != null -> "${session.remaining} cards remaining"
                    else -> "No active session"
                },
                detail = if (active) "Session in progress" else "Start a review from Library",
                accentEmoji = if (active) "🧠" else "📝",
                progress = when {
                    unified != null && unified.total > 0 -> unified.completed.toFloat() / unified.total
                    else -> -1f
                }
            )
        }
    }

    // ── Collections preview ──
    LaunchedEffect(state.collections.items.size) {
        val count = state.collections.items.size
        state.updatePreview(WorkspaceView.Collections) {
            it.copy(
                title = "Collections",
                subtitle = "$count smart collections",
                detail = "",
                accentEmoji = "📂"
            )
        }
    }

    // ── Mining preview ──
    LaunchedEffect(state.miningStatistics.totalMined) {
        val mined = state.miningStatistics.totalMined
        val today = state.miningStatistics.todayMined
        state.updatePreview(WorkspaceView.Mining) {
            it.copy(
                title = "Mining",
                subtitle = "$mined total mined",
                detail = if (today > 0) "$today mined today" else "Ready to mine",
                accentEmoji = "⛏️"
            )
        }
    }

    // ── Reading preview ──
    LaunchedEffect(state.readingLibrary.documents.size) {
        val docs = state.readingLibrary.documents.size
        state.updatePreview(WorkspaceView.Reading) {
            it.copy(
                title = "Reading",
                subtitle = if (docs > 0) "$docs documents" else "No documents",
                detail = "",
                accentEmoji = "📄"
            )
        }
    }

    // ── Activity Log preview ──
    LaunchedEffect(state.activityLog.entries.size) {
        val entries = state.activityLog.entries.size
        state.updatePreview(WorkspaceView.History) {
            it.copy(
                title = "Activity Log",
                subtitle = "$entries recorded events",
                detail = "",
                accentEmoji = "📋"
            )
        }
    }

    // ── Tags preview ──
    LaunchedEffect(state.cards.size) {
        val tagged = state.cards.count { it.tags.isNotEmpty() }
        state.updatePreview(WorkspaceView.Tags) {
            it.copy(
                title = "Tags & Flags",
                subtitle = "$tagged tagged cards",
                detail = "",
                accentEmoji = "🏷️"
            )
        }
    }

    // ── Mistakes preview ──
    LaunchedEffect(state.reviewLog.size) {
        val mistakes = state.reviewLog.count { it.rating == ua.syt0r.kanji.desktop.model.ReviewRating.Again }
        state.updatePreview(WorkspaceView.Mistakes) {
            it.copy(
                title = "Mistakes",
                subtitle = if (mistakes > 0) "$mistakes cards with errors" else "No mistakes yet",
                detail = "",
                accentEmoji = "❗"
            )
        }
    }

    // ── Exams preview ──
    LaunchedEffect(state.cards.size) {
        state.updatePreview(WorkspaceView.Exams) {
            it.copy(
                title = "Exams",
                subtitle = "Assessment ready",
                detail = "${state.cards.size} cards available",
                accentEmoji = "🎓"
            )
        }
    }

    // ── Writing preview ──
    LaunchedEffect(state.writingSession) {
        val session = state.writingSession
        state.updatePreview(WorkspaceView.Writing) {
            it.copy(
                title = "Writing Practice",
                subtitle = if (session != null) "Session active" else "Handwriting drills",
                detail = "",
                accentEmoji = "✍️"
            )
        }
    }

    // ── Grammar preview ──
    LaunchedEffect(Unit) {
        state.updatePreview(WorkspaceView.Grammar) {
            it.copy(
                title = "Grammar",
                subtitle = "Grammar practice",
                detail = "",
                accentEmoji = "💡"
            )
        }
    }

    // ── Settings preview ──
    LaunchedEffect(Unit) {
        state.updatePreview(WorkspaceView.Settings) {
            it.copy(
                title = "Settings",
                subtitle = "Configuration",
                detail = "",
                accentEmoji = "⚙️"
            )
        }
    }

    // ── Sync preview ──
    LaunchedEffect(state.syncBusy, state.lastSyncAt) {
        state.updatePreview(WorkspaceView.Sync) {
            it.copy(
                title = "Sync",
                subtitle = if (state.syncBusy) "Syncing…" else "Cloud sync",
                detail = state.lastSyncAt?.toString()?.take(19)?.replace("T", " ") ?: "Not yet synced",
                accentEmoji = "🔄"
            )
        }
    }

    // ── Game preview ──
    LaunchedEffect(Unit) {
        state.updatePreview(WorkspaceView.Game) {
            it.copy(
                title = "Kaiteyo World",
                subtitle = "Game world",
                detail = "",
                accentEmoji = "🎮"
            )
        }
    }

    // ── Periodic refresh for time-sensitive data (media position, session progress) ──
    LaunchedEffect(Unit) {
        while (true) {
            delay(LaunchpadMotion.PREVIEW_UPDATE_INTERVAL_MS)
            // Media position
            val item = state.media.currentMediaItem
            if (item != null && state.media.isPlaying) {
                state.updatePreview(WorkspaceView.Media) {
                    it.copy(
                        progress = if (item.durationMs > 0) {
                            (state.media.positionMs.toFloat() / item.durationMs).coerceIn(0f, 1f)
                        } else -1f
                    )
                }
            }
            // Review progress
            val unified = state.unifiedSession
            if (unified != null) {
                state.updatePreview(WorkspaceView.Review) {
                    it.copy(
                        progress = if (unified.total > 0) unified.completed.toFloat() / unified.total else -1f
                    )
                }
            }
        }
    }
}
