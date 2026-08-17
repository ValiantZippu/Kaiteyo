@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ua.syt0r.kanji.presentation.screen.main.screen.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.app_data.ImportDeckWord
import ua.syt0r.kanji.core.app_data.WordClassification
import ua.syt0r.kanji.core.game.GameCourse
import ua.syt0r.kanji.core.game.GameEvaluator
import ua.syt0r.kanji.core.game.GameNode
import ua.syt0r.kanji.core.game.GameNodeKind
import ua.syt0r.kanji.core.game.GameNodeResult
import ua.syt0r.kanji.core.game.GameNodeState
import ua.syt0r.kanji.core.game.GameProgressData
import ua.syt0r.kanji.core.game.GameProgressStore
import ua.syt0r.kanji.core.game.GameSnapshot
import ua.syt0r.kanji.core.game.KanjiSource
import ua.syt0r.kanji.core.game.kaiteyoWorld
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenData
import ua.syt0r.kanji.presentation.screen.main.screen.info.toInfoScreenData

// ============================================================
// KAITEYO WORLD — a node-based curriculum running on top of
// the user's real learning state.
//
// • Kanji nodes auto-track: InProgress once any kanji has an
//   SRS card, Completed when every kanji is mastered, and the
//   whole path unlocks in prerequisite order.
// • Kana + vocabulary nodes are completed explicitly (honest
//   labels explain why), with a "Mark mastered" toggle.
// • Every kanji / word in a node opens its real Info screen,
//   and the header's XP/level/rank come from actual completions.
// ============================================================

