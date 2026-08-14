package ua.syt0r.kanji.core.statistics

/**
 * Scores exam answers.
 *
 * Multiple choice: exact match against the correct option.
 * Production (free-text): lenient normalization — trim, case-fold,
 * strip spaces/punctuation, and accept known reading variants so
 * legitimate alternatives are not marked wrong just because the
 * string differs. The correct answer may contain "|" separated
 * accepted variants (e.g. "すい|スイ|みず").
 */
object ExamScorer {

    private val KATAKANA_TO_HIRAGANA = charArrayOf(
        'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ', 'ッ', 'ー', 'ヵ', 'ヶ', 'ヴ',
        'ア', 'イ', 'ウ', 'エ', 'オ', 'カ', 'キ', 'ク', 'ケ', 'コ', 'サ', 'シ',
        'ス', 'セ', 'ソ', 'タ', 'チ', 'ツ', 'テ', 'ト', 'ナ', 'ニ', 'ヌ', 'ネ',
        'ノ', 'ハ', 'ヒ', 'フ', 'ヘ', 'ホ', 'マ', 'ミ', 'ム', 'メ', 'モ', 'ヤ',
        'ユ', 'ヨ', 'ラ', 'リ', 'ル', 'レ', 'ロ', 'ワ', 'ヲ', 'ン', 'ガ', 'ギ',
        'グ', 'ゲ', 'ゴ', 'ザ', 'ジ', 'ズ', 'ゼ', 'ゾ', 'ダ', 'ヂ', 'ヅ', 'デ',
        'ド', 'バ', 'ビ', 'ブ', 'ベ', 'ボ', 'パ', 'ピ', 'プ', 'ペ', 'ポ'
    )

    private val HIRAGANA = charArrayOf(
        'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'ゃ', 'ゅ', 'ょ', 'っ', 'ー', 'か', 'け', 'ゔ',
        'あ', 'い', 'う', 'え', 'お', 'か', 'き', 'く', 'け', 'こ', 'さ', 'し',
        'す', 'せ', 'そ', 'た', 'ち', 'つ', 'て', 'と', 'な', 'に', 'ぬ', 'ね',
        'の', 'は', 'ひ', 'ふ', 'へ', 'ほ', 'ま', 'み', 'む', 'め', 'も', 'や',
        'ゆ', 'よ', 'ら', 'り', 'る', 'れ', 'ろ', 'わ', 'を', 'ん', 'が', 'ぎ',
        'ぐ', 'げ', 'ご', 'ざ', 'じ', 'ず', 'ぜ', 'ぞ', 'だ', 'ぢ', 'づ', 'で',
        'ど', 'ば', 'び', 'ぶ', 'べ', 'ぼ', 'ぱ', 'ぴ', 'ぷ', 'ぺ', 'ぽ'
    )

    fun score(
        question: ExamQuestionRecord,
        userAnswer: String
    ): ExamAnswerResult {
        val normalized = normalize(userAnswer)
        val isMultipleChoice = question.optionsJson != null
        val correct = if (isMultipleChoice) {
            normalized == normalize(question.answer)
        } else {
            val accepted = question.answer.split("|").map { normalize(it) }
            normalized in accepted
        }
        val category = when {
            correct -> "none"
            question.questionType.contains("Reading", ignoreCase = true) -> "wrong_reading"
            question.questionType.contains("Meaning", ignoreCase = true) ||
                question.questionType.contains("Vocab", ignoreCase = true) -> "wrong_meaning"
            question.questionType.contains("Kanji", ignoreCase = true) -> "wrong_kanji"
            question.questionType.contains("Radical", ignoreCase = true) -> "wrong_radical"
            question.questionType.contains("Stroke", ignoreCase = true) -> "wrong_stroke_count"
            else -> "unknown"
        }
        return ExamAnswerResult(
            isCorrect = correct,
            normalizedUserAnswer = userAnswer.trim(),
            mistakeCategory = category
        )
    }

    /** Normalizes free text: trim, lowercase latin, strip separators, kana-fold. */
    fun normalize(input: String): String {
        var result = input.trim()
            .lowercase()
            .replace(Regex("[\\s、,，。．.・/／\\\\-—–]+"), "")
        val sb = StringBuilder(result.length)
        result.forEach { ch ->
            val index = KATAKANA_TO_HIRAGANA.indexOf(ch)
            sb.append(if (index >= 0) HIRAGANA[index] else ch)
        }
        return sb.toString()
    }
}
