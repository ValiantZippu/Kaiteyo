# Kaiteyo Engineering Standards (§163–§376)

**Status**: Accepted — binding engineering contract (adopted via
[ADR-0012](../architecture/decisions/0012-engineering-standards.md))
**Date adopted**: 2026-08-15
**Purpose**: Kaiteyo is a serious professional software product. Every change — human or AI —
must be developed as part of a real software system with architecture, engineering standards,
source control, reproducible builds, automated validation, dependency management, modularity,
testing, observability, documentation, licensing, security, performance engineering,
accessibility, platform-specific behavior, release management, and a migration strategy.
Every implementation decision must be explainable.

The §376 stop-condition deliverables for planning phases live in
[`docs/planning/ENGINEERING_AUDIT.md`](../planning/ENGINEERING_AUDIT.md). Section numbering
(§NNN) is a stable reference; new decisions may extend it.

---

## Table of contents

| Part | Sections | Theme |
|---|---|---|
| I | §163–§175 | Professional constitution, engineering rules, AI workflow, handoffs |
| II | §176–§178 | Architecture, module boundaries, domain-first, UI separation |
| III | §179–§192 | Database, migrations, data ingestion, search, performance |
| IV | §193–§201 | Media, subtitles, mining, Yomitan, browser, Anki |
| V | §202–§214 | Licensing, dependency policy, security, user data, services, events, stats |
| VI | §215–§221 | Testing, crash prevention, logging |
| VII | §222–§228 | Feature flags, design system, themes, assets, icons |
| VIII | §229–§235 | Documentation, ADRs, CI/CD, releases, installers, updates |
| IX | §236–§241 | Developer CLI, toolchain, development doctor |
| X | §242–§253 | Journey, game engine evaluation, world creation, input |
| XI | §254–§264 | Accessibility, localization, content authoring, plugins, API stability |
| XII | §265–§278 | Observability, caching, networking, sync, state, concurrency, memory |
| XIII | §279–§290 | Large datasets, Unicode, fonts, drawing, exams, statistical honesty |
| XIV | §291–§299 | External services, privacy, telemetry, error UX, empty/loading/offline states |
| XV | §300–§309 | QA, device classes, responsive modes, window management, motion |
| XVI | §310–§320 | Code style, naming, god classes, duplication, TODOs, priorities |
| XVII | §321–§330 | Roadmap, vertical slices, definition of done, no fakes, repo hygiene |
| XVIII | §331–§340 | Documentation structure, changelog, versioning, release checklist |
| XIX | §341–§356 | Build discipline, validation ladder, source of truth, migration, debt, reviews |
| XX | §357–§361 | Security of untrusted input (media, imports, plugins, browser, game content) |
| XXI | §362–§376 | Final principles, technology selection, implementation order, stop condition |

---

## Part I — Professional constitution, engineering rules, AI workflow

### 163. Kaiteyo Professional Engineering Standard

From this point forward, Kaiteyo must be treated as a serious professional software product.

It must NOT be developed as a collection of AI-generated screens.

It must NOT be developed as a collection of patches.

It must NOT be developed by repeatedly fixing whatever the previous AI broke.

It must be developed as a real software system with:

- architecture
- engineering standards
- source control
- reproducible builds
- automated validation
- dependency management
- modularity
- testing
- observability
- documentation
- licensing
- security
- performance engineering
- accessibility
- platform-specific behavior
- release management
- migration strategy

Every implementation decision must be explainable.

### 164. Primary Engineering Rule

DO NOT OPTIMIZE FOR: "How many lines of code can we generate?"

OPTIMIZE FOR:

- correctness
- maintainability
- performance
- clarity
- testability
- extensibility
- user experience
- data integrity
- reliability

A 2,000-line implementation that correctly uses an established library is better than
20,000 lines of unnecessary replacement code.

Do not reinvent:

- video codecs
- audio codecs
- HTTP
- SQLite
- JSON parsing
- cryptography
- image decoding
- font rendering
- GPU rendering
- TLS
- compression
- Git
- Anki synchronization protocols
- dictionary parsing

unless there is an actual product-specific reason.

### 165. Engineering Before Coding

Before modifying code:

1. inspect repository
2. inspect build system
3. inspect modules
4. inspect dependencies
5. inspect existing architecture
6. inspect database
7. inspect UI architecture
8. inspect platform targets
9. inspect existing tests
10. inspect documentation
11. inspect TODOs
12. inspect Git history
13. identify unfinished systems
14. identify broken systems
15. identify duplicated systems
16. identify architectural debt

Do not immediately start writing code. First understand the existing system.

### 166. Never Rewrite the Project Blindly

Do not replace the existing architecture merely because another architecture appears cleaner.

Before replacing something, document:

- CURRENT ARCHITECTURE
- PROBLEM
- LIMITATION
- PROPOSED ARCHITECTURE
- MIGRATION COST
- BENEFIT
- RISK

Only then decide.

### 167. Development Environment

The project should have a documented development environment. Document:

- operating systems
- IDE
- JDK/runtime
- compiler
- build tool
- package manager
- version control
- database tools
- asset tools
- testing tools
- debugging tools
- profiling tools

Pin important versions. Do not rely on "latest" for critical build dependencies.

### 168. Primary Code Editor

Support a professional IDE/editor workflow.

Recommended: JetBrains IDEs for Kotlin/JVM-heavy development.

VS Code may be used for: documentation, web, scripts, JSON, YAML, Markdown, asset
organization, auxiliary tooling.

Do not force every task into one editor.

### 169. Source Control

Git is mandatory. Repository hosting: GitHub or equivalent.

Use:

- main
- development/integration branches where necessary
- feature branches
- bugfix branches
- release branches where justified

Do not make enormous uncontrolled commits. Commit logically. Example:

```
feat(dictionary): add JMdict ingestion
fix(navigation): preserve floating bubble position
refactor(database): normalize vocabulary relationships
docs(architecture): document knowledge graph
```

### 170. Commit Quality

A commit should ideally represent one coherent change. Avoid:

- "fixed stuff"
- "update"
- "changes"
- "AI generated"
- "everything"

Use meaningful commit messages.

### 171. Pull Request Standard

Every meaningful change should be reviewable. A PR should contain:

- summary
- problem
- solution
- architecture impact
- screenshots where relevant
- testing performed
- known limitations
- migration notes
- database changes
- license implications

### 172. AI Coding Agent Standard

AI agents are development assistants. They are NOT autonomous architects.

Every AI agent must: READ, UNDERSTAND, PLAN, IMPLEMENT, VALIDATE, DOCUMENT.

Never: GUESS, PATCH, REPEAT.

### 173. AI Agent Workflow

Every coding agent must follow:

- PHASE 1 — Repository reconnaissance
- PHASE 2 — Relevant documentation reading
- PHASE 3 — Dependency analysis
- PHASE 4 — Architecture plan
- PHASE 5 — Implementation
- PHASE 6 — Static analysis
- PHASE 7 — Targeted tests
- PHASE 8 — Documentation
- PHASE 9 — Git diff inspection
- PHASE 10 — Handoff report

### 174. Agent Handoff

Every agent must leave a report:

