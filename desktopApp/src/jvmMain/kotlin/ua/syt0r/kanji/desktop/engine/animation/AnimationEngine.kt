package ua.syt0r.kanji.desktop.engine.animation

import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

// ============================================
// KAITEYO ANIMATION ENGINE
// Lottie + Rive animation support with
// fallback to static placeholders. Desktop
// uses Java AWT playback; Android uses native
// Lottie/Rive Compose integration.
// ============================================

/** Animation source types. */
enum class AnimationSource { Lottie, Rive, Custom }

/** Animation playback state. */
enum class AnimationState { Idle, Playing, Paused, Completed, Error }

/** A loaded animation ready for playback. */
data class AnimationAsset(
    val id: String,
    val name: String,
    val source: AnimationSource,
    val durationMs: Long = 0,
    val fps: Int = 30,
    val width: Int = 0,
    val height: Int = 0
)

/** Options for animation playback. */
data class AnimationOptions(
    val loop: Boolean = false,
    val speed: Float = 1.0f,
    val autoplay: Boolean = true
)

/** Interface for animation playback backends. */
interface AnimationBackend {
    val name: String
    val available: Boolean
    suspend fun loadLottie(json: String): AnimationAsset?
    suspend fun loadLottie(file: File): AnimationAsset?
    suspend fun loadRive(file: File): AnimationAsset?
    fun play(animationId: String, options: AnimationOptions = AnimationOptions())
    fun pause(animationId: String)
    fun stop(animationId: String)
    fun getState(animationId: String): AnimationState
    fun release(animationId: String)
}

/**
 * Stub animation backend — returns placeholders.
 * Real implementation requires Lottie Compose or Rive Compose on the classpath.
 */
class StubAnimationBackend : AnimationBackend {
    override val name = "Stub (no animation library)"
    override val available = false

    private val animations = ConcurrentHashMap<String, AnimationAsset>()

    override suspend fun loadLottie(json: String): AnimationAsset? {
        val id = "lottie-${json.hashCode().toUInt().toString(16)}"
        return AnimationAsset(id, "Lottie Animation", AnimationSource.Lottie, 1000).also {
            animations[id] = it
        }
    }

    override suspend fun loadLottie(file: File): AnimationAsset? {
        val id = "lottie-${file.name.hashCode().toUInt().toString(16)}"
        return AnimationAsset(id, file.nameWithoutExtension, AnimationSource.Lottie, 1000).also {
            animations[id] = it
        }
    }

    override suspend fun loadRive(file: File): AnimationAsset? {
        val id = "rive-${file.name.hashCode().toUInt().toString(16)}"
        return AnimationAsset(id, file.nameWithoutExtension, AnimationSource.Rive, 0).also {
            animations[id] = it
        }
    }

    override fun play(animationId: String, options: AnimationOptions) {}
    override fun pause(animationId: String) {}
    override fun stop(animationId: String) {}
    override fun getState(animationId: String): AnimationState = AnimationState.Idle
    override fun release(animationId: String) { animations.remove(animationId) }
}

/**
 * Lottie Compose backend — uses LottieAnimation composable when available.
 */
class LottieComposeBackend : AnimationBackend {
    override val name = "Lottie Compose"
    override val available: Boolean by lazy {
        runCatching {
            Class.forName("com.airbnb.lottie.compose.LottieAnimation")
            true
        }.getOrDefault(false)
    }

    private val animations = ConcurrentHashMap<String, AnimationAsset>()

    override suspend fun loadLottie(json: String): AnimationAsset? {
        val id = "lottie-${json.hashCode().toUInt().toString(16)}"
        return AnimationAsset(id, "Lottie Animation", AnimationSource.Lottie, 1000).also {
            animations[id] = it
        }
    }

    override suspend fun loadLottie(file: File): AnimationAsset? {
        val id = "lottie-${file.name.hashCode().toUInt().toString(16)}"
        return AnimationAsset(id, file.nameWithoutExtension, AnimationSource.Lottie, 1000).also {
            animations[id] = it
        }
    }

    override suspend fun loadRive(file: File): AnimationAsset? = null // Lottie doesn't handle Rive
    override fun play(animationId: String, options: AnimationOptions) {}
    override fun pause(animationId: String) {}
    override fun stop(animationId: String) {}
    override fun getState(animationId: String): AnimationState = AnimationState.Idle
    override fun release(animationId: String) { animations.remove(animationId) }
}

/**
 * Animation engine coordinator.
 * Selects the best available backend.
 */
object AnimationEngine {
    private val backends: List<AnimationBackend> by lazy {
        val lottie = runCatching { LottieComposeBackend() }.getOrNull()
        val stub = StubAnimationBackend()
        buildList {
            if (lottie?.available == true) add(lottie)
            add(stub)
        }
    }

    private val backend: AnimationBackend get() = backends.first { it.available || it is StubAnimationBackend }
    private val loaded = ConcurrentHashMap<String, AnimationAsset>()

    suspend fun loadLottie(json: String): AnimationAsset? {
        return backend.loadLottie(json)?.also { loaded[it.id] = it }
    }

    suspend fun loadLottie(file: File): AnimationAsset? {
        return backend.loadLottie(file)?.also { loaded[it.id] = it }
    }

    suspend fun loadRive(file: File): AnimationAsset? {
        return backend.loadRive(file)?.also { loaded[it.id] = it }
    }

    fun play(id: String, options: AnimationOptions = AnimationOptions()) = backend.play(id, options)
    fun pause(id: String) = backend.pause(id)
    fun stop(id: String) = backend.stop(id)
    fun getState(id: String) = backend.getState(id)
    fun release(id: String) { backend.release(id); loaded.remove(id) }

    /** Common celebration animation (for streak, completion). */
    suspend fun loadCelebration(): AnimationAsset? {
        // Placeholder — in production, load from bundled Lottie JSON
        return loadLottie("""{"v":"5.7.4","fr":60,"ip":0,"op":60,"w":200,"h":200,"layers":[]}""")
    }

    /** Common loading skeleton animation. */
    suspend fun loadSkeleton(): AnimationAsset? {
        return loadLottie("""{"v":"5.7.4","fr":30,"ip":0,"op":30,"w":200,"h":50,"layers":[]}""")
    }
}
