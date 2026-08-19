package ua.syt0r.kanji.presentation.screen.main.screen.info

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.core.japanese.isKanji
import ua.syt0r.kanji.core.japanese.kanaToRomaji
import ua.syt0r.kanji.core.tts.KanaTtsManager
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract.ScreenState

class InfoScreenViewModel(
    private val viewModelScope: CoroutineScope,
    screenData: InfoScreenData,
    private val loadLetterStateUseCase: InfoScreenContract.LoadLetterStateUseCase,
    private val loadVocabStateUseCase: InfoScreenContract.LoadVocabStateUseCase,
    private val analyticsManager: AnalyticsManager,
    private val kanaTtsManager: KanaTtsManager? = null
) : InfoScreenContract.ViewModel {

    override val state = mutableStateOf<ScreenState>(ScreenState.Loading)

    private val _playingReading = mutableStateOf<String?>(null)
    override val playingReading: State<String?> = _playingReading

    init {
        viewModelScope.launch {
            state.value = withContext(Dispatchers.IO) { screenData.toState() }
        }
    }

    override fun speakReading(reading: String) {
        if (kanaTtsManager == null) return
        viewModelScope.launch {
            try {
                _playingReading.value = reading
                val romaji = reading.kanaToRomaji()
                kanaTtsManager.speak(KanaReading(nihonShiki = romaji))
                delay(600)
            } catch (_: Exception) {
                // TTS failed gracefully without crash
            } finally {
                _playingReading.value = null
            }
        }
    }

    private suspend fun InfoScreenData.toState(): ScreenState {
        return when (this) {
            is InfoScreenData.Letter -> {
                if (letter.length == 1) {
                    reportLetter(letter)
                    loadLetterStateUseCase(letter, viewModelScope)
                } else {
                    loadVocabStateUseCase(asVocabData(), viewModelScope)
                }
            }

            is InfoScreenData.Vocab -> {
                loadVocabStateUseCase(this, viewModelScope)
            }
        }
    }

    private fun InfoScreenData.Letter.asVocabData(): InfoScreenData.Vocab {
        val kanjiReading: String?
        val kanaReading: String?

        when {
            letter.any { it.isKanji() } -> {
                kanjiReading = letter
                kanaReading = null
            }

            else -> {
                kanjiReading = null
                kanaReading = letter
            }
        }

        return InfoScreenData.Vocab(
            id = null,
            kanjiReading = kanjiReading,
            kanaReading = kanaReading
        )
    }

    private fun reportLetter(letter: String) {
        analyticsManager.sendEvent("kanji_info_open") { put("character", letter) }
    }

}
