---
title: Study with a structured curriculum
description: The Curriculum workspace turns your real study data into structured courses — pick a path, and objectives advance as you actually review and mine.
---

The Curriculum workspace is the structured counterpart to free-form study. Instead of just "do reviews until the deck is empty," you pick a course — Kana Foundation, JLPT N5, JLPT N4 — and the app measures your real study data against each lesson's objectives. Complete an objective, and the next one unlocks.

It is not a separate review mode. It reads the same card pool and review log you already use, so a normal review session advances your course automatically.

## How it works

Each course is a sequence of lessons. Every lesson has objectives measured against your live data:

- **Review objectives** — complete N reviews in a deck (or overall)
- **Mining objectives** — mine N new cards
- **Knowledge objectives** — reach a count of cards at a given SRS stage

Completion detection runs against real data, so a "deck not installed" objective can never stall you — it just stays pending and the course continues past it. Progress auto-advances and persists to `curriculum.json`, so quitting mid-course resumes exactly where you left off.

## The loop

**1. Pick a course.** The Curriculum view lists built-in paths — Kana Foundation, JLPT N5, JLPT N4. Courses reference real deck ids; install the matching deck and every objective lights up.

**2. Follow the objectives.** Each lesson shows its objectives with live progress. Review, mine, or study normally — the numbers tick up as your data changes.

**3. Auto-advance.** Finish the last objective and the next lesson opens. Completion is saved per course, per lesson.

**4. Switch freely.** You can exit a course at any time and pick another; progress on each course is kept independently.

## What it measures

Completion is always derived from your actual card pool and review history — never from a timer or a self-reported checkbox. That makes course progress an honest mirror of the work you've actually done.

## The honest limits

- Built-in courses currently cover the kana foundation and the JLPT N5/N4 kanji paths; deeper levels are a natural next step.
- Course progress is desktop-local (the same persistence as your library).

See the [curriculum engine documentation](/docs/features/curriculum/) for the data model, and the [desktop suite overview](/docs/user-guide/desktop-suite/) for where it lives in the app.
