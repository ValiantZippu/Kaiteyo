package ua.syt0r.kanji.desktop.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString
import ua.syt0r.kanji.desktop.engine.reading.ReadingDocument
import ua.syt0r.kanji.desktop.engine.reading.ReadingHistoryEntry
import ua.syt0r.kanji.desktop.ui.workspace.rememberAppState

/**
 * The reading library: recent documents (with progress + bookmarks) and the
 * reading history. Clicking a document opens it in the reader; the trailing
 * button removes it from the library.
 */
@Composable
fun ReadingLibraryPanel(
    onOpenDocument: (ReadingDocument) -> Unit,
    onRemoveDocument: (ReadingDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberAppState()
    val sc = surfaceColors()
    val ac = accent()
    val engine = state.reading

    val recent = engine.documents.sortedByDescending { it.lastOpenedAt }
    val history = engine.history.take(20)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = DsSpacing.Lg,
            vertical = DsSpacing.Md
        ),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        item {
            DsSectionHeader(
                title = resolveSuiteString { libraryLabel },
                subtitle = "${engine.documents.size} ${resolveSuiteString { librarySubtitle }}"
            )
            Spacer(Modifier.height(DsSpacing.Xs))
        }

        if (recent.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DsSpacing.Lg),
                    verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                ) {
                    Text(
                        text = resolveSuiteString { noDocumentsTitle },
                        color = sc.textPrimary,
                        fontSize = DsType.BodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = resolveSuiteString { noDocumentsBody },
                        color = sc.textMuted,
                        fontSize = DsType.Body,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            items(recent, key = { it.id }) { doc ->
                ReadingDocumentRow(
                    doc = doc,
                    isActive = engine.activeDocumentId == doc.id,
                    onClick = { onOpenDocument(doc) },
                    onRemove = { onRemoveDocument(doc) }
                )
            }
        }

        if (history.isNotEmpty()) {
            item {
                Spacer(Modifier.height(DsSpacing.Md))
                DsSectionHeader(
                    title = resolveSuiteString { historyLabel },
                    subtitle = resolveSuiteString { historySubtitle }
                )
                Spacer(Modifier.height(DsSpacing.Xs))
            }
            items(history, key = { "${it.documentId}-${it.openedAt}" }) { entry ->
                ReadingHistoryRow(entry)
            }
        }
    }
}

@Composable
private fun ReadingDocumentRow(
    doc: ReadingDocument,
    isActive: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()

    DsCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DsSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = doc.title,
                    color = if (isActive) ac.primary else sc.textPrimary,
                    fontSize = DsType.Body,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(doc.kind.label)
                        append(" · ")
                        append((doc.progress * 100).toInt())
                        append(resolveSuiteString { percentRead })
                        if (doc.bookmarkCount > 0) {
                            append(" · ")
                            append(doc.bookmarkCount)
                            append(" ")
                            append(resolveSuiteString { bookmarkSuffix })
                        }
                    },
                    color = sc.textMuted,
                    fontSize = 11.sp
                )
            }
            DsIconButton(
                icon = Icons.Default.Close,
                onClick = onRemove,
                contentDescription = resolveSuiteString { removeFromLibrary },
                size = 26.dp
            )
        }
    }
}

@Composable
private fun ReadingHistoryRow(entry: ReadingHistoryEntry, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val local = entry.openedAt.toLocalDateTime(TimeZone.currentSystemDefault())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = entry.title,
                color = sc.textPrimary,
                fontSize = DsType.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${entry.kind.label} · ${(entry.maxProgress * 100).toInt()}% · " +
                    "${local.month.ordinal + 1}/${local.dayOfMonth} ${local.hour}:${local.minute.toString().padStart(2, '0')}",
                color = sc.textMuted,
                fontSize = 10.sp
            )
        }
    }
}
