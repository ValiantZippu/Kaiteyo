# Node Type Registry

**Status**: TARGET (blueprint for the node layer; see ADR-0013)
**Source spec**: [Node Architecture master spec](../NODE_ARCHITECTURE.md) §76–§83, §149, §151
**Purpose**: every `nodeType` in Kaiteyo, its family, fields, provenance, and typical
relationships — the machine-readable contract behind the node system.

> **Legend — status**: `CURRENT` = exists today in some form (see `docs/planning/PRODUCT_AUDIT.md`),
> `TARGET` = specified, not implemented, `FUTURE` = intentionally postponed.
> **Legend — fields**: `U` = universal contract fields (§78) always present; entries below
> list family-specific fields only. All fields are typed; nullable fields are marked `?`.
> **Registry process**: adding a type = a row here + schemaVersion + an ADR note when it
> changes storage. Never invent node types inline (STANDARDS §370).

## 1. LANGUAGE family

| nodeType | Parent | Family-specific fields | Sources | Typical relationships (out) | Status |
|---|---|---|---|---|---|
| `script` | — | `name`, `writingSystem` | kjd, user | `contains` → kana | CURRENT |
| `kana` | script | `character`, `reading`, `type` (hiragana/katakana), `dakuten`? | kjd | `has_reading`, `appears_in` → word | CURRENT |
| `kanji` | — | `character`, `meanings[]`, `readings[]`, `jlpt`?, `grade`?, `frequency`? | kjd, kanjidic | `contains_component`, `uses_radical`, `has_reading`, `appears_in` → word/sentence/media | CURRENT |
| `component` | kanji | `character`, `strokes` | kjd | `contains` → kanji | TARGET |
| `radical` | kanji | `number`, `name`, `character`? | kjd | `uses_radical` (kanji→radical), `contains` → kanji | TARGET |
| `vocabulary` | — | `headword`, `readings[]`, `meanings[]`, `partOfSpeech`, `jlpt`?, `frequency`?, `pitch`? | jmdict, kjd | `contains_character` → kanji, `has_reading`, `conjugates_to`, `appears_in_sentence` | CURRENT |
| `expression` | vocabulary | `headword`, `readings[]`, `meaning`, `notes`? | jmdict, user | `synonym_of`, `antonym_of`, `appears_in_sentence` | TARGET |
| `reading` | kanji/vocabulary | `text`, `type` (on/kun/irregular), `kana` | kjd | `belongs_to` → kanji/vocabulary | CURRENT |
| `meaning` | kanji/vocabulary | `text`, `language`, `glossType` | kjd | `belongs_to` → kanji/vocabulary | CURRENT |
| `grammar` | — | `name`, `pattern`, `meaning`, `jlpt`?, `notes` | kjd, user | `demonstrates` → sentence, `teaches` → objective | TARGET |
| `conjugation` | vocabulary/grammar | `form`, `surface`, `reading` | kjd | `conjugates_to` (base↔form), `appears_in_sentence` | TARGET |
| `sentence` | — | `text`, `translation`?, `furigana`?, `source` | tatoeba?, user, kaiteyo-world | `contains` → vocabulary/kanji, `demonstrates` → grammar | TARGET (RESEARCH: dataset) |
| `paragraph` | sentence | `text`, `order` | kaiteyo-world, user | `contains` → sentence | TARGET |
| `story` | paragraph | `title`, `body`, `level` | kaiteyo-world, user | `contains` → paragraph | TARGET |
| `pitch_pattern` | vocabulary | `pattern` (e.g. 平板), `notes` | external dataset (RESEARCH) | `has_pitch` (vocab→pitch) | TARGET |
| `frequency_entry` | vocabulary/kanji | `rank`, `corpus`, `value` | kjd, frequency datasets | `has_frequency` | TARGET |
| `pronunciation` | vocabulary/kanji | `audioUri`, `voice`, `locale` | kjd TTS, user | `has_pronunciation` | CURRENT (TTS) |

## 2. LEARNING family

