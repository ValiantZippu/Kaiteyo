# Code Signing & Secrets

> Full operational detail lives in `installer/docs/SIGNING.md`. This page is
> the distribution-level summary and policy.

## Per-platform requirements

| Platform | Requirement | Tool | Without credentials |
|---|---|---|---|
| Windows | Code signature (OV/EV cert, SHA-256) | `signtool` + RFC 3161 timestamp | EXE ships unsigned; release notes say so |
| macOS | Developer ID + Hardened Runtime + notarization + stapling | `codesign` + `notarytool` | DMG uploads unsigned with a warning |
| Android | Release keystore | Gradle signing config | Falls back to debug signing (local dev only) |
| Linux | None (sha256 manifest) | — | Integrity gate still runs |

## Where secrets live — and never live

**Never in the repository.** Not in Gradle files, not in installer scripts,
not in docs, not in workflow files, not in generated artifacts.

| Secret | Source |
|---|---|
| `WINDOWS_CERT_BASE64` / `WINDOWS_CERT_PASSWORD` | GitHub Actions secrets |
| `APPLE_ID` / `APPLE_APP_PASSWORD` / `APPLE_TEAM_ID` / `CODESIGN_IDENTITY` | GitHub Actions secrets / developer env |
| `KEYSTORE_BASE64` / `KEYSTORE_PASS` / `SIGN_KEY` / `SIGN_PASS` | GitHub Actions secrets / `~/.kaiteyo/keystore.jks` |

CI decodes certificates into temp files, signs, then deletes them. The Android
keystore resolves from env → `~/.kaiteyo` → repo root (where CI decodes the
base64 secret).

## SmartScreen / Gatekeeper — honest expectations

- **Windows**: with an EV cert + timestamp, downloads show the publisher
  ("syt0r"). Without EV, expect "Unknown publisher". Signing does **not**
  eliminate every warning; install behavior is tested on clean machines.
- **macOS**: notarization is mandatory for a clean Gatekeeper experience;
  without it users see "unidentified developer".
- No platform's signing is faked — an unsigned artifact is published as
  unsigned and called out in release notes, never mislabeled as signed.

## Integrity always

Signing is layered on top of an **always-on integrity gate**: every artifact
gets a sha256 in `artifact-manifest.json` and the update feed verifies hashes
before any install/swap — signed or not (see [checksums.md](checksums.md)).

## Verification before publish

- Windows: `signtool verify /pa /v` + SmartScreen manual test.
- macOS: `codesign --verify --deep --strict`, `spctl --assess`,
  `xcrun stapler validate`.
- Release CI: `verify-artifacts.sh` fails the release on any mismatch.

## Policy

1. **Secrets never enter the repo** — reviewed at every release.
2. **Release pipeline refuses to publish unsigned artifacts when secrets are
   configured** — we respect the OS, we never ask users to bypass security
   dialogs.
3. Certificate renewal/rotation is a documented release step (see
   `docs/releases/RELEASE_CHECKLIST.md`).
