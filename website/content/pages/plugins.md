---
title: Plugins
description: Kaiteyo's plugin plans and current extension points — an honest status of what exists today and what is planned.
---

## Status

The plugin system is **on the roadmap, with groundwork in the repository** — a plugin registry and marketplace scaffold exist (`plugin/` in the source tree), but there is **no runtime plugin loading yet**. Nothing is advertised as working that isn't.

## The plan

When the plugin system ships, plugins will extend the app through well-defined extension points:

<div class="feature-grid">
  <div class="feature-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-grid"/></svg></div>
    <h3>Custom card types</h3>
    <p>Beyond recognition and writing cards — plugin-defined templates with their own grading logic.</p>
  </div>
  <div class="feature-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-book"/></svg></div>
    <h3>Dictionary sources</h3>
    <p>Bring your own dictionaries and example sentence sources into the lookup.</p>
  </div>
  <div class="feature-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-puzzle"/></svg></div>
    <h3>OCR & media backends</h3>
    <p>Pluggable recognition and playback engines behind the existing extension points.</p>
  </div>
  <div class="feature-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-terminal"/></svg></div>
    <h3>Integrations API</h3>
    <p>A public integration API for third-party tools — on the roadmap alongside the plugin runtime.</p>
  </div>
</div>

## What you can do today

Until the plugin runtime lands, the practical extension points are:

- **Yomitan dictionaries** — import your own dictionaries (ZIP/JSON/JMdict/KANJIDIC/KanjiVG) and search them side by side with the built-in one.
- **Anki** — `.apkg` import/export on every platform, plus AnkiConnect for live two-way deck/mining workflows on desktop.
- **Local API & text hook** — drive or read Kaiteyo from external tools over localhost.
- **Deck export/import** — share and reuse decks between devices.
- **Custom fonts & themes** — bring your own fonts; the theme system covers the visual side.

See the [integrations page](/integrations/) for the full, honest list of what works today.

## Getting involved

The extension points are designed with contributors in mind. If you want to build a plugin, start a Discussion on GitHub so the APIs get shaped by a real use case — see the [contributing guide](/contributing/) and the [plugin documentation](/docs/integrations/plugins/).
