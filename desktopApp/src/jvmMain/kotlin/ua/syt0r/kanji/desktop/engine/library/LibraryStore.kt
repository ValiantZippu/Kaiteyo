package ua.syt0r.kanji.desktop.engine.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.engine.review.ReviewSettings
import ua.syt0r.kanji.desktop.engine.search.SearchEngine
import ua.syt0r.kanji.desktop.model.DeckDef
import ua.syt0r.kanji.desktop.model.DeckExportDto
import ua.syt0r.kanji.desktop.model.DeckModeStats
import ua.syt0r.kanji.desktop.model.DeckStats
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.LibraryProgressDto
import ua.syt0r.kanji.desktop.model.LibrarySearchResult
import ua.syt0r.kanji.desktop.model.LibrarySuggestion
import ua.syt0r.kanji.desktop.model.ReviewLogEntry
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.StudyDaySummary
import ua.syt0r.kanji.desktop.model.StudyMode
import ua.syt0r.kanji.desktop.model.StudyModeProgress
import java.io.File
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

// ============================================
// LIBRARY STORE
// The content-management heart of Kaiteyo: decks
// (built-in + custom), typed content, per-mode
// independent progress and a fast universal search
// index. All state is Compose-reactive and
// persisted to ~/.kaiteyo/library/ as JSON.
// ============================================

@Serializable
private data class HistoryDto(
    val recentSearches: List<String> = emptyList(),
    val recentlyStudied: List<String> = emptyList()
)

/** Loaded study statistics: the review log and per-day summaries. */
data class StatisticsSnapshot(
    val reviewLog: List<ReviewLogEntry> = emptyList(),
    val summaries: List<StudyDaySummary> = emptyList()
)