- WHAT I CHANGED
- FILES CHANGED
- ARCHITECTURAL CHANGES
- DATABASE CHANGES
- DEPENDENCIES ADDED
- TESTS
- KNOWN ISSUES
- NEXT STEPS

DO NOT leave the next AI guessing what happened.

### 175. Code Ownership

Every major subsystem must have an owner document. Examples:

```
docs/architecture/database.md
docs/architecture/dictionary.md
docs/architecture/media.md
docs/architecture/mining.md
docs/architecture/stats.md
docs/architecture/journey.md
docs/architecture/game-runtime.md
docs/architecture/integrations.md
```

---

## Part II — Architecture, module boundaries, domain-first

### 176. Module Boundaries

Do not create one gigantic module. Separate conceptual responsibilities. Potential
architecture:

```
core
├── domain
├── data
├── database
├── networking
├── configuration
├── logging
└── utilities

language
├── kanji
├── vocabulary
├── grammar
├── sentences
├── kana
├── pitch
└── knowledge

dictionary
├── search
├── lookup
├── indexing
├── rendering
└── sources

study
├── decks
├── cards
├── scheduling
├── reviews
├── exams
└── learning-state

media
├── library
├── playback
├── subtitles
├── mining
├── screenshots
├── metadata
└── playlists

stats
├── events
├── aggregation
├── heatmap
├── analytics
└── reports

journey
├── runtime
├── world
├── entities
├── quests
├── dialogue
├── discovery
├── navigation
└── progression

integrations
├── anki
├── yomitan
├── asbplayer
├── jidoujisho
└── external-services

ui
├── design-system
├── navigation
├── screens
├── components
├── overlays
└── accessibility

platform
├── desktop
├── android
├── ios
└── web

tools
├── database
├── import
├── export
├── migration
├── release
└── development
```

Actual module names must follow the existing repository's technology and architecture.

### 177. Domain-First Architecture

Business logic must not depend unnecessarily on UI. Example: Kanji knowledge calculation
must NOT live inside `KanjiScreen.kt`. It belongs in domain/application logic. The UI
consumes the result.

### 178. UI Separation

Separate: UI, state, business logic, data access.

Do not: button click → directly SQL query → mutate random global state → redraw screen.

Use structured state flow.

---

## Part III — Database, data ingestion, search, performance

### 179. Database Architecture

Use an embedded relational database for the core offline-first application where
appropriate. SQLite is a strong default. Use a proper schema. Do not store the entire
application as one giant JSON file. Use: tables, indexes, foreign keys, constraints,
transactions, migrations. Use JSON only where it actually belongs.

### 180. Database Migrations

Every schema change must be versioned. Example: migration 001, migration 002,
migration 003. Never tell users "delete your database and reinstall" unless absolutely
unavoidable.

### 181. Database Integrity

Use: foreign keys, unique constraints, not-null constraints, checks, transactions,
indexes. Validate imported external data before inserting it.

### 182. Offline-First

Kaiteyo should remain useful without internet. Core functionality — dictionary, Kanji,
vocabulary, decks, reviews, stats, basic media, Journey content already installed — must
work offline where technically possible. Network-dependent features must clearly identify
their dependency.

### 183. External Data Sources

Prefer established open datasets rather than manually recreating language data. Potential
sources include: KanjiVG, KANJIDIC, JMdict, JMdictFurigana, Tanos JLPT data, frequency
datasets, open pitch resources, other appropriately licensed resources.

Before importing: VERIFY LICENSE, VERIFY ATTRIBUTION, VERIFY REDISTRIBUTION RIGHTS,
VERIFY MODIFICATION RIGHTS, VERIFY WHETHER COMMERCIAL USE IS ALLOWED. Never copy a
dataset merely because another application uses it.

### 184. Data Ingestion Pipeline

External datasets must not be manually copied into random source files. Create a
download → verify → parse → normalize → validate → transform → import → index → test
pipeline. Example:

```
raw/
↓
parser
↓
normalized/
↓
validation
↓
database import
↓
generated indexes
```

Keep provenance.

### 185. Dataset Versioning

Store: dataset name, version, download date, license, source URL, checksum,
transformation version. This allows reproducibility.

### 186. Search Engine

Dictionary search must not simply scan every row. Use appropriate indexing. Potential
technologies: SQLite FTS, trigram indexing where appropriate, prefix indexes, normalized
search fields, cached search results, in-memory indexes where justified.

Search must support: Kanji, kana, romaji, English, readings, words, sentences, tags,
JLPT, frequency.

### 187. Search Architecture

Search pipeline:

```
INPUT
↓
normalization
↓
script detection
↓
tokenization
↓
query interpretation
↓
candidate retrieval
↓
ranking
↓
filters
↓
result presentation
```

Do not make every search query a database-wide brute-force scan.

### 188. Performance Rule

Measure performance. Do not say "this should be fast." Measure: startup time, screen
transition time, search latency, database query latency, frame time, memory usage, CPU
usage, GPU usage, media startup, subtitle parsing, card creation, database import, sync
time.

### 189. Profiling Tools

Use platform-appropriate profilers. For JVM/Kotlin: Java Flight Recorder, JDK Mission
Control, Android Studio Profiler, JetBrains profilers where appropriate. For
native/media/game components: platform profilers, GPU profilers, rendering profilers,
memory profilers. Do not optimize blindly.

### 190. Performance Budgets

Define budgets. Examples: startup, UI interaction, search, memory, database, media,
Journey. Each major subsystem gets a budget. If a feature violates a budget: profile
first.

### 191. UI Performance

Avoid: unnecessary recomposition, large object allocations, blocking UI thread,
synchronous network operations, unnecessary image decoding, unbounded lists, re-rendering
entire screens.

Use: lazy lists, pagination, caching, background work, incremental updates, proper state
management where appropriate.

### 192. Database Performance

Use `EXPLAIN QUERY PLAN` when investigating slow queries. Add indexes based on actual
access patterns. Do not create hundreds of indexes blindly.

---

## Part IV — Media, subtitles, mining, Yomitan, browser, Anki

### 193. Media Architecture

Do not rewrite VLC or FFmpeg from scratch. Use mature media infrastructure. Possible
strategy: FFmpeg/libavcodec/libavformat and/or libVLC and/or mpv/libmpv depending on
platform and licensing requirements.

Evaluate: codec support, subtitle support, hardware acceleration, licensing, embedding
complexity, platform support, API stability. Choose based on engineering evidence.

### 194. Media Player Abstraction

Kaiteyo should have a `MediaPlayer` interface rather than every screen directly depending
on VLC/mpv:

```
Kaiteyo Player API
↓
backend adapter
↓
VLC
or
mpv
or
platform player
```

This allows backend replacement.

### 195. Subtitle Engine

Subtitle system must be independent from player backend. Support: SRT, ASS/SSA, WebVTT,
other supported formats.

Subtitle model: track, line, timing, text, segments, styles, speaker, metadata.

### 196. Mining Architecture

Mining must operate on: media, subtitle, timeline, dictionary, screenshot, audio, user
notes, deck.

Pipeline:

