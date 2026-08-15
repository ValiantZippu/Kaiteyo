# UX Flows

**Status**: MIXED — flows for existing surfaces (Home/Browse/Library/Media/Stats/
Settings/Dictionary) describe the *consolidation target* over what exists today
(see `docs/planning/PRODUCT_AUDIT.md`); flows for Journey/world surfaces are TARGET.
**Source spec**: [Node Architecture master spec](../NODE_ARCHITECTURE.md) §81–§83,
§120–§135, §137–§141 · STANDARDS §296–§299 (error/empty/loading/offline states)

> **Flow format**: each flow lists the user goal, the exact steps, system behavior per
> step, the empty/loading/error/offline states, and acceptance criteria. Every screen
> answers the §154 UX test (primary goal, secondary goal, hidden things, one-click
> things, keyboard/touch, small/large screen, reduced motion, offline).

---

## 1. Launchpad (§126, §135)

**Primary goal**: get to any destination in ≤2 actions. **Secondary**: search. **Hidden**:
launcher settings (reorder is out of scope).

1. User clicks the floating bubble (§8) or presses the launchpad shortcut.
2. Launchpad opens at the window center: spring scale 0.92→1.0, 180 ms, no FPS drop
   (§135).
3. Grid of destinations: **Home · Browse · Library · Media · Stats · Journey · Settings**
   (+ Dictionary/search as a search field at top). Consistent 8dp-spaced buttons
   (§126), one icon set (§228), keyboard focus ring.
4. Keyboard: arrows move focus, Enter opens, Esc closes (returns focus to bubble).
   Mouse: click opens. Touch: tap; gamepad: d-pad + A.
5. Close: same spring reverse; state preserved.

- **Empty state**: n/a (fixed grid).
- **Loading**: none (static grid) — must not flash.
- **Error**: n/a.
- **Offline**: identical (no network dependency).
- **Reduced motion**: fade instead of spring scale.
- **Acceptance criteria**: opens/closes without FPS drop on reference hardware; button
  spacing consistent; no square artifacts around icons (§135); gamepad works where a
  controller is connected; the bubble remains reachable in all window sizes.

## 2. Home (§127)

**Primary goal**: "What should I do now?" — one clear next action. **Secondary**: quick
dictionary search, glance at progress. **Hidden**: detailed reports.

1. On launch (or Home click), Home renders from live state:
   - **Today**: study target vs. completed (real counts).
   - **Continue studying**: next due deck/cards (ReviewService.getDueCards).
   - **Continue watching**: most recent media position (MediaService).
   - **Recent discoveries**: last 5 §111 discoveries (Journey or media).
   - **Collections**: last-updated collections.
   - **Quick dictionary search**: single field, Enter → Dictionary.
   - **Progress heatmap glance**: this week's mini heatmap (StatsService).
   - **Current Journey**: active quest/story objective if Journey installed.
   - **Recommended content**: derived from knowledge gaps + history (never a feed).
2. Every card is a link to its full surface; nothing on Home is decorative.
3. No social feed, no download banner, no tutorial banner (§127).

- **Empty states** (§297): "No due reviews — great job" (with a study suggestion);
  "No media yet — import a video to start mining"; "No discoveries yet"; each is a real
  CTA to the right surface.
- **Loading**: skeletons for dynamic sections (STANDARDS §298); static header renders
  immediately.
- **Error**: section-level failure shows "couldn't load — retry" per section, never a
  blank page.
- **Offline**: everything local; sync-backed sections show "last synced" quietly (§299).
- **Acceptance criteria**: at least one actionable item is always visible; the user can
  reach every other destination from Home in ≤2 clicks; no fabricated numbers.

## 3. Browse (§129)

**Primary goal**: explore the language graph, not just search. **Secondary**: filter
deeply. **Hidden**: raw node internals.

1. Type a query (e.g. 食). Suggestions appear from SearchService.
2. Results group by node family: **Kanji · Words · Sentences · Grammar · Media · Decks ·
   Journey · Collections** (§129), each a node-anchor card (§81).
