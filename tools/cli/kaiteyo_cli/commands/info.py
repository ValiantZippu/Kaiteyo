"""kaiteyo info — project information snapshot.

Fast, read-only project overview: name, repository, branch, commit, working
tree state, version, platform, Java, Gradle wrapper and modules. No build is
run — everything comes from static files and cheap git calls.
"""

from __future__ import annotations

import argparse
import os
import pathlib
import re
import shutil

from .. import platform
from ..context import Context
from ..errors import CliError, ENV, OK
from ..registry import Command
from ..runner import run_capture
from ..secrets import mask_remote
from .gradle import modules_from_settings, wrapper_version

# ``git status`` must stat every file in the working tree; on network mounts
# (WSL/9p, network drives) that can exceed a minute. info is a fast snapshot —
# cap the scan and report "unknown" instead of hanging the whole command.
INFO_GIT_STATUS_TIMEOUT = 90.0  # seconds


def _git(ctx: Context, *args: str, timeout: float | None = None) -> str:
    root = ctx.require_root("The info command")
    result = run_capture(["git", "-C", str(root), *args], timeout=timeout)
    return result.output.strip() if result.exit_code == 0 else ""


def _working_tree(ctx: Context, root: pathlib.Path) -> str:
    """Dirty/clean state, bounded so a slow filesystem can't hang the command.

    ``git status`` has to stat every tracked + untracked file; on network
    mounts (WSL/9p, network drives) that can take a minute or more. We give it
    a generous window, show that we are scanning, and degrade to an explicit
    "unknown" instead of leaving the user staring at a frozen terminal.
    """
    # Progress goes to stderr so it is visible even when stdout is piped/JSON.
    ctx.out.note("  scanning working tree (git status) — slow on network filesystems…")
    result = run_capture(["git", "-C", str(root), "status", "--porcelain"],
                         timeout=INFO_GIT_STATUS_TIMEOUT)
    if result.timed_out:
        ctx.out.note(f"  git status did not finish in {INFO_GIT_STATUS_TIMEOUT:.0f}s — marking unknown.")
        return "unknown (git status timed out)"
    if result.exit_code != 0:
        return "unknown"
    return "dirty" if result.output.strip() else "clean"


def version_from_source(root: pathlib.Path) -> str | None:
    version_file = root / "buildSrc" / "src" / "main" / "kotlin" / "AppVersion.kt"
    if not version_file.is_file():
        return None
    match = re.search(r'versionName\s*=\s*"([^"]+)"',
                      version_file.read_text(encoding="utf-8", errors="replace"))
    return match.group(1) if match else None


def project_name(root: pathlib.Path) -> str:
    settings = root / "settings.gradle.kts"
    if settings.is_file():
        match = re.search(r'rootProject\.name\s*=\s*"([^"]+)"',
                          settings.read_text(encoding="utf-8", errors="replace"))
        if match:
            return match.group(1)
    return root.name


def _cmd_info(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.require_root("The info command")
    # Fast fields first — these read HEAD/config/static files and return
    # immediately. The working-tree scan (git status) is deferred to last so
    # the snapshot appears instantly on slow filesystems.
    branch = _git(ctx, "rev-parse", "--abbrev-ref", "HEAD")
    commit = _git(ctx, "rev-parse", "--short", "HEAD")
    remote = _git(ctx, "remote", "get-url", "origin") or ""
    java = ""
    if shutil.which("java"):
        result = run_capture(["java", "-version"])
        java = result.output.strip().splitlines()[0] if result.output.strip() else ""

    info = {
        "project": project_name(root),
        "repository": mask_remote(remote) if remote else "",
        "branch": branch,
        "commit": commit,
        "version": version_from_source(root),
        "platform": f"{platform.os_name()} ({platform.arch()})",
        "java": java or "not found",
        "gradle_wrapper": wrapper_version(root) or "unknown",
        "modules": modules_from_settings(root),
        "root": str(root),
    }
    if ctx.json_mode:
        # JSON needs the complete snapshot; the working-tree scan is bounded.
        info["working_tree"] = _working_tree(ctx, root)
        ctx.out.json(info)
        return OK
    ctx.out.banner(f"{info['project']} — project info")
    rows = [
        ("Project", info["project"]),
        ("Repository", info["repository"] or "(no origin remote)"),
        ("Branch", info["branch"] or "(detached)"),
        ("Commit", info["commit"] or "-"),
        ("Version", info["version"] or "(undetectable)"),
        ("Platform", info["platform"]),
        ("Java", info["java"]),
        ("Gradle wrapper", info["gradle_wrapper"]),
        ("Root", info["root"]),
    ]
    width = max(len(k) for k, _ in rows)
    for key, value in rows:
        ctx.out.line(f"  {key.ljust(width)}  {value}")
    ctx.out.section("Modules")
    for module in info["modules"]:
        ctx.out.line(f"  {module}")
    # The working-tree scan is the only slow field — run it last and append it.
    dirty = _working_tree(ctx, root)
    ctx.out.line()
    ctx.out.line(f"  {'Working tree'.ljust(width)}  {dirty}")
    return OK


def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    pass


command = Command(
    name="info",
    help="Show project information (branch, commit, version, modules) — no build required",
    description=(
        "Fast snapshot of the repository: name, remote, branch, commit, working tree state,\n"
        "version, platform, Java, Gradle wrapper version and modules. Nothing is built.\n\n"
        "Examples:\n"
        "  kaiteyo info\n"
        "  kaiteyo info --json\n"
    ),
    build=build,
    run=lambda args, ctx: _cmd_info(args, ctx),
    menu=lambda ctx: _cmd_info(argparse.Namespace(), ctx),
    menu_label="Project Info",
)
