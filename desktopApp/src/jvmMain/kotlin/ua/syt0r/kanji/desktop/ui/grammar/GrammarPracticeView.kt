package ua.syt0r.kanji.desktop.ui.grammar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTagChip
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.grammar.CuratedGrammarFacts
import ua.syt0r.kanji.desktop.engine.grammar.GrammarIndex
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString
import ua.syt0r.kanji.desktop.model.ToastKind
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

// ============================================
// GRAMMAR PRACTICE
// An original, explanation-first grammar workflow:
// each card shows the pattern, a plain-language
// explanation, and an example sentence with a
// blank. The learner completes the sentence, then
// reveals the correct answer and self-grades.
// The built-in starter deck works out of the box;
// any card tagged "grammar" joins the session too.
// ============================================

private data class GrammarItem(
    val pattern: String,
    val meaning: String,
    val explanation: String,
    val sentenceBefore: String,
    val answer: String,
    val sentenceAfter: String
) {
    val sentence: String get() = "$sentenceBefore$answer$sentenceAfter"
    val prompt: String get() = "$sentenceBefore＿＿＿$sentenceAfter"
}

/**
 * The built-in grammar content, derived from the curated reference facts via
 * the GrammarIndex (no hardcoded per-card strings — the index is the source).
 * Each example sentence is split around the pattern's real occurrence: the
 * pattern's tail (〜ながら → ながら) is located inside the example and becomes
 * the blank. Entries whose example doesn't literally contain the tail fall
 * back to showing the whole sentence with the pattern appended as context,
 * exactly like user-tagged grammar cards do.
 */
private fun curatedGrammarItems(): List<GrammarItem> {
    val index = GrammarIndex(CuratedGrammarFacts.all)
    return CuratedGrammarFacts.all.mapNotNull { entry ->
        val tail = entry.pattern.removePrefix("〜")
        if (tail.isBlank()) return@mapNotNull null
        val example = entry.examples.firstOrNull()?.japanese ?: return@mapNotNull null
        val at = example.indexOf(tail)
        if (at >= 0) {
            GrammarItem(
                pattern = entry.pattern,
                meaning = shortMeaning(entry.meaning),
                explanation = entry.meaning,
                sentenceBefore = example.substring(0, at),
                answer = tail,
                sentenceAfter = example.substring(at + tail.length)
            )
        } else {
            GrammarItem(
                pattern = entry.pattern,
                meaning = shortMeaning(entry.meaning),
                explanation = entry.meaning,
                sentenceBefore = "",
                answer = tail,
                sentenceAfter = " ($example)"
            )
        }
    }
}

/** Headline for the card header — first clause of the explanation text. */
private fun shortMeaning(meaning: String): String =
    meaning.split(" — ", " —", "— ", ";").first().trim().take(48)

/** All built-in + user-tagged grammar items (deduplicated by pattern). */
private fun allGrammarItems(state: AppState): List<GrammarItem> =
    (curatedGrammarItems() + taggedGrammarItems(state)).distinctBy { it.pattern }

/** Grammar items contributed by user cards tagged `grammar`. */
private fun taggedGrammarItems(state: AppState): List<GrammarItem> =
    state.cards
        .filter { card -> card.tags.any { it.contains("grammar", ignoreCase = true) } }
        .map { card ->
            val note = card.note.trim()
            val example = note.lines().firstOrNull { it.contains("。") || it.contains("．") }
                ?: note
            GrammarItem(
                pattern = card.character,
                meaning = card.meaning,
                explanation = "Added from your card pool. Edit the card note to store a fuller explanation.",
                sentenceBefore = "",
                answer = card.character,
                sentenceAfter = if (example.isNotBlank()) " ($example)" else ""
            )
        }

