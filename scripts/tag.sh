#!/usr/bin/env bash
# Kaiteyo Tag System — YYYY.MM.DD-<channel>-v<VERSION>-<CODE>-<SHA>
# Usage: ./scripts/tag.sh [alpha|beta|release] [--push] [--dry-run]
#   channel auto-detected from current branch if not supplied:
#     early-alpha-develop  -> alpha
#     early-beta-develop   -> beta
#     early-release-develop -> release
# Reads version from buildSrc/src/main/kotlin/AppVersion.kt and version.json

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_VERSION_KT="$ROOT_DIR/buildSrc/src/main/kotlin/AppVersion.kt"
CHANNEL_ARG="${1:-}"

DRY_RUN=false
PUSH=false
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=true ;;
    --push) PUSH=true ;;
  esac
done

# Resolve channel
if [[ "$CHANNEL_ARG" == "--"* ]]; then CHANNEL_ARG=""; fi
if [[ -z "${CHANNEL_ARG:-}" || "$CHANNEL_ARG" == "--"* ]]; then
  BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")"
  case "$BRANCH" in
    early-alpha-develop) CHANNEL="alpha" ;;
    early-beta-develop)  CHANNEL="beta" ;;
    early-release-develop) CHANNEL="release" ;;
    main) CHANNEL="stable" ;;
    *) CHANNEL="dev" ;;
  esac
else
  case "$CHANNEL_ARG" in
    alpha|beta|release|stable|rc|dev) CHANNEL="$CHANNEL_ARG" ;;
    early-alpha*) CHANNEL="alpha" ;;
    early-beta*) CHANNEL="beta" ;;
    early-release*) CHANNEL="release" ;;
    *) echo "Unknown channel: $CHANNEL_ARG (use alpha|beta|release|stable)" >&2; exit 1 ;;
  esac
fi

# Read versionName and versionCode from AppVersion.kt
if [[ ! -f "$APP_VERSION_KT" ]]; then
  echo "AppVersion.kt not found at $APP_VERSION_KT" >&2; exit 1
fi
VERSION_NAME="$(grep -E 'const val versionName' "$APP_VERSION_KT" | sed -E 's/.*\"([^\"]+)\".*/\1/')"
VERSION_CODE="$(grep -E 'const val versionCode' "$APP_VERSION_KT" | sed -E 's/.*= *([0-9]+).*/\1/')"
if [[ -z "$VERSION_NAME" || -z "$VERSION_CODE" ]]; then
  echo "Failed to parse version from $APP_VERSION_KT" >&2; exit 1
fi

DATE="$(date -u +%Y.%m.%d)"
SHA="$(git rev-parse --short=7 HEAD)"
TAG="${DATE}-${CHANNEL}-v${VERSION_NAME}-${VERSION_CODE}-${SHA}"

# Ensure uniqueness — if tag exists, append -N
if git rev-parse "$TAG" >/dev/null 2>&1; then
  i=2
  while git rev-parse "${TAG}-${i}" >/dev/null 2>&1; do i=$((i+1)); done
  TAG="${TAG}-${i}"
fi

BRANCH_NAME="$(git rev-parse --abbrev-ref HEAD)"
COMMIT_MSG="Kaiteyo ${CHANNEL} ${VERSION_NAME} (${VERSION_CODE}) — ${DATE} — ${BRANCH_NAME}@${SHA}"

cat <<EOF
Tag System: YYYY.MM.DD-<channel>-v<VERSION>-<CODE>-<SHA>
  Date:     $DATE (UTC)
  Channel:  $CHANNEL (from ${CHANNEL_ARG:-auto:$BRANCH_NAME})
  Version:  v${VERSION_NAME} (${VERSION_CODE})
  Commit:   $SHA ($BRANCH_NAME)
  Tag:      $TAG
EOF

if [[ "$DRY_RUN" == true ]]; then
  echo "[dry-run] Would create annotated tag: $TAG"
  exit 0
fi

echo "Creating annotated tag $TAG ..."
git tag -a "$TAG" -m "$COMMIT_MSG"

if [[ "$PUSH" == true ]]; then
  echo "Pushing tag $TAG to origin ..."
  git push origin "$TAG"
else
  echo "Tag created locally. Push with: git push origin $TAG"
fi
