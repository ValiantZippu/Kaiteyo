@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.core.app_data.data.RadicalData
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardFlagType
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard

// ============================================
// KANJI BROWSER
// Search · Radical search · JLPT · Grade ·
// Frequency · Strokes · Learned/Unlearned ·
// Difficult · Flagged · Favorites
// Grid / List / Detail
// ============================================

@Serializable
data class KanjiBrowserCriteria(
    val query: String = "",
    val jlptLevels: Set<Int> = emptySet(),
    val grades: Set<Int> = emptySet(),
    val minStrokes: Int? = null,
    val maxStrokes: Int? = null,
    val minFrequency: Int? = null,
    val maxFrequency: Int? = null,
    val showLearned: Boolean = false,
    val showUnlearned: Boolean = false,
    val showDifficult: Boolean = false,
    val showFlagged: Boolean = false,
    val flags: Set<Int> = emptySet(),
    val favoritesOnly: Boolean = false,
    val minLapses: Int? = null,
    val notReviewedDaysAgo: Int? = null,
    val radicals: Set<String> = emptySet(),
    val viewMode: KanjiBrowserViewMode = KanjiBrowserViewMode.Grid,
    val sortBy: KanjiBrowserSort = KanjiBrowserSort.Frequency
)

@Serializable
enum class KanjiBrowserViewMode { Grid, List }

@Serializable
enum class KanjiBrowserSort { Frequency, StrokeCount, JLPT, Difficulty, LastReviewed, Kanji }

