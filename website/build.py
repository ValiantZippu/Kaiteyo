#!/usr/bin/env python3
"""
Kaiteyo website builder.

Generates the complete static website into ./dist from:

  config/            site, navigation, theme, and documentation config
  content/           hand-written pages, wiki articles, FAQ entries
  templates/         Jinja2 templates (layouts + partials)
  assets/            styles, scripts, fonts, icons, images (copied verbatim)
  ../docs            repository documentation (rendered, never duplicated)

Dependencies (build-time only): jinja2, markdown, pygments.

Usage:
    python build.py            # build into dist/
    python build.py --serve    # build, then serve dist/ on http://localhost:8000
"""

from __future__ import annotations

import html
import json
import pathlib
import re
import shutil
import sys
import time
from datetime import date

import markdown
import pygments
from jinja2 import Environment, FileSystemLoader, select_autoescape
from markdown.extensions.toc import TocExtension

ROOT = pathlib.Path(__file__).resolve().parent
CONFIG_DIR = ROOT / "config"
CONTENT_DIR = ROOT / "content"
TEMPLATE_DIR = ROOT / "templates"
ASSET_DIR = ROOT / "assets"
DOCS_SOURCE = ROOT.parent / "docs"
DIST_DIR = ROOT / "dist"

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

def load_json(path: pathlib.Path) -> dict:
    with open(path, encoding="utf-8") as f:
        return json.load(f)


SITE = load_json(CONFIG_DIR / "site.json")
NAVIGATION = load_json(CONFIG_DIR / "navigation.json")
THEMES = load_json(CONFIG_DIR / "themes.json")
DOCUMENTATION = load_json(CONFIG_DIR / "documentation.json")

BASE_PATH = SITE["basePath"]
if not BASE_PATH.endswith("/"):
    BASE_PATH += "/"

YEAR = date.today().year

# Cache-busting query for asset URLs: changes on every build so a rebuilt
# site never serves stale CSS/JS from a visitor's browser cache.
ASSET_QUERY = str(int(time.time()))

# Captions for the desktop gallery (docs/screenshots). Files without an
# entry fall back to a prettified filename.
DESKTOP_SHOT_CAPTIONS = {
    "window-shell": "Window shell — custom title bar and floating launcher",
    "launcher-menu": "Launcher quick controls",
    "launchpad-overlay": "Launchpad tile grid",
    "launchpad-window-strip": "Launchpad window controls",
}

# Phone screenshots copied from the repository by copy_assets().
SCREENSHOTS = {
    "phone": [
        {"file": f"{n}.png", "caption": caption}
        for n, caption in enumerate(
            [
                "Dashboard",
                "Deck details",
                "Study — card",
                "Study — multiple choice",
                "Write kanji",
                "Decks browser",
            ],
            start=1,
        )
    ],
    # Desktop screenshots generated from docs/screenshots by copy_assets()
    # (captured with scripts/capture-window-shell.sh). The list is refreshed
    # after copying so the gallery only ever shows files that exist.
    "desktop": [],
}


def url(path: str) -> str:
    """Turn a site-absolute path (/docs/x/) into a basePath-relative URL."""
    return BASE_PATH + path.lstrip("/")


# ---------------------------------------------------------------------------
# Markdown
# ---------------------------------------------------------------------------

def make_markdown() -> markdown.Markdown:
    extensions = [
        "fenced_code",
        "tables",
        "attr_list",
        "md_in_html",
        "sane_lists",
        TocExtension(permalink=False, toc_depth="2-4"),
        "codehilite",
    ]
    extension_configs = {
        "codehilite": {
            "guess_lang": False,
            "css_class": "codehilite",
            "linenums": False,
            "noclasses": False,
        },
    }
    return markdown.Markdown(extensions=extensions, extension_configs=extension_configs)


MD = make_markdown()
TOC_TEMPLATE = "<div class='toc-contents'>%s</div>"


def parse_frontmatter(text: str) -> tuple[dict, str]:
    if not text.startswith("---"):
        return {}, text
    match = re.match(r"^---\s*\n(.*?)\n---\s*\n", text, re.DOTALL)
    if not match:
        return {}, text
    frontmatter = {}
    for line in match.group(1).splitlines():
        if ":" in line:
            key, value = line.split(":", 1)
            frontmatter[key.strip()] = value.strip().strip('"').strip("'")
    return frontmatter, text[match.end():]


def prettify_title(filename: str) -> str:
    """docs/development/CODING_STANDARDS.md -> 'Coding Standards'."""
    stem = re.sub(r"^\d+_", "", pathlib.Path(filename).stem)
    stem = stem.replace("_", " ").replace("-", " ")
    words = [w for w in stem.split(" ") if w]
    if not words:
        return filename
    return " ".join(w[:1].upper() + w[1:] for w in words)


def extract_h1(text: str) -> str | None:
    for line in text.splitlines():
        if line.startswith("# "):
            return line[2:].strip()
    return None


def extract_plain_text(html_source: str, limit: int = 260) -> str:
    text = re.sub(r"<[^>]+>", " ", html_source)
    text = html.unescape(re.sub(r"\s+", " ", text)).strip()
    return text[:limit]


# Documentation link rewriting: resolve relative .md links to site pages
# (or raw GitHub links for unpublished files), and prefix the base path
# on every internal absolute link.

DOC_PAGES = {}  # source path (relative to docs/) -> url


def resolve_doc_link(href: str, source_rel: str) -> str:
    if "://" in href or href.startswith("mailto:"):
        return href
    if href.startswith("#"):
        return href
    if href.startswith("/"):
        return url(href.lstrip("/"))
    # source_rel is relative to DOCS_SOURCE, so resolve the target against
    # the docs root — not the build's cwd — or doc-to-doc links break.
    source_dir = (DOCS_SOURCE / pathlib.Path(source_rel).parent).resolve()
    target = (source_dir / href.split("#")[0]).resolve()
    try:
        target = target.relative_to(DOCS_SOURCE.resolve())
    except ValueError:
        # Link leaves the docs tree (e.g. ../CONTRIBUTING.md at the repo
        # root). Point it at the repository instead of leaving it broken.
        try:
            outside = target.relative_to(DOCS_SOURCE.resolve().parent)
        except ValueError:
            return href
        return SITE["repository"] + "/blob/develop/" + str(outside).replace("\\", "/") + (
            ("#" + href.split("#", 1)[1]) if "#" in href else ""
        )
    if str(target) in DOC_PAGES:
        return DOC_PAGES[str(target)] + ("#" + href.split("#", 1)[1] if "#" in href else "")
    raw_root = DOCUMENTATION.get("rawLinkRoot", "")
    return raw_root + str(target).replace("\\", "/") + (
        ("#" + href.split("#", 1)[1]) if "#" in href else ""
    )


