package ua.syt0r.kanji.desktop.engine.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// ============================================
// KAITEYO LEARNING BROWSER ENGINE
// A lightweight learning-focused browser. Tabs,
// address bar, back/forward, bookmarks, downloads,
// reader mode and full integration with dictionary
// lookup, OCR, screenshots and mining. Pages are
// fetched with java.net.http and rendered in
// reader mode for study; a JavaFX WebView renderer
// is used when available for full page rendering.
// ============================================

@Serializable
data class BrowserBookmark(
    val id: String,
    val title: String,
    val url: String,
    val createdAt: String = ""
)

@Serializable
data class DownloadRecord(
    val id: String,
    val url: String,
    val fileName: String,
    val targetPath: String,
    val sizeBytes: Long,
    val completed: Boolean = true,
    val createdAt: String = ""
)

@Serializable
data class BrowserHistoryEntry(
    val url: String,
    val title: String,
    val visitedAt: String = ""
)

/** A live tab. */
data class BrowserTab(
    val id: String,
    var title: String = "New Tab",
    var url: String = "",
    var history: MutableList<String> = mutableListOf(),
    var historyIndex: Int = -1
) {
    val canGoBack: Boolean get() = historyIndex > 0
    val canGoForward: Boolean get() = historyIndex < history.size - 1
}

/** Rendering capabilities available on this platform. */
enum class RenderMode { Reader, RawText, WebView, Unavailable }

@Serializable
private data class BrowserStateDto(
    val bookmarks: List<BrowserBookmark> = emptyList(),
    val downloads: List<DownloadRecord> = emptyList(),
    val history: List<BrowserHistoryEntry> = emptyList()
)

class BrowserEngine {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val stateFile: File get() = File(System.getProperty("user.home"), ".kaiteyo/browser-state.json")

    val tabs = mutableStateListOf<BrowserTab>()
    var activeTabId by mutableStateOf<String?>(null)
    var addressBarText by mutableStateOf("")
    var pageTitle by mutableStateOf("")
    var pageUrl by mutableStateOf("")
    var pageContent by mutableStateOf("")
    var renderMode by mutableStateOf(RenderMode.Reader)
    var loading by mutableStateOf(false)
    var lastError by mutableStateOf<String?>(null)
    var selectedText by mutableStateOf<String?>(null)

