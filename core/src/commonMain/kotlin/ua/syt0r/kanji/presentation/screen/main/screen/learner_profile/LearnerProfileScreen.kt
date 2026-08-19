package ua.syt0r.kanji.presentation.screen.main.screen.learner_profile

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.knowledge.ExplanationDepth
import ua.syt0r.kanji.core.knowledge.GraphComplexity
import ua.syt0r.kanji.core.knowledge.LearnerProfile
import ua.syt0r.kanji.core.knowledge.LearnerProfileCatalog
import ua.syt0r.kanji.core.knowledge.ProfilePresentation
import ua.syt0r.kanji.core.knowledge.SentenceDifficulty
import ua.syt0r.kanji.core.knowledge.cards.KanjiCardPresets
import ua.syt0r.kanji.presentation.common.ui.KaiteyoPill
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionCard
import ua.syt0r.kanji.presentation.common.ui.KaiteyoTag
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.learner_profile.LearnerProfileScreenContract.ScreenState

// ============================================================
// LEARNER PROFILE — SCREEN
// ------------------------------------------------------------
// The level-adaptation picker. Each profile sets presentation
// defaults — what a learner sees first (furigana, romaji,
// translations, rare readings, depth, sentence difficulty,
// graph complexity, card preset). Nothing is deleted: every
// profile only changes what is shown by default.
// ============================================================

@Composable
fun LearnerProfileScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = getMultiplatformViewModel<LearnerProfileScreenContract.ViewModel>()
    val state by viewModel.state.collectAsState()

    ProvidePageIdentity(
        PageIdentity(id = "learner_profile", name = "Learner profile", route = "/learner_profile", panel = null)
    ) {
        Column(modifier.fillMaxSize()) {
            ProfileHeader(onClose = onClose, onReset = viewModel::reset)
            if (!state.loaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                ProfileContent(
                    state = state,
                    onSelect = viewModel::select,
                    onUpdateCustom = viewModel::updateCustom
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(onClose: () -> Unit, onReset: () -> Unit) {
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
        Text(
            text = "Learner profile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = surfaceColors.textPrimary
        )
        Text(
            text = "Level adaptation",
            style = MaterialTheme.typography.labelMedium,
            color = surfaceColors.textMuted
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onReset) {
            Icon(
                Icons.Filled.RestartAlt,
                contentDescription = "Reset to default profile",
                tint = surfaceColors.textMuted
            )
        }
    }
}

@Composable
private fun ProfileContent(
    state: ScreenState,
    onSelect: (LearnerProfile) -> Unit,
    onUpdateCustom: (ProfilePresentation) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        item {
            Text(
                text = "Profiles adapt what you see — they never hide your data permanently. Switch at any time; nothing is lost.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalSurfaceColors.current.textMuted
            )
        }

        items(LearnerProfileCatalog.presets, key = { it.id }) { preset ->
            val profile = LearnerProfileCatalog.byId(preset.id) ?: return@items
            ProfileRow(
                preset = preset,
                selected = preset.id == state.selected.id,
                onClick = { onSelect(profile) }
            )
        }

        // The Custom profile gets a real editor — every control writes the
        // persisted ProfilePresentation (spec §23: nothing here deletes data,
        // it only changes what is shown by default).
        if (state.selected == LearnerProfile.Custom) {
            item {
                Spacer(Modifier.height(Dimens.Space1))
                CustomProfileEditor(
                    presentation = state.presentation,
                    onUpdate = onUpdateCustom
                )
            }
        }

        item {
            Spacer(Modifier.height(Dimens.Space2))
            Text(
                text = "WHAT THIS PROFILE SHOWS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = LocalSurfaceColors.current.textSecondary
            )
        }

        item {
            PresentationPreview(state.presentation)
        }
    }
}

// ============================================================
// CUSTOM PROFILE EDITOR
// ============================================================

@Composable
private fun CustomProfileEditor(
    presentation: ProfilePresentation,
    onUpdate: (ProfilePresentation) -> Unit
) {
    val accent = LocalKaiteyoAccent.current
    KaiteyoSectionCard(
        title = "Custom profile",
        subtitle = "Your own combination — every control applies and saves immediately"
    ) {
        EditorGroupLabel("Visibility")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            PillToggle(
                label = "Furigana",
                enabled = presentation.showFurigana,
                tint = accent.primary,
                onToggle = { onUpdate(presentation.copy(showFurigana = it)) }
            )
            PillToggle(
                label = "Romaji",
                enabled = presentation.showRomaji,
                tint = accent.primary,
                onToggle = { onUpdate(presentation.copy(showRomaji = it)) }
            )
            PillToggle(
                label = "Translations",
                enabled = presentation.showTranslations,
                tint = accent.primary,
                onToggle = { onUpdate(presentation.copy(showTranslations = it)) }
            )
            PillToggle(
                label = "Rare readings",
                enabled = presentation.showRareReadings,
                tint = accent.primary,
                onToggle = { onUpdate(presentation.copy(showRareReadings = it)) }
            )
            PillToggle(
                label = "Etymology",
                enabled = presentation.showEtymology,
                tint = accent.secondary,
                onToggle = { onUpdate(presentation.copy(showEtymology = it)) }
            )
        }

        EditorGroupLabel("Explanation depth")
        PillSelect(
            options = ExplanationDepth.entries.map { it.name to explanationLabel(it) },
            selected = presentation.explanationDepth.name,
            tint = accent.secondary,
            onSelect = { name ->
                val depth = ExplanationDepth.entries.firstOrNull { it.name == name } ?: return@PillSelect
                onUpdate(presentation.copy(explanationDepth = depth))
            }
        )

        EditorGroupLabel("Sentence difficulty")
        PillSelect(
            options = SentenceDifficulty.entries.map { it.name to sentenceLabel(it) },
            selected = presentation.sentenceDifficulty.name,
            tint = accent.secondary,
            onSelect = { name ->
                val difficulty = SentenceDifficulty.entries.firstOrNull { it.name == name } ?: return@PillSelect
                onUpdate(presentation.copy(sentenceDifficulty = difficulty))
            }
        )

        EditorGroupLabel("Graph complexity")
        PillSelect(
            options = GraphComplexity.entries.map { it.name to graphLabel(it) },
            selected = presentation.graphComplexity.name,
            tint = accent.secondary,
            onSelect = { name ->
                val complexity = GraphComplexity.entries.firstOrNull { it.name == name } ?: return@PillSelect
                onUpdate(presentation.copy(graphComplexity = complexity))
            }
        )

        EditorGroupLabel("Kanji-page card preset")
        PillSelect(
            options = KanjiCardPresets.all.map { it.id to it.name },
            selected = presentation.cardPresetId,
            tint = accent.secondary,
            onSelect = { id -> onUpdate(presentation.copy(cardPresetId = id)) }
        )

        Spacer(Modifier.height(Dimens.Space2))
        Text(
            text = "Custom overrides only apply while the Custom profile is active — switching profiles keeps your configuration saved.",
            style = MaterialTheme.typography.labelSmall,
            color = LocalSurfaceColors.current.textMuted
        )
    }
}

@Composable
private fun EditorGroupLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = LocalSurfaceColors.current.textSecondary,
        modifier = Modifier.padding(top = Dimens.Space3, bottom = Dimens.Space1)
    )
}

