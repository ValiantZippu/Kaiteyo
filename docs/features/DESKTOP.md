# Kaiteyo — Desktop Experience Specification

## Purpose

The desktop experience is the primary focus of Kaiteyo. It should feel like a premium
native application comparable to Figma, Linear, Raycast, or Arc Browser — a single
cohesive window with native-feeling window management.

## Window Experience

### Custom Title Bar
- `undecorated = true` — no OS chrome; Kaiteyo draws its own 44dp title bar.
- **Left — the "K" window icon**: a click opens the system menu, a double-click
  closes the window (native Windows icon behavior). The icon sits *outside* the
  draggable area so a press never starts a window drag.
- **Wordmark** ("Kaiteyo") next to the icon is part of the draggable region.
- **Right — window controls**: Minimize, Maximize/Restore, Close. Transparent until
  hover; the close button turns red on hover. Spring hover animations (scale + color
  tween).
- A hairline divider separates the title bar from the content.

### System Menu
The custom equivalent of the native window menu, shared by three surfaces:
- **Title bar**: right-click anywhere on the title bar (anchored at the cursor),
  **Alt+Space** or the **context-menu key** (anchored at the title-bar corner), or a
  click on the "K" icon.
- **Dock**: the window button at the bottom of the sidebar rail (expanded and compact).
- **Launchpad**: the window-control strip at the bottom of the launchpad panel.

Items: **Restore** (enabled when maximized), **Minimize**, **Maximize** (enabled when
floating), **Close**. Fully keyboard-accessible — grabs focus on open, ↑/↓ navigate
(wrapping, skipping disabled actions), Enter/Space activates, Esc dismisses.

### Window Dragging
- **Native OS drag wherever possible**:
  - Windows — `WM_NCLBUTTONDOWN`/`HTCAPTION` modal drag loop (`NativeWindowDrag.kt`,
    JNA), giving 1:1 tracking, native double-click maximize/restore, and snap feedback.
  - Linux — EWMH `_NET_WM_MOVERESIZE` client message over X11 (best-effort; falls back
    on failure). X errors are suppressed only during the window-tree scan.
- Compose's `WindowDraggableArea` remains the universal fallback (macOS, and any
  platform where the native call cannot take over).
- Double-click the title bar maximizes/restores (native on Windows, manual handler
  elsewhere).

### Keyboard Shortcuts
- **F11** — toggle maximize/restore.
- **Cmd+W** (macOS) / **Ctrl+W** (Windows/Linux) — close the window.
- **Alt+Space** / context-menu key — system menu.
- Alt+F4 / Win+Up remain OS-native on Windows.

### Window Resize
- Invisible **8-zone resize handles** (5dp edges, 10dp corners) with native resize
  cursors, active only while floating.
- Standard resize math: dragged edges follow the pointer, anchored edges stay put;
  minimum window size 860×600.
- On Windows they coexist with the OS resize border; on macOS/Linux they are the only
  way to edge-resize an undecorated window.

### Window Shape
- Rounded corners (20dp) while floating; square while maximized.
- A full-window background fill sits under the rounded surface so the corner cutouts
  never reveal the OS window's black backdrop.

### Window Bounds Persistence
- Size and position are remembered across launches via `~/.kaiteyo/window.json`
  (`WindowStateStore`).
- Only floating geometry is saved (maximized/minimized states are skipped), throttled
  to ~4 writes/s.
- On load, the saved position is validated against the usable screen area; off-screen
  positions are dropped (size kept) so the window can never reopen off-screen.
- Window bounds are included in profile backups and restored on restore.

## Navigation Modes

- Three dock modes, switchable from the top of the rail: **Expanded** (labeled rail),
  **Compact** (icon rail), **Bubble** (floating launcher).
- The dock rail sits on a configurable edge (left/right/top/bottom), adapts to window
  width, and hosts the window-control button at the bottom.
- The **launchpad** (from the bubble) is a centered overlay with a 5×2 tile grid plus
  the keyboard-accessible window-control strip.

## Implementation

### Key Files
- `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktopApp/Main.kt` — entry point,
  window setup, bounds restore.
- `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktopApp/KaiteyoWindow.kt` — window
  shell: title bar, system menu, resize handles, bounds save, keyboard shortcuts.
- `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktopApp/NativeWindowDrag.kt` —
  native dragging (Windows `WM_NCLBUTTONDOWN`, Linux X11/EWMH).
- `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktopApp/WindowStateStore.kt` —
  persisted window bounds.
- `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktop/ui/workspace/WindowActions.kt` —
  shared window menu, `LocalWindowControls`, dock window button.
- `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktop/ui/workspace/WorkspaceNav.kt` —
  dock rail (hosts the window button).
- `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktop/ui/workspace/FloatingLauncher.kt` —
  bubble launcher + launchpad (window-control strip).
- `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktop/engine/transfer/ProfileArchive.kt` —
  profile backups (include window bounds).

### Window Setup

```kotlin
val savedBounds = WindowStateStore.load()
val windowState = rememberWindowState(
    size = DpSize(
        savedBounds.width.takeIf { it > 0 }?.dp ?: 1200.dp,
        savedBounds.height.takeIf { it > 0 }?.dp ?: 800.dp
    ),
    position = if (savedBounds.x != null && savedBounds.y != null) {
        WindowPosition(savedBounds.x, savedBounds.y)
    } else {
        WindowPosition.PlatformDefault
    }
)

Window(
    onCloseRequest = ::exitApplication,
    state = windowState,
    title = resolveString { appName },
    icon = painterResource(Res.drawable.windowIcon),
    undecorated = true
) {
    KaiteyoWindow(
        windowState = windowState,
        onClose = ::exitApplication,
        content = { KaiteyoApp(windowSizeClass = calculateWindowSizeClass()) }
    )
}
```

## Screenshots

The window shell and launcher states are captured as real screenshots in
`docs/screenshots/` — see the [screenshots index](../screenshots/README.md):

- `window-shell.png` — default window with the floating launcher bubble
- `launcher-menu.png` — bubble quick controls
- `launchpad-overlay.png` — launchpad tile grid
- `launchpad-window-strip.png` — launchpad with the window controls focused

Regenerate them anytime with `scripts/capture-window-shell.sh` (all four states,
or one state by name). The script relies on the dev-only `--capture-state=`
flag, which pre-opens the requested state in a fixed 1200×800 window and exits
after a configurable dwell — normal launches are unaffected.

## Future Improvements
- Native OS window shadows for the undecorated window
- Per-monitor DPI awareness
- Multiple window support
- Minimize to system tray
- Global (system-wide) keyboard shortcuts
