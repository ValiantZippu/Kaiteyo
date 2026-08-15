# Child Experience

**Status**: TARGET — doctrine + framework (NODE §115, GAMEPLAY_SYSTEMS §22). The child
mode is specified but not implemented; it runs on the **same runtime and core** as
the adult experience (ADR-0014 shares one Journey runtime; §71: shared core, different
content/UX/difficulty/presentation).

## Principle

The child experience is **NOT** "adult Kaiteyo + colorful buttons." It is a different
instructional structure: how a child learns Japanese (age 3 → 12) differs from how an
adult self-learner does — in vocabulary, interaction complexity, audio dependence,
feedback style, motivation, and what "progress" means. Technology stays shared; the
learning design diverges deliberately.

## Age bands (design framework, not hard truth)

Treat as guidance — **curriculum follows learner ability, not age alone** (§12).
Ability assessment (what the child can read, recognize, write, understand by ear)
overrides calendar age in every decision.

| Band | Focus | Interaction | Audio dependence | Visual language |
|---|---|---|---|---|
| 2–3 | Phonemes, kana shapes, first words (nouns) | Tap, big targets, no reading required | **High** — everything speaks | Soft shapes, warm palette, one focal point |
| 3–4 | Kana recognition, 2–4 mora words, counting, colors, animals | Tap + simple drag, instant audio feedback | High | Characters, repetition with variation |
| 4–5 | Kana production (trace), simple sentences, family/body/weather | Tracing + tap; guided; no text menus | High (menus read aloud) | Larger UI scale, clear affordances |
| 5–6 | First kanji (numbers, days, basic), reading short labels | Tap, trace, short tasks (2–3 min) | Medium-high | Icon-led, minimal text |
| 6–8 | Kanji + vocabulary families, simple stories, writing with feedback | Full interaction set, still guided | Medium (text becomes primary) | Structured learning pages, gentle rewards |
| 8–10 | Kanji compounds, grammar patterns, sentences, reading practice | Adult-like interactions, child-tuned content | Medium (audio support) | Cleaner layout, achievement clarity |
| 10–12 | Bridge to adult curriculum; JLPT-adjacent content, media practice | Standard | Low (optional) | Standard design, reduced decoration |
| 12+ | Adult curriculum optionally on (with parent consent/guardian profile) | Standard | Low | Standard |

## What differs from the adult experience

| Aspect | Adult (default) | Child mode |
|---|---|---|
| Curriculum | JLPT + self-directed | Age-band curricula, thematic (animals, family, food, seasons) |
| Instruction | Flashcards, exams, reading, mining | Songs, stories, characters, guided quests, games |
| Feedback | Neutral, data-driven | Encouraging, immediate, character-driven (no failure shame) |
| Progression | Knowledge states, statistics, intervals | Visible collection/quest progress, celebrations; SRS runs silently underneath |
| Statistics | Full dashboards, heatmaps, analytics | **Hidden or simplified** — "what you learned today" only |
| Dictionary | Full entries, pitch, etymology | Simplified glosses, furigana everywhere, audio-first |
| Writing | Stroke evaluation, accuracy analytics | Trace-and-cheer, shape-only feedback at low bands |
| Rewards | Achievements, collections | Stamps, collectibles, story unlocks (still no XP/grind) |
| Content filters | Full world | **Safe world profile**: language difficulty caps, no dark/cultural-complex content, curated NPCs/quests |
| Time controls | None (user-managed) | Parent-set session limits, gentle end-of-session |
| Parent features | — | Guardian profile: age band, content filter, daily limits, activity summary |

## What stays shared (never duplicated)

- Same runtime, same save system, same event bus, same knowledge model, same SRS
  engine (FSRS), same dictionary data, same media/mining engines, same rendering.
- Child mode is a **configuration + content filter + presentation layer** over the
  same Journey runtime (§115, GAMEPLAY_SYSTEMS §22) — not a second game.

## Design rules

1. **Ability over age**: onboarding asks "what can the child do?" (read kana? read
   some kanji? nothing?) and sets the band; parents can adjust anytime.
2. **Audio first**: at low bands, every word has TTS/sound; text supports audio,
   never the reverse.
3. **No abstract failure**: wrong answers in practice = "let's try again with a
   hint", never red X's, streaks lost, or visible error counts at low bands.
4. **Session shape**: 3–5 minute activities at 3–6, 5–10 minutes at 6–10; the app
   ends sessions gracefully ("we found 10 words today!").
5. **Characters**: one consistent cast across songs/stories/quests (world characters
   double as learning guides; §113).
6. **Handwriting first**: children write early (trace → freehand) because writing
   anchors kana/kanji memory; stroke evaluation is tuned to be encouraging.
7. **Learning data is real**: a child's knowledge state flows into the same
   knowledge model — when they outgrow child mode, the adult app already knows them.
   No separate fake progress.
8. **Songs**: nursery-song-style audio content with kana captions is a first-class
   content type (AUDIO/CONTENT production; content format docs in `docs/content/`).

## Content requirements

- Child content is **authored, age-gated, and validated** (ADR-0015 gates: no
  kanji above band cap, vocabulary whitelist/blacklist, audio required, reading
  level enforced). Authoring format: `docs/content/content-formats.md`.
- The world content filter selects NPCs/quests/stories flagged child-safe; the map
  shows the same world with filtered content, never a stub world.
- Localization: same i18n system; child strings are separate keys, always
  available in English + Japanese (parent language + target language).

## Definition of done for child mode

1. A child can start the world alone and complete a guided activity with zero text
   menus (audio + icons only) at bands 2–5.
2. All learning events flow to the same knowledge model and statistics as adult
   study (verified, honest data).
3. Parent controls (band, content filter, time limits) work and persist per profile.
4. No child-mode data path exists that duplicates or bypasses the shared core.

## Related

- Spec: NODE §115, GAMEPLAY_SYSTEMS §22, `docs/architecture/nodes/CONTENT_AUTHORING.md`
- Philosophy: `docs/vision/game-philosophy.md` (no-XP rules apply equally)
- Learning doctrine: `docs/vision/learning-philosophy.md`
- Content formats: `docs/content/content-formats.md`
