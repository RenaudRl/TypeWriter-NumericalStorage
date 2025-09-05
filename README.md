# TypeWriter NumericalStorage Extension

Extension providing a configurable Numerical Storage system with level (progression) for Typewriter.

## PlaceholderAPI

This extension exposes placeholders through Typewriter's PlaceholderAPI expansion (`typewriter`).

Available placeholders:

| Placeholder                              | Description |
|------------------------------------------|-------------|
| `%typewriter_ns_balance_<definitionId>%` | Player balance for the given definition |
| `%typewriter_ns_level_<definitionId>%`   | Player level for the given definition |
| `%typewriter_ns_name_<definitionId>%`    | Display name for the given definition |
| `%typewriter_ns_prefix_<definitionId>%`  | Prefix for the given definition |

Replace `<definitionId>` with the id of your `numericalstorage_definition` entry.
