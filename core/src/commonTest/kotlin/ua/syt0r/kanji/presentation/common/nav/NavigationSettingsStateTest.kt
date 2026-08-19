package ua.syt0r.kanji.presentation.common.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ua.syt0r.kanji.core.suspended_property.SuspendedProperty
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesDeckDetailsLetterLayout
import ua.syt0r.kanji.core.user_data.preferences.PreferencesDefaultHomeTab
import ua.syt0r.kanji.core.user_data.preferences.PreferencesLetterPracticeType
import ua.syt0r.kanji.core.user_data.preferences.PreferencesLetterSortOption
import ua.syt0r.kanji.core.user_data.preferences.PreferencesSyncDataInfo
import ua.syt0r.kanji.core.user_data.preferences.PreferencesTheme
import ua.syt0r.kanji.core.user_data.preferences.PreferencesUserInfo
import ua.syt0r.kanji.core.user_data.preferences.PreferencesVocabPracticeType
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition

/**
 * Regression tests for the navigation settings store: persisted values are
 * sanitized on load (a stale/hand-edited blob can never crash layout), legacy
 * three-mode blobs and legacy individual prefs migrate onto the two-mode model,
 * and mode changes persist through the single JSON property.
 */
class NavigationSettingsStateTest {

    private class FakeAppPreferences : PreferencesContract.AppPreferences {

        private val values = mutableMapOf<String, Any?>()

        private fun <T> mem(key: String, default: T): SuspendedProperty<T> =
            object : SuspendedProperty<T> {
                override val key: String = key
                override val onModified: SharedFlow<T> = MutableStateFlow(default)
                @Suppress("UNCHECKED_CAST")
                override suspend fun get(): T = values[key] as? T ?: default
                override suspend fun set(value: T) {
                    values[key] = value
                }
                override suspend fun isModified(): Boolean = values.containsKey(key)
                override suspend fun backup(): kotlinx.serialization.json.JsonPrimitive? = null
                override suspend fun restore(value: kotlinx.serialization.json.JsonPrimitive) = Unit
            }

        override val refreshToken get() = mem("refreshToken", null as String?)
        override val idToken get() = mem("idToken", null as String?)
        override val userInfo get() = mem("userInfo", null as PreferencesUserInfo?)
        override val subscriptionAlert get() = mem("subscriptionAlert", null as String?)
        override val localDataId get() = mem("localDataId", "")
        override val localDataTimestamp get() = mem("localDataTimestamp", null as Instant?)
        override val lastSyncedDataInfo get() = mem("lastSyncedDataInfo", null as PreferencesSyncDataInfo?)
        override val analyticsEnabled get() = mem("analyticsEnabled", false)
        override val practiceType get() = mem("practiceType", PreferencesLetterPracticeType.Writing)
        override val filterNew get() = mem("filterNew", false)
        override val filterDue get() = mem("filterDue", false)
        override val filterDone get() = mem("filterDone", false)
        override val sortOption get() = mem("sortOption", PreferencesLetterSortOption.AddOrder)
        override val isSortDescending get() = mem("isSortDescending", false)
        override val practicePreviewLayout get() = mem("practicePreviewLayout", PreferencesDeckDetailsLetterLayout.Character)
        override val kanaGroupsEnabled get() = mem("kanaGroupsEnabled", false)
        override val theme get() = mem("theme", PreferencesTheme.System)
        override val dailyLimitEnabled get() = mem("dailyLimitEnabled", false)
        override val dailyLimitConfigurationJson get() = mem("dailyLimitConfigurationJson", "")
        override val reminderEnabled get() = mem("reminderEnabled", false)
        override val reminderTime get() = mem("reminderTime", LocalTime(21, 0))
        override val defaultHomeTab get() = mem("defaultHomeTab", PreferencesDefaultHomeTab.GeneralDashboard)
        override val lastAppVersionWhenChangesDialogShown get() = mem("lastAppVersionWhenChangesDialogShown", "")
        override val tutorialSeen get() = mem("tutorialSeen", false)
        override val onboardingCompleted get() = mem("onboardingCompleted", false)
        override val generalDashboardStudyTargets get() = mem("generalDashboardStudyTargets", emptyMap<String, Boolean>())
        override val letterDashboardPracticeType get() = mem("letterDashboardPracticeType", PreferencesLetterPracticeType.Writing)
        override val letterDashboardSortByTime get() = mem("letterDashboardSortByTime", false)
        override val vocabDashboardPracticeType get() = mem("vocabDashboardPracticeType", PreferencesVocabPracticeType.Flashcard)
        override val vocabDashboardSortByTime get() = mem("vocabDashboardSortByTime", false)
        override val vocabNoteTypeId get() = mem("vocabNoteTypeId", "kaiteyo-default")
        override val dailyResetTime get() = mem("dailyResetTime", LocalTime(0, 0))
        override val navSidebarMode get() = mem("navSidebarMode", "")
        override val navSidebarPosition get() = mem("navSidebarPosition", "")
        override val navAutoHide get() = mem("navAutoHide", "")
        override val navCollapsed get() = mem("navCollapsed", false)
        override val navWidth get() = mem("navWidth", 0)
        override val navHeight get() = mem("navHeight", 0)
        override val navFloatingOffsetX get() = mem("navFloatingOffsetX", 0)
        override val navFloatingOffsetY get() = mem("navFloatingOffsetY", 0)
        override val navAccentIndex get() = mem("navAccentIndex", 0)
        override val navSettingsJson get() = mem("navSettingsJson", "")
        override val themeSettingsJson get() = mem("themeSettingsJson", "")
        override val debugSettingsJson get() = mem("debugSettingsJson", "")
        override val learnerProfileJson get() = mem("learnerProfileJson", "")
        override val reviewSettingsJson get() = mem("reviewSettingsJson", "")
        override val backupConfigJson get() = mem("backupConfigJson", "")
        override val savedSearchesJson get() = mem("savedSearchesJson", "")
        override val homeCommandCenterJson get() = mem("homeCommandCenterJson", "")
        override val mediaReferencesJson get() = mem("mediaReferencesJson", "")
        override val deckFavoritesJson get() = mem("deckFavoritesJson", "")
        override val browserColumnsJson get() = mem("browserColumnsJson", "")
        override val shortcutBindingsJson get() = mem("shortcutBindingsJson", "")
        override val tagSortOrder get() = mem("tagSortOrder", "")
        override val collectionSortOrder get() = mem("collectionSortOrder", "")
        override val browserLastQuery get() = mem("browserLastQuery", "")
        override val statisticsGoalsJson get() = mem("statisticsGoalsJson", "")
        override val statisticsGoalHistoryJson get() = mem("statisticsGoalHistoryJson", "")
        override val kanjiCardLayoutJson get() = mem("kanjiCardLayoutJson", "")
        override val wordCardLayoutJson get() = mem("wordCardLayoutJson", "")
        override val sentenceCardLayoutJson get() = mem("sentenceCardLayoutJson", "")
        override val grammarCardLayoutJson get() = mem("grammarCardLayoutJson", "")
        override val collectionCardLayoutJson get() = mem("collectionCardLayoutJson", "")
        override val gameProgressJson get() = mem("gameProgressJson", "")
    }

