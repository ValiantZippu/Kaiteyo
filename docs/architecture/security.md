# Kaiteyo — Security & Privacy

> **Status**: `ARCHITECTED` (threat model) + `PARTIALLY_IMPLEMENTED` (isolation, token storage).
> Companion: `browser.md` (isolation), `platforms.md` (tokens), `anki.md` (loopback), `core.md`.

## 1. Boundaries

| Boundary | Rule |
|----------|------|
| Browser isolation | Websites never access Kaiteyo internal APIs, file system, or other tabs (`browser.md`). JS injection is versioned, read-only helpers. No remote `eval`. |
| Downloaded files | Zip-slip protected (`SafeArchiveExtractor`), validated before persist, quarantined on failure. |
| External platform tokens | OAuth PKCE, encrypted at rest (OS keystore), never logged, revoke on disconnect. |
| AnkiConnect | Loopback only (`127.0.0.1:8765`), no remote, no credentials. |
| Local database | SQLDelight with WAL; backups encrypted if user opts in. |
| Logs | No tokens/PII in logs; Sentry scrubbed. |
| Telemetry | Opt-in only (if ever); documented in `docs/security/PRIVACY.md`. |
| Credentials / sensitive settings | Never committed (`local.properties` gitignored), never in screenshots. |

## 2. Offline-first & privacy

- All core learning, mining to Kaiteyo deck, media/reading consumption, dictionary, OCR, game — fully offline.
- Network is required only for: platform sync, subtitle provider search, streaming, AnkiConnect push — each queued offline and user-visible.
- See `docs/security/PRIVACY.md` for the full privacy statement (what is stored, what leaves the device).

## 3. Failure

Permission denied (file/screen capture) → guided prompt, never silent failure. Token expired → re-auth. Storage full → evict cache, never lose user data.

## 4. Evolution

New adapter → new permission audit; no existing boundary weakened.
