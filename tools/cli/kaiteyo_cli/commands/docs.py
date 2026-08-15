"""kaiteyo docs — documentation discovery.

Browses the repository's docs/ tree without duplicating any documentation:
it reads the structure and file contents directly. No docs are generated or
copied here.
"""

from __future__ import annotations

import argparse
import pathlib

from .. import platform
from ..context import Context
from ..errors import CliError, ENV, OK, USAGE
from ..registry import Command
from ..runner import run_interactive

README_NAMES = {"readme.md", "readme"}


def _docs_dir(ctx: Context) -> pathlib.Path:
    root = ctx.require_root("The docs command")
    docs = root / "docs"
    if not docs.is_dir():
        raise CliError("No docs/ directory in this repository.", code=ENV)
    return docs


def _categories(docs: pathlib.Path) -> list[tuple[str, list[pathlib.Path]]]:
    result = []
    for directory in sorted(d for d in docs.iterdir() if d.is_dir()):
        files = sorted(
            f for f in directory.rglob("*.md")
            if not any(part.startswith(".") for part in f.parts)
        )
        if files:
            result.append((directory.name, files))
    return result


def _print_tree(ctx: Context, depth: int = 2) -> None:
    docs = _docs_dir(ctx)
    ctx.out.banner(f"Documentation tree — docs/ (depth {depth})")
    for directory in sorted(d for d in docs.iterdir() if d.is_dir()):
        ctx.out.line(ctx.out.paint(directory.name + "/", "\x1b[1m"))
        for child in sorted(directory.iterdir()):
            if child.name.lower() in README_NAMES:
                continue
            rel = child.relative_to(docs)
            rel_depth = len(rel.parts)
            if child.is_file() and child.suffix in (".md", ".txt") and rel_depth <= depth:
                ctx.out.line(f"  {rel.as_posix()}")
            elif child.is_dir() and rel_depth < depth:
                ctx.out.line(f"  {rel.as_posix()}/")


def _open_doc(ctx: Context, rel: str) -> None:
    docs = _docs_dir(ctx)
    target = (docs / rel).resolve()
    try:
        target.relative_to(docs.resolve())
    except ValueError:
        raise CliError(f"Path is outside docs/: {rel}", code=USAGE,
                       hint="Only files under docs/ can be opened.")
    if not target.is_file():
        raise CliError(f"Not a file: {rel}", code=USAGE,
                       hint=f"Try `kaiteyo docs tree` to see available files.")
    opener = platform.opener_cmd(str(target))
    if opener and ctx.confirm(f"Open {rel} with the default application?", default=True):
        run_interactive(opener, echo=False)
    else:
        ctx.out.line(f"  {target}")


def _search_docs(ctx: Context, query: str, limit: int = 40) -> None:
    docs = _docs_dir(ctx)
    matches = []
    for path in docs.rglob("*.md"):
        if any(part.startswith(".") for part in path.parts):
            continue
        try:
            for lineno, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
                if query.lower() in line.lower():
                    rel = path.relative_to(docs).as_posix()
                    matches.append((rel, lineno, line.strip()[:140]))
                    if len(matches) >= limit:
                        break
        except OSError:
            continue
        if len(matches) >= limit:
            break
    ctx.out.banner(f"Docs matching '{query}' ({len(matches)})")
    for rel, lineno, line in matches:
        ctx.out.line(f"  {rel}:{lineno}  {line}")


def _cmd_tree(args: argparse.Namespace, ctx: Context) -> int:
    _print_tree(ctx, depth=args.depth)
    return OK


def _cmd_search(args: argparse.Namespace, ctx: Context) -> int:
    _search_docs(ctx, args.query)
    return OK


def _cmd_open(args: argparse.Namespace, ctx: Context) -> int:
    _open_doc(ctx, args.path)
    return OK


def _cmd_topics(args: argparse.Namespace, ctx: Context) -> int:
    docs = _docs_dir(ctx)
    if ctx.json_mode:
        ctx.out.json({
            "categories": [
                {"name": name, "files": [str(f.relative_to(docs)) for f in files]}
                for name, files in _categories(docs)
            ]
        })
        return OK
    ctx.out.banner("Documentation topics")
    for name, files in _categories(docs):
        ctx.out.line(ctx.out.paint(f"{name}/", "\x1b[1m"))
        for file in files:
            ctx.out.line(f"  {file.relative_to(docs).as_posix()}")
    return OK


def _docs_menu(ctx: Context) -> int:
    docs = _docs_dir(ctx)
    while True:
        categories = _categories(docs)
        options = [f"{name}/" for name, _ in categories] + ["Search docs", "Show tree"]
        choice = ctx.menu("Documentation", options)
        if choice is None:
            return OK
        if choice < len(categories):
            name, files = categories[choice]
            file_options = [f.relative_to(docs).as_posix() for f in files]
            file_choice = ctx.menu(f"docs/{name}/", file_options)
            if file_choice is None:
                continue
            _open_doc(ctx, file_options[file_choice])
        elif choice == len(categories):
            query = ctx.prompt("Search docs for", required=True)
            _search_docs(ctx, query)
        else:
            _print_tree(ctx)


def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    subs = sub.add_subparsers(dest="docs_command", metavar="COMMAND")

    def P(name: str, **kw: object):
        if common is not None:
            kw["parents"] = [common]
        return subs.add_parser(name, **kw)  # type: ignore[arg-type]

    p = P("tree", help="Show the docs/ tree")
    p.add_argument("--depth", type=int, default=2)
    p.set_defaults(handler=_cmd_tree)
    p = P("search", help="Search documentation text")
    p.add_argument("query")
    p.set_defaults(handler=_cmd_search)
    p = P("open", help="Open a doc file (default application)")
    p.add_argument("path", help="path relative to docs/, e.g. development/COMMANDS.md")
    p.set_defaults(handler=_cmd_open)
    p = P("topics", help="List documentation categories and files")
    p.set_defaults(handler=_cmd_topics)


command = Command(
    name="docs",
    aliases=["doc"],
    help="Browse repository documentation: topics, tree, search, open",
    description=(
        "Discover the repository documentation without duplicating it: list topics,\n"
        "show the tree, search text, and open files.\n\n"
        "Examples:\n"
        "  kaiteyo docs                interactive topics browser\n"
        "  kaiteyo docs topics\n"
        "  kaiteyo docs tree\n"
        "  kaiteyo docs search release\n"
        "  kaiteyo docs open development/COMMANDS.md\n"
    ),
    build=build,
    run=lambda args, ctx: _dispatch(args, ctx),
    menu=_docs_menu,
    menu_label="Documentation",
)


def _dispatch(args: argparse.Namespace, ctx: Context) -> int:
    handler = getattr(args, "handler", None)
    if handler:
        return handler(args, ctx)
    if ctx.interactive():
        return _docs_menu(ctx)
    raise CliError("Specify a docs subcommand.", code=2,
                   hint="Try: kaiteyo docs topics | tree | search QUERY | open PATH")
