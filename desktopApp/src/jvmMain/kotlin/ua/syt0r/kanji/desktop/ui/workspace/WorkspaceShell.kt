package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.NavLayout
import ua.syt0r.kanji.desktop.appstate.NavPosition
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsToastHostView
import ua.syt0r.kanji.desktop.designsystem.DsToolbarDivider
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.model.ReviewRating
import ua.syt0r.kanji.desktop.ui.activity.ActivityLogView
import ua.syt0r.kanji.desktop.ui.about.ContributionsView
import ua.syt0r.kanji.desktop.ui.collections.CollectionsView
import ua.syt0r.kanji.desktop.ui.dashboard.DashboardView
import ua.syt0r.kanji.desktop.ui.library.LibraryView
import ua.syt0r.kanji.desktop.ui.account.AccountView
import ua.syt0r.kanji.desktop.ui.palette.CommandPaletteOverlay
import ua.syt0r.kanji.desktop.ui.plugins.PluginsView
import ua.syt0r.kanji.desktop.ui.review.ReviewView
import ua.syt0r.kanji.desktop.ui.settings.SettingsView
import ua.syt0r.kanji.desktop.ui.shortcuts.ShortcutsView
import ua.syt0r.kanji.desktop.ui.stats.StatsView
import ua.syt0r.kanji.desktop.ui.sync.SyncView
import ua.syt0r.kanji.desktop.ui.tags.TagFlagView
import ua.syt0r.kanji.desktop.ui.themes.ThemeStudioView
import ua.syt0r.kanji.desktop.ui.transfer.TransferView
import ua.syt0r.kanji.desktop.ui.dictionary.DictionaryManagerView
import ua.syt0r.kanji.desktop.ui.media.MediaView
import ua.syt0r.kanji.desktop.ui.browser_web.LearningBrowserView
import ua.syt0r.kanji.desktop.ui.ocr.OcrView
import ua.syt0r.kanji.desktop.ui.mining.MiningView
import ua.syt0r.kanji.desktop.ui.mining.MiningDialog
import ua.syt0r.kanji.desktop.ui.api.IntegrationsView
import ua.syt0r.kanji.desktop.ui.editor.CardEditorDialog
import ua.syt0r.kanji.desktop.ui.grammar.GrammarPracticeView
import ua.syt0r.kanji.desktop.ui.writing.WritingPracticeView
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.theme.tweenDuration

/** Shared AppState accessor for every view in the suite. */
val LocalAppState = staticCompositionLocalOf<AppState> { error("No AppState in composition") }

@Composable
fun rememberAppState(): AppState = LocalAppState.current

// ============================================
// KAITEYO WORKSPACE — adaptive shell
// The navigation dock lives on any of the four
// edges, animates between Expanded / Compact /
// Hidden, and degrades to a dedicated tab bar in
// compact windows. Global keyboard dispatch,
// command palette and toast host live here.
// ============================================

