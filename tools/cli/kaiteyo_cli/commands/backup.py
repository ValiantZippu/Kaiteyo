"""kaiteyo backup / kaiteyo sync — rsync-based transfers with dry-run first.

Backup copies the repository (excluding build outputs, .git, local config)
to a destination. Sync mirrors a source into a destination.

Safety rules:

  - `--dry-run` is always performed and shown before the real run,
  - destination files are NEVER deleted unless the user passes --delete
    AND confirms,
  - the summary reports added / modified / removed / skipped files and
    bytes transferred.

rsync is required (Linux/macOS natively, Windows via WSL).
"""

from __future__ import annotations

import argparse
import pathlib
import re
import shutil

from .. import platform
from ..context import Context
from ..errors import CliError, ABORT, ENV, OK, TRANSFER_FAIL, USAGE
from ..registry import Command
from ..runner import RunResult, run_capture, run_stream

EXCLUDES = [
    ".git", "build", ".gradle", ".kotlin", "local.properties", "scratch",
    ".freebuff", ".tools", "__pycache__", "*.pyc", ".idea", ".vscode",
    "website/dist",
]

_ADDED = re.compile(r"^>f\+\+\+\+\+\+\+\+\+")
_MODIFIED = re.compile(r"^>f")
_DELETED = re.compile(r"^\*deleting")


def _rsync(ctx: Context) -> str:
    exe = shutil.which("rsync")
    if not exe:
        raise CliError(
            "rsync was not found on PATH.",
            code=ENV,
            hint="Install rsync (Linux/macOS: package manager; Windows: use the WSL distribution, "
                 "or `winget install rsync`).",
        )
    return exe


def _summary(result: RunResult) -> dict:
    added = modified = deleted = 0
    bytes_transferred = 0
    for line in result.output.splitlines():
        if _ADDED.match(line):
            added += 1
        elif _DELETED.match(line):
            deleted += 1
        elif _MODIFIED.match(line):
            modified += 1
        match = re.search(r"Total transferred file size: ([\d,]+)", line)
        if match:
            bytes_transferred = int(match.group(1).replace(",", ""))
    return {"added": added, "modified": modified, "deleted": deleted,
            "bytes_transferred": bytes_transferred}


def _human_bytes(num: int) -> str:
    for factor, suffix in ((1024 ** 4, "TiB"), (1024 ** 3, "GiB"),
                           (1024 ** 2, "MiB"), (1024, "KiB")):
        if num >= factor:
            return f"{num / factor:.1f} {suffix}"
    return f"{num} B"


def _print_summary(ctx: Context, summary: dict, dry: bool) -> None:
    out = ctx.out
    mode = "DRY-RUN (nothing was changed)" if dry else "done"
    out.banner(f"Sync summary — {mode}")
    out.line(f"  Added:     {summary['added']}")
    out.line(f"  Modified:  {summary['modified']}")
    out.line(f"  Removed:   {summary['deleted']}")
    out.line(f"  Transferred: {_human_bytes(summary['bytes_transferred'])}")


def _dry_run(ctx: Context, rsync: str, args: list[str]) -> dict:
    dry = run_capture([rsync, "-a", "-n", "-i", "--stats", *args])
    if dry.exit_code != 0:
        raise CliError(f"rsync dry-run failed (exit {dry.exit_code}): {dry.output.strip()[:400]}",
                       code=TRANSFER_FAIL)
    return _summary(dry)


def _real_run(ctx: Context, rsync: str, args: list[str]) -> dict:
    result = run_capture([rsync, "-a", "-i", "--stats", *args])
    if result.exit_code != 0:
        raise CliError(f"rsync failed (exit {result.exit_code}): {result.output.strip()[:400]}",
                       code=TRANSFER_FAIL)
    return _summary(result)


def _run_transfer(ctx: Context, rsync: str, args: list[str], dry_only: bool) -> int:
    summary = _dry_run(ctx, rsync, args)
    if ctx.json_mode:
        ctx.out.json({"dry_run": summary})
        return OK
    _print_summary(ctx, summary, dry=True)
    if dry_only:
        ctx.out.line()
        ctx.out.info("Dry-run only — re-run without --dry-run to apply.")
        return OK
    if not ctx.confirm("Apply the transfer?", default=False):
        raise CliError("Transfer cancelled.", code=ABORT)
    real = _real_run(ctx, rsync, args)
    _print_summary(ctx, real, dry=False)
    return OK


