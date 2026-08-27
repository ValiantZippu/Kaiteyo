#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate a fully interactive Kaiteyo landing page SVG.

Features: clickable links (<a>), hover glow states (animate), animated
menus, pulsing buttons, search bar with blinking cursor, notification
badges, expandable sections -- maximum visual impact.
"""

import pathlib

OUT = pathlib.Path(__file__).resolve().parent.parent / "assets" / "readme"
OUT.mkdir(parents=True, exist_ok=True)

# -- Tokens --------------------------------------------------------
BG="#050505"; SF="#0D0D0D"; SE="#101010"; SI="#1A1A1A"
BD="#1E1E1E"; BL="#2A2A2A"; BL2="#333333"
T1="#F0F0F0"; T2="#A0A0A0"; T3="#606060"; T4="#404040"
AC="#C2FC8B"; ACD="#9CE85E"; AB="#FEAB57"; ABD="#FD8A2E"
BLU="#7BC8FF"; PU="#A78BFA"; RD="#FF6B6B"
SEARCH = chr(128269)  # 🔍 emoji safe for f-strings
W=1200; REPO="https://github.com/ValiantZippu/Kaiteyo"
REL="https://github.com/ValiantZippu/Kaiteyo/releases"
DOC="https://github.com/ValiantZippu/Kaiteyo/tree/develop/docs"

def a(href, inner, title=""):
    """Wrap SVG content in an <a> tag with optional title tooltip."""
    t = f'<title>{title}</title>' if title else ''
    return f'<a href="{href}">{t}{inner}</a>'

# Pre-build reusable sections as strings
hero = f'''
  <!-- ═══ HERO ═══ -->
  {a(REPO, f'''
  <ellipse cx="{W//2}" cy="180" rx="350" ry="180" fill="{AC}" opacity="0.04" filter="url(#bg)">
    <animate attributeName="opacity" values="0.04;0.08;0.04" dur="3s" repeatCount="indefinite"/>
  </ellipse>
  <rect x="0" y="0" width="{W}" height="2" fill="url(#acG)">
    <animate attributeName="opacity" values="0.8;1;0.8" dur="2s" repeatCount="indefinite"/>
  </rect>
  <g transform="translate({W//2-30},75)" filter="url(#gl)">
    <circle cx="30" cy="30" r="45" fill="{AC}" opacity="0.07">
      <animate attributeName="r" values="45;55;45" dur="3s" repeatCount="indefinite"/>
      <animate attributeName="opacity" values="0.07;0.12;0.07" dur="3s" repeatCount="indefinite"/>
    </circle>
    <path d="M12,5 Q28,3 38,16 Q44,24 42,34 Q41,38 38,42" stroke="{AC}" stroke-width="4.5" fill="none" stroke-linecap="round"/>
    <path d="M14,22 Q28,17 40,26" stroke="{AC}" stroke-width="2.5" fill="none" stroke-linecap="round" opacity="0.7"/>
    <circle cx="42" cy="34" r="2.5" fill="{AB}"/>
  </g>
  <text x="{W//2}" y="185" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="52" font-weight="700" fill="{T1}" letter-spacing="-0.03em">Kaiteyo</text>
  <text x="{W//2}" y="222" text-anchor="middle" font-family="'Noto Sans JP',sans-serif" font-size="22" fill="{T3}" letter-spacing="0.1em">書いてよ</text>
  <text x="{W//2}" y="265" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="17" fill="{T2}">Write it. Practice. Master it.</text>
  <text x="{W//2}" y="292" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="13" fill="{T4}">A premium, cross-platform Japanese learning application</text>
  ''', 'Kaiteyo -- Go to repository')}
'''

badges = f'''
  <!-- ═══ BADGES ═══ -->
  <g transform="translate({W//2-300},315)">
    {a(REPO+'/releases', f'''
    <rect width="90" height="28" rx="6" fill="{SE}" stroke="{BL}" stroke-width="1" stroke-opacity="0.4">
      <animate attributeName="stroke-opacity" values="0.4;0.8;0.4" dur="2s" repeatCount="indefinite"/>
    </rect>
    <text x="45" y="18" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="11" fill="{AC}">v2.2.1</text>
    ''', 'Latest release')}
    {a(REPO+'/blob/develop/LICENSE', f'''
    <rect x="100" y="0" width="90" height="28" rx="6" fill="{SE}" stroke="{BL}"/>
    <text x="145" y="18" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="11" fill="{AB}">GPL-3.0</text>
    ''', 'License: GPL-3.0')}
    <rect x="210" y="0" width="280" height="28" rx="6" fill="{SE}" stroke="{BL}"/>
    <text x="350" y="18" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T3}">Windows · macOS · Linux · Android · iOS</text>
    <rect x="510" y="0" width="100" height="28" rx="6" fill="{SE}" stroke="#7F52FF" stroke-opacity="0.4"/>
    <text x="560" y="18" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="11" fill="#7F52FF">Kotlin 2.1</text>
  </g>
'''

nav = f'''
  <!-- ═══ NAV ═══ -->
  <g transform="translate({W//2-250},385)">
    <rect width="80" height="32" rx="16" fill="{AC}" fill-opacity="0.12" stroke="{AC}" stroke-opacity="0.3">
      <animate attributeName="fill-opacity" values="0.12;0.2;0.12" dur="2s" repeatCount="indefinite"/>
    </rect>
    <text x="40" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{AC}">How it works</text>
    {a('#features', '<rect x="90" y="0" width="70" height="32" rx="16" fill="{SI}" stroke="{BL}"/><text x="125" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Study</text>'.format(**globals()), 'Core study features')}
    {a('#desktop', '<rect x="170" y="0" width="100" height="32" rx="16" fill="{SI}" stroke="{BL}"/><text x="220" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Desktop Suite</text>'.format(**globals()), 'Desktop suite features')}
    {a('#mobile', '<rect x="280" y="0" width="70" height="32" rx="16" fill="{SI}" stroke="{BL}"/><text x="315" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Mobile</text>'.format(**globals()), 'Mobile apps')}
    {a(REL, '<rect x="360" y="0" width="80" height="32" rx="16" fill="{SI}" stroke="{BL}"/><text x="400" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Downloads</text>'.format(**globals()), 'Download releases')}
    {a(DOC, '<rect x="450" y="0" width="80" height="32" rx="16" fill="{SI}" stroke="{BL}"/><text x="490" y="21" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Docs</text>'.format(**globals()), 'Documentation')}
  </g>
'''

# Search bar
search = f'''
  <!-- ═══ SEARCH BAR ═══ -->
  {a(REPO+'/search', f'''
  <g transform="translate({W//2-180},435)">
    <rect width="360" height="38" rx="10" fill="{SI}" stroke="{BL}" stroke-width="1"/>
    <text x="14" y="24" font-family="Inter,system-ui,sans-serif" font-size="13" fill="{T4}">{SEARCH}</text>
    <text x="36" y="24" font-family="Inter,system-ui,sans-serif" font-size="13" fill="{T3}">Search kanji, vocabulary, features...</text>
    <rect x="310" y="10" width="40" height="18" rx="4" fill="{BD}"/>
    <text x="330" y="23" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="9" fill="{T4}">&#x2318;K</text>
    <!-- Blinking cursor -->
    <rect x="205" y="12" width="1.5" height="16" fill="{AC}" opacity="0.7">
      <animate attributeName="opacity" values="0.7;0;0.7" dur="1s" repeatCount="indefinite"/>
    </rect>
  </g>
  ''', 'Search the repository')}
'''

def make_card(x, y, w, h, title, color, features, col_count=2):
    """Build a feature card with clickable items."""
    header_h = 56
    row_h = 32
    rows_per_col = (len(features) + col_count - 1) // col_count
    card_h = header_h + rows_per_col * row_h + 16

    items = ""
    for i, (icon, name, desc) in enumerate(features):
        col = i % col_count
        row = i // col_count
        ix = 20 + col * (w // col_count)
        iy = header_h + 8 + row * row_h
        icon_color = AC if icon == "✅" else AB

        # Each feature row is clickable
        items += f'''
    <g transform="translate({ix},{iy})">
      <rect x="0" y="-12" width="{w // col_count - 16}" height="28" rx="6" fill="transparent">
        <set attributeName="fill" to="{SI}" begin="mouseover" end="mouseout"/>
        <animate attributeName="fill-opacity" values="0;0.5;0" dur="3s" begin="{i * 0.3}s" repeatCount="indefinite"/>
      </rect>
      <text x="4" y="4" font-family="Inter,system-ui,sans-serif" font-size="14" fill="{icon_color}">{icon}</text>
      <text x="28" y="4" font-family="Inter,system-ui,sans-serif" font-size="14" font-weight="500" fill="{T1}">{name}</text>
      <text x="{140 + (w // col_count - 160) // 2}" y="4" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T3}">{desc}</text>
    </g>'''

    return f'''
  <g transform="translate({x},{y})">
    <rect width="{w}" height="{card_h}" rx="14" fill="{SF}" stroke="{BL}" stroke-width="1"/>
    <rect width="{w}" height="3" rx="14" fill="{color}" opacity="0.5">
      <animate attributeName="opacity" values="0.5;0.8;0.5" dur="3s" repeatCount="indefinite"/>
    </rect>
    <text x="24" y="38" font-family="Inter,system-ui,sans-serif" font-size="17" font-weight="600" fill="{T1}">{title}</text>
    <text x="24" y="52" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T4}">{features[0][2][:40]}...</text>
    {items}
  </g>'''

# ═══════════════════════════════════════════════════════════════════
# SECTIONS
# ═══════════════════════════════════════════════════════════════════

# Workflow
workflow = f'''
  <!-- ═══ WORKFLOW ═══ -->
  {a(REPO+'/tree/develop/docs/architecture', f'''
  <g id="how-it-works">
    <rect x="60" y="490" width="{W-120}" height="340" rx="16" fill="{SF}" stroke="{BL}">
      <animate attributeName="stroke-opacity" values="0.4;0.7;0.4" dur="4s" repeatCount="indefinite"/>
    </rect>
    <text x="100" y="528" font-family="Inter,system-ui,sans-serif" font-size="16" font-weight="600" fill="{AC}">> How Kaiteyo works</text>

    <!-- Step 1 -->
    <g transform="translate(100,555)">
      <rect width="220" height="210" rx="12" fill="{SE}" stroke="{BL}"/>
      <rect width="220" height="3" rx="12" fill="{AC}" opacity="0.6"/>
      <text x="18" y="38" font-size="26">📖</text>
      <text x="50" y="38" font-family="Inter,system-ui,sans-serif" font-size="14" font-weight="600" fill="{T1}">Read or Watch</text>
      <text x="18" y="65" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Japanese content in the built-in</text>
      <text x="18" y="80" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">browser, media player, or reading env.</text>
      <rect x="18" y="100" width="65" height="24" rx="6" fill="{SI}" stroke="{BL}"/>
      <text x="50" y="116" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="9" fill="{T3}">Browser</text>
      <rect x="90" y="100" width="55" height="24" rx="6" fill="{SI}" stroke="{BL}"/>
      <text x="117" y="116" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="9" fill="{T3}">Media</text>
      <rect x="152" y="100" width="48" height="24" rx="6" fill="{SI}" stroke="{BL}"/>
      <text x="176" y="116" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="9" fill="{T3}">EPUB</text>
    </g>

    <!-- Arrow 1 -->
    <g transform="translate(330,650)">
      <line x1="0" y1="0" x2="50" y2="0" stroke="{AC}" stroke-width="2" opacity="0.5">
        <animate attributeName="opacity" values="0.3;0.7;0.3" dur="1.5s" repeatCount="indefinite"/>
      </line>
      <polygon points="50,-5 60,0 50,5" fill="{AC}" opacity="0.5">
        <animate attributeName="opacity" values="0.3;0.7;0.3" dur="1.5s" repeatCount="indefinite"/>
      </polygon>
    </g>

    <!-- Step 2 -->
    <g transform="translate(400,555)">
      <rect width="220" height="210" rx="12" fill="{SE}" stroke="{BL}"/>
      <rect width="220" height="3" rx="12" fill="{AC}" opacity="0.6"/>
      <text x="18" y="38" font-size="26">🔍</text>
      <text x="50" y="38" font-family="Inter,system-ui,sans-serif" font-size="14" font-weight="600" fill="{T1}">Hover a Word</text>
      <text x="18" y="65" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Instant dictionary popup with</text>
      <text x="18" y="80" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">readings, definitions, and TTS.</text>
      <rect x="18" y="100" width="60" height="24" rx="6" fill="{AC}" fill-opacity="0.12" stroke="{AC}" stroke-opacity="0.3"/>
      <text x="48" y="116" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="9" fill="{AC}">Yomitan</text>
      <rect x="86" y="100" width="55" height="24" rx="6" fill="{SI}" stroke="{BL}"/>
      <text x="113" y="116" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="9" fill="{T3}">JMdict</text>
    </g>

    <!-- Arrow 2 -->
    <g transform="translate(630,650)">
      <line x1="0" y1="0" x2="50" y2="0" stroke="{AB}" stroke-width="2" opacity="0.5">
        <animate attributeName="opacity" values="0.3;0.7;0.3" dur="1.5s" begin="0.5s" repeatCount="indefinite"/>
      </line>
      <polygon points="50,-5 60,0 50,5" fill="{AB}" opacity="0.5">
        <animate attributeName="opacity" values="0.3;0.7;0.3" dur="1.5s" begin="0.5s" repeatCount="indefinite"/>
      </polygon>
    </g>

    <!-- Step 3 -->
    <g transform="translate(700,555)">
      <rect width="220" height="210" rx="12" fill="{SE}" stroke="{BL}"/>
      <rect width="220" height="3" rx="12" fill="{AB}" opacity="0.6"/>
      <text x="18" y="38" font-size="26">✂️</text>
      <text x="50" y="38" font-family="Inter,system-ui,sans-serif" font-size="14" font-weight="600" fill="{T1}">Mine a Sentence</text>
      <text x="18" y="65" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Card with screenshot, audio clip,</text>
      <text x="18" y="80" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">and timestamp in your SRS queue.</text>
      <rect x="18" y="100" width="65" height="24" rx="6" fill="{AB}" fill-opacity="0.12" stroke="{AB}" stroke-opacity="0.3"/>
      <text x="50" y="116" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="9" fill="{AB}">Mining</text>
      <rect x="90" y="100" width="50" height="24" rx="6" fill="{SI}" stroke="{BL}"/>
      <text x="115" y="116" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="9" fill="{T3}">Anki</text>
    </g>

    <!-- Arrow 3 -->
    <g transform="translate(930,650)">
      <line x1="0" y1="0" x2="50" y2="0" stroke="{BLU}" stroke-width="2" opacity="0.5">
        <animate attributeName="opacity" values="0.3;0.7;0.3" dur="1.5s" begin="1s" repeatCount="indefinite"/>
      </line>
      <polygon points="50,-5 60,0 50,5" fill="{BLU}" opacity="0.5">
        <animate attributeName="opacity" values="0.3;0.7;0.3" dur="1.5s" begin="1s" repeatCount="indefinite"/>
      </polygon>
    </g>

    <!-- Step 4 -->
    <g transform="translate(1000,555)">
      <rect width="140" height="210" rx="12" fill="{SE}" stroke="{BL}"/>
      <rect width="140" height="3" rx="12" fill="{BLU}" opacity="0.6"/>
      <text x="18" y="38" font-size="26">🧠</text>
      <text x="50" y="38" font-family="Inter,system-ui,sans-serif" font-size="14" font-weight="600" fill="{T1}">Review</text>
      <text x="18" y="65" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">Spaced repetition with</text>
      <text x="18" y="80" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">FSRS-5. Jump back to the</text>
      <text x="18" y="95" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T2}">exact scene.</text>
      <rect x="18" y="110" width="60" height="24" rx="6" fill="{BLU}" fill-opacity="0.12" stroke="{BLU}" stroke-opacity="0.3"/>
      <text x="48" y="126" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="9" fill="{BLU}">FSRS-5</text>
    </g>

    <text x="{W//2}" y="800" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T4}">
      Everything works offline. Your data is yours -- import, export, Anki, backup, sync.
    </text>
  </g>
  ''', 'How Kaiteyo works -- architecture overview')}
'''

# ---- Feature sections ----
core_features = [
    ("✅","Kanji & Kana","JLPT N5–N1 + school-grade decks"),
    ("✅","Vocabulary","Readings, meanings, furigana"),
    ("✅","Writing Practice","Stroke-order, brush canvas"),
    ("✅","Spaced Repetition","FSRS-5, custom intervals"),
    ("✅","Deck Management","Create, edit, archive, bulk"),
    ("✅","Radical Search","6000+ characters"),
    ("✅","Text Analysis","Ichiran-style breakdown"),
    ("✅","Statistics","Heatmap, curves, goals, exams"),
    ("✅","Anki Import/Export",".apkg on all platforms"),
    ("✅","Backup & Restore","Profile archives, settings"),
    ("🚧","Sync","GitHub device-flow"),
    ("🚧","Grammar","Explanation-first practice"),
]

desktop_features = [
    ("✅","Dictionary","Yomitan-style -- ZIP/JSON"),
    ("✅","Popup Lookup","Hover → readings, mining, TTS"),
    ("✅","Media Center","VLC / mpv -- subtitles"),
    ("✅","Subtitle Mining","Screenshot + audio + timestamp"),
    ("✅","Learning Browser","Reader mode, bookmarks"),
    ("✅","Local API","Bearer-token HTTP server"),
    ("✅","AnkiConnect","Push/import from Anki"),
    ("✅","Theme Studio","HSV wheel, gradients"),
    ("✅","Onboarding","8-step wizard"),
    ("✅","Installers","Inno Setup, DMG, AppImage"),
    ("✅","Native Window","Drag, resize, snap, rounded"),
    ("✅","Float Launcher","Draggable bubble, snap"),
    ("✅","Overlay Sidebar","4 positions, elevated"),
    ("🚧","OCR","Capture + Tesseract"),
    ("🚧","Auto-Update","Architecture complete"),
    ("🚧","Plugins","Registry + marketplace"),
]

mobile_features = [
    ("✅","Android","Play Store + F-Droid"),
    ("🚧","iOS","Shared engine, macOS builds"),
]

core_card = make_card(60, 870, (W-120)//2 - 10, 400, "Core Study Engine", AC, core_features, 2)
desktop_card = make_card(60, 1440, W-120, 520, "Desktop Suite", AB, desktop_features, 2)
mobile_card = make_card(60, 2020, W-120, 110, "Mobile", BLU, mobile_features, 2)

features_section = f'''
  <!-- ═══ FEATURES ═══ -->
  <text x="{W//2}" y="860" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T4}">All platforms -- Desktop · Android · iOS</text>
  <g id="features">{core_card}</g>
  <g id="desktop">{desktop_card}</g>
  <g id="mobile">{mobile_card}</g>
'''

# ---- Downloads ----
dl_platforms = [
    ("🪟","Windows","EXE · MSI · Portable ZIP",AC,REL),
    ("🍎","macOS","DMG -- arm64 + x64, signed",AC,REL),
    ("🐧","Linux","AppImage · deb · rpm · Flatpak",AC,REL),
    ("🤖","Android","Play Store · F-Droid",AB,"https://play.google.com/store/apps/details?id=ua.syt0r.kanji"),
    ("📱","iOS","App Store",AB,"https://apps.apple.com/ua/app/kanji-dojo/id6745169386"),
]

dl_cards = ""
card_w = 200
gap = 20
total_w = len(dl_platforms) * card_w + (len(dl_platforms)-1) * gap
start_x = (W - total_w) // 2

for i, (icon, name, sub, color, href) in enumerate(dl_platforms):
    cx = start_x + i * (card_w + gap)
    dl_cards += f'''
    {a(href, f'''
    <g transform="translate({cx},0)">
      <rect width="{card_w}" height="160" rx="12" fill="{SE}" stroke="{BL}">
        <animate attributeName="stroke-opacity" values="0.4;0.7;0.4" dur="{2 + i*0.3}s" repeatCount="indefinite"/>
      </rect>
      <rect width="{card_w}" height="3" rx="12" fill="{color}" opacity="0.4"/>
      <text x="18" y="35" font-family="Inter,system-ui,sans-serif" font-size="15" font-weight="600" fill="{T1}">{icon} {name}</text>
      <text x="18" y="58" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T3}">{sub}</text>
      <rect x="18" y="80" width="{card_w-36}" height="36" rx="8" fill="{color}" fill-opacity="0.1" stroke="{color}" stroke-opacity="0.3">
        <animate attributeName="stroke-opacity" values="0.3;0.6;0.3" dur="{1.5 + i*0.2}s" repeatCount="indefinite"/>
      </rect>
      <text x="{card_w//2}" y="103" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" font-weight="500" fill="{color}">{"Download" if i < 3 else "Get App"}</text>
      <!-- Glow ring -->
      <rect x="16" y="78" width="{card_w-32}" height="40" rx="10" fill="none" stroke="{color}" stroke-opacity="0">
        <animate attributeName="stroke-opacity" values="0;0.2;0" dur="{2+i*0.3}s" repeatCount="indefinite"/>
      </rect>
    </g>
    ''', f'{name} -- Download {name}')}

downloads_section = f'''
  <!-- ═══ DOWNLOADS ═══ -->
  <text x="{W//2}" y="2180" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="18" font-weight="600" fill="{AC}">> Downloads</text>
  <g transform="translate(0,2200)">{dl_cards}</g>
'''

# ---- Terminal ----
terminal = f'''
  <!-- ═══ TERMINAL ═══ -->
  <g transform="translate(60,2400)">
    <rect width="{W-120}" height="180" rx="12" fill="#0A0A0A" stroke="{BD}"/>
    <rect width="{W-120}" height="32" rx="12" fill="{SE}"/>
    <rect width="{W-120}" height="12" fill="{SE}"/>
    <circle cx="20" cy="16" r="4" fill="{RD}" opacity="0.6"/>
    <circle cx="36" cy="16" r="4" fill="{AB}" opacity="0.6"/>
    <circle cx="52" cy="16" r="4" fill="{AC}" opacity="0.6"/>
    <text x="{(W-120)//2}" y="20" text-anchor="middle" font-family="JetBrains Mono,monospace" font-size="11" fill="{T4}">terminal</text>

    <g font-family="JetBrains Mono,monospace" font-size="12">
      <text x="20" y="62" fill="{T3}"># Requirements: JDK 17</text>
      <text x="20" y="85" fill="{AC}">$</text>
      <text x="36" y="85" fill="{T1}">git clone https://github.com/ValiantZippu/Kaiteyo.git</text>
      <text x="20" y="108" fill="{AC}">$</text>
      <text x="36" y="108" fill="{T1}">./gradlew :desktopApp:run</text>
      <text x="20" y="131" fill="{AC}">$</text>
      <text x="36" y="131" fill="{T1}">./gradlew :desktopApp:compileKotlinJvm</text>
      <text x="20" y="154" fill="{AC}">$</text>
      <text x="36" y="154" fill="{T1}">./gradlew :core:allTests</text>
    </g>
    <rect x="20" y="160" width="8" height="14" fill="{AC}" opacity="0.8">
      <animate attributeName="opacity" values="0.8;0;0.8" dur="1s" repeatCount="indefinite"/>
    </rect>
  </g>
'''

# ---- Documentation ----
doc_items = [
    ("📖","Index"),("📦","Product"),("🏛️","Architecture"),("🎨","Design"),
    ("🧠","Features"),("🗺️","Roadmap"),("🎮","Game"),("🔌","Integrations"),
    ("👤","User Guide"),("⚙️","Development"),("🧪","Testing"),("📊","State"),
    ("🤖","AI Guide"),("⌨️","CLI"),("📦","Releases"),("🔐","Security"),
    ("⚖️","Legal"),("🐞","Issues"),("📜","Changelog"),
]
doc_cols = 7
doc_rows_count = (len(doc_items) + doc_cols - 1) // doc_cols
doc_links = ""
for i, (icon, name) in enumerate(doc_items):
    col = i % doc_cols
    row = i // doc_cols
    dx = 20 + col * ((W-200) // doc_cols)
    dy = 40 + row * 28
    doc_links += f'<text x="{dx}" y="{dy}" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T1}">{icon} {name}</text>\n'

docs_section = f'''
  <!-- ═══ DOCS ═══ -->
  {a(DOC, f'''
  <text x="{W//2}" y="2620" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="18" font-weight="600" fill="{AC}">> Documentation</text>
  <rect x="60" y="2640" width="{W-120}" height="{40 + doc_rows_count * 28 + 20}" rx="16" fill="{SF}" stroke="{BL}">
    <animate attributeName="stroke-opacity" values="0.4;0.6;0.4" dur="4s" repeatCount="indefinite"/>
  </rect>
  <rect x="60" y="2640" width="{W-120}" height="3" rx="16" fill="{AC}" opacity="0.2"/>
  <g transform="translate(80,2660)">{doc_links}</g>
  ''', 'Browse documentation')}
'''

# ---- Contributing ----
contrib = f'''
  <!-- ═══ CONTRIB ═══ -->
  {a(REPO+'/blob/develop/CONTRIBUTING.md', f'''
  <g transform="translate(60,2800)">
    <rect width="{W-120}" height="80" rx="12" fill="{SF}" stroke="{BL}"/>
    <text x="24" y="35" font-family="Inter,system-ui,sans-serif" font-size="14" font-weight="600" fill="{T1}">> Contributing</text>
    <text x="24" y="58" font-family="Inter,system-ui,sans-serif" font-size="12" fill="{T2}">Fork → branch from develop → make changes → verify → PR</text>
    <rect x="{W-200}" y="25" width="120" height="32" rx="8" fill="{AC}" fill-opacity="0.1" stroke="{AC}" stroke-opacity="0.3"/>
    <text x="{W-140}" y="46" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="12" font-weight="500" fill="{AC}">Get Started →</text>
  </g>
  ''', 'Contributing guide')}
'''

# ---- Footer ----
footer = f'''
  <!-- ═══ FOOTER ═══ -->
  <line x1="200" y1="2930" x2="1000" y2="2930" stroke="url(#grD)" stroke-width="1"/>
  <g transform="translate({W//2-10},2950)">
    <path d="M2,1 Q6,0 8,4 Q9,6 8,8" stroke="{AC}" stroke-width="1.2" fill="none" stroke-linecap="round" opacity="0.25"/>
    <circle cx="8" cy="8" r="1" fill="{AB}" opacity="0.25"/>
  </g>
  <text x="{W//2}" y="2990" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="11" fill="{T4}">
    Free and open source under the GNU General Public License v3.0
  </text>
  <text x="{W//2}" y="3010" text-anchor="middle" font-family="Inter,system-ui,sans-serif" font-size="10" fill="{T4}" opacity="0.5">
    © 2022–2023 Yaroslav Shuliak (original Kanji Dojo) · Kaiteyo is independently developed
  </text>
  <rect x="0" y="3048" width="{W}" height="2" fill="url(#acG)" opacity="0.3"/>
'''

# ═══════════════════════════════════════════════════════════════════
# ASSEMBLE
# ═══════════════════════════════════════════════════════════════════
svg = f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
     viewBox="0 0 {W} 3060" width="{W}" height="3060">
  <defs>
    <linearGradient id="acG" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{AC}"/><stop offset="100%" stop-color="{ACD}"/>
    </linearGradient>
    <linearGradient id="abG" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{AB}"/><stop offset="100%" stop-color="{ABD}"/>
    </linearGradient>
    <linearGradient id="grD" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{AC}" stop-opacity="0"/><stop offset="50%" stop-color="{AC}" stop-opacity="0.3"/><stop offset="100%" stop-color="{AC}" stop-opacity="0"/>
    </linearGradient>
    <filter id="gl"><feGaussianBlur stdDeviation="10" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
    <filter id="sg"><feGaussianBlur stdDeviation="4" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
    <filter id="bg"><feGaussianBlur stdDeviation="50" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
  </defs>

  <rect width="{W}" height="3060" fill="{BG}"/>

  <!-- Ambient orbs -->
  <ellipse cx="{W//2}" cy="200" rx="400" ry="200" fill="{AC}" opacity="0.03" filter="url(#bg)">
    <animate attributeName="opacity" values="0.03;0.06;0.03" dur="4s" repeatCount="indefinite"/>
  </ellipse>
  <ellipse cx="200" cy="1000" rx="300" ry="300" fill="{AB}" opacity="0.02" filter="url(#bg)">
    <animate attributeName="opacity" values="0.02;0.04;0.02" dur="6s" repeatCount="indefinite"/>
  </ellipse>
  <ellipse cx="1000" cy="2000" rx="350" ry="250" fill="{BLU}" opacity="0.02" filter="url(#bg)">
    <animate attributeName="opacity" values="0.02;0.035;0.02" dur="5s" repeatCount="indefinite"/>
  </ellipse>

  {hero}
  {badges}
  {nav}
  {search}
  {workflow}
  {features_section}
  {downloads_section}
  {terminal}
  {docs_section}
  {contrib}
  {footer}
</svg>'''

(OUT / "kaiteyo_landing.svg").write_text(svg, encoding="utf-8")
print(f"✨ Generated kaiteyo_landing.svg ({len(svg):,} bytes, {svg.count('<a ')} clickable links)")
