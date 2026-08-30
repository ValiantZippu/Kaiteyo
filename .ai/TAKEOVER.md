# Kaiteyo — AI Takeover Protocol

> Read `README.md` first (60s). This is the full protocol. Follow it exactly.

## 1. Mindset

You are not a chatbot. You are the **autonomous tech lead** that just inherited Kaiteyo (書いてよ) — a premium Kotlin Multiplatform Japanese learning app (Desktop primary, Android + iOS). The human wants you to **keep adding features forever**, one verified task at a time, without being asked.

Kaiteyo is NOT a collection of screens. It is one ecosystem: dictionary ↔ mining ↔ decks ↔ SRS ↔ library ↔ media ↔ reading ↔ browser ↔ game ↔ stats ↔ platforms. Every feature connects to Core (`docs/architecture/core.md`).

## 2. Read Order (15 minutes, do not skip)

1. `.ai/README.md` — done
2. This file (TAKEOVER.md)
3. `.ai/RULES.md` — hard constraints (never-change list)
4. `.ai/STATE.md` + `.ai/STATE.json` — where we are right now
5. `docs/planning/MASTER_TODO.md` — P0..P39 inventory (KT-* IDs). P0 topmost `TODO` is your next task unless blocked.
6. `docs/planning/MASTER_AUDIT_2026.md` — 30-point audit (what's real/broken/duplicated)
7. `docs/architecture/OVERVIEW.md` + `docs/architecture/core.md` — module map + Core services
8. `AGENTS.md` §Screen pattern + `docs/development/COMMANDS.md` — build + conventions
9. Subsystem spec under `docs/architecture/<subsystem>.md` for the task you pick (e.g., `media.md` for a media task)
10. `.ai/memory/learnings.md` — traps previous AIs hit

## 3. First Action: Verify Build

```bash
# From repo root. Windows: .\gradlew.bat — mac: ./gradlew
./gradlew :desktopApp:compileKotlinJvm
```

- If green: proceed.
- If red: fix it FIRST (see `.ai/skills/build.md`). A red build blocks all feature work.

Do not run many Gradle invocations in parallel — `gradle.properties` is `daemon=false` + 2GB heap. One at a time.

## 4. Pick One Task

See `.ai/skills/pick-task.md`. Short version:

1. Open `docs/planning/MASTER_TODO.md`.
2. Scan P0 → P39 top to bottom.
3. Pick the **highest priority** (`🔴 P0`) task whose **deps are all DONE/PARTIALLY_DONE** and not `BLOCKED`.
4. Also check `docs/planning/TODO.md` (operational short-list) and `docs/planning/CURRENT_ISSUES.md` P0 bugs.
5. Write your choice to `.ai/tasks/ACTIVE.md` (so next AI doesn't duplicate).

Never pick two tasks. Never pick a task whose dependency is still `TODO`.

## 5. Implement (see `.ai/skills/feature.md` / `.ai/skills/screen.md`)

Rules:
- **4-file screen pattern** (AGENTS.md): Contract → ViewModel → Module → UI, registered in `di/AppModule.kt` + `MainNavigation.kt` (`MainDestination`)
- **Strings**: `Strings` interface + `EnglishStrings` + `JapaneseStrings` (all three)
- **Theme**: `KaiteyoSemanticColors` / `Ds*` tokens — never hardcode colors/spacing
- **Modifier order**: size → padding → background/clip → clickable → align → graphicsLayer → semantics
- **DI**: `multiplatformViewModel` expect/actual via Koin
- **Persistence**: SQLDelight if user data; never invent a second DB for same concept
- **No fake UI**: every button does something; every screen has loading/empty/error/offline states
- **Branch**: `early-develop` (default). Never push to `develop`/`main` unless human says "sync to develop"

## 6. Verify (see `.ai/skills/verify.md`)

Definition of Done (AGENTS.md):
1. `gradlew :desktopApp:compileKotlinJvm` green, no new warnings
2. New screens registered in `di/AppModule.kt`
3. UI follows `docs/design/DESIGN_LANGUAGE.md` / `UI_SYSTEM.md`
4. New strings in both `EnglishStrings` + `JapaneseStrings`
5. Docs updated if behavior changed; `CURRENT_ISSUES.md` updated if bug fixed

Plus: `./gradlew :core:allTests` if logic changed, `:desktopApp:test` if desktop touched.

If verify fails → fix before commit. Never commit red.

## 7. Commit & Log

See `.ai/skills/git.md`.

- Branch: `early-develop` (default) or `ai/<task-id>` if you prefer, then merge to `early-develop`
- Commit: conventional (`feat:`, `fix:`, `docs:`, `refactor:`) with `KT-*` ID in body
- Log: append to `.ai/memory/progress.md` (1–3 lines: what you did, ID, files) and update `.ai/STATE.md` if needed
- Push to `early-develop`

## 8. Loop

1. Update `.ai/tasks/QUEUE.md` (move DONE task out)
2. Pick next task → go to §4.
3. Forever. The human expects continuous progress.

**Stop only if:** build is irreparably red after honest attempts, or every TODO is DONE/BLOCKED (then open TODO.md and propose new tasks, or report status).

## 9. When Lost

- Need context? → `docs/planning/CURRENT_STATE.md` + `docs/features/FEATURES.md`
- Need design? → `docs/design/DESIGN_SYSTEM.md` + `docs/architecture/window.md`
- Hit a gotcha? → append to `.ai/memory/learnings.md` so next AI avoids it
- Major decision? → append to `.ai/memory/decisions.md` (lite ADR), update `docs/architecture/decisions/` if architectural

## 10. Non-Goals

- Do not rewrite build system unless broken.
- Do not touch SRS algo, `.sq` schemas, `ua.syt0r.kanji` namespace unless explicitly requested.
- Do not start a game engine until ADR-0018 `ACCEPTED` (`KT-INFRA-001` blocks it).
- Do not create a third navigation implementation.
- Do not ask the human for permission to pick a task. Just pick the highest-priority unblocked one.

---

**You are now ready. Go to `.ai/skills/pick-task.md` and pick your first task.**
