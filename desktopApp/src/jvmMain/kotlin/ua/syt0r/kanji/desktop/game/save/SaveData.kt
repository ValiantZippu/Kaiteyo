package ua.syt0r.kanji.desktop.game.save

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.game.collection.CollectionData
import ua.syt0r.kanji.desktop.game.learning.LearningManager.LearningState
import ua.syt0r.kanji.desktop.game.photography.AlbumData
import ua.syt0r.kanji.desktop.game.player.PlayerState
import ua.syt0r.kanji.desktop.game.quest.QuestManager.QuestStateSnapshot
import ua.syt0r.kanji.desktop.game.settings.GameSettings
import ua.syt0r.kanji.desktop.game.story.StoryProgress
import ua.syt0r.kanji.desktop.game.time.WeatherKind
import java.io.File

// ============================================================
// SAVE SYSTEM (spec §97-98, §117)
// Versioned save data with a migration chain — a game update can
// never destroy saves; old formats migrate forward. Game state is
// split into player/quest/knowledge/collection/world/settings.
// ============================================================

@Serializable
data class SaveData(
    val version: Int = SaveData.CURRENT_VERSION,
    val savedAt: String = "",
    val player: PlayerState = PlayerState(),
    val quests: QuestStateSnapshot = QuestStateSnapshot(),
    val knowledge: LearningState = LearningState(),
    val collection: CollectionData = CollectionData(),
    val world: WorldStateData = WorldStateData(),
    val story: List<StoryProgress> = emptyList(),
    val album: AlbumData = AlbumData(),
    val settings: GameSettings = GameSettings(),
    val stats: GameStatsData = GameStatsData()
) {
    companion object {
        /** Bump on breaking save-format changes; add a migrator for older. */
        const val CURRENT_VERSION = 1
    }
}

/** World-side state (spec §98). */
@Serializable
data class WorldStateData(
    val regionId: String = "hamanaka",
    val cellId: String = "hamanaka-town",
    val discoveredLocations: List<String> = emptyList(),
    val discoveredObjects: List<String> = emptyList(),
    val minuteOfDay: Int = 9 * 60,
    val day: Int = 1,
    val weather: WeatherKind = WeatherKind.Sun,
    val flags: Map<String, String> = emptyMap()
)

/** Honest, activity-gated statistics (spec §66-67). */
@Serializable
data class GameStatsData(
    val questsCompleted: Int = 0,
    val wordsEncountered: Int = 0,
    val photosTaken: Int = 0,
    val locationsDiscovered: Int = 0,
    /** Only real interaction time — never idle/AFK (spec §67). */
    val activeSeconds: Long = 0L,
    val xpEarned: Int = 0
)

/** Migrates an older save payload to the current version. */
object SaveMigrator {

    /**
     * Parse + migrate a raw save. Returns the decoded [SaveData] or null when
     * the payload is corrupt beyond repair (a fresh game starts instead).
     */
    fun load(raw: String): SaveData? = runCatching {
        val probe = Json { ignoreUnknownKeys = true }
            .decodeFromString<SaveProbe>(raw)
        var data = probe.version
        // Future: chain of migrators here, e.g. v1 -> v2. Every old format
        // must land on the current version before use (spec §117, §127).
        Json { ignoreUnknownKeys = true }.decodeFromString<SaveData>(raw)
    }.getOrNull()

    @Serializable
    private data class SaveProbe(val version: Int = SaveData.CURRENT_VERSION)
}

/**
 * Save slots under ~/.kaiteyo/game/saves/. The game autosaves to the active
 * slot; players can keep several journeys (one per region later).
 */
class SaveManager(
    baseDir: File = File(System.getProperty("user.home"), ".kaiteyo/game/saves")
) {
    private val dir: File = baseDir
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    init {
        dir.mkdirs()
    }

    fun save(data: SaveData, slot: String = "slot-1") {
        val file = File(dir, "$slot.json")
        runCatching {
            file.writeText(json.encodeToString(data.copy(savedAt = kotlinx.datetime.Clock.System.now().toString())))
        }
    }

    fun load(slot: String = "slot-1"): SaveData? {
        val file = File(dir, "$slot.json")
        if (!file.exists()) return null
        return SaveMigrator.load(file.readText())
    }

    fun listSlots(): List<String> =
        dir.listFiles { f -> f.extension == "json" }?.map { it.nameWithoutExtension }?.sorted() ?: emptyList()

    fun delete(slot: String) {
        File(dir, "$slot.json").delete()
    }
}