```
SUBTITLE
↓
TEXT SEGMENT
↓
TOKEN
↓
DICTIONARY
↓
GLOSS
↓
USER SELECTION
↓
CARD
↓
MEDIA REFERENCE
↓
SCREENSHOT
↓
AUDIO CLIP
↓
DECK
```

### 197. Yomitan Integration

Do not blindly clone Yomitan. Investigate its architecture and licensing. Determine: what
can be reused, what can be integrated, what APIs exist, what dictionary format exists,
what browser assumptions exist. Kaiteyo may implement an internal glossary engine using
compatible dictionary data without becoming dependent on browser extensions.

### 198. Browser Architecture

If Kaiteyo contains an embedded browser: do not assume Chrome extensions automatically
work. Evaluate: Chromium Embedded Framework, WebView2, Android WebView, platform browser
APIs, extension compatibility. Design an abstraction.

### 199. Anki Integration

Anki must remain an integration. Do not make Kaiteyo internally dependent on Anki.
Kaiteyo owns its own: decks, cards, notes, reviews, scheduling. Anki integration
synchronizes or exports where supported.

### 200. AnkiConnect

Implement an AnkiConnect client abstraction. Do not scatter HTTP calls throughout the UI.
Use:

```
AnkiService
↓
AnkiConnect adapter
↓
request/response models
↓
error handling
↓
retry policy
```

Support: add note, create deck, find notes, update note, add tags, suspend, bury, store
media, sync-related operations where supported. Verify actual AnkiConnect capabilities
before implementation.

### 201. Integration Failure

If Anki is unavailable: Kaiteyo must continue functioning. Display "Anki unavailable" —
not: application crash.

---

## Part V — Licensing, security, user data, services, events, statistics

### 202. License Architecture

Maintain a THIRD_PARTY_NOTICES directory. Track: library, version, license, source,
copyright, modifications. Generate notices automatically where possible.

### 203. Dependency Policy

Before adding a dependency: Why is it needed? Can existing code solve it? Is it
maintained? Is it licensed appropriately? Does it support required platforms? Does it
increase application size? Does it introduce security risk? Does it introduce native
binaries? Does it have transitive dependencies? Document important decisions.

### 204. Security

Never: hardcode secrets, store API keys in Git, trust arbitrary imported data, execute
arbitrary downloaded code, load untrusted native libraries, disable TLS verification.
Use secure storage for credentials.

### 205. User Data

Treat study history, knowledge state, media history, notes, decks, photos as user-owned
data. Provide: export, backup, restore, delete where appropriate.

### 206. Backup

Provide a reliable backup system. Backup: database, settings, user-created cards, study
history, Journey progress, collections, photos where selected. Use versioned backup
formats.

### 207. Import / Export

Support appropriate formats. Examples: JSON, CSV, TXT, APKG where legally/technically
appropriate, Kaiteyo backup format. Do not make exports dependent on internal database
structure.

### 208. API Design

If Kaiteyo exposes APIs: version them. Example: `/api/v1/`. Define: authentication,
request format, response format, errors, rate limits, compatibility.

### 209. Internal Service Interfaces

Use stable interfaces between major systems. Examples: `DictionaryService`,
`SearchService`, `DeckService`, `ReviewService`, `MediaService`, `SubtitleService`,
`MiningService`, `StatsService`, `ExamService`, `JourneyService`, `AnkiService`,
`YomitanService`. Do not expose database implementation details.

### 210. Event System

Stats should be event-driven. Examples: StudyStarted, StudyEnded, CardReviewed,
KanjiEncountered, VocabularyEncountered, MediaStarted, SubtitleSelected, CardMined,
ExamStarted, ExamCompleted, QuestStarted, QuestCompleted, LocationDiscovered,
PhotoTaken. Events become the source for analytics.

### 211. Event Data

Each event should contain: eventId, userId, timestamp, eventType, source, payload,
schemaVersion. Do not put arbitrary UI state into analytics events.

### 212. AFK Detection

Do not simply say "30 minutes = AFK." Use activity signals. Possible: keyboard input,
mouse input, touch input, controller input, window focus, media playback, study
interaction, scrolling, drawing activity. Define a configurable inactivity model. Stats
should distinguish: active study, passive media, idle, background.

### 213. Statistics Architecture

Do not calculate everything directly from UI state. Use: raw events → aggregation →
derived metrics → presentation. This allows statistics to evolve without losing
historical data.

### 214. Heatmap

Heatmap data should be generated from actual study events. Each day: date, active study
time, reviews, new cards, Kanji, vocabulary, grammar, media, exams. Intensity should be
derived from meaningful metrics. Not simply "number of clicks."

---

## Part VI — Testing, crash prevention, logging

### 215. Testing Strategy

Testing layers: unit, integration, database, UI, snapshot where useful, performance,
media, import/export, migration, end-to-end. Do not attempt to test everything with UI
tests.

### 216. Unit Tests

Test: domain rules, scheduling, knowledge scoring, search ranking, parsing,
normalization, statistics calculations, quest logic, relationship logic.

### 217. Database Tests

Test: migration, constraints, queries, indexes, imports, exports, corruption recovery,
transaction behavior.

### 218. UI Tests

Test important flows: launch, navigation, search, dictionary lookup, deck creation,
review, media playback, subtitle selection, mining, settings, floating bubble, sidebar,
launchpad.

### 219. Crash Prevention

Every major user action must have an error path. Examples: database unavailable, media
file missing, subtitle invalid, Anki closed, network unavailable, dataset corrupted,
storage unavailable, permission denied. Never crash because an optional integration
failed.

### 220. Logging

Implement structured logging. Levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL. Never log:
passwords, tokens, private credentials.

### 221. Crash Reporting

If crash reporting is implemented: make it opt-in or privacy-conscious as required.
Record: platform, version, stack trace, subsystem, safe diagnostic metadata. Avoid
collecting unnecessary personal information.

---

## Part VII — Feature flags, design system, assets

### 222. Feature Flags

Large unfinished systems may use feature flags. Examples: journey, embedded-browser,
experimental-player, new-search, new-stats. Do not leave permanent feature flags
everywhere. Remove them when the feature stabilizes.

### 223. Experimental Features

Experimental systems belong under `experimental` or equivalent. Do not mix unstable
prototypes with production architecture.

### 224. Design System

Create one design system. Define: typography, spacing, radii, elevation, icons, colors,
motion, controls, cards, navigation, dialogs, menus. Themes must use tokens. Never
hardcode random colors throughout screens.

### 225. Theme System

Theme architecture: Theme → semantic colors → typography → shapes → motion → components.
Possible themes: light, dark, OLED, custom. All components consume semantic tokens.

### 226. Asset System

Create organized asset directories. Example:

```
assets/
├── branding/
├── icons/
├── logos/
├── banners/
├── illustrations/
├── fonts/
├── sounds/
├── music/
├── game/
│   ├── characters/
│   ├── environment/
│   ├── props/
│   ├── textures/
│   └── materials/
└── media/
```

Do not scatter assets throughout source directories.

### 227. Branding Asset Pipeline

Provide a dedicated place for: logo, icon, wordmark, banner, splash, background, default
avatar, store images. If a new branding asset replaces an existing asset: make a copy,
preserve source, update references, validate dimensions, validate formats. Do not
overwrite the only original.

