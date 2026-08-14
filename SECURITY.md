# Security Policy

Kaiteyo is a local-first, offline-capable application. The security model is designed
around the principle that **your study data belongs on your device** — most features never
touch the network. This document describes what that model is, how to report a
vulnerability, and the areas that involve external services.

See [`docs/security/README.md`](docs/security/README.md) for the full threat model and
[`docs/security/PRIVACY.md`](docs/security/PRIVACY.md) for what data leaves the device.

## Reporting a vulnerability

**Please do not open a public issue for security vulnerabilities.** Instead:

- Email the maintainers via the GitHub security advisory workflow:
  **Security → "Report a vulnerability"** on the repository
  (<https://github.com/ValiantZippu/Kaiteyo/security/advisories>), or
- Open a **private** advisory.

Please include:

- The affected version and platform
- A description of the issue and its impact
- Steps to reproduce (or a proof of concept)
- Whether you believe the issue is exploitable remotely or only locally

We aim to acknowledge reports within a few business days and to respond with a fix plan
(or a reasoned "won't fix") in a reasonable timeframe. Coordinated disclosure is
appreciated: please allow time for a fix before publishing details.

## Supported versions

| Version | Status |
|---|---|
| Latest stable (currently 2.2.x) | ✅ supported |
| Older versions | ⚠️ best-effort — please upgrade |
| Development (`develop`) | 🔧 not for production use |

## Security model

### Local data

- All study data, decks, cards, statistics, settings, and imported dictionaries are stored
  **locally on the device** (SQLDelight databases, DataStore preferences, JSON files under
  `~/.kaiteyo/` on desktop).
- There is **no server-side account database**. There are no user accounts hosted by
  Kaiteyo; the only "account" feature is OAuth against a third-party provider (GitHub) used
  purely for sync storage.
- Data at rest is **not encrypted by Kaiteyo** — it relies on the operating system's
  account/disk protections. On desktop, study data sits in the user's home directory;
  anything written there is readable by other processes running as the same user.
  Sensitive data (e.g., an API token) is stored with the same file permissions as
  settings; it is not obfuscated or encrypted at rest.

### Network features (all opt-in)

| Feature | What it does | When it connects |
|---|---|---|
| Sync | Uploads an encrypted-in-transport backup snapshot (study data) to the user's private GitHub gist | Only when the user signs in and enables sync |
| Account (GitHub OAuth) | Device-flow OAuth; the app receives a short-lived token used only for the gist sync | Only when the user initiates sign-in |
| App data download | Downloads the bundled dictionary database + TTS voice assets from GitHub releases | First build / when assets are missing (developers) |
| Dictionary import | None — Yomitan/JSON/ZIP dictionaries are parsed locally | Never |
| Local HTTP API | A localhost HTTP server for integrations (media, mining, player control) | Localhost only; every endpoint except `/api/health` requires a bearer token |
| AnkiConnect | Communicates with a local Anki installation over its own HTTP API | Localhost only, when the user configures it |
| Media playback | Uses locally installed VLC / mpv / Java Sound | Never (offline files) |
| OCR | Uses locally installed Tesseract (when available) | Never |
| Update checks | Fetches the update feed manifest from GitHub releases | When the user enables auto-update (desktop) |

### Authentication

- **GitHub device flow**: the desktop app never receives the user's GitHub password.
  The user authorizes on github.com and the app receives scoped access tokens used only
  for the sync gist.
- **Local API**: a bearer token is generated once per install, persisted in settings, and
  required on every local API request except `/api/health`.
- **No Kaiteyo credentials exist**: there is no central service, so there are no
  Kaiteyo-managed passwords, session cookies, or API keys to leak.

### Handling untrusted content

- **Imported decks / `.apkg` files**: HTML is sanitized (scripts, styles, event handlers,
  `javascript:` URIs are stripped) before rendering. Imported content is never executed.
- **Imported dictionaries**: parsed locally with strict parsers; corrupt archives fail
  with clear errors. See `docs/data/SOURCES.md` for the provenance of bundled data.
- **Plugins**: the plugin *registry/marketplace scaffold* exists but **no runtime code
  loading is implemented**. If plugin execution is added in the future, this document must
  be updated to describe its sandboxing.
- **Media files**: playback uses locally installed backends (VLC/mpv); Kaiteyo does not
  execute media content.

### Supply chain / builds

- Releases are built by GitHub Actions from tagged commits (see
  [`docs/releases/RELEASE_PROCESS.md`](docs/releases/RELEASE_PROCESS.md)).
- Desktop installers are signed (Windows Authenticode, macOS hardened-runtime +
  notarization); Linux packages ship with sha256 manifests verified at staging time.
- Auto-update downloads are verified against sha256 hashes from the signed update feed
  before installation (`UpdatePolicy` guards version rollback).

## Known limitations (not yet hardened)

- Local data is **not encrypted at rest**; backup archives are plain files.
- The sync gist is protected by the user's GitHub account security, not by Kaiteyo.
- The update feed URL is fetched over HTTPS; certificate validation uses the platform
  trust store.
- No sandboxing for future plugin code execution (not yet implemented).

## Related documentation

- [`docs/security/README.md`](docs/security/README.md) — full threat model and design
- [`docs/security/PRIVACY.md`](docs/security/PRIVACY.md) — data collection and storage
- [`docs/integrations/README.md`](docs/integrations/README.md) — external integrations
- [`docs/releases/RELEASE_PROCESS.md`](docs/releases/RELEASE_PROCESS.md) — build/signing pipeline
