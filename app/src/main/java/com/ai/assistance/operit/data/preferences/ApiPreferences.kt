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
import com.ai.assistance.operit.data.model.ApiProviderType
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

class ApiPreferences(private val context: Context) {

    // Define our preferences keys
    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val API_ENDPOINT = stringPreferencesKey("api_endpoint")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val API_PROVIDER_TYPE = stringPreferencesKey("api_provider_type")

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

        // Key for Waifu Mode
        val ENABLE_WAIFU_MODE = booleanPreferencesKey("enable_waifu_mode")
        val WAIFU_CHAR_DELAY = intPreferencesKey("waifu_char_delay") // 每字符延迟（毫秒）
        val WAIFU_REMOVE_PUNCTUATION = booleanPreferencesKey("waifu_remove_punctuation") // 是否移除标点符号
        val WAIFU_DISABLE_ACTIONS = booleanPreferencesKey("waifu_disable_actions") // 是否禁止动作表情
        val WAIFU_ENABLE_EMOTICONS = booleanPreferencesKey("waifu_enable_emoticons") // 是否启用表情包
        val WAIFU_ENABLE_SELFIE = booleanPreferencesKey("waifu_enable_selfie") // 是否启用自拍功能
        val WAIFU_SELFIE_PROMPT = stringPreferencesKey("waifu_selfie_prompt") // 自拍功能的外貌提示词

        // Key for Context Length
        val CONTEXT_LENGTH = floatPreferencesKey("context_length")

        // Key for Summary Token Threshold
        val SUMMARY_TOKEN_THRESHOLD = floatPreferencesKey("summary_token_threshold")

        // Custom Prompt Settings
        val CUSTOM_INTRO_PROMPT = stringPreferencesKey("custom_intro_prompt")
        val CUSTOM_TONE_PROMPT = stringPreferencesKey("custom_tone_prompt")
        
        // Custom System Prompt Template (Advanced Configuration)
        val CUSTOM_SYSTEM_PROMPT_TEMPLATE = stringPreferencesKey("custom_system_prompt_template")
        // Selected System Prompt Profile Id for global usage
        val CUSTOM_SYSTEM_PROMPT_PROFILE_ID = stringPreferencesKey("custom_system_prompt_profile_id")

        // DeepSeek Model Parameters - Keys for values
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val TOP_P = floatPreferencesKey("top_p")
        val TOP_K = intPreferencesKey("top_k")
        val PRESENCE_PENALTY = floatPreferencesKey("presence_penalty")
        val FREQUENCY_PENALTY = floatPreferencesKey("frequency_penalty")
        val REPETITION_PENALTY = floatPreferencesKey("repetition_penalty")

        // New: Keys for parameter enabled state
        val MAX_TOKENS_ENABLED = booleanPreferencesKey("max_tokens_enabled")
        val TEMPERATURE_ENABLED = booleanPreferencesKey("temperature_enabled")
        val TOP_P_ENABLED = booleanPreferencesKey("top_p_enabled")
        val TOP_K_ENABLED = booleanPreferencesKey("top_k_enabled")
        val PRESENCE_PENALTY_ENABLED = booleanPreferencesKey("presence_penalty_enabled")
        val FREQUENCY_PENALTY_ENABLED = booleanPreferencesKey("frequency_penalty_enabled")
        val REPETITION_PENALTY_ENABLED = booleanPreferencesKey("repetition_penalty_enabled")

        // Default values
        const val DEFAULT_API_ENDPOINT = "https://api.deepseek.com/v1/chat/completions"
        const val DEFAULT_MODEL_NAME = "deepseek-chat"

        // Obfuscated API Keys
        private const val ENCODED_API_KEY_OLD = "c2stZTU2NTM5MGMxNjRjNGNmYTg4MjA2MjRlZjQ3ZDY4YmY="
        private const val ENCODED_API_KEY = "c2stNmI4NTYyMjUzNmFjNDhjMDgwYzUwNDhhYjVmNWQxYmQ="

