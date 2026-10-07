# NumericalStorage

![Java Version](https://img.shields.io/badge/Java-21-orange)
![Target](https://img.shields.io/badge/Target-Paper-blue)
![Typewriter](https://img.shields.io/badge/Typewriter-0.9.0--beta--177-purple)

Named numeric balances per player for **Typewriter**: levels, capacity, interest, a deposit/withdraw menu and PlaceholderAPI placeholders. Requires Typewriter `0.9.0-beta-177` on **Paper**, Java 21.

## Features

- Balances and levels persisted in Typewriter assets (Player NumericalStorage Artifact).
- Asynchronous writes and a short cache, no blocking I/O on the server threads.
- Deferred interest on a cron schedule, capped to the number of catch-up cycles.
- Per-player storage by default; optional per-profile storage through the MMOProfiles plugin.
- Menu built on the GuiAndDialogs extension, with `optionalButtons` (display-only items) on the menu and on each sub-menu.
- Transactions in `INTERNAL` mode (PlaceholderAPI + commands) or `VAULT` mode (Vault economy), and atomic transfer between two storages.

Mutations are serialized per artifact. Vault transactions compensate an external debit/credit if the persistent write fails; arbitrary commands must stay idempotent, since Typewriter cannot derive a reverse operation.

## Entries

| Category | Entry |
|---|---|
| Manifest | `numericalstorage_definition` |
| Artifact | `player_numericalstorage_artifact` |
| Actions | `numericalstorage_menu`, `numericalstorage_open_menu` |

Full field reference on the [wiki](https://docs.borntocraftstudio.net/extensions/free/numerical-storage/).

## Commands and permissions

| Command | Permission |
|---|---|
| `/typewriter ns reset <definition> [player]` | `typewriter.ns.reset` |
| `/typewriter ns level <definition> <level> [player]` | `typewriter.ns.level` |
| `/typewriter ns add <definition> <amount> [player]` | `typewriter.ns.add` |
| `/typewriter ns remove <definition> <amount> [player]` | `typewriter.ns.remove` |
| `/typewriter ns open <definition> [player]` | `typewriter.ns.open` |

## Placeholders

PlaceholderAPI, with `<id>` the definition id:

```text
%typewriter_ns_balance_<id>%
%typewriter_ns_level_<id>%
%typewriter_ns_capacity_<id>%
%typewriter_ns_interest_<id>%
%typewriter_ns_interest_cooldown_<id>%
%typewriter_ns_name_<id>%
%typewriter_ns_prefix_<id>%
```

## Requirements

- Typewriter engine `0.9.0-beta-177`, Paper.
- Typewriter extension **GuiAndDialogs** (menu).
- PlaceholderAPI (`INTERNAL` transactions and placeholders).
- Optional: a Vault-compatible economy (`VAULT` transactions), MMOProfiles (per-profile storage; without it, storage is per player).

## Data migration

New artifacts use a technical identifier and the JSON schema `schema_version: 2`. On first access, a legacy file based on `artifactId` is copied to the canonical path, backed up under `backups/numericalstorage/`, then deleted when possible. Back up the `assets/` folder before updating.

## Building

Requires Java 21 and the Gradle wrapper at the root of the repository.

```powershell
.\gradlew.bat test
.\gradlew.bat build --no-daemon -x test
```

## Documentation and license

Documentation: [BTC Studio Docs](https://docs.borntocraftstudio.net/extensions/free/numerical-storage/).

Licensed under GNU GPLv3 with an additional exception, see `LICENSE` and `LICENSE-EXCEPTION.md`. The Typewriter engine keeps its own license and must not be redistributed with this repository.
