# Windows

## Supported status

✅ **Supported** — primary development platform. The desktop app is the flagship.

## Build & packaging

```bash
./gradlew :desktopApp:createDistributable        # app bundle
./gradlew :desktopApp:packageMsi                 # MSI (jpackage)
powershell -File installer/windows/build.ps1 -Version 2.2.1   # branded Inno Setup EXE
powershell -File installer/windows/portable/build-portable.ps1 -Version 2.2.1  # portable ZIP
```

Packages: **EXE** (branded Inno Setup 6 wizard — install/upgrade/repair/modify, silent
install, file associations, launch-after-install), **MSI** (jpackage), **portable ZIP**
(self-contained data folder). Signing via `signtool` (see `installer/docs/SIGNING.md`).

Requirements to build: Windows host, JDK 17, Inno Setup 6.4+ for the EXE (CI installs it
via `choco`).

## Platform-specific behavior

- **Native window shell** — custom undecorated window with a 44dp title bar; native OS
  dragging via `WM_NCLBUTTONDOWN`/`HTCAPTION` (JNA); 8-zone resize handles; rounded
  corners flatten when maximized.
- **System media keys** — global `WH_KEYBOARD_LL` hook for Play/Pause/Next/Previous/Stop
  (JNA), active only while media is loaded (opt-in, Settings → Media).
- **Tray notifications** — playback/mining notifications (opt-in).
- **VLC / mpv** — detected if installed; Java Sound works out of the box for audio.

## File system & permissions

| Item | Location |
|---|---|
| Study data / desktop suite state | `%LOCALAPPDATA%\Kaiteyo` and `~/.kaiteyo` (home dir) |
| Installer | Program Files (user-selected during install) |
| Uninstall | Preserves `%LOCALAPPDATA%\Kaiteyo`; the uninstaller asks before removing study data |

No elevated permissions are required to run the app. The installer may request elevation
to write to Program Files.

## Input

- Full keyboard support: review shortcuts (`1–4`, `Space`, `B`, `S`, `R`, `Ctrl+Enter`,
  `Ctrl+Z`), command palette, global shortcuts (configurable).
- Mouse: hover-driven popups (dictionary), drag-resize windows/panels.
- Touch/pen: supported through Compose; pen pressure feeds the brush engine when the
  hardware provides it.

## Known limitations

- System media keys hook requires JNA/JNI at runtime (bundled).
- VLC backend needs a VLC install with `libvlc`; otherwise the app falls back.
- iOS targets cannot be built on Windows (expected).
