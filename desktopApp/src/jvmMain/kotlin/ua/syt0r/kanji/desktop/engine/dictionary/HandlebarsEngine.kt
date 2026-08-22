package ua.syt0r.kanji.desktop.engine.dictionary

// ============================================
// KAITEYO HANDLEBARS TEMPLATE ENGINE
// Lightweight Mustache/Handlebars-inspired
// template renderer for dictionary card layouts.
// Supports variables, conditionals, loops, and
// partials — enough for Yomitan-style card
// templates without pulling in a Java template lib.
// ============================================

/** A compiled template ready for rendering. */
data class CompiledTemplate(
    val source: String,
    val tokens: List<TemplateToken>
)

/** A single token in a compiled template. */
sealed class TemplateToken {
    data class Literal(val text: String) : TemplateToken()
    data class Variable(val name: String, val escaped: Boolean = true) : TemplateToken()
    data class Section(val name: String, val inverted: Boolean, val body: List<TemplateToken>) : TemplateToken()
    data class Partial(val name: String) : TemplateToken()
}

/** A template context for variable lookup. */
class TemplateContext(
    private val variables: Map<String, Any?> = emptyMap(),
    private val parent: TemplateContext? = null
) {
    operator fun get(key: String): Any? {
        return variables[key] ?: parent?.get(key)
    }

    fun resolve(key: String): String {
        val value = get(key) ?: return ""
        return when (value) {
            is List<*> -> value.joinToString(", ") { it?.toString() ?: "" }
            is Boolean -> value.toString()
            else -> value.toString()
        }
    }

    fun isTruthy(key: String): Boolean {
        val value = get(key) ?: return false
        return when (value) {
            is Boolean -> value
            is String -> value.isNotBlank()
            is List<*> -> value.isNotEmpty()
            is Number -> value.toDouble() != 0.0
            else -> true
        }
    }

    fun getList(key: String): List<Map<String, Any?>> {
        val value = get(key) ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is List<*> -> value.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> item as? Map<String, Any?>
                    is String -> mapOf("this" to item)
                    else -> mapOf("this" to item?.toString())
                }
            }
            else -> emptyList()
        }
    }

    fun child(extraVars: Map<String, Any?>): TemplateContext {
        return TemplateContext(extraVars, this)
    }
}

object HandlebarsEngine {

    // ----------------------------------------------------------
    // Compilation
    // ----------------------------------------------------------

    fun compile(template: String): CompiledTemplate {
        val tokens = tokenize(template)
        return CompiledTemplate(template, tokens)
    }

    private fun tokenize(template: String): List<TemplateToken> {
        val tokens = mutableListOf<TemplateToken>()
        var i = 0
        val len = template.length

        while (i < len) {
            // Look for {{ or {{{
            val doubleOpen = template.indexOf("{{", i)
            if (doubleOpen == -1) {
                tokens.add(TemplateToken.Literal(template.substring(i)))
                break
            }

            // Emit literal before the tag
            if (doubleOpen > i) {
                tokens.add(TemplateToken.Literal(template.substring(i, doubleOpen)))
            }

            val tripleOpen = template.indexOf("{{{", i)
            val start = if (tripleOpen == doubleOpen) tripleOpen + 3 else doubleOpen + 2
            val closeTag = if (tripleOpen == doubleOpen) "}}}" else "}}"
            val end = template.indexOf(closeTag, start)

            if (end == -1) {
                // Unclosed tag — treat rest as literal
                tokens.add(TemplateToken.Literal(template.substring(doubleOpen)))
                break
            }

            val tagContent = template.substring(start, end).trim()
            val tripleClose = tripleOpen == doubleOpen

            when {
                tagContent.startsWith("!") -> {
                    // Comment — skip
                }
                tagContent.startsWith("^") -> {
                    // Inverted section
                    val name = tagContent.substring(1).trim()
                    val (sectionTokens, newI) = parseSection(template, end + closeTag.length, name, inverted = true)
                    tokens.add(TemplateToken.Section(name, inverted = true, body = sectionTokens))
                    i = newI
                    continue
                }
                tagContent.startsWith(">") -> {
                    // Partial
                    val name = tagContent.substring(1).trim()
                    tokens.add(TemplateToken.Partial(name))
                }
                tagContent.startsWith("#") -> {
                    // Section
                    val name = tagContent.substring(1).trim()
                    val (sectionTokens, newI) = parseSection(template, end + closeTag.length, name, inverted = false)
                    tokens.add(TemplateToken.Section(name, inverted = false, body = sectionTokens))
                    i = newI
                    continue
                }
                else -> {
                    // Variable
                    val name = tagContent
                    val escaped = tripleClose
                    tokens.add(TemplateToken.Variable(name, escaped))
                }
            }

            i = end + closeTag.length
        }

        return tokens
    }

