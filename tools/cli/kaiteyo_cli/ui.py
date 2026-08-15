"""Interactive UI helpers: prompts, confirmations, menus, multi-select.

Every helper is safe in non-interactive mode:

  - confirm()  falls back to the `--yes` flag (never prompts, never blocks).
  - prompt()   raises a CliError asking the caller to supply the value as a flag.
  - menu()     raises a CliError when stdin is not a terminal.

This guarantees interactive prompts never appear in CI pipelines.
"""

from __future__ import annotations

import sys
from typing import Callable

from .errors import CliError, ABORT


def is_interactive() -> bool:
    return sys.stdin.isatty() and sys.stdout.isatty()


def confirm(prompt_text: str, default: bool = False, yes: bool = False) -> bool:
    """Ask for confirmation. `yes=True` (--yes) accepts without asking."""
    if yes:
        return True
    if not is_interactive():
        return default
    suffix = "[Y/n]" if default else "[y/N]"
    while True:
        try:
            answer = input(f"{prompt_text} {suffix} ").strip().lower()
        except (EOFError, KeyboardInterrupt):
            return default
        if not answer:
            return default
        if answer in ("y", "yes"):
            return True
        if answer in ("n", "no"):
            return False
        print("  Please answer y or n.")


def prompt(message: str, default: str | None = None, required: bool = False) -> str:
    """Prompt for a text value. Fails in non-interactive mode."""
    if not is_interactive():
        raise CliError(
            f"Interactive input required for: {message}",
            code=ABORT,
            hint="Run interactively, or supply the value via a command-line flag (see --help).",
        )
    suffix = f" [{default}]" if default else ""
    while True:
        try:
            answer = input(f"{message}{suffix}: ").strip()
        except (EOFError, KeyboardInterrupt):
            raise CliError("Cancelled.", code=ABORT)
        if answer:
            return answer
        if default:
            return default
        if not required:
            return ""


def read_multiline(message: str) -> str:
    """Read an optional multiline description (finish with an empty line)."""
    if not is_interactive():
        return ""
    print(f"{message} (empty line to finish):")
    lines: list[str] = []
    try:
        while True:
            line = input("  ")
            if line == "":
                break
            lines.append(line)
    except (EOFError, KeyboardInterrupt):
        pass
    return "\n".join(lines)


def menu(title: str, options: list[str], prompt_text: str = "Select") -> int | None:
    """Show a numbered menu and return the chosen index (or None for cancel).

    Options may be prefixed with a marker like '· ' to denote a non-selectable
    group header; those entries are still numbered but skipped by the prompt.
    """
    if not is_interactive():
        raise CliError(
            "Interactive menu requested in a non-interactive environment.",
            code=ABORT,
            hint="Use the command's flags instead (e.g. --task, --status). See `kaiteyo <cmd> --help`.",
        )
    print()
    print(f"  {title}")
    print(f"  {'-' * min(56, len(title) + 2)}")
    for i, option in enumerate(options, start=1):
        print(f"  {i:>2}. {option}")
    print(f"  0. Back / Cancel")
    while True:
        try:
            raw = input(f"\n  {prompt_text} [0-{len(options)}]: ").strip()
        except (EOFError, KeyboardInterrupt):
            return None
        if raw in ("", "0"):
            return None
        if raw.isdigit():
            index = int(raw)
            if 1 <= index <= len(options):
                return index - 1
        print(f"  Please enter a number between 0 and {len(options)}.")


def multiselect(title: str, items: list[tuple[str, str]],
                on_toggle: Callable[[list[int]], None] | None = None) -> list[int]:
    """Interactive multi-select of items: (label, description).

    Commands: numbers toggle, 'a' select all, 'n' clear, enter confirms.
    Returns the list of selected indices.
    """
    if not is_interactive():
        raise CliError(
            "Interactive selection requested in a non-interactive environment.",
            code=ABORT,
            hint="Select files with --files/--dirs/--all flags instead. See `kaiteyo git commit --help`.",
        )
    selected: set[int] = set()
    while True:
        print()
        print(f"  {title}")
        print(f"  {'-' * min(56, len(title) + 2)}")
        for i, (label, desc) in enumerate(items, start=1):
            mark = "[x]" if i - 1 in selected else "[ ]"
            line = f"  {i:>2}. {mark} {label}"
            if desc:
                line += f"  ({desc})"
            print(line)
        print("  Commands: <numbers> toggle · a = all · n = none · enter = confirm")
        try:
            raw = input("\n  Selection: ").strip()
        except (EOFError, KeyboardInterrupt):
            return sorted(selected)
        if raw == "":
            return sorted(selected)
        if raw.lower() == "a":
            selected = set(range(len(items)))
        elif raw.lower() == "n":
            selected = set()
        else:
            for part in raw.replace(",", " ").split():
                if part.isdigit() and 1 <= int(part) <= len(items):
                    index = int(part) - 1
                    if index in selected:
                        selected.discard(index)
                    else:
                        selected.add(index)
        if on_toggle:
            on_toggle(sorted(selected))
