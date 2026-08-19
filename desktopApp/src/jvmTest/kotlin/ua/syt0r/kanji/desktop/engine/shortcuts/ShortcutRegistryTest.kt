package ua.syt0r.kanji.desktop.engine.shortcuts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShortcutRegistryTest {

    private val registry = ShortcutRegistry()

    @Test
    fun defaultCatalogHasUniqueIds() {
        val ids = registry.all().map { it.id }
        assertEquals(ids.size, ids.distinct().size, "duplicate shortcut ids")
    }

    @Test
    fun defaultCatalogHasNoDuplicateChords() {
        val chords = registry.all().filter { it.enabled }.map { it.boundChord }
        val duplicates = chords.groupBy { it }.filterValues { it.size > 1 }
        assertTrue(duplicates.isEmpty(), "duplicate default chords: $duplicates")
    }

    @Test
    fun everyWorkspaceViewIsReachableFromTheKeyboard() {
        // The ids the shell registers handlers for — every one must exist in
        // the catalog, otherwise registry.matches can never fire for it.
        val handlerIds = listOf(
            "command-palette", "focus-search", "quick-switch",
            "again", "hard", "good", "easy", "show-answer", "undo",
            "suspend", "bury", "skip", "retry", "preview", "delete-card",
            "select-all", "grid-view", "list-view",
            "open-dashboard", "open-browser", "open-library", "open-review",
            "open-exams", "open-writing", "open-grammar", "open-stats",
            "open-mistakes", "open-settings", "open-themes", "open-history",
            "open-transfer", "open-dictionary", "open-mining", "open-media",
            "tab-new", "tab-close", "tab-next", "tab-previous", "tab-reopen",
            "open-reading", "open-curriculum", "open-graph", "open-browser2",
            "open-ocr", "open-integrations", "open-game", "mine-selection"
        )
        val catalogIds = registry.all().map { it.id }.toSet()
        val missing = handlerIds.filter { it !in catalogIds }
        assertTrue(missing.isEmpty(), "handlers without catalog entries: $missing")
    }

    @Test
    fun mineSelectionUsesCtrlShiftM() {
        val def = registry.get("mine-selection")
        assertNotNull(def)
        assertEquals("m", def.defaultChord.key)
        assertTrue(def.defaultChord.ctrl && def.defaultChord.shift)
    }

    @Test
    fun openGameUsesF9() {
        val def = registry.get("open-game")
        assertNotNull(def)
        assertEquals("f9", def.defaultChord.key)
    }

    @Test
    fun rebindingDetectsConflicts() {
        val result = registry.bind("open-reading", KeyChord("e", ctrl = true, shift = true))
        assertTrue(result.isFailure, "rebind to an occupied chord must fail")
    }

    @Test
    fun resetRestoresDefaultChord() {
        val ok = registry.bind("open-reading", KeyChord("p", ctrl = true, alt = true))
        assertTrue(ok.isSuccess)
        registry.reset("open-reading")
        assertEquals(KeyChord("r", ctrl = true, shift = true), registry.get("open-reading")?.boundChord)
    }

    @Test
    fun unknownIdCannotBind() {
        assertTrue(registry.bind("does-not-exist", KeyChord("z")).isFailure)
        assertNull(registry.get("does-not-exist"))
    }
}
