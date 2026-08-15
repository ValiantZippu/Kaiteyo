# Kaiteyo Architecture — Backup, Import & Export

**Status**: Implemented (core) — real backup verification, real import/export pipeline
**Owner**: core `transfer` + `backup` + `BackupRoute`/`BackupScreen`
**Related**: `docs/architecture/database.md` · `docs/architecture/study-engine.md` ·
`docs/integrations/ANKI.md`

## 1. Purpose

User data is user-owned (§205): study history, knowledge state, media history, notes,
decks, collections must be exportable, backupable, restorable and deletable. Backups are
versioned; exports never depend on internal table layout (§207). Restores never silently
overwrite newer data — preview + conflict policies first.

## 2. Backup architecture (core, shipped)

### Interfaces (`core/.../backup/`)
- `BackupManager` — `backupTo(location)`, `readInfoFrom(location)`, `restoreFrom(location)`
  (suspend; platform file based).
- `BackupArchiveHandler` — `writeBackupZip`, `readZipBackupInfo`, `readBackupZip`
  (expect/actual per platform).
- `BackupArchiveSchema` — the **versioned backup format** (schema constant; restores
  validate against it).
- `BackupInfo` — archive metadata (version, created, counts).
- `BackupRestoreObservable` — completion/event notifier for the UI.

### JVM implementation (`JvmBackupArchiveHandler`)
ZIP archive writer/reader with JSON entries (`writeJsonFile`, `writeFile`), `withZipEntry`
helper; `readZipBackupInfo` reads metadata without full extraction.

### UI (`BackupScreen` / `BackupRoute`)
States: Idle, Loading, UninterruptibleLoading, Error, RestoreConfirmation,
ActionCompleted. `BackupFilePicker` returns `Picked(file)`/`Canceled`;
`getDefaultBackupFileName()`. Restore is gated on an explicit confirmation.

### Honest verification (§325)
`BackupVerifier` (in `BackupSystemExt.kt`, shared by the dead hub path — see §6):
- `verifyChecksum(content, expected)` — **real pure-Kotlin SHA-256** (`Sha256` object in
  commonMain-safe code; JVM `MessageDigest` is unavailable there), verified against FIPS
  test vectors; reports mismatch/read failures honestly — no fabricated "Backup integrity
  verified".
- `verifyDatabaseIntegrity` — reports honestly that it needs the app DB handle.
- `estimateCompressionRatio` — documented as an expectation, not a measurement.
- `backup_metadata` table (filename, size, checksum, automatic flag, notes) persists the
  real checksums.

## 3. Import/export pipeline (core, shipped)

### Formats & policies (`core/.../transfer/ImportExportPipeline.kt`)
- `TransferFormat { Json, Csv, Tsv, Txt, Apkg }`; `ExportFormat` mirrors it.
- `ConflictPolicy { KeepExisting, OverwriteExisting, Skip, KeepNewest }` —
  merge behavior on import.
- `DuplicatePolicy { Skip, Replace, CreateCopy }`.
- `ValidationSeverity { Info, Warning, Error }`; `ValidationIssue`;
  `ImportPreview` (parsed, validated, deduped preview with counts);
  `ImportResult` (applied counts + issues).
- `ImportPipeline` — `preview(text|bytes, format)` (parse + validate + duplicate
  detection via `findDuplicates`), `validateCard`, `apply(policy)` (persisted merge
  through `mergeImportedCards()` → FSRS scheduling).
- `ExportPipeline` — `serialize(bundle, format)` from `ExportBundle`.

### ViewModel (`ImportExportViewModel`)
Flow: `loadCards` → `previewImport` → `applyImport(policy)` | `export(config)` /
`exportToFile(config, fileName)` → `dismissPreview`/`clearError`. States: Idle, Loading,
Preview, Exporting, Importing, Success, Error. `ExportConfig` selects cards (all/deck/
filter). UI: `ImportExportScreen` (Import tab, Export tab, PasteField, PreviewStat,
IncludeToggle, StatusLine, ErrorLine, SuccessLine).

### Anki `.apkg` codec (`core/.../transfer/AnkiPackage*`)
expect/actual per platform:
- `write(cards, deckName): Result<ByteArray>` / `read(bytes): Result<List<KaiteyoCard>>`.
- JVM: builds the real Anki SQLite database (collection, notes, cards, models), zips it,
  and reads it back — `ankiType/ankiQueue/statusFromAnki` mapping, `cardGuid`, `checksum`
  (Anki's checksum), `ankiDue`; `renderTemplate` + `sanitizeHtml`/
  `sanitizeToPlainText` (HTML sanitization on import).
- Android: via `SQLiteDatabase`; iOS: dependency-free pure-Kotlin ZIP/inflate.
- Scheduling mapping, media extraction. iOS/Android runtime verification pending
  (BLOCKED).

## 4. Suite layer

- `ImportExportEngine` (learning) — full-fidelity JSON snapshot + CSV/TSV with dedupe and
  validation (`docs/architecture/study-engine.md` §8).
- Desktop `.apkg` import/export after rewrite + card-pool persistence are on the BLOCKED
  runtime-sweep list.
- The dead `BackupSystemExt` path (`BackupManagerScreen` inside `LearningPowerHub`) is a
  **removal candidate** (audit §5.2) — real suite backup uses the same philosophy as core.

## 5. Error model

- Restore never silently overwrites newer data — preview + conflict policies first.
- Checksum mismatch / unreadable archive → explicit failure with guidance (§296), never
  a crash.
- Platform file access via pickers/permissions (§273): Android SAF with persistable
  grants, iOS document picker, JVM dialogs.

## 6. Dead code note

`BackupSystemExt.kt` is reachable only through the dead `LearningPowerHub`; its
`BackupManagerScreen` callbacks were empty (`onCreateBackup = {}`, etc.). The real backup
UI is `BackupRoute`/`BackupScreen`. Remove the dead path as part of the audit cleanup
(§352) — the `BackupVerifier` SHA-256 code it hosts should be preserved/moved if removed.

## 7. Tests

- `BackupVerifier` SHA-256 verified against FIPS vectors + 200 random buffers vs node
  crypto; transfer pipeline tests in core (`:core:allTests`).
- Gaps: migration tests for backup format evolution (§217), corrupt-archive restore
  tests, large-dataset export tests (§280), Anki .apkg round-trip at scale.

## 8. Open items

- Automatic backup scheduling + retention (metadata schema exists; `is_automatic` field
  unused by UI).
- Suite backup consolidation after the data-layer decision (audit §7-1).
- Cloud backup is intentionally out of scope (sync is provider-based, ADR-0009).

## 9. Node-layer backup scope (TARGET — ADR-0013, NODE §144, STANDARDS §205–§206)

Backups expand to cover the node layer and world state:

- **Included**: `node`/`edge` stores, `user_knowledge` + `knowledge_transition`,
  `event_log` (or the last N months per retention policy), world saves (§144),
  collections, photos (user-selected), and existing decks/cards/history/settings.
- **Excluded**: world package content (re-downloadable, §145) and app-data assets
  (bundled) — backups stay lean and restore fast.
- **Format**: versioned backup envelope with per-section manifests; the existing
  `BackupVerifier` SHA-256 discipline extends to every section (real checksums, honest
  reports).
- **Restore integrity**: transactional restore — a partial restore (nodes restored,
  events truncated) is detected and rejected/repaired, never silently inconsistent
  (database.md §9.3).
- **Versioning**: backup schema version independent from app version (§338); older
  backups migrate forward, never silently drop sections.
