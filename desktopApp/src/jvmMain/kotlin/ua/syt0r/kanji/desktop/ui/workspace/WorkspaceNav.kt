package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.NavPosition
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsMenuDivider
import ua.syt0r.kanji.desktop.designsystem.DsMenuItem
import ua.syt0r.kanji.desktop.designsystem.DsMenuPanel
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import kotlin.math.roundToInt

// ============================================
// KAITEYO NAVIGATION — redesigned workspace nav
// Adaptive dock (left/right/top/bottom), icon-only
// collapse, position picker, and a floating launcher.
// ============================================

private data class NavItem(
    val view: WorkspaceView,
    val icon: ImageVector
)

private val navGroups: List<Pair<String, List<NavItem>>> = listOf(
    "Study" to listOf(
        NavItem(WorkspaceView.Dashboard, Icons.Default.SpaceDashboard),
        NavItem(WorkspaceView.Browser, Icons.Default.GridView),
        NavItem(WorkspaceView.Review, Icons.Default.PlayArrow),
        NavItem(WorkspaceView.Collections, Icons.Default.Folder)
    ),
    "Organize" to listOf(
        NavItem(WorkspaceView.Tags, Icons.Default.Sell),
        NavItem(WorkspaceView.Statistics, Icons.Default.BarChart),
        NavItem(WorkspaceView.History, Icons.Default.History)
    ),
    "System" to listOf(
        NavItem(WorkspaceView.Transfer, Icons.Default.ImportExport),
        NavItem(WorkspaceView.Sync, Icons.Default.Sync),
        NavItem(WorkspaceView.Shortcuts, Icons.Default.Keyboard),
        NavItem(WorkspaceView.Plugins, Icons.Default.Extension),
        NavItem(WorkspaceView.ThemeStudio, Icons.Default.Palette),
        NavItem(WorkspaceView.Settings, Icons.Default.Settings),
        NavItem(WorkspaceView.Contributions, Icons.Default.Favorite)
    )
)

@Composable
private fun navBadge(state: AppState, view: WorkspaceView): String? = when (view) {
    WorkspaceView.Review -> state.dueCount().takeIf { it > 0 }?.toString()
    else -> null
}

// ============================================
// LOGO MARK
// ============================================

@Composable
private fun DsLogoMark(modifier: Modifier = Modifier) {
    val ac = accent()
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(ac.primary),
        contentAlignment = Alignment.Center
    ) {
        Text("K", color = ac.onPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.Bold)
    }
}

// ============================================
// VERTICAL RAIL (left / right edge)
// ============================================

@Composable
fun DsNavRail(
    state: AppState,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()

    Column(
        modifier = Modifier
            .width(if (collapsed) 64.dp else 232.dp)
            .fillMaxHeight()
            .background(sc.background)
            .padding(vertical = DsSpacing.Lg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DsLogoMark()
            if (!collapsed) {
                Spacer(Modifier.width(DsSpacing.Sm))
                Column(Modifier.weight(1f)) {
                    Text("Kaiteyo", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
                    Text("Desktop", color = ac.primary, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(DsSpacing.Lg))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            navGroups.forEach { (groupLabel, items) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (!collapsed) {
                        Text(
                            text = groupLabel.uppercase(),
                            color = sc.textMuted,
                            fontSize = DsType.Caption,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs)
                        )
                    } else {
                        Spacer(Modifier.height(DsSpacing.Xs))
                    }
                    items.forEach { item -> DsNavItem(item, state, collapsed) }
                }
                Spacer(Modifier.height(DsSpacing.Xs))
            }
        }

        Spacer(Modifier.height(DsSpacing.Md))

        if (collapsed) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
            ) {
                DsPositionPicker(state)
                DsIconButton(
                    icon = Icons.Default.Menu,
                    onClick = onToggleCollapsed,
                    contentDescription = "Expand navigation"
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DsSpacing.Md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.dueCount().toString(),
                        color = sc.textPrimary,
                        fontSize = DsType.BodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text("cards due", color = sc.textMuted, fontSize = DsType.Caption)
                }
                DsPositionPicker(state)
                DsIconButton(
                    icon = Icons.Default.MenuOpen,
                    onClick = onToggleCollapsed,
                    contentDescription = "Collapse navigation"
                )
            }
        }
    }
}

@Composable
private fun DsNavItem(item: NavItem, state: AppState, collapsed: Boolean) {
    val sc = surfaceColors()
    val ac = accent()
    val selected = state.currentView == item.view
    val badge = navBadge(state, item.view)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = when {
        selected -> ac.primary.copy(alpha = 0.16f)
        hovered -> sc.surfaceInteractive.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, onClick = { state.currentView = item.view })
            .hoverable(interaction)
            .padding(horizontal = if (collapsed) 0.dp else DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) ac.primary else Color.Transparent)
        )
        Spacer(Modifier.width(DsSpacing.Sm))
        Icon(
            item.icon,
            contentDescription = item.view.label,
            tint = if (selected) ac.primary else sc.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        if (!collapsed) {
            Spacer(Modifier.width(DsSpacing.Md))
            Text(
                text = item.view.label,
                color = if (selected) ac.primary else sc.textSecondary,
                fontSize = DsType.Body,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (badge != null) {
                DsBadge(text = badge, tint = if (selected) ac.primary else sc.textMuted)
            }
        }
    }
}

// ============================================
// HORIZONTAL BAR (top / bottom edge)
// ============================================

