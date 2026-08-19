package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.common.ExtraSpacer
import ua.syt0r.kanji.presentation.common.PaginationLoadLaunchedEffect
import ua.syt0r.kanji.presentation.common.collectAsState
import ua.syt0r.kanji.presentation.common.trackList
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoKanjiPills
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoSenseList
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoVocabHero
import ua.syt0r.kanji.presentation.dialog.SaveWordDialog
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.info.VocabInfoData
import ua.syt0r.kanji.presentation.screen.main.screen.info.use_case.ItemLearningState
import ua.syt0r.kanji.presentation.screen.main.screen.info.infoScreenExpandableSentenceSection

@Composable
fun VocabInfoUI(
    vocabData: VocabInfoData,
    listState: LazyListState,
    listSpacerState: ExtraListSpacerState,
    learningState: ItemLearningState?,
    learningActions: List<LearningAction>,
    onLetterClick: (String) -> Unit,
    onWordClick: ((JapaneseWord) -> Unit)? = null
) {

    val sentencesExpanded = rememberSaveable { mutableStateOf(true) }
    val sentences = vocabData.sentences.collectAsState()
    var showAddToDeckDialog by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current

    if (showAddToDeckDialog) {
        SaveWordDialog(
            word = vocabData.word,
            onDismissRequest = { showAddToDeckDialog = false }
        )
    }

    PaginationLoadLaunchedEffect(
        listState = listState,
        prefetchDistance = InfoScreenContract.ListPrefetchDistance,
        loadMore = { vocabData.sentences.loadMore() }
    )

    val word = vocabData.word
    val reading = word.reading
    val letters = remember(word) {
        (reading.kanjiReading ?: reading.kanaReading)
            .map { it.toString() }.distinct()
    }

    val hero: @Composable () -> Unit = {
        KaiteyoVocabHero(
            word = word,
            typeBadge = "単語",
            onAddToDeck = { showAddToDeckDialog = true },
            onOpenJisho = {
                val searchTerm = reading.kanjiReading ?: reading.kanaReading
                uriHandler.openUri(InfoScreenContract.getJishoSearchUrl(searchTerm))
            },
            onWordClick = onWordClick
        )
    }

    val senseCard: @Composable () -> Unit = {
        KaiteyoSenseList(senses = vocabData.senseList)
    }

    val kanjiPills: @Composable () -> Unit = {
        KaiteyoKanjiPills(
            letters = letters,
            onLetterClick = onLetterClick
        )
    }

    val sentencesSection: LazyListScope.() -> Unit = {
        infoScreenExpandableSentenceSection(
            expanded = sentencesExpanded,
            paginateable = sentences,
            onFuriganaClick = onLetterClick
        )
    }

    when (LocalOrientation.current) {
        Orientation.Portrait -> {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .trackList(listSpacerState)
            ) {

                item(key = "kaiteyo-hero") { hero() }

                if (learningState != null) {
                    item(key = "learning-status") {
                        LearningStatusSection(
                            state = learningState,
                            actions = learningActions
                        )
                    }
                }

                item(key = "kaiteyo-senses") { senseCard() }

                item(key = "kaiteyo-letters") { kanjiPills() }

                sentencesSection()

                listSpacerState.ExtraSpacer(this)

            }
        }

        Orientation.Landscape -> {

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .trackList(listSpacerState)
            ) {

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    item(key = "kaiteyo-hero") { hero() }

                    item(key = "kaiteyo-senses") { senseCard() }

                    item(key = "kaiteyo-letters") { kanjiPills() }

                    listSpacerState.ExtraSpacer(this)
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {

                    if (learningState != null) {
                        item(key = "learning-status") {
                            LearningStatusSection(
                                state = learningState,
                                actions = learningActions
                            )
                        }
                    }

                    sentencesSection()

                    listSpacerState.ExtraSpacer(this)

                }

            }
        }
    }

}
