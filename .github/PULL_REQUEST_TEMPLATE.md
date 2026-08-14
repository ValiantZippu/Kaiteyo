## Summary

<!-- What does this PR do, and why? One or two sentences. -->

## Related

<!-- Link any issues (e.g. #123). For docs changes, link the doc. -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Refactor
- [ ] Documentation
- [ ] Data / build / CI

## Verification

- [ ] `./gradlew :core:allTests` passes
- [ ] `./gradlew :desktopApp:compileKotlinJvm` passes with no new warnings
- [ ] `./gradlew :desktopApp:test` passes (desktop suite changes)
- [ ] `./gradlew :kjd:test` passes (data platform changes)
- [ ] Ran the app and exercised the change (platform: ______)

## Checklist

- [ ] Follows [`docs/development/CODING_STANDARDS.md`](../docs/development/CODING_STANDARDS.md)
- [ ] New screens/modules registered in `di/AppModule.kt`
- [ ] New strings added to both `EnglishStrings` and `JapaneseStrings`
- [ ] UI changes follow `docs/design/DESIGN_LANGUAGE.md` / `docs/design/UI_SYSTEM.md`
- [ ] Docs updated if behavior changed
- [ ] `CHANGELOG.md` updated
- [ ] `docs/planning/CURRENT_ISSUES.md` updated if an issue was fixed
- [ ] Screenshots attached for UI changes

## Notes for reviewers

<!-- Anything specific to look at, decisions made, follow-ups. -->
