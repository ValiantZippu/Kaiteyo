package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================
// KAITEYO IMPORT/EXPORT SYSTEM v1.2
// Anki packages · CSV · JSON · Markdown · TSV
// Clipboard · Drag & Drop · Merge · Replace
// Preview · Conflict resolution · Validation
// ============================================

enum class ImportFormat(val displayName: String, val extension: String) {
    AnkiPackage("Anki Package", ".apkg"),
    CSV("CSV", ".csv"),
    JSON("JSON", ".json"),
    Markdown("Markdown", ".md"),
    TSV("TSV", ".tsv"),
    Text("Plain Text", ".txt")
}

enum class ExportFormat(val displayName: String, val extension: String) {
    CSV("CSV", ".csv"),
    JSON("JSON", ".json"),
    Markdown("Markdown", ".md"),
    Text("Plain Text", ".txt"),
    AnkiCompatible("Anki-Compatible", ".txt")
}

enum class ImportConflictStrategy(val displayName: String) {
    Skip("Skip Duplicates"),
    Replace("Replace Existing"),
    KeepBoth("Keep Both"),
    Merge("Merge Fields"),
    Ask("Ask Each Time")
}

data class ImportPreview(
    val totalCards: Int = 0,
    val newCards: Int = 0,
    val duplicates: Int = 0,
    val errors: Int = 0,
    val conflicts: Int = 0,
    val previewCards: List<ImportPreviewCard> = emptyList()
)

data class ImportPreviewCard(
    val character: String = "",
    val meaning: String = "",
    val reading: String = "",
    val willImport: Boolean = true,
    val isDuplicate: Boolean = false,
    val hasConflict: Boolean = false,
    val issues: List<String> = emptyList()
)

data class ImportResult(
    val imported: Int = 0,
    val skipped: Int = 0,
    val updated: Int = 0,
    val errors: Int = 0,
    val duration: Long = 0L,
    val errorDetails: List<String> = emptyList()
)

data class ExportConfig(
    val format: ExportFormat = ExportFormat.CSV,
    val selectedDecks: List<String> = emptyList(),
    val includeTags: Boolean = true,
    val includeFlags: Boolean = true,
    val includeNotes: Boolean = true,
    val includeHistory: Boolean = false,
    val includeStatistics: Boolean = false,
    val dateRangeStart: String = "",
    val dateRangeEnd: String = "",
    val filteredQuery: String = "",
    val maxCards: Int = 0
)

@Composable
fun ImportExportScreen() {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var selectedTab by remember { mutableStateOf("Import") }
    var selectedFormat by remember { mutableStateOf(ImportFormat.CSV) }
    var conflictStrategy by remember { mutableStateOf(ImportConflictStrategy.Skip) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("Import / Export", style = MaterialTheme.typography.titleLarge,
            color = surfaceColors.textPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Transfer cards between Kaiteyo and other applications",
            color = surfaceColors.textMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Tab bar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Import", "Export", "History", "Settings").forEach { tab ->
                val isSelected = selectedTab == tab
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) accent.primary.copy(alpha = 0.15f) else surfaceColors.surface)
                    .clickable { selectedTab = tab }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Text(tab, color = if (isSelected) accent.primary else surfaceColors.textSecondary,
                        fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        when (selectedTab) {
            "Import" -> ImportTab(accent, surfaceColors, selectedFormat, conflictStrategy)
            "Export" -> ExportTab(accent, surfaceColors)
            "History" -> ImportHistoryTab(accent, surfaceColors)
            "Settings" -> ImportSettingsTab(accent, surfaceColors)
        }
    }
}

