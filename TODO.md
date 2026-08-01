# Kaiteyo — Master TODO

## ✅ Completed

### Build Fixes
- [x] Fixed `animateColorAsState` import (now from `androidx.compose.animation`)
- [x] Fixed `animateFloatAsState` import (now from `androidx.compose.animation.core`)
- [x] Fixed `windowState.window!!.close()` → `window.close()` in FrameWindowScope
- [x] Desktop app compiles successfully (`BUILD SUCCESSFUL in 38s`)
- [x] Fixed all `String.format()` calls (replaced with KMP-safe `formatFloat()`)
- [x] Fixed all `Key.Digit0/Quote/Backtick` → alternative key constants
- [x] Fixed `MutableList` from `.map()` → added `.toMutableList()`
- [x] Added `@OptIn(ExperimentalMaterial3Api::class)` annotations

### Theme Studio v2.0
- [x] Interactive HSV color wheel with drag-to-pick
- [x] RGB editor (sliders + live value display)
- [x] HSL editor (hue/saturation/lightness sliders)
- [x] HSV editor (hue/saturation/value sliders)
- [x] HEX editor (6-char input, validates hex chars)
- [x] 11 color targets (Primary through Error)
- [x] Recent & saved color palette (save/clear, up to 20 colors)
- [x] Apply button that writes to themeState.accentScheme
- [x] Gradient Editor tab (Linear/Radial/Angular)
- [x] Multiple gradient stops (2-8, with visual preview)
- [x] Angle slider for Linear/Angular gradients
- [x] Intensity and opacity controls
- [x] Gradient preview box
- [x] Apply gradient to theme (sets gradientStart/gradientEnd)
- [x] Motion Studio with 5 presets (None to Cinematic)
- [x] Spring physics controls (damping, stiffness)
- [x] Page transition type selector (radio-style)
- [x] Duration slider (50-800ms)
- [x] Reduced motion toggle
- [x] Layout Studio with UI density selector
- [x] Corner radius selector with 4 styles + custom slider
- [x] Sidebar mode/position selectors (extracted components)
- [x] Glow controls (intensity, radius, opacity)
- [x] Transparency toggle with glass opacity
- [x] Theme management (Export/Import/Reset buttons)
- [x] Live Preview Panel (sidebar + dashboard + stats + progress)
- [x] Custom Color hue/saturation/lightness computed properties
- [x] `Color.toArgb()` utility function
- [x] `colorToHex()` utility function
- [x] `formatFloat()` KMP-safe replacement for `String.format()`

### Floating Island Sidebar v2.0
- [x] Drag to reposition (pointerInput detectDragGestures)
- [x] 9 dock states (Left, Right, Top, Bottom + 4 corners + Floating)
- [x] Resizable (drag bottom-right handle)
- [x] Drag handle indicator (floating mode)
- [x] Close/dock button (floating mode)
- [x] Borderless elevated appearance
- [x] Soft glow and shadow effects
- [x] Spring animations on collapse/expand
- [x] Auto-hide mode with slide transitions
- [x] Collapse toggle with rotation animation
- [x] SidebarNavItem with hover/press states
- [x] SidebarDivider, SidebarSectionHeader components
- [x] SidebarProgress (animated progress bar with gradient)
- [x] Dock state matching from SidebarPosition config

### Brush Quality Engine
- [x] Stroke smoothing (moving average low-pass filter)
- [x] Input prediction (extrapolate from velocity + acceleration)
- [x] Bezier smoothing (Catmull-Rom spline, 2-8 segments)
- [x] Velocity-based adaptive smoothing
- [x] Jitter reduction (threshold-based tremor elimination)
- [x] Pressure sensitivity (width range mapping)
- [x] Full `processStroke()` pipeline
- [x] `StrokePoint` data class with pressure and timestamp
- [x] `resolvePressureWidth()` function
- [x] KMP-compatible (no platform-specific APIs)

### Branded Installer (8 steps)
- [x] Welcome screen with feature showcase (6 features)
- [x] Installation location picker (install + data paths)
- [x] Component selection (shortcuts, startup, file associations, auto-update)
- [x] Theme preview (4 base themes with mini live preview)
- [x] Accent theme selector (all schemes in 4-column grid)
- [x] Accessibility settings (UI scale slider 80-200%, font size 4 options, high contrast, animations)
- [x] Animated progress screen (simulated progress bar, step list, animated logo)
- [x] Completion screen (launch now, delete installer, release notes, GitHub)
- [x] Step progress indicator (dots)
- [x] Animated transitions between steps
- [x] Skip button on all non-final steps
- [x] Background gradient decoration

### Onboarding Wizard (8 steps)
- [x] Step 1: Welcome with feature highlights (3 quick info cards)
- [x] Step 2: Theme selection (4 base modes with mini preview)
- [x] Step 3: Accent theme picker (all schemes, 3-column grid)
- [x] Step 4: UI scaling (80-200% slider with live preview)
- [x] Step 5: Font size selection (4 levels with sample Japanese/English text)
- [x] Step 6: Sidebar layout (position icons + mode chips + mini preview)
- [x] Step 7: Animation presets (5 options with descriptions and radio selection)
- [x] Step 8: Finish with 4 quick action cards
- [x] Step progress bar with animated colors
- [x] Animated content transitions
- [x] Skip All button
- [x] First-launch ready

### Anki-like Features (all compiling)
- [x] FlagManagerScreen.kt — Flag management UI
- [x] NoteEditorEnhanced.kt — Markdown note editor
- [x] KeyboardShortcutsPage.kt — Shortcut manager
- [x] AnkiCardOperations.kt — Anki operations
- [x] SearchEngineImpl.kt — Universal search engine
- [x] DeckBrowserEnhanced.kt — Tree-based deck browser

### Documentation
- [x] CHANGELOG.md updated with v2.0.0 features
- [x] COMPLETED.md updated with new feature list
- [x] Master TODO.md updated with completed items

## 🔴 P0 — Critical (Must Fix Next)

### Window Experience
- [ ] Fix window drag region — only top 44dp should be draggable, NOT the entire application
- [ ] Ensure interactive UI components (buttons, lists, settings) are NEVER draggable
- [ ] Fix animation performance — target 60 FPS, no stuttering
- [ ] Fix theme switching smoothness
- [ ] Fix hover animation consistency
- [ ] Fix resize behavior — no panel jumping, no spacing changes, no broken animations

### Design Quality
- [ ] Fix inconsistent spacing throughout UI
- [ ] Ensure rounded panels feel intentional
- [ ] Fix component alignment
- [ ] Establish clear visual hierarchy
- [ ] Make sidebar feel like an elevated floating object, not attached to window

## 🟡 P1 — High (v2.1)

### Polish
- [ ] Implement file I/O for theme export/import
- [ ] Implement actual clipboard copy for theme JSON
- [ ] Add actual file picker for installer paths
- [ ] Add drag-and-drop for gradient stop colors
- [ ] Implement color eyedropper tool
- [ ] Add more icon packs (SVG/PNG upload)
- [ ] Window drag region fix
- [ ] Add undo/redo for theme edits

### Animation System
- [ ] Spring-based hover animations on ALL components
- [ ] Page transitions with 60-120 FPS
- [ ] Settings to disable individual animation types
- [ ] Focus and selection animations

### Performance
- [ ] Profile and optimize theme switching
- [ ] Reduce recomposition in preview panel
- [ ] Optimize sidebar rendering

## 🟢 P2 — Medium (v2.2)

### Features
- [ ] Plugin foundation
- [ ] Backup/restore system
- [ ] Heatmap statistics
- [ ] Card browser bulk actions

