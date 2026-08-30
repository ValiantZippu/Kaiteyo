# Kaiteyo — Platforms

> **Status**: `PARTIALLY_IMPLEMENTED` (GitHub sync transport) + `ARCHITECTED` (platform adapter model).
> Companion: `core.md`, `data-model.md`, `events.md`, `browser.md`.

## 1. What it is

An adapter layer for external services — **AniList, MyAnimeList (MAL), IMDb, future services, supported anime/manga sites, custom web pages** — that distinguishes three very different things the original spec conflated:

| Kind | Example | Sync model | Data | Write access |
|------|---------|------------|------|--------------|
| **Data Platform** | AniList, MAL | Account + OAuth, list/progress sync | metadata, watch progress, lists, ratings, history | Optional (user opts in) |
| **Web Source** | anime website (supported adapter) | No account; scraped/source-specific | streaming URL, episode list | No |
| **Generic Web Page** | IMDb | Read-only metadata (via browser) | title, year, cast | No |

IMDb must not be forced into the same write-sync model as AniList/MAL if its APIs do not support it.

## 2. Why it exists

Users discover on platforms, consume in Kaiteyo, and optionally keep platform lists in sync — without duplicating Content rows (data-model.md).

## 3. What the user does

- Link account (AniList/MAL) → OAuth (loopback or device flow) → token stored encrypted.
- Browse/search platform → import title as `Content` (resolveOrCreate with `ExternalIds{anilist,mal}`).
- Optionally sync watch/read progress (pull/push, gated by setting).
- View platform record in Kaiteyo as a `SourceBinding` on the unified `Content` (not a second record).

## 4. Data model

```kotlin
data class PlatformAccount(
    val id: String, // "anilist:12345"
    val platformId: String, // "anilist"
    val username: String,
    val token: EncryptedString, // never logged
    val scopes: Set<String>,
    val lastSyncAt: Instant?,
    val syncEnabled: Boolean,
)

data class PlatformRecord(
    val externalId: String, // AniList id as string
    val platformId: String,
    val kind: ContentKind,
    val title: String,
    val progress: ContentProgress?,
    val rating: Int?,
    val listStatus: String?, // Watching, Completed, Paused, Dropped, Planning
    val externalIds: ExternalIds,
    val rawJson: JsonObject?, // cached raw response for debugging
)

interface PlatformAdapter {
    val id: String // "anilist", "mal", "imdb"
    val kind: PlatformKind // DataPlatform | WebSource | GenericWebPage
    suspend fun authenticate(): Result<PlatformAccount> // OAuth
    suspend fun search(query: String): List<PlatformRecord>
    suspend fun fetchRecord(externalId: String): Result<PlatformRecord>
    suspend fun pullProgress(account: PlatformAccount): List<PlatformRecord>
    suspend fun pushProgress(account: PlatformAccount, contentId: ContentId, progress: ContentProgress): Result<Unit>
    val supportsPush: Boolean // false for IMDb/generic
    val requiresAuth: Boolean
}

enum class PlatformKind { DataPlatform, WebSource, GenericWebPage }
```

Capabilities matrix (per adapter):

| Capability | AniList | MAL | IMDb | Web Source |
|------------|---------|-----|------|------------|
| Auth | OAuth PKCE | OAuth PKCE | None | None |
| Metadata | ✅ | ✅ | ✅ (browser/metadata API) | adapter-specific |
| Progress | ✅ pull/push | ✅ pull/push | ❌ | ❌ |
| Lists/ratings | ✅ | ✅ | ❌ | ❌ |
| History | ✅ | ✅ | ❌ | ❌ |
| Web-only access | — | — | ✅ | ✅ |

## 5. Ownership & flow

| Concern | Owner |
|---------|-------|
| Auth, token storage, account list | PlatformService |
| Search/fetch via adapters | PlatformService |
| Content resolution/dedup | ContentService (`resolveOrCreate` + `ExternalIds` match) |
| Progress sync (pull/push, queue) | PlatformService → ContentService (+ SyncService queue for offline) |
| UI (link, search, import, sync status) | Platforms screen (consumer) |

```
PlatformService.search("Violet Evergarden", adapter="anilist")
  → PlatformRecord{ externalId="21827", externalIds{anilist=21827, mal=33352} }
  → ContentService.resolveOrCreate(hint=title, externalIds) → Content (one row)
  → SourceBinding(WebSource? no — DataPlatform binding with externalIds)
```

Second search on MAL returning same `mal=33352` resolves to the **same** Content (dedup by externalId union).

## 6. Sync rules

- Pull: periodic (setting, default 1h) + manual "Sync now"; merges into `ContentProgress` (lastWriteWins by `updatedAt`).
- Push: opt-in per account (`syncEnabled`); gated by `supportsPush`; offline → queue in `SyncService` (retry with backoff); failure surfaced as "Sync queued — offline".
- No duplicate Content rows: `UNIQUE(externalIds.anilist)` + `UNIQUE(externalIds.mal)` enforced at resolution, with merge suggestions on conflict.

## 7. Persistence / caching / offline

- `platform_account` (encrypted token), `platform_record` (cached JSON + TTL), `platform_sync_state` (lastSync, queue).
- TTL: metadata 24h, progress 1h (per adapter).
- Offline: cached records + progress visible; sync queues; search unavailable (Offline state with retry).

## 8. Failure states

Auth cancelled/expired (re-auth prompt), rate-limit (backoff + "Try again in X"), network timeout (retry), unsupported operation (e.g., push to IMDb — disabled UI with tooltip), externalId removed upstream (binding marked stale), malformed response (log + Debug).

## 9. Permissions / security

OAuth PKCE (loopback `127.0.0.1` or device flow on platforms without redirect), token encrypted at rest (OS keystore where available), never logged, revoke on disconnect. WebSource adapters never receive tokens. Browser isolation per `browser.md`.

## 10. Evolution

New platform → implement `PlatformAdapter`, add icon + settings entry, no Content model change. New capability → add method with default no-op (generic pages stay read-only).
