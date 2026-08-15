# Relationship Registry

**Status**: TARGET (blueprint for the edge layer; see ADR-0013)
**Source spec**: [Node Architecture master spec](../NODE_ARCHITECTURE.md) §79–§80, §149
**Purpose**: the controlled vocabulary of typed relationships. Every edge in the system
uses one of these types. **Do not use `related_to` where a more precise type exists** (§80).

> **Legend**: `S → T` = source nodeType family → target nodeType family. `dir` =
> direction (`→` directed, `↔` bidirectional semantics), `card` = cardinality per source
> node (1, n, m). `status` follows the registry legend (CURRENT/TARGET/FUTURE).
> Registry process: adding a type = a row here + validation rule + ADR note when storage
> changes.

## 1. Composition & structure

| type | S → T | dir | card | Meaning | Example | Inverse | Status |
|---|---|---|---|---|---|---|---|
| `contains` | any → any | → | n | Generic containment (typed!) | cell contains location; course contains lesson | `part_of` | CURRENT–TARGET |
| `contains_character` | vocabulary → kanji | → | n | Word contains this kanji | 食事 → 食・事 | `appears_in` | CURRENT |
| `contains_component` | kanji → component | → | n | Kanji is built from component | 食 → 人・良 (components) | `part_of` | TARGET |
| `uses_radical` | kanji → radical | → | n | Kanji uses radical | 駅 → 馬 | `part_of` | TARGET |
| `part_of` | child → parent | ← | 1 | Inverse of contains | location part_of cell | `contains` | CURRENT–TARGET |
| `parent_of` / `child_of` | hierarchy | ↔ | 1/n | Direct hierarchy edges (explicit alternative to part_of where a strict tree is needed) | region parent_of prefecture | `child_of` | TARGET |
| `belongs_to` | member → group | → | 1 | Membership without containment (collection, deck, series) | card belongs_to deck | `contains` | CURRENT |

## 2. Language content

| type | S → T | dir | card | Meaning | Example | Inverse | Status |
|---|---|---|---|---|---|---|---|
| `has_reading` | kanji/vocab → reading | → | n | A reading of the node | 食 → しょく / たべる | `belongs_to` | CURRENT |
| `has_meaning` | kanji/vocab → meaning | → | n | A gloss/meaning | 食 → "to eat" | `belongs_to` | CURRENT |
| `has_pitch` | vocab → pitch_pattern | → | 0..n | Pitch accent pattern | 箸 (H-L) vs 橋 (L-H) | `belongs_to` | TARGET |
| `has_frequency` | kanji/vocab → frequency_entry | → | 0..1 | Corpus rank | 食べる → rank 120 | `belongs_to` | TARGET |
| `has_jlpt` | kanji/vocab/grammar → jlpt level | → | 0..1 | JLPT level (estimated where dataset-based, §290) | 食 → N5 | — | CURRENT |
| `has_grade` | kanji → grade | → | 0..1 | School grade | 食 → grade 2 | — | CURRENT |
| `appears_in` | kanji/vocab → word/sentence/line | → | n | Occurrence (generic; prefer typed variants) | 食 appears_in 食事 | — | CURRENT |
| `appears_in_sentence` | word/kanji/grammar → sentence | → | n | Occurrence in example sentence | 駅 appears_in 駅前で待っています。 | `contains` | TARGET |
| `appears_in_media` | word/kanji → subtitle_line/scene | → | n | Occurrence in media | 電車 appears_in episode 3 line 12 | `contains` | TARGET |
| `appears_in_scene` | word/kanji → scene | → | n | Occurrence in a scene | おにぎり appears_in scene 4 | `contains` | TARGET |
| `demonstrates` | sentence → grammar | → | n | Sentence illustrates grammar | 駅前で待っています。 demonstrates 〜で (location) | `referenced_by` | TARGET |
| `synonym_of` | vocab ↔ vocab | ↔ | n | Synonyms | 綺麗 ↔ きれい | `synonym_of` | TARGET |
| `antonym_of` | vocab ↔ vocab | ↔ | n | Antonyms | 高い ↔ 安い | `antonym_of` | TARGET |
| `conjugates_to` | vocab/grammar ↔ conjugation | ↔ | n | Conjugation relation | 食べる → 食べたい / 食べられる / 食べさせた | `conjugates_to` | TARGET |
| `derived_from` | derived node → source node | → | n | Aggregation/derivation (knowledge, aggregates) | knowledge_state derived_from reviews | `generates` | TARGET |
| `related_to` | any → any | ↔ | n | **Escape hatch only** — soft association with no better type | culture trivia links | `related_to` | TARGET (linted) |

## 3. World & content

