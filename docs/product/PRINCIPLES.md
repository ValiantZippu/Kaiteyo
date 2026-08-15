# Kaiteyo — Master Product Principles

> Expands MASTER §2 of [`PRODUCT.md`](PRODUCT.md). Every principle below is a **test**:
> a feature, screen, or subsystem that violates one of these has not finished design.
> Principles are normative for humans and AI agents alike; violations are review-blockers.

## The master statement

**Kaiteyo is professional, dense but understandable, powerful, fast, coherent, highly
connected, extensible, data-driven, user-controlled, offline-friendly, open-data-friendly,
cross-platform, keyboard-friendly, touch-friendly, controller-friendly, accessible,
visually polished, animation-rich without being wasteful, configurable, maintainable, and
scalable.**

That is a lot of adjectives. The definitions below are what make them checkable.

---

## 1. UX principles

| Principle | Definition | Check |
|---|---|---|
| Complexity underneath | The user sees simple concepts; the system maintains the complexity (NODE §81) | Can the user do the thing without ever seeing a node ID, a schema, or a query? |
| Power-tool density | Dense data where appropriate; progressive disclosure; hover/tap details; tooltips; shortcuts; context menus (NODE §121) | Is every piece of chrome earning its pixels? |
| No empty pages | Every major screen uses its space: multi-column, adaptive grids, contextual panels, search/filter regions (NODE §120) | Screenshot the screen at 1920×1080 and at 480×800 — is the space used *intentionally*? |
| One goal per screen | What is the primary goal? Secondary? What can be hidden? (NODE §154) | Stated in the screen's UX flow doc (`docs/architecture/nodes/UX_FLOWS.md`) |
| No dead controls | Every button does something or is visibly disabled | Zero `onClick = { }` in reachable UI (PRODUCT_AUDIT §5) |
| Full input coverage | Keyboard, mouse, touch, gamepad where appropriate (MASTER §27) | Every action reachable by each input class the platform supports |
| State honesty | Loading / empty / error / offline / reduced-motion states exist per screen (NODE §154) | Each state has a spec and a test |
| No fake anything | No fabricated data, statistics, or demo content presented as real (STANDARDS §290) | Every number on screen traces to the event ledger or a database query |

## 2. UI principles

| Principle | Definition | Check |
|---|---|---|
| One design system | Typography, spacing (4dp grid), radii, elevation, icons, colors, motion, components are tokens (STANDARDS §224–§225) | No literal colors/radii/spacings in screen code; everything through tokens |
| Consistency | Same control behaves the same everywhere (NODE §122) | Component inventory in `docs/design/UI_SYSTEM.md` |
| Motion communicates | Animations show origin/destination/change; never decoration (NODE §123) | Every animation has a purpose statement in `docs/design/ANIMATION_SYSTEM.md` |
| Motion is respectful | Animation speed setting, reduced-motion mode, transition intensity (NODE §123) | Reduced motion disables all non-essential animation |
| Responsive by design | Reflow, not jump: panels adapt, cards reflow, text wraps, navigation changes (NODE §124) | Resize test at the extreme sizes of the platform matrix |
| Platform-native feel | Windows feels like Windows, Android like Android, iOS like iOS — all recognizably Kaiteyo (NODE §125) | Platform docs (`docs/platform/`) list the platform-specific behaviors |

## 3. Architecture principles

| Principle | Definition | Check |
|---|---|---|
| Connected, not siloed | Subsystems communicate through documented services/events/nodes (MASTER §1) | Two subsystems never share a database table they don't own |
| Domain-first | Business logic lives in domain, not in screens (STANDARDS §177) | No SQL in composables; no business rules in UI state |
| Smallest correct surface | Change the smallest architectural surface that is correct (STANDARDS §370) | PR diff shows the surface |
| Modules over monoliths | Conceptual responsibilities separated (STANDARDS §176) | Module boundaries documented in `docs/architecture/OVERVIEW.md` |
| Stable interfaces | Services behind interfaces; no DB details leaking (STANDARDS §209) | `SERVICE_CONTRACTS.md` is the contract |
| No parallel implementations | One navigation, one settings, one theme, one SRS, one stats (PRODUCT_AUDIT §6) | Duplication map has zero live rows (ADR-0017) |
| Evidence over vibes | Architecture decisions are ADRs with alternatives and consequences (MASTER §7) | Every ADR exists before the code lands |
| Never rewrite blindly | Document CURRENT → PROBLEM → PROPOSED → MIGRATION COST → BENEFIT → RISK first (STANDARDS §166) | No silent rewrites |

