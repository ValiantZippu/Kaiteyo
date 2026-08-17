package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.activity.OrderSession
import ua.syt0r.kanji.desktop.game.bridge.BridgeLookup
import ua.syt0r.kanji.desktop.game.bridge.BridgeMinePayload
import ua.syt0r.kanji.desktop.game.bridge.BridgePhoto
import ua.syt0r.kanji.desktop.game.bridge.BridgeToastKind
import ua.syt0r.kanji.desktop.game.bridge.GameActivityKind
import ua.syt0r.kanji.desktop.game.bridge.GameBridge
import ua.syt0r.kanji.desktop.game.content.WorldContentLoader
import ua.syt0r.kanji.desktop.game.engine.EntityManager
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.interaction.InteractionResult
import ua.syt0r.kanji.desktop.game.npc.NpcDefinition
import ua.syt0r.kanji.desktop.game.npc.NpcDirector
import ua.syt0r.kanji.desktop.game.npc.NpcScheduleEntry
import ua.syt0r.kanji.desktop.game.photography.Photo
import ua.syt0r.kanji.desktop.game.photography.PhotoCategory
import ua.syt0r.kanji.desktop.game.photography.PhotoTag
import ua.syt0r.kanji.desktop.game.quest.QuestEvent
import ua.syt0r.kanji.desktop.game.quest.QuestManager
import ua.syt0r.kanji.desktop.game.world.WorldPoint
import java.io.File

/**
 * NPC waypoint movement (spec §39, §52), the stall ordering minigame
 * (spec §56) and photo album actions (spec §43-46) — pure logic + session
 * wiring, no AppState.
 */
class NpcOrderPhotoTest {

    private class FakeBridge : GameBridge {
        val toasts = mutableListOf<String>()
        val savedPhotos = mutableListOf<BridgePhoto>()
        override fun lookup(headword: String): BridgeLookup? = null
        override fun hasStudyMaterialFor(headword: String): Boolean = false
        override fun mine(payload: BridgeMinePayload): Boolean = true
        override fun recordActivity(kind: GameActivityKind, detail: String) {}
        override fun savePhotoToDisk(photo: BridgePhoto): Boolean {
            savedPhotos.add(photo)
            return true
        }
        override fun toast(message: String, kind: BridgeToastKind) {
            toasts.add(message)
        }
        override fun getSetting(key: String, default: String): String = default
        override fun setSetting(key: String, value: String) {}
    }

    private val saveDir: File = File(
        System.getProperty("java.io.tmpdir"),
        "kaiteyo-test-npc-order-${System.nanoTime()}"
    )

    private fun newSession(bridge: GameBridge = FakeBridge()): GameSession =
        GameSession(bridge, WorldContentLoader.load(), saveDir)

    // ------------------------------------------------------------
    // NPC waypoint movement (spec §39, §52)
    // ------------------------------------------------------------

    @Test
    fun `npc walks toward its scheduled waypoint instead of teleporting`() {
        val definition = NpcDefinition(
            id = "walker",
            name = "Walker",
            cellId = "town",
            anchor = WorldPoint(0f, 0f),
            schedule = listOf(
                NpcScheduleEntry(0, 1439, "town", WorldPoint(100f, 0f))
            )
        )
        val director = NpcDirector(listOf(definition))
        val entities = EntityManager()
        director.spawn(entities, 9 * 60)
        val npc = director.npc("walker")!!
        val start = npc.entity.position

        // A few fixed ticks: the NPC closes in but must NOT jump straight to
        // the waypoint — that's a walk, not a teleport.
        repeat(10) { director.tick(entities, 12 * 60, 1f / 60f) }
        val after = npc.entity.position
        assertTrue(after.distanceTo(start) > 0f, "NPC should have moved")
        assertTrue(after.distanceTo(Vec2(100f, 0f)) > 5f, "NPC should still be en route")
        assertTrue(npc.isMoving)

        // Long enough to finish the leg: the NPC arrives and idles.
        repeat(600) { director.tick(entities, 12 * 60, 1f / 60f) }
        assertTrue(npc.entity.position.distanceTo(Vec2(100f, 0f)) <= 3f, "NPC should arrive at the waypoint")
        assertFalse(npc.isMoving)
    }

    @Test
    fun `npc with no schedule stands at its anchor`() {
        val definition = NpcDefinition(
            id = "still",
            name = "Still",
            cellId = "town",
            anchor = WorldPoint(42f, 42f)
        )
        val director = NpcDirector(listOf(definition))
        val entities = EntityManager()
        director.spawn(entities, 9 * 60)
        val npc = director.npc("still")!!
        repeat(30) { director.tick(entities, 12 * 60, 1f / 60f) }
        assertEquals(42f, npc.entity.position.x)
        assertEquals(42f, npc.entity.position.y)
        assertFalse(npc.isMoving)
    }

    // ------------------------------------------------------------
    // Ordering minigame (spec §56)
    // ------------------------------------------------------------

