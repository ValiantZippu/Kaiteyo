# 🔌 api — Kaiteyo API Documentation

This directory documents the internal APIs and data formats used by Kaiteyo.

## Contents

This folder currently contains this index only. Individual API references
(DATABASE, SETTINGS, SYNC, IMPORT_EXPORT) are planned; until they are written,
see the architecture documents (`../architecture/`) and the SQLDelight schemas
in `../core/` for the authoritative details.

## Design Principles

1. **Offline-first** — All features work without internet
2. **Local storage** — SQLDelight for structured data, DataStore for preferences
3. **JSON for interchange** — Import/export uses standard JSON
4. **Backward compatible** — Schema changes must include migration paths
