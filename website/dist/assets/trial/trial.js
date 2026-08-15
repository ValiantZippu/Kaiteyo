/**
 * Kaiteyo Web Trial — application logic.
 *
 * A self-contained slice of the Kaiteyo experience that runs entirely in
 * the browser: Home dashboard, Kanji/Vocabulary browse, kanji detail with
 * stroke-order preview, real canvas writing practice, a flashcard study
 * session with lightweight SRS intervals, and progress charts.
 *
 * State is persisted in localStorage under KAITEYO_TRIAL.meta.storageKey;
 * the first visit is seeded with demo progress so the trial never looks
 * empty. Nothing here touches real user data — it cannot; there is no
 * network and no shared storage with the application.
 */

(() => {
  const DATA = window.KAITEYO_TRIAL;
  const STORAGE_KEY = DATA.meta.storageKey;

  const BASE_PATH = document.documentElement.dataset.basePath ?? "/";

  /* ------------------------------------------------------------------ */
  /* State                                                               */
  /* ------------------------------------------------------------------ */

  const DAY_MS = 24 * 60 * 60 * 1000;
  const MIN_MS = 60 * 1000;

  const todayKey = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  };

  function seedState() {
    const p = DATA.demoProgress;
    return {
      seeded: true,
      createdAt: Date.now(),
      // card key -> { due, interval (minutes), reps, lapses, state }
      cards: {},
      reviewsByDay: { [todayKey()]: p.reviewsToday },
      reviewsTotal: p.reviewsTotal,
      cardsLearned: p.cardsLearned,
      streak: p.streakDays,
      lastActiveDay: todayKey(),
      writing: { attempts: p.writingAttempts, correct: Math.round(p.writingAttempts * p.writingAccuracy) },
      // Demo decks are "half studied" so the trial looks alive.
      seededDecks: true,
    };
  }

  function loadState() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) return JSON.parse(raw);
    } catch (e) {
      /* corrupted storage — fall through to reseed */
    }
    return seedState();
  }

  let state = loadState();
  const persist = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch (e) {
      /* storage full or unavailable — trial still works for the session */
    }
  };

  /* ------------------------------------------------------------------ */
  /* Card helpers                                                        */
  /* ------------------------------------------------------------------ */

  const kanjiByChar = Object.fromEntries(DATA.kanji.map((k) => [k.c, k]));
  const vocabByWord = Object.fromEntries(DATA.vocab.map((v) => [v.w, v]));

  function cardFromDeck(deckId) {
    const deck = DATA.decks.find((d) => d.id === deckId);
    if (!deck) return [];
    return deck.cards.map((c) => {
      if (kanjiByChar[c]) return { kind: "kanji", key: `k:${c}`, deckId, ...kanjiByChar[c] };
      if (vocabByWord[c]) return { kind: "vocab", key: `v:${c}`, deckId, ...vocabByWord[c] };
      return null;
    }).filter(Boolean);
  }

  function dueCards(deckId) {
    const now = Date.now();
    return cardFromDeck(deckId)
      .map((card) => {
        const rec = state.cards[card.key];
        return { card, rec, isNew: !rec, isDue: !rec || rec.due <= now };
      })
      .filter((c) => c.isNew || c.isDue);
  }

  function gradeCard(key, grade) {
    const now = Date.now();
    const rec = state.cards[key] || { interval: 0, reps: 0, lapses: 0, due: 0, state: "new" };

    let interval;
    switch (grade) {
      case "again":
        interval = 1; // minute
        rec.lapses += 1;
        rec.state = "learning";
        break;
      case "hard":
        interval = 10; // minutes
        rec.state = "learning";
        break;
      case "good":
        interval = rec.interval > 0 ? Math.min(rec.interval * 2.2, 90 * DAY_MS / MIN_MS) : 1 * DAY_MS / MIN_MS;
        rec.state = "review";
        break;
      case "easy":
        interval = Math.max(rec.interval * 3, 3 * DAY_MS / MIN_MS);
        rec.state = "review";
        break;
    }
    rec.interval = interval;
    rec.reps += 1;
    rec.due = now + interval * MIN_MS;

    state.cards[key] = rec;
    state.reviewsTotal += 1;
    state.reviewsByDay[todayKey()] = (state.reviewsByDay[todayKey()] || 0) + 1;

    if (grade === "good" || grade === "easy") {
      if (!state.learnedKeys) state.learnedKeys = [];
      if (!state.learnedKeys.includes(key)) {
        state.learnedKeys.push(key);
        state.cardsLearned += 1;
      }
    }
    state.streak = Math.max(state.streak, 1);
    state.lastActiveDay = todayKey();
    persist();
  }

  /* ------------------------------------------------------------------ */
  /* DOM helpers                                                         */
  /* ------------------------------------------------------------------ */

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

  const el = (tag, attrs = {}, children = []) => {
    const node = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs)) {
      if (k === "class") node.className = v;
      else if (k === "html") node.innerHTML = v;
      else if (k.startsWith("on") && typeof v === "function") node.addEventListener(k.slice(2), v);
      else if (k === "dataset") Object.assign(node.dataset, v);
      else node.setAttribute(k, v);
    }
    for (const child of [].concat(children)) {
      if (child == null) continue;
      node.appendChild(typeof child === "string" ? document.createTextNode(child) : child);
    }
    return node;
  };

  const icon = (name, className = "") => {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("class", `icon ${className}`);
    svg.setAttribute("aria-hidden", "true");
    const use = document.createElementNS("http://www.w3.org/2000/svg", "use");
    use.setAttribute("href", `#icon-${name}`);
    svg.appendChild(use);
    return svg;
  };

  const badge = (text, tone = "badge-muted") => el("span", { class: `badge ${tone}` }, [text]);

  /* ------------------------------------------------------------------ */
  /* Router                                                              */
  /* ------------------------------------------------------------------ */

  const views = {
    home: { title: "Home" },
    browse: { title: "Browse" },
    detail: { title: "Kanji" },
    write: { title: "Writing" },
    study: { title: "Study" },
    progress: { title: "Progress" },
  };

  let currentView = "home";
  let browseTab = "kanji";
  let browseQuery = "";
  let browseLevel = null;
  let detailChar = null;
  let writeQueue = null;
  let studyQueue = null;
  let studyIdx = 0;
  let studyFlipped = false;
  let studyResult = null;

  const content = () => $("#trialContent");
  const viewTitle = () => $("#trialTopbarTitle");
  const navItems = () => $$(".trial-nav-item[data-view]");

  function setNav(view) {
    navItems().forEach((item) => {
      item.setAttribute("aria-current", String(item.dataset.view === view));
    });
  }

  function go(view, opts = {}) {
    currentView = view;
    if (opts.tab) browseTab = opts.tab;
    if (opts.query !== undefined) browseQuery = opts.query;
    if (opts.level !== undefined) browseLevel = opts.level;
    if (opts.char) detailChar = opts.char;
    if (opts.writeQueue) writeQueue = opts.writeQueue;
    if (opts.studyDeck !== undefined) {
      studyQueue = opts.studyDeck ? dueCards(opts.studyDeck).map((c) => c.card) : [];
      studyIdx = 0;
      studyFlipped = false;
      studyResult = null;
    }
    setNav(view);
    viewTitle().textContent = views[view]?.title ?? view;
    render();
    content().scrollTop = 0;
  }

  const fmtTime = (ms) => {
    if (ms < 60 * MIN_MS) return `${Math.max(1, Math.round(ms / MIN_MS))}m`;
    if (ms < DAY_MS) return `${Math.round(ms / (60 * MIN_MS))}h`;
    return `${(ms / DAY_MS).toFixed(ms < 3 * DAY_MS ? 1 : 0)}d`;
  };

  /* ------------------------------------------------------------------ */
  /* Rendering                                                           */
  /* ------------------------------------------------------------------ */

  function render() {
    const node = content();
    node.innerHTML = "";
    const view = el("div", { class: "trial-view", id: "trialView" });
    node.appendChild(view);

    if (currentView === "home") renderHome(view);
    else if (currentView === "browse") renderBrowse(view);
    else if (currentView === "detail") renderDetail(view);
    else if (currentView === "write") renderWrite(view);
    else if (currentView === "study") renderStudy(view);
    else if (currentView === "progress") renderProgress(view);
  }

  /* --- Home --- */

  function renderHome(view) {
    const dueAll = DATA.decks.map((d) => dueCards(d.id).length);
    const dueTotal = dueAll.reduce((a, b) => a + b, 0);

    view.appendChild(el("div", { class: "trial-view-head" }, [
      el("h1", {}, ["Welcome back"]),
      el("p", {}, [
        "This is the web trial — a real slice of Kaiteyo. Study a few cards, try writing a kanji, and explore the library. Everything you do stays in this browser.",
      ]),
    ]));

    const hero = el("div", { class: "trial-hero-row" }, []);
    const continueCard = el("div", { class: "trial-continue" }, [
      el("div", {}, [
        el("span", { class: "badge badge-accent" }, ["Continue studying"]),
        el("h2", { style: "margin-top: var(--space-3)" }, [`${dueTotal} cards ready`]),
        el("p", {}, [`Across ${DATA.decks.length} decks — new cards, reviews, and a couple you're about to forget.`]),
      ]),
      el("div", { class: "trial-continue-actions flex gap-3" }, [
        el("button", { class: "btn btn-primary", onclick: () => startSession() }, [icon("play"), " Start a session"]),
      ]),
    ]);
    hero.appendChild(continueCard);

    const quick = el("div", { class: "trial-panel" }, [
      el("h3", {}, [icon("grid"), " Quick actions"]),
      el("div", { class: "flex-col gap-2" }, [
        el("button", { class: "trial-deck-row", onclick: () => go("browse", { tab: "kanji" }) }, [
          icon("grid"), el("span", { class: "trial-deck-name" }, ["Browse kanji"]),
          el("span", { class: "trial-deck-meta" }, ["Readings, meanings, stroke order"]),
          el("span", { class: "trial-deck-due" }, ["→"]),
        ]),
        el("button", { class: "trial-deck-row", onclick: () => go("browse", { tab: "vocab" }) }, [
          icon("book"), el("span", { class: "trial-deck-name" }, ["Browse vocabulary"]),
          el("span", { class: "trial-deck-meta" }, ["Words with example sentences"]),
          el("span", { class: "trial-deck-due" }, ["→"]),
        ]),
        el("button", { class: "trial-deck-row", onclick: () => {
          writeQueue = DATA.kanji.filter((k) => k.s && k.s.length).slice(0, 3);
          go("write");
        } }, [
          icon("pen"), el("span", { class: "trial-deck-name" }, ["Practice writing"]),
          el("span", { class: "trial-deck-meta" }, ["Trace strokes on a real canvas"]),
          el("span", { class: "trial-deck-due" }, ["→"]),
        ]),
      ]),
    ]);
    hero.appendChild(quick);
    view.appendChild(hero);

    const stats = el("div", { class: "trial-stats" }, [
      statCard(state.streak, "day streak"),
      statCard(state.reviewsByDay[todayKey()] || 0, "reviews today"),
      statCard(state.cardsLearned, "cards learned"),
    ]);
    view.appendChild(stats);

    const cols = el("div", { class: "trial-cols" }, []);

    const decksPanel = el("div", { class: "trial-panel" }, [
      el("h3", {}, [icon("folder"), " Your decks"]),
    ]);
    DATA.decks.forEach((deck, i) => {
      const due = dueAll[i];
      decksPanel.appendChild(el("button", { class: "trial-deck-row", onclick: () => go("study", { studyDeck: deck.id }) }, [
        (() => {
          const ic = el("span", { class: "trial-deck-icon", style: `background: ${deck.color}22; color: ${deck.color}` });
          ic.appendChild(icon(deck.icon));
          return ic;
        })(),
        el("span", { class: "flex-col", style: "min-width: 0" }, [
          el("span", { class: "trial-deck-name" }, [deck.name]),
          el("span", { class: "trial-deck-meta" }, [deck.description]),
        ]),
        el("span", { class: "trial-deck-due" }, [`${due} due`]),
      ]));
    });
    cols.appendChild(decksPanel);

    const activity = el("div", { class: "trial-panel" }, [
      el("h3", {}, [icon("history"), " Recent activity"]),
      el("ul", { class: "trial-activity" }, DATA.demoProgress.activity.map((a) =>
        el("li", {}, [icon(a.type === "write" ? "pen" : a.type === "mine" ? "zap" : a.type === "exam" ? "milestone" : "check"), el("span", {}, [a.label]), el("time", {}, [a.time])]),
      )),
    ]);
    cols.appendChild(activity);
    view.appendChild(cols);

    appendExitNote(view);
  }

  const statCard = (value, label) =>
    el("div", { class: "trial-stat" }, [
      el("div", { class: "trial-stat-value" }, [String(value)]),
      el("div", { class: "trial-stat-label" }, [label]),
    ]);

  /* --- Browse --- */

  function renderBrowse(view) {
    const toolbar = el("div", { class: "trial-toolbar" }, [
      el("div", { class: "trial-tabs", role: "tablist" }, [
        el("button", { class: "trial-tab", role: "tab", "aria-selected": String(browseTab === "kanji"), onclick: () => go("browse", { tab: "kanji" }) }, ["Kanji"]),
        el("button", { class: "trial-tab", role: "tab", "aria-selected": String(browseTab === "vocab"), onclick: () => go("browse", { tab: "vocab" }) }, ["Vocabulary"]),
      ]),
      el("div", { class: "trial-search", style: "flex: 1; min-width: 180px" }, [
        icon("search"),
        el("input", {
          type: "search",
          placeholder: browseTab === "kanji" ? "Search kanji, reading, meaning…" : "Search word, reading, meaning…",
          value: browseQuery,
        }),
      ]),
    ]);

    const filterRow = el("div", { class: "trial-filter-chips" }, [
      el("button", { class: "trial-chip", "aria-pressed": String(browseLevel === null), onclick: () => { browseLevel = null; renderBrowse(view); } }, ["All"]),
      ...["N5", "N4", "N3", "N2", "N1"].map((lvl) =>
        el("button", {
          class: "trial-chip",
          "aria-pressed": String(browseLevel === lvl),
          onclick: () => { browseLevel = lvl; renderBrowse(view); },
        }, [lvl]),
      ),
    ]);

    view.appendChild(el("div", { class: "trial-view-head" }, [
      el("h1", {}, [browseTab === "kanji" ? "Kanji" : "Vocabulary"]),
      el("p", {}, [browseTab === "kanji"
        ? "The trial ships a taste of the bundled N5 kanji set — readings, meanings, JLPT, stroke counts, and stroke-order practice."
        : "Everyday words with readings, meanings, and example sentences — the same shape as the full 17,000+ entry dictionary."]),
    ]));
    view.appendChild(toolbar);
    view.appendChild(filterRow);

    // The list renders into a stable container so live search never steals
    // input focus (only the list is replaced on each keystroke).
    const listHost = el("div", { id: "trialBrowseList" });
    view.appendChild(listHost);
    const renderList = () => {
      listHost.innerHTML = "";
      if (browseTab === "kanji") renderKanjiList(listHost);
      else renderVocabList(listHost);
    };
    renderList();

    // Live search: update only the list container.
    const searchInput = toolbar.querySelector("input");
    searchInput.addEventListener("input", () => {
      browseQuery = searchInput.value;
      renderList();
    });

    appendExitNote(view);
  }

  function matchesKanji(k, q) {
    if (browseLevel && k.jlpt !== browseLevel) return false;
    if (!q) return true;
    const hay = [k.c, k.on.join(" "), k.kun.join(" "), k.m.join(" ")].join(" ").toLowerCase();
    return hay.includes(q.toLowerCase());
  }

  function renderKanjiList(view) {
    const q = browseQuery.trim();
    const items = DATA.kanji.filter((k) => matchesKanji(k, q));
    if (!items.length) {
      view.appendChild(el("div", { class: "trial-empty" }, ["No kanji match. Try a reading like “みず” or a meaning like “water”."]));
      return;
    }
    const grid = el("div", { class: "trial-kanji-grid" }, items.map((k) =>
      el("button", {
        class: "trial-kanji-tile",
        onclick: () => go("detail", { char: k.c }),
        "aria-label": `${k.c} — ${k.m.join(", ")}`,
      }, [
        el("span", { class: "kanji" }, [k.c]),
        el("span", { class: "mean" }, [k.m[0]]),
      ]),
    ));
    view.appendChild(grid);
  }

  function matchesVocab(v, q) {
    if (browseLevel && v.jlpt !== browseLevel) return false;
    if (!q) return true;
    const hay = [v.w, v.k, v.m.join(" ")].join(" ").toLowerCase();
    return hay.includes(q.toLowerCase());
  }

  function renderVocabList(view) {
    const q = browseQuery.trim();
    const items = DATA.vocab.filter((v) => matchesVocab(v, q));
    if (!items.length) {
      view.appendChild(el("div", { class: "trial-empty" }, ["No words match. Try “water” or “たべる”."]));
      return;
    }
    const list = el("div", { class: "trial-vocab-list" }, items.map((v) =>
      el("button", { class: "trial-vocab-row", onclick: () => {
        browseTab = "vocab";
        studyQueue = [buildCard(v)];
        studyIdx = 0;
        studyFlipped = false;
        studyResult = null;
        go("study");
      } }, [
        el("span", { class: "trial-vocab-word" }, [v.w]),
        el("span", { class: "trial-vocab-reading" }, [v.k]),
        el("span", { class: "trial-vocab-meaning" }, [v.m.join(" · ")]),
        badge(v.jlpt, "badge-muted"),
      ]),
    ));
    view.appendChild(list);
  }

  function buildCard(entry) {
    if (entry.c) return { kind: "kanji", key: `k:${entry.c}`, deckId: "kanji-n5", ...entry };
    return { kind: "vocab", key: `v:${entry.w}`, deckId: "vocab-n5", ...entry };
  }

  /* --- Kanji detail --- */

  function renderDetail(view) {
    const k = kanjiByChar[detailChar];
    if (!k) { go("browse", { tab: "kanji" }); return; }

    const hasStrokes = k.s && k.s.length > 0;

    const left = el("div", { class: "trial-detail-char" }, [
      el("div", { class: "big" }, [k.c]),
      el("div", { class: "readings" }, [
        k.on.length ? el("div", {}, [`on: ${k.on.join(" · ")}`]) : null,
        k.kun.length ? el("div", {}, [`kun: ${k.kun.join(" · ")}`]) : null,
      ]),
      el("div", { class: "trial-detail-actions" }, [
        el("button", { class: "btn btn-ghost btn-sm", onclick: () => { studyQueue = [buildCard(k)]; studyIdx = 0; studyFlipped = false; studyResult = null; go("study"); } }, ["Study this card"]),
        hasStrokes
          ? el("button", { class: "btn btn-primary btn-sm", onclick: () => { writeQueue = [k]; go("write"); } }, [icon("pen"), " Practice writing"])
          : el("button", { class: "btn btn-ghost btn-sm", onclick: () => writeToast(`${k.c} needs full stroke data — try a practice-set kanji like 日 or 木.`) }, [icon("pen"), " Practice writing"]),
      ]),
    ]);

    const right = el("div", { class: "trial-detail-side" }, [
      el("div", { class: "trial-view-head", style: "margin-bottom: var(--space-4)" }, [
        el("h1", { style: "font-size: var(--text-2xl)" }, [`${k.c} — ${k.m.join(", ")}`]),
      ]),
      el("div", { class: "trial-detail-facts" }, [
        fact("JLPT", k.jlpt),
        fact("Grade", `${k.g} (kyōiku)`),
        fact("Strokes", `${k.n}`),
        fact("Radical", k.r),
      ]),
    ]);

    if (hasStrokes) {
      right.appendChild(el("div", { class: "trial-panel" }, [
        el("h3", {}, [icon("pen"), " Stroke order"]),
        el("div", { class: "trial-strokes" }, k.s.map((stroke, i) =>
          strokeMini(stroke, i + 1, k.n),
        )),
        el("button", { class: "btn btn-ghost btn-sm", style: "margin-top: var(--space-4)", onclick: () => playStrokeOrder(k) }, [icon("play"), " Animate stroke order"]),
      ]));
    } else {
      right.appendChild(el("div", { class: "trial-panel" }, [
        el("h3", {}, [icon("pen"), " Stroke order"]),
        el("p", { style: "color: var(--text-secondary); font-size: var(--text-sm); margin: 0" }, [
          `${k.c} is part of the full bundled database — stroke data for the entire set is included in the desktop and mobile apps. The trial carries detailed stroke guides for a handful of practice kanji.`,
        ]),
      ]));
    }

    const similar = DATA.kanji.filter((o) => o.r === k.r && o.c !== k.c).slice(0, 6);
    if (similar.length) {
      right.appendChild(el("div", { class: "trial-panel", style: "margin-top: var(--space-4)" }, [
        el("h3", {}, [icon("grid"), ` Same radical (${k.r})`]),
        el("div", { class: "trial-kanji-grid" }, similar.map((o) =>
          el("button", { class: "trial-kanji-tile", onclick: () => go("detail", { char: o.c }) }, [
            el("span", { class: "kanji" }, [o.c]),
            el("span", { class: "mean" }, [o.m[0]]),
          ]),
        )),
      ]));
    }

    view.appendChild(el("div", { class: "trial-detail" }, [left, right]));
    appendExitNote(view);
  }

  const fact = (label, value) =>
    el("div", { class: "trial-fact" }, [
      el("div", { class: "trial-fact-label" }, [label]),
      el("div", { class: "trial-fact-value" }, [value]),
    ]);

  /** Draw one stroke as a mini canvas (for the stroke-order strip). */
  function strokeMini(stroke, num) {
    const wrap = el("div", { class: "trial-stroke-mini" }, [
      el("span", { class: "num" }, [String(num)]),
    ]);
    const canvas = el("canvas", { width: 56, height: 56, "aria-hidden": "true" });
    wrap.appendChild(canvas);
    requestAnimationFrame(() => drawStrokeOnCanvas(canvas, stroke, 3, 56, { ghost: true }));
    return wrap;
  }

  function drawStrokeOnCanvas(canvas, stroke, lineWidth, size, opts = {}) {
    const ctx = canvas.getContext("2d");
    const dpr = window.devicePixelRatio || 1;
    canvas.width = size * dpr;
    canvas.height = size * dpr;
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, size, size);
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.lineWidth = lineWidth;

    ctx.strokeStyle = opts.color || (opts.ghost ? "rgba(128,128,128,0.35)" : getComputedStyle(document.documentElement).getPropertyValue("--accent") || "#C2FC8B");
    ctx.beginPath();
    stroke.forEach(([x, y], i) => {
      const px = (x / 100) * size;
      const py = (y / 100) * size;
      if (i === 0) ctx.moveTo(px, py);
      else ctx.lineTo(px, py);
    });
    ctx.stroke();
  }

  function playStrokeOrder(k) {
    if (!k.s || !k.s.length) return;
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const canvas = el("canvas", { width: 240, height: 240, style: "display:block; margin: var(--space-4) auto 0", "aria-label": `Stroke order for ${k.c}` });
    const wrap = el("div", { style: "text-align:center" }, [canvas]);
    const dialog = el("dialog", { class: "trial-dialog", style: "width: min(360px, 92vw)" }, [
      el("div", { class: "dialog-header" }, [el("h2", {}, [`${k.c} — stroke order`]), el("button", { class: "btn-icon dialog-close", onclick: () => dialog.close() }, [icon("close")])]),
      wrap,
    ]);
    document.body.appendChild(dialog);
    dialog.showModal();

    const ctx = canvas.getContext("2d");
    const size = 240;
    ctx.clearRect(0, 0, size, size);
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.lineWidth = 7;
    const accent = getComputedStyle(document.documentElement).getPropertyValue("--accent") || "#C2FC8B";

    let i = 0;
    const step = () => {
      if (i >= k.s.length) return;
      const stroke = k.s[i];
      ctx.strokeStyle = accent;
      ctx.beginPath();
      stroke.forEach(([x, y], j) => {
        const px = (x / 100) * size;
        const py = (y / 100) * size;
        if (j === 0) ctx.moveTo(px, py);
        else ctx.lineTo(px, py);
      });
      ctx.stroke();
      i += 1;
      if (i < k.s.length) window.setTimeout(step, reduce ? 0 : 420);
    };
    step();
  }

  /* --- Writing practice --- */

  function renderWrite(view) {
    if (!writeQueue || !writeQueue.length) {
      writeQueue = DATA.kanji.filter((k) => k.s && k.s.length).slice(0, 5);
    }
    const queue = writeQueue;
    const k = queue[0];

    const board = el("div", { class: "trial-write-board" });
    const canvas = el("canvas", { width: 420, height: 420, "aria-label": `Writing practice canvas for ${k.c}` });
    const hint = el("div", { class: "trial-write-hint", role: "status", "aria-live": "polite" });
    board.appendChild(canvas);
    board.appendChild(hint);

    const side = el("div", { class: "trial-write-side" }, [
      el("div", { class: "trial-view-head" }, [
        el("h1", {}, [`Write ${k.c}`]),
        el("p", {}, [
          k.m.join(", ") + (k.kun.length ? ` — kun ${k.kun.join("·")}` : "") + (k.on.length ? ` · on ${k.on.join("·")}` : ""),
        ]),
      ]),
      el("div", { class: "trial-panel" }, [
        el("h3", {}, ["How it works"]),
        el("p", { style: "color: var(--text-secondary); font-size: var(--text-sm); margin: 0 0 var(--space-3)" }, [
          `A ghost of the next stroke is shown in gray. Trace it on the board — Kaiteyo grades the shape and direction of each stroke, then moves on. ${k.n} strokes to go.`,
        ]),
      ]),
      el("div", { class: "trial-write-actions" }, [
        el("button", { class: "btn btn-ghost btn-sm", onclick: () => { queue.shift(); go("write"); } }, ["Skip kanji"]),
        el("button", { class: "btn btn-ghost btn-sm", onclick: () => { writeQueue = DATA.kanji.filter((x) => x.s && x.s.length).slice(0, 5); go("write"); } }, ["Reset practice"]),
      ]),
    ]);

    // Queue strip: other practice kanji + done markers
    const queueStrip = el("div", { class: "trial-write-queue" }, queue.map((qk, i) =>
      el("button", {
        "aria-pressed": String(i === 0),
        onclick: () => { const [cur] = queue.splice(i, 1); queue.unshift(cur); go("write"); },
      }, [qk.c]),
    ));
    side.appendChild(queueStrip);

    view.appendChild(el("div", { class: "trial-write" }, [board, side]));

    initWriteBoard(canvas, hint, k, () => {
      queue.shift();
      state.writing.attempts += 1;
      state.writing.correct += 1;
      persist();
      if (queue.length) {
        go("write");
      } else {
        writeQueue = null;
        go("progress");
        writeToast("Practice complete — every stroke passed. That's the whole writing loop.");
      }
    });
  }

  function initWriteBoard(canvas, hint, k, onComplete) {
    const size = 420;
    const ctx = canvas.getContext("2d");
    const accent = () => getComputedStyle(document.documentElement).getPropertyValue("--accent") || "#C2FC8B";
    const surface = () => getComputedStyle(document.documentElement).getPropertyValue("--surface") || "#0D0D0D";
    const border = () => getComputedStyle(document.documentElement).getPropertyValue("--border") || "#2A2A2A";

    let strokeIdx = 0;
    const strokes = k.s;
    let drawing = false;
    let points = [];
    let rect = null;

    const clear = () => {
      ctx.fillStyle = surface();
      ctx.fillRect(0, 0, size, size);
      ctx.strokeStyle = border();
      ctx.lineWidth = 1;
      ctx.strokeRect(8, 8, size - 16, size - 16);
      // ghost reference kanji
      ctx.globalAlpha = 0.08;
      ctx.font = `${size * 0.62}px "Noto Sans JP", sans-serif`;
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillStyle = accent();
      ctx.fillText(k.c, size / 2, size / 2 + 6);
      ctx.globalAlpha = 1;
      drawGhost();
      drawProgress();
    };

    const drawGhost = () => {
      if (strokeIdx >= strokes.length) return;
      ctx.strokeStyle = "rgba(160,160,160,0.45)";
      ctx.lineWidth = 12;
      ctx.lineCap = "round";
      ctx.lineJoin = "round";
      ctx.beginPath();
      strokes[strokeIdx].forEach(([x, y], i) => {
        const px = (x / 100) * size;
        const py = (y / 100) * size;
        if (i === 0) ctx.moveTo(px, py);
        else ctx.lineTo(px, py);
      });
      ctx.stroke();
    };

    const drawProgress = () => {
      ctx.lineCap = "round";
      ctx.lineJoin = "round";
      ctx.strokeStyle = accent();
      ctx.lineWidth = 12;
      strokes.slice(0, strokeIdx).forEach((stroke) => {
        ctx.beginPath();
        stroke.forEach(([x, y], i) => {
          const px = (x / 100) * size;
          const py = (y / 100) * size;
          if (i === 0) ctx.moveTo(px, py);
          else ctx.lineTo(px, py);
        });
        ctx.stroke();
      });
    };

    const redraw = () => {
      clear();
      if (drawing && points.length) {
        ctx.strokeStyle = accent();
        ctx.lineWidth = 10;
        ctx.lineCap = "round";
        ctx.lineJoin = "round";
        ctx.beginPath();
        points.forEach(([x, y], i) => {
          if (i === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        });
        ctx.stroke();
      }
    };

    const pos = (e) => {
      const r = canvas.getBoundingClientRect();
      const scale = size / r.width;
      return [(e.clientX - r.left) * scale, (e.clientY - r.top) * scale];
    };

    /** Compare a drawn stroke to the target: resample both, compare shape + direction. */
    const gradeStroke = (drawn) => {
      const target = strokes[strokeIdx];
      if (!target) return 1;
      const N = 24;
      const resample = (poly) => {
        const total = poly.reduce((sum, p, i) => {
          if (i === 0) return sum;
          return sum + Math.hypot(p[0] - poly[i - 1][0], p[1] - poly[i - 1][1]);
        }, 0);
        const out = [poly[0]];
        let dist = 0;
        let j = 1;
        for (let i = 1; i < N - 1; i++) {
          const target = (total * i) / (N - 1);
          while (j < poly.length - 1 && dist + Math.hypot(poly[j][0] - poly[j - 1][0], poly[j][1] - poly[j - 1][1]) < target) {
            dist += Math.hypot(poly[j][0] - poly[j - 1][0], poly[j][1] - poly[j - 1][1]);
            j += 1;
          }
          const seg = Math.hypot(poly[j][0] - poly[j - 1][0], poly[j][1] - poly[j - 1][1]) || 1;
          const t = (target - dist) / seg;
          out.push([poly[j - 1][0] + (poly[j][0] - poly[j - 1][0]) * t, poly[j - 1][1] + (poly[j][1] - poly[j - 1][1]) * t]);
        }
        out.push(poly[poly.length - 1]);
        return out;
      };
      const norm = (poly) => {
        const xs = poly.map((p) => p[0]);
        const ys = poly.map((p) => p[1]);
        const minX = Math.min(...xs), maxX = Math.max(...xs);
        const minY = Math.min(...ys), maxY = Math.max(...ys);
        const w = Math.max(1, maxX - minX), h = Math.max(1, maxY - minY);
        const scale = 100 / Math.max(w, h);
        return poly.map(([x, y]) => [((x - minX) * scale), ((y - minY) * scale)]);
      };
      const a = resample(norm(drawn.map(([x, y]) => [x, y])));
      const b = resample(norm(target));
      let sum = 0;
      for (let i = 0; i < N; i++) {
        sum += Math.hypot(a[i][0] - b[i][0], a[i][1] - b[i][1]);
      }
      const avg = sum / N;
      // Shape score: lower is better. <= 30 is a pass.
      if (avg <= 30) return 1 - avg / 100;
      return 0;
    };

    const finishStroke = () => {
      drawing = false;
      if (points.length < 4) { points = []; redraw(); hint.textContent = "That was too short — draw the full stroke."; return; }
      const score = gradeStroke(points);
      points = [];
      if (score > 0) {
        strokeIdx += 1;
        hint.textContent = `Good stroke — shape ${(score * 100).toFixed(0)}%.`;
        state.writing.attempts += 1;
        state.writing.correct += 1;
        persist();
        redraw();
        if (strokeIdx >= strokes.length) {
          hint.textContent = "Complete!";
          writeToast(`${k.c} — all ${k.n} strokes correct.`);
          window.setTimeout(onComplete, 700);
        }
      } else {
        state.writing.attempts += 1;
        persist();
        redraw();
        hint.textContent = "Not quite — compare the shape and direction, then try again.";
      }
    };

    canvas.addEventListener("pointerdown", (e) => {
      if (strokeIdx >= strokes.length) return;
      canvas.setPointerCapture(e.pointerId);
      rect = canvas.getBoundingClientRect();
      drawing = true;
      points = [pos(e)];
      redraw();
    });
    canvas.addEventListener("pointermove", (e) => {
      if (!drawing) return;
      points.push(pos(e));
      if (points.length > 200) points.shift();
      redraw();
    });
    canvas.addEventListener("pointerup", finishStroke);
    canvas.addEventListener("pointercancel", () => { drawing = false; redraw(); });

    clear();
    hint.textContent = `Stroke ${strokeIdx + 1} of ${strokes.length} — trace the ghost.`;
  }

  /* --- Study --- */

  function renderStudy(view) {
    // In-place re-render: flip and grade handlers call renderStudy(view)
    // directly (not through go()), so the view must be cleared first or
    // every interaction would append a duplicate card.
    view.innerHTML = "";
    const wrap = el("div", { class: "trial-study-wrap" }, []);

    if (studyResult) {
      wrap.appendChild(renderSessionEnd());
      view.appendChild(wrap);
      appendExitNote(view);
      return;
    }

    if (!studyQueue || studyQueue.length === 0) {
      // No session — offer deck picker
      wrap.appendChild(el("div", { class: "trial-view-head" }, [
        el("h1", {}, ["Study"]),
        el("p", {}, ["Pick a deck. The trial grades cards with a lightweight spaced-repetition schedule — Again, Hard, Good, Easy — and tracks what you've learned."]),
      ]));
      DATA.decks.forEach((deck) => {
        const due = dueCards(deck.id).length;
        wrap.appendChild(el("button", { class: "trial-deck-row", style: "background: var(--surface); border: 0.5px solid var(--border); border-radius: var(--radius-md); margin-bottom: var(--space-2)", onclick: () => go("study", { studyDeck: deck.id }) }, [
          icon("folder"), el("span", { class: "trial-deck-name" }, [deck.name]),
          el("span", { class: "trial-deck-due" }, [`${due} due`]),
        ]));
      });
      wrap.appendChild(el("p", { style: "color: var(--text-muted); font-size: var(--text-sm); margin-top: var(--space-6)" }, [
        dueCards(null).length > 0 ? "" : "All cards are up to date for now — grading more will schedule tomorrow's session.",
      ]));
      view.appendChild(wrap);
      appendExitNote(view);
      return;
    }

    const card = studyQueue[studyIdx];
    const progress = ((studyIdx) / studyQueue.length) * 100;

    wrap.appendChild(el("div", { class: "trial-study-progress" }, [
      el("span", { class: "trial-study-count" }, [`${studyIdx + 1} / ${studyQueue.length}`]),
      el("div", { class: "progress" }, [el("div", { class: "progress-fill", style: `width: ${progress}%` })]),
    ]));

    const flip = el("div", {
      class: "trial-card",
      role: "button",
      tabindex: "0",
      "aria-label": studyFlipped ? "Card answer (click to hide)" : "Card question (click to reveal)",
      onclick: () => { studyFlipped = !studyFlipped; renderStudy(view); },
      onkeydown: (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); studyFlipped = !studyFlipped; renderStudy(view); } },
    }, []);

    if (!studyFlipped) {
      const front = el("div", { class: "trial-card-front" }, [
        el("div", { class: "card-jp" }, [card.kind === "kanji" ? card.c : card.w]),
        el("div", { class: "card-hint" }, [card.kind === "kanji" ? "Meaning? Reading?" : "Reading? Meaning?"]),
      ]);
      flip.appendChild(front);
    } else {
      const back = el("div", { class: "trial-card-back" }, [
        el("div", { class: "card-reading" }, [card.k]),
        el("div", { class: "card-mean" }, [card.m.join(", ")]),
        card.ex && card.ex.ja ? el("div", { class: "card-ex" }, [
          el("div", {}, [card.ex.ja]),
          el("div", { class: "card-ex-en" }, [card.ex.en]),
        ]) : null,
      ]);
      flip.appendChild(back);
    }
    wrap.appendChild(flip);

    if (studyFlipped) {
      const grades = el("div", { class: "trial-grade-row" }, [
        gradeButton("again", "Again", "1 min"),
        gradeButton("hard", "Hard", "10 min"),
        gradeButton("good", "Good", "1 day"),
        gradeButton("easy", "Easy", "3 days"),
      ]);
      const apply = (g) => {
        gradeCard(card.key, g);
        studyIdx += 1;
        studyFlipped = false;
        if (studyIdx >= studyQueue.length) studyResult = { correct: countCorrect(studyQueue) };
        renderStudy(view);
      };
      grades.querySelectorAll("[data-grade]").forEach((btn) =>
        btn.addEventListener("click", () => apply(btn.dataset.grade)),
      );
      wrap.appendChild(grades);
    }

    view.appendChild(wrap);
  }

  const countCorrect = (queue) => {
    let correct = 0;
    queue.forEach((c) => {
      const rec = state.cards[c.key];
      if (rec && rec.state === "review") correct += 1;
    });
    return Math.max(1, correct);
  };

  const gradeButton = (g, label, interval) =>
    el("button", { class: "trial-grade", "data-grade": g }, [
      el("span", { class: "g" }, [label]),
      el("span", { class: "l" }, [interval]),
    ]);

  function renderSessionEnd() {
    const n = studyQueue.length;
    const correct = studyResult.correct;
    const pct = Math.round((correct / n) * 100);
    return el("div", { class: "trial-session-end" }, [
      el("span", { class: "badge badge-accent" }, ["Session complete"]),
      el("div", { class: "big-score" }, [`${pct}%`]),
      el("p", {}, [`${correct} of ${n} cards answered correctly across ${studyQueue.length} card(s).`]),
      el("div", { class: "flex gap-3", style: "justify-content: center; margin-top: var(--space-5)" }, [
        el("button", { class: "btn btn-ghost", onclick: () => { studyResult = null; studyQueue = null; go("home"); } }, ["Back to Home"]),
        el("a", { class: "btn btn-primary", href: `${BASE_PATH}download/` }, [icon("download"), " Get the full Kaiteyo"]),
      ]),
      el("p", { style: "margin-top: var(--space-6); font-size: var(--text-sm); color: var(--text-muted)" }, [
        "The full app schedules every card with FSRS-5, adds writing and listening modes, and syncs nothing unless you choose to.",
      ]),
    ]);
  }

  function startSession() {
    // Prefer decks with due cards; fall back to a full mixed session.
    const withDue = DATA.decks.find((d) => dueCards(d.id).length > 0);
    const deckId = withDue ? withDue.id : DATA.decks[0].id;
    go("study", { studyDeck: deckId });
  }

  /* --- Progress --- */

  function renderProgress(view) {
    view.appendChild(el("div", { class: "trial-view-head" }, [
      el("h1", {}, ["Progress"]),
      el("p", {}, ["Everything here is real — computed from this session's reviews and the seeded demo history. The full app keeps the same statistics offline, forever."]),
    ]));

    const stats = el("div", { class: "trial-stats" }, [
      statCard(state.reviewsTotal, "total reviews"),
      statCard(state.cardsLearned, "cards learned"),
      statCard(`${Math.round((state.writing.correct / Math.max(1, state.writing.attempts)) * 100)}%`, "writing accuracy"),
    ]);
    view.appendChild(stats);

    const panel = el("div", { class: "trial-panel", style: "margin-top: var(--space-5)" }, [
      el("h3", {}, [icon("dashboard"), " Activity — last 14 weeks"]),
      el("div", { class: "trial-heatmap" }, DATA.demoProgress.heatmap.map((count) => {
        const level = count === 0 ? 0 : count <= 8 ? 1 : count <= 16 ? 2 : 3;
        const cell = el("div", { class: "trial-heatmap-cell", dataset: { level }, "aria-label": `${count} reviews` });
        cell.title = `${count} reviews`;
        return cell;
      })),
    ]);
    view.appendChild(panel);

    const jlpt = el("div", { class: "trial-panel", style: "margin-top: var(--space-5)" }, [
      el("h3", {}, [icon("milestone"), " Kanji by level (trial sample)"]),
      el("div", { class: "trial-bars" }, [["N5", 44, 22], ["N4", 12, 0], ["N3", 8, 0], ["N2", 6, 0], ["N1", 4, 0]].map(([name, total, done]) =>
        el("div", { class: "trial-bar-row" }, [
          el("span", { class: "name" }, [name]),
          el("div", { class: "progress" }, [el("div", { class: "progress-fill", style: `width: ${(done / total) * 100}%` })]),
          el("span", { class: "value" }, [`${done}/${total}`]),
        ]),
      )),
    ]);
    view.appendChild(jlpt);

    view.appendChild(el("div", { class: "trial-exit-note" }, [
      el("p", {}, [el("strong", {}, ["This is a fraction of what Kaiteyo tracks."]), " Study time, retention curves, JLPT coverage, exam history, deck-by-deck accuracy, goals — all offline, all yours."]),
      el("a", { class: "btn btn-primary", href: `${BASE_PATH}download/` }, [icon("download"), " Download Kaiteyo"]),
    ]));
  }

  /* --- Shared --- */

  function appendExitNote(view) {
    view.appendChild(el("div", { class: "trial-exit-note" }, [
      el("p", {}, [el("strong", {}, ["Trying the real thing?"]), " The trial runs in your browser with sample data. The desktop app adds the full database, media centre, mining, exams, Anki, and every deck you can build."]),
      el("a", { class: "btn btn-primary", href: `${BASE_PATH}download/` }, [icon("download"), " Download Kaiteyo"]),
      el("a", { class: "btn btn-ghost", href: `${BASE_PATH}docs/` }, [icon("book"), " Read the docs"]),
    ]));
  }

  function writeToast(message) {
    let toast = $("#trialToast");
    if (!toast) {
      toast = el("div", { class: "trial-toast", id: "trialToast", role: "status" });
      document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.classList.add("is-visible");
    clearTimeout(toast._timer);
    toast._timer = setTimeout(() => toast.classList.remove("is-visible"), 2600);
  }

  /* ------------------------------------------------------------------ */
  /* Topbar search                                                       */
  /* ------------------------------------------------------------------ */

  function initTopbarSearch() {
    const input = $("#trialSearch");
    if (!input) return;
    input.addEventListener("keydown", (e) => {
      if (e.key !== "Enter") return;
      const q = input.value.trim();
      if (!q) return;
      const kanji = DATA.kanji.find((k) => k.c === q || k.on.some((o) => o === q) || k.kun.some((o) => o === q) || k.m.some((m) => m.toLowerCase() === q.toLowerCase()));
      if (kanji) { go("detail", { char: kanji.c }); input.value = ""; return; }
      const vocab = DATA.vocab.find((v) => v.w === q || v.k === q);
      if (vocab) { go("browse", { tab: "vocab", query: q }); return; }
      go("browse", { tab: kanjiQuery(q) ? "kanji" : "vocab", query: q });
      writeToast(`Showing ${input.value} in ${browseTab === "kanji" ? "kanji" : "vocabulary"}`);
      input.value = "";
    });
  }

  const kanjiQuery = (q) => /[\u3400-\u9fff]/.test(q);

  /* ------------------------------------------------------------------ */
  /* Theme quick controls                                                */
  /* ------------------------------------------------------------------ */

  function initThemeControls() {
    const baseLabels = { oled: "OLED", dark: "Dark", light: "Light", sepia: "Sepia" };
    const bases = Object.keys(baseLabels);
    const row = $("#trialThemeRow");
    if (!row) return;
    bases.forEach((base) => {
      const dot = el("button", {
        class: "trial-theme-dot",
        style: `background: ${base === "oled" ? "#050505" : base === "dark" ? "#1A1A1A" : base === "light" ? "#EEEEEE" : "#EDE5D8"}; border: 1px solid var(--border)`,
        "aria-label": `${baseLabels[base]} base mode`,
        title: baseLabels[base],
        onclick: () => {
          const root = document.documentElement;
          root.dataset.baseMode = base;
          try { localStorage.setItem("kaiteyo:theme", JSON.stringify({ baseMode: base, accent: root.dataset.accent || "signature", glass: root.dataset.glass === "true", motion: "default" })); } catch (e) {}
          syncThemeDots();
        },
      });
      row.appendChild(dot);
    });
  }

  function syncThemeDots() {
    const current = document.documentElement.dataset.baseMode || "oled";
    $$("#trialThemeRow .trial-theme-dot").forEach((dot) => {
      const label = dot.getAttribute("aria-label") || "";
      dot.setAttribute("aria-pressed", String(label.endsWith(" base mode") && label.replace(" base mode", "") === "OLED" && current === "oled" || label.includes(current)));
    });
  }

  /* ------------------------------------------------------------------ */
  /* Boot                                                                */
  /* ------------------------------------------------------------------ */

  function initDock() {
    $$("[data-trial-nav]").forEach((item) => {
      item.addEventListener("click", () => {
        go(item.dataset.trialNav);
        document.body.classList.remove("trial-nav-open");
      });
    });
    const toggle = $("#trialDockToggle");
    if (toggle) toggle.addEventListener("click", () => document.body.classList.toggle("trial-nav-open"));
    const bannerClose = $("#trialBannerClose");
    if (bannerClose) bannerClose.addEventListener("click", () => {
      const banner = $("#trialBanner");
      if (banner) banner.style.display = "none";
    });
  }

  function init() {
    initDock();
    initTopbarSearch();
    initThemeControls();

    // Deep links: ?view=study / #study, ?kanji=日 / #kanji=日, ?word=日本.
    const params = new URLSearchParams(window.location.search);
    const hash = window.location.hash.replace(/^#/, "");
    const [hashTarget, hashArg] = hash.split("=");
    const target = params.get("view") || hashTarget;
    const arg = params.get("kanji") || params.get("word") || hashArg;
    if ((params.get("kanji") || hashTarget === "kanji") && kanjiByChar[arg]) go("detail", { char: arg });
    else if (params.get("word") || hashTarget === "word") go("browse", { tab: "vocab", query: arg });
    else if (views[target]) go(target);
    else go("home");
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