@Composable
fun KanjiBrowserScreen(
    navigationState: MainNavigationState,
    dataCenter: KaiteyoDataCenter,
    initialCriteria: KanjiBrowserCriteria = KanjiBrowserCriteria()
) {
    var query by remember { mutableStateOf(initialCriteria.query) }
    var jlptLevels by remember { mutableStateOf(initialCriteria.jlptLevels) }
    var grades by remember { mutableStateOf(initialCriteria.grades) }
    var minStrokes by remember { mutableStateOf(initialCriteria.minStrokes) }
    var maxStrokes by remember { mutableStateOf(initialCriteria.maxStrokes) }
    var minFrequency by remember { mutableStateOf(initialCriteria.minFrequency) }
    var maxFrequency by remember { mutableStateOf(initialCriteria.maxFrequency) }
    var showLearned by remember { mutableStateOf(initialCriteria.showLearned) }
    var showUnlearned by remember { mutableStateOf(initialCriteria.showUnlearned) }
    var showDifficult by remember { mutableStateOf(initialCriteria.showDifficult) }
    var showFlagged by remember { mutableStateOf(initialCriteria.showFlagged) }
    var flags by remember { mutableStateOf(initialCriteria.flags) }
    var favoritesOnly by remember { mutableStateOf(initialCriteria.favoritesOnly) }
    var minLapses by remember { mutableStateOf(initialCriteria.minLapses) }
    var notReviewedDaysAgo by remember { mutableStateOf(initialCriteria.notReviewedDaysAgo) }
    var radicals by remember { mutableStateOf(initialCriteria.radicals) }
    var viewMode by remember { mutableStateOf(initialCriteria.viewMode) }
    var sortBy by remember { mutableStateOf(initialCriteria.sortBy) }

    var showFilters by remember { mutableStateOf(false) }
    var showRadicals by remember { mutableStateOf(initialCriteria.radicals.isNotEmpty()) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var detailCardId by remember { mutableStateOf<String?>(null) }
    var radicalFilteredSet by remember { mutableStateOf<Set<String>?>(null) }
    var flagPickerTarget by remember { mutableStateOf<List<String>?>(null) }
    var tagPickerTarget by remember { mutableStateOf<List<String>?>(null) }

    val scope = rememberCoroutineScope()

    // Radical search: query DB for chars containing all selected radicals
    LaunchedEffect(radicals) {
        if (radicals.isEmpty()) {
            radicalFilteredSet = null
        } else {
            radicalFilteredSet = dataCenter.loadCharactersWithRadicals(radicals)
        }
    }

    val filteredCards by remember(dataCenter.cards, query, jlptLevels, grades, minStrokes, maxStrokes,
        minFrequency, maxFrequency, showLearned, showUnlearned, showDifficult, showFlagged, flags,
        favoritesOnly, minLapses, notReviewedDaysAgo, radicalFilteredSet, sortBy) {
        derivedStateOf {
            var list: List<KaiteyoCard> = dataCenter.cards
            val minS = minStrokes
            val maxS = maxStrokes
            val minF = minFrequency
            val maxF = maxFrequency
            val minL = minLapses
            val notReviewed = notReviewedDaysAgo
            if (radicalFilteredSet != null) {
                list = list.filter { it.id in radicalFilteredSet!! }
            }
            if (favoritesOnly) list = list.filter { dataCenter.isFavorite(it.id) }
            if (showLearned) list = list.filter { dataCenter.isLearned(it.id) }
            if (showUnlearned) list = list.filter { !dataCenter.isLearned(it.id) }
            if (showDifficult) list = list.filter { dataCenter.isDifficult(it.id) }
            if (showFlagged) list = list.filter { dataCenter.cardFlagsFor(it.id) != CardFlagType.None }
            if (flags.isNotEmpty()) list = list.filter { dataCenter.cardFlagsFor(it.id).id in flags }
            if (jlptLevels.isNotEmpty()) {
                list = list.filter { card ->
                    val classes = dataCenter.classifications[card.id].orEmpty()
                    jlptLevels.any { level -> classes.contains("n$level") }
                }
            }
            if (grades.isNotEmpty()) {
                list = list.filter { card ->
                    val classes = dataCenter.classifications[card.id].orEmpty()
                    grades.any { grade -> classes.contains("o$grade") }
                }
            }
            if (minS != null || maxS != null) {
                list = list.filter { card ->
                    val strokes = dataCenter.strokeCounts[card.id] ?: return@filter false
                    (minS == null || strokes >= minS) &&
                        (maxS == null || strokes <= maxS)
                }
            }
            if (minF != null || maxF != null) {
                list = list.filter { card ->
                    val freq = dataCenter.frequencies[card.id] ?: return@filter false
                    (minF == null || freq >= minF) &&
                        (maxF == null || freq <= maxF)
                }
            }
            if (minL != null) {
                list = list.filter { (dataCenter.srsCards[it.id]?.lapses ?: 0) >= minL }
            }
            if (notReviewed != null) {
                list = list.filter { dataCenter.notReviewedFor(it.id, notReviewed) }
            }
            if (query.isNotBlank()) {
                val q = query.trim()
                val lower = q.lowercase()
                list = list.filter { card ->
                    card.character.contains(q) ||
                        card.meaning.lowercase().contains(lower) ||
                        card.reading.contains(q)
                }
            }
            when (sortBy) {
                KanjiBrowserSort.Frequency -> list.sortedBy { dataCenter.frequencies[it.id] ?: Int.MAX_VALUE }
                KanjiBrowserSort.StrokeCount -> list.sortedBy { dataCenter.strokeCounts[it.id] ?: 0 }
                KanjiBrowserSort.JLPT -> list.sortedBy {
                    (dataCenter.classifications[it.id].orEmpty()
                        .firstOrNull { c -> c.startsWith("n") }?.drop(1)?.toIntOrNull()
                        ?: 99)
                }
                KanjiBrowserSort.Difficulty -> list.sortedByDescending { dataCenter.isDifficult(it.id) }
                KanjiBrowserSort.LastReviewed -> list.sortedByDescending {
                    dataCenter.srsCards[it.id]?.lastReview?.toEpochMilliseconds() ?: 0L
                }
                KanjiBrowserSort.Kanji -> list.sortedBy { it.id }
            }
        }
    }

    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        BrowserHeader(
            query = query,
            onQueryChange = { query = it },
            showFilters = showFilters,
            onToggleFilters = { showFilters = !showFilters },
            showRadicals = showRadicals,
            onToggleRadicals = { showRadicals = !showRadicals },
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            selectionMode = selectionMode,
            onToggleSelectionMode = { selectionMode = !selectionMode },
            onClose = { navigationState.navigateBack() }
        )

        // Selection toolbar
        AnimatedVisibility(visible = selectionMode) {
            SelectionToolbar(
                selectedCount = selectedIds.size,
                onClear = { selectedIds = emptySet() },
                onFlag = { flagPickerTarget = selectedIds.toList() },
                onTag = { tagPickerTarget = selectedIds.toList() },
                onFavorite = {
                    scope.launch {
                        selectedIds.forEach { dataCenter.toggleFavorite(it) }
                        selectedIds = emptySet()
                    }
                },
                onResetProgress = {
                    scope.launch {
                        dataCenter.resetProgress(selectedIds.toList())
                        selectedIds = emptySet()
                    }
                }
            )
        }

        // Filters panel
        AnimatedVisibility(visible = showFilters) {
            BrowserFilters(
                jlptLevels = jlptLevels,
                onJlptToggle = { level ->
                    jlptLevels = if (level in jlptLevels) jlptLevels - level else jlptLevels + level
                },
                grades = grades,
                onGradeToggle = { grade ->
                    grades = if (grade in grades) grades - grade else grades + grade
                },
                minStrokes = minStrokes,
                maxStrokes = maxStrokes,
                onMinStrokes = { minStrokes = it },
                onMaxStrokes = { maxStrokes = it },
                minFrequency = minFrequency,
                maxFrequency = maxFrequency,
                onMinFrequency = { minFrequency = it },
                onMaxFrequency = { maxFrequency = it },
                showLearned = showLearned,
                onShowLearned = { showLearned = it },
                showUnlearned = showUnlearned,
                onShowUnlearned = { showUnlearned = it },
                showDifficult = showDifficult,
                onShowDifficult = { showDifficult = it },
                showFlagged = showFlagged,
                onShowFlagged = { showFlagged = it },
                flags = flags,
                onFlagToggle = { flagId ->
                    flags = if (flagId in flags) flags - flagId else flags + flagId
                },
                favoritesOnly = favoritesOnly,
                onFavoritesOnly = { favoritesOnly = it },
                minLapses = minLapses,
                onMinLapses = { minLapses = it },
                notReviewedDaysAgo = notReviewedDaysAgo,
                onNotReviewedDaysAgo = { notReviewedDaysAgo = it },
                sortBy = sortBy,
                onSortBy = { sortBy = it },
                onReset = {
                    jlptLevels = emptySet(); grades = emptySet()
                    minStrokes = null; maxStrokes = null
                    minFrequency = null; maxFrequency = null
                    showLearned = false; showUnlearned = false
                    showDifficult = false; showFlagged = false
                    flags = emptySet(); favoritesOnly = false
                    minLapses = null; notReviewedDaysAgo = null
                }
            )
        }

        // Radical picker
        AnimatedVisibility(visible = showRadicals) {
            RadicalPicker(
                dataCenter = dataCenter,
                selectedRadicals = radicals,
                onRadicalsChange = { radicals = it },
                scope = scope
            )
        }

        // Results
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                dataCenter.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading kanji…", color = surfaceColors.textMuted)
                }
                filteredCards.isEmpty() -> BrowserEmptyState(
                    hasFilters = query.isNotBlank() || jlptLevels.isNotEmpty() || grades.isNotEmpty() ||
                        flags.isNotEmpty() || showFlagged || favoritesOnly || radicals.isNotEmpty(),
                    onClear = {
                        query = ""; jlptLevels = emptySet(); grades = emptySet()
                        flags = emptySet(); showFlagged = false; favoritesOnly = false
                        radicals = emptySet(); radicalFilteredSet = null
                    }
                )
                else -> when (viewMode) {
                    KanjiBrowserViewMode.Grid -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(88.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredCards, key = { it.id }) { card ->
                            KanjiGridTile(
                                card = card,
                                dataCenter = dataCenter,
                                selectionMode = selectionMode,
                                isSelected = card.id in selectedIds,
                                onSelect = {
                                    selectedIds = if (it in selectedIds) selectedIds - it else selectedIds + it
                                },
                                onClick = { detailCardId = card.id },
                                onLongClick = {
                                    selectionMode = true
                                    selectedIds = setOf(card.id)
                                }
                            )
                        }
                    }
                    KanjiBrowserViewMode.List -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredCards, key = { it.id }) { card ->
                            KanjiListRow(
                                card = card,
                                dataCenter = dataCenter,
                                selectionMode = selectionMode,
                                isSelected = card.id in selectedIds,
                                onSelect = {
                                    selectedIds = if (it in selectedIds) selectedIds - it else selectedIds + it
                                },
                                onClick = { detailCardId = card.id },
                                onLongClick = {
                                    selectionMode = true
                                    selectedIds = setOf(card.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Result count bar
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = "${filteredCards.size} kanji",
                color = surfaceColors.textMuted,
                fontSize = 12.sp
            )
        }
    }

    // Dialogs
    detailCardId?.let { cardId ->
        KanjiDetailDialog(
            cardId = cardId,
            dataCenter = dataCenter,
            onDismiss = { detailCardId = null },
            scope = scope
        )
    }

    flagPickerTarget?.let { targets ->
        FlagPickerDialog(
            currentFlag = targets.singleOrNull()?.let { dataCenter.cardFlagsFor(it) },
            onPick = { flagType ->
                scope.launch { dataCenter.setFlag(targets, flagType) }
                flagPickerTarget = null
                selectedIds = emptySet()
                selectionMode = false
            },
            onDismiss = { flagPickerTarget = null }
        )
    }

    tagPickerTarget?.let { targets ->
        TagPickerDialog(
            dataCenter = dataCenter,
            onApply = { tagId, add ->
                scope.launch {
                    if (add) dataCenter.addTagToCards(targets, tagId)
                    else dataCenter.removeTagFromCards(targets, tagId)
                }
                tagPickerTarget = null
            },
            onDismiss = { tagPickerTarget = null }
        )
    }
}

