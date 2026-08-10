package ua.syt0r.kanji.desktop.engine.updates

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ============================================
// UPDATE POLICY
// Channel selection, version comparison and the
// rollback window. Pure logic — no I/O.
// ============================================

/** Rollback guard: how many days the previous version is kept after applying
 *  an update, and how many "launches" must succeed before it is discarded. */
data class RollbackPolicy(
    val keepPreviousDays: Int = 14,
    val successfulLaunchesRequired: Int = 3,
    val confirmRollbackAfterDays: Int = 3
)

data class AppVersionInfo(
    val versionName: String,
    val versionCode: Int
)

class UpdatePolicy(
    private val rollback: RollbackPolicy = RollbackPolicy()
) {

    private val _channel = MutableStateFlow(UpdateChannel.Stable)
    val channel: StateFlow<UpdateChannel> = _channel.asStateFlow()

    fun setChannel(channel: UpdateChannel) {
        _channel.value = channel
    }

    /** True when [latest] is strictly newer than [current]. Falls back to
     *  semantic comparison when version codes are unavailable. */
    fun isUpdateAvailable(current: AppVersionInfo, latest: UpdateManifest.LatestVersion): Boolean {
        if (latest.versionCode > current.versionCode) return true
        if (latest.versionCode == current.versionCode) return false
        return compareSemantic(latest.version, current.versionName) > 0
    }

    /** Minimum-version guard: refuse manifests whose min_app_version is newer
     *  than the running app (the update would require a hop we can't make). */
    fun canApply(current: AppVersionInfo, latest: UpdateManifest.LatestVersion): Boolean {
        if (latest.minAppVersion.isEmpty()) return true
        return compareSemantic(latest.minAppVersion, current.versionName) <= 0
    }

    fun rollbackWindowDays(): Int = rollback.keepPreviousDays

    private fun compareSemantic(a: String, b: String): Int {
        val pa = a.split('.').mapNotNull { it.toIntOrNull() }
        val pb = b.split('.').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val va = pa.getOrElse(i) { 0 }
            val vb = pb.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }
}
