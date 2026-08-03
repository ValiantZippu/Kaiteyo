/**
 * Kaiteyo website — navigation engine.
 *
 * Mirrors the application's floating sidebar
 * (core/.../theme/Theme.kt LayoutConfig + DESIGN_SYSTEM §16):
 *
 *   position : left | right | top | bottom | floating
 *   mode     : docked | floating
 *   collapsed: icons-only (SidebarCompactWidth = 72px)
 *   autoHide : hide until the edge is approached
 *   width    : default | compact | wide
 *
 * Every choice is persisted to localStorage (kaiteyo:nav) and restored
 * on the next visit, like the application's DataStore preferences.
 */

import { getPreference, setPreference } from "./persistence.js";

const NAV_KEY = "nav";
const POSITIONS = ["left", "right", "top", "bottom", "floating"];
const WIDTHS = { default: 260, compact: 220, wide: 300 };

function defaults() {
  return {
    position: "left",
    mode: "docked",
    collapsed: false,
    autoHide: false,
    width: "default",
    floating: { x: 24, y: 96 },
  };
}

function loadInitialState() {
  const saved = getPreference(NAV_KEY, null);
  if (!saved) return defaults();
  return {
    position: POSITIONS.includes(saved.position) ? saved.position : "left",
    mode: saved.mode === "floating" ? "floating" : "docked",
    collapsed: Boolean(saved.collapsed),
    autoHide: Boolean(saved.autoHide),
    width: WIDTHS[saved.width] ? saved.width : "default",
    floating: {
      x: Number.isFinite(saved.floating?.x) ? saved.floating.x : 24,
      y: Number.isFinite(saved.floating?.y) ? saved.floating.y : 96,
    },
  };
}

class NavigationEngine {
  constructor() {
    this.state = loadInitialState();
    this.panel = document.getElementById("navPanel");
    this.dragging = false;
  }

  /** Write state to the DOM. */
  apply() {
    const root = document.documentElement;
    const position =
      this.state.mode === "floating" ? "floating" : this.state.position;

    root.dataset.navPosition = position;
    root.dataset.navCollapsed = this.state.collapsed ? "true" : "false";
    root.dataset.navAutoHide = this.state.autoHide ? "true" : "false";
    root.dataset.navMode = this.state.mode;

    document.documentElement.style.setProperty(
      "--sidebar-width",
      `${WIDTHS[this.state.width]}px`,
    );
    document.documentElement.style.setProperty(
      "--floating-x",
      `${this.state.floating.x}px`,
    );
    document.documentElement.style.setProperty(
      "--floating-y",
      `${this.state.floating.y}px`,
    );

    if (this.panel) {
      this.panel.classList.toggle("is-floating", this.state.mode === "floating");
      this.panel.classList.toggle("hidden", false);
    }

    this.panel?.setAttribute("aria-label", "Site navigation");
    document.dispatchEvent(
      new CustomEvent("kaiteyo:navchange", { detail: { ...this.state } }),
    );
  }

  persist() {
    setPreference(NAV_KEY, this.state);
  }

  setPosition(position) {
    if (!POSITIONS.includes(position)) return;
    this.state.position = position;
    if (position !== "floating") this.state.mode = "docked";
    else this.state.mode = "floating";
    this.apply();
    this.persist();
  }

  setMode(mode) {
    if (mode === "floating") {
      this.state.mode = "floating";
    } else {
      this.state.mode = "docked";
      if (this.state.position === "floating") this.state.position = "left";
    }
    this.apply();
    this.persist();
  }

  toggleCollapsed() {
    this.state.collapsed = !this.state.collapsed;
    this.apply();
    this.persist();
  }

  setCollapsed(collapsed) {
    this.state.collapsed = Boolean(collapsed);
    this.apply();
    this.persist();
  }

  setAutoHide(enabled) {
    this.state.autoHide = Boolean(enabled);
    this.apply();
    this.persist();
  }

