# Journey Vertical Slice — Worked Content (Kamakura + Enoshima)

**Status**: TARGET — reference content. These files are *examples of the contract*: they
must validate against the schemas in [JOURNEY_WORLD_SCHEMA](JOURNEY_WORLD_SCHEMA.md) and
the [content pipeline](CONTENT_AUTHORING.md) gates (§148). They are NOT shipped content —
the actual slice is a CONTENT/3D/ART/AUDIO PRODUCTION effort behind the §91 proof gate.
**Source spec**: [Node Architecture master spec](../NODE_ARCHITECTURE.md) §91, §87–§113 ·
[JOURNEY_WORLD_SCHEMA](JOURNEY_WORLD_SCHEMA.md)

> **Purpose**: one complete, coherent slice — the convenience-store loop (§87), a train
> ride (§104), a quest chain (§100–§102), photography (§95), collections (§110),
> discoveries (§111), and difficulty adaptation (§113) — so the next agent can see every
> schema populated, validate the pipeline, and build the runtime against known data.
> All Japanese strings are illustrative placeholders; real content requires human
> review + localization validation (§148).

---

## 1. World package manifest

```json
{
  "manifest": {
    "packageId": "world.kanagawa.kamakura-enoshima.v1",
    "kind": "world",
    "version": "1.0.0",
    "minEngineVersion": "3.0.0",
    "dependencies": [
      {"packageId": "geo.kanagawa", "version": ">=1.0.0"},
      {"packageId": "data.jmdict", "version": ">=15"}
    ],
    "contentHash": "sha256:<computed-at-build>",
    "license": "CC-BY-SA-4.0 (content), ODbL (geodata-derived layout)",
    "creator": "Kaiteyo World Team",
    "attribution": "Street layout derived from OpenStreetMap (© OpenStreetMap contributors, ODbL). Language data: JMdict (CC BY-SA 3.0).",
    "localization": {"requiredLocales": ["ja", "en"]}
  },
  "world": {
    "worldId": "japan",
    "name": "日本",
    "nameEn": "Japan",
    "regions": ["region:kanto"]
  }
}
```

## 2. Region → city → district

```json
[
  {
    "id": "region:kanto",
    "nodeType": "region",
    "parentId": "world:japan",
    "name": "関東", "nameEn": "Kanto",
    "bounds": [35.0, 139.0, 36.5, 141.0],
    "prefectures": ["pref:kanagawa"]
  },
  {
    "id": "pref:kanagawa",
    "nodeType": "prefecture",
    "parentId": "region:kanto",
    "name": "神奈川県", "nameEn": "Kanagawa",
    "bounds": [35.2, 138.9, 35.6, 139.9],
    "cities": ["city:kamakura", "city:fujisawa"]
  },
  {
    "id": "city:kamakura",
    "nodeType": "city",
    "parentId": "pref:kanagawa",
    "name": "鎌倉市", "nameEn": "Kamakura",
    "bounds": [35.30, 139.50, 35.34, 139.57],
    "districts": ["district:komachi", "district:yuigahama"]
  },
  {
    "id": "district:komachi",
    "nodeType": "district",
    "parentId": "city:kamakura",
    "name": "小町通り", "nameEn": "Komachi-dori",
    "bounds": [35.317, 139.548, 35.322, 139.552],
    "cells": ["cell:komachi/07", "cell:komachi/08"]
  },
  {
    "id": "district:yuigahama",
    "nodeType": "district",
    "parentId": "city:kamakura",
    "name": "由比ヶ浜", "nameEn": "Yuigahama",
    "cells": ["cell:yuigahama/01"]
  },
  {
    "id": "city:fujisawa",
    "nodeType": "city",
    "parentId": "pref:kanagawa",
    "name": "藤沢市", "nameEn": "Fujisawa",
    "districts": ["district:enoshima"]
  },
  {
    "id": "district:enoshima",
    "nodeType": "district",
    "parentId": "city:fujisawa",
    "name": "江の島", "nameEn": "Enoshima",
    "cells": ["cell:enoshima/01", "cell:enoshima/02"]
  }
]
```

## 3. Cells

