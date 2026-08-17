package ua.syt0r.kanji.desktop.engine.search

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.FieldOperator
import ua.syt0r.kanji.desktop.model.MatchAll
import ua.syt0r.kanji.desktop.model.SearchExpr
import ua.syt0r.kanji.desktop.model.SearchField
import ua.syt0r.kanji.desktop.model.SrsStatus

// ============================================
// SEARCH ENGINE
// Tokenizer + recursive-descent parser producing a
// SearchExpr AST, then a stateless evaluator over
// DesktopCard. Supports the full spec:
//   meaning:water  reading:すい  jlpt:n5  grade:1
//   tag:jlpt-n5  flag:red  status:learning  favorite:yes
//   accuracy:>0.8  interval:>30  lapses:>=2  due:today
//   strokes:12..15  freq:<1000
// Boolean composition: implicit AND, OR, NOT, ( ).
// ============================================

object SearchEngine {

    fun parse(query: String): Result<SearchExpr> = runCatching {
        val tokens = tokenize(query)
        val parser = Parser(tokens)
        val expr = parser.parseExpression()
        parser.expectEnd()
        expr
    }

    fun matches(card: DesktopCard, expr: SearchExpr, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Boolean =
        eval(card, expr, today)

    fun matches(card: DesktopCard, query: String, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Boolean {
        val expr = parse(query).getOrNull() ?: return false
        return eval(card, expr, today)
    }

    // ------------------------------------------------------------
    // Evaluation
    // ------------------------------------------------------------

    private fun eval(card: DesktopCard, expr: SearchExpr, today: LocalDate): Boolean = when (expr) {
        is SearchExpr.Text -> {
            val q = expr.value
            card.searchableText.contains(q) ||
                card.onReadings.any { it.contains(q, ignoreCase = true) } ||
                card.kunReadings.any { it.contains(q, ignoreCase = true) } ||
                card.tags.any { it.contains(q, ignoreCase = true) }
        }

        is SearchExpr.Field -> evalField(card, expr, today)

        is SearchExpr.Not -> !eval(card, expr.expr, today)

        is SearchExpr.Group -> {
            val results = expr.children.map { eval(card, it, today) }
            if (expr.mode == MatchAll.MatchAll) results.all { it } else results.any { it }
        }
    }

    private fun evalField(card: DesktopCard, field: SearchExpr.Field, today: LocalDate): Boolean {
        val value = field.value
        val num = value.toDoubleOrNull()

        fun numericCompare(actual: Double?): Boolean {
            if (actual == null) return false
            return when (field.operator) {
                FieldOperator.Eq -> actual == num
                FieldOperator.NotEq -> actual != num
                FieldOperator.Gt -> actual > num!!
                FieldOperator.Lt -> actual < num!!
                FieldOperator.Gte -> actual >= num!!
                FieldOperator.Lte -> actual <= num!!
                else -> actual.toString().contains(value, ignoreCase = true)
            }
        }

        fun stringCompare(actual: String): Boolean = when (field.operator) {
            FieldOperator.Eq -> actual.equals(value, ignoreCase = true)
            FieldOperator.NotEq -> !actual.equals(value, ignoreCase = true)
            FieldOperator.Contains -> actual.contains(value, ignoreCase = true)
            FieldOperator.StartsWith -> actual.startsWith(value, ignoreCase = true)
            FieldOperator.Gt -> actual.compareTo(value, ignoreCase = true) > 0
            FieldOperator.Lt -> actual.compareTo(value, ignoreCase = true) < 0
            FieldOperator.Gte -> actual.compareTo(value, ignoreCase = true) >= 0
            FieldOperator.Lte -> actual.compareTo(value, ignoreCase = true) <= 0
        }

        fun listContains(values: List<String>): Boolean = values.any { it.equals(value, ignoreCase = true) }

        fun truthy(actual: Boolean): Boolean = when (value.lowercase()) {
            "true", "yes", "1", "*" -> actual
            "false", "no", "0" -> !actual
            else -> false
        }

        return when (field.field) {
            SearchField.Id -> card.id.equals(value, ignoreCase = true)
            SearchField.Character -> stringCompare(card.character)
            SearchField.Meaning -> stringCompare(card.meaning)
            SearchField.Reading -> card.readings.any { stringCompare(it) }
            SearchField.OnReading -> card.onReadings.any { stringCompare(it) }
            SearchField.KunReading -> card.kunReadings.any { stringCompare(it) }
            SearchField.Radical -> card.radicals.any { stringCompare(it) }
            SearchField.Component -> card.components.any { stringCompare(it) }
            SearchField.Stroke -> numericCompare(card.strokeCount.toDouble())
            SearchField.Jlpt -> numericCompare(card.jlpt?.toDouble())
            SearchField.Grade -> numericCompare(card.grade?.toDouble())
            SearchField.Frequency -> numericCompare(card.frequency?.toDouble())
            SearchField.Tag -> card.tags.any { it.equals(value, ignoreCase = true) } ||
                listContains(card.tags) ||
                card.tags.any { it.contains(value, ignoreCase = true) && field.operator == FieldOperator.Contains }
            SearchField.Flag -> card.flags.any { it.equals(value, ignoreCase = true) || (field.operator == FieldOperator.Contains && value == "*") }
            SearchField.Status -> card.status.name.equals(value, ignoreCase = true) ||
                statusAlias(card.status, value.lowercase())
            SearchField.Favorite -> truthy(card.favorite)
            SearchField.Note -> card.note.contains(value, ignoreCase = true)
            SearchField.Deck -> stringCompare(card.deckId)
            SearchField.Accuracy -> numericCompare(card.accuracy.toDouble())
            SearchField.Interval -> numericCompare(card.intervalDays)
            SearchField.Lapses -> numericCompare(card.lapses.toDouble())
            SearchField.Reps -> numericCompare(card.reps.toDouble())
            SearchField.Ease -> numericCompare(card.ease)
            SearchField.Due -> dueCompare(card, value, today)
            SearchField.Kind -> card.contentKind.name.equals(value, ignoreCase = true) ||
                card.contentKind.label.equals(value, ignoreCase = true)
        }
    }

    private fun statusAlias(status: SrsStatus, value: String): Boolean = when (value) {
        "young", "mature" -> status == SrsStatus.Review
        "due" -> true
        "unlearned" -> status == SrsStatus.New
        "forgotten" -> status == SrsStatus.Relearning
        else -> false
    }

    private fun dueCompare(card: DesktopCard, value: String, today: LocalDate): Boolean {
        val due = card.dueAt?.toLocalDateTime(TimeZone.currentSystemDefault())?.date ?: return value == "none"
        return when (value.lowercase()) {
            "today" -> due == today
            "tomorrow" -> due == today.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
            "past", "overdue" -> due < today
            "week" -> due in today..today.plus(7, kotlinx.datetime.DateTimeUnit.DAY)
            "none" -> card.dueAt == null
            else -> due.toString().contains(value)
        }
    }

    // ------------------------------------------------------------
    // Tokenizer + Parser
    // ------------------------------------------------------------

    private sealed interface Token {
        data class Word(val value: String) : Token
        data class FieldTok(val field: SearchField, val operator: FieldOperator, val value: String) : Token
        object LParen : Token
        object RParen : Token
        data class Bool(val isOr: Boolean) : Token
        object Not : Token
    }

    private fun tokenize(query: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val s = query

        fun readWord(): String {
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i]
                if (c == ' ' || c == '(' || c == ')') break
                if (c == '"') {
                    i++
                    while (i < s.length && s[i] != '"') { sb.append(s[i]); i++ }
                    i++
                    continue
                }
                sb.append(c)
                i++
            }
            return sb.toString()
        }

        while (i < s.length) {
            val c = s[i]
            when {
                c == ' ' -> i++
                c == '(' -> { tokens.add(Token.LParen); i++ }
                c == ')' -> { tokens.add(Token.RParen); i++ }
                c == '!' || (c == '-' && s.getOrNull(i + 1) == '-') -> { tokens.add(Token.Not); i += if (c == '!') 1 else 2 }
                else -> {
                    // Check for field: prefix.
                    val rest = s.substring(i)
                    val fieldMatch = FIELD_PATTERNS.firstOrNull { (name, _) ->
                        rest.startsWith(name + ":")
                    }
                    if (fieldMatch != null) {
                        val (name, field) = fieldMatch
                        i += name.length + 1
                        val opAndValue = readWord()
                        val (op, value) = splitOperator(opAndValue)
                        tokens.add(Token.FieldTok(field, op, value))
                    } else {
                        val word = readWord()
                        when (word.lowercase()) {
                            "and" -> i += 0 // implicit AND; ignore
                            "or" -> tokens.add(Token.Bool(true))
                            "not" -> tokens.add(Token.Not)
                            else -> tokens.add(Token.Word(word))
                        }
                    }
                }
            }
        }
        return tokens
    }

