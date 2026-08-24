package com.ai.assistance.operit.api.chat.llmprovider

import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.util.AppLogger
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

internal enum class ThinkingQualityControl { LEVELS, TOGGLE_ONLY, UNSUPPORTED }

internal sealed interface ThinkingQualityWireValue {
    data class Text(val value: String) : ThinkingQualityWireValue
    data class Number(val value: Int) : ThinkingQualityWireValue
    data object Omitted : ThinkingQualityWireValue
}

internal data class ThinkingQualityOption(
    val id: String,
    val displayLabel: String,
    val wireValue: ThinkingQualityWireValue,
)

internal data class ThinkingQualityMapping(
    val control: ThinkingQualityControl,
    val parameterLabel: String,
    val options: List<ThinkingQualityOption>,
    val reasoningRequired: Boolean = false,
    val disabledValue: String? = null,
) {
    companion object {
        fun toggleOnly(parameterLabel: String, reasoningRequired: Boolean = false): ThinkingQualityMapping =
            ThinkingQualityMapping(ThinkingQualityControl.TOGGLE_ONLY, parameterLabel, emptyList(), reasoningRequired)

        fun unsupported(): ThinkingQualityMapping = ThinkingQualityMapping(ThinkingQualityControl.UNSUPPORTED, "", emptyList())
    }

    fun optionFor(id: String): ThinkingQualityOption? = options.firstOrNull { it.id == id }
    fun textValueFor(id: String): String? = (optionFor(id)?.wireValue as? ThinkingQualityWireValue.Text)?.value
    fun numberValueFor(id: String): Int? = (optionFor(id)?.wireValue as? ThinkingQualityWireValue.Number)?.value
}

internal object ThinkingQualityMappingRegistry {
    fun resolve(providerTypeId: String, modelName: String): ThinkingQualityMapping {
        val providerType = providerTypeId.trim().uppercase(Locale.US)
        val model = modelName.trim().lowercase(Locale.US)
        return when (providerType) {
            ApiProviderType.XAI.name -> xaiMapping(model)
            ApiProviderType.OPENAI.name, ApiProviderType.OPENAI_GENERIC.name -> openAiMapping(model, false)
            ApiProviderType.OPENAI_RESPONSES.name, ApiProviderType.OPENAI_RESPONSES_GENERIC.name, ApiProviderType.OPENAI_CODEX.name -> openAiMapping(model, true)
            ApiProviderType.GOOGLE.name, ApiProviderType.GEMINI_GENERIC.name -> geminiMapping(model)
            ApiProviderType.NVIDIA.name -> nvidiaMapping(model)
            ApiProviderType.DEEPSEEK.name -> deepSeekMapping(model)
            ApiProviderType.SILICONFLOW.name -> siliconFlowMapping(model)
            ApiProviderType.ANTHROPIC.name, ApiProviderType.ANTHROPIC_GENERIC.name -> anthropicMapping(model)
            ApiProviderType.MNN.name -> ThinkingQualityMapping.toggleOnly("enable_thinking")
            ApiProviderType.LLAMA_CPP.name -> ThinkingQualityMapping.toggleOnly("enable_thinking")
            ApiProviderType.OPENROUTER.name, ApiProviderType.NOUS_PORTAL.name ->
                numberMapping("reasoning.max_tokens", listOf(1_024, 8_192, 16_384, 32_768, 65_536))
            else -> ThinkingQualityMapping.toggleOnly("enable_thinking")
        }
    }

