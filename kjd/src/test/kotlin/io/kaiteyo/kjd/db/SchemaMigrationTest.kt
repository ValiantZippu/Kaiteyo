package io.kaiteyo.kjd.db

import io.kaiteyo.kjd.api.JapaneseDatabase
import io.kaiteyo.kjd.model.Character
import io.kaiteyo.kjd.model.CharacterType
import io.kaiteyo.kjd.model.EntityId
import io.kaiteyo.kjd.model.JlptClassification
import io.kaiteyo.kjd.model.Kanji
import io.kaiteyo.kjd.model.Meaning
import io.kaiteyo.kjd.model.Reading
import io.kaiteyo.kjd.model.SourceRef
import io.kaiteyo.kjd.normalize.JapaneseNormalizer
import io.kaiteyo.kjd.resolve.CanonicalDatabaseBuilder
import io.kaiteyo.kjd.source.BuiltinSources
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies the migration framework: an older (v1) database is upgraded to
 * v2 in place, gaining the FTS/search/component tables while every existing
 * language record is preserved.
 */
class SchemaMigrationTest {

    private fun v1DatabaseFile(): File {
        val builder = CanonicalDatabaseBuilder()
        builder.upsertKanji(
            Kanji(
                id = EntityId("kanji:食"),
                character = Character(
                    id = EntityId("kanji:食"),
                    literal = "食",
                    codepoint = 0x98DF,
                    normalized = JapaneseNormalizer.toNfc("食"),
                    characterType = CharacterType.Kanji
                ),
                onReadings = listOf(Reading("ショク", "on")),
                meanings = listOf(Meaning("eat", "en")),
                jlpt = listOf(JlptClassification(5, SourceRef("tanos-jlpt", isCanonical = true)))
            )
        )
        val file = File(System.getProperty("java.io.tmpdir"), "kjd-migrate-${System.nanoTime()}.db")
        file.deleteOnExit()
        DatabaseWriter().write(builder.snapshot(), file, BuiltinSources.all)

        // Simulate a pre-v2 database: remove the v2-only tables and rewind the
        // version markers, exactly as an old release would look.
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { connection ->
            connection.createStatement().use { stmt ->
                stmt.execute("DROP TABLE IF EXISTS kanji_fts")
                stmt.execute("DROP TABLE IF EXISTS vocab_fts")
                stmt.execute("DROP TABLE IF EXISTS search_index")
                stmt.execute("DROP TABLE IF EXISTS component")
                stmt.execute("PRAGMA user_version = 1")
            }
            connection.createStatement().use { stmt ->
                stmt.execute("UPDATE meta SET value = '1' WHERE key = 'schema_version'")
            }
        }
        return file
    }

    @Test
    fun openMigratesOldDatabaseInPlace() {
        val file = v1DatabaseFile()
        JapaneseDatabase.open(file).use { db ->
            // Migration ran automatically on open.
            assertEquals(Schema.SCHEMA_VERSION, db.schemaVersion())
            assertEquals(Schema.SCHEMA_VERSION, db.queryMeta("schema_version")?.toInt())

            // Original data survived.
            val kanji = db.lookupKanji("食")
            assertNotNull(kanji)
            assertEquals("eat", kanji.meanings.first().value)

            // New v2 capabilities work after migration.
            assertTrue(db.search("eat").any { it.displayText == "食" }, "FTS meaning search after migration")
            assertTrue(db.search("ショク").any { it.displayText == "食" }, "reading search after migration")
        }
    }

    @Test
    fun explicitMigrateIsIdempotent() {
        val file = v1DatabaseFile()
        JapaneseDatabase.migrate(file)
        // Second migration must be a no-op (already at current version).
        JapaneseDatabase.migrate(file)
        JapaneseDatabase.open(file).use { db ->
            assertEquals(Schema.SCHEMA_VERSION, db.schemaVersion())
            assertEquals(1, db.kanjiCount())
        }
    }

    @Test
    fun freshDatabaseIsAlreadyAtCurrentVersion() {
        val builder = CanonicalDatabaseBuilder()
        builder.upsertKanji(
            Kanji(
                id = EntityId("kanji:山"),
                character = Character(
                    id = EntityId("kanji:山"),
                    literal = "山",
                    codepoint = 0x5C71,
                    normalized = JapaneseNormalizer.toNfc("山"),
                    characterType = CharacterType.Kanji
                ),
                onReadings = listOf(Reading("サン", "on")),
                meanings = listOf(Meaning("mountain", "en"))
            )
        )
        val file = File(System.getProperty("java.io.tmpdir"), "kjd-fresh-${System.nanoTime()}.db")
        file.deleteOnExit()
        DatabaseWriter().write(builder.snapshot(), file, BuiltinSources.all)

        JapaneseDatabase.open(file).use { db ->
            assertEquals(Schema.SCHEMA_VERSION, db.schemaVersion())
            assertTrue(db.search("mountain").any { it.displayText == "山" })
        }
    }
}
