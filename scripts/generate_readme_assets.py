#!/usr/bin/env python3
"""Generate SVG visual assets for the Kaiteyo README.

Produces a set of inline-ready SVG files under assets/readme/ that render
as actual visual UI components — dark panels, glowing accents, styled cards,
gradient buttons — matching the Kaiteyo app/website design system.

Usage: python3 scripts/generate_readme_assets.py
"""

import pathlib

OUT = pathlib.Path(__file__).resolve().parent.parent / "assets" / "readme"
OUT.mkdir(parents=True, exist_ok=True)

# ── Kaiteyo design tokens ──────────────────────────────────────────
BG = "#050505"
SURFACE = "#0D0D0D"
SURFACE_E = "#101010"
BORDER = "#1E1E1E"
BORDER_L = "#2A2A2A"
TEXT = "#F0F0F0"
TEXT2 = "#A0A0A0"
TEXT3 = "#606060"
ACCENT = "#C2FC8B"
ACCENT_D = "#9CE85E"
AMBER = "#FEAB57"
AMBER_D = "#FD8A2E"
BLUE = "#7BC8FF"
PURPLE = "#A78BFA"
RED = "#FF6B6B"
W = 1200  # standard width

def write(name, svg):
    (OUT / name).write_text(svg, encoding="utf-8")
    print(f"  ✓ {name}")


# ══════════════════════════════════════════════════════════════════════
# 1. HERO — Full-width banner with logo + title + tagline + badges
# ══════════════════════════════════════════════════════════════════════
write("hero.svg", f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} 420" width="{W}" height="420">
  <defs>
    <linearGradient id="accentGrad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{ACCENT}"/>
      <stop offset="100%" stop-color="{ACCENT_D}"/>
    </linearGradient>
    <linearGradient id="amberGrad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{AMBER}"/>
      <stop offset="100%" stop-color="{AMBER_D}"/>
    </linearGradient>
    <linearGradient id="heroGlow" x1="50%" y1="0%" x2="50%" y2="100%">
      <stop offset="0%" stop-color="{ACCENT}" stop-opacity="0.12"/>
      <stop offset="100%" stop-color="{ACCENT}" stop-opacity="0"/>
    </linearGradient>
    <filter id="glow">
      <feGaussianBlur stdDeviation="12" result="blur"/>
      <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
    </filter>
    <filter id="softGlow">
      <feGaussianBlur stdDeviation="40" result="blur"/>
      <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
    </filter>
  </defs>

  <!-- Background -->
  <rect width="{W}" height="420" fill="{BG}"/>

  <!-- Subtle glow orb behind logo -->
  <ellipse cx="{W//2}" cy="140" rx="200" ry="120" fill="{ACCENT}" opacity="0.04" filter="url(#softGlow)"/>

  <!-- Top accent line -->
  <rect x="0" y="0" width="{W}" height="2" fill="url(#accentGrad)" opacity="0.6"/>

  <!-- Logo mark (simplified K calligraphy) -->
  <g transform="translate({W//2 - 40}, 60)" filter="url(#glow)">
    <path d="M10,5 Q30,3 45,18 Q55,28 52,42 Q50,50 45,55"
          stroke="{ACCENT}" stroke-width="5" fill="none" stroke-linecap="round"/>
    <path d="M15,28 Q35,22 50,32"
          stroke="{ACCENT}" stroke-width="3" fill="none" stroke-linecap="round" opacity="0.7"/>
    <circle cx="52" cy="42" r="3" fill="{AMBER}"/>
  </g>

  <!-- Title -->
  <text x="{W//2}" y="160" text-anchor="middle"
        font-family="Inter, system-ui, sans-serif" font-size="48" font-weight="700"
        fill="{TEXT}" letter-spacing="-0.02em">Kaiteyo</text>

  <!-- Japanese subtitle -->
  <text x="{W//2}" y="195" text-anchor="middle"
        font-family="Noto Sans JP, Hiragino Sans, sans-serif" font-size="20"
        fill="{TEXT3}" letter-spacing="0.08em">書いてよ</text>

  <!-- Tagline -->
  <text x="{W//2}" y="235" text-anchor="middle"
        font-family="Inter, system-ui, sans-serif" font-size="16"
        fill="{TEXT2}">Write it. Practice. Master it.</text>

  <!-- Subtitle -->
  <text x="{W//2}" y="258" text-anchor="middle"
        font-family="Inter, system-ui, sans-serif" font-size="13"
        fill="{TEXT3}">A premium, cross-platform Japanese learning application</text>

  <!-- Badge row -->
  <g transform="translate({W//2 - 280}, 290)">
    <!-- Version -->
    <rect x="0" y="0" width="95" height="26" rx="4" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="48" y="17" text-anchor="middle" font-family="JetBrains Mono, monospace" font-size="11" fill="{ACCENT}">v2.2.1</text>

    <!-- License -->
    <rect x="105" y="0" width="95" height="26" rx="4" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="152" y="17" text-anchor="middle" font-family="JetBrains Mono, monospace" font-size="11" fill="{AMBER}">GPL-3.0</text>

    <!-- Platforms -->
    <rect x="210" y="0" width="260" height="26" rx="4" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="340" y="17" text-anchor="middle" font-family="Inter, system-ui, sans-serif" font-size="11" fill="{TEXT3}">Windows · macOS · Linux · Android · iOS</text>

    <!-- Kotlin -->
    <rect x="480" y="0" width="100" height="26" rx="4" fill="{SURFACE_E}" stroke="#7F52FF" stroke-width="1" stroke-opacity="0.4"/>
    <text x="530" y="17" text-anchor="middle" font-family="JetBrains Mono, monospace" font-size="11" fill="#7F52FF">Kotlin 2.1</text>
  </g>

  <!-- Bottom accent line -->
  <rect x="0" y="418" width="{W}" height="2" fill="url(#accentGrad)" opacity="0.3"/>
