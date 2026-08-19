package ua.syt0r.kanji.desktop.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryResultGroup
import ua.syt0r.kanji.desktop.ui.dictionary.DictionaryPopupContent

/**
 * The read-along glossary — full parity with the shared dictionary popup.
 *
 * Reuses [DictionaryPopupContent] (group blocks, expandable match rows,
 * pitch accent graphs, favorites, per-entry mine) so the reader, browser,
 * subtitles and OCR all present identical dictionary content. Adds the
 * reading context the popup body does not know about: the sentence the word
 * appeared in, phrase mining, and a shortcut into the full dictionary.
 */
@Composable
fun ReadingLookupPopup(
    state: AppState,
    query: String,
    groups: List<DictionaryResultGroup>,
    sentence: String,
    documentTitle: String,
    onMineSentence: () -> Unit,
    onOpenDictionary: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()

    DsCard(
        modifier = modifier.width(420.dp),
        elevated = true
    ) {
        Column {
            // ---- Header: query + dictionary count + close ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                    .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = query,
                        color = sc.textPrimary,
                        fontSize = DsType.BodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (groups.isEmpty()) resolveSuiteString { graphNoResults }
                        else "${groups.size} dictionaries · $documentTitle",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                DsIconButton(
                    icon = Icons.Default.Close,
                    onClick = onClose,
                    contentDescription = resolveSuiteString { closeLookup },
                    size = 26.dp
                )
            }

            // ---- Shared dictionary content (parity) ----------------
            DictionaryPopupContent(
                state = state,
                query = query,
                onMine = { payload ->
                    // Reading context rides along: the sentence the word
                    // appeared in plus the reader source tag.
                    state.mining.mine(
                        payload.copy(
                            sentence = sentence,
                            source = "reader",
                            sourceDetail = documentTitle
                        )
                    )
                    onClose()
                },
                groups = groups
            )

            // ---- Footer: sentence context + actions ----------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DsSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                if (sentence.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(sc.surfaceInteractive.copy(alpha = 0.5f))
                            .padding(DsSpacing.Sm)
                    ) {
                        Text(
                            text = sentence,
                            color = sc.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                ) {
                    DsButton(
                        text = resolveSuiteString { mineSentence },
                        onClick = onMineSentence,
                        kind = DsButtonKind.Primary,
                        compact = true
                    )
                    DsButton(
                        text = resolveSuiteString { openDictionary },
                        onClick = onOpenDictionary,
                        kind = DsButtonKind.Secondary,
                        compact = true
                    )
                }
            }
        }
    }
}
