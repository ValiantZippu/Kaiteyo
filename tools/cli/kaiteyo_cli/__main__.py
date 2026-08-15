"""Allow `python -m kaiteyo_cli` from the tools/cli directory."""

from .app import main

raise SystemExit(main())
