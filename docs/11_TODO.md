# Kaiteyo (書いてよ) — Master TODO

## Priority Legend
- 🔴 **P0 (Critical)** — Blocks all other work
- 🟡 **P1 (High)** — Required for next release
- 🟢 **P2 (Medium)** — Planned for upcoming release
- 🔵 **P3 (Low)** — Nice to have

---

## 🔴 P0 — Critical (Must Fix Now)

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

### Settings
- [ ] Reorganize Appearance settings into professional Appearance Studio
- [ ] Remove randomly placed appearance options

---

## 🟡 P1 — High (Next Release: v1.2)

### Floating Sidebar
- [ ] Implement floating island sidebar (not attached to edge)
- [ ] Rounded corners with elevation
- [ ] Soft shadow with color tinting
- [ ] Soft glow effect
- [ ] Smooth spring animations
- [ ] Dock positions: Left, Right, Top, Bottom, Floating
- [ ] Auto-hide mode
- [ ] Collapse/expand with spring animation
- [ ] Collapsed state shows only floating button
- [ ] Snap to valid dock positions (Windows 11 Snap Layouts style)
- [ ] Drag sidebar to reposition
- [ ] Mobile: Top, Bottom only with snap

### Theme System
- [ ] Implement all 8 built-in themes:
  - [ ] Signature (default) — Lime + Orange
  - [ ] OLED Black — True black
  - [ ] Dark Gray — Softer dark
  - [ ] Light — Clean light mode
  - [ ] Reading — Warm paper tones
  - [ ] Cotton Candy — Pastel
  - [ ] Ocean — Cool blue
  - [ ] Forest — Earthy green
- [ ] Signature theme MUST use BOTH lime (#C2FC8B) and orange (#FEAB57) distributed intelligently
- [ ] Reading theme: warm paper colors, cream backgrounds, low eye strain, ink-like text

---

## 🟢 P2 — Medium (v1.3)

### Theme Studio
- [ ] "+" button opens Theme Studio
- [ ] Start from any built-in theme as base
- [ ] Color editor with multiple color wheels (RGB, HSV, HSL, HEX)
- [ ] Opacity control
- [ ] Gradient editor with multiple stops
- [ ] Live preview showing: sidebar, cards, buttons, lists, dialogs, navigation
- [ ] Theme import/export as JSON
- [ ] Custom theme presets saved locally

### Motion Studio
- [ ] Animation presets: No Animation, Minimal, Standard, Smooth, Bouncy
- [ ] Each preset changes: duration, spring stiffness, bounce, fade timing, hover animations
- [ ] Preview animations before applying

### Layout Studio
- [ ] Sidebar position control
- [ ] Panel spacing adjustment
- [ ] Corner radius control
- [ ] Density modes: Compact, Comfortable, Spacious
- [ ] Transparency/blur control
- [ ] Surface elevation control

---

## 🔵 P3 — Low (Future)

### Polish
- [ ] Page transitions with shared element animations
- [ ] Dialog/modal entrance/exit animations
- [ ] List item animations
- [ ] Native window shadows
- [ ] Glass morphism effects
- [ ] Parallax scrolling

### Accessibility
- [ ] Keyboard navigation (Tab, Shift+Tab, Enter, Escape, Arrow keys)
- [ ] Screen reader support
- [ ] Reduced motion option
- [ ] High contrast mode
- [ ] Font size adjustment

### Internationalization
- [ ] Japanese UI option
- [ ] Community-contributed translations

### Performance
- [ ] Lazy loading for large lists
- [ ] Image caching
- [ ] Memory optimization
- [ ] Startup time reduction
- [ ] Compose compiler metrics

---

## Completed ✅

- [x] Build system working (Gradle, Compose Multiplatform)
- [x] Desktop app compiles and runs
- [x] Basic window with undecorated chrome
- [x] Floating window controls (minimize, maximize, close)
- [x] Window drag region (basic implementation)
- [x] Rounded window corners (20dp)
- [x] Signature theme with lime (#C2FC8B) and orange (#FEAB57)
- [x] Theme system with BaseMode (Light/Dark/Oled)
- [x] Appearance settings with theme mode and accent selection
- [x] Documentation structure (/docs with 11+ files)
- [x] Hover animations (scale, glow, color)
- [x] Spring-based physics for animations
