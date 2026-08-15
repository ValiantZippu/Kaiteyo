# Product Vision

**Status**: LIVE — the product definition. Read before any planning.

## What Kaiteyo is

Kaiteyo (書いてよ — "write it!") is a premium, cross-platform Japanese language
learning **platform** — not a single feature, and not a pile of features glued
together. It is a unified system in which the dictionary, kanji exploration,
vocabulary, grammar, sentences, reading, mining, media learning, statistics, exams,
spaced repetition, and (target) a walkable Japanese world all speak to each other
through one knowledge model.

Today Kaiteyo ships as a Kotlin Multiplatform + Compose Multiplatform application
(desktop primary; Android and iOS) with:

- a bundled dictionary (`AppDataDatabase` via the KJD data platform),
- FSRS-5 spaced repetition with a real study engine,
- decks/cards/reviews, writing practice with stroke evaluation,
- statistics (event-driven), exams (incl. JLPT simulation), achievements,
- a desktop immersion suite: media centre (VLC/mpv backends), subtitle engine,
  dictionary popup, mining engine, OCR, import/export, Anki interop.

The **target** product (ADR-0013/0014/0015, fully specified, not implemented) adds:

- a node-based knowledge graph connecting language, media, and world content,
- an event-derived user-knowledge model (ADR-0016),
- **Journey**: a stylized, walkable representation of Japan where learning happens
  inside the world.

## Who it is for

- **Self-learners** studying Japanese independently (primary).
- **Students** in formal courses — Kaiteyo supplements, never replaces, a textbook.
- **JLPT candidates** N5→N1 (JLPT alignment is one axis, never the whole system).
- **Writers** needing kanji lookup, stroke practice, and pitch/frequency data.
- **Media learners** who want to mine anime/drama rather than type flashcards.
- **Children** (target): a distinct instructional structure on the same core.
- **Polyglots** running multiple learning systems — Anki interop matters to them.

## Product pillars (non-negotiable)

1. **One system, one knowledge model.** Dictionary, learning, media, world, stats,
   decks, exams, mining must all communicate. No silos. (Connected ≠ tightly coupled —
   clean boundaries, shared events/data, see `docs/production/` and the architecture
   docs.)
2. **Craft over features.** Every pixel, animation, interaction intentional. Dense
   without clutter; beautiful without decoration-for-its-own-sake (§53).
3. **Respect the learner.** No gamification gimmicks as the core loop. Users are
   capable adults; the child experience is a different *instructional structure*,
   not a dumbed-down clone.
4. **Offline by default.** Core functionality — dictionary, kanji, vocabulary,
   decks, reviews, stats, media, exams, installed Journey content — works without
   internet. Network features identify their dependency.
5. **Open source, open data.** Community can audit, contribute, fork. Datasets are
   open and provenance-tracked (`docs/data/`).
6. **Evidence over claims.** Statistics and knowledge estimates derive from real
   events. Never fabricate data, statistics, or integrations (§370 contract).

## What Kaiteyo is NOT

- **NOT** a gamified app (no XP, loot, energy, lives, loot boxes, artificial timers
  — even in Journey; see `game-philosophy.md`).
- **NOT** a mobile-first app (desktop primary; mobile is supported).
- **NOT** a standalone dictionary app (lookups are contextual, in-context).
- **NOT** a social network (no friend lists, no sharing walls; optional community
  features remain *optional*).
- **NOT** a replacement for a textbook (a supplement and a platform).
- **NOT** an Anki clone (Anki is an integration; Kaiteyo owns its own decks, cards,
  reviews, scheduling).
- **NOT** a combat RPG (Journey is exploration; see game philosophy).

## Definition of product success

A learner should be able to start from *any* entry point — a word in the dictionary,
an anime episode, a sign in the world — and the entire platform bends toward their
knowledge: what they can read, what to review, what media fits, what to explore next,
what they have truly mastered. The product feels like **one coherent place to live in
Japanese**, never like a collection of tools.

## Related

- Original one-page vision: `docs/roadmap/PROJECT_VISION.md`
- Long-term arc: [long-term-vision.md](long-term-vision.md)
- Learning doctrine: [learning-philosophy.md](learning-philosophy.md)
- Game doctrine: [game-philosophy.md](game-philosophy.md)
- Current implementation reality: `docs/planning/PRODUCT_AUDIT.md`
