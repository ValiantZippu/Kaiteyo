# Kaiteyo — Current Issues

Living issue tracker. **Categories** (see [`README.md`](README.md)): `BUG` (reproducible
defect being fixed/investigated) · `KNOWN ISSUE` (acknowledged limitation, not yet
scheduled) · `BLOCKED` (waiting on platform access or a decision) · `DONE` (fixed).

**Priorities** — 🔴 P0 critical · 🟡 P1 high · 🟢 P2 medium · 🔵 P3 low.

---

## BUG / KNOWN ISSUE — open

### 🔴 P0 — Desktop quality (desktop suite)

| # | Item | Category | Notes |
|---|------|----------|-------|
| 1 | Animation stutter during hover / theme switching / window move — target 60 FPS | KNOWN ISSUE | `KNOWN ISSUE`; part of the polish pass |
| 2 | Resize glitches — panels jump, spacing changes, animations break on resize | BUG | Reproduce with the 8-zone resize handles |
| 3 | Hover animations inconsistent across components | KNOWN ISSUE | Some animate, some don't |
| 4 | Inconsistent spacing / alignment / corner-radius strategy | KNOWN ISSUE | 4dp grid not uniformly followed |

### 🟡 P1

| # | Item | Category | Notes |
|---|------|----------|-------|
| 5 | Archived decks still visible in main lists; no "Archived" restore section | BUG | `is_archived` works (v2.0); filter + restore UI missing |
| 6 | Settings still has randomly placed appearance options | KNOWN ISSUE | Route everything through Settings Center categories |
| 7 | Mobile nav lacks snap behavior (top/bottom only) | FEATURE | see TODO.md |

### 🟢 P2

| # | Item | Category | Notes |
|---|------|----------|-------|
| 8 | OCR is Tesseract-dependent; missing-engine UX is a hint, not a guided setup | KNOWN ISSUE | hardening planned |
| 9 | Auto-update not yet enabled for end users | KNOWN ISSUE | architecture complete; rollout staged |
| 10 | iOS is secondary; several platform paths verified only by build | KNOWN ISSUE | see BLOCKED below |

---

## BLOCKED — code-complete but unverified (needs platform runtime)

These items are **implemented and compiled** but not verified on the target platform.
They are not `DONE` until runtime-verified.

| Item | What's needed |
|---|---|
| iOS `.apkg` import/export (`AnkiPackage.ios.kt`, pure-Kotlin ZIP/inflate) | macOS build + simulator/device |
| iOS file picker/save (`UIDocumentPickerViewController`) | macOS build + device |
| Android SAF picker + persistable re-import grant | Android build + device |
| Windows system media keys (`WH_KEYBOARD_LL` hook) | Windows runtime |
| Windows/Linux native window drag (JNA) | Windows + Linux runtime |
| AnkiConnect deck import (`AnkiImporter`) | Live Anki + AnkiConnect |
| Desktop card-pool persistence on every mutation path | Desktop runtime sweep |
| Desktop `.apkg` import/export after rewrite | Desktop runtime sweep |
| Floating dock island / custom window chrome styling | Desktop runtime sweep |
| First-run onboarding completion path | Desktop runtime sweep |

---

## DONE — recently fixed (history)

Fixed and shipped items are preserved in **`COMPLETED.md`** (by version) and
**`CHANGELOG.md`** (repo root). Notable recent fixes (v2.2–v2.3 era):

- Anki `.apkg` import/export rewritten and working (JVM/Android/iOS) — dead/dangling code
  removed
- Card pool persisted to `~/.kaiteyo/library/cards.json` (imports/edits/reviews survive
  restarts; demo seeding is first-run only)
- Import/Export screen wired to the real pipeline; core imports persist through
  `mergeImportedCards()` with FSRS scheduling
- Screen load failures now emit `RefreshableData.Failed` (no infinite spinners) with
  retry states on dashboards
- Review keyboard shortcuts actually handled (`Space`, `1–4`, `B`, `S`, `R`, `Ctrl+Enter`,
  `Ctrl+Z` — guarded against the reschedule dialog)
- Window dragging scoped to the title bar; interactive components no longer draggable
- Floating dock island design shipped (`WorkspaceShell.kt` / `WorkspaceNav.kt`)
- iOS project fully renamed to Kaiteyo; repo-wide rebranding sweep complete
- Compose import fixes (`animateColorAsState`, `animateFloatAsState`, `window.close()`)
- Unsafe casts in Deck Details made null-safe; dead/disabled controls removed or wired
- AnkiConnect transport + local API bearer auth + integration hub + settings persistence
- System media keys, media notifications (Windows)

Full detail: [`COMPLETED.md`](COMPLETED.md) and the root [`CHANGELOG.md`](../../CHANGELOG.md).

---

## How to use this tracker

1. **New bug** → add a row under BUG with priority; verify against
   `../troubleshooting/` first.
2. **Fix verified** → move the row to DONE (brief note), add to `COMPLETED.md` +
   `CHANGELOG.md`.
3. **Code-complete but unverified** → list under BLOCKED with what's needed.
4. **Won't fix / deferred** → move to `FUTURE_IDEAS.md` with a reason.
