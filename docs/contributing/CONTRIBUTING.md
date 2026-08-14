# Contributing to Kaiteyo

The canonical contributing guide lives at the **repository root**:
[`CONTRIBUTING.md`](../../CONTRIBUTING.md)

It covers the branch strategy, commit conventions, pull request expectations, issue
reporting, and the contribution standards for code, UI, documentation, and data.

This page exists so the guide is also reachable from the documentation site. Everything
below is a condensed summary — read the root file for the full picture.

## At a glance

- **Branch from `develop`** — PRs target `develop`, never `main`.
- **Conventional commits** — `feat:`, `fix:`, `docs:`, `refactor:`, `perf:`, `test:`, `chore:`.
- **Checks must pass** — `./gradlew :core:allTests` and `./gradlew :desktopApp:compileKotlinJvm`.
- **Read the docs first** — `docs/development/CODING_STANDARDS.md` and
  `docs/development/AI_CONTEXT.md` (which includes the "never change" list: SRS logic,
  SQLDelight schemas, the `ua.syt0r.kanji` namespace, Gradle configuration).
- **Documentation is part of the definition of done** — update docs, changelog, and issue
  tracker when behavior changes.
- **Data must be openly licensed** — record source, license, and attribution in
  `docs/data/SOURCES.md`; never claim ownership of third-party datasets.
- **License** — GPL-3.0; contributions are licensed under the project's license.
