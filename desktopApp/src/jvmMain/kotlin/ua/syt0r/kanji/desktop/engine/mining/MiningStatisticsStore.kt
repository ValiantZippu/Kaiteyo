package ua.syt0r.kanji.desktop.engine.mining

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// ============================================
// KAITEYO MINING STATISTICS STORE
// Persistent, incremental counters for card
// mining across EVERY source (dictionary, media,
// browser, OCR, clipboard, reader, API, manual).
// Unlike MediaStatisticsStore (which only counts
// mines that happened inside the media player),
// this store is the single source of truth for
// "how many sentences has the user mined".
// Aggregates are kept per local day and per
// source, so Statistics and Dashboard can answer
// "mined today / this week / from what source"
// without scanning card history.
// ============================================

/** One day of mining activity. [date] is the user's local day (yyyy-MM-dd). */
@Serializable
data class MiningDayStat(
    val date: String,
    val mined: Int = 0,
    val bySource: Map<String, Int> = emptyMap()
)

/** Everything the store persists (also the backup/export format). */
@Serializable
data class MiningStatsDto(
    val days: List<MiningDayStat> = emptyList(),
    val totalMined: Int = 0,
    val bySource: Map<String, Int> = emptyMap()
)

/** Maximum number of daily buckets kept (one year is plenty for a heatmap). */
private const val MAX_MINING_DAYS = 366

class MiningStatisticsStore(
    private val directory: File = File(System.getProperty("user.home"), ".kaiteyo/media")
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val statsFile: File get() = File(directory, "mining-stats.json")

    // Days are kept newest-first (index 0 == today when present).
    val days = mutableStateListOf<MiningDayStat>()

    /** All-time mined count per source (observable — live for dashboards). */
    val bySource = mutableStateMapOf<String, Int>()

    var totalMined by mutableStateOf(0)
        private set

    init {
        directory.mkdirs()
        load()
    }

    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // ------------------------------------------------------------
    // Recording (incremental — O(1), never a history scan)
    // ------------------------------------------------------------

    /** A card was mined from [source] (any source — see [MiningSource]). */
    fun recordMine(source: String) {
        val key = source.ifBlank { "manual" }
        totalMined += 1
        bySource[key] = (bySource[key] ?: 0) + 1
        bumpToday { day ->
            day.copy(
                mined = day.mined + 1,
                bySource = day.bySource + (key to ((day.bySource[key] ?: 0) + 1))
            )
        }
        persist()
    }

    // ------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------

    fun day(date: LocalDate): MiningDayStat =
        days.firstOrNull { it.date == date.toString() } ?: MiningDayStat(date = date.toString())

    fun minedOn(date: LocalDate): Int = day(date).mined

    fun daysBetween(start: LocalDate, end: LocalDate): List<MiningDayStat> =
        days.filter { d ->
            val date = runCatching { LocalDate.parse(d.date) }.getOrNull()
            date != null && date >= start && date <= end
        }.sortedBy { it.date }

    fun minedBetween(start: LocalDate, end: LocalDate): Int =
        daysBetween(start, end).sumOf { it.mined }

    /** Top [n] all-time sources by mined count (source to count). */
    fun minedBySourceTop(n: Int): List<Pair<String, Int>> =
        bySource.entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key to it.value }

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------

    private var lastPersistMs = 0L

    private fun persist() {
        val now = System.currentTimeMillis()
        if (now - lastPersistMs < 1_000) return
        lastPersistMs = now
        runCatching {
            statsFile.writeText(
                json.encodeToString(
                    MiningStatsDto(
                        days = days.toList(),
                        totalMined = totalMined,
                        bySource = bySource.toMap()
                    )
                )
            )
        }
    }

    private fun load() {
        if (!statsFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<MiningStatsDto>(statsFile.readText())
            days.clear(); days.addAll(dto.days)
            totalMined = dto.totalMined
            bySource.clear(); bySource.putAll(dto.bySource)
        }
    }

    /** Update (or create) the bucket for [key], newest-first, capped. */
    private fun bumpToday(transform: (MiningDayStat) -> MiningDayStat) {
        val key = today().toString()
        val idx = days.indexOfFirst { it.date == key }
        if (idx >= 0) {
            days[idx] = transform(days[idx])
        } else {
            days.add(0, transform(MiningDayStat(date = key)))
            while (days.size > MAX_MINING_DAYS) days.removeAt(days.lastIndex)
        }
    }
}
