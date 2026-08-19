package ua.syt0r.kanji.core.knowledge.cards

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

class KanjiCardModelsTest {

    private class FakePreferences : PreferencesContract.AppPreferences {
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
                override suspend fun backup(): JsonPrimitive? = null
                override suspend fun restore(value: JsonPrimitive) = Unit
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
        override val learnerProfileJson get() = mem("learnerProfileJson", "")
        override val gameProgressJson get() = mem("gameProgressJson", "")
    }

    // ---------------------------------------------------------------
    // Layout operations
    // ---------------------------------------------------------------

    @Test
    fun defaultLayoutShowsEverythingInRegistryOrder() {
        val layout = KanjiCardLayout()
        assertEquals(KanjiCardType.entries.map { it.id }, layout.order)
        assertEquals(KanjiCardType.entries.size, layout.visibleCards().size)
    }

    @Test
    fun hideAndShowToggle() {
        val layout = KanjiCardLayout()
        val hidden = layout.setVisible(KanjiCardType.Graph, false)
        assertFalse(hidden.isVisible(KanjiCardType.Graph))
        assertTrue(hidden.visibleCards().none { it == KanjiCardType.Graph })
        val restored = hidden.setVisible(KanjiCardType.Graph, true)
        assertTrue(restored.isVisible(KanjiCardType.Graph))
    }

    @Test
    fun moveUpAndDown() {
        val layout = KanjiCardLayout()
        val movedUp = layout.moveUp(KanjiCardType.Readings)
        // Readings moves above Meaning.
        val index = movedUp.order.indexOf(KanjiCardType.Readings.id)
        assertEquals(KanjiCardType.Meaning.id, movedUp.order[index - 1])

        val movedDown = layout.moveDown(KanjiCardType.Hero)
        assertEquals(KanjiCardType.Meaning.id, movedDown.order.first())
    }

    @Test
    fun moveAtBoundsIsNoOp() {
        val layout = KanjiCardLayout()
        assertEquals(layout, layout.moveUp(KanjiCardType.Hero))
        assertEquals(layout, layout.moveDown(KanjiCardType.Study))
    }

    @Test
    fun sanitizedDropsUnknownIds() {
        val layout = KanjiCardLayout(
            order = listOf("hero", "meaning", "no-such-card"),
            hidden = setOf("graph", "also-missing")
        ).sanitized()
        assertEquals(listOf("hero", "meaning"), layout.order)
        assertEquals(setOf("graph"), layout.hidden)
    }

    @Test
    fun visibleCardsAppendsRegistryTypesMissingFromSavedOrder() {
        // A user saved a layout before Media existed; rendering must still
        // show it (appended after the saved order), without mutating the
        // stored order.
        val layout = KanjiCardLayout(
            order = listOf("hero", "meaning"),
            hidden = setOf("study")
        )
        val visible = layout.visibleCards()
        assertEquals(listOf("hero", "meaning"), layout.order)
        assertEquals(KanjiCardType.entries.size - 1, visible.size) // study hidden
        assertTrue(visible.map { it.id }.containsAll(listOf("hero", "meaning", "media")))
        assertTrue(visible.none { it == KanjiCardType.Study })
    }

    // ---------------------------------------------------------------
    // Presets
    // ---------------------------------------------------------------

    @Test
    fun presetsDifferSensibly() {
        val minimal = KanjiCardPresets.Minimal
        val research = KanjiCardPresets.Research
        assertTrue(minimal.hidden.isNotEmpty())
        assertTrue(research.hidden.isEmpty())
        assertTrue(research.visibleCards().size > minimal.visibleCards().size)
    }

    @Test
    fun presetLookupById() {
        assertEquals("standard", KanjiCardPresets.byId("standard")?.id)
        assertEquals(null, KanjiCardPresets.byId("nope"))
    }

