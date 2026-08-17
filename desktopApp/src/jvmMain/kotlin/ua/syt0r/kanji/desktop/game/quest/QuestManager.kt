package ua.syt0r.kanji.desktop.game.quest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Tracks every quest: availability comes from the prerequisite graph
 * (spec §23), progress comes from [QuestEvent]s, completion grants rewards
 * through [QuestRewardHandler] (implemented by the game session, which owns
 * locations/knowledge/travel).
 */
class QuestManager(
    private val quests: List<Quest>
) {

    /** Serializable snapshot persisted in the save file. */
    @Serializable
    data class QuestStateSnapshot(
        val progress: List<QuestProgress> = emptyList(),
        val completedQuestIds: List<String> = emptyList()
    )

    private val questById: Map<String, Quest> = quests.associateBy { it.id }

    /** Live progress, in quest definition order. */
    val progress = mutableStateListOf<QuestProgress>()

    private val completed = mutableSetOf<String>()

    /** Bumped when a quest completes so the UI can show the panel. */
    var pendingCompletion by mutableStateOf<Quest?>(null)
        private set

    /** Bumped when a quest becomes available. */
    var lastUpdate by mutableStateOf(0)

    var rewardHandler: QuestRewardHandler? = null

    val allQuests: List<Quest> get() = quests

    fun quest(id: String): Quest? = questById[id]

    fun progressFor(id: String): QuestProgress? = progress.firstOrNull { it.questId == id }

    fun isComplete(id: String): Boolean = id in completed

    fun activeQuests(): List<Quest> =
        progress.filter { it.state == QuestState.Active }.mapNotNull { questById[it.questId] }

    fun availableQuests(): List<Quest> =
        progress.filter { it.state == QuestState.Available }.mapNotNull { questById[it.questId] }

    fun initProgress() {
        progress.clear()
        for (quest in quests) {
            val state = if (quest.prerequisites.isEmpty()) QuestState.Available else QuestState.Locked
            progress.add(
                QuestProgress(
                    questId = quest.id,
                    state = state,
                    objectives = quest.objectives.map {
                        ObjectiveProgress(objectiveId = it.id, target = it.count)
                    }
                )
            )
        }
    }

    /**
     * Unlock quests whose prerequisites are now met. Called after every
     * completion and on load.
     */
    fun refreshAvailability() {
        for (entry in progress) {
            val quest = questById[entry.questId] ?: continue
            if (entry.state != QuestState.Locked) continue
            if (quest.prerequisites.all { it in completed }) {
                entry.state = QuestState.Available
                lastUpdate++
            }
        }
    }

    /** Start a quest (mark Active). Returns false if not available. */
    fun start(questId: String): Boolean {
        val entry = progressFor(questId) ?: return false
        if (entry.state != QuestState.Available) return false
        entry.state = QuestState.Active
        lastUpdate++
        return true
    }

    /** Feed a world event into every active quest. */
    fun reportEvent(event: QuestEvent) {
        for (entry in progress) {
            if (entry.state != QuestState.Active) continue
            val quest = questById[entry.questId] ?: continue
            val matched = quest.objectives.filter { objective -> matches(objective, event) }
            for (objective in matched) {
                val objProgress = entry.objectives.firstOrNull { it.objectiveId == objective.id } ?: continue
                if (!objProgress.complete) {
                    objProgress.current = (objProgress.current + 1).coerceAtMost(objProgress.target)
                    if (objProgress.current >= objProgress.target) objProgress.complete = true
                }
            }
            if (entry.objectives.all { it.complete }) {
                completeQuest(quest, entry)
            }
        }
    }

    private fun matches(objective: QuestObjective, event: QuestEvent): Boolean = when (objective.kind) {
        ObjectiveKind.ReachLocation ->
            event is QuestEvent.ReachLocation && event.locationId == objective.targetId
        ObjectiveKind.DiscoverLocation ->
            event is QuestEvent.DiscoverLocation && event.locationId == objective.targetId
        ObjectiveKind.InteractObject ->
            event is QuestEvent.InteractObject && event.objectId == objective.targetId
        ObjectiveKind.TalkToNpc ->
            event is QuestEvent.TalkToNpc && event.npcId == objective.targetId
        ObjectiveKind.ReadSign ->
            event is QuestEvent.ReadSign && event.objectId == objective.targetId
        ObjectiveKind.TakePhoto ->
            event is QuestEvent.TakePhoto && objective.targetId.isBlank() ||
                event is QuestEvent.TakePhoto && event.count >= 1
        ObjectiveKind.LearnWord ->
            event is QuestEvent.LearnWord && event.knowledgeId == objective.targetId
        ObjectiveKind.BuyItem ->
            event is QuestEvent.BuyItem && event.itemId == objective.targetId
        ObjectiveKind.OrderFood ->
            event is QuestEvent.OrderFood &&
                (objective.targetId.isBlank() ||
                    event.itemId == objective.targetId || event.stallId == objective.targetId)
        ObjectiveKind.Season ->
            event is QuestEvent.SeasonChange &&
                event.season.name.equals(objective.targetId, ignoreCase = true)
        ObjectiveKind.Weather ->
            event is QuestEvent.WeatherChange &&
                event.weather.name.equals(objective.targetId, ignoreCase = true)
        ObjectiveKind.RideTrain ->
            event is QuestEvent.RideTrain && event.stationId == objective.targetId
        ObjectiveKind.Listen ->
            event is QuestEvent.Listen && event.lineId == objective.targetId
        ObjectiveKind.WriteKana ->
            event is QuestEvent.WriteKana && event.knowledgeId == objective.targetId
        ObjectiveKind.Collect ->
            event is QuestEvent.CollectItem &&
                (objective.targetId.isBlank() || event.itemId == objective.targetId)
        ObjectiveKind.Custom ->
            event is QuestEvent.Custom && event.key == objective.targetId
    }

    private fun completeQuest(quest: Quest, entry: QuestProgress) {
        entry.state = QuestState.Complete
        entry.completedAt = Clock.System.now().toString()
        completed.add(quest.id)
        lastUpdate++
        pendingCompletion = quest
        rewardHandler?.onQuestComplete(quest)
        refreshAvailability()
    }

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------

    fun snapshot(): QuestStateSnapshot =
        QuestStateSnapshot(progress = progress.toList(), completedQuestIds = completed.sorted())

    fun restore(snapshot: QuestStateSnapshot) {
        progress.clear()
        progress.addAll(snapshot.progress)
        completed.clear()
        completed.addAll(snapshot.completedQuestIds)
        // Re-lock anything whose state was reset by an older save.
        refreshAvailability()
    }
}

/**
 * Where completed-quest rewards land. The game session implements this so
 * rewards (locations, knowledge, travel unlocks, items, cosmetics) apply
 * through the real systems — never through quest-internal state.
 */
fun interface QuestRewardHandler {
    fun onQuestComplete(quest: Quest)
}
