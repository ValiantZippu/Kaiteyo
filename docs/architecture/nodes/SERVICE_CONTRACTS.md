# Service Contracts

**Status**: TARGET — these are the stable internal interfaces (STANDARDS §209) the node/
Journey architecture is built on. Some services already exist in some form
(see `docs/planning/ENGINEERING_AUDIT.md` §3 and `docs/architecture/OVERVIEW.md`); where
marked CURRENT, the contract below is the consolidation target. Where marked TARGET,
nothing exists yet.
**Source spec**: [Node Architecture master spec](../NODE_ARCHITECTURE.md) §76–§162 ·
STANDARDS §209 (internal service interfaces) · §244 (Journey boundary)

> **Contract rules**
> 1. Services are Kotlin interfaces (or equivalent) in the domain layer. UI depends on
>    services, never on repositories or databases directly (STANDARDS §177–§178).
> 2. Every method returns `Result<T>` or suspends; every method has a defined error
>    model; optional integrations fail gracefully with typed errors (STANDARDS §201,
>    §219).
> 3. Every service emits the events listed (see [EVENT_CATALOG](EVENT_CATALOG.md)); no
>    service writes another service's tables.
> 4. Errors are user-explainable (STANDARDS §296): `type`, `message`, `userAction`,
>    `recoverable`.
> 5. Services never expose database implementation details (STANDARDS §209).

---

## 1. NodeService

**Status**: TARGET (node layer, ADR-0013). Owns: `node`/`edge` stores (§NODE_DATA_MODEL 2).

```kotlin
interface NodeService {
    suspend fun getNode(id: String): Result<Node>
    suspend fun getNodes(ids: List<String>): Result<List<Node>>
    suspend fun findByProvenance(source: Source, sourceId: String): Result<Node?>
    suspend fun findNodes(nodeType: String?, family: String?, query: NodeQuery): Result<List<Node>>

    suspend fun createNode(request: CreateNodeRequest): Result<Node>          // validates registry + provenance
    suspend fun updateNodePayload(id: String, payload: Map<String, Any?>): Result<Node>
    suspend fun setStatus(id: String, status: NodeStatus): Result<Node>       // active/archived/suspended/hidden/draft

    suspend fun getOutgoingEdges(nodeId: String, type: String?): Result<List<Edge>>
    suspend fun getIncomingEdges(nodeId: String, type: String?): Result<List<Edge>>
    suspend fun traverse(from: String, hopTypes: List<String>, depth: Int, filter: TraversalFilter): Result<TraversalResult>
    suspend fun createEdge(request: CreateEdgeRequest): Result<Edge>          // validates relationship registry
    suspend fun deleteEdge(edgeId: String): Result<Unit>                      // tombstone policy per type
}
```

| Method | Errors | Events |
|---|---|---|
| `getNode` | `NODE_NOT_FOUND` | — |
| `createNode` | `INVALID_NODE_TYPE`, `INVALID_PAYLOAD`, `DUPLICATE_PROVENANCE` | `node_created` |
| `createEdge` | `INVALID_EDGE_TYPE`, `DANGLING_REFERENCE`, `DUPLICATE_EDGE`, `CYCLE_FORBIDDEN` | `edge_created` |
| `deleteEdge` | `EDGE_NOT_FOUND`, `TOMBSTONE_NOT_ALLOWED` | `edge_retired` |
| `setStatus` | `NODE_NOT_FOUND` | `node_status_changed` |

Validation: node types from [NODE_TYPE_REGISTRY](NODE_TYPE_REGISTRY.md); edge types from
[RELATIONSHIP_REGISTRY](RELATIONSHIP_REGISTRY.md); payload validated against the type's
`schemaVersion`. `related_to` requires justification (registry lint, §80).

---

## 2. KnowledgeGraphService

**Status**: TARGET. Owns: cross-domain edges and the §149 bridge. Thin specialization of
NodeService for graph queries the product is built on.

