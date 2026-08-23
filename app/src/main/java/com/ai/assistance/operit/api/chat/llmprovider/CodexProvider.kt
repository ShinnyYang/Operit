package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import com.ai.assistance.operit.BuildConfig
import com.ai.assistance.operit.data.api.CodexAuthManager
import com.ai.assistance.operit.data.api.CodexOAuthProtocol
import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.data.model.ModelOption
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject

private class CodexAccessTokenProvider(
    private val authManager: CodexAuthManager,
) : ApiKeyProvider {
    override suspend fun getApiKey(): String = authManager.getValidAccessToken()

    override suspend fun getCandidateKeyCount(): Int =
        if (authManager.authState.value == null) 0 else 1
}

class CodexProvider(
    private val authManager: CodexAuthManager,
    modelName: String,
    private val httpClient: OkHttpClient,
    customHeaders: Map<String, String> = emptyMap(),
    supportsVision: Boolean = false,
    supportsAudio: Boolean = false,
    supportsVideo: Boolean = false,
    enableToolCall: Boolean = false,
) : OpenAIResponsesProvider(
    responsesApiEndpoint = CodexOAuthProtocol.CODEX_RESPONSES_ENDPOINT,
    apiKeyProvider = CodexAccessTokenProvider(authManager),
    modelName = modelName,
    client = httpClient,
    customHeaders = customHeaders,
    responsesProviderType = ApiProviderType.OPENAI_CODEX,
    supportsVision = supportsVision,
    supportsAudio = supportsAudio,
    supportsVideo = supportsVideo,
    enableToolCall = enableToolCall,
) {
    override fun applyAuthenticationHeaders(builder: Request.Builder, currentApiKey: String) {
        super.applyAuthenticationHeaders(builder, currentApiKey)
        val accountId = authManager.currentAccountId()
            ?: throw IllegalStateException("Codex account ID is unavailable")
        builder.header("ChatGPT-Account-ID", accountId)
        builder.header("originator", "operit")
        builder.header("User-Agent", "Operit/${BuildConfig.VERSION_NAME}")
        builder.header("session-id", UUID.randomUUID().toString())
        authManager.currentResidency()?.let { residency ->
            builder.header("x-openai-internal-codex-residency", residency)
        }
    }

    override fun customizeFinalRequestObject(
        requestObject: JSONObject,
        messagesArray: JSONArray,
        toolsJson: String?,
    ) {
        super.customizeFinalRequestObject(requestObject, messagesArray, toolsJson)
        requestObject.put("store", false)
        requestObject.put("parallel_tool_calls", false)
        val include = requestObject.optJSONArray("include") ?: JSONArray().also {
            requestObject.put("include", it)
        }
        if (!containsString(include, "reasoning.encrypted_content")) {
            include.put("reasoning.encrypted_content")
        }
    }

    override suspend fun getModelsList(_context: Context): Result<List<ModelOption>> {
        return CodexModelListFetcher.getModelsList(authManager, httpClient)
    }

    private fun containsString(array: JSONArray, value: String): Boolean {
        for (index in 0 until array.length()) {
            if (array.optString(index) == value) return true
        }
        return false
    }
}

object CodexModelPolicy {
    private val explicitlyAllowed = setOf(
        "gpt-5.5",
        "gpt-5.3-codex-spark",
        "gpt-5.4",
        "gpt-5.4-mini",
    )
    private val explicitlyDisallowed = setOf("gpt-5.5-pro")
    private val versionPattern = Regex("^gpt-(\\d+\\.\\d+)")

    fun allows(modelId: String): Boolean {
        val normalized = modelId.trim().lowercase()
        if (normalized in explicitlyDisallowed) return false
        if (normalized in explicitlyAllowed) return true
        if (normalized == "gpt-5.6") return false
        val version = versionPattern.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: return false
        return version > 5.4
    }

    fun filter(models: List<ModelOption>): List<ModelOption> =
        models.filter { allows(it.id) }
}

object CodexModelListFetcher {
    suspend fun getModelsList(
        authManager: CodexAuthManager,
        httpClient: OkHttpClient = SharedHttpClient.instance,
    ): Result<List<ModelOption>> {
        return try {
            val accessToken = authManager.getValidAccessToken()
            val accountId = authManager.currentAccountId()
                ?: throw IllegalStateException("Codex account ID is unavailable")
            val url = CodexOAuthProtocol.CODEX_MODELS_ENDPOINT.toHttpUrl()
                .newBuilder()
                .addQueryParameter("client_version", BuildConfig.VERSION_NAME)
                .build()
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer $accessToken")
                .header("ChatGPT-Account-ID", accountId)
                .header("originator", "operit")
                .header("User-Agent", "Operit/${BuildConfig.VERSION_NAME}")
                .build()
            val responseBody = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Codex model request failed with HTTP ${response.code}")
                    }
                    body
                }
            }
            Result.success(CodexModelPolicy.filter(parseModels(responseBody)))
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    internal fun parseModels(responseBody: String): List<ModelOption> {
        val root = JSONObject(responseBody)
        val models = root.optJSONArray("models")
            ?: throw IllegalArgumentException("Codex model response has no models array")
        val result = mutableListOf<ModelOption>()
        for (index in 0 until models.length()) {
            val model = models.optJSONObject(index) ?: continue
            val id = model.optString("slug", "").ifBlank {
                model.optString("id", "")
            }.trim()
            if (id.isNotEmpty()) {
                val name = model.optString("display_name", "").ifBlank {
                    model.optString("name", "")
                }.trim().ifBlank { id }
                result += ModelOption(id = id, name = name)
            }
        }
        return result
    }
}
