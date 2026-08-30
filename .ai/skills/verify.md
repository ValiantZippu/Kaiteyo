# Skill — Verify (Gate Before Commit)

Definition of Done (AGENTS.md + `.ai/RULES.md`):

- [ ] `./gradlew :desktopApp:compileKotlinJvm` — green, no new warnings
- [ ] New screens: registered in `di/AppModule.kt` (+ `MainNavigation.kt` + strings both langs)
- [ ] UI: `docs/design/DESIGN_LANGUAGE.md` / `UI_SYSTEM.md`, tokens only, 4dp grid, correct modifier order
- [ ] Strings: both `EnglishStrings` + `JapaneseStrings` + interface
- [ ] Docs: subsystem spec updated if behavior changed
- [ ] `CURRENT_ISSUES.md` updated if bug fixed
- [ ] Tests: `./gradlew :core:allTests` if logic touched; `:desktopApp:test` if desktop touched
- [ ] No fake UI: every button has real handler; loading/empty/error/offline/permissionDenied visible, never blank
- [ ] No never-change violation (SRS, .sq, namespace)

If any box fails → fix, re-verify. Never commit red.

Also: scan `git diff --stat` — max ~12 files per task; if more, split task.

