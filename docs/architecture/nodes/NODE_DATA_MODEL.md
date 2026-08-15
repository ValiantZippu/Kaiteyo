# Node Layer Data Model

**Status**: TARGET — this is the target schema for the node layer, to be realized under
ADR-0013. Nothing here is implemented. The current storage of record remains the two
SQLDelight databases (`docs/data/ARCHITECTURE.md`); this document specifies the additive
node layer on top of them and the world/save content stores.
**Source spec**: [Node Architecture master spec](../NODE_ARCHITECTURE.md) §78–§80, §84–§85, §92, §144–§145
**Vocabulary**: [NODE_TYPE_REGISTRY](NODE_TYPE_REGISTRY.md) · [RELATIONSHIP_REGISTRY](RELATIONSHIP_REGISTRY.md)
**Knowledge**: [KNOWLEDGE_STATE_MODEL](KNOWLEDGE_STATE_MODEL.md)

> **How to read**: every table here is a *target contract*. Column types are shown in
> SQL-flavored terms because SQLDelight/SQLite is the project's database standard
> (STANDARDS §179–§181); the final decision on whether a given store is SQLDelight
> tables, a JSON index, or a view over existing tables is made during implementation and
> recorded in ADR-0013. The *fields, keys, constraints, and semantics* are the contract;
> the storage mechanics are the implementation.

---

## 1. Storage map

| Store | Holds | Technology (target) | Mutability | Backed up / synced |
|---|---|---|---|---|
| `AppDataDatabase` (existing) | dictionary: kana, kanji, vocab, readings, meanings, sentences, deck reference data | SQLDelight (read-only asset) | immutable | n/a (bundled) |
| `UserDataDatabase` (existing) | decks, cards, FSRS state, reviews, settings, statistics records | SQLDelight (mutable, migrated) | mutable | yes |
| **Node store (new)** | `node`, `edge` tables — the knowledge graph: identities, relationships, user knowledge, discovery records | SQLDelight tables in `UserDataDatabase` (or companion DB per ADR-0013) | mutable | yes |
| **Event log (new)** | `event_log` — append-only evidence ledger (STANDARDS §210–§211) | SQLDelight | append-only | yes (can be pruned by policy) |
| **World packages (new)** | world/region/cell/object/NPC/quest/dialogue/story content | versioned SQLite package files (`Kaiteyo Content Package`, §8) | immutable (content) | installed packages re-downloadable |
| **Save file (new)** | player position, world progress, quests, discoveries, photos, NPC relationships, story state, per-world settings | versioned JSON (or SQLite) per world | mutable | yes (STANDARDS §206) |
| **Photos / media** | photographs, screenshots, audio clips | files on disk (platform-safe paths, STANDARDS §272) | mutable | optional/user-selected |

Provenance rule (§78): every `node` and `edge` row carries `source` + `source_id` (or
`derived_from` edges). No row exists without provenance.

---

## 2. Node store

### 2.1 `node` table (conceptual)

One row per node. Family-specific fields are stored in a typed, schema-versioned
`payload` JSON column (validated against the registry); high-cardinality queryable
fields (character, headword, readings, jlpt, frequency, tags) are additionally hoisted
into typed columns so searches and filters stay indexed (STANDARDS §186–§187).

```sql
CREATE TABLE node (
  id                TEXT PRIMARY KEY,              -- stable UUID v7, never reused
  node_type         TEXT NOT NULL,                 -- registry value, e.g. 'kanji', 'quest'
  family            TEXT NOT NULL,                 -- LANGUAGE | LEARNING | MEDIA | WORLD | GAMEPLAY | USER | SYSTEM
  schema_version    INTEGER NOT NULL,              -- per node_type registry version
  source            TEXT NOT NULL,                 -- jmdict | kanjidic | kjd | kaiteyo | kaiteyo-world | user | integration
  source_id         TEXT,                          -- id in the source system (NULL only for source='kaiteyo')
  status            TEXT NOT NULL DEFAULT 'active',-- active | archived | suspended | hidden | draft
  created_at        TEXT NOT NULL,                 -- ISO-8601
  updated_at        TEXT NOT NULL,
  parent_id         TEXT REFERENCES node(id),      -- hierarchical parent (e.g. reading -> kanji)
  owner_id          TEXT,                          -- user id for user-created nodes
  world_id          TEXT,                          -- world package id for world nodes
  language          TEXT,                          -- BCP-47 ('ja'), language-content nodes
  locale            TEXT,                          -- BCP-47 display locale where applicable
  character         TEXT,                          -- hoisted: kanji/kana surface
  headword          TEXT,                          -- hoisted: vocabulary headword
  reading           TEXT,                          -- hoisted: primary reading (searchable)
  jlpt              INTEGER,                       -- hoisted: 1..5 where known (dataset-based, §290)
  grade             INTEGER,                       -- hoisted: school grade where known
  frequency_rank    INTEGER,                       -- hoisted: rank in the frequency dataset
  tags              TEXT,                          -- JSON array of strings
  payload           TEXT NOT NULL,                 -- JSON: family-specific fields (registry-validated)
  UNIQUE (source, source_id)                       -- provenance identity
);
```

