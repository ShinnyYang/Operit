package com.ai.assistance.operit.data.stats

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import com.ai.assistance.operit.data.db.AppDatabase
import com.ai.assistance.operit.data.model.BillingMode
import com.ai.assistance.operit.data.model.TokenStatEventEntity
import com.ai.assistance.operit.data.model.TokenStatIdentityEntity
import com.ai.assistance.operit.data.preferences.ApiPreferences
import com.ai.assistance.operit.util.AppLogger
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * 导入器测试：
 * 1. 取消传播：ensureMigrated 的 catch(Exception) 不得吞掉 CancellationException。
 * 2. 冻结价格 + 计数跟踪 + 恢复生命周期（真实 ApiPreferences 快照路径 + 真实
 *    Room 数据库）：
 *    - 无自定义价格迁移也冻结；计数不变时普通 setter/快照变化不重估 baseline；
 *    - 计数变化（真实累计 setter 增长 / 用户 reset 降低）用行内冻结价格重估，
 *      整体替换为快照绝对值，不产生负增量；冻结价格列永不被普通启动替换；
 *      consumePendingRestore）才受控补导一次（替换冻结价格）；部分字段恢复不触发
 *      任何启发式，直到 completion hook 才统一处理；相同 generation 重复消费幂等；
 *    - 受控补导不删除/不覆盖活动 DataStore 文件、不级联删除事件。
 *
 * DataStore 隔离：模块级 `Context.apiDataStore` 委托在单个 JVM 内只创建一个
 * DataStore 实例（绑定首个访问它的 Context），且每个文件的 DataStore 写入在
 * Windows 上不稳定（tmp→目标 renameTo）。因此：
 * - 每个“生命周期阶段”使用独立 filesDir 临时目录，阶段间通过反射清空单例，
 *   使每阶段只读/写自己的文件；
 * - “旧 DataStore 文件恢复”用种子文件（独立 DataStore 实例单次 edit 生成）
 *   复制到目标阶段目录的 datastore/ 来真实模拟，之后重建 ApiPreferences
 *   读取恢复后的文件——与恢复完成→冷启动的实际生命周期一致，全程不删除
 *   或覆盖活动 actor 的文件。
 */
class TokenBaselineImportRunnerTest {

    @Before
    fun isolate() {
        clearApiDataStoreSingleton()
        TokenBaselineImportRunner.databaseProvider = null
        injectApiPreferences(null)
    }

    /** 清空 `Context.apiDataStore` 委托缓存的数据存储单例（隔离生命周期）。 */
    private fun clearApiDataStoreSingleton() {
        val facade = Class.forName("com.ai.assistance.operit.data.preferences.ApiPreferencesKt")
        val delegateField = facade.getDeclaredField("apiDataStore\$delegate")
        delegateField.isAccessible = true
        val delegate = delegateField.get(null)
        val instanceField =
            delegate.javaClass.getDeclaredField("INSTANCE").apply { isAccessible = true }
        instanceField.set(delegate, null)
    }

    private fun injectApiPreferences(instance: ApiPreferences?) {
        val field =
            ApiPreferences::class.java
                .getDeclaredField("INSTANCE")
                .apply { isAccessible = true }
        field.set(null, instance)
    }

    private fun constructApiPreferences(context: Context): ApiPreferences {
        val constructor =
            ApiPreferences::class.java
                .getDeclaredConstructor(Context::class.java)
                .apply { isAccessible = true }
        return constructor.newInstance(context)
    }

