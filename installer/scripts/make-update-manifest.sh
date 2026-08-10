#!/usr/bin/env bash
# ============================================================
# Kaiteyo — generate the update feed manifest
#
# Reads a staged release directory (see stage-artifacts.sh) and
# emits an update manifest for the requested channel, matching
# common/update-manifest.schema.json. The desktop app consumes
# this to discover, verify and apply updates.
#
# Usage: bash installer/scripts/make-update-manifest.sh <version> <channel> [base-url]
#   version   e.g. 2.2.1
#   channel   stable | beta | nightly
#   base-url  where artifacts will be served from (default:
#             the GitHub latest-download URL from version.json)
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VERSION="${1:?usage: make-update-manifest.sh <version> <channel> [base-url]}"
CHANNEL="${2:?usage: make-update-manifest.sh <version> <channel> [base-url]}"
[[ "$CHANNEL" =~ ^(stable|beta|nightly)$ ]] || { echo "bad channel: $CHANNEL" >&2; exit 1; }

RELEASE_DIR="$ROOT/release/kaiteyo-$VERSION"
MANIFEST="$ROOT/installer/common/update-$CHANNEL.json"

[[ -f "$RELEASE_DIR/artifact-manifest.json" ]] || {
  echo "error: no artifact-manifest.json in $RELEASE_DIR — run stage-artifacts.sh first" >&2
  exit 1
}

BASE_URL="${3:-$(jq -r '.update_feed_base' "$ROOT/installer/common/version.json")}"

VERSION_CODE=$(jq -r '.version_code' "$RELEASE_DIR/artifact-manifest.json")
PUBLISHED=$(date -u +%Y-%m-%dT%H:%M:%SZ)
# Release notes link to the GitHub release page (not an asset on the feed
# release — those only carry the channel manifests).
REPO_URL=$(jq -r '.homepage' "$ROOT/installer/common/version.json")
NOTES_URL="${REPO_URL%/}/releases/tag/v$VERSION"

{
  echo "{"
  echo "  \"schema_version\": 1,"
  echo "  \"channel\": \"$CHANNEL\","
  echo "  \"published_at\": \"$PUBLISHED\","
  echo "  \"latest\": {"
  echo "    \"version\": \"$VERSION\","
  echo "    \"version_code\": $VERSION_CODE,"
  echo "    \"release_notes_url\": \"$NOTES_URL\","
  echo "    \"min_app_version\": \"$VERSION\""
  echo "  },"
  echo "  \"artifacts\": {"
  jq -r '.artifacts[] | "\"\(.key)\": {\"url\": \"'"$BASE_URL"'/\(.file)\", \"sha256\": \"\(.sha256)\", \"size_bytes\": \(.size_bytes), \"arch\": \"\(.arch)\"},"' \
    "$RELEASE_DIR/artifact-manifest.json" | sed '$ s/,$//'
  echo "  }"
  echo "}"
} > "$MANIFEST"

echo "Wrote $MANIFEST"
echo "  channel=$CHANNEL version=$VERSION code=$VERSION_CODE published=$PUBLISHED"
echo "  artifacts: $(jq '.artifacts | keys | length' "$MANIFEST")"
