#!/usr/bin/env bash
# ============================================================================
# Kaiteyo — Flatpak build
#
# Builds and bundles a local Flatpak. Publishing to Flathub uses the same
# manifest (PR to flathub/io.github.syt0r.kaiteyo).
#
# Requires: flatpak, flatpak-builder, org.freedesktop.Sdk//22.08 installed.
#
# Usage: bash installer/linux/flatpak/build.sh [version]
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
VERSION="${1:-$(jq -r '.version' "$ROOT/installer/common/version.json")}"

SRC_APP="$ROOT/desktopApp/build/compose/binaries/main/app/Kaiteyo"
[[ -d "$SRC_APP" ]] || { echo "error: $SRC_APP missing" >&2; exit 1; }

HERE="$ROOT/installer/linux/flatpak"
OUT="$HERE/out"
rm -rf "$OUT"
mkdir -p "$OUT"

command -v flatpak-builder >/dev/null 2>&1 || { echo "error: flatpak-builder not found" >&2; exit 1; }

# Launcher inside the sandbox
cat > "$HERE/kaiteyo.sh" <<'EOF'
#!/usr/bin/env bash
export XDG_DATA_HOME="${XDG_DATA_HOME:-$HOME/.local/share}"
export JAVA_HOME=/app/lib/kaiteyo/runtime
exec /app/lib/kaiteyo/bin/Kaiteyo "$@"
EOF
chmod +x "$HERE/kaiteyo.sh"

echo "== Building Flatpak ($VERSION) =="
flatpak-builder \
  --repo="$OUT/repo" \
  --force-clean \
  --ccache \
  "$OUT/build" \
  "$HERE/io.github.syt0r.kaiteyo.yaml"

flatpak build-bundle "$OUT/repo" \
  "$OUT/kaiteyo-$VERSION-x86_64.flatpak" \
  io.github.syt0r.kaiteyo

echo "== Done: $(ls -1 "$OUT"/*.flatpak) =="
echo "  Try it: flatpak install --user $OUT/kaiteyo-$VERSION-x86_64.flatpak"
