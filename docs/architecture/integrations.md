# Kaiteyo Architecture — Integrations Specification

**Status**: Mixed — Anki/Yomitan/local API/account implemented; browser + plugins planned
**Owner**: suite `ua.syt0r.kanji.desktop.engine` (mining, api, account, browser, plugin) +
core `transfer` + `plugin/`
**Related**: `docs/integrations/ANKI.md` · `docs/integrations/YOMITAN_DICTIONARIES.md` ·
`docs/integrations/LOCAL_API.md` · `docs/integrations/PLUGINS.md` ·
`docs/architecture/decisions/0009-github-sync-and-account.md` · `0011-plugin-security-first.md`

## 1. Purpose & principles

Kaiteyo integrates with external systems through **adapters behind stable interfaces**
(§200, §292). No HTTP calls scattered through UI; no internal dependency on any external
service (§199, §201). If an integration is unavailable, Kaiteyo keeps working and says so
("Anki unavailable", not a crash). Imported data (decks, dictionaries, content) is
untrusted — validated before use (§358).

## 2. Anki

### 2.1 Principle
Anki remains an integration (§199). Kaiteyo owns decks/cards/notes/reviews/scheduling;
Anki syncs/exports where supported. Kaiteyo's card pool is the primary destination;
AnkiConnect is a bridge, never the database underneath.

### 2.2 `.apkg` codec (core `transfer`)
Real import/export on all platforms:
- **JVM** — `AnkiPackage.jvm.kt`.
- **Android** — via `SQLiteDatabase`.
- **iOS** — dependency-free pure-Kotlin ZIP/inflate codec (`AnkiPackage.ios.kt`).
Features: scheduling mapping, media extraction, template rendering, HTML sanitization.
iOS/Android paths are code-complete; runtime verification pending (BLOCKED).

### 2.3 AnkiConnect transport (`AnkiConnectTransport : MiningTransport`)
JSON-RPC-style client (`version 6`), `AnkiConfig(host=127.0.0.1, port=8765, apiKey,
deckOverride)` read from settings at call time. 5 s connect / 8 s request timeouts;
responses parsed for `error` field. Operations: `testConnection`, `deckNamesAndIds`,
`findNotes`, `notesInfo`, `findCards`, `cardsInfo`, `retrieveMediaFile`, `send(payload)`
(Basic note with tags + attached media via base64), duplicate detection via
`canAddNotes`. `configured`/`connected`/`lastError` reactive state. Failures are
non-fatal: mines queue as `PendingAnkiExport` and retry without duplicating
(`docs/architecture/mining.md` §5).

### 2.4 Import/export pipeline (core + suite)
JSON/CSV/TSV/TXT with preview + conflict policies and persisted merge
(`mergeImportedCards()` → FSRS scheduling); deck import from AnkiConnect (`AnkiImporter`);
learning-layer snapshot export (full-fidelity JSON + CSV/TSV). See
`docs/architecture/backup.md`.

## 3. Yomitan dictionaries

Compatible **data**, not browser-extension dependency (§197): the dictionary engine parses
Yomitan archive formats (`index.json` + term JSON-LD tuples/objects) and serves an
internal glossary with its own indexing and deinflection. No Yomitan runtime required.
Full detail: `docs/architecture/dictionary.md` + `docs/integrations/YOMITAN_DICTIONARIES.md`.

## 4. Localhost API (`LocalApiServer`)

Ktor Netty server, host `127.0.0.1`, port from `media.api.port` (default 48201).

- **Auth**: every endpoint except `/api/health` requires `Authorization: Bearer <token>`;
  the token is generated once on first use (UUID, 24 chars) and persisted via
  `media.api.token` so external tools keep working across restarts. Unauthorized → JSON
  `{ok:false, message:"Unauthorized — send Authorization: Bearer <token>"}`.
- **Endpoints**: `GET /api/status` (app, version, endpoint, reachable), `GET /api/health`
  (plain `ok`), `POST /api/mine` (`IntegrationCardRequest` → `toPayload()` → `mine()`;
  malformed body → explicit error response), plus player-state endpoints
  (`PlayerStateResponse`) and the WebSocket (`PlayerStateWebSocket`).
- **Self-test** (`selfTest()`) checks auth paths end-to-end.
- API versioning (§208) applies to any public surface; the local API is the integration
  bridge for browser extensions/tools (docs: `docs/integrations/LOCAL_API.md`).

## 5. Account & sync (GitHub, ADR-0009)

- **`AccountEngine`** — identity, connected providers, devices, sessions, sync settings;
  persisted JSON under `~/.kaiteyo/account/` (`identity.json`, `connections.json`,
  `devices.json`, `sessions.json`, `settings.json`). Profile update, device rename/remove,
  per-session sign-out, `recordSync`, `resetLocalAccount`, friendly error mapping.
