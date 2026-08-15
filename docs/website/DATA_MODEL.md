# Website — Unified Project Data Model

**Status**: TARGET (backend) — the model below is the contract the interactive
layer will persist. **Today** the same information exists as *static sources of
truth* (the docs corpus + `website/config/project/*.json`); §4 maps each entity
to its current home. Nothing here is faked: entities that have no live source
yet (comments, notifications) are listed as `no source yet`.

The model follows the MASTER spec §45: canonical objects, rendered by the UI,
referenced by documentation, synchronized with GitHub where appropriate — never
duplicated across surfaces.

---

## 1. Entity overview

| Entity | Purpose | Current home (LIVE) | Target store |
|---|---|---|---|
| `Project` | top-level aggregate (name, version, phase) | `website/config/site.json` | project DB (config row) |
| `Document` | a doc page in the corpus | `docs/**` (markdown) | corpus (read-only mirror) |
| `WikiPage` | a wiki article | `website/content/wiki/**` | wiki store (or corpus) |
| `WhiteboardNode` | a node on the canvas | `whiteboard.json` | whiteboard store |
| `WhiteboardEdge` | a typed connection | `whiteboard.json` | whiteboard store |
| `KanbanBoard` | a board (default = the project) | derived from `MASTER_TODO.md` | board store + corpus mirror |
| `KanbanColumn` | a column definition | `KANBAN_COLUMNS` in `build.py` | board store |
| `Task` | a work item (KT-*) | `docs/planning/MASTER_TODO.md` (parsed) | task store + corpus mirror |
| `Epic` / `Package` | a work package (P0–P39) | `MASTER_TODO.md` section header | task store |
| `Milestone` | a dated milestone | roadmap/changelog data | milestone store |
| `RoadmapItem` | a forward-looking item | `roadmap.json` | roadmap store + corpus mirror |
| `Suggestion` | a proposal submitted by a visitor/contributor | GitHub issues (link) | suggestions store |
| `Proposal` | a formal plan proposal (structured suggestion) | GitHub issues (link) | suggestions store |
| `Decision` | an ADR | `docs/architecture/decisions/*.md` (parsed) | corpus (read-only mirror) |
| `Comment` | discussion attached to an object | GitHub issues/discussions (link) | discussion store |
| `Discussion` | a threaded conversation | GitHub discussions (link) | discussion store |
| `Activity` | an audit-visible event | `activity.json` (snapshot) | event store (append-only) |
| `Contributor` | a person (identity) | GitHub profile (link) | user store |
| `Permission` | role → capability grant | — (roles documented in API.md) | auth store |
| `Label` | a tag (system, priority, type, …) | inline in task rows | label store |
| `Dependency` | a task→task edge | `MASTER_TODO.md` deps column | task store |
| `GitHubLink` | link to issue/PR/commit | — | link store |

---

## 2. Relationships

```
Project 1─n RoadmapItem
Project 1─n KanbanBoard
KanbanBoard 1─n KanbanColumn
KanbanColumn 1─n Task
Epic 1─n Task                    (task.package = epic.id)
Task 1─n Task                    (dependency edges, task.deps)
Task n─1 RoadmapItem             (roadmap item → kanban package filter)
Document n─n Document            (wiki cross-references)
Document 1─n WikiPage            (a page renders from a document)
WhiteboardNode 1─n WhiteboardEdge
Task n─n WhiteboardNode          (node ↔ kanban package/task links)
RoadmapItem 1─n Task             (proposal → roadmap item → tasks)
Suggestion 1─0..1 Proposal
Suggestion 1─0..1 RoadmapItem    (CONVERTED_TO_PLAN provenance)
Suggestion 1─n Activity          (accepted/rejected events)
Comment n─1 (Task|Document|Suggestion|Decision|RoadmapItem)
Activity n─1 Contributor
Permission n─1 Contributor
GitHubLink n─1 (Task|Suggestion|Activity)
```

The cross-surface links are first-class: a whiteboard node shows its kanban
tasks; a kanban card shows its docs and roadmap item; a suggestion shows the
official work it became.

---

## 3. Target schema (backend)

Suggested relational layout (SQLite/Postgres — decision left to the backend
ADR). **This is a contract, not an implementation.**

