package ua.syt0r.kanji.desktop.game.audio

import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.math.PI
import kotlin.math.sin

/**
 * Game audio (spec §91-92) — procedural, zero asset files. Every sound is
 * synthesized from a few pure functions ([synthTone] is the testable core),
 * so the slice ships with real sound effects and a soft ambient pad without
 * a single audio asset.
 *
 * The mixer is fire-and-forget: each effect plays on a short-lived daemon
 * thread; volumes come from [GameSettings]. Nothing here can throw the game
 * loop (headless hosts just stay silent).
 */
class GameAudio {

    private val pool = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "kaiteyo-audio").apply { isDaemon = true }
    }

    @Volatile
    private var sfxVolume: Float = 0.8f
    @Volatile
    private var musicVolume: Float = 0.7f
    @Volatile
    private var ambientEnabled: Boolean = true

    /** The currently playing ambient loop (region theme), or null. */
    @Volatile
    private var ambientKind: AmbientKind? = null
    @Volatile
    private var ambientSeason: SeasonAudio = SeasonAudio.Summer
    private val ambientLock = Any()
    private var ambientThread: Thread? = null

    fun configure(sfxVolume: Float, musicVolume: Float, ambientEnabled: Boolean) {
        this.sfxVolume = sfxVolume.coerceIn(0f, 1f)
        this.musicVolume = musicVolume.coerceIn(0f, 1f)
        this.ambientEnabled = ambientEnabled
        if (!ambientEnabled) stopAmbient()
    }

    /**
     * Set the season the ambient layer responds to (spec §42 + §91-92).
     * Spring adds birdsong, summer cicadas, autumn dry leaves, winter wind —
     * the same region pad, coloured by the season.
     */
    fun setSeason(season: SeasonAudio) {
        synchronized(ambientLock) {
            if (ambientSeason == season) return
            ambientSeason = season
            // Restart the loop so the new texture applies.
            val kind = ambientKind
            if (kind != null) {
                stopAmbient()
                setAmbient(kind)
            }
        }
    }

    // ------------------------------------------------------------
    // SFX
    // ------------------------------------------------------------

    /** Play a synthesized effect at the current SFX volume (non-blocking). */
    fun play(effect: Sfx) {
        val volume = sfxVolume
        if (volume <= 0.01f) return
        val tones = effect.tones
        pool.execute {
            try {
                playTones(tones, volume)
            } catch (t: Throwable) {
                // No audio device / headless — the game never cares.
            }
        }
    }

    // ------------------------------------------------------------
    // Ambient
    // ------------------------------------------------------------

    /**
     * Start (or switch) the ambient loop for a region theme. Idempotent —
     * calling with the same kind is a no-op; null stops the pad.
     */
    fun setAmbient(kind: AmbientKind?) {
        synchronized(ambientLock) {
            if (ambientKind == kind && (kind == null || ambientThread?.isAlive == true)) return
            stopAmbient()
            ambientKind = kind ?: return
            if (!ambientEnabled || musicVolume <= 0.01f) return
            val thread = Thread({
                try {
                    runAmbientLoop(kind)
                } catch (t: Throwable) {
                    // Silent on headless hosts.
                } finally {
                    synchronized(ambientLock) { ambientThread = null }
                }
            }, "kaiteyo-ambient").apply { isDaemon = true }
            ambientThread = thread
            thread.start()
        }
    }

    fun stopAmbient() {
        synchronized(ambientLock) {
            ambientKind = null
            ambientThread?.interrupt()
            ambientThread = null
        }
    }

    /** Shut the audio system down with the session. */
    fun shutdown() {
        stopAmbient()
        pool.shutdownNow()
    }

    // ------------------------------------------------------------
    // Mixer internals
    // ------------------------------------------------------------

    /** Render + play one effect (a short tone sequence) on this thread. */
    private fun playTones(tones: List<Tone>, volume: Float) {
        val sampleRate = 22050
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val line = (AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine).apply {
            open(format)
            start()
        }
        try {
            for (tone in tones) {
                val samples = synthTone(
                    frequency = tone.frequency,
                    seconds = tone.durationSeconds,
                    volume = volume * tone.volume,
                    wave = tone.wave,
                    sampleRate = sampleRate
                )
                line.write(shortToBytes(samples), 0, samples.size * 2)
                if (tone.gapSeconds > 0) {
                    // Pad with silence so notes breathe.
                    val gap = IntArray((tone.gapSeconds * sampleRate).toInt())
                    line.write(shortToBytes(gap), 0, gap.size * 2)
                }
            }
        } finally {
            line.drain()
            line.close()
        }
    }

    /** The ambient loop: a slow chord pad with a light texture per theme. */
    private fun runAmbientLoop(kind: AmbientKind) {
        val sampleRate = 22050
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val line = (AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine).apply {
            open(format)
            start()
        }
        try {
            // A quiet two-note pad (fifth), breathing with a slow LFO.
            val chord = kind.chordFrequencies()
            var phase = 0.0
            var sampleIndex = 0L
            val chunk = 2048
            while (!Thread.currentThread().isInterrupted) {
                val volume = musicVolume * 0.05f
                val samples = ShortArray(chunk)
                for (i in 0 until chunk) {
                    val t = sampleIndex.toDouble() / sampleRate
                    val lfo = 0.5 + 0.5 * sin(2.0 * PI * 0.05 * t) // slow swell
                    var v = 0.0
                    for (f in chord) {
                        v += sin(2.0 * PI * f * t) * (0.35 + 0.65 * lfo)
                    }
                    v /= chord.size
                    // Texture: gentle noise bursts for beach (waves).
                    if (kind == AmbientKind.Beach && sin(2.0 * PI * 0.11 * t) > 0.72) {
                        v += (java.util.Random().nextDouble() * 2.0 - 1.0) * 0.25
                    }
                    if (kind == AmbientKind.Station) {
                        // Faint electric hum: a detuned upper harmonic.
                        v += 0.12 * sin(2.0 * PI * chord.first() * 3.01 * t)
                    }
                    // Seasonal colour (spec §42): the same pad, different mood.
                    val season = ambientSeason
                    when (season) {
                        SeasonAudio.Spring ->
                            // Birdsong: a few quick high chirps when the LFO peaks.
                            if (sin(2.0 * PI * 0.23 * t) > 0.93) {
                                v += 0.14 * sin(2.0 * PI * 1244.0 * t) * (1.0 - kotlin.math.abs(sin(2.0 * PI * 9.0 * t)))
                            }
                        SeasonAudio.Summer ->
                            // Cicadas: a soft high shimmer near the crest.
                            v += 0.08 * sin(2.0 * PI * 523.25 * t) * (0.5 + 0.5 * sin(2.0 * PI * 0.09 * t))
                        SeasonAudio.Autumn ->
                            // Dry leaves: sparse noise crackles.
                            if (sin(2.0 * PI * 0.37 * t) > 0.8) {
                                v += (java.util.Random().nextDouble() * 2.0 - 1.0) * 0.18
                            }
                        SeasonAudio.Winter ->
                            // Wind: slow filtered-sounding wobble.
                            v += 0.10 * sin(2.0 * PI * 0.03 * t) * (java.util.Random().nextDouble() * 2.0 - 1.0)
                    }
                    val value = (v * volume * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767)
                    samples[i] = value.toShort()
                    sampleIndex++
                }
                line.write(shortToBytes(samples), 0, samples.size * 2)
            }
        } finally {
            line.drain()
            line.close()
        }
    }
}

