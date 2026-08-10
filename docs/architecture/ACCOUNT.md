# Kaiteyo Account & Synchronization Structure v1.2

## Professional Account Architecture — Cross-Platform Ready

This document defines the complete account structure for Kaiteyo. It is designed to be platform-agnostic — usable from the desktop app, mobile app, website, CLI, or any API consumer.

---

## 1. CORE DATA MODELS

### 1.1 AuthProvider

```typescript
// Represents the authentication provider for an account
interface AuthProvider {
  providerId: 'github' | 'webdav' | 'selfhosted' | 'dropbox' | 'googledrive' | 'onedrive' | 'local';
  displayName: string;
  
  // Provider-specific identifiers
  github?: { userId: string };
  webdav?: { serverUrl: string };
  selfhosted?: { serverUrl: string };
  dropbox?: { userId: string };
  googledrive?: { userId: string };
  onedrive?: { userId: string };
  local?: { profileId: string };
}
```

### 1.2 AuthToken

```typescript
interface AuthToken {
  accessToken: string;        // OAuth access token
  refreshToken: string;       // OAuth refresh token
  expiresAt: number;          // Expiration timestamp (epoch ms)
  tokenType: 'bearer';        // Token type
  scope: string;              // OAuth scope
}
```

### 1.3 KaiteyoProfile

```typescript
interface KaiteyoProfile {
  id: string;                 // Unique profile identifier
  displayName: string;        // User's display name
  username: string;           // Unique username
  avatarUrl: string;          // Avatar image URL
  email: string;              // Email address
  provider: AuthProvider;     // Authentication provider
  joinedAt: string;           // ISO 8601 join date
  lastSyncAt: string;         // ISO 8601 last sync
  isCloudProfile: boolean;    // Whether this is a cloud-synced profile
  storageUsed: number;        // Bytes used
  storageLimit: number;       // Bytes limit
  deviceCount: number;        // Number of connected devices
}
```

### 1.4 LocalProfile

```typescript
interface LocalProfile {
  id: string;                 // Unique profile identifier
  name: string;               // Profile display name
  createdAt: string;          // ISO 8601 creation date
  lastUsedAt: string;         // ISO 8601 last used
  deckCount: number;          // Number of decks
  cardCount: number;          // Number of cards
  isDefault: boolean;         // Whether this is the default profile
}
```

### 1.5 KaiteyoDevice

```typescript
interface KaiteyoDevice {
  id: string;                 // Unique device identifier
  name: string;               // Device display name (e.g., "My Desktop")
  platform: 'Desktop' | 'Laptop' | 'Tablet' | 'Phone' | 'Unknown';
  appVersion: string;         // Application version (e.g., "1.2.0")
  lastOnline: string;         // ISO 8601 last online timestamp
  lastSyncAt: string;         // ISO 8601 last sync timestamp
  isCurrentDevice: boolean;   // Whether this is the current device
  isTrusted: boolean;         // Whether the device is trusted
}
```

### 1.6 SyncableObject

```typescript
// Every synchronized object includes these fields
interface SyncableObject {
  id: string;                 // Unique identifier
  version: number;            // Incrementing version number
  createdAt: number;          // Creation timestamp (epoch ms)
  modifiedAt: number;         // Last modification timestamp (epoch ms)
  lastSyncedAt: number;       // Last sync timestamp (epoch ms)
  deviceId: string;           // Originating device
  conflictStatus: 'None' | 'LocalChanged' | 'RemoteChanged' | 'BothChanged' | 'Merged' | 'Resolved';
  isDeleted: boolean;         // Soft delete flag
  checksum: string;           // Data integrity hash
}
```

### 1.7 SyncObjectType

```typescript
type SyncObjectType = 
  | 'Card'        // Individual study card
  | 'Deck'        // Card collection
  | 'Tag'         // Card tag
  | 'Setting'     // Application setting
  | 'Theme'       // Theme configuration
  | 'Layout'      // UI layout
  | 'Bookmark'    // User bookmark
  | 'Dictionary'  // Custom dictionary entry
  | 'Shortcut'    // Keyboard shortcut
  | 'Profile';    // User profile
```

