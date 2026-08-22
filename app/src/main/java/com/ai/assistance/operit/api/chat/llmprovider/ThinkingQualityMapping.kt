package com.ai.assistance.operit.api.chat.llmprovider

import com.ai.assistance.operit.data.model.ApiProviderType
import java.util.Locale

internal enum class ThinkingQualityControl {
    LEVELS,
    TOGGLE_ONLY,
}

internal sealed interface ThinkingQualityWireValue {
    data class Text(val value: String) : ThinkingQualityWireValue

    data class Number(val value: Int) : ThinkingQualityWireValue

    data object Omitted : ThinkingQualityWireValue
}

internal data class ThinkingQualityOption(
    val level: Int,
    val displayLabel: String,
    val wireValue: ThinkingQualityWireValue,
)

internal data class ThinkingQualityMapping(
    val control: ThinkingQualityControl,
    val parameterLabel: String,
    val options: List<ThinkingQualityOption>,
) {
    companion object {
        fun toggleOnly(parameterLabel: String): ThinkingQualityMapping = ThinkingQualityMapping(
            control = ThinkingQualityControl.TOGGLE_ONLY,
            parameterLabel = parameterLabel,
            options = emptyList(),
        )
    }

    fun optionFor(level: Int): ThinkingQualityOption? =
        options.firstOrNull { it.level == level }

    fun textValueFor(level: Int): String? =
        optionFor(level)?.wireValue?.let { value ->
            (value as? ThinkingQualityWireValue.Text)?.value
        }

    fun numberValueFor(level: Int): Int? =
        optionFor(level)?.wireValue?.let { value ->
            (value as? ThinkingQualityWireValue.Number)?.value
        }
}

internal object ThinkingQualityMappingRegistry {
    private val openAiEfforts = listOf("low", "medium", "high", "xhigh", "max")
    private val geminiLevels = listOf("MINIMAL", "LOW", "MEDIUM", "HIGH", "HIGH")
    private val nvidiaEfforts = listOf("low", "medium", "high", "max", "max")
    private val deepseekEfforts = listOf("low", "high", "max", "max", "max")

    fun resolve(providerTypeId: String, modelName: String): ThinkingQualityMapping {
        val providerType = providerTypeId.trim().uppercase(Locale.US)
        val normalizedModelName = modelName.trim().lowercase(Locale.US)

        return when (providerType) {
            ApiProviderType.OPENAI.name,
            ApiProviderType.OPENAI_GENERIC.name,
            ApiProviderType.OPENAI_RESPONSES.name,
            ApiProviderType.OPENAI_RESPONSES_GENERIC.name ->
                textMapping("reasoning_effort", openAiEfforts)

            ApiProviderType.GOOGLE.name,
            ApiProviderType.GEMINI_GENERIC.name ->
                textMapping("thinkingLevel", geminiLevels)

            ApiProviderType.NVIDIA.name ->
                if (normalizedModelName.contains("gpt-oss")) {
                    textMapping("reasoning_effort", nvidiaEfforts)
                } else {
                    ThinkingQualityMapping.toggleOnly("enable_thinking")
                }

            ApiProviderType.DEEPSEEK.name ->
                textMapping("reasoning_effort", deepseekEfforts)

            ApiProviderType.SILICONFLOW.name ->
                numberMapping(
                    parameterLabel = "thinking_budget",
                    values = listOf(null, 4_096, 8_192, 16_384, 32_768),
                )

            ApiProviderType.OPENROUTER.name ->
                numberMapping(
                    parameterLabel = "reasoning.max_tokens",
                    values = listOf(null, 1_024, 16_000, 32_000, 64_000),
                )

            ApiProviderType.ANTHROPIC.name,
            ApiProviderType.ANTHROPIC_GENERIC.name ->
                ThinkingQualityMapping.toggleOnly("thinking")

            ApiProviderType.MNN.name,
            ApiProviderType.LLAMA_CPP.name ->
                ThinkingQualityMapping.toggleOnly("enable_thinking")

            else -> ThinkingQualityMapping.toggleOnly("enable_thinking")
        }
    }

