# Remove Filters And Historical Imports

## Previous Behavior

Range queries accept model, call-category, and result-status filters. The v20 to
v21 migration copies chat rows into the ledger, and repository initialization
imports legacy cumulative totals.

## Intended Behavior

Range analysis retains model filtering only. The ledger contains direct formal
inference facts rather than copied conversations or cumulative counters.

## Work

- Delete category and status types, UI controls, strings, query parameters, SQL
  clauses, breakdown queries, entity columns, and indexes.
- Delete legacy usage-row import code and the historical conversation copy from
  the unpublished schema creation.
- Keep current pricing settings and their storage because normal-request cost
  calculations still need them.

## Completion

[DONE]

- Category and status types, UI controls, strings, query parameters, SQL
  clauses, entity columns, indexes, and the historical chat-copy and cumulative
  usage imports are deleted; range analysis keeps model filtering only.
- Pricing decision: released `api_settings` custom prices and the legacy
  `usd_to_cny_exchange_rate` are NOT migrated. The unpublished v21 storage
  starts fresh with built-in model prices and the estimated default rate, so
  `ReleasedProviderModelKeyDecoder` and its bridge stay deleted.
