package ua.syt0r.kanji.desktop.ui.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.media.SubtitleParser
import ua.syt0r.kanji.desktop.engine.navigation.LauncherSnapMath
import ua.syt0r.kanji.desktop.engine.theming.colorToHex
import ua.syt0r.kanji.desktop.engine.stats.HeatmapEngine
import ua.syt0r.kanji.desktop.ui.stats.heatColor
import ua.syt0r.kanji.desktop.ui.writing.WritingCanvas
import ua.syt0r.kanji.desktop.ui.writing.WritingCanvasState
import ua.syt0r.kanji.presentation.common.resources.brand.BrandMark
import ua.syt0r.kanji.presentation.common.theme.AllAccentSchemes
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig

// ============================================
// PRODUCT TUTORIAL — progressive onboarding
//
// A chaptered walkthrough of how Kaiteyo works,
// deliberately NOT a setup wizard (that stays in
// OnboardingWizard). Every major surface gets a
// small live preview built from the real design
// system and the real engines where practical:
// the navigation demo uses the actual snap math,
// the search previews search the real card pool,
// the heatmap preview renders real summaries.
//
// Controls: skip · continue · back · jump to any
// chapter · finish immediately. Per-chapter
// completion persists (Settings → General →
// Product tutorial → replay a single chapter).
// ============================================

data class TutorialChapter(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

val tutorialChapters = listOf(
    TutorialChapter("welcome", "Welcome", "What Kaiteyo is — in one minute", Icons.Default.AutoAwesome),
    TutorialChapter("navigation", "Navigation", "Sidebar and the floating bubble", Icons.Default.ViewSidebar),
    TutorialChapter("library", "Library", "Decks, modes and your card pool", Icons.Default.LibraryBooks),
    TutorialChapter("study", "Study", "Reviews, SRS and daily targets", Icons.Default.PlayArrow),
    TutorialChapter("writing", "Writing", "Handwriting practice that counts", Icons.Default.Create),
    TutorialChapter("browse", "Browse", "Search every kanji, word and card", Icons.Default.MenuBook),
    TutorialChapter("media", "Media", "Anime, subtitles and immersion", Icons.Default.VideoLibrary),
    TutorialChapter("mining", "Mining", "Turn anything you read into cards", Icons.Default.Usb),
    TutorialChapter("stats", "Stats", "Heatmaps, streaks and honest numbers", Icons.Default.BarChart),
    TutorialChapter("customization", "Customization", "Theme, layout and settings search", Icons.Default.Palette),
    TutorialChapter("done", "Done", "You're ready — go study", Icons.Default.CheckCircle)
)

private val LAST_INDEX = tutorialChapters.lastIndex

/**
 * When Settings → replay-a-chapter is used, this holds the target chapter
 * id for the next TutorialOverlay composition to jump to, then clears.
 */
object TutorialReplayTarget {
    var value: String? = null
}

@Composable
fun TutorialOverlay(state: AppState, onClose: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val anim = LocalAnimationConfig.current

    var currentIndex by remember { mutableIntStateOf(0) }
    var direction by remember { mutableIntStateOf(1) }

    fun goTo(index: Int) {
        val clamped = index.coerceIn(0, LAST_INDEX)
        direction = if (clamped > currentIndex) 1 else -1
        currentIndex = clamped
    }

    // Mark a chapter seen when the user leaves it (or finishes it).
    fun advanceFrom(index: Int) {
        val chapter = tutorialChapters[index]
        state.markTutorialChapterComplete(chapter.id)
    }

    // A replay request from Settings jumps straight to that chapter.
    LaunchedEffect(Unit) {
        TutorialReplayTarget.value?.let { target ->
            val idx = tutorialChapters.indexOfFirst { it.id == target }
            if (idx >= 0) {
                direction = if (idx > currentIndex) 1 else -1
                currentIndex = idx
            }
            TutorialReplayTarget.value = null
        }
    }

    // Entrance animation, same family as the wizard.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entranceScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.97f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 220f),
        label = "tutorialEntranceScale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(if (anim.reducedMotion) 0 else 260),
        label = "tutorialEntranceAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(sc.background)
            .graphicsLayer {
                scaleX = entranceScale
                scaleY = entranceScale
                alpha = entranceAlpha
            }
    ) {
        // Soft ambient glow, same as the wizard.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(ac.primary.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(0.9f, 0.05f),
                        radius = 1200f
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .widthIn(max = 980.dp)
                .padding(horizontal = 32.dp, vertical = 28.dp)
        ) {
            // Chapter rail — jump to any section, see completion.
            ChapterRail(
                state = state,
                current = currentIndex,
                onSelect = { goTo(it) },
                modifier = Modifier.width(220.dp).fillMaxHeight()
            )
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                TutorialHeader(currentIndex)
                Spacer(Modifier.height(16.dp))
                TutorialProgress(currentIndex)
                Spacer(Modifier.height(20.dp))

                val duration = if (anim.reducedMotion) 0 else 220
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        val forward = direction > 0
                        val enterOffset = if (forward) 36 else -36
                        val exitOffset = if (forward) -20 else 20
                        (fadeIn(tween(duration)) +
                            slideInHorizontally(tween(duration)) { it / 12 + enterOffset }) togetherWith
                            (fadeOut(tween(duration)) +
                                slideOutHorizontally(tween(duration)) { -it / 12 + exitOffset })
                    },
                    label = "tutorialChapter",
                    modifier = Modifier.weight(1f)
                ) { index ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        ChapterBody(state, index)
                        Spacer(Modifier.height(16.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                TutorialFooter(
                    index = currentIndex,
                    onBack = { goTo(currentIndex - 1) },
                    onSkipAll = {
                        advanceFrom(currentIndex)
                        goTo(LAST_INDEX)
                    },
                    onNext = {
                        advanceFrom(currentIndex)
                        if (currentIndex < LAST_INDEX) goTo(currentIndex + 1)
                        else {
                            // Finishing marks every chapter complete.
                            tutorialChapters.forEach { state.markTutorialChapterComplete(it.id) }
                            onClose()
                        }
                    }
                )
            }
        }
    }
}

