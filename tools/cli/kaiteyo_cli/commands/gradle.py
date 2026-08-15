"""kaiteyo gradle — Gradle Command Center.

Discovers tasks from the repository's Gradle configuration instead of
hardcoding an enormous list:

  - modules come from settings.gradle.kts `include(...)` (parsed statically),
  - task lists come from `gradlew tasks --all` and are cached per repository,
  - the standard lifecycle menu works even before the first task refresh.

The CLI never runs Gradle on its own: the user chooses the command, sees the
exact command line first, and confirms expensive operations. Output is
streamed, timed, and reported with SUCCESS/FAILED + the real exit code.
"""

from __future__ import annotations

import argparse
import json
import pathlib
import re
import time

from .. import history
from ..config import PROJECT_CACHE_DIR
from ..context import Context
from ..errors import CliError, ABORT, ENV, GRADLE_FAIL, OK
from ..registry import Command
from ..runner import run_capture, run_stream
from .. import platform
from ..secrets import mask_text

EXPENSIVE_MARK = "This may take several minutes."
LIFECYCLE_TASKS = ["tasks", "build", "assemble", "test", "clean", "check", "dependencies"]

_INCLUDE_RE = re.compile(r'include(?:\(|\s+\(\s*)([^)]*)\)')
_STRING_RE = re.compile(r'"([^"]+)"')


# ---------------------------------------------------------------------------
# discovery (static — no Gradle required)
# ---------------------------------------------------------------------------

def modules_from_settings(root: pathlib.Path) -> list[str]:
    """Parse module names from settings.gradle.kts include(...) lines."""
    settings = root / "settings.gradle.kts"
    if not settings.is_file():
        settings = root / "settings.gradle"
    if not settings.is_file():
        return []
    text = settings.read_text(encoding="utf-8", errors="replace")
    modules: list[str] = []
    for match in _INCLUDE_RE.finditer(text):
        for s in _STRING_RE.findall(match.group(1)):
            name = s.strip()
            if name and name not in modules:
                modules.append(name if name.startswith(":") else f":{name}")
    # Prefer a stable, conventional order: core, app, desktopApp, iosApp, others.
    priority = ["core", "app", "desktopApp", "iosApp"]
    modules.sort(key=lambda m: (priority.index(m[1:]) if m[1:] in priority else 99, m))
    return modules


def wrapper_version(root: pathlib.Path) -> str | None:
    props = root / "gradle" / "wrapper" / "gradle-wrapper.properties"
    if not props.is_file():
        return None
    for line in props.read_text(encoding="utf-8", errors="replace").splitlines():
        if line.startswith("distributionUrl="):
            match = re.search(r"gradle-([\d.]+)", line)
            if match:
                return match.group(1)
    return None


def gradlew_command(ctx: Context) -> list[str]:
    """The Gradle launcher: wrapper by default, system gradle when configured."""
    root = ctx.require_root("The gradle command")
    preference = ctx.cfg.get("gradle_wrapper", "wrapper")
    if preference == "system":
        if platform.which("gradle"):
            return ["gradle"]
        ctx.out.warn("gradle_wrapper=system but no `gradle` on PATH — falling back to the wrapper.")
    if platform.is_windows() and (root / "gradlew.bat").is_file():
        return ["cmd.exe", "/c", str(root / "gradlew.bat")]
    if (root / "gradlew").is_file():
        return [str(root / "gradlew")]
    if platform.which("gradle"):
        return ["gradle"]
    raise CliError("No Gradle wrapper found and no system `gradle` on PATH.",
                   code=ENV, hint="Run `kaiteyo gradle` from the repository root, or install Gradle.")


# ---------------------------------------------------------------------------
# task cache
# ---------------------------------------------------------------------------

def _cache_file(ctx: Context) -> pathlib.Path:
    root = ctx.require_root("The gradle command")
    return root / PROJECT_CACHE_DIR / "gradle-tasks.json"


def load_task_cache(ctx: Context) -> list[str]:
    try:
        data = json.loads(_cache_file(ctx).read_text(encoding="utf-8"))
        tasks = data.get("tasks", [])
        if isinstance(tasks, list):
            return [t for t in tasks if isinstance(t, str)]
    except (OSError, ValueError):
        pass
    return []


def _save_task_cache(ctx: Context, tasks: list[str]) -> None:
    path = _cache_file(ctx)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({"tasks": sorted(set(tasks)), "generated": time.time()}, indent=2),
                    encoding="utf-8")


