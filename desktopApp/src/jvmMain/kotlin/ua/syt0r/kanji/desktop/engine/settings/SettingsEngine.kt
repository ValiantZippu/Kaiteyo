package ua.syt0r.kanji.desktop.engine.settings

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ============================================
// SETTINGS ENGINE
// Categorized, searchable, reset/import/export
// settings with typed values and change events.
// ============================================

@Serializable
enum class SettingType { Boolean, Int, Float, String, Enum, List }

@Serializable
enum class SettingCategory { General, Navigation, Appearance, Review, Browser, Statistics, History, ImportExport, Sync, Updates, Plugins, Accessibility, Advanced, Media }

data class SettingDef(
    val key: String,
    val name: String,
    val description: String = "",
    val category: SettingCategory,
    val type: SettingType,
    val defaultValue: Any,
    val options: List<String> = emptyList(),
    val searchable: Boolean = true,
    /** Optional sub-section label rendered above the setting within its category. */
    val group: String = ""
) {
    val normalizedDefault: String
        get() = defaultValue.toString()
}

@Serializable
data class SettingsSnapshot(
    val values: Map<String, String>,
    val updatedAt: String = kotlinx.datetime.Clock.System.now().toString()
)

/**
 * Typed settings store with change notification and validation.
 *
 * By default the store is in-memory only (suits previews/tests). Pass a
 * [persistFile] (e.g. ~/.kaiteyo/settings.json) to make every change survive
 * restarts — the desktop app does this, so media key / integration / local
 * API configuration is real persisted state, never a session-only mirror.
 */
