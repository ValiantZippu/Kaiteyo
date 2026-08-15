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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition

// ============================================
// NAVIGATION SETTINGS STATE
// Single source of truth for all navigation
// preferences. Persisted as one JSON blob and
// migrated from both the legacy individual prefs
// and the previous three-mode JSON schema on
// first load after upgrade.
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
            decodeStored(stored)?.let { decoded ->
                // "Remember previous mode" off → always start in the configured default.
                val restored = if (decoded.rememberPreviousMode) {
                    decoded.copy(mode = decoded.lastMode ?: decoded.mode)
                } else {
                    decoded.copy(mode = decoded.defaultMode)
                }
                // Persisted values are validated before use: a stale or hand-edited
                // blob (e.g. written by an older build or on a different device)
                // can never produce out-of-range sizes, invalid phone edges or
                // absurd snap offsets that crash layout after relaunch.
                return sanitize(restored)
            }
        }
        return sanitize(migrateFromLegacy())
    }

    /**
     * Clamp every persisted layout value to a safe range and convert impossible
     * combinations (e.g. phone + left/right sidebar) to valid ones. Called on
     * every load so invalid persisted state is corrected instead of crashing.
     */
    private fun sanitize(s: NavigationSettings): NavigationSettings = s.copy(
        bubble = s.bubble.copy(
            size = s.bubble.size.coerceIn(40, 84),
            iconSize = s.bubble.iconSize.coerceIn(14, 40),
            snapSensitivity = s.bubble.snapSensitivity.coerceIn(20, 200),
            snapDistance = s.bubble.snapDistance.coerceIn(20, 400),
            safeMargin = s.bubble.safeMargin.coerceIn(4, 48),
            holdDurationMs = s.bubble.holdDurationMs.coerceIn(200, 2000),
            idleTimeoutMs = s.bubble.idleTimeoutMs.coerceIn(2000, 120_000),
            fadeOpacity = s.bubble.fadeOpacity.coerceIn(0.15f, 0.8f),
            animationSpeed = s.bubble.animationSpeed.coerceIn(0.25f, 3f),
            elevation = s.bubble.elevation.coerceIn(0, 40)
        ),
        snapOffsetX = s.snapOffsetX.coerceIn(-4000, 4000),
        snapOffsetY = s.snapOffsetY.coerceIn(-4000, 4000),
        phone = s.phone.copy(
            edge = if (s.phone.edge == SidebarPosition.Left || s.phone.edge == SidebarPosition.Right)
                SidebarPosition.Bottom else s.phone.edge,
            snapOffsetX = s.phone.snapOffsetX.coerceIn(-4000, 4000),
            snapOffsetY = s.phone.snapOffsetY.coerceIn(-4000, 4000)
        ),
        animationDurationMs = s.animationDurationMs.coerceIn(0, 2000),
        sidebar = s.sidebar.copy(
            expandedWidthIndex = s.sidebar.expandedWidthIndex.coerceIn(0, ExpandedWidthOptions.lastIndex),
            iconSize = s.sidebar.iconSize.coerceIn(12, 48),
            compactSpacing = s.sidebar.compactSpacing.coerceIn(2, 32)
        ),
        launchpad = s.launchpad.copy(
            scale = s.launchpad.scale.coerceIn(0.6f, 1.4f),
            spacing = s.launchpad.spacing.coerceIn(0.6f, 1.6f),
            opacity = s.launchpad.opacity.coerceIn(0.5f, 1f)
        )
    )

    /**
     * Decode the persisted blob. Tries the current schema first; when the
     * stored blob was written by an older build (three modes, six anchors)
     * the enum names no longer match, so the JSON is rebuilt field-by-field
     * with the legacy values mapped onto the new model.
     */
    private fun decodeStored(stored: String): NavigationSettings? {
        runCatching {
            return json.decodeFromString(NavigationSettings.serializer(), stored)
        }
        runCatching {
            val obj = json.parseToJsonElement(stored).jsonObject
            val modeName = obj["mode"]?.jsonPrimitive?.contentOrNull
            val mapped = buildJsonObject {
                put("mode", legacyModeToNavigationMode(modeName).name)
                put("defaultMode", legacyModeToNavigationMode(obj["defaultMode"]?.jsonPrimitive?.contentOrNull).name)
                obj["lastMode"]?.let { put("lastMode", legacyModeToNavigationMode(it.jsonPrimitive.contentOrNull).name) }
                put("sidebarExpansion", legacyModeToSidebarExpansion(modeName).name)
                put("snapPoint", legacyAnchorToSnapPoint(obj["bubbleAnchor"]?.jsonPrimitive?.contentOrNull).name)
                obj["bubbleOffsetX"]?.let { put("snapOffsetX", it.jsonPrimitive.int) }
                obj["bubbleOffsetY"]?.let { put("snapOffsetY", it.jsonPrimitive.int) }
                for (key in listOf(
                    "rememberPreviousMode", "animationsEnabled", "animationDurationMs",
                    "desktopEdge", "bubble", "sidebar", "accessibility"
                )) {
                    obj[key]?.let { put(key, it) }
                }
                obj["phone"]?.let { phoneEl ->
                    val phoneObj = phoneEl.jsonObject
                    buildJsonObject {
                        phoneObj["edge"]?.let { put("edge", it) }
                        put("snapPoint", legacyAnchorToSnapPoint(phoneObj["bubbleAnchor"]?.jsonPrimitive?.contentOrNull).name)
                        phoneObj["bubbleOffsetX"]?.let { put("snapOffsetX", it.jsonPrimitive.int) }
                        phoneObj["bubbleOffsetY"]?.let { put("snapOffsetY", it.jsonPrimitive.int) }
                    }.let { put("phone", it) }
                }
            }
            return json.decodeFromString(NavigationSettings.serializer(), mapped.toString())
        }
        return null
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
            sidebarExpansion = legacyModeToSidebarExpansion(modeName),
            desktopEdge = edge,
            snapPoint = when (edge) {
                SidebarPosition.Right -> BubbleSnapPoint.RightCenter
                SidebarPosition.Top -> BubbleSnapPoint.TopCenter
                SidebarPosition.Bottom -> BubbleSnapPoint.BottomCenter
                else -> BubbleSnapPoint.LeftCenter
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
