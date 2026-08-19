package ua.syt0r.kanji.core.knowledge.home

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================================
// HOME COMMAND CENTER — what should I do now? (spec §31)
// ------------------------------------------------------------
// Persisted "command center" data for the Home surface:
//   - RECENT SEARCHES   queries the user actually ran
//   - RECENT ENTRIES    kanji / word pages the user opened
//
// These feed Home sections (Recent searches, Recent entries)
// and are recorded by the surfaces that own the action (the
// universal search controller records queries; the kanji / word
// entry screens record their visits). Nothing here is
// fabricated — entries are recorded on real user actions and
// stored with real timestamps.
// ============================================================

/** A search query the user ran, with the time it was run. */
@Serializable
data class RecentSearch(
    val query: String,
    val recordedAt: Long
)

/** A dictionary page the user opened (kanji character or word id). */
@Serializable
data class RecentEntry(
    val kind: RecentEntryKind,
    val ref: String,
    /** Display label (the kanji character or the word spelling). */
    val label: String,
    /** Secondary label (keyword / reading). */
    val subtitle: String? = null,
    val recordedAt: Long
)

@Serializable
enum class RecentEntryKind { Kanji, Word }

/** The persisted command-center state, newest first. */
@Serializable
data class HomeCommandCenterData(
    val recentSearches: List<RecentSearch> = emptyList(),
    val recentEntries: List<RecentEntry> = emptyList()
) {
    /** Caps the stored lists (newest kept); defensive against hand-edited blobs. */
    fun sanitized(): HomeCommandCenterData = HomeCommandCenterData(
        recentSearches = recentSearches
            .filter { it.query.isNotBlank() }
            .distinctBy { it.query }
            .take(MAX_RECENT_SEARCHES),
        recentEntries = recentEntries
            .take(MAX_RECENT_ENTRIES)
    )

    companion object {
        const val MAX_RECENT_SEARCHES = 8
        const val MAX_RECENT_ENTRIES = 10
    }
}

/**
 * Persists [HomeCommandCenterData] as JSON in app preferences.
 * Mirrors KanjiCardLayoutStore: corrupt or stale blobs fall back
 * to empty state — a hand-edited preference can never break Home.
 */
class HomeCommandCenterStore(
    private val preferences: PreferencesContract.AppPreferences
) {

    suspend fun load(): HomeCommandCenterData {
        val raw = preferences.homeCommandCenterJson.get()
        if (raw.isBlank()) return HomeCommandCenterData()
        return runCatching {
            Json.decodeFromString<HomeCommandCenterData>(raw).sanitized()
        }.getOrDefault(HomeCommandCenterData())
    }

    private suspend fun save(data: HomeCommandCenterData) {
        preferences.homeCommandCenterJson.set(Json.encodeToString(data.sanitized()))
    }

    /** Records a search query (deduped, newest first). */
    suspend fun recordSearch(query: String, now: Long) {
        if (query.isBlank()) return
        val current = load()
        val entry = RecentSearch(query = query, recordedAt = now)
        save(
            current.copy(
                recentSearches = listOf(entry) + current.recentSearches.filter { it.query != query }
            )
        )
    }

    /** Records a visited kanji / word entry (newest first). */
    suspend fun recordEntry(entry: RecentEntry) {
        val current = load()
        val deduped = current.recentEntries.filter { it.kind != entry.kind || it.ref != entry.ref }
        save(current.copy(recentEntries = listOf(entry) + deduped))
    }

    suspend fun reset() {
        preferences.homeCommandCenterJson.set("")
    }
}
