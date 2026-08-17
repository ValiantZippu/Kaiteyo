#!/usr/bin/env python3
"""Audit the built website for broken internal links and missing assets.

Checks every href/src in every rendered HTML page in dist/ (including
fragment targets where possible), reporting any link that cannot resolve
to a file on disk when the base path prefix is stripped.

Usage:  python scripts/audit_links.py [path-to-dist]
"""

from __future__ import annotations

import json
import pathlib
import re
import sys
from html.parser import HTMLParser

ROOT = pathlib.Path(__file__).resolve().parent.parent
DIST = ROOT / "dist"

BASE_PATH = "/Kaiteyo/"
config = ROOT / "config" / "site.json"
if config.is_file():
    BASE_PATH = json.loads(config.read_text(encoding="utf-8")).get("basePath", BASE_PATH)


class LinkCollector(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.refs: list[tuple[str, str]] = []  # (attr, value)

    def handle_starttag(self, tag: str, attrs) -> None:
        for name, value in attrs:
            if name in ("href", "src") and value:
                self.refs.append((name, value))


def resolve(path: str) -> pathlib.Path | None:
    """Map a site URL to a file under dist/ (or None for external/anchors)."""
    if path.startswith(("http://", "https://", "mailto:", "tel:", "data:", "javascript:")):
        return None
    if path.startswith("#"):
        return None
    if path.startswith("//"):
        return None
    url = path.split("#", 1)[0].split("?", 1)[0]
    if url.startswith(BASE_PATH):
        url = url[len(BASE_PATH) - 1:]
    elif url == BASE_PATH.rstrip("/"):
        url = "/"
    url = url.lstrip("/")
    candidate = DIST / url
    if candidate.is_file():
        return candidate
    if candidate.is_dir():
        index = candidate / "index.html"
        return index if index.is_file() else candidate
    return None


def main() -> None:
    target = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else DIST
    html_files = sorted(target.rglob("*.html"))
    external = 0
    anchors = 0
    missing: list[tuple[str, str, str]] = []
    checked = 0
    for file in html_files:
        parser = LinkCollector()
        parser.feed(file.read_text(encoding="utf-8"))
        for attr, value in parser.refs:
            if value.startswith(("http://", "https://", "mailto:", "tel:", "data:", "javascript:", "#", "//")):
                if value.startswith("#"):
                    anchors += 1
                else:
                    external += 1
                continue
            checked += 1
            resolved = resolve(value)
            if resolved is None:
                missing.append((str(file.relative_to(target)), attr, value))
            elif resolved.is_dir():
                missing.append((str(file.relative_to(target)), attr, value + "  [dir, no index.html]"))
    print(f"pages: {len(html_files)} | internal links checked: {checked} | external: {external} | anchors: {anchors}")
    if missing:
        print(f"\nBROKEN ({len(missing)}):")
        for page, attr, value in missing:
            print(f"  {page}: {attr}=\"{value}\"")
        sys.exit(1)
    print("No broken internal links or missing assets.")


if __name__ == "__main__":
    main()
