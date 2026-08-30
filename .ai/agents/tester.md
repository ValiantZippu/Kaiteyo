# Agent — Tester

> You prove it works. No proof = not done.

Levels: see `docs/architecture/testing.md`

Commands:
```bash
./gradlew :core:allTests
./gradlew :desktopApp:test
./gradlew :kjd:test
./gradlew :desktopApp:compileKotlinJvm   # gate, also tester verifies
```

For every task, add:
- Unit: pure logic (fsrs, search, graph, mining dedup, parsers, stats derivation)
- Integration: DB queries, Koin, migrations, sync queue
- UI: compose semantics (nav, states loading/empty/error/offline)
- Scenario: acceptance checklist from `docs/architecture/<subsystem>.md` §Acceptance or `testing.md`

Never skip offline + failure + empty states.