## 4. Database principles

| Principle | Definition | Check |
|---|---|---|
| Relational, offline-first | SQLite-family for the core; JSON only where it belongs (STANDARDS §179) | Two SQLDelight DBs; suite JSON is a known debt item |
| Migrations, always | Every schema change is a versioned migration (STANDARDS §180, MASTER §74) | `docs/database/MIGRATIONS.md` |
| Integrity | FKs, unique/not-null/checks, transactions, indexes (STANDARDS §181) | Validated by DB tests |
| Provenance | Every imported row traces to source + version + checksum (MASTER §8) | kjd records it |
| User data is sacred | Never silently destroy user data; backup before destructive migration (MASTER §74) | Migration policy in `MIGRATIONS.md` |

## 5. Performance principles

| Principle | Definition | Check |
|---|---|---|
| Measure, don't guess | Budgets exist and are measured (STANDARDS §188) | `docs/architecture/performance.md` budget table |
| Profile first | Investigate before optimizing (STANDARDS §189–§190) | Any perf change starts with a profile |
| No UI jank | No unnecessary recomposition, blocking the UI thread, sync network, unbounded lists (STANDARDS §191) | 60 FPS gate on desktop; frame budgets per platform tier |
| Indexed queries | EXPLAIN QUERY PLAN before adding indexes; never hundreds of blind indexes (STANDARDS §192) | Query plan documented for the money queries (`NODE_DATA_MODEL.md` §money queries) |
| Streaming by default | Lazy lists, pagination, caching, background work (STANDARDS §191); world cells stream (NODE §92) | No screen renders an unbounded collection |

## 6. Extensibility principles

| Principle | Definition | Check |
|---|---|---|
| Data-driven | Content is data with schemas, not code (ADR-0015) | New world region = new content package, not a code change |
| Contracted | Services and packages have schemas + validation gates (NODE §148) | Package validation gate list in `CONTENT_AUTHORING.md` |
| Plugin-safe | Plugin runtime only after sandbox/capability design (ADR-0011) | No `ClassLoader` loading until the ADR says so |
| Reusable over custom | Established libraries for codecs/HTTP/SQLite/crypto/etc.; custom code only where Kaiteyo adds value (STANDARDS §164, §367) | Dependency policy checklist in `STANDARDS §203` |

## 7. Integrations principles

| Principle | Definition | Check |
|---|---|---|
| Local-first | Integrations run on-device or are user-initiated (STANDARDS §182) | `docs/integrations/README.md` |
| Graceful degradation | Missing optional runtime (VLC, mpv, Tesseract, Anki) = clear message, never a crash (STANDARDS §201) | Integration tests include the unavailable case |
| Never a core dependency | No integration is required for core function (MASTER §15, §201) | Core study works with all integrations disabled |
| Sanitized imports | Imported content never executes (STANDARDS §204) | HTML sanitizer + SafeArchiveExtractor tests |
| Own your data | Kaiteyo owns its decks/SRS/stats; integrations exchange, not absorb (MASTER §15) | Anki sync is optional in both directions |

## 8. Game design principles (the Journey)

| Principle | Definition | Check |
|---|---|---|
| It's a real game | Player, world, movement, camera, interaction, NPCs, quests, stories, rewards, progression (MASTER §21) | Journey features are gated by game-design review, not just learning review |
| Exploration over grinding | Explore → notice → interact → understand → learn → discover → collect → use → remember → return (NODE §87) | No XP economy; progression is knowledge/discovery/story (NODE §116) |
| Language through context | Exposure first, optional depth later; never a quiz wall (NODE §112) | Each activity documents its language exposure, not its quiz count |
| Knowledge-aware | The world adapts to what the user already knows (MASTER §33) | Difficulty inputs are the knowledge model, not a menu option alone |
| Polished slice over vast map | One great location before a large one (NODE §91, STANDARDS §366) | The §91 proof gate blocks world expansion |
| No predatory mechanics | No energy, lives, loot boxes, artificial timers (NODE §117) | Reward spec bans them explicitly |
| Consistent art | Stylized, cozy, Nintendo-quality; never generic (MASTER §38) | Art direction doc is enforced in review |

