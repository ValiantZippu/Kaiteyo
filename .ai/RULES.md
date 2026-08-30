# Kaiteyo — Hard Rules (Never Break)

> Violating any rule is a failed takeover. Read before writing code.

## Never Change

| Rule | Why | Where enforced |
|------|-----|----------------|
| **Never change SRS / FSRS scheduling** | User study data + science | `core/.../srs/fsrs/` — STANDARDS §341 |
| **Never change `.sq` schemas without explicit human request** | Migrations are versioned + irreversible | `core/src/commonMain/sqldelight_*/` |
| **Never rename `ua.syt0r.kanji`** | Breaks every import | grep |
| **Never edit `buildSrc/AppVersion.kt` for a version bump casually** | Version is deliberate | `installer/common/version.json` sync required |
| **Never remove `adjustFlavorTasks()`** | F-Droid reproducibility | `app/build.gradle.kts` |
| **Never commit secrets** | Security | `.gitignore` + manual check |
| **Never force-push to `main`** | Safety | branch policy |
| **Never hand-edit `_generated` SQLDelight interfaces** | Generated | `gradle :core:generate...` |

## Never Invent

- **No fake UI**: every button has a real handler. If not implemented, surface `Empty/Error/Offline` — not a blank screen.
- **No fake data**: `AppState.seedDemoData()` seeding fabricated cards violates the rule. First-run = empty state. See `PRODUCT_AUDIT.md` §5.4.
- **No fabricated stats**: all numbers derive from `ActivityEvent` ledger. Blank days stay blank.
- Dos: every dataset has source/license/provenance (`docs/data/SOURCES.md`).

## Always Do

- **One task at a time** — highest-priority unblocked TODO.
- **Build green before commit**: `:desktopApp:compileKotlinJvm` + `:core:allTests` if touched.
- **4-file screen pattern** + `di/AppModule.kt` + `MainNavigation.kt` + both `*Strings` files.
- **Theme tokens only** — `KaiteyoSemanticColors` / `Ds*`, 4dp grid, correct modifier order.
- **Docs + CURRENT_ISSUES.md** when behavior/bug changes.
- **Branch `early-develop`** by default.

## Architecture Rules (from docs/architecture/core.md)

1. UI does not own persistence.
2. Features communicate via services/events/interfaces.
3. One SRS, one Library, one Activity ledger, one Dictionary, one Mining pipeline.
4. No duplicate DB for same concept.
5. Content is unified (`data-model.md`) — AniList + MAL + local file = one row via `ExternalIds` union.

## Second Boundaries

- No third navigation (choose: `NavShell` stays, `WorkspaceNav` deprecated after ADR-0017).
- Game engine gated on ADR-0018 `ACCEPTED` (do not start runtime code before).
- Chromium JCEF gated on proposed ADR-0020 (license/size/perf gate).
- Browser websites never get internal API access (`browser.md`).
- Settings are service-owned, not screen-owned.

## When Unsure

Read the subsystem spec (`docs/architecture/<name>.md`) + `docs/planning/MASTER_AUDIT_2026.md` for that subsystem. If still unsure, append question to `.ai/memory/decisions.md` and pick the reversible path.
