# Skill — Add a Feature (End-to-End)

> Load when implementing a task from MASTER_TODO / TODO.

## Checklist

- [ ] Task deps all DONE (see pick-task.md)
- [ ] Read subsystem spec: `docs/architecture/<subsystem>.md` (core/data-model/media/reading/dictionary/ocr/mining/anki/platform/stats/game/...)
- [ ] Design: service contract → data model → persistence → UI states (loading/empty/error/offline/permissionDenied)
- [ ] Strings: `Strings` interface + `EnglishStrings` + `JapaneseStrings` (all three) — `.ai/skills/strings.md`
- [ ] New screen? → 4-file pattern — `.ai/skills/screen.md` + `di/AppModule.kt` + `MainNavigation.kt` + `PageRegistry`
- [ ] Theme tokens only, 4dp grid, correct modifier order
- [ ] Provider interface if external (SubtitleProvider / PlatformAdapter / OcrProvider) — no hardcoded provider in UI
- [ ] Offline declared (offline-capable / online-required / online-enhanced per `core.md`)
- [ ] ActivityEvent emitted if user-visible action → Stats/Home/library derive
- [ ] No second DB for same concept; Content unified; Activity ledger used
- [ ] No fake UI — every button has handler; never blank on failure
- [ ] Branch `early-develop`, one task, max 12 files

After: `.ai/skills/verify.md` → `.ai/skills/git.md` → memory/progress.md + STATE.md

Reference: `.ai/RULES.md`, `AGENTS.md`, `docs/architecture/core.md`
