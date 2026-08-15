"""Platform detection: OS, architecture, WSL, shells, and tool lookup.

The CLI runs on Windows, macOS, Linux and inside WSL. Nothing here assumes
bash exists on Windows or that PowerShell exists on Linux.
"""

from __future__ import annotations

import os
import platform
import shutil
import sys

WINDOWS = "windows"
MACOS = "macos"
LINUX = "linux"
OTHER = "other"


def os_name() -> str:
    system = platform.system().lower()
    if system.startswith("win"):
        return WINDOWS
    if system == "darwin":
        return MACOS
    if system == "linux":
        return LINUX
    return OTHER


def is_windows() -> bool:
    return os_name() == WINDOWS


def is_macos() -> bool:
    return os_name() == MACOS


def is_linux() -> bool:
    return os_name() == LINUX


def arch() -> str:
    machine = platform.machine().lower()
    return {"amd64": "x86_64", "x86_64": "x86_64", "arm64": "arm64", "aarch64": "arm64"}.get(
        machine, machine
    )


def in_wsl() -> bool:
    """True when running inside a WSL distribution (Linux guest)."""
    if os.environ.get("WSL_DISTRO_NAME") or os.environ.get("WSL_INTEROP"):
        return True
    if is_linux():
        try:
            with open("/proc/version", encoding="utf-8", errors="replace") as f:
                return "microsoft" in f.read().lower()
        except OSError:
            return False
    return False


def wsl_distro() -> str | None:
    """Name of the current WSL distribution when running inside one."""
    return os.environ.get("WSL_DISTRO_NAME") or None


def wsl_exe() -> str | None:
    """Path to wsl.exe when it can be invoked from this environment."""
    if is_windows():
        return shutil.which("wsl") or shutil.which("wsl.exe")
    if in_wsl():
        # Inside a distro, wsl.exe lives under /mnt/c.
        candidates = ["wsl.exe", "/mnt/c/Windows/System32/wsl.exe"]
        for c in candidates:
            if shutil.which(c):
                return shutil.which(c)
        return None
    return None


def which(name: str) -> str | None:
    return shutil.which(name)


def shell_name() -> str:
    if is_windows():
        return "powershell" if which("powershell") else "cmd"
    return os.environ.get("SHELL", "sh")


def preferred_shell() -> list[str]:
    """The default shell to use for interactive shell-ish commands."""
    if is_windows():
        if which("powershell"):
            return ["powershell", "-NoProfile"]
        return ["cmd.exe", "/c"]
    shell = os.environ.get("SHELL") or shutil.which("bash") or "sh"
    return [shell, "-l"]


def opener_cmd(path: str) -> list[str] | None:
    """Command that opens `path` with the platform default application."""
    if is_windows():
        return ["cmd.exe", "/c", "start", "", path]
    if is_macos():
        return ["open", path]
    for opener in ("xdg-open", "gio", "open"):
        exe = which(opener)
        if exe:
            return [exe, path]
    return None


def python_executable() -> str:
    return sys.executable


def path_env() -> str:
    return os.environ.get("PATH", "")
