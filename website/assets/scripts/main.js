/**
 * Kaiteyo website — bootstrap.
 *
 * Initializes the theme engine, navigation engine, interactive
 * components, and the command palette, then wires the topbar.
 */

import { theme } from "./theme.js";
import { navigation } from "./navigation.js";
import { initComponents } from "./components.js";
import { initSearch } from "./search.js";

const BASE_PATH =
  document.documentElement.dataset.basePath ?? "/";

function initTopbar() {
  document.querySelectorAll("[data-open-search]").forEach((button) => {
    button.addEventListener("click", () =>
      document.dispatchEvent(new CustomEvent("kaiteyo:open-search")),
    );
  });

  document.querySelectorAll("[data-open-theme]").forEach((button) => {
    button.addEventListener("click", () => {
      const dialog = document.getElementById("themeDialog");
      if (dialog) {
        dialog.showModal();
      }
    });
  });

  document.querySelectorAll("[data-open-nav-settings]").forEach((button) => {
    button.addEventListener("click", () => {
      const dialog = document.getElementById("navSettingsDialog");
      if (dialog) {
        dialog.showModal();
      }
    });
  });

  /* Theme dialog controls */
  const themeDialog = document.getElementById("themeDialog");
  if (themeDialog) {
    const baseButtons = themeDialog.querySelectorAll("[data-theme-base]");
    const accentButtons = themeDialog.querySelectorAll("[data-theme-accent]");
    const glassToggle = themeDialog.querySelector("[data-theme-glass]");
    const motionToggle = themeDialog.querySelector("[data-theme-motion]");
    const resetButton = themeDialog.querySelector("[data-theme-reset]");

    const sync = () => {
      const state = theme.getState();
      baseButtons.forEach((button) => {
        button.setAttribute(
          "aria-pressed",
          String(button.dataset.themeBase === state.baseMode),
        );
      });
      accentButtons.forEach((button) => {
        button.setAttribute(
          "aria-pressed",
          String(button.dataset.themeAccent === state.accent),
        );
      });
      if (glassToggle) glassToggle.checked = state.glass;
      if (motionToggle) motionToggle.checked = state.motion === "reduced";
    };

    baseButtons.forEach((button) => {
      button.addEventListener("click", () => {
        theme.setBaseMode(button.dataset.themeBase);
        sync();
      });
    });

    accentButtons.forEach((button) => {
      button.addEventListener("click", () => {
        theme.setAccent(button.dataset.themeAccent);
        sync();
      });
    });

    glassToggle?.addEventListener("change", () => {
      theme.setGlass(glassToggle.checked);
    });

    motionToggle?.addEventListener("change", () => {
      theme.setMotion(motionToggle.checked ? "reduced" : "default");
    });

    resetButton?.addEventListener("click", () => {
      theme.reset();
      sync();
    });

    themeDialog.addEventListener("close", sync);
    sync();
  }

  /* Navigation settings dialog controls */
  const navDialog = document.getElementById("navSettingsDialog");
  if (navDialog) {
    const positionButtons = navDialog.querySelectorAll("[data-nav-pos]");
    const modeButtons = navDialog.querySelectorAll("[data-nav-mode]");
    const widthButtons = navDialog.querySelectorAll("[data-nav-width]");
    const collapseToggle = navDialog.querySelector("[data-nav-collapse]");
    const autoHideToggle = navDialog.querySelector("[data-nav-autohide-toggle]");
    const resetButton = navDialog.querySelector("[data-nav-reset]");

    const sync = () => {
      const state = navigation.getState();
      positionButtons.forEach((button) => {
        button.setAttribute(
          "aria-pressed",
          String(button.dataset.navPos === state.position),
        );
      });
      modeButtons.forEach((button) => {
        button.setAttribute(
          "aria-pressed",
          String(button.dataset.navMode === state.mode),
        );
      });
      widthButtons.forEach((button) => {
        button.setAttribute(
          "aria-pressed",
          String(button.dataset.navWidth === state.width),
        );
      });
      if (collapseToggle) collapseToggle.checked = state.collapsed;
      if (autoHideToggle) autoHideToggle.checked = state.autoHide;
    };

    positionButtons.forEach((button) => {
      button.addEventListener("click", () => {
        navigation.setPosition(button.dataset.navPos);
        sync();
      });
    });

    modeButtons.forEach((button) => {
      button.addEventListener("click", () => {
        navigation.setMode(button.dataset.navMode);
        sync();
      });
    });

    widthButtons.forEach((button) => {
      button.addEventListener("click", () => {
        navigation.setWidth(button.dataset.navWidth);
        sync();
      });
    });

    collapseToggle?.addEventListener("change", () => {
      navigation.setCollapsed(collapseToggle.checked);
    });

    autoHideToggle?.addEventListener("change", () => {
      navigation.setAutoHide(autoHideToggle.checked);
    });

    resetButton?.addEventListener("click", () => {
      navigation.reset();
      sync();
    });

    navDialog.addEventListener("close", sync);
    sync();
  }

  /* Collapse toggle in the nav panel footer. */
  document.querySelectorAll("[data-nav-collapse-toggle]").forEach((button) => {
    button.addEventListener("click", () => navigation.toggleCollapsed());
  });

  /* Mobile menu button. */
  document.querySelectorAll("[data-nav-open]").forEach((button) => {
    button.addEventListener("click", () => document.body.classList.add("nav-open"));
  });
}

function markActiveNavigation() {
  const path = window.location.pathname;
  document.querySelectorAll(".nav-item").forEach((item) => {
    const url = item.getAttribute("href");
    const match =
      path === url ||
      (url !== "/" && path.startsWith(url)) ||
      (path.startsWith("/docs/") && url === "/docs/") ||
      (path.startsWith("/wiki/") && url === "/wiki/");
    item.toggleAttribute("aria-current", match ? "page" : "");
    item.setAttribute("aria-current", match ? "page" : "false");
  });
}

function init() {
  document.documentElement.dataset.basePath = BASE_PATH;

  /* Apply persisted preferences before first paint. */
  theme.apply();
  navigation.apply();
  navigation.initDrag();
  navigation.initAutoHide();
  navigation.initDrawer();

  initComponents();
  initTopbar();
  initSearch(BASE_PATH);
  markActiveNavigation();

  /* Respect system reduced-motion changes at runtime. */
  const motionQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
  motionQuery.addEventListener?.("change", () => {
    const state = theme.getState();
    if (state.motion === "default" && motionQuery.matches) {
      document.body.classList.add("reduce-motion");
    }
  });

  /* Respect system theme changes only when nothing is persisted. */
  if (!window.localStorage.getItem("kaiteyo:theme")) {
    const colorQuery = window.matchMedia("(prefers-color-scheme: light)");
    colorQuery.addEventListener?.("change", () => {
      if (!window.localStorage.getItem("kaiteyo:theme")) {
        theme.setBaseMode(colorQuery.matches ? "light" : "oled");
      }
    });
  }
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", init);
} else {
  init();
}
