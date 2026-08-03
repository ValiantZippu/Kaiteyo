---
title: Importing dictionaries
description: Bring your own Japanese dictionaries into Kaiteyo with Yomitan-format imports and the dictionary manager.
---

Beyond the bundled vocabulary, Kaiteyo lets you **import your own dictionaries** and use several of them side by side.

## Supported formats

- **Yomitan (.zip)** — the format used by the Yomitan/Yomichan browser extension. Contains `index.json`, `term_bank_*.json`, and `kanji_bank_*.json` files.
- **Unpacked dictionary folders** — the same files extracted to a directory.
- **JSON dictionaries** — raw `term_bank` / `kanji_bank` style data.

## Installing a dictionary

1. Open the **Dictionary** workspace.
2. Click **Install dictionary** and pick a `.zip` or folder.
3. Kaiteyo parses the entries, detects the format, and adds it to your installed list.

Every imported dictionary is stored offline under `~/.kaiteyo/dictionary` and can be enabled or disabled at any time without losing data.

## Searching across dictionaries

Lookups search **all enabled dictionaries** at once and group results by dictionary. Imported entries carry full reading, pitch-accent, and frequency information when the source provides it, and results can be mined straight into a card.
