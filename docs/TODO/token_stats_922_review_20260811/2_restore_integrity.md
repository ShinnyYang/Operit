# 2. Restore Integrity

## Previous State

`RoomDatabaseRestoreManager` deletes the active database, WAL, and SHM files before
calling `replaceFile`. `replaceFile` can still fail while renaming or copying the
validated temporary database. The exception path then removes temporary files,
leaving no recoverable active database.

## Intended Change

Validate WAL/SHM compatibility before committing the restore marker. Replace each
staged database file using only same-filesystem atomic move with replacement; do not
delete the active target first or copy after a failed move. Preserve the restore
barrier semantics and the replacing marker.

## Expected State

A failed atomic replacement reports failure without deleting the user's previously
active database. A focused regression test injects the final move failure and
verifies that the existing database remains intact. [DONE]