class SettingsEngine(
    defs: List<SettingDef> = defaultSettings(),
    private val persistFile: java.io.File? = null
) {

    private val definitions = LinkedHashMap<String, SettingDef>()
    private val values = mutableMapOf<String, String>()
    private val listeners = mutableListOf<(String, String, String) -> Unit>()

    /** When the last user change was applied — used as the settings blob's LWW timestamp. */
    private var lastModifiedAtValue: Instant = Clock.System.now()

    /** Timestamp of the most recent [set] call (unchanged values don't bump it). */
    fun lastModifiedAt(): Instant = lastModifiedAtValue

    init {
        defs.forEach { definitions[it.key] = it }
        applyDefaults()
        persistFile?.let { loadFromDisk(it) }
    }

    // ------------------------------------------------------------
    // Optional on-disk persistence (~/.kaiteyo/settings.json)
    // ------------------------------------------------------------

    private fun loadFromDisk(file: java.io.File) {
        if (!file.exists()) return
        runCatching {
            val json = Json { ignoreUnknownKeys = true }
            val snapshot = json.decodeFromString<SettingsSnapshot>(file.readText())
            snapshot.values.forEach { (key, value) ->
                if (key in definitions) values[key] = value
            }
        }
    }

    private fun saveToDisk() {
        val file = persistFile ?: return
        runCatching {
            file.parentFile?.mkdirs()
            val json = Json { prettyPrint = true; encodeDefaults = true }
            file.writeText(json.encodeToString(SettingsSnapshot(values.toMap())))
        }
    }

    val defs: List<SettingDef> get() = definitions.values.toList()

    fun categories(): List<SettingCategory> = SettingCategory.entries.filter { cat -> definitions.values.any { it.category == cat } }

    fun byCategory(category: SettingCategory): List<SettingDef> = definitions.values.filter { it.category == category }

    fun search(query: String): List<SettingDef> {
        if (query.isBlank()) return definitions.values.toList()
        val q = query.lowercase()
        return definitions.values.filter {
            it.name.lowercase().contains(q) || it.description.lowercase().contains(q) ||
                it.key.lowercase().contains(q) || it.category.name.lowercase().contains(q)
        }
    }

    // ------------------------------------------------------------
    // Typed accessors
    // ------------------------------------------------------------

    fun get(key: String): String = values[key] ?: definitions[key]?.normalizedDefault ?: ""

    fun getBool(key: String, default: Boolean = false): Boolean = get(key).toBooleanStrictOrNull() ?: default

    fun getInt(key: String, default: Int = 0): Int = get(key).toIntOrNull() ?: default

    fun getFloat(key: String, default: Float = 0f): Float = get(key).toFloatOrNull() ?: default

    fun getString(key: String, default: String = ""): String = get(key).ifBlank { default }

    fun set(key: String, value: Any) {
        val def = definitions[key] ?: return
        val normalized = normalize(def, value)
        if (values[key] == normalized) return
        val old = values[key]
        values[key] = normalized
        lastModifiedAtValue = Clock.System.now()
        listeners.forEach { it(key, old ?: "", normalized) }
        saveToDisk()
    }

    fun setBool(key: String, value: Boolean) = set(key, value)
    fun setInt(key: String, value: Int) = set(key, value)
    fun setFloat(key: String, value: Float) = set(key, value)
    fun setString(key: String, value: String) = set(key, value)

    private fun normalize(def: SettingDef, value: Any): String = when (def.type) {
        SettingType.Boolean -> (value as? Boolean)?.toString() ?: value.toString().toBooleanStrictOrNull().toString()
        SettingType.Int -> (value as? Int)?.toString() ?: value.toString()
        SettingType.Float -> (value as? Float)?.toString() ?: value.toString()
        SettingType.String, SettingType.Enum, SettingType.List -> value.toString()
    }

    fun reset(key: String) {
        val def = definitions[key] ?: return
        set(key, def.defaultValue)
    }

    fun resetAll() {
        applyDefaults()
        saveToDisk()
    }

    fun resetCategory(category: SettingCategory) {
        definitions.values.filter { it.category == category }.forEach { values[it.key] = it.normalizedDefault }
        saveToDisk()
    }

    /** Write the catalog defaults into the value map (no disk write). */
    private fun applyDefaults() {
        definitions.values.forEach { values[it.key] = it.normalizedDefault }
    }

    fun observe(listener: (String, String, String) -> Unit) {
        listeners.add(listener)
    }

    // ------------------------------------------------------------
    // Import / export
    // ------------------------------------------------------------

    fun exportJson(): String {
        val json = Json { prettyPrint = true; encodeDefaults = true }
        return json.encodeToString(SettingsSnapshot(values.toMap()))
    }

    fun importJson(text: String): Result<Int> = runCatching {
        val json = Json { ignoreUnknownKeys = true }
        val snapshot = json.decodeFromString<SettingsSnapshot>(text)
        var applied = 0
        snapshot.values.forEach { (key, value) ->
            if (key in definitions) {
                values[key] = value
                applied++
            }
        }
        saveToDisk()
        applied
    }

    fun snapshot(): Map<String, String> = values.toMap()

    /**
     * Apply a snapshot, notifying listeners for keys that actually changed so
     * live UI mirrors (navigation, launcher, appearance) update immediately.
     * The modification timestamp is NOT bumped — applying remote state is not
     * a user change.
     */
    fun restore(snapshot: Map<String, String>) {
        snapshot.forEach { (key, value) ->
            if (key in definitions && values[key] != value) {
                val old = values[key]
                values[key] = value
                listeners.forEach { it(key, old ?: "", value) }
            }
        }
        saveToDisk()
    }

    fun has(key: String): Boolean = key in definitions
}

