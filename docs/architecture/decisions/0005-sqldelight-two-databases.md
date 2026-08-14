# ADR-0005: Two SQLDelight Databases (immutable app data / mutable user data)

**Status**: Accepted

## Context

Kaiteyo stores two fundamentally different kinds of data: a large **immutable language
database** (kanji, kana, vocabulary, radicals — bundled with the app) and **mutable user
study data** (decks, cards, SRS state, history, statistics). Mixing them creates migration
pain (the bundled data changes on every release) and bloat (the dictionary is ~100MB).

## Decision

- Two SQLDelight databases in `core/`:
  - `AppDataDatabase` — read-only at runtime, packaged as a prepared SQLite asset
    (versioned: `kanji-dojo-data-base-v15.sql`), generated offline by the KJD pipeline.
  - `UserDataDatabase` — mutable, with versioned `.sqm` migrations
    (`core/src/commonMain/sqldelight_user_data/migrations/`).
- `linkSqlite = true`; drivers per platform (Android driver, JVM sqlite driver,
  native driver for iOS).
- Preferences (settings) live outside SQL: DataStore Preferences.

## Alternatives

- One combined database — rejected: release-time asset regeneration would collide with
  user data and migrations; slower app data updates.
- Room/Realm — rejected: SQLDelight gives typed, KMP-safe SQL with codegen and is already
  the upstream choice.

## Consequences

- App data updates are atomic asset swaps; user data migrations are separate and safe.
- Schema changes to user data require explicit `.sqm` migrations (see the "never change"
  list in `development/AI_CONTEXT.md`).
- The desktop app can apply **incremental patch updates** to the bundled database via KJD
  `DatabasePatcher` (no full re-download).

## Implementation notes

- `core/build.gradle.kts` (sqldelight block), `core/src/commonMain/sqldelight_app_data/`,
  `core/src/commonMain/sqldelight_user_data/`.
- App data asset management: `buildSrc/.../AppAssets.kt` + `PrepareAssetsTask`.
