package ua.syt0r.kanji.core.game

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

// ============================================================
// GAME DOMAIN — a node-based curriculum that runs on top of
// the real Kaiteyo learning state.
//
// A node represents a chunk of content (kana, kanji or
// vocabulary). Kanji nodes auto-track against the user's SRS
// data: a node becomes InProgress once any of its kanji has
// been studied and Completed when all of them are mastered.
// Kana and vocabulary nodes are completed explicitly, with an
// honest label, because the app cannot cheaply prove "ever
// studied" for those content types yet.
//
// Everything here is pure Kotlin — no Compose, no I/O — so the
// evaluator is deterministic and unit-testable.
// ============================================================

enum class GameNodeKind { Kana, Kanji, Vocabulary }

enum class GameNodeState { Locked, Available, InProgress, Completed, Mastered }

/**
 * Where a node's kanji content comes from. Resolved at render time
 * against the live dictionary dataset so courses stay honest — a
 * "JLPT N5" node is exactly the N5 kanji in Kaiteyo's database.
 */
sealed interface KanjiSource {
    /** A fixed list carried by the node itself (Numbers, Directions, …). */
    data object Static : KanjiSource

    /** Kanji whose classification list contains this value ("n5", "o1", …). */
    data class Classification(val value: String) : KanjiSource

    /** Kanji whose frequency rank is at most [maxRank]. */
    data class TopFrequency(val maxRank: Int) : KanjiSource
}

data class GameNode(
    val id: String,
    val kind: GameNodeKind,
    val title: String,
    val subtitle: String,
    val description: String,
    val prerequisites: List<String> = emptyList(),
    /** Fixed content — used when [kanjiSource] is [KanjiSource.Static]. */
    val kanji: List<String> = emptyList(),
    /** Vocabulary classification ("n5"…"n3") for vocabulary nodes. */
    val vocabClassification: String? = null,
    val kanjiSource: KanjiSource = KanjiSource.Static,
    val xp: Int = 100
)

data class GameCourse(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val nodes: List<GameNode>
)

data class GameWorld(
    val id: String,
    val title: String,
    val subtitle: String,
    val courses: List<GameCourse>
)

/** Persisted user state: explicit completions, mastered nodes, extra XP. */
@Serializable
data class GameProgressData(
    val manualCompleted: Set<String> = emptySet(),
    val masteredNodes: Set<String> = emptySet(),
    val xpBonus: Long = 0
)

data class GameNodeResult(
    val node: GameNode,
    val state: GameNodeState,
    /** For kanji nodes: how many of its kanji the user has studied / mastered. */
    val studiedCount: Int = 0,
    val masteredCount: Int = 0,
    val totalCount: Int = 0
)

data class GameCourseResult(
    val course: GameCourse,
    val nodes: List<GameNodeResult>,
    val completed: Int,
    val total: Int
)

data class GameSnapshot(
    val courses: List<GameCourseResult>,
    val xp: Long,
    val level: Int,
    val rank: String,
    val xpIntoLevel: Long,
    val xpForNextLevel: Long,
    val completedNodes: Int,
    val totalNodes: Int,
    val studiedKanji: Int,
    val masteredKanji: Int
)

object GameEvaluator {

    fun evaluate(
        world: GameWorld,
        studiedKanji: Set<String>,
        masteredKanji: Set<String>,
        progress: GameProgressData
    ): GameSnapshot {
        val allNodes = world.courses.flatMap { it.nodes }
        val byId = allNodes.associateBy { it.id }

        fun nodeCompleted(node: GameNode): Boolean =
            node.id in progress.manualCompleted ||
                (node.kind == GameNodeKind.Kanji && node.kanji.isNotEmpty() && node.kanji.all { it in masteredKanji })

        fun nodeMastered(node: GameNode): Boolean =
            node.id in progress.masteredNodes ||
                (node.kind == GameNodeKind.Kanji && nodeCompleted(node))

        val stateCache = mutableMapOf<String, GameNodeState>()

        fun stateOf(id: String): GameNodeState = stateCache.getOrPut(id) {
            val node = byId[id] ?: return@getOrPut GameNodeState.Locked
            when {
                nodeMastered(node) -> GameNodeState.Mastered
                nodeCompleted(node) -> GameNodeState.Completed
                else -> {
                    val prerequisitesMet = node.prerequisites.all { prereq ->
                        val s = stateOf(prereq)
                        s == GameNodeState.Completed || s == GameNodeState.Mastered
                    }
                    if (!prerequisitesMet) {
                        GameNodeState.Locked
                    } else if (node.kind == GameNodeKind.Kanji && node.kanji.any { it in studiedKanji }) {
                        GameNodeState.InProgress
                    } else {
                        GameNodeState.Available
                    }
                }
            }
        }

        val courseResults = world.courses.map { course ->
            val nodeResults = course.nodes.map { node ->
                val studied = node.kanji.count { it in studiedKanji }
                val mastered = node.kanji.count { it in masteredKanji }
                GameNodeResult(
                    node = node,
                    state = stateOf(node.id),
                    studiedCount = studied,
                    masteredCount = mastered,
                    totalCount = node.kanji.size
                )
            }
            GameCourseResult(
                course = course,
                nodes = nodeResults,
                completed = nodeResults.count {
                    it.state == GameNodeState.Completed || it.state == GameNodeState.Mastered
                },
                total = nodeResults.size
            )
        }

        var xp = progress.xpBonus
        val completedNodes = courseResults.sumOf { it.completed }
        courseResults.forEach { course ->
            course.nodes.forEach { nodeResult ->
                when (nodeResult.state) {
                    GameNodeState.Completed -> xp += nodeResult.node.xp
                    GameNodeState.Mastered -> xp += nodeResult.node.xp * 2
                    else -> {}
                }
            }
        }

        // XP → level: level N requires N*(N+1)/2 * 100 cumulative XP.
        // Solved directly instead of iterating so the math is cheap and exact.
        val level = ((1 + sqrt(1 + 8 * (xp / 100.0))) / 2).toInt()
        val xpForNextLevel = 100L * (level + 1) * (level + 2) / 2 - 100L * level * (level + 1) / 2
        val xpAtLevelStart = 100L * level * (level + 1) / 2

        return GameSnapshot(
            courses = courseResults,
            xp = xp,
            level = level,
            rank = rankFor(level),
            xpIntoLevel = (xp - xpAtLevelStart).coerceAtLeast(0),
            xpForNextLevel = xpForNextLevel,
            completedNodes = completedNodes,
            totalNodes = allNodes.size,
            studiedKanji = studiedKanji.size,
            masteredKanji = masteredKanji.size
        )
    }

    fun rankFor(level: Int): String = when {
        level >= 15 -> "Sensei"
        level >= 11 -> "Master"
        level >= 8 -> "Scholar"
        level >= 5 -> "Explorer"
        level >= 3 -> "Learner"
        else -> "Beginner"
    }
}
