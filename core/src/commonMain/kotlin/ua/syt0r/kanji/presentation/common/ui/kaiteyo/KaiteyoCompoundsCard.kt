package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.core.app_data.data.formattedFurigana
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.FuriganaText

// ============================================================
// KANJIVERSE COMPOUNDS CARD
//
// Shows words related to a kanji:
// - Words starting with this kanji
// - Words containing this kanji
// Each word shows: furigana reading, POS tags, definition, bookmark
// Words are clickable → navigate to that word's detail page
// Spring-animated card entrance
// ============================================================

data class CompoundGroup(
    val label: String,
    val words: List<JapaneseWord>
)

@Composable
fun KaiteyoCompoundsCard(
    character: String,
    groups: List<CompoundGroup>,
    onWordClick: (JapaneseWord) -> Unit,
    onBookmarkClick: ((JapaneseWord) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (groups.isEmpty() || groups.all { it.words.isEmpty() }) return

    KaiteyoCard(
        modifier = modifier,
        header = "Compounds",
        subtitle = "Words related to $character"
    ) {
        groups.forEach { group ->
            if (group.words.isNotEmpty()) {
                CompoundGroupSection(
                    label = group.label,
                    character = character,
                    words = group.words,
                    onWordClick = onWordClick,
                    onBookmarkClick = onBookmarkClick
                )
            }
        }
    }
}

@Composable
private fun CompoundGroupSection(
    label: String,
    character: String,
    words: List<JapaneseWord>,
    onWordClick: (JapaneseWord) -> Unit,
    onBookmarkClick: ((JapaneseWord) -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    // Collapsed header — tap to expand
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Words starting with $character",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = surfaceColors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = surfaceColors.textMuted
        )
    }

    // Expanded word list with staggered spring entrance
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(spring(stiffness = Spring.StiffnessLow)),
        exit = shrinkVertically(tween(150))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            words.take(15).forEachIndexed { index, word ->
                CompoundWordRow(
                    word = word,
                    highlightChar = character,
                    index = index,
                    onClick = { onWordClick(word) },
                    onBookmark = onBookmarkClick?.let { cb -> { cb(word) } }
                )
            }
            if (words.size > 15) {
                Text(
                    "… and ${words.size - 15} more",
                    fontSize = 10.sp,
                    color = accent.primary,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CompoundWordRow(
    word: JapaneseWord,
    highlightChar: String,
    index: Int,
    onClick: () -> Unit,
    onBookmark: (() -> Unit)?
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    // Staggered spring animation
    val scale = remember { Animatable(0.95f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(index * 40L)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        alpha.animateTo(1f, animationSpec = tween(150))
    }

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .fillMaxWidth()
            .clip(KaiteyoCardShape)
            .background(
                if (hovered) surfaceColors.surfaceInteractive.copy(alpha = 0.4f)
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kanji pill with highlighting
        val reading = word.reading
        Column(modifier = Modifier.width(100.dp)) {
            FuriganaText(
                furiganaString = reading.formattedFurigana(),
                color = surfaceColors.textMuted,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 9.sp),
                annotationTextStyle = androidx.compose.ui.text.TextStyle(fontSize = 8.sp, color = surfaceColors.textMuted)
            )
            Text(
                text = reading.kanjiReading ?: reading.kanaReading,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = surfaceColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(10.dp))

        // POS + definition
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (word.partOfSpeechList.isNotEmpty()) {
                Text(
                    text = word.partOfSpeechList.joinToString(" / "),
                    fontSize = 9.sp,
                    color = accent.primary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = word.combinedGlossary(),
                fontSize = 11.sp,
                color = surfaceColors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
        }

        // Bookmark
        if (onBookmark != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onBookmark),
                contentAlignment = Alignment.Center
            ) {
                Text("🔖", fontSize = 13.sp)
            }
        }
    }
}
