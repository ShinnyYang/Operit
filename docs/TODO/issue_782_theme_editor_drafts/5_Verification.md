# Verification

## Checks

- Inspect every theme editor write path to ensure it mutates the draft facade rather than the persistent manager
- Inspect save and reset paths to ensure one target is captured before asynchronous work
- Inspect entity cleanup to distinguish target deletion from visual theme reset
- Inspect WebChat theme and structured rendering to ensure they resolve from the requested chat
- Inspect every router mutation to ensure it passes through the route leave guard
- Run formatting and build verification only when explicitly requested

## Manual Scenarios

1. Edit a non-active card, switch tabs, save, and confirm the active chat did not change
2. Edit a group, choose another target, discard, and confirm its stored theme is unchanged
3. Reset a card theme and confirm its AI avatar and custom chat title remain intact
4. Request two different WebChat themes and confirm each response has its matching target source and glass settings
5. Edit a target and leave through the drawer, shortcut, and back navigation; confirm each route change awaits the same dialog

## Completed Static Checks

- Verified that changed theme controls use the draft preferences facade
- Verified target capture, staged-asset cleanup, target metadata commit, and visual reset boundaries
- Verified WebChat resolves its requested chat before building theme and structured-render responses
- Verified route mutations in `OperitApp` enter the shared leave gate
- Ran `git diff --check` successfully

## Not Run

- Gradle, lint, unit tests, and builds were not run because no verification command was requested

## Known Data Constraint

- Existing chat bindings store character-card names. Duplicate names cannot identify a unique card theme until a dedicated chat schema migration introduces stable card IDs

[DONE]
