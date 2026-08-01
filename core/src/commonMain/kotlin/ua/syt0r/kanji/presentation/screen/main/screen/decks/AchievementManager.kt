package ua.syt0r.kanji.presentation.screen.main.screen.decks

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// ============================================
// KAITEYO v2.1 - ACHIEVEMENT MANAGER (Core)
// Tracks progress, filtering, events
// ============================================

class AchievementManagerCore {
    private val achievements = allAchievements.associateBy { it.id }.toMutableMap()
    private val unlockListeners = mutableListOf<(Achievement) -> Unit>()
    private val unlockHistory = mutableListOf<Achievement>()

    fun addListener(callback: (Achievement) -> Unit) { unlockListeners.add(callback) }
    fun removeListener(callback: (Achievement) -> Unit) { unlockListeners.remove(callback) }

    fun getAchievement(id: String): Achievement? = achievements[id]
    fun getAllAchievements(): List<Achievement> = achievements.values.toList().sortedBy { it.name }
    fun getAchievementsByCategory(c: AchievementCategory): List<Achievement> =
        achievements.values.filter { it.category == c }
    fun getUnlockHistory(): List<Achievement> = unlockHistory.toList()

    fun getFilteredAchievements(filter: AchievementFilter): List<Achievement> {
        var results = achievements.values.toList()
        if (filter.searchQuery.isNotBlank()) {
            val q = filter.searchQuery.lowercase()
            results = results.filter {
                it.name.lowercase().contains(q) || it.description.lowercase().contains(q) ||
                it.category.displayName.lowercase().contains(q)
            }
        }
        filter.category?.let { cat -> results = results.filter { it.category == cat } }
        filter.difficulty?.let { d -> results = results.filter { it.difficulty == d } }
        filter.rarity?.let { r -> results = results.filter { it.rarity == r } }
        when (filter.status) {
            AchievementStatusFilter.All -> {}
            AchievementStatusFilter.Unlocked -> results = results.filter { it.isUnlocked }
            AchievementStatusFilter.Locked -> results = results.filter { !it.isUnlocked }
            AchievementStatusFilter.InProgress -> results = results.filter { it.progress > 0 && !it.isUnlocked }
            AchievementStatusFilter.NotStarted -> results = results.filter { it.progress == 0 && !it.isUnlocked }
        }
        if (!filter.showHidden) results = results.filter { !it.isHidden }
        results = when (filter.sortBy) {
            AchievementSort.Name -> results.sortedBy { it.name }
            AchievementSort.Category -> results.sortedBy { it.category.name }
            AchievementSort.Difficulty -> results.sortedBy { it.difficulty.ordinal }
            AchievementSort.Rarity -> results.sortedBy { it.rarity.ordinal }
            AchievementSort.Progress -> results.sortedByDescending { it.progressPercent }
            AchievementSort.RecentlyUnlocked -> results.sortedByDescending { it.unlockedAt ?: Instant.DISTANT_PAST }
            AchievementSort.XP -> results.sortedByDescending { it.xpReward }
        }
        val desc = listOf(AchievementSort.Progress, AchievementSort.RecentlyUnlocked, AchievementSort.XP)
        if (!filter.sortAscending && filter.sortBy !in desc) results = results.reversed()
        return results
    }

    fun getProgress(): AchievementProgress {
        val unlocked = achievements.values.filter { it.isUnlocked }
        val totalXp = unlocked.sumOf { it.xpReward }
        val byCat = achievements.values.groupBy { it.category }
        return AchievementProgress(
            totalAchievements = achievements.size,
            unlockedCount = unlocked.size, totalXp = totalXp,
            bronzeCount = unlocked.count { it.difficulty == AchievementDifficulty.Bronze },
            silverCount = unlocked.count { it.difficulty == AchievementDifficulty.Silver },
            goldCount = unlocked.count { it.difficulty == AchievementDifficulty.Gold },
            platinumCount = unlocked.count { it.difficulty == AchievementDifficulty.Platinum },
            diamondCount = unlocked.count { it.difficulty == AchievementDifficulty.Diamond },
            recentUnlocks = unlockHistory.takeLast(10).reversed(),
            completionPercent = if (achievements.isNotEmpty())
                (unlocked.size.toFloat() / achievements.size * 100f).coerceIn(0f, 100f) else 0f,
            categoryProgress = byCat.mapValues { (_, cats) ->
                val u = cats.count { it.isUnlocked }
                if (cats.isNotEmpty()) u.toFloat() / cats.size * 100f else 0f
            }
        )
    }

    fun updateProgress(id: String, progress: Int): Achievement? {
        val a = achievements[id] ?: return null
        if (a.isUnlocked) return a
        val updated = a.copy(progress = progress.coerceIn(0, a.steps), lastUpdated = Clock.System.now())
        achievements[id] = updated
        return if (updated.progress >= updated.steps) unlockAchievement(id) else null
    }

    fun incrementProgress(id: String, amount: Int = 1): Achievement? {
        val a = achievements[id] ?: return null
        return updateProgress(id, a.progress + amount)
    }

    fun unlockAchievement(id: String): Achievement? {
        val a = achievements[id] ?: return null
        if (a.isUnlocked) return null
        val now = Clock.System.now()
        val updated = a.copy(isUnlocked = true, unlockedAt = now, progress = a.steps, lastUpdated = now)
        achievements[id] = updated
        unlockHistory.add(updated)
        unlockListeners.forEach { it(updated) }
        if (getProgress().completionPercent >= 100f) unlockAchievement("completion-100")
        return updated
    }

    fun resetAll() {
        achievements.replaceAll { _, v ->
            v.copy(isUnlocked = false, unlockedAt = null, progress = 0, lastUpdated = Clock.System.now())
        }
        unlockHistory.clear()
    }
}
