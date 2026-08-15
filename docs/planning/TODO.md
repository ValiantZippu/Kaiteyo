# Kaiteyo (書いてよ) — Master TODO

> **Status taxonomy** (see [`README.md`](README.md)): `TODO` (task with a clear definition
> of done) · `FEATURE` (specified new capability) · `RESEARCH` (needs investigation) ·
> `TECHNICAL DEBT` (deliberate paydown) · `BLOCKED` (waiting on something external) ·
> `DONE` (see `COMPLETED.md` / `CHANGELOG.md`).
>
> **Priority legend** — 🔴 P0 critical · 🟡 P1 high · 🟢 P2 medium · 🔵 P3 low.
>
> **Full inventory**: the hierarchical catalog with every task (KT-* IDs, status,
> priority, dependencies, acceptance criteria) is [`MASTER_TODO.md`](MASTER_TODO.md)
> (work packages P0–P39). This file is the operational short-list; MASTER_TODO is
> authoritative for what exists as a planned task.

---

## TODO — open tasks (priority-ordered)

### 🔴 P0 — Desktop polish (KNOWN ISSUE track, fix before new features)

- [ ] Animation stutter — hover animations, theme switching, and window movement are not
      smooth. Target 60 FPS.
- [ ] Resize glitches — panels jump, spacing changes, animations break during window
      resize.
- [ ] Hover animation inconsistency — some elements animate, others don't.
- [ ] Spacing/alignment consistency — some components don't follow the 4dp grid; visual
      hierarchy between primary/secondary/tertiary content is weak; corner radius
      strategy inconsistent.

### 🟡 P1 — High

- [ ] **Archived decks still visible everywhere** — `is_archived` is persisted and
      toggleable, but the main dashboard lists don't filter archived decks and there is no
      "Archived" section to restore them (explicit follow-up from v2.0). `FEATURE`
- [ ] **Mobile navigation snap** — mobile nav is top/bottom only; add snap behavior
      consistent with desktop.
- [ ] **Sync indicator / sponsor button** in the shell chrome (currently only on portrait
      chrome).
- [ ] **Settings cleanup** — remove remaining randomly placed appearance options; route
      everything through the Settings Center categories.

### 🟢 P2 — Medium

- [ ] **OCR hardening** — Tesseract integration edge cases, missing-engine UX, region
      capture polish (desktop).
- [ ] **Auto-update rollout** — enable update channels (stable/beta/nightly) for end
      users; update-feed wiring is architecture-complete.
- [ ] **Grammar content** — expand the desktop grammar starter deck; add grammar data
      behind the KJD pipeline once an openly licensed grammar dataset is identified
      (`RESEARCH` until then).
- [ ] **Tablet layouts** — dedicated Android/iOS tablet polish (form-factor nav exists).

### 🔵 P3 — Low

- [ ] Accessibility completeness: full keyboard navigation, screen-reader support,
      high-contrast mode, reduced-motion completeness.
- [ ] Performance: lazy loading for large lists, image caching, memory optimization,
      startup-time reduction, Compose compiler metrics.

---

## FEATURE — specified but not yet scheduled

- Community features: shared decks, theme marketplace, study groups, optional
  leaderboards (from `FUTURE_IDEAS.md`).
- Pitch-accent diagrams, graded reading mode, handwriting-recognition improvements.
- Custom card templates (HTML/CSS), scripting API.
- Web/PWA version; Chrome OS / Wear OS review surface.
- KJD: Tatoeba example-sentence adapter, compact binary export, pitch/grammar extension
  datasets (see `kjd/README.md` → Future direction).

---

## NODE & JOURNEY (TARGET) — §157 build order

Specified in `docs/architecture/NODE_ARCHITECTURE.md` (§76–§162) + `docs/architecture/nodes/`
+ ADR-0013/0014/0015. **Nothing here is implemented** (NODE §158). Order follows the §157
handoff and STANDARDS §365 phases; items are foundational-first and dependency-ordered.

- [ ] **Node contract + registries** (ADR-0013) — typed node model (§78), storage decision
      (new tables vs read-model over AppData/UserData), NODE_TYPE_REGISTRY +
      RELATIONSHIP_REGISTRY as code. `FEATURE`
- [ ] **Knowledge graph layer** — edges (§79–§80), indexing, the §149 bridge
      (`represents`, `encountered_by`, `mined_from`, `appears_in_media`). `FEATURE`