def render_markdown(text: str, source_rel: str | None = None) -> tuple[str, list, str]:
    """Render markdown to HTML.

    Returns (html, toc_headings, toc_html). Adds heading ids via the toc
    extension and rewrites internal links relative to basePath.
    """
    MD.reset()
    body = MD.convert(text)

    def fix_link(match):
        href = match.group(1)
        if source_rel and not href.startswith(("/", "http://", "https://", "mailto:", "#")):
            href = resolve_doc_link(href, source_rel)
        elif href.startswith("/"):
            href = url(href.lstrip("/"))
        return f'href="{href}"'

    body = re.sub(r'href="([^"]+)"', fix_link, body)

    def fix_src(match):
        src = match.group(1)
        if src.startswith("/"):
            src = url(src.lstrip("/"))
        return f'src="{src}"'

    body = re.sub(r'src="([^"]+)"', fix_src, body)

    toc_html = MD.toc if hasattr(MD, "toc") else ""
    toc_headings = []
    toc_html = toc_html.replace("<div class='toc-contents'>", "").replace("</div>", "")
    for level, title, ident in re.findall(
        r'<li class="([^"]*)"><a href="#([^"]+)">(.*?)</a>', toc_html
    ):
        depth = int(level.split("_")[-1]) if "_" in level else 3
        toc_headings.append({"level": depth, "title": html.unescape(title), "id": ident})

    # Wrap codehilite output in the Kaiteyo code block component.
    def wrap_code(match):
        classes = match.group(1) or ""
        lang_match = re.search(r"language-([\w-]+)", classes)
        lang = lang_match.group(1) if lang_match else "text"
        code = match.group(2)
        header = (
            '<div class="code-block-header">'
            f'<span class="code-block-lang">{html.escape(lang)}</span>'
            '<button class="copy-button" type="button" aria-label="Copy code">'
            '<svg class="icon" aria-hidden="true"><use href="#icon-copy"/></svg> Copy'
            "</button></div>"
        )
        return f'<div class="code-block">{header}{code}</div>'

    body = re.sub(
        r'<div class="codehilite(?: ([^"]*))?">(.*?)</div>', wrap_code, body, flags=re.DOTALL
    )
    return body, toc_headings, ""


def first_paragraph_plain(md_text: str) -> str:
    for line in md_text.splitlines():
        stripped = line.strip()
        if stripped and not stripped.startswith(("#", ">", "-", "!", "|")):
            return re.sub(r"[*_`\[\]]", "", stripped)[:260]
    return ""


# ---------------------------------------------------------------------------
# Pages
# ---------------------------------------------------------------------------

PAGES: list[dict] = []          # every renderable page
SEARCH_INDEX: list[dict] = []


def render_page(
    url_path: str,
    *,
    title: str,
    description: str = "",
    layout: str = "page.html",
    content_html: str = "",
    toc: list | None = None,
    breadcrumbs: list | None = None,
    prev: dict | None = None,
    next_page: dict | None = None,
    source_path: str | None = None,
    hide_title: bool = False,
    search: bool = True,
    search_type: str = "page",
    search_section: str = "",
    **extra,
) -> dict:
    page = {
        "url": url_path,
        "title": title,
        "description": description,
        "layout": layout,
        "content_html": content_html,
        "toc": toc or [],
        "breadcrumbs": breadcrumbs,
        "prev": prev,
        "next": next_page,
        "source_path": source_path,
        "hide_title": hide_title,
        "page_title": title,
        "page_description": description,
        "page_url": url_path,
        **extra,
    }
    PAGES.append(page)
    if search and title:
        SEARCH_INDEX.append(
            {
                "type": search_type,
                "title": title,
                "url": url(url_path),
                "section": search_section,
                "excerpt": description,
                "icon": "file",
            }
        )
    return page


def render_page_file(md_path: pathlib.Path, url_path: str, *, layout_hint: str | None = None,
                     breadcrumbs: list | None = None, prev: dict | None = None,
                     next_page: dict | None = None, search_type: str = "page",
                     search_section: str = "", screenshots: dict | None = None) -> dict:
    text = md_path.read_text(encoding="utf-8")
    frontmatter, md_text = parse_frontmatter(text)
    body, toc, _ = render_markdown(md_text)
    title = frontmatter.get("title") or extract_h1(md_text) or prettify_title(md_path.name)
    description = frontmatter.get("description", "")
    layout = layout_hint or frontmatter.get("layout", "page.html")
    if layout and not layout.endswith(".html"):
        layout += ".html"
    return render_page(
        url_path,
        title=title,
        description=description,
        layout=layout,
        content_html=body,
        toc=toc,
        breadcrumbs=breadcrumbs,
        prev=prev,
        next_page=next_page,
        source_path=frontmatter.get("source_path"),
        hide_title=frontmatter.get("hide_title") == "true",
        search=frontmatter.get("search", "true") != "false",
        search_type=search_type,
        search_section=search_section,
        raw_content=md_text,
        screenshots=screenshots,
    )


# ---------------------------------------------------------------------------
# Documentation pipeline
# ---------------------------------------------------------------------------