@Composable
private fun PillToggle(
    label: String,
    enabled: Boolean,
    tint: androidx.compose.ui.graphics.Color,
    onToggle: (Boolean) -> Unit
) {
    KaiteyoPill(
        text = "$label: ${onOff(enabled)}",
        selected = enabled,
        onClick = { onToggle(!enabled) }
    )
}

@Composable
private fun PillSelect(
    options: List<Pair<String, String>>,
    selected: String,
    tint: androidx.compose.ui.graphics.Color,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        options.forEach { (id, label) ->
            KaiteyoPill(
                text = label,
                selected = id == selected,
                onClick = { onSelect(id) }
            )
        }
    }
}

@Composable
private fun ProfileRow(
    preset: LearnerProfileCatalog.ProfilePreset,
    selected: Boolean,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(if (selected) accent.primary.copy(alpha = 0.12f) else surfaceColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = surfaceColors.textPrimary
            )
            Text(
                text = preset.description,
                style = MaterialTheme.typography.bodySmall,
                color = surfaceColors.textMuted
            )
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = accent.primary)
        }
    }
}

@Composable
private fun PresentationPreview(presentation: ProfilePresentation) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    KaiteyoSectionCard(title = "Presentation defaults", subtitle = "Applied across kanji, word, sentence and graph surfaces") {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            KaiteyoTag(text = "Furigana: ${onOff(presentation.showFurigana)}", tint = accent.primary)
            KaiteyoTag(text = "Romaji: ${onOff(presentation.showRomaji)}", tint = accent.primary)
            KaiteyoTag(text = "Translations: ${onOff(presentation.showTranslations)}", tint = accent.primary)
            KaiteyoTag(text = "Rare readings: ${onOff(presentation.showRareReadings)}", tint = accent.primary)
            KaiteyoTag(text = "Etymology: ${onOff(presentation.showEtymology)}", tint = accent.secondary)
            KaiteyoTag(text = "Depth: ${explanationLabel(presentation.explanationDepth)}", tint = accent.secondary)
            KaiteyoTag(text = "Sentences: ${sentenceLabel(presentation.sentenceDifficulty)}", tint = accent.secondary)
            KaiteyoTag(text = "Graph: ${graphLabel(presentation.graphComplexity)}", tint = accent.secondary)
            KaiteyoTag(text = "Cards: ${presentation.cardPresetId}", tint = accent.secondary)
        }
        Spacer(Modifier.height(Dimens.Space2))
        Text(
            text = "A profile only changes defaults — rare readings, etymology and advanced grammar remain available and can always be turned on per page.",
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted
        )
    }
}

private fun onOff(value: Boolean): String = if (value) "on" else "off"

private fun explanationLabel(depth: ExplanationDepth): String = when (depth) {
    ExplanationDepth.Simple -> "simple"
    ExplanationDepth.Clear -> "clear"
    ExplanationDepth.Technical -> "technical"
    ExplanationDepth.JapaneseOnly -> "Japanese"
}

private fun sentenceLabel(difficulty: SentenceDifficulty): String = when (difficulty) {
    SentenceDifficulty.Easy -> "easy"
    SentenceDifficulty.Mixed -> "mixed"
    SentenceDifficulty.Hard -> "hard"
}

private fun graphLabel(complexity: GraphComplexity): String = when (complexity) {
    GraphComplexity.Simple -> "simple"
    GraphComplexity.Standard -> "standard"
    GraphComplexity.Full -> "full"
}
