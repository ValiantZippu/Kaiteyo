# Skill — Fix a Bug

1. Reproduce (read `docs/planning/CURRENT_ISSUES.md` # + verify via build or reading call chain to DB).
2. Find root cause (trace entry → VM → repo → DB; not guessing).
3. Fix minimal (one bug, one commit).
4. Verify: `compileKotlinJvm` green + relevant tests + retry the repro path (loading/empty/error/offline states).
5. Update `CURRENT_ISSUES.md` (move to DONE with 2-line note + PR/commit).
6. Commit per `git.md` (fix: ...), log to `memory/progress.md`.

Never hide broken UI behind blank screen — surface `Error` with retry.
