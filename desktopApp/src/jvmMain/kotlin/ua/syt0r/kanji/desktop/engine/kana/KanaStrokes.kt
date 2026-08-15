package ua.syt0r.kanji.desktop.engine.kana

import ua.syt0r.kanji.desktop.engine.stroke_evaluator.ReferenceStroke

// ============================================
// KANA STROKE DATA — built-in writing geometry
// Canonical stroke sequences (polylines in the
// same 0..100 grid as the kanji dataset) for the
// full base syllabary, 46 hiragana + 46 katakana,
// following standard Japanese stroke order.
//
// The stroke sequences are approximations in the
// same spirit as the built-in common-kanji dataset
// (see StrokeEvaluator). When a licensed KanjiVG
// dataset directory is present, the WritingEvaluator
// uses its authoritative kana geometry instead —
// this file is the honest built-in fallback.
//
// Small variants (ゃ ゅ …), dakuten (が …) and
// handakuten (ぱ …) resolve programmatically:
//   small kana   → the shape of their full-size base
//   voiced kana  → base strokes + the dakuten mark
//   handakuten   → base strokes + the handakuten mark
// so the whole syllabary is writable through the
// exact same engine architecture as kanji.
// ============================================

/** Canonical polyline strokes for every base kana (single-character units). */
private val BASE_KANA_STROKES: Map<String, List<ReferenceStroke>> = buildMap {
    // ------------------------------------------------------------
    // HIRAGANA — 46 characters, standard stroke order
    // ------------------------------------------------------------
    put("あ", listOf(
        ReferenceStroke.of(18.0 to 24.0, 82.0 to 24.0),
        ReferenceStroke.of(48.0 to 12.0, 48.0 to 52.0, 36.0 to 58.0, 50.0 to 74.0, 58.0 to 80.0),
        ReferenceStroke.of(58.0 to 30.0, 74.0 to 58.0, 70.0 to 74.0, 64.0 to 84.0)
    ))
    put("い", listOf(
        ReferenceStroke.of(28.0 to 14.0, 24.0 to 50.0, 30.0 to 84.0),
        ReferenceStroke.of(64.0 to 10.0, 72.0 to 48.0, 66.0 to 86.0)
    ))
    put("う", listOf(
        ReferenceStroke.of(30.0 to 18.0, 28.0 to 48.0, 40.0 to 66.0, 58.0 to 68.0),
        ReferenceStroke.of(58.0 to 68.0, 54.0 to 80.0, 60.0 to 84.0)
    ))
    put("え", listOf(
        ReferenceStroke.of(22.0 to 20.0, 74.0 to 20.0),
        ReferenceStroke.of(36.0 to 26.0, 26.0 to 46.0, 46.0 to 54.0, 36.0 to 74.0, 60.0 to 84.0)
    ))
    put("お", listOf(
        ReferenceStroke.of(24.0 to 20.0, 66.0 to 20.0),
        ReferenceStroke.of(44.0 to 14.0, 42.0 to 52.0),
        ReferenceStroke.of(60.0 to 18.0, 60.0 to 52.0, 46.0 to 58.0, 64.0 to 76.0, 58.0 to 84.0),
        ReferenceStroke.of(44.0 to 40.0, 36.0 to 50.0, 44.0 to 58.0)
    ))
    put("か", listOf(
        ReferenceStroke.of(24.0 to 22.0, 76.0 to 22.0),
        ReferenceStroke.of(44.0 to 16.0, 42.0 to 60.0, 30.0 to 84.0),
        ReferenceStroke.of(56.0 to 30.0, 66.0 to 46.0, 62.0 to 70.0, 72.0 to 84.0)
    ))
    put("き", listOf(
        ReferenceStroke.of(20.0 to 16.0, 78.0 to 16.0),
        ReferenceStroke.of(16.0 to 40.0, 74.0 to 40.0),
        ReferenceStroke.of(52.0 to 12.0, 50.0 to 84.0),
        ReferenceStroke.of(48.0 to 52.0, 66.0 to 60.0, 64.0 to 74.0, 70.0 to 84.0)
    ))
    put("く", listOf(ReferenceStroke.of(30.0 to 16.0, 60.0 to 44.0, 36.0 to 84.0)))
    put("け", listOf(
        ReferenceStroke.of(24.0 to 20.0, 72.0 to 20.0),
        ReferenceStroke.of(44.0 to 14.0, 40.0 to 84.0),
        ReferenceStroke.of(56.0 to 30.0, 68.0 to 50.0, 62.0 to 74.0, 72.0 to 84.0)
    ))
    put("こ", listOf(
        ReferenceStroke.of(26.0 to 20.0, 68.0 to 20.0),
        ReferenceStroke.of(26.0 to 70.0, 74.0 to 70.0)
    ))
    put("さ", listOf(
        ReferenceStroke.of(22.0 to 20.0, 76.0 to 20.0),
        ReferenceStroke.of(46.0 to 14.0, 42.0 to 84.0),
        ReferenceStroke.of(52.0 to 44.0, 64.0 to 54.0, 60.0 to 70.0, 68.0 to 84.0)
    ))
    put("し", listOf(ReferenceStroke.of(66.0 to 10.0, 70.0 to 44.0, 58.0 to 76.0, 40.0 to 86.0)))
    put("す", listOf(
        ReferenceStroke.of(24.0 to 20.0, 78.0 to 20.0),
        ReferenceStroke.of(50.0 to 14.0, 48.0 to 70.0, 34.0 to 78.0, 52.0 to 84.0)
    ))
    put("せ", listOf(
        ReferenceStroke.of(22.0 to 22.0, 78.0 to 22.0),
        ReferenceStroke.of(44.0 to 14.0, 40.0 to 84.0),
        ReferenceStroke.of(34.0 to 70.0, 54.0 to 74.0, 66.0 to 68.0)
    ))
    put("そ", listOf(
        ReferenceStroke.of(24.0 to 18.0, 74.0 to 18.0),
        ReferenceStroke.of(30.0 to 34.0, 50.0 to 40.0, 44.0 to 56.0),
        ReferenceStroke.of(44.0 to 56.0, 38.0 to 76.0, 54.0 to 84.0)
    ))
    put("た", listOf(
        ReferenceStroke.of(24.0 to 22.0, 76.0 to 22.0),
        ReferenceStroke.of(44.0 to 16.0, 42.0 to 84.0),
        ReferenceStroke.of(20.0 to 36.0, 14.0 to 48.0, 22.0 to 58.0),
        ReferenceStroke.of(56.0 to 40.0, 68.0 to 54.0, 64.0 to 70.0, 72.0 to 84.0)
    ))
    put("ち", listOf(
        ReferenceStroke.of(20.0 to 20.0, 70.0 to 20.0, 56.0 to 34.0),
        ReferenceStroke.of(38.0 to 30.0, 36.0 to 84.0),
        ReferenceStroke.of(30.0 to 66.0, 56.0 to 70.0, 68.0 to 62.0)
    ))
    put("つ", listOf(ReferenceStroke.of(70.0 to 18.0, 40.0 to 44.0, 20.0 to 70.0, 30.0 to 82.0)))
    put("て", listOf(ReferenceStroke.of(18.0 to 18.0, 74.0 to 20.0, 62.0 to 44.0, 38.0 to 68.0, 46.0 to 82.0)))
    put("と", listOf(
        ReferenceStroke.of(30.0 to 16.0, 26.0 to 60.0, 20.0 to 84.0),
        ReferenceStroke.of(26.0 to 36.0, 72.0 to 36.0, 66.0 to 50.0)
    ))
    put("な", listOf(
        ReferenceStroke.of(24.0 to 22.0, 74.0 to 22.0),
        ReferenceStroke.of(44.0 to 16.0, 42.0 to 84.0),
        ReferenceStroke.of(18.0 to 34.0, 12.0 to 48.0, 20.0 to 60.0),
        ReferenceStroke.of(58.0 to 38.0, 72.0 to 54.0, 66.0 to 74.0, 74.0 to 84.0)
    ))
    put("に", listOf(
        ReferenceStroke.of(24.0 to 22.0, 76.0 to 22.0),
        ReferenceStroke.of(44.0 to 16.0, 42.0 to 84.0),
        ReferenceStroke.of(56.0 to 40.0, 68.0 to 54.0, 62.0 to 72.0, 72.0 to 84.0)
    ))
    put("ぬ", listOf(
        ReferenceStroke.of(24.0 to 18.0, 62.0 to 22.0, 56.0 to 40.0),
        ReferenceStroke.of(56.0 to 40.0, 38.0 to 58.0, 50.0 to 72.0, 66.0 to 64.0, 70.0 to 80.0, 58.0 to 86.0)
    ))
    put("ね", listOf(
        ReferenceStroke.of(22.0 to 22.0, 74.0 to 22.0),
        ReferenceStroke.of(42.0 to 16.0, 38.0 to 84.0),
        ReferenceStroke.of(54.0 to 28.0, 70.0 to 40.0, 64.0 to 58.0),
        ReferenceStroke.of(64.0 to 58.0, 50.0 to 70.0, 62.0 to 80.0, 70.0 to 72.0)
    ))
    put("の", listOf(ReferenceStroke.of(40.0 to 10.0, 60.0 to 18.0, 70.0 to 38.0, 58.0 to 58.0, 34.0 to 72.0, 48.0 to 82.0, 64.0 to 76.0)))
    put("は", listOf(
        ReferenceStroke.of(24.0 to 22.0, 78.0 to 22.0),
        ReferenceStroke.of(44.0 to 16.0, 42.0 to 84.0),
        ReferenceStroke.of(58.0 to 34.0, 72.0 to 50.0, 66.0 to 68.0, 74.0 to 84.0)
    ))
    put("ひ", listOf(ReferenceStroke.of(22.0 to 18.0, 56.0 to 26.0, 66.0 to 44.0, 48.0 to 60.0, 30.0 to 74.0, 44.0 to 84.0)))
    put("ふ", listOf(
        ReferenceStroke.of(24.0 to 16.0, 40.0 to 26.0),
        ReferenceStroke.of(32.0 to 26.0, 24.0 to 52.0, 32.0 to 76.0),
        ReferenceStroke.of(52.0 to 24.0, 68.0 to 48.0, 58.0 to 72.0),
        ReferenceStroke.of(40.0 to 70.0, 56.0 to 74.0, 66.0 to 68.0)
    ))
    put("へ", listOf(ReferenceStroke.of(20.0 to 70.0, 42.0 to 42.0, 58.0 to 34.0, 74.0 to 44.0, 70.0 to 60.0)))
    put("ほ", listOf(
        ReferenceStroke.of(22.0 to 20.0, 76.0 to 20.0),
        ReferenceStroke.of(40.0 to 14.0, 38.0 to 84.0),
        ReferenceStroke.of(58.0 to 14.0, 56.0 to 84.0),
        ReferenceStroke.of(54.0 to 56.0, 70.0 to 62.0, 68.0 to 76.0, 74.0 to 84.0)
    ))
    put("ま", listOf(
        ReferenceStroke.of(24.0 to 22.0, 74.0 to 22.0),
        ReferenceStroke.of(24.0 to 44.0, 74.0 to 44.0),
        ReferenceStroke.of(50.0 to 14.0, 48.0 to 72.0, 36.0 to 78.0, 54.0 to 84.0)
    ))
    put("み", listOf(
        ReferenceStroke.of(24.0 to 18.0, 64.0 to 20.0, 52.0 to 34.0),
        ReferenceStroke.of(36.0 to 28.0, 32.0 to 60.0, 44.0 to 74.0),
        ReferenceStroke.of(44.0 to 74.0, 64.0 to 66.0, 70.0 to 78.0, 56.0 to 84.0)
    ))
    put("む", listOf(
        ReferenceStroke.of(24.0 to 22.0, 74.0 to 22.0),
        ReferenceStroke.of(44.0 to 16.0, 42.0 to 62.0, 30.0 to 74.0, 44.0 to 84.0),
        ReferenceStroke.of(58.0 to 40.0, 70.0 to 52.0, 64.0 to 68.0, 72.0 to 78.0)
    ))
    put("め", listOf(
        ReferenceStroke.of(26.0 to 16.0, 62.0 to 20.0, 56.0 to 38.0),
        ReferenceStroke.of(56.0 to 38.0, 34.0 to 54.0, 44.0 to 70.0, 62.0 to 64.0, 68.0 to 78.0, 54.0 to 84.0)
    ))
    put("も", listOf(
        ReferenceStroke.of(24.0 to 22.0, 76.0 to 22.0),
        ReferenceStroke.of(24.0 to 44.0, 76.0 to 44.0),
        ReferenceStroke.of(52.0 to 14.0, 50.0 to 60.0, 40.0 to 70.0, 56.0 to 84.0)
    ))
    put("や", listOf(
        ReferenceStroke.of(24.0 to 24.0, 76.0 to 24.0),
        ReferenceStroke.of(48.0 to 18.0, 46.0 to 84.0),
        ReferenceStroke.of(58.0 to 40.0, 72.0 to 52.0, 66.0 to 70.0, 74.0 to 84.0)
    ))
    put("ゆ", listOf(
        ReferenceStroke.of(30.0 to 14.0, 26.0 to 60.0, 34.0 to 82.0),
        ReferenceStroke.of(58.0 to 12.0, 68.0 to 44.0, 58.0 to 66.0, 38.0 to 74.0, 48.0 to 82.0, 64.0 to 80.0)
    ))
    put("よ", listOf(
        ReferenceStroke.of(24.0 to 18.0, 62.0 to 18.0, 50.0 to 34.0),
        ReferenceStroke.of(34.0 to 32.0, 32.0 to 60.0, 48.0 to 72.0, 66.0 to 66.0, 64.0 to 80.0)
    ))
    put("ら", listOf(
        ReferenceStroke.of(28.0 to 16.0, 66.0 to 16.0),
        ReferenceStroke.of(40.0 to 20.0, 36.0 to 56.0, 26.0 to 74.0, 42.0 to 84.0, 58.0 to 80.0)
    ))
    put("り", listOf(
        ReferenceStroke.of(30.0 to 16.0, 26.0 to 52.0, 20.0 to 78.0),
        ReferenceStroke.of(62.0 to 12.0, 58.0 to 46.0, 54.0 to 74.0, 60.0 to 82.0)
    ))
    put("る", listOf(
        ReferenceStroke.of(28.0 to 18.0, 64.0 to 18.0),
        ReferenceStroke.of(40.0 to 22.0, 36.0 to 60.0),
        ReferenceStroke.of(36.0 to 60.0, 26.0 to 78.0, 44.0 to 84.0, 60.0 to 78.0)
    ))
    put("れ", listOf(
        ReferenceStroke.of(22.0 to 22.0, 72.0 to 22.0),
        ReferenceStroke.of(40.0 to 16.0, 36.0 to 84.0),
        ReferenceStroke.of(52.0 to 30.0, 66.0 to 42.0, 62.0 to 58.0),
        ReferenceStroke.of(62.0 to 58.0, 52.0 to 70.0, 64.0 to 78.0, 72.0 to 70.0)
    ))
    put("ろ", listOf(ReferenceStroke.of(26.0 to 18.0, 64.0 to 18.0, 50.0 to 40.0, 38.0 to 62.0, 28.0 to 78.0, 46.0 to 84.0, 62.0 to 78.0)))
    put("わ", listOf(
        ReferenceStroke.of(26.0 to 18.0, 70.0 to 18.0, 56.0 to 34.0),
        ReferenceStroke.of(40.0 to 30.0, 38.0 to 64.0, 26.0 to 78.0, 46.0 to 84.0, 62.0 to 78.0)
    ))
    put("を", listOf(
        ReferenceStroke.of(22.0 to 24.0, 66.0 to 24.0),
        ReferenceStroke.of(42.0 to 18.0, 40.0 to 84.0),
        ReferenceStroke.of(56.0 to 34.0, 70.0 to 48.0, 66.0 to 66.0, 72.0 to 80.0)
    ))
    put("ん", listOf(
        ReferenceStroke.of(30.0 to 14.0, 52.0 to 20.0, 46.0 to 36.0),
        ReferenceStroke.of(46.0 to 36.0, 34.0 to 56.0, 40.0 to 74.0, 58.0 to 78.0, 64.0 to 68.0, 56.0 to 84.0)
    ))

    // ------------------------------------------------------------
    // KATAKANA — 46 characters, standard stroke order
    // ------------------------------------------------------------
    put("ア", listOf(
        ReferenceStroke.of(24.0 to 24.0, 76.0 to 24.0),
        ReferenceStroke.of(52.0 to 24.0, 42.0 to 84.0, 34.0 to 88.0)
    ))
    put("イ", listOf(
        ReferenceStroke.of(26.0 to 18.0, 66.0 to 18.0),
        ReferenceStroke.of(62.0 to 26.0, 44.0 to 84.0, 32.0 to 74.0)
    ))
    put("ウ", listOf(
        ReferenceStroke.of(24.0 to 20.0, 60.0 to 20.0),
        ReferenceStroke.of(60.0 to 20.0, 48.0 to 62.0, 38.0 to 84.0),
        ReferenceStroke.of(36.0 to 34.0, 28.0 to 60.0, 34.0 to 80.0)
    ))
    put("エ", listOf(
        ReferenceStroke.of(24.0 to 20.0, 76.0 to 20.0),
        ReferenceStroke.of(48.0 to 20.0, 46.0 to 80.0),
        ReferenceStroke.of(26.0 to 80.0, 74.0 to 80.0)
    ))
    put("オ", listOf(
        ReferenceStroke.of(24.0 to 24.0, 76.0 to 24.0),
        ReferenceStroke.of(48.0 to 18.0, 46.0 to 84.0),
        ReferenceStroke.of(56.0 to 30.0, 72.0 to 46.0)
    ))
    put("カ", listOf(
        ReferenceStroke.of(24.0 to 22.0, 76.0 to 22.0),
        ReferenceStroke.of(52.0 to 22.0, 40.0 to 84.0)
    ))
    put("キ", listOf(
        ReferenceStroke.of(20.0 to 22.0, 78.0 to 22.0),
        ReferenceStroke.of(52.0 to 12.0, 50.0 to 84.0),
        ReferenceStroke.of(24.0 to 54.0, 74.0 to 54.0)
    ))
    put("ク", listOf(
        ReferenceStroke.of(24.0 to 24.0, 74.0 to 24.0),
        ReferenceStroke.of(52.0 to 24.0, 38.0 to 84.0)
    ))
    put("ケ", listOf(
        ReferenceStroke.of(24.0 to 22.0, 76.0 to 22.0),
        ReferenceStroke.of(46.0 to 16.0, 42.0 to 84.0),
        ReferenceStroke.of(56.0 to 30.0, 70.0 to 46.0, 66.0 to 70.0)
    ))
    put("コ", listOf(
        ReferenceStroke.of(26.0 to 22.0, 74.0 to 22.0),
        ReferenceStroke.of(28.0 to 22.0, 26.0 to 70.0, 74.0 to 70.0)
    ))
    put("サ", listOf(
        ReferenceStroke.of(24.0 to 22.0, 76.0 to 22.0),
        ReferenceStroke.of(38.0 to 30.0, 32.0 to 80.0),
        ReferenceStroke.of(62.0 to 30.0, 68.0 to 80.0)
    ))
    put("シ", listOf(
        ReferenceStroke.of(30.0 to 22.0, 40.0 to 36.0),
        ReferenceStroke.of(48.0 to 32.0, 58.0 to 46.0),
        ReferenceStroke.of(58.0 to 48.0, 70.0 to 64.0, 76.0 to 78.0)
    ))
    put("ス", listOf(
        ReferenceStroke.of(26.0 to 22.0, 74.0 to 22.0),
        ReferenceStroke.of(56.0 to 26.0, 40.0 to 84.0, 64.0 to 76.0)
    ))
    put("セ", listOf(
        ReferenceStroke.of(22.0 to 24.0, 78.0 to 24.0),
        ReferenceStroke.of(44.0 to 16.0, 40.0 to 84.0),
        ReferenceStroke.of(34.0 to 66.0, 56.0 to 72.0, 68.0 to 64.0)
    ))
    put("ソ", listOf(
        ReferenceStroke.of(26.0 to 24.0, 38.0 to 36.0),
        ReferenceStroke.of(50.0 to 30.0, 62.0 to 46.0, 76.0 to 66.0, 70.0 to 82.0)
    ))
    put("タ", listOf(
        ReferenceStroke.of(24.0 to 24.0, 74.0 to 24.0),
        ReferenceStroke.of(46.0 to 24.0, 42.0 to 84.0),
        ReferenceStroke.of(56.0 to 30.0, 68.0 to 44.0)
    ))
    put("チ", listOf(
        ReferenceStroke.of(24.0 to 20.0, 78.0 to 20.0),
        ReferenceStroke.of(46.0 to 16.0, 44.0 to 84.0),
        ReferenceStroke.of(56.0 to 30.0, 74.0 to 44.0)
    ))
    put("ツ", listOf(
        ReferenceStroke.of(28.0 to 24.0, 40.0 to 36.0),
        ReferenceStroke.of(52.0 to 24.0, 64.0 to 36.0),
        ReferenceStroke.of(40.0 to 40.0, 54.0 to 58.0, 70.0 to 78.0, 64.0 to 84.0)
    ))
    put("テ", listOf(
        ReferenceStroke.of(24.0 to 24.0, 76.0 to 24.0),
        ReferenceStroke.of(46.0 to 18.0, 44.0 to 84.0),
        ReferenceStroke.of(56.0 to 32.0, 72.0 to 46.0)
    ))
    put("ト", listOf(
        ReferenceStroke.of(44.0 to 14.0, 42.0 to 84.0),
        ReferenceStroke.of(30.0 to 20.0, 58.0 to 30.0)
    ))
    put("ナ", listOf(
        ReferenceStroke.of(24.0 to 24.0, 78.0 to 24.0),
        ReferenceStroke.of(54.0 to 24.0, 40.0 to 84.0)
    ))
    put("ニ", listOf(
        ReferenceStroke.of(24.0 to 24.0, 76.0 to 24.0),
        ReferenceStroke.of(24.0 to 70.0, 76.0 to 70.0)
    ))
    put("ヌ", listOf(
        ReferenceStroke.of(24.0 to 22.0, 74.0 to 22.0),
        ReferenceStroke.of(56.0 to 26.0, 44.0 to 60.0, 28.0 to 80.0, 50.0 to 84.0, 68.0 to 74.0)
    ))
    put("ネ", listOf(
        ReferenceStroke.of(24.0 to 22.0, 74.0 to 22.0),
        ReferenceStroke.of(46.0 to 16.0, 42.0 to 84.0),
        ReferenceStroke.of(56.0 to 28.0, 72.0 to 42.0),
        ReferenceStroke.of(30.0 to 72.0, 48.0 to 78.0)
    ))
    put("ノ", listOf(ReferenceStroke.of(66.0 to 20.0, 40.0 to 84.0)))
    put("ハ", listOf(
        ReferenceStroke.of(34.0 to 22.0, 26.0 to 80.0),
        ReferenceStroke.of(58.0 to 22.0, 74.0 to 80.0)
    ))
    put("ヒ", listOf(
        ReferenceStroke.of(22.0 to 26.0, 76.0 to 26.0),
        ReferenceStroke.of(40.0 to 26.0, 36.0 to 84.0, 46.0 to 88.0)
    ))
    put("フ", listOf(
        ReferenceStroke.of(24.0 to 22.0, 76.0 to 22.0),
        ReferenceStroke.of(48.0 to 26.0, 38.0 to 66.0, 44.0 to 84.0)
    ))
    put("ヘ", listOf(ReferenceStroke.of(20.0 to 68.0, 44.0 to 40.0, 58.0 to 32.0, 76.0 to 46.0, 70.0 to 62.0)))
    put("ホ", listOf(
        ReferenceStroke.of(24.0 to 24.0, 76.0 to 24.0),
        ReferenceStroke.of(48.0 to 16.0, 46.0 to 84.0),
        ReferenceStroke.of(26.0 to 44.0, 40.0 to 52.0),
        ReferenceStroke.of(58.0 to 52.0, 72.0 to 44.0)
    ))
    put("マ", listOf(
        ReferenceStroke.of(26.0 to 22.0, 74.0 to 22.0),
        ReferenceStroke.of(48.0 to 22.0, 40.0 to 84.0),
        ReferenceStroke.of(56.0 to 30.0, 72.0 to 46.0)
    ))
    put("ミ", listOf(
        ReferenceStroke.of(26.0 to 22.0, 74.0 to 22.0),
        ReferenceStroke.of(24.0 to 46.0, 72.0 to 46.0),
        ReferenceStroke.of(22.0 to 70.0, 70.0 to 70.0)
    ))
    put("ム", listOf(
        ReferenceStroke.of(26.0 to 22.0, 60.0 to 22.0),
        ReferenceStroke.of(54.0 to 26.0, 70.0 to 60.0, 38.0 to 84.0, 30.0 to 74.0)
    ))
    put("メ", listOf(
        ReferenceStroke.of(62.0 to 20.0, 36.0 to 84.0),
        ReferenceStroke.of(26.0 to 30.0, 68.0 to 50.0)
    ))
    put("モ", listOf(
        ReferenceStroke.of(24.0 to 22.0, 76.0 to 22.0),
        ReferenceStroke.of(24.0 to 46.0, 76.0 to 46.0),
        ReferenceStroke.of(50.0 to 14.0, 48.0 to 84.0)
    ))
    put("ヤ", listOf(
        ReferenceStroke.of(24.0 to 24.0, 78.0 to 24.0),
        ReferenceStroke.of(48.0 to 18.0, 46.0 to 84.0),
        ReferenceStroke.of(56.0 to 30.0, 72.0 to 46.0)
    ))
    put("ユ", listOf(
        ReferenceStroke.of(28.0 to 24.0, 72.0 to 24.0),
        ReferenceStroke.of(32.0 to 24.0, 30.0 to 70.0, 72.0 to 70.0)
    ))
    put("ヨ", listOf(
        ReferenceStroke.of(26.0 to 22.0, 74.0 to 22.0),
        ReferenceStroke.of(26.0 to 46.0, 74.0 to 46.0),
        ReferenceStroke.of(30.0 to 22.0, 28.0 to 70.0, 74.0 to 70.0)
    ))
    put("ラ", listOf(
        ReferenceStroke.of(24.0 to 22.0, 76.0 to 22.0),
        ReferenceStroke.of(46.0 to 22.0, 42.0 to 84.0)
    ))
    put("リ", listOf(
        ReferenceStroke.of(32.0 to 16.0, 28.0 to 84.0),
        ReferenceStroke.of(62.0 to 16.0, 58.0 to 84.0)
    ))
    put("ル", listOf(
        ReferenceStroke.of(30.0 to 16.0, 70.0 to 16.0),
        ReferenceStroke.of(46.0 to 20.0, 40.0 to 60.0, 28.0 to 84.0, 52.0 to 82.0)
    ))
    put("レ", listOf(ReferenceStroke.of(32.0 to 16.0, 28.0 to 84.0, 52.0 to 80.0)))
    put("ロ", listOf(
        ReferenceStroke.of(24.0 to 22.0, 76.0 to 22.0),
        ReferenceStroke.of(28.0 to 22.0, 26.0 to 78.0, 74.0 to 78.0),
        ReferenceStroke.of(72.0 to 26.0, 72.0 to 74.0)
    ))
    put("ワ", listOf(
        ReferenceStroke.of(24.0 to 22.0, 78.0 to 22.0),
        ReferenceStroke.of(48.0 to 26.0, 40.0 to 70.0, 46.0 to 84.0)
    ))
    put("ヲ", listOf(
        ReferenceStroke.of(24.0 to 22.0, 70.0 to 22.0),
        ReferenceStroke.of(46.0 to 22.0, 42.0 to 84.0),
        ReferenceStroke.of(56.0 to 30.0, 72.0 to 44.0)
    ))
    put("ン", listOf(
        ReferenceStroke.of(30.0 to 22.0, 42.0 to 36.0),
        ReferenceStroke.of(46.0 to 40.0, 60.0 to 58.0, 74.0 to 74.0, 66.0 to 84.0)
    ))
}