    suspend fun resolveForModel(providerTypeId: String, modelName: String, apiEndpoint: String): ThinkingQualityMapping {
        val providerType = providerTypeId.trim().uppercase(Locale.US)
        if (providerType == ApiProviderType.OPENCODE.name) {
            val capability = try {
                OpenCodeModelCatalog.resolve(SharedHttpClient.instance, apiEndpoint, modelName)
            } catch (error: Exception) {
                AppLogger.w("ThinkingQualityMapping", "OpenCode model capability lookup failed", error)
                return genericOpenCodeMapping(OpenCodeRouting.protocolFor(apiEndpoint, modelName))
            } ?: return genericOpenCodeMapping(OpenCodeRouting.protocolFor(apiEndpoint, modelName))
            if (!capability.reasoning) return genericOpenCodeMapping(OpenCodeRouting.protocolFor(apiEndpoint, modelName))
            val protocol = OpenCodeRouting.protocolFor(apiEndpoint, modelName)
            val effort = capability.options.filterIsInstance<OpenCodeReasoningOption.Effort>().firstOrNull()
            if (effort != null) {
                val values = effort.values.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
                    .filterNot { it.equals("none", true) }
                if (values.isNotEmpty()) return textMapping(openCodeParameterLabel(protocol, false), values, capability.reasoningRequired)
            }
            val budget = capability.options.filterIsInstance<OpenCodeReasoningOption.BudgetTokens>().firstOrNull()
            if (budget != null) {
                val values = OpenCodeReasoningMapper.budgetVariants(budget, capability.outputLimit)
                if (values.isNotEmpty() && (protocol.isGeminiProtocol() || protocol.isAnthropicProtocol())) {
                    return numberMapping(openCodeParameterLabel(protocol, true), values, capability.reasoningRequired)
                }
            }
            if (capability.options.any { it is OpenCodeReasoningOption.Toggle } && protocol.isAnthropicProtocol() && modelName.contains("minimax", true)) {
                return ThinkingQualityMapping.toggleOnly("model reasoning", capability.reasoningRequired)
            }
            return genericOpenCodeMapping(protocol)
        }
        if (providerType == ApiProviderType.OPENROUTER.name || providerType == ApiProviderType.NOUS_PORTAL.name) {
            val capability = try {
                OpenRouterModelCatalog.resolve(SharedHttpClient.instance, apiEndpoint, modelName)
            } catch (error: Exception) {
                AppLogger.w("ThinkingQualityMapping", "OpenRouter model capability lookup failed", error)
                return numberMapping("reasoning.max_tokens", listOf(1_024, 8_192, 16_384, 32_768, 65_536))
            } ?: return numberMapping("reasoning.max_tokens", listOf(1_024, 8_192, 16_384, 32_768, 65_536))
            if (capability.supportedEfforts.isNotEmpty()) {
                val disabledValue = capability.supportedEfforts.firstOrNull { it.equals("none", true) }
                val efforts = capability.supportedEfforts.filterNot { it.equals("none", true) }
                if (efforts.isEmpty()) return numberMapping("reasoning.max_tokens", listOf(1_024, 8_192, 16_384, 32_768, 65_536))
                return textMapping("reasoning.effort", efforts, capability.mandatory, disabledValue = disabledValue)
            }
            if (capability.supportsMaxTokens) {
                val max = capability.maxTokens ?: 65_537
                val values = (listOf(1_024, 8_192, 16_384, 32_768, 65_536).filter { it < max } + listOf(max - 1).filter { max > 1 }).distinct()
                if (values.isNotEmpty()) return numberMapping("reasoning.max_tokens", values, capability.mandatory)
            }
            return if (capability.reasoning) ThinkingQualityMapping.toggleOnly("reasoning", capability.mandatory)
            else ThinkingQualityMapping.unsupported()
        }
        return resolve(providerType, modelName)
    }

    private fun openAiMapping(model: String, responses: Boolean): ThinkingQualityMapping {
        val parameterLabel = if (responses) "reasoning.effort" else "reasoning_effort"
        return when {
            model.contains("codex") -> textMapping(parameterLabel, listOf("low", "medium", "high", "xhigh"))
            model.startsWith("o1") || model.startsWith("o3") || model.startsWith("o4") ->
                textMapping(parameterLabel, listOf("low", "medium", "high"))
            model.startsWith("gpt-5.6") ->
                textMapping(parameterLabel, listOf("low", "medium", "high", "xhigh", "max"), disabledValue = "none")
            model.startsWith("gpt-5") ->
                textMapping(parameterLabel, listOf("low", "medium", "high", "xhigh"), disabledValue = "none")
            else -> textMapping(parameterLabel, listOf("low", "medium", "high"))
        }
    }

    private fun geminiMapping(model: String): ThinkingQualityMapping = when {
        model.startsWith("gemini-2.5") -> numberMapping("thinkingBudget", listOf(1_024, 4_096, 8_192, 16_384, 32_768))
        else -> textMapping("thinkingLevel", listOf("MINIMAL", "LOW", "MEDIUM", "HIGH"), reasoningRequired = true)
    }