// ============================================
// Header
// ============================================

@Composable
private fun BrowserHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    showRadicals: Boolean,
    onToggleRadicals: () -> Unit,
    viewMode: KanjiBrowserViewMode,
    onViewModeChange: (KanjiBrowserViewMode) -> Unit,
    selectionMode: Boolean,
    onToggleSelectionMode: () -> Unit,
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Kanji Browser",
                color = surfaceColors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        HeaderIconButton(icon = Icons.Default.SelectAll, selected = selectionMode, onClick = onToggleSelectionMode, accent = accent, surfaceColors = surfaceColors)
        HeaderIconButton(icon = Icons.Default.FilterList, selected = showFilters, onClick = onToggleFilters, accent = accent, surfaceColors = surfaceColors)
        HeaderIconButton(icon = Icons.Outlined.Flag, selected = showRadicals, onClick = onToggleRadicals, accent = accent, surfaceColors = surfaceColors)
        HeaderIconButton(
            icon = if (viewMode == KanjiBrowserViewMode.Grid) Icons.Default.List else Icons.Default.GridView,
            selected = false,
            onClick = { onViewModeChange(if (viewMode == KanjiBrowserViewMode.Grid) KanjiBrowserViewMode.List else KanjiBrowserViewMode.Grid) },
            accent = accent,
            surfaceColors = surfaceColors
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surfaceInteractive)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Search, null, tint = surfaceColors.textMuted, modifier = Modifier.size(18.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = surfaceColors.textPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(accent.primary),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = surfaceColors.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (selected) accent.primary.copy(alpha = 0.15f)
        else if (hovered) surfaceColors.surfaceInteractive else Color.Transparent,
        label = "hdrBg"
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) accent.primary else surfaceColors.textSecondary,
            modifier = Modifier.size(19.dp)
        )
    }
}