        private fun decodeApiKey(encodedKey: String): String {
            return try {
                android.util.Base64.decode(encodedKey, android.util.Base64.NO_WRAP)
                        .toString(Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e("ApiPreferences", "Failed to decode API key", e)
                ""
            }
        }

        val DEFAULT_API_KEY_OLD: String by lazy { decodeApiKey(ENCODED_API_KEY_OLD) }
        val DEFAULT_API_KEY: String by lazy { decodeApiKey(ENCODED_API_KEY) }

        const val DEFAULT_API_PROVIDER_TYPE = "DEEPSEEK"

        const val DEFAULT_SHOW_FPS_COUNTER = false
        const val DEFAULT_ENABLE_AI_PLANNING = false
        const val DEFAULT_KEEP_SCREEN_ON = true

        // Default values for Thinking Mode and Thinking Guidance
        const val DEFAULT_ENABLE_THINKING_MODE = false
        const val DEFAULT_ENABLE_THINKING_GUIDANCE = false

        // Default value for Memory Attachment
        const val DEFAULT_ENABLE_MEMORY_ATTACHMENT = true

        // Default value for Waifu Mode
        const val DEFAULT_ENABLE_WAIFU_MODE = false
        const val DEFAULT_WAIFU_CHAR_DELAY = 500 // 500ms per character (2 chars per second)
        const val DEFAULT_WAIFU_REMOVE_PUNCTUATION = false // 默认保留标点符号
        const val DEFAULT_WAIFU_DISABLE_ACTIONS = false // 默认允许动作表情
        const val DEFAULT_WAIFU_ENABLE_EMOTICONS = false // 默认不启用表情包
        const val DEFAULT_WAIFU_ENABLE_SELFIE = false // 默认不启用自拍功能
        const val DEFAULT_WAIFU_SELFIE_PROMPT = "kipfel (vrchat), long hair, Matcha color hair, purple eyes, sweater vest,  black skirt, black necktie, collared shirt, long sleeves, black headwear, beanie, pleated skirt, hair bun, white shirt, hair ribbon,  hairclip, hair between eyes, black footwear, blush, hair ornament, cat hat,  very long hair,sweater, animal ear headwear, bag, bandaid on leg, socks" // 默认外貌提示词

        // Default value for Context Length (in K)
        const val DEFAULT_CONTEXT_LENGTH = 48.0f

        // Default value for Summary Token Threshold
        const val DEFAULT_SUMMARY_TOKEN_THRESHOLD = 0.70f

        // Default values for custom prompts
        const val DEFAULT_INTRO_PROMPT = "你是Operit，一个全能AI助手，旨在解决用户提出的任何任务。你有各种工具可以调用，以高效完成复杂的请求。"
        const val DEFAULT_TONE_PROMPT = "保持有帮助的语气，并清楚地传达限制。使用问题库根据用户的风格、偏好和过去的信息个性化响应。"
        
        // Default system prompt template (empty means use built-in template)
        const val DEFAULT_SYSTEM_PROMPT_TEMPLATE = ""
        // Default selected system prompt profile id
        const val DEFAULT_SYSTEM_PROMPT_PROFILE_ID = "default"

        // Default values for DeepSeek model parameters
        const val DEFAULT_MAX_TOKENS = 4096
        const val DEFAULT_TEMPERATURE = 1.0f
        const val DEFAULT_TOP_P = 1.0f
        const val DEFAULT_TOP_K = 0
        const val DEFAULT_PRESENCE_PENALTY = 0.0f
        const val DEFAULT_FREQUENCY_PENALTY = 0.0f
        const val DEFAULT_REPETITION_PENALTY = 1.0f

        // Default enabled state for parameters (all disabled by default)
        const val DEFAULT_PARAM_ENABLED = false

        // 自定义参数存储键
        val CUSTOM_PARAMETERS = stringPreferencesKey("custom_parameters")

        // 自定义请求头存储键
        val CUSTOM_HEADERS = stringPreferencesKey("custom_headers")

        // 默认空的自定义参数列表
        const val DEFAULT_CUSTOM_PARAMETERS = "[]"
        const val DEFAULT_CUSTOM_HEADERS = "{}"
    }