// ------------------------------------------------------------
// Data
// ------------------------------------------------------------

/** Every synthesized sound effect in the game. */
enum class Sfx(val tones: List<Tone>) {
    /** Soft interaction blip. */
    Blip(listOf(Tone(660f, 0.06f, wave = Wave.Sine, volume = 0.5f))),
    /** Discovery — a rising two-note sparkle. */
    Sparkle(
        listOf(
            Tone(880f, 0.09f, wave = Wave.Triangle, volume = 0.5f),
            Tone(1320f, 0.14f, wave = Wave.Triangle, volume = 0.5f, gapSeconds = 0.02f)
        )
    ),
    /** Photo shutter — a quick broadband click. */
    Shutter(
        listOf(
            Tone(180f, 0.03f, wave = Wave.Noise, volume = 0.7f),
            Tone(90f, 0.04f, wave = Wave.Sine, volume = 0.6f, gapSeconds = 0.01f)
        )
    ),
    /** Quest complete — a little major arpeggio. */
    Chime(
        listOf(
            Tone(523f, 0.12f, wave = Wave.Sine, volume = 0.5f),
            Tone(659f, 0.12f, wave = Wave.Sine, volume = 0.5f, gapSeconds = 0.03f),
            Tone(784f, 0.22f, wave = Wave.Sine, volume = 0.5f, gapSeconds = 0.03f)
        )
    ),
    /** Reward / collectible pickup. */
    Coin(
        listOf(
            Tone(988f, 0.07f, wave = Wave.Triangle, volume = 0.5f),
            Tone(1319f, 0.16f, wave = Wave.Triangle, volume = 0.5f, gapSeconds = 0.02f)
        )
    ),
    /** Friendly denied / closed / locked. */
    Boop(
        listOf(
            Tone(330f, 0.08f, wave = Wave.Square, volume = 0.25f),
            Tone(262f, 0.12f, wave = Wave.Square, volume = 0.25f, gapSeconds = 0.02f)
        )
    ),
    /** UI menu click. */
    Click(listOf(Tone(520f, 0.04f, wave = Wave.Square, volume = 0.2f))),
    /** Train boarding — a short chug. */
    Train(
        listOf(
            Tone(140f, 0.1f, wave = Wave.Saw, volume = 0.3f),
            Tone(200f, 0.12f, wave = Wave.Saw, volume = 0.3f, gapSeconds = 0.05f)
        )
    ),
    /** Writing success — a clean high note. */
    WriteOk(listOf(Tone(784f, 0.18f, wave = Wave.Sine, volume = 0.5f))),
    /** Writing attempt — neutral. */
    WriteTry(listOf(Tone(392f, 0.1f, wave = Wave.Sine, volume = 0.4f)))
}

