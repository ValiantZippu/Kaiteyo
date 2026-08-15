package ua.syt0r.kanji.desktop.engine.activity

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.engine.settings.SettingsEngine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

// ============================================
// ACTIVITY TRACKER — real study-time accounting
//
// Study time must never equal "application open
// time". This engine keeps an engagement model:
//
//   signal → open interval → inactivity exceeds
//   the timeout → interval closes → AFK → next
//   signal reopens a new interval.
//
// Every interval carries real timestamps, so
// "how long were you actually studying between
// T0 and T1" is a pure overlap computation, and
// the same intervals feed the AFK rain overlay.
//
// The engine is stateless between reads: no
// threads, no timers. Callers signal on real
// interactions (clicks, keys, grading, writing
// strokes) and read the derived state whenever
// they need it. Persistence happens when an
// interval closes and on shutdown.
// ============================================

/** What kind of interaction produced a signal — drives the smart timeout. */
enum class SignalContext { General, Study, Writing, Media }

/** Whether the user is currently engaged, idle, or away. */
enum class ActivityState { Active, Idle, Afk }

/** A real engagement window. [end] is the expiry (last signal + timeout). */
@Serializable
data class ActivityInterval(
    val startIso: String,
    val endIso: String
) {
    val start: Instant get() = Instant.parse(startIso)
    val end: Instant get() = Instant.parse(endIso)
}

/** Point-in-time read of the tracker — cheap, call as often as needed. */
data class ActivitySnapshot(
    val state: ActivityState,
    val lastSignalAt: Instant?,
    val afkSince: Instant?
)

/** Engaged time + engagement count for one calendar day. */
data class DayActivity(
    val date: LocalDate,
    val engaged: Duration,
    val sessions: Int
)

@Serializable
private data class ActivityDto(
    val intervals: List<ActivityInterval> = emptyList()
)

/** Settings keys the tracker reads live (mirrored by SettingsEngine defs). */
object ActivitySettings {
    const val Enabled = "activity.tracking"
    const val AfkMode = "activity.afk-mode"
    const val AfkTimeoutMinutes = "activity.afk-timeout-minutes"
    const val RainEnabled = "activity.rain-enabled"
    const val RainDensity = "activity.rain-density"
    const val RainSpeed = "activity.rain-speed"
    const val RainOpacity = "activity.rain-opacity"
    const val RainDurationSeconds = "activity.rain-duration-seconds"
    const val RainContent = "activity.rain-content"
}

/**
 * Tracks engaged time and AFK state from interaction signals.
 *
 * Thread-safety: intended for single-threaded use from the UI/AppState
 * (Compose dispatches signals and reads state on the same thread).
 */