    // Get API Key as Flow
    val apiKeyFlow: Flow<String> =
            context.apiDataStore.data.map { preferences ->
                val savedApiKey = preferences[API_KEY] ?: DEFAULT_API_KEY

                // 如果是旧API KEY，返回新的API KEY
                if (savedApiKey == DEFAULT_API_KEY_OLD) {
                    DEFAULT_API_KEY
                } else {
                    savedApiKey
                }
            }

    // Get API Endpoint as Flow
    val apiEndpointFlow: Flow<String> =
            context.apiDataStore.data.map { preferences ->
                preferences[API_ENDPOINT] ?: DEFAULT_API_ENDPOINT
            }

    // Get Model Name as Flow
    val modelNameFlow: Flow<String> =
            context.apiDataStore.data.map { preferences ->
                preferences[MODEL_NAME] ?: DEFAULT_MODEL_NAME
            }

    // Get API Provider Type as Flow
    val apiProviderTypeFlow: Flow<ApiProviderType> =
            context.apiDataStore.data.map { preferences ->
                try {
                    ApiProviderType.valueOf(
                            preferences[API_PROVIDER_TYPE] ?: DEFAULT_API_PROVIDER_TYPE
                    )
                } catch (e: Exception) {
                    ApiProviderType.DEEPSEEK
                }
            }

    // DeepSeek Model Parameter Flows - Values
    val maxTokensFlow: Flow<Int> =
            context.apiDataStore.data.map { preferences ->
                preferences[MAX_TOKENS] ?: DEFAULT_MAX_TOKENS
            }

    val temperatureFlow: Flow<Float> =
            context.apiDataStore.data.map { preferences ->
                preferences[TEMPERATURE] ?: DEFAULT_TEMPERATURE
            }

    val topPFlow: Flow<Float> =
            context.apiDataStore.data.map { preferences -> preferences[TOP_P] ?: DEFAULT_TOP_P }

    val topKFlow: Flow<Int> =
            context.apiDataStore.data.map { preferences -> preferences[TOP_K] ?: DEFAULT_TOP_K }

    val presencePenaltyFlow: Flow<Float> =
            context.apiDataStore.data.map { preferences ->
                preferences[PRESENCE_PENALTY] ?: DEFAULT_PRESENCE_PENALTY
            }

    val frequencyPenaltyFlow: Flow<Float> =
            context.apiDataStore.data.map { preferences ->
                preferences[FREQUENCY_PENALTY] ?: DEFAULT_FREQUENCY_PENALTY
            }

    val repetitionPenaltyFlow: Flow<Float> =
            context.apiDataStore.data.map { preferences ->
                preferences[REPETITION_PENALTY] ?: DEFAULT_REPETITION_PENALTY
            }

    // DeepSeek Model Parameter Flows - Enabled state
    val maxTokensEnabledFlow: Flow<Boolean> =
            context.apiDataStore.data.map { preferences ->
                preferences[MAX_TOKENS_ENABLED] ?: DEFAULT_PARAM_ENABLED
            }

    val temperatureEnabledFlow: Flow<Boolean> =
            context.apiDataStore.data.map { preferences ->
                preferences[TEMPERATURE_ENABLED] ?: DEFAULT_PARAM_ENABLED
            }

    val topPEnabledFlow: Flow<Boolean> =
            context.apiDataStore.data.map { preferences ->
                preferences[TOP_P_ENABLED] ?: DEFAULT_PARAM_ENABLED
            }

    val topKEnabledFlow: Flow<Boolean> =
            context.apiDataStore.data.map { preferences ->
                preferences[TOP_K_ENABLED] ?: DEFAULT_PARAM_ENABLED
            }

    val presencePenaltyEnabledFlow: Flow<Boolean> =
            context.apiDataStore.data.map { preferences ->
                preferences[PRESENCE_PENALTY_ENABLED] ?: DEFAULT_PARAM_ENABLED
            }

    val frequencyPenaltyEnabledFlow: Flow<Boolean> =
            context.apiDataStore.data.map { preferences ->
                preferences[FREQUENCY_PENALTY_ENABLED] ?: DEFAULT_PARAM_ENABLED
            }

