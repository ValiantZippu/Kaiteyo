package ua.syt0r.kanji.presentation.screen.main.screen.text_analysis.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.presentation.common.AppCheckBox
import ua.syt0r.kanji.presentation.common.AppDropdownMenu
import ua.syt0r.kanji.presentation.common.AppDropdownMenuItem
import ua.syt0r.kanji.presentation.common.AppListItem
import ua.syt0r.kanji.presentation.common.PaginationLoadLaunchedEffect
import ua.syt0r.kanji.presentation.common.collectAsState
import ua.syt0r.kanji.presentation.common.copyCentered
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.extraColorScheme
import ua.syt0r.kanji.presentation.common.ui.FancyLoading
import ua.syt0r.kanji.presentation.common.ui.FuriganaText
import ua.syt0r.kanji.presentation.common.ui.rememberAdaptiveContentMaxWidth
import ua.syt0r.kanji.presentation.common.ui.kanji.HighlightedLetter
import ua.syt0r.kanji.presentation.dialog.SaveLettersDialog
import ua.syt0r.kanji.presentation.screen.main.screen.text_analysis.TextAnalysisContentMode
import ua.syt0r.kanji.presentation.screen.main.screen.text_analysis.TextAnalysisContentState
import ua.syt0r.kanji.presentation.screen.main.screen.text_analysis.TextAnalysisContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.text_analysis.TextAnalysisNode
import ua.syt0r.kanji.presentation.screen.main.screen.text_analysis.TextAnalysisResult
import ua.syt0r.kanji.text_analysis_action_furigana
import ua.syt0r.kanji.text_analysis_action_highlight
import ua.syt0r.kanji.text_analysis_action_save_letters
import ua.syt0r.kanji.text_analysis_alternative_words
import ua.syt0r.kanji.text_analysis_history_empty
import ua.syt0r.kanji.text_analysis_history_title
import ua.syt0r.kanji.text_analysis_save_letters_apply
import ua.syt0r.kanji.text_analysis_save_letters_cancel
import ua.syt0r.kanji.text_analysis_save_letters_counter
import ua.syt0r.kanji.text_analysis_save_letters_select_kanji
import ua.syt0r.kanji.text_analysis_title
import ua.syt0r.kanji.text_analysis_translation_placeholder
import ua.syt0r.kanji.text_analysis_word_parse_error


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextAnalysisScreenUI(
    screenState: State<ScreenState>,
    saveWord: (TextAnalysisNode.CardData) -> Unit,
    navigateBack: () -> Unit,
    navigateToAccount: () -> Unit,
) {

    var showHistory = rememberSaveable { mutableStateOf(false) }

    ScreenLayout(
        state = screenState,
        showHistory = showHistory,
        navigateBack = navigateBack,
        analysisContent = { screenState ->

            Column(
                modifier = Modifier.padding(horizontal = Dimens.ContentPadding)
            ) {

                AnalysisResultSection(
                    state = screenState.contentState.collectAsState(),
                    saveWord = saveWord,
                    modifier = Modifier.weight(1f)
                )

                HorizontalDivider()

                TextAnalysisInputUI(
                    state = screenState.inputState.collectAsState(),
                    navigateToAccount = navigateToAccount
                )

            }

        },
        historyContent = { screenState ->
            val historyPaginateable = screenState.history.collectAsState()

            if (historyPaginateable.value.total == 0) {
                Text(
                    text = stringResource(Res.string.text_analysis_history_empty),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize()
                )
                return@ScreenLayout
            }

            val historyState = historyPaginateable.value.collectAsState()
            val listState = rememberLazyListState()
            PaginationLoadLaunchedEffect(listState) { historyPaginateable.value.loadMore() }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
            ) {

                items(historyState.list) {
                    AppListItem(
                        onClick = {
                            screenState.setContent(it)
                            showHistory.value = false
                        },
                        headlineContent = {
                            Text(
                                text = it.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }

                item { Spacer(Modifier.height(Dimens.ContentPadding)) }

            }
        }
    )

}

@get:Composable
private val translationTextStyle
    get() = MaterialTheme.typography.titleLarge.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

@Composable
private fun AnalysisResultSection(
    state: State<TextAnalysisContentState>,
    saveWord: (TextAnalysisNode.CardData) -> Unit,
    modifier: Modifier,
) {

    when (val currentContentState = state.value) {
        TextAnalysisContentState.Empty ->
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Dimens.SpacingSmall)
            ) {
                Spacer(Modifier.weight(1f))
                HorizontalDivider()
                Text(
                    text = stringResource(Res.string.text_analysis_translation_placeholder),
                    style = translationTextStyle,
                    modifier = Modifier.padding(vertical = Dimens.SpacingSmall)
                )
                Spacer(Modifier.weight(1f))
            }

        is TextAnalysisContentState.Loaded -> {

            when (val result = currentContentState.result) {

                is TextAnalysisResult.Success -> {

                    Column(
                        modifier = modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = Dimens.SpacingSmall)
                    ) {

                        TextAnalysisHeader(currentContentState)

                        AnalysisContent(
                            displayMode = currentContentState.contentMode.value,
                            nodeList = result.nodeList,
                            translation = result.translation,
                            saveWord = saveWord
                        )

                    }

                }

                is TextAnalysisResult.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMid),
                        modifier = modifier
                            .fillMaxWidth()
                            .wrapContentSize()
                            .widthIn(max = rememberAdaptiveContentMaxWidth(
                                phoneMax = Dimens.ScreenWidth,
                                mediumMax = 480.dp,
                                wideMax = 560.dp
                            ))
                            .verticalScroll(rememberScrollState())
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.alignBy { it.measuredHeight * 4 / 5 }
                        )
                        SelectionContainer(
                            modifier = Modifier.alignByBaseline()
                        ) {
                            Text(text = result.message)
                        }
                    }

                }
            }
        }

    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextAnalysisHeader(contentState: TextAnalysisContentState.Loaded) {

    Crossfade(
        targetState = contentState.contentMode.value,
        modifier = Modifier.fillMaxWidth()
    ) { currentMode ->

        when (currentMode) {
            is TextAnalysisContentMode.Browse -> {

                var showPopup by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { showPopup = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.End)
                        .offset(Dimens.SpacingBig + Dimens.SpacingTiny + 1.dp)
                ) {

                    var furigana by currentMode.furigana
                    var highlight by currentMode.highlight

                    Icon(Icons.Outlined.MoreHoriz, null)

                    AppDropdownMenu(
                        expanded = showPopup,
                        onDismissRequest = { showPopup = false },
                        modifier = Modifier.width(IntrinsicSize.Max)
                    ) {
                        AppDropdownMenuItem(
                            onClick = { furigana = !furigana }
                        ) {
                            Icon(Icons.Outlined.Translate, null)
                            Text(
                                text = stringResource(Res.string.text_analysis_action_furigana),
                                modifier = Modifier.weight(1f)
                            )
                            AppCheckBox(checked = furigana)
                        }
                        AppDropdownMenuItem(
                            onClick = { highlight = !highlight }
                        ) {
                            Icon(Icons.Outlined.BorderColor, null)
                            Text(
                                text = stringResource(Res.string.text_analysis_action_highlight),
                                modifier = Modifier.weight(1f)
                            )
                            AppCheckBox(checked = highlight)
                        }
                        AppDropdownMenuItem(currentMode.switchToSaveLettersMode) {
                            Icon(Icons.Outlined.Bookmarks, null)
                            Text(stringResource(Res.string.text_analysis_action_save_letters))
                        }
                    }

                }

            }

            is TextAnalysisContentMode.SaveLetters -> {

                var lettersToSave by remember { mutableStateOf<List<String>?>(null) }
                lettersToSave?.let {
                    SaveLettersDialog(
                        letters = it,
                        onSaved = { currentMode.switchToBrowseMode },
                        onDismissRequest = { lettersToSave = null }
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    val count = currentMode.selected.value.size

                    Text(
                        text = stringResource(
                            Res.string.text_analysis_save_letters_counter,
                            count.toString()
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(end = Dimens.SpacingMid),
                        style = MaterialTheme.typography.labelMedium.copyCentered()
                    )

                    IconButton(
                        onClick = currentMode.selectNone,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    ) { Icon(Icons.Outlined.Deselect, null) }

                    IconButton(
                        onClick = currentMode.selectAll,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) { Icon(Icons.Outlined.SelectAll, null) }

                    TextButton(
                        onClick = currentMode.selectAllKanji,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(stringResource(Res.string.text_analysis_save_letters_select_kanji))
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentWidth(Alignment.End)
                            .width(IntrinsicSize.Max)
                            .height(IntrinsicSize.Max)
                    ) {
                        TextButton(
                            onClick = currentMode.switchToBrowseMode,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text(stringResource(Res.string.text_analysis_save_letters_cancel))
                        }

                        TextButton(
                            onClick = { lettersToSave = currentMode.selected.value.toList() },
                            modifier = Modifier.align(Alignment.CenterVertically),
                            enabled = count > 0
                        ) {
                            Text(stringResource(Res.string.text_analysis_save_letters_apply))
                        }
                    }

                }

            }
        }

    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.AnalysisContent(
    displayMode: TextAnalysisContentMode,
    nodeList: List<TextAnalysisNode>,
    translation: String,
    saveWord: (TextAnalysisNode.CardData) -> Unit
) {

    when (displayMode) {
        is TextAnalysisContentMode.Browse -> {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.SpacingMid)
            ) {
                nodeList.forEach {
                    AnalysisNode(
                        node = it,
                        displayMode = displayMode,
                        saveWord = saveWord
                    )
                }
            }
            if (displayMode.alternativeWords.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.SpacingMid)
                ) {
                    Text(
                        text = stringResource(Res.string.text_analysis_alternative_words),
                        modifier = Modifier.alignByBaseline()
                    )
                    displayMode.alternativeWords.forEach {
                        AnalysisNode(
                            node = it,
                            displayMode = displayMode,
                            saveWord = saveWord
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider()

            SelectionContainer(
                modifier = Modifier.padding(vertical = Dimens.SpacingMid)
            ) {
                Text(
                    text = translation,
                    style = translationTextStyle
                )
            }

        }

        is TextAnalysisContentMode.SaveLetters -> {

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.SpacingMid),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
            ) {

                displayMode.letters.forEach {
                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .alignByBaseline(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                    ) {

                        HighlightedLetter(
                            letter = it,
                            onClick = { displayMode.toggleSelection(it) },
                            containerColor = MaterialTheme.colorScheme.surface
                        )

                        val highlightColor = when {
                            displayMode.selected.value.contains(it) -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceDim
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimens.SpacingTiny)
                                .background(highlightColor, CircleShape)
                        )

                    }

                }
            }
        }

    }

    Spacer(modifier = Modifier.weight(1f))

}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowScope.AnalysisNode(
    node: TextAnalysisNode,
    displayMode: TextAnalysisContentMode.Browse,
    saveWord: (TextAnalysisNode.CardData) -> Unit
) {
    when (node) {
        is TextAnalysisNode.Word -> WordNode(node, displayMode, saveWord)

        is TextAnalysisNode.Text -> {
            Text(
                text = node.value,
                modifier = Modifier.alignByBaseline()
            )
        }

        is TextAnalysisNode.Compound -> {
            node.childNodeList.forEach { AnalysisNode(it, displayMode, saveWord) }
        }

        is TextAnalysisNode.Error -> Column(
            modifier = Modifier
                .alignByBaseline()
                .width(IntrinsicSize.Max)
        ) {

            Text(text = node.text ?: stringResource(Res.string.text_analysis_word_parse_error))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.SpacingTiny)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
            )

        }

        is TextAnalysisNode.AlternativeGroup -> {
            val displayNode = node.childNodeList.firstOrNull()
            displayNode?.let { AnalysisNode(it, displayMode, saveWord) }
        }
    }
}