### 228. Icon Standard

Do not generate random SVG icons for every feature. Use one coherent icon system. Icons
must: align, scale, have consistent stroke/fill logic, support themes, support
accessibility labels.

---

## Part VIII — Documentation, ADRs, CI/CD, releases, updates

### 229. Documentation Toolchain

Documentation should use: Markdown, Mermaid, architecture diagrams, tables, decision
records. Where useful: generated API documentation, generated schema documentation,
generated dependency notices.

### 230. ADR System

Use Architecture Decision Records. Example:

```
docs/adr/
001-database.md
002-search-engine.md
003-media-backend.md
004-anki-integration.md
005-journey-runtime.md
006-game-engine.md
```

Each ADR: Context, Decision, Alternatives, Consequences, Status.

### 231. CI/CD

Use CI. Every important push should automatically perform appropriate: formatting,
linting, static analysis, unit tests, database tests, build validation. Do not make every
CI job unnecessarily enormous. Use targeted pipelines.

### 232. Build Pipeline

Separate: PR validation, nightly builds, release builds. Release builds should be
reproducible.

### 233. Release Artifacts

Desktop: installer, portable package where appropriate. Android: APK, AAB. Web:
production bundle. Journey/game assets: versioned packages where architecture requires
it.

### 234. Custom Installer

Installer should eventually support: install location, desktop shortcut, start menu
shortcut, file associations where appropriate, language, optional components, updates,
uninstall, repair. Do not build an installer before application packaging is stable.

### 235. Update System

Plan: version checking, download, verification, installation, rollback. Never assume an
update can safely overwrite everything.

---

## Part IX — Developer CLI and toolchain

### 236. Development CLI

Create a professional developer CLI. Possible commands:

```
kaiteyo dev doctor
kaiteyo dev lint
kaiteyo dev test
kaiteyo dev database
kaiteyo dev import
kaiteyo dev export
kaiteyo dev generate
kaiteyo dev docs
kaiteyo dev assets
kaiteyo dev package
kaiteyo dev release
```

The CLI should provide a consistent interface.

### 237. Git CLI

Create a safe convenience command for: status, stage selection, commit, push. But NEVER
hide Git behavior completely. Show: files, diff summary, branch, remote, commit message,
confirmation.

### 238. Gradle Development CLI

If the project uses Gradle, create a wrapper command that exposes common tasks. Examples:
build, test, lint, clean, desktop, android, dependencies. The wrapper must not invent
tasks. It must discover actual Gradle tasks.

### 239. WSL Tooling

If Windows development requires WSL: document supported workflows. Provide convenience
commands for: WSL status, filesystem access, Git, Linux utilities, scripts, development
environment. Do not make WSL mandatory unless the architecture genuinely needs it.

### 240. Development Doctor

One command should diagnose: Java, Gradle, Git, Android SDK, NDK where needed, Python
where needed, Node where needed, CMake where needed, FFmpeg/libVLC/mpv dependencies,
environment variables, disk space, permissions. Example: `kaiteyo dev doctor`. Output:
PASS, WARN, FAIL with remediation instructions.

### 241. Toolchain Minimalism

Do not install 50 tools because they exist. Every tool must have a purpose. Core: Git,
IDE, JDK/runtime, build system, database tooling, profiler, CI. Additional tools only
when required.

---

## Part X — Journey and game runtime

### 242. Game Engine Decision

DO NOT BUILD A GAME ENGINE FROM SCRATCH. Evaluate established engines. Potential
candidates: Godot, Unity, Unreal. For Kaiteyo's stylized cross-platform Journey, evaluate
Godot especially because of: open-source nature, cross-platform support, scene system,
node system, 2D/3D support, GDScript/C#, rendering, mobile support, desktop support. But
do not automatically adopt it. Perform a technical evaluation.

### 243. Embedding Game Runtime

If Journey uses a separate game runtime, define:

```
Kaiteyo Application
↓
Journey Runtime
↓
World Content
```

Define communication: user identity, knowledge state, progress, settings, events,
discoveries. Do not duplicate the user database unnecessarily.

### 244. Game Engine Data Boundary

The game runtime should not directly manipulate arbitrary application database tables.
Use an API/domain boundary. Example:

```
JourneyService
↓
World Runtime Adapter
```

This prevents the game engine from becoming entangled with the entire application.

### 245. 3D Asset Pipeline

Potential tools: Blender, Substance alternatives where appropriate, Krita, Photoshop
alternatives, Figma, SVG tools. Use Blender for: models, UVs, rigging, animation,
environment assets, LOD generation. Keep source assets separate from exported assets.

### 246. 3D Asset Structure

Example:

```
art/
├── source/
│   ├── blender/
│   ├── textures/
│   ├── concept/
│   └── reference/
├── processed/
└── exported/
```

Never make exported game assets the only source files.

### 247. World Creation

Do not manually model all of Japan immediately. Pipeline: reference → geographic data →
procedural base → artist refinement → gameplay placement → optimization → validation.

### 248. Real-World Data

For accurate locations: use appropriately licensed/open geographic datasets. Do not
scrape Google Maps or Google imagery without verifying permission and licensing.
Evaluate: OpenStreetMap, government geographic datasets, public GIS datasets, licensed
commercial datasets. Store attribution.

### 249. Map Data

Separate geographic truth from artistic representation. The game does not need
centimeter-level physical accuracy everywhere. Prioritize: recognizable landmarks, street
layout, railways, major geography, important cultural locations.

### 250. Artistic Accuracy

The goal is recognizable Japan, not photogrammetric simulation of Japan. Use stylization
deliberately.

### 251. Game Input System

Create an input abstraction. Actions: Move, Look, Interact, Jump where relevant, Run,
Camera, Map, Dictionary, Journal, Quest, Inventory, Pause, Screenshot, Confirm, Back.
Map them separately for: keyboard, mouse, touch, controller.

### 252. Control Remapping

Allow users to remap controls. Store bindings in settings. Support: keyboard, mouse,
gamepad, touch configuration where appropriate.

### 253. Sensitivity

Settings may include: camera sensitivity, mouse sensitivity, controller sensitivity,
stick deadzone, aim/look acceleration, vibration. Do not expose 100 settings to
beginners. Use Simple / Advanced modes.

---

## Part XI — Accessibility, localization, content, plugins, API stability

### 254. Accessibility

Support: reduced motion, text scaling, high contrast, color accessibility, subtitles,
subtitle size, subtitle background, audio controls, controller remapping, keyboard
navigation, screen-reader-compatible application UI where practical.

### 255. Localization

Do not hardcode user-facing strings. Use localization keys. Support: Japanese, English —
and design the system to add Korean, Chinese, etc. later.

### 256. Language Content Localization

Separate application localization from Japanese learning content. Do not confuse
"Japanese UI" with "Japanese being learned."

### 257. Content Authoring

Future creators should be able to create: courses, lessons, quests, stories, dialogue,
dictionary supplements, Journey content, exams — through structured content formats. Do
not require source-code modifications for every lesson.

### 258. Content Format

Choose a structured content format. Potential: JSON, YAML, SQLite, custom package format.
Use schemas. Validate content automatically.