</svg>''')


# ══════════════════════════════════════════════════════════════════════
# 2. NAV — Navigation bar
# ══════════════════════════════════════════════════════════════════════
write("nav.svg", f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} 50" width="{W}" height="50">
  <defs>
    <linearGradient id="navBg" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="{SURFACE}"/>
      <stop offset="100%" stop-color="{BG}"/>
    </linearGradient>
  </defs>
  <rect width="{W}" height="50" fill="url(#navBg)"/>
  <rect x="0" y="49" width="{W}" height="1" fill="{BORDER_L}"/>

  <g font-family="Inter, system-ui, sans-serif" font-size="13" text-anchor="middle">
    <text x="150" y="30" fill="{ACCENT}">▸ How it works</text>
    <text x="320" y="30" fill="{TEXT2}">Study</text>
    <text x="460" y="30" fill="{TEXT2}">Desktop Suite</text>
    <text x="620" y="30" fill="{TEXT2}">Mobile</text>
    <text x="750" y="30" fill="{TEXT2}">Downloads</text>
    <text x="880" y="30" fill="{TEXT2}">Develop</text>
    <text x="990" y="30" fill="{TEXT2}">Docs</text>
  </g>
</svg>''')


# ══════════════════════════════════════════════════════════════════════
# 3. WORKFLOW — The Read → Hover → Mine → Review flow
# ══════════════════════════════════════════════════════════════════════
write("workflow.svg", f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} 320" width="{W}" height="320">
  <defs>
    <linearGradient id="wfBg" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="{SURFACE}"/>
      <stop offset="100%" stop-color="{BG}"/>
    </linearGradient>
    <filter id="wfGlow">
      <feGaussianBlur stdDeviation="6" result="blur"/>
      <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
    </filter>
  </defs>

  <rect width="{W}" height="320" fill="url(#wfBg)" rx="16"/>
  <rect x="0" y="0" width="{W}" height="1" fill="{BORDER_L}" rx="16"/>

  <!-- Section title -->
  <text x="60" y="40" font-family="Inter, system-ui, sans-serif" font-size="14" font-weight="600" fill="{ACCENT}">▸ How Kaiteyo works</text>

  <!-- Step 1: Read -->
  <g transform="translate(80, 80)">
    <rect x="0" y="0" width="220" height="180" rx="12" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="20" y="35" font-size="24">📖</text>
    <text x="50" y="35" font-family="Inter, system-ui, sans-serif" font-size="15" font-weight="600" fill="{TEXT}">Read or Watch</text>
    <text x="20" y="65" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">Japanese content in the</text>
    <text x="20" y="82" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">built-in browser, media</text>
    <text x="20" y="99" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">player, or reading env.</text>
    <rect x="20" y="120" width="80" height="24" rx="6" fill="{SURFACE}" stroke="{BORDER}" stroke-width="1"/>
    <text x="60" y="136" text-anchor="middle" font-family="JetBrains Mono, monospace" font-size="10" fill="{TEXT3}">Browser</text>
    <rect x="110" y="120" width="70" height="24" rx="6" fill="{SURFACE}" stroke="{BORDER}" stroke-width="1"/>
    <text x="145" y="136" text-anchor="middle" font-family="JetBrains Mono, monospace" font-size="10" fill="{TEXT3}">Media</text>
    <rect x="12" y="155" width="196" height="1" fill="{BORDER}" opacity="0.5"/>
  </g>

  <!-- Arrow 1 -->
  <g transform="translate(310, 155)">
    <line x1="0" y1="0" x2="50" y2="0" stroke="{ACCENT}" stroke-width="2" opacity="0.5"/>
    <polygon points="50,-5 60,0 50,5" fill="{ACCENT}" opacity="0.5"/>
  </g>

  <!-- Step 2: Hover -->
  <g transform="translate(380, 80)">
    <rect x="0" y="0" width="220" height="180" rx="12" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="20" y="35" font-size="24">🔍</text>
    <text x="50" y="35" font-family="Inter, system-ui, sans-serif" font-size="15" font-weight="600" fill="{TEXT}">Hover a Word</text>
    <text x="20" y="65" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">Instant dictionary popup</text>
    <text x="20" y="82" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">with readings, definitions,</text>
    <text x="20" y="99" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">and TTS pronunciation.</text>
    <rect x="20" y="120" width="60" height="24" rx="6" fill="{ACCENT}" fill-opacity="0.15" stroke="{ACCENT}" stroke-width="1" stroke-opacity="0.3"/>
    <text x="50" y="136" text-anchor="middle" font-family="JetBrains Mono, monospace" font-size="10" fill="{ACCENT}">Yomitan</text>
    <rect x="90" y="120" width="55" height="24" rx="6" fill="{SURFACE}" stroke="{BORDER}" stroke-width="1"/>
    <text x="117" y="136" text-anchor="middle" font-family="JetBrains Mono, monospace" font-size="10" fill="{TEXT3}">JMdict</text>
    <rect x="12" y="155" width="196" height="1" fill="{BORDER}" opacity="0.5"/>
  </g>

  <!-- Arrow 2 -->
  <g transform="translate(610, 155)">
    <line x1="0" y1="0" x2="50" y2="0" stroke="{AMBER}" stroke-width="2" opacity="0.5"/>
    <polygon points="50,-5 60,0 50,5" fill="{AMBER}" opacity="0.5"/>
  </g>

  <!-- Step 3: Mine -->
  <g transform="translate(680, 80)">
    <rect x="0" y="0" width="220" height="180" rx="12" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="20" y="35" font-size="24">✂️</text>
    <text x="50" y="35" font-family="Inter, system-ui, sans-serif" font-size="15" font-weight="600" fill="{TEXT}">Mine a Sentence</text>
    <text x="20" y="65" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">Card with screenshot,</text>
    <text x="20" y="82" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">audio clip, and timestamp</text>
    <text x="20" y="99" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">lands in your SRS queue.</text>
    <rect x="20" y="120" width="80" height="24" rx="6" fill="{AMBER}" fill-opacity="0.15" stroke="{AMBER}" stroke-width="1" stroke-opacity="0.3"/>
    <text x="60" y="136" text-anchor="middle" font-family="JetBrains Mono, monospace" font-size="10" fill="{AMBER}">Mining</text>
    <rect x="110" y="120" width="70" height="24" rx="6" fill="{SURFACE}" stroke="{BORDER}" stroke-width="1"/>
    <text x="145" y="136" text-anchor="middle" font-family="JetBrains Mono, monospace" font-size="10" fill="{TEXT3}">Anki</text>
    <rect x="12" y="155" width="196" height="1" fill="{BORDER}" opacity="0.5"/>
  </g>

  <!-- Arrow 3 -->
  <g transform="translate(910, 155)">
    <line x1="0" y1="0" x2="50" y2="0" stroke="{BLUE}" stroke-width="2" opacity="0.5"/>
    <polygon points="50,-5 60,0 50,5" fill="{BLUE}" opacity="0.5"/>
  </g>

  <!-- Step 4: Review -->
  <g transform="translate(980, 80)">
    <rect x="0" y="0" width="180" height="180" rx="12" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="20" y="35" font-size="24">🧠</text>
    <text x="50" y="35" font-family="Inter, system-ui, sans-serif" font-size="15" font-weight="600" fill="{TEXT}">Review</text>
    <text x="20" y="65" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">Spaced repetition with</text>
    <text x="20" y="82" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">FSRS-5. Jump back to the</text>
    <text x="20" y="99" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT2}">exact scene in the media.</text>
    <rect x="20" y="120" width="70" height="24" rx="6" fill="{BLUE}" fill-opacity="0.15" stroke="{BLUE}" stroke-width="1" stroke-opacity="0.3"/>
    <text x="55" y="136" text-anchor="middle" font-family="JetBrains Mono, monospace" font-size="10" fill="{BLUE}">FSRS-5</text>
    <rect x="12" y="155" width="156" height="1" fill="{BORDER}" opacity="0.5"/>
  </g>

  <!-- Bottom note -->
  <text x="{W//2}" y="300" text-anchor="middle" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT3}">
    Everything works offline. Your data is yours — import, export, Anki, backup, sync.
  </text>