```json
[
  {
    "id": "cell:komachi/07",
    "nodeType": "map_cell",
    "parentId": "district:komachi",
    "x": 7, "y": 3, "size": [96, 64],
    "terrainRef": "assets/terrain/komachi-07.glb",
    "geometryRefs": ["assets/geometry/komachi-07.glb"],
    "audioRefs": ["assets/audio/ambient/komachi-crowd.ogg"],
    "lightingRef": "assets/lighting/komachi.json",
    "navMeshRef": "assets/nav/komachi-07.navmesh",
    "npcs": ["npc:tanaka"],
    "objects": [
      "object:shop-14", "object:onigiri-shelf", "object:vending-03",
      "object:sign-komachi", "object:station-kamakura", "object:tree-12"
    ],
    "knowledgeNodes": ["lang:vocab/おにぎり", "lang:vocab/コンビニ", "lang:kanji/食"],
    "questNodes": ["quest:errand-01"],
    "weatherState": {"default": "clear", "overrides": {"rain": {"objects": ["object:vending-03"]}}}
  },
  {
    "id": "cell:yuigahama/01",
    "nodeType": "map_cell",
    "parentId": "district:yuigahama",
    "x": 0, "y": 0, "size": [256, 128],
    "terrainRef": "assets/terrain/yuigahama-01.glb",
    "audioRefs": ["assets/audio/ambient/ocean.ogg", "assets/audio/ambient/gulls.ogg"],
    "npcs": ["npc:lifeguard", "npc:beachgoer"],
    "objects": ["object:beach-01", "object:seaside-shop"],
    "knowledgeNodes": ["lang:vocab/海", "lang:vocab/波"],
    "questNodes": ["quest:beach-photo-01"]
  }
]
```

## 4. Objects (world object system §93)

```json
[
  {
    "id": "object:shop-14",
    "nodeType": "shop",
    "parentId": "cell:komachi/07",
    "name": "Komachi Convenience", "nameJa": "小町コンビニ",
    "kind": "convenience_store",
    "hours": {"open": 6, "close": 23, "closedDays": []},
    "interactions": ["ENTER", "EXAMINE", "PHOTOGRAPH"],
    "interiorRef": "interior:shop-14",
    "knowledge": ["lang:vocab/コンビニ", "lang:vocab/いらっしゃいませ"],
    "quests": ["quest:errand-01"],
    "renderRef": "assets/props/shop-14.glb"
  },
  {
    "id": "object:onigiri-shelf",
    "nodeType": "interaction_node",
    "parentId": "interior:shop-14",
    "name": "Rice ball shelf", "nameJa": "おにぎりコーナー",
    "description": "Fresh onigiri in the chilled section.",
    "interactions": ["EXAMINE", "READ", "PHOTOGRAPH", "PICK_UP"],
    "knowledge": ["lang:vocab/おにぎり", "lang:kanji/食", "lang:vocab/鮭"],
    "photography": {"collectible": true, "collectionRef": "collection:kamakura-food"},
    "quests": ["quest:errand-01"],
    "renderRef": "assets/props/onigiri-shelf.glb"
  },
  {
    "id": "object:vending-03",
    "nodeType": "interaction_node",
    "parentId": "cell:komachi/07",
    "name": "Vending machine", "nameJa": "自動販売機",
    "interactions": ["EXAMINE", "BUY", "PHOTOGRAPH"],
    "knowledge": ["lang:vocab/自動販売機", "lang:vocab/お茶", "lang:grammar/numbers"],
    "photography": {"collectible": true, "collectionRef": "collection:kamakura-street"},
    "renderRef": "assets/props/vending-03.glb"
  },
  {
    "id": "object:sign-komachi",
    "nodeType": "interaction_node",
    "parentId": "cell:komachi/07",
    "name": "Street sign", "nameJa": "小町通り",
    "interactions": ["READ", "PHOTOGRAPH"],
    "inlineText": {"ja": "小町通り", "furigana": "こまちどおり", "en": "Komachi Street"},
    "knowledge": ["lang:vocab/通り"],
    "renderRef": "assets/props/sign-komachi.glb"
  },
  {
    "id": "object:station-kamakura",
    "nodeType": "station",
    "parentId": "cell:komachi/07",
    "name": "Kamakura Station", "nameJa": "鎌倉駅",
    "lines": ["line:enoden"],
    "platforms": [1, 2],
    "interactions": ["EXAMINE", "READ", "BOARD", "PHOTOGRAPH"],
    "knowledge": ["lang:vocab/駅", "lang:kanji/駅", "lang:vocab/切符"],
    "timetableRef": "data/enoden-timetable.json",
    "quests": ["quest:train-ride-01"]
  },
  {
    "id": "object:beach-01",
    "nodeType": "beach",
    "parentId": "cell:yuigahama/01",
    "name": "Yuigahama Beach", "nameJa": "由比ヶ浜",
    "interactions": ["WALK", "SIT", "SWIM", "PHOTOGRAPH"],
    "knowledge": ["lang:vocab/海", "lang:vocab/波", "lang:vocab/泳ぐ"],
    "quests": ["quest:beach-photo-01"]
  },
  {
    "id": "object:seaside-shop",
    "nodeType": "shop",
    "parentId": "cell:yuigahama/01",
    "name": "Seaside stand", "nameJa": "海の家",
    "kind": "seasonal_stand",
    "seasonal": ["summer"],
    "interactions": ["ENTER", "BUY", "EAT"],
    "knowledge": ["lang:vocab/海の家", "lang:vocab/かき氷"],
    "renderRef": "assets/props/seaside-shop.glb"
  }
]
```

