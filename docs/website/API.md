# Website — API Contracts (interactive layer)

**Status**: DOCUMENTED ONLY / PLANNED. The static site is live; every endpoint
below is a **contract** the future backend must implement. Nothing here is
claimed to work today. The static pages link to these contracts instead of
faking behavior (MASTER spec §70, §74).

Versioned: `/api/v1/`.

---

## 1. Design principles

1. **Server-side enforcement** — permissions are checked on the server. A
   contributor cannot POST a role change and become a maintainer; hiding a
   button in the UI is not security (spec §20, §39, §71).
2. **Corpus is canonical for planning data.** Task status edits write back to
   `docs/planning/MASTER_TODO.md` (or are rejected as conflicts), then the
   static board re-renders. Interactive-only data (suggestions, comments,
   activity, notifications) lives in the project store.
3. **Provenance** — accepted suggestions become roadmap/kanban items that
   reference the original suggestion; nothing is duplicated silently.
4. **Honest errors** — no optimistic success without persistence; on failure
   the client restores state and surfaces the error.
5. **Realtime** is an enhancement, never a fake. If a client cannot
   subscribe, it polls at a documented, conservative interval or shows
   "offline snapshot".

---

## 2. Auth

| Endpoint | Purpose |
|---|---|
| `POST /api/v1/auth/github` | OAuth device/authorization-code flow via GitHub |
| `POST /api/v1/auth/token` | exchange/refresh access token |
| `POST /api/v1/auth/logout` | revoke session |
| `GET /api/v1/auth/me` | current contributor + roles |

Roles: `visitor` (read public), `contributor` (suggest, comment, claim eligible
tasks), `maintainer` (triage, edit docs, move cards), `admin/owner` (approve
plans, manage permissions). See `docs/website/DATA_MODEL.md` §3.1.

---

## 3. Kanban & tasks

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/tasks` | list tasks (filters: package, priority, status, query, assignee) |
| `GET /api/v1/tasks/:id` | one task with deps, labels, links, comments |
| `PATCH /api/v1/tasks/:id` | update status/priority/assignee — **maintainer+** |
| `POST /api/v1/tasks` | create a task (from an accepted suggestion) — **maintainer+** |
| `GET /api/v1/epics` | work packages with open/done counts |

**PATCH contract**:

```json
PATCH /api/v1/tasks/KT-MEDIA-042
{
  "status": "IN_PROGRESS"
}
```

Responses: `200` with the canonical task JSON; `400` invalid status;
`401/403` auth/permission; `409` corpus conflict (the doc changed on disk —
resolve before retrying); `503` write-back unavailable.

---

## 4. Roadmap

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/roadmap` | phases + items, each with docs + kanban links |
| `PATCH /api/v1/roadmap/items/:id` | status/note — **admin/owner only** |
| `POST /api/v1/roadmap/items` | create an item from an accepted suggestion — **admin/owner** |

Acceptance of a proposal creates the roadmap item with
`accepted_from = suggestion.id` (provenance, §7).

---

## 5. Whiteboard

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/whiteboard` | full graph (nodes, edges, groups) |
| `POST /api/v1/whiteboard/nodes` | add node — **maintainer+** |
| `PATCH /api/v1/whiteboard/nodes/:id` | move/rename/relink — **maintainer+** |
| `DELETE /api/v1/whiteboard/nodes/:id` | remove (cascades edges) — **maintainer+** |
| `POST /api/v1/whiteboard/edges` | connect — **maintainer+** |
| `DELETE /api/v1/whiteboard/edges/:id` | disconnect — **maintainer+** |

Node kinds: `system|feature|task|document|database|api|integration|game|question|decision`.
Edge types: `DEPENDS_ON|USES|PRODUCES|CONSUMES|IMPLEMENTS|BLOCKS|RELATED_TO|REPLACES|EXTENDS|DOCUMENTED_BY|TESTED_BY|CONTAINS|FEEDS|CONTROLS|SYNCS|PART_OF|REWARDS`.
Server validates that both endpoints exist and the edge type is in the
vocabulary. Every write is versioned (optimistic concurrency via `updated_at`).

---

## 6. Suggestions

| Endpoint | Purpose |
|---|---|
| `POST /api/v1/suggestions` | create (draft or submit) — **visitor/contributor** |
| `GET /api/v1/suggestions` | public list (never drafts) |
| `GET /api/v1/suggestions/:id` | one suggestion + history |
| `PATCH /api/v1/suggestions/:id` | edit own draft; maintainers update state |
| `POST /api/v1/suggestions/:id/accept` | **admin/owner** — creates roadmap/kanban items with provenance |
| `POST /api/v1/suggestions/:id/reject` | **admin/owner** — requires reason |
| `POST /api/v1/suggestions/:id/comment` | comment — **contributor+** |
| `POST /api/v1/suggestions/:id/convert` | mark CONVERTED_TO_PLAN — **admin/owner** |

**Create contract** (fields per MASTER spec §17):

```json
POST /api/v1/suggestions
{
  "type": "plan",
  "title": "Add a Japanese children's curriculum",
  "problem": "…",
  "solution": "…",
  "rationale": "…",
  "affected": ["game", "learning", "content"],
  "implementation": "…",
  "deps": [],
  "risks": [],
  "alternatives": [],
  "priority_suggestion": "P2"
}
```

**State machine** (spec §18):
`DRAFT → SUBMITTED → TRIAGE → (DISCUSSION | NEEDS_MORE_INFO) → ACCEPTED | REJECTED | DUPLICATE | DEFERRED → CONVERTED_TO_PLAN`.
Transitions are enforced server-side; every transition appends an activity +
audit event. Accept/reject/request-changes always carry a recorded reason.

**Accept flow** (spec §19, §22):

```
Suggestion #183 (ACCEPTED)
   ├─▶ RoadmapItem (phase, status)
   └─▶ Epic / Tasks (created with provenance "from suggestion #183")
