@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ua.syt0r.kanji.presentation.screen.main.screen.knowledge_explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.knowledge.GrammarCatalog
import ua.syt0r.kanji.core.knowledge.KanjiKnowledge
import ua.syt0r.kanji.core.knowledge.findIn
import ua.syt0r.kanji.core.knowledge.StudyEntry
import ua.syt0r.kanji.core.knowledge.StudyStatusProvider
import ua.syt0r.kanji.core.knowledge.cards.WordCardLayout
import ua.syt0r.kanji.core.knowledge.cards.WordCardLayoutStore
import ua.syt0r.kanji.core.knowledge.cards.WordCardPresets
import ua.syt0r.kanji.core.knowledge.cards.WordCardType
import ua.syt0r.kanji.core.knowledge.home.HomeCommandCenterStore
import ua.syt0r.kanji.core.knowledge.home.RecentEntry
import ua.syt0r.kanji.core.knowledge.home.RecentEntryKind
import ua.syt0r.kanji.core.knowledge.media.MediaReference
import ua.syt0r.kanji.core.knowledge.media.MediaReferenceStore
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.common.ui.KaiteyoPill
import ua.syt0r.kanji.presentation.common.ui.KaiteyoTag
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.core.knowledge.LearnerProfileStore
import ua.syt0r.kanji.core.knowledge.SentenceKnowledge
import ua.syt0r.kanji.core.knowledge.WordKnowledge
import ua.syt0r.kanji.core.japanese.kanaToRomaji
import ua.syt0r.kanji.core.knowledge.level.DisplayOverridesStore
import ua.syt0r.kanji.core.knowledge.level.LevelAdapter
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.knowledge.studyStateColor
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionCard
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// WORD ENTRY — SCREEN
// ------------------------------------------------------------
// Standalone word page. WHAT (meanings, readings), HOW IT'S
// USED (parts of speech), WHAT IT'S CONNECTED TO (kanji inside
// the spelling, example sentences) — all real dictionary data.
// ============================================================


private sealed interface WordEntryUiState {
    data object Loading : WordEntryUiState
    data class Loaded(
        val word: WordKnowledge,
        val kanji: List<KanjiKnowledge>,
        val sentences: List<SentenceKnowledge>,
        /** Profile-adapted glossary (per the learner profile's depth). */
        val glossary: List<String>,
        val showTranslations: Boolean,
        val showFurigana: Boolean,
        /** Effective romaji visibility (profile default + user override). */
        val showRomaji: Boolean,
        /** Media references whose Japanese text contains this word. */
        val mediaReferences: List<MediaReference>
    ) : WordEntryUiState

    data class Error(val message: String) : WordEntryUiState
}