    private fun nvidiaMapping(model: String): ThinkingQualityMapping = when {
        model.contains("gpt-oss") -> textMapping("reasoning_effort", listOf("low", "medium", "high"))
        model.contains("nemotron") -> textMapping(
            "reasoning_effort",
            listOf("low", "medium", "high"),
            disabledValue = "none",
        )
        else -> ThinkingQualityMapping.toggleOnly("chat_template_kwargs.enable_thinking")
    }

    private fun deepSeekMapping(model: String): ThinkingQualityMapping {
        // DeepSeek documents three actual effort values. Its medium/xhigh compatibility aliases
        // are normalized to high by the service, so they must not become fake duplicate slider stops.
        // Keep this family-based so new DeepSeek model aliases receive the same documented options.
        return textMapping("reasoning_effort", listOf("low", "high", "max"))
    }

    private fun siliconFlowMapping(model: String): ThinkingQualityMapping {
        // SiliconFlow capability docs are family-based; keep the service tier (for example pro/) separate from the vendor path.
        val path = model.split('/').filter(String::isNotEmpty)
        val modelId = path.lastOrNull().orEmpty()
        val family = when {
            path.firstOrNull() == "pro" -> path.getOrNull(1).orEmpty()
            path.size == 1 && (modelId.startsWith("qwen3-") || modelId.startsWith("qwen3.") || modelId == "qwen3") -> "qwen"
            path.size == 1 && modelId.startsWith("deepseek-v") -> "deepseek-ai"
            path.size == 1 && modelId.startsWith("glm-") -> "zai-org"
            path.size == 1 && modelId.startsWith("hunyuan-") -> "tencent"
            else -> path.firstOrNull().orEmpty()
        }
        return when {
            family == "zai-org" && modelId.startsWith("glm-") ||
                family == "tencent" && modelId.startsWith("hunyuan-") ->
                ThinkingQualityMapping.toggleOnly("enable_thinking")
            family == "deepseek-ai" && modelId.startsWith("deepseek-v4") ->
                textMapping("reasoning_effort", listOf("high", "max"), reasoningRequired = true)
            family == "deepseek-ai" || family == "qwen" ->
                numberMapping("thinking_budget", listOf(128, 4_096, 8_192, 16_384, 32_768))
            else -> numberMapping("thinking_budget", listOf(128, 4_096, 8_192, 16_384, 32_768))
        }
    }

    private fun anthropicMapping(model: String): ThinkingQualityMapping = when {
        model.startsWith("claude-3") -> numberMapping("thinking.budget_tokens", listOf(1_024, 4_096, 8_192, 16_384))
        else -> textMapping("output_config.effort", listOf("low", "medium", "high"))
    }

    private fun xaiMapping(model: String): ThinkingQualityMapping = when {
        model.startsWith("grok-") -> textMapping("reasoning_effort", listOf("low", "medium", "high", "xhigh"), true)
        else -> textMapping("reasoning_effort", listOf("low", "medium", "high"))
    }

    private fun textMapping(parameterLabel: String, values: List<String>, reasoningRequired: Boolean = false, ids: List<String> = values, disabledValue: String? = null): ThinkingQualityMapping = ThinkingQualityMapping(
        ThinkingQualityControl.LEVELS, parameterLabel,
        values.mapIndexed { index, value -> ThinkingQualityOption(ids[index], value, ThinkingQualityWireValue.Text(value)) }, reasoningRequired, disabledValue,
    )

    private fun numberMapping(parameterLabel: String, values: List<Int>, reasoningRequired: Boolean = false): ThinkingQualityMapping = ThinkingQualityMapping(
        ThinkingQualityControl.LEVELS, parameterLabel,
        values.map { value -> ThinkingQualityOption(value.toString(), value.toString(), ThinkingQualityWireValue.Number(value)) }, reasoningRequired,
    )

    private fun openCodeParameterLabel(protocol: ApiProviderType, budget: Boolean): String = when {
        budget && protocol.isGeminiProtocol() -> "thinkingBudget"
        budget && protocol.isAnthropicProtocol() -> "thinking.budget_tokens"
        protocol.isGeminiProtocol() -> "thinkingLevel"
        protocol.isAnthropicProtocol() -> "output_config.effort"
        protocol.isOpenAiResponsesProtocol() -> "reasoning.effort"
        else -> "reasoning_effort"
    }

