package com.ai.assistance.operit.data.stats

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * 统计 spool 元数据的崩溃安全存储（canonical + `.new` + `.bak` + 临时文件协议）。
 *
 * 语义：
 * - [write]：完整新内容先写唯一临时文件并 fsync，再经原子替换/回退协议提交到
 *   canonical；任意中断后 canonical 必为完整旧值或完整新值（不存在半写内容）。
 * - [read]：canonical 缺失时从 `.new`/`.bak` sidecar 恢复完整值（优先新值）；
 *   strict 模式下目录项持久确认失败抛 [IOException]（fail-closed）。
 * - [delete]：删除 canonical 与全部 sidecar，残留由下次使用清理。
 *
 * [strictDirectorySync] 为目录项持久确认回调（见 [Quarantine] 的
 * [TokenStatSpool.requireSpoolDirSync]）；为 null 时尽力而为（普通测试路径）。
 */
internal class TokenStatMetaStore(
    private val file: File,
    private val atomicMove: (File, File) -> Boolean = TokenStatMetaStore::defaultAtomicMove,
    private val strictDirectorySync: ((File) -> Boolean)? = null,
) {
    private val parent: File
        get() = file.parentFile
            ?: throw IllegalStateException("Marker file has no parent directory")

    private val newFile: File
        get() = File(parent, "${file.name}.new")

    private val bakFile: File
        get() = File(parent, "${file.name}.bak")

    suspend fun write(content: String) {
        parent.mkdirs()
        // 上次中断残留恢复：目标缺失时先把完整旧/新值放回目标，之后清理才不会
        // 丢失信号；目标存在时 .new/.bak 都已被目标内容取代，可安全清理。
        if (!file.isFile) {
            when {
                newFile.isFile -> {
                    strictRename(newFile, file)
                    strictDelete(bakFile)
                }
                bakFile.isFile -> {
                    strictRename(bakFile, file)
                }
                else -> Unit
            }
        }
        strictDelete(bakFile)
        strictDelete(newFile)
        deleteStaleTmpFiles()
        // 1. 写完整新内容到唯一临时文件并 fsync：此后内容在断电/崩溃后仍完整。
        val tmp = File(parent, "${file.name}.tmp${UUID.randomUUID()}")
        try {
            FileOutputStream(tmp).use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            strictRename(tmp, newFile)
        } catch (e: Exception) {
            deleteQuietly(tmp)
            throw e
        }
        // 2. 首选原子替换（目标已存在时也允许）；不支持则走回退协议。
        val atomicMoved = try {
            atomicMove(newFile, file)
        } catch (e: AtomicMoveNotSupportedException) {
            false
        } catch (e: IOException) {
            false
        }
        if (atomicMoved) {
            // 提交点：canonical 已替换为完整新值，目录项必须确认持久才允许成功。
            requireDirSyncDurable()
            return
        }
        // 3. old/new/backup 回退：任意中断后目标必为完整旧或完整新值。
        if (file.exists()) strictRename(file, bakFile)
        if (!newFile.renameTo(file)) {
            // 提交失败：尽力把旧值放回目标，保持可读的完整旧内容。
            if (bakFile.exists()) {
                strictRename(bakFile, file)
            }
            throw IOException("Failed to move new content into place: ${file.path}")
        }
        requireDirSyncDurable()
        strictDelete(bakFile)
    }

    suspend fun read(): String? {
        if (file.isFile) {
            strictDelete(newFile)
            strictDelete(bakFile)
            deleteStaleTmpFiles()
            // 返回 canonical 内容前必须确认目录项持久（strict 模式）。
            requireDirSyncDurable()
            return file.readText()
        }
        // 目标缺失：恢复完整值（.new 已 fsync，存在即完整；优先新值）。
        return when {
            newFile.isFile -> {
                strictRename(newFile, file)
                strictDelete(bakFile)
                requireDirSyncDurable()
                file.readText()
            }
            bakFile.isFile -> {
                strictRename(bakFile, file)
                requireDirSyncDurable()
                file.readText()
            }
            else -> {
                deleteStaleTmpFiles()
                null
            }
        }
    }

    suspend fun delete() {
        strictDelete(file)
        strictDelete(newFile)
        strictDelete(bakFile)
        deleteStaleTmpFiles()
    }

    private fun deleteStaleTmpFiles() {
        parent.listFiles { f -> f.name.startsWith("${file.name}.tmp") }?.forEach { strictDelete(it) }
    }

    /** 目录项持久确认（strict 模式）：回调失败即抛 [IOException]（fail-closed）。 */
    private fun requireDirSyncDurable() {
        val strict = strictDirectorySync ?: return
        if (!strict(parent)) {
            throw IOException("Directory entry not confirmed durable for spool metadata: ${file.path}")
        }
    }

    /** strict 模式的重命名：失败抛 [IOException]；成功后必须确认目录项持久。 */
    private fun strictRename(from: File, to: File) {
        if (!from.renameTo(to)) {
            throw IOException("Failed to rename ${from.path} to ${to.path}")
        }
        requireDirSyncDurable()
    }

    /** strict 模式的删除：文件不存在时无目录项变更（不要求 sync）；删除成功且 strict 时确认持久。 */
    private fun strictDelete(f: File) {
        if (f.exists() && f.delete()) requireDirSyncDurable()
    }

    private fun deleteQuietly(f: File) {
        try {
            f.delete()
        } catch (_: Exception) {
        }
    }

    private companion object {
        fun defaultAtomicMove(from: File, to: File): Boolean = try {
            Files.move(
                from.toPath(),
                to.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
            true
        } catch (e: AtomicMoveNotSupportedException) {
            false
        } catch (e: IOException) {
            false
        }
    }
}
