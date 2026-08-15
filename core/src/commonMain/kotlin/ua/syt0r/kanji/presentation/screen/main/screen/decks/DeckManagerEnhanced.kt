package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

// ============================================
// KAITEYO FEATURES HUB
// Central navigation for all deck/card features
// ============================================

/** All available deck/card features */
enum class KaiteyoFeature(val displayName: String, val description: String, val icon: @Composable () -> Unit) {
    DeckBrowser("Deck Browser", "Browse, organize, and manage your decks",
        { Icon(Icons.Default.Folder, null) }),
    CardBrowser("Card Browser", "View, search, and edit cards with advanced filtering",
        { Icon(Icons.Default.List, null) }),
    Tags("Tag Manager", "Create, color, nest, and manage tags",
        { Icon(Icons.Default.Label, null) }),
    Flags("Flag Manager", "Mark cards with 7 color-coded flags",
        { Icon(Icons.Default.Flag, null) }),
    Notes("Card Notes", "Add markdown notes, images, and links to cards",
        { Icon(Icons.Default.Note, null) }),
    CardStatus("Card Status", "Change between New, Learning, Young, Mature, etc.",
        { Icon(Icons.Default.Circle, null) }),
    ReviewSettings("Review Settings", "Customize answer buttons, layout, auto-next, and more",
        { Icon(Icons.Default.Settings, null) }),
    KeyboardShortcuts("Keyboard Shortcuts", "Configure key bindings for every action",
        { Icon(Icons.Default.Keyboard, null) }),
    StudyHistory("Study History", "Audit log of all reviews and card actions",
        { Icon(Icons.Default.History, null) }),
    Statistics("Statistics", "Detailed analytics, charts, and per-deck stats",
        { Icon(Icons.Default.BarChart, null) }),
    Search("Search Engine", "Universal search across all card fields",
        { Icon(Icons.Default.Search, null) }),
    BulkActions("Bulk Actions", "Multi-select, tag, flag, delete, move, suspend, archive",
        { Icon(Icons.Default.Build, null) }),
    ImportExport("Import / Export", "APKG, CSV, JSON, TXT, Markdown transfer",
        { Icon(Icons.Default.FileUpload, null) }),
    Backup("Backup & Restore", "Automatic/manual backups with verification and restore",
        { Icon(Icons.Default.Backup, null) }),
    Plugins("Plugin Manager", "Extend Kaiteyo with community plugins",
        { Icon(Icons.Default.Extension, null) }),
    UndoHistory("Undo History", "Undo/redo support for all actions",
        { Icon(Icons.Default.Undo, null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckFeaturesHub(
    navigationState: MainNavigationState,
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kaiteyo Features") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("All Features", style = MaterialTheme.typography.titleSmall,
                    color = surfaceColors.textMuted, modifier = Modifier.padding(bottom = 4.dp))
            }

            items(KaiteyoFeature.entries) { feature ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceColors.surface)
                        .clickable { navigationState.navigate(feature.toDestination()) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        feature.icon()
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(feature.displayName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(feature.description, fontSize = 12.sp, color = surfaceColors.textMuted)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null, Modifier.size(20.dp),
                        tint = surfaceColors.textMuted)
                }
            }
        }
    }
}

private fun KaiteyoFeature.toDestination(): MainDestination {
    return when (this) {
        KaiteyoFeature.DeckBrowser -> MainDestination.DeckBrowser
        KaiteyoFeature.CardBrowser -> MainDestination.CardBrowser()
        KaiteyoFeature.Tags -> MainDestination.TagManager
        KaiteyoFeature.Flags -> MainDestination.FlagManager
        KaiteyoFeature.Notes -> MainDestination.NoteEditor
        KaiteyoFeature.CardStatus -> MainDestination.CardStatusManager
        KaiteyoFeature.ReviewSettings -> MainDestination.ReviewSettings
        KaiteyoFeature.KeyboardShortcuts -> MainDestination.KeyboardShortcuts
        KaiteyoFeature.StudyHistory -> MainDestination.StudyHistory
        KaiteyoFeature.Statistics -> MainDestination.StatisticsDashboard
        KaiteyoFeature.Search -> MainDestination.SearchEngine
        KaiteyoFeature.BulkActions -> MainDestination.BulkActions
        KaiteyoFeature.ImportExport -> MainDestination.ImportExport
        KaiteyoFeature.Backup -> MainDestination.BackupManager
        KaiteyoFeature.Plugins -> MainDestination.PluginManager
        KaiteyoFeature.UndoHistory -> MainDestination.UndoHistory
    }
}
