package com.ai.assistance.operit.api.chat.llmprovider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiThinkingConfigTest {
    @Test
    fun mapsModelOptionToGeminiLevel() {
        val config = GeminiThinkingConfig.fromOption("gemini-3-pro", "HIGH")
        assertEquals("HIGH", config.thinkingLevel)
    }

    @Test
    fun requestsThoughtSummariesForAnEnabledOption() {
        val config = GeminiThinkingConfig.fromOption("gemini-3-pro", "HIGH")
        assertTrue(config.includeThoughts)
    }
}
