package com.ai.assistance.operit.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.ModelParameter
import com.ai.assistance.operit.data.model.ParameterCategory
import com.ai.assistance.operit.data.model.ParameterValueType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Define the DataStore at the module level
private val Context.apiDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "api_settings")

class ApiPreferences private constructor(private val context: Context) {

    // Define our preferences keys
    companion object {
        @Volatile
        private var INSTANCE: ApiPreferences? = null

        fun getInstance(context: Context): ApiPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = ApiPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
        // 动态生成FunctionType的Token键
        fun getTokenInputKey(functionType: FunctionType) =
                intPreferencesKey("token_input_${functionType.name}")

        fun getTokenOutputKey(functionType: FunctionType) =
                intPreferencesKey("token_output_${functionType.name}")

        val SHOW_FPS_COUNTER = booleanPreferencesKey("show_fps_counter")
        val ENABLE_AI_PLANNING = booleanPreferencesKey("enable_ai_planning")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")

        // Keys for Thinking Mode and Thinking Guidance
        val ENABLE_THINKING_MODE = booleanPreferencesKey("enable_thinking_mode")
        val ENABLE_THINKING_GUIDANCE = booleanPreferencesKey("enable_thinking_guidance")

        // Key for Memory Attachment
        val ENABLE_MEMORY_ATTACHMENT = booleanPreferencesKey("enable_memory_attachment")

        // Key for Context Length
        val CONTEXT_LENGTH = floatPreferencesKey("context_length")

        // Key for Summary Token Threshold
        val SUMMARY_TOKEN_THRESHOLD = floatPreferencesKey("summary_token_threshold")

        // Custom Prompt Settings
        val CUSTOM_INTRO_PROMPT = stringPreferencesKey("custom_intro_prompt")
        val CUSTOM_TONE_PROMPT = stringPreferencesKey("custom_tone_prompt")
        
        // Custom System Prompt Template (Advanced Configuration)
        val CUSTOM_SYSTEM_PROMPT_TEMPLATE = stringPreferencesKey("custom_system_prompt_template")

        const val DEFAULT_SHOW_FPS_COUNTER = false
        const val DEFAULT_ENABLE_AI_PLANNING = false
        const val DEFAULT_KEEP_SCREEN_ON = true

        // Default values for Thinking Mode and Thinking Guidance
        const val DEFAULT_ENABLE_THINKING_MODE = false
        const val DEFAULT_ENABLE_THINKING_GUIDANCE = false

        // Default value for Memory Attachment
        const val DEFAULT_ENABLE_MEMORY_ATTACHMENT = true

        // Default value for Context Length (in K)
        const val DEFAULT_CONTEXT_LENGTH = 48.0f

        // Default value for Summary Token Threshold
        const val DEFAULT_SUMMARY_TOKEN_THRESHOLD = 0.70f

        // Default values for custom prompts
        const val DEFAULT_INTRO_PROMPT = "你是Operit，一个全能AI助手，旨在解决用户提出的任何任务。你有各种工具可以调用，以高效完成复杂的请求。"
        const val DEFAULT_TONE_PROMPT = "保持有帮助的语气，并清楚地传达限制。使用问题库根据用户的风格、偏好和过去的信息个性化响应。"
        
        // Default system prompt template (empty means use built-in template)
        const val DEFAULT_SYSTEM_PROMPT_TEMPLATE = ""

        // 自定义参数存储键
        val CUSTOM_PARAMETERS = stringPreferencesKey("custom_parameters")

        // 自定义请求头存储键
        val CUSTOM_HEADERS = stringPreferencesKey("custom_headers")

        // 默认空的自定义参数列表
        const val DEFAULT_CUSTOM_PARAMETERS = "[]"
        const val DEFAULT_CUSTOM_HEADERS = "{}"

        // API 配置默认值
        const val DEFAULT_API_ENDPOINT = "https://api.deepseek.com/v1/chat/completions"
        const val DEFAULT_MODEL_NAME = "deepseek-chat"
        private const val ENCODED_API_KEY = "c2stNmI4NTYyMjUzNmFjNDhjMDgwYzUwNDhhYjVmNWQxYmQ="
        val DEFAULT_API_KEY: String by lazy { decodeApiKey(ENCODED_API_KEY) }

        private fun decodeApiKey(encodedKey: String): String {
            return try {
                android.util.Base64.decode(encodedKey, android.util.Base64.NO_WRAP)
                    .toString(Charsets.UTF_8)
            } catch (e: Exception) {
                android.util.Log.e("ApiPreferences", "Failed to decode API key", e)
                ""
            }
        }
    }