## 5. Interior

```json
{
  "id": "interior:shop-14",
  "nodeType": "interior",
  "parentId": "object:shop-14",
  "name": "Komachi Convenience interior", "nameJa": "小町コンビニ店内",
  "layoutRef": "assets/interiors/shop-14.glb",
  "lightingRef": "assets/lighting/shop-14.json",
  "objects": ["object:onigiri-shelf", "object:register-14", "object:drink-cooler"]
}
```

## 6. NPC — Tanaka the shopkeeper (§98) with full deterministic schedule

```json
{
  "id": "npc:tanaka",
  "nodeType": "npc",
  "identity": {
    "name": "田中", "nameRomaji": "Tanaka", "occupation": "shopkeeper",
    "ageCategory": "adult", "appearanceRef": "assets/npc/tanaka.glb",
    "personality": "friendly", "dialect": "standard"
  },
  "homeCell": "cell:komachi/07",
  "scheduleRef": "schedule:tanaka",
  "relationships": {"player": 0},
  "dialogueRefs": ["dlg:tanaka-greeting", "dlg:tanaka-errand", "dlg:tanaka-farewell"],
  "knowledge": ["lang:vocab/いらっしゃいませ", "lang:vocab/ありがとう"],
  "quests": ["quest:errand-01"],
  "activities": ["WORK", "EAT", "HOME"],
  "renderRef": "assets/npc/tanaka.glb"
}
```

Schedule (`schedule:tanaka`) — deterministic by construction (§98):

```json
{
  "id": "schedule:tanaka",
  "slots": [
    {"timeOfDay": "morning", "weekday": "*", "season": "*", "weather": "*",
     "locationRef": "interior:shop-14", "activity": "WORK", "dialogVariant": "working"},
    {"timeOfDay": "afternoon", "weekday": "*", "season": "*", "weather": "*",
     "locationRef": "interior:shop-14", "activity": "WORK", "dialogVariant": "working"},
    {"timeOfDay": "evening", "weekday": "mon-fri", "season": "*", "weather": "*",
     "locationRef": "interior:shop-14", "activity": "WORK", "dialogVariant": "closing"},
    {"timeOfDay": "evening", "weekday": "sat-sun", "season": "*", "weather": "*",
     "locationRef": "cell:komachi/07", "activity": "WALK", "dialogVariant": "off-duty"},
    {"timeOfDay": "night", "weekday": "*", "season": "*", "weather": "*",
     "locationRef": "home:tanaka", "activity": "HOME", "dialogVariant": "home"}
  ]
}
```

## 7. Dialogue (§99) with difficulty variants (§113)