### 1.8 SyncOperation

```typescript
interface SyncOperation {
  objectId: string;           // Object being operated on
  objectType: SyncObjectType; // Type of object
  operation: 'Add' | 'Modify' | 'Delete' | 'Move' | 'Merge' | 'Rename';
  data: string;               // Serialized object data
  timestamp: number;          // Operation timestamp (epoch ms)
}
```

### 1.9 SyncConflict

```typescript
interface SyncConflict {
  objectId: string;           // Conflicting object
  objectType: SyncObjectType; // Type of object
  localVersion: number;       // Local version number
  remoteVersion: number;      // Remote version number
  localModifiedAt: number;    // Local modification time
  remoteModifiedAt: number;   // Remote modification time
  localData: string;          // Local serialized data
  remoteData: string;         // Remote serialized data
  resolvedData: string;       // Resolved data (after conflict resolution)
  fieldDiffs: FieldDiff[];    // Per-field differences
}
```

### 1.10 FieldDiff

```typescript
interface FieldDiff {
  fieldName: string;          // Field name
  localValue: string;         // Local value
  remoteValue: string;        // Remote value
  isSelected: boolean;        // Whether this field is selected in resolution
}
```

### 1.11 BackupMetadata

```typescript
interface BackupMetadata {
  id: string;                 // Unique backup identifier
  name: string;               // Backup display name
  createdAt: number;          // Creation timestamp (epoch ms)
  size: number;               // Backup size in bytes
  checksum: string;           // Integrity checksum
  version: number;            // Backup format version
  isEncrypted: boolean;       // Whether backup is encrypted
  includesHistory: boolean;   // Whether review history is included
  includesSettings: boolean;  // Whether settings are included
  includesThemes: boolean;    // Whether themes are included
  profileId: string;          // Associated profile
  deviceName: string;         // Device that created the backup
}
```

### 1.12 SyncSettings

```typescript
interface SyncSettings {
  provider: AuthProvider;                     // Sync provider
  autoSync: boolean;                          // Enable automatic sync
  syncFrequency: 'Every5Minutes' | 'Every15Minutes' | 'Every30Minutes' | 'EveryHour' | 'Every6Hours' | 'Manual';
  meteredNetwork: boolean;                    // Allow sync on metered connections
  wifiOnly: boolean;                          // Only sync on Wi-Fi
  conflictStrategy: 'KeepLocal' | 'KeepRemote' | 'Merge' | 'KeepNewest' | 'ChooseFields' | 'AlwaysLocal' | 'AlwaysRemote' | 'AskEachTime';
  autoBackup: boolean;                        // Enable automatic backups
  backupFrequency: 'EveryHour' | 'Daily' | 'Weekly' | 'Monthly' | 'Manual';
  maxBackups: number;                         // Maximum number of backups to keep
  encryptLocalData: boolean;                  // Encrypt local sensitive data
  syncOnAppStart: boolean;                    // Sync when app starts
  syncOnAppResume: boolean;                   // Sync when app resumes
  showSyncNotifications: boolean;             // Show sync notifications
}
```

---

## 2. API ENDPOINTS

### 2.1 Authentication

```
POST   /api/v1/auth/github/device          # Request GitHub device code
POST   /api/v1/auth/github/token           # Poll for GitHub token
POST   /api/v1/auth/refresh                # Refresh authentication token
POST   /api/v1/auth/revoke                 # Revoke authentication token
GET    /api/v1/auth/session                # Get current session info
```

### 2.2 Profile

```
GET    /api/v1/profile                     # Get current profile
PUT    /api/v1/profile                     # Update profile
DELETE /api/v1/profile                     # Delete profile
GET    /api/v1/profile/stats               # Get profile statistics
POST   /api/v1/profile/export              # Export profile data
POST   /api/v1/profile/import              # Import profile data
```

### 2.3 Devices

```
GET    /api/v1/devices                     # List connected devices
POST   /api/v1/devices                     # Register a device
PUT    /api/v1/devices/:id                 # Update device (rename)
DELETE /api/v1/devices/:id                 # Remove a device
POST   /api/v1/devices/:id/sync            # Force sync a device
POST   /api/v1/devices/:id/logout          # Log out a device
```

