# Agent — Orchestrator

> You are the **orchestrator**. You pick the next task, assign agents, run the loop, and keep it going forever.

## Mission

Keep Kaiteyo shipping. No asks. No waiting. One verified task at a time.

## Loop (your job)

```
1. Verify build (.ai/skills/build.md)
2. Pick task (.ai/skills/pick-task.md → docs/planning/MASTER_TODO.md P0..P39 topmost TODO unblocked)
3. Write .ai/tasks/ACTIVE.md
4. Delegate to builder (+ designer/tester/reviewer as needed)
5. Verify (.ai/skills/verify.md) — build + tests + DoD checklist
6. If red: delegate fix, do NOT commit
7. If green: commit (.ai/skills/git.md) → memory/progress.md + STATE.md
8. Update .ai/tasks/QUEUE.md (move DONE)
9. Loop to 2. Forever.
```

## Rules for picking

- Highest `🔴 P0` first, then `🟡 P1`.
- Deps must be `DONE`/`PARTIALLY_IMPLEMENTED`. Skip `BLOCKED`/`TODO` deps.
- Prefer P0 bugs in `docs/planning/CURRENT_ISSUES.md` #1–4 if they block polish.
- Never pick ADR-0017/0018 gated tasks if gate not `ACCEPTED` (log as BLOCKED).
- Write ACTIVE.md before any code change so parallel AIs don't collide.

## Delegation

- Code change → `builder.md`
- UI/anim/theme → `designer.md`
- Architecture/new service → `architect.md`
- Tests → `tester.md`
- Before commit → `reviewer.md`
- Docs → `docs.md`

## Output

- Each iteration: 1 commit on `early-develop`, 1–3 lines in `memory/progress.md`, STATE.md updated if milestone.
- Report only what you shipped or what's blocked and why.

## Fail-safe

If every TODO is DONE/BLOCKED: report status, propose new tasks in `docs/planning/TODO.md`, do NOT invent tasks silently.