</svg>''')


# ══════════════════════════════════════════════════════════════════════
# 4. FEATURES — Core Study Engine card
# ══════════════════════════════════════════════════════════════════════
core_features = [
    ("✅", "Kanji & Kana", "JLPT N5–N1 + school-grade decks"),
    ("✅", "Vocabulary", "Readings, meanings, furigana, examples"),
    ("✅", "Writing Practice", "Stroke-order, brush canvas, evaluation"),
    ("✅", "Spaced Repetition", "FSRS-5, custom intervals, daily limits"),
    ("✅", "Deck Management", "Create, edit, archive, bulk actions"),
    ("✅", "Radical Search", "6000+ characters, dictionary-backed"),
    ("✅", "Text Analysis", "Word-by-word breakdown (Ichiran-style)"),
    ("✅", "Statistics", "Heatmap, curves, goals, exams, achievements"),
    ("✅", "Anki Import/Export", ".apkg on all platforms"),
    ("✅", "Backup & Restore", "Profile archives, settings, window state"),
    ("🚧", "Sync", "GitHub device-flow + private-gist"),
    ("🚧", "Grammar", "Explanation-first practice with starter deck"),
]

def make_feature_card(title, color, subtitle, features, x_off=0):
    card_w = 540
    row_h = 26
    header_h = 56
    card_h = header_h + len(features) * row_h + 20

    rows = ""
    for i, (icon, name, desc) in enumerate(features):
        y = header_h + 10 + i * row_h
        icon_color = ACCENT if icon == "✅" else AMBER
        rows += f'''    <text x="24" y="{y}" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{icon_color}">{icon}</text>
    <text x="44" y="{y}" font-family="Inter, system-ui, sans-serif" font-size="13" font-weight="500" fill="{TEXT}">{name}</text>
    <text x="200" y="{y}" font-family="Inter, system-ui, sans-serif" font-size="12" fill="{TEXT3}">{desc}</text>
