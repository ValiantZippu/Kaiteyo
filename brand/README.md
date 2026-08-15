# 🎨 brand — Kaiteyo Brand Assets (the drop folder)

**This is THE place to put Kaiteyo branding assets.**

Everything user-visible about the Kaiteyo identity flows from `brand/source/`.
Drop a logo, an icon, a banner or an illustration here, run the sync pipeline,
and the application, website, installer and docs all pick it up through the
semantic `BrandAssets` API. **No more hunting through `core/…`, `desktopApp/…`,
random drawable folders or SVG folders.**

> The repository is the source of truth. The *source* files you drop here are
> never modified. Validated copies land in `brand/processed/`, generated
> binaries land in `brand/generated/`, and the app consumes *those* — never the
> originals.

---

## 1. Where to put what

```
brand/
├── source/                 ← YOU drop assets here (source of truth)
│   ├── logos/              ← app-logo.svg, app-logo-light/dark.svg, app-logo-wide/compact.svg
│   ├── marks/              ← app-mark.svg (compact symbol, transparent bg)
│   ├── app-icons/          ← app-icon.svg (1024×1024 square master)
│   ├── banners/            ← home-banner.svg (wide horizontal)
│   ├── backgrounds/        ← splash-background.svg, og-background.svg
│   ├── illustrations/      ← empty-state-*.svg
│   ├── favicons/           ← favicon.svg
│   ├── promotional/        ← social-preview.svg, og-cover.svg
│   └── fonts/              ← licensed fonts (ttf/otf) + LICENSE-*.txt
├── processed/              ← validated copies, ready to sync (generated)
├── generated/              ← raster/binary outputs: png, ico, icns (generated)
├── manifests/
│   └── assets.json         ← machine-readable role → file manifest
└── scripts/
    ├── validate-assets.sh  ← fail-fast validation against the manifest
    ├── sync-assets.sh      ← copy processed → application resources
    └── render-icons.mjs    ← dependency-free SVG → PNG/ICO/ICNS rasterizer
```

| Asset role | File | Purpose | Recommended dimensions | Formats |
|---|---|---|---|---|
| App logo | `source/logos/app-logo.svg` | README, website header, About page, splash | ≥ 512×512 | SVG (preferred), PNG |
| App logo (light) | `source/logos/app-logo-light.svg` | Light/OLED surfaces | ≥ 512×512 | SVG |
| App logo (dark) | `source/logos/app-logo-dark.svg` | Dark surfaces | ≥ 512×512 | SVG |
| App logo (wide) | `source/logos/app-logo-wide.svg` | Wide surfaces (website, installer banner) | 1200×630 or wider | SVG, PNG, WebP |
| App logo (compact) | `source/logos/app-logo-compact.svg` | Small spaces, narrow layouts | 400×120+ | SVG |
| App mark | `source/marks/app-mark.svg` | Compact symbol: nav rails, title bar, chips | 32×32 (vector) | SVG |
| App icon | `source/app-icons/app-icon.svg` | Launcher/window/installer icon master | 1024×1024 square | SVG master |
| Home banner | `source/banners/home-banner.svg` | Dashboard / home hero | 1280×640 | SVG, PNG, WebP |
| Favicon | `source/favicons/favicon.svg` | Website, browser tab | 64×64+ (vector) | SVG, ICO, PNG |
| Social preview | `source/promotional/social-preview.svg` | GitHub/OG/Twitter card | 1200×630 | SVG, PNG |
| Empty states | `source/illustrations/empty-state-*.svg` | Library/media/settings empty states | 480×360 | SVG |
| Fonts | `source/fonts/` | Brand typefaces (must include license) | — | TTF/OTF/WOFF2 |

## 2. Naming convention

- `kebab-case`, all lowercase: `app-logo.svg`, `app-mark-dark.svg`, `home-banner.webp`.
- Variant suffixes come **last**: `-light`, `-dark`, `-monochrome`, `-compact`, `-wide`, `-square`.
- **Never** ship `logo-final.svg`, `logo-final2.svg`, `newlogo.svg`, `K-new.svg` etc.
  Replace the canonical file instead — the pipeline overwrites from `brand/source/`.

