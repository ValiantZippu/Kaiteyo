package ua.syt0r.kanji.desktop.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsToolbar
import ua.syt0r.kanji.desktop.designsystem.DsToolbarDivider
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString
import ua.syt0r.kanji.desktop.model.ToastKind
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

/**
 * The native Reading workspace. Shows the library (recent documents +
 * history) until a document is opened; the active document renders in the
 * reader with dictionary lookup + mining.
 */
@Composable
fun ReadingView(state: AppState, modifier: Modifier = Modifier) {
    val engine = state.reading

    val active = engine.activeDocument
    if (active != null) {
        ReadingDocumentView(
            document = active,
            onBack = {
                engine.closeDocument()
                state.readingLibrary.save()
            },
            modifier = modifier
        )
        return
    }

    Column(modifier.fillMaxSize()) {
        DsToolbar(
            title = resolveSuiteString { readingTitle },
            subtitle = resolveSuiteString { readingSubtitle },
            actions = {
                DsButton(
                    text = resolveSuiteString { importFile },
                    icon = Icons.Default.Add,
                    onClick = { importReadingFile(state) },
                    compact = true
                )
                DsButton(
                    text = resolveSuiteString { pasteText },
                    icon = Icons.Default.ContentPaste,
                    onClick = { importReadingClipboard(state) },
                    kind = DsButtonKind.Secondary,
                    compact = true
                )
            }
        )
        DsToolbarDivider()

        Row(Modifier.fillMaxSize()) {
            ReadingLibraryPanel(
                onOpenDocument = { doc ->
                    engine.activeDocumentId = doc.id
                    state.readingLibrary.save()
                },
                onRemoveDocument = { doc ->
                    engine.removeDocument(doc.id)
                    state.readingLibrary.save()
                },
                modifier = Modifier.width(380.dp)
            )

            // Right-hand pane: how reading fits the study workflow.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                val sc = surfaceColors()
                Text(
                    text = resolveSuiteString { readLookupMine },
                    color = sc.textPrimary,
                    fontSize = DsType.Title,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = resolveSuiteString { noDocumentsBody },
                    color = sc.textSecondary,
                    fontSize = DsType.Body,
                    lineHeight = 21.sp
                )
                Spacer(Modifier.height(DsSpacing.Sm))
                ReadingTipCard(
                    title = resolveSuiteString { tipClickWord },
                    body = resolveSuiteString { tipClickWordBody }
                )
                ReadingTipCard(
                    title = resolveSuiteString { tipBookmarks },
                    body = resolveSuiteString { tipBookmarksBody }
                )
                ReadingTipCard(
                    title = resolveSuiteString { tipSearch },
                    body = resolveSuiteString { tipSearchBody }
                )
                ReadingTipCard(
                    title = resolveSuiteString { tipProgress },
                    body = resolveSuiteString { tipProgressBody }
                )
            }
        }
    }
}

@Composable
private fun ReadingTipCard(title: String, body: String) {
    val sc = surfaceColors()
    DsCard {
        Column(Modifier.padding(DsSpacing.Md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = sc.textPrimary,
                fontSize = DsType.Body,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                color = sc.textMuted,
                fontSize = DsType.Body,
                lineHeight = 18.sp
            )
        }
    }
}

// ------------------------------------------------------------
// Import actions
// ------------------------------------------------------------

private fun importReadingFile(state: AppState) {
    val file = pickReadingFile() ?: return
    val doc = state.reading.openFile(file)
    state.readingLibrary.save()
    if (doc != null) {
        state.activityLog.record(ActivityCategory.System, "Opened reading document \"${doc.title}\"")
        state.toastHost.show("Reading \"${doc.title}\"", kind = ToastKind.Success)
    } else {
        state.reading.lastError?.let {
            state.toastHost.show(it, kind = ToastKind.Warning)
        }
    }
}

private fun importReadingClipboard(state: AppState) {
    val text = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard
            .getData(DataFlavor.stringFlavor) as? String
    }.getOrNull() ?: run {
        state.toastHost.show(resolveSuiteString { noClipboardText }, kind = ToastKind.Warning)
        return
    }
    if (text.isBlank()) {
        state.toastHost.show(resolveSuiteString { clipboardTextEmpty }, kind = ToastKind.Warning)
        return
    }
    val title = "Pasted text ${java.time.LocalTime.now().withNano(0)}"
    val doc = state.reading.importClipboard(title, text)
    state.readingLibrary.save()
    if (doc != null) {
        state.activityLog.record(ActivityCategory.System, "Imported clipboard text as a reading document")
        state.toastHost.show("Imported ${text.length} characters", kind = ToastKind.Success)
    } else {
        state.reading.lastError?.let {
            state.toastHost.show(it, kind = ToastKind.Warning)
        }
    }
}

/** AWT file picker (JVM desktop) — returns null when cancelled. */
private fun pickReadingFile(): java.io.File? = runCatching {
    val dialog = java.awt.FileDialog(
        null as java.awt.Frame?,
        "Open reading document",
        java.awt.FileDialog.LOAD
    )
    dialog.isMultipleMode = false
    dialog.isVisible = true
    dialog.files.firstOrNull()
}.getOrNull()
