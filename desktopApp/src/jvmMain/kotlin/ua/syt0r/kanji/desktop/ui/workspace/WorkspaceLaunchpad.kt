package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspacePreview
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors

// ============================================
// KAITEYO TASK VIEW — workspace overview
// Replaces the old navigation menu Launchpad.
// Shows live workspace previews in a spatial
// grid — each card represents a real workspace
// with its current state.
// ============================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceLaunchpad(
    state: AppState,
    onSwitchTo: (WorkspaceView) -> Unit,
    onDismiss: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()

    // Build the workspace list: active workspaces first, then pinned/core, then secondary
    val previews = remember(state.workspacePreviews, state.tabs) {
        buildWorkspaceCards(state)
    }

    // Keyboard navigation
    var focusIndex by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { List(24) { FocusRequester() } }

    // Focus the first card on open
    LaunchedEffect(Unit) {
        if (previews.isNotEmpty()) {
            focusRequesters[0].requestFocus()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelWidth = minOf(960.dp, (maxWidth - 48.dp).coerceAtLeast(400.dp))
        val panelHeight = minOf(680.dp, (maxHeight - 48.dp).coerceAtLeast(400.dp))

        Column(
            modifier = Modifier
                .width(panelWidth)
                .height(panelHeight)
                .shadow(48.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(sc.surfaceElevated.copy(alpha = 0.98f))
                .border(1.dp, sc.border.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                .padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORKSPACES",
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                // Active workspace indicator
                state.activeTab?.let { tab ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ac.primary)
                        )
                        Text(
                            text = tab.view.label,
                            color = ac.primary,
                            fontSize = DsType.Caption,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Workspace cards grid
            FlowRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md),
                maxItemsInEachRow = 3
            ) {
                previews.forEachIndexed { index, card ->
                    WorkspaceCard(
                        preview = card.preview,
                        icon = card.icon,
                        isActive = card.isActive,
                        hasActivity = card.hasActivity,
                        index = index,
                        focusRequester = focusRequesters.getOrElse(index) { focusRequesters.last() },
                        onClick = {
                            onSwitchTo(card.preview.view)
                            onDismiss()
                        },
                        onKeyDown = { key ->
                            when (key) {
                                Key.DirectionRight -> {
                                    val next = (index + 1).coerceAtMost(previews.lastIndex)
                                    focusIndex = next
                                    focusRequesters.getOrElse(next) { focusRequesters.last() }.requestFocus()
                                    true
                                }
                                Key.DirectionLeft -> {
                                    val prev = (index - 1).coerceAtLeast(0)
                                    focusIndex = prev
                                    focusRequesters.getOrElse(prev) { focusRequesters.last() }.requestFocus()
                                    true
                                }
                                Key.DirectionDown -> {
                                    // Move to next row (3 cards per row)
                                    val next = (index + 3).coerceAtMost(previews.lastIndex)
                                    focusIndex = next
                                    focusRequesters.getOrElse(next) { focusRequesters.last() }.requestFocus()
                                    true
                                }
                                Key.DirectionUp -> {
                                    // Move to previous row
                                    val prev = (index - 3).coerceAtLeast(0)
                                    focusIndex = prev
                                    focusRequesters.getOrElse(prev) { focusRequesters.last() }.requestFocus()
                                    true
                                }
                                Key.Enter, Key.Spacebar -> {
                                    onSwitchTo(card.preview.view)
                                    onDismiss()
                                    true
                                }
                                else -> false
                            }
                        }
                    )
                }
            }

            // Footer hint
            Text(
                text = "Click a workspace to switch • Arrow keys to navigate • Esc to close",
                color = sc.textMuted.copy(alpha = 0.6f),
                fontSize = DsType.Caption,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================
// WORKSPACE CARD
// A miniature window preview for each workspace.
// ============================================

@Composable
private fun WorkspaceCard(
    preview: WorkspacePreview,
    icon: ImageVector,
    isActive: Boolean,
    hasActivity: Boolean,
    index: Int,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onKeyDown: (Key) -> Boolean
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            hovered -> 1.03f
            isActive -> 1.01f
            else -> 1f
        },
        animationSpec = LaunchpadMotion.cardHoverScale,
        label = "cardScale"
    )
    val elevation by animateDpAsState(
        targetValue = when {
            hovered -> 12.dp
            isActive -> 8.dp
            else -> 4.dp
        },
        label = "cardElevation"
    )

    val cardShape = RoundedCornerShape(DsRadius.Lg)

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(180.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation, cardShape)
            .clip(cardShape)
            .background(
                when {
                    isActive -> ac.primary.copy(alpha = 0.08f)
                    hovered -> sc.surfaceInteractive.copy(alpha = 0.8f)
                    else -> sc.surface
                }
            )
            .then(
                if (isActive) Modifier.border(1.5.dp, ac.primary.copy(alpha = 0.3f), cardShape)
                else if (hovered) Modifier.border(1.dp, sc.border.copy(alpha = 0.4f), cardShape)
                else Modifier
            )
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) onKeyDown(keyEvent.key)
                else false
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(DsSpacing.Md),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            // Top row: icon + title + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                // Icon badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(DsRadius.Sm))
                        .background(
                            if (isActive) ac.primary.copy(alpha = 0.15f)
                            else sc.surfaceInteractive
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) ac.primary else sc.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preview.title,
                        color = sc.textPrimary,
                        fontSize = DsType.Body,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (preview.subtitle.isNotBlank()) {
                        Text(
                            text = preview.subtitle,
                            color = sc.textMuted,
                            fontSize = DsType.Caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Active indicator
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ac.primary)
                    )
                }
            }

            // Preview content area — shows workspace-specific state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DsRadius.Sm))
                    .background(sc.surfaceInteractive.copy(alpha = 0.5f))
                    .padding(DsSpacing.Sm),
                contentAlignment = Alignment.CenterStart
            ) {
                WorkspacePreviewContent(preview)
            }

            // Bottom: detail line + optional emoji
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (preview.detail.isNotBlank()) {
                    Text(
                        text = preview.detail,
                        color = sc.textMuted,
                        fontSize = DsType.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (preview.accentEmoji.isNotBlank()) {
                    Text(
                        text = preview.accentEmoji,
                        fontSize = 12.sp
                    )
                }
            }

            // Progress bar if available
            if (preview.progress in 0f..1f) {
                LinearProgressIndicator(
                    progress = { preview.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ac.primary,
                    trackColor = sc.surfaceInteractive,
                )
            }
        }
    }
}