### 259. Content Packages

Future downloadable content should be packaged. Example: Kaiteyo Content Package —
manifest, metadata, license, version, dependencies, nodes, assets, localization.

### 260. Content License

Every content package must identify: creator, license, source, attribution, version. No
anonymous copied content.

### 261. Plugin Architecture

Do not create plugins unless there is a real extension boundary. Potential plugin areas:
dictionary providers, media providers, import/export, theme packs, content packs,
integrations. Plugins must have: manifest, version, permissions, compatibility, API
version.

### 262. Plugin Security

Never allow plugins unlimited access by default. Define capabilities. Example:
READ_DICTIONARY, WRITE_DECK, READ_MEDIA, WRITE_MEDIA, NETWORK, FILESYSTEM. Ask for
permission where appropriate.

### 263. API Stability

Mark APIs: internal, experimental, stable, deprecated. Do not let internal classes
accidentally become public extension APIs.

### 264. Deprecation

When removing an API: document, warn, migrate, remove. Do not silently break
integrations.

---

## Part XII — Observability, networking, sync, state, concurrency, memory

### 265. Observability

Development builds should expose diagnostics. Examples: database status, cache status,
media backend, FPS, memory, network, integration status. These should be hidden or
simplified for normal users.

### 266. Debug Overlay

Developer mode may expose: FPS, frame time, memory, CPU, GPU, database query time,
network, current node, current world cell, loaded assets, active quests. Never expose
this clutter in normal mode.

### 267. Cache Architecture

Define caches explicitly. Examples: dictionary cache, search cache, image cache, media
metadata, subtitle cache, world cell cache, asset cache. Each cache must have: owner,
size limit, eviction, persistence, invalidating conditions.

### 268. Networking

Network operations must be: asynchronous, cancelable, retryable where appropriate,
timeout-aware. Do not block the UI.

### 269. Network Failure

Handle: offline, slow connection, timeout, server error, authentication failure, partial
response — without crashing.

### 270. Synchronization

If Kaiteyo eventually supports cloud sync: define conflict resolution before
implementation. Possible conflict: same card edited on two devices. Do not blindly
overwrite.

### 271. Device Sync

Synchronize data based on semantic objects. Potential: cards, decks, reviews, settings,
knowledge, stats, Journey progress, collections. Do not sync temporary UI state.

### 272. File System

Use platform-safe storage abstractions. Do not hardcode `C:\Users\...` or
`/storage/emulated/...` into domain logic.

### 273. Media File Access

Use platform file picker APIs. Remember: Android permissions, Windows paths, Linux paths,
macOS sandboxing, web limitations.

### 274. Data Ownership

Every major data object must have an owner. Example: Database owns persistent state.
Media library owns media metadata. Player owns playback state. Journey owns world runtime
state. Stats consumes events. Do not have five modules simultaneously own the same state.

### 275. State Management

Avoid global mutable state. Use: immutable state where practical, unidirectional data
flow, events, state holders, repositories/services. The exact framework depends on the
project's technology.

### 276. Concurrency

Never perform expensive operations on the UI thread. Potential background tasks: database
import, dictionary indexing, subtitle parsing, media metadata, image processing, dataset
ingestion, statistics aggregation, backup, sync.

### 277. Job Management

Long-running tasks need: progress, cancellation, error, retry, resume where practical.
Example: "Importing JMdict..." — not a frozen screen.

### 278. Memory Management

Watch for: large dictionaries, images, video thumbnails, subtitle files, 3D assets, world
cells, audio clips. Use: streaming, lazy loading, caching, eviction.

---

## Part XIII — Large datasets, Unicode, fonts, drawing, exams, statistical honesty

### 279. Large Dataset Testing

Test against realistic datasets. Do not test dictionary search against 20 words. Test
against the actual scale expected by production.

### 280. Import Testing

Test: valid data, malformed data, partial data, duplicate data, unexpected encoding,
Unicode, Japanese text, large files.

### 281. Unicode

Japanese text support must be treated as first-class. Test: hiragana, katakana, kanji,
iteration marks, punctuation, full-width characters, half-width characters, emoji,
surrogate pairs, combining marks. Do not assume ASCII.

### 282. Text Normalization

Define normalization policy. Examples: NFC, NFKC where appropriate, full-width/half-width,
kana normalization, whitespace. Do not normalize blindly. Language data may depend on
exact representation.

### 283. Font Architecture

Fonts must support required Japanese glyphs. Evaluate: Japanese font coverage, weights,
rendering, fallback, licensing, file size.

### 284. Drawing Engine

Kanji/Kana writing practice should have a real stroke model. Support: pressure where
available, brush size, stroke timing, eraser, undo, redo, stroke playback, stroke order
comparison. Do not store drawings only as screenshots. Store stroke data.

### 285. Stroke Data

Represent: stroke, points, timestamp, pressure, velocity where useful. This enables:
analysis, replay, comparison, statistics.

### 286. Kana

Kana must be treated as first-class learning data. Include: hiragana, katakana, dakuten,
handakuten, small kana, extended katakana, foreign sound combinations. Writing practice
should use the same drawing infrastructure.

### 287. Exam Engine

Exam system should be modular. Question types: multiple choice, typing, reading,
listening, writing, dictation, sentence completion, translation, Kanji recognition, Kanji
writing, vocabulary, grammar, pitch, comprehension.

### 288. Exam Generation

Questions should derive from: user knowledge, study history, JLPT level, course, deck,
media exposure, Journey content. Do not generate random questions disconnected from what
the user learned.

### 289. Exam Security

Exam answers must be stored with: question version, answer, timestamp, exam version. If
exam content changes later, historical results remain interpretable.

### 290. Statistical Correctness

Stats must never fabricate precision. If the system cannot accurately determine "JLPT
level": say estimated / approximate / based on studied content. Do not present an
unofficial estimate as an official JLPT result.

---

## Part XIV — External services, privacy, telemetry, error UX, states

### 291. JPDB / Anime / External Knowledge

External databases may enrich Kaiteyo. But: external data ≠ official user mastery. Keep
sources distinct.

### 292. External Service Abstraction

External services must be adapters. Example: `AnimeMetadataProvider` could have
`AniListAdapter` and other providers. The UI should depend on `AnimeMetadataProvider`,
not directly on AniList HTTP calls.

### 293. Rate Limits

Respect external APIs. Implement: caching, rate limiting, backoff, timeouts. Never spam
external services.

### 294. Privacy

Default to local data. Do not send study history, media history, knowledge state to
external services unless necessary and clearly disclosed.

### 295. Telemetry

Do not secretly collect usage data. If telemetry exists: document it, minimize it, make
privacy choices clear, avoid sensitive learning content.

### 296. Error UX

Error messages must explain: what happened, why, what the user can do.

Bad: "Error 500."

Better: "Kaiteyo couldn't connect to Anki. Anki appears to be closed. Open Anki and try
again."

### 297. Empty States

Every screen must have intentional empty states. Examples: No decks, No media, No
discoveries, No exams, No Journey progress, No search results. Never: blank white
rectangle.

### 298. Loading States

Use: skeletons, progress, spinners, progressive rendering where appropriate. Never freeze
the application silently.

