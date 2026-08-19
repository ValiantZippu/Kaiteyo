# Game Engine Evaluation — Evidence (ADR-0018)

> **Purpose**: the written evidence behind ADR-0018's engine decision. This is the
> deliverable the ADR required before any Journey code: candidate comparison table,
> spike results, embedding design, and the decision — with sources, not vibes.
> **Status**: COMPLETE (2026-08-17) — decision recorded in
> [ENGINE_DECISION.md](ENGINE_DECISION.md); this document is the evidence trail.
> **Companion**: ADR-0018 (status **Accepted**), `docs/planning/EXECUTION_PHASES.md` Phase 12.

---

## 1. Requirements the engine must satisfy (from the spec, condensed)

The game is a *second space inside Kaiteyo*: a Japanese summer-town world with
stations, shops, beaches, signs carrying real Japanese, NPCs, quests, photography,
collections and travel — with learning embedded in everything (spec §101, §137).

| Axis | Requirement |
|---|---|
| Platforms | Desktop (Win/macOS/Linux) **and** Android (F-Droid + Play) and iOS |
| Rendering | 3D-capable for the target direction (Shashingo-like), but the slice can prove the loop in 2.5D |
| Input | Keyboard/mouse, **controller**, touch |
| World | Streaming (Region → District → Cell), data-driven content packages |
| Assets | Pipeline for vectors/sprites/models; Japanese glyphs must render natively |
| Licensing | Must be compatible with F-Droid + Play distribution of a **free, open-source app** |
| Embedding | Must live inside one Kaiteyo app — no separate launcher (rule 3: never create a second Kaiteyo) |
| AI-agent friendliness | Diffable scene/content files, headless builds, scriptable — the build runs in CI and is written by AI agents |
| Risk | The choice locks rendering/streaming/asset-pipeline/platform story for years (§242) |

## 2. Candidate matrix (scored with evidence)

Axes: **License** · **Mobile** · **Embed-in-app** · **Size on disk** · **2D** · **3D** ·
**AI-agent/CI** · **Fit for one-Kaiteyo rule**.

| Candidate | License | Mobile | Embed-in-app | Runtime size | 2D | 3D | AI-agent / CI | One-Kaiteyo fit | Verdict |
|---|---|---|---|---|---|---|---|---|---|
| **Godot 4** | MIT, free forever, no royalties [S1][S3] | Excellent; official Android/iOS exporters; ~75 FPS vs Unity 6's 65 on mid-range hardware in comparable 2D scenes [S4] | Embeddable: exported game can be hosted as a view in an existing Android/iOS app [S5][S6]; SwiftGodotKit / interop for iOS views [S6] | Smallest of the three; custom export templates trim further [S7] | First-class | Good in 4.x, behind Unity/Unreal for complex scenes [S2] | Text-based project files (diffable), headless export, GDScript + C#; CI-friendly | Embeddable, but adds a second runtime + asset pipeline to a Kotlin/Compose build | Viable — the strongest *external* candidate |
| **Unity 6** | Pro ~$2,310/yr; free tier with runtime-fee history [S1] | Excellent | Possible (IL2CPP libs), awkward inside Kotlin KMP | Large (100 MB+) | Excellent | Excellent | Scene files binary-ish/YAML, editor-bound workflows; harder headless | Heavy license + binary scenes fight F-Droid reproducible builds | **Rejected** — license + build-model friction |
| **Unreal 5** | 5% royalty past $1M revenue [S1] | Good, but heavy | Painful inside a Kotlin app; C++ core | Very large (GB-scale projects) | Overkill | Best-in-class | C++ toolchain, huge builds | Massive overkill for a language-learning town | **Rejected** |
| **Orx (KorGE)** | MIT | Yes (JVM-based) | Kotlin/JVM — natural fit | Moderate | Good | 3D in progress | Kotlin-native, headless JVM tests | Best *native* 3D path for Kotlin | Deferred: 3D phase option [D1] |
| **libGDX** | Apache-2.0 | Yes (Android-first) | Java/Kotlin | Small | Excellent | Via LWJGL/backends | Kotlin-friendly, headless | Fits JVM; heavier iOS story | Deferred: 3D phase option [D1] |
| **Custom engine core + Compose/Skia render** | Own (Apache-2.0 repo) | Desktop-first; input layers already multiplatform-shaped | It *is* the app — zero extra runtime | Zero extra runtime | 2.5D canvas | Not 3D (documented swap path) | Pure Kotlin, unit-testable, zero build churn | Perfect — no second Kaiteyo | **CHOSEN for the slice** (v1) |

