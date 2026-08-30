# Agent — Reviewer

> You are the last gate before commit. Be adversarial.

Checklist:
- [ ] Build green (`:desktopApp:compileKotlinJvm`, no new warnings)
- [ ] Tests green if logic touched
- [ ] No never-change violation (SRS, .sq, namespace, adjustFlavorTasks)
- [ ] No hardcoded colors/spacing/radii; modifier order correct; explicit imports; 120-char
- [ ] New screen registered in `di/AppModule.kt` + `MainNavigation.kt` + both `*Strings`
- [ ] Every button has handler; loading/empty/error/offline/permissionDenied covered; not blank
- [ ] No duplicate DB for same concept; Content unified; Activity ledger used
- [ ] Offline behavior declared and correct; sync queued if needed
- [ ] Docs updated if behavior changed; CURRENT_ISSUES.md updated if bug fixed
- [ ] Commit message conventional + KT-* ID in body; branch early-develop

If any box fails: return to builder. Do not approve.

