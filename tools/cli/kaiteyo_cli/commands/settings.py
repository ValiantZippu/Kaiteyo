"""kaiteyo settings — central CLI configuration.

Views and edits the single CLI configuration (user scope by default, project
scope with --scope project). One source of truth shared by every command.
"""

from __future__ import annotations

import argparse

from ..context import Context
from ..errors import CliError, OK, USAGE
from ..registry import Command
from ..config import DEFAULTS, _VALID

SCOPES = ("user", "project")

DESCRIPTIONS = {
    "project_root": "Repository root (auto-detected when unset)",
    "preferred_branch": "Branch used by git workflows (e.g. sync)",
    "default_remote": "Default git remote name (origin)",
    "preferred_terminal": "Terminal for `open`-style actions (auto when unset)",
    "preferred_shell": "Shell used for shell commands (auto when unset)",
    "wsl_distro": "WSL distribution to use by default",
    "gradle_wrapper": "wrapper (gradlew) or system (gradle on PATH)",
    "confirmations": "ask | yes | no — confirmation behavior",
    "theme": "auto | light | dark — output theme",
    "verbosity": "quiet | normal | verbose",
}


def _cmd_list(args: argparse.Namespace, ctx: Context) -> int:
    if args.json:
        ctx.out.json(ctx.cfg.describe())
        return OK
    ctx.out.banner("CLI configuration")
    rows = [(key, str(ctx.cfg[key]), DESCRIPTIONS.get(key, "")) for key in ctx.cfg.keys()]
    width = max(len(k) for k, _, _ in rows)
    for key, value, description in rows:
        ctx.out.line(f"  {key.ljust(width)}  {value:<12} {description}")
    ctx.out.line()
    ctx.out.dim("User config: " + str(ctx.cfg.user_file))
    if ctx.cfg.project_file:
        ctx.out.dim("Project config: " + str(ctx.cfg.project_file))
    return OK


def _cmd_get(args: argparse.Namespace, ctx: Context) -> int:
    if args.key not in DEFAULTS:
        raise CliError(f"Unknown config key: {args.key}", code=USAGE,
                       hint=f"Valid keys: {', '.join(DEFAULTS)}")
    value = ctx.cfg[args.key]
    if args.json:
        ctx.out.json({args.key: value})
        return OK
    ctx.out.line(str(value if value is not None else ""))
    return OK


def _cmd_set(args: argparse.Namespace, ctx: Context) -> int:
    if args.key not in DEFAULTS:
        raise CliError(f"Unknown config key: {args.key}", code=USAGE,
                       hint=f"Valid keys: {', '.join(DEFAULTS)}")
    if args.key in _VALID and args.value not in _VALID[args.key]:
        raise CliError(f"Invalid value {args.value!r} for {args.key}.", code=USAGE,
                       hint=f"Valid values: {', '.join(_VALID[args.key])}")
    scope = args.scope if args.scope in SCOPES else "user"
    ctx.cfg.set(args.key, args.value, scope=scope)
    ctx.out.ok(f"{args.key} = {args.value} ({scope})")
    return OK


def _cmd_reset(args: argparse.Namespace, ctx: Context) -> int:
    scope = args.scope if args.scope in SCOPES else "user"
    if args.key and args.key not in DEFAULTS:
        raise CliError(f"Unknown config key: {args.key}", code=USAGE,
                       hint=f"Valid keys: {', '.join(DEFAULTS)}")
    ctx.cfg.set(args.key or "project_root", None, scope=scope)
    ctx.out.ok("Reset " + (args.key or "project_root"))
    return OK


def _settings_menu(ctx: Context) -> int:
    while True:
        options = [f"{key} = {ctx.cfg[key]}" for key in ctx.cfg.keys()] + ["Show config files"]
        choice = ctx.menu("Settings", options)
        if choice is None:
            return OK
        if choice == len(options) - 1:
            _cmd_list(argparse.Namespace(json=False), ctx)
            continue
        key = ctx.cfg.keys()[choice]
        hint = ""
        if key in _VALID:
            hint = f" ({' | '.join(_VALID[key])})"
        value = ctx.prompt(f"New value for {key}{hint}", default=str(ctx.cfg[key] or ""))
        if value == str(ctx.cfg[key] or ""):
            continue
        try:
            _cmd_set(argparse.Namespace(key=key, value=value, scope="user"), ctx)
        except CliError as exc:
            ctx.out.error(exc.message)
            if exc.hint:
                ctx.out.dim(f"  {exc.hint}")


def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    sub.add_argument("--scope", choices=SCOPES, default="user", help="config scope")
    subs = sub.add_subparsers(dest="settings_command", metavar="COMMAND")

    def P(name: str, **kw: object):
        if common is not None:
            kw["parents"] = [common]
        return subs.add_parser(name, **kw)  # type: ignore[arg-type]

    p = P("list", help="Show all configuration values")
    p.set_defaults(handler=_cmd_list)
    p = P("get", help="Show one value")
    p.add_argument("key")
    p.set_defaults(handler=_cmd_get)
    p = P("set", help="Set a value")
    p.add_argument("key")
    p.add_argument("value")
    p.set_defaults(handler=_cmd_set)
    p = P("reset", help="Reset a key to its default")
    p.add_argument("key", nargs="?")
    p.set_defaults(handler=_cmd_reset)


command = Command(
    name="settings",
    aliases=["config"],
    help="View and edit the central CLI configuration",
    description=(
        "One configuration source shared by every command. Values resolve as:\n"
        "defaults < ~/.kaiteyo/cli/config.json (user) < <root>/.kaiteyo/config.json (project)\n"
        "< KAITEYO_* environment variables.\n\n"
        "Examples:\n"
        "  kaiteyo settings\n"
        "  kaiteyo settings list\n"
        "  kaiteyo settings get wsl_distro\n"
        "  kaiteyo settings set gradle_wrapper wrapper\n"
        "  kaiteyo settings set confirmations ask --scope project\n"
    ),
    build=build,
    run=lambda args, ctx: _dispatch(args, ctx),
    menu=_settings_menu,
    menu_label="Settings",
)


def _dispatch(args: argparse.Namespace, ctx: Context) -> int:
    handler = getattr(args, "handler", None)
    if handler:
        return handler(args, ctx)
    if ctx.interactive():
        return _settings_menu(ctx)
    raise CliError("Specify a settings subcommand.", code=2,
                   hint="Try: kaiteyo settings list | get KEY | set KEY VALUE | reset [KEY]")
