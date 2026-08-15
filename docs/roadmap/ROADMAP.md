# Kaiteyo (書いてよ) — Roadmap

This roadmap is a living document. It reflects **where the project actually is** — shipped
milestones are historical; unshipped items are labeled with their status. Priorities shift;
the issue tracker (`docs/planning/CURRENT_ISSUES.md`) and task list
(`docs/planning/TODO.md`) are the operational source of truth.

> **Blueprint context**: the master product definition is `docs/product/PRODUCT.md`
> (MASTER §0–§88), the per-subsystem status matrix is `docs/planning/CURRENT_STATE.md`,
> and the full task inventory is `docs/planning/MASTER_TODO.md` (work packages P0–P39).
> The game-side roadmap lives in `docs/game/README.md` + `docs/production/phases.md`;
> the vision is `docs/product/VISION.md` (+ `docs/roadmap/PROJECT_VISION.md`).

## Current status

- **v2.2.1 shipped** — platform polish & rebranding completion: premium installer
  subsystem, first-run onboarding, auto-update architecture, native window shell, unified
  statistics dashboard. See [`CHANGELOG.md`](../../CHANGELOG.md).
- **Desktop suite is the flagship** and is feature-complete for the core immersion loop
  (dictionary, media, mining, OCR, browser, sync, theming).
- **Known rough edges** (tracked in `CURRENT_ISSUES.md`): animation stutter during hover /
  resize, inconsistent hover animations, and several "code-complete but unverified on
  platform" items (iOS, Windows runtime checks).

## Next release: v2.3 — Anki interoperability & persistent data

Status: in progress (much is already implemented per `docs/planning/COMPLETED.md`).

- [x] Persistent desktop card pool (`~/.kaiteyo/library/cards.json`) — imports/edits/
      reviews survive restarts
- [x] Real Anki `.apkg` export/import (JVM/Android/iOS) with scheduling mapping, media
      extraction, template rendering, HTML sanitization
- [x] Unified import/export pipeline (JSON/CSV/TSV/TXT) with preview + conflict policies
- [ ] Filter archived decks out of the main lists + an "Archived" restore section
- [ ] Release v2.3

## Short term (next few milestones)

| Area | Items | Status |
|---|---|---|
| Desktop polish | Animation stutter (60 FPS), resize glitches, hover consistency, spacing/radius audit | 🟡 open (`CURRENT_ISSUES.md` P0/P1) |
| OCR | Harden Tesseract integration, failure UX, region capture polish | 🟡 partial |
| Auto-update | Roll out update channels (stable/beta/nightly) to end users | 🟡 architecture done |
| Mobile | Verify Android/iOS platform actuals (file pickers, APKG, backup) on-device; finish mobile sync UX | 🟡 pending platform verification |
| Sync | Improve conflict handling and cross-device UX beyond desktop-first | 🟢 |
| iOS | Full App Store parity for the shared engine; CI-ish verification | 🟢 |

## Mid term

- **Grammar**: expand grammar practice content beyond the starter deck; add grammar data
  behind the KJD pipeline (needs an openly licensed grammar dataset).
- **Plugin runtime**: implement sandboxed plugin loading (capability model, subprocess or
  classloader sandbox) — deliberately deferred, security first (ADR-0011).
- **Accessibility**: screen-reader support, full keyboard navigation, high-contrast,
  reduced-motion completeness.
- **Tablet layouts**: dedicated tablet polish on Android/iOS.
- **KJD consolidation**: unify the two jdata implementations (desktop `engine/jdata` vs
  standalone `kjd`) into one pipeline.
- **Data**: Tatoeba example-sentence dataset behind the KJD pipeline; pitch-accent and
  grammar extension datasets (see `kjd/README.md` → Future direction).
- **Node layer foundations**: node model + knowledge graph (ADR-0013), user knowledge
  states (`docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md`), and the content pipeline
  (ADR-0015) — the shared substrate Journey, media exposure, and discovery will build on.
- **Journey engine evaluation**: evaluate established game engines per STANDARDS §242 and
  document the decision (ADR-0014) before any world work.

## Long term (ideas, not commitments)

From `docs/planning/FUTURE_IDEAS.md` — nothing here is scheduled:

- **Journey world** — Kamakura + Enoshima vertical slice as the first world. **Target
  architecture only** (ADR-0014, NODE §158): no implementation, no launch claim.
  Full spec: `docs/architecture/NODE_ARCHITECTURE.md` + `docs/architecture/nodes/`.
- Community features: shared decks, theme marketplace, study groups, optional leaderboards
- AI-assisted scheduling & learning paths (as a *future enhancement* to FSRS, not a
  replacement)
- Handwriting recognition improvements, pitch-accent diagrams, graded reading mode
- Web/PWA version for Chromebooks, Chrome OS, Wear OS flashcard review
- Custom card templates (HTML/CSS), scripting API, offline TTS expansions

## Explicit non-goals (from `PROJECT_VISION.md`)

- No gamification gimmicks (points/badges/streaks as the core loop — the app has
  achievements, but the design treats users as capable adults). Journey follows the same
  rule: exploration/discovery-first progression, never XP grinding (NODE §86, §116)
- Not a mobile-first app (mobile is supported; desktop is primary)
- Not a social network
- No Kaiteyo-hosted central service (sync is provider-based; see ADR-0009)