```json
{
  "id": "dlg:tanaka-greeting",
  "speaker": "npc:tanaka",
  "variants": {
    "BEGINNER": {
      "lines": [
        {"ja": "いらっしゃいませ！", "en": "Welcome!", "furigana": "いらっしゃいませ",
         "voiceRef": "audio/tts/irasshaimase.ogg", "emotion": "happy",
         "knowledge": ["lang:vocab/いらっしゃいませ"]},
        {"ja": "何かお探しですか？", "en": "Are you looking for something?",
         "furigana": "なにか おさがし ですか",
         "knowledge": ["lang:vocab/探す"]}
      ],
      "choices": [
        {"id": "c1", "textJa": "おにぎりを探しています。", "textEn": "I'm looking for onigiri.",
         "conditions": [], "effects": [{"quest": "quest:errand-01", "objective": 0, "op": "hint"}],
         "knowledge": ["lang:vocab/おにぎり"]},
        {"id": "c2", "textJa": "ありがとうございます。", "textEn": "Thank you.",
         "conditions": [], "effects": []}
      ]
    },
    "INTERMEDIATE": {
      "lines": [
        {"ja": "いらっしゃいませ。今日は何をお探しですか。", "en": "Welcome. What are you looking for today?",
         "furigana": "いらっしゃいませ。きょうは なにを おさがしですか。",
         "knowledge": ["lang:vocab/今日", "lang:grammar/〜を探す"]},
        {"ja": "新しく鮭のおにぎりが入りましたよ。", "en": "We just got new salmon onigiri.",
         "knowledge": ["lang:vocab/鮭", "lang:vocab/新しい"]}
      ],
      "choices": [
        {"id": "c1", "textJa": "鮭のおにぎりを二つください。", "textEn": "Two salmon onigiri, please.",
         "conditions": [], "effects": [{"quest": "quest:errand-01", "objective": 1, "op": "complete"}],
         "knowledge": ["lang:vocab/二つ", "lang:grammar/counters"]}
      ]
    },
    "ADVANCED": {
      "lines": [
        {"ja": "あら、いらっしゃいませ。今日は何をお探しですか？", "en": "Oh, welcome. What are you looking for today?",
         "knowledge": ["lang:grammar/あら"]},
        {"ja": "うちの鮭おにぎりは評判でしてね、よかったらどうぞ。", "en": "Our salmon onigiri are quite popular — please, help yourself.",
         "knowledge": ["lang:vocab/評判", "lang:grammar/〜てね"]}
      ],
      "choices": [
        {"id": "c1", "textJa": "評判なら、二ついただきます。", "textEn": "If they're that popular, I'll take two.",
         "effects": [{"quest": "quest:errand-01", "objective": 1, "op": "complete"}],
         "knowledge": ["lang:vocab/評判"]}
      ]
    }
  }
}
```

## 8. Quest — the errand (§100–§102)

```json
{
  "id": "quest:errand-01",
  "kind": "STORY",
  "level": "BEGINNER",
  "title": "A small errand", "titleJa": "おつかい",
  "giverNpc": "npc:tanaka",
  "objectives": [
    {"id": 0, "type": "INTERACT", "target": "object:onigiri-shelf", "conditionRef": "interaction:EXAMINE", "order": 1,
     "hint": "Look at the onigiri shelf.", "knowledge": ["lang:vocab/おにぎり"]},
    {"id": 1, "type": "COLLECT", "target": "item:onigiri-salmon", "order": 2,
     "hint": "Collect a salmon onigiri.", "knowledge": ["lang:vocab/鮭"]},
    {"id": 2, "type": "TALK", "target": "npc:tanaka", "dialogueRef": "dlg:tanaka-errand", "order": 3,
     "hint": "Return to Tanaka.", "knowledge": ["lang:vocab/ありがとう"]}
  ],
  "rewards": [
    {"kind": "discovery", "ref": "discovery:komachi-backstreet"},
    {"kind": "collection_entry", "ref": "collection:kamakura-food/onigiri"}
  ],
  "knowledge": ["lang:vocab/おにぎり", "lang:vocab/鮭", "lang:grammar/〜をください"],
  "storyConsequences": [{"story": "story:summer-day", "beat": "errand-complete"}],
  "conditions": [{"worldTime": {"after": "morning"}}]
}
```

## 9. Story — a summer day in Kamakura (§102)

