package com.ai.assistance.operit.api.chat.llmprovider

import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.data.model.ModelParameter
import com.ai.assistance.operit.data.model.ParameterCategory
import com.ai.assistance.operit.data.model.ParameterValueType
import com.ai.assistance.operit.util.AppLogger
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** The reasoning capabilities published by models.opencode.ai for one model. */
internal data class OpenCodeReasoningCapability(
    val reasoning: Boolean,
    val options: List<OpenCodeReasoningOption>,
    val outputLimit: Int
)

internal sealed class OpenCodeReasoningOption {
    data class Effort(val values: List<String?>) : OpenCodeReasoningOption()
    object Toggle : OpenCodeReasoningOption()

    data class BudgetTokens(val min: Int?, val max: Int?) : OpenCodeReasoningOption()
}

internal sealed class OpenCodeReasoningVariant {
    data class Effort(val value: String) : OpenCodeReasoningVariant()
    data class BudgetTokens(val value: Int) : OpenCodeReasoningVariant()
    data class Toggle(val enabled: Boolean) : OpenCodeReasoningVariant()
}

/**
 * Maps Operit's five global quality positions to the finite variants exposed by
 * OpenCode. The mapping deliberately happens after removing the optional `none`
 * value, so a model's declared capability remains the source of truth.
 */
internal object OpenCodeReasoningMapper {
    fun select(
        capability: OpenCodeReasoningCapability?,
        enableThinking: Boolean,
        qualityLevel: Int
    ): OpenCodeReasoningVariant? {
        if (capability == null || !capability.reasoning || capability.options.isEmpty()) {
            return null
        }

        // OpenCode gives effort options precedence over toggle and budget options.
        val effort = capability.options.filterIsInstance<OpenCodeReasoningOption.Effort>().firstOrNull()
        if (effort != null) {
            if (!enableThinking) {
                return if (effort.values.any { it == null || it.equals("none", ignoreCase = true) }) {
                    OpenCodeReasoningVariant.Effort("none")
                } else {
                    null
                }
            }
            val selectedEffort = effortForQuality(effort.values, qualityLevel) ?: return null
            return OpenCodeReasoningVariant.Effort(selectedEffort)
        }

        val toggle = capability.options.any { it is OpenCodeReasoningOption.Toggle }
        val budget = capability.options.filterIsInstance<OpenCodeReasoningOption.BudgetTokens>().firstOrNull()
        if (budget != null) {
            if (!enableThinking) {
                return if (toggle) OpenCodeReasoningVariant.Toggle(false) else null
            }
            val budgets = budgetVariants(budget, capability.outputLimit)
            if (budgets.isEmpty()) return null
            val selectedBudget = budgets[qualityIndex(budgets.size, qualityLevel)]
            return OpenCodeReasoningVariant.BudgetTokens(selectedBudget)
        }

        // A toggle has no intensity dimension, so every quality position selects
        // the same variant. Unsupported protocol-specific toggles are left empty
        // by toParameters rather than being replaced with an invented effort value.
        return if (toggle) OpenCodeReasoningVariant.Toggle(enableThinking) else null
    }

    internal fun effortForQuality(values: List<String?>, qualityLevel: Int): String? {
        val activeValues = values
            .mapNotNull { it?.trim()?.takeIf { value -> value.isNotEmpty() } }
            .filterNot { it.equals("none", ignoreCase = true) }
        if (activeValues.isEmpty()) return null
        return activeValues[qualityIndex(activeValues.size, qualityLevel)]
    }

    internal fun qualityIndex(optionCount: Int, qualityLevel: Int): Int {
        require(optionCount > 0) { "optionCount must be positive" }
        val quality = qualityLevel.coerceIn(1, 5)
        return when (optionCount) {
            1 -> 0
            2 -> if (quality <= 2) 0 else 1
            3 -> when {
                quality <= 2 -> 0
                quality <= 4 -> 1
                else -> 2
            }
            4 -> when (quality) {
                1 -> 0
                2, 3 -> 1
                4 -> 2
                else -> 3
            }
            else -> (((quality - 1) * (optionCount - 1)) + 2) / 4
        }.coerceIn(0, optionCount - 1)
    }

