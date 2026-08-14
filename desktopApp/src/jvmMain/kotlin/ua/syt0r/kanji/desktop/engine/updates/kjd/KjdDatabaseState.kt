package ua.syt0r.kanji.desktop.engine.updates.kjd

import io.kaiteyo.kjd.api.JapaneseDatabase
import io.kaiteyo.kjd.patch.DatabaseFingerprint
import java.io.File

// ============================================
// KJD DATABASE STATE + LOCATOR
//
// [KjdDatabaseLocator] resolves the *bundled*
// KJD language database — the file the app
// ships with and patches in place. On first
// run the bundled classpath asset (if any) is
// copied into the user data dir so patching is
// always applied to a writable copy, never the
// app install directory.
// ============================================

data class KjdDatabaseState(
    val schemaVersion: Int,
    val fingerprint: String,
    val databaseVersion: String?
)

class KjdDatabaseLocator(
    private val dataDir: File = kjdDatabaseDir()
) {

    /** The writable copy the updater patches. */
    val databaseFile: File = File(dataDir, KJD_DATABASE_NAME)

    /**
     * Resolves the database the updater operates on:
     * the user-data copy if it exists, otherwise the
     * bundled classpath asset (seeded on first run).
     * Returns null when neither is available.
     */
    fun resolveDatabaseFile(): File? {
        if (databaseFile.isFile) return databaseFile
        val bundled = javaClass.classLoader.getResourceAsStream(BUNDLED_RESOURCE_PATH) ?: return null
        dataDir.mkdirs()
        bundled.use { input -> databaseFile.outputStream().use { output -> input.copyTo(output) } }
        return databaseFile
    }

    /** Reads the current schema/fingerprint/version from a database file. */
    fun readState(file: File): KjdDatabaseState? = runCatching {
        JapaneseDatabase.open(file).use { db ->
            KjdDatabaseState(
                schemaVersion = db.schemaVersion(),
                fingerprint = DatabaseFingerprint.compute(file),
                databaseVersion = db.generatorVersion()
            )
        }
    }.getOrNull()

    companion object {
        /** Classpath path of the bundled KJD database asset. */
        const val BUNDLED_RESOURCE_PATH: String = "kjd/kjd-japanese.db"
    }
}

/** User data directory that holds the patched KJD database. */
fun kjdDatabaseDir(): File = File(System.getProperty("user.home"), ".kaiteyo/kjd")

/** Download staging + applied-state records live here (mirrors the app updater). */
fun kjdUpdatesDataDir(): File = File(System.getProperty("user.home"), ".kaiteyo/updates/kjd")

/** Base URL of the KJD patch feed directory (per-channel files appended). */
const val KJD_PATCH_FEED_BASE_URL: String =
    "https://github.com/ValiantZippu/Kaiteyo/releases/download/update-feed"

/** Default filename of the bundled database. */
const val KJD_DATABASE_NAME: String = "kjd-japanese.db"
