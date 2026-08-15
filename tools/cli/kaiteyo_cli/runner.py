"""Safe command execution.

Every external process the CLI starts goes through this module so the CLI can:

  - stream output live and preserve child colors,
  - mask secrets in whatever is displayed,
  - time the operation and report elapsed time,
  - return the real exit code (child errors are never swallowed),
  - raise a helpful CliError when an executable is missing.
"""

from __future__ import annotations

import os
import subprocess
import sys
import time
from dataclasses import dataclass, field

from .errors import CliError, ENV, FAILED
from .secrets import mask_text


@dataclass
class RunResult:
    exit_code: int = 0
    elapsed: float = 0.0
    output: str = ""
    timed_out: bool = False


def _env_merge(extra: dict[str, str] | None) -> dict[str, str] | None:
    if not extra:
        return None
    env = dict(os.environ)
    env.update(extra)
    return env


def _find(cmd: list[str]) -> str:
    """First element of the command; used for missing-executable errors."""
    return cmd[0] if cmd else ""


# Bounded default so a stalled child (e.g. `git status` over a slow network
# filesystem) can never hang the CLI silently. Commands that need more time
# (explicit git workflows) pass a larger timeout; snapshot/diagnostic commands
# pass a smaller one and degrade gracefully on timeout.
DEFAULT_TIMEOUT = 120.0  # seconds

def run_capture(cmd: list[str], cwd: str | None = None, env: dict[str, str] | None = None,
                timeout: float | None = None) -> RunResult:
    """Run a command, capturing its combined output. Never displays anything.

    When ``timeout`` is omitted the child is bounded by [DEFAULT_TIMEOUT]; a
    timed-out run returns ``RunResult(timed_out=True, exit_code=124)`` with
    whatever output was produced before the deadline instead of hanging.
    """
    start = time.monotonic()
    try:
        proc = subprocess.run(
            cmd, cwd=cwd, env=_env_merge(env), capture_output=True, text=True,
            timeout=DEFAULT_TIMEOUT if timeout is None else timeout, errors="replace",
        )
        output = (proc.stdout or "") + (proc.stderr or "")
    except FileNotFoundError:
        raise CliError(f"Executable not found: {_find(cmd)}", code=ENV,
                       hint=f"Is it installed and on PATH? Check with `kaiteyo doctor`.")
    except subprocess.TimeoutExpired as exc:  # pragma: no cover - timing dependent
        output = (exc.stdout or "") + (exc.stderr or "") if isinstance(exc.stdout, str) else ""
        return RunResult(exit_code=124, elapsed=time.monotonic() - start, output=output, timed_out=True)
    return RunResult(exit_code=proc.returncode, elapsed=time.monotonic() - start, output=output)


def run_stream(cmd: list[str], cwd: str | None = None, env: dict[str, str] | None = None,
               echo: bool = True, prefix: str = "") -> RunResult:
    """Run a command, streaming its output line-by-line (masked) to stdout.

    `echo` prints the command line first (masked). Returns the child's exit
    code — child failures are never converted into success.
    """
    if echo:
        shown = mask_text(" ".join(cmd))
        print(f"{prefix}{shown}")
    start = time.monotonic()
    try:
        proc = subprocess.Popen(
            cmd, cwd=cwd, env=_env_merge(env),
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace",
        )
    except FileNotFoundError:
        raise CliError(f"Executable not found: {_find(cmd)}", code=ENV,
                       hint=f"Is it installed and on PATH? Check with `kaiteyo doctor`.")
    captured: list[str] = []
    assert proc.stdout is not None
    for line in proc.stdout:
        captured.append(line)
        print(mask_text(line), end="", flush=True)
    proc.wait()
    return RunResult(exit_code=proc.returncode, elapsed=time.monotonic() - start,
                     output="".join(captured))


def run_capture_bytes(cmd: list[str], cwd: str | None = None,
                      env: dict[str, str] | None = None, timeout: float | None = None):
    """Run a command and return (exit_code, raw_bytes, elapsed).

    Used for tools that emit non-UTF-8 encodings (e.g. wsl.exe prints UTF-16
    when its output is piped). Bounded by [DEFAULT_TIMEOUT] when ``timeout``
    is omitted; a timeout returns (124, partial_bytes, elapsed).
    """
    start = time.monotonic()
    try:
        proc = subprocess.run(cmd, cwd=cwd, env=_env_merge(env), capture_output=True,
                              timeout=DEFAULT_TIMEOUT if timeout is None else timeout)
        raw = (proc.stdout or b"") + (proc.stderr or b"")
        return proc.returncode, raw, time.monotonic() - start
    except FileNotFoundError:
        raise CliError(f"Executable not found: {_find(cmd)}", code=ENV,
                       hint=f"Is it installed and on PATH? Check with `kaiteyo doctor`.")
    except subprocess.TimeoutExpired as exc:  # pragma: no cover
        return 124, exc.stdout or b"", time.monotonic() - start


def run_interactive(cmd: list[str], cwd: str | None = None, env: dict[str, str] | None = None,
                    echo: bool = True) -> RunResult:
    """Run a command with the terminal passed through (shells, editors, tail -f)."""
    if echo:
        print(mask_text(" ".join(cmd)))
    start = time.monotonic()
    try:
        proc = subprocess.Popen(cmd, cwd=cwd, env=_env_merge(env))
    except FileNotFoundError:
        raise CliError(f"Executable not found: {_find(cmd)}", code=ENV,
                       hint=f"Is it installed and on PATH? Check with `kaiteyo doctor`.")
    proc.wait()
    return RunResult(exit_code=proc.returncode, elapsed=time.monotonic() - start)


def check_found(executable: str, label: str | None = None, hint: str | None = None) -> None:
    """Raise a friendly environment error when an executable is missing."""
    import shutil

    if not shutil.which(executable):
        name = label or executable
        raise CliError(f"{name} was not found on PATH.", code=ENV, hint=hint)


def fail_result(result: RunResult, label: str = "command") -> CliError:
    return CliError(f"{label} failed with exit code {result.exit_code}.", code=FAILED,
                    hint="See the output above for the underlying error.")
