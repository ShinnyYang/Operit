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

/**
 * OpenRouter provider.
 *
 * OpenRouter chat completions are largely OpenAI-compatible, but reasoning is controlled via
 * the unified `reasoning` object instead of the app's generic `enableThinking` toggle.
 *
 * Reasoning is emitted only when the model catalog declares a supported control,
 * using the model's published effort or max-token field.
 *
 * This provider keeps the shared OpenAI request/response handling while applying OpenRouter's
 * request-body conventions and default headers.
 */
open class OpenRouterProvider(
    apiEndpoint: String,
    apiKeyProvider: ApiKeyProvider,
    modelName: String,
    client: OkHttpClient,
    customHeaders: Map<String, String> = emptyMap(),
    providerType: ApiProviderType = ApiProviderType.OPENROUTER,
    supportsVision: Boolean = false,
    supportsAudio: Boolean = false,
    supportsVideo: Boolean = false,
    enableToolCall: Boolean = false
) : OpenAIProvider(
    apiEndpoint = apiEndpoint,
    apiKeyProvider = apiKeyProvider,
    modelName = modelName,
    client = client,
    customHeaders = mergeOpenRouterHeaders(customHeaders),
    providerType = providerType,
    supportsVision = supportsVision,
    supportsAudio = supportsAudio,
    supportsVideo = supportsVideo,
    enableToolCall = enableToolCall
) {

    private val openRouterApiEndpoint: String = apiEndpoint
    private val openRouterProviderType: ApiProviderType = providerType

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

        applyOpenRouterReasoning(
            context = context,
            requestJson = jsonObject,
            enableThinking = enableThinking
        )

        val logJson = JSONObject(jsonObject.toString())
        if (logJson.has("tools")) {
            val toolsArray = logJson.getJSONArray("tools")
            logJson.put("tools", "[${toolsArray.length()} tools omitted for brevity]")
        }
        val sanitizedLogJson = sanitizeImageDataForLogging(logJson)
        logLargeString(
            "OpenRouterProvider",
            sanitizedLogJson.toString(4),
            "Final OpenRouter request body: "
        )

        return createJsonRequestBody(jsonObject.toString())
    }

    private fun applyOpenRouterReasoning(
        context: Context,
        requestJson: JSONObject,
        enableThinking: Boolean
    ) {
        val reasoningObject = requestJson.optJSONObject("reasoning")
        val existingHasExplicitReasoningControl =
            reasoningObject?.let {
                it.has("enabled") || it.has("max_tokens") || it.has("effort")
            } == true

        when {
            reasoningObject == null && requestJson.has("reasoning") && !requestJson.isNull("reasoning") -> {
                AppLogger.w(
                    "OpenRouterProvider",
                    "Skipping OpenRouter reasoning adaptation because reasoning is not an object"
                )
            }

            existingHasExplicitReasoningControl -> {
                AppLogger.d(
                    "OpenRouterProvider",
                    "Preserving caller-supplied OpenRouter reasoning object"
                )
            }

            else -> {
                val finalReasoningObject = reasoningObject ?: JSONObject()
                val mapping = runBlocking {
                    ThinkingQualityMappingRegistry.resolveForModel(
                        openRouterProviderType.name,
                        modelName,
                        openRouterApiEndpoint,
                    )
                }
                if (mapping.control == ThinkingQualityControl.UNSUPPORTED) return
                val thinkingEnabled = enableThinking || mapping.reasoningRequired
                if (thinkingEnabled) {
                    val optionId = runBlocking { ApiPreferences.getInstance(context).thinkingOptionIdFlow.first() }
                    val selected = mapping.optionFor(optionId)
                    if (mapping.control == ThinkingQualityControl.LEVELS && selected == null) {
                        throw IllegalArgumentException("OpenRouter option is not supported: $optionId")
                    }
                    when (val wireValue = selected?.wireValue) {
                        is ThinkingQualityWireValue.Text -> finalReasoningObject.put("effort", wireValue.value)
                        is ThinkingQualityWireValue.Number -> finalReasoningObject.put("max_tokens", wireValue.value)
                        ThinkingQualityWireValue.Omitted, null -> Unit
                    }
                    requestJson.put("reasoning", finalReasoningObject)
                    AppLogger.d(
                        "OpenRouterProvider",
                        "OpenRouter thinking enabled via " + mapping.parameterLabel
                    )
                } else {
                    mapping.disabledValue?.let { finalReasoningObject.put("effort", it) }
                        ?: finalReasoningObject.put("enabled", false)
                    requestJson.put("reasoning", finalReasoningObject)
                    AppLogger.d(
                        "OpenRouterProvider",
                        "OpenRouter thinking disabled via reasoning.enabled=false"
                    )
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_HTTP_REFERER = "ai.assistance.operit"
        private const val DEFAULT_X_TITLE = "Assistance App"

        private fun mergeOpenRouterHeaders(customHeaders: Map<String, String>): Map<String, String> {
            val merged = linkedMapOf<String, String>()

            if (customHeaders.keys.none { it.equals("HTTP-Referer", ignoreCase = true) }) {
                merged["HTTP-Referer"] = DEFAULT_HTTP_REFERER
            }
            if (customHeaders.keys.none { it.equals("X-Title", ignoreCase = true) }) {
                merged["X-Title"] = DEFAULT_X_TITLE
            }

            merged.putAll(customHeaders)
            return merged
        }
    }
}
