---
title: How to mine anime
description: The immersion loop — watch Japanese media, look up words, and turn them into cards without breaking the episode.
---

Mining is the skill of turning the Japanese you encounter in the wild into cards you'll actually review. Anime with subtitles is the classic source, and Kaiteyo's Media Centre makes the loop: **watch → understand → look up → mine → review**.

## What you need

- Kaiteyo desktop with the Media Centre
- A video file with Japanese subtitles (SRT, ASS, SSA or VTT — all supported)
- A player backend: [VLC or mpv](/docs/integrations/media_backends/), or the built-in audio engine for audio-only

## The loop

**1. Watch with Japanese subtitles.** Not English. Japanese subtitles are your reading practice; the audio is your listening practice; together they're comprehensible input.

**2. When a word stops you — look it up.** Select the subtitle text (click a word, drag across a phrase, or select the whole line). The dictionary popup opens instantly: reading, meaning, example, pitch, tags. You never leave the video.

**3. Mine the sentence.** The same popup creates the card. Kaiteyo captures everything automatically: the sentence, the target word, its reading and definition, a screenshot of that exact moment, the timestamp, and the source. One click.

**4. It lands in your deck.** Mined cards are ordinary Kaiteyo cards — they enter the SRS queue, appear in statistics, and can feed exams. Not a separate toy system.

**5. Review later.** The card includes the screenshot, so next time you review it you're back in that scene. When you get it right, the word is yours.

## The mining etiquette

Quality beats quantity, every time:

- **Mine sentences you almost understand** — one unknown word per sentence. That's the sweet spot.
- **Skip sentences with three unknowns.** You won't retain the sentence, you'll just get a card you dread.
- **Mine with the screenshot.** The visual context is half the memory.
- **Prefer i+1**: the sentence plus one step of difficulty.

A good session is 3–8 high-quality cards, not 30 desperate ones.

## Batch mining

When you finish an episode, the transcript panel lets you multi-select lines and bulk-mine the ones that earned it. Same quality bar — this is just faster.

## Beyond anime

The same loop works for:

- **Drama and variety shows** with subtitles
- **YouTube** via the learning browser
- **Video games and visual novels** via the text hook — a texthooker sends the current line to Kaiteyo, and you look up + mine without alt-tabbing
- **Your own recordings** — audio mining with the built-in player
- **Any image or PDF** with Japanese text via OCR and the dictionary

## The honest limits

The desktop Media Centre is a desktop feature. OCR needs a local Tesseract install. None of this is hidden: the [media documentation](/docs/features/media/) and [media workspace wiki](/wiki/media-workspace/) describe exactly what works and what needs what.

See [how mining works](/wiki/mining/) for the mechanics behind the cards.
