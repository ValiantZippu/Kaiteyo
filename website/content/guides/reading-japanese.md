---
title: Read Japanese with instant lookups
description: The native reading workspace — open a local document, click any word for its dictionary card, and mine sentences without ever leaving the text.
---

The Reading workspace turns local documents into a study surface. Open a TXT, Markdown or HTML file, and every Japanese word becomes clickable: hover or click for the full dictionary card, mine the sentence into your deck, bookmark your place, and keep a reading history that survives restarts.

It is the same loop as [mining anime](/guides/mining-anime/), applied to text you read at your own pace.

## What you need

- Kaiteyo desktop
- A Japanese text file: `.txt`, `.md`, or `.html` (EPUB is on the roadmap)

That's it — the dictionary, tokenization and mining are all built in.

## The loop

**1. Open a document.** Import a file from disk (or paste text straight from the clipboard). The reader normalizes TXT, Markdown and HTML into clean paragraphs.

**2. Read with colour.** Each word is tokenized live: words you've already mined or suspended are tinted by their status, so your known territory is visible at a glance while unknown words stand out.

**3. Click any word.** The dictionary popup opens at your cursor — headword, reading, definitions, tags, and a mine button. Clicking further explores the entry's relations.

**4. Mine phrases too.** Select a whole phrase, not just one word, and mine it as a single card with the full sentence as context. One card, real context, tagged `phrase`.

**5. Keep your place.** Bookmarks and highlights persist per document; reading history records what you read and how far you got. Reopen the document and you're exactly where you left off.

## Search inside the document

The reader includes in-document search, so you can hunt for a specific word or grammar pattern across the text you're reading — and every match is a click away from a lookup.

## Deepening: the graph and the path

The dictionary card's **Graph** button hands the word to the Knowledge Graph: components, radical, words containing the kanji, kanji inside the word, media appearances, and a **Find path** tool that walks real relations — 食べる → 食 → 食事 — so one word pulls open the whole network around it.

Mined words also feed the Curriculum courses, so a reading session advances structured objectives the same way a review session does.

## The honest limits

- EPUB parsing is planned, not shipped — convert to HTML or text for now.
- Japanese word segmentation is dictionary-driven; invented or highly slangy text may segment imperfectly.
- The Reading workspace is desktop-only.

See the [reading documentation](/docs/features/reading/) for the full engine detail and the [desktop suite overview](/docs/user-guide/desktop-suite/) for where it lives in the app.
