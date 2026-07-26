package com.ai.assistance.operit.ui.features.settings.screens.theme

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.model.ActivePrompt
import com.ai.assistance.operit.data.model.CharacterCard
import com.ai.assistance.operit.data.model.CharacterGroupCard
import com.ai.assistance.operit.data.preferences.ActivePromptManager
import com.ai.assistance.operit.data.preferences.CharacterCardManager
import com.ai.assistance.operit.data.preferences.CharacterGroupCardManager
import com.ai.assistance.operit.data.preferences.DisplayPreferencesManager
import com.ai.assistance.operit.data.preferences.UserPreferencesManager
import com.ai.assistance.operit.ui.features.settings.sections.SaveThemeSettingsAction
import com.ai.assistance.operit.ui.main.navigation.RegisterRouteBackGuard
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal data class ThemeSettingsShared(
    val context: android.content.Context,
    val preferencesManager: ThemeSettingsDraftPreferences,
    val displayPreferencesManager: DisplayPreferencesManager,
    val scope: CoroutineScope,
    val activeThemeTargetName: String?,
    val activeThemeTargetAvatarUri: String?,
    val isGroupThemeTarget: Boolean,
    val saveThemeSettingsWithCharacterCard: SaveThemeSettingsAction,
)

private sealed interface ThemeEditorPendingAction {
    data class SelectTarget(val target: ActivePrompt) : ThemeEditorPendingAction

    data object LeaveScreen : ThemeEditorPendingAction
}

