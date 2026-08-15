"""kaiteyo git — staged, safe Git workflows.

The primary workflow (Commit & Push) turns

    git status && git add . && git commit -m "..." && git push origin develop

into `kaiteyo git commit`. Selection, preview, commit title, and push are
separate, visible steps. Destructive operations (clean, force, resets) are
never performed silently — they are previewed and confirmed, or refused.
"""

from __future__ import annotations

import argparse
import re
from dataclasses import dataclass, field

from .. import history
from ..context import Context
from ..errors import CliError, ABORT, ENV, GIT_FAIL, OK
from ..registry import Command
from ..runner import run_capture, run_interactive, run_stream
from ..secrets import mask_remote, mask_text

GIT = "git"


# ---------------------------------------------------------------------------
# plumbing
# ---------------------------------------------------------------------------

# ``git status`` stats every tracked + untracked file. On network mounts
# (WSL/9p, network drives) that can take a minute or more — the CLI must not
# sit frozen with no output. We show progress and surface a clear timeout
# error instead of hanging silently.
GIT_STATUS_TIMEOUT = 180.0  # seconds — explicit git workflows may wait


def _run(ctx: Context, *args: str, cwd: str | None = None, check: bool = False,
         timeout: float | None = None):
    root = ctx.require_git()
    cmd = [GIT, "-C", str(root), *args]
    result = run_capture(cmd, cwd=cwd or str(root), timeout=timeout)
    if result.timed_out:
        raise CliError(
            f"git {' '.join(args)} timed out after {timeout or 'the default'}s",
            code=GIT_FAIL,
            hint="The working-tree scan could not finish. This usually means a slow or network-"
                 "backed filesystem (WSL/9p, network drive). Retry, or run git directly.",
        )
    if check and result.exit_code != 0:
        raise CliError(
            f"git {' '.join(args)} failed: {result.output.strip()}",
            code=GIT_FAIL,
            hint="See the git output above. Resolve the conflict and retry.",
        )
    return result


def _git_exists() -> bool:
    import shutil

    return shutil.which(GIT) is not None


def _require_git(ctx: Context) -> None:
    if not _git_exists():
        raise CliError("Git was not found on PATH.", code=ENV,
                       hint="Install git (https://git-scm.com) and retry.")


def _is_repo(ctx: Context) -> bool:
    return _run(ctx, "rev-parse", "--is-inside-work-tree").exit_code == 0


@dataclass
class StatusSnapshot:
    branch: str = ""
    upstream: str = ""
    ahead: int = 0
    behind: int = 0
    staged: list[str] = field(default_factory=list)     # paths
    unstaged: list[str] = field(default_factory=list)   # paths
    untracked: list[str] = field(default_factory=list)  # paths
    clean: bool = True
    has_upstream: bool = False
    gone: bool = False
    no_commits: bool = False

    @property
    def all_changed(self) -> list[str]:
        seen: list[str] = []
        for path in self.staged + self.unstaged + self.untracked:
            if path not in seen:
                seen.append(path)
        return seen

    def describe(self) -> dict:
        return {
            "branch": self.branch,
            "upstream": self.upstream,
            "ahead": self.ahead,
            "behind": self.behind,
            "has_upstream": self.has_upstream,
            "staged": self.staged,
            "unstaged": self.unstaged,
            "untracked": self.untracked,
            "clean": self.clean,
        }