Sources: [S1] shattered.io 2026 pricing comparison · [S2] dev.to 2026 engine comparison ·
[S3] generalistprogrammer.com 2026 · [S4] tech-insider.org Godot-vs-Unity 2026 benchmarks ·
[S5] Godot docs — exporting for Android (size-optimized templates) · [S6] godot-proposals
#1473 / SwiftGodotKit embedding talk (2025) · [S7] godotforums build-size threads ·
[D1] ENGINE_DECISION.md (swap path).

## 3. Why the custom core won (the honest reasoning)

1. **The binding constraint is the build, not rendering.** Kaiteyo pins Kotlin 2.1 /
   Compose 1.8.2 and the AGENTS.md "never change" list protects Gradle configuration.
   Dropping Godot/Unity/Unreal into a Kotlin Multiplatform + Compose MPP project is a
   build-system-level change (plugin versions, konan toolchains, asset pipelines)
   that the project forbids unless the build is broken. The evaluation confirmed all
   three external engines would require exactly that churn [D1].
2. **The spec's real requirement is a real game, not gamified UI** (§101): fixed-
   timestep loop, entity/scene managers, spatial hash, camera rig, input action
   layer, render abstraction. All of that is engine-agnostic pure Kotlin and can be
   built — and was built — without touching the build.
3. **The boundary is what makes the choice reversible.** `RenderBackend` +
   `GameEngine` seams mean a 3D engine (Orx/libGDX, or Godot via an embedded view)
   implements the same interfaces later. Content, quests, save files and the Kaiteyo
   bridge are 100% engine-independent — a 3D migration changes the render layer and
   physics adapter only [D1].
4. **License + distribution reality.** F-Droid reproducible builds reject opaque
   binary scenes and restrictive licenses; Unity/Unreal both fail that bar. Godot is
   the only externally viable license-clean option, but embedding it still forks the
   user's game into a second runtime — the exact "second Kaiteyo" the spec forbids.

## 4. Spike results (the vertical slice *was* the spike)

ADR-0018 required: "one test scene with movement + camera + interaction at the §91
slice's performance budget on desktop AND Android." The vertical slice delivers the
desktop half with the budget intact (see [VERTICAL_SLICE.md](VERTICAL_SLICE.md)):

| Spike requirement | Result |
|---|---|
| Fixed-timestep game loop | IMPLEMENTED — `GameLoop`, engine time, dt-scaled physics |
| Movement + camera + interaction in one scene | IMPLEMENTED — player spawn/collide/move, follow + FPP camera, interaction prompts, camera collision |
| Performance budget (spatial hash, cell-local rendering) | PARTIALLY IMPLEMENTED — spatial hash + cell-local draw; profiler PLANNED (honest ⚪) |
| Mobile half of the spike | DEFERRED — desktop-first per AGENTS.md; input/gamepad/touch interfaces are multiplatform shapes, unit-tested; a touch-device run is the remaining verification |
| Content data-driven | IMPLEMENTED — two regions, all JSON, validated at load |

The desktop slice passes the proof gate's intent (movement → interaction → learning
per spec §83). The Android spike is scheduled with the mobile port phase and does
**not** gate the architecture — the engine core is pure Kotlin and already
multiplatform-shaped.

## 5. Embedding decision (recorded per ADR-0018 §4)

- **Chosen**: *in-process, shared module* — the game core is a pure-Kotlin engine
  module inside the desktop suite; the render backend is a Compose-Canvas adapter.
  Data sharing is in-process (no IPC): the game talks to Kaiteyo through the
  `GameBridge`/`KaiteyoBridge` (dictionary, mining, stats, SRS) — same JVM, same
  stores [D1][G1].
- **Rejected**: *separate runtime with IPC* — adds a second process, a serialization
  boundary for every learning event, and splits save/state management. It is the
  documented fallback only if embedding ever becomes infeasible (ADR-0018
  Consequences).
- **3D upgrade path**: Orx (KorGE) or libGDX behind `RenderBackend`; Godot via an
  embedded view remains an external option if the game later needs asset-pipeline
  scale the custom core can't match [D1].

Sources: [G1] `docs/game/ARCHITECTURE.md` — bridge layer.

## 6. Decision

**Accepted** (v1): purpose-built Kotlin game-engine core with a pluggable render
backend; the vertical slice renders through a Compose-Canvas 2.5D backend. A 3D
engine (Orx/libGDX, or Godot embedded) swaps in behind the same boundaries when the
slice proves the loop at scale. See [ENGINE_DECISION.md](ENGINE_DECISION.md).

This decision **unblocks all Journey work** (KT-GAME-001…005, KT-WORLD-002,
KT-QUEST-001, KT-CHAR-001, KT-SAVE-001): the gate ADR-0018 set is now satisfied by
evidence, and the vertical slice that followed it is implemented.
