package ua.syt0r.kanji.presentation.screen.main.screen.decks

// KAITEYO ACHIEVEMENT DEFS - Mastery & Social

fun buildMasteryAchievements() = listOf(
    Achievement("first-session", "First Steps", "Complete your first study session",
        "\uD83D\uDC76", AchievementCategory.Mastery, AchievementDifficulty.Bronze, xpReward = 5),
    Achievement("sessions-10", "Regular Student", "Complete 10 study sessions",
        "\uD83C\uDF92", AchievementCategory.Mastery, AchievementDifficulty.Bronze, xpReward = 10, steps = 10),
    Achievement("sessions-50", "Dedicated Student", "Complete 50 study sessions",
        "\uD83D\uDCD3", AchievementCategory.Mastery, AchievementDifficulty.Silver, xpReward = 25, steps = 50),
    Achievement("sessions-100", "Study Veteran", "Complete 100 study sessions",
        "\uD83D\uDCDA", AchievementCategory.Mastery, AchievementDifficulty.Silver, xpReward = 50, steps = 100),
    Achievement("sessions-500", "Lifelong Learner", "Complete 500 study sessions",
        "\uD83C\uDF93", AchievementCategory.Mastery, AchievementDifficulty.Gold, xpReward = 200, steps = 500),
    Achievement("sessions-1000", "Master of Kaiteyo", "Complete 1,000 study sessions",
        "\uD83C\uDFC6", AchievementCategory.Mastery, AchievementDifficulty.Platinum, AchievementRarity.Epic,
        xpReward = 500, steps = 1000),
    Achievement("accuracy-80", "Sharp Mind", "Maintain 80%+ accuracy over 100 reviews",
        "\uD83C\uDFAF", AchievementCategory.Mastery, AchievementDifficulty.Silver, xpReward = 50),
    Achievement("accuracy-90", "Precision Learner", "Maintain 90%+ accuracy over 500 reviews",
        "\uD83D\uDC8E", AchievementCategory.Mastery, AchievementDifficulty.Gold, xpReward = 100),
    Achievement("accuracy-95", "Almost Perfect", "Maintain 95%+ accuracy over 1,000 reviews",
        "\uD83D\uDC51", AchievementCategory.Mastery, AchievementDifficulty.Platinum, AchievementRarity.Epic,
        xpReward = 250),
    Achievement("xp-1000", "XP Apprentice", "Earn 1,000 XP from achievements",
        "\u26A1", AchievementCategory.Mastery, AchievementDifficulty.Bronze, xpReward = 25, steps = 1000),
    Achievement("xp-5000", "XP Warrior", "Earn 5,000 XP from achievements",
        "\uD83D\uDD25", AchievementCategory.Mastery, AchievementDifficulty.Silver, xpReward = 50, steps = 5000),
    Achievement("xp-10000", "XP Champion", "Earn 10,000 XP from achievements",
        "\uD83D\uDCAA", AchievementCategory.Mastery, AchievementDifficulty.Gold, xpReward = 100, steps = 10000),
    Achievement("xp-25000", "XP Legend", "Earn 25,000 XP from achievements",
        "\uD83C\uDFC6", AchievementCategory.Mastery, AchievementDifficulty.Platinum, xpReward = 500, steps = 25000),
    Achievement("xp-50000", "XP Immortal", "Earn 50,000 XP from achievements",
        "\uD83D\uDC51", AchievementCategory.Mastery, AchievementDifficulty.Diamond, AchievementRarity.Legendary,
        xpReward = 1000, steps = 50000),
    Achievement("completion-100", "100% Completion", "Unlock every achievement in Kaiteyo",
        "\uD83C\uDF1F", AchievementCategory.Mastery, AchievementDifficulty.Diamond, AchievementRarity.Mythic,
        xpReward = 10000, isSecret = true, isHidden = true)
)

fun buildSocialAchievements() = listOf(
    Achievement("first-sync", "Sync Starter", "Sync your data for the first time",
        "\uD83D\uDD04", AchievementCategory.Social, AchievementDifficulty.Bronze, xpReward = 10),
    Achievement("sync-10", "Cloud Regular", "Sync your data 10 times",
        "\u2601\uFE0F", AchievementCategory.Social, AchievementDifficulty.Silver, xpReward = 25, steps = 10),
    Achievement("sync-100", "Cloud Master", "Sync your data 100 times",
        "\uD83C\uDF24\uFE0F", AchievementCategory.Social, AchievementDifficulty.Gold, xpReward = 50, steps = 100)
)

fun buildAllAchievements(): List<Achievement> =
    buildLearningAchievements() +
    buildWritingAchievements() +
    buildConsistencyAchievements() +
    buildReadingAchievements() +
    buildVocabularyAchievements() +
    buildKanjiAchievements() +
    buildExplorationAchievements() +
    buildSecretAchievements() +
    buildMasteryAchievements() +
    buildSocialAchievements()

val allAchievements: List<Achievement> by lazy { buildAllAchievements() }
