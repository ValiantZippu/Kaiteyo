package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// PITCH ACCENT DISPLAY
//
// Inspired by Kaiteyo's pitch accent card. Shows the
// word with pitch contour visualization. Supports three
// display styles: None, Arrow, Overline.
// ============================================================

enum class PitchAccentStyle(val displayName: String, val description: String) {
    None("None", "Do not show accent."),
    Arrow("Arrow", "Mark pitch drop with arrow symbol."),
    Overline("Overline", "Overline high pitch morae.")
}

/**
 * A single mora with its pitch level (0 = low, 1 = high).
 */
data class PitchMora(
    val text: String,
    val pitchHigh: Boolean
)

/**
 * Pitch accent data for a word.
 */
data class PitchAccentData(
    val word: String,
    val reading: String,
    val morae: List<PitchMora>,
    val pitchNumber: Int, // 0 = heiban, 1 = atamadaka, etc.
    val altAccents: List<String> = emptyList(),
    val isCommon: Boolean = false
)

@Composable
fun PitchAccentCard(
    data: PitchAccentData,
    onPlayAudio: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var style by remember { mutableStateOf(PitchAccentStyle.Overline) }
    var showStylePicker by remember { mutableStateOf(false) }

    KaiteyoCard(
        modifier = modifier,
        header = "Pitch Accent",
        subtitle = "${data.reading} · ${data.pitchNumber}型"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Common badge + pitch type
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (data.isCommon) {
                    KaiteyoBadge(
                        text = "◆ Common",
                        containerColor = accent.primary.copy(alpha = 0.12f),
                        contentColor = accent.primary
                    )
                }
                KaiteyoBadge(
                    text = "${data.pitchNumber}型",
                    containerColor = accent.secondary.copy(alpha = 0.12f),
                    contentColor = accent.secondary
                )
            }

            // Reading
            Text(
                text = data.reading,
                fontSize = 13.sp,
                color = surfaceColors.textMuted
            )

            // Word with pitch visualization
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.animateContentSize()
            ) {
                when (style) {
                    PitchAccentStyle.None -> {
                        Text(
                            text = data.word,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = surfaceColors.textPrimary
                        )
                    }
                    PitchAccentStyle.Overline -> {
                        data.morae.forEach { mora ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Overline for high pitch
                                Box(
                                    modifier = Modifier
                                        .height(3.dp)
                                        .width((mora.text.length * 20).dp)
                                        .background(
                                            if (mora.pitchHigh) accent.primary
                                            else Color.Transparent
                                        )
                                )
                                Text(
                                    text = mora.text,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mora.pitchHigh) accent.primary
                                    else surfaceColors.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    PitchAccentStyle.Arrow -> {
                        data.morae.forEachIndexed { index, mora ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Pitch level indicator
                                Box(
                                    modifier = Modifier
                                        .height(if (mora.pitchHigh) 16.dp else 4.dp)
                                        .width(1.dp)
                                        .background(
                                            if (mora.pitchHigh) accent.primary
                                            else surfaceColors.textMuted.copy(alpha = 0.3f)
                                        )
                                )
                                Text(
                                    text = mora.text,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = surfaceColors.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                // Drop arrow at the pitch drop point
                                if (index > 0 &&
                                    data.morae.getOrNull(index - 1)?.pitchHigh == true &&
                                    !mora.pitchHigh
                                ) {
                                    Text(
                                        text = "↓",
                                        fontSize = 12.sp,
                                        color = accent.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Alt accent reading
            if (data.altAccents.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "alt. accent: ${data.altAccents.joinToString(", ")}",
                    fontSize = 11.sp,
                    color = surfaceColors.textMuted
                )
            }

            Spacer(Modifier.height(12.dp))

            // Style picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Pitch Accent Style",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = surfaceColors.textPrimary
                    )
                    Text(
                        text = style.description,
                        fontSize = 10.sp,
                        color = surfaceColors.textMuted
                    )
                }
                // Style selector
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PitchAccentStyle.entries.forEach { s ->
                        val selected = s == style
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (selected) accent.primary.copy(alpha = 0.15f)
                                    else surfaceColors.surfaceInteractive
                                )
                                .border(
                                    1.dp,
                                    if (selected) accent.primary.copy(alpha = 0.4f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { style = s }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = s.displayName,
                                fontSize = 10.sp,
                                color = if (selected) accent.primary else surfaceColors.textSecondary
                            )
                        }
                    }
                }
            }

            // Play audio button
            if (onPlayAudio != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.primary.copy(alpha = 0.10f))
                        .clickable(onClick = onPlayAudio)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Play audio",
                        tint = accent.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Play pronunciation",
                        fontSize = 12.sp,
                        color = accent.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Compact pitch accent inline display — shows the word with
 * pitch contour markers directly in a sentence or word list.
 */
@Composable
fun InlinePitchAccent(
    data: PitchAccentData,
    style: PitchAccentStyle = PitchAccentStyle.Overline,
    modifier: Modifier = Modifier
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        when (style) {
            PitchAccentStyle.None -> {
                Text(
                    text = data.word,
                    fontSize = 20.sp,
                    color = surfaceColors.textPrimary
                )
            }
            PitchAccentStyle.Overline -> {
                data.morae.forEach { mora ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .width((mora.text.length * 14).dp)
                                .background(if (mora.pitchHigh) accent.primary else Color.Transparent)
                        )
                        Text(
                            text = mora.text,
                            fontSize = 18.sp,
                            color = if (mora.pitchHigh) accent.primary else surfaceColors.textPrimary
                        )
                    }
                }
            }
            PitchAccentStyle.Arrow -> {
                data.morae.forEachIndexed { index, mora ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .height(if (mora.pitchHigh) 10.dp else 2.dp)
                                .width(1.dp)
                                .background(
                                    if (mora.pitchHigh) accent.primary
                                    else surfaceColors.textMuted.copy(alpha = 0.3f)
                                )
                        )
                        Text(
                            text = mora.text,
                            fontSize = 18.sp,
                            color = surfaceColors.textPrimary
                        )
                    }
                }
            }
        }
    }
}
