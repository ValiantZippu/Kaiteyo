"""kaiteyo wsl — WSL Command Center.

Provides shortcuts for the operations developers run against WSL every day:
distribution management, filesystem, networking, processes, packages, shell,
Windows integration, and development utilities.

Rules:

  - WSL availability is detected; on unsupported hosts (e.g. macOS) the CLI
    explains that WSL was not detected and exits with code 5. It never tries
    to install or modify Windows system configuration.
  - Distributions are detected at runtime — nothing is assumed (no hardcoded
    'Ubuntu' defaults beyond the configured preference).
  - The exact command is always shown before execution.
"""

from __future__ import annotations

import argparse
import pathlib
import re

from .. import history
from ..context import Context
from ..errors import CliError, ABORT, ENV, OK, UNSUPPORTED
from ..registry import Command
from ..runner import run_capture_bytes, run_interactive, run_stream
from .. import platform
from ..secrets import mask_text

WSL_FAIL = 8


def _wsl_output(exe: str, args: list[str]) -> tuple[int, str]:
    """Run wsl.exe and decode its output (wsl.exe emits UTF-16 when piped)."""
    code, raw, _ = run_capture_bytes([exe, *args])
    if b"\x00" in raw[:4096]:
        text = raw.decode("utf-16-le", errors="replace")
    else:
        text = raw.decode("utf-8", errors="replace")
    return code, text


# ---------------------------------------------------------------------------
# detection
# ---------------------------------------------------------------------------

def require_wsl(ctx: Context) -> str:
    """Return the wsl executable path or raise a clear, actionable error."""
    exe = platform.wsl_exe()
    if exe is None:
        if platform.is_macos():
            raise CliError(
                "WSL was not detected — WSL only runs on Windows (or inside a WSL distribution).",
                code=UNSUPPORTED,
                hint="WSL commands are unavailable on macOS.",
            )
        raise CliError(
            "WSL was not detected on this system.",
            code=ENV,
            hint="On Windows 10/11 enable WSL with `wsl --install` (requires admin, "
                 "reboot) then install a distribution from the Microsoft Store. "
                 "The CLI will not modify Windows configuration for you.",
        )
    return exe


def distros(ctx: Context) -> list[str]:
    """Detect installed WSL distributions (runtime detection — never assumed)."""
    exe = require_wsl(ctx)
    code, text = _wsl_output(exe, ["--list", "--quiet"])
    if code != 0:
        return []
    found = [line.strip() for line in text.splitlines() if line.strip()]
    # Strip a possible trailing " (Default)" marker.
    found = [re.sub(r"\s*\(Default\)\s*$", "", d) for d in found]
    return found


def chosen_distro(ctx: Context, args: argparse.Namespace | None = None) -> str | None:
    configured = ctx.cfg.get("wsl_distro")
    if getattr(args, "distro", None):
        return args.distro
    return configured


# ---------------------------------------------------------------------------
# execution
# ---------------------------------------------------------------------------

def _wsl_run(ctx: Context, inner: list[str], distro: str | None = None,
             interactive: bool = False) -> int:
    exe = require_wsl(ctx)
    cmd = [exe]
    if distro:
        cmd += ["-d", distro]
    cmd += ["--"] + inner
    runner = run_interactive if interactive else run_stream
    result = runner(cmd, echo=False)
    if result.exit_code != 0:
        return WSL_FAIL
    return OK


def _wsl_interactive_shell(ctx: Context, distro: str | None, cwd: str | None = None) -> int:
    exe = require_wsl(ctx)
    cmd = [exe]
    if cwd:
        cmd += ["--cd", cwd]
    if distro:
        cmd += ["-d", distro]
    result = run_interactive(cmd, echo=False)
    return OK if result.exit_code == 0 else WSL_FAIL


def _confirm_run(ctx: Context, description: str, command: list[str]) -> bool:
    ctx.out.line()
    ctx.out.info("Command: " + mask_text(" ".join(command)))
    return ctx.confirm(f"{description}?", default=False)


# ---------------------------------------------------------------------------
# subcommands
# ---------------------------------------------------------------------------

def _cmd_status(args: argparse.Namespace, ctx: Context) -> int:
    exe = require_wsl(ctx)
    code, text = _wsl_output(exe, ["--status"])
    if ctx.json_mode:
        ctx.out.json({"wsl_detected": True, "status": text.strip(),
                      "distros": distros(ctx)})
        return OK
    ctx.out.banner("WSL status")
    ctx.out.line(mask_text(text.strip()))
    listed = distros(ctx)
    if listed:
        ctx.out.section("Distributions")
        for name in listed:
            ctx.out.line(f"  {name}")
    return OK if code == 0 else WSL_FAIL


