package ua.syt0r.kanji.presentation.screen.main.screen.sentence_entry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.knowledge.LearnerProfileStore
import ua.syt0r.kanji.core.knowledge.SentenceAnalyzer
import ua.syt0r.kanji.core.knowledge.level.LevelAdapter
import ua.syt0r.kanji.presentation.screen.main.screen.sentence_entry.SentenceEntryContract.ScreenState

class SentenceEntryViewModel(
    private val viewModelScope: CoroutineScope,
    private val analyzer: SentenceAnalyzer,
    private val profileStore: LearnerProfileStore
) : SentenceEntryContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val state: StateFlow<ScreenState> = _state

    private var loadedSentence: String? = null
    private var loadedTranslation: String = ""

    override fun load(sentence: String, translation: String) {
        if (loadedSentence == sentence) return
        loadedSentence = sentence
        loadedTranslation = translation
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            if (sentence.isBlank()) {
                _state.value = ScreenState.Error("Empty sentence.")
                return@launch
            }
            val analysis = analyzer.analyze(sentence)
            // Profile adaptation (KT-LEVEL-003, spec §23–§24): whether the
            // translation is shown is a presentation decision driven by the
            // learner profile — the data itself is never deleted.
            val preference = profileStore.load()
            val presentation = LevelAdapter.effectivePresentation(
                profile = preference.profile,
                overrides = preference.customPresentation
            )
            _state.value = ScreenState.Loaded(
                sentence = sentence,
                translation = translation,
                showTranslation = LevelAdapter.showTranslations(presentation),
                showFurigana = LevelAdapter.showFurigana(presentation),
                showRomaji = LevelAdapter.showRomaji(presentation),
                analysis = analysis
            )
        }
    }

    override fun retry() {
        val sentence = loadedSentence ?: return
        // Reset so load() actually re-runs (it dedupes on the same sentence).
        loadedSentence = null
        load(sentence, loadedTranslation)
    }
}
