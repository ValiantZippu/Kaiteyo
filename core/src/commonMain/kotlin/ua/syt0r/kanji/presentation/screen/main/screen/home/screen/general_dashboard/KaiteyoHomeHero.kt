package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoProgressRing
import kotlin.math.roundToInt

// ============================================================
// KANJIVERSE HOME HERO — the front door of the dashboard.
//
// Theme-native hero: the gradient is built from the app's own
// accent + surface tokens (dark, green) instead of a hardcoded
// purple/blue — it always matches the user's theme. Every
// number comes from [GeneralDashboardStats] — real study data.
// ============================================================

/**
 * The greeting banner that opens the Home page. Time-aware greeting,
 * a big Japanese call-to-action line, animated KPI chips and a streak
 * badge — all fed by [stats].
 */
@Composable
fun KaiteyoDashboardHero(
    stats: GeneralDashboardStats,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val greeting = when (now.hour) {
        in 5..11 -> "おはようございます"
        in 12..16 -> "こんにちは"
        in 17..20 -> "こんばんは"
        else -> "まだまだ行こう"
    }
    val englishGreeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Still going"
    }
    val dayName = now.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val monthName = Month.entries[now.monthNumber - 1].name.lowercase().replaceFirstChar { it.uppercase() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.16f),
                        surfaceColors.surfaceElevated,
                        surfaceColors.surface
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Greeting header row: English small, Japanese big.
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = englishGreeting.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
                color = accent.primary
            )
            Text(
                text = greeting,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = surfaceColors.textPrimary
            )
            Text(
                text = "$dayName, ${now.dayOfMonth} $monthName · keep the streak alive",
                fontSize = 12.sp,
                color = surfaceColors.textSecondary,
                lineHeight = 17.sp
            )
        }

        Spacer(Modifier.height(2.dp))

        // KPI chips — animated counts, accent-tinted surfaces.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KaiteyoHeroStatChip(
                icon = Icons.Default.School,
                value = stats.reviewsToday,
                label = "reviews today",
                color = accent.primary,
                modifier = Modifier.weight(1f)
            )
            KaiteyoHeroStatChip(
                icon = Icons.Default.LocalFireDepartment,
                value = stats.currentStreak,
                label = "day streak",
                color = accent.secondary,
                modifier = Modifier.weight(1f)
            )
            KaiteyoHeroStatChip(
                icon = Icons.Default.TrendingUp,
                value = stats.totalReviews.toInt(),
                label = "total reviews",
                color = surfaceColors.textSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        // Search CTA — accent pill that opens the dictionary.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val hovered by interactionSource.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (hovered) accent.primary.copy(alpha = 0.26f)
                        else accent.primary.copy(alpha = 0.16f)
                    )
                    .hoverable(interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onSearchClick
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = accent.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Explore the dictionary",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent.primary
                    )
                }
            }
        }
    }
}

/**
 * The "continue studying" hero card — the primary action of Home.
 *
 * Tapping it ALWAYS does something useful:
 *  - cards ready  → starts the best available practice session
 *  - nothing due  → [onNothingLeft] (navigates to the Library so the
 *    user can pick a deck instead of dead-ending on Home)
 */
@Composable
fun KaiteyoStudyHeroCard(
    stats: GeneralDashboardStats,
    newCount: Int,
    dueCount: Int,
    onContinue: () -> Unit,
    onNothingLeft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current
    val totalReady = newCount + dueCount
    val doneFraction = stats.todayProgressFraction

    val animatedFraction by animateFloatAsState(
        targetValue = doneFraction,
        animationSpec = tween(900),
        label = "studyHeroRing"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.primary.copy(alpha = if (hovered) 0.22f else 0.16f),
                        surfaceColors.surfaceElevated
                    )
                )
            )
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (totalReady == 0) onNothingLeft() else onContinue() }
            )
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accent.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (totalReady == 0) "Nothing queued" else "Continue studying",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = surfaceColors.textPrimary
                )
            }

            Text(
                text = when {
                    totalReady == 0 -> "Tap to go to the Library and pick a deck"
                    else -> "$totalReady cards ready · ${stats.reviewedToday} done today"
                },
                fontSize = 12.sp,
                color = surfaceColors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (totalReady > 0) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KaiteyoCountPill(
                        text = "new",
                        count = newCount,
                        color = accent.primary
                    )
                    KaiteyoCountPill(
                        text = "due",
                        count = dueCount,
                        color = surfaceColors.kanjiDue
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Animated progress ring with "today" center.
        KaiteyoProgressRing(
            progress = animatedFraction,
            size = 76.dp,
            strokeWidth = 8.dp,
            color = accent.primary
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(animatedFraction * 100).roundToInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = surfaceColors.textPrimary
                )
                Text(
                    text = "today",
                    fontSize = 8.sp,
                    color = surfaceColors.textMuted
                )
            }
        }
    }
}

/** A compact Kaiteyo-style KPI chip: icon in a tinted disc, animated count. */
@Composable
private fun KaiteyoHeroStatChip(
    icon: ImageVector,
    value: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(700),
        label = "heroKpi"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceColors.surface.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = if (animatedValue == 0) "—" else animatedValue.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textPrimary
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = surfaceColors.textMuted,
            maxLines = 1
        )
    }
}

/** New/due pill used inside the study hero card. */
@Composable
private fun KaiteyoCountPill(
    text: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$count $text",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = LocalSurfaceColors.current.textPrimary
        )
    }
}
