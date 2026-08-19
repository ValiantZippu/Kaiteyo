@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ua.syt0r.kanji.presentation.screen.main.screen.sentence_entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.knowledge.AnnotatedToken
import ua.syt0r.kanji.core.knowledge.SentenceAnalysis
import ua.syt0r.kanji.core.knowledge.SentenceDifficultyLevel
import ua.syt0r.kanji.core.knowledge.SentenceDifficultyTier
import ua.syt0r.kanji.core.knowledge.SentenceTokenKind
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionCard
import ua.syt0r.kanji.presentation.common.ui.KaiteyoTag
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.sentence_entry.SentenceEntryContract.ScreenState

@Composable
fun SentenceEntryScreen(
    sentence: String,
    translation: String,
    onClose: () -> Unit,
    onOpenKanji: (String) -> Unit,
    onOpenWord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = getMultiplatformViewModel<SentenceEntryContract.ViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sentence) { viewModel.load(sentence, translation) }

    ProvidePageIdentity(
        PageIdentity(id = "sentence_entry", name = "Sentence", route = "/sentence", panel = null)
    ) {
        Column(modifier.fillMaxSize()) {
            Header(onClose = onClose)
            when (val current = state) {
                is ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ScreenState.Error -> KaiteyoEmptyState(
                    icon = "⚠️",
                    title = "Sentence unavailable",
                    message = current.message,
                    actionLabel = "Retry",
                    onAction = viewModel::retry
                )
                is ScreenState.Loaded -> LoadedContent(
                    analysis = current.analysis,
                    translation = current.translation,
                    showTranslation = current.showTranslation,
                    showFurigana = current.showFurigana,
                    onOpenKanji = onOpenKanji,
                    onOpenWord = onOpenWord
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
        Text("Sentence", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        Text("Interactive analysis", style = MaterialTheme.typography.labelMedium, color = surfaceColors.textMuted)
    }
}

@Composable
private fun LoadedContent(
    analysis: SentenceAnalysis,
    translation: String,
    showTranslation: Boolean,
    showFurigana: Boolean,
    onOpenKanji: (String) -> Unit,
    onOpenWord: (Long) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        item {
            KaiteyoSectionCard(title = "Sentence", subtitle = "Tap any token to look it up") {
                TokenFlow(
                    tokens = analysis.tokens,
                    showFurigana = showFurigana,
                    onOpenKanji = onOpenKanji,
                    onOpenWord = onOpenWord
                )
                // Profile-driven translation visibility (spec §24): a profile
                // that hides translations keeps the sentence fully readable;
                // the translation is presentation-hidden, never deleted.
                if (showTranslation && translation.isNotBlank()) {
                    Spacer(Modifier.height(Dimens.Space3))
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = surfaceColors.textSecondary
                    )
                }
            }
        }
        item {
            DifficultyCard(difficulty = analysis.difficulty)
        }
        if (analysis.grammarMatches.isNotEmpty()) {
            item {
                GrammarCard(analysis = analysis)
            }
        }
        item {
            VocabularyCard(analysis = analysis, onOpenWord = onOpenWord)
        }
    }
}

// ============================================================
// INTERACTIVE TOKENS
// ============================================================

@Composable
private fun TokenFlow(
    tokens: List<AnnotatedToken>,
    showFurigana: Boolean,
    onOpenKanji: (String) -> Unit,
    onOpenWord: (Long) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tokens.forEach { annotated ->
            val token = annotated.token
            when (token.kind) {
                SentenceTokenKind.Punctuation -> Text(
                    text = token.text,
                    fontSize = 22.sp,
                    color = surfaceColors.textMuted
                )

                else -> InteractiveToken(
                    annotated = annotated,
                    showFurigana = showFurigana,
                    onOpenKanji = onOpenKanji,
                    onOpenWord = onOpenWord
                )
            }
        }
    }
}

