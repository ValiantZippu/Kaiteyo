package ua.syt0r.kanji.desktop.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsChip
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsFlagBadge
import ua.syt0r.kanji.desktop.designsystem.DsNumericField
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTagChip
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.review.ReviewFilterPreset
import ua.syt0r.kanji.desktop.engine.review.ReviewSettings
import ua.syt0r.kanji.desktop.engine.review.ReviewSession
import ua.syt0r.kanji.desktop.engine.srs.SrsScheduler
import ua.syt0r.kanji.desktop.engine.srs.intervalDaysToDuration
import ua.syt0r.kanji.desktop.engine.srs.toLike
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.model.StudyMode

// ============================================
// REVIEW
// Full review screen: grading, reveal, controls
// (bury/suspend/undo/retry/forget/reschedule),
// session progress and post-session summary.
// ============================================

@Composable
fun ReviewView(state: AppState) {
    val session = state.reviewSession
    if (session == null) {
        ReviewLaunchPanel(state)
    } else {
        SessionPanel(state, session)
    }
}

// ============================================
// LAUNCH / SETTINGS PANEL
// ============================================

@Composable
private fun ReviewLaunchPanel(state: AppState) {
    val sc = surfaceColors()
    var selectedPreset by remember { mutableStateOf(ReviewFilterPreset.DueToday) }
    var customQuery by remember { mutableStateOf("") }
    var settings by remember { mutableStateOf(state.reviewSettings) }

    val last = state.lastSessionStats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        if (last != null) {
            DsCard(elevated = true) {
                Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    Text("Last session", color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                        SummaryStat("Total", last.total.toString(), Modifier.weight(1f))
                        SummaryStat("Again", last.again.toString(), Modifier.weight(1f))
                        SummaryStat("Hard", last.hard.toString(), Modifier.weight(1f))
                        SummaryStat("Good", last.good.toString(), Modifier.weight(1f))
                        SummaryStat("Easy", last.easy.toString(), Modifier.weight(1f))
                        SummaryStat("Accuracy", "${(last.accuracy * 100).toInt()}%", Modifier.weight(1f))
                    }
                }
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Review queue", color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReviewFilterPreset.entries.forEach { preset ->
                        DsChip(
                            text = preset.label,
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("Settings", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.Medium)
                DsToggle(
                    checked = settings.includeNew,
                    onCheckedChange = { settings = settings.copy(includeNew = it) },
                    label = "Include new cards"
                )
                DsToggle(
                    checked = settings.shuffle,
                    onCheckedChange = { settings = settings.copy(shuffle = it) },
                    label = "Shuffle queue"
                )
                DsToggle(
                    checked = settings.autoAdvance,
                    onCheckedChange = { settings = settings.copy(autoAdvance = it) },
                    label = "Auto-advance after grading"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
                    Text("New limit", color = sc.textSecondary, fontSize = DsType.Body)
                    DsNumericField(
                        value = settings.newLimit,
                        onValueChange = { settings = settings.copy(newLimit = it) },
                        modifier = Modifier.width(120.dp)
                    )
                    Text("Review limit", color = sc.textSecondary, fontSize = DsType.Body)
                    DsNumericField(
                        value = settings.reviewLimit,
                        onValueChange = { settings = settings.copy(reviewLimit = it) },
                        modifier = Modifier.width(120.dp)
                    )
                }

                Spacer(Modifier.height(DsSpacing.Sm))
                DsButton(
                    text = "Start ${selectedPreset.label}",
                    icon = Icons.Default.PlayArrow,
                    onClick = {
                        state.reviewSettings = settings
                        state.startReview(
                            query = if (selectedPreset == ReviewFilterPreset.All) "" else selectedPreset.query,
                            settings = settings
                        )
                    }
                )
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Custom search review", color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Run a filtered review from any search expression.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
                ua.syt0r.kanji.desktop.designsystem.DsSearchField(
                    value = customQuery,
                    onValueChange = { customQuery = it },
                    placeholder = "tag:jlpt-n4 flag:red interval:>30 …"
                )
                DsButton(
                    text = "Start custom review",
                    icon = Icons.Default.PlayArrow,
                    onClick = { state.startReview(query = customQuery, settings = settings) },
                    kind = DsButtonKind.Secondary
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val ac = accent()
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = ac.primary, fontSize = DsType.Heading, fontWeight = FontWeight.Bold)
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
    }
}

// ============================================
// LIVE SESSION
// ============================================

@Composable
private fun SessionPanel(state: AppState, session: ReviewSession) {
    val sc = surfaceColors()
    val ac = accent()
    val card = session.current()?.card
    var rescheduleDialog by remember { mutableStateOf(false) }

    if (card == null) {
        return
    }

    val revealed = state.answerRevealed
    val stats = session.sessionStats()
    val progress = if (session.total == 0) 0f else session.currentIndex.toFloat() / session.total

    // Mode-aware presentation: each study lane asks a different question.
    val mode = state.libraryActiveMode
    val frontText = when (mode) {
        StudyMode.Recognition -> card.meaning
        StudyMode.Cloze -> "＿＿＿"
        StudyMode.Pattern -> card.character
        else -> card.character
    }
    val frontSize = when (mode) {
        StudyMode.Recognition -> 36.sp
        StudyMode.Cloze -> 72.sp
        StudyMode.Pattern -> 72.sp
        else -> 120.sp
    }
    val frontSubtitle = when (mode) {
        StudyMode.Recognition -> "Which form does this meaning belong to?"
        StudyMode.Cloze -> "Complete the pattern: ${card.meaning}"
        StudyMode.Pattern -> "Explain this pattern in your own words, then reveal the answer."
        else -> null
    }
    val revealLabel = when (mode) {
        StudyMode.Cloze -> "Reveal pattern"
        StudyMode.Pattern -> "Reveal explanation"
        else -> "Show Answer"
    }

    Column(Modifier.fillMaxSize()) {
        // Progress header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Text(
                text = "${session.currentIndex + 1} / ${session.total}",
                color = sc.textPrimary,
                fontSize = DsType.Title,
                fontWeight = FontWeight.Bold
            )
            DsProgressBar(
                fraction = progress,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "A${stats.again} H${stats.hard} G${stats.good} E${stats.easy}",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Xl),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
            ) {
                // Front of the card — varies by study mode
                Text(
                    text = frontText,
                    color = sc.textPrimary,
                    fontSize = frontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (frontSubtitle != null) {
                    Text(
                        text = frontSubtitle,
                        color = sc.textMuted,
                        fontSize = DsType.Body,
                        textAlign = TextAlign.Center
                    )
                }
                if (mode != null) {
                    Text(
                        text = "${mode.label} — ${mode.hint}",
                        color = accent().primary,
                        fontSize = DsType.Caption,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!revealed) {
                    DsButton(
                        text = revealLabel,
                        icon = Icons.Default.Visibility,
                        onClick = { state.answerRevealed = true },
                        kind = DsButtonKind.Primary
                    )
                    Text(
                        text = "press Space to reveal",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                } else {
                    RevealedContent(state, card, mode)
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            if (revealed) {
                GradingRow(state, card)
                Spacer(Modifier.height(DsSpacing.Xs))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SessionControl(state, "Bury", Icons.Default.Schedule, "B") { state.buryCurrent() }
                SessionControl(state, "Suspend", Icons.Default.Block, "S") { state.suspendCurrent() }
                SessionControl(state, "Skip", Icons.Default.SkipNext, "Ctrl⏎") { state.skipCurrent() }
                SessionControl(state, "Undo", Icons.Default.History, "Ctrl Z", enabled = session.currentIndex > 0) { state.undoLast() }
                SessionControl(state, "Retry", Icons.Default.Refresh, "R") { state.retryCurrent() }
                SessionControl(state, "Forget", Icons.Default.DeleteForever, "") { state.forgetCurrent() }
                SessionControl(state, "Reschedule", Icons.Default.Schedule, "") { rescheduleDialog = true }
                SessionControl(state, "Edit", Icons.Default.Edit, "") { state.openEditor(card) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "1 Again · 2 Hard · 3 Good · 4 Easy · Space reveal",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
    }

    if (rescheduleDialog) {
        DsDialog(title = "Reschedule card", onDismiss = { rescheduleDialog = false }) {
            var days by remember { mutableStateOf(1) }
            Text(
                text = "Move ${card.character} to a custom due date.",
                color = sc.textSecondary,
                fontSize = DsType.Body
            )
            Spacer(Modifier.height(DsSpacing.Lg))
            DsNumericField(value = days, onValueChange = { days = it }, label = "Days from now")
            Spacer(Modifier.height(DsSpacing.Xl))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)) {
                DsButton(text = "Cancel", kind = DsButtonKind.Ghost, onClick = { rescheduleDialog = false })
                DsButton(text = "Apply", onClick = {
                    rescheduleDialog = false
                    state.rescheduleCurrent(days)
                })
            }
        }
    }
}

@Composable
private fun RevealedContent(state: AppState, card: DesktopCard, mode: StudyMode?) {
    val sc = surfaceColors()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        if (mode == StudyMode.Cloze) {
            Text(
                text = "The pattern",
                color = sc.textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = card.character,
                color = accent().primary,
                fontSize = DsType.Display,
                fontWeight = FontWeight.Bold
            )
        }
        if (mode == StudyMode.Recognition) {
            Text(
                text = card.character,
                color = sc.textPrimary,
                fontSize = DsType.Display,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = card.meaning,
            color = sc.textPrimary,
            fontSize = DsType.Heading,
            fontWeight = FontWeight.SemiBold
        )
        if (mode == StudyMode.Pattern && card.note.isNotBlank()) {
            Text(
                text = card.note,
                color = sc.textSecondary,
                fontSize = DsType.Body,
                textAlign = TextAlign.Center
            )
        }
        if (card.onReadings.isNotEmpty()) {
            Text(
                text = card.onReadings.joinToString("・"),
                color = accent().primary,
                fontSize = DsType.BodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        if (card.kunReadings.isNotEmpty()) {
            Text(
                text = card.kunReadings.joinToString("・"),
                color = sc.textSecondary,
                fontSize = DsType.BodyLarge
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsBadge(text = "JLPT N${card.jlpt ?: "?"}", tint = accent().primary)
            DsBadge(text = "${card.strokeCount} strokes", tint = Color(0xFF7BC8FF))
            card.flags.forEach { flag ->
                DsFlagBadge(label = flag, colorHex = "#FF6B6B")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            card.tags.take(4).forEach { tag ->
                DsTagChip(label = tag, colorHex = "#7BC8FF")
            }
        }
    }
}

@Composable
private fun GradingRow(state: AppState, card: DesktopCard) {
    val ratings = ReviewRating.entries
    val next = ratings.map { rating ->
        rating to SrsScheduler.schedule(
            currentStatus = card.status.toLike(),
            currentInterval = card.intervalDays,
            currentEase = card.ease,
            lapses = card.lapses,
            learningSteps = 0,
            rating = rating.toLike()
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        next.forEach { (rating, result) ->
            val duration = intervalDaysToDuration(result.intervalDays)
            val label = when {
                duration.inWholeDays >= 1 -> "${duration.inWholeDays}d"
                duration.inWholeHours >= 1 -> "${duration.inWholeHours}h"
                else -> "${duration.inWholeMinutes}m"
            }
            DsButton(
                text = "${rating.displayName} · $label",
                onClick = { state.rateCurrent(rating) },
                modifier = Modifier.weight(1f),
                kind = when (rating) {
                    ReviewRating.Again -> DsButtonKind.Danger
                    ReviewRating.Hard -> DsButtonKind.Secondary
                    ReviewRating.Good -> DsButtonKind.Primary
                    ReviewRating.Easy -> DsButtonKind.AccentTint
                }
            )
        }
    }
}

@Composable
private fun SessionControl(
    state: AppState,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    hint: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(if (hovered && enabled) sc.surfaceInteractive else Color.Transparent)
            .then(
                if (enabled) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick).hoverable(interaction)
                else Modifier
            )
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) if (hovered) ac.primary else sc.textSecondary else sc.textMuted,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (enabled) sc.textSecondary else sc.textMuted,
            fontSize = DsType.Caption
        )
        if (hint.isNotBlank()) {
            Text(
                text = hint,
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}
