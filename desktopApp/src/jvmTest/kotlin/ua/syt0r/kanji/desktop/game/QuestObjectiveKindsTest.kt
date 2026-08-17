package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.quest.ObjectiveKind
import ua.syt0r.kanji.desktop.game.quest.Quest
import ua.syt0r.kanji.desktop.game.quest.QuestEvent
import ua.syt0r.kanji.desktop.game.quest.QuestManager
import ua.syt0r.kanji.desktop.game.quest.QuestObjective
import ua.syt0r.kanji.desktop.game.quest.QuestRewards

/**
 * New objective kinds (spec §21): writing a kana and listening to a line are
 * real, event-driven objectives — the same graph machinery as every quest.
 */
class QuestObjectiveKindsTest {

    private fun manager(): QuestManager {
        val quest = Quest(
            id = "q",
            title = "Test",
            objectives = listOf(
                QuestObjective(id = "w", kind = ObjectiveKind.WriteKana, targetId = "kana-a", count = 1, description = "Trace あ"),
                QuestObjective(id = "l", kind = ObjectiveKind.Listen, targetId = "tk1", count = 1, description = "Listen")
            ),
            rewards = QuestRewards(xp = 10)
        )
        val manager = QuestManager(listOf(quest))
        manager.initProgress()
        assertTrue(manager.start("q"))
        return manager
    }

    @Test
    fun `write kana objective completes on the matching trace`() {
        val manager = manager()
        manager.reportEvent(QuestEvent.WriteKana("kana-a"))
        val progress = manager.progressFor("q")!!
        assertTrue(progress.objectives.first { it.objectiveId == "w" }.complete)
        assertFalse(progress.objectives.first { it.objectiveId == "l" }.complete)
    }

    @Test
    fun `a trace of a different target does not complete it`() {
        val manager = manager()
        manager.reportEvent(QuestEvent.WriteKana("kana-i"))
        val progress = manager.progressFor("q")!!
        assertFalse(progress.objectives.first { it.objectiveId == "w" }.complete)
    }

    @Test
    fun `listen objective completes on the matching line`() {
        val manager = manager()
        manager.reportEvent(QuestEvent.Listen("tk1"))
        val progress = manager.progressFor("q")!!
        assertTrue(progress.objectives.first { it.objectiveId == "l" }.complete)
    }

    @Test
    fun `quest completes when every objective is met`() {
        val manager = manager()
        manager.reportEvent(QuestEvent.WriteKana("kana-a"))
        manager.reportEvent(QuestEvent.Listen("tk1"))
        assertTrue(manager.isComplete("q"))
    }

    @Test
    fun `collect objective counts any item when target is blank`() {
        val quest = Quest(
            id = "collect",
            title = "Collect drinks",
            objectives = listOf(
                QuestObjective(id = "c", kind = ObjectiveKind.Collect, targetId = "", count = 2, description = "Collect 2 drinks")
            ),
            rewards = QuestRewards(xp = 10)
        )
        val manager = QuestManager(listOf(quest))
        manager.initProgress()
        assertTrue(manager.start("collect"))
        manager.reportEvent(QuestEvent.CollectItem("DrinkWater"))
        assertFalse(manager.isComplete("collect"))
        manager.reportEvent(QuestEvent.CollectItem("DrinkTea"))
        assertTrue(manager.isComplete("collect"))
    }

    @Test
    fun `collect objective matches only the target item when specified`() {
        val quest = Quest(
            id = "collect-tea",
            title = "Collect tea",
            objectives = listOf(
                QuestObjective(id = "c", kind = ObjectiveKind.Collect, targetId = "DrinkTea", count = 1, description = "Collect tea")
            ),
            rewards = QuestRewards(xp = 10)
        )
        val manager = QuestManager(listOf(quest))
        manager.initProgress()
        assertTrue(manager.start("collect-tea"))
        manager.reportEvent(QuestEvent.CollectItem("DrinkWater"))
        assertFalse(manager.isComplete("collect-tea"))
        manager.reportEvent(QuestEvent.CollectItem("DrinkTea"))
        assertTrue(manager.isComplete("collect-tea"))
    }
}