def build_documentation():
    sections = []
    for section in DOCUMENTATION["sections"]:
        files = list(section.get("files", []))
        if section.get("directory"):
            directory = (DOCS_SOURCE / section["directory"]).resolve()
            if directory.is_dir():
                for candidate in sorted(directory.glob("*.md")):
                    rel = str(candidate.relative_to(DOCS_SOURCE.resolve())).replace("\\", "/")
                    if candidate.name.lower() == "readme.md":
                        continue
                    if rel not in DOCUMENTATION["internalOnly"]:
                        files.append(rel)
        pages = []
        for rel in files:
            source = (DOCS_SOURCE / rel).resolve()
            if not source.is_file():
                print(f"  ! missing doc file: {rel}")
                continue
            slug = pathlib.Path(rel).stem.lower().replace(" ", "-")
            page_url = f"docs/{section['id']}/{slug}/"
            DOC_PAGES[rel.replace("\\", "/")] = url(page_url)
            pages.append({"rel": rel, "slug": slug, "url": page_url, "source": source})
        sections.append({"section": section, "pages": pages})

    # Register every doc page so cross-links resolve before rendering.
    for entry in sections:
        for page in entry["pages"]:
            DOC_PAGES[page["rel"]] = url(page["url"])

    flat = [{"section": s["section"], "page": p} for s in sections for p in s["pages"]]

    # Pass 1: read every document, compute titles and descriptions.
    for item in flat:
        entry = item["page"]
        text = entry["source"].read_text(encoding="utf-8")
        frontmatter, md_text = parse_frontmatter(text)
        item["text"] = text
        item["frontmatter"] = frontmatter
        item["md_text"] = md_text
        item["title"] = (
            frontmatter.get("title")
            or extract_h1(md_text)
            or prettify_title(pathlib.Path(entry["rel"]).name)
        )
        entry["title"] = item["title"]
        item["description"] = frontmatter.get(
            "description", first_paragraph_plain(md_text)
        )

    # Pass 2: render each document with known neighbours for prev/next.
    for index, item in enumerate(flat):
        entry = item["page"]
        body, toc, _ = render_markdown(item["md_text"], source_rel=entry["rel"])
        prev = flat[index - 1] if index > 0 else None
        next_page = flat[index + 1] if index < len(flat) - 1 else None
        render_page(
            entry["url"],
            title=item["title"],
            description=item["description"],
            layout="docs.html",
            content_html=body,
            toc=toc,
            breadcrumbs=[
                {"title": "Documentation", "url": "docs/"},
                {"title": item["section"]["title"], "url": f"docs/{item['section']['id']}/"},
                {"title": item["title"], "url": None},
            ],
            prev={"title": prev["title"], "url": prev["page"]["url"]} if prev else None,
            next_page={"title": next_page["title"], "url": next_page["page"]["url"]} if next_page else None,
            source_path=entry["rel"],
            search=True,
            search_type="doc",
            search_section=item["section"]["title"],
            docs_tree=sections,
            current_section=item["section"]["id"],
            current_url=entry["url"],
        )
        # Search: also index the first 3 section headings for deeper results.
        for heading in toc[:3]:
            SEARCH_INDEX.append(
                {
                    "type": "doc",
                    "title": f"{heading['title']} — {item['title']}",
                    "url": url(entry["url"]) + f"#{heading['id']}",
                    "section": item["section"]["title"],
                    "excerpt": heading["title"],
                    "icon": "book",
                }
            )

    # Section index pages — every docs breadcrumb points at
    # docs/{section_id}/, so each section gets a real landing page that
    # lists its documents (with descriptions and prev/next through the
    # section). Without this, section roots 404 on the deployed site.
    descriptions = {item["page"]["url"]: item["description"] for item in flat}
    for entry in sections:
        section = entry["section"]
        pages = entry["pages"]
        if not pages:
            continue
        section_url = f"docs/{section['id']}/"
        cards = "\n".join(
            f"<a class='card card-hover link-card' href='{url(page['url'])}' "
            f"style='display:block; padding: var(--space-5); margin-bottom: var(--space-4); text-decoration:none'>"
            f"<div class='card-title' style='margin-bottom: var(--space-2)'>{html.escape(page['title'])}</div>"
            f"<p class='card-description'>{html.escape(descriptions.get(page['url'], ''))}</p></a>"
            for page in pages
        )
        render_page(
            section_url,
            title=section["title"],
            description=section.get("description", ""),
            layout="docs.html",
            content_html=cards,
            breadcrumbs=[
                {"title": "Documentation", "url": "docs/"},
                {"title": section["title"], "url": None},
            ],
            search=False,
            docs_tree=sections,
            current_section=section["id"],
            current_url=section_url,
        )

    return sections


# ---------------------------------------------------------------------------
# Special pages: FAQ, shortcuts, gallery, changelog
# ---------------------------------------------------------------------------

def build_faq():
    faq_dir = CONTENT_DIR / "faq"
    categories = {}
    for md_file in sorted(faq_dir.glob("*.md")):
        text = md_file.read_text(encoding="utf-8")
        frontmatter, md_text = parse_frontmatter(text)
        category = frontmatter.get("category", "General")
        question = frontmatter.get("title") or md_file.stem
        body, _, _ = render_markdown(md_text)
        categories.setdefault(category, []).append(
            {"question": question, "body": body, "id": md_file.stem.lower().replace("_", "-")}
        )
        SEARCH_INDEX.append(
            {
                "type": "faq",
                "title": question,
                "url": url("faq/") + f"#faq-{md_file.stem.lower().replace('_', '-')}",
                "section": category,
                "excerpt": first_paragraph_plain(md_text),
                "icon": "help",
            }
        )
    return categories


