# Local HTTP API (desktop)

The desktop app runs a small **localhost HTTP server** (Ktor) so external tools and
scripts can integrate with Kaiteyo: query current media, mine cards, and control playback.
It is built for GameSentenceMiner-style workflows and automation.

> Implemented in `desktopApp/.../engine/api/LocalApiServer.kt`.

## Purpose

Give trusted local tools a stable, scriptable interface into the app's media and mining
state — without opening any network port to the outside world.

## Configuration

| Setting | Key | Default |
|---|---|---|
| Enable server | `media.api.enabled` | off |
| Port | `media.api.port` | fixed per install (persisted) |
| Bearer token | `media.api.token` | generated once per install, persisted |

## Authentication

- **Every endpoint except `/api/health` requires `Authorization: Bearer <token>`.**
- The token is generated once and stored in settings (`settings.json` under `~/.kaiteyo/`).
- `selfTest()` verifies liveness + auth.

## Endpoints

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/health` | GET | Liveness (no auth required) |
| `/api/status` | GET | Server/integration status incl. media state |
| `/api/mine` | POST | Create a card from a `IntegrationCardRequest` payload (mining) |
| `/api/media/current` | GET | Currently loaded media |
| `/api/media/subtitle` | GET | Current subtitle line |
| `/api/player/control` | POST | Playback control |
| `/api/player/seek` | POST | Seek to position |

## Security considerations

- Binds to localhost only; the bearer token blocks accidental exposure and local
  unauthenticated callers.
- The token is not encrypted at rest — it lives in the user's settings file with the same
  protections as other local data (see `SECURITY.md`).
- If you expose the port (e.g. via SSH tunneling), the bearer token is the only barrier —
  keep it secret.

## Development notes

- Settings persistence: `SettingsEngine` (`settings.json`).
- Integration hub UI: `IntegrationsView` — status cards + "Test connection" per
  integration (Local API, GameSentenceMiner, AnkiConnect, Text hook, Player WebSocket,
  System media keys).
