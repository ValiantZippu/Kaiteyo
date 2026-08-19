package ua.syt0r.kanji.presentation.screen.main.screen.sentence

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.app_data.data.FuriganaString
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.core.knowledge.SentenceAnalyzer
import ua.syt0r.kanji.core.knowledge.SentenceKnowledge
import ua.syt0r.kanji.presentation.screen.main.screen.sentence.SentenceScreenContract.ScreenState

class SentenceScreenViewModel(
    private val viewModelScope: CoroutineScope,
    private val knowledge: KnowledgeRepository,
    private val analyzer: SentenceAnalyzer
) : SentenceScreenContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Idle)
    override val state: StateFlow<ScreenState> = _state

    private var lastQuery: String = ""

    override fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _state.value = ScreenState.Idle
            return
        }
        lastQuery = trimmed
        _state.value = ScreenState.Explorer(trimmed, emptyList(), loading = true)
        viewModelScope.launch {
            _state.value = ScreenState.Explorer(
                query = trimmed,
                results = knowledge.sentencesWithText(trimmed, limit = 40),
                loading = false
            )
        }
    }

    override fun open(sentence: SentenceKnowledge) {
        _state.value = ScreenState.Detail(
            sentence = sentence,
            analysis = null,
            related = emptyList(),
            loading = true
        )
        viewModelScope.launch {
            val analysis = analyzer.analyze(sentence.text)
            // Related: corpus sentences sharing the sentence's first kanji —
            // a real, explainable relatedness edge (never fabricated).
            val related = sentence.text.firstOrNull { it.isKanjiChar() }
                ?.let { kanji ->
                    knowledge.sentencesWithText(kanji.toString(), limit = 8)
                        .filterNot { it.text == sentence.text }
                }
                .orEmpty()
            _state.value = ScreenState.Detail(
                sentence = sentence,
                analysis = analysis,
                related = related,
                loading = false
            )
        }
    }

    override fun openByText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            _state.value = ScreenState.Idle
            return
        }
        lastQuery = ""
        _state.value = ScreenState.Detail(
            sentence = SentenceKnowledge(
                text = trimmed,
                translation = "",
                furigana = FuriganaString(emptyList())
            ),
            analysis = null,
            related = emptyList(),
            loading = true
        )
        viewModelScope.launch {
            val exact = knowledge.sentencesWithText(trimmed, limit = 20)
                .firstOrNull { it.text == trimmed }
            if (exact != null) {
                open(exact)
            } else {
                _state.value = ScreenState.Error(
                    "\"$trimmed\" was not found in the bundled corpus."
                )
            }
        }
    }

    override fun back() {
        val current = _state.value
        if (current !is ScreenState.Detail) {
            _state.value = ScreenState.Idle
            return
        }
        // Return to the explorer with the last query (re-run it so the list
        // is fresh) or to idle when there was no query.
        if (lastQuery.isBlank()) {
            _state.value = ScreenState.Idle
        } else {
            search(lastQuery)
        }
    }

    override fun retry() {
        when (val current = _state.value) {
            is ScreenState.Detail -> open(current.sentence)
            is ScreenState.Explorer -> search(current.query)
            else -> _state.value = ScreenState.Idle
        }
    }

    private fun Char.isKanjiChar(): Boolean =
        code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF
}
