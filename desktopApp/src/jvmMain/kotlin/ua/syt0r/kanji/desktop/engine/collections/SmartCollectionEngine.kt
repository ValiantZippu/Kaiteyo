package ua.syt0r.kanji.desktop.engine.collections

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.desktop.model.CollectionDef
import ua.syt0r.kanji.desktop.model.CollectionKind
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SmartCondition
import ua.syt0r.kanji.desktop.model.SmartField
import ua.syt0r.kanji.desktop.model.SmartOperator
import ua.syt0r.kanji.desktop.model.SmartCollectionRule
import ua.syt0r.kanji.desktop.model.SmartCollectionPresets
import ua.syt0r.kanji.desktop.model.SrsStatus
import kotlin.math.abs
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ============================================
// SMART COLLECTION ENGINE
// Evaluates smart rules (user-defined + built-in
// presets) against a pool of cards. Handles nested
// collections, automatic + manual membership, and
// pinned ordering.
// ============================================

class SmartCollectionEngine {

    /** Resolve the card ids that belong to a collection (manual ∪ automatic ∪ smart). */
    fun resolve(
        collection: CollectionDef,
        cards: List<DesktopCard>
    ): List<DesktopCard> {
        val byId = cards.associateBy { it.id }
        val manual = collection.cardIds.mapNotNull { byId[it] }
        val smart = collection.smartRule?.let { evaluate(it, cards) } ?: emptyList()
        val merged = LinkedHashMap<String, DesktopCard>()
        manual.forEach { merged[it.id] = it }
        smart.forEach { merged[it.id] = it }
        return merged.values.toList()
    }

    /** Evaluate a smart rule against the whole pool. */
    fun evaluate(rule: SmartCollectionRule, cards: List<DesktopCard>): List<DesktopCard> {
        if (rule.conditions.isEmpty()) return cards
        val results = cards.map { card -> card to matches(card, rule) }
        return results.filter { it.second }.map { it.first }
    }

    fun matches(card: DesktopCard, rule: SmartCollectionRule): Boolean {
        val outcomes = rule.conditions.map { evalCondition(card, it) }
        return when (rule.match) {
            ua.syt0r.kanji.desktop.model.MatchAll.MatchAll -> outcomes.all { it }
            ua.syt0r.kanji.desktop.model.MatchAll.MatchAny -> outcomes.any { it }
        }
    }

    fun evalCondition(card: DesktopCard, c: SmartCondition): Boolean {
        val value = c.value
        val num = value.toDoubleOrNull()

        fun cmp(actual: Double?): Boolean {
            if (actual == null) return false
            return when (c.operator) {
                SmartOperator.Equals -> actual == num
                SmartOperator.NotEquals -> actual != num
                SmartOperator.GreaterThan -> actual > num!!
                SmartOperator.LessThan -> actual < num!!
                SmartOperator.Contains -> actual.toString().contains(value)
                SmartOperator.Is -> actual == num
            }
        }

        fun str(actual: String): Boolean = when (c.operator) {
            SmartOperator.Equals -> actual.equals(value, ignoreCase = true)
            SmartOperator.NotEquals -> !actual.equals(value, ignoreCase = true)
            SmartOperator.Contains -> actual.contains(value, ignoreCase = true)
            SmartOperator.Is -> actual.equals(value, ignoreCase = true)
            SmartOperator.GreaterThan -> actual.compareTo(value) > 0
            SmartOperator.LessThan -> actual.compareTo(value) < 0
        }

        fun bool(actual: Boolean): Boolean =
            if (value.equals("true", true) || value == "1" || value == "*") actual
            else if (value.equals("false", true) || value == "0") !actual
            else false

        return when (c.field) {
            SmartField.Status -> str(card.status.name)
            SmartField.Tag -> card.tags.any { str(it) } || card.tags.any { it.equals(value, true) }
            SmartField.Flag -> card.flags.any { it.equals(value, true) } || (value == "*" && card.flags.isNotEmpty())
            SmartField.Favorite -> bool(card.favorite)
            SmartField.Accuracy -> cmp(card.accuracy.toDouble())
            SmartField.Lapses -> cmp(card.lapses.toDouble())
            SmartField.Interval -> cmp(card.intervalDays)
            SmartField.Reps -> cmp(card.reps.toDouble())
            SmartField.Jlpt -> cmp(card.jlpt?.toDouble())
            SmartField.Grade -> cmp(card.grade?.toDouble())
            SmartField.Strokes -> cmp(card.strokeCount.toDouble())
            SmartField.Frequency -> cmp(card.frequency?.toDouble())
            SmartField.Due -> duePredicate(card, value)
            SmartField.LastReviewed -> lastReviewedPredicate(card, value)
        }
    }