    val repetitionPenaltyEnabledFlow: Flow<Boolean> =
            context.apiDataStore.data.map { preferences ->
                preferences[REPETITION_PENALTY_ENABLED] ?: DEFAULT_PARAM_ENABLED
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

    // Flow for Waifu Mode
    val enableWaifuModeFlow: Flow<Boolean> =
        context.apiDataStore.data.map { preferences ->
            preferences[ENABLE_WAIFU_MODE] ?: DEFAULT_ENABLE_WAIFU_MODE
        }

    val waifuCharDelayFlow: Flow<Int> =
        context.apiDataStore.data.map { preferences ->
            preferences[WAIFU_CHAR_DELAY] ?: DEFAULT_WAIFU_CHAR_DELAY
        }

    val waifuRemovePunctuationFlow: Flow<Boolean> =
        context.apiDataStore.data.map { preferences ->
            preferences[WAIFU_REMOVE_PUNCTUATION] ?: DEFAULT_WAIFU_REMOVE_PUNCTUATION
        }

    val waifuDisableActionsFlow: Flow<Boolean> =
        context.apiDataStore.data.map { preferences ->
            preferences[WAIFU_DISABLE_ACTIONS] ?: DEFAULT_WAIFU_DISABLE_ACTIONS
        }

    val waifuEnableEmoticonsFlow: Flow<Boolean> =
        context.apiDataStore.data.map { preferences ->
            preferences[WAIFU_ENABLE_EMOTICONS] ?: DEFAULT_WAIFU_ENABLE_EMOTICONS
        }

    val waifuEnableSelfieFlow: Flow<Boolean> =
        context.apiDataStore.data.map { preferences ->
            preferences[WAIFU_ENABLE_SELFIE] ?: DEFAULT_WAIFU_ENABLE_SELFIE
        }

    val waifuSelfiePromptFlow: Flow<String> =
        context.apiDataStore.data.map { preferences ->
            preferences[WAIFU_SELFIE_PROMPT] ?: DEFAULT_WAIFU_SELFIE_PROMPT
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

    // Selected Global System Prompt Profile Id Flow
    val customSystemPromptProfileIdFlow: Flow<String> =
        context.apiDataStore.data.map { preferences ->
            preferences[CUSTOM_SYSTEM_PROMPT_PROFILE_ID] ?: DEFAULT_SYSTEM_PROMPT_PROFILE_ID
        }

    // Flow for Custom Headers
    val customHeadersFlow: Flow<String> =
        context.apiDataStore.data.map { preferences ->
            preferences[CUSTOM_HEADERS] ?: DEFAULT_CUSTOM_HEADERS
        }

    // Save API Key
    suspend fun saveApiKey(apiKey: String) {
        context.apiDataStore.edit { preferences -> preferences[API_KEY] = apiKey }
    }

    // Save API Endpoint
    suspend fun saveApiEndpoint(endpoint: String) {
        context.apiDataStore.edit { preferences -> preferences[API_ENDPOINT] = endpoint }
    }

    // Save Model Name
    suspend fun saveModelName(modelName: String) {
        context.apiDataStore.edit { preferences -> preferences[MODEL_NAME] = modelName }
    }

    // Save API Provider Type
    suspend fun saveApiProviderType(apiProviderType: ApiProviderType) {
        context.apiDataStore.edit { preferences ->
            preferences[API_PROVIDER_TYPE] = apiProviderType.name
        }
    }

    // Save DeepSeek Model Parameters - Value and enabled state
    suspend fun saveMaxTokens(maxTokens: Int, isEnabled: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[MAX_TOKENS] = maxTokens
            preferences[MAX_TOKENS_ENABLED] = isEnabled
        }
    }

    suspend fun saveTemperature(temperature: Float, isEnabled: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[TEMPERATURE] = temperature
            preferences[TEMPERATURE_ENABLED] = isEnabled
        }
    }

