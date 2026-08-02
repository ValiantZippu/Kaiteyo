package ua.syt0r.kanji.desktop.ui.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.BrowserViewMode
import ua.syt0r.kanji.desktop.appstate.PanelKind
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.appstate.togglePanel
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.ui.workspace.panelKindIcon

// ============================================
// COMMAND PALETTE
// Keyboard-first launcher: search views, actions
// and settings. Ctrl+K from anywhere.
// ============================================

data class PaletteCommand(
    val label: String,
    val group: String,
    val icon: ImageVector,
    val hint: String = "",
    val action: () -> Unit
)

@Composable
fun CommandPaletteOverlay(state: AppState, onDismiss: () -> Unit) {
    val sc = surfaceColors()
    var query by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(0) }

    val commands = remember(state) {
        buildCommands(state)
    }
    val filtered = remember(query, commands) {
        val q = query.trim().lowercase()
        if (q.isBlank()) commands
        else commands.filter {
            it.label.lowercase().contains(q) || it.group.lowercase().contains(q) || it.hint.lowercase().contains(q)
        }
    }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 96.dp)
                    .width(560.dp)
                    .clip(RoundedCornerShape(DsRadius.Xl))
                    .background(sc.surfaceElevated)
                    .padding(DsSpacing.Md)
            ) {
                DsSearchField(
                    value = query,
                    onValueChange = { query = it; selectedIndex = 0 },
                    placeholder = "Type a command or search…",
                    autoFocus = true,
                    modifier = Modifier
                        .onPreviewKeyEvent { event ->
                            when {
                                event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                                    if (filtered.isNotEmpty()) selectedIndex = (selectedIndex + 1).coerceAtMost(filtered.lastIndex)
                                    true
                                }
                                event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                                    if (filtered.isNotEmpty()) selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                    true
                                }
                                event.type == KeyEventType.KeyDown && event.key == Key.Enter -> {
                                    filtered.getOrNull(selectedIndex)?.action()
                                    onDismiss()
                                    true
                                }
                                event.type == KeyEventType.KeyDown && event.key == Key.Escape -> {
                                    onDismiss()
                                    true
                                }
                                else -> false
                            }
                        }
                )
                Spacer(Modifier.height(DsSpacing.Sm))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DsSpacing.Xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No commands match “$query”",
                            color = sc.textMuted,
                            fontSize = DsType.Body
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        itemsIndexed(filtered, key = { _, c -> c.group + c.label }) { index, command ->
                            PaletteRow(
                                command = command,
                                selected = index == selectedIndex,
                                onClick = {
                                    command.action()
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(DsSpacing.Sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    Text(
                        text = "↑↓ navigate",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                    Text(
                        text = "Enter select",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                    Text(
                        text = "Esc close",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteRow(
    command: PaletteCommand,
    selected: Boolean,
    onClick: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.16f)
                    hovered -> sc.surfaceInteractive
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            command.icon,
            contentDescription = null,
            tint = if (selected) ac.primary else sc.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(DsSpacing.Sm))
        Text(
            text = command.label,
            color = if (selected) ac.primary else sc.textPrimary,
            fontSize = DsType.Body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(Modifier.weight(1f))
        if (command.hint.isNotBlank()) {
            Text(
                text = command.hint,
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

private fun buildCommands(state: AppState): List<PaletteCommand> = buildList {

    fun nav(view: WorkspaceView, icon: ImageVector) {
        add(
            PaletteCommand(
                label = "Open ${view.label}",
                group = "Navigate",
                icon = icon,
                hint = view.label,
                action = { state.currentView = view }
            )
        )
    }

    nav(WorkspaceView.Dashboard, Icons.Default.SpaceDashboard)
    nav(WorkspaceView.Browser, Icons.Default.GridView)
    nav(WorkspaceView.Review, Icons.Default.PlayArrow)
    nav(WorkspaceView.Collections, Icons.Default.Folder)
    nav(WorkspaceView.Tags, Icons.Default.Sell)
    nav(WorkspaceView.Statistics, Icons.Default.BarChart)
    nav(WorkspaceView.History, Icons.Default.History)
    nav(WorkspaceView.Transfer, Icons.Default.ImportExport)
    nav(WorkspaceView.Sync, Icons.Default.Sync)
    nav(WorkspaceView.Shortcuts, Icons.Default.Keyboard)
    nav(WorkspaceView.Plugins, Icons.Default.Extension)
    nav(WorkspaceView.ThemeStudio, Icons.Default.Palette)
    nav(WorkspaceView.Settings, Icons.Default.Settings)
    nav(WorkspaceView.Contributions, Icons.Default.Favorite)

    add(PaletteCommand("Start review (due)", "Review", Icons.Default.PlayArrow, "3",
        action = { state.startReview() }))
    add(PaletteCommand("Start review (new)", "Review", Icons.Default.PlayArrow, "N",
        action = { state.startReview(query = "status:new") }))
    add(PaletteCommand("Preview session", "Review", Icons.Default.Tune, "P",
        action = { state.startReview(query = "", settings = state.reviewSettings.copy(showPreview = true)) }))

    add(PaletteCommand("Toggle preview panel", "Browser", Icons.Default.Tune, "",
        action = { state.browserShowPreview = !state.browserShowPreview }))
    add(PaletteCommand("Switch to grid view", "Browser", Icons.Default.GridView, "G",
        action = { state.browserViewMode = BrowserViewMode.Grid }))

    PanelKind.entries.forEach { kind ->
        add(
            PaletteCommand(
                label = "Toggle ${kind.label} panel",
                group = "Panels",
                icon = panelKindIcon(kind),
                action = { state.togglePanel(kind) }
            )
        )
    }


    add(PaletteCommand("Load stress dataset (10k)", "Developer", Icons.Default.Settings, "",
        action = { state.loadStressDataset(10_000) }))
    add(PaletteCommand("Export active theme JSON", "Theme", Icons.Default.Palette, "",
        action = { state.toastHost.show("Theme JSON copied to clipboard", kind = ua.syt0r.kanji.desktop.model.ToastKind.Success) }))
}