- [ ] **Dictionary as node interface** — node-anchored lookup + traversal chips (§81). `FEATURE`
- [ ] **Kanji / Vocabulary node experiences** — full §82/§83 sections incl. "where have I
      seen this?" (needs knowledge graph + media exposure events). `FEATURE`
- [ ] **User Knowledge model** — `KNOWLEDGE_STATE_MODEL.md`: dimensions, state machine,
      event-derived transitions, FSRS bridge (scheduler logic untouched — STANDARDS §6). `FEATURE`
- [ ] **Knowledge scoring** — derived dials (§85), simplified display, no fabricated
      precision (§290). `FEATURE`
- [ ] **Browse / Library node views** — §128/§129 filters and grouping as node queries. `FEATURE`
- [ ] **Stats over node events** — §131 drill-down; Journey/media/study events feed one
      stream (STANDARDS §210–§213). `FEATURE`
- [ ] **Media as node family** — §130; Series/Episode/Scene/SubtitleLine nodes over the
      existing media engine. `FEATURE`
- [ ] **Mining into the graph** — MiningEvent → `mined_from` edges; card provenance. `FEATURE`
- [ ] **Journey engine evaluation** (STANDARDS §242) — evaluate Godot/Unity/Unreal,
      document decision, then runtime prototype (ADR-0014). `RESEARCH` → `FEATURE`
- [ ] **Content pipeline** (ADR-0015) — schemas, validation gates (§148), packages
      (§145); then the node editor (§147). `FEATURE` (editor `FUTURE`)
- [ ] **Journey runtime prototype** — `JOURNEY_RUNTIME_SPEC.md`: UI layers, HUD, overlays,
      input, save. `FEATURE`
- [ ] **Kamakura + Enoshima vertical slice** (§91) — the proof gate before any world
      expansion; 3D/ART/AUDIO/CONTENT PRODUCTION. `FEATURE`
- [ ] **Children mode** (§115) — configuration + content filter over the same runtime. `FEATURE`

## Node & Journey — target architecture build order (§157)

> **Contract**: ADR-0013 (node layer) · ADR-0014 (Journey target) · ADR-0015 (content
> authoring). Status labels follow the NODE §158 taxonomy — nothing in this section is
> claimed as shipped. Deep specs: `docs/architecture/NODE_ARCHITECTURE.md` (§76–§162) +
> `docs/architecture/nodes/`. The §157 order is a dependency graph, reordered here
> against actual repo state: foundations (steps 1–16) are largely done; the target work
> is the node layer, knowledge graph, and the Journey phases.

| # | Task (§157) | Repo status | Gate / dependency | Docs |
|---|---|---|---|---|
| 1 | Repository/core stability | ✅ done | — | `PRODUCT_AUDIT.md`, `ENGINEERING_AUDIT.md` |
| 2 | Design system | ✅ done | — | `docs/design/` |
| 3 | Database | ✅ done (two SQLDelight DBs) | — | `architecture/database.md` |
| 4 | **Node model** (contract + registries + storage) | 🔬 TARGET | decide storage per ADR-0013; SQLDelight `node`/`edge` tables or read-model | `nodes/NODE_DATA_MODEL.md`, `NODE_TYPE_REGISTRY.md`, `RELATIONSHIP_REGISTRY.md` |
| 5 | **Knowledge graph** (user knowledge layer) | 🔬 TARGET (profile exists) | after 4; events first (ADR-0016) | `nodes/KNOWLEDGE_STATE_MODEL.md`, `EVENT_CATALOG.md` |
| 6 | Dictionary | ✅ done (suite + bundled) | — | `architecture/dictionary.md` |
| 7 | Kanji | ✅ done | — | `architecture/language-model.md` |
| 8 | Vocabulary | ✅ done | — | `architecture/language-model.md` |
| 9 | Library | ✅ done | — | `features/LIBRARY.md` |
| 10 | Browse | 🟡 partial (SearchEngine; node-family browse is target) | after 4 | `UX_FLOWS.md` §3 |
| 11 | Study engine | ✅ done (FSRS-5, unified store) | — | `architecture/study-engine.md` |
| 12 | Statistics/events | ✅ done (event-driven) | — | `architecture/statistics.md`, `EVENT_CATALOG.md` |
| 13 | Exams | ✅ done | — | `architecture/exams.md` |
| 14 | Media abstraction | ✅ done | — | `architecture/media.md` |
| 15 | Mining | ✅ done | — | `architecture/mining.md` |
| 16 | Integrations | 🟡 partial (AnkiConnect e2e BLOCKED) | — | `architecture/integrations.md` |
| 17 | **Journey runtime prototype** | 🔬 TARGET | engine evaluation (STANDARDS §242) → ADR; then node foundations (4–5) | `nodes/JOURNEY_RUNTIME_SPEC.md`, `GAMEPLAY_SYSTEMS.md` |
| 18 | **World data** (packages, Kamakura/Enoshima) | 🔬 TARGET | content pipeline (ADR-0015) | `nodes/JOURNEY_WORLD_SCHEMA.md`, `CONTENT_AUTHORING.md` |
| 19 | **Vertical slice** (Kamakura + Enoshima) | 🔬 TARGET | 17 + 18; the §91 proof gate | `nodes/GAMEPLAY_SYSTEMS.md` §4, `TEST_PLAN.md` §13 |
| 20 | Children mode | 🔬 TARGET | after 19 | `nodes/GAMEPLAY_SYSTEMS.md` §22 |

