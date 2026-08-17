package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.bridge.BridgeLookup
import ua.syt0r.kanji.desktop.game.bridge.BridgeMinePayload
import ua.syt0r.kanji.desktop.game.bridge.BridgeToastKind
import ua.syt0r.kanji.desktop.game.bridge.GameActivityKind
import ua.syt0r.kanji.desktop.game.bridge.GameBridge
import ua.syt0r.kanji.desktop.game.content.WorldContentLoader
import ua.syt0r.kanji.desktop.game.engine.EntityManager
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.interaction.InteractionResult
import ua.syt0r.kanji.desktop.game.npc.NpcDefinition
import ua.syt0r.kanji.desktop.game.npc.NpcDirector
import ua.syt0r.kanji.desktop.game.quest.QuestEvent
import ua.syt0r.kanji.desktop.game.quest.QuestManager
import ua.syt0r.kanji.desktop.game.quest.ObjectiveKind
import ua.syt0r.kanji.desktop.game.quest.Quest
import ua.syt0r.kanji.desktop.game.quest.QuestObjective
import ua.syt0r.kanji.desktop.game.time.Season
import ua.syt0r.kanji.desktop.game.time.SeasonSystem
import ua.syt0r.kanji.desktop.game.time.WorldClock
import ua.syt0r.kanji.desktop.game.world.WorldPoint
import java.io.File

/**
 * NPC patrols (spec §39), weather-gated presence (spec §41), the season
 * cycle (spec §42) and the Kamakura night quest chain — pure logic + session
 * wiring, no AppState.
 */
class PatrolWeatherSeasonTest {

    private class FakeBridge : GameBridge {
        val toasts = mutableListOf<String>()
        override fun lookup(headword: String): BridgeLookup? = null
        override fun hasStudyMaterialFor(headword: String): Boolean = false
        override fun mine(payload: BridgeMinePayload): Boolean = true
        override fun recordActivity(kind: GameActivityKind, detail: String) {}
        override fun toast(message: String, kind: BridgeToastKind) {
            toasts.add(message)
        }
        override fun getSetting(key: String, default: String): String = default
        override fun setSetting(key: String, value: String) {}
    }

    private val saveDir: File = File(
        System.getProperty("java.io.tmpdir"),
        "kaiteyo-test-patrol-${System.nanoTime()}"
    )

    private fun newSession(bridge: GameBridge = FakeBridge()): GameSession =
        GameSession(bridge, WorldContentLoader.load(), saveDir)

    // ------------------------------------------------------------
    // NPC patrols (spec §39)
    // ------------------------------------------------------------

    @Test
    fun `patrol npc walks the loop point by point with pauses`() {
        val definition = NpcDefinition(
            id = "roamer",
            name = "Roamer",
            cellId = "town",
            anchor = WorldPoint(0f, 0f),
            patrolPoints = listOf(WorldPoint(50f, 0f), WorldPoint(0f, 50f)),
            patrolPauseSeconds = 1f
        )
        val director = NpcDirector(listOf(definition))
        val entities = EntityManager()
        director.spawn(entities, 9 * 60)
        val npc = director.npc("roamer")!!

        // Walk to the first patrol point (takes a while at 26 u/s).
        repeat(200) { director.tick(entities, 12 * 60, 1f / 60f) }
        assertTrue(npc.entity.position.distanceTo(Vec2(50f, 0f)) <= 4f, "should reach point 1")
        // Pausing at point 1 — not moving.
        val paused = npc.entity.position
        repeat(30) { director.tick(entities, 12 * 60, 1f / 60f) }
        assertTrue(npc.entity.position.distanceTo(paused) < 1f, "pauses before next leg")
        // Then walks to point 2.
        repeat(300) { director.tick(entities, 12 * 60, 1f / 60f) }
        assertTrue(npc.entity.position.distanceTo(Vec2(0f, 50f)) <= 4f, "should reach point 2")
    }

    // ------------------------------------------------------------
    // Weather gating (spec §41)
    // ------------------------------------------------------------

    @Test
    fun `rain-only npc is absent in the sun and present in the rain`() {
        val definition = NpcDefinition(
            id = "rain-guy",
            name = "Rain Guy",
            cellId = "town",
            anchor = WorldPoint(10f, 10f),
            weatherPhases = listOf("Rain")
        )
        val director = NpcDirector(listOf(definition))
        val entities = EntityManager()
        director.spawn(entities, 9 * 60)
        // Spawns gated by weather at tick time.
        director.tick(entities, 12 * 60, 1f / 60f, weatherKind = "Sun")
        assertEquals(null, director.npc("rain-guy"), "absent in the sun")

        director.tick(entities, 12 * 60, 1f / 60f, weatherKind = "Rain")
        assertNotNull(director.npc("rain-guy"), "appears in the rain")

        director.tick(entities, 12 * 60, 1f / 60f, weatherKind = "Sun")
        assertEquals(null, director.npc("rain-guy"), "leaves when it clears")
    }

