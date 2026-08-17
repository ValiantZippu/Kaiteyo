package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.audio.SeasonAudio
import ua.syt0r.kanji.desktop.game.bridge.BridgeLookup
import ua.syt0r.kanji.desktop.game.bridge.BridgeMinePayload
import ua.syt0r.kanji.desktop.game.bridge.BridgeToastKind
import ua.syt0r.kanji.desktop.game.bridge.GameActivityKind
import ua.syt0r.kanji.desktop.game.bridge.GameBridge
import ua.syt0r.kanji.desktop.game.content.WorldContentLoader
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.time.Season
import ua.syt0r.kanji.desktop.game.time.toSeasonAudio
import java.io.File

/**
 * Chained NPC routes (spec §39, §52), seasonal audio (spec §42, §91-92),
 * the summer/autumn seasonal events, the debug teleport (spec §121) and
 * the quest-log category data (spec §21).
 */
class RoutesSeasonalAudioTest {

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
        "kaiteyo-test-routes-${System.nanoTime()}"
    )

    private fun newSession(bridge: GameBridge = FakeBridge()): GameSession =
        GameSession(bridge, WorldContentLoader.load(), saveDir)

    // ------------------------------------------------------------
    // Chained NPC routes (spec §39, §52)
    // ------------------------------------------------------------

    @Test
    fun `chained route npc walks the active leg and pauses`() {
        val session = newSession()
        // Evening in summer: the bon dancer is out with a chained route.
        session.debugSetTime(18 * 60)
        session.debugForceSeason(Season.Summer)
        val dancer = session.npcDirector.npc("bon-dancer")
        assertNotNull(dancer, "bon dancer appears on summer evenings")

        val start = dancer!!.entity.position
        val leg = dancer.activeRoute(session.clock.minuteOfDay)
        assertNotNull(leg, "evening has an active route leg")
        assertEquals(3, leg.points.size)

        // The dancer spawns at the first route point, pauses there (3 s),
        // then walks on — tick past the pause and confirm real movement.
        var moved = false
        for (i in 0 until 300) { // 10 s at fixed dt
            session.tick(1f / 30f)
            if (dancer.entity.position.distanceTo(start) > 1f) {
                moved = true
                break
            }
        }
        assertTrue(moved, "dancer walks, not teleports")
    }

    @Test
    fun `chained route npc despawns outside its window`() {
        val session = newSession()
        session.debugForceSeason(Season.Summer)
        // Midday: the dancer's evening-only route window is closed.
        session.debugSetTime(12 * 60)
        assertNull(session.npcDirector.npc("bon-dancer"), "no dancer at midday")
        // Re-open the window: he returns when evening comes.
        session.debugSetTime(18 * 60)
        assertNotNull(session.npcDirector.npc("bon-dancer"), "dancer returns in the evening")
    }

    @Test
    fun `autumn leaf watcher appears in autumn only`() {
        val session = newSession()
        session.debugForceSeason(Season.Autumn)
        session.debugSetTime(10 * 60)
        assertNotNull(session.npcDirector.npc("momiji-sensei"), "leaf watcher in autumn")
        session.debugForceSeason(Season.Summer)
        assertNull(session.npcDirector.npc("momiji-sensei"), "leaf watcher gone in summer")
    }

    // ------------------------------------------------------------
    // Seasonal audio (spec §42, §91-92)
    // ------------------------------------------------------------

    @Test
    fun `season maps to the audio season layer`() {
        val session = newSession()
        assertEquals(SeasonAudio.Summer, session.seasons.current.toSeasonAudio)
        session.debugForceSeason(Season.Winter)
        assertEquals(SeasonAudio.Winter, session.seasons.current.toSeasonAudio)
        session.debugForceSeason(Season.Autumn)
        assertEquals(SeasonAudio.Autumn, session.seasons.current.toSeasonAudio)
        session.debugForceSeason(Season.Spring)
        assertEquals(SeasonAudio.Spring, session.seasons.current.toSeasonAudio)
    }

    // ------------------------------------------------------------
    // Summer + autumn events (spec §42)
    // ------------------------------------------------------------

    @Test
    fun `summer and autumn quests are authored with the right gates`() {
        val session = newSession()
        val summer = session.quests.quest("summer-festival")
        assertNotNull(summer, "summer festival quest authored")
        assertTrue(summer!!.objectives.any { it.kind.name == "Season" && it.targetId == "Summer" })

        val autumn = session.quests.quest("autumn-leaves")
        assertNotNull(autumn, "autumn leaves quest authored")
        assertTrue(autumn!!.objectives.any { it.kind.name == "Season" && it.targetId == "Autumn" })
        assertTrue(autumn.learningTargets.contains("momiji"))
        assertTrue(autumn.learningTargets.contains("kaki"))
    }

    @Test
    fun `seasonal knowledge nodes exist with sentences`() {
        val graph = WorldContentLoader.load().knowledgeGraph
        for (id in listOf("natsu", "aki", "momiji", "kaki", "bonodori")) {
            assertNotNull(graph.node(id), "knowledge node $id exists")
        }
        val momiji = graph.node("momiji")
        assertNotNull(momiji)
        assertEquals(2, momiji.difficulty)
    }

    // ------------------------------------------------------------
    // Debug teleport (spec §121)
    // ------------------------------------------------------------

    @Test
    fun `debug teleport moves the player`() {
        val session = newSession()
        session.debugTeleport(500f, 400f)
        assertEquals(500f, session.player.entity.position.x, 0.5f)
        assertEquals(400f, session.player.entity.position.y, 0.5f)
    }

    @Test
    fun `debug teleport to location moves to its anchor`() {
        val session = newSession()
        val beach = session.world.location("beach") ?: return
        session.debugTeleportToLocation("beach")
        assertEquals(beach.anchor.x, session.player.entity.position.x, 0.5f)
        assertEquals(beach.anchor.y, session.player.entity.position.y, 0.5f)
    }

    // ------------------------------------------------------------
    // Quest-log category data (spec §21)
    // ------------------------------------------------------------

    @Test
    fun `every authored quest has a category`() {
        val session = newSession()
        for (quest in session.quests.allQuests) {
            assertTrue(quest.category.name.isNotBlank(), "quest ${quest.id} has a category")
        }
        // The summer/autumn events land in distinct categories.
        assertEquals("Social", session.quests.quest("summer-festival")!!.category.name)
        assertEquals("Exploration", session.quests.quest("autumn-leaves")!!.category.name)
    }

    @Test
    fun `content validator accepts chained routes`() {
        val session = newSession()
        assertEquals(0, session.validator.errors.size, "no content errors")
        val dancer = session.npcDirector.definitions.firstOrNull { it.id == "bon-dancer" }
        assertNotNull(dancer, "bon dancer definition present")
        assertEquals(1, dancer!!.routes.size)
        assertEquals(3, dancer.routes.first().points.size)
    }
}
