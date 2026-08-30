# Kaiteyo — Current State (for AIs)

> Human-readable pointer. Machine-readable: `STATE.json`. Keep both in sync.

## Head

- **Branch**: `early-develop`
- **Version**: `2.2.1` (2210, AppData v15) — `buildSrc/AppVersion.kt`
- **Kotlin**: 2.1.20 / Compose 1.8.2 / JDK 17
- **Build**: `gradlew :desktopApp:compileKotlinJvm` — expected green (2GB heap, slow)
- **Last audit**: `docs/planning/MASTER_AUDIT_2026.md` (2026-08-30, HEAD 6e7fcf7d)

## Where We Are

- **Core app** (`core/` + `KaiteyoApp` + `NavShell`): Library/SRS/Stats/Exams/Settings/Onboarding/Base themes — `IMPLEMENTED`, ships.
- **Desktop suite** (`desktopApp/.../desktop/`): Dictionary/Media/Mining/OCR/Browser/Reading — `IMPLEMENTED` but suite-only; Media mounted as `DesktopMediaCentreContent` (`MainDestination.Media`), rest pending ADR-0017.
- **P0 bugs**: hover/anim stutter, resize glitches, spacing 4dp drift — see `CURRENT_ISSUES.md` #1–4.
- **Master architecture pack**: `docs/architecture/{core,data-model,media,subtitles,browser,reading,ocr,dictionary,mining,anki,events,platforms,stats,library,browse,home,game,sync,security,navigation,window,debug,testing}.md` + `integrations/{yomitan,asbplayer,jidoujisho,chromium,anilist,myanimelist}` + `planning/ROADMAP.md` (phases 0–7).

## Next Task (highest P unblocked)

Check `docs/planning/MASTER_TODO.md` P0 topmost `TODO` whose deps are `DONE`. As of 2026-08-30: `KT-INFRA-001` (ADR-0017 one-product decision) gates all consolidation; if you cannot make product decisions, pick the topmost `P0` bug (anim stutter #1) or the topmost `TODO` not blocked (mount remaining suite views or OCR guided setup `KT-DESK-*`).

Also see `docs/planning/TODO.md` operational short-list and `docs/planning/CURRENT_ISSUES.md` P0.

## Gates

- `KT-INFRA-001` (ADR-0017) — blocks all duplication removal / suite→Core migration.
- `KT-INFRA-003` (demo-data removal) — follow-up once ADR-0017 lands.
- `ADR-0018` (game engine) — blocks Journey runtime code.
- `ADR-0020` (Chromium JCEF) — proposed gate, not yet ADR.

## What An AI Should Do Now

1. Verify build green.
2. Pick next task per `.ai/skills/pick-task.md`.
3. Implement per `.ai/skills/feature.md`.
4. Verify per `.ai/skills/verify.md`.
5. Commit per `.ai/skills/git.md`.
6. Log to `.ai/memory/progress.md` + update this file.
7. Loop.

## Recent Progress

> AIs: append 1–3 lines per completed task below (newest first). Keep last 20.

- 2026-08-30 — Master architecture + 30-point audit pack (docs/architecture/*.md + integrations) — source-only, `early-develop`.
- 2026-08-30 — `.ai/` autonomous takeover core created — this file.
- (prior: see `docs/planning/CURRENT_ISSUES.md` DONE history + `.ai/memory/progress.md`)