```kotlin
interface KnowledgeGraphService {
    suspend fun wordsContainingKanji(kanjiId: String, limit: Int): Result<List<Node>>
    suspend fun kanjiOfWord(wordId: String): Result<List<Node>>
    suspend fun relatedNodes(nodeId: String, hopTypes: List<String>?, limit: Int): Result<List<RelatedNode>>
    suspend fun whereHaveISeen(nodeId: String, userId: String): Result<List<ExposureHit>>
    suspend fun mediaAppearances(nodeId: String): Result<List<MediaAppearance>>
    suspend fun journeyLinks(nodeId: String): Result<List<JourneyLink>>
    suspend fun languageNodesForWorldObject(worldObjectId: String): Result<List<Node>>
}
```

`ExposureHit`: `{ nodeId, nodeType, exposureType, context, position, timestamp, sourceRef }`
— the §83 "where have I seen this?" result, grouped by world in the UI.

`MediaAppearance`: `{ mediaNodeId, mediaType, title, timestamp, subtitleLineId?, sceneId? }`.

`JourneyLink`: `{ worldObjectId, name, nameJa, locationPath, discoveredAt?, collectionRef? }`.

---

## 3. SearchService

**Status**: CURRENT, partial (suite `DictionaryService`; core app lookup). Consolidation
target over the node layer. Owns: query normalization → retrieval → ranking (§187).

```kotlin
interface SearchService {
    suspend fun search(query: String, filters: SearchFilters): Result<SearchResults>
    suspend fun searchKanji(query: String, filters: KanjiFilters): Result<List<Node>>
    suspend fun searchVocabulary(query: String, filters: VocabFilters): Result<List<Node>>
    suspend fun searchSentences(query: String, filters: SentenceFilters): Result<List<Node>>
    suspend fun searchMedia(query: String, filters: MediaFilters): Result<List<Node>>
    suspend fun searchDeckCards(query: String, deckId: String, filters: CardFilters): Result<List<Card>>
    suspend fun getSuggestions(query: String, limit: Int): Result<List<String>>
}
```

`SearchFilters` (per §129): jlpt, frequency range, reading, part of speech, pitch, source
provenance, media-linked only, knowledge state, deck membership, difficulty, language.

Pipeline (STANDARDS §187): input → normalization (NFC/NFKC, full/half width — §282) →
script detection (kanji/kana/romaji) → tokenization → query interpretation → candidate
retrieval (indexed, not brute-force — §186) → ranking → filters → presentation grouping
by node family (§77, §129).

Latency budget: see [TEST_PLAN](TEST_PLAN.md) §2 (p50/p95 targets per STANDARDS §188).

---

## 4. DictionaryService

**Status**: CURRENT (suite) / partial (core app). Owns: dictionary lookup, entries,
import of Yomitan-compatible dictionaries (AGENTS.md → Desktop Dictionary Engine).

```kotlin
interface DictionaryService {
    suspend fun lookup(text: String, mode: SearchMode): Result<List<DictionaryMatch>>
    suspend fun getEntry(entryId: String): Result<DictionaryEntry>
    suspend fun groupedLookup(text: String): Result<Map<String, List<DictionaryMatch>>>   // grouped by dictionary
    suspend fun getInstalledDictionaries(): Result<List<InstalledDictionary>>
    suspend fun setEnabled(dictionaryId: String, enabled: Boolean): Result<Unit>
    suspend fun importDictionary(fileRef: PlatformFile, options: ImportOptions): Result<ImportResult>
    suspend fun reindex(dictionaryId: String): Result<Unit>
}
```

Node integration: every dictionary entry resolves to (or creates) language nodes with
`(source, source_id)` provenance; matches become node anchors for §81 traversal chips.
Import pipeline keeps `source`/`sourceId`/license (§184–§185, §202).

---

## 5. LibraryService (decks, cards, notes, collections)

**Status**: CURRENT (core Library hub; suite learning store) — consolidation target.
Owns: decks, cards, notes, collections, bulk operations (§128).