### 2.4 Synchronization

```
POST   /api/v1/sync/upload                 # Upload local changes
POST   /api/v1/sync/download               # Download remote changes
GET    /api/v1/sync/state                  # Get remote sync state
POST   /api/v1/sync/resolve                # Resolve a conflict
GET    /api/v1/sync/status                 # Get sync status
GET    /api/v1/sync/statistics             # Get sync statistics
```

### 2.5 Backup

```
POST   /api/v1/backup                      # Create a backup
GET    /api/v1/backup                      # List backups
GET    /api/v1/backup/:id                  # Get backup details
POST   /api/v1/backup/:id/restore          # Restore from backup
DELETE /api/v1/backup/:id                  # Delete a backup
GET    /api/v1/backup/:id/verify           # Verify backup integrity
POST   /api/v1/backup/import               # Import a backup
```

### 2.6 Storage

```
GET    /api/v1/storage                     # Get storage information
GET    /api/v1/storage/usage               # Get detailed storage usage
```

---

## 3. OAUTH FLOW (GitHub Device Flow)

```
┌─────────┐         ┌──────────┐         ┌────────┐
│  Kaiteyo │         │  GitHub   │         │  User   │
│   App    │         │   OAuth   │         │        │
└────┬─────┘         └────┬─────┘         └────┬────┘
     │                    │                    │
     │  POST /device/code │                    │
     │  client_id, scope  │                    │
     │───────────────────>│                    │
     │                    │                    │
     │  device_code       │                    │
     │  user_code         │                    │
     │  verification_uri  │                    │
     │<───────────────────│                    │
     │                    │                    │
     │  Display code      │                    │
     │────────────────────────────────────────>│
     │                    │                    │
     │                    │   Visit URL        │
     │                    │   Enter code       │
     │                    │<───────────────────│
     │                    │                    │
     │  Poll for token    │                    │
     │  (every 5s)        │                    │
     │───────────────────>│                    │
     │                    │                    │
     │  access_token      │                    │
     │  refresh_token     │                    │
     │<───────────────────│                    │
     │                    │                    │
     │  Encrypt & store   │                    │
     │  tokens securely   │                    │
     │                    │                    │
```

---

## 4. SYNC FLOW

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Local App   │     │  Sync Engine │     │  Cloud       │
│  (Offline)   │     │              │     │  Provider    │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       │  1. Queue changes  │                    │
       │───────────────────>│                    │
       │                    │                    │
       │  2. Check auth     │                    │
       │                    │───────────────────>│
       │                    │                    │
       │  3. Upload local   │                    │
       │                    │───────────────────>│
       │                    │                    │
       │  4. Get remote     │                    │
       │                    │<───────────────────│
       │                    │                    │
       │  5. Download       │                    │
       │                    │<───────────────────│
       │                    │                    │
       │  6. Resolve        │                    │
       │   conflicts        │                    │
       │                    │                    │
       │  7. Apply changes  │                    │
       │<───────────────────│                    │
       │                    │                    │
       │  8. Update state   │                    │
       │                    │                    │
