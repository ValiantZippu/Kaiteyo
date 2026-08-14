package io.kaiteyo.kjd.patch

import io.kaiteyo.kjd.api.JapaneseDatabase
import io.kaiteyo.kjd.db.Schema
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for [DatabaseDiffGenerator]: row-level delta detection across the
 * content tables, fingerprint divergence, and the no-change case.
 */
class DatabaseDiffTest {

    @Test
    fun diffDetectsInsertsUpdatesAndDeletes() {
        val oldFile = PatchTestFixtures.buildOldDatabase()
        val newFile = PatchTestFixtures.buildNewDatabase()
        val patch = DatabaseDiffGenerator().generate(oldFile, newFile)

        assertEquals(2, patch.fromSchemaVersion)
        assertEquals(2, patch.toSchemaVersion)
        assertNotEquals(patch.fromFingerprint, patch.toFingerprint)

        // Kanji: 山 inserted, 水 updated, nothing deleted.
        val kanji = patch.tables.getValue("kanji")
        assertEquals(listOf("id"), kanji.keyColumns)
        assertTrue(kanji.inserts.any { it["id"] == JsonPrimitive("kanji:山") }, "kanji insert for 山")
        assertTrue(kanji.updates.any { it["id"] == JsonPrimitive("kanji:水") }, "kanji update for 水")
        assertTrue(kanji.deletes.isEmpty())

        // Vocab: 食堂 inserted, 食べる deleted.
        val vocab = patch.tables.getValue("vocab")
        assertTrue(vocab.inserts.any { it["id"] == JsonPrimitive("vocab:shokudo") }, "vocab insert for 食堂")
        assertTrue(
            vocab.deletes.any { it == listOf(JsonPrimitive("vocab:taberu")) },
            "vocab delete for 食べる"
        )

        // Senses: 食堂's sense inserted, 食べる's sense deleted.
        val sense = patch.tables.getValue("sense")
        assertTrue(sense.inserts.any { it["id"] == JsonPrimitive("sense:shokudo:0") })
        assertTrue(sense.deletes.any { it == listOf(JsonPrimitive("sense:taberu:0")) })

        // Source table (identical BuiltinSources in both) must not diff.
        assertTrue(patch.tables.getValue("source").isEmpty)

        assertTrue(patch.summary.inserted >= 3) // kanji + vocab + sense
        assertTrue(patch.summary.updated >= 1)  // kanji 水
        assertTrue(patch.summary.deleted >= 2)  // vocab + sense
    }

    @Test
    fun identicalDatabasesProduceEmptyPatch() {
        val oldFile = PatchTestFixtures.buildOldDatabase()
        val copy = File.createTempFile("kjd-patch-copy-", ".db")
        copy.deleteOnExit()
        oldFile.copyTo(copy, overwrite = true)

        val patch = DatabaseDiffGenerator().generate(oldFile, copy)

        assertTrue(patch.summary.isEmpty)
        assertTrue(patch.tables.values.all { it.isEmpty })
        assertEquals(patch.fromFingerprint, patch.toFingerprint)
    }

    @Test
    fun generatedDatabasesRecordTheirFingerprint() {
        val file = PatchTestFixtures.buildNewDatabase()
        JapaneseDatabase.open(file).use { db ->
            assertEquals(
                DatabaseFingerprint.compute(file),
                db.queryMeta(Schema.Meta.STATE_FINGERPRINT)
            )
        }
    }

    @Test
    fun patchRoundTripsThroughJson() {
        val patch = DatabaseDiffGenerator().generate(
            PatchTestFixtures.buildOldDatabase(),
            PatchTestFixtures.buildNewDatabase()
        )
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .encodeToString(DatabasePatch.serializer(), patch)
        val decoded = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString(DatabasePatch.serializer(), json)

        assertEquals(patch.summary, decoded.summary)
        assertEquals(patch.fromFingerprint, decoded.fromFingerprint)
        assertEquals(patch.toFingerprint, decoded.toFingerprint)
        assertEquals(patch.tables.keys, decoded.tables.keys)
        assertEquals(patch.tables.getValue("kanji").inserts, decoded.tables.getValue("kanji").inserts)
        assertEquals(patch.tables.getValue("vocab").deletes, decoded.tables.getValue("vocab").deletes)
    }
}
