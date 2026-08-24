package com.ai.assistance.operit.api.chat.llmprovider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenCodeReasoningMapperTest {
    @Test
    fun declaredEffortIdsAreSelectedOneToOne() {
        val capability = OpenCodeReasoningCapability(
            reasoning = true,
            options = listOf(OpenCodeReasoningOption.Effort(listOf(null, "low", "medium", "high"))),
            outputLimit = 64_000,
        )
        assertEquals(OpenCodeReasoningVariant.Effort("high"), OpenCodeReasoningMapper.select(capability, true, "high"))
    }

    @Test
    fun unknownEffortIdUsesTheFirstDeclaredEffort() {
        val capability = OpenCodeReasoningCapability(
            reasoning = true,
            options = listOf(OpenCodeReasoningOption.Effort(listOf("low", "high"))),
            outputLimit = 64_000,
        )
        assertEquals(OpenCodeReasoningVariant.Effort("low"), OpenCodeReasoningMapper.select(capability, true, "missing"))
    }

    @Test
    fun effortWithoutNoneLeavesThinkingOffAsAnUnsetVariant() {
        val capability = OpenCodeReasoningCapability(
            reasoning = true,
            options = listOf(OpenCodeReasoningOption.Effort(listOf("low", "high"))),
            outputLimit = 64_000,
        )
        assertNull(OpenCodeReasoningMapper.select(capability, false, "high"))
    }

    @Test
    fun toggleHasAnExplicitVariant() {
        val capability = OpenCodeReasoningCapability(
            reasoning = true,
            options = listOf(OpenCodeReasoningOption.Toggle),
            outputLimit = 64_000,
        )
        assertEquals(OpenCodeReasoningVariant.Toggle(true), OpenCodeReasoningMapper.select(capability, true, ""))
        assertEquals(OpenCodeReasoningVariant.Toggle(false), OpenCodeReasoningMapper.select(capability, false, ""))
    }

    @Test
    fun budgetOptionsUseTheirWireValueIds() {
        val capability = OpenCodeReasoningCapability(
            reasoning = true,
            options = listOf(
                OpenCodeReasoningOption.Toggle,
                OpenCodeReasoningOption.BudgetTokens(min = null, max = 81_920),
            ),
            outputLimit = 65_536,
        )
        assertEquals(OpenCodeReasoningVariant.BudgetTokens(32_768), OpenCodeReasoningMapper.select(capability, true, "32768"))
        assertEquals(OpenCodeReasoningVariant.BudgetTokens(65_535), OpenCodeReasoningMapper.select(capability, true, "65535"))
    }
}
