# Skill — Git (Branch / Commit / Push)

Branch: `early-develop` is the AI default. Never push to `develop`/`main` unless human says "sync to develop" or "merge it".

Flow:
```bash
git status --short
git diff --stat
git checkout early-develop
git pull --ff-only   # or fetch, but never force
# implement + verify green first!
git add <task files>   # never secrets, never local.properties
git diff --cached --stat
git commit -m "feat(<area>): <what> [KT-*]

<1-2 line body: why, what subsystem spec.>
"
git push origin early-develop
```

Naming:
- `feat(media): attach subtitle browser [KT-MEDIA-012]`
- `fix(dict): deinflection prefix scoring [KT-DICT-004]`
- `docs(arch): core service contracts [KT-DOC-001]`

Branch alternative: `ai/KT-*` then merge to `early-develop` with `--no-ff` — same commit message.

Sync to develop (only when human says):
```bash
git checkout develop; git merge --no-ff early-develop; git push origin develop
```

Reference: `skills/BRANCH_POLICY.md`, `docs/development/GITHUB_WORKFLOW.md`
