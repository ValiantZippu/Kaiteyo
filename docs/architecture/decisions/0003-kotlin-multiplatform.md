# ADR-0003: Kotlin Multiplatform + Compose Multiplatform

**Status**: Accepted
**Date**: fork era (continuous with upstream Kanji Dojo)

## Context

Kaiteyo targets Windows, macOS, Linux, Android, and iOS from a single codebase. The fork
inherited a Kotlin Multiplatform project (from Kanji Dojo) and the decision to keep that
architecture was reaffirmed during the Kaiteyo redesign: a single study engine must behave
identically everywhere, and the desktop suite must share the engine.

## Decision

- Use **Kotlin Multiplatform** with a shared `core` module (`commonMain` + platform
  source sets: `jvmMain`, `androidMain`, `iosMain`).
- Use **Compose Multiplatform** for UI, shared from `core` across all targets.
- Keep platform entry points thin: `desktopApp/` (JVM), `app/` (Android), `iosApp/`
  (Swift host + Compose).
- Pin the toolchain: JDK 17 (`jvmToolchain(17)`), Kotlin language/API version 2.1, Compose
  1.8.2, versions catalog in `gradle/libs.versions.toml`.

## Alternatives

- Separate native UIs per platform — rejected: duplicated study engine and UI, higher
  maintenance.
- KMP for logic + per-platform UI — considered viable but Compose MPP matured enough to
  share UI too; sharing the UI keeps the design system consistent.
- A web-first implementation — rejected: offline-first desktop immersion is the core use
  case.

## Consequences

- One study engine everywhere; feature parity is easier to maintain.
- Desktop is the flagship; iOS is secondary (needs a macOS host; some platform actuals are
  verified manually).
- New contributors must learn KMP expect/actual patterns.

## Implementation notes

- `settings.gradle.kts`: modules `app`, `iosApp`, `desktopApp`, `core`, `mediaGenerator`,
  `kjd`.
- `core/build.gradle.kts`: jvm + android + ios targets, `jvmToolchain(17)`,
  `languageVersion`/`apiVersion` KOTLIN_2_1.