| nodeType | Parent | Family-specific fields | Sources | Typical relationships | Status |
|---|---|---|---|---|---|
| `course` | — | `title`, `language`, `level`, `objectives[]` | kaiteyo, user | `contains` → lesson, `teaches` → objective | TARGET |
| `lesson` | course | `title`, `order`, `level` | kaiteyo, user | `contains` → topic, `requires` → lesson | TARGET |
| `topic` | lesson | `title`, `contentRef` | kaiteyo, user | `contains` → objective/exercise | TARGET |
| `objective` | topic/course | `title`, `knowledgeDimension` (§84) | kaiteyo | `teaches` → node; `assessed_by` → question | TARGET |
| `exercise` | topic/objective | `type`, `prompt`, `config` | kaiteyo, user | `exercises` → node; `contains` → question | TARGET |
| `question` | exercise/exam | `type`, `prompt`, `answer`, `options[]`?, `distractors[]` | kaiteyo, user | `assesses` → node; `references` → content | TARGET (exam engine CURRENT, partial) |
| `exam` | — | `title`, `mode`, `level`?, `questions[]` | kaiteyo, user | `contains` → question, `evaluates` → knowledge | TARGET (exam engine CURRENT, partial) |
| `deck` | — | `name`, `kind` (letter/vocab), `isArchived`, `settings` | user, kaiteyo, anki | `contains` → card/note, `reviews` via card | CURRENT |
| `note` | deck | `fields`, `template`? | user, anki | `belongs_to` → deck, `generates` → card | CURRENT |
| `card` | note/deck | `kind`, `srsState` (FSRS), `due`, `ease`, `state`, `lapses` | user | `belongs_to` → deck/note, `teaches` → node, `reviews` → review | CURRENT |
| `review` | card | `result`, `elapsed`, `scheduledInterval`, `timestamp` | user | `reviews` (card→node), `updates` → user_knowledge | CURRENT |
| `study_session` | — | `startedAt`, `endedAt`, `kind` | user | `participates_in` → review | CURRENT, partial |
| `user_knowledge` | language node | `dimension`, `state`, `score`, `evidence[]` | kaiteyo (derived) | `mastered_by` (node→knowledge), `derived_from` → events | TARGET (model: `KNOWLEDGE_STATE_MODEL.md`) |
| `mastery_state` | user_knowledge | `level`, `confidence`, `dimensions` | kaiteyo (derived) | `mastered_by`, `depends_on` → user_knowledge | TARGET |

## 3. MEDIA family

| nodeType | Parent | Family-specific fields | Sources | Typical relationships | Status |
|---|---|---|---|---|---|
| `media_source` | — | `uri`, `type`, `hash`?, `metadata` | user, integration | `imported_from`, `contains` → series/episode | TARGET |
| `series` | media_source | `title`, `kind` (anime/movie/show), `externalId`? | integration (AniList et al., §292) | `contains` → episode | TARGET (desktop suite metadata partial) |
| `anime` | series | `titleJa`, `titleEn`, `studios`, `airedAt` | integration | `belongs_to` → series | TARGET |
| `movie` | series | `title`, `duration` | user, integration | `belongs_to` → series | TARGET |
| `episode` | series | `number`, `title`, `duration`, `uri` | user, integration | `belongs_to` → series, `contains` → scene/video | TARGET |
| `video` | episode/media_source | `uri`, `codec`, `duration`, `start`? | user | `belongs_to` → episode, `has` → subtitle_track | CURRENT, partial |
| `audio` | media_source | `uri`, `format`, `duration`, `clipStart`?, `clipEnd`? | user | `mined_from`, `belongs_to` → media | CURRENT, partial |
| `subtitle_track` | episode/video | `language`, `format`, `fileRef` | user, kaiteyo | `contains` → subtitle_line | CURRENT, partial |
| `subtitle_line` | subtitle_track | `text`, `start`, `end`, `speaker`? | user, kaiteyo | `appears_in` (word→line), `contains` → vocabulary | CURRENT, partial |
| `scene` | episode | `start`, `end`, `title`?, `description`? | kaiteyo, user | `appears_in_scene`, `contains` → subtitle_line/screenshot | TARGET |
| `screenshot` | scene/media | `imageUri`, `timestamp`, `ocrText`? | user | `depicts` → object/node, `mined_from` | CURRENT, partial (suite OCR) |
| `clip` | video/audio | `start`, `end`, `name`? | user | `belongs_to` → video/audio, `mined_from` | CURRENT, partial |
| `mining_event` | — | `sourceKind`, `payloadRef`, `timestamp`, `result` | kaiteyo | `mined_from` (card←source), `generated_from` → card | CURRENT, partial |

