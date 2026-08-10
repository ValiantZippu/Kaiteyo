#!/usr/bin/env bash
# ============================================================
# scripts/capture-window-shell.sh
#
# Captures real screenshots of every Kaiteyo launcher state and
# saves them to docs/screenshots/, replacing the schematic
# mockup (window-shell.svg). Uses the app's dev-only capture
# mode (`--capture-state=`) so each state is deterministic.
#
#   ./scripts/capture-window-shell.sh            # all four states
#   ./scripts/capture-window-shell.sh launchpad  # one state
#
# States -> files:
#   shell     docs/screenshots/window-shell.png
#   menu      docs/screenshots/launcher-menu.png
#   launchpad docs/screenshots/launchpad-overlay.png
#   strip     docs/screenshots/launchpad-window-strip.png
#
# The app is launched once per state with a fixed 1200x800
# window, dwells for CAPTURE_DWELL seconds, and exits on its
# own — the script never kills anything. Overrides:
#   KAITEYO_STATES="shell menu"   subset of states to capture
#   STARTUP_DELAY=10              seconds to wait for the window
#   CAPTURE_DWELL=25              seconds the app stays open
#   OUT_DIR=docs/screenshots      where PNGs are written
#
# Requirements per OS:
#   Linux   : xdotool + ImageMagick (`import`)
#   macOS   : built-in screencapture + osascript (System Events)
#   Windows : Git Bash + PowerShell (Win32 GetWindowRect)
# ============================================================
set -euo pipefail

OUT_DIR="${OUT_DIR:-docs/screenshots}"
mkdir -p "$OUT_DIR"

WINDOW_TITLE="${WINDOW_TITLE:-Kaiteyo}"
STARTUP_DELAY="${STARTUP_DELAY:-10}"
CAPTURE_DWELL="${CAPTURE_DWELL:-25}"

declare -A STATE_FILE=(
  [shell]="window-shell.png"
  [menu]="launcher-menu.png"
  [launchpad]="launchpad-overlay.png"
  [strip]="launchpad-window-strip.png"
)

# One state given as $1, otherwise all of them.
if [ $# -ge 1 ]; then
  STATES="$1"
else
  STATES="${KAITEYO_STATES:-shell menu launchpad strip}"
fi

case "$(uname -s)" in
  Darwin) OS="macos" ;;
  MINGW*|MSYS*|CYGWIN*) OS="windows" ;;
  Linux) OS="linux" ;;
  *) echo "Unsupported OS: $(uname -s)" >&2; exit 1 ;;
esac

capture_window() { # $1 = output path
  case "$OS" in
    linux)
      WIN_ID="$(xdotool search --name "$WINDOW_TITLE" 2>/dev/null | head -1 || true)"
      if [ -z "$WIN_ID" ]; then
        echo "  !! window '$WINDOW_TITLE' not found" >&2
        return 1
      fi
      import -window "$WIN_ID" "$1"
      ;;
    macos)
      # Bring the JVM's front window forward, then capture it by id.
      osascript -e 'tell application "System Events" to set frontmost of (first process whose name is "java") to true' 2>/dev/null || true
      sleep 1
      WIN_ID="$(osascript -e 'tell application "System Events" to get id of window 1 of (first process whose name is "java")' 2>/dev/null || true)"
      if [ -z "$WIN_ID" ]; then
        echo "  !! java window not found" >&2
        return 1
      fi
      screencapture -o -l"$WIN_ID" "$1"
      ;;
    windows)
      powershell -NoProfile -Command '
        Add-Type @"
using System;
using System.Runtime.InteropServices;
public class KaiteyoCapture {
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT r);
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
}
"@
        $p = Get-Process | Where-Object { $_.MainWindowTitle -like "*'"$WINDOW_TITLE"'*" } | Select-Object -First 1
        if (-not $p) { Write-Error "No window titled '"'"$WINDOW_TITLE"'"' found."; exit 1 }
        [KaiteyoCapture]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
        Start-Sleep -Seconds 1
        $r = New-Object KaiteyoCapture+RECT
        [KaiteyoCapture]::GetWindowRect($p.MainWindowHandle, [ref]$r) | Out-Null
        Add-Type -AssemblyName System.Drawing
        $bmp = New-Object System.Drawing.Bitmap($r.Right - $r.Left, $r.Bottom - $r.Top)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.CopyFromScreen($r.Left, $r.Top, 0, 0, $bmp.Size)
        $bmp.Save("'"$(cygpath -w "$1")"'", [System.Drawing.Imaging.ImageFormat]::Png)
        $g.Dispose(); $bmp.Dispose()
      '
      ;;
  esac
}

for state in $STATES; do
  out="${OUT_DIR}/${STATE_FILE[$state]}"
  echo "== capturing '$state' -> $out"
  ./gradlew :desktopApp:run --quiet \
    --args="--capture-state=$state --capture-dwell=$((CAPTURE_DWELL * 1000))" \
    >/tmp/kaiteyo-capture-$state.log 2>&1 &
  sleep "$STARTUP_DELAY"
  if capture_window "$out"; then
    echo "   saved $out"
  else
    echo "   FAILED — see /tmp/kaiteyo-capture-$state.log" >&2
  fi
  # Wait for the app to exit on its own (dwell), then move on.
  sleep "$CAPTURE_DWELL"
done

echo "Done. When the PNGs look right, delete docs/screenshots/window-shell.svg (the mockup)."