def _cmd_backup(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.require_root("The backup command")
    rsync = _rsync(ctx)
    if not args.to:
        raise CliError("Backup destination required.", code=USAGE,
                       hint="Example: kaiteyo backup --to /mnt/backup/kaiteyo")
    destination = pathlib.Path(args.to).expanduser()
    if not destination.exists():
        raise CliError(f"Destination does not exist: {destination}", code=USAGE,
                       hint="Create the destination directory first, or check the path.")
    excludes = [f"--exclude={name}" for name in EXCLUDES]
    trailing = "/" if str(destination).endswith(("/", "\\")) else ""
    args_list = excludes + [str(root) + "/", str(destination) + trailing]
    return _run_transfer(ctx, rsync, args_list, dry_only=args.dry_run)


def _cmd_sync(args: argparse.Namespace, ctx: Context) -> int:
    rsync = _rsync(ctx)
    if not args.from_dir or not args.to:
        raise CliError("Both --from and --to are required.", code=USAGE,
                       hint="Example: kaiteyo sync --from ./media --to /mnt/backup/media")
    source = pathlib.Path(args.from_dir).expanduser()
    destination = pathlib.Path(args.to).expanduser()
    for label, path in (("Source", source), ("Destination", destination)):
        if not path.exists():
            raise CliError(f"{label} does not exist: {path}", code=USAGE)
    base = []
    if args.delete:
        ctx.out.warn("--delete will REMOVE files in the destination that are missing in the source.")
        if not ctx.confirm("Allow destination deletion?", default=False):
            raise CliError("Sync cancelled — --delete requires confirmation.", code=ABORT)
        base.append("--delete")
    if args.exclude:
        base += [f"--exclude={name}" for name in args.exclude.split(",") if name.strip()]
    trailing = "/" if str(destination).endswith(("/", "\\")) else ""
    args_list = base + [str(source) + "/", str(destination) + trailing]
    return _run_transfer(ctx, rsync, args_list, dry_only=args.dry_run)


def _backup_menu(ctx: Context) -> int:
    while True:
        options = ["Backup repository (to a destination)", "Sync directories",
                   "Show backup excludes"]
        choice = ctx.menu("Backup / Sync", options)
        if choice is None:
            return OK
        try:
            if choice == 0:
                destination = ctx.prompt("Backup destination (existing directory)", required=True)
                dry = ctx.confirm("Dry-run only first?", default=True)
                _cmd_backup(argparse.Namespace(to=destination, dry_run=dry), ctx)
            elif choice == 1:
                src = ctx.prompt("Source directory", required=True)
                dst = ctx.prompt("Destination directory", required=True)
                delete = ctx.confirm("Allow deleting extra destination files (--delete)?", default=False)
                dry = ctx.confirm("Dry-run only first?", default=True)
                _cmd_sync(argparse.Namespace(from_dir=src, to=dst, delete=delete,
                                             dry_run=dry, exclude=None), ctx)
            else:
                ctx.out.banner("Backup excludes")
                for name in EXCLUDES:
                    ctx.out.line(f"  {name}")
        except CliError as exc:
            ctx.out.error(exc.message)
            if exc.hint:
                ctx.out.dim(f"  {exc.hint}")


def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    sub.add_argument("--to", metavar="DIR", help="backup destination")
    sub.add_argument("--from", dest="from_dir", metavar="DIR", help="sync source")
    sub.add_argument("--delete", action="store_true", help="sync: allow destination deletion (confirmed)")
    sub.add_argument("--exclude", metavar="LIST", help="comma-separated extra excludes")
    sub.add_argument("--dry-run", action="store_true", help="show what would happen, change nothing")


command = Command(
    name="backup",
    aliases=["b"],
    help="Backup the repository / sync directories (rsync, dry-run first, never silent deletes)",
    description=(
        "rsync-based backup and sync. Always shows a dry-run summary first:\n"
        "added / modified / removed files and bytes transferred. Destination\n"
        "files are never deleted unless --delete is given AND confirmed.\n\n"
        "Examples:\n"
        "  kaiteyo backup --to /mnt/backup/kaiteyo --dry-run\n"
        "  kaiteyo backup --to /mnt/backup/kaiteyo\n"
        "  kaiteyo sync --from ./docs --to /mnt/backup/docs --dry-run\n"
        "  kaiteyo sync --from ./docs --to /mnt/backup/docs\n"
    ),
    build=build,
    run=lambda args, ctx: _dispatch(args, ctx),
    menu=_backup_menu,
    menu_label="Backup / Sync",
)


def _dispatch(args: argparse.Namespace, ctx: Context) -> int:
    if args.to:
        return _cmd_backup(args, ctx)
    if args.from_dir:
        return _cmd_sync(args, ctx)
    if ctx.interactive():
        return _backup_menu(ctx)
    raise CliError("Specify --to (backup) or --from/--to (sync).", code=USAGE,
                   hint="See `kaiteyo backup --help` for examples.")