@OptIn(ExperimentalMaterial3Api::class)
@NonRestartableComposable
@Composable
internal fun ThemeSettingsContent() {
    val context = LocalContext.current
    val preferencesManager = remember { UserPreferencesManager.getInstance(context) }
    val displayPreferencesManager = remember { DisplayPreferencesManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    val characterCardManager = remember { CharacterCardManager.getInstance(context) }
    val characterGroupCardManager = remember { CharacterGroupCardManager.getInstance(context) }
    val activePromptManager = remember { ActivePromptManager.getInstance(context) }
    val activePrompt: ActivePrompt? by activePromptManager.activePromptFlow.collectAsState(initial = null)
    val characterCardIds by characterCardManager.characterCardListFlow.collectAsState(initial = emptyList())
    val characterGroups by characterGroupCardManager.allCharacterGroupCardsFlow.collectAsState(
        initial = emptyList(),
    )
    var characterCards by remember { mutableStateOf(emptyList<CharacterCard>()) }

    LaunchedEffect(characterCardIds) {
        characterCards = characterCardManager.getAllCharacterCards()
    }

    val initialThemeTarget = activePrompt
    if (initialThemeTarget == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else {
        ThemeSettingsContentEditor(
            preferencesManager = preferencesManager,
            displayPreferencesManager = displayPreferencesManager,
            scope = scope,
            activePromptManager = activePromptManager,
            initialThemeTarget = initialThemeTarget,
            characterCards = characterCards,
            characterGroups = characterGroups,
        )
    }
}

@Composable
internal fun ThemeSettingsContentEditor(
    preferencesManager: UserPreferencesManager,
    displayPreferencesManager: DisplayPreferencesManager,
    scope: CoroutineScope,
    activePromptManager: ActivePromptManager,
    initialThemeTarget: ActivePrompt,
    characterCards: List<CharacterCard>,
    characterGroups: List<CharacterGroupCard>,
) {
    val context = LocalContext.current
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
    var selectedThemeTab by remember { mutableStateOf(ThemeSettingsTab.BASIC) }
    var selectedThemeTarget by remember { mutableStateOf(initialThemeTarget) }
    var editorPreferences by remember { mutableStateOf<ThemeSettingsDraftPreferences?>(null) }
    var showSaveSuccessMessage by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<ThemeEditorPendingAction?>(null) }
    var exitContinuation by remember { mutableStateOf<CancellableContinuation<Boolean>?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scrollState = androidx.compose.foundation.rememberScrollState()

    LaunchedEffect(selectedThemeTarget) {
        val target = selectedThemeTarget
        editorPreferences = null
        val snapshot = when (target) {
            is ActivePrompt.CharacterCard ->
                preferencesManager.resolveThemePreferenceSnapshot(characterCardId = target.id)

            is ActivePrompt.CharacterGroup ->
                preferencesManager.resolveThemePreferenceSnapshot(characterGroupId = target.id)
        }
        if (selectedThemeTarget == target) {
            editorPreferences = ThemeSettingsDraftPreferences(preferencesManager, snapshot)
        }
    }
    val draftForDirtyState = editorPreferences
    val hasUnsavedChanges =
        if (draftForDirtyState == null) {
            false
        } else {
            val dirty by draftForDirtyState.hasUnsavedChangesFlow.collectAsState(
                initial = draftForDirtyState.hasUnsavedChanges,
            )
            dirty
        }

    val selectedCharacterCard = (selectedThemeTarget as? ActivePrompt.CharacterCard)
        ?.let { target -> characterCards.firstOrNull { it.id == target.id } }
    val selectedCharacterGroup = (selectedThemeTarget as? ActivePrompt.CharacterGroup)
        ?.let { target -> characterGroups.firstOrNull { it.id == target.id } }
    val selectedAvatarUri by remember(selectedThemeTarget) {
        when (val target = selectedThemeTarget) {
            is ActivePrompt.CharacterCard -> preferencesManager.getAiAvatarForCharacterCardFlow(target.id)
            is ActivePrompt.CharacterGroup -> preferencesManager.getAiAvatarForCharacterGroupFlow(target.id)
        }
    }.collectAsState(initial = null)
    val selectedThemeTargetName =
        selectedCharacterGroup?.name
            ?: selectedCharacterCard?.name
            ?: context.getString(R.string.theme_default_character_card)

    fun finishPendingAction(allowNavigation: Boolean) {
        val action = pendingAction
        pendingAction = null
        when (action) {
            is ThemeEditorPendingAction.SelectTarget -> {
                if (allowNavigation) {
                    selectedThemeTarget = action.target
                }
            }

            ThemeEditorPendingAction.LeaveScreen -> {
                val continuation = exitContinuation
                exitContinuation = null
                continuation?.resume(allowNavigation)
            }

            null -> Unit
        }
    }

    fun saveCurrentDraft() {
        val draft = editorPreferences ?: return
        if (isSaving) return
        val target = selectedThemeTarget
        val savedValues = draft.values
        val resetRequested = draft.isResetRequested
        isSaving = true
        draft.beginSave(savedValues)
        scope.launch {
            try {
                if (resetRequested) {
                    activePromptManager.resetThemeDraft(target)
                } else {
                    activePromptManager.commitThemeDraft(target, savedValues)
                }
                draft.markSaved(savedValues)
                showSaveSuccessMessage = true
                finishPendingAction(allowNavigation = true)
            } catch (e: CancellationException) {
                draft.cancelSave()
                throw e
            } catch (e: Exception) {
                draft.cancelSave()
                AppLogger.e("ThemeSettings", "Failed to save theme draft", e)
                Toast.makeText(context, context.getString(R.string.theme_save_failed), Toast.LENGTH_LONG)
                    .show()
            } finally {
                isSaving = false
            }
        }
    }

    RegisterRouteBackGuard {
        if (pendingAction != null) {
            return@RegisterRouteBackGuard false
        }
        val draft = editorPreferences
        if (draft == null || !hasUnsavedChanges) {
            return@RegisterRouteBackGuard true
        }
        suspendCancellableCoroutine<Boolean> { continuation ->
            pendingAction = ThemeEditorPendingAction.LeaveScreen
            exitContinuation = continuation
            continuation.invokeOnCancellation {
                if (exitContinuation === continuation) {
                    exitContinuation = null
                    pendingAction = null
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ThemeSettingsTargetSelector(
            selectedTarget = selectedThemeTarget,
            characterCards = characterCards,
            characterGroups = characterGroups,
            enabled = pendingAction !is ThemeEditorPendingAction.LeaveScreen && !isSaving,
            onTargetSelected = { target ->
                val draft = editorPreferences
                if (pendingAction !is ThemeEditorPendingAction.LeaveScreen && target != selectedThemeTarget) {
                    if (draft != null && hasUnsavedChanges) {
                        pendingAction = ThemeEditorPendingAction.SelectTarget(target)
                    } else {
                        selectedThemeTarget = target
                    }
                }
            },
        )

        val draft = editorPreferences
        if (draft == null) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            DisposableEffect(draft) {
                onDispose(draft::dispose)
            }
            val shared = ThemeSettingsShared(
                context = context,
                preferencesManager = draft,
                displayPreferencesManager = displayPreferencesManager,
                scope = scope,
                activeThemeTargetName = selectedThemeTargetName,
                activeThemeTargetAvatarUri = selectedAvatarUri,
                isGroupThemeTarget = selectedThemeTarget is ActivePrompt.CharacterGroup,
                saveThemeSettingsWithCharacterCard = { action ->
                    scope.launch { action() }
                },
            )

            // External picker callbacks retain this exact draft, even after another target is selected.
            key(draft) {
                ThemeSettingsTabbedContent(
                    selectedTab = selectedThemeTab,
                    onSelectedTabChange = { selectedThemeTab = it },
                    scrollState = scrollState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    basicContent = {
                        ThemeSettingsBasicTab(
                            shared = shared,
                            cardColors = cardColors,
                        )
                    },
                    backgroundContent = {
                        ThemeSettingsBackgroundTab(
                            shared = shared,
                            cardColors = cardColors,
                            scrollState = scrollState,
                        )
                    },
                    chatContent = {
                        ThemeSettingsChatTab(
                            shared = shared,
                            cardColors = cardColors,
                        )
                    },
                    inputContent = {
                        ThemeSettingsInputTab(
                            shared = shared,
                            cardColors = cardColors,
                        )
                    },
                    interfaceContent = {
                        ThemeSettingsInterfaceTab(
                            shared = shared,
                            cardColors = cardColors,
                        )
                    },
                    footerContent = {
                        ThemeSettingsFooter(
                            showSaveSuccessMessage = showSaveSuccessMessage,
                            onShowSaveSuccessMessageChange = { showSaveSuccessMessage = it },
                            saveEnabled = hasUnsavedChanges && !isSaving,
                            isSaving = isSaving,
                            onSave = ::saveCurrentDraft,
                            onReset = draft::reset,
                        )
                    },
                )
            }
        }
    }

    if (pendingAction != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    finishPendingAction(allowNavigation = false)
                }
            },
            title = { Text(stringResource(R.string.theme_unsaved_title)) },
            text = { Text(stringResource(R.string.theme_unsaved_message)) },
            confirmButton = {
                TextButton(onClick = ::saveCurrentDraft, enabled = !isSaving) {
                    Text(stringResource(R.string.theme_save_and_continue))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            editorPreferences?.discard()
                            finishPendingAction(allowNavigation = true)
                        },
                        enabled = !isSaving,
                    ) {
                        Text(stringResource(R.string.theme_discard_and_continue))
                    }
                    TextButton(
                        onClick = { finishPendingAction(allowNavigation = false) },
                        enabled = !isSaving,
                    ) {
                        Text(stringResource(R.string.cancel_action))
                    }
                }
            },
        )
    }
}