```kotlin
interface LibraryService {
    suspend fun getLibraryView(filter: LibraryFilter): Result<LibraryView>       // All/Decks/Collections/Imported/Recent/Favorites
    suspend fun createDeck(request: CreateDeckRequest): Result<Deck>
    suspend fun getDeck(deckId: String): Result<Deck>
    suspend fun getDeckPage(deckId: String, section: DeckSection): Result<DeckPageData>  // Overview/Study/Cards/Browse/Stats/Settings
    suspend fun updateDeck(deckId: String, patch: DeckPatch): Result<Deck>
    suspend fun archiveDeck(deckId: String): Result<Unit>
    suspend fun createCard(request: CreateCardRequest): Result<Card>
    suspend fun bulkAction(request: BulkActionRequest): Result<BulkActionResult> // select/tag/move/merge/export/suspend/bury/delete
    suspend fun createCollection(request: CreateCollectionRequest): Result<Collection>
    suspend fun addToCollection(collectionId: String, nodeIds: List<String>): Result<Unit>
    suspend fun getRecent(limit: Int): Result<List<Node>>
    suspend fun getFavorites(): Result<List<Node>>
}
```

Bulk safety: destructive actions require confirm + export/undo where practical
(STANDARDS §205–§207); `delete` respects tombstoning for mined/provenance-bearing cards.

---

## 6. ReviewService (scheduler)

**Status**: CURRENT (FSRS-5, ADR-0006). Owns: review queue, scheduling arithmetic.
**Never changed** per the never-change list (STANDARDS §6).

```kotlin
interface ReviewService {
    suspend fun getDueCards(deckId: String?, limit: Int): Result<List<Card>>
    suspend fun getNewCards(deckId: String?, limit: Int): Result<List<Card>>
    suspend fun submitReview(cardId: String, rating: Rating, timeMs: Long): Result<ReviewOutcome>
    suspend fun getReviewStats(deckId: String?): Result<ReviewStats>
    suspend fun suspendCard(cardId: String): Result<Unit>
    suspend fun buryCard(cardId: String): Result<Unit>
}
```

Integration: `submitReview` emits `card_reviewed` events; the knowledge service consumes
them to move user-knowledge states (KNOWLEDGE_STATE_MODEL §4) — the scheduler never
touches knowledge tables and vice versa.

---

## 7. StatsService / EventService

**Status**: CURRENT (event-driven statistics repository) — expansion target. Owns:
`event_log` (append), aggregations (derived), heatmap, reports (STANDARDS §210–§214).

```kotlin
interface EventService {
    suspend fun record(event: DomainEvent): Result<Unit>          // validated, schema-versioned
    suspend fun query(userId: String, filter: EventFilter): Result<EventPage>
}

interface StatsService {
    suspend fun getOverview(userId: String): Result<StatsOverview>            // knowledge dials + heatmap glance (§131)
    suspend fun getSection(section: StatsSection): Result<StatsSectionData>   // Learning/Kanji/Vocab/Grammar/Media/Exams/Journey
    suspend fun getHeatmap(userId: String, year: Int): Result<Heatmap>
    suspend fun getKnowledgeScores(userId: String): Result<KnowledgeScores>   // derived from KNOWLEDGE_STATE_MODEL §5
    suspend fun getDrillDown(userId: String, section: StatsSection, nodeId: String?): Result<DrillDown>
}
```

Every number is explainable from events (STANDARDS §213, §290); no fabricated precision.

---

## 8. ExamService

**Status**: CURRENT (ExamEngine + JLPT simulation) — modularization target
(STANDARDS §287–§289).

```kotlin
interface ExamService {
    suspend fun createExam(request: CreateExamRequest): Result<Exam>           // sources: knowledge, history, JLPT, course, deck, media, Journey
    suspend fun getQuestions(examId: String, count: Int): Result<List<Question>>
    suspend fun submitAnswer(examId: String, questionId: String, answer: Answer): Result<AnswerResult>
    suspend fun completeExam(examId: String): Result<ExamResult>
    suspend fun getExamHistory(userId: String): Result<List<ExamRecord>>
}
```

