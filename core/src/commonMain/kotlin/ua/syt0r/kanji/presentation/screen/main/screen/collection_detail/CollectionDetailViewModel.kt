package ua.syt0r.kanji.presentation.screen.main.screen.collection_detail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.core.knowledge.LibraryCatalog
import ua.syt0r.kanji.core.user_data.database.LetterPracticeRepository
import ua.syt0r.kanji.presentation.screen.main.screen.collection_detail.CollectionDetailContract.ScreenState

class CollectionDetailViewModel(
    private val viewModelScope: CoroutineScope,
    private val knowledge: KnowledgeRepository,
    private val catalog: LibraryCatalog,
    private val letterPracticeRepository: LetterPracticeRepository
) : CollectionDetailContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val state: StateFlow<ScreenState> = _state

    private var loadedId: String? = null
    private var page = 0
    private var allKanjiChars: List<String> = emptyList()

    companion object {
        private const val PAGE_SIZE = 50
    }

    override fun load(collectionId: String) {
        if (loadedId == collectionId) return
        loadedId = collectionId
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            page = 0
            val collection = catalog.collection(collectionId)
            if (collection == null) {
                _state.value = ScreenState.Error("Collection not found.")
                return@launch
            }
            allKanjiChars = catalog.kanjiIn(collectionId)
            val firstPage = loadPage()
            _state.value = ScreenState.Loaded(
                collection = collection,
                kanji = firstPage,
                totalKanji = allKanjiChars.size,
                hasMore = allKanjiChars.size > firstPage.size,
                loadingMore = false
            )
        }
    }

    override fun loadMore() {
        val current = _state.value as? ScreenState.Loaded ?: return
        if (!current.hasMore || current.loadingMore) return
        _state.value = current.copy(loadingMore = true)
        viewModelScope.launch {
            val next = loadPage()
            val latest = _state.value as? ScreenState.Loaded ?: return@launch
            val merged = latest.kanji + next
            _state.value = latest.copy(
                kanji = merged,
                hasMore = merged.size < allKanjiChars.size,
                loadingMore = false
            )
        }
    }

    override fun retry() {
        val id = loadedId ?: return
        // Reset so load() actually re-runs (it dedupes on the same id).
        loadedId = null
        load(id)
    }

    override fun studyDeck() {
        val current = _state.value as? ScreenState.Loaded ?: return
        if (current.studyDeckId != null || allKanjiChars.isEmpty()) return
        viewModelScope.launch {
            // Find-or-create: re-studying a lesson must never duplicate the
            // deck. The title is stable so a second tap reuses the same deck.
            val title = "Lesson: ${current.collection.title}"
            val deckId = letterPracticeRepository.getDecks()
                .firstOrNull { it.name == title }?.id
                ?: run {
                    letterPracticeRepository.createDeck(title, allKanjiChars)
                    letterPracticeRepository.getDecks()
                        .firstOrNull { it.name == title }?.id
                }
            if (deckId != null) {
                _state.value = current.copy(studyDeckId = deckId)
            }
        }
    }

    override fun clearStudyDeckId() {
        val current = _state.value as? ScreenState.Loaded ?: return
        if (current.studyDeckId != null) {
            _state.value = current.copy(studyDeckId = null)
        }
    }

    private suspend fun loadPage(): List<ua.syt0r.kanji.core.knowledge.KanjiKnowledge> {
        val window = allKanjiChars.drop(page * PAGE_SIZE).take(PAGE_SIZE)
        page++
        return knowledge.kanjiBatch(window)
    }
}
