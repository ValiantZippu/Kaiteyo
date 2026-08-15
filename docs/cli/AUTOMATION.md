# Kaiteyo CLI — Automation & Scripting

The CLI is designed for scripts and CI as much as for humans. Human output is
the default; structured output and non-interactive behavior are opt-in.

## Non-interactive safety

Prompts never appear in pipelines:

- When stdin/stdout is not a TTY, the CLI is automatically non-interactive.
- `--non-interactive` forces this behavior explicitly.
- Confirmations resolve to the safe default (usually "no") unless `--yes`
  is passed.
- Input prompts and menus fail with exit code 4 and a hint about which flag
  to pass instead (e.g. `--title`, `--task`).

## CI-friendly patterns

```bash
# Git
kaiteyo git status --json
kaiteyo git commit --all --title "chore: bump version" --push --yes

# Gradle
kaiteyo gradle --task :core:allTests --yes
kaiteyo gradle tasks --refresh

# Diagnostics / info
kaiteyo doctor --json
kaiteyo info --json

# WSL
kaiteyo wsl --status --json

# Backup — dry-run first, always
kaiteyo backup --to /mnt/backup/kaiteyo --dry-run --json
```

## Exit codes

| Code | Meaning | Scripts |
|---|---|---|
| 0 | success | proceed |
| 1 | operation failed | report failure |
| 2 | usage error (bad args / unknown command) | fix invocation |
| 3 | environment problem (tool missing, not a repo) | check setup |
| 4 | aborted / prompt needed in non-interactive mode | add the missing flag |
| 5 | unsupported on this platform | platform guard |
| 6 | git failed | handle git failure |
| 7 | Gradle failed | handle build failure |
| 8 | backup/sync or WSL failed | handle transfer failure |

Example:

```bash
kaiteyo gradle --task :desktopApp:compileKotlinJvm --yes
status=$?
if [ "$status" -eq 7 ]; then
  echo "Compilation failed — see the Gradle output above."
  exit 1
fi
```

## JSON output

Commands that return structured information support `--json` (human output
remains the default):

| Command | Output |
|---|---|
| `kaiteyo git status --json` | branch, upstream, ahead/behind, staged/unstaged/untracked, clean |
| `kaiteyo doctor --json` | checks with status/detail/hint + summary counts |
| `kaiteyo info --json` | project, repository, branch, commit, version, modules, … |
| `kaiteyo gradle tasks --json` | cached task list |
| `kaiteyo gradle search Q --json` | query + matches |
| `kaiteyo gradle modules --json` | module list |
| `kaiteyo wsl --status --json` | wsl detected + status + distributions |
| `kaiteyo docs topics --json` | categories + files |
| `kaiteyo files find/large/dupes/root --json` | structured results |
| `kaiteyo logs list --json` | detected logs with size/mtime |
| `kaiteyo release status/check --json` | version/changelog/tags / precondition checks |
| `kaiteyo backup --to … --dry-run --json` | dry-run summary (added/modified/removed/bytes) |
| `kaiteyo settings list/get --json` | configuration values |

In `--json` mode the command writes exactly one JSON document to stdout;
everything else goes to stderr.

## Secret safety in automation

- Git remote URLs with embedded credentials are masked (`https://user:***@host`).
- Known secret patterns (`token=`, `password:`, `Authorization: Bearer …`,
  `ghp_…`, `sk-…`) are masked in streamed output.
- Command history stores command *paths* only (`git commit`) — never
  arguments, so titles and secrets can't leak into history.
