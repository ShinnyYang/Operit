package com.ai.assistance.operit.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ai.assistance.operit.data.preferences.PromptPreferencesManager.Companion.DEFAULT_CHAT_PROFILE_ID
import com.ai.assistance.operit.data.preferences.PromptPreferencesManager.Companion.DEFAULT_DESKTOP_PET_PROFILE_ID
import com.ai.assistance.operit.data.preferences.PromptPreferencesManager.Companion.DEFAULT_VOICE_PROFILE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// 功能类型枚举
enum class PromptFunctionType {
    CHAT, // 对话功能
    VOICE, // 语音功能
    DESKTOP_PET // 桌宠功能
}

private val Context.functionalPromptDataStore by
        preferencesDataStore(name = "functional_prompt_preferences")

/** 管理不同功能的提示词配置 */
class FunctionalPromptManager(private val context: Context) {

    private val dataStore = context.functionalPromptDataStore
    private val promptPreferencesManager = PromptPreferencesManager(context)

    // 默认提示词配置ID
    companion object {
        const val DEFAULT_PROMPT_PROFILE_ID = "default"

        // 为不同功能创建配置映射键
        private fun functionPromptMappingKey(functionType: PromptFunctionType) =
                stringPreferencesKey("function_prompt_${functionType.name.lowercase()}")

        // 新增：为每个功能类型获取其特定的默认配置ID
        fun getDefaultProfileIdForFunction(functionType: PromptFunctionType): String {
            return when (functionType) {
                PromptFunctionType.CHAT -> DEFAULT_CHAT_PROFILE_ID
                PromptFunctionType.VOICE -> DEFAULT_VOICE_PROFILE_ID
                PromptFunctionType.DESKTOP_PET -> DEFAULT_DESKTOP_PET_PROFILE_ID
            }
        }
    }

    // 功能提示词配置映射Flow
    val functionPromptMappingFlow: Flow<Map<PromptFunctionType, String>> =
            dataStore.data.map { preferences ->
                PromptFunctionType.values().associateWith { functionType ->
                    preferences[functionPromptMappingKey(functionType)]
                            ?: getDefaultProfileIdForFunction(functionType)
                }
            }

    // 获取指定功能的提示词配置ID
    fun getPromptProfileIdForFunction(functionType: PromptFunctionType): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[functionPromptMappingKey(functionType)]
                    ?: getDefaultProfileIdForFunction(functionType)
        }
    }

    // 设置功能的提示词配置
    suspend fun setPromptProfileForFunction(functionType: PromptFunctionType, profileId: String) {
        dataStore.edit { preferences ->
            preferences[functionPromptMappingKey(functionType)] = profileId
        }
    }

    // 重置所有功能提示词配置为默认
    suspend fun resetAllFunctionPrompts() {
        dataStore.edit { preferences ->
            PromptFunctionType.values().forEach { functionType ->
                // 重置为各自的默认配置
                preferences[functionPromptMappingKey(functionType)] =
                        getDefaultProfileIdForFunction(functionType)
            }
        }
    }

    // 获取功能对应的提示词内容
    suspend fun getPromptForFunction(functionType: PromptFunctionType): Pair<String, String> {
        val profileId = getPromptProfileIdForFunction(functionType).first()
        val profile = promptPreferencesManager.getPromptProfileFlow(profileId).first()
        return Pair(profile.introPrompt, profile.tonePrompt)
    }

    // 获取自定义系统提示模板（通过 ApiPreferences）
    suspend fun getCustomSystemPromptTemplate(): String {
        val apiPreferences = com.ai.assistance.operit.data.preferences.ApiPreferences.getInstance(context)
        return apiPreferences.customSystemPromptTemplateFlow.first()
    }

    // 初始化默认配置
    suspend fun initializeIfNeeded() {
        // 确保提示词配置管理器已初始化
        promptPreferencesManager.initializeIfNeeded()

        // 确保所有功能类型都有默认映射
        val currentMapping = functionPromptMappingFlow.first()

        dataStore.edit { preferences ->
            PromptFunctionType.values().forEach { functionType ->
                if (!currentMapping.containsKey(functionType)) {
                    // 为不同功能类型分配对应的默认提示词配置
                    val defaultProfileId = getDefaultProfileIdForFunction(functionType)
                    preferences[functionPromptMappingKey(functionType)] = defaultProfileId
                }
            }
        }
    }
}
