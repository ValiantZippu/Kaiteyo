package ua.syt0r.kanji.presentation.screen.main.screen.knowledge_graph

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.knowledge.GraphTrail
import ua.syt0r.kanji.core.knowledge.KnowledgeEdgeType
import ua.syt0r.kanji.core.knowledge.KnowledgeGraph
import ua.syt0r.kanji.core.knowledge.KnowledgeGraphRepository
import ua.syt0r.kanji.presentation.screen.main.screen.knowledge_graph.KnowledgeGraphContract.ScreenState

class KnowledgeGraphViewModel(
    private val viewModelScope: CoroutineScope,
    private val repository: KnowledgeGraphRepository
) : KnowledgeGraphContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val state: StateFlow<ScreenState> = _state

    private var loadedRoot: String? = null
    private var graph: KnowledgeGraph? = null
    private var typeFilter: Set<KnowledgeEdgeType>? = null
    private var trail: GraphTrail = GraphTrail.Empty

    override fun load(root: String) {
        if (loadedRoot == root) return
        loadedRoot = root
        graph = null
        typeFilter = null
        trail = GraphTrail(entries = listOf(root), position = 0)
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            val opened = repository.initialGraph(root)
            if (opened.isEmpty) {
                _state.value = ScreenState.Error(
                    "Nothing in the knowledge graph is connected to \"$root\"."
                )
                return@launch
            }
            graph = opened
            _state.value = ScreenState.Loaded(
                root = root,
                graph = opened,
                selectedId = opened.rootId,
                typeFilter = null,
                loading = false,
                exhaustedIds = emptySet(),
                trail = trail
            )
        }
    }

    override fun selectNode(nodeId: String) {
        val current = _state.value as? ScreenState.Loaded ?: return
        _state.value = current.copy(selectedId = nodeId)
    }

    override fun expandNode(nodeId: String) {
        val currentGraph = graph ?: return
        val current = _state.value as? ScreenState.Loaded ?: return
        if (current.loading) return
        _state.value = current.copy(loading = true)
        viewModelScope.launch {
            val result = repository.expand(currentGraph, nodeId, typeFilter)
            graph = result.graph
            // Expanding is a real navigation step — record it in the trail.
            trail = trail.push(nodeId)
            _state.value = ScreenState.Loaded(
                root = current.root,
                graph = result.graph,
                selectedId = nodeId,
                typeFilter = typeFilter,
                loading = false,
                exhaustedIds = result.exhausted.toSet(),
                trail = trail
            )
        }
    }

    override fun goBack() {
        val current = _state.value as? ScreenState.Loaded ?: return
        val (newTrail, nodeId) = trail.back()
        if (nodeId == null) return
        trail = newTrail
        _state.value = current.copy(
            selectedId = nodeId,
            trail = trail
        )
    }

    override fun goForward() {
        val current = _state.value as? ScreenState.Loaded ?: return
        val (newTrail, nodeId) = trail.forward()
        if (nodeId == null) return
        trail = newTrail
        _state.value = current.copy(
            selectedId = nodeId,
            trail = trail
        )
    }

    override fun setTypeFilter(types: Set<KnowledgeEdgeType>?) {
        typeFilter = types
        val current = _state.value as? ScreenState.Loaded ?: return
        _state.value = current.copy(typeFilter = types)
    }

    override fun reset() {
        val root = loadedRoot ?: return
        loadedRoot = null
        load(root)
    }

    override fun retry() {
        loadedRoot?.let { load(it) }
    }
}
