# ADR-0006: FSRS-5 for Spaced Repetition

**Status**: Accepted

## Context

Kaiteyo's review scheduling is the core learning loop. The project needed a modern,
research-backed spaced-repetition algorithm that is deterministic, offline, and works the
same on every platform.

## Decision

- Use **FSRS-5** (Free Spaced Repetition Scheduler) as the scheduling algorithm,
  implemented in shared Kotlin (`core/.../srs/fsrs/` — `FsrsAlgorithm.kt`,
  `FsrsScheduler.kt`, `DefaultFsrsScheduler(Fsrs5())`).
- Wrap it in Kaiteyo's own SRS layer: `SrsManager`, letter/vocab SRS managers, daily
  limit manager, and review flows (grade → reschedule → persist).
- SRS state is persisted in the user database (FSRS card state: ease, stability,
  difficulty, due, reps, lapses).

## Alternatives

- SM-2 (classic Anki algorithm) — rejected: older, fixed intervals; FSRS-5 adapts to
  individual performance.
- A custom algorithm — rejected: reinventing scheduling research.
- Server-side scheduling — rejected: offline-first requirement.

## Consequences

- Scheduling is deterministic and offline; cards transfer cleanly with import/export.
- This is "never change" territory: SRS algorithm logic and core learning logic are not
  to be modified casually (`development/AI_CONTEXT.md`).

## Implementation notes

- `core/src/commonMain/kotlin/ua/syt0r/kanji/core/srs/` (fsrs/ subpackage).
- Tests: `core/src/commonTest/kotlin/FsrsSchedulerTest.kt`.
- Desktop suite has its own review queue engine (`desktop/engine/review/`) that builds on
  the same scheduling concepts.
