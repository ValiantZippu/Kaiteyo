# Kaiteyo — Testing Plan (Master)

> **Status**: `ARCHITECTED` — derived from `planning/ROADMAP.md` phase exits.
> Companion: `../testing/README.md` (existing strategy), `../architecture/nodes/TEST_PLAN.md` (node/Journey contract), `ROADMAP.md`.

## 1. Levels

| Level | Framework | Location | What |
|-------|-----------|----------|------|
| Unit | `kotlin.test` (commonTest) + JUnit | `core/src/commonTest`, `desktopApp/src/jvmTest`, `kjd/src/test` | pure logic: FSRS, search, graph, mining dedup, Anki mapper, subtitle parser, OCR normalization, statistics derivation |
| Integration | `kotlin.test` + in-memory fakes | same | DB queries (SQLDelight), Koin wiring, platform SAFFake, migration, sync queue |
| UI (Compose) | Compose test + semantics | `core/src/commonTest` / `desktopApp/jvmTest` | navigation, screen states (loading/empty/error/offline), dictionary popup, subtitle browser |
| Database | SQLDelight with in-memory driver | commonTest | schema, migrations, indices, daily_stats rollups |
| Media | `MediaEngineLifecycleTest` + `MediaEngineTickSafetyTest` | jvmTest | tick safety (hostile backend), lifecycle (rapid open/tick/swap), screenshot, A-B loop |
| Subtitle | parser fuzz + timing tests | jvmTest | SRT/ASS/SSA/VTT/embedded, normalization, offset, search index, episode matching |
| Dictionary | segmenter + deinflection + SearchOcrProvider | commonTest/jvmTest | EXACT/PREFIX/KANA/DEINFLECT + scoring + priority |
| OCR | OcrResult + reading order + caching | jvmTest | regions, confidence, vertical read order, hash key, LRU, guided-setup path |
| Mining | MiningEngine + MinedRecord | jvmTest | validation, dedup (hash idempotent), recipe mapping, Both destination fan-out |
| Anki | AnkiPackage codec + AnkiConnect | commonTest/jvmTest | .apkg round-trip, media extraction, transport retry queue |
| Platform | adapter + ContentService resolve | commonTest | search, fetch, dedup by ExternalIds, pull/push, offline queue |
| Browser | BrowserEngine + Bridge | jvmTest | selection → DictionaryService, video event → MediaSession, isolation (no internal API exposure) |
| Game | World systems | jvmTest (existing 58 files) | chunks, terrain, player, NPCs, quests, saves, render backend boundary |
| Performance | benchmarks | jvmTest/androidTest | 60/120fps, large library/subtitles/PDF/manga, OCR batch, startup, memory |
| Offline | scenario | — | all local paths without network; platform/Anki queued; cached subtitles readable |
| Failure | chaos | — | missing engine, malformed file, unsupported format, timeout, permission denied, storage full, partial failure |

## 2. Acceptance criteria (representative)

### Media acceptance (prompt §TESTING PLAN)

A user can: 1) import a local video, 2) organize it into a collection, 3) attach a subtitle, 4) play it, 5) navigate subtitle cues, 6) select Japanese text, 7) open Yomitan popup, 8) view definition, 9) create mining context, 10) save to Kaiteyo, 11) optionally export to Anki, 12) close the app, 13) reopen, 14) resume at previous position. If any fails, Media is not complete.

### Reading acceptance

Open local manga (`CBZ`/folder) + EPUB + scanned PDF + web page → selectable text → Yomitan → mine with screenshot → Library shows item → Stats counts reading time → reopen resumes at page.

### Mining acceptance

Same `MiningContext` from media/reading/OCR/clipboard mines to Kaiteyo, Anki, or Both with no divergence; offline Anki mines are queued and visible with retry.

### Browse/Platform acceptance

Link AniList/MAL → search → import → one `Content` row (not two) → Library shows → sync progress both ways (or correctly read-only for IMDb).

### Stats/Home acceptance

One `ActivityEvent` ledger; Home and Stats never diverge; no fabricated numbers; blank days stay blank.

### Game acceptance

Game play → `ActivityEvent` → Stats; `WORLD_TEXT_SELECTED` → dictionary; quest reward → DeckService; knowledge adapts to known kanji/vocab/grammar.

## 3. Commands

```bash
./gradlew :core:allTests
./gradlew :desktopApp:compileKotlinJvm   # Definition of Done gate
./gradlew :desktopApp:test
./gradlew :kjd:test
```

## 4. Evolution

New subsystem → new acceptance checklist in this file + `MASTER_TODO.md` KT-* entry with test requirements.