    private fun splitOperator(raw: String): Pair<FieldOperator, String> {
        if (raw.startsWith(">=")) return FieldOperator.Gte to raw.substring(2)
        if (raw.startsWith("<=")) return FieldOperator.Lte to raw.substring(2)
        if (raw.startsWith("!=")) return FieldOperator.NotEq to raw.substring(2)
        if (raw.startsWith(">")) return FieldOperator.Gt to raw.substring(1)
        if (raw.startsWith("<")) return FieldOperator.Lt to raw.substring(1)
        if (raw.startsWith("~")) return FieldOperator.Contains to raw.substring(1)
        if (raw.startsWith("^")) return FieldOperator.StartsWith to raw.substring(1)
        if (raw.startsWith("=")) return FieldOperator.Eq to raw.substring(1)
        return FieldOperator.Contains to raw
    }

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun expectEnd() {
            if (pos != tokens.size) error("Unexpected token at position $pos")
        }

        fun parseExpression(): SearchExpr {
            val children = mutableListOf<SearchExpr>()
            var mode = MatchAll.MatchAll

            children.add(parseTerm())
            while (pos < tokens.size) {
                when (val t = tokens[pos]) {
                    is Token.Bool -> { mode = if (t.isOr) MatchAll.MatchAny else MatchAll.MatchAll; pos++ }
                    Token.RParen -> break
                    else -> children.add(parseTerm())
                }
            }
            return if (children.size == 1 && mode == MatchAll.MatchAll) children.first()
            else SearchExpr.Group(mode, children)
        }

