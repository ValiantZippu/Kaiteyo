/**
 * Lightweight smoke test for the Kaiteyo web trial.
 *
 * Runs trial.js under a minimal DOM shim (no jsdom dependency) and
 * exercises the main views + core logic to catch runtime errors.
 *
 * Usage: node scripts/trial-smoke-test.js
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..");

/* ---------------- DOM shim ---------------- */

class FakeNode {
  constructor(tag) {
    this.tagName = (tag || "").toUpperCase();
    this.children = [];
    this.attributes = {};
    this.dataset = {};
    this.style = {};
    this.classList = { add() {}, remove() {}, toggle() {} };
    this.listeners = {};
    this.parentNode = null;
    this.scrollTop = 0;
    this.textContent = "";
    this.innerHTML = "";
    this.hidden = false;
    this._rect = { left: 0, top: 0, width: 420, height: 420 };
  }
  appendChild(child) {
    if (child && child.parentNode !== null) return child;
    if (child && typeof child === "object") {
      child.parentNode = this;
      this.children.push(child);
    }
    return child;
  }
  setAttribute(k, v) {
    this.attributes[k] = String(v);
    if (k.startsWith("data-")) this.dataset[k.slice(5)] = String(v);
    if (k === "aria-selected") this.ariaSelected = String(v);
    if (k === "aria-current") this.ariaCurrent = String(v);
    if (k === "aria-pressed") this.ariaPressed = String(v);
  }
  getAttribute(k) {
    if (k.startsWith("data-")) return this.dataset[k.slice(5)] ?? null;
    return this.attributes[k] ?? null;
  }
  addEventListener(t, f) {
    (this.listeners[t] = this.listeners[t] || []).push(f);
  }
  removeEventListener() {}
  querySelector() { return null; }
  querySelectorAll() { return []; }
  focus() {}
  showModal() {}
  close() {}
  getBoundingClientRect() { return this._rect; }
  setPointerCapture() {}
  scrollIntoView() {}
  getContext() {
    return {
      scale() {}, clearRect() {}, beginPath() {}, moveTo() {}, lineTo() {},
      stroke() {}, fillRect() {}, strokeRect() {}, fillText() {},
      set lineWidth(v) {}, set strokeStyle(v) {}, set fillStyle(v) {},
      set font(v) {}, set textAlign(v) {}, set textBaseline(v) {},
      set globalAlpha(v) {}, set lineCap(v) {}, set lineJoin(v) {},
    };
  }
}

const elements = [];
global.window = {
  KAITEYO_TRIAL: null,
  matchMedia: () => ({ matches: false, addEventListener() {} }),
  addEventListener() {},
  requestAnimationFrame: (f) => setTimeout(f, 0),
  devicePixelRatio: 1,
  location: { search: "", hash: "" },
  localStorage: {
    _s: {},
    getItem(k) { return this._s[k] ?? null; },
    setItem(k, v) { this._s[k] = String(v); },
  },
};
global.document = {
  documentElement: { dataset: { basePath: "/" } },
  readyState: "complete",
  createElement: (t) => { const e = new FakeNode(t); elements.push(e); return e; },
  createTextNode: (t) => String(t),
  createElementNS: (ns, t) => new FakeNode(t),
  addEventListener() {},
  body: new FakeNode("body"),
  querySelector: (sel) => {
    if (sel.startsWith("#")) return document.getElementById(sel.slice(1));
    if (sel === ".trial-nav-item[data-view]") return null;
    return null;
  },
  querySelectorAll: () => [],
  getElementById: (id) => {
    // Provide the containers the trial expects.
    if (id === "trialContent") return new FakeNode("div");
    if (id === "trialTopbarTitle") return new FakeNode("span");
    if (id === "trialSearch") return new FakeNode("input");
    if (id === "trialThemeRow") return new FakeNode("div");
    if (id === "trialDockToggle") return new FakeNode("button");
    if (id === "trialBannerClose") return new FakeNode("button");
    return null;
  },
};

global.Node = class {};

/* ---------------- Load data + trial ---------------- */

const dataSrc = fs.readFileSync(path.join(ROOT, "assets/trial/data.js"), "utf8");
eval(dataSrc); // sets window.KAITEYO_TRIAL

const trialSrc = fs.readFileSync(path.join(ROOT, "assets/trial/trial.js"), "utf8");
eval(trialSrc);

console.log("trial.js executed without throwing");
console.log("trial boot OK (data + DOM shim)");
