"""The Context passed to every command: output, config, root, and flags.

Commands use ctx helpers for prompts/confirmations so non-interactive and
CI behavior is consistent everywhere:

  - ctx.confirm(...)      -> honors --yes, defaults safely in non-interactive
  - ctx.prompt(...)       -> fails in non-interactive mode
  - ctx.menu(...)         -> fails in non-interactive mode
"""

from __future__ import annotations

import argparse
import pathlib

from . import ui
from .config import Config
from .errors import CliError, ENV
from .output import Out


class Context:
    def __init__(self, flags: argparse.Namespace, out: Out, cfg: Config, root: pathlib.Path | None):
        self.flags = flags
        self.out = out
        self.cfg = cfg
        self.root = root

    # -- properties --------------------------------------------------------
    @property
    def yes(self) -> bool:
        return bool(getattr(self.flags, "yes", False))

    @property
    def json_mode(self) -> bool:
        return self.out.json_mode

    def interactive(self) -> bool:
        return ui.is_interactive()

    # -- root --------------------------------------------------------------
    def require_root(self, for_command: str = "this command") -> pathlib.Path:
        if self.root is None:
            raise CliError(
                f"{for_command} requires a Kaiteyo repository root.",
                code=ENV,
                hint="Run from inside the repository, or pass --root PATH.",
            )
        return self.root

    def require_git(self) -> pathlib.Path:
        return self.require_root("This git command")

    # -- UI shortcuts ------------------------------------------------------
    def confirm(self, prompt_text: str, default: bool = False) -> bool:
        """Confirmation honoring config + --yes; safe default in CI."""
        if self.yes:
            return True
        mode = self.cfg.get("confirmations", "ask")
        if mode == "yes":
            return True
        if mode == "no":
            return False
        return ui.confirm(prompt_text, default=default, yes=False)

    def prompt(self, message: str, default: str | None = None, required: bool = False) -> str:
        return ui.prompt(message, default=default, required=required)

    def read_multiline(self, message: str) -> str:
        return ui.read_multiline(message)

    def menu(self, title: str, options: list[str]) -> int | None:
        return ui.menu(title, options)

    def multiselect(self, title: str, items: list[tuple[str, str]]) -> list[int]:
        return ui.multiselect(title, items)
