package ua.syt0r.kanji.presentation.common.nav

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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow as materialShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ua.syt0r.kanji.presentation.common.resources.string.StringResolveScope
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition

// ============================================
// NAVIGATION SETTINGS
// Tabbed settings page for the adaptive
// navigation system with a live miniature
// preview. Every change is applied immediately
// and persisted as JSON.
// ============================================

enum class SettingsTab(val labelResolver: StringResolveScope<String>) {
    General({ nav.generalTabLabel }),
    Sidebar({ nav.sidebarTabLabel }),
    Floating({ nav.floatingTabLabel }),
    Phone({ nav.phoneTabLabel }),
    Accessibility({ nav.accessibilityTabLabel })
}

@Composable
fun NavigationSettingsOverlay(
    navSettings: NavigationSettingsState,
    formFactor: FormFactor,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val surfaceColors = LocalSurfaceColors.current
        var selectedTab by remember { mutableStateOf(SettingsTab.General) }

        Surface(
            modifier = Modifier
                // Full-screen dialog: keep the header and bottom content clear
                // of the system status bar / gesture area (phone top & bottom
                // bars are covered by this dialog window, so only system bars
                // need clearing here). No-op on desktop where insets are 0.
                .safeDrawingPadding()
                .then(
                    if (formFactor.isPhone) {
                        Modifier.fillMaxWidth().padding(10.dp)
                    } else {
                        Modifier.widthIn(max = 780.dp)
                    }
                )
                .fillMaxHeight(if (formFactor.isPhone) 0.94f else 0.9f)
                // Shadow before clip/background so it renders behind the surface.
                .materialShadow(28.dp, RoundedCornerShape(scaledRadius(Dimens.RadiusXl)))
                .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusXl))),
            shape = RoundedCornerShape(scaledRadius(Dimens.RadiusXl)),
            color = surfaceColors.surfaceElevated
        ) {
            Column(Modifier.fillMaxSize()) {
                SettingsHeader(
                    formFactor = formFactor,
                    onDismiss = onDismiss
                )

                SettingsTabRow(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.padding(horizontal = Dimens.Space3)
                )

                Spacer(Modifier.height(Dimens.Space2))

                if (formFactor.isPhone) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Dimens.Space3),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                    ) {
                        SettingsPreviewPanel(navSettings.settings, formFactor)
                        SettingsTabContent(navSettings, formFactor, selectedTab)
                        ResetNavigationButton(navSettings)
                        Spacer(Modifier.height(Dimens.Space2))
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space1),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(end = Dimens.Space1),
                            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                        ) {
                            SettingsTabContent(navSettings, formFactor, selectedTab)
                            ResetNavigationButton(navSettings)
                            Spacer(Modifier.height(Dimens.Space2))
                        }
                        Box(
                            Modifier
                                .width(320.dp)
                                .fillMaxHeight()
                        ) {
                            SettingsPreviewPanel(
                                navSettings.settings,
                                formFactor,
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = Dimens.Space2)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(formFactor: FormFactor, onDismiss: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.Space4, end = Dimens.Space2, top = Dimens.Space2, bottom = Dimens.Space1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = resolveString { nav.settingsLabel },
                style = MaterialTheme.typography.titleMedium,
                color = surfaceColors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (formFactor.isPhone) "Phone layout" else "Desktop layout",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = surfaceColors.textSecondary)
        }
    }
}

@Composable
private fun SettingsTabRow(
    selected: SettingsTab,
    onSelect: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SettingsTab.entries.forEach { tab ->
            val isSelected = selected == tab
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                    .background(
                        when {
                            isSelected -> accent.primary.copy(alpha = 0.16f)
                            isHovered -> surfaceColors.surfaceInteractive
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) accent.primary.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(scaledRadius(Dimens.RadiusMd))
                    )
                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(tab) }
                    .hoverable(interactionSource)
                    .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = resolveString(tab.labelResolver),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) accent.primary else surfaceColors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
