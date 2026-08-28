# Media Center — Migration

> Status: IMPLEMENTED — no schema changes, no data loss

## Scope

No `AppDataDatabase`/`UserDataDatabase` SQLDelight `.sq` changes (never change per AGENTS.md). All media state is file-based JSON under `~/.kaiteyo/`:

| Store | Path | Format | Migration |
|---|---|---|---|
| Library | `~/.kaiteyo/media/library.json` (`LibraryDto`) | JSON | `runCatching` → empty on corrupt, no silent destroy |
| Media state | `~/.kaiteyo/media-state.json` (`MediaStateDto` + `PlaybackQueueDto` `queue.json`) | JSON | same |
| Stats | `~/.kaiteyo/media/stats.json` (`MediaStatsDto`) | JSON | same, 1s coalesce |
| Mining | `~/.kaiteyo/mining-state.json` (`MiningStateDto`) | JSON | same, `pendingExports` idempotent |
| Events | `~/.kaiteyo/event_log.json` (`List<EventRecord>`) | JSON | same |
| Browser | `~/.kaiteyo/browser-state.json` (`BrowserStateDto`) | JSON | same |
| Downloads | `~/.kaiteyo/downloads/*` files + in-memory `DownloadJob` | files | filename sanitized, traversal blocked |

## B1 prototype removal

- Deletes `core/.../MediaCentreScreen.kt.bak` and replaces prototype `MediaCentreScreen.kt` (40 fakes) with honest placeholder.
- **Touches no user data:** `~/.kaiteyo/media/*` untouched; `LibraryStore` decks/cards preserved.
- Koin override unchanged (`desktopApp/Main.kt` `single<MediaCentreContent> { DesktopMediaCentreContent }`).

## Graph indexing

`MediaNodeGraph` is derived (lazy), not persisted — re-index is version-safe, no migration needed. Old `miningEvents`/`bookmarks` still seed graph via `addMiningEvent`/`addBookmark`.

## Safety

- Never silently destroy user decks/history/dictionary/settings/node data.
- Media-specific broken data (prototype tracks) had no persistence, so no migration needed.
- All reads/writes `runCatching`; corrupt snapshots rebuild from empty log (EventLog.reset).
