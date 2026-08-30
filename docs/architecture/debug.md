# Kaiteyo — Debug System

> **Status**: `IMPLEMENTED` (overlay + command palette) + `ARCHITECTED` (full panel).
> Companion: `events.md`, `core.md`, `navigation.md`.

## 1. What it is

A developer/debug architecture where every screen identifies itself and the debug panel surfaces the full runtime state for bug reports and local diagnosis.

## 2. What exists

- `PageIdentity` / `ProvidePageIdentity` / `PageRegistry` + `KaiteyoDebugOverlay` (bottom-corner: Page/Route/Panel + "copy debug info" — version + theme + nav mode), gated by Settings → Developer "Show page debug info".
- `CommandPaletteOverlay` (suite, `Ctrl+K`) — every destination reachable, plus diagnostic actions.
- `ActivityTracker` (engagement state).

## 3. Target panel (must be built)

Every screen identifies itself (`ProvidePageIdentity { label = "Media > Episode Player" }`), and the Debug panel (Settings → Developer) shows:

| Section | Content |
|---------|---------|
| Screen | current screen label + route + component |
| State | ViewModel StateFlow snapshot (sanitized) |
| Errors | last errors per service (media/dictionary/mining/sync) |
| Network | online/offline, sync queue size, last sync, platform token status (no token values) |
| Database | AppData version, UserData version, row counts, last migration, integrity |
| Media session | `MediaSession` (item, playbackState, active subtitle track, syncOffsetMs) |
| Subtitle session | attached tracks, provider results, cached associations |
| Dictionary session | enabled dictionaries, last lookup, index status |
| Mining session | last `MiningContext`, last `MinedRecord`, Anki queue size, next retry |
| Reading session | documentId, chapter/page, highlights/bookmarks count |
| Game session | world, region, quest state, save slot (if active) |

Actions: Copy debug info (version, theme, nav mode, screen, last errors) to clipboard — so reports say `Screen: Media > Episode Player` not "that one screen".

## 4. Rules

- Debug never crashes the app (all reads `runCatching`).
- No PII/tokens in copy output.
- Gated by developer toggle; not shown to end users by default.

## 5. Evolution

New service → new Debug section (one read hook). No app-owned debug state.