    @Test
    fun newPresetsAreValidAndDistinct() {
        val ids = KanjiCardPresets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "preset ids must be unique")
        listOf("intermediate", "writing", "reading", "dictionary").forEach { id ->
            val preset = KanjiCardPresets.byId(id)
            assertTrue(preset != null, "preset $id must exist")
            preset!!.layout.hidden.forEach { hiddenId ->
                assertTrue("$hiddenId must be a real card id", KanjiCardType.byId(hiddenId) != null)
            }
            assertTrue("preset $id must keep cards visible", preset.layout.visibleCards().isNotEmpty())
        }
        // Writing keeps strokes; Reading keeps sentences; Dictionary keeps all data cards.
        assertTrue(KanjiCardPresets.byId("writing")!!.layout.isVisible(KanjiCardType.Stroke))
        assertTrue(KanjiCardPresets.byId("reading")!!.layout.isVisible(KanjiCardType.Sentence))
        assertTrue(KanjiCardPresets.byId("dictionary")!!.layout.isVisible(KanjiCardType.Frequency))
    }

    // ---------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------

    @Test
    fun storeRoundTripsLayout() = runBlocking {
        val prefs = FakePreferences()
        val store = KanjiCardLayoutStore(prefs)
        val layout = KanjiCardLayout()
            .setVisible(KanjiCardType.Stroke, false)
            .setVisible(KanjiCardType.Graph, false)

        store.save(layout)
        val loaded = store.load()
        assertEquals(layout.order, loaded.order)
        assertEquals(layout.hidden, loaded.hidden)
    }

    @Test
    fun storeFallsBackToDefaultsOnBlankAndCorrupt() = runBlocking {
        val prefs = FakePreferences()
        val store = KanjiCardLayoutStore(prefs)
        assertEquals(KanjiCardLayout(), store.load())

        prefs.kanjiCardLayoutJson.set("{ this is not json")
        assertEquals(KanjiCardLayout(), store.load())
    }

    @Test
    fun storeResetClearsPersistedValue() = runBlocking {
        val prefs = FakePreferences()
        val store = KanjiCardLayoutStore(prefs)
        store.save(KanjiCardLayout().setVisible(KanjiCardType.Study, false))
        store.reset()
        assertEquals("", prefs.kanjiCardLayoutJson.get())
        assertEquals(KanjiCardLayout(), store.load())
    }

    @Test
    fun exampleLimitFallsBackToDefault() {
        val layout = KanjiCardLayout()
        assertEquals(6, layout.exampleLimit(KanjiCardType.Sentence, 6))
        assertEquals(24, layout.exampleLimit(KanjiCardType.Vocabulary, 24))
    }

    @Test
    fun setExampleLimitPersistsAndClampsToOne() {
        val layout = KanjiCardLayout()
            .setExampleLimit(KanjiCardType.Sentence, 12)
        assertEquals(12, layout.exampleLimit(KanjiCardType.Sentence, 6))
        // 0 is not a legal limit — a hidden card disables a section instead.
        val clamped = layout.setExampleLimit(KanjiCardType.Sentence, 0)
        assertEquals(1, clamped.exampleLimit(KanjiCardType.Sentence, 6))
        // Other cards are unaffected.
        assertEquals(24, layout.exampleLimit(KanjiCardType.Vocabulary, 24))
    }

    @Test
    fun setExampleLimitIsSerializedRoundTrip() = runBlocking {
        val store = KanjiCardLayoutStore(prefs)
        val layout = KanjiCardLayout()
            .setExampleLimit(KanjiCardType.Sentence, 9)
            .setExampleLimit(KanjiCardType.Vocabulary, 30)
        store.save(layout)
        val loaded = store.load()
        assertEquals(9, loaded.exampleLimit(KanjiCardType.Sentence, 6))
        assertEquals(30, loaded.exampleLimit(KanjiCardType.Vocabulary, 24))
        assertEquals(18, loaded.exampleLimit(KanjiCardType.Related, 18))
    }

    @Test
    fun layoutSerializes() {
        val layout = KanjiCardLayout().setVisible(KanjiCardType.Component, false)
        val json = Json.encodeToString(layout)
        val decoded = Json.decodeFromString<KanjiCardLayout>(json)
        assertEquals(layout.hidden, decoded.hidden)
    }
}