    @Test
    fun `order session opens with a menu and orders one item`() {
        val order = OrderSession()
        val menu = listOf(
            ua.syt0r.kanji.desktop.game.activity.MenuItem("takoyaki", "たこ焼き", "たこやき", "octopus dumplings", knowledgeId = "takoyaki")
        )
        order.open("stall-takoyaki", menu)
        assertTrue(order.isActive)
        assertEquals("stall-takoyaki", order.stallId)

        val item = order.order("takoyaki")
        assertNotNull(item)
        assertEquals("たこ焼き", order.lastOrdered?.nameJp)
        // A second order does nothing (one order per visit).
        order.order("takoyaki")
        assertEquals("たこ焼き", order.lastOrdered?.nameJp)

        order.close()
        assertFalse(order.isActive)
        assertTrue(order.items.isEmpty())
    }

    @Test
    fun `ordering from the stall completes the OrderFood quest objective`() {
        val session = newSession()
        // Evening: the festival folk are out (spec §40).
        session.clock.minuteOfDay = 18 * 60
        session.tick(1f / 60f) // spawn the evening population

        // Complete the chain the real way: welcome (station + station
        // master) → festival-1 (reach the grounds, talk to Aoi) → festival-2.
        session.player.entity.teleport(Vec2(1080f, 264f))
        session.tick(1f / 60f)
        session.handleInteraction(InteractionResult.StartDialogue("ekicho"))
        repeat(2) { session.dialogue.advance() }
        session.dialogue.choose(0)
        assertTrue(session.quests.isComplete("welcome"), "welcome should complete")

        session.player.entity.teleport(Vec2(960f, 700f))
        session.tick(1f / 60f)
        val aoi = session.npcDirector.npc("aoi")
        assertNotNull(aoi, "Aoi should be present in the evening")
        session.player.entity.teleport(aoi.entity.position + Vec2(0f, 60f))
        session.handleInteraction(InteractionResult.StartDialogue("aoi"))
        session.dialogue.advance() // finish her dialogue
        assertTrue(session.quests.isComplete("festival-1"), "festival-1 should complete")
        // festival-2 becomes available once festival-1 completes; start it.
        assertTrue(session.quests.start("festival-2"), "festival-2 should be available")

        // Walk to the takoyaki stall and order takoyaki off the Japanese menu.
        session.player.entity.teleport(Vec2(960f, 780f))
        val result = session.interaction.interact(session.player.entity.position)
        assertTrue(result is InteractionResult.OrderFood, "expected OrderFood, got $result")
        session.handleInteraction(result)
        assertTrue(session.state.orderOpen)
        assertEquals("stall-takoyaki", session.state.orderStallId)

        session.chooseOrderItem("takoyaki")
        val progress = session.quests.progressFor("festival-2")
        assertTrue(progress!!.objectives.first { it.objectiveId == "order-takoyaki" }.complete)
        assertTrue(session.learning.isDiscovered("takoyaki"))
        assertTrue(session.player.state.inventory.any { it.itemId == "takoyaki" })

        session.closeOrder()
        assertFalse(session.state.orderOpen)
    }

    // ------------------------------------------------------------
    // Photo album actions (spec §43-46)
    // ------------------------------------------------------------

    @Test
    fun `saving a photo forwards it to the bridge`() {
        val bridge = FakeBridge()
        val session = newSession(bridge)
        val photo = Photo(
            id = "photo-test-1",
            title = "海",
            category = PhotoCategory.Nature,
            tags = listOf(PhotoTag("umi", "海", "うみ", "sea")),
            regionId = "hamanaka"
        )
        session.album.add(photo)
        session.state.photoDetail = photo.id
        assertNotNull(session.state.photoDetail)

        session.savePhotoToDisk(photo)
        assertEquals(1, bridge.savedPhotos.size)
        assertEquals("海", bridge.savedPhotos.first().title)
    }

    @Test
    fun `deleting a photo removes it from the album and closes the detail`() {
        val session = newSession()
        val photo = Photo(id = "photo-del", title = "Cat", category = PhotoCategory.Animals)
        session.album.add(photo)
        session.state.photoDetail = photo.id

        session.deletePhoto(photo.id)
        assertTrue(session.album.photos.none { it.id == "photo-del" })
        assertEquals(null, session.state.photoDetail)
    }

    @Test
    fun `order food event matches by stall or item`() {
        val quest = ua.syt0r.kanji.desktop.game.quest.Quest(
            id = "q",
            title = "q",
            objectives = listOf(
                ua.syt0r.kanji.desktop.game.quest.QuestObjective(
                    id = "o1",
                    kind = ua.syt0r.kanji.desktop.game.quest.ObjectiveKind.OrderFood,
                    targetId = "stall-takoyaki"
                )
            )
        )
        val manager = QuestManager(listOf(quest))
        manager.initProgress()
        manager.start("q")

        manager.reportEvent(QuestEvent.OrderFood("takoyaki", "stall-takoyaki"))
        assertTrue(manager.progressFor("q")!!.objectives.first().complete)
    }
}
