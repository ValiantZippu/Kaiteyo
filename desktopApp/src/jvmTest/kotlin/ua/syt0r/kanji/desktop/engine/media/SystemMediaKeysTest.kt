package ua.syt0r.kanji.desktop.engine.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure-logic tests for the system media key mapping. The hook itself (a
 * WH_KEYBOARD_LL message pump) cannot run in CI, so the testable surface is
 * the VK-code → action mapping the hook is built on.
 */
class SystemMediaKeysTest {

    @Test
    fun `media vk codes map to the right actions`() {
        assertEquals(SystemMediaKeyAction.Toggle, systemMediaKeyActionForVk(0xB3)) // VK_MEDIA_PLAY_PAUSE
        assertEquals(SystemMediaKeyAction.Next, systemMediaKeyActionForVk(0xB0)) // VK_MEDIA_NEXT_TRACK
        assertEquals(SystemMediaKeyAction.Previous, systemMediaKeyActionForVk(0xB1)) // VK_MEDIA_PREV_TRACK
        assertEquals(SystemMediaKeyAction.Stop, systemMediaKeyActionForVk(0xB2)) // VK_MEDIA_STOP
    }

    @Test
    fun `non-media keys resolve to null`() {
        assertNull(systemMediaKeyActionForVk(0x41)) // 'A'
        assertNull(systemMediaKeyActionForVk(0))
        assertNull(systemMediaKeyActionForVk(0x7F)) // VK_SNAPSHOT
    }

    @Test
    fun `every action is reachable and maps to exactly one vk`() {
        val mediaVks = listOf(0xB0, 0xB1, 0xB2, 0xB3)
        val reached = mediaVks.mapNotNull { systemMediaKeyActionForVk(it) }
        assertEquals(SystemMediaKeyAction.entries.size, reached.distinct().size)
        SystemMediaKeyAction.entries.forEach { action ->
            assertEquals(1, reached.count { it == action })
        }
    }
}