    suspend fun resolveForModel(
        providerTypeId: String,
        modelName: String,
        apiEndpoint: String,
    ): ThinkingQualityMapping {
        if (providerTypeId.trim().uppercase(Locale.US) != ApiProviderType.OPENCODE.name) {
            return resolve(providerTypeId, modelName)
        }

        val capability = OpenCodeModelCatalog.resolve(
            client = SharedHttpClient.instance,
            baseEndpoint = apiEndpoint,
            modelName = modelName,
        ) ?: return ThinkingQualityMapping.toggleOnly("model reasoning")

        val protocol = OpenCodeRouting.protocolFor(apiEndpoint, modelName)
        val effort = capability.options.filterIsInstance<OpenCodeReasoningOption.Effort>().firstOrNull()
        if (effort != null) {
            val values = (1..5).map { qualityLevel ->
                OpenCodeReasoningMapper.effortForQuality(effort.values, qualityLevel)
            }
            if (values.all { it != null }) {
                return textMapping(
                    parameterLabel = openCodeParameterLabel(protocol, budget = false),
                    values = values.map { requireNotNull(it) },
                )
            }
        }

        val budget = capability.options.filterIsInstance<OpenCodeReasoningOption.BudgetTokens>().firstOrNull()
        if (budget != null) {
            val values = (1..5).map { qualityLevel ->
                (OpenCodeReasoningMapper.select(capability, true, qualityLevel)
                    as? OpenCodeReasoningVariant.BudgetTokens)
                    ?.value
            }
            if (values.all { it != null }) {
                return numberMapping(
                    parameterLabel = openCodeParameterLabel(protocol, budget = true),
                    values = values.map { it },
                )
            }
        }

        return ThinkingQualityMapping.toggleOnly("model reasoning")
    }

    private fun openCodeParameterLabel(protocol: ApiProviderType, budget: Boolean): String =
        when {
            budget && protocol.isGeminiProtocol() -> "thinkingBudget"
            budget && protocol.isAnthropicProtocol() -> "budget_tokens"
            protocol.isGeminiProtocol() -> "thinkingLevel"
            protocol.isAnthropicProtocol() -> "output_config.effort"
            protocol.isOpenAiResponsesProtocol() -> "reasoning.effort"
            else -> "reasoning_effort"
        }

    private fun ApiProviderType.isOpenAiResponsesProtocol(): Boolean =
        this == ApiProviderType.OPENAI_RESPONSES_GENERIC || this == ApiProviderType.OPENAI_RESPONSES

    private fun ApiProviderType.isAnthropicProtocol(): Boolean =
        this == ApiProviderType.ANTHROPIC_GENERIC || this == ApiProviderType.ANTHROPIC

    private fun ApiProviderType.isGeminiProtocol(): Boolean =
        this == ApiProviderType.GEMINI_GENERIC || this == ApiProviderType.GOOGLE

    private fun textMapping(
        parameterLabel: String,
        values: List<String>,
    ): ThinkingQualityMapping = ThinkingQualityMapping(
        control = ThinkingQualityControl.LEVELS,
        parameterLabel = parameterLabel,
        options = values.mapIndexed { index, value ->
            ThinkingQualityOption(
                level = index + 1,
                displayLabel = value,
                wireValue = ThinkingQualityWireValue.Text(value),
            )
        },
    )

    private fun numberMapping(
        parameterLabel: String,
        values: List<Int?>,
    ): ThinkingQualityMapping = ThinkingQualityMapping(
        control = ThinkingQualityControl.LEVELS,
        parameterLabel = parameterLabel,
        options = values.mapIndexed { index, value ->
            val wireValue = value?.let(ThinkingQualityWireValue::Number) ?: ThinkingQualityWireValue.Omitted
            ThinkingQualityOption(
                level = index + 1,
                displayLabel = when (wireValue) {
                    ThinkingQualityWireValue.Omitted -> "auto"
                    is ThinkingQualityWireValue.Number -> wireValue.value.toString()
                    is ThinkingQualityWireValue.Text -> wireValue.value
                },
                wireValue = wireValue,
            )
        },
    )
}
