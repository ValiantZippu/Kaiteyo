@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ua.syt0r.kanji.presentation.screen.main.screen.sentence

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.knowledge.AnnotatedToken
import ua.syt0r.kanji.core.knowledge.GrammarCatalog
import ua.syt0r.kanji.core.knowledge.SentenceDifficultyLevel
import ua.syt0r.kanji.core.knowledge.SentenceDifficultyTier
import ua.syt0r.kanji.core.knowledge.SentenceKnowledge
import ua.syt0r.kanji.core.knowledge.SentenceTokenKind
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import ua.syt0r.kanji.presentation.common.ui.FuriganaText
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionCard
import ua.syt0r.kanji.presentation.common.ui.KaiteyoTag
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.sentence.SentenceScreenContract.ScreenState

// ============================================================
// SENTENCE — SCREEN
// ------------------------------------------------------------
// The token-interactive sentence page (spec §26–§27). Search the
// bundled corpus, open a sentence, and read it token-by-token:
// every token that resolves to a dictionary entry is tappable,
// grammar matches are highlighted with their meaning, difficulty
// is a labelled surface estimate, and provenance is shown
// honestly (bundled corpus — never fabricated).
// ============================================================

@Composable
fun SentenceScreen(
    initialQuery: String = "",
    initialSentence: String? = null,
    onClose: () -> Unit,
    onOpenKanji: (String) -> Unit,
    onOpenWord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = getMultiplatformViewModel<SentenceScreenContract.ViewModel>()
    val state by viewModel.state.collectAsState()

    var query by remember { mutableStateOf(initialQuery) }

    LaunchedEffect(Unit) {
        when {
            initialSentence != null -> viewModel.openByText(initialSentence)
            initialQuery.isNotBlank() -> viewModel.search(initialQuery)
        }
    }

    val panel = when (state) {
        is ScreenState.Idle -> "Landing"
        is ScreenState.Explorer -> "Explorer"
        is ScreenState.Detail -> "Detail"
        is ScreenState.Error -> "Error"
    }

    ProvidePageIdentity(
        PageIdentity(id = "sentence", name = "Sentence", route = "/sentence", panel = panel)
    ) {
        Column(modifier.fillMaxSize()) {
            SentenceHeader(
                query = query,
                onQueryChange = {
                    query = it
                    viewModel.search(it)
                },
                onClear = {
                    query = ""
                    viewModel.search("")
                },
                onBack = onClose
            )

            when (val current = state) {
                is ScreenState.Idle -> LandingContent()
                is ScreenState.Explorer -> ExplorerContent(
                    state = current,
                    onOpen = { viewModel.open(it) }
                )
                is ScreenState.Detail -> DetailContent(
                    state = current,
                    onOpenKanji = onOpenKanji,
                    onOpenWord = onOpenWord,
                    onOpenSentence = { sentence -> viewModel.open(sentence) }
                )
                is ScreenState.Error -> KaiteyoEmptyState(
                    icon = "⚠️",
                    title = "Sentence unavailable",
                    message = current.message
                )
            }
        }
    }
}

// ============================================================
// HEADER
// ============================================================

@Composable
private fun SentenceHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = surfaceColors.textPrimary
            )
        }
        Text(
            text = "Sentences",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = surfaceColors.textPrimary
        )
        Spacer(Modifier.weight(1f))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.widthIn(min = 160.dp, max = 420.dp),
            placeholder = { Text("Search the corpus…", color = surfaceColors.textMuted) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = surfaceColors.textMuted)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = surfaceColors.textMuted)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* search runs on every change */ })
        )
    }
}

// ============================================================
// LANDING
// ============================================================

@Composable
private fun LandingContent() {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.Space4),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "文",
            fontSize = 72.sp,
            color = accent.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(Dimens.Space3))
        Text(
            text = "Sentence reader",
            style = MaterialTheme.typography.titleMedium,
            color = surfaceColors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(Dimens.Space1))
        Text(
            text = "Search the bundled corpus, open a sentence, and tap any token to look it up.",
            style = MaterialTheme.typography.bodyMedium,
            color = surfaceColors.textMuted
        )
    }
}

// ============================================================
// EXPLORER — corpus search results
// ============================================================

@Composable
private fun ExplorerContent(
    state: ScreenState.Explorer,
    onOpen: (SentenceKnowledge) -> Unit
) {
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        item {
            Text(
                text = "${state.results.size} sentences matching \"${state.query}\" — from the bundled corpus",
                style = MaterialTheme.typography.labelSmall,
                color = LocalSurfaceColors.current.textMuted,
                modifier = Modifier.padding(bottom = Dimens.Space1)
            )
        }
        if (state.results.isEmpty()) {
            item {
                KaiteyoEmptyState(
                    icon = "🔍",
                    title = "No sentences found",
                    message = "Nothing in the bundled corpus matched \"${state.query}\". Try a word, reading, or kanji."
                )
            }
        } else {
            items(state.results, key = { it.text }) { sentence ->
                SentenceRowCard(
                    sentence = sentence,
                    onClick = { onOpen(sentence) }
                )
            }
        }
    }
}

