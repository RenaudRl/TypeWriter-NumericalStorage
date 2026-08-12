# Changelog

## 0.9 — 2026-08-12

- **`transactionMode`** on the transaction config: `INTERNAL` keeps the PlaceholderAPI plus
  console command path, `VAULT` goes through the Vault Economy API directly and ignores
  `amountPlaceholder` and the command fields.
- `addCommand` and `removeCommand` were documented backwards. `addCommand` completes a deposit
  into storage and is checked *before* the persistent mutation; `removeCommand` completes a
  withdrawal.
- Persistence is asynchronous, with per-artifact mutation locks and a short-lived cache, so
  concurrent deposits can no longer race each other.
- Storage files move to JSON schema version 2 under a technical artifact path. A legacy file based
  on the old semantic `artifactId` is backed up under `backups/numericalstorage/` and migrated on
  first access — back up the Typewriter `assets/` directory before upgrading.
- `artifactId` is a generated technical identifier: leave it empty and Typewriter fills it in.
- Placeholders take an explicit definition argument (`%typewriter_ns_balance_bank%`), which
  removes the old underscore-splitting ambiguity when a definition ID itself contains underscores.
- Optional profile-aware keys when the Profiles integration is installed.
