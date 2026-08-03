package ua.syt0r.kanji.desktop.engine.dictionary

// ============================================
// KAITEYO DEINFLECTION ENGINE
// A compact conjugate rule table covering the
// common Japanese verb/adjective inflections and
// politeness forms. Used to restore dictionary
// headwords when looking up inflected text, e.g.
// 食べた -> 食べる, 行って -> 行く, 大きかった -> 大きい.
//
// BFS over the rule table: every rule that matches
// a suffix is applied to produce a shorter headword,
// and the process repeats. The result is an ordered
// list of plausible dictionary headwords.
// ============================================

data class DeinflectionResult(
    val word: String,          // the dictionary headword we recovered
    val ruleName: String,      // human label of the rule applied
    val reason: String         // short technical reason tag
)

private data class DeinflectionRule(
    val suffix: String,
    val replace: String,
    val ruleName: String,
    val reason: String
)

object Deinflect {

    private val rules: List<DeinflectionRule> = buildRules()

    private fun buildRules(): List<DeinflectionRule> {
        fun s(suffix: String, replace: String, name: String, reason: String) =
            DeinflectionRule(suffix, replace, name, reason)
        return listOf(
            s("なかった", "", "negative past", "negative past"),
            s("らなかった", "る", "negative", "plain negative"),
            s("る，なかった", "", "polite past negative", "polite past negative"),
            s("ませんでした", "", "polite past negative", "polite past negative"),
            s("ました", "", "polite past", "polite past"),
            s("ません", "", "polite negative", "polite negative"),
            s("ます", "", "polite", "polite"),
            s("ないと", "る", "negative", "obligation"),
            s("なければ", "る", "conditional", "negative conditional"),
            s("なけれ", "る", "conditional", "negative conditional"),
            s("なくて", "る", "te-form", "negative te-form"),
            s("なきゃ", "る", "negative", "obligation"),
            s("られ", "る", "passive", "passive"),
            s("れる", "る", "passive", "passive"),
            s("られる", "る", "passive", "passive"),
            s("られた", "る", "passive past", "passive past"),
            s("させる", "る", "causative", "causative"),
            s("せて", "る", "causative", "causative te"),
            s("せ", "る", "causative", "causative"),
            s("あった", "", "past", "past"),
            s("いた", "いる", "past", "past"),
            s("いて", "いる", "te-form", "te-form"),
            s("えば", "う", "conditional", "conditional"),
            s("けれ", "け", "conditional", "conditional"),
            s("ければ", "", "conditional", "conditional"),
            s("くない", "", "adjective", "adjective negative"),
            s("かった", "い", "past", "past"),
            s("くなる", "い", "adverbial-change", "becoming"),
            s("くて", "い", "adverbial", "adverbial"),
            s("かっただ", "", "past", "past"),
            s("いです", "い", "polite", "polite adjective"),
            s("して", "する", "te-form", "te-form"),
            s("します", "する", "polite", "polite"),
            s("した", "する", "past", "past"),
            s("してた", "する", "past", "past progressive"),
            s("できる", "", "potential", "potential"),
            s("さ", "", "nominalization", "nominalization"),
            s("られるの", "る", "noun", "noun nominalization"),
            s("んな", "", "colloquial", "colloquial"),
            s("ん", "", "negative", "negative colloquial"),
            s("なければならない", "る", "conditional", "must do"),
            s("なければいけない", "る", "conditional", "must do"),
            s("ない", "", "negative", "negative"),
            s("ぬ", "", "negative", "negative"),
            // te-form kana base reduction (verbs)
            s("って", "う", "te-form", "te-form"),
            s("って", "つ", "te-form", "te-form"),
            s("って", "る", "te-form", "te-form"),
            s("いて", "く", "te-form", "te-form"),
            s("いで", "ぐ", "te-form", "te-form"),
            s("んで", "む", "te-form", "te-form"),
            s("んで", "ぶ", "te-form", "te-form"),
            s("んで", "ぬ", "te-form", "te-form"),
            s("した", "", "past", "past"),
            s("た", "る", "past", "past"),
            s("だ", "", "past", "past (verb)"),
            s("ている", "る", "progressive", "progressive"),
            s("ていた", "る", "progressive past", "progressive past"),
            s("ておく", "る", "preparation", "preparation action"),
            s("てしまう", "る", "completion", "completion"),
            s("てもいい", "る", "permission", "permission"),
            s("させる", "る", "causative", "causative"),
            s("られる", "る", "potential", "potential"),
            s("れる", "る", "passive", "passive"),
            s("れば", "る", "conditional", "conditional"),
            s("えば", "う", "conditional", "conditional"),
            s("ながら", "", "while", "simultaneous action"),
            s("ら", "", "conditional", "conditional"),
            s("ば", "", "conditional", "conditional")
        ).distinctBy { it.suffix }
    }

    /**
     * Breadth-first deinflection. Returns candidate headwords,
     * shortest first, each tagged with the rule that produced it.
     */
    fun deinflect(surface: String): List<DeinflectionResult> {
        val start = surface.trim()
        if (start.isEmpty()) return emptyList()

        val seen = linkedSetOf<String>()
        val results = mutableListOf<DeinflectionResult>()
        val queue = ArrayDeque<String>()
        queue.addLast(start)
        seen.add(start)

        while (queue.isNotEmpty()) {
            val word = queue.removeFirst()
            for (rule in rules) {
                if (word.endsWith(rule.suffix)) {
                    val candidate = word.dropLast(rule.suffix.length) + rule.replace
                    if (candidate.isNotEmpty() && seen.add(candidate)) {
                        results.add(DeinflectionResult(candidate, rule.ruleName, rule.reason))
                        queue.addLast(candidate)
                    }
                }
            }
        }
        return results.sortedBy { it.word.length }
    }
}