@Composable
fun KaiteyoWorkspace(state: AppState) {
    var paletteOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val d = state.shortcutDispatcher
        d.register("command-palette") { paletteOpen = true }
        d.register("quick-switch") { paletteOpen = true }
        d.register("toggle-nav") { state.cycleNavLayout() }
        d.register("focus-search") { state.currentView = WorkspaceView.Browser }
        d.register("open-dashboard") { state.currentView = WorkspaceView.Dashboard }
        d.register("open-browser") { state.currentView = WorkspaceView.Browser }
        d.register("open-library") { state.currentView = WorkspaceView.Library }
        d.register("open-review") { state.currentView = WorkspaceView.Review }
        d.register("open-writing") { state.currentView = WorkspaceView.Writing }
        d.register("open-grammar") { state.currentView = WorkspaceView.Grammar }
        d.register("open-stats") { state.currentView = WorkspaceView.Statistics }
        d.register("open-settings") { state.currentView = WorkspaceView.Settings }
        d.register("open-themes") { state.currentView = WorkspaceView.ThemeStudio }
        d.register("open-history") { state.currentView = WorkspaceView.History }
        d.register("open-transfer") { state.currentView = WorkspaceView.Transfer }
        d.register("open-dictionary") { state.currentView = WorkspaceView.Dictionary }
        d.register("open-mining") { state.currentView = WorkspaceView.Mining }
        d.register("open-media") { state.currentView = WorkspaceView.Media }
        d.register("open-browser2") { state.currentView = WorkspaceView.LearningBrowser }
        d.register("open-ocr") { state.currentView = WorkspaceView.Ocr }
        d.register("open-integrations") { state.currentView = WorkspaceView.Integrations }
        d.register("mine-selection") { if (state.browserEngine.selectedText != null) state.mining.openMining() }

        d.register("again") { if (state.reviewSession != null) state.rateCurrent(ReviewRating.Again) }
        d.register("hard") { if (state.reviewSession != null) state.rateCurrent(ReviewRating.Hard) }
        d.register("good") { if (state.reviewSession != null) state.rateCurrent(ReviewRating.Good) }
        d.register("easy") { if (state.reviewSession != null) state.rateCurrent(ReviewRating.Easy) }
        d.register("show-answer") { if (state.reviewSession != null) state.answerRevealed = true }
        d.register("undo") { if (state.reviewSession != null) state.undoLast() }
        d.register("suspend") { if (state.reviewSession != null) state.suspendCurrent() }
        d.register("bury") { if (state.reviewSession != null) state.buryCurrent() }
        d.register("skip") { if (state.reviewSession != null) state.skipCurrent() }
        d.register("retry") { if (state.reviewSession != null) state.retryCurrent() }
    }

    CompositionLocalProvider(LocalAppState provides state) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .focusable()
                .onKeyEvent { handleGlobalKey(state, it) }
        ) {
            val compact = maxWidth < 720.dp
            Box(Modifier.fillMaxSize()) {
                DsToastHostView(host = state.toastHost, modifier = Modifier.fillMaxSize()) {
                    WorkspaceLayout(state, compact = compact, onOpenPalette = { paletteOpen = true })
                }

                if (paletteOpen) {
                    CommandPaletteOverlay(state = state, onDismiss = { paletteOpen = false })
                }

                if (state.mining.miningDialogOpen) {
                    MiningDialog(state)
                }

                if (state.editingCard != null) {
                    CardEditorDialog(state)
                }

                if (state.launcherEnabled) {
                    DsFloatingLauncher(state)
                }
            }
        }
    }
}

@Composable
private fun WorkspaceLayout(state: AppState, compact: Boolean, onOpenPalette: () -> Unit) {
    if (compact) {
        CompactLayout(state, onOpenPalette)
    } else {
        DesktopLayout(state, onOpenPalette)
    }
}

/** Compact windows get a real tab bar — never a shrunk desktop dock. */
@Composable
private fun CompactLayout(state: AppState, onOpenPalette: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        if (state.compactNavPosition == NavPosition.Top) {
            DsCompactNavBar(state)
        }
        ContentColumn(state, onOpenPalette, Modifier.weight(1f).fillMaxWidth())
        if (state.compactNavPosition != NavPosition.Top) {
            DsCompactNavBar(state)
        }
    }
}

