# Dependency Updates

Record dependency changes here with date, reason, verification, and any migration or platform notes. The current source of truth is `gradle/libs.versions.toml` plus plugin versions in `settings.gradle.kts`.

## 2026-08-01 Baseline

- Kotlin Multiplatform/plugin configuration: 2.1.20 in `settings.gradle.kts`; version catalog Kotlin library: 2.0.21. Verify both before changing either.
- Compose Multiplatform: 1.8.2.
- Android Gradle Plugin: 8.5.2.
- SQLDelight: 2.0.2.
- JDK baseline: 17.

Verification performed: `:core:compileKotlinJvm` completed successfully.

## Update Record Template

```markdown
### YYYY-MM-DD - <dependency>

Previous: <version>
New: <version>
Reason: <security, bug fix, compatibility, or feature>
Verification: <exact commands and result>
Migration notes: <required code/config changes>
Related troubleshooting: <links>
```

Related: [Updating Dependencies](../setup/UpdatingDependencies.md), [Gradle](../troubleshooting/Gradle.md), [Version History](VersionHistory.md).