## 9. Learning design principles

| Principle | Definition | Check |
|---|---|---|
| SRS is sacred | Scheduling stays FSRS-owned; no parallel schedulers (STANDARDS §6, ADR-0016) | One review path per card type |
| Knowledge is dimensioned | Recognition ≠ production ≠ writing ≠ listening (NODE §84) | Knowledge model has 8 dimensions |
| Evidence-driven | Knowledge transitions derive from events, never guesses (ADR-0016) | Event catalog is the only input |
| Respect prior knowledge | No re-teaching what the user knows (MASTER §33) | Adaptive difficulty spec in `docs/learning/adaptive-learning.md` + `docs/learning/curriculum-engine.md` |
| Context before cards | Media/Journey provide context; cards are the optional deepening (NODE §112) | Mining is a user action, not an auto-popup |
| Honest difficulty | JLPT/frequency/grade are real dataset properties, not marketing labels | Provenance in kjd |

## 10. Documentation principles

| Principle | Definition | Check |
|---|---|---|
| Reality first | Docs reflect code; contradictions are bugs (STANDARDS §336) | Status taxonomy used everywhere |
| No orphans | Every doc links to parent/related/architecture/TODO/ADR (MASTER §86) | Link check passes (MASTER §87) |
| No filler | Banned phrases: "etc.", "best practices", "implement similarly" (MASTER §82) | Reviewer searches for banned phrases |
| Repository is the memory | Decisions that exist only in a conversation do not exist (STANDARDS §374) | Every decision lands as an ADR or doc |
| Agents read first | AI agents read docs, then code, then plan, then act (MASTER §68) | `docs/ai/AI_AGENT_GUIDE.md` is binding |

## 11. Code quality principles

| Principle | Definition | Check |
|---|---|---|
| Explainable decisions | Every implementation decision can be explained (STANDARDS §163) | PR description states the why |
| No fake implementation | No placeholder buttons, fake data, dead handlers (MASTER §83) | PRODUCT_AUDIT §5 categories are regression-blockers |
| Test what you change | Targeted tests with acceptance criteria (STANDARDS §370) | Definition of Done in `AGENTS.md` |
| Document what you change | Docs updated in the same change (MASTER §69) | No architecture drift |
| Report what you couldn't finish | Honest handoff reports (STANDARDS §174) | Agent handoff section in every large PR |

## 12. Data provenance & licensing principles

| Principle | Definition | Check |
|---|---|---|
| Provenance on everything | source, sourceId, license, version, checksum (NODE §78, MASTER §8) | kjd records; node layer will carry |
| Verify before use | LICENSE, ATTRIBUTION, REDISTRIBUTION, MODIFICATION, COMMERCIAL USE verified per dataset (STANDARDS §183) | `docs/data/SOURCES.md` table |
| Attribution explicit | Third-party notices and in-app credits (STANDARDS §202) | `docs/legal/THIRD_PARTY_NOTICES.md` |
| Never assume licenses | Compatibility claims require verification (MASTER §83) | Datasets without verified licenses are marked BLOCKED |
| No proprietary scraping | Lawful sources only for geographic/real-world data (MASTER §24) | OSM/GIS with license review, never scraping |

---

## Related

- [`PRODUCT.md`](PRODUCT.md) — the blueprint (MASTER §2)
- [`VISION.md`](VISION.md) — the vision
- `docs/engineering/ENGINEERING_STANDARDS.md` — engineering-specific rules (§163–§376)
- `docs/design/DESIGN_LANGUAGE.md` — UI/UX execution
- `docs/game/game-overview.md` — game design execution (`docs/game/README.md` for the full set)
