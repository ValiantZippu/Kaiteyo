/**
 * Kaiteyo website — global search + command palette.
 *
 * The search index is generated at build time
 * (assets/search/index.json) so the runtime stays tiny:
 * no framework, no external search library.
 *
 * Open with Ctrl+K / Cmd+K, or "/" outside an input.
 * Navigate with arrows, open with Enter, close with Esc.
 */

import { escapeHtml, normalize } from "./utils.js";

const TYPE_LABELS = {
  page: "Page",
  doc: "Documentation",
  wiki: "Wiki",
  guide: "Guide",
  faq: "FAQ",
  shortcut: "Shortcut",
  roadmap: "Roadmap",
  changelog: "Changelog",
  feature: "Feature",
};

class CommandPalette {
  constructor(basePath) {
    this.basePath = basePath;
    this.dialog = document.getElementById("commandPalette");
    this.input = this.dialog?.querySelector("input");
    this.results = this.dialog?.querySelector(".palette-results");
    this.empty = this.dialog?.querySelector(".palette-empty");
    this.index = [];
    this.selected = -1;
  }

  async loadIndex() {
    try {
      const response = await fetch(`${this.basePath}assets/search/index.json`, {
        cache: "force-cache",
      });
      if (!response.ok) throw new Error(`Search index unavailable (${response.status})`);
      this.index = await response.json();
    } catch {
      this.index = [];
    }
  }

  open() {
    if (!this.dialog) return;
    this.selected = -1;
    this.input.value = "";
    this.render([]);
    this.dialog.showModal();
    window.setTimeout(() => this.input.focus(), 0);
  }

  close() {
    this.dialog?.close();
  }

  isOpen() {
    return this.dialog?.open ?? false;
  }

  /** Score an entry against the query. */
  score(entry, tokens) {
    const title = normalize(entry.title);
    const excerpt = normalize(entry.excerpt ?? "");
    const section = normalize(entry.section ?? "");
    const haystack = normalize([title, section, excerpt, entry.url].join(" "));
    let score = 0;

    for (const token of tokens) {
      if (!token) continue;
      if (title.startsWith(token)) score += 40;
      else if (title.includes(token)) score += 24;
      else if (section.includes(token)) score += 10;
      else if (excerpt.includes(token)) score += 6;
      else if (haystack.includes(token)) score += 2;
      else return 0;
    }

    /* Exact title matches float to the top. */
    if (title === tokens.join(" ")) score += 60;
    score += entry.type === "page" ? 4 : 0;
    return score;
  }

  render(entries) {
    if (!this.results || !this.empty) return;
    this.results.innerHTML = "";

    if (!entries.length) {
      this.empty.hidden = false;
      this.empty.textContent = this.input.value.trim()
        ? "No results found. Try a different search."
        : "Type to search the site, documentation, wiki, and more.";
      return;
    }

    this.empty.hidden = true;

    /* Group by type, preserving order. */
    const grouped = new Map();
    for (const entry of entries) {
      const type = entry.type || "page";
      if (!grouped.has(type)) grouped.set(type, []);
      grouped.get(type).push(entry);
    }

    grouped.forEach((items, type) => {
      const label = document.createElement("div");
      label.className = "palette-group-label";
      label.textContent = TYPE_LABELS[type] ?? type;
      this.results.appendChild(label);

      items.forEach((entry) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "palette-result";
        button.setAttribute("role", "option");
        button.innerHTML = `
          <svg class="icon" aria-hidden="true"><use href="#icon-${escapeHtml(entry.icon || "file")}"/></svg>
          <span class="palette-result-title">${escapeHtml(entry.title)}</span>
          <span class="palette-result-type">${TYPE_LABELS[type] ?? type}</span>`;
        button.addEventListener("click", () => this.navigate(entry));
        button.addEventListener("pointermove", () => {
          this.selected = Array.from(this.results.querySelectorAll(".palette-result")).indexOf(button);
          this.refreshSelection();
        });
        this.results.appendChild(button);
      });
    });

    this.selected = -1;
  }

  refreshSelection() {
    const items = Array.from(this.results?.querySelectorAll(".palette-result") ?? []);
    items.forEach((item, i) => {
      const selected = i === this.selected;
      item.setAttribute("aria-selected", String(selected));
      if (selected) item.scrollIntoView({ block: "nearest" });
    });
  }

  navigate(entry) {
    window.location.assign(new URL(entry.url, window.location.origin).href);
    this.close();
  }

  onInput() {
    const query = this.input.value.trim();
    if (!query) {
      this.render([]);
      return;
    }
    const tokens = normalize(query).split(" ").filter(Boolean);

    const scored = this.index
      .map((entry) => ({ entry, score: this.score(entry, tokens) }))
      .filter((result) => result.score > 0)
      .sort((a, b) => b.score - a.score)
      .slice(0, 24);

    this.render(scored.map((result) => result.entry));
  }

  init() {
    if (!this.dialog || !this.input) return;
    this.loadIndex();

    this.input.addEventListener("input", () => {
      this.selected = -1;
      this.onInput();
    });

    this.dialog.addEventListener("keydown", (event) => {
      const items = () => this.results.querySelectorAll(".palette-result");
      switch (event.key) {
        case "ArrowDown":
          event.preventDefault();
          this.selected = Math.min(this.selected + 1, items().length - 1);
          this.refreshSelection();
          break;
        case "ArrowUp":
          event.preventDefault();
          this.selected = Math.max(this.selected - 1, -1);
          this.refreshSelection();
          break;
        case "Enter": {
          const item = items()[this.selected];
          if (item) item.click();
          break;
        }
        case "Escape":
          this.close();
          break;
      }
    });
  }
}

export function initSearch(basePath) {
  const palette = new CommandPalette(basePath);
  palette.init();

  document.addEventListener("keydown", (event) => {
    const target = event.target;
    const typing = target instanceof HTMLInputElement ||
      target instanceof HTMLTextAreaElement ||
      target?.isContentEditable;

    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
      event.preventDefault();
      palette.isOpen() ? palette.close() : palette.open();
      return;
    }

    if (event.key === "/" && !typing && !palette.isOpen()) {
      event.preventDefault();
      palette.open();
    }
  });

  /* Expose for the topbar search button. */
  document.addEventListener("kaiteyo:open-search", () => palette.open());
  document.addEventListener("kaiteyo:close-search", () => palette.close());

  return palette;
}