    private fun duePredicate(card: DesktopCard, value: String): Boolean {
        val due = card.dueAt
        return when (value.lowercase()) {
            "today" -> due?.let { it <= Clock.System.now() } ?: false
            "overdue" -> due?.let { it <= Clock.System.now() } ?: false
            "none", "never" -> due == null
            else -> due?.let {
                val days = value.toDoubleOrNull()
                days == null || it.toEpochMilliseconds() < days * 86_400_000 + Clock.System.now().toEpochMilliseconds()
            } ?: false
        }
    }

    private fun lastReviewedPredicate(card: DesktopCard, value: String): Boolean {
        val last = card.lastReviewedAt ?: return false
        val days = when {
            value.equals("today", true) -> 0
            value.equals("yesterday", true) -> 1
            else -> value.toDoubleOrNull() ?: return false
        }
        val cutoff = Clock.System.now().minus(ua.syt0r.kanji.desktop.engine.srs.intervalDaysToDuration(days.toDouble()))
        return when {
            days == 0 -> last.toLocalDateTime(TimeZone.currentSystemDefault()).date == Clock.System.todayIn(TimeZone.currentSystemDefault())
            else -> last >= cutoff
        }
    }

    // ------------------------------------------------------------
    // Built-in smart collection definitions
    // ------------------------------------------------------------

    fun builtIns(): List<CollectionDef> = listOf(
        CollectionDef("smart-recent", "Recently learned", "Studied in the last 24h", CollectionKind.Smart, smartRule = SmartCollectionPresets.recentlyLearned(1)),
        CollectionDef("smart-failed-today", "Failed today", "Answered Again today", CollectionKind.Smart, smartRule = SmartCollectionPresets.failedToday()),
        CollectionDef("smart-failed-week", "Failed this week", "Lapsed cards from the past week", CollectionKind.Smart, smartRule = SmartCollectionPresets.failedThisWeek()),
        CollectionDef("smart-low-accuracy", "Low accuracy", "Below 60% accuracy with 5+ reviews", CollectionKind.Smart, smartRule = SmartCollectionPresets.lowAccuracy(0.6f)),
        CollectionDef("smart-not-reviewed", "Not reviewed", "Cards never reviewed", CollectionKind.Smart, smartRule = SmartCollectionPresets.notReviewed()),
        CollectionDef("smart-flagged", "Flagged", "Any card with a flag", CollectionKind.Smart, smartRule = SmartCollectionPresets.flagged()),
        CollectionDef("smart-favorite", "Favorite", "Favorite cards", CollectionKind.Smart, smartRule = SmartCollectionPresets.favorite()),
        CollectionDef("smart-new", "New cards", "Never studied", CollectionKind.Smart, smartRule = SmartCollectionRule(conditions = listOf(SmartCondition(SmartField.Status, SmartOperator.Equals, SrsStatus.New.name)))),
        CollectionDef("smart-due", "Due today", "Cards due today", CollectionKind.Smart, smartRule = SmartCollectionRule(conditions = listOf(SmartCondition(SmartField.Due, SmartOperator.Equals, "today"))))
    )
}

// ============================================
// COLLECTION STORE
// Manages the tree of user collections (nested),
// favorites and pinning, with built-ins merged in.
// ============================================

