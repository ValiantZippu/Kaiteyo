package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors

// ============================================
// SEARCH ENGINE
// Universal search across all card fields
// Kanji, Kana, Meaning, Tag, Flag, Deck, Notes
// Stroke count, JLPT, Frequency, Status
// ============================================

data class SearchCriteria(
    val query: String = "",
    val field: SearchField = SearchField.All,
    val tagIds: List<Long> = emptyList(),
    val flagType: CardFlagType? = null,
    val statuses: List<CardStatus> = emptyList(),
    val deckIds: List<Long> = emptyList(),
    val jlptLevel: Int? = null,
    val minStrokeCount: Int? = null,
    val maxStrokeCount: Int? = null,
    val minFrequency: Int? = null,
    val maxFrequency: Int? = null,
    val sortBy: SearchSortField = SearchSortField.Relevance,
    val sortAscending: Boolean = true,
    val isRegex: Boolean = false,
    val matchCase: Boolean = false
)

data class SearchPreset(
    val name: String,
    val criteria: SearchCriteria,
    val icon: @Composable () -> Unit = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) }
)

private val defaultPresets = listOf(
    SearchPreset("Due cards", SearchCriteria(statuses = listOf(CardStatus.Learning, CardStatus.Relearning))),
    SearchPreset("New cards", SearchCriteria(statuses = listOf(CardStatus.New))),
    SearchPreset("Flagged cards", SearchCriteria(flagType = CardFlagType.Red)),
    SearchPreset("Suspended", SearchCriteria(statuses = listOf(CardStatus.Suspended))),
    SearchPreset("JLPT N5", SearchCriteria(jlptLevel = 5)),
    SearchPreset("Most lapses", SearchCriteria(sortBy = SearchSortField.Relevance)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineScreen(
    cards: List<KaiteyoCard> = emptyList(),
    onSearch: (SearchCriteria) -> Unit = { },
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var criteria by remember { mutableStateOf(SearchCriteria()) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showPresets by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<KaiteyoCard>>(emptyList()) }

    // Perform search when criteria changes
    LaunchedEffect(criteria) {
        results = performSearch(cards, criteria)
        onSearch(criteria)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = { showPresets = !showPresets }) {
                        Icon(Icons.Default.Bookmark, "Presets")
                    }
                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(if (showAdvanced) "Simple" else "Advanced")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = criteria.query,
                onValueChange = { criteria = criteria.copy(query = it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search cards...") },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
                trailingIcon = {
                    if (criteria.query.isNotBlank()) {
                        IconButton(onClick = { criteria = criteria.copy(query = "") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* search triggered */ }),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Results count
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${results.size} results", fontSize = 12.sp, color = surfaceColors.textMuted,
                    modifier = Modifier.weight(1f))
                // Sort dropdown
                var sortExpanded by remember { mutableStateOf(false) }
                Text("Sort: ", fontSize = 12.sp, color = surfaceColors.textMuted)
                Text(criteria.sortBy.displayName, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { sortExpanded = true })
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    SearchSortField.entries.forEach { field ->
                        DropdownMenuItem(
                            text = { Text(field.displayName) },
                            onClick = { criteria = criteria.copy(sortBy = field); sortExpanded = false }
                        )
                    }
                }
                IconButton(onClick = { criteria = criteria.copy(sortAscending = !criteria.sortAscending) },
                    modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (criteria.sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        "Sort order", Modifier.size(16.dp)
                    )
                }
            }

            // Presets bar
            if (showPresets) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    defaultPresets.forEach { preset ->
                        SuggestionChip(
                            onClick = { criteria = preset.criteria },
                            label = { Text(preset.name, fontSize = 11.sp) },
                            icon = preset.icon
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Advanced filters
            if (showAdvanced) {
                AdvancedSearchFilters(
                    criteria = criteria,
                    onUpdate = { criteria = it },
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Search results
            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, Modifier.size(48.dp), tint = surfaceColors.textMuted)
                        Spacer(Modifier.height(8.dp))
                        Text("No results found", fontSize = 16.sp, color = surfaceColors.textMuted)
                        Text("Try adjusting your search criteria", fontSize = 13.sp, color = surfaceColors.textMuted)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(results, key = { it.id }) { card ->
                        SearchResultCard(
                            card = card,
                            query = criteria.query,
                            surfaceColors = surfaceColors,
                            accent = accent
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedSearchFilters(
    criteria: SearchCriteria,
    onUpdate: (SearchCriteria) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Advanced Filters", fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = surfaceColors.textMuted)

        // Field selector
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Field:", Modifier.width(80.dp), fontSize = 12.sp)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = criteria.field.displayName,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.menuAnchor().weight(1f),
                    textStyle = TextStyle(fontSize = 12.sp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    singleLine = true
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SearchField.entries.forEach { field ->
                        DropdownMenuItem(
                            text = { Text(field.displayName) },
                            onClick = { onUpdate(criteria.copy(field = field)); expanded = false }
                        )
                    }
                }
            }
        }

        // Status multi-select
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Status:", Modifier.width(80.dp), fontSize = 12.sp)
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                CardStatus.entries.forEach { status ->
                    val isSelected = status in criteria.statuses
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newStatuses = if (isSelected) criteria.statuses - status
                            else criteria.statuses + status
                            onUpdate(criteria.copy(statuses = newStatuses))
                        },
                        label = { Text(status.displayName.take(4), fontSize = 9.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // Flag filter
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Flag:", Modifier.width(80.dp), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CardFlagType.entries.forEach { flag ->
                    val isSelected = criteria.flagType == flag
                    Box(
                        modifier = Modifier.size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) flag.colorFromHex() else Color.Transparent)
                            .border(
                                if (isSelected) 1.dp else 1.dp,
                                if (isSelected) flag.colorFromHex() else surfaceColors.textMuted.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onUpdate(criteria.copy(flagType = if (isSelected) null else flag)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected && flag == CardFlagType.None) {
                            Text("✕", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // JLPT + Stroke count row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("JLPT", fontSize = 11.sp, color = surfaceColors.textMuted)
                var expandedJlpt by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedJlpt, onExpandedChange = { expandedJlpt = it }) {
                    OutlinedTextField(
                        value = criteria.jlptLevel?.let { "N$it" } ?: "Any",
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 12.sp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedJlpt) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expandedJlpt, onDismissRequest = { expandedJlpt = false }) {
                        listOf(null, 5, 4, 3, 2, 1).forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level?.let { "N$it" } ?: "Any") },
                                onClick = { onUpdate(criteria.copy(jlptLevel = level)); expandedJlpt = false }
                            )
                        }
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Min Strokes", fontSize = 11.sp, color = surfaceColors.textMuted)
                OutlinedTextField(
                    value = criteria.minStrokeCount?.toString() ?: "",
                    onValueChange = { onUpdate(criteria.copy(minStrokeCount = it.toIntOrNull())) },
                    textStyle = TextStyle(fontSize = 12.sp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Max Strokes", fontSize = 11.sp, color = surfaceColors.textMuted)
                OutlinedTextField(
                    value = criteria.maxStrokeCount?.toString() ?: "",
                    onValueChange = { onUpdate(criteria.copy(maxStrokeCount = it.toIntOrNull())) },
                    textStyle = TextStyle(fontSize = 12.sp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Options
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = criteria.isRegex, onCheckedChange = { onUpdate(criteria.copy(isRegex = it)) },
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Regex", fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = criteria.matchCase, onCheckedChange = { onUpdate(criteria.copy(matchCase = it)) },
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Match case", fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultCard(
    card: KaiteyoCard,
    query: String,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag indicator
            if (card.flag != CardFlagType.None) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(card.flag.colorFromHex())
                )
                Spacer(Modifier.width(8.dp))
            }

            // Card content
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(card.character, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(status = card.status)
                    if (card.isSuspended) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Block, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
                Text(card.reading, fontSize = 13.sp, color = surfaceColors.textMuted)
                Text(card.meaning, fontSize = 13.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                // Tags
                if (card.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        card.tags.take(3).forEach { tag ->
                            Text(tag.name, fontSize = 10.sp, color = accent.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accent.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                        if (card.tags.size > 3) {
                            Text("+${card.tags.size - 3}", fontSize = 10.sp, color = surfaceColors.textMuted)
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Stats column
            Column(horizontalAlignment = Alignment.End) {
                Text("${card.interval}d", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("${(card.ease * 100).toInt()}%", fontSize = 11.sp, color = surfaceColors.textMuted)
                Text("${card.reviewCount} rev", fontSize = 11.sp, color = surfaceColors.textMuted)
            }
        }
    }
}

// ============================================
// SEARCH LOGIC
// ============================================

private fun performSearch(cards: List<KaiteyoCard>, criteria: SearchCriteria): List<KaiteyoCard> {
    var results = cards

    // Text query filtering
    if (criteria.query.isNotBlank()) {
        val query = if (criteria.matchCase) criteria.query else criteria.query.lowercase()
        results = results.filter { card ->
            val searchIn = when (criteria.field) {
                SearchField.All -> listOf(
                    card.character, card.reading, card.meaning, card.notes,
                    card.deck, card.flag.displayName, card.status.displayName,
                    card.tags.joinToString(" ") { it.name }
                )
                SearchField.Kanji -> listOf(card.character)
                SearchField.Kana -> listOf(card.reading)
                SearchField.Meaning -> listOf(card.meaning)
                SearchField.Tag -> card.tags.map { it.name }
                SearchField.Flag -> listOf(card.flag.displayName)
                SearchField.Deck -> listOf(card.deck)
                SearchField.Notes -> listOf(card.notes)
                SearchField.Status -> listOf(card.status.displayName)
                else -> listOf(card.character, card.reading, card.meaning)
            }
            val fullText = searchIn.joinToString(" ").let { if (criteria.matchCase) it else it.lowercase() }
            if (criteria.isRegex) {
                try { fullText.contains(Regex(query)) } catch (_: Exception) { false }
            } else {
                fullText.contains(query)
            }
        }
    }

    // Status filter
    if (criteria.statuses.isNotEmpty()) {
        results = results.filter { it.status in criteria.statuses }
    }

    // Flag filter
    criteria.flagType?.let { flag ->
        results = if (flag == CardFlagType.None) results.filter { it.flag == CardFlagType.None }
        else results.filter { it.flag == flag }
    }

    // JLPT filter
    criteria.jlptLevel?.let { level ->
        results = results.filter { card ->
            card.tagNames.any { it.contains("jlpt", ignoreCase = true) && it.contains("$level") }
        }
    }

    // Stroke count filter
    criteria.minStrokeCount?.let { min ->
        results = results.filter { card ->
            card.character.length >= min // Simplified proxy
        }
    }
    criteria.maxStrokeCount?.let { max ->
        results = results.filter { card ->
            card.character.length <= max
        }
    }

    // Sort
    results = when (criteria.sortBy) {
        SearchSortField.Relevance -> results
        SearchSortField.Kanji -> results.sortedBy { it.character }
        SearchSortField.Deck -> results.sortedBy { it.deck }
        SearchSortField.Created -> results.sortedBy { it.createdAt }
        SearchSortField.Modified -> results.sortedBy { it.modifiedAt }
        SearchSortField.Interval -> results.sortedBy { it.interval }
        SearchSortField.Ease -> results.sortedBy { it.ease }
        SearchSortField.Reviews -> results.sortedBy { it.reviewCount }
        SearchSortField.Accuracy -> results.sortedBy { it.accuracy }
        SearchSortField.Frequency -> results.sortedBy { it.character.length } // Proxy
        SearchSortField.StrokeCount -> results.sortedBy { it.character.length }
        SearchSortField.JLPT -> results.sortedBy { card ->
            card.tagNames.firstOrNull { it.contains("jlpt") }?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        }
        SearchSortField.Reading -> results.sortedBy { it.character }
        SearchSortField.Meaning -> results.sortedBy { it.character }
        SearchSortField.Lapses -> results.sortedBy { it.interval } // proxy
        SearchSortField.LastReviewed -> results.sortedBy { it.modifiedAt }
        SearchSortField.NextReview -> results.sortedBy { it.modifiedAt }
        SearchSortField.Status -> results.sortedBy { it.deck }
        SearchSortField.Flag -> results.sortedBy { it.character }
    }

    if (!criteria.sortAscending) results = results.reversed()
    return results
}
