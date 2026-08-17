package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.data.model.ModelConfigData
import com.ai.assistance.operit.data.model.ModelOption
import com.ai.assistance.operit.data.preferences.ModelConfigManager
import okhttp3.OkHttpClient

/** Routes OpenCode Zen/Go models to the protocol-specific provider already used by Operit. */
class OpenCodeProvider private constructor(
    private val delegate: AIService,
    private val baseEndpoint: String,
    private val modelName: String,
    private val apiKeyProvider: ApiKeyProvider
) : AIService by delegate {
    // Keep the routed provider identity so shared response handling recognizes Responses/Gemini streams.
    override val providerModel: String = delegate.providerModel

    override suspend fun getModelsList(context: Context): Result<List<ModelOption>> {
        return ModelListFetcher.getModelsList(
            context = context,
            apiKey = apiKeyProvider.getApiKey(),
            apiEndpoint = baseEndpoint,
            apiProviderType = ApiProviderType.OPENCODE
        )
    }

    companion object {
        fun create(
            config: ModelConfigData,
            modelConfigManager: ModelConfigManager,
            context: Context,
            client: OkHttpClient,
            customHeaders: Map<String, String>,
            apiKeyProvider: ApiKeyProvider,
            supportsVision: Boolean,
            supportsAudio: Boolean,
            supportsVideo: Boolean,
            enableToolCall: Boolean
        ): AIService {
            val model = config.modelName.trim().removePrefix("opencode/").removePrefix("opencode-go/")
            val endpoint = OpenCodeRouting.endpointFor(config.apiEndpoint, model)
            val provider = OpenCodeRouting.protocolFor(config.apiEndpoint, model)
            val routedConfig = config.copy(
                apiEndpoint = endpoint,
                modelName = model,
                apiProviderType = provider,
                apiProviderTypeId = provider.name
            )
            val routed = AIServiceFactory.buildRoutedService(
                config = routedConfig,
                modelConfigManager = modelConfigManager,
                context = context,
                client = client,
                customHeaders = customHeaders,
                apiKeyProvider = apiKeyProvider,
                supportsVision = supportsVision,
                supportsAudio = supportsAudio,
                supportsVideo = supportsVideo,
                enableToolCall = enableToolCall
            )
            return OpenCodeProvider(routed, config.apiEndpoint, model, apiKeyProvider)
        }

    }
}

internal object OpenCodeRouting {
    fun protocolFor(baseEndpoint: String, modelName: String): ApiProviderType {
        val model = modelName.lowercase()
        return when {
            model.startsWith("gpt-") || model.startsWith("grok-") || model.contains("codex") ->
                ApiProviderType.OPENAI_RESPONSES_GENERIC
            model.startsWith("claude-") || model.startsWith("qwen") ||
                (isGo(baseEndpoint) && model.startsWith("minimax-")) ->
                ApiProviderType.ANTHROPIC_GENERIC
            model.startsWith("gemini-") -> ApiProviderType.GEMINI_GENERIC
            else -> ApiProviderType.OPENAI_GENERIC
        }
    }

    fun endpointFor(baseEndpoint: String, modelName: String): String {
        val base = normalizedBase(baseEndpoint)
        return when (protocolFor(baseEndpoint, modelName)) {
            ApiProviderType.OPENAI_RESPONSES_GENERIC -> "$base/responses"
            ApiProviderType.ANTHROPIC_GENERIC -> "$base/messages"
            ApiProviderType.GEMINI_GENERIC -> "$base/models/$modelName"
            else -> "$base/chat/completions"
        }
    }

    fun modelsEndpoint(baseEndpoint: String): String = "${normalizedBase(baseEndpoint)}/models"

    private fun normalizedBase(endpoint: String): String {
        val trimmed = endpoint.trim().removeSuffix("/")
        return when {
            trimmed.endsWith("/v1") -> trimmed
            trimmed.endsWith("/zen") || trimmed.endsWith("/zen/go") -> "$trimmed/v1"
            else -> "$trimmed/v1"
        }
    }

    private fun isGo(endpoint: String): Boolean = endpoint.trim().removeSuffix("/").endsWith("/zen/go")
}