### 299. Offline States

Clearly indicate when: content unavailable, sync pending, external service unavailable.
Do not destroy functionality unnecessarily.

---

## Part XV — QA, device classes, responsive modes, window management, motion

### 300. Accessibility QA

Test: keyboard-only, touch, mouse, controller, large text, reduced motion, dark/light,
high contrast.

### 301. Cross-Platform QA

Minimum target matrix should be documented. Example: Windows desktop, Android, Web. Other
platforms only if actually supported. Do not claim support merely because a framework
compiles.

### 302. Device Classes

Test: small phone, large phone, tablet, small laptop, desktop, ultrawide monitor. UI must
remain usable.

### 303. Responsive Breakpoints

Do not hardcode one layout. Define responsive modes: COMPACT, STANDARD, WIDE, ULTRAWIDE.
Each screen decides how information reflows.

### 304. Window Management

Desktop application must correctly support: maximize, minimize, restore, resize,
fullscreen, window movement, DPI scaling, multi-monitor, taskbar/dock interaction. Window
controls must not disappear behind the operating system taskbar.

### 305. Title Bar

Custom title bar only if technically justified. If custom: dragging, double click, system
menu, minimize, maximize, close must behave correctly. Do not create a fake title bar
that breaks Windows behavior.

### 306. Rendering Performance

Animations must maintain target frame rate. Avoid: continuous unnecessary animations,
expensive blur everywhere, huge shadows, unbounded particles, repeated recomposition,
large translucent layers.

### 307. Motion System

Centralize motion definitions. Examples: micro, short, standard, large. Settings:
animation speed, reduced motion. Do not individually invent durations for every screen.

### 308. Visual Regression

Important screens should have visual regression tests or snapshots where practical.
Especially: Home, Browse, Library, Stats, Dictionary, Media, Settings, Launchpad,
Sidebar, Journey HUD.

### 309. Design QA

Every screen should be reviewed for: alignment, spacing, typography, contrast, hierarchy,
responsive behavior, interaction, animation, empty state, loading state, error state.

---

## Part XVI — Code style, naming, health, priorities, issues

### 310. Code Style

Use automatic formatting. Do not spend human time debating whitespace. Use: formatter,
linter, static analyzer as part of development.

### 311. Naming

Names must describe purpose.

Bad: `DataManager2`, `UtilsFinal`, `NewScreen`, `TempRepository`.

Good: `DictionaryRepository`, `SubtitleParser`, `KnowledgeGraphService`,
`ReviewScheduler`, `JourneyProgressRepository`.

### 312. Utility Classes

Do not create gigantic `Utils` classes. Split utilities by responsibility.

### 313. God Classes

Reject classes that know everything. Examples to avoid: `KaiteyoManager`,
`AppManager`, `MainController`, `EverythingService`. If a class becomes enormous:
identify responsibilities, split them.

### 314. File Size

No arbitrary line-count requirement. However, extremely large files should trigger
architectural review. If a file contains UI, database, business logic, networking and
animation all together: refactor.

### 315. Duplication

Do not copy/paste implementations. If three systems need the same behavior: extract an
appropriate abstraction. But do not prematurely abstract tiny code.

### 316. Clean Architecture Principle

Dependency direction should generally point toward stable domain concepts:

```
UI
↓
Application
↓
Domain
```

Infrastructure implements interfaces required by domain/application. Do not let domain
logic depend on a specific UI toolkit, a specific database driver, or a specific media
backend unless unavoidable.

### 317. Document Why

Comments should explain why, not what.

Bad: `// increment i`

Good: `// Keep the index stable while the filtered list changes so keyboard navigation
// does not jump unexpectedly.`

### 318. TODO Standard

No `// TODO fix later`. Use `TODO(owner/subsystem):` — specific problem, expected
behavior, dependency, priority.

### 319. Priority System

- P0 = catastrophic (crash on startup)
- P1 = critical (database corruption)
- P2 = important (missing dictionary filter)
- P3 = normal (animation polish)
- P4 = nice-to-have (decorative effect)

### 320. Issue Tracking

Every major issue should become an issue. Issue contains: problem, expected behavior,
actual behavior, reproduction, platform, screenshots/logs, priority, affected subsystem.

---

## Part XVII — Roadmap, vertical slices, definition of done, no fakes

### 321. Roadmap

Separate: NOW, NEXT, LATER, RESEARCH. Do not mix speculative ideas with active
implementation tasks.

### 322. Vertical Slice Development

Do not build 100% dictionary, then 100% media, then 100% Journey. Instead build vertical
slices. Example — Dictionary slice: search → result → Kanji page → vocabulary → user
knowledge → card → stats. Then expand.

### 323. Feature Completeness

A feature is not complete when "button exists." It is complete when UI, state, domain
logic, database, error handling, empty state, loading state, accessibility, performance,
persistence, tests and documentation are addressed appropriately.

### 324. Definition of Done

Every feature should answer: Does it work? Does it persist? Does it survive relaunch?
Does it work offline where expected? Does it fail safely? Does it look correct? Does it
animate correctly? Does it work on supported platforms? Does it have tests? Is it
documented?

### 325. No Fake Implementation

Never create: buttons that do nothing, fake loading screens, placeholder statistics
pretending to be real, hardcoded database results, fake search, fake synchronization,
fake player controls, fake Journey progress. If something is not implemented: show a
clear development state.

### 326. No Hardcoded Product Data

Do not hardcode Kanji lists, vocabulary, statistics, deck contents, media metadata,
Journey progression inside UI source files. Use data models.

### 327. No Magic Numbers

Avoid random `padding = 17`, `radius = 13`, `animation = 237ms` unless deliberately
defined as a design token.

### 328. No Random Design Generation

AI must not invent a new design language every time it edits a screen. Read the design
system first. Existing design tokens are authoritative.

### 329. No AI Vibe-Coding

Reject: giant generated components, duplicate components, placeholder logic, random
dependencies, fake APIs, unnecessary abstractions, random animations, hardcoded data,
screens that only look good in screenshots. Code must correspond to actual architecture.

### 330. Repository Cleanliness

Repository root should remain clean. Only essential root files. Documentation belongs in
`docs/`, development tools in `tools/`, scripts in `scripts/`, assets in `assets/`, tests
in `tests/`. Do not dump screenshots, temporary files, AI outputs, or random text files
into the repository root.

---

## Part XVIII — Documentation structure, changelog, versioning, releases

### 331. Documentation Structure

Suggested:

```
docs/
├── README.md
├── architecture/
├── database/
├── language/
├── dictionary/
├── study/
├── media/
├── mining/
├── statistics/
├── exams/
├── journey/
├── game/
├── integrations/
├── ui/
├── design/
├── development/
├── testing/
├── release/
├── licensing/
├── security/
├── adr/
└── contributing/
```

### 332. Development Documentation

Must include: getting started, architecture, toolchain, development commands, database,
testing, debugging, profiling, release, contributing, AI-agent workflow.

### 333. Contributor Experience

A new developer should be able to: clone, install prerequisites, run setup, open project,
run application, run tests, understand architecture, make a small change, run validation,
create a commit — without asking the original developer 50 questions.

