# Anki Integration

Kaiteyo interoperates with [Anki](https://apps.ankiweb.net/) in two ways:

1. **`.apkg` package import/export** — a self-contained file format, implemented in shared
   `core` and available on desktop, Android, and iOS.
2. **AnkiConnect** — a live bridge to a running local Anki installation, implemented in
   the desktop suite (push mined cards, import decks).

## Purpose

Anki compatibility is a deliberate feature of the "your data is yours" philosophy: users
who have years of Anki history can bring it into Kaiteyo, and users who mine cards in
Kaiteyo can forward them to Anki. The formats are documented, offline, and do not require
any Kaiteyo service.

## `.apkg` import/export

### Architecture

- Shared contract: `core/src/commonMain/.../transfer/` — `AnkiPackage.kt` plus
  platform actuals:
  - JVM: `AnkiPackage.jvm.kt` (SQLite via sqlite-jdbc)
  - Android: `AnkiPackage.android.kt` (Android built-in `SQLiteDatabase`, schema v11)
  - iOS: `AnkiPackage.ios.kt` (SQLDelight `NativeSqliteDriver` + a dependency-free
    pure-Kotlin ZIP/inflate codec — `IosZip.kt` / `IosInflate.kt`)
- Import/export UI: `ImportExportSystem.kt` + `core/.../transfer/ImportExportViewModel`
  and the `ImportExportPipeline`.

### Data flow (export)

```
Kaiteyo decks/cards
  → template rendering ({{Field}}, {{text:}}, {{cloze:N:}}, {{FrontSide}})
  → deterministic GUIDs, sfld/csum computation
  → SQLite collection.anki2 (schema v11) + media manifest
  → .apkg (ZIP) file
```

### Data flow (import)

```
.apkg file
  → ZIP validation (must contain collection.anki2)
  → deck hierarchy preserved (Japanese::N5::Kanji → nested Kaiteyo decks)
  → notes → cards (ord preserved), tags, scheduling
  → FSRS scheduling mapping (type/queue → SRS, interval, ease, reps, lapses, due)
  → media extracted + references repaired (stored under ~/.kaiteyo/anki-media on desktop)
  → HTML sanitized (scripts/styles/event-handlers/javascript: URIs stripped)
```

### Compatibility limits (honest)

- Exact template styling, typing-mode cards, and cram scheduling are **not** reproduced —
  imported content is rendered to safe plain text with sanitized HTML kept in the note field.
- Scheduling is an approximation: Anki queue/type map onto Kaiteyo SRS state; ease is a
  decimal from permille; due dates are epoch-day approximations.

### Failure handling

- Non-ZIP or missing-database packages fail with clear, actionable error messages.
- Rollback-safe: operations use temporary files; a failed import never corrupts existing data.

## AnkiConnect (desktop suite)

### Purpose

Push mined cards straight into Anki and import whole decks from Anki — both over Anki's
own local HTTP API (AnkiConnect).

### Architecture

- `desktopApp/.../engine/mining/AnkiConnectTransport.kt` — real AnkiConnect client:
  list/create decks, add Basic notes, tags, base64 screenshot/audio media,
  `canAddNotes` duplicate detection.
- `desktopApp/.../engine/transfer/AnkiImporter.kt` + `AnkiImportMapper.kt` — deck/note/card
  import from AnkiConnect with two-layered duplicate detection (Anki note GUID on
  `DesktopCard.externalId`, plus a content-fingerprint fallback) and user-selectable
  conflict policy (Skip / Update / Duplicate).
- UI: `IntegrationsView` (hub with status cards + "Test connection"),
  `AnkiImportDialog` (live deck preview, deck + policy selection, progress, per-deck results).

### Protocol & security

- Speaks AnkiConnect's HTTP JSON-RPC on the user's local Anki port (localhost only).
- No credentials involved; trust is implicit to the local machine.
- Mined-card forwarding is opt-in (`media.anki.*` settings).

### Version compatibility

- AnkiConnect is an external tool (https://github.com/FooSoft/anki-connect); compatibility
  is tested against current AnkiConnect versions. Degrades gracefully when Anki is not
  running (failed requests surface as actionable UI errors).

## Development notes

- Tests: `core/src/commonTest/.../transfer/ImportPipelineTest.kt`,
  `TransferCodecsTest.kt`, `core/src/jvmTest/.../transfer/AnkiPackageJvmTest.kt`,
  `desktopApp/src/jvmTest/.../transfer/AnkiImportMapperTest.kt`.
- AnkiConnect import requires a live Anki instance to verify end-to-end.
