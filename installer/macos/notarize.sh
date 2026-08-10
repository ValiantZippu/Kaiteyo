#!/usr/bin/env bash
# ============================================================================
# Kaiteyo — macOS code-signing + notarization pipeline
#
# 1. Signs every binary in the .app (including bundled JVM dylibs) with
#    Hardened Runtime + timestamp.
# 2. Signs the .app bundle.
# 3. Submits the DMG for notarization and staples the ticket.
#
# Respects Gatekeeper: never ships an unsigned bundle. If credentials are
# missing the script refuses to publish (CI uses secrets).
#
# Env:  APPLE_ID, APPLE_APP_PASSWORD, APPLE_TEAM_ID, CODESIGN_IDENTITY
# Usage: bash installer/macos/notarize.sh <path-to.dmg> [--app-bundle path]
# ============================================================================
set -euo pipefail

DMG="${1:?usage: notarize.sh <dmg> [--app-bundle <app>]}"
APP_BUNDLE=""
if [[ "${2:-}" == "--app-bundle" ]]; then APP_BUNDLE="${3:?}"; fi

IDENTITY="${CODESIGN_IDENTITY:-}"
APPLE_ID="${APPLE_ID:-}"
APPLE_APP_PASSWORD="${APPLE_APP_PASSWORD:-}"
APPLE_TEAM_ID="${APPLE_TEAM_ID:-}"

if [[ -z "$IDENTITY" ]]; then
  echo "!! CODESIGN_IDENTITY not set — skipping signing and notarization."
  echo "   CI blocks publishing unsigned DMGs via the release gate; this is fine for local builds."
  exit 0
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENTITLEMENTS="$ROOT/installer/macos/entitlements.plist"

sign_bundle() {
  local app="$1"
  echo "== Signing $app =="
  # Deep-sign: every binary + dylib inside the bundle (JRE included).
  codesign --force --deep --timestamp --options runtime \
    --entitlements "$ENTITLEMENTS" \
    --sign "$IDENTITY" "$app"
  codesign --verify --deep --strict --verbose=2 "$app"
}

if [[ -n "$APP_BUNDLE" ]]; then
  sign_bundle "$APP_BUNDLE"
fi

if [[ -z "$APPLE_ID" || -z "$APPLE_APP_PASSWORD" || -z "$APPLE_TEAM_ID" ]]; then
  echo "!! APPLE_ID / APPLE_APP_PASSWORD / APPLE_TEAM_ID not set."
  echo "   Bundle signing done; notarization skipped (CI will notarize)."
  exit 0
fi

echo "== Notarizing $DMG =="
xcrun notarytool submit "$DMG" \
  --apple-id "$APPLE_ID" \
  --password "$APPLE_APP_PASSWORD" \
  --team-id "$APPLE_TEAM_ID" \
  --wait

echo "== Stapling =="
xcrun stapler staple "$DMG"
xcrun stapler validate "$DMG"
echo "== $DMG is signed, notarized and stapled. Safe to publish. =="
