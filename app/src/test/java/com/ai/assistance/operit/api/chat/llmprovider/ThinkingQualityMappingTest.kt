package com.ai.assistance.operit.api.chat.llmprovider

import com.ai.assistance.operit.data.model.ApiProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinkingQualityMappingTest {
    @Test
    fun xaiMapsSupportedModelsToReasoningEfforts() {
        val mapping = ThinkingQualityMappingRegistry.resolve(ApiProviderType.XAI.name, "grok-4.6")

        assertEquals(ThinkingQualityControl.LEVELS, mapping.control)
        assertEquals("reasoning_effort", mapping.parameterLabel)
        assertEquals(listOf("low", "medium", "high", "xhigh"), mapping.options.map { it.displayLabel })
        assertEquals("high", mapping.textValueFor("high"))
    }

    @Test
    fun xaiKeepsGenericControlsForNewGrokModels() {
        val mapping = ThinkingQualityMappingRegistry.resolve(ApiProviderType.XAI.name, "grok-3-mini")

        assertEquals(ThinkingQualityControl.LEVELS, mapping.control)
        assertTrue(mapping.options.isNotEmpty())
    }

    @Test
    fun openAiUsesFiveNamedEffortValues() {
        val mapping = ThinkingQualityMappingRegistry.resolve(ApiProviderType.OPENAI.name, "gpt-5.6-luna")

        assertEquals(listOf("low", "medium", "high", "xhigh", "max"), mapping.options.map { it.displayLabel })
        assertEquals("high", mapping.textValueFor("high"))
    }

    @Test
    fun providersUseModelSpecificWireValues() {
        val gemini = ThinkingQualityMappingRegistry.resolve(ApiProviderType.GOOGLE.name, "gemini-3-flash")
        val deepseek = ThinkingQualityMappingRegistry.resolve(ApiProviderType.DEEPSEEK.name, "deepseek-reasoner")

        assertEquals(listOf("MINIMAL", "LOW", "MEDIUM", "HIGH"), gemini.options.map { it.displayLabel })
        assertEquals(listOf("low", "high", "max"), deepseek.options.map { it.displayLabel })
        assertEquals(listOf("low", "high", "max"), deepseek.options.map { deepseek.textValueFor(it.id) })
    }

    @Test
    fun deepSeekChatRemainsInTheDeepSeekFamilyMapping() {
        val mapping = ThinkingQualityMappingRegistry.resolve(ApiProviderType.DEEPSEEK.name, "deepseek-chat")

        assertEquals(ThinkingQualityControl.LEVELS, mapping.control)
        assertEquals(3, mapping.options.size)
        assertEquals(listOf("low", "high", "max"), mapping.options.map { it.id })
    }

    @Test
    fun providerSpecificModelMatchingControlsLevelSupport() {
        val gptOss = ThinkingQualityMappingRegistry.resolve(ApiProviderType.NVIDIA.name, "gpt-oss-120b")
        val otherNvidiaModel = ThinkingQualityMappingRegistry.resolve(ApiProviderType.NVIDIA.name, "nemotron-future")

        assertTrue(gptOss.options.isNotEmpty())
        assertTrue(otherNvidiaModel.options.isNotEmpty())
    }

    @Test
    fun numericProviderValuesRemainTyped() {
        val mapping = ThinkingQualityMappingRegistry.resolve(ApiProviderType.SILICONFLOW.name, "Qwen3")

        assertEquals("128", mapping.options.first().displayLabel)
        assertEquals(8_192, mapping.numberValueFor("8192"))
    }
}
