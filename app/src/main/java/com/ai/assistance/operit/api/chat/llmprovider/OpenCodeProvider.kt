package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import com.ai.assistance.operit.core.chat.hooks.PromptTurn
import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.data.model.ModelConfigData
import com.ai.assistance.operit.data.model.ModelOption
import com.ai.assistance.operit.data.model.ModelParameter
import com.ai.assistance.operit.data.model.ToolPrompt
import com.ai.assistance.operit.data.preferences.ApiPreferences
import com.ai.assistance.operit.data.preferences.ModelConfigManager
import com.ai.assistance.operit.data.stats.ProviderUsageSnapshot
import com.ai.assistance.operit.data.stats.TokenStatCategory
import com.ai.assistance.operit.util.stream.Stream
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient

/** Routes OpenCode Zen/Go models to the protocol-specific provider already used by Operit. */
class OpenCodeProvider private constructor(
    private val delegate: AIService,
    private val baseEndpoint: String,
    private val modelName: String,
    private val protocol: ApiProviderType,
    private val client: OkHttpClient,
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

    override suspend fun sendMessage(
        context: Context,
        chatHistory: List<PromptTurn>,
        modelParameters: List<ModelParameter<*>>,
        enableThinking: Boolean,
        stream: Boolean,
        availableTools: List<ToolPrompt>?,
        preserveThinkInHistory: Boolean,
        onTokensUpdated: suspend (input: Long, cachedInput: Long, output: Long) -> Unit,
        onUsageReported: (suspend (ProviderUsageSnapshot, attempt: Int) -> Unit)?,
        onNonFatalError: suspend (error: String) -> Unit,
        enableRetry: Boolean,
        statsCategory: TokenStatCategory?
    ): Stream<String> {
        val qualityLevel = if (enableThinking) {
            try {
                ApiPreferences.getInstance(context).thinkingQualityLevelFlow.first()
            } catch (_: Exception) {
                3
            }
        } else {
            1
        }
        val capability = OpenCodeModelCatalog.resolve(client, baseEndpoint, modelName)
        val variant = OpenCodeReasoningMapper.select(capability, enableThinking, qualityLevel)
        val opencodeParameters = OpenCodeReasoningParameters.forVariant(
            protocol = protocol,
            modelName = modelName,
            capability = capability,
            variant = variant
        )

        return delegate.sendMessage(
            context = context,
            chatHistory = chatHistory,
            modelParameters = modelParameters + opencodeParameters,
            enableThinking = enableThinking,
            stream = stream,
            availableTools = availableTools,
            preserveThinkInHistory = preserveThinkInHistory,
            onTokensUpdated = onTokensUpdated,
            onUsageReported = onUsageReported,
            onNonFatalError = onNonFatalError,
            enableRetry = enableRetry,
            statsCategory = statsCategory
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
            return OpenCodeProvider(
                delegate = routed,
                baseEndpoint = config.apiEndpoint,
                modelName = model,
                protocol = provider,
                client = client,
                apiKeyProvider = apiKeyProvider
            )
        }
    }
}

internal object OpenCodeRouting {
    fun protocolFor(baseEndpoint: String, modelName: String): ApiProviderType {
        val model = modelName.lowercase()
        return when {
            model.startsWith("gpt-") || model.startsWith("grok-") || model.contains("codex") ->
                ApiProviderType.OPENAI_RESPONSES_GENERIC
            model.startsWith("claude-") || model.startsWith("qwen") || model.startsWith("minimax-") ->
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

    fun catalogProviderId(baseEndpoint: String): String =
        if (isGo(baseEndpoint)) "opencode-go" else "opencode"

    private fun normalizedBase(endpoint: String): String {
        val trimmed = endpoint.trim().removeSuffix("/")
        return if (trimmed.endsWith("/v1")) trimmed else "$trimmed/v1"
    }

    private fun isGo(endpoint: String): Boolean {
        val trimmed = endpoint.trim().removeSuffix("/").lowercase()
        return trimmed.endsWith("/zen/go") || trimmed.endsWith("/zen/go/v1")
    }
}
