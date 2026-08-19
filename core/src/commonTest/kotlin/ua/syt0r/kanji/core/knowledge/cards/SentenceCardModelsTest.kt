package ua.syt0r.kanji.core.knowledge.cards

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.suspended_property.SuspendedProperty
import kotlin.test.Test

class SentenceCardModelsTest {

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
        override val reminderTime get() = mem("reminderTime", kotlin.time.Duration.parse("PT9H").inWholeMilliseconds.let { kotlinx.datetime.LocalTime(9, 0) })
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
        val layout = SentenceCardLayout()
        assertEquals(SentenceCardType.entries.size, layout.visibleCards().size)
    }

    @Test
    fun hiddenCardIsNotVisible() {
        val layout = SentenceCardLayout().setVisible(SentenceCardType.Tokens, false)
        assertFalse(layout.isVisible(SentenceCardType.Tokens))
        assertEquals(SentenceCardType.entries.size - 1, layout.visibleCards().size)
    }

    @Test
    fun moveUpSwapsWithPrevious() {
        val layout = SentenceCardLayout().moveUp(SentenceCardType.Translation)
        val cards = layout.visibleCards()
        // Translation was at index 1, should now be at index 0
        assertEquals(SentenceCardType.Translation, cards.first())
    }

    @Test
    fun moveDownSwapsWithNext() {
        val layout = SentenceCardLayout().moveDown(SentenceCardType.Hero)
        val cards = layout.visibleCards()
        // Hero was at index 0, should now be at index 1
        assertEquals(SentenceCardType.Translation, cards.first())
        assertEquals(SentenceCardType.Hero, cards[1])
    }

    @Test
    fun moveUpAtTopIsNoop() {
        val original = SentenceCardLayout()
        val moved = original.moveUp(SentenceCardType.Hero)
        assertEquals(original.order, moved.order)
    }

    @Test
    fun moveDownAtBottomIsNoop() {
        val original = SentenceCardLayout()
        val moved = original.moveDown(SentenceCardType.Study)
        assertEquals(original.order, moved.order)
    }

    @Test
    fun sanitizationRemovesStaleIds() {
        val layout = SentenceCardLayout(
            order = SentenceCardType.entries.map { it.id } + "stale_id",
            hidden = setOf("stale_hidden")
        )
        val sanitized = layout.sanitized()
        assertEquals(SentenceCardType.entries.size, sanitized.order.size)
        assertTrue(sanitized.hidden.isEmpty())
    }

    // ------------------------------------------------------------------
    // Presets
    // ------------------------------------------------------------------

    @Test
    fun minimalPresetHidesAdvancedCards() {
        val preset = SentenceCardPresets.Minimal
        assertFalse(preset.isVisible(SentenceCardType.Tokens))
        assertFalse(preset.isVisible(SentenceCardType.Grammar))
        assertFalse(preset.isVisible(SentenceCardType.Difficulty))
        assertTrue(preset.isVisible(SentenceCardType.Hero))
        assertTrue(preset.isVisible(SentenceCardType.Translation))
    }

    @Test
    fun advancedPresetShowsAllCards() {
        val preset = SentenceCardPresets.Advanced
        SentenceCardType.entries.forEach { type ->
            assertTrue("$type should be visible in Advanced preset", preset.isVisible(type))
        }
    }

    @Test
    fun allPresetsHaveUniqueIds() {
        val ids = SentenceCardPresets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun newPresetsAreValidAndDistinct() {
        val ids = SentenceCardPresets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "preset ids must be unique")
        listOf("intermediate", "writing", "reading", "dictionary").forEach { id ->
            val preset = SentenceCardPresets.byId(id)
            assertTrue(preset != null, "preset $id must exist")
            preset!!.layout.hidden.forEach { hiddenId ->
                assertTrue("$hiddenId must be a real card id", SentenceCardType.byId(hiddenId) != null)
            }
            assertTrue("preset $id must keep cards visible", preset.layout.visibleCards().isNotEmpty())
        }
        // Writing keeps the surface; Dictionary keeps every analysis card.
        assertTrue(SentenceCardPresets.byId("writing")!!.layout.isVisible(SentenceCardType.Translation))
        assertTrue(SentenceCardPresets.byId("dictionary")!!.layout.isVisible(SentenceCardType.Source))
    }

    // ------------------------------------------------------------------
    // Layout store
    // ------------------------------------------------------------------

    @Test
    fun storeReturnsDefaultWhenBlank() = runBlocking {
        val prefs = FakePreferences()
        val store = SentenceCardLayoutStore(prefs)
        val layout = store.load()
        assertEquals(SentenceCardLayout(), layout)
    }

    @Test
    fun storeRoundTrips() = runBlocking {
        val prefs = FakePreferences()
        val store = SentenceCardLayoutStore(prefs)
        val modified = SentenceCardLayout()
            .setVisible(SentenceCardType.Tokens, false)
            .moveUp(SentenceCardType.Difficulty)
        store.save(modified)
        val loaded = store.load()
        assertEquals(modified, loaded)
    }

    @Test
    fun storeResetClearsJson() = runBlocking {
        val prefs = FakePreferences()
        val store = SentenceCardLayoutStore(prefs)
        store.save(SentenceCardLayout(hidden = setOf("hero")))
        store.reset()
        val loaded = store.load()
        assertEquals(SentenceCardLayout(), loaded)
    }

    @Test
    fun storeHandlesCorruptJsonGracefully() = runBlocking {
        val prefs = FakePreferences()
        prefs.sentenceCardLayoutJson.set("NOT VALID JSON {{{")
        val store = SentenceCardLayoutStore(prefs)
        val loaded = store.load()
        assertEquals(SentenceCardLayout(), loaded)
    }

    // ------------------------------------------------------------------
    // By ID lookup
    // ------------------------------------------------------------------

    @Test
    fun byIdReturnsCorrectType() {
        SentenceCardType.entries.forEach { type ->
            assertEquals(type, SentenceCardType.byId(type.id))
        }
    }

    @Test
    fun byIdReturnsNullForUnknown() {
        assertEquals(null, SentenceCardType.byId("nonexistent"))
    }

    // ------------------------------------------------------------------
    // Preset by ID
    // ------------------------------------------------------------------

    @Test
    fun presetByIdReturnsCorrectPreset() {
        SentenceCardPresets.all.forEach { preset ->
            assertEquals(preset, SentenceCardPresets.byId(preset.id))
        }
    }

    @Test
    fun presetByIdReturnsNullForUnknown() {
        assertEquals(null, SentenceCardPresets.byId("nonexistent"))
    }
}

private class FakeSuspendedProperty<T>(
    override val key: String,
    private var value: T
) : SuspendedProperty<T> {
    override suspend fun get(): T = value
    override suspend fun set(value: T) { this.value = value }
}