    internal fun budgetVariants(
        option: OpenCodeReasoningOption.BudgetTokens,
        outputLimit: Int
    ): List<Int> {
        // This mirrors OpenCode's high/max budget variant construction.
        val maximum = minOf(
            option.max ?: (outputLimit - 1),
            outputLimit - 1,
            1_048_575
        )
        if (maximum <= 0) return emptyList()
        val high = minOf(
            maxOf(option.min ?: 0, (maximum + 1) / 2),
            maximum
        )
        return listOf(high, maximum).distinct().filter { it > 0 }
    }
}

/** Internal request marker used to disable generic provider reasoning defaults. */
internal object OpenCodeReasoningParameters {
    const val INTERNAL_MARKER = "__operit_opencode_reasoning"

    fun isInternal(parameter: ModelParameter<*>): Boolean = parameter.apiName == INTERNAL_MARKER

    fun isMarked(parameters: List<ModelParameter<*>>): Boolean = parameters.any(::isInternal)

    fun forVariant(
        protocol: ApiProviderType,
        modelName: String,
        capability: OpenCodeReasoningCapability?,
        variant: OpenCodeReasoningVariant?
    ): List<ModelParameter<*>> {
        val result = mutableListOf<ModelParameter<*>>(marker())
        if (variant == null) return result

        when (variant) {
            is OpenCodeReasoningVariant.Effort -> {
                when {
                    protocol.isOpenAiResponses() -> {
                        val reasoning = JSONObject().put("effort", variant.value)
                        if (!variant.value.equals("none", ignoreCase = true)) {
                            reasoning.put("summary", "auto")
                        }
                        result += objectParameter(
                            apiName = "reasoning",
                            value = reasoning
                        )
                        if (!variant.value.equals("none", ignoreCase = true)) {
                            result += objectParameter(
                                apiName = "include",
                                value = JSONArray().put("reasoning.encrypted_content")
                            )
                        }
                    }
                    protocol.isOpenAiChat() -> {
                        result += stringParameter("reasoning_effort", variant.value)
                    }
                    protocol.isAnthropic() -> {
                        anthropicEffortParameters(result, modelName, capability, variant.value)
                    }
                    protocol.isGemini() -> {
                        result += objectParameter(
                            apiName = "thinkingConfig",
                            value = JSONObject()
                                .put("includeThoughts", true)
                                .put("thinkingLevel", variant.value),
                            category = ParameterCategory.GENERATION
                        )
                    }
                }
            }
            is OpenCodeReasoningVariant.BudgetTokens -> {
                when {
                    protocol.isAnthropic() -> {
                        result += objectParameter(
                            apiName = "thinking",
                            value = JSONObject()
                                .put("type", "enabled")
                                .put("budget_tokens", variant.value)
                        )
                    }
                    protocol.isGemini() -> {
                        result += objectParameter(
                            apiName = "thinkingConfig",
                            value = JSONObject()
                                .put("includeThoughts", true)
                                .put("thinkingBudget", variant.value),
                            category = ParameterCategory.GENERATION
                        )
                    }
                }
            }
            is OpenCodeReasoningVariant.Toggle -> {
                // OpenCode's native fallback currently defines a wire-level toggle
                // for MiniMax's Anthropic-compatible route. Other SDKs have no
                // generic toggle lowerer, so they remain at the provider default.
                if (protocol.isAnthropic() && modelName.contains("minimax", ignoreCase = true)) {
                    result += objectParameter(
                        apiName = "thinking",
                        value = JSONObject().put(
                            "type",
                            if (variant.enabled) "adaptive" else "disabled"
                        )
                    )
                }
            }
        }
        return result
    }

