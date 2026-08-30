# Kaiteyo — Chromium / Browser Engine

> **Status**: `PARTIALLY_IMPLEMENTED` (JavaFX WebView) + `PLANNED` (JCEF).
> Companion: `docs/architecture/browser.md` (Bridge + security), `docs/architecture/media.md`.

## 1. Options evaluated

| Option | License | Binary size | Startup | GPU | Update story | Cross-platform | Kaiteyo fit |
|--------|---------|-------------|---------|-----|--------------|----------------|-------------|
| **JavaFX WebView** (current) | GPL+CE | small | fast | limited | via JDK | Win/macOS/Linux (if JavaFX bundled) | ✅ lightweight target |
| **JCEF (Java Chromium Embedded)** | BSD (CEF) | large (~150MB) | slower | full | must re-bundle CEF | Win/macOS/Linux (arch-specific) | Planned — gated |
| **JxBrowser** | commercial | large | slower | full | vendor | Win/macOS/Linux | ❌ commercial — not GPL-compatible |
| **Electron-style** | varies | large | slow | full | separate process | — | ❌ not JVM |

## 2. Decision

Ship lightweight **JavaFX WebView** now; evaluate **JCEF** behind a license/size/perf gate (proposed `ADR-0020`). Never block mining/reading on the browser — local media/reading work without any browser.

## 3. Kaiteyo Browser Bridge (security)

Websites have **zero** access to Kaiteyo internals. The Bridge exposes page metadata, selected text + rect, video events, readable text to Kaiteyo — never the reverse. See `docs/architecture/browser.md` for the full boundary.

## 4. Integration difficulty / risk

JCEF risk: binary size, update lag behind Chromium, packaging complexity per arch, GPL compliance of CEF. Mitigation: keep `BrowserEngine` interface engine-agnostic so swapping is one impl change.

## 5. Maintenance

Track JavaFX + CEF releases; test WebView availability fallback (missing JavaFX → guided install, not crash).