  setWidth(width) {
    if (!WIDTHS[width]) return;
    this.state.width = width;
    this.apply();
    this.persist();
  }

  hide() {
    this.panel?.classList.add("hidden");
  }

  show() {
    this.panel?.classList.remove("hidden");
  }

  reset() {
    this.state = defaults();
    this.apply();
    this.persist();
  }

  getState() {
    return { ...this.state };
  }

  /** Wire floating-island drag behaviour. */
  initDrag() {
    if (!this.panel) return;
    const handle = this.panel.querySelector(".drag-handle");
    if (!handle) return;

    const clamp = (value, max) => Math.min(Math.max(value, 8), max - 8);

    handle.addEventListener("pointerdown", (event) => {
      if (this.state.mode !== "floating") return;
      this.dragging = true;
      this.panel.classList.add("is-dragging");
      handle.setPointerCapture(event.pointerId);
      const startX = event.clientX;
      const startY = event.clientY;
      const origin = { ...this.state.floating };

      const onMove = (moveEvent) => {
        if (!this.dragging) return;
        const rect = this.panel.getBoundingClientRect();
        this.state.floating = {
          x: clamp(origin.x + (moveEvent.clientX - startX), window.innerWidth - rect.width),
          y: clamp(origin.y + (moveEvent.clientY - startY), window.innerHeight - rect.height),
        };
        this.apply();
      };

      const onUp = () => {
        this.dragging = false;
        this.panel.classList.remove("is-dragging");
        this.persist();
        handle.removeEventListener("pointermove", onMove);
        handle.removeEventListener("pointerup", onUp);
        handle.removeEventListener("pointercancel", onUp);
      };

      handle.addEventListener("pointermove", onMove);
      handle.addEventListener("pointerup", onUp);
      handle.addEventListener("pointercancel", onUp);
    });
  }

  /** Edge-approach reveal for auto-hide mode. */
  initAutoHide() {
    if (!this.panel) return;
    let hideTimer = null;
    const edge = 24;

    window.addEventListener("pointermove", (event) => {
      if (!this.state.autoHide || this.state.mode === "floating") return;
      const nearEdge =
        this.state.position === "left" && event.clientX < edge;
      const rightEdge = this.state.position === "right" &&
        event.clientX > window.innerWidth - edge;
      const topEdge = this.state.position === "top" && event.clientY < edge;
      const bottomEdge = this.state.position === "bottom" &&
        event.clientY > window.innerHeight - edge;

      if (nearEdge || rightEdge || topEdge || bottomEdge) {
        window.clearTimeout(hideTimer);
        this.show();
      } else if (!this.panel.contains(event.target)) {
        window.clearTimeout(hideTimer);
        hideTimer = window.setTimeout(() => this.hide(), 400);
      }
    });

    this.panel.addEventListener("pointerenter", () => {
      window.clearTimeout(hideTimer);
      this.show();
    });

    this.panel.addEventListener("pointerleave", () => {
      if (!this.state.autoHide) return;
      window.clearTimeout(hideTimer);
      hideTimer = window.setTimeout(() => this.hide(), 300);
    });
  }

  /** Mobile drawer toggling. */
  initDrawer() {
    const scrim = document.getElementById("navScrim");
    const openButtons = document.querySelectorAll("[data-nav-open]");
    const closeButtons = document.querySelectorAll("[data-nav-close]");

    const open = () => document.body.classList.add("nav-open");
    const close = () => document.body.classList.remove("nav-open");

    openButtons.forEach((button) => button.addEventListener("click", open));
    closeButtons.forEach((button) => button.addEventListener("click", close));
    scrim?.addEventListener("click", close);
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") close();
    });

    /* Close after navigating to a new page anchor. */
    this.panel?.addEventListener("click", (event) => {
      if (event.target.closest("a")) close();
    });
  }
}

export const navigation = new NavigationEngine();
