package ua.syt0r.kanji.desktop.ui.ocr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.ocr.CaptureRegion
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

// ============================================
// KAITEYO OCR WORKSPACE
// Capture Japanese text from images, clipboard or
// screen regions and feed it into the dictionary
// lookup and mining workflow. Tesseract (Tess4J)
// is detected automatically when present.
// ============================================

@Composable
fun OcrView(state: AppState) {
    val sc = surfaceColors()
    val ocr = state.ocr
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = "OCR",
            subtitle = if (ocr.available) "Tesseract backend detected — recognition ready."
            else "No OCR backend detected — capture still works; install Tesseract + Tess4J to enable recognition.",
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(
                        text = "From file",
                        icon = Icons.Default.FileOpen,
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = {
                            val file = chooseImageFile()
                            if (file != null) {
                                try {
                                    ocr.ocrImage(file)
                                } catch (e: Exception) {
                                    error = e.message ?: "OCR failed"
                                }
                            }
                        }
                    )
                    DsButton(
                        text = "Clipboard",
                        icon = Icons.Default.ContentPaste,
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = {
                            try {
                                ocr.ocrClipboard()
                            } catch (e: Exception) {
                                error = e.message ?: "Clipboard OCR failed"
                            }
                        }
                    )
                    DsButton(
                        text = "Full screen",
                        icon = Icons.Default.Expand,
                        onClick = {
                            try {
                                ocr.ocrFullScreen()
                            } catch (e: Exception) {
                                error = e.message ?: "Screen capture failed"
                            }
                        }
                    )
                    DsButton(
                        text = "Region",
                        icon = Icons.Default.TextFields,
                        kind = DsButtonKind.Ghost,
                        compact = true,
                        onClick = {
                            try {
                                // default to a center region until an interactive selector is added
                                val w = java.awt.Toolkit.getDefaultToolkit().screenSize
                                ocr.ocrScreen(CaptureRegion(w.width / 4, w.height / 4, w.width / 2, w.height / 2))
                            } catch (e: Exception) {
                                error = e.message ?: "Region capture failed"
                            }
                        }
                    )
                }
            }
        )

        if (ocr.lastResult == null) {
            DsCard {
                DsEmptyState(
                    title = "No text captured yet",
                    message = "Run OCR from an image file, the clipboard or the full screen to extract Japanese text and look it up.",
                    icon = Icons.Default.TextFields
                )
            }
        } else {
            val result = ocr.lastResult
            result?.let { res ->
                DsCard {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(DsSpacing.Xl),
                        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Recognized text", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    buildString {
                                        append("${res.lines.size} lines")
                                        res.imagePath?.let { append("  ·  ").append(it.substringAfterLast(File.separatorChar)) }
                                    },
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption
                                )
                            }
                            DsBadge(text = res.language.uppercase(), tint = sc.textSecondary)
                        }

                        Text(
                            res.text,
                            color = sc.textPrimary,
                            fontSize = DsType.BodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .verticalScroll(rememberScrollState())
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                            DsButton(
                                text = "Look up all",
                                icon = Icons.Default.TextFields,
                                onClick = {
                                    val head = res.lines.firstOrNull()?.text.orEmpty()
                                    if (head.isNotBlank()) state.dictionary.query = head
                                }
                            )
                            DsButton(
                                text = "Mine lines",
                                icon = Icons.Default.ContentPaste,
                                kind = DsButtonKind.Secondary,
                                compact = true,
                                onClick = {
                                    res.lines.take(6).forEach { line ->
                                        val t = line.text.trim()
                                        if (t.length >= 2) {
                                            state.mining.openMining(
                                                MiningPayload(
                                                    headword = t.take(40),
                                                    definition = line.confidence.toString(),
                                                    sentence = t,
                                                    source = "ocr",
                                                    sourceDetail = "image"
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        Text("Lines", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
                        res.lines.forEach { line ->
                            Text(
                                line.text.ifBlank { "(blank)" },
                                color = sc.textSecondary,
                                fontSize = DsType.Body,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        error?.let { err ->
            DsCard {
                Text(err, color = sc.textPrimary, fontSize = DsType.Body)
                DsButton(
                    text = "OK",
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    modifier = Modifier.padding(top = DsSpacing.Sm),
                    onClick = { error = null }
                )
            }
        }
    }
}

private fun chooseImageFile(): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Open image for OCR"
        fileSelectionMode = JFileChooser.FILES_ONLY
        addChoosableFileFilter(FileNameExtensionFilter("Images", "png", "jpg", "jpeg", "bmp", "gif", "webp"))
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}
