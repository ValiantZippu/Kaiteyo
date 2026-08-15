# Kaiteyo Node Architecture — Master Specification

**Status**: TARGET ARCHITECTURE — blueprint, not implementation
**Date**: 2026-08-15
**Scope**: This document is the product/architecture specification for the node system, the
Journey world, the UX principles, and the product integration model — sections **§76–§162**
of the Kaiteyo master specification. It is the direct predecessor of the
[Kaiteyo Engineering Standards](../engineering/ENGINEERING_STANDARDS.md) (§163–§376), which
governs *how* everything here gets built.

> **READ THIS FIRST.** Before implementing anything described here, read
> `docs/engineering/ENGINEERING_STANDARDS.md` (the constitution), `docs/planning/ENGINEERING_AUDIT.md`
> (the current repository map), and `docs/planning/PRODUCT_AUDIT.md` (what is real today).
> **Nothing in this document is implemented.** The current repository has two SQLDelight
> databases, FSRS-5 scheduling, and a Compose MPP UI — it does **not** have a node system,
> a knowledge graph, or a Journey world. Those are the *target* this document specifies.
> Honest labeling rules are §158 and §159.

Section numbers are stable identifiers. Refer to them as `NODE §NNN`. The engineering
constitution sections are referred to as `STANDARDS §NNN`. Where this document says
"see registry", it means the deep reference documents in
[`docs/architecture/nodes/`](nodes/README.md).

---

# 76. KAITEYO NODE ARCHITECTURE — MASTER SPECIFICATION

The node system is not a decorative graph.

It is one of the fundamental architectural concepts of Kaiteyo.

Kaiteyo should be designed around the idea that almost everything meaningful can be
represented as a node with typed relationships.

The node system must support:

- dictionary
- learning
- media
- Journey
- world
- quests
- stories
- exams
- decks
- statistics
- user knowledge
- collections
- discovery
- content authoring

A node must never become a generic "thing" with hundreds of unrelated nullable fields.

Use typed node families.

### In depth — what "node system" means for the repository

- **Conceptual model, not a mandate to rewrite the database.** The current `AppDataDatabase`
  and `UserDataDatabase` (see `docs/data/ARCHITECTURE.md`) remain the storage of record.
  The node system is the *identity and relationship layer* that sits on top — either as
  normalized tables added to the user database, as a read-model over app data, or as a
  hybrid. The decision on storage mechanics is ADR-0013.
- **Every meaningful thing is addressable.** A kanji, a subtitle line, a quest, a photo, a
  deck, a review, an NPC, a discovery — all get a stable identity and typed relationships,
  so any subsystem can consume any other subsystem's data without duplicating it
  (§152 — "can another subsystem consume this without duplicating data?").
- **Typed families are the anti-"god object" rule.** A node's `nodeType` determines its
  schema. No nullable soup. Family registry: `docs/architecture/nodes/NODE_TYPE_REGISTRY.md`.
- **Provenance is non-negotiable** (see §78): every node must be traceable to a source
  (a dataset, the app itself, a user, or the world content).
- **Acceptance criteria**
  - A `Node` type exists in the domain layer with `nodeType`, stable `id`, and schema-version
    discipline (see §78).
  - Every subsystem named above can be described as nodes + typed relationships without
    exception and without introducing new special cases in the node model.
  - No node type has more than one "nullable catch-all" field; everything else is typed.
- **Dependencies**: domain model (STANDARDS §365 phase 3) → database (phase 4) → knowledge
  graph (phase 6). **Status**: TARGET. **TODO**: `docs/planning/TODO.md` → Node & Journey.

---

# 77. NODE FAMILY HIERARCHY

Define a hierarchy similar to:

```
LANGUAGE
├── Script
├── Kana
├── Kanji
├── Component
├── Radical
├── Vocabulary
├── Expression
├── Reading
├── Meaning
├── Grammar
├── Conjugation
├── Sentence
├── Paragraph
├── Story
├── Pitch Pattern
├── Frequency Entry
└── Pronunciation

LEARNING
├── Course
├── Lesson
├── Topic
├── Objective
├── Exercise
├── Question
├── Exam
├── Deck
├── Note
├── Card
├── Review
├── Study Session
├── User Knowledge
└── Mastery State

MEDIA
├── Media Source
├── Series
├── Anime
├── Movie
├── Episode
├── Video
├── Audio
├── Subtitle Track
├── Subtitle Line
├── Scene
├── Screenshot
├── Clip
└── Mining Event

WORLD
├── World
├── Region
├── Prefecture
├── City
├── District
├── Neighborhood
├── Map Cell
├── Street
├── Building
├── Interior
├── Landmark
├── Station
├── Road
├── Railway
├── Beach
├── Park
├── Shop
├── Restaurant
├── School
├── Aquarium
├── Shrine
├── Temple
└── Natural Feature

GAMEPLAY
├── Player
├── Avatar
├── NPC
├── NPC Schedule
├── Interaction
├── Activity
├── Quest
├── Quest Objective
├── Story
├── Story Beat
├── Dialogue
├── Discovery
├── Collection
├── Photograph
├── Achievement
├── Reward
├── Event
├── Season
├── Weather State
└── Day/Night State

USER
├── Profile
├── Preferences
├── Goal
├── Knowledge State
├── Study History
├── Media History
├── Journey Progress
├── Discovery History
├── Quest Progress
├── Exam History
├── Deck Ownership
└── Collection Ownership

SYSTEM
├── Source
├── Dataset
├── Integration
├── Plugin
├── Theme
├── Asset
├── Feature Flag
└── Configuration
```

This hierarchy must be documented and kept extensible.

### In depth — how the hierarchy maps to reality

- **LANGUAGE** — largely **CURRENT**: the KJD pipeline and `AppDataDatabase` already model
  kana, kanji, vocab, readings, meanings, sentences (letters/vocab tables); pitch, grammar,
  conjugations, components and radicals are **TARGET** (partially available in datasets,
  see `docs/data/SOURCES.md`).
- **LEARNING** — largely **CURRENT** in `UserDataDatabase` (decks, cards, reviews, FSRS
  state). `Course`/`Lesson`/`Topic`/`Objective`/`Exercise`/`Exam` are **TARGET** (exam
  generation exists in the statistics engine; formal course model does not).
- **MEDIA** — **CURRENT, partial**: the desktop suite has media bookmarks, audio clips,
  subtitle parsing; a formal `Series/Episode/Scene/SubtitleLine` node graph is **TARGET**.
- **WORLD + GAMEPLAY** — **TARGET**: nothing exists. Full target model in
  `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md`.
- **USER** — **CURRENT, partial**: profile/preferences/study history exist; goals, journey
  progress, discovery history, quest progress, collection ownership are **TARGET**.
- **SYSTEM** — **CURRENT, partial**: themes, config, feature flags exist in some form;
  `Source`/`Dataset`/`Integration`/`Plugin` node records are **TARGET** (KJD keeps
  provenance; see `docs/data/SOURCES.md`).

The full machine-readable registry with fields per type is
`docs/architecture/nodes/NODE_TYPE_REGISTRY.md`.

- **Extensibility rules**
  - New families must be proposed through the registry process (a table row + ADR note if
    it changes storage).
  - A node type's `schemaVersion` allows evolution without breaking consumers.
  - No family may depend on another family's *implementation*; only on its node contract.
- **Acceptance criteria**
  - Every registry type lists: family, parent, required/optional fields, sources, typical
    relationships, status.
  - `Browse`, `Dictionary`, `Library`, `Stats`, `Journey` can each be implemented purely
    against the registry, without reaching into another subsystem's internals.

---

# 78. UNIVERSAL NODE CONTRACT

Define a base conceptual contract.

Every persistent node should have:

```
id
nodeType
schemaVersion
createdAt
updatedAt
source
sourceId
status
metadata
```

Where appropriate:

```
parentId
ownerId
worldId
language
locale
tags
relationships
```

Do not force fields onto nodes that do not need them.

Every node must have explicit provenance.

For external data:

```
source = "jmdict"
sourceId = external identifier
```

For internally-created content:

```
source = "kaiteyo"
```

For user-created content:

```
source = "user"
```

For Journey content:

```
source = "kaiteyo-world"
```

### In depth — the contract as a schema

Field-level contract (types are target-domain types, storage may be SQL/JSON depending on
ADR-0013):

| Field | Type | Required | Semantics |
|---|---|---|---|
| `id` | stable UUID (v7, time-ordered) | always | Globally unique; never reused, never mutated |
| `nodeType` | enum (registry) | always | Exactly one type from `NODE_TYPE_REGISTRY.md` |
| `schemaVersion` | int | always | Version of the node-type schema this node conforms to |
| `createdAt` / `updatedAt` | ISO-8601 | always | Creation and last mutation |
| `source` | enum: `jmdict` / `kanjidic` / `kjd` / `kaiteyo` / `kaiteyo-world` / `user` / `integration` | always | Provenance origin (see §159 — never fabricate) |
| `sourceId` | string | when `source != kaiteyo` | Identifier in the source system |
| `status` | enum: `active` / `archived` / `suspended` / `hidden` / `draft` | always | Lifecycle, not knowledge state |
| `metadata` | typed map | optional | Family-specific small data; must be schema-validated per nodeType |
| `parentId` | node id | where hierarchical | e.g. Reading → Kanji; Episode → Series |
| `ownerId` | user id | user-created nodes | Ownership for `source = "user"` |
| `worldId` | world id | world nodes | Which world package (§145) provided this node |
| `language` / `locale` | BCP-47 | language content | Which language/locale a node belongs to |
| `tags` | string set | optional | Free-form but registry-recommended |
| `relationships` | relationship list | optional | Explicit typed edges (see §79); may live in a separate store |

Rules:

- A node's *identity* is immutable; only `status`/`metadata`/`relationships` change over
  time. Corrections to content create a new node version (same `id`, bumped `schemaVersion`
  or a `supersedes` relationship) rather than mutating history.