// ============================================
// Selection toolbar
// ============================================

@Composable
private fun SelectionToolbar(
    selectedCount: Int,
    onClear: () -> Unit,
    onFlag: () -> Unit,
    onTag: () -> Unit,
    onFavorite: () -> Unit,
    onResetProgress: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColors.surfaceElevated)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "$selectedCount selected",
            color = accent.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        BulkActionButton("Flag", onClick = onFlag, accent = accent, surfaceColors = surfaceColors)
        BulkActionButton("Tag", onClick = onTag, accent = accent, surfaceColors = surfaceColors)
        BulkActionButton("Favorite", onClick = onFavorite, accent = accent, surfaceColors = surfaceColors)
        BulkActionButton("Reset", onClick = onResetProgress, accent = accent, surfaceColors = surfaceColors)
        TextButton(onClick = onClear) {
            Text("Clear", color = surfaceColors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BulkActionButton(
    label: String,
    onClick: () -> Unit,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.primary.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ============================================
// Filters
// ============================================

@Composable
private fun BrowserFilters(
    jlptLevels: Set<Int>,
    onJlptToggle: (Int) -> Unit,
    grades: Set<Int>,
    onGradeToggle: (Int) -> Unit,
    minStrokes: Int?,
    maxStrokes: Int?,
    onMinStrokes: (Int?) -> Unit,
    onMaxStrokes: (Int?) -> Unit,
    minFrequency: Int?,
    maxFrequency: Int?,
    onMinFrequency: (Int?) -> Unit,
    onMaxFrequency: (Int?) -> Unit,
    showLearned: Boolean,
    onShowLearned: (Boolean) -> Unit,
    showUnlearned: Boolean,
    onShowUnlearned: (Boolean) -> Unit,
    showDifficult: Boolean,
    onShowDifficult: (Boolean) -> Unit,
    showFlagged: Boolean,
    onShowFlagged: (Boolean) -> Unit,
    flags: Set<Int>,
    onFlagToggle: (Int) -> Unit,
    favoritesOnly: Boolean,
    onFavoritesOnly: (Boolean) -> Unit,
    minLapses: Int?,
    onMinLapses: (Int?) -> Unit,
    notReviewedDaysAgo: Int?,
    onNotReviewedDaysAgo: (Int?) -> Unit,
    sortBy: KanjiBrowserSort,
    onSortBy: (KanjiBrowserSort) -> Unit,
    onReset: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Filters", color = surfaceColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset) {
                Text("Reset all", color = accent.primary, fontSize = 12.sp)
            }
        }

        FilterSection("JLPT") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((5 downTo 1).toList()) { level ->
                    FilterChip(
                        label = "N$level",
                        selected = level in jlptLevels,
                        onClick = { onJlptToggle(level) },
                        accent = accent,
                        surfaceColors = surfaceColors
                    )
                }
            }
        }

        FilterSection("Grade") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(1, 2, 3, 4, 5, 6, 8, 9, 10)) { grade ->
                    FilterChip(
                        label = when (grade) {
                            8 -> "Secondary"
                            9 -> "Names"
                            10 -> "Names Var."
                            else -> grade.toString()
                        },
                        selected = grade in grades,
                        onClick = { onGradeToggle(grade) },
                        accent = accent,
                        surfaceColors = surfaceColors
                    )
                }
            }
        }

        FilterSection("Status") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("Learned", showLearned, { onShowLearned(!showLearned) }, accent, surfaceColors)
                FilterChip("Unlearned", showUnlearned, { onShowUnlearned(!showUnlearned) }, accent, surfaceColors)
                FilterChip("Difficult", showDifficult, { onShowDifficult(!showDifficult) }, accent, surfaceColors)
                FilterChip("Flagged", showFlagged, { onShowFlagged(!showFlagged) }, accent, surfaceColors)
                FilterChip("Favorites", favoritesOnly, { onFavoritesOnly(!favoritesOnly) }, accent, surfaceColors)
                if (minLapses != null) {
                    FilterChip("Failed ≥ $minLapses", true, { onMinLapses(null) }, accent, surfaceColors)
                } else {
                    FilterChip("Failed 3+", false, { onMinLapses(3) }, accent, surfaceColors)
                }
                if (notReviewedDaysAgo != null) {
                    FilterChip("Not reviewed ≥ ${notReviewedDaysAgo}d", true, { onNotReviewedDaysAgo(null) }, accent, surfaceColors)
                } else {
                    FilterChip("Not reviewed 30d", false, { onNotReviewedDaysAgo(30) }, accent, surfaceColors)
                }
            }
        }

        FilterSection("Flags") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CardFlagType.entries.filter { it != CardFlagType.None }) { flag ->
                    FilterChip(
                        label = flag.displayName,
                        selected = flag.id in flags,
                        onClick = { onFlagToggle(flag.id) },
                        accent = accent,
                        surfaceColors = surfaceColors,
                        dotColor = flag.colorFromHex()
                    )
                }
            }
        }

        FilterSection("Strokes") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberStepper(value = minStrokes, label = "Min", onChange = onMinStrokes, accent = accent, surfaceColors = surfaceColors)
                Text("–", color = surfaceColors.textMuted)
                NumberStepper(value = maxStrokes, label = "Max", onChange = onMaxStrokes, accent = accent, surfaceColors = surfaceColors)
            }
        }

        FilterSection("Frequency (rank)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberStepper(value = minFrequency, label = "Min", onChange = onMinFrequency, accent = accent, surfaceColors = surfaceColors)
                Text("–", color = surfaceColors.textMuted)
                NumberStepper(value = maxFrequency, label = "Max", onChange = onMaxFrequency, accent = accent, surfaceColors = surfaceColors)
            }
        }

        FilterSection("Sort") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(KanjiBrowserSort.entries) { sort ->
                    FilterChip(
                        label = sort.displayName,
                        selected = sortBy == sort,
                        onClick = { onSortBy(sort) },
                        accent = accent,
                        surfaceColors = surfaceColors
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = surfaceColors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    dotColor: Color? = null
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accent.primary.copy(alpha = 0.14f) else surfaceColors.surfaceInteractive,
        label = "chipBg"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, if (selected) accent.primary.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (dotColor != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        }
        Text(
            label,
            color = if (selected) accent.primary else surfaceColors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun NumberStepper(
    value: Int?,
    label: String,
    onChange: (Int?) -> Unit,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceColors.surfaceInteractive)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, color = surfaceColors.textMuted, fontSize = 11.sp)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(accent.primary.copy(alpha = 0.12f))
                .clickable {
                    val newValue = ((value ?: 0) - 1).coerceAtLeast(0)
                    onChange(newValue)
                },
            contentAlignment = Alignment.Center
        ) {
            Text("−", color = accent.primary, fontSize = 13.sp)
        }
        Text(
            text = value?.toString() ?: "Any",
            color = surfaceColors.textPrimary,
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(accent.primary.copy(alpha = 0.12f))
                .clickable { onChange((value ?: 0) + 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = accent.primary, fontSize = 13.sp)
        }
    }
}

