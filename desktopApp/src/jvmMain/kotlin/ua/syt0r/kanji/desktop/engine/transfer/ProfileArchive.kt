package ua.syt0r.kanji.desktop.engine.transfer

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.history.ActivityEntry
import ua.syt0r.kanji.desktop.engine.theming.ThemePresets
import ua.syt0r.kanji.desktop.model.CollectionDef
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ReviewLogEntry
import ua.syt0r.kanji.desktop.model.SavedFilter
import ua.syt0r.kanji.desktop.model.StudyDaySummary
import ua.syt0r.kanji.desktop.model.ToastKind
import ua.syt0r.kanji.desktopApp.SavedWindowBounds
import ua.syt0r.kanji.desktopApp.WindowStateStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// ============================================
// NATIVE PROFILE ARCHIVE
// A fully lossless "profile" backup. Captures
// every piece of user data the suite owns — cards,
// review log, daily summaries, collections, saved
// filters, settings, the active theme and the
// activity ledger — into one portable JSON / ZIP.
// ============================================

@Serializable
data class ProfileData(
    val version: Int = 1,
    val exportedAt: String = "",
    val app: String = "kaiteyo-desktop",
    val cards: List<DesktopCard> = emptyList(),
    val reviewLog: List<ReviewLogEntry> = emptyList(),
    val summaries: List<StudyDaySummary> = emptyList(),
    val collections: List<CollectionDef> = emptyList(),
    val savedFilters: List<SavedFilter> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
    val themeId: String = "",
    val activity: List<ActivityEntry> = emptyList(),
    val windowBounds: SavedWindowBounds = SavedWindowBounds(),
    val metadata: Map<String, String> = emptyMap()
) {
    val cardCount: Int get() = cards.size
    val reviewCount: Int get() = reviewLog.size
    val studyDays: Int get() = summaries.size
    val collectionCount: Int get() = collections.size
}

object ProfileArchive {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    const val PROFILE_ENTRY = "profile.json"
    const val DEFAULT_EXTENSION = "kaiteyozip"

    // ------------------------------------------------------------
    // JSON
    // ------------------------------------------------------------

    fun toJson(data: ProfileData): String = json.encodeToString(data)

    fun fromJson(text: String): ProfileData = json.decodeFromString<ProfileData>(text)

    // ------------------------------------------------------------
    // ZIP (profile.json inside an archive)
    // ------------------------------------------------------------

    fun toZip(data: ProfileData): ByteArray {
        val payload = toJson(data).toByteArray(Charsets.UTF_8)
        return ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zip ->
                zip.putNextEntry(ZipEntry(PROFILE_ENTRY))
                zip.write(payload)
                zip.closeEntry()
            }
            baos.toByteArray()
        }
    }

    fun fromZip(bytes: ByteArray): Result<ProfileData> = runCatching {
        var result: ProfileData? = null
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == PROFILE_ENTRY) {
                    result = fromJson(zip.readBytes().toString(Charsets.UTF_8))
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        result ?: error("Archive does not contain $PROFILE_ENTRY")
    }

    /** Auto-generated backup filename, e.g. kaiteyo-backup-20260802-2130.kaiteyozip */
    fun timestampedName(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "kaiteyo-backup-%04d%02d%02d-%02d%02d.%s".format(
            now.year, now.monthNumber, now.dayOfMonth, now.hour, now.minute, DEFAULT_EXTENSION
        )
    }
}

// ============================================
// AppState capture / restore extensions
// ============================================

fun AppState.capture(): ProfileData = ProfileData(
    exportedAt = Clock.System.now().toString(),
    cards = cards.toList(),
    reviewLog = reviewLog.toList(),
    summaries = summaries.toList(),
    collections = collections.collections,
    savedFilters = filterStore.saved,
    settings = settings.snapshot(),
    themeId = activeThemeId,
    activity = activityLog.entries.asReversed(),
    windowBounds = WindowStateStore.read(),
    metadata = mapOf(
        "cards" to cards.size.toString(),
        "reviews" to reviewLog.size.toString(),
        "studyDays" to summaries.size.toString()
    )
)

fun AppState.restore(data: ProfileData) {
    cards.clear()
    cards.addAll(data.cards)
    library.saveCards(data.cards)
    reviewLog.clear()
    reviewLog.addAll(data.reviewLog)
    summaries.clear()
    summaries.addAll(data.summaries)
    collections.load(data.collections)
    filterStore.loadSaved(data.savedFilters)
    if (data.settings.isNotEmpty()) settings.restore(data.settings)
    if (data.themeId.isNotBlank() && ThemePresets.all.any { it.id == data.themeId }) applyTheme(data.themeId)
    // Restore the window placement captured in the backup. It takes effect on
    // the next launch (the running window keeps its geometry until the user
    // moves it, which then supersedes the restored bounds).
    if (data.windowBounds.isUsable) WindowStateStore.save(data.windowBounds)
    activityLog.load(data.activity)
    activityLog.record(ActivityCategory.Import, "Restored profile backup (${data.cards.size} cards)")
    toastHost.show("Profile restored — ${data.cards.size} cards, ${data.reviewLog.size} reviews", kind = ToastKind.Success)
}
