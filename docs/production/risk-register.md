# Risk Register

**Status**: LIVE — living document. **Source**: expansion spec §62; STANDARDS §62;
`docs/planning/ENGINEERING_AUDIT.md` §6; `docs/planning/CURRENT_ISSUES.md`.

## Legend

Probability/impact: L / M / H (low/medium/high). Status: OPEN / MITIGATED /
ACCEPTED / CLOSED / MONITOR. Every risk has a trigger (what would escalate it)
and an owner/system.

## Risk table

| # | Risk | Prob | Impact | Mitigation | Trigger | Owner | Status |
|---|---|---|---|---|---|---|---|
| 1 | **3D world scope creep** — world expands before the slice proves the loop | M | H | Vertical slice gate (§366); packages per region (§72); fidelity L0–L4 allows shallow worlds | slice fails exit criteria OR first non-slice content is authored | Journey lead | OPEN |
| 2 | **Game engine choice** — picking the wrong engine (or deferring forever) | M | H | Mandated evaluation (STANDARDS §242) → ADR before code; criteria: stylized rendering, streaming, mobile support, licensing | engine work starts without an ADR | Architecture | OPEN |
| 3 | **Asset production cost** — art/audio content is the real budget | H | H | L0–L4 fidelity ladder; stylized (mid-poly) direction; asset pipeline + validation; reuse | slice art budget exceeds plan | Content/Art | OPEN |
| 4 | **Two-app consolidation stays open** — duplicate SRS/settings/statistics/nav/decks drift | H | H | Decide (§7-1); PRODUCT_AUDIT §1; consolidate data layers | consolidation decision not made by v2.4 | Architecture | OPEN |
| 5 | **Data licensing** — a dataset's license blocks redistribution or commercial use | M | H | License verification before ingestion (§183–§185); SOURCES.md; KJD gates | unverified dataset ingested | Data/KJD | MONITOR |
| 6 | **Media licensing** (app) — VLC GPL interplay | L | M | VLCJ GPL-3 compatible (comment in build); backend abstraction allows mpv | license change or legal review | Legal | MONITOR |
| 7 | **External API dependency** (AnkiConnect, sync providers) | M | M | Integration abstraction (§199–§201); graceful degradation; no hard dependency | AnkiConnect breaks users' installs | Integrations | MITIGATED |
| 8 | **Mobile GPU limitations** — world performance on low-tier Android/iOS | M | H | Tier budgets (30 FPS min); LOD/streaming/dynamic resolution; measure on reference devices (§143) | slice perf fails mobile low tier | Rendering | OPEN |
| 9 | **Platform verification gaps** — iOS/Windows/Linux runtime unverified | H | M | BLOCKED list (CURRENT_ISSUES); CI/hardware; platform docs | release ships unverified paths | Platform | OPEN |
| 10 | **Desktop polish P0** — animation stutter/resize glitches unresolved | M | M | CURRENT_ISSUES #1–#4 track; perf budgets; window rebuild largely addressed | polish slips a release | Desktop | MONITOR |
| 11 | **Database migration** — future schema changes break user data | M | H | Versioned migrations (ADR-0005); migration tests; never "delete your DB" (§180) | schema change without migration test | Data | MITIGATED |
| 12 | **Subtitle parsing** — malformed SRT/ASS breaks playback | M | M | Subtitle engine hardening; fail-safe tick (Media crash-proofing done); tests | subtitle edge case crashes media | Media | MITIGATED |
| 13 | **Dictionary indexing** — brute-force search at scale | M | M | FTS/trigram indexing target (STANDARDS §186–§187); measured latency | search latency budget exceeded | Dictionary | OPEN |
| 14 | **Curriculum authoring cost** — lessons/stories/exams need content production | H | M | Content formats + pipeline (ADR-0015); community authoring path; start small (slice content) | content blocks a release | Content | OPEN |
| 15 | **AI-generated code quality** — drift, duplicate implementations, fake features | M | H | Standards (§163–§376); PRODUCT_AUDIT honesty gates; 10-phase agent workflow (§173); review discipline | an agent claims completion without implementation | Process | MONITOR |
| 16 | **Architecture fragmentation** — subsystems diverge into isolated stacks | M | H | Node layer (ADR-0013); one event stream (ADR-0016); shared service contracts; audits | duplicate data system appears in code | Architecture | OPEN |
| 17 | **Performance regression** — app or world breaks a budget | M | M | Budgets (§188–§190); profilers; perf tests in TEST_PLAN | frame/memory budget violated | Perf | MONITOR |
| 18 | **Save compatibility** — world saves break across versions | M | M | Versioned saves; refuse-newer; migration tests; save in backups (§38) | save migration untested | Journey | OPEN |
| 19 | **Sync conflict maturity** — cross-device user data conflicts | M | M | ADR-0009; conflict UX evolution; provider-based sync | two-device conflict loses data | Sync | MONITOR |
| 20 | **Security** — secrets, untrusted imports, plugin sandbox | L | H | Never hardcode secrets (§204); plugin runtime deferred (ADR-0011); sanitize imports (apkg HTML sanitization exists) | plugin loading ships unsandboxed | Security | MITIGATED |
| 21 | **Community content quality** — marketplace content bypasses validation | M | M | Hard gates (ADR-0015); capability model; validation fixtures | community package ships broken | Content/Plugins | OPEN |
| 22 | **Docs drift** — code and docs disagree; orphans/dead links | M | L | DocumentationRules; orphan/link checks; refresh audit on change (§335–§336) | docs claim shipped for target systems | Docs | MONITOR |
| 23 | **Child-mode scope** — building a second product instead of a config layer | M | M | Rule: same runtime, config + filter (§71, §115); shared core enforced | child mode forks the stack | Journey | OPEN |
| 24 | **AnkiConnect e2e** — unverified integration breaks user workflows | M | M | BLOCKED until live Anki test; graceful "Anki unavailable" (§201) | release claims AnkiConnect without e2e | Integrations | BLOCKED |
| 25 | **Anime/media data** (AniList-like) — API terms, rate limits, user accounts | M | M | Optional integration; no core dependency; privacy-aware (§25 spec) | integration becomes required | Integrations | OPEN |
| 26 | **Speech/audio production** — TTS vs authored voice quality | M | L | TTS reuse + authored audio with honest fallback (§158) | audio quality blocks immersion | Audio | MONITOR |
| 27 | **Website/community** — marketing vs engineering balance | L | L | Website is docs consumer (Python build); not on critical path | site blocks release | Website | MONITOR |
| 28 | **Tooling sprawl** — tools built that nobody uses | L | L | Build tools only when justified (§59); wrapper discovers real tasks (§238) | tool count grows without use | Tooling | MONITOR |
| 29 | **Burnout/velocity** — enormous scope, one developer | H | H | Vertical slices; honest roadmap; parallel-safe work identified; standards reduce rework | velocity collapses | Leadership | OPEN |
| 30 | **License compliance drift** — dependencies/datasets added without notice | M | M | THIRD_PARTY_NOTICES; DependencyUpdates.md; license check tooling (§202) | unlicensed dependency ships | Legal/Data | MONITOR |

## Top escalation paths

- Risks 1 + 2 + 3 compound: **world scope without engine ADR + without slice**
  is the project's biggest failure mode. Gate: engine evaluation → slice.
- Risk 4 + 16 compound: **two-app drift + fragmentation** make every later
  change more expensive. Gate: consolidation decision.
- Risk 9 + 17: **unverified platforms + perf** → release quality. Gate: BLOCKED
  list + budget measurements.

## Related

- Audit (TOP-100 risks): [project-audit.md](project-audit.md)
- Issues: `docs/planning/CURRENT_ISSUES.md` · Audit: `docs/planning/ENGINEERING_AUDIT.md` §6
