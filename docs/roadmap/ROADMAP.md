# Kaiteyo (書いてよ) — Roadmap

## Development Milestones

### v1.1 — Foundation & Branding (Current)
- [x] Build system working (Gradle, Compose Multiplatform)
- [x] Desktop app compiles and runs
- [x] Basic window with undecorated chrome
- [x] Floating window controls (minimize, maximize, close)
- [x] Window drag region
- [x] Rounded window corners
- [x] Signature theme with lime (#C2FC8B) and orange (#FEAB57)
- [x] Theme system with BaseMode (Light/Dark/Oled)
- [x] Appearance settings with theme mode and accent selection
- [x] Documentation structure (/docs)

### v1.2 — Window & Sidebar Experience
- [ ] Fix window dragging — only designated drag regions, not entire app
- [ ] Fix animation performance — 60 FPS target
- [ ] Fix design quality — consistent spacing, intentional radius
- [ ] Floating sidebar implementation
  - [ ] Left, Right, Top, Bottom docking
  - [ ] Floating mode
  - [ ] Auto-hide
  - [ ] Collapse/expand with spring animation
  - [ ] Snap to dock positions (Windows 11 Snap Layouts style)
- [ ] Sidebar drag to reposition
- [ ] Smooth resize behavior
- [ ] Mobile sidebar (Top, Bottom only)

### v1.3 — Theme Engine & Appearance Studio
- [ ] Complete Theme Studio
  - [ ] Color editor (RGB, HSV, HSL, HEX)
  - [ ] Opacity control
  - [ ] Gradient editor with multiple stops
  - [ ] Live preview
- [ ] All built-in themes
  - [ ] Signature (default)
  - [ ] OLED Black
  - [ ] Dark Gray
  - [ ] Light
  - [ ] Reading (warm paper)
  - [ ] Cotton Candy
  - [ ] Ocean
  - [ ] Forest
- [ ] Theme import/export (JSON)
- [ ] Custom theme presets
- [ ] Motion Studio
  - [ ] Animation presets (None, Minimal, Standard, Smooth, Bouncy)
  - [ ] Duration control
  - [ ] Spring stiffness/damping
- [ ] Layout Studio
  - [ ] Sidebar position
  - [ ] Panel spacing
  - [ ] Corner radius
  - [ ] Density modes (Compact, Comfortable, Spacious)
  - [ ] Transparency/blur
  - [ ] Surface elevation

### v1.4 — Dashboard & Learning Experience
- [ ] Dashboard redesign
- [ ] Learning analytics
- [ ] Progress insights
- [ ] Study statistics
- [ ] Review calendar
- [ ] Streak tracking (optional, user-configurable)
- [ ] Custom study plans

### v1.5 — Polish & Performance
- [ ] Animation polish
  - [ ] Page transitions
  - [ ] Shared element transitions
  - [ ] List animations
  - [ ] Dialog/modal animations
- [ ] Performance optimization
  - [ ] Lazy loading
  - [ ] Image caching
  - [ ] Memory optimization
  - [ ] Startup time reduction
- [ ] Accessibility
  - [ ] Screen reader support
  - [ ] Keyboard navigation
  - [ ] Reduced motion
  - [ ] High contrast mode
- [ ] Internationalization
  - [ ] Japanese UI option
  - [ ] Other languages

### v2.0 — Community & Cloud
- [ ] User accounts
- [ ] Cloud sync
- [ ] Shared decks
- [ ] Community features
- [ ] API for third-party integrations

### v2.1 — AI Assistance
- [ ] AI-powered learning paths
- [ ] Intelligent review scheduling
- [ ] Smart difficulty adjustment
- [ ] Writing recognition
- [ ] Pronunciation feedback

### v2.2 — Platform Expansion
- [ ] Microsoft Store release
- [ ] Mac App Store release
- [ ] Linux package (Flathub, Snap)
- [ ] Progressive Web App (PWA)

### v3.0 — Advanced Features
- [ ] Plugin system
- [ ] Custom card types
- [ ] Advanced statistics
- [ ] Learning groups/classes
- [ ] Tutor mode

## Priority Legend

- 🔴 **Critical** — Must fix before next release
- 🟡 **High** — Should be in next release
- 🟢 **Medium** — Planned for upcoming release
- 🔵 **Low** — Nice to have, no timeline

## Current Sprint Focus

**Sprint: v1.2 Window & Sidebar**
1. 🔴 Fix window drag region (only top area, not entire app)
2. 🔴 Fix animation performance (60 FPS)
3. 🟡 Implement floating sidebar
4. 🟡 Sidebar docking positions
5. 🟢 Sidebar collapse/expand animations
6. 🟢 Smooth resize behavior
