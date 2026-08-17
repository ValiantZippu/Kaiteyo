#!/usr/bin/env python3
"""Local preview server for the built Kaiteyo website.

The site is built for a base path (/Kaiteyo/ by default, matching GitHub
Pages). Assets and internal links are all prefixed with that base path, so
serving dist/ from the root yields a correctly styled site but broken
asset URLs. This server rewrites the base-path prefix to the dist root,
making the local preview behave exactly like the deployed site.

Usage:
    python build.py              # build dist/ first
    python scripts/serve.py      # serve http://localhost:8000/Kaiteyo/
    python scripts/serve.py 9000 # custom port
"""

from __future__ import annotations

import json
import pathlib
import sys
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

ROOT = pathlib.Path(__file__).resolve().parent.parent
DIST = ROOT / "dist"
CONFIG = ROOT / "config" / "site.json"

BASE_PATH = "/Kaiteyo/"
if CONFIG.is_file():
    BASE_PATH = json.loads(CONFIG.read_text(encoding="utf-8")).get("basePath", BASE_PATH)
if not BASE_PATH.startswith("/"):
    BASE_PATH = "/" + BASE_PATH


class BasePathHandler(SimpleHTTPRequestHandler):
    """Serve dist/ while treating BASE_PATH as the site root."""

    def translate_path(self, path: str) -> str:
        if path.startswith(BASE_PATH):
            path = path[len(BASE_PATH) - 1:]  # keep the leading slash
        elif path == BASE_PATH.rstrip("/"):
            path = "/"
        return super().translate_path(path)

    def log_message(self, fmt: str, *args) -> None:
        sys.stderr.write("%s - - [%s] %s\n" % (self.address_string(), self.log_date_time_string(), fmt % args))


def main() -> None:
    if not DIST.is_dir():
        sys.exit(f"dist/ not found at {DIST} — run `python build.py` first.")
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
    handler = partial(BasePathHandler, directory=str(DIST))
    server = ThreadingHTTPServer(("127.0.0.1", port), handler)
    print(f"Serving {DIST} at http://127.0.0.1:{port}{BASE_PATH} (base path: {BASE_PATH})")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()
