package ua.syt0r.kanji.desktop.ui.browser_web

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.browser.BrowserEngine
import ua.syt0r.kanji.desktop.engine.browser.ReaderMode
import ua.syt0r.kanji.desktop.engine.browser.RenderMode
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload
import java.io.File

// ============================================
// KAITEYO LEARNING BROWSER
// Lightweight study-focused browser: tabs, address
// bar, back/forward, bookmarks, downloads, history,
// reader mode and one-click dictionary lookup and
// mining of selected text.
// ============================================

@Composable
fun LearningBrowserView(state: AppState) {
    val sc = surfaceColors()
    val browser = state.browserEngine
    var section by remember { mutableStateOf("bookmarks") }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = "Learning Browser",
            subtitle = "A focused browser for reading Japanese — select text to look it up or mine a card.",
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(
                        text = "New tab",
                        icon = Icons.Default.Add,
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = { browser.newTab() }
                    )
                }
            }
        )

        BrowserChrome(state)

        Row(Modifier.fillMaxWidth().height(500.dp), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
            Column(Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                PageCard(state)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                SidePanel(state, section, onSectionChange = { section = it })
            }
        }
    }
}

@Composable
private fun BrowserChrome(state: AppState) {
    val sc = surfaceColors()
    val browser = state.browserEngine
    val activeTab = browser.activeTab

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Md), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            // Tabs strip
            if (browser.tabs.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    browser.tabs.forEach { tab ->
                        Row(
                            modifier = Modifier
                                .background(
                                    if (tab.id == browser.activeTabId) sc.surfaceInteractive
                                    else sc.surfaceElevated
                                )
                                .clickable { browser.activateTab(tab.id) }
                                .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                tab.title.take(20),
                                color = if (tab.id == browser.activeTabId) sc.textPrimary else sc.textSecondary,
                                fontSize = DsType.Caption
                            )
                            DsIconButton(
                                icon = Icons.Default.Close,
                                onClick = { browser.closeTab(tab.id) },
                                contentDescription = "Close tab",
                                size = 20.dp
                            )
                        }
                    }
                }
            }

            // Address bar + actions
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsIconButton(
                    icon = Icons.Default.ArrowBack,
                    onClick = { browser.goBack() },
                    contentDescription = "Back"
                )
                DsIconButton(
                    icon = Icons.Default.ArrowForward,
                    onClick = { browser.goForward() },
                    contentDescription = "Forward"
                )
                DsIconButton(
                    icon = Icons.Default.Refresh,
                    onClick = { browser.refresh() },
                    contentDescription = "Refresh"
                )
                DsTextField(
                    value = browser.addressBarText,
                    onValueChange = { browser.addressBarText = it },
                    placeholder = "URL or search…",
                    modifier = Modifier.weight(1f)
                )
                DsButton(
                    text = "Go",
                    kind = DsButtonKind.Primary,
                    compact = true,
                    onClick = { browser.navigate(browser.addressBarText) }
                )
                DsIconButton(
                    icon = if (browser.isBookmarked(browser.pageUrl.ifBlank { activeTab?.url.orEmpty() })) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    onClick = { browser.toggleBookmark() },
                    contentDescription = "Bookmark page"
                )
            }
        }
    }
}