@Composable
fun GameScreen(navigationState: MainNavigationState) {
    val dataCenter = koinInject<KaiteyoDataCenter>()
    val gameStore = koinInject<GameProgressStore>()
    val appDataRepository = koinInject<AppDataRepository>()

    var progress by remember { mutableStateOf(GameProgressData()) }
    var contentReady by remember { mutableStateOf(false) }
    var vocabByNode by remember { mutableStateOf<Map<String, List<ImportDeckWord>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        dataCenter.ensureLoaded()
        progress = gameStore.load()
        val resolved = mutableMapOf<String, List<ImportDeckWord>>()
        kaiteyoWorld.courses.flatMap { it.nodes }
            .filter { it.vocabClassification != null }
            .forEach { node ->
                val level = node.vocabClassification!!.removePrefix("n").toIntOrNull()
                if (level != null) {
                    resolved[node.id] = runCatching {
                        appDataRepository.getImportDeckWords(WordClassification.JLPT(level).dbValue)
                    }.getOrDefault(emptyList())
                }
            }
        vocabByNode = resolved
        contentReady = true
    }

    if (dataCenter.isLoading || !contentReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading Kaiteyo World…", color = LocalSurfaceColors.current.textMuted)
        }
        return
    }
    if (dataCenter.loadError) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Could not load learning data", color = LocalSurfaceColors.current.textMuted)
        }
        return
    }

    // Resolve JLPT / grade / frequency sources against the live dataset.
    val resolvedWorld = remember(contentReady, dataCenter.cards.size) {
        kaiteyoWorld.copy(
            courses = kaiteyoWorld.courses.map { course ->
                course.copy(
                    nodes = course.nodes.map { node ->
                        if (node.kanjiSource == KanjiSource.Static) node
                        else node.copy(kanji = resolveKanji(node, dataCenter))
                    }
                )
            }
        )
    }

    val studiedKanji = dataCenter.srsCards.keys.toSet()
    val masteredKanji = dataCenter.cards.filter { it.status == CardStatus.Mature }.map { it.id }.toSet()

    val snapshot = remember(resolvedWorld, studiedKanji, masteredKanji, progress) {
        GameEvaluator.evaluate(resolvedWorld, studiedKanji, masteredKanji, progress)
    }

    val scope = rememberCoroutineScope()
    var expandedNodeId by remember { mutableStateOf<String?>(null) }

    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(Modifier.fillMaxSize()) {
        GameHeader(snapshot = snapshot, accent = accent, surfaceColors = surfaceColors)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            snapshot.courses.forEach { courseResult ->
                item(key = "course-${courseResult.course.id}") {
                    GameCourseCard(
                        courseResult = courseResult,
                        expandedNodeId = expandedNodeId,
                        vocabByNode = vocabByNode,
                        studiedKanji = studiedKanji,
                        masteredKanji = masteredKanji,
                        accent = accent,
                        surfaceColors = surfaceColors,
                        onToggleNode = { expandedNodeId = if (expandedNodeId == it) null else it },
                        onOpenKanji = { char ->
                            navigationState.navigate(MainDestination.Info(InfoScreenData.Letter(char)))
                        },
                        onOpenVocab = { entry ->
                            scope.launch {
                                val full = appDataRepository.getWord(entry.id, entry.kanji, entry.kana)
                                if (full != null) {
                                    navigationState.navigate(MainDestination.Info(full.toInfoScreenData()))
                                }
                            }
                        },
                        onOpenBrowser = { navigationState.navigate(MainDestination.KanjiBrowser()) },
                        onToggleComplete = { node, isCompleted ->
                            scope.launch {
                                progress = if (isCompleted) gameStore.uncompleteNode(node.id, progress)
                                else gameStore.completeNode(node.id, progress)
                            }
                        },
                        onToggleMastered = { node ->
                            scope.launch { progress = gameStore.toggleMastered(node.id, progress) }
                        }
                    )
                }
            }
            item(key = "game-footnote") {
                Text(
                    text = "Kanji nodes track your real study: a node completes when every kanji in it is mastered in your SRS. Kana and vocabulary nodes are marked complete by you after studying them.",
                    color = surfaceColors.textMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun GameHeader(
    snapshot: GameSnapshot,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val progressFraction = if (snapshot.xpForNextLevel > 0) {
        (snapshot.xpIntoLevel.toFloat() / snapshot.xpForNextLevel).coerceIn(0f, 1f)
    } else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Kaiteyo World",
                    color = surfaceColors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A path through the language, built from your own study.",
                    color = surfaceColors.textMuted,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.primary.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = snapshot.rank,
                        color = accent.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Level ${snapshot.level}",
                        color = surfaceColors.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = accent.primary,
                trackColor = surfaceColors.surfaceInteractive
            )
            Text(
                text = "${snapshot.xp} XP",
                color = surfaceColors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("${snapshot.completedNodes}/${snapshot.totalNodes}", "nodes", accent.primary, surfaceColors)
            StatPill("${snapshot.studiedKanji}", "kanji studied", Color(0xFF7BC8FF), surfaceColors)
            StatPill("${snapshot.masteredKanji}", "mastered", Color(0xFFC2FC8B), surfaceColors)
        }
    }
}

@Composable
private fun StatPill(value: String, label: String, color: Color, surfaceColors: SurfaceColors) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceColors.surface)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(label, color = surfaceColors.textMuted, fontSize = 10.sp)
    }
}

@Composable
private fun GameCourseCard(
    courseResult: ua.syt0r.kanji.core.game.GameCourseResult,
    expandedNodeId: String?,
    vocabByNode: Map<String, List<ImportDeckWord>>,
    studiedKanji: Set<String>,
    masteredKanji: Set<String>,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    onToggleNode: (String) -> Unit,
    onOpenKanji: (String) -> Unit,
    onOpenVocab: (ImportDeckWord) -> Unit,
    onOpenBrowser: () -> Unit,
    onToggleComplete: (GameNode, Boolean) -> Unit,
    onToggleMastered: (GameNode) -> Unit
) {
    val course = courseResult.course
    val fraction = if (courseResult.total == 0) 0f else courseResult.completed.toFloat() / courseResult.total

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(course.icon, fontSize = 18.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = course.title,
                    color = surfaceColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = course.subtitle,
                    color = surfaceColors.textMuted,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "${courseResult.completed}/${courseResult.total}",
                color = accent.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = accent.primary,
            trackColor = surfaceColors.surfaceInteractive
        )

        courseResult.nodes.forEach { nodeResult ->
            val expanded = expandedNodeId == nodeResult.node.id
            GameNodeRow(
                nodeResult = nodeResult,
                expanded = expanded,
                accent = accent,
                surfaceColors = surfaceColors,
                onClick = { onToggleNode(nodeResult.node.id) }
            )
            if (expanded) {
                GameNodeDetail(
                    nodeResult = nodeResult,
                    vocab = vocabByNode[nodeResult.node.id].orEmpty(),
                    studiedKanji = studiedKanji,
                    masteredKanji = masteredKanji,
                    accent = accent,
                    surfaceColors = surfaceColors,
                    onOpenKanji = onOpenKanji,
                    onOpenVocab = onOpenVocab,
                    onOpenBrowser = onOpenBrowser,
                    onToggleComplete = onToggleComplete,
                    onToggleMastered = onToggleMastered
                )
            }
        }
    }
}

