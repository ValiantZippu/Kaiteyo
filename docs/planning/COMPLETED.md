# Kaiteyo — Completed Features

This document tracks completed features and milestones. When a task from TODO.md or CURRENT_ISSUES.md is completed, add it here.

## v2.0.0 — Premium Experience (Latest)

### Theme Studio v2.0
- [x] Interactive HSV color wheel with drag-to-pick
- [x] RGB/HSL/HSV/HEX synchronized editors
- [x] 11 color targets (Primary, Secondary, Tertiary, Background, Surface, Text, SurfaceVar, Outline, Success, Warning, Error)
- [x] Recent & saved color palette (save/clear, up to 20 colors)
- [x] Apply button writes to themeState.accentScheme
- [x] Gradient Editor (Linear/Radial/Angular, 2-8 stops, angle, intensity, opacity)
- [x] Motion Studio (5 presets, spring physics, page transitions, duration, reduced motion)
- [x] Layout Studio (density, corner radius, sidebar mode/position, glow, transparency)
- [x] Theme Management (Export/Import/Reset buttons)
- [x] Live Preview Panel (sidebar + dashboard + stats + progress cards)
- [x] KMP-safe formatting (no String.format!)
- [x] Custom Color hue/saturation/lightness computed properties

### Floating Island Sidebar v2.0
- [x] Drag to reposition (detectDragGestures)
- [x] 9 dock states (Left, Right, Top, Bottom + TL/TR/BL/BR + Floating)
- [x] Resizable via bottom-right handle
- [x] Drag handle indicator for floating mode
- [x] Close/dock button for floating mode
- [x] Borderless elevated glass appearance
- [x] Spring animations on all transitions
- [x] Auto-hide mode with slide transitions
- [x] Collapse toggle with rotation animation
- [x] SidebarNavItem, SidebarDivider, SidebarSectionHeader, SidebarProgress

### Brush Quality Engine
- [x] Stroke smoothing (moving average low-pass filter)
- [x] Input prediction (velocity + acceleration extrapolation)
- [x] Bezier smoothing (Catmull-Rom spline, 2-8 segments)
- [x] Velocity-based adaptive smoothing
- [x] Jitter reduction (tremor elimination)
- [x] Pressure sensitivity support
- [x] Full processStroke() pipeline
- [x] StrokePoint data class
- [x] resolvePressureWidth() function

### Branded Installer
- [x] 8-step installation wizard
- [x] Welcome screen with 6 feature showcases
- [x] Installation location picker
- [x] Component selection (shortcuts, startup, file assoc, auto-update)
- [x] Theme preview with 4 base modes
- [x] Accent theme selector
- [x] Accessibility settings (scale, font size, contrast, animations)
- [x] Animated progress screen
- [x] Completion screen with post-install options
- [x] Animated transitions between steps
- [x] Step progress indicator

### Onboarding Wizard
- [x] 8-step first-launch setup
- [x] Theme, accent, scaling, font size, sidebar, animations, finish
- [x] Live preview for each setting
- [x] Animated transitions
- [x] Skip All button
- [x] Step progress bar

### Anki-like Features (All Compiling)
- [x] FlagManagerScreen.kt
- [x] NoteEditorEnhanced.kt
- [x] KeyboardShortcutsPage.kt
- [x] AnkiCardOperations.kt
- [x] SearchEngineImpl.kt
- [x] DeckBrowserEnhanced.kt

### Build Fixes
- [x] Desktop app compiles successfully
- [x] Fixed String.format() → KMP-safe formatFloat()
- [x] Fixed Key.Digit0/Quote/Backtick → alternative constants
- [x] Fixed MutableList from .map() → .toMutableList()
- [x] Added @OptIn(ExperimentalMaterial3Api::class) annotations

## v1.1.0

### Build System
- [x] Desktop app compiles successfully
- [x] Fixed `animateColorAsState` import (now from `androidx.compose.animation`)
- [x] Fixed `animateFloatAsState` import (now from `androidx.compose.animation.core`)
- [x] Fixed `windowState.window!!.close()` → `window.close()` in FrameWindowScope
- [x] Fixed missing `@Composable` import

### Window Experience
- [x] Undecorated window (no title bar)
- [x] Floating window controls (minimize, maximize, close)
- [x] Spring-based hover animations on controls
- [x] Window drag region using WindowDraggableArea
- [x] Rounded corners (20dp)

### Theme System
- [x] Base mode support (Light, Dark, Oled)
- [x] KaiteyoThemeState with composition locals
- [x] Accent scheme selection
- [x] Gradient support per accent scheme
- [x] Glow configuration
- [x] Radius configuration
- [x] Animation configuration
- [x] Density configuration
- [x] SurfaceColors composition local

### Documentation
- [x] All 15+ documentation files in `/docs/`
- [x] Architecture Decision Records
- [x] AI Context for assistant onboarding
- [ ] Fix animation performance (stuttering)

### Theme System
- [ ] Implement remaining 7 built-in themes
- [ ] Signature theme: distribute lime + orange intelligently

## Planned for v1.2

### Floating Sidebar
- [ ] Floating island design
- [ ] Multiple dock positions
- [ ] Auto-hide
- [ ] Spring animations

### Appearance Studio
- [ ] Full Theme Studio with color editor
- [ ] Gradient editor
- [ ] Live preview
- [ ] Theme import/export