@Composable
private fun PageCard(state: AppState) {
    val sc = surfaceColors()
    val browser = state.browserEngine

    DsCard {
        Column(
            Modifier
                .fillMaxWidth()
                .height(430.dp)
                .padding(DsSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(browser.pageTitle.ifBlank { "New Tab" }, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(browser.pageUrl.ifBlank { "No page loaded" }, color = sc.textMuted, fontSize = DsType.Caption)
                }
                DsBadge(text = browser.renderMode.name, tint = sc.textSecondary)
            }

            when {
                browser.loading -> Text("Loading…", color = sc.textMuted, fontSize = DsType.Body)
                browser.lastError != null -> {
                    Text("Error: ${browser.lastError}", color = sc.textPrimary, fontSize = DsType.Body)
                    DsButton(
                        text = "Retry",
                        icon = Icons.Default.Refresh,
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = { browser.refresh() }
                    )
                }
                browser.renderMode == RenderMode.Reader -> {
                    Text(
                        ReaderMode.extractReadable(browser.pageContent),
                        color = sc.textSecondary,
                        fontSize = DsType.Body,
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    )
                }
                browser.renderMode == RenderMode.RawText -> {
                    Text(
                        browser.pageContent.take(8000),
                        color = sc.textSecondary,
                        fontSize = DsType.Body,
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    )
                }
                browser.renderMode == RenderMode.WebView -> {
                    Text("Rendered by the platform WebView.", color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                }
                else -> {
                    Text("Rendering unavailable.", color = sc.textMuted, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                }
            }

            SelectedTextRow(state)
        }
    }
}

@Composable
private fun SelectedTextRow(state: AppState) {
    val sc = surfaceColors()
    val browser = state.browserEngine
    val text = browser.selectedText
    if (text.isNullOrBlank()) {
        Text("Select text on the page to look it up or mine it.", color = sc.textMuted, fontSize = DsType.Caption)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text("“${text.take(60)}”", color = sc.textPrimary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
        DsButton(
            text = "Look up",
            icon = Icons.Default.Bookmark,
            kind = DsButtonKind.Secondary,
            compact = true,
            onClick = {
                state.dictionary.query = text
                state.mining.openMining(
                    MiningPayload(
                        headword = text.take(40),
                        definition = "",
                        sentence = browser.selectedText.orEmpty(),
                        source = "browser",
                        sourceDetail = browser.pageUrl
                    )
                )
            }
        )
    }
}

@Composable
private fun SidePanel(state: AppState, section: String, onSectionChange: (String) -> Unit) {
    val sc = surfaceColors()
    val sections = listOf(
        "bookmarks" to "Bookmarks",
        "history" to "History",
        "downloads" to "Downloads"
    )

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                sections.forEach { (key, label) ->
                    DsButton(
                        text = label,
                        kind = if (section == key) DsButtonKind.Primary else DsButtonKind.Secondary,
                        compact = true,
                        onClick = { onSectionChange(key) }
                    )
                }
            }
            when (section) {
                "bookmarks" -> BookmarksList(state)
                "history" -> HistoryList(state)
                "downloads" -> DownloadsList(state)
            }
        }
    }
}

@Composable
private fun BookmarksList(state: AppState) {
    val sc = surfaceColors()
    val bookmarks = state.browserEngine.bookmarks
    if (bookmarks.isEmpty()) {
        Text("No bookmarks. Tap the bookmark icon on a page.", color = sc.textMuted, fontSize = DsType.Body)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        bookmarks.take(30).forEach { bm ->
            Row(
                Modifier.fillMaxWidth().clickable { state.browserEngine.navigate(bm.url) }.padding(vertical = DsSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(bm.title, color = sc.textPrimary, fontSize = DsType.Body)
                    Text(bm.url, color = sc.textMuted, fontSize = DsType.Caption, maxLines = 1)
                }
                DsIconButton(
                    icon = Icons.Default.Close,
                    onClick = { state.browserEngine.removeBookmark(bm.id) },
                    contentDescription = "Remove bookmark",
                    size = 24.dp
                )
            }
        }
    }
}

@Composable
private fun HistoryList(state: AppState) {
    val sc = surfaceColors()
    val history = state.browserEngine.history
    if (history.isEmpty()) {
        Text("No browsing history yet.", color = sc.textMuted, fontSize = DsType.Body)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        history.take(30).forEach { item ->
            Row(
                Modifier.fillMaxWidth().clickable { state.browserEngine.navigate(item.url) }.padding(vertical = DsSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = sc.textPrimary, fontSize = DsType.Body)
                    Text(item.url, color = sc.textMuted, fontSize = DsType.Caption, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DownloadsList(state: AppState) {
    val sc = surfaceColors()
    val downloads = state.browserEngine.downloads
    if (downloads.isEmpty()) {
        Text("No downloads.", color = sc.textMuted, fontSize = DsType.Body)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        downloads.take(30).forEach { dl ->
            Row(Modifier.fillMaxWidth().padding(vertical = DsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(dl.fileName, color = sc.textPrimary, fontSize = DsType.Body)
                    Text(dl.url, color = sc.textMuted, fontSize = DsType.Caption, maxLines = 1)
                }
                Text(String.format("%.1f KB", dl.sizeBytes / 1024.0), color = sc.textMuted, fontSize = DsType.Caption)
            }
        }
    }
}
