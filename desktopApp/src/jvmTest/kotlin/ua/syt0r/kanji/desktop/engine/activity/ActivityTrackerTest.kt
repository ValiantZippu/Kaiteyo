package ua.syt0r.kanji.desktop.engine.activity

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import ua.syt0r.kanji.desktop.engine.settings.SettingsEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies the engagement model behind activity-based study time:
 * signals open/extend intervals, lapsed intervals close (AFK), the next
 * signal resumes, and engaged time is a pure overlap computation — never
 * "application open" time.
 */
class ActivityTrackerTest {

    private val base = Instant.parse("2026-08-15T12:00:00Z")

    private fun tracker(
        custom: Boolean = false,
        timeoutMinutes: Int = 5
    ): ActivityTracker {
        val settings = SettingsEngine()
        if (custom) {
            settings.set("activity.afk-mode", "custom")
            settings.set("activity.afk-timeout-minutes", timeoutMinutes)
        }
        return ActivityTracker(
            settings = settings,
            persistFile = java.io.File.createTempFile("kaiteyo-activity-test", ".json")
        )
    }

    private fun after(minutes: Long): Instant = base.plus(minutes, DateTimeUnit.MINUTE)

    // ---------------------------------------------------------------
    // Engaged time = overlap of real intervals, not wall clock
    // ---------------------------------------------------------------

    @Test
    fun engagedSinceCountsOnlyActiveIntervals() {
        val t = tracker()
        // Smart mode, General signals → 2-minute timeout.
        t.recordSignal(SignalContext.General, base)          // interval [12:00, 12:02]
        t.recordSignal(SignalContext.General, after(1))       // extends to [12:00, 12:03]
        // No signal until 12:10 — the lapsed tail is included once (12:03),
        // nothing else counts.
        assertEquals(3.minutes, t.engagedSince(base, after(10)))
    }

    @Test
    fun afkPausesAndResumeStartsANewInterval() {
        val t = tracker()
        t.recordSignal(SignalContext.General, base)
        assertEquals(ActivityState.Active, t.snapshot(after(1)).state)
        // 12:03 > 12:02 expiry → away.
        assertEquals(ActivityState.Afk, t.snapshot(after(3)).state)
        assertTrue(t.isAfk(after(3)))

        // The user returns — a new engagement opens; the session is NOT over.
        t.recordSignal(SignalContext.General, after(10))
        assertEquals(ActivityState.Active, t.snapshot(after(10)).state)
        // Intervals: [12:00,12:02] + [12:10,12:12(now)] = 4 minutes.
        assertEquals(4.minutes, t.engagedSince(base, after(10)))
    }

    @Test
    fun engagedTimeNeverExceedsElapsedWallTime() {
        val t = tracker()
        t.recordSignal(SignalContext.General, base)
        t.recordSignal(SignalContext.General, after(1))
        val engaged = t.engagedSince(base, after(10))
        assertTrue(engaged <= 10.minutes)
        assertTrue(engaged > Duration.ZERO)
    }

    // ---------------------------------------------------------------
    // AFK timeouts: smart vs custom
    // ---------------------------------------------------------------

    @Test
    fun smartContextsUseDifferentTimeouts() {
        val t = tracker()
        // Study signals keep a 5-minute engagement; General only 2.
        assertEquals(5.minutes, t.timeoutFor(SignalContext.Study, base))
        assertEquals(2.minutes, t.timeoutFor(SignalContext.General, base))
        t.recordSignal(SignalContext.Study, base)
        assertEquals(ActivityState.Active, t.snapshot(after(4)).state)
        assertEquals(ActivityState.Afk, t.snapshot(after(6)).state)
    }

    @Test
    fun customModeUsesConfiguredTimeout() {
        val t = tracker(custom = true, timeoutMinutes = 1)
        assertEquals(1.minutes, t.timeoutFor(SignalContext.Study, base))
        t.recordSignal(SignalContext.Study, base)
        assertEquals(ActivityState.Active, t.snapshot(after(0)).state)
        assertEquals(ActivityState.Afk, t.snapshot(after(2)).state)
    }