'''
    return f'''  <g transform="translate({x_off}, 0)">
    <rect x="0" y="0" width="{card_w}" height="{card_h}" rx="12" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <rect x="0" y="0" width="{card_w}" height="3" rx="12" fill="{color}" opacity="0.6"/>
    <text x="24" y="36" font-family="Inter, system-ui, sans-serif" font-size="16" font-weight="600" fill="{TEXT}">{title}</text>
    <text x="24" y="52" font-family="Inter, system-ui, sans-serif" font-size="11" fill="{TEXT3}">{subtitle}</text>
{rows}  </g>'''

core_card = make_feature_card(
    "Core Study Engine", ACCENT,
    "All platforms — Desktop · Android · iOS",
    core_features, 0
)

write("features_core.svg", f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 560 420" width="560" height="420">
  <rect width="560" height="420" fill="{BG}"/>
{core_card}
</svg>''')


# ══════════════════════════════════════════════════════════════════════
# 5. FEATURES — Desktop Suite card
# ══════════════════════════════════════════════════════════════════════
desktop_features = [
    ("✅", "Dictionary", "Yomitan-style — ZIP/JSON, JMdict, KANJIDIC"),
    ("✅", "Popup Lookup", "Hover any text → readings, mining, TTS"),
    ("✅", "Media Center", "VLC / mpv — SRT/ASS/SSA/VTT subtitles"),
    ("✅", "Subtitle Mining", "Cards with screenshot + audio + timestamp"),
    ("✅", "Learning Browser", "Reader mode, bookmarks, lookup & mining"),
    ("✅", "Local API", "Bearer-token HTTP — media, mining, player"),
    ("✅", "AnkiConnect", "Push mined cards, import from Anki"),
    ("✅", "Theme Studio", "HSV wheel, gradients, presets, live preview"),
    ("✅", "Onboarding", "8-step wizard — theme, accent, scale, font"),
    ("✅", "Installers", "Inno Setup, DMG, AppImage/deb/rpm/Flatpak"),
    ("✅", "Native Window", "Title bar, OS drag, resize, snap, rounded"),
    ("✅", "Float Launcher", "Draggable bubble, snap-to-edge, modes"),
    ("✅", "Overlay Sidebar", "Floats on content, 4 positions, elevated"),
    ("🚧", "OCR", "Capture works; Tesseract detection"),
    ("🚧", "Auto-Update", "Architecture complete; staged rollout"),
    ("🚧", "Plugins", "Registry + marketplace scaffold"),
]

