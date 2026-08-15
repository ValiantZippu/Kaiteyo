# Kaiteyo — Vibe Coding Guide

This guide is for developers using AI assistants (Claude, GPT, Gemini, local models,
Codebuff/agent CLIs) to contribute to Kaiteyo. It covers zero-setup, the daily
workflow, and how to keep AI work safe in this repo. It complements — and defers to —
`AI_CONTEXT.md` (project facts + never-change list) and
`docs/ai/AI_AGENT_GUIDE.md` (the binding agent workflow).

## Prerequisites

### Accounts
- **GitHub** account (free)
- **VS Code** (free) or **IntelliJ IDEA** (Community is free; recommended for Kotlin)

### Software (Windows examples)

| Tool | How |
|------|-----|
| Git | https://git-scm.com/download/win |
| JDK 17 | https://adoptium.net/temurin/releases/?version=17 (MSI, "Add to PATH") |
| VS Code | https://code.visualstudio.com/ |
| Android Studio (optional) | https://developer.android.com/studio — only for `:app` builds |

### VS Code extensions

**Kotlin**, **Gradle for Java**, **Extension Pack for Java**, **GitLens**,
**Error Lens**, **Even Better TOML**, **YAML**, **Markdown All in One**,
**GitHub Pull Requests**.

Optional AI extensions: **Continue** (continue.dev), **Cline**, **GitHub Copilot**.

## First clone

```bash
git clone https://github.com/ValiantZippu/Kaiteyo.git
cd Kaiteyo
git checkout develop        # default integration branch
./gradlew :desktopApp:run   # first build downloads dependencies (5–15 min)
```

## What is Gradle?

Gradle is the build system: it downloads dependencies, compiles Kotlin, packages the
app, and runs tests.

```bash
./gradlew :desktopApp:run               # run the desktop app
./gradlew :desktopApp:compileKotlinJvm  # compile only (fast feedback)
./gradlew :core:allTests                # shared-engine tests
./gradlew :desktopApp:test              # desktop suite tests
./gradlew clean                         # clean outputs
```

Windows: `.\gradlew.bat ...` instead of `./gradlew ...`.

## What is JDK / why PATH matters

JDK 17 is required (the build pins `jvmToolchain(17)`). Kotlin compiles to the JVM.
If `java --version` doesn't show 17, reinstall Temurin with "Add to PATH" and restart
the terminal. Gradle's toolchain can auto-download 17, but installing it directly is
more predictable.

## Daily workflow

### Starting a session
```bash
git checkout develop
git pull --rebase
```

Read, in order: `docs/README.md` → `docs/development/AI_CONTEXT.md` →
`docs/planning/CURRENT_ISSUES.md` → `docs/planning/TODO.md`. If you're an AI agent,
also read `docs/ai/AI_AGENT_GUIDE.md` and `docs/engineering/ENGINEERING_STANDARDS.md`.

### Making changes
```bash
git checkout -b feature/my-feature
# edit...
./gradlew :desktopApp:compileKotlinJvm   # compile after each meaningful change
git add <files>
git commit -m "feat(scope): concise description"
git push origin feature/my-feature
```

Commit messages: `type(scope): description` — types `feat/fix/docs/refactor/perf/
test/style/chore`. One coherent change per commit. Never commit `local.properties`,
keystores, or secrets (all gitignored).

### Creating a pull request
Push the branch, open a PR to `develop`, and fill in the summary/problem/solution/
testing sections. PRs are squash-merged.

## AI workflow tips (safe by construction)

1. **Read before writing** — docs first, then the actual source. Never "fix"
   something from memory.
2. **Check the never-change list** (`AI_CONTEXT.md`) — SRS logic, SQLDelight schemas,
   package namespace `ua.syt0r.kanji`, Gradle config are off-limits unless explicitly
   requested.
3. **Plan, then make the smallest correct change** — one file at a time, compile
   after each change. Don't bulk-rewrite.
4. **Follow the screen pattern** — new core screens need the 4-file pattern
   (Contract/ViewModel/Module/Screen) and registration in `di/AppModule.kt`
   (+ `MainNavigation.kt` for new destinations).
5. **Use the design system** — desktop UI must use `Ds*` components and tokens; no
   hardcoded colors/radii/spacing. See `docs/design/UI_SYSTEM.md`.
6. **i18n** — new strings go in `Strings.kt` + `EnglishStrings.kt` +
   `JapaneseStrings.kt`.
7. **Update docs** — behavior changes update the affected docs; fixed issues update
   `docs/planning/CURRENT_ISSUES.md`; solved setup problems update
   `docs/troubleshooting/`.
8. **Leave a handoff** — per `ENGINEERING_STANDARDS.md` §174, report what changed,
   files, tests, known issues, next steps. Don't leave the next agent guessing.

## Common errors and fixes

| Error | Cause / fix |
|-------|-------------|
| `Unresolved reference 'X'` | Missing import. Check Compose MPP import rules in `AI_CONTEXT.md` (e.g. `spring` → `androidx.compose.animation.core`). |
| `BUILD FAILED` | Compile error — read the `e:` lines for file/line. Fix, recompile. |
| `java not found` / wrong version | JDK 17 not on PATH. |
| `Gradle sync failed` | Network or cache — `./gradlew clean`, retry. |
| `Kotlin/Native targets cannot be built` | Expected on Windows; ignore (flag is set). |
| `SDK location not found` | Android build without `ANDROID_HOME`/`local.properties` — see `DEVELOPMENT_SETUP.md` Step 7. |

## How CI builds releases

Pushing a tag (`vX.Y.Z`) triggers `.github/workflows/build-release.yml` →
`build-all.yml`: Linux builds `:app:assembleFdroidRelease` + desktop Linux packages;
Windows/macOS build desktop distributions; artifacts are staged, verified
(`stage-artifacts.sh` + `verify-artifacts.sh`), update feeds are generated, and a
GitHub Release is created. See `docs/architecture/ci-cd.md` and
`docs/releases/RELEASE_PROCESS.md`.

## How signing works (Android)

No keystore or credentials are in git. The build resolves the keystore from
`KEYSTORE_PATH` env → `~/.kaiteyo/keystore.jks` → repo-root `keystore.jks` (CI decodes
it from the `KEYSTORE_BASE64` secret); fallback is debug signing. Release secrets come
from CI env vars — never commit them.

## Related

- `AI_CONTEXT.md` — project facts, never-change list, import rules
- `DEVELOPER_GUIDE.md` — building/testing/debugging
- `DEVELOPMENT_SETUP.md` — from zero to running
- `docs/ai/AI_AGENT_GUIDE.md` — the binding agent workflow
- `docs/engineering/ENGINEERING_STANDARDS.md` — the engineering contract
