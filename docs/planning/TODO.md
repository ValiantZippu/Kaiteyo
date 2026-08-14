# Kaiteyo (書いてよ) — Master TODO

> **Status taxonomy** (see [`README.md`](README.md)): `TODO` (task with a clear definition
> of done) · `FEATURE` (specified new capability) · `RESEARCH` (needs investigation) ·
> `TECHNICAL DEBT` (deliberate paydown) · `BLOCKED` (waiting on something external) ·
> `DONE` (see `COMPLETED.md` / `CHANGELOG.md`).
>
> **Priority legend** — 🔴 P0 critical · 🟡 P1 high · 🟢 P2 medium · 🔵 P3 low.

---

## TODO — open tasks (priority-ordered)

### 🔴 P0 — Desktop polish (KNOWN ISSUE track, fix before new features)

- [ ] Animation stutter — hover animations, theme switching, and window movement are not
      smooth. Target 60 FPS.
- [ ] Resize glitches — panels jump, spacing changes, animations break during window
      resize.
- [ ] Hover animation inconsistency — some elements animate, others don't.
- [ ] Spacing/alignment consistency — some components don't follow the 4dp grid; visual
      hierarchy between primary/secondary/tertiary content is weak; corner radius
      strategy inconsistent.

### 🟡 P1 — High

- [ ] **Archived decks still visible everywhere** — `is_archived` is persisted and
      toggleable, but the main dashboard lists don't filter archived decks and there is no
      "Archived" section to restore them (explicit follow-up from v2.0). `FEATURE`
- [ ] **Mobile navigation snap** — mobile nav is top/bottom only; add snap behavior
      consistent with desktop.
- [ ] **Sync indicator / sponsor button** in the shell chrome (currently only on portrait
      chrome).
- [ ] **Settings cleanup** — remove remaining randomly placed appearance options; route
      everything through the Settings Center categories.

### 🟢 P2 — Medium

- [ ] **OCR hardening** — Tesseract integration edge cases, missing-engine UX, region
      capture polish (desktop).
- [ ] **Auto-update rollout** — enable update channels (stable/beta/nightly) for end
      users; update-feed wiring is architecture-complete.
- [ ] **Grammar content** — expand the desktop grammar starter deck; add grammar data
      behind the KJD pipeline once an openly licensed grammar dataset is identified
      (`RESEARCH` until then).
- [ ] **Tablet layouts** — dedicated Android/iOS tablet polish (form-factor nav exists).

### 🔵 P3 — Low

- [ ] Accessibility completeness: full keyboard navigation, screen-reader support,
      high-contrast mode, reduced-motion completeness.
- [ ] Performance: lazy loading for large lists, image caching, memory optimization,
      startup-time reduction, Compose compiler metrics.

---

## FEATURE — specified but not yet scheduled

- Community features: shared decks, theme marketplace, study groups, optional
  leaderboards (from `FUTURE_IDEAS.md`).
- Pitch-accent diagrams, graded reading mode, handwriting-recognition improvements.
- Custom card templates (HTML/CSS), scripting API.
- Web/PWA version; Chrome OS / Wear OS review surface.
- KJD: Tatoeba example-sentence adapter, compact binary export, pitch/grammar extension
  datasets (see `kjd/README.md` → Future direction).

## RESEARCH — needs investigation before scheduling

- AI-assisted review scheduling (how it would coexist with FSRS-5; likely a future
  enhancement, not a replacement).
- Openly licensed grammar and example-sentence datasets for the KJD pipeline.
- Plugin runtime sandboxing design (capability model, subprocess vs classloader) — see
  ADR-0011.

## TECHNICAL DEBT — deliberate paydowns

- **Two jdata implementations** — `kjd/` (standalone) and the desktop suite's
  `engine/jdata` platform evolved separately; consolidate into one pipeline (ADR-0007).
- **No UI tests** — Compose UI testing harness not established (see `../testing/README.md`).
- **Platform actuals under-verified** — several Android/iOS/Windows paths are
  "code-complete, runtime-unverified" (tracked in `CURRENT_ISSUES.md` → BLOCKED).
- **Website `dist/` is committed** — regenerate it on every docs change (the site build
  is a manual step; consider CI).
- **Scattered status tracking** — features historically tracked across FEATURES.md /
  TODO.md / COMPLETED.md; FEATURES.md is now the single source of truth.

## BLOCKED — waiting on something external

- iOS runtime verification (APKG import/export, file pickers) — needs a macOS build +
  device/simulator.
- Windows runtime verification (media keys, native drag, tray notifications) — needs a
  Windows machine.
- Android SAF picker verification (import/export, re-import grant) — needs an Android
  build + device.
- AnkiConnect import end-to-end — needs a live Anki + AnkiConnect instance.
- Flathub / Snap publishing — needs maintainer accounts + CI wiring.

---

## DONE — delivered (historical, summarized)

Everything below is **shipped**. Details in `COMPLETED.md` and `CHANGELOG.md`.

- **Window experience** — custom 44dp title bar, scoped drag region, 8-zone resize,
  system menu, window-state persistence, rounded corners (v2.2.1).
- **Theme system** — BaseMode Light/Dark/OLED; accent schemes; **17 built-in presets**
  (Signature, OLED, Dark Gray, Light, Reading, Solarized, Nord, Catppuccin, Gruvbox,
  Tokyo Night, Dracula, Nothing OS, Material, Glass, Cotton Candy, Ocean, Forest);
  Theme Studio (color wheel, gradients, motion, layout, JSON import/export) (v2.0/v2.2).
- **Navigation** — floating dock island (4 edges), expanded/compact/hidden layouts,
  compact tab bar <720dp, persistent workspace panels, command palette (v2.2).
- **Study engine** — JLPT + grade decks, vocab/flashcards, writing + stroke evaluation,
  FSRS-5 SRS, deck archive, unified Library hub, text analysis, statistics + exams +
  achievements (v2.0+).
- **Installer & first-run** — branded installers (Inno/DMG/AppImage/deb/rpm),
  onboarding wizard, auto-update architecture (v2.2.1).
- **Anki interop & persistence** — `.apkg` import/export, JSON/CSV/TSV/TXT pipeline,
  persistent card pool (v2.3 in progress).