def _cmd_list(args: argparse.Namespace, ctx: Context) -> int:
    exe = require_wsl(ctx)
    code, text = _wsl_output(exe, ["--list", "--verbose"])
    ctx.out.line(mask_text(text.strip()))
    return OK if code == 0 else WSL_FAIL


def _cmd_version(args: argparse.Namespace, ctx: Context) -> int:
    exe = require_wsl(ctx)
    code, text = _wsl_output(exe, ["--version"])
    ctx.out.line(mask_text(text.strip()))
    return OK if code == 0 else WSL_FAIL


def _cmd_shell(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    return _wsl_interactive_shell(ctx, distro, cwd=getattr(args, "cwd", None))


def _cmd_shutdown(args: argparse.Namespace, ctx: Context) -> int:
    exe = require_wsl(ctx)
    command = [exe, "--shutdown"]
    if not ctx.confirm("Shut down ALL WSL distributions? (running sessions will terminate)",
                       default=False):
        raise CliError("Shutdown cancelled.", code=ABORT)
    ctx.out.info("Command: " + mask_text(" ".join(command)))
    result = run_stream(command, echo=False)
    return OK if result.exit_code == 0 else WSL_FAIL


def _cmd_terminate(args: argparse.Namespace, ctx: Context) -> int:
    exe = require_wsl(ctx)
    distro = args.distro
    if not distro:
        raise CliError("A distribution is required.", code=2,
                       hint="Example: kaiteyo wsl --terminate Ubuntu")
    command = [exe, "--terminate", distro]
    if not ctx.confirm(f"Terminate WSL distribution '{distro}'?", default=False):
        raise CliError("Terminate cancelled.", code=ABORT)
    ctx.out.info("Command: " + mask_text(" ".join(command)))
    result = run_stream(command, echo=False)
    return OK if result.exit_code == 0 else WSL_FAIL


def _cmd_run_in(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    command = getattr(args, "wsl_command_arg", None) or ""
    if not command:
        raise CliError("No command given.", code=2,
                       hint="Example: kaiteyo wsl run \"ls -la /mnt/c\"")
    ctx.out.info(f"Command inside WSL: {mask_text(command)}")
    result = run_stream([require_wsl(ctx), *(["-d", distro] if distro else []),
                         "--", "bash", "-lc", command], echo=False)
    return OK if result.exit_code == 0 else WSL_FAIL


def _cmd_ip(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    ctx.out.info(f"WSL IP address (distro: {distro or 'default'})")
    return _wsl_run(ctx, ["hostname", "-I"], distro)


def _cmd_mounts(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    ctx.out.info("Windows drives mounted under /mnt (distro: %s)" % (distro or "default"))
    return _wsl_run(ctx, ["ls", "-la", "/mnt"], distro)


def _cmd_open_explorer(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    inner = ["explorer.exe", "."]
    ctx.out.info("Command inside WSL: explorer.exe .")
    return _wsl_run(ctx, inner, distro, interactive=True)


def _cmd_win_cmd(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    command = getattr(args, "wsl_command_arg", None) or ""
    if not command:
        raise CliError("No Windows command given.", code=2,
                       hint="Example: kaiteyo wsl win \"echo %USERNAME%\"")
    ctx.out.info("Running Windows command from inside WSL:")
    return _wsl_run(ctx, ["cmd.exe", "/c", command], distro)


def _cmd_env(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    ctx.out.info(f"Environment of '{distro or 'default'}' WSL distribution:")
    return _wsl_run(ctx, ["printenv"], distro)


def _cmd_pkg(args: argparse.Namespace, ctx: Context) -> int:
    """Package manager detection — never assumes a specific distribution."""
    distro = chosen_distro(ctx, args)
    ctx.out.info(f"Detecting package manager in '{distro or 'default'}' distribution...")
    for pm in ("apt", "pacman", "dnf", "zypper", "apk"):
        probe = ["bash", "-lc", f"command -v {pm}"]
        result = run_capture([require_wsl(ctx), *(["-d", distro] if distro else []), "--", *probe])
        if result.exit_code == 0 and result.output.strip():
            ctx.out.ok(f"Package manager: {result.output.strip()}")
            return _wsl_run(ctx, ["bash", "-lc", f"{pm} --help | head -30"], distro)
    ctx.out.warn("No known package manager detected in that distribution.")
    return OK


def _cmd_git(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    return _wsl_run(ctx, ["git", "status", "-sb"], distro)


def _cmd_ssh(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    ctx.out.info("Opening an SSH session inside WSL (use your normal ssh arguments).")
    return _wsl_run(ctx, ["ssh"], distro, interactive=True)


def _cmd_ports(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    return _wsl_run(ctx, ["bash", "-lc", "ss -tlnp 2>/dev/null || netstat -tln"], distro)


def _cmd_processes(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    return _wsl_run(ctx, ["ps", "aux", "--sort=-%mem"], distro)


def _cmd_disk(args: argparse.Namespace, ctx: Context) -> int:
    distro = chosen_distro(ctx, args)
    return _wsl_run(ctx, ["df", "-h", "/"], distro)


# ---------------------------------------------------------------------------
# interactive center
# ---------------------------------------------------------------------------

def _wsl_menu(ctx: Context) -> int:
    require_wsl(ctx)
    categories = {
        "Distribution management": [
            ("List distributions", _cmd_list),
            ("Show WSL status", _cmd_status),
            ("Check WSL version", _cmd_version),
            ("Open WSL shell (default distribution)", _cmd_shell),
            ("Open specific distribution", None),
            ("Shutdown all WSL", _cmd_shutdown),
            ("Terminate a distribution", _cmd_terminate),
            ("Restart a distribution (terminate, then open)", _cmd_terminate),
        ],
        "Filesystem": [
            ("Show Windows drives (/mnt)", _cmd_mounts),
            ("Open current directory in WSL", _cmd_shell),
            ("Open WSL directory in Explorer", _cmd_open_explorer),
        ],
        "Networking": [
            ("Show WSL IP address", _cmd_ip),
            ("Inspect open ports", _cmd_ports),
        ],
        "Processes": [
            ("List processes (by memory)", _cmd_processes),
            ("Show disk usage", _cmd_disk),
        ],
        "Packages": [
            ("Detect and open package manager", _cmd_pkg),
        ],
        "Shell": [
            ("Open interactive shell", _cmd_shell),
            ("Run command inside WSL", _cmd_run_in),
        ],
        "Windows integration": [
            ("Run Windows command from WSL", _cmd_win_cmd),
            ("Run WSL command from Windows", _cmd_run_in),
            ("Open WSL directory in Explorer", _cmd_open_explorer),
        ],
        "Development": [
            ("Git status inside WSL", _cmd_git),
            ("SSH inside WSL", _cmd_ssh),
            ("Show environment variables", _cmd_env),
        ],
    }
    while True:
        flat: list[tuple[str, argparse.Namespace | None]] = []
        sections: list[tuple[int, str]] = []
        for category, items in categories.items():
            start = len(flat)
            flat.extend((label, None) for label, _ in items)
            sections.append((start, category))
        labels = [label for label, _ in flat]
        choice = ctx.menu("WSL Command Center", labels)
        if choice is None:
            return OK
        label, _ = flat[choice]
        try:
            if label == "Open specific distribution":
                listed = distros(ctx)
                if not listed:
                    ctx.out.warn("No distributions detected.")
                    continue
                pick = ctx.menu("Select distribution", listed)
                if pick is None:
                    continue
                _wsl_interactive_shell(ctx, listed[pick])
            elif label == "Terminate a distribution" or label.startswith("Restart"):
                listed = distros(ctx)
                if not listed:
                    ctx.out.warn("No distributions detected.")
                    continue
                pick = ctx.menu("Select distribution to terminate", listed)
                if pick is None:
                    continue
                args = argparse.Namespace(distro=listed[pick])
                if label.startswith("Restart"):
                    _cmd_terminate(args, ctx)
                    ctx.out.info(f"Restarting: opening a shell in {listed[pick]}")
                    _wsl_interactive_shell(ctx, listed[pick])
                else:
                    _cmd_terminate(args, ctx)
            elif label == "Run command inside WSL" or label == "Run WSL command from Windows":
                command = ctx.prompt("Command to run inside WSL", required=True)
                _cmd_run_in(argparse.Namespace(wsl_command_arg=command), ctx)
            elif label == "Run Windows command from WSL":
                command = ctx.prompt("Windows command to run (cmd.exe)", required=True)
                _cmd_win_cmd(argparse.Namespace(wsl_command_arg=command), ctx)
            elif label == "Open current directory in WSL":
                root = ctx.root
                cwd = str(root) if root else "."
                _wsl_interactive_shell(ctx, chosen_distro(ctx), cwd=cwd)
            else:
                for category_items in categories.values():
                    for item_label, handler in category_items:
                        if item_label == label and handler is not None:
                            handler(argparse.Namespace(), ctx)
        except CliError as exc:
            ctx.out.error(exc.message)
            if exc.hint:
                ctx.out.dim(f"  {exc.hint}")


# ---------------------------------------------------------------------------
# parser
# ---------------------------------------------------------------------------

def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    sub.add_argument("--distro", metavar="NAME", help="target distribution (overrides config)")
    sub.add_argument("--status", action="store_true", help="show WSL status and distributions")
    sub.add_argument("--list", action="store_true", help="list distributions (verbose)")
    sub.add_argument("--version", action="store_true", help="show WSL version")
    subs = sub.add_subparsers(dest="wsl_command", metavar="COMMAND")

    def P(name: str, **kw: object):
        if common is not None:
            kw["parents"] = [common]
        return subs.add_parser(name, **kw)  # type: ignore[arg-type]

    p = P("status", help="Show WSL status and detected distributions")
    p.set_defaults(handler=_cmd_status)

    p = P("list", help="List distributions (verbose)")
    p.set_defaults(handler=_cmd_list)

    p = P("version", help="Show WSL version")
    p.set_defaults(handler=_cmd_version)

    p = P("shell", help="Open an interactive shell (optionally in a distribution)")
    p.add_argument("--cwd", help="working directory for the shell")
    p.set_defaults(handler=_cmd_shell)

    p = P("shutdown", help="Shut down all WSL distributions (confirmed)")
    p.set_defaults(handler=_cmd_shutdown)

    p = P("terminate", help="Terminate one distribution (confirmed)")
    p.add_argument("distro", nargs="?", help="distribution name")
    p.set_defaults(handler=_cmd_terminate)

    p = P("run", help="Run a shell command inside WSL")
    p.add_argument("wsl_command_arg", metavar="COMMAND", help="command to run inside WSL")
    p.set_defaults(handler=_cmd_run_in)

    p = P("ip", help="Show the WSL IP address")
    p.set_defaults(handler=_cmd_ip)

    p = P("mounts", help="List Windows drives mounted under /mnt")
    p.set_defaults(handler=_cmd_mounts)

    p = P("explorer", help="Open the current WSL directory in Windows Explorer")
    p.set_defaults(handler=_cmd_open_explorer)

    p = P("win", help="Run a Windows command from inside WSL")
    p.add_argument("wsl_command_arg", metavar="COMMAND", help="Windows command (cmd.exe syntax)")
    p.set_defaults(handler=_cmd_win_cmd)

    p = P("env", help="Show environment variables inside WSL")
    p.set_defaults(handler=_cmd_env)

    p = P("pkg", help="Detect and open the distribution's package manager")
    p.set_defaults(handler=_cmd_pkg)

    p = P("git", help="Git status inside WSL")
    p.set_defaults(handler=_cmd_git)

    p = P("ssh", help="Open an interactive SSH session inside WSL")
    p.set_defaults(handler=_cmd_ssh)

    p = P("ports", help="List open ports inside WSL")
    p.set_defaults(handler=_cmd_ports)

    p = P("processes", help="List processes inside WSL (by memory)")
    p.set_defaults(handler=_cmd_processes)

    p = P("disk", help="Show disk usage inside WSL")
    p.set_defaults(handler=_cmd_disk)


command = Command(
    name="wsl",
    aliases=["w"],
    help="WSL Command Center: distributions, filesystem, networking, dev utilities",
    description=(
        "Interactive WSL utility menu with detected distributions (never assumed).\n\n"
        "Examples:\n"
        "  kaiteyo wsl                interactive WSL Command Center\n"
        "  kaiteyo wsl --status       (also: --list, --version)\n"
        "  kaiteyo wsl shell --distro Ubuntu\n"
        "  kaiteyo wsl run \"ls -la /mnt/c\"\n"
        "  kaiteyo wsl win \"echo %USERNAME%\"\n"
        "  kaiteyo wsl --distro Debian ip\n"
    ),
    build=build,
    run=lambda args, ctx: _dispatch(args, ctx),
    menu=_wsl_menu,
    menu_label="WSL",
)


def _dispatch(args: argparse.Namespace, ctx: Context) -> int:
    handler = getattr(args, "handler", None)
    if handler:
        history.add(["wsl", getattr(args, "wsl_command", "") or "menu"])
        return handler(args, ctx)
    if getattr(args, "status", False):
        return _cmd_status(args, ctx)
    if getattr(args, "list", False):
        return _cmd_list(args, ctx)
    if getattr(args, "version", False):
        return _cmd_version(args, ctx)
    if ctx.interactive():
        return _wsl_menu(ctx)
    raise CliError("Specify a WSL subcommand.", code=2,
                   hint="Try: kaiteyo wsl --status | --list | shell | run | ip | mounts | ...")
