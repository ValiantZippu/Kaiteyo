"""kaiteyo release — release preflight (read-only).

Prepares the architecture for a release workflow: version, changelog, git
status, tags, and a preview of the planned steps. This command NEVER
publishes, tags, bumps versions, or uploads anything — it shows what a
release would involve and checks preconditions.
"""

from __future__ import annotations

import argparse
import pathlib
import re

from ..context import Context
from ..errors import CliError, ENV, OK
from ..registry import Command
from ..runner import run_capture
from .info import project_name, version_from_source
from .git import _snapshot, _require_git
from .gradle import wrapper_version


def _git(ctx: Context, *args: str) -> str:
    root = ctx.require_root("The release command")
    result = run_capture(["git", "-C", str(root), *args])
    return result.output.strip() if result.exit_code == 0 else ""


def _changelog_versions(root: pathlib.Path) -> list[str]:
    changelog = root / "CHANGELOG.md"
    if not changelog.is_file():
        return []
    text = changelog.read_text(encoding="utf-8", errors="replace")
    return re.findall(r"^##\s+v?([\d.]+)", text, re.MULTILINE)


def _cmd_status(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.require_root("The release command")
    version = version_from_source(root)
    changelog = _changelog_versions(root)
    branch = _git(ctx, "rev-parse", "--abbrev-ref", "HEAD")
    tags = _git(ctx, "tag", "--list")
    info = {
        "project": project_name(root),
        "version": version,
        "changelog_versions": changelog,
        "latest_changelog": changelog[0] if changelog else None,
        "branch": branch,
        "tags": [t for t in tags.splitlines() if t],
        "gradle_wrapper": wrapper_version(root),
    }
    if ctx.json_mode:
        ctx.out.json(info)
        return OK
    tags = info["tags"]
    shown_tags = tags[-12:]
    tags_text = ", ".join(shown_tags) if shown_tags else "(none)"
    if len(tags) > len(shown_tags):
        tags_text += f" (+{len(tags) - len(shown_tags)} more)"
    ctx.out.banner("Release status")
    rows = [
        ("Project", info["project"]),
        ("Version (AppVersion.kt)", info["version"] or "(undetectable)"),
        ("Latest changelog entry", info["latest_changelog"] or "(none)"),
        ("Branch", branch),
        ("Tags", tags_text),
    ]
    width = max(len(k) for k, _ in rows)
    for key, value in rows:
        ctx.out.line(f"  {key.ljust(width)}  {value}")
    return OK


def _cmd_check(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.require_root("The release command")
    _require_git(ctx)
    checks: list[dict] = []
    version = version_from_source(root)
    checks.append({
        "name": "version source",
        "status": "pass" if version else "fail",
        "detail": f"AppVersion.kt → {version}" if version else "AppVersion.kt unreadable",
        "hint": None if version else "Expect buildSrc/src/main/kotlin/AppVersion.kt.",
    })
    changelog = _changelog_versions(root)
    latest = changelog[0] if changelog else None
    checks.append({
        "name": "changelog",
        "status": "pass" if (latest and latest == version) else "warn",
        "detail": f"CHANGELOG.md head: {latest or '(none)'}" + ("" if latest == version else f" (version {version} missing)"),
        "hint": "Add an entry for the version being released.",
    })
    try:
        snap = _snapshot(ctx)
        checks.append({
            "name": "working tree",
            "status": "pass" if snap.clean else "fail",
            "detail": "clean" if snap.clean else f"{len(snap.all_changed)} changed file(s) — commit or stash first",
            "hint": "Releases should be tagged from a clean tree.",
        })
        checks.append({
            "name": "upstream",
            "status": "pass" if snap.has_upstream else "warn",
            "detail": snap.upstream or "no upstream configured",
            "hint": "Set an upstream so `kaiteyo git push` works.",
        })
    except CliError as exc:
        checks.append({"name": "git", "status": "fail", "detail": exc.message, "hint": exc.hint})
    if ctx.json_mode:
        ctx.out.json({"checks": checks})
        return OK
    ctx.out.banner("Release preconditions")
    for check in checks:
        mark = {"pass": ctx.out.ok, "warn": ctx.out.warn, "fail": ctx.out.fail}[check["status"]]
        mark(f"{check['name']}: {check['detail']}")
        if check.get("hint"):
            ctx.out.dim(f"         → {check['hint']}")
    return OK


def _cmd_preview(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.require_root("The release command")
    version = version_from_source(root)
    changelog = _changelog_versions(root)
    tags = [t for t in _git(ctx, "tag", "--list").splitlines() if t]
    ctx.out.banner("Release preview — nothing will be executed")
    ctx.out.info(f"Current version: {version or '(undetectable)'}")
    ctx.out.info(f"Planned tag:     v{version}" if version else "Planned tag: (unknown — version source missing)")
    ctx.out.line()
    steps = [
        ("1. Version", "Bump AppVersion.kt (versionCode, versionName, desktopAppVersion)."),
        ("2. Changelog", "Add a CHANGELOG.md entry for the new version."),
        ("3. Commit", "Commit the changes with `kaiteyo git commit`."),
        ("4. Tag", f"git tag v{version or '<version>'}" if version else "git tag v<version>"),
        ("5. Push", "Push the branch and the tag (git push --tags)."),
        ("6. Build", "Build platform artifacts: :desktopApp:package* per OS, :app:assembleFdroidRelease."),
        ("7. GitHub release", "Create the GitHub release from the tag (manual step)."),
    ]
    for label, text in steps:
        ctx.out.line(f"  {label:<9} {text}")
    ctx.out.line()
    ctx.out.warn("This command only previews. It does not bump, tag, push or publish anything.")
    if changelog:
        ctx.out.dim(f"  Changelog versions on file: {', '.join(changelog[:5])}")
    if tags:
        ctx.out.dim(f"  Existing tags: {', '.join(tags[-8:])}")
    return OK


def _release_menu(ctx: Context) -> int:
    while True:
        options = ["Status", "Check preconditions", "Preview release steps"]
        choice = ctx.menu("Release (read-only)", options)
        if choice is None:
            return OK
        try:
            if choice == 0:
                _cmd_status(argparse.Namespace(), ctx)
            elif choice == 1:
                _cmd_check(argparse.Namespace(), ctx)
            else:
                _cmd_preview(argparse.Namespace(), ctx)
        except CliError as exc:
            ctx.out.error(exc.message)
            if exc.hint:
                ctx.out.dim(f"  {exc.hint}")


def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    subs = sub.add_subparsers(dest="release_command", metavar="COMMAND")

    def P(name: str, **kw: object):
        if common is not None:
            kw["parents"] = [common]
        return subs.add_parser(name, **kw)  # type: ignore[arg-type]

    p = P("status", help="Show version, changelog, branch and tags")
    p.set_defaults(handler=_cmd_status)
    p = P("check", help="Check release preconditions (read-only)")
    p.set_defaults(handler=_cmd_check)
    p = P("preview", help="Preview the release steps — never executes them")
    p.set_defaults(handler=_cmd_preview)


command = Command(
    name="release",
    help="Release preflight: version, changelog, git status, tags, step preview",
    description=(
        "Read-only release preparation: current version, changelog state, git\n"
        "preconditions, and a preview of the steps a release involves. Nothing\n"
        "is ever published, tagged or bumped by this command.\n\n"
        "Examples:\n"
        "  kaiteyo release status\n"
        "  kaiteyo release check\n"
        "  kaiteyo release preview\n"
    ),
    build=build,
    run=lambda args, ctx: _dispatch(args, ctx),
    menu=_release_menu,
    menu_label="Release",
)


def _dispatch(args: argparse.Namespace, ctx: Context) -> int:
    handler = getattr(args, "handler", None)
    if handler:
        return handler(args, ctx)
    if ctx.interactive():
        return _release_menu(ctx)
    raise CliError("Specify a release subcommand.", code=2,
                   hint="Try: kaiteyo release status | check | preview")
