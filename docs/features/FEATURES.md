# Kaiteyo (書いてよ) — Features

## Feature Status Legend
- ✅ **Implemented** — Working and tested
- 🚧 **In Progress** — Being actively developed
- 📋 **Planned** — Scheduled for future release
- 💡 **Future Idea** — Under consideration

---

## Core Learning

### Spaced Repetition
- ✅ SRS algorithm for review scheduling
- ✅ Custom review intervals
- ✅ Per-card difficulty adjustment
- 📋 AI-optimized scheduling

### Study Decks
- ✅ Built-in JLPT decks (N5-N1)
- ✅ Custom deck creation
- ✅ Deck import/export
- 📋 Shared community decks

### Writing Practice
- ✅ Stroke order diagrams
- ✅ Stroke order animation
- ✅ Drawing canvas for practice
- 💡 AI-powered stroke recognition

### Reading Practice
- ✅ Vocabulary lookup
- ✅ Furigana display
- ✅ Example sentences
- 📋 Reading mode with graded texts

---

## Desktop Experience

### Window
- ✅ Undecorated window (no title bar)
- ✅ Floating window controls (minimize, maximize, close)
- ✅ Window drag region
- ✅ Rounded corners (20dp)
- 🚧 Proper drag region (not entire app)
- 📋 Native window shadows

### Sidebar
- 📋 Floating island design
- 📋 Multiple dock positions (Left, Right, Top, Bottom)
- 📋 Floating mode
- 📋 Auto-hide
- 📋 Collapse/expand with spring animation
- 📋 Snap to dock positions

### Appearance
- ✅ Theme system with BaseMode (Light/Dark/Oled)
- ✅ Accent scheme selection
- ✅ Signature theme (Lime + Orange)
- 🚧 All built-in themes (8 total)
- 📋 Theme Studio with color editor
- 📋 Gradient editor
- 📋 Live preview
- 📋 Theme import/export

### Animations
- ✅ Hover animations (scale, glow, color)
- ✅ Spring-based physics
- 📋 Motion Studio with presets
- 📋 Page transitions
- 📋 Shared element transitions
- 📋 Reduced motion support

---

## Platform Support

### Desktop (JVM)
- ✅ Windows (MSI installer)
- ✅ macOS (DMG installer)
- ✅ Linux (Deb, AppImage)
- 📋 Microsoft Store
- 📋 Mac App Store
- 📋 Flathub/Snap

### Mobile
- ✅ Android (APK, Play Store)
- 📋 iOS (App Store)
- 📋 Tablet-optimized layout

---

## Data & Sync

### Local Storage
- ✅ SQLDelight database
- ✅ DataStore preferences
- ✅ Offline-first architecture
- ✅ Backup/restore

### Cloud (Future)
- 📋 User accounts
- 📋 Cross-device sync
- 📋 Cloud backup
- 📋 Shared decks

---

## Customization

### Theme
- ✅ Base mode (Light/Dark/Oled)
- ✅ Accent color selection
- 📋 Full color editor
- 📋 Gradient editor
- 📋 Custom theme presets
- 📋 Theme import/export

### Layout
- 📋 Sidebar position
- 📋 Density modes
- 📋 Corner radius
- 📋 Transparency/blur
- 📋 Surface elevation

### Animation
- 📋 Animation presets
- 📋 Speed control
- 📋 Spring physics tuning

---

## Accessibility

- 📋 Keyboard navigation
- 📋 Screen reader support
- 📋 Reduced motion
- 📋 High contrast mode
- 📋 Font size adjustment
- 📋 Color blindness support

---

## Internationalization

- ✅ English UI
- 📋 Japanese UI
- 📋 Other languages (community contributed)

---

## Technical Features

- ✅ Kotlin Multiplatform
- ✅ Compose Multiplatform UI
- ✅ Koin dependency injection
- ✅ SQLDelight database
- ✅ Ktor HTTP client
- ✅ DataStore preferences
- ✅ AboutLibraries for OSS credits
- ✅ Version catalog (libs.versions.toml)
- ✅ Build configuration (buildSrc)
