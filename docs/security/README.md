# Security

The canonical security policy is the repository-root
[`SECURITY.md`](../../SECURITY.md) (vulnerability reporting, supported versions, and the
security model). This page expands the threat model and design.

## Design principle: local-first

Kaiteyo has **no central service**. There are no Kaiteyo-hosted accounts, no Kaiteyo
databases, and no Kaiteyo API keys. The security posture is therefore mostly about
*local data protection* and *how opt-in network features behave*.

## Threat model

| Threat | Exposure | Mitigation |
|---|---|---|
| Local attacker reads study data | Data at rest in home dir / app sandbox | OS account protections; Kaiteyo does **not** encrypt data at rest (documented limitation) |
| Malicious imported deck (`.apkg`) | HTML/XSS-style content | HTML sanitization before render (scripts/styles/event handlers/`javascript:` URIs stripped); imported content never executes |
| Malicious dictionary archive | Zip-slip / parser abuse | Strict local parsers; corrupt archives fail with clear errors; safe archive extraction tested (KJD `SafeArchiveExtractorTest`) |
| Local API abuse | Media/mining/player control on localhost | Bearer token required on every endpoint except `/api/health`; localhost bind |
| AnkiConnect misuse | Local Anki instance | Talked to only when user configures it; localhost |
| OAuth token theft | GitHub token used for sync | Device-flow OAuth (no password in app); scoped token; token stored in `TokenVault` with OS-typical file protections (not encrypted at rest) |
| Supply chain (malicious release) | Installer/update tampering | CI builds from tags; signed installers (Windows Authenticode, macOS hardened-runtime + notarization); sha256 manifests + verification gate at release staging; update downloads sha256-verified |
| Plugin code execution | Arbitrary code | **Not possible yet** — plugin runtime loading is not implemented (registry/marketplace scaffold only). When added, this page and `SECURITY.md` must document the sandbox |
| Media content | Malicious media files | Playback delegated to installed backends (VLC/mpv/Java Sound); Kaiteyo does not parse/execute media |
| OCR content | Malicious images | Delegated to local Tesseract when present |

## What is NOT protected (known limitations)

- **No encryption at rest** for local data or backups.
- **No sandbox** for future plugins (not implemented yet).
- **Sync security** = the user's GitHub account security (private gist).
- **No authentication** between the app and AnkiConnect (trusts the local machine).

## Secrets handling rules (for developers)

- Never commit keystores, passwords, tokens, or signing properties. Keystore resolution:
  env → `~/.kaiteyo/keystore.jks` → repo root (CI decodes `KEYSTORE_BASE64`), debug
  fallback otherwise.
- CI secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
  (Android), Apple signing/notarization credentials (macOS).
- The local API bearer token is generated per install and never committed.

## Related

- [`PRIVACY.md`](PRIVACY.md) — what data is stored and what leaves the device
- [`../integrations/README.md`](../integrations/README.md) — external integrations
- [`../releases/RELEASE_PROCESS.md`](../releases/RELEASE_PROCESS.md) — build/signing pipeline