/** Desktop dock on any edge, with animated layout states. Bubble mode hides the dock entirely. */
@Composable
private fun DesktopLayout(state: AppState, onOpenPalette: () -> Unit) {
    val sc = surfaceColors()
    // The dock is visible in Expanded and Compact modes; Bubble mode
    // removes it entirely and hands navigation to the floating launcher.
    val dockVisible = state.navLayout != NavLayout.Bubble
    val position = state.navPosition
    // Dock show/hide honors the animation speed / reduced-motion config,
    // matching the view transitions.
    val dockDuration = tweenDuration(LocalAnimationConfig.current, 240)
    val motion = tween<IntOffset>(dockDuration)
    val fadeMotion = tween<Float>(dockDuration)
    val rail = position == NavPosition.Left || position == NavPosition.Right

    Box(Modifier.fillMaxSize().background(sc.background)) {
        if (rail) {
            Row(Modifier.fillMaxSize()) {
                if (position == NavPosition.Left) {
                    AnimatedVisibility(
                        visible = dockVisible,
                        enter = slideInHorizontally(motion) { -it } + fadeIn(fadeMotion),
                        exit = slideOutHorizontally(motion) { -it } + fadeOut(fadeMotion),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        DsNavRail(state, onOpenPalette)
                    }
                    ContentColumn(state, onOpenPalette, Modifier.weight(1f).fillMaxHeight())
                } else {
                    ContentColumn(state, onOpenPalette, Modifier.weight(1f).fillMaxHeight())
                    AnimatedVisibility(
                        visible = dockVisible,
                        enter = slideInHorizontally(motion) { it } + fadeIn(fadeMotion),
                        exit = slideOutHorizontally(motion) { it } + fadeOut(fadeMotion),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        DsNavRail(state, onOpenPalette)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                if (position == NavPosition.Top) {
                    AnimatedVisibility(
                        visible = dockVisible,
                        enter = slideInVertically(motion) { -it } + fadeIn(fadeMotion),
                        exit = slideOutVertically(motion) { -it } + fadeOut(fadeMotion),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DsNavBar(state, onOpenPalette)
                    }
                    ContentColumn(state, onOpenPalette, Modifier.weight(1f).fillMaxWidth())
                } else {
                    ContentColumn(state, onOpenPalette, Modifier.weight(1f).fillMaxWidth())
                    AnimatedVisibility(
                        visible = dockVisible,
                        enter = slideInVertically(motion) { it } + fadeIn(fadeMotion),
                        exit = slideOutVertically(motion) { it } + fadeOut(fadeMotion),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DsNavBar(state, onOpenPalette)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentColumn(state: AppState, onOpenPalette: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        DsTopBar(state = state, onOpenPalette = onOpenPalette)
        DsToolbarDivider()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    WorkspaceContent(state)
                    DsFloatingPanelLayer(state)
                }
                DsDockColumn(state)
            }
        }
    }
}

private fun handleGlobalKey(state: AppState, event: KeyEvent): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val name = keyName(event.key)
    return state.shortcutDispatcher.handle(
        pressedKey = name,
        ctrl = event.isCtrlPressed,
        shift = event.isShiftPressed,
        alt = event.isAltPressed,
        meta = event.isMetaPressed
    )
}

/** Normalize a [Key] to the string format used by KeyChord. */
private fun keyName(key: Key): String = when (key) {
    Key.Zero -> "0"; Key.One -> "1"; Key.Two -> "2"; Key.Three -> "3"
    Key.Four -> "4"; Key.Five -> "5"; Key.Six -> "6"; Key.Seven -> "7"
    Key.Eight -> "8"; Key.Nine -> "9"
    Key.A -> "a"; Key.B -> "b"; Key.C -> "c"; Key.D -> "d"; Key.E -> "e"; Key.F -> "f"
    Key.G -> "g"; Key.H -> "h"; Key.I -> "i"; Key.J -> "j"; Key.K -> "k"; Key.L -> "l"
    Key.M -> "m"; Key.N -> "n"; Key.O -> "o"; Key.P -> "p"; Key.Q -> "q"; Key.R -> "r"
    Key.S -> "s"; Key.T -> "t"; Key.U -> "u"; Key.V -> "v"; Key.W -> "w"; Key.X -> "x"
    Key.Y -> "y"; Key.Z -> "z"
    Key.Spacebar -> " "
    Key.Enter -> "enter"
    Key.Comma -> "comma"
    Key.Slash -> "/"
    Key.Delete -> "delete"
    Key.Backspace -> "backspace"
    Key.Escape -> "escape"
    Key.Tab -> "tab"
    else -> key.toString()
}

@Composable
private fun WorkspaceContent(state: AppState) {
    // Tab switches slide + fade between views. The direction follows the
    // navigation order — moving forward slides left, moving back slides
    // right — and the duration honors the animation speed / reduced-motion
    // configuration.
    val duration = tweenDuration(LocalAnimationConfig.current, 280)
    val slideMotion = tween<IntOffset>(duration)
    val fadeMotion = tween<Float>(duration)

    AnimatedContent(
        targetState = state.currentView,
        contentKey = { view ->
            // Browser is a legacy alias for Library — collapse it so
            // navigating between them doesn't slide identical content.
            if (view == WorkspaceView.Browser) WorkspaceView.Library else view
        },
        transitionSpec = {
            val forward = WorkspaceView.entries.indexOf(targetState) >=
                WorkspaceView.entries.indexOf(initialState)
            val enterSlide =
                if (forward) slideInHorizontally(slideMotion) { it }
                else slideInHorizontally(slideMotion) { -it }
            val exitSlide =
                if (forward) slideOutHorizontally(slideMotion) { -it / 3 }
                else slideOutHorizontally(slideMotion) { it / 3 }
            (enterSlide + fadeIn(fadeMotion)) togetherWith
                (exitSlide + fadeOut(fadeMotion))
        },
        label = "workspaceView"
    ) { view ->
        when (view) {
            WorkspaceView.Dashboard -> DashboardView(state)
            // The Library IS the browser — universal search, browsing and
            // editing all live there. Older entry points that navigated to
            // the standalone Browser land here now.
            WorkspaceView.Browser -> LibraryView(state)
            WorkspaceView.Library -> LibraryView(state)
            WorkspaceView.Dictionary -> DictionaryManagerView(state)
            WorkspaceView.Mining -> MiningView(state)
            WorkspaceView.Media -> MediaView(state)
            WorkspaceView.LearningBrowser -> LearningBrowserView(state)
            WorkspaceView.Ocr -> OcrView(state)
            WorkspaceView.Integrations -> IntegrationsView(state)
            WorkspaceView.Review -> ReviewView(state)
            WorkspaceView.Writing -> WritingPracticeView(state)
            WorkspaceView.Grammar -> GrammarPracticeView(state)
            WorkspaceView.Collections -> CollectionsView(state)
            WorkspaceView.Tags -> TagFlagView(state)
            WorkspaceView.Statistics -> StatsView(state)
            WorkspaceView.History -> ActivityLogView(state)
            WorkspaceView.Transfer -> TransferView(state)
            WorkspaceView.Sync -> SyncView(state)
            WorkspaceView.Shortcuts -> ShortcutsView(state)
            WorkspaceView.Plugins -> PluginsView(state)
            WorkspaceView.ThemeStudio -> ThemeStudioView(state)
            WorkspaceView.Settings -> SettingsView(state)
            WorkspaceView.Account -> AccountView(state)
            WorkspaceView.Contributions -> ContributionsView(state)
        }
    }
}

// ============================================
// TOP BAR
// ============================================

@Composable
private fun DsTopBar(state: AppState, onOpenPalette: () -> Unit) {
    val sc = surfaceColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = state.currentView.label,
                color = sc.textPrimary,
                fontSize = DsType.Title,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Kaiteyo Desktop — ${state.cards.size} cards loaded",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(DsRadius.Md))
                .background(sc.surfaceElevated)
                .clickable(onClick = onOpenPalette)
                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = sc.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Search or jump to…",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(DsRadius.Sm))
                        .background(sc.surfaceInteractive)
                        .padding(horizontal = DsSpacing.Xs, vertical = 2.dp)
                ) {
                    Text(
                        text = "Ctrl K",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
        }
        DsIconButton(
            icon = Icons.Default.Settings,
            onClick = { state.currentView = WorkspaceView.Settings },
            contentDescription = "Settings"
        )
        DsPanelMenuButton(state)
    }
}

// ============================================
// KEY-NAME HELPER FOR REVIEW SHORTCUT REBINDING UI
// ============================================

/** Inverse mapping so the Shortcuts screen can render chord labels. */
fun chordLabelFor(key: String): String = when (key) {
    " " -> "Space"
    "enter" -> "Enter"
    "comma" -> ","
    else -> key
}
