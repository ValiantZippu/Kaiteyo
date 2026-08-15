# Kaiteyo CLI — Command Reference

Every command supports `--help` (e.g. `kaiteyo git --help`) and the global
flags below. Exit codes are documented at the end.

## Global flags

```text
--root PATH           project root override (auto-detected otherwise)
--yes, -y             assume yes for confirmations
--non-interactive     never prompt; fail instead of asking (CI)
--json                machine-readable output where supported
--no-color            disable colored output
--verbose             more detailed output
--quiet, -q           suppress non-essential output
--version             show the CLI version
```

In non-interactive environments (no TTY, CI), prompts never appear:

- confirmations default to the safe answer (or `--yes`),
- input prompts (e.g. a commit title) fail with a clear message and exit code 4,
- menus fail with a clear message and exit code 4.

---

## `kaiteyo` — main menu

**Purpose.** Interactive entry point. Shows the command center:

```text
1  Git          6  Project Info    11 Backup / Sync
2  Gradle       7  Files           12 Settings
3  WSL          8  Logs            13 Exit
4  Dev Tools    9  Documentation
5  Doctor      10  Release
```

Plus your recent commands for quick reuse.

**Usage.** `kaiteyo` (interactive). With no TTY and no command, prints help
and exits with code 2.

---

## `kaiteyo git` — Git workflows

**Purpose.** Staged, safe Git workflows. The primary flow is Commit & Push:

```text
CHECK STATUS → SHOW CHANGES → SELECT WHAT TO INCLUDE → COMMIT TITLE
→ OPTIONAL DESCRIPTION → STAGE → COMMIT → PUSH
```

**Interactive.** `kaiteyo git` shows the Git Command Center:
Status · Commit & Push · Diff · Log · Branches · Sync · Clean (untracked).

**Non-interactive.**

```bash
kaiteyo git status                        # summary (branch, upstream, ahead/behind, files)
kaiteyo git status --json                 # machine-readable status
kaiteyo git commit --all --title "Fix library" --push
kaiteyo git commit --all --title "Fix" --message "Multiline\ndescription" --push
kaiteyo git commit --modified --title "Tidy code"        # modified files only
kaiteyo git commit --untracked --title "Add config"
kaiteyo git commit --files "core/src/a.kt,core/src/b.kt" --title "T"
kaiteyo git commit --dirs "docs,core" --title "T"
kaiteyo git push                          # branch/upstream info, then confirm (or --yes)
kaiteyo git sync                          # fetch → ff-only pull → push
kaiteyo git log --count 30
kaiteyo git diff [--staged] [--full]
kaiteyo git branches
kaiteyo git clean                         # preview untracked files, then confirmed delete
```

**Selection.** `commit` stages: all changes (`--all`), modified only
(`--modified`), untracked only (`--untracked`), individual files (`--files`),
directories (`--dirs`), or an interactive multi-select. The staged result is
shown *before* committing.

**Push safety.** The push step always shows current branch, remote (masked),
upstream, and ahead/behind. If there is no upstream, it offers
`git push -u <remote> <branch>`. Force push is never used.

**Warnings.**

- `git clean` deletes untracked files — it previews first and requires confirmation.
- A clean tree means nothing to commit; no empty commits are ever created.
- Commit failures (e.g. conflicts) are reported; nothing is pushed.

---

## `kaiteyo gradle` — Gradle Command Center

**Purpose.** Discover available Gradle tasks instead of remembering them, and
run them with full visibility. The CLI **never runs Gradle automatically** —
the user chooses.

**Discovery.**

- Modules are parsed statically from `settings.gradle.kts` (`include(...)`).
- The full task list comes from `gradlew tasks --all` and is cached in
  `<root>/.kaiteyo/gradle-tasks.json`. Refresh explicitly with
  `kaiteyo gradle tasks --refresh` (runs Gradle — confirmed first).

**Interactive menu.**

```text
1  tasks                 7  dependencies
2  build                 8  desktop tasks (:desktopApp:*)
3  assemble              9  run configuration (:desktopApp:run)
4  test                 10  search tasks
5  clean                11  custom command
6  check                12  refresh task list
```

**Non-interactive.**

```bash
kaiteyo gradle --task tasks
kaiteyo gradle --task :desktopApp:compileKotlinJvm --yes
kaiteyo gradle tasks                 # cached task list
kaiteyo gradle tasks --refresh       # re-run `gradlew tasks --all`
kaiteyo gradle search desktop        # substring search across cached tasks
kaiteyo gradle modules               # modules from settings.gradle.kts
```

