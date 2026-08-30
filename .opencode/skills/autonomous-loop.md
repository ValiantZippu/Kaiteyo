# Skill — Autonomous Loop (Keep Adding Features Forever)

> **THE skill.** Load this when you want the AI to take over Kaiteyo and keep shipping without being asked. One task at a time, forever.

## Prompt to invoke

> "Load `.ai/skills/autonomous-loop.md` and start the Kaiteyo autonomous loop. Pick the highest-priority unblocked task and ship it. Keep looping."

## Loop

```kotlin
while (true) {
    verifyBuild()          // .ai/skills/build.md — must be green before feature work
    task = pickTask()      // .ai/skills/pick-task.md — P0 topmost unblocked TODO
    writeActive(task)      // .ai/tasks/ACTIVE.md
    implement(task)        // .ai/skills/feature.md (+ screen.md / strings.md as needed)
    verify(task)           // .ai/skills/verify.md — build + tests + DoD checklist
    if (!verified) { fix(); continue }  // never commit red
    commit(task)           // .ai/skills/git.md
    log(task)              // .ai/memory/progress.md + .ai/STATE.md + tasks/QUEUE.md
    // loop — next task
}
```

## Per-iteration budget

- Max 12 files changed (unless migration). If more, split task.
- One task, one commit on `early-develop`, one log entry.

## Stop conditions

- Build irreparably red after honest fix attempts → report, stop.
- All TODOs DONE/BLOCKED → report status, propose new tasks in `docs/planning/TODO.md`, stop.
- Human interrupts → stop.

## State across sessions

`memory/progress.md` (append-only) + `STATE.md`/`STATE.json` persist. Next AI resumes from there.

## Invariants

- Never pick blocked task whose gate not ACCEPTED.
- Never create second DB for same concept.
- Never fake UI — every button does something, every screen has loading/empty/error/offline.
