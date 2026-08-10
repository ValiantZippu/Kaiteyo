# Kaiteyo (書いてよ) — Documentation

Kaiteyo is a premium, cross-platform Japanese language learning application. Originally a fork of
Kanji Dojo, it has been completely redesigned with a focus on desktop-first UX, beautiful
craftsmanship, and a cohesive design system.

The name **Kaiteyo** (書いてよ) means "write it!" in Japanese — an invitation to practice and
engage with the language actively.

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

## Documentation Map

```
docs/
├── README.md                     ← You are here
│
├── architecture/
│   ├── OVERVIEW.md               Project structure, modules, data flow
│   ├── SYNC.md                   Sync architecture
│   ├── ACCOUNT.md                Account structure
│   ├── NAVIGATION.md             Navigation system
│   └── FILE_STRUCTURE.md         Repository file structure reference
│
├── design/
│   ├── DESIGN_SYSTEM.md          Design system overview
│   ├── DESIGN_LANGUAGE.md        UI philosophy, spacing, typography, shadows
│   ├── UI_SYSTEM.md              Component specs, interaction rules
│   ├── THEME_SYSTEM.md           Theme tokens, built-in themes, custom themes
│   └── ANIMATION_SYSTEM.md       Animation philosophy, presets, patterns
│
├── branding/
│   ├── BRAND_GUIDELINES.md       Brand guidelines
│   ├── BRANDING.md               Logo, colors, voice, rebranding guide
│   └── README.md                 Brand assets index
│
├── roadmap/
│   ├── PROJECT_VISION.md         Mission, philosophy, long-term goals
│   └── ROADMAP.md                Milestones and version plan
│
├── features/
│   ├── FEATURES.md               Every feature with status
│   ├── DESKTOP.md                Desktop feature set
│   ├── LIBRARY.md                Library experience
│   ├── THEMES.md                 Theme gallery and theming
│   └── README.md                 Feature specs index
│
├── development/
│   ├── AI_CONTEXT.md             Written for AI assistants — read this first
│   ├── COMMANDS.md               Searchable command library for development and releases
│   ├── DEVELOPER_GUIDE.md        Development guide
│   ├── CODING_STANDARDS.md       Coding standards
│   ├── DEVELOPMENT_SETUP.md      From zero to running
│   ├── GITHUB_WORKFLOW.md        Git, branches, releases
│   ├── DOCUMENTATION_RULES.md    Rules for maintaining documentation
│   └── VIBE_CODING_GUIDE.md      For AI-assisted development
│
├── contributing/
│   └── CONTRIBUTING.md           How to contribute
│
├── assets/
│   └── ASSETS.md                 App data assets and media pipeline
│
├── releases/
│   └── RELEASE_PROCESS.md        Release process step by step
│
├── api/
│   └── README.md                 API documentation (database, settings, sync)
│
├── guides/
│   ├── BEGINNER_GUIDE.md
│   ├── SETUP_GUIDE.md
│   ├── GIT_GUIDE.md
│   └── README.md
│
├── planning/
│   ├── TODO.md                   Master task list
│   ├── CURRENT_ISSUES.md         Living bug/issue tracker
│   ├── CHANGELOG.md              Human-readable release history
│   ├── COMPLETED.md              Completed features by version
│   ├── FUTURE_IDEAS.md           Backlog of ideas
│   └── README.md                 Planning index
│
├── decisions/
│   ├── 0001-brand.md             ADR: brand
│   └── 0002-theme.md             ADR: theme
│
├── setup/
│   ├── FreshSetup.md             Fresh machine setup
│   ├── FirstBuild.md             First build walkthrough
│   ├── RequiredSoftware.md       Required toolchain
│   └── UpdatingDependencies.md   How to update dependencies
│
├── maintenance/
│   ├── DependencyUpdates.md      Dependency update log
│   ├── VersionHistory.md         Release and solved-issue history
│   └── KnownLimitations.md       Statused limitations
│
├── troubleshooting/
│   ├── README.md                 Troubleshooting index
│   ├── BuildErrors.md / Gradle.md / Java.md / Git.md / VSCode.md
│   ├── Android.md / Desktop.md / iOS.md
│   ├── Windows.md / Linux.md / macOS.md / CommonProblems.md
│
└── screenshots/                  Screen captures and mockups (see README.md)
```

## How to Start Developing

1. Read `development/DEVELOPMENT_SETUP.md` for environment setup
2. Read `development/AI_CONTEXT.md` for AI-assisted development workflow
3. Check `planning/CURRENT_ISSUES.md` for what needs fixing
4. Check `planning/TODO.md` for prioritized tasks
5. Follow `development/CODING_STANDARDS.md` for code standards
6. Read `design/DESIGN_LANGUAGE.md` and `design/UI_SYSTEM.md` before making UI changes
7. Read `troubleshooting/README.md` and record any solved issue immediately

## Key Principles

1. **Desktop first** — Most serious study happens at a desk
2. **Craft over features** — Every pixel should feel intentional
3. **Offline by default** — Learning should not depend on internet
4. **Open source** — Transparency builds trust
5. **No gamification** — Treat users as capable adults