        private fun parseTerm(): SearchExpr {
            val expr = parseFactor()
            if (pos < tokens.size && tokens[pos] == Token.RParen) return expr
            return expr
        }

        private fun parseFactor(): SearchExpr {
            val t = tokens.getOrNull(pos) ?: error("Unexpected end of query")
            return when (t) {
                Token.LParen -> {
                    pos++
                    val inner = parseExpression()
                    if (tokens.getOrNull(pos) == Token.RParen) pos++
                    else error("Missing closing parenthesis")
                    inner
                }
                Token.Not -> {
                    pos++
                    SearchExpr.Not(parseFactor())
                }
                is Token.FieldTok -> { pos++; SearchExpr.Field(t.field, t.operator, t.value) }
                is Token.Word -> { pos++; SearchExpr.Text(t.value.lowercase()) }
                else -> error("Unexpected token $t")
            }
        }
    }

    private val FIELD_PATTERNS: List<Pair<String, SearchField>> = listOf(
        "id" to SearchField.Id,
        "onreading" to SearchField.OnReading,
        "kunreading" to SearchField.KunReading,
        "character" to SearchField.Character,
        "meaning" to SearchField.Meaning,
        "reading" to SearchField.Reading,
        "radical" to SearchField.Radical,
        "component" to SearchField.Component,
        "stroke" to SearchField.Stroke,
        "strokes" to SearchField.Stroke,
        "jlpt" to SearchField.Jlpt,
        "grade" to SearchField.Grade,
        "frequency" to SearchField.Frequency,
        "freq" to SearchField.Frequency,
        "tag" to SearchField.Tag,
        "flag" to SearchField.Flag,
        "status" to SearchField.Status,
        "favorite" to SearchField.Favorite,
        "note" to SearchField.Note,
        "deck" to SearchField.Deck,
        "accuracy" to SearchField.Accuracy,
        "interval" to SearchField.Interval,
        "lapses" to SearchField.Lapses,
        "reps" to SearchField.Reps,
        "ease" to SearchField.Ease,
        "due" to SearchField.Due,
        "kind" to SearchField.Kind,
        "type" to SearchField.Kind
    )
}

/** Convenience: run a query over a pool of cards with sorting. */
fun filterAndSort(
    cards: List<DesktopCard>,
    query: String,
    sort: (DesktopCard) -> Comparable<*>? = { it.character },
    ascending: Boolean = true
): List<DesktopCard> {
    val expr = SearchEngine.parse(query).getOrNull() ?: return cards
    val matched = cards.filter { SearchEngine.matches(it, expr) }
    val sorted = matched.sortedWith { a, b ->
        val aKey = sort(a); val bKey = sort(b)
        if (aKey == null && bKey == null) 0
        else if (aKey == null) 1
        else if (bKey == null) -1
        else @Suppress("UNCHECKED_CAST") (aKey as Comparable<Any>).compareTo(bKey as Comparable<Any>)
    }
    return if (ascending) sorted else sorted.reversed()
}
