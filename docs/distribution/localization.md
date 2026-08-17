# Installer & Distribution Localization

The distribution layer is prepared for localization — installer strings are
**not** hardcoded into layout code. The app itself has its own localization
system (interface-based `Strings` with `EnglishStrings` / `JapaneseStrings`,
selected by locale) — see `docs/architecture/localization.md`. This page covers
the installer/package surfaces.

## Installer strings

| Platform | Mechanism | Status |
|---|---|---|
| Windows (Inno) | Inno's `[Languages]` system + `Messages`/`CustomMessages` | English shipped; Japanese ready to add as a `.isl` |
| Linux (deb/rpm/Flatpak) | AppStream `<name>`/`<summary>` are plain metadata; the installer is the package manager (already localized by the OS) | English metadata; localized `<name>` variants possible in metainfo |
| Android | Store listing localized via Play Console / F-Droid metadata | English; store-localized listings prepared |

Rules:

- Every user-facing installer string lives in a localizable slot (Inno
  `CustomMessages`, not inline text).
- At minimum **English + Japanese** are prepared; adding a language is an
  addition, never a rewrite.
- The installer never mixes languages mid-flow.

## The app phase (onboarding)

The `OnboardingWizard` uses the app's normal Strings system — it is already
localized through the same `resolveString` lookup as the rest of the app, so
"Show onboarding again" and every wizard label follow the app language.

## Package metadata localization

- AppStream metainfo supports `<name xml:lang="ja">` variants — used when the
  distro's software center is in Japanese.
- `.desktop` `Name[ja]` entries can be added the same way.
- The installer brand copy itself (welcome text, buttons) is prepared for
  localization in the same pass.

## Rules

1. No hardcoded user-facing strings in installer scripts/iss files.
2. Japanese and English stay in sync (mirrors the app's dual-Strings rule).
3. Localized metadata must not break the `verify-artifacts.sh`/schema checks
   (metainfo remains valid XML with `xml:lang` attributes).
