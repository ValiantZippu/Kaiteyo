"""kaiteyo files — developer file utilities.

Convenience operations (tree, find, text search, large files, recent files,
duplicate detection, open) — deliberately not a full file manager.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import pathlib
import time

from .. import platform
from ..context import Context
from ..errors import CliError, OK, USAGE
from ..registry import Command
from ..runner import run_interactive

SKIP_DIRS = {".git", ".gradle", ".kotlin", "build", "node_modules", "dist",
             "__pycache__", ".idea", ".vscode", ".tools", "scratch", ".freebuff"}

HUMAN = [
    (1024 ** 4, "TiB"), (1024 ** 3, "GiB"), (1024 ** 2, "MiB"), (1024, "KiB"),
]


def human_size(num: int) -> str:
    for factor, suffix in HUMAN:
        if num >= factor:
            return f"{num / factor:.1f} {suffix}"
    return f"{num} B"


def _walk(root: pathlib.Path, skip: set[str] | None = None) -> list[pathlib.Path]:
    skip = SKIP_DIRS if skip is None else skip
    result: list[pathlib.Path] = []
    for dirpath, dirnames, filenames in os.walk(root):
        rel = pathlib.Path(dirpath).relative_to(root)
        dirnames[:] = sorted(d for d in dirnames if d not in skip)
        for name in sorted(filenames):
            result.append(pathlib.Path(dirpath) / name)
    return result


def _cmd_tree(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.root or pathlib.Path.cwd()
    depth = args.depth
    for dirpath, dirnames, filenames in os.walk(root):
        rel = pathlib.Path(dirpath).relative_to(root)
        if len(rel.parts) > depth:
            dirnames[:] = []
            continue
        dirnames[:] = sorted(d for d in dirnames if d not in SKIP_DIRS)
        indent = "  " * len(rel.parts)
        ctx.out.line(f"{indent}{rel.name}/" if rel.parts else f"{indent}{root.name}/")
        for name in sorted(filenames)[:args.max]:
            ctx.out.line(f"{indent}  {name}")
    return OK


def _cmd_find(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.root or pathlib.Path.cwd()
    needle = args.pattern.lower()
    matches = [p for p in _walk(root) if needle in p.name.lower()]
    if args.json:
        ctx.out.json({"pattern": args.pattern, "matches": [str(p) for p in matches]})
        return OK
    for path in matches:
        ctx.out.line(f"  {path.relative_to(root).as_posix()}")
    ctx.out.info(f"{len(matches)} match(es).")
    return OK


def _cmd_search(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.root or pathlib.Path.cwd()
    rg = platform.which("rg")
    if rg:
        result = run_interactive([rg, "-n", "--hidden", "--glob", "!.git/**",
                                  "--glob", "!build/**", "--glob", "!.gradle/**",
                                  args.query, str(root)], echo=False)
        return OK if result.exit_code in (0, 1) else OK
    # Python fallback (no ripgrep).
    matches = []
    for path in _walk(root):
        if any(part.startswith(".") for part in path.parts):
            continue
        try:
            for lineno, line in enumerate(path.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
                if args.query.lower() in line.lower():
                    matches.append((path, lineno, line.strip()[:140]))
                    if len(matches) >= args.max:
                        break
        except OSError:
            continue
        if len(matches) >= args.max:
            break
    for path, lineno, line in matches:
        ctx.out.line(f"  {path.relative_to(root).as_posix()}:{lineno}  {line}")
    ctx.out.info(f"{len(matches)} match(es) (python fallback — install ripgrep for speed).")
    return OK


def _cmd_large(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.root or pathlib.Path.cwd()
    sizes = [(p, p.stat().st_size) for p in _walk(root) if p.is_file()]
    sizes.sort(key=lambda item: item[1], reverse=True)
    top = sizes[: args.top]
    if args.json:
        ctx.out.json({"large": [{"path": str(p), "bytes": size} for p, size in top]})
        return OK
    ctx.out.banner(f"Largest files in {root.name}/ (top {len(top)})")
    for path, size in top:
        ctx.out.line(f"  {human_size(size):>9}  {path.relative_to(root).as_posix()}")
    return OK


def _cmd_recent(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.root or pathlib.Path.cwd()
    cutoff = time.time() - args.days * 86400
    recent = []
    for path in _walk(root):
        try:
            mtime = path.stat().st_mtime
        except OSError:
            continue
        if mtime >= cutoff:
            recent.append((path, mtime))
    recent.sort(key=lambda item: item[1], reverse=True)
    recent = recent[: args.top]
    for path, mtime in recent:
        stamp = time.strftime("%Y-%m-%d %H:%M", time.localtime(mtime))
        ctx.out.line(f"  {stamp}  {path.relative_to(root).as_posix()}")
    return OK


def _cmd_dupes(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.root or pathlib.Path.cwd()
    min_size = args.min_mb * 1024 * 1024
    by_size: dict[int, list[pathlib.Path]] = {}
    for path in _walk(root):
        try:
            size = path.stat().st_size
        except OSError:
            continue
        if size >= min_size:
            by_size.setdefault(size, []).append(path)
    groups = []
    for size, paths in by_size.items():
        if len(paths) < 2:
            continue
        hashes: dict[str, list[pathlib.Path]] = {}
        for path in paths:
            digest = hashlib.md5()
            with open(path, "rb") as f:
                for chunk in iter(lambda: f.read(65536), b""):
                    digest.update(chunk)
            hashes.setdefault(digest.hexdigest(), []).append(path)
        for digest, dupes in hashes.items():
            if len(dupes) > 1:
                groups.append((size, dupes))
    groups.sort(key=lambda g: g[0], reverse=True)
    if args.json:
        ctx.out.json({"duplicate_groups": [
            {"bytes": size, "files": [str(p) for p in dupes]} for size, dupes in groups]})
        return OK
    ctx.out.banner(f"Duplicate files (>= {args.min_mb} MB, md5)")
    for size, dupes in groups:
        ctx.out.line(f"  {human_size(size)}")
        for path in dupes:
            ctx.out.line(f"    {path.relative_to(root).as_posix()}")
    if not groups:
        ctx.out.info("No duplicates found.")
    return OK


def _cmd_open(args: argparse.Namespace, ctx: Context) -> int:
    target = pathlib.Path(args.path or ".").resolve()
    if not target.exists():
        raise CliError(f"Path does not exist: {target}", code=USAGE)
    opener = platform.opener_cmd(str(target))
    if not opener:
        raise CliError("No default opener found for this platform.", code=USAGE)
    result = run_interactive(opener, echo=False)
    return OK if result.exit_code == 0 else OK


def _cmd_root(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.require_root("The files command")
    if args.json:
        ctx.out.json({"root": str(root)})
        return OK
    ctx.out.line(str(root))
    return OK


def _files_menu(ctx: Context) -> int:
    while True:
        options = ["Project tree", "Find by name", "Search text", "Large files",
                   "Recently modified", "Duplicates", "Open project root",
                   "Open current directory"]
        choice = ctx.menu("File Utilities", options)
        if choice is None:
            return OK
        try:
            if choice == 0:
                _cmd_tree(argparse.Namespace(depth=2, max=40), ctx)
            elif choice == 1:
                _cmd_find(argparse.Namespace(pattern=ctx.prompt("Find files matching", required=True), json=False), ctx)
            elif choice == 2:
                _cmd_search(argparse.Namespace(query=ctx.prompt("Search text for", required=True), max=40), ctx)
            elif choice == 3:
                _cmd_large(argparse.Namespace(top=15, json=False), ctx)
            elif choice == 4:
                _cmd_recent(argparse.Namespace(days=7, top=30), ctx)
            elif choice == 5:
                _cmd_dupes(argparse.Namespace(min_mb=1, json=False), ctx)
            elif choice == 6:
                _cmd_open(argparse.Namespace(path="."), ctx)
            else:
                _cmd_open(argparse.Namespace(path=os.getcwd()), ctx)
        except CliError as exc:
            ctx.out.error(exc.message)
            if exc.hint:
                ctx.out.dim(f"  {exc.hint}")


def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    subs = sub.add_subparsers(dest="files_command", metavar="COMMAND")

    def P(name: str, **kw: object):
        if common is not None:
            kw["parents"] = [common]
        return subs.add_parser(name, **kw)  # type: ignore[arg-type]

    p = P("tree", help="Show the project tree")
    p.add_argument("--depth", type=int, default=2)
    p.add_argument("--max", type=int, default=40)
    p.set_defaults(handler=_cmd_tree)

    p = P("find", help="Find files by name")
    p.add_argument("pattern")
    p.set_defaults(handler=_cmd_find)

    p = P("search", help="Search file contents")
    p.add_argument("query")
    p.add_argument("--max", type=int, default=40)
    p.set_defaults(handler=_cmd_search)

    p = P("large", help="Show the largest files")
    p.add_argument("--top", type=int, default=15)
    p.set_defaults(handler=_cmd_large)

    p = P("recent", help="Show recently modified files")
    p.add_argument("--days", type=int, default=7)
    p.add_argument("--top", type=int, default=30)
    p.set_defaults(handler=_cmd_recent)

    p = P("dupes", help="Detect duplicate files (by size + md5)")
    p.add_argument("--min-mb", type=float, default=1.0)
    p.set_defaults(handler=_cmd_dupes)

    p = P("open", help="Open a path with the default application")
    p.add_argument("path", nargs="?", default=".")
    p.set_defaults(handler=_cmd_open)

    p = P("root", help="Print the project root")
    p.set_defaults(handler=_cmd_root)


command = Command(
    name="files",
    aliases=["f"],
    help="File utilities: tree, find, search text, large/recent files, duplicates",
    description=(
        "Developer file convenience: project tree, find by name, text search\n"
        "(ripgrep when available), largest files, recently modified, duplicate\n"
        "detection, and opening paths.\n\n"
        "Examples:\n"
        "  kaiteyo files tree --depth 2\n"
        "  kaiteyo files find ViewModel\n"
        "  kaiteyo files search sqlDelight\n"
        "  kaiteyo files large --top 10 --json\n"
        "  kaiteyo files recent --days 3\n"
        "  kaiteyo files dupe\n"
        "  kaiteyo files open docs\n"
    ),
    build=build,
    run=lambda args, ctx: _dispatch(args, ctx),
    menu=_files_menu,
    menu_label="Files",
)


def _dispatch(args: argparse.Namespace, ctx: Context) -> int:
    handler = getattr(args, "handler", None)
    if handler:
        return handler(args, ctx)
    if ctx.interactive():
        return _files_menu(ctx)
    raise CliError("Specify a files subcommand.", code=2,
                   hint="Try: kaiteyo files tree | find | search | large | recent | dupes | open | root")
