"""kaiteyo dev — the developer toolbox.

A hub that routes into the other commands (git, gradle, wsl, files, logs,
docs, release, doctor) plus a few quick views (environment, dependencies,
testing) — without duplicating their implementation.
"""

from __future__ import annotations

import argparse
import os
import pathlib

from ..context import Context
from ..errors import CliError, OK
from ..registry import Command

_IMPORTED: dict[str, object] = {}


def _cmd(name: str) -> object:
    if name not in _IMPORTED:
        import importlib

        module = importlib.import_module(f".{name}", package=__package__)
        _IMPORTED[name] = module.command
    return _IMPORTED[name]


def _quick_env(ctx: Context) -> int:
    keys = ["JAVA_HOME", "ANDROID_HOME", "ANDROID_SDK_ROOT", "PATH", "SHELL",
            "GRADLE_USER_HOME", "NO_COLOR", "WSL_DISTRO_NAME"]
    ctx.out.banner("Environment (selected variables)")
    for key in keys:
        value = os.environ.get(key, "")
        if key == "PATH":
            ctx.out.line(f"  PATH: {value}")
        else:
            ctx.out.line(f"  {key}={value}")
    return OK


def _quick_dependencies(ctx: Context) -> int:
    root = ctx.require_root("The dev command")
    catalog = root / "gradle" / "libs.versions.toml"
    if not catalog.is_file():
        ctx.out.warn("No gradle/libs.versions.toml found.")
        return OK
    ctx.out.banner("Dependencies (version catalog)")
    for line in catalog.read_text(encoding="utf-8", errors="replace").splitlines():
        stripped = line.strip()
        if stripped and not stripped.startswith(("#", "[", "]")):
            ctx.out.line(f"  {line.rstrip()}")
    return OK


def _quick_testing(ctx: Context) -> int:
    ctx.out.banner("Testing")
    ctx.out.line("  Core tests:   ./gradlew :core:allTests")
    ctx.out.line("  Desktop:      ./gradlew :desktopApp:test")
    ctx.out.line("  KJD platform: ./gradlew :kjd:test")
    ctx.out.line("  See docs/testing/README.md for the full strategy.")
    return OK


def _dev_menu(ctx: Context) -> int:
    sections = [
        ("Git", "git", lambda: _cmd("git").menu(ctx)),
        ("Gradle", "gradle", lambda: _cmd("gradle").menu(ctx)),
        ("WSL", "wsl", lambda: _cmd("wsl").menu(ctx)),
        ("Environment (quick view)", "env", lambda: _quick_env(ctx)),
        ("Files", "files", lambda: _cmd("files").menu(ctx)),
        ("Logs", "logs", lambda: _cmd("logs").menu(ctx)),
        ("Dependencies (quick view)", "deps", lambda: _quick_dependencies(ctx)),
        ("Testing (quick view)", "test", lambda: _quick_testing(ctx)),
        ("Documentation", "docs", lambda: _cmd("docs").menu(ctx)),
        ("Release (read-only)", "release", lambda: _cmd("release").menu(ctx)),
        ("Diagnostics", "doctor", lambda: _cmd("doctor").run(argparse.Namespace(), ctx)),
    ]
    while True:
        options = [label for label, _, _ in sections]
        choice = ctx.menu("Developer Toolbox", options)
        if choice is None:
            return OK
        try:
            sections[choice][2]()
        except CliError as exc:
            ctx.out.error(exc.message)
            if exc.hint:
                ctx.out.dim(f"  {exc.hint}")


def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    sub.add_argument("section", nargs="?", choices=["env", "deps", "test"],
                     help="non-interactive section: env, deps, test")


def _noninteractive_section(ctx: Context, section: str) -> int:
    if section == "env":
        return _quick_env(ctx)
    if section == "deps":
        return _quick_dependencies(ctx)
    if section == "test":
        return _quick_testing(ctx)
    raise CliError("Interactive sections require a terminal.", code=2,
                   hint="In scripts use the underlying commands directly, e.g. `kaiteyo git status`.")


command = Command(
    name="dev",
    aliases=["d"],
    help="Developer toolbox: route to git, gradle, wsl, env, files, logs, docs, release, doctor",
    description=(
        "The central developer toolbox. Routes into the dedicated commands and\n"
        "adds quick views for environment, dependencies and testing — reusing\n"
        "their implementations instead of duplicating them.\n\n"
        "Examples:\n"
        "  kaiteyo dev                  interactive toolbox\n"
        "  kaiteyo dev env              environment quick view\n"
        "  kaiteyo dev deps             version catalog quick view\n"
    ),
    build=build,
    run=lambda args, ctx: _dev_dispatch(args, ctx),
    menu=_dev_menu,
    menu_label="Dev Tools",
)


def _dev_dispatch(args: argparse.Namespace, ctx: Context) -> int:
    section = getattr(args, "section", None)
    if section:
        return _noninteractive_section(ctx, section)
    if ctx.interactive():
        return _dev_menu(ctx)
    raise CliError("Specify a dev section.", code=2,
                   hint="Try: kaiteyo dev env | deps | test")