```

---

## 5. CONFLICT RESOLUTION MATRIX

| Strategy | Local Wins | Remote Wins | Merge | Best For |
|----------|-----------|-------------|-------|----------|
| KeepLocal | ✅ All fields | ❌ | ❌ | When local is authoritative |
| KeepRemote | ❌ | ✅ All fields | ❌ | When cloud is authoritative |
| Merge | ✅ Non-empty | ✅ Non-empty | ✅ Fields | General use |
| KeepNewest | ✅ If newer | ✅ If newer | ❌ | Time-based |
| ChooseFields | ✅ Selected | ✅ Selected | ✅ Per-field | Manual review |
| AlwaysLocal | ✅ Always | ❌ | ❌ | Offline-first |
| AlwaysRemote | ❌ | ✅ Always | ❌ | Cloud-first |
| AskEachTime | User decides | User decides | User decides | Safety |

---

## 6. ERROR RECOVERY

| Error Type | HTTP Code | Recovery Action | Retry Delay |
|-----------|-----------|----------------|-------------|
| NetworkInterruption | 0 | Retry | 5s |
| TokenExpiration | 401 | Refresh token | Immediate |
| ServerError | 500, 503 | Retry | 10s |
| RateLimit | 429 | Retry | 60s |
| PartialUpload | 206 | Resume upload | Immediate |
| PartialDownload | 206 | Resume download | Immediate |
| Conflict | 409 | Resolve | Manual |
| CorruptedData | 422 | Skip object | None |
| NotFound | 404 | Remove locally | None |

---

## 7. SECURITY MODEL

### 7.1 Token Security
- OAuth tokens encrypted at rest using platform-specific encryption
- Never stored in plain text
- Automatically refreshed before expiration
- Revoked on sign out
- Never exposed to UI, logs, or crash reports

### 7.2 Data Security
- All API responses validated
- Checksum verification for all synchronized data
- Rate limiting protection
- Encrypted local storage for sensitive data
- No credentials stored in application code

### 7.3 Device Trust
- Each device has a unique identifier
- Devices can be individually trusted or revoked
- Sign out from individual devices or all devices
- Force sync capability for troubleshooting

---

## 8. OFFLINE ARCHITECTURE

```
┌─────────────────────────────────────────────┐
│              User Interface                  │
│         (Works without connectivity)         │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│           Offline Change Queue               │
│     Tracks: Add · Modify · Delete · Move     │
│     Persisted locally until sync             │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│           Sync Engine (when online)          │
│     1. Process queue                         │
│     2. Upload changes                        │
│     3. Download remote                       │
│     4. Resolve conflicts                     │
│     5. Apply to local DB                     │
└─────────────────────────────────────────────┘
```

### 8.1 Offline Capabilities
- All study functionality works without internet
- Changes are queued locally with timestamps
- Automatic sync when connection is restored
- Never interrupts studying for connectivity
- Queue persists across app restarts

---

## 9. BACKUP STRUCTURE

### 9.1 Backup Format (JSON)

```json
{
  "version": 1,
  "createdAt": 1722000000000,
  "profile": {
    "id": "profile_001",
    "name": "Default",
    "createdAt": "2026-01-01T00:00:00Z"
  },
  "data": {
    "decks": { ... },
    "cards": { ... },
    "settings": { ... },
    "themes": { ... },
    "history": { ... }
  },
  "checksum": "a1b2c3d4e5f6..."
}
```

### 9.2 Backup Retention
- Automatic cleanup keeps last 50 backups
- Configurable maximum backup count
- Oldest backups are removed first
- Manual backups are never auto-deleted

---

## 10. MULTI-PROFILE ISOLATION

Each profile maintains completely separate:

| Data | Isolated | Shared |
|------|----------|--------|
| Decks | ✅ | ❌ |
| Cards | ✅ | ❌ |
| Review History | ✅ | ❌ |
| Statistics | ✅ | ❌ |
| Settings | ✅ | ❌ |
| Themes | ✅ | ❌ |
| Layouts | ✅ | ❌ |
| Keyboard Shortcuts | ✅ | ❌ |
| Bookmarks | ✅ | ❌ |
| Custom Dictionaries | ✅ | ❌ |
| Imported Resources | ✅ | ❌ |
| Sync Configuration | ✅ | ❌ |

---

## 11. FUTURE PROVIDER INTEGRATION

To add a new cloud provider:

1. **Implement `CloudProvider` interface**:
   ```typescript
   interface CloudProvider {
     providerId: string;
     displayName: string;
     initialize(authSession: AuthSession): Promise<void>;
     upload(operations: SyncOperation[]): Promise<SyncResult[]>;
     download(sinceTimestamp: number): Promise<SyncOperation[]>;
     getRemoteState(): Promise<RemoteState>;
     deleteObject(objectId: string, objectType: SyncObjectType): Promise<void>;
     getStorageInfo(): Promise<StorageInfo>;
     validateConnection(): Promise<boolean>;
   }
   ```

2. **Add `AuthProvider` variant** to the union type

3. **Register in provider factory**

4. **Add UI option** in settings

### Planned Providers
- GitHub (current — uses Gist API)
- WebDAV (any WebDAV-compatible server)
- Self-Hosted (custom Kaiteyo sync server)
- Dropbox (Dropbox API v2)
- Google Drive (Google Drive API v3)
- OneDrive (Microsoft Graph API)
- iCloud (CloudKit)
- S3-Compatible (MinIO, AWS S3, etc.)

---

## 12. DATA FLOW DIAGRAM

```
┌─────────────────────────────────────────────────────────────────────┐
│                        KAITEYO ACCOUNT SYSTEM                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────────┐  │
│  │  GitHub OAuth │───>│  Auth Manager│───>│  Secure Token Store  │  │
│  │  Device Flow  │    │              │    │  (Encrypted)         │  │
│  └──────────────┘    └──────┬───────┘    └──────────────────────┘  │
│                             │                                       │
│  ┌──────────────┐    ┌──────▼───────┐    ┌──────────────────────┐  │
│  │  Profile      │<───│  Sync Engine │───>│  Cloud Provider      │  │
│  │  Manager      │    │              │    │  (GitHub Gist)       │  │
│  └──────────────┘    └──────┬───────┘    └──────────────────────┘  │
│                             │                                       │
│  ┌──────────────┐    ┌──────▼───────┐    ┌──────────────────────┐  │
│  │  Device       │<───│  Conflict    │───>│  Encryption Layer    │  │
│  │  Manager      │    │  Resolver    │    │  (Checksum/Integrity)│  │
│  └──────────────┘    └──────────────┘    └──────────────────────┘  │
│                                                                     │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────────┐  │
│  │  Backup       │    │  Offline     │    │  Error Recovery      │  │
│  │  Manager      │    │  Queue       │    │  (Auto-retry)        │  │
│  └──────────────┘    └──────────────┘    └──────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 13. SETTINGS REFERENCE