@Composable
fun TextAnalysisNode.PartOfSpeech.toHighlightColor(
    noHighlightColor: Color = Color.Unspecified
): Color {
    return when (this) {
        TextAnalysisNode.PartOfSpeech.Noun -> MaterialTheme.extraColorScheme.new
        TextAnalysisNode.PartOfSpeech.Verb -> MaterialTheme.extraColorScheme.success
        TextAnalysisNode.PartOfSpeech.Adjective -> MaterialTheme.extraColorScheme.due
        else -> noHighlightColor
    }
}

@Composable
private fun RowScope.WordNode(
    node: TextAnalysisNode.Word,
    displayMode: TextAnalysisContentMode.Browse,
    saveWord: (TextAnalysisNode.CardData) -> Unit
) {

    var showPopup = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable { showPopup.value = true }
            .padding(Dimens.SpacingMid, Dimens.SpacingSmall)
            .width(IntrinsicSize.Max)
            .alignByBaseline()
    ) {

        Box(Modifier.align(Alignment.BottomStart)) {
            TextAnalysisVocabCardPopup(
                showPopup = showPopup,
                node = node,
                saveWord = saveWord
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {

            when {
                node.reading.furigana != null && displayMode.furigana.value -> {
                    FuriganaText(node.reading.furigana)
                }

                else -> Text(node.text)
            }

            val highlightColor: Color? = when {
                !displayMode.highlight.value -> null
                else -> node.highlightPartOfSpeech?.toHighlightColor(MaterialTheme.colorScheme.surface)
            }

            if (highlightColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.SpacingTiny)
                        .background(highlightColor, CircleShape)
                )
            }
        }

    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenLayout(
    state: State<ScreenState>,
    showHistory: MutableState<Boolean>,
    navigateBack: () -> Unit,
    analysisContent: @Composable (ScreenState.Loaded) -> Unit,
    historyContent: @Composable (ScreenState.Loaded) -> Unit,
) {

    BoxWithConstraints {

        val splitView = maxWidth > 800.dp

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = navigateBack
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    title = {
                        val title = when {
                            showHistory.value && !splitView -> {
                                stringResource(Res.string.text_analysis_history_title)
                            }

                            else -> {
                                stringResource(Res.string.text_analysis_title)
                            }
                        }
                        Text(text = title)
                    },
                    actions = {
                        if (!splitView) {
                            IconButton(
                                onClick = { showHistory.value = !showHistory.value }
                            ) {
                                val switchActionIcon: ImageVector = when {
                                    showHistory.value -> Icons.AutoMirrored.Outlined.ManageSearch
                                    else -> Icons.Outlined.History
                                }
                                Icon(switchActionIcon, null)
                            }
                        }

                        var showConfigurationDialog by remember { mutableStateOf(false) }
                        if (showConfigurationDialog) {
                            TextAnalysisConfigurationDialog(
                                onDismissRequest = { showConfigurationDialog = false }
                            )
                        }

                        IconButton(
                            onClick = { showConfigurationDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                            )
                        }

                    }
                )
            }
        ) { paddingValues ->

            Crossfade(
                targetState = state.value to showHistory.value,
                modifier = Modifier.padding(paddingValues)
            ) { (screenState, showHistory) ->
                when (screenState) {
                    ScreenState.Loading -> {
                        FancyLoading(Modifier.fillMaxSize().wrapContentSize())
                    }

                    is ScreenState.Loaded -> {
                        when {
                            splitView -> {
                                Row {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMid)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.text_analysis_history_title),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        historyContent(screenState)
                                    }
                                    VerticalDivider()
                                    Box(Modifier.weight(2f)) { analysisContent(screenState) }
                                }
                            }

                            showHistory -> historyContent(screenState)
                            else -> analysisContent(screenState)
                        }
                    }
                }
            }

        }

    }

}