/** The default settings catalog — categorized, searchable, resettable. */
fun defaultSettings(): List<SettingDef> = listOf(
    SettingDef("general.language", "Language", "Application language", SettingCategory.General, SettingType.Enum, "system", options = listOf("system", "en", "ja")),
    SettingDef("general.confirm-before-delete", "Confirm before delete", "Ask before destructive actions", SettingCategory.General, SettingType.Boolean, true),
    SettingDef("general.startup-view", "Startup view", "Where the app opens", SettingCategory.General, SettingType.Enum, "dashboard", options = listOf("dashboard", "browser", "review", "collections")),
    SettingDef("onboarding.completed", "Onboarding completed", "Whether the first-run wizard has finished (internal)", SettingCategory.General, SettingType.Boolean, false, searchable = false),
    SettingDef("onboarding.version", "Onboarding version", "Version of the onboarding flow seen (internal)", SettingCategory.General, SettingType.Int, 1, searchable = false),
    SettingDef("workspace.panels", "Workspace panels", "Persisted panel layout (internal)", SettingCategory.General, SettingType.String, "", searchable = false),

    SettingDef("navigation.layout", "Navigation mode", "Expanded, compact icons, or bubble launcher", SettingCategory.Navigation, SettingType.Enum, "expanded", options = listOf("expanded", "compact", "bubble"), group = "General"),
    SettingDef("navigation.default-layout", "Default mode", "Mode used when launching", SettingCategory.Navigation, SettingType.Enum, "expanded", options = listOf("expanded", "compact", "bubble"), group = "General"),
    SettingDef("navigation.remember-last", "Remember previous mode", "Restore the mode you last used on startup", SettingCategory.Navigation, SettingType.Boolean, true, group = "General"),
    SettingDef("navigation.position", "Sidebar placement", "Which edge the sidebar dock lives on", SettingCategory.Navigation, SettingType.Enum, "left", options = listOf("left", "right", "top", "bottom"), group = "General"),
    SettingDef("navigation.sidebar-width", "Expanded width", "Predefined expanded dock widths (presets only)", SettingCategory.Navigation, SettingType.Enum, "standard", options = listOf("narrow", "standard", "wide"), group = "General"),
    SettingDef("navigation.compact-icon-size", "Compact icon size", "Tab bar icon scale", SettingCategory.Navigation, SettingType.Enum, "medium", options = listOf("small", "medium", "large"), group = "General"),
    SettingDef("navigation.collapsed", "Collapse navigation", "Icon-only navigation (legacy)", SettingCategory.Navigation, SettingType.Boolean, false, searchable = false, group = "General"),
    SettingDef("navigation.mode", "Navigation mode", "Sidebar, floating launcher, or both (legacy)", SettingCategory.Navigation, SettingType.Enum, "traditional", options = listOf("traditional", "floating", "both"), searchable = false, group = "General"),

    SettingDef("navigation.icon-size", "Icon size", "Dock and switcher icon scale", SettingCategory.Navigation, SettingType.Enum, "medium", options = listOf("small", "medium", "large"), group = "Sidebar"),
    SettingDef("navigation.label-mode", "Label visibility", "Labels in the expanded dock", SettingCategory.Navigation, SettingType.Enum, "always", options = listOf("always", "on-hover", "hidden"), group = "Sidebar"),
    SettingDef("navigation.compact-spacing", "Compact icon spacing", "Vertical rhythm between dock items", SettingCategory.Navigation, SettingType.Enum, "comfortable", options = listOf("tight", "comfortable", "spacious"), group = "Sidebar"),

    SettingDef("launcher.enabled", "Bubble mode", "Replace the sidebar with the floating launcher", SettingCategory.Navigation, SettingType.Boolean, false, group = "Bubble"),
    SettingDef("launcher.default-position", "Default position", "Where the launcher starts", SettingCategory.Navigation, SettingType.Enum, "bottom-right", options = listOf("bottom-right", "bottom-left", "top-right", "top-left", "right", "left"), group = "Bubble"),
    SettingDef("launcher.auto-fade", "Auto fade", "Fade the launcher after inactivity", SettingCategory.Navigation, SettingType.Boolean, true, group = "Bubble"),
    SettingDef("launcher.fade-delay", "Fade delay (seconds)", "Inactivity before the launcher fades", SettingCategory.Navigation, SettingType.Int, 6, group = "Bubble"),
    SettingDef("launcher.fade-opacity", "Faded opacity", "Transparency while faded (0 = invisible, 1 = solid)", SettingCategory.Navigation, SettingType.Float, 0.25f, group = "Bubble"),
    SettingDef("launcher.fade-duration", "Fade duration (ms)", "How long the fade animation takes", SettingCategory.Navigation, SettingType.Int, 450, group = "Bubble"),
    SettingDef("launcher.size", "Bubble size", "Bubble diameter", SettingCategory.Navigation, SettingType.Enum, "medium", options = listOf("small", "medium", "large"), group = "Bubble"),
    SettingDef("launcher.icon-size", "Icon size", "Icon inside the bubble", SettingCategory.Navigation, SettingType.Enum, "medium", options = listOf("small", "medium", "large"), group = "Bubble"),
    SettingDef("launcher.snap", "Snap to edges", "Snap to edges and corners when released", SettingCategory.Navigation, SettingType.Boolean, true, group = "Bubble"),
    SettingDef("launcher.snap-sensitivity", "Snap sensitivity", "Higher values snap from farther away", SettingCategory.Navigation, SettingType.Float, 1.0f, group = "Bubble"),
    SettingDef("launcher.animation-speed", "Animation speed", "Bubble movement and launchpad animation speed", SettingCategory.Navigation, SettingType.Float, 1.0f, group = "Bubble"),
    SettingDef("launcher.pos-x", "Launcher X (internal)", "Remembered launcher position", SettingCategory.Navigation, SettingType.Float, 0.88f, searchable = false, group = "Bubble"),
    SettingDef("launcher.pos-y", "Launcher Y (internal)", "Remembered launcher position", SettingCategory.Navigation, SettingType.Float, 0.86f, searchable = false, group = "Bubble"),
    SettingDef("launcher.pos-x-phone", "Launcher X phone (internal)", "Remembered phone launcher position", SettingCategory.Navigation, SettingType.Float, 0.88f, searchable = false, group = "Bubble"),
    SettingDef("launcher.pos-y-phone", "Launcher Y phone (internal)", "Remembered phone launcher position", SettingCategory.Navigation, SettingType.Float, 0.78f, searchable = false, group = "Bubble"),

    SettingDef("navigation.compact-position", "Navigation position", "Tab bar position on phones and compact windows", SettingCategory.Navigation, SettingType.Enum, "bottom", options = listOf("top", "bottom"), group = "Compact"),
    SettingDef("navigation.tooltip-delay", "Tooltip behavior", "Delay before hover tooltips appear", SettingCategory.Navigation, SettingType.Int, 450, group = "Compact"),

    SettingDef("navigation.animations", "Enable animations", "Animate layout changes and transitions", SettingCategory.Navigation, SettingType.Boolean, true, group = "Animations"),
    SettingDef("navigation.animation-speed", "Animation duration", "1.0 = default, 0.5 = half, 2.0 = double", SettingCategory.Navigation, SettingType.Float, 1.0f, group = "Animations"),
    SettingDef("navigation.reduced-motion", "Reduced motion", "Disable all navigation and launcher animations", SettingCategory.Navigation, SettingType.Boolean, false, group = "Animations"),

    SettingDef("navigation.larger-icons", "Larger icons", "Scale up sidebar and launcher icons", SettingCategory.Navigation, SettingType.Boolean, false, group = "Accessibility"),
    SettingDef("navigation.larger-hitbox", "Larger hitboxes", "Easier to grab the launcher and recover it after fading", SettingCategory.Navigation, SettingType.Boolean, false, group = "Accessibility"),
    SettingDef("navigation.high-contrast", "High contrast", "Stronger borders and surfaces for the navigation", SettingCategory.Navigation, SettingType.Boolean, false, group = "Accessibility"),

    SettingDef("appearance.base-mode", "Base mode", "Oled, Dark, Light or Sepia", SettingCategory.Appearance, SettingType.Enum, "oled", options = listOf("oled", "dark", "light", "sepia")),
    SettingDef("appearance.accent", "Accent color", "Accent theme", SettingCategory.Appearance, SettingType.Enum, "signature", options = listOf("signature", "cotton", "ocean", "forest", "sunset", "lavender", "mono")),
    SettingDef("appearance.density", "Density", "Compact / Comfortable / Spacious", SettingCategory.Appearance, SettingType.Enum, "comfortable", options = listOf("compact", "comfortable", "spacious")),
    SettingDef("appearance.reduced-motion", "Reduced motion", "Disable animations", SettingCategory.Appearance, SettingType.Boolean, false),
    SettingDef("appearance.high-contrast", "High contrast", "Increase contrast", SettingCategory.Appearance, SettingType.Boolean, false),
    SettingDef("appearance.large-text", "Large text", "Scale up typography", SettingCategory.Appearance, SettingType.Boolean, false),
    SettingDef("appearance.glass", "Glass panels", "Blurred translucency", SettingCategory.Appearance, SettingType.Boolean, false),
    SettingDef("appearance.corner-radius", "Corner radius", "Panel rounding", SettingCategory.Appearance, SettingType.Enum, "rounded", options = listOf("square", "rounded", "soft")),

    SettingDef("review.show-new", "Show new cards", "Include new cards in review", SettingCategory.Review, SettingType.Boolean, true),
    SettingDef("review.daily-new-limit", "Daily new card limit", "Max new cards per day", SettingCategory.Review, SettingType.Int, 20),
    SettingDef("review.daily-review-limit", "Daily review limit", "Max reviews per day", SettingCategory.Review, SettingType.Int, 200),
    SettingDef("review.shuffle", "Shuffle queue", "Randomize review order", SettingCategory.Review, SettingType.Boolean, true),
    SettingDef("review.confirm-mistakes", "Confirm mistakes", "Require confirmation on Again", SettingCategory.Review, SettingType.Boolean, false),
    SettingDef("review.auto-answer", "Auto-advance", "Advance automatically after grading", SettingCategory.Review, SettingType.Boolean, true),

    SettingDef("browser.default-view", "Default browser view", "Grid, list or details", SettingCategory.Browser, SettingType.Enum, "grid", options = listOf("grid", "list", "details")),
    SettingDef("browser.show-preview-panel", "Show preview panel", "Side-by-side card preview", SettingCategory.Browser, SettingType.Boolean, true),
    SettingDef("browser.cards-per-page", "Cards per page", "Grid density", SettingCategory.Browser, SettingType.Int, 48),
    // Persisted library browse state (internal — written by the deck catalog).
    SettingDef("browser.library-sort", "Library sort (internal)", "Persisted deck catalog sort", SettingCategory.Browser, SettingType.Enum, "name", options = listOf("name", "newest", "due", "new", "favorite"), searchable = false),
    SettingDef("browser.library-scope", "Library scope (internal)", "Persisted library rail selection", SettingCategory.Browser, SettingType.Enum, "all", options = listOf("all", "kanji", "vocabulary", "grammar", "radical", "sentence", "media", "due", "new", "favorites", "recent", "archived"), searchable = false),
    SettingDef("browser.library-filter-jlpt", "Library JLPT filter (internal)", "Persisted JLPT filter", SettingCategory.Browser, SettingType.Int, 0, searchable = false),
    SettingDef("browser.library-filter-difficulty", "Library difficulty filter (internal)", "Persisted difficulty filter", SettingCategory.Browser, SettingType.Int, 0, searchable = false),
    SettingDef("browser.library-filter-favorites", "Library favorites filter (internal)", "Persisted favorites-only filter", SettingCategory.Browser, SettingType.Boolean, false, searchable = false),

    SettingDef("stats.default-period", "Default stats period", "Dashboard default range", SettingCategory.Statistics, SettingType.Enum, "month", options = listOf("day", "week", "month", "year", "all")),
    SettingDef("stats.show-goals", "Show goals", "Display goal progress", SettingCategory.Statistics, SettingType.Boolean, true),

    SettingDef("account.profile-name", "Profile name", "Your display name", SettingCategory.General, SettingType.String, "Learner"),
    SettingDef("account.learner-level", "Learner level", "How far along you are", SettingCategory.General, SettingType.Enum, "beginner", options = listOf("beginner", "intermediate", "advanced")),
    SettingDef("account.joined-at", "Account created", "When this profile was created (internal)", SettingCategory.General, SettingType.String, "", searchable = false),
    SettingDef("account.last-backup-at", "Last backup", "Timestamp of the most recent backup (internal)", SettingCategory.General, SettingType.String, "", searchable = false),

    SettingDef("history.max-entries", "Max activity entries", "Ring buffer size", SettingCategory.History, SettingType.Int, 2000),

    SettingDef("import.default-policy", "Default conflict policy", "How imports resolve collisions", SettingCategory.ImportExport, SettingType.Enum, "keep-existing", options = listOf("keep-existing", "overwrite", "skip", "keep-newest")),

    SettingDef("sync.auto", "Automatic sync", "Sync on a schedule", SettingCategory.Sync, SettingType.Boolean, false),
    SettingDef("sync.interval-minutes", "Sync interval", "Minutes between syncs", SettingCategory.Sync, SettingType.Int, 30),

    SettingDef("updates.channel", "Update channel", "Which release channel to check (stable / beta / nightly)", SettingCategory.Updates, SettingType.Enum, "stable", options = listOf("stable", "beta", "nightly"), searchable = false),
    SettingDef("updates.check-on-startup", "Check for updates on launch", "Quietly check the feed when the app starts", SettingCategory.Updates, SettingType.Boolean, false),

    // ---- KJD language database patches ------------------------------
    SettingDef("updates.kjd-check-on-startup", "Update language database on launch", "Download and apply KJD data patches when the app starts", SettingCategory.Updates, SettingType.Boolean, true, group = "Language data"),
    SettingDef("updates.kjd-channel", "Language data channel", "Which KJD patch feed to check (stable / beta / nightly)", SettingCategory.Updates, SettingType.Enum, "stable", options = listOf("stable", "beta", "nightly"), group = "Language data"),
    SettingDef("updates.kjd-last-checked", "Last data check (internal)", "When the KJD feed was last checked", SettingCategory.Updates, SettingType.String, "", searchable = false, group = "Language data"),
    SettingDef("updates.kjd-applied-version", "Applied data version (internal)", "Database version the bundled language data is at", SettingCategory.Updates, SettingType.String, "", searchable = false, group = "Language data"),
    SettingDef("updates.kjd-applied-fingerprint", "Applied data fingerprint (internal)", "Fingerprint of the current bundled language database", SettingCategory.Updates, SettingType.String, "", searchable = false, group = "Language data"),

    SettingDef("plugins.enabled", "Plugins enabled", "Load plugins on start", SettingCategory.Plugins, SettingType.Boolean, true),
    SettingDef("plugins.installed", "Installed plugins", "Serialized plugin registry (internal)", SettingCategory.Plugins, SettingType.String, "", searchable = false),

    SettingDef("accessibility.screen-reader", "Screen reader", "Enable accessibility announcements", SettingCategory.Accessibility, SettingType.Boolean, false),
    SettingDef("accessibility.font-scale", "Font scale", "1.0 = default", SettingCategory.Accessibility, SettingType.Float, 1.0f),

    SettingDef("advanced.dev-mode", "Developer mode", "Show advanced diagnostics", SettingCategory.Advanced, SettingType.Boolean, false),
    SettingDef("advanced.telemetry", "Anonymous telemetry", "Send anonymous usage stats", SettingCategory.Advanced, SettingType.Boolean, false),

    // ---- Media workspace -------------------------------------------
    SettingDef("media.default-speed", "Default playback speed", "Speed applied when media opens", SettingCategory.Media, SettingType.Float, 1.0f, group = "Playback"),
    SettingDef("media.resume-playback", "Resume playback", "Continue from the last position", SettingCategory.Media, SettingType.Boolean, true, group = "Playback"),
    SettingDef("media.seek-amount-ms", "Arrow seek amount (ms)", "How far ← / → jump", SettingCategory.Media, SettingType.Int, 5000, group = "Playback"),
    SettingDef("media.condensed-playback", "Condensed playback", "Skip unsubtitled sections", SettingCategory.Media, SettingType.Boolean, false, group = "Playback"),
    SettingDef("media.condensed-gap-ms", "Condensed gap threshold (ms)", "Skip gaps longer than this", SettingCategory.Media, SettingType.Int, 4000, group = "Playback"),
    SettingDef("media.auto-pause", "Auto-pause", "Pause automatically around cues", SettingCategory.Media, SettingType.Enum, "off", options = listOf("off", "at-cue-start", "at-cue-end", "before-cue"), group = "Playback"),

    SettingDef("media.subtitle-font-size", "Subtitle font size", "Overlay subtitle size (pt)", SettingCategory.Media, SettingType.Int, 20, group = "Subtitles"),
    SettingDef("media.subtitle-outline", "Subtitle outline", "Readable outline behind text", SettingCategory.Media, SettingType.Boolean, true, group = "Subtitles"),
    SettingDef("media.subtitle-position", "Subtitle position", "Where the overlay sits", SettingCategory.Media, SettingType.Enum, "bottom", options = listOf("bottom", "top"), group = "Subtitles"),
    SettingDef("media.subtitle-annotation", "Annotation mode", "Token annotation style", SettingCategory.Media, SettingType.Enum, "status", options = listOf("off", "reading", "status", "frequency"), group = "Subtitles"),
    SettingDef("media.subtitle-theme", "Subtitle theme", "Classic / Minimal / Cinema / High contrast / Custom", SettingCategory.Media, SettingType.Enum, "classic", options = listOf("classic", "minimal", "cinema", "high-contrast", "custom"), group = "Subtitles"),
    SettingDef("media.subtitle-opacity", "Subtitle background opacity", "Backdrop strength when the theme is Custom", SettingCategory.Media, SettingType.Float, 0.55f, group = "Subtitles"),
    SettingDef("media.subtitle-weight", "Subtitle weight", "Text weight when the theme is Custom", SettingCategory.Media, SettingType.Enum, "bold", options = listOf("normal", "medium", "semibold", "bold"), group = "Subtitles"),
    SettingDef("media.dual-subtitles", "Dual subtitles", "Show a second subtitle track", SettingCategory.Media, SettingType.Boolean, false, group = "Subtitles"),

    SettingDef("media.mine-screenshot", "Capture screenshot on mine", "Attach a frame to mined cards", SettingCategory.Media, SettingType.Boolean, true, group = "Mining"),
    SettingDef("media.mine-audio", "Capture audio on mine", "Attach an audio clip to mined cards", SettingCategory.Media, SettingType.Boolean, true, group = "Mining"),
    SettingDef("media.mine-video", "Capture video clip on mine", "Attach an MP4 clip of the cue range (needs ffmpeg)", SettingCategory.Media, SettingType.Boolean, false, group = "Mining"),
    SettingDef("media.audio-padding-ms", "Audio clip padding (ms)", "Extra audio before/after a cue", SettingCategory.Media, SettingType.Int, 200, group = "Mining"),
    SettingDef("media.mine-deck", "Default mining deck", "Deck used for media cards", SettingCategory.Media, SettingType.String, "default", group = "Mining"),
    SettingDef("media.mine-duplicate-policy", "Duplicate policy", "What to do with a repeated sentence+word", SettingCategory.Media, SettingType.Enum, "create", options = listOf("create", "skip", "update"), group = "Mining"),

    SettingDef("media.mining-mode", "Mining destination", "Where mined cards are sent", SettingCategory.Media, SettingType.Enum, "kaiteyo", options = listOf("kaiteyo", "forward", "both"), group = "Integrations"),
    SettingDef("media.gsm.host", "GameSentenceMiner host", "Local GSM server address", SettingCategory.Media, SettingType.String, "127.0.0.1", group = "Integrations"),
    SettingDef("media.gsm.port", "GameSentenceMiner port", "Local GSM server port", SettingCategory.Media, SettingType.Int, 9000, group = "Integrations"),
    SettingDef("media.gsm.path", "GameSentenceMiner path", "Card submission endpoint", SettingCategory.Media, SettingType.String, "/api/save", group = "Integrations"),
    SettingDef("media.gsm.token", "GameSentenceMiner token", "Optional bearer token", SettingCategory.Media, SettingType.String, "", group = "Integrations"),

    SettingDef("media.study-mode-default", "Study mode by default", "Count watch time as study time", SettingCategory.Media, SettingType.Boolean, false, group = "Study"),
    SettingDef("media.hotkeys", "Media hotkeys", "Space, ←→, A mine, D dictionary, F transcript", SettingCategory.Media, SettingType.Boolean, true, group = "Study", searchable = false),

    SettingDef("media.resume-prompt", "Ask before resuming", "Offer Resume / Start over when reopening media", SettingCategory.Media, SettingType.Boolean, true, group = "Playback"),
    SettingDef("media.perf-profile", "Performance profile", "Battery / Balanced / Quality backend presets", SettingCategory.Media, SettingType.Enum, "balanced", options = listOf("battery", "balanced", "quality"), group = "Playback"),
    SettingDef("media.replay-count", "Replay count", "How many times R replays a subtitle", SettingCategory.Media, SettingType.Int, 1, group = "Playback"),
    SettingDef("media.mini-player", "Persistent mini player", "Keep playing in a compact player while browsing", SettingCategory.Media, SettingType.Boolean, false, group = "Playback"),
    SettingDef("media.auto-advance", "Auto-advance episodes", "Play the next episode (or queued item) when one finishes", SettingCategory.Media, SettingType.Boolean, false, group = "Playback"),
    SettingDef("media.watch-folders", "Watch library folders", "Auto-add new media that appears in watched folders", SettingCategory.Media, SettingType.Boolean, false, group = "Playback"),
    SettingDef("media.system-media-keys", "System media keys", "Control playback with global media keys (Play/Pause, Next, Previous, Stop) while the app is in the background", SettingCategory.Media, SettingType.Boolean, true, group = "Playback"),
    SettingDef("media.notifications", "Playback notifications", "Show desktop notifications when playback starts, pauses or finishes", SettingCategory.Media, SettingType.Boolean, true, group = "Playback"),
    SettingDef("media.mpv-shader", "mpv shader file", "Optional GLSL shader path (e.g. an Anime4K pipeline) passed to mpv", SettingCategory.Media, SettingType.String, "", group = "Playback"),

    SettingDef("media.text-hook.enabled", "Text hook server", "Accept Japanese text lines over TCP (texthookers, scripts)", SettingCategory.Media, SettingType.Boolean, false, group = "Integrations"),
    SettingDef("media.text-hook.port", "Text hook port", "TCP port the text hook listens on", SettingCategory.Media, SettingType.Int, 8766, group = "Integrations"),
    SettingDef("media.ws.enabled", "Player WebSocket", "Broadcast live player state + accept commands", SettingCategory.Media, SettingType.Boolean, false, group = "Integrations"),
    SettingDef("media.ws.port", "WebSocket port", "ws://127.0.0.1:<port>", SettingCategory.Media, SettingType.Int, 8765, group = "Integrations"),

    SettingDef("media.api.enabled", "Local integration API", "Start the local HTTP API on launch (browser connector / scripts)", SettingCategory.Media, SettingType.Boolean, false, group = "Integrations"),
    SettingDef("media.api.port", "Local API port", "Port the local integration HTTP server binds to", SettingCategory.Media, SettingType.Int, 48201, group = "Integrations"),
    SettingDef("media.api.token", "Local API token (internal)", "Auto-generated bearer token protecting the local API", SettingCategory.Media, SettingType.String, "", searchable = false, group = "Integrations"),

    SettingDef("media.anki.enabled", "AnkiConnect", "Show AnkiConnect as an available integration", SettingCategory.Media, SettingType.Boolean, false, group = "Integrations"),
    SettingDef("media.anki.send-mined", "Forward mined cards to Anki", "Send every completed mine to AnkiConnect as well as Kaiteyo", SettingCategory.Media, SettingType.Boolean, false, group = "Integrations"),
    SettingDef("media.anki.host", "AnkiConnect host", "AnkiConnect server address", SettingCategory.Media, SettingType.String, "127.0.0.1", group = "Integrations"),
    SettingDef("media.anki.port", "AnkiConnect port", "AnkiConnect server port", SettingCategory.Media, SettingType.Int, 8765, group = "Integrations"),
    SettingDef("media.anki.key", "AnkiConnect API key", "Optional API key (AnkiConnect 2.1.55+) for a locked server", SettingCategory.Media, SettingType.String, "", group = "Integrations"),

    SettingDef("media.condensed-fast-forward", "Fast-forward unsubtitled gaps", "With condensed playback, advance quickly through gaps instead of jumping straight to the next subtitle", SettingCategory.Media, SettingType.Boolean, false, group = "Playback")
)