### Sync Settings (Full List)

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `provider` | AuthProvider | Local | Sync provider |
| `autoSync` | boolean | true | Enable automatic sync |
| `syncFrequency` | enum | Every15Minutes | How often to sync |
| `meteredNetwork` | boolean | false | Allow on metered |
| `wifiOnly` | boolean | true | Only sync on Wi-Fi |
| `conflictStrategy` | enum | AskEachTime | How to resolve conflicts |
| `autoBackup` | boolean | true | Enable automatic backups |
| `backupFrequency` | enum | Daily | How often to backup |
| `maxBackups` | number | 10 | Max backups to keep |
| `encryptLocalData` | boolean | true | Encrypt local data |
| `syncOnAppStart` | boolean | true | Sync on app launch |
| `syncOnAppResume` | boolean | true | Sync on app resume |
| `showSyncNotifications` | boolean | true | Show sync notifications |

---

## 14. VERSION HISTORY

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-07 | Initial account system with GitHub OAuth |
| 1.1.0 | 2026-08 | Multi-profile support, device management |
| 1.2.0 | 2026-09 | Modular sync engine, conflict resolution, backup system, encryption, error recovery, offline queue |

---

## 15. IMPLEMENTATION FILES

| File | Purpose |
|------|---------|
| `core/.../account/AccountModels.kt` | All data models, enums, and interfaces |
| `core/.../account/GitHubOAuth.kt` | GitHub OAuth Device Flow, token encryption, authentication manager |
| `core/.../account/MultiProfileManager.kt` | Profile manager, device manager, backup manager |
| `core/.../sync/SyncEngine.kt` | Modular sync engine, conflict resolver, error recovery, change tracker |
| `core/.../sync/GitHubCloudProvider.kt` | GitHub Gist cloud provider, encryption layer, WebDAV stub |
| `presentation/.../sync/SyncSettingsUI.kt` | Full settings UI with 5 tabs |
| `docs/architecture/SYNC.md` | Architecture documentation |
| `docs/architecture/ACCOUNT.md` | This document — complete account structure reference |