@Composable
private fun GameNodeRow(
    nodeResult: GameNodeResult,
    expanded: Boolean,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    onClick: () -> Unit
) {
    val node = nodeResult.node
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        if (hovered || expanded) surfaceColors.surfaceInteractive else Color.Transparent
    )
    val (icon, iconColor) = nodeStatusVisual(nodeResult.state, accent, surfaceColors)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 16.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = node.title,
                color = surfaceColors.textPrimary,
                fontSize = 13.sp,
                fontWeight = if (nodeResult.state == GameNodeState.Mastered) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = node.subtitle,
                color = surfaceColors.textMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (node.kind == GameNodeKind.Kanji && node.kanji.isNotEmpty()) {
            Text(
                text = "${nodeResult.masteredCount}/${nodeResult.totalCount}",
                color = if (nodeResult.masteredCount == nodeResult.totalCount) Color(0xFFC2FC8B) else surfaceColors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text("+${node.xp} XP", color = iconColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = surfaceColors.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun GameNodeDetail(
    nodeResult: GameNodeResult,
    vocab: List<ImportDeckWord>,
    studiedKanji: Set<String>,
    masteredKanji: Set<String>,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    onOpenKanji: (String) -> Unit,
    onOpenVocab: (ImportDeckWord) -> Unit,
    onOpenBrowser: () -> Unit,
    onToggleComplete: (GameNode, Boolean) -> Unit,
    onToggleMastered: (GameNode) -> Unit
) {
    val node = nodeResult.node
    val isCompleted = nodeResult.state == GameNodeState.Completed || nodeResult.state == GameNodeState.Mastered
    val isMastered = nodeResult.state == GameNodeState.Mastered

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surfaceInteractive.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = node.description,
            color = surfaceColors.textSecondary,
            fontSize = 12.sp
        )

        when (node.kind) {
            GameNodeKind.Kanji -> {
                Text(
                    text = if (nodeResult.totalCount == 0) "No kanji in this node yet"
                    else "${nodeResult.masteredCount} of ${nodeResult.totalCount} mastered — tap a kanji for its detail page.",
                    color = surfaceColors.textMuted,
                    fontSize = 11.sp
                )
                if (node.kanji.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        node.kanji.forEach { char ->
                            GameKanjiCell(
                                char = char,
                                isStudied = char in studiedKanji,
                                isMastered = char in masteredKanji,
                                accent = accent,
                                surfaceColors = surfaceColors,
                                onClick = { onOpenKanji(char) }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onOpenBrowser) {
                        Text("Open kanji browser", color = accent.primary, fontSize = 12.sp)
                    }
                }
            }

            GameNodeKind.Vocabulary -> {
                if (vocab.isEmpty()) {
                    Text(
                        text = "Words resolve from the JLPT classification in your dictionary. Add the ${node.vocabClassification?.uppercase() ?: ""} vocabulary deck in Library to study them.",
                        color = surfaceColors.textMuted,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = "${vocab.size} words — tap one for its detail page.",
                        color = surfaceColors.textMuted,
                        fontSize = 11.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        vocab.take(12).forEach { word ->
                            GameVocabRow(
                                word = word,
                                accent = accent,
                                surfaceColors = surfaceColors,
                                onClick = { onOpenVocab(word) }
                            )
                        }
                        if (vocab.size > 12) {
                            Text(
                                text = "…and ${vocab.size - 12} more",
                                color = surfaceColors.textMuted,
                                fontSize = 10.sp
                            )
                        }
                        if (vocab.size > 12) {
                            Text(
                                text = "…and ${vocab.size - 12} more",
                                color = surfaceColors.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isCompleted) {
                        TextButton(onClick = { onToggleComplete(node, false) }) {
                            Text("Mark complete", color = accent.primary, fontSize = 12.sp)
                        }
                    } else {
                        TextButton(onClick = { onToggleComplete(node, true) }) {
                            Text("Undo complete", color = surfaceColors.textMuted, fontSize = 12.sp)
                        }
                        if (!isMastered) {
                            TextButton(onClick = { onToggleMastered(node) }) {
                                Text("Mark mastered ★", color = Color(0xFFFFD93D), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            GameNodeKind.Kana -> {
                Text(
                    text = "Kana doesn't carry per-character SRS data in this build, so completion is manual — mark it done once you can read and write it confidently.",
                    color = surfaceColors.textMuted,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isCompleted) {
                        TextButton(onClick = { onToggleComplete(node, false) }) {
                            Text("Mark complete", color = accent.primary, fontSize = 12.sp)
                        }
                    } else {
                        TextButton(onClick = { onToggleComplete(node, true) }) {
                            Text("Undo complete", color = surfaceColors.textMuted, fontSize = 12.sp)
                        }
                        if (!isMastered) {
                            TextButton(onClick = { onToggleMastered(node) }) {
                                Text("Mark mastered ★", color = Color(0xFFFFD93D), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = accent.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Prerequisites: " + if (node.prerequisites.isEmpty()) "none — start here"
                else node.prerequisites.joinToString(", "),
                color = surfaceColors.textMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun GameKanjiCell(
    char: String,
    isStudied: Boolean,
    isMastered: Boolean,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background = when {
        isMastered -> accent.primary.copy(alpha = 0.28f)
        isStudied -> accent.primary.copy(alpha = 0.12f)
        else -> surfaceColors.surfaceInteractive
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            fontSize = 20.sp,
            color = surfaceColors.textPrimary
        )
        if (isMastered) {
            Text(
                text = "✓",
                color = Color(0xFFC2FC8B),
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp)
            )
        }
        if (hovered && !isMastered) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent.primary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp).size(12.dp)
            )
        }
    }
}

@Composable
private fun GameVocabRow(
    word: ImportDeckWord,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        if (hovered) surfaceColors.surfaceInteractive else Color.Transparent
    )
    val expression = word.kanji ?: word.kana

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = expression,
            color = surfaceColors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${word.kana} · ${word.meaning.orEmpty().take(40)}",
            color = surfaceColors.textMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun nodeStatusVisual(
    state: GameNodeState,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
): Pair<String, Color> = when (state) {
    GameNodeState.Locked -> "🔒" to surfaceColors.textMuted
    GameNodeState.Available -> "▶" to accent.primary
    GameNodeState.InProgress -> "◐" to Color(0xFFFFD93D)
    GameNodeState.Completed -> "✓" to Color(0xFFC2FC8B)
    GameNodeState.Mastered -> "★" to Color(0xFFFFD93D)
}

/** Resolves a node's kanji from its [KanjiSource] against the live dataset. */
private fun resolveKanji(node: GameNode, dataCenter: KaiteyoDataCenter): List<String> = when (val source = node.kanjiSource) {
    KanjiSource.Static -> node.kanji
    is KanjiSource.Classification -> dataCenter.classifications.entries
        .filter { it.value.contains(source.value) }
        .map { it.key }
        .sortedBy { dataCenter.frequencies[it] ?: Int.MAX_VALUE }
    is KanjiSource.TopFrequency -> dataCenter.cards
        .filter { (dataCenter.frequencies[it.id] ?: Int.MAX_VALUE) <= source.maxRank }
        .map { it.id }
        .sortedBy { dataCenter.frequencies[it] ?: Int.MAX_VALUE }
}
