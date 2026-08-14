package ua.syt0r.kanji.desktop.engine.updates.kjd

import io.kaiteyo.kjd.api.JapaneseDatabase
import io.kaiteyo.kjd.db.DatabaseWriter
import io.kaiteyo.kjd.model.Character
import io.kaiteyo.kjd.model.CharacterType
import io.kaiteyo.kjd.model.Component
import io.kaiteyo.kjd.model.EntityId
import io.kaiteyo.kjd.model.JlptClassification
import io.kaiteyo.kjd.model.Kanji
import io.kaiteyo.kjd.model.Meaning
import io.kaiteyo.kjd.model.Reading
import io.kaiteyo.kjd.model.Sense
import io.kaiteyo.kjd.model.SourceRef
import io.kaiteyo.kjd.model.Stroke
import io.kaiteyo.kjd.model.VocabularyEntry
import io.kaiteyo.kjd.model.VocabularyReading
import io.kaiteyo.kjd.normalize.JapaneseNormalizer
import io.kaiteyo.kjd.patch.DatabaseDiffGenerator
import io.kaiteyo.kjd.patch.DatabaseFingerprint
import io.kaiteyo.kjd.patch.DatabasePatch
import io.kaiteyo.kjd.resolve.CanonicalDatabaseBuilder
import io.kaiteyo.kjd.source.BuiltinSources
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end test of the KJD database updater:
 *
 *   1. builds two real KJD databases (v1, v2) with DatabaseWriter;
 *   2. generates a real incremental patch with DatabaseDiffGenerator;
 *   3. serves it through fake checker/downloader (no network);
 *   4. runs the full check → download → apply pipeline;
 *   5. verifies the bundled database now fingerprints as v2 and the new
 *      vocabulary entry is queryable through the public API.
 */
class KjdDatabaseUpdaterTest {

    // ------------------------------------------------------------
    // Fixtures — real KJD data, mirroring the pipeline tests
    // ------------------------------------------------------------

    private fun kanjiEat() = Kanji(
        id = EntityId("kanji:食"),
        character = Character(
            id = EntityId("kanji:食"),
            literal = "食",
            codepoint = 0x98DF,
            normalized = JapaneseNormalizer.toNfc("食"),
            characterType = CharacterType.Kanji,
            strokeCount = 9
        ),
        onReadings = listOf(Reading("ショク", "on")),
        kunReadings = listOf(Reading("く.う", "kun")),
        meanings = listOf(Meaning("eat", "en")),
        grade = 2,
        jlpt = listOf(JlptClassification(5, SourceRef("tanos-jlpt", isCanonical = true))),
        strokeCount = 9,
        strokes = listOf(
            Stroke(id = EntityId("stroke:kanji:食:1"), index = 1, characterId = EntityId("kanji:食"), path = "M1,1 L2,2")
        ),
        components = listOf(Component(id = EntityId("component:kanji:食:食"), character = "食", role = "radical"))
    )

    private fun vocab(
        id: String,
        expression: String,
        reading: String,
        gloss: String,
        senseIndex: Int
    ): Pair<VocabularyEntry, Sense> {
        val entry = VocabularyEntry(
            id = EntityId(id),
            expression = expression,
            readings = listOf(
                VocabularyReading(value = reading, isKanaOnly = JapaneseNormalizer.isKanaOnly(reading))
            ),
            kanjiIds = expression.filter { it in "食水" }.map { EntityId("kanji:$it") }
        )
        val sense = Sense(
            id = EntityId("sense:$id:$senseIndex"),
            vocabularyId = EntityId(id),
            index = senseIndex,
            glosses = listOf(Meaning(gloss, "en"))
        )
        return entry to sense
    }

    /** Builds a KJD database file for the given vocab additions. */
    private fun buildDatabase(
        target: File,
        withDiningHall: Boolean
    ) {
        val builder = CanonicalDatabaseBuilder()
        builder.upsertKanji(kanjiEat())

        val (eatEntry, eatSense) = vocab("vocab:jmdict_1000990", "食べる", "たべる", "to eat", 0)
        builder.upsertVocab(eatEntry)
        builder.addSense(eatSense)

        if (withDiningHall) {
            val (diningEntry, diningSense) = vocab("vocab:jmdict_1001000", "食堂", "しょくどう", "dining hall", 0)
            builder.upsertVocab(diningEntry)
            builder.addSense(diningSense)
        }

        DatabaseWriter().write(builder.snapshot(), target, BuiltinSources.all)
    }

