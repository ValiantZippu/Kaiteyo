# AI Skills & Reference

> Complete guide for AI assistants working on Kaiteyo. Read this before making any changes.

---

## 🧠 What AI Can Do

| Skill | Scope |
|-------|-------|
| **Fix bugs** | Read error, find root cause, fix code, verify build |
| **Add features** | Follow screen pattern, register module, add strings |
| **Refactor code** | Extract, rename, restructure — verify no behavior change |
| **Write docs** | Architecture, guides, API docs, troubleshooting |
| **Update dependencies** | Via version catalog, verify compatibility |
| **Create screens** | 4-file pattern + DI registration |
| **Add strings** | Interface + English + Japanese implementations |
| **Fix build errors** | Read compiler output, fix imports/types/syntax |
| **Resolve merge conflicts** | Analyze both sides, pick correct resolution |
| **Run tests** | Execute test suites, read failures, fix |

---

## 🚫 What AI Must NEVER Do

| Rule | Why |
|------|-----|
| Never change SRS algorithm | Core learning logic — affects all users' study data |
| Never change `.sq` schemas | Database migrations are versioned and irreversible |
| Never rename `ua.syt0r.kanji` | Project namespace — breaks everything |
| Never edit `buildSrc/AppVersion.kt` | Version bumps are manual, deliberate decisions |
| Never remove `adjustFlavorTasks()` | F-Droid reproducible builds requirement |
| Never commit secrets | API keys, passwords, tokens must never be in code |
| Never force-push to `main` | Production safety |
| Never delete git history | Preserve project history |
| Never add Python/pip to build steps | Deploy is Node.js-only image |
| Never start/stop dev servers | Platform manages dev servers |
| Never hand-edit `_generated` files | Regenerate with `convex dev --once` |

---

## 📁 Key Files to Read Before Editing

| Area | File |
|------|------|
| **Always first** | `docs/development/AI_CONTEXT.md` |
| **Build commands** | `docs/development/COMMANDS.md` |
| **Current bugs** | `docs/planning/CURRENT_ISSUES.md` |
| **Architecture** | `docs/architecture/OVERVIEW.md` |
| **Screen pattern** | `AGENTS.md` (Screen pattern section) |
| **Design system** | `docs/design/DESIGN_LANGUAGE.md` |
| **Coding style** | `docs/development/CODING_STANDARDS.md` |
| **Desktop suite** | `docs/features/DESKTOP.md` |
| **Data sources** | `docs/data/SOURCES.md` |

---

## 🔧 AI Workflow

### Before Making Changes
1. Read `AI_CONTEXT.md` for the never-change list
2. Read `CURRENT_ISSUES.md` for known bugs
3. Read the subsystem spec under `docs/architecture/` for the area you're touching
4. Understand the screen pattern if adding a new screen

### While Making Changes
1. Make minimal, focused changes
2. Follow existing code style exactly
3. Use existing libraries — check before adding new ones
4. Follow the modifier order convention
5. Use theme tokens, never hardcoded colors

### After Making Changes
1. Verify: `./gradlew :desktopApp:compileKotlinJvm`
2. Run tests if logic changed: `./gradlew :core:allTests`
3. Update docs if behavior changed
4. Update `CURRENT_ISSUES.md` if you fixed a bug
5. Push to `early-develop` (never `develop` or `main`)

---

## 🎯 AI Decision Tree

```
User asks to fix a bug
  → Read CURRENT_ISSUES.md
  → Find the relevant code
  → Fix it
  → Verify build passes
  → Push to early-develop

User asks to add a feature
  → Determine if it's a new screen or modification
  → If new screen: create 4 files + register in AppModule.kt
  → If modification: find existing screen, modify it
  → Add strings to EnglishStrings + JapaneseStrings
  → Verify build passes
  → Push to early-develop

User asks to merge/sync
  → "sync to develop" = merge early-develop → develop
  → "merge it" = create PR early-develop → develop
  → Never push to main unless explicitly told

User asks to refactor
  → Verify no behavior change
  → Keep same public API
  → Run tests after
  → Push to early-develop
```

---

## 🌐 Branch Rules for AI

| Command | AI Action |
|---------|-----------|
| Default | Push to `early-develop` |
| "sync to develop" | Merge `early-develop` → `develop` |
| "merge it" | PR `early-develop` → `develop`, merge |
| "push to testing-chamber" | Push to `testing-chamber` |
| "release" | Only with explicit version bump instruction |

---

## 🏗️ Adding a New Screen (AI Checklist)

```kotlin
// 1. Contract
interface MyFeatureScreenContract {
    interface ViewModel {
        val state: StateFlow<MyFeatureState>
    }
}

// 2. ViewModel
class MyFeatureScreenViewModel(...) : MyFeatureScreenContract.ViewModel {
    override val state = MutableStateFlow(MyFeatureState())
}

// 3. Module
val myFeatureScreenModule = module {
    multiplatformViewModel<MyFeatureScreenContract.ViewModel> {
        MyFeatureScreenViewModel(get())
    }
}

// 4. UI
@Composable
fun MyFeatureScreenUI(...) {
    val viewModel = getMultiplatformViewModel<MyFeatureScreenContract.ViewModel>()
    // ...
}
```

Then register in `AppModule.kt`:
```kotlin
val screenModules = listOf(
    // ... existing modules
    myFeatureScreenModule,
)
```

---

## 📝 Adding Strings (AI Checklist)

```kotlin
// 1. In Strings.kt interface
val myFeatureTitle: String

// 2. In EnglishStrings.kt
override val myFeatureTitle = "My Feature"

// 3. In JapaneseStrings.kt
override val myFeatureTitle = "マイ機能"
```

---

## 🔍 Common AI Mistakes to Avoid

| Mistake | Prevention |
|---------|------------|
| Adding new imports that don't exist | Check Compose MPP 1.8 import rules in AI_CONTEXT.md |
| Using `import foo.*` | Always explicit imports |
| Forgetting to register screen module | Check `AppModule.kt` after creating screen |
| Using wrong Compose import | Read AI_CONTEXT.md import rules section |
| Adding dependency without version catalog | Always add to `gradle/libs.versions.toml` |
| Pushing to `develop` directly | Always push to `early-develop` |
| Hardcoding colors | Use `Ds*` tokens or theme tokens |
| Breaking brace balance | Verify with `python3 -c` brace count |

---

## 🧪 Testing AI Changes

```bash
# Desktop compilation (most important)
./gradlew :desktopApp:compileKotlinJvm

# Shared tests
./gradlew :core:allTests

# Full desktop build + run
./gradlew :desktopApp:run

# Android
./gradlew :app:assembleDebug
```

**If any command fails, fix the error before pushing.**

---

## 📚 Skills Index

| Skill | Document | Purpose |
|-------|----------|---------|
| Branch management | [`BRANCH_POLICY.md`](BRANCH_POLICY.md) | Which branches to use, never touch |
| Contribution workflow | [`CONTRIBUTOR_GUIDE.md`](CONTRIBUTOR_GUIDE.md) | How to contribute properly |
| AI capabilities | This file | What AI can/cannot do |
| Feature tracking | [`TODO_FEATURES.md`](TODO_FEATURES.md) | What's done, in progress, planned |
| Architecture | [`ARCHITECTURE_GUIDE.md`](ARCHITECTURE_GUIDE.md) | Folder structure explained |
