# Kaiteyo (書いてよ) — Project Vision

> **Revision note (2026-08)**: the product vision has been expanded into the Master
> Blueprint. **The canonical vision is now [`docs/product/VISION.md`](../product/VISION.md)**
> and the full specification is [`docs/product/PRODUCT.md`](../product/PRODUCT.md) (MASTER
> §0–§88). This file keeps the original mission statement and the non-goals, revised
> below for consistency with the blueprint. Where this file and the blueprint differ,
> the blueprint wins.

## What is Kaiteyo?

Kaiteyo is a premium, cross-platform Japanese language learning application — a **connected
Japanese language ecosystem** (dictionary, kanji/kana/vocabulary/grammar/sentence systems,
learning platform with library + SRS study, media center with Yomitan-style glossing and
ASBPlayer-style mining, Anki/AnkiConnect integration, statistics, exams, knowledge graph,
curriculum, and — as target architecture — an actual 3D Japanese-learning game world, the
Journey). Originally a fork of Kanji Dojo, Kaiteyo has been completely redesigned with a
focus on desktop-first UX, beautiful craftsmanship, and a cohesive design system.

The name "Kaiteyo" (書いてよ) is Japanese for "write it!" — an invitation to practice and engage with the language actively.

## Target Audience

- **Self-learners** studying Japanese independently
- **Students** in formal Japanese language courses
- **JLPT candidates** preparing for N5 through N1
- **Writers** who need kanji lookup and stroke practice
- **Polyglots** who use multiple learning systems

## Philosophy

1. **Craft over features** — Every pixel, animation, and interaction should feel intentional.
2. **Desktop first** — Most serious study happens at a desk. The desktop experience is the primary focus.
3. **Respect the learner** — No gamification gimmicks. Clean, professional interfaces that treat users as capable adults.
4. **Offline by default** — Learning should not depend on internet connectivity.
5. **Open source** — Transparency builds trust. The community can audit, contribute, and fork.

## Goals

- Provide the most polished Japanese learning experience on desktop
- Maintain full offline functionality
- Support spaced repetition with a scientifically sound algorithm
- Offer deep customization through the Appearance Studio
- Keep the app responsive and lightweight

## Long-Term Vision

- v1.x: Establish desktop excellence with Kaiteyo branding, theming, and window experience
- v2.x: Dashboard redesign, learning analytics, progress insights
- v3.x: Cloud sync, community features, shared decks
- v4.x: AI-assisted learning paths, intelligent review scheduling

## What Kaiteyo is NOT (revised against the Master Blueprint)

- **NOT a gamified language app** — points/badges/streaks are never the core loop. The
  Journey is an *actual game* whose mechanics are exploration/discovery/collection —
  never XP grinding (MASTER §21–§22, NODE §86/§116).
- **NOT a mobile-first app** — mobile is supported and shares the core engine; desktop is
  primary.
- **NOT merely a dictionary app** — the dictionary is a major *subsystem* of the
  ecosystem, not the whole product (MASTER §11).
- **NOT a social network** — no friend walls or sharing feeds; community features are
  strictly opt-in and secondary (MASTER §65).
- **NOT a replacement for a textbook** — it is a companion that connects media,
  reference, and practice; the curriculum system grows into structured courses
  (MASTER §29).
- **NOT a collection of disconnected applications** — dictionary, media, study, and game
  share one knowledge model (MASTER §1).
