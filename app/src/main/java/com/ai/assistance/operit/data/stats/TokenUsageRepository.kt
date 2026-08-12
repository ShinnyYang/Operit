package com.ai.assistance.operit.data.stats

import android.content.Context
import androidx.room.withTransaction
import com.ai.assistance.operit.data.dao.TokenUsageDao
import com.ai.assistance.operit.data.db.AppDatabase
import com.ai.assistance.operit.data.model.TokenStatsModelEntity
import com.ai.assistance.operit.data.model.TokenUsageIdentity
import com.ai.assistance.operit.data.model.TokenUsageRecordEntity
import com.ai.assistance.operit.data.model.TokenUsageRecordSource
import com.ai.assistance.operit.data.preferences.ApiPreferences
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Room owner for token usage plus the one-time cumulative-counter import. */
class TokenUsageRepository private constructor(context: Context) {
    companion object {
        private const val TAG = "TokenUsageRepository"

        @Volatile
        private var instance: TokenUsageRepository? = null

        fun getInstance(context: Context): TokenUsageRepository =
            instance ?: synchronized(this) {
                instance ?: TokenUsageRepository(context.applicationContext).also { instance = it }
            }
    }

    private val appContext = context.applicationContext
    internal val database = AppDatabase.getDatabase(appContext)
    internal val dao: TokenUsageDao = database.tokenUsageDao()
    private val legacyDataSource = ApiPreferences.getInstance(appContext)
    private val statsPreferences = TokenStatsPreferences(appContext)
    private val importMutex = Mutex()

    @Volatile
    private var initializationComplete = false

    suspend fun ensureInitialized() {
        if (initializationComplete) return
        importMutex.withLock {
            if (initializationComplete) return
            if (statsPreferences.importedAtMs() == null) {
                val snapshot = legacyDataSource.readTokenStatsMigrationSnapshot()
                val importedAtMs = System.currentTimeMillis()
                database.withTransaction {
                    dao.insertRecords(snapshot.totals.map { total ->
                        TokenUsageRecordEntity(
                            importKey = TokenUsageIdentity(null, total.provider, total.model).encode(),
                            occurredAtMs = null,
                            source = TokenUsageRecordSource.REQUEST,
                            configId = null,
                            provider = total.provider,
                            model = total.model,
                            category = null,
                            status = null,
                            requestCount = total.requestCount,
                            uncachedInputTokens =
                                (total.inputTokens - total.cachedInputTokens).coerceAtLeast(0L),
                            cachedInputTokens = total.cachedInputTokens,
                            cacheWriteTokens = null,
                            totalInputTokens = total.inputTokens,
                            outputTokens = total.outputTokens,
                            reasoningTokens = null,
                            ttftMs = null,
                            durationMs = null,
                        )
                    })
                    snapshot.prices.forEach { price ->
                        val current =
                            dao.getStatsModel("", price.provider, price.model)
                                ?: TokenStatsModelEntity("", price.provider, price.model)
                        dao.upsertStatsModel(
                            current.copy(
                                billingMode = price.settings.billingMode?.name,
                                currency = price.settings.currency?.name,
                                inputPricePerMillion = price.settings.inputPricePerMillion,
                                cachedInputPricePerMillion =
                                    price.settings.cachedInputPricePerMillion,
                                cacheWritePricePerMillion =
                                    price.settings.cacheWritePricePerMillion,
                                outputPricePerMillion = price.settings.outputPricePerMillion,
                                pricePerRequest = price.settings.pricePerRequest,
                            )
                        )
                    }
                }
                statsPreferences.completeMigration(
                    importedAtMs = importedAtMs,
                    releasedUsdToCnyRate = snapshot.usdToCnyRate,
                )
                AppLogger.i(
                    TAG,
                    "Imported ${snapshot.totals.size} cumulative totals and " +
                        "${snapshot.prices.size} price settings",
                )
            }
            legacyDataSource.clearMigratedTokenStatsData()
            initializationComplete = true
        }
    }

    suspend fun record(record: TokenUsageRecordEntity) {
        ensureInitialized()
        dao.insertRecord(record)
    }

}
