# Skill — Pick Next Task

> Load when you need to decide what to work on.

## Algorithm

1. Read `docs/planning/MASTER_TODO.md` P0 → P39 top to bottom.
2. For each task row:
   - Skip if `Status` = `DONE` / `BLOCKED` (gate not ACCEPTED) / `IN PROGRESS` (already taken, check `.ai/tasks/ACTIVE.md`).
   - Skip if any `Deps` is still `TODO`/`BLOCKED`.
   - Pick first remaining with highest priority `🔴 P0` → `🟡 P1` → ...
3. Cross-check `docs/planning/TODO.md` operational short-list (P0 bugs are often top priority for polish).
4. Cross-check `docs/planning/CURRENT_ISSUES.md` P0 #1–4 (anim stutter, resize) — if user-facing, treat as P0.
5. Tie-break: smaller task first (ship faster), then task you have context for.
6. Write to `.ai/tasks/ACTIVE.md`:
   ```
   # ACTIVE — <KT-ID> <Title>
   Status: picked @ <timestamp> by <ai session>
   Deps: <list, all DONE>
   Next: implement per .ai/skills/feature.md
   ```

## Output

- One `KT-*` ID + title.
- If no unblocked TODO: report "All TODOs DONE/BLOCKED — propose new or report status" and stop loop.

## Common picks (2026-08 snapshot)

- If ADR-0017 blocked: pick topmost P0 bug (anim/resize/hover/spacing) or OCR guided setup or suite-view mounting not gated.
- Never pick ADR-0018 game runtime or ADR-0020 JCEF until gate ACCEPTED.
