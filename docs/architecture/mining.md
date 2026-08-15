# Kaiteyo Architecture — Mining Workflow Specification

**Status**: Implemented (suite engine; reached in the shipped app via the Media Centre)
**Owner**: suite `ua.syt0r.kanji.desktop.engine.mining`
**Related**: `docs/architecture/dictionary.md` · `docs/architecture/media.md` ·
`docs/architecture/integrations.md` · `docs/user-guide/DESKTOP_SUITE.md`

## 1. Purpose

A unified, source-agnostic card-creation pipeline (§196): anything you encounter — a
dictionary entry, subtitle line, screenshot, OCR result, clipboard text, browser selection,
reader selection, image, audio, or the integration API — becomes a **card** in the Kaiteyo
card pool with full source/tag/note/media data so it can be studied immediately. Kaiteyo's
own card pool is always the primary destination; Anki is an optional bridge, never the
database underneath (§199).

## 2. Payload (`MiningPayload`)

The single contract between every source and the engine (serializable — it persists in
pending exports and crosses the API):

| Field | Purpose |
|---|---|
| `headword` (required) | the mined word/expression |
| `reading` | kana reading |
| `definition` | gloss (truncated to first line, ≤ 400 chars in the card) |
| `sentence` | context sentence (duplicate key + card note) |
| `screenshotPath` / `audioPath` / `videoPath` | media references |
| `timestamp` | media timestamp (Double, seconds) |
| `source` / `sourceDetail` | provenance (MiningSource label + detail) |
| `tags` / `flags` / `notes` | user annotations |
| `deckId` | target deck (default `DesktopCard.DEFAULT_DECK_ID`) |
| `example` | example sentence |
| `pitchAccent` | `List<MinedDictionaryData>` from the lookup |

## 3. Sources (`MiningSource`)

`Dictionary · Browser · Video · Subtitle · Ocr · Clipboard · Reader · Image · Audio · Api`
(Integration API). Each source builds a payload and calls `mine()` — no source has
bespoke card-creation logic.

## 4. MiningEngine — card creation (`mine(payload, destinationOverride)`)

### Destination resolution (`resolveDestination`)
Order: explicit override → `media.mine-destination` setting (`"anki"` / `"both"` / default)
→ legacy `media.anki.send-mined` flag. `CardDestination`: Kaiteyo / Anki / Both.

