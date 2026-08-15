"""Command registry.

Every command (current or future: android, data, perf, docker, ...) is a
`Command` registered here. The app builds argparse from the registry, so
adding a new command is a matter of one module + one registration.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass, field
from typing import TYPE_CHECKING, Callable

if TYPE_CHECKING:  # pragma: no cover
    from .context import Context


@dataclass
class Command:
    name: str
    help: str
    description: str
    run: Callable[[argparse.Namespace, "Context"], int]

    #: build(sub, common) — `common` carries the global flags (--json, --yes, ...)
    #: so nested subparsers can inherit them. See app.build_parser().
    build: Callable[[argparse.ArgumentParser, argparse.ArgumentParser | None], None]

    aliases: list[str] = field(default_factory=list)
    menu: Callable[["Context"], int] | None = None      # interactive entry
    menu_label: str | None = None                       # label in the main menu

    def __post_init__(self) -> None:
        if self.menu_label is None:
            self.menu_label = self.name.capitalize()


class Registry:
    def __init__(self) -> None:
        self._commands: dict[str, Command] = {}
        self._aliases: dict[str, str] = {}

    def register(self, command: Command) -> None:
        self._commands[command.name] = command
        for alias in command.aliases:
            self._aliases[alias] = command.name

    def get(self, name: str) -> Command | None:
        if name in self._commands:
            return self._commands[name]
        if name in self._aliases:
            return self._commands[self._aliases[name]]
        return None

    def resolve(self, name: str) -> tuple[Command | None, str]:
        """Resolve a possibly-aliased name; returns (command, canonical name)."""
        if name in self._commands:
            return self._commands[name], name
        if name in self._aliases:
            canonical = self._aliases[name]
            return self._commands[canonical], canonical
        return None, name

    def all(self) -> list[Command]:
        return list(self._commands.values())

    def names(self) -> list[str]:
        return list(self._commands)

    def suggestions(self, name: str, limit: int = 3) -> list[str]:
        import difflib

        return difflib.get_close_matches(name, self.names(), n=limit)
