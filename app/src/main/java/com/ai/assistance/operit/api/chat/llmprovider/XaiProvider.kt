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

/** Maps the app's five quality levels to xAI's supported reasoning efforts. */
internal object XaiReasoningMapper {
    private val enabledEfforts = listOf("low", "medium", "medium", "high", "xhigh")

    fun effortForQuality(enableThinking: Boolean, qualityLevel: Int): String {
        if (!enableThinking) {
            // xAI reasoning models cannot disable reasoning; use the lowest supported effort.
            return "low"
        }
        return enabledEfforts[qualityLevel.coerceIn(1, 5) - 1]
    }
}

internal fun xaiModelSupportsReasoningEffort(modelName: String): Boolean {
    val normalized = modelName.trim().lowercase()
    return normalized.startsWith("grok-4.5") || normalized.startsWith("grok-4.6")
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
            val existingEffort = requestJson.optString("reasoning_effort", "").trim()
            if (existingEffort.isEmpty()) {
                val qualityLevel = try {
                    runBlocking {
                        ApiPreferences.getInstance(context).thinkingQualityLevelFlow.first()
                    }
                } catch (error: Exception) {
                    AppLogger.e(
                        "XaiProvider",
                        "Failed to read thinking quality level; aborting xAI request",
                        error
                    )
                    throw error
                }

                requestJson.put(
                    "reasoning_effort",
                    XaiReasoningMapper.effortForQuality(enableThinking, qualityLevel)
                )
            }
        }

        return createJsonRequestBody(requestJson.toString())
    }
}
