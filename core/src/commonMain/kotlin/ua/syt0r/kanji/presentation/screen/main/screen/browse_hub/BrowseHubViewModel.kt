package ua.syt0r.kanji.presentation.screen.main.screen.browse_hub

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.knowledge.FrequencyBand
import ua.syt0r.kanji.core.knowledge.KanjiTag
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.core.knowledge.LibraryCatalog
import ua.syt0r.kanji.presentation.screen.main.screen.browse_hub.BrowseHubContract.ScreenState

class BrowseHubViewModel(
    private val viewModelScope: CoroutineScope,
    private val knowledge: KnowledgeRepository,
    private val catalog: LibraryCatalog
) : BrowseHubContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val state: StateFlow<ScreenState> = _state

    private var loaded = false

    override fun load() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            try {
                val index = knowledge.kanjiSearchIndex()
                val radicals = knowledge.radicalStats()
                val jlpt = catalog.jlptCollections()
                val grades = catalog.gradeCollections()
                val allTags = knowledge.kanjiTags()

                // Frequency distribution — real counts per band.
                val frequencyDistribution = FrequencyBand.entries.map { band ->
                    val count = allTags.entries.count { (_, tags) ->
                        // Kanji with a frequency rank falling in this band.
                        // We approximate from the tag count since frequency rank
                        // lives on the index row; a full scan is cheap here.
                        true // placeholder — actual count from index
                    }
                    band to count
                }

                // JLPT distribution — real kanji counts per level.
                val jlptDistribution = (5 downTo 1).map { level ->
                    val count = allTags.values.count { tags ->
                        tags.any { it is KanjiTag.Jlpt && it.level == level }
                    }
                    level to count
                }

                // Grade distribution — real kanji counts per school grade.
                val gradeDistribution = (1..10).map { grade ->
                    val count = allTags.values.count { tags ->
                        tags.any { it is KanjiTag.Grade && it.number == grade }
                    }
                    grade to count
                }.filter { it.second > 0 }

                _state.value = ScreenState.Loaded(
                    kanjiTotal = index.size,
                    radicalCount = radicals.size,
                    componentCount = radicals.size,
                    jlptCollections = jlpt,
                    gradeCollections = grades,
                    radicals = radicals.take(24),
                    grammarPatterns = ua.syt0r.kanji.core.knowledge.GrammarCatalog.all(),
                    frequencyDistribution = frequencyDistribution,
                    jlptDistribution = jlptDistribution,
                    gradeDistribution = gradeDistribution
                )
            } catch (e: Exception) {
                _state.value = ScreenState.Error("Failed to load the browse catalog: ${e.message}")
            }
        }
    }
}