### 3.1 Identity & permission

```sql
CREATE TABLE contributors (
  id            TEXT PRIMARY KEY,      -- provider:username or uuid
  provider      TEXT NOT NULL,         -- 'github' | 'local' | ...
  display_name  TEXT NOT NULL,
  is_maintainer INTEGER NOT NULL DEFAULT 0,
  is_admin      INTEGER NOT NULL DEFAULT 0,
  created_at    TEXT NOT NULL
);

CREATE TABLE permissions (
  id          INTEGER PRIMARY KEY,
  role        TEXT NOT NULL,           -- visitor | contributor | maintainer | admin
  capability  TEXT NOT NULL,           -- e.g. 'suggestions.create', 'tasks.move'
  UNIQUE (role, capability)
);
```

### 3.2 Planning objects

```sql
CREATE TABLE epics (
  id       TEXT PRIMARY KEY,           -- 'P2'
  title    TEXT NOT NULL
);

CREATE TABLE tasks (
  id              TEXT PRIMARY KEY,    -- 'KT-MEDIA-042'
  epic_id         TEXT NOT NULL REFERENCES epics(id),
  title           TEXT NOT NULL,
  status          TEXT NOT NULL,       -- vocabulary in CURRENT_STATE.md
  priority        TEXT,                -- P0..P3
  deps            TEXT,                -- comma-separated task ids
  acceptance      TEXT,
  package_order   INTEGER,
  updated_at      TEXT NOT NULL,
  source_doc      TEXT NOT NULL        -- docs/planning/MASTER_TODO.md
);

CREATE TABLE milestones (
  id          TEXT PRIMARY KEY,
  title       TEXT NOT NULL,
  target_date TEXT
);

CREATE TABLE roadmap_items (
  id           TEXT PRIMARY KEY,       -- 'ROADMAP-14'
  phase        TEXT NOT NULL,
  title        TEXT NOT NULL,
  status       TEXT NOT NULL,          -- DONE | PARTIAL | ACTIVE | TARGET | PLANNED
  note         TEXT,
  docs_url     TEXT,
  kanban_pkg   TEXT REFERENCES epics(id),
  accepted_from TEXT REFERENCES suggestions(id)
);
```

### 3.3 Whiteboard

```sql
CREATE TABLE whiteboard_nodes (
  id          TEXT PRIMARY KEY,
  label       TEXT NOT NULL,
  kind        TEXT NOT NULL,           -- system|feature|task|document|database|api|integration|game|question|decision
  group_id    TEXT,
  status      TEXT NOT NULL,
  x           REAL NOT NULL,
  y           REAL NOT NULL,
  w           REAL,
  h           REAL,
  description TEXT,
  docs_url    TEXT,
  task_filter TEXT                     -- kanban package filter
);

CREATE TABLE whiteboard_edges (
  id      INTEGER PRIMARY KEY,
  from_id TEXT NOT NULL REFERENCES whiteboard_nodes(id),
  to_id   TEXT NOT NULL REFERENCES whiteboard_nodes(id),
  type    TEXT NOT NULL,               -- DEPENDS_ON|USES|PRODUCES|CONSUMES|IMPLEMENTS|BLOCKS|RELATED_TO|REPLACES|EXTENDS|DOCUMENTED_BY|TESTED_BY|CONTAINS|FEEDS|CONTROLS|SYNCS|PART_OF|REWARDS
  label   TEXT,
  UNIQUE (from_id, to_id, type)
);

CREATE TABLE whiteboard_groups (
  id     TEXT PRIMARY KEY,
  label  TEXT NOT NULL,
  x REAL NOT NULL, y REAL NOT NULL, w REAL NOT NULL, h REAL NOT NULL
);
```

### 3.4 Suggestions & discussion

