# Target Serialization

## Previous Behavior

The settings editor starts a coroutine for the global preference update and schedules another coroutine for the scoped snapshot. Target activation can run between them.

## Change

Use one coordinator for prompt transitions, target-bound saves, and target-bound resets. The coordinator validates the captured target while holding the same mutex used for activation, then performs the shared update and scoped snapshot synchronously.

## Expected Result

An edit belongs only to the target that initiated it. Events from a page that is no longer active do not write into the new target.

[DONE]