class CollectionStore(
    private val smartEngine: SmartCollectionEngine = SmartCollectionEngine()
) {
    private val _collections = mutableListOf<CollectionDef>()
    val collections: List<CollectionDef> get() = _collections.toList()

    init {
        _collections.addAll(smartEngine.builtIns())
    }

    fun load(collections: List<CollectionDef>) {
        _collections.clear()
        _collections.addAll(collections)
        val ids = _collections.map { it.id }.toSet()
        _collections.removeAll { it.id.isBlank() }
        // Ensure built-ins always exist.
        smartEngine.builtIns().forEach { builtIn ->
            if (_collections.none { it.id == builtIn.id }) _collections.add(builtIn)
        }
    }

    fun create(name: String, description: String = "", kind: CollectionKind = CollectionKind.Manual, parentId: String? = null): CollectionDef {
        val def = CollectionDef(
            id = "col-${abs(Clock.System.now().toEpochMilliseconds())}",
            name = name,
            description = description,
            kind = kind,
            parentId = parentId,
            pinned = false,
            favorite = false
        )
        _collections.add(def)
        return def
    }

    fun update(def: CollectionDef) {
        val idx = _collections.indexOfFirst { it.id == def.id }
        if (idx == -1) return
        _collections[idx] = def
    }

    fun delete(id: String) {
        val children = _collections.filter { it.parentId == id }.map { it.id }
        children.forEach { delete(it) }
        _collections.removeAll { it.id == id }
    }

    fun togglePinned(id: String): Boolean {
        val idx = _collections.indexOfFirst { it.id == id }
        if (idx == -1) return false
        _collections[idx] = _collections[idx].copy(pinned = !_collections[idx].pinned)
        return true
    }

    fun toggleFavorite(id: String): Boolean {
        val idx = _collections.indexOfFirst { it.id == id }
        if (idx == -1) return false
        _collections[idx] = _collections[idx].copy(favorite = !_collections[idx].favorite)
        return true
    }

    fun childrenOf(parentId: String?): List<CollectionDef> = _collections.filter { it.parentId == parentId }

    /**
     * Resolve every card in the collection: manual + smart membership, plus
     * (when a [library] is supplied) the cards of every owned deck and every
     * descendant collection — a collection tree is a deck tree.
     */
    fun resolveCards(
        def: CollectionDef,
        cards: List<DesktopCard>,
        library: ua.syt0r.kanji.desktop.engine.library.LibraryStore? = null
    ): List<DesktopCard> {
        val byId = LinkedHashMap<String, DesktopCard>()
        smartEngine.resolve(def, cards).forEach { byId[it.id] = it }
        if (library != null) {
            fun collect(current: CollectionDef) {
                decksIn(current, library).forEach { deck ->
                    library.cardsIn(deck, cards).forEach { byId[it.id] = it }
                }
                childrenOf(current.id).forEach { collect(it) }
            }
            collect(def)
        }
        return byId.values.toList()
    }

    // ------------------------------------------------------------
    // Deck membership — a collection is a container of decks (and
    // subcollections). Deck ids are membership references: the decks
    // themselves stay canonical in the Library store.
    // ------------------------------------------------------------

    /** The decks owned by this collection (direct ids, deduplicated). */
    fun decksIn(def: CollectionDef, library: ua.syt0r.kanji.desktop.engine.library.LibraryStore): List<ua.syt0r.kanji.desktop.model.DeckDef> =
        def.deckIds.mapNotNull { library.deck(it) }.distinctBy { it.id }

    /**
     * Every deck owned by the collection or any of its descendant
     * collections, in tree order — a collection tree is a deck tree.
     */
    fun resolveDecks(
        def: CollectionDef,
        library: ua.syt0r.kanji.desktop.engine.library.LibraryStore
    ): List<ua.syt0r.kanji.desktop.model.DeckDef> {
        val result = LinkedHashSet<ua.syt0r.kanji.desktop.model.DeckDef>()
        fun collect(current: CollectionDef) {
            decksIn(current, library).forEach { result.add(it) }
            childrenOf(current.id).forEach { collect(it) }
        }
        collect(def)
        return result.toList()
    }

    /** Attach a deck to a collection (idempotent membership). */
    fun addDeck(collectionId: String, deckId: String) {
        val idx = _collections.indexOfFirst { it.id == collectionId }
        if (idx == -1) return
        val def = _collections[idx]
        if (deckId in def.deckIds) return
        _collections[idx] = def.copy(deckIds = def.deckIds + deckId)
    }

    /** Detach a deck from a collection. */
    fun removeDeck(collectionId: String, deckId: String) {
        val idx = _collections.indexOfFirst { it.id == collectionId }
        if (idx == -1) return
        val def = _collections[idx]
        if (deckId !in def.deckIds) return
        _collections[idx] = def.copy(deckIds = def.deckIds - deckId)
    }

    /** Whether a collection directly owns a deck. */
    fun ownsDeck(collectionId: String, deckId: String): Boolean =
        _collections.firstOrNull { it.id == collectionId }?.deckIds?.contains(deckId) == true

    fun rename(id: String, name: String) {
        val idx = _collections.indexOfFirst { it.id == id }
        if (idx == -1 || name.isBlank()) return
        _collections[idx] = _collections[idx].copy(name = name.trim())
    }

    fun duplicate(def: CollectionDef): CollectionDef {
        val copy = def.copy(
            id = "col-${abs(Clock.System.now().toEpochMilliseconds())}",
            name = "${def.name} (copy)",
            pinned = false,
            favorite = false,
            archived = false
        )
        _collections.add(copy)
        return copy
    }

    fun toggleArchived(id: String): Boolean {
        val idx = _collections.indexOfFirst { it.id == id }
        if (idx == -1) return false
        _collections[idx] = _collections[idx].copy(archived = !_collections[idx].archived)
        return true
    }

    /** Move a collection under another parent (null = root). Prevents cycles. */
    fun move(id: String, newParentId: String?) {
        if (id == newParentId) return
        if (newParentId != null) {
            // Reject moves that would create a cycle (child above its own subtree).
            var cursor: String? = newParentId
            while (cursor != null) {
                if (cursor == id) return
                cursor = _collections.firstOrNull { it.id == cursor }?.parentId
            }
        }
        val idx = _collections.indexOfFirst { it.id == id }
        if (idx == -1) return
        _collections[idx] = _collections[idx].copy(parentId = newParentId)
    }

    /**
     * Merge [sourceIds] into [targetId]: their manual card ids are folded into the
     * target, and the sources are deleted. If the target is empty (no rule, no cards)
     * and a single smart source exists, its rule is adopted so smart behavior survives
     * the merge. Returns the number of card ids added to the target.
     */
    fun merge(targetId: String, sourceIds: List<String>): Int {
        if (sourceIds.isEmpty()) return 0
        val targetIdx = _collections.indexOfFirst { it.id == targetId }
        if (targetIdx == -1) return 0

        val target = _collections[targetIdx]
        val mergedCardIds = LinkedHashSet(target.cardIds)
        val mergedDeckIds = LinkedHashSet(target.deckIds)
        val smartSources = sourceIds.mapNotNull { id -> _collections.firstOrNull { it.id == id } }
            .filter { it.smartRule != null }

        var added = 0
        sourceIds.mapNotNull { id -> _collections.firstOrNull { it.id == id } }.forEach { source ->
            val before = mergedCardIds.size
            mergedCardIds.addAll(source.cardIds)
            mergedDeckIds.addAll(source.deckIds)
            added += mergedCardIds.size - before
        }

        val adoptedRule = if (target.smartRule == null && target.cardIds.isEmpty() && smartSources.size == 1) {
            smartSources.first().smartRule
        } else {
            target.smartRule
        }

        sourceIds.forEach { delete(it) }
        _collections[targetIdx] = target.copy(
            cardIds = mergedCardIds.toList(),
            deckIds = mergedDeckIds.toList(),
            smartRule = adoptedRule
        )
        return added
    }

    fun archived(): List<CollectionDef> = _collections.filter { it.archived }

    fun export(def: CollectionDef, cards: List<DesktopCard>): String {
        val payload = CollectionExport(def = def, cardIds = cards.map { it.id })
        return Json { prettyPrint = true }.encodeToString(payload)
    }

    fun pinned(): List<CollectionDef> = _collections.filter { it.pinned }
    fun favorites(): List<CollectionDef> = _collections.filter { it.favorite }
}

/** Serializable payload for exporting a collection + its card membership. */
@Serializable
data class CollectionExport(
    val def: CollectionDef,
    val cardIds: List<String>,
    val exportedAt: String = kotlinx.datetime.Clock.System.now().toString()
)