@Composable
fun DsNavBar(
    state: AppState,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    compact: Boolean = false
) {
    val sc = surfaceColors()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (collapsed || compact) 52.dp else 64.dp)
            .background(sc.background)
            .padding(horizontal = DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!compact) {
            DsLogoMark()
            if (!collapsed) {
                Spacer(Modifier.width(DsSpacing.Sm))
                Text("Kaiteyo", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(DsSpacing.Lg))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navGroups.forEachIndexed { groupIndex, (_, items) ->
                if (groupIndex > 0) NavGroupSeparator()
                items.forEach { item -> DsNavPill(item, state, collapsed) }
            }
        }
        if (!compact) {
            DsPositionPicker(state)
            DsIconButton(
                icon = if (collapsed) Icons.Default.Menu else Icons.Default.MenuOpen,
                onClick = onToggleCollapsed,
                contentDescription = if (collapsed) "Expand navigation" else "Collapse navigation"
            )
        }
    }
}

@Composable
private fun DsNavPill(item: NavItem, state: AppState, collapsed: Boolean) {
    val sc = surfaceColors()
    val ac = accent()
    val selected = state.currentView == item.view
    val badge = navBadge(state, item.view)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = when {
        selected -> ac.primary.copy(alpha = 0.16f)
        hovered -> sc.surfaceInteractive.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, onClick = { state.currentView = item.view })
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        Icon(
            item.icon,
            contentDescription = item.view.label,
            tint = if (selected) ac.primary else sc.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        if (!collapsed) {
            Text(
                text = item.view.label,
                color = if (selected) ac.primary else sc.textSecondary,
                fontSize = DsType.Body,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        if (badge != null && !collapsed) {
            DsBadge(text = badge, tint = if (selected) ac.primary else sc.textMuted)
        }
    }
}

@Composable
private fun NavGroupSeparator() {
    val sc = surfaceColors()
    Box(
        Modifier
            .padding(horizontal = DsSpacing.Sm)
            .width(1.dp)
            .height(20.dp)
            .background(sc.border.copy(alpha = 0.5f))
    )
}

// ============================================
// POSITION PICKER
// ============================================

@Composable
private fun DsPositionPicker(state: AppState) {
    var open by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .onGloballyPositioned { if (anchor != it) anchor = it }
    ) {
        DsIconButton(
            icon = Icons.Default.Tune,
            onClick = { open = true },
            contentDescription = "Navigation position",
            tint = accent().primary
        )
    }

    val coords = anchor
    if (open && coords != null) {
        val windowPos = coords.positionInWindow()
        Popup(
            onDismissRequest = { open = false },
            offset = IntOffset(windowPos.x.roundToInt(), windowPos.y.roundToInt() + coords.size.height),
            properties = PopupProperties(focusable = true)
        ) {
            DsMenuPanel(
                menuItems = NavPosition.entries.map { option ->
                    DsMenuItem(
                        label = option.label,
                        icon = positionIcon(option),
                        checked = state.navPosition == option,
                        onAction = { state.updateNavPosition(option) }
                    )
                },
                onDismiss = { open = false }
            )
        }
    }
}

private fun positionIcon(position: NavPosition): ImageVector = when (position) {
    NavPosition.Left -> Icons.Default.KeyboardArrowLeft
    NavPosition.Right -> Icons.Default.KeyboardArrowRight
    NavPosition.Top -> Icons.Default.KeyboardArrowUp
    NavPosition.Bottom -> Icons.Default.KeyboardArrowDown
}

// ============================================
// FLOATING LAUNCHER
// ============================================

@Composable
fun DsFloatingLauncher(state: AppState, onOpenPalette: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sc = surfaceColors()
    val ac = accent()

    Box(Modifier.fillMaxSize()) {
        if (expanded) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.16f))
                    .clickable { expanded = false }
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(DsSpacing.Xl),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            if (expanded) {
                DsFloatingMenu(state, onNavigate = { expanded = false }, onOpenPalette = onOpenPalette)
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(ac.primary)
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = if (expanded) "Close launcher" else "Open launcher",
                    tint = ac.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun DsFloatingMenu(state: AppState, onNavigate: () -> Unit, onOpenPalette: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val glass = state.settings.getBool("appearance.glass")
    val bg = if (glass) Color.Black.copy(alpha = 0.72f) else sc.surfaceElevated
    val shape = RoundedCornerShape(DsRadius.Xl)

    Column(
        modifier = Modifier
            .width(288.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, sc.border.copy(alpha = 0.3f), shape)
            .padding(DsSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        navGroups.forEach { (groupLabel, items) ->
            Text(
                text = groupLabel.uppercase(),
                color = sc.textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs)
            )
            items.forEach { item ->
                val selected = state.currentView == item.view
                val badge = navBadge(state, item.view)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DsRadius.Sm))
                        .background(if (selected) ac.primary.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { state.currentView = item.view; onNavigate() }
                        .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = if (selected) ac.primary else sc.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(DsSpacing.Sm))
                    Text(
                        text = item.view.label,
                        color = if (selected) ac.primary else sc.textPrimary,
                        fontSize = DsType.Body,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (badge != null) {
                        DsBadge(text = badge, tint = if (selected) ac.primary else sc.textMuted)
                    }
                }
            }
            Spacer(Modifier.height(DsSpacing.Sm))
        }
        DsMenuDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DsRadius.Sm))
                .clickable { onOpenPalette(); onNavigate() }
                .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = sc.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(DsSpacing.Sm))
            Text("Command palette", color = sc.textPrimary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
            Text("Ctrl K", color = sc.textMuted, fontSize = DsType.Caption)
        }
    }
}
