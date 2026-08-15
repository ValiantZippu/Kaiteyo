# Kaiteyo — Vision

> **Status**: live vision, superseding the short version in `docs/roadmap/PROJECT_VISION.md`
> (that file was updated to point here). This is the **product vision**, not the code
> status — implementation status for every claim below is tracked in
> `docs/planning/CURRENT_STATE.md` and `docs/features/FEATURES.md`.

## The one-sentence vision

**Kaiteyo is a connected Japanese language ecosystem — one application in which reading,
watching, looking up, mining, studying, testing, exploring, and playing are the same
activity, and every word the user touches leaves a trace that makes the next encounter
easier.**

## What Kaiteyo will be

The blueprint (MASTER §1) defines the destination as a single ecosystem containing:

| Pillar | What it means in practice | Status (2026) |
|---|---|---|
| **Dictionary** | A professional Japanese dictionary — kanji, vocabulary, readings, meanings, radicals, pitch, frequency, JLPT — with Yomitan-style popup glossing anywhere in the app | ✅ bundled + suite dictionary; popup in suite |
| **Kanji/kana knowledge system** | Every character an exploration hub: strokes, components, words, sentences, media, mastery | ✅ study/writing; hub traversal is target (NODE §82) |
| **Grammar & sentences** | Grammar nodes, conjugations, example sentences woven through every surface | 🚧 starter deck; dataset research open |
| **Learning platform** | Library/decks/cards, FSRS-5 SRS, reviews, exams, curriculum/courses | ✅ core; curriculum target |
| **Mining system** | Any Japanese text/media → card, with screenshot/audio/timestamp provenance | ✅ suite; graph edges target |
| **Media center** | Serious video/audio player with subtitles, mining, screenshots, bookmarks | ✅ suite |
| **Integrations** | Anki (.apkg all platforms + AnkiConnect), Yomitan dictionaries, local API, plugins later | ✅ core+suite; plugins deferred |
| **Statistics** | Event-driven, honest, drill-downable; smart active-time; heatmap | ✅ |
| **Knowledge graph** | One user-knowledge model across every surface | 🔬 target (ADR-0013/0016) |
| **Exams** | JLPT-aligned, generated, adaptive over time | ✅ generated; adaptive target |
| **The Journey (game)** | An actual 3D world where exploration, discovery, culture, photography, and daily life teach Japanese | 🔬 architected only — no code |
| **Website & web trial** | Product site + a limited browser experience | ✅ site; trial planned |
| **Cross-platform** | Windows/macOS/Linux desktop flagship; Android; iOS | ✅ |

## Why Kaiteyo exists

Most Japanese learning tools split study into disconnected silos — a flashcard app here,
a dictionary there, a video player somewhere else, a game nowhere near the dictionary.
Kaiteyo's thesis: **the connections are the product.** The user moves naturally:

```
Kanji → word → sentence → grammar → media → scene → Journey → location → object
→ discovery → deck → review → stats → exam → mastery
```

…without ever feeling they switched applications (NODE §155). The knowledge graph is the
connective tissue; the node system is the language; the event ledger is the evidence
(NODE §162).

## The experience we are building toward

### The learning loop (exists today, in the suite)

1. Read or watch something in Japanese.
2. Hover a word — the dictionary popup appears instantly.
3. Mine a sentence — a card lands in the SRS queue with screenshot, audio, timestamp.
4. Review with FSRS — jump straight back to the exact scene in the media.

### The Journey loop (target — NODE §87)

1. Walk into a convenience store.
2. See おにぎり. Interact. See お・に・ぎ・り.
3. Learn the word, photograph it, add a discovery, optionally make a card.
4. Later the word appears in media — knowledge increases, statistics update, a quest
   progresses.
5. The player feels: **"I am living inside a Japanese learning world"** — never "I am
   grinding XP in an educational RPG" (NODE §86).

## Design philosophy (summary)

Full principles: [`PRINCIPLES.md`](PRINCIPLES.md). The vision-level commitments:

- **Craft over features** — every pixel, animation, and interaction intentional; the UI
  is a power tool that is also beautiful (NODE §120–§125).
- **Desktop first, everywhere capable** — desktop is the flagship; Android/iOS share the
  core engine; controllers/touch/keyboard all first-class (MASTER §27).
- **Respect the learner** — no predatory mechanics (NODE §117), no fake data
  (STANDARDS §290), honest statistics (MASTER §46), adaptive difficulty (MASTER §33).
- **Offline by default** — learning never depends on connectivity (MASTER §78).
- **Open data, open source** — GPL-3.0, openly licensed datasets with verified
  provenance (MASTER §8), local-first architecture (ADR-0009).
- **The game is a real game** — exploration/discovery-first, Nintendo-quality polish,
  never a gamification skin (MASTER §21–§22).

## What Kaiteyo is NOT (revised against the blueprint)

These were the original non-goals; they are kept, clarified against the master blueprint:

- **NOT a gamified flashcard app.** Points/badges/streaks are not the core loop. The
  Journey is an *actual game* with learning as its purpose — this is not a contradiction:
  the game earns its mechanics through exploration/discovery/collection, never through
  XP grinding (NODE §86, §116).
- **NOT a mobile-first app.** Mobile is supported and shares the engine; desktop is
  primary.
- **NOT just a dictionary.** The dictionary is a *subsystem* of the ecosystem (MASTER
  §11), not the whole product.
- **NOT a social network.** No friend walls, no sharing feeds; community features are
  strictly opt-in and secondary (MASTER §65).
- **NOT a replacement for a textbook.** It is a companion that connects media, reference,
  and practice — and its curriculum system (MASTER §29) grows into structured courses.
- **NOT a collection of disconnected applications.** The whole point is that dictionary,
  media, study, and game share one knowledge model (MASTER §1).

## Long-term vision (time-boxed, from the roadmap)

- **v2.x (now)**: platform polish, immersion suite, Anki interop, persistent data.
- **v3.x**: node layer + knowledge graph foundations (ADR-0013/0016), grammar/pitch
  data, curriculum system, node-based Browse/Library/Stats.
- **v4.x**: Journey engine decision (ADR-0018) → runtime prototype → Kamakura +
  Enoshima vertical slice (NODE §91) — the proof gate before any world expansion.
- **v5.x**: world expansion via content packages (ADR-0015), children's world
  (MASTER §30), cloud/sync maturity, web trial.

## Related

- Blueprint: [`PRODUCT.md`](PRODUCT.md) (MASTER §1, §2)
- Principles: [`PRINCIPLES.md`](PRINCIPLES.md)
- Current reality: [`../planning/CURRENT_STATE.md`](../planning/CURRENT_STATE.md)
- Roadmap: [`../roadmap/ROADMAP.md`](../roadmap/ROADMAP.md)
- Game vision: [`../game/game-overview.md`](../game/game-overview.md) + [`../vision/game-philosophy.md`](../vision/game-philosophy.md)
- Historical vision (pre-blueprint): [`../roadmap/PROJECT_VISION.md`](../roadmap/PROJECT_VISION.md)