    private fun mockContext(filesDir: File): Context {
        val context = mock<Context>()
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.packageName).thenReturn("com.ai.assistance.operit")
        whenever(context.filesDir).thenReturn(filesDir)
        whenever(context.noBackupFilesDir).thenReturn(File(filesDir, "no_backup"))
        whenever(context.getDatabasePath(any())).thenAnswer { invocation ->
            File(filesDir, invocation.getArgument<String>(0))
        }
        return context
    }

    /** 真实 Room 数据库（JVM 驱动），与迁移测试同一套支撑。 */
    private fun openDatabase(filesDir: File): AppDatabase =
        Room.databaseBuilder(mockContext(filesDir), AppDatabase::class.java, "app_database")
            .setDriver(JdbcSQLiteDriver())
            .addMigrations(AppDatabase.MIGRATION_28_29)
            .allowMainThreadQueries()
            .build()

    // ==== 旧 DataStore 文件种子与“恢复”模拟 ====

    private val providerModel = "DEEPSEEK:deepseek-chat"
    private val providerModelB = "OPENAI:gpt-4o"

    private fun seedTwoModels(seedFile: File, modelBStats: Triple<Long, Long, Long>) {
        seedPreferencesFile(seedFile) { prefs ->
            prefs[ApiPreferences.getTokenInputKey(providerModel)] = 1_000_000L
            prefs[ApiPreferences.getTokenCachedInputKey(providerModel)] = 200_000L
            prefs[ApiPreferences.getTokenOutputKey(providerModel)] = 500_000L
            prefs[ApiPreferences.getTokenInputKey(providerModelB)] = modelBStats.first
            prefs[ApiPreferences.getTokenCachedInputKey(providerModelB)] = modelBStats.second
            prefs[ApiPreferences.getTokenOutputKey(providerModelB)] = modelBStats.third
        }
    }

    /**
     * 用独立 DataStore 实例单次 edit 生成“旧偏好文件”种子（等价于备份中的
     * api_settings.preferences_pb）。种子文件独立于被测阶段目录，不触碰任何
     * 活动 actor 的文件。
     *
     * 注意：不取消 scope——取消会打断 DataStore 内部 actor 并触发
     * CompletionHandlerException；测试进程短命，遗留的闲置 actor 无影响。
     */
    private fun seedPreferencesFile(seedFile: File, block: (MutablePreferences) -> Unit) {
        seedFile.parentFile?.mkdirs()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store =
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { seedFile },
            )
        runBlocking { store.edit { block(it) } }
    }

    private fun seedCountsOnly(seedFile: File) {
        seedPreferencesFile(seedFile) { prefs ->
            prefs[ApiPreferences.getTokenInputKey(providerModel)] = 1_000_000L
            prefs[ApiPreferences.getTokenCachedInputKey(providerModel)] = 200_000L
            prefs[ApiPreferences.getTokenOutputKey(providerModel)] = 500_000L
        }
    }

    private fun seedWithInputPrice(seedFile: File, inputPrice: Double) {
        seedPreferencesFile(seedFile) { prefs ->
            prefs[ApiPreferences.getTokenInputKey(providerModel)] = 1_000_000L
            prefs[ApiPreferences.getTokenCachedInputKey(providerModel)] = 200_000L
            prefs[ApiPreferences.getTokenOutputKey(providerModel)] = 500_000L
            prefs[ApiPreferences.getModelInputPriceKey(providerModel)] = inputPrice.toFloat()
        }
    }

    private fun seedWithOutputPrice(seedFile: File, outputPrice: Double) {
        seedPreferencesFile(seedFile) { prefs ->
            prefs[ApiPreferences.getTokenInputKey(providerModel)] = 1_000_000L
            prefs[ApiPreferences.getTokenCachedInputKey(providerModel)] = 200_000L
            prefs[ApiPreferences.getTokenOutputKey(providerModel)] = 500_000L
            prefs[ApiPreferences.getModelOutputPriceKey(providerModel)] = outputPrice.toFloat()
        }
    }

    /**
     * 模拟“恢复完成”：把种子文件复制到目标阶段的 datastore 目录，随后该阶段
     * 重建 ApiPreferences（首次访问读取恢复后的文件）——与恢复完成→冷启动的
     * 真实生命周期一致。复制发生在该阶段 DataStore actor 创建之前，不删除/
     * 不覆盖任何活动 actor 的文件。
     */
    private fun restorePreferencesInto(filesDir: File, seedFile: File) {
        val target = File(filesDir, "datastore/api_settings.preferences_pb")
        target.parentFile?.mkdirs()
        seedFile.copyTo(target, overwrite = true)
    }

    private suspend fun assertBaselineFrozenAt(
        database: AppDatabase,
        expectedCost: Double,
        expectedInputTokens: Long,
    ) {
        val dao = database.tokenStatsDao()
        val baseline = dao.getAllBaselines().single()
        assertEquals(expectedCost, baseline.costInPricingCurrency!!, 1e-9)
        assertEquals(expectedInputTokens, baseline.inputTokens)
    }

    // ==== 测试 ====

    @Test
    fun `cancellation propagates through import runner instead of being swallowed`() =
        runBlocking {
            val context = mock<Context>()
            whenever(context.applicationContext).thenReturn(context)
            val prefs = mock<ApiPreferences>()
            whenever(prefs.legacyStatsSnapshot())
                .thenThrow(CancellationException("import cancelled"))
            injectApiPreferences(prefs)
            try {
                TokenBaselineImportRunner.ensureMigrated(context)
                fail("expected CancellationException to propagate")
            } catch (e: CancellationException) {
                assertEquals("import cancelled", e.message)
            } finally {
                injectApiPreferences(null)
            }
        }

    @Test
    fun `migration without custom price freezes and later price change does not reprice`() =
        runBlocking {
            val dbDir = kotlin.io.path.createTempDirectory("runner-test").toFile()
            val database = openDatabase(dbDir)
            TokenBaselineImportRunner.databaseProvider = { database }

            // 阶段 A：旧偏好文件只有计数、没有自定义价格（普通用户从未自定义价格
            // 也代表完整状态）→ 首次迁移按内置默认价估算并冻结
            val phaseA = kotlin.io.path.createTempDirectory("runner-phase").toFile()
            val seedA = kotlin.io.path.createTempDirectory("runner-seed").toFile()
            val seedFileA = File(seedA, "seed.preferences_pb")
            seedCountsOnly(seedFileA)
            restorePreferencesInto(phaseA, seedFileA)
            val ctxA = mockContext(phaseA)
            val prefsA = constructApiPreferences(ctxA)
            injectApiPreferences(prefsA)
            try {
                Mockito.mockStatic(AppLogger::class.java).use {
                    TokenBaselineImportRunner.ensureMigrated(ctxA)
                }
                // 800k*1 + 200k*0.02 + 500k*2 = 1.804
                assertBaselineFrozenAt(database, 1.804, 1_000_000L)
            } finally {
                injectApiPreferences(null)
            }

            // 阶段 B：冷启动后快照含用户价格（相当于首次普通改价的快照路径），
            // 但没有恢复生命周期信号 → 已冻结 baseline 不得重估
            clearApiDataStoreSingleton()
            val phaseB = kotlin.io.path.createTempDirectory("runner-phase").toFile()
            val seedB = kotlin.io.path.createTempDirectory("runner-seed").toFile()
            val seedFileB = File(seedB, "seed.preferences_pb")
            seedWithInputPrice(seedFileB, 2.0)
            restorePreferencesInto(phaseB, seedFileB)
            val ctxB = mockContext(phaseB)
            val prefsB = constructApiPreferences(ctxB)
            injectApiPreferences(prefsB)
            try {
                Mockito.mockStatic(AppLogger::class.java).use {
                    TokenBaselineImportRunner.ensureMigrated(ctxB)
                }
                assertBaselineFrozenAt(database, 1.804, 1_000_000L)
                assertEquals(1, database.tokenStatsDao().countBaselines())
            } finally {
                injectApiPreferences(null)
                TokenBaselineImportRunner.databaseProvider = null
                database.close()
            }
        }

    fun `normal import preserves baseline for model missing from current snapshot`() =
        runBlocking {
            val dbDir = kotlin.io.path.createTempDirectory("runner-test").toFile()
            val database = openDatabase(dbDir)
            TokenBaselineImportRunner.databaseProvider = { database }

            // 阶段 A：旧偏好含 A+B 两个模型 → 两个 baseline
            val phaseA = kotlin.io.path.createTempDirectory("runner-phase").toFile()
            val seedA = kotlin.io.path.createTempDirectory("runner-seed").toFile()
            val seedFileA = File(seedA, "seed.preferences_pb")
            seedTwoModels(seedFileA, Triple(2_000_000L, 0L, 1_000_000L))
            restorePreferencesInto(phaseA, seedFileA)
            val ctxA = mockContext(phaseA)
            val prefsA = constructApiPreferences(ctxA)
            injectApiPreferences(prefsA)
            try {
                Mockito.mockStatic(AppLogger::class.java).use {
                    TokenBaselineImportRunner.ensureMigrated(ctxA)
                }
                assertEquals(2, database.tokenStatsDao().countBaselines())
            } finally {
                injectApiPreferences(null)
            }

            // 阶段 B：快照暂时只含 A（B 的偏好键缺失/被清空）→ 普通导入
            // 不得删除 B 的 baseline，只更新明确存在的 A
            clearApiDataStoreSingleton()
            val phaseB = kotlin.io.path.createTempDirectory("runner-phase").toFile()
            val seedB = kotlin.io.path.createTempDirectory("runner-seed").toFile()
            val seedFileB = File(seedB, "seed.preferences_pb")
            seedPreferencesFile(seedFileB) { prefs ->
                prefs[ApiPreferences.getTokenInputKey(providerModel)] = 2_000_000L
                prefs[ApiPreferences.getTokenCachedInputKey(providerModel)] = 200_000L
                prefs[ApiPreferences.getTokenOutputKey(providerModel)] = 1_000_000L
            }
            restorePreferencesInto(phaseB, seedFileB)
            val ctxB = mockContext(phaseB)
            val prefsB = constructApiPreferences(ctxB)
            injectApiPreferences(prefsB)
            try {
                Mockito.mockStatic(AppLogger::class.java).use {
                    TokenBaselineImportRunner.ensureMigrated(ctxB)
                }
                val dao = database.tokenStatsDao()
                assertEquals("B baseline must survive a normal import", 2, dao.countBaselines())
                val identityB =
                    TokenStatIdentityResolver.identityId("", "OPENAI", "gpt-4o")
                val baselineB = dao.getBaseline(identityB)!!
                assertEquals(2_000_000L, baselineB.inputTokens)
                assertEquals(1_000_000L, baselineB.outputTokens)
                val identityA =
                    TokenStatIdentityResolver.identityId("", "DEEPSEEK", "deepseek-chat")
                assertEquals(2_000_000L, dao.getBaseline(identityA)!!.inputTokens)
            } finally {
                injectApiPreferences(null)
                TokenBaselineImportRunner.databaseProvider = null
                database.close()
            }
        }

    @Test
    fun `explicit reset deletes only the reset model baseline`() =
        runBlocking {
            val dbDir = kotlin.io.path.createTempDirectory("runner-test").toFile()
            val database = openDatabase(dbDir)
            TokenBaselineImportRunner.databaseProvider = { database }

            val phase = kotlin.io.path.createTempDirectory("runner-phase").toFile()
            val seed = kotlin.io.path.createTempDirectory("runner-seed").toFile()
            val seedFile = File(seed, "seed.preferences_pb")
            seedTwoModels(seedFile, Triple(2_000_000L, 0L, 1_000_000L))
            restorePreferencesInto(phase, seedFile)
            val ctx = mockContext(phase)
            val prefs = constructApiPreferences(ctx)
            injectApiPreferences(prefs)
            try {
                Mockito.mockStatic(AppLogger::class.java).use {
                    TokenBaselineImportRunner.ensureMigrated(ctx)
                }
                val dao = database.tokenStatsDao()
                assertEquals(2, dao.countBaselines())

                // 显式重置 B：独立删除路径，只删 B 的 baseline
                TokenStatsResetCoordinator.daoProvider = { dao }
                try {
                    TokenStatsResetCoordinator.resetStatisticsForProviderModel(
                        ctx,
                        providerModelB,
                    )
                } finally {
                    TokenStatsResetCoordinator.daoProvider = null
                }
                assertEquals(1, dao.countBaselines())
                val identityB =
                    TokenStatIdentityResolver.identityId("", "OPENAI", "gpt-4o")
                assertEquals(null, dao.getBaseline(identityB))
                val identityA =
                    TokenStatIdentityResolver.identityId("", "DEEPSEEK", "deepseek-chat")
                assertEquals(1_000_000L, dao.getBaseline(identityA)!!.inputTokens)
            } finally {
                injectApiPreferences(null)
                TokenBaselineImportRunner.databaseProvider = null
                database.close()
            }
        }

    @Test

    @Test
    fun `cumulative setter growth on normal startup updates counts with frozen pricing`() =
        runBlocking {
            val dbDir = kotlin.io.path.createTempDirectory("runner-test").toFile()
            val database = openDatabase(dbDir)
            TokenBaselineImportRunner.databaseProvider = { database }

            // 阶段 A：首次导入 N（含自定义价格 1.0/2.0）→ 冻结
            val phaseA = kotlin.io.path.createTempDirectory("runner-phase").toFile()
            val seedA = kotlin.io.path.createTempDirectory("runner-seed").toFile()
            val seedFileA = File(seedA, "seed.preferences_pb")
            seedWithInputPrice(seedFileA, 1.0)
            restorePreferencesInto(phaseA, seedFileA)
            val ctxA = mockContext(phaseA)
            val prefsA = constructApiPreferences(ctxA)
            injectApiPreferences(prefsA)
            try {
                Mockito.mockStatic(AppLogger::class.java).use {
                    TokenBaselineImportRunner.ensureMigrated(ctxA)
                }
                val dao = database.tokenStatsDao()
                val before = dao.getAllBaselines().single()
                assertEquals(1_000_000L, before.inputTokens)
                assertEquals(1.804, before.costInPricingCurrency!!, 1e-9)
                val frozenInput = before.frozenInputPricePerMillion
                val frozenOutput = before.frozenOutputPricePerMillion
            } finally {
                injectApiPreferences(null)
            }

            // 阶段 B：真实累计 setter 增长计数（updateTokensForProviderModel 是
            // 现有累计 setter；新阶段独立 DataStore 文件，首写安全），再次普通启动：
            // 计数更新为 N+X，但冻结价格不变（按冻结价重估成本）。
            clearApiDataStoreSingleton()
            val phaseB = kotlin.io.path.createTempDirectory("runner-phase").toFile()
            val ctxB = mockContext(phaseB)
            val prefsB = constructApiPreferences(ctxB)
            injectApiPreferences(prefsB)
            try {
                // 真实累计 setter：在空快照上累计写入 N+X（输入 2M、输出 1M、缓存 200k）
                prefsB.updateTokensForProviderModel(
                    providerModel,
                    inputTokens = 2_000_000,
                    outputTokens = 1_000_000,
                    cachedInputTokens = 200_000,
                )
                Mockito.mockStatic(AppLogger::class.java).use {
                    TokenBaselineImportRunner.ensureMigrated(ctxB)
                }
                val dao = database.tokenStatsDao()
                val after = dao.getAllBaselines().single()
                assertEquals(2_000_000L, after.inputTokens)
                assertEquals(1_000_000L, after.outputTokens)
                assertEquals(200_000L, after.cachedInputTokens)
                // 冻结价格列不被普通启动替换（输入仍 1.0、输出仍 2.0）
                assertEquals(1.0, after.frozenInputPricePerMillion!!, 1e-9)
                assertEquals(2.0, after.frozenOutputPricePerMillion!!, 1e-9)
                // 按冻结价重估：1.8M*1.0 + 200k*0.02 + 1M*2.0 = 1.8+0.004+2.0
                assertEquals(3.804, after.costInPricingCurrency!!, 1e-9)
            } finally {
                injectApiPreferences(null)
                TokenBaselineImportRunner.databaseProvider = null
                database.close()
            }
        }

    @Test
    fun `user reset drop on normal startup replaces baseline with absolute smaller value`() =
        runBlocking {
            val dbDir = kotlin.io.path.createTempDirectory("runner-test").toFile()
            val database = openDatabase(dbDir)
            TokenBaselineImportRunner.databaseProvider = { database }

            // 阶段 A：首次导入 N（含自定义价格）
            val phaseA = kotlin.io.path.createTempDirectory("runner-phase").toFile()
            val seedA = kotlin.io.path.createTempDirectory("runner-seed").toFile()
            val seedFileA = File(seedA, "seed.preferences_pb")
            seedWithInputPrice(seedFileA, 1.0)
            restorePreferencesInto(phaseA, seedFileA)
            val ctxA = mockContext(phaseA)
            val prefsA = constructApiPreferences(ctxA)
            injectApiPreferences(prefsA)
            try {
                Mockito.mockStatic(AppLogger::class.java).use {
                    TokenBaselineImportRunner.ensureMigrated(ctxA)
                }
                assertEquals(1_000_000L, database.tokenStatsDao().getAllBaselines().single().inputTokens)
            } finally {
                injectApiPreferences(null)
            }

            // 阶段 B：用户 reset 旧统计 → 快照计数变小（绝对值替换，不产生负增量）
            clearApiDataStoreSingleton()
            val phaseB = kotlin.io.path.createTempDirectory("runner-phase").toFile()
            val seedB = kotlin.io.path.createTempDirectory("runner-seed").toFile()
            val seedFileB = File(seedB, "seed.preferences_pb")
            seedPreferencesFile(seedFileB) { prefs ->
                prefs[ApiPreferences.getTokenInputKey(providerModel)] = 100_000L
                prefs[ApiPreferences.getTokenCachedInputKey(providerModel)] = 0L
                prefs[ApiPreferences.getTokenOutputKey(providerModel)] = 50_000L
                prefs[ApiPreferences.getModelInputPriceKey(providerModel)] = 1.0f
            }
            restorePreferencesInto(phaseB, seedFileB)
            val ctxB = mockContext(phaseB)
            val prefsB = constructApiPreferences(ctxB)
            injectApiPreferences(prefsB)
            try {
                Mockito.mockStatic(AppLogger::class.java).use {
                    TokenBaselineImportRunner.ensureMigrated(ctxB)
                }
                val dao = database.tokenStatsDao()
                val after = dao.getAllBaselines().single()
                assertEquals(100_000L, after.inputTokens)
                assertEquals(50_000L, after.outputTokens)
                // 冻结价重估：100k*1.0 + 50k*2.0 = 0.1 + 0.1
                assertEquals(0.2, after.costInPricingCurrency!!, 1e-9)
                assertEquals(1.0, after.frozenInputPricePerMillion!!, 1e-9)
                assertEquals(2.0, after.frozenOutputPricePerMillion!!, 1e-9)
            } finally {
                injectApiPreferences(null)
                TokenBaselineImportRunner.databaseProvider = null
                database.close()
            }
        }
}
