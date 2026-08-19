package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Graph-trail tests (KT-GRAPH-004, spec §75): the graph is a navigation
 * surface — breadcrumbs, back and forward must behave like a real history.
 */
class GraphTrailTest {

    private fun trailOf(vararg ids: String): GraphTrail =
        GraphTrail(entries = ids.toList(), position = ids.lastIndex)

    @Test
    fun emptyTrailHasNoCurrent() {
        val trail = GraphTrail.Empty
        assertTrue(trail.isEmpty)
        assertNull(trail.current)
        assertNull(trail.root)
        assertFalse(trail.canGoBack)
        assertFalse(trail.canGoForward)
    }

    @Test
    fun pushAppendsAndMovesFocus() {
        val trail = GraphTrail.Empty.push("kanji:食")
        assertEquals("kanji:食", trail.current)
        assertEquals(listOf("kanji:食"), trail.breadcrumbs())
        assertFalse(trail.canGoBack)

        val next = trail.push("word:1")
        assertEquals("word:1", next.current)
        assertEquals(listOf("kanji:食", "word:1"), next.breadcrumbs())
        assertTrue(next.canGoBack)
    }

    @Test
    fun repeatedConsecutivePushIsNoOp() {
        val trail = trailOf("kanji:食").push("kanji:食")
        assertEquals(listOf("kanji:食"), trail.entries)
    }

    @Test
    fun backMovesToPrevious() {
        val trail = trailOf("kanji:食", "word:1", "sentence:2")
        val (backed, nodeId) = trail.back()
        assertEquals("word:1", nodeId)
        assertEquals("word:1", backed.current)
        assertEquals(listOf("kanji:食", "word:1"), backed.breadcrumbs())
    }

    @Test
    fun backAtRootIsNoOp() {
        val trail = trailOf("kanji:食")
        val (same, nodeId) = trail.back()
        assertNull(nodeId)
        assertEquals(trail, same)
    }

    @Test
    fun forwardMovesToRedoPath() {
        val trail = trailOf("kanji:食", "word:1", "sentence:2")
        val (backed, _) = trail.back()
        val (forwarded, nodeId) = backed.forward()
        assertEquals("sentence:2", nodeId)
        assertEquals("sentence:2", forwarded.current)
    }

    @Test
    fun pushAfterBackClearsForward() {
        val trail = trailOf("kanji:食", "word:1", "sentence:2")
        val (backed, _) = trail.back()
        // The stale forward entry (sentence:2) must be discarded.
        val pushed = backed.push("word:3")
        assertEquals(listOf("kanji:食", "word:1", "word:3"), pushed.entries)
        assertFalse(pushed.canGoForward)
    }

    @Test
    fun pushRepeatedAfterBackIsNotTreatedAsConsecutiveNoOp() {
        // After going back to word:1, pushing word:1 again would be a no-op
        // only if it were the current AND last entry — here it is current but
        // not last, so the forward path is discarded and word:1 is appended.
        val trail = trailOf("kanji:食", "word:1", "sentence:2")
        val (backed, _) = trail.back()
        val pushed = backed.push("word:1")
        assertEquals(listOf("kanji:食", "word:1", "word:1"), pushed.entries)
        assertFalse(pushed.canGoForward)
    }

    @Test
    fun breadcrumbsEndAtCurrentPosition() {
        val trail = trailOf("kanji:食", "word:1", "sentence:2")
        val (backed, _) = trail.back()
        assertEquals(listOf("kanji:食", "word:1"), backed.breadcrumbs())
        assertEquals("kanji:食", backed.root)
    }
}
