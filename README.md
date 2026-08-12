# NumericalStorage — extension publique TypeWriter

Version officielle pour Paper/Folia et le moteur TypeWriter public. Elle cible Java 21 afin de rester compatible avec l’écosystème officiel TypeWriter `0.9.0-beta-175`.

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
