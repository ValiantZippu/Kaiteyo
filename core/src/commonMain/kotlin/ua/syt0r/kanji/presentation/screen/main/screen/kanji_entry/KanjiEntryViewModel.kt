package ua.syt0r.kanji.presentation.screen.main.screen.kanji_entry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.core.knowledge.LearnerProfileStore
import ua.syt0r.kanji.core.knowledge.home.HomeCommandCenterStore
import ua.syt0r.kanji.core.knowledge.home.RecentEntry
import ua.syt0r.kanji.core.knowledge.home.RecentEntryKind
import ua.syt0r.kanji.core.knowledge.SentenceDifficultyScorer
import ua.syt0r.kanji.core.knowledge.level.LevelAdapter
import ua.syt0r.kanji.core.knowledge.cards.KanjiCardLayout
import ua.syt0r.kanji.core.knowledge.cards.KanjiCardLayoutStore
import ua.syt0r.kanji.core.knowledge.cards.KanjiCardPresets
import ua.syt0r.kanji.core.knowledge.cards.KanjiCardType
import ua.syt0r.kanji.presentation.screen.main.screen.kanji_entry.KanjiEntryContract.ScreenState

class KanjiEntryViewModel(
    private val viewModelScope: CoroutineScope,
    private val knowledge: KnowledgeRepository,
    private val layoutStore: KanjiCardLayoutStore,
    private val profileStore: LearnerProfileStore,
    private val homeStore: HomeCommandCenterStore
) : KanjiEntryContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val state: StateFlow<ScreenState> = _state

    private var loadedCharacter: String? = null
    private var layout: KanjiCardLayout = KanjiCardLayout()

    override fun load(character: String) {
        if (loadedCharacter == character) return
        loadedCharacter = character
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            layout = layoutStore.load()
            val kanji = knowledge.kanji(character)
            if (kanji == null) {
                _state.value = ScreenState.Error(
                    "${character} is not in the bundled dictionary data."
                )
                return@launch
            }
            // Record the visit so Home's "Recent entries" reflects real usage.
            homeStore.recordEntry(
                RecentEntry(
                    kind = RecentEntryKind.Kanji,
                    ref = character,
                    label = character,
                    subtitle = kanji.keyword,
                    recordedAt = Clock.System.now().toEpochMilliseconds()
                )
            )
            val radicals = knowledge.componentsIn(character)
            // Load up to the maximum per-card limit (50); the cards themselves
            // slice by the user's configured exampleLimit so raising the limit
            // in edit mode shows more items without a reload (spec §21).
            val words = knowledge.wordsContaining(character, limit = 50)
            // Related kanji: shared-radical edges (real data, real queries).
            val related = knowledge.kanjiRelatedByRadical(character, limit = 50)
                .let { characters -> knowledge.kanjiBatch(characters) }
            // Profile-aware sentence selection: sentences are filtered by the
            // learner profile's effective difficulty preference (spec §23), so a
            // beginner sees easy examples and an advanced learner sees hard ones.
            val preference = profileStore.load()
            val presentation = LevelAdapter.effectivePresentation(
                profile = preference.profile,
                overrides = preference.customPresentation
            )
            val sentences = buildList {
                val readings = (kanji.onReadings + kanji.kunReadings).distinct().take(3)
                readings.forEach { reading ->
                    addAll(knowledge.sentencesWithText(reading, limit = 20))
                }
            }
                .distinctBy { it.text }
                .filter { sentence ->
                    val level = SentenceDifficultyScorer.score(sentence.text)
                    SentenceDifficultyScorer.acceptableForDifficulty(level, presentation.sentenceDifficulty)
                }
                .take(50)
            val grammar = sentences.flatMap { knowledge.grammarIn(it.text) }
                .distinctBy { it.patternId }

            _state.value = ScreenState.Loaded(
                character = character,
                kanji = kanji,
                radicals = radicals,
                words = words,
                related = related,
                sentences = sentences,
                grammar = grammar,
                layout = layout,
                editMode = false
            )
        }
    }

    override fun toggleCard(type: KanjiCardType) {
        layout = layout.setVisible(type, !layout.isVisible(type))
        persistLayout()
        emitLayout()
    }

    override fun moveCardUp(type: KanjiCardType) {
        layout = layout.moveUp(type)
        persistLayout()
        emitLayout()
    }

    override fun moveCardDown(type: KanjiCardType) {
        layout = layout.moveDown(type)
        persistLayout()
        emitLayout()
    }

    override fun setCardLimit(type: KanjiCardType, limit: Int) {
        layout = layout.setExampleLimit(type, limit)
        persistLayout()
        emitLayout()
    }

    override fun applyPreset(presetId: String) {
        KanjiCardPresets.byId(presetId)?.let { preset ->
            layout = preset.layout
            persistLayout()
            emitLayout()
        }
    }

    override fun setEditMode(enabled: Boolean) {
        emitLayout(editMode = enabled)
    }

    override fun retry() {
        loadedCharacter?.let { load(it) }
    }

    private fun persistLayout() {
        viewModelScope.launch { layoutStore.save(layout) }
    }

    private fun emitLayout(editMode: Boolean? = null) {
        val current = _state.value as? ScreenState.Loaded ?: return
        _state.value = current.copy(layout = layout, editMode = editMode ?: current.editMode)
    }
}