- Provenance rule: `source` must be set and truthful. Derived/aggregate data (e.g. "user
  knowledge score") is never `source = "jmdict"` — it is `source = "kaiteyo"` and must
  reference the contributing nodes via `derived_from` relationships.
- Do **not** force `relationships` to be inline JSON on every node; the relationship store
  (§79) may be separate. The contract is conceptual identity + provenance, not one blob.

- **Acceptance criteria**
  - Validators reject nodes without `id`, `nodeType`, `schemaVersion`, `source`.
  - Two systems reading the same node agree on its meaning without reading each other's
    code.
  - Node versioning preserves auditability: you can always answer "where did this come from,
    when, and what changed".

---

# 79. RELATIONSHIP MODEL

Relationships must also be first-class concepts.

Example:

```
KANJI
|
+--contains--> COMPONENT
|
+--has_reading--> READING
|
+--appears_in--> WORD
|
+--appears_in--> SENTENCE
|
+--appears_in--> MEDIA_SUBTITLE
|
+--represented_by--> WORLD_OBJECT
|
+--studied_by--> USER_KNOWLEDGE
```

Relationships should support:

```
id
type
sourceNode
targetNode
metadata
confidence
source
createdAt
updatedAt
```

Where useful:

```
weight
frequency
ordering
position
direction
context
```

### In depth — the edge model

| Field | Type | Semantics |
|---|---|---|
| `id` | stable UUID | Unique edge identity (edges are first-class and addressable) |
| `type` | enum (relationship registry) | One type from `RELATIONSHIP_REGISTRY.md` |
| `sourceNode` / `targetNode` | node id | Directed: `source --type--> target` |
| `metadata` | typed map | Edge payload (e.g. stroke index for `contains_component`, sentence index for `appears_in_sentence`) |
| `confidence` | float 0..1 | Data quality/confidence — REQUIRED for imported/probabilistic edges, optional for curated ones |
| `source` | provenance enum | Same provenance discipline as nodes |
| `createdAt` / `updatedAt` | ISO-8601 | Timestamps |
| `weight` / `frequency` / `ordering` / `position` | numeric | Relevance or ordering hints (e.g. word frequency, sentence position, stroke order) |
| `direction` | `out` / `in` / `bidirectional` | Semantic directionality of the type |
| `context` | string/JSON | Optional usage context (e.g. "sentence 3, subtitle track 2") |

Storage options (decide in ADR-0013): an `edge` table in `UserDataDatabase` for
user-generated edges; a read-model/derived index over `AppDataDatabase` for dictionary
edges; hybrid. Edges must be indexable by `sourceNode` and `targetNode` and by `type`
(needed for the node-exploration UX in §81–§83).

- **Acceptance criteria**
  - Traversal in both directions is cheap: "kanji → words containing it" and "word → its
    kanji" both resolve without full scans (indexes, per STANDARDS §192).
  - Edges can be filtered by `type`, `source`, `confidence`, and `status` of the target.
  - Deleting a node must define what happens to its edges (cascade, rewire, or tombstone)
    — pick per relationship type; never silently corrupt the graph.

---

# 80. RELATIONSHIP TYPES

Create a controlled relationship vocabulary.

Examples:

```
contains
contains_character
contains_component
uses_radical
has_reading
has_meaning
has_pitch
has_frequency
has_jlpt
has_grade
appears_in
appears_in_sentence
appears_in_media
appears_in_scene
demonstrates
synonym_of
antonym_of
related_to
derived_from
conjugates_to
part_of
parent_of
child_of
precedes
follows
requires
teaches
reviews
mastered_by
encountered_by
discovered_by
located_at
contains_location
unlocks
rewards
references
mined_from
generated_from
imported_from
mapped_to
represents
depicts
belongs_to
scheduled_at
participates_in
depends_on
```

Do not use "related_to" where a more precise relation exists.

### In depth — a controlled vocabulary, not a free-for-all

- The full registry (semantics, direction, cardinality, examples, status) lives in
  `docs/architecture/nodes/RELATIONSHIP_REGISTRY.md`. Every type listed above is defined
  there.
- **`related_to` is the escape hatch — and the smell.** It is allowed only for genuinely
  soft associations. When an author wants `related_to`, they must first try to find (or
  propose) a precise type: `synonym_of`, `antonym_of`, `conjugates_to`, `derived_from`,
  `appears_in_sentence`, etc.
- New relationship types are added through the registry process, same as node types.
- **Acceptance criteria**
  - A lint/validation check fails when `related_to` is used and a more precise type from
    the registry exists for that source/target combination.
  - Every relationship type used in code/data appears in the registry with a definition;
    nothing is coined inline (per STANDARDS §370 — do not invent schemas without
    documenting them).

---

# 81. NODE NAVIGATION EXPERIENCE

The node system must be visible to the user where useful.

Example:

```
食べる

→ 食
→ 食事
→ 食堂
→ 食べ物
→ 食べ方
→ 食べたい
→ 食べられる
→ sentences
→ anime scenes
→ Journey objects
→ decks
→ user's mastery
```

The user should be able to continue exploring.

This should feel like traversing a living language database.

It should NOT feel like browsing a developer database.

UX principle:

Complexity should be underneath the interface.

The user sees simple concepts.

The system maintains the complexity.

### In depth — turning the graph into navigation

- Every lookup result (dictionary, browse, subtitle, Journey object) is a **node anchor**:
  it offers "related" traversal chips — kanji, words, conjugations, sentences, media,
  Journey objects, decks, mastery — one click away. This is the "living database" feel.
- Traversal is **bounded and ranked**: never dump 2,000 edges; show ranked, typed,
  previewable chips with progressive disclosure (§121).
- Each traversal hop must be cheap and must record a lightweight event
  (`NodeTraversed`), which feeds Browse suggestions and statistics (STANDARDS §210).
- **Acceptance criteria**
  - From any dictionary entry, the user can reach: its kanji components, its conjugations,
    example sentences, media appearances, and their own mastery, in ≤ 2 clicks.
  - The UI never shows raw node identifiers or edge tables to the user.

---

# 82. KANJI NODE EXPERIENCE

A Kanji page should not be merely:

```
character
meaning
readings
stroke order
```

It should become an exploration hub.

Example:

```
食

Overview
Writing
Readings
Words
Components
Radicals
Grammar
Sentences
Media
Frequency
JLPT
User Knowledge
Practice
Journey Discoveries
```

The page should allow natural traversal.

Example:

```
食
→ 食べる
→ 食事
→ 食堂
→ 食べ物
→ 食べられる
→ 食べさせる

and:

食
→ component
→ other Kanji sharing component

and:

食
→ anime scene
→ subtitle
→ screenshot
→ card
```

### In depth — kanji page contract

Sections and their backing relationships:

| Page section | Backing node/edge data | Status today |
|---|---|---|
| Overview | Kanji node + reading/meaning edges | CURRENT (kanji details exist) |
| Writing | stroke data + stroke practice (STANDARDS §284–§285) | CURRENT, partial |
| Readings | `has_reading` edges | CURRENT |
| Words | `appears_in` → Vocabulary (ranked by frequency) | CURRENT, partial |
| Components / Radicals | `contains_component`, `uses_radical` | TARGET (dataset-dependent) |
| Grammar / Conjugations | grammar nodes | TARGET |
| Sentences | `appears_in_sentence` (ranked by usefulness) | TARGET (needs example-sentence dataset, `RESEARCH` in TODO) |
| Media | `appears_in_media` via subtitle index | TARGET |
| Frequency / JLPT | `has_frequency`, `has_jlpt` | CURRENT |
| User Knowledge | UserKnowledge node (§84) | TARGET as graph; FSRS data CURRENT |
| Practice | generates practice from knowledge state | TARGET |
| Journey Discoveries | `discovered_by`, `represented_by` | TARGET |

- **Acceptance criteria**
  - Every section degrades gracefully: absent data shows "not available yet" — never an
    empty broken panel, never fabricated data (§158, §159).
  - The page is reachable from every surface that mentions the kanji (dictionary, browse,
    subtitle, deck card, Journey object).

---

# 83. VOCABULARY NODE EXPERIENCE

Vocabulary page:

```
word
reading
meaning
pitch
frequency
JLPT
Kanji
grammar
conjugations
sentences
media
user knowledge
cards
practice
```

Include:

```
"Where have I seen this?"
```

Possible results:

```
Anime
Media
Journey
Previous reviews
Mined cards
```

### In depth — "where have I seen this?"

- This is the flagship *personalization* query: `appears_in_media` + `mined_from` +
  `reviewed_by` + `encountered_by` (Journey) edges merged, ranked by recency and context.
- It connects three worlds the user already lives in: media, study, and Journey — turning
  "I know this word" into "I know this word *because I saw it in these five places*".
- Each hit links back to its source (episode + timestamp, subtitle line, screenshot,
  Journey location, review date) so "where" is actionable, not decorative.
- **Acceptance criteria**
  - The query completes within the search latency budget (STANDARDS §188) at full dataset
    scale.
  - Hits are grouped by world (Media / Journey / Study) with per-hit provenance and a
    direct jump action.
  - A word the user has never encountered anywhere shows the section as "not yet
    encountered" — with a path to encounter it (suggested media/Journey content) instead
    of a wall of zeros.

---

# 84. USER KNOWLEDGE NODE

User Knowledge is not simply:

```
known = true/false.
```

Use a richer state.

Possible state:

```
UNSEEN
ENCOUNTERED
EXPOSED
LEARNING
WEAK
FAMILIAR
RECOGNIZED
PRODUCED
LISTENING_RECOGNIZED
READING_RECOGNIZED
WRITING_CAPABLE
STRONG
MASTERED
DORMANT
FORGOTTEN
```

Knowledge dimensions:

```
reading
writing
listening
recognition
production
context
meaning
pronunciation
```

A user may:

```
recognize 食

but not:

write 食

or:

recognize 食べる

but not:

produce 食べられない.
```

These are different knowledge states.

### In depth — knowledge state machine

Full model: `docs/architecture/nodes/KNOWLEDGE_STATE_MODEL.md`. Key decisions:

- **Knowledge is per-dimension, per-node.** A `UserKnowledge` node exists per
  (user, language node, dimension). So `食|reading` and `食|writing` are separate states —
  that is what makes "recognize but not write" expressible.
- **States are ordered but not linear.** `LEARNING` may come from `ENCOUNTERED` or from a
  review failure; `FORGOTTEN` returns to the exposure pipeline, not necessarily to
  `UNSEEN`.
- **Relationship to FSRS (CURRENT).** FSRS-5 already tracks per-card scheduling state
  (new/learning/review/relearning + interval + stability). The knowledge state is a
  *wider* layer: FSRS drives *when* to review a card; UserKnowledge records *what the user
  can demonstrably do* with a node (recognize? write? produce?). Mapping:
  FSRS review success/failure feeds state transitions; the state never replaces FSRS
  scheduling (STANDARDS §6, "never change SRS algorithm logic").
- **Triggers that move states** (examples): dictionary lookup → `ENCOUNTERED`; first
  successful recognition review → `RECOGNIZED`; successful production/writing review →
  `PRODUCED` / `WRITING_CAPABLE`; Journey interaction → `EXPOSED`; long inactivity →
  `DORMANT`; failed review after mastery → `FORGOTTEN`.
- **Acceptance criteria**
  - The system can answer, per node and dimension: current state, last transition,
    transition history, and contributing evidence (events).
  - No UI ever asks the user to self-rate against this machine; it is derived from real
    events (STANDARDS §210) — self-report is optional auxiliary signal only.

---

# 85. KNOWLEDGE SCORE

Do not reduce learning to one arbitrary percentage.

Possible dimensions:

```
Kanji recognition
Kanji writing
Vocabulary recognition
Vocabulary production
Listening
Reading
Grammar
Pitch
Contextual understanding
```

Display simplified summaries to users.

Internally maintain richer state.

### In depth — scoring model

- **Internal**: per-dimension scores are derived, not stored as truth (STANDARDS §213:
  raw events → aggregation → derived metrics). Each dimension aggregates the knowledge
  states of its nodes (weighted by frequency/recency), plus review performance
  (FSRS stability/retrievability where available).
- **Display**: users see a small number of simplified dials (e.g. Kanji, Vocabulary,
  Listening, Reading, Writing) with clear labels like "estimated" where the underlying
  data is partial (STANDARDS §290 — never fabricate precision).
- **No single "Japanese level" number** unless it is explicitly labeled as an estimate
  with its basis (e.g. "based on studied content: ~N4 vocabulary coverage").
- **Acceptance criteria**
  - Every displayed score can be drilled down to the evidence behind it.
  - Two different scoring questions ("what should I review", "what do I know") use
    different derived views and never collapse into one fake percentage.

---

# 86. GAME DESIGN PHILOSOPHY

JOURNEY MUST NOT BECOME A GENERIC RPG.

It is primarily:

```
exploration
observation
discovery
language
culture
story
photography
collection
daily life
```

with light RPG-style progression for motivation.

The player should feel:

```
"I am living inside a Japanese learning world."
```

Not:

```
"I am grinding XP in an educational RPG."
```

### In depth — the philosophy as a guardrail

- **Design test (§153) applies to every Journey feature**: does it improve exploration,
  language, culture, story, discovery, or immersion? If not, it does not belong.
- Progression is evidence-shaped (see §116): the player levels up *knowledge, discoveries,
  collections, story, locations, photography* — never abstract XP. Reward systems must
  avoid predatory mechanics entirely (§117).
- This philosophy is consistent with the roadmap's existing non-goal: "no gamification
  gimmicks (points/badges/streaks as the core loop)" — Journey is exploration-first, not
  points-first.

---

# 87. JOURNEY CORE LOOP

Core loop:

```
EXPLORE
↓
NOTICE
↓
INTERACT
↓
UNDERSTAND
↓
LEARN
↓
DISCOVER
↓
COLLECT
↓
USE
↓
REMEMBER
↓
RETURN
```

Example:

```
Player walks into a convenience store.

Sees:

おにぎり

Interacts.

Learns:

おにぎり

Sees:

お
に
ぎ
り

Learns vocabulary.

Sees related items.

Takes photograph.

Adds discovery.

Optionally creates card.

Later sees the word in media.

Knowledge increases.

Statistics update.

Quest progresses.
```

This is the kind of integration required.

### In depth — the loop as data flow

Each step maps to concrete events and node operations (so "integration" is checkable):

| Loop step | User action | Nodes/edges created | Events emitted (STANDARDS §210) |
|---|---|---|---|
| EXPLORE | move through world | (cell streaming) | `LocationVisited` |
| NOTICE | object enters interaction range | interaction prompt (§139) | — |
| INTERACT | examine/talk/photograph | `Interaction`, maybe `Dialogue` | `InteractionStarted` |
| UNDERSTAND | glossary lookup (§140) | `EncounteredBy` (user→node) | `VocabularyEncountered`, `KanjiEncountered` |
| LEARN | optional deeper study | `UserKnowledge` transitions (§84) | `StudyStarted` |
| DISCOVER | new location/object recorded | `Discovery` node | `LocationDiscovered` |
| COLLECT | photo/collection entry | `Photograph`, `CollectionItem` | `PhotoTaken` |
| USE | word appears in review/media later | `AppearsInMedia` edges, reviews | `CardReviewed` |
| REMEMBER | knowledge strengthens | `UserKnowledge` state up | derived stats |
| RETURN | player comes back to the world | quest/story progress | `QuestProgressed` |

- **Acceptance criteria**
  - A single convenience-store interaction (the §87 example) exercises the full loop end to
    end: word → kanji breakdown → photo → discovery → optional card → later media match →
    stats/quest update.
  - Nothing in the loop requires a network connection (offline-first, STANDARDS §182).

---

# 88. JOURNEY WORLD STRUCTURE

Do not build "Japan" as one giant undivided map.

Use hierarchical streaming:

```
WORLD
↓
REGION
↓
PREFECTURE
↓
CITY
↓
DISTRICT
↓
MAP CELL
↓
LOCATION
↓
INTERIOR
↓
INTERACTION NODE
```

Example:

```
Japan
→ Kanagawa
→ Kamakura
→ Komachi
→ Cell 07
→ Convenience Store
→ Rice Ball Shelf
→ おにぎり node
```

### In depth — world hierarchy data model

Full schema: `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md` (§2). Key rules:

- The hierarchy is a **streaming tree**: only cells near the player are materialized
  (§92). Higher levels are metadata + geometry summaries until entered.
- Each level has a stable `worldId`-scoped identity, so content packages (§145) can add
  regions without renumbering anything.
- `INTERACTION NODE` is the leaf that carries language content — this is the seam where
  the world graph meets the knowledge graph (§149).
- **Acceptance criteria**
  - Adding a new region/city/district is a content-package addition (§145) — no engine
    code change, no schema migration.
  - Any node in the hierarchy is addressable by its path (e.g. `world/japan/kanagawa/
    kamakura/komachi/cell-07/shop-14/onigiri-shelf`).

---

# 89. WORLD MAP UX

World map should be beautiful.

Not a developer map.

Use:

```
stylized geography
clean typography
soft terrain
water
roads
railways
landmarks
discovery markers
quest markers
visited regions
unvisited regions
knowledge density
```

Map modes:

```
WORLD
REGION
CITY
DISTRICT
WALKING MAP
```

The map should transition smoothly between scales.

### In depth — map contract

- **Stylized, not satellite.** The map is an artistic representation (STANDARDS §249–§250:
  geographic truth separated from artistic representation; recognizable landmarks and
  layout over photogrammetric accuracy).
- **Zoom is continuous.** World → region → city → district → walking map is one smooth
  camera path, not page switches.
- **Overlays are contextual**: quest markers and discovery markers appear per mode;
  knowledge density (a soft heat of words you know vs. words available per area) is a
  toggleable layer that connects map to learning.
- Data sources for geographic truth must be licensed (OpenStreetMap or equivalent, with
  attribution — STANDARDS §248). **Status**: TARGET. **Flags**: EXTERNAL DEPENDENCY
  (geodata), ART PRODUCTION (stylized map art).
- **Acceptance criteria**
  - All map modes work with a controller, keyboard, and touch (STANDARDS §251).
  - Map transitions hold the FPS budget (§143) on the lowest supported device.

---

# 90. WORLD OF JAPAN

The long-term world may represent large portions of Japan.

However:

DO NOT attempt to create the entire country manually in one release.

Build a scalable world system.

Each update may add:

```
new region
new city
new district
new landmarks
new stories
new quests
new vocabulary
new cultural objects
```

The architecture must support adding regions without rewriting the engine.

### In depth — world expansion policy

- The world is **content**, not code (STANDARDS §361: data-driven content, no arbitrary
  code execution from packages). Regions ship as versioned world packages (§145).
- Expansion order is gated by the vertical slice (§91) and by content-production capacity —
  every new region is a CONTENT PRODUCTION + ART PRODUCTION + AUDIO PRODUCTION effort.
- Never release a half-finished region as "featured"; unshipped regions are simply absent
  from the map (no fake content, §158).

---

# 91. VERTICAL SLICE

Before attempting Japan:

Build one extremely polished vertical slice.

Recommended:

```
Kamakura + Enoshima.
```

The slice should contain:

```
streets
beach
railway
station
shops
temples
shrines
residential areas
aquarium-like attraction
ocean
NPCs
vehicles
trains
weather
day/night
photography
language nodes
quests
dialogue
collections
media-style contextual learning
```

The goal is to prove the architecture.

### In depth — slice acceptance criteria

The slice must prove every column of the architecture (mirrors STANDARDS §366):

1. **Movement & camera** — first/third person switch (§96) at stable FPS.
2. **World systems** — cells stream (§92), day/night + weather (§107–§108), one season.
3. **Interaction** — object → prompt → glossary → learning path (§87, §139–§140).
4. **Language** — every sign/object/line is a language node with dictionary linkage.
5. **Characters** — player character + a small cast of NPCs with schedules (§97–§98).
6. **Narrative** — one story arc + several quests (§100–§102).
7. **Collection/discovery** — photography, discoveries, collection entries (§95, §110–§111).
8. **Progression** — journal, rewards, map reveal (§116–§119).
9. **Persistence** — save/load across restarts (§144); learning data lives in shared user
   data, not the save.
10. **Performance** — documented budgets met on reference desktop AND a reference mobile
    device (§143).

- **Flags**: 3D PRODUCTION (Kamakura environment), ART PRODUCTION, AUDIO PRODUCTION,
  CONTENT PRODUCTION, GAME ENGINE SELECTION (STANDARDS §242 — evaluate Godot or similar;
  no custom engine). **Status**: TARGET, phased as STANDARDS §365 phases 21–23.

---

# 92. WORLD CELL SYSTEM

The world should be divided into cells.

Each cell contains:

```
terrain
geometry
NPCs
objects
audio
lighting
navigation
interactions
knowledge nodes
quest nodes
streaming metadata
```

Cells should load/unload dynamically.

Do not load the entire world into memory.

### In depth — cell contract

- Cell size is content-defined per region (city blocks vs. wilderness). Neighboring cells
  stream in/out around the player with LOD tiers (STANDARDS §267 — cell cache with owner,
  size limit, eviction).
- **Deterministic content**: cell content is authored data (world packages), not generated
  at runtime — so debugging and saves stay reproducible (§98's determinism requirement
  applies to the whole world).
- Cell state (what the player did inside) is stored separately from cell content: content
  is immutable package data; player-driven changes are sparse overrides in the save
  (§144).
- **Acceptance criteria**
  - Moving across a region boundary never loads the whole region — only adjacent cells.
  - Streaming never blocks the frame; budget documented per platform (§143).

---

# 93. WORLD OBJECT SYSTEM

Every meaningful object can optionally become an interaction node.

Examples:

```
vending machine
train ticket machine
sign
restaurant menu
bicycle
car
tree
flower
food
book
newspaper
shop
train
boat
aquarium exhibit
```

Objects can expose:

```
name
Japanese name
description
knowledge nodes
interactions
photography
quests
dialogue
collection
learning content
```

### In depth — object model

Full schema: `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md` (§3). Key rules:

- An object's **language surface** (its Japanese name, sign text, menu) is the primary
  knowledge connection: object → `represents` → vocabulary/kanji nodes, or object *is*
  the anchor for inline text nodes (signs, menus, labels).
- Object interactions are **typed and reused** (§94), never per-object hardcoded scripts.
- Photography eligibility, collection membership, and quest hooks are declared metadata on
  the object, not code.

---

# 94. INTERACTION SYSTEM

Do not hardcode every object interaction.

Create reusable interaction types.

Examples:

```
LOOK
EXAMINE
TALK
PHOTOGRAPH
READ
PICK_UP
BUY
SIT
EAT
DRINK
SWIM
BOARD
ENTER
EXIT
SEARCH
LISTEN
PLAY
WATCH
COLLECT
```

Each interaction can connect to:

```
knowledge
dialogue
quest
story
animation
sound
statistics
```

### In depth — interaction contract

| Interaction | Typical target | Knowledge hook | Quest/story hook |
|---|---|---|---|
| LOOK / EXAMINE | objects, landmarks | glossary (§140) | discovery |
| TALK | NPCs | dialogue lines (§99) | story/quest |
| PHOTOGRAPH | objects, scenes, NPCs | photo → discovery/collection (§95, §111) | photography quests |
| READ | signs, menus, books | inline text nodes → vocab | reading quests |
| PICK_UP / COLLECT | items | item vocab | collection quests |
| BUY | shops, vending machines | item names, prices (numbers!) | daily-life |
| EAT / DRINK | food/drink | food vocab, counters, polite forms | culture quests |
| SIT / ENTER / EXIT / BOARD | furniture, buildings, trains | location vocab | transport quests |
| SEARCH / LISTEN / WATCH | environments, NPCs, media | audio/visual vocab | story beats |

Each interaction type defines: eligibility, required inputs, available outputs
(knowledge/dialogue/quest/stat events), animation/sound hooks, and failure behavior —
implemented as data-driven definitions, not per-object code.

- **Acceptance criteria**
  - Adding a new object with a new *combination* of existing interactions requires zero
    code (only content).
  - A new interaction *type* is a code change gated by the authoring pipeline (§146–§148)
    and an ADR note, not a casual addition.

---

# 95. PHOTOGRAPHY SYSTEM

Photography should be a real Journey mechanic.

Camera:

```
first-person camera mode
third-person camera mode
free camera
zoom
focus
composition
filters
photo gallery
location metadata
object recognition
knowledge extraction
```

A photo can become:

```
Discovery
Collection item
Memory
Quest objective
Learning card source
```

### In depth — photography contract

- **Composition matters**: framing guides, focus, and filters are deliberately simple and
  cozy (§97's art direction) — not a full pro camera sim.
- **Object recognition + knowledge extraction**: a photo taken of a recognized object/scene
  produces a `Photograph` node linked via `depicts` → object → knowledge nodes, so the
  photo's vocabulary is discoverable later ("photo → words in it").
- A photo can feed the mining pipeline as a *source* (MINING_SOURCE = PHOTO), reusing the
  existing mining flow (AGENTS.md → Mining Engine) so "learn from your own photo" is not a
  separate system.
- **Acceptance criteria**
  - Every photo stores location metadata + timestamp + linked nodes (or explicit "no
    recognition" state — no fabricated links).
  - Photo gallery is part of the Journal (§119) and offline-first.

---

# 96. FIRST PERSON / THIRD PERSON

Support both where practical.

FIRST PERSON:

```
better for:

photography
reading signs
immersion
dictionary interaction
exploration
fine interaction
```

THIRD PERSON:

```
better for:

character movement
social scenes
walking
animations
cosmetic/avatar visibility
```

Allow switching through a deliberate camera action.

Do not force both cameras into every interaction.

### In depth — camera policy

- One deliberate switch action (default key/button), configurable (STANDARDS §251–§253).
- Per-interaction camera preference is *declared* (e.g. photography defaults first-person,
  dialogue defaults third-person) but never enforced as a hard lock.
- Mobile gets the same switching, mapped to touch (a deliberate camera button — no
  accidental switches).

---

# 97. CHARACTER SYSTEM

Character system:

```
Avatar
Appearance
Clothing
Accessories
Animation
Emotes
Walking
Running
Swimming
Sitting
Riding
Photography
```

Avoid:

```
generic Roblox-like proportions
generic mobile-game characters
overly complicated AAA character production
```

Use a coherent artistic direction.

Document:

```
stylized Nintendo-like
cozy
clean
expressive
simple enough for cross-platform performance.
```

### In depth — art direction contract

- Art direction must be written down (a style sheet in `docs/design/`), covering:
  proportions, silhouette, palette, animation style, and cross-platform budgets
  (STANDARDS §245–§246: source assets in Blender, exported assets processed).
- Character budgets: polygon/texture/animation counts per platform tier (§143) so the
  "cozy" style is a performance decision too, not just a look.
- Customization (appearance/clothing/accessories) is cosmetic-only; rewards (§117) may
  grant cosmetics, never power.

---

# 98. NPC SYSTEM

NPCs should have:

```
identity
appearance
occupation
age category
location
schedule
relationships
dialogue
knowledge
quests
activities
```

NPC schedule:

```
morning
afternoon
evening
night

weekday
weekend
season
weather
```

NPCs should appear to have lives.

But the system must be deterministic enough for debugging.

### In depth — NPC contract

Full schema: `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md` (§5). Key rules:

- **Deterministic simulation tiers** (STANDARDS §105 & §143): NPCs are data-driven with
  declarative schedules — a shopkeeper is at the shop during open hours, at home at night,
  etc. No emergent-AI dependency; debugging must be reproducible.
- NPCs have a small set of activities per schedule slot; dialogue is conditional on
  schedule, season, weather, quest state, and relationship level.
- NPC relationships with the player evolve through interactions and quests (stored in
  save data, §144).

---

# 99. DIALOGUE SYSTEM

Dialogue nodes:

```
speaker
text
Japanese
translation
furigana
voice
emotion
choices
conditions
effects
knowledge nodes
quest effects
```

Dialogue may teach vocabulary or grammar naturally.

Do not make every conversation an obvious lesson.

### In depth — dialogue contract

- Dialogue is authored content (data-driven, §146), written by humans, not generated.
- Language exposure is **ambient**: lines carry knowledge-node links, but the UI only
  reveals them on demand (§112 — no flashcard interruptions inside conversations).
- Choices are typed (story/quest/language), each with conditions and effects; effects
  mutate quest state, NPC relationships, and (optionally) discoveries.
- **Acceptance criteria**
  - Every dialogue line has Japanese + translation + furigana and is validated by the
    content pipeline (§148).
  - A line's knowledge links are traversable from the dialogue transcript (→ dictionary).

---

# 100. QUEST SYSTEM

Quest types:

```
DISCOVERY
EXPLORATION
LANGUAGE
PHOTOGRAPHY
COLLECTION
STORY
CULTURE
LISTENING
READING
WRITING
VOCABULARY
KANJI
GRAMMAR
MEDIA
DAILY
```

Quest structure:

```
Quest
→ objectives
→ conditions
→ interactions
→ rewards
→ knowledge
→ story consequences
```

### In depth — quest contract

Full schema: `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md` (§6). Key rules:

- **Quests are data, not code**: schema-validated content with typed objectives,
  conditions, and rewards (STANDARDS §257–§258). Quests reference nodes by id; the engine
  only evaluates conditions.
- Quest knowledge links are explicit: a quest may *teach* (grammar quests, vocabulary
  quests) but teaching happens through the normal learning surfaces (§112), never through
  pop-up flashcards.
- **Acceptance criteria**
  - Every quest objective is checkable by the engine from world state + user events
    (no hidden arbitrary code).
  - Quest failures are allowed and non-punitive (no energy systems, §117).

---

# 101. QUEST UX

Do not show giant RPG quest lists everywhere.

Use:

```
small objective cards
map markers
subtle notifications
journal
progress
contextual hints
```

Quest UI should disappear when not needed.

### In depth — quest UX contract

- Quest state lives in the Journal (§119) and the map overlay; the HUD shows at most one
  compact objective card (§138) near the location.
- Contextual hints: near a quest target, a subtle prompt shows; away from it, the quest
  disappears from the world UI entirely (but stays in the Journal).
- **Acceptance criteria**
  - A player can complete a quest chain without ever opening a "quest log".
  - Quest UI never covers the interaction prompt (§139) or the knowledge overlay (§140).

---

# 102. STORY SYSTEM

Stories should be composed from:

```
Story
→ Chapter
→ Scene
→ Beat
→ Dialogue
→ Interaction
→ Choice
→ Outcome
```

Stories can teach naturally.

Example:

```
A summer day in Kamakura.

The player:

takes train
buys drink
walks beach
meets character
visits shop
photographs object
learns words
returns home

This can become both:

story progression

and

language progression.
```

### In depth — story contract

- Story structure (Story → Chapter → Scene → Beat) is a typed node hierarchy with
  `precedes`/`follows` and `requires` edges — deterministic, saveable, replayable.
- "Both progressions" is explicit: each story beat may carry knowledge nodes
  (words/kanji/grammar encountered) and Journey progress (discoveries, relationships,
  quest flags). The same event updates both graphs (§149).
- **Acceptance criteria**
  - Save/load restores exactly the story beat; skipping/changing order is impossible by
    construction (strict `requires` edges) unless the author declares optional beats.
  - A story's language content is re-findable afterwards ("words I learned in the Kamakura
    story") — via the shared knowledge graph.

---

# 103. DAILY LIFE ACTIVITIES

Activities may include:

```
walk
sit
eat
drink
shop
visit
photograph
ride train
go to beach
swim
visit aquarium
read
listen
talk
observe
collect
write
study
```

These are not just minigames.

They should create contextual language exposure.

### In depth — activities as exposure

- Each activity is a typed `Activity` node with declared language surfaces (e.g. eating →
  menu words + counters; riding → station/direction vocabulary, §104).
- Activities reward *exposure*: encountering words counts toward discovery and knowledge,
  never toward a "score". No time-pressure minigames, no fail states that punish.

---

# 104. TRAIN SYSTEM

Future train system should support:

```
stations
routes
lines
platforms
timetables
vehicles
boarding
exiting
announcements
signage
destination text
```

Knowledge nodes can connect to:

```
station names
direction words
transport vocabulary
numbers
time expressions
```

Do not simulate an entire railway network physically if unnecessary.

Use a data-driven route simulation.

### In depth — train contract

- **Data-driven route simulation** (explicitly NOT a physics sim): stations/lines/
  timetables are authored data; the train moves along declared routes and announces
  stations per timetable. Standard: STANDARDS §105 scalable simulation tiers.
- Announcements/signage are *language gold*: station names, "next stop", direction, door
  sides, time expressions — each is a knowledge node with TTS hookup (existing TTS infra).
- **Status**: TARGET (slice-level: one line, Kamakura→Enoshima, is the initial scope;
  §91). **Flags**: CONTENT PRODUCTION, AUDIO PRODUCTION.

---

# 105. VEHICLE SYSTEM

Vehicles:

```
cars
buses
trains
bicycles
boats
```

Need:

```
traffic
routes
spawn rules
LOD
animation
audio
```

Do not simulate every vehicle individually if unnecessary.

Use scalable simulation tiers.

### In depth — simulation tiers

- Tier 0 (background): ambient traffic — sounds + low-LOD visual flow, no individual
  logic.
- Tier 1 (route): vehicles follow authored routes with spawn rules; used for buses/trains
  the player can actually board.
- Tier 2 (interactive): vehicles the player rides/drives.
- Choose the lowest tier that satisfies the scene (STANDARDS §367 — don't build custom
  simulation systems the engine already provides).

---

# 106. OCEAN / BEACH

Beach system:

```
walking
sitting
swimming
water
waves
weather
photography
objects
NPCs
activities
```

Optional:

```
diving
aquarium
marine discoveries
```

Water must be performance-aware.

### In depth — beach/ocean contract

- Water rendering is delegated to the game engine's standard water material with
  platform-tier quality presets (§143); no custom ocean sim.
- Swimming is an activity with its own animation set (§97) and exposure surfaces
  (water/pool/sea vocabulary).
- The Enoshima slice includes beach + an aquarium-like attraction (§91) with exhibits as
  knowledge nodes (marine-life vocabulary, reading panels).

---

# 107. WORLD TIME

World clock:

```
real time
game time
accelerated time
```

Day:

```
morning
day
evening
night
```

Time affects:

```
lighting
NPCs
shops
transport
quests
events
weather
```

### In depth — world clock contract

- Modes: **real time** (wall clock), **game time** (fixed day length), **accelerated**
  (fast-forward for scheduling convenience). Player-selectable, persisted in save.
- Time-of-day drives the deterministic schedule engine (§98): shops open/close, NPCs
  move, trains run per timetable, lighting/audio change.
- Quest/event timing is authored against *game time of day*, never against wall-clock —
  so accelerated time can't soft-lock a timed quest.

---

# 108. WEATHER

Weather:

```
clear
cloudy
rain
storm
snow
fog
```

Weather affects:

```
lighting
audio
NPCs
movement
world appearance
quests
photography
```

Do not make weather purely cosmetic.

### In depth — weather contract

- Weather is a state on each cell/region (authored per day/season with deterministic
  seeds — reproducible for debugging), affecting: ambient audio, lighting, particle
  effects, NPC schedule overrides, some quest conditions, and photo conditions
  (a photo quest may require rain).
- Weather never blocks core progression permanently (a "wait until clear" quest has a
  guaranteed clear-day within N days).

---

# 109. SEASONS

```
Spring
Summer
Autumn
Winter
```

Seasonal changes:

```
vegetation
events
NPC clothing
food
weather
quests
photography
world decoration
```

### In depth — seasons contract

- Season is world-level state with authored per-season content variants (NPC outfits,
  food items, event quests, decoration swap). The vertical slice (§91) ships **one**
  season (summer) and the architecture proves the swap is data-driven.
- Season affects language content too (seasonal vocabulary: 花見, 花火, 紅葉, 雪) — tying
  world time to the knowledge graph.

---

# 110. COLLECTION SYSTEM

Collections:

```
Kanji
Vocabulary
Objects
Food
Locations
Photography
Media
NPCs
Stories
Discoveries
```

Collections must connect to Knowledge Graph nodes.

### In depth — collection contract

- A `Collection` node groups member nodes with `belongs_to` edges. Membership is earned
  through real encounters (§111) — no auto-granting for logged-out browsing.
- Every collection type maps to knowledge content: a food collection item links to its
  food vocabulary; a location collection links to location words/kanji.
- **Acceptance criteria**
  - Viewing a collection item shows its language links and "encountered at" provenance.
  - Collections are per-user, synced (STANDARDS §271) like other user data, and offline.

---

# 111. DISCOVERY SYSTEM

Discovery is distinct from collection.

Collection:

```
"I have this."
```

Discovery:

```
"I encountered this."
```

A user may discover:

```
word
Kanji
location
object
NPC
food
sign
media scene
cultural fact
```

Discovery history contributes to:

```
stats
Journey
knowledge
quests
```

### In depth — discovery contract

- `Discovery` nodes are event-derived records: "encountered X at place/time via source".
  They feed `encountered_by` / `discovered_by` edges, stats, and quest conditions —
  without being "owned" like collection items.
- Discoveries are the bridge between media/study/Journey: the same word discovered in a
  subtitle and at a Kamakura shop accumulates into one user-knowledge trajectory (§84).
- **Acceptance criteria**
  - The user can always answer "where have I seen this?" (§83) because discovery records
    are complete and honest — and absent records are shown as absent.

---

# 112. LEARNING IN JOURNEY

Journey should never constantly interrupt exploration with flashcards.

Instead:

```
contextual exposure

then:

optional deeper learning.
```

Example:

```
player sees:

電車

UI subtly reveals:

電車

Later:

"Want to learn more?"

Then:

dictionary

cards

writing

pronunciation

related words
```

### In depth — exposure-first learning policy

- **Ambient reveal**: nearby/observed language surfaces render subtly in-world (signs,
  item labels) without interrupting play. The interaction prompt (§139) and knowledge
  overlay (§140) are the *only* immediate learning surfaces.
- **Opt-in depth**: "Want to learn more?" leads to the normal Kaiteyo surfaces (dictionary,
  cards, writing, pronunciation) — inside the game, without leaving the world (§140).
- **Never during**: no flashcards during dialogue (§99), no quiz pop-ups on entering a
  shop, no interruption of movement for a "lesson".

---

# 113. JOURNEY KNOWLEDGE DIFFICULTY

Normal Kaiteyo user may choose:

```
BEGINNER
ELEMENTARY
INTERMEDIATE
ADVANCED
CUSTOM
```

Journey content adapts.

The same location may have different language depth.

```
BEGINNER:

駅

train

simple sentences

INTERMEDIATE:

駅員さんに切符を見せる

ADVANCED:

natural conversational dialogue
```

Do not build entirely separate worlds for every level.

Reuse world geometry.

Change:

```
dialogue
quests
knowledge density
language complexity
available explanations.
```

### In depth — adaptation model

- Difficulty is a **content filter on the same world**: one geometry, N language depths.
  Dialogue variants, quest text, glossary richness, and available explanations swap by
  player level.
- The player's actual knowledge state (§84) can refine the chosen level (adaptive
  smoothing: show words near their known/familiar boundary more often).
- **Acceptance criteria**
  - A location authored for 3 levels costs ~1 location, not 3 world builds.
  - Changing difficulty mid-game is safe (only content presentation changes; knowledge
    and quest state persist).

---

# 114. NORMAL KAITEYO VS JOURNEY

Normal Kaiteyo:

```
dictionary-first
learning-first
media-first
study-first
```

Journey:

```
exploration-first
context-first
discovery-first
story-first
```

Shared foundation:

```
knowledge graph
database
user knowledge
statistics
cards
accounts
settings
```

### In depth — one product, two modes

- The shared foundation is the *same data and the same engine*: Journey is a front-end
  context over the knowledge graph, not a separate app (see ADR-0014). This is what makes
  §87's loop and §83's "where have I seen this" physically possible.
- UI/UX differ (game world vs. study app), but identity, theme, settings, knowledge, and
  stats carry across (§141).

---

# 115. CHILD MODE VS NORMAL JOURNEY

Do not make two completely unrelated games.

Use:

```
same world engine
same node architecture
same knowledge graph
different UX
different progression
different language complexity
different safety
different content
```

Child Mode:

```
more guided.
```

Normal Mode:

```
more open.
```

### In depth — child mode contract

- Child mode is a **configuration + content filter of the same runtime** (STANDARDS §115
  era design: safety first). Differences: guided quests, simpler language depth (§113),
  restricted content (no mature themes, no unmoderated social surfaces), different
  progression pacing, parent-visible settings.
- No separate engine, no separate data model, no duplicated world content.

---

# 116. GAME PROGRESSION

Avoid conventional XP grinding.

Possible progression:

```
knowledge
discoveries
collections
story completion
locations
photography
quests
language mastery
```

The player should feel:

```
"I know more Japanese."
```

rather than:

```
"I have 87,421 XP."
```

### In depth — progression contract

- Progression is **evidence-shaped**: the progress page (Journal §119) shows real growth —
  words/kanji known (mapped to §84–§85), discoveries, collections, story milestones,
  photos, locations — not a single XP number.
- Internal counters (for unlock logic) are fine, but never surfaced as the primary
  feedback loop. Rewards unlock via these evidence milestones (§117).

---

# 117. REWARD SYSTEM

Rewards may include:

```
new locations
new stories
new camera filters
cosmetics
collection entries
journal pages
photo frames
music
world events
```

Do not use predatory monetization mechanics.

Avoid:

```
energy systems
lives
loot boxes
artificial timers
```

### In depth — reward ethics

- All rewards are non-consumable, non-urgent, and earned through real engagement
  (knowledge/discovery/story). Nothing is purchasable that affects learning outcomes;
  cosmetics/camera filters are the only purchasable surface if monetization is ever added,
  and only with clear disclosure.
- **Acceptance criteria**: no mechanism in the Journey can force a wait, a loss, or a
  purchase to continue — offline and unmonetized play is complete.

---

# 118. MAP PROGRESSION

The map itself can progressively reveal.

Example:

```
Japan
→ Kanto
→ Kanagawa
→ Kamakura
→ district
→ streets
→ locations
```

Discovery reveals detail.

### In depth — map reveal contract

- Higher levels (world/region) are visible early; district/street/location detail unlocks
  with real discovery (§111) and quests.
- Unvisited areas show stylized geography with *no* fake detail (§158) — "undiscovered"
  must look intentional, not broken.

---

# 119. JOURNAL

Journal contains:

```
memories
photos
discoveries
people
places
words
Kanji
stories
quests
maps
```

The journal should feel like a personal travel notebook.

### In depth — journal contract

- Journal is the *personalization layer of the world*: a curated, per-user view over
  Journey data (photos, discoveries, people, places, words) + quests + maps.
- Words/Kanji entries link to the knowledge graph (§83) — "words I met in Kamakura" is a
  Journal section and a real query.
- **Acceptance criteria**
  - Journal renders offline and is exportable/backupable with user data (STANDARDS §205–§206).

---

# 120. UX PRINCIPLE — NO EMPTY SPACE

Every major Kaiteyo screen must use available space intelligently.

Do NOT:

```
put one card in the middle of a giant empty page.
```

Do NOT:

```
center everything by default.
```

Do NOT:

```
add giant margins because they are easy.
```

Use:

```
multi-column layouts
adaptive grids
contextual panels
secondary information
responsive cards
side information
search/filter regions
activity panels
```

while maintaining visual hierarchy.

Empty space may exist intentionally.

But it must look intentional.

### In depth — enforcement

- This principle is a standing review gate: screens are reviewed against it in the design
  review checklist (STANDARDS §355) and the visual bug register (§136) tracks violations.
- Concretely: use the space budget the window gives you — sidebar 20% / content 80%
  (§134), multi-column browse (§129), contextual panels on dictionary/kanji pages
  (§81–§83).
- **Acceptance criteria**: no primary screen ships with a single centered element on a
  blank canvas; every blank region has a purpose or is deliberately composed whitespace.

---

# 121. UX PRINCIPLE — INFORMATION DENSITY

Kaiteyo is a power tool.

Do not make everything enormous.

Use:

```
compact controls
dense data where appropriate
progressive disclosure
hover/tap details
expandable panels
tooltips
keyboard shortcuts
context menus
```

The interface should feel:

```
professional
fast
deep
polished
```

not:

```
toy-like
empty
vibecoded
template-generated
```

### In depth — density contract

- Density is a *default*, not a hidden advanced mode: compact controls, dense data grids
  in Browse/Stats/Library, hover/tap affordances for detail, and full keyboard shortcuts
  everywhere (§154 asks per screen: what should be one click away, what should be
  keyboard-accessible).
- Density never wins over comprehension: hierarchy and spacing tokens from the design
  system are the guardrails (STANDARDS §224, `docs/design/DESIGN_SYSTEM.md`).

---

# 122. UX PRINCIPLE — CONSISTENCY

Every:

```
button
menu
panel
modal
card
search field
tab
slider
dropdown
tooltip
context menu
```

must obey the same design system.

No random radius.

No random padding.

No random font sizes.

No random shadows.

No random animation.

### In depth — consistency contract

- Single source of truth: design tokens (color/type/spacing/radius/elevation/motion)
  defined in the design system and consumed by components; no ad-hoc values in screens
  (STANDARDS §224–§225; the token-based theme system is ADR-0002).
- New components are added to the design system before use; the visual bug register
  (§136) tracks violations found in existing screens.

---

# 123. UX PRINCIPLE — MOTION

Animations should communicate:

```
where something came from
where it went
what changed
what opened
what closed
```

Animation should not exist merely because animation is possible.

Settings should include:

```
animation speed
reduced motion
transition intensity
```

### In depth — motion contract

- Motion tokens/presets are defined in `docs/design/ANIMATION_SYSTEM.md` (spring physics
  based). Every animation is either a defined preset or a deliberate, reviewed exception.
- Accessibility: `reduced motion` and `transition intensity` settings must be honored
  everywhere (STANDARDS §254); the floating-bubble magnet behavior (§133) is the
  canonical *meaningful* animation — it communicates snapping, it does not decorate.

---

# 124. UX PRINCIPLE — RESPONSIVENESS

UI must react to:

```
window size
orientation
input type
platform
DPI
accessibility settings
```

Elements should smoothly reflow.

When resizing:

```
panels adapt
cards reflow
text wraps
navigation changes
controls remain usable
```

Avoid sudden jumps.

### In depth — responsiveness contract

- Layouts are constraint-driven (window size classes; the project already uses
  material3-window-size-class) with adaptive breakpoints; resizing animates via the
  motion system, never teleports.
- "Sudden jumps" (layout snapping, overlapping controls on resize) are tracked as visual
  bugs (§136) — the P0 resize-glitch items in `docs/planning/CURRENT_ISSUES.md` are the
  current offenders.

---

# 125. UX PRINCIPLE — PLATFORM NATIVITY

Windows should feel appropriate on Windows.

Android should feel appropriate on Android.

iOS should feel appropriate on iOS.

But all platforms must remain recognizably Kaiteyo.

Do not make Windows look like a badly copied iOS application.

### In depth — platform contract

- Platform-appropriate chrome and input conventions (window controls/title bar on desktop,
  system gestures/back on mobile), while the design system keeps the product identity
  (STANDARDS §125-style: shared tokens, platform-adapted components).
- Platform-specific behavior is documented per platform in `docs/platform/`.

---

# 126. LAUNCHPAD UX

Launchpad is a navigation surface.

It should:

```
open centrally
animate smoothly
have clear hierarchy
use consistent button spacing
support keyboard
support mouse
support touch
support gamepad where appropriate
```

Core destinations should include:

```
Home
Browse
Library
Media
Stats
Journey
Dictionary/search
Settings
```

Do not fill it with redundant features.

### In depth — launchpad contract

- Launchpad is the primary navigation surface (opened from the floating bubble, §133):
  central overlay, spring-open animation, consistent grid, full input support
  (keyboard/mouse/touch/gamepad).
- Destinations listed above are the only core entries; everything else lives inside those
  destinations (no redundant shortcuts that duplicate the sidebar).
- The visual requirements in §135 (no FPS drop, no square artifacts, correct spacing)
  are acceptance criteria for the component.

---

# 127. HOME UX

Home should answer:

```
"What should I do now?"
```

without becoming a dashboard nightmare.

Possible areas:

```
Today
Study target
Continue studying
Continue watching
Recent discoveries
Collections
Quick dictionary search
Progress heatmap glance
Current Journey
Recommended content
```

No:

```
social feed
download button
tutorial button
random redundant modules
```

unless explicitly relevant.

### In depth — home contract

- Home is a *decision surface*: it surfaces the single most useful next action per area,
  driven by real state (due reviews, in-progress media, current Journey objective,
  today's target). Every area links to its full screen.
- "Recommended content" is derived from knowledge gaps and history (STANDARDS §213) —
  never a marketing feed.

---

# 128. LIBRARY UX

Library is the user's learning workspace.

Top-level:

```
All
Decks
Collections
Imported
Recent
Favorites
```

Deck page:

```
Overview
Study
Cards
Browse
Statistics
Settings
```

Bulk actions:

```
select
tag
move
merge
export
suspend
bury
delete
```

### In depth — library contract

- Library organizes user-owned learning objects (decks, cards, notes, collections,
  imported content) as node views — "decks I imported", "cards mined from media",
  "collection: Kamakura food".
- Bulk actions must be safe: confirm, undo where practical, export before delete
  (STANDARDS §205–§207).
- **Acceptance criteria**: Library's All/Decks/Collections/Imported/Recent/Favorites
  filters map to real node queries (nodeType + provenance + recency), not hardcoded lists.

---

# 129. BROWSE UX

Browse is not just search.

It is exploration.

Search:

```
食
```

Results:

```
Kanji
Words
Sentences
Grammar
Media
Decks
Journey
Collections
```

Filters:

```
JLPT
frequency
reading
part of speech
pitch
source
media
knowledge
deck
difficulty
```

### In depth — browse contract

- Browse = node-graph exploration with a search box on top: results are grouped by node
  family (§77) and each result is a traversal anchor (§81).
- Filters map to node fields/edges (frequency, JLPT, source provenance, knowledge state
  (§84), deck membership, Journey discovery status).
- **Acceptance criteria**: any filter combination resolves within the search latency
  budget (STANDARDS §186–§188) at full dataset scale; empty results explain *why* and
  offer a way out (loosen filters, browse family instead).

---

# 130. MEDIA UX

Media Centre should feel like a serious media application.

Sections:

```
Library
Continue Watching
Playlists
Folders
Anime
Movies
Videos
Audio
Mining
History
```

Player:

```
video
subtitle
dictionary
timeline
controls
mining
screenshots
notes
```

Do not force the user into separate applications for normal learning workflows.

### In depth — media contract

- Media Centre is the home of the existing media/mining stack (AGENTS.md → Media Engine,
  Mining Engine): one player with subtitle + dictionary + mining + screenshots + notes
  (§130 player spec) — the "media-learning workflow in one app" promise.
- Media nodes (§77 MEDIA family) give Media Centre its data model: library/continue/
  playlists/folders/anime/movies/videos/audio/mining/history are node queries
  (Series/Episode/SubtitleLine/MiningEvent edges).
- **Acceptance criteria**: subtitle selection → dictionary popup → mine card → review is
  one continuous flow inside Media Centre (already the desktop suite's loop; formalize it
  as node data).

---

# 131. STATS UX

Stats should not become a wall of numbers.

Hierarchy:

```
Overview
→ Learning
→ Kanji
→ Vocabulary
→ Grammar
→ Media
→ Exams
→ Journey
```

Allow deeper drill-down.

### In depth — stats contract

- Overview = simplified knowledge dials (§85) + heatmap glance + key flows; each section
  drills into event-derived detail (STANDARDS §210–§214).
- Journey appears as a stats section once Journey data exists (discoveries, locations,
  photos, quests) — derived from the same event stream.
- **Acceptance criteria**: every number on every stats screen can be explained (its event
  basis) or is labeled as an estimate (§290). No fabricated precision.

---

# 132. SETTINGS UX

Settings should be structured.

Categories:

```
General
Appearance
Animation
Navigation
Input
Study
Flashcards
Dictionary
Media
Subtitles
Mining
Anki
Yomitan
Sync
Journey
Children
Privacy
Storage
Advanced
Developer
```

Settings navigation must not crash.

### In depth — settings contract

- One Settings Center with the categories above; category availability adapts per
  platform (Journey/Children only where applicable).
- The listed categories map to real subsystems; adding a category requires a setting
  owner (STANDARDS §175). "Settings navigation must not crash" is a literal acceptance
  criterion — the center must be exercised by UI tests (STANDARDS §218) and settings
  changes must never throw.

---

# 133. FLOATING NAVIGATION

Floating bubble:

```
- draggable
- no crash
- three magnetic positions per side
- smooth magnetic attraction
- subtle jiggle
- persistent location
- click = Launchpad
- hold = alternate menu
- right click = alternate menu
- touch hold supported
- no unnecessary Quick Access
```

Snap behavior:

```
FREE POSITION
↓
nearest snap point
↓
magnetic pull
↓
small elastic movement
↓
settle
```

Not:

```
teleport.
```

### In depth — floating bubble contract

- Implementation: spring-based magnetic snap with declared snap points (3 per side),
  position persisted (desktop suite already persists window/state under `~/.kaiteyo/`),
  input parity: click/hold/right-click/touch-hold.
- "No crash" is an explicit requirement: the bubble must never crash on drag, resize, or
  platform input events; it is covered by UI tests (STANDARDS §218) and the P1 "mobile
  navigation snap" TODO item extends the same behavior to mobile top/bottom.
- The subtle jiggle is a *purposeful* affordance (indicating draggability), governed by
  the motion system (§123).

---

# 134. SIDEBAR

Sidebar must NEVER become the entire content area on desktop.

Desktop target:

```
approximately 20% navigation
approximately 80% content
```

But responsive.

Sidebar should contain:

```
navigation
context
optional secondary actions
```

Content remains visible.

### In depth — sidebar contract

- Desktop: fixed-ratio split (≈20/80), resizable within bounds, collapsible; content
  stays visible and primary.
- Mobile: the sidebar becomes top/bottom navigation (never a full-screen takeover of
  content). See §124 responsiveness and the existing mobile-nav snap TODO.
- **Acceptance criteria**: at no window size does navigation consume the content area;
  the sidebar never covers the launchpad trigger.

---

# 135. LAUNCHPAD

Launchpad:

```
center of window
smooth open
smooth close
no FPS drop
correct button spacing
no square artifacts
consistent icons
keyboard navigation
mouse
touch
gamepad
```

### In depth

Full component contract with acceptance criteria (performance, spacing, input parity,
icon consistency) — see §126. The "no square artifacts" and "no FPS drop" items are
tracked as explicit acceptance checks, and any violation goes into the visual bug
register (§136).

---

# 136. VISUAL BUG REGISTER

Document all known visual problems.

Examples:

```
light rectangles around text
icon square artifacts
incorrect padding
inconsistent radius
clipped elements
overlapping elements
wrong sidebar width
wrong launchpad position
window controls
title bar
floating bubble
animation artifacts
blank space
incorrect scaling
```

Each must become a tracked bug.

### In depth — register contract

- The register is a **living checklist** maintained in `docs/planning/CURRENT_ISSUES.md`
  (which already tracks the P0 visual items: animation stutter, resize glitches, hover
  inconsistency, spacing/radius). New finds are added as tracked bugs there, with: screen,
  description, reproduction, screenshot, severity, status.
- Standard fields per entry: area (§120–§135 system), description, expected vs actual,
  repro steps, platform/OS, severity, linked TODO item.

---

# 137. GAME UI LAYER ARCHITECTURE

Separate:

```
WORLD UI
JOURNEY UI
GLOBAL KAITEYO UI
DICTIONARY OVERLAY
KNOWLEDGE OVERLAY
MEDIA OVERLAY
SYSTEM UI
```

Do not mix game HUD with desktop application navigation.

### In depth — layer contract

- Each UI layer has a distinct owner, input scope, and z-order policy:

| Layer | Owner | Scope | Z |
|---|---|---|---|
| WORLD UI | game runtime | in-world elements (signs, prompts, HUD) | lowest |
| JOURNEY UI | journey features | journal, quests, camera, collections | mid |
| GLOBAL KAITEYO UI | shared app | launchpad, settings, library chrome | above Journey |
| DICTIONARY OVERLAY | shared dictionary | lookup popup (§140) | above game |
| KNOWLEDGE OVERLAY | shared knowledge | glossary, mastery display | above game |
| MEDIA OVERLAY | media | subtitle/dictionary in media contexts | above game |
| SYSTEM UI | app shell | dialogs, confirmations, updates | top |

- The game HUD is never mixed into app navigation chrome (no window buttons inside the
  world); the shared overlays (§140) bridge the worlds without merging them.

---

# 138. WORLD HUD

Minimal HUD.

Possible:

```
time
weather
location
objective
interaction prompt
camera control
```

No giant permanent HUD.

### In depth — HUD contract

- HUD is a thin, collapsible strip: time/weather/location/objective + the interaction
  prompt (§139) + camera switch (§96). Most of the time only the interaction prompt and a
  subtle location label are visible.
- Everything else lives in overlays/Journal (§119, §140).

---

# 139. INTERACTION PROMPT

When near an object:

```
small contextual prompt.
```

Example:

```
[Interact]

then:

おにぎり
Onigiri
```

No giant UI panel unless expanded.

### In depth — prompt contract

- Proximity → small prompt with object name (Japanese + gloss). Interact → contextual
  options (examine/photograph/talk/read per §94). Expand → the knowledge overlay (§140) —
  the prompt itself never grows into a panel.

---

# 140. KNOWLEDGE OVERLAY

Press dictionary action:

```
small glossary appears.
```

Can expand into:

```
full dictionary.
```

This allows:

```
game
→ dictionary
without leaving the world.
```

### In depth — overlay contract

- The knowledge overlay is the shared dictionary popup (existing `DictionaryPopup`
  behavior in the desktop suite) hosted inside the game layer: compact glossary on
  demand, expandable to the full dictionary entry, with mining/create-card actions
  available from it (§82–§83 surfaces).
- This is the concrete seam where "one product" (§114, §151) is experienced: game text →
  dictionary → card → review, no app switching.

---

# 141. GAME ↔ KAITEYO TRANSITION

Entering Journey should feel like entering another part of Kaiteyo.

Not launching a completely unrelated application.

Shared:

```
identity
theme
settings
knowledge
statistics
library
```

### In depth — transition contract

- Journey is a destination inside the app (Launchpad entry, §126), sharing account,
  theme, settings, knowledge, stats, library, and update mechanism (ADR-0014).
- Entering/exiting uses the motion system (§123) with a clear "you are entering the
  world" transition; leaving preserves all state (§144).

---

# 142. WORLD AUDIO

Audio nodes:

```
ambient
music
weather
NPC
vehicles
trains
ocean
shops
environment
```

Language audio:

```
dialogue
announcements
pronunciation
subtitles
```

Audio should react to world state.

### In depth — audio contract

- Audio is data-driven per cell/location/object (§92–§93): ambient beds, weather layers
  (§108), schedule-aware shop/NPC/train sounds (§98, §104), with volume mixing by
  distance.
- Language audio reuses the app's TTS infrastructure (existing kana TTS voices) for
  pronunciation/announcements; dialogue audio is authored content (CONTENT/AUDIO
  PRODUCTION).
- **Acceptance criteria**: muting/volume settings from the app's settings apply inside
  the world; audio reacts to time/weather deterministically.

---

# 143. WORLD PERFORMANCE

Target smoothness.

Document:

```
FPS targets by platform
dynamic resolution where appropriate
LOD
occlusion
streaming
texture budgets
mesh budgets
audio budgets
NPC simulation tiers
```

Desktop high-end:

```
higher fidelity.
```

Mobile:

```
lower complexity.
```

Same world data.

Different presentation budgets.

### In depth — performance budgets

| Platform tier | Target | Strategy |
|---|---|---|
| Desktop high-end | 60+ FPS, high fidelity | max LOD, dynamic resolution cap, high texture/mesh budgets |
| Desktop mid | 60 FPS | medium LOD, adjusted budgets |
| Mobile (Android/iOS mid) | 30–60 FPS | low LOD, occlusion, aggressive streaming, reduced NPC tiers, texture compression |
| Mobile low | 30 FPS target | minimum preset, dynamic resolution active |

- Budgets are *documented and measured* (STANDARDS §188–§190): startup, frame time,
  memory, streaming, audio, save. Cell streaming (§92) and simulation tiers (§105) are
  the primary levers; the vertical slice (§91) is where budgets get proven on reference
  devices.

---

# 144. GAME SAVE SYSTEM

Save:

```
player position
world progress
quests
discoveries
collections
photos
NPC relationships
story state
settings
```

Learning data remains in shared user data.

### In depth — save contract

- One save per user per world: sparse overrides over immutable content (§92) —
  position, world progress, quests, discoveries, collections, photos, NPC relationships,
  story state, per-world settings. Versioned (§145) and compatible across app versions.
- **Learning data never lives in the save**: knowledge, reviews, cards, stats stay in the
  shared user data (§114 shared foundation) — that is what makes Journey progress and
  study progress one trajectory.
- Save is offline, exportable with backups (STANDARDS §205–§206), and never blocks play.

---

# 145. WORLD VERSIONING

World content must be versioned.

World package:

```
worldId
version
dependencies
minimum engine version
content hash
assets
nodes
localization
```

### In depth — package contract

- Worlds ship as **content packages** (schema in `docs/architecture/nodes/CONTENT_AUTHORING.md`):
  manifest (worldId, version, dependencies, minimum engine version, content hash),
  assets, nodes/relationships, localization, license/attribution (STANDARDS §259–§260).
- Packages are validated (§148), hash-verified, and never execute code (STANDARDS §361).
- The engine reports version incompatibility instead of failing silently.

---

# 146. CONTENT AUTHORING SYSTEM

Future editor must allow creators to create:

```
nodes
relationships
locations
objects
quests
dialogue
stories
lessons
language content
media links
```

without modifying application source code.

### In depth — authoring contract

- Authoring is data-driven end-to-end (STANDARDS §257–§258): authors produce schemas +
  validated content (JSON/YAML/SQLite per ADR-0015), the runtime consumes packages.
- "Without modifying source code" is the acceptance criterion for the editor: a lesson,
  quest, or region authored in the editor ships as content, not as a code change.

---

# 147. NODE EDITOR

Create conceptual node editor.

Features:

```
create
edit
connect
filter
search
inspect
validate
preview
```

Validation:

```
missing relationship
invalid node type
missing localization
missing asset
broken reference
circular dependency where forbidden
invalid quest requirement
```

### In depth — node editor contract

- The node editor is the authoring tool for nodes/relationships: visual create/edit/
  connect/filter/search/inspect + built-in validation (§148) + preview (how it looks in
  Browse, Dictionary, and Journey).
- Validation checks enumerated above are hard gates: an invalid node cannot be published.
- **Status**: FUTURE tool (authoring infra is behind the content pipeline, §146); the
  validation rules are specified NOW so future content doesn't rot.

---

# 148. CONTENT VALIDATION

Before content ships:

```
schema validation
relationship validation
asset validation
localization validation
license validation
performance validation
```

### In depth — validation gates

| Gate | Checks | Failure behavior |
|---|---|---|
| Schema | node/relationship conforms to registry + `schemaVersion` | reject |
| Relationship | types from registry; cardinality; no dangling refs; no forbidden cycles | reject with report |
| Asset | referenced assets exist, hashed, within size budgets | reject |
| Localization | every required locale string present (§99 dialogue, signs) | reject |
| License | attribution/creator/license metadata present (STANDARDS §260) | reject |
| Performance | sample queries within latency budgets; cell budgets (§143) | warn/reject by severity |

Validation runs in the authoring pipeline and on package install (runtime side
verification).

---

# 149. KNOWLEDGE GRAPH + GAME GRAPH

Do not build two unrelated graphs.

Use:

```
Language Knowledge Graph

and:

World Graph
```

connected through explicit relationships.

Example:

```
WORLD OBJECT
↓
represents
↓
VOCABULARY NODE

or:

LOCATION
↓
contains
↓
LANGUAGE CONTENT
```

### In depth — the two-graph bridge

- Language Knowledge Graph: nodes from §77 LANGUAGE/LEARNING + user knowledge (§84).
- World Graph: nodes from §77 WORLD/GAMEPLAY.
- They are connected ONLY through explicit typed edges (`represents`, `contains`,
  `discovered_by`, `encountered_by`, `mined_from`) — the registry (§80) is the contract.
- This is the architectural heart of the product: it is what makes §83, §87, §112, §114
  and §150 actually true. Build it in the node layer (ADR-0013), keep both graphs in the
  same identity/relationship system so traversal never crosses "app boundaries".

---

# 150. THE FUNDAMENTAL KAITEYO LOOP

Everything should ultimately connect to:

```
DISCOVER
↓
UNDERSTAND
↓
PRACTICE
↓
USE
↓
REMEMBER
↓
EXPLORE MORE
```

Example:

```
User sees a train sign.

↓ Discovery

User reads:

駅

↓ Dictionary

駅

↓ Kanji

駅

↓ Words

駅前
駅員
駅長

↓ Sentence

駅前で待っています。

↓ Media

anime scene containing 駅

↓ Card

Kaiteyo deck

↓ Review

↓ Stats

↓ Journey

User visits station.

↓ Real-world context

↓ Knowledge strengthens.

This is the product.
```

### In depth — the loop as the product definition

- This loop is the *definition of done* for the whole node architecture: any feature that
  cannot be placed somewhere on this loop is suspect (§152).
- The example chain is a test scenario: a single kanji (駅) must be traceable from a world
  sign through dictionary → words → sentence → media → card → review → stats → Journey,
  with every hop a real node relationship (§149).

---

# 151. KAITEYO SHOULD FEEL LIKE ONE SYSTEM

The final UX must not feel like:

```
Dictionary + flashcards + media player + game + statistics + decks
randomly glued together.
```

It should feel like:

```
ONE LANGUAGE WORLD.
```

```
Dictionary is the knowledge interface.
Browse is the exploration interface.
Library is the learning workspace.
Media is contextual immersion.
Journey is contextual exploration.
Stats is the reflection layer.
Exams are evaluation.
Knowledge Graph is the connective tissue.
User Knowledge is the personalization layer.
Anki/Yomitan are integrations.
```

### In depth — one-system contract

- Each surface has a *single role* (table above); a feature that needs a new role must
  justify itself (§152). The knowledge graph + user knowledge are the shared substrate;
  integrations (Anki/Yomitan) are adapters, never core dependencies (STANDARDS §199).

---

# 152. FINAL ARCHITECTURAL TEST

For every new feature ask:

```
"What node does this create or consume?"
"What relationship does it create?"
"How does User Knowledge change?"
"How does this appear in Browse?"
"Can this appear in the Dictionary?"
"Can this contribute to Library?"
"Can this contribute to Stats?"
"Can this contribute to Journey?"
"Can this contribute to Media?"
"Can this be used offline?"
"Can this work on all supported platforms?"
"Can another subsystem consume this without duplicating data?"
```

If the answer is always "no", investigate whether the feature is isolated unnecessarily.

### In depth

- This test is a standing review gate for features and PRs (STANDARDS §355). The answer
  to "can another subsystem consume this without duplicating data?" is the node
  architecture's reason to exist (§76). Features that fail the test are either rejected
  or redesigned onto the shared graph.

---

# 153. FINAL GAME DESIGN TEST

For every Journey feature ask:

```
Does this improve:

exploration?
language?
culture?
story?
discovery?
immersion?
```

If not, it may not belong.

Do not add RPG mechanics merely because games have them.

### In depth

- This test gates every Journey system (§86–§119): XP, loot, energy, quest logs, and
  generic RPG tropes fail it by default. Anything that passes is added with a note
  explaining which of the six it serves.

---

# 154. FINAL UX TEST

For every screen:

```
What is the user's primary goal?
What is secondary?
What can be hidden?
What deserves screen space?
What should be one click away?
What should be discoverable?
What should be keyboard accessible?
What should be touch accessible?
What happens on a small screen?
What happens on a huge monitor?
What happens with reduced motion?
What happens when the user has no internet?
```

### In depth

- The test is answered in the design review for each screen (STANDARDS §355) and recorded
  in the screen's spec (the §126–§135 screen contracts are the first answers). Offline
  (STANDARDS §182) and reduced-motion (§123) answers are mandatory, not optional.

---

# 155. FINAL PRODUCT PRINCIPLE

KAITEYO SHOULD NOT BE BUILT AS:

```
"hundreds of features."
```

It should be built as:

```
A connected language ecosystem.
```

The user should be able to move naturally:

```
Kanji
→ word
→ sentence
→ grammar
→ media
→ scene
→ Journey
→ location
→ object
→ discovery
→ deck
→ review
→ stats
→ exam
→ mastery
```

without feeling that they have switched applications.

### In depth

- The transition chain above is a *navigation requirement*: every arrow is a real,
  one-or-two-click path implemented via the node graph (§81–§83, §150). Mastery is the
  terminal state (§84–§85) reachable from anywhere on the chain.

---

# 156. DOCUMENTATION COMPLETION REQUIREMENT

The documentation pass is not complete until the above concepts are represented in the
repository documentation with:

- diagrams
- node definitions
- relationship definitions
- UX flows
- architecture decisions
- acceptance criteria
- implementation dependencies
- TODO references

Do not simply paste this prompt into a Markdown file.

Translate it into actual project documentation.

Turn concepts into structured documents.

Create cross-links.

Create indexes.

Create diagrams.

Create tables where useful.

Create TODO items.

Create ADRs where decisions are required.

### In depth — how this requirement is satisfied

This section is implemented by the documents it points to:

| Requirement | Where |
|---|---|
| Diagrams | master graph (§160) in this doc; per-system diagrams in `nodes/` docs |
| Node definitions | `docs/architecture/nodes/NODE_TYPE_REGISTRY.md` |
| Relationship definitions | `docs/architecture/nodes/RELATIONSHIP_REGISTRY.md` |
| UX flows | §126–§135 screen contracts; `docs/design/` existing specs |
| Architecture decisions | ADR-0013, ADR-0014, ADR-0015 |
| Acceptance criteria | inline per section here + per-system in `nodes/` docs |
| Implementation dependencies | inline per section; master order in §157 / STANDARDS §365 |
| TODO references | `docs/planning/TODO.md` → Node & Journey |

---

# 157. FINAL AGENT HANDOFF REQUIREMENT

At the end, identify the first implementation tasks that should be handed to future
coding agents.

Prefer foundational tasks.

Example order:

```
1. repository/core stability
2. design system
3. database
4. node model
5. knowledge graph
6. dictionary
7. Kanji
8. vocabulary
9. Library
10. Browse
11. study engine
12. statistics
13. exams
14. media
15. mining
16. integrations
17. Journey runtime
18. world data
19. vertical slice
20. children mode
```

However, reorder based on actual repository dependencies.

### In depth — the actual handoff order for this repository

Ordered by *current repository reality* (see `docs/planning/ENGINEERING_AUDIT.md` §5 and
STANDARDS §365 phases), the node/Journey build order is:

| # | Task | Current state | Depends on |
|---|---|---|---|
| 1 | Repository/core stabilization (two-app question, dead code) | 🔴 open (audit) | — |
| 2 | Design system hardening (tokens, §120–§125) | partial | 1 |
| 3 | Database additions for node layer (ADR-0013) | TARGET | 1, 2 |
| 4 | Node model (contract §78 + registry) | TARGET | 3 |
| 5 | Knowledge graph (edges §79–§80, bridge §149) | TARGET | 4 |
| 6 | Dictionary as node interface (existing engine, §81–§83) | partial | 4, 5 |
| 7 | Kanji/Vocabulary node experiences (§82–§83) | partial | 6 |
| 8 | User Knowledge + scoring (§84–§85, KNOWLEDGE_STATE_MODEL) | TARGET | 5 |
| 9 | Library/Browse node views (§128–§129) | partial | 6, 7 |
| 10 | Study engine + review loop over knowledge graph | current (FSRS) | 8 |
| 11 | Statistics event-driven over node events (§131) | partial | 10 |
| 12 | Exams (§132 area; STANDARDS §287–§289) | partial | 10, 11 |
| 13 | Media as node family (§130) | partial | 5 |
| 14 | Mining into node graph (MINING events) | partial | 13 |
| 15 | Integrations as adapters (Anki/Yomitan) | current | — |
| 16 | Journey runtime (engine selection §242, ADR-0014) | TARGET | 4, 5, 8 |
| 17 | World data + packages (§88–§93, §145) | TARGET | 16 |
| 18 | Kamakura/Enoshima vertical slice (§91) | TARGET | 16, 17 |
| 19 | Children mode (§115) | TARGET | 18 |

Tracked as TODO items in `docs/planning/TODO.md` → Node & Journey.

---

# 158. DO NOT PRETEND THE GAME ALREADY EXISTS

Journey is a TARGET ARCHITECTURE unless actual implementation is present.

Document:

```
CURRENT:
what exists now.

TARGET:
what we want.

FUTURE:
what is intentionally postponed.
```

This distinction must appear everywhere.

### In depth — labeling rules for every document in this tree

- Every section here carries a status label (CURRENT / TARGET / FUTURE / OPEN QUESTION)
  — see the status line in each "In depth" block and the registries.
- Nothing may be described as implemented unless `docs/planning/PRODUCT_AUDIT.md`
  confirms it. When in doubt, label TARGET.

---

# 159. DO NOT TURN THE DOCUMENTATION INTO A FICTIONAL DESIGN BIBLE

The architecture must remain connected to engineering reality.

If something is technologically uncertain:

```
OPEN QUESTION.
```

If something is expensive:

```
HIGH COST.
```

If something requires a separate engine:

```
SEPARATE RUNTIME.
```

If something requires external data:

```
EXTERNAL DEPENDENCY.
```

If something requires licensing review:

```
LEGAL REVIEW.
```

If something requires significant content production:

```
CONTENT PRODUCTION.
```

If something requires artists:

```
ART PRODUCTION.
```

If something requires 3D artists:

```
3D PRODUCTION.
```

If something requires sound:

```
AUDIO PRODUCTION.
```

### In depth — flag usage

- Flags are used on every TARGET/FUTURE item that carries cost or uncertainty. The most
  important open questions for Journey (engine selection, content pipeline, dataset
  licensing) are consolidated in `docs/planning/ENGINEERING_AUDIT.md` §7 and the
  registries.

---

# 160. FINAL MASTER NODE GRAPH

Create a master conceptual graph resembling:

```
                         KAITEYO
                            |
       +--------------------+--------------------+
       |                    |                    |
   LANGUAGE              MEDIA                WORLD
       |                    |                    |
   Kanji                  Anime             Location
   Vocab                  Episode           Object
   Grammar                Scene             NPC
   Sentence               Subtitle          Activity
       |                    |                    |
       +---------+----------+---------+----------+
                 |                    |
             KNOWLEDGE            DISCOVERY
                 |                    |
                 +---------+----------+
                           |
                     USER KNOWLEDGE
                           |
              +------------+------------+
              |            |            |
           LIBRARY       STATS        EXAMS
              |
           DECK/CARD
              |
           REVIEW
              |
           MASTERY
```

And:

```
MEDIA
→ SUBTITLE
→ WORD
→ DICTIONARY
→ KNOWLEDGE
→ CARD
→ LIBRARY
→ REVIEW
→ STATS

WORLD
→ OBJECT
→ LANGUAGE
→ DISCOVERY
→ KNOWLEDGE
→ JOURNEY
→ STATS

DICTIONARY
→ KANJI
→ VOCAB
→ SENTENCE
→ MEDIA
→ WORLD
```

This graph should become the conceptual center of the project.

### In depth

- This graph is the single reference diagram for the project's identity. It is rendered
  (ASCII/Mermaid variants) in `docs/architecture/nodes/README.md` and referenced from the
  product docs, `docs/roadmap/`, and the audit. Every subsystem claims its place on this
  graph; anything off-graph must justify itself (§152).

---

# 161. FINAL STANDARD

The project should eventually feel like a mansion rather than a shed.

But the mansion must have:

```
a foundation
rooms
hallways
doors
labels
wiring
plumbing
electrical systems
maintenance access
floor plans
emergency exits
```

In engineering terms:

```
Architecture
Modules
Nodes
Relationships
Data
APIs
UX
UI
Content
Assets
Testing
Documentation
Roadmap
```

must all correspond.

No beautiful interface should sit on top of chaotic architecture.

No massive database should exist without a user experience.

No game world should exist without knowledge integration.

No statistics should exist without trustworthy event data.

No deck should exist without a real scheduling system.

No media player should exist without a media data model.

No dictionary should exist without structured language data.

No external integration should become a core dependency.

### In depth — the correspondence audit

- Each "room" of the mansion has an owner document (STANDARDS §175), a data model, an API,
  a UI, an error model, a test strategy, a performance strategy, and documentation —
  checked by the §376 audit (see `docs/planning/ENGINEERING_AUDIT.md`). The negative
  clauses above are literal acceptance tests for the current debt items (duplicate SRS,
  suite vs core, jdata duplication).

---

# 162. FINAL STOP CONDITION

STOP after documentation and architecture are complete.

DO NOT:

```
compile
build
run Gradle
run long tests
generate a fake implementation
claim features are implemented
```

The next AI will code.

Your job is to make sure that next AI knows exactly what it is building.

The repository should now contain enough architectural information that multiple future
coding agents can work independently without destroying one another's work.

The documentation is now the map.

The node system is the language.

The knowledge graph is the connective tissue.

The TODO is the construction schedule.

The architecture is the blueprint.

The UI/UX specification is the interior design.

The Journey specification is the world plan.

The database is the foundation.

The future coding agents are the construction crew.

### In depth — how this stop condition is honored here

- This documentation pass makes **no code changes, no builds, no tests** — it only
  produces architecture documents, registries, ADRs, and TODO/roadmap entries
  (STANDARDS §341–§342: documentation passes do not trigger Gradle).
- The next agent's starting point is `docs/planning/ENGINEERING_AUDIT.md` (the §376
  handoff) plus the documents produced here: the master spec (this file), the registries,
  the world/runtime specs, and ADR-0013/0014/0015.
- The construction schedule is `docs/planning/TODO.md` → Node & Journey (§157).

---

*End of Node Architecture master specification (§76–§162). The engineering constitution
that governs building it is `docs/engineering/ENGINEERING_STANDARDS.md` (§163–§376). The
deep reference documents for implementers are in `docs/architecture/nodes/`. The handoff
state is `docs/planning/ENGINEERING_AUDIT.md`.*
