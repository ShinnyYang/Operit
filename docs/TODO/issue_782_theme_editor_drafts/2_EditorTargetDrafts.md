# Editor Target Drafts

## Previous Behavior

The theme screen derives its target from `ActivePromptManager.activePromptFlow`. Each tab collects shared preference flows and persists individual control changes through `saveThemeSettingsWithCharacterCard`. Switching tabs disposes local input state, and selecting a different edit target would require changing the active chat prompt.

## Change

Create a screen-owned draft preferences facade. It exposes the existing theme flows and save API to current sections, but stores mutations in a `StateFlow` backed by the selected target snapshot. The facade delegates recent colors to their global store and does not write role-bound theme keys until the screen commits the draft.

Add a selector above the tabs with the default role, non-default cards, and groups. Changing target asks the user to save, discard, or cancel when the current draft is dirty. Save captures the source target before asynchronous work begins. The footer owns the single save and reset actions; reset becomes a draft reset until saved.

Picker results retain their source draft before launching external activities. Staged files stay available while their draft is saving, then are deleted when an unsaved or failed draft is disposed.

## Expected Result

Target selection does not change the active chat. Tab navigation retains unsaved inputs. All role-bound controls participate in one draft and one explicit save operation.

[DONE]