def _snapshot(ctx: Context) -> StatusSnapshot:
    ctx.out.note("  scanning working tree (git status) — slow on network filesystems…")
    result = _run(ctx, "status", "--porcelain=v1", "-b", timeout=GIT_STATUS_TIMEOUT)
    if result.exit_code != 0:
        raise CliError(f"git status failed: {result.output.strip()}", code=GIT_FAIL)
    snap = StatusSnapshot()
    for raw_line in result.output.splitlines():
        if not raw_line.strip():
            continue
        if raw_line.startswith("## "):
            _parse_branch_line(snap, raw_line[3:])
            continue
        code, path = raw_line[:2], raw_line[3:]
        if " -> " in path:
            path = path.split(" -> ", 1)[1]
        if code == "??":
            snap.untracked.append(path)
        else:
            if code[0] != " " and code[0] != "?":
                snap.staged.append(path)
            if code[1] != " " and code[1] != "?":
                snap.unstaged.append(path)
    snap.clean = not (snap.staged or snap.unstaged or snap.untracked)
    return snap


_BRANCH_RE = re.compile(
    r"^(?P<branch>.+?)(?:\.\.\.(?P<upstream>[^\s]+)(?: \[(?P<info>[^\]]+)\])?)?$"
)


def _parse_branch_line(snap: StatusSnapshot, line: str) -> None:
    if line.startswith("No commits yet on "):
        snap.branch = line.split("on ", 1)[1]
        snap.no_commits = True
        return
    match = _BRANCH_RE.match(line)
    if not match:
        snap.branch = line
        return
    snap.branch = match.group("branch")
    snap.upstream = match.group("upstream") or ""
    snap.has_upstream = bool(snap.upstream)
    info = match.group("info") or ""
    if "gone" in info:
        snap.gone = True
    for kind, value in re.findall(r"(ahead|behind) (\d+)", info):
        if kind == "ahead":
            snap.ahead = int(value)
        else:
            snap.behind = int(value)


def _show_snapshot(ctx: Context, snap: StatusSnapshot) -> None:
    out = ctx.out
    out.banner("Git Status")
    out.info(f"Branch:      {snap.branch}")
    if snap.has_upstream:
        out.info(f"Upstream:    {snap.upstream}")
        out.info(f"Ahead/behind: {snap.ahead} ahead, {snap.behind} behind")
        if snap.gone:
            out.warn("Upstream branch has been deleted (gone).")
    else:
        out.warn("No upstream branch configured.")
    out.line()
    if snap.clean:
        out.ok("Working tree is clean — nothing to commit.")
        return
    if snap.staged:
        out.section("Staged")
        for path in snap.staged:
            out.line(f"  {path}")
    if snap.unstaged:
        out.section("Modified (unstaged)")
        for path in snap.unstaged:
            out.line(f"  {path}")
    if snap.untracked:
        out.section("Untracked")
        for path in snap.untracked:
            out.line(f"  {path}")


def _remote(ctx: Context) -> str:
    result = _run(ctx, "remote", "get-url", ctx.cfg.get("default_remote", "origin"))
    return result.output.strip() if result.exit_code == 0 else ""


# ---------------------------------------------------------------------------
# status
# ---------------------------------------------------------------------------

def _cmd_status(args: argparse.Namespace, ctx: Context) -> int:
    _require_git(ctx)
    if not _is_repo(ctx):
        raise CliError("Not inside a git repository.", code=ENV,
                       hint="Run from inside the Kaiteyo repository, or pass --root.")
    snap = _snapshot(ctx)
    if ctx.json_mode:
        ctx.out.json(snap.describe())
        return OK
    _show_snapshot(ctx, snap)
    return OK


# ---------------------------------------------------------------------------
# commit & push (the primary workflow)
# ---------------------------------------------------------------------------

