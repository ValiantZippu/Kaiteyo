---
title: How mining works
description: The mining pipeline — how a word encountered in media becomes a card in your deck.
---

Mining is the pipeline that turns Japanese you encounter (in media, the browser, OCR, or the clipboard) into study cards. This article is how it works.

## The payload

Every mined card starts as a `MiningPayload` — a source-agnostic bundle: headword, reading, definition, the sentence, a screenshot and/or audio path, tags, flags, notes, timestamp, source, and destination deck. Because the payload is source-agnostic, every source builds one and the pipeline is identical downstream.

## Sources

- **Subtitles** — select subtitle text in the Media Centre, mine the sentence (with screenshot + audio + timestamp)
- **Browser** — select text in the learning browser, mine from the dictionary popup
- **OCR** — recognized text from a screen capture, image, or clipboard becomes a lookup and then a card
- **Dictionary** — any dictionary lookup can become a card directly
- **Clipboard** — copied Japanese text routes through the same lookup
- **Text hook** — games and texthookers push lines into the dictionary workflow

## Duplicate protection

The mining engine keeps a record of mined content, so re-mining the same word or sentence doesn't create duplicates — the record makes the operation idempotent in practice.

## Where cards go

Mined cards land in your card pool and become normal Kaiteyo cards — they flow through SRS, statistics, and exams exactly like any card you added by hand. Media-mined cards also record a `MediaMiningEvent`, so reviewing the card later can jump you back to the exact timestamp in the source media.

## Integrations

Mined cards can be forwarded to **Anki** via AnkiConnect (opt-in) when you want a copy in an external workflow. Kaiteyo's own mining never depends on any external service.

See [the mining guide](/guides/mining-anime/) for the practical workflow.