Notes:

- `UNIQUE (source, source_id)` guarantees that importing the same external row twice
  yields the same node (idempotent import, §184-style pipeline).
- `payload` is validated by node type; a new field on a type = `schema_version` bump +
  registry row update (§77 extensibility rules).
- `status` is lifecycle only. Knowledge state is not stored here — it lives in
  `user_knowledge` (§4). This separation is mandatory (§78, §84).

Indexes:

```sql
CREATE INDEX idx_node_type        ON node(node_type);
CREATE INDEX idx_node_family      ON node(family);
CREATE INDEX idx_node_character   ON node(character);
CREATE INDEX idx_node_headword    ON node(headword);
CREATE INDEX idx_node_reading     ON node(reading);
CREATE INDEX idx_node_jlpt        ON node(jlpt);
CREATE INDEX idx_node_freq        ON node(frequency_rank);
CREATE INDEX idx_node_parent      ON node(parent_id);
CREATE INDEX idx_node_world       ON node(world_id);
CREATE INDEX idx_node_status      ON node(status);
```

### 2.2 `edge` table (conceptual)

One row per typed relationship (§79–§80). Edges are first-class: they have identity,
provenance, and metadata.

```sql
CREATE TABLE edge (
  id              TEXT PRIMARY KEY,                -- stable UUID
  type            TEXT NOT NULL,                   -- relationship registry value, e.g. 'appears_in'
  source_node     TEXT NOT NULL REFERENCES node(id),
  target_node     TEXT NOT NULL REFERENCES node(id),
  source          TEXT NOT NULL,                   -- provenance (same enum as node.source)
  confidence      REAL,                            -- 0..1; REQUIRED for imported/derived edges
  weight          REAL,                            -- ranking hint (frequency, relevance)
  frequency       REAL,                            -- occurrence count/rate where meaningful
  ordering        INTEGER,                         -- ordinal hint (stroke order, sentence index)
  position        TEXT,                            -- positional data (sentence index, subtitle offset)
  context         TEXT,                            -- free-form contextual note (JSON)
  metadata        TEXT,                            -- JSON payload
  created_at      TEXT NOT NULL,
  updated_at      TEXT NOT NULL,
  UNIQUE (type, source_node, target_node)          -- prevent duplicate identical edges
);
```

Indexes (traversal is the core read pattern, §81–§83, §149):

```sql
CREATE INDEX idx_edge_source ON edge(source_node);
CREATE INDEX idx_edge_target ON edge(target_node);
CREATE INDEX idx_edge_type   ON edge(type);
-- composite for "all outgoing edges of a node, filtered by type":
CREATE INDEX idx_edge_src_type ON edge(source_node, type);
CREATE INDEX idx_edge_tgt_type ON edge(target_node, type);
```

Deletion policy (per registry §5): containment/membership edges (`contains`,
`belongs_to`, `contains_location`, `part_of`) cascade with the parent node; exposure/
provenance edges (`encountered_by`, `discovered_by`, `mined_from`, `generated_from`,
`derived_from`) are tombstoned (soft-delete via a `retired_at` column or a status) —
history must survive (§84 evidence, §111 discovery). Mapping edges (`mapped_to`,
`imported_from`) rewire on re-import. The exact mechanics are part of ADR-0013.

---

## 3. Derived indexes over existing databases