def _select_paths(ctx: Context, snap: StatusSnapshot, args: argparse.Namespace) -> list[str]:
    """Decide what to stage: flags first, then an interactive menu."""
    if getattr(args, "all", False):
        return snap.all_changed
    if getattr(args, "modified", False):
        return snap.staged + snap.unstaged
    if getattr(args, "untracked", False):
        return snap.untracked
    files = getattr(args, "files", None)
    if files:
        requested = [p.strip() for p in files.split(",") if p.strip()]
        return [p for p in requested if p in snap.all_changed] or requested
    dirs_arg = getattr(args, "dirs", None)
    if dirs_arg:
        requested = [d.strip("/") for d in dirs_arg.split(",") if d.strip()]
        return [p for p in snap.all_changed
                if any(p.startswith(d + "/") or p == d for d in requested)]

    # Interactive selection.
    options = ["All changes", "Modified files only", "Untracked files only",
               "Choose files individually", "Choose directories"]
    choice = ctx.menu("What should be included in the commit?", options)
    if choice is None:
        raise CliError("Nothing to commit.", code=ABORT)
    if choice == 0:
        return snap.all_changed
    if choice == 1:
        return snap.staged + snap.unstaged
    if choice == 2:
        return snap.untracked
    if choice == 3:
        items = [(p, _short_code(snap, p)) for p in snap.all_changed]
        return [snap.all_changed[i] for i in ctx.multiselect("Select files to stage", items)]
    if choice == 4:
        dirs = _changed_dirs(snap)
        items = [(d, f"{len(_paths_in(snap, d))} changed file(s)") for d in dirs]
        picked = [dirs[i] for i in ctx.multiselect("Select directories", items)]
        return [p for d in picked for p in _paths_in(snap, d)]
    return []


def _short_code(snap: StatusSnapshot, path: str) -> str:
    if path in snap.untracked:
        return "untracked"
    if path in snap.staged and path in snap.unstaged:
        return "staged+modified"
    if path in snap.staged:
        return "staged"
    return "modified"


def _changed_dirs(snap: StatusSnapshot) -> list[str]:
    dirs: list[str] = []
    for path in snap.all_changed:
        parts = path.split("/")
        if len(parts) > 1:
            d = parts[0]
        else:
            d = "."
        if d not in dirs:
            dirs.append(d)
    return dirs


def _paths_in(snap: StatusSnapshot, directory: str) -> list[str]:
    if directory == ".":
        return [p for p in snap.all_changed if "/" not in p]
    return [p for p in snap.all_changed if p.startswith(directory + "/")]


def _stage(ctx: Context, paths: list[str]) -> None:
    if not paths:
        return
    result = _run(ctx, "add", "--", *paths)
    if result.exit_code != 0:
        raise CliError(f"git add failed: {result.output.strip()}", code=GIT_FAIL)


def _commit_title(args: argparse.Namespace, ctx: Context) -> str:
    title = getattr(args, "title", None)
    if title:
        return title.strip()
    if not ctx.interactive():
        raise CliError(
            "A commit title is required.",
            code=ABORT,
            hint="Pass --title \"Your commit message\" in non-interactive mode.",
        )
    return ctx.prompt("Commit title", required=True)


def _commit_message(args: argparse.Namespace, ctx: Context) -> str:
    message = getattr(args, "message", None)
    if message:
        return message
    if getattr(args, "title", None) and not ctx.interactive():
        return ""
    return ctx.read_multiline("Commit description (optional)")


def _do_commit(ctx: Context, title: str, message: str) -> None:
    cmd = [GIT, "-C", str(ctx.require_git()), "commit", "-m", title]
    if message:
        cmd += ["-m", message]
    result = run_stream(cmd)
    if result.exit_code != 0:
        raise CliError(
            f"Commit failed (exit {result.exit_code}).",
            code=GIT_FAIL,
            hint="Check for merge conflicts or pre-commit hooks. Nothing was pushed.",
        )