    private fun anthropicEffortParameters(
        result: MutableList<ModelParameter<*>>,
        modelName: String,
        capability: OpenCodeReasoningCapability?,
        effort: String
    ) {
        val thinking = anthropicThinkingForEffort(modelName, capability?.outputLimit ?: 0)
        if (thinking != null) {
            result += objectParameter("thinking", thinking)
        }
        result += objectParameter(
            apiName = "output_config",
            value = JSONObject().put("effort", effort)
        )
    }

    private fun anthropicThinkingForEffort(modelName: String, outputLimit: Int): JSONObject? {
        val id = modelName.lowercase()
        if (id.contains("opus-4-5") || id.contains("opus-4.5")) {
            val budget = minOf(16_000, (outputLimit / 2 - 1).coerceAtLeast(0))
            return if (budget > 0) {
                JSONObject().put("type", "enabled").put("budget_tokens", budget)
            } else {
                null
            }
        }

        if (id.contains("kimi") || id.contains("k2p")) {
            return JSONObject().put("type", "adaptive").put("display", "summarized")
        }

        val match = CLAUDE_VERSION_REGEX.find(id) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: 0
        return when {
            major > 4 || (major == 4 && minor >= 7) ->
                JSONObject().put("type", "adaptive").put("display", "summarized")
            major == 4 && minor == 6 -> JSONObject().put("type", "adaptive")
            else -> null
        }
    }

    private fun marker(): ModelParameter<Boolean> = ModelParameter(
        id = INTERNAL_MARKER,
        name = INTERNAL_MARKER,
        apiName = INTERNAL_MARKER,
        defaultValue = true,
        currentValue = true,
        isEnabled = true,
        valueType = ParameterValueType.BOOLEAN,
        category = ParameterCategory.OTHER,
        isCustom = false
    )

    private fun stringParameter(apiName: String, value: String): ModelParameter<String> = ModelParameter(
        id = "opencode-$apiName",
        name = apiName,
        apiName = apiName,
        defaultValue = value,
        currentValue = value,
        isEnabled = true,
        valueType = ParameterValueType.STRING,
        category = ParameterCategory.OTHER,
        isCustom = false
    )

    private fun objectParameter(
        apiName: String,
        value: Any,
        category: ParameterCategory = ParameterCategory.OTHER
    ): ModelParameter<String> {
        val serialized = value.toString()
        return ModelParameter(
            id = "opencode-$apiName",
            name = apiName,
            apiName = apiName,
            defaultValue = serialized,
            currentValue = serialized,
            isEnabled = true,
            valueType = ParameterValueType.OBJECT,
            category = category,
            isCustom = false
        )
    }

    private fun ApiProviderType.isOpenAiResponses(): Boolean =
        this == ApiProviderType.OPENAI_RESPONSES || this == ApiProviderType.OPENAI_RESPONSES_GENERIC

    private fun ApiProviderType.isOpenAiChat(): Boolean =
        this == ApiProviderType.OPENAI || this == ApiProviderType.OPENAI_GENERIC || this == ApiProviderType.OPENAI_LOCAL

    private fun ApiProviderType.isAnthropic(): Boolean =
        this == ApiProviderType.ANTHROPIC || this == ApiProviderType.ANTHROPIC_GENERIC

    private fun ApiProviderType.isGemini(): Boolean =
        this == ApiProviderType.GOOGLE || this == ApiProviderType.GEMINI_GENERIC

    private val CLAUDE_VERSION_REGEX =
        Regex("claude-(?:[a-z]+-)?(\\d+)(?:[.-](\\d{1,2}))?(?:[.@-]|$)")
}

/** Fetches and caches the same model capability catalog used by OpenCode. */
internal object OpenCodeModelCatalog {
    private const val CATALOG_URL = "https://models.opencode.ai/api.json"
    private const val CACHE_TTL_MS = 5 * 60 * 1000L
    private const val TAG = "OpenCodeModelCatalog"