    // ---------------------------------------------------------------
    // Sanitization
    // ---------------------------------------------------------------

    @Test
    fun storedOutOfRangeValuesAreClampedOnLoad() = runBlocking {
        val prefs = FakeAppPreferences()
        prefs.navSettingsJson.set(
            """
            {
              "bubble": {
                "size": 500, "iconSize": 2, "snapSensitivity": 5,
                "snapDistance": 1000, "safeMargin": 99, "holdDurationMs": 50,
                "fadeOpacity": 0.05, "elevation": 99, "idleTimeoutMs": 100
              },
              "snapOffsetX": -50000, "snapOffsetY": 50000,
              "animationDurationMs": 5000,
              "sidebar": { "expandedWidthIndex": 99, "iconSize": 200, "compactSpacing": 100 },
              "launchpad": { "scale": 5, "spacing": 9, "opacity": 0.1 }
            }
            """.trimIndent()
        )

        val state = NavigationSettingsState(prefs)
        val settings = state.settings

        assertEquals(84, settings.bubble.size)
        assertEquals(14, settings.bubble.iconSize)
        assertEquals(20, settings.bubble.snapSensitivity)
        assertEquals(400, settings.bubble.snapDistance)
        assertEquals(48, settings.bubble.safeMargin)
        assertEquals(200, settings.bubble.holdDurationMs)
        assertEquals(0.15f, settings.bubble.fadeOpacity)
        assertEquals(40, settings.bubble.elevation)
        assertEquals(2000, settings.bubble.idleTimeoutMs)

        assertEquals(-4000, settings.snapOffsetX)
        assertEquals(4000, settings.snapOffsetY)
        assertEquals(2000, settings.animationDurationMs)

        assertEquals(ExpandedWidthOptions.lastIndex, settings.sidebar.expandedWidthIndex)
        assertEquals(48, settings.sidebar.iconSize)
        assertEquals(32, settings.sidebar.compactSpacing)

        assertEquals(1.4f, settings.launchpad.scale)
        assertEquals(1.6f, settings.launchpad.spacing)
        assertEquals(0.5f, settings.launchpad.opacity)
    }

