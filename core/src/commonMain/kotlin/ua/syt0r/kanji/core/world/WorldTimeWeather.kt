package ua.syt0r.kanji.core.world

import kotlin.random.Random

// ============================================================
// WORLD — TIME OF DAY & WEATHER
// ------------------------------------------------------------
// A day/night cycle and a weather system. Both are observable
// through the runtime's flows so the renderer and UI can react.
// Weather transitions smoothly; the day cycle is a continuous
// 0..1 clock.
// ============================================================

/**
 * Time-of-day configuration.
 */
data class DayCycleConfig(
    /** Real seconds for one full day. Default 24 minutes. */
    val dayLengthSeconds: Double = 1440.0,
    /** Starting time of day 0..1 (0.35 ≈ 8:24 AM). */
    val startTime: Float = 0.35f
)

/**
 * Weather configuration.
 */
data class WeatherConfig(
    /** Minimum seconds between weather changes. */
    val minChangeIntervalSeconds: Double = 180.0,
    /** Probability weights per weather type. */
    val weights: Map<WorldWeather, Double> = mapOf(
        WorldWeather.Clear to 0.40,
        WorldWeather.PartlyCloudy to 0.25,
        WorldWeather.Cloudy to 0.15,
        WorldWeather.Rain to 0.10,
        WorldWeather.HeavyRain to 0.04,
        WorldWeather.Fog to 0.03,
        WorldWeather.Wind to 0.03
    )
)

/**
 * Time and weather system — advances the clock and rolls weather.
 */
class TimeWeatherSystem(
    private val dayConfig: DayCycleConfig = DayCycleConfig(),
    private val weatherConfig: WeatherConfig = WeatherConfig(),
    seed: Long = 99
) : WorldSystem {

    private val rng = Random(seed)
    private var elapsedSeconds = 0.0
    private var timeOfDay = dayConfig.startTime
    private var weather: WorldWeather = WorldWeather.Clear
    private var timeSinceWeatherChange = 0.0

    /** Current time of day 0..1. */
    val currentTimeOfDay: Float get() = timeOfDay

    /** Current weather. */
    val currentWeather: WorldWeather get() = weather

    /** Human-readable clock (e.g. "14:30"). */
    fun clockLabel(): String {
        val totalMinutes = (timeOfDay * 24 * 60).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "%02d:%02d".format(hours, minutes)
    }

    /** Whether it is currently day (6:00–18:00). */
    val isDaytime: Boolean get() = isDaytimeAt(timeOfDay)

    /** Whether the given time of day is daylight (6:00–18:00). */
    fun isDaytimeAt(value: Float): Boolean = value in 0.25f..0.75f

    /** Sun intensity 0..1 (for renderer lighting). */
    fun sunIntensity(): Float = sunIntensityAt(timeOfDay)

    /** Sun intensity for an arbitrary time of day (0 = midnight, 0.5 = noon). */
    fun sunIntensityAt(value: Float): Float {
        val dist = kotlin.math.abs(value - 0.5f)
        return (1f - dist * 2f).coerceIn(0f, 1f)
    }

    /** Human-readable clock for an arbitrary time of day (e.g. "14:30"). */
    fun clockLabelAt(value: Float): String {
        val totalMinutes = ((value.coerceIn(0f, 1f)) * 24 * 60).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "%02d:%02d".format(hours, minutes)
    }

    override suspend fun onStart(runtime: WorldRuntime) {
        timeOfDay = dayConfig.startTime
        weather = WorldWeather.Clear
        elapsedSeconds = 0.0
        runtime.setTimeOfDay(timeOfDay)
        runtime.setWeather(weather)
    }

    override suspend fun onStop(runtime: WorldRuntime) {}

    override suspend fun onUpdate(deltaSeconds: Double, runtime: WorldRuntime) {
        elapsedSeconds += deltaSeconds
        timeOfDay = (timeOfDay + (deltaSeconds / dayConfig.dayLengthSeconds).toFloat()) % 1.0f
        runtime.setTimeOfDay(timeOfDay)

        timeSinceWeatherChange += deltaSeconds
        if (timeSinceWeatherChange >= weatherConfig.minChangeIntervalSeconds) {
            timeSinceWeatherChange = 0.0
            // Roll the next weather using the weights.
            val total = weatherConfig.weights.values.sum()
            var roll = rng.nextDouble() * total
            var next = WorldWeather.Clear
            for ((type, weight) in weatherConfig.weights) {
                roll -= weight
                if (roll <= 0) {
                    next = type
                    break
                }
            }
            if (next != weather) {
                weather = next
                runtime.setWeather(weather)
            }
        }
    }
}
