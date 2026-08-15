---
title: How statistics work
description: The architecture behind Kaiteyo's numbers — what's recorded, how it's aggregated, and what each metric means.
---

Every statistic in Kaiteyo is computed from real database records — nothing is hardcoded or fabricated. This article is the architecture behind the numbers.

## Recording

Study activity is written to the user database as it happens: daily rollups, review-per-item totals, writing attempts, study sessions, exam records and questions, and learning mistakes. A `StatisticsRecorder` keeps daily counters updated incrementally — no full-history scans on every answer.

## Aggregation

A `StatisticsController` (Koin singleton) sits in front of the recording layer and feeds the statistics screen. Aggregates come from dedicated calculators, each answering one question:

| Calculator | Question |
|---|---|
| Profile | What am I strong and weak at? |
| Velocity | How fast am I going? |
| Deck retention | Which decks are weak? |
| Knowledge growth | How has my knowledge changed over time? |
| Weekly exam | What should this week's exam cover? |
| Goal history | Do I complete my goals? |

All six are pure functions (no Compose, no DB), covered by unit tests.

## Metric definitions

These definitions are shared everywhere, so a number means the same thing on every screen:

- **Studied** — at least one recorded review
- **Learned** — reviewed with an FSRS interval ≥ 1 day
- **Mature** — interval ≥ 21 days; **Mastered** — interval ≥ 180 days
- **Weak** — ≥ 3 lapses on the FSRS card
- **Retention** — correct ÷ total over the window (per grade, per interval bucket, per deck)
- **Velocity window** — trailing 30 local days, expressed weekly

## Exams

Exams are generated from studied content — kanji from the studied catalog (with radicals/stroke counts), vocabulary from user decks — with JLPT distribution, production + recognition question types, and valid distractors. A reproducible seed means the same input produces the same exam. Wrong answers become learning-mistake records.

## Export & privacy

The statistics system can export a human-readable report, CSV daily rollups, or a JSON aggregate (no internal DB details). Everything is computed **offline** — no statistics ever leave the device.