### 334. AI Contributor Experience

A future AI should be able to read README, architecture, relevant subsystem docs, TODO,
ADR, and code — and understand: where to modify, what not to modify, what dependencies
exist, what contracts must remain stable.

### 335. Automated Documentation Index

Create an index linking: all architecture documents, all ADRs, all subsystem documents,
all development guides. No orphan documents.

### 336. Documentation Freshness

When architecture changes: update documentation. Do not allow code says A,
documentation says B.

### 337. Changelog

Maintain `CHANGELOG.md` with meaningful release notes. Categorize: Added, Changed, Fixed,
Removed, Security, Performance.

### 338. Versioning

Use a documented versioning strategy. Application version, database schema version,
content version, API version, Journey world version must be distinguishable.

### 339. Release Channels

Possible: development, nightly, beta, stable. Only if the project actually needs them.

### 340. Release Checklist

Before release: database migration, backup, tests, lint, packaging, licenses, assets,
localization, performance, crash checks, installer, release notes.

---

## Part XIX — Build discipline, validation ladder, source of truth, reviews

### 341. Build Rule

DO NOT repeatedly compile the project during every documentation or planning pass.
Compilation is expensive.

- When an AI is asked to perform an architecture/documentation task: DO NOT BUILD.
- When an AI is asked to perform a pure code-edit task: DO NOT AUTOMATICALLY BUILD
  unless necessary.
- When implementation is complete: run the smallest appropriate validation first.
- Only perform full builds when justified.

### 342. Validation Ladder

Use the cheapest useful validation first:

- LEVEL 1 — format/lint
- LEVEL 2 — static analysis
- LEVEL 3 — targeted unit test
- LEVEL 4 — targeted integration test
- LEVEL 5 — module build
- LEVEL 6 — full build
- LEVEL 7 — full end-to-end validation

Do not jump directly to LEVEL 6 for every tiny change.

### 343. Development Command Documentation

Create a canonical command reference. Example categories: Setup, Development, Testing,
Database, Import, Export, Formatting, Lint, Profiling, Packaging, Release, Git, WSL. The
actual commands must be generated from the project's real toolchain. Do not invent
commands that do not exist.

### 344. Reproducible Development

A second developer should receive approximately the same dependencies, toolchain,
generated files, database schema, and build output from the same repository revision.

### 345. Environment Files

Document environment variables. Provide `.env.example` where appropriate. Never commit
secrets.

### 346. Generated Files

Clearly distinguish: source, generated, cached, build output. Generated files should not
accidentally become authoritative source.

### 347. Source of Truth

For every important object define SOURCE OF TRUTH. Example:

- Kanji metadata: database dataset
- Theme: design tokens
- Study history: event/database layer
- UI state: application state
- Journey world: world content package

Do not allow multiple competing sources.

### 348. Migration Strategy

When replacing old architecture:

```
OLD
↓
adapter
↓
new architecture
↓
migration
↓
remove adapter
```

Do not break everything simultaneously.

### 349. Legacy Systems

Document legacy code. Mark: LEGACY. Do not casually delete working behavior.

### 350. Technical Debt

Maintain `docs/technical-debt.md`. Each item: problem, impact, risk, estimated effort,
priority.

### 351. Architectural Health

Periodically inspect: dependency graph, module boundaries, large files, cyclic
dependencies, unused dependencies, dead code, duplicate logic, database complexity, UI
complexity.

### 352. Dead Code

Remove obsolete code after confirming: no references, no public API, no migration
dependency, no external integration. Do not accumulate old implementations indefinitely.

### 353. Developer Experience

A professional project should make common operations easy. One command should eventually
be enough for: environment diagnosis, database preparation, formatting, tests,
documentation generation, packaging.

### 354. Development Dashboard

Optional developer dashboard may show: build state, database state, test state,
dependency state, documentation state, feature flags, performance.

### 355. Code Review Checklist

Review: architecture, correctness, security, performance, UX, accessibility, tests,
database, platform compatibility, documentation, license.

### 356. Security Review Checklist

Review: network, file access, imports, plugins, embedded browser, external APIs,
credentials, downloads, content packages, native libraries.

---

## Part XX — Security of untrusted input

### 357. Media Security

Media files are untrusted input. Handle: malformed containers, malicious subtitles,
unexpected codecs, huge metadata, corrupt files. Do not assume media files are safe.

### 358. Import Security

Imported decks/content/datasets are untrusted. Validate before processing.

### 359. Plugin Security

Treat plugins as potentially untrusted. Permissions must be explicit.

### 360. Embedded Browser Security

If using Chromium/WebView: sandbox where possible, restrict privileged APIs, avoid
exposing filesystem unnecessarily, validate navigation, control downloads, separate
browser permissions from application permissions.

### 361. Game Security

Journey content packages must not execute arbitrary application code. Prefer
data-driven content over downloaded scripts.

---

## Part XXI — Final principles, technology selection, implementation order, stop condition

### 362. Final Professional Engineering Principle

Kaiteyo must be built like a product that could be maintained for ten years. That means:
DO NOT chase line count, feature count, screenshots, or AI output volume.

Build: stable foundations, clear contracts, real data, real tests, real integrations,
real UX, real performance, real documentation.

Every subsystem must have OWNER, DATA MODEL, API, UI, ERROR MODEL, TEST STRATEGY,
PERFORMANCE STRATEGY, DOCUMENTATION where applicable.

### 363. Final Tool Decision Principle

Use the best existing professional tool when it solves the problem. Examples: Git,
GitHub, JetBrains IDE, VS Code, SQLite, Blender, Krita, Figma, Mermaid, FFmpeg, libVLC,
mpv/libmpv, Godot if selected for Journey, Android Studio, platform profilers, CI/CD. Do
not replace mature infrastructure with thousands of lines of custom code merely to claim
ownership.

Kaiteyo's custom engineering effort should concentrate on the things that make Kaiteyo
unique: knowledge graph, learning model, dictionary UX, study system, mining workflow,
media-learning integration, Journey, world/knowledge connection, statistics, exams,
cross-platform UX, integrations.

### 364. Final Technology Selection Process

Before adopting any major technology:

1. Identify requirement.
2. List candidates.
3. Evaluate platform support.
4. Evaluate license.
5. Evaluate maintenance.
6. Evaluate performance.
7. Evaluate integration complexity.
8. Evaluate binary/application size.
9. Evaluate security.
10. Prototype.
11. Benchmark.
12. Document decision.

Do not select technologies because "AI knows it."

### 365. Build the Foundation First

Recommended implementation dependency order:

- PHASE 0 — Repository stabilization
- PHASE 1 — Toolchain
- PHASE 2 — Design system
- PHASE 3 — Core domain
- PHASE 4 — Database
- PHASE 5 — Data ingestion
- PHASE 6 — Knowledge graph
- PHASE 7 — Dictionary/search
- PHASE 8 — Kanji/Kana/Vocabulary
- PHASE 9 — User knowledge
- PHASE 10 — Library/decks/cards
- PHASE 11 — Review scheduler
- PHASE 12 — Statistics/events
- PHASE 13 — Exams
- PHASE 14 — Media abstraction
- PHASE 15 — Subtitle engine
- PHASE 16 — Mining
- PHASE 17 — Anki/Yomitan integrations
- PHASE 18 — Home/Browse/Library/Stats UX
- PHASE 19 — Navigation/floating/launchpad
- PHASE 20 — Embedded browser/media workflows
- PHASE 21 — Journey data model
- PHASE 22 — Journey runtime prototype
- PHASE 23 — Kamakura vertical slice
- PHASE 24 — World expansion
- PHASE 25 — Children mode
- PHASE 26 — Advanced content authoring
- PHASE 27 — Cloud/sync
- PHASE 28 — Release engineering

