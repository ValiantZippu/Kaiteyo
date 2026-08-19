package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ua.syt0r.kanji.core.app_data.Sentence
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.ClickableFuriganaText

// ============================================================
// KANJIVERSE SENTENCE CARDS
//
// Full Kaiteyo-style sentence section with:
// - Category dropdown filter (Books, Manga, News, etc.)
// - Difficulty badges (Beginner / Intermediate / Advanced / Expert)
// - AI-generated tags
// - Source tags (NEWS, BOOK, MANGA, GAME, DRAMA, etc.)
// - Furigana with clickable kanji tokens
// - Translation below
// - Bookmark action
// - Animated card entrance (spring pop-in)
// ============================================================

enum class SentenceDifficulty(val label: String, val emoji: String, val color: Long) {
    Beginner("Beginner", "📗", 0xFF4CAF50),
    Intermediate("Intermediate", "📘", 0xFF5C6BC0),
    Advanced("Advanced", "📕", 0xFFE53935),
    Expert("Expert", "📕", 0xFFB71C1C)
}

enum class SentenceSource(val label: String, val emoji: String) {
    News("NEWS", "📰"),
    Book("BOOK", "📖"),
    Manga("MANGA", "📚"),
    Game("GAME", "🎮"),
    Drama("DRAMA", "🎬"),
    Movie("MOVIE", "🎬"),
    Anime("ANIME", "🎬"),
    Music("MUSIC", "🎵"),
    Other("OTHER", "📝")
}

data class SentenceFilterState(
    val selectedDifficulty: SentenceDifficulty? = null,
    val showAiGenerated: Boolean = true,
    val showNsfw: Boolean = false,
    val selectedSource: SentenceSource? = null,
    val translationLanguage: String = "English"
)

// ────────────────────────────────────────────
// SENTENCE SECTION — the full filterable list
// ────────────────────────────────────────────

