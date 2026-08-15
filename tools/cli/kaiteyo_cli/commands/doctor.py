"""kaiteyo doctor — environment diagnostics.

Inspects the development environment WITHOUT changing anything. Reports
PASS/WARN/FAIL per check, a summary, and machine-readable output with --json.
Nothing is auto-fixed; remediation hints are printed, not executed.
"""

from __future__ import annotations

import argparse
import json
import os
import pathlib
import shutil

from .. import platform
from ..context import Context
from ..errors import OK
from ..registry import Command
from ..runner import run_capture
from .. import root as root_mod

PASS, WARN, FAIL, INFO = "pass", "warn", "fail", "info"


def _check_version(exe: str, arg: str = "--version") -> str | None:
    result = run_capture([exe, arg])
    if result.exit_code != 0:
        return None
    return result.output.strip().splitlines()[0] if result.output.strip() else ""


def _git_user_config(ctx: Context, root: pathlib.Path) -> tuple[str, str]:
    def get(name: str) -> str:
        result = run_capture(["git", "-C", str(root), "config", "--get", name])
        return result.output.strip() if result.exit_code == 0 else ""
    return get("user.name"), get("user.email")


def _java_version() -> str | None:
    exe = shutil.which("java")
    if not exe:
        return None
    result = run_capture([exe, "-version"])
    # java prints to stderr.
    line = result.output.strip().splitlines()[0] if result.output.strip() else ""
    return line or None


def _disk_free(path: pathlib.Path) -> tuple[int, int]:
    try:
        usage = shutil.disk_usage(str(path))
        return usage.free, usage.total
    except OSError:
        return -1, -1


def run_checks(ctx: Context) -> list[dict]:
    checks: list[dict] = []
    root = ctx.root
    exe = shutil.which

    # -- platform ----------------------------------------------------------
    checks.append({
        "name": "os",
        "status": INFO,
        "detail": f"{platform.os_name()} ({platform.arch()})"
                  + (" — running inside WSL" if platform.in_wsl() else ""),
        "hint": None,
    })
    checks.append({
        "name": "python",
        "status": PASS,
        "detail": f"Python {platform.python_executable()} — the CLI itself",
        "hint": None,
    })

    # -- git ---------------------------------------------------------------
    if exe("git"):
        version = _check_version("git", "--version")
        checks.append({"name": "git", "status": PASS, "detail": version or "git found",
                       "hint": None})
        if root:
            name, email = _git_user_config(ctx, root)
            if name and email:
                checks.append({"name": "git config", "status": PASS,
                               "detail": f"user.name={name}, user.email={email}", "hint": None})
            else:
                missing = [k for k, v in (("user.name", name), ("user.email", email)) if not v]
                checks.append({"name": "git config", "status": WARN,
                               "detail": "missing " + ", ".join(missing),
                               "hint": "git config user.name \"You\" && git config user.email you@example.com"})
    else:
        checks.append({"name": "git", "status": FAIL, "detail": "not found on PATH",
                       "hint": "Install git: https://git-scm.com/downloads"})

    # -- java --------------------------------------------------------------
    java = _java_version()
    if java:
        ok17 = "17" in java
        checks.append({"name": "java", "status": PASS if ok17 else WARN,
                       "detail": java,
                       "hint": None if ok17 else "Kaiteyo builds require JDK 17 (jvmToolchain(17))."})
    else:
        checks.append({"name": "java", "status": FAIL, "detail": "not found on PATH",
                       "hint": "Install a JDK 17 (e.g. Temurin) and put java on PATH."})
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        ok_dir = pathlib.Path(java_home).is_dir()
        checks.append({"name": "JAVA_HOME", "status": PASS if ok_dir else WARN,
                       "detail": java_home,
                       "hint": None if ok_dir else "JAVA_HOME points to a missing directory."})
    else:
        checks.append({"name": "JAVA_HOME", "status": WARN, "detail": "not set",
                       "hint": "Gradle works without it, but some tooling expects JAVA_HOME."})

    # -- gradle wrapper ----------------------------------------------------
    if root:
        gradlew = (root / "gradlew") if not platform.is_windows() else (root / "gradlew.bat")
        if gradlew.is_file():
            version = None
            props = root / "gradle" / "wrapper" / "gradle-wrapper.properties"
            if props.is_file():
                for line in props.read_text(encoding="utf-8", errors="replace").splitlines():
                    if line.startswith("distributionUrl=") and "gradle-" in line:
                        import re
                        match = re.search(r"gradle-([\d.]+)", line)
                        if match:
                            version = match.group(1)
            checks.append({"name": "gradle wrapper", "status": PASS,
                           "detail": f"present (Gradle {version})" if version else "present",
                           "hint": None})
        else:
            checks.append({"name": "gradle wrapper", "status": FAIL,
                           "detail": "gradlew not found",
                           "hint": "Run from the repository root, or regenerate the wrapper."})
    else:
        checks.append({"name": "gradle wrapper", "status": WARN, "detail": "no repository root",
                       "hint": None})

    # -- WSL ---------------------------------------------------------------
    wsl = platform.wsl_exe()
    if wsl:
        from .wsl import _wsl_output

        code, text = _wsl_output(wsl, ["--list", "--quiet"])
        distro_list = [l.strip() for l in text.splitlines() if l.strip()] if code == 0 else []
        checks.append({"name": "wsl", "status": PASS,
                       "detail": "detected — distributions: " + (", ".join(distro_list) or "none"),
                       "hint": None})
    else:
        checks.append({"name": "wsl", "status": INFO,
                       "detail": "not applicable" if platform.is_macos() else "not detected",
                       "hint": None if platform.is_macos()
                       else "Only relevant on Windows; `kaiteyo wsl` explains how to enable it."})

    # -- repository --------------------------------------------------------
    if root:
        from .git import _snapshot, _require_git
        _require_git(ctx)
        try:
            snap = _snapshot(ctx)
            checks.append({"name": "repository", "status": PASS if snap.clean else WARN,
                           "detail": f"{snap.branch} — "
                                     + ("clean" if snap.clean
                                        else f"{len(snap.all_changed)} changed file(s)"),
                           "hint": None})
        except Exception as exc:  # pragma: no cover - defensive
            checks.append({"name": "repository", "status": WARN,
                           "detail": f"git status unavailable ({exc})", "hint": None})
    else:
        checks.append({"name": "repository", "status": WARN, "detail": "not a Kaiteyo repo root",
                       "hint": "Run from the repository or pass --root PATH."})

    # -- disk --------------------------------------------------------------
    target = root or pathlib.Path.cwd()
    free, total = _disk_free(target)
    if free < 0:
        checks.append({"name": "disk", "status": WARN, "detail": "could not read disk usage",
                       "hint": None})
    else:
        free_gb = free / (1024 ** 3)
        status = PASS if free_gb > 10 else (WARN if free_gb > 2 else FAIL)
        checks.append({"name": "disk", "status": status,
                       "detail": f"{free_gb:.1f} GB free on {target}",
                       "hint": None if status == PASS
                       else "Free space for builds: 10 GB+ recommended."})

    # -- android env -------------------------------------------------------
    for var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        value = os.environ.get(var)
        checks.append({"name": var, "status": PASS if value else WARN,
                       "detail": value or "not set",
                       "hint": None if value else "Needed for Android builds (see docs/development/COMMANDS.md)."})

    # -- optional tools ----------------------------------------------------
    for tool, needed in (("rsync", "backup/sync"), ("rg", "fast file search")):
        checks.append({"name": tool, "status": PASS if exe(tool) else INFO,
                       "detail": "found" if exe(tool) else "not found (optional)",
                       "hint": None if exe(tool) else f"Used by kaiteyo {needed}; install for that feature."})

    return checks


