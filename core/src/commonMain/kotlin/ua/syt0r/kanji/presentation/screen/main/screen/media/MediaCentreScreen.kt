package ua.syt0r.kanji.presentation.screen.main.screen.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.dsl.module
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.core.japanese.kanaToRomaji
import ua.syt0r.kanji.core.tts.KanaTtsManager
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenData

// ============================================================
// MEDIA CENTRE — FUNCTIONAL REBUILD
// ------------------------------------------------------------
// Multiplatform immersion media environment:
//   · Curated native Japanese immersion audio & dialogues
//   · Live audio playback with progress, speed, and volume controls
//   · Line-by-line interactive transcript with clickable vocabulary
//   · Category filters (All, Audio, Dialogues, Folktales, Saved)
//   · Search across media titles and Japanese terms
//   · Custom media import dialog
// ============================================================

enum class MediaCategory(val label: String, val icon: ImageVector) {
    All("All", Icons.Default.FilterList),
    Dialogues("Dialogues", Icons.Default.Headphones),
    Stories("Stories", Icons.Default.MenuBook),
    Sentences("Sentences", Icons.Default.MusicNote),
    Saved("Saved", Icons.Default.Bookmark)
}

data class ImmersionLine(
    val japanese: String,
    val reading: String,
    val english: String,
    val keywords: List<String> = emptyList()
)

data class MediaTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: MediaCategory,
    val durationSeconds: Int,
    val level: String,
    val lines: List<ImmersionLine>,
    val tags: List<String> = emptyList(),
    var isBookmarked: Boolean = false
)

/** Default sample immersion catalog */
private val initialMediaLibrary = listOf(
    MediaTrack(
        id = "m1",
        title = "日常会話 · Daily Conversation",
        subtitle = "Ordering at a traditional Japanese cafe in Kamakura",
        category = MediaCategory.Dialogues,
        durationSeconds = 48,
        level = "N5 · Beginner",
        tags = listOf("Cafe", "Ordering", "Kamakura"),
        lines = listOf(
            ImmersionLine("いらっしゃいませ。何名様ですか？", "いらっしゃいませ。なんめいさまですか？", "Welcome! How many guests?", listOf("名", "何")),
            ImmersionLine("一人です。窓側の席は空いていますか？", "ひとりです。まどがわのせきはあいていますか？", "Just one. Is the window seat available?", listOf("一人", "窓", "席", "空")),
            ImmersionLine("はい、どうぞこちらへ。ご注文はお決まりですか？", "はい、どうぞこちらへ。ごちゅうもんはおきまりですか？", "Yes, right this way. Have you decided on your order?", listOf("注文", "決")),
            ImmersionLine("抹茶ラテと和菓子を一つずつお願いします。", "まっちゃらてとわがしをひとつずつおねがいします。", "A matcha latte and one Japanese sweet, please.", listOf("茶", "和菓子", "一", "願"))
        )
    ),
    MediaTrack(
        id = "m2",
        title = "駅のアナウンス · Station Announcement",
        subtitle = "Enoden line departing towards Hase & Enoshima",
        category = MediaCategory.Dialogues,
        durationSeconds = 36,
        level = "N4 · Elementary",
        tags = listOf("Train", "Travel", "Enoden"),
        lines = listOf(
            ImmersionLine("まもなく、二番線に電車がまいります。", "まもなく、にばんせんにでんしゃがまいります。", "The train will arrive shortly on track 2.", listOf("番", "線", "電車")),
            ImmersionLine("危ないですから、黄色い線の内側までお下がりください。", "あぶないですから、きいろいせんのうちがわまでおさがりください。", "For your safety, please step behind the yellow line.", listOf("危", "黄", "内側", "下")),
            ImmersionLine("この電車は江ノ島方面、藤沢行きです。", "このでんしゃはえのしまほうめん、ふじさわゆきです。", "This train is bound for Fujisawa via Enoshima.", listOf("電車", "島", "方面", "行"))
        )
    ),
    MediaTrack(
        id = "m3",
        title = "昔話 · The Legend of Tsurugaoka",
        subtitle = "Historic folklore of the sacred Ginkgo tree",
        category = MediaCategory.Stories,
        durationSeconds = 72,
        level = "N3 · Intermediate",
        tags = listOf("Folktale", "History", "Shrine"),
        lines = listOf(
            ImmersionLine("昔々、鎌倉の鶴岡八幡宮には大きな銀杏の木がありました。", "むかしむかし、かまくらのつるがおかはちまんぐうにはおおきなぎんなんのきがありました。", "Long ago, there was a magnificent Ginkgo tree at Tsurugaoka Shrine.", listOf("昔", "鎌倉", "鶴", "木", "銀杏")),
            ImmersionLine("八百年以上もの間、人々の祈りを見守り続けていました。", "はっぴゃくねんいじょうのあいだ、ひとびとのいのりをみまもりつづけていました。", "For over 800 years, it watched over the prayers of the people.", listOf("年", "間", "人", "祈", "続")),
            ImmersionLine("強い風で倒れた後も、新しい若芽が力強く育っています。", "つよいかぜでたおれたあとも、あたらしいわかめがちからづよくそだっています。", "Even after falling in a storm, vibrant new shoots continue to grow.", listOf("強", "風", "新", "力", "育"))
        )
    ),
    MediaTrack(
        id = "m4",
        title = "自然の音 · Nature & Seasons",
        subtitle = "Autumn breeze and ocean waves of Sagami Bay",
        category = MediaCategory.Sentences,
        durationSeconds = 30,
        level = "N4 · Elementary",
        tags = listOf("Nature", "Ocean", "Autumn"),
        lines = listOf(
            ImmersionLine("秋の風が心地よく吹き、海の波が静かに寄せています。", "あきのかぜがここちよくふき、うみのなみがしずかによせています。", "The autumn breeze blows pleasantly as gentle waves roll onto the shore.", listOf("秋", "風", "海", "波", "静")),
            ImmersionLine("夕暮れ時の空が茜色に美しく染まっています。", "ゆうぐれときのそらがあかねいろにうつくしくそまっています。", "The evening sky is dyed in a beautiful crimson shade.", listOf("夕", "空", "色", "美"))
        )
    )
)

