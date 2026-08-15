# Kaiteyo CLI — Configuration

There is **one** configuration source shared by every command — no duplicated
config across tools.

## Files

| Scope | Location | Purpose |
|---|---|---|
| User | `~/.kaiteyo/cli/config.json` | Per-developer preferences |
| Project | `<root>/.kaiteyo/config.json` | Per-repository overrides (committed or not, your choice) |
| Environment | `KAITEYO_<KEY>` variables | CI / per-invocation overrides |

> The desktop app owns `~/.kaiteyo/` itself; the CLI stores its user state
> under `~/.kaiteyo/cli/` and its project cache under `<root>/.kaiteyo/` so
> the two never collide.

## Precedence (low → high)

```text
built-in defaults
  < user config          ~/.kaiteyo/cli/config.json
  < project config       <root>/.kaiteyo/config.json
  < environment          KAITEYO_<KEY>
  < command-line flags   (--yes, --root, --json, --quiet, ...)
```

## Keys

| Key | Default | Valid values | Meaning |
|---|---|---|---|
| `project_root` | auto | path | Repository root; auto-detected when unset |
| `preferred_branch` | `develop` | branch name | Branch used by git workflows |
| `default_remote` | `origin` | remote name | Default git remote |
| `preferred_terminal` | auto | — | Terminal for open-style actions |
| `preferred_shell` | auto | — | Shell for shell commands |
| `wsl_distro` | auto | distribution name | WSL distribution used by default |
| `gradle_wrapper` | `wrapper` | `wrapper`, `system` | Use `gradlew` or system `gradle` |
| `confirmations` | `ask` | `ask`, `yes`, `no` | Confirmation behavior |
| `theme` | `auto` | `auto`, `light`, `dark` | Output theme |
| `verbosity` | `normal` | `quiet`, `normal`, `verbose` | Output verbosity |

## Managing configuration

```bash
kaiteyo settings                    # interactive editor
kaiteyo settings list               # show resolved values + file locations
kaiteyo settings get gradle_wrapper
kaiteyo settings set wsl_distro Ubuntu
kaiteyo settings set confirmations ask --scope project
kaiteyo settings reset confirmations
```

Environment override example:

```bash
KAITEYO_CONFIRMATIONS=yes kaiteyo gradle --task tasks
KAITEYO_WSL_DISTRO=Debian kaiteyo wsl ip
```

## Cache

`<root>/.kaiteyo/gradle-tasks.json` caches the Gradle task list (from
`gradlew tasks --all`) so task browsing/searching doesn't re-run Gradle.
Refresh it explicitly with `kaiteyo gradle tasks --refresh`. Safe to delete —
it is regenerated. `kaiteyo clean` removes it with confirmation.
