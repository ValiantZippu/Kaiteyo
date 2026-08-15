"""kaiteyo run — safe generic command runner.

Runs a command through a chosen category (git, gradle, wsl, powershell,
cmd/windows, shell, custom) with full visibility: working directory, command,
arguments, and environment notes are shown before execution. It is not an
arbitrary silent executor — the exact command line is always displayed.
"""

from __future__ import annotations

import argparse
import os

from .. import platform
from ..context import Context
from ..errors import CliError, ABORT, ENV, OK, USAGE
from ..registry import Command
from ..runner import run_stream
from ..secrets import mask_text
from .gradle import gradlew_command


def _category_command(ctx: Context, category: str, args: list[str]) -> list[str]:
    category = category.lower()
    if category in ("git", "g"):
        return ["git", *args]
    if category in ("gradle", "gr"):
        return gradlew_command(ctx) + args
    if category in ("wsl", "w"):
        exe = platform.wsl_exe()
        if not exe:
            raise CliError("WSL was not detected.", code=ENV,
                           hint="`kaiteyo wsl` explains how to enable WSL on Windows.")
        return [exe, *args]
    if category in ("powershell", "ps"):
        if not platform.which("powershell"):
            raise CliError("PowerShell was not found on PATH.", code=ENV,
                           hint="This command is only available on Windows.")
        return ["powershell", "-NoProfile", "-Command", " ".join(args)]
    if category in ("windows", "cmd", "win"):
        if not platform.is_windows() and not platform.in_wsl():
            raise CliError("cmd.exe is only available on Windows (or inside WSL).", code=ENV)
        return ["cmd.exe", "/c", " ".join(args)]
    if category in ("shell", "sh", "bash"):
        shell = os.environ.get("SHELL") or platform.which("bash") or "sh"
        return [shell, "-lc", " ".join(args)]
    if category in ("custom", "c"):
        if not args:
            raise CliError("Custom command is empty.", code=USAGE)
        return args
    raise CliError(f"Unknown category: {category}", code=USAGE,
                   hint="Categories: git, gradle, wsl, powershell, cmd, shell, custom")


def _cmd_run(args: argparse.Namespace, ctx: Context) -> int:
    run_args = getattr(args, "run_args", None) or []
    category = getattr(args, "category", None)
    if not category and not run_args:
        raise CliError("A category and command are required.", code=USAGE,
                       hint="Example: kaiteyo run git status — see `kaiteyo run --help`.")
    category = category or "custom"
    command = _category_command(ctx, category, run_args)

    cwd = str(ctx.root or os.getcwd())
    ctx.out.banner("Command preview")
    ctx.out.info(f"Working directory: {cwd}")
    ctx.out.info(f"Command:           {mask_text(' '.join(command))}")
    if category in ("gradle", "gr"):
        ctx.out.info("Environment:       Gradle wrapper (JAVA_HOME may be needed)")
    elif category in ("wsl", "w"):
        ctx.out.info("Environment:       WSL (runs in the default/configured distribution)")
    if not ctx.confirm("Execute?", default=True):
        raise CliError("Run cancelled.", code=ABORT)
    result = run_stream(command, cwd=cwd, echo=False)
    if result.exit_code != 0:
        raise CliError(f"Command failed (exit {result.exit_code}).", code=1,
                       hint="See the output above.")
    return OK


def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    sub.add_argument("category", nargs="?", help="git | gradle | wsl | powershell | cmd | shell | custom")
    # dest="run_args" — must not collide with the top-level `command` dest.
    sub.add_argument("run_args", nargs=argparse.REMAINDER, help="the command and its arguments")


command = Command(
    name="run",
    help="Run a command through a category with full visibility (preview before execute)",
    description=(
        "Runs a command inside a chosen category. Always shows the working\n"
        "directory, exact command, and environment notes before executing.\n\n"
        "Examples:\n"
        "  kaiteyo run git status\n"
        "  kaiteyo run gradle :desktopApp:compileKotlinJvm\n"
        "  kaiteyo run wsl ls -la /mnt/c\n"
        "  kaiteyo run powershell Get-Process\n"
        "  kaiteyo run cmd dir\n"
        "  kaiteyo run shell 'echo hi'\n"
        "  kaiteyo run custom curl -I https://example.com\n"
    ),
    build=build,
    run=lambda args, ctx: _cmd_run(args, ctx),
)