desktop_card = make_feature_card(
    "Desktop Suite", AMBER,
    "Windows · macOS · Linux — the flagship experience",
    desktop_features, 0
)

write("features_desktop.svg", f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 560 520" width="560" height="520">
  <rect width="560" height="520" fill="{BG}"/>
{desktop_card}
</svg>''')


# ══════════════════════════════════════════════════════════════════════
# 6. FEATURES — Mobile card
# ══════════════════════════════════════════════════════════════════════
mobile_features = [
    ("✅", "Android", "Play Store + F-Droid · Firebase, billing"),
    ("🚧", "iOS", "Shared engine + shell · macOS-only builds"),
]

mobile_card = make_feature_card(
    "Mobile", BLUE,
    "",
    mobile_features, 0
)

write("features_mobile.svg", f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 560 160" width="560" height="160">
  <rect width="560" height="160" fill="{BG}"/>
{mobile_card}
</svg>''')


# ══════════════════════════════════════════════════════════════════════
# 7. DOWNLOADS — Platform download cards
# ══════════════════════════════════════════════════════════════════════
write("downloads.svg", f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} 220" width="{W}" height="220">
  <defs>
    <linearGradient id="dlBg" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="{SURFACE}"/>
      <stop offset="100%" stop-color="{BG}"/>
    </linearGradient>
    <linearGradient id="dlGreen" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{ACCENT}"/>
      <stop offset="100%" stop-color="{ACCENT_D}"/>
    </linearGradient>
    <linearGradient id="dlAmber" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{AMBER}"/>
      <stop offset="100%" stop-color="{AMBER_D}"/>
    </linearGradient>
  </defs>

  <rect width="{W}" height="220" fill="url(#dlBg)" rx="16"/>
  <rect x="0" y="0" width="{W}" height="1" fill="{BORDER_L}"/>

  <text x="60" y="40" font-family="Inter, system-ui, sans-serif" font-size="14" font-weight="600" fill="{ACCENT}">▸ Downloads</text>

  <!-- Windows -->
  <g transform="translate(60, 65)">
    <rect x="0" y="0" width="200" height="130" rx="12" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="20" y="30" font-family="Inter, system-ui, sans-serif" font-size="14" font-weight="600" fill="{TEXT}">Windows</text>
    <text x="20" y="50" font-family="Inter, system-ui, sans-serif" font-size="11" fill="{TEXT3}">EXE · MSI · Portable ZIP</text>
    <rect x="20" y="70" width="80" height="32" rx="8" fill="url(#dlGreen)" opacity="0.15"/>
    <text x="60" y="91" text-anchor="middle" font-family="Inter, system-ui, sans-serif" font-size="12" font-weight="500" fill="{ACCENT}">Download</text>
  </g>

  <!-- macOS -->
  <g transform="translate(280, 65)">
    <rect x="0" y="0" width="200" height="130" rx="12" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="20" y="30" font-family="Inter, system-ui, sans-serif" font-size="14" font-weight="600" fill="{TEXT}">macOS</text>
    <text x="20" y="50" font-family="Inter, system-ui, sans-serif" font-size="11" fill="{TEXT3}">DMG — arm64 + x64, signed</text>
    <rect x="20" y="70" width="80" height="32" rx="8" fill="url(#dlGreen)" opacity="0.15"/>
    <text x="60" y="91" text-anchor="middle" font-family="Inter, system-ui, sans-serif" font-size="12" font-weight="500" fill="{ACCENT}">Download</text>
  </g>

  <!-- Linux -->
  <g transform="translate(500, 65)">
    <rect x="0" y="0" width="200" height="130" rx="12" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="20" y="30" font-family="Inter, system-ui, sans-serif" font-size="14" font-weight="600" fill="{TEXT}">Linux</text>
    <text x="20" y="50" font-family="Inter, system-ui, sans-serif" font-size="11" fill="{TEXT3}">AppImage · deb · rpm · Flatpak</text>
    <rect x="20" y="70" width="80" height="32" rx="8" fill="url(#dlGreen)" opacity="0.15"/>
    <text x="60" y="91" text-anchor="middle" font-family="Inter, system-ui, sans-serif" font-size="12" font-weight="500" fill="{ACCENT}">Download</text>
  </g>

  <!-- Android -->
  <g transform="translate(720, 65)">
    <rect x="0" y="0" width="200" height="130" rx="12" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="20" y="30" font-family="Inter, system-ui, sans-serif" font-size="14" font-weight="600" fill="{TEXT}">Android</text>
    <text x="20" y="50" font-family="Inter, system-ui, sans-serif" font-size="11" fill="{TEXT3}">Play Store · F-Droid</text>
    <rect x="20" y="70" width="80" height="32" rx="8" fill="url(#dlAmber)" opacity="0.15"/>
    <text x="60" y="91" text-anchor="middle" font-family="Inter, system-ui, sans-serif" font-size="12" font-weight="500" fill="{AMBER}">Get App</text>
  </g>

  <!-- iOS -->
  <g transform="translate(940, 65)">
    <rect x="0" y="0" width="200" height="130" rx="12" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
    <text x="20" y="30" font-family="Inter, system-ui, sans-serif" font-size="14" font-weight="600" fill="{TEXT}">iOS</text>
    <text x="20" y="50" font-family="Inter, system-ui, sans-serif" font-size="11" fill="{TEXT3}">App Store</text>
    <rect x="20" y="70" width="80" height="32" rx="8" fill="url(#dlAmber)" opacity="0.15"/>
    <text x="60" y="91" text-anchor="middle" font-family="Inter, system-ui, sans-serif" font-size="12" font-weight="500" fill="{AMBER}">Get App</text>
  </g>