def refresh_task_cache(ctx: Context) -> list[str]:
    """Run `gradlew tasks --all` and cache the result. Expensive — confirmed first."""
    gradle = gradlew_command(ctx)
    command = gradle + ["tasks", "--all", "--console=plain"]
    ctx.out.warn(EXPENSIVE_MARK)
    if not ctx.confirm("Refresh the Gradle task list (runs Gradle now)?", default=True):
        raise CliError("Task refresh cancelled.", code=ABORT)
    result = run_stream(command, echo=True)
    if result.exit_code != 0:
        raise CliError(f"`gradlew tasks` failed (exit {result.exit_code}).", code=GRADLE_FAIL,
                       hint="See the Gradle output above.")
    tasks = [
        line.strip() for line in result.output.splitlines()
        if line.strip() and not line.startswith((" ", "-", "Tasks", "----", "To see")) and " - " in line
    ]
    tasks = [line.split(" - ", 1)[0].strip() for line in tasks]
    tasks = [t for t in tasks if not t.startswith(("-", ">"))]
    _save_task_cache(ctx, tasks)
    ctx.out.ok(f"Cached {len(tasks)} tasks.")
    return tasks


def search_tasks(ctx: Context, query: str, tasks: list[str]) -> list[str]:
    """Substring match with module-prefix awareness (e.g. 'desktop' matches :desktopApp:*)."""
    q = query.lower()
    results = [t for t in tasks if q in t.lower()]
    if not results:
        results = [t for t in tasks if q in t.lower().split(":")[-1]]
    return results


# ---------------------------------------------------------------------------
# execution
# ---------------------------------------------------------------------------

HEAVY_PATTERN = re.compile(
    r"(build|assemble|test|check|package|install|run|benchmark|publish|bundle|connected|lint|dependencyUpdates)",
    re.IGNORECASE,
)


def run_task(ctx: Context, task: str, args: argparse.Namespace) -> int:
    gradle = gradlew_command(ctx)
    command = gradle + [task]
    if getattr(args, "no_daemon", False):
        command.append("--no-daemon")
    if getattr(args, "stacktrace", False):
        command.append("--stacktrace")

    ctx.out.line()
    ctx.out.info("Command: " + mask_text(" ".join(command)))
    if HEAVY_PATTERN.search(task):
        ctx.out.warn(EXPENSIVE_MARK)
    if ctx.interactive() and not ctx.yes and not ctx.confirm("Run?", default=False):
        raise CliError("Gradle run cancelled.", code=ABORT)

    start = time.monotonic()
    result = run_stream(command, echo=False)
    elapsed = time.monotonic() - start
    success = result.exit_code == 0
    ctx.out.result(success, result.exit_code, elapsed, label="Gradle")
    if not success:
        raise CliError(f"Gradle task failed: {task} (exit {result.exit_code}).",
                       code=GRADLE_FAIL,
                       hint="Run the same task with --stacktrace for a full error trace.")
    return OK


# ---------------------------------------------------------------------------
# commands
# ---------------------------------------------------------------------------

def _cmd_tasks(args: argparse.Namespace, ctx: Context) -> int:
    tasks = load_task_cache(ctx)
    if getattr(args, "refresh", False):
        tasks = refresh_task_cache(ctx)  # explicit request — runs Gradle
    elif not tasks:
        if ctx.interactive():
            tasks = refresh_task_cache(ctx)
        else:
            ctx.out.info("No cached task list yet.")
            ctx.out.info("Run `kaiteyo gradle tasks --refresh` once to generate it (this runs Gradle).")
            return OK
    if ctx.json_mode:
        ctx.out.json({"tasks": sorted(set(tasks))})
        return OK
    if not tasks:
        ctx.out.info("No cached tasks yet — run `kaiteyo gradle tasks --refresh`.")
        return OK
    ctx.out.banner(f"Gradle tasks ({len(tasks)})")
    for task in sorted(set(tasks)):
        ctx.out.line(f"  {task}")
    return OK


def _cmd_search(args: argparse.Namespace, ctx: Context) -> int:
    tasks = load_task_cache(ctx)
    if not tasks and ctx.interactive():
        tasks = refresh_task_cache(ctx)
    if not tasks:
        raise CliError("No task list available.", code=ENV,
                       hint="Run `kaiteyo gradle tasks --refresh` once (it runs Gradle).")
    matches = search_tasks(ctx, args.query, tasks)
    if ctx.json_mode:
        ctx.out.json({"query": args.query, "matches": matches})
        return OK
    ctx.out.banner(f"Tasks matching '{args.query}' ({len(matches)})")
    for task in sorted(set(matches)):
        ctx.out.line(f"  {task}")
    return OK


def _cmd_modules(args: argparse.Namespace, ctx: Context) -> int:
    root = ctx.require_root("The gradle command")
    modules = modules_from_settings(root)
    if ctx.json_mode:
        ctx.out.json({"modules": modules})
        return OK
    ctx.out.banner("Modules (from settings.gradle.kts)")
    for module in modules:
        ctx.out.line(f"  {module}")
    return OK


def _cmd_run(args: argparse.Namespace, ctx: Context) -> int:
    if not args.task:
        raise CliError("No task given.", code=2, hint="Example: kaiteyo gradle --task :core:compileKotlinJvm")
    history.add(["gradle", args.task])
    return run_task(ctx, args.task, args)


