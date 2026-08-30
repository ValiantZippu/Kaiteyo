# Kaiteyo — Window System (Desktop)

> **Status**: `IMPLEMENTED` (KaiteyoWindow 889 lines + chrome).
> Companion: `../platform/WINDOWS.md`, `core.md`, `design/design-system.md`.

## 1. What it is

A rounded, themed, undecorated desktop window with native integration — not a default OS frame.

## 2. Implementation

| File | Role |
|------|------|
| `desktopApp/Main.kt` | `application { startKoin; rememberWindowState(1200×800 or store.load); Window(undecorated=true, icon) { KaiteyoDesktopSuite(shell={KaiteyoWindow}) } }` |
| `KaiteyoWindow.kt` | 44dp draggable title bar (BrandMark + draggable `WindowDraggableArea` double-click maximize + JNA native drag Win/Linux + Alt+Space menu), pill controls (Min/Max/Close, accent hover), rounded `RoundedCornerShape(DsRadius.Xl)` + 1dp border 35% opacity, 1dp divider, 320ms fade, work-area watch 2s loop, 250ms throttled bounds persist, F11 maximize, Ctrl/Cmd+W close, Esc dismiss |
| `NativeWindowChrome.kt` | DWM rounding + theme border (retry 5×100ms until HWND realized) — Win11 square-while-maximized via OS, fallback rounded surface on Win10/Linux/macOS |
| `WindowStateStore.kt` | Floating bounds + maximized flag (never persists minimized/fullscreen) |
| `WindowWorkArea.kt` + `WindowConstraints.kt` | clampRect to work area (taskbar any edge, macOS menubar+dock), 8-zone resize (5dp edge/10dp corner) |
| `WindowMessageHandler.kt` | WM_NCHITTEST for snap layout on maximize hover |

Imports follow Compose MPP 1.8.2 rules (`animateColorAsState` from `androidx.compose.animation`, etc.).

## 3. Requirements

Rounded window, transparent/themed titlebar, native controls integrated into theme, Windows snapping, resizing (smooth, no flashing), DPI scaling, multi-monitor, high refresh, fullscreen/maximized/minimized/restore — with platform-specific limitations documented (taskbar-top Windows edge case is P0 #2 follow-up).

## 4. Evolution

New window feature → add to `KaiteyoWindow.kt` (chrome) or `WindowConstraints.kt` (sizing). No second shell.

## 5. Verification gate

Runtime sweep required (taskbar-top Windows, multi-monitor, 125–200% DPI) — tracked as `BLOCKED` in `CURRENT_ISSUES.md`.
