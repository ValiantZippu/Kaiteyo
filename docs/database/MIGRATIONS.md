# Database Migration Policy

> **Status**: policy `CURRENT` (applies to the shipped `UserDataDatabase` and the kjd
> patch feeds); the node-layer additions (ADR-0013) must follow the same policy.

## 1. Rules (MASTER §74, STANDARDS §180)

1. **Every schema change is versioned.** No silent DDL. SQLDelight migrations are
   numbered files (`core/src/commonMain/sqldelight_user_data/migrations/N.sqm`) applied
   in order. Current: **15 migrations**.
2. **Never silently destroy user data.** If a change requires dropping/rewriting user
   data, the migration must (a) be flagged destructive, (b) back up first, (c) require a
   user-facing explanation where appropriate.
3. **Preserve compatibility.** Forward migrations only; an app version never opens a
   database schema newer than itself. Old app + new DB → clear "update the app" message,
   never a crash.
4. **Backup before destructive migration.** The app's backup system
   (`docs/architecture/backup.md`) is the safety net; destructive migrations trigger an
   automatic pre-migration backup when feasible.
5. **Validate migrations.** Every migration ships with a test: fresh-install at N,
   upgrade from N→N+1, and rollback where the engine supports it (STANDARDS §217).
6. **Rollback where possible.** Application-level rollback (restore backup) always;
   database-level rollback when the migration is reversible (keep the inverse script for
   destructive changes).
7. **Provenance.** The `schema_version`/PRAGMA user_version is recorded and checked on
   startup; corruption is detected before migration (integrity check), never migrated
   blindly.

## 2. Migration workflow

```
1. Change .sq schema files (+ any new .sqm)
2. Add migration N+1 (and N+2 if an upgrade path is needed)
3. Regenerate SQLDelight interfaces (per AGENTS.md commands)
4. Write/update migration tests (upgrade from every supported version)
5. Update docs/architecture/database.md + this file
6. Add a CHANGELOG entry if user-visible
```

## 3. Destructive change process (extra gate)

For any migration that removes columns/tables or rewrites user data:

1. **Impact review** — what data is affected, how users are told.
2. **Backup gate** — automatic pre-migration backup or explicit user confirmation.
3. **Data plan** — map/transform/export old data; never silently drop unless the data is
   provably reconstructible.
4. **Rollback plan** — restore path documented and tested.
5. **ADR if architectural** — schema changes that alter the data model's meaning get an
   ADR (MASTER §7).

## 4. Read-only app data (AppDataDatabase)

The bundled dictionary database is **immutable per version** — updates ship as a new
asset version (`AppDataDatabaseVersion`, declared in `buildSrc/AppAssets.kt`) and, on
desktop, as **incremental patch feeds** (kjd base + delta applied at runtime). Patches
are validated before apply and roll back on failure (never corrupts the base).

## 5. Data migrations beyond SQL

- **Settings**: DataStore keys have a documented migration path (suite `SettingsEngine`
  already migrates legacy keys; known vocabulary drift tracked — PRODUCT_AUDIT §5.4).
- **Suite JSON → unified store** (ADR-0017 target): migration must be additive and
  one-way-verified, with a backup of the JSON before first conversion.
- **Save files** (Journey target): `docs/game/save-system.md` §Versioning.
- **Content packages** (ADR-0015): package version + schema version in the manifest;
  forward-compatible readers required.

## 6. Testing matrix (STANDARDS §217)

| Case | Test |
|---|---|
| Fresh install at latest | ✅ core tests |
| Upgrade from every supported version | migration tests (extend with each new .sqm) |
| Upgrade with user data present | fixture data preserved assertions |
| Downgrade (new app → old DB impossible) | guard test: clear error |
| Corrupt DB before migration | integrity check → recovery path |
| Destructive migration | backup gate + rollback test |
| kjd patch apply failure | patch rollback test (base intact) |

## Related

- Database spec: `docs/architecture/database.md`
- Backup: `docs/architecture/backup.md`
- kjd data updates: `docs/architecture/DATA_PLATFORM.md`
- Node-layer storage (target): `docs/architecture/nodes/NODE_DATA_MODEL.md`
- Journey saves: `docs/game/save-system.md`
- ADR-0005 (two databases)