The dictionary content already lives in `AppDataDatabase`. The node layer does **not**
copy it; it references it:

- **Read-model option**: kanji/vocab/reading/sentence nodes are materialized lazily into
  the node store (or served as virtual nodes resolved against app-data rows) when an edge
  targets them.
- **View option**: language nodes are views/joins over `AppDataDatabase` tables + the
  node store, so dictionary data is never duplicated (NODE §152 — "consume without
  duplicating data").
- **Edge option**: cross-domain edges (word → subtitle line → card → knowledge) are the
  only rows added to the node store; they reference app-data rows by their
  `(source, source_id)` provenance identity.

Decision to be recorded in ADR-0013; all three options preserve the contracts below.

---

## 4. User knowledge store

Full model semantics: [KNOWLEDGE_STATE_MODEL](KNOWLEDGE_STATE_MODEL.md). Storage:

```sql
-- one row per (user, language node, dimension)
CREATE TABLE user_knowledge (
  id              TEXT PRIMARY KEY,
  user_id         TEXT NOT NULL,
  node_id         TEXT NOT NULL REFERENCES node(id),
  dimension       TEXT NOT NULL,   -- reading | writing | listening | recognition |
                                   -- production | context | meaning | pronunciation
  state           TEXT NOT NULL,   -- UNSEEN..FORGOTTEN (registry of states)
  state_since     TEXT NOT NULL,
  updated_at      TEXT NOT NULL,
  evidence_count  INTEGER NOT NULL DEFAULT 0,
  UNIQUE (user_id, node_id, dimension)
);

-- append-only evidence ledger; the transition history (KNOWLEDGE_STATE_MODEL §3.1)
CREATE TABLE knowledge_transition (
  id              TEXT PRIMARY KEY,
  knowledge_id    TEXT NOT NULL REFERENCES user_knowledge(id),
  from_state      TEXT,
  to_state        TEXT NOT NULL,
  trigger_event_id  TEXT NOT NULL,   -- references event_log.id
  trigger_type    TEXT NOT NULL,     -- e.g. 'review_success', 'vocabulary_encountered'
  event_time      TEXT NOT NULL,
  payload         TEXT               -- JSON: any transition-specific evidence
);

-- derived score cache; always rebuildable from events (STANDARDS §213)
CREATE TABLE knowledge_score (
  user_id         TEXT NOT NULL,
  dimension       TEXT NOT NULL,
  score           REAL NOT NULL,     -- 0..1 derived
  basis           TEXT NOT NULL,     -- what the score is based on (nodes, evidence window)
  confidence      REAL NOT NULL,     -- 0..1 how reliable the basis is
  computed_at     TEXT NOT NULL,
  schema_version  INTEGER NOT NULL,
  PRIMARY KEY (user_id, dimension)
);
```

Rules:

- `knowledge_transition` is append-only and immutable once written; corrections create
  new rows, never edits (auditability, §84).
- `knowledge_score` is a cache: it may be deleted and rebuilt from
  `user_knowledge` + `event_log` at any time. Nothing reads it as truth.
- Scores are never presented with more precision than their basis supports
  (STANDARDS §290).

---

## 5. Event log

The evidence ledger behind stats and knowledge (STANDARDS §210–§211). Full catalog:
[EVENT_CATALOG](EVENT_CATALOG.md).

```sql
CREATE TABLE event_log (
  event_id        TEXT PRIMARY KEY,   -- UUID v7
  user_id         TEXT NOT NULL,
  occurred_at     TEXT NOT NULL,      -- wall-clock when the event happened
  event_type      TEXT NOT NULL,      -- catalog value, e.g. 'card_reviewed'
  source          TEXT NOT NULL,      -- subsystem: study | media | mining | journey | exam | system | sync
  payload         TEXT NOT NULL,      -- JSON payload (schema per event_type)
  schema_version  INTEGER NOT NULL,   -- payload schema version
  session_id      TEXT                -- grouping for study sessions
);

CREATE INDEX idx_event_user_time ON event_log(user_id, occurred_at);
CREATE INDEX idx_event_type     ON event_log(event_type);
```

- Event payloads carry only semantic facts (node ids, review results, durations) — never
  UI state or private credentials (STANDARDS §211, §220).
- Aggregations (heatmap, knowledge scores, exam stats) read from `event_log` and are
  re-runnable; the log is the single source of truth for derived metrics (STANDARDS §213).

---

## 6. World content store

World content ships as versioned packages (NODE §145, ADR-0015); the runtime installs
them into a content database (read-only at runtime). Content tables mirror the
JOURNEY_WORLD_SCHEMA document structures:

```sql
CREATE TABLE world_package (
  package_id      TEXT PRIMARY KEY,   -- e.g. 'world.japan.v1'
  world_id        TEXT NOT NULL,
  version         TEXT NOT NULL,
  min_engine_version TEXT NOT NULL,
  content_hash    TEXT NOT NULL,
  license         TEXT NOT NULL,
  attribution     TEXT NOT NULL,
  installed_at    TEXT NOT NULL
);

CREATE TABLE world_node (
  id              TEXT PRIMARY KEY,   -- scoped id, e.g. 'cell:kamakura/komachi/07'
  package_id      TEXT NOT NULL REFERENCES world_package(package_id),
  node_type       TEXT NOT NULL,      -- world | region | prefecture | city | district | neighborhood |
                                      -- map_cell | location | interior | interaction_node | npc | quest | ...
  parent_id       TEXT REFERENCES world_node(id),
  name            TEXT,
  name_ja         TEXT,
  payload         TEXT NOT NULL,      -- typed JSON per node_type (JOURNEY_WORLD_SCHEMA)
  asset_refs      TEXT,               -- JSON array of asset ids
  level           TEXT                -- BEGINNER | ELEMENTARY | INTERMEDIATE | ADVANCED (content variants)
);

CREATE TABLE world_edge (             -- typed edges within the world graph + to language nodes
  id              TEXT PRIMARY KEY,
  package_id      TEXT NOT NULL,
  type            TEXT NOT NULL,      -- contains_location | located_at | represents | references | ...
  source_node     TEXT NOT NULL,
  target_node     TEXT NOT NULL,
  metadata        TEXT
);
```

- Language bridge: world objects carry `represents` edges to language node identities
  (`(source, source_id)` pairs) — the §149 bridge is data, not code.
- Content is immutable: world packages never mutate at runtime; player-driven change is
  sparse override data in the save (§7).

---

## 7. Save store

One save per (user, world), versioned and sparse (NODE §144). Target shape:

```json
{
  "save_version": 1,
  "user_ref": "user-uuid",
  "world_package_id": "world.japan.v1",
  "player": {
    "position": [x, y, z],
    "rotation": [yaw, pitch],
    "camera_mode": "third_person",
    "camera_prefs": {}
  },
  "world_progress": {
    "visited_cells": ["cell:kamakura/komachi/07"],
    "revealed_map": ["district:kamakura/komachi"],
    "clock_mode": "game_time",
    "clock": "2026-07-25T14:30:00Z",
    "weather_seed": 482913,
    "season": "summer"
  },
  "quests": {
    "quest:errand-01": {"state": "active", "objective_progress": [1, 0, 0]}
  },
  "discoveries": ["discovery:onigiri", "discovery:komachi-backstreet"],
  "collections": {"collection:kamakura-food": ["item:onigiri"]},
  "photos": [{"photo_id": "photo:001", "timestamp": "...", "location": "...", "links": ["object:shop-14/onigiri-shelf"]}],
  "npc_relationships": {"npc:tanaka": 3},
  "story_state": {"story:summer-day": {"chapter": 2, "beat": "errand-complete"}},
  "world_settings": {"difficulty": "BEGINNER", "hud_mode": "minimal"}
}
```

- Learning data is **never** in the save (NODE §144): knowledge, reviews, cards, and
  stats stay in the shared user data. The save only records world-relevant state.
- Save integrity: schema-versioned, checksummed, exported with backups (STANDARDS §206),
  and never required for core learning features.

---

## 8. Query patterns (the money queries)

These are the queries the product is built around. Each must be implemented with the
indexes above and validated at production scale (STANDARDS §279, §369).

### 8.1 Kanji → words containing it (kanji page, §82)

```sql
SELECT w.* FROM node w
JOIN edge e ON e.source_node = w.id
WHERE e.target_node = :kanji_node_id
  AND e.type = 'contains_character'
  AND w.node_type = 'vocabulary'
ORDER BY w.frequency_rank ASC NULLS LAST
LIMIT 200;
```

### 8.2 Word → its kanji (§83)

```sql
SELECT k.* FROM node k
JOIN edge e ON e.target_node = k.id
WHERE e.source_node = :word_node_id
  AND e.type = 'contains_character';
```

### 8.3 "Where have I seen this?" (§83, §149 — the product query)

Merge exposure across worlds, rank by recency:

```sql
-- media + Journey exposure
SELECT e.type AS exposure_type, t.id AS seen_in_id, t.node_type AS seen_in_type,
       e.context, e.position, e.created_at
FROM edge e
JOIN node t ON t.id = e.target_node
WHERE e.source_node = :word_node_id
  AND e.type IN ('appears_in_media', 'appears_in_scene', 'encountered_by', 'discovered_by', 'mined_from')
ORDER BY e.created_at DESC
LIMIT 50;

-- study exposure (reviews)
SELECT r.reviewed_at, r.result, r.card_id
FROM review r
JOIN card c ON c.id = r.card_id
JOIN edge e ON e.source_node = c.id
WHERE e.target_node = :word_node_id AND e.type = 'teaches'
ORDER BY r.reviewed_at DESC
LIMIT 50;
```

### 8.4 Knowledge aggregation for a dimension (§85)

```sql
-- count of nodes in each knowledge state, for the user, weighted by frequency
SELECT k.state, COUNT(*) AS nodes, SUM(COALESCE(n.frequency_rank, 0)) AS freq_sum
FROM user_knowledge k
JOIN node n ON n.id = k.node_id
WHERE k.user_id = :user AND k.dimension = :dimension
GROUP BY k.state;
```

### 8.5 Review selection hinting (knowledge-informed, FSRS-owned §84.4)

```sql
-- cards whose taught node is WEAK or FORGOTTEN, as candidates for the scheduler
SELECT c.id FROM card c
JOIN edge e ON e.source_node = c.id AND e.type = 'teaches'
JOIN user_knowledge k ON k.node_id = e.target_node AND k.user_id = :user
WHERE c.deck_id = :deck AND c.srs_state IN ('new','learning','review')
  AND k.state IN ('WEAK','FORGOTTEN','LEARNING')
ORDER BY k.updated_at ASC;
```

The scheduler's arithmetic remains FSRS-owned; this only *selects candidate order*.

### 8.6 Journey → language bridge (§149)

```sql
-- words represented by objects in a cell (for knowledge density overlay, §89)
SELECT ln.id, ln.headword, ln.reading FROM node ln
JOIN world_edge we ON we.target_node = ln.id AND we.type = 'represents'
JOIN world_node wn ON wn.id = we.source_node
WHERE wn.parent_id = :cell_id;
```

### 8.7 Discovery history for stats (§111, §131)

```sql
SELECT d.kind, d.found_at, COUNT(*) FROM discovery d
WHERE d.user_id = :user
GROUP BY d.kind, strftime('%Y-%m', d.found_at);
```

---

## 9. Migration & versioning

- Node/edge/knowledge schemas are versioned with SQLDelight `.sqm` migrations like the
  existing `UserDataDatabase` (STANDARDS §180; current migrations `1.sqm`…`14.sqm`).
  The node layer starts at the next free migration number.
- `schema_version` on rows handles payload-shape evolution independent of table
  migrations.
- World packages are content-versioned (§145); the engine enforces
  `min_engine_version` and refuses unknown versions with a clear message, never a crash
  (STANDARDS §219).
- Never change SQLDelight `.sq` schemas without an explicit request (AGENTS.md "never
  change" list) — the node layer is additive and must not alter existing tables.

---

## 10. Acceptance criteria

1. Every row in `node`/`edge` has valid provenance; imports are idempotent.
2. All §8 queries complete within the search latency budget at full dataset scale.
3. Deleting a user account removes user-owned nodes, edges, knowledge, and events —
   with app-data and world-content nodes untouched (STANDARDS §205).
4. The knowledge score cache can be rebuilt from `event_log` alone (STANDARDS §213).
5. No existing `AppDataDatabase`/`UserDataDatabase` table is modified by this layer.
