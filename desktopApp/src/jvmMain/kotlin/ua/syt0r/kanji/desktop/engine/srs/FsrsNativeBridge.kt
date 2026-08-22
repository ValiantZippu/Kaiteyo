package ua.syt0r.kanji.desktop.engine.srs

// ============================================
// KAITEYO FSRS-RS NATIVE BRIDGE
// Stub for future Rust-based FSRS implementation.
// When the card pool exceeds 100k, the Kotlin
// implementation can be swapped for a Rust/WASM
// version via this bridge. For now, delegates to
// the existing pure-Kotlin FSRS-5 scheduler.
// ============================================

/**
 * Interface for FSRS scheduling backends.
 * The default is the pure-Kotlin implementation.
 * A future FSRS-rs backend would call into a Rust
 * library via JNI (JVM) or WASM (iOS).
 */
interface FsrsBackend {
    val name: String
    val version: String
    val available: Boolean get() = true

    /** Schedule a card review. */
    fun schedule(
        difficulty: Float,
        stability: Float,
        elapsedDays: Int,
        rating: Int  // 1=Again, 2=Hard, 3=Good, 4=Easy
    ): FsrsScheduleResult

    /** Batch schedule multiple cards. */
    fun scheduleBatch(
        cards: List<FsrsCardInput>
    ): List<FsrsScheduleResult>

    /** Memory usage in bytes (for monitoring). */
    fun memoryUsage(): Long = 0
}

data class FsrsCardInput(
    val cardId: Long,
    val difficulty: Float,
    val stability: Float,
    val elapsedDays: Int,
    val rating: Int
)

data class FsrsScheduleResult(
    val cardId: Long = 0,
    val newDifficulty: Float,
    val newStability: Float,
    val nextReviewDays: Int,
    val interval: Int,
    val rating: Int
)

/**
 * Pure-Kotlin FSRS-5 backend — the current default.
 * This delegates to the existing FsrsScheduler in core.
 */
class KotlinFsrsBackend : FsrsBackend {
    override val name = "FSRS-5 (Kotlin)"
    override val version = "5.0"

    override fun schedule(
        difficulty: Float,
        stability: Float,
        elapsedDays: Int,
        rating: Int
    ): FsrsScheduleResult {
        // TODO: Wire to core FsrsScheduler
        val newStability = stability * when (rating) {
            1 -> 0.5f
            2 -> 0.8f
            3 -> 1.1f
            4 -> 1.5f
            else -> 1.0f
        }
        val interval = (newStability * 1.2f).toInt().coerceAtLeast(1)
        return FsrsScheduleResult(
            newDifficulty = (difficulty + (4 - rating) * 0.1f).coerceIn(1f, 10f),
            newStability = newStability,
            nextReviewDays = interval,
            interval = interval,
            rating = rating
        )
    }

    override fun scheduleBatch(cards: List<FsrsCardInput>): List<FsrsScheduleResult> {
        return cards.map { card ->
            schedule(card.difficulty, card.stability, card.elapsedDays, card.rating)
                .copy(cardId = card.cardId)
        }
    }
}

/**
 * Future FSRS-rs backend via JNI.
 * Requires libfsrs_rs.so / fsrs_rs.dll on the classpath.
 */
class FsrsRsBackend : FsrsBackend {
    override val name = "FSRS-rs (Rust)"
    override val version = "5.1"

    override val available: Boolean by lazy {
        runCatching {
            System.loadLibrary("fsrs_rs")
            true
        }.getOrDefault(false)
    }

    override fun schedule(
        difficulty: Float,
        stability: Float,
        elapsedDays: Int,
        rating: Int
    ): FsrsScheduleResult {
        if (!available) error("FSRS-rs native library not loaded")
        // JNI call would go here
        return KotlinFsrsBackend().schedule(difficulty, stability, elapsedDays, rating)
    }

    override fun scheduleBatch(cards: List<FsrsCardInput>): List<FsrsScheduleResult> {
        return cards.map { card ->
            schedule(card.difficulty, card.stability, card.elapsedDays, card.rating)
                .copy(cardId = card.cardId)
        }
    }
}

/**
 * Composite backend that selects the best available implementation.
 */
object FsrsNativeBridge {
    private val backends: List<FsrsBackend> by lazy {
        val rs = runCatching { FsrsRsBackend() }.getOrNull()
        val kotlin = KotlinFsrsBackend()
        buildList {
            if (rs?.available == true) add(rs)
            add(kotlin)
        }
    }

    val active: FsrsBackend get() = backends.first()

    fun schedule(
        difficulty: Float,
        stability: Float,
        elapsedDays: Int,
        rating: Int
    ): FsrsScheduleResult = active.schedule(difficulty, stability, elapsedDays, rating)
}