    // 获取指定功能类型的token计数
    fun getTokensForFunctionFlow(functionType: FunctionType): Flow<Pair<Int, Int>> {
        return context.apiDataStore.data.map { preferences ->
            val inputTokens = preferences[getTokenInputKey(functionType)] ?: 0
            val outputTokens = preferences[getTokenOutputKey(functionType)] ?: 0
            Pair(inputTokens, outputTokens)
        }
    }

    // Get FPS Counter Display setting as Flow
    val showFpsCounterFlow: Flow<Boolean> =
            context.apiDataStore.data.map { preferences ->
                preferences[SHOW_FPS_COUNTER] ?: DEFAULT_SHOW_FPS_COUNTER
            }

    // Get AI Planning setting as Flow
    val enableAiPlanningFlow: Flow<Boolean> =
            context.apiDataStore.data.map { preferences ->
                preferences[ENABLE_AI_PLANNING] ?: DEFAULT_ENABLE_AI_PLANNING
            }

    // Get Keep Screen On setting as Flow
    val keepScreenOnFlow: Flow<Boolean> =
            context.apiDataStore.data.map { preferences ->
                preferences[KEEP_SCREEN_ON] ?: DEFAULT_KEEP_SCREEN_ON
            }

    // Flow for Thinking Mode
    val enableThinkingModeFlow: Flow<Boolean> =
        context.apiDataStore.data.map { preferences ->
            preferences[ENABLE_THINKING_MODE] ?: DEFAULT_ENABLE_THINKING_MODE
        }

    // Flow for Thinking Guidance
    val enableThinkingGuidanceFlow: Flow<Boolean> =
        context.apiDataStore.data.map { preferences ->
            preferences[ENABLE_THINKING_GUIDANCE] ?: DEFAULT_ENABLE_THINKING_GUIDANCE
            }

    // Flow for Memory Attachment
    val enableMemoryAttachmentFlow: Flow<Boolean> =
        context.apiDataStore.data.map { preferences ->
            preferences[ENABLE_MEMORY_ATTACHMENT] ?: DEFAULT_ENABLE_MEMORY_ATTACHMENT
        }

    // Flow for Context Length
    val contextLengthFlow: Flow<Float> =
        context.apiDataStore.data.map { preferences ->
            preferences[CONTEXT_LENGTH] ?: DEFAULT_CONTEXT_LENGTH
        }

    // Flow for Summary Token Threshold
    val summaryTokenThresholdFlow: Flow<Float> =
        context.apiDataStore.data.map { preferences ->
            preferences[SUMMARY_TOKEN_THRESHOLD] ?: DEFAULT_SUMMARY_TOKEN_THRESHOLD
        }

    // Custom Prompt Flows
    val customIntroPromptFlow: Flow<String> =
            context.apiDataStore.data.map { preferences ->
                preferences[CUSTOM_INTRO_PROMPT] ?: DEFAULT_INTRO_PROMPT
            }

    val customTonePromptFlow: Flow<String> =
            context.apiDataStore.data.map { preferences ->
                preferences[CUSTOM_TONE_PROMPT] ?: DEFAULT_TONE_PROMPT
            }

    // Custom System Prompt Template Flow
    val customSystemPromptTemplateFlow: Flow<String> =
            context.apiDataStore.data.map { preferences ->
                preferences[CUSTOM_SYSTEM_PROMPT_TEMPLATE] ?: DEFAULT_SYSTEM_PROMPT_TEMPLATE
            }

    // Flow for Custom Headers
    val customHeadersFlow: Flow<String> =
        context.apiDataStore.data.map { preferences ->
            preferences[CUSTOM_HEADERS] ?: DEFAULT_CUSTOM_HEADERS
        }

    // Save FPS Counter Display setting
    suspend fun saveShowFpsCounter(showFpsCounter: Boolean) {
        context.apiDataStore.edit { preferences -> preferences[SHOW_FPS_COUNTER] = showFpsCounter }
    }

    // Save AI Planning setting
    suspend fun saveEnableAiPlanning(isEnabled: Boolean) {
        context.apiDataStore.edit { preferences -> preferences[ENABLE_AI_PLANNING] = isEnabled }
    }