/** One synthesized note. */
data class Tone(
    val frequency: Float,
    val durationSeconds: Float,
    val wave: Wave = Wave.Sine,
    val volume: Float = 0.5f,
    val gapSeconds: Float = 0f
)

enum class Wave { Sine, Triangle, Square, Saw, Noise }

/** Ambient loop themes, one per region (spec §91-92). */
enum class AmbientKind(val chordFrequencies: () -> List<Double>) {
    SeasideTown({ listOf(220.0, 277.18, 329.63) }), // A major — warm town
    Beach({ listOf(196.0, 246.94) }),               // G — open water
    Station({ listOf(110.0, 164.81, 220.0) }),      // A — deep, humming
    HistoricTown({ listOf(174.61, 220.0, 261.63) }) // F — temple bell mood
}

/**
 * The season colour applied to the ambient pad (spec §42, §91-92). Mirrors
 * [ua.syt0r.kanji.desktop.game.time.Season] without coupling audio to time.
 */
enum class SeasonAudio { Spring, Summer, Autumn, Winter }

// ------------------------------------------------------------
// Pure synthesis core (unit-tested)
// ------------------------------------------------------------

/**
 * Render one tone to 16-bit mono samples. Envelope: quick attack, exponential
 * decay — so notes never click. Pure and deterministic for tests.
 */
fun synthTone(
    frequency: Float,
    seconds: Float,
    volume: Float,
    wave: Wave = Wave.Sine,
    sampleRate: Int = 22050
): ShortArray {
    val count = (seconds * sampleRate).toInt().coerceAtLeast(0)
    val samples = ShortArray(count)
    val phaseStep = 2.0 * PI * frequency / sampleRate
    for (i in 0 until count) {
        val t = i.toDouble() / sampleRate
        val phase = phaseStep * i
        val raw = when (wave) {
            Wave.Sine -> sin(phase)
            Wave.Triangle -> 2.0 / PI * kotlin.math.asin(sin(phase))
            Wave.Square -> if (sin(phase) >= 0) 1.0 else -1.0
            Wave.Saw -> 2.0 * (t * frequency - kotlin.math.floor(0.5 + t * frequency))
            Wave.Noise -> java.util.Random().nextDouble() * 2.0 - 1.0
        }
        // Exponential decay envelope (smooth off, no clicks).
        val envelope = kotlin.math.exp(-3.0 * t / seconds.coerceAtLeast(0.001f))
        val value = (raw * envelope * volume.coerceIn(0f, 1f) * Short.MAX_VALUE)
            .toInt().coerceIn(-32768, 32767)
        samples[i] = value.toShort()
    }
    return samples
}

/** 16-bit little-endian mono PCM for [SourceDataLine]. */
fun shortToBytes(samples: ShortArray): ByteArray {
    val bytes = ByteArray(samples.size * 2)
    for (i in samples.indices) {
        val v = samples[i].toInt()
        bytes[i * 2] = (v and 0xFF).toByte()
        bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
    }
    return bytes
}

/** Convert an int sample array (silence gaps) to PCM bytes. */
fun shortToBytes(samples: IntArray): ByteArray =
    shortToBytes(ShortArray(samples.size) { samples[it].toShort() })