| type | S → T | dir | card | Meaning | Example | Inverse | Status |
|---|---|---|---|---|---|---|---|
| `located_at` | object/npc/location → location | → | 1 | Physical placement | station located_at district | `contains_location` | TARGET |
| `contains_location` | location → location | → | n | Spatial containment (typed part_of for the world tree) | prefecture contains_location city | `located_at` | TARGET |
| `represents` | world object → language node | → | 1..n | World ↔ knowledge bridge (§149) | shop sign represents 駅 | `references` | TARGET |
| `depicts` | photograph/screenshot → object/node | → | n | Photo/screenshot content | photo depicts おにぎり | `depicted_in` | TARGET |
| `references` | content → node | → | n | Citation/link without ownership | quest references station | `referenced_by` | TARGET |
| `imported_from` | node → source/dataset/integration | → | 1 | Provenance (§78) | vocab imported_from jmdict | `generates` | CURRENT |
| `generated_from` | node → source/dataset | → | 1 | Pipeline provenance | card generated_from mining_event | `imported_from` | CURRENT |
| `mined_from` | card → media/scene/line/photo | → | 1 | Card origin in media | card mined_from subtitle_line | `generates` | CURRENT, partial |
| `mapped_to` | external ↔ internal node | ↔ | 1 | Adapter mapping (Anki/Yomitan/AniList ids) | anki note mapped_to note | `mapped_to` | TARGET |

## 4. Learning & progression

| type | S → T | dir | card | Meaning | Example | Inverse | Status |
|---|---|---|---|---|---|---|---|
| `teaches` | lesson/quest/dialogue → node | → | n | Content teaches a node (explicit) | lesson teaches 食べる | `learned_via` | TARGET |
| `reviews` | card/review → node | → | n | Card/review practices a node | card reviews 食 | `reviewed_by` | CURRENT |
| `mastered_by` | node → user_knowledge | → | 1 | Node's mastery record | 食 mastered_by user_knowledge(食,reading) | `owns` | TARGET |
| `encountered_by` | node → user/discovery | → | n | Exposure record (Journey/media/study) | 電車 encountered_by discovery | `discovered_by` | TARGET |
| `discovered_by` | node → user/discovery | → | n | Discovery record | location discovered_by user | `encountered_by` | TARGET |
| `scheduled_at` | npc/vehicle/event → time slot | → | n | Schedule placement | npc scheduled_at afternoon-slot | `has_schedule` | TARGET |
| `unlocks` | quest/reward → node/quest/location | → | n | Unlock gate | quest unlocks map district | `requires` | TARGET |
| `rewards` | quest → reward | → | n | Quest reward | quest rewards camera filter | `unlocks` | TARGET |
| `requires` | node → node | → | n | Hard prerequisite (also story beats, lessons) | story_beat requires quest | `unlocks` | TARGET |
| `precedes` / `follows` | story_beat/lesson ↔ story_beat/lesson | ↔ | 1 | Linear ordering | chapter precedes chapter | `follows` | TARGET |
| `participates_in` | npc/player → quest/story/activity | → | n | Role in an activity | npc participates_in quest | `has_participant` | TARGET |
| `depends_on` | system → system | → | n | Operational dependency (packages, plugins, flags) | world package depends_on engine version | — | TARGET |

## 5. Registry rules

1. **Directionality is semantic**: `contains` reads "S contains T"; traversal is cheap in
   both directions via indexes (§79) regardless of semantic direction.
2. **`related_to` is linted**: validation (§148) fails when a more precise type exists
   for the same pair.
3. **Cardinality is a constraint, not a hint**: violations are validation errors.
4. **Provenance on edges**: imported edges carry `confidence` and `source`; derived edges
   must be explainable (`generated_from`/`derived_from`).
5. **Deletion policy per type**: containment/membership edges cascade with the parent;
   exposure/provenance edges tombstone (history must survive — §84 evidence, §111
   discovery); mapping edges rewire on re-import. Recorded per type during storage design
   (ADR-0013).

## 6. The two-graph bridge (§149)

Edges that cross between the **Language Knowledge Graph** and the **World Graph**:

| Edge | From (world) | To (language) |
|---|---|---|
| `represents` | object/location/sign | vocabulary/kanji/grammar |
| `encountered_by` / `discovered_by` | discovery | any language node |
| `mined_from` | card (via mining) | media line → language nodes |
| `appears_in_media` / `appears_in_scene` | media | language nodes |
| `teaches` (ambient) | dialogue/quest/activity | language nodes |

These five edge families are the product (NODE §150): every other subsystem consumes
them. Build them first (ADR-0013, TODO → knowledge graph).
