# NumericalStorage — extension publique TypeWriter

Version officielle pour Paper/Folia et le moteur TypeWriter public. Elle cible Java 21 et
TypeWriter `0.9.0-beta-176`.

## Fonctionnalités

- soldes et niveaux persistés dans les assets TypeWriter ;
- écritures asynchrones et cache court, sans I/O bloquante sur les threads Paper/Folia ;
- intérêt différé par cron, plafonné au nombre de cycles rattrapés ;
- mode global ou mode profil via l'intégration Profiles ;
- GUI officielle OmniGUI/GuiAndDialogs ;
- transactions `INTERNAL` (PlaceholderAPI + commandes) ou `VAULT` ;
- transfert atomique entre deux stockages.

Les mutations sont sérialisées par artefact. Les transactions Vault compensent un débit/crédit
externe si l'écriture persistante échoue ; les commandes arbitraires doivent rester idempotentes,
car TypeWriter ne peut pas en déduire une opération inverse.

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

Les nouveaux artefacts utilisent un identifiant technique et le schéma JSON `schema_version: 2`.
Au premier accès, un fichier historique basé sur `artifactId` est copié vers le chemin canonique,
sauvegardé sous `backups/numericalstorage/`, puis supprimé si possible. Garder une sauvegarde du
dossier `assets/` avant mise à jour.

## Build

Prérequis : Java 21 et le wrapper Gradle fourni à la racine du dépôt public.

```powershell
.\gradlew.bat test
.\gradlew.bat build --no-daemon -x test
```

Le build public utilise uniquement les APIs officielles TypeWriter/Paper et les dépendances
publiques déclarées. Il ne dépend pas des extensions BTC custom.

## Documentation et licence

Documentation : [BTC Studio Docs](https://docs.borntocraftstudio.net/extensions/free/numerical-storage/).

Cette extension est publiée sous licence MIT. Le moteur TypeWriter reste soumis à sa propre
licence et ne doit pas être redistribué avec ce dépôt.