</svg>''')


# ══════════════════════════════════════════════════════════════════════
# 8. DEV — Development section
# ══════════════════════════════════════════════════════════════════════
write("dev.svg", f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} 280" width="{W}" height="280">
  <defs>
    <linearGradient id="devBg" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="{SURFACE}"/>
      <stop offset="100%" stop-color="{BG}"/>
    </linearGradient>
  </defs>

  <rect width="{W}" height="280" fill="url(#devBg)" rx="16"/>
  <rect x="0" y="0" width="{W}" height="1" fill="{BORDER_L}"/>

  <text x="60" y="40" font-family="Inter, system-ui, sans-serif" font-size="14" font-weight="600" fill="{ACCENT}">▸ Development</text>

  <!-- Code block -->
  <rect x="60" y="60" width="{W - 120}" height="200" rx="12" fill="#0A0A0A" stroke="{BORDER}" stroke-width="1"/>
  <rect x="60" y="60" width="{W - 120}" height="32" rx="12" fill="{SURFACE_E}"/>
  <rect x="60" y="80" width="{W - 120}" height="12" fill="{SURFACE_E}"/>
  <circle cx="80" cy="76" r="4" fill="{RED}" opacity="0.6"/>
  <circle cx="96" cy="76" r="4" fill="{AMBER}" opacity="0.6"/>
  <circle cx="112" cy="76" r="4" fill="{ACCENT}" opacity="0.6"/>
  <text x="160" y="79" font-family="JetBrains Mono, monospace" font-size="11" fill="{TEXT3}">terminal</text>

  <text x="80" y="118" font-family="JetBrains Mono, monospace" font-size="12" fill="{TEXT3}"># Requirements: JDK 17</text>
  <text x="80" y="138" font-family="JetBrains Mono, monospace" font-size="12" fill="{ACCENT}">$</text>
  <text x="96" y="138" font-family="JetBrains Mono, monospace" font-size="12" fill="{TEXT}">git clone https://github.com/ValiantZippu/Kaiteyo.git</text>
  <text x="80" y="158" font-family="JetBrains Mono, monospace" font-size="12" fill="{ACCENT}">$</text>
  <text x="96" y="158" font-family="JetBrains Mono, monospace" font-size="12" fill="{TEXT}">./gradlew :desktopApp:run</text>
  <text x="80" y="178" font-family="JetBrains Mono, monospace" font-size="12" fill="{ACCENT}">$</text>
  <text x="96" y="178" font-family="JetBrains Mono, monospace" font-size="12" fill="{TEXT}">./gradlew :desktopApp:compileKotlinJvm</text>
  <text x="80" y="198" font-family="JetBrains Mono, monospace" font-size="12" fill="{ACCENT}">$</text>
  <text x="96" y="198" font-family="JetBrains Mono, monospace" font-size="12" fill="{TEXT}">./gradlew :core:allTests</text>
  <text x="80" y="218" font-family="JetBrains Mono, monospace" font-size="12" fill="{ACCENT}">$</text>
  <text x="96" y="218" font-family="JetBrains Mono, monospace" font-size="12" fill="{TEXT}">./gradlew :desktopApp:packageMsi</text>
  <text x="80" y="238" font-family="JetBrains Mono, monospace" font-size="11" fill="{TEXT3}"># First build downloads assets — network required</text>
</svg>''')


