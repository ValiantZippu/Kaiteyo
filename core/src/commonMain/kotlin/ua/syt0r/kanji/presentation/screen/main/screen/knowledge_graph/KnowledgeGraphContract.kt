package ua.syt0r.kanji.presentation.screen.main.screen.knowledge_graph

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.knowledge.GraphTrail
import ua.syt0r.kanji.core.knowledge.KnowledgeEdgeType
import ua.syt0r.kanji.core.knowledge.KnowledgeGraph
import ua.syt0r.kanji.core.knowledge.KnowledgeNode

// ============================================================
// KNOWLEDGE GRAPH — CONTRACT
// ------------------------------------------------------------
// Standalone graph explorer. A pan/zoom canvas over the
// progressively-expanding graph rooted at a kanji (or any node
// id). Selecting a node shows an inspector; expanding pulls the
// next ring of real relationships.
// ============================================================

interface KnowledgeGraphContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        fun load(root: String)
        fun selectNode(nodeId: String)
        fun expandNode(nodeId: String)
        fun setTypeFilter(types: Set<KnowledgeEdgeType>?)
        fun reset()
        fun retry()
        /** Move back/forward through the visited trail (KT-GRAPH-004). */
        fun goBack()
        fun goForward()
    }

    sealed interface ScreenState {

        data object Loading : ScreenState

        data class Loaded(
            val root: String,
            val graph: KnowledgeGraph,
            val selectedId: String?,
            val typeFilter: Set<KnowledgeEdgeType>?,
            val loading: Boolean,
            val exhaustedIds: Set<String>,
            /** Navigation trail: breadcrumbs + back/forward (KT-GRAPH-004). */
            val trail: GraphTrail = GraphTrail()
        ) : ScreenState {
            val selectedNode: KnowledgeNode?
                get() = selectedId?.let { graph.node(it) }
        }

        data class Error(val message: String) : ScreenState
    }
}
