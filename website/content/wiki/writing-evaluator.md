---
title: The writing evaluator
description: How Kaiteyo grades handwriting — stroke matching, direction, order, and strictness.
---

When you draw a kanji in Kaiteyo, the evaluator compares your strokes against the reference stroke data in real time. This article is how that works under the hood.

## Reference data

Every kanji in the writing system carries a stroke set derived from **KanjiVG** — each stroke is a path with a defined order, direction, start and end point. The KJD data platform normalizes these into the geometry the evaluator uses, with per-stroke bounds computed up front.

## What gets graded

For each stroke you draw, the evaluator checks:

- **Order** — strokes are expected in the reference sequence. The first stroke you draw must be stroke 1, the second stroke 2, and so on.
- **Direction** — the path direction of the drawn stroke relative to the reference.
- **Start / end position** — where the stroke begins and ends, compared to the reference stroke's endpoints.
- **Shape** — the overall path of the stroke, not just its endpoints.

## Strictness

The evaluator has three strictness modes — **Relaxed**, **Normal**, and **Exam** — which scale how tight the tolerances are for position and shape. Relaxed is forgiving enough for first contact with a kanji; Exam applies the same bar used in exam mode.

## How it feeds the rest of the app

Writing attempts and outcomes are recorded through the statistics pipeline, so writing accuracy is tracked per deck and over time. Writing cards run through the same scheduler as recognition cards — writing is a first-class study mode, not an add-on. Writing mistakes surface in weakness analytics like any other mistake type.

## Implementation

The evaluator lives in the shared core (stroke matching and scoring are pure logic, unit-tested); the drawing surface is a brush canvas with smoothing, prediction, and pressure support. See the repository's stroke evaluator tests for the exact scoring semantics.