# ══════════════════════════════════════════════════════════════════════
# 9. DOCS — Documentation links grid
# ══════════════════════════════════════════════════════════════════════
doc_items = [
    ("📖", "Index", "docs/README.md"),
    ("📦", "Product", "docs/product/PRODUCT.md"),
    ("🏛️", "Architecture", "docs/architecture/OVERVIEW.md"),
    ("🎨", "Design", "docs/design/README.md"),
    ("🧠", "Features", "docs/features/FEATURES.md"),
    ("🗺️", "Roadmap", "docs/roadmap/ROADMAP.md"),
    ("🎮", "Game", "docs/game/README.md"),
    ("🔌", "Integrations", "docs/integrations/README.md"),
    ("👤", "User Guide", "docs/user-guide/README.md"),
    ("⚙️", "Development", "docs/development/DEVELOPER_GUIDE.md"),
    ("🧪", "Testing", "docs/testing/README.md"),
    ("📊", "State", "docs/planning/CURRENT_STATE.md"),
    ("🤖", "AI Guide", "docs/ai/AI_AGENT_GUIDE.md"),
    ("⌨️", "CLI", "docs/cli/README.md"),
    ("📦", "Releases", "docs/releases/RELEASE_PROCESS.md"),
    ("🔐", "Security", "SECURITY.md"),
    ("⚖️", "Legal", "docs/legal/README.md"),
    ("🐞", "Issues", "docs/planning/CURRENT_ISSUES.md"),
    ("📜", "Changelog", "CHANGELOG.md"),
]