def _cmd_doctor(args: argparse.Namespace, ctx: Context) -> int:
    checks = run_checks(ctx)
    if ctx.json_mode:
        ctx.out.json({"checks": checks,
                      "summary": {
                          "pass": sum(1 for c in checks if c["status"] == PASS),
                          "warn": sum(1 for c in checks if c["status"] == WARN),
                          "fail": sum(1 for c in checks if c["status"] == FAIL),
                      }})
        return OK
    ctx.out.banner("Kaiteyo doctor — environment diagnostics")
    for check in checks:
        status = check["status"]
        name = check["name"]
        detail = check["detail"] or ""
        if status == PASS:
            ctx.out.ok(f"{name}: {detail}")
        elif status == WARN:
            ctx.out.warn(f"{name}: {detail}")
        elif status == FAIL:
            ctx.out.fail(f"{name}: {detail}")
        else:
            ctx.out.info(f"{name}: {detail}")
        if check.get("hint"):
            ctx.out.dim(f"         → {check['hint']}")
    fails = sum(1 for c in checks if c["status"] == FAIL)
    warns = sum(1 for c in checks if c["status"] == WARN)
    ctx.out.line()
    if fails:
        ctx.out.fail(f"{fails} failing, {warns} warnings — see hints above. Nothing was modified.")
    elif warns:
        ctx.out.warn(f"No failures; {warns} warnings — see hints above.")
    else:
        ctx.out.ok("All checks passed.")
    return OK


def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    pass


command = Command(
    name="doctor",
    help="Inspect the environment (OS, git, Java, Gradle, WSL, repo, disk) — read-only",
    description=(
        "Runs PASS/WARN/FAIL checks across OS, Java/JAVA_HOME, Gradle wrapper, git, WSL,\n"
        "repository state, disk space and key environment variables. Never modifies anything.\n\n"
        "Examples:\n"
        "  kaiteyo doctor\n"
        "  kaiteyo doctor --json\n"
    ),
    build=build,
    run=lambda args, ctx: _cmd_doctor(args, ctx),
)