def _push(ctx: Context, snap: StatusSnapshot, args: argparse.Namespace) -> int:
    """Push with full visibility: branch, upstream, ahead/behind, remote."""
    root = ctx.require_git()
    out = ctx.out
    remote = ctx.cfg.get("default_remote", "origin")
    remote_url = _remote(ctx)
    out.section("Push preview")
    out.info(f"Current branch: {snap.branch}")
    out.info(f"Remote:         {remote}" + (f"  ({mask_remote(remote_url)})" if remote_url else ""))
    if not snap.has_upstream:
        out.warn(f"No upstream configured for {snap.branch}.")
        if not ctx.confirm(f"Push and set upstream (git push -u {remote} {snap.branch})?", default=True):
            out.info("Push skipped.")
            return OK
        result = run_stream([GIT, "-C", str(root), "push", "-u", remote, snap.branch])
    else:
        out.info(f"Upstream:       {snap.upstream}")
        if snap.ahead > 0 or snap.behind > 0:
            out.info(f"Ahead/behind:   {snap.ahead} ahead, {snap.behind} behind")
        if not ctx.confirm("Push to remote?", default=True):
            out.info("Push skipped.")
            return OK
        result = run_stream([GIT, "-C", str(root), "push"])
    if result.exit_code != 0:
        raise CliError(
            f"Push failed (exit {result.exit_code}).",
            code=GIT_FAIL,
            hint="Check the remote URL (masked above) and your credentials. Nothing was force-pushed.",
        )
    return OK


def _cmd_commit(args: argparse.Namespace, ctx: Context) -> int:
    _require_git(ctx)
    if not _is_repo(ctx):
        raise CliError("Not inside a git repository.", code=ENV,
                       hint="Run from inside the Kaiteyo repository, or pass --root.")
    snap = _snapshot(ctx)
    if snap.clean:
        ctx.out.ok("Working tree is clean — nothing to commit. (No empty commits are created.)")
        return OK

    if not ctx.json_mode:
        _show_snapshot(ctx, snap)

    paths = _select_paths(ctx, snap, args)
    if not paths:
        raise CliError("Nothing selected to commit.", code=ABORT)
    _stage(ctx, paths)

    staged = _snapshot(ctx)
    if not staged.staged:
        raise CliError("Nothing staged after selection.", code=ABORT)
    ctx.out.section("Will be committed")
    for path in staged.staged:
        ctx.out.line(f"  {path}")

    if not ctx.confirm("Proceed with the commit?", default=True):
        raise CliError("Commit cancelled.", code=ABORT)

    title = _commit_title(args, ctx)
    message = _commit_message(args, ctx)
    _do_commit(ctx, title, message)
    ctx.out.ok(f"Committed: {title}")

    history.add(["git", "commit"])

    want_push = True if getattr(args, "push", False) else (False if getattr(args, "no_push", False) else None)
    if want_push is None:
        want_push = ctx.confirm("Push now?", default=True) if ctx.interactive() else False
    if want_push:
        return _push(ctx, _snapshot(ctx), args)
    return OK


# ---------------------------------------------------------------------------
# push / sync / log / diff / branches / clean
# ---------------------------------------------------------------------------

def _cmd_push(args: argparse.Namespace, ctx: Context) -> int:
    _require_git(ctx)
    snap = _snapshot(ctx)
    if ctx.json_mode:
        ctx.out.json(snap.describe())
        return OK
    _show_snapshot(ctx, snap)
    if snap.clean and not snap.ahead:
        ctx.out.info("Nothing to push (clean and up to date).")
        return OK
    return _push(ctx, snap, args)


def _cmd_sync(args: argparse.Namespace, ctx: Context) -> int:
    _require_git(ctx)
    root = ctx.require_git()
    remote = ctx.cfg.get("default_remote", "origin")
    out = ctx.out
    out.banner("Sync")
    result = run_stream([GIT, "-C", str(root), "fetch", remote])
    if result.exit_code != 0:
        raise CliError(f"git fetch failed (exit {result.exit_code}).", code=GIT_FAIL,
                       hint="Check your network and remote configuration.")
    snap = _snapshot(ctx)
    out.info(f"Branch: {snap.branch}  |  ahead {snap.ahead}  |  behind {snap.behind}")
    plan: list[list[str]] = []
    if not snap.has_upstream:
        out.warn(f"No upstream for {snap.branch} — cannot sync.")
        return OK
    if snap.behind > 0:
        plan.append([GIT, "-C", str(root), "pull", "--ff-only"])
    if snap.ahead > 0:
        plan.append([GIT, "-C", str(root), "push"])
    if not plan:
        out.ok("Already up to date.")
        return OK
    for cmd in plan:
        out.info("Will run: " + mask_text(" ".join(cmd)))
    if ctx.confirm("Run sync plan?", default=True):
        for cmd in plan:
            result = run_stream(cmd)
            if result.exit_code != 0:
                raise CliError(f"Sync step failed (exit {result.exit_code}).", code=GIT_FAIL,
                               hint="See the output above; resolve and retry.")
        out.ok("Sync complete.")
    else:
        raise CliError("Sync cancelled.", code=ABORT)
    return OK


