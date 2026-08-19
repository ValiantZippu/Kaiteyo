package ua.syt0r.kanji.presentation.screen.main.screen.radical_explorer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.knowledge.KanjiKnowledge
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.core.knowledge.RadicalStats
import ua.syt0r.kanji.presentation.screen.main.screen.radical_explorer.RadicalExplorerContract.ScreenState

class RadicalExplorerViewModel(
    private val viewModelScope: CoroutineScope,
    private val knowledge: KnowledgeRepository
) : RadicalExplorerContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val state: StateFlow<ScreenState> = _state

    private var radicals: List<RadicalStats> = emptyList()
    private var selectedRadicals: List<String> = emptyList()
    private var minStrokes: Int? = null
    private var jlpt: Int? = null
    private var grade: Int? = null

    override fun load() {
        if (radicals.isNotEmpty()) return
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            radicals = knowledge.radicalStats()
            if (radicals.isEmpty()) {
                _state.value = ScreenState.Error("No radical data in the bundled dataset.")
                return@launch
            }
            emitLoaded()
        }
    }

    override fun toggleRadical(radical: String) {
        selectedRadicals = if (radical in selectedRadicals) {
            selectedRadicals - radical
        } else {
            // Radix-style multi-select; cap at 4 so queries stay snappy.
            (selectedRadicals + radical).take(4)
        }
        refreshKanji()
    }

    override fun clearSelection() {
        selectedRadicals = emptyList()
        refreshKanji()
    }

    override fun setMinStrokes(strokes: Int?) {
        minStrokes = strokes
        refreshKanji()
    }

    override fun setJlpt(level: Int?) {
        jlpt = level
        refreshKanji()
    }

    override fun setGrade(grade: Int?) {
        this.grade = grade
        refreshKanji()
    }

    override fun retry() {
        radicals = emptyList()
        load()
    }

    private fun refreshKanji() {
        val current = _state.value as? ScreenState.Loaded ?: return
        if (selectedRadicals.isEmpty()) {
            _state.value = current.copy(kanji = emptyList(), kanjiTotal = 0, loadingKanji = false)
            return
        }
        _state.value = current.copy(loadingKanji = true)
        viewModelScope.launch {
            val characters = knowledge.kanjiForRadicals(selectedRadicals, limit = 80)
            val total = knowledge.kanjiForRadicalsCount(selectedRadicals)
            val kanji = applyFilters(characters)
            val currentState = _state.value as? ScreenState.Loaded ?: return@launch
            _state.value = currentState.copy(
                kanji = kanji,
                kanjiTotal = total,
                loadingKanji = false
            )
        }
    }

    /** JLPT / grade filters are applied over the fetched window (honest scope: see docs). */
    private fun applyFilters(characters: List<KanjiKnowledge>): List<KanjiKnowledge> {
        var result = characters
        val jlptFilter = jlpt
        if (jlptFilter != null) {
            result = result.filter { kanji -> kanji.jlpt?.level == jlptFilter }
        }
        val gradeFilter = grade
        if (gradeFilter != null) {
            result = result.filter { kanji ->
                kanji.classifications.any { it is ua.syt0r.kanji.core.knowledge.KanjiTag.Grade && it.number == gradeFilter }
            }
        }
        return result
    }

    private fun emitLoaded() {
        val filtered = radicals
            .filter { minStrokes == null || it.strokeCount >= minStrokes!! }
            .sortedBy { it.strokeCount }
        _state.value = ScreenState.Loaded(
            radicals = filtered,
            selectedRadicals = selectedRadicals,
            minStrokes = minStrokes,
            jlpt = jlpt,
            grade = grade,
            kanji = emptyList(),
            kanjiTotal = 0,
            loadingKanji = false
        )
    }
}
