package ua.syt0r.kanji.desktop.engine.grammar

// ============================================
// KAITEYO GRAMMAR — CURATED REFERENCE FACTS
// A small, clearly-labeled starter set of
// grammar patterns, treated like the kana and
// radical reference tables: "Reference facts;
// no third-party license applies." The pattern
// of record (full dataset adoption, e.g. an
// openly licensed grammar corpus) is gated by
// docs/data/SOURCES.md — the moment a dataset
// is adopted it replaces/supplements this set
// through the same GrammarIndex.
// ============================================

object CuratedGrammarFacts {

    private const val SOURCE = "kaiteyo-grammar-reference"

    private fun entry(
        pattern: String,
        meaning: String,
        jlpt: Int,
        forms: List<Pair<String, String>> = emptyList(),
        examples: List<Pair<String, String>> = emptyList()
    ) = GrammarEntry(
        id = "grammar:$pattern",
        pattern = pattern,
        meaning = meaning,
        jlpt = jlpt,
        forms = forms.map { (label, p) -> GrammarForm(label, p) },
        examples = examples.map { (ja, en) -> GrammarExample(ja, en) },
        source = SOURCE
    )

    val all: List<GrammarEntry> = listOf(
        entry(
            "〜は",
            "Topic marker — establishes what the sentence is about.",
            5,
            examples = listOf("これは本です。" to "This is a book.")
        ),
        entry(
            "〜が",
            "Subject marker; also 'but' at the start of a clause.",
            5,
            examples = listOf("猫が好きです。" to "I like cats.")
        ),
        entry(
            "〜を",
            "Object marker — marks the direct object of a verb.",
            5,
            examples = listOf("水を飲みます。" to "I drink water.")
        ),
        entry(
            "〜に",
            "Particle: location/time of action, indirect object, direction.",
            5,
            examples = listOf("七時に起きます。" to "I wake up at seven.")
        ),
        entry(
            "〜で",
            "Particle: place of action, means/instrument, language.",
            5,
            examples = listOf("電車で行きます。" to "I go by train.")
        ),
        entry(
            "〜てください",
            "Polite request: 'please do …'.",
            5,
            forms = listOf("〜て下さい" to "〜てください"),
            examples = listOf("ちょっと待ってください。" to "Please wait a moment.")
        ),
        entry(
            "〜たい",
            "Desire: 'want to do …'.",
            5,
            forms = listOf("〜たいです" to "〜たいです"),
            examples = listOf("日本に行きたいです。" to "I want to go to Japan.")
        ),
        entry(
            "〜ない",
            "Negative: 'not do …'.",
            5,
            examples = listOf("コーヒーを飲まない。" to "I don't drink coffee.")
        ),
        entry(
            "〜た",
            "Past tense (ta-form).",
            5,
            forms = listOf("〜ました" to "〜ました"),
            examples = listOf("昨日、映画を見た。" to "I watched a movie yesterday.")
        ),
        entry(
            "〜から",
            "Because … (reason); also 'from' as a particle.",
            5,
            examples = listOf("時間がないから急ぎます。" to "I'll hurry because I have no time.")
        ),
        entry(
            "〜ましょう",
            "Suggestive/volitional: 'let's …' / 'shall I …?'.",
            5,
            examples = listOf("一緒に行きましょう。" to "Let's go together.")
        ),
        entry(
            "〜と思う",
            "Opinion: 'I think …'.",
            4,
            examples = listOf("彼は来ると思います。" to "I think he will come.")
        ),
        entry(
            "〜なければならない",
            "Obligation: 'must do …'.",
            4,
            forms = listOf("〜なければいけない" to "〜なければいけない"),
            examples = listOf("宿題をしなければならない。" to "I must do my homework.")
        ),
        entry(
            "〜てもいい",
            "Permission: 'may do …'.",
            4,
            examples = listOf("ここで写真を撮ってもいいですか。" to "May I take a photo here?")
        ),
        entry(
            "〜ながら",
            "While doing …",
            4,
            examples = listOf("音楽を聞きながら勉強します。" to "I study while listening to music.")
        )
    )
}
