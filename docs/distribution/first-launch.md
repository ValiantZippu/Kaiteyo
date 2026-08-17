# First Launch

What happens the first time Kaiteyo runs after installation, and how a broken
first launch is recovered.

## The flow

```
INSTALL
  → LAUNCH (installer offers, user accepts)
  → FIRST-RUN DETECTION (persisted flag, not missing-file inference)
  → ONBOARDING WIZARD (theme/appearance; skippable)
  → LOCAL DATA INITIALIZATION
  → HOME
```

No setup repeats on later launches. An **update** skips straight to the app
(with a migration screen only when the user-data schema changed) — onboarding
never replays.

## Local data initialization

- The app data database (dictionary/kanji data) is a bundled read-only asset —
  no download needed at first launch; initialization is local and fast.
- User-data DB + demo/deck seeding happen in the background where possible:
  the app shell is shown first, non-critical systems initialize lazily (see
  `docs/architecture/performance.md` — no media centre / game engine / full
  dictionary index warmed before the home screen unless required).
- Any progress indicator shown is **real** (bytes/items vs. total). When exact
  progress is unavailable, an honest indeterminate indicator is used — never a
  fake percentage (see `installer/docs/ARCHITECTURE.md` §5).

## Large downloads

Kaiteyo is offline-first: required data is bundled. Optional online features
are clearly separated. If a future optional download is large, the UI shows
size + estimated remaining + cancel/retry/pause — and a network failure shows
"Couldn't download the required data" with **Retry / Use offline setup /
Cancel**, never a crash (see [troubleshooting.md](troubleshooting.md)).

## First-launch performance

The first screen appears before non-critical work runs. Onboarding itself is
lightweight (theme state only); the heavy dictionary index builds lazily on
first use.

## Crash recovery / safe mode

If Kaiteyo crashes immediately after installation:

- **`kaiteyo --safe-mode`** disables custom themes, GPU effects, plugins and
  optional integrations so the app can start on problem machines.
- The app offers "open logs" and UI-state reset; user data is never
  auto-deleted.
- Documented flags: `--version`, `--safe-mode`, `--open <resource>` — see the
  CLI docs (`docs/cli/COMMANDS.md`). Only real flags are documented.

## Migration from older versions

- Old Kaiteyo data is detected and migrated in place — never overwritten
  blindly. SQLDelight user-data migrations are versioned (see
  `docs/database/MIGRATIONS.md`).
- Migration failure preserves the old data and offers Retry / restore /
  diagnostics — never a silent wipe.
- Downgrade is not supported; the update policy's min-version guard warns and
  prevents downgrade loops (see [updates.md](updates.md)).

## Install-source awareness

The app can detect its install source (store / package manager / standalone /
portable) where useful for update behavior and diagnostics — without using it
to gate features.