    private fun genericOpenCodeMapping(protocol: ApiProviderType): ThinkingQualityMapping = when {
        protocol.isGeminiProtocol() -> textMapping("thinkingLevel", listOf("LOW", "MEDIUM", "HIGH"))
        protocol.isAnthropicProtocol() -> textMapping("output_config.effort", listOf("low", "medium", "high"))
        protocol.isOpenAiResponsesProtocol() -> textMapping("reasoning.effort", listOf("low", "medium", "high"))
        else -> textMapping("reasoning_effort", listOf("low", "medium", "high"))
    }

    private fun ApiProviderType.isOpenAiResponsesProtocol() = this == ApiProviderType.OPENAI_RESPONSES_GENERIC || this == ApiProviderType.OPENAI_RESPONSES
    private fun ApiProviderType.isAnthropicProtocol() = this == ApiProviderType.ANTHROPIC_GENERIC || this == ApiProviderType.ANTHROPIC
    private fun ApiProviderType.isGeminiProtocol() = this == ApiProviderType.GEMINI_GENERIC || this == ApiProviderType.GOOGLE
}

internal data class OpenRouterModelCapability(val reasoning: Boolean, val mandatory: Boolean, val supportedEfforts: List<String>, val supportsMaxTokens: Boolean, val maxTokens: Int?)

internal object OpenRouterModelCatalog {
    private const val CACHE_TTL_MS = 5 * 60 * 1000L
    private data class Snapshot(val fetchedAt: Long, val models: Map<String, OpenRouterModelCapability>)
    @Volatile private var snapshot: Snapshot? = null
    private val mutex = Mutex()

    suspend fun resolve(client: OkHttpClient, endpoint: String, modelName: String): OpenRouterModelCapability? {
        val current = snapshot
        if (current != null && System.currentTimeMillis() - current.fetchedAt < CACHE_TTL_MS) return current.models[modelName.lowercase(Locale.US)]
        return mutex.withLock {
            val cached = snapshot
            val fresh = if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS) cached else fetch(client, endpoint)
            snapshot = fresh
            fresh.models[modelName.lowercase(Locale.US)]
        }
    }

    private suspend fun fetch(client: OkHttpClient, endpoint: String): Snapshot = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(modelsUrl(endpoint)).get().header("Accept", "application/json").build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("OpenRouter model catalog HTTP " + response.code)
            response.body?.string() ?: throw IllegalStateException("OpenRouter model catalog response is empty")
        }
        val data = JSONObject(body).optJSONArray("data") ?: throw IllegalStateException("OpenRouter model catalog has no data")
        val result = mutableMapOf<String, OpenRouterModelCapability>()
        for (index in 0 until data.length()) {
            val model = data.optJSONObject(index) ?: continue
            val id = model.optString("id").trim().lowercase(Locale.US)
            if (id.isEmpty()) continue
            val reasoning = model.optJSONObject("reasoning")
            val supported = reasoning?.optJSONArray("supported_efforts")?.let { array ->
                buildList { for (i in 0 until array.length()) array.optString(i).trim().takeIf(String::isNotEmpty)?.let(::add) }
            }.orEmpty()
            val topProvider = model.optJSONObject("top_provider")
            result[id] = OpenRouterModelCapability(
                reasoning = reasoning != null || model.optJSONArray("supported_parameters")?.toString()?.contains("reasoning") == true,
                mandatory = reasoning?.optBoolean("mandatory", false) ?: false,
                supportedEfforts = supported,
                supportsMaxTokens = reasoning?.optBoolean("supports_max_tokens", false) ?: false,
                maxTokens = topProvider?.optInt("max_completion_tokens", 0)?.takeIf { it > 1 },
            )
        }
        Snapshot(System.currentTimeMillis(), result)
    }

    private fun modelsUrl(endpoint: String): String {
        val parsed = URL(endpoint)
        val path = parsed.path.substringBeforeLast("/chat/completions").trimEnd('/')
        return if (parsed.host.equals("openrouter.ai", true)) parsed.protocol + "://" + parsed.authority + "/api/v1/models"
        else parsed.protocol + "://" + parsed.authority + path + "/models"
    }
}
