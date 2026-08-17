package ua.syt0r.kanji.desktop.game.time

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable

// ============================================================
// SEASONS (spec §42) — spring / summer / autumn / winter
// The world's palette and some content shift with the season.
// The cycle is a small number of world-days so players actually
// see the change; real-season pacing is a constant away.
// ============================================================

@Serializable
enum class Season(val label: String) {
    Spring("Spring"),
    Summer("Summer"),
    Autumn("Autumn"),
    Winter("Winter");

    companion object {
        fun fromDay(day: Int, cycleDays: Int): Season {
            val index = ((day - 1).floorMod(cycleDays * 4)) / cycleDays
            return entries[index.coerceIn(0, 3)]
        }
    }
}

/** Tint overlay each season applies (in addition to time-of-day). */
val Season.tint: Long
    get() = when (this) {
        Season.Spring -> 0x4CAF50 // fresh green
        Season.Summer -> 0x000000 // none
        Season.Autumn -> 0xFF8F00 // warm amber
        Season.Winter -> 0x90CAF9 // cool blue
    }

/** Overlay alpha; summer is neutral. */
val Season.tintAlpha: Float
    get() = when (this) {
        Season.Spring -> 0.07f
        Season.Summer -> 0f
        Season.Autumn -> 0.10f
        Season.Winter -> 0.14f
    }

/** The ambient-audio season colour (spec §42, §91-92). */
val Season.toSeasonAudio: ua.syt0r.kanji.desktop.game.audio.SeasonAudio
    get() = when (this) {
        Season.Spring -> ua.syt0r.kanji.desktop.game.audio.SeasonAudio.Spring
        Season.Summer -> ua.syt0r.kanji.desktop.game.audio.SeasonAudio.Summer
        Season.Autumn -> ua.syt0r.kanji.desktop.game.audio.SeasonAudio.Autumn
        Season.Winter -> ua.syt0r.kanji.desktop.game.audio.SeasonAudio.Winter
    }

/**
 * Owns the world's season. Derived from the [WorldClock]'s day counter so a
 * save/load never loses the season; [cycleDays] is how many world-days each
 * season lasts.
 */
class SeasonSystem(
    private val clock: WorldClock,
    var cycleDays: Int = 3
) {
    var current by mutableStateOf(Season.Summer)
        private set

    /** Recompute the season from the clock's day; returns true on change. */
    fun sync(): Boolean {
        val next = Season.fromDay(clock.day, cycleDays)
        val changed = next != current
        current = next
        return changed
    }

    /**
     * The weather a season leans toward (spec §41-42): winter snows,
     * spring rains, summer is clear. The weather system uses this so
     * seasonal content is reachable instead of pure RNG.
     */
    fun seasonalWeather(): ua.syt0r.kanji.desktop.game.time.WeatherKind = when (current) {
        Season.Spring -> ua.syt0r.kanji.desktop.game.time.WeatherKind.Rain
        Season.Summer -> ua.syt0r.kanji.desktop.game.time.WeatherKind.Sun
        Season.Autumn -> ua.syt0r.kanji.desktop.game.time.WeatherKind.Cloud
        Season.Winter -> ua.syt0r.kanji.desktop.game.time.WeatherKind.Snow
    }
}

private fun Int.floorMod(mod: Int): Int = ((this % mod) + mod) % mod