def build_wiki():
    wiki_dir = CONTENT_DIR / "wiki"
    articles = []
    for md_file in sorted(wiki_dir.glob("*.md")):
        text = md_file.read_text(encoding="utf-8")
        frontmatter, md_text = parse_frontmatter(text)
        slug = md_file.stem.lower().replace("_", "-")
        title = frontmatter.get("title") or prettify_title(md_file.stem)
        description = frontmatter.get("description", "")
        body, toc, _ = render_markdown(md_text)
        articles.append(
            {
                "slug": slug,
                "title": title,
                "description": description,
                "excerpt": first_paragraph_plain(md_text),
                "body": body,
                "toc": toc,
                "frontmatter": frontmatter,
            }
        )

    if not articles:
        return []

    # Wiki index page.
    render_page(
        "wiki/",
        title="Wiki",
        description="Short, focused articles about Japanese study, the app, and how things work.",
        layout="page.html",
        content_html="\n".join(
            f"<section class='card card-hover' style='padding: var(--space-5)'>"
            f"<h2 style='font-size: var(--text-lg); margin: 0 0 var(--space-2)'><a href='{url('wiki/') + a['slug']}/'>{a['title']}</a></h2>"
            f"<p class='text-secondary' style='margin: 0'>{html.escape(a['excerpt'])}</p></section>"
            for a in articles
        ),
        search=False,
    )

    # Individual articles with prev/next.
    for index, article in enumerate(articles):
        prev = articles[index - 1] if index > 0 else None
        next_page = articles[index + 1] if index < len(articles) - 1 else None
        render_page(
            f"wiki/{article['slug']}/",
            title=article["title"],
            description=article["description"],
            layout="page.html",
            content_html=article["body"],
            toc=article["toc"],
            breadcrumbs=[
                {"title": "Wiki", "url": "wiki/"},
                {"title": article["title"], "url": None},
            ],
            prev={"title": prev["title"], "url": f"wiki/{prev['slug']}/"} if prev else None,
            next_page={"title": next_page["title"], "url": f"wiki/{next_page['slug']}/"} if next_page else None,
            search=True,
            search_type="wiki",
        )
        SEARCH_INDEX.append(
            {
                "type": "wiki",
                "title": article["title"],
                "url": url(f"wiki/{article['slug']}/"),
                "section": "Wiki",
                "excerpt": article["excerpt"],
                "icon": "compass",
            }
        )
    return articles


def build_guides():
    """Editorial learning guides — content/guides/*.md, rendered with an
    index page and prev/next navigation like the wiki."""
    guides_dir = CONTENT_DIR / "guides"
    articles = []
    for md_file in sorted(guides_dir.glob("*.md")):
        text = md_file.read_text(encoding="utf-8")
        frontmatter, md_text = parse_frontmatter(text)
        slug = md_file.stem.lower().replace("_", "-")
        title = frontmatter.get("title") or prettify_title(md_file.stem)
        description = frontmatter.get("description", "")
        body, toc, _ = render_markdown(md_text)
        articles.append(
            {
                "slug": slug,
                "title": title,
                "description": description,
                "excerpt": first_paragraph_plain(md_text),
                "body": body,
                "toc": toc,
                "frontmatter": frontmatter,
            }
        )

    if not articles:
        return []

    # Guides index page.
    render_page(
        "guides/",
        title="Guides",
        description="Editorial guides for learning Japanese with Kaiteyo — from your first kana to mining anime and reading your statistics.",
        layout="page.html",
        content_html="\n".join(
            f"<a class='card card-hover link-card' href='{url('guides/') + a['slug']}/' style='display:block; padding: var(--space-5); margin-bottom: var(--space-4); text-decoration:none'>"
            f"<div class='card-title' style='margin-bottom: var(--space-2)'>{html.escape(a['title'])}</div>"
            f"<p class='card-description'>{html.escape(a['excerpt'])}</p></a>"
            for a in articles
        ),
        search=False,
    )

    for index, article in enumerate(articles):
        prev = articles[index - 1] if index > 0 else None
        next_page = articles[index + 1] if index < len(articles) - 1 else None
        render_page(
            f"guides/{article['slug']}/",
            title=article["title"],
            description=article["description"],
            layout="page.html",
            content_html=article["body"],
            toc=article["toc"],
            breadcrumbs=[
                {"title": "Guides", "url": "guides/"},
                {"title": article["title"], "url": None},
            ],
            prev={"title": prev["title"], "url": f"guides/{prev['slug']}/"} if prev else None,
            next_page={"title": next_page["title"], "url": f"guides/{next_page['slug']}/"} if next_page else None,
            search=True,
            search_type="guide",
        )
        SEARCH_INDEX.append(
            {
                "type": "guide",
                "title": article["title"],
                "url": url(f"guides/{article['slug']}/"),
                "section": "Guides",
                "excerpt": article["excerpt"],
                "icon": "compass",
            }
        )
    return articles


def build_shortcuts():
    shortcuts_path = CONTENT_DIR / "shortcuts.json"
    if not shortcuts_path.is_file():
        return []
    shortcuts = json.loads(shortcuts_path.read_text(encoding="utf-8"))
    for group in shortcuts.get("groups", shortcuts):
        for shortcut in group["items"]:
            SEARCH_INDEX.append(
                {
                    "type": "shortcut",
                    "title": shortcut["action"],
                    "url": url("shortcuts/"),
                    "section": group["category"],
                    "excerpt": shortcut["description"],
                    "icon": "keyboard",
                }
            )
    return shortcuts


def build_changelog():
    # The changelog lives at the repository root (GitHub convention); the
    # website reads it from there directly.
    source = ROOT.parent / "CHANGELOG.md"
    if not source.is_file():
        return []
    text = source.read_text(encoding="utf-8")
    versions = []
    parts = re.split(r"^## ", text, flags=re.MULTILINE)
    for part in parts[1:]:
        lines = part.splitlines()
        version_match = re.match(r"v?([\d.]+)", lines[0].strip())
        if not version_match:
            continue
        version = version_match.group(1)
        sections = []
        current_title = ""
        for line in lines[1:]:
            stripped = line.strip()
            if stripped.startswith("### "):
                current_title = stripped[4:].strip()
            elif stripped.startswith("- ") and current_title:
                item = stripped[2:].strip()
                item = re.sub(r"\*\*?([^*]+)\*\*?", r"\1", item)
                sections.append({"title": current_title, "item": item})
        grouped = {}
        for entry in sections:
            grouped.setdefault(entry["title"], []).append(entry["item"])
        versions.append(
            {
                "version": version,
                "sections": [{"title": k, "items": v} for k, v in grouped.items()],
            }
        )
        excerpt = " ".join(item for s in sections for item in s["item"][:3])
        SEARCH_INDEX.append(
            {
                "type": "changelog",
                "title": f"Version {version}",
                "url": url("changelog/") + f"#v{version.replace('.', '-')}",
                "section": "Changelog",
                "excerpt": excerpt[:260],
                "icon": "history",
            }
        )
    return versions


