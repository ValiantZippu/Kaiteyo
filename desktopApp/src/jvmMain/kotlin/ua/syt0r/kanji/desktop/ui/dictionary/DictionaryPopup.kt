package ua.syt0r.kanji.desktop.ui.dictionary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.engine.dictionary.PitchAccent
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryMatch
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryResultGroup
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload

// ============================================
// KAITEYO DICTIONARY POPUP
// A universal popup that works everywhere �?"
// media, browser, reader, OCR, clipboard and study
// screens. Shows definitions, readings, pitch
// accent, frequency, kanji details and has a
// one-click Mine button.
// ============================================

@Composable
fun DictionaryPopupContent(
    state: AppState,
    query: String,
    onMine: (MiningPayload) -> Unit,
    groups: List<DictionaryResultGroup>
) {
    val sc = surfaceColors()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(DsSpacing.Md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                query,
                color = sc.textPrimary,
                fontSize = DsType.Title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${groups.size} dictionaries",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
        Spacer(Modifier.height(DsSpacing.Md))

        if (groups.isEmpty()) {
            Text(
                "No dictionary matches for \"$query\". Try another spelling or install a dictionary.",
                color = sc.textMuted,
                fontSize = DsType.Body
            )
            return@Column
        }

        groups.forEach { group ->
            DictionaryGroupBlock(state, group, onMine)
            Spacer(Modifier.height(DsSpacing.Md))
        }
    }
}

/** One dictionary group with its matches. */
@Composable
fun DictionaryGroupBlock(
    state: AppState,
    group: DictionaryResultGroup,
    onMine: (MiningPayload) -> Unit
) {
    val sc = surfaceColors()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surfaceElevated)
            .border(1.dp, sc.border.copy(alpha = 0.4f), RoundedCornerShape(DsRadius.Md))
            .padding(DsSpacing.Md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = sc.textSecondary,
                modifier = Modifier.width(16.dp)
            )
            Spacer(Modifier.width(DsSpacing.Sm))
            Text(
                group.dictionary.name,
                color = sc.textSecondary,
                fontSize = DsType.Label,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${group.matches.size} match(es)",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
        group.matches.forEach { match ->
            DictionaryMatchRow(state, match, onMine)
            Spacer(Modifier.height(DsSpacing.Sm))
        }
    }
}

