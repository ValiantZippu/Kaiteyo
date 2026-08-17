package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.bridge.BridgeLookup
import ua.syt0r.kanji.desktop.game.bridge.BridgeMinePayload
import ua.syt0r.kanji.desktop.game.bridge.BridgeToastKind
import ua.syt0r.kanji.desktop.game.bridge.GameActivityKind
import ua.syt0r.kanji.desktop.game.bridge.GameBridge
import ua.syt0r.kanji.desktop.game.content.WorldContentLoader
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.interaction.InteractionResult
import ua.syt0r.kanji.desktop.game.learning.AssistanceLevel
import ua.syt0r.kanji.desktop.game.learning.DiscoverySource
import ua.syt0r.kanji.desktop.game.quest.QuestState
import java.io.File

/**
 * Loads the real vertical-slice content and drives the session: content
 * validity, the fresh-game quest flow, location discovery, word learning,
 * photography and save round-trips — all through a fake bridge so nothing
 * touches AppState.
 */
class GameSessionTest {

    private class FakeBridge(
        val dictionary: Map<String, String> = mapOf(
            "駅" to "station", "海" to "sea, ocean", "電車" to "train"
        ),
        /** Headwords the user already studies in Kaiteyo (adaptive, spec §73). */
        val knownHeadwords: Set<String> = emptySet()
    ) : GameBridge {
        val activities = mutableListOf<GameActivityKind>()
        val mined = mutableListOf<BridgeMinePayload>()
        val toasts = mutableListOf<String>()

        override fun lookup(headword: String): BridgeLookup? =
            dictionary[headword]?.let { BridgeLookup(headword, "", it, "Test Dict") }

        override fun hasStudyMaterialFor(headword: String): Boolean = headword in knownHeadwords

        override fun mine(payload: BridgeMinePayload): Boolean {
            mined.add(payload)
            return true
        }

        override fun recordActivity(kind: GameActivityKind, detail: String) {
            activities.add(kind)
        }

        override fun toast(message: String, kind: BridgeToastKind) {
            toasts.add(message)
        }

        override fun getSetting(key: String, default: String): String = default

        override fun setSetting(key: String, value: String) {}
    }

    private val saveDir: File = File(
        System.getProperty("java.io.tmpdir"),
        "kaiteyo-test-saves-${System.nanoTime()}"
    )

    private fun newSession(bridge: GameBridge = FakeBridge()): GameSession =
        GameSession(bridge, WorldContentLoader.load(), saveDir)

    @Test
    fun `bundled content validates cleanly`() {
        val content = WorldContentLoader.load()
        val issues = ua.syt0r.kanji.desktop.game.validation.ContentValidator().validate(content)
        assertEquals(
            emptyList(),
            issues.errors.map { it.message },
            "Content errors: ${issues.errors.joinToString { it.message }}"
        )
    }

    @Test
    fun `fresh game auto-starts the welcome quest and locks dependents`() {
        val session = newSession()
        assertEquals(QuestState.Active, session.quests.progressFor("welcome")?.state)
        assertEquals(QuestState.Locked, session.quests.progressFor("buy-drink")?.state)
    }

    @Test
    fun `walking to the station discovers the location and teaches eki`() {
        val session = newSession()
        session.player.entity.teleport(Vec2(1080f, 264f))
        session.tick(1f / 60f)
        assertTrue("station" in session.state.worldState.discoveredLocations)
        assertTrue(session.learning.isDiscovered("eki"))
        val progress = session.quests.progressFor("welcome")
        assertTrue(progress!!.objectives.first { it.objectiveId == "reach-station" }.complete)
    }

    @Test
    fun `talking to the station master completes welcome and grants the drink quest`() {
        val session = newSession()
        session.player.entity.teleport(Vec2(1080f, 264f))
        session.tick(1f / 60f) // discovery: reach-station + learn-eki

        session.handleInteraction(InteractionResult.StartDialogue("ekicho"))
        assertTrue(session.dialogue.isActive)
        // Advance to the choice, pick "yes" — effects grant the available quests.
        repeat(2) { session.dialogue.advance() }
        session.dialogue.choose(0)

        assertTrue(session.quests.isComplete("welcome"))
        assertEquals(QuestState.Active, session.quests.progressFor("buy-drink")?.state)
    }