### Anki-only path
Forward to `miningIntegration.forward(payload, Anki)`. On success → record mine, activity
log, toast, return `null` (no Kaiteyo card). **On failure → the word is never lost**:
create a native card anyway; if Anki was enabled, queue a `PendingAnkiExport` for retry;
toast explains ("Anki unavailable — saved to Kaiteyo, export queued for retry" vs "AnkiConnect
is disabled — saved to Kaiteyo") (§201).

### Native path
1. Definition normalization (first line, ≤ 400 chars; `"(no definition)"` fallback).
2. **Duplicate detection** — default key = `sentence` (first 60 chars) + headword, scanned
   against the existing card pool's note text. Policy from `media.mine-duplicate-policy`
   (default `"create"`):
   - `skip` → toast + return the existing card (no duplicate).
   - `update` → merge meaning/reading, add `re-mined` tag, toast, return updated card.
3. `createNativeCard(payload)` → `DesktopCard`; `state.addCard(card)`; activity log with
   card id.
4. **External transports** (when enabled): forward to AnkiConnect (and any registered
   `MiningTransport`); Anki failures queue `PendingAnkiExport` — Kaiteyo mining never
   depends on them.
5. `recordMine(...)` → `MinedRecord` (dedupe + activity feed).
6. Opt-in OS notification when the app is backgrounded (`media.notifyMined`).
7. Subtitle source + active media → `recordMiningEvent(card, payload)` for the
   Media ↔ Card round-trip ("Recently Mined" jumps back to the exact timestamp).
8. Toast: `Mined "X" → study it in Review`.

### Conveniences
- `mineFromDictionary(match: DictionaryMatch)` — wraps a lookup match into a payload.
- `payloadForEntry(entry, dictionaryName)` — payload from `DictionaryEntry`
  (fills `MinedDictionaryData` pitch/frequency).
- `openMining(payload?)` / `closeMining()` / `draft` — the mining dialog (power-user
  workflow with `MiningTemplate` presets and source selection).

## 5. Persistence & repeat protection

`~/.kaiteyo/mining-state.json` (`MiningStateDto`): `recentSources`, `templates`,
`mines: List<MinedRecord>`, `pendingExports`. Every mine records id, headword, createdAt,
source, cardId, destination, anki status/error — re-mining the same capture is detected
rather than duplicated.

`PendingAnkiExport(id, mineId, payload, createdAt, attempts, lastError)` — a mine Kaiteyo
accepted but AnkiConnect could not receive. `retryPendingAnki()` replays the queue without
re-mining and without duplicating the Kaiteyo card. `pendingExportCount` badges the UI.

## 6. Pipeline (conceptual)

```
SUBTITLE / OCR / CLIPBOARD / DICTIONARY / BROWSER / READER / IMAGE / AUDIO / API / VIDEO
  ↓ TEXT SEGMENT (shared segmenter)
  ↓ TOKEN
  ↓ DICTIONARY (gloss via DictionaryService)
  ↓ GLOSS
  ↓ USER SELECTION (DictionaryPopup → "Create card" / "Mine subtitle" / "Mine from OCR")
  ↓ CARD (mine() → card pool)
  ↓ MEDIA REFERENCE (screenshot / audio clip / video + timestamp)  [when applicable]
  ↓ DECK (default or chosen)
  ↓ (optional) ANKI forward → PendingAnkiExport on failure
```

## 7. Integrations

- **AnkiConnect transport** (`AnkiConnectTransport : MiningTransport`) — JSON-RPC-style
  requests (`version 6`, optional API key, 5 s connect / 8 s request timeouts); operations
  via `call(action, params)`: `testConnection`, `deckNamesAndIds`, `findNotes`,
  `notesInfo`, `findCards`, `cardsInfo`, `retrieveMediaFile`, `canAddNotes`-style dedupe,
  `send(payload)` building a Basic note with `Kaiteyo::<deck>` (or `deckOverride`), tags,
  and screenshot/audio attached as media (base64). `configured` = host + valid port;
  `connected`/`lastError` reactive state.
- **MiningIntegration** — the registry of `MiningTransport`s; `forward(payload,
  destination)` fans out to enabled transports and returns per-transport results.
- **GameSentenceMiner** — a transport payload builder for external tools
  (`gameSentenceMinerPayload`).
- **LocalApiServer** — `/api/mine` + `/api/status` + `/api/health` (see
  `docs/architecture/integrations.md` §4); `IntegrationCardRequest.toPayload()` converts
  API requests into `MiningPayload`.

## 8. Error model

- Anki unreachable/disabled → card still created; export queued; explicit toasts (§201).
- Duplicate mine → policy-driven (skip/update), never a silent duplicate.
- Failed lookups → honest toast + log; no card created (§219).
- All persistence reads/writes `runCatching` — corrupt mining state resets to empty.

## 9. Tests

- Mining engine tests: payload→card, duplicate policies, pending-export lifecycle, retry
  (`desktopApp/src/jvmTest/.../mining/`).
- `AnkiConnectTransport` unit tests with a stubbed HTTP layer where present.
- Gaps: end-to-end subtitle→mine→card→review (§218), OCR→mine integration, API e2e.

## 10. Open items

- Mining UX reachable outside the Media Centre in the shipped app (consolidation,
  audit §7-1).
- Batch/queue mining UX beyond AnkiConnect; mining statistics dashboard
  (`MiningStatisticsStore` exists).

## 11. Node-layer integration (TARGET — ADR-0013, NODE §150)

### 11.1 Mining as graph provenance

- `mine()` creates: the card (Library), a `mining_event` node, and `mined_from` edges
  (card ← source line/scene/photo/object/audio) with full provenance (source kind,
  source ref, timestamp).
- Duplicate protection becomes **node-identity based**: the same source ref + headword
  yields the same `mined_from` edge target (idempotent-ish behavior preserved).

### 11.2 Sources map to world & media nodes

| Mining source | Node anchor | Edges created |
|---|---|---|
| dictionary | dictionary entry → language node | `mined_from` + `generated_from` |
| video/subtitle | `subtitle_line` | `mined_from` (+ `appears_in_media` index) |
| screenshot/OCR | `screenshot` | `mined_from`, `depicts` → recognized objects/nodes |
| Journey object/photo | `object` / `photograph` | `mined_from`, `depicts`, `encountered_by` |
| clipboard/reader/browser | `mining_event` | `mined_from` |
| integrations API | `mining_event` | `mined_from` |

### 11.3 The §150 loop seam

The mining event is the point where media/Journey exposure becomes *owned study
material*: card → deck → review → stats, with the original source one hop away
("where did this card come from?" — always answerable via `mined_from`).

### 11.4 Acceptance criteria

- Every mined card traces to its source through node edges (provenance walk).
- Undo (`mining_undone`) retires edges + card consistently (tombstone policy, registry
  §5) — no orphan edges.
- New source kinds are additive (payload + edges), never new special cases in the
  engine (SERVICE_CONTRACTS §10).