    @Test
    fun phoneSidebarEdgeNeverLeftOrRight() = runBlocking {
        val prefs = FakeAppPreferences()
        prefs.navSettingsJson.set(
            """{"phone": {"edge": "Left", "snapOffsetX": -99999, "snapOffsetY": 99999}}"""
        )
        val state = NavigationSettingsState(prefs)
        // Left/Right are impossible on phones → corrected to Bottom; offsets clamped.
        assertEquals(SidebarPosition.Bottom, state.settings.phone.edge)
        assertEquals(-4000, state.settings.phone.snapOffsetX)
        assertEquals(4000, state.settings.phone.snapOffsetY)
    }

    // ---------------------------------------------------------------
    // Legacy migration
    // ---------------------------------------------------------------

    @Test
    fun legacyThreeModeBlobMapsOntoTwoModeModel() = runBlocking {
        val prefs = FakeAppPreferences()
        // A blob written by the old three-mode schema: unknown enum names for
        // mode/sidebarExpansion/snapPoint mean the current-schema decode fails
        // and the legacy rebuild path must map them.
        prefs.navSettingsJson.set(
            """
            {
              "mode": "FloatingIsland",
              "sidebarExpansion": "IconsOnly",
              "bubbleAnchor": "Left",
              "bubbleOffsetX": 12, "bubbleOffsetY": -8
            }
            """.trimIndent()
        )
        val state = NavigationSettingsState(prefs)
        assertEquals(NavigationMode.Floating, state.settings.mode)
        // legacyModeToSidebarExpansion keys off the MODE name: "FloatingIsland"
        // is not IconsOnly/AutoHide/Compact, so the layout falls back to Expanded.
        assertEquals(SidebarExpansion.Expanded, state.settings.sidebarExpansion)
        assertEquals(BubbleSnapPoint.LeftCenter, state.settings.snapPoint)
        assertEquals(12, state.settings.snapOffsetX)
        assertEquals(-8, state.settings.snapOffsetY)
    }

    @Test
    fun legacyIndividualPrefsMigrateOnFirstLoad() = runBlocking {
        val prefs = FakeAppPreferences()
        prefs.navSettingsJson.set("")
        prefs.navSidebarMode.set("Bubble")
        prefs.navSidebarPosition.set("Right")
        prefs.navWidth.set(1000)

        val state = NavigationSettingsState(prefs)
        assertEquals(NavigationMode.Floating, state.settings.mode)
        assertEquals(SidebarPosition.Right, state.settings.desktopEdge)
        assertEquals(BubbleSnapPoint.RightCenter, state.settings.snapPoint)
        // 1000dp is nearest the widest option (340dp) → last index.
        assertEquals(ExpandedWidthOptions.lastIndex, state.settings.sidebar.expandedWidthIndex)

        // The migration is persisted so the legacy prefs are no longer consulted.
        val persisted = prefs.navSettingsJson.get()
        assertTrue(persisted.contains("Floating"), "persisted: $persisted")
    }

    // ---------------------------------------------------------------
    // Mode restore / persistence
    // ---------------------------------------------------------------

    @Test
    fun rememberPreviousModeFalseUsesDefaultMode() = runBlocking {
        val prefs = FakeAppPreferences()
        prefs.navSettingsJson.set(
            """{"rememberPreviousMode": false, "defaultMode": "Floating", "mode": "Sidebar"}"""
        )
        val state = NavigationSettingsState(prefs)
        assertEquals(NavigationMode.Floating, state.settings.mode)
    }

    @Test
    fun lastModeIsRestoredWhenRemembering() = runBlocking {
        val prefs = FakeAppPreferences()
        prefs.navSettingsJson.set(
            """{"rememberPreviousMode": true, "lastMode": "Floating", "mode": "Sidebar"}"""
        )
        val state = NavigationSettingsState(prefs)
        assertEquals(NavigationMode.Floating, state.settings.mode)
    }

    @Test
    fun setModeTracksLastModeAndPersists() = runBlocking {
        val prefs = FakeAppPreferences()
        val state = NavigationSettingsState(prefs)
        // Fresh defaults: Sidebar.
        assertEquals(NavigationMode.Sidebar, state.settings.mode)

        state.setMode(NavigationMode.Floating)
        assertEquals(NavigationMode.Floating, state.settings.mode)
        assertEquals(NavigationMode.Floating, state.settings.lastMode)

        val persisted = prefs.navSettingsJson.get()
        assertTrue(persisted.contains("\"mode\":\"Floating\""), "persisted: $persisted")
    }

    @Test
    fun resetReturnsToDefaults() = runBlocking {
        val prefs = FakeAppPreferences()
        prefs.navSettingsJson.set(
            """{"mode": "Floating", "desktopEdge": "Right", "bubble": {"size": 84}}"""
        )
        val state = NavigationSettingsState(prefs)
        assertEquals(NavigationMode.Floating, state.settings.mode)

        state.reset()
        assertEquals(NavigationMode.Sidebar, state.settings.mode)
        assertEquals(SidebarPosition.Left, state.settings.desktopEdge)
        assertEquals(56, state.settings.bubble.size)
    }
}