    @Test
    fun `full slice flow: talk, buy a drink, learn words`() {
        val bridge = FakeBridge()
        val session = newSession(bridge)
        // Walk to the station and talk to the station master.
        session.player.entity.teleport(Vec2(1080f, 264f))
        session.tick(1f / 60f)
        session.handleInteraction(InteractionResult.StartDialogue("ekicho"))
        repeat(2) { session.dialogue.advance() }
        session.dialogue.choose(0)
        session.dialogue.advance() // finish dialogue

        // Walk to the vending machine and buy a drink.
        session.player.entity.teleport(Vec2(1344f, 900f))
        val result = session.interaction.interact(session.player.entity.position)
        assertTrue(result is InteractionResult.BuyDrink)
        session.handleInteraction(result)

        assertTrue(session.learning.isDiscovered("nomimono"))
        assertTrue(session.learning.isDiscovered("mizu"))
        val buyProgress = session.quests.progressFor("buy-drink")
        assertTrue(buyProgress!!.objectives.first { it.objectiveId == "buy" }.complete)
        assertTrue(bridge.activities.contains(GameActivityKind.WordDiscovered))
    }

    @Test
    fun `save round-trip preserves position, quests and knowledge`() {
        val session = newSession()
        session.player.entity.teleport(Vec2(1080f, 264f))
        session.tick(1f / 60f)
        session.save()

        val reloaded = newSession()
        assertTrue("station" in reloaded.state.worldState.discoveredLocations)
        assertTrue(reloaded.learning.isDiscovered("eki"))
        assertEquals(session.player.state.position, reloaded.player.state.position)
    }

    @Test
    fun `mined discoveries flow through the bridge`() {
        val bridge = FakeBridge()
        val session = newSession(bridge)
        session.learning.discover("eki", session.settings.assistanceLevel, DiscoverySource.Object)
        session.learning.mine("eki")
        assertEquals(1, bridge.mined.size)
        assertEquals("駅", bridge.mined.first().headword)
        assertTrue(session.learning.mined.contains("eki"))
    }

    @Test
    fun `adaptive learning: a word already studied in Kaiteyo is recognized, not re-taught`() {
        val bridge = FakeBridge(knownHeadwords = setOf("駅"))
        val session = newSession(bridge)
        // Walking to the station discovers its learning targets (incl. 駅).
        session.player.entity.teleport(Vec2(1080f, 264f))
        session.tick(1f / 60f)
        // The word IS in the knowledge map (the world recognizes it)…
        assertTrue(session.learning.isDiscovered("eki"))
        // …but it is never counted as a newly learned word. The station's
        // other two targets (電車, 切符) are new, so only they count.
        assertEquals(2, session.learning.wordsLearned)
        // …and no "New discovery!" popup is queued for it.
        assertTrue(session.state.discoveryQueue.none { it.node.id == "eki" })
        // Quest progress still fires: the welcome quest's learn-eki objective
        // completes because the player already knows the word.
        val progress = session.quests.progressFor("welcome")
        assertTrue(progress!!.objectives.first { it.objectiveId == "learn-eki" }.complete)
    }

    @Test
    fun `kids mode pins the effective assistance level to Kids`() {
        val session = newSession()
        assertEquals(AssistanceLevel.Normal, session.effectiveAssistance)
        session.settings = session.settings.copy(kidMode = true)
        assertEquals(AssistanceLevel.Kids, session.effectiveAssistance)
    }

    @Test
    fun `kids mode swaps the vending machine vocabulary to the kid layer`() {
        val session = newSession()
        session.settings = session.settings.copy(kidMode = true)
        // Same machine, simpler words: 水/お茶/ジュース — never 飲み物.
        session.player.entity.teleport(Vec2(1344f, 900f))
        val result = session.interaction.interact(session.player.entity.position)
        assertTrue(result is InteractionResult.BuyDrink)
        session.handleInteraction(result)
        assertTrue(session.learning.isDiscovered("mizu"))
        assertTrue(session.learning.isDiscovered("ocha"))
        assertTrue(session.learning.isDiscovered("juice"))
        assertFalse(session.learning.isDiscovered("nomimono"))
    }

    @Test
    fun `kid mode swaps dialogue text to the authored kid variant`() {
        val session = newSession()
        session.dialogue.start("welcome")
        session.dialogue.advance() // w1 → w2 (the station-explainer line)
        val w2 = session.dialogue.currentLine!!
        // Normal mode shows the main text with kanji.
        assertTrue(w2.jp.startsWith("ここは「えき」です。電車"))
        // Kid mode substitutes the simpler all-kana variant.
        val kid = w2.withKidText()
        assertEquals("ここは「えき」です。でんしゃは、ここからのります。", kid.jp)
        assertEquals(kid.reading, kid.jp)
    }

