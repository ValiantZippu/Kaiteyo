package ua.syt0r.kanji.desktop.engine.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Settings change notification — the contract the Dashboard study target relies
 * on to update live when the daily review target changes in Settings:
 * observers fire on real changes (including restores), skip no-op writes, and
 * the unsubscribe lambda returned by [SettingsEngine.observe] stops delivery.
 */
class SettingsEngineTest {

    private fun engine() = SettingsEngine(defs = defaultSettings())

    @Test
    fun `observe fires with old and new value on an actual change`() {
        val settings = engine()
        val events = mutableListOf<Triple<String, String, String>>()
        settings.observe { key, old, new -> events.add(Triple(key, old, new)) }

        settings.setInt("stats.daily-target", 30)

        assertEquals(1, events.size)
        assertEquals(Triple("stats.daily-target", "20", "30"), events.single())
    }

    @Test
    fun `observe does not fire for no-op writes`() {
        val settings = engine()
        var fired = 0
        settings.observe { _, _, _ -> fired++ }

        settings.setInt("stats.daily-target", 20) // already the default
        assertEquals(0, fired, "Writing the same value must not notify")
    }

    @Test
    fun `unsubscribe stops future notifications`() {
        val settings = engine()
        var fired = 0
        val unsubscribe = settings.observe { _, _, _ -> fired++ }

        settings.setInt("stats.daily-target", 30)
        assertEquals(1, fired)

        unsubscribe()
        settings.setInt("stats.daily-target", 40)
        assertEquals(1, fired, "Unsubscribed observers must not receive changes")
    }

    @Test
    fun `restore notifies observers for keys that actually changed`() {
        val settings = engine()
        val events = mutableListOf<String>()
        settings.observe { key, _, _ -> events.add(key) }

        settings.restore(mapOf("stats.daily-target" to "25", "general.language" to "en"))

        assertEquals(listOf("stats.daily-target", "general.language"), events)

        // Restoring the same values again must not re-notify.
        settings.restore(mapOf("stats.daily-target" to "25"))
        assertEquals(2, events.size)
    }

    @Test
    fun `unknown keys are ignored and get falls back to default`() {
        val settings = engine()
        assertEquals(20, settings.getInt("stats.daily-target", 20))
        assertFalse(settings.has("stats.nonexistent"))
    }
}
