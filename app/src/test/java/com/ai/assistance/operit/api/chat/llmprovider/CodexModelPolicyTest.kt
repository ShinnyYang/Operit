package com.ai.assistance.operit.api.chat.llmprovider

import com.ai.assistance.operit.data.model.ModelOption
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexModelPolicyTest {
    @Test
    fun explicitOpenCodeModelsAreAllowed() {
        assertTrue(CodexModelPolicy.allows("gpt-5.4"))
        assertTrue(CodexModelPolicy.allows("gpt-5.3-codex-spark"))
    }

    @Test
    fun fastGptVersionsWithSuffixAreAllowed() {
        assertTrue(CodexModelPolicy.allows("gpt-5.6-luna"))
        assertTrue(CodexModelPolicy.allows("gpt-5.7-codex"))
    }

    @Test
    fun disallowedAndBareVersionModelsAreRemoved() {
        assertFalse(CodexModelPolicy.allows("gpt-5.5-pro"))
        assertFalse(CodexModelPolicy.allows("gpt-5.6"))
        assertFalse(CodexModelPolicy.allows("claude-3"))
    }

    @Test
    fun filterKeepsResponseOrder() {
        val models = listOf(
            ModelOption("gpt-5.6-luna", "Luna"),
            ModelOption("gpt-5.5-pro", "Pro"),
            ModelOption("gpt-5.4", "5.4"),
        )

        assertTrue(
            CodexModelPolicy.filter(models).map { it.id } == listOf("gpt-5.6-luna", "gpt-5.4")
        )
    }
}
