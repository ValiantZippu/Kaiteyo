---
category: Data & privacy
title: Is there an account or cloud sync?
---

No account is required — Kaiteyo is offline-first and everything works without one. Sync is currently **desktop-first and gist-based**: an opt-in GitHub gist transport (OAuth device flow) exists in the desktop suite, but there is no central Kaiteyo service. Backup and restore are the reliable cross-device path today: profile archives include your data, settings, and window state, and Anki `.apkg` files move decks between devices. See [the account documentation](/docs/architecture/account/) for the current state.