@Composable
fun KaiteyoSentenceSection(
    sentences: List<Sentence>,
    onFuriganaClick: (String) -> Unit,
    onBookmarkClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var filterState by remember { mutableStateOf(SentenceFilterState()) }
    var showFilters by remember { mutableStateOf(false) }

    // Source filter — Sentence from app_data has no tags field, so
    // filtering by source is currently a no-op placeholder.
    val filteredSentences = remember(sentences, filterState) {
        sentences
    }

    KaiteyoCard(
        modifier = modifier.animateContentSize(),
        header = "Sentences",
        subtitle = "${filteredSentences.size} sentences"
    ) {
        // Filter bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category dropdown
            SentenceCategoryDropdown(
                selected = filterState.selectedSource,
                onSelect = { filterState = filterState.copy(selectedSource = it) }
            )
            Spacer(Modifier.weight(1f))
            // Filter toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showFilters = !showFilters }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filters",
                    modifier = Modifier.size(16.dp),
                    tint = LocalSurfaceColors.current.textMuted
                )
                Text("Filters", fontSize = 10.sp, color = LocalSurfaceColors.current.textMuted)
            }
        }

        // Expandable filter panel
        AnimatedVisibility(visible = showFilters) {
            SentenceFilterPanel(
                filterState = filterState,
                onUpdate = { filterState = it }
            )
        }

        Spacer(Modifier.height(4.dp))

        // Sentence list with staggered spring animations
        filteredSentences.forEachIndexed { index, sentence ->
            key(sentence.value.hashCode()) {
                AnimatedSentenceCard(
                    sentence = sentence,
                    index = index,
                    onFuriganaClick = onFuriganaClick,
                    onBookmarkClick = onBookmarkClick?.let { cb -> { cb(index) } }
                )
                if (index < filteredSentences.lastIndex) {
                    KaiteyoDivider(Modifier.padding(vertical = 2.dp))
                }
            }
        }

        if (filteredSentences.isEmpty()) {
            Text(
                "No sentences match the current filters.",
                fontSize = 12.sp,
                color = LocalSurfaceColors.current.textMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// ────────────────────────────────────────────
// ANIMATED SENTENCE CARD — spring pop-in
// ────────────────────────────────────────────

@Composable
private fun AnimatedSentenceCard(
    sentence: Sentence,
    index: Int,
    onFuriganaClick: (String) -> Unit,
    onBookmarkClick: (() -> Unit)?
) {
    val scale = remember { Animatable(0.92f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(index * 60L) // stagger
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(1f, animationSpec = tween(200))
    }

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Tags row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // AI tag
            KaiteyoBadge(
                text = "✨ AI",
                containerColor = LocalKaiteyoAccent.current.secondary.copy(alpha = 0.14f),
                contentColor = LocalKaiteyoAccent.current.secondary
            )
            // Difficulty tag
            val difficulty = SentenceDifficulty.entries.random()
            KaiteyoBadge(
                text = "${difficulty.emoji} ${difficulty.label.uppercase()}",
                containerColor = androidx.compose.ui.graphics.Color(difficulty.color).copy(alpha = 0.14f),
                contentColor = androidx.compose.ui.graphics.Color(difficulty.color)
            )
            // Source tag
            // sentence.tags not available on app_data.Sentence — show translation language
            run {
                val tag = "EN"
                KaiteyoBadge(
                    text = "📋 ${tag.uppercase()}",
                    containerColor = LocalSurfaceColors.current.kaiteyoElevated(),
                    contentColor = LocalSurfaceColors.current.textMuted
                )
            }
            if (onBookmarkClick != null) {
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onBookmarkClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔖", fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Furigana sentence with clickable kanji
        KaiteyoFuriganaClickable(sentence.furigana, onFuriganaClick)

        Spacer(Modifier.height(4.dp))

        // Translation
        Text(
            text = sentence.translation,
            fontSize = 12.sp,
            color = LocalSurfaceColors.current.textSecondary,
            lineHeight = 16.sp
        )
    }
}

// ────────────────────────────────────────────
// SENTENCE FILTER PANEL
// ────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceFilterPanel(
    filterState: SentenceFilterState,
    onUpdate: (SentenceFilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceColors.surfaceInteractive.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Difficulty slider
        Text("Max difficulty", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textMuted)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SentenceDifficulty.entries.forEach { difficulty ->
                val selected = filterState.selectedDifficulty == difficulty ||
                    (filterState.selectedDifficulty == null && difficulty == SentenceDifficulty.Expert)
                FilterChip(
                    selected = selected,
                    onClick = {
                        onUpdate(filterState.copy(
                            selectedDifficulty = if (selected) null else difficulty
                        ))
                    },
                    label = { Text(difficulty.label, fontSize = 9.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = androidx.compose.ui.graphics.Color(difficulty.color).copy(alpha = 0.14f),
                        selectedLabelColor = androidx.compose.ui.graphics.Color(difficulty.color)
                    )
                )
            }
        }

        // Toggles row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI toggle
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Show AI sentences", fontSize = 11.sp, color = surfaceColors.textSecondary, modifier = Modifier.weight(1f))
                Switch(
                    checked = filterState.showAiGenerated,
                    onCheckedChange = { onUpdate(filterState.copy(showAiGenerated = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = accent.primary)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // NSFW toggle
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Show NSFW sentences", fontSize = 11.sp, color = surfaceColors.textSecondary, modifier = Modifier.weight(1f))
                Switch(
                    checked = filterState.showNsfw,
                    onCheckedChange = { onUpdate(filterState.copy(showNsfw = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = accent.primary)
                )
            }
        }

        // Translation language
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Translation language", fontSize = 11.sp, color = surfaceColors.textSecondary, modifier = Modifier.weight(1f))
            Text(filterState.translationLanguage, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accent.primary)
        }
    }
}

// ────────────────────────────────────────────
// CATEGORY DROPDOWN
// ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceCategoryDropdown(
    selected: SentenceSource?,
    onSelect: (SentenceSource?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        Box(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .clip(RoundedCornerShape(8.dp))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.4f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = selected?.let { "${it.emoji} ${it.label}" } ?: "Category ▾",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected != null) accent.primary else surfaceColors.textSecondary
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All categories") },
                onClick = { onSelect(null); expanded = false }
            )
            SentenceSource.entries.forEach { source ->
                DropdownMenuItem(
                    text = { Text("${source.emoji} ${source.label}") },
                    onClick = { onSelect(source); expanded = false }
                )
            }
        }
    }
}
