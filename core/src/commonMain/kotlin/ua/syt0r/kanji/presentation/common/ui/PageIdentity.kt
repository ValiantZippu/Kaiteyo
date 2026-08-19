package ua.syt0r.kanji.presentation.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

// ============================================================
// PAGE IDENTITY
// ------------------------------------------------------------
// Every major screen declares a PageIdentity (id, human name,
// route, optional panel) through a single CompositionLocal. The
// debug overlay reads it, so bug reports can name the exact page
// without manually hardcoding page names into dozens of screens.
//
// Example:
//   Page: Browse        Route: /browse      Panel: SearchResults
//
// The registry maps analytics codes to human-readable names so
// the shell can build an identity for destinations that do not
// declare one explicitly.
// ============================================================

data class PageIdentity(
    val id: String,
    val name: String,
    val route: String,
    /** Sub-surface inside the page, e.g. the active tab or panel. */
    val panel: String? = null
) {
    /** The "copy debug info" payload for bug reports. */
    fun summary(): String = buildString {
        append("Page: ").append(name).append('\n')
        append("Route: ").append(route).append('\n')
        if (panel != null) append("Panel: ").append(panel).append('\n')
        append("Id: ").append(id)
    }
}

/**
 * The current page identity, provided by the shell and overridable by any
 * screen that knows more detail (e.g. a search screen reporting its active
 * panel). Null = the shell's generic identity should be shown.
 */
val LocalPageIdentity = staticCompositionLocalOf<PageIdentity?> { null }

/** Declares the current page identity for the debug overlay. */
@Composable
fun ProvidePageIdentity(
    identity: PageIdentity,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalPageIdentity provides identity, content = content)
}

/** Reads the current page identity, if one is declared. */
@Composable
fun rememberPageIdentity(): PageIdentity? = LocalPageIdentity.current

/**
 * Human-readable names for analytics codes used by navigation destinations.
 * This is the fallback when a screen does not declare its own identity.
 */
object PageRegistry {

    private val names = mapOf(
        "home" to "Home",
        "deck_picker" to "Deck picker",
        "deck_edit" to "Deck editor",
        "deck_details" to "Deck details",
        "letter_practice" to "Letter practice",
        "vocab_practice" to "Word practice",
        "info" to "Details",
        "backup" to "Backup",
        "feedback" to "Feedback",
        "daily_limit" to "Daily limit",
        "account" to "Account",
        "sync" to "Sync",
        "appearance_studio" to "Appearance studio",
        "text_analysis" to "Text analysis",
        "kanji_browser" to "Kanji browser",
        "kanji_entry" to "Kanji entry",
        "word_entry" to "Word entry",
        "knowledge_graph" to "Knowledge graph",
        "radical_explorer" to "Radical explorer",
        "knowledge_explorer" to "Dictionary explorer",
        "sentence_entry" to "Sentence",
        "component_explorer" to "Component explorer",
        "browse_hub" to "Browse",
        "collection_detail" to "Collection",
        "statistics" to "Statistics",
        "search" to "Search",
        "library" to "Library",
        "browse" to "Browse",
        "settings" to "Settings",
        "media" to "Media",
        "game" to "World",
        "world" to "World 3D",
        "card_settings" to "Card layouts",
        "learner_profile" to "Learner profile",
        "about" to "About",
        "credits" to "Credits"
    )

    /** Human-readable name for an analytics code; falls back to the raw code. */
    fun nameFor(analyticsName: String?): String {
        val raw = analyticsName ?: "home"
        return names[raw]
            ?: raw.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
    }

    /** Route string for an analytics code ("/browse"). */
    fun routeFor(analyticsName: String?): String = "/${analyticsName ?: "home"}"
}
