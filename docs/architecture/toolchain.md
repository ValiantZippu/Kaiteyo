# Kaiteyo Architecture — Toolchain & Development Environment

**Status**: Implemented and pinned (§167 — no "latest" for critical deps)
**Owner**: repository root (Gradle config) + `docs/development/`
**Related**: `docs/development/DEVELOPMENT_SETUP.md` · `docs/development/COMMANDS.md` ·
`docs/cli/README.md` · `docs/maintenance/DependencyUpdates.md` · `docs/architecture/ci-cd.md`

## 1. Pinned toolchain

| Tool | Version | Where pinned |
|---|---|---|
| JDK | 17 (`jvmToolchain(17)` everywhere) | each module `build.gradle.kts` |
| Kotlin | 2.1.20 | `gradle/libs.versions.toml` `[versions]` **and** `settings.gradle.kts` `pluginManagement` |
| Kotlin language/api version | `KOTLIN_2_1` (compilerOptions) | each module `build.gradle.kts` |
| Compose Multiplatform | 1.8.2 | catalog + `settings.gradle.kts` (`org.jetbrains.compose`) |
| Android Gradle Plugin | 8.5.2 | `settings.gradle.kts` (literal) |
| SQLDelight | 2.0.2 | catalog + settings plugin |
| Kotlin serialization | 1.8.0 | catalog |
| kotlinx-datetime | 0.6.1 | catalog |
| Koin | 4.0.0 | catalog |
| Ktor | 3.1.2 | catalog |
| Gradle | wrapper (`gradlew`/`gradlew.bat`) | `gradle/wrapper/` |
| Git | any modern | repo policy |

**Critical rule**: `settings.gradle.kts` `pluginManagement` holds **literal** plugin
versions (the version catalog is not accessible inside `pluginManagement`) — keep them in
sync with `[versions]` in `gradle/libs.versions.toml`. Every dependency change is
recorded in `docs/maintenance/DependencyUpdates.md` with reason/verification/migration
notes (§203).

## 2. Modules (include set)

`settings.gradle.kts`: `:app` (Android), `:iosApp`, `:desktopApp`, `:core` (shared KMP),
`:mediaGenerator` (javacv/coil asset generation), `:kjd` (data platform). `buildSrc/`
provides `AppVersion` + `AppAssets` (not a project module). `installer/` is a script/config
subsystem, not a Gradle module (ADR-0010). `website/` is a Python build, unrelated to the
Gradle build.

## 3. Build environment

- `gradle.properties`: `org.gradle.daemon=false`, conservative 2 GB heap (8 GB dev
  machine). **Builds are slow — do not launch many Gradle invocations in parallel.**
- iOS targets cannot build on Windows — expected; `kotlin.native.ignoreDisabledTargets=true`.
- Android needs `ANDROID_HOME`/`ANDROID_SDK_ROOT` + machine-local `local.properties`
  (`sdk.dir=...`), never committed. Setup snippet in `docs/development/COMMANDS.md`.
- Asset download tasks fetch missing assets from GitHub releases on first build —
  **needs network** (see `docs/architecture/assets.md`).

## 4. WSL notes (§239)

The repo may live on a 9p (drvfs) mount where Gradle's `CachingFileHasher` can fail with a
bare `java.io.IOException: I/O error` when the project `.gradle` cache holds files left by
a live daemon (9p refuses to delete/open locked files). Recovery:
1. Kill stray `GradleDaemon` processes.
2. Delete the project `.gradle` directory **while no daemon holds it**.
3. Prefer a Linux-side `GRADLE_USER_HOME` (e.g. `/root/.gradle-kaiteyo` — the repo's
   `scratch/run-gradle.sh` documents this) to avoid drvfs journal problems entirely.

## 5. Editors

- JetBrains IDEs for Kotlin/JVM-heavy work (§168).
- VS Code for docs/web/scripts/JSON/YAML/Markdown/asset organization.
- No IDE run configurations ship in the repo (stale `.run/` configs removed). Run
  `gradlew :desktopApp:run` from the terminal; add `-Duser.language=ja -Duser.country=JP`
  for the Japanese UI locale.

## 6. Validation ladder (§342)

Cheapest useful validation first: format/lint (L1) → static analysis (L2) → targeted unit
test (L3) → targeted integration test (L4) → module build (L5) → full build (L6) → full
e2e (L7). Documentation/planning passes do **not** build (§341). Compile targets:
`:desktopApp:compileKotlinJvm` (desktop loop), `:core:allTests` (shared engine),
`:desktopApp:test` (desktop suite), `:kjd:test` (data platform).

## 7. Developer CLI

`docs/cli/` documents the `kaiteyo` command center. Per §236–§238 the command set must be
**generated from the real toolchain** — never invent commands. Categories: setup,
development, testing, database, import/export, formatting, lint, profiling, packaging,
release, git, WSL (§343). Planned/future: `kaiteyo dev doctor` (Java/Gradle/Git/Android
SDK/NDK/media deps/env/disk/permissions with PASS/WARN/FAIL + remediation, §240), task
discovery wrapper (§238).

## 8. Reproducibility (§344)

Same revision → approximately same dependencies, generated files, database schema, build
output: pinned catalog + literal plugin versions, JDK toolchains, managed assets with
checksums (AppAssets), versioned SQLDelight migrations, `org.gradle.daemon=false`.

## 9. Open items

- `kaiteyo dev doctor` implementation (spec §240).
- Gradle task discovery wrapper (§238).
- Profiling toolchain runbook (JFR/JMC/Android Studio Profiler) — see
  `docs/architecture/performance.md`.

## 10. Node & content tooling (TARGET — ADR-0013, ADR-0015)

- **Schema generation**: node/edge/knowledge schemas (NODE_DATA_MODEL) are added as
  SQLDelight interfaces — regenerate via the existing `:core:generateCommonMain*
  DatabaseInterface` tasks (AGENTS.md).
- **Registry tooling**: a small lint/validation command validates authored content
  against NODE_TYPE_REGISTRY / RELATIONSHIP_REGISTRY (the §148 schema gate) — the
  authoring pipeline's first gate, runnable in CI (§7).
- **Package tooling**: `kaiteyo` CLI gains package build/validate/install commands
  (from the real toolchain, §343 — never invented commands); `kaiteyo dev doctor`
  (§240) later checks world-package prerequisites.
- **Game tooling** (with the Journey phase): Blender → LOD/compression pipeline
  (STANDARDS §245–§246); asset validation reuses the AppAssets discipline extended to
  packages.
- Reproducibility extends to packages: manifest + content hash + pinned engine version
  (§145, §344).
