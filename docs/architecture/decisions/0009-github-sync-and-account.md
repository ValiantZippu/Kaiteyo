# ADR-0009: GitHub Device-Flow OAuth + Private-Gist Sync (No Central Service)

**Status**: Accepted

## Context

Kaiteyo needed cross-device study-data sync without running a central server — the
project has no backend, no user database, and no budget for one. Privacy and
"your data is yours" are core values.

## Decision

- **Account = GitHub OAuth (device flow)**, used only to authorize sync. No Kaiteyo-hosted
  accounts, passwords, or sessions.
- **Sync transport = the user's private GitHub gist**: study data is serialized as a
  backup snapshot and uploaded to a private gist the app creates; sync state tracks
  local/remote versions.
- Implementations: shared OAuth + cloud provider in `core` (`GitHubOAuth`,
  `GitHubCloudProvider`, `SyncEngine`), desktop transport in
  `desktop/engine/sync/` (`GitHubGistSyncTransport`, `CloudSyncCoordinator`) with a
  `SyncBackupFileProvider` per platform.
- Mobile: sync provider APIs exist (`SyncBackupFileProvider` actuals); the full sync UI is
  desktop-first (the desktop suite's Sync view).

## Alternatives

- Self-hosted sync server — rejected: no backend, cost/complexity, privacy surface.
- WebDAV — rejected (placeholder was removed): less standard auth, weaker mobile story.
- Cloud storage SDKs (Dropbox/Google Drive) — considered; GitHub gist chosen for zero
  additional accounts and OAuth reuse.

## Consequences

- No central attack surface; privacy is bounded by the user's GitHub account.
- Sync data is not end-to-end encrypted (private gist only) — documented in
  `security/PRIVACY.md`.
- Sync is opt-in and desktop-oriented; mobile sync UX is partial.

## Implementation notes

- `core/.../sync/`, `core/.../account/GitHubOAuth.kt`
- `desktop/engine/account/` (`GitHubDeviceFlowClient`, `TokenVault`) and
  `desktop/engine/sync/`
- See `architecture/SYNC.md` and `architecture/ACCOUNT.md`.
