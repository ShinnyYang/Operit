# Verification

## Automated Coverage

- A target save finishes before a queued target transition
- A stale target does not write a scoped snapshot
- Legacy migration only copies global data when its default-card ownership is unambiguous

## Manual Acceptance

1. Configure the default card and create A.
2. Activate A and change several theme values.
3. Switch to the default card, then return to A.
4. Confirm A retains its own values and the default card retains its own values.
5. Repeat the switch while changing a theme value and confirm neither target inherits the other target's values.

Automated test commands were not run because this repository requires an explicit request before local testing.

[DONE]
