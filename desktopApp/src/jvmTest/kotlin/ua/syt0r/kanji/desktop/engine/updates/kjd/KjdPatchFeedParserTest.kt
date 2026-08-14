package ua.syt0r.kanji.desktop.engine.updates.kjd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KjdPatchFeedParserTest {

    private val sha256 = "a".repeat(64)

    @Test
    fun parsesValidFeed() {
        val raw = """
            {
              "schema_version": 1,
              "channel": "stable",
              "published_at": "2026-01-01T00:00:00Z",
              "database_version": "1.2.0",
              "patches": [
                {
                  "from_database_version": "1.1.0",
                  "from_fingerprint": "abc",
                  "to_database_version": "1.2.0",
                  "to_fingerprint": "def",
                  "url": "https://example.com/patches/1.1.0-to-1.2.0.json",
                  "sha256": "$sha256",
                  "size_bytes": 4096
                }
              ]
            }
        """.trimIndent()

        val feed = KjdPatchFeedParser.parse(raw)
        assertEquals(1, feed.schemaVersion)
        assertEquals("1.2.0", feed.databaseVersion)
        assertEquals(1, feed.patches.size)
        assertEquals("abc", feed.patches[0].fromFingerprint)
        assertEquals("def", feed.patches[0].toFingerprint)
        assertEquals(4096L, feed.patches[0].sizeBytes)
    }

    @Test
    fun rejectsUnsupportedSchemaVersion() {
        val raw = """
            {
              "schema_version": 2,
              "channel": "stable",
              "patches": [
                {
                  "from_fingerprint": "abc",
                  "to_fingerprint": "def",
                  "url": "https://example.com/p.json",
                  "sha256": "$sha256"
                }
              ]
            }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> { KjdPatchFeedParser.parse(raw) }
    }

    @Test
    fun rejectsMissingSha256() {
        val raw = """
            {
              "schema_version": 1,
              "channel": "stable",
              "patches": [
                {
                  "from_fingerprint": "abc",
                  "to_fingerprint": "def",
                  "url": "https://example.com/p.json",
                  "sha256": "short"
                }
              ]
            }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> { KjdPatchFeedParser.parse(raw) }
    }

    @Test
    fun rejectsHttpUrl() {
        val raw = """
            {
              "schema_version": 1,
              "channel": "stable",
              "patches": [
                {
                  "from_fingerprint": "abc",
                  "to_fingerprint": "def",
                  "url": "http://example.com/p.json",
                  "sha256": "$sha256"
                }
              ]
            }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> { KjdPatchFeedParser.parse(raw) }
    }

    @Test
    fun rejectsEmptyPatches() {
        val raw = """
            {
              "schema_version": 1,
              "channel": "stable",
              "patches": []
            }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> { KjdPatchFeedParser.parse(raw) }
    }

    @Test
    fun feedFileNamePerChannel() {
        assertEquals(
            "kjd-update-stable.json",
            ua.syt0r.kanji.desktop.engine.updates.UpdateChannel.Stable.kjdFeedFileName()
        )
        assertEquals(
            "kjd-update-nightly.json",
            ua.syt0r.kanji.desktop.engine.updates.UpdateChannel.Nightly.kjdFeedFileName()
        )
        assertTrue("kjd-update-beta.json".endsWith(".json"))
    }
}
