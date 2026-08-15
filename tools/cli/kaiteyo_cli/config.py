"""Central CLI configuration.

A single configuration source shared by every command (no duplicated config
across tools). Values resolve in this order (lowest to highest priority):

    1. built-in defaults
    2. user config    ~/.kaiteyo/cli/config.json        (per-developer)
    3. project config <root>/.kaiteyo/config.json       (per-repository)
    4. environment    KAITEYO_<KEY> environment variables
    5. command-line flags (handled by callers)

The CLI's user state lives under ~/.kaiteyo/cli/ — the desktop app owns
~/.kaiteyo/ itself, so the CLI never touches the app's files.
"""

from __future__ import annotations

import json
import os
import pathlib
from typing import Any

from .errors import CliError, USAGE

USER_CONFIG_DIR = pathlib.Path.home() / ".kaiteyo" / "cli"
PROJECT_CACHE_DIR = ".kaiteyo"  # relative to the project root

DEFAULTS: dict[str, Any] = {
    "project_root": None,
    "preferred_branch": "develop",
    "default_remote": "origin",
    "preferred_terminal": None,  # auto-detect when None
    "preferred_shell": None,     # auto-detect when None
    "wsl_distro": None,          # auto-select default distribution when None
    "gradle_wrapper": "wrapper", # "wrapper" (gradlew) | "system" (gradle on PATH)
    "confirmations": "ask",      # "ask" | "yes" | "no"
    "theme": "auto",             # "auto" | "light" | "dark"
    "verbosity": "normal",       # "quiet" | "normal" | "verbose"
}

_VALID = {
    "gradle_wrapper": ("wrapper", "system"),
    "confirmations": ("ask", "yes", "no"),
    "theme": ("auto", "light", "dark"),
    "verbosity": ("quiet", "normal", "verbose"),
}


def _load_json(path: pathlib.Path) -> dict:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else {}
    except (OSError, ValueError):
        return {}


class Config:
    def __init__(self, project_root: str | None = None):
        self.project_root = pathlib.Path(project_root) if project_root else None
        self.user_file = USER_CONFIG_DIR / "config.json"
        self.project_file = (
            self.project_root / PROJECT_CACHE_DIR / "config.json"
            if self.project_root
            else None
        )
        self.values: dict[str, Any] = dict(DEFAULTS)
        self.values.update(_load_json(self.user_file))
        if self.project_file:
            self.values.update(_load_json(self.project_file))
        for key in self.values:
            env = os.environ.get(f"KAITEYO_{key.upper()}")
            if env:
                self.values[key] = env

    # -- accessors ---------------------------------------------------------
    def get(self, key: str, default: Any = None) -> Any:
        if key not in DEFAULTS:
            return default
        return self.values.get(key, default)

    def __getitem__(self, key: str) -> Any:
        if key not in DEFAULTS:
            raise KeyError(key)
        return self.values[key]

    def keys(self) -> list[str]:
        return list(DEFAULTS)

    # -- mutation ----------------------------------------------------------
    def set(self, key: str, value: Any, *, scope: str = "user") -> None:
        if key not in DEFAULTS:
            raise CliError(f"Unknown config key: {key}", code=USAGE,
                           hint=f"Valid keys: {', '.join(DEFAULTS)}")
        # None / empty means "unset" (back to default) — valid for every key.
        if value in ("", None):
            value = None
        elif key in _VALID and value not in _VALID[key]:
            raise CliError(
                f"Invalid value {value!r} for {key}.", code=USAGE,
                hint=f"Valid values: {', '.join(_VALID[key])}")
        target = self.user_file if scope == "user" else self.project_file
        if target is None:
            raise CliError("No project root — cannot write project config.", code=USAGE)
        data = _load_json(target)
        data[key] = value
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
        self.values[key] = value

    def describe(self) -> dict[str, Any]:
        return {k: self.values.get(k) for k in DEFAULTS}
