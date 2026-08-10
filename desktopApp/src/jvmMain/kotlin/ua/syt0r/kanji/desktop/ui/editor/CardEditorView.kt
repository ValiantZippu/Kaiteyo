package ua.syt0r.kanji.desktop.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsChip
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsNumericField
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTagChip
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus

// ============================================
// CARD EDITOR
// Full-featured modal editor for any card in the
// pool: content metadata (kanji, meaning, readings,
// radicals), notes, tags, color flags, favorite,
// and SRS state (status, interval, lapses, reps).
// Launched from the browser, review session and
// dashboard via AppState.editingCard.
// ============================================

private val flagPalette = listOf(
    "red" to "#FF6B6B",
    "orange" to "#FEAB57",
    "yellow" to "#FFD93D",
    "green" to "#C2FC8B",
    "blue" to "#7BC8FF",
    "purple" to "#A78BFA"
)

@Composable
fun CardEditorDialog(state: AppState) {
    val card = state.editingCard ?: return
    var deleteConfirm by remember { mutableStateOf(false) }

    DsDialog(title = "Edit card", onDismiss = { state.editingCard = null }) {
        val draft = remember(card.id) { EditorDraft.from(card) }
        val allTags = remember(state.cards.size) {
            state.cards.flatMap { it.tags }.distinct().sorted()
        }

        Column(
            modifier = Modifier
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            // Headline: character + meaning
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Column(Modifier.weight(1f)) {
                    DsTextField(
                        value = draft.character,
                        onValueChange = { draft.character = it },
                        placeholder = "かんじ",
                        label = "Character"
                    )
                }
                Column(Modifier.weight(2f)) {
                    DsTextField(
                        value = draft.meaning,
                        onValueChange = { draft.meaning = it },
                        placeholder = "Primary meaning",
                        label = "Meaning"
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                DsTextField(
                    value = draft.onReadings,
                    onValueChange = { draft.onReadings = it },
                    placeholder = "オン, ジツ",
                    label = "On readings",
                    modifier = Modifier.weight(1f)
                )
                DsTextField(
                    value = draft.kunReadings,
                    onValueChange = { draft.kunReadings = it },
                    placeholder = "ひ, び",
                    label = "Kun readings",
                    modifier = Modifier.weight(1f)
                )
            }

            DsTextField(
                value = draft.radicals,
                onValueChange = { draft.radicals = it },
                placeholder = "radical components, comma separated",
                label = "Radicals"
            )

            DsTextField(
                value = draft.note,
                onValueChange = { draft.note = it },
                placeholder = "Mnemonic, example sentence, context…",
                label = "Note",
                singleLine = false
            )

            // Tags
            Text(
                text = "TAGS",
                color = surfaceColors().textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsTextField(
                    value = draft.newTag,
                    onValueChange = { draft.newTag = it },
                    placeholder = "jlpt-n4",
                    label = null,
                    modifier = Modifier.weight(1f)
                )
                DsButton(
                    text = "Add",
                    icon = Icons.Default.Add,
                    onClick = {
                        val tag = draft.newTag.trim()
                        if (tag.isNotBlank() && tag !in draft.tags) {
                            draft.tags.add(tag)
                        }
                        draft.newTag = ""
                    },
                    compact = true
                )
            }
            if (draft.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
                ) {
                    draft.tags.forEach { tag ->
                        DsTagChip(
                            label = tag,
                            removable = true,
                            onRemove = { draft.tags.remove(tag) }
                        )
                    }
                }
            }
            if (allTags.isNotEmpty()) {
                Text(
                    text = "EXISTING TAGS",
                    color = surfaceColors().textMuted,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
                ) {
                    allTags.filter { it !in draft.tags }.take(12).forEach { tag ->
                        DsChip(text = tag, selected = false, onClick = { draft.tags.add(tag) })
                    }
                }
            }

            // Flags
            Text(
                text = "FLAGS",
                color = surfaceColors().textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
            ) {
                flagPalette.forEach { (name, hex) ->
                    DsChip(
                        text = name,
                        selected = name in draft.flags,
                        onClick = {
                            if (name in draft.flags) draft.flags.remove(name) else draft.flags.add(name)
                        },
                        trailing = null
                    )
                }
            }

            // Preferences
            DsToggle(
                checked = draft.favorite,
                onCheckedChange = { draft.favorite = it },
                label = "Favorite"
            )

            // SRS state
            Text(
                text = "SCHEDULING",
                color = surfaceColors().textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
                DsSelect(
                    selected = draft.status,
                    options = SrsStatus.entries.toList(),
                    onSelected = { draft.status = it },
                    labelOf = { it.name },
                    modifier = Modifier.weight(1f)
                )
                DsNumericField(
                    value = draft.intervalDays,
                    onValueChange = { draft.intervalDays = it },
                    modifier = Modifier.width(110.dp),
                    label = "Interval (days)"
                )
                DsNumericField(
                    value = draft.lapses,
                    onValueChange = { draft.lapses = it },
                    modifier = Modifier.width(90.dp),
                    label = "Lapses"
                )
                DsNumericField(
                    value = draft.reps,
                    onValueChange = { draft.reps = it },
                    modifier = Modifier.width(90.dp),
                    label = "Reps"
                )
            }

            Spacer(Modifier.height(DsSpacing.Sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DsButton(
                    text = "Delete",
                    icon = Icons.Default.Delete,
                    kind = DsButtonKind.Danger,
                    onClick = { deleteConfirm = true },
                    compact = true
                )
                Spacer(Modifier.weight(1f))
                DsButton(text = "Cancel", kind = DsButtonKind.Ghost, onClick = { state.editingCard = null })
                DsButton(
                    text = "Save",
                    enabled = draft.character.isNotBlank() || draft.meaning.isNotBlank(),
                    onClick = { state.saveEditedCard(draft.toCard(card)) }
                )
            }
        }
    }

    if (deleteConfirm) {
        DsConfirmDialog(
            title = "Delete card",
            message = "Delete '${card.character}'? This cannot be undone.",
            confirmText = "Delete",
            danger = true,
            onConfirm = { state.deleteEditingCard() },
            onDismiss = { deleteConfirm = false }
        )
    }
}

/** Mutable working copy of the card fields being edited (state-backed). */
private class EditorDraft(
    character: String,
    meaning: String,
    onReadings: String,
    kunReadings: String,
    radicals: String,
    note: String,
    tags: List<String>,
    flags: List<String>,
    favorite: Boolean,
    status: SrsStatus,
    intervalDays: Int,
    lapses: Int,
    reps: Int
) {
    var character by mutableStateOf(character)
    var meaning by mutableStateOf(meaning)
    var onReadings by mutableStateOf(onReadings)
    var kunReadings by mutableStateOf(kunReadings)
    var radicals by mutableStateOf(radicals)
    var note by mutableStateOf(note)
    val tags = mutableStateListOf<String>().apply { addAll(tags) }
    val flags = mutableStateListOf<String>().apply { addAll(flags) }
    var favorite by mutableStateOf(favorite)
    var status by mutableStateOf(status)
    var intervalDays by mutableStateOf(intervalDays)
    var lapses by mutableStateOf(lapses)
    var reps by mutableStateOf(reps)
    var newTag by mutableStateOf("")

    companion object {
        fun from(card: DesktopCard): EditorDraft = EditorDraft(
            character = card.character,
            meaning = card.meaning,
            onReadings = card.onReadings.joinToString(", "),
            kunReadings = card.kunReadings.joinToString(", "),
            radicals = card.radicals.joinToString(", "),
            note = card.note,
            tags = card.tags.toMutableList(),
            flags = card.flags.toMutableList(),
            favorite = card.favorite,
            status = card.status,
            intervalDays = card.intervalDays.toInt(),
            lapses = card.lapses,
            reps = card.reps
        )
    }

    fun toCard(base: DesktopCard): DesktopCard = base.copy(
        character = character.trim(),
        meaning = meaning.trim(),
        onReadings = splitList(onReadings),
        kunReadings = splitList(kunReadings),
        radicals = splitList(radicals),
        note = note.trim(),
        tags = tags.distinct().toList(),
        flags = flags.distinct().toList(),
        favorite = favorite,
        status = status,
        intervalDays = intervalDays.toDouble(),
        lapses = lapses,
        reps = reps
    )

    private fun splitList(raw: String): List<String> =
        raw.split(Regex("[,、，\\s]+")).filter { it.isNotBlank() }
}
