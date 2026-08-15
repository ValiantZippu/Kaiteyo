# Content Formats

**Status**: TARGET (schema contracts — ADR-0015). These schemas are the *contract*
for authored content; the pipeline (`content-pipeline.md`) validates them. Shape
may evolve, but the principles (validation, versioning, localization, provenance)
are fixed.

## Format family

| Format | Purpose | Key fields |
|---|---|---|
| `course.json` | a curriculum (lesson graph) | id, version, locale, lessons[], edges[] |
| `lesson.json` | one lesson (kana/kanji/vocab/grammar/reading/…unit) | id, version, kind, targets[], prereqs[], activities[] |
| `quest.json` | a quest (game) | id, version, type, prerequisites[], objectives[], rewards[], unlock[] |
| `dialogue.json` | NPC dialogue tree | id, version, npcRef, nodes[] (lines, choices, conditions, effects) |
| `story.json` | a graded story | id, version, difficulty vector, chapters[], sentences[] (knowledge links) |
| `exam.json` | an exam definition | id, version, sections[], question templates, scoring |
| `location.json` / `npc.json` / `world.json` | world content | see `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md` |
| `world-event.json` | scheduled events | id, window, location, content refs |

Every format shares a **common envelope**:

```json
{
  "schemaVersion": 1,
  "id": "lesson:hiragana-a",
  "version": "1.2.0",
  "kind": "lesson",
  "locale": ["ja", "en"],
  "license": "CC-BY-4.0",
  "source": "kaiteyo-authoring",
  "provenance": { "author": "...", "created": "...", "checksum": "..." },
  "content": { ... format-specific ... }
}
```

## Example: `quest.json`

```json
{
  "schemaVersion": 1,
  "id": "quest:errand-01",
  "version": "1.0.0",
  "kind": "quest",
  "type": "errand",
  "locale": ["ja", "en"],
  "content": {
    "title": { "ja": "お使い", "en": "The Errand" },
    "giver": "npc:shopkeeper-14",
    "prerequisites": [ { "quest": "quest:intro-01", "state": "completed" } ],
    "objectives": [
      { "id": "obj-1", "type": "interact",
        "condition": { "target": "location:bakery", "action": "talk", "count": 1 } },
      { "id": "obj-2", "type": "collect",
        "condition": { "item": "item:bread", "count": 1 },
        "dependsOn": ["obj-1"] }
    ],
    "rewards": [ { "type": "story_beat", "ref": "story:shopkeeper-trust" },
                 { "type": "collection", "ref": "collection:komachi-stamps" } ],
    "unlock": [ { "type": "quest", "ref": "quest:errand-02" } ],
    "failure": { "policy": "none" },
    "repeatable": false
  }
}
```

Validation notes: objective types are from the closed interaction catalog
(`docs/game/interaction-system.md`); rewards from the closed reward catalog
(`docs/game/progression-rewards.md`); references must resolve.

## Example: `dialogue.json`

```json
{
  "schemaVersion": 1,
  "id": "npc:shopkeeper-14/welcome",
  "version": "1.0.0",
  "kind": "dialogue",
  "content": {
    "npcRef": "npc:shopkeeper-14",
    "nodes": [
      { "id": "start",
        "lines": [
          { "speaker": "npc", "text": { "ja": "いらっしゃいませ！", "en": "Welcome!" },
            "knowledgeLinks": ["vocab:いらっしゃいませ"], "audioRef": "audio:...wav" }
        ],
        "choices": [ { "label": { "ja": "こんにちは", "en": "Hello" }, "next": "hello" } ],
        "effects": [] },
      { "id": "hello",
        "lines": [ { "speaker": "npc", "text": { "ja": "…", "en": "…" }, "knowledgeLinks": [] } ],
        "choices": [], "effects": [ { "type": "quest_progress", "ref": "quest:errand-01", "objective": "obj-1" } ] }
    ]
  }
}
```

## Example: `story.json`

```json
{
  "schemaVersion": 1,
  "id": "story:summer-day",
  "version": "1.0.0",
  "kind": "story",
  "content": {
    "difficulty": { "vocab": "N5", "grammar": "N4", "culture": "medium" },
    "chapters": [
      { "id": "ch-1",
        "sentences": [
          { "text": { "ja": "夏の鎌倉はとても暑いです。", "en": "Kamakura in summer is very hot." },
            "knowledgeLinks": ["vocab:夏", "vocab:鎌倉", "vocab:暑い", "grammar:は…です"],
            "furigana": { "夏": "なつ" } }
        ] }
    ],
    "comprehension": [
      { "id": "q-1", "type": "multiple_choice", "question": { "ja": "…", "en": "…" },
        "choices": [], "answer": 0 }
    ]
  }
}
```

## Example: `exam.json`

```json
{
  "schemaVersion": 1,
  "id": "exam:jlpt-n5-mock",
  "version": "1.0.0",
  "kind": "exam",
  "content": {
    "sections": [
      { "id": "vocab", "type": "jlpt_vocab", "band": "N5", "durationMs": 120000,
        "questionTemplate": { "types": ["multiple_choice"], "source": "deck:jlpt-n5-vocab",
                              "count": 30 } },
      { "id": "reading", "type": "jlpt_reading", "band": "N5", "durationMs": 180000,
        "storyRef": "story:exam-n5-1" }
    ],
    "scoring": { "partialCredit": false, "bandScope": "N5" }
  }
}
```

## Validation rules (every format, ADR-0015 gates)

1. Schema version present + supported.
2. `id` unique, scoped, path-valid; references resolve within package or
   declared dependency.
3. Enums closed (objective types, reward types, interaction types, question
   types) — unknown values fail.
4. Localization complete (both locales present for every user-visible string).
5. Provenance complete (license, source, checksum).
6. Size/content budgets (no oversized packages).
7. Difficulty metadata present for learning/story content (adaptive needs it).

## Localization

- Content strings are `{ "ja": ..., "en": ... }` maps (never externalized into
  code); future locales add keys without breaking validators.
- World content is localized like everything else (NPC names, signs, quests).
- Child mode adds a separate content surface (age-gated) — same format, flagged
  `audience: child` (validated by the child filter).

## Related

- Pipeline: [content-pipeline.md](content-pipeline.md)
- Authoring contract: `docs/architecture/nodes/CONTENT_AUTHORING.md`
- World data schema: `docs/architecture/nodes/JOURNEY_WORLD_SCHEMA.md`
- Localization: `docs/architecture/localization.md`