3. Click any result → its node page (kanji/vocab/dictionary entry) with traversal chips.
4. Filters bar (collapsible): JLPT, frequency, reading, part of speech, pitch, source,
   media-linked, knowledge state, deck, difficulty. Filter changes re-query; state is
   preserved while browsing.
5. Empty results: explain why ("No vocabulary at JLPT N1 with reading しょく") + offer
   loosening actions (remove filters / browse the family instead).
6. Keyboard: `/` focuses search; arrows navigate results; Enter opens.

- **Acceptance criteria**: any filter combination resolves within the search latency
  budget (§188) at full dataset scale; results always group by family; traversal chips
  appear on every result; offline identical (local index).

## 4. Dictionary lookup (§81, §150 hub)

**Primary goal**: understand a word/kanji immediately. **Secondary**: act on it (card,
media, Journey). **Hidden**: provenance/confidence of sources.

1. Lookup entry: Home search, Browse, subtitle hover (§6), Journey knowledge overlay
   (§7), clipboard, or global shortcut (Ctrl+Shift+D on desktop).
2. Result card: headword, reading, gloss, pitch?, frequency, JLPT, related-node chips
   (kanji, conjugations, sentences, media, Journey objects, decks, mastery).
3. "Where have I seen this?" section (§83): media hits (episode+timestamp), Journey
   discoveries, previous reviews, mined cards — grouped by world, each with a jump action.
4. Actions: create card (mining), edit card, add tags, suspend, copy, pronunciation
   (TTS), open full dictionary (§82/§83 page).
5. Dictionary import lives in Settings→Dictionary (Yomitan-compatible archives).

- **Error**: dictionary database missing/corrupt → recoverable message + fallback
  (existing behavior: replace with fallback or report; STANDARDS §219).
- **Offline**: fully local.
- **Acceptance criteria**: lookup latency within budget; every entry reachable from every
  surface that mentions it; provenance visible on request but never intrusive.

## 5. Kanji page (§82)

**Primary goal**: master a kanji from every angle. **Secondary**: practice it. **Hidden**:
data-source attribution for each section.

1. Sections (tabs or scroll sections): Overview · Writing · Readings · Words ·
   Components · Radicals · Grammar · Sentences · Media · Frequency · JLPT · User
   Knowledge · Practice · Journey Discoveries.
