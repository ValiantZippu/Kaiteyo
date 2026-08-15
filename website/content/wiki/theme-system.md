---
title: How theming works
description: The theme architecture — base modes, accent schemes, the Theme Studio, and how a theme is stored.
---

Kaiteyo's theming isn't a light/dark toggle with a tint — it's a small design system with base modes, accent schemes, and an optional full editor.

## Base modes

Four base modes define the surface hierarchy (background, surface, elevated, interactive, borders, text colors):

- **OLED Black** — true black (`#050505`) so pixels turn off
- **Dark Gray** — softer dark
- **Light** — clean light
- **Sepia** — warm paper tones for reading

Each mode keeps the same structure, so switching modes never breaks the UI.

## Accent schemes

Seven signature accents (signature lime+orange, cotton candy, ocean, forest, sunset, lavender, monochrome) define primary/secondary/tertiary colors plus glows and gradients. The signature is **lime `#C2FC8B`** with **orange `#FEAB57`**.

## The Theme Studio (desktop)

A full editor for everything beyond the presets:

- **Color Studio** — wheels (RGB/HSV/HSL/HEX), opacity, gradient editor, live preview
- **Motion Studio** — animation presets (none → bouncy), speed, spring tuning
- **Layout Studio** — density modes, corner radius, transparency/blur, surface elevation

Themes export to and import from JSON. The theme format is documented in `docs/features/THEMES.md`.

## Architecture

A `ThemeManager` holds the live state: base mode, accent scheme, glow config, radius config, animation config, and density config. Compose `CompositionLocal`s (`LocalKaiteyoAccent`, `LocalSurfaceColors`, …) propagate it to every screen — which is why a theme change is instant and app-wide.

## How the website mirrors it

The website's theme engine maps 1:1 to the app's tokens, and your theme preference persists in your browser the same way the app persists yours. The [Theme Gallery](/theme-gallery/) demonstrates every combination live.
