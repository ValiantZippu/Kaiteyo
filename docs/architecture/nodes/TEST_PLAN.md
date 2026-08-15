# Test Plan (Node & Journey)

**Status**: TARGET — the test contract the implementation must satisfy. Existing test
infrastructure: `kotlin.test` in `commonTest` + JUnit platform (`:core:allTests`,
`:desktopApp:test`); see `docs/testing/README.md` and STANDARDS §215–§218.
**Source spec**: STANDARDS §215–§218, §279–§282, §342 (validation ladder) ·
[Node Architecture master spec](../NODE_ARCHITECTURE.md) §76–§162

> **Validation ladder** (STANDARDS §342): use the cheapest useful validation first —
> L1 format/lint → L2 static analysis → L3 targeted unit test → L4 targeted integration
> test → L5 module build → L6 full build → L7 e2e. Documentation passes do NOT build
> (§341).

## 1. Principles

1. Test the **contract, not the implementation**: node/edge/knowledge/event contracts are
   tested against their registries and schemas, so storage swaps (ADR-0013) don't break
   tests.
2. Test at **real scale** for search/knowledge queries (STANDARDS §279, §369) — not with
   20 words.
3. Test **malformed everything** (STANDARDS §280, §357–§358): media files, subtitles,
   imports, packages are untrusted input.
4. **Japanese text is first-class** (§281): full-width/half-width, kana variants,
   iteration marks, surrogates, combining marks.
5. UI tests cover the critical flows (STANDARDS §218): launch, navigation, search,
   lookup, deck creation, review, media playback, subtitle selection, mining, settings,
   floating bubble, sidebar, launchpad — plus the new Journey HUD/overlay flows.

## 2. Performance budgets (STANDARDS §188–§190)

| Metric | Budget (reference desktop) | Budget (reference mobile) |
|---|---|---|
| App cold start (first frame) | ≤ 2.5 s | ≤ 4 s |
| Screen transition | ≤ 150 ms p95 | ≤ 250 ms p95 |
| Dictionary lookup / search query | ≤ 50 ms p50, ≤ 150 ms p95 at full dataset | ≤ 100 ms p50 |
| Node traversal ("where have I seen this") | ≤ 150 ms p95 at full scale | ≤ 300 ms p95 |
| Frame time (app UI) | 60 FPS steady | 30–60 FPS |
| Frame time (world, slice) | 60 FPS (§143 tier) | 30 FPS (§143 tier) |
| Media startup (video) | ≤ 1.5 s | ≤ 3 s |
| Subtitle parse (1h video) | ≤ 200 ms | ≤ 500 ms |
| Card creation (mine) | ≤ 100 ms | ≤ 250 ms |
| World cell stream (adjacent cells) | ≤ 500 ms background, no frame hitches | ≤ 1 s |
| Save/load | ≤ 200 ms | ≤ 500 ms |
| Memory (large dataset open) | within platform budgets (§190) | within platform budgets |

Violation → profile first (STANDARDS §189), then fix; record in the budget table.

## 3. Node system tests (NODE §76–§83, ADR-0013)

### 3.1 Registry conformance (unit, L3)

- Every node type in code/data exists in [NODE_TYPE_REGISTRY](NODE_TYPE_REGISTRY.md).
- Every relationship type exists in [RELATIONSHIP_REGISTRY](RELATIONSHIP_REGISTRY.md);
  `related_to` lint fails where a precise type exists (§80).
- `schema_version` bumps are additive-only; no field re-purposing (registry §2).

### 3.2 Contract (unit, L3)

- Node without `id`/`nodeType`/`schemaVersion`/`source` is rejected (§78).
- Provenance: `source` is mandatory and truthful; derived nodes declare
  `source = kaiteyo` + `derived_from` edges (§78).
- `UNIQUE (source, source_id)` idempotence: importing the same row twice → same node.

### 3.3 Edge model (unit + db, L3–L4)

- Cardinality violations rejected (RELATIONSHIP_REGISTRY §3).
- Traversal both directions with correct indexes (NODE_DATA_MODEL §8.1–§8.3).
- Deletion policy per edge family: cascade vs tombstone vs rewire (registry §5).
- Forbidden cycles rejected (story/quest `requires` graphs).

### 3.4 Query patterns at scale (integration, L4–L5)

- §8.1 kanji→words and §8.2 word→kanji with full dictionary dataset within budget.
- §8.3 "where have I seen this" with 50k words + 10k subtitle lines + 1k discoveries.
- §8.4 knowledge aggregation; §8.5 review-candidate query; §8.6 bridge; §8.7 discovery
  stats.

### 3.5 Normalization & unicode (unit, L3)

- NFC/NFKC policy (§282): full-width/half-width equivalence for search; exact
  representation preserved for content; kana variants (ひらがな/カタカナ/小書き) handled.

## 4. User knowledge tests (KNOWLEDGE_STATE_MODEL, NODE §84–§85)

- State machine: every §3 transition is unit-tested; illegal transitions rejected.
- "Recognize 食 but not write it" is representable (dimension separation).
- Evidence-driven: states only change via trigger events; no UI self-rating as truth.
- Transition history append-only; replay reproduces state (L4).
- FSRS boundary: knowledge layer never writes SRS fields; scheduler arithmetic untouched
  (STANDARDS §6 never-change — guard test).
- Score derivation: `knowledge_score` rebuildable from `event_log` alone (L4).
- Precision honesty: scores labeled estimated where basis is partial (§290).