    // ---------------------------------------------------------------
    // Disabled tracking falls back to wall time (existing behavior)
    // ---------------------------------------------------------------

    @Test
    fun disabledTrackingReportsWallTime() {
        val settings = SettingsEngine()
        settings.set("activity.tracking", false)
        val t = ActivityTracker(
            settings = settings,
            persistFile = java.io.File.createTempFile("kaiteyo-activity-test", ".json")
        )
        assertFalse(t.enabled)
        assertEquals(10.minutes, t.engagedSince(base, after(10)))
        assertEquals(ActivityState.Idle, t.snapshot(after(5)).state)
        assertFalse(t.isAfk(after(5)))
    }

    // ---------------------------------------------------------------
    // Per-day buckets
    // ---------------------------------------------------------------

    @Test
    fun engagedForDaySumsOnlyThatDay() {
        val t = tracker()
        t.recordSignal(SignalContext.General, base)   // 2026-08-15
        t.recordSignal(SignalContext.General, after(1))
        // A second engagement on the next day.
        val nextDay = base.plus(1, DateTimeUnit.DAY)
        t.recordSignal(SignalContext.General, nextDay)

        val day1 = kotlinx.datetime.LocalDate(2026, kotlinx.datetime.Month.AUGUST, 15)
        val day2 = kotlinx.datetime.LocalDate(2026, kotlinx.datetime.Month.AUGUST, 16)
        assertEquals(3.minutes, t.engagedForDay(day1, after(2)))
        assertTrue(t.engagedForDay(day2, nextDay) >= 2.minutes)
        assertTrue(t.engagedForDay(day2, nextDay) <= 2.minutes + 2.seconds)
    }

    @Test
    fun resetClearsState() {
        val t = tracker()
        t.recordSignal(SignalContext.General, base)
        t.resetForTesting()
        assertEquals(ActivityState.Idle, t.snapshot(after(1)).state)
        assertEquals(Duration.ZERO, t.engagedSince(base, after(1)))
    }

    // ---------------------------------------------------------------
    // Per-day buckets (sessions + engaged time) for the stats overview
    // ---------------------------------------------------------------

    @Test
    fun dayActivitiesBucketsSessionsAndEngagedByLocalDay() {
        val t = tracker()
        // Build times from LOCAL dates so the assertions hold in any timezone.
        val tz = TimeZone.currentSystemDefault()
        val day1 = LocalDate(2026, Month.AUGUST, 15)
        val day2 = LocalDate(2026, Month.AUGUST, 16)
        val d1Noon = day1.atStartOfDayIn(tz) + 12.hours
        val d2Noon = day2.atStartOfDayIn(tz) + 12.hours

        // Day 1: two engagements — [12:00, 12:03] then [12:30, 12:32].
        t.recordSignal(SignalContext.General, d1Noon)
        t.recordSignal(SignalContext.General, d1Noon + 1.minutes)
        t.recordSignal(SignalContext.General, d1Noon + 30.minutes)
        // Day 2: one engagement.
        t.recordSignal(SignalContext.General, d2Noon)

        val rows = t.dayActivities(day2, 2, d2Noon)
        assertEquals(2, rows.size)
        assertEquals(day1, rows[0].date)
        assertEquals(day2, rows[1].date)
        assertEquals(5.minutes, rows[0].engaged)
        assertEquals(2, rows[0].sessions)
        assertEquals(2.minutes, rows[1].engaged)
        assertEquals(1, rows[1].sessions)
    }

    @Test
    fun dayActivitiesAreEmptyForDaysOutsideTheWindow() {
        val t = tracker()
        val tz = TimeZone.currentSystemDefault()
        val day1 = LocalDate(2026, Month.AUGUST, 15)
        val day2 = LocalDate(2026, Month.AUGUST, 16)
        val d1Noon = day1.atStartOfDayIn(tz) + 12.hours
        t.recordSignal(SignalContext.General, d1Noon)

        // Requesting a window that starts AFTER the engagement yields zeros.
        val rows = t.dayActivities(day2, 2, day2.atStartOfDayIn(tz) + 12.hours)
        assertTrue(rows.all { it.engaged == Duration.ZERO && it.sessions == 0 })
    }
}
