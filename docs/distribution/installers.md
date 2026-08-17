# Installer Behavior

How Kaiteyo's installers behave across platforms — the shared contract every
platform implementation honors.

## Installer → application handoff

- The installer **never force-launches** the app. It offers a "Launch Kaiteyo"
  checkbox/task (on by default on Windows, user-selectable), and only launches
  when the user accepts.
- On the first launch after install, the app shows its own one-time onboarding
  wizard (see [onboarding.md](onboarding.md)).

## Silent install

- **Windows**: `Kaiteyo-Setup-<v>.exe /VERYSILENT /SUPPRESSMSGBOXES /NORESTART`
  (tasks selectable via `/TASKS`). This is what the WinGet/Chocolatey/Scoop
  manifests use.
- **Linux**: package managers are silent by nature (`apt install -y`,
  `dnf install -y`, `flatpak install -y`); the AppImage needs no installer at
  all.
- **Android**: installs come from the store or sideload; no silent UI.

## Error handling (user-facing)

Ordinary users must **never** see stack traces, Gradle errors, or internal
paths. Installer failures show:

- A clear message ("Installation failed"), a Retry action where retrying is
  meaningful (e.g. transient network/download failure), and a Cancel action.
- "Details available" — the technical detail goes to a **log file**, not the
  dialog.
- Windows specifics: insufficient-rights → offer re-launch elevated; disk-space
  check before install; corrupt payload → checksum mismatch page (verify, then
  retry); existing-install conflict → upgrade vs. separate choice.

## Interrupted install / update

- **Never leave a broken install.** If the installer is interrupted, the
  previous working installation is preserved where feasible; a partially
  written app is never reported as installed.
- **Updates are atomic**: download → verify (sha256) → stage → replace →
  restart. A working app is never overwritten byte-by-byte from an unverified
  download (see [updates.md](updates.md)).
- Windows: the Inno upgrade path only replaces `{app}` after the new payload is
  fully staged and checksum-verified.

## Logging

- Installer logs are **separate** from application logs: Windows
  `%TEMP%\Kaiteyo Setup Log*.txt` (Inno), Linux package-manager logs, etc.
- Logs contain technical detail, never passwords/tokens/keys or user content.
- No installer telemetry — installation is local unless the user explicitly
  opts into a network service (see [security.md](security.md)).

## Crash recovery

If Kaiteyo crashes immediately after installation:

- **Safe mode** — `kaiteyo --safe-mode` disables custom themes, GPU effects,
  plugins and optional integrations so the app can start on problem machines
  (see [first-launch.md](first-launch.md) and the CLI docs).
- UI state resets and "open logs" are reachable from the app; user data is
  never auto-deleted as a "fix".

## Retry

Network failures during install support retry without restarting the whole
installer. The Inno progress page is driven by a real task list, so a failed
step can be re-run; download failures surface a Retry button.

## Localization

Installer strings are prepared for localization (English + Japanese at
minimum) — no hardcoded copy buried in layout code. Windows uses Inno's
language system; see [localization](faq.md#localization) notes.
