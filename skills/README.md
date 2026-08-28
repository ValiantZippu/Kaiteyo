# Skills Index

> Quick reference for AI assistants, contributors, and maintainers.

---

## Documents

| Document | Purpose | Audience |
|----------|---------|----------|
| [`BRANCH_POLICY.md`](BRANCH_POLICY.md) | Branch rules, naming, merge strategy | Everyone |
| [`CONTRIBUTOR_GUIDE.md`](CONTRIBUTOR_GUIDE.md) | How to contribute properly | Human contributors |
| [`AI_SKILLS.md`](AI_SKILLS.md) | AI capabilities, constraints, workflow | AI assistants |
| [`TODO_FEATURES.md`](TODO_FEATURES.md) | Feature roadmap & task tracking | Everyone |
| [`ARCHITECTURE_GUIDE.md`](ARCHITECTURE_GUIDE.md) | Folder structure & module guide | Everyone |

---

## Quick Reference

### Branch Rules
- **`early-develop`** → AI pushes here, daily dev
- **`develop`** → Stable, merge via PR only
- **`main`** → Production, never touch
- **`testing-chamber`** → Experiments, delete when done

### Build Commands
```bash
./gradlew :desktopApp:compileKotlinJvm    # Compile
./gradlew :desktopApp:run                 # Run
./gradlew :core:allTests                  # Test
```

### AI Workflow
1. Read `AI_CONTEXT.md`
2. Make changes
3. Verify build passes
4. Push to `early-develop`

### Screen Pattern
```
Contract → ViewModel → Module → UI
Register in AppModule.kt
```

### String Pattern
```
Interface → EnglishStrings → JapaneseStrings
All three required
```