## 3. Light / dark variants

Drop `app-logo-light.svg` / `app-logo-dark.svg` next to `app-logo.svg` and the
`BrandAssets` API picks the right one for the active theme:

- **Light / OLED** surface → `-light` variant, then fall back to the default.
- **Dark** surface → `-dark` variant, then fall back to the default.
- No variant at all → the default asset is used everywhere (never a substitute).

## 4. Supported formats

| Format | Logos | Marks | Icons | Banners | Illustrations |
|---|---|---|---|---|---|
| SVG | ✅ preferred | ✅ | ✅ master | ✅ | ✅ |
| PNG | ✅ | ✅ | ✅ | ✅ | ✅ |
| WebP | — | — | — | ✅ | — |
| JPEG | — | — | — | ⚠️ (photos only) | — |

SVG stays vector everywhere possible (crisp at 100–200 %+ DPI). Raster formats
are for large photographic/rich surfaces, never for the compact mark.

## 5. How assets reach the app (source → validated → copied → resource)

```
brand/source/app-icons/app-icon.svg      ← you drop this
        │  brand/scripts/validate-assets.sh
        ▼  (exists? format? dimensions? naming? duplicates?)
brand/processed/app-icon.svg             ← validated copy (your file untouched)
        │  brand/scripts/sync-assets.sh  +  render-icons.mjs
        ▼  (copy + rasterize png/ico/icns)
application resources                    ← core drawables, desktopApp window icon,
        │                                   Android vectors, installer assets, website
        ▼
BrandAssets.AppIcon                      ← semantic role consumed by UI
```

Replacing `brand/source/app-icons/app-icon.svg` later requires **zero code
changes**: re-run `sync-assets.sh` and every surface updates.

## 6. Adding a new asset role

1. Drop the file in `brand/source/<category>/` with a `kebab-case` name.
2. Add a `"roles"` entry in `brand/manifests/assets.json` (source path,
   destinations, formats, dimensions).
3. If the app should expose it, add a `BrandAssetRole` + `BrandAssets.<Name>`
   property in
   `core/src/commonMain/kotlin/ua/syt0r/kanji/presentation/common/resources/brand/BrandAssets.kt`.
4. Run `bash brand/scripts/validate-assets.sh && bash brand/scripts/sync-assets.sh`.
5. Reference `BrandAssets.<Name>` in UI — never a raw file path.

## 7. Fonts

Licensed Kaiteyo fonts go in `brand/source/fonts/` **with their license file**
(e.g. `LICENSE-OFL.txt`, `LICENSE-SIL.txt`). Bundled fonts are loaded through
the theme/typography system (`AppTheme` → `Typography`), never hardcoded per
screen. Unlicensed/copyrighted fonts must not be committed.

## 8. Rules of the road

1. Never edit `brand/source/*` originals — the pipeline only *reads* them.
2. Never reference `brand/source/...` paths from app code at runtime.
3. Never invent a placeholder logo — if an asset is missing, the neutral
   fallback (text/wordmark) is used until the real asset is supplied.
4. Aspect ratio is preserved everywhere; logos are never stretched or squished.
5. Meaningful branding images carry a `contentDescription`; decorative ones use
   `null` so they stay out of the accessibility tree.

## 9. Pipeline

```bash
# 1. Validate the drop folder against the manifest (fail fast)
bash brand/scripts/validate-assets.sh

# 2. Sync processed assets into application resources + generate binaries
bash brand/scripts/sync-assets.sh

# 3. Regenerate raster outputs (png/ico/icns) — called by sync-assets.sh
node brand/scripts/render-icons.mjs
```

See `docs/branding/BRANDING.md` for the brand identity and
`docs/branding/BRAND_GUIDELINES.md` for usage rules.
