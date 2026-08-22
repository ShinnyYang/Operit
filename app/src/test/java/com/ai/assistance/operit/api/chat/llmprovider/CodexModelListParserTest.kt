package com.ai.assistance.operit.api.chat.llmprovider

import org.junit.Assert.assertEquals
import org.junit.Test

class CodexModelListParserTest {
    @Test
    fun parsesSlugAndIdFields() {
        val models = CodexModelListFetcher.parseModels(
            """
            {
              "models": [
                {"slug": "gpt-5.6-luna", "display_name": "Luna"},
                {"id": "gpt-5.4", "display_name": "5.4"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals(listOf("gpt-5.6-luna", "gpt-5.4"), models.map { it.id })
        assertEquals(listOf("Luna", "5.4"), models.map { it.name })
    }
}
