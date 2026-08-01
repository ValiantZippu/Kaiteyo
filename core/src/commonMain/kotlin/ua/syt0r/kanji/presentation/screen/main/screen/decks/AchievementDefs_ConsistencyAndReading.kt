package ua.syt0r.kanji.presentation.screen.main.screen.decks

// KAITEYO ACHIEVEMENT DEFS - Consistency & Reading

fun buildConsistencyAchievements() = listOf(
    Achievement("streak-3", "Getting Started", "Maintain a 3-day study streak",
        "\uD83C\uDF31", AchievementCategory.Consistency, AchievementDifficulty.Bronze, xpReward = 10, steps = 3),
    Achievement("streak-7", "Week Warrior", "Maintain a 7-day study streak",
        "\uD83D\uDD25", AchievementCategory.Consistency, AchievementDifficulty.Bronze, xpReward = 25, steps = 7),
    Achievement("streak-14", "Two Weeks Strong", "Maintain a 14-day study streak",
        "\uD83D\uDCAA", AchievementCategory.Consistency, AchievementDifficulty.Silver, xpReward = 50, steps = 14),
    Achievement("streak-30", "Monthly Master", "Maintain a 30-day study streak",
        "\u2B50", AchievementCategory.Consistency, AchievementDifficulty.Silver, xpReward = 100, steps = 30),
    Achievement("streak-60", "Dedicated Scholar", "Maintain a 60-day study streak",
        "\uD83C\uDF1F", AchievementCategory.Consistency, AchievementDifficulty.Gold, xpReward = 200, steps = 60),
    Achievement("streak-100", "Century of Learning", "Maintain a 100-day study streak",
        "\uD83D\uDC8E", AchievementCategory.Consistency, AchievementDifficulty.Gold, AchievementRarity.Rare,
        xpReward = 500, steps = 100),
    Achievement("streak-200", "Half-Year Hero", "Maintain a 200-day study streak",
        "\uD83D\uDC51", AchievementCategory.Consistency, AchievementDifficulty.Platinum, AchievementRarity.Epic,
        xpReward = 1000, steps = 200),
    Achievement("streak-365", "Year of Kaiteyo", "Maintain a 365-day study streak",
        "\uD83C\uDF89", AchievementCategory.Consistency, AchievementDifficulty.Platinum, AchievementRarity.Legendary,
        xpReward = 2500, steps = 365),
    Achievement("streak-500", "Iron Will", "Maintain a 500-day study streak",
        "\uD83C\uDFC6", AchievementCategory.Consistency, AchievementDifficulty.Diamond, AchievementRarity.Legendary,
        xpReward = 5000, steps = 500),
    Achievement("streak-1000", "Millennium Scholar", "Maintain a 1,000-day study streak",
        "\uD83D\uDC64", AchievementCategory.Consistency, AchievementDifficulty.Diamond, AchievementRarity.Mythic,
        xpReward = 10000, steps = 1000, isSecret = true)
)

fun buildReadingAchievements() = listOf(
    Achievement("first-reading", "First Reading", "Read your first Japanese text",
        "\uD83D\uDCD6", AchievementCategory.Reading, AchievementDifficulty.Bronze, xpReward = 5),
    Achievement("first-story", "Story Time", "Complete reading your first story",
        "\uD83D\uDCDA", AchievementCategory.Reading, AchievementDifficulty.Bronze, xpReward = 10),
    Achievement("stories-5", "Bookworm", "Read 5 complete stories",
        "\uD83D\uDCD5", AchievementCategory.Reading, AchievementDifficulty.Silver, xpReward = 25, steps = 5),
    Achievement("stories-10", "Page Turner", "Read 10 complete stories",
        "\uD83D\uDCD7", AchievementCategory.Reading, AchievementDifficulty.Silver, xpReward = 50, steps = 10),
    Achievement("stories-25", "Avid Reader", "Read 25 complete stories",
        "\uD83D\uDCD8", AchievementCategory.Reading, AchievementDifficulty.Gold, xpReward = 100, steps = 25),
    Achievement("stories-50", "Reading Master", "Read 50 complete stories",
        "\uD83D\uDCD9", AchievementCategory.Reading, AchievementDifficulty.Gold, xpReward = 200, steps = 50),
    Achievement("stories-100", "Library of Kaiteyo", "Read 100 complete stories",
        "\uD83D\uDCDA", AchievementCategory.Reading, AchievementDifficulty.Platinum, AchievementRarity.Rare,
        xpReward = 500, steps = 100)
)
