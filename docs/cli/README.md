# Kaiteyo CLI — Developer Command Center

`kaiteyo` is a single, cross-platform command-line tool that wraps the
repetitive repository operations developers perform every day:

- **Git workflows** — status → select → preview → commit → push in one guided flow.
- **Gradle** — discover tasks, search them, preview the exact command, run with live output.
- **WSL** — distributions, filesystem, networking, processes, packages, and dev utilities.
- **Environment diagnostics** — `doctor`, `info`, and a developer toolbox (`dev`).
- **Supporting utilities** — docs browser, file utilities, logs, release preflight,
  backup/sync, safe cleanup, and a generic command runner.

It is written in **Python 3 (standard library only)** — no build step, no
dependencies, no Gradle involvement. It runs on Windows, macOS, Linux, and
inside WSL.

## Installation

The CLI needs **Python 3.9+** on `PATH` and nothing else.

### Option 1 — repository launchers (recommended)

Clone the repository and run from the root:

```bash
./kaiteyo --help          # Linux / macOS / WSL
kaiteyo.cmd --help        # Windows (cmd.exe / PowerShell)
```

For use anywhere, add the launcher directory to your `PATH`:

```bash
# POSIX — symlink into a bin directory already on PATH
ln -s "$PWD/tools/cli/bin/kaiteyo" ~/.local/bin/kaiteyo

# Windows — add tools\cli\bin to your PATH in System Settings
```

### Option 2 — pip (optional)

```bash
pip install -e tools/cli     # provides the `kaiteyo` command via console script
```

### Option 3 — module

```bash
python -m kaiteyo_cli        # from the tools/cli directory
```

## Quick start

```bash
kaiteyo                      # interactive command center (main menu)
kaiteyo git                  # Git Command Center
kaiteyo git commit           # guided: select → preview → commit → push
kaiteyo gradle               # Gradle Command Center (task discovery + search)
kaiteyo doctor               # environment diagnostics (PASS / WARN / FAIL)
kaiteyo info                 # project snapshot (branch, commit, version, modules)
kaiteyo wsl --status         # WSL availability + distributions
kaiteyo docs                 # browse repository documentation
```

Typical day:

```bash
kaiteyo git status           # what's changed
kaiteyo git commit           # stage, preview, commit, push — one flow
kaiteyo gradle search test   # find the right test task
kaiteyo gradle --task :core:allTests   # run it with live output
kaiteyo doctor               # sanity-check the environment
```

## Your first five minutes

```bash
./kaiteyo --help           # see every command
./kaiteyo info             # instant project snapshot (branch, commit, version, modules)
./kaiteyo doctor           # environment check — tells you what's missing, never modifies
./kaiteyo git status       # what's changed in the working tree
./kaiteyo git commit       # guided: select files → preview → title → commit → push
./kaiteyo gradle --task :desktopApp:compileKotlinJvm   # run a build with live output
```

That's the whole loop: **`info` to orient → `doctor` if something's off →
`git commit` to save work → `gradle` to build.** Every command shows its
exact underlying command line, asks before anything destructive, and works
non-interactively with flags if you prefer scripting.

## Key behaviors

- **Safe by default** — force pushes, branch deletion, resets and discards are
  never performed silently. Destructive operations are previewed and confirmed.
- **Visible** — the exact command line is always shown before execution
  (Gradle, WSL, backup/sync, generic run).
- **Never hangs silently** — every child process (git, gradle, …) has a hard
  timeout, and long-running steps print a progress note to stderr immediately.
  On slow filesystems (WSL/9p mounts, network drives) `git status` can take a
  minute — fast fields print first, the working-tree state is appended when the
  scan finishes, and a stalled scan degrades to `unknown` instead of freezing.
  See [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
- **CI-friendly** — every command has a non-interactive mode; prompts never
  appear in pipelines. See [AUTOMATION.md](AUTOMATION.md).
- **One configuration** — shared, layered config. See [CONFIGURATION.md](CONFIGURATION.md).
- **Secret-safe** — tokens, passwords and embedded credentials are masked in
  all displayed output.
- **Extensible** — new tools (android, data, perf, docker, …) register as one
  module. See [ARCHITECTURE.md](ARCHITECTURE.md).

## Command overview

| Command | Purpose |
|---|---|
| `kaiteyo git` | Guided Git workflows: status, commit & push, sync, log, diff, branches, clean |
| `kaiteyo gradle` | Gradle Command Center: task discovery, search, preview, live output |
| `kaiteyo wsl` | WSL utility center: distributions, filesystem, networking, dev tools |
| `kaiteyo dev` | Developer toolbox: routes into the tools + env/deps/test quick views |
| `kaiteyo doctor` | Read-only environment diagnostics (PASS / WARN / FAIL) |
| `kaiteyo info` | Project snapshot without building anything |
| `kaiteyo docs` | Browse / search / open repository documentation |
| `kaiteyo files` | Tree, find, text search, large/recent files, duplicates |
| `kaiteyo logs` | List, tail, search, export and clear detected logs |
| `kaiteyo release` | Read-only release preflight: version, changelog, tags, step preview |
| `kaiteyo backup` | rsync backup / sync with dry-run first, never silent deletes |
| `kaiteyo clean` | Safe cleanup (caches) + confirmed Gradle-output cleanup |
| `kaiteyo run` | Run a command through git/gradle/wsl/powershell/cmd/shell/custom with preview |
| `kaiteyo settings` | Central CLI configuration |

Aliases: `g` = git, `gr` = gradle, `w` = wsl, `d` = dev, `b` = backup,
`doc` = docs, `f` = files, `l` = logs, `config` = settings.

## Documentation map

- [COMMANDS.md](COMMANDS.md) — full command reference (purpose, interactive &
  non-interactive usage, examples, warnings, exit codes).
- [CONFIGURATION.md](CONFIGURATION.md) — configuration keys, files, precedence.
- [AUTOMATION.md](AUTOMATION.md) — scripting, CI, JSON output, exit codes.
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) — common problems and fixes.
- [ARCHITECTURE.md](ARCHITECTURE.md) — how the CLI is built and how to extend it.

## Troubleshooting

- **`kaiteyo` not found** — the launcher directory isn't on `PATH`.
- **Python missing** — install Python 3.9+ and retry.
- **`git` not found / not a repository** — run from inside the repository or
  pass `--root PATH`.

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for details.