cols = 3
col_w = 360
row_h = 36
rows_count = (len(doc_items) + cols - 1) // cols
card_h = 56 + rows_count * row_h + 16

doc_rows = ""
for i, (icon, name, path) in enumerate(doc_items):
    col = i % cols
    row = i // cols
    x = 24 + col * col_w
    y = 56 + row * row_h
    doc_rows += f'''    <text x="{x}" y="{y}" font-family="Inter, system-ui, sans-serif" font-size="13" fill="{TEXT}">{icon} {name}</text>
    <text x="{x + 100}" y="{y}" font-family="JetBrains Mono, monospace" font-size="10" fill="{TEXT3}">{path}</text>
'''

write("docs.svg", f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {card_h}" width="{W}" height="{card_h}">
  <rect width="{W}" height="{card_h}" fill="{BG}" rx="16"/>
  <rect x="0" y="0" width="{W}" height="1" fill="{BORDER_L}"/>

  <text x="60" y="36" font-family="Inter, system-ui, sans-serif" font-size="14" font-weight="600" fill="{ACCENT}">▸ Documentation</text>

  <rect x="40" y="50" width="{W - 80}" height="{card_h - 66}" rx="12" fill="{SURFACE_E}" stroke="{BORDER_L}" stroke-width="1"/>
{doc_rows}</svg>''')


# ══════════════════════════════════════════════════════════════════════
# 10. FOOTER — Bottom bar
# ══════════════════════════════════════════════════════════════════════
write("footer.svg", f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} 80" width="{W}" height="80">
  <defs>
    <linearGradient id="ftGrad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="{ACCENT}" stop-opacity="0"/>
      <stop offset="50%" stop-color="{ACCENT}" stop-opacity="0.3"/>
      <stop offset="100%" stop-color="{ACCENT}" stop-opacity="0"/>
    </linearGradient>
  </defs>
  <rect width="{W}" height="80" fill="{BG}"/>
  <rect x="100" y="0" width="{W - 200}" height="1" fill="url(#ftGrad)"/>

  <!-- Logo mark -->
  <g transform="translate({W//2 - 12}, 20)">
    <path d="M2,1 Q8,0 11,4 Q13,7 12,10 Q11,12 10,13"
          stroke="{ACCENT}" stroke-width="1.5" fill="none" stroke-linecap="round" opacity="0.3"/>
    <path d="M3,7 Q8,5 12,8"
          stroke="{ACCENT}" stroke-width="1" fill="none" stroke-linecap="round" opacity="0.2"/>
    <circle cx="12" cy="10" r="1" fill="{AMBER}" opacity="0.3"/>
  </g>

  <text x="{W//2}" y="52" text-anchor="middle" font-family="Inter, system-ui, sans-serif" font-size="11" fill="{TEXT3}">
    Free and open source under the GNU General Public License v3.0
  </text>
  <text x="{W//2}" y="68" text-anchor="middle" font-family="Inter, system-ui, sans-serif" font-size="10" fill="{TEXT3}" opacity="0.5">
    © 2022–2023 Yaroslav Shuliak (original Kanji Dojo) · Kaiteyo is independently developed
  </text>
</svg>''')


print(f"\n✨ Generated {len(list(OUT.iterdir()))} SVG assets in {OUT}/")
