package ua.syt0r.kanji.core.knowledge

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

/**
 * Tests for the level-profile system: catalog defaults per profile,
 * effective presentation (custom overrides honored only for Custom),
 * and the persisted store (round-trip, sanitize-on-load, reset).
 */
class LevelProfileTest {

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
    // Catalog defaults
    // ---------------------------------------------------------------

    @Test
    fun absoluteBeginnerShowsRomajiAndSimpleExplanations() {
        val presentation = LearnerProfileCatalog.defaultsFor(LearnerProfile.AbsoluteBeginner)
        assertTrue(presentation.showRomaji)
        assertTrue(presentation.showFurigana)
        assertTrue(presentation.showTranslations)
        assertEquals(ExplanationDepth.Simple, presentation.explanationDepth)
        assertEquals(SentenceDifficulty.Easy, presentation.sentenceDifficulty)
        assertEquals(GraphComplexity.Simple, presentation.graphComplexity)
        assertEquals("beginner", presentation.cardPresetId)
    }

    @Test
    fun advancedRevealsRareReadingsAndFullGraph() {
        val presentation = LearnerProfileCatalog.defaultsFor(LearnerProfile.Advanced)
        assertTrue(presentation.showRareReadings)
        assertEquals(ExplanationDepth.Technical, presentation.explanationDepth)
        assertEquals(SentenceDifficulty.Hard, presentation.sentenceDifficulty)
        assertEquals(GraphComplexity.Full, presentation.graphComplexity)
        assertEquals("advanced", presentation.cardPresetId)
    }

    @Test
    fun nativeIsJapaneseFirst() {
        val presentation = LearnerProfileCatalog.defaultsFor(LearnerProfile.Native)
        assertEquals(false, presentation.showTranslations)
        assertEquals(ExplanationDepth.JapaneseOnly, presentation.explanationDepth)
    }

    @Test
    fun researchShowsEverything() {
        val presentation = LearnerProfileCatalog.defaultsFor(LearnerProfile.Research)
        assertTrue(presentation.showRareReadings)
        assertTrue(presentation.showEtymology)
        assertTrue(presentation.showTranslations)
        assertEquals(GraphComplexity.Full, presentation.graphComplexity)
        assertEquals("research", presentation.cardPresetId)
    }

    @Test
    fun everyProfileHasACatalogPreset() {
        for (profile in LearnerProfile.entries) {
            assertTrue(
                LearnerProfileCatalog.presets.any { it.id == profile.id },
                "missing preset for ${profile.id}"
            )
            assertEquals(profile, LearnerProfileCatalog.byId(profile.id))
        }
    }

    // ---------------------------------------------------------------
    // Effective presentation
    // ---------------------------------------------------------------

    @Test
    fun customHonorsOverrides() {
        val overrides = ProfilePresentation(
            showRomaji = true,
            showFurigana = false,
            explanationDepth = ExplanationDepth.Technical
        )
        val preference = LearnerProfilePreference(profile = LearnerProfile.Custom, customPresentation = overrides)
        val effective = preference.effectivePresentation()
        assertEquals(overrides, effective)
    }

    @Test
    fun customWithoutOverridesFallsBackToCatalogDefaults() {
        val preference = LearnerProfilePreference(profile = LearnerProfile.Custom)
        assertEquals(
            LearnerProfileCatalog.defaultsFor(LearnerProfile.Custom),
            preference.effectivePresentation()
        )
    }

    @Test
    fun nonCustomIgnoresStoredOverrides() {
        val preference = LearnerProfilePreference(
            profile = LearnerProfile.Beginner,
            customPresentation = ProfilePresentation(showRomaji = false)
        )
        assertEquals(
            LearnerProfileCatalog.defaultsFor(LearnerProfile.Beginner),
            preference.effectivePresentation()
        )
    }

    @Test
    fun switchingAwayFromCustomDropsOverridesButKeepsThemOnReturn() {
        val custom = LearnerProfilePreference().withCustomPresentation(ProfilePresentation(showRomaji = true))
        val beginner = custom.withProfile(LearnerProfile.Beginner)
        assertNull(beginner.customPresentation)
        // The custom config is re-applied when switching back.
        val backToCustom = beginner.copy(profile = LearnerProfile.Custom, customPresentation = custom.customPresentation)
        assertEquals(custom.effectivePresentation(), backToCustom.effectivePresentation())
    }

    // ---------------------------------------------------------------
    // Store persistence
    // ---------------------------------------------------------------

    @Test
    fun storeRoundTripsProfileAndOverrides() = runBlocking {
        val prefs = FakeAppPreferences()
        val store = LearnerProfileStore(prefs)
        val preference = LearnerProfilePreference(profile = LearnerProfile.Advanced)

        store.save(preference)
        val loaded = store.load()
        assertEquals(LearnerProfile.Advanced, loaded.profile)
        assertEquals(
            LearnerProfileCatalog.defaultsFor(LearnerProfile.Advanced),
            loaded.effectivePresentation()
        )
    }

    @Test
    fun storeDefaultsOnBlankAndCorruptBlob() = runBlocking {
        val prefs = FakeAppPreferences()
        val store = LearnerProfileStore(prefs)

        assertEquals(LearnerProfile.Intermediate, store.load().profile)

        prefs.learnerProfileJson.set("{ not valid json")
        assertEquals(LearnerProfile.Intermediate, store.load().profile)

        prefs.learnerProfileJson.set("{\"profile\":\"DoesNotExist\"}")
        assertEquals(LearnerProfile.Intermediate, store.load().profile)
    }

    @Test
    fun saveProfilePreservesCustomOverridesWhenReturningToCustom() = runBlocking {
        val prefs = FakeAppPreferences()
        val store = LearnerProfileStore(prefs)

        store.save(LearnerProfilePreference(profile = LearnerProfile.Custom).withCustomPresentation(ProfilePresentation(showRomaji = true)))
        store.saveProfile(LearnerProfile.Beginner)
        assertEquals(LearnerProfile.Beginner, store.load().profile)
        assertNull(store.load().customPresentation)

        store.save(LearnerProfilePreference(profile = LearnerProfile.Custom).withCustomPresentation(ProfilePresentation(showRomaji = true)))
        assertEquals(ProfilePresentation(showRomaji = true), store.load().effectivePresentation())
    }

    @Test
    fun resetClearsTheStoredProfile() = runBlocking {
        val prefs = FakeAppPreferences()
        val store = LearnerProfileStore(prefs)
        store.save(LearnerProfilePreference(profile = LearnerProfile.Research))
        assertEquals(LearnerProfile.Research, store.load().profile)

        store.reset()
        assertEquals(LearnerProfile.Intermediate, store.load().profile)
    }
}
