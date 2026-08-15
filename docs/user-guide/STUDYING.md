# Studying with Kaiteyo

This guide covers the core study engine, available on **all platforms** (desktop,
Android, iOS). It describes the screens you actually see — screen names below match
the app's destinations and composables.

## Kanji & kana study

- **Decks** — built-in JLPT (N5–N1) and school-grade kanji decks, plus kana
  (hiragana/katakana) decks. Create your own decks and add characters from the
  character info screens.
- **Deck details** (`DeckDetailsScreen`) — open a deck to see its items, progress,
  filtering, sorting, and configuration (what you practice, in what order, with
  which answer methods). Tabs/groups let you browse items, edit, run bulk actions,
  duplicate, export and delete.
- **Character info screen** (`InfoScreen`) — tap a character anywhere to see its
  readings (on/kun), meanings, radicals/components, stroke order, JLPT/grade info,
  and related vocabulary.

### Study modes (letter decks)

- **Reading** (`ScreenLetterPracticeType.Reading`) — see the character and recall
  its reading/meaning; answer with multiple choice or by self-grading.
- **Writing** (`ScreenLetterPracticeType.Writing`) — the character is displayed with
  its stroke order; draw it on the canvas. The stroke evaluator grades your attempt
  (relaxed / normal / exam strictness).
- Answer methods and review order are configurable per deck
  (`DeckDetailsScreenConfiguration`).

## Vocabulary study & flashcards

- **Vocab decks** — study words with readings, meanings, furigana, and example
  sentences.
- **Flashcards** (`ScreenVocabPracticeType.Flashcard`) — front/back cards with
  reveal.
- **Reading picker** (`ReadingPicker`) — choose the correct reading from shuffled
  options (shuffleable).
- **Writing** (`Writing`) — type or write the word.
- Example sentences show furigana; TTS pronunciation is available on supported
  platforms.

## Writing practice & stroke evaluation

- The drawing canvas includes brush settings (smoothing, prediction, pressure),
  stroke guides, and stroke-order animation.
- Your strokes are evaluated against the canonical stroke data (KanjiVG-derived):
  count, order, and per-stroke shape accuracy with an overall score.
- Strictness levels: Relaxed / Normal / Exam (Exam also rejects correct strokes
  drawn in the wrong order).
- On desktop, the suite's `WritingPracticeView` provides its own launch + session
  panels using the same card pool.

## Spaced repetition (SRS)

- Kaiteyo schedules reviews with **FSRS-5** (see `core/srs/fsrs/`) with configurable
  intervals and a daily review limit.
- Cards have states (new / learning / review / relearning); you grade reviews
  (**Again, Hard, Good, Easy**).
- Review sessions support **bury**, **suspend**, **retry**, **forget** (reschedule),
  and **undo**.
- **Daily limits** — cap how many new cards/reviews you see per day
  (`DailyLimitScreen`, Settings → Study).

## Deck management

- Create, edit, rename, archive/restore, duplicate, and delete decks
  (`DeckEditScreen`, `DeckManager`).
- **Archive** — archive a deck to hide it from the main lists; restore anytime.
- **Bulk actions** (`BulkActionsScreen`) — select multiple cards to tag, flag,
  favorite, suspend, reset, or delete.
- **Import/export** — bring data in from Anki (`.apkg`), JSON/CSV/TSV/TXT, or
  export your decks to the same formats (`ImportExportScreen`,
  `../integrations/ANKI.md`).

## Tags, flags, suspend & bury

- **Tags** (`TagManagerScreen`) — label cards and decks; manage them from the tag
  manager.
- **Flags** (`FlagManagerScreen`) — mark cards (e.g. for later review); flag manager
  with stats and bulk ops.
- **Suspend** — remove a card from review queues until you unsuspend it.
- **Bury** — postpone a card to the next day (per-session).

## Searching

- **Word & sentence search** (`SearchScreen`) — search vocabulary by reading or
  meaning; search sentences and words in the dictionary data.
- **Radical search** — build a character from its radicals (search by radicals).
- **Text analysis** (`TextAnalysisScreen`) — paste or open a text and get a
  word-by-word breakdown (Ichiran-style): each word with reading, meaning, part of
  speech, and card creation.
- The desktop suite adds dictionary-style search across installed dictionaries
  (see [DESKTOP_SUITE.md](DESKTOP_SUITE.md)).

## Statistics

The Statistics screen (`StatisticsScreen`, shared across platforms; desktop Stats
view) shows your learning over time:

- **Heatmap** of daily activity
- **Knowledge** — JLPT/frequency coverage, learning profile
- **Retention** — SRS grades, intervals, per-deck retention
- **Sessions / mistakes / weak entities**
- **Goals** — with goal history
- **Exams** — take generated exams (e.g. weekly exam) and see graded results
- Achievements for consistency, vocabulary, kanji, and exploration

Everything is computed from real database data — no fake charts (see
`../features/STATISTICS.md` for metric definitions).

## Tips

- Review daily — consistency beats intensity (the heatmap rewards streaks).
- Use the daily limit to avoid burnout; adjust it in settings.
- On desktop, combine study with immersion: mine cards from subtitles, then review
  them (see [DESKTOP_SUITE.md](DESKTOP_SUITE.md)).