```json
{
  "id": "story:summer-day",
  "title": "A summer day in Kamakura", "titleJa": "鎌倉の夏の一日",
  "chapters": [
    {
      "id": "chapter:1",
      "beats": [
        {"id": "beat:arrive", "sceneRef": "scene:kamakura-station", "dialogueRefs": [],
         "interactions": ["READ"], "requires": [], "follows": null,
         "knowledge": ["lang:vocab/駅"]},
        {"id": "beat:errand", "sceneRef": "scene:komachi-street", "dialogueRefs": ["dlg:tanaka-greeting"],
         "interactions": ["TALK", "EXAMINE", "PICK_UP"],
         "requires": ["quest:errand-01"], "follows": "beat:arrive",
         "knowledge": ["lang:vocab/おにぎり", "lang:vocab/鮭"]},
        {"id": "beat:train", "sceneRef": "scene:enoden", "dialogueRefs": [],
         "interactions": ["BOARD", "LISTEN"],
         "requires": [], "follows": "beat:errand",
         "knowledge": ["lang:vocab/江ノ電", "lang:vocab/次は"]},
        {"id": "beat:beach", "sceneRef": "scene:yuigahama", "dialogueRefs": ["dlg:lifeguard"],
         "interactions": ["WALK", "SWIM", "PHOTOGRAPH"],
         "requires": [], "follows": "beat:train",
         "knowledge": ["lang:vocab/海", "lang:vocab/波", "lang:vocab/泳ぐ"]},
        {"id": "beat:home", "sceneRef": "scene:kamakura-station", "dialogueRefs": [],
         "interactions": [], "requires": [], "follows": "beat:beach",
         "knowledge": ["lang:vocab/帰る"]}
      ]
    }
  ]
}
```

## 10. Train line — Enoden (data-driven route simulation, §104)

```json
{
  "id": "line:enoden",
  "name": "江ノ島電鉄", "nameEn": "Enoshima Electric Railway",
  "stations": ["station:kamakura", "station:hase", "station:yuigahama", "station:enoshima"],
  "vehicles": [
    {"id": "train:enoden-01", "model": "300形", "capacity": 120,
     "route": ["station:kamakura", "station:hase", "station:yuigahama", "station:enoshima"]}
  ],
  "timetable": [
    {"departure": "07:05", "from": "station:kamakura", "to": "station:enoshima", "stops": 3, "vehicle": "train:enoden-01"},
    {"departure": "07:35", "from": "station:kamakura", "to": "station:enoshima", "stops": 3, "vehicle": "train:enoden-01"}
  ],
  "announcements": [
    {"ja": "次は、長谷。", "en": "Next stop: Hase.", "station": "station:hase", "type": "next_stop"},
    {"ja": "まもなく、江の島。", "en": "Arriving at Enoshima.", "station": "station:enoshima", "type": "arrival"}
  ],
  "knowledge": ["lang:vocab/江ノ電", "lang:vocab/次は", "lang:vocab/まもなく", "lang:grammar/time-expressions"]
}
```

## 11. Collections & discoveries (§110–§111)

```json
[
  {
    "id": "collection:kamakura-food",
    "title": "Kamakura food", "titleJa": "鎌倉の食べ物",
    "kind": "food",
    "members": ["item:onigiri", "item:kakigori", "item:kamakura-vegetables"]
  },
  {
    "id": "collection:kamakura-street",
    "title": "Kamakura street", "titleJa": "鎌倉の町",
    "kind": "objects",
    "members": ["object:vending-03", "object:sign-komachi"]
  },
  {
    "id": "discovery:komachi-backstreet",
    "kind": "location",
    "nodeRef": "cell:komachi/08",
    "foundAt": "2026-07-25T15:00:00Z",
    "source": "quest-reward",
    "questRef": "quest:errand-01"
  }
]
```

## 12. Difficulty mapping (§113) — same geometry, three depths

| Content | BEGINNER | INTERMEDIATE | ADVANCED |
|---|---|---|---|
| Sign 小町通り | gloss shown, furigana always | furigana on demand | no furigana by default |
| Dialogue variants | short sentences, full furigana | natural sentences, furigana on demand | natural conversation, no furigana |
| Quest text | plain + hint | plain | idiomatic |
| Glossary depth | word + gloss + picture | word + gloss + example | word + gloss + example + nuance |
| Knowledge density overlay | highlights words ≤N3 | highlights words ≤N2 | highlights all unknown |

## 13. Acceptance criteria for this content (the §91 proof)

1. Every JSON object here validates against JOURNEY_WORLD_SCHEMA + §148 gates with zero
   engine changes.
2. The onigiri loop (§87) runs e2e: enter shop → examine shelf → glossary (おにぎり +
   食) → photo → discovery → optional card → quest objective 1 → collect → talk →
   quest complete → story beat "errand-complete".
3. The Enoden ride plays: board → announcement 次は長谷 → arrival → knowledge recorded.
4. Tanaka appears at the register in the morning and is absent at night (deterministic
   schedule), and his dialogue depth matches the player's level.
5. All strings pass localization validation (ja + en complete) and license metadata is
   present (§260).
