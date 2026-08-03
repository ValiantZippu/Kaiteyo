---
title: Local API and integrations
description: Connect external tools like GameSentenceMiner to Kaiteyo with a local HTTP server.
---

Kaiteyo exposes a **local HTTP API** so external tools can integrate with your deck. It is off by default and only listens on `127.0.0.1`.

## Endpoints

- `GET /api/health` — server liveness check.
- `GET /api/status` — server and app status, including installed dictionaries.
- `POST /api/mine` — submit a mined card from an external tool.

## What it enables

- **GameSentenceMiner** — capture sentences from games and push them into Kaiteyo.
- Custom scripts and tools that want to create review cards programmatically.

## Safety

The server binds to localhost only and never exposes your deck or study data over the network. Everything runs on your machine, and the API keeps a record of the last request so you can see exactly what external tools sent.