**Preview & execution.** The exact command line is printed first, e.g.
`./gradlew :desktopApp:packageRelease`. Expensive tasks are marked with
"This may take several minutes." and confirmed (`Run? [y/N]`) in interactive
mode. Output streams live, elapsed time and the real exit code are shown, and
the run ends with `SUCCESS` or `FAILED` — Gradle's own error output is never
swallowed.

**Warnings.** Gradle runs can take minutes and download dependencies; they are
never started without an explicit choice.

---

## `kaiteyo wsl` — WSL Command Center

**Purpose.** Interactive WSL utilities: distribution management, filesystem,
networking, processes, packages, shell, Windows integration, and development
shortcuts. Runs on Windows (or inside a WSL distribution). On macOS/other
hosts the CLI explains that WSL was not detected and exits with code 5 — it
never installs or modifies Windows system configuration.

**Distribution detection** is runtime-based (`wsl --list --quiet`); no
specific distribution is assumed. The configured `wsl_distro` (or `--distro`)
is used when set.

**Non-interactive.**

```bash
kaiteyo wsl --status                 # WSL status + detected distributions
kaiteyo wsl --list                   # verbose distribution list
kaiteyo wsl --version
kaiteyo wsl shell --distro Ubuntu    # open a shell
kaiteyo wsl run "ls -la /mnt/c"      # run a command inside WSL
kaiteyo wsl win "echo %USERNAME%"    # run a Windows command from WSL
kaiteyo wsl ip | mounts | ports | processes | disk | env | pkg | git | ssh
kaiteyo wsl --terminate Ubuntu       # confirmed termination
kaiteyo wsl --shutdown               # confirmed shutdown of all distros
```

**Visibility.** Every WSL command shows the exact command line before
execution. `--shutdown` (kills all distributions) and `--terminate` require
confirmation.

---

## `kaiteyo dev` — Developer Toolbox

**Purpose.** Central hub that routes into the dedicated tools and adds quick
views — reusing implementations rather than duplicating them.

Sections: Git · Gradle · WSL · Environment · Files · Logs · Dependencies ·
Testing · Documentation · Release · Diagnostics.

**Non-interactive.** `kaiteyo dev env`, `kaiteyo dev deps`, `kaiteyo dev test`.

---

## `kaiteyo doctor` — environment diagnostics

**Purpose.** Inspects (never modifies): OS/architecture, Python, git (+
user.name/email), Java/JAVA_HOME, Gradle wrapper, WSL + distributions,
repository state, disk space, ANDROID_HOME/ANDROID_SDK_ROOT, optional tools.

**Usage.**

```bash
kaiteyo doctor          # PASS / WARN / FAIL lines + summary + remediation hints
kaiteyo doctor --json   # structured checks with a summary
```

**Warnings.** Read-only: nothing is fixed automatically. Hints are printed,
not executed.

---

## `kaiteyo info` — project information

**Purpose.** Fast, build-free snapshot: project name, repository (masked),
branch, commit, working tree state, version, platform, Java, Gradle wrapper,
modules.

```bash
kaiteyo info            # human table
kaiteyo info --json     # machine-readable
```

---

## `kaiteyo docs` — documentation browser

**Purpose.** Discover repository documentation without duplicating it.

```bash
kaiteyo docs                    # interactive topics browser
kaiteyo docs topics             # categories + files
kaiteyo docs tree [--depth 2]
kaiteyo docs search release     # text search across docs/
kaiteyo docs open development/COMMANDS.md
```

**Warnings.** `open` only accepts paths under `docs/`.

---

## `kaiteyo files` — file utilities

**Purpose.** Developer file convenience — not a file manager.

```bash
kaiteyo files tree --depth 2
kaiteyo files find ViewModel
kaiteyo files search sqlDelight        # ripgrep when available, Python fallback
kaiteyo files large --top 10
kaiteyo files recent --days 3
kaiteyo files dupes --min-mb 1         # size + md5 duplicate groups
kaiteyo files open docs                # default application
kaiteyo files root
```

Build/`.git`/`.gradle` directories are skipped by scans.

---

## `kaiteyo logs` — log viewer

