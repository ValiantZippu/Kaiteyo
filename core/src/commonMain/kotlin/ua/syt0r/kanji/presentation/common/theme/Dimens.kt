package ua.syt0r.kanji.presentation.common.theme

import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO v1.2.0 — Dimensions & Spacing
// Premium layout system with density support
// ============================================

object Dimens {

    // --- Corner Radius System ---
    // Base values (multiplied by RadiusConfig.globalMultiplier at runtime)
    val RadiusXs = 4.dp      // Checkboxes, small indicators
    val RadiusSm = 8.dp      // Buttons, inputs, small cards
    val RadiusMd = 12.dp     // Standard cards, list items
    val RadiusLg = 16.dp     // Large cards, modals, context panels
    val RadiusXl = 24.dp     // Sidebar panel, main content panel
    val Radius2xl = 32.dp    // Large containers, dialogs

    // --- Spacing Scale (base values) ---
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 20.dp
    val Space6 = 24.dp
    val Space8 = 32.dp
    val Space10 = 40.dp
    val Space12 = 48.dp
    val Space16 = 64.dp
    val Space20 = 80.dp

    // --- Legacy (backward compatible) ---
    val SpacingTiny = 2.dp
    val SpacingSmall = 4.dp
    val SpacingMid = 8.dp
    val SpacingBig = 12.dp

    val ContentPadding = 20.dp
    val ContentPaddingSmall = 16.dp

    val Icon = 24.dp
    val IconSmall = 20.dp
    val IconButton = 40.dp

    val ScreenWidth = 400.dp

    val PopupMinWidth = 160.dp
    val PopupMaxSize = 300.dp

    // --- Kaiteyo v1.2.0 Enhanced Layout ---
    val SidebarWidth = 260.dp
    val SidebarCompactWidth = 72.dp
    val SidebarRadius = 24.dp
    val SidebarFloatingRadius = 32.dp
    val ContentRadius = 24.dp
    val PanelGap = 24.dp
    val WindowPadding = 24.dp
    val CardMinWidth = 240.dp
    val CardMaxWidth = 400.dp
    val CardRadius = 16.dp
    val ButtonRadius = 12.dp
    val InputRadius = 12.dp

    // --- v1.2.0 New ---
    val FloatingControlsSize = 32.dp
    val FloatingControlsGap = 6.dp
    val GlowRingSize = 48.dp
    val DragRegionHeight = 48.dp

    // --- Appearance Studio specific ---
    val StudioPreviewMinWidth = 320.dp
    val StudioSectionSpacing = 32.dp
    val StudioControlHeight = 48.dp
    val ColorSwatchSize = 28.dp
    val ColorPickerWidth = 280.dp
}