package ua.syt0r.kanji.desktop.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.dictionary.SegmentToken
import ua.syt0r.kanji.desktop.engine.dictionary.WordStatus
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.media.SubtitleCue

// ============================================================
// KANJIVERSE SENTENCE BREAKDOWN — the media mining surface.
//
// Rendered in the player's dictionary side panel while a subtitle
// line is active: the sentence's real segmented vocabulary as a
// Kaiteyo-style breakdown — status dots, furigana-style
// readings, dictionary-form badges for inflected words — with a
// one-click lookup or mine action per word and a "mine the whole
// sentence" action on the header. Every word comes from the real
// JapaneseSegmenter pipeline; nothing is fabricated.
// ============================================================

@Composable
fun KaiteyoSentenceBreakdown(
    media: MediaEngine,
    cue: SubtitleCue,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()

    // Real segmented vocabulary for this cue, Japanese tokens only.
    val tokens = remember(cue) {
        media.tokensFor(cue).filter { it.isJapanese }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(sc.surfaceElevated.copy(alpha = 0.55f))
            .padding(DsSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {

        // ── Header: title + count + mine-the-sentence action ──
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Sentence vocabulary",
                    color = sc.textPrimary,
                    fontSize = DsType.Label,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${tokens.size} words in this line · from the real segmenter",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            if (tokens.isNotEmpty()) {
                DsButton(
                    text = "Mine sentence",
                    icon = Icons.Default.Add,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { media.mineCue(cue) }
                )
            }
        }

        if (tokens.isEmpty()) {
            Text(
                "No Japanese vocabulary detected in this subtitle line.",
                color = sc.textMuted,
                fontSize = DsType.Caption,
                modifier = Modifier.padding(vertical = DsSpacing.Sm)
            )
            return@Column
        }

        // ── The words, one Kaiteyo row each ──
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tokens.forEach { token ->
                KaiteyoBreakdownWordRow(
                    token = token,
                    onLookup = { media.lookupText(token.surface) },
                    onMine = { media.mineCue(cue, token) }
                )
            }
        }
    }
}

@Composable
private fun KaiteyoBreakdownWordRow(
    token: SegmentToken,
    onLookup: () -> Unit,
    onMine: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val statusColor = token.status.kaiteyoStatusColor()
    val headword = token.dictionaryMatch?.entry?.headword

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(sc.surfaceInteractive.copy(alpha = 0.35f))
            .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {

        // Status dot — non-color indicator via the badge label too.
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )

        // Word + furigana-style reading.
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                token.surface,
                color = sc.textPrimary,
                fontSize = DsType.BodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                if (token.reading.isNotBlank()) {
                    Text(
                        token.reading,
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                if (headword != null && headword != token.surface) {
                    DsBadge(text = "辞書形: $headword", tint = ac.primary)
                }
            }
        }

        // Status badge + actions.
        DsBadge(text = token.status.label, tint = statusColor)
        DsIconButton(
            icon = Icons.Default.Search,
            onClick = onLookup,
            contentDescription = "Look up ${token.surface}",
            size = 26.dp
        )
        DsIconButton(
            icon = Icons.Default.Add,
            onClick = onMine,
            contentDescription = "Mine ${token.surface}",
            size = 26.dp,
            tint = ac.primary
        )
    }
}

// ============================================================
// Status → color + label (matches the subtitle annotation ramp)
// ============================================================

private val WordStatus.label: String
    get() = when (this) {
        WordStatus.Unknown -> "unknown"
        WordStatus.Known -> "known"
        WordStatus.Learning -> "learning"
        WordStatus.Mature -> "mature"
        WordStatus.New -> "new"
        WordStatus.Mined -> "mined"
        WordStatus.Suspended -> "suspended"
    }

private fun WordStatus.kaiteyoStatusColor(): Color = when (this) {
    WordStatus.Unknown -> Color(0xFF9E9E9E)
    WordStatus.Known -> Color(0xFFC2FC8B)
    WordStatus.Learning -> Color(0xFF7BC8FF)
    WordStatus.Mature -> Color(0xFFA78BFA)
    WordStatus.New -> Color(0xFFFFD93D)
    WordStatus.Mined -> Color(0xFFFEAB57)
    WordStatus.Suspended -> Color(0xFFFF6B6B)
}
