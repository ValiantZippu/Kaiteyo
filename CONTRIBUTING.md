# Contributing to Kaiteyo

Thanks for your interest in Kaiteyo! This project welcomes contributions of all kinds —
code, documentation, design, data, translations, bug reports, and ideas.

Kaiteyo is a Kotlin Multiplatform + Compose Multiplatform project (desktop, Android, iOS)
with a desktop-first immersion suite. Before you start, please read:

1. [`README.md`](README.md) — what the project is and its current status
2. [`docs/development/DEVELOPMENT_SETUP.md`](docs/development/DEVELOPMENT_SETUP.md) — environment setup
3. [`docs/development/CODING_STANDARDS.md`](docs/development/CODING_STANDARDS.md) — code conventions
4. [`docs/development/AI_CONTEXT.md`](docs/development/AI_CONTEXT.md) — conventions and "never change" list (also written for AI-assisted contributors)
5. [`docs/planning/CURRENT_ISSUES.md`](docs/planning/CURRENT_ISSUES.md) — known issues to fix

## Quick start

```bash
# 1. Fork the repository on GitHub, then clone your fork
git clone https://github.com/<your-username>/Kaiteyo.git
cd Kaiteyo

# 2. Create a branch from develop (the default branch)
git checkout develop
git checkout -b feature/your-feature

# 3. Verify the build works before changing anything (JDK 17 required)
./gradlew :desktopApp:compileKotlinJvm

# 4. Make your changes, then run checks
./gradlew :core:allTests
./gradlew :desktopApp:compileKotlinJvm

# 5. Commit with a conventional message and push
git commit -m "feat: add your feature"
git push origin feature/your-feature

# 6. Open a pull request targeting develop
```

## Branching strategy

```
main              — production-ready code (protected; releases only)
└── develop       — default branch; integration branch; PRs target this
     ├── feature/*   — new features
     ├── fix/*       — bug fixes
     ├── docs/*      — documentation
     ├── refactor/*  — refactoring
     └── release/*   — release preparation
```

Keep branches short-lived and scoped. Feature branches are squash-merged; release branches
are merged with a regular merge.

## Commit conventions

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add floating sidebar
fix: correct window drag behavior
docs: update architecture guide
refactor: extract theme state holder
perf: optimize animation performance
test: add unit tests for ThemeManager
chore: update dependency versions
```

Format: `type(scope): description`. Keep the first line under ~72 characters; add a body
explaining *why* when the change isn't obvious.

## Pull requests

A good PR:

- **Targets `develop`** (never `main` directly).
- **Has a clear title** describing the change.
- **Links an issue** when one exists (bug reports and feature proposals live in Issues).
- **Is small and focused** — one logical change per PR. Large features should be split
  into reviewable chunks.
- **Passes the checks**: `:core:allTests` and `:desktopApp:compileKotlinJvm` are green,
  with no new compiler warnings.
- **Includes screenshots or a short description** for UI changes.
- **Updates documentation and the changelog** where behavior changed.

### Code review expectations

- All code is reviewed before merging.
- Reviewers check: correctness, performance (no obvious recomposition/threading issues),
  alignment with the design system, and adherence to existing architecture patterns.
- Be kind and concrete in reviews; explain the reasoning behind requested changes.

## Issue reports

- **Search first** — the issue may already be tracked in
  [`docs/planning/CURRENT_ISSUES.md`](docs/planning/CURRENT_ISSUES.md) or on GitHub.
- For bugs, include: Kaiteyo version, platform/OS, steps to reproduce, expected vs.
  actual behavior, and logs if available.
- For feature proposals, describe the problem you're trying to solve and how you imagine
  the feature working — not just "add X".

## Contribution standards by area

### Code (Kotlin / Compose)

- Follow [`docs/development/CODING_STANDARDS.md`](docs/development/CODING_STANDARDS.md):
  4-space indent, 120-char lines, explicit imports, `val` over `var`.
- Follow the existing screen pattern: `{Feature}ScreenContract.kt`, `{Feature}ScreenViewModel.kt`,
  `{Feature}ScreenModule.kt`, `{Feature}Screen.kt`/`{Feature}ScreenUI.kt`, and register new
  modules in `core/.../di/AppModule.kt`.
- Verify a library is already used in the project before adding a new dependency; add new
  dependencies to the version catalog (`gradle/libs.versions.toml`), not as inline versions.
- Do not change: SRS algorithm logic, SQLDelight schemas (unless explicitly requested),
  the `ua.syt0r.kanji` namespace, or Gradle build configuration unless the build is broken.
- Add or update tests in `core/src/commonTest/` (or `desktopApp/src/jvmTest/`) for logic changes.

### UI

- Read [`docs/design/DESIGN_LANGUAGE.md`](docs/design/DESIGN_LANGUAGE.md) and
  [`docs/design/UI_SYSTEM.md`](docs/design/UI_SYSTEM.md) first.
- Follow the design tokens (`Ds*` components on desktop, theme tokens in core), the 4dp
  spacing grid, and the documented modifier order.
- Keep animations subtle, fast, and spring-based where the codebase already does so.

### Documentation

- Documentation is part of the definition of done — update it when behavior changes.
- New strings require editing both `EnglishStrings` and `JapaneseStrings` (the `Strings`
  interface enforces this).
- Record solved issues in `docs/troubleshooting/` and `docs/planning/CURRENT_ISSUES.md`.

### Data

- Data changes go through the KJD pipeline (`kjd/`), which ingests *openly licensed*
  datasets and generates the bundled language database. Never add third-party data to the
  repo without recording its source, license, and attribution requirements in
  `docs/data/SOURCES.md`.
- Do not claim ownership of externally sourced datasets; keep original Kaiteyo code/data
  distinct from third-party data.

## Third-party dependency rules

- New runtime dependencies must have a license compatible with GPL-3.0 (the project license).
- Prefer small, maintained libraries over big frameworks.
- Note new dependencies in the changelog and (where relevant) in
  [`docs/data/SOURCES.md`](docs/data/SOURCES.md) or the in-app credits
  (`core/credits/libraries/`).

## Licensing

Kaiteyo is licensed under **GPL-3.0** (see [`LICENSE`](LICENSE)). By contributing, you
agree that your contributions will be licensed under the project's license. External data
sources keep their own licenses; see [`docs/legal/README.md`](docs/legal/README.md).

## Code of conduct

Be respectful and constructive. Harassment, personal attacks, and dismissive behavior are
not tolerated. Use GitHub Issues for bug reports and feature requests, and GitHub
Discussions for questions and ideas.
