"""Exit codes and error handling.

Exit codes are stable so the CLI is usable from CI scripts:

    0  success
    1  generic failure (an operation ran and failed)
    2  usage error (unknown command, invalid arguments)
    3  environment problem (missing executable, not inside a repository)
    4  aborted by the user — or a prompt was required but the run is non-interactive
    5  unsupported on this platform (e.g. WSL commands on macOS)
    6  git operation failed
    7  Gradle operation failed
    8  backup/sync (transfer) or WSL operation failed
"""

from __future__ import annotations

OK = 0
FAILED = 1
USAGE = 2
ENV = 3
ABORT = 4
UNSUPPORTED = 5
GIT_FAIL = 6
GRADLE_FAIL = 7
TRANSFER_FAIL = 8

EXIT_DOCS = {
    OK: "success",
    FAILED: "the operation ran but failed",
    USAGE: "bad usage — unknown command or invalid arguments",
    ENV: "environment problem — a required tool or path is missing",
    ABORT: "aborted by the user, or a prompt was needed in non-interactive mode",
    UNSUPPORTED: "the command is not supported on this platform",
    GIT_FAIL: "a git operation failed",
    GRADLE_FAIL: "a Gradle operation failed",
    TRANSFER_FAIL: "a backup/sync or WSL operation failed",
}


class CliError(Exception):
    """Raised by commands to signal a failure with a known exit code."""

    def __init__(self, message: str, code: int = FAILED, hint: str | None = None):
        super().__init__(message)
        self.message = message
        self.code = code
        self.hint = hint
