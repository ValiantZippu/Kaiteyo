package io.kaiteyo.kjd.patch

import io.kaiteyo.kjd.api.JapaneseDatabase
import io.kaiteyo.kjd.db.Schema
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [DatabasePatcher]: successful apply (with fingerprint
 * verification and index rebuild), end-state equivalence with a fresh
 * generation, safety rejections and idempotency.
 */
class DatabasePatcherTest {

    private fun tempCopy(source: File): File {
        val copy = File.createTempFile("kjd-patch-target-", ".db")
        copy.deleteOnExit()
        source.copyTo(copy, overwrite = true)
        return copy
    }

    private fun oldToNewPatch(): DatabasePatch = DatabaseDiffGenerator().generate(
        PatchTestFixtures.buildOldDatabase(),
        PatchTestFixtures.buildNewDatabase()
    )

    @Test
    fun applyBringsTargetToTargetState() {
        val oldFile = PatchTestFixtures.buildOldDatabase()
        val newFile = PatchTestFixtures.buildNewDatabase()
        val patch = DatabaseDiffGenerator().generate(oldFile, newFile)
        val target = tempCopy(oldFile)

        val result = DatabasePatcher().apply(target, patch)
        assertTrue(result is PatchResult.Applied)
        assertEquals(patch.summary, (result as PatchResult.Applied).summary)

        // Content matches the new release.
        JapaneseDatabase.open(target).use { db ->
            assertNotNull(db.lookupKanji("山"), "inserted kanji present")
            val water = db.lookupKanji("水")!!
            assertTrue(water.meanings.any { it.value == "Wednesday" }, "updated kanji meanings")
            assertNull(db.lookupVocabulary("食べる"), "deleted vocab removed")
            val dining = db.lookupVocabulary("食堂")
            assertNotNull(dining, "inserted vocab present")
            assertEquals("dining hall", dining.senses.first().glosses.first().value)

            // Derived indexes rebuilt — Japanese search + FTS meaning search.
            assertTrue(db.search("食堂").any { it.displayText == "食堂" })
            assertTrue(db.search("Wednesday").any { it.displayText == "水" })

            // Metadata updated.
            assertEquals(Schema.SCHEMA_VERSION, db.schemaVersion())
            assertEquals(patch.toFingerprint, db.queryMeta(Schema.Meta.STATE_FINGERPRINT))
        }

        // The patched database is content-equivalent to a fresh generation.
        assertEquals(
            DatabaseFingerprint.compute(newFile),
            DatabaseFingerprint.compute(target)
        )
    }

    @Test
    fun applyIsIdempotent() {
        val patch = oldToNewPatch()
        val target = tempCopy(PatchTestFixtures.buildOldDatabase())

        assertTrue(DatabasePatcher().apply(target, patch) is PatchResult.Applied)
        assertEquals(PatchResult.AlreadyApplied, DatabasePatcher().apply(target, patch))
    }

    @Test
    fun applyingToUnrelatedDatabaseIsRejectedAndNonDestructive() {
        val patch = oldToNewPatch()
        val unrelated = PatchTestFixtures.buildUnrelatedDatabase()
        val target = tempCopy(unrelated)
        val before = DatabaseFingerprint.compute(target)

        assertFailsWith<IllegalStateException> { DatabasePatcher().apply(target, patch) }

        // Byte-for-byte identical state: nothing was written.
        assertEquals(before, DatabaseFingerprint.compute(target))
        JapaneseDatabase.open(target).use { db ->
            assertNull(db.lookupKanji("山"), "no partial apply of foreign content")
            assertNotNull(db.lookupKanji("火"))
        }
    }

    @Test
    fun forceCannotBypassPostApplyVerification() {
        val patch = oldToNewPatch()
        val target = tempCopy(PatchTestFixtures.buildUnrelatedDatabase())
        val before = DatabaseFingerprint.compute(target)

        // force bypasses the pre-check, but the post-apply verification still
        // refuses to commit a database that does not converge to the target
        // state — and rolls everything back.
        assertFailsWith<IllegalStateException> {
            DatabasePatcher().apply(target, patch, force = true)
        }
        assertEquals(before, DatabaseFingerprint.compute(target))
        JapaneseDatabase.open(target).use { db ->
            assertNull(db.lookupKanji("山"), "nothing was committed")
        }
    }

    @Test
    fun preFingerprintDatabaseAppliesCleanly() {
        // A database that predates fingerprint recording (meta row missing)
        // still verifies by content and applies without force.
        val oldFile = PatchTestFixtures.buildOldDatabase()
        val target = tempCopy(oldFile)
        DriverManager.getConnection("jdbc:sqlite:${target.absolutePath}").use { connection ->
            connection.createStatement().use {
                it.execute("DELETE FROM meta WHERE key = 'state_fingerprint'")
            }
        }
        val patch = DatabaseDiffGenerator().generate(oldFile, PatchTestFixtures.buildNewDatabase())

        val result = DatabasePatcher().apply(target, patch)
        assertTrue(result is PatchResult.Applied)
        assertEquals(patch.toFingerprint, DatabaseFingerprint.compute(target))
        JapaneseDatabase.open(target).use { db ->
            assertNotNull(db.lookupVocabulary("食堂"))
        }
    }

    @Test
    fun schemaMismatchIsAlwaysRejectedEvenWithForce() {
        val patch = oldToNewPatch()
        val target = tempCopy(PatchTestFixtures.buildOldDatabase())

        // Simulate a schema-1 database.
        DriverManager.getConnection("jdbc:sqlite:${target.absolutePath}").use { connection ->
            connection.createStatement().use { it.execute("PRAGMA user_version = 1") }
        }

        assertFailsWith<IllegalArgumentException> {
            DatabasePatcher().apply(target, patch, force = true)
        }
    }

    @Test
    fun patchAppliesFromJsonFile() {
        val patch = oldToNewPatch()
        val patchFile = File.createTempFile("kjd-patch-", ".json")
        patchFile.deleteOnExit()
        patchFile.writeText(
            kotlinx.serialization.json.Json { prettyPrint = true }
                .encodeToString(DatabasePatch.serializer(), patch)
        )
        val target = tempCopy(PatchTestFixtures.buildOldDatabase())

        val decoded = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString(DatabasePatch.serializer(), patchFile.readText())
        val result = DatabasePatcher().apply(target, decoded)
        assertTrue(result is PatchResult.Applied)

        JapaneseDatabase.open(target).use { db ->
            assertNotNull(db.lookupVocabulary("食堂"))
            assertNull(db.lookupVocabulary("食べる"))
        }
    }
}
