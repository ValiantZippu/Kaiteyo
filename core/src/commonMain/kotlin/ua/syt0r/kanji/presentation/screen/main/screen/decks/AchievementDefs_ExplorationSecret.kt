package ua.syt0r.kanji.presentation.screen.main.screen.decks

// KAITEYO ACHIEVEMENT DEFS - Exploration & Secret

fun buildExplorationAchievements() = listOf(
    Achievement("customized-theme", "Stylist", "Customize your first theme",
        "\uD83C\uDFA8", AchievementCategory.Exploration, AchievementDifficulty.Bronze, xpReward = 10),
    Achievement("created-deck", "Deck Architect", "Create your first custom deck",
        "\uD83D\uDCC1", AchievementCategory.Exploration, AchievementDifficulty.Bronze, xpReward = 10),
    Achievement("imported-deck", "Importer", "Import a deck from external source",
        "\uD83D\uDCE5", AchievementCategory.Exploration, AchievementDifficulty.Silver, xpReward = 25),
    Achievement("exported-deck", "Exporter", "Export a deck for sharing or backup",
        "\uD83D\uDCE4", AchievementCategory.Exploration, AchievementDifficulty.Silver, xpReward = 25),
    Achievement("used-shortcuts", "Keyboard Ninja", "Use 5 different keyboard shortcuts",
        "\u2328\uFE0F", AchievementCategory.Exploration, AchievementDifficulty.Silver, xpReward = 30, steps = 5),
    Achievement("created-layout", "Layout Designer", "Create a custom review layout",
        "\uD83D\uDCD0", AchievementCategory.Exploration, AchievementDifficulty.Gold, xpReward = 50),
    Achievement("custom-tags-10", "Tag Master", "Create 10 custom tags",
        "\uD83C\uDFF7\uFE0F", AchievementCategory.Exploration, AchievementDifficulty.Silver, xpReward = 25, steps = 10),
    Achievement("all-features", "Feature Explorer", "Visit every major feature at least once",
        "\uD83D\uDD8D\uFE0F", AchievementCategory.Exploration, AchievementDifficulty.Gold, xpReward = 100),
    Achievement("used-bulk-actions", "Power User", "Use bulk actions on 50+ cards at once",
        "\u26A1", AchievementCategory.Exploration, AchievementDifficulty.Gold, xpReward = 75),
    Achievement("first-backup", "Safe and Sound", "Create your first backup",
        "\uD83D\uDCBE", AchievementCategory.Exploration, AchievementDifficulty.Bronze, xpReward = 10)
)

fun buildSecretAchievements() = listOf(
    Achievement("easter-egg", "Secret Discoverer", "Find a secret easter egg in Kaiteyo",
        "\uD83E\uDD5A", AchievementCategory.Secret, AchievementDifficulty.Gold, AchievementRarity.Epic,
        xpReward = 100, isHidden = true, isSecret = true),
    Achievement("night-owl", "Night Owl", "Study after midnight for 7 consecutive days",
        "\uD83E\uDD89", AchievementCategory.Secret, AchievementDifficulty.Silver, AchievementRarity.Rare,
        xpReward = 50, steps = 7, isHidden = true),
    Achievement("speed-demon", "Speed Demon", "Answer 20 cards in under 60 seconds",
        "\uD83C\uDFC3", AchievementCategory.Secret, AchievementDifficulty.Gold, AchievementRarity.Epic,
        xpReward = 100, steps = 20, isHidden = true),
    Achievement("perfectionist", "Perfectionist", "Get 100% accuracy in a 50+ card session",
        "\u2728", AchievementCategory.Secret, AchievementDifficulty.Platinum, AchievementRarity.Epic,
        xpReward = 250, isHidden = true)
)
