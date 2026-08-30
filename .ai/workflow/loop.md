# Workflow — Autonomous Loop (Detailed)

```mermaid
verifyBuild -> pickTask -> writeActive -> implement -> verify -> commit -> log -> pickTask ...
                     |                                 |
                     +-> if no unblocked TODO -> report+stop
                                                    |
                                    if verify red -> fix (loop within)
```

Step docs:
- Build: `../skills/build.md`
- Pick: `../skills/pick-task.md`
- Implement: `../skills/feature.md` (+ `screen.md`/`strings.md` as needed)
- Verify: `../skills/verify.md` + `agents/reviewer.md`
- Commit: `../skills/git.md`
- Log: append `memory/progress.md` + update `STATE.md`/`STATE.json` + `tasks/QUEUE.md`

Timebox: one task ~ 20–60 min. If larger: split or log as multi-commit epic.
