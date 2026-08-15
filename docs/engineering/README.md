# 🛠️ engineering — Engineering Standards Index

The engineering constitution is [`ENGINEERING_STANDARDS.md`](ENGINEERING_STANDARDS.md)
(§163–§376, ADR-0012). The blueprint (MASTER §84) requested specific engineering
documents; they live where the repository keeps them, mapped here so nobody searches for
a missing file:

| Blueprint target (§84) | Actual location | Notes |
|---|---|---|
| `docs/engineering/CODING_STANDARDS.md` | `docs/development/CODING_STANDARDS.md` | Code style, conventions |
| `docs/engineering/TESTING.md` | `docs/testing/README.md` + `docs/architecture/nodes/TEST_PLAN.md` | Test levels, commands, acceptance |
| `docs/engineering/PERFORMANCE.md` | `docs/architecture/performance.md` | Budgets, profiling |
| `docs/engineering/SECURITY.md` | `docs/security/README.md` + root `SECURITY.md` | Threat model, policies |
| `docs/engineering/PRIVACY.md` | `docs/security/PRIVACY.md` | Data collection, opt-in |
| `docs/engineering/RELEASES.md` | `docs/releases/` (`RELEASE_PROCESS.md`, `RELEASE_CHECKLIST.md`) | Release workflow |
| `docs/engineering/ENGINEERING_STANDARDS.md` | ✅ this folder | The constitution itself |
| Toolchain / dev env | `docs/architecture/toolchain.md`, `docs/development/` | Pinned versions, setup |
| CI/CD | `docs/architecture/ci-cd.md` | Pipelines |

## How the engineering docs relate

```
ENGINEERING_STANDARDS.md (§163–§376) — the constitution (rules, phases, gates)
   ├── development/ — how to work (setup, standards, AI context, docs rules)
   ├── testing/ — how to prove it works
   ├── architecture/performance.md — how fast it must be
   ├── security/ — how it stays safe
   ├── releases/ + architecture/ci-cd.md — how it ships
   └── planning/MASTER_TODO.md — what to build (in dependency order)
```

## Related

- [`../product/PRODUCT.md`](../product/PRODUCT.md) — MASTER §84 mapping
- [`../ai/AI_AGENT_GUIDE.md`](../ai/AI_AGENT_GUIDE.md) — how agents use these docs
- [`../planning/ENGINEERING_AUDIT.md`](../planning/ENGINEERING_AUDIT.md) — audit + starting files
