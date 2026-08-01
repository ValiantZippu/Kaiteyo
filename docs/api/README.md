# 🔌 api — Kaiteyo API Documentation

This directory documents the internal APIs and data formats used by Kaiteyo.

## Contents

| File | Purpose |
|------|---------|
| `DATABASE.md` | Database schema, tables, queries |
| `SETTINGS.md` | Settings/preferences system |
| `SYNC.md` | Sync protocol and data format |
| `IMPORT_EXPORT.md` | Import/export formats and procedures |

## Design Principles

1. **Offline-first** — All features work without internet
2. **Local storage** — SQLDelight for structured data, DataStore for preferences
3. **JSON for interchange** — Import/export uses standard JSON
4. **Backward compatible** — Schema changes must include migration paths
