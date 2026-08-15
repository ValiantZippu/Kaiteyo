"""Command history.

Remembers the *command path* of recently used commands (e.g. ["git", "commit"])
so the main menu can offer quick reuse. Arguments are never stored — this keeps
commit titles, file lists and any secrets out of the history file by design.
"""

from __future__ import annotations

import json
import pathlib

from .config import USER_CONFIG_DIR

MAX_ENTRIES = 10


def _history_file() -> pathlib.Path:
    return USER_CONFIG_DIR / "history.json"


def _load() -> list[list[str]]:
    try:
        data = json.loads(_history_file().read_text(encoding="utf-8"))
        if isinstance(data, list):
            return [entry for entry in data if isinstance(entry, list)]
    except (OSError, ValueError):
        pass
    return []


def add(command_path: list[str]) -> None:
    """Record a used command path (names only — never arguments)."""
    if not command_path:
        return
    history = _load()
    history = [entry for entry in history if entry != command_path]
    history.insert(0, command_path)
    del history[MAX_ENTRIES:]
    try:
        _history_file().parent.mkdir(parents=True, exist_ok=True)
        _history_file().write_text(json.dumps(history), encoding="utf-8")
    except OSError:  # pragma: no cover - read-only home dir etc.
        pass


def recent() -> list[list[str]]:
    return _load()
