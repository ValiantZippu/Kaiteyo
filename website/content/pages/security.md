---
title: Security
description: How Kaiteyo handles security — reporting vulnerabilities, our security posture, and what the app does and doesn't do with your data.
---

## Reporting a vulnerability

If you find a security issue in Kaiteyo, please report it privately rather than in a public issue:

- **Email:** open a [security advisory on GitHub](https://github.com/ValiantZippu/Kaiteyo/security/advisories/new) (preferred), or use the GitHub issue tracker with the `security` label for non-sensitive reports.
- Please include: the affected version, a description of the issue, steps to reproduce, and impact. You'll get an acknowledgement and we'll coordinate disclosure.

We treat security reports seriously and respond as quickly as we can.

## Security posture

- **Local-first.** Your study data, decks and progress are stored on your device. There is no cloud database of user content, and no account is required.
- **No tracking.** The app does not phone home; there are no analytics or telemetry in the application.
- **Open source.** The [entire codebase](https://github.com/ValiantZippu/Kaiteyo) is public — security through visibility, auditable by anyone.
- **Local integrations.** Optional integrations (such as AnkiConnect) talk only to software you run on your own machine over localhost.

## What the web trial stores

The web trial is a static, client-side page. Anything you do there — decks, reviews, writing practice — is stored in your browser's localStorage and cleared when you clear site data. Nothing is transmitted to any server.

## Privacy

For the full picture of what data Kaiteyo stores and how it's handled, see the [privacy policy](/privacy/) and the in-repository [privacy documentation](/docs/security/privacy/).
