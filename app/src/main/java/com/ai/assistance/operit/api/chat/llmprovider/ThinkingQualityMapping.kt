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
    val id: String,
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

    fun optionFor(id: String): ThinkingQualityOption? =
        options.firstOrNull { it.id == id }

    fun selectedOptionId(id: String): String =
        optionFor(id)?.id ?: options.first().id

    fun textValueFor(id: String): String? =
        options.firstOrNull { it.id == id }?.wireValue?.let { value ->
            (value as? ThinkingQualityWireValue.Text)?.value
        } ?: options.firstOrNull()?.wireValue?.let { value ->
            (value as? ThinkingQualityWireValue.Text)?.value
        }

    fun numberValueFor(id: String): Int? =
        options.firstOrNull { it.id == id }?.wireValue?.let { value ->
            (value as? ThinkingQualityWireValue.Number)?.value
        } ?: options.firstOrNull()?.wireValue?.let { value ->
            (value as? ThinkingQualityWireValue.Number)?.value
        }
}

internal object ThinkingQualityMappingRegistry {
    fun resolve(providerTypeId: String, modelName: String): ThinkingQualityMapping {
        val providerType = providerTypeId.trim().uppercase(Locale.US)
        val normalizedModelName = modelName.trim().lowercase(Locale.US)

        return when (providerType) {
            ApiProviderType.XAI.name ->
                xaiMapping(normalizedModelName)

            ApiProviderType.OPENAI.name,
            ApiProviderType.OPENAI_GENERIC.name ->
                textMapping("reasoning_effort", listOf("low", "medium", "high"))

            ApiProviderType.OPENAI_RESPONSES.name,
            ApiProviderType.OPENAI_RESPONSES_GENERIC.name,
            ApiProviderType.OPENAI_CODEX.name ->
                textMapping("reasoning.effort", listOf("minimal", "low", "medium", "high"))

            ApiProviderType.GOOGLE.name,
            ApiProviderType.GEMINI_GENERIC.name ->
                geminiMapping(normalizedModelName)

            ApiProviderType.NVIDIA.name ->
                if (normalizedModelName.contains("gpt-oss")) {
                    textMapping("reasoning_effort", listOf("low", "medium", "high"))
                } else {
                    ThinkingQualityMapping.toggleOnly("enable_thinking")
                }

            ApiProviderType.DEEPSEEK.name ->
                textMapping("reasoning_effort", listOf("low", "high", "max"))

            ApiProviderType.SILICONFLOW.name ->
                numberMapping(
                    parameterLabel = "thinking_budget",
                    values = listOf(4_096, 8_192, 16_384, 32_768),
                )

            ApiProviderType.OPENROUTER.name ->
                numberMapping(
                    parameterLabel = "reasoning.max_tokens",
                    values = listOf(1_024, 16_000, 32_000, 64_000),
                )

            ApiProviderType.ANTHROPIC.name,
            ApiProviderType.ANTHROPIC_GENERIC.name ->
                if (
                    normalizedModelName.contains("4-6") ||
                    normalizedModelName.contains("4.7") ||
                    normalizedModelName.contains("4-7") ||
                    normalizedModelName.contains("4.8") ||
                    normalizedModelName.contains("4-8") ||
                    normalizedModelName.contains("5-")
                ) {
                    textMapping("output_config.effort", listOf("low", "medium", "high"))
                } else {
                    numberMapping("thinking.budget_tokens", listOf(1_024, 4_096, 8_192, 16_384))
                }

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
            val values = effort.values.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
                .filterNot { it.equals("none", ignoreCase = true) }
            if (values.isNotEmpty()) {
                return textMapping(
                    parameterLabel = openCodeParameterLabel(protocol, budget = false),
                    values = values,
                )
            }
        }

        val budget = capability.options.filterIsInstance<OpenCodeReasoningOption.BudgetTokens>().firstOrNull()
        if (budget != null) {
            val values = OpenCodeReasoningMapper.budgetVariants(budget, capability.outputLimit)
            if (values.isNotEmpty()) {
                return numberMapping(
                    parameterLabel = openCodeParameterLabel(protocol, budget = true),
                    values = values,
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
                id = value,
                displayLabel = value,
                wireValue = ThinkingQualityWireValue.Text(value),
            )
        },
    )

    private fun numberMapping(
        parameterLabel: String,
        values: List<Int>,
    ): ThinkingQualityMapping = ThinkingQualityMapping(
        control = ThinkingQualityControl.LEVELS,
        parameterLabel = parameterLabel,
        options = values.mapIndexed { index, value ->
            val wireValue = ThinkingQualityWireValue.Number(value)
            ThinkingQualityOption(
                id = value.toString(),
                displayLabel = when (wireValue) {
                    is ThinkingQualityWireValue.Number -> wireValue.value.toString()
                    is ThinkingQualityWireValue.Text -> wireValue.value
                    ThinkingQualityWireValue.Omitted -> ""
                },
                wireValue = wireValue,
            )
        },
    )

    private fun geminiMapping(modelName: String): ThinkingQualityMapping =
        when {
            modelName.startsWith("gemini-2.5-pro") ->
                numberMapping("thinkingBudget", listOf(1_024, 8_192, 16_384, 32_768))
            modelName.startsWith("gemini-2.5-flash") ->
                numberMapping("thinkingBudget", listOf(1_024, 4_096, 8_192, 16_384, 24_576))
            modelName.startsWith("gemini-3-flash-lite") ->
                textMapping("thinkingLevel", listOf("MINIMAL", "LOW", "MEDIUM"))
            modelName.startsWith("gemini-3-pro") ->
                textMapping("thinkingLevel", listOf("LOW", "HIGH"))
            else -> textMapping("thinkingLevel", listOf("MINIMAL", "LOW", "MEDIUM", "HIGH"))
        }

    private fun xaiMapping(modelName: String): ThinkingQualityMapping =
        when {
            modelName.startsWith("grok-4.5") ->
                textMapping("reasoning_effort", listOf("low", "medium", "high"))
            modelName.startsWith("grok-4.6") ->
                textMapping("reasoning_effort", listOf("low", "medium", "high", "xhigh"))
            else -> ThinkingQualityMapping.toggleOnly("enable_thinking")
        }
}
