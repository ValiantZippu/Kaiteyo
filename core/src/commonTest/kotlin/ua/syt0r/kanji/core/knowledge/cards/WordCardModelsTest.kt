package ua.syt0r.kanji.core.knowledge.cards

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.get
import kotlinx.serialization.json.JsonPrimitive
import ua.syt0r.kanji.core.suspended_property.SuspendedProperty
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WordCardModelsTest {

    private class FakePreferences : PreferencesContract.AppPreferences {
        private val values = mutableMapOf<String, Any?>()
        private fun <T> mem(key: String, default: T): SuspendedProperty<T> =
            object : SuspendedProperty<T> {
                override val key: String = key
                override val onModified: SharedFlow<T> = MutableStateFlow(default)
                @Suppress("UNCHECKED_CAST")
                override suspend fun get(): T = values[key] as? T ?: default
                override suspend fun set(value: T) { values[key] = value }
                override suspend fun isModified(): Boolean = values.containsKey(key)
                override suspend fun backup(): JsonPrimitive? = null
                override suspend fun restore(value: JsonPrimitive) = Unit
            }

        override val refreshToken get() = mem("refreshToken", null as String?)
        override val idToken get() = mem("idToken", null as String?)
        override val userInfo get() = mem("userInfo", null as PreferencesUserInfo?)
        override val subscriptionAlert get() = mem("subscriptionAlert", null as String?)
        override val localDataId get() = mem("localDataId", "")
        override val localDataTimestamp get() = mem("localDataTimestamp", null as kotlinx.datetime.Instant?)
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
        override val reminderTime get() = mem("reminderTime", kotlinx.datetime.LocalTime(21, 0))
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

    private val prefs = FakePreferences()
    private val store = WordCardLayoutStore(prefs)

    // ---------------------------------------------------------------
    // Layout operations
    // ---------------------------------------------------------------

    @Test
    fun defaultLayoutShowsAllCards() {
        val layout = WordCardLayout()
        assertEquals(WordCardType.entries.size, layout.visibleCards().size)
        assertTrue(WordCardType.entries.all { layout.isVisible(it) })
    }

    @Test
    fun hideCard() {
        val layout = WordCardLayout().setVisible(WordCardType.Frequency, visible = false)
        assertFalse(layout.isVisible(WordCardType.Frequency))
        assertEquals(WordCardType.entries.size - 1, layout.visibleCards().size)
    }

    @Test
    fun showCard() {
        val layout = WordCardLayout()
            .setVisible(WordCardType.Frequency, visible = false)
            .setVisible(WordCardType.Frequency, visible = true)
        assertTrue(layout.isVisible(WordCardType.Frequency))
    }

    @Test
    fun moveUp() {
        val original = WordCardType.entries.map { it.id }
        val layout = WordCardLayout(order = original)
        val moved = layout.moveUp(WordCardType.Meanings)
        assertEquals(original[1], moved.order[0])
        assertEquals(original[0], moved.order[1])
    }

    @Test
    fun moveUpAtTopIsNoOp() {
        val layout = WordCardLayout()
        assertEquals(layout, layout.moveUp(WordCardType.Hero))
    }

    @Test
    fun moveDown() {
        val original = WordCardType.entries.map { it.id }
        val layout = WordCardLayout(order = original)
        val moved = layout.moveDown(WordCardType.Hero)
        assertEquals(original[1], moved.order[0])
        assertEquals(original[0], moved.order[1])
    }

    @Test
    fun moveDownAtBottomIsNoOp() {
        val layout = WordCardLayout()
        val last = WordCardType.entries.last()
        assertEquals(layout, layout.moveDown(last))
    }

    // ---------------------------------------------------------------
    // Persistence round-trip
    // ---------------------------------------------------------------

    @Test
    fun saveAndLoad() = kotlinx.coroutines.runBlocking {
        val custom = WordCardLayout(
            order = listOf("hero", "sentences", "kanji", "readings", "meanings"),
            hidden = setOf("grammar", "study")
        )
        store.save(custom)
        val loaded = store.load()
        assertEquals(custom.order, loaded.order)
        assertEquals(custom.hidden, loaded.hidden)
    }

    @Test
    fun loadDefaultsOnEmpty() = kotlinx.coroutines.runBlocking {
        val loaded = store.load()
        assertEquals(WordCardLayout(), loaded)
    }

    @Test
    fun loadDefaultsOnCorruptJson() = kotlinx.coroutines.runBlocking {
        prefs.wordCardLayoutJson.set("{ this is not json}")
        val loaded = store.load()
        assertEquals(WordCardLayout(), loaded)
        // The corrupt value should be cleared.
        assertEquals("", prefs.wordCardLayoutJson.get())
    }

    @Test
    fun sanitizedDropsUnknownIds() {
        val layout = WordCardLayout(
            order = listOf("hero", "unknown-card", "readings"),
            hidden = setOf("unknown-card", "hero")
        )
        val sanitized = layout.sanitized()
        assertEquals(listOf("hero", "readings"), sanitized.order)
        assertEquals(setOf("hero"), sanitized.hidden)
    }

    @Test
    fun resetClearsPersistedValue() = kotlinx.coroutines.runBlocking {
        val custom = WordCardLayout(
            order = listOf("sentences", "hero"),
            hidden = setOf("meanings")
        )
        store.save(custom)
        assertEquals(custom, store.load())

        store.reset()
        assertEquals(WordCardLayout(), store.load())
        assertEquals("", prefs.wordCardLayoutJson.get())
    }

    @Test
    fun visibleCardsAppendsRegistryTypesMissingFromSavedOrder() {
        // A layout saved before a card type existed must not lose that card
        // — missing registry types are appended at render time (KT-CARD-001).
        val layout = WordCardLayout(
            order = listOf("hero"),
            hidden = setOf("study")
        )
        val visible = layout.visibleCards()
        assertTrue(visible.map { it.id }.containsAll(WordCardType.entries.map { it.id }))
        // Hidden state is respected even for appended cards.
        assertFalse(layout.isVisible(WordCardType.Study))
        assertTrue(layout.isVisible(WordCardType.Media))
    }

    // ---------------------------------------------------------------
    // Presets
    // ---------------------------------------------------------------

    @Test
    fun minimalPresetHidesAdvancedCards() {
        val preset = WordCardPresets.Minimal
        assertFalse(preset.isVisible(WordCardType.Frequency))
        assertFalse(preset.isVisible(WordCardType.Grammar))
        assertTrue(preset.isVisible(WordCardType.Hero))
        assertTrue(preset.isVisible(WordCardType.Readings))
    }

    @Test
    fun researchPresetShowsAll() {
        val preset = WordCardPresets.Research
        assertTrue(preset.hidden.isEmpty())
        assertEquals(WordCardType.entries.size, preset.visibleCards().size)
    }

    @Test
    fun presetByIdLookup() {
        assertEquals("standard", WordCardPresets.byId("standard")?.id)
        assertEquals(null, WordCardPresets.byId("nonexistent"))
    }

    @Test
    fun newPresetsAreValidAndDistinct() {
        val ids = WordCardPresets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "preset ids must be unique")
        listOf("intermediate", "writing", "reading", "dictionary").forEach { id ->
            val preset = WordCardPresets.byId(id)
            assertTrue(preset != null, "preset $id must exist")
            preset!!.layout.hidden.forEach { hiddenId ->
                assertTrue("$hiddenId must be a real card id", WordCardType.byId(hiddenId) != null)
            }
            assertTrue("preset $id must keep cards visible", preset.layout.visibleCards().isNotEmpty())
        }
        // Writing keeps the spelling-focused cards; Dictionary keeps lexical data.
        assertTrue(WordCardPresets.byId("writing")!!.layout.isVisible(WordCardType.Kanji))
        assertFalse(WordCardPresets.byId("dictionary")!!.layout.isVisible(WordCardType.Media))
    }
}