@Composable
private fun SentenceRowCard(
    sentence: SentenceKnowledge,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(surfaceColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2)
    ) {
        Text(sentence.text, style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary)
        Spacer(Modifier.height(2.dp))
        Text(sentence.translation, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.Space1),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            KaiteyoTag(
                text = sentence.provenance.sourceLabel.ifBlank { sentence.provenance.sourceType.label },
                tint = accent.secondary
            )
        }
    }
}

// ============================================================
// DETAIL — analyzed sentence
// ============================================================

@Composable
private fun DetailContent(
    state: ScreenState.Detail,
    onOpenKanji: (String) -> Unit,
    onOpenWord: (Long) -> Unit,
    onOpenSentence: (SentenceKnowledge) -> Unit
) {
    if (state.loading || state.analysis == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val analysis = state.analysis
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        // Hero: the sentence itself.
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusLg))
                    .background(surfaceColors.surface)
                    .padding(Dimens.Space5),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FuriganaText(
                    furiganaString = state.sentence.furigana,
                    textStyle = MaterialTheme.typography.headlineSmall,
                    color = surfaceColors.textPrimary
                )
                Spacer(Modifier.height(Dimens.Space2))
                Text(
                    text = state.sentence.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = surfaceColors.textSecondary
                )
                Spacer(Modifier.height(Dimens.Space3))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                    KaiteyoTag(text = difficultyLabel(analysis.difficulty), tint = difficultyColor(analysis.difficulty.tier))
                    KaiteyoTag(
                        text = state.sentence.provenance.sourceLabel.ifBlank { state.sentence.provenance.sourceType.label },
                        tint = accent.secondary
                    )
                }
            }
        }

        // Interactive tokens.
        item {
            KaiteyoSectionCard(
                title = "Tokens",
                subtitle = "Tap a token to look it up — unlinked tokens have no dictionary match"
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    analysis.tokens.forEach { annotated ->
                        TokenChip(
                            annotated = annotated,
                            onOpenKanji = onOpenKanji,
                            onOpenWord = onOpenWord
                        )
                    }
                }
            }
        }

        // Grammar found in the sentence.
        if (analysis.grammarMatches.isNotEmpty()) {
            item {
                KaiteyoSectionCard(
                    title = "Grammar",
                    subtitle = "Patterns matched from the built-in reference catalog — a hint, not a morphological parse"
                ) {
                    analysis.grammarMatches.forEach { match ->
                        val pattern = GrammarCatalog.byId(match.patternId)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                        ) {
                            KaiteyoTag(text = match.matchedText, tint = accent.secondary)
                            pattern?.let {
                                Text(
                                    text = it.meaning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = surfaceColors.textSecondary
                                )
                                it.jlpt?.let { level ->
                                    Text(
                                        text = "N$level",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = surfaceColors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Difficulty explainability.
        item {
            KaiteyoSectionCard(title = "Difficulty", subtitle = "Surface estimate — length, kanji density, grammar density") {
                analysis.difficulty.factors.forEach { factor ->
                    Text(
                        text = "·  $factor",
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaceColors.textSecondary,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        // Provenance (never fabricated).
        item {
            KaiteyoSectionCard(title = "Source") {
                val provenance = state.sentence.provenance
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KaiteyoTag(text = provenance.sourceType.label, tint = accent.primary)
                    Text(
                        text = provenance.sourceLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaceColors.textSecondary
                    )
                    Text(
                        text = provenance.confidence.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = surfaceColors.textMuted
                    )
                }
                Text(
                    text = "This sentence is bundled reference data shipped with the app. If a sentence was AI- or user-generated it would be labelled here — it never is for corpus data.",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.padding(top = Dimens.Space1)
                )
            }
        }

        // Related corpus sentences.
        if (state.related.isNotEmpty()) {
            item {
                Text(
                    text = "RELATED SENTENCES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = surfaceColors.textSecondary
                )
            }
            items(state.related, key = { it.text }) { sentence ->
                SentenceRowCard(sentence = sentence, onClick = { onOpenSentence(sentence) })
            }
        }
    }
}

// ============================================================
// TOKEN CHIP
// ============================================================

@Composable
private fun TokenChip(
    annotated: AnnotatedToken,
    onOpenKanji: (String) -> Unit,
    onOpenWord: (Long) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    // Resolve what a tap opens: whole-token word first, then the single
    // kanji, then the first kanji character inside a compound.
    val word = annotated.word
    val kanji = annotated.kanji
    val firstKanjiChar = annotated.kanjiCharacters.firstOrNull()
    val hasGrammar = annotated.grammar.isNotEmpty()

    val onClick: (() -> Unit)? = when {
        word != null -> { { onOpenWord(word.id) } }
        kanji != null -> { { onOpenKanji(kanji.character) } }
        firstKanjiChar != null -> { { onOpenKanji(firstKanjiChar.character) } }
        else -> null
    }

    val isKanjiLike = annotated.token.kind == SentenceTokenKind.Kanji ||
        annotated.token.kind == SentenceTokenKind.Mixed

    // Hover state for popup
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showPopup by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        if (isHovered && onClick != null) {
            kotlinx.coroutines.delay(400)
            showPopup = true
        } else {
            showPopup = false
        }
    }

    val chipModifier = Modifier
        .widthIn(min = 30.dp)
        .clip(RoundedCornerShape(Dimens.RadiusSm))
        .background(
            when {
                isHovered && onClick != null -> accent.primary.copy(alpha = 0.18f)
                hasGrammar -> accent.secondary.copy(alpha = 0.14f)
                isKanjiLike -> accent.primary.copy(alpha = 0.10f)
                else -> surfaceColors.surfaceInteractive
            }
        )
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .then(
            if (onClick != null) Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            else Modifier
        )

    // Hover popup showing definition
    if (showPopup && onClick != null) {
        Popup(
            onDismissRequest = { showPopup = false },
            properties = PopupProperties(focusable = false)
        ) {
            TokenHoverPopup(
                annotated = annotated,
                onOpen = {
                    showPopup = false
                    onClick()
                }
            )
        }
    }

    Column(
        modifier = chipModifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = annotated.token.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isKanjiLike) FontWeight.SemiBold else FontWeight.Normal,
            color = if (onClick != null) surfaceColors.textPrimary else surfaceColors.textMuted,
            textDecoration = if (hasGrammar) TextDecoration.Underline else TextDecoration.None
        )
        val reading = word?.kanaReading
        if (reading != null && reading != annotated.token.text) {
            Text(
                text = reading,
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TokenHoverPopup(
    annotated: AnnotatedToken,
    onOpen: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val word = annotated.word
    val kanji = annotated.kanji

    Column(
        modifier = Modifier
            .widthIn(min = 140.dp, max = 260.dp)
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(surfaceColors.surface)
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Headword
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = annotated.token.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = surfaceColors.textPrimary
            )
            val reading = word?.kanaReading
            if (reading != null && reading != annotated.token.text) {
                Text(
                    text = "  $reading",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent.secondary
                )
            }
        }

        // Definition
        if (word != null) {
            val glossary = word.glossary.joinToString(", ")
            if (glossary.isNotEmpty()) {
                Text(
                    text = glossary,
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textSecondary,
                    maxLines = 3
                )
            }
        } else if (kanji != null) {
            val meanings = kanji.meanings.joinToString(", ")
            if (meanings.isNotEmpty()) {
                Text(
                    text = meanings,
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textSecondary,
                    maxLines = 3
                )
            }
            val onReadings = kanji.onReadings.joinToString(" ")
            val kunReadings = kanji.kunReadings.joinToString(" ")
            if (onReadings.isNotEmpty() || kunReadings.isNotEmpty()) {
                Text(
                    text = listOfNotNull(
                        onReadings.ifBlank { null },
                        kunReadings.ifBlank { null }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.primary
                )
            }
        } else {
            val grammar = annotated.grammar.joinToString(", ") { it.patternId }
            if (grammar.isNotEmpty()) {
                Text(
                    text = grammar,
                    style = MaterialTheme.typography.bodySmall,
                    color = accent.secondary
                )
            }
        }

        // Tap hint
        Text(
            text = "Tap to open →",
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted
        )
    }
}

// ============================================================
// HELPERS
// ============================================================

private fun difficultyLabel(level: SentenceDifficultyLevel): String =
    "Difficulty ${level.level}/10 · ${level.tier.label}"

private fun difficultyColor(tier: SentenceDifficultyTier): Color = when (tier) {
    SentenceDifficultyTier.VeryEasy -> Color(0xFF66BB6A)
    SentenceDifficultyTier.Easy -> Color(0xFF81C784)
    SentenceDifficultyTier.Moderate -> Color(0xFFFFB74D)
    SentenceDifficultyTier.Hard -> Color(0xFFEF5350)
    SentenceDifficultyTier.VeryHard -> Color(0xFFB71C1C)
}
