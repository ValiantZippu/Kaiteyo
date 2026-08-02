package ua.syt0r.kanji.desktop.engine.search

import kotlinx.datetime.Clock
import ua.syt0r.kanji.desktop.model.SavedFilter
import kotlin.random.Random

// ============================================
// SAVED FILTER STORE
// Saved searches, pinned filters, and recent
// filters with LRU trimming. In-memory + serialized
// snapshot for persistence via SettingsEngine.
// ============================================

class SavedFilterStore(
    private val maxSaved: Int = 200,
    private val maxRecent: Int = 12,
    private val recentTtlMs: Long = 30L * 24 * 60 * 60 * 1000
) {
    private val _saved = mutableListOf<SavedFilter>()
    val saved: List<SavedFilter> get() = _saved.toList()

    private val _recent = mutableListOf<SavedFilter>()
    val recent: List<SavedFilter> get() = _recent.toList()

    private var idCounter = 0L

    fun loadSaved(filters: List<SavedFilter>) {
        _saved.clear()
        _saved.addAll(filters)
        idCounter = (_saved.maxOfOrNull { it.useCount } ?: 0).toLong()
    }

    fun all(): List<SavedFilter> = buildList {
        addAll(_saved.sortedWith(compareByDescending<SavedFilter> { it.pinned }.thenByDescending { it.useCount }))
        addAll(_recent.filter { r -> _saved.none { it.id == r.id } })
    }

    fun save(name: String, query: String): SavedFilter {
        val existing = _saved.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing != null) {
            val updated = existing.copy(query = query, lastUsedAt = Clock.System.now().toEpochMilliseconds(), useCount = existing.useCount + 1)
            _saved.replaceAll { if (it.id == existing.id) updated else it }
            return updated
        }
        val filter = SavedFilter(
            id = "saved-${Random.nextLong().toString(36)}",
            name = name,
            query = query,
            pinned = false,
            lastUsedAt = Clock.System.now().toEpochMilliseconds(),
            useCount = 1
        )
        _saved.add(0, filter)
        if (_saved.size > maxSaved) _saved.removeAt(_saved.lastIndex)
        return filter
    }

    fun delete(id: String) {
        _saved.removeAll { it.id == id }
    }

    fun rename(id: String, newName: String): Boolean {
        val idx = _saved.indexOfFirst { it.id == id }
        if (idx == -1) return false
        _saved[idx] = _saved[idx].copy(name = newName)
        return true
    }

    fun togglePinned(id: String): Boolean {
        val idx = _saved.indexOfFirst { it.id == id }
        if (idx == -1) return false
        _saved[idx] = _saved[idx].copy(pinned = !_saved[idx].pinned)
        return true
    }

    /** Record a query run (even if not saved) into the recents ring. */
    fun recordRecent(query: String) {
        if (query.isBlank()) return
        val now = Clock.System.now().toEpochMilliseconds()
        _recent.removeAll { it.query == query }
        _recent.add(
            0,
            SavedFilter(
                id = "recent-${Random.nextLong().toString(36)}",
                name = query,
                query = query,
                pinned = false,
                lastUsedAt = now,
                useCount = 1
            )
        )
        // Trim expired.
        _recent.removeAll { (now - it.lastUsedAt) > recentTtlMs }
        while (_recent.size > maxRecent) _recent.removeAt(_recent.lastIndex)
    }

    fun clearRecent() {
        _recent.clear()
    }

    fun clearAll() {
        _saved.clear()
        _recent.clear()
    }
}
