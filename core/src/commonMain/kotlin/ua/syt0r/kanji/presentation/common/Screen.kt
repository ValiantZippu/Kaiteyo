package ua.syt0r.kanji.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.presentation.common.ui.rememberAdaptiveContentMaxWidth

@Composable
fun ScrollableScreenContainer(
    contentModifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    contentPaddings: PaddingValues = PaddingValues(bottom = 12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {

    // Phone keeps the classic 400dp column; tablets/desktop grow the content
    // (capped at 720dp for form-style screens) and center it instead of
    // rendering a tiny column in a huge window.
    val maxWidth = rememberAdaptiveContentMaxWidth(
        phoneMax = 400.dp,
        mediumMax = 560.dp,
        wideMax = 720.dp
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth()
                .widthIn(max = maxWidth)
                .then(contentModifier)
                .padding(contentPaddings),
            verticalArrangement = verticalArrangement
        ) {
            content()
        }
    }

}