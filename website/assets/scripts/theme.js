/**
 * Kaiteyo website — theme engine.
 *
 * Mirrors the application theme engine
 * (core/.../presentation/common/theme/Theme.kt):
 *   - Base modes: oled, dark, light, sepia
 *   - Accent schemes: signature, cotton-candy, ocean, forest, sunset,
 *     lavender, monochrome
 *   - Glass overlay (transparency + blur)
 *
 * Theme state is applied to <html data-base-mode data-accent data-glass>
 * and driven entirely by CSS variables (styles/themes.css).
 */

import { getPreference, setPreference } from "./persistence.js";
import { normalize } from "./utils.js";

const THEME_KEY = "theme";
const BASE_MODES = ["oled", "dark", "light", "sepia"];
const ACCENTS = [
  "signature",
  "cotton-candy",
  "ocean",
  "forest",
  "sunset",
  "lavender",
  "monochrome",
];

function systemBaseMode() {
  return window.matchMedia("(prefers-color-scheme: light)").matches
    ? "light"
    : "oled";
}

function loadInitialState() {
  const saved = getPreference(THEME_KEY, null);
  if (saved && saved.baseMode) {
    return {
      baseMode: BASE_MODES.includes(saved.baseMode) ? saved.baseMode : "oled",
      accent: ACCENTS.includes(saved.accent) ? saved.accent : "signature",
      glass: Boolean(saved.glass),
    };
  }
  return {
    baseMode: systemBaseMode(),
    accent: "signature",
    glass: false,
  };
}

class ThemeEngine {
  constructor() {
    this.state = loadInitialState();
    this.motion = getPreference("motion", "default");
  }

  /** Write the current state to the DOM. */
  apply() {
    const root = document.documentElement;
    root.dataset.baseMode = this.state.baseMode;
    root.dataset.accent = this.state.accent;
    root.dataset.glass = this.state.glass ? "true" : "false";
    document.body.classList.toggle("reduce-motion", this.motion === "reduced");
    document.dispatchEvent(new CustomEvent("kaiteyo:themechange", { detail: { ...this.state } }));
  }

  persist() {
    setPreference(THEME_KEY, this.state);
  }

  setBaseMode(id) {
    if (!BASE_MODES.includes(id)) return;
    document.body.classList.add("theme-switching");
    window.setTimeout(() => document.body.classList.remove("theme-switching"), 200);
    this.state.baseMode = id;
    this.apply();
    this.persist();
  }

  setAccent(id) {
    if (!ACCENTS.includes(id)) return;
    this.state.accent = id;
    this.apply();
    this.persist();
  }

  setGlass(enabled) {
    this.state.glass = Boolean(enabled);
    this.apply();
    this.persist();
  }

  setMotion(preference) {
    this.motion = normalize(preference) === "reduced" ? "reduced" : "default";
    this.apply();
    setPreference("motion", this.motion);
  }

  reset() {
    this.state = { baseMode: "oled", accent: "signature", glass: false };
    this.motion = "default";
    this.apply();
    this.persist();
    setPreference("motion", "default");
  }

  getState() {
    return { ...this.state, motion: this.motion };
  }
}

export const theme = new ThemeEngine();
