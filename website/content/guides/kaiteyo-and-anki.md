---
title: Kaiteyo and Anki
description: How to run Kaiteyo alongside an existing Anki workflow — apkg import/export, AnkiConnect, and which tool does what.
---

If you already have years of Anki history, the last thing you want is a tool that makes you start over — or one that pretends Anki doesn't exist. Kaiteyo's approach is honest: it's a complete learning environment with its own scheduler and data, and it interoperates with Anki in real, documented ways. This guide is how to actually combine them.

## First: what each tool is

- **Kaiteyo** — a connected Japanese-learning environment: kanji, vocabulary, writing, dictionary, media, mining, statistics and exams, all sharing one data model. Its scheduler is **FSRS-5**, its own.
- **Anki** — a general-purpose flashcard app. Powerful, universal, and (for Japanese) typically paired with external dictionaries and mining tools.

They're complementary. Kaiteyo isn't "Anki for Japanese" — it's the environment Anki users bolt together from separate tools, already connected.

## The two bridges

### 1. `.apkg` import/export (all platforms)

The standard Anki package format, implemented in shared code and available on **desktop, Android and iOS**:

- **Export** — send a Kaiteyo deck to `.apkg`, open it in Anki. Notes, cards, tags and scheduling carry across (deck hierarchy preserved: `Japanese::N5::Kanji` → nested Kaiteyo collections).
- **Import** — bring an `.apkg` into Kaiteyo. Media is extracted and references repaired; HTML is sanitised.

Honest limits: exact Anki template styling, typing-mode cards and cram scheduling are **not** reproduced — imported content renders as safe text with sanitised HTML. Scheduling is an approximation (Anki's queue/type map onto Kaiteyo's SRS state). That's documented, not hidden.

### 2. AnkiConnect (desktop)

A live bridge to a running local Anki installation:

- **Push mined cards** straight into Anki (Basic notes, tags, screenshot/audio media, duplicate detection) — mine in Kaiteyo's Media Centre, review in Anki if that's your home.
- **Import whole decks** from Anki over AnkiConnect with conflict policies (Skip / Update / Duplicate).

AnkiConnect talks to Anki's own localhost JSON-RPC API. No credentials, local-only, opt-in.

## Which workflow is yours?

| Situation | Recommended |
|---|---|
| Everything already in Anki, happy there | Import what you need into Kaiteyo, or use AnkiConnect for mined cards — keep reviewing in Anki |
| Want the connected environment, keep Anki as backup | Use Kaiteyo daily; export decks to `.apkg` periodically |
| Switching fully | Import your `.apkg` collections once — hierarchy and scheduling come along |
| Mining-heavy | Mine in Kaiteyo (subtitles, browser, text hook), forward to Anki via AnkiConnect if you want a copy |

## The rules of the road

- **No lock-in, either direction.** Your data is yours; the formats are documented and offline. Nothing requires a Kaiteyo service.
- **No fake claims.** What Kaiteyo reproduces from Anki is documented precisely in the [Anki integration docs](/docs/integrations/anki/) — styling and cram scheduling are the known gaps.
- **Don't double-schedule.** Pick a home for review and stick to it. Syncing the same cards through two schedulers produces chaos.

## Getting started

- [Anki integration documentation](/docs/integrations/anki/) — formats, data flow, compatibility limits
- [Local API & integrations](/wiki/local-api/) — the other ways in and out
- [Mining anime](/guides/mining-anime/) — the mining loop that pairs with Anki
