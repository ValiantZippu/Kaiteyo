package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.screen.main.features.StatisticsController
import ua.syt0r.kanji.presentation.screen.main.screen.statistics.StatisticsScreen

// ============================================
// KAITEYO v1.2 — LEARNING POWER HUB
// Central navigation and management screen
// that orchestrates all learning power features
// ============================================

enum class LearningFeature(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
) {
    Tags("tags", "Tags", "Nested, colored tags with full management",
        Icons.Default.Label, Color(0xFFA78BFA)),
    Flags("flags", "Flags", "7-color flag system with search & bulk",
        Icons.Default.Flag, Color(0xFFFF6B6B)),
    Notes("notes", "Notes", "Rich markdown notes with formatting",
        Icons.Default.Description, Color(0xFF7BC8FF)),
    CardBrowser("browser", "Card Browser", "15+ columns, sortable, searchable",
        Icons.Default.TableChart, Color(0xFFFEAB57)),
    DeckBrowser("decks", "Deck Browser", "Nested folders, drag-drop, merge/split",
        Icons.Default.Folder, Color(0xFFC2FC8B)),
    AnkiOps("anki", "Anki Operations", "All SRS operations: suspend/bury/forget/etc",
        Icons.Default.Bolt, Color(0xFFFFD93D)),
    ReviewSettings("review", "Review Settings", "Layouts, buttons, sizes, modes",
        Icons.Default.Settings, Color(0xFFB0B0B0)),
    Shortcuts("shortcuts", "Shortcuts", "Keyboard shortcuts with profiles",
        Icons.Default.Keyboard, Color(0xFFA78BFA)),
    ImportExport("import", "Import/Export", "APKG, CSV, JSON, TXT, Markdown",
        Icons.Default.ImportExport, Color(0xFF7BC8FF)),
    Statistics("stats", "Statistics", "Dashboard, graphs, per-deck, per-card",
        Icons.Default.BarChart, Color(0xFFFEAB57)),
    Search("search", "Search", "Universal search across all fields",
        Icons.Default.Search, Color(0xFFC2FC8B)),
    BulkActions("bulk", "Bulk Actions", "Multi-select operations on cards",
        Icons.Default.SelectAll, Color(0xFFFFD93D)),
    History("history", "History", "Review, edit, import, export history",
        Icons.Default.History, Color(0xFFB0B0B0)),
    Backup("backup", "Backup", "Auto/manual backup with verification",
        Icons.Default.Backup, Color(0xFFA78BFA)),
    Plugins("plugins", "Plugins", "Extension points and plugin management",
        Icons.Default.Extension, Color(0xFF7BC8FF))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPowerHub(
    initialCards: List<KaiteyoCard>,
    initialDecks: List<KaiteyoDeck>,
    initialTags: List<CardTag>,
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    var cards by remember { mutableStateOf(initialCards) }
    var decks by remember { mutableStateOf(initialDecks) }
    var tags by remember { mutableStateOf(initialTags) }
    var selectedFeature by remember { mutableStateOf<LearningFeature?>(null) }
    var showGrid by remember { mutableStateOf(true) }

    // Quick stats
    val totalCards = cards.size
    val dueCards = cards.count { it.status == CardStatus.New || it.status == CardStatus.Learning || it.status == CardStatus.Relearning }
    val matureCards = cards.count { it.status == CardStatus.Mature }
    val flaggedCards = cards.count { it.flag != CardFlagType.None }
    val suspendedCards = cards.count { it.isSuspended || it.status == CardStatus.Suspended }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learning Power Hub") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = { showGrid = !showGrid }) {
                        Icon(if (showGrid) Icons.Default.ViewList else Icons.Default.GridView, "Toggle View")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColors.surface,
                    titleContentColor = surfaceColors.textPrimary
                )
            )
        }
    ) { padding ->
        if (selectedFeature == null) {
            // Main hub view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Quick stats bar
                QuickStatsBar(
                    totalCards = totalCards,
                    dueCards = dueCards,
                    matureCards = matureCards,
                    flaggedCards = flaggedCards,
                    suspendedCards = suspendedCards,
                    surfaceColors = surfaceColors,
                    accent = accent
                )

                Spacer(Modifier.height(8.dp))

                // Feature grid
                if (showGrid) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        userScrollEnabled = false
                    ) {
                        items(LearningFeature.entries) { feature ->
                            FeatureCard(
                                feature = feature,
                                accent = accent,
                                surfaceColors = surfaceColors,
                                onClick = { selectedFeature = feature }
                            )
                        }
                    }
                } else {
                    // List view
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        LearningFeature.entries.forEach { feature ->
                            FeatureListItem(
                                feature = feature,
                                accent = accent,
                                surfaceColors = surfaceColors,
                                onClick = { selectedFeature = feature }
                            )
                            if (feature != LearningFeature.entries.last()) {
                                HorizontalDivider(
                                    color = surfaceColors.border.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Quick actions
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleSmall,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionChip("Study Due ($dueCards)", Icons.Default.PlayArrow, accent.primary) {
                        selectedFeature = LearningFeature.AnkiOps
                    }
                    QuickActionChip("Browse Cards", Icons.Default.TableChart, surfaceColors.textMuted) {
                        selectedFeature = LearningFeature.CardBrowser
                    }
                    QuickActionChip("Review Stats", Icons.Default.BarChart, surfaceColors.textMuted) {
                        selectedFeature = LearningFeature.Statistics
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        } else {
            // Feature screen
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (selectedFeature) {
                    LearningFeature.Tags -> TagManagerScreenFull(
                        tags = tags,
                        cards = cards,
                        onAddTag = { name, color, parentId ->
                            val newTag = CardTag(
                                id = (tags.maxOfOrNull { it.id } ?: 0) + 1,
                                name = name,
                                color = color,
                                parentId = parentId
                            )
                            tags = tags + newTag
                        },
                        onUpdateTag = { id, name, color, parentId ->
                            tags = tags.map { if (it.id == id) it.copy(name = name, color = color, parentId = parentId) else it }
                        },
                        onDeleteTag = { id ->
                            tags = tags.filter { it.id != id }
                            cards = cards.map { card ->
                                card.copy(tagNames = card.tagNames.filter { tn ->
                                    tags.none { t -> t.name == tn && t.id == id }
                                }.toMutableList())
                            }
                        },
                        onMergeTags = { sourceId, targetId ->
                            val source = tags.find { it.id == sourceId }
                            val target = tags.find { it.id == targetId }
                            if (source != null && target != null) {
                                cards = cards.map { card ->
                                    card.copy(
                                        tagNames = card.tagNames.map {
                                            if (it == source.name) target.name else it
                                        }.toMutableList()
                                    )
                                }
                                tags = tags.filter { it.id != sourceId }
                            }
                        },
                        onApplyTagToCards = { tagId, cardIds ->
                            val tag = tags.find { it.id == tagId }
                            if (tag != null) {
                                cards = cards.map { card ->
                                    if (card.id in cardIds && !card.tagNames.contains(tag.name)) {
                                        card.copy(tagNames = (card.tagNames + tag.name).toMutableList())
                                    } else card
                                }
                            }
                        },
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.Flags -> FlagManagerScreenFull(
                        cards = cards,
                        onFlagCard = { cardId, flag ->
                            cards = cards.map { if (it.id == cardId) it.copy(flag = flag) else it }
                        },
                        onBulkFlag = { cardIds, flag ->
                            cards = cards.map { if (it.id in cardIds) it.copy(flag = flag) else it }
                        },
                        onStudyByFlag = { flag ->
                            selectedFeature = null
                        },
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.Notes -> NoteEditorFullScreen(
                        cards = cards,
                        onSaveNote = { cardId, content ->
                            cards = cards.map { if (it.id == cardId) it.copy(notes = content) else it }
                        },
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.CardBrowser -> CardBrowserFullScreen(
                        cards = cards,
                        onFlagCard = { cardId, flag ->
                            cards = cards.map { if (it.id == cardId) it.copy(flag = flag) else it }
                        },
                        onStatusChange = { cardId, status ->
                            cards = cards.map { if (it.id == cardId) it.copy(status = status) else it }
                        },
                        onUpdateCard = { updatedCard ->
                            cards = cards.map { if (it.id == updatedCard.id) updatedCard else it }
                        },
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.DeckBrowser -> DeckBrowserFullScreen(
                        decks = decks,
                        onDeckClick = { deck ->
                            // Switch to card browser filtered by deck
                            selectedFeature = null
                        },
                        onFavorite = { deck ->
                            decks = decks.map { if (it.id == deck.id) it.copy(isFavorite = !it.isFavorite) else it }
                        },
                        onArchive = { deck ->
                            decks = decks.map { if (it.id == deck.id) it.copy(isArchived = !it.isArchived) else it }
                        },
                        onMerge = { source, target ->
                            // Merge logic
                            decks = decks.filter { it.id != source.id }
                        },
                        onMove = { deck, newParent ->
                            decks = decks.map { if (it.id == deck.id) it.copy(parentId = newParent.id) else it }
                        },
                        onCreateDeck = { name, parentId ->
                            val newDeck = KaiteyoDeck(
                                id = "deck_${(decks.size + 1).toString().padStart(3, '0')}",
                                name = name,
                                parentId = parentId
                            )
                            decks = decks + newDeck
                        },
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.AnkiOps -> AnkiOperationsFullScreen(
                        cards = cards,
                        onOperation = { op, affectedCards ->
                            cards = cards.map { card ->
                                if (card.id in affectedCards.map { it.id }) {
                                    when (op) {
                                        CardOperation.SuspendCard -> card.copy(isSuspended = true, status = CardStatus.Suspended)
                                        CardOperation.SuspendNote -> card.copy(isSuspended = true, status = CardStatus.Suspended)
                                        CardOperation.BuryCard -> card.copy(isBuried = true, status = CardStatus.Buried)
                                        CardOperation.BuryNote -> card.copy(isBuried = true, status = CardStatus.Buried)
                                        CardOperation.BurySiblings -> card.copy(isBuried = true, status = CardStatus.Buried)
                                        CardOperation.ForgetCard -> card.copy(status = CardStatus.New, interval = 0, ease = 2.5f, lapses = 0)
                                        CardOperation.ResetProgress -> card.copy(status = CardStatus.New, interval = 0, ease = 2.5f, lapses = 0, reviewCount = 0, accuracy = 0f)
                                        CardOperation.Reposition -> card.copy(priority = 0)
                                        CardOperation.ChangeDueDate -> card.copy(lastReviewed = "")
                                        CardOperation.SetInterval -> card.copy(interval = 30)
                                        CardOperation.PreviewMode -> card
                                        CardOperation.CramMode -> card
                                    }
                                } else card
                            }
                        },
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.ReviewSettings -> ReviewSettingsFullScreen(
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.Shortcuts -> KeyboardShortcutsFullScreen(
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.ImportExport -> ImportExportScreen()
                    LearningFeature.Statistics -> StatisticsScreen(
                        controller = koinInject<StatisticsController>(),
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.Search -> SearchEngineScreen(
                        cards = cards,
                        onSearch = { },
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.BulkActions -> BulkActionsFullScreen(
                        cards = cards,
                        tags = tags,
                        onBulkOperation = { action, cardIds ->
                            when (action) {
                                "delete" -> cards = cards.filter { it.id !in cardIds }
                                "suspend" -> cards = cards.map { if (it.id in cardIds) it.copy(isSuspended = true, status = CardStatus.Suspended) else it }
                                "bury" -> cards = cards.map { if (it.id in cardIds) it.copy(isBuried = true, status = CardStatus.Buried) else it }
                                "archive" -> cards = cards.map { if (it.id in cardIds) it.copy(isArchived = true, status = CardStatus.Archived) else it }
                                "reset" -> cards = cards.map { if (it.id in cardIds) it.copy(status = CardStatus.New, interval = 0) else it }
                                "export" -> { /* trigger export */ }
                            }
                        },
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.History -> HistoryFullScreen(
                        cards = cards,
                        onClose = { selectedFeature = null }
                    )
                    LearningFeature.Backup -> BackupManagerScreen(
                        onDismiss = { selectedFeature = null },
                        onCreateBackup = { },
                        onRestoreBackup = { },
                        onDeleteBackup = { },
                        onVerifyBackup = { },
                        onUpdateConfig = { }
                    )
                    LearningFeature.Plugins -> PluginManagerScreen(
                        onClose = { selectedFeature = null }
                    )
                    null -> {}
                }
            }
        }
    }
}

// ── Quick Stats Bar ──

@Composable
private fun QuickStatsBar(
    totalCards: Int,
    dueCards: Int,
    matureCards: Int,
    flaggedCards: Int,
    suspendedCards: Int,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surfaceElevated)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickStatItem("Total", "$totalCards", Icons.Default.ViewModule, surfaceColors.textPrimary, surfaceColors)
        QuickStatItem("Due", "$dueCards", Icons.Default.Notifications, accent.primary, surfaceColors)
        QuickStatItem("Mature", "$matureCards", Icons.Default.CheckCircle, Color(0xFFC2FC8B), surfaceColors)
        QuickStatItem("Flagged", "$flaggedCards", Icons.Default.Flag, Color(0xFFFF6B6B), surfaceColors)
        QuickStatItem("Suspended", "$suspendedCards", Icons.Default.Block, surfaceColors.textMuted, surfaceColors)
    }
}

@Composable
private fun QuickStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    surfaceColors: SurfaceColors
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(18.dp), tint = color)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        Text(label, fontSize = 10.sp, color = surfaceColors.textMuted)
    }
}

// ── Feature Card (Grid) ──

@Composable
private fun FeatureCard(
    feature: LearningFeature,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(feature.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(feature.icon, null, Modifier.size(24.dp), tint = feature.color)
            }
            Column {
                Text(
                    feature.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = surfaceColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    feature.description,
                    fontSize = 11.sp,
                    color = surfaceColors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ── Feature List Item ──

@Composable
private fun FeatureListItem(
    feature: LearningFeature,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(feature.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(feature.icon, null, Modifier.size(22.dp), tint = feature.color)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(feature.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = surfaceColors.textPrimary)
            Text(feature.description, fontSize = 12.sp, color = surfaceColors.textMuted, maxLines = 1)
        }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = surfaceColors.textMuted)
    }
}

// ── Quick Action Chip ──

@Composable
private fun QuickActionChip(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp, maxLines = 1) },
        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
        modifier = Modifier.height(32.dp),
        shape = RoundedCornerShape(16.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = surfaceColors.surfaceInteractive,
            labelColor = surfaceColors.textPrimary
        )
    )
}
