---
title: OCR and screen capture
description: Recognize Japanese text from images and your screen, then mine or look it up.
---

Kaiteyo's built-in **OCR** turns text you can see into text you can study — no browser extension required.

## Capture methods

- **Image files** — open a screenshot or photo and recognize the text in it.
- **Clipboard** — capture whatever is on your clipboard with one click.
- **Full screen** — recognize the entire current screen.
- **Region** — drag a selection box and recognize only the area you chose.

## Recognition

OCR defaults to Japanese (`jpn`) and is powered by **Tesseract** when the `tess4j` library is on the classpath, with graceful fallback when it is not available. Results come back as lines of recognized text.

## From text to cards

Recognized lines can be **mined into cards** or looked up in the dictionary, closing the loop from a game or video on screen to a review card in your deck.
