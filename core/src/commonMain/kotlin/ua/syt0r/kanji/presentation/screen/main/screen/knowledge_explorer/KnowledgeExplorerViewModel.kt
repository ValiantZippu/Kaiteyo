package ua.syt0r.kanji.presentation.screen.main.screen.knowledge_explorer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.knowledge.KnowledgeEdgeType
import ua.syt0r.kanji.core.knowledge.KnowledgeGraph
import ua.syt0r.kanji.core.knowledge.KnowledgeGraphRepository
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.core.knowledge.KnowledgeSearchEngine
import ua.syt0r.kanji.core.knowledge.KnowledgeSearchQuery
import ua.syt0r.kanji.core.knowledge.LearnerProfileStore
import ua.syt0r.kanji.core.knowledge.SearchFilters
import ua.syt0r.kanji.core.knowledge.SearchSort
import ua.syt0r.kanji.core.knowledge.level.LevelAdapter
import ua.syt0r.kanji.presentation.screen.main.screen.knowledge_explorer.KnowledgeExplorerContract.ScreenState

/** A search request: query text + composable filters + sort. */
private data class SearchRequest(
    val text: String,
    val filters: SearchFilters,
    val sort: SearchSort
)

@OptIn(FlowPreview::class)
class KnowledgeExplorerViewModel(
    private val viewModelScope: CoroutineScope,
    private val knowledge: KnowledgeRepository,
    private val searchEngine: KnowledgeSearchEngine,
    private val graphRepository: KnowledgeGraphRepository,
    private val profileStore: LearnerProfileStore,
    initialQuery: String = ""
) : KnowledgeExplorerContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Initial)
    override val state: StateFlow<ScreenState> = _state

    private val text = MutableStateFlow(initialQuery)
    private val filters = MutableStateFlow(SearchFilters.None)
    private val sort = MutableStateFlow(SearchSort.Relevance)

    init {
        viewModelScope.launch {
            combine(text, filters, sort) { t, f, s -> SearchRequest(t, f, s) }
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { request -> runSearch(request.text, request.filters, request.sort) }
        }
    }

    /** Runs a search immediately (filter/sort chips and retry skip the debounce). */
    private suspend fun runSearch(text: String, filters: SearchFilters, sort: SearchSort) {
        // A stale debounced query must never overwrite a detail or graph the
        // user already navigated to.
        val before = _state.value
        if (before !is ScreenState.Initial &&
            before !is ScreenState.Searching &&
            before !is ScreenState.Results
        ) {
            return
        }
        // Blank text is only meaningful with active filters (filter-only browse).
        if (text.isBlank() && filters == SearchFilters.None) {
            if (before !is ScreenState.Initial) _state.value = ScreenState.Initial
            return
        }
        _state.value = ScreenState.Searching(text, filters, sort)
        val results = searchEngine.search(
            KnowledgeSearchQuery(text = text, filters = filters, sort = sort)
        )
        // Apply only if the user is still waiting on this search.
        if (_state.value is ScreenState.Searching) {
            _state.value = ScreenState.Results(text, results, filters, sort)
        }
    }

    private fun reSearchImmediately() {
        viewModelScope.launch { runSearch(text.value, filters.value, sort.value) }
    }

    override fun onQueryChange(text: String) {
        this.text.value = text
        // Instant feedback: a truly blank query with no filters returns to the
        // landing state; blank with filters keeps showing filter-only results.
        if (text.isBlank() && filters.value == SearchFilters.None) {
            _state.value = ScreenState.Initial
        }
    }

    override fun clearSearch() {
        text.value = ""
        filters.value = SearchFilters.None
        sort.value = SearchSort.Relevance
        _state.value = ScreenState.Initial
    }

    override fun setFilter(filters: SearchFilters) {
        this.filters.value = filters
        reSearchImmediately()
    }

    override fun setSort(sort: SearchSort) {
        this.sort.value = sort
        reSearchImmediately()
    }

    override fun clearFilters() {
        filters.value = SearchFilters.None
        reSearchImmediately()
    }

    override fun browse(filters: SearchFilters) {
        this.filters.value = filters
        text.value = ""
        reSearchImmediately()
    }

    override fun retry() {
        reSearchImmediately()
    }

    override fun openKanji(character: String) {
        viewModelScope.launch {
            _state.value = ScreenState.KanjiDetail(
                character = character,
                kanji = null,
                radicals = emptyList(),
                words = emptyList(),
                loading = true
            )
            val kanji = knowledge.kanji(character)
            val radicals = knowledge.componentsIn(character)
            val words = knowledge.wordsContaining(character, limit = 20)
            // Sentence edges from the kanji's readings (real corpus lookups).
            val sentences = buildList {
                val readings = (kanji?.onReadings.orEmpty() + kanji?.kunReadings.orEmpty())
                    .distinct()
                    .take(3)
                readings.forEach { reading ->
                    addAll(knowledge.sentencesWithText(reading, limit = 3))
                }
            }.distinctBy { it.text }
            // Browse surface adapts to the learner profile too (spec §23): the
            // same sentences a kanji entry would show, filtered by difficulty.
            val preference = profileStore.load()
            val presentation = LevelAdapter.effectivePresentation(
                profile = preference.profile,
                overrides = preference.customPresentation
            )
            val adapted = LevelAdapter.adaptedSentences(sentences, presentation, limit = 6)
            val grammar = adapted.flatMap { knowledge.grammarIn(it.text) }
                .distinctBy { it.patternId }

            _state.value = ScreenState.KanjiDetail(
                character = character,
                kanji = kanji,
                radicals = radicals,
                words = words,
                sentences = adapted,
                grammar = grammar
            )
        }
    }

    override fun openWord(wordId: Long) {
        viewModelScope.launch {
            val word = knowledge.word(wordId) ?: run {
                _state.value = ScreenState.Error("Word not found in the dictionary data.")
                return@launch
            }
            // Profile adaptation on the browse surface (spec §23–§24): the
            // learner profile controls how many senses appear, whether example
            // translations are shown, and the difficulty of the examples.
            val preference = profileStore.load()
            val presentation = LevelAdapter.effectivePresentation(
                profile = preference.profile,
                overrides = preference.customPresentation
            )
            _state.value = ScreenState.WordDetail(
                word = word,
                sentences = LevelAdapter.adaptedSentences(
                    sentences = knowledge.sentencesForWordReading(word, limit = 12),
                    presentation = presentation,
                    limit = 8
                ),
                kanji = knowledge.searchKanjiOfWord(word),
                glossary = LevelAdapter.adaptedGlossary(word, presentation),
                showTranslations = LevelAdapter.showTranslations(presentation)
            )
        }
    }

    override fun openGraph(character: String) {
        viewModelScope.launch {
            _state.value = ScreenState.Graph(graph = KnowledgeGraph(), selected = null, loading = true)
            val graph = graphRepository.initialGraph(character)
            _state.value = ScreenState.Graph(graph = graph, selected = graph.rootId)
        }
    }

    override fun expandNode(nodeId: String, types: Set<KnowledgeEdgeType>?) {
        viewModelScope.launch {
            val current = _state.value as? ScreenState.Graph ?: return@launch
            if (current.loading) return@launch
            _state.value = current.copy(loading = true)
            val expansion = graphRepository.expand(current.graph, nodeId, types)
            _state.value = ScreenState.Graph(
                graph = expansion.graph,
                selected = nodeId,
                loading = false
            )
        }
    }

    override fun focusNode(nodeId: String) {
        viewModelScope.launch {
            val current = _state.value as? ScreenState.Graph ?: return@launch
            if (current.loading) return@launch
            val graph = current.graph
            val hasNeighbors = graph.neighbors(nodeId).isNotEmpty()
            _state.value = if (hasNeighbors) {
                ScreenState.Graph(graph = graph, selected = nodeId)
            } else {
                // First touch of a node loads its one-hop expansion.
                val expansion = graphRepository.expand(graph, nodeId, null)
                ScreenState.Graph(
                    graph = expansion.graph,
                    selected = nodeId,
                    loading = false
                )
            }
        }
    }

    override fun back() {
        _state.value = ScreenState.Initial
    }
}
