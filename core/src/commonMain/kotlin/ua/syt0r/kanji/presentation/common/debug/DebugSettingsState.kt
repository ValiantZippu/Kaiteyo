package ua.syt0r.kanji.presentation.common.debug

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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================================
// DEBUG SETTINGS
// ------------------------------------------------------------
// Persisted developer-tool state for the Debug Panel. Separate
// from NavigationSettings so the two surfaces own their state
// independently; stored as one JSON blob (same pattern as
// navSettingsJson / themeSettingsJson).
//
// Only settings that genuinely control something live here.
// A control that does nothing would be a ghost control — when a
// debug feature cannot be implemented honestly (e.g. a layout-
// bounds overlay), it is intentionally absent rather than fake.
// ============================================================

@Serializable
data class DebugSettings(
    /** Show the bottom-corner page identity (Page / Route / Panel). */
    val showPageInfo: Boolean = false,
    /** Show a live FPS readout (smoothed from frame times). */
    val showFps: Boolean = false,
    /** Show the current window/viewport size. */
    val showViewport: Boolean = false,
    /** Force-snap all navigation transitions (debugging motion issues). */
    val disableAnimations: Boolean = false
) {
    /** True when any debug overlay surface is enabled. */
    val anyEnabled: Boolean
        get() = showPageInfo || showFps || showViewport
}

class DebugSettingsState(
    private val appPreferences: PreferencesContract.AppPreferences,
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
) {

    private val scope = CoroutineScope(dispatcher)

    var settings by mutableStateOf(load())
        private set

    /** Apply a change immediately (live) and schedule a persistence write. */
    fun update(transform: (DebugSettings) -> DebugSettings) {
        settings = transform(settings)
        persist()
    }

    fun reset() {
        settings = DebugSettings()
        persist()
    }

    private fun persist() {
        scope.launch {
            runCatching {
                appPreferences.debugSettingsJson.set(json.encodeToString(DebugSettings.serializer(), settings))
            }
        }
    }

    private fun load(): DebugSettings {
        val stored = runBlocking { appPreferences.debugSettingsJson.get() }
        if (!stored.isNullOrBlank()) {
            runCatching {
                return sanitize(json.decodeFromString(DebugSettings.serializer(), stored))
            }
        }
        return DebugSettings()
    }

    /**
     * All fields are booleans, so there is nothing to clamp — kept as a hook
     * so future debug settings with numeric ranges follow the navigation
     * settings sanitize-on-load pattern.
     */
    private fun sanitize(s: DebugSettings): DebugSettings = s

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

val LocalDebugSettings = compositionLocalOf<DebugSettingsState?> { null }

@Composable
fun rememberDebugSettingsState(
    appPreferences: PreferencesContract.AppPreferences
): DebugSettingsState {
    return remember { DebugSettingsState(appPreferences) }
}