@Composable
fun GrammarPracticeView(state: AppState) {
    var session by remember { mutableStateOf<List<GrammarItem>?>(null) }
    var index by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var practiceMs by remember { mutableStateOf(0L) }

    val items = session
    if (items == null) {
        GrammarLaunchPanel(
            state = state,
            onStart = {
                val deck = allGrammarItems(state)
                    .shuffled()
                    .take(12)
                session = deck
                index = 0
                revealed = false
                input = ""
                practiceMs = 0L
            }
        )
        return
    }

    val item = items.getOrNull(index)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Text(
                text = "${index + 1} / ${items.size}",
                color = surfaceColors().textPrimary,
                fontSize = DsType.Title,
                fontWeight = FontWeight.SemiBold
            )
            DsProgressBar(
                fraction = if (items.isEmpty()) 0f else index.toFloat() / items.size,
                modifier = Modifier.weight(1f)
            )
            DsBadge(text = resolveSuiteString { grammarTitle }, tint = accent().primary)
        }

        if (item == null) {
            DsCard(elevated = true) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(DsSpacing.Xl),
                    verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Session complete",
                        color = surfaceColors().textPrimary,
                        fontSize = DsType.Heading,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You worked through ${items.size} grammar patterns.",
                        color = surfaceColors().textMuted,
                        fontSize = DsType.Body
                    )
                    DsButton(
                        text = "Practice again",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            index = 0
                            revealed = false
                            input = ""
                            practiceMs = 0L
                        }
                    )
                }
            }
            return
        }

        // Pattern + explanation (front of the card)
        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(
                        text = item.pattern,
                        color = accent().primary,
                        fontSize = DsType.Heading,
                        fontWeight = FontWeight.Bold
                    )
                    Text(item.meaning, color = surfaceColors().textMuted, fontSize = DsType.Body)
                }
                Text(item.explanation, color = surfaceColors().textSecondary, fontSize = DsType.Body)
                Spacer(Modifier.height(DsSpacing.Sm))
                Text(
                    text = item.prompt,
                    color = surfaceColors().textPrimary,
                    fontSize = DsType.BodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Completion input
        DsTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = "Complete the sentence (type the pattern)…",
            label = "Your answer"
        )

        // Reveal + grading
        if (revealed) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    val normalized = { s: String -> s.replace(" ", "").replace("　", "") }
                    val correct = normalized(input).isNotEmpty() && normalized(item.answer).contains(normalized(input))
                    Text(
                        text = if (correct) "Correct pattern!" else "Keep practicing",
                        color = if (correct) Color(0xFFC2FC8B) else Color(0xFFFFB86B),
                        fontSize = DsType.Title,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.sentence,
                        color = surfaceColors().textPrimary,
                        fontSize = DsType.BodyLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        DsTagChip(label = "Pattern: ${item.pattern}", colorHex = "#7BC8FF")
                        DsTagChip(label = "Meaning: ${item.meaning}", colorHex = "#A78BFA")
                    }
                }
            }
        }

        val advance: () -> Unit = {
            if (index + 1 >= items.size) {
                state.recordPracticeTime(practiceMs.seconds)
                state.activityLog.record(ActivityCategory.Study, "Grammar session finished (${items.size} patterns)")
                state.toastHost.show("Grammar practice complete", kind = ToastKind.Success)
                session = null
            } else {
                index += 1
                revealed = false
                input = ""
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (revealed) {
                DsButton(
                    text = "Again",
                    kind = DsButtonKind.Danger,
                    onClick = {
                        practiceMs += 25_000
                        advance()
                    }
                )
                DsButton(
                    text = "Good",
                    onClick = {
                        practiceMs += 25_000
                        advance()
                    }
                )
            } else {
                DsButton(
                    text = "Reveal",
                    icon = Icons.Default.Visibility,
                    onClick = { revealed = true }
                )
            }
            Spacer(Modifier.weight(1f))
            DsButton(
                text = "Skip",
                kind = DsButtonKind.Ghost,
                onClick = { advance() }
            )
        }
    }
}

@Composable
private fun GrammarLaunchPanel(state: AppState, onStart: () -> Unit) {
    val sc = surfaceColors()
    val userPatterns = taggedGrammarItems(state).size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DsSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        DsCard(elevated = true) {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text(
                    "${resolveSuiteString { grammarTitle }} practice",
                    color = sc.textPrimary,
                    fontSize = DsType.Heading,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Explanation-first pattern review: read the pattern and its explanation, complete the example sentence, then reveal and grade yourself.",
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsBadge(text = "${curatedGrammarItems().size} ${resolveSuiteString { builtInPatternsLabel }}", tint = accent().primary)
                    if (userPatterns > 0) {
                        DsBadge(text = "+$userPatterns ${resolveSuiteString { fromYourCardsLabel }}", tint = Color(0xFFC2FC8B))
                    }
                }
                DsButton(
                    text = resolveSuiteString { startGrammarSession },
                    icon = Icons.Default.PlayArrow,
                    onClick = onStart
                )
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("How the workflow works", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.SemiBold)
                GrammarHowRow("1", "Front: the pattern, its meaning, and a plain-language explanation.")
                GrammarHowRow("2", "Complete the example sentence by typing the pattern.")
                GrammarHowRow("3", "Reveal: the full sentence appears, and your answer is checked.")
                GrammarHowRow("4", "Grade yourself. Study time feeds the dashboard; cards tagged 'grammar' are folded in automatically.")
            }
        }
    }
}

@Composable
private fun GrammarHowRow(number: String, text: String) {
    val sc = surfaceColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Box(
            modifier = Modifier
                .width(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(number, color = accent().primary, fontSize = DsType.Caption, fontWeight = FontWeight.Bold)
        }
        Text(text, color = sc.textSecondary, fontSize = DsType.Body)
    }
}
