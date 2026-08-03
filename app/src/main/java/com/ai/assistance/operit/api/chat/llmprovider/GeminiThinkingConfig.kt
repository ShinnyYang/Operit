package com.ai.assistance.operit.api.chat.llmprovider

import com.ai.assistance.operit.data.preferences.ApiPreferences
import org.json.JSONObject

/** Builds Gemini thinkingConfig exclusively from the application's global thinking setting. */
internal object GeminiThinkingConfig {
    private const val INCLUDE_THOUGHTS = "includeThoughts"
    private const val THINKING_LEVEL = "thinkingLevel"
    private val globalQualityLevels = listOf("MINIMAL", "LOW", "MEDIUM", "HIGH", "HIGH")
    private val reservedParameterNames =
        setOf(
            "thinking_level",
            THINKING_LEVEL,
            "thinking_budget",
            "thinkingBudget",
            "include_thoughts",
            INCLUDE_THOUGHTS,
            "thinking_config",
            "thinkingConfig"
        )

    fun fromGlobalQuality(qualityLevel: Int): JSONObject {
        val qualityIndex = qualityLevel - ApiPreferences.MIN_THINKING_QUALITY_LEVEL
        return JSONObject()
            .put(INCLUDE_THOUGHTS, true)
            .put(THINKING_LEVEL, globalQualityLevels[qualityIndex])
    }

    fun isReservedParameter(apiName: String): Boolean = apiName in reservedParameterNames
}
