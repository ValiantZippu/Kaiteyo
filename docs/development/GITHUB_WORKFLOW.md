# Kaiteyo — GitHub Workflow

## Overview

Kaiteyo uses a simple trunk-based fork of GitFlow: `develop` is the default
integration branch, `main` holds tagged releases, and feature/fix/docs branches merge
back to `develop` via squash-merges. Tag pushes trigger the release pipeline
(`.github/workflows/build-release.yml`).

## Branch strategy

```
main              — production-ready; tags only (vX.Y.Z)
  └── develop     — default integration branch (origin/develop)
       ├── feature/*   — new features (feature/floating-sidebar)
       ├── fix/*       — bug fixes (fix/window-drag)
       ├── docs/*      — documentation (docs/theme-system)
       ├── refactor/*  — code restructuring
       └── release/*   — release preparation (rare; usually tag from develop)

Tags: v1.1.0, v2.0.0, ... (must be 3-part; version source: buildSrc/AppVersion.kt)
```

### Branch naming

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feature/` | New features | `feature/floating-sidebar` |
| `fix/` | Bug fixes | `fix/window-drag-region` |
| `docs/` | Documentation | `docs/theme-system` |
| `refactor/` | Code refactoring | `refactor/settings-module` |
| `release/` | Release preparation | `release/v2.3.0` |
| `hotfix/` | Emergency fixes | `hotfix/v2.2.1` |

## Daily workflow

```bash
# Start: sync develop
git checkout develop
git pull --rebase

# Create a feature branch
git checkout -b feature/my-feature

# Edit, then compile-check frequently
./gradlew :desktopApp:compileKotlinJvm

# Stage, commit, push
git add <specific files>
git commit -m "feat(scope): description"
git push origin feature/my-feature
```

> Stage specific files, not `git add .` — never commit `local.properties`, build
> outputs, or secrets (all gitignored, but be deliberate).

## Pull requests

1. Push the branch; open a PR against **`develop`**.
2. PR description covers: summary, problem, solution, architecture impact,
   screenshots where relevant, testing performed, known limitations, DB changes,
   license implications (per `ENGINEERING_STANDARDS.md` §171).
3. Keep PRs small and reviewable — one coherent change.
4. Merge with **squash** (feature branches) — commit history on `develop` stays
   linear-ish and meaningful.

### Fork workflow (external contributors)

1. Fork on GitHub; clone your fork.
2. `git remote add upstream https://github.com/ValiantZippu/Kaiteyo.git`
3. `git fetch upstream && git checkout -b feature/x upstream/develop`
4. Push to your fork; open a PR to `ValiantZippu/Kaiteyo`'s `develop`.

## Commit message convention

```
type(scope): description

[optional body]

[optional footer]
```

| Type | Usage |
|------|-------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation |
| `refactor` | Code restructuring |
| `perf` | Performance improvement |
| `test` | Tests |
| `style` | Formatting, styling |
| `chore` | Build, dependencies, CI |

Examples:
```
feat(media): add subtitle phrase selection
fix(nav): preserve bubble position across restarts
docs(architecture): document knowledge graph
refactor(statistics): unify dashboards into one screen
```

## Tags and releases

```bash
# Bump version in buildSrc/AppVersion.kt FIRST (single source of truth)
git add buildSrc/src/main/kotlin/AppVersion.kt
git commit -m "chore(release): v2.3.0"
git push origin develop
# Then tag (usually from develop, or main after merge)
git tag v2.3.0
git push origin v2.3.0
```

### What the tag push does (`build-release.yml`)

1. `build-all` — reusable workflow: Linux builds `:app:assembleFdroidRelease` +
   desktop Linux packages; Windows/macOS build `packageDistributionForCurrentOS`.
2. `stage-and-verify` — `stage-artifacts.sh` collects artifacts into
   `release/kaiteyo-<ver>` and `verify-artifacts.sh` checks the sha256 manifest;
   `make-update-manifest.sh` regenerates stable/beta/nightly update feeds against the
   `update-feed` release.
3. `create-release` — `softprops/action-gh-release` publishes installers + the
   artifact manifest, and refreshes the `update-feed` prerelease that the desktop
   updater reads.

Full detail: `docs/architecture/ci-cd.md`, `docs/releases/RELEASE_PROCESS.md`,
`docs/releases/RELEASE_CHECKLIST.md`.

## CI checks on PRs

`build-all.yml` (workflow_reuse) compiles/validates the key targets. iOS targets are
skipped on Windows/most runners (expected). There is no lint/format job — style is
enforced by convention (`docs/development/CODING_STANDARDS.md`).

## Git configuration tips

```bash
git config --global pull.rebase true
git config --global diff.colorMoved zebra
git config --global core.longpaths true   # Windows
```

## Protected branches

- `main` — protected: PR review + status checks, no direct pushes.
- `develop` — protected: status checks required, direct pushes restricted for most
  contributors.

## Related

- `DEVELOPER_GUIDE.md` — build/test commands
- `docs/releases/RELEASE_PROCESS.md` — end-to-end release workflow
- `docs/architecture/ci-cd.md` — CI/CD pipeline detail
- `docs/guides/GIT_GUIDE.md` — beginner Git guide