def _cmd_log(args: argparse.Namespace, ctx: Context) -> int:
    _require_git(ctx)
    count = getattr(args, "count", None) or 20
    result = run_interactive([GIT, "-C", str(ctx.require_git()),
                              "log", "--oneline", "--decorate", "--graph", f"-{count}"])
    return OK if result.exit_code == 0 else GIT_FAIL


def _cmd_diff(args: argparse.Namespace, ctx: Context) -> int:
    _require_git(ctx)
    base = [GIT, "-C", str(ctx.require_git()), "diff"]
    if getattr(args, "staged", False):
        base.append("--cached")
    ctx.out.note("  computing diff --stat — slow on network filesystems…")
    stat = run_capture(base + ["--stat"], timeout=GIT_STATUS_TIMEOUT)
    if stat.timed_out:
        raise CliError(
            "git diff --stat timed out while scanning the working tree.",
            code=GIT_FAIL,
            hint="Slow/network filesystem (WSL/9p, network drive). Retry or run git directly.",
        )
    ctx.out.line(mask_text(stat.output.strip()))
    if getattr(args, "full", False):
        result = run_interactive(base)
        return OK if result.exit_code == 0 else GIT_FAIL
    if ctx.interactive() and stat.output.strip() and ctx.confirm("Show full diff?", default=False):
        result = run_interactive(base)
        return OK if result.exit_code == 0 else GIT_FAIL
    return OK


def _cmd_branches(args: argparse.Namespace, ctx: Context) -> int:
    _require_git(ctx)
    result = run_interactive([GIT, "-C", str(ctx.require_git()), "branch", "-a", "-vv"])
    return OK if result.exit_code == 0 else GIT_FAIL


def _cmd_clean(args: argparse.Namespace, ctx: Context) -> int:
    _require_git(ctx)
    root = ctx.require_git()
    out = ctx.out
    out.warn("This removes untracked files from the working tree.")
    dry = run_capture([GIT, "-C", str(root), "clean", "-n", "-d"])
    if not dry.output.strip():
        out.ok("Nothing to clean.")
        return OK
    out.line(mask_text(dry.output.strip()))
    if not ctx.confirm("Delete these untracked files and directories? This cannot be undone.", default=False):
        raise CliError("Clean cancelled.", code=ABORT)
    result = run_stream([GIT, "-C", str(root), "clean", "-f", "-d"])
    return OK if result.exit_code == 0 else GIT_FAIL


# ---------------------------------------------------------------------------
# interactive center
# ---------------------------------------------------------------------------

def _git_menu(ctx: Context) -> int:
    while True:
        choice = ctx.menu("Git Command Center", [
            "Status",
            "Commit & Push",
            "Diff",
            "Log",
            "Branches",
            "Sync",
            "Clean (untracked files)",
        ])
        if choice is None:
            return OK
        try:
            if choice == 0:
                _cmd_status(argparse.Namespace(), ctx)
            elif choice == 1:
                _cmd_commit(argparse.Namespace(), ctx)
            elif choice == 2:
                _cmd_diff(argparse.Namespace(), ctx)
            elif choice == 3:
                _cmd_log(argparse.Namespace(), ctx)
            elif choice == 4:
                _cmd_branches(argparse.Namespace(), ctx)
            elif choice == 5:
                _cmd_sync(argparse.Namespace(), ctx)
            elif choice == 6:
                _cmd_clean(argparse.Namespace(), ctx)
        except CliError as exc:
            ctx.out.error(exc.message)
            if exc.hint:
                ctx.out.dim(f"  {exc.hint}")


