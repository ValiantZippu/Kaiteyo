package ua.syt0r.kanji.desktop.engine.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// ============================================
// KAITEYO MEDIA STATISTICS STORE
// Persistent, incremental media activity data.
// Watch time (leisure vs study), dictionary
// lookups, mined sentences and watch sessions
// are recorded per local day. History is
// append-only in aggregate form — a review
// never rewrites a past day. The store answers
// "how much media have I watched / studied /
// looked up / mined" for any date range without
// scanning review history.
// ============================================

/** One day of media activity. [date] is the user's local day (yyyy-MM-dd). */
@Serializable
data class MediaDayStat(
    val date: String,
    val watchMs: Long = 0,
    val studyMs: Long = 0,
    val lookups: Int = 0,
    val mined: Int = 0,
    val sessions: Int = 0
) {
    val watchMinutes: Long get() = watchMs / 60000
    val studyMinutes: Long get() = studyMs / 60000
}

/** Everything the store persists (also serves as the backup/export format). */
@Serializable
data class MediaStatsDto(
    val days: List<MediaDayStat> = emptyList(),
    val totalWatchMs: Long = 0,
    val totalStudyMs: Long = 0,
    val totalLookups: Int = 0,
    val totalMined: Int = 0,
    val totalSessions: Int = 0
)

/** Human-facing overview for the Media dashboard / Statistics integration. */
data class MediaStatsSummary(
    val totalWatchMs: Long,
    val totalStudyMs: Long,
    val totalLookups: Int,
    val totalMined: Int,
    val totalSessions: Int,
    val distinctDays: Int,
    val activeLast7Days: Int
) {
    val mediaHours: Float get() = totalWatchMs / 3600000f
    val studyHours: Float get() = totalStudyMs / 3600000f
    val leisureHours: Float get() = (totalWatchMs - totalStudyMs) / 3600000f
}

/** Maximum number of daily buckets kept (one year is plenty for a heatmap). */
private const val MAX_DAYS = 366

class MediaStatisticsStore(
    private val directory: File = File(System.getProperty("user.home"), ".kaiteyo/media")
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val statsFile: File get() = File(directory, "stats.json")

    // Days are kept newest-first (index 0 == today when present).
    val days = mutableStateListOf<MediaDayStat>()

    var totalWatchMs by mutableStateOf(0L)
        private set
    var totalStudyMs by mutableStateOf(0L)
        private set
    var totalLookups by mutableStateOf(0)
        private set
    var totalMined by mutableStateOf(0)
        private set
    var totalSessions by mutableStateOf(0)
        private set

    init {
        directory.mkdirs()
        load()
    }

    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // ------------------------------------------------------------
    // Recording (incremental — O(1), never a history scan)
    // ------------------------------------------------------------

    /** A continuous playback stretch ended. [study] = study-mode was active. */
    fun recordWatch(ms: Long, study: Boolean) {
        if (ms <= 0) return
        totalWatchMs += ms
        bumpToday { it.copy(watchMs = it.watchMs + ms) }
        if (study) {
            totalStudyMs += ms
            bumpToday { it.copy(studyMs = it.studyMs + ms) }
        }
        persist()
    }

    /** A dictionary lookup was performed (word selection or manual query). */
    fun recordLookup() {
        totalLookups += 1
        bumpToday { it.copy(lookups = it.lookups + 1) }
        persist()
    }

    /** A card was mined from media. */
    fun recordMined() {
        totalMined += 1
        bumpToday { it.copy(mined = it.mined + 1) }
        persist()
    }

    /** A media item was opened (one per open, not per resume). */
    fun recordSession() {
        totalSessions += 1
        bumpToday { it.copy(sessions = it.sessions + 1) }
        persist()
    }

    /**
     * Backfill/record watch time against an explicit local date. Used by tests
     * (and a possible future import/manual-entry path) without touching the
     * system clock.
     */
    internal fun recordWatchOn(date: String, watchMs: Long) {
        if (watchMs <= 0) return
        totalWatchMs += watchMs
        bump(date) { it.copy(watchMs = it.watchMs + watchMs) }
        persist()
    }

    /** Update today's bucket via [transform], creating it when missing. */
    private fun bumpToday(transform: (MediaDayStat) -> MediaDayStat) =
        bump(today().toString(), transform)

    /** Update (or create) the bucket for [key], newest-first, capped. */
    private fun bump(key: String, transform: (MediaDayStat) -> MediaDayStat) {
        val idx = days.indexOfFirst { it.date == key }
        if (idx >= 0) {
            days[idx] = transform(days[idx])
        } else {
            days.add(0, transform(MediaDayStat(date = key)))
            while (days.size > MAX_DAYS) days.removeAt(days.lastIndex)
        }
    }

    // ------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------

    fun day(date: LocalDate): MediaDayStat = days.firstOrNull { it.date == date.toString() }
        ?: MediaDayStat(date = date.toString())

    fun daysBetween(start: LocalDate, end: LocalDate): List<MediaDayStat> =
        days.filter { d ->
            val date = runCatching { LocalDate.parse(d.date) }.getOrNull()
            date != null && date >= start && date <= end
        }.sortedBy { it.date }

    fun watchMsBetween(start: LocalDate, end: LocalDate): Long =
        daysBetween(start, end).sumOf { it.watchMs }

    fun studyMsBetween(start: LocalDate, end: LocalDate): Long =
        daysBetween(start, end).sumOf { it.studyMs }

    fun lookupsBetween(start: LocalDate, end: LocalDate): Int =
        daysBetween(start, end).sumOf { it.lookups }

    fun minedBetween(start: LocalDate, end: LocalDate): Int =
        daysBetween(start, end).sumOf { it.mined }

    val mediaHours: Float get() = totalWatchMs / 3600000f
    val studyHours: Float get() = totalStudyMs / 3600000f

    /** Days with any recorded activity inside the last [days] local days. */
    fun activeDays(lastDays: Int): Int {
        val today = today()
        val cutoff = today.minus(lastDays.toLong(), DateTimeUnit.DAY)
        return days.count { d ->
            runCatching { LocalDate.parse(d.date) }.getOrNull()?.let { it >= cutoff && it <= today } == true
        }
    }

    fun summary(): MediaStatsSummary = MediaStatsSummary(
        totalWatchMs = totalWatchMs,
        totalStudyMs = totalStudyMs,
        totalLookups = totalLookups,
        totalMined = totalMined,
        totalSessions = totalSessions,
        distinctDays = days.size,
        activeLast7Days = activeDays(7)
    )

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------

    // Coalesce writes: rapid lookups/mines shouldn't rewrite the file each time.
    private var lastPersistMs = 0L

    private fun persist() {
        val now = System.currentTimeMillis()
        if (now - lastPersistMs < 1_000) return
        lastPersistMs = now
        runCatching {
            statsFile.writeText(
                json.encodeToString(
                    MediaStatsDto(
                        days.toList(),
                        totalWatchMs,
                        totalStudyMs,
                        totalLookups,
                        totalMined,
                        totalSessions
                    )
                )
            )
        }
    }

    private fun load() {
        if (!statsFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<MediaStatsDto>(statsFile.readText())
            days.clear(); days.addAll(dto.days)
            totalWatchMs = dto.totalWatchMs
            totalStudyMs = dto.totalStudyMs
            totalLookups = dto.totalLookups
            totalMined = dto.totalMined
            totalSessions = dto.totalSessions
        }
    }
}
