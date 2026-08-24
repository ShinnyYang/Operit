package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import com.ai.assistance.operit.core.chat.hooks.PromptTurn
import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.data.model.ModelParameter
import com.ai.assistance.operit.data.model.ToolPrompt
import com.ai.assistance.operit.data.preferences.ApiPreferences
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.json.JSONObject

/** Selects one of the efforts supported by the current Grok model. */
internal object XaiReasoningMapper {
    fun effortForOption(optionId: String): String = optionId
}

internal fun xaiModelSupportsReasoningEffort(modelName: String): Boolean {
    val normalized = modelName.trim().lowercase()
    return normalized.isNotEmpty()
}

/** xAI's OpenAI-compatible Chat Completions provider for Grok models. */
class XaiProvider(
    apiEndpoint: String,
    apiKeyProvider: ApiKeyProvider,
    modelName: String,
    client: OkHttpClient,
    customHeaders: Map<String, String> = emptyMap(),
    supportsVision: Boolean = false,
    supportsAudio: Boolean = false,
    supportsVideo: Boolean = false,
    enableToolCall: Boolean = false
) : OpenAIProvider(
    apiEndpoint = apiEndpoint,
    apiKeyProvider = apiKeyProvider,
    modelName = modelName,
    client = client,
    customHeaders = customHeaders,
    providerType = ApiProviderType.XAI,
    supportsVision = supportsVision,
    supportsAudio = supportsAudio,
    supportsVideo = supportsVideo,
    enableToolCall = enableToolCall,
    includeUsageInStream = true
) {
    override fun createRequestBody(
        context: Context,
        chatHistory: List<PromptTurn>,
        modelParameters: List<ModelParameter<*>>,
        enableThinking: Boolean,
        stream: Boolean,
        availableTools: List<ToolPrompt>?,
        preserveThinkInHistory: Boolean
    ): RequestBody {
        val requestJson = JSONObject(
            super.createRequestBodyInternal(
                context,
                chatHistory,
                modelParameters,
                stream,
                availableTools,
                preserveThinkInHistory
            )
        )

        if (xaiModelSupportsReasoningEffort(modelName)) {
            val mapping = ThinkingQualityMappingRegistry.resolve(ApiProviderType.XAI.name, modelName)
            if (!enableThinking && !mapping.reasoningRequired) {
                return createJsonRequestBody(requestJson.toString())
            }
            val existingEffort = requestJson.optString("reasoning_effort", "").trim()
            if (existingEffort.isEmpty()) {
                val optionId = try {
                    runBlocking {
                        ApiPreferences.getInstance(context).thinkingOptionIdFlow.first()
                    }
                } catch (error: Exception) {
                    AppLogger.e(
                        "XaiProvider",
                        "Failed to read thinking option id; aborting xAI request",
                        error
                    )
                    throw error
                }

                val effort = mapping.textValueFor(optionId)
                    ?: throw IllegalArgumentException("xAI option is not supported: $optionId")
                requestJson.put("reasoning_effort", effort)
            }
        }

        return createJsonRequestBody(requestJson.toString())
    }
}