- Question types (STANDARDS §287): multiple choice, typing, reading, listening, writing,
  dictation, sentence completion, translation, kanji recognition, kanji writing,
  vocabulary, grammar, pitch, comprehension.
- Generation derives from real user state (STANDARDS §288) — never random content
  disconnected from what the user learned.
- Answers stored with question version + exam version for historical interpretability
  (STANDARDS §289).

---

## 9. MediaService / SubtitleService

**Status**: CURRENT (MediaEngine, subtitle engine) — node-model integration target (§130).

```kotlin
interface MediaService {
    suspend fun getLibrary(filter: MediaFilter): Result<MediaLibrary>          // Library/Continue Watching/Playlists/Folders/Anime/Movies/Videos/Audio/Mining/History
    suspend fun getPlaybackSession(mediaId: String): Result<PlaybackSession>
    suspend fun updatePlaybackPosition(mediaId: String, positionMs: Long): Result<Unit>
    suspend fun createPlaylist(name: String, mediaIds: List<String>): Result<Playlist>
    suspend fun importMedia(fileRef: PlatformFile): Result<MediaNode>
}

interface SubtitleService {
    suspend fun loadTrack(mediaId: String, trackRef: String): Result<SubtitleTrack>
    suspend fun getLines(trackId: String, range: TimeRange): Result<List<SubtitleLine>>
    suspend fun lookupAt(trackId: String, positionMs: Long): Result<SubtitleLine?>
    suspend fun indexLines(mediaId: String): Result<IndexReport>               // builds appears_in_media edges
}
```

Integration: subtitle lines become MEDIA-family nodes; `indexLines` materializes
word→line edges (`appears_in_media`) through the KnowledgeGraphService — this is the
engine behind §83 and §150's media hop. Subtitle parsing stays independent of the player
backend (STANDARDS §195).

---

## 10. MiningService

**Status**: CURRENT (MiningEngine + MiningPayload) — node integration target (§130, §150).

```kotlin
interface MiningService {
    suspend fun mine(payload: MiningPayload): Result<MinedCard>
    suspend fun mineFromDictionary(match: DictionaryMatch): Result<MinedCard>
    suspend fun getMiningHistory(userId: String, filter: MiningFilter): Result<List<MiningEvent>>
    suspend fun undoMine(minedCardId: String): Result<Unit>
}
```

Node integration: `mine()` creates a card (Library), a `mining_event` node, and
`mined_from` edges (card ← subtitle/scene/photo/object) with provenance. Duplicate
protection via MinedRecord (existing behavior). `mine()` accepts sources: dictionary,
browser, video/subtitle, OCR, clipboard, reader, image, audio, integrations API, Journey
(photo/object).

---

## 11. JourneyService (domain boundary, §244)

**Status**: TARGET (ADR-0014). The stable boundary between the application and the game
runtime. The runtime talks only to this service — never to application tables.

```kotlin
interface JourneyService {
    // world access
    suspend fun getWorldsInstalled(): Result<List<WorldSummary>>
    suspend fun openWorld(worldId: String): Result<WorldSession>
    suspend fun closeWorld(sessionId: String): Result<Unit>

    // player/state
    suspend fun getPlayerState(sessionId: String): Result<PlayerState>
    suspend fun save(sessionId: String): Result<Unit>
    suspend fun load(sessionId: String): Result<PlayerState>

    // world → knowledge bridge (§149)
    suspend fun exposeLanguageNode(sessionId: String, languageNodeId: String, exposure: ExposureKind): Result<Unit>
    suspend fun getGlossary(sessionId: String, languageNodeId: String): Result<GlossaryEntry>

    // discoveries, collections, photos, quests
    suspend fun recordDiscovery(sessionId: String, discovery: DiscoveryRequest): Result<Discovery>
    suspend fun recordPhoto(sessionId: String, photo: PhotoRequest): Result<Photograph>
    suspend fun progressQuest(sessionId: String, questId: String, objectiveId: Int, result: QuestObjectiveResult): Result<QuestProgress>
    suspend fun advanceStory(sessionId: String, storyId: String, beatId: String): Result<StoryState>

    // integration events for stats
    suspend fun reportJourneyEvent(sessionId: String, event: JourneyEvent): Result<Unit>
}
```

