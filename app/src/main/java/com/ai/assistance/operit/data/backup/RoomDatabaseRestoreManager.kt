package com.ai.assistance.operit.data.backup

import android.content.Context
import android.net.Uri
import com.ai.assistance.operit.data.db.AppDatabase
import com.ai.assistance.operit.data.stats.TokenStatSpool
import com.ai.assistance.operit.util.AppLogger
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object RoomDatabaseRestoreManager {

    private const val TAG = "RoomDbRestore"
    private const val DB_NAME = "app_database"

    private const val AUTO_BACKUP_FILE_PREFIX = "room_db_backup_"
    private const val MANUAL_BACKUP_FILE_PREFIX = "room_db_manual_backup_"

    fun listRecentAutoBackups(context: Context, limit: Int = 3): List<File> {
        val newDir = OperitBackupDirs.roomDbDir()
        val legacyDir = OperitBackupDirs.operitRootDir()

        val backups = sequenceOf(newDir, legacyDir)
            .flatMap { dir ->
                (dir.listFiles { f ->
                    f.isFile && f.name.startsWith(AUTO_BACKUP_FILE_PREFIX) && f.name.endsWith(".zip")
                }?.asSequence() ?: emptySequence())
            }
            .distinctBy { it.name }
            .toList()

        return backups.sortedByDescending { it.name }.take(limit)
    }

    fun listRecentBackups(context: Context, limit: Int = 3): List<File> {
        val newDir = OperitBackupDirs.roomDbDir()
        val legacyDir = OperitBackupDirs.operitRootDir()

        val backups = sequenceOf(newDir, legacyDir)
            .flatMap { dir ->
                (dir.listFiles { f ->
                    f.isFile && isRoomDatabaseBackupFile(f.name)
                }?.asSequence() ?: emptySequence())
            }
            .distinctBy { it.name }
            .toList()

        return backups
            .sortedWith(compareByDescending<File> { it.lastModified() }.thenByDescending { it.name })
            .take(limit)
    }

    fun isRoomDatabaseBackupFile(name: String): Boolean {
        return (name.startsWith(AUTO_BACKUP_FILE_PREFIX) || name.startsWith(MANUAL_BACKUP_FILE_PREFIX)) &&
            name.endsWith(".zip")
    }

    suspend fun restoreFromBackupUri(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            RoomDatabaseBackupRestoreLock.mutex.withLock {
                val cacheFile = File.createTempFile("room_db_restore_", ".zip", context.cacheDir)
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Failed to open uri")

                    restoreFromBackupFileInternal(context, cacheFile)
                } finally {
                    cacheFile.delete()
                }
            }
        }
    }

    suspend fun restoreFromBackupFile(context: Context, zipFile: File) {
        withContext(Dispatchers.IO) {
            RoomDatabaseBackupRestoreLock.mutex.withLock {
                restoreFromBackupFileInternal(context, zipFile)
            }
        }
    }

    private suspend fun restoreFromBackupFileInternal(context: Context, zipFile: File) {
        if (!zipFile.exists() || !zipFile.isFile) {
            throw IllegalArgumentException("Backup file not found: ${zipFile.absolutePath}")
        }

        val targetDb = context.getDatabasePath(DB_NAME)
        val targetWal = File(targetDb.absolutePath + "-wal")
        val targetShm = File(targetDb.absolutePath + "-shm")

        val dir = targetDb.parentFile ?: throw IllegalStateException("Database dir not found")

        val tmpDb = File(dir, "${DB_NAME}.restore.tmp")
        val tmpWal = File(dir, "${DB_NAME}-wal.restore.tmp")
        val tmpShm = File(dir, "${DB_NAME}-shm.restore.tmp")

        tmpDb.delete()
        tmpWal.delete()
        tmpShm.delete()

        try {
            // P1 终审：两阶段恢复屏障。prepareBeforeCommit 关闭数据库并完成全部可失败
            // 的准备工作（解压 ZIP 到 tmp 文件 + 验证必需条目）；commitReplacement 持久化
            // REPLACING 标记——只有该标记成功落盘后 restore epoch 才递增、恢复前开始的
            // 旧请求在收尾时被明确拒绝；block 只做目标文件删除/替换（不再有可失败的解压
            // 步骤）。备份损坏/缺条目/读取失败都在 commit 之前失败：epoch 不变、进程仍
            // 接受事件，绝不因选择了错误备份文件而锁死当前进程（审计 P1）。
            TokenStatSpool.withExclusiveRestoreAccess(
                context = context,
                prepareBeforeCommit = {
                    try {
                        AppDatabase.closeDatabase()
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "closeDatabase failed", e)
                    }
                    extractAndValidate(zipFile, tmpDb, tmpWal, tmpShm)
                },
                commitReplacement = {
                    RestoreReplacingMarker.persist(context)
                },
                block = {
                    targetWal.delete()
                    targetShm.delete()
                    targetDb.delete()

                    replaceFile(tmpDb, targetDb)
                    if (tmpWal.exists()) {
                        replaceFile(tmpWal, targetWal)
                    } else {
                        tmpWal.delete()
                        targetWal.delete()
                    }

                    if (tmpShm.exists()) {
                        replaceFile(tmpShm, targetShm)
                    } else {
                        tmpShm.delete()
                        targetShm.delete()
                    }
                },
            )
            RestoreReplacingMarker.delete(context)
        } catch (e: Exception) {
            tmpDb.delete()
            tmpWal.delete()
            tmpShm.delete()
            throw e
        }
    }

    /**
     * 解压 ZIP 到 tmp 文件并验证必需条目（审计 P1：在 commit 之前完成全部可失败工作）。
     * 缺 [DB_NAME] 抛 [IllegalArgumentException]；读取失败向上传播——调用方处于
     * prepareBeforeCommit，epoch 未递增、进程仍接受事件。
     */
    private fun extractAndValidate(zipFile: File, tmpDb: File, tmpWal: File, tmpShm: File) {
        var extractedDb = false
        var extractedWal = false
        var extractedShm = false

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val name = entry.name

                when (name) {
                    DB_NAME -> {
                        writeStreamToFile(zis, tmpDb)
                        extractedDb = true
                    }
                    "${DB_NAME}-wal" -> {
                        writeStreamToFile(zis, tmpWal)
                        extractedWal = true
                    }
                    "${DB_NAME}-shm" -> {
                        writeStreamToFile(zis, tmpShm)
                        extractedShm = true
                    }
                }

                zis.closeEntry()
            }
        }

        if (!extractedDb) {
            throw IllegalArgumentException("Invalid backup zip: missing $DB_NAME")
        }
    }

    private fun writeStreamToFile(input: ZipInputStream, target: File) {
        val buffer = ByteArray(64 * 1024)
        BufferedOutputStream(FileOutputStream(target)).use { output ->
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
            }
        }
    }

    private fun replaceFile(from: File, to: File) {
        if (to.exists()) {
            to.delete()
        }
        if (!from.renameTo(to)) {
            from.copyTo(to, overwrite = true)
            from.delete()
        }
    }
}
