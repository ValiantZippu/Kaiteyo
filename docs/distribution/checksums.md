# Checksums & Artifact Integrity

Every released artifact ships with a **SHA-256** checksum. No release publishes
without one — the integrity gate fails the build.

## Where the checksums live

| Location | What |
|---|---|
| `artifact-manifest.json` | Attached to every GitHub release; one entry per artifact (name, size, sha256, arch) |
| `update-<channel>.json` | Update feeds — every entry carries `sha256`, `size_bytes`, `arch` |
| Release notes | Human-readable checksum list next to the download links |

## How they are generated

`installer/scripts/stage-artifacts.sh` renames artifacts to canonical names
and computes the sha256 manifest; `installer/scripts/verify-artifacts.sh`
re-checks every artifact against it and **fails the release** on any mismatch
(missing, corrupt, wrong name, wrong version).

```bash
bash installer/scripts/stage-artifacts.sh 2.2.1 release/kaiteyo-2.2.1
bash installer/scripts/verify-artifacts.sh 2.2.1
```

Checksums are computed from the **actual built artifacts** — never fabricated,
never copied from a previous release.

## User verification commands

- **Windows**: right-click the installer → Properties → File hashes (or
  `Get-FileHash Kaiteyo-Setup-2.2.1.exe -Algorithm SHA256`).
- **macOS / Linux**:

```bash
shasum -a 256 Kaiteyo-2.2.1-linux.AppImage      # macOS / BSD
sha256sum Kaiteyo-2.2.1-linux.AppImage           # Linux
```

Compare the output with the release notes / `artifact-manifest.json`.

## Integrity flow

```
build artifact
   → stage (canonical name)
   → sha256 computed
   → manifest written
   → verify re-checks (fail = no release)
   → manifest + artifacts attached to release
   → update feed built from manifest (same hashes)
```

## Update verification

The updater verifies `sha256` + `size_bytes` **before** writing anything to
disk — a tampered or truncated download is rejected, not installed
(see [updates.md](updates.md)).

## Honesty rule

No fake checksums. If an artifact is rebuilt (e.g. after a signing change), the
manifest is regenerated from the new bytes — `verify-artifacts.sh` enforces
this automatically.
