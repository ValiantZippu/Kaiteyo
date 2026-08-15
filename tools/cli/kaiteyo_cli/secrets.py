"""Secret safety: mask sensitive values in anything we display.

GitHub tokens, passwords, API keys and private keys must never be printed.
This module masks:

  - KEY=VALUE / KEY: VALUE assignments for known secret names
  - Authorization / Bearer headers
  - credentials embedded in URLs (https://user:pass@host/...)
"""

from __future__ import annotations

import re

_SECRET_KEY = re.compile(
    r"(?i)\b(?:token|secret|password|passwd|pass|api[_-]?key|apikey|auth(?:orization)?|"
    r"private[_-]?key|access[_-]?key|client[_-]?secret|bearer|session[_-]?id|cookie)\b"
)

_ASSIGN = re.compile(
    r"(?i)(\b(?:token|secret|password|passwd|pass|api[_-]?key|apikey|private[_-]?key|"
    r"access[_-]?key|client[_-]?secret)\b\s*[=:]\s*)(['\"]?)([^\s'\";,&]+)\2?"
)

_AUTH_HEADER = re.compile(r"(?i)(authorization\s*:\s*(?:bearer\s+|basic\s+)?)(\S+)")

_URL_USERINFO = re.compile(r"://([^/@\s]+)@")

_SECRET_VALUE = re.compile(r"(?i)(gh[pousr]_[A-Za-z0-9]{20,}|(?:sk|rk)-[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16})")


def mask_text(text: str) -> str:
    """Return `text` with any recognizable secret values replaced by '***'."""
    masked = _AUTH_HEADER.sub(lambda m: m.group(1) + "***", text)
    masked = _ASSIGN.sub(lambda m: f"{m.group(1)}{m.group(2)}***{m.group(2)}", masked)
    masked = _SECRET_VALUE.sub("***", masked)
    return masked


def mask_url(url: str) -> str:
    """Mask credentials embedded in a URL (e.g. https://user:TOKEN@host)."""
    return _URL_USERINFO.sub(lambda m: "://***@" if "@" in m.group(1) else m.group(0), url)


def mask_remote(remote: str) -> str:
    """Git remote URLs can embed tokens; never print them in full."""
    remote = mask_url(remote)
    return mask_text(remote)
