package ua.syt0r.kanji.core.knowledge

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================================
// LEARNER PROFILE STORE
// ------------------------------------------------------------
// Persisted profile choice + custom presentation overrides.
// Mirrors KanjiCardLayoutStore: corrupt or stale blobs fall back
// to defaults — a hand-edited preference can never break layout.
// The Custom profile's overrides are only honored when the active
// profile is Custom; switching profiles never destroys the saved
// custom configuration.
// ============================================================

@Serializable
data class LearnerProfilePreference(
    val profile: LearnerProfile = LearnerProfile.Intermediate,
    /** Custom overrides; honored only while [profile] is [LearnerProfile.Custom]. */
    val customPresentation: ProfilePresentation? = null
) {
    /** The presentation to actually use for the stored profile. */
    fun effectivePresentation(): ProfilePresentation = when (profile) {
        LearnerProfile.Custom -> customPresentation ?: LearnerProfileCatalog.defaultsFor(LearnerProfile.Custom)
        else -> LearnerProfileCatalog.defaultsFor(profile)
    }

    fun withCustomPresentation(presentation: ProfilePresentation): LearnerProfilePreference =
        copy(profile = LearnerProfile.Custom, customPresentation = presentation)

    /** Drops overrides when switching away from Custom (kept for return). */
    fun withProfile(profile: LearnerProfile): LearnerProfilePreference =
        copy(profile = profile)

    /** Clamps to known profiles; custom overrides are kept only for Custom. */
    fun sanitized(): LearnerProfilePreference {
        val knownProfile = LearnerProfile.entries.firstOrNull { it == profile } ?: LearnerProfile.Intermediate
        return copy(
            profile = knownProfile,
            customPresentation = if (knownProfile == LearnerProfile.Custom) customPresentation else null
        )
    }
}

class LearnerProfileStore(
    private val preferences: PreferencesContract.AppPreferences
) {

    suspend fun load(): LearnerProfilePreference {
        val raw = preferences.learnerProfileJson.get()
        if (raw.isBlank()) return LearnerProfilePreference()
        return runCatching {
            Json.decodeFromString<LearnerProfilePreference>(raw).sanitized()
        }.getOrDefault(LearnerProfilePreference())
    }

    suspend fun save(preference: LearnerProfilePreference) {
        preferences.learnerProfileJson.set(Json.encodeToString(preference.sanitized()))
    }

    suspend fun saveProfile(profile: LearnerProfile) {
        save(load().withProfile(profile))
    }

    suspend fun reset() {
        preferences.learnerProfileJson.set("")
    }
}
