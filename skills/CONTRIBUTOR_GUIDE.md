# Contributor Guide

> How to contribute to Kaiteyo — properly, professionally, and without breaking things.

---

## Quick Start

```bash
# 1. Fork & clone
git clone https://github.com/<you>/Kaiteyo.git
cd Kaiteyo

# 2. Verify it builds (JDK 17 required)
./gradlew :desktopApp:compileKotlinJvm

# 3. Create a branch
git checkout develop
git checkout -b feature/your-feature

# 4. Make changes, test
./gradlew :core:allTests

# 5. Commit with conventional message
git commit -m "feat(scope): description"

# 6. Push & PR
git push origin feature/your-feature
# Open PR → target develop
```

---

## Branch Naming

| Prefix | When | Example |
|--------|------|---------|
| `feature/` | New feature | `feature/dictionary-popup` |
| `fix/` | Bug fix | `fix/window-drag` |
| `docs/` | Documentation | `docs/architecture-guide` |
| `refactor/` | Code restructuring | `refactor/settings-module` |
| `release/` | Release prep | `release/v2.3.0` |

**Never** use: `fix1`, `test`, `asdf`, `new-stuff`. Be descriptive.

---

## Commit Convention

```
type(scope): description
```

| Type | Use For |
|------|---------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation |
| `refactor` | Code restructuring (no behavior change) |
| `perf` | Performance improvement |
| `test` | Adding/updating tests |
| `chore` | Build, deps, CI, tooling |
| `style` | Formatting (no logic change) |

**Examples:**
```
feat(media): add subtitle phrase selection
fix(nav): preserve bubble position across restarts
docs(architecture): document knowledge graph
refactor(statistics): unify dashboards into one screen
```

**Rules:**
- First line under 72 characters
- Use imperative mood ("add" not "added")
- Explain *why* in the body when the change isn't obvious
- Reference issues: `Fixes #123`

---

## Code Standards

Read [`docs/development/CODING_STANDARDS.md`](docs/development/CODING_STANDARDS.md) fully. Key rules:

- **4-space indent**, 120-char max line width
- **Explicit imports** — no wildcard `import foo.*`
- **`val` over `var`** — immutable by default
- **`data class`** for state, **`sealed class`** for hierarchies
- **Modifier order:** size → padding → background/clip → clickable → align → graphicsLayer → semantics
- **No hardcoded colors** — use theme tokens (`Ds*` on desktop, `AppTheme` in core)
- **Spring animations** — no hard-coded durations unless via `tweenDuration`

---

## Screen Pattern (Most Important Convention)

Every feature screen follows **4 files** in `core/src/commonMain/kotlin/ua/syt0r/kanji/presentation/screen/main/screen/<feature>/`:

```
FeatureScreenContract.kt    → Interface (ViewModel + state)
FeatureScreenViewModel.kt   → Implementation
FeatureScreenModule.kt      → Koin DI module
FeatureScreen.kt / UI.kt    → Composables
```

**Register new modules** in `core/.../di/AppModule.kt` → `screenModules` list.

---

## What NOT to Change

These are protected. Do not touch unless explicitly asked:

| Area | Why |
|------|-----|
| SRS algorithm logic | Core learning logic — changes affect all users' study progress |
| SQLDelight `.sq` schemas | Database migrations are versioned and sensitive |
| `ua.syt0r.kanji` namespace | Project identity — renaming breaks everything |
| Gradle build config | Unless the build is actually broken |
| `adjustFlavorTasks()` in app/build.gradle.kts | F-Droid reproducible builds |

---

## Testing

```bash
# Run all shared tests
./gradlew :core:allTests

# Compile desktop
./gradlew :desktopApp:compileKotlinJvm

# Android debug build
./gradlew :app:assembleDebug
```

**Always run tests before pushing.** If you break the build, fix it before opening a PR.

---

## PR Checklist

Before opening a PR, verify:

- [ ] `./gradlew :desktopApp:compileKotlinJvm` passes
- [ ] `./gradlew :core:allTests` passes (if you touched logic)
- [ ] No new compiler warnings
- [ ] New screens registered in `AppModule.kt`
- [ ] New strings added to both `EnglishStrings` and `JapaneseStrings`
- [ ] Docs updated if behavior changed
- [ ] `CURRENT_ISSUES.md` updated if you fixed a bug
- [ ] Branch is up to date with target
- [ ] PR has clear title and description
- [ ] Screenshots included for UI changes

---

## Adding Dependencies

1. Check if a similar library is already used
2. Must be GPL-3.0 compatible
3. Add to `gradle/libs.versions.toml` (version catalog)
4. Reference via catalog, never inline version numbers
5. Note in changelog and `docs/data/SOURCES.md` if relevant

---

## Adding Strings (i18n)

Strings are interface-based, not resource files:

```kotlin
// 1. Add to the interface
interface Strings {
    val myNewString: String
}

// 2. Add English implementation
class EnglishStrings : Strings {
    override val myNewString = "Hello"
}

// 3. Add Japanese implementation
class JapaneseStrings : Strings {
    override val myNewString = "こんにちは"
}
```

**Both implementations are required** — the interface enforces it.

---

## Getting Help

| Need | Where |
|------|-------|
| Architecture questions | [`docs/architecture/OVERVIEW.md`](docs/architecture/OVERVIEW.md) |
| Build problems | [`docs/troubleshooting/`](docs/troubleshooting/README.md) |
| Design questions | [`docs/design/DESIGN_LANGUAGE.md`](docs/design/DESIGN_LANGUAGE.md) |
| AI assistant help | [`docs/ai/AI_AGENT_GUIDE.md`](docs/ai/AI_AGENT_GUIDE.md) |
| Current bugs | [`docs/planning/CURRENT_ISSUES.md`](docs/planning/CURRENT_ISSUES.md) |
| Roadmap | [`docs/roadmap/ROADMAP.md`](docs/roadmap/ROADMAP.md) |

---

## Code of Conduct

- Be respectful and constructive
- No harassment, personal attacks, or dismissive behavior
- Use GitHub Issues for bugs/features, Discussions for questions
- Review code kindly — explain reasoning, not just "change this"
