package com.ai.assistance.operit.data.backup

import android.content.ContentResolver
import android.net.Uri
import android.os.Looper
import androidx.room.Room
import com.ai.assistance.operit.api.chat.llmprovider.TokenTrackingAIService
import com.ai.assistance.operit.api.chat.llmprovider.TokenTrackingAIService.Companion.RecordOutcome
import com.ai.assistance.operit.data.db.AppDatabase
import com.ai.assistance.operit.data.stats.JdbcSQLiteDriver
import com.ai.assistance.operit.data.stats.TokenStatReliabilityTestBase
import com.ai.assistance.operit.data.stats.TokenStatSpool
import com.ai.assistance.operit.data.stats.TokenStatsLedger
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.OperitPaths
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.sql.SQLException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * P1 终审：生产备份/恢复 Manager 的屏障接线测试（真实 Manager + 真实 spool/Room 文件，
 * 纯 JVM 基建与 [TokenStatReliabilityTestBase] 一致）。
 *
 * 覆盖：备份排空 spool 且事件在备份中恰一次；恢复清除旧 spool 绝不 replay；恢复前开始
 * 的旧 epoch 请求不写入恢复后的数据库；替换前失败可继续、替换开始后失败拒绝新事件。
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class BackupRestoreBarrierTest : TokenStatReliabilityTestBase() {

    private val filesDir get() = File(root, "files")
    private val dataDir get() = File(root, "data")
    private val cacheDir get() = File(root, "cache")
    private val databasesDir get() = File(dataDir, "databases")
    private val spoolDir get() = File(filesDir, TokenStatSpool.SPOOL_DIR_NAME)

    private var mainExecutor: ExecutorService? = null
    private var looperStatic: MockedStatic<Looper>? = null

    @Before
    fun setUpBarrierMocks() {
        // RawSnapshotBackupManager 的 object 初始化会构造 Handler(Looper.getMainLooper())；
        // 纯 JVM 没有 Looper，静态 mock 提供非空实例（onProgress 一律传 null，
        // mainHandler.post 永远不会被调用）。
        looperStatic = Mockito.mockStatic(Looper::class.java).also {
            it.`when`<Looper> { Looper.getMainLooper() }.thenReturn(mock())
        }
        // Manager 用 Dispatchers.Main 汇报进度：安装真实单线程 Main（与
        // CleanupReliabilityTest 相同的模式）。
        mainExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "test-main-thread") }
        Dispatchers.setMain(mainExecutor!!.asCoroutineDispatcher())

        // 生产目录布局：filesDir 内是 spool，Room 数据库在 dataDir/databases 下，
        // 与 RawSnapshotBackupManager 打包的 payload/databases 一致。
        whenever(context.filesDir).thenReturn(filesDir)
        whenever(context.dataDir).thenReturn(dataDir)
        whenever(context.cacheDir).thenReturn(cacheDir)
        whenever(context.getExternalFilesDir(null)).thenReturn(File(root, "external_files"))
        whenever(context.getDatabasePath(any())).thenAnswer { File(databasesDir, it.getArgument<String>(0)) }
        cacheDir.mkdirs()
        // RestoreReplacingMarker.persist 写 filesDir/restore_replacing.flag；
        // 生产 filesDir 恒存在，JVM 测试需显式创建。
        filesDir.mkdirs()
        // RoomDatabaseRestoreManager 把恢复目标写 room_restore_target 下的 tmp 文件，
        // 目标目录需存在（生产由 getDatabasePath 保证）。
        File(root, "room_restore_target").mkdirs()
    }

    @After
    fun tearDownBarrierMocks() {
        Dispatchers.resetMain()
        mainExecutor?.shutdown()
        looperStatic?.close()
    }

    @Test
    fun `export drains spool only event into the backup database exactly once`() = runBlocking {
        val spool = spoolDir.apply { mkdirs() }
        File(spool, "sealed_1.jsonl").writeText(line(request("spool-only-in-backup")) + "\n")
        // spool 内非段文件：验证 spool 目录本身被排除出备份 zip（OperitPaths 排除名单）
        File(spool, "stray.txt").writeText("not a segment")

        OperitPaths.downloadsDirOverrideForTest = File(root, "sdcard")
        try {
            Mockito.mockStatic(AppLogger::class.java).use {
                val out = RawSnapshotBackupManager.exportToBackupDir(context, onProgress = null)
                assertTrue("backup zip must exist", out.isFile)

                // 屏障 drain 后：spool 无残留段、事件已在 Room 中
                assertTrue(
                    "spool must be drained by the snapshot barrier",
                    spool.listFiles().orEmpty().none {
                        it.name.startsWith("sealed_") || it.name == TokenStatSpool.ACTIVE_FILE_NAME
                    },
                )
                assertEquals(1, database.tokenStatsDao().countEvents())

                val exported = unzipTo(out, File(root, "unzipped-backup"))
                // 事件在备份中恰出现一次（经 Room 数据库文件进入备份）
                val restoredDb = File(exported, "payload/databases/app_database")
                assertTrue("backup must contain the drained database", restoredDb.isFile)
                val count = JdbcSQLiteDriver().open(restoredDb.absolutePath).use { connection ->
                    connection.prepare("SELECT count(*) FROM token_stat_events WHERE eventId = 'spool-only-in-backup'")
                        .use { statement ->
                            if (statement.step()) statement.getLong(0) else -1L
                        }
                }
                assertEquals(1L, count)
                // spool 目录被排除出备份，源文件保留
                assertFalse(
                    "spool directory must be excluded from the backup zip",
                    File(exported, "payload/files/${TokenStatSpool.SPOOL_DIR_NAME}/stray.txt").exists(),
                )
                assertTrue("source spool file must stay untouched", File(spool, "stray.txt").exists())
            }
        } finally {
            OperitPaths.downloadsDirOverrideForTest = null
        }
    }

    @Test
    fun `raw restore clears the pre restore spool so replay cannot inject old events`() = runBlocking {
        val spool = spoolDir.apply { mkdirs() }
        // 仅存在于 spool（未入 Room）的旧事件：恢复后绝不能 replay 进新数据库
        File(spool, "sealed_1.jsonl").writeText(line(request("old-pre-restore")) + "\n")
        val zip = rawSnapshotZip(emptyMap())
        val uri = mock<Uri>()
        val resolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(resolver)
        whenever(resolver.openInputStream(uri)).thenReturn(FileInputStream(zip))

        val epochBefore = TokenStatSpool.captureRestoreEpoch()
        // 测试 Room 实例打开着 dataDir/databases/app_database，restore 会替换该目录
        // （Windows 文件锁）；先关闭释放，restore 完成后重建用于断言。
        database.close()
        TokenStatsLedger.databaseProvider = null
        try {
            Mockito.mockStatic(AppLogger::class.java).use {
                RawSnapshotBackupManager.restoreFromBackupUri(context, uri, onProgress = null)
            }
        } finally {
            reopenDatabase()
        }

        // 备份 zip 不含 spool → files 替换按排除名单保留 spool 目录 → 屏障清理删除它
        assertFalse("old spool must be cleared by the restore barrier", spool.exists())
        assertFalse(
            "REPLACING marker must be removed after a successful restore",
            File(filesDir, RestoreReplacingMarker.FILE_NAME).exists(),
        )
        assertTrue(
            "restore must pass through the epoch fencing barrier",
            TokenStatSpool.captureRestoreEpoch() > epochBefore,
        )

        // replay（模拟重启后的重放）：旧事件绝不进入（已替换的）数据库
        TokenStatSpool.replay(context)
        delay(300)
        assertFalse(spool.exists())
        assertEquals(0, database.tokenStatsDao().countEvents())
    }

    private fun reopenDatabase() {
        database =
            Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                .setDriver(JdbcSQLiteDriver())
                .addMigrations(AppDatabase.MIGRATION_20_21)
                .allowMainThreadQueries()
                .build()
        TokenStatsLedger.databaseProvider = { database }
    }

    @Test
    fun `room restore rejects pre restore epoch requests and replaces the database`() = runBlocking {
        // 先让测试库在默认路径（dataDir/databases）打开，再把恢复目标改指独立路径，
        // 避免 JVM 下替换打开中的 Room 文件（Windows 文件锁）。
        database.tokenStatsDao().deleteAllEvents()
        val restoredDb = File(File(root, "room_restore_target"), "app_database")
        whenever(context.getDatabasePath(any())).thenAnswer {
            File(File(root, "room_restore_target"), it.getArgument<String>(0))
        }

        // 恢复前开始的请求：捕获旧 epoch
        val oldRequest = request("old-epoch-room-restore")
        val oldEpoch = oldRequest.sessionEpoch

        val zip = File(cacheDir, "room-restore-test.zip").apply {
            ZipOutputStream(FileOutputStream(this)).use { zos ->
                zos.putNextEntry(ZipEntry("app_database"))
                zos.write(ByteArray(0))
                zos.closeEntry()
            }
        }
        Mockito.mockStatic(AppLogger::class.java).use {
            RoomDatabaseRestoreManager.restoreFromBackupFile(context, zip)
        }

        // 屏障生效：epoch 递增、替换完成后 REPLACING 标记已删除、新事件被拒绝
        assertTrue(
            "restore must bump the restore epoch after the REPLACING commit",
            TokenStatSpool.captureRestoreEpoch() > oldEpoch,
        )
        assertFalse(TokenStatSpool.isAcceptingEvents())
        assertFalse(File(filesDir, RestoreReplacingMarker.FILE_NAME).exists())
        assertEquals(
            "restored database must not contain any old events",
            0L,
            eventCountIn(restoredDb),
        )

        // 恢复前开始的请求（旧 epoch）收尾时被明确拒绝，绝不写入恢复后的数据库/spool
        assertEquals(
            RecordOutcome.LOST,
            TokenTrackingAIService.recordSafely(context, oldRequest),
        )
        assertFalse(File(spoolDir, TokenStatSpool.ACTIVE_FILE_NAME).exists())
        assertEquals(0L, eventCountIn(restoredDb))
    }

    @Test
    fun `restore failure before replacement keeps accepting events and new requests land`() = runBlocking {
        val epochBefore = TokenStatSpool.captureRestoreEpoch()
        // 损坏备份：缺少 manifest → 在 prepareBeforeCommit（替换前）明确失败
        val zip = rawSnapshotZip(emptyMap(), manifest = null)
        val uri = mock<Uri>()
        val resolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(resolver)
        whenever(resolver.openInputStream(uri)).thenReturn(FileInputStream(zip))

        Mockito.mockStatic(AppLogger::class.java).use {
            try {
                RawSnapshotBackupManager.restoreFromBackupUri(context, uri, onProgress = null)
                fail("restore must fail on a corrupt backup")
            } catch (e: IllegalArgumentException) {
                // 替换前的失败：epoch 不变、进程仍接受事件
            }
        }
        assertEquals(epochBefore, TokenStatSpool.captureRestoreEpoch())
        assertTrue("pre-replacement failure must keep accepting events", TokenStatSpool.isAcceptingEvents())

        // 新请求照常入账
        TokenTrackingAIService.recordSafely(context, request("after-failed-restore"))
        awaitEvent("after-failed-restore")
        assertEquals(1, database.tokenStatsDao().countEvents())
    }

    @Test
    fun `restore failure after replacement started rejects all new events`() = runBlocking {
        database.tokenStatsDao().deleteAllEvents()
        val epochBefore = TokenStatSpool.captureRestoreEpoch()
        // payload 含文件：替换（block）开始后必然失败（JVM 下 AtomicFile 复制抛错）
        val zip = rawSnapshotZip(mapOf("payload/files/some-file.txt" to "x".toByteArray()))
        val uri = mock<Uri>()
        val resolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(resolver)
        whenever(resolver.openInputStream(uri)).thenReturn(FileInputStream(zip))

        Mockito.mockStatic(AppLogger::class.java).use {
            try {
                RawSnapshotBackupManager.restoreFromBackupUri(context, uri, onProgress = null)
                fail("restore must fail after replacement started")
            } catch (e: Exception) {
                // 替换开始后的失败：epoch 已递增、本进程拒绝一切新事件直至重启
            }
        }
        assertTrue(
            "restore epoch must bump before replacement starts",
            TokenStatSpool.captureRestoreEpoch() > epochBefore,
        )
        assertFalse(
            "replacement-started failure must reject new events",
            TokenStatSpool.isAcceptingEvents(),
        )
        assertEquals(
            RecordOutcome.LOST,
            TokenTrackingAIService.recordSafely(context, request("post-replacement")),
        )
        assertFalse(File(spoolDir, TokenStatSpool.ACTIVE_FILE_NAME).exists())
        assertEquals(0, database.tokenStatsDao().countEvents())
    }

    @Test
    fun `startup consumes abandoned restore marker and discards pre restore spool`() = runBlocking {
        // 模拟崩溃于"替换已开始（REPLACING 已持久化）但未成功完成（标记未删除）"：
        // 重启后启动路径必须先消费标记、清理旧 spool，再开始 replay——旧事件绝不进数据库。
        val spool = spoolDir.apply { mkdirs() }
        File(spool, "sealed_1.jsonl").writeText(line(request("abandoned-pre-restore")) + "\n")
        val marker = File(filesDir, RestoreReplacingMarker.FILE_NAME)
        marker.writeText("REPLACING\n")

        val consumed = TokenStatSpool.consumeAbandonedRestoreIfAny(context)
        assertTrue("marker must be consumed at startup", consumed)
        assertFalse("old spool must be discarded", spool.exists())
        assertFalse("marker must be removed", marker.exists())

        // 之后正常启动 replay：数据库仍是本机旧库，但旧 spool 已清空，无旧事件注入
        TokenStatSpool.replay(context)
        delay(300)
        assertEquals(0, database.tokenStatsDao().countEvents())
        // 新请求照常落账
        TokenTrackingAIService.recordSafely(context, request("post-abandoned-restore"))
        awaitEvent("post-abandoned-restore")
        assertEquals(1, database.tokenStatsDao().countEvents())
    }

    @Test
    fun `startup without marker does not touch the spool`() = runBlocking {
        val spool = spoolDir.apply { mkdirs() }
        File(spool, "sealed_1.jsonl").writeText(line(request("normal-startup")) + "\n")
        assertFalse("no marker must mean normal startup", TokenStatSpool.consumeAbandonedRestoreIfAny(context))
        TokenStatSpool.replay(context)
        awaitEvent("normal-startup")
        assertEquals(1, database.tokenStatsDao().countEvents())
    }

    @Test
    fun `room restore corrupt zip fails before commit and keeps accepting events`() = runBlocking {
        val epochBefore = TokenStatSpool.captureRestoreEpoch()
        // 缺 app_database 条目的 ZIP：prepareBeforeCommit 解压验证失败 → 不 commit、
        // epoch 不变、marker 不写、进程仍接受事件（绝不用错误备份锁死当前进程）。
        val zip = File(cacheDir, "room-restore-corrupt.zip").apply {
            ZipOutputStream(FileOutputStream(this)).use { zos ->
                zos.putNextEntry(ZipEntry("other-file"))
                zos.write(ByteArray(0))
                zos.closeEntry()
            }
        }
        try {
            RoomDatabaseRestoreManager.restoreFromBackupFile(context, zip)
            fail("restore must fail on a corrupt zip")
        } catch (e: IllegalArgumentException) {
            // expected
        }
        assertEquals(epochBefore, TokenStatSpool.captureRestoreEpoch())
        assertTrue("pre-commit failure must keep accepting events", TokenStatSpool.isAcceptingEvents())
        assertFalse(File(filesDir, RestoreReplacingMarker.FILE_NAME).exists())

        // 新请求照常落账
        TokenTrackingAIService.recordSafely(context, request("after-corrupt-room-restore"))
        awaitEvent("after-corrupt-room-restore")
        assertEquals(1, database.tokenStatsDao().countEvents())
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private val validManifest =
        """
        {"formatVersion":1,"packageName":"com.ai.assistance.operit","createdAt":1,
         "includes":["payload/files/","payload/external_files/","payload/shared_prefs/",
         "payload/datastore/","payload/databases/"],"includeTerminalData":true}
        """.trimIndent()

    /** 生成 RawSnapshotBackupManager 可识别的备份 zip；manifest=null 时缺 manifest（损坏）。 */
    private fun rawSnapshotZip(entries: Map<String, ByteArray>, manifest: String? = validManifest): File {
        val zip = File(cacheDir, "raw-restore-test.zip")
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            if (manifest != null) {
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(manifest.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            entries.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return zip
    }

    private fun unzipTo(zip: File, targetDir: File): File {
        ZipInputStream(java.io.BufferedInputStream(FileInputStream(zip))).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (!entry.isDirectory) {
                    val out = File(targetDir, entry.name)
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { output -> zis.copyTo(output) }
                }
                zis.closeEntry()
            }
        }
        return targetDir
    }

    /** 直连 sqlite 统计事件表；表缺失（恢复后的库为空）视为 0 条旧事件。 */
    private fun eventCountIn(dbFile: File): Long =
        JdbcSQLiteDriver().open(dbFile.absolutePath).use { connection ->
            try {
                connection.prepare("SELECT count(*) FROM token_stat_events").use { statement ->
                    if (statement.step()) statement.getLong(0) else -1L
                }
            } catch (e: SQLException) {
                0L
            }
        }
}