internal fun SettingsTabContent(
    navSettings: NavigationSettingsState,
    formFactor: FormFactor,
    tab: SettingsTab
) {
    when (tab) {
        SettingsTab.General -> GeneralSection(navSettings, formFactor)
        SettingsTab.Sidebar -> SidebarSection(navSettings)
        SettingsTab.Floating -> FloatingSection(navSettings, formFactor)
        SettingsTab.Phone -> PhoneSection(navSettings)
        SettingsTab.Accessibility -> AccessibilitySection(navSettings)
    }
}

// ============================================
// PREVIEW PANEL
// ============================================

@Composable
internal fun SettingsPreviewPanel(
    settings: NavigationSettings,
    formFactor: FormFactor,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Live Preview",
            style = MaterialTheme.typography.labelMedium,
            color = surfaceColors.textMuted,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = Dimens.Space2)
        )
        NavigationPreview(
            settings = settings,
            formFactor = formFactor,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Changes apply instantly",
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted,
            modifier = Modifier.padding(top = Dimens.Space2)
        )
    }
}

// ============================================
// NAVIGATION SETTINGS PAGE
// Embeddable page (used by the Settings Center)
// with tab row, live preview and tab content.
// ============================================

@Composable
fun NavigationSettingsPage(
    navSettings: NavigationSettingsState,
    formFactor: FormFactor,
    modifier: Modifier = Modifier,
    initialTab: SettingsTab = SettingsTab.General
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        SettingsTabRow(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.fillMaxWidth()
        )

        if (formFactor.isPhone) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                SettingsPreviewPanel(navSettings.settings, formFactor)
                SettingsTabContent(navSettings, formFactor, selectedTab)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    SettingsTabContent(navSettings, formFactor, selectedTab)
                }
                Box(
                    Modifier
                        .width(320.dp)
                ) {
                    SettingsPreviewPanel(
                        navSettings.settings,
                        formFactor,
                        Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

// ============================================
// GENERAL
// ============================================

@Composable
private fun GeneralSection(navSettings: NavigationSettingsState, formFactor: FormFactor) {
    val settings = navSettings.settings
    SettingsSection(title = resolveString { nav.generalTabLabel }) {
        Text(
            text = resolveString { nav.defaultModeLabel },
            style = MaterialTheme.typography.bodyMedium,
            color = LocalSurfaceColors.current.textPrimary,
            modifier = Modifier.padding(bottom = Dimens.Space1)
        )
        ModePicker(
            current = settings.defaultMode,
            onChange = { mode -> navSettings.update { it.copy(defaultMode = mode) } }
        )
        ToggleRow(
            label = resolveString { nav.rememberPreviousModeLabel },
            checked = settings.rememberPreviousMode,
            onChange = { value -> navSettings.update { it.copy(rememberPreviousMode = value) } }
        )
        Text(
            text = resolveString { nav.placementLabel },
            style = MaterialTheme.typography.bodyMedium,
            color = LocalSurfaceColors.current.textPrimary,
            modifier = Modifier.padding(bottom = Dimens.Space1)
        )
        EdgePicker(
            current = settings.edgeFor(formFactor),
            edges = if (formFactor.isPhone) {
                listOf(SidebarPosition.Top, SidebarPosition.Bottom)
            } else {
                SidebarPosition.entries
            },
            onSelect = { edge ->
                navSettings.update { current ->
                    if (formFactor.isPhone) current.copy(phone = current.phone.copy(edge = edge))
                    else current.copy(desktopEdge = edge)
                }
            }
        )
        ToggleRow(
            label = resolveString { nav.enableAnimationsLabel },
            checked = settings.animationsEnabled,
            onChange = { value -> navSettings.update { it.copy(animationsEnabled = value) } }
        )
        // Page debug info is now controlled from Settings > Debug > Show panel name.
        if (settings.animationsEnabled) {
            SliderRow(
                label = "Animation duration",
                value = settings.animationDurationMs.toFloat(),
                range = 120f..600f,
                valueLabel = { "${it.toInt()} ms" },
                onValueChange = { navSettings.update { current -> current.copy(animationDurationMs = it.toInt()) } }
            )
            ToggleRow(
                label = "Reduce motion",
                checked = settings.accessibility.reducedMotion,
                onChange = { navSettings.update { current -> current.copy(accessibility = current.accessibility.copy(reducedMotion = it)) } }
            )
        }
    }
}

// ============================================
// SIDEBAR
// ============================================

@Composable
private fun SidebarSection(navSettings: NavigationSettingsState) {
    val settings = navSettings.settings
    SettingsSection(title = resolveString { nav.sidebarTabLabel }) {
        // Resolve in composable context — optionLabel itself is a plain lambda.
        val expandedLabel = resolveString { nav.sidebarExpandedLabel }
        val compactLabel = resolveString { nav.sidebarCompactLabel }
        ChoiceRow(
            label = resolveString { nav.sidebarLayoutLabel },
            options = SidebarExpansion.entries,
            optionLabel = {
                when (it) {
                    SidebarExpansion.Expanded -> expandedLabel
                    SidebarExpansion.Compact -> compactLabel
                }
            },
            selected = settings.sidebarExpansion,
            onSelect = { navSettings.update { current -> current.copy(sidebarExpansion = it) } }
        )
        ChoiceRow(
            label = resolveString { nav.expandedWidthLabel },
            options = ExpandedWidthOptions,
            optionLabel = { "$it dp" },
            selected = settings.sidebar.expandedWidth,
            onSelect = { navSettings.update { current ->
                val index = ExpandedWidthOptions.indexOf(it)
                current.copy(sidebar = current.sidebar.copy(expandedWidthIndex = if (index >= 0) index else current.sidebar.expandedWidthIndex))
            } }
        )
        SliderRow(
            label = resolveString { nav.sidebarIconSizeLabel },
            value = settings.sidebar.iconSize.toFloat(),
            range = 16f..32f,
            valueLabel = { "${it.toInt()} dp" },
            onValueChange = { navSettings.update { current -> current.copy(sidebar = current.sidebar.copy(iconSize = it.toInt())) } }
        )
        SliderRow(
            label = resolveString { nav.compactSpacingLabel },
            value = settings.sidebar.compactSpacing.toFloat(),
            range = 4f..16f,
            valueLabel = { "${it.toInt()} dp" },
            onValueChange = { navSettings.update { current -> current.copy(sidebar = current.sidebar.copy(compactSpacing = it.toInt())) } }
        )
        ChoiceRow(
            label = resolveString { nav.labelsVisibilityLabel },
            options = NavLabelVisibility.entries,
            optionLabel = { it.name },
            selected = settings.sidebar.labelVisibility,
            onSelect = { navSettings.update { current -> current.copy(sidebar = current.sidebar.copy(labelVisibility = it)) } }
        )
    }
}

// ============================================
// PHONE
// ============================================

@Composable
private fun PhoneSection(navSettings: NavigationSettingsState) {
    val settings = navSettings.settings
    SettingsSection(title = resolveString { nav.phoneTabLabel }) {
        Text(
            text = resolveString { nav.phoneStoredSeparatelyHint },
            style = MaterialTheme.typography.bodySmall,
            color = LocalSurfaceColors.current.textMuted
        )
        Text(
            text = resolveString { nav.phoneNavPositionLabel },
            style = MaterialTheme.typography.bodyMedium,
            color = LocalSurfaceColors.current.textPrimary,
            modifier = Modifier.padding(top = Dimens.Space1, bottom = Dimens.Space1)
        )
        EdgePicker(
            current = settings.phone.edge,
            edges = listOf(SidebarPosition.Top, SidebarPosition.Bottom),
            onSelect = { edge ->
                navSettings.update { current ->
                    current.copy(phone = current.phone.copy(edge = edge))
                }
            }
        )
        Text(
            text = resolveString { nav.phoneLauncherPositionLabel },
            style = MaterialTheme.typography.bodyMedium,
            color = LocalSurfaceColors.current.textPrimary,
            modifier = Modifier.padding(top = Dimens.Space2, bottom = Dimens.Space1)
        )
        SnapAnchorPicker(
            current = settings.phone.snapPoint,
            edges = BubbleSnapPoint.PickerOrder,
            onSelect = { snap ->
                navSettings.update { current ->
                    current.copy(phone = current.phone.copy(snapPoint = snap))
                }
            }
        )
    }
}

// ============================================
// FLOATING (bubble)
// ============================================

@Composable
private fun FloatingSection(navSettings: NavigationSettingsState, formFactor: FormFactor) {
    val settings = navSettings.settings
    val bubble = settings.bubble
    val floatingModeOn = settings.mode == NavigationMode.Floating
    SettingsSection(title = resolveString { nav.floatingTabLabel }) {
        ToggleRow(
            label = "Enable floating mode",
            checked = floatingModeOn,
            onChange = { enabled ->
                if (enabled) navSettings.setMode(NavigationMode.Floating)
                else navSettings.setMode(settings.lastMode ?: settings.defaultMode)
            }
        )
        SliderRow(
            label = "Bubble size",
            value = bubble.size.toFloat(),
            range = 44f..72f,
            valueLabel = { "${it.toInt()} dp" },
            onValueChange = { navSettings.update { current -> current.copy(bubble = current.bubble.copy(size = it.toInt())) } }
        )
        SliderRow(
            label = resolveString { nav.bubbleIconSizeLabel },
            value = bubble.iconSize.toFloat(),
            range = 16f..34f,
            valueLabel = { "${it.toInt()} dp" },
            onValueChange = { navSettings.update { current -> current.copy(bubble = current.bubble.copy(iconSize = it.toInt())) } }
        )
        SliderRow(
            label = resolveString { nav.holdDurationLabel },
            value = bubble.holdDurationMs.toFloat(),
            range = 200f..1500f,
            valueLabel = { "${it.toInt()} ms" },
            onValueChange = { navSettings.update { current -> current.copy(bubble = current.bubble.copy(holdDurationMs = it.toLong())) } }
        )
        SliderRow(
            label = resolveString { nav.safeMarginLabel },
            value = bubble.safeMargin.toFloat(),
            range = 4f..48f,
            valueLabel = { "${it.toInt()} dp" },
            onValueChange = { navSettings.update { current -> current.copy(bubble = current.bubble.copy(safeMargin = it.toInt())) } }
        )
        // Resolve in composable context — optionLabel itself is a plain lambda.
        val neverLabel = resolveString { nav.autoHideNever }
        val tenSecondsLabel = resolveString { nav.autoHideTenSeconds }
        val twentySecondsLabel = resolveString { nav.autoHideTwentySeconds }
        val thirtySecondsLabel = resolveString { nav.autoHideThirtySeconds }
        val oneMinuteLabel = resolveString { nav.autoHideOneMinute }
        val customLabel = resolveString { nav.autoHideCustom }
        ChoiceRow(
            label = resolveString { nav.autoHideLabel },
            options = AutoHidePreset.entries,
            optionLabel = { preset ->
                when (preset) {
                    AutoHidePreset.Never -> neverLabel
                    AutoHidePreset.TenSeconds -> tenSecondsLabel
                    AutoHidePreset.TwentySeconds -> twentySecondsLabel
                    AutoHidePreset.ThirtySeconds -> thirtySecondsLabel
                    AutoHidePreset.OneMinute -> oneMinuteLabel
                    AutoHidePreset.Custom -> customLabel
                }
            },
            selected = bubble.autoHide,
            onSelect = { preset ->
                navSettings.update { current ->
                    current.copy(
                        bubble = current.bubble.copy(
                            autoHide = preset,
                            autoFade = preset != AutoHidePreset.Never
                        )
                    )
                }
            }
        )
        if (bubble.autoHide == AutoHidePreset.Custom && bubble.autoFade) {
            SliderRow(
                label = "Idle timeout",
                value = bubble.idleTimeoutMs.toFloat(),
                range = 2000f..60000f,
                valueLabel = { "${(it / 1000).toInt()}s" },
                onValueChange = { navSettings.update { current -> current.copy(bubble = current.bubble.copy(idleTimeoutMs = it.toLong())) } }
            )
        }
        ToggleRow(
            label = "Hover reveal",
            checked = bubble.hoverReveal,
            onChange = { navSettings.update { current -> current.copy(bubble = current.bubble.copy(hoverReveal = it)) } }
        )
        if (bubble.autoFade) {
            SliderRow(
                label = "Idle opacity",
                value = bubble.fadeOpacity,
                range = 0.2f..0.6f,
                valueLabel = { "${(it * 100).toInt()}%" },
                onValueChange = { navSettings.update { current -> current.copy(bubble = current.bubble.copy(fadeOpacity = it)) } }
            )
        }
        SliderRow(
            label = "Bubble elevation",
            value = bubble.elevation.toFloat(),
            range = 4f..28f,
            valueLabel = { "${it.toInt()} dp" },
            onValueChange = { navSettings.update { current -> current.copy(bubble = current.bubble.copy(elevation = it.toInt())) } }
        )
        SliderRow(
            label = "Snap preview size",
            value = bubble.snapDistance.toFloat(),
            range = 40f..120f,
            valueLabel = { "${it.toInt()} dp" },
            onValueChange = { navSettings.update { current -> current.copy(bubble = current.bubble.copy(snapDistance = it.toInt())) } }
        )
        Text(
            text = resolveString { nav.snapPositionLabel },
            style = MaterialTheme.typography.bodyMedium,
            color = LocalSurfaceColors.current.textPrimary,
            modifier = Modifier.padding(bottom = Dimens.Space1)
        )
        SnapAnchorPicker(
            current = settings.snapPointFor(formFactor),
            edges = BubbleSnapPoint.PickerOrder,
            onSelect = { snap ->
                navSettings.update { current ->
                    if (formFactor.isPhone) current.copy(phone = current.phone.copy(snapPoint = snap))
                    else current.copy(snapPoint = snap)
                }
            }
        )
        SliderRow(
            label = "Snap sensitivity",
            value = bubble.snapSensitivity.toFloat(),
            range = 40f..160f,
            valueLabel = { it.toInt().toString() },
            onValueChange = { navSettings.update { current -> current.copy(bubble = current.bubble.copy(snapSensitivity = it.toInt())) } }
        )
        SliderRow(
            label = "Animation speed",
            value = bubble.animationSpeed,
            range = 0.5f..2f,
            valueLabel = { "${it}x" },
            onValueChange = { navSettings.update { current -> current.copy(bubble = current.bubble.copy(animationSpeed = it)) } }
        )
    }

    // ============ LAUNCHPAD ============
    SettingsSection(title = "Launchpad") {
        val launchpad = settings.launchpad
        SliderRow(
            label = "Launchpad scale",
            value = launchpad.scale,
            range = 0.7f..1.2f,
            valueLabel = { "${(it * 100).toInt()}%" },
            onValueChange = { navSettings.update { current -> current.copy(launchpad = current.launchpad.copy(scale = it)) } }
        )
        SliderRow(
            label = "Tile spacing",
            value = launchpad.spacing,
            range = 0.7f..1.5f,
            valueLabel = { "${(it * 100).toInt()}%" },
            onValueChange = { navSettings.update { current -> current.copy(launchpad = current.launchpad.copy(spacing = it)) } }
        )
        ChoiceRow(
            label = "Expand direction",
            options = LaunchpadDirection.entries,
            optionLabel = { it.name },
            selected = launchpad.direction,
            onSelect = { navSettings.update { current -> current.copy(launchpad = current.launchpad.copy(direction = it)) } }
        )
        SliderRow(
            label = "Panel opacity",
            value = launchpad.opacity,
            range = 0.6f..1f,
            valueLabel = { "${(it * 100).toInt()}%" },
            onValueChange = { navSettings.update { current -> current.copy(launchpad = current.launchpad.copy(opacity = it)) } }
        )
        ToggleRow(
            label = "Staggered tile reveal",
            checked = launchpad.staggeredReveal,
            onChange = { navSettings.update { current -> current.copy(launchpad = current.launchpad.copy(staggeredReveal = it)) } }
        )
    }
}



// ============================================
// ACCESSIBILITY
// ============================================

@Composable
private fun AccessibilitySection(navSettings: NavigationSettingsState) {
    val settings = navSettings.settings
    SettingsSection(title = resolveString { nav.accessibilityTabLabel }) {
        ToggleRow(
            label = "Reduce motion",
            checked = settings.accessibility.reducedMotion,
            onChange = { navSettings.update { current -> current.copy(accessibility = current.accessibility.copy(reducedMotion = it)) } }
        )
        ToggleRow(
            label = "Larger hitboxes",
            checked = settings.accessibility.largerHitboxes,
            onChange = { navSettings.update { current -> current.copy(accessibility = current.accessibility.copy(largerHitboxes = it)) } }
        )
        ToggleRow(
            label = "Larger icons",
            checked = settings.accessibility.largerIcons,
            onChange = { navSettings.update { current -> current.copy(accessibility = current.accessibility.copy(largerIcons = it)) } }
        )
        ToggleRow(
            label = "High contrast",
            checked = settings.accessibility.highContrast,
            onChange = { navSettings.update { current -> current.copy(accessibility = current.accessibility.copy(highContrast = it)) } }
        )
    }
}

// ============================================
// SNAP ANCHOR PICKER — visual 12-point grid
// Rows mirror the screen edges: top edge, left /
// right edges, bottom edge — 3 snap points each.
// ============================================

@Composable
private fun SnapAnchorPicker(
    current: BubbleSnapPoint,
    edges: List<BubbleSnapPoint>,
    onSelect: (BubbleSnapPoint) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    // Visual rows mirroring the edges; the two middle columns hold the
    // left/right edge points with the center intentionally empty.
    val gridRows = listOf(
        listOf(BubbleSnapPoint.TopLeft, BubbleSnapPoint.TopCenter, BubbleSnapPoint.TopRight),
        listOf(BubbleSnapPoint.LeftTop, BubbleSnapPoint.RightTop),
        listOf(BubbleSnapPoint.LeftCenter, BubbleSnapPoint.RightCenter),
        listOf(BubbleSnapPoint.LeftBottom, BubbleSnapPoint.RightBottom),
        listOf(BubbleSnapPoint.BottomLeft, BubbleSnapPoint.BottomCenter, BubbleSnapPoint.BottomRight)
    ).map { row -> row.filter { it in edges } }
        .filter { it.isNotEmpty() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        gridRows.forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowOptions.forEach { snap ->
                    val selected = current == snap
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusSm)))
                            .background(
                                when {
                                    selected -> accent.primary.copy(alpha = 0.16f)
                                    isHovered -> surfaceColors.surfaceInteractive
                                    else -> surfaceColors.surface
                                }
                            )
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) accent.primary
                                else surfaceColors.border.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(scaledRadius(Dimens.RadiusSm))
                            )
                            .clickable(interactionSource = interactionSource, indication = null) { onSelect(snap) }
                            .hoverable(interactionSource),
                        contentAlignment = Alignment.Center
                    ) {
                        // Miniature window with the bubble dot at the snap position.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight(0.55f)
                                .clip(RoundedCornerShape(3.dp))
                                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.6f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (selected) accent.primary else surfaceColors.border)
                                    .align(dotAlignment(snap))
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun dotAlignment(snap: BubbleSnapPoint): Alignment {
    return when (snap) {
        BubbleSnapPoint.TopLeft, BubbleSnapPoint.LeftTop -> Alignment.TopStart
        BubbleSnapPoint.TopCenter -> Alignment.TopCenter
        BubbleSnapPoint.TopRight, BubbleSnapPoint.RightTop -> Alignment.TopEnd
        BubbleSnapPoint.BottomLeft, BubbleSnapPoint.LeftBottom -> Alignment.BottomStart
        BubbleSnapPoint.BottomCenter -> Alignment.BottomCenter
        BubbleSnapPoint.BottomRight, BubbleSnapPoint.RightBottom -> Alignment.BottomEnd
        BubbleSnapPoint.LeftCenter -> Alignment.CenterStart
        BubbleSnapPoint.RightCenter -> Alignment.CenterEnd
    }
}

