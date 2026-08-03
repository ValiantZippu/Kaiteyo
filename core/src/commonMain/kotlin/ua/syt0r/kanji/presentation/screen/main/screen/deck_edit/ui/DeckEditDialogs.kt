package ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenConfiguration


@Composable
fun DeleteDeckDialog(
    configuration: DeckEditScreenConfiguration.EditExisting,
    onDismissRequest: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {

    val strings = resolveString { deckEdit }

    MultiplatformDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = strings.deleteTitle) },
        content = { Text(text = strings.deleteMessage(configuration.title)) },
        buttons = {
            TextButton(
                onClick = onDeleteConfirmed
            ) {
                Text(text = strings.deleteButtonDefault)
            }
        }
    )

}

@Composable
fun SaveDeckDialog(
    title: MutableState<String>,
    isArchived: MutableState<Boolean>,
    isArchiveEnabled: Boolean,
    toggleArchive: () -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {

    val strings = resolveString { deckEdit }

    MultiplatformDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = strings.saveTitle) },
        content = {
            Column {
                TextField(
                    value = title.value,
                    onValueChange = { title.value = it },
                    singleLine = true,
                    modifier = Modifier.clip(MaterialTheme.shapes.small)
                        .fillMaxWidth(),
                    isError = title.value.isEmpty(),
                    trailingIcon = {
                        IconButton(
                            onClick = { title.value = "" }
                        ) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    label = { Text(strings.saveInputHint) },
                    colors = TextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.onSurface,
                        errorCursorColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                        errorLabelColor = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                if (isArchiveEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(text = strings.archiveTitle, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = strings.archiveHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isArchived.value,
                            onCheckedChange = { toggleArchive() }
                        )
                    }
                }
            }
        },
        buttons = {
            TextButton(
                enabled = title.value.isNotEmpty(),
                onClick = onConfirm,
                modifier = Modifier.animateContentSize()
            ) {
                Text(text = strings.saveButtonDefault)
            }
        }
    )

}

@Composable
fun DeckEditLeaveConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {

    val strings = resolveString { deckEdit }

    MultiplatformDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(strings.leaveConfirmationTitle) },
        content = { Text(strings.leaveConfirmationMessage) },
        buttons = {
            TextButton(onDismissRequest) {
                Text(strings.leaveConfirmationCancel)
            }
            TextButton(onConfirmation) {
                Text(strings.leaveConfirmationAccept)
            }
        }
    )

}

// Test with 丌
@Composable
fun DeckEditUnknownCharactersDialog(
    characters: List<String>,
    onDismissRequest: () -> Unit
) {

    MultiplatformDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = resolveString { deckEdit.unknownTitle })
        },
        content = {
            Text(text = resolveString { deckEdit.unknownMessage(characters.toList()) })
        },
        buttons = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(text = resolveString { deckEdit.unknownButton })
            }
        }
    )

}
