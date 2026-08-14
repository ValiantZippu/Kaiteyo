package ua.syt0r.kanji.desktop.engine.media

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaStatisticsStoreTest {

    private lateinit var dir: File

    @BeforeTest
    fun setup() {
        dir = Files.createTempDirectory("kaiteyo-stats-test").toFile()
    }

    @AfterTest
    fun teardown() {
        dir.deleteRecursively()
    }

    @Test
    fun recordsTotalsAndTodayBucket() {
        val store = MediaStatisticsStore(dir)
        store.recordWatch(120_000, study = false)
        store.recordWatch(60_000, study = true)
        store.recordLookup()
        store.recordLookup()
        store.recordMined()
        store.recordSession()

        assertEquals(180_000L, store.totalWatchMs)
        assertEquals(60_000L, store.totalStudyMs)
        assertEquals(2, store.totalLookups)
        assertEquals(1, store.totalMined)
        assertEquals(1, store.totalSessions)
        assertEquals(1, store.days.size)

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        assertEquals(180_000L, store.watchMsBetween(today, today))
        assertEquals(60_000L, store.studyMsBetween(today, today))
        assertEquals(2, store.lookupsBetween(today, today))
        assertEquals(1, store.minedBetween(today, today))
        assertTrue(store.activeDays(7) >= 1)
        assertTrue(store.mediaHours > 0f)
        assertTrue(store.summary().mediaHours > 0f)
        assertTrue(store.summary().activeLast7Days >= 1)
    }

    @Test
    fun zeroActivityIsSafe() {
        val store = MediaStatisticsStore(dir)
        assertEquals(0L, store.totalWatchMs)
        assertEquals(0, store.totalLookups)
        assertEquals(0f, store.mediaHours)
        assertTrue(store.days.isEmpty())
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        assertEquals(0L, store.watchMsBetween(today, today))
    }

    @Test
    fun ignoresNonPositiveWatch() {
        val store = MediaStatisticsStore(dir)
        store.recordWatch(0, study = false)
        store.recordWatch(-5, study = false)
        assertEquals(0L, store.totalWatchMs)
    }

    @Test
    fun persistsAcrossInstances() {
        MediaStatisticsStore(dir).run {
            recordWatch(90_000, study = false)
            recordLookup()
            recordMined()
        }
        val reloaded = MediaStatisticsStore(dir)
        assertEquals(90_000L, reloaded.totalWatchMs)
        assertEquals(1, reloaded.totalLookups)
        assertEquals(1, reloaded.totalMined)
        assertEquals(1, reloaded.days.size)
    }

    @Test
    fun multipleDaysKeepSeparateBuckets() {
        val store = MediaStatisticsStore(dir)
        store.recordWatch(10_000, study = false)
        // Backdate a second bucket via the internal seam (clock stays untouched).
        store.recordWatchOn("2000-01-01", 5_000)
        assertEquals(2, store.days.size)
        assertEquals(5_000L, store.day(kotlinx.datetime.LocalDate(2000, 1, 1)).watchMs)
        assertEquals(15_000L, store.totalWatchMs)
    }
}
