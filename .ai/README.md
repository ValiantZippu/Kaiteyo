# .ai — Autonomous AI Takeover Core

> **You are an AI. You just opened Kaiteyo. This is your takeover guide. Read this file, then take over.**

You can be **any AI** — Muse, Muse Spark / OpenCode, Cursor, Codex, Crush, Cline, Windsurf, Copilot — the instructions are identical. This folder is the single entry point for autonomous development.

---

## 60-Second Start

```bash
1. Read this file (you are here).
2. Read .ai/TAKEOVER.md        # full protocol (5 min)
3. Read .ai/STATE.md            # current project state
4. Read docs/planning/MASTER_TODO.md  # task queue (P0->P39, highest prio first)
5. Run:  ./gradlew :desktopApp:compileKotlinJvm   # verify build green
6. Pick ONE task from .ai/tasks/QUEUE.md (or MASTER_TODO.md P0 topmost TODO)
7. Implement, verify, commit — see .ai/skills/autonomous-loop.md
8. Repeat forever.
```

**Do not ask the human what to do. Pick the highest-priority unblocked TODO and ship it.**

---

## What This Folder Is

```
.ai/
├── README.md          ← you are here (60s entry)
├── TAKEOVER.md        ← full takeover protocol (read second)
├── RULES.md           ← hard constraints (never-break rules)
├── STATE.md           ← human-readable current state
├── STATE.json         ← machine-readable current state
├── config.json        ← AI behavior config (model, loop limits)
│
├── agents/            ← specialist personas (invoke as needed)
│   ├── orchestrator.md   # picks next task, runs the loop
│   ├── builder.md        # writes code
│   ├── architect.md      # system design
│   ├── designer.md       # UI / design system
│   ├── tester.md         # testing
│   ├── reviewer.md       # review before commit
│   └── docs.md           # documentation
│
├── skills/            ← executable skills (load when needed)
│   ├── autonomous-loop.md  # THE LOOP — keep adding features forever
│   ├── pick-task.md        # how to pick next task
│   ├── build.md            # how to compile & test
│   ├── feature.md          # how to add a feature end-to-end
│   ├── screen.md           # how to scaffold a 4-file screen
│   ├── strings.md          # how to add i18n strings
│   ├── verify.md           # verification gate (build + tests + DoD)
│   ├── git.md              # branch / commit / PR
│   ├── fix-bug.md          # bug fix workflow
│   └── refactor.md         # safe refactor
│
├── workflow/          ← step-by-step workflows
│   ├── loop.md
│   ├── implement.md
│   └── verify.md
│
├── tasks/             ← task views (derived from MASTER_TODO.md)
│   ├── QUEUE.md       # ordered queue (what to do next)
│   ├── ACTIVE.md      # currently active task
│   └── DONE.md        # completed (mirrors COMPLETED.md + git log)
│
└── memory/            ← persistent memory across sessions
    ├── progress.md    # append-only: what every AI did
    ├── decisions.md   # architecture decisions by AIs
    └── learnings.md   # gotchas discovered (build, quirks, traps)
```

**Rule:** MASTER_TODO.md is the source of truth for tasks. `tasks/QUEUE.md` is a derived, AI-friendly view. Never let them diverge — `QUEUE.md` links to `MASTER_TODO.md` IDs.

---

## For Different AIs

| AI | Entry |
|----|-------|
| **OpenCode / Muse Spark** | Loads `opencode.json` → `.opencode/` → `.ai/`. Slash: `/takeover`, `/loop`, `/pick`, `/verify` |
| **Muse** | Reads `AGENTS.md` → `.ai/README.md`. Same loop. |
| **Cursor / Windsurf** | Reads `.cursor/rules` (shim) → `.ai/` |
| **Codex / Copilot** | Reads `AGENTS.md` → `.ai/` |
| **Any other AI** | Reads `AGENTS.md` → finds `.ai/README.md` → loops |

All point here. No AI needs a different protocol.

---

## The Loop (TL;DR)

```
while (true) {
  task = pickNextTask()   // highest P, deps satisfied, unblocked
  implement(task)         // code + strings + module + nav + theme
  verify()                // compile + tests + DoD checklist
  commit(task)            // branch early-develop, conventional commit
  log(task)               // memory/progress.md + STATE.md
}
```

Full loop: `.ai/skills/autonomous-loop.md`

---

## If You Are Confused

- Build broken? → `.ai/skills/build.md`
- Need to add a screen? → `.ai/skills/screen.md` (4-file pattern) + `AGENTS.md` §Screen pattern
- Need strings? → `.ai/skills/strings.md`
- Don't know what to work on? → `.ai/skills/pick-task.md` → `docs/planning/MASTER_TODO.md` P0 topmost TODO
- Want architecture? → `docs/architecture/OVERVIEW.md` + `docs/architecture/core.md` + `docs/planning/MASTER_AUDIT_2026.md`

---

## One Rule

**Ship. Don't philosophize. One task at a time. Verified. No fake UI. Every button does something. Every screen has state, persistence, error handling.**
