package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition

// ============================================
// NAVIGATION SETTINGS STATE
// Single source of truth for all navigation
// preferences. Persisted as one JSON blob and
// migrated from the legacy individual prefs on
// first launch after upgrade.
// ============================================

class NavigationSettingsState(
    private val appPreferences: PreferencesContract.AppPreferences,
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
) {

    private val scope = CoroutineScope(dispatcher)

    var settings by mutableStateOf(load())
        private set

    /** Apply a change immediately (live) and schedule a persistence write. */
    fun update(transform: (NavigationSettings) -> NavigationSettings) {
        val next = transform(settings)
        // Keep track of the last used mode so "remember previous mode" can restore it.
        settings = if (next.mode != settings.mode) next.copy(lastMode = next.mode) else next
        persist()
    }

    /** Switch navigation mode and remember it as the last used mode. */
    fun setMode(mode: NavigationMode) = update { it.copy(mode = mode) }

    fun reset() {
        settings = NavigationSettings()
        persist()
    }

    private fun persist() {
        scope.launch {
            runCatching {
                appPreferences.navSettingsJson.set(json.encodeToString(NavigationSettings.serializer(), settings))
            }
        }
    }

    private fun load(): NavigationSettings {
        val stored = runBlocking { appPreferences.navSettingsJson.get() }
        if (!stored.isNullOrBlank()) {
            runCatching {
                val decoded = json.decodeFromString(NavigationSettings.serializer(), stored)
                // "Remember previous mode" off → always start in the configured default.
                return if (decoded.rememberPreviousMode) {
                    decoded.copy(mode = decoded.lastMode ?: decoded.mode)
                } else {
                    decoded.copy(mode = decoded.defaultMode)
                }
            }
        }
        return migrateFromLegacy()
    }

    private fun migrateFromLegacy(): NavigationSettings {
        val modeName = runBlocking { appPreferences.navSidebarMode.get() }
        val positionName = runBlocking { appPreferences.navSidebarPosition.get() }
        val legacyWidth = runBlocking { appPreferences.navWidth.get() }

        val edge = enumByName(positionName, SidebarPosition.Left)
        val widthIndex = ExpandedWidthOptions.indices
            .minByOrNull { kotlin.math.abs(ExpandedWidthOptions[it] - legacyWidth) }
            ?: 1

        val migrated = NavigationSettings(
            mode = legacyModeToNavigationMode(modeName),
            desktopEdge = edge,
            bubbleAnchor = when (edge) {
                SidebarPosition.Right -> BubbleAnchor.Right
                SidebarPosition.Top -> BubbleAnchor.TopRight
                SidebarPosition.Bottom -> BubbleAnchor.BottomRight
                else -> BubbleAnchor.Left
            },
            sidebar = SidebarSettings(expandedWidthIndex = widthIndex)
        )
        // Persist the migrated blob so the legacy prefs are no longer consulted.
        scope.launch {
            runCatching {
                appPreferences.navSettingsJson.set(json.encodeToString(NavigationSettings.serializer(), migrated))
            }
        }
        return migrated
    }

    private inline fun <reified T : Enum<T>> enumByName(name: String?, default: T): T {
        if (name == null) return default
        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

val LocalNavigationSettings = compositionLocalOf<NavigationSettingsState?> { null }

@Composable
fun rememberNavigationSettingsState(
    appPreferences: PreferencesContract.AppPreferences
): NavigationSettingsState {
    return remember { NavigationSettingsState(appPreferences) }
}