@Composable
private fun InteractiveToken(
    annotated: AnnotatedToken,
    showFurigana: Boolean,
    onOpenKanji: (String) -> Unit,
    onOpenWord: (Long) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val token = annotated.token
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val clickable = when {
        annotated.kanji != null -> Modifier.clickable { onOpenKanji(annotated.kanji.character) }
        annotated.word != null -> Modifier.clickable { onOpenWord(annotated.word.id) }
        else -> Modifier
    }

    val background = when {
        hovered -> surfaceColors.surfaceInteractive
        annotated.grammar.isNotEmpty() -> accent.primary.copy(alpha = 0.10f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .background(background)
            .hoverable(interactionSource)
            .then(clickable)
            .padding(horizontal = 3.dp, vertical = 1.dp)
    ) {
        Text(
            text = token.text,
            fontSize = 22.sp,
            color = surfaceColors.textPrimary
        )
        // Small under-token gloss: word reading or kanji keyword. Honoring the
        // profile's furigana preference (spec §24) — hidden, never deleted.
        val gloss = when {
            !showFurigana -> ""
            annotated.word != null -> annotated.word.kanaReading
            annotated.kanji != null -> annotated.kanji.keyword ?: ""
            else -> ""
        }
        if (gloss.isNotEmpty()) {
            Text(
                text = gloss,
                fontSize = 10.sp,
                color = surfaceColors.textMuted,
                maxLines = 1
            )
        }
    }
}

// ============================================================
// DIFFICULTY
// ============================================================

@Composable
private fun DifficultyCard(difficulty: SentenceDifficultyLevel) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    KaiteyoSectionCard(title = "Difficulty", subtitle = "Surface-feature estimate") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
        ) {
            KaiteyoTag(text = "${difficulty.level}/10", tint = difficultyTint(difficulty, accent.primary, accent.secondary))
            Text(
                text = difficulty.tier.label,
                style = MaterialTheme.typography.bodyMedium,
                color = surfaceColors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (difficulty.factors.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.Space2))
            Text(
                text = difficulty.factors.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted
            )
        }
    }
}

@Composable
private fun difficultyTint(level: SentenceDifficultyLevel, low: androidx.compose.ui.graphics.Color, high: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color =
    when (level.tier) {
        SentenceDifficultyTier.VeryEasy -> androidx.compose.ui.graphics.Color(0xFF66BB6A)
        SentenceDifficultyTier.Easy -> androidx.compose.ui.graphics.Color(0xFF9CCC65)
        SentenceDifficultyTier.Moderate -> low
        SentenceDifficultyTier.Hard -> high
        SentenceDifficultyTier.VeryHard -> androidx.compose.ui.graphics.Color(0xFFEF5350)
    }

// ============================================================
// GRAMMAR HIGHLIGHTS
// ============================================================

@Composable
private fun GrammarCard(analysis: SentenceAnalysis) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    KaiteyoSectionCard(title = "Grammar patterns", subtitle = "Built-in reference catalog") {
        Text(
            text = analysis.sentence,
            style = MaterialTheme.typography.bodyMedium,
            color = surfaceColors.textPrimary
        )
        Spacer(Modifier.height(Dimens.Space3))
        analysis.grammarMatches.forEach { match ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
            ) {
                KaiteyoTag(text = match.matchedText, tint = accent.secondary)
                Text(
                    text = match.patternId,
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================================
// VOCABULARY
// ============================================================

@Composable
private fun VocabularyCard(
    analysis: SentenceAnalysis,
    onOpenWord: (Long) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val words = analysis.tokens.mapNotNull { it.word }.distinctBy { it.id }
    if (words.isEmpty()) return
    KaiteyoSectionCard(title = "Vocabulary in this sentence", subtitle = "${words.size} tokens linked") {
        words.forEach { word ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .clickable { onOpenWord(word.id) }
                    .padding(horizontal = Dimens.Space2, vertical = Dimens.Space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
            ) {
                Text(word.displaySpelling, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
                Text(word.kanaReading, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
                Text(
                    text = word.combinedGlossary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textSecondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }
    }
}
