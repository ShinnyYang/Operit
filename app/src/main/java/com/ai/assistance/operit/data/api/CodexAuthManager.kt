package com.ai.assistance.operit.data.api

import android.content.Context
import com.ai.assistance.operit.data.preferences.CodexAuthPreferences
import com.ai.assistance.operit.data.preferences.CodexAuthState
import com.ai.assistance.operit.util.AppLogger
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

class CodexAuthManager private constructor(context: Context) {
    private val preferences = CodexAuthPreferences.getInstance(context)
    private val oauthClient = CodexOAuthClient(
        client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build(),
    )
    private val refreshMutex = Mutex()

    val authState: StateFlow<CodexAuthState?> = preferences.authState

    suspend fun saveLoginTokens(tokens: CodexOAuthTokenResponse): CodexAuthState {
        val completeTokens = tokens.requireComplete()
        val accessToken = requireNotNull(completeTokens.accessToken)
        val idToken = requireNotNull(completeTokens.idToken)
        val refreshToken = requireNotNull(completeTokens.refreshToken)
        val accessClaims = CodexOAuthProtocol.parseJwtClaims(accessToken)
            ?: throw IOException("Codex access token is not a valid JWT")
        val idClaims = CodexOAuthProtocol.parseJwtClaims(idToken)
        val accountId = idClaims?.accountId ?: accessClaims.accountId
            ?: throw IOException("Codex token has no ChatGPT account ID")
        val expiresAt = accessClaims.expiresAtMillis
            ?: completeTokens.expiresInSeconds?.let { System.currentTimeMillis() + it * 1000L }
            ?: throw IOException("Codex access token has no expiration")
        val state = CodexAuthState(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = expiresAt,
            accountId = accountId,
            residency = accessClaims.residency ?: idClaims?.residency,
            email = idClaims?.email ?: accessClaims.email,
        )
        preferences.save(state)
        return state
    }

    suspend fun getValidAccessToken(): String {
        val current = preferences.currentState()
            ?: throw IOException("Codex is not logged in")
        if (current.expiresAtMillis - System.currentTimeMillis() > REFRESH_WINDOW_MILLIS) {
            return current.accessToken
        }

        return refreshMutex.withLock {
            val latest = preferences.currentState()
                ?: throw IOException("Codex is not logged in")
            if (latest.expiresAtMillis - System.currentTimeMillis() > REFRESH_WINDOW_MILLIS) {
                latest.accessToken
            } else {
                refreshAccessToken(latest).accessToken
            }
        }
    }

    suspend fun refreshAccessToken(current: CodexAuthState): CodexAuthState {
        val response = oauthClient.refreshAccessToken(current.refreshToken)
        val accessToken = response.accessToken
            ?: throw IOException("Codex refresh response has no access token")
        val accessClaims = CodexOAuthProtocol.parseJwtClaims(accessToken)
            ?: throw IOException("Codex refreshed access token is not a valid JWT")
        val idClaims = response.idToken?.let(CodexOAuthProtocol::parseJwtClaims)
        val accountId = idClaims?.accountId ?: accessClaims.accountId ?: current.accountId
        val expiresAt = accessClaims.expiresAtMillis
            ?: response.expiresInSeconds?.let { System.currentTimeMillis() + it * 1000L }
            ?: throw IOException("Codex refreshed access token has no expiration")
        val updated = current.copy(
            accessToken = accessToken,
            refreshToken = response.refreshToken ?: current.refreshToken,
            expiresAtMillis = expiresAt,
            accountId = accountId,
            residency = accessClaims.residency ?: idClaims?.residency ?: current.residency,
            email = idClaims?.email ?: accessClaims.email ?: current.email,
        )
        preferences.save(updated)
        return updated
    }

    suspend fun logout() {
        val current = preferences.currentState()
        try {
            if (current != null) {
                oauthClient.revokeToken(current.refreshToken, "refresh_token")
            }
        } catch (error: Exception) {
            AppLogger.e(TAG, "Failed to revoke Codex OAuth token", error)
        } finally {
            preferences.clear()
        }
    }

    fun currentAccountId(): String? = preferences.currentState()?.accountId

    fun currentResidency(): String? = preferences.currentState()?.residency

    companion object {
        private const val TAG = "CodexAuthManager"
        private const val REFRESH_WINDOW_MILLIS = 5 * 60 * 1000L

        @Volatile
        private var instance: CodexAuthManager? = null

        fun getInstance(context: Context): CodexAuthManager {
            return instance ?: synchronized(this) {
                instance ?: CodexAuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
