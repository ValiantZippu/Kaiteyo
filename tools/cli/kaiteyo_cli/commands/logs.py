"""kaiteyo logs — log viewer.

Log locations are detected from the project/application configuration, never
invented:

  - repository root build logs: build_log.txt, build_errors.txt,
    build_output.txt, run_build.txt, *_build*.txt (the names the project
    .gitignore already treats as build logs),
  - repository root *.log files (JVM crash dumps excluded by default),
  - .tools/*.log (tool artifact logs),
  - ~/.kaiteyo/*.log (desktop app logs, if any).

Provides: list, latest, follow, search, clear (confirmed), export.
"""

from __future__ import annotations

import argparse
import pathlib
import shutil
import time

from ..config import USER_CONFIG_DIR
from ..context import Context
from ..errors import CliError, ABORT, OK
from ..registry import Command
from ..runner import run_interactive

BUILD_LOG_NAMES = {"build_log.txt", "build_errors.txt", "build_output.txt",
                   "run_build.txt", "filelist.txt"}
JVM_DUMP = ("hs_err_pid", "replay_pid")


def _candidates(ctx: Context) -> list[pathlib.Path]:
    paths: list[pathlib.Path] = []
    if ctx.root:
        paths.append(ctx.root)
        tools = ctx.root / ".tools"
        if tools.is_dir():
            paths.append(tools)
    home_logs = USER_CONFIG_DIR.parent  # ~/.kaiteyo
    if home_logs.is_dir():
        paths.append(home_logs)
    return paths


def detect_logs(ctx: Context) -> list[pathlib.Path]:
    found: list[pathlib.Path] = []
    for base in _candidates(ctx):
        if base.name == ".kaiteyo":
            found.extend(sorted(base.glob("*.log")))
            continue
        for pattern in ("*.log", "*.txt"):
            for path in sorted(base.glob(pattern)):
                if path.name in BUILD_LOG_NAMES or pattern == "*.log":
                    if path.name.startswith(JVM_DUMP) and not ctx.cfg.get("show_crash_dumps", False):
                        continue
                    found.append(path)
    # De-duplicate and prefer project-root logs first.
    seen = set()
    unique = []
    for path in found:
        if path not in seen:
            seen.add(path)
            unique.append(path)
    return sorted(unique, key=lambda p: p.stat().st_mtime, reverse=True)


def _show_log(ctx: Context, path: pathlib.Path, lines: int = 40) -> None:
    try:
        content = path.read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError as exc:
        raise CliError(f"Cannot read {path}: {exc}")
    ctx.out.banner(f"{path} — last {min(lines, len(content))} of {len(content)} lines")
    for line in content[-lines:]:
        ctx.out.line(f"  {line}")


def _cmd_list(args: argparse.Namespace, ctx: Context) -> int:
    logs = detect_logs(ctx)
    if ctx.json_mode:
        ctx.out.json({"logs": [
            {"path": str(p), "bytes": p.stat().st_size,
             "modified": time.strftime("%Y-%m-%d %H:%M", time.localtime(p.stat().st_mtime))}
            for p in logs]})
        return OK
    if not logs:
        ctx.out.info("No log files found in the detected locations (repo root, .tools/, ~/.kaiteyo).")
        return OK
    ctx.out.banner("Detected log files")
    for path in logs:
        size = path.stat().st_size
        stamp = time.strftime("%Y-%m-%d %H:%M", time.localtime(path.stat().st_mtime))
        ctx.out.line(f"  {stamp}  {size:>10} B  {path}")
    return OK


def _cmd_latest(args: argparse.Namespace, ctx: Context) -> int:
    logs = detect_logs(ctx)
    if not logs:
        ctx.out.info("No log files found.")
        return OK
    path = args.file and next((p for p in logs if str(p).endswith(args.file)), None) or logs[0]
    _show_log(ctx, path, args.lines)
    return OK


def _cmd_follow(args: argparse.Namespace, ctx: Context) -> int:
    logs = detect_logs(ctx)
    if not logs:
        ctx.out.info("No log files found.")
        return OK
    path = args.file and next((p for p in logs if str(p).endswith(args.file)), None) or logs[0]
    ctx.out.info(f"Following {path} — Ctrl+C to stop.")
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            f.seek(0, 2)  # start at the end
            while True:
                line = f.readline()
                if line:
                    print(line, end="", flush=True)
                else:
                    time.sleep(0.3)
    except KeyboardInterrupt:
        return OK


def _cmd_search(args: argparse.Namespace, ctx: Context) -> int:
    logs = detect_logs(ctx)
    matches = []
    for path in logs:
        try:
            for lineno, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
                if args.query.lower() in line.lower():
                    matches.append((path, lineno, line.strip()[:150]))
                    if len(matches) >= args.max:
                        break
        except OSError:
            continue
        if len(matches) >= args.max:
            break
    ctx.out.banner(f"Log matches for '{args.query}' ({len(matches)})")
    for path, lineno, line in matches:
        ctx.out.line(f"  {path.name}:{lineno}  {line}")
    return OK


