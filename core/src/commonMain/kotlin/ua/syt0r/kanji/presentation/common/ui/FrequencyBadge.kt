package ua.syt0r.kanji.presentation.common.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.knowledge.FrequencyBand
import ua.syt0r.kanji.core.knowledge.frequencyNormalized
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// FREQUENCY BADGE
// ------------------------------------------------------------
// A compact, accessible badge that shows kanji/word frequency
// information. Supports both label and numeric rank display.
// Uses color coding that remains accessible (label + icon +
// numeric rank + tooltip).
// ============================================================

/**
 * Frequency badge size variants.
 */
enum class FrequencyBadgeSize {
    /** Tiny — inline with text, 10sp. */
    Tiny,
    /** Small — card-level, 12sp. */
    Small,
    /** Medium — standalone, 14sp. */
    Medium,
    /** Large — hero/featured, 16sp. */
    Large
}

/**
 * A compact frequency badge that shows the frequency band
 * with appropriate color coding.
 *
 * @param band The frequency band to display.
 * @param rank Optional numeric rank (e.g., "#183").
 * @param size Badge size variant.
 * @param showLabel Whether to show the text label.
 * @param showRank Whether to show the numeric rank.
 */
@Composable
fun FrequencyBadge(
    band: FrequencyBand,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    size: FrequencyBadgeSize = FrequencyBadgeSize.Small,
    showLabel: Boolean = true,
    showRank: Boolean = true
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    val textColor = when (band) {
        FrequencyBand.VeryCommon -> Color(0xFF2E7D32) // green
        FrequencyBand.Common -> accent.primary
        FrequencyBand.Moderate -> Color(0xFFF57C00) // orange
        FrequencyBand.Uncommon -> Color(0xFFE65100) // deep orange
        FrequencyBand.Rare -> Color(0xFFC62828) // red
    }

    val bgColor = when (band) {
        FrequencyBand.VeryCommon -> Color(0xFF2E7D32).copy(alpha = 0.12f)
        FrequencyBand.Common -> accent.primary.copy(alpha = 0.12f)
        FrequencyBand.Moderate -> Color(0xFFF57C00).copy(alpha = 0.12f)
        FrequencyBand.Uncommon -> Color(0xFFE65100).copy(alpha = 0.12f)
        FrequencyBand.Rare -> Color(0xFFC62828).copy(alpha = 0.12f)
    }

    val fontSize = when (size) {
        FrequencyBadgeSize.Tiny -> 9.sp
        FrequencyBadgeSize.Small -> 11.sp
        FrequencyBadgeSize.Medium -> 13.sp
        FrequencyBadgeSize.Large -> 15.sp
    }

    val paddingH = when (size) {
        FrequencyBadgeSize.Tiny -> 4.dp
        FrequencyBadgeSize.Small -> 6.dp
        FrequencyBadgeSize.Medium -> 8.dp
        FrequencyBadgeSize.Large -> 10.dp
    }

    val paddingV = when (size) {
        FrequencyBadgeSize.Tiny -> 1.dp
        FrequencyBadgeSize.Small -> 2.dp
        FrequencyBadgeSize.Medium -> 3.dp
        FrequencyBadgeSize.Large -> 4.dp
    }

    val label = band.label

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(horizontal = paddingH, vertical = paddingV),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Color dot indicator for accessibility
            Box(
                modifier = Modifier
                    .size(if (size == FrequencyBadgeSize.Tiny) 5.dp else 7.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            if (showLabel || showRank) {
                Spacer(Modifier.width(4.dp))
            }
            if (showLabel) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (showLabel && showRank && rank != null) {
                Text(
                    text = " ",
                    color = textColor,
                    fontSize = fontSize
                )
            }
            if (showRank && rank != null) {
                Text(
                    text = "#$rank",
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

/**
 * A frequency progress bar that shows normalized frequency
 * as a filled bar. Useful for list items and cards.
 *
 * @param rank The frequency rank (1 = most common).
 * @param maxRank The maximum rank for normalization.
 */
@Composable
fun FrequencyBar(
    rank: Int?,
    modifier: Modifier = Modifier,
    maxRank: Int = 5000,
    height: androidx.compose.ui.unit.Dp = 4.dp
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    val normalized = frequencyNormalized(rank, maxRank)
    val barColor by animateColorAsState(
        targetValue = when {
            normalized > 0.8f -> Color(0xFF2E7D32)
            normalized > 0.6f -> accent.primary
            normalized > 0.4f -> Color(0xFFF57C00)
            normalized > 0.2f -> Color(0xFFE65100)
            else -> Color(0xFFC62828)
        },
        label = "frequencyBarColor"
    )

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(surfaceColors.textMuted.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(height / 2))
                .then(
                    Modifier.background(barColor.copy(alpha = normalized.coerceIn(0.02f, 1f)))
                )
        )
    }
}