    // ------------------------------------------------------------
    // Fakes
    // ------------------------------------------------------------

    private class FakeChecker(
        private val patch: DatabasePatch,
        private val feedVersion: String
    ) : KjdPatchChecker {
        override suspend fun check(
            channel: ua.syt0r.kanji.desktop.engine.updates.UpdateChannel,
            state: KjdDatabaseState
        ): KjdPatchCheckResult {
            if (state.fingerprint != patch.fromFingerprint) {
                return KjdPatchCheckResult.NoPatch(channel, "2026-01-01T00:00:00Z", feedVersion)
            }
            return KjdPatchCheckResult.PatchAvailable(
                channel = channel,
                entry = KjdPatchFeed.PatchEntry(
                    fromDatabaseVersion = "1.0.0",
                    fromFingerprint = patch.fromFingerprint,
                    toDatabaseVersion = feedVersion,
                    toFingerprint = patch.toFingerprint,
                    url = "https://example.invalid/patch.json",
                    sha256 = "b".repeat(64),
                    sizeBytes = 1234
                ),
                feed = KjdPatchFeed(
                    databaseVersion = feedVersion,
                    patches = listOf(
                        KjdPatchFeed.PatchEntry(
                            fromDatabaseVersion = "1.0.0",
                            fromFingerprint = patch.fromFingerprint,
                            toDatabaseVersion = feedVersion,
                            toFingerprint = patch.toFingerprint
                        )
                    )
                )
            )
        }
    }

    /** Checker that never matches — used where the database is absent anyway. */
    private class StubChecker : KjdPatchChecker {
        override suspend fun check(
            channel: ua.syt0r.kanji.desktop.engine.updates.UpdateChannel,
            state: KjdDatabaseState
        ): KjdPatchCheckResult =
            KjdPatchCheckResult.NoPatch(channel, "2026-01-01T00:00:00Z", "1.1.0")
    }

    private class FakeDownloader(private val patchJson: String) : KjdPatchDownloader {
        override suspend fun download(
            entry: KjdPatchFeed.PatchEntry,
            targetDir: File,
            onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit
        ): KjdPatchDownloadResult {
            val file = File(targetDir, "kjd-patch-${entry.toDatabaseVersion}.json")
            file.writeText(patchJson)
            return KjdPatchDownloadResult.Downloaded(
                file = file,
                patch = Json { ignoreUnknownKeys = true }
                    .decodeFromString<DatabasePatch>(patchJson),
                sizeBytes = patchJson.length.toLong()
            )
        }
    }

    // ------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------

