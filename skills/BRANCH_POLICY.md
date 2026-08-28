# Branch Policy

> **Every contributor — human or AI — must follow this policy.**

---

## Branch Map

```
main                    PRODUCTION — tagged releases only
  │
  └── develop           STABLE — integration branch, PRs target here
       │
       ├── early-develop     ACTIVE DEV — daily work, AI pushes here
       │
       ├── testing-chamber   EXPERIMENTS — throwaway branches, break things
       │
       ├── feature/*         FEATURES — scoped work targeting develop
       ├── fix/*             FIXES — bug fixes targeting develop
       ├── docs/*            DOCS — documentation changes
       └── refactor/*        REFACTORS — code restructuring
```

---

## Branch Rules

### 🟢 `early-develop`
- **Purpose:** Active development. This is where AI and contributors push daily work.
- **Who pushes:** AI assistant (automatically), contributors (via PR or direct push if trusted).
- **Target for PRs:** Yes — PRs from `feature/*`, `fix/*`, etc. can target this.
- **Merges into:** `develop` (when explicitly told: "sync to develop").
- **Stability:** Unstable. May break. That's fine — it's the sandbox before stable.

### 🟡 `develop`
- **Purpose:** Stable integration. Features that work get merged here.
- **Who pushes:** Only via merge from `early-develop` or approved PRs.
- **Never push directly.** Always merge through a PR or explicit "sync to develop" command.
- **CI must pass** before merge.

### 🔴 `main`
- **Purpose:** Production releases only.
- **Who pushes:** Release manager only. Tagged versions (vX.Y.Z).
- **Never touch** unless you are cutting a release.
- **Protected branch** — PR review + status checks required.

### 🧪 `testing-chamber`
- **Purpose:** Throwaway experiments. Try things. Break things. Delete when done.
- **Who pushes:** Anyone. No rules.
- **Never merge** into any other branch. Delete when experiment is over.

---

## Rules for AI Assistants

| Rule | Detail |
|------|--------|
| **Default push target** | `early-develop` — always, automatically |
| **Never push to** | `main` (production) unless explicitly told |
| **Never push to** | `develop` (stable) unless explicitly told "sync to develop" |
| **PR target** | `early-develop` by default |
| **When told "sync to develop"** | Merge `early-develop` → `develop`, push both |
| **When told "merge it"** | Create PR `early-develop` → `develop`, merge it |
| **Stash local changes** | Before switching branches, stash or commit |

---

## Rules for Human Contributors

| Rule | Detail |
|------|--------|
| **Create branches from** | `develop` (for features/fixes) or `early-develop` (for active dev) |
| **PR target** | `develop` (for stable work) or `early-develop` (for WIP) |
| **Never commit directly to** | `main` |
| **Branch naming** | `feature/description`, `fix/description`, `docs/description` |
| **Keep branches short-lived** | Merge or delete within a few days |
| **Delete merged branches** | After PR is merged, delete the feature branch |

---

## Merge Strategy

| From → To | Method | Notes |
|-----------|--------|-------|
| `feature/*` → `develop` | Squash merge | Clean history |
| `feature/*` → `early-develop` | Squash or regular | WIP is fine |
| `early-develop` → `develop` | Regular merge | Preserves feature commits |
| `develop` → `main` | Regular merge + tag | Release only |
| `testing-chamber` → anything | **NEVER** | Delete when done |

---

## Emergency: Stuck in Wrong Branch

```powershell
# If you're on develop with local changes and can't pull:
git stash
git pull origin develop
git stash pop

# If you're stuck in a merge:
git merge --abort

# If you're stuck in a rebase:
git rebase --abort

# If you want to switch to early-develop:
git checkout early-develop
git pull origin early-develop
```

---

## Version Branches (Release)

When preparing a release:
1. Create `release/vX.Y.Z` from `develop`
2. Bump version in `buildSrc/src/main/kotlin/AppVersion.kt`
3. Final testing on the release branch
4. Merge `release/vX.Y.Z` → `main` (tag it) AND → `develop` (back-merge)
5. Delete the release branch
