# Kaiteyo — Tag System

> Date-first, channel-aware, version-pinned, always unique.

## Format

```
YYYY.MM.DD-<channel>-v<VERSION>-<CODE>-<SHA>[-N]
```

| Segment | Source | Example | Purpose |
|---|---|---|---|
| `YYYY.MM.DD` | UTC date at tag creation | `2026.08.31` | Year / month / exact day — chronological sort |
| `<channel>` | branch → channel map | `alpha` / `beta` / `release` / `stable` | Title / release track |
| `v<VERSION>` | `AppVersion.kt:versionName` | `v2.2.1` | Semantic title version number |
| `<CODE>` | `AppVersion.kt:versionCode` | `2210` | Monotonic build number, unique across bumps |
| `<SHA>` | `git rev-parse --short=7 HEAD` | `d22e0c1f` | Commit traceability, guarantees uniqueness |
| `[-N]` | auto-suffix if collision | `-2` | Same-day same-commit re-tag |

### Full examples

```
2026.08.31-alpha-v2.2.1-2210-d22e0c1      # early-alpha-develop @ 2.2.1
2026.08.31-beta-v2.2.1-2210-6a987d8       # early-beta-develop @ 2.2.1
2026.08.31-release-v2.2.1-2210-d22e0c1    # early-release-develop (ex-develop) @ 2.2.1
2026.08.31-stable-v2.3.0-2220-a1b2c3d     # main @ 2.3.0
2026.08.31-alpha-v2.2.1-2210-d22e0c1-2    # second tag same day/commit
```

Sorting: `YYYY.MM.DD` prefix makes `git tag --sort=version:refname` and lexicographic sort chronological.

## Channel map (branch → tag channel)

| Branch | Channel | Meaning |
|---|---|---|
| `early-alpha-develop` | `alpha` | Experimental, unstable |
| `early-beta-develop` | `beta` | Feature-complete, QA |
| `early-release-develop` | `release` | Prerelease / RC, final before main |
| `main` | `stable` | Production publish |
| other | `dev` | Fallback |

Override channel explicitly: `./scripts/tag.sh beta`

## Version source — single truth

* `buildSrc/src/main/kotlin/AppVersion.kt` — `versionName` + `versionCode` + `desktopAppVersion`
* `installer/common/version.json` — must match `AppVersion.kt` (bump via `installer/scripts/bump-version.sh 2.3.0 2220`)

Tag embeds both — version bump already breaks uniqueness even same day.

## Uniqueness guarantee

1. **Date + version + code** — same day same version collides → suffix `-N`
2. **+ SHA** — different commits same day never collide
3. **+ -N** — same commit re-tagged same day gets `-2`, `-3`

No manual `v1.0` reuse: tag creation fails safely and suggests next suffix.

## Usage

### PowerShell (Windows — primary)
```powershell
# auto-detect channel from current branch
.\scripts\tag.ps1
.\scripts\tag.ps1 -DryRun

# explicit channel + push
.\scripts\tag.ps1 alpha -Push
.\scripts\tag.ps1 beta -DryRun
.\scripts\tag.ps1 release -Push
```

### Bash (macOS/Linux/CI)
```bash
./scripts/tag.sh              # auto channel
./scripts/tag.sh --dry-run
./scripts/tag.sh alpha --push
./scripts/tag.sh beta --dry-run
./scripts/tag.sh release --push
```

Both read `AppVersion.kt`, compute `YYYY.MM.DD` (UTC), resolve `SHA`, check existing tags, create annotated tag:

```
Kaiteyo alpha 2.2.1 (2210) — 2026.08.31 — early-alpha-develop@d22e0c1
```

### What it does
1. Parse `versionName`/`versionCode`
2. `date -u +%Y.%m.%d` + channel + `git rev-parse --short=7 HEAD`
3. `TAG=YYYY.MM.DD-channel-vVERSION-CODE-SHA`
4. If `TAG` exists → `TAG-N`
5. `git tag -a TAG -m "Kaiteyo …"`
6. Optional `--push`: `git push origin TAG`

## Listing & verifying

```bash
git tag --sort=version:refname | grep -E "^2026\."
git tag --list "2026.08.*-alpha-*"
git show 2026.08.31-alpha-v2.2.1-2210-d22e0c1
```

## CI integration

Release workflow (`build-release.yml`) triggers on tag `YYYY.MM.DD-*-v*` and builds `build-all.yml` per channel. Add tag filter in CI:

```yaml
on:
  push:
    tags: ["[0-9][0-9][0-9][0-9].[0-9][0-9].[0-9][0-9]-*-v*"]
```

## Manual tagging (without script)

```bash
DATE=$(date -u +%Y.%m.%d)
SHA=$(git rev-parse --short=7 HEAD)
git tag -a $DATE-release-v2.2.1-2210-$SHA -m "Kaiteyo release 2.2.1 — $DATE"
git push origin $DATE-release-v2.2.1-2210-$SHA
```

## Migration from old scheme

Old `v2.3.0` tags remain valid. New system coexists — old tags have no date prefix and sort separately. For new work, always use date-first tags. To mark a legacy version with new system, re-tag same commit:

```bash
./scripts/tag.sh release --push # creates 2026.08.31-release-v2.2.1-2210-xxxx
```

## Rules

1. One source of version truth — never hand-edit tag version part, always from `AppVersion.kt`.
2. Always annotated tags (`-a`) — lightweight tags are not pushed.
3. Never reuse tag name — script auto-increments.
4. Tag message must contain channel + version + branch + SHA for audit.
5. Prerelease tags (`alpha`/`beta`/`release`) never go to `main` release notes — only `stable` does.