# ---------------------------------------------------------------------------
# parser
# ---------------------------------------------------------------------------

def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    subs = sub.add_subparsers(dest="git_command", metavar="COMMAND")

    def P(name: str, **kw: object):
        if common is not None:
            kw["parents"] = [common]
        return subs.add_parser(name, **kw)  # type: ignore[arg-type]

    p = P("status", help="Show branch, upstream and changed files")
    p.set_defaults(handler=_cmd_status)

    p = P("commit", help="Stage, preview, commit and push (primary workflow)")
    p.add_argument("--all", action="store_true", help="stage all changes")
    p.add_argument("--modified", action="store_true", help="stage modified files only")
    p.add_argument("--untracked", action="store_true", help="stage untracked files only")
    p.add_argument("--files", metavar="LIST", help="comma-separated paths to stage")
    p.add_argument("--dirs", metavar="LIST", help="comma-separated directories to stage")
    p.add_argument("--title", metavar="TITLE", help="commit title (required in scripts)")
    p.add_argument("--message", metavar="TEXT", help="commit description")
    p.add_argument("--push", action="store_true", help="push after committing")
    p.add_argument("--no-push", action="store_true", help="do not push after committing")
    p.set_defaults(handler=_cmd_commit)

    p = P("push", help="Show branch info, then push (never force)")
    p.set_defaults(handler=_cmd_push)

    p = P("sync", help="Fetch, fast-forward pull and push")
    p.set_defaults(handler=_cmd_sync)

    p = P("log", help="Show recent commit history")
    p.add_argument("--count", type=int, default=20, help="number of commits (default 20)")
    p.set_defaults(handler=_cmd_log)

    p = P("diff", help="Show working-tree changes")
    p.add_argument("--staged", action="store_true", help="show staged changes")
    p.add_argument("--full", action="store_true", help="show the full diff (no prompt)")
    p.set_defaults(handler=_cmd_diff)

    p = P("branches", help="List local and remote branches")
    p.set_defaults(handler=_cmd_branches)

    p = P("clean", help="Remove untracked files (previewed, confirmed)")
    p.set_defaults(handler=_cmd_clean)


command = Command(
    name="git",
    aliases=["g"],
    help="Staged, safe Git workflows (status, commit & push, sync, log, diff)",
    description=(
        "Guided Git workflows with full visibility: status → select → preview → commit → push.\n\n"
        "Examples:\n"
        "  kaiteyo git                       interactive Git command center\n"
        "  kaiteyo git commit                guided commit & push\n"
        "  kaiteyo git commit --all --title \"Fix library\" --push\n"
        "  kaiteyo git status --json         machine-readable status\n"
        "  kaiteyo git sync\n"
        "  kaiteyo git log --count 10\n"
        "  kaiteyo git diff --staged\n"
        "  kaiteyo git branches\n"
    ),
    build=build,
    run=lambda args, ctx: _dispatch(args, ctx),
    menu=_git_menu,
    menu_label="Git",
)


def _dispatch(args: argparse.Namespace, ctx: Context) -> int:
    handler = getattr(args, "handler", None)
    if handler is None:
        # No subcommand: show the git center (or help when non-interactive).
        if ctx.interactive():
            return _git_menu(ctx)
        raise CliError("Specify a git subcommand.", code=2,
                       hint="Try: kaiteyo git status | commit | push | sync | log | diff | branches | clean")
    return handler(args, ctx)
