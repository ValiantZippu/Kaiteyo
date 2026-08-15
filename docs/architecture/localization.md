# Kaiteyo Architecture — Localization Plan

**Status**: Implemented (EN + JA, interface-based); extensible to more languages
**Owner**: core `presentation/common/resources/string/Strings.kt` +
`EnglishStrings` + `JapaneseStrings`
**Related**: `docs/architecture/content.md` (content vs app localization) ·
`docs/development/CODING_STANDARDS.md`

## 1. System

User-facing strings are **interface-based, not resource files** (§255). The interface is
the contract; implementations are complete or the project does not compile — missing
translations are impossible by construction.

### Core API (`Strings.kt`)
- `interface Strings` — declares every string. Top-level: `appName`, `hiragana`,
  `katakana`, `kunyomi`, `onyomi`, `loading`, practice-type labels
  (`letterPracticeTypeWriting/Reading`, `vocabPracticeTypeFlashcard/ReadingPicker/
  Writing`), review states (`reviewStateDone/Due/New`), then **nested per-screen
  groups**: `home`, `commonDashboard`, `stats`, `search`, `settings`, `dailyLimit`,
  `tutorialDialog`, `alternativeDialog`, `reminderDialog`, `about`, `backup`,
  `feedback`, `account`, `sync`, `credits`, `syncDialog`, `syncSnackbar`, `deckPicker`,
  `deckDetails`, `deckEdit`, `commonPractice`, `letterPractice`, … — each a sub-interface.
- `EnglishStrings` / `JapaneseStrings` — the two implementations.
- `getStrings()` — selects by `Locale.current.language`: `"ja"` → `JapaneseStrings`,
  else `EnglishStrings`.
- `LocalStrings` — `compositionLocalOf<Strings> { EnglishStrings }` (safe default).
- `resolveString { someString }` — composable accessor via `StringResolveScope<T>`.
- Localized values support formatting/parameters where needed (composable scopes).

### How the system is enforced
Adding a string = edit the `Strings` interface **and** both implementations. The
compiler enforces completeness — no runtime key lookup, no missing-key crashes, no
translation files to drift out of sync.

## 2. Rules (§255)

- Never hardcode user-facing strings in screens — every screen consumes
  `resolveString`.
- Adding a string requires interface + `EnglishStrings` + `JapaneseStrings`
  (Definition of Done in `AGENTS.md`).
- Design supports Korean, Chinese, etc. later — a new locale is a new implementation of
  the same interface, not a schema change.
- Keep wording concise (Japanese/English lengths differ; compact layouts must not
  overflow — QA per §303/§302).

## 3. Content vs app localization (§256)

- **Application localization**: the `Strings` system (UI chrome language).
- **Learning content**: Japanese *is the subject* — content is data with its own fields
  (meanings in English, readings, glosses, `vocab_sense_gloss.language`), governed by
  `docs/architecture/language-model.md` + `docs/architecture/content.md`. The two are
  never conflated: a Japanese-UI user is not "learning Japanese UI".
- Dictionary gloss language is data, independent of UI locale.

## 4. Platform notes

- Core strings shared across desktop/Android/iOS (single `commonMain` source) — no
  per-platform string divergence.
- Desktop runs with `-Duser.language=ja -Duser.country=JP` for the Japanese UI locale.
- The suite's desktop-only surfaces (Dictionary popup, Media, Mining, Exams, Settings
  categories) use their own string tables where they aren't core strings — keep new
  suite strings consistent with the core `Strings` contract where shared.

## 5. Tests

- Compile-time completeness is the primary guarantee (the interface enforces it).
- Gap: screenshot/visual checks per locale for compact layouts; string-length assertions
  for tight components.

## 6. Open items

- Runtime locale switching (currently follows `Locale.current` at composition).
- Formatting/pluralization per locale as new languages land.
- Accessibility interplay: large-text modes with long Japanese strings in compact
  layouts.

## 7. World content localization (TARGET — NODE §99, §148, ADR-0015)

- **Application UI** stays on the interface-based `Strings` system (§256 — never confuse
  "Japanese UI" with "Japanese being learned").
- **World content** (signs, dialogue, quests, menus, station announcements) is
  localized *in the content package* with per-locale fields: `ja` (the language being
  learned — always present), `en` (learner translation), plus optional
  `furigana`/`romaji`. Localization completeness is a hard validation gate (§148): a
  package missing `en` for a dialogue line is rejected.
- The same content may ship a *learner-facing* translation and a *UI-locale* display
  variant; the two are separate fields, never conflated.
- Future locales (ko/zh/…) add translation fields per package — no schema change to
  the app (§255 extensibility).