Errors: `WORLD_NOT_FOUND`, `WORLD_VERSION_INCOMPATIBLE`, `SAVE_CORRUPT`, `QUEST_STATE_INVALID`,
`STORY_ORDER_VIOLATION`, `SESSION_EXPIRED`. All recoverable; never crash the app
(STANDARDS §219).

---

## 12. WorldStreamingService (cell system, §92)

**Status**: TARGET. Owns: cell loading/unloading, LOD, cache (STANDARDS §267).

```kotlin
interface WorldStreamingService {
    suspend fun getCellsAround(position: WorldPosition, radius: Int): Result<List<CellLoad>>
    suspend fun unloadCells(cellIds: List<String>): Result<Unit>
    suspend fun getCell(cellId: String): Result<WorldCell>
    suspend fun getObject(objectId: String): Result<WorldObject>
    suspend fun getInteractions(objectId: String): Result<List<Interaction>>
}
```

Cache: owner = streaming service; size limit per platform tier (§143); eviction = LRU +
distance; persistence = none (content is immutable packages); invalidation = package
version change.

---

## 13. QuestService / StoryService

**Status**: TARGET (data-driven, JOURNEY_WORLD_SCHEMA §7–§8).

```kotlin
interface QuestService {
    suspend fun getActiveQuests(sessionId: String): Result<List<QuestProgress>>
    suspend fun getQuest(questId: String): Result<Quest>
    suspend fun evaluateObjectives(sessionId: String, questId: String, worldState: WorldState): Result<QuestEvaluation>
    suspend fun acceptQuest(questId: String): Result<Unit>
    suspend fun completeQuest(questId: String): Result<QuestReward>
}

interface StoryService {
    suspend fun getCurrentBeat(sessionId: String, storyId: String): Result<StoryBeat>
    suspend fun advance(sessionId: String, storyId: String, choiceId: String?): Result<StoryBeat>
    suspend fun getDialogue(beatId: String): Result<List<DialogueLine>>
}
```

Quest condition types: INTERACT, COLLECT, DISCOVER, PHOTOGRAPH, TALK, READ, LISTEN, VISIT,
WRITE, REVIEW, EXAM. Story order is enforced by `requires` edges; optional beats are
author-declared (JOURNEY_WORLD_SCHEMA §8).

---

## 14. NPCSchedulerService (deterministic schedules, §98)

**Status**: TARGET.

```kotlin
interface NpcSchedulerService {
    suspend fun resolve(NpcId, worldTime: WorldTime, weather: WeatherKind, season: Season): Result<NpcScheduleSlot>
    suspend fun getActivities(NpcId, time: WorldTime): Result<List<Activity>>
    suspend fun getRelationship(userId: String, npcId: String): Result<Int>
    suspend fun changeRelationship(userId: String, npcId: String, delta: Int): Result<Int>
}
```

Deterministic by construction: schedule slots are authored data; the same
(NPC, time, weather, season, quest-state) input yields the same output — required for
debugging (§98) and save/load determinism (§144).

---

## 15. PhotographyService (§95)

**Status**: TARGET.

```kotlin
interface PhotographyService {
    suspend fun capture(sessionId: String, cameraState: CameraState): Result<Photograph>
    suspend fun recognize(photoId: String): Result<PhotoRecognition>          // object recognition + knowledge extraction
    suspend fun applyFilter(photoId: String, filter: FilterId): Result<Photograph>
    suspend fun getGallery(userId: String, worldId: String): Result<List<Photograph>>
    suspend fun linkToCollection(photoId: String, collectionId: String): Result<Unit>
}
```