# ---------------------------------------------------------------------------
# interactive center
# ---------------------------------------------------------------------------

def _task_menu(ctx: Context, tasks: list[str]) -> int:
    """Browse cached tasks grouped by module (for the 'browse/search' option)."""
    grouped: dict[str, list[str]] = {}
    for task in sorted(set(tasks)):
        if task.startswith(":"):
            module, _, name = task[1:].partition(":")
            grouped.setdefault(f":{module}", []).append(name or task)
        else:
            grouped.setdefault("(root)", []).append(task)
    for module, names in grouped.items():
        ctx.out.section(module)
        for name in names[:40]:
            ctx.out.line(f"  {name}")
    return OK


def _gradle_menu(ctx: Context) -> int:
    tasks = load_task_cache(ctx)
    while True:
        options = [
            "tasks",
            "build",
            "assemble",
            "test",
            "clean",
            "check",
            "dependencies",
            "desktop tasks (:desktopApp:*)",
            "run configuration (:desktopApp:run)",
            "search tasks",
            "custom command",
            "refresh task list",
        ]
        choice = ctx.menu("Gradle Command Center", options)
        if choice is None:
            return OK
        try:
            if choice == 0:
                _cmd_tasks(argparse.Namespace(), ctx)
            elif choice in (1, 2, 3, 4, 5, 6):
                names = ["build", "assemble", "test", "clean", "check", "dependencies"]
                run_task(ctx, names[choice - 1], argparse.Namespace())
            elif choice == 7:
                desktop = sorted(t for t in tasks if t.startswith(":desktopApp:")) if tasks else []
                if not desktop:
                    ctx.out.info("No cached :desktopApp: tasks yet — refresh the task list first.")
                    continue
                _task_menu(ctx, desktop)
            elif choice == 8:
                run_task(ctx, ":desktopApp:run", argparse.Namespace())
            elif choice == 9:
                query = ctx.prompt("Search tasks for", required=True)
                _cmd_search(argparse.Namespace(query=query), ctx)
            elif choice == 10:
                custom = ctx.prompt("Gradle task or arguments", required=True)
                run_task(ctx, custom, argparse.Namespace())
            elif choice == 11:
                refresh_task_cache(ctx)
        except CliError as exc:
            ctx.out.error(exc.message)
            if exc.hint:
                ctx.out.dim(f"  {exc.hint}")


# ---------------------------------------------------------------------------
# parser
# ---------------------------------------------------------------------------

def build(sub: argparse.ArgumentParser, common: argparse.ArgumentParser | None = None) -> None:
    sub.add_argument("--task", metavar="TASK", help="run a task directly (non-interactive)")
    sub.add_argument("--no-daemon", action="store_true", help="append --no-daemon")
    sub.add_argument("--stacktrace", action="store_true", help="append --stacktrace")
    subs = sub.add_subparsers(dest="gradle_command", metavar="COMMAND")

    def P(name: str, **kw: object):
        if common is not None:
            kw["parents"] = [common]
        return subs.add_parser(name, **kw)  # type: ignore[arg-type]

    p = P("tasks", help="List discovered tasks (cached)")
    p.add_argument("--refresh", action="store_true", help="re-run `gradlew tasks --all`")
    p.set_defaults(handler=_cmd_tasks)

    p = P("search", help="Search the cached task list")
    p.add_argument("query", help="substring to search, e.g. desktop, test, release")
    p.set_defaults(handler=_cmd_search)

    p = P("modules", help="List modules from settings.gradle.kts")
    p.set_defaults(handler=_cmd_modules)


command = Command(
    name="gradle",
    aliases=["gr"],
    help="Gradle Command Center: discover tasks, preview, run with live output",
    description=(
        "Discovers Gradle tasks from the repository and runs them with a preview, "
        "elapsed time, exit code and SUCCESS/FAILED result. Never runs Gradle "
        "automatically — the user chooses.\n\n"
        "Examples:\n"
        "  kaiteyo gradle                 interactive Gradle Command Center\n"
        "  kaiteyo gradle --task tasks\n"
        "  kaiteyo gradle --task :desktopApp:compileKotlinJvm --yes\n"
        "  kaiteyo gradle tasks --refresh (runs Gradle once)\n"
        "  kaiteyo gradle search desktop\n"
        "  kaiteyo gradle modules\n"
    ),
    build=build,
    run=lambda args, ctx: _dispatch(args, ctx),
    menu=_gradle_menu,
    menu_label="Gradle",
)


def _dispatch(args: argparse.Namespace, ctx: Context) -> int:
    handler = getattr(args, "handler", None)
    if handler:
        return handler(args, ctx)
    if args.task:
        return _cmd_run(args, ctx)
    if ctx.interactive():
        return _gradle_menu(ctx)
    raise CliError("Specify a Gradle action.", code=2,
                   hint="Try: kaiteyo gradle --task TASK | tasks | search QUERY | modules")
