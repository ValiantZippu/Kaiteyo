# Kaiteyo (書いてよ) — Start Here

## What is Kaiteyo?

Kaiteyo is a premium, cross-platform Japanese language learning application. Originally a fork of Kanji Dojo, it has been completely redesigned with a focus on desktop-first UX, beautiful craftsmanship, and a cohesive design system.

The name "Kaiteyo" (書いてよ) means "write it!" in Japanese — an invitation to practice and engage with the language actively.

## Quick Facts

| Attribute | Value |
|-----------|-------|
| Platform | Desktop (Windows/macOS/Linux), Android, iOS |
| UI Framework | Compose Multiplatform |
| Language | Kotlin (KMP) |
| Build System | Gradle |
| DI | Koin |
| Database | SQLDelight |
| HTTP | Ktor |
| Min JDK | 17 |

## Vision

Kaiteyo aims to be the most polished Japanese learning experience on desktop. The focus is craftsmanship, clarity, responsiveness, smooth motion, and a cohesive design system — inspired by Figma, Linear, Raycast, and Arc Browser.

## Design Language

- **4dp grid** for all spacing
- **System fonts** (SF Pro, Segoe UI, Roboto)
- **Token-based theming** with 8 built-in themes
- **Spring-based animations** at 60 FPS
- **Floating UI elements** (sidebar, controls)
- **Signature colors**: Lime (#C2FC8B) + Orange (#FEAB57)

## Documentation Map

```
docs/
├── 00_START_HERE.md          ← You are here
├── 01_PROJECT_VISION.md      Mission, philosophy, long-term goals
├── 02_DESIGN_LANGUAGE.md     UI philosophy, spacing, typography, shadows
├── 03_BRANDING.md            Logo, colors, voice, rebranding guide
├── 04_ROADMAP.md             Milestones v1.1 through v3.0
├── 05_FEATURES.md            Every feature with status
├── 06_ARCHITECTURE.md        Project structure, modules, data flow
├── 07_THEME_SYSTEM.md        Theme tokens, built-in themes, custom themes
├── 08_UI_GUIDELINES.md       Component specs, interaction rules
├── 09_ANIMATION_GUIDELINES.md Animation philosophy, presets, patterns
├── AI_CONTEXT.md             Written for AI assistants — read this first
├── Commands.md               Searchable command library for development and releases
│
├── troubleshooting/           Build, setup, platform, Git, and toolchain issues
│   ├── README.md              Troubleshooting index and issue template
│   ├── BuildErrors.md         Recorded build failures and warnings
│   ├── Gradle.md              Gradle configuration and dependency issues
│   ├── Java.md                JDK and JAVA_HOME issues
│   ├── Git.md                 Repository and workflow issues
│   ├── VSCode.md              Editor and language-server issues
│   ├── Android.md             Android SDK and APK issues
│   ├── Desktop.md             Desktop runtime and packaging issues
│   ├── iOS.md                 iOS host and Xcode issues
│   ├── Windows.md             Windows shell and environment issues
│   ├── Linux.md               Linux permissions and desktop issues
│   ├── macOS.md               macOS/Xcode issues
│   └── CommonProblems.md      Cross-platform recurring issues
│
├── setup/                     Fresh machine and first-build guides
│   ├── FreshSetup.md
│   ├── FirstBuild.md
│   ├── RequiredSoftware.md
│   └── UpdatingDependencies.md
│
├── maintenance/               Ongoing project maintenance records
│   ├── DependencyUpdates.md
│   ├── VersionHistory.md       Release and solved-issue history
│   └── KnownLimitations.md     Statused limitations
│
├── development/
│   ├── DEVELOPMENT_SETUP.md  From zero to running
│   ├── CONTRIBUTING.md       How to contribute
│   ├── GITHUB_WORKFLOW.md    Git, branches, releases
│   ├── RELEASE_GUIDE.md      Release process step by step
│   ├── STYLE_GUIDE.md        Coding standards
│   └── VIBE_CODING_GUIDE.md  For AI-assisted development
│
├── planning/
│   ├── TODO.md               Master task list
│   ├── CURRENT_ISSUES.md     Living bug/issue tracker
│   ├── CHANGELOG.md          Human-readable release history
│   └── COMPLETED.md          Completed features by version
│
├── features/
│   ├── FLASHCARDS.md
│   ├── ANKI_IMPORT.md
│   ├── ANKI_EXPORT.md
│   ├── TAG_SYSTEM.md
│   ├── FLAG_SYSTEM.md
│   ├── SEARCH.md
│   ├── STATS.md
│   ├── THEMES.md
│   ├── MOBILE.md
│   └── DESKTOP.md
│
├── assets/
│   ├── logo.md
│   ├── colors.md
│   ├── icons.md
│   └── mockups.md
│
├── api/
│   ├── DATABASE.md
│   ├── SETTINGS.md
│   ├── SYNC.md
│   └── IMPORT_EXPORT.md
│
└── decisions/
    ├── 0001-brand.md
    └── 0002-theme.md
```

## How to Start Developing

1. Read `development/DEVELOPMENT_SETUP.md` for environment setup
2. Read `AI_CONTEXT.md` for AI-assisted development workflow
3. Check `planning/CURRENT_ISSUES.md` for what needs fixing
4. Check `planning/TODO.md` for prioritized tasks
5. Follow `development/STYLE_GUIDE.md` for code standards
6. Read `02_DESIGN_LANGUAGE.md` and `08_UI_GUIDELINES.md` before making UI changes
7. Read `troubleshooting/README.md` and record any solved issue immediately

## Current Status

- **Build**: Desktop compiles successfully
- **Window**: Undecorated with floating controls, drag region needs fixing
- **Themes**: 8 built-in themes planned, Signature (Lime+Orange) implemented
- **Sidebar**: Basic implementation, needs floating island redesign
- **Appearance Studio**: Basic implementation, needs full Theme Studio

## Key Principles

1. **Desktop first** — Most serious study happens at a desk
2. **Craft over features** — Every pixel should feel intentional
3. **Offline by default** — Learning should not depend on internet
4. **Open source** — Transparency builds trust
5. **No gamification** — Treat users as capable adults