# ---------------------------------------------------------------------------
# Project command center
# ---------------------------------------------------------------------------
# Renders the public project surfaces (status, kanban, roadmap, whiteboard,
# suggestions, decisions, activity, contributing) from real repository data.
# The kanban derives from docs/planning/MASTER_TODO.md (single source of
# truth, MASTER §44) — nothing is duplicated by hand. Read-only for now:
# the interactive layer's API contracts are documented in
# docs/website/API.md (MASTER §70/§74 — never fake persistence).

PROJECT_DIR = ROOT / "config" / "project"
MASTER_TODO_SOURCE = DOCS_SOURCE / "planning" / "MASTER_TODO.md"
DECISIONS_DIR = DOCS_SOURCE / "architecture" / "decisions"

STATUS_EMOJI = {
    "✅": "DONE",
    "🚧": "IN_PROGRESS",
    "🔬": "TARGET",
    "📋": "PLANNED",
    "🔍": "RESEARCH",
    "⛔": "BLOCKED",
    "💀": "PLACEHOLDER",
}
PRIORITY_EMOJI = {"🔴": "P0", "🟡": "P1", "🟢": "P2", "🔵": "P3"}

# Kanban columns: only columns that actually hold tasks are rendered
# (the spec forbids meaningless columns; no fabricated READY/REVIEW yet).
KANBAN_COLUMNS = [
    ("Backlog", ["PLANNED", "RESEARCH", "PLACEHOLDER"]),
    ("Planned (target)", ["TARGET"]),
    ("In progress", ["IN_PROGRESS"]),
    ("Blocked", ["BLOCKED"]),
    ("Completed", ["DONE"]),
]

# Primary documentation for a work package (epic) — used for card links.
PACKAGE_DOCS = {
    "P0": "/docs/decisions/0017-one-product-architecture/",
    "P2": "/docs/architecture/database/",
    "P3": "/docs/architecture/dictionary/",
    "P4": "/docs/architecture/language-model/",
    "P6": "/docs/architecture/language-model/",
    "P7": "/docs/features/library/",
    "P8": "/docs/architecture/study-engine/",
    "P9": "/docs/architecture/statistics/",
    "P10": "/docs/architecture/exams/",
    "P11": "/docs/architecture/media/",
    "P12": "/docs/architecture/mining/",
    "P14": "/docs/integrations/anki/",
    "P15": "/docs/integrations/anki/",
    "P16": "/docs/website/readme/",
    "P17": "/docs/platform/android/",
    "P18": "/docs/platform/windows/",
    "P19": "/docs/decisions/0018-game-engine-evaluation/",
    "P20": "/docs/architecture/node_architecture/",
    "P21": "/docs/architecture/node_architecture/",
    "P22": "/docs/architecture/node_architecture/",
    "P32": "/docs/testing/",
    "P36": "/docs/",
}

# Required knowledge per package, shown on the contributor page.
PACKAGE_KNOWLEDGE = {
    "P2": ["SQLDelight", "SQLite", "migrations", "SQLDelight .sq files"],
    "P3": ["dictionary formats (JMdict/Yomitan)", "indexing (FTS/trigram)", "search ranking"],
    "P4": ["kanji data (KanjiVG/KANJIDIC)", "stroke rendering"],
    "P8": ["SRS (FSRS)", "Kotlin", "SQLDelight"],
    "P9": ["event-driven design", "statistics aggregation"],
    "P11": ["media playback", "subtitle parsing", "dictionary", "UI", "async programming"],
    "P12": ["subtitle formats", "dictionary lookup", "card model"],
    "P19": ["game engines", "3D rendering", "performance profiling"],
    "P20": ["content packages", "world streaming", "LOD"],
    "P36": ["Python", "Jinja2", "static site builds", "JavaScript"],
}


def load_project_json(name: str) -> dict:
    path = PROJECT_DIR / name
    if not path.is_file():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def parse_master_todo() -> tuple[list, list]:
    """Parse docs/planning/MASTER_TODO.md into tasks and packages (epics)."""
    text = MASTER_TODO_SOURCE.read_text(encoding="utf-8")
    packages: list[dict] = []
    tasks: list[dict] = []
    current_package = None
    row_re = re.compile(
        r"^\|\s*(KT-[A-Z0-9]+-\d+)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|$"
    )
    for line in text.splitlines():
        pkg = re.match(r"^## (P\d+)\s*[—-]\s*(.+?)\s*$", line)
        if pkg:
            current_package = {
                "id": pkg.group(1),
                "title": pkg.group(2),
                "docs": PACKAGE_DOCS.get(pkg.group(1)),
            }
            packages.append(current_package)
            continue
        m = row_re.match(line)
        if m and current_package:
            id_, title, status_raw, pri_raw, deps_raw, accept_raw = m.groups()
            tasks.append({
                "id": id_,
                "title": title,
                "status": STATUS_EMOJI.get(status_raw, "UNKNOWN"),
                "priority": PRIORITY_EMOJI.get(pri_raw, ""),
                "deps": deps_raw,
                "acceptance": accept_raw,
                "package": current_package["id"],
            })
    for pkg in packages:
        pkg_tasks = [t for t in tasks if t["package"] == pkg["id"]]
        pkg["taskCount"] = len(pkg_tasks)
        pkg["openCount"] = sum(1 for t in pkg_tasks if t["status"] != "DONE")
        pkg["doneCount"] = pkg["taskCount"] - pkg["openCount"]
    return tasks, packages


def build_decisions() -> list:
    """Index ADRs (docs/architecture/decisions) and link the rendered docs pages."""
    decisions = []
    if not DECISIONS_DIR.is_dir():
        return decisions
    for md_file in sorted(DECISIONS_DIR.glob("*.md")):
        if md_file.name == "README.md":
            continue
        text = md_file.read_text(encoding="utf-8")
        title = ""
        status = "Unknown"
        for line in text.splitlines()[:8]:
            m = re.match(r"^# (ADR-\d+)[:：]?\s*(.*)$", line)
            if m and not title:
                title = (m.group(2).strip() or m.group(1))
            s = re.match(r"^\*\*Status\*\*:\s*(.+)$", line)
            if s:
                status = s.group(1).strip()
        stem = md_file.stem.lower()
        decisions.append({
            "id": stem,
            "title": title or md_file.stem,
            "status": status,
            "url": f"docs/decisions/{stem}/",
        })
    return decisions


