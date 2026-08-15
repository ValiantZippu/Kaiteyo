# Getting Started

## 1. Install Kaiteyo

### Desktop (Windows / macOS / Linux)

Grab the package for your OS from the [GitHub Releases](https://github.com/ValiantZippu/Kaiteyo/releases):

| OS | Package | Notes |
|---|---|---|
| Windows | `kaiteyo-<ver>-windows-setup.exe` | Branded Inno Setup wizard; also `.msi` and portable `.zip` |
| macOS | `kaiteyo-<ver>-macos-arm64.dmg` / `-x64.dmg` | Signed + notarized; drag to Applications |
| Linux | `.AppImage`, `.deb`, `.rpm` | Flatpak/Snap manifests also in-tree |

On Windows, the installer offers install/upgrade/repair/modify, optional shortcuts
and file associations, and preserves your data on uninstall. On macOS, drag the
`.app` into Applications. On Linux, install via your package manager or run the
AppImage directly.

> The desktop app checks for updates through the built-in update system (stable
> channel by default) when enabled — Settings → Updates.

### Android

- **Google Play:** [Kaiteyo on Google Play](https://play.google.com/store/apps/details?id=ua.syt0r.kanji) (adds analytics, billing, review)
- **F-Droid:** [Kaiteyo on F-Droid](https://f-droid.org/en/packages/ua.syt0r.kanji.fdroid/) (no Google services)

### iOS

- **App Store:** [Kaiteyo on the App Store](https://apps.apple.com/ua/app/kanji-dojo/id6745169386)

## 2. First launch

**Desktop** — the first launch shows the **onboarding wizard** (`OnboardingWizard`,
8 steps): Welcome → **Theme** (OLED/Dark/Light/Sepia) → **Accent** (7 accent
schemes) → **UI scaling** (80–160%) → **Font size** (Small…Extra large) →
**Navigation** (dock position + mode) → **Motion** (Off/Minimal/Balanced/Smooth/
Cinematic) → Finish. Every step has a live preview and can be skipped; reopen it
any time from **Settings → General → "Show onboarding again"**.

**Mobile** — launch straight into the home screen.

## 3. The home screen

The home screen is your dashboard and gateway (`HomeScreen` → `HomeScreenTab`s):

- **Home tab** (`GeneralDashboard`) — study stats, review counts, recent activity,
  quick practice entry points, and a tutorial dialog on first use.
- **Library tab** — the unified library hub (`LibraryScreen`):
  - **Sections** with stat summary rows (Stats, Study, Library, Review)
  - Drill-down screens: **Kanji Decks**, **Vocabulary**, **Word & Sentence Search**
  - Deck cards show progress; tap to open deck details.
- **Stats tab** — the unified statistics dashboard (`StatisticsScreen`): heatmap,
  knowledge, retention, sessions/mistakes, goals, exams.
- **Search tab** — word & sentence search (`SearchScreen`).
- **Settings tab** — app settings (`SettingsScreen`).

On desktop, the same destinations are reachable from the workspace dock
(Dashboard, Library, Stats, Settings…) plus the desktop-only immersion views —
see [DESKTOP_SUITE.md](DESKTOP_SUITE.md).

## 4. First steps

1. Pick a deck from the Library (built-in JLPT N5–N1 and school-grade kanji decks,
   kana decks, and vocabulary decks are included; you can create your own).
2. Open the deck and choose a study mode:
   - **Reading** — see the character/word, its reading, meaning, and furigana
   - **Writing** — practice drawing with the brush canvas and stroke-order guides
   - **Flashcards** (vocabulary) — front/back cards; also **Reading picker** and
     **Writing** modes
3. Review what you learned — cards are scheduled by the spaced-repetition system
   (FSRS-5).

See [STUDYING.md](STUDYING.md) for the full study workflow.

## 5. Desktop-only setup (optional)

The desktop suite adds the immersion workspace — dictionaries, media, mining, OCR,
and a browser. See [DESKTOP_SUITE.md](DESKTOP_SUITE.md). If you're coming from
Anki, see `../integrations/ANKI.md` for importing your `.apkg` collection.

## Troubleshooting

- Can't launch the app? Check `docs/troubleshooting/` (platform-specific sections).
- Data lives on your device — back it up from **Settings** (backup/restore; see
  `../data/ARCHITECTURE.md` for where files live). Desktop backups include settings
  and window state.
