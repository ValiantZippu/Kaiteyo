/* Kaiteyo kanban — client-side filtering only (real data, rendered by the build).
   Drag-and-drop editing is a documented backend feature (docs/website/API.md);
   this view never fakes persistence. */
(function () {
  "use strict";

  var board = document.querySelector("[data-kanban]");
  if (!board) return;

  var search = document.getElementById("kanban-search");
  var pkgSelect = document.getElementById("kanban-package");
  var priSelect = document.getElementById("kanban-priority");
  var reset = document.getElementById("kanban-reset");
  var empty = document.querySelector(".kanban-empty");

  function readParams() {
    var params = new URLSearchParams(window.location.search);
    if (params.get("package")) pkgSelect.value = params.get("package");
  }

  function cardCounts() {
    var counts = {};
    document.querySelectorAll("[data-column]").forEach(function (col) {
      var visible = 0;
      col.querySelectorAll("[data-card]").forEach(function (card) {
        if (!card.hidden) visible += 1;
      });
      counts[col.dataset.column] = visible;
    });
    return counts;
  }

  function apply() {
    var q = (search.value || "").trim().toLowerCase();
    var pkg = pkgSelect.value;
    var pri = priSelect.value;
    var anyVisible = false;

    document.querySelectorAll("[data-card]").forEach(function (card) {
      var text = (card.dataset.text || "").toLowerCase();
      var matches = true;
      if (q && text.indexOf(q) === -1) matches = false;
      if (pkg && card.dataset.package !== pkg) matches = false;
      if (pri && card.dataset.priority !== pri) matches = false;
      card.hidden = !matches;
      if (matches) anyVisible = true;
    });

    document.querySelectorAll("[data-column]").forEach(function (col) {
      var count = col.querySelector(".kanban-count");
      if (count) {
        var visible = cardCounts()[col.dataset.column] || 0;
        count.textContent = String(visible);
      }
      col.style.display = visibleIn(col) ? "" : "none";
    });

    if (empty) empty.hidden = anyVisible;
  }

  function visibleIn(col) {
    var any = false;
    col.querySelectorAll("[data-card]").forEach(function (card) {
      if (!card.hidden) any = true;
    });
    return any;
  }

  search.addEventListener("input", apply);
  pkgSelect.addEventListener("change", apply);
  priSelect.addEventListener("change", apply);
  reset.addEventListener("click", function () {
    search.value = "";
    pkgSelect.value = "";
    priSelect.value = "";
    apply();
  });

  readParams();
  apply();
})();