private val KanjiBrowserSort.displayName: String
    get() = when (this) {
        KanjiBrowserSort.Frequency -> "Frequency"
        KanjiBrowserSort.StrokeCount -> "Strokes"
        KanjiBrowserSort.JLPT -> "JLPT"
        KanjiBrowserSort.Difficulty -> "Difficulty"
        KanjiBrowserSort.LastReviewed -> "Last reviewed"
        KanjiBrowserSort.Kanji -> "Kanji"
    }

// ============================================
// Radical picker
// ============================================

@Composable
private fun RadicalPicker(
    dataCenter: KaiteyoDataCenter,
    selectedRadicals: Set<String>,
    onRadicalsChange: (Set<String>) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var radicals by remember { mutableStateOf<List<RadicalData>>(emptyList()) }

    LaunchedEffect(Unit) {
        radicals = dataCenter.loadRadicals()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Radical search", color = surfaceColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (selectedRadicals.isNotEmpty()) {
                Text("${selectedRadicals.size} selected", color = accent.primary, fontSize = 12.sp)
            }
        }
        val grouped = radicals.groupBy { it.strokesCount }
        LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            grouped.toSortedMap().forEach { (count, group) ->
                item(key = "group-$count") {
                    Text(
                        text = "$count strokes",
                        color = surfaceColors.textMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                item(key = "radicals-$count") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        group.forEach { radical ->
                            RadicalChip(
                                radical = radical.radical,
                                selected = radical.radical in selectedRadicals,
                                onClick = {
                                    onRadicalsChange(
                                        if (radical.radical in selectedRadicals) selectedRadicals - radical.radical
                                        else selectedRadicals + radical.radical
                                    )
                                },
                                accent = accent,
                                surfaceColors = surfaceColors
                            )
                        }
                    }
                }
            }
        }
        if (selectedRadicals.isNotEmpty()) {
            TextButton(
                onClick = { onRadicalsChange(emptySet()) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Clear radicals", color = accent.primary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RadicalChip(
    radical: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent.primary.copy(alpha = 0.18f) else surfaceColors.surfaceInteractive)
            .border(1.dp, if (selected) accent.primary.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            radical,
            color = if (selected) accent.primary else surfaceColors.textPrimary,
            fontSize = 17.sp
        )
    }
}

