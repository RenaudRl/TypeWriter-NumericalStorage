# NumericalStorage — extension publique TypeWriter

Version officielle pour Paper/Folia et le moteur TypeWriter public. Elle cible Java 21 afin de rester compatible avec l’écosystème officiel TypeWriter `0.9.0-beta-177`.

## Fonctionnalités

- soldes et niveaux persistés dans les assets TypeWriter ;
- écritures asynchrones et cache court, sans I/O bloquante sur les threads Paper/Folia ;
- intérêt différé par cron, plafonné au nombre de cycles rattrapés ;
- mode global ou mode profil via l’intégration Profiles ;
- GUI officielle OmniGUI/GuiAndDialogs ;
- transactions `INTERNAL` (PlaceholderAPI + commandes) ou `VAULT` ;
- transfert atomique entre deux stockages.

Les mutations sont sérialisées par artefact. Les transactions Vault compensent un débit/crédit externe si l’écriture persistante échoue ; les commandes arbitraires doivent rester idempotentes, car TypeWriter ne peut pas en déduire une opération inverse.

## Placeholders

La version publique utilise PlaceholderAPI :

```text
%typewriter_ns_balance_<id>%
%typewriter_ns_level_<id>%
%typewriter_ns_capacity_<id>%
%typewriter_ns_interest_<id>%
%typewriter_ns_interest_cooldown_<id>%
%typewriter_ns_name_<id>%
%typewriter_ns_prefix_<id>%
```

## Migration des données

Les nouveaux artefacts utilisent un identifiant technique et le schéma JSON `schema_version: 2`. Au premier accès, un fichier historique basé sur `artifactId` est copié vers le chemin canonique, sauvegardé sous `backups/numericalstorage/`, puis supprimé si possible. Garder une sauvegarde du dossier `assets/` avant mise à jour.

## GUI et compatibilité Typewriter

Les boutons du menu sont interceptés directement par `MenuSessionService` avant tout dispatch de commande Bukkit. Cela conserve le contexte `Player` attendu par Typewriter, y compris sur Folia, et évite que les boutons restent sans effet.

<<<<<<< Updated upstream
## 📜 License
Licensed under the **MIT License**.

## Documentation

Full documentation available at [BTC Studio Docs](https://docs.borntocraftstudio.net/extensions/free/numerical-storage/).
=======
Les actions générées par les layouts utilisent les formes suivantes :

```text
numstorage tx <definitionId> <action>
numstorage upgrade <definitionId>
numstorage back_main <definitionId>
```

Le handler vérifie qu'une session de menu est active, accepte les espaces multiples et est réenregistré de façon idempotente lors d'un reload. Les commandes `/ns` restent disponibles pour l'administration et les intégrations console.

Les écritures de données et les opérations externes sont exécutées hors des threads de région ; les accès au joueur et à l'API Bukkit repassent par les schedulers Paper/Folia appropriés.

## Build

Prérequis : Java 21 et le wrapper Gradle fourni à la racine du dépôt public.

```powershell
.\gradlew.bat :TypeWriter-NumericalStorage:test
.\gradlew.bat :TypeWriter-NumericalStorage:build --no-daemon -x test
```

Le build inclut le projet OmniGUI officiel comme sous-projet. La version publique ne dépend pas des extensions BTC custom et ne doit pas être déployée avec elles sur le serveur public de test.

Licence MIT.
>>>>>>> Stashed changes

---

## 📜 Licence

**GNU General Public License v3.0 or later** — [LICENSE](LICENSE) — with a
**linking exception** for the Typewriter engine — [LICENSE-EXCEPTION.md](LICENSE-EXCEPTION.md).

| | |
|---|---|
| You may | Run it anywhere, **including on a monetised server**. Study it, modify it, use it as a base, and redistribute it — **even for a fee**. GPLv3 §4 explicitly allows charging for a copy. |
| You must | Publish the complete corresponding source of your version under GPLv3, preserve the copyright notices, and **state that you modified it and when** (§5(a)). |
| You may not | Ship a closed-source or proprietary version, relicense under stricter terms, or strip the attribution and present this work as your own — §8 terminates your rights automatically. |
| Marks | **"Born To Craft"** and **"BTC Studio"** are **not** covered by the GPL. Fork it freely, sell your fork if you like — but **rebrand it**. |

> Reselling this code is legally allowed and practically pointless: whoever buys a
> copy from you receives, under the GPL, the right to redistribute it for free.
> That is the protection — not a clause forbidding sale, which the GPL does not
> permit us to add.

### About Typewriter

This is a **third-party extension**. It uses the public extension API of the
[Typewriter](https://github.com/gabber235/Typewriter) engine by gabber235 and
contains none of its source. Born To Craft Studio is not affiliated with or
endorsed by the Typewriter project.

The engine itself is **not** free software — its licence forbids redistributing
it. **Get it from the Typewriter project, and never redistribute it**, including
inside a fork of this repository.

Full attribution, the statement of modifications required by §5(a), and the
trademark reservation are in **[NOTICE.md](NOTICE.md)**. Read it before
redistributing.

© 2026 Born To Craft Studio.
