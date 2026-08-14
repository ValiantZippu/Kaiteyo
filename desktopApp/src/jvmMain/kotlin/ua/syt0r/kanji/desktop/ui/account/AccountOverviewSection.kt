package ua.syt0r.kanji.desktop.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsStatTile
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.account.AccountEngine
import ua.syt0r.kanji.desktop.engine.account.ProviderKind
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.StudyDaySummary

// ============================================
// ACCOUNT — OVERVIEW
// The dashboard: profile hero, learning stats,
// live storage breakdown and sync status.
// Also hosts the shared account UI helpers used
// by every section (avatar, dates, bytes, streaks).
// ============================================

@Composable
fun AccountOverviewSection(
    state: AppState,
    engine: AccountEngine,
    onOpenSection: (AccountSection) -> Unit
) {
    val sc = surfaceColors()
    val identity by engine.identity.collectAsState()
    val connections by engine.connections.collectAsState()
    val storage by engine.storage.collectAsState()
    val settingsData by engine.settingsData.collectAsState()

    val (currentStreak, longestStreak) = computeStreaks(state.summaries.toList())
    val learned = state.cards.count { it.status != SrsStatus.New }
    val cloud = connections.firstOrNull { it.isConnected && it.kind != ProviderKind.Local }

    // ── Profile hero ──
    DsCard(elevated = true) {
        Row(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountAvatar(name = identity.displayName, seed = identity.avatarSeed, size = 64.dp)
            Spacer(Modifier.width(DsSpacing.Lg))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                ) {
                    Text(
                        text = identity.displayName,
                        color = sc.textPrimary,
                        fontSize = DsType.Heading,
                        fontWeight = FontWeight.Bold
                    )
                    DsBadge(
                        text = if (cloud != null) "Cloud · ${cloud.displayName}" else "Local only",
                        tint = if (cloud != null) Color(0xFFC2FC8B) else sc.textMuted
                    )
                }
                Text(
                    text = identity.email.ifBlank { identity.username }.ifBlank { identity.displayName },
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
                Text(
                    text = "Member since ${formatDate(identity.joinedAtEpochMs)} · ${identity.learnerLevel.replaceFirstChar { it.uppercase() }}",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            DsButton(
                text = "Edit profile",
                icon = Icons.Default.Edit,
                kind = DsButtonKind.Ghost,
                compact = true,
                onClick = { onOpenSection(AccountSection.Profile) }
            )
        }
    }

    // ── Learning stats ──
    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(title = "Learning", subtitle = "Your study progress at a glance")
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                DsStatTile("Current streak", "${currentStreak}d", Modifier.weight(1f))
                DsStatTile("Longest streak", "${longestStreak}d", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                DsStatTile("Reviews", state.totalReviews().toString(), Modifier.weight(1f))
                DsStatTile("Cards learned", learned.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                DsStatTile("Study time", state.formatDuration(state.totalStudyTime()), Modifier.weight(1f))
                DsStatTile("Due now", state.dueCount().toString(), Modifier.weight(1f))
            }
        }
    }

    // ── Storage ──
    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(
                title = "Storage",
                subtitle = "${formatBytes(storage.totalBytes)} on this device",
                action = {
                    DsButton(
                        text = "Details",
                        kind = DsButtonKind.Ghost,
                        compact = true,
                        onClick = { onOpenSection(AccountSection.Developer) }
                    )
                }
            )
            storage.sorted.take(6).forEach { category ->
                Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category.label,
                            color = sc.textPrimary,
                            fontSize = DsType.Body,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatBytes(category.bytes),
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                    DsProgressBar(
                        fraction = if (storage.totalBytes > 0) category.bytes.toFloat() / storage.totalBytes else 0f,
                        height = 4.dp
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                MiniStat("Cards", state.cards.size.toString(), Modifier.weight(1f))
                MiniStat("Decks", state.library.decks.size.toString(), Modifier.weight(1f))
                MiniStat(
                    "Dictionary entries",
                    state.dictionary.installed.sumOf { it.entryCount }.toString(),
                    Modifier.weight(1f)
                )
            }
        }
    }

    // ── Sync status ──
    DsCard {
        Column(
            modifier = Modifier.padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSectionHeader(
                title = "Sync",
                subtitle = "Device-to-device synchronization",
                action = {
                    DsButton(
                        text = "Open Sync",
                        icon = Icons.Default.Sync,
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = { onOpenSection(AccountSection.Sync) }
                    )
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                SyncStatPill(
                    "Last sync",
                    state.lastSyncAt?.let { formatDateTime(it.toEpochMilliseconds()) } ?: "Never",
                    Modifier.weight(1f)
                )
                SyncStatPill(
                    "Automatic",
                    if (settingsData.autoSync) "Every ${settingsData.syncIntervalMinutes} min" else "Off",
                    Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.lastSyncMessage,
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    modifier = Modifier.weight(1f)
                )
                if (state.syncBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

// ============================================
// SHARED HELPERS
// ============================================

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Lg))
            .background(sc.surfaceInteractive.copy(alpha = 0.4f))
            .padding(DsSpacing.Md)
    ) {
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
        Text(value, color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SyncStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Lg))
            .background(sc.surfaceInteractive.copy(alpha = 0.4f))
            .padding(DsSpacing.Md)
    ) {
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
        Text(value, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

/** Initial-based avatar with a stable color derived from the seed. */
@Composable
fun AccountAvatar(name: String, seed: String = "", size: Dp = 48.dp) {
    val color = remember(seed) { avatarColor(seed.ifBlank { name }) }
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(DsRadius.Xl))
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).ifBlank { "?" }.uppercase(),
            color = color,
            fontSize = with(density) { (size.value * 0.42f).toSp() },
            fontWeight = FontWeight.Bold
        )
    }
}

private val avatarPalette = listOf(
    Color(0xFF7BC8FF),
    Color(0xFFA78BFA),
    Color(0xFFC2FC8B),
    Color(0xFFFEAB57),
    Color(0xFFFFD93D),
    Color(0xFFFF8FA3)
)

private fun avatarColor(seed: String): Color =
    avatarPalette[(seed.hashCode() and Int.MAX_VALUE) % avatarPalette.size]

fun formatDate(millis: Long): String {
    if (millis <= 0) return "—"
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')}"
}

fun formatDateTime(millis: Long): String {
    if (millis <= 0) return "—"
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')} " +
        "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

/** Current + longest study streak (consecutive days with at least one review). */
fun computeStreaks(summaries: List<StudyDaySummary>): Pair<Int, Int> {
    val studied = summaries
        .filter { it.newCount + it.reviewCount > 0 }
        .map { it.day }
        .toMutableSet()
    if (studied.isEmpty()) return 0 to 0

    val tz = TimeZone.currentSystemDefault()
    var cursor = Clock.System.todayIn(tz)
    if (cursor.toString() !in studied) cursor = cursor.minus(1, DateTimeUnit.DAY)
    var current = 0
    while (cursor.toString() in studied) {
        current++
        cursor = cursor.minus(1, DateTimeUnit.DAY)
    }

    val sorted = studied
        .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        .sorted()
    var longest = 0
    var run = 0
    var previous: LocalDate? = null
    sorted.forEach { date ->
        run = if (previous != null && date == previous.plus(1, DateTimeUnit.DAY)) run + 1 else 1
        longest = maxOf(longest, run)
        previous = date
    }
    return current to longest
}
