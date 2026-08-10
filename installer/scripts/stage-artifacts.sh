#!/usr/bin/env bash
# ============================================================
# Kaiteyo — stage release artifacts
#
# Collects every build output into release/kaiteyo-<version>/
# with canonical names, computes sha256 checksums, and writes
# artifact-manifest.json (schema: common/artifact-manifest.schema.json).
#
# Usage:
#   stage-artifacts.sh <version> [release-dir] [--from <source-root>]
#
#   version      e.g. 2.2.1
#   release-dir  default: release/kaiteyo-<version>
#   --from dir   scan <dir> instead of the repo root. CI passes the
#                downloaded artifacts dir; the same globs work for both.
#
# Required artifacts (missing = FAIL): windows-exe, macos-arm64/x64 dmg,
# linux appimage/deb/rpm. Optional (missing = warn): msi, portable zip,
# flatpak, apk.
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VERSION="${1:?usage: stage-artifacts.sh <version> [release-dir] [--from <source-root>]}"
RELEASE_DIR="${2:-$ROOT/release/kaiteyo-$VERSION}"
SOURCE_ROOT="$ROOT"

if [[ "${3:-}" == "--from" ]]; then
  SOURCE_ROOT="${4:?--from requires a directory}"
fi

mkdir -p "$RELEASE_DIR"

declare -a ENTRIES=()
declare -a MISSING=()

stage() { # stage <key> <arch> <glob> <dest-name> <required:true|false> [signed]
  local key="$1" arch="$2" glob="$3" dest="$4" required="$5" signed="${6:-false}"
  local found
  found=$(find "$SOURCE_ROOT" -path "$glob" -type f 2>/dev/null | head -n1 || true)
  if [[ -z "$found" ]]; then
    if [[ "$required" == "true" ]]; then
      MISSING+=("$key")
      echo "  !! MISSING required: $key ($glob)"
    else
      echo "  !! skipping optional: $key ($glob)"
    fi
    return 0
  fi
  cp "$found" "$RELEASE_DIR/$dest"
  local sha size
  sha=$(sha256sum "$RELEASE_DIR/$dest" | cut -d' ' -f1)
  size=$(stat -c%s "$RELEASE_DIR/$dest")
  ENTRIES+=("{\"key\":\"$key\",\"file\":\"$dest\",\"sha256\":\"$sha\",\"size_bytes\":$size,\"arch\":\"$arch\",\"signed\":$signed}")
  echo "  + $dest ($([ "$signed" = true ] && echo signed || echo unsigned), $size bytes)"
}

echo "== Staging Kaiteyo $VERSION -> ${RELEASE_DIR#$ROOT/} (from ${SOURCE_ROOT#$ROOT/}) =="

# Windows
stage windows-exe      x64    "**/Kaiteyo-Setup-*.exe"            "Kaiteyo-$VERSION-windows-setup.exe"  true  true
stage windows-msi      x64    "**/*.msi"                          "Kaiteyo-$VERSION-windows.msi"         false true
stage windows-portable x64    "**/*Portable*.zip"                 "Kaiteyo-$VERSION-windows-portable.zip" false false

# macOS (canonical arch suffixes: arm64 / x64)
stage macos-arm   arm64 "**/*-arm64.dmg" "Kaiteyo-$VERSION-macos-arm64.dmg" true  true
stage macos-intel x64   "**/*-x64.dmg"   "Kaiteyo-$VERSION-macos-x64.dmg"   true  true

# Linux
stage linux-appimage x86_64 "**/Kaiteyo-*.AppImage" "Kaiteyo-$VERSION-linux.AppImage" true  false
stage linux-deb      amd64  "**/*.deb"             "Kaiteyo-$VERSION-linux.deb"      true  false
stage linux-rpm      x86_64 "**/*.rpm"             "Kaiteyo-$VERSION-linux.rpm"      true  false
stage linux-flatpak  x86_64 "**/*.flatpak"         "Kaiteyo-$VERSION-linux.flatpak"  false false

# Android (optional — not every release ships an APK)
stage android-apk    arm64  "**/*.apk"             "Kaiteyo-$VERSION-android.apk"    false true

if [[ ${#MISSING[@]} -gt 0 ]]; then
  echo "== FAIL: required artifacts missing: ${MISSING[*]} ==" >&2
  exit 1
fi

# Manifest
{
  echo "{"
  echo "  \"schema_version\": 1,"
  echo "  \"version\": \"$VERSION\","
  echo "  \"version_code\": $(jq -r '.version_code // 0' "$ROOT/installer/common/version.json" 2>/dev/null || echo 0),"
  echo "  \"artifacts\": ["
  if [[ ${#ENTRIES[@]} -gt 0 ]]; then
    printf '    %s,\n' "${ENTRIES[@]}" | sed '$ s/,$//'
  fi
  echo "  ]"
  echo "}"
} > "$RELEASE_DIR/artifact-manifest.json"

echo "== Wrote artifact-manifest.json ($((${#ENTRIES[@]} + 1)) entries) =="
ls -1 "$RELEASE_DIR"
