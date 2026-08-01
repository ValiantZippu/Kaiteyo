package ua.syt0r.kanji.presentation.screen.main.screen.decks

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

// ============================================
// KAITEYO v2.1 - ACHIEVEMENT SYSTEM
// 70+ achievements, 10 categories, 5 difficulties
// 6 rarity tiers, XP rewards, badges, progress
// ============================================

// --- DATA MODELS ---

@Serializable
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val category: AchievementCategory,
    val difficulty: AchievementDifficulty,
    val rarity: AchievementRarity = AchievementRarity.Common,
    val xpReward: Int = 10,
    val isHidden: Boolean = false,
    val isSecret: Boolean = false,
    val prerequisites: List<String> = emptyList(),
    val steps: Int = 1,
    var progress: Int = 0,
    var isUnlocked: Boolean = false,
    var unlockedAt: Instant? = null,
    var lastUpdated: Instant = Clock.System.now()
) {
    val progressPercent: Float
        get() = (progress.toFloat() / steps.coerceAtLeast(1)).coerceIn(0f, 1f)
    val isComplete: Boolean get() = progress >= steps || isUnlocked
    val badge: String get() = when (difficulty) {
        AchievementDifficulty.Bronze -> "\uD83E\uDD4B"
        AchievementDifficulty.Silver -> "\uD83E\uDD48"
        AchievementDifficulty.Gold -> "\uD83E\uDD47"
        AchievementDifficulty.Platinum -> "\uD83D\uDC8E"
        AchievementDifficulty.Diamond -> "\uD83D\uDC51"
    }
}

enum class AchievementCategory(
    val displayName: String, val icon: String, val description: String
) {
    Learning("Learning", "\uD83D\uDCDA", "Core learning milestones"),
    Writing("Writing", "\u270D\uFE0F", "Writing practice achievements"),
    Consistency("Consistency", "\uD83D\uDD25", "Streak and consistency rewards"),
    Reading("Reading", "\uD83D\uDCD6", "Reading comprehension milestones"),
    Vocabulary("Vocabulary", "\uD83D\uDDE3\uFE0F", "Vocabulary mastery"),
    Kanji("Kanji", "\u6F22", "Kanji proficiency achievements"),
    Exploration("Exploration", "\uD83D\uDD0D", "Feature exploration rewards"),
    Secret("Secret", "\u2B50", "Hidden achievements"),
    Mastery("Mastery", "\uD83C\uDFC6", "Overall mastery milestones"),
    Social("Social", "\uD83C\uDF10", "Community features")
}

enum class AchievementDifficulty(
    val displayName: String, val color: String, val xpMultiplier: Int
) {
    Bronze("Bronze", "#CD7F32", 1),
    Silver("Silver", "#C0C0C0", 2),
    Gold("Gold", "#FFD700", 5),
    Platinum("Platinum", "#E5E4E2", 10),
    Diamond("Diamond", "#B9F2FF", 25)
}

enum class AchievementRarity(
    val displayName: String, val color: String, val threshold: Float
) {
    Common("Common", "#808080", 0.5f),
    Uncommon("Uncommon", "#4CAF50", 0.25f),
    Rare("Rare", "#2196F3", 0.1f),
    Epic("Epic", "#9C27B0", 0.05f),
    Legendary("Legendary", "#FF9800", 0.01f),
    Mythic("Mythic", "#F44336", 0.001f)
}

// --- FILTER MODELS ---

data class AchievementProgress(
    val totalAchievements: Int = 0,
    val unlockedCount: Int = 0,
    val totalXp: Int = 0,
    val bronzeCount: Int = 0,
    val silverCount: Int = 0,
    val goldCount: Int = 0,
    val platinumCount: Int = 0,
    val diamondCount: Int = 0,
    val recentUnlocks: List<Achievement> = emptyList(),
    val completionPercent: Float = 0f,
    val categoryProgress: Map<AchievementCategory, Float> = emptyMap()
)

data class AchievementUnlockEvent(
    val achievement: Achievement,
    val isNewUnlock: Boolean = true,
    val timestamp: Instant = Clock.System.now()
)

data class AchievementFilter(
    val searchQuery: String = "",
    val category: AchievementCategory? = null,
    val difficulty: AchievementDifficulty? = null,
    val rarity: AchievementRarity? = null,
    val status: AchievementStatusFilter = AchievementStatusFilter.All,
    val sortBy: AchievementSort = AchievementSort.Name,
    val sortAscending: Boolean = true,
    val showHidden: Boolean = false
)

enum class AchievementStatusFilter(val displayName: String) {
    All("All"), Unlocked("Unlocked"), Locked("Locked"),
    InProgress("In Progress"), NotStarted("Not Started")
}

enum class AchievementSort(val displayName: String) {
    Name("Name"), Category("Category"), Difficulty("Difficulty"),
    Rarity("Rarity"), Progress("Progress"),
    RecentlyUnlocked("Recently Unlocked"), XP("XP Reward")
}
