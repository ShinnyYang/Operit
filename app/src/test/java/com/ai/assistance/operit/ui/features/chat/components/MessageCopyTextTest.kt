package com.ai.assistance.operit.ui.features.chat.components

import com.ai.assistance.operit.data.model.ChatMessage
import com.ai.assistance.operit.data.model.ChatMessageDisplayMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageCopyTextTest {

    @Test fun cleanMessageContentForCopy_removesMultipleOpenAiReasoningMetadata() {
        val content =
            """
            <meta provider="openai:responses_reasoning">first-payload</meta>
            <tool name="run"><param name="command">pwd</param></tool>
            <meta provider="openai:responses_reasoning">second-payload</meta>
            final answer
            """.trimIndent()

        assertEquals("final answer", cleanMessageContentForCopy(content))
    }

    @Test fun cleanMessageContentForCopy_removesGeminiThoughtSignature() {
        val content = "prefix<meta provider=\"gemini:thought_signature\">signature</meta>suffix"

        assertEquals("prefixsuffix", cleanMessageContentForCopy(content))
    }

    @Test fun cleanMessageContentForCopy_preservesOtherMetaAndMarkdown() {
        val content = "<meta provider=\"other\">value</meta>\n**answer**"

        assertEquals(content, cleanMessageContentForCopy(content))
    }

    @Test fun cleanMessageContentForCopy_preservesHtmlMetaBeforeInternalMetadata() {
        val content =
            "<meta charset=\"utf-8\">visible" +
                "<meta provider=\"openai:responses_reasoning\">payload</meta>answer"

        assertEquals("<meta charset=\"utf-8\">visibleanswer", cleanMessageContentForCopy(content))
    }

    @Test fun buildSelectedMessagesPlainText_usesChatIndexOrderAndPlainTextConversion() = runTest {
        val chatHistory =
            listOf(
                ChatMessage(sender = "user", content = "**first**", timestamp = 30L),
                ChatMessage(sender = "ai", content = "second", timestamp = 10L),
                ChatMessage(sender = "user", content = "third", timestamp = 20L),
            )

        val result =
            buildSelectedMessagesPlainText(chatHistory, setOf(2, 0, 1)) { markdown ->
                markdown.replace("**", "")
            }

        assertEquals("first\n\nsecond\n\nthird", result)
    }

    @Test fun buildSelectedMessagesPlainText_cleansMetadataAndKeepsOnlyUserAndAi() = runTest {
        val chatHistory =
            listOf(
                ChatMessage(
                    sender = "user",
                    content = "answer<meta provider=\"gemini:thought_signature\">signature</meta>",
                    timestamp = 1L,
                ),
                ChatMessage(sender = "system", content = "hidden", timestamp = 2L),
                ChatMessage(
                    sender = "user",
                    content = "hidden placeholder",
                    timestamp = 3L,
                    displayMode = ChatMessageDisplayMode.HIDDEN_PLACEHOLDER,
                ),
                ChatMessage(sender = "ai", content = "<think>empty</think>", timestamp = 4L),
                ChatMessage(sender = "ai", content = "reply", timestamp = 5L),
            )

        val result =
            buildSelectedMessagesPlainText(chatHistory, setOf(0, 1, 2, 3, 4, 99)) { content -> content }

        assertEquals("answer\n\nreply", result)
    }
}