```sql
CREATE TABLE suggestions (
  id           INTEGER PRIMARY KEY,
  type         TEXT NOT NULL,          -- feature|plan|bug|refactor|docs|design|perf|research|architecture|content
  title        TEXT NOT NULL,
  problem      TEXT,
  solution     TEXT,
  rationale    TEXT,
  affected     TEXT,
  implementation TEXT,
  deps         TEXT,
  risks        TEXT,
  alternatives TEXT,
  priority_suggestion TEXT,
  status       TEXT NOT NULL,          -- DRAFT|SUBMITTED|TRIAGE|DISCUSSION|NEEDS_MORE_INFO|ACCEPTED|REJECTED|DUPLICATE|DEFERRED|CONVERTED_TO_PLAN
  author_id    TEXT NOT NULL REFERENCES contributors(id),
  github_issue TEXT,                   -- provenance link
  created_at   TEXT NOT NULL,
  updated_at   TEXT NOT NULL
);

CREATE TABLE comments (
  id         INTEGER PRIMARY KEY,
  object_kind TEXT NOT NULL,           -- task|document|suggestion|decision|roadmap_item
  object_id  TEXT NOT NULL,
  author_id  TEXT NOT NULL REFERENCES contributors(id),
  body       TEXT NOT NULL,
  created_at TEXT NOT NULL
);

CREATE TABLE discussions (
  id          INTEGER PRIMARY KEY,
  title       TEXT NOT NULL,
  object_kind TEXT,
  object_id   TEXT,
  created_at  TEXT NOT NULL
);
```

### 3.5 Activity & audit

```sql
CREATE TABLE activity (
  id          INTEGER PRIMARY KEY,
  kind        TEXT NOT NULL,           -- commit|decision|release|documentation|task_move|proposal_*|permission_change|...
  title       TEXT NOT NULL,
  detail      TEXT,
  actor_id    TEXT REFERENCES contributors(id),
  object_kind TEXT,
  object_id   TEXT,
  created_at  TEXT NOT NULL
);

CREATE TABLE audit_log (
  id          INTEGER PRIMARY KEY,
  actor_id    TEXT NOT NULL REFERENCES contributors(id),
  action      TEXT NOT NULL,           -- 'suggestion.accept', 'task.move', 'roadmap.edit', 'permission.change', ...
  object_kind TEXT,
  object_id   TEXT,
  before      TEXT,                    -- JSON snapshot
  after       TEXT,                    -- JSON snapshot
  created_at  TEXT NOT NULL
);
-- audit_log is append-only; normal users cannot write or edit it.
```

### 3.6 Notifications

```sql
CREATE TABLE notifications (
  id          INTEGER PRIMARY KEY,
  user_id     TEXT NOT NULL REFERENCES contributors(id),
  kind        TEXT NOT NULL,           -- proposal_update|comment_reply|task_assignment|review_request|mention|proposal_accepted|proposal_rejected|roadmap_change
  object_kind TEXT,
  object_id   TEXT,
  read        INTEGER NOT NULL DEFAULT 0,
  created_at  TEXT NOT NULL
);
```

---

## 4. Mapping: static sources of truth → model

| Model entity | LIVE representation | How it stays in sync |
|---|---|---|
| `Task`, `Epic` | `MASTER_TODO.md` rows | parsed at build; editing the doc re-renders the board |
| `RoadmapItem` | `roadmap.json` | hand-maintained mirror of ROADMAP/CURRENT_STATE |
| `WhiteboardNode/Edge` | `whiteboard.json` | hand-maintained; validation in ARCHITECTURE §6.3 |
| `Decision` | `docs/architecture/decisions/*.md` | parsed at build |
| `Activity` | `activity.json` snapshot | refresh procedure (ARCHITECTURE §6.2) |
| `Suggestion` | GitHub issues (link only) | link preserved until backend ships |
| `Comment/Discussion` | GitHub issues/discussions | link preserved until backend ships |
| `Contributor`, `Permission` | — (documented roles only) | no source yet — auth is backend work |
| `Notification` | — | no source yet |

---

## 5. Consistency rules

1. A model entity has **one canonical writer**. Tasks/decisions/roadmap items
   are written in the corpus; suggestions/comments/activity are written in the
   project store. The UI renders, never invents.
2. `CONVERTED_TO_PLAN` suggestions keep `accepted_from` provenance back to the
   original proposal — the plan never orphans its history.
3. Status values are constrained to the corpus vocabulary; the schema CHECK
   constraints should encode it server-side.
4. GitHub is the code-hosting backend, not a second planner. Sync is
   one-directional for planning data (corpus → GitHub links), and
   bidirectional only where GitHub is the origin (issues → suggestions).
