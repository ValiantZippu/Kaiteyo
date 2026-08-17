# Windows Distribution

Supported architecture: **x64**. Windows 10 21H2+ (64-bit).

> **arm64 note:** Windows arm64 is **not built** (no CI runner, no artifact).
> The download page must not claim it. See [README.md](README.md) matrix — the
> only honest status is ❌ not built. When an arm64 build is added, it must be a
> real arm64 artifact produced and tested by CI, never an x64 build relabeled.

## Formats

| Format | Purpose | Build |
|---|---|---|
| **EXE** (Inno Setup 6) | Primary consumer installer — install/upgrade/repair/modify, silent mode, Start Menu + desktop tasks, launch checkbox | `powershell -File installer/windows/build.ps1 -Version <v>` |
| **MSI** (jpackage) | Enterprise / Programs & Features / group policy | `./gradlew :desktopApp:packageMsi` |
| **portable ZIP** | No-install self-contained copy | `powershell -File installer/windows/portable/build-portable.ps1 -Version <v>` |

The EXE is the flagship. MSI exists for enterprise audiences that manage
software via MSI; portable exists for users who want no installer at all. We do
**not** ship MSIX yet — it is evaluated but not built (see `installer/docs/ARCHITECTURE.md`).

## Install location & data

| Kind | Where |
|---|---|
| Application | `%ProgramFiles%\Kaiteyo` (user-selectable during install) |
| User data | `%LOCALAPPDATA%\Kaiteyo` (and `~/.kaiteyo`) — **never co-located with the app** |
| Cache / logs | Under the user data dir |

Because app code and user data live in different trees, upgrades never touch
user content, and the uninstaller can remove the app while keeping data
(see [uninstall.md](uninstall.md)).

## Shortcuts

- **Start Menu**: real application entry — Kaiteyo icon, name, and uninstall
  access (Inno creates the standard uninstall entry).
- **Desktop shortcut**: offered as an optional task, **off by default** — no
  desktop clutter without permission.
- Both are user-selectable tasks (`Tasks` in `kaiteyo.iss`), individually
  controllable in silent mode.

## Silent install

Package managers and enterprise deployments need unattended install:

```
Kaiteyo-Setup-<v>.exe /VERYSILENT /SUPPRESSMSGBOXES /NORESTART
# plus /TASKS="..." to pick specific tasks (e.g. desktopicon, startmenu)
```

`/VERYSILENT` shows no UI and exits 0 on success. Component/task selection is
individual (`/COMPONENTS`, `/TASKS`) so package-manager wrappers can pin a
specific surface. Documented in `installer/docs/BUILD.md`.

## Package managers

Kaiteyo provides manifests for the three mainstream Windows managers — WinGet,
Chocolatey and Scoop — under `installer/windows/packaging/`. These are
**defined but not yet published** (publishing requires maintainer accounts on
each community repository). See [windows-package-managers.md](windows-package-managers.md).

| Manager | Manifest | Install | Upgrade | Uninstall |
|---|---|---|---|---|
| WinGet | `winget/` | `winget install kaiteyo` | `winget upgrade kaiteyo` | `winget uninstall kaiteyo` |
| Chocolatey | `chocolatey/` | `choco install kaiteyo` | `choco upgrade kaiteyo` | `choco uninstall kaiteyo` |
| Scoop | `scoop/` | `scoop install kaiteyo` | `scoop update kaiteyo` | `scoop uninstall kaiteyo` |

All three wrap the same signed EXE silent install, so behavior is identical to
a manual install (same AppId, same data location, same uninstall path).

## Signing & SmartScreen

- The EXE is signed with signtool + RFC 3161 timestamp when signing secrets are
  configured (see [signing.md](signing.md)).
- With an EV cert, SmartScreen shows the publisher. With OV/no cert, expect
  "Unknown publisher" — this is documented, never hidden, and the release
  notes call out unsigned builds honestly.
- Signing does **not** magically remove every warning; behavior is tested on a
  clean machine before release (see [troubleshooting.md](troubleshooting.md)).

## Uninstall

The Inno uninstaller is real: it removes application files and the Start
Menu/desktop entries, then **explicitly asks** whether to keep study data
(default) or remove it — with a plain-language list of what each choice deletes.
Never does it silently delete decks, databases, settings, study history or
media. See [uninstall.md](uninstall.md).
