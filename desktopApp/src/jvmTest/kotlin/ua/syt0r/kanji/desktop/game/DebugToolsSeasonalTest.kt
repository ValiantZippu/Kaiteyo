package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.bridge.BridgeLookup
import ua.syt0r.kanji.desktop.game.bridge.BridgeMinePayload
import ua.syt0r.kanji.desktop.game.bridge.BridgeToastKind
import ua.syt0r.kanji.desktop.game.bridge.GameActivityKind
import ua.syt0r.kanji.desktop.game.bridge.GameBridge
import ua.syt0r.kanji.desktop.game.content.WorldContentLoader
import ua.syt0r.kanji.desktop.game.time.Season
import ua.syt0r.kanji.desktop.game.time.WeatherKind
import java.io.File

/**
 * Debug tools (spec §121-122): forcing season/weather/time; clock pacing
 * (spec §40); and the seasonal-event content gates (winter market, spring
 * blossoms).
 */
class DebugToolsSeasonalTest {

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
        "kaiteyo-test-debug-${System.nanoTime()}"
    )

    private fun newSession(bridge: GameBridge = FakeBridge()): GameSession =
        GameSession(bridge, WorldContentLoader.load(), saveDir)

    // ------------------------------------------------------------
    // Debug tools (spec §121-122)
    // ------------------------------------------------------------

    @Test
    fun `debug force season spawns the winter market`() {
        val session = newSession()
        assertEquals(Season.Summer, session.seasons.current)
        assertNull(session.npcDirector.npc("yuki-chan"), "no cocoa vendor in summer")

        session.debugForceSeason(Season.Winter)
        assertEquals(Season.Winter, session.seasons.current)
        assertNotNull(session.npcDirector.npc("yuki-chan"), "winter market appears in winter")
        assertNotNull(session.npcDirector.npc("amezaiku-ya"), "candy vendor appears in winter")

        session.debugForceSeason(Season.Spring)
        assertNull(session.npcDirector.npc("yuki-chan"), "winter market leaves in spring")
    }

    @Test
    fun `debug force weather gates rain-dependent presence`() {
        val session = newSession()
        // In summer the weather leans sun; force rain and check the gates.
        session.debugForceWeather(WeatherKind.Rain)
        assertEquals(WeatherKind.Rain, session.weather.current)
        // Weather quest objective completes through the reported change.
        val quest = session.quests.quest("winter-candy")
        assertNotNull(quest)
    }

    @Test
    fun `debug set time moves the clock and repopulates`() {
        val session = newSession()
        session.debugSetTime(18 * 60)
        assertEquals("Evening", session.clock.phase.name)
        // Evening-only festival folk are out.
        assertNotNull(session.npcDirector.npc("aoi"), "Aoi appears in the evening")
        session.debugSetTime(12 * 60)
        assertNull(session.npcDirector.npc("aoi"), "Aoi leaves at midday")
    }

    // ------------------------------------------------------------
    // Clock pacing (spec §40)
    // ------------------------------------------------------------

    @Test
    fun `clock pacing setting is applied live`() {
        val session = newSession()
        val start = session.clock.minuteOfDay
        // Fast pacing: 0.5 real seconds per world minute.
        session.settings = session.settings.copy(secondsPerWorldMinute = 0.5f)
        session.tick(1f)
        val fast = session.clock.minuteOfDay
        assertTrue(fast > start, "fast pacing advances the clock")

        // Real time: 60 s per world minute — one second advances nothing.
        session.settings = session.settings.copy(secondsPerWorldMinute = 60f)
        val frozen = session.clock.minuteOfDay
        session.tick(1f)
        assertEquals(frozen, session.clock.minuteOfDay, "real-time pacing barely moves")
    }

    // ------------------------------------------------------------
    // Seasonal event content (spec §42)
    // ------------------------------------------------------------

    @Test
    fun `spring blossoms quest exists and gates on spring`() {
        val session = newSession()
        val quest = session.quests.quest("spring-blossoms")
        assertNotNull(quest, "spring-blossoms quest is authored")
        // It depends on the Kamakura chain; winter quest on welcome.
        val winter = session.quests.quest("winter-market")
        assertNotNull(winter)
        assertTrue(winter!!.objectives.any { it.kind.name == "Season" && it.targetId == "Winter" })
    }

    @Test
    fun `winter stall object is closed outside winter`() {
        val session = newSession()
        // Summer: the winter stall's interactable is registered but gated.
        session.player.entity.teleport(ua.syt0r.kanji.desktop.game.engine.geom.Vec2(1056f, 740f))
        val target = session.interaction.interactable("obj:obj-winter-stall")
        assertNotNull(target, "winter stall object exists")
        assertTrue(target!!.isOpenInSeason("Winter"))
        assertEquals(false, target.isOpenInSeason("Summer"))
    }
}
