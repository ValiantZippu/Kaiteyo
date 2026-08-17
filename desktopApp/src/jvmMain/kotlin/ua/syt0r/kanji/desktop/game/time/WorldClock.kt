package ua.syt0r.kanji.desktop.game.time

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import kotlin.math.sin

/**
 * World time (spec §40). The slice runs on a compressed clock so day/night
 * cycles are visible during a play session; real minutes are trivial to
 * restore later. Time gates activities: shops open, the station master is
 * only at the station in the morning.
 */
class WorldClock(
    var minuteOfDay: Int = 9 * 60,
    var day: Int = 1
) {

    /** How many real seconds equal one world minute (30 = 48 min per hour). */
    var secondsPerWorldMinute: Float = 0.5f

    private var accumulator: Float = 0f

    val phase: TimePhase get() = TimePhase.fromMinutes(minuteOfDay)

    fun tick(dt: Float) {
        accumulator += dt
        while (accumulator >= secondsPerWorldMinute) {
            accumulator -= secondsPerWorldMinute
            minuteOfDay++
            if (minuteOfDay >= 1440) {
                minuteOfDay = 0
                day++
            }
        }
    }

    /** Jump the clock forward (train travel, time-gated events). */
    fun advanceMinutes(minutes: Int) {
        var m = minuteOfDay + minutes
        while (m >= 1440) {
            m -= 1440
            day++
        }
        minuteOfDay = m
    }

    /** World-clock tint the renderer applies (night = deep blue). */
    fun lightTint(): Float = when (phase) {
        TimePhase.Morning -> 0.06f
        TimePhase.Day -> 0f
        TimePhase.Evening -> 0.12f
        TimePhase.Night -> 0.5f
    }

    fun hourLabel(): String = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

    companion object {
        const val MINUTES_PER_DAY = 1440
    }
}

@Serializable
enum class TimePhase(val label: String) {
    Morning("Morning"),
    Day("Day"),
    Evening("Evening"),
    Night("Night");

    companion object {
        fun fromMinutes(minutes: Int): TimePhase = when {
            minutes in 5 * 60 until 10 * 60 -> Morning
            minutes in 10 * 60 until 17 * 60 -> Day
            minutes in 17 * 60 until 21 * 60 -> Evening
            else -> Night
        }
    }
}

/** Weather (spec §41) — V1 is a simple sun/cloud/rain model. */
@Serializable
enum class WeatherKind(val label: String) {
    Sun("Sun"), Cloud("Cloud"), Rain("Rain"), Snow("Snow")
}

class WeatherSystem(initial: WeatherKind = WeatherKind.Sun) {

    var current by mutableStateOf(initial)
        private set

    /** Restore weather from a save. */
    fun setWeather(kind: WeatherKind) {
        current = kind
    }

    private var transitionTimer = 0f
    private val transitionInterval = 45f

    fun tick(dt: Float) {
        // Gentle random transitions — never aggressive (spec §41).
        transitionTimer += dt
        if (transitionTimer >= transitionInterval) {
            transitionTimer = 0f
            if (kotlin.random.Random.nextFloat() < 0.06f) {
                // The season leans on the weather (spec §42): winter snows,
                // spring rains. The seasonal weather is much more likely, so
                // a snowy-day quest is reachable, not a lottery.
                val seasonal = seasonalWeather
                val next = if (kotlin.random.Random.nextFloat() < 0.6f) {
                    seasonal
                } else {
                    WeatherKind.entries.random()
                }
                if (next != null && next != current) {
                    current = next
                }
            }
        }
    }

    /**
     * The weather this world leans toward, set by the season system each
     * tick (Winter → Snow, Spring → Rain, …). Null until the season syncs.
     */
    var seasonalWeather: WeatherKind? = null

    /** Rain intensity 0..1 used by the VFX layer. */
    fun rainIntensity(): Float =
        if (current == WeatherKind.Rain) 0.5f + 0.3f * sin(System.nanoTime() / 5e8f) else 0f
}
