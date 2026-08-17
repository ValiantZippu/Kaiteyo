package ua.syt0r.kanji.core.game

import kotlinx.serialization.json.Json
import org.koin.dsl.module
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================================
// GAME PROGRESS STORE
// Persists the user's explicit game state (manually completed
// nodes, mastered nodes, bonus XP) as JSON in the preferences
// layer — the same mechanism as review settings and statistics
// goals. Kanji auto-progress is NOT stored here; it is derived
// from the real SRS state every render, so the game can never
// disagree with what the user has actually studied.
// ============================================================

class GameProgressStore(
    private val appPreferences: PreferencesContract.AppPreferences
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): GameProgressData {
        val raw = appPreferences.gameProgressJson.get() ?: ""
        if (raw.isBlank()) return GameProgressData()
        return runCatching { json.decodeFromString<GameProgressData>(raw) }
            .getOrDefault(GameProgressData())
    }

    suspend fun save(data: GameProgressData) {
        appPreferences.gameProgressJson.set(json.encodeToString(GameProgressData.serializer(), data))
    }

    suspend fun completeNode(nodeId: String, current: GameProgressData): GameProgressData {
        val updated = current.copy(manualCompleted = current.manualCompleted + nodeId)
        save(updated)
        return updated
    }

    suspend fun uncompleteNode(nodeId: String, current: GameProgressData): GameProgressData {
        val updated = current.copy(
            manualCompleted = current.manualCompleted - nodeId,
            masteredNodes = current.masteredNodes - nodeId
        )
        save(updated)
        return updated
    }

    suspend fun toggleMastered(nodeId: String, current: GameProgressData): GameProgressData {
        val mastered = if (nodeId in current.masteredNodes) current.masteredNodes - nodeId
        else current.masteredNodes + nodeId
        val updated = current.copy(
            masteredNodes = mastered,
            manualCompleted = current.manualCompleted + nodeId
        )
        save(updated)
        return updated
    }
}

val gameModule = module {
    single { GameProgressStore(get()) }
}
