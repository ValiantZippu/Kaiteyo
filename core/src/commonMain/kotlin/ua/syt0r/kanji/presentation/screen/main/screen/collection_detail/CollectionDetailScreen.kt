package ua.syt0r.kanji.presentation.screen.main.screen.collection_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.knowledge.KanjiKnowledge
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionCard
import ua.syt0r.kanji.presentation.common.ui.KaiteyoTag
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.collection_detail.CollectionDetailContract.ScreenState

// ============================================================
// COLLECTION DETAIL — SCREEN (spec §29–§30)
// ------------------------------------------------------------
// A library collection's page: header (title, description, real
// count) + the lazy-loaded kanji entries it contains. Tapping a
// kanji opens its entry — COLLECTION → KANJI → WORDS → SENTENCES.
// Paging keeps large collections (e.g. 1,000+ grade sets) cheap.
// ============================================================

@Composable
fun CollectionDetailScreen(
    collectionId: String,
    onClose: () -> Unit,
    onOpenKanji: (String) -> Unit,
    onOpenDeck: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = getMultiplatformViewModel<CollectionDetailContract.ViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(collectionId) { viewModel.load(collectionId) }

    // Lesson → SRS study path: when the "Study as deck" action published a
    // deck id, navigate to it exactly once (the id is cleared after use).
    LaunchedEffect(state) {
        val deckId = (state as? ScreenState.Loaded)?.studyDeckId ?: return@LaunchedEffect
        viewModel.clearStudyDeckId()
        onOpenDeck(deckId)
    }

    ProvidePageIdentity(
        PageIdentity(id = "collection_detail", name = "Collection", route = "/collections/$collectionId", panel = null)
    ) {
        Column(modifier.fillMaxSize()) {
            Header(onClose = onClose)
            when (val current = state) {
                is ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ScreenState.Error -> KaiteyoEmptyState(
                    icon = "📚",
                    title = "Collection unavailable",
                    message = current.message,
                    actionLabel = "Retry",
                    onAction = viewModel::retry
                )
                is ScreenState.Loaded -> LoadedContent(
                    state = current,
                    onOpenKanji = onOpenKanji,
                    onLoadMore = viewModel::loadMore,
                    onStudyDeck = viewModel::studyDeck
                )
            }
        }
    }
}

@Composable
private fun Header(onClose: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = surfaceColors.textPrimary)
        }
        Text("Collection", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        Text("Library material", style = MaterialTheme.typography.labelMedium, color = surfaceColors.textMuted)
    }
}

@Composable
private fun LoadedContent(
    state: ScreenState.Loaded,
    onOpenKanji: (String) -> Unit,
    onLoadMore: () -> Unit,
    onStudyDeck: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusXl))
                    .background(surfaceColors.surface)
                    .padding(Dimens.Space5)
            ) {
                Text(state.collection.title, style = MaterialTheme.typography.titleLarge, color = surfaceColors.textPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(Dimens.Space2))
                Text(state.collection.description, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
                Spacer(Modifier.height(Dimens.Space3))
                KaiteyoTag(text = "${state.totalKanji} kanji", tint = accent.primary)
                if (state.totalKanji > 0) {
                    Spacer(Modifier.height(Dimens.Space3))
                    // Lesson → deck study path (spec §29): turns this
                    // collection into a real SRS letter deck, reusing an
                    // existing deck with the same lesson title when present.
                    Text(
                        text = "Study as deck",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.RadiusSm))
                            .clickable(onClick = onStudyDeck)
                            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space1)
                    )
                }
            }
        }

        if (state.kanji.isEmpty()) {
            item {
                KaiteyoEmptyState(icon = "🔍", title = "No kanji", message = "This collection has no kanji entries.")
            }
        } else {
            item {
                Text(
                    text = "Showing ${state.kanji.size} of ${state.totalKanji}",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted
                )
            }
            items(state.kanji.size, key = { state.kanji[it].character }) { index ->
                KanjiRow(kanji = state.kanji[index], onClick = { onOpenKanji(state.kanji[index].character) })
            }
            if (state.hasMore) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = Dimens.Space3), contentAlignment = Alignment.Center) {
                        if (state.loadingMore) {
                            CircularProgressIndicator(Modifier.size(26.dp))
                        } else {
                            Text(
                                text = "Load more",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                                    .clickable(onClick = onLoadMore)
                                    .padding(horizontal = Dimens.Space4, vertical = Dimens.Space2)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KanjiRow(
    kanji: KanjiKnowledge,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(surfaceColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        Text(kanji.character, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        Column(Modifier.weight(1f)) {
            kanji.keyword?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary)
            }
            val readings = (kanji.onReadings + kanji.kunReadings).distinct()
            if (readings.isNotEmpty()) {
                Text(
                    text = readings.take(3).joinToString("・"),
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted
                )
            }
        }
        kanji.jlpt?.let { KaiteyoTag(text = it.label, tint = LocalKaiteyoAccent.current.primary) }
    }
}
