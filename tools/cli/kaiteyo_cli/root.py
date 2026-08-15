"""Project root detection.

The CLI locates the Kaiteyo repository by walking up from the current
directory looking for repository markers. It never hardcodes a path.

Markers considered (in decreasing confidence):

    .git                     any git repository
    settings.gradle.kts      Gradle settings (Kotlin DSL)
    settings.gradle          Gradle settings (Groovy DSL)
    gradlew / gradlew.bat    Gradle wrapper
    build.gradle.kts         Gradle build
    build.gradle             Gradle build

A directory qualifies as the root when it contains `.git` plus at least one
Gradle marker, or `.git` alone with no Gradle markers anywhere above it (a
plain git repo is still usable for the git commands).
"""

from __future__ import annotations

import os
import pathlib

GIT_MARKER = ".git"
GRADLE_MARKERS = (
    "settings.gradle.kts",
    "settings.gradle",
    "gradlew",
    "gradlew.bat",
    "build.gradle.kts",
    "build.gradle",
)

ALL_MARKERS = (GIT_MARKER,) + GRADLE_MARKERS


def find_root(start: str | os.PathLike | None = None) -> pathlib.Path | None:
    """Walk up from `start` (default: cwd) and return the first project root."""
    current = pathlib.Path(start or os.getcwd()).resolve()
    if not current.exists():
        return None
    for candidate in (current, *current.parents):
        has_git = (candidate / GIT_MARKER).exists()
        gradle = [m for m in GRADLE_MARKERS if (candidate / m).exists()]
        if has_git and gradle:
            return candidate
        # A bare git repo with no Gradle markers anywhere above it.
        if has_git and not _gradle_above(candidate):
            return candidate
    return None


def _gradle_above(path: pathlib.Path) -> bool:
    for parent in path.parents:
        for m in GRADLE_MARKERS:
            if (parent / m).exists():
                return True
    return False


def is_root(root: str | os.PathLike) -> bool:
    """Is this directory itself a project root?"""
    path = pathlib.Path(root)
    has_git = (path / GIT_MARKER).exists()
    gradle = any((path / m).exists() for m in GRADLE_MARKERS)
    return has_git and gradle