**Purpose.** Work with logs actually present on the system — locations are
detected from the project/application configuration, never invented:
repo-root build logs (`build_log.txt`, `build_errors.txt`, `*_build*.txt`,
`*.log`), `.tools/*.log`, and `~/.kaiteyo/*.log`.

```bash
kaiteyo logs list
kaiteyo logs latest --lines 60
kaiteyo logs follow [--file name]
kaiteyo logs search error
kaiteyo logs export ./log-export
kaiteyo logs clear                     # confirmed deletion of log files only
```

**Warnings.** `clear` deletes only detected log files (never source) and
requires confirmation.

---

## `kaiteyo release` — release preflight (read-only)

**Purpose.** Prepares the architecture for releases without doing anything:
version (AppVersion.kt), changelog state, git preconditions, tags, and a
preview of the steps a release would involve.

```bash
kaiteyo release status
kaiteyo release check       # PASS/WARN/FAIL preconditions
kaiteyo release preview     # planned steps — nothing is executed
```

**Warnings.** This command never bumps versions, creates tags, pushes, or
publishes anything.

---

## `kaiteyo backup` / `kaiteyo sync` — rsync transfers

**Purpose.** Backup the repository or sync directories with rsync. A dry-run
summary (added / modified / removed / bytes transferred) is always shown
first; destination files are never deleted unless `--delete` is given **and**
confirmed.

```bash
kaiteyo backup --to /mnt/backup/kaiteyo --dry-run
kaiteyo backup --to /mnt/backup/kaiteyo
kaiteyo sync --from ./docs --to /mnt/backup/docs --dry-run
kaiteyo sync --from ./docs --to /mnt/backup/docs [--delete]
```

Backup excludes build outputs, `.git`, `.gradle`, `.kotlin`, local config and
scratch dirs.

**Warnings.** rsync is required (Linux/macOS natively; Windows via WSL).
`--delete` mirrors the source into the destination and removes extras — it
always requires confirmation.

---

## `kaiteyo clean` — cleanup utilities

**Purpose.** Two explicit tiers:

- **Safe** (default): CLI caches (`<root>/.kaiteyo/*`, `__pycache__` under
  `tools/cli`).
- **Destructive** (`--build`): Gradle outputs (`build/`, `.gradle/`,
  `.kotlin/`) — confirmed loudly; regenerated on the next build.

```bash
kaiteyo clean --dry-run
kaiteyo clean
kaiteyo clean --build --dry-run
kaiteyo clean --build
```

**Warnings.** Source code is never deleted.

---

## `kaiteyo run` — safe generic command runner

**Purpose.** Run a command through a category with full visibility: working
directory, exact command, and environment notes are shown before execution.

```bash
kaiteyo run git status
kaiteyo run gradle :desktopApp:compileKotlinJvm
kaiteyo run wsl ls -la /mnt/c
kaiteyo run powershell Get-Process
kaiteyo run cmd dir
kaiteyo run shell 'echo hi'
kaiteyo run custom curl -I https://example.com
```

**Warnings.** Not an arbitrary silent executor — the command line is always
displayed and confirmed.

---

## `kaiteyo settings` — configuration

**Purpose.** View and edit the single CLI configuration shared by every
command (see [CONFIGURATION.md](CONFIGURATION.md)).

```bash
kaiteyo settings
kaiteyo settings list
kaiteyo settings get wsl_distro
kaiteyo settings set gradle_wrapper wrapper
kaiteyo settings set confirmations ask --scope project
kaiteyo settings reset [key]
```

---

## Command history & suggestions

- Recent command paths are shown in the main menu (`Recent: git commit · gradle tasks …`).
  Arguments are never stored (secrets stay out of history by design).
- Typing an unknown command suggests close matches:
  `Unknown command: pussh → Did you mean: push`.

---

## Exit codes

| Code | Meaning |
|---|---|
| 0 | success |
| 1 | the operation ran but failed |
| 2 | usage error — unknown command or invalid arguments |
| 3 | environment problem — a required tool or path is missing |
| 4 | aborted by the user, or a prompt was needed in non-interactive mode |
| 5 | unsupported on this platform (e.g. WSL on macOS) |
| 6 | a git operation failed |
| 7 | a Gradle operation failed |
| 8 | a backup/sync or WSL operation failed |

Every command exits 0 on success and a documented non-zero code on failure,
so the CLI is usable in CI scripts. See [AUTOMATION.md](AUTOMATION.md).