    @Test
    fun `winter-only npc respects the season`() {
        val definition = NpcDefinition(
            id = "winter-guy",
            name = "Winter Guy",
            cellId = "town",
            anchor = WorldPoint(10f, 10f),
            seasons = listOf("Winter")
        )
        val director = NpcDirector(listOf(definition))
        val entities = EntityManager()
        director.spawn(entities, 9 * 60)
        director.tick(entities, 12 * 60, 1f / 60f, seasonKind = "Summer")
        assertEquals(null, director.npc("winter-guy"))

        director.tick(entities, 12 * 60, 1f / 60f, seasonKind = "Winter")
        assertNotNull(director.npc("winter-guy"))
    }

    // ------------------------------------------------------------
    // Season cycle (spec §42)
    // ------------------------------------------------------------

    @Test
    fun `season advances with the day counter and wraps`() {
        val clock = WorldClock(day = 1)
        val seasons = SeasonSystem(clock, cycleDays = 3)
        seasons.sync()
        assertEquals(Season.Summer, seasons.current)

        clock.day = 4
        seasons.sync()
        assertEquals(Season.Autumn, seasons.current)

        clock.day = 13 // 3*4=12-day cycle → day 13 is Summer again
        seasons.sync()
        assertEquals(Season.Summer, seasons.current)
    }

    @Test
    fun `seasonal weather leans toward the season`() {
        val clock = WorldClock(day = 1)
        val seasons = SeasonSystem(clock, cycleDays = 3)
        seasons.sync()
        assertEquals("Sun", seasons.seasonalWeather().name)
        clock.day = 10 // Winter
        seasons.sync()
        assertEquals("Snow", seasons.seasonalWeather().name)
    }

    // ------------------------------------------------------------
    // Quest objective kinds (spec §41-42)
    // ------------------------------------------------------------

    @Test
    fun `season and weather objectives complete on events`() {
        val quest = Quest(
            id = "q",
            title = "q",
            objectives = listOf(
                QuestObjective("o1", ObjectiveKind.Season, targetId = "Winter"),
                QuestObjective("o2", ObjectiveKind.Weather, targetId = "Snow")
            )
        )
        val manager = QuestManager(listOf(quest))
        manager.initProgress()
        manager.start("q")

        manager.reportEvent(QuestEvent.WeatherChange(ua.syt0r.kanji.desktop.game.time.WeatherKind.Snow))
        assertTrue(manager.progressFor("q")!!.objectives[1].complete)
        assertFalse(manager.progressFor("q")!!.objectives[0].complete)

        manager.reportEvent(QuestEvent.SeasonChange(Season.Winter))
        assertTrue(manager.progressFor("q")!!.objectives[0].complete)
        assertTrue(manager.quest("q")?.let { manager.isComplete(it.id) } == true)
    }

    // ------------------------------------------------------------
    // Kamakura night (spec §40, content)
    // ------------------------------------------------------------

    @Test
    fun `lantern keeper appears at night in kamakura and grants the quest`() {
        val session = newSession()
        // Evening — the keeper's window opens at 17:00.
        session.clock.minuteOfDay = 18 * 60
        session.tick(1f / 60f)

        // Travel to Kamakura: simulate the unlocked train and the arrival
        // chain already being done (the night quest gates on it).
        session.state.worldState = session.state.worldState.copy(
            flags = session.state.worldState.flags + ("travel:kamakura" to "true")
        )
        session.quests.start("station-travel")
        session.quests.progressFor("station-travel")!!.objectives.forEach {
            session.quests.reportEvent(QuestEvent.Custom("station-travel"))
            it.complete = true
        }
        session.quests.refreshAvailability()
        session.quests.start("kamakura-arrival")
        session.quests.progressFor("kamakura-arrival")!!.objectives.forEach {
            session.quests.reportEvent(QuestEvent.Custom("kamakura-arrival"))
            it.complete = true
        }
        session.quests.refreshAvailability()

        session.travelTo("kamakura")
        session.clock.minuteOfDay = 19 * 60
        session.tick(1f / 60f)

        val sakura = session.npcDirector.npc("sakura")
        assertNotNull(sakura, "Lantern Keeper should be present at night in Kamakura")
        session.player.entity.teleport(sakura.entity.position + Vec2(0f, 60f))
        session.handleInteraction(InteractionResult.StartDialogue("sakura"))
        assertTrue(session.dialogue.isActive)
        // Her first line grants the quest (prerequisite chain complete).
        assertTrue(session.quests.quest("kamakura-night-lanterns") != null)
        assertTrue(session.quests.availableQuests().any { it.id == "kamakura-night-lanterns" })
    }
}