@Composable
fun DictionaryMatchRow(
    state: AppState,
    match: DictionaryMatch,
    onMine: (MiningPayload) -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val entry = match.entry
    var expanded by remember { mutableStateOf(false) }
    var bookmarked by remember(match.dictionary.id, entry.headword) {
        mutableStateOf(state.dictionary.isFavorite(match.dictionary.id, entry.headword))
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = DsSpacing.Sm)
        ) {
            Text(
                entry.headword,
                color = sc.textPrimary,
                fontSize = DsType.Heading,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(DsSpacing.Sm))
            entry.readings.firstOrNull()?.let { r ->
                Text(
                    r.reading,
                    color = ac.primary,
                    fontSize = DsType.Body,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            if (entry.frequency.rank != null) {
                Spacer(Modifier.width(DsSpacing.Sm))
                Text(
                    "freq #${entry.frequency.rank}",
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
        entry.senses.firstOrNull()?.let { sense ->
            Text(
                sense.glosses.joinToString("; "),
                color = sc.textSecondary,
                fontSize = DsType.Body,
                maxLines = if (expanded) Int.MAX_VALUE else 2
            )
        }
        if (expanded) {
            Spacer(Modifier.height(DsSpacing.Sm))
            val pitch = entry.readings.flatMap { it.pitchAccents }
            if (pitch.isNotEmpty()) {
                // Visual notation: color each mora by its high/low pitch using
                // the standard downstep convention, when the reading exposes a
                // mora breakdown. Falls back to the plain text form otherwise.
                val reading = entry.readings.firstOrNull { it.pitchAccents.isNotEmpty() }
                val moras = reading?.elements?.takeIf { it.isNotEmpty() }
                if (moras != null && pitch.isNotEmpty()) {
                    PitchAccentGraph(moras, pitch)
                } else {
                    Text(
                        "Pitch: " + pitch.joinToString(", ") { p ->
                            val down = p.downstep?.let { " (downstep $it)" } ?: ""
                            "pos${p.position}$down"
                        },
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
            entry.kanjiSpellings.firstOrNull()?.let { k ->
                Text(
                    buildString {
                        append("Kanji: ${k.character}")
                        if (k.strokeCounts.isNotEmpty()) append("  \u00b7  ${k.strokeCounts.first()} strokes")
                        k.grade?.let { append("  \u00b7  grade $it") }
                        k.jlpt?.let { append("  \u00b7  N$it") }
                    },
                    color = sc.textSecondary,
                    fontSize = DsType.Caption,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = "Mine word",
                    icon = Icons.Default.Bookmark,
                    kind = DsButtonKind.Primary,
                    compact = true,
                    onClick = { onMine(state.mining.payloadForEntry(entry, match.dictionary.name)) }
                )
                DsButton(
                    text = "Graph",
                    icon = Icons.Default.Route,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = {
                        state.pendingGraphNode = entry.headword
                        state.currentView = ua.syt0r.kanji.desktop.appstate.WorkspaceView.Graph
                    }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            DsIconButton(
                icon = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                onClick = {
                    bookmarked = !bookmarked
                    state.dictionary.toggleFavorite(match.dictionary.id, entry.headword)
                },
                contentDescription = if (bookmarked) "Remove favorite" else "Add favorite",
                size = 22.dp
            )
        }
    }
}

/**
 * Visual pitch notation: each mora of the reading colored by its high/low
 * pitch under the standard downstep convention.
 *
 * Convention (position N = the pitch drops after mora N, 0 = flat/heiban):
 *   - N = 0:      first mora low, rest high        (平板)
 *   - N = 1:      first mora high, rest low        (頭高)
 *   - 1 < N < len: first low, high until N, low    (中高)
 *   - N = len:    first low, rest high, drop off   (尾高)
 *
 * Only rendered when the dictionary actually provides a mora breakdown and
 * accent position — never fabricated for readings without data.
 */
@Composable
private fun PitchAccentGraph(moras: List<String>, accents: List<PitchAccent>) {
    val sc = surfaceColors()
    val ac = accent()
    val accent = accents.first()
    val n = moras.size

    fun isHigh(i: Int): Boolean = when {
        accent.position <= 0 -> i > 0                          // heiban
        accent.position == 1 -> i == 0                         // atamadaka
        accent.position >= n -> i > 0                          // odaka
        else -> i in 1 until accent.position                  // nakadaka
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        moras.forEachIndexed { i, mora ->
            Text(
                mora,
                color = if (isHigh(i)) ac.primary else sc.textSecondary,
                fontSize = 14.sp,
                fontWeight = if (isHigh(i)) FontWeight.Bold else FontWeight.Normal
            )
        }
        Spacer(Modifier.width(DsSpacing.Sm))
        // The accent type label, derived from the position against the mora
        // count — same rules as the coloring above.
        Text(
            when {
                accent.position <= 0 -> "平板"
                accent.position == 1 -> "頭高"
                accent.position >= n -> "尾高"
                else -> "中高"
            },
            color = sc.textMuted,
            fontSize = DsType.Caption
        )
        accent.downstep?.let { ds ->
            Text(
                "· downstep $ds",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

/** Floating popup window hosting the dictionary content. */
@Composable
fun DictionaryPopupOverlay(
    state: AppState,
    query: String,
    onDismiss: () -> Unit
) {
    val sc = surfaceColors()
    val groups = remember(query) {
        if (query.isBlank()) emptyList()
        else state.dictionary.lookup(query)
    }
    Column(
        Modifier
            .shadow(DsRadius.Lg, RoundedCornerShape(DsRadius.Lg))
            .clip(RoundedCornerShape(DsRadius.Lg))
            .background(sc.surfaceElevated)
            .border(1.dp, sc.border, RoundedCornerShape(DsRadius.Lg))
            .width(420.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
        ) {
            Text(
                "Dictionary",
                color = sc.textPrimary,
                fontSize = DsType.Label,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            DsIconButton(
                icon = Icons.Default.Close,
                onClick = onDismiss,
                contentDescription = "Close popup",
                size = 22.dp
            )
        }
        DictionaryPopupContent(state, query, onMine = { state.mining.openMining(it) }, groups = groups)
    }
}