A photo may become: discovery, collection item, memory, quest objective, or card source
(mine path). Recognition produces `depicts` edges; failed recognition is explicit (no
fabricated links, §95 acceptance criteria).

---

## 16. DialogueService (§99)

**Status**: TARGET.

```kotlin
interface DialogueService {
    suspend fun getDialogue(dialogueId: String, playerLevel: Difficulty, state: WorldState): Result<Dialogue>
    suspend fun choose(choiceId: String): Result<DialogueOutcome>             // applies conditions/effects
    suspend fun getKnowledgeNodes(dialogueId: String, lineIndex: Int): Result<List<Node>>
}
```

Difficulty adaptation (§113): line variants keyed by level; knowledge links surface only
on demand (§112).

---

## 17. ContentService / PackageService (§145–§148, ADR-0015)

**Status**: TARGET (pipeline specified; see [CONTENT_AUTHORING](CONTENT_AUTHORING.md)).

```kotlin
interface ContentService {
    suspend fun installPackage(packageFile: PlatformFile, verify: VerifyOptions): Result<InstalledPackage>
    suspend fun uninstallPackage(packageId: String): Result<Unit>
    suspend fun getInstalledPackages(): Result<List<InstalledPackage>>
    suspend fun validatePackage(packageFile: PlatformFile): Result<ValidationReport>   // §148 gates
    suspend fun getPackageContent(packageId: String, query: PackageQuery): Result<ContentPage>
}
```

Install enforces: manifest hash, dependency versions, min engine version, license
metadata (§145, STANDARDS §259–§261), and never executes content code (STANDARDS §361).

---

## 18. IntegrationService (Anki / Yomitan / AniList, STANDARDS §199–§201, §292–§293)

**Status**: CURRENT (`.apkg`, AnkiConnect partial; Yomitan import) — adapter targets.

```kotlin
interface IntegrationService {
    suspend fun getIntegrations(): Result<List<IntegrationStatus>>
    suspend fun anki(): AnkiService
    suspend fun yomitan(): YomitanService
    suspend fun metadataProvider(): AnimeMetadataProvider
}

interface AnkiService {          // client abstraction — no scattered HTTP (STANDARDS §200)
    suspend fun getStatus(): Result<AnkiStatus>                // "Anki unavailable" ≠ crash (§201)
    suspend fun addNote(request: AnkiNoteRequest): Result<Long>
    suspend fun createDeck(name: String): Result<Long>
    suspend fun findNotes(query: String): Result<List<Long>>
    suspend fun updateNoteFields(noteId: Long, fields: Map<String, String>): Result<Unit>
    suspend fun addTags(noteIds: List<Long>, tags: List<String>): Result<Unit>
    suspend fun suspendNotes(noteIds: List<Long>): Result<Unit>
    suspend fun buryNotes(noteIds: List<Long>): Result<Unit>
    suspend fun storeMedia(fileRef: PlatformFile): Result<String>
}

interface YomitanService {       // glossary engine over compatible dictionary data (§197)
    suspend fun parseDictionary(fileRef: PlatformFile): Result<DictImportBundle>
    suspend fun lookup(text: String): Result<List<DictionaryMatch>>
}

interface AnimeMetadataProvider {
    suspend fun searchSeries(title: String): Result<List<SeriesSummary>>
    suspend fun getSeries(seriesId: String): Result<Series>
}
```

External services are adapters behind these interfaces (STANDARDS §292); rate limits,
caching, backoff, timeouts are in the adapters (STANDARDS §293). External data never
equals official mastery (STANDARDS §291).

---

## 19. SettingsService / ThemeService

**Status**: CURRENT (settings; theme system ADR-0002). Categories per §132.

