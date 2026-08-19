---
title: Extend Kaiteyo with plugins
description: The plugin marketplace ships curated extensions; the sandbox denies anything it doesn't explicitly allow, so installing stays safe.
---

Kaiteyo's study surface is built in, but the edges are extensible. The Plugins workspace has two tabs: **Installed** (manage what you've enabled) and **Marketplace** (browse and install curated extensions). Everything you install runs behind a capability sandbox.

## The marketplace

The marketplace reads a curated index of community plugins hosted on GitHub — dictionary helpers, exporters, workflow tools. Each plugin declares:

- **Name, version, author, description**
- **A category and license**
- **Download and star counts** so you can judge popularity at a glance
- **A manifest** listing what it wants to do

One click installs, one click updates (the marketplace shows an **Update** badge when a newer version exists), and one toggle disables. The catalog is available offline with a featured fallback, so the tab is never empty.

## The sandbox — deny by default

Every plugin manifest is validated before install. The sandbox is **deny-by-default**: a plugin may only do what its declared permissions explicitly allow. Unknown or undeclared capabilities are rejected at install time — there's no silent "maybe it's fine" path.

Known capability names (reading history, dictionary access, media handling, and friends) pass the gate; anything else bounces the install. The validation logic is unit-tested, so the gate itself is a checked invariant rather than an aspiration.

## Managing what you have

- **Enable / disable** — flip a toggle, the plugin's commands and panels appear or disappear
- **Uninstall** — removes the plugin and its contributed surface (cards mined via it stay in your deck)
- **Version badges** — installed, enabled, disabled, and out-of-date states at a glance

## The honest limits

- The plugin model is **metadata + declared capabilities**, not arbitrary code execution — runtime sandboxing of downloaded bytecode stays off by design, gated by the plugin architecture decision record.
- The catalog is curated; publishing new plugins is a repo/PR flow, not a self-serve upload.

See the [plugin architecture documentation](/docs/integrations/plugins/) for the capability model, manifest schema, and validation rules.