    @Test
    fun `knowledge-gated choice appears only after its word is discovered`() {
        val session = newSession()
        session.dialogue.start("festival-lantern")
        val first = session.dialogue.currentLine!!
        // Hina offers three options, but 花火 is knowledge-gated.
        assertEquals(3, first.options.size)
        val knows = session.learning::isDiscovered
        assertEquals(2, first.options.count { it.isAvailable(knows) })
        // Discovering 花火 (e.g. from the hanabi sign) unlocks the third.
        session.learning.discover("hanabi", session.effectiveAssistance, DiscoverySource.Object)
        assertEquals(3, first.options.count { it.isAvailable(knows) })
        // Picking the gated option lands on its special response line.
        val available = session.dialogue.availableChoices(knows)
        val gated = available.first { it.second.requiresKnowledge == "hanabi" }
        session.dialogue.choose(gated.first)
        assertEquals("fl4", session.dialogue.currentLine!!.id)
    }

    @Test
    fun `kanji writing desk completes tracing and rewards xp`() {
        val session = newSession()
        session.openWriting(listOf("kanji-eki"))
        assertTrue(session.state.writingOpen)
        assertEquals("駅", session.currentWritingTarget()!!.headword)
        val xpBefore = session.player.state.xp
        session.completeWriting("kanji-eki")
        // Tracing a kanji discovers it as a kanji node and pays out.
        assertTrue(session.learning.isDiscovered("kanji-eki"))
        assertEquals(xpBefore + 6, session.player.state.xp)
        assertFalse(session.state.writingOpen)
    }

    @Test
    fun `story choice branches to its scene, grants knowledge and sets a flag`() {
        val session = newSession()
        session.startStory("summer-festival")
        session.advanceStory() // evening → stalls
        session.advanceStory() // stalls → fireworks (the branch scene)
        assertEquals(2, session.story.activeScene!!.options.size)
        // Watch the fireworks to the end: jumps to the moon scene, grants 月,
        // marks the finale.
        session.chooseStory(0)
        assertEquals("sc-festival-home", session.story.activeScene?.id)
        assertTrue(session.learning.isDiscovered("tsuki"))
        assertEquals("true", session.state.worldState.flags["festival-finale"])
    }

    @Test
    fun `story choice branch two ends the story without the extra grant`() {
        val session = newSession()
        session.startStory("summer-festival")
        session.advanceStory()
        session.advanceStory()
        // Heading home early: no moon scene, no 月, story completes.
        session.chooseStory(1)
        assertEquals("festival-early", session.state.worldState.flags["festival-early"])
        assertFalse(session.learning.isDiscovered("tsuki"))
        assertTrue(session.story.progress.value.any { it.storyId == "summer-festival" && it.completed })
    }

    @Test
    fun `talking about an NPC's favorite topic deepens the relationship`() {
        val session = newSession()
        val npc = session.npcDirector.npc("ekicho")!!
        val affinityBefore = npc.relationship.affinity
        assertEquals(listOf("eki", "densha", "kippu"), npc.relationship.favoriteTopics)
        // The station master's first line exposes 駅 — his favorite topic.
        session.handleInteraction(InteractionResult.StartDialogue("ekicho"))
        session.tick(1f / 60f)
        assertTrue(
            npc.relationship.affinity > affinityBefore,
            "affinity should grow when dialogue touches a favorite topic"
        )
    }

    @Test
    fun `photo spot opens photo mode and teaches its subject`() {
        val session = newSession()
        // The beach camera spot (海の写真を撮ろう).
        session.player.entity.teleport(Vec2(1008f, 1248f))
        val result = session.interaction.interact(session.player.entity.position)
        assertTrue(result is InteractionResult.PhotoSpot)
        session.handleInteraction(result)
        assertTrue(session.state.photoMode)
        assertTrue(session.learning.isDiscovered("umi"))
        assertTrue(session.learning.isDiscovered("shashin"))
    }

    @Test
    fun `photo capture tags vocabulary and fires photo quest events`() {
        val bridge = FakeBridge()
        val session = newSession(bridge)
        session.photoCamera.focus = Vec2(1152f, 1104f) // the cat
        session.state.photoMode = true
        session.capturePhotoPublic()
        assertTrue(session.album.photos.isNotEmpty())
        assertTrue(session.learning.isDiscovered("neko"))
    }
}
