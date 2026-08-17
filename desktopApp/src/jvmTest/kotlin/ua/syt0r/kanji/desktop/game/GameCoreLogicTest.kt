package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.engine.SpatialHash

import ua.syt0r.kanji.desktop.game.engine.geom.Rect
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.engine.input.ControlScheme
import ua.syt0r.kanji.desktop.game.engine.input.GameKey
import ua.syt0r.kanji.desktop.game.engine.input.InputAction
import ua.syt0r.kanji.desktop.game.engine.input.InputState
import ua.syt0r.kanji.desktop.game.learning.KnowledgeGraph
import ua.syt0r.kanji.desktop.game.learning.KnowledgeKind
import ua.syt0r.kanji.desktop.game.learning.KnowledgeLink
import ua.syt0r.kanji.desktop.game.learning.KnowledgeNode
import ua.syt0r.kanji.desktop.game.learning.KnowledgeRelation
import ua.syt0r.kanji.desktop.game.learning.Sentence
import ua.syt0r.kanji.desktop.game.time.TimePhase
import ua.syt0r.kanji.desktop.game.time.WorldClock
import ua.syt0r.kanji.desktop.game.world.Cell
import ua.syt0r.kanji.desktop.game.world.TileDef
import ua.syt0r.kanji.desktop.game.world.TileGrid
import ua.syt0r.kanji.desktop.game.world.WorldRect

class GameCoreLogicTest {

    // ------------------------------------------------------------
    // Input
    // ------------------------------------------------------------

    @Test
    fun `scheme maps actions both ways and supports rebinding`() {
        val scheme = ControlScheme.default()
        assertEquals(InputAction.MoveUp, scheme.actionFor(GameKey.W))
        assertTrue(scheme.keysFor(InputAction.MoveUp).contains(GameKey.W))

        val rebound = scheme.bind(InputAction.Jump, GameKey.J)
        assertTrue(rebound.keysFor(InputAction.Jump).contains(GameKey.J))
        val unbound = rebound.unbind(InputAction.Jump, GameKey.J)
        assertFalse(unbound.keysFor(InputAction.Jump).contains(GameKey.J))
    }

    @Test
    fun `raw edge queue never loses presses between frames`() {
        val state = InputState()
        // Press arrives between beginFrame calls…
        state.press(InputAction.Interact)
        // …the next frame delivers it as a fresh edge.
        state.beginFrame()
        assertTrue(state.wasPressedThisFrame(InputAction.Interact))
        // Second frame: no edge, still held.
        state.beginFrame()
        assertFalse(state.wasPressedThisFrame(InputAction.Interact))
        assertTrue(state.isPressed(InputAction.Interact))
        state.release(InputAction.Interact)
        state.beginFrame()
        assertTrue(state.wasReleasedThisFrame(InputAction.Interact))
    }

    // ------------------------------------------------------------
    // World clock
    // ------------------------------------------------------------

    @Test
    fun `clock phases follow time of day`() {
        assertEquals(TimePhase.Morning, TimePhase.fromMinutes(9 * 60))
        assertEquals(TimePhase.Day, TimePhase.fromMinutes(13 * 60))
        assertEquals(TimePhase.Evening, TimePhase.fromMinutes(19 * 60))
        assertEquals(TimePhase.Night, TimePhase.fromMinutes(23 * 60))
    }

    @Test
    fun `clock rolls over a day`() {
        val clock = WorldClock(minuteOfDay = 1439, day = 3)
        clock.tick(clock.secondsPerWorldMinute)
        assertEquals(0, clock.minuteOfDay)
        assertEquals(4, clock.day)
    }

    // ------------------------------------------------------------
    // Tile collision
    // ------------------------------------------------------------

    @Test
    fun `tile grid resolves solid tiles by sliding`() {
        // 3x3: grass with a solid block in the middle.
        val cell = Cell(
            id = "t",
            name = "t",
            tiles = listOf("...", ".B.", "..."),
            legend = mapOf(
                "." to TileDef(color = 0x8BC34A),
                "B" to TileDef(color = 0x9E9E9E, solid = true)
            ),
            bounds = WorldRect(0f, 0f, 144f, 144f)
        )
        val grid = TileGrid(cell, tileSize = 48f)
        assertTrue(grid.isSolidAt(1, 1))
        assertFalse(grid.isSolidAt(0, 0))

        // A rect moving left into the block stops at its right edge.
        val size = Vec2(24f, 32f)
        val result = grid.resolve(
            Vec2(120f, 72f), // right of the block (block x 48..96)
            size,
            Vec2(-50f, 0f),
            1f
        )
        // Block right edge (96) + half width (12) => 108.
        assertEquals(108f, result.x, 0.1f)
    }

    @Test
    fun `water is solid and out-of-bounds is solid`() {
        val cell = Cell(
            id = "t",
            name = "t",
            tiles = listOf("~", "."),
            legend = mapOf(
                "~" to TileDef(color = 0x4FC3F7, solid = true, animated = true),
                "." to TileDef(color = 0x8BC34A)
            ),
            bounds = WorldRect(0f, 0f, 96f, 96f)
        )
        val grid = TileGrid(cell, tileSize = 48f)
        assertTrue(grid.isSolidAt(0, 0))
        assertTrue(grid.isSolidAt(5, 5)) // out of bounds
        assertFalse(grid.isFree(Rect(20f, 20f, 10f, 10f)))
        assertTrue(grid.isFree(Rect(70f, 70f, 10f, 10f)))
    }

    // ------------------------------------------------------------
    // Spatial hash
    // ------------------------------------------------------------

    @Test
    fun `spatial hash finds nearest entry in radius`() {
        val hash = SpatialHash(cellSize = 48f)
        hash.insert("a", Rect(0f, 0f, 10f, 10f))
        hash.insert("b", Rect(100f, 100f, 10f, 10f))
        val nearest = hash.nearest(Vec2(4f, 4f), 30f)
        assertNotNull(nearest)
        assertEquals("a", nearest!!.id)
        assertNull(hash.nearest(Vec2(1000f, 1000f), 30f))
    }

    // ------------------------------------------------------------
    // Knowledge graph
    // ------------------------------------------------------------

    @Test
    fun `knowledge graph chains kanji to word to sentence`() {
        val graph = KnowledgeGraph(
            nodes = listOf(
                KnowledgeNode("kanji-eki", KnowledgeKind.KANJI, "駅", "えき", "station"),
                KnowledgeNode("eki", KnowledgeKind.WORD, "駅", "えき", "station", kanjiIds = listOf("kanji-eki"),
                    sentence = Sentence(
                        jp = "駅はどこですか。", reading = "えきはどこですか。",
                        translation = "Where is the station?", vocabulary = listOf("eki", "doko")
                    )
                ),
                KnowledgeNode("doko", KnowledgeKind.WORD, "どこ", "どこ", "where")
            ),
            links = listOf(
                KnowledgeLink("kanji-eki", "eki", KnowledgeRelation.COMPOUND),
                KnowledgeLink("eki", "doko", KnowledgeRelation.WORD_IN_SENTENCE)
            )
        )
        val chain = graph.chainFor("eki")
        assertEquals(listOf("kanji-eki", "eki", "doko"), chain.map { it.id })
        assertEquals("eki", graph.nodeByHeadword("駅")?.id)
        assertEquals(listOf("eki", "doko"), graph.expand("kanji-eki").map { it.id })
    }
}
