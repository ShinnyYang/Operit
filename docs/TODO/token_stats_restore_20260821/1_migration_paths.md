# Migration Paths

The Room migration owns rows that already exist in `messages` and
`message_variants`. The repository importer owns released cumulative counters and
pricing because those values live outside Room. Both paths are one-time operations
and retain data before removing obsolete legacy keys.

[DONE]
