package ua.syt0r.kanji.core.knowledge.media

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

/**
 * Media-reference store tests (spec §28): recordings are newest-first and
 * deduped by title+text+timestamp, matching finds substring hits, corrupt
 * blobs fall back to empty, and the list is capped so a hand-edited blob can
 * never grow unbounded.
 */
class MediaReferenceStoreTest {

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
        override val gameProgressJson get() = mem("gameProgressJson", "")
    }

    private fun ref(kind: MediaReferenceKind, title: String, text: String, time: Long, at: Long = time) =
        MediaReference(kind = kind, title = title, text = text, timestampMs = time, recordedAt = at)

    @Test
    fun startsEmptyAndBlankBlobsStayEmpty() = runBlocking {
        val store = MediaReferenceStore(FakeAppPreferences())
        assertTrue(store.load().references.isEmpty())
    }

    @Test
    fun recordIsNewestFirstAndDedupedByTitleTextTime() = runBlocking {
        val store = MediaReferenceStore(FakeAppPreferences())
        store.record(ref(MediaReferenceKind.Bookmark, "Show.mkv", "食べる", 5000))
        store.record(ref(MediaReferenceKind.Bookmark, "Show.mkv", "日本語", 10000))
        store.record(ref(MediaReferenceKind.Bookmark, "Show.mkv", "食べる", 5000)) // duplicate — dropped

        val references = store.load().references
        assertEquals(2, references.size)
        assertEquals("日本語", references[0].text)
        assertEquals("食べる", references[1].text)
    }

    @Test
    fun blankTextIsIgnored() = runBlocking {
        val store = MediaReferenceStore(FakeAppPreferences())
        store.record(ref(MediaReferenceKind.Bookmark, "Show.mkv", "   ", 1000))
        assertTrue(store.load().references.isEmpty())
    }

    @Test
    fun matchingFindsSubstringHits() = runBlocking {
        val store = MediaReferenceStore(FakeAppPreferences())
        store.record(ref(MediaReferenceKind.Bookmark, "Show.mkv", "日本語を勉強する", 1000))
        store.record(ref(MediaReferenceKind.Bookmark, "Film.mkv", "勉強する時間", 2000))
        store.record(ref(MediaReferenceKind.Bookmark, "Other.mkv", "漢字", 3000))

        val hits = store.matching("勉強")
        assertEquals(2, hits.size)
    }

    @Test
    fun referencesAreCapped() = runBlocking {
        val store = MediaReferenceStore(FakeAppPreferences())
        repeat(MediaReferenceHistory.MAX_REFERENCES + 10) { i ->
            store.record(ref(MediaReferenceKind.Bookmark, "Show.mkv", "単語$i", i.toLong()))
        }
        assertEquals(MediaReferenceHistory.MAX_REFERENCES, store.load().references.size)
    }

    @Test
    fun corruptBlobFallsBackToEmpty() = runBlocking {
        val prefs = FakeAppPreferences()
        prefs.mediaReferencesJson.set("{ not valid json")
        assertTrue(MediaReferenceStore(prefs).load().references.isEmpty())
    }

    @Test
    fun resetClearsEverything() = runBlocking {
        val store = MediaReferenceStore(FakeAppPreferences())
        store.record(ref(MediaReferenceKind.Bookmark, "Show.mkv", "食べる", 1000))
        store.reset()
        assertTrue(store.load().references.isEmpty())
    }
}
