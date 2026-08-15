/* Kaiteyo whiteboard — pan / zoom / semantic-LOD architecture canvas.
   Data source: window.KAITEYO_WHITEBOARD (embedded from website/config/project/whiteboard.json).
   Read-only view; editing is a documented backend feature (docs/website/API.md). */
(function () {
  "use strict";

  var data = window.KAITEYO_WHITEBOARD;
  var canvas = document.getElementById("wb-canvas");
  if (!canvas || !data) return;

  var groups = data.groups || [];
  var nodes = data.nodes || [];
  var edges = data.edges || [];
  var canvasW = (data.canvas && data.canvas.width) || 2600;
  var canvasH = (data.canvas && data.canvas.height) || 1700;

  var viewport = document.createElement("div");
  viewport.className = "whiteboard-viewport";
  canvas.appendChild(viewport);

  var svgNS = "http://www.w3.org/2000/svg";
  var svg = document.createElementNS(svgNS, "svg");
  svg.setAttribute("width", canvasW);
  svg.setAttribute("height", canvasH);
  svg.setAttribute("aria-hidden", "true");
  viewport.appendChild(svg);

  var edgeLayer = document.createElementNS(svgNS, "g");
  svg.appendChild(edgeLayer);

  var nodeMap = {};
  var nodeEls = {};

  /* LOD levels: 1 = groups only, 2 = node labels, 3 = full detail. */
  var LOD_GROUP_ONLY = 0.32;
  var LOD_LABELS = 0.6;

  function kindClass(kind) {
    return "wb-node-kind-" + (kind || "system");
  }

  function statusLabel(status) {
    return status || "";
  }

  /* ---- build edges (SVG) ---- */
  var edgeLookup = {};
  edges.forEach(function (edge) {
    edgeLookup[edge.from + "->" + edge.to] = edge;
  });

  function nodeCenter(id) {
    var n = nodeMap[id];
    return n ? { x: n.x + (n.w || 230) / 2, y: n.y + (n.h || 74) / 2 } : null;
  }

  function drawEdges() {
    edgeLayer.innerHTML = "";
    edges.forEach(function (edge) {
      var a = nodeCenter(edge.from);
      var b = nodeCenter(edge.to);
      if (!a || !b) return;
      var mid = { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 };
      var d = "M" + a.x + "," + a.y + " C" + mid.x + "," + a.y + " " + mid.x + "," + b.y + " " + b.x + "," + b.y;
      var path = document.createElementNS(svgNS, "path");
      path.setAttribute("d", d);
      path.setAttribute("class", "wb-edge");
      path.setAttribute("data-type", edge.type || "");
      path.setAttribute("fill", "none");
      edgeLayer.appendChild(path);
      if (edge.label) {
        var label = document.createElementNS(svgNS, "text");
        label.setAttribute("class", "wb-edge-label");
        label.setAttribute("x", mid.x);
        label.setAttribute("y", mid.y - 4);
        label.textContent = edge.label;
        edgeLayer.appendChild(label);
      }
    });
  }

  /* ---- build groups ---- */
  var groupEls = {};
  groups.forEach(function (group) {
    var el = document.createElement("div");
    el.className = "wb-group";
    el.style.left = group.x + "px";
    el.style.top = group.y + "px";
    el.style.width = group.w + "px";
    el.style.height = group.h + "px";
    var label = document.createElement("span");
    label.className = "wb-group-label";
    label.textContent = group.label;
    el.appendChild(label);
    viewport.appendChild(el);
    groupEls[group.id] = el;
  });

  /* ---- build nodes ---- */
  nodes.forEach(function (node) {
    nodeMap[node.id] = node;
    var el = document.createElement("article");
    el.className = "wb-node " + kindClass(node.kind);
    el.style.left = node.x + "px";
    el.style.top = node.y + "px";
    el.setAttribute("role", "button");
    el.setAttribute("tabindex", "0");
    el.setAttribute("aria-label", node.label + (node.description ? " — " + node.description : ""));
    el.dataset.id = node.id;

    var head = document.createElement("div");
    head.className = "wb-node-head";
    var label = document.createElement("span");
    label.className = "wb-node-label";
    label.textContent = node.label;
    head.appendChild(label);
    el.appendChild(head);

    if (node.description) {
      var desc = document.createElement("p");
      desc.className = "wb-node-desc";
      desc.textContent = node.description;
      el.appendChild(desc);
    }

    var status = document.createElement("span");
    status.className = "wb-node-status";
    status.textContent = statusLabel(node.status);
    el.appendChild(status);

    function open() {
      if (node.docs) window.open(node.docs, "_blank", "noopener");
    }
    el.addEventListener("click", function (e) {
      e.stopPropagation();
      open();
    });
    el.addEventListener("keydown", function (e) {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        open();
      }
    });

    viewport.appendChild(el);
    nodeEls[node.id] = el;
  });

  drawEdges();

  /* ---- view state ---- */
  var scale = 0.6;
  var tx = 40;
  var ty = 40;
  var panning = false;
  var startX = 0;
  var startY = 0;
  var startTx = 0;
  var startTy = 0;
  var moved = false;

  function applyTransform() {
    viewport.style.transform = "translate(" + tx + "px," + ty + "px) scale(" + scale + ")";
    applyLOD();
    updateMinimap();
  }

  function canvasSize() {
    return { w: canvas.clientWidth, h: canvas.clientHeight };
  }

  function applyLOD() {
    var level = scale < LOD_GROUP_ONLY ? 1 : scale < LOD_LABELS ? 2 : 3;
    canvas.dataset.lod = level;
    Object.keys(groupEls).forEach(function (id) {
      groupEls[id].style.display = level >= 1 ? "" : "none";
    });
    nodes.forEach(function (node) {
      var el = nodeEls[node.id];
      if (level === 1) {
        el.style.display = "none";
      } else if (level === 2) {
        el.style.display = "";
        var desc = el.querySelector(".wb-node-desc");
        if (desc) desc.style.display = "none";
        var status = el.querySelector(".wb-node-status");
        if (status) status.style.display = "none";
      } else {
        el.style.display = "";
        var d2 = el.querySelector(".wb-node-desc");
        if (d2) d2.style.display = "";
        var s2 = el.querySelector(".wb-node-status");
        if (s2) s2.style.display = "";
      }
    });
  }

  /* ---- pan ---- */
  canvas.addEventListener("pointerdown", function (e) {
    if (e.target.closest(".wb-node")) return;
    panning = true;
    moved = false;
    startX = e.clientX;
    startY = e.clientY;
    startTx = tx;
    startTy = ty;
    canvas.classList.add("is-panning");
    canvas.setPointerCapture(e.pointerId);
  });

  canvas.addEventListener("pointermove", function (e) {
    if (!panning) return;
    var dx = e.clientX - startX;
    var dy = e.clientY - startY;
    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) moved = true;
    tx = startTx + dx;
    ty = startTy + dy;
    applyTransform();
  });

  function endPan(e) {
    if (!panning) return;
    panning = false;
    canvas.classList.remove("is-panning");
    if (e && e.pointerId !== undefined) {
      try { canvas.releasePointerCapture(e.pointerId); } catch (_) { /* noop */ }
    }
  }

  canvas.addEventListener("pointerup", endPan);
  canvas.addEventListener("pointercancel", endPan);

  /* ---- zoom ---- */
  function zoomAt(factor, cx, cy) {
    var box = canvasSize();
    var px = cx !== undefined ? cx : box.w / 2;
    var py = cy !== undefined ? cy : box.h / 2;
    var newScale = Math.min(2.5, Math.max(0.08, scale * factor));
    var worldX = (px - tx) / scale;
    var worldY = (py - ty) / scale;
    tx = px - worldX * newScale;
    ty = py - worldY * newScale;
    scale = newScale;
    applyTransform();
  }

  canvas.addEventListener("wheel", function (e) {
    e.preventDefault();
    var rect = canvas.getBoundingClientRect();
    zoomAt(Math.exp(-e.deltaY * 0.0015), e.clientX - rect.left, e.clientY - rect.top);
  }, { passive: false });

  document.getElementById("wb-zoom-in").addEventListener("click", function () {
    zoomAt(1.3);
  });
  document.getElementById("wb-zoom-out").addEventListener("click", function () {
    zoomAt(1 / 1.3);
  });

  /* ---- fit ---- */
  function fit() {
    var box = canvasSize();
    var minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    nodes.forEach(function (n) {
      minX = Math.min(minX, n.x);
      minY = Math.min(minY, n.y);
      maxX = Math.max(maxX, n.x + (n.w || 230));
      maxY = Math.max(maxY, n.y + (n.h || 74));
    });
    if (!isFinite(minX)) { minX = 0; minY = 0; maxX = canvasW; maxY = canvasH; }
    var pad = 60;
    var contentW = (maxX - minX) + pad * 2;
    var contentH = (maxY - minY) + pad * 2;
    scale = Math.min(1, Math.max(0.08, Math.min(box.w / contentW, box.h / contentH)));
    tx = (box.w - (maxX - minX) * scale) / 2 - minX * scale;
    ty = (box.h - (maxY - minY) * scale) / 2 - minY * scale;
    applyTransform();
  }

  document.getElementById("wb-reset").addEventListener("click", fit);

  /* ---- minimap ---- */
  var mmWrap = document.createElement("div");
  mmWrap.className = "wb-minimap";
  var mmCanvas = document.createElement("canvas");
  mmCanvas.className = "wb-minimap-canvas";
  mmCanvas.width = 200;
  mmCanvas.height = 120;
  mmWrap.appendChild(mmCanvas);
  canvas.appendChild(mmWrap);

  function mmScale() {
    return { sx: 200 / canvasW, sy: 120 / canvasH };
  }

  function updateMinimap() {
    var ctx = mmCanvas.getContext("2d");
    var s = mmScale();
    ctx.clearRect(0, 0, 200, 120);
    ctx.fillStyle = "rgba(0,0,0,0.04)";
    nodes.forEach(function (n) {
      ctx.fillRect(n.x * s.sx, n.y * s.sy, (n.w || 230) * s.sx, (n.h || 74) * s.sy);
    });
    var box = canvasSize();
    var wx = -tx / scale;
    var wy = -ty / scale;
    var ww = box.w / scale;
    var wh = box.h / scale;
    ctx.strokeStyle = "var(--accent, #c9431f)";
    ctx.lineWidth = 2;
    ctx.strokeRect(wx * s.sx, wy * s.sy, ww * s.sx, wh * s.sy);
  }

  /* ---- keyboard ---- */
  canvas.addEventListener("keydown", function (e) {
    if (e.key === "+" || e.key === "=") zoomAt(1.3);
    else if (e.key === "-") zoomAt(1 / 1.3);
    else if (e.key === "0") fit();
  });
  canvas.setAttribute("tabindex", "0");

  /* ---- init ---- */
  fit();
  window.addEventListener("resize", function () {
    applyTransform();
    updateMinimap();
  });
})();
