"""Command registry: every command module registers itself here.

Future modules (android, data, perf, docker, ...) follow the same pattern:
create commands/<name>.py exposing a `Command`, then add one line here.
"""

from __future__ import annotations

from ..registry import Registry

_registry = Registry()


def _register() -> None:
    import importlib

    # Order matters for the main menu: the most-used commands come first.
    names = ["git", "gradle", "wsl", "dev", "doctor", "info", "files", "logs",
             "docs", "release", "backup", "settings", "clean", "runcmd"]
    for name in names:
        module = importlib.import_module(f".{name}", package=__name__)
        _registry.register(module.command)


_register()


def registry() -> Registry:
    return _registry
