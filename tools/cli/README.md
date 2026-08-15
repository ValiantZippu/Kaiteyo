# Kaiteyo CLI

The Kaiteyo developer command center: git workflows, Gradle task control, WSL
utilities, diagnostics, docs and file browsing — one cross-platform tool.

This directory contains the CLI itself. **Documentation lives in
[`docs/cli/`](../../docs/cli/README.md)** — overview, command reference,
configuration, automation, architecture and troubleshooting.

## Quick start

```bash
# From the repository root
./kaiteyo --help              # or kaiteyo.cmd on Windows

# From anywhere (add tools/cli/bin to PATH)
tools/cli/bin/kaiteyo --help

# Via pip (optional)
pip install -e tools/cli
```

## Layout

```text
bin/             launchers (POSIX + Windows)
pyproject.toml   optional packaging
kaiteyo_cli/     the CLI package (app, core modules, commands/)
```

Requires Python 3.9+; standard library only — no dependencies, no build step.