    private fun parseSection(template: String, from: Int, sectionName: String, inverted: Boolean): Pair<List<TemplateToken>, Int> {
        val tokens = mutableListOf<TemplateToken>()
        var i = from
        val endTag = "/$sectionName"
        val len = template.length

        while (i < len) {
            val nextOpen = template.indexOf("{{", i)
            if (nextOpen == -1) {
                tokens.add(TemplateToken.Literal(template.substring(i)))
                break
            }
            if (nextOpen > i) {
                tokens.add(TemplateToken.Literal(template.substring(i, nextOpen)))
            }

            val nextClose = template.indexOf("}}", nextOpen + 2)
            if (nextClose == -1) {
                tokens.add(TemplateToken.Literal(template.substring(nextOpen)))
                break
            }

            val tag = template.substring(nextOpen + 2, nextClose).trim()
            if (tag == endTag) {
                return Pair(tokens, nextClose + 2)
            }

            // Nested tag inside section — recurse
            tokens.add(TemplateToken.Literal("{{$tag}}"))
            i = nextClose + 2
        }

        return Pair(tokens, len)
    }

    // ----------------------------------------------------------
    // Rendering
    // ----------------------------------------------------------

    fun render(compiled: CompiledTemplate, context: TemplateContext): String {
        return renderTokens(compiled.tokens, context)
    }

    fun render(template: String, context: TemplateContext): String {
        val compiled = compile(template)
        return render(compiled, context)
    }

    private fun renderTokens(tokens: List<TemplateToken>, context: TemplateContext): String {
        val sb = StringBuilder()
        for (token in tokens) {
            when (token) {
                is TemplateToken.Literal -> sb.append(token.text)
                is TemplateToken.Variable -> {
                    val value = context.resolve(token.name)
                    sb.append(if (token.escaped) htmlEscape(value) else value)
                }
                is TemplateToken.Section -> {
                    if (token.inverted) {
                        if (!context.isTruthy(token.name)) {
                            sb.append(renderTokens(token.body, context))
                        }
                    } else {
                        val list = context.getList(token.name)
                        if (list.isNotEmpty()) {
                            for (item in list) {
                                sb.append(renderTokens(token.body, context.child(item)))
                            }
                        } else if (context.isTruthy(token.name)) {
                            sb.append(renderTokens(token.body, context))
                        }
                    }
                }
                is TemplateToken.Partial -> {
                    // Partial rendering is handled at a higher level
                    sb.append("{{> ${token.name}}}")
                }
            }
        }
        return sb.toString()
    }

    // ----------------------------------------------------------
    // Built-in helpers
    // ----------------------------------------------------------

    private fun htmlEscape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    // ----------------------------------------------------------
    // Convenience: render a DictionaryEntry with a Yomitan-style template
    // ----------------------------------------------------------

    fun renderDictionaryEntry(entry: DictionaryEntry, template: String, dictionaryName: String = ""): String {
        val context = TemplateContext(
            mapOf(
                "expression" to entry.headword,
                "reading" to entry.readings.joinToString(", ") { it.reading },
                "glossary" to entry.senses.flatMap { it.glosses }.joinToString("; "),
                "partOfSpeech" to entry.senses.flatMap { it.partOfSpeech }.distinct().joinToString(", "),
                "tags" to entry.senses.flatMap { it.tags }.distinct().joinToString(", "),
                "frequency" to (entry.frequency.rank?.toString() ?: ""),
                "pitchAccent" to entry.readings.flatMap { r ->
                    r.pitchAccents.map { "pos:${it.position}" }
                }.joinToString(" "),
                "dictionary" to dictionaryName,
                "senses" to entry.senses.map { sense ->
                    mapOf(
                        "partOfSpeech" to sense.partOfSpeech.joinToString(", "),
                        "glossary" to sense.glosses.joinToString("; "),
                        "tags" to sense.tags.joinToString(", ")
                    )
                },
                "readings" to entry.readings.map { reading ->
                    mapOf(
                        "reading" to reading.reading,
                        "pitchAccent" to reading.pitchAccents.joinToString(" ") { "pos:${it.position}" },
                        "info" to reading.readingInformation.joinToString(", ")
                    )
                }
            )
        )
        return render(template, context)
    }

    /** Common Yomitan-style card templates. */
    object Templates {
        val FRONT = """
            <div class="card">
                <div class="expression">{{expression}}</div>
                <div class="reading">{{reading}}</div>
            </div>
        """.trimIndent()

        val BACK = """
            <div class="card">
                <div class="expression">{{expression}}</div>
                <div class="reading">{{reading}}</div>
                <hr>
                <div class="glossary">{{glossary}}</div>
                <div class="tags">{{partOfSpeech}} {{tags}}</div>
                {{#if frequency}}<div class="freq">#{{frequency}}</div>{{/if}}
            </div>
        """.trimIndent()

        val FULL = """
            <div class="card full">
                <div class="head">
                    <span class="expression">{{expression}}</span>
                    <span class="reading">{{reading}}</span>
                </div>
                <div class="body">
                    {{#senses}}
                    <div class="sense">
                        <span class="pos">{{partOfSpeech}}</span>
                        <span class="gloss">{{glossary}}</span>
                        {{#if tags}}<span class="tags">[{{tags}}]</span>{{/if}}
                    </div>
                    {{/senses}}
                </div>
                <div class="meta">
                    {{#if frequency}}<span class="freq">#{frequency}</span>{{/if}}
                    {{#if pitchAccent}}<span class="pitch">{{pitchAccent}}</span>{{/if}}
                    {{#if dictionary}}<span class="dict">{{dictionary}}</span>{{/if}}
                </div>
            </div>
        """.trimIndent()
    }
}