    private data class Snapshot(
        val fetchedAt: Long,
        val providers: Map<String, Map<String, OpenCodeReasoningCapability>>
    )

    @Volatile private var snapshot: Snapshot? = null
    private val refreshMutex = Mutex()

    suspend fun resolve(
        client: OkHttpClient,
        baseEndpoint: String,
        modelName: String
    ): OpenCodeReasoningCapability? {
        val providerId = OpenCodeRouting.catalogProviderId(baseEndpoint)
        val now = System.currentTimeMillis()
        snapshot?.takeIf { now - it.fetchedAt < CACHE_TTL_MS }?.let {
            return it.providers[providerId]?.get(modelName)
        }

        return refreshMutex.withLock {
            val current = snapshot
            val refreshedNow = System.currentTimeMillis()
            if (current != null && refreshedNow - current.fetchedAt < CACHE_TTL_MS) {
                return@withLock current.providers[providerId]?.get(modelName)
            }

            val fresh = try {
                fetch(client)
            } catch (error: Exception) {
                AppLogger.w(TAG, "刷新OpenCode模型能力目录失败", error)
                null
            }
            if (fresh != null) {
                snapshot = fresh
                fresh.providers[providerId]?.get(modelName)
            } else {
                current?.providers?.get(providerId)?.get(modelName)
            }
        }
    }

    private suspend fun fetch(client: OkHttpClient): Snapshot = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(CATALOG_URL)
            .header("Accept", "application/json")
            .build()
        val catalogClient = client.newBuilder()
            .callTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val body = catalogClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("OpenCode model catalog HTTP ${response.code}")
            }
            response.body?.string() ?: throw IOException("OpenCode model catalog response is empty")
        }
        val root = JSONObject(body)
        val providers = listOf("opencode", "opencode-go").associateWith { providerId ->
            parseProvider(root.optJSONObject(providerId))
        }
        Snapshot(System.currentTimeMillis(), providers)
    }

    private fun parseProvider(provider: JSONObject?): Map<String, OpenCodeReasoningCapability> {
        if (provider == null) return emptyMap()
        val models = provider.optJSONObject("models") ?: return emptyMap()
        val result = mutableMapOf<String, OpenCodeReasoningCapability>()
        val keys = models.keys()
        while (keys.hasNext()) {
            val modelId = keys.next()
            val model = models.optJSONObject(modelId) ?: continue
            result[modelId] = OpenCodeReasoningCapability(
                reasoning = model.optBoolean("reasoning", false),
                options = parseOptions(model),
                outputLimit = model.optJSONObject("limit")?.optInt("output", 0) ?: 0
            )
        }
        return result
    }

    private fun parseOptions(model: JSONObject): List<OpenCodeReasoningOption> {
        if (!model.has("reasoning_options") || model.isNull("reasoning_options")) {
            return emptyList()
        }
        val array = model.optJSONArray("reasoning_options") ?: return emptyList()
        val result = mutableListOf<OpenCodeReasoningOption>()
        for (index in 0 until array.length()) {
            val option = array.optJSONObject(index) ?: continue
            when (option.optString("type")) {
                "effort" -> {
                    val values = option.optJSONArray("values") ?: JSONArray()
                    val parsed = buildList {
                        for (valueIndex in 0 until values.length()) {
                            add(if (values.isNull(valueIndex)) null else values.optString(valueIndex))
                        }
                    }
                    result += OpenCodeReasoningOption.Effort(parsed)
                }
                "toggle" -> result += OpenCodeReasoningOption.Toggle
                "budget_tokens" -> result += OpenCodeReasoningOption.BudgetTokens(
                    min = optionalInt(option, "min"),
                    max = optionalInt(option, "max")
                )
            }
        }
        return result
    }

    private fun optionalInt(objectValue: JSONObject, key: String): Int? =
        (objectValue.opt(key) as? Number)?.toInt()
}