    // Save Keep Screen On setting
    suspend fun saveKeepScreenOn(isEnabled: Boolean) {
        context.apiDataStore.edit { preferences -> preferences[KEEP_SCREEN_ON] = isEnabled }
    }

    // Save Thinking Mode setting
    suspend fun saveEnableThinkingMode(isEnabled: Boolean) {
        context.apiDataStore.edit { preferences -> preferences[ENABLE_THINKING_MODE] = isEnabled }
    }

    // Save Thinking Guidance setting
    suspend fun saveEnableThinkingGuidance(isEnabled: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[ENABLE_THINKING_GUIDANCE] = isEnabled
        }
    }

    // Save Memory Attachment setting
    suspend fun saveEnableMemoryAttachment(isEnabled: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[ENABLE_MEMORY_ATTACHMENT] = isEnabled
        }
    }

    // Save Context Length
    suspend fun saveContextLength(length: Float) {
        context.apiDataStore.edit { preferences ->
            preferences[CONTEXT_LENGTH] = length
        }
    }

    // Save Summary Token Threshold
    suspend fun saveSummaryTokenThreshold(threshold: Float) {
        context.apiDataStore.edit { preferences ->
            preferences[SUMMARY_TOKEN_THRESHOLD] = threshold
        }
    }

    // 保存显示和行为设置的方法，不会影响模型参数
    suspend fun saveDisplaySettings(
            showFpsCounter: Boolean,
            keepScreenOn: Boolean
    ) {
        context.apiDataStore.edit { preferences ->
            preferences[SHOW_FPS_COUNTER] = showFpsCounter
            preferences[KEEP_SCREEN_ON] = keepScreenOn
        }
    }

    // 保存自定义请求头
    suspend fun saveCustomHeaders(headersJson: String) {
        context.apiDataStore.edit { preferences ->
            preferences[CUSTOM_HEADERS] = headersJson
        }
    }

    // 读取自定义请求头
    suspend fun getCustomHeaders(): String {
        val preferences = context.apiDataStore.data.first()
        return preferences[CUSTOM_HEADERS] ?: DEFAULT_CUSTOM_HEADERS
    }

    /**
     * 更新指定功能类型的token计数
     * @param functionType 要更新的功能类型
     * @param inputTokens 新增的输入token
     * @param outputTokens 新增的输出token
     */
    suspend fun updateTokensForFunction(
            functionType: FunctionType,
            inputTokens: Int,
            outputTokens: Int
    ) {
        context.apiDataStore.edit { preferences ->
            val inputKey = getTokenInputKey(functionType)
            val outputKey = getTokenOutputKey(functionType)

            val currentInputTokens = preferences[inputKey] ?: 0
            val currentOutputTokens = preferences[outputKey] ?: 0

            preferences[inputKey] = currentInputTokens + inputTokens
            preferences[outputKey] = currentOutputTokens + outputTokens
        }
    }

    // Save custom prompts
    suspend fun saveCustomPrompts(introPrompt: String, tonePrompt: String) {
        context.apiDataStore.edit { preferences ->
            preferences[CUSTOM_INTRO_PROMPT] = introPrompt
            preferences[CUSTOM_TONE_PROMPT] = tonePrompt
        }
    }

    // Save custom system prompt template
    suspend fun saveCustomSystemPromptTemplate(template: String) {
        context.apiDataStore.edit { preferences ->
            preferences[CUSTOM_SYSTEM_PROMPT_TEMPLATE] = template
        }
    }

    // Reset custom prompts to default values
    suspend fun resetCustomPrompts() {
        context.apiDataStore.edit { preferences ->
            preferences[CUSTOM_INTRO_PROMPT] = DEFAULT_INTRO_PROMPT
            preferences[CUSTOM_TONE_PROMPT] = DEFAULT_TONE_PROMPT
        }
    }

    // Reset custom system prompt template to default
    suspend fun resetCustomSystemPromptTemplate() {
        context.apiDataStore.edit { preferences ->
            preferences[CUSTOM_SYSTEM_PROMPT_TEMPLATE] = DEFAULT_SYSTEM_PROMPT_TEMPLATE
        }
    }

    // 重置所有功能类型的token计数
    suspend fun resetAllFunctionTokenCounts() {
        context.apiDataStore.edit { preferences ->
            for (functionType in FunctionType.values()) {
                preferences[getTokenInputKey(functionType)] = 0
                preferences[getTokenOutputKey(functionType)] = 0
            }
        }
    }
}