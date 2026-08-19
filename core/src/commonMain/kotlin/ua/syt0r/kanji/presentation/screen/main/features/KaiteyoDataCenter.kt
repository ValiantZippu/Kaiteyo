package ua.syt0r.kanji.presentation.screen.main.features

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.app_data.data.KanjiClassificationEntry
import ua.syt0r.kanji.core.app_data.data.KanjiListEntry
import ua.syt0r.kanji.core.app_data.data.KanjiMeaningEntry
import ua.syt0r.kanji.core.app_data.data.KanjiReadingEntry
import ua.syt0r.kanji.core.app_data.data.RadicalData
import ua.syt0r.kanji.core.srs.SrsPracticeType
import ua.syt0r.kanji.core.srs.SrsCardKey
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.database.CardDatabaseManager
import ua.syt0r.kanji.core.user_data.database.FsrsCardRepository
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryRepository
import ua.syt0r.kanji.core.user_data.database.StreakData
import ua.syt0r.kanji.core.user_data.database.StudyHistoryRow
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardDifficulty
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardFlagType
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardTag
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard

// ============================================
// KAITEYO DATA CENTER
// Real, shared data layer for the redesigned shell.
// Kanji catalog (app data) + user state (flags, tags,
// favorites, SRS cards, activity) with persistence
// through CardDatabaseManager.
// ============================================

/**
 * Favorite marker persisted through `card_flag` using a
 * reserved flag type id that does not collide with colors.
 */
const val FAVORITE_FLAG_TYPE: Int = 8

/**
 * The reference kanji catalog deck. Every kanji in the bundled catalog gets
 * a "Kanji Browser" card so the browser can list/filter them, but these
 * cards are a REFERENCE, not a study queue — they must never inflate the
 * "new"/"due" counts that drive the user's daily workload.
 */
const val KANJI_BROWSER_DECK_NAME: String = "Kanji Browser"

val LETTER_WRITING_PRACTICE_TYPE: Long = SrsPracticeType.LetterWriting.value

fun isFavoriteFlag(flagType: Int?): Boolean = flagType == FAVORITE_FLAG_TYPE