- **`GitHubDeviceFlowClient`** — device flow OAuth: `requestDeviceCode` →
  `pollForToken` (with interval) → `refreshToken` → `revokeToken` → `fetchUserInfo`;
  plain `HttpClient` with form/JSON helpers.
- **`TokenVault`** — `interface TokenVault { save/read/delete/clear/configuredKinds }`;
  `FileTokenVault` with 0600 permissions, machine-key-derived XOR obfuscation — the seam
  for OS keychain backends (§204: credentials never in Git, never plaintext).
- Conflict dialog + offline/retry states; semantic-object sync (§271); conflict
  resolution defined before multi-provider expansion (§270). Full detail:
  `docs/architecture/SYNC.md` + `ACCOUNT.md`.

## 6. Embedded browser (implemented as a lightweight engine; webview planned)

`BrowserEngine` (suite): tabs, bookmarks, downloads, history, Reader mode
(`RenderMode { Reader, RawText, WebView, Unavailable }` — `decide(url, html)`,
`extractReadable`), selection → dictionary (`captureSelection`, `openSelectedInDictionary`),
mining. Navigation falls back gracefully when no webview renderer is available. Per
§198/§360 the abstraction is required: do not assume Chrome extensions work; a real
embedded-browser backend (CEF/WebView2/Android WebView) is an evaluated decision with
sandboxing, navigation validation, download control, permission separation — planned.

## 7. Plugins (deferred, security-first)

- Core `plugin_registry` table + suite `PluginRegistry` + `PluginMarketplace` exist as
  **metadata**; actual plugin loading is deliberately deferred (ADR-0011) pending a
  sandbox design (capability model, subprocess vs classloader).
- `PluginMarketplace` — fetch index/manifest from a URL (`DEFAULT_INDEX_URL`, 10 s
  timeout), `localManifest`, `demoCatalog`; marketplace data is untrusted input.
- Future plugin areas (§261): dictionary providers, media providers, import/export,
  theme packs, content packs, integrations. Plugins declare manifest, version,
  permissions, compatibility, API version (§261–§262, §359).
- Internal classes must never accidentally become public extension APIs (§263).

## 8. External knowledge services

Anime/metadata providers are adapters behind interfaces (e.g. `AnimeMetadataProvider`
with a future `AniListAdapter`) (§292) — UI depends on the interface, never provider HTTP.
External data ≠ official user mastery (§291). Rate limits: caching, backoff, timeouts
(§293). Privacy default: local data (§294).

## 9. Sync & account (summary)

GitHub device-flow OAuth + private-gist sync (ADR-0009) — `SyncManager`/`AccountManager`
(core) + `AccountEngine` (suite) with conflict dialog, offline/retry states. Semantic
objects only (§271); temporary UI state never syncs.

## 10. Error model (all integrations)

- Integration down → app continues; explicit "unavailable" messaging (§201, §296).
- Network: async, cancelable, retryable, timeout-aware, never on the UI thread
  (§268–§269).
- Imported data (decks, dictionaries, content) validated before use (§358); media files
  untrusted (§357).

## 11. Tests

- `AnkiConnectTransport`/`AnkiImporter` unit tests with stubbed HTTP; mining pending-
  export lifecycle tests; `TokenVault` round-trip tests; `LocalApiServer.selfTest`.
- Gaps: AnkiConnect e2e against a live Anki (BLOCKED), browser reader-mode fixtures,
  marketplace index validation tests.

## 12. Open items

- AnkiConnect end-to-end verification (needs live Anki).
- Embedded-browser backend decision (research + ADR; §198/§360).
- Plugin sandbox design (ADR-0011).
- Sync beyond desktop-first (mobile UX, conflict maturity).

## Node-layer integration (TARGET — ADR-0013, NODE §151, STANDARDS §199–§201, §292)

Integrations are **adapters, never core dependencies** (§151): the node layer treats
Anki/Yomitan/AniList as external identities with `mapped_to` edges.

- `mapped_to` edges bind external ids (Anki note id, Yomitan entry id, AniList series
  id) to internal nodes — one canonical node per concept, many external aliases
  (RELATIONSHIP_REGISTRY §3).
- `integration` (SYSTEM family) nodes track status/config; `integration_status_changed`
  events feed the UI status badge (§EVENT_CATALOG §9) — an unavailable integration is a
  typed status, never a crash (§201).
- Import paths (APKG, Yomitan archives) carry provenance: `imported_from` edges point
  at the source/dataset node (STANDARDS §184–§185).
- External data never equals official mastery (§291): AniList seen-tags or JPDB-style
  knowledge may enrich, but user knowledge comes from Kaiteyo evidence
  (KNOWLEDGE_STATE_MODEL §2).
- Rate limits/caching/backoff live in adapters (§293); the UI only sees the
  provider interfaces (SERVICE_CONTRACTS §18).
