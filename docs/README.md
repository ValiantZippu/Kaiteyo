# Kaiteyo (書いてよ) — Documentation

Kaiteyo is a premium, cross-platform Japanese language learning application. Originally a
fork of Kanji Dojo, it has been redesigned with a focus on desktop-first UX and a cohesive
design system. This is the entry point to all project documentation — it behaves like a
navigable documentation site, even when viewed directly on GitHub.

> **Status of the docs:** this tree is maintained alongside the code. If you find a
> contradiction between a document and the source, the source wins — please report it
> (see [Contributing](../CONTRIBUTING.md)).

## Quick facts

| Attribute | Value |
|-----------|-------|
| Platform | Desktop (Windows/macOS/Linux), Android, iOS |
| UI framework | Compose Multiplatform 1.8.2 |
| Language | Kotlin (KMP) 2.1.20 |
| Build system | Gradle (version catalog, JDK 17) |
| DI | Koin |
| Database | SQLDelight (app data + user data) + DataStore preferences |
| HTTP | Ktor |
| Current version | 2.2.1 (see [CHANGELOG](../CHANGELOG.md)) |

## Documentation map

```
docs/
├── README.md                      ← you are here
│
├── architecture/                  How Kaiteyo is built
│   ├── OVERVIEW.md                Modules, UI architecture, data flow
│   ├── FILE_STRUCTURE.md          Repository layout reference
│   ├── DATA_PLATFORM.md           KJD language data platform (jdata)
│   ├── SYNC.md                    Sync architecture
│   ├── ACCOUNT.md                 Account structure
│   ├── NAVIGATION.md              Navigation system
│   └── decisions/                 Architecture Decision Records (ADRs)
│
├── data/                          Data architecture & open-source data sources
│   ├── ARCHITECTURE.md            Where data lives, databases, migrations, caching
│   └── SOURCES.md                 Dataset provenance, licenses, redistribution
│
├── design/                        Design system & UX
│   ├── DESIGN_SYSTEM.md           Complete visual identity design system
│   ├── DESIGN_LANGUAGE.md         UI philosophy, spacing, typography
│   ├── UI_SYSTEM.md               Component specs, interaction rules
│   ├── THEME_SYSTEM.md            Theme tokens, built-in themes, custom themes
│   └── ANIMATION_SYSTEM.md        Animation philosophy, presets, patterns
│
├── branding/                      Brand assets & guidelines
│
├── features/                      Feature specs & status
│   ├── FEATURES.md                Full feature status matrix
│   ├── DESKTOP.md                 Desktop suite feature set
│   ├── LIBRARY.md                 Library experience
│   ├── MEDIA.md                   Media center
│   ├── STATISTICS.md              Statistics & analytics
│   └── THEMES.md                  Theme gallery and theming
│
├── user-guide/                    End-user documentation
│   ├── GETTING_STARTED.md         Install, first launch, onboarding
│   ├── STUDYING.md                Kanji, vocabulary, writing, SRS, decks
│   ├── DESKTOP_SUITE.md           Dictionary, media, mining, OCR, browser
│   └── CUSTOMIZATION.md           Themes, settings, shortcuts
│
├── integrations/                  Third-party & external integrations
│   ├── ANKI.md                    Anki .apkg + AnkiConnect
│   ├── YOMITAN_DICTIONARIES.md    Yomitan-compatible dictionary import
│   ├── MEDIA_BACKENDS.md          VLC / mpv / Java Sound backends
│   ├── LOCAL_API.md               Localhost HTTP API
│   └── PLUGINS.md                 Plugin registry & marketplace (planned)
│
├── platform/                      Per-platform documentation
│   ├── WINDOWS.md / LINUX.md / MACOS.md / ANDROID.md / IOS.md
│
├── releases/                      Release engineering
│   ├── RELEASE_PROCESS.md         End-to-end release workflow
│   └── RELEASE_CHECKLIST.md       Pre-release verification checklist
│
├── security/                      Security & privacy
│   ├── README.md                  Threat model (see also root SECURITY.md)
│   └── PRIVACY.md                 What data is stored, what leaves the device
│
├── legal/                         Licensing & third-party notices
│   ├── README.md                  License structure
│   └── THIRD_PARTY_NOTICES.md     Third-party data & libraries
│
├── testing/                       Testing strategy
│   └── README.md                  Test levels, locations, commands
│
├── api/                           Database, settings, sync API notes
│
├── development/                   For developers
│   ├── AI_CONTEXT.md              Read this first (AI-assisted workflow)
│   ├── COMMANDS.md                Command library
│   ├── DEVELOPER_GUIDE.md         Development guide
│   ├── CODING_STANDARDS.md        Coding standards
│   ├── DEVELOPMENT_SETUP.md       From zero to running
│   ├── GITHUB_WORKFLOW.md         Git, branches, releases
│   ├── DocumentationRules.md      Rules for maintaining docs
│   └── VIBE_CODING_GUIDE.md       AI-assisted development
│
├── contributing/                  Contribution guide (canonical: root CONTRIBUTING.md)
│
├── setup/                         Fresh machine & first build guides
│
├── maintenance/                   Dependency, version, limitation records
│
├── planning/                      Project planning (living docs)
│   ├── TODO.md                    Master task list
│   ├── CURRENT_ISSUES.md          Living bug/issue tracker
│   ├── COMPLETED.md               Completed features by version
│   ├── FUTURE_IDEAS.md            Backlog of ideas
│   └── README.md                  Planning index & status taxonomy
│
├── roadmap/                       Vision & roadmap
│   ├── PROJECT_VISION.md          Mission and philosophy
│   └── ROADMAP.md                 Milestones and version plan
│
├── guides/                        Beginner, setup, and Git guides
│
├── troubleshooting/               Solved-issue knowledge base
│
└── screenshots/                   Desktop screen captures
```

## How to start developing

1. Read `development/DEVELOPMENT_SETUP.md` for environment setup
2. Read `development/AI_CONTEXT.md` for the AI-assisted development workflow (and the
   "never change" list)
3. Check `planning/CURRENT_ISSUES.md` for what needs fixing
4. Check `planning/TODO.md` for prioritized tasks
5. Follow `development/CODING_STANDARDS.md` for code standards
6. Read `design/DESIGN_LANGUAGE.md` and `design/UI_SYSTEM.md` before making UI changes
7. Record any solved issue in `troubleshooting/README.md` immediately

## How to use Kaiteyo

- Start with `user-guide/GETTING_STARTED.md`
- Study workflow: `user-guide/STUDYING.md`
- Desktop immersion suite (dictionary, media, mining): `user-guide/DESKTOP_SUITE.md`
- Customization: `user-guide/CUSTOMIZATION.md`

## Documentation principles

1. **Documentation reflects reality.** Unfinished features are labeled as planned/partial;
   broken behavior is recorded as a known issue.
2. **Every folder has a purpose** and a README/index where useful.
3. **No dead links** — broken links are bugs (see `development/DocumentationRules.md`).
4. **Planning is separate from user documentation.**
5. **Third-party attribution is explicit** — see `data/SOURCES.md` and `legal/`.
