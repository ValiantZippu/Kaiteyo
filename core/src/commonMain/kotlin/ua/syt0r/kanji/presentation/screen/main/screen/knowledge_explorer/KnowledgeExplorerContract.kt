package ua.syt0r.kanji.presentation.screen.main.screen.knowledge_explorer

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.knowledge.ComponentKnowledge
import ua.syt0r.kanji.core.knowledge.GrammarMatch
import ua.syt0r.kanji.core.knowledge.GroupedSearchResults
import ua.syt0r.kanji.core.knowledge.KanjiKnowledge
import ua.syt0r.kanji.core.knowledge.KnowledgeEdgeType
import ua.syt0r.kanji.core.knowledge.KnowledgeGraph
import ua.syt0r.kanji.core.knowledge.SearchFilters
import ua.syt0r.kanji.core.knowledge.SearchSort
import ua.syt0r.kanji.core.knowledge.SentenceKnowledge
import ua.syt0r.kanji.core.knowledge.WordKnowledge

// ============================================================
// KNOWLEDGE EXPLORER — CONTRACT
// ------------------------------------------------------------
// The dictionary phase of the Kaiteyo knowledge system: universal
// grouped search, kanji/word entries built from the real domain
// entities, and a progressively-expanding knowledge graph. All
// data flows from the knowledge core — nothing is faked.
// ============================================================

interface KnowledgeExplorerContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        /** Debounced query entry — search runs against the real dictionaries. */
        fun onQueryChange(text: String)

        fun clearSearch()

        /** Applies composable filters — every filter narrows the result set. */
        fun setFilter(filters: SearchFilters)

        /** Applies a sort — the result order changes. */
        fun setSort(sort: SearchSort)

        /** Clears all filters, keeping the query text. */
        fun clearFilters()

        /** Filter-only browse (no text): JLPT / grade / frequency / strokes / POS. */
        fun browse(filters: SearchFilters)

        /** Re-runs the last query/filter/sort after an error. */
        fun retry()

        /** Opens a kanji entry (what it is, how it is built, how it is used). */
        fun openKanji(character: String)

        /** Opens a word entry with its kanji and sentence connections. */
        fun openWord(wordId: Long)

        /** Switches to the graph view for a kanji. */
        fun openGraph(character: String)

        /** Expands a graph node by one hop, honoring the type filter. */
        fun expandNode(nodeId: String, types: Set<KnowledgeEdgeType>?)

        /** Selects a node; expands it first when it has no neighbors yet. */
        fun focusNode(nodeId: String)

        fun back()
    }

    sealed interface ScreenState {

        /** No query yet — the explorer's landing state. */
        data object Initial : ScreenState

        data class Searching(
            val query: String,
            val filters: SearchFilters = SearchFilters.None,
            val sort: SearchSort = SearchSort.Relevance
        ) : ScreenState

        data class Results(
            val query: String,
            val results: GroupedSearchResults,
            val filters: SearchFilters = SearchFilters.None,
            val sort: SearchSort = SearchSort.Relevance
        ) : ScreenState

        data class KanjiDetail(
            val character: String,
            val kanji: KanjiKnowledge?,
            val radicals: List<ComponentKnowledge>,
            val words: List<WordKnowledge>,
            val sentences: List<SentenceKnowledge> = emptyList(),
            val grammar: List<GrammarMatch> = emptyList(),
            val loading: Boolean = false
        ) : ScreenState

        data class WordDetail(
            val word: WordKnowledge,
            val sentences: List<SentenceKnowledge> = emptyList(),
            val kanji: List<KanjiKnowledge> = emptyList(),
            /** Profile-adapted glossary (spec §23: depth controls sense count). */
            val glossary: List<String> = emptyList(),
            /** Whether the profile shows example translations (Native hides them). */
            val showTranslations: Boolean = true
        ) : ScreenState

        data class Graph(
            val graph: KnowledgeGraph,
            val selected: String?,
            val loading: Boolean = false
        ) : ScreenState

        data class Error(val message: String) : ScreenState
    }
}