// ============================================
// Grid / List items
// ============================================

@Composable
private fun KanjiGridTile(
    card: KaiteyoCard,
    dataCenter: KaiteyoDataCenter,
    selectionMode: Boolean,
    isSelected: Boolean,
    onSelect: (String) -> Unit,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue = when {
            isSelected -> accent.primary.copy(alpha = 0.16f)
            hovered -> surfaceColors.surfaceInteractive
            else -> surfaceColors.surface
        },
        label = "tileBg"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, if (isSelected) accent.primary.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null) {
                if (selectionMode) onSelect(card.id) else onClick(card.id)
            }
            .hoverable(interactionSource)
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = card.character,
                fontSize = 30.sp,
                color = surfaceColors.textPrimary,
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                card.flag.takeIf { it != CardFlagType.None }?.let { flag ->
                    Box(Modifier.size(6.dp).clip(CircleShape).background(flag.colorFromHex()))
                }
                if (card.isFavorite) {
                    Icon(Icons.Default.Star, null, tint = accent.secondary, modifier = Modifier.size(10.dp))
                }
                dataCenter.classifications[card.id].orEmpty()
                    .firstOrNull { it.startsWith("n") }
                    ?.let { level ->
                        Text(
                            text = level.uppercase(),
                            color = surfaceColors.textMuted,
                            fontSize = 9.sp
                        )
                    }
            }
        }
        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) accent.primary else surfaceColors.textMuted,
                modifier = Modifier.align(Alignment.TopStart).size(16.dp)
            )
        }
    }
}

