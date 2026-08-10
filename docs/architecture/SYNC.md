# Kaiteyo Synchronization Architecture v1.2

## Overview

Kaiteyo's synchronization system is built on a modular, offline-first architecture. Every module is independently replaceable, allowing future cloud providers to be added without redesigning the core system.

## Architecture Layers

```
┌─────────────────────────────────────────────┐
│              Authentication Layer            │
│   GitHub OAuth · WebDAV · Self-Hosted · etc  │
└──────────────────────┬──────────────────────┘
                       │
┌──────────────────────▼──────────────────────┐
│              Cloud Provider Layer            │
│     GitHub Gist · WebDAV · Dropbox · etc     │
└──────────────────────┬──────────────────────┘
                       │
┌──────────────────────▼──────────────────────┐
│              Sync Engine Layer               │
│   Upload · Download · Versioning · Queuing   │
└──────────────────────┬──────────────────────┘
                       │
┌──────────────────────▼──────────────────────┐
│            Conflict Resolver Layer            │
│  Keep Local · Keep Remote · Merge · Fields   │
└──────────────────────┬──────────────────────┘
                       │
┌──────────────────────▼──────────────────────┐
│              Encryption Layer                │
│    AES-256 · Checksum · Integrity Verify     │
└──────────────────────┬──────────────────────┘
                       │
┌──────────────────────▼──────────────────────┐
│           Background Task Manager            │
│     Auto Sync · Scheduled · Event-driven     │
└──────────────────────┬──────────────────────┘
                       │
┌──────────────────────▼──────────────────────┐
│         Local Database Adapter               │
│    SQLDelight · Versioning · Change Tracking  │
└─────────────────────────────────────────────┘
```

## Data Model

Every synchronized object includes:

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique identifier |
| `version` | Long | Incrementing version number |
| `createdAt` | Long | Creation timestamp (epoch ms) |
| `modifiedAt` | Long | Last modification timestamp |
| `lastSyncedAt` | Long | Last sync timestamp |
| `deviceId` | String | Originating device |
| `conflictStatus` | ConflictStatus | Local/Remote/Both/Merged |
| `isDeleted` | Boolean | Soft delete flag |
| `checksum` | String | Data integrity hash |

## Sync Object Types

- `Card` - Individual study card
- `Deck` - Card collection
- `Tag` - Card tag
- `Setting` - Application setting
- `Theme` - Theme configuration
- `Layout` - UI layout
- `Bookmark` - User bookmark
- `Dictionary` - Custom dictionary entry
- `Shortcut` - Keyboard shortcut
- `Profile` - User profile

## Sync Flow

1. **Check Authentication** - Validate and refresh tokens
2. **Upload Local Changes** - Send new/modified objects
3. **Get Remote State** - Fetch remote checksum and version
4. **Download Remote Changes** - Pull objects newer than last sync
5. **Resolve Conflicts** - Detect and resolve version conflicts
6. **Apply Changes** - Write resolved operations to local DB
7. **Update State** - Record sync completion and statistics

## Conflict Resolution Strategies

| Strategy | Behavior |
|----------|----------|
| Keep Local | Discard remote changes, keep local version |
| Keep Remote | Overwrite local with remote version |
| Merge | Combine fields, prefer non-empty values |
| Keep Newest | Keep whichever was modified most recently |
| Choose Fields | User selects individual field values |
| Always Local | Always prefer local (never overwrite) |
| Always Remote | Always prefer remote (overwrite local) |
| Ask Each Time | Prompt user for each conflict |

## GitHub OAuth Implementation

### Device Flow (Desktop)

1. App requests device code from GitHub
2. GitHub returns `user_code` and `verification_uri`
3. App displays code to user
4. User visits `github.com/login/device` and enters code
5. App polls for token every interval seconds
6. On success, receives `access_token` and `refresh_token`
7. Tokens are encrypted and stored securely

### Token Security

- Tokens encrypted with platform-specific key
- Never stored in plain text
- Automatically refreshed when expired
- Revoked on sign out
- Never exposed to UI or logs

## Multi-Profile System

Each profile maintains completely separate:
- Decks and cards
- Statistics and history
- Settings and preferences
- Themes and layouts
- Keyboard shortcuts
- Bookmarks
- Custom dictionaries
- Review progress

## Backup System

### Backup Types
- **Automatic Local** - Scheduled based on frequency
- **Manual Local** - User-initiated
- **Cloud Backup** - To sync provider

### Backup Contents
- Profile data
- Deck and card data
- Review history
- Settings and preferences
- Themes and layouts
- Keyboard shortcuts
- Bookmarks

### Backup Features
- Compression
- Integrity checking (checksum)
- Version tracking
- Restore point creation
- Backup verification
- Automatic cleanup (keeps last 50)

## Device Management

Devices are tracked with:
- Unique device ID
- Platform (Desktop/Laptop/Tablet/Phone)
- Application version
- Last online timestamp
- Last sync timestamp
- Trust status

## Error Recovery

| Error Type | Recovery Action |
|------------|----------------|
| Network Interruption | Retry after 5s |
| Token Expiration | Auto-refresh token |
| Server Error | Retry after 10s |
| Rate Limit | Retry after 60s |
| Partial Upload | Resume upload |
| Partial Download | Resume download |
| Conflict | Resolve with strategy |
| Corrupted Data | Skip object |
| Interrupted Sync | Restart sync |

## Security Model

- OAuth tokens encrypted at rest
- No credentials stored in plain text
- All API responses validated
- Checksum verification for all data
- Rate limiting protection
- Token refresh before expiration

## Offline Mode

- All functionality works without connectivity
- Changes queued locally
- Auto-sync when connection restored
- Never interrupt studying

## Future Provider Support

The architecture supports adding providers without core changes:

1. Implement `CloudProvider` interface
2. Add `AuthProvider` variant
3. Register in provider factory
4. Add UI option

### Planned Providers
- GitHub (current)
- WebDAV
- Self-Hosted Server
- Dropbox
- Google Drive
- OneDrive
- iCloud
- S3-Compatible Storage