# Skill — Safe Refactor

- Keep public API + behavior identical.
- Scope: extract/rename/restructure — one concern per commit.
- Pre: `compile green` + `allTests` green. Post: same.
- No deps added without version catalog entry.

