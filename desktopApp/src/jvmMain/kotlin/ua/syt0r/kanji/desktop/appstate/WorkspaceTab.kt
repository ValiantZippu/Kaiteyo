package ua.syt0r.kanji.desktop.appstate

import kotlinx.serialization.Serializable

/**
 * One open workspace session — a browser-style tab.
 *
 * Every tab is an independent view instance: it remembers which [view] it
 * shows plus the per-instance state of that view (browser/library search,
 * view mode, selection) so switching tabs restores exactly where you were.
 * Tabs persist across restarts via the SettingsEngine (`workspace.tabs`).
 */
@Serializable
data class WorkspaceTab(
    val id: String,
    val view: WorkspaceView,
    val title: String = view.label,
    // Snapshot of the tab's browser/library state.
    val browserQuery: String = "",
    val browserViewMode: BrowserViewMode = BrowserViewMode.Grid,
    val browserShowPreview: Boolean = true,
    val selectedCardId: String? = null,
    val selectedCardIds: List<String> = emptyList()
)

/** Persisted workspace session: open tabs + the active one. */
@Serializable
data class WorkspaceTabsPayload(
    val tabs: List<WorkspaceTab> = emptyList(),
    val activeTabId: String? = null
)
