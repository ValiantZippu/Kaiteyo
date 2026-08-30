# Kaiteyo — AniList Integration

> **Status**: `ARCHITECTED` (adapter interface) — see `docs/architecture/platforms.md`.

## 1. Upstream

- API: https://anilist.gitbook.io/anilist-apiv2-docs/ (GraphQL, OAuth 2.0 PKCE)
- Data: anime/manga metadata, watch/read progress, lists (Watching/Completed/Paused/Dropped/Planning), ratings, history.

## 2. Kaiteyo adapter

Implements `PlatformAdapter` (`id="anilist"`, `kind=DataPlatform`, `supportsPush=true`, `requiresAuth=true`).

- **Auth**: OAuth PKCE (loopback `127.0.0.1:{port}/callback` on desktop; device flow on mobile where loopback unavailable), token encrypted at rest.
- **Pull**: `pullProgress(account)` → GraphQL `MediaListCollection` → `PlatformRecord[]` → `ContentService` (dedup by `ExternalIds{anilist}`).
- **Push**: `pushProgress(account, contentId, progress)` → `SaveMediaListEntry(mutation)` — opt-in per account (`syncEnabled`), queued offline via `SyncService`.
- **Search**: `search(query)` → `Page { media(search:)` → `PlatformRecord[]`.

## 3. Content dedup

One `Content` row may have both `ExternalIds{anilist, mal}`. AniList and MAL records for the same work resolve to same `Content` via externalId union — not two rows.

## 4. Failure / rate limit

GraphQL `429` → backoff (60s → 5m → 30m, capped); token 401 → re-auth prompt; malformed response → log + Debug + retry once.

## 5. Evolution

New AniList field → adapter mapping extension; no Content schema break.
