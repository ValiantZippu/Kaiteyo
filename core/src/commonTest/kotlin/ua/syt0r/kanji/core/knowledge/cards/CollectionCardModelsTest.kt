package ua.syt0r.kanji.core.knowledge.cards

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.suspended_property.SuspendedProperty
import kotlin.test.Test

class CollectionCardModelsTest {

    // ------------------------------------------------------------------
    // Fake preferences
    // ------------------------------------------------------------------

    private class FakePreferences : PreferencesContract.AppPreferences {
        private fun <T> mem(key: String, initial: T): SuspendedProperty<T> =
            FakeSuspendedProperty(key, initial)

        override val refreshToken get() = mem<String?>("refreshToken", null)
        override val idToken get() = mem<String?>("idToken", null)
        override val userInfo get() = mem<ua.syt0r.kanji.core.user_data.preferences.PreferencesUserInfo?>("userInfo", null)
        override val subscriptionAlert get() = mem<String?>("subscriptionAlert", null)
        override val localDataId get() = mem("localDataId", "")
        override val localDataTimestamp get() = mem<kotlinx.datetime.Instant?>("localDataTimestamp", null)
        override val lastSyncedDataInfo get() = mem<ua.syt0r.kanji.core.user_data.preferences.PreferencesSyncDataInfo?>("lastSyncedDataInfo", null)
        override val analyticsEnabled get() = mem("analyticsEnabled", false)
        override val practiceType get() = mem("practiceType", ua.syt0r.kanji.core.user_data.preferences.PreferencesLetterPracticeType.Hiragana)
        override val filterNew get() = mem("filterNew", false)
        override val filterDue get() = mem("filterDue", false)
        override val filterDone get() = mem("filterDone", false)
        override val sortOption get() = mem("sortOption", ua.syt0r.kanji.core.user_data.preferences.PreferencesLetterSortOption.Time)
        override val isSortDescending get() = mem("isSortDescending", false)
        override val practicePreviewLayout get() = mem("practicePreviewLayout", ua.syt0r.kanji.core.user_data.preferences.PreferencesDeckDetailsLetterLayout.Writing)
        override val kanaGroupsEnabled get() = mem("kanaGroupsEnabled", false)
        override val theme get() = mem("theme", ua.syt0r.kanji.core.user_data.preferences.PreferencesTheme.Light)
        override val dailyLimitEnabled get() = mem("dailyLimitEnabled", false)
        override val dailyLimitConfigurationJson get() = mem("dailyLimitConfigurationJson", "")
        override val reminderEnabled get() = mem("reminderEnabled", false)
        override val reminderTime get() = mem("reminderTime", kotlinx.datetime.LocalTime(9, 0))
        override val defaultHomeTab get() = mem("defaultHomeTab", ua.syt0r.kanji.core.user_data.preferences.PreferencesDefaultHomeTab.General)
        override val lastAppVersionWhenChangesDialogShown get() = mem("lastAppVersionWhenChangesDialogShown", "")
        override val tutorialSeen get() = mem("tutorialSeen", false)
        override val onboardingCompleted get() = mem("onboardingCompleted", false)
        override val generalDashboardStudyTargets get() = mem("generalDashboardStudyTargets", emptyMap<String, Boolean>())
        override val letterDashboardPracticeType get() = mem("letterDashboardPracticeType", ua.syt0r.kanji.core.user_data.preferences.PreferencesLetterPracticeType.Hiragana)
        override val letterDashboardSortByTime get() = mem("letterDashboardSortByTime", false)
        override val vocabDashboardPracticeType get() = mem("vocabDashboardPracticeType", ua.syt0r.kanji.core.user_data.preferences.PreferencesVocabPracticeType.Kanji)
        override val vocabDashboardSortByTime get() = mem("vocabDashboardSortByTime", false)
        override val vocabNoteTypeId get() = mem("vocabNoteTypeId", "")
        override val dailyResetTime get() = mem("dailyResetTime", kotlinx.datetime.LocalTime(0, 0))
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

    // ------------------------------------------------------------------
    // Layout operations
    // ------------------------------------------------------------------

    @Test
    fun defaultLayoutShowsAllCards() {
        val layout = CollectionCardLayout()
        assertEquals(CollectionCardType.entries.size, layout.visibleCards().size)
    }

    @Test
    fun hiddenCardIsNotVisible() {
        val layout = CollectionCardLayout().setVisible(CollectionCardType.FrequencyDistribution, false)
        assertFalse(layout.isVisible(CollectionCardType.FrequencyDistribution))
        assertEquals(CollectionCardType.entries.size - 1, layout.visibleCards().size)
    }

    @Test
    fun moveUpSwapsWithPrevious() {
        val layout = CollectionCardLayout().moveUp(CollectionCardType.KanjiGrid)
        val cards = layout.visibleCards()
        assertEquals(CollectionCardType.KanjiGrid, cards.first())
    }

