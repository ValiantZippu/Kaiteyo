"""kaiteyo — the Kaiteyo developer command center.

Entry point for `python -m kaiteyo_cli`, the launchers in tools/cli/bin/ and
the repo-root `kaiteyo` script. Parses global flags, dispatches to registered
commands, provides the interactive main menu, command suggestions for typos,
history, and consistent error reporting.
"""

from __future__ import annotations

import argparse
import pathlib
import sys

from . import __version__
from .commands import registry
from .config import Config
from .context import Context
from .errors import EXIT_DOCS, ABORT, CliError, FAILED, OK, USAGE
from .history import add as history_add
from .history import recent as history_recent
from .output import Out
from .root import find_root


def build_parser() -> argparse.ArgumentParser:
    # Common flags are available on the main parser AND every subcommand, so
    # `kaiteyo wsl --status --json` works just like `kaiteyo --json wsl --status`.
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument("--yes", "-y", action="store_true", help="assume yes for confirmations")
    common.add_argument("--non-interactive", action="store_true",
                        help="never prompt; fail instead of asking (CI)")
    common.add_argument("--json", action="store_true", help="machine-readable output where supported")
    common.add_argument("--no-color", action="store_true", help="disable colored output")
    common.add_argument("--verbose", action="store_true", help="more detailed output")
    common.add_argument("--quiet", "-q", action="store_true", help="suppress non-essential output")

    parser = argparse.ArgumentParser(
        prog="kaiteyo",
        parents=[common],
        description="Kaiteyo developer command center — git, gradle, wsl, doctor, docs, files and more.",
        epilog="Run `kaiteyo <command> --help` for command details. Exit codes: "
               + ", ".join(f"{k}={v}" for k, v in sorted(EXIT_DOCS.items())),
    )
    parser.add_argument("--version", action="version", version=f"kaiteyo {__version__}")
    parser.add_argument("--root", metavar="PATH", help="project root override (auto-detected otherwise)")

    subs = parser.add_subparsers(dest="command", metavar="COMMAND")
    for cmd in registry().all():
        sub = subs.add_parser(cmd.name, aliases=cmd.aliases, parents=[common],
                              help=cmd.help, description=cmd.description,
                              formatter_class=argparse.RawDescriptionHelpFormatter)
        cmd.build(sub, common)
    return parser


def _main_menu(ctx: Context) -> int:
    from .registry import Command  # noqa: F401 (typing only)

    reg = registry()
    ordered = ["git", "gradle", "wsl", "dev", "doctor", "info", "files", "logs",
               "docs", "release", "backup", "settings"]
    out = ctx.out
    while True:
        out.banner("Kaiteyo Command Center")
        options = []
        for name in ordered:
            cmd = reg.get(name)
            assert cmd is not None
            options.append(cmd.menu_label or name)
        options.append("Exit")
        choice = ctx.menu("Select a tool", options)
        if choice is None or choice == len(options) - 1:
            out.info("Goodbye.")
            return OK
        name = ordered[choice]
        cmd = reg.get(name)
        if cmd is None:
            continue
        history_add([name])
        try:
            if cmd.menu:
                cmd.menu(ctx)
            else:
                cmd.run(argparse.Namespace(), ctx)
        except CliError as exc:
            out.error(exc.message)
            if exc.hint:
                out.dim(f"  {exc.hint}")


def main(argv: list[str] | None = None) -> int:
    argv = list(sys.argv[1:] if argv is None else argv)
    parser = build_parser()

    if not argv and sys.stdin.isatty():
        # `kaiteyo` with no arguments → interactive main menu.
        out = Out(json_mode=False)
        root = _resolve_root(None)
        cfg = Config(str(root) if root else None)
        ctx = Context(argparse.Namespace(yes=False, json=False, no_color=False,
                                         verbose=False, quiet=False), out, cfg, root)
        _print_recent(out)
        return _main_menu(ctx)

    # Suggest close matches for unknown commands before argparse rejects them.
    token = _command_token(argv)
    if token is not None and registry().get(token) is None:
        suggestions = _suggest(token)
        print(f"Unknown command: {token}", file=sys.stderr)
        if suggestions:
            print("Did you mean: " + ", ".join(suggestions) + "?", file=sys.stderr)
        print("Run `kaiteyo --help` for the command list.", file=sys.stderr)
        return USAGE

    args = parser.parse_args(argv)
    out = Out(json_mode=args.json, quiet=args.quiet, color=False if args.no_color else None)

    if args.command is None:
        # Non-interactive invocation without a command → usage help.
        parser.print_help()
        return USAGE

    command, canonical = registry().resolve(args.command)
    if command is None:  # pragma: no cover - guarded above
        return USAGE

    if not out.json_mode and not args.quiet and args.verbose:
        print(f"[INFO] kaiteyo {__version__} — command: {canonical}")

    root = _resolve_root(args.root)
    cfg = Config(str(root) if root else None)
    ctx = Context(args, out, cfg, root)

    history_add([canonical])
    try:
        return command.run(args, ctx)
    except CliError as exc:
        out.error(exc.message)
        if exc.hint:
            out.dim(f"  {exc.hint}")
        return exc.code
    except KeyboardInterrupt:
        print()
        out.warn("Interrupted.")
        return ABORT
    except BrokenPipeError:  # pragma: no cover
        return OK
    except Exception as exc:  # defensive — never show a raw traceback
        if args.verbose:
            raise
        out.error(f"Unexpected error: {exc}")
        out.dim("  Re-run with --verbose for a full traceback.")
        return FAILED


# Common subcommands (git, gradle, wsl, ...) so typos like `pussh` suggest
# `git push` — the registry only knows top-level commands.
_SUBCOMMAND_HINTS = {
    "status": "git status", "commit": "git commit", "push": "git push",
    "sync": "git sync", "log": "git log", "diff": "git diff",
    "branches": "git branches",
    "tasks": "gradle tasks", "modules": "gradle modules",
    "shell": "wsl shell", "terminate": "wsl terminate", "shutdown": "wsl shutdown",
    "mounts": "wsl mounts", "processes": "wsl processes", "ports": "wsl ports",
    "topics": "docs topics", "tree": "docs tree",
    "dupes": "files dupes", "large": "files large", "recent": "files recent",
}


def _suggest(token: str, limit: int = 3) -> list[str]:
    import difflib

    pool = list(registry().names())
    for cmd in registry().all():
        pool.extend(cmd.aliases)
    pool.extend(_SUBCOMMAND_HINTS)
    matches = difflib.get_close_matches(token, pool, n=limit)
    return [_SUBCOMMAND_HINTS.get(m, m) for m in matches]


def _command_token(argv: list[str]) -> str | None:
    """Return the first token that looks like a top-level command.

    Skips options and the value consumed by --root PATH.
    """
    skip_next = False
    for token in argv:
        if skip_next:
            skip_next = False
            continue
        if token == "--root":
            skip_next = True
            continue
        if token.startswith("-"):
            continue
        return token
    return None


def _resolve_root(override: str | None) -> pathlib.Path | None:
    if override:
        path = pathlib.Path(override).expanduser().resolve()
        if not path.is_dir():
            raise CliError(f"--root is not a directory: {path}", code=USAGE)
        return path
    return find_root()


def _print_recent(out: Out) -> None:
    entries = history_recent()
    if entries:
        out.line()
        out.dim("Recent: " + " · ".join(" ".join(e) for e in entries[:8]))


if __name__ == "__main__":
    raise SystemExit(main())
