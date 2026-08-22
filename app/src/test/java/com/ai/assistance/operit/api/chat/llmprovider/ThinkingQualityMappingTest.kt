package com.ai.assistance.operit.api.chat.llmprovider

import com.ai.assistance.operit.data.model.ApiProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinkingQualityMappingTest {
    @Test
    fun openAiUsesFiveNamedEffortValues() {
        val mapping = ThinkingQualityMappingRegistry.resolve(ApiProviderType.OPENAI.name, "gpt-5.6-luna")

        assertEquals(listOf("low", "medium", "high", "xhigh", "max"), mapping.options.map { it.displayLabel })
        assertEquals("high", mapping.textValueFor(3))
    }

    @Test
    fun providersKeepRepeatedWireValuesAtTheirOriginalPositions() {
        val gemini = ThinkingQualityMappingRegistry.resolve(ApiProviderType.GOOGLE.name, "gemini-3")
        val deepseek = ThinkingQualityMappingRegistry.resolve(ApiProviderType.DEEPSEEK.name, "deepseek-reasoner")

        assertEquals(listOf("MINIMAL", "LOW", "MEDIUM", "HIGH", "HIGH"), gemini.options.map { it.displayLabel })
        assertEquals(listOf("low", "high", "max", "max", "max"), deepseek.options.map { it.displayLabel })
    }

    @Test
    fun providerSpecificModelMatchingControlsLevelSupport() {
        val gptOss = ThinkingQualityMappingRegistry.resolve(ApiProviderType.NVIDIA.name, "gpt-oss-120b")
        val otherNvidiaModel = ThinkingQualityMappingRegistry.resolve(ApiProviderType.NVIDIA.name, "nemotron")

        assertTrue(gptOss.options.isNotEmpty())
        assertTrue(otherNvidiaModel.options.isEmpty())
    }

    @Test
    fun numericProviderValuesRemainTypedAndExposeAutoForOmittedValue() {
        val mapping = ThinkingQualityMappingRegistry.resolve(ApiProviderType.SILICONFLOW.name, "Qwen3")

        assertEquals("auto", mapping.options.first().displayLabel)
        assertEquals(8_192, mapping.numberValueFor(3))
    }
}