## 4. WORLD family

| nodeType | Parent | Family-specific fields | Sources | Typical relationships | Status |
|---|---|---|---|---|---|
| `world` | — | `worldId`, `version`, `regionSummary` | kaiteyo-world | `contains_location` → region | TARGET |
| `region` | world | `nameJa`, `nameEn`, `bounds`, `summary` | kaiteyo-world | `contains_location` → prefecture/city | TARGET |
| `prefecture` | region | `nameJa`, `nameEn`, `code` | kaiteyo-world | `contains_location` → city | TARGET |
| `city` | prefecture | `nameJa`, `nameEn`, `bounds` | kaiteyo-world | `contains_location` → district | TARGET |
| `district` | city | `nameJa`, `nameEn`, `bounds` | kaiteyo-world | `contains_location` → neighborhood/cell | TARGET |
| `neighborhood` | district | `nameJa`, `nameEn`, `bounds` | kaiteyo-world | `contains_location` → street/cell | TARGET |
| `map_cell` | district/neighborhood | `cellX`, `cellY`, `size`, `contentRefs[]` | kaiteyo-world | `contains_location` → location; `contains` → object | TARGET |
| `street` | neighborhood/city | `nameJa`, `nameEn`, `geometryRef`, `length` | kaiteyo-world, geodata | `contains_location` → building/shop | TARGET |
| `building` | street/cell | `name`, `nameJa`, `type`, `geometryRef` | kaiteyo-world | `contains_location` → interior/shop/restaurant | TARGET |
| `interior` | building | `name`, `layoutRef`, `lightingRef` | kaiteyo-world | `contains` → object/interaction | TARGET |
| `landmark` | district/city | `nameJa`, `nameEn`, `description`, `geometryRef` | kaiteyo-world | `represents` → vocabulary/kanji; `depicts` | TARGET |
| `station` | district/railway | `nameJa`, `nameEn`, `lines[]`, `platforms[]` | kaiteyo-world | `represents` → vocabulary; `located_at`; `belongs_to` → railway | TARGET |
| `road` | district/city | `name`, `kind`, `geometryRef` | kaiteyo-world, geodata | `contains_location` → street | TARGET |
| `railway` | region/prefecture | `name`, `lineRefs[]`, `stations[]` | kaiteyo-world | `contains_location` → station | TARGET |
| `beach` | city/region | `nameJa`, `nameEn`, `bounds` | kaiteyo-world | `located_at`, `represents` → vocab | TARGET |
| `park` | city/district | `name`, `nameJa`, `bounds` | kaiteyo-world | `located_at`, `contains` → object | TARGET |
| `shop` | building/street | `name`, `nameJa`, `kind`, `hours`, `menuRefs[]` | kaiteyo-world | `represents` → vocab; `contains` → object; `participates_in` → quest | TARGET |
| `restaurant` | building/street | `name`, `nameJa`, `cuisine`, `menuRefs[]` | kaiteyo-world | `represents` → food vocab; `contains` → object | TARGET |
| `school` | building/district | `name`, `nameJa`, `kind` | kaiteyo-world | `located_at`, `participates_in` → quest | TARGET |
| `aquarium` | building/landmark | `nameJa`, `nameEn`, `exhibits[]` | kaiteyo-world | `contains` → exhibit object; `represents` → marine vocab | TARGET |
| `shrine` | district/city | `nameJa`, `nameEn`, `deity`?, `festivals[]` | kaiteyo-world | `represents` → vocab; `participates_in` → event | TARGET |
| `temple` | district/city | `nameJa`, `nameEn`, `sect`? | kaiteyo-world | `represents` → vocab | TARGET |
| `natural_feature` | region/city | `name`, `nameJa`, `kind` (mountain/sea/river) | kaiteyo-world | `located_at`, `represents` → vocab | TARGET |

