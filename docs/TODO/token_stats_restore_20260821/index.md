---
fork_repository: https://github.com/luojiaping/Operit.git
working_branch: feat/release-version-bump
---

# Restore Token Statistics Migration

## Background

The token statistics rewrite removed the one-time import from released `api_settings`
keys and removed copying token-bearing assistant messages into the Room ledger.
That would make an upgrade appear to lose historical usage.

## Intent

Restore a one-time, idempotent migration for both legacy cumulative statistics and
token-bearing chat history while keeping new requests in the current Room schema.

## Scope

- `MIGRATION_20_21` copies existing assistant messages and message variants.
- The first token repository initialization imports old cumulative counters and
  custom prices from `api_settings`, then marks and clears the legacy keys.
- No build or test commands are run locally.

## Steps

1. [DONE] Restore the Room history copy.
2. [DONE] Restore the legacy DataStore import marker and importer.
3. [DONE] Review the resulting migration paths.
