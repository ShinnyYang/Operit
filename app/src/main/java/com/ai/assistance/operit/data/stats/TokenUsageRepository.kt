package com.ai.assistance.operit.data.stats

import android.content.Context
import com.ai.assistance.operit.data.dao.TokenUsageDao
import com.ai.assistance.operit.data.db.AppDatabase
import com.ai.assistance.operit.data.model.TokenUsageRecordEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Room owner for successful formal-inference usage and pricing. */
class TokenUsageRepository private constructor(context: Context) {
    companion object {
        @Volatile
        private var instance: TokenUsageRepository? = null
        private val databaseAccessMutex = Mutex()

        fun getInstance(context: Context): TokenUsageRepository =
            instance ?: synchronized(this) {
                instance ?: TokenUsageRepository(context.applicationContext).also { instance = it }
            }

        /** Prevent Room access while a restore replaces database files. */
        suspend fun <T> withDatabaseAccess(block: suspend () -> T): T =
            databaseAccessMutex.withLock { block() }

        suspend fun <T> withDatabaseRestore(block: suspend () -> T): T =
            withDatabaseAccess { block() }
    }

    private val appContext = context.applicationContext

    internal suspend fun <T> withDao(block: suspend (TokenUsageDao) -> T): T =
        withDatabaseAccess { block(AppDatabase.getDatabase(appContext).tokenUsageDao()) }

    suspend fun record(record: TokenUsageRecordEntity) {
        withDao { dao -> dao.insertRecord(record) }
    }
}
