# Kaiteyo Glossary

The terminology used across the documentation. Keep this in sync when terms are
added or redefined (§335 freshness rule).

## Product & platform

| Term | Meaning |
|---|---|
| Kaiteyo | 書いてよ — "write it!" The product (formerly Kanji Dojo fork) |
| KMP / CMP | Kotlin Multiplatform / Compose Multiplatform (the stack) |
| Desktop suite | The JVM-only immersion suite (`ua.syt0r.kanji.desktop.*`): dictionary, media, mining, OCR, browser, learning engines |
| Core app / shipped app | The shared Compose MPP product (`KaiteyoApp`) |
| Two-app problem | Duplicated data systems between core app and desktop suite; media is bridged, SRS/settings/statistics/nav/decks await consolidation |
| KJD | The standalone data platform (`kjd/`): open datasets → bundled language DB |

## Node & knowledge (target — ADR-0013/0016)

| Term | Meaning |
|---|---|
| Node | A typed entity with stable identity (kanji, word, sentence, location, NPC, quest, media…) |
| Edge / relationship | A typed connection (never a generic `related_to`), e.g. `contains_character`, `encountered_by` |
| Node families | LANGUAGE, LEARNING, MEDIA, WORLD, GAMEPLAY, USER, SYSTEM |
| Provenance | Source/version/license trace of every node (`source`, `sourceId`, `schemaVersion`) |
| Knowledge state | Per-dimension state machine: UNKNOWN → SEEN → RECOGNIZED → PARTIALLY KNOWN → LEARNING → FAMILIAR → MASTERED (+ decay paths) |
| Dimension | reading, writing, listening, recognition, production, context, meaning, pronunciation |
| Evidence | An event that updates knowledge (review, writing, exam, lookup, exposure) |
| ADR-0016 rule | Knowledge is derived from events; self-rating is a signal, never truth |

## Learning & SRS

| Term | Meaning |
|---|---|
| FSRS-5 | The spaced-repetition algorithm (ADR-0006); *when* to review — never changed |
| Note → CardType → NoteCard | Unified learning model: content → study direction → card with own SRS state |
| LearningStage | introduced / learning / established / mature (criteria-based) |
| Card pool | The persistent desktop card store (`~/.kaiteyo/library/cards.json`) |
| Mining | Creating a card from context (subtitle, world text, dictionary, OCR) via `MiningPayload` |
| Mining destination | Kaiteyo / Anki / Both (configurable) |
| AnkiConnect | The local HTTP API for Anki interop (integration, not dependency) |
| JLPT | Japanese-Language Proficiency Test N5–N1; one alignment axis, not the whole curriculum |
| Kana content | Full syllabary content system (base, dakuten, handakuten, yōon, extended katakana) |
| Stroke evaluation | Per-stroke scoring (shape/direction/order) — built-in dataset + KanjiVG when installed |

## Media

| Term | Meaning |
|---|---|
| Media Centre | The core destination hosting the suite's media workspace (backends: VLC/mpv/Java Sound) |
| KaiteyoMediaPlayer | The player abstraction; backends behind it (VLC/mpv) — the rest of the app never depends on backend APIs |
| Subtitle engine | SRT/ASS/SSA/VTT parsing independent of the player backend |
| Playback capability | What a backend can do (gated, honest UI) |

## World & game (target)

| Term | Meaning |
|---|---|
| Journey | The explorable Japan world (ADR-0014) — a destination in the Launchpad |
| Vertical slice | Kamakura + Enoshima — the proof gate before expansion (§91, §366) |
| Map cell / chunk | The streaming unit of the world |
| Fidelity L0–L4 | Abstract map → recognizable region → street accuracy → landmark detail → playable location |
| Interaction node | The smallest actionable world entity (sign, object, seat, door) |
| Simulation tier | NPC detail by distance: near / same area / abstract (§105) |
| Knowledge-density overlay | Map heat of known vs unmet words per area |
| WORLD_TEXT_SELECTED | The event that starts the in-world learning flow (text → analyzer → dictionary → knowledge) |
| Content package | Versioned, validated, installable content (ADR-0015) |
| Child mode | A configuration + content filter over the same runtime (§115) |

## Content & data

| Term | Meaning |
|---|---|
| AppDataDatabase / UserDataDatabase | The two SQLDelight databases (immutable app data / mutable user data) |
| Open dataset | JMdict, KANJIDIC, KanjiVG, frequency, JLPT (license-verified; `docs/data/SOURCES.md`) |
| Provenance record | dataset name, version, license, source URL, checksum, transformation version (§185) |
| Package gates | Validation checks every content package must pass (ADR-0015) |

## Architecture & process

| Term | Meaning |
|---|---|
| ADR | Architecture Decision Record (Context/Decision/Alternatives/Consequences/Status) |
| Screen pattern | 4-file pattern: Contract / ViewModel / Module / UI (+ registration in `AppModule.kt`) |
| Never-change list | SRS logic, `.sq` schemas, package namespace, build config (see `docs/development/AI_CONTEXT.md`) |
| §373 handoff state | The required documentation inventory (indexed in `ENGINEERING_AUDIT.md` §10) |
| Ds* | Desktop suite design-system components |