2. Traversal chips in every section (Words ranked by frequency; "other kanji sharing
   this component"; media scene list).
3. Practice section generates writing/recognition drills from knowledge state
   (KNOWLEDGE_STATE_MODEL §4) — never pop-up interruptions from other surfaces (§112).
4. User Knowledge section: per-dimension state dials (§85) with "estimated" labeling
   where derived (§290).

- **Empty sections** show "not available yet" + the path to enable (e.g. "needs example
  sentence data") — never blank panels, never fabricated data (§158–§159).
- **Acceptance criteria**: from any surface that shows 食, reaching the full kanji page
  is ≤2 clicks; writing practice uses the real stroke model (STANDARDS §284–§285).

## 6. Vocabulary page (§83)

Same contract as kanji page plus: conjugations, pitch, "Where have I seen this?" and
cards/practice. Acceptance criteria per §83.

## 7. Library (§128)

**Primary goal**: manage learning workspace. **Secondary**: bulk operations. **Hidden**:
import mechanics behind a button.

1. Top-level tabs: **All · Decks · Collections · Imported · Recent · Favorites** — real
   node queries (§128 acceptance criteria), not hardcoded lists.
2. Deck page tabs: Overview · Study · Cards · Browse · Statistics · Settings.
3. Bulk mode: multi-select → tag / move / merge / export / suspend / bury / delete.
   Destructive actions confirm; delete offers export/undo where practical (STANDARDS
   §205–§207).
4. Import entry: APKG / JSON / CSV / TSV / TXT with preview + conflict policies
   (existing pipeline; ADR-0015 governs authored content separately).

- **Empty states**: "No decks — create one or import"; "No cards — mine from media".
- **Acceptance criteria**: every tab reflects live state; bulk actions never leave
  partial state (transactional); archived decks filterable + restorable (TODO P1).

## 8. Media Centre (§130)

**Primary goal**: consume media and mine from it without leaving the app. **Secondary**:
organize libraries/playlists. **Hidden**: backend details (VLC/mpv).

1. Sections: Library · Continue Watching · Playlists · Folders · Anime · Movies · Videos ·
   Audio · Mining · History.
2. Player: video + subtitle track + dictionary overlay + timeline + controls + mining +
   screenshots + notes (§130).
3. Subtitle hover/click → dictionary popup (§81) → "mine" → card appears in deck — one
   continuous flow, no app switching.
4. Screenshot → optional OCR (desktop) → lookup/mine path.
5. Playback position persisted per episode (§3 events; resume on return).

- **Error**: file missing → recoverable message with file-open action; backend missing
  (VLC not installed) → graceful degradation (existing) (STANDARDS §219, §193–§194).
- **Offline**: local media fully works; streaming/online metadata degrades with clear
  labels (§299).
- **Acceptance criteria**: subtitle → popup → mine → review is ≤4 actions; no separate
  app required for the normal learning workflow (§130).

## 9. Stats (§131)

**Primary goal**: reflect on progress. **Secondary**: drill into detail. **Hidden**:
aggregation internals.

1. Overview: knowledge dials (kanji/vocab/listening/reading/writing — §85), heatmap
   glance, key flows (reviews/day, media minutes, discoveries).
2. Drill-down: Learning → Kanji → Vocabulary → Grammar → Media → Exams → Journey; each
   section can be filtered by time range and opened per node ("my history with 食").
3. Every number explains its basis on hover/tap ("based on 42 reviews over 30 days") or
   is labeled estimated (§290).

- **Empty**: new user sees "no data yet — start studying" with CTA; no zero-filled
  fake charts.
- **Offline**: all local.
- **Acceptance criteria**: no fabricated precision; every drill-down reachable in ≤3
  clicks; Journey section appears only when Journey data exists (§131).

## 10. Settings (§132)

**Primary goal**: change something specific. **Secondary**: discover options. **Hidden**:
advanced/developer categories.

1. Categories sidebar/grid: General · Appearance · Animation · Navigation · Input ·
   Study · Flashcards · Dictionary · Media · Subtitles · Mining · Anki · Yomitan · Sync ·
   Journey · Children · Privacy · Storage · Advanced · Developer.
2. Category availability adapts per platform (Journey/Children only where applicable).
3. Every change persists immediately or explicitly ("Save" where batch); settings
   changes never throw; navigation through the center never crashes (§132).
4. Search within settings (optional, advanced).

- **Acceptance criteria**: every category reachable in ≤2 clicks; settings survive
  relaunch; Journey/Children sections are hidden on platforms where unsupported.

## 11. Floating bubble & sidebar (§133–§134)

1. Bubble is draggable; release → nearest of 3 snap points per side → magnetic pull →
   small elastic settle (§133). Position persists.
2. Click = Launchpad; hold / right-click = alternate menu (copy, settings, quit — no
   redundant Quick Access).
3. Sidebar: ≈20% nav / 80% content on desktop; resizable within bounds; collapsible;
   on mobile becomes top/bottom nav (never full-screen takeover).

- **Acceptance criteria**: bubble never crashes on drag/resize (UI-tested, STANDARDS
  §218); snap is elastic, never teleport; content stays visible at all window sizes.

## 12. Journey — world entry (§141, §138–§140)

**Primary goal**: explore and learn in context. **Secondary**: quests/photography.

1. Launchpad → Journey → world selection (installed packages; §145) → "enter".
2. Transition: spring cross-fade into the world; HUD fades in (time, weather, location,
   objective, interaction prompt, camera control).
3. Movement: first/third person toggle (deliberate action, §96); input per STANDARDS
   §251–§253.
4. Proximity to an object → interaction prompt (§139): `[Interact] おにぎり Onigiri`.
   Interact → contextual options (EXAMINE/PHOTOGRAPH/READ/TALK per §94).
5. Dictionary action → knowledge overlay (§140): compact glossary → expand to full
   dictionary → actions (card/tags/pronunciation/related). All inside the world.
6. HUD stays minimal; quest UI appears only as objective card/map marker (§101).

- **Empty states**: no worlds installed → "Journey packs available in Settings →
  Journey" (download flow, clearly labeled); no discoveries yet in Journal.
- **Loading**: world/cell streaming shows smooth loading (never frozen; §92, §298).
- **Error**: package version mismatch → clear message + update path (§145); save corrupt
  → recover last good save with explanation (§144, STANDARDS §219).
- **Offline**: installed worlds play fully offline (STANDARDS §182).
- **Acceptance criteria**: the full §87 loop (onigiri example) completes end-to-end;
  exiting preserves all state; entering again resumes exactly.

## 13. Journey — photography (§95, §139)

1. Camera mode (first-person default for photography, §96) → framing guides → capture.
2. Recognition: photo links to object/nodes via `depicts` (explicit when recognized,
   explicit "not recognized" otherwise — no fabricated links).
3. Photo → gallery (Journal) → options: add to collection, set as memory, quest
   objective, or mine to card (§95 "a photo can become…").

## 14. Journey — quests (§100–§101)

1. Quest accepted (NPC/dialogue/notice board) → objective card appears (one at a time)
   + map marker.
2. Objective completes → subtle notification + quest progress; quest UI disappears when
   not relevant.
3. Completion → rewards (discovery/location/cosmetic/journal/music — §117; no
   energy/lives/loot boxes/timers).

## 15. Journey — journal (§119)

1. Journal = personal travel notebook: memories, photos, discoveries, people, places,
   words/kanji (graph-linked), stories, quests, maps.
2. Word/kanji entries link to the knowledge graph ("words I met in Kamakura" is a real
   query, §83).
3. Journal is offline, exportable with backups (STANDARDS §205–§206).

## 16. Children mode (§115)

1. Same runtime, different UX: guided quests, simpler language depth (§113), restricted
   content, parent-visible settings; no separate world build.
2. Progression paced differently; no social surfaces; safety review checklist
   (STANDARDS §356–§360 spirit applied to content).

## 17. Cross-cutting states (STANDARDS §296–§299)

| State | Behavior | Reference |
|---|---|---|
| Empty | Intentional, with CTA, never blank rectangle | §297 |
| Loading | Skeletons/progress, never silent freeze | §298 |
| Error | What happened + why + what to do; typed ServiceError (§SERVICE_CONTRACTS 23) | §296 |
| Offline | Feature works or clearly labeled dependency; never destroyed functionality | §299, §182 |
| Reduced motion | Fades instead of springs/sweeps everywhere, incl. world | §123, §254 |
| Keyboard | Every screen navigable by keyboard (incl. game HUD, §7) | §154, STANDARDS §251–§254 |
| Touch/gamepad | Parity for Launchpad, world, media, browse | §126, §251 |

## 18. Screen → node mapping (the §152 trace)

Every screen's data comes from nodes/edges/events — traceable:

| Screen | Backing services | Node families consumed |
|---|---|---|
| Launchpad | Settings, ContentService | SYSTEM |
| Home | Stats, Review, Media, Journey | LEARNING, MEDIA, GAMEPLAY |
| Browse | Search, NodeService | all |
| Dictionary | DictionaryService, KnowledgeGraph | LANGUAGE, MEDIA, WORLD |
| Kanji/Vocab page | KnowledgeGraph, Knowledge, Stats | LANGUAGE, LEARNING |
| Library | LibraryService | LEARNING, MEDIA, GAMEPLAY (collections) |
| Media Centre | Media, Subtitle, Mining | MEDIA, LEARNING |
| Stats | Stats, Knowledge | all (derived) |
| Settings | Settings, Theme | SYSTEM |
| Journey world | JourneyService + runtime services | WORLD, GAMEPLAY, LANGUAGE, MEDIA |
| Journal | Journey, Discovery, Collection, Photography, Knowledge | GAMEPLAY, LANGUAGE |