Open decisions that gate this work: one-product consolidation (§7-1 in `ENGINEERING_AUDIT.md`)
and the game-engine evaluation (STANDARDS §242 — must be a documented ADR before any
Journey code).

## RESEARCH — needs investigation before scheduling

- AI-assisted review scheduling (how it would coexist with FSRS-5; likely a future
  enhancement, not a replacement).
- Openly licensed grammar and example-sentence datasets for the KJD pipeline.
- Plugin runtime sandboxing design (capability model, subprocess vs classloader) — see
  ADR-0011.

## TECHNICAL DEBT — deliberate paydowns

- **Two jdata implementations** — `kjd/` (standalone) and the desktop suite's
  `engine/jdata` platform evolved separately; consolidate into one pipeline (ADR-0007).
- **No UI tests** — Compose UI testing harness not established (see `../testing/README.md`).
- **Platform actuals under-verified** — several Android/iOS/Windows paths are
  "code-complete, runtime-unverified" (tracked in `CURRENT_ISSUES.md` → BLOCKED).
- **Website `dist/` is committed** — regenerate it on every docs change (the site build
  is a manual step; consider CI).
- **Scattered status tracking** — features historically tracked across FEATURES.md /
  TODO.md / COMPLETED.md; FEATURES.md is now the single source of truth.

## BLOCKED — waiting on something external

- iOS runtime verification (APKG import/export, file pickers) — needs a macOS build +
  device/simulator.
- Windows runtime verification (media keys, native drag, tray notifications) — needs a
  Windows machine.
- Android SAF picker verification (import/export, re-import grant) — needs an Android
  build + device.
- AnkiConnect import end-to-end — needs a live Anki + AnkiConnect instance.
- Flathub / Snap publishing — needs maintainer accounts + CI wiring.

---

## DONE — delivered (historical, summarized)

Everything below is **shipped**. Details in `COMPLETED.md` and `CHANGELOG.md`.

- **Window experience** — custom 44dp title bar, scoped drag region, 8-zone resize,
  system menu, window-state persistence, rounded corners (v2.2.1).
- **Theme system** — BaseMode Light/Dark/OLED; accent schemes; **17 built-in presets**
  (Signature, OLED, Dark Gray, Light, Reading, Solarized, Nord, Catppuccin, Gruvbox,
  Tokyo Night, Dracula, Nothing OS, Material, Glass, Cotton Candy, Ocean, Forest);
  Theme Studio (color wheel, gradients, motion, layout, JSON import/export) (v2.0/v2.2).
- **Navigation** — floating dock island (4 edges), expanded/compact/hidden layouts,
  compact tab bar <720dp, persistent workspace panels, command palette (v2.2).
- **Study engine** — JLPT + grade decks, vocab/flashcards, writing + stroke evaluation,
  FSRS-5 SRS, deck archive, unified Library hub, text analysis, statistics + exams +
  achievements (v2.0+).
- **Installer & first-run** — branded installers (Inno/DMG/AppImage/deb/rpm),
  onboarding wizard, auto-update architecture (v2.2.1).
- **Anki interop & persistence** — `.apkg` import/export, JSON/CSV/TSV/TXT pipeline,
  persistent card pool (v2.3 in progress).