    val bookmarks = mutableStateListOf<BrowserBookmark>()
    val downloads = mutableStateListOf<DownloadRecord>()
    val history = mutableStateListOf<BrowserHistoryEntry>()

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(12))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    init {
        load()
        newTab()
    }

    // ------------------------------------------------------------
    // Tabs
    // ------------------------------------------------------------

    fun newTab(url: String = ""): BrowserTab {
        val tab = BrowserTab("tab-${System.currentTimeMillis()}")
        tabs.add(tab)
        activeTabId = tab.id
        if (url.isNotBlank()) navigate(url)
        else {
            addressBarText = ""
            pageTitle = "New Tab"
            pageContent = ""
            renderMode = RenderMode.Reader
            lastError = null
        }
        return tab
    }

    fun closeTab(id: String) {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx == -1) return
        tabs.removeAt(idx)
        if (tabs.isEmpty()) newTab()
        else if (activeTabId == id) {
            activeTabId = tabs[(idx - 1).coerceAtLeast(0)].id
        }
    }

    fun activateTab(id: String) {
        activeTabId = id
        val tab = tabs.firstOrNull { it.id == id }
        addressBarText = tab?.url.orEmpty()
        pageTitle = tab?.title ?: "New Tab"
    }

    val activeTab: BrowserTab? get() = activeTabId?.let { id -> tabs.firstOrNull { it.id == id } }

    // ------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------

    fun navigate(rawUrl: String) {
        val url = normalize(rawUrl) ?: return
        val tab = activeTab() ?: return
        tab.url = url
        tab.title = url
        addressBarText = url
        loading = true
        lastError = null

        // push history
        if (tab.history.isEmpty() || tab.history[tab.historyIndex] != url) {
            tab.history.add(url)
            tab.historyIndex = tab.history.size - 1
        }

        // try JavaFX WebView first (full rendering), fall back to fetch+reader.
        if (tryWebViewNavigation(url)) {
            loading = false
            return
        }
        fetchAndRender(url)
    }

    fun goBack() {
        val tab = activeTab() ?: return
        if (!tab.canGoBack) return
        tab.historyIndex -= 1
        val url = tab.history[tab.historyIndex]
        tab.url = url
        addressBarText = url
        fetchAndRender(url)
    }

    fun goForward() {
        val tab = activeTab() ?: return
        if (!tab.canGoForward) return
        tab.historyIndex += 1
        val url = tab.history[tab.historyIndex]
        tab.url = url
        addressBarText = url
        fetchAndRender(url)
    }

    fun refresh() {
        val url = activeTab()?.url ?: return
        fetchAndRender(url)
    }

    private fun fetchAndRender(url: String) {
        loading = true
        val result = fetch(url)
        loading = false
        result.onSuccess { html ->
            pageContent = html
            renderMode = ReaderMode.decide(url, html)
            val title = ReaderMode.extractTitle(html)
            pageTitle = title.ifBlank { url }
            activeTab()?.title = pageTitle
            lastError = null
            recordHistory(url, pageTitle)
        }.onFailure { e ->
            lastError = e.message
            pageTitle = "Failed to load"
        }
    }

    private fun fetch(url: String): Result<String> = runCatching {
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(20))
            .header("User-Agent", "Kaiteyo-LearningBrowser/1.0")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..399) error("HTTP ${response.statusCode()}")
        response.body()
    }

    // ------------------------------------------------------------
    // Selection / lookup / mining helpers
    // ------------------------------------------------------------

    fun captureSelection(text: String) {
        selectedText = text.trim().ifBlank { null }
    }

    fun openSelectedInDictionary() {
        // handled by UI: pops the dictionary popup with selectedText
    }

    // ------------------------------------------------------------
    // Bookmarks
    // ------------------------------------------------------------

    fun isBookmarked(url: String): Boolean = bookmarks.any { it.url == url }

    fun toggleBookmark() {
        val url = pageUrl.ifBlank { activeTab()?.url.orEmpty() }
        if (url.isBlank()) return
        val existing = bookmarks.firstOrNull { it.url == url }
        if (existing != null) bookmarks.remove(existing)
        else bookmarks.add(0, BrowserBookmark("bm-${System.currentTimeMillis()}", pageTitle.ifBlank { url }, url))
        save()
    }

    fun removeBookmark(id: String) {
        bookmarks.removeAll { it.id == id }
        save()
    }

    // ------------------------------------------------------------
    // Downloads
    // ------------------------------------------------------------

    fun download(url: String, targetDir: File): Result<DownloadRecord> = runCatching {
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(60))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() !in 200..399) error("HTTP ${response.statusCode()}")
        val fileName = URI(url).path.substringAfterLast('/').ifBlank { "download-${System.currentTimeMillis()}" }
        val target = File(targetDir, fileName)
        target.writeBytes(response.body())
        val rec = DownloadRecord("dl-${System.currentTimeMillis()}", url, fileName, target.absolutePath, response.body().size.toLong())
        downloads.add(0, rec)
        save()
        rec
    }

    // ------------------------------------------------------------
    // History
    // ------------------------------------------------------------

    private fun recordHistory(url: String, title: String) {
        history.removeAll { it.url == url }
        history.add(0, BrowserHistoryEntry(url, title.ifBlank { url }))
        while (history.size > 500) history.removeAt(history.lastIndex)
        save()
    }

    fun clearHistory() {
        history.clear()
        save()
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private fun activeTab(): BrowserTab? = tabs.firstOrNull { it.id == activeTabId }

    private fun normalize(raw: String): String? {
        var url = raw.trim()
        if (url.isBlank()) return null
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = if (url.contains(".")) "https://$url" else "https://www.google.com/search?q=${url.replace(' ', '+')}"
        }
        return url
    }

    /** Reflection-driven JavaFX WebView navigation when javafx.web is present. */
    private fun tryWebViewNavigation(url: String): Boolean {
        return runCatching {
            Class.forName("javafx.scene.web.WebView")
            renderMode = RenderMode.WebView
            webViewCallback?.invoke(url)
            recordHistory(url, url)
            true
        }.getOrDefault(false)
    }

    /** Set by the UI layer when a real WebView host is mounted. */
    var webViewCallback: ((String) -> Unit)? = null

    private fun load() {
        if (!stateFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<BrowserStateDto>(stateFile.readText())
            bookmarks.clear(); bookmarks.addAll(dto.bookmarks)
            downloads.clear(); downloads.addAll(dto.downloads)
            history.clear(); history.addAll(dto.history)
        }
    }

    private fun save() {
        runCatching {
            stateFile.writeText(
                json.encodeToString(BrowserStateDto(bookmarks.toList(), downloads.toList(), history.toList()))
            )
        }
    }
}

/** Reader-mode extraction helpers (lightweight HTML parsing). */
object ReaderMode {

    fun decide(url: String, html: String): RenderMode = when {
        html.contains("<html", true) || html.contains("<!doctype html", true) -> RenderMode.Reader
        else -> RenderMode.RawText
    }

    fun extractTitle(html: String): String {
        val m = Regex("<title[^>]*>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
        val raw = m?.groupValues?.get(1).orEmpty()
        return stripTags(raw).trim().take(200)
    }

    fun stripTags(html: String): String =
        html.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")

    /** Extract readable paragraphs for study (reader mode). */
    fun extractReadable(html: String): String {
        val withoutScripts = html.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
        val paragraphs = Regex("<p[^>]*>([\\s\\S]*?)</p>", RegexOption.IGNORE_CASE).findAll(withoutScripts)
        val text = paragraphs.map { stripTags(it.groupValues[1]) }.filter { it.isNotBlank() }.joinToString("\n\n")
        return text.ifBlank { stripTags(withoutScripts).take(12000) }
    }
}