@Composable
private fun ImportTab(accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
                       surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
                       selectedFormat: ImportFormat, conflictStrategy: ImportConflictStrategy) {
    Text("Import Cards", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(12.dp))

    // Format selector
    Text("Format", color = surfaceColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ImportFormat.entries.forEach { format ->
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .background(if (format == selectedFormat) accent.primary.copy(alpha = 0.15f) else surfaceColors.surface)
                .border(1.dp, if (format == selectedFormat) accent.primary else surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .clickable { }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(format.extension, color = if (format == selectedFormat) accent.primary else surfaceColors.textSecondary,
                    fontSize = 12.sp, fontWeight = if (format == selectedFormat) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Drop zone
    Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp))
        .background(surfaceColors.surfaceElevated.copy(alpha = 0.5f))
        .border(2.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        .clickable { }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(accent.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center) { Text("↑", color = accent.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Drag & drop files here", color = surfaceColors.textPrimary, fontSize = 14.sp)
            Text("or click to browse", color = accent.primary, fontSize = 12.sp)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Conflict strategy
    Text("Conflict Strategy", color = surfaceColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
    ImportConflictStrategy.entries.forEach { strategy ->
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(if (strategy == conflictStrategy) accent.primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { }.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(16.dp).clip(CircleShape)
                .background(if (strategy == conflictStrategy) accent.primary else surfaceColors.border), contentAlignment = Alignment.Center) {
                if (strategy == conflictStrategy) Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(strategy.displayName, color = surfaceColors.textPrimary, fontSize = 13.sp)
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
    Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary),
        modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Start Import", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

@Composable
private fun ExportTab(accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
                      surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors) {
    Text("Export Cards", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(12.dp))

    Text("Format", color = surfaceColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ExportFormat.entries.forEach { format ->
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .background(surfaceColors.surface).border(1.dp, surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .clickable { }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(format.displayName, color = surfaceColors.textSecondary, fontSize = 12.sp)
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text("Include:", color = surfaceColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))
    listOf("Tags", "Flags", "Notes", "Review History", "Statistics").forEach { item ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(accent.primary), contentAlignment = Alignment.Center) {
                Text("✓", color = accent.onPrimary, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(item, color = surfaceColors.textPrimary, fontSize = 13.sp)
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
    Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary),
        modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Export", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

@Composable
private fun ImportHistoryTab(accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
                              surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors) {
    Text("Import History", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(12.dp))
    Text("No recent imports", color = surfaceColors.textMuted, fontSize = 13.sp)
}

@Composable
private fun ImportSettingsTab(accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
                               surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors) {
    Text("Import Settings", style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(12.dp))
    Text("Import/export configuration coming soon",
        color = surfaceColors.textMuted, fontSize = 13.sp)
}

// ============================================
// GLOBAL SEARCH ENGINE
// ============================================

data class SearchQuery(
    val text: String = "",
    val filters: SearchFilters = SearchFilters(),
    val sortBy: SearchSort = SearchSort.Relevance,
    val page: Int = 1,
    val pageSize: Int = 50
)

data class SearchFilters(
    val decks: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val flags: List<String> = emptyList(),
    val difficulties: List<String> = emptyList(),
    val jlptLevels: List<String> = emptyList(),
    val strokeCountRange: IntRange = 1..50,
    val minInterval: Int = 0,
    val maxInterval: Int = 36500,
    val onlyNew: Boolean = false,
    val onlyDue: Boolean = false,
    val onlySuspended: Boolean = false,
    val onlyFlagged: Boolean = false,
    val onlyFavorite: Boolean = false,
    val onlyArchived: Boolean = false,
    val hasNotes: Boolean = false,
    val hasTags: Boolean = false,
    val hasFlags: Boolean = false,
    val regex: String = "",
    val combineWith: String = "AND" // AND, OR, NOT
)

enum class SearchSort(val displayName: String) {
    Relevance("Relevance"),
    Newest("Newest First"),
    Oldest("Oldest First"),
    DueFirst("Due First"),
    IntervalAsc("Interval ↑"),
    IntervalDesc("Interval ↓"),
    Difficulty("Difficulty"),
    Deck("Deck Name"),
    Character("Character")
}

data class SearchResult(
    val totalResults: Int = 0,
    val results: List<SearchResultItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val searchTime: Long = 0L
)

data class SearchResultItem(
    val cardId: String = "",
    val character: String = "",
    val meaning: String = "",
    val reading: String = "",
    val deck: String = "",
    val tags: List<String> = emptyList(),
    val interval: Int = 0,
    val dueDate: String = "",
    val ease: Float = 2.5f,
    val matchField: String = "",
    val matchSnippet: String = ""
)

// ============================================
// SEARCH UI COMPONENT
// ============================================

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search cards, kanji, meanings...",
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surfaceInteractive)
            .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔍", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (query.isEmpty()) placeholder else query,
            color = if (query.isEmpty()) surfaceColors.textMuted else surfaceColors.textPrimary,
            fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (query.isNotEmpty()) {
            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(surfaceColors.border).clickable { onQueryChange("") },
                contentAlignment = Alignment.Center) { Text("×", color = surfaceColors.textMuted, fontSize = 11.sp) }
        }
    }
}