/** The dakuten (voicing) mark — two short diagonal ticks, top-right. */
private val DAKUTEN_MARK: List<ReferenceStroke> = listOf(
    ReferenceStroke.of(58.0 to 16.0, 70.0 to 28.0),
    ReferenceStroke.of(64.0 to 18.0, 76.0 to 30.0)
)

/** The handakuten (semi-voicing) mark — a small circle, top-right. */
private val HANDAKUTEN_MARK: List<ReferenceStroke> = listOf(
    ReferenceStroke.of(60.0 to 20.0, 66.0 to 26.0, 72.0 to 24.0, 70.0 to 30.0, 62.0 to 28.0)
)

/**
 * Resolve the canonical strokes for any kana character. Handles the full
 * syllabary programmatically from the base set:
 *   - base kana        → its own strokes
 *   - small variants   → the shape of their full-size base
 *   - voiced kana      → base strokes + dakuten mark
 *   - semi-voiced kana → base strokes + handakuten mark
 * Returns null for anything without geometry (multi-unit clusters like きゃ
 * are graded per character and never invented here).
 */
fun kanaStrokesFor(expression: String): List<ReferenceStroke>? {
    if (expression.length != 1) return null

    // ヴ is ウ + dakuten — the base form of the v-series.
    if (expression == "ヴ") return (BASE_KANA_STROKES["ウ"] ?: return null) + DAKUTEN_MARK

    BASE_KANA_STROKES[expression]?.let { return it }

    // Small variants share the shape of their full-size base.
    kanaShapeAlias(expression)?.let { base ->
        BASE_KANA_STROKES[base]?.let { return it }
    }

    // Voiced / semi-voiced: look up the unvoiced base character.
    val record = kanaByCharacter(expression) ?: return null
    val baseChar = voicedBase(expression) ?: record.base.takeIf { it.isNotEmpty() }
    if (baseChar != null) {
        val baseStrokes = BASE_KANA_STROKES[baseChar] ?: kanaShapeAlias(baseChar)?.let { BASE_KANA_STROKES[it] }
            ?: return null
        val mark = when (record.category) {
            KanaCategory.Dakuten -> DAKUTEN_MARK
            KanaCategory.Handakuten -> HANDAKUTEN_MARK
            else -> null
        }
        return if (mark != null) baseStrokes + mark else null
    }
    return null
}

/** Whether the writing engine has canonical strokes for this kana. */
fun kanaWritingSupported(expression: String): Boolean = kanaStrokesFor(expression) != null

/** The reference stroke count used for display (e.g. "3 strokes"). */
fun kanaReferenceStrokeCount(expression: String): Int = kanaStrokesFor(expression)?.size ?: 0
