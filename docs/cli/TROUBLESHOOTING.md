# Kaiteyo CLI — Troubleshooting

Every error message explains **what** happened, **why**, and **what to do
next** (a hint line). This page covers the common cases.

## `kaiteyo: command not found`

- **Why.** The launcher directory is not on `PATH`.
- **Fix.** Run `./kaiteyo` from the repository root, add `tools/cli/bin` to
  `PATH`, symlink `tools/cli/bin/kaiteyo` into a bin directory, or
  `pip install -e tools/cli`.

## Python is missing

- **Why.** The CLI requires Python 3.9+.
- **Fix.** Install Python 3.9+ (python.org, your package manager, or the
  Microsoft Store on Windows) and retry.

## `Executable not found: git` (exit 3)

- **Why.** `git` is not installed or not on PATH.
- **Fix.** Install git and re-run `kaiteyo doctor` to confirm.

## `requires a Kaiteyo repository root` (exit 3)

- **Why.** The CLI walks up from the current directory looking for markers
  (`.git`, `settings.gradle.kts`, `gradlew`, `build.gradle.kts`, …) and found
  none.
- **Fix.** Run from inside the repository, or pass `--root /path/to/Kaiteyo`.

## `Interactive input required for: …` (exit 4)

- **Why.** A prompt (e.g. a commit title) was needed but the run is
  non-interactive (CI, pipe, `--non-interactive`).
- **Fix.** Supply the value as a flag, e.g. `--title "…"`, `--task …`,
  `--files a,b`. The hint names the flag.

## `Interactive menu requested in a non-interactive environment` (exit 4)

- **Why.** A menu-driven command was invoked in a pipeline.
- **Fix.** Use the non-interactive form (see `kaiteyo <cmd> --help`), e.g.
  `kaiteyo gradle --task tasks` instead of `kaiteyo gradle`.

## Git conflicts / commit failures (exit 6)

- **Why.** The commit or push failed (conflicts, hooks, network).
- **Fix.** Resolve conflicts in your editor, then re-run
  `kaiteyo git commit --all --title "…" --push`. Nothing is pushed on a
  failed commit.

## `Gradle task failed …` (exit 7)

- **Why.** The chosen Gradle task exited non-zero.
- **Fix.** Re-run with `--stacktrace` (`kaiteyo gradle --task X --stacktrace`)
  or `--verbose`. Gradle's own error output is always shown — it is never
  swallowed.

## `WSL was not detected` (exit 3/5)

- **Why.** `wsl.exe` is unavailable (macOS: exit 5 — WSL only runs on
  Windows).
- **Fix.** On Windows 10/11, enable WSL with `wsl --install` (admin, reboot),
  then install a distribution from the Microsoft Store. The CLI will not
  modify Windows configuration for you.

## `rsync was not found` (exit 3)

- **Why.** `kaiteyo backup/sync` needs rsync.
- **Fix.** Linux/macOS: install via your package manager. Windows: run the
  CLI inside WSL, or install rsync another way.

## `Unknown command: pussh` (exit 2)

- **Why.** Typo.
- **Fix.** The CLI suggests close matches (`Did you mean: push`). Run
  `kaiteyo --help` for the full list.

## Output has no colors

- **Why.** Not a TTY, `NO_COLOR` is set, `TERM=dumb`, or `--no-color`.
- **Fix.** Colors are an enhancement; status markers remain readable
  (`[PASS] [WARN] [FAIL] [INFO]`).

## `git status` / `info` / `doctor` seem frozen or very slow

- **Why.** Commands that inspect the working tree run `git status`, which has
  to stat every tracked + untracked file. On slow filesystems — WSL/9p
  mounts (`/mnt/...` on a Windows host), network drives, large trees — that
  scan can take a minute or more. Older versions of the CLI waited silently;
  now every such command prints a progress note
  (`scanning working tree (git status)…`) to **stderr** immediately, so you
  can see it is working, and every git call has a hard timeout.
- **What you'll see.** Fast output appears right away (`kaiteyo info` prints
  project/branch/commit/version before scanning); the working-tree line is
  appended when the scan finishes. If the scan exceeds its bound, the CLI
  reports `unknown`/an explicit timeout error instead of hanging.
- **Fix / workarounds.**
  - Wait it out — the scan eventually completes.
  - Keep the repo on a local disk for development if you can (this is the
    real fix; 9p/network mounts are always slow).
  - For a quick overview, `kaiteyo info` still gives branch/commit/version
    instantly — only the working-tree state is deferred.
  - For heavy git work (large diffs, full status), run `git` directly; the
    CLI never hides what git says.

## The CLI runs Gradle on its own / slowly

- **Why.** It doesn't run Gradle automatically. `kaiteyo gradle tasks
  --refresh` runs `gradlew tasks --all` once and caches the result;
  browsing/searching afterwards uses the cache.
- **Fix.** Delete `<root>/.kaiteyo/gradle-tasks.json` (or
  `kaiteyo clean`) to reset the cache.