def _cmd_clear(args: argparse.Namespace, ctx: Context) -> int:
    logs = detect_logs(ctx)
    if not logs:
        ctx.out.info("No log files to clear.")
        return OK
    ctx.out.warn("This deletes detected log files (build logs, tool logs). Source code is never touched.")
    for path in logs:
        ctx.out.line(f"  {path}")
    if not ctx.confirm("Delete these log files?", default=False):
        raise CliError("Clear cancelled.", code=ABORT)
    for path in logs:
        try:
            path.unlink()
            ctx.out.ok(f"Deleted {path}")
        except OSError as exc:
            ctx.out.warn(f"Could not delete {path}: {exc}")
    return OK


def _cmd_export(args: argparse.Namespace, ctx: Context) -> int:
    logs = detect_logs(ctx)
    if not logs:
        ctx.out.info("No log files to export.")
        return OK
    destination = pathlib.Path(args.dest).resolve()
    destination.mkdir(parents=True, exist_ok=True)
    for path in logs:
        target = destination / path.name
        try:
            shutil.copy2(path, target)
            ctx.out.ok(f"Exported {path.name}")
        except OSError as exc:
            ctx.out.warn(f"Could not export {path}: {exc}")
    return OK


def _logs_menu(ctx: Context) -> int:
    while True:
        options = ["Latest logs", "Follow a log (tail)", "Search logs",
                   "Clear logs (confirmed)", "Export logs"]
        choice = ctx.menu("Log Viewer", options)
        if choice is None:
            return OK
        try:
            if choice == 0:
                _cmd_latest(argparse.Namespace(file=None, lines=40), ctx)
            elif choice == 1:
                _cmd_follow(argparse.Namespace(file=None), ctx)
            elif choice == 2:
                _cmd_search(argparse.Namespace(query=ctx.prompt("Search logs for", required=True), max=40), ctx)
            elif choice == 3:
                _cmd_clear(argparse.Namespace(), ctx)
            else:
                _cmd_export(argparse.Namespace(dest=ctx.prompt("Export to directory", required=True)), ctx)
        except CliError as exc:
            ctx.out.error(exc.message)
            if exc.hint:
                ctx.out.dim(f"  {exc.hint}")


def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    subs = sub.add_subparsers(dest="logs_command", metavar="COMMAND")

    def P(name: str, **kw: object):
        if common is not None:
            kw["parents"] = [common]
        return subs.add_parser(name, **kw)  # type: ignore[arg-type]

    p = P("list", help="List detected log files")
    p.set_defaults(handler=_cmd_list)

    p = P("latest", help="Show the tail of the newest (or given) log")
    p.add_argument("--file", help="log file name to show (default: newest)")
    p.add_argument("--lines", type=int, default=40)
    p.set_defaults(handler=_cmd_latest)

    p = P("follow", help="Tail a log (Ctrl+C to stop)")
    p.add_argument("--file", help="log file name to follow (default: newest)")
    p.set_defaults(handler=_cmd_follow)

    p = P("search", help="Search across detected logs")
    p.add_argument("query")
    p.add_argument("--max", type=int, default=40)
    p.set_defaults(handler=_cmd_search)

    p = P("clear", help="Delete detected logs (confirmed)")
    p.set_defaults(handler=_cmd_clear)

    p = P("export", help="Copy detected logs to a directory")
    p.add_argument("dest", help="destination directory")
    p.set_defaults(handler=_cmd_export)


command = Command(
    name="logs",
    aliases=["l"],
    help="View, follow, search, clear and export detected project/app logs",
    description=(
        "Detects log locations from the project configuration (repo root build logs,\n"
        ".tools/, ~/.kaiteyo) — nothing is invented. Clear always confirms.\n\n"
        "Examples:\n"
        "  kaiteyo logs list\n"
        "  kaiteyo logs latest --lines 60\n"
        "  kaiteyo logs follow\n"
        "  kaiteyo logs search error\n"
        "  kaiteyo logs export ./log-export\n"
        "  kaiteyo logs clear\n"
    ),
    build=build,
    run=lambda args, ctx: _dispatch(args, ctx),
    menu=_logs_menu,
    menu_label="Logs",
)


def _dispatch(args: argparse.Namespace, ctx: Context) -> int:
    handler = getattr(args, "handler", None)
    if handler:
        return handler(args, ctx)
    if ctx.interactive():
        return _logs_menu(ctx)
    raise CliError("Specify a logs subcommand.", code=2,
                   hint="Try: kaiteyo logs list | latest | follow | search QUERY | export DEST | clear")
