package ua.syt0r.kanji.desktop.ui.palette

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.BrowserViewMode
import ua.syt0r.kanji.desktop.appstate.PanelKind
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.appstate.togglePanel
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.ui.workspace.allNavItems
import ua.syt0r.kanji.desktop.ui.workspace.panelKindIcon
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.theme.tweenDuration

// ============================================
// COMMAND PALETTE
// Keyboard-first launcher: search views, actions
// and settings. Ctrl+K from anywhere.
// ============================================

data class PaletteCommand(
    val label: String,
    val group: String,
    val icon: ImageVector,
    val hint: String = "",
    val action: () -> Unit
)

@Composable
fun CommandPaletteOverlay(state: AppState, onDismiss: () -> Unit) {
    val sc = surfaceColors()
    var query by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(0) }

    val commands = remember(state) {
        buildCommands(state)
    }
    val filtered = remember(query, commands) {
        val q = query.trim().lowercase()
        if (q.isBlank()) commands
        else commands.filter {
            it.label.lowercase().contains(q) || it.group.lowercase().contains(q) || it.hint.lowercase().contains(q)
        }
    }

    // When a command runs, show a brief drawn-checkmark confirmation
    // before the palette dismisses.
    var confirmed by remember { mutableStateOf<PaletteCommand?>(null) }

    fun execute(command: PaletteCommand) {
        if (confirmed != null) return
        command.action()
        confirmed = command
    }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 96.dp)
                    .width(560.dp)
                    .clip(RoundedCornerShape(DsRadius.Xl))
                    .background(sc.surfaceElevated)
                    .padding(DsSpacing.Md)
            ) {
                DsSearchField(
                    value = query,
                    onValueChange = { query = it; selectedIndex = 0 },
                    placeholder = "Type a command or search…",
                    autoFocus = true,
                    modifier = Modifier
                        .onPreviewKeyEvent { event ->
                            when {
                                event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                                    if (filtered.isNotEmpty()) selectedIndex = (selectedIndex + 1).coerceAtMost(filtered.lastIndex)
                                    true
                                }
                                event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                                    if (filtered.isNotEmpty()) selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                    true
                                }
                                event.type == KeyEventType.KeyDown && event.key == Key.Enter -> {
                                    filtered.getOrNull(selectedIndex)?.let { execute(it) }
                                    true
                                }
                                event.type == KeyEventType.KeyDown && event.key == Key.Escape -> {
                                    onDismiss()
                                    true
                                }
                                else -> false
                            }
                        }
                )
                Spacer(Modifier.height(DsSpacing.Sm))

                if (filtered.isEmpty()) {
                    // Softly fade + slide the empty state in when the query
                    // yields no results, honoring speed / reduced-motion.
                    val duration = tweenDuration(LocalAnimationConfig.current, 220)
                    // Fresh state each time the branch (re)enters composition so
                    // the enter animation runs on appearance; while still empty
                    // across keystrokes it persists and doesn't re-animate.
                    val visibleState = remember {
                        MutableTransitionState(false).apply { targetState = true }
                    }
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn(animationSpec = tween(duration)) +
                            slideInVertically(animationSpec = tween(duration)) { it / 2 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DsSpacing.Xl),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No commands match “$query”",
                                color = sc.textMuted,
                                fontSize = DsType.Body
                            )
                        }
                    }
                } else {
                    // The result list slides one row-height in the direction
                    // the selection moves — arrow keys or typing that jumps
                    // the selection — so the menu feels like it scrolls with
                    // you. Duration honors speed / reduced-motion config.
                    val duration = tweenDuration(LocalAnimationConfig.current, 200)
                    val slideMotion = tween<IntOffset>(duration)
                    val fadeMotion = tween<Float>(duration)
                    // Row height scales with density / display zoom / font
                    // scale, so measure the first rendered row rather than
                    // hardcode it — the slide then lands perfectly on the
                    // next row at any setting.
                    var rowHeightPx by remember { mutableIntStateOf(0) }
                    val slideDistancePx = if (rowHeightPx > 0) rowHeightPx
                        else with(LocalDensity.current) { 40.dp.roundToPx() }
                    AnimatedContent(
                        targetState = selectedIndex,
                        transitionSpec = {
                            val movingDown = targetState > initialState
                            val enter =
                                if (movingDown) slideInVertically(slideMotion) { slideDistancePx }
                                else slideInVertically(slideMotion) { -slideDistancePx }
                            val exit =
                                if (movingDown) slideOutVertically(slideMotion) { -slideDistancePx }
                                else slideOutVertically(slideMotion) { slideDistancePx }
                            (enter + fadeIn(fadeMotion)) togetherWith
                                (exit + fadeOut(fadeMotion))
                        },
                        label = "paletteSelection"
                    ) { index ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                        ) {
                            itemsIndexed(filtered, key = { _, c -> c.group + c.label }) { rowIndex, command ->
                                PaletteRow(
                                    command = command,
                                    selected = rowIndex == index,
                                    onClick = { execute(command) },
                                    // When typing reorders the list (selection
                                    // unchanged), rows glide to their new spot.
                                    modifier = Modifier
                                        .animateItem()
                                        .onGloballyPositioned { coords ->
                                            // Measure once — every row is the
                                            // same height.
                                            if (rowHeightPx == 0) rowHeightPx = coords.size.height
                                        }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(DsSpacing.Sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    Text(
                        text = "↑↓ navigate",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                    Text(
                        text = "Enter select",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                    Text(
                        text = "Esc close",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }

            // Brief drawn-checkmark confirmation before the palette closes.
            confirmed?.let { command ->
                SuccessCheckOverlay(
                    label = command.label,
                    onFinished = onDismiss,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ============================================
// SUCCESS CHECK — drawn + bouncing confirmation
// Played after a command runs, just before the
// palette closes. Honors speed / reduced motion.
// ============================================

@Composable
private fun SuccessCheckOverlay(
    label: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    val config = LocalAnimationConfig.current

    val drawProgress = remember { Animatable(0f) }
    val scale = remember { Animatable(0.55f) }
    val badgeAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (config.reducedMotion) {
            onFinished()
            return@LaunchedEffect
        }
        launch {
            badgeAlpha.animateTo(1f, tween(tweenDuration(config, 120)))
        }
        launch {
            scale.animateTo(1.08f, spring(dampingRatio = 0.42f, stiffness = 480f))
            scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 900f))
        }
        drawProgress.animateTo(
            1f,
            tween(tweenDuration(config, 300), easing = FastOutSlowInEasing)
        )
        delay(tweenDuration(config, 200).toLong())
        onFinished()
    }

    Box(
        modifier = modifier
            .background(Color(0x33000000))
            // Swallow clicks so the confirmation can't be skipped by a stray
            // click (Escape still dismisses immediately).
            .clickable(interactionSource = null, indication = null) { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = badgeAlpha.value
            }
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ac.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(44.dp)) {
                    val stroke = 5.dp.toPx()
                    val check = Path().apply {
                        moveTo(size.width * 0.22f, size.height * 0.52f)
                        lineTo(size.width * 0.44f, size.height * 0.72f)
                        lineTo(size.width * 0.78f, size.height * 0.30f)
                    }
                    val measure = PathMeasure().apply { setPath(check, false) }
                    val partial = Path()
                    measure.getSegment(
                        0f,
                        measure.length * drawProgress.value.coerceIn(0f, 1f),
                        partial,
                        true
                    )
                    drawPath(
                        path = partial,
                        color = ac.primary,
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
            Spacer(Modifier.height(DsSpacing.Sm))
            Text(
                text = label,
                color = sc.textPrimary,
                fontSize = DsType.Body,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Done",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

@Composable
private fun PaletteRow(
    command: PaletteCommand,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.16f)
                    hovered -> sc.surfaceInteractive
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            command.icon,
            contentDescription = null,
            tint = if (selected) ac.primary else sc.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(DsSpacing.Sm))
        Text(
            text = command.label,
            color = if (selected) ac.primary else sc.textPrimary,
            fontSize = DsType.Body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(Modifier.weight(1f))
        if (command.hint.isNotBlank()) {
            Text(
                text = command.hint,
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

private fun buildCommands(state: AppState): List<PaletteCommand> = buildList {

    fun nav(view: WorkspaceView, icon: ImageVector) {
        add(
            PaletteCommand(
                label = "Open ${view.label}",
                group = "Navigate",
                icon = icon,
                hint = view.label,
                action = { state.currentView = view }
            )
        )
    }

    allNavItems.forEach { (view, icon) -> nav(view, icon) }

    // ---- Media workspace commands --------------------------------
    // Hints read the live configurable hotkey bindings (Media → Settings →
    // Keyboard shortcuts) so the palette always tells the truth.
    fun mediaHint(actionId: String): String = state.media.hotkeys.chordLabel(actionId)
    add(PaletteCommand("Open Media", "Media", Icons.Default.PlayArrow, "Alt+V",
        action = { state.currentView = WorkspaceView.Media }))
    add(PaletteCommand("Play / Pause", "Media", Icons.Default.PlayArrow, mediaHint("play-pause"),
        action = { state.media.togglePlay() }))
    add(PaletteCommand("Replay subtitle", "Media", Icons.Default.PlayArrow, mediaHint("replay"),
        action = { state.media.replayCue() }))
    add(PaletteCommand("Mine current sentence", "Media", Icons.Default.Sell, mediaHint("mine"),
        action = { state.media.mineCurrentCue() }))
    add(PaletteCommand("Capture screenshot", "Media", Icons.Default.PhotoCamera, mediaHint("screenshot"),
        action = { state.media.captureScreenshot() }))
    add(PaletteCommand("OCR current frame", "Media", Icons.Default.PhotoCamera, "",
        action = { state.media.ocrFrame() }))
    add(PaletteCommand("Toggle transcript", "Media", Icons.Default.History, mediaHint("transcript"),
        action = { state.media.transcriptOpen = !state.media.transcriptOpen }))
    add(PaletteCommand("Toggle dictionary", "Media", Icons.Default.Tune, mediaHint("dictionary"),
        action = { state.media.toggleDictionaryFromPalette() }))
    add(PaletteCommand("Loop current subtitle", "Media", Icons.Default.PlayArrow, mediaHint("loop"),
        action = { state.media.toggleLoopCue() }))
    add(PaletteCommand("Toggle condensed playback", "Media", Icons.Default.PlayArrow, mediaHint("condensed"),
        action = { state.media.toggleCondensed() }))
    add(PaletteCommand("Next subtitle", "Media", Icons.Default.PlayArrow, mediaHint("next-cue"),
        action = { state.media.replayNextCue() }))
    add(PaletteCommand("Previous subtitle", "Media", Icons.Default.PlayArrow, mediaHint("prev-cue"),
        action = { state.media.replayPreviousCue() }))

    // ---- Exam quick-starts -----------------------------------------
    // Generated from real study state and staged for the Exams view; the
    // palette never fabricates a session. Labels resolve through the suite
    // l10n layer so the palette speaks the app's language.
    fun stageExam(title: String, draft: ua.syt0r.kanji.desktop.engine.learning.ExamDraft?) {
        if (draft == null) {
            state.toastHost.show("No content for $title", kind = ua.syt0r.kanji.desktop.model.ToastKind.Warning)
        } else {
            state.pendingExamDraft = draft
            state.currentView = WorkspaceView.Exams
        }
    }

    add(PaletteCommand("Start ${ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString { weeklyAssessmentLabel }}", "Exams", Icons.Default.School, "",
        action = { stageExam("weekly assessment", state.learning.exams.generateWeekly()) }))
    add(PaletteCommand("Start ${ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString { mistakesReviewLabel }}", "Exams", Icons.Default.School, "",
        action = { stageExam("mistakes review", state.learning.exams.generate(ua.syt0r.kanji.desktop.engine.learning.ExamType.Mistakes, questionCount = 15)) }))

    add(PaletteCommand("Start review (due)", "Review", Icons.Default.PlayArrow, "3",
        action = { state.startReview() }))
    add(PaletteCommand("Start review (new)", "Review", Icons.Default.PlayArrow, "N",
        action = { state.startReview(query = "status:new") }))
    add(PaletteCommand("Preview session", "Review", Icons.Default.Tune, "P",
        action = { state.startReview(query = "", settings = state.reviewSettings.copy(showPreview = true)) }))

    add(PaletteCommand("Toggle preview panel", "Browser", Icons.Default.Tune, "",
        action = { state.browserShowPreview = !state.browserShowPreview }))
    add(PaletteCommand("Switch to grid view", "Browser", Icons.Default.GridView, "G",
        action = { state.browserViewMode = BrowserViewMode.Grid }))

    PanelKind.entries.forEach { kind ->
        add(
            PaletteCommand(
                label = "Toggle ${kind.label} panel",
                group = "Panels",
                icon = panelKindIcon(kind),
                action = { state.togglePanel(kind) }
            )
        )
    }


    add(PaletteCommand("Load stress dataset (10k)", "Developer", Icons.Default.Settings, "",
        action = { state.loadStressDataset(10_000) }))
    add(PaletteCommand("Export active theme JSON", "Theme", Icons.Default.Palette, "",
        action = {
            val json = state.themeManager.exportJson(state.themeManager.activeTheme.id)
            java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(
                java.awt.datatransfer.StringSelection(json),
                null
            )
            state.toastHost.show("Theme JSON copied to clipboard", kind = ua.syt0r.kanji.desktop.model.ToastKind.Success)
        }))
}
