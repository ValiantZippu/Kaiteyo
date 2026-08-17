package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.ui.graphics.Color

// ============================================
// KAITEYO DECK MODELS
// Shared deck data model used by the live deck
// browser routes (features/DeckFeatureScreens.kt).
// ============================================

data class KaiteyoDeck(
    val id: String = "deck_001",
    val name: String = "N5 Kanji",
    val description: String = "JLPT N5 level kanji characters",
    val parentId: String? = null,
    val color: Color = Color(0xFFC2FC8B),
    val icon: String = "漢",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isArchived: Boolean = false,
    val isVirtual: Boolean = false,
    val isDynamic: Boolean = false,
    val isSmart: Boolean = false,
    val cardCount: Int = 120,
    val newCount: Int = 23,
    val reviewCount: Int = 45,
    val dueCount: Int = 12,
    val learningCount: Int = 8,
    val matureCount: Int = 89,
    val accuracy: Float = 0.85f,
    val retention: Float = 0.91f,
    val averageInterval: Int = 45,
    val totalStudyTime: Long = 3600000L,
    val createdAt: String = "2026-01-01",
    val lastStudied: String = "2026-07-28",
    val children: MutableList<KaiteyoDeck> = mutableListOf(),
    val filters: DeckFilters? = null
)

data class DeckFilters(
    val tags: List<String> = emptyList(),
    val flags: List<String> = emptyList(),
    val minInterval: Int = 0,
    val maxInterval: Int = 36500,
    val minDifficulty: Float = 0f,
    val maxDifficulty: Float = 1f,
    val onlyNew: Boolean = false,
    val onlyDue: Boolean = false,
    val onlySuspended: Boolean = false,
    val onlyFlagged: Boolean = false,
    val onlyTagged: String = "",
    val regex: String = ""
)