## 5. GAMEPLAY family

| nodeType | Parent | Family-specific fields | Sources | Typical relationships | Status |
|---|---|---|---|---|---|
| `player` | — | `profileRef`, `cameraPrefs`, `position` | kaiteyo (save) | `participates_in` → quest/story; `owns` → avatar | TARGET |
| `avatar` | player | `appearance`, `clothing`, `accessories`, `animSet` | kaiteyo | `belongs_to` → player | TARGET |
| `npc` | — | `identity`, `occupation`, `ageCategory`, `homeCell`, `scheduleRef` | kaiteyo-world | `participates_in` → dialogue/quest; `located_at` | TARGET |
| `npc_schedule` | npc | `slots[]` (time-of-day × weekday/weekend × season × weather), `activityRefs` | kaiteyo-world | `scheduled_at` → activity/location | TARGET |
| `interaction` | — | `type` (from §94 registry), `objectRef`, `conditions`, `effects[]` | kaiteyo-world | `participates_in` → dialogue/quest; `references` → object | TARGET |
| `activity` | — | `kind`, `duration`, `locationRef`, `exposureNodes[]` | kaiteyo-world | `scheduled_at`, `teaches` → node (ambient) | TARGET |
| `quest` | — | `title`, `kind` (§100), `level`, `objectives[]`, `rewards[]`, `giverNpc`? | kaiteyo-world | `contains` → quest_objective, `unlocks` → node/quest, `rewards` → reward | TARGET |
| `quest_objective` | quest | `type`, `conditionRef`, `targetRef`, `progress`, `order` | kaiteyo-world | `requires` → node/interaction; `references` → world object | TARGET |
| `story` (gameplay) | — | `title`, `chapters[]`, `playerLevel`? | kaiteyo-world | `contains` → story_beat, `requires` → quest | TARGET |
| `story_beat` | story | `sceneRef`, `dialogueRefs[]`, `choices[]`, `outcomes[]` | kaiteyo-world | `precedes`/`follows` → story_beat; `references` → dialogue | TARGET |
| `dialogue` | — | `speakerRef`, `textJa`, `translation`, `furigana`, `voiceRef`?, `emotion`, `choices[]`, `conditions`, `effects[]`, `knowledgeNodes[]` | kaiteyo-world | `teaches` → node (ambient), `participates_in` → story | TARGET |
| `discovery` | — | `kind` (word/kanji/location/object/npc/food/sign/media/cultural), `foundAt`, `source` | kaiteyo (derived) | `discovered_by` (user), `encountered_by` → node, `belongs_to` → collection | TARGET |
| `collection` | — | `title`, `kind` (§110), `coverRef`? | user, kaiteyo | `belongs_to` → member nodes | TARGET |
| `photograph` | — | `imageUri`, `timestamp`, `locationRef`, `filters`, `recognizedNodes[]` | user | `depicts` → object/node, `belongs_to` → collection, `mined_from` | TARGET |
| `achievement` | — | `title`, `description`, `conditionRef` | kaiteyo | `rewards` → reward | CURRENT (app achievements exist), world achievements TARGET |
| `reward` | — | `kind` (cosmetic/filter/collection/journal/music/location/story), `payloadRef` | kaiteyo | `unlocks` → node | TARGET |
| `event` | — | `name`, `season`?, `window`, `conditionRefs[]` | kaiteyo-world | `participates_in` → quest/activity | TARGET |
| `season` | world | `kind` (spring/summer/autumn/winter), `start`, `end` | kaiteyo-world | `depends_on` → weather; affects content variants | TARGET |
| `weather_state` | cell/region | `kind`, `startedAt`, `duration` | kaiteyo-world | `depends_on` → cell; `affects` → npc/quest | TARGET |
| `day_night_state` | world | `timeOfDay`, `sunState` | kaiteyo-world | `depends_on` → world clock | TARGET |

## 6. USER family

