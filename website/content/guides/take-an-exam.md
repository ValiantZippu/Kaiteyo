---
title: Test yourself with exams
description: Structured assessments built from your real card pool — matching, readings, cloze, ordering and timed questions, scored and reviewed.
---

Review sessions tell you whether you remembered *today's* cards. Exams tell you what you *actually know* — a structured assessment drawn from your real pool, scored across question types, and reviewed like any other session.

## Question types

The exam generator produces six deterministic question families from your own cards:

- **Matching** — pair the expression with its meaning
- **Reading** — choose the reading that fits
- **Cloze** — fill the gap in a real sentence
- **Ordering** — reconstruct a sentence from shuffled pieces
- **Free response** — type what you know (no hints)
- **Timed** — the same questions against a clock

Wrong-answer distractors are drawn from the real pool, so every option is a plausible thing you might actually confuse.

## Where exams come from

- **Weekly assessment** — a scheduled mixed exam of what's due
- **Mistakes review** — re-exam the cards you keep getting wrong
- **Kanji workshop** — the generator-driven mixed exam, always available from the exam type chips

You can start any of them from the Exams workspace, or straight from the command palette.

## The loop

**1. Pick an exam.** Choose a scope — weekly, mistakes, or workshop.

**2. Answer.** Each question is scored as you go: correct, wrong, or again.

**3. Review the result.** Scores roll into your study statistics, and missed cards go back into the normal review rotation where the SRS schedule picks them up.

## The honest limits

- Exam content is bounded by your own pool — a near-empty deck makes for a short exam.
- Free-response scoring is string-comparison based; close-but-not-exact kana/kanji variants are judged on the reading where possible.

See the [exam engine documentation](/docs/features/exams/) for the generator design and scoring rules.
