---
title: Contributing
description: How to help with Kaiteyo — code, design, translation, documentation, or ideas.
---

<div class="prose-note">
  <svg class="icon" aria-hidden="true"><use href="#icon-contribute"/></svg>
  <strong>Looking for what to work on?</strong> The live contributor dashboard — real tasks,
  good-first picks, and required knowledge — lives in the
  <a href="/project/contributing/">Command Center → Contributing</a>.
  Full guidelines live in the repository —
  <a href="https://github.com/ValiantZippu/Kaiteyo/blob/develop/CONTRIBUTING.md" target="_blank" rel="noopener">CONTRIBUTING.md</a>.
</div>

## Ways to contribute

<div class="feature-grid">
  <div class="feature-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-code"/></svg></div>
    <h3>Code</h3>
    <p>Fix bugs, add features, or improve the Gradle/KMP tooling. Everything is Kotlin Multiplatform with Compose Multiplatform.</p>
  </div>
  <div class="feature-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-palette"/></svg></div>
    <h3>Design</h3>
    <p>Refine the design system, contribute themes, or review UI details against the documented design language.</p>
  </div>
  <div class="feature-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-font"/></svg></div>
    <h3>Documentation</h3>
    <p>The <code>docs/</code> folder is the source of truth for this website — fix a typo, clarify a guide, or translate.</p>
  </div>
  <div class="feature-card">
    <div class="feature-icon"><svg class="icon" aria-hidden="true"><use href="#icon-chat"/></svg></div>
    <h3>Community</h3>
    <p>Answer questions in Discussions, report crashes with detail, or suggest deck features informed by real study habits.</p>
  </div>
</div>

## Getting started

1. **Fork** the repository and create a branch from `develop`.
2. **Set up** the build — see the [Development Guide](/docs/development/developer_guide/) for requirements.
3. **Make your change**, keeping it focused. Small, reviewable PRs land fastest.
4. **Open a pull request** describing the motivation and the testing you did.

Before large changes, open a Discussion first — the maintainers can confirm the direction and avoid wasted work.

## Standards

- Branch from `develop`, not `main`.
- Follow the existing code style — the codebase uses ktfmt/ktlint conventions.
- Keep docs in `docs/` in sync when behavior changes.
- Test on at least one desktop platform and one mobile platform when touching shared code.