def build_project(tasks: list, packages: list, decisions: list):
    systems = load_project_json("systems.json").get("systems", [])
    whiteboard = load_project_json("whiteboard.json")
    roadmap = load_project_json("roadmap.json")
    suggestions = load_project_json("suggestions.json")
    activity = load_project_json("activity.json")

    columns = []
    for label, statuses in KANBAN_COLUMNS:
        cards = [t for t in tasks if t["status"] in statuses]
        if cards:
            columns.append({"label": label, "cards": cards, "count": len(cards)})

    open_tasks = [t for t in tasks if t["status"] != "DONE"]
    blocked = [t for t in tasks if t["status"] == "BLOCKED"]
    high_priority = [t for t in open_tasks if t["priority"] == "P0"]
    top_tasks = sorted(
        [t for t in open_tasks if t["priority"]],
        key=lambda t: t["priority"],
    )[:8]
    good_first = [
        t for t in open_tasks
        if t["package"] in ("P16", "P29", "P30", "P32", "P36")
    ][:6]

    whiteboard_json = json.dumps(whiteboard, ensure_ascii=False)

    common = {
        "systems": systems,
        "packages": packages,
        "columns": columns,
        "roadmap_phases": roadmap.get("phases", []),
        "suggestion_workflow": suggestions.get("workflow", []),
        "suggestion_types": suggestions.get("types", []),
        "suggestion_items": suggestions.get("items", []),
        "activity_events": activity.get("events", []),
        "activity_captured": activity.get("captured", ""),
        "decisions": decisions,
        "open_tasks": len(open_tasks),
        "blocked_tasks": len(blocked),
        "high_priority": len(high_priority),
        "top_tasks": top_tasks,
        "good_first": good_first,
        "package_knowledge": PACKAGE_KNOWLEDGE,
    }

    render_page(
        "project/",
        title="Project",
        description="Kaiteyo's public command center — what is being built, what is planned, and what is done.",
        layout="project-index.html",
        search=True,
        search_type="page",
        search_section="Project",
        **common,
    )
    render_page(
        "project/kanban/",
        title="Kanban",
        description="What is happening now — the live task board derived from docs/planning/MASTER_TODO.md.",
        layout="kanban.html",
        search=True,
        search_type="page",
        search_section="Project",
        **common,
    )
    render_page(
        "project/roadmap/",
        title="Roadmap",
        description="Where Kaiteyo is going — the forward-looking plan, linked to kanban and documentation.",
        layout="roadmap.html",
        search=True,
        search_type="page",
        search_section="Project",
        **common,
    )
    render_page(
        "project/whiteboard/",
        title="Whiteboard",
        description="The Kaiteyo system, visually — an interactive architecture canvas. Pan, zoom, explore.",
        layout="whiteboard.html",
        search=True,
        search_type="page",
        search_section="Project",
        whiteboard_json=whiteboard_json,
        **common,
    )
    render_page(
        "project/suggestions/",
        title="Suggestions",
        description="Propose plans for Kaiteyo. The official plan is protected — accepted proposals become tracked work with provenance.",
        layout="suggestions.html",
        search=True,
        search_type="page",
        search_section="Project",
        **common,
    )
    render_page(
        "project/decisions/",
        title="Architecture Decisions",
        description="Why Kaiteyo is built the way it is — every recorded ADR.",
        layout="decisions.html",
        search=True,
        search_type="page",
        search_section="Project",
        **common,
    )
    render_page(
        "project/activity/",
        title="Activity",
        description="The project evolving — commits, decisions, releases, and documentation changes.",
        layout="activity.html",
        search=False,
        **common,
    )
    render_page(
        "project/contributing/",
        title="Contributing",
        description="What can you actually help with? Start here.",
        layout="contributing.html",
        search=True,
        search_type="page",
        search_section="Project",
        **common,
    )

    # Search index entries for project objects (real data only).
    for task in top_tasks + good_first:
        SEARCH_INDEX.append({
            "type": "task",
            "title": f"{task['id']} — {task['title']}",
            "url": url("project/kanban/") + f"#card-{task['id'].lower()}",
            "section": "Kanban",
            "excerpt": task["acceptance"][:160],
            "icon": "grid",
        })
    for system in systems:
        SEARCH_INDEX.append({
            "type": "system",
            "title": system["name"],
            "url": url("project/"),
            "section": "Project",
            "excerpt": system["summary"],
            "icon": "layers",
        })
    for decision in decisions:
        SEARCH_INDEX.append({
            "type": "decision",
            "title": decision["title"],
            "url": url(decision["url"]),
            "section": "Decisions",
            "excerpt": decision["status"],
            "icon": "scale",
        })
    for phase in roadmap.get("phases", []):
        for item in phase["items"]:
            SEARCH_INDEX.append({
                "type": "roadmap",
                "title": item["title"],
                "url": url("project/roadmap/") + f"#item-{item['id'].lower()}",
                "section": "Roadmap",
                "excerpt": item.get("note", "")[:160],
                "icon": "milestone",
            })

    print("  project command center: "
          f"{len(systems)} systems | {len(tasks)} tasks | {len(packages)} packages | "
          f"{len(decisions)} decisions | {len(roadmap.get('phases', []))} roadmap phases")


# ---------------------------------------------------------------------------
# Builder
# ---------------------------------------------------------------------------

BRAND_MARK_SOURCE = ROOT.parent / "installer" / "assets" / "brand" / "kaiteyo-mark.svg"