```kotlin
interface SettingsService {
    suspend fun getSettings(): Result<Settings>              // typed per category; versioned
    suspend fun update(category: SettingsCategory, patch: SettingsPatch): Result<Settings>
    suspend fun resetCategory(category: SettingsCategory): Result<Unit>
}

interface ThemeService {
    suspend fun getActiveTheme(): Result<Theme>
    suspend fun applyTheme(themeId: String): Result<Unit>
    suspend fun getThemes(): Result<List<Theme>>
}
```

Settings categories (per §132): General, Appearance, Animation, Navigation, Input,
Study, Flashcards, Dictionary, Media, Subtitles, Mining, Anki, Yomitan, Sync, Journey,
Children, Privacy, Storage, Advanced, Developer. Settings changes never throw;
navigation through the center never crashes (§132).

---

## 20. SyncService / BackupService (STANDARDS §270–§271, §205–§206)

**Status**: CURRENT (GitHub sync ADR-0009; backup) — extension for node/Journey data.

```kotlin
interface SyncService {
    suspend fun sync(): Result<SyncReport>
    suspend fun getStatus(): Result<SyncStatus>
    suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution): Result<Unit>
}

interface BackupService {
    suspend fun createBackup(scope: BackupScope): Result<BackupFile>          // includes node/knowledge/save/collections/photos
    suspend fun restoreBackup(fileRef: PlatformFile): Result<RestoreReport>
    suspend fun getBackups(): Result<List<BackupRecord>>
}
```

Sync is semantic-object based (STANDARDS §271): cards, decks, reviews, settings,
knowledge, stats, Journey progress, collections. Never sync temporary UI state.
Conflicts resolve explicitly, never by blind overwrite (STANDARDS §270).

---

## 21. SaveService (world save, §144)

**Status**: TARGET. See [NODE_DATA_MODEL](NODE_DATA_MODEL.md) §7.

```kotlin
interface SaveService {
    suspend fun writeSave(userId: String, worldId: String, save: WorldSave): Result<Unit>
    suspend fun readSave(userId: String, worldId: String): Result<WorldSave>
    suspend fun deleteSave(userId: String, worldId: String): Result<Unit>
    suspend fun validateSave(save: WorldSave): Result<SaveValidation>
}
```

Versioned, checksummed, sparse overrides; learning data never included (§144).

---

## 22. Observability & diagnostics (STANDARDS §265–§266)

```kotlin
interface DiagnosticsService {
    suspend fun getDiagnostics(): Result<DiagnosticsReport>     // db status, cache status, media backend, FPS, memory, network, integration status
    suspend fun getDebugOverlay(): Result<DebugOverlay>         // dev mode: current node, world cell, loaded assets, active quests, query times
}
```

Never exposed in normal mode (§266).

---

## 23. Cross-cutting: error model

Every service error is:

```kotlin
data class ServiceError(
    val type: ErrorType,          // typed enum per service
    val message: String,          // user-explainable (§296)
    val userAction: String?,      // "Open Anki and try again"
    val recoverable: Boolean,
    val cause: Throwable? = null
)
```

Error presentation rules (STANDARDS §296–§299): explain what happened, why, what to do;
never show bare codes as the message; empty/loading/offline states are intentional and
specified per screen ([UX_FLOWS](UX_FLOWS.md)).

---

## 24. Service dependency graph

```
UI layer
  └─ Services (this document)
       ├─ NodeService / KnowledgeGraphService / SearchService / DictionaryService
       ├─ LibraryService / ReviewService / ExamService / StatsService (EventService)
       ├─ MediaService / SubtitleService / MiningService
       ├─ JourneyService ← WorldStreamingService, QuestService, StoryService,
       │                   NPCSchedulerService, PhotographyService, DialogueService,
       │                   SaveService
       ├─ ContentService (packages)
       ├─ IntegrationService (Anki/Yomitan/metadata)
       ├─ SettingsService / ThemeService
       ├─ SyncService / BackupService
       └─ DiagnosticsService
```

Domain rules live in domain logic, consumed by services, rendered by UI
(STANDARDS §177–§178). Journey runtime talks only to JourneyService (§244).
