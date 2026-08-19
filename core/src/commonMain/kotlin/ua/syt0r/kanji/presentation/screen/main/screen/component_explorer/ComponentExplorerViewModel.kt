package ua.syt0r.kanji.presentation.screen.main.screen.component_explorer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.knowledge.ComponentKnowledge
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.presentation.screen.main.screen.component_explorer.ComponentExplorerContract.ScreenState

class ComponentExplorerViewModel(
    private val viewModelScope: CoroutineScope,
    private val knowledge: KnowledgeRepository
) : ComponentExplorerContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val state: StateFlow<ScreenState> = _state

    private var loaded = false

    override fun load() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            try {
                // Every radical-derived component, with real kanji counts.
                val stats = knowledge.radicalStats()
                val components = stats.map { stats ->
                    ComponentExplorerContract.ComponentSummary(
                        component = ComponentKnowledge(
                            component = stats.radical,
                            radicalOf = stats.radical,
                            strokesCount = stats.strokeCount
                        ),
                        kanjiCount = stats.kanjiCount
                    )
                }.sortedByDescending { it.kanjiCount }

                _state.value = ScreenState.Loaded(components = components)
            } catch (e: Exception) {
                _state.value = ScreenState.Error("Failed to load the component catalog: ${e.message}")
            }
        }
    }

    override fun selectComponent(component: String) {
        val current = _state.value as? ScreenState.Loaded ?: return
        if (current.selected == component) return
        _state.value = current.copy(selected = component, kanji = emptyList(), kanjiTotal = 0, loadingKanji = true)
        viewModelScope.launch {
            val characters = knowledge.kanjiWithRadicals(listOf(component))
            val kanji = knowledge.kanjiBatch(characters.take(60))
            val latest = _state.value as? ScreenState.Loaded ?: return@launch
            _state.value = latest.copy(
                kanji = kanji,
                kanjiTotal = characters.size,
                loadingKanji = false
            )
        }
    }

    override fun clearSelection() {
        val current = _state.value as? ScreenState.Loaded ?: return
        _state.value = current.copy(selected = null, kanji = emptyList(), kanjiTotal = 0, loadingKanji = false)
    }
}