def write_brand_images(target: pathlib.Path):
    """Synthesize the site's favicon, app mark and social cover from the real
    Kaiteyo brand mark (installer/assets/brand/kaiteyo-mark.svg). The
    website/assets/images/ dir is gitignored and empty, so without this step
    every page ships broken image links."""
    images = target / "assets" / "images"
    images.mkdir(parents=True, exist_ok=True)
    if BRAND_MARK_SOURCE.is_file():
        mark = BRAND_MARK_SOURCE.read_text(encoding="utf-8")
        (images / "kaiteyo-mark.svg").write_text(mark, encoding="utf-8")
        (images / "favicon.svg").write_text(mark, encoding="utf-8")
        # Social cover: mark centered on the brand gradient with the wordmark.
        (images / "og-cover.svg").write_text(
            '<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630" viewBox="0 0 1200 630">'
            '<defs><linearGradient id="ogbg" x1="0" y1="0" x2="1" y2="1">'
            '<stop offset="0%" stop-color="#4f46e5"/><stop offset="100%" stop-color="#312e81"/>'
            '</linearGradient></defs>'
            '<rect width="1200" height="630" fill="url(#ogbg)"/>'
            '<g transform="translate(500 95) scale(0.28)">'
            + mark.replace('<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">', "<svg xmlns='http://www.w3.org/2000/svg' width='1024' height='1024' viewBox='0 0 1024 1024'>")
            + '</g>'
            '<text x="600" y="520" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" '
            'font-size="64" font-weight="700" fill="#ffffff" letter-spacing="10">KAITEYO</text>'
            '<text x="600" y="580" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" '
            'font-size="28" fill="#c7d2fe" letter-spacing="4">書いてよ — Write. Practice. Master.</text>'
            '</svg>',
            encoding="utf-8",
        )
        print("  brand images synthesized (kaiteyo-mark, favicon, og-cover)")


def copy_assets():
    target = DIST_DIR / "assets"
    shutil.rmtree(target, ignore_errors=True)
    shutil.copytree(ASSET_DIR, target)
    write_brand_images(DIST_DIR)

    # Phone screenshots from the repository (fastlane metadata).
    phone_source = (
        ROOT.parent
        / "fastlane"
        / "metadata"
        / "android"
        / "en-US"
        / "images"
        / "phoneScreenshots"
    )
    phone_target = target / "screenshots" / "phone"
    phone_target.mkdir(parents=True, exist_ok=True)
    if phone_source.is_dir():
        for png in sorted(phone_source.glob("*.png")):
            shutil.copy2(png, phone_target / png.name)

    # Desktop screenshots from the repository documentation (docs/screenshots).
    desktop_source = ROOT.parent / "docs" / "screenshots"
    desktop_target = target / "screenshots" / "desktop"
    desktop_target.mkdir(parents=True, exist_ok=True)
    if desktop_source.is_dir():
        for image in sorted(desktop_source.glob("*")):
            if image.suffix.lower() in (".png", ".jpg", ".jpeg", ".webp", ".svg"):
                shutil.copy2(image, desktop_target / image.name)

    # Refresh the desktop gallery from what actually exists (the repo may
    # only contain a subset — window-shell.svg today).
    desktop_gallery = []
    if desktop_target.is_dir():
        for image in sorted(desktop_target.iterdir()):
            desktop_gallery.append({
                "file": image.name,
                "caption": DESKTOP_SHOT_CAPTIONS.get(image.stem, image.stem.replace("-", " ").title()),
            })
    SCREENSHOTS["desktop"] = desktop_gallery

def make_env() -> Environment:
    env = Environment(
        loader=FileSystemLoader([str(TEMPLATE_DIR), str(TEMPLATE_DIR / "layouts")]),
        autoescape=select_autoescape(["html", "xml"]),
    )
    env.globals.update(
        basePath=BASE_PATH,
        site=SITE,
        navigation=NAVIGATION,
        themes=THEMES,
        year=YEAR,
        assetQuery=ASSET_QUERY,
        footer_columns=[
            {
                "title": group["title"],
                "items": [
                    {"title": item["title"], "url": item["url"], "external": False}
                    for item in group["items"]
                ],
            }
            for group in NAVIGATION["groups"]
        ]
        + [
            {
                "title": "Community",
                "items": [
                    {"title": item["title"], "url": item["url"], "external": True}
                    for item in NAVIGATION["external"]
                ],
            }
        ],
    )
    return env


