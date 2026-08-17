# Distribution Security Model

How users can trust that what they download is Kaiteyo, and that updates are
safe to apply.

## Trust model

| Layer | Mechanism |
|---|---|
| Transport | HTTPS-only for downloads, feeds, and updates |
| Integrity | SHA-256 per artifact (`artifact-manifest.json` + update feeds), verified before install/apply |
| Provenance | Releases come only from the GitHub release process (tags `v*.*` → `build-release.yml`) |
| Signing (where the OS requires it) | Windows Authenticode, macOS notarization, Android keystore — see [signing.md](signing.md) |
| Update feed | Pinned to the `update-feed` release, schema-validated, channel-pinned |

## Release provenance

1. A maintainer bumps the version (`bump-version.sh`) and tags `vX.Y.Z`.
2. CI builds on clean runners, packages per platform, signs where configured,
   stages artifacts, computes checksums, and **verifies them**.
3. Verification failure = **no release**.
4. The release + manifest are published; update feeds are generated **from the
   verified manifest** and published to the `update-feed` release.

An artifact on the releases page that isn't in the manifest is a release bug —
report it.

## Update verification

The updater:

- fetches the channel feed over HTTPS (never HTTP);
- validates the manifest against its JSON schema;
- verifies `sha256` + `size_bytes` before writing anything;
- stages next to the current version, replaces atomically, keeps the previous
  version for rollback, and never touches user data.

It is **never** a "download whatever URL the server returns and execute it"
design. The `ReleaseProvider` abstraction keeps update logic decoupled from any
single URL (GitHub today, official server or store APIs later) without
loosening verification (see [updates.md](updates.md)).

## What never happens

- **No secrets in packages** — certificates, keystores, passwords, tokens are
  never embedded in installers or the repo (see [signing.md](signing.md)).
- **No embedded credentials or debug endpoints** in release builds.
- **No developer-only paths** — release packages contain no dev/data-dir
  assumptions (user data always goes to the platform-standard locations).
- **No root application execution on Linux** — the app runs as the user; only
  installation uses package-manager privileges.
- **No unnecessary Android permissions** — runtime permissions are requested
  only when a feature needs them.
- **No arbitrary command execution** from the installer — the Inno script runs
  a fixed task list; no downloaded code is executed.
- **No unsafe temp paths** — staging uses the user data dir, not world-writable
  locations.

## Installer security review checklist

- [ ] No arbitrary command execution
- [ ] No unsafe temporary paths
- [ ] No insecure download
- [ ] No unsigned update execution
- [ ] No secrets in the package
- [ ] No embedded credentials
- [ ] No debug endpoints
- [ ] No developer-only paths
- [ ] No root requirement on Linux
- [ ] No unnecessary Android permissions
- [ ] Signing secrets never in the repo (audited each release)

## Privacy

- Installation is **local**. No installer telemetry, no upload of decks, study
  data, dictionary history or personal files.
- Any future telemetry would be documented, privacy-conscious and opt-in
  (see `docs/security/PRIVACY.md`).

## Related

- `docs/security/README.md` — the app-level threat model.
- `SECURITY.md` (repo root) — reporting a vulnerability.