class LibraryStore(
    private val directory: File = File(System.getProperty("user.home"), ".kaiteyo/library")
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ------------------------------------------------------------
    // State
    // ------------------------------------------------------------
    val decks = mutableStateListOf<DeckDef>()
    private val _progress = mutableStateMapOf<String, Map<StudyMode, StudyModeProgress>>()
    val recentSearches = mutableStateListOf<String>()
    val recentlyStudied = mutableStateListOf<String>()

    /** Bumped on any structural mutation so views can recompute caches. */
    var revision by mutableStateOf(0)
        private set

    private val deckFile: File get() = File(directory, "decks.json")
    private val progressFile: File get() = File(directory, "progress.json")
    private val historyFile: File get() = File(directory, "history.json")
    private val statisticsFile: File get() = File(directory, "statistics.json")
    private val cardsFile: File get() = File(directory, "cards.json")

    init {
        directory.mkdirs()
        loadDecks()
        loadProgress()
        loadHistory()
    }

    // ------------------------------------------------------------
    // Card pool persistence
    // The desktop suite's card pool is the user's study data; it must
    // survive restarts the same way decks, progress and statistics do.
    // A missing or corrupt file simply means "no persisted pool yet"
    // so the caller can seed demo data instead of crashing.
    // ------------------------------------------------------------
    fun loadCards(): List<DesktopCard>? {
        if (!cardsFile.exists()) return null
        return runCatching {
            json.decodeFromString<List<DesktopCard>>(cardsFile.readText())
        }.getOrNull()
    }

    fun saveCards(cards: List<DesktopCard>) {
        runCatching { cardsFile.writeText(json.encodeToString(cards)) }
    }

    private fun bump() {
        revision++
    }

    // ------------------------------------------------------------
    // Deck queries
    // ------------------------------------------------------------
    fun deck(id: String?): DeckDef? = decks.firstOrNull { it.id == id }

    fun rootDecks(): List<DeckDef> = decks.filter { it.parentId == null && !it.archived }

    /** Every non-archived deck — used for move/merge targets and bulk actions. */
    fun allDecks(): List<DeckDef> = decks.filter { !it.archived }

    /**
     * Decks usable as a move/merge target for [id]: non-archived,
     * excluding the deck itself and its whole subtree (prevents cycles).
     */
    fun validTargetsFor(id: String): List<DeckDef> {
        val excluded = HashSet<String>()
        fun mark(deckId: String) {
            if (!excluded.add(deckId)) return
            decks.filter { it.parentId == deckId }.forEach { mark(it.id) }
        }
        mark(id)
        return decks.filter { !it.archived && it.id !in excluded }.sortedBy { it.name.lowercase() }
    }

    fun childrenOf(parentId: String?): List<DeckDef> = decks.filter { it.parentId == parentId && !it.archived }

    fun builtIn(): List<DeckDef> = decks.filter { it.builtIn && !it.archived }

    fun custom(): List<DeckDef> = decks.filter { !it.builtIn && !it.archived }

    fun archived(): List<DeckDef> = decks.filter { it.archived }

    fun decksForKind(kind: ua.syt0r.kanji.desktop.model.ContentKind): List<DeckDef> =
        decks.filter { it.kind == kind && !it.archived }

    // ------------------------------------------------------------
    // Deck CRUD
    // ------------------------------------------------------------
    fun create(
        name: String,
        description: String = "",
        kind: ua.syt0r.kanji.desktop.model.ContentKind = ua.syt0r.kanji.desktop.model.ContentKind.Kanji,
        parentId: String? = null,
        difficulty: Int = 2,
        tags: List<String> = emptyList()
    ): DeckDef {
        val def = DeckDef(
            id = "deck-${abs(Clock.System.now().toEpochMilliseconds())}",
            name = name.trim().ifBlank { "Untitled deck" },
            description = description,
            kind = kind,
            builtIn = false,
            parentId = parentId,
            difficulty = difficulty,
            tags = tags,
            source = "custom",
            createdAt = Clock.System.now()
        )
        decks.add(def)
        saveDecks()
        bump()
        return def
    }

    fun update(def: DeckDef) {
        val idx = decks.indexOfFirst { it.id == def.id }
        if (idx == -1) return
        decks[idx] = def
        saveDecks()
        bump()
    }

    fun delete(id: String) {
        childrenOf(id).forEach { delete(it.id) }
        decks.removeAll { it.id == id }
        saveDecks()
        bump()
    }

    fun duplicate(def: DeckDef): DeckDef {
        val copy = def.copy(
            id = "deck-${abs(Clock.System.now().toEpochMilliseconds())}",
            name = "${def.name} (copy)",
            builtIn = false,
            pinned = false,
            favorite = false,
            archived = false,
            source = "custom",
            createdAt = Clock.System.now(),
            importedAt = null
        )
        decks.add(copy)
        saveDecks()
        bump()
        return copy
    }

    fun rename(id: String, name: String) {
        val idx = decks.indexOfFirst { it.id == id }
        if (idx == -1 || name.isBlank()) return
        decks[idx] = decks[idx].copy(name = name.trim())
        saveDecks()
        bump()
    }

    fun setDescription(id: String, description: String) {
        val idx = decks.indexOfFirst { it.id == id }
        if (idx == -1) return
        decks[idx] = decks[idx].copy(description = description)
        saveDecks()
        bump()
    }

    fun setDifficulty(id: String, difficulty: Int) {
        val idx = decks.indexOfFirst { it.id == id }
        if (idx == -1) return
        decks[idx] = decks[idx].copy(difficulty = difficulty.coerceIn(1, 5))
        saveDecks()
        bump()
    }

    fun setTags(id: String, tags: List<String>) {
        val idx = decks.indexOfFirst { it.id == id }
        if (idx == -1) return
        decks[idx] = decks[idx].copy(tags = tags)
        saveDecks()
        bump()
    }

    fun togglePinned(id: String) {
        val idx = decks.indexOfFirst { it.id == id }
        if (idx == -1) return
        decks[idx] = decks[idx].copy(pinned = !decks[idx].pinned)
        saveDecks()
        bump()
    }

    fun toggleFavorite(id: String) {
        val idx = decks.indexOfFirst { it.id == id }
        if (idx == -1) return
        decks[idx] = decks[idx].copy(favorite = !decks[idx].favorite)
        saveDecks()
        bump()
    }

    fun toggleArchived(id: String) {
        val idx = decks.indexOfFirst { it.id == id }
        if (idx == -1) return
        decks[idx] = decks[idx].copy(archived = !decks[idx].archived)
        saveDecks()
        bump()
    }

    /** Move a deck under another parent (null = root). Prevents cycles. */
    fun move(id: String, newParentId: String?) {
        if (id == newParentId) return
        if (newParentId != null) {
            var cursor: String? = newParentId
            while (cursor != null) {
                if (cursor == id) return
                cursor = decks.firstOrNull { it.id == cursor }?.parentId
            }
        }
        val idx = decks.indexOfFirst { it.id == id }
        if (idx == -1) return
        decks[idx] = decks[idx].copy(parentId = newParentId)
        saveDecks()
        bump()
    }

    /**
     * Merge [fromId] into [intoId]: the source deck's explicit card
     * membership is folded into the target, then the source deck is
     * deleted (including its children). Returns true when it happened.
     */
    fun merge(intoId: String, fromId: String): Boolean {
        if (intoId == fromId) return false
        val targetIdx = decks.indexOfFirst { it.id == intoId }
        val sourceIdx = decks.indexOfFirst { it.id == fromId }
        if (targetIdx == -1 || sourceIdx == -1) return false
        val target = decks[targetIdx]
        val source = decks[sourceIdx]
        decks[targetIdx] = target.copy(
            cardIds = (LinkedHashSet(target.cardIds) + source.cardIds).toList(),
            tags = (LinkedHashSet(target.tags) + source.tags).toList().distinct()
        )
        delete(fromId)
        saveDecks()
        bump()
        return true
    }

    // ------------------------------------------------------------
    // Membership resolution
    // ------------------------------------------------------------
    /**
     * The cards in a deck: explicit membership ∪ dynamic filter
     * matches. Built-in decks are pure filters, so they grow with
     * any imported content automatically.
     */
    fun cardsIn(deck: DeckDef, cards: List<DesktopCard>): List<DesktopCard> {
        val byId = cards.associateBy { it.id }
        val result = LinkedHashMap<String, DesktopCard>()
        deck.cardIds.forEach { id -> byId[id]?.let { result[id] = it } }
        if (deck.filterQuery.isNotBlank()) {
            val expr = SearchEngine.parse(deck.filterQuery).getOrNull()
            if (expr != null) {
                cards.forEach { card -> if (SearchEngine.matches(card, expr)) result[card.id] = card }
            }
        }
        return result.values.toList()
    }

    fun contains(deck: DeckDef, card: DesktopCard, cards: List<DesktopCard>): Boolean =
        deck.cardIds.contains(card.id) ||
            (deck.filterQuery.isNotBlank() && SearchEngine.matches(card, deck.filterQuery))

    /** Every non-archived deck whose membership (explicit or filter) includes [card]. */
    fun decksContaining(card: DesktopCard, cards: List<DesktopCard>): List<DeckDef> =
        decks.filter { !it.archived && contains(it, card, cards) }

    fun deckIdFor(card: DesktopCard, cards: List<DesktopCard>): String? =
        decksContaining(card, cards).firstOrNull()?.id

    // ------------------------------------------------------------
    // Membership mutations
    // ------------------------------------------------------------
    fun addCards(deckId: String, cardIds: List<String>) {
        val idx = decks.indexOfFirst { it.id == deckId }
        if (idx == -1 || cardIds.isEmpty()) return
        val current = decks[idx]
        decks[idx] = current.copy(cardIds = (LinkedHashSet(current.cardIds) + cardIds).toList())
        saveDecks()
        bump()
    }

    fun removeCards(deckId: String, cardIds: List<String>) {
        val idx = decks.indexOfFirst { it.id == deckId }
        if (idx == -1 || cardIds.isEmpty()) return
        val current = decks[idx]
        decks[idx] = current.copy(cardIds = current.cardIds.filterNot { it in cardIds })
        saveDecks()
        bump()
    }

    fun moveCards(fromDeckId: String, toDeckId: String, cardIds: List<String>) {
        removeCards(fromDeckId, cardIds)
        addCards(toDeckId, cardIds)
    }

    fun cardsInAny(cardIds: List<String>, cards: List<DesktopCard>): List<DesktopCard> {
        val byId = cards.associateBy { it.id }
        return cardIds.mapNotNull { byId[it] }
    }

    // ------------------------------------------------------------
    // Per-mode progress
    // ------------------------------------------------------------
    fun modeProgress(cardId: String, mode: StudyMode): StudyModeProgress =
        _progress[cardId]?.get(mode) ?: StudyModeProgress()

    fun setModeProgress(cardId: String, mode: StudyMode, progress: StudyModeProgress) {
        val current = _progress[cardId] ?: emptyMap()
        _progress[cardId] = current + (mode to progress)
        saveProgress()
    }

    /** Apply many mode-progress entries and persist once (used by demo seeding). */
    fun bulkSetProgress(progress: Map<String, Map<StudyMode, StudyModeProgress>>) {
        progress.forEach { (cardId, modes) -> _progress[cardId] = modes }
        saveProgress()
        bump()
    }

    fun allProgressFor(cardId: String): Map<StudyMode, StudyModeProgress> =
        _progress[cardId] ?: emptyMap()

    /** Grade one card in one mode. Updates only that mode's progress. */
    fun recordRating(cardId: String, mode: StudyMode, rating: ReviewRating, now: Instant = Clock.System.now()) {
        val current = modeProgress(cardId, mode)
        val updated = LibraryScheduler.schedule(current, rating, now)
        setModeProgress(cardId, mode, updated)
        recentlyStudied.remove(cardId)
        recentlyStudied.add(0, cardId)
        while (recentlyStudied.size > 100) recentlyStudied.removeAt(recentlyStudied.lastIndex)
        saveHistory()
        bump()
    }

    fun suspend(cardId: String, mode: StudyMode) {
        val current = modeProgress(cardId, mode)
        setModeProgress(cardId, mode, current.copy(status = SrsStatus.Suspended, dueAt = null))
        bump()
    }

    fun bury(cardId: String, mode: StudyMode) {
        val current = modeProgress(cardId, mode)
        setModeProgress(cardId, mode, current.copy(status = SrsStatus.Buried, dueAt = null))
        bump()
    }

    fun forget(cardId: String, mode: StudyMode) {
        setModeProgress(cardId, mode, LibraryScheduler.forget(modeProgress(cardId, mode)))
        bump()
    }

    fun reschedule(cardId: String, mode: StudyMode, days: Int) {
        setModeProgress(cardId, mode, LibraryScheduler.reschedule(modeProgress(cardId, mode), days))
        bump()
    }

    /** Seed a mode's progress for freshly added cards (keeps mode tracks aligned on first contact). */
    fun ensureProgress(cardId: String, mode: StudyMode) {
        if (mode !in (_progress[cardId] ?: emptyMap())) {
            setModeProgress(cardId, mode, StudyModeProgress())
        }
    }

    // ------------------------------------------------------------
    // Per-mode study queue
    // ------------------------------------------------------------
    /**
     * Build the study queue for a deck in one mode. Cards are
     * projected onto the mode's own SRS state, so due/new counts
     * are fully independent per mode.
     */
    fun modeQueue(
        deck: DeckDef,
        mode: StudyMode,
        cards: List<DesktopCard>,
        settings: ReviewSettings = ReviewSettings(),
        now: Instant = Clock.System.now()
    ): List<DesktopCard> {
        val projected = cardsIn(deck, cards).map { card ->
            LibraryScheduler.project(card, modeProgress(card.id, mode))
        }
        val active = projected.filter { it.status != SrsStatus.Suspended && it.status != SrsStatus.Buried }
        val newCards = if (settings.includeNew) {
            active.filter { it.status == SrsStatus.New }.take(settings.newLimit)
        } else emptyList()
        val due = active.filter { it.status != SrsStatus.New && it.dueAt != null && it.dueAt <= now }
            .take(settings.reviewLimit)
        val combined = (newCards + due).distinctBy { it.id }
        return if (settings.shuffle) combined.shuffled(java.util.Random(7)) else combined
    }

    // ------------------------------------------------------------
    // Statistics
    // ------------------------------------------------------------
    fun deckModeStats(deck: DeckDef, mode: StudyMode, cards: List<DesktopCard>, now: Instant = Clock.System.now()): DeckModeStats {
        val pool = cardsIn(deck, cards)
        val entries = pool.map { modeProgress(it.id, mode) }
        val total = entries.size
        val newCount = entries.count { it.isNew }
        val learning = entries.count { it.status == SrsStatus.Learning || it.status == SrsStatus.Relearning }
        val review = entries.count { it.status == SrsStatus.Review }
        val suspended = entries.count { it.status == SrsStatus.Suspended }
        val buried = entries.count { it.status == SrsStatus.Buried }
        val due = entries.count { it.isDue }
        val completed = entries.count { it.isLearned }
        val mastered = entries.count { it.isCompleted }
        val rated = entries.filter { it.totalReviews > 0 }
        val accuracy = if (rated.isEmpty()) 0f else rated.sumOf { it.accuracy.toDouble() }.toFloat() / rated.size
        val bestStreak = rated.maxOfOrNull { it.bestStreak } ?: 0
        val avgInterval = rated.filter { it.status == SrsStatus.Review }
            .let { r -> if (r.isEmpty()) 0.0 else r.sumOf { it.intervalDays } / r.size }
        val totalReviews = entries.sumOf { it.totalReviews }
        return DeckModeStats(
            mode = mode,
            total = total,
            newCount = newCount,
            learningCount = learning,
            reviewCount = review,
            suspendedCount = suspended,
            buriedCount = buried,
            dueCount = due,
            completedCount = completed,
            masteredCount = mastered,
            accuracy = accuracy,
            bestStreak = bestStreak,
            avgInterval = avgInterval,
            totalReviews = totalReviews
        )
    }

    fun deckStats(deck: DeckDef, cards: List<DesktopCard>, now: Instant = Clock.System.now()): DeckStats {
        val modes = StudyMode.entries.associateWith { deckModeStats(deck, it, cards, now) }
        val pool = cardsIn(deck, cards)
        return DeckStats(
            total = pool.size,
            byMode = modes,
            anyDue = modes.values.sumOf { it.dueCount },
            anyNew = modes.values.sumOf { it.newCount },
            anyCompleted = modes.values.sumOf { it.masteredCount },
            favoriteCount = pool.count { it.favorite },
            targetedKind = deck.kind
        )
    }

    // ------------------------------------------------------------
    // Universal search
    // ------------------------------------------------------------
    fun search(cards: List<DesktopCard>, query: String, limit: Int = 500): List<LibrarySearchResult> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val expr = SearchEngine.parse(q).getOrNull() ?: return emptyList()
        return cards.asSequence()
            .filter { SearchEngine.matches(it, expr) }
            .take(limit)
            .map { card -> LibrarySearchResult(card, deckIdFor(card, cards)) }
            .toList()
    }

    /** Live suggestions while typing. */
    fun suggestions(cards: List<DesktopCard>, query: String, limit: Int = 14): List<LibrarySuggestion> {
        val q = query.trim().lowercase()
        val out = LinkedHashMap<String, LibrarySuggestion>()

        fun add(s: LibrarySuggestion) {
            out.putIfAbsent(s.kind + "::" + s.title + "::" + s.payload, s)
        }

        // Deck matches.
        decks.filter { !it.archived }.forEach { d ->
            if (q.isBlank() || d.name.contains(q, true) || d.description.contains(q, true) || d.tags.any { it.contains(q, true) }) {
                add(
                    LibrarySuggestion(
                        kind = "deck",
                        title = d.name,
                        subtitle = "${d.kind.label} · ${deckDescription(d)}",
                        payload = "deck:${d.id}",
                        action = "open-deck:${d.id}"
                    )
                )
            }
        }

        // Content-kind quick scopes.
        ua.syt0r.kanji.desktop.model.ContentKind.entries.forEach { kind ->
            if (q.isBlank() || kind.label.contains(q, true) || kind.name.contains(q, true)) {
                add(LibrarySuggestion("kind", kind.label, "Browse all ${kind.label.lowercase()} content", "kind:${kind.name}"))
            }
        }

        // JLPT levels.
        if (q.isBlank() || "jlpt".contains(q) || q.contains("n5") || q.contains("n4") || q.contains("n3") || q.contains("n2") || q.contains("n1")) {
            (5 downTo 1).forEach { level ->
                add(LibrarySuggestion("jlpt", "JLPT N$level", "Filter content by JLPT level N$level", "jlpt:$level"))
            }
        }

        // School grades.
        if (q.isBlank() || "grade".contains(q)) {
            (1..6).forEach { grade ->
                add(LibrarySuggestion("grade", "School grade $grade", "Filter content by school grade $grade", "grade:$grade"))
            }
        }

        // Frequency buckets.
        if (q.isBlank() || "freq".contains(q) || "frequency".contains(q)) {
            add(LibrarySuggestion("frequency", "Top 100", "Highest frequency content", "freq:<=100"))
            add(LibrarySuggestion("frequency", "Top 500", "Very high frequency content", "freq:<=500"))
            add(LibrarySuggestion("frequency", "Top 2000", "High frequency content", "freq:<=2000"))
        }

        // Tags found in the pool.
        if (q.isNotBlank()) {
            cards.asSequence().flatMap { it.tags.asSequence() }.distinct().filter { it.contains(q, true) }.take(6).forEach { tag ->
                add(LibrarySuggestion("tag", "#$tag", "Entries tagged $tag", "tag:$tag"))
            }
        }

        // Recent searches.
        recentSearches.filter { it.contains(q, true) || q.isBlank() }.take(4).forEach { r ->
            add(LibrarySuggestion("recent", "↺ $r", "Previous search", r))
        }

        // Recently studied entries.
        if (q.isBlank() || recentSearches.isEmpty()) {
            recentlyStudied.take(4).mapNotNull { id -> cards.firstOrNull { it.id == id } }.forEach { card ->
                add(
                    LibrarySuggestion(
                        kind = "recent-entry",
                        title = card.character,
                        subtitle = card.meaning.take(60),
                        payload = card.character,
                        action = "open-entry:${card.id}"
                    )
                )
            }
        }

        return out.values.take(limit)
    }

    private fun deckDescription(d: DeckDef): String =
        if (d.description.isBlank()) "Deck" else d.description

    fun recordSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        recentSearches.remove(q)
        recentSearches.add(0, q)
        while (recentSearches.size > 30) recentSearches.removeAt(recentSearches.lastIndex)
        saveHistory()
    }

    // ------------------------------------------------------------
    // Smart scopes (for the Library rail)
    // ------------------------------------------------------------
    fun dueToday(cards: List<DesktopCard>): List<DesktopCard> =
        cards.filter { (it.status == SrsStatus.Learning || it.status == SrsStatus.Review || it.status == SrsStatus.Relearning) && it.dueAt != null && it.dueAt <= Clock.System.now() }

    fun newCards(cards: List<DesktopCard>): List<DesktopCard> =
        cards.filter { it.status == SrsStatus.New }

    fun favorites(cards: List<DesktopCard>): List<DesktopCard> =
        cards.filter { it.favorite }

    fun studiedCards(cards: List<DesktopCard>): List<DesktopCard> {
        val byId = cards.associateBy { it.id }
        return recentlyStudied.mapNotNull { byId[it] }
    }

    // ------------------------------------------------------------
    // Export / import
    // ------------------------------------------------------------
    fun exportDeck(deck: DeckDef, cards: List<DesktopCard>): String {
        val payload = DeckExportDto(deck = deck.copy(cardIds = cardsIn(deck, cards).map { it.id }), cardIds = cardsIn(deck, cards).map { it.id })
        return json.encodeToString(payload)
    }

    fun importDeck(jsonText: String, cards: List<DesktopCard>): DeckDef? = try {
        val dto = json.decodeFromString<DeckExportDto>(jsonText)
        val imported = dto.deck.copy(
            id = "deck-${abs(Clock.System.now().toEpochMilliseconds())}",
            builtIn = false,
            archived = false,
            source = "imported",
            importedAt = Clock.System.now(),
            createdAt = Clock.System.now(),
            cardIds = dto.cardIds.filter { id -> cards.any { it.id == id } }
        )
        decks.add(imported)
        saveDecks()
        bump()
        imported
    } catch (e: Exception) {
        null
    }

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------
    private fun loadDecks() {
        if (!deckFile.exists()) {
            seedBuiltIns()
            return
        }
        runCatching {
            val loaded = json.decodeFromString<List<DeckDef>>(deckFile.readText())
            decks.clear()
            decks.addAll(loaded)
            // Built-ins are always present (they may be re-seeded by imports).
            BuiltInDecks.all.forEach { builtIn ->
                if (decks.none { it.id == builtIn.id }) decks.add(builtIn)
            }
            bump()
        }.onFailure { seedBuiltIns() }
    }

    private fun seedBuiltIns() {
        decks.clear()
        decks.addAll(BuiltInDecks.all)
        saveDecks()
        bump()
    }

    /**
     * Replace the whole deck catalog with an incoming set (sync restore).
     * Blank ids are dropped and built-in decks are always re-added so the
     * library never ends up without its standard catalog.
     */
    fun restoreDecks(decks: List<DeckDef>) {
        val incoming = decks.filter { it.id.isNotBlank() }
        this.decks.clear()
        this.decks.addAll(incoming)
        BuiltInDecks.all.forEach { builtIn ->
            if (this.decks.none { it.id == builtIn.id }) this.decks.add(builtIn)
        }
        saveDecks()
        bump()
    }

    private fun loadProgress() {
        if (!progressFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<LibraryProgressDto>(progressFile.readText())
            _progress.clear()
            dto.progress.forEach { (cardId, modes) ->
                _progress[cardId] = modes.entries.associate { (name, p) ->
                    StudyMode.fromName(name) to p
                }
            }
        }
    }

    private fun loadHistory() {
        if (!historyFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<HistoryDto>(historyFile.readText())
            recentSearches.clear()
            recentSearches.addAll(dto.recentSearches)
            recentlyStudied.clear()
            recentlyStudied.addAll(dto.recentlyStudied)
        }
    }

    fun saveDecks() {
        runCatching { deckFile.writeText(json.encodeToString(decks.toList())) }
    }

    private fun saveProgress() {
        runCatching {
            val dto = LibraryProgressDto(
                progress = _progress.mapValues { (_, modes) ->
                    modes.entries.associate { (mode, p) -> mode.name to p }
                }
            )
            progressFile.writeText(json.encodeToString(dto))
        }
    }

    private fun saveHistory() {
        runCatching {
            historyFile.writeText(json.encodeToString(HistoryDto(recentSearches.toList(), recentlyStudied.toList())))
        }
    }

    // ------------------------------------------------------------
    // Study statistics (review log + daily summaries).
    // Persisted so stats survive restarts; the live lists stay owned
    // by AppState and are pushed here on every mutation.
    // ------------------------------------------------------------

    fun loadStatistics(): StatisticsSnapshot? {
        if (!statisticsFile.exists()) return null
        return runCatching {
            val dto = json.decodeFromString<StatisticsDto>(statisticsFile.readText())
            StatisticsSnapshot(
                reviewLog = dto.reviewLog.map {
                    ReviewLogEntry(
                        cardId = it.cardId,
                        reviewedAt = Instant.parse(it.reviewedAtIso),
                        rating = ReviewRating.valueOf(it.ratingName),
                        timeSpent = it.timeSpentMs.milliseconds,
                        intervalBefore = it.intervalBefore,
                        intervalAfter = it.intervalAfter,
                        wasNew = it.wasNew,
                        source = it.source
                    )
                },
                summaries = dto.summaries.map {
                    StudyDaySummary(
                        day = it.day,
                        newCount = it.newCount,
                        reviewCount = it.reviewCount,
                        correctCount = it.correctCount,
                        wrongCount = it.wrongCount,
                        timeSpent = it.timeSpentMs.milliseconds
                    )
                }
            )
        }.getOrNull()
    }

    fun saveStatistics(reviewLog: List<ReviewLogEntry>, summaries: List<StudyDaySummary>) {
        runCatching {
            statisticsFile.writeText(
                json.encodeToString(
                    StatisticsDto(
                        reviewLog = reviewLog.map {
                            ReviewLogDto(
                                cardId = it.cardId,
                                reviewedAtIso = it.reviewedAt.toString(),
                                ratingName = it.rating.name,
                                timeSpentMs = it.timeSpent.inWholeMilliseconds,
                                intervalBefore = it.intervalBefore,
                                intervalAfter = it.intervalAfter,
                                wasNew = it.wasNew,
                                source = it.source
                            )
                        },
                        summaries = summaries.map {
                            SummaryDto(
                                day = it.day,
                                newCount = it.newCount,
                                reviewCount = it.reviewCount,
                                correctCount = it.correctCount,
                                wrongCount = it.wrongCount,
                                timeSpentMs = it.timeSpent.inWholeMilliseconds
                            )
                        }
                    )
                )
            )
        }
    }
}

@Serializable
private data class StatisticsDto(
    val reviewLog: List<ReviewLogDto> = emptyList(),
    val summaries: List<SummaryDto> = emptyList()
)

@Serializable
private data class ReviewLogDto(
    val cardId: String,
    val reviewedAtIso: String,
    val ratingName: String,
    val timeSpentMs: Long = 0,
    val intervalBefore: Double = 0.0,
    val intervalAfter: Double = 0.0,
    val wasNew: Boolean = false,
    val source: String = "review"
)

@Serializable
private data class SummaryDto(
    val day: String,
    val newCount: Int = 0,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val timeSpentMs: Long = 0
)