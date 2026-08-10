# Updating Dependencies

Dependencies are centralized in `gradle/libs.versions.toml`; plugin versions are in `settings.gradle.kts`.

1. Change one related version group at a time.
2. Run `./gradlew :core:compileKotlinJvm`.
3. Run `./gradlew :desktopApp:compileKotlinJvm`.
4. Run `./gradlew :app:assembleDebug` when Android dependencies changed.
5. Record compatibility notes in [Dependency Updates](../maintenance/DependencyUpdates.md) and [Version History](../maintenance/VersionHistory.md).
6. Update troubleshooting pages if a new failure is discovered.

Use `--refresh-dependencies` only to diagnose cache state. Never commit generated caches or secrets.

Related: [Gradle](../troubleshooting/Gradle.md), [First Build](FirstBuild.md), [Command Library](../development/COMMANDS.md).
