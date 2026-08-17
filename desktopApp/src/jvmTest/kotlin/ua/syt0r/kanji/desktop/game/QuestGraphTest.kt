package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.quest.ObjectiveKind
import ua.syt0r.kanji.desktop.game.quest.Quest
import ua.syt0r.kanji.desktop.game.quest.QuestEvent
import ua.syt0r.kanji.desktop.game.quest.QuestManager
import ua.syt0r.kanji.desktop.game.quest.QuestObjective
import ua.syt0r.kanji.desktop.game.quest.QuestRewards
import ua.syt0r.kanji.desktop.game.quest.QuestState

class QuestGraphTest {

    private val firstQuest = Quest(
        id = "welcome",
        title = "Find the station",
        prerequisites = emptyList(),
        objectives = listOf(
            QuestObjective("o1", ObjectiveKind.ReachLocation, "station", 1, "Walk to the station"),
            QuestObjective("o2", ObjectiveKind.TalkToNpc, "ekicho", 1, "Talk to the station master")
        ),
        rewards = QuestRewards(xp = 40, unlocks = listOf("buy-drink"))
    )

    private val secondQuest = Quest(
        id = "buy-drink",
        title = "Buy a drink",
        prerequisites = listOf("welcome"),
        objectives = listOf(
            QuestObjective("o1", ObjectiveKind.BuyItem, "drink", 1, "Buy a drink")
        ),
        rewards = QuestRewards(xp = 30)
    )

    private fun manager(): QuestManager {
        val manager = QuestManager(listOf(firstQuest, secondQuest))
        manager.initProgress()
        return manager
    }

    @Test
    fun `roots are available, dependents are locked`() {
        val m = manager()
        assertEquals(QuestState.Available, m.progressFor("welcome")?.state)
        assertEquals(QuestState.Locked, m.progressFor("buy-drink")?.state)
    }

    @Test
    fun `completing a quest unlocks its dependents`() {
        val m = manager()
        val rewards = mutableListOf<Quest>()
        m.rewardHandler = QuestRewardHandler { rewards.add(it) }

        m.start("welcome")
        m.reportEvent(QuestEvent.ReachLocation("station"))
        m.reportEvent(QuestEvent.TalkToNpc("ekicho"))

        assertTrue(m.isComplete("welcome"))
        assertEquals(listOf("welcome"), rewards.map { it.id })
        // Availability refresh makes the dependent quest available.
        m.refreshAvailability()
        assertEquals(QuestState.Available, m.progressFor("buy-drink")?.state)
    }

    @Test
    fun `irrelevant events never progress objectives`() {
        val m = manager()
        m.start("welcome")
        m.reportEvent(QuestEvent.ReachLocation("beach"))
        m.reportEvent(QuestEvent.BuyItem("drink"))
        assertFalse(m.isComplete("welcome"))
    }

    @Test
    fun `counted objectives reach their target`() {
        val photoQuest = Quest(
            id = "photos",
            title = "Take three photos",
            prerequisites = emptyList(),
            objectives = listOf(
                QuestObjective("o1", ObjectiveKind.TakePhoto, "", 3, "Take 3 photos")
            )
        )
        val m = QuestManager(listOf(photoQuest))
        m.initProgress()
        m.start("photos")
        m.reportEvent(QuestEvent.TakePhoto(1))
        m.reportEvent(QuestEvent.TakePhoto(1))
        assertFalse(m.isComplete("photos"))
        m.reportEvent(QuestEvent.TakePhoto(1))
        assertTrue(m.isComplete("photos"))
    }

    @Test
    fun `word-learning objectives progress on real discoveries`() {
        val wordQuest = Quest(
            id = "learn",
            title = "Learn eki",
            prerequisites = emptyList(),
            objectives = listOf(
                QuestObjective("o1", ObjectiveKind.LearnWord, "eki", 1, "Learn 駅")
            )
        )
        val m = QuestManager(listOf(wordQuest))
        m.initProgress()
        m.start("learn")
        m.reportEvent(QuestEvent.LearnWord("eki"))
        assertTrue(m.isComplete("learn"))
    }

    @Test
    fun `snapshot and restore preserve progress`() {
        val m = manager()
        m.start("welcome")
        m.reportEvent(QuestEvent.ReachLocation("station"))
        val snapshot = m.snapshot()

        val restored = manager()
        restored.restore(snapshot)
        val progress = restored.progressFor("welcome")
        assertEquals(QuestState.Active, progress?.state)
        assertTrue(progress?.objectives?.firstOrNull { it.objectiveId == "o1" }?.complete == true)
        assertFalse(progress?.objectives?.firstOrNull { it.objectiveId == "o2" }?.complete == true)
    }
}