@Composable
fun WordEntryScreen(
    wordId: Long,
    onClose: () -> Unit,
    onOpenKanji: (String) -> Unit,
    onOpenSentence: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val knowledge = koinInject<KnowledgeRepository>()
    val profileStore = koinInject<LearnerProfileStore>()
    val displayOverrides = koinInject<DisplayOverridesStore>()
    val homeStore = koinInject<HomeCommandCenterStore>()
    val mediaReferenceStore = koinInject<MediaReferenceStore>()
    val layoutStore = koinInject<WordCardLayoutStore>()
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<WordEntryUiState>(WordEntryUiState.Loading) }
    var layout by remember { mutableStateOf(WordCardLayout()) }
    var showCustomize by remember { mutableStateOf(false) }
    var retryTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { layout = layoutStore.load() }

    LaunchedEffect(wordId, retryTick) {
        state = WordEntryUiState.Loading
        val word = knowledge.word(wordId)
        if (word == null) {
            state = WordEntryUiState.Error("Word #$wordId is not in the bundled dictionary data.")
            return@LaunchedEffect
        }
        // Record the visit so Home's "Recent entries" reflects real usage.
        homeStore.recordEntry(
            RecentEntry(
                kind = RecentEntryKind.Word,
                ref = wordId.toString(),
                label = word.displaySpelling,
                subtitle = word.kanaReading,
                recordedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
        // Profile adaptation (spec §23–§24): the learner profile controls how
        // many senses are shown, whether example translations/furigana appear,
        // and the difficulty of the example sentences. Data is filtered for
        // presentation, never deleted.
        val preference = profileStore.load()
        // User-level romaji override (spec §24) applies on top of the profile
        // default; null keeps the profile's own setting.
        val presentation = LevelAdapter.applyRomajiOverride(
            presentation = LevelAdapter.effectivePresentation(
                profile = preference.profile,
                overrides = preference.customPresentation
            ),
            romajiOverride = displayOverrides.load().override
        )
        val kanji = knowledge.searchKanjiOfWord(word)
        val sentences = LevelAdapter.adaptedSentences(
            sentences = knowledge.sentencesForWordReading(word, limit = 12),
            presentation = presentation,
            limit = 8
        )
        // Media references (spec §28): real occurrences of this word's
        // spelling/reading across the user's media library, recorded on
        // bookmark actions in the desktop Media Centre.
        val mediaReferences = buildList {
            addAll(mediaReferenceStore.matching(word.kanaReading, limit = 4))
            word.kanjiReading?.let { addAll(mediaReferenceStore.matching(it, limit = 4)) }
        }.distinctBy { it.text + it.title + it.timestampMs }.take(6)

        state = WordEntryUiState.Loaded(
            word = word,
            kanji = kanji,
            sentences = sentences,
            glossary = LevelAdapter.adaptedGlossary(word, presentation),
            showTranslations = LevelAdapter.showTranslations(presentation),
            showFurigana = LevelAdapter.showFurigana(presentation),
            showRomaji = LevelAdapter.showRomaji(presentation),
            mediaReferences = mediaReferences
        )
    }

    ProvidePageIdentity(
        PageIdentity(id = "word_entry", name = "Word entry", route = "/word/$wordId", panel = null)
    ) {
        Column(modifier.fillMaxSize()) {
            WordEntryHeader(
                wordId = wordId,
                onClose = onClose,
                showRomaji = (state as? WordEntryUiState.Loaded)?.showRomaji == true,
                onToggleRomaji = {
                    scope.launch {
                        val current = state as? WordEntryUiState.Loaded ?: return@launch
                        val next = !current.showRomaji
                        displayOverrides.setRomaji(next)
                        state = current.copy(showRomaji = next)
                    }
                },
                onCustomize = { showCustomize = true }
            )
            when (val current = state) {
                is WordEntryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is WordEntryUiState.Error -> KaiteyoEmptyState(
                    icon = "⚠️",
                    title = "Word unavailable",
                    message = current.message,
                    actionLabel = "Retry",
                    onAction = { retryTick++ }
                )
                is WordEntryUiState.Loaded -> WordEntryBody(
                    state = current,
                    layout = layout,
                    onOpenKanji = onOpenKanji,
                    onOpenSentence = onOpenSentence
                )
            }
        }
    }

    // Card customization (spec §20–§21): show/hide + presets, persisted in
    // the word-card layout store — the page is data, never hardcoded.
    if (showCustomize) {
        WordLayoutDialog(
            layout = layout,
            onDismiss = { showCustomize = false },
            onChange = { newLayout ->
                layout = newLayout
                scope.launch { layoutStore.save(newLayout) }
            }
        )
    }
}

@Composable
private fun WordEntryHeader(
    wordId: Long,
    onClose: () -> Unit,
    showRomaji: Boolean,
    onToggleRomaji: () -> Unit,
    onCustomize: () -> Unit
) {
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
        Text("Word", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        Text("entry", style = MaterialTheme.typography.labelMedium, color = surfaceColors.textMuted)
        Spacer(Modifier.weight(1f))
        // Per-user romaji override (spec §24): flips the effective value and
        // persists it — the profile default applies until the user overrides.
        KaiteyoPill(
            text = "Aa romaji",
            selected = showRomaji,
            onClick = onToggleRomaji
        )
        TextButton(onClick = onCustomize) {
            Text("Customize", style = MaterialTheme.typography.labelMedium, color = surfaceColors.textSecondary)
        }
    }
}

@Composable
private fun WordEntryBody(
    state: WordEntryUiState.Loaded,
    layout: WordCardLayout,
    onOpenKanji: (String) -> Unit,
    onOpenSentence: ((String, String) -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val word = state.word
    // Readings/Meanings live inside the hero block and are gated separately.
    val showReadings = layout.isVisible(WordCardType.Readings)
    val showMeanings = layout.isVisible(WordCardType.Meanings)
    // Grammar patterns found in the real example sentences.
    val grammarMatches = remember(state.sentences) {
        state.sentences.flatMap { GrammarCatalog.findIn(it.text) }
            .distinctBy { it.patternId + it.matchedText }
            .take(12)
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            // Responsive measure (KT-CARD-004, spec §22): capped + centered on
            // wide windows, full width on narrow ones — no clipping or overflow.
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1080.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
        ) {
            // The page renders in the user's saved card order (spec §20–§21).
            layout.visibleCards().forEach { type ->
            when (type) {
                WordCardType.Hero -> item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.RadiusLg))
                            .background(surfaceColors.surface)
                            .padding(Dimens.Space6),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(word.displaySpelling, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                        if (showReadings && word.kanjiReading != null) {
                            Text(word.kanaReading, style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textMuted)
                        }
                        if (showReadings && state.showRomaji && word.kanaReading.isNotBlank()) {
                            Text(
                                text = word.kanaReading.kanaToRomaji(),
                                style = MaterialTheme.typography.bodySmall,
                                color = surfaceColors.textMuted
                            )
                        }
                        if (showMeanings) {
                            Spacer(Modifier.height(Dimens.Space3))
                            Text(
                                text = state.glossary.joinToString("; "),
                                style = MaterialTheme.typography.bodyLarge,
                                color = surfaceColors.textSecondary
                            )
                            if (state.glossary.size < word.glossary.size) {
                                Text(
                                    text = "+${word.glossary.size - state.glossary.size} more senses hidden by your learner profile",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = surfaceColors.textMuted,
                                    modifier = Modifier.padding(top = Dimens.Space2)
                                )
                            }
                        }
                    }
                }

                WordCardType.PartOfSpeech -> if (word.partOfSpeech.isNotEmpty()) {
                    item {
                        KaiteyoSectionCard(title = "Parts of speech") {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                            ) {
                                word.partOfSpeech.forEach { pos ->
                                    Text(
                                        text = pos,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = surfaceColors.textSecondary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(Dimens.RadiusSm))
                                            .background(surfaceColors.surfaceInteractive)
                                            .padding(horizontal = Dimens.Space2, vertical = Dimens.Space1)
                                    )
                                }
                            }
                        }
                    }
                }

                WordCardType.Kanji -> if (state.kanji.isNotEmpty()) {
                    item {
                        Text(
                            text = "KANJI — ${state.kanji.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = surfaceColors.textSecondary,
                            modifier = Modifier.padding(top = Dimens.Space2)
                        )
                    }
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                        ) {
                            state.kanji.forEach { kanji ->
                                Column(
                                    modifier = Modifier
                                        .widthIn(min = 76.dp)
                                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                                        .background(surfaceColors.surface)
                                        .clickable { onOpenKanji(kanji.character) }
                                        .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(kanji.character, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                                    kanji.keyword?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted, maxLines = 1)
                                    }
                                    kanji.jlpt?.let { tag ->
                                        Text(tag.label, style = MaterialTheme.typography.labelSmall, color = accent.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                WordCardType.Media -> if (state.mediaReferences.isNotEmpty()) {
                    item {
                        KaiteyoSectionCard(title = "Found in your media", subtitle = "Bookmarked in your library") {
                            state.mediaReferences.forEach { ref ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = Dimens.Space1),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = ref.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = surfaceColors.textPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${ref.title} · ${mediaTimestamp(ref.timestampMs)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = surfaceColors.textMuted,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = ref.kind.name.lowercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent.primary
                                    )
                                }
                            }
                        }
                    }
                }

                WordCardType.Sentences -> if (state.sentences.isNotEmpty()) {
                    item {
                        Text(
                            text = "SENTENCES — ${state.sentences.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = surfaceColors.textSecondary,
                            modifier = Modifier.padding(top = Dimens.Space2)
                        )
                    }
                    items(state.sentences, key = { it.text }) { sentence ->
                        WordExampleRow(
                            sentence = sentence,
                            showTranslation = state.showTranslations,
                            onClick = if (onOpenSentence != null) {
                                { onOpenSentence(sentence.text, sentence.translation) }
                            } else null
                        )
                    }
                }

                WordCardType.Grammar -> if (grammarMatches.isNotEmpty()) {
                    item {
                        KaiteyoSectionCard(title = "Grammar", subtitle = "Patterns found in the example sentences") {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                            ) {
                                grammarMatches.forEach { match ->
                                    Text(
                                        text = match.matchedText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = surfaceColors.textSecondary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(Dimens.RadiusSm))
                                            .background(surfaceColors.surfaceInteractive)
                                            .padding(horizontal = Dimens.Space2, vertical = Dimens.Space1)
                                    )
                                }
                            }
                        }
                    }
                }

                WordCardType.Study -> item {
                    WordStudyCard(wordId = word.id)
                }

                // Frequency has no word-level data source yet — it stays in
                // the registry (data may arrive later) but renders nothing
                // today rather than a fake card.
                WordCardType.Readings, WordCardType.Meanings -> Unit
                WordCardType.Frequency -> Unit
            }
        }
    }
    }
}

