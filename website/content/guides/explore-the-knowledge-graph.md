---
title: Explore the knowledge graph
description: How kanji, words, components and radicals connect — traversing the graph, finding paths, and turning exploration into study.
---

Japanese isn't a list of 2,000 kanji; it's a network. 食 shows up in 食べる, 食事, 食堂 and 食料品店. The Knowledge Graph makes that network visible and walkable: every entry is a node with real relations, and your own study history is layered on top.

## What you need

- Kaiteyo desktop with at least one dictionary installed (the bundled kanji dictionary works out of the box)

## Starting from a word

Search any kanji or vocabulary in the graph and you get the node page: readings, meanings, JLPT band, frequency rank, and your knowledge state (new card, learning, known, mature, mined, suspended).

From there you can walk the edges:

- **Kanji node** → components (部品), its radical, and every word that contains it
- **Word node** → the kanji inside it
- Click any chip to hop to that node — the breadcrumb trail takes you back

## Find a path

The **Find path** tool answers "how is A connected to B?". Type a target expression and the graph walks real relations up to four hops: 食べる → 食 → 食事. It's a concrete, data-driven way to see the shape of the vocabulary you're learning.

## Where have I seen this?

Nodes carry media exposure: if you've mined cards from anime or other media, the word shows where it appeared — title and timestamp. Your reading is part of the graph.

## From exploring to studying

- **Practice** on any node starts a review session filtered to that exact word or kanji — exploration turns into SRS repetition in one click.
- **Mine** from the dictionary popup anywhere; mined words become cards.
- The **Curriculum** workspace turns the same study data into structured courses (kana foundation, JLPT paths) with objectives and auto-advance — so exploring the graph and following a course reinforce each other.

## The honest limits

- The graph is a read-model over your installed dictionaries: no dictionary data, no graph. Entries outside your dictionaries don't resolve.
- Path search is BFS over relation edges, capped at four hops.
- The Knowledge Graph and Curriculum workspaces are desktop features.

See the [graph documentation](/docs/architecture/node_architecture/) for the node model and [curriculum documentation](/docs/learning/curriculum-engine/) for the course engine.