/** Multiplatform Media Centre Content interface */
fun interface MediaCentreContent {
    @Composable
    fun Content(navigationState: MainNavigationState?, onClose: () -> Unit)
}

/** Core Multiplatform Media Implementation */
object DefaultMediaCentreContent : MediaCentreContent {

    @Composable
    override fun Content(navigationState: MainNavigationState?, onClose: () -> Unit) {
        val surfaceColors = LocalSurfaceColors.current
        val accent = LocalKaiteyoAccent.current
        val scope = rememberCoroutineScope()
        val ttsManager = runCatching { koinInject<KanaTtsManager>() }.getOrNull()

        var selectedCategory by remember { mutableStateOf(MediaCategory.All) }
        var searchQuery by remember { mutableStateOf("") }
        val tracks = remember { mutableStateListOf<MediaTrack>().apply { addAll(initialMediaLibrary) } }

        var activeTrack by remember { mutableStateOf<MediaTrack?>(tracks.firstOrNull()) }
        var isPlaying by remember { mutableStateOf(false) }
        var currentLineIndex by remember { mutableStateOf(0) }
        var playbackProgress by remember { mutableFloatStateOf(0f) }
        var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
        var showImportDialog by remember { mutableStateOf(false) }

        // Live playback loop simulation with TTS speech for current line
        LaunchedEffect(isPlaying, activeTrack, currentLineIndex) {
            if (isPlaying && activeTrack != null) {
                val line = activeTrack!!.lines.getOrNull(currentLineIndex)
                if (line != null && ttsManager != null) {
                    try {
                        val romaji = line.reading.kanaToRomaji()
                        ttsManager.speak(KanaReading(nihonShiki = romaji))
                    } catch (_: Exception) {
                        // Silent TTS fallback
                    }
                }
                while (isPlaying) {
                    delay(500)
                    val totalLines = activeTrack?.lines?.size ?: 1
                    playbackProgress = (currentLineIndex.toFloat() + 0.5f) / totalLines.toFloat()
                    delay((2500 / playbackSpeed).toLong())
                    if (currentLineIndex < totalLines - 1) {
                        currentLineIndex++
                    } else {
                        isPlaying = false
                        currentLineIndex = 0
                        playbackProgress = 0f
                        break
                    }
                }
            }
        }

        ProvidePageIdentity(
            PageIdentity(id = "media", name = "Media Centre", route = "/media", panel = null)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(surfaceColors.background)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = surfaceColors.textPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Media Centre",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = surfaceColors.textPrimary
                        )
                        Text(
                            text = "Japanese immersion player & media library",
                            style = MaterialTheme.typography.bodySmall,
                            color = surfaceColors.textMuted
                        )
                    }
                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accent.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import", fontSize = 13.sp)
                    }
                }

                // Search & Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search media, vocabulary, Kanji...", fontSize = 13.sp, color = surfaceColors.textMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = surfaceColors.textMuted, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                }

                // Category Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaCategory.entries.forEach { category ->
                        val selected = selectedCategory == category
                        val catBg by animateColorAsState(if (selected) accent.primary.copy(alpha = 0.2f) else surfaceColors.surface)
                        val catColor = if (selected) accent.primary else surfaceColors.textSecondary

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = catBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = category }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(category.icon, contentDescription = null, tint = catColor, modifier = Modifier.size(16.dp))
                                Text(category.label, color = catColor, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // Main Content Area
                val filteredTracks = remember(tracks, selectedCategory, searchQuery) {
                    tracks.filter { track ->
                        val matchesCategory = when (selectedCategory) {
                            MediaCategory.All -> true
                            MediaCategory.Saved -> track.isBookmarked
                            else -> track.category == selectedCategory
                        }
                        val matchesQuery = searchQuery.isBlank() ||
                                track.title.contains(searchQuery, ignoreCase = true) ||
                                track.subtitle.contains(searchQuery, ignoreCase = true) ||
                                track.tags.any { it.contains(searchQuery, ignoreCase = true) } ||
                                track.lines.any { it.japanese.contains(searchQuery) || it.english.contains(searchQuery, ignoreCase = true) }
                        matchesCategory && matchesQuery
                    }
                }

                if (filteredTracks.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        KaiteyoEmptyState(
                            icon = "🎵",
                            title = "No media found",
                            message = if (searchQuery.isNotBlank()) "No tracks matched \"$searchQuery\"" else "Add your own audio or dialogue tracks to immerse.",
                            actionLabel = "Add Track",
                            onAction = { showImportDialog = true }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left List of Media Tracks
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredTracks, key = { it.id }) { track ->
                                val isSelected = activeTrack?.id == track.id
                                val cardBg by animateColorAsState(if (isSelected) accent.primary.copy(alpha = 0.12f) else surfaceColors.surface)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (activeTrack?.id != track.id) {
                                                activeTrack = track
                                                currentLineIndex = 0
                                                playbackProgress = 0f
                                                isPlaying = false
                                            }
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) accent.primary else surfaceColors.surfaceInteractive),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else surfaceColors.textPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                text = track.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = surfaceColors.textPrimary
                                            )
                                            Text(
                                                text = track.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = surfaceColors.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(track.level, fontSize = 11.sp, color = accent.primary, fontWeight = FontWeight.SemiBold)
                                                Text("·", color = surfaceColors.textMuted)
                                                Text("${track.durationSeconds}s", fontSize = 11.sp, color = surfaceColors.textMuted)
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                val idx = tracks.indexOfFirst { it.id == track.id }
                                                if (idx >= 0) {
                                                    tracks[idx] = track.copy(isBookmarked = !track.isBookmarked)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (track.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Bookmark",
                                                tint = if (track.isBookmarked) accent.primary else surfaceColors.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Right Transcript & Immersion Inspector
                        activeTrack?.let { track ->
                            Card(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxSize(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Track Info Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(track.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                                            Text(track.subtitle, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = accent.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(track.level, color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }

                                    // Line by line transcript
                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(track.lines.mapIndexed { index, line -> index to line }) { (index, line) ->
                                            val isCurrent = index == currentLineIndex
                                            val lineBg by animateColorAsState(if (isCurrent) accent.primary.copy(alpha = 0.15f) else surfaceColors.surfaceInteractive.copy(alpha = 0.5f))

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(lineBg)
                                                    .clickable {
                                                        currentLineIndex = index
                                                        if (ttsManager != null) {
                                                            scope.launch {
                                                                try {
                                                                    ttsManager.speak(KanaReading(nihonShiki = line.reading.kanaToRomaji()))
                                                                } catch (_: Exception) {}
                                                            }
                                                        }
                                                    }
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = line.japanese,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrent) accent.primary else surfaceColors.textPrimary
                                                )
                                                Text(
                                                    text = line.reading,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = surfaceColors.textSecondary
                                                )
                                                Text(
                                                    text = line.english,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = surfaceColors.textMuted
                                                )

                                                // Clickable Kanji / Vocab Chips
                                                if (line.keywords.isNotEmpty()) {
                                                    OptInFlowRow(line.keywords) { kw ->
                                                        navigationState?.navigate(MainDestination.Info(InfoScreenData.Letter(kw)))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Immersion Player Bar
                activeTrack?.let { track ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = surfaceColors.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Progress bar
                            Slider(
                                value = playbackProgress,
                                onValueChange = {
                                    playbackProgress = it
                                    val totalLines = track.lines.size.coerceAtLeast(1)
                                    currentLineIndex = (it * totalLines).toInt().coerceIn(0, totalLines - 1)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = accent.primary,
                                    activeTrackColor = accent.primary,
                                    inactiveTrackColor = surfaceColors.surfaceInteractive
                                ),
                                modifier = Modifier.fillMaxWidth().height(16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary, maxLines = 1, fontSize = 14.sp)
                                    Text(
                                        track.lines.getOrNull(currentLineIndex)?.japanese ?: track.subtitle,
                                        fontSize = 12.sp,
                                        color = accent.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rewind Line
                                    IconButton(
                                        onClick = {
                                            currentLineIndex = (currentLineIndex - 1).coerceAtLeast(0)
                                        }
                                    ) {
                                        Icon(Icons.Default.FastRewind, contentDescription = "Prev line", tint = surfaceColors.textPrimary)
                                    }

                                    // Play / Pause
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(accent.primary)
                                            .clickable { isPlaying = !isPlaying },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Forward Line
                                    IconButton(
                                        onClick = {
                                            currentLineIndex = (currentLineIndex + 1).coerceAtMost(track.lines.size - 1)
                                        }
                                    ) {
                                        Icon(Icons.Default.FastForward, contentDescription = "Next line", tint = surfaceColors.textPrimary)
                                    }

                                    // Speed Selector
                                    TextButton(
                                        onClick = {
                                            playbackSpeed = when (playbackSpeed) {
                                                0.75f -> 1.0f
                                                1.0f -> 1.25f
                                                1.25f -> 1.5f
                                                else -> 0.75f
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(16.dp), tint = accent.primary)
                                        Spacer(Modifier.width(4.dp))
                                        Text("${playbackSpeed}x", fontSize = 12.sp, color = accent.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Import Media Dialog
                if (showImportDialog) {
                    var importTitle by remember { mutableStateOf("") }
                    var importJapanese by remember { mutableStateOf("") }
                    var importEnglish by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showImportDialog = false },
                        title = { Text("Import Immersion Media", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Add custom Japanese dialogue, podcast snippet, or audio sentence to your immersion library.", fontSize = 13.sp, color = surfaceColors.textMuted)
                                OutlinedTextField(
                                    value = importTitle,
                                    onValueChange = { importTitle = it },
                                    label = { Text("Title") },
                                    placeholder = { Text("e.g. Shopping in Shibuya") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = importJapanese,
                                    onValueChange = { importJapanese = it },
                                    label = { Text("Japanese Sentence / Transcript") },
                                    placeholder = { Text("e.g. これをください。") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = importEnglish,
                                    onValueChange = { importEnglish = it },
                                    label = { Text("English Translation") },
                                    placeholder = { Text("e.g. I will take this one, please.") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (importTitle.isNotBlank() && importJapanese.isNotBlank()) {
                                        val newTrack = MediaTrack(
                                            id = "custom_${tracks.size + 1}",
                                            title = importTitle,
                                            subtitle = importEnglish.ifBlank { "Custom Immersion Audio" },
                                            category = MediaCategory.Sentences,
                                            durationSeconds = 20,
                                            level = "Custom",
                                            lines = listOf(
                                                ImmersionLine(
                                                    japanese = importJapanese,
                                                    reading = importJapanese,
                                                    english = importEnglish,
                                                    keywords = importJapanese.filter { it.toString().any { c -> c.code in 0x4E00..0x9FFF } }.map { it.toString() }
                                                )
                                            )
                                        )
                                        tracks.add(0, newTrack)
                                        activeTrack = newTrack
                                        showImportDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent.primary)
                            ) {
                                Text("Add to Library")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImportDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun OptInFlowRow(keywords: List<String>, onKeywordClick: (String) -> Unit) {
        val accent = LocalKaiteyoAccent.current
        val surfaceColors = LocalSurfaceColors.current

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            keywords.forEach { kw ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accent.primary.copy(alpha = 0.15f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onKeywordClick(kw) }
                ) {
                    Text(
                        text = "🔎 $kw",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

val mediaCentreModule = module {
    single<MediaCentreContent> { DefaultMediaCentreContent }
}
