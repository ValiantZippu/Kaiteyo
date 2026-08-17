package ua.syt0r.kanji.desktop.game.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * Developer tools (spec §121-122) — dev-only, never exposed to players.
 * Teleport, unlock-all, complete-quest, spawn/inspect objects, and an
 * optional debug overlay (FPS, frame time, loaded cells, entities, active
 * quests, current location).
 */
class DebugTools {

    var enabled by mutableStateOf(false)

    // ------------------------------------------------------------
    // Overlay data (updated each frame)
    // ------------------------------------------------------------
    var fps by mutableStateOf(0f)
        private set
    var frameTimeMs by mutableStateOf(0f)
        private set
    var loadedCells by mutableStateOf(0)
        private set
    var entityCount by mutableStateOf(0)
        private set
    var drawCalls by mutableStateOf(0)
        private set
    var currentCellId by mutableStateOf("")
        private set
    var currentRegionId by mutableStateOf("")
        private set
    var activeQuests by mutableStateOf(0)
        private set

    // ------------------------------------------------------------
    // Frame-time profiling (spec §94 — measure, then optimize)
    // ------------------------------------------------------------
    /** Rolling window of recent frame times (ms), newest last. */
    private val frameTimeWindow = ArrayDeque<Float>()

    /** Snapshot of the rolling window (oldest first) for the sparkline. */
    val frameTimes: List<Float> get() = frameTimeWindow.toList()

    /** Average frame time over the window. */
    val avgFrameTimeMs: Float
        get() = frameTimeWindow.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size } ?: 0f

    /** Worst frame in the window. */
    val maxFrameTimeMs: Float
        get() = frameTimeWindow.maxOrNull() ?: 0f

    /** 95th-percentile frame time — the frame the player actually feels. */
    val p95FrameTimeMs: Float
        get() {
            if (frameTimeWindow.isEmpty()) return 0f
            val sorted = frameTimeWindow.sorted()
            val idx = (sorted.size * 0.95f).toInt().coerceIn(0, sorted.lastIndex)
            return sorted[idx]
        }

    var showNodeIds by mutableStateOf(false)
    var showInteractionBounds by mutableStateOf(false)
    var showCollision by mutableStateOf(false)

    fun updateFrame(
        fps: Float,
        frameTimeMs: Float,
        loadedCells: Int,
        entityCount: Int,
        cellId: String,
        regionId: String,
        activeQuests: Int
    ) {
        this.fps = fps
        this.frameTimeMs = frameTimeMs
        this.loadedCells = loadedCells
        this.entityCount = entityCount
        this.currentCellId = cellId
        this.currentRegionId = regionId
        this.activeQuests = activeQuests
        // Keep a fixed-size rolling window (60 Hz → 2 seconds of history).
        frameTimeWindow.addLast(frameTimeMs)
        while (frameTimeWindow.size > 120) frameTimeWindow.removeFirst()
    }

    fun toggle() {
        enabled = !enabled
    }
}

/** A development-only command console (teleport/unlock/etc.). */
class DevConsole {

    val history = mutableListOf<String>()

    fun run(command: String): String = when {
        command.startsWith("tp ") -> "TP: use the debug overlay (TODO: world teleport UI)"
        command == "unlockall" -> "Unlock-all is handled by the debug menu"
        command == "help" -> "help, tp <region:cell:x:y>, unlockall, quests, nodes, save, load"
        command == "quests" -> "List active quests — see the quest log panel"
        command == "save" -> "Manual save requested"
        command == "load" -> "Load last save requested"
        command == "nodes" -> "Toggle node-id display"
        else -> "Unknown command: $command"
    }
}
