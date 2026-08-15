# 🗄️ database — Data Layer

This section indexes the database documentation. The full database specification lives
in `docs/architecture/database.md`; the target node-layer storage contract is
`docs/architecture/nodes/NODE_DATA_MODEL.md`.

| Document | Purpose | Status |
|---|---|---|
| [`MIGRATIONS.md`](MIGRATIONS.md) | Migration policy: rules, workflow, destructive-change process, rollback | Current |
| `docs/architecture/database.md` | Two SQLDelight DBs (AppData/UserData), DataStore preferences, suite JSON stores | Current |
| `docs/architecture/nodes/NODE_DATA_MODEL.md` | Target node/edge/knowledge/event/save schema | Target (ADR-0013) |
| `docs/data/ARCHITECTURE.md` | Where data lives, dataset ingestion, caching | Current |
| `docs/architecture/backup.md` | Backup, import/export | Current |

## Facts (verified)

- `AppDataDatabase` — bundled read-only dictionary asset (versioned, `AppDataDatabaseVersion = 15`), kjd-generated.
- `UserDataDatabase` — mutable user data with **15 versioned migrations**
  (`core/src/commonMain/sqldelight_user_data/migrations/1.sqm … 15.sqm`) plus
  enhancement/statistics `.sq` files.
- Desktop suite additionally uses JSON stores (`~/.kaiteyo/`) — consolidation target
  (ADR-0017, KT-DB-004).
