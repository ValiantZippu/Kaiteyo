package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================
// FLAG MANAGER
// Full management interface for card flags
// Search by flag, Study by flag, Bulk flagging
// ============================================

data class FlagStats(
    val flagType: CardFlagType,
    val totalCards: Int = 0,
    val dueCards: Int = 0,
    val newCards: Int = 0,
    val averageEase: Float = 2.5f,
    val averageAccuracy: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagManagerScreen(
    cards: List<KaiteyoCard> = emptyList(),
    onFlagCard: (String, CardFlagType) -> Unit = { _, _ -> },
    onStudyByFlag: (CardFlagType) -> Unit = { },
    onBulkFlag: (List<String>, CardFlagType) -> Unit = { _, _ -> },
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var selectedFlag by remember { mutableStateOf<CardFlagType?>(null) }
    var showBulkDialog by remember { mutableStateOf(false) }

    // Compute stats per flag
    val flagStats = remember(cards) {
        CardFlagType.entries.filter { it != CardFlagType.None }.map { flag ->
            val flagged = cards.filter { it.flag == flag }
            FlagStats(
                flagType = flag,
                totalCards = flagged.size,
                dueCards = flagged.count { c -> c.status == CardStatus.New || c.status == CardStatus.Learning || c.status == CardStatus.Relearning },
                newCards = flagged.count { it.status == CardStatus.New },
                averageEase = if (flagged.isNotEmpty()) flagged.map { it.ease }.average().toFloat() else 2.5f,
                averageAccuracy = if (flagged.isNotEmpty()) flagged.map { it.accuracy }.average().toFloat() else 0f
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flag Manager") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = { showBulkDialog = true }) {
                        Icon(Icons.Default.Build, "Bulk Flag")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats overview
            item {
                Text("Overview", style = MaterialTheme.typography.titleSmall,
                    color = surfaceColors.textMuted, modifier = Modifier.padding(bottom = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val totalFlagged = flagStats.sumOf { it.totalCards }
                    FlagStatCard("Flagged Cards", "$totalFlagged", Icons.Default.Flag, Modifier.weight(1f))
                    FlagStatCard("Due Flagged", "${flagStats.sumOf { it.dueCards }}", Icons.Default.Today, Modifier.weight(1f))
                }
            }

            // Per-flag breakdown
            item {
                Text("By Flag Color", style = MaterialTheme.typography.titleSmall,
                    color = surfaceColors.textMuted, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            }

            items(flagStats) { stat ->
                FlagDetailCard(
                    stats = stat,
                    isExpanded = selectedFlag == stat.flagType,
                    onClick = {
                        selectedFlag = if (selectedFlag == stat.flagType) null else stat.flagType
                    },
                    onStudy = { onStudyByFlag(stat.flagType) }
                )
            }

            // Quick actions
            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleSmall,
                    color = surfaceColors.textMuted, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onBulkFlag(cards.map { it.id }, CardFlagType.Red) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Flag All Red") }
                    OutlinedButton(
                        onClick = { onBulkFlag(cards.map { it.id }, CardFlagType.None) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear All Flags") }
                }
            }

            // Legend
            item {
                Text("Flag Legend", style = MaterialTheme.typography.titleSmall,
                    color = surfaceColors.textMuted, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Card {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        CardFlagType.entries.filter { it != CardFlagType.None }.forEach { flag ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(16.dp)
                                        .clip(CircleShape)
                                        .background(flag.colorFromHex())
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(flag.displayName, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                Text("ID: ${flag.id}", fontSize = 12.sp, color = surfaceColors.textMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    // Bulk flag dialog
    if (showBulkDialog) {
        BulkFlagDialog(
            flags = CardFlagType.entries.filter { it != CardFlagType.None },
            onSelect = { flag ->
                onBulkFlag(cards.map { it.id }, flag)
                showBulkDialog = false
            },
            onDismiss = { showBulkDialog = false }
        )
    }
}

@Composable
private fun FlagDetailCard(
    stats: FlagStats,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onStudy: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val flagColor = stats.flagType.colorFromHex()
    val bgColor by animateColorAsState(
        targetValue = if (isExpanded) flagColor.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(200), label = "flagBg"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp)
                        .clip(CircleShape)
                        .background(flagColor)
                        .then(Modifier.border(2.dp, flagColor.copy(alpha = 0.3f), CircleShape)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Flag,
                        null,
                        Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(stats.flagType.displayName, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Text("${stats.totalCards} cards", fontSize = 12.sp, color = surfaceColors.textMuted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${stats.dueCards} due", fontSize = 13.sp, color = accent.primary)
                    Text("${stats.newCards} new", fontSize = 12.sp, color = surfaceColors.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = surfaceColors.textMuted
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        StatValue("Avg Ease", flagFormatFloat(stats.averageEase, 1))
                        StatValue("Avg Accuracy", "${(stats.averageAccuracy * 100).toInt()}%")
                        StatValue("Total", "${stats.totalCards}")
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onStudy,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = flagColor)
                        ) { Text("Study ${stats.flagType.displayName}") }
                        OutlinedButton(
                            onClick = { /* Filter cards by this flag */ },
                            modifier = Modifier.weight(1f)
                        ) { Text("Show Cards") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BulkFlagDialog(
    flags: List<CardFlagType>,
    onSelect: (CardFlagType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Flag Cards") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Select a flag to apply to all visible cards:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                flags.forEach { flag ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(flag) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(20.dp)
                                .clip(CircleShape)
                                .background(flag.colorFromHex())
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(flag.displayName, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(CardFlagType.None) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("No Flag (Clear)", fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// KMP-compatible float formatting
private fun flagFormatFloat(value: Float, decimals: Int): String {
    val factor = when (decimals) { 0 -> 1; 1 -> 10; 2 -> 100; else -> 1000 }
    val rounded = (value * factor).toInt()
    val intPart = rounded / factor
    val decPart = (rounded % factor).let { if (it < 0) -it else it }
    return if (decimals > 0) "$intPart.${decPart.toString().padStart(decimals, '0')}" else "$intPart"
}

@Composable
fun FlagStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
