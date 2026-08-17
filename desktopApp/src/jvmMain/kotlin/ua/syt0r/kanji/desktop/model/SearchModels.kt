package ua.syt0r.kanji.desktop.model

import kotlinx.serialization.Serializable

// ============================================
// SEARCH QUERY AST
// Field filters (meaning:water, jlpt:n5, ...),
// boolean composition (AND implicit, OR, NOT,
// parentheses) and negation. Evaluated by the
// SearchEngine against DesktopCard.
// ============================================

sealed interface SearchExpr {
    /** Field filter:  field:value */
    @Serializable
    data class Field(
        val field: SearchField,
        val operator: FieldOperator = FieldOperator.Eq,
        val value: String = ""
    ) : SearchExpr

    /** Bare text token, matched against the whole card. */
    @Serializable
    data class Text(val value: String) : SearchExpr

    /** All/Any conjunction of children. */
    @Serializable
    data class Group(
        val mode: MatchAll = MatchAll.MatchAll,
        val children: List<SearchExpr> = emptyList()
    ) : SearchExpr

    /** Negation wrapper. */
    @Serializable
    data class Not(val expr: SearchExpr) : SearchExpr
}

@Serializable
enum class SearchField {
    Id, Character, Meaning, Reading, OnReading, KunReading,
    Radical, Component, Stroke, Jlpt, Grade, Frequency,
    Tag, Flag, Status, Favorite, Note, Deck, Accuracy,
    Interval, Lapses, Reps, Due, Ease, Kind
}

@Serializable
enum class FieldOperator { Eq, NotEq, Gt, Lt, Gte, Lte, Contains, StartsWith }

/** A saved / pinned / recent search filter. */
@Serializable
data class SavedFilter(
    val id: String,
    val name: String,
    val query: String,
    val pinned: Boolean = false,
    val lastUsedAt: Long = 0L,
    val useCount: Int = 0
)

@Serializable
enum class BrowserViewMode { Grid, List, Details }

@Serializable
data class BrowserSort(
    val field: SortField = SortField.Character,
    val ascending: Boolean = true
)

@Serializable
enum class SortField { Character, Meaning, Jlpt, Grade, Frequency, Strokes, Accuracy, Interval, Lapses, Due, Status }