// ============================================
// SHELL PIECES
// ============================================

@Composable
private fun ChapterRail(
    state: AppState,
    current: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(sc.surfaceElevated.copy(alpha = 0.6f))
            .border(1.dp, sc.border.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
            // The real Kaiteyo mark — centralized brand asset, not a "K".
            BrandMark(modifier = Modifier.size(30.dp), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("KAITEYO", color = sc.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("Tutorial", color = sc.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        tutorialChapters.forEachIndexed { index, chapter ->
            val selected = index == current
            val complete = state.tutorialChapterComplete(chapter.id)
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            selected -> ac.primary.copy(alpha = 0.14f)
                            hovered -> sc.surfaceInteractive
                            else -> Color.Transparent
                        }
                    )
                    .clickable(interactionSource = interaction, indication = null) { onSelect(index) }
                    .hoverable(interaction)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    chapter.icon,
                    contentDescription = null,
                    tint = if (selected) ac.primary else sc.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = chapter.title,
                    color = if (selected) ac.primary else sc.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (complete) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = ac.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialHeader(index: Int) {
    val sc = surfaceColors()
    val meta = tutorialChapters[index]
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent().primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                meta.icon,
                contentDescription = null,
                tint = accent().primary,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(meta.title, color = sc.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(meta.subtitle, color = sc.textMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TutorialProgress(index: Int) {
    val sc = surfaceColors()
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        for (i in tutorialChapters.indices) {
            val done = i < index
            val active = i == index
            val bg by animateColorAsState(
                targetValue = when {
                    done -> accent().primary
                    active -> accent().primary.copy(alpha = 0.5f)
                    else -> sc.border.copy(alpha = 0.3f)
                },
                animationSpec = tween(220),
                label = "tutorialProgress"
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(bg)
            )
        }
    }
}

@Composable
private fun TutorialFooter(
    index: Int,
    onBack: () -> Unit,
    onSkipAll: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index > 0) {
            TutorialGhostButton("Back", Icons.Default.ArrowBack, onBack)
        } else {
            Spacer(Modifier.width(1.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (index < LAST_INDEX) {
                Text(
                    text = "Skip all",
                    color = surfaceColors().textMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSkipAll)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            TutorialPrimaryButton(
                text = if (index < LAST_INDEX) "Continue" else "Finish",
                icon = Icons.Default.ArrowForward,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun TutorialPrimaryButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (hovered) ac.primary.copy(alpha = 0.92f) else ac.primary)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text, color = ac.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Icon(icon, contentDescription = null, tint = ac.onPrimary, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun TutorialGhostButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val sc = surfaceColors()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (hovered) sc.surfaceInteractive else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = sc.textSecondary, modifier = Modifier.size(15.dp))
        Text(text, color = sc.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Generic copy block used by most chapters. */
@Composable
private fun CopyBlock(bullets: List<String>) {
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        bullets.forEach { bullet ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent().primary)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = bullet,
                    color = sc.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

// ============================================
// CHAPTER BODIES — each with a live preview
// ============================================

@Composable
private fun ChapterBody(state: AppState, index: Int) {
    when (index) {
        0 -> ChapterWelcome()
        1 -> ChapterNavigation(state)
        2 -> ChapterLibrary(state)
        3 -> ChapterStudy(state)
        4 -> ChapterWriting(state)
        5 -> ChapterBrowse(state)
        6 -> ChapterMedia()
        7 -> ChapterMining(state)
        8 -> ChapterStats(state)
        9 -> ChapterCustomization(state)
        else -> ChapterDone(state)
    }
}

@Composable
private fun ChapterWelcome() {
    val sc = surfaceColors()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CopyBlock(
            listOf(
                "Kaiteyo is a Japanese study environment: decks, SRS reviews, handwriting practice, a dictionary browser, media immersion and mining — all in one window.",
                "Your study data is real: every review updates your decks, your home screen, your stats and your knowledge profile together.",
                "This tour takes about two minutes. Skip any chapter, jump around, or finish right away — you can replay everything from Settings → General → Product tutorial."
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChapterPill(Icons.Default.ViewSidebar, "Navigation")
            ChapterPill(Icons.Default.LibraryBooks, "Decks")
            ChapterPill(Icons.Default.PlayArrow, "SRS")
            ChapterPill(Icons.Default.MenuBook, "Browse")
        }
    }
}

@Composable
private fun ChapterPill(icon: ImageVector, label: String) {
    val sc = surfaceColors()
    val ac = accent()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ac.primary.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ac.primary, modifier = Modifier.size(14.dp))
        Text(label, color = sc.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ============================================
// NAVIGATION — live draggable bubble demo
// (uses the REAL LauncherSnapMath engine)
// ============================================

@Composable
private fun ChapterNavigation(state: AppState) {
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CopyBlock(
            listOf(
                "Sidebar mode docks navigation to any edge and keeps roughly 80% of the window for content.",
                "Floating mode replaces the dock with a bubble. Click it to open the Launchpad, hold or right-click for the mode menu, drag it anywhere — it snaps magnetically to one of 12 edge points when released.",
                "Your mode, position and snap point persist across restarts — and are validated against the current window so nothing ever restores off-screen."
            )
        )
        // Live demo: the same snap math the real bubble uses, in miniature.
        BubbleSnapDemo()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TutorialPill(text = "Drag the bubble below")
        }
    }
}

@Composable
private fun TutorialPill(text: String) {
    val sc = surfaceColors()
    Text(
        text = text,
        color = sc.textMuted,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(sc.surfaceInteractive)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** Mini window with a fully draggable bubble that snaps via LauncherSnapMath. */
@Composable
private fun BubbleSnapDemo() {
    val sc = surfaceColors()
    val ac = accent()
    val demoWidth = 320.dp
    val demoHeight = 180.dp
    val bubbleSize = 40.dp

    val layout = LauncherSnapMath.SnapLayout(
        windowWidth = 320f,
        windowHeight = 180f,
        bubbleSize = 40f,
        edgeInset = 8f
    )
    var dragPos by remember {
        mutableStateOf(LauncherSnapMath.anchorPosition(ua.syt0r.kanji.desktop.appstate.LauncherSnapPoint.BottomRight, layout))
    }
    var target by remember { mutableStateOf(dragPos) }
    var dragging by remember { mutableStateOf(false) }
    val animatedPos by animateOffsetAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 320f),
        label = "tutorialBubblePos"
    )

    Box(
        modifier = Modifier
            .size(demoWidth, demoHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(sc.surface)
            .border(1.dp, sc.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        // Mini content mock so the 20/80 idea reads visually.
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .width(64.dp)
                .fillMaxHeight()
                .padding(vertical = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(sc.surfaceElevated)
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .width(180.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(sc.background)
                .border(1.dp, sc.border.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
        )

        // The draggable bubble.
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedPos.x.roundToInt(), animatedPos.y.roundToInt()) }
                .size(bubbleSize)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragged = false
                        var previous = down.position
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            when (event.type) {
                                PointerEventType.Move -> {
                                    if ((change.position - down.position).getDistance() > 4f) dragged = true
                                    if (dragged) {
                                        change.consume()
                                        dragPos += change.position - previous
                                        target = dragPos
                                        previous = change.position
                                    }
                                }
                                PointerEventType.Release -> {
                                    if (dragged) {
                                        val settled = LauncherSnapMath.settle(dragPos, layout)
                                        dragPos = settled.anchor
                                        target = settled.anchor
                                        dragging = false
                                    }
                                    return@awaitEachGesture
                                }
                                else -> {}
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = if (dragging) 0.92f else 1f; scaleY = if (dragging) 0.92f else 1f }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(ac.primary, ac.primary.copy(alpha = 0.8f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ViewSidebar, contentDescription = null, tint = ac.onPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ============================================
// LIBRARY — mini deck strip + search over real cards
// ============================================

@Composable
private fun ChapterLibrary(state: AppState) {
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CopyBlock(
            listOf(
                "The Library is your study hub. Decks are typed by content (kanji, vocabulary, grammar, radicals, sentences) and each kind has its own study modes.",
                "Every deck shows live new / learning / due counts straight from the learning store — the same numbers Home and Stats use.",
                "The universal search bar at the top of the Library searches decks, entries and the unified learning store together."
            )
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(sc.surface)
                .border(1.dp, sc.border.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Your decks", color = sc.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            val decks = state.library.allDecks().take(3)
            if (decks.isEmpty()) {
                Text("No decks yet — they appear here once you import or create one.", color = sc.textMuted, fontSize = 13.sp)
            } else {
                decks.forEach { deck ->
                    val stats = state.library.deckStats(deck, state.cards.toList())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(accent().primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(deck.kind.glyph, color = accent().primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(deck.name, color = sc.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${stats.total} cards · ${stats.anyDue} due · ${stats.anyNew} new", color = sc.textMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// STUDY — real review flow mock on a real card
// ============================================

@Composable
private fun ChapterStudy(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CopyBlock(
            listOf(
                "Reviews run on spaced repetition: each answer schedules the card further out, and lapses come back sooner.",
                "Grade with Again / Hard / Good / Easy (1–4). Today's target is tracked on Home; every answer updates your summaries, streak and heatmap.",
                "Study time is measured from real engagement — pauses count against you, not for you."
            )
        )
        var revealed by remember { mutableStateOf(false) }
        val sample = remember(state.cards) {
            state.cards.firstOrNull { it.character.isNotEmpty() }
        }
        if (sample != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(sc.surface)
                    .border(1.dp, sc.border.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(sample.character, color = sc.textPrimary, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                if (revealed) {
                    Text(sample.meaning, color = sc.textSecondary, fontSize = 15.sp)
                } else {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { revealed = true }
                            .background(ac.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Show answer", color = ac.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (revealed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReviewDemoButton("Again", sc, ac)
                        ReviewDemoButton("Hard", sc, ac)
                        ReviewDemoButton("Good", sc, ac)
                        ReviewDemoButton("Easy", sc, ac)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewDemoButton(label: String, sc: ua.syt0r.kanji.presentation.common.theme.SurfaceColors, ac: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ac.primary.copy(alpha = 0.14f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = ac.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================
// WRITING — kanji stroke practice preview
// ============================================

@Composable
private fun ChapterWriting(state: AppState) {
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CopyBlock(
            listOf(
                "Writing practice picks kanji from your real pool — weakest and due first — and grades your strokes against the reference stroke order.",
                "Accuracy and attempts are recorded per kanji and surface on Home (weakest area) and Stats.",
                "Launch it from the Writing view, a deck's Writing mode, or the Home quick actions."
            )
        )
        // A REAL attempt: the same canvas the Writing view uses, on a kanji
        // from your pool that the active dataset can actually grade. Draw
        // freehand, reveal, and the real stroke evaluator scores it.
        val sample = remember(state.cards) {
            state.cards.firstOrNull { c ->
                c.character.length == 1 &&
                    c.character[0].code in 0x4E00..0x9FFF &&
                    state.writingEvaluator.supports(c.character)
            }?.character ?: "書"
        }
        val canvasState = remember { WritingCanvasState() }
        var revealed by remember { mutableStateOf(false) }
        WritingCanvas(
            revealed = revealed,
            answer = sample,
            canvasState = canvasState,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TutorialGhostButton(
                text = if (revealed) "Hide answer" else "Reveal answer",
                icon = Icons.Default.Visibility,
                onClick = { revealed = !revealed }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (canvasState.isEmpty) {
                    "Draw $sample with your mouse or stylus, then reveal."
                } else {
                    "${canvasState.strokes.size} stroke(s) drawn — reveal to check them."
                },
                color = sc.textMuted,
                fontSize = 12.sp
            )
        }
        if (revealed && canvasState.strokes.isNotEmpty()) {
            val evaluation = remember(revealed, sample, canvasState.strokes.size) {
                state.writingEvaluator.evaluate(
                    expression = sample,
                    drawnStrokes = canvasState.normalizedStrokes(),
                    canvasWidth = canvasState.canvasSize?.width?.toDouble() ?: 360.0,
                    canvasHeight = canvasState.canvasSize?.height?.toDouble() ?: 360.0
                )
            }
            if (evaluation.supported && evaluation.strokes.isNotEmpty()) {
                DsBadge(
                    text = "${evaluation.correctStrokes}/${evaluation.strokes.size} strokes · ${(evaluation.accuracy * 100).toInt()}% · ${evaluation.sourceLabel}",
                    tint = if (evaluation.accuracy >= 0.7f) successColor() else warningColor()
                )
            } else {
                Text(
                    text = "No reference stroke data for $sample in the active dataset — your attempt is still recorded as real practice.",
                    color = sc.textMuted,
                    fontSize = 12.sp
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TutorialPill(text = "Trace the strokes in order")
            TutorialPill(text = "Real accuracy, real attempts")
            TutorialPill(text = "Practice appears on your heatmap")
        }
    }
}

// ============================================
// BROWSE — search the real card pool live
// ============================================

@Composable
private fun ChapterBrowse(state: AppState) {
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CopyBlock(
            listOf(
                "Browse searches your actual card pool — kanji, vocabulary, readings, meanings, decks and JLPT tags — not a curated sample.",
                "Typing shows merged results: decks first, then matching entries with their live learning state.",
                "Every result opens a detail page with Study / Write / Add to deck actions."
            )
        )
        var query by remember { mutableStateOf("") }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(sc.surface)
                .border(1.dp, sc.border.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DsSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Try 食, mizu, water…"
            )
            val results = remember(query, state.cards.size) {
                val q = query.trim()
                if (q.isEmpty()) emptyList()
                else state.cards.asSequence()
                    .filter {
                        it.character.contains(q) || it.readings.any { r -> r.contains(q) } ||
                            it.meaning.contains(q, ignoreCase = true)
                    }
                    .take(4)
                    .toList()
            }
            if (query.isNotBlank() && results.isEmpty()) {
                Text("No matches — try a different word.", color = sc.textMuted, fontSize = 12.sp)
            } else {
                results.forEach { card ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(card.character, color = sc.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp))
                        Column(Modifier.weight(1f)) {
                            Text(card.meaning, color = sc.textSecondary, fontSize = 13.sp, maxLines = 1)
                            Text(card.readings.take(2).joinToString(" · "), color = sc.textMuted, fontSize = 11.sp, maxLines = 1)
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent().primary.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("Open", color = accent().primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// MEDIA — static player preview
// ============================================

@Composable
private fun ChapterMedia() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CopyBlock(
            listOf(
                "The Media workspace plays video and audio with subtitle overlays — hover a subtitle to look it up instantly, mine it, or jump to the dictionary.",
                "Screenshot capture, frame stepping, A–B repeat and playback speed are built in.",
                "A mini player keeps playing while you browse anywhere else in the app."
            )
        )
        // Interactive timeline demo — the subtitles are parsed by the REAL
        // SubtitleParser, cue lookup uses the same model the player uses, and
        // the A–B loop behaves like the real player's range loop.
        MediaTimelineDemo()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TutorialPill(text = "Scrub the timeline")
            TutorialPill(text = "Set A and B to loop a line")
        }
    }
}

/**
 * Mini media player: an auto-advancing playhead over a real parsed subtitle
 * track, a scrubber (tap or drag), and an A–B range loop — the same model
 * the real player exposes, in a self-contained demo.
 */
@Composable
private fun MediaTimelineDemo() {
    val sc = surfaceColors()
    val ac = accent()
    // Real parser on a short demo script — same cue model as the player.
    val track = remember {
        SubtitleParser.parseSrt(
            """
            00:00:00,400 --> 00:00:02,100
            そういうことか…

            00:00:02,300 --> 00:00:04,600
            分かった、行こう。

            00:00:04,800 --> 00:00:07,200
            また明日ね。
            """.trimIndent(),
            name = "demo"
        )
    }
    val durationMs = track.cues.maxOf { it.endMs }.coerceAtLeast(1)
    var positionMs by remember { mutableStateOf(0L) }
    var playing by remember { mutableStateOf(true) }
    var loopStart by remember { mutableStateOf<Long?>(null) }
    var loopEnd by remember { mutableStateOf<Long?>(null) }

    // Playhead: bounded coroutine, cancelled with the composition. Loops
    // inside the A–B range once both markers are set (real loop semantics).
    LaunchedEffect(playing, loopStart, loopEnd) {
        while (playing) {
            delay(100)
            val rangeStart = loopStart ?: 0L
            val rangeEnd = loopEnd?.takeIf { it > rangeStart } ?: durationMs
            positionMs = if (positionMs + 100 >= rangeEnd) rangeStart else positionMs + 100
        }
    }

    val activeCue = track.cueAt(positionMs)

    fun msAt(x: Float, width: Float): Long =
        ((x / width.coerceAtLeast(1f)) * durationMs).toLong().coerceIn(0, durationMs)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(sc.surface)
            .border(1.dp, sc.border.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Video area with the live subtitle overlay.
        Box(
            Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(sc.background)
        ) {
            Text(
                text = "demo.srt · ${track.cues.size} cues",
                color = sc.textMuted,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (activeCue != null) sc.surfaceElevated.copy(alpha = 0.95f)
                        else sc.surfaceInteractive.copy(alpha = 0.5f)
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = activeCue?.text ?: "…",
                    color = if (activeCue != null) sc.textPrimary else sc.textMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (!playing) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = sc.textMuted.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Center).size(26.dp)
                )
            }
        }

        // Timeline — scrub (tap or drag), subtitle markers, A–B range. Same
        // visual language as the real seek bar in the player.
        Box(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset -> positionMs = msAt(offset.x, size.width.toFloat()) }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> positionMs = msAt(offset.x, size.width.toFloat()) },
                        onDrag = { change, _ ->
                            change.consume()
                            positionMs = msAt(change.position.x, size.width.toFloat())
                        }
                    )
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val trackY = size.height / 2
                val playedX = (positionMs.toFloat() / durationMs) * w
                // base track
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f),
                    topLeft = Offset(0f, trackY - 1.dp.toPx() / 2),
                    size = Size(w, 1.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                // played region
                drawRoundRect(
                    color = ac.primary,
                    topLeft = Offset(0f, trackY - 1.dp.toPx() / 2),
                    size = Size(playedX, 1.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                // subtitle start markers
                track.cues.forEach { cue ->
                    val x = (cue.startMs.toFloat() / durationMs) * w
                    if (x in 0f..w) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = Offset(x, trackY - 4.dp.toPx()),
                            end = Offset(x, trackY + 4.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
                // A–B loop range
                val a = loopStart
                val b = loopEnd
                if (a != null && b != null && b > a) {
                    val ax = (a.toFloat() / durationMs) * w
                    val bx = (b.toFloat() / durationMs) * w
                    drawRect(
                        color = ac.primary.copy(alpha = 0.25f),
                        topLeft = Offset(ax, trackY - 3.dp.toPx()),
                        size = Size((bx - ax).coerceAtLeast(0f), 6.dp.toPx())
                    )
                }
                // thumb
                drawCircle(Color.White, 5.dp.toPx(), Offset(playedX, trackY))
                drawCircle(ac.primary, 3.dp.toPx(), Offset(playedX, trackY))
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                MediaEngine.formatTime(positionMs),
                color = sc.textPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(MediaEngine.formatTime(durationMs), color = sc.textMuted, fontSize = 10.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DemoPillButton(
                icon = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                onClick = { playing = !playing }
            )
            DemoPillButton(
                text = "A · ${loopStart?.let { MediaEngine.formatTime(it) } ?: "set"}",
                onClick = {
                    loopStart = positionMs
                    if (loopEnd != null && positionMs >= loopEnd!!) loopEnd = null
                }
            )
            DemoPillButton(
                text = "B · ${loopEnd?.let { MediaEngine.formatTime(it) } ?: "set"}",
                onClick = {
                    loopEnd = positionMs
                    if (loopStart != null && positionMs <= loopStart!!) loopStart = null
                }
            )
            if (loopStart != null && loopEnd != null && loopEnd!! > loopStart!!) {
                DemoPillButton(text = "Clear loop", onClick = { loopStart = null; loopEnd = null })
            }
            Spacer(Modifier.weight(1f))
            if (activeCue != null) {
                Text(
                    text = "${MediaEngine.formatTime(activeCue.startMs)} – ${MediaEngine.formatTime(activeCue.endMs)}",
                    color = sc.textMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/** Small pill button used by the tutorial demos. */
@Composable
private fun DemoPillButton(
    text: String = "",
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (hovered) sc.surfaceInteractive else ac.primary.copy(alpha = 0.1f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = ac.primary, modifier = Modifier.size(13.dp))
        if (text.isNotEmpty()) Text(text, color = sc.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ============================================
// MINING — preview of a mined card
// ============================================

@Composable
private fun ChapterMining(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CopyBlock(
            listOf(
                "Mining turns anything you encounter into a card: dictionary popup → “Create card”, subtitle → “Mine”, OCR, clipboard, browser selection.",
                "Every mine carries source context — the sentence, and optionally a screenshot or audio clip.",
                "Mined cards flow straight into your deck pool, SRS and stats like any other card."
            )
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(sc.surface)
                .border(1.dp, sc.border.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Example mine", color = sc.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("食べる", color = sc.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(76.dp))
                Column(Modifier.weight(1f)) {
                    Text("たべる — to eat", color = sc.textSecondary, fontSize = 13.sp)
                    Text("Context: 「ご飯を食べる」 · source: Subtitle · clip: 2.4s", color = sc.textMuted, fontSize = 11.sp)
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(ac.primary.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Mined", color = ac.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ============================================
// STATS — real heatmap preview
// ============================================

@Composable
private fun ChapterStats(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CopyBlock(
            listOf(
                "Stats is honest: every number comes from real reviews, writing attempts and activity — nothing is fabricated.",
                "The heatmap shows each day's activity intensity (blank = nothing studied), with hover tooltips and a full day-detail view on click.",
                "Streaks, retention, JLPT coverage and due forecasts all read from the same summaries."
            )
        )
        val grid = remember(state.summaries.size) {
            HeatmapEngine.buildAligned(state.summaries, weeks = 26)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(sc.surface)
                .border(1.dp, sc.border.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Your activity — last 26 weeks", color = sc.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                grid.weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        week.days.forEach { day ->
                            Box(
                                Modifier
                                    .size(11.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (day == null) sc.surfaceInteractive.copy(alpha = 0.4f) else heatColor(day.level, sc, ac))
                            )
                        }
                    }
                    Spacer(Modifier.width(3.dp))
                }
            }
            Text("Blank days had no study activity — the color density is real intensity.", color = sc.textMuted, fontSize = 11.sp)
        }
    }
}

// ============================================
// CUSTOMIZATION — live accent + layout hint
// ============================================

@Composable
private fun ChapterCustomization(state: AppState) {
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CopyBlock(
            listOf(
                "Everything is configurable from Settings — navigation mode, bubble behavior, sidebar width, motion, and a full theme studio.",
                "Settings has instant search: type “floating” or “heatmap” and jump straight to the matching controls.",
                "The floating bubble, sidebar and launchpad all remember your choices across restarts."
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AllAccentSchemes.take(8).forEach { scheme ->
                val interaction = remember { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(scheme.primary)
                        .border(2.dp, if (hovered) Color.White else sc.border, CircleShape)
                        .clickable(interactionSource = interaction, indication = null) {
                            state.themeManager.updateActiveColors { c ->
                                c.copy(
                                    primary = colorToHex(scheme.primary),
                                    primaryDark = colorToHex(scheme.primaryDark),
                                    secondary = colorToHex(scheme.secondary),
                                    secondaryDark = colorToHex(scheme.secondaryDark),
                                    tertiary = scheme.tertiary?.let { colorToHex(it) } ?: c.tertiary,
                                    onPrimary = colorToHex(scheme.onPrimary),
                                    onSecondary = colorToHex(scheme.onSecondary)
                                )
                            }
                        }
                        .hoverable(interaction)
                )
            }
        }
        Text(
            text = "Tap a swatch — the whole app re-themes live. (Settings → Appearance → Theme Studio for the full editor.)",
            color = sc.textMuted,
            fontSize = 12.sp
        )
    }
}

// ============================================
// DONE
// ============================================

@Composable
private fun ChapterDone(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(ac.primary, ac.secondary ?: ac.primary))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ac.onPrimary, modifier = Modifier.size(34.dp))
        }
        Text("You're ready", color = sc.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            text = if (state.dueCount() > 0) {
                "You have ${state.dueCount()} cards due right now. Finish the tour and start reviewing!"
            } else {
                "Nothing is due right now — a perfect moment to explore the Library or mine something new."
            },
            color = sc.textMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChapterPill(Icons.Default.PlayArrow, "Review")
            ChapterPill(Icons.Default.LibraryBooks, "Library")
            ChapterPill(Icons.Default.MenuBook, "Browse")
        }
    }
}


