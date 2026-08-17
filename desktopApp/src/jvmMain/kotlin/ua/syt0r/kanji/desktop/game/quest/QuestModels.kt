package ua.syt0r.kanji.desktop.game.quest

import kotlinx.serialization.Serializable

// ============================================================
// QUEST SYSTEM (spec §20-24, §119)
// Quests are data — authored in JSON with real-world-like tasks
// ("Find the station", "Buy a drink"), not XP-grind chores.
// ============================================================

@Serializable
enum class QuestCategory(val label: String) {
    Exploration("Exploration"),
    Vocabulary("Vocabulary"),
    Kanji("Kanji"),
    Grammar("Grammar"),
    Listening("Listening"),
    Reading("Reading"),
    Speaking("Speaking"),
    Writing("Writing"),
    Photography("Photography"),
    Collection("Collection"),
    Story("Story"),
    Social("Social"),
    Travel("Travel")
}

@Serializable
data class Quest(
    val id: String,
    val title: String,
    val titleJp: String = "",
    val description: String = "",
    val category: QuestCategory = QuestCategory.Exploration,
    val level: Int = 1,
    /** World-node ids that must be completed first (dependency graph, §23). */
    val prerequisites: List<String> = emptyList(),
    val objectives: List<QuestObjective> = emptyList(),
    val rewards: QuestRewards = QuestRewards(),
    val learningTargets: List<String> = emptyList(),
    val locationId: String? = null,
    val dialogueId: String? = null,
    val storyId: String? = null
)

@Serializable
data class QuestObjective(
    val id: String,
    val kind: ObjectiveKind,
    /** Target id: location, object, npc, knowledge node, item, count… */
    val targetId: String = "",
    val count: Int = 1,
    val description: String = "",
    val descriptionJp: String = "",
    /** Optional Japanese text the player must read/understand to complete it. */
    val jpHint: String = ""
)

@Serializable
enum class ObjectiveKind {
    ReachLocation, InteractObject, TalkToNpc, ReadSign, TakePhoto,
    LearnWord, BuyItem, OrderFood, RideTrain, DiscoverLocation, Listen, WriteKana,
    /** Completed when the world enters a season/weather (spec §41-42). */
    Season, Weather, Custom,
    /**
     * Collect N of an item (blank targetId = any item) — collection quests
     * (spec §8). Progresses via [QuestEvent.CollectItem].
     */
    Collect
}

@Serializable
data class QuestRewards(
    val xp: Int = 0,
    val items: List<String> = emptyList(),
    /** Node ids granted on completion (locations, travel, stories…). */
    val unlocks: List<String> = emptyList(),
    val knowledge: List<String> = emptyList(),
    val cosmetics: List<String> = emptyList(),
    val stamps: List<String> = emptyList()
)

/** Lifecycle of one quest instance. */
@Serializable
enum class QuestState { Locked, Available, Active, Complete }

/** Progress of one objective inside an active quest. */
@Serializable
data class ObjectiveProgress(
    val objectiveId: String,
    var current: Int = 0,
    val target: Int = 1,
    var complete: Boolean = false
)

@Serializable
data class QuestProgress(
    val questId: String,
    var state: QuestState = QuestState.Locked,
    val objectives: List<ObjectiveProgress> = emptyList(),
    var completedAt: String? = null
)

/**
 * World events that progress objectives. Systems fire these; the
 * [QuestManager] decides which active quests care.
 */
sealed interface QuestEvent {
    data class ReachLocation(val locationId: String) : QuestEvent
    data class InteractObject(val objectId: String) : QuestEvent
    data class TalkToNpc(val npcId: String) : QuestEvent
    data class ReadSign(val objectId: String) : QuestEvent
    data class TakePhoto(val count: Int = 1) : QuestEvent
    data class LearnWord(val knowledgeId: String) : QuestEvent
    data class BuyItem(val itemId: String) : QuestEvent
    data class RideTrain(val stationId: String) : QuestEvent
    data class DiscoverLocation(val locationId: String) : QuestEvent
    /** An item was ordered from a stall's Japanese menu (spec §56). */
    data class OrderFood(val itemId: String, val stallId: String) : QuestEvent

    /** The world entered a new season (spec §42). */
    data class SeasonChange(val season: ua.syt0r.kanji.desktop.game.time.Season) : QuestEvent

    /** The weather changed (spec §41). */
    data class WeatherChange(val weather: ua.syt0r.kanji.desktop.game.time.WeatherKind) : QuestEvent

    /** A dialogue line was presented (listening practice, spec §61). */
    data class Listen(val lineId: String) : QuestEvent
    /** A kana trace passed the writing desk (spec §57-59). */
    data class WriteKana(val knowledgeId: String) : QuestEvent
    /** An item was added to the player's inventory (collection quests, §8). */
    data class CollectItem(val itemId: String) : QuestEvent
    data class Custom(val key: String) : QuestEvent
}