// ============================================
// EDGE PICKER — sidebar placement
// ============================================

@Composable
private fun EdgePicker(
    current: SidebarPosition,
    edges: List<SidebarPosition>,
    onSelect: (SidebarPosition) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        edges.forEach { edge ->
            val selected = current == edge
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                    .background(
                        when {
                            selected -> accent.primary.copy(alpha = 0.16f)
                            isHovered -> surfaceColors.surfaceInteractive
                            else -> surfaceColors.surface
                        }
                    )
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) accent.primary else surfaceColors.border.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(scaledRadius(Dimens.RadiusMd))
                    )
                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(edge) }
                    .hoverable(interactionSource),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    edgeIcon(edge),
                    contentDescription = edge.displayName,
                    tint = if (selected) accent.primary else surfaceColors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun edgeIcon(edge: SidebarPosition): ImageVector = when (edge) {
    SidebarPosition.Left -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
    SidebarPosition.Right -> Icons.AutoMirrored.Filled.KeyboardArrowRight
    SidebarPosition.Top -> Icons.Default.KeyboardArrowUp
    SidebarPosition.Bottom -> Icons.Default.KeyboardArrowDown
}

// ============================================
// PRIMITIVES
// ============================================

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = surfaceColors.textMuted,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = Dimens.Space1)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusLg)))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.5f))
                .padding(Dimens.Space3),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2),
            content = content
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = surfaceColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = surfaceColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueLabel(value),
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.width(140.dp)
        )
    }
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    optionLabel: (T) -> String,
    selected: T,
    onSelect: (T) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = surfaceColors.textPrimary,
            modifier = Modifier.padding(bottom = Dimens.Space1)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space1)) {
            options.forEach { option ->
                val isSelected = option == selected
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                        .background(
                            when {
                                isSelected -> accent.primary.copy(alpha = 0.16f)
                                isHovered -> surfaceColors.surfaceInteractive
                                else -> surfaceColors.surface
                            }
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accent.primary else surfaceColors.border.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(scaledRadius(Dimens.RadiusMd))
                        )
                        .clickable(interactionSource = interactionSource, indication = null) { onSelect(option) }
                        .hoverable(interactionSource)
                        .padding(horizontal = Dimens.Space1, vertical = Dimens.Space2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optionLabel(option),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) accent.primary else surfaceColors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ModePicker(
    current: NavigationMode,
    onChange: (NavigationMode) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val modes = listOf(
        NavigationMode.Floating to resolveString { nav.modeFloatingLabel },
        NavigationMode.Sidebar to resolveString { nav.modeSidebarLabel }
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
            .background(surfaceColors.surfaceInteractive.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        modes.forEach { (mode, label) ->
            val isSelected = current == mode
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusSm)))
                    .background(
                        when {
                            isSelected -> accent.primary.copy(alpha = 0.18f)
                            isHovered -> surfaceColors.surfaceInteractive
                            else -> Color.Transparent
                        }
                    )
                    .clickable(interactionSource = interactionSource, indication = null) { onChange(mode) }
                    .hoverable(interactionSource)
                    .padding(vertical = Dimens.Space1),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) accent.primary else surfaceColors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ResetNavigationButton(navSettings: NavigationSettingsState) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
            .background(if (isHovered) surfaceColors.surfaceInteractive else surfaceColors.surface)
            .clickable(interactionSource = interactionSource, indication = null, onClick = { navSettings.reset() })
            .hoverable(interactionSource)
            .padding(vertical = Dimens.Space2),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Reset navigation settings",
            style = MaterialTheme.typography.labelLarge,
            color = accent.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun scaledRadius(base: Dp): Dp {
    val multiplier = LocalRadiusConfig.current.style.globalMultiplier
    return base * multiplier
}