@Composable
private fun ThemeSettingsTargetSelector(
    selectedTarget: ActivePrompt,
    characterCards: List<CharacterCard>,
    characterGroups: List<CharacterGroupCard>,
    enabled: Boolean,
    onTargetSelected: (ActivePrompt) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when (selectedTarget) {
        is ActivePrompt.CharacterCard -> {
            characterCards.firstOrNull { it.id == selectedTarget.id }?.name
                ?: stringResource(R.string.theme_default_character_card)
        }

        is ActivePrompt.CharacterGroup -> {
            characterGroups.firstOrNull { it.id == selectedTarget.id }?.name.orEmpty()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.theme_target_title),
                style = MaterialTheme.typography.labelLarge,
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val icon = if (selectedTarget is ActivePrompt.CharacterGroup) {
                        Icons.Default.Groups
                    } else {
                        Icons.Default.Person
                    }
                    Icon(imageVector = icon, contentDescription = null)
                    Text(
                        text = selectedLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.theme_default_character_card)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onTargetSelected(
                                ActivePrompt.CharacterCard(CharacterCardManager.DEFAULT_CHARACTER_CARD_ID),
                            )
                        },
                    )
                    characterCards
                        .filter { it.id != CharacterCardManager.DEFAULT_CHARACTER_CARD_ID }
                        .forEach { card ->
                            DropdownMenuItem(
                                text = { Text(card.name) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                onClick = {
                                    expanded = false
                                    onTargetSelected(ActivePrompt.CharacterCard(card.id))
                                },
                            )
                        }
                    if (characterGroups.isNotEmpty()) {
                        HorizontalDivider()
                        characterGroups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                                onClick = {
                                    expanded = false
                                    onTargetSelected(ActivePrompt.CharacterGroup(group.id))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