## 5. Event system tests (EVENT_CATALOG, STANDARDS §210–§213)

- Every event type validates against its payload schema (versioned).
- Replay test: rebuilding heatmap + knowledge scores from a backup log reproduces
  results (L4).
- AFK model (§212): activity signals classified correctly into
  active/passive/idle/background.
- No credentials/UI-state in payloads (static check, L2).

## 6. Dictionary & search tests (§186–§187)

- Search latency at full dataset; filters combine without brute-force scans.
- Script detection: kanji/kana/romaji/English queries resolve correctly.
- Import: valid/malformed/partial/duplicate/encoded/large archives (§280); provenance
  recorded (§185).
- `related_to` lint (§80) runs over imported content.

## 7. Study engine & library tests

- FSRS-5 behavior (existing suite of tests; never regress — STANDARDS §6).
- Library filters (All/Decks/Collections/Imported/Recent/Favorites) reflect live node
  queries.
- Bulk actions transactional; delete respects tombstones; archive filtering (TODO P1).
- Import/export: JSON/CSV/TSV/TXT/APKG round-trips; exports not tied to internal schema
  (STANDARDS §207).

## 8. Media & mining tests

- Media playback: bad files, missing codecs, corrupt containers, huge metadata
  (STANDARDS §357) — recoverable errors, never crash.
- Subtitle: SRT/ASS/SSA/VTT parse; malformed/encoded/large files (§280); timing edge
  cases; independent of player backend (§195).
- Mining: every source kind (dictionary/browser/subtitle/OCR/clipboard/reader/image/
  audio/integration/Journey) produces a card + `mined_from` provenance; duplicate
  protection; undo (§196).
- Subtitle index → `appears_in_media` edges at scale (L4).

## 9. Journey runtime tests (TARGET, slice-gated §91)

### 9.1 World & streaming
- Cell streaming: adjacent loads/unloads; no full-region load; no frame hitches
  (§92, §143 budgets).
- Determinism: same (NPC, time, weather, season, quest state) → same schedule output
  (§98, L3 unit on NPCSchedulerService).

### 9.2 Interaction & knowledge overlay
- Object → prompt → glossary → full dictionary → card (L7 e2e in slice).
- Interaction types resolve per §94 table; new object = content only (no code).

### 9.3 Quest & story
- Objective conditions evaluate from world state + events; invalid conditions rejected
  at content validation (§148) — not at runtime.
- Story ordering enforced (`requires`); save/load restores exact beat (§144).

### 9.4 Photography
- Capture → recognition → `depicts` edges; failed recognition explicit; photo →
  collection/quest/card paths.

### 9.5 Save system
- Save/load determinism: identical inputs → identical world state.
- Versioned; corrupt save → recoverable error with last-good-restore (§144).

### 9.6 Difficulty adaptation
- One geometry, N depths: BEGINNER/INTERMEDIATE/ADVANCED variants render per level;
  mid-game switch safe (only presentation changes; §113).

### 9.7 Performance (§143)
- Slice runs at tier targets on reference desktop + reference mobile; budgets documented.

## 10. Content pipeline tests (ADR-0015, §145–§148)

- Each §148 gate: schema, relationship, asset, localization, license, performance —
  invalid content cannot publish or install.
- Package install: manifest hash, dependency versions, min engine version; unknown
  version → clear error, no crash (§145, STANDARDS §219).
- No code execution from content (STANDARDS §361) — security test (L4).

## 11. Integration tests (STANDARDS §199–§201, §292–§293)

- Anki unavailable → "Anki unavailable", app continues (§201); AnkiConnect adapter
  against a live instance (BLOCKED list in TODO until hardware available).
- Yomitan dictionary import round-trip; glossary engine works without browser extension
  assumptions (§197).
- External metadata provider adapter; rate-limit/backoff/timeout behavior (§293).

## 12. UX & platform QA (STANDARDS §300–§309)

- Accessibility QA (§300): keyboard-only, touch, mouse, controller, large text, reduced
  motion, dark/light, high contrast.
- Device classes (§302): small phone, large phone, tablet, small laptop, desktop,
  ultrawide.
- Responsive modes (§303): COMPACT/STANDARD/WIDE/ULTRAWIDE reflow without jumps.
- Window management (§304–§305): maximize/minimize/restore/resize/fullscreen/DPI/
  multi-monitor; custom title bar behavior correct.
- Visual regression (§308) for Home, Browse, Library, Stats, Dictionary, Media,
  Settings, Launchpad, Sidebar, Journey HUD where practical.
- Design QA checklist per screen (§309): alignment, spacing, typography, contrast,
  hierarchy, responsive, interaction, animation, empty/loading/error states.
- Visual bug register (§136): every find becomes a tracked bug in CURRENT_ISSUES.md.

## 13. Acceptance criteria gate (the §91 proof)

The vertical slice is not "done" until these pass on reference hardware:

1. §87 onigiri loop e2e (examine → glossary → photo → discovery → optional card → stats/
   quest update) — L7.
2. All §2 budgets met on reference desktop + mobile.
3. Save/load determinism across restarts.
4. Content-only additions (new object/NPC/dialogue/quest/story) require zero engine
   changes — proven by the content pipeline tests (§10).
5. No STANDARDS §325–§329 violations: no fake implementations, hardcoded data, or
   placeholder logic in the slice.
