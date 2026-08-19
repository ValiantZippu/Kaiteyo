# Kaiteyo — Per-Destination UI/UX Audit

> **What this is**: the complete per-screen audit matrix required by the UI/UX overhaul
> directive (spec §34, §157) and tracked under `KT-UI-001` in
> [`OVERHAUL_BACKLOG.md`](OVERHAUL_BACKLOG.md). Every discoverable UI destination is
> recorded with its purpose, current state, and status. This is the *source-level* audit —
> it is grounded in the actual screen modules and routes present in
> `core/src/commonMain/kotlin/ua/syt0r/kanji/presentation/screen/main/` and
> `.../features/`, **not** a claim that each surface has been runtime-verified.
>
> Status legend (consistent with [`README.md`](README.md)):
> ✅ `IMPLEMENTED` (code + UI + state exist) · 🚧 `IN PROGRESS` (known gaps) ·
> 🔬 `TARGET` (architected, runtime pending) · 📋 `PLANNED` · 💀 `PLACEHOLDER` (scaffold).
> A `✅` here means the destination exists and is wired into navigation — it does **not**
> claim visual QA passed (see [`VISUAL_QA_CHECKLIST.md`](VISUAL_QA_CHECKLIST.md)) or that
> every control is ghost-free (see `PRODUCT_AUDIT.md`).

---

## 1. Navigation shell & app chrome

| Page | Destination / Route | Purpose | Status | Notes |
|---|---|---|---|---|
| App shell | `KaiteyoApp` / `NavShell` | Window + topbar + navigation host + content | ✅ | One `NavShell` drives Floating + Sidebar (one `NavigationController`) |
| Sidebar | `NavShell` sidebar presentation | App navigation | ✅ | Explicit width (≈20%, clamped 208–384 dp); collapse/expand; hover/focus |
| Floating bubble | `NavShell` floating presentation | App navigation | ✅ | Drag/snap/hold disambiguation; right-click menu; `LauncherSnapMath` tested |
| Launchpad | `NavShell` launchpad | Centered entry point | ✅ | Glass panel, internal scroll on short windows |
| Topbar / chrome | `KaiteyoWindow` (desktop) | Themed chrome, drag, window controls | 🚧 | Open item KT-NAV-007 (polish, resize flashes) |
| Home | `MainDestination.Home` | Dashboard / continuation point | ✅ | Command center; **no separate "Resume" destination** |

## 2. Library family

| Page | Destination / Route | Purpose | Status | Notes |
|---|---|---|---|---|
| Library hub | `screen/library` | Collections + decks + study entry | ✅ | Collections strip; deck catalog scoped by collection |
| Deck browser | `DeckBrowserRoute` | Browse decks | ✅ | |
| Card browser | `CardBrowserRoute` | Browse cards in a deck | ✅ | |
| Deck details | `MainDestination.DeckDetails` | Deck configuration & content | ✅ | Per-type configurations (Letter/…/Vocab) |
| Deck editor | `MainDestination.DeckEdit` | Create/edit a deck | ✅ | |
| Deck picker | `MainDestination.DeckPicker` | Choose a deck for an action | ✅ | |
| Collection detail | `screen/collection_detail` | Collection content + owned decks | ✅ | `CollectionDef.deckIds` + `SmartCollectionEngine` |
| Exams | `ExamWorkspace` (feature) | Exam practice | ✅ | Lives in Library scope, not top-level nav |
| Import/Export | `ImportExportRoute` | Import/export user data | ✅ | |
| Backup | `BackupRoute` / `BackupScreen` | Backup/restore | ✅ | Dead `BackupSystemExt` path removed (see PRODUCT_AUDIT) |
| Migration | `MigrationDialog` (feature) | Data migration prompts | ✅ | |

## 3. Dictionary family

| Page | Destination / Route | Purpose | Status | Notes |
|---|---|---|---|---|
| Browse hub | `screen/browse_hub` | Explore Japanese (kanji/words/…/collections) | ✅ | |
| Kanji browser | `screen/kanji_browser` | Kanji search grid/list | ✅ | Filters + sorts |
| Kanji entry | `screen/kanji_entry` | Modular kanji page | ✅ | Card registry (`KanjiCardType`/layout/presets), level-adaptive |
| Word entry | vocabulary surfaces | Word page (meaning/readings/kanji/examples) | ✅ | Modular word cards (`WordCardModels`) |
| Sentence entry | `screen/sentence_entry` | Sentence page + interactive tokens | ✅ | `SentenceCardModels`; grammar highlighting |
| Sentence | `screen/sentence` | Sentence exploration/list | ✅ | |
| Grammar | `GrammarCardModels` + `GrammarCatalog` | Grammar pattern entries | ✅ | Particles + ~25 patterns; honest non-corpus labeling |
| Radical explorer | `screen/radical_explorer` | Radical grid + filters + result preview | ✅ | `RadicalExplorerTest` |
| Component explorer | `screen/component_explorer` | "Why does this look like this?" | ✅ | |
| Collection detail | `screen/collection_detail` | JLPT/grade list pages | ✅ | |
| Universal search | `UniversalSearch` (feature) | Grouped KANJI/WORDS/SENTENCES/GRAMMAR | ✅ | Wildcard/normalization/filters/sorts; `SearchPipeline`/`TrigramIndex` |


## 4. Knowledge & graph family

