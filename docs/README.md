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
├── product/                       Product definition — WHAT Kaiteyo is
│   ├── README.md                  Product section index
│   ├── PRODUCT.md                 **The Master Blueprint** (§0–§88, product wrapper)
│   ├── VISION.md                  Product vision (supersedes roadmap/PROJECT_VISION.md)
│   └── PRINCIPLES.md              Checkable principles per domain
│
├── engineering/                   The engineering constitution
│   ├── README.md                  Engineering index (maps §84 targets → real locations)
│   └── ENGINEERING_STANDARDS.md   Professional engineering standard (§163–§376, ADR-0012)
│
├── ai/                            Guidance for AI coding agents
│   └── AI_AGENT_GUIDE.md          Binding agent guide: read order, workflow, task selection
│
├── architecture/                  How Kaiteyo is built
│   ├── OVERVIEW.md                Modules, UI architecture, data flow
│   ├── FILE_STRUCTURE.md          Repository layout reference
│   ├── DATA_PLATFORM.md           KJD language data platform (jdata)
│   ├── SYNC.md                    Sync architecture
│   ├── ACCOUNT.md                 Account structure
│   ├── NAVIGATION.md              Navigation system
│   ├── NODE_ARCHITECTURE.md       Node system & Journey product spec (§76–§162)
│   ├── nodes/                     Node architecture reference docs
│   │   ├── README.md              Node docs index + master conceptual graph (§160)
│   │   ├── NODE_TYPE_REGISTRY.md  Every node type: fields, sources, status
│   │   ├── RELATIONSHIP_REGISTRY.md  Typed relationship vocabulary (§79–§80)
│   │   ├── KNOWLEDGE_STATE_MODEL.md  User knowledge: dimensions, states, scoring (§84–§85)
│   │   ├── JOURNEY_WORLD_SCHEMA.md   World content: cells, objects, NPCs, quests, dialogue (§88–§113)
│   │   ├── JOURNEY_RUNTIME_SPEC.md   Journey runtime: UI layers, HUD, overlays, save, perf (§137–§144)
│   │   ├── JOURNEY_SLICE_CONTENT.md  Worked Kamakura+Enoshima slice: reference JSON (§91)
│   │   ├── CONTENT_AUTHORING.md      Authoring pipeline, validation gates, packages (§145–§148)
│   │   ├── NODE_DATA_MODEL.md        Node-layer storage contract: node/edge/knowledge/event/save
│   │   ├── EVENT_CATALOG.md          Event catalog behind stats & knowledge (§210–§211)
│   │   ├── SERVICE_CONTRACTS.md      Internal service interfaces (§209, §244)
│   │   ├── TEST_PLAN.md              Test contract for node & Journey (§215–§218)
│   │   ├── UX_FLOWS.md               UX flows: existing surfaces + Journey (§296–§299)
│   │   └── GAMEPLAY_SYSTEMS.md       Journey gameplay systems spec (§86–§119)
│   ├── README.md                  Architecture index — every subsystem doc (§175)
│   ├── database.md                Database spec & migration plan
│   ├── language-model.md          Language data model & knowledge graph
│   ├── dictionary.md              Dictionary engine spec
│   ├── study-engine.md            Study engine & card model
│   ├── statistics.md              Statistics & event model
│   ├── exams.md                   Exam system spec
│   ├── media.md                   Media, playback & subtitle engine
│   ├── WORLD_SYSTEM.md            World runtime, chunks, terrain, NPCs, trains (implemented)
│   ├── mining.md                  Mining workflow spec
│   ├── integrations.md            Integrations spec
│   ├── browser.md                 Embedded browser architecture (planned)
│   ├── journey.md                 Journey, world & game runtime plan
│   ├── localization.md            Localization plan
│   ├── performance.md             Performance strategy & budgets
│   ├── toolchain.md               Toolchain & dev environment
│   ├── backup.md                  Backup, import & export
│   ├── ci-cd.md                   CI/CD & release pipeline
│   ├── accessibility.md           Accessibility plan
│   ├── content.md                 Content system & authoring
│   ├── assets.md                  Asset system specification
│   ├── pitch-frequency.md         Pitch accent & frequency architecture
│   └── decisions/                 Architecture Decision Records (ADR-0001…0019)
│
├── vision/                        Why Kaiteyo exists (philosophy layer)
│   ├── README.md                  Vision index
│   ├── product-vision.md          What Kaiteyo is / is not; pillars
│   ├── design-philosophy.md       How it must feel (§53–§56)
│   ├── learning-philosophy.md     Learning doctrine
│   ├── game-philosophy.md         Journey as a real game (no XP)
│   ├── child-experience.md        Child instructional structure
│   ├── normal-user-experience.md  Two interconnected experiences
│   └── long-term-vision.md        The arc to a language platform
│
├── game/                          Journey — the game as a real game
│   ├── README.md                  Game docs index & map (implementation + target)
│   ├── ENGINE_DECISION.md         Why the engine core is 2.5D Canvas today + 3D swap path
│   ├── ARCHITECTURE.md            The implemented architecture (desktop/game package map)
│   ├── WORLD.md                   World structure + content pipeline (add a region/quest/word)
│   ├── VERTICAL_SLICE.md          Honest per-system status of the playable slice
│   ├── ROADMAP.md                 Living roadmap (foundation → slice → regions → expansion)
│   ├── TODO.md                    Massive categorized backlog (spec §133)
│   ├── game-overview.md           Genre, pillars, core loop, scope gates (target)
│   ├── world-architecture.md      Hierarchy, fidelity L0–L4, packages (target)
│   ├── map-system.md              Map modes, reveal, knowledge-density overlay
│   ├── world-streaming.md         Chunks, managers, budgets, travel
│   ├── camera.md                  First/third person + camera modes
│   ├── player.md                  Player/avatar, camera summary, input abstraction
│   ├── interaction-system.md      InteractionComponent & interaction types
│   ├── npc-system.md              NPC model & simulation tiers
│   ├── dialogue-system.md         Data-driven dialogue trees
│   ├── quest-system.md            Quest node model & non-punitive rules
│   ├── progression-rewards.md     Anti-grind progression & rewards
│   ├── collectibles-photography.md  Discovery, photos, collections, swimming
│   ├── transportation.md          Trains/stations — scalable network
│   ├── environment-simulation.md  Time, weather, seasons (deterministic)
│   ├── learning-in-world.md       WORLD_TEXT_SELECTED → knowledge flow
│   ├── save-system.md             Versioned, checksummed, split-rule saves
│   ├── game-audio.md              Audio buses, zones, mixing
│   └── asset-pipeline.md          Game asset pipeline (naming, formats, licenses, packages)
│
├── rendering/                     Journey rendering direction (target)
│   ├── README.md                  Rendering index & ground rules
│   ├── rendering-architecture.md  Artistic direction, toon hybrid, LOD
│   ├── environment-visuals.md     Weather/water/vegetation/particles
│   └── rendering-performance.md   Per-tier budgets (proposed targets)
│
├── input/                         Input layer
│   ├── README.md                  Input index
│   ├── input-system.md            Abstract action layer, profiles, remapping
│   ├── game-controls.md           Journey default mappings (all devices)
│   ├── mobile-controls.md         Touch design, joystick, gestures, controller
│   └── accessibility.md           Input accessibility (keyboard-first)
│
├── learning/                      Learning depth (target + live)
│   ├── README.md                  Learning index
│   ├── curriculum-engine.md        Graph-based curriculum
│   ├── adaptive-learning.md       Difficulty vectors & recommendations
│   ├── kanji-knowledge-graph.md   Kanji graph navigation (§9)
│   ├── progress-model.md          Knowledge states & evidence model
│   └── reading-stories.md         Graded reading & story content
│
├── content/                       Authorable content formats (target)
│   ├── README.md                  Content index
│   ├── content-formats.md         JSON schemas: quest/dialogue/story/exam/…
│   └── content-pipeline.md        Validation gates, packages, install (ADR-0015)
│
├── tools/                         Developer tooling
│   └── README.md                  Tooling map (CLI live; proposals)
│
├── reference/                     Lookup layer
│   ├── README.md                  Reference index
│   ├── glossary.md                Terminology across specs
│   └── subsystem-map.md           Subsystem → docs/status/entry points
│
├── production/                    Delivery layer
│   ├── README.md                  Production index
│   ├── phases.md                  Phase graph & implementation order
│   ├── risk-register.md           Risks with P/I/mitigation/owner (§62)
│   ├── technical-debt.md          Deliberate paydowns
│   └── project-audit.md           §63 audit + TOP-100 tasks + TOP-100 risks
│
├── website/                       Website docs + project command center
│   ├── README.md                  Site surfaces, command center, content plan, web trial
│   ├── ARCHITECTURE.md            Build flow, data flow, honesty rules, maintenance
│   ├── DATA_MODEL.md              Unified project data model + target schema
│   └── API.md                     Interactive-layer API contracts (auth, kanban, whiteboard, suggestions, realtime)
│
├── media/                         Media workflows (suite + product target)
│   ├── YOMITAN.md                 Yomitan-style glossing: research + native design
│   └── ASBPLAYER_WORKFLOW.md      ASBPlayer-style subtitle mining workflow
│
├── ui/                            UI catalogs
│   └── SETTINGS.md                Settings catalog (default · range · effect · persistence)
│
├── database/                      Database migration policy
│   └── MIGRATIONS.md              Migration rules, destructive-change gate, testing matrix
│
├── data/                          Data architecture & open-source data sources
│   ├── README.md                  Data section index
│   ├── ARCHITECTURE.md            Where data lives, databases, migrations, caching
│   └── SOURCES.md                 Dataset provenance, licenses, redistribution
│
├── design/                        Design system & UX
│   ├── README.md                  Design section index & principles
│   ├── DESIGN_SYSTEM.md           Complete visual identity design system
│   ├── DESIGN_LANGUAGE.md         UI philosophy, spacing, typography
│   ├── UI_SYSTEM.md               Component specs, interaction rules
│   ├── THEME_SYSTEM.md            Theme tokens, built-in themes, custom themes
│   ├── ANIMATION_SYSTEM.md        Animation philosophy, presets, patterns
│   └── KAITEYO_EXPRESSIVE.md      **Expressive design system** — theming architecture,
│                                  UX principles, UI guidelines, full color palettes,
│                                  semantic tokens, heatmap design language, accessibility
│
├── assets/                        Asset inventory & branding assets
│   └── ASSETS.md                  Logos, icons, palettes, resource locations
│
├── branding/                      Brand assets & guidelines
│   ├── README.md                  Asset inventory + logo rules
│   ├── BRAND_GUIDELINES.md        Full brand guidelines
│   └── BRANDING.md                Rebranding history & sweep checklist
│
├── features/                      Feature specs & status
│   ├── README.md                  Feature section index & status legend
│   ├── FEATURES.md                Full feature status matrix (source of truth)
│   ├── DESKTOP.md                 Desktop suite feature set
│   ├── LIBRARY.md                 Library experience
│   ├── MEDIA.md                   Media center
│   ├── STATISTICS.md              Statistics & analytics
│   └── THEMES.md                  Theme gallery and theming
│
├── user-guide/                    End-user documentation
│   ├── README.md                  User guide index & platform coverage
│   ├── GETTING_STARTED.md         Install, first launch, onboarding
│   ├── STUDYING.md                Kanji, vocabulary, writing, SRS, decks
│   ├── DESKTOP_SUITE.md           Dictionary, media, mining, OCR, browser
│   └── CUSTOMIZATION.md           Themes, settings, shortcuts
│
├── integrations/                  Third-party & external integrations
│   ├── README.md                  Integrations index & status matrix
│   ├── ANKI.md                    Anki .apkg + AnkiConnect
│   ├── YOMITAN_DICTIONARIES.md    Yomitan-compatible dictionary import
│   ├── MEDIA_BACKENDS.md          VLC / mpv / Java Sound backends
│   ├── LOCAL_API.md               Localhost HTTP API
│   └── PLUGINS.md                 Plugin registry & marketplace (planned)
│
├── platform/                      Per-platform documentation
│   ├── README.md                  Platform index
│   ├── WINDOWS.md / LINUX.md / MACOS.md / ANDROID.md / IOS.md
│
├── releases/                      Release engineering
│   ├── README.md                  Release section index
│   ├── RELEASE_PROCESS.md         End-to-end release workflow
│   └── RELEASE_CHECKLIST.md       Pre-release verification checklist
│
├── distribution/                  Distribution, packaging & installation
│   ├── README.md                  Package matrix + index (Windows/Linux/Android)
│   ├── architecture.md            Build → package → install → update → sign → release
│   ├── windows.md                 Windows: EXE/MSI/portable, silent, package managers
│   ├── windows-package-managers.md  WinGet / Chocolatey / Scoop manifests
│   ├── linux.md · linux-appimage.md · linux-debian.md · linux-ubuntu.md
│   ├── linux-fedora.md · linux-flatpak.md · linux-arch.md
│   ├── android.md                 Flavors, APK/AAB, signing, Play/F-Droid
│   ├── installers.md · uninstall.md · signing.md · updates.md · checksums.md
│   ├── release-process.md · ci-cd.md · security.md
│   ├── onboarding.md · first-launch.md · troubleshooting.md · faq.md
│   └── artifacts.md · versioning.md · localization.md
│
├── packaging/                     Packaging SOURCE index (installer/ tree)
│   └── README.md                  installer/ layout → doc map
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
│   └── README.md                  API index (references planned)
│
├── cli/                           Developer command center (kaiteyo CLI)
│   ├── README.md                  CLI overview, installation, quick start
│   ├── COMMANDS.md                Full command reference
│   ├── CONFIGURATION.md           Config files, keys, precedence
│   ├── AUTOMATION.md              Scripting / CI / JSON output / exit codes
│   ├── ARCHITECTURE.md            How the CLI is built; adding new tools
│   └── TROUBLESHOOTING.md         Common problems & fixes
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
├── contributing/                  Contribution guide
│   └── CONTRIBUTING.md            How to contribute (canonical: root CONTRIBUTING.md)
│
├── setup/                         Fresh machine & first build guides
│   ├── FreshSetup.md              From zero to a working checkout
│   ├── FirstBuild.md              First build walkthrough
│   ├── RequiredSoftware.md        Prerequisites per platform
│   └── UpdatingDependencies.md    How dependencies are bumped
│
├── maintenance/                   Dependency, version, limitation records
│   ├── DependencyUpdates.md       Dependency change log
│   ├── KnownLimitations.md        Living limitation list
│   └── VersionHistory.md          Dated build/release history
│
├── planning/                      Project planning (living docs)
│   ├── TODO.md                    Master task list
│   ├── CURRENT_ISSUES.md          Living bug/issue tracker
│   ├── COMPLETED.md               Completed features by version
│   ├── FUTURE_IDEAS.md            Backlog of ideas
│   ├── PRODUCT_AUDIT.md           Product audit: real / duplicated / dead / fake
│   ├── ENGINEERING_AUDIT.md       Engineering audit & agent handoff (§376 deliverables)
│   ├── MASTER_TODO.md             Master TODO: full work packages P0–P39 (KT-* IDs)
│   ├── CURRENT_STATE.md           Per-subsystem status matrix (live audit map)
│   └── README.md                  Planning index & status taxonomy
│
├── roadmap/                       Vision & roadmap
│   ├── PROJECT_VISION.md          Mission and philosophy
│   ├── ROADMAP.md                 Milestones and version plan
│   ├── FUTURE_FEATURES.md         **Complete feature roadmap** — all planned features
│   │                              organized by domain (learning, dictionary, desktop,
│   │                              mobile, game, sync, community) with priority and effort
│   ├── FUTURE_LIBRARIES.md        **Libraries & dependencies roadmap** — every library
│   │                              Kaiteyo could adopt, with KMP compatibility and
│   │                              integration notes (NLP, UI, data, media, testing)
│   └── YOMITAN_INTEGRATION.md     **Yomitan & dictionary roadmap** — full Yomitan format
│                                  spec, native dictionary architecture, compatibility
│                                  matrix, and 5-phase implementation plan
│
├── guides/                        Beginner, setup, and Git guides
│   ├── README.md                  Guides index
│   ├── BEGINNER_GUIDE.md          First steps with Kaiteyo
│   ├── SETUP_GUIDE.md             Setup walkthrough
│   └── GIT_GUIDE.md               Git workflow
│
├── troubleshooting/               Solved-issue knowledge base
│   ├── README.md                  Index by symptom
│   ├── BuildErrors.md · CommonProblems.md · Gradle.md · Java.md
│   ├── Desktop.md · Windows.md · macOS.md · Linux.md · Android.md · iOS.md
│   └── VSCode.md · Git.md
│
└── screenshots/                   Desktop screen captures
    └── README.md                  Capture conventions & pending assets
```

## How to start developing

1. Read `development/DEVELOPMENT_SETUP.md` for environment setup
2. Read `engineering/ENGINEERING_STANDARDS.md` — the engineering contract (ADR-0012)
3. Read `development/AI_CONTEXT.md` for the AI-assisted development workflow (and the
   "never change" list)
4. Check `planning/ENGINEERING_AUDIT.md` for the audit, dependency map and starting files
5. Check `planning/CURRENT_ISSUES.md` for what needs fixing
6. Check `planning/MASTER_TODO.md` + `planning/TODO.md` for prioritized tasks
7. Check `production/project-audit.md` for the §63 audit (TOP-100 tasks + risks)
8. Read `vision/` before planning product work; `game/` + `rendering/` + `input/` before
   any Journey work (all TARGET)
9. Use `reference/subsystem-map.md` to find the canonical docs for any subsystem
10. Follow `development/CODING_STANDARDS.md` for code standards
11. Read `design/DESIGN_LANGUAGE.md` and `design/UI_SYSTEM.md` before making UI changes
12. Before touching a subsystem, read its spec under `architecture/` (Specifications)
13. Record any solved issue in `troubleshooting/README.md` immediately

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
