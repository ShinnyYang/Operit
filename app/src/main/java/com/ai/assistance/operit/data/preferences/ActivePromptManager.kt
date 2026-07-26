package com.ai.assistance.operit.data.preferences

import android.content.Context
import com.ai.assistance.operit.data.model.ActivePrompt
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

class ActivePromptManager private constructor(context: Context) {

    private val characterCardManager = CharacterCardManager.getInstance(context)
    private val characterGroupCardManager = CharacterGroupCardManager.getInstance(context)
    private val userPreferencesManager = UserPreferencesManager.getInstance(context)
    private val themeOperations = ThemeTargetOperationCoordinator(::getActivePrompt)

    val activePromptFlow: Flow<ActivePrompt> =
        combine(
            characterGroupCardManager.observeActiveCharacterGroupId(),
            characterCardManager.observeActiveCharacterCardId()
        ) { groupId, cardId ->
            when {
                !groupId.isNullOrBlank() -> ActivePrompt.CharacterGroup(groupId)
                !cardId.isNullOrBlank() -> ActivePrompt.CharacterCard(cardId)
                else -> ActivePrompt.CharacterCard(CharacterCardManager.DEFAULT_CHARACTER_CARD_ID)
            }
        }.distinctUntilChanged()

    suspend fun getActivePrompt(): ActivePrompt = activePromptFlow.first()

    suspend fun setActivePrompt(prompt: ActivePrompt) {
        themeOperations.runTransition {
            when (prompt) {
                is ActivePrompt.CharacterGroup -> {
                    characterGroupCardManager.setActiveCharacterGroupCard(prompt.id)
                    characterCardManager.clearActiveCharacterCard()
                }
                is ActivePrompt.CharacterCard -> {
                    characterCardManager.setActiveCharacterCard(prompt.id)
                    characterGroupCardManager.setActiveCharacterGroupCard(null)
                }
            }
        }
    }

    internal suspend fun <T> runThemeTransition(action: suspend () -> T): T {
        return themeOperations.runTransition(action)
    }

    suspend fun saveThemeForActivePrompt(
        target: ActivePrompt,
        saveAction: suspend () -> Unit,
    ): Boolean {
        val saved = themeOperations.runForTarget(target) {
            saveAction()
            when (target) {
                is ActivePrompt.CharacterGroup ->
                    userPreferencesManager.saveCurrentThemeToCharacterGroup(target.id)

                is ActivePrompt.CharacterCard ->
                    userPreferencesManager.saveCurrentThemeToCharacterCard(target.id)
            }
        }
        if (!saved) {
            AppLogger.w(TAG, "Ignoring theme save because the active prompt changed")
        }
        return saved
    }

    suspend fun resetThemeForActivePrompt(target: ActivePrompt): Boolean {
        val reset = themeOperations.runForTarget(target) {
            userPreferencesManager.resetThemeSettings()
            when (target) {
                is ActivePrompt.CharacterGroup ->
                    userPreferencesManager.deleteCharacterGroupTheme(target.id)

                is ActivePrompt.CharacterCard ->
                    userPreferencesManager.deleteCharacterCardTheme(target.id)
            }
        }
        if (!reset) {
            AppLogger.w(TAG, "Ignoring theme reset because the active prompt changed")
        }
        return reset
    }

    suspend fun saveAiAvatarForPrompt(target: ActivePrompt, avatarUri: String?) {
        themeOperations.runTransition {
            when (target) {
                is ActivePrompt.CharacterGroup ->
                    userPreferencesManager.saveAiAvatarForCharacterGroup(target.id, avatarUri)

                is ActivePrompt.CharacterCard ->
                    userPreferencesManager.saveAiAvatarForCharacterCard(target.id, avatarUri)
            }
            if (getActivePrompt() == target) {
                userPreferencesManager.saveCurrentThemeAiAvatar(avatarUri)
            }
        }
    }

    suspend fun saveCustomChatTitleForPrompt(target: ActivePrompt, title: String?) {
        themeOperations.runTransition {
            when (target) {
                is ActivePrompt.CharacterGroup ->
                    userPreferencesManager.saveCustomChatTitleForCharacterGroup(target.id, title)

                is ActivePrompt.CharacterCard ->
                    userPreferencesManager.saveCustomChatTitleForCharacterCard(target.id, title)
            }
            if (getActivePrompt() == target) {
                userPreferencesManager.saveCurrentThemeChatTitle(title)
            }
        }
    }

    suspend fun activateForChatBinding(characterCardName: String?, characterGroupId: String?) {
        val normalizedGroupId = characterGroupId?.trim()?.takeIf { it.isNotBlank() }
        if (!normalizedGroupId.isNullOrBlank()) {
            setActivePrompt(ActivePrompt.CharacterGroup(normalizedGroupId))
            return
        }

        val normalizedCardName = characterCardName?.trim()?.takeIf { it.isNotBlank() }
        if (normalizedCardName != null) {
            val targetCard = characterCardManager.findCharacterCardByName(normalizedCardName)
            if (targetCard != null) {
                setActivePrompt(ActivePrompt.CharacterCard(targetCard.id))
                return
            }
        }

        setActivePrompt(ActivePrompt.CharacterCard(CharacterCardManager.DEFAULT_CHARACTER_CARD_ID))
    }

    suspend fun resolveActiveCardIdForSend(): String {
        return when (val prompt = getActivePrompt()) {
            is ActivePrompt.CharacterCard -> prompt.id
            is ActivePrompt.CharacterGroup -> CharacterCardManager.DEFAULT_CHARACTER_CARD_ID
        }
    }

    companion object {
        private const val TAG = "ActivePromptManager"

        @Volatile
        private var INSTANCE: ActivePromptManager? = null

        fun getInstance(context: Context): ActivePromptManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ActivePromptManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