def build():
    print(f"Kaiteyo website builder — base path: {BASE_PATH}")
    if DIST_DIR.exists():
        shutil.rmtree(DIST_DIR)
    DIST_DIR.mkdir(parents=True)

    copy_assets()
    print("  assets copied")

    sections = build_documentation()
    print(f"  documentation: {sum(len(s['pages']) for s in sections)} pages across {len(sections)} sections")

    faq_categories = build_faq()
    print(f"  faq: {sum(len(v) for v in faq_categories.values())} entries")

    shortcuts = build_shortcuts()
    changelog = build_changelog()
    print(f"  shortcuts: {sum(len(g['items']) for g in shortcuts.get('groups', shortcuts))} | changelog versions: {len(changelog)}")

    wiki_articles = build_wiki()
    print(f"  wiki: {len(wiki_articles)} articles")

    guide_articles = build_guides()
    print(f"  guides: {len(guide_articles)} articles")

    # --- Content pages ---
    page_dir = CONTENT_DIR / "pages"
    layouts_hint = {
        "index.md": "landing.html",
        "screenshots.md": "screenshots.html",
        "shortcuts.md": None,  # replaced by dedicated layout below
        "theme-gallery.md": "gallery.html",
        "changelog.md": None,  # replaced by dedicated layout below
        "faq.md": None,        # replaced by dedicated layout below
    }
    for md_file in sorted(page_dir.glob("*.md")):
        if md_file.name in ("shortcuts.md", "changelog.md", "faq.md"):
            continue
        slug = md_file.stem
        if slug == "index":
            page_url = "index.html"
        else:
            page_url = f"{slug}/"
        render_page_file(
            md_file,
            page_url,
            layout_hint=layouts_hint.get(md_file.name, "page.html"),
            search_type="page",
            screenshots=SCREENSHOTS,
        )

    # --- Special rendered pages ---
    if shortcuts:
        render_page(
            "shortcuts/",
            title="Keyboard Shortcuts",
            description="Every default keybinding in Kaiteyo — grouped, searchable, and configurable.",
            layout="shortcuts.html",
            content_html="",
            search=False,
            shortcuts=shortcuts,
        )
    if changelog:
        render_page(
            "changelog/",
            title="Changelog",
            description="Version history of Kaiteyo, maintained alongside the source code.",
            layout="changelog.html",
            content_html="",
            search=False,
            changelog=changelog,
        )
    render_page(
        "faq/",
        title="Frequently Asked Questions",
        description="Searchable answers to common questions about Kaiteyo.",
        layout="faq.html",
        content_html="",
        search=False,
        faq_categories=faq_categories,
    )

    # --- Project command center ---
    tasks, packages = parse_master_todo()
    decisions = build_decisions()
    build_project(tasks, packages, decisions)

    # --- Templates ---
    env = make_env()

    def canonical_path(page):
        return url(page["url"]) if page["url"] != "index.html" else url("")

    for page in PAGES:
        template = env.get_template(page["layout"])
        rendered = template.render(**page, canonical_path=canonical_path(page))
        out_path = DIST_DIR / page["url"]
        if out_path.suffix == "":
            out_path = out_path / "index.html"
        elif out_path.name == "index.html":
            out_path = out_path  # page url already ends with index.html
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(rendered, encoding="utf-8")

    # Docs index page (aggregates sections).
    docs_index_template = env.get_template("layouts/docs-index.html")
    docs_index = docs_index_template.render(
        page_title="Documentation",
        page_description="Everything about Kaiteyo — written in the repository, rendered here.",
        site=SITE,
        basePath=BASE_PATH,
        sections=sections,
    )
    (DIST_DIR / "docs" / "index.html").parent.mkdir(parents=True, exist_ok=True)
    (DIST_DIR / "docs" / "index.html").write_text(docs_index, encoding="utf-8")

    # --- Search index (after every page has registered) ---
    search_path = DIST_DIR / "assets" / "search"
    search_path.mkdir(parents=True, exist_ok=True)
    (search_path / "index.json").write_text(
        json.dumps(SEARCH_INDEX, ensure_ascii=False, indent=0), encoding="utf-8"
    )
    print(f"  search index: {len(SEARCH_INDEX)} entries")

    # --- Static output files ---
    write_static_outputs(env)

    print(f"  pages: {len(PAGES)}")
    print(f"Done -> {DIST_DIR}")


def write_static_outputs(env: Environment):
    site_url = SITE["url"].rstrip("/") + "/" + BASE_PATH.lstrip("/")

    # sitemap.xml
    urls = []
    for page in PAGES:
        if page["url"] == "index.html":
            loc = SITE["url"] + "/" + BASE_PATH.lstrip("/").rstrip("/")
        else:
            loc = SITE["url"] + "/" + BASE_PATH.lstrip("/") + page["url"].replace("index.html", "")
        urls.append(f"  <url><loc>{loc}</loc></url>")
    urls.append(f"  <url><loc>{SITE['url']}/</loc></url>")
    (DIST_DIR / "sitemap.xml").write_text(
        '<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n'
        + "\n".join(sorted(urls))
        + "\n</urlset>\n",
        encoding="utf-8",
    )

    # robots.txt
    (DIST_DIR / "robots.txt").write_text(
        f"User-agent: *\nAllow: /\nSitemap: {SITE['url']}/{BASE_PATH.lstrip('/')}sitemap.xml\n",
        encoding="utf-8",
    )

    # 404 page
    not_found = env.get_template("404.html").render(
        site=SITE, basePath=BASE_PATH, year=YEAR
    )
    (DIST_DIR / "404.html").write_text(not_found, encoding="utf-8")

    # 500 page
    server_error = env.get_template("500.html").render(
        site=SITE, basePath=BASE_PATH, year=YEAR
    )
    (DIST_DIR / "500.html").write_text(server_error, encoding="utf-8")

    # Offline page
    offline = env.get_template("offline.html").render(
        site=SITE, basePath=BASE_PATH, year=YEAR
    )
    (DIST_DIR / "offline.html").write_text(offline, encoding="utf-8")

    # RSS (changelog)
    changelog_html = (DIST_DIR / "changelog" / "index.html").read_text(encoding="utf-8")
    items = re.findall(r'<section class="timeline-item[^"]*" id="v([\d-]+)"[^>]*>(.*?)</section>', changelog_html, re.DOTALL)
    rss_items = []
    for version_id, body in items[:15]:
        version = version_id.replace("-", ".")
        title_match = re.search(r"<h2>v([\d.]+)</h2>", body)
        text = re.sub(r"<[^>]+>", " ", body)
        text = html.unescape(re.sub(r"\s+", " ", text)).strip()[:600]
        rss_items.append(
            f"    <item>\n"
            f"      <title>Kaiteyo v{version}</title>\n"
            f"      <link>{site_url}changelog/#v{version_id}</link>\n"
            f"      <guid>{site_url}changelog/#v{version_id}</guid>\n"
            f"      <description>{html.escape(text)}</description>\n"
            f"    </item>"
        )
    (DIST_DIR / "rss.xml").write_text(
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<rss version="2.0"><channel>\n'
        f"  <title>{html.escape(SITE['rss']['title'])}</title>\n"
        f"  <link>{site_url}changelog/</link>\n"
        f"  <description>{html.escape(SITE['rss']['description'])}</description>\n"
        f"  <lastBuildDate>{date.today().strftime('%a, %d %b %Y 00:00:00 +0000')}</lastBuildDate>\n"
        + "\n".join(rss_items)
        + "\n</channel></rss>\n",
        encoding="utf-8",
    )


def serve():
    import http.server

    handler = http.server.SimpleHTTPRequestHandler
    os_path = str(DIST_DIR)
    import os

    old = os.getcwd()
    os.chdir(os_path)
    try:
        print(f"Serving {os_path} → http://localhost:8000/{BASE_PATH.lstrip('/')}")
        http.server.HTTPServer(("127.0.0.1", 8000), handler).serve_forever()
    finally:
        os.chdir(old)


if __name__ == "__main__":
    build()
    if "--serve" in sys.argv:
        serve()