    suspend fun saveTopP(topP: Float, isEnabled: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[TOP_P] = topP
            preferences[TOP_P_ENABLED] = isEnabled
        }
    }

    suspend fun saveTopK(topK: Int, isEnabled: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[TOP_K] = topK
            preferences[TOP_K_ENABLED] = isEnabled
        }
    }

    suspend fun savePresencePenalty(presencePenalty: Float, isEnabled: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[PRESENCE_PENALTY] = presencePenalty
            preferences[PRESENCE_PENALTY_ENABLED] = isEnabled
        }
    }

    suspend fun saveFrequencyPenalty(frequencyPenalty: Float, isEnabled: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[FREQUENCY_PENALTY] = frequencyPenalty
            preferences[FREQUENCY_PENALTY_ENABLED] = isEnabled
        }
    }

    suspend fun saveRepetitionPenalty(repetitionPenalty: Float, isEnabled: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[REPETITION_PENALTY] = repetitionPenalty
            preferences[REPETITION_PENALTY_ENABLED] = isEnabled
        }
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

    // Save Waifu Mode setting
    suspend fun saveEnableWaifuMode(isEnabled: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[ENABLE_WAIFU_MODE] = isEnabled
        }
    }

    suspend fun saveWaifuCharDelay(delayMs: Int) {
        context.apiDataStore.edit { preferences ->
            preferences[WAIFU_CHAR_DELAY] = delayMs
        }
    }

    suspend fun saveWaifuRemovePunctuation(removePunctuation: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[WAIFU_REMOVE_PUNCTUATION] = removePunctuation
        }
    }

    suspend fun saveWaifuDisableActions(disableActions: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[WAIFU_DISABLE_ACTIONS] = disableActions
        }
    }

    suspend fun saveWaifuEnableEmoticons(enableEmoticons: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[WAIFU_ENABLE_EMOTICONS] = enableEmoticons
        }
    }

    suspend fun saveWaifuEnableSelfie(enableSelfie: Boolean) {
        context.apiDataStore.edit { preferences ->
            preferences[WAIFU_ENABLE_SELFIE] = enableSelfie
        }
    }

    suspend fun saveWaifuSelfiePrompt(selfiePrompt: String) {
        context.apiDataStore.edit { preferences ->
            preferences[WAIFU_SELFIE_PROMPT] = selfiePrompt
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

    // Save all settings at once
    suspend fun saveApiSettings(apiKey: String, endpoint: String, modelName: String) {
        context.apiDataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
            preferences[API_ENDPOINT] = endpoint
            preferences[MODEL_NAME] = modelName
        }
    }

    // 添加包含ApiProviderType参数的新saveApiSettings方法
    suspend fun saveApiSettings(
            apiKey: String,
            endpoint: String,
            modelName: String,
            apiProviderType: ApiProviderType
    ) {
        context.apiDataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
            preferences[API_ENDPOINT] = endpoint
            preferences[MODEL_NAME] = modelName
            preferences[API_PROVIDER_TYPE] = apiProviderType.name
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

    // Get all model parameters as a list
    suspend fun getAllModelParameters(): List<ModelParameter<*>> {
        val preferences = context.apiDataStore.data.first()
        val parameters = mutableListOf<ModelParameter<*>>()

        // 添加标准参数
        // Max Tokens
        parameters.add(
                ModelParameter(
                        id = "max_tokens",
                        name = "最大生成Token数",
                        apiName = "max_tokens",
                        description = "控制AI每次最多生成的Token数量",
                        defaultValue = DEFAULT_MAX_TOKENS,
                        currentValue = preferences[MAX_TOKENS] ?: DEFAULT_MAX_TOKENS,
                        isEnabled = preferences[MAX_TOKENS_ENABLED] ?: DEFAULT_PARAM_ENABLED,
                        valueType = ParameterValueType.INT,
                        minValue = 1,
                        maxValue = 16000,
                        category = ParameterCategory.GENERATION
                )
        )

        // Temperature
        parameters.add(
                ModelParameter(
                        id = "temperature",
                        name = "温度",
                        apiName = "temperature",
                        description = "控制输出的随机性。较低的值更确定性，较高的值更随机",
                        defaultValue = DEFAULT_TEMPERATURE,
                        currentValue = preferences[TEMPERATURE] ?: DEFAULT_TEMPERATURE,
                        isEnabled = preferences[TEMPERATURE_ENABLED] ?: DEFAULT_PARAM_ENABLED,
                        valueType = ParameterValueType.FLOAT,
                        minValue = 0.0f,
                        maxValue = 2.0f,
                        category = ParameterCategory.CREATIVITY
                )
        )

        // Top P
        parameters.add(
                ModelParameter(
                        id = "top_p",
                        name = "Top-P 采样",
                        apiName = "top_p",
                        description = "作为温度的替代方案，模型仅考虑概率最高的Top-P比例的token",
                        defaultValue = DEFAULT_TOP_P,
                        currentValue = preferences[TOP_P] ?: DEFAULT_TOP_P,
                        isEnabled = preferences[TOP_P_ENABLED] ?: DEFAULT_PARAM_ENABLED,
                        valueType = ParameterValueType.FLOAT,
                        minValue = 0.0f,
                        maxValue = 1.0f,
                        category = ParameterCategory.CREATIVITY
                )
        )

        // Top K
        parameters.add(
                ModelParameter(
                        id = "top_k",
                        name = "Top-K 采样",
                        apiName = "top_k",
                        description = "模型仅考虑概率最高的K个token。0表示禁用",
                        defaultValue = DEFAULT_TOP_K,
                        currentValue = preferences[TOP_K] ?: DEFAULT_TOP_K,
                        isEnabled = preferences[TOP_K_ENABLED] ?: DEFAULT_PARAM_ENABLED,
                        valueType = ParameterValueType.INT,
                        minValue = 0,
                        maxValue = 100,
                        category = ParameterCategory.CREATIVITY
                )
        )

        // Presence Penalty
        parameters.add(
                ModelParameter(
                        id = "presence_penalty",
                        name = "存在惩罚",
                        apiName = "presence_penalty",
                        description = "增强模型谈论新主题的倾向。值越高，惩罚越大",
                        defaultValue = DEFAULT_PRESENCE_PENALTY,
                        currentValue = preferences[PRESENCE_PENALTY] ?: DEFAULT_PRESENCE_PENALTY,
                        isEnabled = preferences[PRESENCE_PENALTY_ENABLED] ?: DEFAULT_PARAM_ENABLED,
                        valueType = ParameterValueType.FLOAT,
                        minValue = -2.0f,
                        maxValue = 2.0f,
                        category = ParameterCategory.REPETITION
                )
        )

        // Frequency Penalty
        parameters.add(
                ModelParameter(
                        id = "frequency_penalty",
                        name = "频率惩罚",
                        apiName = "frequency_penalty",
                        description = "减少模型重复同一词语的可能性。值越高，惩罚越大",
                        defaultValue = DEFAULT_FREQUENCY_PENALTY,
                        currentValue = preferences[FREQUENCY_PENALTY] ?: DEFAULT_FREQUENCY_PENALTY,
                        isEnabled = preferences[FREQUENCY_PENALTY_ENABLED] ?: DEFAULT_PARAM_ENABLED,
                        valueType = ParameterValueType.FLOAT,
                        minValue = -2.0f,
                        maxValue = 2.0f,
                        category = ParameterCategory.REPETITION
                )
        )

        // Repetition Penalty
        parameters.add(
                ModelParameter(
                        id = "repetition_penalty",
                        name = "重复惩罚",
                        apiName = "repetition_penalty",
                        description = "进一步减少重复。1.0表示不惩罚，大于1.0会降低重复可能性",
                        defaultValue = DEFAULT_REPETITION_PENALTY,
                        currentValue = preferences[REPETITION_PENALTY]
                                        ?: DEFAULT_REPETITION_PENALTY,
                        isEnabled = preferences[REPETITION_PENALTY_ENABLED]
                                        ?: DEFAULT_PARAM_ENABLED,
                        valueType = ParameterValueType.FLOAT,
                        minValue = 0.0f,
                        maxValue = 2.0f,
                        category = ParameterCategory.REPETITION
                )
        )

        // 加载自定义参数
        val customParamsJson = preferences[CUSTOM_PARAMETERS] ?: DEFAULT_CUSTOM_PARAMETERS
        if (customParamsJson != DEFAULT_CUSTOM_PARAMETERS) {
            try {
                val customParamsList =
                        Json.decodeFromString<List<CustomParameterData>>(customParamsJson)
                for (customParam in customParamsList) {
                    val param =
                            when (ParameterValueType.valueOf(customParam.valueType)) {
                                ParameterValueType.INT -> {
                                    ModelParameter(
                                            id = customParam.id,
                                            name = customParam.name,
                                            apiName = customParam.apiName,
                                            description = customParam.description,
                                            defaultValue = customParam.defaultValue.toInt(),
                                            currentValue = customParam.currentValue.toInt(),
                                            isEnabled = customParam.isEnabled,
                                            valueType = ParameterValueType.INT,
                                            minValue = customParam.minValue?.toInt(),
                                            maxValue = customParam.maxValue?.toInt(),
                                            category =
                                                    ParameterCategory.valueOf(customParam.category),
                                            isCustom = true
                                    )
                                }
                                ParameterValueType.FLOAT -> {
                                    ModelParameter(
                                            id = customParam.id,
                                            name = customParam.name,
                                            apiName = customParam.apiName,
                                            description = customParam.description,
                                            defaultValue = customParam.defaultValue.toFloat(),
                                            currentValue = customParam.currentValue.toFloat(),
                                            isEnabled = customParam.isEnabled,
                                            valueType = ParameterValueType.FLOAT,
                                            minValue = customParam.minValue?.toFloat(),
                                            maxValue = customParam.maxValue?.toFloat(),
                                            category =
                                                    ParameterCategory.valueOf(customParam.category),
                                            isCustom = true
                                    )
                                }
                                ParameterValueType.STRING -> {
                                    ModelParameter(
                                            id = customParam.id,
                                            name = customParam.name,
                                            apiName = customParam.apiName,
                                            description = customParam.description,
                                            defaultValue = customParam.defaultValue,
                                            currentValue = customParam.currentValue,
                                            isEnabled = customParam.isEnabled,
                                            valueType = ParameterValueType.STRING,
                                            category =
                                                    ParameterCategory.valueOf(customParam.category),
                                            isCustom = true
                                    )
                                }
                                ParameterValueType.BOOLEAN -> {
                                    ModelParameter(
                                            id = customParam.id,
                                            name = customParam.name,
                                            apiName = customParam.apiName,
                                            description = customParam.description,
                                            defaultValue = customParam.defaultValue.toBoolean(),
                                            currentValue = customParam.currentValue.toBoolean(),
                                            isEnabled = customParam.isEnabled,
                                            valueType = ParameterValueType.BOOLEAN,
                                            category =
                                                    ParameterCategory.valueOf(customParam.category),
                                            isCustom = true
                                    )
                                }
                            }
                    parameters.add(param)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 如果解析失败，忽略自定义参数
            }
        }

        return parameters
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

    // Save selected global system prompt profile id
    suspend fun saveCustomSystemPromptProfileId(profileId: String) {
        context.apiDataStore.edit { preferences ->
            preferences[CUSTOM_SYSTEM_PROMPT_PROFILE_ID] = profileId
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

    // Reset selected global system prompt profile id to default
    suspend fun resetCustomSystemPromptProfileId() {
        context.apiDataStore.edit { preferences ->
            preferences[CUSTOM_SYSTEM_PROMPT_PROFILE_ID] = DEFAULT_SYSTEM_PROMPT_PROFILE_ID
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

    // 添加确保初始化API提供商类型的方法
    suspend fun ensureApiProviderTypeInitialized() {
        // 检查API提供商类型是否已设置，如果没有则设置默认值
        val preferences = context.apiDataStore.data.first()
        if (preferences[API_PROVIDER_TYPE] == null) {
            Log.d("ApiPreferences", "初始化默认API提供商类型: $DEFAULT_API_PROVIDER_TYPE")
            saveApiProviderType(ApiProviderType.DEEPSEEK)
        }
    }
}

/** 用于序列化存储自定义参数的数据类 */
@Serializable
data class CustomParameterData(
        val id: String,
        val name: String,
        val apiName: String,
        val description: String,
        val defaultValue: String,
        val currentValue: String,
        val isEnabled: Boolean = false,
        val valueType: String,
        val minValue: String? = null,
        val maxValue: String? = null,
        val category: String
)