// ============================================
// WORKSPACE PREVIEW CONTENT
// Lightweight state representation for each workspace.
// ============================================

@Composable
private fun WorkspacePreviewContent(preview: WorkspacePreview) {
    val sc = surfaceColors()
    val ac = accent()

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        when (preview.view) {
            WorkspaceView.Dashboard -> {
                Text(
                    text = "Dashboard",
                    color = sc.textPrimary,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
                if (preview.detail.isNotBlank()) {
                    Text(
                        text = preview.detail,
                        color = if (preview.detail.contains("caught up")) ac.primary else sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
            WorkspaceView.Library -> {
                Text(
                    text = "Library",
                    color = sc.textPrimary,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
                if (preview.subtitle.isNotBlank()) {
                    Text(
                        text = preview.subtitle,
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                if (preview.detail.isNotBlank()) {
                    Text(
                        text = preview.detail,
                        color = ac.primary,
                        fontSize = DsType.Caption,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            WorkspaceView.Media -> {
                Text(
                    text = preview.subtitle.ifBlank { "Media Center" },
                    color = sc.textPrimary,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
                if (preview.detail.isNotBlank()) {
                    Text(
                        text = preview.detail,
                        color = if (preview.detail.contains("Playing")) ac.primary else sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
            WorkspaceView.Statistics -> {
                Text(
                    text = "Statistics",
                    color = sc.textPrimary,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
                if (preview.detail.isNotBlank()) {
                    Text(
                        text = preview.detail,
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
            WorkspaceView.Dictionary -> {
                Text(
                    text = preview.subtitle.ifBlank { "Dictionary" },
                    color = sc.textPrimary,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
                if (preview.detail.isNotBlank()) {
                    Text(
                        text = preview.detail,
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
            WorkspaceView.Review -> {
                Text(
                    text = preview.subtitle.ifBlank { "Review Session" },
                    color = sc.textPrimary,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
                if (preview.detail.isNotBlank()) {
                    Text(
                        text = preview.detail,
                        color = if (preview.detail.contains("progress")) ac.primary else sc.textMuted,
                        fontSize = DsType.Caption,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            WorkspaceView.Game -> {
                Text(
                    text = "Kaiteyo World",
                    color = sc.textPrimary,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = preview.detail.ifBlank { "Game world" },
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            else -> {
                Text(
                    text = preview.subtitle.ifBlank { preview.view.label },
                    color = sc.textPrimary,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
                if (preview.detail.isNotBlank()) {
                    Text(
                        text = preview.detail,
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
        }
    }
}

// ============================================
// WORKSPACE CARD BUILDER
// ============================================

private data class WorkspaceCardData(
    val preview: WorkspacePreview,
    val icon: ImageVector,
    val isActive: Boolean,
    val hasActivity: Boolean
)

private fun buildWorkspaceCards(state: AppState): List<WorkspaceCardData> {
    val activeView = state.currentView
    val now = System.currentTimeMillis()
    val fiveMinutesAgo = now - 5 * 60 * 1000

    // Core workspaces that always appear
    val coreViews = listOf(
        WorkspaceView.Dashboard,
        WorkspaceView.Library,
        WorkspaceView.Dictionary,
        WorkspaceView.Statistics,
        WorkspaceView.Media,
        WorkspaceView.Settings
    )

    // Study tools
    val studyViews = listOf(
        WorkspaceView.Review,
        WorkspaceView.Writing,
        WorkspaceView.Grammar,
        WorkspaceView.Exams
    )

    // Materials & tools
    val toolViews = listOf(
        WorkspaceView.Collections,
        WorkspaceView.Tags,
        WorkspaceView.Mistakes,
        WorkspaceView.History,
        WorkspaceView.LearningBrowser,
        WorkspaceView.Ocr,
        WorkspaceView.Mining,
        WorkspaceView.Integrations,
        WorkspaceView.Game
    )

    val iconMap = mapOf(
        WorkspaceView.Dashboard to Icons.Default.SpaceDashboard,
        WorkspaceView.Library to Icons.Default.LibraryBooks,
        WorkspaceView.Dictionary to Icons.Default.MenuBook,
        WorkspaceView.Statistics to Icons.Default.BarChart,
        WorkspaceView.Media to Icons.Default.VideoLibrary,
        WorkspaceView.Settings to Icons.Default.Settings,
        WorkspaceView.Review to Icons.Default.PlayArrow,
        WorkspaceView.Writing to Icons.Default.Create,
        WorkspaceView.Grammar to Icons.Default.Lightbulb,
        WorkspaceView.Exams to Icons.Default.School,
        WorkspaceView.Collections to Icons.Default.Bookmarks,
        WorkspaceView.Tags to Icons.Default.Search,
        WorkspaceView.Mistakes to Icons.Default.Search,
        WorkspaceView.History to Icons.Default.History,
        WorkspaceView.LearningBrowser to Icons.Default.Search,
        WorkspaceView.Ocr to Icons.Default.Camera,
        WorkspaceView.Mining to Icons.Default.Search,
        WorkspaceView.Integrations to Icons.Default.Settings,
        WorkspaceView.Game to Icons.Default.Apps
    )

    val results = mutableListOf<WorkspaceCardData>()

    // Active workspace first
    val activePreview = state.workspacePreviews[activeView]
        ?: WorkspacePreview(view = activeView)
    results.add(
        WorkspaceCardData(
            preview = activePreview.copy(isActive = true),
            icon = iconMap[activeView] ?: Icons.Default.Apps,
            isActive = true,
            hasActivity = true
        )
    )

    // Core workspaces (excluding active)
    coreViews.filter { it != activeView }.forEach { view ->
        val preview = state.workspacePreviews[view] ?: WorkspacePreview(view = view)
        val hasActivity = preview.lastActiveEpochMs > fiveMinutesAgo
        results.add(
            WorkspaceCardData(
                preview = preview,
                icon = iconMap[view] ?: Icons.Default.Apps,
                isActive = false,
                hasActivity = hasActivity
            )
        )
    }

    // Study tools (only if they have activity)
    studyViews.filter { it != activeView }.forEach { view ->
        val preview = state.workspacePreviews[view] ?: WorkspacePreview(view = view)
        if (preview.lastActiveEpochMs > fiveMinutesAgo || view == WorkspaceView.Review) {
            results.add(
                WorkspaceCardData(
                    preview = preview,
                    icon = iconMap[view] ?: Icons.Default.Apps,
                    isActive = false,
                    hasActivity = preview.lastActiveEpochMs > fiveMinutesAgo
                )
            )
        }
    }

    // Other tools (only if they have activity)
    toolViews.filter { it != activeView }.forEach { view ->
        val preview = state.workspacePreviews[view] ?: WorkspacePreview(view = view)
        if (preview.lastActiveEpochMs > fiveMinutesAgo) {
            results.add(
                WorkspaceCardData(
                    preview = preview,
                    icon = iconMap[view] ?: Icons.Default.Apps,
                    isActive = false,
                    hasActivity = true
                )
            )
        }
    }

    return results
}
