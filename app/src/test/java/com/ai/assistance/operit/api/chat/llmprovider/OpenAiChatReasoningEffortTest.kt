package com.ai.assistance.operit.api.chat.llmprovider

import com.ai.assistance.operit.data.collects.ModelThinkingConfigDefaults
import com.ai.assistance.operit.data.model.ApiProviderType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * OpenAI's Chat Completions endpoint rejects reasoning_effort with a 400 on models that
 * do not reason, so the parameter must only reach the wire for models that accept it.
 */
class OpenAiChatReasoningEffortTest {
    private fun mapping(provider: ApiProviderType, modelName: String): ThinkingQualityMapping =
        ThinkingQualityMappingRegistry.resolve(
            providerTypeId = provider.name,
            modelName = modelName,
            thinkingConfigurations = ModelThinkingConfigDefaults.forProvider(provider.name)
        )

    /** Mirrors what OpenAIProvider.createRequestBody sends for a freshly seeded config. */
    private fun requestBody(
        provider: ApiProviderType,
        modelName: String,
        enableThinking: Boolean
    ): JSONObject {
        val thinkingConfigurations = ModelThinkingConfigDefaults.forProvider(provider.name)
        val requestJson = JSONObject().put("model", modelName).put("stream", true)
        ThinkingConfigurationApplier.apply(
            requestJson = requestJson,
            providerTypeId = provider.name,
            modelName = modelName,
            apiEndpoint = "https://api.openai.com/v1/chat/completions",
            thinkingConfigurations = thinkingConfigurations,
            enableThinking = enableThinking,
            // ModelConfigManager seeds a config with the first option its mapping offers.
            optionId = mapping(provider, modelName).options.firstOrNull()?.id.orEmpty(),
        )
        return requestJson
    }

    @Test
    fun openAiChatModelsNeverReceiveReasoningEffort() {
        for (model in listOf("gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-3.5-turbo", "chatgpt-4o-latest")) {
            assertEquals(model, ThinkingQualityControl.UNSUPPORTED, mapping(ApiProviderType.OPENAI, model).control)
            assertFalse(model, requestBody(ApiProviderType.OPENAI, model, enableThinking = true).has("reasoning_effort"))
            assertFalse(model, requestBody(ApiProviderType.OPENAI, model, enableThinking = false).has("reasoning_effort"))
        }
    }

    @Test
    fun openAiReasoningModelsKeepReasoningEffortWhenThinkingIsOn() {
        for (model in listOf("o3", "o4-mini", "gpt-5-mini", "gpt-5.6-luna", "gpt-oss-120b", "codex-mini-latest")) {
            assertEquals(model, ThinkingQualityControl.LEVELS, mapping(ApiProviderType.OPENAI, model).control)
            assertEquals(model, "low", requestBody(ApiProviderType.OPENAI, model, enableThinking = true).getString("reasoning_effort"))
        }
    }

    @Test
    fun openAiReasoningModelsOmitReasoningEffortWhenThinkingIsOff() {
        assertFalse(requestBody(ApiProviderType.OPENAI, "o3", enableThinking = false).has("reasoning_effort"))
    }

    @Test
    fun compatibleEndpointsDropReasoningEffortForOpenAiChatModels() {
        assertEquals(ThinkingQualityControl.UNSUPPORTED, mapping(ApiProviderType.OPENAI_GENERIC, "gpt-4o").control)
        assertFalse(requestBody(ApiProviderType.OPENAI_GENERIC, "gpt-4o", enableThinking = true).has("reasoning_effort"))
    }

    @Test
    fun compatibleEndpointsKeepReasoningEffortForThirdPartyModels() {
        for (model in listOf("deepseek-reasoner", "glm-4.6", "qwen3-235b-a22b")) {
            assertEquals(model, ThinkingQualityControl.LEVELS, mapping(ApiProviderType.OPENAI_GENERIC, model).control)
            assertEquals(model, "low", requestBody(ApiProviderType.OPENAI_GENERIC, model, enableThinking = true).getString("reasoning_effort"))
        }
    }
}