| nodeType | Parent | Family-specific fields | Sources | Typical relationships | Status |
|---|---|---|---|---|---|
| `profile` | — | `name`, `avatarRef`, `language`, `createdAt` | user | `owns` → preferences/goals | CURRENT, partial |
| `preferences` | profile | `settingsRef`, `appearanceRef` | user | `belongs_to` → profile | CURRENT |
| `goal` | profile | `title`, `kind` (daily/target/course), `target`, `progress` | user | `depends_on` → course/knowledge | TARGET |
| `knowledge_state` | profile | `dimensionScores[]` (§85) | kaiteyo (derived) | `derived_from` → user_knowledge | TARGET |
| `study_history` | profile | `eventsRef[]`, `summary` | kaiteyo (derived) | `depends_on` → review/study_session | CURRENT, partial |
| `media_history` | profile | `watchedRefs[]`, `playbackPositions[]` | kaiteyo (derived) | `depends_on` → episode/video | CURRENT, partial |
| `journey_progress` | profile | `worldRef`, `position`, `storyState`, `questState` | kaiteyo (save) | `depends_on` → quest/story/discovery | TARGET |
| `discovery_history` | profile | `discoveryRefs[]`, `summary` | kaiteyo (derived) | `depends_on` → discovery | TARGET |
| `quest_progress` | profile | `questRefs[]`, `objectiveProgress[]` | kaiteyo (save) | `depends_on` → quest | TARGET |
| `exam_history` | profile | `examRefs[]`, `scores[]`, `questionVersions` | kaiteyo | `depends_on` → exam | CURRENT, partial |
| `deck_ownership` | profile | `deckRefs[]`, `perDeckSettings` | user | `owns` → deck | CURRENT |
| `collection_ownership` | profile | `collectionRefs[]`, `itemProgress` | user | `owns` → collection | TARGET |

## 7. SYSTEM family

| nodeType | Parent | Family-specific fields | Sources | Typical relationships | Status |
|---|---|---|---|---|---|
| `source` | — | `name`, `url`, `license`, `version`, `checksum`, `attribution` | kjd | `generated_from` → nodes | CURRENT, partial |
| `dataset` | source | `datasetName`, `version`, `transformVersion`, `schemaVersion` | kjd | `generated_from` → nodes | CURRENT, partial |
| `integration` | — | `kind` (anki/yomitan/ankiweb…), `status`, `configRef` | user, integration | `imported_from` → nodes | CURRENT |
| `plugin` | — | `manifest`, `version`, `permissions[]`, `apiVersion` | user, kaiteyo | `depends_on` → capability | FUTURE (ADR-0011) |
| `theme` | — | `name`, `tokenRef`, `source` | user, kaiteyo | `depends_on` → configuration | CURRENT |
| `asset` | — | `uri`, `type`, `hash`, `size`, `license` | kaiteyo-world, kaiteyo | `depicts`/`references` → nodes | CURRENT, partial |
| `feature_flag` | — | `name`, `enabled`, `owner`, `expiry`? | kaiteyo | — | CURRENT, partial |
| `configuration` | — | `key`, `value`, `scope`, `owner` | user, kaiteyo | — | CURRENT |

## 8. Registry rules

1. **One node, one `nodeType`, one family.** A node never masquerades as another type.
2. **Fields are additive with `schemaVersion` bumps** — never repurposed silently.
3. **`status` lifecycle** (`active`/`archived`/`suspended`/`hidden`/`draft`) is orthogonal
   to knowledge state (§84).
4. **Provenance** (`source`, `sourceId`) is mandatory per §78 — especially for imported
   content; derived nodes must say so (`source = "kaiteyo"` + `derived_from` edges).
5. **Adding a type** requires: registry row, schemaVersion, validation rule (§148),
   and — if storage changes — an ADR note.

## 9. Status summary

| Family | Status today | Gap to close first |
|---|---|---|
| LANGUAGE | CURRENT, partial | components/radicals/pitch/grammar datasets (RESEARCH), sentence corpus |
| LEARNING | CURRENT, partial | course/lesson/topic/objective model, user_knowledge |
| MEDIA | CURRENT, partial | series/scene graph, subtitle line model as nodes |
| WORLD | TARGET | everything (world packages, §145) |
| GAMEPLAY | TARGET | everything (runtime, ADR-0014) |
| USER | CURRENT, partial | goals, journey/discovery/quest progress |
| SYSTEM | CURRENT, partial | source/dataset provenance records |