    @Test
    fun moveDownSwapsWithNext() {
        val layout = CollectionCardLayout().moveDown(CollectionCardType.Hero)
        val cards = layout.visibleCards()
        assertEquals(CollectionCardType.KanjiGrid, cards.first())
        assertEquals(CollectionCardType.Hero, cards[1])
    }

    @Test
    fun moveUpAtTopIsNoop() {
        val original = CollectionCardLayout()
        val moved = original.moveUp(CollectionCardType.Hero)
        assertEquals(original.order, moved.order)
    }

    @Test
    fun moveDownAtBottomIsNoop() {
        val original = CollectionCardLayout()
        val moved = original.moveDown(CollectionCardType.Statistics)
        assertEquals(original.order, moved.order)
    }

    @Test
    fun sanitizationRemovesStaleIds() {
        val layout = CollectionCardLayout(
            order = CollectionCardType.entries.map { it.id } + "stale_id",
            hidden = setOf("stale_hidden")
        )
        val sanitized = layout.sanitized()
        assertEquals(CollectionCardType.entries.size, sanitized.order.size)
        assertTrue(sanitized.hidden.isEmpty())
    }

    // ------------------------------------------------------------------
    // Presets
    // ------------------------------------------------------------------

    @Test
    fun minimalPresetHidesAdvancedCards() {
        val preset = CollectionCardPresets.Minimal
        assertFalse(preset.isVisible(CollectionCardType.FrequencyDistribution))
        assertFalse(preset.isVisible(CollectionCardType.JLPTBreakdown))
        assertFalse(preset.isVisible(CollectionCardType.StudyState))
        assertTrue(preset.isVisible(CollectionCardType.Hero))
        assertTrue(preset.isVisible(CollectionCardType.KanjiGrid))
    }

    @Test
    fun advancedPresetShowsAllCards() {
        val preset = CollectionCardPresets.Advanced
        CollectionCardType.entries.forEach { type ->
            assertTrue("$type should be visible in Advanced preset", preset.isVisible(type))
        }
    }

    @Test
    fun allPresetsHaveUniqueIds() {
        val ids = CollectionCardPresets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    // ------------------------------------------------------------------
    // Layout store
    // ------------------------------------------------------------------

    @Test
    fun storeReturnsDefaultWhenBlank() = runBlocking {
        val prefs = FakePreferences()
        val store = CollectionCardLayoutStore(prefs)
        val layout = store.load()
        assertEquals(CollectionCardLayout(), layout)
    }

    @Test
    fun storeRoundTrips() = runBlocking {
        val prefs = FakePreferences()
        val store = CollectionCardLayoutStore(prefs)
        val modified = CollectionCardLayout()
            .setVisible(CollectionCardType.FrequencyDistribution, false)
            .moveUp(CollectionCardType.KanjiList)
        store.save(modified)
        val loaded = store.load()
        assertEquals(modified, loaded)
    }

    @Test
    fun storeResetClearsJson() = runBlocking {
        val prefs = FakePreferences()
        val store = CollectionCardLayoutStore(prefs)
        store.save(CollectionCardLayout(hidden = setOf("hero")))
        store.reset()
        val loaded = store.load()
        assertEquals(CollectionCardLayout(), loaded)
    }

    @Test
    fun storeHandlesCorruptJsonGracefully() = runBlocking {
        val prefs = FakePreferences()
        prefs.collectionCardLayoutJson.set("NOT VALID JSON {{{")
        val store = CollectionCardLayoutStore(prefs)
        val loaded = store.load()
        assertEquals(CollectionCardLayout(), loaded)
    }

    // ------------------------------------------------------------------
    // By ID lookup
    // ------------------------------------------------------------------

    @Test
    fun byIdReturnsCorrectType() {
        CollectionCardType.entries.forEach { type ->
            assertEquals(type, CollectionCardType.byId(type.id))
        }
    }

    @Test
    fun byIdReturnsNullForUnknown() {
        assertEquals(null, CollectionCardType.byId("nonexistent"))
    }

    // ------------------------------------------------------------------
    // Preset by ID
    // ------------------------------------------------------------------

    @Test
    fun presetByIdReturnsCorrectPreset() {
        CollectionCardPresets.all.forEach { preset ->
            assertEquals(preset, CollectionCardPresets.byId(preset.id))
        }
    }

    @Test
    fun presetByIdReturnsNullForUnknown() {
        assertEquals(null, CollectionCardPresets.byId("nonexistent"))
    }
}

private class FakeSuspendedProperty<T>(
    override val key: String,
    private var value: T
) : SuspendedProperty<T> {
    override suspend fun get(): T = value
    override suspend fun set(value: T) { this.value = value }
}
