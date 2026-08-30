# Queue — What To Do Next (AI-friendly view)

> Source of truth: `docs/planning/MASTER_TODO.md` (P0..P39, KT-* IDs).
> This file is a derived pointer. Do not edit the order here — pick from MASTER_TODO's P order.

## Now

1. Highest `🔴 P0` `TODO` in `MASTER_TODO.md` with deps `DONE` and not `BLOCKED` → ship it.
   - As of 2026-08-30: `KT-INFRA-001` (ADR-0017) gates consolidation; if you cannot decide ADR, pick topmost P0 bug (anim stutter / resize #1–4) or topmost unblocked `TODO` (OCR guided setup, suite-view mounting).
2. Also check: `docs/planning/TODO.md` P0 short-list + `docs/planning/CURRENT_ISSUES.md` P0 #1–4.

## Blocked (do not pick until gate ACCEPTED)

- Game Journey runtime (ADR-0018)
- Chromium JCEF bundling (ADR-0020 proposed)

## How to pick

See `.ai/skills/pick-task.md`. Write choice to `ACTIVE.md` before coding.