// ============================================================
// WORD STUDY CARD (spec §15) — real flashcard SRS state
// ============================================================

@Composable
private fun WordStudyCard(wordId: Long) {
    val surfaceColors = LocalSurfaceColors.current
    val provider = koinInject<StudyStatusProvider>()
    var entry by remember { mutableStateOf<StudyEntry?>(null) }

    LaunchedEffect(wordId) { entry = provider.wordState(wordId) }

    KaiteyoSectionCard(title = "Study", subtitle = "Real flashcard state for this word") {
        val current = entry
        if (current == null) {
            Text(
                text = "Loading…",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted
            )
        } else if (current.isNew) {
            Text(
                text = "Not studied yet — add this word to a deck to start.",
                style = MaterialTheme.typography.bodySmall,
                color = surfaceColors.textSecondary
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                Text(
                    text = current.practiceLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = surfaceColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                KaiteyoTag(text = current.state.label, tint = studyStateColor(current.state))
            }
            Text(
                text = "Review scheduling comes from your real FSRS cards.",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted,
                modifier = Modifier.padding(top = Dimens.Space2)
            )
        }
    }
}


// ============================================================
// WORD CARD CUSTOMIZATION (spec §20–§21)
// ============================================================

@Composable
private fun WordLayoutDialog(
    layout: WordCardLayout,
    onDismiss: () -> Unit,
    onChange: (WordCardLayout) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    // Card types that render real content on a word page today. Frequency and
    // Study stay in the registry (data may arrive later) but are not listed
    // here — a checkbox must never be a ghost control.
    val configurable = listOf(
        WordCardType.Hero, WordCardType.Readings, WordCardType.Meanings,
        WordCardType.PartOfSpeech, WordCardType.Kanji, WordCardType.Media,
        WordCardType.Sentences, WordCardType.Grammar
    )

    MultiplatformDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize word page", fontWeight = FontWeight.SemiBold) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                // Presets — one tap replaces the layout.
                Text("Presets", style = MaterialTheme.typography.labelMedium, color = surfaceColors.textMuted)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    WordCardPresets.all.forEach { preset ->
                        val active = preset.layout.hidden == layout.hidden && preset.layout.order == layout.order
                        KaiteyoPill(
                            text = preset.name,
                            selected = active,
                            onClick = { onChange(preset.layout) }
                        )
                    }
                }

                androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = Dimens.Space2))

                // Show / hide per card.
                configurable.forEach { type ->
                    val visible = layout.isVisible(type)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChange(layout.setVisible(type, !visible)) }
                            .padding(vertical = Dimens.Space1),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = surfaceColors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (visible) "shown" else "hidden",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (visible) surfaceColors.textSecondary else surfaceColors.textMuted
                        )
                    }
                }

                Text(
                    text = "Drag reordering is roadmap (KT-CARD-004); order follows the saved layout.",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.padding(top = Dimens.Space1)
                )
            }
        },
        buttons = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun mediaTimestamp(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
private fun WordExampleRow(
    sentence: SentenceKnowledge,
    showTranslation: Boolean,
    onClick: (() -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = clickableModifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(surfaceColors.surface)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2)
    ) {
        Text(sentence.text, style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary)
        if (showTranslation) {
            Spacer(Modifier.height(2.dp))
            Text(sentence.translation, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
        }
    }
}
