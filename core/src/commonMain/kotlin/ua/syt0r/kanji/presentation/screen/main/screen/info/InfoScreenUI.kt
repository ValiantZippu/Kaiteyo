package ua.syt0r.kanji.presentation.screen.main.screen.info

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.app_data.Sentence
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.info_no_data_copy
import ua.syt0r.kanji.info_no_data_jisho
import ua.syt0r.kanji.info_no_data_letter
import ua.syt0r.kanji.info_no_data_vocab
import ua.syt0r.kanji.presentation.common.AppListItem
import ua.syt0r.kanji.presentation.common.ExpandButton
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.common.PaginateableState
import ua.syt0r.kanji.presentation.common.copyCentered
import ua.syt0r.kanji.presentation.common.rememberExtraListSpacerState
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.neutralButtonColors
import ua.syt0r.kanji.presentation.common.trackOverlay
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoSentenceRow
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoVocabRow
import ua.syt0r.kanji.presentation.common.ui.rememberAdaptiveContentMaxWidth
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.info.ui.LetterInfoUI
import ua.syt0r.kanji.presentation.screen.main.screen.info.ui.LearningAction
import ua.syt0r.kanji.presentation.screen.main.screen.info.ui.VocabInfoUI
import ua.syt0r.kanji.presentation.screen.main.screen.info.use_case.ItemLearningState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreenUI(
    state: State<ScreenState>,
    learningState: ItemLearningState?,
    learningActions: List<LearningAction>,
    onUpButtonClick: () -> Unit,
    onLetterClick: (String) -> Unit,
    onWordClick: (JapaneseWord) -> Unit,
    onPlayReading: ((String) -> Unit)? = null,
    isPlayingReading: String? = null,
    onSaveUserNote: ((String) -> Unit)? = null
) {
    ScreenLayout(
        state = state,
        toolbar = {
            TopAppBar(
                title = { Text(text = "") },
                navigationIcon = {
                    IconButton(onClick = onUpButtonClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        letter = { data, listState, listSpacerState ->
            LetterInfoUI(
                letterData = data,
                listState = listState,
                listSpacerState = listSpacerState,
                learningState = learningState,
                learningActions = learningActions,
                onFuriganaClick = onLetterClick,
                onWordClick = onWordClick,
                onPlayReading = onPlayReading,
                isPlayingReading = isPlayingReading,
                onSaveUserNote = onSaveUserNote
            )
        },
        vocab = { data, listState, listSpacerState ->
            VocabInfoUI(
                vocabData = data,
                listState = listState,
                listSpacerState = listSpacerState,
                learningState = learningState,
                learningActions = learningActions,
                onLetterClick = onLetterClick,
                onWordClick = onWordClick
            )
        },
        noData = {
            val searchTerm: String
            val message: String

            when (it) {
                is InfoScreenData.Letter -> {
                    searchTerm = it.letter
                    message = stringResource(Res.string.info_no_data_letter, it.letter)
                }

                is InfoScreenData.Vocab -> {
                    searchTerm = it.kanjiReading ?: it.kanaReading ?: it.id!!.toString()
                    message = stringResource(Res.string.info_no_data_vocab, searchTerm)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentWidth()
                    .widthIn(max = rememberAdaptiveContentMaxWidth(
                        phoneMax = Dimens.ScreenWidth,
                        mediumMax = 480.dp,
                        wideMax = 560.dp
                    ))
                    .padding(Dimens.ContentPadding)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                val clipboardManager = LocalClipboardManager.current
                Button(
                    onClick = { clipboardManager.setText(AnnotatedString(searchTerm)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.neutralButtonColors()
                ) {
                    Text(text = stringResource(Res.string.info_no_data_copy))
                }

                val uriHandler = LocalUriHandler.current
                Button(
                    onClick = {
                        uriHandler.openUri(InfoScreenContract.getJishoSearchUrl(searchTerm))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.neutralButtonColors()
                ) {
                    Text(text = stringResource(Res.string.info_no_data_jisho))
                }
            }
        }
    )
}

@Composable
private fun ScreenLayout(
    state: State<ScreenState>,
    toolbar: @Composable () -> Unit,
    letter: @Composable (LetterInfoData, LazyListState, ExtraListSpacerState) -> Unit,
    vocab: @Composable (VocabInfoData, LazyListState, ExtraListSpacerState) -> Unit,
    noData: @Composable (InfoScreenData) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val listSpacerState = rememberExtraListSpacerState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = toolbar,
        floatingActionButton = {
            val showUpButton = remember {
                derivedStateOf { listState.firstVisibleItemIndex > 0 }
            }

            AnimatedVisibility(
                visible = showUpButton.value,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { coroutineScope.launch { listState.scrollToItem(0) } },
                    modifier = Modifier
                        .trackOverlay(listSpacerState)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .trackOverlay(listSpacerState)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = state.value,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                contentKey = { it::class }
            ) {
                when (it) {
                    is ScreenState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is ScreenState.Loaded.Letter -> letter(it.data, listState, listSpacerState)
                    is ScreenState.Loaded.Vocab -> vocab(it.data, listState, listSpacerState)
                    is ScreenState.NoData -> noData(it.data)
                }
            }
        }
    }
}

fun LazyListScope.infoScreenExpandableVocabSection(
    expanded: MutableState<Boolean>,
    paginateable: PaginateableState<JapaneseWord>,
    onWordClick: (JapaneseWord) -> Unit,
    onFuriganaClick: (String) -> Unit,
    addWordToVocabDeckClick: (JapaneseWord) -> Unit
) {
    val items = paginateable.list

    if (items.isEmpty()) return

    item {
        AppListItem(
            headlineContent = {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vocab (${paginateable.total})", style = MaterialTheme.typography.titleSmall)
                    ExpandButton(
                        expanded = expanded.value,
                        onClick = { expanded.value = !expanded.value }
                    )
                }
            }
        )
    }

    if (expanded.value) {
        itemsIndexed(
            items = items,
            key = { index, item -> "vocab_${index}_${item.reading.kanaReading}" }
        ) { _, word ->
            KaiteyoVocabRow(
                word = word,
                onClick = { onWordClick(word) },
                onBookmarkClick = { addWordToVocabDeckClick(word) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
    }
}

fun LazyListScope.infoScreenExpandableSentenceSection(
    expanded: MutableState<Boolean>,
    paginateable: PaginateableState<Sentence>,
    onFuriganaClick: (String) -> Unit
) {
    val items = paginateable.list

    if (items.isEmpty()) return

    item {
        AppListItem(
            headlineContent = {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sentences (${paginateable.total})", style = MaterialTheme.typography.titleSmall)
                    ExpandButton(
                        expanded = expanded.value,
                        onClick = { expanded.value = !expanded.value }
                    )
                }
            }
        )
    }

    if (expanded.value) {
        itemsIndexed(
            items = items,
            key = { index, item -> "sentence_${index}_${item.value.take(10)}" }
        ) { _, item ->
            KaiteyoSentenceRow(
                sentence = item,
                onFuriganaClick = onFuriganaClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
