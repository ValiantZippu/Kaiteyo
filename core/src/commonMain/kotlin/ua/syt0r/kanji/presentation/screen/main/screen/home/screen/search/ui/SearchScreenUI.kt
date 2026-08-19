package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.presentation.common.CollapsibleContainer
import ua.syt0r.kanji.presentation.common.CollapsibleContainerState
import ua.syt0r.kanji.presentation.common.JapaneseWordUI
import ua.syt0r.kanji.presentation.common.isNearListEnd
import ua.syt0r.kanji.presentation.common.rememberCollapsibleContainerState
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.trackItemPosition
import ua.syt0r.kanji.presentation.common.ui.kanji.HighlightedLetter
import ua.syt0r.kanji.presentation.dialog.SaveWordDialog
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.SearchScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.SearchScreenContract.KaiteyoHomeState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.SearchScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.data.RadicalSearchState

/** Which part of the dictionary results to show — filters the same live
 *  search pipeline instead of running a separate query per tab. */
private enum class SearchContentFilter(val label: String) {
    All("All"),
    Kanji("Kanji"),
    Vocabulary("Vocabulary")
}

@Composable
fun SearchScreenUI(
    state: State<ScreenState>,
    radicalsState: State<RadicalSearchState>,
    kaiteyoHomeState: State<KaiteyoHomeState>,
    onSubmitInput: (String) -> Unit,
    onRadicalsSectionExpanded: () -> Unit,
    onRadicalsSelected: (Set<String>) -> Unit,
    onCharacterClick: (String) -> Unit,
    onWordClick: (JapaneseWord) -> Unit,
    onScrolledToEnd: () -> Unit,
    onWordFeedback: (JapaneseWord) -> Unit,
    onLoadKaiteyoHome: () -> Unit
) {

    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    var contentFilter by remember { mutableStateOf(SearchContentFilter.All) }

    val inputState = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val selectedRadicalsState = rememberSaveable() {
        mutableStateOf(emptySet<String>())
    }

    LaunchedEffect(Unit) {
        snapshotFlow { inputState.value }
            .onEach { onSubmitInput(it.text) }
            .launchIn(this)
        snapshotFlow { selectedRadicalsState.value }
            .onEach { onRadicalsSelected(it) }
            .launchIn(this)
        onLoadKaiteyoHome()
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            Surface {
                RadicalSearch(
                    state = radicalsState,
                    selectedRadicals = selectedRadicalsState,
                    onCharacterClick = {
                        inputState.value = inputState.value.run {
                            TextFieldValue(
                                text = text + it,
                                selection = TextRange(text.length + 1)
                            )
                        }
                    }
                )
            }
        }
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // ---- Header: title + subtitle (Library-style) ---------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = resolveString { nav.browseLabel },
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "Kanji, vocabulary and expressions — one search",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            val searchContainerState = rememberCollapsibleContainerState()

            CollapsibleContainer(searchContainerState) {
                InputSection(
                    inputState = inputState,
                    onOpenRadicalSearch = {
                        coroutineScope.launch {
                            sheetState.show()
                            onRadicalsSectionExpanded()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                val isProgressVisible = remember { derivedStateOf { state.value.isLoading } }
                androidx.compose.animation.AnimatedVisibility(
                    visible = isProgressVisible.value,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        trackColor = MaterialTheme.colorScheme.background
                    )
                }
            }

            // Content-type filter chips — Browse tabs over one shared pipeline.
            val hasResults = remember(state.value) {
                derivedStateOf {
                    state.value.characters.isNotEmpty() || state.value.words.value.items.isNotEmpty()
                }
            }
            val isQueryEmpty = inputState.value.text.isEmpty()
            if (hasResults.value) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchContentFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = contentFilter == filter,
                            onClick = { contentFilter = filter },
                            label = { Text(filter.label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            if (isQueryEmpty) {
                // Kaiteyo-style dictionary home while nothing is typed.
                SearchKaiteyoHome(
                    state = kaiteyoHomeState.value,
                    onRefresh = onLoadKaiteyoHome,
                    onCharacterClick = onCharacterClick,
                    onWordClick = onWordClick
                )
            } else {
                ListContent(
                    screenState = state.value,
                    searchContainerState = searchContainerState,
                    contentFilter = contentFilter,
                    onCharacterClick = onCharacterClick,
                    onWordClick = onWordClick,
                    onScrolledToEnd = onScrolledToEnd
                )
            }

        }

    }

}

@Composable
private fun InputSection(
    inputState: MutableState<TextFieldValue>,
    onOpenRadicalSearch: () -> Unit,
    modifier: Modifier
) {

    var enteredText by inputState
    val interactionSource = remember { MutableInteractionSource() }
    val isInputFocused = remember { mutableStateOf(false) }

    val isHintVisible = remember {
        derivedStateOf { !isInputFocused.value && enteredText.text.isEmpty() }
    }
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isInputFocused.value) {
                    Modifier.border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(6.dp))
        IconButton(
            onClick = onOpenRadicalSearch,
            modifier = Modifier.size(34.dp)
        ) {
            Text(text = "部", color = accent)
        }
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = enteredText,
                onValueChange = { enteredText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isInputFocused.value = it.isFocused },
                maxLines = 1,
                singleLine = true,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(accent),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color)
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isHintVisible.value,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = resolveString { search.inputHint },
                    style = MaterialTheme.typography.titleMedium,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
        IconButton(
            onClick = { enteredText = TextFieldValue() },
            modifier = Modifier.size(34.dp)
        ) {
            Icon(Icons.Default.Close, null, tint = color)
        }

    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListContent(
    screenState: ScreenState,
    searchContainerState: CollapsibleContainerState,
    contentFilter: SearchContentFilter,
    onCharacterClick: (String) -> Unit,
    onWordClick: (JapaneseWord) -> Unit,
    onScrolledToEnd: () -> Unit
) {

    val listState = rememberLazyListState()

    val canLoadMoreWords = remember(screenState) {
        derivedStateOf { screenState.words.value.canLoadMore }
    }

    if (canLoadMoreWords.value) {
        LaunchedEffect(Unit) {
            snapshotFlow { listState.layoutInfo }
                .map { it.isNearListEnd(SearchScreenContract.LoadMoreWordsFromEndThreshold) }
                .filter { it }
                .collect { onScrolledToEnd() }
        }
    }

    val shouldShowScrollUpButton = remember {
        derivedStateOf { listState.firstVisibleItemIndex > 10 }
    }

    var wordToAddToVocabDeck by remember { mutableStateOf<JapaneseWord?>(null) }
    wordToAddToVocabDeck?.let {
        SaveWordDialog(
            word = it,
            onDismissRequest = { wordToAddToVocabDeck = null }
        )
    }

    Box {

        val contentBottomPadding = remember { mutableStateOf(0.dp) }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(searchContainerState.nestedScrollConnection)
        ) {

            if (contentFilter != SearchContentFilter.Vocabulary) {
                item {
                    SearchHeader(
                        text = resolveString { search.charactersTitle(screenState.characters.size) }
                    )
                }

                if (screenState.characters.isNotEmpty()) item {

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(modifier = Modifier.width(20.dp)) }
                        items(screenState.characters) {
                            HighlightedLetter(
                                letter = it,
                                onClick = onCharacterClick,
                                aspectRatioConstraintOrientation = Orientation.Vertical
                            )
                        }
                        item { Spacer(modifier = Modifier.width(20.dp)) }
                    }

                }
            }

            val currentWordsState = screenState.words.value

            if (contentFilter != SearchContentFilter.Kanji) {
                stickyHeader {
                    SearchHeader(
                        text = resolveString { search.wordsTitle(currentWordsState.totalCount) }
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }

                itemsIndexed(currentWordsState.items) { index, word ->
                    JapaneseWordUI(
                        index = index,
                        word = word,
                        onClick = { onWordClick(word) },
                        onFuriganaClick = onCharacterClick,
                        addWordToVocabDeckClick = { wordToAddToVocabDeck = word }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(contentBottomPadding.value + 16.dp)) }

        }

        AnimatedVisibility(
            visible = shouldShowScrollUpButton.value,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .trackItemPosition { contentBottomPadding.value = it.heightFromScreenBottom }
        ) {
            val coroutineScope = rememberCoroutineScope()
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch { listState.scrollToItem(0) }
                    coroutineScope.launch { searchContainerState.expand() }
                }
            ) {
                Icon(Icons.Default.KeyboardArrowUp, null)
            }
        }

    }

}

@Composable
private fun SearchHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .wrapContentSize(Alignment.CenterStart)
    )
}