| Page | Destination / Route | Purpose | Status | Notes |
|---|---|---|---|---|
| Knowledge explorer | `screen/knowledge_explorer` | Connected exploration | ✅ | |
| Knowledge graph | `screen/knowledge_graph` | Interactive graph navigation | ✅ | Progressive expansion, relationship filters, legend; `KnowledgeGraphTest`/`GraphLayoutTest`/`GraphTrailTest` |
| Node layer | `core/knowledge/nodes` | Typed node/relationship registry | ✅ | `NodeRegistryTest`, `MediaNodeBridgeTest` |
| Learner profile | `screen/learner_profile` | Level profile (beginner→research/custom) | ✅ | `LearnerProfile` + `PresetAdapter` + `LevelAdapterTest` |

## 5. Study / practice family

| Page | Destination / Route | Purpose | Status | Notes |
|---|---|---|---|---|
| Letter practice | `MainDestination.LetterPractice` | Writing/reading practice | ✅ | |
| Vocab practice | `MainDestination.VocabPractice` | Vocabulary review | ✅ | |
| Practice common | `screen/practice_common` | Shared practice UI | ✅ | |
| Vocab card | `screen/vocab_card` | Card face/back surfaces | ✅ | |
| Study resume | `StudyResume` (feature) | Continue from Home | ✅ | Not a top-level destination |
| Text analysis | `screen/text_analysis` | Parse text into tokens/words | ✅ | `SentenceTokenizer`/`WordSegmenter` |
| Stats | `screen/statistics` + `screen/stats` + `StatisticsController` | Study statistics | ✅ | Heatmap (year nav, hover), per-deck/type, knowledge snapshot |

## 6. Content & settings family

| Page | Destination / Route | Purpose | Status | Notes |
|---|---|---|---|---|
| Settings | `screen/settings` | Settings Center (categories) | ✅ | Nav settings wired to real state |
| About | `MainDestination.About` | App info | ✅ | |
| Credits | `MainDestination.Credits` | Attribution (AboutLibraries) | ✅ | |
| Info | `screen/info` | Info data | ✅ | |
| Account | `screen/account` | Account/sync | ✅ | |
| Sync | `screen/sync` / `SyncDialog` | Synchronization | ✅ | |
| Daily limit | `screen/daily_limit` | Study limit config | ✅ | |
| Feedback | `screen/feedback` | User feedback | ✅ | |
| Backup | `screen/backup` | Backup settings | ✅ | |

## 7. Media & world family

| Page | Destination / Route | Purpose | Status | Notes |
|---|---|---|---|---|
| Media Centre | `MainDestination.Media` → `DesktopMediaCentreContent` | Media library + player + learning integration | ✅ | Core destination; crash root-cause fixed; lifecycle tests (`MediaEngineLifecycleTest`) |
| Media node family | `MediaNodeFamily` | Series/Episode/Scene/SubtitleLine nodes | 🔬 | TARGET (see MASTER TODO §118) |
| World / game | `screen/world` + `screen/game` | World engine + game slice | 🚧 | Renderer-agnostic `RenderBackend`; **currently 2.5D Compose Canvas**; true 3D engine is PLANNED (see `docs/game/`) |

## 8. Feature controllers & overlays

| Controller | Purpose | Status |
|---|---|---|
| `CommandPalette` | Ctrl+K global palette / settings search | ✅ |
| `DeepLinkHandler` | `kaiteyo://` deep links | ✅ |
| `DeckFeaturesController` / `DeckFeatureScreens` | Deck feature route registration | ✅ |
| `KaiteyoDataCenter` | Data source + provenance surfacing | ✅ |
| `StatisticsController` | Stats route registration | ✅ |
| `UniversalSearch` | Grouped universal search | ✅ |
| `StudyResume` | Continue-from-home | ✅ |
| `SyncDialog` / `MigrationDialog` | Sync + migration dialogs | ✅ |
| `ExamWorkspace` | Exam surfaces | ✅ |

---

## Cross-cutting audit notes (source-level, not runtime-verified)

1. **Navigation is consolidated** — Floating and Sidebar are two presentations of one
   `NavShell`/`NavigationController`; there is no independent duplicate nav system.
2. **Themes are real** — `Theme.kt`/`Color.kt` (Light/Dark/OLED/Sepia) + token scales
   (`Dimens.kt`, `Typography.kt`, `AnimationTokens.kt`, `ThemeSettingsState.kt`).
3. **Cards are modular** — `CardRegistry` + per-entity card models/layouts/presets +
   `PresetAdapter`; page layout is data, not hardcoded per screen.
4. **Study is integrated, not a stray destination** — Home is the continuation point;
   Library is the study/material hub.
5. **Remaining open work** is concentrated in: runtime sweeps (nav mode-switch stress,
   per-OS feel, topbar chrome, resize polish), design-token consolidation &
   hardcoded-color sweep, IP/gated research (AI service layer, voice/handwriting), and the
   true-3D world engine. See `MASTER_IMPLEMENTATION_TODO.md` and `CURRENT_ISSUES.md`.

## Related

- [`PRODUCT_AUDIT.md`](PRODUCT_AUDIT.md) — product/feature gap analysis
- [`ENGINEERING_AUDIT.md`](ENGINEERING_AUDIT.md) — engineering/code-quality audit
- [`VISUAL_QA_CHECKLIST.md`](VISUAL_QA_CHECKLIST.md) — per-theme visual QA checklist
- [`MASTER_IMPLEMENTATION_TODO.md`](MASTER_IMPLEMENTATION_TODO.md) — 150-item ops list
- [`OVERHAUL_BACKLOG.md`](OVERHAUL_BACKLOG.md) — spec-derived backlog (KT-* IDs)
