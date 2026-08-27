#!/usr/bin/env python3
"""Generate a single, gorgeous, interactive-looking SVG landing page for the Kaiteyo README.

One seamless dark canvas with animated glows, pulsing accents, interactive-looking
buttons, a terminal block with blinking cursor, and the full product story.
"""

import pathlib

OUT = pathlib.Path(__file__).resolve().parent.parent / "assets" / "readme"
OUT.mkdir(parents=True, exist_ok=True)

# ── Kaiteyo design tokens ──────────────────────────────────────────
BG = "#050505"
SURFACE = "#0D0D0D"
SE = "#101010"
SI = "#1A1A1A"
BORDER = "#1E1E1E"
BORDER_L = "#2A2A2A"
T1 = "#F0F0F0"
T2 = "#A0A0A0"
T3 = "#606060"
T4 = "#404040"
AC = "#C2FC8B"
ACD = "#9CE85E"
AB = "#FEAB57"
ABD = "#FD8A2E"
BL = "#7BC8FF"
PU = "#A78BFA"
RD = "#FF6B6B"
W = 1200

svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} 3200" width="{W}" height="3200">
  <defs>
    <!-- Gradients -->
    <linearGradient id="acG" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{AC}"/><stop offset="100%" stop-color="{ACD}"/>
    </linearGradient>
    <linearGradient id="abG" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{AB}"/><stop offset="100%" stop-color="{ABD}"/>
    </linearGradient>
    <linearGradient id="blG" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{BL}"/>
    </linearGradient>
    <linearGradient id="grD" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="{AC}" stop-opacity="0"/><stop offset="100%" stop-color="{AC}" stop-opacity="0.3"/>
    </linearGradient>
    <linearGradient id="wfLine" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{AC}"/><stop offset="100%" stop-color="{AB}"/>
    </linearGradient>

    <!-- Glow filters -->
    <filter id="gl"><feGaussianBlur stdDeviation="10" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
    <filter id="sg"><feGaussianBlur stdDeviation="4" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
    <filter id="bg"><feGaussianBlur stdDeviation="50" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
    <filter id="cg"><feGaussianBlur stdDeviation="20" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
  </defs>

  <!-- ═══════════════════════════════════════════════════════════ -->
  <!-- CANVAS BACKGROUND                                          -->
  <!-- ═══════════════════════════════════════════════════════════ -->
  <rect width="{W}" height="3200" fill="{BG}"/>

  <!-- Ambient glow orbs -->
  <ellipse cx="{W//2}" cy="200" rx="400" ry="200" fill="{AC}" opacity="0.03" filter="url(#bg)">
    <animate attributeName="opacity" values="0.03;0.06;0.03" dur="4s" repeatCount="indefinite"/>
  </ellipse>
  <ellipse cx="200" cy="800" rx="300" ry="300" fill="{AB}" opacity="0.02" filter="url(#bg)">
    <animate attributeName="opacity" values="0.02;0.04;0.02" dur="6s" repeatCount="indefinite"/>
  </ellipse>
  <ellipse cx="1000" cy="1600" rx="350" ry="250" fill="{BL}" opacity="0.02" filter="url(#bg)">
    <animate attributeName="opacity" values="0.02;0.035;0.02" dur="5s" repeatCount="indefinite"/>
  </ellipse>

  <!-- ═══════════════════════════════════════════════════════════ -->
  <!-- HERO SECTION (0–450)                                       -->
  <!-- ═══════════════════════════════════════════════════════════ -->

  <!-- Top accent line -->
  <rect x="0" y="0" width="{W}" height="2" fill="url(#acG)" opacity="0.8">
    <animate attributeName="opacity" values="0.8;1;0.8" dur="3s" repeatCount="indefinite"/>
  </rect>

  <!-- Logo mark with pulse -->
  <g transform="translate({W//2 - 30}, 80)" filter="url(#gl)">
    <circle cx="30" cy="30" r="50" fill="{AC}" opacity="0.06">
      <animate attributeName="r" values="50;58;50" dur="3s" repeatCount="indefinite"/>
      <animate attributeName="opacity" values="0.06;0.1;0.06" dur="3s" repeatCount="indefinite"/>
    </circle>
    <path d="M12,5 Q28,3 38,16 Q44,24 42,34 Q41,38 38,42"
          stroke="{AC}" stroke-width="4.5" fill="none" stroke-linecap="round"/>
    <path d="M14,22 Q28,17 40,26"
          stroke="{AC}" stroke-width="2.5" fill="none" stroke-linecap="round" opacity="0.7"/>
    <circle cx="42" cy="34" r="2.5" fill="{AB}"/>
  </g>

  <!-- Title -->
  <text x="{W//2}" y="185" text-anchor="middle"
        font-family="Inter,system-ui,sans-serif" font-size="52" font-weight="700"
        fill="{T1}" letter-spacing="-0.03em">Kaiteyo</text>

  <!-- Japanese -->
  <text x="{W//2}" y="222" text-anchor="middle"
        font-family="'Noto Sans JP',Hiragino Sans,sans-serif" font-size="22"
        fill="{T3}" letter-spacing="0.1em">書いてよ</text>

  <!-- Tagline -->
  <text x="{W//2}" y="265" text-anchor="middle"
        font-family="Inter,system-ui,sans-serif" font-size="17"
        fill="{T2}">Write it. Practice. Master it.</text>

  <text x="{W//2}" y="292" text-anchor="middle"
        font-family="Inter,system-ui,sans-serif" font-size="13"
        fill="{T4}">A premium, cross-platform Japanese learning application</text>

  <!-- Badge row -->
  <g transform="translate({W//2 - 300}, 320)">
    <rect x="0" y="0" width="90" height="28" rx="6" fill="{SE}" stroke="{BORDER_L}"/>
    <text x="45" y="18" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="11" fill="{AC}">v2.2.1</text>
    <rect x="100" y="0" width="90" height="28" rx="6" fill="{SE}" stroke="{BORDER_L}"/>
    <text x="145" y="18" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="11" fill="{AB}">GPL-3.0</text>
    <rect x="210" y="0" width="280" height="28" rx="6" fill="{SE}" stroke="{BORDER_L}"/>
    <text x="350" y="18" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T3}">Windows · macOS · Linux · Android · iOS</text>
    <rect x="510" y="0" width="100" height="28" rx="6" fill="{SE}" stroke="#7F52FF" stroke-opacity="0.4"/>
    <text x="560" y="18" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="11" fill="#7F52FF">Kotlin 2.1</text>
  </g>

  <!-- Navigation pills -->
  <g transform="translate({W//2 - 260}, 390)">
    <rect x="0" y="0" width="80" height="32" rx="16" fill="{AC}" fill-opacity="0.12" stroke="{AC}" stroke-opacity="0.3"/>
    <text x="40" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{AC}">How it works</text>
    <rect x="90" y="0" width="70" height="32" rx="16" fill="{SI}" stroke="{BORDER_L}"/>
    <text x="125" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Study</text>
    <rect x="170" y="0" width="100" height="32" rx="16" fill="{SI}" stroke="{BORDER_L}"/>
    <text x="220" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Desktop Suite</text>
    <rect x="280" y="0" width="70" height="32" rx="16" fill="{SI}" stroke="{BORDER_L}"/>
    <text x="315" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Mobile</text>
    <rect x="360" y="0" width="80" height="32" rx="16" fill="{SI}" stroke="{BORDER_L}"/>
    <text x="400" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Downloads</text>
    <rect x="450" y="0" width="80" height="32" rx="16" fill="{SI}" stroke="{BORDER_L}"/>
    <text x="490" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Develop</text>
  </g>


  <!-- ═══════════════════════════════════════════════════════════ -->
  <!-- WORKFLOW SECTION (450–850)                                 -->
  <!-- ═══════════════════════════════════════════════════════════ -->
  <rect x="60" y="480" width="{W-120}" height="340" rx="16" fill="{SURFACE}" stroke="{BORDER_L}"/>

  <text x="100" y="520" font-family="Inter,system-ui,sans-serif" font-size="15" font-weight="600" fill="{AC}">▸ How Kaiteyo works</text>

  <!-- Step 1 -->
  <g transform="translate(100, 550)">
    <rect width="230" height="220" rx="12" fill="{SE}" stroke="{BORDER_L}"/>
    <rect width="230" height="3" rx="12" fill="{AC}" opacity="0.5"/>
    <text x="20" y="40" font-size="28">📖</text>
    <text x="55" y="40" font-family="Inter,system-ui,sans-serif" font-size="15" font-weight="600" fill="{T1}">Read or Watch</text>
    <text x="20" y="70" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T2}">Japanese content in the</text>
    <text x="20" y="87" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T2}">built-in browser, media</text>
    <text x="20" y="104" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T2}">player, or reading env.</text>
    <rect x="20" y="130" width="75" height="26" rx="6" fill="{SI}" stroke="{BORDER_L}"/>
    <text x="57" y="147" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="10" fill="{T3}">Browser</text>
    <rect x="105" y="130" width="60" height="26" rx="6" fill="{SI}" stroke="{BORDER_L}"/>
    <text x="135" y="147" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="10" fill="{T3}">Media</text>
    <rect x="175" y="130" width="40" height="26" rx="6" fill="{SI}" stroke="{BORDER_L}"/>
    <text x="195" y="147" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="10" fill="{T3}">EPUB</text>
  </g>

  <!-- Arrow 1 -->
  <g transform="translate(340, 650)">
    <line x1="0" y1="0" x2="60" y2="0" stroke="{AC}" stroke-width="2" opacity="0.4"/>
    <polygon points="60,-5 70,0 60,5" fill="{AC}" opacity="0.4"/>
  </g>

  <!-- Step 2 -->
  <g transform="translate(420, 550)">
    <rect width="230" height="220" rx="12" fill="{SE}" stroke="{BORDER_L}"/>
    <rect width="230" height="3" rx="12" fill="{AC}" opacity="0.5"/>
    <text x="20" y="40" font-size="28">🔍</text>
    <text x="55" y="40" font-family="Inter,system-ui,sans-serif" font-size="15" font-weight="600" fill="{T1}">Hover a Word</text>
    <text x="20" y="70" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T2}">Instant dictionary popup</text>
    <text x="20" y="87" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T2}">with readings, definitions,</text>
    <text x="20" y="104" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T2}">and TTS pronunciation.</text>
    <rect x="20" y="130" width="65" height="26" rx="6" fill="{AC}" fill-opacity="0.12" stroke="{AC}" stroke-opacity="0.3"/>
    <text x="52" y="147" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="10" fill="{AC}">Yomitan</text>
    <rect x="95" y="130" width="60" height="26" rx="6" fill="{SI}" stroke="{BORDER_L}"/>
    <text x="125" y="147" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="10" fill="{T3}">JMdict</text>
  </g>

  <!-- Arrow 2 -->
  <g transform="translate(660, 650)">
    <line x1="0" y1="0" x2="60" y2="0" stroke="{AB}" stroke-width="2" opacity="0.4"/>
    <polygon points="60,-5 70,0 60,5" fill="{AB}" opacity="0.4"/>
  </g>

  <!-- Step 3 -->
  <g transform="translate(740, 550)">
    <rect width="230" height="220" rx="12" fill="{SE}" stroke="{BORDER_L}"/>
    <rect width="230" height="3" rx="12" fill="{AB}" opacity="0.5"/>
    <text x="20" y="40" font-size="28">✂️</text>
    <text x="55" y="40" font-family="Inter,system-ui,sans-serif" font-size="15" font-weight="600" fill="{T1}">Mine a Sentence</text>
    <text x="20" y="70" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T2}">Card with screenshot,</text>
    <text x="20" y="87" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T2}">audio clip, and timestamp</text>
    <text x="20" y="104" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T2}">lands in your SRS queue.</text>
    <rect x="20" y="130" width="75" height="26" rx="6" fill="{AB}" fill-opacity="0.12" stroke="{AB}" stroke-opacity="0.3"/>
    <text x="57" y="147" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="10" fill="{AB}">Mining</text>
    <rect x="105" y="130" width="55" height="26" rx="6" fill="{SI}" stroke="{BORDER_L}"/>
    <text x="132" y="147" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="10" fill="{T3}">Anki</text>
  </g>

  <!-- Arrow 3 -->
  <g transform="translate(980, 650)">
    <line x1="0" y1="0" x2="60" y2="0" stroke="{BL}" stroke-width="2" opacity="0.4"/>
    <polygon points="60,-5 70,0 60,5" fill="{BL}" opacity="0.4"/>
  </g>

  <!-- Step 4 -->
  <g transform="translate(1060, 550)">
    <rect width="110" height="220" rx="12" fill="{SE}" stroke="{BORDER_L}"/>
    <rect width="110" height="3" rx="12" fill="{BL}" opacity="0.5"/>
    <text x="20" y="40" font-size="28">🧠</text>
    <text x="55" y="40" font-family="Inter,system-ui,sans-serif" font-size="15" font-weight="600" fill="{T1}">Review</text>
    <text x="20" y="70" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Spaced repetition</text>
    <text x="20" y="87" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">with FSRS-5.</text>
    <text x="20" y="104" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Jump back to the</text>
    <text x="20" y="121" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">exact scene.</text>
    <rect x="20" y="140" width="70" height="26" rx="6" fill="{BL}" fill-opacity="0.12" stroke="{BL}" stroke-opacity="0.3"/>
    <text x="55" y="157" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="10" fill="{BL}">FSRS-5</text>
  </g>

  <!-- Bottom note -->
  <text x="{W//2}" y="805" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T4}">
    Everything works offline. Your data is yours — import, export, Anki, backup, sync.
  </text>


  <!-- ═══════════════════════════════════════════════════════════ -->
  <!-- CORE STUDY ENGINE (870–1450)                               -->
  <!-- ═══════════════════════════════════════════════════════════ -->
  <text x="{W//2}" y="910" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="18" font-weight="600" fill="{AC}">▸ Core Study Engine</text>
  <text x="{W//2}" y="935" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T4}">All platforms — Desktop · Android · iOS</text>

  <rect x="60" y="955" width="{W-120}" height="480" rx="16" fill="{SURFACE}" stroke="{BORDER_L}"/>
  <rect x="60" y="955" width="{W-120}" height="3" rx="16" fill="{AC}" opacity="0.4"/>

  <!-- Features grid: 2 columns -->
  <g font-family="Inter,system-ui,sans-serif">
    <!-- Column 1 -->
    <text x="100" y="1000" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1000" font-size="13" font-weight="500" fill="{T1}">Kanji &amp; Kana</text>
    <text x="260" y="1000" font-size="12" fill="{T3}">JLPT N5–N1 + school-grade decks</text>

    <text x="100" y="1035" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1035" font-size="13" font-weight="500" fill="{T1}">Vocabulary</text>
    <text x="260" y="1035" font-size="12" fill="{T3}">Readings, meanings, furigana, examples</text>

    <text x="100" y="1070" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1070" font-size="13" font-weight="500" fill="{T1}">Writing Practice</text>
    <text x="260" y="1070" font-size="12" fill="{T3}">Stroke-order, brush canvas, evaluation</text>

    <text x="100" y="1105" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1105" font-size="13" font-weight="500" fill="{T1}">Spaced Repetition</text>
    <text x="260" y="1105" font-size="12" fill="{T3}">FSRS-5, custom intervals, daily limits</text>

    <text x="100" y="1140" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1140" font-size="13" font-weight="500" fill="{T1}">Deck Management</text>
    <text x="260" y="1140" font-size="12" fill="{T3}">Create, edit, archive, bulk actions</text>

    <text x="100" y="1175" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1175" font-size="13" font-weight="500" fill="{T1}">Radical Search</text>
    <text x="260" y="1175" font-size="12" fill="{T3}">6000+ characters, dictionary-backed</text>

    <!-- Column 2 -->
    <text x="640" y="1000" font-size="13" fill="{AC}">✅</text>
    <text x="664" y="1000" font-size="13" font-weight="500" fill="{T1}">Text Analysis</text>
    <text x="790" y="1000" font-size="12" fill="{T3}">Word-by-word (Ichiran-style)</text>

    <text x="640" y="1035" font-size="13" fill="{AC}">✅</text>
    <text x="664" y="1035" font-size="13" font-weight="500" fill="{T1}">Statistics</text>
    <text x="790" y="1035" font-size="12" fill="{T3}">Heatmap, curves, goals, exams</text>

    <text x="640" y="1070" font-size="13" fill="{AC}">✅</text>
    <text x="664" y="1070" font-size="13" font-weight="500" fill="{T1}">Anki Import/Export</text>
    <text x="790" y="1070" font-size="12" fill="{T3}">.apkg on all platforms</text>

    <text x="640" y="1105" font-size="13" fill="{AC}">✅</text>
    <text x="664" y="1105" font-size="13" font-weight="500" fill="{T1}">Backup &amp; Restore</text>
    <text x="790" y="1105" font-size="12" fill="{T3}">Profile archives, settings</text>

    <text x="640" y="1140" font-size="13" fill="{AB}">🚧</text>
    <text x="664" y="1140" font-size="13" font-weight="500" fill="{T1}">Sync</text>
    <text x="790" y="1140" font-size="12" fill="{T3}">GitHub device-flow + private-gist</text>

    <text x="640" y="1175" font-size="13" fill="{AB}">🚧</text>
    <text x="664" y="1175" font-size="13" font-weight="500" fill="{T1}">Grammar</text>
    <text x="790" y="1175" font-size="12" fill="{T3}">Explanation-first with starter deck</text>
  </g>

  <!-- Separator line -->
  <line x1="100" y1="1210" x2="{W-100}" y2="1210" stroke="{BORDER}" stroke-width="1"/>

  <!-- Bottom accent strip -->
  <rect x="60" y="1420" width="{W-120}" height="3" rx="16" fill="url(#acG)" opacity="0.2"/>


  <!-- ═══════════════════════════════════════════════════════════ -->
  <!-- DESKTOP SUITE (1460–2100)                                  -->
  <!-- ═══════════════════════════════════════════════════════════ -->
  <text x="{W//2}" y="1500" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="18" font-weight="600" fill="{AB}">▸ Desktop Suite</text>
  <text x="{W//2}" y="1525" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T4}">Windows · macOS · Linux — the flagship experience</text>

  <rect x="60" y="1545" width="{W-120}" height="520" rx="16" fill="{SURFACE}" stroke="{BORDER_L}"/>
  <rect x="60" y="1545" width="{W-120}" height="3" rx="16" fill="{AB}" opacity="0.4"/>

  <g font-family="Inter,system-ui,sans-serif">
    <!-- Column 1 -->
    <text x="100" y="1590" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1590" font-size="13" font-weight="500" fill="{T1}">Dictionary</text>
    <text x="260" y="1590" font-size="12" fill="{T3}">Yomitan-style — ZIP/JSON, JMdict, KANJIDIC</text>

    <text x="100" y="1625" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1625" font-size="13" font-weight="500" fill="{T1}">Popup Lookup</text>
    <text x="260" y="1625" font-size="12" fill="{T3}">Hover any text → readings, mining, TTS</text>

    <text x="100" y="1660" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1660" font-size="13" font-weight="500" fill="{T1}">Media Center</text>
    <text x="260" y="1660" font-size="12" fill="{T3}">VLC / mpv — SRT/ASS/SSA/VTT subtitles</text>

    <text x="100" y="1695" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1695" font-size="13" font-weight="500" fill="{T1}">Subtitle Mining</text>
    <text x="260" y="1695" font-size="12" fill="{T3}">Cards with screenshot + audio + timestamp</text>

    <text x="100" y="1730" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1730" font-size="13" font-weight="500" fill="{T1}">Learning Browser</text>
    <text x="260" y="1730" font-size="12" fill="{T3}">Reader mode, bookmarks, lookup &amp; mining</text>

    <text x="100" y="1765" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1765" font-size="13" font-weight="500" fill="{T1}">Local API</text>
    <text x="260" y="1765" font-size="12" fill="{T3}">Bearer-token HTTP — media, mining, player</text>

    <text x="100" y="1800" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1800" font-size="13" font-weight="500" fill="{T1}">AnkiConnect</text>
    <text x="260" y="1800" font-size="12" fill="{T3}">Push mined cards, import from Anki</text>

    <text x="100" y="1835" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="1835" font-size="13" font-weight="500" fill="{T1}">Theme Studio</text>
    <text x="260" y="1835" font-size="12" fill="{T3}">HSV wheel, gradients, presets, live preview</text>

    <!-- Column 2 -->
    <text x="640" y="1590" font-size="13" fill="{AC}">✅</text>
    <text x="664" y="1590" font-size="13" font-weight="500" fill="{T1}">Onboarding</text>
    <text x="790" y="1590" font-size="12" fill="{T3}">8-step wizard — theme, accent, scale, font</text>

    <text x="640" y="1625" font-size="13" fill="{AC}">✅</text>
    <text x="664" y="1625" font-size="13" font-weight="500" fill="{T1}">Installers</text>
    <text x="790" y="1625" font-size="12" fill="{T3}">Inno Setup, DMG, AppImage/deb/rpm/Flatpak</text>

    <text x="640" y="1660" font-size="13" fill="{AC}">✅</text>
    <text x="664" y="1660" font-size="13" font-weight="500" fill="{T1}">Native Window</text>
    <text x="790" y="1660" font-size="12" fill="{T3}">Title bar, OS drag, resize, snap, rounded</text>

    <text x="640" y="1695" font-size="13" fill="{AC}">✅</text>
    <text x="664" y="1695" font-size="13" font-weight="500" fill="{T1}">Float Launcher</text>
    <text x="790" y="1695" font-size="12" fill="{T3}">Draggable bubble, snap-to-edge, modes</text>

    <text x="640" y="1730" font-size="13" fill="{AC}">✅</text>
    <text x="664" y="1730" font-size="13" font-weight="500" fill="{T1}">Overlay Sidebar</text>
    <text x="790" y="1730" font-size="12" fill="{T3}">Floats on content, 4 positions, elevated</text>

    <text x="640" y="1765" font-size="13" fill="{AB}">🚧</text>
    <text x="664" y="1765" font-size="13" font-weight="500" fill="{T1}">OCR</text>
    <text x="790" y="1765" font-size="12" fill="{T3}">Capture works; Tesseract detection</text>

    <text x="640" y="1800" font-size="13" fill="{AB}">🚧</text>
    <text x="664" y="1800" font-size="13" font-weight="500" fill="{T1}">Auto-Update</text>
    <text x="790" y="1800" font-size="12" fill="{T3}">Architecture complete; staged rollout</text>

    <text x="640" y="1835" font-size="13" fill="{AB}">🚧</text>
    <text x="664" y="1835" font-size="13" font-weight="500" fill="{T1}">Plugins</text>
    <text x="790" y="1835" font-size="12" fill="{T3}">Registry + marketplace scaffold</text>
  </g>

  <rect x="60" y="2050" width="{W-120}" height="3" rx="16" fill="url(#abG)" opacity="0.2"/>


  <!-- ═══════════════════════════════════════════════════════════ -->
  <!-- MOBILE (2100–2250)                                         -->
  <!-- ═══════════════════════════════════════════════════════════ -->
  <text x="{W//2}" y="2130" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="18" font-weight="600" fill="{BL}">▸ Mobile</text>

  <rect x="60" y="2150" width="{W-120}" height="80" rx="16" fill="{SURFACE}" stroke="{BORDER_L}"/>
  <rect x="60" y="2150" width="{W-120}" height="3" rx="16" fill="{BL}" opacity="0.3"/>

  <g font-family="Inter,system-ui,sans-serif">
    <text x="100" y="2195" font-size="13" fill="{AC}">✅</text>
    <text x="124" y="2195" font-size="13" font-weight="500" fill="{T1}">Android</text>
    <text x="230" y="2195" font-size="12" fill="{T3}">Play Store + F-Droid · Firebase, billing</text>

    <text x="640" y="2195" font-size="13" fill="{AB}">🚧</text>
    <text x="664" y="2195" font-size="13" font-weight="500" fill="{T1}">iOS</text>
    <text x="730" y="2195" font-size="12" fill="{T3}">Shared engine + shell · macOS-only builds</text>
  </g>


  <!-- ═══════════════════════════════════════════════════════════ -->
  <!-- DOWNLOADS (2280–2530)                                      -->
  <!-- ═══════════════════════════════════════════════════════════ -->
  <text x="{W//2}" y="2320" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="18" font-weight="600" fill="{AC}">▸ Downloads</text>

  <!-- 5 download cards -->
  <g transform="translate(60, 2345)">
    <!-- Windows -->
    <g>
      <rect width="216" height="160" rx="12" fill="{SE}" stroke="{BORDER_L}"/>
      <rect width="216" height="3" rx="12" fill="{AC}" opacity="0.3"/>
      <text x="20" y="35" font-family="Inter,system-ui,sans-serif" font-size="15" font-weight="600" fill="{T1}">🪟 Windows</text>
      <text x="20" y="58" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T3}">EXE · MSI · Portable ZIP</text>
      <rect x="20" y="80" width="176" height="36" rx="8" fill="{AC}" fill-opacity="0.1" stroke="{AC}" stroke-opacity="0.3"/>
      <text x="108" y="103" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" font-weight="500" fill="{AC}">Download</text>
      <!-- Pulse glow on button -->
      <rect x="20" y="80" width="176" height="36" rx="8" fill="none" stroke="{AC}" stroke-opacity="0.1">
        <animate attributeName="stroke-opacity" values="0.1;0.3;0.1" dur="2s" repeatCount="indefinite"/>
      </rect>
    </g>

    <!-- macOS -->
    <g transform="translate(236, 0)">
      <rect width="216" height="160" rx="12" fill="{SE}" stroke="{BORDER_L}"/>
      <rect width="216" height="3" rx="12" fill="{AC}" opacity="0.3"/>
      <text x="20" y="35" font-family="Inter,system-ui,sans-serif" font-size="15" font-weight="600" fill="{T1}">🍎 macOS</text>
      <text x="20" y="58" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T3}">DMG — arm64 + x64, signed</text>
      <rect x="20" y="80" width="176" height="36" rx="8" fill="{AC}" fill-opacity="0.1" stroke="{AC}" stroke-opacity="0.3"/>
      <text x="108" y="103" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" font-weight="500" fill="{AC}">Download</text>
      <rect x="20" y="80" width="176" height="36" rx="8" fill="none" stroke="{AC}" stroke-opacity="0.1">
        <animate attributeName="stroke-opacity" values="0.1;0.3;0.1" dur="2.3s" repeatCount="indefinite"/>
      </rect>
    </g>

    <!-- Linux -->
    <g transform="translate(472, 0)">
      <rect width="216" height="160" rx="12" fill="{SE}" stroke="{BORDER_L}"/>
      <rect width="216" height="3" rx="12" fill="{AC}" opacity="0.3"/>
      <text x="20" y="35" font-family="Inter,system-ui,sans-serif" font-size="15" font-weight="600" fill="{T1}">🐧 Linux</text>
      <text x="20" y="58" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T3}">AppImage · deb · rpm · Flatpak</text>
      <rect x="20" y="80" width="176" height="36" rx="8" fill="{AC}" fill-opacity="0.1" stroke="{AC}" stroke-opacity="0.3"/>
      <text x="108" y="103" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" font-weight="500" fill="{AC}">Download</text>
      <rect x="20" y="80" width="176" height="36" rx="8" fill="none" stroke="{AC}" stroke-opacity="0.1">
        <animate attributeName="stroke-opacity" values="0.1;0.3;0.1" dur="2.6s" repeatCount="indefinite"/>
      </rect>
    </g>

    <!-- Android -->
    <g transform="translate(708, 0)">
      <rect width="216" height="160" rx="12" fill="{SE}" stroke="{BORDER_L}"/>
      <rect width="216" height="3" rx="12" fill="{AB}" opacity="0.3"/>
      <text x="20" y="35" font-family="Inter,system-ui,sans-serif" font-size="15" font-weight="600" fill="{T1}">🤖 Android</text>
      <text x="20" y="58" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T3}">Play Store · F-Droid</text>
      <rect x="20" y="80" width="176" height="36" rx="8" fill="{AB}" fill-opacity="0.1" stroke="{AB}" stroke-opacity="0.3"/>
      <text x="108" y="103" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" font-weight="500" fill="{AB}">Get App</text>
      <rect x="20" y="80" width="176" height="36" rx="8" fill="none" stroke="{AB}" stroke-opacity="0.1">
        <animate attributeName="stroke-opacity" values="0.1;0.3;0.1" dur="2s" repeatCount="indefinite"/>
      </rect>
    </g>

    <!-- iOS -->
    <g transform="translate(944, 0)">
      <rect width="216" height="160" rx="12" fill="{SE}" stroke="{BORDER_L}"/>
      <rect width="216" height="3" rx="12" fill="{AB}" opacity="0.3"/>
      <text x="20" y="35" font-family="Inter,system-ui,sans-serif" font-size="15" font-weight="600" fill="{T1}">📱 iOS</text>
      <text x="20" y="58" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T3}">App Store</text>
      <rect x="20" y="80" width="176" height="36" rx="8" fill="{AB}" fill-opacity="0.1" stroke="{AB}" stroke-opacity="0.3"/>
      <text x="108" y="103" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" font-weight="500" fill="{AB}">Get App</text>
      <rect x="20" y="80" width="176" height="36" rx="8" fill="none" stroke="{AB}" stroke-opacity="0.1">
        <animate attributeName="stroke-opacity" values="0.1;0.3;0.1" dur="2.4s" repeatCount="indefinite"/>
      </rect>
    </g>
  </g>


  <!-- ═══════════════════════════════════════════════════════════ -->
  <!-- DEVELOPMENT (2550–2800)                                    -->
  <!-- ═══════════════════════════════════════════════════════════ -->
  <text x="{W//2}" y="2580" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="18" font-weight="600" fill="{AC}">▸ Development</text>

  <rect x="60" y="2600" width="{W-120}" height="190" rx="12" fill="#0A0A0A" stroke="{BORDER}"/>
  <!-- Window chrome -->
  <rect x="60" y="2600" width="{W-120}" height="32" rx="12" fill="{SE}"/>
  <rect x="60" y="2620" width="{W-120}" height="12" fill="{SE}"/>
  <circle cx="80" cy="2616" r="4" fill="{RD}" opacity="0.6"/>
  <circle cx="96" cy="2616" r="4" fill="{AB}" opacity="0.6"/>
  <circle cx="112" cy="2616" r="4" fill="{AC}" opacity="0.6"/>
  <text x="{W//2}" y="2620" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="11" fill="{T4}">terminal</text>

  <g font-family="JetBrains Mono,monospace" font-size="12">
    <text x="80" y="2665" fill="{T3}"># Requirements: JDK 17</text>
    <text x="80" y="2690" fill="{AC}">$</text>
    <text x="96" y="2690" fill="{T1}">git clone https://github.com/ValiantZippu/Kaiteyo.git</text>
    <text x="80" y="2715" fill="{AC}">$</text>
    <text x="96" y="2715" fill="{T1}">./gradlew :desktopApp:run</text>
    <text x="80" y="2740" fill="{AC}">$</text>
    <text x="96" y="2740" fill="{T1}">./gradlew :desktopApp:compileKotlinJvm</text>
    <text x="80" y="2765" fill="{AC}">$</text>
    <text x="96" y="2765" fill="{T1}">./gradlew :core:allTests</text>
  </g>

  <!-- Blinking cursor -->
  <rect x="80" y="2775" width="8" height="14" fill="{AC}" opacity="0.8">
    <animate attributeName="opacity" values="0.8;0;0.8" dur="1s" repeatCount="indefinite"/>
  </rect>


  <!-- ═══════════════════════════════════════════════════════════ -->
  <!-- DOCUMENTATION (2810–2980)                                  -->
  <!-- ═══════════════════════════════════════════════════════════ -->
  <text x="{W//2}" y="2850" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="18" font-weight="600" fill="{AC}">▸ Documentation</text>

  <rect x="60" y="2870" width="{W-120}" height="100" rx="16" fill="{SURFACE}" stroke="{BORDER_L}"/>
  <rect x="60" y="2870" width="{W-120}" height="3" rx="16" fill="{AC}" opacity="0.2"/>

  <!-- Doc links in 4 columns -->
  <g font-family="Inter,system-ui,sans-serif" font-size="12">
    <text x="100" y="2910" fill="{T1}">📖 Index</text>
    <text x="250" y="2910" fill="{T1}">📦 Product</text>
    <text x="400" y="2910" fill="{T1}">🏛️ Architecture</text>
    <text x="570" y="2910" fill="{T1}">🎨 Design</text>
    <text x="710" y="2910" fill="{T1}">🧠 Features</text>
    <text x="850" y="2910" fill="{T1}">🗺️ Roadmap</text>
    <text x="1000" y="2910" fill="{T1}">🎮 Game</text>

    <text x="100" y="2940" fill="{T1}">🔌 Integrations</text>
    <text x="250" y="2940" fill="{T1}">👤 User Guide</text>
    <text x="400" y="2940" fill="{T1}">⚙️ Development</text>
    <text x="570" y="2940" fill="{T1}">🧪 Testing</text>
    <text x="710" y="2940" fill="{T1}">📊 State</text>
    <text x="850" y="2940" fill="{T1}">🤖 AI Guide</text>
    <text x="1000" y="2940" fill="{T1}">⌨️ CLI</text>
  </g>


  <!-- ═══════════════════════════════════════════════════════════ -->
  <!-- FOOTER (3000–3200)                                         -->
  <!-- ═══════════════════════════════════════════════════════════ -->

  <!-- Gradient divider -->
  <line x1="200" y1="3050" x2="1000" y2="3050" stroke="url(#grD)" stroke-width="1"/>

  <!-- Mini logo -->
  <g transform="translate({W//2 - 10}, 3070)">
    <path d="M2,1 Q6,0 8,4 Q9,6 8,8" stroke="{AC}" stroke-width="1.2" fill="none" stroke-linecap="round" opacity="0.25"/>
    <circle cx="8" cy="8" r="1" fill="{AB}" opacity="0.25"/>
  </g>

  <text x="{W//2}" y="3110" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T4}">
    Free and open source under the GNU General Public License v3.0
  </text>
  <text x="{W//2}" y="3130" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="10" fill="{T4}" opacity="0.5">
    © 2022–2023 Yaroslav Shuliak (original Kanji Dojo) · Kaiteyo is independently developed
  </text>

  <!-- Bottom accent -->
  <rect x="0" y="3198" width="{W}" height="2" fill="url(#acG)" opacity="0.3"/>

</svg>'''

(OUT / "kaiteyo_landing.svg").write_text(svg, encoding="utf-8")
print(f"✨ Generated kaiteyo_landing.svg ({len(svg):,} bytes)")
