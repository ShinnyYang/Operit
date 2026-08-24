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
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * NVIDIA API Catalog / NIM provider.
 *
 * Official docs expose model-specific reasoning_effort values for GPT-OSS and
 * the current Nemotron 3 Super/Ultra endpoints.
 *
 * The request mapper writes only the control published for the selected model.
 */
class NvidiaAIProvider(
    apiEndpoint: String,
    apiKeyProvider: ApiKeyProvider,
    modelName: String,
    client: OkHttpClient,
    customHeaders: Map<String, String> = emptyMap(),
    providerType: ApiProviderType = ApiProviderType.NVIDIA,
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
    providerType = providerType,
    supportsVision = supportsVision,
    supportsAudio = supportsAudio,
    supportsVideo = supportsVideo,
    enableToolCall = enableToolCall
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
        val baseRequestBodyJson = super.createRequestBodyInternal(
            context,
            chatHistory,
            modelParameters,
            stream,
            availableTools,
            preserveThinkInHistory
        )
        val jsonObject = JSONObject(baseRequestBodyJson)

        val mapping = ThinkingQualityMappingRegistry.resolve(ApiProviderType.NVIDIA.name, modelName)
        when (mapping.control) {
            ThinkingQualityControl.TOGGLE_ONLY -> {
                val chatTemplateKwargs = jsonObject.optJSONObject("chat_template_kwargs") ?: JSONObject()
                chatTemplateKwargs.put("enable_thinking", enableThinking)
                jsonObject.put("chat_template_kwargs", chatTemplateKwargs)
            }
            ThinkingQualityControl.LEVELS -> if (!jsonObject.has("reasoning_effort")) {
                val effort = if (enableThinking) resolveNvidiaReasoningEffort(context) else mapping.disabledValue
                if (effort != null) jsonObject.put("reasoning_effort", effort)
            }
            ThinkingQualityControl.UNSUPPORTED -> Unit
        }

        AppLogger.d(
            "NvidiaAIProvider",
            "NVIDIA thinking mapping applied: control=" + mapping.control + ", enabled=" + enableThinking
        )

        return createJsonRequestBody(jsonObject.toString())
    }

    private fun resolveNvidiaReasoningEffort(context: Context): String? {
        val qualityLevel = try {
            runBlocking { ApiPreferences.getInstance(context).thinkingOptionIdFlow.first() }
        } catch (error: Exception) {
            AppLogger.e("NvidiaAIProvider", "Failed to read thinking option id", error)
            throw error
        }

        val mapping = ThinkingQualityMappingRegistry.resolve(ApiProviderType.NVIDIA.name, modelName)
        return mapping.textValueFor(qualityLevel)
            ?: throw IllegalArgumentException("NVIDIA option is not supported: $qualityLevel")
    }
}
