/**
 * Kaiteyo website — small DOM and general utilities.
 * Nothing theme- or navigation-specific lives here.
 */

/** Query a single element, with a required root. */
export function $(selector, root = document) {
  return root.querySelector(selector);
}

/** Query all matching elements as an array. */
export function $$(selector, root = document) {
  return Array.from(root.querySelectorAll(selector));
}

/** Call fn once after the element becomes visible in the viewport. */
export function onVisible(element, fn, options = {}) {
  if (typeof IntersectionObserver === "undefined") {
    fn();
    return () => {};
  }
  const observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        fn();
        observer.disconnect();
      }
    },
    { rootMargin: "0px 0px 120px 0px", ...options },
  );
  observer.observe(element);
  return () => observer.disconnect();
}

/** Debounce a function call. */
export function debounce(fn, wait = 150) {
  let timer = null;
  return (...args) => {
    window.clearTimeout(timer);
    timer = window.setTimeout(() => fn(...args), wait);
  };
}

/** Escape HTML to prevent injection when rendering user content. */
export function escapeHtml(value) {
  const div = document.createElement("div");
  div.textContent = value;
  return div.innerHTML;
}

/** Normalize text for search matching. */
export function normalize(value) {
  return String(value)
    .toLowerCase()
    .normalize("NFKC")
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .replace(/\s+/g, " ")
    .trim();
}
