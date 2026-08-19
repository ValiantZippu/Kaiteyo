package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.app_data.Sentence
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// KANJIVERSE MINING — the sentence → words pipeline view.
//
// Mirrors the reference "mine a word" breakdown: a source
// sentence card with source tags, a translation card with an
// attribution line, then every vocabulary item of the sentence
// as an expandable breakdown list. Each word row can jump to
// its dictionary page or be bookmarked straight into a deck.
// ============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaiteyoSourceCard(
    sentence: Sentence,
    tags: List<String>,
    sourceLabel: String?,
    onFuriganaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    KaiteyoCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        // Tag row: source tags + spacer + speaker affordance
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { tag ->
                    KaiteyoBadge(
                        text = tag,
                        containerColor = accent.primary.copy(alpha = 0.12f),
                        contentColor = accent.primary
                    )
                }
            }
            Text("🔊", fontSize = 14.sp)
        }

        Spacer(Modifier.height(12.dp))

        // The sentence with clickable furigana
        KaiteyoFuriganaClickable(
            furigana = sentence.furigana,
            onFuriganaClick = onFuriganaClick,
            modifier = Modifier.fillMaxWidth()
        )

        if (sourceLabel != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#",
                    fontSize = 11.sp,
                    color = accent.primary
                )
                Text(
                    text = sourceLabel,
                    fontSize = 11.sp,
                    color = surfaceColors.textMuted
                )
            }
        }
    }
}

@Composable
fun KaiteyoTranslationCard(
    translation: String,
    attribution: String?,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    KaiteyoCard(
        modifier = modifier,
        header = "Translations",
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = translation,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = surfaceColors.textPrimary
        )
        if (attribution != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = attribution,
                fontSize = 10.sp,
                color = surfaceColors.textMuted
            )
        }
    }
}

@Composable
fun KaiteyoVocabBreakdown(
    words: List<JapaneseWord>,
    onWordClick: (JapaneseWord) -> Unit,
    onBookmarkClick: (JapaneseWord) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    KaiteyoCard(
        modifier = modifier,
        header = "Vocabulary Breakdown",
        subtitle = "${words.size} words in this sentence",
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        if (words.isEmpty()) {
            Text(
                "No vocabulary breakdown available for this sentence.",
                fontSize = 12.sp,
                color = surfaceColors.textMuted,
                modifier = Modifier.padding(12.dp)
            )
            return@KaiteyoCard
        }
        words.forEachIndexed { index, word ->
            if (index > 0) {
                KaiteyoDivider(Modifier.padding(horizontal = 12.dp))
            }
            KaiteyoVocabRow(
                word = word,
                onClick = { onWordClick(word) },
                onBookmarkClick = { onBookmarkClick(word) }
            )
        }
    }
}

/**
 * A compact pill that advertises the mining flow — "select a word
 * in a sentence to mine it" — used to introduce the breakdown.
 */
@Composable
fun KaiteyoMiningHint(
    text: String,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(KaiteyoCardShape)
            .background(accent.primary.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(accent.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text("⛏", fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = surfaceColors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
