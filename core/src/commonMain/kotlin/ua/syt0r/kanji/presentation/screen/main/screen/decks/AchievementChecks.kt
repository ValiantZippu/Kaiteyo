package ua.syt0r.kanji.presentation.screen.main.screen.decks

// ============================================
// KAITEYO v2.1 - ACHIEVEMENT CHECK FUNCTIONS
// Convenience methods for checking milestone
// thresholds across all achievement categories
// ============================================

fun AchievementManagerCore.checkReviews(reviewCount: Int): List<Achievement> {
    val n = mutableListOf<Achievement>()
    if (reviewCount >= 1) incrementProgress("first-review", 1)?.let { n.add(it) }
    if (reviewCount >= 100) updateProgress("reviews-100", 100)?.let { n.add(it) }
    if (reviewCount >= 500) updateProgress("reviews-500", 500)?.let { n.add(it) }
    if (reviewCount >= 1000) updateProgress("reviews-1000", 1000)?.let { n.add(it) }
    if (reviewCount >= 5000) updateProgress("reviews-5000", 5000)?.let { n.add(it) }
    if (reviewCount >= 10000) updateProgress("reviews-10000", 10000)?.let { n.add(it) }
    if (reviewCount >= 50000) updateProgress("reviews-50000", 50000)?.let { n.add(it) }
    if (reviewCount >= 100000) updateProgress("reviews-100000", 100000)?.let { n.add(it) }
    return n
}

fun AchievementManagerCore.checkWriting(writeCount: Int): List<Achievement> {
    val n = mutableListOf<Achievement>()
    if (writeCount >= 1) incrementProgress("first-kanji", 1)?.let { n.add(it) }
    if (writeCount >= 50) updateProgress("kanji-50", 50)?.let { n.add(it) }
    if (writeCount >= 100) updateProgress("kanji-100", 100)?.let { n.add(it) }
    if (writeCount >= 500) updateProgress("kanji-500", 500)?.let { n.add(it) }
    if (writeCount >= 1000) updateProgress("kanji-1000", 1000)?.let { n.add(it) }
    if (writeCount >= 5000) updateProgress("kanji-5000", 5000)?.let { n.add(it) }
    return n
}

fun AchievementManagerCore.checkStreak(streak: Int): List<Achievement> {
    val n = mutableListOf<Achievement>()
    if (streak >= 3) updateProgress("streak-3", 3)?.let { n.add(it) }
    if (streak >= 7) updateProgress("streak-7", 7)?.let { n.add(it) }
    if (streak >= 14) updateProgress("streak-14", 14)?.let { n.add(it) }
    if (streak >= 30) updateProgress("streak-30", 30)?.let { n.add(it) }
    if (streak >= 60) updateProgress("streak-60", 60)?.let { n.add(it) }
    if (streak >= 100) updateProgress("streak-100", 100)?.let { n.add(it) }
    if (streak >= 200) updateProgress("streak-200", 200)?.let { n.add(it) }
    if (streak >= 365) updateProgress("streak-365", 365)?.let { n.add(it) }
    if (streak >= 500) updateProgress("streak-500", 500)?.let { n.add(it) }
    if (streak >= 1000) updateProgress("streak-1000", 1000)?.let { n.add(it) }
    return n
}

fun AchievementManagerCore.checkVocabulary(wordCount: Int): List<Achievement> {
    val n = mutableListOf<Achievement>()
    if (wordCount >= 10) updateProgress("words-10", 10)?.let { n.add(it) }
    if (wordCount >= 50) updateProgress("words-50", 50)?.let { n.add(it) }
    if (wordCount >= 100) updateProgress("words-100", 100)?.let { n.add(it) }
    if (wordCount >= 500) updateProgress("words-500", 500)?.let { n.add(it) }
    if (wordCount >= 1000) updateProgress("words-1000", 1000)?.let { n.add(it) }
    if (wordCount >= 2000) updateProgress("words-2000", 2000)?.let { n.add(it) }
    if (wordCount >= 5000) updateProgress("words-5000", 5000)?.let { n.add(it) }
    if (wordCount >= 10000) updateProgress("words-10000", 10000)?.let { n.add(it) }
    return n
}

fun AchievementManagerCore.checkSessions(sessionCount: Int): List<Achievement> {
    val n = mutableListOf<Achievement>()
    if (sessionCount >= 1) incrementProgress("first-session", 1)?.let { n.add(it) }
    if (sessionCount >= 10) updateProgress("sessions-10", 10)?.let { n.add(it) }
    if (sessionCount >= 50) updateProgress("sessions-50", 50)?.let { n.add(it) }
    if (sessionCount >= 100) updateProgress("sessions-100", 100)?.let { n.add(it) }
    if (sessionCount >= 500) updateProgress("sessions-500", 500)?.let { n.add(it) }
    if (sessionCount >= 1000) updateProgress("sessions-1000", 1000)?.let { n.add(it) }
    return n
}

fun AchievementManagerCore.checkXp(totalXp: Int): List<Achievement> {
    val n = mutableListOf<Achievement>()
    if (totalXp >= 1000) updateProgress("xp-1000", 1000)?.let { n.add(it) }
    if (totalXp >= 5000) updateProgress("xp-5000", 5000)?.let { n.add(it) }
    if (totalXp >= 10000) updateProgress("xp-10000", 10000)?.let { n.add(it) }
    if (totalXp >= 25000) updateProgress("xp-25000", 25000)?.let { n.add(it) }
    if (totalXp >= 50000) updateProgress("xp-50000", 50000)?.let { n.add(it) }
    return n
}

fun AchievementManagerCore.checkStories(storyCount: Int): List<Achievement> {
    val n = mutableListOf<Achievement>()
    if (storyCount >= 1) incrementProgress("first-story", 1)?.let { n.add(it) }
    if (storyCount >= 5) updateProgress("stories-5", 5)?.let { n.add(it) }
    if (storyCount >= 10) updateProgress("stories-10", 10)?.let { n.add(it) }
    if (storyCount >= 25) updateProgress("stories-25", 25)?.let { n.add(it) }
    if (storyCount >= 50) updateProgress("stories-50", 50)?.let { n.add(it) }
    if (storyCount >= 100) updateProgress("stories-100", 100)?.let { n.add(it) }
    return n
}

fun calculateAchievementRarity(totalUsers: Int, unlockedBy: Int): AchievementRarity {
    if (totalUsers <= 0) return AchievementRarity.Common
    val pct = unlockedBy.toFloat() / totalUsers
    return when {
        pct <= 0.001f -> AchievementRarity.Mythic
        pct <= 0.01f -> AchievementRarity.Legendary
        pct <= 0.05f -> AchievementRarity.Epic
        pct <= 0.1f -> AchievementRarity.Rare
        pct <= 0.25f -> AchievementRarity.Uncommon
        else -> AchievementRarity.Common
    }
}
