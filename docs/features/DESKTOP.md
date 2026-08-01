# Kaiteyo — Desktop Experience Specification

## Purpose

The desktop experience is the primary focus of Kaiteyo. It should feel like a premium native application comparable to Figma, Linear, Raycast, or Arc Browser.

## Window Experience

### No Title Bar
- `undecorated = true` — No default OS title bar
- No custom title bar strip either
- Only floating window controls in the top-right corner

### Floating Window Controls
- Three buttons: Minimize, Maximize/Restore, Close
- No visible container — buttons float above the UI
- Spring-based hover animations (scale 1.0 → 1.15, glow effect)
- Close button uses red (#FF6B6B) on hover
- Minimize/Maximize use accent color on hover

### Window Dragging
- Only the top 44dp of the window is draggable
- Uses `WindowDraggableArea` for the drag region
- Interactive UI components (buttons, lists, settings) are NEVER draggable
- The rest of the UI remains fully interactive

### Window Shape
- Rounded corners (20dp) using `RoundedCornerShape`
- Corners become square when maximized
- Native Windows snap layouts work with undecorated windows

### Resize Behavior
- Smooth resize with no panel jumping
- Consistent spacing during resize
- Animations remain smooth during resize

## Floating Sidebar

### Floating Island Design
- Not attached to window edge
- 8dp gap from edge
- Rounded corners (16dp)
- Elevated with soft shadow and soft glow
- Width: 280dp expanded, 56dp collapsed

### Dock Positions
- Left (default)
- Right
- Top
- Bottom
- Floating (detached, movable)
- Auto-hide (hidden until hover/click)

### Collapse/Expand
- Spring animation (damping 0.6, stiffness 300)
- When collapsed: only a floating button remains
- Clicking the button expands with fluid animation

### Snap to Positions
- Similar to Windows 11 Snap Layouts
- Only valid dock positions — not free positioning
- Smooth snap animation (250ms, spring)

## Implementation

### Key Files
- `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktopApp/Main.kt` — Window setup, floating controls, drag region
- `core/src/commonMain/kotlin/ua/syt0r/kanji/presentation/common/theme/Dimens.kt` — Window dimensions

### Window Setup
```kotlin
Window(
    onCloseRequest = { exitApplication() },
    state = windowState,
    title = resolveString { appName },
    icon = painterResource(Res.drawable.windowIcon),
    undecorated = true
) {
    KaiteyoWindow(
        windowState = windowState,
        content = {
            KanjiDojoApp(windowSizeClass = calculateWindowSizeClass())
        }
    )
}
```

### Drag Region
```kotlin
@Composable
private fun FrameWindowScope.WindowDragRegion(modifier: Modifier = Modifier) {
    WindowDraggableArea(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize())
    }
}
```

## Future Improvements
- Native window shadows
- Per-monitor DPI awareness
- Multiple window support
- Window layout persistence
- Minimize to system tray
- Global keyboard shortcuts
