---
title: How decks work
description: The data model behind collections, decks, cards, and the scheduler state that drives every screen.
---

Decks are the organizing unit of everything you study. This article is how the deck/card data model works.

## Deck entities

Decks are real entities, not labels: you can create, rename, merge, archive, duplicate, and delete them, nest them in collections, and pin favorites. Vocabulary decks are backed by the bundled dictionary, so adding a word resolves its reading, meaning, and kanji links automatically.

## Cards and SRS state

Each card carries its own scheduler record — new / learning / review / relearning state, interval, reps, lapses, and due time. This is the same state Study, the Library, and Statistics all read, which is why "due" counts never disagree between screens.

## Live counts

Deck rows show new / learning / due / mature counts derived live from the SRS state. The Library aggregates these across all content: total kanji, words, review counts.

## Imported and mined cards

- **Imported** (Anki `.apkg` or CSV/JSON) — scheduling is mapped onto Kaiteyo's SRS state (queue/type → state, interval, ease, reps, lapses, due). Deck hierarchy is preserved as nested collections.
- **Mined** — a mined card is an ordinary Kaiteyo card with sentence, reading, definition, screenshot, audio, timestamp, and source fields; it enters the SRS queue like any other.

## The full-app structure

The schema lives in `core/src/commonMain/sqldelight_user_data/` (mutable user database) alongside the read-only bundled app database (dictionary data). Backups and Anki exports read from the same model, so your data is portable and yours.
