package ua.syt0r.kanji.desktop.engine.plugin

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// ============================================
// PLUGIN MARKETPLACE
// A curated index of plugins hosted on GitHub.
// The index is a plain JSON file (index.json)
// published to any GitHub repository (default:
// kaiteyo/plugins). Each entry carries the raw
// manifest URL; installing fetches that manifest
// and registers it with the local PluginRegistry.
//
// A small built-in demo catalog keeps the
// marketplace useful when offline.
// ============================================

@Serializable
data class MarketplacePlugin(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String = "",
    val category: String = "Utility",
    val license: String = "MIT",
    val downloads: Long = 0,
    val stars: Long = 0,
    val minAppVersion: String = "1.0.0",
    val manifestUrl: String = "",
    val sourceUrl: String = "",
    val homepage: String = "",
    val tags: List<String> = emptyList()
)

@Serializable
data class MarketplaceIndex(
    val version: Int = 1,
    val updatedAt: String = "",
    val plugins: List<MarketplacePlugin> = emptyList()
)

object PluginMarketplace {

    /** Where the curated plugin index lives. Point this at your own GitHub repo. */
    const val DEFAULT_INDEX_URL = "https://raw.githubusercontent.com/kaiteyo/plugins/main/index.json"

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private fun client(): HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /** Fetch the curated plugin index from GitHub. */
    fun fetchIndex(url: String = DEFAULT_INDEX_URL, timeoutSeconds: Long = 10): Result<MarketplaceIndex> = runCatching {
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("User-Agent", "Kaiteyo-Desktop")
            .GET()
            .build()
        val response = client().send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) error("Marketplace responded with HTTP ${response.statusCode()}")
        json.decodeFromString<MarketplaceIndex>(response.body())
    }

    /** Fetch a single plugin manifest from its raw GitHub URL. */
    fun fetchManifest(url: String, timeoutSeconds: Long = 10): Result<PluginManifest> = runCatching {
        if (url.isBlank()) error("No manifest URL for this plugin")
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("User-Agent", "Kaiteyo-Desktop")
            .GET()
            .build()
        val response = client().send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) error("Manifest responded with HTTP ${response.statusCode()}")
        json.decodeFromString<PluginManifest>(response.body())
    }

    /**
     * Build a manifest straight from a marketplace entry. Used when the
     * remote manifest cannot be reached so installation still works offline.
     */
    fun localManifest(plugin: MarketplacePlugin): PluginManifest = PluginManifest(
        id = plugin.id,
        name = plugin.name,
        version = plugin.version,
        author = plugin.author,
        description = plugin.description,
        permissions = plugin.tags,
        minAppVersion = plugin.minAppVersion,
        enabled = true
    )

    /** The built-in featured catalog, shown when GitHub is unreachable. */
    fun demoCatalog(): List<MarketplacePlugin> = listOf(
        MarketplacePlugin(
            id = "kaiteyo-audio",
            name = "Kanji Audio",
            version = "1.2.0",
            author = "Kaiteyo",
            description = "Adds reading playback commands and a per-card audio panel using platform TTS.",
            category = "Reading",
            license = "MIT",
            downloads = 4_800,
            stars = 128,
            tags = listOf("audio", "reading")
        ),
        MarketplacePlugin(
            id = "kaiteyo-pitch-accent",
            name = "Pitch Accent Trainer",
            version = "1.0.4",
            author = "Kaiteyo",
            description = "Highlights Tokyo-standard pitch accents in the dictionary and browser previews.",
            category = "Reading",
            license = "MIT",
            downloads = 2_150,
            stars = 76,
            tags = listOf("pitch", "accent", "pronunciation")
        ),
        MarketplacePlugin(
            id = "kaiteyo-radical-helper",
            name = "Radical Reference",
            version = "0.9.2",
            author = "Kaiteyo",
            description = "A compact radical breakdown panel with stroke hints for every kanji you browse.",
            category = "Study",
            license = "MIT",
            downloads = 1_900,
            stars = 54,
            tags = listOf("radicals", "study")
        ),
        MarketplacePlugin(
            id = "kaiteyo-night-reader",
            name = "Night Reader",
            version = "1.1.0",
            author = "Kaiteyo",
            description = "Warm, low-glare reading theme that shifts review sessions into night mode.",
            category = "Appearance",
            license = "MIT",
            downloads = 3_400,
            stars = 91,
            tags = listOf("theme", "appearance")
        ),
        MarketplacePlugin(
            id = "kaiteyo-anki-bridge",
            name = "Anki Bridge",
            version = "2.0.1",
            author = "Kaiteyo",
            description = "One-click sync of your Kaiteyo cards into Anki and back, including scheduling data.",
            category = "Export",
            license = "MIT",
            downloads = 6_200,
            stars = 210,
            tags = listOf("anki", "export", "sync")
        ),
        MarketplacePlugin(
            id = "kaiteyo-kanji-stats",
            name = "Kanji Insights",
            version = "1.0.0",
            author = "Kaiteyo",
            description = "Frequency, JLPT and grade breakdown charts wired into the statistics dashboard.",
            category = "Statistics",
            license = "MIT",
            downloads = 1_250,
            stars = 33,
            tags = listOf("stats", "analytics")
        )
    )

    /** Prefer the remote index; fall back to the demo catalog on any failure. */
    fun load(customUrl: String = DEFAULT_INDEX_URL): Result<MarketplaceIndex> =
        fetchIndex(customUrl).recoverCatching {
            MarketplaceIndex(plugins = demoCatalog())
        }
}