class KaiteyoDataCenter(
    private val appDataRepository: AppDataRepository,
    private val fsrsCardRepository: FsrsCardRepository,
    private val cardDatabaseManager: CardDatabaseManager,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    private val timeUtils: TimeUtils
) {

    var isLoading by mutableStateOf(true)
    var loadError by mutableStateOf(false)

    // Kanji catalog
    val cards = mutableStateListOf<KaiteyoCard>()
    val frequencies = mutableStateMapOf<String, Int>()
    val strokeCounts = mutableStateMapOf<String, Int>()
    val classifications = mutableStateMapOf<String, MutableList<String>>()
    val radicalsInCharacter = mutableStateMapOf<String, List<String>>()

    // User state
    val flags = mutableStateMapOf<String, CardFlagType>()
    val favorites = mutableStateOf<Set<String>>(emptySet())
    val tags = mutableStateListOf<CardTag>()
    val cardTags = mutableStateMapOf<String, Set<Long>>()
    val srsCards = mutableStateMapOf<String, ua.syt0r.kanji.core.srs.fsrs.FsrsCard>()

    // Activity
    val activity = mutableStateListOf<KaiteyoActivity>()
    val streaks = mutableStateOf<List<StreakData>>(emptyList())
    val totalReviews = mutableStateOf(0L)
    val totalPracticeTime = mutableStateOf(Duration.ZERO)

    // Collections
    val collections = mutableStateListOf<KaiteyoCollection>()

    private var loaded = false

    suspend fun ensureLoaded() {
        if (loaded) return
        loaded = true
        load()
    }

    /** Re-attempt a failed load. No-op while a load is already in progress. */
    suspend fun retryLoad() {
        if (isLoading) return
        loaded = false
        ensureLoaded()
    }

    private suspend fun load() {
        isLoading = true
        loadError = false
        try {
            val kanjiList: List<KanjiListEntry> = appDataRepository.getAllKanji()
            val meanings: List<KanjiMeaningEntry> = appDataRepository.getAllKanjiMeanings()
            val readings: List<KanjiReadingEntry> = appDataRepository.getAllKanjiReadings()
            val classes: List<KanjiClassificationEntry> = appDataRepository.getAllClassifications()
            frequencies.clear()
            kanjiList.forEach { frequencies[it.kanji] = it.frequency ?: Int.MAX_VALUE }
            strokeCounts.clear()
            strokeCounts.putAll(appDataRepository.getKanjiStrokeCounts())

            val meaningByKanji = meanings.groupBy { it.kanji }.mapValues { (_, v) -> v.map { it.meaning } }
            val onReadingsByKanji = readings
                .filter { it.readingType == "on" }
                .groupBy { it.kanji }
                .mapValues { (_, v) -> v.map { it.reading } }

            classifications.clear()
            classes.forEach { entry ->
                classifications.getOrPut(entry.kanji) { mutableListOf() }.add(entry.classification)
            }

            val allSrs = fsrsCardRepository.getAll()
            srsCards.clear()
            allSrs.forEach { (key, card) ->
                if (key.practiceType == LETTER_WRITING_PRACTICE_TYPE) {
                    srsCards[key.itemKey] = card
                }
            }

            // Flags + favorites
            flags.clear()
            favorites.value = emptySet()
            CardFlagType.entries.filter { it != CardFlagType.None }.forEach { flagType ->
                cardDatabaseManager.getCardsByFlag(flagType.id).forEach {
                    if (it.practiceType == LETTER_WRITING_PRACTICE_TYPE) {
                        flags[it.cardKey] = flagType
                    }
                }
            }
            favorites.value = cardDatabaseManager.getCardsByFlag(FAVORITE_FLAG_TYPE)
                .filter { it.practiceType == LETTER_WRITING_PRACTICE_TYPE }
                .map { it.cardKey }
                .toSet()

            // Tags
            tags.clear()
            cardTags.clear()
            cardDatabaseManager.getAllTags().forEach { tagData ->
                tags.add(
                    CardTag(
                        id = tagData.id,
                        name = tagData.name,
                        color = tagData.color,
                        parentId = tagData.parentId,
                        createdAt = tagData.createdAt,
                        modifiedAt = tagData.modifiedAt
                    )
                )
            }
            tags.forEach { tag ->
                cardDatabaseManager.getCardsByTag(tag.id).forEach { ref ->
                    if (ref.practiceType == LETTER_WRITING_PRACTICE_TYPE) {
                        val current = cardTags[ref.cardKey] ?: emptySet()
                        cardTags[ref.cardKey] = current + tag.id
                    }
                }
            }

            // Activity
            activity.clear()
            cardDatabaseManager.getRecentHistory(100).forEach { row ->
                activity.add(KaiteyoActivity.fromHistoryRow(row))
            }
            val recentReviews = loadRecentReviews()
            activity.addAll(0, recentReviews)

            // Streaks / totals
            streaks.value = reviewHistoryRepository.getStreaks(timeUtils.getCurrentTime().time)
            totalReviews.value = reviewHistoryRepository.getTotalReviewsCount()
            totalPracticeTime.value = reviewHistoryRepository.getTotalPracticeTime(60_000L)

            buildCatalog(kanjiList, meaningByKanji, onReadingsByKanji)
            rebuildCollections()
        } catch (t: Throwable) {
            loadError = true
        } finally {
            isLoading = false
        }
    }

    private suspend fun loadRecentReviews(): List<KaiteyoActivity> {
        return runCatching {
            val now = Clock.System.now()
            val start = now - 30.days
            reviewHistoryRepository.getReviews(start, now)
                .sortedByDescending { it.timestamp }
                .take(60)
                .map { it.toActivity() }
        }.getOrDefault(emptyList())
    }

    private fun buildCatalog(
        kanjiList: List<KanjiListEntry>,
        meaningByKanji: Map<String, List<String>>,
        onReadingsByKanji: Map<String, List<String>>
    ) {
        cards.clear()
        kanjiList.forEach { entry ->
            val character = entry.kanji
            val srsCard = srsCards[character]
            cards.add(
                KaiteyoCard(
                    id = character,
                    character = character,
                    meaning = meaningByKanji[character]?.firstOrNull() ?: "",
                    reading = onReadingsByKanji[character]?.take(3)?.joinToString("・") ?: "",
                    deck = KANJI_BROWSER_DECK_NAME,
                    deckId = 0L,
                    tags = mutableListOf(),
                    tagNames = mutableListOf(),
                    flag = flags[character] ?: CardFlagType.None,
                    notes = "",
                    status = srsCard.toCardStatus(),
                    difficulty = if (isDifficult(character)) CardDifficulty.Hard else CardDifficulty.Good,
                    priority = 0,
                    isSuspended = false,
                    isBuried = false,
                    isArchived = false,
                    isFavorite = favorites.value.contains(character),
                    customFields = mutableMapOf(),
                    aliases = mutableListOf(),
                    relatedCards = mutableListOf(),
                    createdAt = "",
                    modifiedAt = "",
                    lastReviewed = srsCard?.lastReview?.toString() ?: "",
                    reviewCount = srsCard?.repeats ?: 0,
                    interval = srsCard?.interval?.inWholeDays?.toInt() ?: 0,
                    ease = 2.5f,
                    lapses = srsCard?.lapses ?: 0,
                    accuracy = 0.85f,
                    totalTimeStudied = 0L
                )
            )
        }
        cards.sortBy { it.id }
    }

    suspend fun rebuildCollections() {
        val now = Clock.System.now()
        val result = mutableListOf<KaiteyoCollection>()

        fun addSmart(title: String, icon: String, predicate: (KaiteyoCard) -> Boolean) {
            val ids = cards.filter(predicate).map { it.id }.toSet()
            if (ids.isNotEmpty()) result.add(KaiteyoCollection.smart(title, icon, ids))
        }

        addSmart("Recently learned", "🕐") { card ->
            card.lastReviewed.isNotBlank() &&
                runCatching { Instant.parse(card.lastReviewed) > now - 24.hours }.getOrDefault(false)
        }
        addSmart("Needs review", "🔔") { card ->
            val srs = srsCards[card.id] ?: return@addSmart false
            val reviewTime = srs.lastReview ?: return@addSmart false
            reviewTime + srs.interval <= now
        }
        addSmart("Frequently failed", "⚠️") { card ->
            (srsCards[card.id]?.lapses ?: 0) >= 3
        }
        addSmart("Not studied in 30 days", "🌙") { card ->
            val srs = srsCards[card.id]
            if (srs == null) true
            else (srs.lastReview ?: Instant.fromEpochMilliseconds(0)) < now - 30.days
        }
        addSmart("Flagged", "🚩") { card -> card.flag != CardFlagType.None }
        addSmart("Favorites", "★") { card -> card.isFavorite }

        val userCollections = cardDatabaseManager.runCatching { getFilteredDecks() }
            .getOrDefault(emptyList())
            .map { deck ->
                KaiteyoCollection(
                    id = deck.id.toString(),
                    name = deck.name,
                    icon = "📁",
                    isSmart = false,
                    criteria = deck.searchQuery,
                    cardIds = emptySet(),
                    createdAt = deck.createdAt
                )
            }

        collections.clear()
        result.forEach { collections.add(it) }
        userCollections.forEach { collections.add(it) }
    }

    fun cardFlagsFor(cardId: String): CardFlagType = flags[cardId] ?: CardFlagType.None

    fun isFavorite(cardId: String): Boolean = cardId in favorites.value

    fun isLearned(cardId: String): Boolean = srsCards[cardId] != null

    fun isDifficult(cardId: String): Boolean {
        val card = srsCards[cardId] ?: return false
        val params = card.params
        return params is ua.syt0r.kanji.core.srs.fsrs.FsrsCardParams.Existing &&
            params.difficulty >= 0.55
    }

    fun srsStatus(cardId: String): CardStatus {
        val card = srsCards[cardId] ?: return CardStatus.New
        return when (card.status) {
            ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus.New -> CardStatus.New
            ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus.Learning -> CardStatus.Learning
            ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus.Review -> CardStatus.Mature
            ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus.Relearning -> CardStatus.Relearning
        }
    }

    fun cardById(cardId: String): KaiteyoCard? = cards.firstOrNull { it.id == cardId }

    suspend fun loadRadicals(): List<RadicalData> = appDataRepository.getRadicals()

    suspend fun loadCharactersWithRadicals(radicals: Set<String>): Set<String> {
        val result = appDataRepository.getCharactersWithRadicals(radicals.toList())
        radicalsInCharacter.clear()
        result.forEach { character ->
            appDataRepository.getRadicalsInCharacter(character)
                .map { it.radical }
                .distinct()
                .let { radicalsInCharacter[character] = it }
        }
        return result.toSet()
    }

    fun notReviewedFor(cardId: String, days: Int): Boolean {
        val srs = srsCards[cardId] ?: return true
        val last = srs.lastReview ?: return true
        return last < Clock.System.now() - days.days
    }

    fun meaningsFor(cardId: String): List<String> {
        val card = cardById(cardId) ?: return emptyList()
        return card.meaning.split(",").filter { it.isNotBlank() }.map { it.trim() }
    }

    suspend fun setFlag(cardIds: List<String>, flagType: CardFlagType) {
        cardDatabaseManager.bulkSetFlag(
            cardIds.map { Triple(it, LETTER_WRITING_PRACTICE_TYPE, flagType.id) }
        )
        if (flagType == CardFlagType.None) {
            cardIds.forEach { flags.remove(it) }
        } else {
            cardIds.forEach { flags[it] = flagType }
        }
        updateCards(cardIds) { it.copy(flag = flagType) }
        rebuildCollections()
    }

    suspend fun toggleFavorite(cardId: String) {
        val current = favorites.value
        val updated = if (cardId in current) {
            cardDatabaseManager.removeFlag(cardId, LETTER_WRITING_PRACTICE_TYPE)
            current - cardId
        } else {
            cardDatabaseManager.setFlag(cardId, LETTER_WRITING_PRACTICE_TYPE, FAVORITE_FLAG_TYPE)
            current + cardId
        }
        favorites.value = updated
        updateCards(listOf(cardId)) { it.copy(isFavorite = cardId in updated) }
        rebuildCollections()
    }

    private fun updateCards(cardIds: List<String>, transform: (KaiteyoCard) -> KaiteyoCard) {
        val ids = cardIds.toSet()
        cards.forEachIndexed { index, card ->
            if (card.id in ids) {
                cards[index] = transform(card)
            }
        }
    }

    suspend fun addTagToCards(cardIds: List<String>, tagId: Long) {
        cardIds.forEach { cardDatabaseManager.addTagToCard(it, LETTER_WRITING_PRACTICE_TYPE, tagId) }
        cardIds.forEach { id ->
            val current = cardTags[id] ?: emptySet()
            cardTags[id] = current + tagId
        }
        cards.forEach { card ->
            if (card.id in cardIds && card.tags.none { it.id == tagId }) {
                tags.firstOrNull { it.id == tagId }?.let { tag ->
                    card.tags.add(tag)
                    card.tagNames.add(tag.name)
                }
            }
        }
        rebuildCollections()
    }

    suspend fun removeTagFromCards(cardIds: List<String>, tagId: Long) {
        cardIds.forEach { cardDatabaseManager.removeTagFromCard(it, LETTER_WRITING_PRACTICE_TYPE, tagId) }
        cardIds.forEach { id ->
            val current = cardTags[id] ?: emptySet()
            cardTags[id] = current - tagId
        }
        cards.forEach { card ->
            if (card.id in cardIds) {
                card.tags.removeAll { it.id == tagId }
                card.tagNames.removeAll { tag -> card.tags.none { it.name == tag } }
            }
        }
        rebuildCollections()
    }

    suspend fun createTag(name: String, color: String, parentId: Long? = null): Long =
        cardDatabaseManager.createTag(name, color, parentId).also { _ ->
            cardDatabaseManager.getAllTags().forEach { tagData ->
                if (tags.none { it.id == tagData.id }) {
                    tags.add(
                        CardTag(
                            id = tagData.id,
                            name = tagData.name,
                            color = tagData.color,
                            parentId = tagData.parentId,
                            createdAt = tagData.createdAt,
                            modifiedAt = tagData.modifiedAt
                        )
                    )
                }
            }
        }

    suspend fun updateTag(tagId: Long, name: String, color: String, parentId: Long?) {
        cardDatabaseManager.updateTag(tagId, name, color, parentId)
        val index = tags.indexOfFirst { it.id == tagId }
        if (index != -1) {
            val tag = tags[index]
            tags[index] = tag.copy(name = name, color = color, parentId = parentId)
        }
    }

    suspend fun deleteTag(tagId: Long) {
        cardDatabaseManager.deleteTag(tagId)
        tags.removeAll { it.id == tagId }
        cardTags.forEach { (id, tagIds) ->
            cardTags[id] = tagIds - tagId
        }
        rebuildCollections()
    }

    suspend fun mergeTags(sourceId: Long, targetId: Long) {
        cardDatabaseManager.mergeTags(sourceId, targetId)
        val source = tags.firstOrNull { it.id == sourceId }
        tags.removeAll { it.id == sourceId }
        cardTags.forEach { (id, tagIds) ->
            if (tagIds.contains(sourceId)) {
                cardTags[id] = (tagIds - sourceId) + targetId
            }
        }
        cards.forEach { card ->
            if (source != null) {
                card.tags.removeAll { it.id == sourceId }
                tags.firstOrNull { it.id == targetId }?.let { target ->
                    if (card.tagNames.contains(source.name)) {
                        card.tags.add(target)
                        card.tagNames.add(target.name)
                    }
                }
            }
        }
        rebuildCollections()
    }

    suspend fun resetProgress(cardIds: List<String>) {
        cardIds.forEach { id ->
            runCatching {
                cardDatabaseManager.removeFlag(id, LETTER_WRITING_PRACTICE_TYPE)
            }
        }
        flags.forEach { (id, _) ->
            if (id in cardIds) flags.remove(id)
        }
        rebuildCollections()
    }

    /**
     * Reloads SRS state for the given cards after an external
     * FsrsCardRepository update (reset / forget) so the in-memory
     * catalog stays consistent with the database.
     */
    suspend fun refreshAfterReset(cardIds: List<String>) {
        val allSrs = fsrsCardRepository.getAll()
        cardIds.forEach { cardId ->
            val refreshed = allSrs[SrsCardKey(cardId, LETTER_WRITING_PRACTICE_TYPE)]
            if (refreshed != null) {
                srsCards[cardId] = refreshed
            } else {
                srsCards.remove(cardId)
            }
        }
        val indexByCard = cards.mapIndexed { index, card -> card.id to index }.toMap()
        cardIds.forEach { cardId ->
            val index = indexByCard[cardId] ?: return@forEach
            val current = cards[index]
            val srsCard = srsCards[cardId]
            cards[index] = current.copy(
                status = srsCard.toCardStatus(),
                lastReviewed = srsCard?.lastReview?.toString() ?: "",
                reviewCount = srsCard?.repeats ?: 0,
                interval = srsCard?.interval?.inWholeDays?.toInt() ?: 0,
                lapses = srsCard?.lapses ?: 0,
                isSuspended = false,
                isBuried = false
            )
        }
        rebuildCollections()
    }
}

