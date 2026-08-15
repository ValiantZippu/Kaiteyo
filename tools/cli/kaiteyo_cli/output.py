"""Terminal output: consistent status markers, colors, banners, JSON emission.

Colors are only emitted when the terminal supports them (a TTY, no NO_COLOR).
Status markers use plain ASCII text ([PASS]/[WARN]/[FAIL]/[INFO]) so they are
readable on every terminal — color is an enhancement, never a requirement.
"""

from __future__ import annotations

import json
import os
import sys

RESET = "\x1b[0m"
BOLD = "\x1b[1m"
DIM = "\x1b[2m"
RED = "\x1b[31m"
GREEN = "\x1b[32m"
YELLOW = "\x1b[33m"
BLUE = "\x1b[34m"
MAGENTA = "\x1b[35m"
CYAN = "\x1b[36m"

# Windows 10+ cmd.exe/PowerShell support ANSI once VT processing is enabled.
if sys.platform == "win32":
    try:
        os.system("")  # noqa: S605 - enables VT mode in cmd.exe
    except Exception:  # pragma: no cover
        pass


def _supports_color() -> bool:
    if os.environ.get("NO_COLOR"):
        return False
    if os.environ.get("TERM") == "dumb":
        return False
    return sys.stdout.isatty()


class Out:
    """Small output facade shared by every command."""

    def __init__(self, color: bool | None = None, json_mode: bool = False, quiet: bool = False):
        self.color = _supports_color() if color is None else color
        self.json_mode = json_mode
        self.quiet = quiet

    # -- low level ---------------------------------------------------------
    def paint(self, text: str, *codes: str) -> str:
        if not self.color or not codes:
            return text
        return "".join(codes) + text + RESET

    def line(self, text: str = "") -> None:
        if not self.quiet:
            # flush=True so progress lines are visible immediately, even when
            # stdout is piped (Python buffers otherwise). Long operations that
            # print before doing work must not look frozen.
            print(text, flush=True)

    def dim(self, text: str) -> None:
        self.line(self.paint(text, DIM))

    def banner(self, title: str, width: int = 60) -> None:
        rule = "=" * width
        self.line(self.paint(rule, BOLD))
        self.line(self.paint(f"  {title}", BOLD))
        self.line(self.paint(rule, BOLD))
        self.line()

    def section(self, title: str) -> None:
        self.line()
        self.line(self.paint(title, BOLD, CYAN))

    # -- status markers ----------------------------------------------------
    def status(self, mark: str, text: str, color: str) -> None:
        self.line(f"{self.paint(mark, BOLD, color)} {text}")

    def ok(self, text: str) -> None:
        self.status("[PASS]", text, GREEN)

    def warn(self, text: str) -> None:
        self.status("[WARN]", text, YELLOW)

    def fail(self, text: str) -> None:
        self.status("[FAIL]", text, RED)

    def info(self, text: str) -> None:
        self.status("[INFO]", text, BLUE)

    def note(self, text: str) -> None:
        """Progress/status line on stderr, always flushed.

        Used for long-running steps (e.g. `git status` over a slow network
        filesystem) so the user sees activity even when stdout is piped or
        JSON mode is on — stderr never pollutes command output.
        """
        print(self.paint(text, DIM), file=sys.stderr, flush=True)

    def error(self, text: str) -> None:
        # Errors always go to stderr, even in quiet mode.
        print(self.paint(f"[ERROR] {text}", BOLD, RED), file=sys.stderr, flush=True)

    # -- result banner -----------------------------------------------------
    def result(self, success: bool, exit_code: int, elapsed: float, label: str = "") -> None:
        if success:
            self.line(self.paint("SUCCESS", BOLD, GREEN) + self.paint(f"  ({elapsed:.1f}s)", DIM))
        else:
            self.line(
                self.paint("FAILED", BOLD, RED)
                + self.paint(f"  exit code {exit_code}", DIM)
                + self.paint(f"  ({elapsed:.1f}s)", DIM)
            )
        if label:
            self.dim(f"  {label}")

    # -- tables ------------------------------------------------------------
    def table(self, rows: list[list[str]], headers: list[str] | None = None) -> None:
        all_rows: list[list[str]] = ([headers] if headers else []) + rows
        if not all_rows:
            return
        widths = [max(len(str(r[i])) for r in all_rows) for i in range(len(all_rows[0]))]
        for row in all_rows:
            cells = [str(c).ljust(widths[i]) for i, c in enumerate(row)]
            if headers and row is all_rows[0]:
                self.line(self.paint("  ".join(cells), BOLD))
            else:
                self.line("  " + "  ".join(cells))

    # -- JSON --------------------------------------------------------------
    def json(self, obj: object) -> None:
        """Emit a single JSON document on stdout; nothing else is printed."""
        print(json.dumps(obj, indent=2, ensure_ascii=False, default=str), flush=True)