    private fun tempDir(name: String): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "kjd-updater-$name-${System.nanoTime()}")
        dir.mkdirs()
        dir.deleteOnExit()
        return dir
    }

    private suspend fun awaitState(
        updater: KjdDatabaseUpdater,
        predicate: (KjdDatabaseUpdateState) -> Boolean
    ) {
        withTimeout(15_000) {
            updater.state.first { predicate(it) }
        }
    }

    @Test
    fun fullPipelinePatchesBundledDatabase() = runBlocking {
        val root = tempDir("pipeline")
        val v1 = File(root, "v1.db")
        val v2 = File(root, "v2.db")
        buildDatabase(v1, withDiningHall = false)
        buildDatabase(v2, withDiningHall = true)

        // Real patch between the two real releases.
        val patch = DatabaseDiffGenerator().generate(v1, v2)
        assertTrue(patch.summary.inserted > 0, "Patch should add the dining-hall vocab row")

        // The bundled database is a copy of v1 placed where the locator expects it.
        val dataDir = File(root, "data")
        dataDir.mkdirs()
        val bundled = File(dataDir, "kjd-japanese.db")
        v1.copyTo(bundled, overwrite = true)

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val updater = KjdDatabaseUpdater(
            scope = scope,
            checker = FakeChecker(patch, "1.1.0"),
            downloader = FakeDownloader(
                Json { encodeDefaults = true }.encodeToString(DatabasePatch.serializer(), patch)
            ),
            locator = KjdDatabaseLocator(dataDir),
            dataDir = dataDir
        )

        updater.checkOnStartup("stable")
        awaitState(updater) { it is KjdDatabaseUpdateState.Available }
        assertTrue(updater.state.value is KjdDatabaseUpdateState.Available)

        updater.download()
        awaitState(updater) { it is KjdDatabaseUpdateState.ReadyToApply }
        assertTrue(updater.state.value is KjdDatabaseUpdateState.ReadyToApply)

        updater.apply()
        awaitState(updater) { it is KjdDatabaseUpdateState.UpToDate }

        // The bundled database must now fingerprint as v2 ...
        assertEquals(
            DatabaseFingerprint.compute(v2),
            DatabaseFingerprint.compute(bundled),
            "Patched database should match the v2 fingerprint"
        )

        // ... and the new vocabulary entry must be queryable.
        JapaneseDatabase.open(bundled).use { db ->
            assertNotNull(db.lookupVocabulary("食堂"), "New vocab 食堂 should exist after patch")
            assertNotNull(db.lookupVocabulary("食べる"), "Existing vocab 食べる must survive the patch")
            assertNull(db.lookupVocabulary("不存在"), "Unrelated lookup stays empty")
        }

        // The applied state was persisted for the next launch.
        assertEquals("1.1.0", updater.appliedState.databaseVersion)
        assertEquals(patch.toFingerprint, updater.appliedState.fingerprint)
        scope.cancel()
    }

    @Test
    fun alreadyAppliedDatabaseIsIdempotent() = runBlocking {
        val root = tempDir("idempotent")
        val v1 = File(root, "v1.db")
        val v2 = File(root, "v2.db")
        buildDatabase(v1, withDiningHall = false)
        buildDatabase(v2, withDiningHall = true)

        val patch = DatabaseDiffGenerator().generate(v1, v2)

        // Bundled database is already v2 AND the applied-state file records it
        // (simulates the next launch after a successful apply) — the updater
        // must skip without touching the feed.
        val dataDir = File(root, "data")
        dataDir.mkdirs()
        val bundled = File(dataDir, "kjd-japanese.db")
        v2.copyTo(bundled, overwrite = true)
        File(dataDir, "kjd-applied-state.json").writeText(
            """
            {
              "databaseVersion": "1.1.0",
              "fingerprint": "${patch.toFingerprint}",
              "appliedAt": "2026-01-01T00:00:00Z"
            }
            """.trimIndent()
        )

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val updater = KjdDatabaseUpdater(
            scope = scope,
            checker = FakeChecker(patch, "1.1.0"),
            downloader = FakeDownloader("{}"),
            locator = KjdDatabaseLocator(dataDir),
            dataDir = dataDir
        )

        // The DB already matches the recorded applied state.
        assertEquals("1.1.0", updater.appliedState.databaseVersion)
        assertEquals(patch.toFingerprint, updater.appliedState.fingerprint)

        // The checker finds no patch whose fromFingerprint matches v2 (it is
        // already the target), so the flow settles UpToDate without touching
        // the database file.
        updater.checkOnStartup("stable")
        awaitState(updater) { it is KjdDatabaseUpdateState.UpToDate }
        assertTrue(updater.state.value is KjdDatabaseUpdateState.UpToDate)
        scope.cancel()
    }

    @Test
    fun noBundledDatabaseReportsGracefully() = runBlocking {
        val root = tempDir("no-db")
        val dataDir = File(root, "data") // exists but contains no database

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val updater = KjdDatabaseUpdater(
            scope = scope,
            checker = StubChecker(),
            downloader = FakeDownloader("{}"),
            locator = KjdDatabaseLocator(dataDir),
            dataDir = dataDir
        )

        updater.checkOnStartup("stable")
        assertTrue(updater.state.value is KjdDatabaseUpdateState.NoBundledDatabase)
        scope.cancel()
    }
}
