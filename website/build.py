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
    # Desktop screenshots generated from docs/screenshots by copy_assets();
    # captured with scripts/capture-window-shell.sh.
    "desktop": [
        {"file": "window-shell.png", "caption": "Window shell — custom title bar and floating launcher"},
        {"file": "launcher-menu.png", "caption": "Launcher quick controls"},
        {"file": "launchpad-overlay.png", "caption": "Launchpad tile grid"},
        {"file": "launchpad-window-strip.png", "caption": "Launchpad window controls"},
    ],
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
    source_dir = pathlib.Path(source_rel).parent
    target = (source_dir / href.split("#")[0]).resolve()
    try:
        target = target.relative_to(DOCS_SOURCE.resolve())
    except ValueError:
        return href
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
    source = DOCS_SOURCE / "planning" / "CHANGELOG.md"
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
# Builder
# ---------------------------------------------------------------------------

def copy_assets():
    target = DIST_DIR / "assets"
    shutil.rmtree(target, ignore_errors=True)
    shutil.copytree(ASSET_DIR, target)

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
        for png in sorted(desktop_source.glob("*.png")):
            shutil.copy2(png, desktop_target / png.name)


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

    # --- Search index ---
    search_path = DIST_DIR / "assets" / "search"
    search_path.mkdir(parents=True, exist_ok=True)
    (search_path / "index.json").write_text(
        json.dumps(SEARCH_INDEX, ensure_ascii=False, indent=0), encoding="utf-8"
    )
    print(f"  search index: {len(SEARCH_INDEX)} entries")

    # --- Templates ---
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