private fun ua.syt0r.kanji.core.srs.fsrs.FsrsCard?.toCardStatus(): CardStatus {
    if (this == null) return CardStatus.New
    return when (status) {
        ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus.New -> CardStatus.New
        ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus.Learning -> CardStatus.Learning
        ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus.Review -> CardStatus.Mature
        ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus.Relearning -> CardStatus.Relearning
    }
}

private fun ReviewHistoryItem.toActivity(): KaiteyoActivity {
    val failed = grade <= 1
    return KaiteyoActivity(
        timestamp = timestamp,
        type = if (failed) KaiteyoActivityType.ReviewFailed else KaiteyoActivityType.Review,
        title = if (failed) "Failed review: $key" else "Reviewed: $key",
        cardKey = key,
        details = "Grade ${grade} · ${duration.inWholeSeconds}s"
    )
}

// ============================================
// Models
// ============================================

enum class KaiteyoActivityType {
    Review,
    ReviewFailed,
    Edit,
    Import,
    Export,
    Tag,
    Flag,
    Note,
    Study,
    System
}

data class KaiteyoActivity(
    val timestamp: Instant,
    val type: KaiteyoActivityType,
    val title: String,
    val cardKey: String? = null,
    val details: String = ""
) {
    companion object {
        fun fromHistoryRow(row: StudyHistoryRow): KaiteyoActivity {
            val type = when (row.actionType) {
                0 -> KaiteyoActivityType.Review
                1 -> KaiteyoActivityType.Study
                2 -> KaiteyoActivityType.Edit
                3 -> KaiteyoActivityType.Import
                4 -> KaiteyoActivityType.Export
                5 -> KaiteyoActivityType.Tag
                6 -> KaiteyoActivityType.Flag
                7 -> KaiteyoActivityType.Note
                else -> KaiteyoActivityType.System
            }
            return KaiteyoActivity(
                timestamp = row.timestamp,
                type = type,
                title = row.details.ifBlank { type.name },
                cardKey = row.cardKey,
                details = row.details
            )
        }
    }
}

data class KaiteyoCollection(
    val id: String,
    val name: String,
    val icon: String,
    val isSmart: Boolean,
    val criteria: String,
    val cardIds: Set<String>,
    val createdAt: Instant = Clock.System.now()
) {
    companion object {
        fun smart(name: String, icon: String, cardIds: Set<String>): KaiteyoCollection =
            KaiteyoCollection(
                id = "smart-$name",
                name = name,
                icon = icon,
                isSmart = true,
                criteria = "",
                cardIds = cardIds
            )
    }
}
