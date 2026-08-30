# Kaiteyo — MyAnimeList (MAL) Integration

> **Status**: `ARCHITECTED` — see `docs/architecture/platforms.md`.

## 1. Upstream

- API: https://myanimelist.net/apiconfig/references/api/v2 (REST + OAuth 2.0 PKCE)
- Data: anime/manga metadata, watch/read progress, lists, ratings, history.

## 2. Kaiteyo adapter

Implements `PlatformAdapter` (`id="mal"`, `kind=DataPlatform`, `supportsPush=true`).

- **Auth**: OAuth PKCE (same flow as AniList).
- **Pull/push/search**: REST equivalents of the AniList adapter.
- **Dedup**: same `ExternalIds{mal}` → `Content` union logic as AniList.

## 3. Differences from AniList

- Rate limits stricter; fewer fields per request (paging required).
- GraphQL vs REST — adapter encapsulates; callers see same `PlatformRecord` shape.

## 4. Evolution

Same as AniList — new MAL field → adapter mapping extension.
