# Skill — Build & Test

> Load before writing code (verify green baseline) and after (verify your change).

Commands (repo root; Windows: `.\gradlew.bat`):

```bash
# 1. Compile desktop (gate)
./gradlew :desktopApp:compileKotlinJvm

# 2. Tests if you touched logic
./gradlew :core:allTests           # shared KMP logic
./gradlew :desktopApp:test         # desktop suite
./gradlew :kjd:test                # data platform

# 3. Run (optional, not in headless)
./gradlew :desktopApp:run          # needs display
```

Gotchas:
- `gradle.properties`: `daemon=false`, 2GB heap — builds slow, don't parallelize.
- JDK 17 required (`jvmToolchain(17)`). iOS targets ignored on Windows (expected).
- `core/src/.../composeResources/files/` is managed — files not in `AppAssets.kt` are deleted. Never drop there.
- Plugin versions literal in `settings.gradle.kts` ↔ `gradle/libs.versions.toml` must sync.

If red: read compiler output bottom-up, fix imports/types/screens registration/brace balance, re-run one command at a time.

Reference: `docs/development/COMMANDS.md`, `.ai/memory/learnings.md`
