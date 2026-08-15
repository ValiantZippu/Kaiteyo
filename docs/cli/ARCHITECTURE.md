# Kaiteyo CLI — Architecture

## Design principles

1. **One coherent layer, not three scripts.** All commands share the same
   core modules: output, UI, config, root detection, runner, secrets,
   errors. No duplicated git/gradle/wsl logic across platform scripts.
2. **Platform adapters only where needed.** `platform.py` answers "which OS,
   are we in WSL, which opener/shell?" — commands stay platform-agnostic.
3. **Safe defaults.** Destructive operations are previewed and confirmed;
   prompts never appear in CI; secrets are masked.
4. **Visible.** The exact command line is shown before execution everywhere.
5. **Extensible.** Adding a tool = one module + one registration line.

## Layout

```text
tools/cli/
├── bin/kaiteyo, bin/kaiteyo.cmd    launchers (add to PATH)
├── pyproject.toml                  optional pip install
└── kaiteyo_cli/
    ├── app.py                      parser, dispatch, main menu, suggestions
    ├── context.py                  Context passed to every command
    ├── registry.py                 Command registry (extensibility)
    ├── output.py                   status markers, banners, colors, JSON
    ├── ui.py                       prompts / confirm / menus (CI-safe)
    ├── runner.py                   process execution (streaming, timing, masking)
    ├── platform.py                 OS / WSL / shell / opener detection
    ├── root.py                     repository root detection
    ├── config.py                   central configuration
    ├── history.py                  command-path history
    ├── secrets.py                  secret masking
    ├── errors.py                   exit codes + CliError
    └── commands/                   one module per tool
        ├── __init__.py             registers all commands
        ├── git.py  gradle.py  wsl.py  dev.py  doctor.py  info.py
        ├── docs.py  files.py  logs.py  release.py  backup.py
        └── clean.py  runcmd.py  settings.py
```

Repository-root launchers (`./kaiteyo`, `kaiteyo.cmd`) are thin wrappers that
point at `tools/cli`.

## How a command runs

```text
app.py main()
  → resolve root (--root or walk-up markers)
  → load Config (defaults < user < project < env)
  → build Context (flags, output, config, root)
  → registry.resolve(name)  (aliases + typo suggestions)
  → command.run(args, ctx)  → exit code
```

`Context` gives commands safe UI access: `ctx.confirm()` honors `--yes` and
config; `ctx.prompt()` / `ctx.menu()` fail cleanly in non-interactive mode.

## Where commands get their data

| Concern | Source |
|---|---|
| Git state | `git status --porcelain -b`, `git diff --stat`, `git log` (git is never reimplemented) |
| Gradle modules | `settings.gradle.kts` `include(...)` (parsed statically) |
| Gradle tasks | `gradlew tasks --all`, cached in `<root>/.kaiteyo/gradle-tasks.json` |
| WSL distributions | `wsl --list --quiet` (runtime detection, never assumed) |
| Log locations | repo-root build logs, `.tools/*.log`, `~/.kaiteyo/*.log` (detected, never invented) |
| Version | `buildSrc/.../AppVersion.kt`, `gradle/wrapper/gradle-wrapper.properties` |

## Adding a new command (e.g. a future `kaiteyo android`)

1. Create `commands/android.py`:
   ```python
   from ..registry import Command

   def build(sub): ...            # argparse subparsers
   def _menu(ctx): ...            # optional interactive entry

   command = Command(
       name="android",
       aliases=["adb"],
       help="...",
       description="...",
       build=build,
       run=lambda args, ctx: _dispatch(args, ctx),
       menu=_menu,
       menu_label="Android",
   )
   ```
2. Add `"android"` to the `names` list in `commands/__init__.py`.
3. That's it — `--help`, aliases, suggestions, JSON, history and the main
   menu wiring come from the registry.

Reserved names for future tools (per the project plan): `android` (ADB,
APK, logcat, screen capture), `data` (database/dataset status, validation,
statistics, migration status — no production data mutation), `perf`
(repository diagnostics, large-file/build-artifact inspection, resource
inspection), `docker`, `kotlin`, `java`, `node`, `python`, `website`, plus
GitHub releases and dependency inspection.

## Testing the CLI

The CLI is a dependency-free Python package; there is no test harness yet.
Verification without Gradle:

```bash
python -m py_compile tools/cli/kaiteyo_cli/*.py tools/cli/kaiteyo_cli/commands/*.py
./kaiteyo --help
./kaiteyo info
./kaiteyo doctor
./kaiteyo git status --json
```

`doctor` is the built-in environment self-check.