@Composable
private fun KanjiListRow(
    card: KaiteyoCard,
    dataCenter: KaiteyoDataCenter,
    selectionMode: Boolean,
    isSelected: Boolean,
    onSelect: (String) -> Unit,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue = when {
            isSelected -> accent.primary.copy(alpha = 0.14f)
            hovered -> surfaceColors.surfaceInteractive
            else -> surfaceColors.surface
        },
        label = "rowBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null) {
                if (selectionMode) onSelect(card.id) else onClick(card.id)
            }
            .hoverable(interactionSource)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) accent.primary else surfaceColors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = card.character,
            fontSize = 22.sp,
            color = surfaceColors.textPrimary,
            modifier = Modifier.width(44.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = card.reading.ifBlank { "—" },
                color = surfaceColors.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = card.meaning.take(60).ifBlank { "No meaning" },
                color = surfaceColors.textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            dataCenter.strokeCounts[card.id]?.let { strokes ->
                Text("${strokes}画", color = surfaceColors.textMuted, fontSize = 11.sp)
            }
            dataCenter.classifications[card.id].orEmpty().firstOrNull { it.startsWith("n") }?.let {
                JlptBadge(it, accent, surfaceColors)
            }
            card.flag.takeIf { it != CardFlagType.None }?.let { flag ->
                Box(Modifier.size(8.dp).clip(CircleShape).background(flag.colorFromHex()))
            }
            if (card.isFavorite) {
                Icon(Icons.Default.Favorite, null, tint = Color(0xFFFF6B9D), modifier = Modifier.size(14.dp))
            }
            if (dataCenter.isDifficult(card.id)) {
                Text("⚠", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun JlptBadge(
    level: String,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.primary.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(level.uppercase(), color = accent.primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================
// Empty state
// ============================================

@Composable
private fun BrowserEmptyState(hasFilters: Boolean, onClear: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accent.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text("字", fontSize = 32.sp, color = accent.primary)
            }
            Text("No kanji found", color = surfaceColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(
                if (hasFilters) "Try adjusting or clearing the filters" else "Search for a kanji, reading or meaning",
                color = surfaceColors.textMuted,
                fontSize = 13.sp
            )
            if (hasFilters) {
                OutlinedButton(onClick = onClear) {
                    Text("Clear filters", color = accent.primary, fontSize = 13.sp)
                }
            }
        }
    }
}

// ============================================
// Dialogs
// ============================================

@Composable
fun KanjiDetailDialog(
    cardId: String,
    dataCenter: KaiteyoDataCenter,
    onDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val card = dataCenter.cardById(cardId)

    if (card == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var tagPickerOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(460.dp)
                .height(560.dp),
            shape = RoundedCornerShape(24.dp),
            color = surfaceColors.surfaceElevated
        ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = card.character,
                            fontSize = 56.sp,
                            color = surfaceColors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { scope.launch { dataCenter.toggleFavorite(cardId) } }) {
                            Icon(
                                if (card.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (card.isFavorite) Color(0xFFFF6B9D) else surfaceColors.textMuted
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close", tint = surfaceColors.textMuted)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = card.meaning,
                            color = surfaceColors.textSecondary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = card.reading,
                            color = surfaceColors.textSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Info chips
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dataCenter.strokeCounts[cardId]?.let { strokes ->
                            InfoChip("${strokes} strokes", accent, surfaceColors)
                        }
                        dataCenter.classifications[cardId].orEmpty().forEach { cls ->
                            InfoChip(
                                when {
                                    cls.startsWith("n") -> "JLPT ${cls.uppercase()}"
                                    else -> "Grade ${cls.drop(1)}"
                                },
                                accent,
                                surfaceColors
                            )
                        }
                        dataCenter.frequencies[cardId]?.let { freq ->
                            InfoChip("#$freq most frequent", accent, surfaceColors)
                        }
                        if (dataCenter.isLearned(cardId)) {
                            InfoChip("Learned", accent, surfaceColors)
                        }
                        if (dataCenter.isDifficult(cardId)) {
                            InfoChip("Difficult", Color(0xFFFF6B6B), surfaceColors)
                        }
                        card.flag.takeIf { it != CardFlagType.None }?.let { flag ->
                            InfoChip("Flagged ${flag.displayName}", flag.colorFromHex(), surfaceColors)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Flag selector
                    Text("Flag", color = surfaceColors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlagColorOption(CardFlagType.None, card.flag == CardFlagType.None, surfaceColors) {
                            scope.launch { dataCenter.setFlag(listOf(cardId), CardFlagType.None) }
                        }
                        CardFlagType.entries.filter { it != CardFlagType.None }.forEach { flag ->
                            FlagColorOption(flag, card.flag == flag, surfaceColors) {
                                scope.launch { dataCenter.setFlag(listOf(cardId), flag) }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Tags
                    Text("Tags", color = surfaceColors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    val cardTagIds = dataCenter.cardTags[cardId].orEmpty()
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (cardTagIds.isEmpty()) {
                            Text("No tags", color = surfaceColors.textMuted, fontSize = 12.sp)
                        }
                        dataCenter.tags.forEach { tag ->
                            if (tag.id in cardTagIds) {
                                TagBadge(tag, accent, surfaceColors)
                            }
                        }
                        TextButton(onClick = { tagPickerOpen = true }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("+ Add", color = accent.primary, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Close", color = surfaceColors.textSecondary)
                        }
                        Button(
                            onClick = { scope.launch { dataCenter.resetProgress(listOf(cardId)) } },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B6B).copy(alpha = 0.15f),
                                contentColor = Color(0xFFFF6B6B)
                            )
                        ) {
                            Text("Reset progress")
                        }
                    }
                }
        }
    }

    if (tagPickerOpen) {
        TagPickerDialog(
            dataCenter = dataCenter,
            onApply = { tagId, add ->
                scope.launch {
                    if (add) dataCenter.addTagToCards(listOf(cardId), tagId)
                    else dataCenter.removeTagFromCards(listOf(cardId), tagId)
                }
                tagPickerOpen = false
            },
            onDismiss = { tagPickerOpen = false }
        )
    }
}

@Composable
private fun InfoChip(
    label: String,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.primary.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoChip(label: String, color: Color, surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FlagColorOption(
    flag: CardFlagType,
    selected: Boolean,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    onClick: () -> Unit
) {
    val color = if (flag == CardFlagType.None) surfaceColors.textMuted else flag.colorFromHex()
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (flag == CardFlagType.None) 0.15f else 1f))
            .border(2.dp, if (selected) Color.White.copy(alpha = 0.9f) else Color.Transparent, CircleShape)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun TagBadge(
    tag: ua.syt0r.kanji.presentation.screen.main.screen.decks.CardTag,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    val color = tag.getDisplayColor()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(tag.name, color = color, fontSize = 11.sp)
    }
}

// ============================================
// Flag picker dialog (bulk)
// ============================================

@Composable
fun FlagPickerDialog(
    currentFlag: CardFlagType?,
    onPick: (CardFlagType) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.width(360.dp),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColors.surfaceElevated
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Set flag", color = surfaceColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                CardFlagType.entries.forEach { flag ->
                    val color = if (flag == CardFlagType.None) surfaceColors.textMuted else flag.colorFromHex()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (currentFlag == flag) color.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { onPick(flag) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.size(16.dp).clip(CircleShape).background(color))
                        Text(
                            if (flag == CardFlagType.None) "No flag" else flag.displayName,
                            color = surfaceColors.textPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.weight(1f))
                        if (currentFlag == flag) {
                            Icon(Icons.Default.Done, null, tint = color, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel", color = surfaceColors.textMuted)
                }
            }
        }
    }
}

// ============================================
// Tag picker dialog (bulk)
// ============================================

@Composable
fun TagPickerDialog(
    dataCenter: KaiteyoDataCenter,
    onApply: (Long, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableStateOf("#C2FC8B") }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.width(380.dp),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColors.surfaceElevated
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tags", color = surfaceColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

                dataCenter.tags.forEach { tag ->
                    val color = tag.getDisplayColor()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.10f))
                            .clickable { onApply(tag.id, true) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                        Text(tag.name, color = surfaceColors.textPrimary, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Text("+", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaceColors.surfaceInteractive)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = surfaceColors.textPrimary, fontSize = 13.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(accent.primary),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (newTagName.isEmpty()) {
                                    Text("New tag name", color = surfaceColors.textMuted, fontSize = 13.sp)
                                }
                                inner()
                            }
                        )
                    }
                    TextButton(
                        enabled = newTagName.isNotBlank(),
                        onClick = {
                            scope.launch {
                                val id = dataCenter.createTag(newTagName.trim(), newTagColor)
                                onApply(id, true)
                            }
                        }
                    ) {
                        Text("Create", color = accent.primary, fontSize = 13.sp)
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel", color = surfaceColors.textMuted)
                }
            }
        }
    }
}