This is a dependency graph, not a rigid calendar. Reorder if the actual repository
proves a different dependency.

### 366. Do Not Start With the Entire World

The first Journey implementation must NOT attempt: entire Japan, all trains, all
vehicles, all cities, all buildings, all NPCs, all seasons, all activities. Instead build
ONE POLISHED LOCATION.

The vertical slice must prove: movement, camera, interaction, dictionary, language node,
NPC, dialogue, quest, discovery, photography, collection, knowledge, stats, save/load,
performance. Once this works: expand.

### 367. Do Not Build Custom Engine Systems Prematurely

Use the selected engine's rendering, physics, scene system, animation, audio, input,
navigation, and asset pipeline unless Kaiteyo genuinely requires custom behavior. Custom
code belongs around the engine, not in competition with it.

### 368. Architecture Before Scale

- Before adding 1,000 locations: prove one location.
- Before adding 100,000 vocabulary entries: prove ingestion/indexing.
- Before adding millions of events: prove event aggregation.
- Before adding thousands of quests: prove the quest schema.
- Before adding hundreds of screens: prove the design system.

### 369. Scale Testing

Test the architecture at anticipated scale. Examples: full dictionary dataset, large
deck, large review history, large media library, large subtitle corpus, large Journey
save, large collection, many quests. Do not discover scalability problems after
production.

### 370. Final Agent Contract

Every future coding AI must obey:

- READ THE DOCUMENTATION FIRST.
- INSPECT THE ACTUAL CODE SECOND.
- MAKE A PLAN THIRD.
- CHANGE THE SMALLEST CORRECT ARCHITECTURAL SURFACE.
- DO NOT DESTROY EXISTING WORK.
- DO NOT INVENT APIs.
- DO NOT INVENT DATABASE SCHEMAS WITHOUT DOCUMENTING THEM.
- DO NOT ADD RANDOM DEPENDENCIES.
- DO NOT BUILD UNNECESSARILY.
- DO NOT COMPILE UNNECESSARILY.
- DO NOT CLAIM IMPLEMENTATION WITHOUT IMPLEMENTING IT.
- DO NOT CREATE PLACEHOLDER BUTTONS AS FINAL FEATURES.
- DO NOT FAKE DATA.
- DO NOT FAKE STATISTICS.
- DO NOT FAKE INTEGRATIONS.
- DO NOT LEAVE BROKEN NAVIGATION.
- DO NOT LEAVE CRASHES.
- DO NOT LEAVE UNDOCUMENTED ARCHITECTURAL CHANGES.
- TEST WHAT YOU CHANGE.
- DOCUMENT WHAT YOU CHANGE.
- REPORT WHAT YOU COULD NOT FINISH.

### 371. Final Kaiteyo Engineering Mantra

- USE EXISTING TECHNOLOGY FOR INFRASTRUCTURE.
- BUILD CUSTOM TECHNOLOGY FOR KAITEYO'S UNIQUE VALUE.
- DESIGN THE DATA MODEL BEFORE THE UI.
- DESIGN THE DOMAIN BEFORE THE SCREEN.
- DESIGN THE API BEFORE THE INTEGRATION.
- MEASURE BEFORE OPTIMIZING.
- TEST BEFORE CLAIMING DONE.
- DOCUMENT BEFORE HANDING OFF.
- BUILD SMALL VERTICAL SLICES.
- KEEP THE SYSTEM MODULAR.
- KEEP THE DATA OWNED AND TRACEABLE.
- KEEP THE USER EXPERIENCE SIMPLE DESPITE INTERNAL COMPLEXITY.

### 372. Final Definition of Professional

Professional does NOT mean more code. Professional means:

- another developer can understand it.
- another AI can safely modify it.
- the database survives migration.
- the application survives errors.
- the user can recover from failures.
- the UI survives resizing.
- the media player survives bad files.
- the dictionary survives malformed data.
- the integration survives unavailable services.
- the game survives large worlds.
- the statistics survive years of history.
- the project survives its original developer.

And most importantly: Kaiteyo remains understandable even after becoming enormous.

### 373. Final Handoff State

At the completion of this documentation phase, the repository must contain:

ARCHITECTURE, DESIGN SYSTEM, DATABASE SPECIFICATION, KNOWLEDGE GRAPH, LANGUAGE MODEL,
DICTIONARY SPECIFICATION, STUDY ENGINE, CARD MODEL, STATISTICS MODEL, EXAM MODEL, MEDIA
ARCHITECTURE, SUBTITLE ARCHITECTURE, MINING ARCHITECTURE, ANKI INTEGRATION, YOMITAN
INTEGRATION, BROWSER ARCHITECTURE, JOURNEY ARCHITECTURE, WORLD ARCHITECTURE, GAME RUNTIME
PLAN, NODE SYSTEM, CONTENT SYSTEM, PLUGIN SYSTEM, ASSET SYSTEM, TOOLCHAIN, TEST STRATEGY,
PERFORMANCE STRATEGY, SECURITY STRATEGY, LICENSE STRATEGY, CI/CD STRATEGY, RELEASE
STRATEGY, DEVELOPER CLI PLAN, DATABASE MIGRATION PLAN, BACKUP PLAN, SYNC PLAN,
ACCESSIBILITY PLAN, LOCALIZATION PLAN, TODO, ROADMAP, ADR INDEX, CHANGELOG.

### 374. Absolute Documentation Rule

If an important architectural decision exists only inside an AI conversation, it does not
exist as project knowledge. Move it into the repository. The repository must become the
permanent memory of Kaiteyo.

### 375. Absolute Engineering Rule

Do not confuse ambition with implementation. Kaiteyo can be enormous. That is acceptable.
But every enormous subsystem must be decomposed into: architecture, interfaces, data,
implementation, tests, documentation, milestones.

### 376. Final Stop Condition

After completing this documentation and planning task:

DO NOT: run Gradle, compile the entire application, generate massive placeholder code,
rewrite unrelated files, create fake implementations, invent missing dependencies, claim
completion of unimplemented systems.

Instead provide:

1. repository audit
2. architecture map
3. technology decisions
4. dependency map
5. implementation order
6. risks
7. unresolved questions
8. first implementation milestone
9. exact files/modules future agents should begin with

Then stop. The next agent is responsible for implementation. The architecture is the
contract. The code must obey the contract.

---

*This document is the binding engineering contract for Kaiteyo (ADR-0012). The §376
deliverables are maintained in `docs/planning/ENGINEERING_AUDIT.md`; the detailed
real/duplicated/dead/fake audit lives in `docs/planning/PRODUCT_AUDIT.md`.*
