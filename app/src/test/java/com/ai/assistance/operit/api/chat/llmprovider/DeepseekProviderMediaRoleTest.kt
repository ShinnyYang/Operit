package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import com.ai.assistance.operit.core.chat.hooks.PromptTurn
import com.ai.assistance.operit.core.chat.hooks.PromptTurnKind
import com.ai.assistance.operit.data.model.ModelParameter
import com.ai.assistance.operit.data.model.ToolPrompt
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.ImagePoolManager
import com.ai.assistance.operit.util.OperitPaths
import java.io.File
import java.nio.file.Files
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.Buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class DeepseekProviderMediaRoleTest {

    private lateinit var imagePoolRoot: File
    private var previousSystemLogEnabled = true
    private var previousFileLogEnabled = true

    @Before
    fun setUpImagePool() {
        previousSystemLogEnabled = AppLogger.enableSystemLog
        previousFileLogEnabled = AppLogger.enableFileLogging
        AppLogger.enableSystemLog = false
        AppLogger.enableFileLogging = false

        imagePoolRoot = Files.createTempDirectory("deepseek-media-role-test").toFile()
        ImagePoolManager.initialize(imagePoolRoot, preloadNow = false)
        val poolDirectory = OperitPaths.imagePoolDir(imagePoolRoot)
        File(poolDirectory, "$IMAGE_ID.dat").writeText(IMAGE_BASE64)
        File(poolDirectory, "$IMAGE_ID.meta").writeText(
            """{"mimeType":"image/png","width":1,"height":1}"""
        )
    }

    @After
    fun tearDownImagePool() {
        ImagePoolManager.clear()
        imagePoolRoot.deleteRecursively()
        AppLogger.enableSystemLog = previousSystemLogEnabled
        AppLogger.enableFileLogging = previousFileLogEnabled
    }

    @Test
    fun `DeepSeek injects user images but strips assistant and tool image links`() {
        val history =
            listOf(
                PromptTurn(
                    PromptTurnKind.USER,
                    "user text<link type=\"image\" id=\"$IMAGE_ID\">Image</link>"
                ),
                PromptTurn(
                    PromptTurnKind.ASSISTANT,
                    "assistant text<link id=\"$IMAGE_ID\" type=\"image\">Image</link>"
                ),
                PromptTurn(
                    PromptTurnKind.TOOL_RESULT,
                    "tool text<link type=\"image\" id=\"missing\">Image</link>"
                )
            )

        val request = buildRequestJson(history)
        val messages = request.getJSONArray("messages")

        val userContent = messages.getJSONObject(0).getJSONArray("content")
        assertEquals("image_url", userContent.getJSONObject(0).getString("type"))
        assertEquals(
            "data:image/png;base64,$IMAGE_BASE64",
            userContent.getJSONObject(0).getJSONObject("image_url").getString("url")
        )
        assertEquals("user text", userContent.getJSONObject(1).getString("text"))

        val assistantMessage = messages.getJSONObject(1)
        assertEquals("assistant", assistantMessage.getString("role"))
        assertEquals("assistant text", assistantMessage.getString("content"))

        val toolHistoryMessage = messages.getJSONObject(2)
        assertEquals("user", toolHistoryMessage.getString("role"))
        assertEquals("tool text", toolHistoryMessage.getString("content"))

        assertFalse(request.toString().contains("<link"))
        assertTrue(request.toString().contains("image_url"))
    }

    @Test
    fun `DeepSeek strips image links from structured tool results`() {
        val history =
            listOf(
                PromptTurn(
                    PromptTurnKind.TOOL_CALL,
                    """<tool name="read_file"><param name="path">/tmp/image.png</param></tool>"""
                ),
                PromptTurn(
                    PromptTurnKind.TOOL_RESULT,
                    """<tool_result name="read_file"><content>tool text<link type="image" id="missing">Image</link></content></tool_result>"""
                )
            )

        val request =
            buildRequestJson(
                history = history,
                enableToolCall = true,
                availableTools = listOf(ToolPrompt(name = "read_file", description = "Read a file"))
            )
        val messages = request.getJSONArray("messages")
        val toolMessage =
            (0 until messages.length())
                .map { messages.getJSONObject(it) }
                .single { it.getString("role") == "tool" }

        assertEquals("tool text", toolMessage.getString("content"))
        assertFalse(request.toString().contains("<link"))
        assertFalse(request.toString().contains("image_url"))
    }

    private fun buildRequestJson(
        history: List<PromptTurn>,
        enableToolCall: Boolean = false,
        availableTools: List<ToolPrompt>? = null
    ): JSONObject {
        val provider =
            DeepseekProvider(
                apiEndpoint = "https://example.test/v1/chat/completions",
                apiKeyProvider = SingleApiKeyProvider("test-key"),
                modelName = "deepseek-test",
                client = OkHttpClient(),
                supportsVision = true,
                enableToolCall = enableToolCall
            )
        val method =
            DeepseekProvider::class.java.declaredMethods.single {
                it.name == "createRequestBody" && it.parameterCount == 7
            }
        method.isAccessible = true
        val body =
            method.invoke(
                provider,
                mock<Context>(),
                history,
                emptyList<ModelParameter<*>>(),
                false,
                false,
                availableTools,
                false
            ) as RequestBody
        val buffer = Buffer()
        body.writeTo(buffer)
        return JSONObject(buffer.readUtf8())
    }

    private companion object {
        const val IMAGE_ID = "image-present"
        const val IMAGE_BASE64 = "aW1hZ2U="
    }
}
