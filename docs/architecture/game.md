# Kaiteyo — Game (Journey / Kaiteyo World)

> **Status**: `PARTIALLY_IMPLEMENTED` (2.5D Canvas vertical slice) + `ARCHITECTED` (full world spec — NODE §86–§119).
> Companion: `core.md`, `data-model.md`, `events.md`, `stats.md`, `dictionary.md`, `../game/*`, `nodes/JOURNEY_WORLD_SCHEMA.md`, `WORLD_SYSTEM.md`.

## 1. What it is

A **real game** — not gamification — whose language-learning is the content, not a skin. World, player, progression, map, nodes, regions, quests, encounters, enemies, NPCs, rewards, inventory, skills, save system, offline mode, assets, rendering, performance — all documented as a full subsystem that **belongs to the same Kaiteyo ecosystem**.

Game is never a separate database universe.

## 2. Pillars (NODE §86)

Exploration · Observation · Discovery · Language · Culture · Story · Photography · Collection · Daily life. Light RPG progression for motivation — never XP grinding (NODE §116). Inspirations: Nintendo polish, Shashingo environment-language, Shin-chan daily life — not clones.

Every Journey feature must pass: *does it improve exploration, language, culture, story, discovery, or immersion?* If not, it may not belong.

## 3. What the player does

Explore a streamable Japan (Kamakura + Enoshima slice first), walk/look/interact, ride trains, visit locations/interiors, meet NPCs, read signs/dialogue, solve language quests, collect words/kanji/vocabulary, photograph, unlock regions, progress through quests — where every encounter can surface `DictionaryPopup` and `MiningService`.

## 4. Architecture

```
GameRuntime (isolated, crash-safe — suite GameEngine / core WorldRuntime)
  ├── World (streamable cells, LOD, Region→Prefecture→City→District→Cell→Location→Interior→InteractionNode)
  ├── Player (avatar, camera, movement)
  ├── NPCs (schedules, dialogue trees)
  ├── Quests (objectives, prerequisites, branching, non-punitive)
  ├── Dialogue (data-driven trees, conditions)
  ├── Inventory/Skills/Collections
  ├── SaveSystem (versioned, checksummed, atomic)
  └── Bridge (KaiteyoBridge / GameBridge) → Kaiteyo Core
```

Current: CanvasRenderer 2.5D (Sakamura map, WASD+gamepad+touch, patrols/weather, dialogue TTS). Target: renderer-agnostic `RenderBackend` → swappable 3D (ADR-0018 `PROPOSED`).

## 5. Kaiteyo Core connections (load-bearing)

| Game concept | Kaiteyo Core | How |
|--------------|-------------|-----|
| Game activity | ActivityService | `GameNodeCompleted`, `StudySessionStarted` → `ActivityEvent` → Stats |
| Vocabulary/kanji | DictionaryService | `WORLD_TEXT_SELECTED` → `DictionaryService.lookup` → popup in world |
| Reward/progression | DeckService / Study | quest reward → card/exp (never the only progression) → `fsrs_card` |
| Encounter knowledge | Knowledge model (`KNOWLEDGE_STATE_MODEL.md`) | difficulty adapts to known kanji/vocab/grammar/mistakes — never re-teaches known |
| Game content | Library where appropriate | discovered words/locations as `Content` + `LibraryItem` (opt-in) |
| Stats | StatisticsService | derived from same ActivityEvents — Game never owns a second stats table |

Data: never duplicate; Game save references knowledge-graph state, not copies it.

## 6. Data model (selected)

```kotlin
data class GameNode(val id: String, val kind: NodeKind, val worldPosition: WorldPosition, val languageHints: LanguageHints)
data class Quest(val id: String, val prerequisites: List<String>, val objectives: List<Objective>, val rewards: List<Reward>, val branching: Branch?)
data class SaveData(val version: Int, val player: PlayerState, val discoveredNodes: Set<String>, val quests: QuestState, val inventory: Inventory, val worldClock: WorldClock, val languageKnowledgeRef: String /* → knowledge graph snapshot id */)
interface RenderBackend { fun render(scene: Scene, camera: Camera) } // Canvas today, 3D later
```

## 7. Persistence / save system

- Saves: versioned JSON, checksummed, atomic writes (temp file → rename), corruption recovery (null fallback, never crash), per-slot files.
- World state ↔ save mapping in `docs/game/save-system.md`.
- Knowledge is referenced, not duplicated — the save stores a graph snapshot id + delta.

## 8. Offline

Fully offline (local world cells + save). Platform/online features (if ever) are enhancement, not requirement. Spec: offline-first per `core.md`.

## 9. Performance

Per-tier budgets (Low/Medium/High/Ultra) in `docs/rendering/rendering-performance.md`; streaming budget for cells; LOD; occlusion; texture compression; mesh optimization.

## 10. Failure states

Cell load failure (retry, show placeholder), save corrupt (recover previous version), world crash (isolated runtime — app survives, error in save/debug), missing asset (placeholder + log).

## 11. Evolution

New region → new content package (ADR-0015), no engine rewrite. New quest/dialogue language requirement → node `requires` edge + graph query. Engine swap → `RenderBackend` boundary.

## 12. Implementation order

Per `phases.md` / `game/ROADMAP.md`: foundation → Kamakura+Enoshima vertical slice → adjacent locations → larger city → regional → multi-region. Engine choice (ADR-0018) gates runtime work.
