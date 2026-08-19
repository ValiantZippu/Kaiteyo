package ua.syt0r.kanji.core.knowledge.cards

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.suspended_property.SuspendedProperty
import kotlin.test.Test

class GrammarCardModelsTest {

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
        val layout = GrammarCardLayout()
        assertEquals(GrammarCardType.entries.size, layout.visibleCards().size)
    }

    @Test
    fun hiddenCardIsNotVisible() {
        val layout = GrammarCardLayout().setVisible(GrammarCardType.Structure, false)
        assertFalse(layout.isVisible(GrammarCardType.Structure))
        assertEquals(GrammarCardType.entries.size - 1, layout.visibleCards().size)
    }

    @Test
    fun moveUpSwapsWithPrevious() {
        val layout = GrammarCardLayout().moveUp(GrammarCardType.Meaning)
        val cards = layout.visibleCards()
        assertEquals(GrammarCardType.Meaning, cards.first())
    }

    @Test
    fun moveDownSwapsWithNext() {
        val layout = GrammarCardLayout().moveDown(GrammarCardType.Hero)
        val cards = layout.visibleCards()
        assertEquals(GrammarCardType.Meaning, cards.first())
        assertEquals(GrammarCardType.Hero, cards[1])
    }

    @Test
    fun moveUpAtTopIsNoop() {
        val original = GrammarCardLayout()
        val moved = original.moveUp(GrammarCardType.Hero)
        assertEquals(original.order, moved.order)
    }

    @Test
    fun moveDownAtBottomIsNoop() {
        val original = GrammarCardLayout()
        val moved = original.moveDown(GrammarCardType.Study)
        assertEquals(original.order, moved.order)
    }

    @Test
    fun sanitizationRemovesStaleIds() {
        val layout = GrammarCardLayout(
            order = GrammarCardType.entries.map { it.id } + "stale_id",
            hidden = setOf("stale_hidden")
        )
        val sanitized = layout.sanitized()
        assertEquals(GrammarCardType.entries.size, sanitized.order.size)
        assertTrue(sanitized.hidden.isEmpty())
    }

    // ------------------------------------------------------------------
    // Presets
    // ------------------------------------------------------------------

    @Test
    fun minimalPresetHidesAdvancedCards() {
        val preset = GrammarCardPresets.Minimal
        assertFalse(preset.isVisible(GrammarCardType.Structure))
        assertFalse(preset.isVisible(GrammarCardType.JLPT))
        assertFalse(preset.isVisible(GrammarCardType.RelatedGrammar))
        assertTrue(preset.isVisible(GrammarCardType.Hero))
        assertTrue(preset.isVisible(GrammarCardType.Meaning))
    }

    @Test
    fun advancedPresetShowsAllCards() {
        val preset = GrammarCardPresets.Advanced
        GrammarCardType.entries.forEach { type ->
            assertTrue("$type should be visible in Advanced preset", preset.isVisible(type))
        }
    }

    @Test
    fun allPresetsHaveUniqueIds() {
        val ids = GrammarCardPresets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun newPresetsAreValidAndDistinct() {
        val ids = GrammarCardPresets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "preset ids must be unique")
        listOf("intermediate", "writing", "reading", "dictionary").forEach { id ->
            val preset = GrammarCardPresets.byId(id)
            assertTrue(preset != null, "preset $id must exist")
            preset!!.layout.hidden.forEach { hiddenId ->
                assertTrue("$hiddenId must be a real card id", GrammarCardType.byId(hiddenId) != null)
            }
            assertTrue("preset $id must keep cards visible", preset.layout.visibleCards().isNotEmpty())
        }
        // Reading keeps kanji-in-examples; Dictionary keeps every data card.
        assertTrue(GrammarCardPresets.byId("reading")!!.layout.isVisible(GrammarCardType.Kanji))
        assertTrue(GrammarCardPresets.byId("dictionary")!!.layout.isVisible(GrammarCardType.RelatedGrammar))
    }

    // ------------------------------------------------------------------
    // Layout store
    // ------------------------------------------------------------------

    @Test
    fun storeReturnsDefaultWhenBlank() = runBlocking {
        val prefs = FakePreferences()
        val store = GrammarCardLayoutStore(prefs)
        val layout = store.load()
        assertEquals(GrammarCardLayout(), layout)
    }

    @Test
    fun storeRoundTrips() = runBlocking {
        val prefs = FakePreferences()
        val store = GrammarCardLayoutStore(prefs)
        val modified = GrammarCardLayout()
            .setVisible(GrammarCardType.Structure, false)
            .moveUp(GrammarCardType.JLPT)
        store.save(modified)
        val loaded = store.load()
        assertEquals(modified, loaded)
    }

    @Test
    fun storeResetClearsJson() = runBlocking {
        val prefs = FakePreferences()
        val store = GrammarCardLayoutStore(prefs)
        store.save(GrammarCardLayout(hidden = setOf("hero")))
        store.reset()
        val loaded = store.load()
        assertEquals(GrammarCardLayout(), loaded)
    }

    @Test
    fun storeHandlesCorruptJsonGracefully() = runBlocking {
        val prefs = FakePreferences()
        prefs.grammarCardLayoutJson.set("NOT VALID JSON {{{")
        val store = GrammarCardLayoutStore(prefs)
        val loaded = store.load()
        assertEquals(GrammarCardLayout(), loaded)
    }

    // ------------------------------------------------------------------
    // By ID lookup
    // ------------------------------------------------------------------

    @Test
    fun byIdReturnsCorrectType() {
        GrammarCardType.entries.forEach { type ->
            assertEquals(type, GrammarCardType.byId(type.id))
        }
    }

    @Test
    fun byIdReturnsNullForUnknown() {
        assertEquals(null, GrammarCardType.byId("nonexistent"))
    }

    // ------------------------------------------------------------------
    // Preset by ID
    // ------------------------------------------------------------------

    @Test
    fun presetByIdReturnsCorrectPreset() {
        GrammarCardPresets.all.forEach { preset ->
            assertEquals(preset, GrammarCardPresets.byId(preset.id))
        }
    }

    @Test
    fun presetByIdReturnsNullForUnknown() {
        assertEquals(null, GrammarCardPresets.byId("nonexistent"))
    }
}

private class FakeSuspendedProperty<T>(
    override val key: String,
    private var value: T
) : SuspendedProperty<T> {
    override suspend fun get(): T = value
    override suspend fun set(value: T) { this.value = value }
}