class ActivityTracker(
    private val settings: SettingsEngine,
    private val persistFile: java.io.File = java.io.File(
        System.getProperty("user.home"), ".kaiteyo/activity.json"
    )
) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    /** Closed (or fully-expired) engagement windows, oldest first. */
    private val intervals = ArrayDeque<ActivityInterval>()
    /** The currently open engagement, if any. */
    private var pending: ActivityInterval? = null
    private var lastSignalAtValue: Instant? = null
    private var afkSinceValue: Instant? = null

    /** Keep the persisted window bounded — ~4 years of hourly intervals. */
    private val maxIntervals = 35000

    init {
        load()
        // Never leave a half-open engagement behind on exit.
        Runtime.getRuntime().addShutdownHook(Thread { persist() })
    }

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    val enabled: Boolean get() = settings.getBool(ActivitySettings.Enabled, true)

    /** Timeout for the current mode (smart is context-aware; custom is fixed). */
    fun timeoutFor(context: SignalContext, now: Instant = Clock.System.now()): Duration {
        if (settings.getString(ActivitySettings.AfkMode, "smart") != "smart") {
            val minutes = settings.getInt(ActivitySettings.AfkTimeoutMinutes, 5).coerceIn(1, 120)
            return minutes.minutes
        }
        return when (context) {
            SignalContext.Study -> 5.minutes
            SignalContext.Writing -> 6.minutes
            SignalContext.Media -> 10.minutes
            SignalContext.General -> 2.minutes
        }
    }

    /** Record a real interaction. Opens or extends the current engagement. */
    fun recordSignal(
        context: SignalContext = SignalContext.General,
        now: Instant = Clock.System.now()
    ) {
        if (!enabled) return
        lastSignalAtValue = now
        val timeout = timeoutFor(context, now)
        val current = pending
        if (current == null) {
            // Fresh engagement — the very first signal of this run.
            pending = ActivityInterval(now.toString(), (now + timeout).toString())
        } else {
            val currentEnd = Instant.parse(current.endIso)
            if (now >= currentEnd) {
                // The previous engagement already lapsed → close it and start
                // a new one. This is the "resume after AFK" path: the session
                // is not permanently ended, it just starts a new interval.
                closePending(now)
                pending = ActivityInterval(now.toString(), (now + timeout).toString())
            } else {
                // Extend the live engagement.
                pending = ActivityInterval(current.startIso, (now + timeout).toString())
            }
        }
        if (afkSinceValue != null) afkSinceValue = null
    }

    /**
     * How long the user was genuinely engaged between [from] and [now].
     * Sums every interval that overlaps that window (clamped). Always ≤
     * (now - from). Falls back to the whole window when tracking is off.
     */
    fun engagedSince(from: Instant, now: Instant = Clock.System.now()): Duration {
        if (!enabled) return now - from
        val end = now
        var total = Duration.ZERO
        // Close a lapsed pending interval first so its expiry is counted.
        pending?.let { p ->
            if (end >= Instant.parse(p.endIso)) closePending(end)
        }
        synchronized(intervals) {
            for (interval in intervals) {
                total += overlap(interval.start, interval.end, from, end)
            }
        }
        pending?.let { p ->
            total += overlap(Instant.parse(p.startIso), Instant.parse(p.endIso), from, end)
        }
        return total
    }

    /** Engaged time that falls inside one calendar day (drives per-day totals). */
    fun engagedForDay(
        date: LocalDate,
        now: Instant = Clock.System.now()
    ): Duration {
        if (!enabled) return Duration.ZERO
        val tz = TimeZone.currentSystemDefault()
        val dayStart = date.atStartOfDayIn(tz)
        val dayEnd = dayStart + 1.days
        pending?.let { p ->
            if (now >= Instant.parse(p.endIso)) closePending(now)
        }
        var total = Duration.ZERO
        synchronized(intervals) {
            for (interval in intervals) {
                total += overlap(interval.start, interval.end, dayStart, dayEnd)
            }
        }
        pending?.let { p ->
            total += overlap(Instant.parse(p.startIso), Instant.parse(p.endIso), dayStart, dayEnd)
        }
        return total
    }

    /** Engaged time today so far — displayed on the dashboard/status. */
    fun engagedToday(now: Instant = Clock.System.now()): Duration =
        engagedForDay(now.toLocalDateTime(TimeZone.currentSystemDefault()).date, now)

    /**
     * Per-day totals for the trailing window of [days] calendar days ending at
     * [endDate] (oldest first, [endDate] last). One pass over the interval
     * store: each engagement counts as one session per day it touches and its
     * overlap with each day contributes to that day's engaged time. An interval
     * that spans midnight counts toward both days.
     */
    fun dayActivities(
        endDate: LocalDate,
        days: Int,
        now: Instant = Clock.System.now()
    ): List<DayActivity> {
        val tz = TimeZone.currentSystemDefault()
        val startDate = endDate.minus((days - 1).toLong(), DateTimeUnit.DAY)
        val windowStart = startDate.atStartOfDayIn(tz)
        val windowEnd = endDate.atStartOfDayIn(tz) + 1.days
        val result = (days - 1 downTo 0).map { offset ->
            val date = endDate.minus(offset.toLong(), DateTimeUnit.DAY)
            DayActivity(date, Duration.ZERO, 0)
        }.toMutableList()
        val indexByDate = result.withIndex().associate { (i, row) -> row.date to i }
        if (!enabled) return result

        pending?.let { p -> if (now >= Instant.parse(p.endIso)) closePending(now) }

        fun accumulate(interval: ActivityInterval) {
            val s = maxOf(interval.start, windowStart)
            val e = minOf(interval.end, windowEnd)
            if (e <= s) return
            var dayStart = s.toLocalDateTime(tz).date.atStartOfDayIn(tz)
            while (dayStart < e) {
                val dayEnd = dayStart + 1.days
                val overlap = overlap(dayStart, dayEnd, s, e)
                if (overlap > Duration.ZERO) {
                    indexByDate[dayStart.toLocalDateTime(tz).date]?.let { i ->
                        val row = result[i]
                        result[i] = DayActivity(row.date, row.engaged + overlap, row.sessions + 1)
                    }
                }
                dayStart = dayEnd
            }
        }

        synchronized(intervals) {
            for (interval in intervals) accumulate(interval)
        }
        pending?.let { accumulate(it) }
        return result
    }

    /**
     * Current state: Active while a signal is fresh, Idle on first run before
     * any signal, Afk once the engagement lapsed. Used by the rain overlay.
     * O(1) — it is polled every second by the UI and must never iterate.
     */
    fun snapshot(now: Instant = Clock.System.now()): ActivitySnapshot {
        if (!enabled || lastSignalAtValue == null) {
            return ActivitySnapshot(ActivityState.Idle, lastSignalAtValue, null)
        }
        val lapsed = pending != null && now >= Instant.parse(pending!!.endIso)
        val state = if (lapsed) ActivityState.Afk else ActivityState.Active
        val afkSince = if (lapsed) Instant.parse(pending!!.endIso) else null
        return ActivitySnapshot(state, lastSignalAtValue, afkSince)
    }

    val lastSignalAt: Instant? get() = lastSignalAtValue

    /** True while the user is away and rain is enabled — drives the overlay. */
    fun isAfk(now: Instant = Clock.System.now()): Boolean =
        snapshot(now).state == ActivityState.Afk

    /** For tests / settings changes — drop all in-memory state. */
    fun resetForTesting() {
        synchronized(intervals) { intervals.clear() }
        pending = null
        lastSignalAtValue = null
        afkSinceValue = null
    }

    // ------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------

    private fun overlap(aStart: Instant, aEnd: Instant, bStart: Instant, bEnd: Instant): Duration {
        val start = maxOf(aStart, bStart)
        val end = minOf(aEnd, bEnd)
        return if (end > start) end - start else Duration.ZERO
    }

    private fun closePending(now: Instant) {
        val p = pending ?: return
        synchronized(intervals) {
            intervals.addLast(p)
            if (intervals.size > maxIntervals) {
                repeat(intervals.size - maxIntervals) { intervals.removeFirst() }
            }
        }
        pending = null
        afkSinceValue = now
        persist()
    }

    // ------------------------------------------------------------
    // Persistence (~/.kaiteyo/activity.json)
    // ------------------------------------------------------------

    private fun load() {
        if (!persistFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<ActivityDto>(persistFile.readText())
            synchronized(intervals) {
                intervals.clear()
                dto.intervals.takeLast(maxIntervals).forEach { intervals.addLast(it) }
            }
        }
    }

    private fun persist() {
        runCatching {
            persistFile.parentFile?.mkdirs()
            val snapshotDto: ActivityDto
            synchronized(intervals) {
                val all = buildList {
                    addAll(intervals)
                    pending?.let { add(it) }
                }
                snapshotDto = ActivityDto(all.takeLast(maxIntervals))
            }
            persistFile.writeText(json.encodeToString(snapshotDto))
        }
    }

    /** Trim + persist immediately (called when a session ends). */
    fun flush() = persist()
}
