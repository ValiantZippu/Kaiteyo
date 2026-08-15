# Architecture Decision Records

Architecture Decision Records (ADRs) capture the significant decisions behind Kaiteyo's
architecture — *why* it is built the way it is — so future contributors (human or AI) can
understand the trade-offs without re-deriving them.

## How to add an ADR

1. Pick the next free number (`NNNN-`).
2. Use the template below (Title, Status, Context, Decision, Alternatives, Consequences,
   Implementation notes).
3. Mark the status honestly:
   - **Accepted** — the decision is implemented in the codebase
   - **Proposed** — the decision is agreed but not implemented
   - **Deprecated** / **Superseded by NNNN** — replaced by a later decision
4. Add it to the list below and to the docs map (`docs/README.md`).

## Template

```markdown
# ADR-NNNN: <short title>

**Status**: Accepted | Proposed | Deprecated
**Date**: YYYY-MM

## Context
Why this decision was needed.

## Decision
What was decided.

## Alternatives
What else was considered and why it was rejected.

## Consequences
What this decision means for the project going forward.

## Implementation notes
Where in the codebase this decision is realized.
```

## Index

| ADR | Title | Status |
|---|---|---|
| [0001](0001-brand.md) | Kaiteyo brand identity (fork of Kanji Dojo) | Accepted |
| [0002](0002-theme.md) | Token-based theme system | Accepted |
| [0003](0003-kotlin-multiplatform.md) | Kotlin Multiplatform + Compose Multiplatform for all platforms | Accepted |
| [0004](0004-shared-ui-and-screen-pattern.md) | Shared UI in `core` with the 4-file screen pattern + Koin DI | Accepted |
| [0005](0005-sqldelight-two-databases.md) | Two SQLDelight databases (immutable app data / mutable user data) | Accepted |
| [0006](0006-fsrs-srs.md) | FSRS-5 for spaced repetition | Accepted |
| [0007](0007-kjd-data-platform.md) | KJD — standalone data platform generates the bundled language database | Accepted |
| [0008](0008-desktop-suite.md) | Desktop immersion suite as a self-contained module in `desktopApp` | Accepted |
| [0009](0009-github-sync-and-account.md) | GitHub device-flow OAuth + private-gist sync (no central service) | Accepted |
| [0010](0010-installer-decoupling.md) | Installer subsystem decoupled from the Gradle build | Accepted |
| [0011](0011-plugin-security-first.md) | Plugin runtime loading deferred (security first) | Accepted |
| [0012](0012-engineering-standards.md) | Professional engineering standards adopted (§163–§376) | Accepted |
| [0013](0013-node-architecture.md) | Node-based architecture as the connective tissue (§76–§162) | Proposed |
| [0014](0014-journey-target-architecture.md) | Journey as target architecture (possible separate runtime, vertical slice first) | Proposed |
| [0015](0015-content-authoring.md) | Data-driven content authoring with hard validation gates | Proposed |
| [0016](0016-event-driven-user-knowledge.md) | Event-driven user knowledge (dimensioned, FSRS-owned scheduling) | Proposed |
| [0017](0017-one-product-architecture.md) | One product — resolve the two parallel applications | Proposed |
| [0018](0018-game-engine-evaluation.md) | Game engine evaluation (no Journey code before acceptance) | Proposed |
| [0019](0019-website-command-center.md) | Website as project command center (corpus-first render, documented backend contracts) | Proposed |
