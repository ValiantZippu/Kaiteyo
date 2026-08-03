/**
 * Kaiteyo website — interactive components.
 *
 * Lightweight, self-contained enhancements driven by data attributes so
 * the same markup works everywhere:
 *
 *   data-tabs / .tab / .tab-panel      -> tab groups
 *   .copy-button inside .code-block    -> clipboard copy
 *   [data-comparison]                  -> before/after image slider
 *   .reveal                            -> scroll-reveal
 *   [data-lightbox]                    -> fullscreen image preview
 *   dialog[data-dialog]                -> backdrop-click close
 */

import { $, $$ } from "./utils.js";

/** Tabs: button groups switching matching panels. */
function initTabs(root = document) {
  $$("[data-tabs]", root).forEach((group) => {
    const buttons = $$(".tab", group);
    const panels = $$(".tab-panel", group.closest("[data-tabs-root]") ?? group);
    const select = (index) => {
      buttons.forEach((button, i) => {
        const selected = i === index;
        button.setAttribute("aria-selected", String(selected));
        button.tabIndex = selected ? 0 : -1;
        if (selected) button.focus?.();
      });
      panels.forEach((panel, i) => {
        if (panel.dataset.tab === buttons[index]?.dataset.tab) {
          panel.hidden = false;
        } else if (i === index && !panel.dataset.tab) {
          panel.hidden = false;
        } else {
          panel.hidden = true;
        }
      });
    };

    buttons.forEach((button, index) => {
      button.setAttribute("role", "tab");
      button.addEventListener("click", () => select(index));
      button.addEventListener("keydown", (event) => {
        if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
        const target =
          event.key === "ArrowRight"
            ? (index + 1) % buttons.length
            : (index - 1 + buttons.length) % buttons.length;
        select(target);
        event.preventDefault();
      });
    });

    select(buttons.findIndex((button) => button.getAttribute("aria-selected") === "true") >= 0
      ? buttons.findIndex((button) => button.getAttribute("aria-selected") === "true")
      : 0);
  });
}

/** Copy-to-clipboard buttons inside code blocks. */
function initCopyButtons(root = document) {
  $$(".copy-button", root).forEach((button) => {
    button.addEventListener("click", async () => {
      const block = button.closest(".code-block");
      const text = block?.querySelector("pre")?.textContent ?? "";
      try {
        await navigator.clipboard.writeText(text);
      } catch {
        /* clipboard unavailable (insecure context) — select instead */
        const range = document.createRange();
        const pre = block?.querySelector("pre");
        if (pre) {
          range.selectNodeContents(pre);
          const selection = window.getSelection();
          selection?.removeAllRanges();
          selection?.addRange(range);
        }
      }
      const original = button.innerHTML;
      button.classList.add("is-copied");
      button.innerHTML = '<svg class="icon" aria-hidden="true"><use href="#icon-check"/></svg> Copied';
      window.setTimeout(() => {
        button.classList.remove("is-copied");
        button.innerHTML = original;
      }, 1600);
    });
  });
}

/** Before/after comparison slider. */
function initComparison(root = document) {
  $$("[data-comparison]", root).forEach((wrap) => {
    const after = wrap.querySelector(".comparison-after");
    const handle = wrap.querySelector(".comparison-handle");
    if (!after || !handle) return;

    const setPosition = (percent) => {
      const clamped = Math.min(100, Math.max(0, percent));
      after.style.width = `${clamped}%`;
      handle.style.left = `${clamped}%`;
    };

    const fromEvent = (event) => {
      const rect = wrap.getBoundingClientRect();
      return ((event.clientX - rect.left) / rect.width) * 100;
    };

    let dragging = false;

    wrap.addEventListener("pointerdown", (event) => {
      dragging = true;
      wrap.setPointerCapture(event.pointerId);
      setPosition(fromEvent(event));
    });

    wrap.addEventListener("pointermove", (event) => {
      if (dragging) setPosition(fromEvent(event));
    });

    wrap.addEventListener("pointerup", () => {
      dragging = false;
    });

    handle.addEventListener("keydown", (event) => {
      const current = parseFloat(after.style.width || "50");
      if (event.key === "ArrowLeft") setPosition(current - 2);
      if (event.key === "ArrowRight") setPosition(current + 2);
    });

    handle.setAttribute("role", "slider");
    handle.setAttribute("tabindex", "0");
    handle.setAttribute("aria-valuemin", "0");
    handle.setAttribute("aria-valuemax", "100");
    handle.setAttribute("aria-valuenow", "50");
    handle.setAttribute("aria-label", "Comparison position");
  });
}

/** Scroll reveal. */
function initReveals(root = document) {
  $$(".reveal", root).forEach((element) => {
    if (typeof IntersectionObserver === "undefined") {
      element.classList.add("is-visible");
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("is-visible");
            observer.unobserve(entry.target);
          }
        });
      },
      { rootMargin: "0px 0px -40px 0px" },
    );
    observer.observe(element);
  });
}

/** Fullscreen image preview via dialog. */
function initLightbox(root = document) {
  const images = $$("[data-lightbox]", root);
  if (!images.length) return;

  const dialog = document.createElement("dialog");
  dialog.className = "lightbox";
  dialog.setAttribute("aria-label", "Image preview");
  dialog.innerHTML = `
    <div class="lightbox-stage">
      <button class="btn-icon lightbox-close" data-lightbox-close aria-label="Close preview">
        <svg class="icon" aria-hidden="true"><use href="#icon-close"/></svg>
      </button>
      <img alt="" />
      <p class="lightbox-caption"></p>
    </div>`;
  document.body.appendChild(dialog);

  const img = dialog.querySelector("img");
  const caption = dialog.querySelector(".lightbox-caption");

  images.forEach((source) => {
    source.addEventListener("click", () => {
      const full = source.dataset.full ?? source.src;
      img.src = full;
      img.alt = source.alt || "";
      caption.textContent = source.dataset.caption ?? "";
      dialog.showModal();
    });
  });

  dialog.querySelector("[data-lightbox-close]").addEventListener("click", () => dialog.close());
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close();
  });
}

/** Close dialogs on backdrop click. */
function initDialogs(root = document) {
  $$("dialog[data-dialog]", root).forEach((dialog) => {
    dialog.addEventListener("click", (event) => {
      const rect = dialog.getBoundingClientRect();
      const outside =
        event.clientX < rect.left ||
        event.clientX > rect.right ||
        event.clientY < rect.top ||
        event.clientY > rect.bottom;
      if (outside) dialog.close();
    });
  });
}

/** Update the current year in footer elements. */
function initYears() {
  $$("[data-year]").forEach((element) => {
    element.textContent = String(new Date().getFullYear());
  });
}

export function initComponents() {
  initTabs();
  initCopyButtons();
  initComparison();
  initReveals();
  initLightbox();
  initDialogs();
  initYears();
}