```

---

## 7. Decisions & documents

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/decisions` | ADR index (parsed from the corpus) |
| `GET /api/v1/documents/:path` | doc metadata + graph (related, used by, depends on, planned work) |
| `GET /api/v1/documents/:path/history` | doc version history (from git) |

Document history is derived from git blame/log of `docs/**` — author, date,
change, previous/new version. Status (CURRENT/PROPOSED/DEPRECATED) comes from
front-matter + ADR status.

---

## 8. Activity & audit

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/activity` | public activity feed (paginated) |
| `GET /api/v1/audit` | full audit log — **admin only** |
| `POST /api/v1/audit` | internal: append (server-only) |

The audit log is append-only and records `actor, action, object, before, after,
timestamp` for: proposal accepted/rejected, task moved, roadmap modified,
wiki/doc updated, permissions changed (spec §46).

---

## 9. Notifications

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/notifications` | mine (unread first) |
| `PATCH /api/v1/notifications/:id` | mark read |
| `GET /api/v1/notifications/settings` | per-kind opt-out |

Kinds: `proposal_update`, `comment_reply`, `task_assignment`, `review_request`,
`mention`, `proposal_accepted`, `proposal_rejected`, `roadmap_change`,
`documentation_discussion`. No spam: default settings are conservative and
configurable (spec §47).

---

## 10. Search

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/search?q=subtitle` | unified search across wiki, docs, tasks, roadmap, suggestions, decisions, systems |

Results include the relationship context (e.g. `subtitle` → Media Centre,
Subtitle System, Multi-word Selection, Yomitan, Mining, AnkiConnect, related
tasks) — the search index is built from the same graph as the whiteboard
(spec §31).

---

## 11. Realtime (WebSocket)

Channel: `/ws/v1/events`.

| Event | Payload |
|---|---|
| `task.updated` | task id, status, actor |
| `suggestion.accepted` | suggestion id, new roadmap/task ids |
| `roadmap.updated` | item id, status |
| `document.updated` | doc path, author |
| `activity.new` | activity event |

Contract (spec §72): connection + auth via token, subscription per object kind
(`subscribe: { kinds: ["task", "suggestion"] }`), conflict handling via
versioned objects, reconnect with resume token, offline changes queued and
replayed with server deduplication, global ordering per channel. **Do not poll
every second as a substitute.**

---

## 12. GitHub integration

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/github/link/:object` | resolve object → issue/PR/commit |
| `POST /api/v1/github/issue` | create issue from suggestion (provenance link) |
| `POST /api/v1/github/sync` | one-way import of issue state into suggestion state |

GitHub remains the development backend; the website remains the product-level
planning interface. Synchronization is documented, rate-limited, and never
replaces the canonical planning objects (spec §41, §44).

---

## 13. Security model

1. All permission checks server-side; roles per DATA_MODEL §3.1.
2. Public vs private: drafts, moderation notes, security reports, and
   maintainer-only discussions are never served to visitors (spec §39).
3. Input validation: every write validates enums (status, type, edge type),
   lengths, and ownership. No SQL interpolation from client strings.
4. Audit log not writable by users.
5. Sensitive endpoints (admin, audit) require re-authentication.
6. Moderation (spec §40): report, hide, lock, archive, spam handling,
   duplicate marking — implemented as states + activity events, not a social
   feed.

---

## 14. Implementation order (dependency-aware)

1. Auth + roles (everything else depends on it).
2. Suggestions create/read + state machine with audit.
3. Accept → roadmap/kanban conversion with provenance.
4. Kanban PATCH with corpus write-back.
5. Activity feed from event store.
6. Whiteboard CRUD.
7. Notifications.
8. Realtime layer.
9. Unified search.

Each step keeps the static site as the fallback rendering; the frontend
progressively enhances from server data (spec §37, §44).
