# Uninstall — Data Preservation Contract

The single most important rule: **an uninstaller may remove application files,
but it never silently removes user data.** Kaiteyo's data is the user's — decks,
databases, settings, study history, media, imported content.

## The two trees

| Tree | What lives there | Uninstall behavior |
|---|---|---|
| **Application files** | Program Files / `/usr/lib/kaiteyo` / `/opt` / APK | Removed by uninstall |
| **User data** | `%LOCALAPPDATA%\Kaiteyo` / `~/.kaiteyo` / XDG dirs / Android sandbox | **Preserved unless the user explicitly chooses removal** |

Because the two are never co-located, upgrades and uninstalls can treat them
independently.

## Uninstall options (Windows)

The Inno uninstaller presents explicit choices:

| Choice | Removes |
|---|---|
| **Keep my study data** (default) | Application + shortcuts + registry entries only |
| **Remove everything** | The above **plus** decks, database, settings, study history, media, user-created content — with a plain-language list of what will be deleted before the action |

Destructive operations are always explicit, labelled, and default to safe.

## Uninstall options (Linux)

- `apt remove` / `dnf remove` / `pacman -R` / `flatpak uninstall` remove the
  application. **User data in `~/.kaiteyo` / XDG dirs is never touched** by
  package managers.
- `apt purge` removes config files under `/etc` (if any existed); `~/.kaiteyo`
  user data is still preserved — deleting it requires an explicit user action.

## Android

Uninstalling from the launcher removes the app; Android offers "clear data"
separately and the store never deletes user data on update. Backup/restore
paths are documented in `docs/architecture/backup.md`.

## Reset vs uninstall

Kaiteyo provides a documented **reset** path that is distinct from uninstall:

| Action | Scope |
|---|---|
| UI reset | Re-run onboarding / restore appearance defaults — settings only |
| Database reset | Clear study/database state — still a user-confirmed action |
| Complete data deletion | Remove everything in the data dir — explicit, labelled |

Users should never need to uninstall to fix a broken state, and uninstall must
never be their only "backup" mechanism — see [backup](faq.md#backup) guidance.

## Uninstall → data preservation guarantee

- Uninstaller **never** deletes: decks, databases, settings, study history,
  media, imported dictionaries, photos, or any user-created content — unless
  the user selected the explicit "Remove everything" option.
- The uninstaller tells the user where their data remains after uninstall
  ("Your study data is kept at …"), so reinstalling later restores everything.
