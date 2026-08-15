# Kaiteyo Architecture — Embedded Browser (Planned)

**Status**: Planned — RESEARCH. No backend selected; a documented evaluation precedes
implementation (STANDARDS §364)
**Owner**: unassigned (desktop workspace)
**Related**: `docs/architecture/integrations.md` §5 · `docs/architecture/media.md` ·
`docs/security/README.md` · `docs/architecture/journey.md` §7 (same security posture)

## 1. Purpose

A **study-focused browser workspace** — reading Japanese sites, dictionary lookup, and
watching supported media — where text selection feeds the dictionary popup, OCR, and
mining (§196). It is *not* a general-purpose browser: its job is the study loop
(select text → lookup → mine → card). Per STANDARDS §198 the browser is an **abstraction**,
never a raw dependency on one engine.

## 2. Principles

- **Do not assume Chrome extensions work** (§198). Extension compatibility is an
  evaluation criterion, not a given. The dictionary popup and mining loop are Kaiteyo
  surfaces, implemented inside Kaiteyo — not browser-extension content scripts.
- **Security by default** (§360): the browser is an untrusted-content host. Sandbox
  where possible, restrict privileged APIs, avoid exposing the filesystem, validate
  navigation, control downloads, and keep browser permissions separate from application
  permissions.
- **Graceful degradation**: if no embedded browser is available on a platform, the
  workspace either uses the platform fallback or is honestly unavailable (§325, §299) —
  never a broken surface.

## 3. Candidates to evaluate (before any decision)

| Candidate | Platforms | Notes |
|---|---|---|
| CEF (Chromium Embedded Framework) | Windows/macOS/Linux | Full Chromium; heavyweight, licensing/size cost |
| WebView2 (Edge/Chromium) | Windows | OS-provided on Win 10+; extension model differs |
| Android WebView | Android | Platform default; capability limits |
| WKWebView / SFSafariViewController | iOS | Platform default |
| Lightweight HTML renderer / local file view | all | No remote browsing; covers local content only |

Evaluation must cover (§198, §364): platform support, extension compatibility,
sandboxing, navigation/download control, binary size, licensing, maintenance, and
embedding complexity. The outcome is a documented ADR — no adoption by default.

## 4. Abstractions (design targets)

- `BrowserService` — the stable interface (§209): load URL/file, navigation events,
  selection events (→ dictionary popup), download control, permission surface. UI depends
  on this, never on a concrete engine.
- Selection/mining bridge — selected Japanese text flows into the same
  `MiningPayload` pipeline as every other source (§196, `docs/architecture/mining.md`).
- Subtitle/media interplay — supported media in the browser uses the media backend
  abstraction (`docs/architecture/media.md` §2), not a second player.

## 5. Security requirements (§360)

- Sandboxed rendering; no privileged app APIs exposed to page content.
- Filesystem exposure limited to explicit user actions (open/save via pickers).
- Navigation validation (scheme allow-list, download interception).
- Downloads are user-initiated, scanned/named sanely, and land in a user-chosen location.
- Browser permissions are separate from application permissions.
- No TLS verification disabling, ever (§204).

## 6. Open items

- Full candidate evaluation + ADR (RESEARCH, `planning/TODO.md`).
- Extension-model analysis: what the Yomitan/ASBPlayer workflows need vs what each
  backend can provide.
- Performance budgets for the workspace (page load, selection→lookup latency) per
  `docs/architecture/performance.md` §2.

## 7. Node-layer integration (TARGET — NODE §150, §196)

Browser selections feed the shared loop exactly like every other source:

- Selected Japanese text → `DictionaryService.lookup` → node anchor → traversal chips
  (§81) and mining (`mining_source = browser`, SERVICE_CONTRACTS §10).
- Every selection+lookup emits `dictionary_lookup`/`subtitle_selected`-style events
  (EVENT_CATALOG §2–§3) feeding knowledge exposure and stats — same evidence stream as
  media/Journey.
- If a page maps to a `media_source` node (video/anime pages), the workspace can link
  it into the MEDIA family (import metadata as a node, never as raw page state).
- Security invariants are unchanged and non-negotiable: sandboxed rendering, validated
  navigation, separate permissions (§360) — the node layer never weakens them.
